/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

def call() {
  def gitHub = getGitHubClient('sonatype/scan-gradle-plugin')

  def workflowRun = gitHubTriggerWorkflow(gitHub, 'ci-release.yml', 'main')

  def conclusion = gitHubPollWorkflowCompletion(gitHub, workflowRun, 1800, 30)

  // successful release workflowRun run will have 1 artifact
  gitHubArtifactDownload(gitHub, workflowRun, 1)

  if (conclusion != 'success') {
    error "Workflow run ${workflowRun.id} did not succeed: ${conclusion}"
  }
}
