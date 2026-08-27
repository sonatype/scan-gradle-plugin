/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.api;

import java.io.Serializable;
import java.util.Collections;
import java.util.SortedMap;

/**
 * @since 1.0.1
 */
public class ComponentIdentifier
    implements Serializable
{
  private static final long serialVersionUID = -2228730540775558696L;

  /**
   * The component format.
   */
  private final String format;

  /**
   * The component coordinates.
   */
  private final SortedMap<String, String> coordinates;

  /**
   * Creates a new ComponentIdentifier with the given format and coordinates.
   */
  public ComponentIdentifier(final String format, final SortedMap<String, String> coordinates) {
    this.format = format;
    this.coordinates = Collections.unmodifiableSortedMap(coordinates);
  }

  /**
   * Returns the format.
   */
  public String getFormat() {
    return format;
  }

  /**
   * Returns the coordinates.
   */
  public SortedMap<String, String> getCoordinates() {
    return coordinates;
  }

  @Override
  public String toString() {
    return format + ": " + coordinates;
  }
}
