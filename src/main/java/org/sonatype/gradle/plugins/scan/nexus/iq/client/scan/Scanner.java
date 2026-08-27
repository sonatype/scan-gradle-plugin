/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.client.scan;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.Preferences;

import com.sonatype.insight.scan.client.ClientScanRequest;
import com.sonatype.insight.scan.client.ClientScanner;
import com.sonatype.insight.scan.config.ScanPropertiesLoader;
import com.sonatype.insight.scan.file.FileScanRequest;
import com.sonatype.insight.scan.file.FileScanner;
import com.sonatype.insight.scan.file.ModuleScanRequest;
import com.sonatype.insight.scan.file.ScanSession;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.ScanConfiguration;
import com.sonatype.insight.scan.model.ScanMetadata;
import com.sonatype.insight.scan.model.ScanSummary;
import com.sonatype.insight.scan.model.io.ScanWriter;
import com.sonatype.insight.scan.model.io.ScanWriterFactory;
import com.sonatype.insight.scan.module.model.Dependency;
import com.sonatype.insight.scan.module.model.Module;
import com.sonatype.nexus.git.utils.branch.BranchNameFinderBuilder;
import com.sonatype.nexus.git.utils.commit.CommitHashFinderBuilder;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * @since 1.0.1
 */
public class Scanner
{
  private final ScanPropertiesLoader scanPropertiesLoader;

  private final ClientScanner clientScanner;

  private final FileScanner fileScanner;

  private final ScanWriterFactory scanWriterFactory;

  private final Logger log;

  /**
   * Constructs a new Scanner without a logger.
   */
  public Scanner(
      final ScanPropertiesLoader scanPropertiesLoader,
      final ClientScanner clientScanner,
      final FileScanner fileScanner,
      final ScanWriterFactory scanWriterFactory)
  {
    this(scanPropertiesLoader, clientScanner, fileScanner, scanWriterFactory, null);
  }

  /**
   * Constructs a new Scanner with an optional logger.
   */
  public Scanner(
      final ScanPropertiesLoader scanPropertiesLoader,
      final ClientScanner clientScanner,
      final FileScanner fileScanner,
      final ScanWriterFactory scanWriterFactory,
      final Logger log)
  {
    this.scanPropertiesLoader = scanPropertiesLoader;
    this.clientScanner = clientScanner;
    this.fileScanner = fileScanner;
    this.scanWriterFactory = scanWriterFactory;
    this.log = log;
  }

  /**
   * Scans the given targets and modules without licensed features.
   */
  public ScanResult scan(
      final Properties config,
      final List<File> targets,
      final Optional<File> baseDirectory,
      final Optional<String> instanceId,
      final Map<String, String> envVars,
      final List<Module> modules) throws IOException
  {
    return scan(config, targets, baseDirectory, instanceId, envVars, modules, null);
  }

  /**
   * Scans the given targets and modules with optional licensed features.
   */
  public ScanResult scan(
      final Properties config,
      final List<File> targets,
      final Optional<File> baseDirectory,
      final Optional<String> instanceId,
      final Map<String, String> envVars,
      final List<Module> modules,
      final Set<String> licensedFeatures) throws IOException
  {
    Scan scan = new Scan();
    scan.setConfiguration(createScanConfiguration(config));

    File scanFile = createScanFileInATempFolder(envVars);

    try (ScanWriter scanWriter = scanWriterFactory.newWriter(scanFile)) {
      scan.getSummary().setStartTime();
      scanWriter.openScan(scan);
      scanWriter.writeConfiguration(scan.getConfiguration());
      scanWriter.writeMetadata(getScanMetadata(baseDirectory, envVars));
      ScanSession scanSession = new ScanSession(scan, scanWriter);
      scanSession.setLicensedFeatures(licensedFeatures);
      if (envVars != null) {
        envVars.forEach(scanSession::setEnvVar);
      }
      clientScanner.scan(new ClientScanRequest(scan));
      scanFiles(targets, scanSession, baseDirectory);
      scanModules(modules, scanSession, baseDirectory);
      populateInstanceId(scan.getSummary(), instanceId);
      scanWriter.writeSummary(scan.getSummary());
      scanWriter.closeScan();
      scan.getSummary().setEndTime();
      return new ScanResult(scan, scanFile);
    }
  }

  // package visible only for testing
  ScanMetadata getScanMetadata(final Optional<File> baseDirectory, final Map<String, String> envVars) {
    Optional<String> commitHash = new CommitHashFinderBuilder()
        .withEnvironmentVariableDefault()
        .withGitRepoAtPath(baseDirectory.map(file -> file.getAbsolutePath() + "/.git").orElse(null))
        .withEnvironmentOverride(envVars != null ? envVars : new HashMap<>())
        .withLogger(log)
        .build()
        .tryGetCommitHash();

    final ScanMetadata scanMetadata = new ScanMetadata();
    if (commitHash.isPresent()) {
      scanMetadata.setCommitHash(commitHash.get());
    }
    else {
      log.debug("Commit hash for scan with baseDirectory: {} could not be found.", baseDirectory);
    }

    final Optional<String> branchName = new BranchNameFinderBuilder()
        .withEnvironmentVariableDefault()
        .withGitRepoAtPath(baseDirectory.map(file -> file.getAbsolutePath() + "/.git").orElse(null))
        .withEnvironmentOverride(envVars != null ? envVars : new HashMap<>())
        .withLogger(log)
        .build()
        .getBranchName();

    if (branchName.isPresent()) {
      scanMetadata.setBranchName(branchName.get());
    }
    else {
      log.debug("Branch name for scan with baseDirectory: {} could not be found.", baseDirectory);
    }

    return scanMetadata;
  }

  private void populateInstanceId(final ScanSummary scanSummary, final Optional<String> instanceId) {
    if (instanceId.isPresent()) {
      scanSummary.putClientInfo("insight.instanceId", instanceId.get());
    }
    else {
      scanSummary.putClientInfo("insight.instanceId", generateInstanceId());
    }
  }

  private String generateInstanceId() {
    Preferences preferences = Preferences.userRoot().node("/com/sonatype/clm");
    String instanceId = preferences.get("instanceId", null);
    if (instanceId == null) {
      instanceId = UUID.randomUUID().toString().replace("-", "");
      preferences.put("instanceId", instanceId);
    }
    return instanceId;
  }

  // Visible for testing
  void scanFiles(final List<File> targets, final ScanSession scanSession, final Optional<File> baseDirectory) {
    FileScanRequest fileScanRequest = new FileScanRequest(scanSession, targets);
    if (baseDirectory.isPresent()) {
      fileScanRequest = fileScanRequest.setBasedir(baseDirectory.get());
    }
    fileScanner.scan(fileScanRequest);
  }

  private ScanConfiguration createScanConfiguration(final Properties config) throws IOException {
    Properties scanConfiguration = new Properties();
    scanConfiguration.putAll(config);
    scanPropertiesLoader.loadDefaults(scanConfiguration, "configuration.properties");
    scanPropertiesLoader.resolveAliases(scanConfiguration);
    return new ScanConfiguration(scanConfiguration);
  }

  void scanModules(final List<Module> modules, final ScanSession scanSession, final Optional<File> baseDirectory) {
    for (final Module module : modules) {
      ModuleScanRequest scanRequest = new ModuleScanRequest(scanSession);
      baseDirectory.ifPresent(directory -> scanRequest.setBasedir(directory));
      scanRequest.setModule(module.getId(), module.getIdKind(), module.getPathname());

      module.getConsumedArtifacts().stream().forEach(artifact -> {
        if (artifact.isMonitored()) {
          File file = new File(artifact.getPathname());
          String id = StringUtils.defaultIfBlank(artifact.getId(), "unknown:unknown:unknown");
          scanRequest.addConsumedFile(file, id);
        }
      });

      Map<String, List<String>> childrenByDependencyId = new HashMap<>();
      for (final Dependency dependency : module.getDependencies()) {
        List<String> childIds = getChildDependencyIds(scanRequest, dependency, childrenByDependencyId);
        List<String> existingChildIds = childrenByDependencyId.get(dependency.getId());
        if (existingChildIds == null || existingChildIds.size() < childIds.size()) {
          scanRequest.addDependency(dependency.getId(), dependency.isDirect(), childIds);
          childrenByDependencyId.put(dependency.getId(), childIds);
        }
      }

      fileScanner.scan(scanRequest);
    }
  }

  private List<String> getChildDependencyIds(
      ModuleScanRequest scanRequest,
      Dependency dependency,
      Map<String, List<String>> childrenByDependencyId)
  {
    List<String> ids = new ArrayList<>();
    for (Dependency child : dependency.getDependencies()) {
      ids.add(child.getId());
      List<String> childIds = getChildDependencyIds(scanRequest, child, childrenByDependencyId);
      List<String> existingChildIds = childrenByDependencyId.get(child.getId());
      if (existingChildIds == null || existingChildIds.size() < childIds.size()) {
        scanRequest.addDependency(child.getId(), false, childIds);
        childrenByDependencyId.put(child.getId(), childIds);
      }
    }
    return ids;
  }

  // Visible for Testing

  /**
   * Creates a temporary file for storing the scan results. If the environment variable "WORKSPACE_TMP" is set, the file
   * will be created in the specified directory. Otherwise, the file will be created in the default JVM temporary
   * directory.
   */
  protected File createScanFileInATempFolder(Map<String, String> envVars) throws IOException {
    String workspaceTempFolder = (envVars != null) ? envVars.get("WORKSPACE_TMP") : null;
    if (workspaceTempFolder == null || workspaceTempFolder.isBlank()) {
      return createScanFileInJavaDefaultTemp();
    }

    if (log != null) {
      log.debug("Using workspace temporary folder {} for storing the scan file.", workspaceTempFolder);
    }

    File workspaceTempDir = new File(workspaceTempFolder);

    if (!workspaceTempDir.exists() && !workspaceTempDir.mkdir()) {
      return createScanFileInJavaDefaultTemp();
    }

    try {
      return File.createTempFile("scan-", ".xml.gz", workspaceTempDir);
    }
    catch (IOException e) {
      if (log != null) {
        log.warn("Failed to use: {}", workspaceTempDir, e);
      }
      return createScanFileInJavaDefaultTemp();
    }
  }

  private File createScanFileInJavaDefaultTemp() throws IOException {
    if (log != null) {
      log.debug("Using default JVM temporary folder for storing the scan file.");
    }

    return File.createTempFile("scan-", ".xml.gz");
  }
}
