#!/usr/bin/env bash
#
# Java 25 PREVIEW features.
#
# Preview features need --enable-preview at both compile and run time, which is
# why these live outside the Maven build - the main project stays preview-free.
# The single-file source launcher handles both flags at once.
#
set -u
cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

for file in StableValues.java StructuredConcurrency.java; do
    echo
    echo "################################################################"
    echo "# $file"
    echo "################################################################"
    java --enable-preview --source 25 "$file"
done

echo
echo "Both of these are PREVIEW APIs in Java 25 - they can change or disappear."
echo "Never enable --enable-preview in production: a class file compiled with it"
echo "refuses to load on any other JDK version, even a newer one."
