package com.github.mickroll.maven.dependency_duplicator_plugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.apache.maven.model.Dependency;
import org.junit.jupiter.api.Test;

class DependencyClonerTest {

    @Test
    void testEmptyConfig() {
        final DependencyCloner underTest = new DependencyCloner(config(null, null));
        final Dependency dependency = dep("com.example", "someartifact", "jar", null, "compile");

        final Dependency clone = underTest.clone(dependency);

        assertThat(clone.getGroupId()).isEqualTo(dependency.getGroupId());
        assertThat(clone.getArtifactId()).isEqualTo(dependency.getArtifactId());
        assertThat(clone.getType()).isEqualTo(dependency.getType());
        assertThat(clone.getClassifier()).isEqualTo(dependency.getClassifier());
        assertThat(clone.getScope()).isEqualTo(dependency.getScope());
    }

    @Test
    void testScope() {
        final DependencyCloner underTest = new DependencyCloner(config("newScope", null));
        final Dependency dependency = dep("com.example", "someartifact", "jar", null, "compile");

        final Dependency clone = underTest.clone(dependency);

        assertThat(clone.getGroupId()).isEqualTo(dependency.getGroupId());
        assertThat(clone.getArtifactId()).isEqualTo(dependency.getArtifactId());
        assertThat(clone.getType()).isEqualTo(dependency.getType());
        assertThat(clone.getClassifier()).isEqualTo(dependency.getClassifier());
        assertThat(clone.getScope()).isEqualTo("newScope");
    }

    @Test
    void testNewType() {
        final DependencyCloner underTest = new DependencyCloner(config(null, "newType"));
        final Dependency dependency = dep("com.example", "someartifact", "jar", null, "compile");

        final Dependency clone = underTest.clone(dependency);

        assertThat(clone.getGroupId()).isEqualTo(dependency.getGroupId());
        assertThat(clone.getArtifactId()).isEqualTo(dependency.getArtifactId());
        assertThat(clone.getType()).isEqualTo("newType");
        assertThat(clone.getClassifier()).isEqualTo(dependency.getClassifier());
        assertThat(clone.getScope()).isEqualTo(dependency.getScope());
    }

    private static DuplicatorConfig config(final String targetScope, final String targetType) {
        return new DuplicatorConfig(Collections.emptyList(), targetScope, targetType, false);
    }

    private static Dependency dep(final String groupId, final String artifactId, final String type, final String classifier, final String scope) {
        final Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setType(type);
        dependency.setClassifier(classifier);
        dependency.setScope(scope);
        return dependency;
    }
}
