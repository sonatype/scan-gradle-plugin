/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.ossindex;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sonatype.goodies.packageurl.PackageUrl;
import org.sonatype.goodies.packageurl.PackageUrlBuilder;
import org.sonatype.gradle.plugins.scan.common.PluginVersionUtils;
import org.sonatype.ossindex.service.api.componentreport.ComponentReport;
import org.sonatype.ossindex.service.api.componentreport.ComponentReportVulnerability;

import com.google.common.collect.ImmutableMap;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Tool;
import org.cyclonedx.model.vulnerability.Vulnerability;
import org.cyclonedx.model.vulnerability.Vulnerability.Advisory;
import org.cyclonedx.model.vulnerability.Vulnerability.Affect;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Severity;
import org.cyclonedx.parsers.JsonParser;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedDependency;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.gradle.plugins.scan.ossindex.CycloneDxResponseHandler.FILE_NAME_OUTPUT;

@RunWith(MockitoJUnitRunner.class)
public class CycloneDxResponseHandlerTest
{
  private OssIndexPluginExtension extension;

  private CycloneDxResponseHandler handler;

  @Mock
  private Project project;

  @Before
  public void setup() {
    extension = new OssIndexPluginExtension(null);
    extension.setOutputFormat(OutputFormat.JSON_CYCLONE_DX_1_4);

    when(project.getGroup()).thenReturn("test-group");
    when(project.getName()).thenReturn("some-name");
    when(project.getVersion()).thenReturn("0.0.1");

    handler = new CycloneDxResponseHandler(extension, project);
  }

  @After
  public void tearDown() {
    new File(FILE_NAME_OUTPUT).delete();
  }

  @Test
  public void testHandleOssIndexResponse_noComponents() {
    handler.handleOssIndexResponse(Collections.emptySet(), Collections.emptyMap(), Collections.emptyMap());

    File file = new File(FILE_NAME_OUTPUT);
    assertThat(file.exists()).isFalse();
  }

  @Test
  public void testHandleOssIndexResponse_nonVulnerableComponentNoShowAll() {
    ResolvedDependency resolvedDependency = mock(ResolvedDependency.class);
    PackageUrl packageUrl = new PackageUrlBuilder().type("maven").namespace("g").name("a").version("v").build();
    ComponentReport componentReport = new ComponentReport();

    Map<ResolvedDependency, PackageUrl> dependenciesMap = Collections.singletonMap(resolvedDependency, packageUrl);
    Map<PackageUrl, ComponentReport> response = Collections.singletonMap(packageUrl, componentReport);

    handler.handleOssIndexResponse(Collections.emptySet(), dependenciesMap, response);

    File file = new File(FILE_NAME_OUTPUT);
    assertThat(file.exists()).isFalse();
  }

  @Test
  public void testHandleOssIndexResponse_nonVulnerableComponentShowAll() throws ParseException {
    extension.setShowAll(true);

    ResolvedDependency resolvedDependency = mock(ResolvedDependency.class);
    PackageUrl packageUrl = new PackageUrlBuilder().type("maven").namespace("g").name("a").version("v").build();
    ComponentReport componentReport = new ComponentReport();

    Map<ResolvedDependency, PackageUrl> dependenciesMap = Collections.singletonMap(resolvedDependency, packageUrl);
    Map<PackageUrl, ComponentReport> response = Collections.singletonMap(packageUrl, componentReport);

    handler.handleOssIndexResponse(Collections.emptySet(), dependenciesMap, response);

    File file = new File(FILE_NAME_OUTPUT);
    assertThat(file.exists()).isTrue();

    JsonParser jsonParser = new JsonParser();
    Bom bom = jsonParser.parse(file);

    assertThat(bom).isNotNull();
    assertThat(bom.getVulnerabilities()).isNullOrEmpty();
    assertThat(bom.getSerialNumber()).isNotBlank();

    Metadata metadata = bom.getMetadata();
    assertThat(metadata).isNotNull();
    assertThat(metadata.getTimestamp()).isNotNull();

    Component component = bom.getMetadata().getComponent();
    assertThat(component).isNotNull();
    assertThat(component.getGroup()).isEqualTo(project.getGroup());
    assertThat(component.getName()).isEqualTo(project.getName());
    assertThat(component.getVersion()).isEqualTo(project.getVersion());

    assertThat(metadata.getTools()).hasSize(1);
    Tool tool = metadata.getTools().get(0);
    assertThat(tool.getVendor()).isEqualTo("Sonatype");
    assertThat(tool.getName()).isEqualTo("Scan Gradle Plugin (aka Sherlock Trunks)");
    assertThat(tool.getVersion()).isEqualTo(PluginVersionUtils.getPluginVersion());

    assertComponents(bom, packageUrl);
  }

  @Test
  public void testHandleOssIndexResponse_vulnerableComponent() throws ParseException {
    ResolvedDependency resolvedDependency = mock(ResolvedDependency.class);
    PackageUrl packageUrl = new PackageUrlBuilder().type("maven").namespace("g").name("a").version("v").build();

    ComponentReport componentReport = new ComponentReport();
    componentReport.setCoordinates(packageUrl);

    ComponentReportVulnerability vulnerability = buildComponentReportVulnerability();
    componentReport.setVulnerabilities(Collections.singletonList(vulnerability));

    Map<ResolvedDependency, PackageUrl> dependenciesMap = Collections.singletonMap(resolvedDependency, packageUrl);
    Map<PackageUrl, ComponentReport> response = Collections.singletonMap(packageUrl, componentReport);

    handler.handleOssIndexResponse(Collections.emptySet(), dependenciesMap, response);

    File file = new File(FILE_NAME_OUTPUT);
    assertThat(file.exists()).isTrue();

    JsonParser jsonParser = new JsonParser();
    Bom bom = jsonParser.parse(file);
    assertThat(bom).isNotNull();

    assertComponents(bom, packageUrl);
    assertVulnerability(bom, vulnerability, packageUrl);
  }

  @Test
  public void testHandleOssIndexResponse_vulnerabilityOnMultipleComponents() throws ParseException {
    ResolvedDependency resolvedDependency1 = mock(ResolvedDependency.class);
    ResolvedDependency resolvedDependency2 = mock(ResolvedDependency.class);

    PackageUrl packageUrl1 = new PackageUrlBuilder().type("maven").namespace("g1").name("a1").version("v1").build();
    PackageUrl packageUrl2 = new PackageUrlBuilder().type("maven").namespace("g2").name("a2").version("v2").build();

    ComponentReport componentReport1 = new ComponentReport();
    componentReport1.setCoordinates(packageUrl1);

    ComponentReport componentReport2 = new ComponentReport();
    componentReport2.setCoordinates(packageUrl1);

    ComponentReportVulnerability vulnerability = buildComponentReportVulnerability();

    componentReport1.setVulnerabilities(Collections.singletonList(vulnerability));
    componentReport2.setVulnerabilities(Collections.singletonList(vulnerability));

    Map<ResolvedDependency, PackageUrl> dependenciesMap =
        ImmutableMap.of(resolvedDependency1, packageUrl1, resolvedDependency2, packageUrl2);
    Map<PackageUrl, ComponentReport> response =
        ImmutableMap.of(packageUrl1, componentReport1, packageUrl2, componentReport2);

    handler.handleOssIndexResponse(Collections.emptySet(), dependenciesMap, response);

    File file = new File(FILE_NAME_OUTPUT);
    assertThat(file.exists()).isTrue();

    JsonParser jsonParser = new JsonParser();
    Bom bom = jsonParser.parse(file);
    assertThat(bom).isNotNull();

    assertComponents(bom, packageUrl1, packageUrl2);
    assertVulnerability(bom, vulnerability, packageUrl1, packageUrl2);
  }

  private void assertComponents(Bom bom, PackageUrl... packageUrls) {
    List<Component> components = Stream.of(packageUrls).map(packageUrl -> {
      Component component = new Component();
      component.setType(Component.Type.LIBRARY);
      component.setGroup(packageUrl.getNamespaceAsString());
      component.setName(packageUrl.getName());
      component.setVersion(packageUrl.getVersion());
      component.setPurl(packageUrl.toString());
      component.setBomRef(packageUrl.toString());
      return component;
    }).collect(Collectors.toList());

    assertThat(bom.getComponents()).containsExactlyInAnyOrderElementsOf(components);
  }

  private ComponentReportVulnerability buildComponentReportVulnerability() {
    ComponentReportVulnerability vulnerability = new ComponentReportVulnerability();
    vulnerability.setId("TEST-123");
    vulnerability.setReference(URI.create("https://test.com/TEST-123"));
    vulnerability.setExternalReferences(Collections.singletonList(URI.create("https://external-test.com/TEST-123")));
    vulnerability.setCvssScore(10.0F);
    vulnerability.setCvssVector("some/test/vector");
    vulnerability.setDescription("This is a test vulnerability");
    vulnerability.setCwe("CWE-456");
    return vulnerability;
  }

  private void assertVulnerability(Bom bom, ComponentReportVulnerability vulnerability, PackageUrl... packageUrls) {
    assertThat(bom.getVulnerabilities()).hasSize(1);
    Vulnerability resultVulnerability = bom.getVulnerabilities().get(0);

    assertThat(resultVulnerability.getId()).isEqualTo(vulnerability.getId());
    assertThat(resultVulnerability.getSource()).isNotNull();
    assertThat(resultVulnerability.getSource().getName()).isEqualTo("OSS Index");
    assertThat(resultVulnerability.getSource().getUrl()).isEqualTo(vulnerability.getReference().toString());

    assertThat(resultVulnerability.getRatings()).hasSize(1);
    Rating rating = resultVulnerability.getRatings().get(0);
    assertThat(rating.getScore()).isEqualTo(vulnerability.getCvssScore().doubleValue());
    assertThat(rating.getSeverity()).isEqualTo(
        Severity.fromString(VulnerabilityUtils.getAssessment(vulnerability.getCvssScore()).toLowerCase(Locale.ROOT)));
    assertThat(rating.getVector()).isEqualTo(vulnerability.getCvssVector());

    assertThat(resultVulnerability.getCwes())
        .containsExactly(Integer.valueOf(vulnerability.getCwe().replaceAll("\\D+", "")));
    assertThat(resultVulnerability.getDescription()).isEqualTo(vulnerability.getDescription());

    assertThat(resultVulnerability.getAdvisories())
        .extracting(Advisory::getUrl)
        .map(URI::create)
        .containsExactlyInAnyOrderElementsOf(vulnerability.getExternalReferences());

    assertThat(resultVulnerability.getTools()).hasSize(1);
    Tool tool = resultVulnerability.getTools().get(0);
    assertThat(tool.getVendor()).isEqualTo("Sonatype");
    assertThat(tool.getName()).isEqualTo("OSS Index");

    assertThat(resultVulnerability.getAffects())
        .extracting(Affect::getRef)
        .map(PackageUrl::parse)
        .containsExactlyInAnyOrder(packageUrls);
  }
}
