import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Processus1{
    private static final int PROCESSUS_ID = 0;
    private static final AtomicInteger horloge = new AtomicInteger(0);
    private static final ReentrantLock horlogeLock = new ReentrantLock();
    private static int compteurEvenements = 0;

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(5001);
            System.out.println("Processus 1 (Scalaire) démarré sur le port 5001");

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

            evenementLocal("Calcul d'une moyenne de plusieurs valeurs");
            Thread.sleep(10000);

            envoyerMessage("Message du P1 vers P2: Données initiales", 5002);
            Thread.sleep(10000);
            
            evenementLocal("Mise à jour d'une base de données de clients");
            Thread.sleep(10000);

            envoyerMessage("Message du P1 vers P3: Rapport généré", 5003);
            Thread.sleep(10000);
            
            evenementLocal("Génération d'un rapport financier");
            Thread.sleep(10000);
            
            envoyerMessage("Message du P1 vers P4: Notification de sauvegarde", 5004);
            Thread.sleep(10000);
            
            evenementLocal("Vérification d'intégrité des données");
            Thread.sleep(10000);

            Thread.sleep(5000);
            System.out.println("Processus 1 terminé");
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
            MessageTrackerClient.addEvent("P1", description, horloge.get());            
            System.out.println("[P1] Événement local #" + compteurEvenements + 
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
            MessageTrackerClient.addEvent("P1", "send " + message, horlogeCourante);
            MessageTrackerClient.addMessage("P1", "P" + (port-5000), message, horlogeCourante, 0);
                
            new Thread(() -> {
                try {
                    Socket socket = new Socket("localhost", port);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(new MessageData(message, horlogeCourante, PROCESSUS_ID));
                    out.flush();
                    socket.close();
                    
                    System.out.println("[P1] Envoi à P" + (port-5000) + ": " + message + 
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

            MessageTrackerClient.addEvent("P1", "recv " + data.getMessage(), nouvelleHorloge);
            MessageTrackerClient.addMessage("P" + data.getProcessusId(), "P1", data.getMessage(), data.getHorloge(), nouvelleHorloge);
            System.out.println("[P1] Réception de P" + data.getProcessusId() + ": " + data.getMessage() + " (H=" + nouvelleHorloge + ")");
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