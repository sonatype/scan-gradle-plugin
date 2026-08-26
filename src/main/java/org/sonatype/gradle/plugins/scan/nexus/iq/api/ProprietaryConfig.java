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
import java.util.Properties;

/**
 * Contains lists of packages and regular expressions. When a component matches a package name or regex it is considered
 * proprietary.
 *
 * @since 1.0.1
 */
public class ProprietaryConfig
    implements Serializable
{
  private static final long serialVersionUID = -7657413812830158270L;

  private final List<String> packages;

  private final List<String> regexes;

  public ProprietaryConfig(final List<String> packages, final List<String> regexes) {
    this.packages = Collections.unmodifiableList(packages);
    this.regexes = Collections.unmodifiableList(regexes);
  }

  public List<String> getPackages() {
    return packages;
  }

  public List<String> getRegexes() {
    return regexes;
  }

  public Properties toProperties() {
    Properties properties = new Properties();
    properties.put("proprietaryPackages", String.join(",", packages));
    properties.put("proprietaryRegexes", String.join(":::", regexes));
    return properties;
  }
}
