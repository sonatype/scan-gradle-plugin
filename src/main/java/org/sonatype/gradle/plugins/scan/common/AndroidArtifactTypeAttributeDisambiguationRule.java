/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.common;

import org.gradle.api.attributes.AttributeDisambiguationRule;
import org.gradle.api.attributes.MultipleCandidatesDetails;

import static org.gradle.api.artifacts.type.ArtifactTypeDefinition.JAR_TYPE;

public class AndroidArtifactTypeAttributeDisambiguationRule
    implements AttributeDisambiguationRule<String>
{
  private static final String AAR_TYPE = "aar";

  @Override
  public void execute(MultipleCandidatesDetails<String> details) {
    if (details.getCandidateValues().contains(JAR_TYPE)) {
      details.closestMatch(JAR_TYPE);
    }
    else if (details.getCandidateValues().contains(AAR_TYPE)) {
      details.closestMatch(AAR_TYPE);
    }
  }
}
