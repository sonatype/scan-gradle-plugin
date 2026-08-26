/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.client.scan;

import java.io.File;

import com.sonatype.insight.scan.model.Scan;

/**
 * @since 1.0.1
 */
public class ScanResult
{
  private Scan scan;

  private final File scanFile;

  public ScanResult(final Object scan, final File scanFile) {
    // since IQ 130 the 'xstream' relocation breaks the IQ functional tests; this is a workaround for that issue.
    // A more permanent fix could be bringing all ITs into the main module (i.e. api),
    // so all test could benefit from the same set of relocations, and this change can be reverted
    if (scan instanceof Scan) {
      this.scan = (Scan) scan;
    }
    this.scanFile = scanFile;
  }

  public Scan getScan() {
    return scan;
  }

  public File getScanFile() {
    return scanFile;
  }
}
