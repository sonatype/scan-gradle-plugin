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
public class PolicyAlert
    implements Serializable
{
  private static final long serialVersionUID = 3296155389738750550L;

  /**
   * The trigger policy fact.
   */
  private final PolicyFact trigger;

  /**
   * The actions associated with this policy alert.
   */
  private final List<? extends Action> actions;

  /**
   * Constructs a PolicyAlert.
   */
  public PolicyAlert(final PolicyFact trigger, final List<? extends Action> actions) {
    this.trigger = trigger;
    this.actions = Collections.unmodifiableList(actions);
  }

  /**
   * Returns the trigger policy fact.
   */
  public PolicyFact getTrigger() {
    return trigger;
  }

  /**
   * Returns the actions associated with this policy alert.
   */
  public List<? extends Action> getActions() {
    return actions;
  }
}
