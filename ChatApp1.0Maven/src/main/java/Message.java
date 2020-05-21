import com.google.gson.Gson;

public class Message {

    String sender;
    String receiver;
    String content;

    private static Gson gson = new Gson();

    public Message(String sender, String receiver, String content) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
    }

//    public Message(String sender, String content) {
//        this.sender = sender;
//        this.content = content;
//        this.receiver = null;
//    }

    String getSender() {
        return sender;
    }

    private void setSender(String sender) {
        this.sender = sender;
    }

    String getReceiver() {
        return receiver;
    }

    private void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    String getContent() {
        return content;
    }

    private void setContent(String content) {
        this.content = content;
    }

    String encode() {
        String json = gson.toJson(this);
        return json;
    }

    static Message decode(String msg) {
        if(msg == null) return null;
        Message message = gson.fromJson(msg, Message.class);
        return message;
    }
}
