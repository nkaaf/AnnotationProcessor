# Annotation Processor

[![License](https://img.shields.io/github/license/nkaaf/AnnotationProcessor)](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt)

[![DeepSource](https://deepsource.io/gh/nkaaf/AnnotationProcessor.svg/?label=active+issues&show_trend=true)](https://deepsource.io/gh/nkaaf/AnnotationProcessor/?ref=repository-badge)
[![Build Status](https://travis-ci.com/nkaaf/AnnotationProcessor.svg?branch=master)](https://travis-ci.com/nkaaf/AnnotationProcessor)

[![GitHub all releases](https://img.shields.io/github/downloads/nkaaf/AnnotationProcessor/total)](https://github.com/nkaaf/AnnotationProcessor/releases)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.nkaaf/annotationprocessor)](https://mvnrepository.com/artifact/io.github.nkaaf/annotationprocessor)

[![GitHub forks](https://img.shields.io/github/forks/nkaaf/AnnotationProcessor?style=social)](https://github.com/nkaaf/AnnotationProcessor/network/members)
[![GitHub Repo stars](https://img.shields.io/github/stars/nkaaf/AnnotationProcessor?style=social)](https://github.com/nkaaf/AnnotationProcessor/stargazers)
[![GitHub watchers](https://img.shields.io/github/watchers/nkaaf/AnnotationProcessor?style=social)](https://github.com/nkaaf/AnnotationProcessor/watchers)

## Index

- [Use Case](#use-case)
- Usage
    - [Build System](#build-system)
        - [Maven](#usage-maven)
        - [Gradle](#usage-gradle)
    - [Java](#java)
        - [Non-Modular](#non-modular)
        - [Modular (Java 9+)](#modular-java-9)
            - [Maven](#java-maven)
            - [Gradle](#java-gradle)
- [Developing](#developing)
    - [The Problem with Multi-Release JARs and IDEs](#the-problem-with-multi-release-jars-and-ides)
    - [Testing](#testing)
        - [Needed Components](#needed-components)
        - [Used SDKMAN! JDKs](#used-sdkman-jdks)
        - [Used SDKMAN! Maven](#used-sdkman-maven)
        - [Used Java Libraries](#used-java-libraries)
- [License](#license)
    - [Licenses of used Libraries](#licenses-of-used-libraries-and-tools)

## Use Case

In case you want to create a new annotation processor, you can use the annotation <strong>@AnnotationProcessor</strong>,
to automatically create the required <strong>javax.annotation.processing.Processor</strong> file in the
<strong>META-INF/services/</strong> directory at compile time.

> ❗ To use this properly, see how to import it into your [Build System](#build-system) and integrate it into [Java](#java) ❗

**[↑ Back to Index](#index)**

## Build System

### <a name="usage-maven"></a> Maven

```xml
<dependency>
    <groupId>io.github.nkaaf</groupId>
    <artifactId>annotationprocessor</artifactId>
    <version>1.0</version>
</dependency>
```

**[↑ Back to Index](#index)**

### <a name="usage-gradle"></a> Gradle

```groovy
plugins {
    id 'java-library'
}

dependencies {
    compileOnly 'io.github.nkaaf:annotationprocessor:1.0'
    annotationProcessor 'io.github.nkaaf:annotationprocessor:1.0'
}
```

**[↑ Back to Index](#index)**

## Java

Annotate your annotation processor with <strong>@AnnotationProcessor</strong>. The processor behind this annotation
checks if your annotation processor is built conforming ([JSP 269](https://www.jcp.org/en/jsr/detail?id=269)). You can
either extend your processor with <strong>javax.annotation.processing.AbstractProcessor</strong> or directly implement
it with <strong>javax.annotation.processing.Processor</strong>.

**[↑ Back to Index](#index)**

### Non-Modular

You only need to import the dependency with your build system.

> [Maven Example](examples/maven/non-modular/pom.xml)
>
> [Gradle Example](examples/gradle/non-modular/build.gradle)

**[↑ Back to Index](#index)**

### Modular (Java 9+)

You need to add the dependencies' module in the <strong>module-info.java</strong> of the desired module. In addition,
you need to set it up in your build system ([Maven](#java-maven)/ [Gradle](#java-gradle)).

**[↑ Back to Index](#index)**

#### <a name="java-maven"></a> Maven

Your module only needs to require the io.github.nkaaf.annotationprocessor module. It gets the module java.compiler
automatically, so you don't need it additionally for your annotation processor.

```java
module yourModule {
    requires static io.github.nkaaf.annotationprocessor;
}
```

You need to add the dependency as an annotation path in maven-compiler-plugin.

```xml
<pluin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.8.1</version>
    <configuration>
        <release>JAVA_VERSION_GREATER_OR_EQUALS_9</release>
        <annotationProcessorPaths>
            <annotationProcessorPath>
                <groupId>io.github.nkaaf</groupId>
                <artifactId>annotationprocessor</artifactId>
                <version>1.0</version>
            </annotationProcessorPath>
        </annotationProcessorPaths>
    </configuration>
</pluin>
```

> [Maven Example](examples/maven/modular/pom.xml)

**[↑ Back to Index](#index)**

#### <a name="java-gradle"></a> Gradle (6.4+)

Your module must require the io.github.nkaaf.annotationprocessor module.

> ❗ It will NOT automatically get the java.compiler module (different behaviour than in Maven) ❗

```java
module yourModule {
    requires static io.github.nkaaf.annotationprocessor;
}
```

You also need to turn on module path inference and add the release flag.

> ❗ You cannot use JDK 9 because the release flag causes an error with Gradle. JDK 10 causes another error with
> Gradle, but the Java 10 release flag can be used with JDK 12 and later. See the error tickets linked in example.
> But you can use the release flag 11, and a JDK 11 ❗

```groovy
java {
    modularity.inferModulePath.set(true)
}

// < Gradle 6.6
compileJava {
    options.compilerArgs.addAll(['--release', '11'])
}

// >= Gradle 6.6
compileJava {
    options.release.set(11)
}
```

> [Gradle Example](examples/gradle/modular/build.gradle)

**[↑ Back to Index](#index)**

## Developing

To ensure maximum compatibility, this project must be compiled with Java 9.

**[↑ Back to Index](#index)**

### The Problem with Multi-Release JARs and IDEs

Most IDEs do not support multi-release jars properly. The problem is that the package and class names are identical. The
IDEs cannot compile them, even though this mechanism is clearly defined in the Maven POM.

**[↑ Back to Index](#index)**

### Testing

There is also a problem with the tests. There is no automatic way to change the JDKs, so that both versions of my
annotation processor are tested. My solution is a bash script that compiles and tests the classes. It uses
[SDKMAN!](https://sdkman.io/) and the [Junit Jupiter Engine](https://junit.org/junit5/), which is imported by
[Maven](https://maven.apache.org/). You can easily run the [test script](src/test/test.sh) with bash from anywhere on
your computer. If you do not have the required Java Libraries installed in the Maven folder, the script downloads them.
This also applies to the JDKs with SDKMAN!. The mechanism for switching JDKs with SDKMAN! is not perfect for my purpose,
because it depends on hardcoded Java versions. These can be deleted at any time in the lists of SDKMAN! without me
notice it.

**[↑ Back to Index](#index)**

#### Needed Components

- [Bash](https://www.gnu.org/software/bash/)
- [SDKMAN!](https://sdkman.io/)
- [Maven](https://maven.apache.org/)

**[↑ Back to Index](#index)**

#### Used SDKMAN! JDKs

- 8.0.282-zulu
- 11.0.10-zulu
- 15.0.2-sapmchn
- 16.0.1-zulu

Also tested with following, at the moment not available, SDKMAN! JDKs:

- 12.0.2-sapmchn
- 13.0.2-sapmchn
- 14.0.2-sapmchn
- 15.0.2-zulu

**[↑ Back to Index](#index)**

#### Used SDKMAN! Maven

- 3.8.1

Also tested with following SDKMAN! Maven Versions:

- 3.6.3

**[↑ Back to Index](#index)**

#### Used Java Libraries

- [JUnit Jupiter API 5.7.2](https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api/5.7.2)
- [JUnit Platform Console Standalone 1.7.2](https://mvnrepository.com/artifact/org.junit.platform/junit-platform-console-standalone/1.7.2)
  (Used in testing script for command line support)

**[↑ Back to Index](#index)**

## License

This Project is licensed under the GNU Lesser General Public License 2.1 or any
later ([LGPL 2.1](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt)).

### Licenses of used Libraries and Tools

This list includes only Libraries and tools that are explicit imported/used in this project.

- [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)
    - [Maven](https://github.com/apache/maven)
    - [Maven Compiler Plugin 3.8.1](https://github.com/apache/maven-compiler-plugin)
    - [Maven GPG Plugin 1.6](https://github.com/apache/maven-gpg-plugin)
    - [Maven Jar Plugin 3.2.0](https://github.com/apache/maven-jar-plugin)
    - [Maven Javadoc Plugin 3.2.0](https://github.com/apache/maven-javadoc-plugin)
    - [Maven Release Plugin 2.5.3](https://github.com/apache/maven-release)
    - [Maven Source Plugin 3.2.1](https://github.com/apache/maven-source-plugin)
    - [SDKMAN!](https://github.com/sdkman/sdkman-cli)

- [Eclipse Public License 1.0](http://www.eclipse.org/legal/epl-v10.html)
    - [Nexus Staging Maven Plugin 1.6.8](https://github.com/sonatype/nexus-maven-plugins/tree/master/staging/maven-plugin)

- [Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-v20.html)
    - [JUnit Jupiter API 5.7.2](https://github.com/junit-team/junit5)
    - [JUnit Jupiter Console Standalone 1.7.2](https://github.com/junit-team/junit5)

- [GNU Lesser General Public License 3.0](http://www.gnu.org/licenses/lgpl-3.0.txt):
    - [License Maven Plugin 2.0.0](https://github.com/mojohaus/license-maven-plugin)

**[↑ Back to Index](#index)**
