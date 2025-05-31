import java.io.Serializable;
import java.util.Arrays;
class MessageDataVect implements Serializable {
    private final String message;
    private final int[] horloge;

    public MessageDataVect(String message, int[] horloge) {
        this.message = message;
        this.horloge = Arrays.copyOf(horloge, horloge.length);
    }

    public String getMessage() { return message; }
    public int[] getHorloge() { return Arrays.copyOf(horloge, horloge.length); }
}