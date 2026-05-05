import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatClient {
    private static final String SETTINGS_FILE = "settings.txt";
    private static final String LOGS_FILE_PATH = "src/client/file.log";
    private static String serverHost = "localhost";
    private static int serverPort;
    private static String username;

    public static void main(String[] args) throws IOException {
        loadSettings();

        System.out.print("Введите ваше имя: ");
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        username = console.readLine();

        Socket socket = new Socket(serverHost, serverPort);
        System.out.println("Подключено к чату!");

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println("HELLO|" + username);
        logToFile("Подключен к серверу как " + username);

        // Поток на чтение входящих сообщений от сервера.
        Thread receiver = new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    if (msg.startsWith("MSG|")) {
                        String[] parts = msg.split("\\|", 4);
                        if (parts.length == 4) {
                            String sender = parts[1];
                            String text = parts[3];

                            System.out.println("\n[" + sender + "]: " + text);
                            System.out.print("Вы: ");
                        }
                    }
                    logToFile(msg);
                }
            } catch (IOException e) {
                System.out.println("Соединение потеряно");
            }
        });
        receiver.start();

        String userInput;
        System.out.println("Чат начат! Для выхода введите /exit");
        System.out.print("Вы: ");

        while (true) {
            // Основной поток на чтение вводимого.
            userInput = console.readLine();
            if (userInput.equals("/exit")) {
                out.println("EXIT");
                System.out.println("Выход из чата...");
                break;
            }
            String fullMsg = "MSG|" + username + "|" + System.currentTimeMillis() + "|" + userInput;
            out.println(fullMsg);
            logToFile(fullMsg);
            System.out.print("Вы: ");
        }

        socket.close();
        System.exit(0);
    }

    private static void loadSettings() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(SETTINGS_FILE));
        String line = br.readLine();
        if (line != null && line.startsWith("port=")) {
            serverPort = Integer.parseInt(line.split("=")[1]);
        }
        br.close();
    }

    public static void logToFile(String message) {
        try (FileWriter fw = new FileWriter(LOGS_FILE_PATH, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + " " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}