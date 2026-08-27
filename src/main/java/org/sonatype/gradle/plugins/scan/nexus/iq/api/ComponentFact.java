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

  /**
   * The component identifier.
   */
  private final ComponentIdentifier componentIdentifier;

  /**
   * The component hash.
   */
  private final String hash;

  /**
   * The list of constraint facts.
   */
  private final List<ConstraintFact> constraintFacts;

  /**
   * The list of path names.
   */
  private final List<String> pathNames;

  /**
   * The display name.
   */
  private final ComponentDisplayName displayName;

  /**
   * Creates a new ComponentFact with the given parameters.
   */
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

  /**
   * Returns the hash.
   */
  public String getHash() {
    return hash;
  }

  /**
   * Returns the constraint facts.
   */
  public List<ConstraintFact> getConstraintFacts() {
    return constraintFacts;
  }

  /**
   * Returns the path names.
   */
  public List<String> getPathNames() {
    return pathNames;
  }

  /**
   * Returns the display name.
   */
  public ComponentDisplayName getDisplayName() {
    return displayName;
  }

  /**
   * Returns the component identifier.
   */
  public ComponentIdentifier getComponentIdentifier() {
    return componentIdentifier;
  }

  @Override
  public String toString() {
    return "\n Component(displayName=" + displayName + ", hash=" + hash + ") " + constraintFacts;
  }
}
