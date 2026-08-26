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

  private final List<PolicyAlert> policyAlerts;

  private final int affectedComponentCount;

  private final int criticalComponentCount;

  private final int severeComponentCount;

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

  public int getAffectedComponentCount() {
    return affectedComponentCount;
  }

  public int getCriticalComponentCount() {
    return criticalComponentCount;
  }

  public int getSevereComponentCount() {
    return severeComponentCount;
  }

  public int getModerateComponentCount() {
    return moderateComponentCount;
  }

  public int getCriticalPolicyViolationCount() {
    return criticalPolicyViolationCount;
  }

  public int getSeverePolicyViolationCount() {
    return severePolicyViolationCount;
  }

  public int getModeratePolicyViolationCount() {
    return moderatePolicyViolationCount;
  }

  public int getLegacyViolationCount() {
    return legacyViolationCount;
  }

  public int getTotalComponentCount() {
    return totalComponentCount;
  }

  public List<PolicyAlert> getPolicyAlerts() {
    return policyAlerts;
  }

  public boolean hasFailures() {
    return hasActionOfType(Action.ID_FAIL);
  }

  public boolean hasWarnings() {
    return hasActionOfType(Action.ID_WARN);
  }

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
