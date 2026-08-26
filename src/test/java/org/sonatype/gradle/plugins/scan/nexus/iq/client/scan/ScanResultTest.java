/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.client.scan;

import java.io.File;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ScanResult}.
 */
public class ScanResultTest
{
  @Test
  public void getScanReturnsNullWhenObjectIsNotScan() {
    File scanFile = new File("test-scan.json");
    ScanResult scanResult = new ScanResult("not-a-scan-object", scanFile);

    assertThat(scanResult.getScan()).isNull();
    assertThat(scanResult.getScanFile()).isEqualTo(scanFile);
  }
}
