/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.sonatype.ossindex.service.client.internal.Version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for plugin version information.
 */
public class PluginVersionUtils
{
  private static final Logger log = LoggerFactory.getLogger(PluginVersionUtils.class);

  private static final String PROPERTIES_PATH = "com/sonatype/insight/client.properties";

  private PluginVersionUtils() {
    // Utils class
  }

  /**
   * Returns the current plugin version.
   */
  public static String getPluginVersion() {
    String pluginVersion = Version.UNKNOWN;
    try (InputStream stream = PluginVersionUtils.class.getClassLoader().getResourceAsStream(PROPERTIES_PATH)) {
      Properties properties = new Properties();
      properties.load(stream);
      pluginVersion = properties.getProperty("version", Version.UNKNOWN);
    }
    catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    return pluginVersion;
  }
}
