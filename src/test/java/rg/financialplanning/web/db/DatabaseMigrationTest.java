package rg.financialplanning.web.db;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.*;

public class DatabaseMigrationTest {

    private Path dir;

    @Before
    public void dir() throws IOException {
        dir = Files.createTempDirectory("planner-db");
    }

    @After
    public void cleanup() throws IOException {
        try (var files = Files.walk(dir)) {
            files.sorted((a, b) -> b.compareTo(a)).forEach(p -> p.toFile().delete());
        }
    }

    @Test
    public void freshFileIsCurrent() {
        try (Database db = new Database(dir.resolve("fresh.db"))) {
            assertEquals(4, db.schemaVersion());
            assertTrue(hasColumn(db, "entries", "annual_payment"));
            assertTrue(hasColumn(db, "projection_history", "mortgage_interest"));
            assertTrue(hasColumn(db, "persons", "ss_fra_monthly"));
            assertTrue(hasColumn(db, "projection_history", "health_premiums"));
            assertTrue(hasColumn(db, "premium_estimates", "medical_inflation_pct"));
        }
    }

    @Test
    public void version1FileIsMigratedOnOpen() {
        Path file = dir.resolve("old.db");
        try (Database db = new Database(file)) {
            // Turn the fresh file back into a version-1 one.
            db.inTransaction(c -> {
                try (Statement st = c.createStatement()) {
                    st.execute("ALTER TABLE entries DROP COLUMN annual_payment");
                    st.execute("ALTER TABLE projection_history DROP COLUMN mortgage_interest");
                    st.execute("ALTER TABLE projection_history DROP COLUMN mortgage_principal");
                    st.execute("ALTER TABLE projection_history DROP COLUMN mortgage_extra");
                    st.execute("ALTER TABLE projection_history DROP COLUMN health_premiums");
                    st.execute("ALTER TABLE projection_history DROP COLUMN irmaa");
                    st.execute("ALTER TABLE persons DROP COLUMN ss_fra_monthly");
                    st.execute("ALTER TABLE persons DROP COLUMN ss_claim_age");
                    st.execute("ALTER TABLE persons DROP COLUMN pre_medicare_premium_monthly");
                    st.execute("ALTER TABLE persons DROP COLUMN medicare_plan_premium_monthly");
                    st.execute("DROP TABLE premium_estimates");
                    st.execute("DELETE FROM schema_meta");
                    st.execute("INSERT INTO schema_meta(version, applied_at) VALUES (1, 'x')");
                }
                return null;
            });
            assertEquals(1, db.schemaVersion());
            assertFalse(hasColumn(db, "entries", "annual_payment"));
        }
        try (Database db = new Database(file)) {
            assertEquals(4, db.schemaVersion());
            assertTrue(hasColumn(db, "premium_estimates", "prompt"));
            assertTrue(hasColumn(db, "entries", "annual_payment"));
            assertTrue(hasColumn(db, "projection_history", "mortgage_interest"));
            assertTrue(hasColumn(db, "projection_history", "mortgage_principal"));
            assertTrue(hasColumn(db, "projection_history", "mortgage_extra"));
            assertTrue(hasColumn(db, "persons", "ss_claim_age"));
            assertTrue(hasColumn(db, "persons", "medicare_plan_premium_monthly"));
            assertTrue(hasColumn(db, "projection_history", "irmaa"));
        }
    }

    private static boolean hasColumn(Database db, String table, String column) {
        return db.read(c -> {
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
                while (rs.next()) {
                    if (column.equals(rs.getString("name"))) return true;
                }
                return false;
            }
        });
    }
}
