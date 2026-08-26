/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.gradle.plugins.scan.nexus.iq.client;

import java.util.Optional;

import com.sonatype.insight.brain.client.ConfigurationClient;
import com.sonatype.insight.brain.client.RestClientFactory;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;
import com.sonatype.insight.client.utils.UserAgentUtils;
import org.sonatype.gradle.plugins.scan.nexus.iq.client.common.Authentication;
import org.sonatype.gradle.plugins.scan.nexus.iq.client.common.ServerConfig;
import org.sonatype.gradle.plugins.scan.nexus.iq.client.impl.DefaultIqClient;
import org.sonatype.gradle.plugins.scan.nexus.iq.client.impl.ReportClientFactory;
import org.sonatype.gradle.plugins.scan.nexus.iq.client.impl.ScannerFactory;

import org.slf4j.Logger;

/**
 * Builder for creating {@link IqClient} instances.
 *
 * @since 1.0.1
 */
public class IqClientBuilder
{
  private ServerConfig serverConfig;

  private Optional<Logger> log = Optional.empty();

  private String userAgent;

  private IqClientBuilder() {
    // private constructor - use create()
  }

  /**
   * Creates a new builder instance.
   *
   * @return a new IqClientBuilder
   */
  public static IqClientBuilder create() {
    return new IqClientBuilder();
  }

  /**
   * Sets the server configuration.
   *
   * @param serverConfig the server configuration containing address and authentication
   * @return this builder
   */
  public IqClientBuilder withServerConfig(final ServerConfig serverConfig) {
    this.serverConfig = serverConfig;
    return this;
  }

  /**
   * Sets the logger for the client.
   *
   * @param logger the logger to use
   * @return this builder
   */
  public IqClientBuilder withLogger(final Logger logger) {
    this.log = Optional.ofNullable(logger);
    return this;
  }

  /**
   * Sets the user agent string for HTTP requests.
   *
   * @param userAgent the user agent string
   * @return this builder
   */
  public IqClientBuilder withUserAgent(final String userAgent) {
    this.userAgent = userAgent;
    return this;
  }

  /**
   * Builds the IqClient instance.
   *
   * @return a new IqClient configured with the provided settings
   */
  public IqClient build() {
    Configuration configuration = buildConfiguration(serverConfig);
    RestClientFactory.RestClient restClient =
        new RestClientFactory().newRestCIClient(configuration, log.orElse(null));
    ConfigurationClient configurationClient = new ConfigurationClient(configuration);
    ReportClientFactory reportClientFactory = new ReportClientFactory(configuration);

    return new DefaultIqClient(
        configurationClient,
        new ScannerFactory(),
        restClient,
        reportClientFactory,
        log,
        Optional.empty());
  }

  private Configuration buildConfiguration(final ServerConfig serverConfig) {
    Configuration config = new Configuration();
    config.setServerUrl(serverConfig.getAddress().toString());

    Authentication authentication = serverConfig.getAuthentication();
    if (authentication != null) {
      SimpleAuthentication simpleAuthentication = new SimpleAuthentication();
      simpleAuthentication.setUsername(authentication.getUsername());
      simpleAuthentication.setPassword(new String(authentication.getPassword()));
      config.setServerAuth(simpleAuthentication);
    }

    config.setUserAgent(userAgent == null ? UserAgentUtils.getDefaultUserAgent() : userAgent);

    return config;
  }
}
