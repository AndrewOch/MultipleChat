package model;

public class Message {

    private Client author;
    private String content;

    public Message(Client author, String content) {
        this.author = author;
        this.content = content;
    }

    @Override
    public String toString() {
        return author.getUsername() + ": " + content;
    }

    public Client getAuthor() {
        return author;
    }

    public void setAuthor(Client author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
