/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.ossindex;

import java.io.File;

import org.sonatype.ossindex.service.client.OssindexClientConfiguration;
import org.sonatype.ossindex.service.client.cache.DirectoryCache;
import org.sonatype.ossindex.service.client.transport.AuthConfiguration;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder for OSS Index client configuration.
 */
public class OssIndexClientConfigurationBuilder
{
  private final Logger log = LoggerFactory.getLogger(OssIndexClientConfigurationBuilder.class);

  /**
   * Builds an OSS Index client configuration from the given extension settings.
   */
  public OssindexClientConfiguration build(OssIndexPluginExtension extension) {
    OssindexClientConfiguration clientConfiguration = new OssindexClientConfiguration();

    if (extension != null) {
      if (StringUtils.isNoneBlank(extension.getUsername(), extension.getPassword())) {
        clientConfiguration
            .setAuthConfiguration(new AuthConfiguration(extension.getUsername(), extension.getPassword()));
      }
      else {
        log.info("Using anonymous request");
      }

      if (extension.isUseCache()) {
        DirectoryCache.Configuration cacheConfig = new DirectoryCache.Configuration();

        if (StringUtils.isNotBlank(extension.getCacheDirectory())) {
          File cacheDirectory = new File(extension.getCacheDirectory());
          cacheConfig.setBaseDir(cacheDirectory.toPath());
        }

        if (StringUtils.isNotBlank(extension.getCacheExpiration())) {
          try {
            cacheConfig.setExpireAfter(Duration.parse(extension.getCacheExpiration()));
          }
          catch (IllegalArgumentException e) {
            log.warn(
                "Invalid cache duration value: {}. Please read https://www.javadoc.io/doc/joda-time/joda-time/2.10.4/org/joda/time/Duration.html#parse-java.lang.String-",
                extension.getCacheExpiration());
          }
        }

        clientConfiguration.setCacheConfiguration(cacheConfig);
      }

      clientConfiguration.setProxyConfiguration(extension.getProxyConfiguration());
    }

    return clientConfiguration;
  }
}
