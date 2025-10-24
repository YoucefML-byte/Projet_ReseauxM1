package serveur;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ServeurTCP {
    public static void main(String[] args) {
        System.out.println("Serveur waiting for client");
        try{


            //Creation d'un socket qui ecoute le port 9999
            ServerSocket serverSocket = new ServerSocket(9999);
            //Si un client fait une demande de connexion
            Socket socket = serverSocket.accept();
            //Ici la connexion avec le client est etablie
            System.out.println("Serveur accepted");

            //On communique avec le client
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            //le flux sortant
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

            //une fois qu'on a terminer de communiquer avec le client on ferme la socket
            socket.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
