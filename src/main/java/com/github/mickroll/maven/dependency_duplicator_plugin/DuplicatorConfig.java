package com.github.mickroll.maven.dependency_duplicator_plugin;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.model.Dependency;

/**
 * Plugin configuration.
 *
 * @author mickroll
 */
public class DuplicatorConfig {

    private final static String PROPERTY_SOURCE_DEPENDENCIES = "ddp.sourceDependencies";
    private final static String PROPERTY_TARGET_SCOPE = "ddp.targetScope";
    private final static String PROPERTY_TARGET_TYPE = "ddp.targetType";
    private final static String PROPERTY_TARGET_CLASSIFIER = "ddp.targetClassifier";
    private final static String PROPERTY_ADD_DEPENDENCIES_DOWNSTREAM = "ddp.addDependenciesDownstream";

    private final List<DependencyMatcher> dependenciesToMatch;
    private final String targetScope;
    private final String targetType;
    private final String targetClassifier;
    private final boolean addDependenciesDownstream;

    DuplicatorConfig(final List<DependencyMatcher> dependenciesToMatch,
            final String targetScope,
            final String targetType,
            final String targetClassifier,
            final boolean addDependenciesDownstream) {
        this.dependenciesToMatch = dependenciesToMatch;
        this.targetScope = targetScope;
        this.targetType = targetType;
        this.targetClassifier = targetClassifier;
        this.addDependenciesDownstream = addDependenciesDownstream;
    }

    public Optional<DependencyMatcher> findAnyMatcher(final Dependency dependency) {
        return dependenciesToMatch.stream().filter(matcher -> matcher.matches(dependency)).findFirst();
    }

    public List<DependencyMatcher> getDependenciesToMatch() {
        return dependenciesToMatch;
    }

    public Optional<String> getTargetScope() {
        return Optional.ofNullable(targetScope);
    }

    public Optional<String> getTargetType() {
        return Optional.ofNullable(targetType);
    }

    public Optional<String> getTargetClassifier() {
        return Optional.ofNullable(targetClassifier);
    }

    public boolean isAddDependenciesDownstream() {
        return addDependenciesDownstream;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("dependenciesToMatch=").append(dependenciesToMatch);
        if (targetScope != null) {
            sb.append(", targetScope=").append(targetScope);
        }
        if (targetType != null) {
            sb.append(", targetType=").append(targetType);
        }
        if (targetClassifier != null) {
            sb.append(", targetClassifier=").append(targetClassifier);
        }
        sb.append(", addDependenciesDownstream=").append(addDependenciesDownstream);
        return sb.toString();
    }

    /**
     * Read config properties and construct DuplicatorConfig.
     *
     * @param projectProperties properties to read, typically these are the maven project properties
     * @return read config
     */
    public static DuplicatorConfig read(final Properties projectProperties) {
        final String config = projectProperties.getProperty(PROPERTY_SOURCE_DEPENDENCIES, "");
        final List<DependencyMatcher> dependenciesToMatch = Stream.of(config.split(","))
                .map(String::trim)
                .filter(element -> !element.isEmpty())
                .map(DependencyMatcher::new)
                .collect(Collectors.toList());

        final String targetScope = projectProperties.getProperty(PROPERTY_TARGET_SCOPE);
        final String targetType = projectProperties.getProperty(PROPERTY_TARGET_TYPE);
        final String targetClassifier = projectProperties.getProperty(PROPERTY_TARGET_CLASSIFIER);

        final boolean addDependenciesDownstream = Boolean
                .parseBoolean(projectProperties.getProperty(PROPERTY_ADD_DEPENDENCIES_DOWNSTREAM, Boolean.TRUE.toString()));

        return new DuplicatorConfig(dependenciesToMatch, targetScope, targetType, targetClassifier, addDependenciesDownstream);
    }
}
