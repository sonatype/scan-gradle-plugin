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

public class NexusIqPluginIndexExtension
{
  private boolean allConfigurations;

  private Set<String> modulesExcluded;

  private Map<String, String> variantAttributes;

  private boolean excludeCompileOnly;

  public NexusIqPluginIndexExtension(Project project) {
    modulesExcluded = Collections.emptySet();
    variantAttributes = Collections.emptyMap();
  }

  public boolean isAllConfigurations() {
    return allConfigurations;
  }

  public void setAllConfigurations(boolean allConfigurations) {
    this.allConfigurations = allConfigurations;
  }

  public Set<String> getModulesExcluded() {
    return modulesExcluded;
  }

  public void setModulesExcluded(Set<String> modulesExcluded) {
    this.modulesExcluded = modulesExcluded;
  }

  public Map<String, String> getVariantAttributes() {
    return variantAttributes;
  }

  public void setVariantAttributes(Map<String, String> variantAttributes) {
    this.variantAttributes = variantAttributes;
  }

  public boolean isExcludeCompileOnly() {
    return excludeCompileOnly;
  }

  public void setExcludeCompileOnly(boolean excludeCompileOnly) {
    this.excludeCompileOnly = excludeCompileOnly;
  }
}
