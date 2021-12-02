package programs;

import servers.CServer;

public class ProgramServerCharStart {
    public static void main(String[] args) {
        CServer server = new CServer();
        server.start(4445);
    }
}