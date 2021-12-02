package clients;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.Set;

public class ChatClient {

    private final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private final int DEFAULT_SERVER_PORT = 5454;
    private final String QUIT = "/exit";
    private static final int BUFFER = 1024;

    private String host;
    private int port;
    private SocketChannel client;
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);
    private Selector selector;
    private Charset charset = Charset.forName("UTF-8");

    public ChatClient() {
        this.host = DEFAULT_SERVER_HOST;
        this.port = DEFAULT_SERVER_PORT;
    }

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    // Закрываем связанный поток
    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Устанавливаем соединение с сервером и отправляем и получаем информацию
    protected void start() {
        try {
            // Открываем канал
            client = SocketChannel.open();
            // Установлен на неблокирующий
            client.configureBlocking(false);

            // Открываем селектор для управления несколькими каналами
            selector = Selector.open();
            // Зарегистрируемся для прослушивания событий подключения
            client.register(selector, SelectionKey.OP_CONNECT);
            // Пользователь подключается к указанному серверу через адрес и порт
            client.connect(new InetSocketAddress(host, port));

            while (true) {
                // Выбор готового канала из нескольких каналов путем опроса
                selector.select();
                // Слушаем, какие события запускаются
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey key : selectionKeys) {
                    // После получения соответствующего канала подключаемся к серверу или принимаем сообщения от других пользователей
                    handles(key);
                }
                selectionKeys.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handles(SelectionKey key) throws IOException {
        // Событие CONNECT - событие готовности соединения
        if (key.isConnectable()) {
            SocketChannel client = (SocketChannel) key.channel();
            if (client.isConnectionPending()) {
                // Указывает, что это соединение находится в состоянии готовности, затем подключается
                client.finishConnect();
                // Обработка введенной пользователем информации (блокировка звонка)
                new Thread(new UserInputHander(this)).start();
            }
            // Регистрируем событие чтения для этого пользователя
            client.register(selector, SelectionKey.OP_READ);
        }
        // событие READ - сервер пересылает сообщение
        else if (key.isReadable()) {
            SocketChannel client = (SocketChannel) key.channel();
            // Считываем данные из канала
            String msg = receive(client);
            if (msg.isEmpty()) {
                // Сервер неисправен, выходим из клиента
                close(selector);
            } else {
                System.out.println(msg);
            }
        }


    }

    // Пользователь отправляет данные на указанный сервер (записывая данные в SocketChannel)
    // Сервер получает SocketChannel в указанном канале через селектор, а затем считывает отправленную информацию через канал
    public void send(String msg) throws IOException {
        if (msg.isEmpty()) {
            // Пользователь не ввел сообщение
            return;
        }
        // Сначала очищаем буферную область
        wBuffer.clear();
        // Записываем информацию для отправки на сервер в буфер wBuffer
        wBuffer.put(charset.encode(msg));
        // Из состояния записи в состояние чтения
        wBuffer.flip();
        while (wBuffer.hasRemaining()) {
            // Пока в буфере есть информация, SocketChannel продолжает читать связанную информацию
            client.write(wBuffer);
        }
        // Проверяем, готов ли пользователь выйти
        if (readyToQuit(msg)) {
            close(selector);
        }
    }

    private String receive(SocketChannel client) throws IOException {
        rBuffer.clear();
        // Считываем информацию из буфера
        while (client.read(rBuffer) > 0) {
        }
        // Переход из состояния чтения в состояние записи
        rBuffer.flip();
        return String.valueOf(charset.decode(rBuffer));
    }
}

class UserInputHander implements Runnable {

    private ChatClient chatClient;

    public UserInputHander(ChatClient chatClient) {
        this.chatClient = chatClient;
    }


    @Override
    public void run() {
        try {
            // Ожидаем ввода пользователя
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String input = consoleReader.readLine();
                chatClient.send(input);

                if (chatClient.readyToQuit(input)) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Client1 {
    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
        chatClient.start();
    }
}

class Client2 {
    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
        chatClient.start();
    }
}
