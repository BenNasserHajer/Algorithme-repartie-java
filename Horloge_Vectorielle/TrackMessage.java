import java.io.Serializable;

public class TrackMessage implements Serializable {
    String fromProcess;
    String toProcess;
    String content;
    int[] sendVector;
    int[] receiveVector;
    
    public TrackMessage(String fromProcess, String toProcess, String content, 
                       int[] sendVector, int[] receiveVector) {
        this.fromProcess = fromProcess;
        this.toProcess = toProcess;
        this.content = content;
        this.sendVector = sendVector;
        this.receiveVector = receiveVector;
    }
}
