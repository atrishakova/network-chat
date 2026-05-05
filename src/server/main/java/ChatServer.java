import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {
    private static final String SETTINGS_FILE_PATH = "settings.txt";
    private static int port;
    private static final String LOGS_FILE_PATH = "src/server/file.log";

    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        // Основной поток.
        loadSettings();
        logToFile("Сервер запущен на порту " + port);

        try(ServerSocket serverSocket = new ServerSocket(port)){
            while (true) {
                // Поток на подключение новых клиентов.
                Socket clientSocket = serverSocket.accept();
                System.out.println("Новое подключение от: " + clientSocket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Ошибка работы сервера: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadSettings() throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(SETTINGS_FILE_PATH))) {
            String line = br.readLine();
            if (line != null && line.startsWith("port=")) {
                port = Integer.parseInt(line.split("=")[1]);
            } else {
                port = 8080;
            }
        }
    }

    public static void broadcast(String message, ClientHandler sender) throws IOException {
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

    public static void removeClient(ClientHandler client) {
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

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            // Поток на каждого клиента.
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

                String msg;
                while ((msg = in.readLine()) != null) {
                    if (msg.equals("EXIT")) {
                        break;
                    }
                    if (msg.startsWith("MSG|")) {
                        broadcast(msg, this);
                    }
                }
            } catch (IOException e) {
                System.err.println("Ошибка в потоке клиента " + username + ": " + e.getMessage());
            } finally {
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
                }
            }
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }
    }
}
