import java.io.Serializable;

public class ServiceMessage implements Serializable {
    private MessageType messageType;
    private String fileName;
    private String parentDir;
    private long fileSize;
    private int currentPart;
    private byte [] body;

    public ServiceMessage(MessageType messageType) {
        this.messageType = messageType;
    }
    public ServiceMessage(MessageType messageType, String fileName) {
        this.messageType = messageType;
        this.fileName = fileName;
    }
    public ServiceMessage(MessageType messageType, long fileSize, int currentPart, byte[] body) {
        this.messageType = messageType;
        this.fileSize = fileSize;
        this.currentPart = currentPart;
        this.body = body;
    }

    public MessageType getMessageType() {
        return messageType;
    }
    public String getFileName() {
        return fileName;
    }
    public long getFileSize() {
        return fileSize;
    }
    public int getCurrentPart() {
        return currentPart;
    }
    public byte[] getBody() {
        return body;
    }
}