package com.github.mickroll.maven.dependency_duplicator_plugin;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.maven.model.Dependency;
import org.junit.jupiter.api.Test;

class DependencyMatcherTest {

    private static final Dependency EXAMPLE_DEPENDENCY;
    static {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("com.example");
        dependency.setArtifactId("someartifact");
        dependency.setType("jar");
        EXAMPLE_DEPENDENCY = dependency;
    }

    private static final Dependency EXAMPLE_DEPENDENCY_WITH_CLASSIFIER;
    static {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("com.example");
        dependency.setArtifactId("someartifact");
        dependency.setType("jar");
        dependency.setClassifier("testclassifier");
        EXAMPLE_DEPENDENCY_WITH_CLASSIFIER = dependency;
    }

    ////////////
    // tests without classifier in given dependency

    @Test
    void testGroupOnly() {
        assertThat(new DependencyMatcher("com.example").matches(EXAMPLE_DEPENDENCY)).isFalse();
    }

    @Test
    void testGroupOnlyWithMatchAny() {
        assertThat(new DependencyMatcher("com.example:.*").matches(EXAMPLE_DEPENDENCY)).isTrue();
    }

    @Test
    void testGroupArtifactOnly() {
        assertThat(new DependencyMatcher("com.example:someartifact").matches(EXAMPLE_DEPENDENCY)).isFalse();
    }

    @Test
    void testGroupArtifactOnlyWithMatchAny() {
        assertThat(new DependencyMatcher("com.example:someartifact:.*").matches(EXAMPLE_DEPENDENCY)).isTrue();
    }

    @Test
    void testGroupArtifactType() {
        assertThat(new DependencyMatcher("com.example:someartifact:jar").matches(EXAMPLE_DEPENDENCY)).isTrue();
    }

    ////////////
    // tests with classifier in given dependency

    @Test
    void testClassifier_GroupOnly() {
        assertThat(new DependencyMatcher("com.example").matches(EXAMPLE_DEPENDENCY_WITH_CLASSIFIER)).isFalse();
    }

    @Test
    void testClassifier_GroupOnlyWithMatchAny() {
        assertThat(new DependencyMatcher("com.example:.*").matches(EXAMPLE_DEPENDENCY_WITH_CLASSIFIER)).isTrue();
    }

    @Test
    void testClassifier_GroupArtifactOnly() {
        assertThat(new DependencyMatcher("com.example:someartifact").matches(EXAMPLE_DEPENDENCY_WITH_CLASSIFIER)).isFalse();
    }

    @Test
    void testClassifier_GroupArtifactOnlyWithMatchAny() {
        assertThat(new DependencyMatcher("com.example:someartifact:.*").matches(EXAMPLE_DEPENDENCY_WITH_CLASSIFIER)).isTrue();
    }

    @Test
    void testClassifier_GroupArtifactTypeOnly() {
        assertThat(new DependencyMatcher("com.example:someartifact:jar").matches(EXAMPLE_DEPENDENCY_WITH_CLASSIFIER)).isFalse();
    }

    @Test
    void testClassifier_GroupArtifactTypeOnlyWithMatchAny() {
        assertThat(new DependencyMatcher("com.example:someartifact:jar:.*").matches(EXAMPLE_DEPENDENCY_WITH_CLASSIFIER)).isTrue();
    }

    @Test
    void testClassifier_GroupArtifactTypeClassifier() {
        assertThat(new DependencyMatcher("com.example:someartifact:jar:testclassifier").matches(EXAMPLE_DEPENDENCY_WITH_CLASSIFIER)).isTrue();
    }
}
