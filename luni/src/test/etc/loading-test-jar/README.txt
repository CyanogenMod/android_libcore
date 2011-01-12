This directory contains the source code for the loading-test.jar file,
which is included as a resource in the luni tests. It is used for
testing the various class loaders.

The Android build system doesn't support dynamically producing
resources in any sane way. To update the resource, use the script
build.sh in this directory, and then copy the resulting jar file into
the luni test resources directory.
