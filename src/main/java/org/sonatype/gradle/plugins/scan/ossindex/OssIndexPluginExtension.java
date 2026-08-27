/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.ossindex;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sonatype.ossindex.service.client.transport.AuthConfiguration;
import org.sonatype.ossindex.service.client.transport.ProxyConfiguration;

import groovy.lang.Closure;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.model.Component;
import org.gradle.api.Project;

/**
 * Extension for configuring the OSS Index audit task.
 */
public class OssIndexPluginExtension
{
  private String username;

  private String password;

  private boolean useCache;

  private String cacheDirectory;

  /**
   * It must follow the duration format from <a href=
   * "https://www.javadoc.io/doc/joda-time/joda-time/2.10.4/org/joda/time/Duration.html#parse-java.lang.String-">
   * Duration</a>
   */
  private String cacheExpiration;

  private boolean allConfigurations;

  private Set<String> modulesIncluded;

  private Set<String> modulesExcluded;

  private boolean simulationEnabled;

  private boolean simulatedVulnerabilityFound;

  private boolean colorEnabled;

  private ProxyConfiguration proxyConfiguration;

  private boolean showAll;

  private boolean printBanner;

  private boolean failOnDetection;

  private Set<String> excludeVulnerabilityIds;

  private Set<String> excludeCoordinates;

  private OutputFormat outputFormat;

  private Component.Type cycloneDxComponentType;

  private Map<String, String> variantAttributes;

  private boolean excludeCompileOnly;

  /**
   * Creates a new extension instance.
   */
  public OssIndexPluginExtension(Project project) {
    username = "";
    password = "";
    useCache = true;
    cacheDirectory = "";
    cacheExpiration = "";
    simulationEnabled = false;
    simulatedVulnerabilityFound = false;
    colorEnabled = true;
    showAll = false;
    printBanner = true;
    failOnDetection = true;
    excludeVulnerabilityIds = new HashSet<>();
    excludeCoordinates = new HashSet<>();
    outputFormat = OutputFormat.DEFAULT;
    cycloneDxComponentType = Component.Type.LIBRARY;
    variantAttributes = Collections.emptyMap();
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
   * Returns whether the cache is enabled.
   */
  public boolean isUseCache() {
    return useCache;
  }

  /**
   * Sets whether the cache is enabled.
   */
  public void setUseCache(boolean useCache) {
    this.useCache = useCache;
  }

  /**
   * Returns the cache directory.
   */
  public String getCacheDirectory() {
    return cacheDirectory;
  }

  /**
   * Sets the cache directory.
   */
  public void setCacheDirectory(String cacheDirectory) {
    this.cacheDirectory = cacheDirectory;
  }

  /**
   * Returns the cache expiration.
   */
  public String getCacheExpiration() {
    return cacheExpiration;
  }

  /**
   * Sets the cache expiration.
   */
  public void setCacheExpiration(String cacheExpiration) {
    this.cacheExpiration = cacheExpiration;
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
   * Returns the modules included.
   */
  public Set<String> getModulesIncluded() {
    return modulesIncluded;
  }

  /**
   * Sets the modules included.
   */
  public void setModulesIncluded(Set<String> modulesIncluded) {
    this.modulesIncluded = modulesIncluded;
  }

  /**
   * Returns the modules excluded.
   */
  public Set<String> getModulesExcluded() {
    return modulesExcluded;
  }

  /**
   * Sets the modules excluded.
   */
  public void setModulesExcluded(Set<String> modulesExcluded) {
    this.modulesExcluded = modulesExcluded;
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
  public void setSimulationEnabled(boolean simulationEnabled) {
    this.simulationEnabled = simulationEnabled;
  }

  /**
   * Returns whether a simulated vulnerability is found.
   */
  public boolean isSimulatedVulnerabilityFound() {
    return simulatedVulnerabilityFound;
  }

  /**
   * Sets whether a simulated vulnerability is found.
   */
  public void setSimulatedVulnerabilityFound(boolean simulatedVulnerabilityFound) {
    this.simulatedVulnerabilityFound = simulatedVulnerabilityFound;
  }

  /**
   * Returns whether color output is enabled.
   */
  public boolean isColorEnabled() {
    return colorEnabled;
  }

  /**
   * Sets whether color output is enabled.
   */
  public void setColorEnabled(boolean colorEnabled) {
    this.colorEnabled = colorEnabled;
  }

  /**
   * Returns the proxy configuration.
   */
  public ProxyConfiguration getProxyConfiguration() {
    return proxyConfiguration;
  }

  /**
   * Sets the proxy configuration.
   */
  public void setProxyConfiguration(Closure<ProxyConfiguration> closure) {
    proxyConfiguration = new ProxyConfiguration();
    AuthConfiguration authConfiguration = new AuthConfiguration();
    proxyConfiguration.setAuthConfiguration(authConfiguration);

    closure.setResolveStrategy(Closure.DELEGATE_FIRST);
    closure.setDelegate(proxyConfiguration);
    closure.call();

    if (StringUtils.isAllBlank(authConfiguration.getUsername(), authConfiguration.getPassword())) {
      proxyConfiguration.setAuthConfiguration(null);
    }
  }

  /**
   * Returns whether to show all dependencies.
   */
  public boolean isShowAll() {
    return showAll;
  }

  /**
   * Sets whether to show all dependencies.
   */
  public void setShowAll(boolean showAll) {
    this.showAll = showAll;
  }

  /**
   * Returns whether to print the banner.
   */
  public boolean isPrintBanner() {
    return printBanner;
  }

  /**
   * Sets whether to print the banner.
   */
  public void setPrintBanner(boolean printBanner) {
    this.printBanner = printBanner;
  }

  /**
   * Returns whether to fail on vulnerability detection.
   */
  public boolean isFailOnDetection() {
    return failOnDetection;
  }

  /**
   * Sets whether to fail on vulnerability detection.
   */
  public void setFailOnDetection(boolean failOnDetection) {
    this.failOnDetection = failOnDetection;
  }

  /**
   * Returns the vulnerability IDs to exclude.
   */
  public Set<String> getExcludeVulnerabilityIds() {
    return excludeVulnerabilityIds;
  }

  /**
   * Sets the vulnerability IDs to exclude.
   */
  public void setExcludeVulnerabilityIds(Set<String> excludeVulnerabilityIds) {
    this.excludeVulnerabilityIds = excludeVulnerabilityIds;
  }

  /**
   * Returns the coordinates to exclude.
   */
  public Set<String> getExcludeCoordinates() {
    return excludeCoordinates;
  }

  /**
   * Sets the coordinates to exclude.
   */
  public void setExcludeCoordinates(Set<String> excludeCoordinates) {
    this.excludeCoordinates = excludeCoordinates;
  }

  /**
   * Returns the output format.
   */
  public OutputFormat getOutputFormat() {
    return outputFormat;
  }

  /**
   * Sets the output format.
   */
  public void setOutputFormat(OutputFormat outputFormat) {
    this.outputFormat = outputFormat;
  }

  /**
   * Returns the CycloneDX component type.
   */
  public Component.Type getCycloneDxComponentType() {
    return cycloneDxComponentType;
  }

  /**
   * Sets the CycloneDX component type.
   */
  public void setCycloneDxComponentType(Component.Type cycloneDxComponentType) {
    this.cycloneDxComponentType = cycloneDxComponentType;
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
   * Returns whether to exclude compile-only dependencies.
   */
  public boolean isExcludeCompileOnly() {
    return excludeCompileOnly;
  }

  /**
   * Sets whether to exclude compile-only dependencies.
   */
  public void setExcludeCompileOnly(boolean excludeCompileOnly) {
    this.excludeCompileOnly = excludeCompileOnly;
  }
}
