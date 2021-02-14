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

__import_with_maven() {
  if [ "$#" -ne 1 ]; then
    __echo yellow "DEV NOTE: incorrect usage!"
    return 1
  fi

  local artifact

  artifact=$1

  if ! mvn org.apache.maven.plugins:maven-dependency-plugin:3.1.2:get -DremoteRepositories=https://repo.maven.apache.org -Dartifact="$artifact" >/dev/null; then
    __echo red "Could not download library. Check your network connection."
    return 1
  fi

  return 0
}
