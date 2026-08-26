/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.client.impl;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.insight.brain.client.PolicyAction;
import com.sonatype.insight.brain.client.ReportClient;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.brain.client.UnsupportedServerVersionException;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.scan.model.ClientScanType;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ApplicationPolicyEvaluation;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.PolicyEvaluationResult;
import org.sonatype.gradle.plugins.scan.nexus.iq.client.IqClientException;

import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.0.1
 */
public class PolicyEvaluator
{
  static final String DEVELOPER_LICENSE_FEATURE = "developer-dashboard";

  static final String PRIORITIES_REPORT_FEATURE_FLAG = "prioritized-findings-report";

  private static final Logger log = LoggerFactory.getLogger(PolicyEvaluator.class);

  private final RestClient restClient;

  private final ReportClientFactory reportClientFactory;

  public PolicyEvaluator(
      final RestClient restClient,
      final ReportClientFactory reportClientFactory)
  {
    this.restClient = restClient;
    this.reportClientFactory = reportClientFactory;
  }

  public ApplicationPolicyEvaluation evaluateApplication(
      final ClientScanResult clientScanResult,
      final String appId,
      final ClientScanType clientScanType,
      final String stageId) throws IqClientException
  {
    return evaluateApplication(clientScanResult, appId, clientScanType, stageId, null);
  }

  public ApplicationPolicyEvaluation evaluateApplication(
      final ClientScanResult clientScanResult,
      final String appId,
      final ClientScanType clientScanType,
      final String stageId,
      final File resultFile) throws IqClientException
  {
    try {
      final PolicyEvaluationPollingResult evaluationPollingResult =
          restClient.evaluatePolicy(appId, stageId, clientScanResult, clientScanType);

      final com.sonatype.clm.dto.model.ScanReceipt scanReceipt = evaluationPollingResult.getScanReceipt();
      com.sonatype.clm.dto.model.policy.PolicyEvaluationResult evaluationResultDto =
          evaluationPollingResult.getResult();

      // convert DTO model to API model
      final PolicyEvaluationResult policyEvaluationResult = PolicyEvaluationResultMapper.map(evaluationResultDto);

      if (resultFile != null) {
        final PolicyActionResolver policyActionResolver = new PolicyActionResolver();
        final PolicyAction policyAction = policyActionResolver.resolve(policyEvaluationResult.getPolicyAlerts());

        restClient.saveResults(appId, resultFile, scanReceipt, evaluationResultDto, policyAction.toString());
      }

      final String reportLink = getReportLink(appId, scanReceipt.getScanId());
      final String prioritiesLink = getPrioritiesLink(appId, scanReceipt.getScanId());

      return new ApplicationPolicyEvaluation(
          policyEvaluationResult.getAffectedComponentCount(),
          policyEvaluationResult.getCriticalComponentCount(),
          policyEvaluationResult.getSevereComponentCount(),
          policyEvaluationResult.getModerateComponentCount(),
          policyEvaluationResult.getCriticalPolicyViolationCount(),
          policyEvaluationResult.getSeverePolicyViolationCount(),
          policyEvaluationResult.getModeratePolicyViolationCount(),
          policyEvaluationResult.getLegacyViolationCount(),
          policyEvaluationResult.getTotalComponentCount(),
          policyEvaluationResult.getPolicyAlerts(),
          reportLink,
          prioritiesLink);
    }
    catch (HttpResponseException e) {
      throw new IqClientException("Received invalid response from IQ server. HTTP status: " + e.getStatusCode()
          + " Application: " + appId + " Stage: " + stageId, e);
    }
    catch (IOException e) {
      throw new IqClientException("The policy evaluation results could not be fetched from the IQ Server. Application: "
          + appId + " Stage: " + stageId, e);
    }
  }

  /**
   * @since 5.2.0
   */
  public String getReportLink(final String applicationId, final String scanId) {
    final ReportClient reportClient = reportClientFactory.get(applicationId, scanId);
    return reportClient.linkToReport();
  }

  public String getPrioritiesLink(final String applicationId, final String scanId) {
    boolean isDevelopmentDashboardEnabled = false;
    boolean isPrioritizedFindingsReportEnabled = false;

    try {
      final Set<String> licensedFeatures = restClient.getLicensedFeatures();
      isDevelopmentDashboardEnabled = licensedFeatures.contains(DEVELOPER_LICENSE_FEATURE);
      isPrioritizedFindingsReportEnabled = licensedFeatures.contains(PRIORITIES_REPORT_FEATURE_FLAG);
    }
    catch (final IOException e) {
      log.warn("Unable to fetch IQ licensed features", e);
    }

    if (!isDevelopmentDashboardEnabled || !isPrioritizedFindingsReportEnabled) {
      return null;
    }

    final ReportClient reportClient = reportClientFactory.get(applicationId, scanId);
    String prioritiesLink;
    try {
      restClient.validateServerVersion("1.188");
      prioritiesLink = reportClient.linkToIntegrationPrioritiesReport("gradle");
    }
    catch (IOException | UnsupportedServerVersionException ignored) {
      prioritiesLink = reportClient.linkToPrioritiesReport();
    }

    return prioritiesLink;
  }
}
