package com.github.mickroll.maven.dependency_duplicator_plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
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

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        LOG.info("duplicating dependencies to projects in reactor");
        for (MavenProject project : session.getAllProjects()) {
            DuplicatorConfig config = readConfig(project);
            if (config.getDependenciesToMatch().isEmpty()) {
                continue;
            }
            LOG.debug("config for {}: {}", project.getName(), config);

            List<Dependency> newDependencies = new ArrayList<>();
            for (Dependency existingDependency : project.getDependencies()) {
                Optional<DependencyMatcher> foundMatcher = findAnyMatcher(config, existingDependency);
                if (!foundMatcher.isPresent()) {
                    continue;
                }
                
                Dependency cloned = existingDependency.clone();
                config.getTargetType().ifPresent(cloned::setType);
                config.getTargetScope().ifPresent(cloned::setScope);
                LOG.debug("[{}] adding dependency {} because of {}", project.getName(), cloned.getManagementKey(), foundMatcher.get());
                newDependencies.add(cloned);
            }
            if (!newDependencies.isEmpty()) {
                LOG.info("[{}] adding dependencies {}", project.getName(), newDependencies.stream().map(Dependency::getManagementKey).collect(Collectors.toList()));
                project.getDependencies().addAll(newDependencies);
            }
        }
        LOG.info("finished.");
    }

    private DuplicatorConfig readConfig(MavenProject project) {
        String config = project.getProperties().getProperty(PROPERTY_SOURCE_DEPENDENCIES, "");
        List<DependencyMatcher> dependenciesToMatch = Stream.of(config.split(","))
                .map(String::trim)
                .filter(element -> !element.isEmpty())
                .map(DependencyMatcher::new)
                .collect(Collectors.toList());

        String targetScope = project.getProperties().getProperty(PROPERTY_TARGET_SCOPE);
        String targetType = project.getProperties().getProperty(PROPERTY_TARGET_TYPE);

        return new DuplicatorConfig(dependenciesToMatch, targetScope, targetType);
    }
    
    private Optional<DependencyMatcher> findAnyMatcher(DuplicatorConfig config, Dependency dependency) {
        return config.getDependenciesToMatch().stream()
            .filter(matcher -> matcher.matches(dependency))
            .findFirst();
    }

    public static class DuplicatorConfig {
        private final List<DependencyMatcher> dependenciesToMatch;
        private final String targetScope;
        private final String targetType;

        public DuplicatorConfig(List<DependencyMatcher> dependenciesToMatch, String targetScope, String targetType) {
            this.dependenciesToMatch = dependenciesToMatch;
            this.targetScope = targetScope;
            this.targetType = targetType;
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

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("dependenciesToMatch=").append(dependenciesToMatch);
            if (targetScope != null) {
                sb.append(", targetScope=").append(targetScope);
            }
            if (targetType != null) {
                sb.append(", targetType=").append(targetType);
            }
            return sb.toString();
        }
    }

    public static class DependencyMatcher {

        private Pattern dependencyPattern;

        public DependencyMatcher(String dependencyRegex) {
            this.dependencyPattern = Pattern.compile(dependencyRegex);
        }

        public boolean matches(Dependency dependency) {
            return dependencyPattern.matcher(dependency.getManagementKey()).matches();
        }

        @Override
        public String toString() {
            return dependencyPattern.pattern();
        }
    }
}
