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
public class ComponentDisplayName
    implements Serializable
{
  private static final long serialVersionUID = -7005158197393657122L;

  /**
   * The list of parts that make up the display name.
   */
  public final List<ComponentDisplayNamePart> parts;

  /**
   * Creates a new ComponentDisplayName with the given parts.
   */
  public ComponentDisplayName(final List<ComponentDisplayNamePart> parts) {
    this.parts = Collections.unmodifiableList(parts);
  }

  @Override
  public String toString() {
    StringBuilder joiner = new StringBuilder();
    if (parts != null) {
      for (ComponentDisplayNamePart part : parts) {
        if (part != null && part.value != null) {
          joiner.append(part.value);
        }
      }
    }
    return joiner.toString();
  }
}
