@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

echo ==^> Building standalone Impostor debug APK (no Firebase or secrets needed)...
call gradlew.bat assembleDebug --no-daemon
if errorlevel 1 (
    echo ERROR: Gradle build failed. Make sure JDK 17+ and Android SDK are installed.
    exit /b 1
)

set "APK_PATH=%CD%\app\build\outputs\apk\debug\app-debug.apk"
if not exist "%APK_PATH%" (
    echo ERROR: APK not found at "%APK_PATH%"
    exit /b 1
)

echo ==^> Build succeeded: %APK_PATH%

where adb >nul 2>nul
if %errorlevel%==0 (
    for /f "skip=1 tokens=1,2" %%A in ('adb devices') do (
        if "%%B"=="device" (
            echo ==^> Installing onto connected Android device %%A...
            adb -s %%A install -r "%APK_PATH%"
            if not errorlevel 1 (
                adb -s %%A shell monkey -p com.aistudio.impostorgame.qvnxkp -c android.intent.category.LAUNCHER 1 >nul 2>nul
                echo ==^> Installed and launched!
                exit /b 0
            )
        )
    )
)

echo.
echo No USB/wireless ADB device detected.
echo Copy this APK to your Android phone and tap it to install:
echo   %APK_PATH%
