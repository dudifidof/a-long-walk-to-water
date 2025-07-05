@echo off
rem Simple helper to build the mod and launch the client on Windows
cd /d %~dp0
call gradlew.bat clean build
call gradlew.bat runclient > run\client.log 2>&1
