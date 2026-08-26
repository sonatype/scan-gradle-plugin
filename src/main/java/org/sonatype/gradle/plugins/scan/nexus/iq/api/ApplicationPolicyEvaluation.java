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

  private final int affectedComponentCount;

  private final int criticalComponentCount;

  private final int severeComponentCount;

  private final int moderateComponentCount;

  private final int criticalPolicyViolationCount;

  private final int severePolicyViolationCount;

  private final int moderatePolicyViolationCount;

  private final int legacyViolationCount;

  private final int totalComponentCount;

  private final List<PolicyAlert> policyAlerts;

  private final String applicationCompositionReportUrl;

  private final String applicationPrioritiesUrl;

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

  public String getApplicationCompositionReportUrl() {
    return applicationCompositionReportUrl;
  }

  public String getApplicationPrioritiesUrl() {
    return applicationPrioritiesUrl;
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
