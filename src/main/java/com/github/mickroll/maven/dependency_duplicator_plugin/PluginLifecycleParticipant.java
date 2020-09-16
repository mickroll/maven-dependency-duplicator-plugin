package com.github.mickroll.maven.dependency_duplicator_plugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectSorter;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.slf4j.Logger;

@Component(role = AbstractMavenLifecycleParticipant.class)
public class PluginLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    @Requirement
    private Logger logger;

    // needs maven 3.7.0, see https://github.com/apache/maven/pull/368
    // @Requirement(hint = GraphBuilder.HINT)
    // private GraphBuilder graphBuilder;

    @Override
    public void afterProjectsRead(final MavenSession session) throws MavenExecutionException {
        logger.info("duplicating dependencies to projects in reactor");

        if (session.getProjectDependencyGraph() == null) {
            // may happen, if module graph is not valid
            logger.warn("Execution of maven-dependency-duplicator-plugin is not supported in this environment: "
                    + "Current MavenSession does not provide a ProjectDependencyGraph.");
            return;
        }

        final Map<MavenProject, DependencySet> newProjectDependencies = createDuplicateDependenciesForProjects(session);

        addNewDependenciesToProjects(newProjectDependencies);

        logger.info("rebuilding project dependency graph");
        rebuildDependencyGraph(session);

        logger.info("finished.");
    }

    private void rebuildDependencyGraph(final MavenSession session) {
        final ProjectDependencyGraph graph = session.getProjectDependencyGraph();
        final ProjectSorter newSorter;
        try {
            newSorter = new ProjectSorter(graph.getAllProjects());
        } catch (CycleDetectedException | DuplicateProjectException e) {
            logger.error("unable to rebuild project dependency graph", e);
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
                    logger.warn("unable to rebuild project dependency graph, unexpected nested graph implementation found in {}: {}",
                            graph.getClass().getName(), nestedGraph.getClass().getName());
                }
            } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
                logger.error("unable to set rebuilt project dependency graph", e);
            }
        } else {
            logger.warn("unable to rebuild project dependency graph, unexpected graph implementation found: {}", graph.getClass().getName());
        }
    }

    private void replaceProjectSorter(final Object obj, final String fieldName, final ProjectSorter newSorter) {
        try {
            final Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, newSorter);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            logger.error("unable to set rebuilt project dependency graph", e);
        }
    }

    private Map<MavenProject, DependencySet> createDuplicateDependenciesForProjects(final MavenSession session) {
        final Map<MavenProject, DependencySet> newProjectDependencies = new LinkedHashMap<>();
        for (final MavenProject project : session.getAllProjects()) {
            final DuplicatorConfig config = DuplicatorConfig.read(project.getProperties());
            if (config.getDependenciesToMatch().isEmpty()) {
                continue;
            }
            logger.debug("config for {}: {}", project.getName(), config);
            final DependencyCloner dependencyCloner = new DependencyCloner(config);
            final List<Dependency> duplicatedDependencies = new ArrayList<>();
            for (final Dependency existingDependency : project.getDependencies()) {
                final Optional<DependencyMatcher> foundMatcher = config.findAnyMatcher(existingDependency);
                if (!foundMatcher.isPresent()) {
                    continue;
                }

                final Dependency cloned = dependencyCloner.clone(existingDependency);
                logger.debug("[{}] duplicating dependency {} because of {}", project.getName(), getNameForLog(existingDependency), foundMatcher.get());
                duplicatedDependencies.add(cloned);
            }
            if (!duplicatedDependencies.isEmpty()) {
                final List<MavenProject> targetProjectsForNewDependencies = new ArrayList<>();
                targetProjectsForNewDependencies.add(project);
                if (config.isAddDependenciesDownstream()) {
                    targetProjectsForNewDependencies.addAll(session.getProjectDependencyGraph().getDownstreamProjects(project, true));
                }
                for (final MavenProject targetProject : targetProjectsForNewDependencies) {
                    newProjectDependencies.computeIfAbsent(targetProject, p -> new DependencySet()).addAll(duplicatedDependencies);
                }
            }
        }
        return newProjectDependencies;
    }

    private void addNewDependenciesToProjects(final Map<MavenProject, DependencySet> newProjectDependencies) {
        for (final Entry<MavenProject, DependencySet> entry : newProjectDependencies.entrySet()) {
            final MavenProject project = entry.getKey();
            final DependencySet newDependencies = entry.getValue();
            logger.info("[{}] adding duplicated dependencies: {}",
                    project.getName(), newDependencies.asSet().stream().map(this::getNameForLog).collect(Collectors.toList()));
            project.getDependencies().addAll(newDependencies.asSet());
        }
    }

    private String getNameForLog(final Dependency dependency) {
        return dependency.getManagementKey() + ":" + dependency.getScope();
    }
}
