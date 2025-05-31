import java.io.Serializable;

public class MessageData implements Serializable {
    private final String message;
    private final int horloge;
    private final int processusId;

    public MessageData(String message, int horloge, int processusId) {
        this.message = message;
        this.horloge = horloge;
        this.processusId = processusId + 1;
    }

    public String getMessage() { return message; }
    public int getHorloge() { return horloge; }
    public int getProcessusId() { return processusId; }
}
