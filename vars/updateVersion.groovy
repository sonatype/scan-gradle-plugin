/*
 * Copyright (c) 2023-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import groovy.transform.Field

@Field
String logScope = "gradle -> updateVersion"

def call(String version) {
  String manifestPath = 'gradle.properties'

  echo "$logScope: Updating version in ${manifestPath} to '${version}'"

  String manifest = readFile(manifestPath)
  String updated = manifest.replaceFirst(
      /(?m)^version=[^=]+$/,
      "version=${version}"
  )

  if (updated == manifest) {
    error "$logScope: Failed to find the version property to update in ${manifestPath}"
  }

  writeFile(file: manifestPath, text: updated)
}
