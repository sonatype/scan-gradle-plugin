/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.ossindex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.sonatype.gradle.plugins.scan.common.PluginVersionUtils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for creating ASCII art banners.
 */
public class BannerUtils
{
  private static final Logger log = LoggerFactory.getLogger(BannerUtils.class);

  private static final String HEADER_PATH = "org/sonatype/gradle/plugins/scan/ossindex/banner.txt";

  private BannerUtils() {
    // Utils class
  }

  /**
   * Creates and returns the plugin banner string.
   */
  public static String createBanner() {
    StringBuilder sb = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(BannerUtils.class.getClassLoader().getResourceAsStream(HEADER_PATH)))) {

      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line).append(System.lineSeparator());
      }

      sb.append("Gradle Scan version: ")
          .append(PluginVersionUtils.getPluginVersion())
          .append(System.lineSeparator());

      sb.append(StringUtils.repeat("-", 150))
          .append(System.lineSeparator());
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    return sb.toString();
  }
}
