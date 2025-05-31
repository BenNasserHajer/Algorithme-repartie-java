import java.io.Serializable;

public class TrackEvent implements Serializable {
    String processName;
    String eventName;
    int clock;
    int[][] matrixClock;
    
    public TrackEvent(String processName, String eventName, int clock, int[][] matrixClock) {
        this.processName = processName;
        this.eventName = eventName;
        this.clock = clock;
        this.matrixClock = matrixClock;
    }
}