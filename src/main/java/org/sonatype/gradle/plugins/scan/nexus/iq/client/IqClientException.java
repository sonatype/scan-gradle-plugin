/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.client;

/**
 * Exception thrown by IQ client operations.
 */
public class IqClientException
    extends Exception
{
  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message.
   */
  public IqClientException(final String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the detail message.
   * @param cause   the cause.
   */
  public IqClientException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
