/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.ossindex;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.sonatype.goodies.packageurl.PackageUrl;
import org.sonatype.goodies.packageurl.PackageUrlBuilder;
import org.sonatype.gradle.plugins.scan.common.DependenciesFinder;
import org.sonatype.ossindex.service.api.componentreport.ComponentReport;
import org.sonatype.ossindex.service.api.componentreport.ComponentReportVulnerability;
import org.sonatype.ossindex.service.client.OssindexClient;
import org.sonatype.ossindex.service.client.OssindexClientConfiguration;
import org.sonatype.ossindex.service.client.internal.OssindexClientImpl;
import org.sonatype.ossindex.service.client.marshal.GsonMarshaller;
import org.sonatype.ossindex.service.client.marshal.Marshaller;
import org.sonatype.ossindex.service.client.transport.Transport;
import org.sonatype.ossindex.service.client.transport.Transport.TransportException;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import org.cyclonedx.model.Component;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.impldep.com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OssIndexAuditTask
    extends DefaultTask
{
  private static Logger log = LoggerFactory.getLogger(OssIndexResponseHandler.class);

  private final OssIndexPluginExtension extension;

  private final DependenciesFinder dependenciesFinder;

  public OssIndexAuditTask() {
    extension = getProject().getExtensions().getByType(OssIndexPluginExtension.class);
    dependenciesFinder = new DependenciesFinder();
  }

  @TaskAction
  public void audit() {
    boolean hasVulnerabilities;

    try (OssindexClient ossIndexClient = buildOssIndexClient()) {
      Set<ResolvedDependency> dependencies = getProject().getAllprojects().stream()
          .filter(project -> extension.getModulesIncluded() == null || extension.getModulesIncluded().isEmpty() || extension.getModulesIncluded().contains(project.getName()))
          .filter(project -> extension.getModulesExcluded() == null || !extension.getModulesExcluded().contains(project.getName()))
          .flatMap(project -> dependenciesFinder
              .findResolvedDependencies(project, extension.isAllConfigurations(), extension.getVariantAttributes(),
                  extension.isExcludeCompileOnly())
              .stream())
          .collect(Collectors.toCollection(LinkedHashSet::new));
      BiMap<ResolvedDependency, PackageUrl> dependenciesMap = HashBiMap.create();

      dependencies.forEach(dependency -> buildDependenciesMap(dependency, dependenciesMap));

      List<PackageUrl> packageUrls = new ArrayList<>(dependenciesMap.values());

      Map<PackageUrl, ComponentReport> response;

      if (extension.isPrintBanner()) {
        log.info(BannerUtils.createBanner());
      }

      log.info("Checking vulnerabilities in {} dependencies", dependenciesMap.size());

      if (extension.isSimulationEnabled()) {
        response = buildSimulatedResponse(packageUrls);
      }
      else {
        response = ossIndexClient.requestComponentReports(packageUrls);
      }

      Set<String> vulnerabilityIdsToExclude = extension.getExcludeVulnerabilityIds();
      Set<PackageUrl> coordinatesToExclude = toPackageUrls(extension.getExcludeCoordinates());
      VulnerabilityExclusionFilter vulnerabilityExclusionFilter =
          new VulnerabilityExclusionFilter(vulnerabilityIdsToExclude, coordinatesToExclude);
      vulnerabilityExclusionFilter.apply(response);

      OssIndexResponseHandler responseHandler = buildResponseHandler();
      hasVulnerabilities = responseHandler.handleOssIndexResponse(dependencies, dependenciesMap, response);
    }
    catch (TransportException e) {
      throw new GradleException("Connection to OSS Index failed, check your credentials: " + e.getMessage(), e);
    }
    catch (UnknownHostException e) {
      throw new GradleException("Connection to OSS Index failed, check your internet status: " + e.getMessage(), e);
    }
    catch (Exception e) {
      throw new GradleException("Could not audit the project: " + e.getMessage(), e);
    }

    if (hasVulnerabilities && extension.isFailOnDetection()) {
      throw new GradleException("Vulnerabilities detected, check log output to review them");
    }
  }

  @VisibleForTesting
  OssindexClient buildOssIndexClient() {
    OssindexClientConfiguration clientConfiguration = new OssIndexClientConfigurationBuilder().build(extension);
    Transport transport = new TransportBuilder().build(getProject());
    Marshaller marshaller = new GsonMarshaller();

    return new OssindexClientImpl(clientConfiguration, transport, marshaller);
  }

  private Map<PackageUrl, ComponentReport> buildSimulatedResponse(
      List<PackageUrl> packageUrls) throws URISyntaxException
  {
    Map<PackageUrl, ComponentReport> map = new HashMap<>();

    for (PackageUrl packageUrl : packageUrls) {
      ComponentReport report = new ComponentReport();
      report.setCoordinates(packageUrl);

      if (extension.isSimulatedVulnerabilityFound()) {
        ComponentReportVulnerability vulnerability = new ComponentReportVulnerability();
        vulnerability.setId("123-456-789");
        vulnerability.setTitle("Simulated");
        vulnerability.setCvssScore(4f);
        vulnerability.setReference(new URI("http://test/123"));
        report.setVulnerabilities(Lists.newArrayList(vulnerability));
      }

      map.put(packageUrl, report);
    }

    return map;
  }

  @VisibleForTesting
  void buildDependenciesMap(ResolvedDependency dependency, BiMap<ResolvedDependency, PackageUrl> mapAccumulator) {
    mapAccumulator.forcePut(dependency, toPackageUrl(dependency));

    dependency.getChildren().forEach(child -> {
      if (mapAccumulator.forcePut(child, toPackageUrl(child)) == null) {
        buildDependenciesMap(child, mapAccumulator);
      }
    });
  }

  private Set<PackageUrl> toPackageUrls(Set<String> coordinates) {
    Set<PackageUrl> packageUrls = new HashSet<>();

    for (String coordinate : coordinates) {
      String[] sections = coordinate.split(":");
      if (sections.length != 3) {
        continue;
      }
      String group = sections[0];
      String name = sections[1];
      String version = sections[2];
      packageUrls.add(toPackageUrl(group, name, version));
    }

    return packageUrls;
  }

  private PackageUrl toPackageUrl(ResolvedDependency dependency) {
    ModuleVersionIdentifier id = dependency.getModule().getId();
    return toPackageUrl(id.getGroup(), id.getName(), id.getVersion());
  }

  private PackageUrl toPackageUrl(String namespace, String name, String version) {
    return new PackageUrlBuilder()
        .type("maven")
        .namespace(namespace)
        .name(name)
        .version(version)
        .build();
  }

  @VisibleForTesting
  OssIndexResponseHandler buildResponseHandler() {
    switch (extension.getOutputFormat()) {
      case DEPENDENCY_GRAPH:
        return new DependencyGraphResponseHandler(extension);
      case JSON_CYCLONE_DX_1_4:
        return new CycloneDxResponseHandler(extension, getProject());
      default:
        return new DefaultResponseHandler(extension);
    }
  }

  @Input
  public String getUsername() {
    return extension.getUsername();
  }

  @Input
  public String getPassword() {
    return extension.getPassword();
  }

  @Input
  public boolean isUseCache() {
    return extension.isUseCache();
  }

  @Input
  public String getCacheDirectory() {
    return extension.getCacheDirectory();
  }

  @Input
  public String getCacheExpiration() {
    return extension.getCacheExpiration();
  }

  @Input
  public boolean isAllConfigurations() {
    return extension.isAllConfigurations();
  }

  @Input
  public boolean isColorEnabled() {
    return extension.isColorEnabled();
  }

  @Input
  public boolean isShowAll() {
    return extension.isShowAll();
  }

  @Input
  public boolean isPrintBanner() {
    return extension.isPrintBanner();
  }

  @Input
  @Optional
  public Set<String> getModulesIncluded() {
    return extension.getModulesIncluded();
  }

  @Input
  @Optional
  public Set<String> getModulesExcluded() {
    return extension.getModulesExcluded();
  }

  @Input
  @Optional
  public OutputFormat getOutputFormat() {
    return extension.getOutputFormat();
  }

  @Input
  @Optional
  public Component.Type getCycloneDxComponentType() {
    return extension.getCycloneDxComponentType();
  }
}
