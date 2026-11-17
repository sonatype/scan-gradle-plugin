/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.scan;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.brain.client.PolicyAction;
import com.sonatype.insight.scan.module.model.Module;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.Action;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ApplicationPolicyEvaluation;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.PolicyAlert;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.PolicyFact;
import org.sonatype.gradle.plugins.scan.nexus.iq.api.ProprietaryConfig;
import org.sonatype.gradle.plugins.scan.nexus.iq.client.IqClient;
import org.sonatype.gradle.plugins.scan.nexus.iq.client.IqClientBuilder;
import org.sonatype.gradle.plugins.scan.nexus.iq.client.IqClientException;
import org.sonatype.gradle.plugins.scan.nexus.iq.client.common.Authentication;
import org.sonatype.gradle.plugins.scan.nexus.iq.client.common.ServerConfig;
import org.sonatype.gradle.plugins.scan.nexus.iq.client.impl.PolicyActionResolver;
import org.sonatype.gradle.plugins.scan.nexus.iq.client.scan.ScanResult;
import org.sonatype.gradle.plugins.scan.nexus.iq.output.ColorSupport;
import org.sonatype.gradle.plugins.scan.nexus.iq.output.ConsoleStyle;
import org.sonatype.gradle.plugins.scan.nexus.iq.output.EvaluationResultsFormatter;
import org.sonatype.gradle.plugins.scan.nexus.iq.output.EvaluationSummaryFormatter;

import org.sonatype.gradle.plugins.scan.common.DependenciesFinder;
import org.sonatype.gradle.plugins.scan.common.PluginVersionUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.impldep.com.google.common.annotations.VisibleForTesting;
import org.gradle.work.DisableCachingByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.stream;

/**
 * Gradle task for scanning and evaluating dependencies against Nexus IQ Server policies.
 */
@DisableCachingByDefault(because = "Scanning submits the dependency list to a remote Nexus IQ Server, which "
    + "creates a report and evaluates it against server-side policies that can change independently of this "
    + "build, so the result is not reproducible from the task inputs")
public class NexusIqScanTask
    extends DefaultTask
{
  private final Logger log = LoggerFactory.getLogger(NexusIqScanTask.class);

  private static final String MINIMAL_SERVER_VERSION_REQUIRED = "1.69.0";

  private static final String USER_AGENT_NAME = "Sonatype_Nexus_Gradle";

  private final NexusIqPluginScanExtension extension;

  private DependenciesFinder dependenciesFinder;

  private org.gradle.api.logging.Logger reportLogger;

  /**
   * Constructs a new NexusIqScanTask.
   */
  public NexusIqScanTask() {
    extension = getProject().getExtensions().getByType(NexusIqPluginScanExtension.class);
    dependenciesFinder = new DependenciesFinder();
  }

  /**
   * Executes the scan against Nexus IQ Server.
   */
  @TaskAction
  public void scan() {
    final ApplicationPolicyEvaluation applicationPolicyEvaluation;
    try {
      if (extension.isSimulationEnabled()) {
        log.info("Simulating scan...");

        List<PolicyAlert> alerts = new ArrayList<>();
        if (extension.getSimulatedPolicyActionId() != null) {
          alerts.add(new PolicyAlert(new PolicyFact("policyId", "Policy Name", 10, Collections.emptyList()),
              Collections.singletonList(new Action(extension.getSimulatedPolicyActionId()))));
        }

        dependenciesFinder.findModules(getProject(), extension.isAllConfigurations(), extension.getModulesExcluded(),
            extension.getVariantAttributes(), extension.isExcludeCompileOnly());

        applicationPolicyEvaluation =
            new ApplicationPolicyEvaluation(0, 0, 0, 0, 0, 0, 0, 0, 1, alerts, "simulated/report",
                "simulated/priorities");
      }
      else {
        IqClient iqClient = IqClientBuilder.create()
            .withServerConfig(new ServerConfig(new URI(getServerUrl()),
                    new Authentication(extension.getUsername(), extension.getPassword())))
            .withLogger(log)
            .withUserAgent(buildUserAgent())
            .build();

        iqClient.validateServerVersion(MINIMAL_SERVER_VERSION_REQUIRED);

        verifyOrCreateApplication(iqClient);

        ProprietaryConfig proprietaryConfig =
            iqClient.getProprietaryConfigForApplicationEvaluation(extension.getApplicationId());

        File scanFolder = new File(extension.getScanFolderPath());
        List<Module> modules = dependenciesFinder.findModules(getProject(), extension.isAllConfigurations(),
            extension.getModulesExcluded(), extension.getVariantAttributes(), extension.isExcludeCompileOnly());

        ScanResult scanResult = iqClient.scan(extension.getApplicationId(), proprietaryConfig, buildProperties(),
            buildScanTargets(), scanFolder, Collections.emptyMap(), Collections.emptySet(), modules);

        File jsonResultsFile = null;
        if (StringUtils.isNotBlank(extension.getResultFilePath())) {
          jsonResultsFile = new File(extension.getResultFilePath());
        }
        applicationPolicyEvaluation = iqClient.evaluateApplication(
                extension.getApplicationId(), extension.getStage(), scanResult, scanFolder, jsonResultsFile);
      }
    }
    catch (IqClientException e) {
      String reason = "Could not scan the project: " + e.getMessage();
      if (e.getCause() != null && StringUtils.isNotBlank(e.getCause().getMessage())) {
        reason = StringUtils.appendIfMissing(reason, ".");
        reason += " Please check this cause: " + e.getCause().getMessage();
      }
      throw new GradleException(reason, e);
    }
    catch (Exception e) {
      throw new GradleException("Could not scan the project: " + e.getMessage(), e);
    }

    PolicyAction policyAction = new PolicyActionResolver().resolve(applicationPolicyEvaluation.getPolicyAlerts());
    logReport(policyAction, applicationPolicyEvaluation);
  }

  private void verifyOrCreateApplication(IqClient iqClient) throws IqClientException {
    if (!iqClient.verifyOrCreateApplication(extension.getApplicationId(), extension.getOrganizationId())) {
      String message;
      if (StringUtils.isBlank(extension.getOrganizationId())) {
        message = String.format(
            "Application ID %s doesn't exist and couldn't be created or the user %s doesn't have the "
                + "'Application Evaluator' role for that application.",
            extension.getApplicationId(), extension.getUsername());
      }
      else {
        message = String.format(
            "Application ID %s or Organization ID %s don't exist and couldn't be created or the user %s doesn't have "
                + "the 'Application Evaluator' role for that application.",
            extension.getApplicationId(), extension.getOrganizationId(), extension.getUsername());
      }
      throw new IllegalArgumentException(message);
    }
  }

  private Properties buildProperties() {
    Properties properties = new Properties();
    if (StringUtils.isNotBlank(extension.getDirIncludes())) {
      properties.setProperty("dirIncludes", extension.getDirIncludes());
    }
    if (StringUtils.isNotBlank(extension.getDirExcludes())) {
      properties.setProperty("dirExcludes", extension.getDirExcludes());
    }
    return properties;
  }

  private List<File> buildScanTargets() {
    if (extension.getScanTargets() != null && !extension.getScanTargets().isEmpty()) {
      // Using the same approach as the Jenkins plugin for consistency
      DirectoryScanner directoryScanner = new DirectoryScanner();
      directoryScanner.setBasedir(extension.getScanFolderPath());
      directoryScanner.setIncludes(extension.getScanTargets().toArray(new String[extension.getScanTargets().size()]));
      directoryScanner.addDefaultExcludes();
      directoryScanner.scan();
      return Stream
          .concat(stream(directoryScanner.getIncludedDirectories()), stream(directoryScanner.getIncludedFiles()))
          .map(file -> new File(extension.getScanFolderPath(), file))
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  private void logReport(PolicyAction policyAction, ApplicationPolicyEvaluation evaluation) {
    // the level decides where the report shows: error and warn survive --quiet, lifecycle is the default level
    Consumer<String> out = line -> {
      switch (policyAction) {
        case FAIL:
          reportLogger().error(line);
          break;
        case WARN:
          reportLogger().warn(line);
          break;
        default:
          reportLogger().lifecycle(line);
      }
    };
    // printing the report must never change the outcome of the build
    try {
      ConsoleStyle style = ConsoleStyle.detect(isColorSupported());
      boolean resultsPrinted = EvaluationResultsFormatter.print(out, style, evaluation);
      EvaluationSummaryFormatter.print(out, style, evaluation, describe(policyAction), extension.getStage(),
          resultsPrinted);
    }
    catch (RuntimeException e) {
      reportLogger().warn("Could not print the IQ evaluation report: {}", e.toString());
      reportLogger().debug("Could not print the IQ evaluation report", e);
    }

    if (PolicyAction.FAIL == policyAction) {
      throw new GradleException("Sonatype IQ Server policy evaluation failed. See the evaluation results above.");
    }
  }

  private boolean isColorSupported() {
    return extension.isColorEnabled()
        && ColorSupport.isSupported(getProject().getGradle().getStartParameter().getConsoleOutput(), System.getenv());
  }

  private static String describe(PolicyAction policyAction) {
    switch (policyAction) {
      case FAIL:
        return "failure";
      case WARN:
        return "warning";
      default:
        return "none";
    }
  }

  private String buildUserAgent() {
    return String.format("%s/%s (Java %s; %s %s; Gradle %s)",
        USER_AGENT_NAME,
        PluginVersionUtils.getPluginVersion(),
        System.getProperty("java.version"),
        System.getProperty("os.name"),
        System.getProperty("os.version"),
        getProject().getGradle().getGradleVersion());
  }

  /**
   * Returns the scan folder path.
   */
  @Input
  public String getScanFolderPath() {
    return extension.getScanFolderPath();
  }

  /**
   * Returns the username for authentication.
   */
  @Input
  public String getUsername() {
    return extension.getUsername();
  }

  /**
   * Returns the password for authentication.
   */
  @Input
  public String getPassword() {
    return extension.getPassword();
  }

  /**
   * Returns the application ID.
   */
  @Input
  public String getApplicationId() {
    return extension.getApplicationId();
  }

  /**
   * Returns the organization ID.
   */
  @Input
  public String getOrganizationId() {
    return extension.getOrganizationId();
  }

  /**
   * Returns the server URL.
   */
  @Input
  public String getServerUrl() {
    return extension.getServerUrl();
  }

  /**
   * Returns the stage.
   */
  @Input
  public String getStage() {
    return extension.getStage();
  }

  /**
   * Returns whether all configurations should be scanned.
   */
  @Input
  public boolean isAllConfigurations() {
    return extension.isAllConfigurations();
  }

  /**
   * Returns the set of excluded modules.
   */
  @Input
  public Set<String> getModulesExcluded() {
    return extension.getModulesExcluded();
  }

  /**
   * Returns the directory include patterns.
   */
  @Input
  public String getDirIncludes() {
    return extension.getDirIncludes();
  }

  /**
   * Returns the directory exclude patterns.
   */
  @Input
  public String getDirExcludes() {
    return extension.getDirExcludes();
  }

  private org.gradle.api.logging.Logger reportLogger() {
    return reportLogger != null ? reportLogger : getLogger();
  }

  @VisibleForTesting
  void setReportLogger(org.gradle.api.logging.Logger reportLogger) {
    this.reportLogger = reportLogger;
  }

  @VisibleForTesting
  void setDependenciesFinder(DependenciesFinder dependenciesFinder) {
    this.dependenciesFinder = dependenciesFinder;
  }
}
