/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan;

import java.util.List;

import org.junit.runners.Parameterized;

public class ScanIT_Gradle_Versions_9_0_to_9_7
    extends ScanPluginIntegrationTestBase
{
  @Parameterized.Parameters(name = "Version: {0}")
  public static List<String> data() {
    return List.of("9.0.0", "9.7.1");
  }

  public ScanIT_Gradle_Versions_9_0_to_9_7(final String gradleVersion) {
    super(gradleVersion);
  }
}
