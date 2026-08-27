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
public class PolicyFact
    implements Serializable
{
  private static final long serialVersionUID = 7109576543471007652L;

  /**
   * The policy ID.
   */
  private final String policyId;

  /**
   * The policy name.
   */
  private final String policyName;

  /**
   * The threat level.
   */
  private final int threatLevel;

  /**
   * The component facts.
   */
  private final List<ComponentFact> componentFacts;

  /**
   * Constructs a PolicyFact.
   */
  public PolicyFact(
      final String policyId,
      final String policyName,
      final int threatLevel,
      final List<ComponentFact> componentFacts)
  {
    this.policyId = policyId;
    this.policyName = policyName;
    this.threatLevel = threatLevel;
    this.componentFacts = Collections.unmodifiableList(componentFacts);
  }

  /**
   * Returns the policy ID.
   */
  public String getPolicyId() {
    return policyId;
  }

  /**
   * Returns the policy name.
   */
  public String getPolicyName() {
    return policyName;
  }

  /**
   * Returns the threat level.
   */
  public int getThreatLevel() {
    return threatLevel;
  }

  /**
   * Returns the component facts.
   */
  public List<ComponentFact> getComponentFacts() {
    return componentFacts;
  }

  @Override
  public String toString() {
    return "\nPolicy(" + policyName + ") " + componentFacts;
  }
}
