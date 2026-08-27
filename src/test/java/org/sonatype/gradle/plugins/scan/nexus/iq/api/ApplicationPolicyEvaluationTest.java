/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.api;

import java.util.Collections;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationPolicyEvaluationTest
{
  @Test
  public void applicationPolicyEvaluation_exposesCounts() {
    ApplicationPolicyEvaluation e = new ApplicationPolicyEvaluation(
        0, 1, 2, 3, 0, 0, 0, 4, 5,
        Collections.emptyList(), "report/url", "prio/url");

    assertThat(e.getCriticalComponentCount()).isEqualTo(1);
    assertThat(e.getSevereComponentCount()).isEqualTo(2);
    assertThat(e.getModerateComponentCount()).isEqualTo(3);
    assertThat(e.getLegacyViolationCount()).isEqualTo(4);
    assertThat(e.getTotalComponentCount()).isEqualTo(5);
    assertThat(e.getApplicationCompositionReportUrl()).isEqualTo("report/url");
    assertThat(e.getPolicyAlerts()).isEmpty();
  }

  @Test
  public void action_constantsAreCorrect() {
    assertThat(Action.ID_FAIL).isEqualTo("fail");
    assertThat(Action.ID_WARN).isEqualTo("warn");
    assertThat(Action.ID_NOTIFY).isEqualTo("notify");
  }

  @Test
  public void action_constructors() {
    Action a1 = new Action("fail");
    assertThat(a1.getActionTypeId()).isEqualTo("fail");
    assertThat(a1.getTarget()).isNull();
    assertThat(a1.getTargetType()).isNull();

    Action a2 = new Action("warn", "target1", "type1");
    assertThat(a2.getActionTypeId()).isEqualTo("warn");
    assertThat(a2.getTarget()).isEqualTo("target1");
    assertThat(a2.getTargetType()).isEqualTo("type1");
  }

  @Test
  public void policyFact_toString() {
    PolicyFact fact = new PolicyFact("id1", "Security-Vulnerability", 9, Collections.emptyList());
    assertThat(fact.toString()).isEqualTo("\nPolicy(Security-Vulnerability) []");
  }
}
