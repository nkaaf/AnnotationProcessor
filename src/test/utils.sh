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

_default="\e[0m"
_dim="\e[2m"
_red="\e[31m"
_green="\e[32m"
_yellow="\e[33m"

__echo() {
  if [ "$#" -ne 2 ]; then
    echo -e "${_yellow}DEV NOTE: incorrect usage!$_default"
    return 1
  fi

  local color

  case $1 in
  red)
    color=$_red
    ;;
  green)
    color=$_green
    ;;
  yellow)
    color=$_yellow
    ;;
  esac

  echo -e "$color$2$_default"
  return 0
}

__debug() {
  if [ "$#" -ne 2 ]; then
    __echo yellow "DEV NOTE: incorrect usage!"
    return 1
  fi

  if [ "$1" = true ]; then
    echo -e "$_dim$2$_default"
  fi
  return 0
}

__install_sdkman() {
  install_script=$(curl -s "https://get.sdkman.io")

  if [ -z "$install_script" ]; then
    __echo red "The SDKMAN! installation script cannot be downloaded. Check your network connection."
    return 1
  fi

  bash -c "$install_script" >/dev/null
  return 0
}

__install_jdk() {
  if [ "$#" -ne 1 ]; then
    __echo yellow "DEV NOT: incorrect usage!"
    return 1
  fi

  local jdk_version

  jdk_version=$1

  if ! sdk install java "$jdk_version"; then
    __echo red "The JDK $jdk_version cannot be downloaded. Check your network connection."
    return 1
  fi
  return 0
}

__install_maven() {
  local jdk_version

  jdk_version="8.0.282-zulu"

  if ! sdk use java $jdk_version >/dev/null; then
    __echo yellow "Required Java JDK does not exists. It will be downloaded..."
    if ! __install_jdk $jdk_version; then
      return 1
    fi
    sdk use java $jdk_version >/dev/null
  fi

  if ! sdk install maven 3.6.3; then
    __echo red "Maven cannot be downloaded. Check your network connection."
    return 1
  fi
  return 0
}

__import_with_maven() {
  if [ "$#" -ne 1 ]; then
    __echo yellow "DEV NOTE: incorrect usage!"
    return 1
  fi

  local artifact

  artifact=$1

  if ! mvn org.apache.maven.plugins:maven-dependency-plugin:3.1.2:get -DremoteRepositories=https://repo.maven.apache.org -Dartifact="$artifact" >/dev/null; then
    __echo red "Could not download artifact $artifact. Check your network connection."
    return 1
  fi

  return 0
}

__sdkman_switch_auto_answer_mode() {
  if [ "$#" -ne 1 ]; then
    __echo yellow "DEV NOTE: incorrect usage!"
    return 1
  fi

  local enable

  enable=$1

  sed -i "s/sdkman_auto_answer=.*/sdkman_auto_answer=$enable/" "$HOME"/.sdkman/etc/config
  return 0
}
