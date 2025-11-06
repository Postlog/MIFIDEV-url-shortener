@echo off
REM Скрипт для запуска URL Shortener на Windows

echo === URL Shortener ===
echo Сборка проекта...

REM Проверка наличия Maven
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Maven не найден. Установите Maven для продолжения.
    exit /b 1
)

REM Сборка проекта
call mvn clean package -q

REM Проверка успешности сборки
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Ошибка сборки проекта
    exit /b 1
)

echo ✅ Сборка завершена успешно
echo Запуск приложения...
echo.

REM Запуск приложения
java -jar target\url-shortener.jar

