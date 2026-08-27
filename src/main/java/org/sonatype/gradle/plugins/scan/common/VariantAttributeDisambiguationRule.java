/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.common;

import javax.inject.Inject;

import org.gradle.api.attributes.AttributeDisambiguationRule;
import org.gradle.api.attributes.MultipleCandidatesDetails;

/**
 * Disambiguates variant attributes based on a configured value.
 */
public class VariantAttributeDisambiguationRule
    implements AttributeDisambiguationRule<String>
{
  private final String variantValue;

  /**
   * Constructs a rule with the specified variant value.
   */
  @Inject
  public VariantAttributeDisambiguationRule(String variantValue) {
    this.variantValue = variantValue;
  }

  @Override
  public void execute(MultipleCandidatesDetails<String> details) {
    if (details.getCandidateValues().contains(variantValue)) {
      details.closestMatch(variantValue);
    }
  }
}
