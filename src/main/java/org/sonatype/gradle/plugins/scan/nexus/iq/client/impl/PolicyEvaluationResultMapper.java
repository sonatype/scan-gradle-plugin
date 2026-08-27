/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.client.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.sonatype.gradle.plugins.scan.nexus.iq.api.Action;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ComponentDisplayName;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ComponentDisplayNamePart;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ComponentFact;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ComponentIdentifier;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ConditionFact;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ConstraintFact;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.PolicyAlert;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.PolicyEvaluationResult;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.PolicyFact;

/**
 * @since 1.0.1
 */
public final class PolicyEvaluationResultMapper
{
  private PolicyEvaluationResultMapper() {
  }

  /**
   * Maps a policy evaluation result from the IQ DTO model to the plugin's API model.
   */
  public static PolicyEvaluationResult map(final com.sonatype.clm.dto.model.policy.PolicyEvaluationResult input) {
    List<PolicyAlert> alerts = input.getAlerts().stream().map(PolicyEvaluationResultMapper::map)
        .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    return new PolicyEvaluationResult(input.getAffectedComponentCount(), input.getCriticalComponentCount(),
        input.getSevereComponentCount(), input.getModerateComponentCount(),
        input.getCriticalPolicyViolationCount(), input.getSeverePolicyViolationCount(),
        input.getModeratePolicyViolationCount(),
        input.getLegacyViolationCount(), input.getTotalComponentCount(), alerts);
  }

  private static PolicyAlert map(final com.sonatype.clm.dto.model.policy.PolicyAlert input) {
    List<Action> actions = input.getActions().stream().map(PolicyEvaluationResultMapper::map)
        .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    return new PolicyAlert(map(input.getTrigger()), actions);
  }

  private static Action map(final com.sonatype.clm.dto.model.policy.Action action) {
    return new Action(action.getActionTypeId(), action.getTarget(), action.getTargetType());
  }

  private static PolicyFact map(final com.sonatype.clm.dto.model.policy.PolicyFact input) {
    if (input == null) {
      return null;
    }
    List<ComponentFact> componentFacts =
        input.getComponentFacts() != null
            ? input.getComponentFacts().stream().map(PolicyEvaluationResultMapper::map)
            .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList))
            : Collections.emptyList();
    return new PolicyFact(input.getPolicyId(), input.getPolicyName(), input.getThreatLevel(), componentFacts);
  }

  private static ComponentFact map(final com.sonatype.clm.dto.model.policy.ComponentFact input) {
    List<ConstraintFact> constraintFacts = input.getConstraintFacts().stream().map(PolicyEvaluationResultMapper::map)
        .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    ComponentIdentifier componentIdentifier = map(input.getComponentIdentifier());
    ComponentDisplayName displayName = map(input.getDisplayName());
    return new ComponentFact(componentIdentifier, input.getHash(), constraintFacts, input.getPathnames(), displayName);
  }

  private static ConstraintFact map(final com.sonatype.clm.dto.model.policy.ConstraintFact input) {
    List<ConditionFact> conditionFacts = input.getConditionFacts().stream().map(PolicyEvaluationResultMapper::map)
        .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    return new ConstraintFact(input.getConstraintId(), input.getConstraintName(), input.getOperatorName(),
        conditionFacts);
  }

  private static ConditionFact map(final com.sonatype.clm.dto.model.policy.ConditionFact input) {
    return new ConditionFact(input.getConditionTypeId(), input.getSummary(), input.getReason());
  }

  private static ComponentDisplayName map(
      final com.sonatype.clm.dto.model.component.ComponentDisplayName input)
  {
    if (input == null || input.parts == null) {
      return null;
    }
    List<ComponentDisplayNamePart> displayNameParts = input.parts.stream().map(PolicyEvaluationResultMapper::map)
        .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    return new ComponentDisplayName(displayNameParts);
  }

  private static ComponentDisplayNamePart map(
      final com.sonatype.clm.dto.model.component.ComponentDisplayNamePart input)
  {
    return new ComponentDisplayNamePart(input.field, input.value);
  }

  private static ComponentIdentifier map(
      final com.sonatype.clm.dto.model.component.ComponentIdentifier input)
  {
    return input == null ? null : new ComponentIdentifier(input.getFormat(), input.getCoordinates());
  }
}
