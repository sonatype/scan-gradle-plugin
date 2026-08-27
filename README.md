<!--

    Copyright (c) 2020-present Sonatype, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->

<p align="center">
    <img src="https://github.com/sonatype/scan-gradle-plugin/blob/main/docs/images/sherlocktrunks.png" width="350"/>
</p>

# Sonatype Scan Gradle Plugin - AKA Sherlock Trunks #
[![Maven Central](https://img.shields.io/maven-central/v/org.sonatype.gradle.plugins/scan-gradle-plugin.svg?label=Maven%20Central)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.sonatype.gradle.plugins%22%20AND%20a%3A%22scan-gradle-plugin%22)

[![Gradle Plugins Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/org/sonatype/gradle/plugins/scan/org.sonatype.gradle.plugins.scan.gradle.plugin/maven-metadata.xml.svg?colorB=007ec6&label=Gradle%20Plugins%20Portal)](https://plugins.gradle.org/plugin/org.sonatype.gradle.plugins.scan)

![GitHub Actions CI](https://github.com/sonatype/scan-gradle-plugin/actions/workflows/ci-build.yaml/badge.svg)

Gradle plugin that scans the dependencies of a Gradle project using Sonatype platforms: Guide (through OSS Index compatibility) and Nexus IQ Server.

## Compile and Publish to Local Maven Cache

> ./gradlew clean publishToMavenLocal

If you want to save some time, skip integration tests:

> ./gradlew clean publishToMavenLocal -x integrationTest

## Run Integration Tests

> ./gradlew integrationTest

## Compatibility
The plugin from release 4.0.0 onwards, can be used with Java 17 installed locally on projects with Gradle versions:
- 7.6.4
- 8.4 or higher

Gradle 5.x and 6.x are no longer supported, since Java 17 requires Gradle 7.3 or higher.

Gradle 8.3 is no longer supported: it fails while instrumenting the plugin classpath, because its bundled ASM cannot
read the Java 25 (class file major version 69) entries under `META-INF/versions/25` in `bcprov-jdk18on`, a transitive
dependency of the Nexus Platform API. Gradle 8.4 and higher skip the entries they cannot read instead of failing.
`bcprov-jdk18on` cannot be downgraded to avoid this, as earlier versions carry known vulnerabilities.

It has ranges of supported versions due to a known bug in Gradle for plugins with multi-jar dependencies. See more at:
- https://github.com/gradle/gradle/issues/27156
- https://discuss.gradle.org/t/is-there-any-way-to-skip-gradle-instrumenting-classpath-file-transformer/45213/3

Releases 3.0.0 until 3.1.x can be used with Java 11 installed locally on projects with Gradle 5.0 until 6.4.1, 7.6.4, or 8.3 or higher.

All plugin releases prior to 3.0.0, can be used on projects with Gradle 3.3 or higher (local installation or wrapper) and Java 8 installed locally.

## Supported Programming Languages
Gradle can be used to build projects developed in various programming languages. This plugin supports:
- Java
- Kotlin
- Scala
- Groovy

## How to Use
- Create/Clone/Download any Gradle project.
- Edit its `build.gradle` file adding this:

```
plugins {
  id 'org.sonatype.gradle.plugins.scan' version '4.0.0' // Update the version as needed
}
```

- Or `build.gradle.kts`:
```
plugins {
    id ("org.sonatype.gradle.plugins.scan") version "4.0.0" // Update the version as needed
}
```

Some basic examples follow, which we strongly advise reading :)

After doing so, specific usage on CI tools can be found at https://github.com/guillermo-varela/example-scan-gradle-plugin

Also, when running the tasks please set the log level to `INFO` to see results using `-i` or `--info`:

```
./gradlew ossIndexAudit --info
./gradlew nexusIQScan --info
```

### Sonatype Guide (through OSS Index compatibility)
Originally, this plugin was integrated with OSS Index but OSS Index has been migrated to Sonatype Guide, meaning that
this plugin now gets to use the Sonatype Guide APIs to provide data about open source components with OSS Index compatibility.

More details about the OSS Index Migration to Sonatype Guide can be found at https://help.sonatype.com/en/oss-index-migration-to-sonatype-guide.html

While there is a free tier for Sonatype Guide, it does require authentication (no anonymous requests can be made).
See authentication documentation for Sonatype Guide with OSS Index compatibility at https://help.sonatype.com/en/using-guide-personal-access-tokens-with-oss-index-api-integrations.html

If you are using Groovy (build.gradle file):
```groovy
ossIndexAudit {
    username = 'email' // required, anonymous queries are not supported
    password = 'token' // required, anonymous queries are not supported
    allConfigurations = false // if true includes the dependencies in all resolvable configurations. By default is false, meaning only 'compileClasspath', 'runtimeClasspath', 'releaseCompileClasspath' and 'releaseRuntimeClasspath' are considered
    useCache = true // true by default
    cacheDirectory = 'some/path' // by default it uses the user data directory (according to OS)
    cacheExpiration = 'PT12H' // 12 hours if omitted. It must follow the Joda Time specification at https://www.javadoc.io/doc/joda-time/joda-time/2.10.4/org/joda/time/Duration.html#parse-java.lang.String-
    proxyConfiguration { // extra configuration when running behind a proxy without direct internet access
        protocol = 'http' // can be 'http' (default) or 'https'
        host = 'proxy-host' // hostname for the proxy
        port = 8080 // port for the proxy
        authConfiguration.username = 'username' // username for the proxy (if credentials are required)
        authConfiguration.password = 'password' // password for the proxy (if credentials are required)
    }
    modulesIncluded = ['module-1', 'module-2'] // Optional. For multi-module projects, the names of the sub-modules to include for auditing. If not specified all modules are included.
    modulesExcluded = ['module-1', 'module-2'] // Optional. For multi-module projects, the names of the sub-modules to exclude from auditing. If not specified no modules are excluded. This value is processed after 'modulesIncluded' if both are specified.

    // For projects using multiple custom variants for the release distribution, a Map can be set with the attributes names and values to match the specific variant. See more at the section "How to Deal with Multiple Release Variants" below in this doc.
    variantAttributes = ['com.android.build.api.attributes.ProductFlavor:version': 'prod', 'other.attribute': 'other value'] // Optional, use it only when the plugin can't match a variant on its own

    // ossIndexAudit can be configured to exclude vulnerabilities from matching
    excludeVulnerabilityIds = ['39d74cc8-457a-4e57-89ef-a258420138c5'] // list containing ids of vulnerabilities to be ignored
    excludeCoordinates = ['commons-fileupload:commons-fileupload:1.3'] // list containing coordinate of components which if vulnerable should be ignored
    excludeCompileOnly = true // if true then dependencies under the 'compileOnly' configuration will be ignored. By default is false

    // By default, the audit scan will fail the task/build if any vulnerabilities are found.
    // Set this to 'false' to allow the task to succeed even when vulnerabilities are detected.
    // Use this option only if you rely on an external tool to further process the output of this plugin (see below for output options).
    failOnDetection = true

    // Output options
    outputFormat = 'DEFAULT' // Optional, other values are: 'DEPENDENCY_GRAPH' prints dependency graph showing direct/transitive dependencies, 'JSON_CYCLONE_DX_1_4' prints a CycloneDX 1.4 SBOM in JSON format.
    cycloneDxComponentType = 'LIBRARY' // Optional, only used when outputFormat = 'JSON_CYCLONE_DX_1_4' to define the type of component this project is for the BOM metadata with possible values: 'LIBRARY' (default), 'APPLICATION', 'FRAMEWORK', 'CONTAINER', 'OPERATING_SYSTEM', 'DEVICE', 'FIRMWARE' and 'FILE'.
    isColorEnabled = false // if true (and outputFormat = "DEFAULT") prints vulnerability description in color. By default is true.
    showAll = false // if true prints all dependencies. By default is false, meaning only dependencies with vulnerabilities will be printed.
    printBanner = true // if true will print ASCII text banner. By default is true.
}
```

Or if you are using Kotlin (build.gradle.kts file):
```kotlin
ossIndexAudit {
    username = 'email' // required, anonymous queries are not supported
    password = 'token' // required, anonymous queries are not supported
    isAllConfigurations =
        false // if true includes the dependencies in all resolvable configurations. By default is false, meaning only "compileClasspath", "runtimeClasspath", "releaseCompileClasspath" and "releaseRuntimeClasspath" are considered
    isUseCache = true // true by default
    cacheDirectory = "some/path" // by default it uses the user data directory (according to OS)
    cacheExpiration =
        "PT12H" // 12 hours if omitted. It must follow the Joda Time specification at https://www.javadoc.io/doc/joda-time/joda-time/2.10.4/org/joda/time/Duration.html#parse-java.lang.String-
    proxyConfiguration { // extra configuration when running behind a proxy without direct internet access
        protocol = "http" // can be "http" (default) or "https"
        host = "proxy-host" // hostname for the proxy
        port = 8080 // port for the proxy
        authConfiguration.username = "username" // username for the proxy (if credentials are required)
        authConfiguration.password = "password" // password for the proxy (if credentials are required)
    }
    modulesIncluded = listOf("module-1", "module-2") // Optional. For multi-module projects, the names of the sub-modules to include for auditing. If not specified all modules are included.
    modulesExcluded = listOf("module-1", "module-2") // Optional. For multi-module projects, the names of the sub-modules to exclude from auditing. If not specified no modules are excluded. This value is processed after 'modulesIncluded' if both are specified.

    // For projects using multiple custom variants for the release distribution, a Map can be set with the attributes names and values to match the specific variant. See more at the section "How to Deal with Multiple Release Variants" below in this doc.
    variantAttributes = mapOf("com.android.build.api.attributes.ProductFlavor:version" to "prod", "other.attribute" to "other value") // Optional, use it only when the plugin can't match a variant on its own

    // ossIndexAudit can be configured to exclude vulnerabilities from matching
    excludeVulnerabilityIds =
        listOf("39d74cc8-457a-4e57-89ef-a258420138c5") // list containing ids of vulnerabilities to be ignored
    excludeCoordinates =
        listOf("commons-fileupload:commons-fileupload:1.3") // list containing coordinate of components which if vulnerable should be ignored
    excludeCompileOnly = true // if true then dependencies under the 'compileOnly' configuration will be ignored. By default is false

    // By default, the audit scan will fail the task/build if any vulnerabilities are found.
    // Set this to 'false' to allow the task to succeed even when vulnerabilities are detected.
    // Use this option only if you rely on an external tool to further process the output of this plugin (see below for output options).
    failOnDetection = true

    // Output options
    outputFormat = "DEFAULT" // Optional, other values are: "DEPENDENCY_GRAPH" prints dependency graph showing direct/transitive dependencies, "JSON_CYCLONE_DX_1_4" prints a CycloneDX 1.4 SBOM in JSON format.
    cycloneDxComponentType = "LIBRARY" // Optional, only used when outputFormat = "JSON_CYCLONE_DX_1_4" to define the type of component this project is for the BOM metadata with possible values: "LIBRARY" (default), "APPLICATION", "FRAMEWORK", "CONTAINER", "OPERATING_SYSTEM", "DEVICE", "FIRMWARE" and "FILE".
    isColorEnabled = false // if true (and outputFormat = "DEFAULT") prints vulnerability description in color. By default is true.
    isShowAll = false // if true prints all dependencies. By default is false, meaning only dependencies with vulnerabilities will be printed.
    isPrintBanner = true // if true will print ASCII text banner. By default is true.
}
```

- Open Terminal on the project's root and run `./gradlew ossIndexAudit`
- You should see the audit result on Terminal.

### Nexus IQ Server Scan and Evaluate
- Start a local instance of IQ Server, or get the URL and credentials of a remote one.
- Configure IQ Server settings inside the `nexusIQScan` configuration on the file `build.gradle` e.g.

Groovy:
```
nexusIQScan {
    username = 'admin' // Make sure to use an user with the role 'Application Evaluator' in the given IQ Server application
    password = 'pass'
    serverUrl = 'http://localhost:8070'
    applicationId = 'app'
    organizationId = 'orgId' // Optional. If provided, a validation will be done to check if the given application ID exists under the organization ID (please note this is different than the organization name). If the application doesn't exists, then it will be created under the organization.
    stage = 'build' // build is used if omitted
    allConfigurations = false // if true includes the dependencies in all resolvable configurations. By default is false, meaning only 'compileClasspath', 'runtimeClasspath', 'releaseCompileClasspath' and 'releaseRuntimeClasspath' are considered
    resultFilePath = 'results.json' // Optional. JSON file containing results of the evaluation
    modulesExcluded = ['module-1', 'module-2'] // Optional. For multi-module projects, the names of the sub-modules to exclude from scanning and evaluation.
    dirExcludes = 'some-ant-pattern' // Optional. Comma separated ant-like glob patterns to select directories/archives that should be excluded. For Android projects we suggest using '**/classes.jar,**/annotations.zip,**/lint.jar,**/internal_impl-*.jar'
    dirIncludes = 'some-ant-pattern' // Optional. Comma separated ant-like glob patterns to select directories/archives that should be examined

    // For projects using multiple custom variants for the release distribution, a Map can be set with the attributes names and values to match the specific variant. See more at the section "How to Deal with Multiple Release Variants" below in this doc.
    variantAttributes = ['com.android.build.api.attributes.ProductFlavor:version': 'prod', 'other.attribute': 'other value'] // Optional, use it only when the plugin can't match a variant on its own
    scanTargets = ['package-lock.json', '**/*.lock'] // Optional. Ant-like glob patterns for relative paths (to the project's folder) to select additional files to be scanned and evaluated.
    excludeCompileOnly = true // if true then dependencies under the 'compileOnly' configuration will be ignored. By default is false.
}
```

Kotlin:
```
nexusIQScan {
    username = "admin" // Make sure to use an user with the role "Application Evaluator" in the given IQ Server application
    password = "pass"
    serverUrl = "http://localhost:8070"
    applicationId = "app"
    organizationId = "orgId" // Optional. If provided, a validation will be done to check if the given application ID exists under the organization ID (please note this is different than the organization name). If the application doesn"t exists, then it will be created under the organization.
    stage = "build" // build is used if omitted
    isAllConfigurations = false // if true includes the dependencies in all resolvable configurations. By default is false, meaning only "compileClasspath", "runtimeClasspath", "releaseCompileClasspath" and "releaseRuntimeClasspath" are considered
    resultFilePath = "results.json" // Optional. JSON file containing results of the evaluation
    modulesExcluded = listOf("module-1", "module-2") // Optional. For multi-module projects, the names of the sub-modules to exclude from scanning and evaluation.
    dirExcludes = "some-ant-pattern" // Optional. Comma separated ant-like glob patterns to select directories/archives that should be excluded. For Android projects we suggest using "**/classes.jar,**/annotations.zip,**/lint.jar,**/internal_impl-*.jar"
    dirIncludes = "some-ant-pattern" // Optional. Comma separated ant-like glob patterns to select directories/archives that should be examined

    // For projects using multiple custom variants for the release distribution, a Map can be set with the attributes names and values to match the specific variant. See more at the section "How to Deal with Multiple Release Variants" below in this doc.
    variantAttributes = mapOf("com.android.build.api.attributes.ProductFlavor:version" to "prod", "other.attribute" to "other value") // Optional, use it only when the plugin can't match a variant on its own
    scanTargets = listOf("package-lock.json", "**/*.lock") // Optional. Ant-like glob patterns for relative paths (to the project's folder) to select additional files to be scanned and evaluated.
    excludeCompileOnly = true // if true then dependencies under the 'compileOnly' configuration will be ignored. By default is false.
}
```

- Open Terminal on the project's root and run `./gradlew nexusIQScan`
- You should see the scan report URL report on Terminal.

### Nexus IQ Index
Allows you to save information about the dependencies of a project into module information (`module.xml`) files that Sonatype CI tools can use to include these dependencies in a scan.

- Open Terminal on the project's root and run `./gradlew nexusIQIndex`

For multi-module projects, you can configure a list of sub-modules to exclude from indexing.
Groovy:
```
nexusIQIndex {
     modulesExcluded = ['module-1', 'module-2'] // Optional. For multi-module projects, the names of the sub-modules to exclude from indexing.
     excludeCompileOnly = true // if true then dependencies under the 'compileOnly' configuration will be ignored. By default is false.
}
```

Kotlin:
```
nexusIQIndex {
     modulesExcluded = listOf("module-1", "module-2") // Optional. For multi-module projects, the names of the sub-modules to exclude from indexing.
     excludeCompileOnly = true // if true then dependencies under the 'compileOnly' configuration will be ignored. By default is false.
}
```

### Sensitive Data
Sometimes it's not desirable to keep sensitive data stored on `build.gradle`. For such cases it's possible to use project properties (-P arguments) or system properties (-D arguments or injected from a tool) from command line or environment variables when running the `nexusIQScan` or `ossIndexAudit` tasks.

Here is an example using project properties for the credentials, Groovy
```
nexusIQScan {
    username = project['username']
    password = project['password']
    serverUrl = 'http://localhost:8070'
    applicationId = 'app'
}

ossIndexAudit {
    username = project['username']
    password = project['password']
}
```

Kotlin:

```
nexusIQScan {
    username = project["username"]
    password = project["password"]
    serverUrl = "http://localhost:8070"
    applicationId = "app"
}

ossIndexAudit {
    username = project["username"]
    password = project["password"]
}
```

On command line:
```
./gradlew nexusIQScan -Pusername=admin -Ppassword=pass
```

```
./gradlew ossIndexAudit -Pusername=admin -Ppassword=pass
```

Each property name can be set as needed.

Here is an example using system properties for the credentials (Groovy):

```
nexusIQScan {
    username = System.properties['username']
    password = System.properties['password']
    serverUrl = 'http://localhost:8070'
    applicationId = 'app'
}

ossIndexAudit {
    username = System.properties['username']
    password = System.properties['password']
}
```

As mentioned above the values can be set on command line using -D arguments or injected via a tool (CI/CD for instance).

Finally this is how environment variables can be used (usually values are injected from the local environment or by a CI tool, Groovy):
```
nexusIQScan {
    username = System.getenv('username')
    password = System.getenv('password')
    serverUrl = 'http://localhost:8070'
    applicationId = 'app'
}

ossIndexAudit {
    username = System.getenv('username')
    password = System.getenv('password')
}
```

Kotlin version:
```
nexusIQScan {
    username = System.getenv("username")
    password = System.getenv("password")
    serverUrl = "http://localhost:8070"
    applicationId = "app"
}

ossIndexAudit {
    username = System.getenv("username")
    password = System.getenv("password")
}
```

### Multi-module projects
Just apply the plugin on the root project and all sub-modules will be processed and the output will be a single report
with all components found in each module. This includes Android projects.

## How to Deal with Multiple Release Variants
This plugin makes its best effort to find the release (production) configuration and variant to get the dependencies to analyze.

However, a Gradle project can have multiple custom release variants and the plugin might not be able to tell Gradle which one to pick, resulting in an error like this:

```
> Could not resolve all dependencies for configuration 'sonatypeCopyConfiguration0'.
   > Could not resolve project :common-lib.
     Required by:
         project :app
      > The consumer was configured to find a runtime of a component, as well as attribute 'com.android.build.api.attributes.BuildTypeAttr' with value 'release'. However we cannot choose between the following variants of project :baseapp:
          - ciReleaseRuntimeElements
          - prodReleaseRuntimeElements
        All of them match the consumer attributes:
          - Variant 'ciReleaseRuntimeElements' capability common-lib:1.0.0 declares a runtime of a component, as well as attribute 'com.android.build.api.attributes.BuildTypeAttr' with value 'release':
              - Unmatched attributes:
                  - Provides attribute 'com.android.build.api.attributes.AgpVersionAttr' with value '7.2.2' but the consumer didn't ask for it
                  - Provides attribute 'com.android.build.api.attributes.ProductFlavor:version' with value 'ci' but the consumer didn't ask for it
                  - Provides attribute 'com.android.build.gradle.internal.attributes.VariantAttr' with value 'ciRelease' but the consumer didn't ask for it
                  - Provides a library but the consumer didn't ask for it
                  - Provides attribute 'org.gradle.jvm.environment' with value 'android' but the consumer didn't ask for it
          - Variant 'prodReleaseRuntimeElements' capability common-lib:1.0.0 declares a runtime of a component, as well as attribute 'com.android.build.api.attributes.BuildTypeAttr' with value 'release':
              - Unmatched attributes:
                  - Provides attribute 'com.android.build.api.attributes.AgpVersionAttr' with value '7.2.2' but the consumer didn't ask for it
                  - Provides attribute 'com.android.build.api.attributes.ProductFlavor:version' with value 'prod' but the consumer didn't ask for it
                  - Provides attribute 'com.android.build.gradle.internal.attributes.VariantAttr' with value 'prodRelease' but the consumer didn't ask for it
                  - Provides a library but the consumer didn't ask for it
                  - Provides attribute 'org.gradle.jvm.environment' with value 'android' but the consumer didn't ask for it
```

From that output we can see the value of the attribute `com.android.build.api.attributes.ProductFlavor:version` can be used to distinguish between the available variants.

Since attribute names and values can be customized on each project, this plugin allows to set the attributes needed to match the right variant using the property `variantAttributes`.

In the example above, the following configuration would allow the plugin to choose the `prodReleaseRuntimeElements` variant:

*Groovy*
```
nexusIQScan {
    variantAttributes = ['com.android.build.api.attributes.ProductFlavor:version': 'prod']
}

ossIndexAudit {
    variantAttributes = ['com.android.build.api.attributes.ProductFlavor:version': 'prod']
}
```

*Kotlin*
```
nexusIQScan {
    variantAttributes = mapOf("com.android.build.api.attributes.ProductFlavor:version" to "prod")
}

ossIndexAudit {
    variantAttributes = mapOf("com.android.build.api.attributes.ProductFlavor:version" to "prod")
}
```

See more information about attributes matching for variant selection see https://docs.gradle.org/current/userguide/variant_model.html#sec:variant-select-errors

## Contributing

We care a lot about making the world a safer place, and that's why we created this `scan-gradle-plugin`. If you as well want to speed up the pace of software development by working on this project, jump on in! Before you start work, create a new issue, or comment on an existing issue, to let others know you are!

Check the full contributing guidelines at: [CONTRIBUTING.md](./.github/CONTRIBUTING.md)

## The Fine Print

The `scan-gradle-plugin` is supported by Sonatype. Customers may make support requests to [https://support.sonatype.com](https://support.sonatype.com). 

## Getting help

Looking to contribute to our code but need some help? There's a few ways to get information:

* Chat with us on [Gitter](https://gitter.im/sonatype/nexus-developers)
