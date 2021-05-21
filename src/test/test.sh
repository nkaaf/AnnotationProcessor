#!/usr/bin/env bash

###
# #%L
# AnnotationProcessor
# %%
# Copyright (C) 2021 Niklas Kaaf
# %%
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as
# published by the Free Software Foundation, either version 2.1 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Lesser Public License for more details.
#
# You should have received a copy of the GNU General Lesser Public
# License along with this program.  If not, see
# <http://www.gnu.org/licenses/lgpl-2.1.html>.
# #L%
###

_test_dir=$(dirname "${BASH_SOURCE[0]}")

_pwd=$(pwd)
if [ "$_test_dir" == "." ]; then
  _test_dir=$_pwd
else
  _test_dir=$(
    cd "$_test_dir"
    pwd
  )
fi

. "$_test_dir/utils.sh"

_usage="Usage: ./$(basename "$0") [OPTIONS]
Test Script for AnnotationProcessor-Project
[OPTIONS]:

  -h, --help, -?    display this help text
  -d, --debug, -X   showing debug messages during tests
"

DEBUG=false

while :; do
  case $1 in
  -h | --help | -\?)
    echo "$_usage"
    exit
    ;;
  -d | --debug | -X)
    __echo yellow "Debug mode is active"
    DEBUG=true
    break
    ;;
  -?*)
    __echo yellow "Invalid option: $1"
    echo "$_usage"
    exit
    ;;
  *)
    if [ "$1" != "" ]; then
      __echo yellow "WARNING: Not parsing $1"
    fi
    break
    ;;
  esac
done

_project_dir="$(dirname "$(dirname "$_test_dir")")"
_target_dir="$_project_dir/target"
_sources_dir="$_project_dir/src"
_out_dir="$_target_dir/testing/out"
_package_name_dir="io/github/nkaaf/annotationprocessor"

__debug $DEBUG "Generating output directory..."
mkdir -p "$_out_dir"
__debug $DEBUG "Output directory generated"

__debug $DEBUG "Check if required commands are available..."
if [ -f "$HOME/.bashrc" ]; then
  . "$HOME/.bashrc"
fi

if ! command -v sdk >/dev/null; then
  if [ -f "$HOME/.zshrc" ]; then
    . "$HOME/.zshrc"
  fi
fi

if ! command -v sdk >/dev/null; then
  __echo red "SDKMAN! is not installed. Would you like to install it now? [Y/n]."
  read -r answer
  if [ "$answer" != "Y" ] && [ "$answer" != "y" ]; then
    __echo red "You can't use this script without SDKMAN! installed!"
    exit 1
  fi

  if ! __install_sdkman; then
    exit 1
  fi

  . "$HOME/.sdkman/bin/sdkman-init.sh"
fi

__sdkman_switch_auto_answer_mode true

if ! command -v mvn >/dev/null; then
  __echo red "Maven is not installed. Would you like to install it with SDKMAN! now? [Y/n]."
  read -r answer
  if [ "$answer" != "Y" ] && [ "$answer" != "y" ]; then
    __echo red "You can't use this script without Maven installed!"
    exit 1
  fi

  if ! __install_maven; then
    exit 1
  fi
fi
__debug $DEBUG "All required commands are installed"

_junit_jar="$HOME/.m2/repository/org/junit/platform/junit-platform-console-standalone/1.7.1/junit-platform-console-standalone-1.7.1.jar"

# These are needed because the JUnit Console Standalone does not include the module-info's for its dependencies
_junit_api_jar="$HOME/.m2/repository/org/junit/jupiter/junit-jupiter-api/5.7.1/junit-jupiter-api-5.7.1.jar"
_junit_platform_jar="$HOME/.m2/repository/org/junit/platform/junit-platform-commons/1.7.1/junit-platform-commons-1.7.1.jar"
_opentest_jar="$HOME/.m2/repository/org/opentest4j/opentest4j/1.2.0/opentest4j-1.2.0.jar"
_apiguardian_jar="$HOME/.m2/repository/org/apiguardian/apiguardian-api/1.1.0/apiguardian-api-1.1.0.jar"

__debug $DEBUG "Check if required libraries are imported, and if not, import them..."
if [ ! -f "$_junit_jar" ]; then
  if ! __import_with_maven org.junit.platform:junit-platform-console-standalone:1.7.1:jar; then
    exit 1
  fi
fi
if [ ! -f "$_junit_api_jar" ]; then
  if ! __import_with_maven org.junit.jupiter:junit-jupiter-api:5.7.1:jar; then
    exit 1
  fi
fi
if [ ! -f "$_junit_platform_jar" ]; then
  if ! __import_with_maven org.junit.platform:junit-platform-commons:1.7.1:jar; then
    exit 1
  fi
fi
if [ ! -f "$_opentest_jar" ]; then
  if ! __import_with_maven org.opentest4j:opentest4j:1.2.0:jar; then
    exit 1
  fi
fi
if [ ! -f "$_apiguardian_jar" ]; then
  if ! __import_with_maven org.apiguardian:apiguardian-api:1.1.0:jar; then
    exit 1
  fi
fi
__debug $DEBUG "All libraries are (now) imported"

_compiler_options="-classpath $_junit_jar -encoding UTF-8 -proc:none"

_annotation_processor_file="$_sources_dir/main/java/$_package_name_dir/annotation/AnnotationProcessor.java"
_annotation_processor_test_file_6="$_test_dir/java/$_package_name_dir/AnnotationProcessorTest.java"
_annotation_processor_test_file_9="$_test_dir/java9/$_package_name_dir/AnnotationProcessorTest.java"
_annotation_processor_processor_file_6="$_sources_dir/main/java/$_package_name_dir/processor/AnnotationProcessorProcessor.java"
_annotation_processor_processor_file_9="$_sources_dir/main/java9/$_package_name_dir/processor/AnnotationProcessorProcessor.java"

_test() {
  if [ "$#" -ne 1 ]; then
    __echo yellow "DEV NOTE: incorrect usage!"
    return 1
  fi

  local java_version
  local java_options
  local compile_modules
  local module_path
  local module_options
  local main_dir
  local classpath_with_test

  java_version=$1
  compile_modules=false
  module_options=

  rm -rf "${_out_dir:?}/"*

  case $java_version in
  "6" | "7" | "8")
    java_options="-source 1.$java_version -target 1.$java_version"
    ;;
  "9" | "10" | "11" | "12" | "13" | "14" | "15" | "16")
    java_options="--release $java_version"
    compile_modules=true
    ;;
  *)
    __echo yellow "DEV NOTE: incorrect Java Version $java_version!"
    return 1
    ;;
  esac

  if [ $compile_modules == true ]; then
    module_path="$_junit_api_jar:$_junit_platform_jar:$_apiguardian_jar:$_opentest_jar"

    if ! eval "javac -d $_out_dir/main $_compiler_options $java_options $_sources_dir/main/java9/module-info.java $_annotation_processor_file $_annotation_processor_processor_file_9"; then
      __echo red "Java $java_version Test failed"
      return 0
    fi

    module_path="$module_path:$_out_dir/main"

    if ! eval "javac -d $_out_dir/test $_compiler_options $java_options --module-path $module_path $_sources_dir/test/java9/module-info.java"; then
      __echo red "Java $java_version Test failed"
      return 0
    fi

    module_path="$module_path:$_out_dir/test"

    if ! eval "javac -d $_out_dir/test $_compiler_options $java_options --module-path $module_path $_annotation_processor_test_file_9"; then
      __echo red "Java $java_version Test failed"
      return 0
    fi

    module_options="-Dmodule.path=$module_path"
    classpath_with_test="$_out_dir/test"
    main_dir="$_out_dir/main"
  else
    if ! eval "javac -d $_out_dir $_compiler_options $java_options $_annotation_processor_file $_annotation_processor_processor_file_6 $_annotation_processor_test_file_6"; then
      __echo red "Java $java_version Test failed"
      return 0
    fi

    classpath_with_test=$_out_dir
    main_dir=$_out_dir
  fi

  if eval "java $module_options -Dsrc.dir=$_sources_dir -Dout.dir=$main_dir -jar $_junit_jar --classpath $classpath_with_test --scan-classpath --disable-banner --details=none"; then
    __echo green "Java $java_version Test successful"
  else
    __echo red "Java $java_version Test failed"
  fi
  return 0
}

__debug $DEBUG "--- Java 6 cannot be tested, because the junit library requires at least Java 8"

__debug $DEBUG "--- Java 7 cannot be tested, because the junit library requires at least Java 8"

__debug $DEBUG "Testing with Java 8..."
_jdk_8_version="8.0.282-zulu"
if ! sdk use java $_jdk_8_version >/dev/null; then
  __debug $DEBUG "Required Java 8 JDK does not exists. It will be downloaded..."
  if ! __install_jdk $_jdk_8_version; then
    exit 1
  fi
  sdk use java $_jdk_8_version >/dev/null
fi
_test "8"

__debug $DEBUG "--- Currently (02.2021) there is no Java 9 available at SDKMAN!"

__debug $DEBUG "--- Currently (02.2021) there is no Java 10 available at SDKMAN!"

__debug $DEBUG "Testing with Java 11..."
_jdk_11_version="11.0.10-zulu"
if ! sdk use java $_jdk_11_version >/dev/null; then
  __debug $DEBUG "Required Java 11 JDK does not exists. It will be downloaded..."
  if ! __install_jdk $_jdk_11_version; then
    exit 1
  fi
  sdk use java $_jdk_11_version >/dev/null
fi
_test "11"

__debug $DEBUG "--- Currently (05.2021) there is no Java 12 available at SDKMAN!"

__debug $DEBUG "--- Currently (05.2021) there is no Java 13 available at SDKMAN!"

__debug $DEBUG "--- Currently (05.2021) there is no Java 14 available at SDKMAN!"

__debug $DEBUG "Testing with Java 15..."
_jdk_15_version="15.0.2-sapmchn"
if ! sdk use java $_jdk_15_version >/dev/null; then
  __debug $DEBUG "Required Java 15 JDK does not exists. It will be downloaded..."
  if ! __install_jdk $_jdk_15_version; then
    exit 1
  fi
  sdk use java $_jdk_15_version >/dev/null
fi
_test "15"

__debug $DEBUG "Testing with Java 16..."
_jdk_16_version="16.0.1-zulu"
if ! sdk use java $_jdk_16_version >/dev/null; then
  __debug $DEBUG "Required Java 16 JDK does not exists. It will be downloaded..."
  if ! __install_jdk $_jdk_16_version; then
    exit 1
  fi
  sdk use java $_jdk_16_version >/dev/null
fi
_test "16"

__sdkman_switch_auto_answer_mode false
