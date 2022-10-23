package client;

import java.io.Serializable;

public class Message implements Serializable {
    private Info info;
    private Drawable drawable;
    private Chat chat;

    public Message(Drawable drawable) {
        this.drawable = drawable;
    }

    public Message(Chat chat) {
        this.chat = chat;
    }

    public Message(Info info) {
        this.info = info;
    }

    public Drawable getDrawable() {
        return drawable;
    }

    public void setDrawable(Drawable drawable) {
        this.drawable = drawable;
    }

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public Info getInfo() {
        return info;
    }

    public void setInfo(Info info) {
        this.info = info;
    }
}
