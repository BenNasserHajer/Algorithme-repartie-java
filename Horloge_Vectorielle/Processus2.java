
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

public class Processus2 {
    private static final int NB_PROCESSUS = 4;
    private static final int PROCESSUS_ID = 1;
    private static int[] horlogeVectorielle = new int[NB_PROCESSUS];
    private static final ReentrantLock horlogeLock = new ReentrantLock();
    private static int compteurEvenements = 0;
    private static final Object affichageLock = new Object();

    public static void main(String[] args) {
        try {
            Arrays.fill(horlogeVectorielle, 0);
            ServerSocket serverSocket = new ServerSocket(5002);
            System.out.println("Processus 2 démarré sur le port 5002");

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void evenementLocal(String description) {
        horlogeLock.lock();
        try {
            horlogeVectorielle[PROCESSUS_ID]++;
            compteurEvenements++;
            
            MessageTrackerClient.addEvent("P2", description, horlogeVectorielle);
            
            synchronized (affichageLock) {
                System.out.println("[P2] Événement local #" + compteurEvenements + ": " + description);
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

            MessageTrackerClient.addEvent("P2", "send " + message, horlogeVectorielle);

            new Thread(() -> {
                try {
                    Socket socket = new Socket("localhost", port);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(new MessageDataVect(message, horlogeCopie));
                    out.flush();
                    socket.close();
                    
                    synchronized (affichageLock) {
                        System.out.println("[P2] Envoi: " + message);
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
                
                String eventDesc = "recv " + data.getMessage();
                MessageTrackerClient.addEvent("P2", eventDesc, horlogeVectorielle);
                
                synchronized (affichageLock) {
                    System.out.println("[P2] Réception : " + data.getMessage());
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

