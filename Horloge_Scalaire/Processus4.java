import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Processus4{
    private static final int PROCESSUS_ID = 3;
    private static final AtomicInteger horloge = new AtomicInteger(0);
    private static final ReentrantLock horlogeLock = new ReentrantLock();
    private static int compteurEvenements = 0;

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(5004);
            System.out.println("Processus 4 (Scalaire) démarré sur le port 5004");

            Thread recepteur = new Thread(() -> {
                try {
                    while (true) {
                        Socket socket = serverSocket.accept();
                        new Thread(() -> traiterMessage(socket)).start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            recepteur.start();

            if (!attendreSignalDemarrage()) {
                System.out.println("Impossible de se connecter au coordinateur. Arrêt du processus.");
                return;
            }

            evenementLocal("Synchronisation des données avec le cloud");
            Thread.sleep(10000);
            
            envoyerMessage("Message du P4 vers P1: Synchronisation terminée", 5001);
            Thread.sleep(10000);
            
            evenementLocal("Analyse de sécurité du réseau");
            Thread.sleep(10000);

            evenementLocal("Mise à jour des configurations système");
            Thread.sleep(10000);

            envoyerMessage("Message du P4 vers P2: Configurations mises à jour", 5002);
            Thread.sleep(10000);

            evenementLocal("Audit des connexions récentes");
            Thread.sleep(10000);

            evenementLocal("Maintenance préventive des serveurs");
            Thread.sleep(10000);

            envoyerMessage("Message du P4 vers P3: Maintenance effectuée", 5003);
            Thread.sleep(10000);

            Thread.sleep(5000);
            System.out.println("Processus 4 terminé");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void evenementLocal(String description) {
        horlogeLock.lock();
        try {
            horloge.incrementAndGet();
            compteurEvenements++;
             // Modification ici - seulement l'horloge scalaire
             MessageTrackerClient.addEvent("P4", description, horloge.get()); 
            
            System.out.println("[P4] Événement local #" + compteurEvenements + 
                             " (H=" + horloge.get() + "): " + description);
        } finally {
            horlogeLock.unlock();
        }
    }

    private static void envoyerMessage(String message, int port) {
        horlogeLock.lock();
        try {
            horloge.incrementAndGet();
            int horlogeCourante = horloge.get();
             // Ajout du tracking du message
             MessageTrackerClient.addEvent("P4", "send " + message, horlogeCourante);
             MessageTrackerClient.addMessage("P4", "P" + (port-5000), message, horlogeCourante, 0);
            
            new Thread(() -> {
                try {
                    Socket socket = new Socket("localhost", port);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(new MessageData(message, horlogeCourante, PROCESSUS_ID));
                    out.flush();
                    socket.close();
                    
                    System.out.println("[P4] Envoi à P" + (port-5000) + ": " + message + 
                                      " (H=" + horlogeCourante + ")");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } finally {
            horlogeLock.unlock();
        }
    }

    private static void traiterMessage(Socket socket) {
        try {
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            MessageData data = (MessageData) in.readObject();

            horlogeLock.lock();
            try {
                int nouvelleHorloge = Math.max(horloge.get(), data.getHorloge()) + 1;
                horloge.set(nouvelleHorloge);
                 // Ajout du tracking de la réception
            MessageTrackerClient.addEvent("P4", "recv " + data.getMessage(), nouvelleHorloge);
            MessageTrackerClient.addMessage("P" + data.getProcessusId(), "P4", data.getMessage(), data.getHorloge(), nouvelleHorloge);
                System.out.println("[P4] Réception de P" + data.getProcessusId() + ": " + 
                                 data.getMessage() + " (H=" + nouvelleHorloge + ")");
            } finally {
                horlogeLock.unlock();
            }
            
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean attendreSignalDemarrage() {
        try {
            Socket socket = new Socket("localhost", 5000);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            out.println("REGISTER:" + PROCESSUS_ID);
            String response = in.readLine();
            socket.close();
            
            while (!"START".equals(response)) {
                Thread.sleep(1000);
                socket = new Socket("localhost", 5000);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out.println("CHECK_STATUS");
                response = in.readLine();
                socket.close();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}