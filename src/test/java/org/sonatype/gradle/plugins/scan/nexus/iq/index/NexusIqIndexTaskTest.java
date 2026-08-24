/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.index;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

import com.sonatype.insight.scan.module.model.Module;
import com.sonatype.insight.scan.module.model.io.ModuleIoManager;

import org.sonatype.gradle.plugins.scan.common.DependenciesFinder;

import com.google.common.collect.Sets;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.gradle.plugins.scan.nexus.iq.index.NexusIqIndexTask.MODULE_XML_FILE;
import static org.sonatype.gradle.plugins.scan.nexus.iq.scan.NexusIqPluginScanExtension.SONATYPE_CLM_FOLDER;

@RunWith(MockitoJUnitRunner.class)
public class NexusIqIndexTaskTest
{
  @Mock
  private DependenciesFinder dependenciesFinderMock;

  @Mock
  private ModuleIoManager moduleIoManagerMock;

  @Test
  public void testSaveModule_singleModule() throws IOException {
    Module module = new Module()
        .setId("test-module")
        .setPathname("test-module");
    File file = Paths.get("test-module", "build", SONATYPE_CLM_FOLDER, MODULE_XML_FILE).toFile();

    when(dependenciesFinderMock.findModules(any(Project.class), eq(false), anySet(), anyMap(), eq(false)))
        .thenReturn(Collections.singletonList(module));
    doNothing().when(moduleIoManagerMock).writeModule(file, module);

    NexusIqIndexTask task = buildIndexTask(null);
    task.setDependenciesFinder(dependenciesFinderMock);
    task.setModuleIoManager(moduleIoManagerMock);
    task.saveModule();

    verify(dependenciesFinderMock).findModules(any(Project.class), eq(false), anySet(), anyMap(), eq(false));
    verify(moduleIoManagerMock).writeModule(file, module);
  }

  @Test
  public void testSaveModule_multipleModules() throws IOException {
    Module module1 = new Module()
        .setId("test-module-1")
        .setPathname("test-module-1");
    Module module2 = new Module()
        .setId("test-module-2")
        .setPathname("test-module-2");
    File file1 = Paths.get("test-module-1", "build", SONATYPE_CLM_FOLDER, MODULE_XML_FILE).toFile();
    File file2 = Paths.get("test-module-2", "build", SONATYPE_CLM_FOLDER, MODULE_XML_FILE).toFile();

    when(dependenciesFinderMock.findModules(any(Project.class), eq(false), anySet(), anyMap(), eq(false)))
        .thenReturn(Arrays.asList(module1, module2));
    doNothing().when(moduleIoManagerMock).writeModule(file1, module1);
    doNothing().when(moduleIoManagerMock).writeModule(file2, module2);

    NexusIqIndexTask task = buildIndexTask(null);
    task.setDependenciesFinder(dependenciesFinderMock);
    task.setModuleIoManager(moduleIoManagerMock);
    task.saveModule();

    verify(dependenciesFinderMock).findModules(any(Project.class), eq(false), anySet(), anyMap(), eq(false));
    verify(moduleIoManagerMock).writeModule(file1, module1);
    verify(moduleIoManagerMock).writeModule(file2, module2);
  }

  @Test
  public void testSaveModule_excludeModules() throws IOException {
    Set<String> modulesExcluded = Sets.newHashSet("test-module-1", "test-module-2");

    when(dependenciesFinderMock.findModules(any(Project.class), eq(false), eq(modulesExcluded), anyMap(), eq(false)))
        .thenReturn(Collections.emptyList());

    NexusIqIndexTask task = buildIndexTask(extension -> extension.setModulesExcluded(modulesExcluded));
    task.setDependenciesFinder(dependenciesFinderMock);
    task.saveModule();

    verify(dependenciesFinderMock).findModules(any(Project.class), eq(false), eq(modulesExcluded), anyMap(), eq(false));
  }

  @Test
  public void testSaveModule_excludeCompileOnly() throws IOException {
    when(dependenciesFinderMock.findModules(any(Project.class), anyBoolean(), anySet(), anyMap(), eq(true)))
        .thenReturn(Collections.emptyList());

    NexusIqIndexTask task = buildIndexTask(extension -> extension.setExcludeCompileOnly(true));
    task.setDependenciesFinder(dependenciesFinderMock);
    task.saveModule();

    verify(dependenciesFinderMock).findModules(any(Project.class), anyBoolean(), anySet(), anyMap(), eq(true));
  }

  private NexusIqIndexTask buildIndexTask(Consumer<NexusIqPluginIndexExtension> extenstionConsumer) {
    Project project = ProjectBuilder.builder().build();
    NexusIqPluginIndexExtension extension = new NexusIqPluginIndexExtension(project);
    if (extenstionConsumer != null) {
      extenstionConsumer.accept(extension);
    }
    project.getExtensions().add("nexusIQIndex", extension);
    return project.getTasks().create("nexusIQIndex", NexusIqIndexTask.class);
  }
}
