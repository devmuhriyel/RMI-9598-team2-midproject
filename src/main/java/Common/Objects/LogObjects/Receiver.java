package Common.Objects.LogObjects;

import java.io.Serializable;

public class Receiver implements Serializable {
    String name;

    public Receiver(String name) {
        this.name = name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}
