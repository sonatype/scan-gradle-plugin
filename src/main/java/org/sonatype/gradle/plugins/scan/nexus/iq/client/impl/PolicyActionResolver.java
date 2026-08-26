/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.client.impl;

import java.util.List;

import com.sonatype.insight.brain.client.PolicyAction;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.Action;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.PolicyAlert;

/**
 * @since 1.0.1
 */
public class PolicyActionResolver
{
  public PolicyAction resolve(List<PolicyAlert> alerts) {
    PolicyAction outcome = PolicyAction.NONE;
    for (PolicyAlert alert : alerts) {
      for (Action action : alert.getActions()) {
        String actionTypeId = action.getActionTypeId();
        if (Action.ID_FAIL.equals(actionTypeId)) {
          outcome = outcome.combine(PolicyAction.FAIL);
        }
        else if (Action.ID_WARN.equals(actionTypeId)) {
          outcome = outcome.combine(PolicyAction.WARN);
        }
      }
    }
    return outcome;
  }
}
