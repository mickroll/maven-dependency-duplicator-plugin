package com.github.mickroll.maven.dependency_duplicator_plugin.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.maven.model.Dependency;
import org.junit.jupiter.api.Test;

class DependencyDuplicationTest {

    private static final Dependency EXAMPLE_SOMEARTIFACT;
    static {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("com.example");
        dependency.setArtifactId("someartifact");
        dependency.setType("jar");
        EXAMPLE_SOMEARTIFACT = dependency;
    }

    private static final Dependency EXAMPLE_OTHERARTIFACT;
    static {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("com.example");
        dependency.setArtifactId("otherartifact");
        dependency.setType("jar");
        EXAMPLE_OTHERARTIFACT = dependency;
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
        assertThat(dupl("com.example").matches(EXAMPLE_SOMEARTIFACT)).isFalse();
        assertThat(dupl("com.example").matches(EXAMPLE_OTHERARTIFACT)).isFalse();
    }

    @Test
    void testGroupOnlyWithMatchAny() {
        assertThat(dupl("com.example:.*").matches(EXAMPLE_SOMEARTIFACT)).isTrue();
        assertThat(dupl("com.example:.*").matches(EXAMPLE_OTHERARTIFACT)).isTrue();
    }

    @Test
    void testGroupArtifactOnly() {
        assertThat(dupl("com.example:someartifact").matches(EXAMPLE_SOMEARTIFACT)).isFalse();
        assertThat(dupl("com.example:someartifact").matches(EXAMPLE_OTHERARTIFACT)).isFalse();
    }

    @Test
    void testGroupArtifactOnlyWithMatchAny() {
        assertThat(dupl("com.example:someartifact:.*").matches(EXAMPLE_SOMEARTIFACT)).isTrue();
        assertThat(dupl("com.example:someartifact:.*").matches(EXAMPLE_OTHERARTIFACT)).isFalse();
    }

    @Test
    void testGroupArtifactType() {
        assertThat(dupl("com.example:someartifact:jar").matches(EXAMPLE_SOMEARTIFACT)).isTrue();
        assertThat(dupl("com.example:someartifact:jar").matches(EXAMPLE_OTHERARTIFACT)).isFalse();
    }

    @Test
    void testTwoDependencies() {
        final DependencyDuplication config = dupl("com.example:someartifact:jar, com.example:otherartifact:jar");

        assertThat(config.matches(EXAMPLE_SOMEARTIFACT)).isTrue();
        assertThat(config.matches(EXAMPLE_OTHERARTIFACT)).isTrue();
    }

    ////////////
    // tests with classifier in given dependency

    @Test
    void testClassifier_GroupOnly() {
        assertThat(dupl("com.example").matches(EXAMPLE_DEPENDENCY_WITH_CLASSIFIER)).isFalse();
    }

    @Test
    void testClassifier_GroupOnlyWithMatchAny() {
        assertThat(dupl("com.example:.*").matches(EXAMPLE_DEPENDENCY_WITH_CLASSIFIER)).isTrue();
    }

    @Test
    void testClassifier_GroupArtifactOnly() {
        assertThat(dupl("com.example:someartifact").matches(EXAMPLE_DEPENDENCY_WITH_CLASSIFIER)).isFalse();
    }

    @Test
    void testClassifier_GroupArtifactOnlyWithMatchAny() {
        assertThat(dupl("com.example:someartifact:.*").matches(EXAMPLE_DEPENDENCY_WITH_CLASSIFIER)).isTrue();
    }

    @Test
    void testClassifier_GroupArtifactTypeOnly() {
        assertThat(dupl("com.example:someartifact:jar").matches(EXAMPLE_DEPENDENCY_WITH_CLASSIFIER)).isFalse();
    }

    @Test
    void testClassifier_GroupArtifactTypeOnlyWithMatchAny() {
        assertThat(dupl("com.example:someartifact:jar:.*").matches(EXAMPLE_DEPENDENCY_WITH_CLASSIFIER)).isTrue();
    }

    @Test
    void testClassifier_GroupArtifactTypeClassifier() {
        assertThat(dupl("com.example:someartifact:jar:testclassifier").matches(EXAMPLE_DEPENDENCY_WITH_CLASSIFIER)).isTrue();
    }

    // duplication tests

    @Test
    void testEmptyConfig() {
        final DependencyDuplication underTest = dupl(null, null, null);
        final Dependency dependency = dep("com.example", "someartifact", "jar", null, "compile");

        final Dependency clone = underTest.doDuplicate(dependency);

        assertThat(clone.getGroupId()).isEqualTo(dependency.getGroupId());
        assertThat(clone.getArtifactId()).isEqualTo(dependency.getArtifactId());
        assertThat(clone.getType()).isEqualTo(dependency.getType());
        assertThat(clone.getClassifier()).isEqualTo(dependency.getClassifier());
        assertThat(clone.getScope()).isEqualTo(dependency.getScope());
    }

    @Test
    void testScope() {
        final DependencyDuplication underTest = dupl("newScope", null, null);
        final Dependency dependency = dep("com.example", "someartifact", "jar", null, "compile");

        final Dependency clone = underTest.doDuplicate(dependency);

        assertThat(clone.getGroupId()).isEqualTo(dependency.getGroupId());
        assertThat(clone.getArtifactId()).isEqualTo(dependency.getArtifactId());
        assertThat(clone.getType()).isEqualTo(dependency.getType());
        assertThat(clone.getClassifier()).isEqualTo(dependency.getClassifier());
        assertThat(clone.getScope()).isEqualTo("newScope");
    }

    @Test
    void testNewType() {
        final DependencyDuplication underTest = dupl(null, "newType", null);
        final Dependency dependency = dep("com.example", "someartifact", "jar", null, "compile");

        final Dependency clone = underTest.doDuplicate(dependency);

        assertThat(clone.getGroupId()).isEqualTo(dependency.getGroupId());
        assertThat(clone.getArtifactId()).isEqualTo(dependency.getArtifactId());
        assertThat(clone.getType()).isEqualTo("newType");
        assertThat(clone.getClassifier()).isEqualTo(dependency.getClassifier());
        assertThat(clone.getScope()).isEqualTo(dependency.getScope());
    }

    @Test
    void testNewClassifier() {
        final DependencyDuplication underTest = dupl(null, null, "newClassifier");
        final Dependency dependency = dep("com.example", "someartifact", "jar", null, "compile");

        final Dependency clone = underTest.doDuplicate(dependency);

        assertThat(clone.getGroupId()).isEqualTo(dependency.getGroupId());
        assertThat(clone.getArtifactId()).isEqualTo(dependency.getArtifactId());
        assertThat(clone.getType()).isEqualTo(dependency.getType());
        assertThat(clone.getClassifier()).isEqualTo("newClassifier");
        assertThat(clone.getScope()).isEqualTo(dependency.getScope());
    }

    private static DependencyDuplication dupl(final String sourceRegEx) {
        final DependencyDuplication result = new DependencyDuplication();
        result.source = sourceRegEx;
        return result;
    }

    private static DependencyDuplication dupl(final String targetScope, final String targetType, final String targetClassifier) {
        final DependencyDuplication result = new DependencyDuplication();
        result.targetScope = targetScope;
        result.targetType = targetType;
        result.targetClassifier = targetClassifier;
        return result;
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
