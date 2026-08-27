/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.api;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * @since 1.0.1
 */
public class ApplicationPolicyEvaluation
    implements Serializable
{
  private static final long serialVersionUID = -2325945279783644170L;

  /**
   * The affected component count.
   */
  private final int affectedComponentCount;

  /**
   * The critical component count.
   */
  private final int criticalComponentCount;

  /**
   * The severe component count.
   */
  private final int severeComponentCount;

  /**
   * The moderate component count.
   */
  private final int moderateComponentCount;

  /**
   * The critical policy violation count.
   */
  private final int criticalPolicyViolationCount;

  /**
   * The severe policy violation count.
   */
  private final int severePolicyViolationCount;

  /**
   * The moderate policy violation count.
   */
  private final int moderatePolicyViolationCount;

  /**
   * The legacy violation count.
   */
  private final int legacyViolationCount;

  /**
   * The total component count.
   */
  private final int totalComponentCount;

  /**
   * The policy alerts.
   */
  private final List<PolicyAlert> policyAlerts;

  /**
   * The application composition report URL.
   */
  private final String applicationCompositionReportUrl;

  /**
   * The application priorities URL.
   */
  private final String applicationPrioritiesUrl;

  /**
   * Constructs an ApplicationPolicyEvaluation.
   */
  public ApplicationPolicyEvaluation(
      final int affectedComponentCount,
      final int criticalComponentCount,
      final int severeComponentCount,
      final int moderateComponentCount,
      final int criticalPolicyViolationCount,
      final int severePolicyViolationCount,
      final int moderatePolicyViolationCount,
      final int legacyViolationCount,
      final int totalComponentCount,
      final List<PolicyAlert> policyAlerts,
      final String applicationCompositionReportUrl,
      final String applicationPrioritiesUrl)
  {
    this.affectedComponentCount = affectedComponentCount;
    this.criticalComponentCount = criticalComponentCount;
    this.severeComponentCount = severeComponentCount;
    this.moderateComponentCount = moderateComponentCount;
    this.criticalPolicyViolationCount = criticalPolicyViolationCount;
    this.severePolicyViolationCount = severePolicyViolationCount;
    this.moderatePolicyViolationCount = moderatePolicyViolationCount;
    this.legacyViolationCount = legacyViolationCount;
    this.totalComponentCount = totalComponentCount;
    this.policyAlerts = Collections.unmodifiableList(policyAlerts);
    this.applicationCompositionReportUrl = applicationCompositionReportUrl;
    this.applicationPrioritiesUrl = applicationPrioritiesUrl;
  }

  /**
   * Returns the affected component count.
   */
  public int getAffectedComponentCount() {
    return affectedComponentCount;
  }

  /**
   * Returns the critical component count.
   */
  public int getCriticalComponentCount() {
    return criticalComponentCount;
  }

  /**
   * Returns the severe component count.
   */
  public int getSevereComponentCount() {
    return severeComponentCount;
  }

  /**
   * Returns the moderate component count.
   */
  public int getModerateComponentCount() {
    return moderateComponentCount;
  }

  /**
   * Returns the critical policy violation count.
   */
  public int getCriticalPolicyViolationCount() {
    return criticalPolicyViolationCount;
  }

  /**
   * Returns the severe policy violation count.
   */
  public int getSeverePolicyViolationCount() {
    return severePolicyViolationCount;
  }

  /**
   * Returns the moderate policy violation count.
   */
  public int getModeratePolicyViolationCount() {
    return moderatePolicyViolationCount;
  }

  /**
   * Returns the legacy violation count.
   */
  public int getLegacyViolationCount() {
    return legacyViolationCount;
  }

  /**
   * Returns the total component count.
   */
  public int getTotalComponentCount() {
    return totalComponentCount;
  }

  /**
   * Returns the policy alerts.
   */
  public List<PolicyAlert> getPolicyAlerts() {
    return policyAlerts;
  }

  /**
   * Returns the application composition report URL.
   */
  public String getApplicationCompositionReportUrl() {
    return applicationCompositionReportUrl;
  }

  /**
   * Returns the application priorities URL.
   */
  public String getApplicationPrioritiesUrl() {
    return applicationPrioritiesUrl;
  }

  /**
   * Returns whether there are any failures.
   */
  public boolean hasFailures() {
    return hasActionOfType(Action.ID_FAIL);
  }

  /**
   * Returns whether there are any warnings.
   */
  public boolean hasWarnings() {
    return hasActionOfType(Action.ID_WARN);
  }

  /**
   * Returns whether there are any notifications.
   */
  public boolean hasNotifications() {
    return hasActionOfType(Action.ID_NOTIFY);
  }

  private boolean hasActionOfType(final String actionTypeId) {
    for (PolicyAlert policyAlert : policyAlerts) {
      for (Action action : policyAlert.getActions()) {
        if (actionTypeId.equals(action.getActionTypeId())) {
          return true;
        }
      }
    }
    return false;
  }
}
