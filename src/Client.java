import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private Scanner messageScanner;

    public Client(Socket socket, String username) {
        try {
            this.username = username;
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);  // Auto-flush enabled
            start();
        } catch (IOException e) {
            closeEverything(socket, in, out);
        }
    }

    @Override
    public void run() {
        listenForMessages();
        sendMessage();
    }

    public void sendMessage() {
        try {
            out.println(username);
            messageScanner = new Scanner(System.in);
            while (socket.isConnected()) {
                String message = messageScanner.nextLine();
                out.println(message);
            }
        } catch (Exception e) {
            closeEverything(socket, in, out);
        }
    }

    public void listenForMessages() {
        new Thread(() -> {
            String message;
            while (socket.isConnected()) {
                try {
                    message = in.readLine();
                    if (message != null) {
                        System.out.println(message);
                        if (message.equals("ERROR: Username contains forbidden words")) {
                            closeEverything(socket, in, out);
                        }
                    }
                } catch (IOException e) {
                    closeEverything(socket, in, out);
                    break;
                }
            }
        }).start();
    }

    public void closeEverything(Socket socket, BufferedReader in, PrintWriter out) {
        try {
            messageScanner.close();
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Scanner usernameScanner = new Scanner(System.in);
            System.out.println("enter your username");
            String username = usernameScanner.nextLine();
            Socket socket = new Socket("localhost", 1234);
            new Client(socket, username);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
