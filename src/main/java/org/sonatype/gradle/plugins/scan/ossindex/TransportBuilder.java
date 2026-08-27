/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.ossindex;

import org.sonatype.gradle.plugins.scan.common.PluginVersionUtils;
import org.sonatype.ossindex.service.client.internal.VersionSupplier;
import org.sonatype.ossindex.service.client.transport.HttpClientTransport;
import org.sonatype.ossindex.service.client.transport.UserAgentBuilder;
import org.sonatype.ossindex.service.client.transport.UserAgentBuilder.Product;
import org.sonatype.ossindex.service.client.transport.UserAgentSupplier;

import org.gradle.api.Project;
import org.gradle.internal.impldep.com.google.common.annotations.VisibleForTesting;

/**
 * Builder for HTTP transport used by OSS Index client.
 */
public class TransportBuilder
{
  /**
   * Builds an HTTP transport configured with the appropriate user agent.
   */
  public HttpClientTransport build(Project project) {
    UserAgentSupplier userAgentSupplier = buildUserAgentSupplier(project);
    return new HttpClientTransport(userAgentSupplier);
  }

  @VisibleForTesting
  UserAgentSupplier buildUserAgentSupplier(Project project) {
    return new UserAgentSupplier(new VersionSupplier().get())
    {
      @Override
      protected void customize(UserAgentBuilder builder) {
        builder.product(new Product("Gradle", project.getGradle().getGradleVersion()));
        builder.product(new Product("Gradle-Plugin", PluginVersionUtils.getPluginVersion()));
      }
    };
  }
}
