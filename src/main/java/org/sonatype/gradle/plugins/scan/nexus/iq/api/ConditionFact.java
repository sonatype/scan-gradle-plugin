/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.api;

import java.io.Serializable;

/**
 * @since 1.0.1
 */
public class ConditionFact
    implements Serializable
{
  private static final long serialVersionUID = 938533952225092145L;

  private final String conditionTypeId;

  private final String summary;

  private final String reason;

  public ConditionFact(final String conditionTypeId, final String summary, final String reason) {
    this.conditionTypeId = conditionTypeId;
    this.summary = summary;
    this.reason = reason;
  }

  public String getConditionTypeId() {
    return conditionTypeId;
  }

  public String getSummary() {
    return summary;
  }

  public String getReason() {
    return reason;
  }

  @Override
  public String toString() {
    return summary + " because: " + reason;
  }
}
