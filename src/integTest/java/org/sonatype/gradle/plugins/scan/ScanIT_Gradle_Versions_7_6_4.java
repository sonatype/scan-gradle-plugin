/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan;

import java.util.List;

import org.junit.runners.Parameterized;

public class ScanIT_Gradle_Versions_7_6_4
    extends ScanPluginIntegrationTestBase
{
  @Parameterized.Parameters(name = "Version: {0}")
  public static List<String> data() {
    return List.of("7.6.4");
  }

  public ScanIT_Gradle_Versions_7_6_4(final String gradleVersion) {
    super(gradleVersion);
  }
}
