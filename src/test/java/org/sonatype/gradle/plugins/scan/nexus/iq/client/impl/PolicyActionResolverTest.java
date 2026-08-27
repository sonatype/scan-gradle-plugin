/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.client.impl;

import java.util.Arrays;
import java.util.Collections;

import com.sonatype.insight.brain.client.PolicyAction;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.Action;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.PolicyAlert;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.PolicyFact;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PolicyActionResolver}.
 */
public class PolicyActionResolverTest
{
  private final PolicyActionResolver resolver = new PolicyActionResolver();

  @Test
  public void emptyAlertsReturnsNone() {
    PolicyAction result = resolver.resolve(Collections.emptyList());
    assertThat(result).isEqualTo(PolicyAction.NONE);
  }

  @Test
  public void alertWithFailActionReturnsFail() {
    PolicyFact trigger = new PolicyFact("policy-id", "policy-name", 5, Collections.emptyList());
    PolicyAlert alert = new PolicyAlert(trigger, Arrays.asList(new Action(Action.ID_FAIL)));

    PolicyAction result = resolver.resolve(Arrays.asList(alert));
    assertThat(result).isEqualTo(PolicyAction.FAIL);
  }

  @Test
  public void alertWithOnlyWarnActionReturnsWarn() {
    PolicyFact trigger = new PolicyFact("policy-id", "policy-name", 5, Collections.emptyList());
    PolicyAlert alert = new PolicyAlert(trigger, Arrays.asList(new Action(Action.ID_WARN)));

    PolicyAction result = resolver.resolve(Arrays.asList(alert));
    assertThat(result).isEqualTo(PolicyAction.WARN);
  }

  @Test
  public void failTakesPrecedenceOverWarn() {
    PolicyFact trigger = new PolicyFact("policy-id", "policy-name", 5, Collections.emptyList());
    PolicyAlert alertWithWarn = new PolicyAlert(trigger, Arrays.asList(new Action(Action.ID_WARN)));
    PolicyAlert alertWithFail = new PolicyAlert(trigger, Arrays.asList(new Action(Action.ID_FAIL)));

    PolicyAction result = resolver.resolve(Arrays.asList(alertWithWarn, alertWithFail));
    assertThat(result).isEqualTo(PolicyAction.FAIL);
  }
}
