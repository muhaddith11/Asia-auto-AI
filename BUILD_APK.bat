@echo off
chcp 65001 >nul
title Smart Car Control - APK Builder

echo.
echo ========================================
echo   SMART CAR CONTROL - APK YARATISH
echo ========================================
echo.

set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot
set ANDROID_SDK_ROOT=C:\android-sdk
set PATH=%JAVA_HOME%\bin;%PATH%

echo Java tekshirilmoqda...
java -version >nul 2>&1
if errorlevel 1 (
    echo XATO: Java topilmadi!
    pause
    exit /b 1
)
echo Java: OK

echo.
echo APK yigʻilmoqda... (3-5 daqiqa)
echo Iltimos kuting...
echo.

cd /d "%~dp0"
call gradlew.bat assembleDebug

if errorlevel 1 (
    echo.
    echo ========================================
    echo   XATO: APK yaratishda muammo!
    echo   Yuqoridagi xato xabarini koʻring
    echo ========================================
    pause
    exit /b 1
)

echo.
echo ========================================
echo   APK TAYYOR!
echo ========================================
echo.
echo APK fayl joylashgan:
echo %~dp0app\build\outputs\apk\debug\app-debug.apk
echo.

set APK_PATH=%~dp0app\build\outputs\apk\debug\app-debug.apk
if exist "%APK_PATH%" (
    echo APK hajmi:
    for %%A in ("%APK_PATH%") do echo %%~zA bayt
    echo.
    echo APK faylni koʻrsatish uchun Enter bosing...
    pause
    explorer /select,"%APK_PATH%"
) else (
    echo APK fayl topilmadi, qayta urinib koʻring
    pause
)
