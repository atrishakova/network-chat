import org.junit.Test;
import java.io.*;
import static org.junit.Assert.*;

public class ChatServerTest {
    @Test
    public void testLogToFile() {
        File log = new File("src/server/file.log");
        long before = log.exists() ? log.length() : 0;
        ChatServer.logToFile("Тест");
        assertTrue(log.length() > before);
    }
}