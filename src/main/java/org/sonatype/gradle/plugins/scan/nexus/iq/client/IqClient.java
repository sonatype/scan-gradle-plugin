/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.client;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.sonatype.insight.scan.module.model.Module;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ApplicationPolicyEvaluation;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ProprietaryConfig;
import org.sonatype.gradle.plugins.scan.nexus.iq.client.scan.ScanResult;

/**
 * Slim IQ client interface for Gradle plugin - contains only methods used by NexusIqScanTask.
 *
 * @since 1.0.1
 */
public interface IqClient
{
  /**
   * Validates the minimal IQ Server version required.
   *
   * @param minimalServerVersionRequired the minimal server version to validate against
   * @throws IqClientException when communication with IQ Server cannot be performed
   */
  void validateServerVersion(String minimalServerVersionRequired) throws IqClientException;

  /**
   * Verifies the applicationPublicId and if such an application does not exist and automatic application creation is
   * enabled, then the method creates the new application under the given organization id and returns true to indicate
   * the application will now be available.
   *
   * @param applicationPublicId the application public ID to verify
   * @param organizationId      the organization under which the app will be created
   * @return true to indicate the application is verified
   * @throws IqClientException when communication with IQ Server cannot be performed
   */
  boolean verifyOrCreateApplication(String applicationPublicId, String organizationId) throws IqClientException;

  /**
   * Fetches the proprietary config for the given application.
   *
   * @param applicationId the application public ID
   * @return the proprietary configuration containing packages and regexes to exclude
   * @throws IqClientException when communication with IQ Server cannot be performed
   */
  ProprietaryConfig getProprietaryConfigForApplicationEvaluation(String applicationId) throws IqClientException;

  /**
   * Scans components found in the given lists of files and modules using Sonatype IQ Server.
   *
   * @param applicationId     Public application ID from IQ Server.
   * @param proprietaryConfig Configuration to exclude known proprietary components.
   * @param properties        Configuration properties for the scan.
   * @param scanTargets       List of files to scan.
   * @param scanFolder        Base directory of the project or binary being scanned.
   * @param envVars           Environmental variables for the scan.
   * @param licensedFeatures  IQ licensed features.
   * @param modules           List of modules to scan.
   * @return {@link ScanResult} instance with the scan results.
   * @throws IqClientException If there is an error during the scan process.
   */
  ScanResult scan(
      String applicationId,
      ProprietaryConfig proprietaryConfig,
      Properties properties,
      List<File> scanTargets,
      File scanFolder,
      Map<String, String> envVars,
      Set<String> licensedFeatures,
      List<Module> modules) throws IqClientException;

  /**
   * Evaluates the scanned application and returns the policy evaluation result.
   *
   * @param applicationId  the application public ID
   * @param stageId        the stage ID for evaluation
   * @param scanResult     the scan result from a previous scan
   * @param scanFolder     the base directory used during scanning
   * @param jsonResultsFile optional file to write JSON results to
   * @return the policy evaluation result
   * @throws IqClientException when communication with IQ Server cannot be performed
   */
  ApplicationPolicyEvaluation evaluateApplication(
      String applicationId,
      String stageId,
      ScanResult scanResult,
      File scanFolder,
      File jsonResultsFile) throws IqClientException;
}
