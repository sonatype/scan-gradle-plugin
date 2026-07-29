/*
 * Copyright (c) 2020-present Sonatype, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sonatype.gradle.plugins.scan.ossindex;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.sonatype.goodies.packageurl.PackageUrl;
import org.sonatype.gradle.plugins.scan.common.PluginVersionUtils;
import org.sonatype.ossindex.service.api.componentreport.ComponentReport;
import org.sonatype.ossindex.service.api.componentreport.ComponentReportVulnerability;

import com.google.common.base.CharMatcher;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.Version;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Tool;
import org.cyclonedx.model.vulnerability.Vulnerability;
import org.cyclonedx.model.vulnerability.Vulnerability.Advisory;
import org.cyclonedx.model.vulnerability.Vulnerability.Affect;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Severity;
import org.cyclonedx.model.vulnerability.Vulnerability.Source;
import org.gradle.api.Project;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.artifacts.ResolvedDependency;

public class CycloneDxResponseHandler
    implements OssIndexResponseHandler
{
  public static final String FILE_NAME_OUTPUT = "oss-index-cyclonedx-bom.json";

  private final OssIndexPluginExtension extension;

  private final Project project;

  public CycloneDxResponseHandler(OssIndexPluginExtension extension, Project project) {
    this.extension = extension;
    this.project = project;
  }

  @Override
  public boolean handleOssIndexResponse(
      Set<ResolvedDependency> dependencies,
      Map<ResolvedDependency, PackageUrl> dependenciesMap,
      Map<PackageUrl, ComponentReport> response)
  {
    int dependenciesCount = dependenciesMap.size();

    if (!extension.isShowAll()) {
      dependenciesCount = (int) dependenciesMap.values().parallelStream()
          .filter(packageUrl -> !response.get(packageUrl).getVulnerabilities().isEmpty())
          .count();

      if (dependenciesCount == 0) {
        log.info("No vulnerabilities found!");
        return false;
      }
      else {
        log.info("Found vulnerabilities in {} dependencies", dependenciesCount);
      }
    }

    Bom bom = buildBom();

    Map<String, Vulnerability> vulnerabilitiesById = new HashMap<>();

    for (Entry<ResolvedDependency, PackageUrl> entry : dependenciesMap.entrySet()) {
      PackageUrl packageUrl = entry.getValue();
      ComponentReport componentReport = response.get(packageUrl);

      if (componentReport != null) {
        List<ComponentReportVulnerability> componentVulnerabilities = componentReport.getVulnerabilities();

        if (!componentVulnerabilities.isEmpty() || extension.isShowAll()) {
          Component component = buildComponent(packageUrl);

          for (ComponentReportVulnerability componentVulnerability : componentVulnerabilities) {
            Vulnerability vulnerability = vulnerabilitiesById.get(componentVulnerability.getId());
            if (vulnerability == null) {
              vulnerability = new Vulnerability();
              vulnerability.setId(componentVulnerability.getId());

              addSource(componentVulnerability, vulnerability);

              addAdvisories(componentVulnerability, vulnerability);

              addRating(componentVulnerability, vulnerability);

              addCwe(componentVulnerability, vulnerability);

              vulnerability.setDescription(componentVulnerability.getDescription());

              addToolDetails(vulnerability);
              vulnerabilitiesById.put(componentVulnerability.getId(), vulnerability);
            }

            addAffectedVersionRanges(component, componentVulnerability, vulnerability);
          }

          bom.addComponent(component);
        }
      }
    }

    if (!vulnerabilitiesById.isEmpty()) {
      bom.setVulnerabilities(new ArrayList<>(vulnerabilitiesById.values()));
    }

    generateFile(bom);

    return !vulnerabilitiesById.isEmpty();
  }

  private Bom buildBom() {
    Bom bom = new Bom();
    bom.setSerialNumber("urn:uuid:" + UUID.randomUUID().toString());

    Tool tool = new Tool();
    tool.setVendor("Sonatype");
    tool.setName("Scan Gradle Plugin (aka Sherlock Trunks)");
    tool.setVersion(PluginVersionUtils.getPluginVersion());

    Metadata metadata = new Metadata();
    metadata.addTool(tool);
    metadata.setTimestamp(new Date());

    Component component = new Component();
    component.setGroup(Objects.toString(project.getGroup()));
    component.setName(project.getName());
    component.setVersion(Objects.toString(project.getVersion()));
    component.setType(extension.getCycloneDxComponentType());
    metadata.setComponent(component);

    bom.setMetadata(metadata);
    return bom;
  }

  private Component buildComponent(PackageUrl packageUrl) {
    Component component = new Component();
    component.setType(Component.Type.LIBRARY);
    component.setGroup(packageUrl.getNamespaceAsString());
    component.setName(packageUrl.getName());
    component.setVersion(packageUrl.getVersion());
    component.setPurl(packageUrl.toString());
    component.setBomRef(packageUrl.toString());
    return component;
  }

  private void addSource(ComponentReportVulnerability componentVulnerability, Vulnerability vulnerability) {
    Source source = new Source();
    source.setName("OSS Index");
    source.setUrl(componentVulnerability.getReference().toString());
    vulnerability.setSource(source);
  }

  private void addAdvisories(ComponentReportVulnerability componentVulnerability, Vulnerability vulnerability) {
    List<Advisory> advisories = new ArrayList<>();

    componentVulnerability.getExternalReferences().forEach(externalReference -> {
      Advisory advisory = new Advisory();
      advisory.setUrl(externalReference.toString());
      advisories.add(advisory);
    });

    vulnerability.setAdvisories(advisories);
  }

  private void addRating(ComponentReportVulnerability componentVulnerability, Vulnerability vulnerability) {
    Rating rating = new Rating();
    rating.setScore(Double.valueOf(componentVulnerability.getCvssScore()));
    rating.setSeverity(Severity
        .fromString(VulnerabilityUtils.getAssessment(componentVulnerability.getCvssScore()).toLowerCase(Locale.ROOT)));
    rating.setVector(Objects.toString(componentVulnerability.getCvssVector(), "Unspecified"));
    vulnerability.addRating(rating);
  }

  private void addCwe(ComponentReportVulnerability componentVulnerability, Vulnerability vulnerability) {
    String cwe = CharMatcher.inRange('0', '9').precomputed()
        .retainFrom(StringUtils.defaultIfBlank(componentVulnerability.getCwe(), ""));
    if (NumberUtils.isDigits(cwe)) {
      vulnerability.addCwe(Integer.parseInt(cwe));
    }
  }

  private void addToolDetails(Vulnerability vulnerability) {
    Tool tool = new Tool();
    tool.setVendor("Sonatype");
    tool.setName("OSS Index");
    vulnerability.setTools(Collections.singletonList(tool));
  }

  private void addAffectedVersionRanges(
      Component component,
      ComponentReportVulnerability componentVulnerability,
      Vulnerability vulnerability)
  {
    Affect affect = new Affect();
    affect.setRef(component.getBomRef());

    List<Affect> affects = vulnerability.getAffects() != null ? vulnerability.getAffects() : new ArrayList<>();
    affects.add(affect);
    vulnerability.setAffects(affects);
  }

  private void generateFile(Bom bom) {
    BomJsonGenerator generator = BomGeneratorFactory.createJson(Version.VERSION_14, bom);

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME_OUTPUT))) {
      writer.write(generator.toJsonString());
      writer.flush();
      log.info("CycloneDX SBOM file: {}", FILE_NAME_OUTPUT);
    }
    catch (Exception e) {
      throw new RuntimeException("Error generating the CycloneDX SBOM file", e);
    }
  }
}
