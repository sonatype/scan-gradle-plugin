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

  /**
   * Action type identifier for fail action.
   */
  public static final String ID_FAIL = "fail";

  /**
   * Action type identifier for warn action.
   */
  public static final String ID_WARN = "warn";

  /**
   * Action type identifier for notify action.
   */
  public static final String ID_NOTIFY = "notify";

  /**
   * The action type identifier.
   */
  private final String actionTypeId;

  /**
   * The target of the action.
   */
  private final String target;

  /**
   * The type of the target.
   */
  private final String targetType;

  /**
   * Creates a new Action with the given action type identifier.
   */
  public Action(final String actionTypeId) {
    this(actionTypeId, null, null);
  }

  /**
   * Creates a new Action with the given action type identifier, target, and target type.
   */
  public Action(final String actionTypeId, final String target, final String targetType) {
    this.actionTypeId = actionTypeId;
    this.target = target;
    this.targetType = targetType;
  }

  /**
   * Returns the action type identifier.
   */
  public String getActionTypeId() {
    return actionTypeId;
  }

  /**
   * Returns the target.
   */
  public String getTarget() {
    return target;
  }

  /**
   * Returns the target type.
   */
  public String getTargetType() {
    return targetType;
  }
}
