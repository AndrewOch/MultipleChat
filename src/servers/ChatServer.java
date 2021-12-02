package servers;

import model.Client;
import model.Message;
import model.Room;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {
    // Номер порта
    private int DEFAULT_PORT = 5454;

    // Выход из чата при входе пользователя quit
    private final String QUIT = "/exit";

    // Устанавливаем буфер
    private static final int BUFFER = 1024;

    // Используем канал для получения Socket
    private ServerSocketChannel server;

    // Отвечаем за мониторинг всех событий и изменений состояния в канале, который нам нужно обработать
    private Selector selector;

    // Чтение канала
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);

    // Запись в буфер других клиентских каналов при пересылке сообщений
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);

    // Единая кодировка с utf-8
    private Charset charset = Charset.forName("UTF-8");

    // Сохранение пользовательских портов прослушивания на стороне сервера
    private int port;

    private List<Client> clients;

    private List<Room> rooms;


    public ChatServer(int port) {
        this.port = port;

        clients = new CopyOnWriteArrayList<>();
        rooms = new ArrayList<>();
        rooms.add(new Room("11-003"));
        rooms.add(new Room("Друзья"));
        rooms.add(new Room("Флуд"));
    }

    public ChatServer() {
        this.port = DEFAULT_PORT;
    }

    // Включаем сервер
    private void start() {
        try {
            server = ServerSocketChannel.open();
            // Чтобы реализовать неблокирующий вызов NIO, установите ServerSocketChannel в неблокирующее состояние
            server.configureBlocking(false);
            // Устанавливаем порт прослушивания
            server.socket().bind(new InetSocketAddress(port));

            selector = Selector.open();
            // Используется для регистрации с помощью селектора для отслеживания связанных событий подключения, инициированного пользователем к серверу, и связанные события сохраняются в SelectionKey
            server.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Запускаем сервер, слушаем порт:" + port + "...");

            while (true) {
                // Выбор готового канала из нескольких каналов путем опроса
                selector.select();
                // Набор наборов включает все события, запускаемые селектором, которые отслеживаются, когда функция select () в настоящее время вызывается один раз
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                // Итеративная коллекция наборов
                for (SelectionKey key : selectionKeys) {
                    // Обработка запускается по времени
                    handles(key);
                }
                selectionKeys.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(selector);
        }
    }

    // Сервер используется для обработки соединения с клиентом, получения сообщения клиента и его пересылки
    private void handles(SelectionKey key) throws IOException {
        // ПРИНЯТЬ время - установить соединение с клиентом
        if (key.isAcceptable()) {
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            // Клиентский канал
            SocketChannel socketChannel = server.accept();
            // Преобразуем в неблокирующий
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
            System.out.println(getClientName(socketChannel) + "связано");

            Client client = new Client(socketChannel);

            clients.add(client);

            wBuffer.clear();
            wBuffer.put(charset.encode("Введите ваше имя:"));
            wBuffer.flip();
            while (wBuffer.hasRemaining()) {
                socketChannel.write(wBuffer);
            }
        }
        // READ событие-клиент отправляет информацию
        else if (key.isReadable()) {
            SocketChannel socketChannel = (SocketChannel) key.channel();
            // Считываем информацию из канала
            String fwdMsg = receive(socketChannel);
            if (fwdMsg.isEmpty()) {
                // У клиента исключение, и он больше не будет слушать событие
                key.cancel();
                selector.wakeup();
            } else {
                Client client = findClientByPort(socketChannel);
                assert client != null;

                if (client.getAuthorised()) {

                    if (client.getRoomId() == -1) {
                        Integer roomId = Integer.valueOf(fwdMsg);
                        client.setRoomId(roomId);

                        for (Message message : rooms.get(roomId).getMessages()) {
                            wBuffer.clear();
                            wBuffer.put(charset.encode(message.toString()));
                            wBuffer.flip();
                            while (wBuffer.hasRemaining()) {
                                socketChannel.write(wBuffer);
                            }
                        }
                    } else {
                        if ("/quit".equals(fwdMsg)) {
                            // бегаем по всем клиентам и обовещаем их о событии
                            client.setRoomId(-1);
                            for (Client c : clients) {
                                wBuffer.clear();
                                wBuffer.put(charset.encode(client.getUsername() + " exited the chat"));
                                wBuffer.flip();
                                while (wBuffer.hasRemaining()) {
                                    c.getChannel().write(wBuffer);
                                }
                            }

                            wBuffer.clear();
                            wBuffer.put(charset.encode("Выбери чат:\n"));
                            wBuffer.flip();
                            while (wBuffer.hasRemaining()) {
                                socketChannel.write(wBuffer);
                            }

                            for (int i = 0; i < rooms.size(); i++) {
                                wBuffer.clear();
                                wBuffer.put(charset.encode(i + ". " + rooms.get(i).getRoomName() + "\n"));
                                wBuffer.flip();
                                while (wBuffer.hasRemaining()) {
                                    socketChannel.write(wBuffer);
                                }
                            }
                        } else {
                            Message message = new Message(client, fwdMsg);

                            System.out.println(message);
                            // Данные не пустые, затем пересылаем сообщение
                            forwardMessage(socketChannel, fwdMsg);
                            // Проверяем, вышел ли пользователь из системы
                            if (readyToQuit(fwdMsg)) {
                                key.cancel();
                                selector.wakeup();
                                System.out.println(getClientName(socketChannel) + "Отключено");
                            }
                        }
                    }
                } else {
                    authorise(client, fwdMsg);

                    wBuffer.clear();
                    wBuffer.put(charset.encode("Выбери чат:\n"));
                    wBuffer.flip();
                    while (wBuffer.hasRemaining()) {
                        socketChannel.write(wBuffer);
                    }

                    for (int i = 0; i < rooms.size(); i++) {
                        wBuffer.clear();
                        wBuffer.put(charset.encode(i + ". " + rooms.get(i).getRoomName() + "\n"));
                        wBuffer.flip();
                        while (wBuffer.hasRemaining()) {
                            socketChannel.write(wBuffer);
                        }
                    }
                }
            }
        }
    }

    private void authorise(Client client, String name) {
        client.setUsername(name);
        client.setAuthorised(true);
    }

    private Client findClientByPort(SocketChannel socketChannel) {
        for (Client client : clients) {
            if (client.getSocket().equals(socketChannel.socket())) {
                return client;
            }
        }
        return null;
    }

    // пересылаем сообщение, отправленное клиентом, на чужую консоль
    private void forwardMessage(SocketChannel socketChannel, String fwdMsg) throws IOException {
        Client client = findClientByPort(socketChannel);
        assert client != null;

        // keys () вернет все коллекции SelectionKey, которые были зарегистрированы в селекторе (т.е. онлайн-клиенты в чате)
        for (SelectionKey key : selector.keys()) {
            Channel connectedClient = key.channel();
            if (connectedClient instanceof ServerSocketChannel) {
                continue;
            }
            if (key.isValid()) {
                Client currentClient = findClientByPort((SocketChannel) key.channel());
                assert currentClient != null;

                if (Objects.equals(currentClient.getRoomId(), client.getRoomId())) {
                    // ключ сохраняет состояние соединения незакрытым и гарантирует, что сообщение не будет переадресовано лицу, отправившему сообщение, при пересылке сообщения
                    wBuffer.clear();
                    // Записываем данные в буфер
                    wBuffer.put(charset.encode(client.getUsername() + ": " + fwdMsg));
                    // Переводим состояние записи wBuffer в состояние чтения
                    wBuffer.flip();
                    while (wBuffer.hasRemaining()) {
                        ((SocketChannel) connectedClient).write(wBuffer);
                    }
                }
            }
        }
    }

    // Считываем информацию, введенную другими пользователями, из SocketChannel
    private String receive(SocketChannel client) throws IOException {
        // Очищаем содержимое кеша
        rBuffer.clear();
        // Считываем информацию в буфер
        while (client.read(rBuffer) > 0) {
        }
        rBuffer.flip();
        return String.valueOf(charset.decode(rBuffer));
    }


    private String getClientName(SocketChannel client) {
        return "Клиент [" + client.socket().getPort() + "]";
    }


    private boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer(5454);
        chatServer.start();
    }

}