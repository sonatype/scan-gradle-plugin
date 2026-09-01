/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.client.impl;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.sonatype.insight.scan.anon.Anonymizer;
import com.sonatype.insight.scan.client.ClientScanner;
import com.sonatype.insight.scan.config.ScanPropertiesLoader;
import com.sonatype.insight.scan.file.FileScanner;
import com.sonatype.insight.scan.hash.internal.DefaultDigester;
import com.sonatype.insight.scan.hash.internal.JavaDigester;
import com.sonatype.insight.scan.model.io.ScanWriterFactory;
import org.sonatype.gradle.plugins.scan.nexus.iq.client.scan.Scanner;

import org.slf4j.Logger;

/**
 * @since 1.0.1
 */
public class ScannerFactory
{
  static {
    // TrueZIP (pulled in transitively by insight-scanner-archive) logs its startup banner and driver
    // discovery via java.util.logging rather than SLF4J, so it bypasses logback.xml. Raise its level
    // before any archive scanning triggers TrueZIP class loading.
    java.util.logging.Logger.getLogger("de.schlichtherle.truezip").setLevel(java.util.logging.Level.SEVERE);
  }

  private final Map<Optional<Logger>, Scanner> scanners = new ConcurrentHashMap<>();

  /**
   * Returns a Scanner instance, memoized by logger presence.
   */
  public Scanner getScanner(final Optional<Logger> logger) {
    return scanners.computeIfAbsent(logger, ScannerFactory::memoizeScanner);
  }

  private static Scanner memoizeScanner(final Optional<Logger> logger) {
    if (logger.isPresent()) {
      ScanPropertiesLoader scanPropertiesLoader = new ScanPropertiesLoader(logger.get());
      ClientScanner clientScanner = new ClientScanner(logger.get());
      FileScanner fileScanner =
          new FileScanner(new DefaultDigester(new JavaDigester(), logger.get()), new Anonymizer(), logger.get());
      ScanWriterFactory scanWriterFactory = new ScanWriterFactory(logger.get());
      return new Scanner(scanPropertiesLoader, clientScanner, fileScanner, scanWriterFactory, logger.get());
    }
    else {
      ScanPropertiesLoader scanPropertiesLoader = new ScanPropertiesLoader();
      ClientScanner clientScanner = new ClientScanner();
      FileScanner fileScanner = new FileScanner(new DefaultDigester(new JavaDigester()), new Anonymizer());
      ScanWriterFactory scanWriterFactory = new ScanWriterFactory();
      return new Scanner(scanPropertiesLoader, clientScanner, fileScanner, scanWriterFactory);
    }
  }
}
