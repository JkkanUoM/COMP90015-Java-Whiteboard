package client;

import java.io.Serializable;

public class Info implements Serializable {
    private String username;
    private int action;
    public static final int JOINED = 1;
    public static final int LEFT = 2;
    public static final int IN = 3;

    public Info(String username, int action) {
        this.username = username;
        this.action = action;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }
}
