/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.scan.module.model.Artifact;
import com.sonatype.insight.scan.module.model.Dependency;
import com.sonatype.insight.scan.module.model.Module;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.Usage;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.internal.impldep.com.google.common.annotations.VisibleForTesting;

import static org.gradle.api.plugins.JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME;
import static org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME;

/**
 * Finds and resolves dependencies for Gradle projects.
 */
public class DependenciesFinder
{
  private static final String RELEASE_COMPILE_LEGACY_CONFIGURATION_NAME = "_releaseCompile";

  private static final String RELEASE_RUNTIME_APK_LEGACY_CONFIGURATION_NAME = "_releaseApk";

  private static final String RELEASE_RUNTIME_LIBRARY_LEGACY_CONFIGURATION_NAME = "_releasePublish";

  private static final String RELEASE_COMPILE_CONFIGURATION_NAME = "releaseCompileClasspath";

  private static final String RELEASE_RUNTIME_CONFIGURATION_NAME = "releaseRuntimeClasspath";

  private static final Set<String> CONFIGURATION_NAMES = ImmutableSet.of(
      COMPILE_CLASSPATH_CONFIGURATION_NAME,
      RUNTIME_CLASSPATH_CONFIGURATION_NAME,
      RELEASE_COMPILE_LEGACY_CONFIGURATION_NAME,
      RELEASE_RUNTIME_APK_LEGACY_CONFIGURATION_NAME,
      RELEASE_RUNTIME_LIBRARY_LEGACY_CONFIGURATION_NAME,
      RELEASE_COMPILE_CONFIGURATION_NAME,
      RELEASE_RUNTIME_CONFIGURATION_NAME);

  /**
   * Finds resolved dependencies for the given project.
   */
  public Set<ResolvedDependency> findResolvedDependencies(
      Project project,
      boolean allConfigurations,
      Map<String, String> variantAttributes,
      boolean excludeCompileOnlyDependencies)
  {
    addDisambiguationRules(project, variantAttributes);

    Set<String> compileOnlyDependenciesIds =
        excludeCompileOnlyDependencies ? getCompileOnlyDependencyIds(project) : Collections.emptySet();

    return new LinkedHashSet<>(new LinkedHashSet<>(project.getConfigurations()).stream()
        .filter(configuration -> isAcceptableConfiguration(configuration, allConfigurations)).flatMap(configuration -> {
          Stream<ResolvedDependency> dependencies =
              configuration.getResolvedConfiguration().getFirstLevelModuleDependencies().stream();

          if (!compileOnlyDependenciesIds.isEmpty() && shouldRemoveCompileOnlyDependencies(project, configuration)) {
            Set<ResolvedDependency> filteredDependencies =
                dependencies.collect(Collectors.toCollection(LinkedHashSet::new));

            filteredDependencies
                .removeIf(dependency -> compileOnlyDependenciesIds.contains(getResolvedDependencyId(dependency)));
            return filteredDependencies.stream();
          }

          return dependencies;
        }).collect(collectResolvedDependencies()).values());
  }

  /**
   * Finds modules for the given root project and its subprojects.
   */
  public List<Module> findModules(
      Project rootProject,
      boolean allConfigurations,
      Set<String> modulesExcluded,
      Map<String, String> variantAttributes,
      boolean excludeCompileOnlyDependencies)
  {
    List<Module> modules = new ArrayList<>();

    rootProject.allprojects(project -> {

      addDisambiguationRules(project, variantAttributes);

      if (!modulesExcluded.contains(project.getName())) {
        Module module = buildModule(project);

        Set<String> compileOnlyDependenciesIds =
            excludeCompileOnlyDependencies ? getCompileOnlyDependencyIds(project) : Collections.emptySet();

        findResolvedArtifacts(project, allConfigurations, compileOnlyDependenciesIds).stream()
            .map(resolvedArtifact -> new Artifact()
                .setId(getArtifactId(resolvedArtifact))
                .setPathname(resolvedArtifact.getFile())
                .setMonitored(true))
            .forEach(module::addConsumedArtifact);

        findResolvedDependencies(project, allConfigurations, variantAttributes, excludeCompileOnlyDependencies).forEach(
            resolvedDependency -> module.addDependency(processDependency(resolvedDependency, true, new HashSet<>())));

        modules.add(module);
      }
    });

    return modules;
  }

  private void addDisambiguationRules(Project project, Map<String, String> variantAttributes) {
    project.getDependencies().attributesSchema(attributesSchema -> {
      if (isAndroidProject(project)) {
        attributesSchema
            .attribute(Attribute.of("artifactType", String.class))
            .getDisambiguationRules()
            .add(AndroidArtifactTypeAttributeDisambiguationRule.class);
      }

      if (variantAttributes != null) {
        variantAttributes.forEach((attributeName, attributeValue) -> attributesSchema
            .attribute(Attribute.of(attributeName, String.class), strategy -> strategy
                .getDisambiguationRules()
                .add(VariantAttributeDisambiguationRule.class, config -> config.params(attributeValue))));
      }
    });
  }

  @VisibleForTesting
  Set<ResolvedArtifact> findResolvedArtifacts(
      Project project,
      boolean allConfigurations,
      Set<String> compileOnlyDependenciesIds)
  {
    return new LinkedHashSet<>(project.getConfigurations()).stream()
        .filter(configuration -> isAcceptableConfiguration(configuration, allConfigurations)).flatMap(configuration -> {

          Stream<ResolvedArtifact> artifacts = configuration.getResolvedConfiguration().getResolvedArtifacts().stream();

          if (!compileOnlyDependenciesIds.isEmpty() && shouldRemoveCompileOnlyDependencies(project, configuration)) {
            Set<ResolvedArtifact> filteredArtifacts = artifacts.collect(Collectors.toSet());
            Set<String> dependenciesToRemove = new HashSet<>();

            configuration.getResolvedConfiguration().getFirstLevelModuleDependencies().stream()
                .filter(dependency -> compileOnlyDependenciesIds.contains(getResolvedDependencyId(dependency)))
                .forEach(dependency -> fillAllChildDependencies(dependency, dependenciesToRemove));

            filteredArtifacts.removeIf(artifact -> dependenciesToRemove.contains(getArtifactId(artifact)));
            return filteredArtifacts.stream();
          }

          return artifacts;
        }).collect(Collectors.toSet());
  }

  @VisibleForTesting
  Module buildModule(Project project) {
    Module module = new Module()
        .setIdKind("gradle")
        .setPathname(project.getProjectDir())
        .setBuilderInfo(Module.BI_CLM_TOOL, "gradle")
        .setBuilderInfo(Module.BI_CLM_VERSION, PluginVersionUtils.getPluginVersion());

    module.setId(getId(project));
    if (project.getParent() != null) {
      module.setParentId(getId(project.getParent()));
    }

    return module;
  }

  String getId(Project project) {
    StringBuilder idBuilder = new StringBuilder();
    if (StringUtils.isNotBlank(project.getGroup().toString())) {
      idBuilder.append(project.getGroup().toString()).append(":");
    }
    idBuilder.append(project.getName());
    if (StringUtils.isNotBlank(project.getVersion().toString())
        && !"unspecified".equals(project.getVersion().toString())) {
      idBuilder.append(":").append(project.getVersion().toString());
    }
    return idBuilder.toString();
  }

  private String getArtifactId(ResolvedArtifact resolvedArtifact) {
    ModuleVersionIdentifier artifactId = resolvedArtifact.getModuleVersion().getId();
    return artifactId.getGroup() + ":" + artifactId.getName() + ":" + artifactId.getVersion();
  }

  private String getDependencyId(org.gradle.api.artifacts.Dependency dependency) {
    return dependency.getGroup() + ":" + dependency.getName() + ":" + dependency.getVersion();
  }

  private String getResolvedDependencyId(ResolvedDependency resolvedDependency) {
    return resolvedDependency.getModuleGroup() + ":" + resolvedDependency.getModuleName() + ":"
        + resolvedDependency.getModuleVersion();
  }

  @VisibleForTesting
  boolean isAndroidProject(Project project) {
    PluginContainer pluginContainer = project.getPlugins();
    return pluginContainer.hasPlugin("com.android.application") || pluginContainer.hasPlugin("com.android.library");
  }

  @VisibleForTesting
  Dependency processDependency(
      ResolvedDependency resolvedDependency,
      boolean isDirect,
      Set<String> processedDependencies)
  {
    Dependency dependency = new Dependency()
        .setId(resolvedDependency.getName())
        .setDirect(isDirect);

    processedDependencies.add(resolvedDependency.getName());

    resolvedDependency.getChildren().forEach(child -> {
      if (processedDependencies.add(child.getName())) {
        dependency.addDependency(processDependency(child, false, processedDependencies));
      }
      else if (!isParent(resolvedDependency, child, new HashSet<>())) {
        Dependency childDependency = new Dependency()
            .setId(child.getName())
            .setDirect(false);

        dependency.addDependency(childDependency);
      }
    });

    return dependency;
  }

  private boolean isParent(
      ResolvedDependency dependency,
      ResolvedDependency possibleParent,
      Set<String> processedParents)
  {
    Set<ResolvedDependency> parents = dependency.getParents();
    for (ResolvedDependency parent : parents) {
      if (parent.getName().equals(possibleParent.getName())) {
        return true;
      }
      if (processedParents.add(parent.getName())) {
        isParent(parent, possibleParent, processedParents);
      }
    }

    return false;
  }

  private boolean isAcceptableConfiguration(Configuration configuration, boolean allConfigurations) {
    Usage usage = configuration.getAttributes().getAttribute(Usage.USAGE_ATTRIBUTE);
    if (usage != null && !usage.getName().equals(Usage.JAVA_API) && !usage.getName().equals(Usage.JAVA_RUNTIME)) {
      return false;
    }

    if (allConfigurations) {
      return configuration.isCanBeResolved();
    }
    return configuration.isCanBeResolved() && (CONFIGURATION_NAMES.contains(configuration.getName())
        || StringUtils.endsWithIgnoreCase(configuration.getName(), RELEASE_COMPILE_LEGACY_CONFIGURATION_NAME)
        || StringUtils.endsWithIgnoreCase(configuration.getName(), RELEASE_RUNTIME_APK_LEGACY_CONFIGURATION_NAME)
        || StringUtils.endsWithIgnoreCase(configuration.getName(), RELEASE_RUNTIME_LIBRARY_LEGACY_CONFIGURATION_NAME)
        || StringUtils.endsWithIgnoreCase(configuration.getName(), RELEASE_COMPILE_CONFIGURATION_NAME)
        || StringUtils.endsWithIgnoreCase(configuration.getName(), RELEASE_RUNTIME_CONFIGURATION_NAME));
  }

  private Set<String> getCompileOnlyDependencyIds(Project project) {
    Configuration configuration = project.getConfigurations().findByName(COMPILE_ONLY_CONFIGURATION_NAME);

    if (configuration != null) {
      DependencySet dependencies = configuration.getAllDependencies();

      if (dependencies != null) {
        return dependencies.stream().map(this::getDependencyId).collect(Collectors.toSet());
      }
    }

    return Collections.emptySet();
  }

  private boolean shouldRemoveCompileOnlyDependencies(Project project, Configuration configuration) {
    Configuration compileOnlyConfiguration = project.getConfigurations().findByName(COMPILE_ONLY_CONFIGURATION_NAME);
    return compileOnlyConfiguration != null && configuration.getExtendsFrom().contains(compileOnlyConfiguration);
  }

  private void fillAllChildDependencies(ResolvedDependency resolvedDependency, Set<String> dependenciesIds) {
    if (!dependenciesIds.add(getResolvedDependencyId(resolvedDependency))) {
      return;
    }

    if (resolvedDependency.getChildren() != null) {
      for (ResolvedDependency child : resolvedDependency.getChildren()) {
        fillAllChildDependencies(child, dependenciesIds);
      }
    }
  }

  private Collector<ResolvedDependency, ?, LinkedHashMap<String, ResolvedDependency>> collectResolvedDependencies() {
    return Collectors.toMap(ResolvedDependency::getName, Function.identity(), (existing, replacement) -> {
      if (StringUtils.containsAny(replacement.getConfiguration().toLowerCase(Locale.ROOT), "runtime", "release")) {
        return replacement;
      }
      return existing;
    }, LinkedHashMap::new);
  }
}
