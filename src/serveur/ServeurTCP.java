package serveur;

import java.net.ServerSocket;
import java.net.Socket;

public class ServeurTCP {

    public static void main(String[] args) {
        System.out.println("Serveur en attente de clients...");

        try (ServerSocket serverSocket = new ServerSocket(6666)) {

            // Boucle infinie : on accepte plusieurs clients
            while (true) {
                // Bloque jusqu'à ce qu'un client se connect
                Socket socket = serverSocket.accept();
                System.out.println("Client connecté : " + socket.getInetAddress());

                // On délègue la gestion de ce client à un thread
                ClientHandler handler = new ClientHandler(socket);
                Thread t = new Thread(handler);
                t.start(); // lance le thread
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}//