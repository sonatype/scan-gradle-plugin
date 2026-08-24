/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan;

import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

public class ScanIT_Gradle_Versions_8_4_to_8_5
    extends ScanPluginIntegrationTestBase
{
  @Parameterized.Parameters(name = "Version: {0}")
  public static List<String> data() {
    return List.of("8.4", "8.5");
  }

  public ScanIT_Gradle_Versions_8_4_to_8_5(final String gradleVersion) {
    super(gradleVersion);
  }
}
