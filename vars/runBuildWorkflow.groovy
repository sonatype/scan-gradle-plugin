/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

def call(String branch, boolean runIntegrationTests) {
  def gitHub = getGitHubClient('sonatype/scan-gradle-plugin')

  boolean runITs = runIntegrationTests & branch == 'main'
  def inputs = [ runIntegrationTests: runITs ]
  def workflowRun = gitHubTriggerWorkflow(gitHub, 'ci-build.yml', branch, inputs)

  def conclusion = gitHubPollWorkflowCompletion(gitHub, workflowRun, 600, 30)

  // successful workflowRun run will have 1 or 5 artifacts
  gitHubArtifactDownload(gitHub, workflowRun, runITs ? 5 : 1)

  collectTestResults(runITs
      ? ['target/test-results/test/*.xml', 'target/it*/*.xml']
      : ['target/test-results/test/*.xml'])

  if (conclusion != 'success') {
    error "Workflow run ${workflowRun.id} did not succeed: ${conclusion}"
  }
}
