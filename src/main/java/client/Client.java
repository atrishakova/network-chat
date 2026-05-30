package client;

public class Client {
    private ChatClient chatClient;
    private Thread clientThread;

    public void start() {
        chatClient = new ChatClient();
        clientThread = new Thread(() -> chatClient.start());
        clientThread.start();
    }

    public void stop() {
        if (chatClient != null) {
            chatClient.stop();
        }
        if (clientThread != null && clientThread.isAlive()) {
            clientThread.interrupt();
        }
    }

    // Добавьте этот метод для запуска из командной строки
    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }
}