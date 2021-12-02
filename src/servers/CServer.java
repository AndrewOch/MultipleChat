package servers;

import model.Client;
import model.Message;
import model.Room;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CServer {

    private static boolean running = true;
    // список клиентов
    private List<ClientHandler> clients;
    private List<Room> rooms;

    public CServer() {
        // Список для работы с многопоточностью
        clients = new CopyOnWriteArrayList<>();
        rooms = new ArrayList<>();
        rooms.add(new Room("11-003"));
        rooms.add(new Room("Друзья"));
        rooms.add(new Room("Флуд"));

    }

    public void start(int port) {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        // запускаем бесконечный цикл
        while (running) {
            try {
                // запускаем обработчик сообщений для каждого подключаемого клиента
                new ClientHandler(serverSocket.accept()).start();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private class ClientHandler extends Thread {
        // связь с одним клиентом
        private Client client;
        // информация, поступающая от клиента
        private BufferedReader in;

        ClientHandler(Socket socket) throws IOException {
            // добавляем текущее подключение в список
            this.client = new Client(socket);
            clients.add(this);
            System.out.println("New client " + socket.getPort());

            PrintWriter out = new PrintWriter(client.getSocket().getOutputStream(), true);
            out.println("Введите своё имя:");
        }

        public void run() {
            try {
                // получем входной поток для конкретного клиента
                in = new BufferedReader(
                        new InputStreamReader(client.getSocket().getInputStream()));

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (!client.getAuthorised()) {
                        client.setUsername(inputLine);
                        client.setAuthorised(true);

                        PrintWriter out = new PrintWriter(client.getSocket().getOutputStream(), true);
                        out.println("Выбери чат:");
                        for (int i = 0; i < rooms.size(); i++) {
                            out.println(i + ". " + rooms.get(i).getRoomName());
                        }
                    } else {
                        if (client.getRoomId() == -1) {
                            Integer roomId = Integer.valueOf(inputLine);
                            client.setRoomId(roomId);

                            PrintWriter out = new PrintWriter(client.getSocket().getOutputStream(), true);

                            for (Message message : rooms.get(roomId).getMessages()) {
                                out.println(message.toString());
                            }

                        } else {
                            if ("/exit".equals(inputLine)) {
                                // бегаем по всем клиентам и обовещаем их о событии
                                client.setRoomId(-1);
                                for (ClientHandler client : clients) {
                                    PrintWriter out = new PrintWriter(client.client.getSocket().getOutputStream(), true);
                                    out.println(this.client.getUsername() + " exited the chat");
                                }
                                PrintWriter out = new PrintWriter(client.getSocket().getOutputStream(), true);
                                out.println("Выбери чат:");
                                for (int i = 0; i < rooms.size(); i++) {
                                    out.println(i + ". " + rooms.get(i).getRoomName());
                                }
                            } else {
                                Message message = new Message(client, inputLine);

                                rooms.get(client.getRoomId()).addMessage(message);
                                for (ClientHandler client : clients) {
                                    if (client.client.getAuthorised()) {
                                        PrintWriter out = new PrintWriter(client.client.getSocket().getOutputStream(), true);
                                        out.println(message);
                                    }
                                }
                            }
                        }
                    }
                }
                in.close();
                client.getSocket().close();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}