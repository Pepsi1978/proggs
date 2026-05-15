@echo off
setlocal

if defined CODEX_GIT_LOCK_REAL_GIT if exist "%CODEX_GIT_LOCK_REAL_GIT%" set "REALGIT=%CODEX_GIT_LOCK_REAL_GIT%"
if not defined REALGIT if exist "C:\Program Files\Git\cmd\git.exe" set "REALGIT=C:\Program Files\Git\cmd\git.exe"
if not defined REALGIT if exist "C:\Program Files\Git\bin\git.exe" set "REALGIT=C:\Program Files\Git\bin\git.exe"
if not defined REALGIT set "REALGIT=git.exe"

if "%~1"=="--version" goto run_real
if /I "%~1"=="version" goto run_real
if "%~1"=="--help" goto run_real
if /I "%CODEX_GIT_LOCK_BYPASS%"=="1" goto run_real

call :find_subcommand %*
if /I "%GIT_SUBCOMMAND%"=="status" goto run_real
if /I "%GIT_SUBCOMMAND%"=="diff" goto run_real
if /I "%GIT_SUBCOMMAND%"=="log" goto run_real
if /I "%GIT_SUBCOMMAND%"=="show" goto run_real
if /I "%GIT_SUBCOMMAND%"=="rev-parse" goto run_real
if /I "%GIT_SUBCOMMAND%"=="ls-files" goto run_real
if /I "%GIT_SUBCOMMAND%"=="ls-tree" goto run_real
if /I "%GIT_SUBCOMMAND%"=="cat-file" goto run_real
if /I "%GIT_SUBCOMMAND%"=="blame" goto run_real
if /I "%GIT_SUBCOMMAND%"=="grep" goto run_real
if /I "%GIT_SUBCOMMAND%"=="shortlog" goto run_real
if /I "%GIT_SUBCOMMAND%"=="describe" goto run_real
if /I "%GIT_SUBCOMMAND%"=="merge-base" goto run_real

set "WRAPPER=%~dp0..\hooks\codex-git-wrapper.ps1"
if not exist "%WRAPPER%" set "WRAPPER=%USERPROFILE%\.codex\hooks\codex-git-wrapper.ps1"

pwsh -NoProfile -ExecutionPolicy Bypass -File "%WRAPPER%" %*
exit /b %ERRORLEVEL%

:run_real
"%REALGIT%" %*
exit /b %ERRORLEVEL%

:find_subcommand
set "GIT_SUBCOMMAND="
:find_loop
if "%~1"=="" goto :eof
if "%~1"=="-C" (
    shift
    shift
    goto find_loop
)
if /I "%~1"=="--git-dir" (
    shift
    shift
    goto find_loop
)
if /I "%~1"=="--work-tree" (
    shift
    shift
    goto find_loop
)
if /I "%~1"=="--namespace" (
    shift
    shift
    goto find_loop
)
if /I "%~1"=="-c" (
    shift
    shift
    goto find_loop
)
if /I "%~1"=="--config-env" (
    shift
    shift
    goto find_loop
)
set "ARG=%~1"
if "%ARG:~0,10%"=="--git-dir=" (
    shift
    goto find_loop
)
if "%ARG:~0,12%"=="--work-tree=" (
    shift
    goto find_loop
)
if "%ARG:~0,12%"=="--namespace=" (
    shift
    goto find_loop
)
if "%ARG:~0,1%"=="-" (
    shift
    goto find_loop
)
set "GIT_SUBCOMMAND=%~1"
goto :eof
