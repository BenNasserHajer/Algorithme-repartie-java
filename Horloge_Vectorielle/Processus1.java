
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

public class Processus1 {
    private static final int NB_PROCESSUS = 4;
    private static final int PROCESSUS_ID = 0;
    private static int[] horlogeVectorielle = new int[NB_PROCESSUS];
    private static final ReentrantLock horlogeLock = new ReentrantLock();
    private static int compteurEvenements = 0;
    private static final Object affichageLock = new Object();

    public static void main(String[] args) {
        try {
            Arrays.fill(horlogeVectorielle, 0);
            ServerSocket serverSocket = new ServerSocket(5001);
            System.out.println("Processus 1 démarré sur le port 5001");

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void evenementLocal(String description) {
        horlogeLock.lock();
        try {
            horlogeVectorielle[PROCESSUS_ID]++;
            compteurEvenements++;
            
            // Envoi au tracker
            MessageTrackerClient.addEvent("P1", description, horlogeVectorielle);
            
            synchronized (affichageLock) {
                System.out.println("[P1] Événement local #" + compteurEvenements + ": " + description);
                afficherHorloge();
            }
        } finally {
            horlogeLock.unlock();
        }
    }

    private static void envoyerMessage(String message, int port) {
        horlogeLock.lock();
        try {
            horlogeVectorielle[PROCESSUS_ID]++;
            int[] horlogeCopie = Arrays.copyOf(horlogeVectorielle, NB_PROCESSUS);

            // Envoi au tracker avant l'envoi du message
            MessageTrackerClient.addEvent("P1", "send " + message, horlogeVectorielle);

            new Thread(() -> {
                try {
                    Socket socket = new Socket("localhost", port);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(new MessageDataVect(message, horlogeCopie));
                    out.flush();
                    socket.close();
                    
                    synchronized (affichageLock) {
                        System.out.println("[P1] Envoi: " + message);
                        afficherHorloge();
                    }
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
            MessageDataVect data = (MessageDataVect) in.readObject();

            horlogeLock.lock();
            try {
                int[] horlogeRecue = data.getHorloge();
                for (int i = 0; i < NB_PROCESSUS; i++) {
                    horlogeVectorielle[i] = Math.max(horlogeVectorielle[i], horlogeRecue[i]);
                }
                horlogeVectorielle[PROCESSUS_ID]++;
                
                // Envoi au tracker après réception
                MessageTrackerClient.addEvent("P1", "recv " + data.getMessage(), horlogeVectorielle);
                
                synchronized (affichageLock) {
                    System.out.println("[P1] Réception : " + data.getMessage());
                    afficherHorloge();
                }
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

    private static void afficherHorloge() {
        System.out.println("Horloge vectorielle: " + Arrays.toString(horlogeVectorielle));
    }
}


