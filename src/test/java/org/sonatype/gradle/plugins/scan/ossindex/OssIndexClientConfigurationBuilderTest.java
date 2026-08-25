/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.ossindex;

import java.nio.file.Paths;

import org.sonatype.ossindex.service.client.OssindexClientConfiguration;
import org.sonatype.ossindex.service.client.cache.DirectoryCache;
import org.sonatype.ossindex.service.client.cache.DirectoryCache.Configuration;
import org.sonatype.ossindex.service.client.transport.AuthConfiguration;
import org.sonatype.ossindex.service.client.transport.ProxyConfiguration;

import groovy.lang.Closure;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OssIndexClientConfigurationBuilderTest
{
  private OssIndexClientConfigurationBuilder builder;

  @Before
  public void setup() {
    builder = new OssIndexClientConfigurationBuilder();
  }

  @Test
  public void testBuild_nullExtension() {
    OssindexClientConfiguration expected = new OssindexClientConfiguration();
    OssindexClientConfiguration result = builder.build(null);

    assertThat(result).isNotNull();
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  public void testBuild_defaultValuesExtension() {
    OssindexClientConfiguration expected = new OssindexClientConfiguration();
    OssindexClientConfiguration result = builder.build(new OssIndexPluginExtension(null));

    assertThat(result).isNotNull();
    assertThat(result).usingRecursiveComparison().ignoringFields("cacheConfiguration").isEqualTo(expected);
    assertThat(result.getCacheConfiguration()).isInstanceOf(DirectoryCache.Configuration.class);
    assertThat(result.getProxyConfiguration()).isNull();
  }

  @Test
  public void testBuild_customValuesExtension() {
    OssIndexPluginExtension extension = new OssIndexPluginExtension(null);
    extension.setUsername("username");
    extension.setPassword("password");
    extension.setUseCache(false);

    OssindexClientConfiguration result = builder.build(extension);

    assertThat(result).isNotNull();
    assertThat(result.getAuthConfiguration()).isNotNull();
    assertThat(result.getAuthConfiguration().getUsername()).isEqualTo(extension.getUsername());
    assertThat(result.getAuthConfiguration().getPassword()).isEqualTo(extension.getPassword());
    assertThat(result.getCacheConfiguration()).isNull();
  }

  @Test
  public void testBuild_customCacheValuesExtension() {
    OssIndexPluginExtension extension = new OssIndexPluginExtension(null);
    extension.setCacheDirectory("/fake-dir/");
    extension.setCacheExpiration("PT60S");

    OssindexClientConfiguration result = builder.build(extension);

    assertThat(result).isNotNull();
    assertThat(result.getCacheConfiguration()).isInstanceOf(Configuration.class);

    Configuration cacheConfiguration = (Configuration) result.getCacheConfiguration();
    assertThat(cacheConfiguration.getBaseDir()).isEqualTo(Paths.get(extension.getCacheDirectory()));
    assertThat(cacheConfiguration.getExpireAfter()).isEqualTo(Duration.parse(extension.getCacheExpiration()));
  }

  @Test
  public void testBuild_invalidExpirationCacheExtension() {
    OssIndexPluginExtension extension = new OssIndexPluginExtension(null);
    extension.setCacheExpiration("FAKE");

    Duration expectedDuration = new Configuration().getExpireAfter();
    OssindexClientConfiguration result = builder.build(extension);

    assertThat(result).isNotNull();
    assertThat(result.getCacheConfiguration()).isInstanceOf(Configuration.class);

    Configuration cacheConfiguration = (Configuration) result.getCacheConfiguration();
    assertThat(cacheConfiguration.getExpireAfter()).isEqualTo(expectedDuration);
  }

  @Test
  @SuppressWarnings("serial")
  public void testBuild_usingProxy() {
    OssIndexPluginExtension extension = new OssIndexPluginExtension(null);
    extension.setProxyConfiguration(new Closure<ProxyConfiguration>(null)
    {
      @Override
      public ProxyConfiguration call() {
        ProxyConfiguration proxyConfiguration = (ProxyConfiguration) getDelegate();
        proxyConfiguration.setHost("test-host");
        proxyConfiguration.setPort(8080);
        return proxyConfiguration;
      }
    });

    OssindexClientConfiguration result = builder.build(extension);

    assertThat(result.getProxyConfiguration()).isNotNull();
    assertThat(result.getProxyConfiguration().getHost()).isEqualTo("test-host");
    assertThat(result.getProxyConfiguration().getPort()).isEqualTo(8080);
    assertThat(result.getProxyConfiguration().getProtocol()).isEqualTo(ProxyConfiguration.HTTP);
    assertThat(result.getProxyConfiguration().getAuthConfiguration()).isNull();
  }

  @Test
  @SuppressWarnings("serial")
  public void testBuild_usingProxyHttps() {
    OssIndexPluginExtension extension = new OssIndexPluginExtension(null);
    extension.setProxyConfiguration(new Closure<ProxyConfiguration>(null)
    {
      @Override
      public ProxyConfiguration call() {
        ProxyConfiguration proxyConfiguration = (ProxyConfiguration) getDelegate();
        proxyConfiguration.setProtocol(ProxyConfiguration.HTTPS);
        return proxyConfiguration;
      }
    });

    OssindexClientConfiguration result = builder.build(extension);

    assertThat(result.getProxyConfiguration().getProtocol()).isEqualTo(ProxyConfiguration.HTTPS);
  }

  @Test
  @SuppressWarnings("serial")
  public void testBuild_usingProxyWithAuthentication() {
    OssIndexPluginExtension extension = new OssIndexPluginExtension(null);
    extension.setProxyConfiguration(new Closure<ProxyConfiguration>(null)
    {
      @Override
      public ProxyConfiguration call() {
        ProxyConfiguration proxyConfiguration = (ProxyConfiguration) getDelegate();
        AuthConfiguration authConfiguration = proxyConfiguration.getAuthConfiguration();
        authConfiguration.setUsername("test-username");
        authConfiguration.setPassword("test-password");
        return proxyConfiguration;
      }
    });

    OssindexClientConfiguration result = builder.build(extension);

    AuthConfiguration authConfiguration = result.getProxyConfiguration().getAuthConfiguration();
    assertThat(authConfiguration).isNotNull();
    assertThat(authConfiguration.getUsername()).isEqualTo("test-username");
    assertThat(authConfiguration.getPassword()).isEqualTo("test-password");
  }
}
