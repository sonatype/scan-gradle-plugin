/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.output;

import java.util.Locale;
import java.util.function.Consumer;

import com.sonatype.clm.dto.model.policy.Stage;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ApplicationPolicyEvaluation;

import static org.sonatype.gradle.plugins.scan.nexus.iq.output.EvaluationResultsFormatter.clean;

/**
 * The closing summary of the evaluation, as printed by the IQ CLI.
 */
public final class EvaluationSummaryFormatter
{
  private EvaluationSummaryFormatter() {
  }

  /**
   * @param policyAction {@code failure}, {@code warning} or {@code none}
   * @param tightHeader whether the output right before already ends with a spacer line
   */
  public static void print(
      final Consumer<String> out,
      final ConsoleStyle style,
      final ApplicationPolicyEvaluation evaluation,
      final String policyAction,
      final String stageId,
      final boolean tightHeader)
  {
    style.printHeader(out, "Evaluation Summary", tightHeader);
    out.accept("Policy action: " + actionText(style, policyAction));
    out.accept("Stage: " + clean(stageId));
    out.accept(String.format(Locale.ROOT,
        "Policy violations: %s critical, %s severe, %s moderate - legacy violations: %d",
        style.boldRed(String.valueOf(evaluation.getCriticalPolicyViolationCount())),
        style.boldMagenta(String.valueOf(evaluation.getSeverePolicyViolationCount())),
        style.boldYellow(String.valueOf(evaluation.getModeratePolicyViolationCount())),
        evaluation.getLegacyViolationCount()));
    out.accept(String.format(Locale.ROOT,
        "Components affected: %s critical, %s severe, %s moderate - total components: %d",
        style.boldRed(String.valueOf(evaluation.getCriticalComponentCount())),
        style.boldMagenta(String.valueOf(evaluation.getSevereComponentCount())),
        style.boldYellow(String.valueOf(evaluation.getModerateComponentCount())),
        evaluation.getTotalComponentCount()));
    boolean compliance = Stage.ID_COMPLIANCE.equals(stageId);
    out.accept((compliance ? "Bill of Materials page" : "Application report") + ": "
        + style.blue(clean(evaluation.getApplicationCompositionReportUrl())));
    String prioritiesUrl = evaluation.getApplicationPrioritiesUrl();
    if (prioritiesUrl != null && !compliance) {
      out.accept("Developer priorities: " + style.blue(clean(prioritiesUrl)));
    }
    out.accept(style.spacer());
  }

  private static String actionText(final ConsoleStyle style, final String policyAction) {
    switch (policyAction) {
      case "failure":
        return style.red(policyAction);
      case "warning":
        return style.yellow(policyAction);
      default:
        return style.green(policyAction);
    }
  }
}
