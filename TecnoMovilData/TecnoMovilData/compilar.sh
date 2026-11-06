#!/bin/bash
# Compila todo el proyecto (requiere JDK 17 o superior).
set -e
rm -rf out && mkdir out
javac -encoding UTF-8 -d out $(find src -name '*.java')
echo "Compilación exitosa -> carpeta out/"
