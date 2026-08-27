/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

def call(String branch, boolean runIntegrationTests) {
  def gitHub = getGitHubClient('sonatype/scan-gradle-plugin')

  def inputs = [ runIntegrationTests: runIntegrationTests ]
  def workflowRun = gitHubTriggerWorkflow(gitHub, 'ci-build.yml', branch, inputs)

  gitHubPollWorkflowCompletion(gitHub, workflowRun, 600, 30)

  // successful release workflowRun run will have 1 or 5 artifacts
  gitHubArtifactDownload(gitHub, workflowRun, runIntegrationTests ? 5 : 1)
}
