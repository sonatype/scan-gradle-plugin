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
 * Result of a scan operation.
 * <p>
 * Note: {@link #getScan()} may return {@code null} when the {@code instanceof Scan} guard in the constructor fails,
 * which can occur during xstream relocation or deserialization problems. Callers must null-check the return value.
 *
 * @since 1.0.1
 */
public class ScanResult
{
  private Scan scan;

  private final File scanFile;

  /**
   * Constructs a ScanResult from a scan object and its serialized file.
   */
  public ScanResult(final Object scan, final File scanFile) {
    // since IQ 130 the 'xstream' relocation breaks the IQ functional tests; this is a workaround for that issue.
    // A more permanent fix could be bringing all ITs into the main module (i.e. api),
    // so all test could benefit from the same set of relocations, and this change can be reverted
    if (scan instanceof Scan) {
      this.scan = (Scan) scan;
    }
    this.scanFile = scanFile;
  }

  /**
   * Returns the scan model, which may be null if deserialization failed.
   */
  public Scan getScan() {
    return scan;
  }

  /**
   * Returns the file containing the serialized scan data.
   */
  public File getScanFile() {
    return scanFile;
  }
}
