#!/bin/sh
./gradlew installDist
cp build/install/Orbit/lib/*.jar /usr/local/lib
cp build/install/Orbit/bin/Orbit /usr/local/bin/orb