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
package org.sonatype.gradle.plugins.scan;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.sonatype.gradle.plugins.scan.ossindex.BannerUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.gradle.testkit.runner.TaskOutcome.FAILED;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.sonatype.gradle.plugins.scan.nexus.iq.index.NexusIqIndexTask.MODULE_XML_FILE;
import static org.sonatype.gradle.plugins.scan.nexus.iq.scan.NexusIqPluginScanExtension.SONATYPE_CLM_FOLDER;
import static org.sonatype.gradle.plugins.scan.ossindex.CycloneDxResponseHandler.FILE_NAME_OUTPUT;

@RunWith(Parameterized.class)
public abstract class ScanPluginIntegrationTestBase
{
  private static final String DEPENDENCY_PREFIX = "+--- ";

  /*
   * Parameters are declared in subclasses in order run subsets of Gradle versions to avoid Memory errors on CI.
   */

  private final String gradleVersion;

  public ScanPluginIntegrationTestBase(String gradleVersion) {
    this.gradleVersion = gradleVersion;
  }

  @Rule
  public final TemporaryFolder testProjectDir = new TemporaryFolder();
  private File buildFile;

  @Before
  public void setup() throws IOException {
    buildFile = testProjectDir.newFile("build.gradle");
  }

  @After
  public void tearDown() {
    buildFile = null;
    new File(FILE_NAME_OUTPUT).delete();
  }

  @Test
  public void testScanTask_MissingTaskConfiguration_NexusIQ() throws IOException {
    writeFile(buildFile, "missing-scan.gradle");

    String errorMessage = "property '%s' doesn't have a configured value.";

    assertThatThrownBy(() -> GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("nexusIQScan")
        .build())
            .hasMessageContaining(String.format(errorMessage, "applicationId"))
            .hasMessageContaining(String.format(errorMessage, "username"))
            .hasMessageContaining(String.format(errorMessage, "password"))
            .hasMessageContaining(String.format(errorMessage, "serverUrl"));
  }

  @Test
  public void testScanTask_NoPolicyAction_NexusIQ() throws IOException {
    writeFile(buildFile, "control_dependency_graph.gradle");

    final BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("nexusIQScan", "--info")
        .build();

    assertBuildOutputText_NexusIQ(result, "None");

    assertThat(result.task(":nexusIQScan").getOutcome()).isEqualTo(SUCCESS);
  }

  @Test
  public void testScanTask_WarnPolicyAction_NexusIQ() throws IOException {
    writeFile(buildFile, "policy-warn.gradle");

    final BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("nexusIQScan", "--info")
        .build();

    assertBuildOutputText_NexusIQ(result, "Warn");

    assertThat(result.task(":nexusIQScan").getOutcome()).isEqualTo(SUCCESS);
  }

  @Test
  public void testScanTask_FailPolicyAction_NexusIQ() throws IOException {
    writeFile(buildFile, "policy-fail_dependency_graph.gradle");

    final BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("nexusIQScan", "--info")
        .buildAndFail();

    assertBuildOutputText_NexusIQ(result, "Failure");
    assertThat(result.task(":nexusIQScan").getOutcome()).isEqualTo(FAILED);
  }

  @Test
  public void testScanTask_Android_NexusIQ() throws IOException {
    File target = copyResource("android");

    final BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(target)
        .withPluginClasspath()
        .withArguments("nexusIQScan", "--info")
        .build();

    assertBuildOutputText_NexusIQ(result, "None");
    assertThat(result.task(":nexusIQScan").getOutcome()).isEqualTo(SUCCESS);
  }

  @Test
  public void testScanTask_AndroidMultipleFlavors_NexusIQ() throws IOException {
    File target = copyResource("android-multiple-flavors");

    final BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(target)
        .withPluginClasspath()
        .withArguments("nexusIQScan", "--info")
        .build();

    assertBuildOutputText_NexusIQ(result, "None");
    assertThat(result.task(":nexusIQScan").getOutcome()).isEqualTo(SUCCESS);
  }

  @Test
  public void testIndexTask_NexusIQ() throws IOException {
    writeFile(buildFile, "control_default.gradle");

    final BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("nexusIQIndex", "--info")
        .build();

    String resultOutput = result.getOutput();
    assertThat(resultOutput).contains("Saved module information to");
    assertThat(resultOutput).contains(SONATYPE_CLM_FOLDER + File.separator + MODULE_XML_FILE);
    assertThat(result.task(":nexusIQIndex").getOutcome()).isEqualTo(SUCCESS);
  }

  @Test
  public void testAuditTask_NoVulnerabilities_OssIndex_Default_Empty() throws IOException {
    writeFile(buildFile, "control_default_not_all.gradle");

    final BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("ossIndexAudit", "--info")
        .build();

    String resultOutput = result.getOutput();
    assertThat(resultOutput).contains("No vulnerabilities found!");
    assertThat(resultOutput).doesNotContainPattern("commons-collections[:@]3\\.1");
    assertThat(result.task(":ossIndexAudit").getOutcome()).isEqualTo(SUCCESS);
  }

  @Test
  public void testAuditTask_NoVulnerabilities_OssIndex_Default_ShowAll() throws IOException {
    writeFile(buildFile, "control_default.gradle");

    final BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("ossIndexAudit", "--info")
        .build();

    assertBuildOutputText_OssIndex_Default(result,
        " - pkg:maven/commons-collections/commons-collections@3.1 - No vulnerabilities found!");
    assertThat(result.task(":ossIndexAudit").getOutcome()).isEqualTo(SUCCESS);
    assertBannerText(result.getOutput(), true);
  }

  @Test
  public void testAuditTask_NoVulnerabilities_OssIndex_DependencyGraph_Empty() throws IOException {
    writeFile(buildFile, "control_dependency_graph_not_all.gradle");

    final BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("ossIndexAudit", "--info")
        .build();

    String resultOutput = result.getOutput();
    assertThat(resultOutput).contains("No vulnerabilities found!");
    assertThat(resultOutput).doesNotContainPattern("commons-collections[:@]3\\.1");
    assertThat(result.task(":ossIndexAudit").getOutcome()).isEqualTo(SUCCESS);
  }

  @Test
  public void testAuditTask_NoVulnerabilities_OssIndex_CycloneDx_Empty() throws IOException {
    writeFile(buildFile, "control_dependency_graph_not_all.gradle");

    BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("ossIndexAudit", "--info")
        .build();

    String resultOutput = result.getOutput();
    assertThat(resultOutput).contains("No vulnerabilities found!");
    assertThat(result.task(":ossIndexAudit").getOutcome()).isEqualTo(SUCCESS);

    File file = new File(FILE_NAME_OUTPUT);
    assertThat(file.exists()).isFalse();
  }

  @Test
  public void testAuditTask_NoVulnerabilities_OssIndex_DependencyGraph_ShowAll() throws IOException {
    writeFile(buildFile, "control_dependency_graph.gradle");

    final BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("ossIndexAudit", "--info")
        .build();

    assertBuildOutputText_OssIndex_DependencyGraph(result, 0);
    assertThat(result.task(":ossIndexAudit").getOutcome()).isEqualTo(SUCCESS);
  }

  @Test
  public void testAuditTask_NoVulnerabilities_OssIndex_CycloneDx_ShowAll() throws IOException {
    writeFile(buildFile, "control_cycloneDx.gradle");

    BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("ossIndexAudit", "--info")
        .build();

    assertThat(result.task(":ossIndexAudit").getOutcome()).isEqualTo(SUCCESS);
    assertThat(result.getOutput()).contains("CycloneDX SBOM file: " + FILE_NAME_OUTPUT);
  }

  @Test
  public void testAuditTask_Vulnerabilities_OssIndex_DependencyGraph() throws IOException {
    writeFile(buildFile, "policy-fail_dependency_graph.gradle");

    final BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("ossIndexAudit", "--info")
        .buildAndFail();

    assertBuildOutputText_OssIndex_DependencyGraph(result, 1);
    assertThat(result.task(":ossIndexAudit").getOutcome()).isEqualTo(FAILED);
  }

  @Test
  public void testAuditTask_Vulnerabilities_OssIndex_Default() throws IOException {
    writeFile(buildFile, "policy-fail_default.gradle");

    final BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("ossIndexAudit", "--info")
        .buildAndFail();

    assertBuildOutputText_OssIndex_Default(result,
        " - pkg:maven/commons-collections/commons-collections@3.1 - 1 vulnerability found!");
    assertThat(result.task(":ossIndexAudit").getOutcome()).isEqualTo(FAILED);
  }

  @Test
  public void testAuditTask_ExcludeVulnerabilities_ByVulnerabilityId_OssIndex() throws IOException {
    writeFile(buildFile, "exclude_vulnerabilities_by_vulnerability_id.gradle");

    final BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("ossIndexAudit", "--info")
        .build();

    assertBuildOutputText_ExcludeVulnerabilities_OssIndex(result);
  }

  @Test
  public void testAuditTask_ExcludeVulnerabilities_ByCoordinate_OssIndex() throws IOException {
    writeFile(buildFile, "exclude_vulnerabilities_by_coordinate.gradle");

    final BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("ossIndexAudit", "--info")
        .build();

    assertBuildOutputText_ExcludeVulnerabilities_OssIndex(result);
  }

  @Test
  public void testAuditTask_TransitiveDepdendencies_OssIndex() throws IOException {
    writeFile(buildFile, "transitive-dependencies.gradle");

    BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("ossIndexAudit", "--info")
        .build();

    assertThat(result.getOutput()).contains(String.format(
        "%sorg.hamcrest:hamcrest-core:2.2: 0 vulnerabilities detected%n"
        + "|    %sorg.hamcrest:hamcrest:2.2: 0 vulnerabilities detected", DEPENDENCY_PREFIX, DEPENDENCY_PREFIX));
    assertThat(result.task(":ossIndexAudit").getOutcome()).isEqualTo(SUCCESS);
  }

  @Test
  public void testAuditTask_ExcludeTestDependencies_OssIndex_DependencyGraph() throws IOException {
    writeFile(buildFile, "exclude-test-dependency-graph.gradle");

    BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("ossIndexAudit", "--info")
        .build();

    assertThat(result.getOutput()).contains("0 dependencies");
    assertThat(result.task(":ossIndexAudit").getOutcome()).isEqualTo(SUCCESS);
  }

  @Test
  public void testAuditTask_ExcludeTestDependencies_OssIndex_Default() throws IOException {
    writeFile(buildFile, "exclude-test-default.gradle");

    BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("ossIndexAudit", "--info")
        .build();

    assertThat(result.getOutput()).contains("0 dependencies");
    assertThat(result.task(":ossIndexAudit").getOutcome()).isEqualTo(SUCCESS);
  }

  @Test
  public void testAuditTask_IncludeTestDependencies_OssIndex_DependencyGraph() throws IOException {
    writeFile(buildFile, "include-test-dependency-graph.gradle");

    BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("ossIndexAudit", "--info")
        .build();

    assertBuildOutputText_OssIndex_DependencyGraph(result, 0);
    assertThat(result.task(":ossIndexAudit").getOutcome()).isEqualTo(SUCCESS);
  }

  @Test
  public void testAuditTask_IncludeTestDependencies_OssIndex_Default() throws IOException {
    writeFile(buildFile, "include-test-default.gradle");

    BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("ossIndexAudit", "--info")
        .build();

    assertBuildOutputText_OssIndex_Default(result,
        " - pkg:maven/commons-collections/commons-collections@3.1 - No vulnerabilities found!");
    assertThat(result.task(":ossIndexAudit").getOutcome()).isEqualTo(SUCCESS);
  }

  @Test
  public void testAuditTask_ExcludeCompileOnlyDependencies_OssIndex_DependencyGraph() throws IOException {
    writeFile(buildFile, "exclude-compileOnly-dependency-graph.gradle");

    BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("ossIndexAudit", "--info")
        .build();

    String output = result.getOutput();

    // Direct dependency on compileOnly and its transitive ones are not found
    assertThat(output).doesNotContain("com.fasterxml.jackson.core:jackson-databind");
    assertThat(output).doesNotContain("com.fasterxml.jackson.core:jackson-annotations");
    assertThat(output).doesNotContain("com.fasterxml.jackson.core:jackson-core");

    // Only the dependency explicitly declared in another configuration is found
    assertThat(output).contains("net.bytebuddy:byte-buddy");
    assertThat(result.task(":ossIndexAudit").getOutcome()).isEqualTo(SUCCESS);
  }

  @Test
  public void testAuditTask_ExcludeCompileOnlyDependencies_OssIndex_Default() throws IOException {
    writeFile(buildFile, "exclude-compileOnly-default.gradle");

    BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("ossIndexAudit", "--info")
        .build();

    String output = result.getOutput();

    // Direct dependency on compileOnly and its transitive ones are not found
    assertThat(output).doesNotContain("pkg:maven/com.fasterxml.jackson.core/jackson-databind");
    assertThat(output).doesNotContain("pkg:maven/com.fasterxml.jackson.core/jackson-annotations");
    assertThat(output).doesNotContain("pkg:maven/com.fasterxml.jackson.core/jackson-core");

    // Only the dependency explicitly declared in another configuration is found
    assertThat(output).contains("pkg:maven/net.bytebuddy/byte-buddy");
    assertThat(result.task(":ossIndexAudit").getOutcome()).isEqualTo(SUCCESS);
  }

  @Test
  public void testAuditTask_Android_OssIndex() throws IOException {
    File target = copyResource("android");

    BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(target)
        .withPluginClasspath()
        .withArguments("ossIndexAudit", "--info")
        .build();

    String resultOutput = result.getOutput();
    assertThat(resultOutput).contains(String.format(
        "%scom.android.support:appcompat-v7:24.2.1: 0 vulnerabilities detected%n"
            + "|    %scom.android.support:animated-vector-drawable:24.2.1: 0 vulnerabilities detected",
        DEPENDENCY_PREFIX, DEPENDENCY_PREFIX));
    assertThat(resultOutput).contains(
        String.format("%scommons-collections:commons-collections:3.1: 0 vulnerabilities detected", DEPENDENCY_PREFIX));

    assertThat(result.task(":ossIndexAudit").getOutcome()).isEqualTo(SUCCESS);
  }

  @Test
  public void testAuditTask_AndroidMultipleFlavors_OssIndex() throws IOException {
    File target = copyResource("android-multiple-flavors");

    BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(target)
        .withPluginClasspath()
        .withArguments("ossIndexAudit", "--info")
        .build();

    String resultOutput = result.getOutput();
    assertThat(resultOutput).contains(String.format(
        "%scom.android.support:appcompat-v7:24.2.1: 0 vulnerabilities detected%n"
            + "|    %scom.android.support:animated-vector-drawable:24.2.1: 0 vulnerabilities detected",
        DEPENDENCY_PREFIX, DEPENDENCY_PREFIX));
    assertThat(resultOutput).contains(
        String.format("%scommons-collections:commons-collections:3.1: 0 vulnerabilities detected", DEPENDENCY_PREFIX));

    assertThat(result.task(":ossIndexAudit").getOutcome()).isEqualTo(SUCCESS);
  }

  @Test
  public void testAuditTask_HideBanner() throws IOException {
    writeFile(buildFile, "hide_banner.gradle");

    final BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.getRoot())
        .withPluginClasspath()
        .withArguments("ossIndexAudit", "--info")
        .build();

    assertBannerText(result.getOutput(), false);
  }

  private void assertBannerText(String resultOutput, boolean showBanner) {
    String bannerText = BannerUtils.createBanner();
    if (showBanner) {
      assertThat(resultOutput).contains(bannerText);
    }
    else {
      assertThat(resultOutput).doesNotContain(bannerText);
    }
  }

  private void writeFile(File destination, String resourceName) throws IOException {
    try (InputStream contentStream = getClass().getClassLoader().getResourceAsStream(resourceName);
        BufferedWriter output = new BufferedWriter(new FileWriter(destination))) {
      assert contentStream != null;
      IOUtils.copy(contentStream, output, StandardCharsets.UTF_8);
    }
  }

  private File copyResource(String resource) throws IOException {
    URL url = getClass().getResource(File.separator + resource);
    File target = new File(testProjectDir.getRoot(), resource);
    FileUtils.copyDirectory(new File(url.getFile()), target);
    return target;
  }

  private void assertBuildOutputText_NexusIQ(BuildResult result, String policyAction) {
    final String resultOutput = result.getOutput();
    assertThat(resultOutput).contains("Policy Action: " + policyAction);
    assertThat(resultOutput).contains("Number of components affected: 0 critical, 0 severe, 0 moderate");
    assertThat(resultOutput).contains("Number of legacy violations: 0");
    assertThat(resultOutput).contains("The detailed report can be viewed online at simulated/report");
    assertThat(resultOutput).contains("Number of components: 1");
  }

  private void assertBuildOutputText_OssIndex_DependencyGraph(BuildResult result, int vulnerabilities) {
    String resultOutput = result.getOutput();
    assertThat(resultOutput)
        .contains(String.format("%scommons-collections:commons-collections:3.1: %s vulnerabilities detected",
            DEPENDENCY_PREFIX, vulnerabilities));
  }

  private void assertBuildOutputText_OssIndex_Default(BuildResult result, String expectedText) {
    String resultOutput = result.getOutput();
    assertThat(resultOutput).contains(expectedText);
  }

  private void assertBuildOutputText_ExcludeVulnerabilities_OssIndex(BuildResult result) {
    String resultOutput = result.getOutput();
    assertThat(resultOutput).contains("No vulnerabilities found!");
    assertThat(resultOutput).doesNotContainPattern("commons-collections[:@]3\\.1");
    assertThat(result.task(":ossIndexAudit").getOutcome()).isEqualTo(SUCCESS);
  }
}
