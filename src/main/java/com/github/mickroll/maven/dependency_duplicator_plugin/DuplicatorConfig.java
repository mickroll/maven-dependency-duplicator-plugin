package com.github.mickroll.maven.dependency_duplicator_plugin;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

public class DuplicatorConfig {

    private final static String PROPERTY_SOURCE_DEPENDENCIES = "ddp.sourceDependencies";
    private final static String PROPERTY_TARGET_SCOPE = "ddp.targetScope";
    private final static String PROPERTY_TARGET_TYPE = "ddp.targetType";
    private final static String PROPERTY_ADD_DEPENDENCIES_DOWNSTREAM = "ddp.addDependenciesDownstream";

    private final List<DependencyMatcher> dependenciesToMatch;
    private final String targetScope;
    private final String targetType;
    private final boolean addDependenciesDownstream;

    public DuplicatorConfig(final List<DependencyMatcher> dependenciesToMatch, final String targetScope, final String targetType,
            final boolean addDependenciesDownstream) {
        this.dependenciesToMatch = dependenciesToMatch;
        this.targetScope = targetScope;
        this.targetType = targetType;
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
        sb.append(", addDependenciesDownstream=").append(addDependenciesDownstream);
        return sb.toString();
    }

    public static DuplicatorConfig read(final MavenProject project) {
        final String config = project.getProperties().getProperty(PROPERTY_SOURCE_DEPENDENCIES, "");
        final List<DependencyMatcher> dependenciesToMatch = Stream.of(config.split(",")).map(String::trim)
                .filter(element -> !element.isEmpty()).map(DependencyMatcher::new).collect(Collectors.toList());

        final String targetScope = project.getProperties().getProperty(PROPERTY_TARGET_SCOPE);
        final String targetType = project.getProperties().getProperty(PROPERTY_TARGET_TYPE);

        final boolean addDependenciesDownstream = Boolean
                .valueOf(project.getProperties().getProperty(PROPERTY_ADD_DEPENDENCIES_DOWNSTREAM, Boolean.TRUE.toString()));

        return new DuplicatorConfig(dependenciesToMatch, targetScope, targetType, addDependenciesDownstream);
    }
}
