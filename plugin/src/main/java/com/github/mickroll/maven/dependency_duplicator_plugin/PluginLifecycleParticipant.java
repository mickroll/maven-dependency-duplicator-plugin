package com.github.mickroll.maven.dependency_duplicator_plugin;

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
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mickroll.maven.dependency_duplicator_plugin.config.DependencyDuplication;
import com.github.mickroll.maven.dependency_duplicator_plugin.config.PluginMojo;

@Component(role = AbstractMavenLifecycleParticipant.class)
public class PluginLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private static final Logger LOG = LoggerFactory.getLogger(PluginLifecycleParticipant.class);

    @Inject
    private PluginConfigResolver pluginConfigResolver;

    @Inject
    private DependencyGraphBuilder dependencyGraphBuilder;

    // TODO #3: use GraphBuilder, as soon as available (needs maven 3.7.0, see https://github.com/apache/maven/pull/368 )
    // @Inject
    // private GraphBuilder graphBuilder;

    @Override
    public void afterProjectsRead(final MavenSession session) throws MavenExecutionException {
        LOG.info("duplicating dependencies to projects in reactor");

        final long startTime = System.currentTimeMillis();

        if (session.getProjectDependencyGraph() == null) {
            LOG.warn("Current MavenSession does not provide a ProjectDependencyGraph.");
            // TODO #3: build graph using GraphBuilder
            return;
        }

        final Map<MavenProject, DependencySet> newProjectDependencies = createDuplicateDependenciesForProjects(session);

        addNewDependenciesToProjects(newProjectDependencies);

        LOG.info("rebuilding project dependency graph");
        dependencyGraphBuilder.rebuildDependencyGraph(session);
        // TODO #3: build graph using GraphBuilder, as soon as available (needs maven 3.7.0, see https://github.com/apache/maven/pull/368 )

        LOG.info("finished after {}ms.", System.currentTimeMillis() - startTime);
    }

    private Map<MavenProject, DependencySet> createDuplicateDependenciesForProjects(final MavenSession session) {
        final Map<MavenProject, DependencySet> newProjectDependencies = new LinkedHashMap<>();
        for (final MavenProject project : session.getAllProjects()) {
            final Optional<PluginMojo> pluginConfig = pluginConfigResolver.findPluginConfig(session, project);
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
                        project.getName(), getNameForLog(existingDependency), dependencyDuplication.getdependencyKeys());

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
