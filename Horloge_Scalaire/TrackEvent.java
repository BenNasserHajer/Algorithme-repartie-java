import java.io.Serializable;

public class TrackEvent implements Serializable {
    String processName;
    String eventName;
    int clock;
    
    public TrackEvent(String processName, String eventName, int clock) {
        this.processName = processName;
        this.eventName = eventName;
        this.clock = clock;
    }
}
