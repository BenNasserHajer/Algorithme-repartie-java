import java.io.Serializable;

public class MessageData implements Serializable {
    private final String message;
    private final int[][] horloge;

    public MessageData(String message, int[][] horloge) {
        this.message = message;
        this.horloge = horloge;
    }

    public String getMessage() {
        return message;
    }

    public int[][] getHorloge() {
        return horloge;
    }
}