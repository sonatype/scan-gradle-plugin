/*
 * Copyright (c) 2023-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import groovy.transform.Field

@Field
String logScope = "gradle -> createReleaseFromTag"

def call(String tagName) {
  try {
    createGithubRelease(tagName, tagName, '', true)
    echo "$logScope: creation successful for tag ${tagName}'"
  } catch (Exception ex) {
    echo "$logScope: failure: ${ex.getMessage()}"
  }
}
