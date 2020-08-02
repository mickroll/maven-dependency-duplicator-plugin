package com.github.mickroll.maven.dependency_duplicator_plugin;

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
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(role = AbstractMavenLifecycleParticipant.class)
public class PluginLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private static final Logger LOG = LoggerFactory.getLogger(PluginLifecycleParticipant.class);

    @Override
    public void afterProjectsRead(final MavenSession session) throws MavenExecutionException {
        LOG.info("duplicating dependencies to projects in reactor");

        if (session.getProjectDependencyGraph() == null) {
            // may happen, if module graph is not valid
            LOG.warn("Execution of maven-dependency-duplicator-plugin is not supported in this environment: "
                    + "Current MavenSession does not provide a ProjectDependencyGraph.");
            return;
        }

        final Map<MavenProject, DependencySet> newProjectDependencies = createDuplicateDependenciesForProjects(session);

        addNewDependenciesToProjects(newProjectDependencies);

        LOG.info("finished.");
    }

    private Map<MavenProject, DependencySet> createDuplicateDependenciesForProjects(final MavenSession session) {
        final Map<MavenProject, DependencySet> newProjectDependencies = new LinkedHashMap<>();
        for (final MavenProject project : session.getAllProjects()) {
            final DuplicatorConfig config = DuplicatorConfig.read(project);
            if (config.getDependenciesToMatch().isEmpty()) {
                continue;
            }
            LOG.debug("config for {}: {}", project.getName(), config);

            final List<Dependency> duplicatedDependencies = new ArrayList<>();
            for (final Dependency existingDependency : project.getDependencies()) {
                final Optional<DependencyMatcher> foundMatcher = config.findAnyMatcher(existingDependency);
                if (!foundMatcher.isPresent()) {
                    continue;
                }

                final Dependency cloned = existingDependency.clone();
                config.getTargetType().ifPresent(cloned::setType);
                config.getTargetScope().ifPresent(cloned::setScope);
                LOG.debug("[{}] duplicating dependency {} because of {}", project.getName(), getNameForLog(existingDependency), foundMatcher.get());
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
            LOG.info("[{}] adding duplicated dependencies: {}",
                    project.getName(), newDependencies.asSet().stream().map(this::getNameForLog).collect(Collectors.toList()));
            project.getDependencies().addAll(newDependencies.asSet());
        }
    }

    private String getNameForLog(final Dependency dependency) {
        return dependency.getManagementKey() + ":" + dependency.getScope();
    }
}
