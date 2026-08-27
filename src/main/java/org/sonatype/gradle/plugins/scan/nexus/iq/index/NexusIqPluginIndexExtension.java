/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.index;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Project;

/**
 * Extension for configuring the nexusIQIndex task.
 */
public class NexusIqPluginIndexExtension
{
  private boolean allConfigurations;

  private Set<String> modulesExcluded;

  private Map<String, String> variantAttributes;

  private boolean excludeCompileOnly;

  /**
   * Constructs an extension for the given project.
   */
  public NexusIqPluginIndexExtension(Project project) {
    modulesExcluded = Collections.emptySet();
    variantAttributes = Collections.emptyMap();
  }

  /**
   * Returns whether all configurations should be included.
   */
  public boolean isAllConfigurations() {
    return allConfigurations;
  }

  /**
   * Sets whether all configurations should be included.
   */
  public void setAllConfigurations(boolean allConfigurations) {
    this.allConfigurations = allConfigurations;
  }

  /**
   * Returns the set of excluded module names.
   */
  public Set<String> getModulesExcluded() {
    return modulesExcluded;
  }

  /**
   * Sets the modules to exclude.
   */
  public void setModulesExcluded(Set<String> modulesExcluded) {
    this.modulesExcluded = modulesExcluded;
  }

  /**
   * Returns the variant attributes for dependency resolution.
   */
  public Map<String, String> getVariantAttributes() {
    return variantAttributes;
  }

  /**
   * Sets the variant attributes for dependency resolution.
   */
  public void setVariantAttributes(Map<String, String> variantAttributes) {
    this.variantAttributes = variantAttributes;
  }

  /**
   * Returns whether compile-only dependencies should be excluded.
   */
  public boolean isExcludeCompileOnly() {
    return excludeCompileOnly;
  }

  /**
   * Sets whether compile-only dependencies should be excluded.
   */
  public void setExcludeCompileOnly(boolean excludeCompileOnly) {
    this.excludeCompileOnly = excludeCompileOnly;
  }
}
