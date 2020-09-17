package com.github.mickroll.maven.dependency_duplicator_plugin.config;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Defines a set of rules for dependency duplication.
 *
 * @author mickroll
 */
public class DependencyDuplication {

    /**
     * Defines a comma separated list of regular expressions in the form {@code groupId:artifactId:type[:classifier]} that is used to match against
     * dependencies.
     */
    @Parameter(required = true)
    String source;

    /**
     * New scope for duplicated dependency.
     * <p>
     * Leave empty to copy source scope.
     */
    @Parameter
    String targetScope;

    /**
     * New type for duplicated dependency.
     * <p>
     * Leave empty to copy source type.
     */
    @Parameter
    String targetType;

    /**
     * New classifier for duplicated dependency.
     * <p>
     * Leave empty to copy source classifier.
     */
    @Parameter
    String targetClassifier;

    /**
     * Set to <code>false</code>, if duplicated dependencies should not be copied into dependent projects.
     */
    @Parameter(defaultValue = "true")
    boolean addDownstream;

    /**
     * Determines, if given dependency is matched by the configured {@link #source} regExes.
     *
     * @param dependency dependency
     * @return {@code true}, if management key of dependency is matched by source regEx
     * @see Dependency#getManagementKey()
     * @see String#matches(String)
     */
    public boolean matches(final Dependency dependency) {
        return Stream.of(source.split(","))
                .map(String::trim)
                .filter(element -> !element.isEmpty())
                .anyMatch(element -> dependency.getManagementKey().matches(element));
    }

    public Dependency doDuplicate(final Dependency source) {
        final Dependency clone = source.clone();
        getTargetClassifier().ifPresent(clone::setClassifier);
        getTargetScope().ifPresent(clone::setScope);
        getTargetType().ifPresent(clone::setType);
        clone.clearManagementKey(); // value is cached, beyond changes via setters
        return clone;
    }

    public String getSource() {
        return source;
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

    public boolean isAddDownstream() {
        return addDownstream;
    }

    @Override
    public String toString() {
        return "{source=" + source
                + (targetScope != null ? ", targetScope=" + targetScope : "")
                + (targetType != null ? ", targetType=" + targetType : "")
                + (targetClassifier != null ? ", targetClassifier=" + targetClassifier : "")
                + ", addDownstream=" + addDownstream + "}";
    }
}