package rg.financialplanning.web.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

/**
 * Single-writer SQLite connection for the plan store. Creates the schema on
 * first open. Default location {@code ~/.financial-planning/planner.db};
 * tests pass a temp file.
 */
public class Database implements AutoCloseable {

    private static final int CURRENT_SCHEMA = 5;

    private final Path dbFile;
    private final Connection conn;

    public static Path defaultPath() {
        return Path.of(System.getProperty("user.home"), ".financial-planning", "planner.db");
    }

    public Database() {
        this(defaultPath());
    }

    public Database(Path dbFile) {
        this.dbFile = dbFile;
        try {
            Files.createDirectories(dbFile.toAbsolutePath().getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create " + dbFile.getParent(), e);
        }
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath());
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
                st.execute("PRAGMA journal_mode = WAL");
            }
            initSchema();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to open SQLite at " + dbFile, e);
        }
    }

    public Path file() {
        return dbFile;
    }

    /** Runs the work inside one transaction; the connection is the lock. */
    public <T> T inTransaction(SqlWork<T> work) {
        synchronized (conn) {
            try {
                conn.setAutoCommit(false);
                try {
                    T result = work.run(conn);
                    conn.commit();
                    return result;
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Database write failed", e);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public <T> T read(SqlWork<T> work) {
        synchronized (conn) {
            try {
                return work.run(conn);
            } catch (SQLException e) {
                throw new IllegalStateException("Database read failed", e);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection c) throws Exception;
    }

    private void initSchema() throws SQLException {
        String ddl;
        try (InputStream in = Database.class.getResourceAsStream("/planner-schema.sql")) {
            if (in == null) throw new IllegalStateException("planner-schema.sql missing from classpath");
            ddl = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        try (Statement st = conn.createStatement()) {
            for (String stmt : ddl.replaceAll("--[^\n]*", "").split(";")) {
                String s = stmt.trim();
                if (!s.isEmpty()) st.execute(s);
            }
        }
        int version = schemaVersion();
        if (version == 0) {
            // Fresh file: the DDL above is already the current shape.
            markVersion(CURRENT_SCHEMA);
            return;
        }
        for (int v = version + 1; v <= CURRENT_SCHEMA; v++) {
            try (Statement st = conn.createStatement()) {
                for (String s : migration(v)) st.execute(s);
            }
            markVersion(v);
        }
    }

    /** Statements that bring a database from version {@code v - 1} to {@code v}. */
    private static String[] migration(int v) {
        switch (v) {
            case 2:
                return new String[] {
                    "ALTER TABLE entries ADD COLUMN annual_payment REAL NOT NULL DEFAULT 0",
                    "ALTER TABLE projection_history ADD COLUMN mortgage_interest REAL NOT NULL DEFAULT 0",
                    "ALTER TABLE projection_history ADD COLUMN mortgage_principal REAL NOT NULL DEFAULT 0",
                    "ALTER TABLE projection_history ADD COLUMN mortgage_extra REAL NOT NULL DEFAULT 0",
                };
            case 3:
                return new String[] {
                    "ALTER TABLE persons ADD COLUMN ss_fra_monthly REAL NOT NULL DEFAULT 0",
                    "ALTER TABLE persons ADD COLUMN ss_claim_age INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE persons ADD COLUMN pre_medicare_premium_monthly REAL NOT NULL DEFAULT 0",
                    "ALTER TABLE persons ADD COLUMN medicare_plan_premium_monthly REAL NOT NULL DEFAULT 0",
                    "ALTER TABLE projection_history ADD COLUMN health_premiums REAL NOT NULL DEFAULT 0",
                    "ALTER TABLE projection_history ADD COLUMN irmaa REAL NOT NULL DEFAULT 0",
                };
            case 4:
                // The DDL already created premium_estimates on this open; nothing to alter.
                return new String[0];
            case 5:
                // Likewise explanations.
                return new String[0];
            default:
                throw new IllegalStateException("No migration to schema version " + v);
        }
    }

    private void markVersion(int version) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO schema_meta(version, applied_at) VALUES (?, ?)")) {
            ps.setInt(1, version);
            ps.setString(2, Instant.now().toString());
            ps.executeUpdate();
        }
    }

    public int schemaVersion() {
        return read(c -> {
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT MAX(version) FROM schema_meta")) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    @Override
    public void close() {
        try {
            conn.close();
        } catch (SQLException ignored) {
        }
    }
}
