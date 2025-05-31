import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Processus2{
    private static final int PROCESSUS_ID = 1;
    private static final AtomicInteger horloge = new AtomicInteger(0);
    private static final ReentrantLock horlogeLock = new ReentrantLock();
    private static int compteurEvenements = 0;

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(5002);
            System.out.println("Processus 2 (Scalaire) démarré sur le port 5002");

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

            evenementLocal("Analyse des données statistiques");
            Thread.sleep(10000);

            envoyerMessage("Message du P2 vers P1: Résultats d'analyse", 5001);
            Thread.sleep(10000);
            
            evenementLocal("Création d'un index pour la base de données");
            Thread.sleep(10000);
            
            evenementLocal("Optimisation du cache système");
            Thread.sleep(10000);
            
            envoyerMessage("Message du P2 vers P3: Optimisation terminée", 5003);
            Thread.sleep(10000);
            
            evenementLocal("Validation des entrées utilisateur");
            Thread.sleep(10000);
            
            envoyerMessage("Message du P2 vers P1: Archivage terminé", 5001);
            Thread.sleep(10000);

            Thread.sleep(5000);
            System.out.println("Processus 2 terminé");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void evenementLocal(String description) {
        horlogeLock.lock();
        try {
            horloge.incrementAndGet();
            compteurEvenements++;
            MessageTrackerClient.addEvent("P2", description, horloge.get());
            
            System.out.println("[P2] Événement local #" + compteurEvenements + 
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
                MessageTrackerClient.addEvent("P2", "send " + message, horlogeCourante);
                MessageTrackerClient.addMessage("P2", "P" + (port-5000), message, horlogeCourante, 0);
            new Thread(() -> {
                try {
                    Socket socket = new Socket("localhost", port);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(new MessageData(message, horlogeCourante, PROCESSUS_ID));
                    out.flush();
                    socket.close();
                    
                    System.out.println("[P2] Envoi à P" + (port-5000) + ": " + message + 
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
                    MessageTrackerClient.addEvent("P2", "recv " + data.getMessage(), nouvelleHorloge);
                    MessageTrackerClient.addMessage("P" + data.getProcessusId(), "P2", data.getMessage(), data.getHorloge(), nouvelleHorloge);
                        
                System.out.println("[P2] Réception de P" + data.getProcessusId() + ": " + 
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