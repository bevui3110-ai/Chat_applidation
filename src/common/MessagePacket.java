package common;

import java.io.Serializable;
import java.util.UUID;

public class MessagePacket implements Serializable {
    private static final long serialVersionUID = 1L;
    private String type;     // REGISTER, LOGIN, LOGIN_SUCCESS, LOGIN_FAIL, CHAT_ALL, CHAT_PRIVATE, FILE, IMAGE, UPDATE_USER, STICKER, RECALL_MSG
    private String sender;   
    private String receiver; 
    private String content;  // Chứa password khi đăng nhập/đăng ký hoặc nội dung tin nhắn
    private String fileName; 
    private byte[] fileData; 
    private String timestamp; // Trường lưu thời gian gửi tin nhắn
    private String msgId;     // ID định danh duy nhất cho từng tin nhắn

    public MessagePacket(String type, String sender, String receiver, String content) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.msgId = UUID.randomUUID().toString();
    }

    public MessagePacket(String type, String sender, String receiver, String content, String msgId) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.msgId = (msgId != null && !msgId.isEmpty()) ? msgId : UUID.randomUUID().toString();
    }

    public String getType() { return type; }
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public String getContent() { return content; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }
    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    // Getter và Setter cho thời gian (timestamp)
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    // Getter và Setter cho msgId
    public String getMsgId() {
        if (msgId == null || msgId.isEmpty()) {
            msgId = UUID.randomUUID().toString();
        }
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }
}