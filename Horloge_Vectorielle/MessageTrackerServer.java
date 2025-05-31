import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.List;

public class MessageTrackerServer {
    private final Map<String, List<Event>> processEvents = new ConcurrentHashMap<>();
    private final Set<TrackMessage> messages = new ConcurrentSkipListSet<>(
        Comparator.comparingInt(m -> m.sendVector[Integer.parseInt(m.fromProcess.substring(1))-1])
    );
    private final JFrame frame = new JFrame("Unified Message Tracker - Horloge Vectorielle");
    private final DrawingPanel drawingPanel = new DrawingPanel();

    public static void main(String[] args) {
        new MessageTrackerServer().start();
    }

    private void start() {
        initializeGUI();
        startServer();
    }

    private void initializeGUI() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.add(new JScrollPane(drawingPanel), BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(12349)) {
                System.out.println("Tracker server running on port 12349");
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleClient(Socket socket) {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            Object input = in.readObject();
            try { Thread.sleep(100); } catch (InterruptedException ie) {}
            
            if (input instanceof TrackEvent) {
                TrackEvent event = (TrackEvent)input;
                System.out.println("Received event: " + event.processName + " - " + event.eventName);
                processEvents.computeIfAbsent(event.processName, k -> new ArrayList<>())
                            .add(new Event(event.eventName, event.vectorClock));
            } 
            else if (input instanceof TrackMessage) {
                TrackMessage msg = (TrackMessage)input;
                System.out.println("Received message: " + msg.fromProcess + " -> " + msg.toProcess);
                messages.add(msg);
            }
            drawingPanel.updateSize();
            drawingPanel.repaint();
        } catch (Exception e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }

    private class DrawingPanel extends JPanel {
        private static final int PROCESS_WIDTH = 200;
        private static final int EVENT_HEIGHT = 80;
        private static final int MARGIN = 80;
        private static final int PROCESS_SPACING = 120;
        private static final int EVENT_SPACING = 20;

        public void updateSize() {
            int maxEvents = processEvents.values().stream()
                .mapToInt(List::size).max().orElse(0);
            int width = MARGIN * 2 + processEvents.size() * (PROCESS_WIDTH + PROCESS_SPACING) - PROCESS_SPACING;
            int height = 100 + maxEvents * (EVENT_HEIGHT + EVENT_SPACING);
            setPreferredSize(new Dimension(width, height));
            revalidate();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            List<String> processes = new ArrayList<>(processEvents.keySet());
            Collections.sort(processes);
            
            for (int i = 0; i < processes.size(); i++) {
                String process = processes.get(i);
                int x = MARGIN + i * (PROCESS_WIDTH + PROCESS_SPACING);
                
                // Dessiner la ligne de processus
                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Arial", Font.BOLD, 14));
                g2.drawString(process, x, 30);
                g2.drawLine(x, 40, x, getHeight() - 50);
                
                List<Event> events = processEvents.get(process);
                if (events == null) continue;
                
                for (int j = 0; j < events.size(); j++) {
                    int y = 60 + j * (EVENT_HEIGHT + EVENT_SPACING);
                    
                    // Couleur en fonction du type d'événement
                    Color eventColor = Color.RED;
                    String eventName = events.get(j).name.toLowerCase();
                    
                    if (eventName.contains("send")) {
                        eventColor = Color.GREEN;
                    } else if (eventName.contains("recv") || eventName.contains("reception")) {
                        eventColor = new Color(139, 69, 19); // Marron
                    }
                    
                    // Dessiner le point d'événement
                    g2.setColor(eventColor);
                    g2.fillOval(x - 4, y - 4, 8, 8);
                    
                    // Dessiner le nom de l'événement
                    g2.setColor(Color.BLACK);
                    g2.setFont(new Font("Arial", Font.PLAIN, 12));
                    String[] parts = events.get(j).name.split(" ", 2);
                    if (parts.length > 1) {
                        g2.drawString(parts[0], x + 15, y + 5);
                        g2.drawString(parts[1], x + 15, y + 20);
                    } else {
                        g2.drawString(events.get(j).name, x + 15, y + 5);
                    }
                    
                    // Dessiner l'horloge vectorielle
                    int[] vector = events.get(j).vectorClock;
                    if (vector != null) {
                        g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
                        String vectorStr = Arrays.toString(vector);
                        g2.drawString(vectorStr, x + 15, y + 35);
                    }
                }
            }
            
            // Dessiner les flèches de messages
            g2.setColor(new Color(0, 0, 255, 150));
            for (TrackMessage msg : messages) {
                try {
                    int fromIdx = processes.indexOf(msg.fromProcess);
                    int toIdx = processes.indexOf(msg.toProcess);
                    
                    if (fromIdx != -1 && toIdx != -1) {
                        int fromX = MARGIN + fromIdx * (PROCESS_WIDTH + PROCESS_SPACING);
                        int toX = MARGIN + toIdx * (PROCESS_WIDTH + PROCESS_SPACING);
                        
                        List<Event> fromEvents = processEvents.get(msg.fromProcess);
                        List<Event> toEvents = processEvents.get(msg.toProcess);
                        
                        if (fromEvents != null && toEvents != null) {
                            int fromY = -1, toY = -1;
                            
                            // Trouver l'événement d'envoi
                            for (int i = 0; i < fromEvents.size(); i++) {
                                if (fromEvents.get(i).name.contains(msg.content)) {
                                    fromY = 60 + i * (EVENT_HEIGHT + EVENT_SPACING);
                                    break;
                                }
                            }
                            
                            // Trouver l'événement de réception
                            for (int i = 0; i < toEvents.size(); i++) {
                                if (toEvents.get(i).name.contains(msg.content)) {
                                    toY = 60 + i * (EVENT_HEIGHT + EVENT_SPACING);
                                    break;
                                }
                            }
                            
                            if (fromY != -1 && toY != -1) {
                                // Dessiner la flèche
                                g2.drawLine(fromX, fromY, toX, toY);
                                
                                // Dessiner la pointe de la flèche
                                int arrowSize = 6;
                                if (toX > fromX) {
                                    g2.drawLine(toX, toY, toX - arrowSize, toY - arrowSize);
                                    g2.drawLine(toX, toY, toX - arrowSize, toY + arrowSize);
                                } else {
                                    g2.drawLine(toX, toY, toX + arrowSize, toY - arrowSize);
                                    g2.drawLine(toX, toY, toX + arrowSize, toY + arrowSize);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class Event {
        final String name;
        final int[] vectorClock;
        
        Event(String name, int[] vectorClock) {
            this.name = name;
            this.vectorClock = vectorClock;
        }
    }
}
