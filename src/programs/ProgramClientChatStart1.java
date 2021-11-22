package programs;

import clients.SocketClient;

import java.util.Scanner;

public class ProgramClientChatStart1 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        SocketClient client = new SocketClient();
        client.startConnection("127.0.0.1", 4445);
        while (true) {
            String message = scanner.nextLine();
            client.sendMessage(message);
        }
    }
}