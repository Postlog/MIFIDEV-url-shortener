#!/bin/bash
# Скрипт для запуска тестов с отчётом о покрытии

echo "=== URL Shortener - Test Runner ==="
echo ""

# Проверка наличия Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven не найден. Установите Maven для продолжения."
    exit 1
fi

echo "🧪 Запуск тестов..."
echo ""

# Запуск тестов с покрытием
mvn clean test jacoco:report

# Проверка результата
if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Все тесты пройдены успешно!"
    echo ""
    echo "📊 Отчёт о покрытии создан: target/site/jacoco/index.html"
    echo ""
    
    # Попытка открыть отчёт (если поддерживается)
    if command -v open &> /dev/null; then
        echo "Открываю отчёт о покрытии в браузере..."
        open target/site/jacoco/index.html
    elif command -v xdg-open &> /dev/null; then
        echo "Открываю отчёт о покрытии в браузере..."
        xdg-open target/site/jacoco/index.html
    else
        echo "Откройте вручную: target/site/jacoco/index.html"
    fi
else
    echo ""
    echo "❌ Некоторые тесты провалились"
    exit 1
fi

