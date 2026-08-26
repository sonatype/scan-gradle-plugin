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
public class ComponentDisplayNamePart
    implements Serializable
{
  private static final long serialVersionUID = 4543461576396548761L;

  public final String field;

  public final String value;

  public ComponentDisplayNamePart(final String field, final String value) {
    this.field = field;
    this.value = value;
  }
}
