package serveur;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        System.out.println("Thread démarré pour le client : " + socket.getInetAddress());

        try {
            // On communique avec CE client
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            while(true) {
                //on lit ce qu'il y a dans le flux (donc ce que le client à envoyer au serveur)
                String message = in.readLine();

                if(message.trim().equalsIgnoreCase("quit")) {
                    out.println("bye bye");
                    break;
                }

                System.out.println("Voici ce que le client à envoyé : "+message);

                System.out.println("Et voici ma réponse : "+message.toUpperCase());


                //On envoie un message au client

                out.println(message.toUpperCase());

            }

            socket.close();
            System.out.println("Connexion fermée avec : " + socket.getInetAddress());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
