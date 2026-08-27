/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.index;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.scan.module.model.Module;
import com.sonatype.insight.scan.module.model.io.ModuleIoManager;

import org.sonatype.gradle.plugins.scan.common.DependenciesFinder;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.impldep.com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonatype.gradle.plugins.scan.nexus.iq.scan.NexusIqPluginScanExtension.SONATYPE_CLM_FOLDER;

/**
 * Task for saving module information to module.xml files.
 */
public class NexusIqIndexTask
    extends DefaultTask
{
  /**
   * The name of the module XML file.
   */
  public static final String MODULE_XML_FILE = "module.xml";

  private final Logger log = LoggerFactory.getLogger(NexusIqIndexTask.class);

  private final NexusIqPluginIndexExtension extension;

  private DependenciesFinder dependenciesFinder;

  private ModuleIoManager moduleIoManager;

  /**
   * Constructs a NexusIqIndexTask.
   */
  public NexusIqIndexTask() {
    extension = getProject().getExtensions().getByType(NexusIqPluginIndexExtension.class);
    dependenciesFinder = new DependenciesFinder();
    moduleIoManager = new ModuleIoManager(log);
  }

  /**
   * Saves module information to module.xml files.
   */
  @TaskAction
  public void saveModule() {
    try {
      List<Module> modules = dependenciesFinder.findModules(getProject(), extension.isAllConfigurations(),
          extension.getModulesExcluded(), extension.getVariantAttributes(), extension.isExcludeCompileOnly());
      List<File> files = new ArrayList<>(modules.size());

      for (Module module : modules) {
        File file = Paths.get(module.getPathname(), "build", SONATYPE_CLM_FOLDER, MODULE_XML_FILE).toFile();
        moduleIoManager.writeModule(file, module);
        files.add(file);
      }

      log.info("Saved module information to {}", StringUtils.join(files, ", "));
    }
    catch (Exception e) {
      throw new GradleException("Could not save the module information for the project: " + e.getMessage(), e);
    }
  }

  @VisibleForTesting
  void setDependenciesFinder(DependenciesFinder dependenciesFinder) {
    this.dependenciesFinder = dependenciesFinder;
  }

  @VisibleForTesting
  void setModuleIoManager(ModuleIoManager moduleIoManager) {
    this.moduleIoManager = moduleIoManager;
  }

  /**
   * Returns whether all configurations should be included.
   */
  @Input
  public boolean isAllConfigurations() {
    return extension.isAllConfigurations();
  }

  /**
   * Returns the set of excluded module names.
   */
  @Input
  public Set<String> getModulesExcluded() {
    return extension.getModulesExcluded();
  }
}
