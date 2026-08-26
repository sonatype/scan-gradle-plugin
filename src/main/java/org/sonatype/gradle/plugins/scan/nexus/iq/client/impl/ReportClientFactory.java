/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.client.impl;

import com.sonatype.insight.brain.client.ReportClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;

/**
 * @since 1.0.1
 */
public class ReportClientFactory
{
  private final Configuration configuration;

  public ReportClientFactory(Configuration configuration) {
    this.configuration = configuration;
  }

  public ReportClient get(final String applicationId, final String scanId) {
    return new ReportClient(configuration, applicationId, scanId);
  }
}
