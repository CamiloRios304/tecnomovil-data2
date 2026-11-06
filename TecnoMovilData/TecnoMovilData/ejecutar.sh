#!/bin/bash
# Uso: ./ejecutar.sh [numeroDeUsuarios]   (por defecto 100000)
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out co.edu.iudigital.tecnomovil.app.TecnoMovilDataApp "$@"
