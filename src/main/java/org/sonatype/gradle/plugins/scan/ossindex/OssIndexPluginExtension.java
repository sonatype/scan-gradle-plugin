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

  public boolean isUseCache() {
    return useCache;
  }

  public void setUseCache(boolean useCache) {
    this.useCache = useCache;
  }

  public String getCacheDirectory() {
    return cacheDirectory;
  }

  public void setCacheDirectory(String cacheDirectory) {
    this.cacheDirectory = cacheDirectory;
  }

  public String getCacheExpiration() {
    return cacheExpiration;
  }

  public void setCacheExpiration(String cacheExpiration) {
    this.cacheExpiration = cacheExpiration;
  }

  public boolean isAllConfigurations() {
    return allConfigurations;
  }

  public void setAllConfigurations(boolean allConfigurations) {
    this.allConfigurations = allConfigurations;
  }

  public Set<String> getModulesIncluded() {
    return modulesIncluded;
  }

  public void setModulesIncluded(Set<String> modulesIncluded) {
    this.modulesIncluded = modulesIncluded;
  }

  public Set<String> getModulesExcluded() {
    return modulesExcluded;
  }

  public void setModulesExcluded(Set<String> modulesExcluded) {
    this.modulesExcluded = modulesExcluded;
  }

  public boolean isSimulationEnabled() {
    return simulationEnabled;
  }

  public void setSimulationEnabled(boolean simulationEnabled) {
    this.simulationEnabled = simulationEnabled;
  }

  public boolean isSimulatedVulnerabilityFound() {
    return simulatedVulnerabilityFound;
  }

  public void setSimulatedVulnerabilityFound(boolean simulatedVulnerabilityFound) {
    this.simulatedVulnerabilityFound = simulatedVulnerabilityFound;
  }

  public boolean isColorEnabled() {
    return colorEnabled;
  }

  public void setColorEnabled(boolean colorEnabled) {
    this.colorEnabled = colorEnabled;
  }
  public ProxyConfiguration getProxyConfiguration() {
    return proxyConfiguration;
  }

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

  public boolean isShowAll() {
    return showAll;
  }

  public void setShowAll(boolean showAll) {
    this.showAll = showAll;
  }

  public boolean isPrintBanner() {
    return printBanner;
  }

  public void setPrintBanner(boolean printBanner) {
    this.printBanner = printBanner;
  }

  public boolean isFailOnDetection() {
    return failOnDetection;
  }

  public void setFailOnDetection(boolean failOnDetection) {
    this.failOnDetection = failOnDetection;
  }

  public Set<String> getExcludeVulnerabilityIds() {
    return excludeVulnerabilityIds;
  }

  public void setExcludeVulnerabilityIds(Set<String> excludeVulnerabilityIds) {
    this.excludeVulnerabilityIds = excludeVulnerabilityIds;
  }

  public Set<String> getExcludeCoordinates() {
    return excludeCoordinates;
  }

  public void setExcludeCoordinates(Set<String> excludeCoordinates) {
    this.excludeCoordinates = excludeCoordinates;
  }

  public OutputFormat getOutputFormat() {
    return outputFormat;
  }

  public void setOutputFormat(OutputFormat outputFormat) {
    this.outputFormat = outputFormat;
  }

  public Component.Type getCycloneDxComponentType() {
    return cycloneDxComponentType;
  }

  public void setCycloneDxComponentType(Component.Type cycloneDxComponentType) {
    this.cycloneDxComponentType = cycloneDxComponentType;
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
