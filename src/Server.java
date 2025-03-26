import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class Server extends Thread {

    private final ServerSocket SERVER_SOCKET;
    private final int PORT;
    private static final Set<String> bannedWords = new HashSet<>();
    public Server(String configFilePath) {
        Properties prop = new Properties();
        try (FileInputStream fis = new FileInputStream(configFilePath)) {
            prop.load(fis);
            this.PORT = Integer.parseInt(prop.getProperty("PORT"));
            String[] words = prop.getProperty("BANNED_WORDS", "").split(",");
            bannedWords.addAll(Arrays.asList(words));
            SERVER_SOCKET = new ServerSocket(PORT);
            this.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void startServer() {
        System.out.println("Server started on port: " + PORT);
        while (!SERVER_SOCKET.isClosed()) {
            try {
                Socket socket = SERVER_SOCKET.accept();
                System.out.println("Client connected");
                ClientHandler clientHandler = new ClientHandler(socket);
                Thread thread = new Thread(clientHandler);
                thread.start();

            } catch (IOException e) {
                closeServerSocket();
                throw new RuntimeException(e);
            }
        }
    }

    public void closeServerSocket() {
        try {
            if (SERVER_SOCKET != null) {
                SERVER_SOCKET.close();
                System.out.println("Server closed");
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public static Set<String> getBannedWords() {
        return bannedWords;
    }

    @Override
    public void run() {
        startServer();
    }

    public static void main(String[] args) {
        new Server("server.properties");
    }
}
