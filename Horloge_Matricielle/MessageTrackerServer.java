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
        Comparator.comparingInt(m -> m.sendClock)
    );
    private final JFrame frame = new JFrame("Unified Message Tracker - Horloge Matricielle");
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
                            .add(new Event(event.eventName, event.clock, event.matrixClock));
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
        private static final int EVENT_HEIGHT = 100;
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
                    
                    // Dessiner l'horloge matricielle
                    int[][] matrix = events.get(j).matrixClock;
                    if (matrix != null) {
                        g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
                        int clockY = y + 35;
                        for (int[] row : matrix) {
                            StringBuilder sb = new StringBuilder("[");
                            for (int k = 0; k < row.length; k++) {
                                sb.append(row[k]);
                                if (k < row.length - 1) sb.append(", ");
                            }
                            sb.append("]");
                            g2.drawString(sb.toString(), x + 15, clockY);
                            clockY += 12;
                        }
                    }
                }
            }
        }
    }

    private static class Event {
        final String name;
        final int clock;
        final int[][] matrixClock;
        
        Event(String name, int clock, int[][] matrixClock) {
            this.name = name;
            this.clock = clock;
            this.matrixClock = matrixClock;
        }
    }
}
