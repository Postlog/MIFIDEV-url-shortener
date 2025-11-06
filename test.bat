@echo off
REM Скрипт для запуска тестов с отчётом о покрытии на Windows

echo === URL Shortener - Test Runner ===
echo.

REM Проверка наличия Maven
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Maven не найден. Установите Maven для продолжения.
    exit /b 1
)

echo 🧪 Запуск тестов...
echo.

REM Запуск тестов с покрытием
call mvn clean test jacoco:report

REM Проверка результата
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ Все тесты пройдены успешно!
    echo.
    echo 📊 Отчёт о покрытии создан: target\site\jacoco\index.html
    echo.
    echo Открываю отчёт о покрытии в браузере...
    start target\site\jacoco\index.html
) else (
    echo.
    echo ❌ Некоторые тесты провалились
    exit /b 1
)

