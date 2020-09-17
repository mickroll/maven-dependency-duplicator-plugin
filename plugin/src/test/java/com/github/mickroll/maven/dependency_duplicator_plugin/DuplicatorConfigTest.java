package com.github.mickroll.maven.dependency_duplicator_plugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.jupiter.api.Test;

class DuplicatorConfigTest {

    @Test
    void testEmpty() {
        final Properties properties = new Properties();

        final DuplicatorConfig config = DuplicatorConfig.read(properties);

        assertThat(config.isAddDependenciesDownstream()).isTrue(); // default is 'true'
        assertThat(config.getTargetScope()).isEmpty(); // default is 'no change'
        assertThat(config.getTargetType()).isEmpty(); // default is 'no change'
        assertThat(config.getDependenciesToMatch()).isEmpty(); // default is 'no dependencies to match'
    }

    @Test
    void testAddDependenciesDownstream() {
        final Properties properties = new Properties();
        properties.setProperty("ddp.addDependenciesDownstream", "true");

        final DuplicatorConfig config = DuplicatorConfig.read(properties);

        assertThat(config.isAddDependenciesDownstream()).isTrue();
    }

    @Test
    void testAddDependenciesDownstreamFalse() {
        final Properties properties = new Properties();
        properties.setProperty("ddp.addDependenciesDownstream", "false");

        final DuplicatorConfig config = DuplicatorConfig.read(properties);

        assertThat(config.isAddDependenciesDownstream()).isFalse();
    }

    @Test
    void testTargetClassifier() {
        final Properties properties = new Properties();
        properties.setProperty("ddp.targetClassifier", "testClassifier");

        final DuplicatorConfig config = DuplicatorConfig.read(properties);

        assertThat(config.getTargetClassifier()).hasValue("testClassifier");
    }

    @Test
    void testTargetScope() {
        final Properties properties = new Properties();
        properties.setProperty("ddp.targetScope", "testScope");

        final DuplicatorConfig config = DuplicatorConfig.read(properties);

        assertThat(config.getTargetScope()).hasValue("testScope");
    }

    @Test
    void testTargetType() {
        final Properties properties = new Properties();
        properties.setProperty("ddp.targetType", "testType");

        final DuplicatorConfig config = DuplicatorConfig.read(properties);

        assertThat(config.getTargetType()).hasValue("testType");
    }

    @Test
    void testSourceDependencies_1() {
        final Properties properties = new Properties();
        properties.setProperty("ddp.sourceDependencies", "com.example:testartifact:jar");

        final DuplicatorConfig config = DuplicatorConfig.read(properties);

        assertThat(config.getDependenciesToMatch()).hasSize(1);
        assertThat(config.getDependenciesToMatch().get(0).getDependencyPattern()).isEqualTo("com.example:testartifact:jar");
    }

    @Test
    void testSourceDependencies_2() {
        final Properties properties = new Properties();
        properties.setProperty("ddp.sourceDependencies", "com.example:testartifact:jar , com.example:testartifact2:jar:testclassifier");

        final DuplicatorConfig config = DuplicatorConfig.read(properties);

        assertThat(config.getDependenciesToMatch()).hasSize(2);
        assertThat(config.getDependenciesToMatch().get(0).getDependencyPattern()).isEqualTo("com.example:testartifact:jar");
        assertThat(config.getDependenciesToMatch().get(1).getDependencyPattern()).isEqualTo("com.example:testartifact2:jar:testclassifier");
    }

    @Test
    void testSourceDependencies_regex() {
        final Properties properties = new Properties();
        properties.setProperty("ddp.sourceDependencies", "com.example:.*");

        final DuplicatorConfig config = DuplicatorConfig.read(properties);

        assertThat(config.getDependenciesToMatch()).hasSize(1);
        assertThat(config.getDependenciesToMatch().get(0).getDependencyPattern()).isEqualTo("com.example:.*");
    }
}
