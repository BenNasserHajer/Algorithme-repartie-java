import java.io.Serializable;

public class TrackMessage implements Serializable {
    String fromProcess;
    String toProcess;
    String content;
    int sendClock;
    int receiveClock;
    
    public TrackMessage(String fromProcess, String toProcess, String content, 
                       int sendClock, int receiveClock) {
        this.fromProcess = fromProcess;
        this.toProcess = toProcess;
        this.content = content;
        this.sendClock = sendClock;
        this.receiveClock = receiveClock;
    }
}

    

