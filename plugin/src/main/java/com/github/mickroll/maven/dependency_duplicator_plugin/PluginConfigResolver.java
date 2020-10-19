package com.github.mickroll.maven.dependency_duplicator_plugin;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.BasicComponentConfigurator;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mickroll.maven.dependency_duplicator_plugin.config.PluginMojo;

public class PluginConfigResolver {

    private static final Logger LOG = LoggerFactory.getLogger(PluginConfigResolver.class);

    @Inject
    private PlexusContainer container;

    @Inject
    private MojoDescriptorCreator mojoDescriptorCreator;

    public Optional<PluginMojo> findPluginConfig(final MavenSession session, final MavenProject project) {
        MavenProject projectCursor = project;
        Plugin plugin = null;
        while (projectCursor != null) {
            plugin = projectCursor.getPlugin(PluginMojo.PLUGIN_KEY);
            if (plugin != null) {
                // break to keep correct projectCursor
                break;
            }
            projectCursor = projectCursor.getParent();
        }

        if (plugin == null) {
            LOG.debug("plugin definition not found in project or parent projects");
            return Optional.empty();
        }
        if (plugin.getConfiguration() == null) {
            LOG.debug("plugin configuration not found in project or parent projects");
            return Optional.empty();
        }

        try {
            final MojoDescriptor descriptor = mojoDescriptorCreator.getMojoDescriptor(PluginMojo.PLUGIN_KEY + ":" + PluginMojo.GOAL, session, projectCursor);
            LOG.debug("descriptor created " + descriptor);

            descriptor.setConfiguration(getConfig(plugin.getConfiguration()));
            final PlexusConfiguration config = getConfig(plugin.getConfiguration());

            final PluginMojo mojo = new PluginMojo();
            new BasicComponentConfigurator().configureComponent(mojo, config, container.getContainerRealm());

            return Optional.of(mojo);
        } catch (PluginNotFoundException | PluginResolutionException | PluginDescriptorParsingException | MojoNotFoundException
                | NoPluginFoundForPrefixException | InvalidPluginDescriptorException | PluginVersionResolutionException
                | ComponentConfigurationException e) {
            LOG.error("unable to read plugin configuration ", e);
            return Optional.empty();
        }
    }

    private PlexusConfiguration getConfig(final Object config) {
        if (config instanceof PlexusConfiguration) {
            return (PlexusConfiguration) config;
        } else if (config instanceof Xpp3Dom) {
            return new XmlPlexusConfiguration((Xpp3Dom) config);
        }
        throw new IllegalArgumentException("unable to convert config to plexus config: " + config);
    }
}
