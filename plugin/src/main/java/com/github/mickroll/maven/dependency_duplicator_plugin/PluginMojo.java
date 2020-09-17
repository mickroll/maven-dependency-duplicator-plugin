package com.github.mickroll.maven.dependency_duplicator_plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

public class PluginMojo extends AbstractMojo {

    @Parameter(property = "blubb")
    String blubb2;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        throw new MojoExecutionException("This mojo should not be executed. It may only be used for configuration.");
    }
}
