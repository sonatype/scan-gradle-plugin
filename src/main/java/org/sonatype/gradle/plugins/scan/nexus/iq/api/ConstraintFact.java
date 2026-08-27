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
public class ConstraintFact
    implements Serializable
{
  private static final long serialVersionUID = 4898080572741007698L;

  /**
   * The constraint identifier.
   */
  private final String constraintId;

  /**
   * The constraint name.
   */
  private final String constraintName;

  /**
   * The operator name.
   */
  private final String operatorName;

  /**
   * The list of condition facts.
   */
  private final List<ConditionFact> conditionFacts;

  /**
   * Creates a new ConstraintFact with the given parameters.
   */
  public ConstraintFact(
      final String constraintId,
      final String constraintName,
      final String operatorName,
      final List<ConditionFact> conditionFacts)
  {
    this.constraintId = constraintId;
    this.constraintName = constraintName;
    this.operatorName = operatorName;
    this.conditionFacts = Collections.unmodifiableList(conditionFacts);
  }

  /**
   * Returns the constraint identifier.
   */
  public String getConstraintId() {
    return constraintId;
  }

  /**
   * Returns the constraint name.
   */
  public String getConstraintName() {
    return constraintName;
  }

  /**
   * Returns the operator name.
   */
  public String getOperatorName() {
    return operatorName;
  }

  /**
   * Returns the condition facts.
   */
  public List<ConditionFact> getConditionFacts() {
    return conditionFacts;
  }

  @Override
  public String toString() {
    return "\n  Constraint(" + constraintName + ") " + conditionFacts + " ";
  }
}
