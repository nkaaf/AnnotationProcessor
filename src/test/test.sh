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
if [ -f "$HOME/.zshrc" ]; then
  . "$HOME"/.zshrc
fi

if [ -f "$HOME/.bashrc" ]; then
  . "$HOME"/.zshrc
fi

if ! command -v sdk >/dev/null; then
  __echo red "SDKMAN! is not installed. Please install it before using this script."
  exit 1
fi

if ! command -v mvn >/dev/null; then
  __echo red "Maven is not installed. Please install if before using this script."
  exit 1
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
  if ! __import_with_maven org.junit.platform:junit-platform-console-standalone:jar:1.7.1; then
    exit 1
  fi
fi
if [ ! -f "$_junit_api_jar" ]; then
  if ! __import_with_maven org.junit.jupiter:junit-jupiter-api:jar:5.7.1; then
    exit 1
  fi
fi
if [ ! -f "$_junit_platform_jar" ]; then
  if ! __import_with_maven org.junit.platform:junit-platform-commons:jar:1.7.1; then
    exit 1
  fi
fi
if [ ! -f "$_opentest_jar" ]; then
  if ! __import_with_maven org.opentest4j:opentest4j:jar:1.2.0; then
    exit 1
  fi
fi
if [ ! -f "$_apiguardian_jar" ]; then
  if ! __import_with_maven org.apiguardian:apiguardian-api:jar:1.1.0; then
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
  "9" | "10" | "11" | "12" | "13" | "14" | "15")
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
_java_8_sdk="8.0.282-zulu"
if ! sdk use java $_java_8_sdk >/dev/null; then
  __debug $DEBUG "Required Java 8 SDK does not exists. It will be downloaded..."
  if ! sdk install java $_java_8_sdk >/dev/null; then
    __echo red "Could not download Java sdk. Check your network connection."
    exit 1
  fi
  sdk use java $_java_8_sdk >/dev/null
fi
_test "8"

__debug $DEBUG "--- Currently (02.2021) there is no Java 9 available at SDKMAN!"

__debug $DEBUG "--- Currently (02.2021) there is no Java 10 available at SDKMAN!"

__debug $DEBUG "Testing with Java 11..."
_java_11_sdk="11.0.10-zulu"
if ! sdk use java $_java_11_sdk >/dev/null; then
  __debug $DEBUG "Required Java 11 SDK does not exists. It will be downloaded..."
  if ! sdk install java $_java_11_sdk >/dev/null; then
    __echo red "Could not download Java sdk. Check your network connection."
    exit 1
  fi
  sdk use java $_java_11_sdk
fi
_test "11"

__debug $DEBUG "Testing with Java 12..."
_java_12_sdk="12.0.2-sapmchn"
if ! sdk use java $_java_12_sdk >/dev/null; then
  __debug $DEBUG "Required Java 12 SDK does not exists. It will be downloaded..."
  if ! sdk install java $_java_12_sdk >/dev/null; then
    __echo red "Could not download Java 12 SDK. Check your network connection."
    exit 1
  fi
  sdk use java $_java_12_sdk
fi
_test "12"

__debug $DEBUG "Testing with Java 13..."
_java_13_sdk="13.0.2-sapmchn"
if ! sdk use java $_java_13_sdk >/dev/null; then
  __debug $DEBUG "Required Java 13 SDK does not exists. It will be downloaded..."
  if ! sdk install java $_java_13_sdk >/dev/null; then
    __echo red "Could not download Java sdk. Check your network connection."
    exit 1
  fi
  sdk use java $_java_13_sdk
fi
_test "13"

__debug $DEBUG "Testing with Java 14..."
_java_14_sdk="14.0.2-sapmchn"
if ! sdk use java $_java_14_sdk >/dev/null; then
  __debug $DEBUG "Required Java 14 SDK does not exists. It will be downloaded..."
  if ! sdk install java $_java_14_sdk >/dev/null; then
    __echo red "Could not download Java sdk. Check your network connection."
    exit 1
  fi
  sdk use java $_java_14_sdk
fi
_test "14"

__debug $DEBUG "Testing with Java 15..."
_java_15_sdk="15.0.2-zulu"
if ! sdk use java $_java_15_sdk >/dev/null; then
  __debug $DEBUG "Required Java 15 SDK does not exists. It will be downloaded..."
  if ! sdk install java $_java_15_sdk >/dev/null; then
    __echo red "Could not download Java sdk. Check your network connection."
    exit 1
  fi
  sdk use java $_java_15_sdk
fi
_test "15"
