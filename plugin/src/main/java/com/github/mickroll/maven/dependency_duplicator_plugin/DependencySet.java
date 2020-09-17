package com.github.mickroll.maven.dependency_duplicator_plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;

/**
 * {@link Dependency} has no {@link Object#equals(Object)}-Method.
 * <p>
 * This is a straight-forward implementation of a {@link Set} of dependencies.
 *
 * @author mickroll
 * @see Dependency
 */
public class DependencySet {

    private final Set<Dependency> dependencies = new LinkedHashSet<>();

    public void add(final Dependency newDependency) {
        if (!dependencies.stream().anyMatch(d -> isDeepEqualTo(d, newDependency))) {
            dependencies.add(newDependency);
        }
    }

    public void addAll(final Collection<Dependency> newDependencies) {
        newDependencies.forEach(this::add);
    }

    public Set<Dependency> asSet() {
        return Collections.unmodifiableSet(dependencies);
    }

    static boolean isDeepEqualTo(final Dependency d1, final Dependency d2) {
        return Objects.equals(d1.getGroupId(), d2.getGroupId())
                && Objects.equals(d1.getArtifactId(), d2.getArtifactId())
                && Objects.equals(d1.getVersion(), d2.getVersion())
                && Objects.equals(d1.getClassifier(), d2.getClassifier())
                && Objects.equals(d1.getScope(), d2.getScope())
                && Objects.equals(d1.getType(), d2.getType())
                && Objects.equals(d1.getOptional(), d2.getOptional())
                && Objects.equals(
                        d1.getExclusions().stream().map(e -> e.getGroupId() + ":" + e.getArtifactId()).collect(Collectors.toSet()),
                        d2.getExclusions().stream().map(e -> e.getGroupId() + ":" + e.getArtifactId()).collect(Collectors.toSet()));
    }
}
