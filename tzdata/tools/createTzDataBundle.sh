#!/bin/bash

# A script to generate TZ data updates.
#
# Usage: ./createTzDataBundle.sh <tzupdate.properties file> <output file>
# See libcore.tzdata.update.tools.CreateTzDataBundle for more information.

TOOLS_DIR=src/main/libcore/tzdata/update/tools
UPDATE_DIR=../update/src/main/libcore/tzdata/update
GEN_DIR=./gen

# Fail if anything below fails
set -e

rm -rf ${GEN_DIR}
mkdir -p ${GEN_DIR}

javac \
    ${TOOLS_DIR}/CreateTzDataBundle.java \
    ${TOOLS_DIR}/TzDataBundleBuilder.java \
    ${UPDATE_DIR}/ConfigBundle.java \
    ${UPDATE_DIR}/FileUtils.java \
    -d ${GEN_DIR}

java -cp ${GEN_DIR} libcore.tzdata.update.tools.CreateTzDataBundle $@
