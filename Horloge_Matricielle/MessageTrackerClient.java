import java.io.*;
import java.net.*;

public class MessageTrackerClient {
    public static void addEvent(String processName, String eventName, 
                              int clock, int[][] matrixClock) {
        try {
            Socket socket = new Socket("localhost", 12349);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(new TrackEvent(processName, eventName, clock, matrixClock));
            out.flush();
            socket.close();
        } catch (IOException e) {
            System.err.println("Error sending event to tracker: " + e.getMessage());
        }
    }

    public static void addMessage(String fromProcess, String toProcess, 
                                String content, int sendClock, int receiveClock) {
        try {
            Socket socket = new Socket("localhost", 12349);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(new TrackMessage(fromProcess, toProcess, content, sendClock, receiveClock));
            out.flush();
            socket.close();
        } catch (IOException e) {
            System.err.println("Error sending message to tracker: " + e.getMessage());
        }
    }
}