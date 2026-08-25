/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.ossindex;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.sonatype.ossindex.service.client.transport.UserAgentSupplier;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.util.GradleVersion;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TransportBuilderTest
{
  private TransportBuilder builder;

  @Before
  public void setup() {
    builder = new TransportBuilder();
  }

  @Test
  public void testBuild() {
    Project project = ProjectBuilder.builder().build();
    assertThat(builder.build(project)).isNotNull();
  }

  @Test
  public void testBuildUserAgentSupplier() throws IOException {
    Project project = ProjectBuilder.builder().build();

    UserAgentSupplier result = builder.buildUserAgentSupplier(project);

    assertThat(result).isNotNull();

    String gradleVersion = GradleVersion.current().getVersion();
    String pluginVersion;

    try (InputStream stream = getClass().getResourceAsStream("/com/sonatype/insight/client.properties")) {
      Properties properties = new Properties();
      properties.load(stream);
      pluginVersion = properties.getProperty("version");
    }

    assertThat(result.get()).endsWith("Gradle/" + gradleVersion + " Gradle-Plugin/" + pluginVersion);
  }
}
