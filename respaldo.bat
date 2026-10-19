@echo off
rem Respaldo de la base de datos de SEIPO.
rem Lee los datos de conexion de config.properties y guarda respaldos\<base>_AAAA-MM-DD_HHMM.sql
rem Al terminar espera una tecla para que se lea el mensaje; el Programador de tareas lo ejecuta con /sinpausa
setlocal EnableExtensions
cd /d "%~dp0"
set "PAUSA=1"
if /i "%~1"=="/sinpausa" set "PAUSA="

if not exist "config.properties" (
    echo ERROR: no se encontro config.properties en %CD%
    echo Copie config.ejemplo.properties como config.properties y ajuste los datos de conexion.
    goto :error
)

set "db.host=localhost"
set "db.puerto=3306"
set "db.base=seipo_db"
set "db.usuario=root"
set "db.clave="
set "respaldo.dump="
for /f "usebackq eol=# tokens=1,* delims==" %%a in ("config.properties") do set "%%a=%%b"

rem Herramienta de respaldo: la de config.properties, MariaDB 12.3, XAMPP o la del PATH
set "DUMP="
if defined respaldo.dump (
    if not exist "%respaldo.dump%" (
        echo ERROR: no existe la herramienta indicada en respaldo.dump: %respaldo.dump%
        goto :error
    )
    set "DUMP=%respaldo.dump%"
    goto :respaldar
)
for %%f in (
    "C:\Program Files\MariaDB 12.3\bin\mariadb-dump.exe"
    "C:\Program Files\MariaDB 12.3\bin\mysqldump.exe"
    "C:\xampp\mysql\bin\mariadb-dump.exe"
    "C:\xampp\mysql\bin\mysqldump.exe"
) do if not defined DUMP if exist "%%~f" set "DUMP=%%~f"
if not defined DUMP for %%n in (mariadb-dump.exe mysqldump.exe) do if not defined DUMP (
    for /f "delims=" %%p in ('where %%n 2^>nul') do if not defined DUMP set "DUMP=%%p"
)
if not defined DUMP (
    echo ERROR: no se encontro mariadb-dump.exe ni mysqldump.exe.
    echo Indique su ruta en config.properties con la clave respaldo.dump
    goto :error
)

:respaldar
for /f %%d in ('powershell -NoProfile -Command "Get-Date -Format yyyy-MM-dd_HHmm"') do set "FECHA=%%d"
if not exist "respaldos" mkdir "respaldos"
set "ARCHIVO=respaldos\%db.base%_%FECHA%.sql"

rem La clave va por variable de entorno para que no aparezca en la linea de comandos
set "MYSQL_PWD=%db.clave%"
echo Respaldando %db.base% de %db.host%:%db.puerto% con "%DUMP%"...
"%DUMP%" --host=%db.host% --port=%db.puerto% --user=%db.usuario% --databases %db.base% --add-drop-database --single-transaction --routines --triggers --default-character-set=utf8mb4 --result-file="%ARCHIVO%"
set "RESULTADO=%ERRORLEVEL%"
set "MYSQL_PWD="

if not "%RESULTADO%"=="0" (
    echo ERROR: el respaldo fallo con codigo %RESULTADO%. Revise que el servicio de MariaDB este iniciado y los datos de config.properties.
    if exist "%ARCHIVO%" del "%ARCHIVO%"
    goto :error
)
echo Respaldo guardado en %CD%\%ARCHIVO%
if defined PAUSA pause
exit /b 0

rem Sale con codigo 1 para que el Programador de tareas registre la falla
:error
if defined PAUSA pause
exit /b 1
