/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.output;

import java.util.Map;

import org.gradle.api.logging.configuration.ConsoleOutput;

/**
 * Decides whether colors may be used, from public Gradle API and the environment only.
 * <p>
 * A plugin cannot see whether Gradle attached the build to a terminal: {@code --console=auto} is reported the same
 * way for a terminal and for redirected output, CI or an IDE. So colors are only used when the console was explicitly
 * requested as {@code rich} or {@code verbose} (for example {@code --console=rich}, or {@code org.gradle.console=rich}
 * in {@code gradle.properties}); in every other case no escape sequences are emitted. {@code NO_COLOR} always wins.
 */
public final class ColorSupport
{
  private ColorSupport() {
  }

  public static boolean isSupported(final ConsoleOutput console, final Map<String, String> env) {
    String noColor = env.get("NO_COLOR");
    if (noColor != null && !noColor.isEmpty()) {
      return false;
    }
    return console == ConsoleOutput.Rich || console == ConsoleOutput.Verbose;
  }
}
