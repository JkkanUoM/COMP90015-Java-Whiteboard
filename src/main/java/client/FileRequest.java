package client;

import java.io.Serializable;

public class FileRequest implements Serializable {
    private String s;

    public FileRequest (String s) {
        this.s = s;
    }

    public String getString() {
        return s;
    }

    public void setS(String s) {
        this.s = s;
    }
}
