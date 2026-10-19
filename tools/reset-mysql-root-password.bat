@echo off
setlocal

:: ===========================================================================
:: Reset local MySQL 8 root password
:: ---------------------------------------------------------------------------
:: WHY THIS FILE IS PURE ASCII / ENGLISH:
::   cmd.exe parses .bat files byte-by-byte using the system ANSI codepage
::   (GBK on a Chinese Windows). If the file contains UTF-8 Chinese text,
::   some multi-byte sequences swallow the following CR, which MERGES two
::   lines together. A merged line loses its :: comment marker and gets
::   executed as a command, producing errors like:
::       'xxxx' is not recognized as an internal or external command
::   `chcp 65001` does NOT fix this, because the file is already being read
::   with the wrong codepage before that line takes effect.
::   Keeping this file ASCII-only makes it work under any codepage.
::   Chinese explanation lives in: tools/reset-mysql-root-password.md
::
:: WHAT IT DOES:
::   Temporarily starts mysqld with --init-file so it runs one ALTER USER
::   statement, then restarts the normal service. This is safer than the
::   often-suggested --skip-grant-tables, which leaves the server open to
::   anyone for a window of time.
::
:: DATA IS NOT AFFECTED. Only the credential row in mysql.user changes.
::
:: USAGE: right-click this file -> Run as administrator
:: ===========================================================================

net session >nul 2>&1
if errorlevel 1 (
    echo.
    echo [!] Administrator privileges required. Requesting elevation...
    powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)

set "MYSQLD=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe"
set "INITFILE=%TEMP%\mysql-root-reset.sql"

if not exist "%MYSQLD%" (
    echo.
    echo [ERROR] mysqld.exe not found at:
    echo         %MYSQLD%
    echo         Edit the MYSQLD variable in this script to match your install path.
    echo.
    pause
    exit /b 1
)

echo.
echo ============================================================
echo   Reset MySQL root@localhost password
echo ============================================================
echo.
echo Tip: use a simple password (letters + digits) for this reset,
echo      then change it to your usual strong one after logging in.
echo      Special characters may be mangled by shell escaping.
echo.
set /p "NEWPWD=Enter the NEW root password: "
if "%NEWPWD%"=="" (
    echo.
    echo [ERROR] Password cannot be empty.
    pause
    exit /b 1
)

> "%INITFILE%" echo ALTER USER 'root'@'localhost' IDENTIFIED BY '%NEWPWD%';

echo.
echo [1/4] Stopping service MySQL80 ...
net stop MySQL80
if errorlevel 1 (
    echo.
    echo [ERROR] Failed to stop the service.
    echo         Run "net stop MySQL80" manually to see the real error.
    pause
    exit /b 1
)

echo.
echo [2/4] Starting mysqld with --init-file (about 15 seconds) ...
start "" /B "%MYSQLD%" --init-file="%INITFILE%" --console
timeout /t 15 /nobreak >nul

echo.
echo [3/4] Stopping the temporary mysqld process ...
taskkill /F /IM mysqld.exe >nul 2>&1
timeout /t 3 /nobreak >nul

echo.
echo [4/4] Starting service MySQL80 again ...
net start MySQL80
if errorlevel 1 (
    echo.
    echo [ERROR] Failed to start the service!
    echo         Try "net start MySQL80" manually.
    echo         If it still fails, check the .err log under
    echo         C:\ProgramData\MySQL\MySQL Server 8.0\
    echo.
    pause
    exit /b 1
)

del "%INITFILE%" >nul 2>&1

echo.
echo ============================================================
echo   Done. Verify with:
echo     "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p
echo   If you can log in, the reset worked.
echo ============================================================
pause
