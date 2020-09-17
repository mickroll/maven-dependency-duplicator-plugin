package com.github.mickroll.maven.dependency_duplicator_plugin.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Runs various test scenarios.
 * <p>
 * For details, see the corresponding readme files.
 *
 * @author mickroll
 */
class IntegrationTest {

    @Test
    void testSimple() throws Exception {
        new MavenWrapper("simple").run("test", 0);
    }

    @Test
    void testMultiModule() throws Exception {
        final List<String> stdout = new MavenWrapper("multimodule").run("install", 0);

        assertThat(stdout).contains("found class: org.example.submod1.Submod1Implementation");
        assertThat(stdout).contains("found class: org.example.submod1.TestImplementation");
        assertThat(stdout).contains("found class: org.example.submod2.Submod2Implementation"); // this is found because of the automatically added dependency:
    }

    @Test
    void testExtraDependencies() throws Exception {
        final List<String> stdout = new MavenWrapper("extra-dependencies").run("install", 0);

        assertThat(stdout).contains("USED COMMONS-LANG3!");
    }
}
