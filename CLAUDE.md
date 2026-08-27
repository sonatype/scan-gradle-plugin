# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A single Gradle plugin (`org.sonatype.gradle.plugins.scan`, a.k.a. *Sherlock Trunks*) that scans a Gradle
project's dependencies against Sonatype platforms. Written in Java 17. Published to the Gradle Plugins Portal
and Maven Central. For deep architecture and release detail see `docs/overview.md` — this file covers only what
you need day to day.

## Build & test commands

The build **requires Java 17** and fails fast if the running JVM is anything else. Always use the wrapper.

```bash
./gradlew build                                      # full build + unit tests + license check + integration tests
./gradlew clean publishToMavenLocal                  # build and install into ~/.m2 for local plugin testing
./gradlew clean publishToMavenLocal -x integrationTest  # skip the slow integration tests
./gradlew test                                       # unit tests only
./gradlew integrationTest                            # all integration tests (depends on it1–it4)
./gradlew it1                                        # one integration-test group (it1, it2, it3, or it4)
```

Run a single unit test class/method with the standard Gradle filter:

```bash
./gradlew test --tests '*DependenciesFinderTest'
./gradlew test --tests 'org.sonatype.gradle.plugins.scan.ossindex.VulnerabilityUtilsTest.someMethod'
```

**Required env vars for any build**: `NEXUS_RM_USERNAME` and `NEXUS_RM_PASSWORD`. The IQ/CLM dependencies
(`insight-brain-client`, `insight-scanner-*`, `com.sonatype.clm.dto.model`) resolve only from the private
Sonatype Maven repo configured in `build.gradle`; without these credentials the build cannot resolve dependencies.

## Architecture

`ScanPlugin` (`src/main/java/.../scan/ScanPlugin.java`) is the entry point. It registers three tasks, each with a
matching configuration extension of the same name:

| Task           | Extension      | Package             | Purpose                                                             |
|----------------|----------------|---------------------|---------------------------------------------------------------------|
| `nexusIQScan`  | `nexusIQScan`  | `nexus/iq/scan/`    | Scan + evaluate dependencies against a Nexus IQ Server / Lifecycle. |
| `nexusIQIndex` | `nexusIQIndex` | `nexus/iq/index/`   | Write `module.xml` files that Sonatype CI tools consume.            |
| `ossIndexAudit`| `ossIndexAudit`| `ossindex/`         | Audit dependencies via OSS Index (now served by Sonatype Guide).    |

Shared code lives in `common/` — most importantly `DependenciesFinder` (dependency resolution) and the Android
variant/attribute disambiguation rules. `ossindex/` also holds the response handlers that render audit output as
plain text, dependency graph, or CycloneDx SBOM.

### Plugin-owned IQ client (`nexus/iq/client/`, `nexus/iq/api/`)

The plugin talks to IQ Server through its **own slim client** rather than the retired `nexus-platform-api`.
`IqClient` is the interface used by `NexusIqScanTask`; `client/impl/DefaultIqClient` is the implementation, built
via `IqClientBuilder`. The `nexus/iq/api/` package holds plugin-owned DTOs (`ApplicationPolicyEvaluation`,
`PolicyAlert`, `ComponentIdentifier`, etc.) that used to come from the platform API. When touching IQ behavior,
prefer this client surface — keep `IqClient` slim (only what tasks actually call).

### shadowJar relocation (important)

The published jar is assembled by the Gradle Shadow plugin and **relocates** `org.objectweb.asm` and
`org.apache.commons` under `org.sonatype.gradle.plugins.scan.shadow.*` to avoid clashing with the Gradle runtime.
Auto-relocation is disabled — relocations are declared explicitly in `build.gradle`'s `shadowJar` block. If you add
a dependency that could collide with Gradle's own classpath, add a relocation there.

## Gradle version compatibility

From release 4.0.0 the plugin targets Java 17 and supports **Gradle 7.6.4 and 8.4+**. Gradle 5.x/6.x (bundle
Groovy 2.x, can't run on Java 16+) and Gradle 8.3 (its bundled ASM can't read Java 25 class entries in
`bcprov-jdk18on`) are intentionally untested. Integration tests are split by target Gradle version into separate
`ScanIT_Gradle_Versions_*` classes, grouped into tasks `it1`–`it4` for parallel CI. See
`gradle/integration-test.gradle` for the mapping and the reasoning comments.

## CI & release

CI is **hybrid**: `Jenkinsfile` orchestrates but the actual Gradle build runs on GitHub Actions
(`.github/workflows/ci-build.yml`), triggered and polled by the `vars/*.groovy` steps (`runBuildWorkflow`, etc.).
Jenkins then runs a Nexus IQ policy evaluation on the built jars. The Jenkins shared-library steps refer to build
outputs under `target/` even though Gradle writes to `build/` — that's an artifact-collection mapping, not a
second build system.

Releases run from `Jenkinsfile.release` (version handling via `net.researchgate.release`; publishing via
`com.gradle.plugin-publish` to the Plugins Portal and JReleaser to Maven Central). Do not hand-edit release
bookkeeping — see `docs/overview.md` for the full flow.

## Conventions

- Every source file carries the Sonatype license header enforced by the `com.github.hierynomus.license` plugin
  (`header.txt`); `./gradlew build` will fail on a missing header. Run `./gradlew licenseFormat` to apply it.
- Tests use JUnit 4, AssertJ, and Mockito.
- Do not modify `releasenotes/CHANGELOG.md` or other release-notes files during feature/bugfix work; those change
  only at release time.
