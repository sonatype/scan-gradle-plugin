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
public class Action
    implements Serializable
{
  private static final long serialVersionUID = 559865623400986951L;

  public static final String ID_FAIL = "fail";

  public static final String ID_WARN = "warn";

  public static final String ID_NOTIFY = "notify";

  private final String actionTypeId;

  private final String target;

  private final String targetType;

  public Action(final String actionTypeId) {
    this(actionTypeId, null, null);
  }

  public Action(final String actionTypeId, final String target, final String targetType) {
    this.actionTypeId = actionTypeId;
    this.target = target;
    this.targetType = targetType;
  }

  public String getActionTypeId() {
    return actionTypeId;
  }

  public String getTarget() {
    return target;
  }

  public String getTargetType() {
    return targetType;
  }
}
