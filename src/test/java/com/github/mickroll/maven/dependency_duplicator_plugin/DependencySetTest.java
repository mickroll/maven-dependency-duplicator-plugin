package com.github.mickroll.maven.dependency_duplicator_plugin;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.maven.model.Dependency;
import org.junit.jupiter.api.Test;

class DependencySetTest {

    @Test
    void testEmptyEquals() {
        final Dependency d1 = new Dependency();
        final Dependency d2 = new Dependency();

        assertThat(DependencySet.isDeepEqualTo(d1, d2)).isTrue();
    }

    @Test
    void testGATC_Same() {
        final Dependency d1 = dep("com.example", "someartifact", "jar", "testclassifier");
        final Dependency d2 = dep("com.example", "someartifact", "jar", "testclassifier");

        assertThat(DependencySet.isDeepEqualTo(d1, d2)).isTrue();
    }

    @Test
    void testGATC_DiffG() {
        final Dependency d1 = dep("com.example", "someartifact", "jar", "testclassifier");
        final Dependency d2 = dep("com.example2", "someartifact", "jar", "testclassifier");

        assertThat(DependencySet.isDeepEqualTo(d1, d2)).isFalse();
    }

    @Test
    void testGATC_DiffA() {
        final Dependency d1 = dep("com.example", "someartifact", "jar", "testclassifier");
        final Dependency d2 = dep("com.example", "someartifact2", "jar", "testclassifier");

        assertThat(DependencySet.isDeepEqualTo(d1, d2)).isFalse();
    }

    @Test
    void testGATC_DiffT() {
        final Dependency d1 = dep("com.example", "someartifact", "jar", "testclassifier");
        final Dependency d2 = dep("com.example", "someartifact", "war", "testclassifier");

        assertThat(DependencySet.isDeepEqualTo(d1, d2)).isFalse();
    }

    @Test
    void testGATC_DiffC() {
        final Dependency d1 = dep("com.example", "someartifact", "jar", "testclassifier");
        final Dependency d2 = dep("com.example", "someartifact", "jar", "testclassifier2");

        assertThat(DependencySet.isDeepEqualTo(d1, d2)).isFalse();
    }

    @Test
    void testGATC_DiffNoC() {
        final Dependency d1 = dep("com.example", "someartifact", "jar", "testclassifier");
        final Dependency d2 = dep("com.example", "someartifact", "jar", null);

        assertThat(DependencySet.isDeepEqualTo(d1, d2)).isFalse();
    }

    @Test
    void testGAT_Same() {
        final Dependency d1 = dep("com.example", "someartifact", "jar", null);
        final Dependency d2 = dep("com.example", "someartifact", "jar", null);

        assertThat(DependencySet.isDeepEqualTo(d1, d2)).isTrue();
    }

    private static Dependency dep(final String groupId, final String artifactId, final String type, final String classifier) {
        final Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setType(type);
        dependency.setClassifier(classifier);
        return dependency;
    }
}
