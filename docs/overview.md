<!--

    Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at https://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->

# Document Purpose

This page provides high-level technical information regarding the **Sonatype Scan Gradle Plugin** (a.k.a. *Sherlock
Trunks*).

## Product Overview

The plugin scans the dependencies of a Gradle project using Sonatype platforms: **Sonatype Guide** (through OSS Index
compatibility) and **Nexus IQ Server** / Lifecycle.

For usage, configuration options and compatibility details, see the [README.md](../README.md). The plugin is published
on the [Gradle Plugins Portal][1] and [Maven Central][2]. CI usage examples live in the
[example-scan-gradle-plugin][3] repository.

## High-Level Technical Description

The project produces a single Gradle plugin, `org.sonatype.gradle.plugins.scan`, written in **Java 17**. The entry
point is [`ScanPlugin`](../src/main/java/org/sonatype/gradle/plugins/scan/ScanPlugin.java), which registers three
tasks, each with its own configuration extension:

| Task           | Extension          | Purpose                                                                                           |
|----------------|--------------------|---------------------------------------------------------------------------------------------------|
| `nexusIQScan`  | `nexusIQScan`      | Scan and evaluate the project's dependencies against a Nexus IQ Server.                           |
| `nexusIQIndex` | `nexusIQIndex`     | Write dependency information into module (`module.xml`) files that Sonatype CI tools can consume. |
| `ossIndexAudit`| `ossIndexAudit`    | Audit the project's dependencies using OSS Index (now served by Sonatype Guide).                  |

The main source is organized under [`src/main/java/org/sonatype/gradle/plugins/scan`](../src/main/java/org/sonatype/gradle/plugins/scan):

- `common/` – dependency resolution helpers ([`DependenciesFinder`](../src/main/java/org/sonatype/gradle/plugins/scan/common/DependenciesFinder.java))
  and Android variant/attribute disambiguation rules.
- `ossindex/` – the `ossIndexAudit` task, its extension, and the response handlers that render results
  (plain text, dependency graph, CycloneDx SBOM).
- `nexus/iq/scan/` – the `nexusIQScan` task and extension.
- `nexus/iq/index/` – the `nexusIQIndex` task and extension.

Notable runtime dependencies (declared in [`build.gradle`](../build.gradle)):

- `com.sonatype.insight.brain:insight-brain-client` – Nexus IQ / Lifecycle policy evaluation client.
- `com.sonatype.insight.scan:insight-scanner-archive` (with `insight-client-utils`, `insight-scanner-model`, `insight-module-model`) – Component scanning and module model for Nexus IQ.
- `com.sonatype.clm:com.sonatype.clm.dto.model` – Data transfer objects for IQ Server communication.
- `org.sonatype.ossindex:ossindex-service-client` – OSS Index / Sonatype Guide client.

The plugin supports projects written in Java, Kotlin, Scala and Groovy.

## Data Persistence

The plugin persists no data of its own. The `ossIndexAudit` task can maintain an optional local response cache
(enabled by default, configurable via `useCache` / `cacheDirectory` / `cacheExpiration`).

## Local Development

### Build

The build is Gradle based and requires **Java 17** locally (the build fails fast if the running JVM is not 17). Use the
Gradle wrapper:

```bash
# full build, including unit tests
./gradlew build

# build and install into the local Maven cache (~/.m2)
./gradlew clean publishToMavenLocal

# skip the (slow) integration tests
./gradlew clean publishToMavenLocal -x integrationTest
```

The plugin's IQ-related dependencies (`insight-brain-client`, `insight-scanner-*`, `com.sonatype.clm.dto.model`)
are resolved from an internal Sonatype Maven repository (`srsa`), so the `NEXUS_RM_USERNAME` and
`NEXUS_RM_PASSWORD` environment variables must be set.

The final plugin jar is assembled by the [Gradle Shadow plugin][4] (`shadowJar`), which bundles the plugin and its
dependencies and **relocates** `org.objectweb.asm` and `org.apache.commons` under
`org.sonatype.gradle.plugins.scan.shadow.*` to avoid conflicts with the Gradle runtime. Licenses on source files are
enforced by the `com.github.hierynomus.license` plugin using [`header.txt`](../header.txt).

### Tests

- **Unit tests** live under `src/test` (JUnit 4, AssertJ, Mockito) and run as part of `./gradlew test` / `build`.
- **Integration tests** live under `src/integTest` and use the [Gradle TestKit][5] to run the plugin against real
  Gradle runtimes. They are wired up in [`gradle/integration-test.gradle`](../gradle/integration-test.gradle).

Integration tests are split by target Gradle version into separate classes (e.g.
`ScanIT_Gradle_Versions_8_4_to_8_5`, `..._8_6_to_8_10`, `..._8_11_to_8_14`). For parallel CI execution these are
grouped into four tasks, `it1`–`it4`, which the aggregate `integrationTest` task depends on:

```bash
# run all integration tests
./gradlew integrationTest

# run a single group
./gradlew it1
```

Both unit and integration tests can also be run directly from the IDE.

> **Compatibility note:** From release 4.0.0 the plugin targets Java 17 and supports Gradle 7.6.4 and 8.4+. Gradle 5.x,
> 6.x and 8.3 are intentionally not tested — see the *Compatibility* section of the [README.md](../README.md) and the
> comments in [`gradle/integration-test.gradle`](../gradle/integration-test.gradle) for the reasons.

## Continuous Integration

CI uses a **hybrid model**: Jenkins orchestrates the pipeline while the actual Gradle build runs on GitHub Actions.

- [`Jenkinsfile`](../Jenkinsfile) drives the per-branch build. It calls
  [`runBuildWorkflow`](../vars/runBuildWorkflow.groovy), which triggers the GitHub Actions
  [`ci-build.yml`](../.github/workflows/ci-build.yml) workflow on the branch being built, polls it to completion, and
  downloads the resulting artifacts. Jenkins then runs a **Nexus IQ policy evaluation** (with Java reachability
  analysis) on the built jars and publishes the test results.
- [`.github/workflows/ci-build.yml`](../.github/workflows/ci-build.yml) contains a `gradle-build` job (license check,
  build, unit tests, `copyDependencies`) and an `it-tests` matrix job that runs the integration-test groups
  `it1`–`it4` in parallel. Built jars, dependency jars and test result XML are uploaded as workflow artifacts for
  Jenkins to consume.

Loading this repository's own `vars/*.groovy` from the branch under build keeps the `Jenkinsfile` and shared-library
steps in sync.

## Publishing / Release

Releases are driven by [`Jenkinsfile.release`](../Jenkinsfile.release):

1. **Prepare** – [`updateVersion`](../vars/updateVersion.groovy) removes `-SNAPSHOT` from `version` in
   [`gradle.properties`](../gradle.properties) and commits/pushes the release version to `main`.
2. **Build and publish** – [`runReleaseWorkflow`](../vars/runReleaseWorkflow.groovy) triggers the GitHub Actions
   [`ci-release.yml`](../.github/workflows/ci-release.yml) workflow, which:
   - runs `./gradlew publish publishPlugins` to publish the plugin to the [Gradle Plugins Portal][1] (via the
     `com.gradle.plugin-publish` plugin, using `GRADLE_PUBLISH_KEY` / `GRADLE_PUBLISH_SECRET`); and
   - runs `./gradlew jreleaserFullRelease` to publish signed artifacts to [Maven Central][2] via
     [JReleaser][6] (GPG signing plus `JRELEASER_MAVENCENTRAL_*` credentials).
3. **Policy evaluation** – a final Nexus IQ evaluation runs against the `release` stage.
4. **Finish** – the fix version is recorded in Jira (`jiraSetFixVersion`) and `updateVersion` bumps
   `gradle.properties` to the next `-SNAPSHOT` version, which is committed back to `main`.

Version bookkeeping uses the `net.researchgate.release` Gradle plugin. Releases with SNAPSHOT dependencies are blocked
when the build runs with `-PisReleaseJob`.

## References

- [Gradle Plugins Portal – scan plugin][1]
- [Maven Central – scan-gradle-plugin][2]
- [CI usage examples][3]
- [Gradle Shadow plugin][4]
- [Gradle TestKit][5]
- [JReleaser][6]

[1]: https://plugins.gradle.org/plugin/org.sonatype.gradle.plugins.scan
[2]: https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.sonatype.gradle.plugins%22%20AND%20a%3A%22scan-gradle-plugin%22
[3]: https://github.com/guillermo-varela/example-scan-gradle-plugin
[4]: https://gradleup.com/shadow/
[5]: https://docs.gradle.org/current/userguide/test_kit.html
[6]: https://jreleaser.org/
