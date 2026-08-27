/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.client.common;

import java.net.URI;
import java.net.URISyntaxException;

import static java.util.Objects.requireNonNull;

/**
 * This class is used to represent the {@code address} and optionally any {@code authentication} information necessary
 * to connect to an external server resource.
 */
public class ServerConfig
{
  private final URI address;

  private final Authentication authentication;

  /**
   * The {@link URI} representing the address is required.
   *
   * @param address the URI of the external resource.
   * @throws URISyntaxException   if {@code address} is not valid.
   * @throws NullPointerException if {@code address} not provided.
   */
  public ServerConfig(final URI address) throws URISyntaxException {
    this(address, (Authentication) null);
  }

  /**
   * The {@link URI} representing the address is required.
   *
   * @param address        the URI of the external resource.
   * @param authentication the authentication details.
   * @throws NullPointerException if {@code address} not provided.
   */
  public ServerConfig(final URI address, final Authentication authentication) {
    requireNonNull(address, "Address must not be null");
    this.address = address.getPath().endsWith("/") ? address : address.resolve(address.getPath() + "/").normalize();
    this.authentication = authentication;
  }

  /**
   * @return the URI of the external resource.
   */
  public URI getAddress() {
    return address.normalize();
  }

  /**
   * @return the authentication details.
   */
  public Authentication getAuthentication() {
    return authentication;
  }
}
