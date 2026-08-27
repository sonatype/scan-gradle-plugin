/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.ossindex;

/**
 * Output format options for OSS Index vulnerability reports.
 */
public enum OutputFormat
{
  /** Plain text output format. */
  DEFAULT,
  /** Dependency tree output format. */
  DEPENDENCY_GRAPH,
  /** CycloneDX JSON SBOM output format. */
  JSON_CYCLONE_DX_1_4
}
