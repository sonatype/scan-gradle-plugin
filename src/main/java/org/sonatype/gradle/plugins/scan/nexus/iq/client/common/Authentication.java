/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.client.common;

import java.io.Serial;
import java.io.Serializable;

import static java.util.Objects.requireNonNull;

/**
 * This class is an abstraction of authentication credentials. Internally passwords are stored as {@code char[]} to
 * avoid logging any sensitive information as well as allowing the objects to be garbage collected properly.
 */
public class Authentication
    implements Serializable
{
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * The username for authentication.
   */
  private final String username;

  /**
   * The password for authentication.
   */
  private final char[] password;

  /**
   * Both {@code username} and {@code password} are required.
   *
   * @param username the username.
   * @param password the password.
   * @throws NullPointerException if either {@code username} or {@code password} not provided.
   */
  public Authentication(final String username, final char[] password) {
    requireNonNull(username, "Username is required.");
    requireNonNull(password, "Password is required.");

    this.username = username;
    this.password = password;
  }

  /**
   * Both {@code username} and {@code password} are required.
   *
   * @param username the username.
   * @param password the password.
   * @throws NullPointerException if either {@code username} or {@code password} not provided.
   */
  public Authentication(final String username, final String password) {
    requireNonNull(username, "Username is required.");
    requireNonNull(password, "Password is required.");

    this.username = username;
    this.password = password.toCharArray();
  }

  /**
   * @return the username.
   */
  public String getUsername() {
    return username;
  }

  /**
   * @return the password.
   */
  public char[] getPassword() {
    return password;
  }
}
