/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.client.impl;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import com.sonatype.insight.brain.client.ConfigurationClient;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.module.model.Module;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ApplicationPolicyEvaluation;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ProprietaryConfig;
import org.sonatype.gradle.plugins.scan.nexus.iq.client.IqClient;
import org.sonatype.gradle.plugins.scan.nexus.iq.client.IqClientException;
import org.sonatype.gradle.plugins.scan.nexus.iq.client.scan.ScanResult;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;

/**
 * Default implementation of {@link IqClient}.
 *
 * @since 1.0.1
 */
public class DefaultIqClient
    implements IqClient
{
  private final RestClient restClient;

  private final ConfigurationClient configurationClient;

  private final PolicyEvaluator policyEvaluator;

  private final ScannerFactory scannerFactory;

  private final Optional<String> instanceId;

  private final Optional<Logger> log;

  static final String IQ_VERSION_SUPPORTING_CDX_1_7 = "1.206.0";

  static final String MAX_CDX_SUPPORTED_VERSION_PROPERTY = "max-cyclonedx-supported-version";

  static final String MAX_CDX_SUPPORTED_VERSION_PRE_1_7 = "1.6";

  public DefaultIqClient(
      final ConfigurationClient configurationClient,
      final ScannerFactory scannerFactory,
      final RestClient restClient,
      final ReportClientFactory reportClientFactory,
      final Optional<Logger> log,
      final Optional<String> instanceId)
  {
    this.restClient = restClient;
    this.scannerFactory = scannerFactory;
    this.configurationClient = configurationClient;
    this.instanceId = instanceId;
    this.log = log;

    if (restClient != null && reportClientFactory != null) {
      this.policyEvaluator = new PolicyEvaluator(restClient, reportClientFactory);
    }
    else {
      this.policyEvaluator = null;
    }
  }

  @Override
  public void validateServerVersion(final String minimalServerVersionRequired) throws IqClientException {
    if (configurationClient == null) {
      throwServerNotConfiguredException();
    }

    try {
      configurationClient.validateServerVersion(minimalServerVersionRequired);
    }
    catch (IOException e) {
      throw new IqClientException("Could not validate the IQ Server version.", e);
    }
  }

  @Override
  public boolean verifyOrCreateApplication(
      final String applicationPublicId,
      final String organizationId) throws IqClientException
  {
    if (configurationClient == null) {
      throwServerNotConfiguredException();
    }

    try {
      return configurationClient.verifyOrCreateApplication(applicationPublicId, organizationId);
    }
    catch (IOException e) {
      throw new IqClientException(getVerifyOrCreateApplicationErrorMessage(e), e);
    }
  }

  @Override
  public ProprietaryConfig getProprietaryConfigForApplicationEvaluation(final String appId) throws IqClientException {
    if (configurationClient == null) {
      throwServerNotConfiguredException();
    }

    try {
      com.sonatype.clm.dto.model.ProprietaryConfig iqProprietaryConfig =
          configurationClient.getProprietaryConfigForApplicationEvaluation(appId);
      return new ProprietaryConfig(iqProprietaryConfig.getPackages(), iqProprietaryConfig.getRegexes());
    }
    catch (IOException e) {
      throw new IqClientException(
          "Could not retrieve proprietary config for application: " + appId + " from IQ Server", e);
    }
  }

  @Override
  public ScanResult scan(
      final String applicationId,
      final ProprietaryConfig proprietaryConfig,
      final Properties properties,
      final List<File> scanTargets,
      final File scanFolder,
      final Map<String, String> envVars,
      final Set<String> licensedFeatures,
      final List<Module> modules) throws IqClientException
  {
    log.ifPresent(logger -> logger.info("{} Scanning application {}.", getFormattedCurrentDateTime(), applicationId));
    try {
      Properties scanConfig = proprietaryConfig.toProperties();
      scanConfig.putAll(properties);
      addPropertiesBasedOnIqServerVersion(scanConfig, restClient);
      ScanResult scanResult = scannerFactory.getScanner(log).scan(scanConfig, scanTargets,
          Optional.ofNullable(scanFolder), instanceId, envVars, modules, licensedFeatures);
      scanResult.getScanFile().deleteOnExit();
      return scanResult;
    }
    catch (IOException e) {
      throw new IqClientException(
          "Could not scan application: " + applicationId + " with targets: " + scanTargets, e);
    }
    finally {
      log.ifPresent(logger -> logger.info(
          "{} Finished scanning application {}.", getFormattedCurrentDateTime(), applicationId));
    }
  }

  @Override
  public ApplicationPolicyEvaluation evaluateApplication(
      final String applicationId,
      final String stageId,
      final ScanResult scanResult,
      final File scanFolder,
      final File jsonResultsFile) throws IqClientException
  {
    if (policyEvaluator == null) {
      throwServerNotConfiguredException();
    }
    Scan scan = scanResult.getScan();
    if (scan == null) {
      throw new IqClientException(
          "Scan model could not be read. This may indicate an xstream relocation or deserialization problem.");
    }
    ClientScanResult clientScanResult =
        new ClientScanResult(scanResult.getScanFile(), scan.hasThirdPartyScanContent());

    log.ifPresent(logger -> logger.info("{} Evaluating application {} for stage {}.",
        getFormattedCurrentDateTime(), applicationId, stageId));
    try {
      return policyEvaluator
          .evaluateApplication(clientScanResult, applicationId, ClientScanType.SONATYPE,
              stageId, jsonResultsFile);
    }
    finally {
      log.ifPresent(logger -> logger.info("{} Finished evaluating application {} for stage {}.",
          getFormattedCurrentDateTime(), applicationId, stageId));
    }
  }

  // Visible for testing
  void addPropertiesBasedOnIqServerVersion(final Properties scanConfiguration, final RestClient restClient)
      throws IOException
  {
    if (restClient != null && restClient.compareServerVersion(IQ_VERSION_SUPPORTING_CDX_1_7) < 0) {
      scanConfiguration.setProperty(MAX_CDX_SUPPORTED_VERSION_PROPERTY, MAX_CDX_SUPPORTED_VERSION_PRE_1_7);
      final String serverVersion = restClient.getServerVersion();
      log.ifPresent(logger -> logger.info("Setting {} to {} for IQ Server version {}",
          MAX_CDX_SUPPORTED_VERSION_PROPERTY, MAX_CDX_SUPPORTED_VERSION_PRE_1_7, serverVersion));
    }
  }

  private void throwServerNotConfiguredException() throws IqClientException {
    throw new IqClientException("IQ Server was not configured by builder");
  }

  private String getVerifyOrCreateApplicationErrorMessage(Throwable e) {
    if (e instanceof HttpResponseException && ((HttpResponseException) e).getStatusCode() == HttpStatus.SC_NOT_FOUND) {
      return "IQ Server is missing Auto App Creation. Supported IQ Server versions are 1.45.0 or newer.";
    }
    return "Could not verify application public id from IQ Server";
  }

  private String getFormattedCurrentDateTime() {
    return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(ZonedDateTime.now());
  }
}
