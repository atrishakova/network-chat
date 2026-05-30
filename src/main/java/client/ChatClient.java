package client;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatClient {
    private static final String SETTINGS_FILE = "settings.txt";
    private static final String LOGS_FILE_PATH = "src/main/java/client/file.log";

    private String serverHost = "localhost";
    private int serverPort;
    private String username;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private BufferedReader console;
    private Thread receiverThread;
    private volatile boolean running = true;

    public void start() {
        try {
            loadSettings();

            System.out.print("Введите ваше имя: ");
            console = new BufferedReader(new InputStreamReader(System.in));
            username = console.readLine();

            socket = new Socket(serverHost, serverPort);
            System.out.println("Подключено к чату!");

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("HELLO|" + username);
            logToFile("Подключен к серверу как " + username);

            // Поток на чтение входящих сообщений
            receiverThread = new Thread(this::receiveMessages);
            receiverThread.start();

            // Основной цикл отправки сообщений
            sendMessages();

        } catch (IOException e) {
            if (running) {
                System.err.println("Ошибка подключения к серверу: " + e.getMessage());
            } else {
                System.out.println("Соединение с сервером потеряно");
            }
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        try {
            if (out != null) out.println("EXIT");
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (console != null) console.close();
        } catch (IOException e) {
            // Игнорируем ошибки при закрытии
        }
    }

    private void receiveMessages() {
        try {
            String msg;
            while (running && (msg = in.readLine()) != null) {
                if (msg.startsWith("SERVER_SHUTDOWN")) {
                    System.out.println("\n" + msg.split("\\|")[1]);
                    System.out.println("Нажмите Enter для выхода...");
                    running = false;
                    break;
                } else if (msg.startsWith("MSG|")) {
                    String[] parts = msg.split("\\|", 4);
                    if (parts.length == 4) {
                        String sender = parts[1];
                        String text = parts[3];
                        System.out.println("\n[" + sender + "]: " + text);
                        if (running) {
                            System.out.print("Вы: ");
                        }
                    }
                }
                logToFile(msg);
            }
        } catch (IOException e) {
            if (running) {
                System.out.println("\nСоединение с сервером потеряно");
                running = false;
            }
        }
    }

    private void sendMessages() throws IOException {
        String userInput;
        System.out.println("Чат начат! Для выхода введите /exit");
        System.out.print("Вы: ");

        while (running && (userInput = console.readLine()) != null) {
            if (userInput.equals("/exit")) {
                out.println("EXIT");
                System.out.println("Выход из чата...");
                break;
            }
            if (!running) break;

            String fullMsg = "MSG|" + username + "|" + System.currentTimeMillis() + "|" + userInput;
            out.println(fullMsg);
            logToFile(fullMsg);
            if (running) {
                System.out.print("Вы: ");
            }
        }
    }

    private void loadSettings() throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(SETTINGS_FILE))) {
            String line = br.readLine();
            if (line != null && line.startsWith("port=")) {
                serverPort = Integer.parseInt(line.split("=")[1]);
            } else {
                serverPort = 8080;
            }
        }
    }

    public static void logToFile(String message) {
        try (FileWriter fw = new FileWriter(LOGS_FILE_PATH, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + " " + message);
        } catch (IOException e) {
            System.err.println("Ошибка записи в лог: " + e.getMessage());
        }
    }
}