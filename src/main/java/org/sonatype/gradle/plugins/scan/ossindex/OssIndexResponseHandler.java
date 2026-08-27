/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.ossindex;

import java.util.Map;
import java.util.Set;

import org.sonatype.goodies.packageurl.PackageUrl;
import org.sonatype.ossindex.service.api.componentreport.ComponentReport;

import org.gradle.api.artifacts.ResolvedDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for processing OSS Index response data.
 */
public interface OssIndexResponseHandler
{
  /**
   * Shared logger for response handlers.
   */
  Logger log = LoggerFactory.getLogger(OssIndexResponseHandler.class);

  /**
   * Processes the OSS Index response and reports vulnerabilities.
   */
  boolean handleOssIndexResponse(
      Set<ResolvedDependency> dependencies,
      Map<ResolvedDependency, PackageUrl> dependenciesMap,
      Map<PackageUrl, ComponentReport> response);
}
