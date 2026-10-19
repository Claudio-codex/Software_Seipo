@echo off
rem Compila el sistema y las pruebas JUnit 5 y las ejecuta contra la base seipo_test.
rem Requiere el servicio de MariaDB iniciado y permisos del usuario seipo sobre seipo_test (ver README).
setlocal EnableDelayedExpansion
cd /d "%~dp0"

set "OUT=out-pruebas"
set "JUNIT=SEIPO\lib-test\junit-platform-console-standalone-1.12.2.jar"
set "CP="
for %%j in (SEIPO\lib\*.jar) do set "CP=!CP!;%%j"

if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%"
rem Lista de fuentes con / para que javac acepte rutas con espacios
(for /r SEIPO\src %%f in (*.java) do (set "f=%%f" & echo "!f:\=/!")) > "%OUT%\fuentes.txt"
(for /r SEIPO\test %%f in (*.java) do (set "f=%%f" & echo "!f:\=/!")) >> "%OUT%\fuentes.txt"

echo Compilando...
javac -encoding UTF-8 -nowarn -d "%OUT%\clases" -cp "%JUNIT%!CP!" @"%OUT%\fuentes.txt"
if errorlevel 1 (
    echo ERROR: no se pudo compilar.
    exit /b 1
)

echo Ejecutando las pruebas contra seipo_test...
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar "%JUNIT%" execute --class-path "%OUT%\clases!CP!" ^
    --scan-class-path --details=tree --disable-banner --reports-dir "%OUT%\reportes" %*
exit /b %ERRORLEVEL%
