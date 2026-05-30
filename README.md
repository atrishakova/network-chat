# Network chat

## Запуск
1. Настроить `settings.txt` (порт)
2. Собрать: `mvn compile`
3. Сервер: `java src/main/java/server/ChatServer.java`
4. Клиент: `java src/main/java/client/ChatClient.java`
5. Ввести имя, отправить сообщение, `/exit` для выхода

## Логи
Все действия пишутся в `file.log` (сервер: src/main/java/server/file.log и каждый клиент: src/main/java/client/file.log).