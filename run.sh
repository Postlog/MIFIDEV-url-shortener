#!/bin/bash
# Скрипт для запуска URL Shortener

echo "=== URL Shortener ==="
echo "Сборка проекта..."

# Проверка наличия Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven не найден. Установите Maven для продолжения."
    exit 1
fi

# Сборка проекта
mvn clean package -q

# Проверка успешности сборки
if [ $? -ne 0 ]; then
    echo "❌ Ошибка сборки проекта"
    exit 1
fi

echo "✅ Сборка завершена успешно"
echo "Запуск приложения..."
echo ""

# Запуск приложения
java -jar target/url-shortener.jar

