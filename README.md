# Network chat

## Запуск
1. Настроить `settings.txt` (порт)
2. Собрать: `mvn compile`
3. Сервер: `java src/server/main/java/ChatServer.java`
4. Клиент: `java src/client/main/java/ChatClient.java`
5. Ввести имя, отправить сообщение, `/exit` для выхода

## Логи
Все действия пишутся в `file.log` (сервер: src/server/file.log и каждый клиент: src/client/file.log).