package com.github.mickroll.maven.dependency_duplicator_plugin.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;

public class MavenWrapper {
    private static final String BASE_DIR = "target/test-classes/it/";
    private static final String MVN_COMMAND;
    static {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            MVN_COMMAND = "mvn.cmd";
        } else {
            MVN_COMMAND = "mvn";
        }
    }

    private final File runDir;

    public MavenWrapper(final String subdir) {
        this.runDir = new File(BASE_DIR, subdir).getAbsoluteFile();
    }

    public List<String> run(final String goal, final int expectedExitValue) throws IOException, InterruptedException {
        final Process process = new ProcessBuilder(Arrays.asList(MVN_COMMAND, goal))
                .directory(runDir)
                .redirectErrorStream(true)
                .start();

        final List<String> stdoutLines = new ArrayList<>();
        Executors.newSingleThreadExecutor()
        .submit(() -> new BufferedReader(new InputStreamReader(process.getInputStream())).lines().forEach(stdoutLines::add));

        process.waitFor(1, TimeUnit.MINUTES);

        FileUtils.writeLines(new File(runDir, "build.log"), stdoutLines);

        if (process.exitValue() != expectedExitValue) {
            System.out.println(formatLines("> ", stdoutLines));
        }
        assertThat(process.exitValue()).as("exit value").isEqualTo(expectedExitValue);

        return stdoutLines;
    }

    public static String formatLines(final String prefix, final List<String> lines) {
        final StringBuilder result = new StringBuilder();
        for (final String line : lines) {
            result.append(prefix).append(line).append("\n");
        }
        return result.toString();
    }
}