import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {

    public static Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    public String username;
    private Set<String> bannedWords = Server.getBannedWords();

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            this.username = this.in.readLine();
            if (isBlacklisted(username)) {
                out.println("ERROR: Username contains forbidden words");
                closeEverything(socket, in, out);
                return;
            }
            clients.put(this.username, this);
            broadcastMessage("has joined the chat");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        out.println("Welcome to my server " + this.username);
        out.println("write /help to see the list of commands");
        String messageFromClient;

        while (socket.isConnected()) {
            try {
                messageFromClient = in.readLine();
                System.out.println(messageFromClient);
                if (messageFromClient != null) {
                    if (messageFromClient.startsWith("/")) {
                        handleCommand(messageFromClient);
                    } else {
                        if (!isBlacklisted(messageFromClient)) broadcastMessage(messageFromClient);
                        else {
                            System.out.println("Message from client is blacklisted, message was not sent");
                            out.println("ERROR: your message was not sent because it contains forbidden words");
                        }
                    }
                }

            } catch (IOException e) {
                closeEverything(socket, in, out);
                break;
            }
        }
    }

    public void broadcastMessage(String message) {
        for (ClientHandler client : clients.values()) {
            if (!client.username.equals(this.username)) {
                client.out.println(this.username + ": " + message);
            }
        }
    }

    public void removeClientHandler() {
        clients.remove(this.username);
        broadcastMessage(this.username + " has left the chat");
    }

    private boolean isBlacklisted(String message) {
        for (String bannedWord : bannedWords) {
            if (message.toLowerCase().contains(bannedWord)) {
                return true;
            }

        }
        return false;
    }


    private void handleCommand(String message) {
        String[] tokens = message.split(" ", 2);
        String command = tokens[0];
        String arguments = tokens.length > 1 ? tokens[1] : "";
        System.out.println(Arrays.deepToString(tokens));
        switch (command.toLowerCase()) {
            case "/msg" -> handlePrivateMessage(arguments);
            case "/help" -> sendHelpMessage();
            case "/userlist" -> sendUserList();
            case "/bannedwords" -> sendBannedWordsList();
            case "/msgexcept" -> handleMessageExcept(arguments);
            default -> out.println("ERROR: unknown command. Type /help to get the list of all available commands");
        }

    }


    private void handlePrivateMessage(String arguments) {

        String[] tokens = arguments.split(" ");

        if (tokens.length < 2) {
            out.println("Invalid command. Usage: /msg <username1> <username2> ... <message>");
            return;
        }

        int messageStartIndex = 0;
        for (int i = 0; i < tokens.length; i++) {
            if (!clients.containsKey(tokens[i])) {
                messageStartIndex = i;
                break;
            }
        }

        if (messageStartIndex == 0) {
            out.println("No valid message provided after the usernames.");
            return;
        }

        String message = String.join(" ", Arrays.copyOfRange(tokens, messageStartIndex, tokens.length));

        StringBuilder offlineUsers = new StringBuilder();
        for (int i = 0; i < messageStartIndex; i++) {
            String targetUsername = tokens[i];
            ClientHandler targetClient = clients.get(targetUsername);

            if (targetClient != null) {
                targetClient.out.println("[Private from " + this.username + "]: " + message);
            } else {
                offlineUsers.append(targetUsername).append(" ");
            }
        }

        this.out.println("[Private to one or multiple users]: " + message);


        if (offlineUsers.length() > 0) {
            this.out.println("The following users are not online: " + offlineUsers.toString().trim());
        }
    }


    private void sendHelpMessage() {
        String helpMessage = "Available commands:\n"
                + "/msg <username1> <username2> ... <message> - Send a private message to one multiple users.\n"
                + "/help - Show this help message.\n"
                + "/userlist - Show the list of online users.\n"
                + "/bannedwords - Send the list of banned words"
                + "/msgexcept <username1> <username2> ... <message> - Send message to every user except specified ones";
        out.println(helpMessage);
    }

    private void sendUserList() {
        StringBuilder userList = new StringBuilder("Connected users:\n");
        for (String user : clients.keySet()) {
            userList.append(user).append("\n");
        }
        out.println(userList.toString());
    }


    private void sendBannedWordsList() {
        out.println("list of banned words:");
        for(String bannedWord :bannedWords) out.println(bannedWord);
    }


    private void handleMessageExcept(String arguments) {

        String[] tokens = arguments.split(" ");

        if (tokens.length < 2) {
            out.println("Invalid command. Usage: /msgexcept <username1> <username2> ... <message>");
            return;
        }

        int messageStartIndex = 0;
        for (int i = 0; i < tokens.length; i++) {
            if (!clients.containsKey(tokens[i])) {
                messageStartIndex = i;
                break;
            }
        }

        if (messageStartIndex == 0) {
            out.println("No valid message provided after the usernames.");
            return;
        }


        String message = String.join(" ", Arrays.copyOfRange(tokens, messageStartIndex, tokens.length));


        Set<String> excludedUsers = new HashSet<>();
        for (int i = 0; i < messageStartIndex; i++) {
            excludedUsers.add(tokens[i]);
        }
        excludedUsers.add(this.username);

        StringBuilder offlineUsers = new StringBuilder();
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            String targetUsername = entry.getKey();
            ClientHandler targetClient = entry.getValue();

            if (!excludedUsers.contains(targetUsername)) {
                try {
                    targetClient.out.println("[Broadcast from " + this.username + " to everyone except specified]: " + message);
                } catch (Exception e) {
                    closeEverything(socket, in, out);
                }
            }
        }


        this.out.println("[Broadcast to everyone except " + excludedUsers + "]: " + message);
    }




    private void closeEverything(Socket socket, BufferedReader in, PrintWriter out) {
        removeClientHandler();
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            System.out.println("client disconnected");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
