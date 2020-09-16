package com.github.mickroll.maven.dependency_duplicator_plugin;

import java.util.regex.Pattern;

import org.apache.maven.model.Dependency;

/**
 * Matches a given Dependency in the form {@code groupId:artifactId:type[:classifier]} to a supplied regex.
 *
 * @author mickroll
 * @see Dependency#getManagementKey()
 */
public class DependencyMatcher {

    private final Pattern dependencyPattern;

    public DependencyMatcher(final String dependencyRegex) {
        this.dependencyPattern = Pattern.compile(dependencyRegex);
    }

    public boolean matches(final Dependency dependency) {
        return dependencyPattern.matcher(dependency.getManagementKey()).matches();
    }

    public String getDependencyPattern() {
        return dependencyPattern.pattern();
    }

    @Override
    public String toString() {
        return dependencyPattern.pattern();
    }
}
