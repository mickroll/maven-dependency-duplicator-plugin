package com.github.mickroll.maven.dependency_duplicator_plugin;

import java.lang.reflect.Field;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.graph.GraphBuilder;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.ProjectSorter;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helps rebuilding the dependency graph.
 * <p>
 * Will be replaced with a call to {@link GraphBuilder}, as soon as available (needs maven 3.7.0, see https://github.com/apache/maven/pull/368 ).
 *
 * @author mickroll
 */
public class DependencyGraphBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(DependencyGraphBuilder.class);

    public void rebuildDependencyGraph(final MavenSession session) {
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
}
