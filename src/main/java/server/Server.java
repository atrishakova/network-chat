package server;

import java.io.IOException;

public class Server {
    private ChatServer chatServer;
    private Thread serverThread;

    public void start() {
        chatServer = new ChatServer();
        serverThread = new Thread(() -> chatServer.start());
        serverThread.start();
    }

    public void stop() {
        if (chatServer != null) {
            chatServer.stop();
        }
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }

    // Добавьте этот метод для запуска из командной строки
    public static void main(String[] args) {
        Server server = new Server();
        server.start();

        // Остановка по Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Остановка сервера...");
            server.stop();
        }));

        System.out.println("Сервер запущен. Нажмите Ctrl+C для остановки.");
    }
}
