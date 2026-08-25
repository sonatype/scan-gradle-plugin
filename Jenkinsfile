/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
@Library(['private-pipeline-library', 'jenkins-shared', 'int-jenkins-shared']) _

Map<String, ?> pipelineCommon = pipelineCommon retentionPolicy: RetentionPolicy.TEN_BUILDS

String deployBranch = 'main'

properties([
    parameters([
        booleanParam(
            name: 'runIntegrationTests',
            defaultValue: true,
            description: 'If checked it includes all integration tests; otherwise only unit tests will run.'
        )
    ])
])

Closure policyEvaluation = { stage ->
  nexusPolicyEvaluation(
    unstableBuildOnScanningWarnings: false,
    iqStage: stage,
    iqApplication: 'scan-gradle-plugin',
    iqScanPatterns: [
      [scanPattern: 'build/dependencies/*.jar']
    ],
    failBuildOnNetworkError: true
  )
}

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
  stages {
    stage('Preparation') {
      steps {
        githubStatusUpdate('pending')
      }
    }
    stage('License Check') {
      steps {
        licenseCheck()
      }
    }
    stage('Build and Test') {
      steps {
        gradleExec(params.runIntegrationTests ? 'build copyDependencies integrationTest' : 'build copyDependencies')
        collectTestResults(['**/test-results/*/*.xml'])
      }
    }
    stage('Collect Distribution Files') {
      steps {
        collectDist([includes: ['build/libs/scan-gradle-plugin-*-SNAPSHOT.jar']])
      }
    }
    stage('Evaluate Policies') {
      steps {
        vulnerabilityScan(policyEvaluation, isDeployBranch(env, deployBranch) ? 'build' : 'develop')
      }
    }
    stage('Upload Artifacts') {
      when {
        expression { return isDeployBranch(env, deployBranch) }
      }
      steps {
        gradleExec("publish")
      }
    }
  }
  post {
    success {
      githubStatusUpdate('success')
    }
    failure {
      githubStatusUpdate('failure')
    }
    always {
      postHandler({ build, env -> buildNotifications(build, env) }, currentBuild, env)
    }
    cleanup() {
      dockerRemoveImages()
      deleteDir()
    }
  }
}

String BUILD_IMAGE_ID() { return "${sonatypeDockerRegistryId()}/integrations/gradle-build-pipeline" }

def gradleExec(String cmd) {
  dockerPrepareBuildImage(BUILD_IMAGE_ID(), true)
  withCredentials([[$class: 'ZipFileBinding', credentialsId: 'gnupg', variable: 'gpgZip']]) {
    withDockerImage(BUILD_IMAGE_ID(), 'rsc-ro-npmrc', '-v $gpgZip/gnupg:/home/jenkins/.gnupg') {
      configFileProvider(
        [configFile(fileId: 'external-gpg-init.gradle', variable: 'initGradlePath')]) {
        sh 'chmod 700 /home/jenkins/.gnupg'
        sh 'chmod 600 /home/jenkins/.gnupg/*'
        sh "./gradlew --init-script $initGradlePath --stacktrace --console=plain --no-daemon --info ${cmd}"
      }
    }
  }
}
