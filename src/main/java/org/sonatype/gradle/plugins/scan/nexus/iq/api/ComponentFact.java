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
public class ComponentFact
    implements Serializable
{
  private static final long serialVersionUID = -9042385304532875926L;

  private final ComponentIdentifier componentIdentifier;

  private final String hash;

  private final List<ConstraintFact> constraintFacts;

  private final List<String> pathNames;

  private final ComponentDisplayName displayName;

  public ComponentFact(
      final ComponentIdentifier componentIdentifier,
      final String hash,
      final List<ConstraintFact> constraintFacts,
      final List<String> pathNames,
      final ComponentDisplayName displayName)
  {
    this.componentIdentifier = componentIdentifier;
    this.hash = hash;
    this.constraintFacts = Collections.unmodifiableList(constraintFacts);
    this.pathNames = Collections.unmodifiableList(pathNames);
    this.displayName = displayName;
  }

  public String getHash() {
    return hash;
  }

  public List<ConstraintFact> getConstraintFacts() {
    return constraintFacts;
  }

  public List<String> getPathNames() {
    return pathNames;
  }

  public ComponentDisplayName getDisplayName() {
    return displayName;
  }

  public ComponentIdentifier getComponentIdentifier() {
    return componentIdentifier;
  }

  @Override
  public String toString() {
    return "\n Component(displayName=" + displayName + ", hash=" + hash + ") " + constraintFacts;
  }
}
