/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.ossindex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.sonatype.goodies.packageurl.PackageUrl;
import org.sonatype.ossindex.service.api.componentreport.ComponentReport;
import org.sonatype.ossindex.service.api.componentreport.ComponentReportVulnerability;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.artifacts.ResolvedDependency;

/**
 * Default response handler that logs vulnerability information in plain text format.
 */
public class DefaultResponseHandler
    implements OssIndexResponseHandler
{
  private final OssIndexPluginExtension extension;

  /**
   * Constructs a handler with the given extension configuration.
   */
  public DefaultResponseHandler(OssIndexPluginExtension extension) {
    this.extension = extension;
  }

  @Override
  public boolean handleOssIndexResponse(
      final Set<ResolvedDependency> dependencies,
      final Map<ResolvedDependency, PackageUrl> dependenciesMap,
      final Map<PackageUrl, ComponentReport> response)
  {
    boolean hasVulnerabilities = false;
    int index = 1;
    int dependenciesCount = dependenciesMap.size();

    if (!extension.isShowAll()) {
      dependenciesCount = (int) dependenciesMap.values().parallelStream()
          .filter(packageUrl -> !response.get(packageUrl).getVulnerabilities().isEmpty())
          .count();

      if (dependenciesCount == 0) {
        log.info("No vulnerabilities found!");
      }
      else {
        log.info("Found vulnerabilities in {} dependencies", dependenciesCount);
      }
    }

    for (Entry<ResolvedDependency, PackageUrl> entry : dependenciesMap.entrySet()) {
      PackageUrl packageUrl = entry.getValue();
      ComponentReport componentReport = response.get(packageUrl);

      List<ComponentReportVulnerability> vulnerabilities = getSortedVulnerabilities(componentReport);
      if (!vulnerabilities.isEmpty() || extension.isShowAll()) {
        log.info(getProcessingPackageUrlString(packageUrl, vulnerabilities, index++, dependenciesCount));
        for (ComponentReportVulnerability vulnerability : vulnerabilities) {
          log.info(getVulnerabilityDetailsString(vulnerability));
        }
      }

      boolean vulnerable = !vulnerabilities.isEmpty();
      if (vulnerable) {
        hasVulnerabilities = true;
      }
    }

    return hasVulnerabilities;
  }

  private List<ComponentReportVulnerability> getSortedVulnerabilities(ComponentReport componentReport) {
    List<ComponentReportVulnerability> vulnerabilities = new ArrayList<>();
    if (!Objects.isNull(componentReport)) {
      vulnerabilities = componentReport.getVulnerabilities();
      vulnerabilities.sort(Comparator.comparing(ComponentReportVulnerability::getCvssScore).reversed());

    }
    return vulnerabilities;
  }

  private String getProcessingPackageUrlString(
      PackageUrl packageUrl,
      List<ComponentReportVulnerability> vulnerabilities,
      int index,
      int totalComponents)
  {
    String packageUrlProcessingText = "[" + index + "/" + totalComponents + "] - " + packageUrl + " - ";

    if (vulnerabilities.isEmpty()) {
      packageUrlProcessingText += "No vulnerabilities found!";
    }
    else if (vulnerabilities.size() == 1) {
      packageUrlProcessingText += "1 vulnerability found!";
    }
    else {
      packageUrlProcessingText += vulnerabilities.size() + " vulnerabilities found!";
    }

    if (extension.isColorEnabled()) {
      if (vulnerabilities.isEmpty()) {
        packageUrlProcessingText =
            VulnerabilityUtils.addColor(VulnerabilityUtils.ASCII_COLOR_GREEN, packageUrlProcessingText);
      }
      else {
        Float maxCvssScore = vulnerabilities.stream()
            .map(ComponentReportVulnerability::getCvssScore)
            .max(Comparator.naturalOrder())
            .orElse(0F);

        packageUrlProcessingText =
            VulnerabilityUtils.addColorBasedOnCvssScore(maxCvssScore, packageUrlProcessingText);
      }
    }

    return packageUrlProcessingText;
  }

  private String getVulnerabilityDetailsString(ComponentReportVulnerability vulnerability) {
    Float cvssScore = vulnerability.getCvssScore();

    return new StringBuilder()
        .append(System.lineSeparator())
        .append(addColour(cvssScore, "   Vulnerability Title:  ")).append(vulnerability.getTitle())
        .append(System.lineSeparator())
        .append(addColour(cvssScore, "   ID:  ")).append(vulnerability.getId())
        .append(System.lineSeparator())
        .append(addColour(cvssScore, "   Description:  "))
        .append(StringUtils.abbreviate(Objects.toString(vulnerability.getDescription(),"").replaceAll("\n", " "), 140))
        .append(System.lineSeparator())
        .append(addColour(cvssScore, "   CVSS Score:  ")).append("(").append(vulnerability.getCvssScore()).append("/10")
        .append(", ").append(VulnerabilityUtils.getAssessment(cvssScore)).append(")")
        .append(System.lineSeparator())
        .append(addColour(cvssScore, "   CVSS Vector:  "))
        .append(Objects.toString(vulnerability.getCvssVector(), "Unspecified"))
        .append(System.lineSeparator())
        .append(addColour(cvssScore, "   CVE:  ")).append(Objects.toString(vulnerability.getCve(), "Unspecified"))
        .append(System.lineSeparator())

        .append(addColour(cvssScore, "   Reference:  ")).append(vulnerability.getReference())
        .append(System.lineSeparator()).toString();
  }

  private String addColour(Float cvssScore, String text) {
    if (extension.isColorEnabled()) {
      return VulnerabilityUtils.addColorBasedOnCvssScore(cvssScore, text);
    }
    else {
      return text;
    }
  }
}
