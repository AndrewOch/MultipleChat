package model;

import java.net.Socket;
import java.nio.channels.SocketChannel;

public class Client {

    private String username;
    private Socket socket;
    private SocketChannel channel;
    private Boolean isAuthorised;
    private Integer roomId;

    public Client(String username, SocketChannel channel) {
        this.username = username;
        this.channel = channel;
        this.socket = channel.socket();
        this.isAuthorised = true;
        this.roomId = -1;
    }

    public Client(SocketChannel channel) {
        this.channel = channel;
        this.socket = channel.socket();
        this.isAuthorised = false;
        this.roomId = -1;
    }

    public Client(String username, Socket socket) {
        this.username = username;
        this.socket = socket;
        this.isAuthorised = true;
        this.roomId = -1;
    }

    public Client(Socket socket) {
        this.socket = socket;
        this.isAuthorised = false;
        this.roomId = -1;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public void setAuthorised(Boolean authorised) {
        isAuthorised = authorised;
    }

    public Boolean getAuthorised() {
        return isAuthorised;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }
}
