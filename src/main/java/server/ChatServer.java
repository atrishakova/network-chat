package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {
    private static final String SETTINGS_FILE_PATH = "settings.txt";
    private static final String LOGS_FILE_PATH = "src/main/java/server/file.log";

    private int port;
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private ServerSocket serverSocket;
    private volatile boolean running = true;

    public void stop() {
        if (!running) return;

        running = false;

        // Отправляем всем клиентам сообщение о закрытии сервера
        broadcastToAll("SERVER_SHUTDOWN|Сервер остановлен. Соединение будет закрыто.");

        // Небольшая задержка, чтобы клиенты успели получить сообщение
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            // Закрыть все соединения клиентов
            for (ClientHandler client : clients) {
                client.close();
            }
            clients.clear();
            System.out.println("Сервер успешно остановлен. Все клиенты отключены.");
        } catch (IOException e) {
            System.err.println("Ошибка при остановке сервера: " + e.getMessage());
        }
    }

    private void broadcastToAll(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public void start() {
        try {
            loadSettings();
            serverSocket = new ServerSocket(port);
            logToFile("Сервер запущен на порту " + port);
            System.out.println("Сервер запущен на порту " + port);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Новое подключение от: " + clientSocket.getRemoteSocketAddress());
                    ClientHandler handler = new ClientHandler(clientSocket);
                    clients.add(handler);
                    new Thread(handler).start();
                } catch (SocketException e) {
                    if (running) {
                        System.err.println("Ошибка при принятии подключения: " + e.getMessage());
                    } else {
                        System.out.println("Сервер остановлен, новые подключения не принимаются");
                    }
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Ошибка работы сервера: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void loadSettings() throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(SETTINGS_FILE_PATH))) {
            String line = br.readLine();
            if (line != null && line.startsWith("port=")) {
                port = Integer.parseInt(line.split("=")[1]);
            } else {
                port = 8080;
            }
        }
    }

    private void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
        logToFile(message);

        String[] parts = message.split("\\|", 4);
        if (parts.length == 4) {
            System.out.println("[" + parts[1] + "]: " + parts[3]);
        }
    }

    private void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    public static void logToFile(String message) {
        synchronized (ChatServer.class) {
            try (FileWriter fw = new FileWriter(LOGS_FILE_PATH, true);
                 PrintWriter out = new PrintWriter(fw)) {
                out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + " " + message);
            } catch (IOException e) {
                System.err.println("Ошибка записи в лог: " + e.getMessage());
            }
        }
    }

    class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String init = in.readLine();

                if (init == null || !init.startsWith("HELLO|")) {
                    System.out.println("Неверный протокол, закрываем соединение");
                    socket.close();
                    return;
                }

                username = init.split("\\|")[1];
                logToFile(username + " подключился");
                System.out.println(username + " присоединился к чату");

                String msg;
                while ((msg = in.readLine()) != null && running) {
                    if (msg.equals("EXIT")) {
                        break;
                    }
                    if (msg.startsWith("MSG|")) {
                        broadcast(msg, this);
                    }
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("Ошибка в потоке клиента " + username + ": " + e.getMessage());
                }
            } finally {
                close();
            }
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        public void close() {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Ошибка при закрытии сокета: " + e.getMessage());
            }
            removeClient(this);
            if (username != null) {
                logToFile(username + " отключился");
                System.out.println(username + " покинул чат");
            }
        }
    }
}