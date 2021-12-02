package protocol;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

// protocol.Message contents: [DATA LENGTH(4), MESSAGE TYPE(4), DATA(?)]
// Factory Method depending on context can be moved outside
public class Message {
    // Some abstract types of message. E.G. Chat protocol.Message and Register protocol.Message
    // Better to make message complex typed. So values must be 1,2,4,8,... to make available store few values in one int
    public static final int TYPE1 = 1;
    public static final int TYPE2 = 2;

    public static final int MAX_LENGTH = 1000;

    public static Message createMessage(int messageType, byte[] data)
            throws IllegalArgumentException {

        if (data.length > MAX_LENGTH) {
            throw new IllegalArgumentException("protocol.Message can't be " + data.length
                    + " bytes length. Maximum is " + MAX_LENGTH + "."
            );
        }

        if (messageType != TYPE1 && messageType != TYPE2) {
            throw new IllegalArgumentException("Wrong message type");
        }
        return new Message(data, messageType);
    }

    public static byte[] getBytes(Message message) {

        int rawMessageLength = 4 + 4 + message.getData().length;

        byte[] rawMessage = new byte[rawMessageLength];

        int j = 0;
        byte[] type = ByteBuffer.allocate(4).putInt(message.getType()).array();
        for (int i = 0; i < type.length; i++) {
            rawMessage[j++] = type[i];
        }
        byte[] length = ByteBuffer.allocate(4).putInt(message.getData().length).array();
        for (int i = 0; i < length.length; i++) {
            rawMessage[j++] = length[i];
        }
        byte[] data = message.getData();
        for (int i = 0; i < data.length; i++) {
            rawMessage[j++] = data[i];
        }
        return rawMessage;
    }


    protected final byte[] data;
    protected final int type;

    public Message(byte[] data, int type) {
        this.data = data;
        this.type = type;
    }

    public byte[] getData() {
        return data;
    }

    public int getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Message{" +
                "data=" + Arrays.toString(data) +
                ", type=" + type +
                '}';
    }
}