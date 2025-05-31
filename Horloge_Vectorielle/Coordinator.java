import java.io.*;
import java.net.*;
import java.util.*;

public class Coordinator {
    private static final int PORT_COORDINATEUR = 5000;
    private static final int NB_PROCESSUS = 4;
    
    // Liste des processus enregistrés
    private static Set<Integer> processusEnregistres = new HashSet<>();
    
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT_COORDINATEUR);
            System.out.println("Coordinateur démarré sur le port " + PORT_COORDINATEUR);
            System.out.println("En attente d'enregistrement des " + NB_PROCESSUS + " processus...");
            
            while (true) {
                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                
                // Lecture du message d'enregistrement
                String message = in.readLine();
                
                if (message.startsWith("REGISTER")) {
                    int processId = Integer.parseInt(message.split(":")[1]);
                    processusEnregistres.add(processId);
                    System.out.println("Processus " + (processId + 1) + " enregistré. " + 
                                       processusEnregistres.size() + "/" + NB_PROCESSUS + " processus prêts.");
                    
                    // Si tous les processus sont enregistrés, envoyer le signal de démarrage
                    if (processusEnregistres.size() == NB_PROCESSUS) {
                        System.out.println("Tous les processus sont prêts! Envoi du signal de démarrage...");
                        out.println("START");
                    } else {
                        out.println("WAIT");
                    }
                } else if (message.equals("CHECK_STATUS")) {
                    if (processusEnregistres.size() == NB_PROCESSUS) {
                        out.println("START");
                    } else {
                        out.println("WAIT");
                    }
                }
                
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}