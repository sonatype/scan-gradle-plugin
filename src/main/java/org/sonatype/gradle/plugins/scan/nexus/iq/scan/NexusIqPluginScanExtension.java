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

/**
 * Configuration extension for the nexusIQScan task.
 */
public class NexusIqPluginScanExtension
{
  /**
   * The default folder name for Sonatype CLM.
   */
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

  /**
   * Constructs a new extension instance with defaults.
   */
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

  /**
   * Returns the username.
   */
  public String getUsername() {
    return username;
  }

  /**
   * Sets the username.
   */
  public void setUsername(String username) {
    this.username = username;
  }


  /**
   * Returns the password.
   */
  public String getPassword() {
    return password;
  }

  /**
   * Sets the password.
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Returns the server URL.
   */
  public String getServerUrl() {
    return serverUrl;
  }

  /**
   * Sets the server URL.
   */
  public void setServerUrl(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  /**
   * Returns the application ID.
   */
  public String getApplicationId() {
    return applicationId;
  }

  /**
   * Sets the application ID.
   */
  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  /**
   * Returns the organization ID.
   */
  public String getOrganizationId() {
    return organizationId;
  }

  /**
   * Sets the organization ID.
   */
  public void setOrganizationId(String organizationId) {
    this.organizationId = organizationId;
  }

  /**
   * Returns the scan folder path.
   */
  public String getScanFolderPath() {
    return scanFolderPath;
  }

  /**
   * Sets the scan folder path.
   */
  public void setScanFolderPath(String scanFolderPath) {
    this.scanFolderPath = scanFolderPath;
  }

  /**
   * Returns the result file path.
   */
  public String getResultFilePath() {
    return resultFilePath;
  }

  /**
   * Sets the result file path.
   */
  public void setResultFilePath(final String resultFilePath) {
    this.resultFilePath = resultFilePath;
  }

  /**
   * Returns the stage.
   */
  public String getStage() {
    return stage;
  }

  /**
   * Sets the stage.
   */
  public void setStage(final String stage) {
    this.stage = stage;
  }

  /**
   * Returns whether all configurations are included.
   */
  public boolean isAllConfigurations() {
    return allConfigurations;
  }

  /**
   * Sets whether all configurations are included.
   */
  public void setAllConfigurations(boolean allConfigurations) {
    this.allConfigurations = allConfigurations;
  }

  /**
   * Returns whether simulation is enabled.
   */
  public boolean isSimulationEnabled() {
    return simulationEnabled;
  }

  /**
   * Sets whether simulation is enabled.
   */
  public void setSimulationEnabled(final boolean simulationEnabled) {
    this.simulationEnabled = simulationEnabled;
  }

  /**
   * Returns the simulated policy action ID.
   */
  public String getSimulatedPolicyActionId() {
    return simulatedPolicyActionId;
  }

  /**
   * Sets the simulated policy action ID.
   */
  public void setSimulatedPolicyActionId(final String simulatedPolicyActionId) {
    this.simulatedPolicyActionId = simulatedPolicyActionId;
  }

  /**
   * Returns the excluded modules.
   */
  public Set<String> getModulesExcluded() {
    return modulesExcluded;
  }

  /**
   * Sets the excluded modules.
   */
  public void setModulesExcluded(Set<String> modulesExcluded) {
    this.modulesExcluded = modulesExcluded;
  }

  /**
   * Returns the directory includes pattern.
   */
  public String getDirIncludes() {
    return dirIncludes;
  }

  /**
   * Sets the directory includes pattern.
   */
  public void setDirIncludes(String dirIncludes) {
    this.dirIncludes = dirIncludes;
  }

  /**
   * Returns the directory excludes pattern.
   */
  public String getDirExcludes() {
    return dirExcludes;
  }

  /**
   * Sets the directory excludes pattern.
   */
  public void setDirExcludes(String dirExcludes) {
    this.dirExcludes = dirExcludes;
  }

  /**
   * Returns the variant attributes.
   */
  public Map<String, String> getVariantAttributes() {
    return variantAttributes;
  }

  /**
   * Sets the variant attributes.
   */
  public void setVariantAttributes(Map<String, String> variantAttributes) {
    this.variantAttributes = variantAttributes;
  }

  /**
   * Returns the scan targets.
   */
  public Set<String> getScanTargets() {
    return scanTargets;
  }

  /**
   * Sets the scan targets.
   */
  public void setScanTargets(Set<String> scanTargets) {
    this.scanTargets = scanTargets;
  }

  /**
   * Returns whether compile-only dependencies are excluded.
   */
  public boolean isExcludeCompileOnly() {
    return excludeCompileOnly;
  }

  /**
   * Sets whether compile-only dependencies are excluded.
   */
  public void setExcludeCompileOnly(boolean excludeCompileOnly) {
    this.excludeCompileOnly = excludeCompileOnly;
  }
}
