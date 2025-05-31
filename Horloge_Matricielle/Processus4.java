import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

public class Processus4 {
    private static final int NB_PROCESSUS = 4;
    private static final int PROCESSUS_ID = 3;
    private static int[][] horlogeMatricielle = new int[NB_PROCESSUS][NB_PROCESSUS];
    private static final ReentrantLock horlogeLock = new ReentrantLock();
    private static int compteurEvenements = 0;
    private static final Object affichageLock = new Object();

    public static void main(String[] args) {
        try {
            for (int i = 0; i < NB_PROCESSUS; i++) {
                Arrays.fill(horlogeMatricielle[i], 0);
            }

            ServerSocket serverSocket = new ServerSocket(5004);
            System.out.println("Processus 4 démarré sur le port 5004");

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
            
            envoyerMessage("Message du P4 vers P1: Synchronisation terminée", 5001, 0);
            Thread.sleep(10000);
            
            evenementLocal("Analyse de sécurité du réseau");
            Thread.sleep(10000);

            evenementLocal("Mise à jour des configurations système");
            Thread.sleep(10000);

            envoyerMessage("Message du P4 vers P2: Configurations mises à jour", 5002, 1);
            Thread.sleep(10000);

            evenementLocal("Audit des connexions récentes");
            Thread.sleep(10000);

            evenementLocal("Maintenance préventive des serveurs");
            Thread.sleep(10000);

            envoyerMessage("Message du P4 vers P3: Maintenance effectuée", 5003, 2);
            Thread.sleep(10000);

            Thread.sleep(5000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void evenementLocal(String description) {
        horlogeLock.lock();
        try {
            horlogeMatricielle[PROCESSUS_ID][PROCESSUS_ID]++;
            compteurEvenements++;
            
            MessageTrackerClient.addEvent("P4", description, horlogeMatricielle[PROCESSUS_ID][PROCESSUS_ID], horlogeMatricielle);
            
            synchronized (affichageLock) {
                System.out.println("[P4] Événement local #" + compteurEvenements + ": " + description);
                afficherHorloge();
            }
        } finally {
            horlogeLock.unlock();
        }
    }

    private static void envoyerMessage(String message, int port, int dest) {
        horlogeLock.lock();
        try {
            horlogeMatricielle[PROCESSUS_ID][PROCESSUS_ID]++;
            horlogeMatricielle[PROCESSUS_ID][dest]++;
            
            MessageTrackerClient.addEvent("P4", "send " + message, horlogeMatricielle[PROCESSUS_ID][PROCESSUS_ID], horlogeMatricielle);
            
            int[][] horlogeCopie = new int[NB_PROCESSUS][NB_PROCESSUS];
            for (int i = 0; i < NB_PROCESSUS; i++) {
                System.arraycopy(horlogeMatricielle[i], 0, horlogeCopie[i], 0, NB_PROCESSUS);
            }

            new Thread(() -> {
                try {
                    Socket socket = new Socket("localhost", port);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(new MessageData(message, horlogeCopie));
                    out.flush();
                    socket.close();
                    
                    synchronized (affichageLock) {
                        System.out.println("[P4] Envoi: " + message + " vers le port " + port);
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
            MessageData data = (MessageData) in.readObject();

            horlogeLock.lock();
            try {
                horlogeMatricielle[PROCESSUS_ID][PROCESSUS_ID]++;
                
                int[][] horlogeRecue = data.getHorloge();
                for (int i = 0; i < NB_PROCESSUS; i++) {
                    for (int j = 0; j < NB_PROCESSUS; j++) {
                        horlogeMatricielle[i][j] = Math.max(horlogeMatricielle[i][j], horlogeRecue[i][j]);
                    }
                }
                
                String eventDesc = "recv " + data.getMessage();
                MessageTrackerClient.addEvent("P4", eventDesc, horlogeMatricielle[PROCESSUS_ID][PROCESSUS_ID], horlogeMatricielle);
                
                synchronized (affichageLock) {
                    System.out.println("[P4] Réception: " + data.getMessage());
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
        StringBuilder sb = new StringBuilder();
        sb.append("Horloge matricielle :\n");
        
        for (int i = 0; i < NB_PROCESSUS; i++) {
            sb.append("[ ");
            for (int j = 0; j < NB_PROCESSUS; j++) {
                sb.append(horlogeMatricielle[i][j]);
                if (j < NB_PROCESSUS - 1) sb.append(", ");
            }
            sb.append(" ]\n");
        }
        
        System.out.println(sb.toString());
    }
}