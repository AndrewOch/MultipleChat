package model;

import java.util.ArrayList;
import java.util.List;

public class Room {

    private String roomName;
    private List<Message> messages;

    public Room(String roomName, List<Message> messages) {
        this.roomName = roomName;
    }

    public Room() {
    }

    public void addMessage(Message message) {
        if (messages.size() < 100) {
            messages.add(message);
        } else {
            messages.remove(0);
            messages.add(message);
        }
    }

    public Room(String roomName) {
        this.roomName = roomName;
        this.messages = new ArrayList<>();
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }


    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
