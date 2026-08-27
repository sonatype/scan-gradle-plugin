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
public class PolicyEvaluationResult
    implements Serializable
{
  private static final long serialVersionUID = 8297359987633171422L;

  /**
   * List of policy alerts from the evaluation.
   */
  private final List<PolicyAlert> policyAlerts;

  /**
   * Count of components affected by policy violations.
   */
  private final int affectedComponentCount;

  /**
   * Count of components with critical severity violations.
   */
  private final int criticalComponentCount;

  /**
   * Count of components with severe violations.
   */
  private final int severeComponentCount;

  /**
   * Count of components with moderate violations.
   */
  private final int moderateComponentCount;

  /**
   * @since 3.15
   */
  private final int criticalPolicyViolationCount;

  /**
   * @since 3.15
   */
  private final int severePolicyViolationCount;

  /**
   * @since 3.15
   */
  private final int moderatePolicyViolationCount;

  /**
   * @since 3.2
   */
  private final int legacyViolationCount;

  /**
   * @since 3.28
   */
  private final int totalComponentCount;

  /**
   * Constructs a policy evaluation result with the given counts and alerts.
   */
  public PolicyEvaluationResult(
      final int affectedComponentCount,
      final int criticalComponentCount,
      final int severeComponentCount,
      final int moderateComponentCount,
      final int criticalPolicyViolationCount,
      final int severePolicyViolationCount,
      final int moderatePolicyViolationCount,
      final int legacyViolationCount,
      final int totalComponentCount,
      final List<PolicyAlert> policyAlerts)
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
  }

  /**
   * Returns the count of affected components.
   */
  public int getAffectedComponentCount() {
    return affectedComponentCount;
  }

  /**
   * Returns the count of critical components.
   */
  public int getCriticalComponentCount() {
    return criticalComponentCount;
  }

  /**
   * Returns the count of severe components.
   */
  public int getSevereComponentCount() {
    return severeComponentCount;
  }

  /**
   * Returns the count of moderate components.
   */
  public int getModerateComponentCount() {
    return moderateComponentCount;
  }

  /**
   * Returns the count of critical policy violations.
   */
  public int getCriticalPolicyViolationCount() {
    return criticalPolicyViolationCount;
  }

  /**
   * Returns the count of severe policy violations.
   */
  public int getSeverePolicyViolationCount() {
    return severePolicyViolationCount;
  }

  /**
   * Returns the count of moderate policy violations.
   */
  public int getModeratePolicyViolationCount() {
    return moderatePolicyViolationCount;
  }

  /**
   * Returns the count of legacy violations.
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
   * Returns the list of policy alerts.
   */
  public List<PolicyAlert> getPolicyAlerts() {
    return policyAlerts;
  }

  /**
   * Returns whether the result contains failures.
   */
  public boolean hasFailures() {
    return hasActionOfType(Action.ID_FAIL);
  }

  /**
   * Returns whether the result contains warnings.
   */
  public boolean hasWarnings() {
    return hasActionOfType(Action.ID_WARN);
  }

  /**
   * Returns whether the result contains notifications.
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
