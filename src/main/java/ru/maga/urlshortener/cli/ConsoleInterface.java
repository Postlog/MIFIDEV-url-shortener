package ru.maga.urlshortener.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.maga.urlshortener.config.AppConfig;
import ru.maga.urlshortener.domain.ShortUrl;
import ru.maga.urlshortener.service.LinkManagementService;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Console interface for the URL shortener application.
 * Provides user-friendly CLI with commands and help.
 */
public class ConsoleInterface {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleInterface.class);
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    private final LinkManagementService linkService;
    private final AppConfig config;
    private final BufferedReader reader;
    private UUID currentUserId;
    private boolean running;

    public ConsoleInterface(LinkManagementService linkService, AppConfig config) {
        this.linkService = linkService;
        this.config = config;
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.running = true;
    }

    /**
     * Starts the interactive console interface.
     */
    public void start() {
        printWelcome();
        initializeUser();

        while (running) {
            try {
                System.out.print("\n> ");
                String input = reader.readLine();
                if (input == null || input.trim().isEmpty()) {
                    continue;
                }
                processCommand(input.trim());
            } catch (IOException e) {
                logger.error("Error reading input", e);
                System.out.println("❌ Ошибка чтения ввода");
            }
        }
    }

    private void printWelcome() {
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║          Сервис сокращения ссылок URL Shortener          ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println("\nВведите 'help' для списка команд");
    }

    private void initializeUser() {
        System.out.print("\nВведите ваш UUID (или нажмите Enter для создания нового): ");
        try {
            String input = reader.readLine().trim();
            if (input.isEmpty()) {
                currentUserId = linkService.createUser();
                System.out.println("\n✅ Создан новый пользователь");
                System.out.println("📋 Ваш UUID: " + currentUserId);
                System.out.println("⚠️  Сохраните этот UUID для будущих сессий!");
            } else {
                try {
                    UUID userId = UUID.fromString(input);
                    if (linkService.userExists(userId)) {
                        currentUserId = userId;
                        System.out.println("✅ Вход выполнен успешно");
                    } else {
                        System.out.println("⚠️  Пользователь не найден, создаю новый аккаунт");
                        currentUserId = linkService.createUser();
                        System.out.println("📋 Ваш UUID: " + currentUserId);
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("❌ Некорректный UUID, создаю новый аккаунт");
                    currentUserId = linkService.createUser();
                    System.out.println("📋 Ваш UUID: " + currentUserId);
                }
            }
        } catch (IOException e) {
            logger.error("Error initializing user", e);
            currentUserId = linkService.createUser();
        }
    }

    private void processCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        String command = parts[0].toLowerCase();

        try {
            switch (command) {
                case "help" -> printHelp();
                case "create" -> handleCreate(parts);
                case "open" -> handleOpen(parts);
                case "list" -> handleList();
                case "info" -> handleInfo(parts);
                case "update" -> handleUpdate(parts);
                case "delete" -> handleDelete(parts);
                case "stats" -> handleStats();
                case "uuid" -> printCurrentUuid();
                case "cleanup" -> handleCleanup();
                case "exit", "quit" -> handleExit();
                default -> System.out.println("❌ Неизвестная команда. Введите 'help' для справки.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Ошибка: " + e.getMessage());
        } catch (SecurityException e) {
            System.out.println("🔒 Ошибка доступа: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing command", e);
            System.out.println("❌ Произошла ошибка при выполнении команды");
        }
    }

    private void printHelp() {
        System.out.println("\n📚 Доступные команды:");
        System.out.println("\n  create <URL> [limit]  - Создать короткую ссылку");
        System.out.println("                          URL: полная ссылка (http:// или https://)");
        System.out.println("                          limit: макс. кол-во переходов (по умолчанию: " +
                config.getDefaultClickLimit() + ")");
        System.out.println("                          Пример: create https://example.com 50");
        System.out.println("\n  open <код>            - Открыть ссылку в браузере");
        System.out.println("                          Пример: open aB3Xy9");
        System.out.println("\n  list                  - Показать все ваши ссылки");
        System.out.println("\n  info <код>            - Показать информацию о ссылке");
        System.out.println("                          Пример: info aB3Xy9");
        System.out.println("\n  update <код> <лимит>  - Изменить лимит переходов");
        System.out.println("                          Пример: update aB3Xy9 200");
        System.out.println("\n  delete <код>          - Удалить ссылку (только владелец)");
        System.out.println("                          Пример: delete aB3Xy9");
        System.out.println("\n  stats                 - Показать статистику системы");
        System.out.println("  uuid                  - Показать ваш UUID");
        System.out.println("  cleanup               - Запустить очистку истекших ссылок");
        System.out.println("  help                  - Показать эту справку");
        System.out.println("  exit, quit            - Выйти из программы");
    }

    private void handleCreate(String[] parts) {
        if (parts.length < 2) {
            System.out.println("❌ Использование: create <URL> [limit]");
            return;
        }

        String url = parts[1].split("\\s+")[0];
        Integer clickLimit = null;

        String[] urlAndLimit = parts[1].split("\\s+");
        if (urlAndLimit.length > 1) {
            try {
                clickLimit = Integer.parseInt(urlAndLimit[1]);
            } catch (NumberFormatException e) {
                System.out.println("⚠️  Некорректный лимит, используется значение по умолчанию");
            }
        }

        ShortUrl shortUrl = linkService.createShortUrl(url, currentUserId, clickLimit);
        System.out.println("\n✅ Короткая ссылка создана успешно!");
        System.out.println("📎 Короткий код: " + shortUrl.getShortCode());
        System.out.println("🔗 Полная короткая ссылка: " + config.getShortenerDomain() + "/" + shortUrl.getShortCode());
        System.out.println("🎯 Оригинальный URL: " + shortUrl.getOriginalUrl());
        System.out.println("⏱  Истекает: " + DATE_FORMATTER.format(shortUrl.getExpiresAt()));
        System.out.println("🔢 Лимит переходов: " + formatLimit(shortUrl.getClickLimit()));
    }

    private void handleOpen(String[] parts) {
        if (parts.length < 2) {
            System.out.println("❌ Использование: open <код>");
            return;
        }

        String shortCode = parts[1];
        Optional<String> originalUrl = linkService.processClick(shortCode);

        if (originalUrl.isEmpty()) {
            System.out.println("❌ Ссылка недоступна или не найдена");
            linkService.getShortUrlInfo(shortCode).ifPresent(url -> {
                if (url.isExpired()) {
                    System.out.println("⏰ Причина: истёк срок действия");
                } else if (url.hasReachedClickLimit()) {
                    System.out.println("🚫 Причина: исчерпан лимит переходов");
                }
            });
            return;
        }

        System.out.println("🌐 Открываю URL: " + originalUrl.get());
        openUrlInBrowser(originalUrl.get());
    }

    private void handleList() {
        List<ShortUrl> links = linkService.getUserLinks(currentUserId);

        if (links.isEmpty()) {
            System.out.println("\n📭 У вас пока нет созданных ссылок");
            return;
        }

        System.out.println("\n📋 Ваши ссылки (" + links.size() + "):");
        System.out.println("─".repeat(120));

        for (ShortUrl link : links) {
            String status = link.isAccessible() ? "✅" : (link.isExpired() ? "⏰" : "🚫");
            System.out.printf("%s %s | %s\n", status, link.getShortCode(),
                    truncate(link.getOriginalUrl(), 60));
            System.out.printf("   Переходов: %d/%s | Истекает: %s\n",
                    link.getClickCount(),
                    formatLimit(link.getClickLimit()),
                    DATE_FORMATTER.format(link.getExpiresAt()));
            System.out.println();
        }
    }

    private void handleInfo(String[] parts) {
        if (parts.length < 2) {
            System.out.println("❌ Использование: info <код>");
            return;
        }

        String shortCode = parts[1];
        Optional<ShortUrl> shortUrlOpt = linkService.getShortUrlInfo(shortCode);

        if (shortUrlOpt.isEmpty()) {
            System.out.println("❌ Ссылка не найдена");
            return;
        }

        ShortUrl shortUrl = shortUrlOpt.get();
        System.out.println("\n📊 Информация о ссылке:");
        System.out.println("─".repeat(80));
        System.out.println("📎 Короткий код: " + shortUrl.getShortCode());
        System.out.println("🔗 Полная короткая ссылка: " + config.getShortenerDomain() + "/" + shortUrl.getShortCode());
        System.out.println("🎯 Оригинальный URL: " + shortUrl.getOriginalUrl());
        System.out.println("👤 Владелец: " + (shortUrl.isOwnedBy(currentUserId) ? "Вы" : "Другой пользователь"));
        System.out.println("📅 Создана: " + DATE_FORMATTER.format(shortUrl.getCreatedAt()));
        System.out.println("⏰ Истекает: " + DATE_FORMATTER.format(shortUrl.getExpiresAt()));
        System.out.println("🔢 Переходов: " + shortUrl.getClickCount() + "/" + formatLimit(shortUrl.getClickLimit()));
        System.out.println("📊 Статус: " + (shortUrl.isAccessible() ? "✅ Активна" :
                (shortUrl.isExpired() ? "⏰ Истекла" : "🚫 Лимит исчерпан")));
    }

    private void handleUpdate(String[] parts) throws IOException {
        if (parts.length < 2) {
            System.out.println("❌ Использование: update <код> <новый_лимит>");
            return;
        }

        String[] args = parts[1].split("\\s+");
        if (args.length < 2) {
            System.out.println("❌ Использование: update <код> <новый_лимит>");
            return;
        }

        String shortCode = args[0];
        int newLimit = Integer.parseInt(args[1]);

        linkService.updateClickLimit(shortCode, currentUserId, newLimit);
        System.out.println("✅ Лимит переходов обновлён: " + formatLimit(newLimit));
    }

    private void handleDelete(String[] parts) {
        if (parts.length < 2) {
            System.out.println("❌ Использование: delete <код>");
            return;
        }

        String shortCode = parts[1];
        linkService.deleteShortUrl(shortCode, currentUserId);
        System.out.println("✅ Ссылка успешно удалена");
    }

    private void handleStats() {
        System.out.println("\n📊 Статистика системы:");
        System.out.println("─".repeat(40));
        System.out.println(linkService.getStatistics());
        System.out.println("⚙️  TTL по умолчанию: " + config.getLinkTtlSeconds() + "с (" +
                (config.getLinkTtlSeconds() / 3600) + "ч)");
        System.out.println("🔢 Лимит по умолчанию: " + formatLimit(config.getDefaultClickLimit()));
    }

    private void printCurrentUuid() {
        System.out.println("\n👤 Ваш UUID: " + currentUserId);
    }

    private void handleCleanup() {
        System.out.println("🧹 Запуск очистки истекших ссылок...");
        int deleted = linkService.cleanupExpiredLinks();
        System.out.println("✅ Очистка завершена. Удалено ссылок: " + deleted);
    }

    private void handleExit() {
        System.out.println("\n👋 До свидания!");
        running = false;
    }

    private void openUrlInBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("✅ Ссылка открыта в браузере");
            } else {
                System.out.println("⚠️  Desktop API не поддерживается на этой системе");
                System.out.println("🔗 Скопируйте ссылку: " + url);
            }
        } catch (Exception e) {
            logger.error("Error opening URL in browser", e);
            System.out.println("❌ Не удалось открыть браузер");
            System.out.println("🔗 Скопируйте ссылку: " + url);
        }
    }

    private String formatLimit(int limit) {
        return limit == -1 ? "∞" : String.valueOf(limit);
    }

    private String truncate(String str, int length) {
        return str.length() > length ? str.substring(0, length - 3) + "..." : str;
    }
}

