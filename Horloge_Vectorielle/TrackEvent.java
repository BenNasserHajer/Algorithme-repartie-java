
import java.io.Serializable;
import java.util.Arrays;

public class TrackEvent implements Serializable {
    String processName;
    String eventName;
    int[] vectorClock;
    
    public TrackEvent(String processName, String eventName, int[] vectorClock) {
        this.processName = processName;
        this.eventName = eventName;
        this.vectorClock = Arrays.copyOf(vectorClock, vectorClock.length);
    }
}