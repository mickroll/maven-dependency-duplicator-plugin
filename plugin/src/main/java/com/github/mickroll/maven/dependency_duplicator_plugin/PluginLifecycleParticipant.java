package com.github.mickroll.maven.dependency_duplicator_plugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectSorter;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.configurator.BasicComponentConfigurator;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mickroll.maven.dependency_duplicator_plugin.config.DependencyDuplication;
import com.github.mickroll.maven.dependency_duplicator_plugin.config.PluginMojo;

@Component(role = AbstractMavenLifecycleParticipant.class)
public class PluginLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private static final Logger LOG = LoggerFactory.getLogger(PluginLifecycleParticipant.class);

    @Inject
    private PlexusContainer container;

    @Inject
    private MojoDescriptorCreator mojoDescriptorCreator;

    // needs maven 3.7.0, see https://github.com/apache/maven/pull/368
    // @Inject
    // private GraphBuilder graphBuilder;

    @Override
    public void afterProjectsRead(final MavenSession session) throws MavenExecutionException {
        LOG.info("duplicating dependencies to projects in reactor");

        final long startTime = System.currentTimeMillis();

        if (session.getProjectDependencyGraph() == null) {
            LOG.warn("Current MavenSession does not provide a ProjectDependencyGraph.");
            return;
        }

        final Map<MavenProject, DependencySet> newProjectDependencies = createDuplicateDependenciesForProjects(session);

        addNewDependenciesToProjects(newProjectDependencies);

        LOG.info("rebuilding project dependency graph");
        rebuildDependencyGraph(session);

        LOG.info("finished after {}ms.", System.currentTimeMillis() - startTime);
    }

    private Optional<PluginMojo> findPluginConfig(final MavenSession session, final MavenProject project) {
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

    private void rebuildDependencyGraph(final MavenSession session) {
        final ProjectDependencyGraph graph = session.getProjectDependencyGraph();
        final ProjectSorter newSorter;
        try {
            newSorter = new ProjectSorter(graph.getAllProjects());
        } catch (CycleDetectedException | DuplicateProjectException e) {
            LOG.error("unable to rebuild project dependency graph", e);
            return;
        }

        if ("org.apache.maven.graph.DefaultProjectDependencyGraph".equals(graph.getClass().getName())) {
            replaceProjectSorter(graph, "sorter", newSorter);
        } else if ("org.apache.maven.graph.FilteredProjectDependencyGraph".equals(graph.getClass().getName())) {
            try {
                final Field field = graph.getClass().getDeclaredField("projectDependencyGraph");
                field.setAccessible(true);
                final Object nestedGraph = field.get(graph);
                if ("org.apache.maven.graph.DefaultProjectDependencyGraph".equals(nestedGraph.getClass().getName())) {
                    replaceProjectSorter(nestedGraph, "sorter", newSorter);
                } else {
                    LOG.warn(
                            "unable to rebuild project dependency graph, unexpected nested graph implementation found in {}: {}",
                            graph.getClass().getName(), nestedGraph.getClass().getName());
                }
            } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
                LOG.error("unable to set rebuilt project dependency graph", e);
            }
        } else {
            LOG.warn("unable to rebuild project dependency graph, unexpected graph implementation found: {}", graph.getClass().getName());
        }
    }

    private void replaceProjectSorter(final Object obj, final String fieldName, final ProjectSorter newSorter) {
        try {
            final Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, newSorter);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            LOG.error("unable to set rebuilt project dependency graph", e);
        }
    }

    private Map<MavenProject, DependencySet> createDuplicateDependenciesForProjects(final MavenSession session) {
        final Map<MavenProject, DependencySet> newProjectDependencies = new LinkedHashMap<>();
        for (final MavenProject project : session.getAllProjects()) {
            final Optional<PluginMojo> pluginConfig = findPluginConfig(session, project);
            if (!pluginConfig.isPresent()) {
                LOG.info("[{}] did not find any plugin config", project.getName());
                continue;
            }
            final PluginMojo config = pluginConfig.get();
            LOG.debug("config for {}: {}", project.getName(), config);
            if (!config.hasDefinedDuplications()) {
                continue;
            }
            for (final Dependency existingDependency : project.getDependencies()) {
                final Optional<DependencyDuplication> foundDuplicationHolder = config.findFirstDuplicationConfig(existingDependency);
                if (!foundDuplicationHolder.isPresent()) {
                    continue;
                }
                final DependencyDuplication dependencyDuplication = foundDuplicationHolder.get();

                final List<Dependency> newDependencies = new ArrayList<>();
                newDependencies.add(dependencyDuplication.doDuplicate(existingDependency));
                LOG.debug("[{}] duplicating dependency {} because of {}",
                        project.getName(), getNameForLog(existingDependency), dependencyDuplication.getSource());

                if (!dependencyDuplication.getAdditionalDependencies().isEmpty()) {
                    LOG.debug("[{}] adding additional dependencies {}", getNamesForLog(dependencyDuplication.getAdditionalDependencies()));
                    newDependencies.addAll(dependencyDuplication.getAdditionalDependencies());
                }

                final List<MavenProject> targetProjectsForNewDependencies = new ArrayList<>();
                targetProjectsForNewDependencies.add(project);
                if (dependencyDuplication.isAddDownstream()) {
                    targetProjectsForNewDependencies.addAll(session.getProjectDependencyGraph().getDownstreamProjects(project, true));
                }
                for (final MavenProject targetProject : targetProjectsForNewDependencies) {
                    newProjectDependencies.computeIfAbsent(targetProject, p -> new DependencySet()).addAll(newDependencies);
                }
            }
        }
        return newProjectDependencies;
    }

    private void addNewDependenciesToProjects(final Map<MavenProject, DependencySet> newProjectDependencies) {
        for (final Entry<MavenProject, DependencySet> entry : newProjectDependencies.entrySet()) {
            final MavenProject project = entry.getKey();
            final DependencySet newDependencies = entry.getValue();
            LOG.info("[{}] adding dependencies: {}", project.getName(), getNamesForLog(newDependencies.asSet()));
            project.getDependencies().addAll(newDependencies.asSet());
        }
    }

    private String getNameForLog(final Dependency dependency) {
        return dependency.getManagementKey() + ":" + dependency.getScope();
    }

    private Collection<String> getNamesForLog(final Collection<Dependency> dependencies) {
        return dependencies.stream().map(this::getNameForLog).collect(Collectors.toList());
    }
}
