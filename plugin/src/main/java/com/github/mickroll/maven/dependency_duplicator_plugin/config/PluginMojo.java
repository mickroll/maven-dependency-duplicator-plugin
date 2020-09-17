package com.github.mickroll.maven.dependency_duplicator_plugin.config;

import java.util.List;
import java.util.Optional;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = PluginMojo.GOAL)
public class PluginMojo extends AbstractMojo {

    public static final String GOAL = "duplicate-dependencies";

    public static final String PLUGIN_KEY = "com.github.madprogger:dependency-duplicator-plugin";

    /**
     * Duplications to match and execute against existing dependencies.
     * <p>
     * First match wins.
     */
    @Parameter
    List<DependencyDuplication> duplications;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        throw new MojoExecutionException("This mojo should not be executed. It may only be used for configuration.");
    }

    public boolean hasDefinedDuplications() {
        return !duplications.isEmpty();
    }

    /**
     * Finds the first (top to bottom) DependencyDuplication that matches the given dependency.
     *
     * @param dependency dependency
     * @return first found duplication, if any
     */
    public Optional<DependencyDuplication> findFirstDuplicationConfig(final Dependency dependency) {
        return duplications.stream().filter(matcher -> matcher.matches(dependency)).findFirst();
    }

    @Override
    public String toString() {
        return duplications.toString();
    }
}
