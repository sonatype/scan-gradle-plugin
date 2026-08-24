/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.common;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.scan.module.model.Artifact;
import com.sonatype.insight.scan.module.model.Dependency;
import com.sonatype.insight.scan.module.model.Module;

import com.google.common.collect.Sets;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.Usage;
import org.gradle.api.model.ObjectFactory;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Test;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME;
import static org.gradle.api.plugins.JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME;
import static org.gradle.api.plugins.JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME;
import static org.gradle.api.plugins.JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DependenciesFinderTest
{
  private static final String COMMONS_COLLECTIONS_DEPENDENCY = "commons-collections:commons-collections:3.1";

  private DependenciesFinder finder;

  @Before
  public void setup() {
    finder = new DependenciesFinder();
  }

  @Test
  public void testFindResolvedDependencies_includeCompileDependencies() {
    Project project = buildProject(IMPLEMENTATION_CONFIGURATION_NAME, false);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap(), false);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeRuntimeDependencies() {
    Project project = buildProject(RUNTIME_ONLY_CONFIGURATION_NAME, false);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap(), false);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeLegacyCompileAndroidDependencies() {
    Project project = buildProject("_releaseCompile", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap(), false);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeLegacyRuntimeApkAndroidDependencies() {
    Project project = buildProject("_releaseApk", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap(), false);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeLegacyRuntimeLibraryAndroidDependencies() {
    Project project = buildProject("_releasePublish", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap(), false);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeCompileAndroidDependencies() {
    Project project = buildProject("releaseCompileClasspath", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap(), false);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeRuntimeAndroidDependencies() {
    Project project = buildProject("releaseRuntimeClasspath", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap(), false);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeLegacyCompileApkAndroidDependenciesUsingVariant() {
    Project project = buildProject("variantProd_ReleaseCompile", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap(), false);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeLegacyRuntimeApkAndroidDependenciesUsingVariant() {
    Project project = buildProject("variantProd_ReleaseApk", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap(), false);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeLegacyRuntimeLibraryAndroidDependenciesUsingVariant() {
    Project project = buildProject("variantProd_ReleasePublish", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap(), false);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeAndroidCompileDependenciesUsingVariant() {
    Project project = buildProject("variantProdReleaseCompileClasspath", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap(), false);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_includeAndroidRuntimeDependenciesUsingVariant() {
    Project project = buildProject("variantProdReleaseRuntimeClasspath", true);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap(), false);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_omitTestDependencies() {
    Project project = buildProject(TEST_IMPLEMENTATION_CONFIGURATION_NAME, false);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap(), false);
    assertThat(result).isEmpty();
  }

  @Test
  public void testFindResolvedDependencies_includeTestDependencies() {
    Project project = buildProject(TEST_IMPLEMENTATION_CONFIGURATION_NAME, false);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, true, emptyMap(), false);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedDependencies_multiModuleProject() {
    Project parentProject = ProjectBuilder.builder().withName("parent").build();
    Project childProject = buildProject(IMPLEMENTATION_CONFIGURATION_NAME, false, parentProject);

    assertThat(finder.findResolvedDependencies(childProject, false, emptyMap(), false)).hasSize(1);
    assertThat(finder.findResolvedDependencies(parentProject, false, emptyMap(), false)).isEmpty();
  }

  @Test
  public void testFindResolvedDependencies_excludeNonJavaConfiguration() {
    Project project = buildProject("implementationDependenciesMetadata", true);

    project.getConfigurations().getByName("implementationDependenciesMetadata").attributes(attributeContainer -> {
      ObjectFactory factory = project.getObjects();
      attributeContainer.attribute(Usage.USAGE_ATTRIBUTE, factory.named(Usage.class, Usage.NATIVE_LINK));
    });

    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, true, emptyMap(), false);
    assertThat(result).isEmpty();
  }

  @Test
  public void testFindResolvedDependencies_excludeCompileOnlyDependencies() {
    Project project = buildProject(COMPILE_ONLY_CONFIGURATION_NAME, false);
    Set<ResolvedDependency> result = finder.findResolvedDependencies(project, false, emptyMap(), true);
    assertThat(result).isEmpty();
  }

  @Test
  public void testFindResolvedArtifacts_includeCompileDependencies() {
    Project project = buildProject(IMPLEMENTATION_CONFIGURATION_NAME, false);
    Set<ResolvedArtifact> result = finder.findResolvedArtifacts(project, false, emptySet());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedArtifacts_includeRuntimeDependencies() {
    Project project = buildProject(RUNTIME_ONLY_CONFIGURATION_NAME, false);
    Set<ResolvedArtifact> result = finder.findResolvedArtifacts(project, false, emptySet());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedArtifacts_includeAndroidCompileDependencies() {
    Project project = buildProject("releaseCompileClasspath", true);
    Set<ResolvedArtifact> result = finder.findResolvedArtifacts(project, false, emptySet());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedArtifacts_includeAndroidRuntimeDependencies() {
    Project project = buildProject("releaseRuntimeClasspath", true);
    Set<ResolvedArtifact> result = finder.findResolvedArtifacts(project, false, emptySet());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedArtifacts_omitTestDependencies() {
    Project project = buildProject(TEST_IMPLEMENTATION_CONFIGURATION_NAME, false);
    Set<ResolvedArtifact> result = finder.findResolvedArtifacts(project, false, emptySet());
    assertThat(result).isEmpty();
  }

  @Test
  public void testFindResolvedArtifacts_includeTestDependencies() {
    Project project = buildProject(TEST_IMPLEMENTATION_CONFIGURATION_NAME, false);
    Set<ResolvedArtifact> result = finder.findResolvedArtifacts(project, true, emptySet());
    assertThat(result).hasSize(1);
  }

  @Test
  public void testFindResolvedArtifacts_omitCompileOnlyDependencies() {
    Project project = buildProject(COMPILE_ONLY_CONFIGURATION_NAME, false);
    Set<ResolvedArtifact> result =
        finder.findResolvedArtifacts(project, false, Sets.newHashSet(COMMONS_COLLECTIONS_DEPENDENCY));
    assertThat(result).isEmpty();
  }

  @Test
  public void testBuildModule_withBasicInfo() {
    Project project = buildProject(IMPLEMENTATION_CONFIGURATION_NAME, false);
    Module module = finder.buildModule(project);

    assertThat(module).isNotNull();
    assertThat(module.getIdKind()).isEqualTo("gradle");
    assertThat(module.getBuilderInfo(Module.BI_CLM_TOOL)).isEqualTo("gradle");
    assertThat(module.getBuilderInfo(Module.BI_CLM_VERSION)).isEqualTo(PluginVersionUtils.getPluginVersion());
    assertThat(module.getPathname()).isEqualTo(project.getProjectDir().getAbsolutePath());
    assertThat(module.getId()).isEqualTo(project.getName());
  }

  @Test
  public void testBuildModule_withGroup() {
    Project project = buildProject(IMPLEMENTATION_CONFIGURATION_NAME, false);
    project.setGroup("groupTest");
    Module module = finder.buildModule(project);

    assertThat(module).isNotNull();
    assertThat(module.getId()).isEqualTo(project.getGroup() + ":" + project.getName());
  }

  @Test
  public void testBuildModule_withGroupAndVersion() {
    Project project = buildProject(IMPLEMENTATION_CONFIGURATION_NAME, false);
    project.setGroup("groupTest");
    project.setVersion("0.0.1");
    Module module = finder.buildModule(project);

    assertThat(module).isNotNull();
    assertThat(module.getId()).isEqualTo(project.getGroup() + ":" + project.getName() + ":" + project.getVersion());
  }

  @Test
  public void testFindModules_singleModule() {
    Project project = buildProject(IMPLEMENTATION_CONFIGURATION_NAME, false);
    List<Module> modules = finder.findModules(project, false, emptySet(), emptyMap(), false);

    assertThat(modules).hasSize(1);

    Module module = modules.get(0);
    assertThat(module.getId()).isEqualTo(project.getName());
    assertThat(module.getConsumedArtifacts()).hasSize(1);
    assertThat(module.getDependencies()).hasSize(1);

    Dependency dependency = module.getDependencies().get(0);
    assertThat(dependency.getId()).isEqualTo(COMMONS_COLLECTIONS_DEPENDENCY);
    assertThat(dependency.isDirect()).isTrue();

    Artifact artifact = module.getConsumedArtifacts().get(0);
    assertThat(artifact.getId()).isEqualTo(COMMONS_COLLECTIONS_DEPENDENCY);
  }

  @Test
  public void testFindModules_multiModule() {
    Project parentProject = ProjectBuilder.builder().withName("parent").build();
    Project childProject = buildProject(IMPLEMENTATION_CONFIGURATION_NAME, false, parentProject);
    List<Module> modules = finder.findModules(parentProject, false, emptySet(), emptyMap(), false);

    assertThat(modules).hasSize(2);

    Module parentModule = modules.get(0);
    assertThat(parentModule.getId()).isEqualTo(parentProject.getName());
    assertThat(parentModule.getConsumedArtifacts()).isEmpty();
    assertThat(parentModule.getParentId()).isNull();

    Module childModule = modules.get(1);
    assertThat(childModule.getId()).isEqualTo(parentProject.getName() + ":" + childProject.getName());
    assertThat(childModule.getConsumedArtifacts()).hasSize(1);
    assertThat(childModule.getDependencies()).hasSize(1);
    assertThat(childModule.getParentId()).isEqualTo(parentProject.getName());

    Dependency dependency = childModule.getDependencies().get(0);
    assertThat(dependency.getId()).isEqualTo(COMMONS_COLLECTIONS_DEPENDENCY);
    assertThat(dependency.isDirect()).isTrue();

    Artifact artifact = childModule.getConsumedArtifacts().get(0);
    assertThat(artifact.getId()).isEqualTo(COMMONS_COLLECTIONS_DEPENDENCY);
  }

  @Test
  public void testFindModules_multiModuleWithModuleExcluded() {
    Project parentProject = ProjectBuilder.builder().withName("parent").build();
    Project childProject = buildProject(IMPLEMENTATION_CONFIGURATION_NAME, false, parentProject);
    List<Module> modules =
        finder.findModules(parentProject, false, Collections.singleton(childProject.getName()), emptyMap(), false);

    assertThat(modules).hasSize(1);
    assertThat(modules.get(0).getId()).isEqualTo(parentProject.getName());
  }

  @Test
  public void testFindModules_excludeCompileOnlyDependencies() {
    Project parentProject = ProjectBuilder.builder().withName("parent").build();
    Project childProject = buildProject(COMPILE_ONLY_CONFIGURATION_NAME, false, parentProject);
    List<Module> modules =
        finder.findModules(parentProject, false, Collections.singleton(childProject.getName()), emptyMap(), false);

    assertThat(modules).hasSize(1);
    assertThat(modules.get(0).getId()).isEqualTo(parentProject.getName());
  }

  @Test
  public void testProcessDependency() {
    testProcessDependency(false);
  }

  @Test
  public void testProcessDependency_avoidCircularDependenciesStackOverflowError() {
    testProcessDependency(true);
  }

  private void testProcessDependency(boolean setupCircularDependencies) {
    ResolvedDependency parentDependency = mock(ResolvedDependency.class);
    when(parentDependency.getName()).thenReturn("g:a:v");

    ResolvedDependency singleChildDependency = mock(ResolvedDependency.class);
    when(singleChildDependency.getName()).thenReturn("g2:a2:v2");

    ResolvedDependency multiChildDependency = mock(ResolvedDependency.class);
    when(multiChildDependency.getName()).thenReturn("g3:a3:v3");

    ResolvedDependency subChildDependency = mock(ResolvedDependency.class);
    when(subChildDependency.getName()).thenReturn("g4:a4:v4");

    when(multiChildDependency.getChildren()).thenReturn(Set.of(subChildDependency));
    when(subChildDependency.getParents()).thenReturn(Set.of(multiChildDependency));

    if (setupCircularDependencies) {
      when(singleChildDependency.getChildren()).thenReturn(Set.of(parentDependency));
      when(parentDependency.getParents()).thenReturn(Set.of(singleChildDependency));
    }

    when(parentDependency.getChildren()).thenReturn(Set.of(singleChildDependency, multiChildDependency));
    when(singleChildDependency.getParents()).thenReturn(Set.of(parentDependency));
    when(multiChildDependency.getParents()).thenReturn(Set.of(parentDependency));

    Dependency dependency = finder.processDependency(parentDependency, true, new HashSet<>());
    assertThat(dependency).isNotNull();
    assertThat(dependency.isDirect()).isTrue();
    assertThat(dependency.getId()).isEqualTo("g:a:v");
    assertThat(dependency.getDependencies()).hasSize(2);

    Collections.sort(dependency.getDependencies(), Comparator.comparing(Dependency::getId));

    Dependency child1 = dependency.getDependencies().get(0);
    assertThat(child1.isDirect()).isFalse();
    assertThat(child1.getId()).isEqualTo("g2:a2:v2");
    assertThat(child1.getDependencies()).isEmpty();

    Dependency child2 = dependency.getDependencies().get(1);
    assertThat(child2.isDirect()).isFalse();
    assertThat(child2.getId()).isEqualTo("g3:a3:v3");
    assertThat(child2.getDependencies()).hasSize(1);

    Dependency subChild2 = child2.getDependencies().get(0);
    assertThat(subChild2.isDirect()).isFalse();
    assertThat(subChild2.getId()).isEqualTo("g4:a4:v4");
    assertThat(subChild2.getDependencies()).isEmpty();
  }

  private Project buildProject(String configurationName, boolean needToCreateConfiguration) {
    return buildProject(configurationName, needToCreateConfiguration, null);
  }

  private Project buildProject(String configurationName, boolean needToCreateConfiguration, Project parent) {
    ProjectBuilder projectBuilder = ProjectBuilder.builder();
    if (parent != null) {
      projectBuilder.withParent(parent);
    }
    Project project = projectBuilder.build();

    project.getPluginManager().apply("java");
    project.getRepositories().mavenCentral();
    if (needToCreateConfiguration) {
      project.getConfigurations().create(configurationName);
    }
    DependencyHandler dependencyHandler = project.getDependencies();
    dependencyHandler.add(configurationName, COMMONS_COLLECTIONS_DEPENDENCY);
    return project;
  }
}
