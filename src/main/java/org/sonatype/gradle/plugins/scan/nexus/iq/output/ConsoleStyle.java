/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.output;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Glyphs, section headers and colors of the IQ console output. Falls back to ASCII when the console charset cannot
 * encode the glyphs. Uncolored text is identical to the colored text without the escape sequences.
 */
public final class ConsoleStyle
{
  private static final int HEADER_WIDTH = 88;

  private static final String RULE = "─";

  private static final String BULLET = " ➤ ";

  private static final String RESET = "\u001B[0m";

  private static final String RED = "31";

  private static final String GREEN = "32";

  private static final String YELLOW = "33";

  private static final String BLUE = "34";

  private static final String MAGENTA = "35";

  private static final String CYAN = "36";

  private static final String BOLD = "1";

  private final boolean ascii;

  private final boolean color;

  private ConsoleStyle(final boolean ascii, final boolean color) {
    this.ascii = ascii;
    this.color = color;
  }

  /**
   * @param color whether the console supports colors, decided by the caller
   */
  public static ConsoleStyle detect(final boolean color) {
    return new ConsoleStyle(!canEncodeGlyphs(consoleCharset()), color);
  }

  public static ConsoleStyle of(final boolean ascii, final boolean color) {
    return new ConsoleStyle(ascii, color);
  }

  /**
   * The charset the console is written with: {@code stdout.encoding} (JDK 19+), then {@code sun.stdout.encoding},
   * then the JVM default.
   */
  private static Charset consoleCharset() {
    for (String property : new String[]{"stdout.encoding", "sun.stdout.encoding"}) {
      String name = System.getProperty(property);
      if (name != null) {
        try {
          return Charset.forName(name);
        }
        catch (RuntimeException ignored) {
          // unknown charset name, try the next source
        }
      }
    }
    return Charset.defaultCharset();
  }

  static boolean canEncodeGlyphs(final Charset charset) {
    if (!charset.canEncode()) {
      return false;
    }
    CharsetEncoder encoder = charset.newEncoder();
    return encoder.canEncode(RULE) && encoder.canEncode(BULLET);
  }

  public String bullet() {
    return ascii ? " > " : BULLET;
  }

  /**
   * Spacers are a single space rather than an empty line: some log viewers drop zero-length lines.
   */
  public String spacer() {
    return " ";
  }

  /**
   * A section header: the rule line with the title, followed by a spacer. Unless {@code tight}, a spacer also
   * precedes it; a tight header is used right after a block that already ends with a spacer.
   */
  public void printHeader(final Consumer<String> out, final String title, final boolean tight) {
    if (!tight) {
      out.accept(spacer());
    }
    out.accept(header(title));
    out.accept(spacer());
  }

  String header(final String title) {
    int padCount = (HEADER_WIDTH - title.length()) / 2;
    String rule = ascii ? "-" : RULE;
    StringBuilder pad = new StringBuilder();
    for (int i = 0; i < padCount; i++) {
      pad.append(rule);
    }
    return pad + " " + green(title) + " " + pad;
  }

  public String red(final String text) {
    return paint(text, RED);
  }

  public String green(final String text) {
    return paint(text, GREEN);
  }

  public String yellow(final String text) {
    return paint(text, YELLOW);
  }

  public String blue(final String text) {
    return paint(text, BLUE);
  }

  public String cyan(final String text) {
    return paint(text, CYAN);
  }

  public String boldBlue(final String text) {
    return paint(text, BOLD + ";" + BLUE);
  }

  public String boldRed(final String text) {
    return paint(text, BOLD + ";" + RED);
  }

  public String boldMagenta(final String text) {
    return paint(text, BOLD + ";" + MAGENTA);
  }

  public String boldYellow(final String text) {
    return paint(text, BOLD + ";" + YELLOW);
  }

  private String paint(final String text, final String code) {
    return color ? String.format(Locale.ROOT, "\u001B[%sm%s%s", code, text, RESET) : text;
  }
}
