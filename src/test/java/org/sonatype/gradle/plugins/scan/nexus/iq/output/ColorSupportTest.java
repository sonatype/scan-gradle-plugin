/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.output;

import java.util.Map;

import org.gradle.api.logging.configuration.ConsoleOutput;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ColorSupportTest
{
  private static final Map<String, String> TERMINAL = Map.of("TERM", "xterm-256color");

  @Test
  public void testPlainNeverColors() {
    assertThat(ColorSupport.isSupported(ConsoleOutput.Plain, TERMINAL)).isFalse();
  }

  @Test
  public void testRichAndVerboseColor() {
    assertThat(ColorSupport.isSupported(ConsoleOutput.Rich, Map.of())).isTrue();
    assertThat(ColorSupport.isSupported(ConsoleOutput.Verbose, TERMINAL)).isTrue();
  }

  @Test
  public void testNoColorWinsOverRich() {
    assertThat(ColorSupport.isSupported(ConsoleOutput.Rich, Map.of("NO_COLOR", "1"))).isFalse();
  }

  @Test
  public void testAutoNeverColors() {
    assertThat(ColorSupport.isSupported(ConsoleOutput.Auto, TERMINAL)).isFalse();
    assertThat(ColorSupport.isSupported(ConsoleOutput.Auto, Map.of())).isFalse();
  }
}
