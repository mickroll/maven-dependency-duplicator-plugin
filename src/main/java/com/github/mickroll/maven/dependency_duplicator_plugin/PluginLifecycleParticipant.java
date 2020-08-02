package com.github.mickroll.maven.dependency_duplicator_plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private final String PROPERTY_SOURCE_DEPENDENCIES = "ddp.sourceDependencies";
    private final String PROPERTY_TARGET_SCOPE = "ddp.targetScope";
    private final String PROPERTY_TARGET_TYPE = "ddp.targetType";
    private final String PROPERTY_ADD_DEPENDENCIES_DOWNSTREAM = "ddp.addDependenciesDownstream";

    @Override
    public void afterProjectsRead(final MavenSession session) throws MavenExecutionException {
        LOG.info("duplicating dependencies to projects in reactor");

        if (session.getProjectDependencyGraph() == null) {
            // may happen, if module graph is not valid
            LOG.warn("Execution of maven-dependency-duplicator-plugin is not supported in this environment: "
                    + "Current MavenSession does not provide a ProjectDependencyGraph.");
            return;
        }

        for (final MavenProject project : session.getAllProjects()) {
            final DuplicatorConfig config = readConfig(project);
            if (config.getDependenciesToMatch().isEmpty()) {
                continue;
            }
            LOG.debug("config for {}: {}", project.getName(), config);

            final List<Dependency> newDependencies = new ArrayList<>();
            for (final Dependency existingDependency : project.getDependencies()) {
                final Optional<DependencyMatcher> foundMatcher = findAnyMatcher(config, existingDependency);
                if (!foundMatcher.isPresent()) {
                    continue;
                }

                final Dependency cloned = existingDependency.clone();
                config.getTargetType().ifPresent(cloned::setType);
                config.getTargetScope().ifPresent(cloned::setScope);
                LOG.debug("[{}] duplicating dependency {} because of {}",
                        project.getName(), getNameForLog(existingDependency), foundMatcher.get());
                newDependencies.add(cloned);
            }
            if (!newDependencies.isEmpty()) {
                final List<MavenProject> targetProjectsForNewDependencies = new ArrayList<>();
                targetProjectsForNewDependencies.add(project);
                if (config.isAddDependenciesDownstream()) {
                    targetProjectsForNewDependencies.addAll(session.getProjectDependencyGraph().getDownstreamProjects(project, true));
                }

                LOG.info("{} adding duplicated dependencies: {}",
                        targetProjectsForNewDependencies.stream().map(MavenProject::getName).collect(Collectors.toList()),
                        newDependencies.stream().map(this::getNameForLog).collect(Collectors.toList()));

                for (final MavenProject downstreamProject : targetProjectsForNewDependencies) {
                    downstreamProject.getDependencies().addAll(newDependencies);
                }
            }
        }

        LOG.info("finished.");
    }

    private DuplicatorConfig readConfig(final MavenProject project) {
        final String config = project.getProperties().getProperty(PROPERTY_SOURCE_DEPENDENCIES, "");
        final List<DependencyMatcher> dependenciesToMatch = Stream.of(config.split(",")).map(String::trim)
                .filter(element -> !element.isEmpty()).map(DependencyMatcher::new).collect(Collectors.toList());

        final String targetScope = project.getProperties().getProperty(PROPERTY_TARGET_SCOPE);
        final String targetType = project.getProperties().getProperty(PROPERTY_TARGET_TYPE);

        final boolean addDependenciesDownstream =
                Boolean.valueOf(project.getProperties().getProperty(PROPERTY_ADD_DEPENDENCIES_DOWNSTREAM, Boolean.TRUE.toString()));

        return new DuplicatorConfig(dependenciesToMatch, targetScope, targetType, addDependenciesDownstream);
    }

    private Optional<DependencyMatcher> findAnyMatcher(final DuplicatorConfig config, final Dependency dependency) {
        return config.getDependenciesToMatch().stream().filter(matcher -> matcher.matches(dependency)).findFirst();
    }

    private String getNameForLog(final Dependency dependency) {
        return dependency.getManagementKey() + ":" + dependency.getScope();
    }

    public static class DuplicatorConfig {
        private final List<DependencyMatcher> dependenciesToMatch;
        private final String targetScope;
        private final String targetType;
        private final boolean addDependenciesDownstream;

        public DuplicatorConfig(final List<DependencyMatcher> dependenciesToMatch, final String targetScope, final String targetType,
                final boolean addDependenciesDownstream) {
            this.dependenciesToMatch = dependenciesToMatch;
            this.targetScope = targetScope;
            this.targetType = targetType;
            this.addDependenciesDownstream = addDependenciesDownstream;
        }

        public List<DependencyMatcher> getDependenciesToMatch() {
            return dependenciesToMatch;
        }

        public Optional<String> getTargetScope() {
            return Optional.ofNullable(targetScope);
        }

        public Optional<String> getTargetType() {
            return Optional.ofNullable(targetType);
        }

        public boolean isAddDependenciesDownstream() {
            return addDependenciesDownstream;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("dependenciesToMatch=").append(dependenciesToMatch);
            if (targetScope != null) {
                sb.append(", targetScope=").append(targetScope);
            }
            if (targetType != null) {
                sb.append(", targetType=").append(targetType);
            }
            sb.append(", addDependenciesDownstream=").append(addDependenciesDownstream);
            return sb.toString();
        }
    }

    public static class DependencyMatcher {

        private final Pattern dependencyPattern;

        public DependencyMatcher(final String dependencyRegex) {
            this.dependencyPattern = Pattern.compile(dependencyRegex);
        }

        public boolean matches(final Dependency dependency) {
            return dependencyPattern.matcher(dependency.getManagementKey()).matches();
        }

        @Override
        public String toString() {
            return dependencyPattern.pattern();
        }
    }
}
