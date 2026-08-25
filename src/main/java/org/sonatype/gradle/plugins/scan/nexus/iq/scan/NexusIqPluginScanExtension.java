/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.scan;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.client.PolicyAction;

import org.gradle.api.Project;

public class NexusIqPluginScanExtension
{
  public static final String SONATYPE_CLM_FOLDER = "sonatype-clm";

  private String stage;

  private String scanFolderPath;

  private String resultFilePath;

  private String username;

  private String password;

  private String serverUrl;

  private String applicationId;

  private String organizationId;

  private boolean allConfigurations;

  private boolean simulationEnabled;

  private String simulatedPolicyActionId;

  private Set<String> modulesExcluded;

  private String dirIncludes;

  private String dirExcludes;

  private Map<String, String> variantAttributes;

  private Set<String> scanTargets;

  private boolean excludeCompileOnly;

  public NexusIqPluginScanExtension(Project project) {
    stage = Stage.ID_BUILD;
    organizationId = "";
    simulationEnabled = false;
    simulatedPolicyActionId = PolicyAction.NONE.toString();
    scanFolderPath = project.getRootDir().getAbsolutePath();
    modulesExcluded = Collections.emptySet();
    dirIncludes = "";
    dirExcludes = "";
    variantAttributes = Collections.emptyMap();
    scanTargets = Collections.emptySet();
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }


  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public void setServerUrl(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(String organizationId) {
    this.organizationId = organizationId;
  }

  public String getScanFolderPath() {
    return scanFolderPath;
  }

  public void setScanFolderPath(String scanFolderPath) {
    this.scanFolderPath = scanFolderPath;
  }

  public String getResultFilePath() {
    return resultFilePath;
  }

  public void setResultFilePath(final String resultFilePath) {
    this.resultFilePath = resultFilePath;
  }

  public String getStage() {
    return stage;
  }

  public void setStage(final String stage) {
    this.stage = stage;
  }

  public boolean isAllConfigurations() {
    return allConfigurations;
  }

  public void setAllConfigurations(boolean allConfigurations) {
    this.allConfigurations = allConfigurations;
  }

  public boolean isSimulationEnabled() {
    return simulationEnabled;
  }

  public void setSimulationEnabled(final boolean simulationEnabled) {
    this.simulationEnabled = simulationEnabled;
  }

  public String getSimulatedPolicyActionId() {
    return simulatedPolicyActionId;
  }

  public void setSimulatedPolicyActionId(final String simulatedPolicyActionId) {
    this.simulatedPolicyActionId = simulatedPolicyActionId;
  }

  public Set<String> getModulesExcluded() {
    return modulesExcluded;
  }

  public void setModulesExcluded(Set<String> modulesExcluded) {
    this.modulesExcluded = modulesExcluded;
  }

  public String getDirIncludes() {
    return dirIncludes;
  }

  public void setDirIncludes(String dirIncludes) {
    this.dirIncludes = dirIncludes;
  }

  public String getDirExcludes() {
    return dirExcludes;
  }

  public void setDirExcludes(String dirExcludes) {
    this.dirExcludes = dirExcludes;
  }

  public Map<String, String> getVariantAttributes() {
    return variantAttributes;
  }

  public void setVariantAttributes(Map<String, String> variantAttributes) {
    this.variantAttributes = variantAttributes;
  }

  public Set<String> getScanTargets() {
    return scanTargets;
  }

  public void setScanTargets(Set<String> scanTargets) {
    this.scanTargets = scanTargets;
  }

  public boolean isExcludeCompileOnly() {
    return excludeCompileOnly;
  }

  public void setExcludeCompileOnly(boolean excludeCompileOnly) {
    this.excludeCompileOnly = excludeCompileOnly;
  }
}
