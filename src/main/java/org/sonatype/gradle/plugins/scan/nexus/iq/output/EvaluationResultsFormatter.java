/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.output;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sonatype.gradle.plugins.scan.nexus.iq.api.Action;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ApplicationPolicyEvaluation;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ComponentDisplayName;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ComponentDisplayNamePart;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ComponentFact;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ConditionFact;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ConstraintFact;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.PolicyAlert;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.PolicyFact;

/**
 * Per-component policy results, as printed by the IQ CLI: {@code name@version:} followed by one line per policy
 * and one line per constraint.
 */
public final class EvaluationResultsFormatter
{
  private static final String SECURITY_VULNERABILITY_SEVERITY = "SecurityVulnerabilitySeverity";

  private static final Pattern SECURITY_VULNERABILITY_SEVERITY_PATTERN =
      Pattern.compile("Found security vulnerability (\\S+) with severity [^(]+ \\(severity = ([\\d.]+)\\)");

  private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\p{Cntrl}\\u0080-\\u009F]");

  private static final List<String> NAME_LABELS = List.of("Artifact", "ID", "Name");

  private EvaluationResultsFormatter() {
  }

  /**
   * Prints the results of the evaluation, one entry per line; prints nothing (not even the header) when there are
   * none.
   *
   * @return whether anything was printed
   */
  public static boolean print(
      final Consumer<String> out,
      final ConsoleStyle style,
      final ApplicationPolicyEvaluation evaluation)
  {
    Map<Object, List<Entry>> byComponent = group(evaluation);
    if (byComponent.isEmpty()) {
      return false;
    }
    style.printHeader(out, "Evaluation Results", false);
    for (List<Entry> entries : byComponent.values()) {
      format(entries, style).forEach(out);
      out.accept(style.spacer());
    }
    return true;
  }

  static final class Entry
  {
    final ComponentFact component;

    final PolicyFact policy;

    final Action action;

    Entry(final ComponentFact component, final PolicyFact policy, final Action action) {
      this.component = component;
      this.policy = policy;
      this.action = action;
    }
  }

  private static Map<Object, List<Entry>> group(final ApplicationPolicyEvaluation evaluation) {
    Map<Object, List<Entry>> byComponent = new LinkedHashMap<>();
    for (PolicyAlert alert : evaluation.getPolicyAlerts()) {
      Action action = mostSevere(alert.getActions());
      PolicyFact policy = alert.getTrigger();
      if (action == null || policy == null) {
        continue;
      }
      for (ComponentFact component : policy.getComponentFacts()) {
        byComponent.computeIfAbsent(keyOf(component), k -> new ArrayList<>())
            .add(new Entry(component, policy, action));
      }
    }
    byComponent.values().forEach(
        list -> list.sort(Comparator.comparingInt((Entry e) -> e.policy.getThreatLevel()).reversed()));
    return byComponent;
  }

  private static Object keyOf(final ComponentFact component) {
    if (component.getComponentIdentifier() != null) {
      return component.getComponentIdentifier().toString();
    }
    return component.getHash() != null ? component.getHash() : new Object();
  }

  private static Action mostSevere(final List<? extends Action> actions) {
    Action result = null;
    int resultSeverity = -1;
    for (Action action : actions) {
      int severity = Action.ID_FAIL.equals(action.getActionTypeId()) ? 2 :
          Action.ID_WARN.equals(action.getActionTypeId()) ? 1 : 0;
      if (severity > resultSeverity) {
        result = action;
        resultSeverity = severity;
      }
    }
    return result;
  }

  // visible for testing
  static List<String> format(final List<Entry> entries, final ConsoleStyle style) {
    List<String> lines = new ArrayList<>();
    ComponentFact first = entries.get(0).component;
    lines.add(style.boldBlue(clean(normalizeDisplayName(first))) + ":");
    String lastPolicyId = "";
    for (Entry entry : entries) {
      if (!Objects.equals(entry.policy.getPolicyId(), lastPolicyId)) {
        String actionType = entry.action.getActionTypeId();
        String label = Action.ID_FAIL.equals(actionType) ? style.red("Failure") :
            Action.ID_WARN.equals(actionType) ? style.yellow("Warning") : style.blue("Notification");
        lines.add(style.bullet() + label + " for policy '" + style.green(clean(entry.policy.getPolicyName()))
            + "' due to:");
        lastPolicyId = entry.policy.getPolicyId();
      }
      for (ConstraintFact constraint : entry.component.getConstraintFacts()) {
        lines.add("    - " + style.cyan(clean(constraint.getConstraintName())) + ": "
            + formatConditions(constraint.getConditionFacts()));
      }
    }
    return lines;
  }

  static String normalizeDisplayName(final ComponentFact component) {
    ComponentDisplayName displayName = component.getDisplayName();
    if (displayName == null) {
      return component.getHash() != null ? component.getHash() : "unknown";
    }
    String name = firstPartValue(displayName, NAME_LABELS);
    if (name == null) {
      String rendered = displayName.toString();
      return rendered.isEmpty() ? "unknown" : rendered;
    }
    String version = firstPartValue(displayName, List.of("Version"));
    return version != null ? name + "@" + version : name;
  }

  private static String firstPartValue(final ComponentDisplayName displayName, final List<String> labels) {
    if (displayName.parts == null) {
      return null;
    }
    for (String label : labels) {
      for (ComponentDisplayNamePart part : displayName.parts) {
        if (part != null && label.equalsIgnoreCase(part.field) && part.value != null && !part.value.isEmpty()) {
          return part.value;
        }
      }
    }
    return null;
  }

  private static String formatConditions(final List<ConditionFact> conditions) {
    StringBuilder builder = new StringBuilder();
    int i = 0;
    while (i < conditions.size()) {
      ConditionFact condition = conditions.get(i);
      String message = clean(condition.getReason());
      if (SECURITY_VULNERABILITY_SEVERITY.equals(condition.getConditionTypeId())) {
        String combined = i + 1 < conditions.size() &&
            SECURITY_VULNERABILITY_SEVERITY.equals(conditions.get(i + 1).getConditionTypeId())
            ? combineReasons(condition, conditions.get(i + 1)) : null;
        if (combined != null) {
          message = combined;
          i++;
        }
        else {
          message = refineReason(condition);
        }
      }
      if (builder.length() > 0) {
        builder.append("; ");
      }
      builder.append(message);
      i++;
    }
    return builder.toString();
  }

  private static String refineReason(final ConditionFact condition) {
    if (condition.getReason() == null) {
      return clean(null);
    }
    Matcher matcher = SECURITY_VULNERABILITY_SEVERITY_PATTERN.matcher(condition.getReason());
    return matcher.matches()
        ? "Found security vulnerability " + matcher.group(1) + " with severity " + matcher.group(2)
        : clean(condition.getReason());
  }

  private static String combineReasons(final ConditionFact first, final ConditionFact second) {
    if (first.getReason() == null || second.getReason() == null) {
      return null;
    }
    Matcher m1 = SECURITY_VULNERABILITY_SEVERITY_PATTERN.matcher(first.getReason());
    Matcher m2 = SECURITY_VULNERABILITY_SEVERITY_PATTERN.matcher(second.getReason());
    if (m1.matches() && m2.matches() && m1.group(1).equals(m2.group(1)) && m1.group(2).equals(m2.group(2))) {
      return "Found security vulnerability " + m1.group(1) + " with severity " + m1.group(2);
    }
    return null;
  }

  /**
   * Values come from the server and from scanned artifacts; keep them on one line and free of control characters
   * (including ESC and the C1 range, which some terminals treat as escape introducers) so they cannot forge log
   * lines or inject terminal commands.
   */
  public static String clean(final String value) {
    return value == null ? "" : CONTROL_CHARACTERS.matcher(value).replaceAll(" ");
  }
}
