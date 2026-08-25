/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
@Library(['private-pipeline-library', 'jenkins-shared', 'int-jenkins-shared']) _

Map<String, ?> pipelineCommon = pipelineCommon retentionPolicy: RetentionPolicy.TEN_BUILDS

String deployBranch = 'main'

pipeline {
  agent { label pipelineCommon.agentLabel }
  options {
    buildDiscarder(
        logRotator(
            numToKeepStr: pipelineCommon.NUM_TO_KEEP_STR,
            daysToKeepStr: pipelineCommon.DAYS_TO_KEEP_STR,
            artifactNumToKeepStr: pipelineCommon.ARTIFACT_NUM_TO_KEEP_STR,
            artifactDaysToKeepStr: pipelineCommon.ARTIFACT_DAYS_TO_KEEP_STR
        )
    )
    timestamps()
  }
  parameters {
    booleanParam(
        name: 'runIntegrationTests',
        defaultValue: true,
        description: 'If checked it includes all integration tests; otherwise only unit tests will run.'
    )
  }
  stages {
    stage('Prepare') {
      steps {
        script {
          env.BRANCH_NAME = env.BRANCH_NAME ?: 'main'
          // Load this repo's own vars/ from the branch being built, not the library's
          // default version, so Jenkinsfile changes and vars/*.groovy changes stay in sync.
          library "scan-gradle-plugin@${env.BRANCH_NAME}"
        }
        githubStatusUpdate('pending')
      }
    }
    stage('Build and Test') {
      steps {
        runBuildWorkflow(env.BRANCH_NAME, params.runIntegrationTests)
        collectTestResults(params.runIntegrationTests
            ? ['target/test-results/test/*.xml', 'target/it*/*.xml']
            : ['target/test-results/test/*.xml'])
      }
    }
    stage('Policy Evaluation') {
      steps {
        nexusPolicyEvaluation(
          unstableBuildOnScanningWarnings: false,
          iqStage: env.BRANCH_NAME == 'main' ? 'build': 'develop',
          iqApplication: 'scan-gradle-plugin',
          iqScanPatterns: [
            [scanPattern: 'target/dependencies/*.jar'],
            [scanPattern: 'target/libs/scan-gradle-plugin-*-main.jar']
          ],
          failBuildOnNetworkError: true,
          reachability: [
            javaAnalysis: [
              enable: true,
              includes: [
                [pattern: 'target/dependencies/*.jar'],
                [pattern: 'target/libs/scan-gradle-plugin-*-SNAPSHOT-main.jar']
              ],
              namespaces: [
                [namespace: 'org.sonatype.gradle.plugins.scan']
              ]
            ]
          ]
        )
      }
    }
    stage('Collect Distribution Files') {
      steps {
        collectDist([includes: [
            'target/libs/*-SNAPSHOT.jar'
        ]])
      }
    }
  }
  post {
    always {
      script {
        if (env.BRANCH_NAME == 'main') {
          buildNotifications(currentBuild, env)
        }
        deleteDir()
      }
    }
    success {
      githubStatusUpdate('success')
    }
    failure {
      githubStatusUpdate('failure')
    }
  }
}
