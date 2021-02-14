# Annotation Processor

![License](https://img.shields.io/github/license/nkaaf/AnnotationProcessor)

![Libraries.io SourceRank](https://img.shields.io/librariesio/sourcerank/maven/io.github.nkaaf:annotationprocessor)
![GitHub all releases](https://img.shields.io/github/downloads/nkaaf/AnnotationProcessor/total)

![Dependent repos (via libraries.io)](https://img.shields.io/librariesio/dependent-repos/maven/io.github.nkaaf:annotationprocessor)
![Dependents (via libraries.io)](https://img.shields.io/librariesio/dependents/nkaaf/io.github.nkaaf:annotationprocessor)
![Libraries.io dependency status for GitHub repo](https://img.shields.io/librariesio/github/nkaaf/AnnotationProcessor)

![GitHub release (latest by date)](https://img.shields.io/github/v/release/nkaaf/AnnotationProcessor)
![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/nkaaf/AnnotationProcessor)

![Maven Central](https://img.shields.io/maven-central/v/io.github.nkaaf/annotationprocessor)

![GitHub forks](https://img.shields.io/github/forks/nkaaf/AnnotationProcessor?style=social)
![GitHub Repo stars](https://img.shields.io/github/stars/nkaaf/AnnotationProcessor?style=social)
![GitHub watchers](https://img.shields.io/github/watchers/nkaaf/AnnotationProcessor?style=social)

## Index

* [Use Case](#use-case)
* Usage
    * [Build System](#build-system)
        * [Maven](#usage-maven)
        * [Gradle](#usage-gradle)
    * [Java](#java)
        * [Non-Modular](#non-modular)
        * [Modular (Java 9+)](#modular-java-9)
            * [Maven](#java-maven)
            * [Gradle](#java-gradle)
* [Developing](#developing)
    * [The Problem with Multi-Release JARs and IDEs](#the-problem-with-multi-release-jars-and-ides)
    * [Testing](#testing)
        * [Needed Components](#needed-components)
        * [Used SDKMAN! JDKs](#used-sdkman-jdks)
        * [Used Java Libraries](#used-java-libraries)
* [License](#license)
    * [Licenses of used Libraries](#licenses-of-used-libraries-and-tools)

## Use Case

In case you want to create a new annotation processor you can use the Annotation <strong>@AnnotationProcessor</strong>
to automatically create the required <strong>javax.annotation.processing.Processor</strong> file in
<strong>META-INF/services/</strong> directory at compile time.
> ❗ To use this correctly look up how to import it to your [Build System](#build-system) and integrate into [Java](#java) ❗

## Build System

### <a name="usage-maven"></a> Maven

```xml
<dependency>
    <groupId>io.github.nkaaf</groupId>
    <artifactId>annotationprocessor</artifactId>
    <version>1.0</version>
</dependency>
```

**[⬆ Back to Index](#index)**

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

**[⬆ Back to Index](#index)**

## Java

Annotate your AnnotationProcessor with <strong>@AnnotationProcessor</strong>. The Processor behind this annotation
checks if your Annotation is built compliant ([JSP 269](https://www.jcp.org/en/jsr/detail?id=269)). You can either
extending the <strong>javax.annotation.processing.AbstractProcessor</strong> or directly implement the
<strong>javax.annotation.processing.Processor</strong>.

### Non-Modular

You only have to import the dependency with your build system.
> [Maven Example](examples/maven/non-modular/pom.xml)
>
> [Gradle Example](examples/gradle/non-modular/build.gradle)

**[⬆ Back to Index](#index)**

### Modular (Java 9+)

You have to add the dependencies' module to the <strong>module-info.java</strong> of the desired module. In addition,
you have to set it up in your Build System ([Maven](#java-maven)/ [Gradle](#java-gradle)).

#### <a name="java-maven"></a> Maven

Your module only has to require the io.github.nkaaf.annotationprocessor module. It will get the java.compiler module
automatically, so you don't need to require this as well for your annotation processor.

```java
module yourModule {
    requires static io.github.nkaaf.annotationprocessor;
}
```

You have to add the dependency as Annotation Path in maven-compiler-plugin.

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

**[⬆ Back to Index](#index)**

#### <a name="java-gradle"></a> Gradle (6.4+)

Your module has to require the io.github.nkaaf.annotationprocessor module.

> ❗ It will NOT get the java.compiler module automatically (other behaviour than in Maven) ❗

```java
module yourModule {
    requires static io.github.nkaaf.annotationprocessor;
}
```

Also have to turn on module path inference and add the release flag.

> ❗ You cannot use the JDK 9 because the release flag causes an error with Gradle. JDK 10 causes another error with
> Gradle, but the Java 10 release flag can be used with JDK 12. See Bug Tickets linked in Example.
> But you can use the release flag 11, and a JDK 11 ❗ <br/>

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

**[⬆ Back to Index](#index)**

## Developing

To provide maximum compatibility, this project has to be compiled with Java 9.

### The Problem with Multi-Release JARs and IDEs

Most of the IDEs do not support multi-release jars properly. The problem is that the package and filenames, of both
(Java and Java9), are identical. The IDEs cannot compile them, even if this mechanism is clearly defined in the Maven
POM.

**[⬆ Back to Index](#index)**

### Testing

There is also a problem with the tests. An automatic way to change the JDK, so both versions of the annotation processor
will be tested, did not exist. My solution is a testing bash script that will compile and test the classes. It depends
on [SDKMAN!](https://sdkman.io/) and the [Junit Jupiter Engine](https://junit.org/junit5/), which is imported by
[Maven](https://maven.apache.org/). You can easily run the [test script](src/test/test.sh), with bash, from everywhere
on your computer. If you have not the required Java Libraries installed in the maven folder, the script will download
them. This also applied to the Java JDKs with SDKMAN!. The mechanism of changing the JDK with SDKMAN! is not perfect,
because it depends on hardcoded java versions. These can be deleted everytime in the lists of SDKMAN! without me
noticing it.

**[⬆ Back to Index](#index)**

#### Needed Components

* [Bash](https://www.gnu.org/software/bash/)
* [SDKMAN!](https://sdkman.io/)
* [Maven](https://maven.apache.org/)

**[⬆ Back to Index](#index)**

#### Used SDKMAN! JDKs

* 8.0.282-zulu
* 11.0.10-zulu
* 12.0.2-sapmchn
* 13.0.2-sapmchn
* 14.0.2-sapmchn
* 15.0.2-zulu

**[⬆ Back to Index](#index)**

#### Used Java Libraries

* [JUnit Jupiter API 5.7.1](https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api/5.7.1)
* [JUnit Platform Console Standalone 1.7.1](https://mvnrepository.com/artifact/org.junit.platform/junit-platform-console-standalone/1.7.1) (Used in testing script for command line support)

**[⬆ Back to Index](#index)**

## License

This Project is licensed under the GNU Lesser General Public License 2.1 or any later ([LGPL 2.1](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt)).

#### Licenses of used Libraries and Tools

This list includes only Libraries and tools that are explicit imported/used in this project.

* [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt):
    * [Maven](https://github.com/apache/maven)
    * [Maven Compiler Plugin](https://github.com/apache/maven-compiler-plugin)
    * [Maven GPG Plugin](https://github.com/apache/maven-gpg-plugin)
    * [Maven Jar Plugin](https://github.com/apache/maven-jar-plugin)
    * [Maven Javadoc Plugin](https://github.com/apache/maven-javadoc-plugin)
    * [Maven Source Plugin](https://github.com/apache/maven-source-plugin)
    * [SDKMAN!](https://github.com/sdkman/sdkman-cli)

* [Eclipse Public License 1.0](http://www.eclipse.org/legal/epl-v10.html)
    * [Nexus Staging Maven Plugin](https://github.com/sonatype/nexus-maven-plugins/tree/master/staging/maven-plugin)

* [Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-v20.html)
    * [JUnit Jupiter API](https://github.com/junit-team/junit5)
    * [JUnit Jupiter Console Standalone](https://github.com/junit-team/junit5)

* [GNU Lesser General Public License 3.0](http://www.gnu.org/licenses/lgpl-3.0.txt):
    * [License Maven Plugin](https://github.com/mojohaus/license-maven-plugin)

**[⬆ Back to Index](#index)**
