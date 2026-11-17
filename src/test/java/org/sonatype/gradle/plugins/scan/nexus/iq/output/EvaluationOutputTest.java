/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.output;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.policy.Stage;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.Action;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ApplicationPolicyEvaluation;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ComponentDisplayName;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ComponentDisplayNamePart;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ComponentFact;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ComponentIdentifier;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ConditionFact;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ConstraintFact;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.PolicyAlert;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.PolicyFact;

import org.junit.Test;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class EvaluationOutputTest
{
  private static final ConsoleStyle PLAIN = ConsoleStyle.of(false, false);

  @Test
  public void testResults_groupedPerComponentAndSortedByThreatLevel() {
    ComponentFact axios = component("axios", "0.21.1", "a1",
        constraint("Critical risk CVSS score", severity("CVE-2025-1", "9.9"), severity("CVE-2025-1", "9.9")));
    ComponentFact jwt = component("jsonwebtoken", "8.5.1", "b1",
        constraint("High risk CVSS score", severity("CVE-2022-1", "8.1")));
    ApplicationPolicyEvaluation evaluation = evaluation(
        alert("p1", "Security-Medium", 5, Action.ID_WARN, jwt),
        alert("p2", "Security-High", 8, Action.ID_NOTIFY, jwt),
        alert("p3", "Security-Critical", 10, Action.ID_FAIL, axios));

    List<String> lines = results(PLAIN, evaluation);

    assertThat(lines).containsExactly(
        " ",
        "─────────────────────────────────── Evaluation Results ───────────────────────────────────",
        " ",
        "jsonwebtoken@8.5.1:",
        " ➤ Notification for policy 'Security-High' due to:",
        "    - High risk CVSS score: Found security vulnerability CVE-2022-1 with severity 8.1",
        " ➤ Warning for policy 'Security-Medium' due to:",
        "    - High risk CVSS score: Found security vulnerability CVE-2022-1 with severity 8.1",
        " ",
        "axios@0.21.1:",
        " ➤ Failure for policy 'Security-Critical' due to:",
        "    - Critical risk CVSS score: Found security vulnerability CVE-2025-1 with severity 9.9",
        " ");
  }

  @Test
  public void testResults_noAlerts_printsNothing() {
    assertThat(results(PLAIN, evaluation())).isEmpty();
  }

  @Test
  public void testResults_mostSevereActionLabelsThePolicy() {
    ComponentFact lib = component("lib", "1.0", "h", constraint("c", severity("CVE-1", "5.0")));
    PolicyFact policy = new PolicyFact("p", "Policy", 5, singletonList(lib));
    ApplicationPolicyEvaluation evaluation = evaluation(new PolicyAlert(policy,
        List.of(new Action(Action.ID_NOTIFY), new Action(Action.ID_FAIL), new Action(Action.ID_WARN))));

    assertThat(results(PLAIN, evaluation)).contains(" ➤ Failure for policy 'Policy' due to:");
  }

  @Test
  public void testResults_differentSeveritiesAreNotMerged() {
    ComponentFact lib = component("lib", "1.0", "h",
        constraint("c", severity("CVE-1", "5.0"), severity("CVE-1", "7.0")));

    assertThat(results(PLAIN, evaluation(alert("p", "P", 5, Action.ID_FAIL, lib))))
        .contains("    - c: Found security vulnerability CVE-1 with severity 5.0; "
            + "Found security vulnerability CVE-1 with severity 7.0");
  }

  @Test
  public void testResults_sanitizesValuesAndHandlesNullReason() {
    ComponentFact lib = component("li\nb\u001B[31m", "1.0", "h",
        constraint("c", new ConditionFact("Other", "s", null)));

    List<String> lines = results(PLAIN, evaluation(alert("p\r\nx", "P", 5, Action.ID_WARN, lib)));

    assertThat(lines).contains("li b [31m@1.0:", "    - c: ");
    assertThat(String.join("", lines)).doesNotContain("\u001B");
  }

  @Test
  public void testClean_replacesAllControlCharacters() {
    String dirty = "a\u0007b\bc\td\u009Be\u001Bf\rg\nh\u007Fi";
    assertThat(EvaluationResultsFormatter.clean(dirty)).isEqualTo("a b c d e f g h i");
    assertThat(EvaluationResultsFormatter.clean("plain text (7.5)")).isEqualTo("plain text (7.5)");
    assertThat(EvaluationResultsFormatter.clean(null)).isEmpty();
  }

  @Test
  public void testResults_componentWithoutDisplayNameFallsBackToHash() {
    ComponentFact lib = new ComponentFact(null, "hash123", singletonList(constraint("c", severity("CVE-1", "5.0"))),
        List.of(), null);

    assertThat(results(PLAIN, evaluation(alert("p", "P", 5, Action.ID_WARN, lib)))).contains("hash123:");
  }

  @Test
  public void testSummary_failure() {
    List<String> lines = summary(PLAIN, evaluationWithCounts(), "failure", "build");

    assertThat(lines).containsExactly(
        "─────────────────────────────────── Evaluation Summary ───────────────────────────────────",
        " ",
        "Policy action: failure",
        "Stage: build",
        "Policy violations: 45 critical, 28 severe, 2 moderate - legacy violations: 3",
        "Components affected: 13 critical, 4 severe, 1 moderate - total components: 71",
        "Application report: https://iq.example.com/report",
        "Developer priorities: https://iq.example.com/priorities",
        " ");
  }

  @Test
  public void testSummary_complianceStageShowsBillOfMaterialsAndNoPriorities() {
    List<String> lines = summary(PLAIN, evaluationWithCounts(), "none", Stage.ID_COMPLIANCE);

    assertThat(lines).contains("Bill of Materials page: https://iq.example.com/report")
        .noneMatch(line -> line.startsWith("Developer priorities"));
  }

  @Test
  public void testSummary_withoutPrecedingResultsHasLeadingSpacer() {
    List<String> lines = new ArrayList<>();
    EvaluationSummaryFormatter.print(lines::add, PLAIN, evaluationWithCounts(), "none", "build", false);

    assertThat(lines.subList(0, 3)).containsExactly(" ",
        "─────────────────────────────────── Evaluation Summary ───────────────────────────────────", " ");
  }

  @Test
  public void testResults_returnsWhetherAnythingWasPrinted() {
    ComponentFact lib = component("lib", "1.0", "h", constraint("c", severity("CVE-1", "5.0")));

    assertThat(EvaluationResultsFormatter.print(line -> { }, PLAIN, evaluation())).isFalse();
    assertThat(EvaluationResultsFormatter.print(line -> { }, PLAIN,
        evaluation(alert("p", "P", 5, Action.ID_WARN, lib)))).isTrue();
  }

  @Test
  public void testSummary_noPrioritiesUrl() {
    ApplicationPolicyEvaluation evaluation =
        new ApplicationPolicyEvaluation(0, 0, 0, 0, 0, 0, 0, 0, 1, List.of(), "https://iq.example.com/report", null);

    assertThat(summary(PLAIN, evaluation, "none", "build")).noneMatch(line -> line.startsWith("Developer"));
  }

  @Test
  public void testAsciiFallback() {
    ConsoleStyle ascii = ConsoleStyle.of(true, false);
    ComponentFact lib = component("lib", "1.0", "h", constraint("c", severity("CVE-1", "5.0")));

    List<String> lines = results(ascii, evaluation(alert("p", "P", 5, Action.ID_WARN, lib)));

    assertThat(lines.get(1)).startsWith("-----").contains(" Evaluation Results ").doesNotContain("─");
    assertThat(lines).contains(" > Warning for policy 'P' due to:");
    assertThat(String.join("", lines)).matches("\\A\\p{ASCII}*\\z");
  }

  @Test
  public void testCanEncodeGlyphs() {
    assertThat(ConsoleStyle.canEncodeGlyphs(StandardCharsets.UTF_8)).isTrue();
    assertThat(ConsoleStyle.canEncodeGlyphs(StandardCharsets.US_ASCII)).isFalse();
    assertThat(ConsoleStyle.canEncodeGlyphs(StandardCharsets.ISO_8859_1)).isFalse();
  }

  @Test
  public void testUncoloredOutputHasNoEscapeCharactersAndColoredOutputDoes() {
    ComponentFact lib = component("lib", "1.0", "h", constraint("c", severity("CVE-1", "5.0")));
    ApplicationPolicyEvaluation evaluation = evaluation(alert("p", "P", 5, Action.ID_FAIL, lib));
    ConsoleStyle colored = ConsoleStyle.of(false, true);

    assertThat(String.join("", results(PLAIN, evaluation))).doesNotContain("\u001B");
    assertThat(String.join("", summary(PLAIN, evaluationWithCounts(), "failure", "build"))).doesNotContain("\u001B");
    assertThat(results(colored, evaluation)).anyMatch(line -> line.contains("\u001B[31mFailure\u001B[0m"));
    assertThat(summary(colored, evaluationWithCounts(), "warning", "build"))
        .anyMatch(line -> line.contains("\u001B[33mwarning\u001B[0m"));
    // stripping the escape sequences gives exactly the plain text
    assertThat(strip(results(colored, evaluation))).isEqualTo(results(PLAIN, evaluation));
    assertThat(strip(summary(colored, evaluationWithCounts(), "none", "build")))
        .isEqualTo(summary(PLAIN, evaluationWithCounts(), "none", "build"));
  }

  private static List<String> strip(final List<String> lines) {
    List<String> result = new ArrayList<>();
    lines.forEach(line -> result.add(line.replaceAll("\u001B\\[[0-9;]*m", "")));
    return result;
  }

  private static List<String> results(final ConsoleStyle style, final ApplicationPolicyEvaluation evaluation) {
    List<String> lines = new ArrayList<>();
    EvaluationResultsFormatter.print(lines::add, style, evaluation);
    return lines;
  }

  private static List<String> summary(
      final ConsoleStyle style,
      final ApplicationPolicyEvaluation evaluation,
      final String action,
      final String stage)
  {
    List<String> lines = new ArrayList<>();
    EvaluationSummaryFormatter.print(lines::add, style, evaluation, action, stage, true);
    return lines;
  }

  private static ApplicationPolicyEvaluation evaluationWithCounts() {
    return new ApplicationPolicyEvaluation(0, 13, 4, 1, 45, 28, 2, 3, 71, List.of(),
        "https://iq.example.com/report", "https://iq.example.com/priorities");
  }

  private static ApplicationPolicyEvaluation evaluation(final PolicyAlert... alerts) {
    return new ApplicationPolicyEvaluation(0, 0, 0, 0, 0, 0, 0, 0, 1, List.of(alerts), "report", "priorities");
  }

  private static PolicyAlert alert(
      final String id,
      final String name,
      final int threat,
      final String action,
      final ComponentFact... components)
  {
    return new PolicyAlert(new PolicyFact(id, name, threat, List.of(components)), singletonList(new Action(action)));
  }

  private static ComponentFact component(
      final String name,
      final String version,
      final String hash,
      final ConstraintFact... constraints)
  {
    ComponentDisplayName displayName = new ComponentDisplayName(List.of(
        new ComponentDisplayNamePart("Artifact", name), new ComponentDisplayNamePart("Version", version)));
    return new ComponentFact(new ComponentIdentifier("maven", new TreeMap<>(Map.of("artifactId", name, "version", version))), hash, List.of(constraints),
        List.of(), displayName);
  }

  private static ConstraintFact constraint(final String name, final ConditionFact... conditions) {
    return new ConstraintFact("id-" + name, name, "AND", List.of(conditions));
  }

  private static ConditionFact severity(final String cve, final String score) {
    return new ConditionFact("SecurityVulnerabilitySeverity", "summary",
        "Found security vulnerability " + cve + " with severity " + (score.startsWith("9") ? "Critical" : "High")
            + " (severity = " + score + ")");
  }
}
