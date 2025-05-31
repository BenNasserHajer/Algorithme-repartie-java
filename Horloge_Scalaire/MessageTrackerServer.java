import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.List;
import java.awt.geom.AffineTransform;


public class MessageTrackerServer {
    private final Map<String, List<Event>> processEvents = new ConcurrentHashMap<>();
    private final Set<TrackMessage> messages = new ConcurrentSkipListSet<>(
        Comparator.comparingInt(m -> m.sendClock)
    );
    private final JFrame frame = new JFrame("Message Tracker - Horloge Scalaire");
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
                            .add(new Event(event.eventName, event.clock));
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
        private static final int EVENT_HEIGHT = 60;
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
                    
                    // Dessiner l'horloge scalaire
                    g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
                    g2.drawString("H=" + events.get(j).clock, x + 15, y + 35);
                }
            }
            
            // Dessiner les flèches de messages
            g2.setColor(Color.BLUE);
            for (TrackMessage msg : messages) {
                int fromIndex = processes.indexOf(msg.fromProcess);
                int toIndex = processes.indexOf(msg.toProcess);
                
                if (fromIndex != -1 && toIndex != -1) {
                    int fromX = MARGIN + fromIndex * (PROCESS_WIDTH + PROCESS_SPACING);
                    int toX = MARGIN + toIndex * (PROCESS_WIDTH + PROCESS_SPACING);
                    
                    // Trouver les positions Y des événements correspondants
                    int sendY = findEventY(msg.fromProcess, "send", msg.sendClock);
                    int receiveY = findEventY(msg.toProcess, "recv", msg.receiveClock);
                    
                    if (sendY != -1 && receiveY != -1) {
                        g2.drawLine(fromX, sendY, toX, receiveY);
                        // Dessiner une flèche
                        drawArrow(g2, fromX, sendY, toX, receiveY);
                    }
                }
            }
        }
        
        private int findEventY(String process, String type, int clock) {
            List<Event> events = processEvents.get(process);
            if (events == null) return -1;
            
            for (int i = 0; i < events.size(); i++) {
                Event e = events.get(i);
                if (e.clock == clock && e.name.toLowerCase().contains(type)) {
                    return 60 + i * (EVENT_HEIGHT + EVENT_SPACING);
                }
            }
            return -1;
        }
        
        private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2) {
            double dx = x2 - x1, dy = y2 - y1;
            double angle = Math.atan2(dy, dx);
            int len = (int) Math.sqrt(dx*dx + dy*dy);
            
            Polygon arrowHead = new Polygon();  
            arrowHead.addPoint(0, 0);
            arrowHead.addPoint(-5, -10);
            arrowHead.addPoint(5, -10);
            
            AffineTransform tx = new AffineTransform();
            tx.translate(x2, y2);
            tx.rotate(angle - Math.PI/2);
            
            g2.setStroke(new BasicStroke(1));
            g2.fill(tx.createTransformedShape(arrowHead));
        }
    }

    private static class Event {
        final String name;
        final int clock;
        
        Event(String name, int clock) {
            this.name = name;
            this.clock = clock;
        }
    }
} 
