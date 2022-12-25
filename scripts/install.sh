#!/bin/sh
./gradlew installDist
cp -r build/install/Orbit /opt
