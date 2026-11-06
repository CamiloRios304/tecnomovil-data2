@echo off
chcp 65001 > nul
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out co.edu.iudigital.tecnomovil.app.TecnoMovilDataApp %*
