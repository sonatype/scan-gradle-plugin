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

  private final PolicyFact trigger;

  private final List<? extends Action> actions;

  public PolicyAlert(final PolicyFact trigger, final List<? extends Action> actions) {
    this.trigger = trigger;
    this.actions = Collections.unmodifiableList(actions);
  }

  public PolicyFact getTrigger() {
    return trigger;
  }

  public List<? extends Action> getActions() {
    return actions;
  }
}
