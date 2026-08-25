/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan;

import org.sonatype.gradle.plugins.scan.nexus.iq.index.NexusIqPluginIndexExtension;
import org.sonatype.gradle.plugins.scan.nexus.iq.index.NexusIqIndexTask;
import org.sonatype.gradle.plugins.scan.nexus.iq.scan.NexusIqPluginScanExtension;
import org.sonatype.gradle.plugins.scan.nexus.iq.scan.NexusIqScanTask;
import org.sonatype.gradle.plugins.scan.ossindex.OssIndexAuditTask;
import org.sonatype.gradle.plugins.scan.ossindex.OssIndexPluginExtension;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ScanPluginTest
{
  private ScanPlugin plugin;

  @Before
  public void setup() {
    plugin = new ScanPlugin();
  }

  @Test
  public void testApply() {
    Project project = ProjectBuilder.builder().build();
    plugin.apply(project);

    assertThat(project.getTasks().getByName("nexusIQScan")).isInstanceOf(NexusIqScanTask.class);
    assertThat(project.getExtensions().getByName("nexusIQScan")).isInstanceOf(NexusIqPluginScanExtension.class);

    assertThat(project.getTasks().getByName("nexusIQIndex")).isInstanceOf(NexusIqIndexTask.class);
    assertThat(project.getExtensions().getByName("nexusIQIndex")).isInstanceOf(NexusIqPluginIndexExtension.class);

    assertThat(project.getTasks().getByName("ossIndexAudit")).isInstanceOf(OssIndexAuditTask.class);
    assertThat(project.getExtensions().getByName("ossIndexAudit")).isInstanceOf(OssIndexPluginExtension.class);
  }
}
