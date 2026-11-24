package rg.financialplanning.web.claude;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs one prompt through the local {@code claude} CLI in print mode and returns
 * its final answer. The prompt goes on stdin: on Windows {@code claude.cmd} passes
 * through cmd.exe, which cuts an argument at its first newline.
 *
 * <p>A web-research run takes two to four minutes, so callers run this off the
 * request thread. {@code ANTHROPIC_API_KEY} is removed from the child's
 * environment so the run bills the subscription, never an API key.
 */
public class ClaudeCliRunner {

    private static final Logger log = LoggerFactory.getLogger(ClaudeCliRunner.class);
    public static final long DEFAULT_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(6);
    public static final int DEFAULT_MAX_TURNS = 15;

    private final String executable;
    private final long timeoutMs;

    public ClaudeCliRunner() {
        this(null, DEFAULT_TIMEOUT_MS);
    }

    /** @param executable path to the CLI, or null/blank to look it up on PATH. */
    public ClaudeCliRunner(String executable, long timeoutMs) {
        this.executable = executable == null || executable.isBlank() ? resolveClaude() : executable;
        this.timeoutMs = timeoutMs;
    }

    public String executable() {
        return executable;
    }

    /**
     * @param allowedTools comma-separated CLI tool allow-list ({@code "WebSearch,WebFetch"}), or null.
     * @param maxTurns     cap on agentic turns; 0 for the CLI default. A research prompt with
     *                     no cap decides its own budget, and the budget is the session window.
     */
    public Result run(String prompt, String allowedTools, int maxTurns) {
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.add("-p");
        if (allowedTools != null && !allowedTools.isBlank()) {
            command.add("--allowedTools");
            command.add(allowedTools);
        }
        if (maxTurns > 0) {
            command.add("--max-turns");
            command.add(Integer.toString(maxTurns));
        }
        return exec(command, prompt);
    }

    /**
     * One answer from the model alone: no tools, a single turn, and no transcript
     * written to disk, so the figures in the prompt stay in the planner's own store.
     */
    public Result runWithoutTools(String prompt) {
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.add("-p");
        command.add("--tools");
        command.add("");
        command.add("--max-turns");
        command.add("1");
        command.add("--no-session-persistence");
        return exec(command, prompt);
    }

    private Result exec(List<String> command, String prompt) {
        long start = System.currentTimeMillis();
        log.info("claude run: {} (prompt on stdin, {} chars)", String.join(" ", command), prompt.length());
        try {
            ProcessBuilder pb = new ProcessBuilder(command).redirectErrorStream(false);
            pb.environment().remove("ANTHROPIC_API_KEY");
            Process proc = pb.start();
            // Print mode still probes stdin; leaving the pipe open blocks the CLI forever.
            try (var stdin = proc.getOutputStream()) {
                stdin.write(prompt.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ignore) {
                // stdin already closed by the child
            }
            StreamCollector out = StreamCollector.start(proc.getInputStream());
            StreamCollector err = StreamCollector.start(proc.getErrorStream());
            if (!proc.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                proc.destroyForcibly();
                proc.waitFor();
                log.warn("claude run timed out after {} ms", timeoutMs);
                return Result.error("claude timed out after " + (timeoutMs / 1000) + " s", elapsed(start));
            }
            int exit = proc.exitValue();
            String stdout = out.get().trim();
            String stderr = err.get().trim();
            if (exit != 0) {
                String detail = !stderr.isBlank() ? stderr : (!stdout.isBlank() ? stdout : "(no output)");
                log.warn("claude exit={}: {}", exit, summarize(detail));
                return Result.error("claude exited " + exit + ": " + summarize(detail), elapsed(start));
            }
            if (stdout.isBlank()) {
                return Result.error("claude produced no output" + (stderr.isBlank() ? "" : ": " + summarize(stderr)),
                        elapsed(start));
            }
            log.info("claude run ok in {} ms ({} chars)", elapsed(start), stdout.length());
            return new Result(true, stdout, null, elapsed(start));
        } catch (IOException e) {
            log.warn("claude run failed: {}", e.toString());
            return Result.error("Could not start " + executable + ": " + e.getMessage(), elapsed(start));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.error("interrupted", elapsed(start));
        }
    }

    private static long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }

    /** The CLI from PATH, Windows launchers first. Falls back to the bare name, which fails loudly. */
    static String resolveClaude() {
        boolean win = System.getProperty("os.name", "").toLowerCase().contains("win");
        String[] candidates = win ? new String[] {"claude.exe", "claude.cmd", "claude"} : new String[] {"claude"};
        String path = System.getenv("PATH");
        if (path != null) {
            for (String name : candidates) {
                for (String dir : path.split(File.pathSeparator)) {
                    if (dir.isBlank()) continue;
                    try {
                        Path p = Paths.get(dir, name);
                        if (Files.isRegularFile(p)) return p.toAbsolutePath().toString();
                    } catch (Exception ignore) {
                        // malformed PATH entry
                    }
                }
            }
        }
        return candidates[0];
    }

    private static String summarize(String s) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) return "(empty)";
        return t.length() > 300 ? t.substring(0, 300) + "..." : t;
    }

    public record Result(boolean ok, String output, String error, long durationMs) {
        static Result error(String message, long durationMs) {
            return new Result(false, null, message, durationMs);
        }
    }

    /** Drains one process stream on a daemon thread so both pipes empty concurrently. */
    private static final class StreamCollector {
        private final Thread thread;
        private volatile String result = "";

        static StreamCollector start(InputStream in) {
            return new StreamCollector(in);
        }

        private StreamCollector(InputStream in) {
            thread = new Thread(() -> {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line).append('\n');
                } catch (IOException e) {
                    sb.append("[stream read error: ").append(e).append(']');
                }
                result = sb.toString();
            }, "claude-stream-pump");
            thread.setDaemon(true);
            thread.start();
        }

        String get() {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return result;
        }
    }
}
