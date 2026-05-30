import client.ChatClient;
import org.junit.Test;
import java.io.*;
import static org.junit.Assert.*;

public class ChatClientTest {
    @Test
    public void testLogToFile() {
        File log = new File("src/main/java/client/file.log");
        long before = log.exists() ? log.length() : 0;
        ChatClient.logToFile("Тест");
        assertTrue(log.length() > before);
    }
}