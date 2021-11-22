package programs;

import servers.ChatServer;

public class ProgramServerCharStart {
    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.start(4445);
    }
}