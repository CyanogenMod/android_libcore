#!/bin/bash
#
# A script that generates an ICU data file containing just timezone rules data.
# The file can be used to provide time zone rules updates for compatible
# devices. Note: Only the rules are contained and new timezones will not have
# the translations.
#
# Usage:
# ./createIcuUpdateResources.sh <tzdata tar.gz file> <ICU version>
#
# e.g.
# ./createIcuUpdateResources.sh ~/Downloads/tzdata2015b.tar.gz 55
#
# After execution the file is generated.

if (( $# != 2 )); then
  echo "Missing arguments"
  echo "Usage:"
  echo "./createIcuUpdateResources.sh <tzdata tar.gz file> <ICU version>"
  exit 1
fi

if [[ -z "${ANDROID_BUILD_TOP}" ]]; then
  echo "Configure your environment with build/envsetup.sh and lunch"
  exit 1
fi

TZ_DATA_FILE=$1
ICU_VERSION=$2

if [[ ! -f ${TZ_DATA_FILE} ]]; then
  echo "${TZ_DATA_FILE} not found"
  exit 1
fi

# Keep track of the original working dir. Must be the "tools" dir.
START_DIR=`pwd`
ICU_DIR=${ANDROID_BUILD_TOP}/external/icu/icu4c/source
BUILD_DIR=${START_DIR}/icu_build

# Fail if anything below fails
set -e

rm -rf ${BUILD_DIR}
mkdir -p ${BUILD_DIR}
cd ${BUILD_DIR}

# Configure the build
${ICU_DIR}/runConfigureICU Linux
mkdir -p ${BUILD_DIR}/bin
cd ${BUILD_DIR}/tools/tzcode
ln -s ${ICU_DIR}/tools/tzcode/icuregions ./icuregions
ln -s ${ICU_DIR}/tools/tzcode/icuzones ./icuzones
cp ${TZ_DATA_FILE} .

# Make the tools
make

# Then make the whole thing
cd ${BUILD_DIR}
make -j32

# Generate the tzdata.lst file used to configure which files are included.
ICU_LIB_DIR=${BUILD_DIR}/lib
BIN_DIR=${BUILD_DIR}/bin
TZ_FILES=tzdata.lst

echo metaZones.res > ${TZ_FILES}
echo timezoneTypes.res >> ${TZ_FILES}
echo windowsZones.res >> ${TZ_FILES}
echo zoneinfo64.res >> ${TZ_FILES}

# Copy all the .res files we need here a from, e.g. ./data/out/build/icudt55l
RES_DIR=data/out/build/icudt${ICU_VERSION}l
cp ${RES_DIR}/metaZones.res ${BUILD_DIR}
cp ${RES_DIR}/timezoneTypes.res ${BUILD_DIR}
cp ${RES_DIR}/windowsZones.res ${BUILD_DIR}
cp ${RES_DIR}/zoneinfo64.res ${BUILD_DIR}

# This is the package name required for the .dat file to be accepted by ICU.
# This also affects the generated file name.
ICU_PACKAGE=icudt${ICU_VERSION}l

# Create the file
LD_LIBRARY_PATH=${ICU_LIB_DIR} ${BIN_DIR}/pkgdata -F -m common -v -T . -d . -p ${ICU_PACKAGE} ${TZ_FILES}
cp ${ICU_PACKAGE}.dat ${START_DIR}/icu_tzdata.dat

# Copy the file to the original working dir.
echo File can be found here: ${START_DIR}/icu_tzdata.dat
