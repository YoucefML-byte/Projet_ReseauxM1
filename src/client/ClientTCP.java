package client;

import message.Message;
import message.ShotRequest;
import message.ShotResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientTCP {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public void connecter(String ip, int port) throws IOException {
        socket = new Socket(ip, port);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        System.out.println("Connecté au serveur : " + ip + ":" + port);
    }

    public void envoyer(Message msg) {
        String texte = msg.serialize();  //  objet → chaîne
        out.println(texte);
        System.out.println("📤 Envoyé : " + texte);
    }

    public Message recevoir() {
        try {
            String raw = in.readLine();  //  texte reçu du réseau
            if (raw == null) return null;
            System.out.println("Message reçu du serveur : " + raw);
            return Message.deserialize(raw);  // chaîne → objet
        } catch (IOException e) {
            System.err.println("Erreur réception : " + e.getMessage());
            return null;
        }
    }

    public void startMessaging() throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.print("Commande ('shoot x y' ou 'quit') : ");
            String input = console.readLine();

            // ici l'utilisateur à taper ctrl +z ou ctrl + d donc il veut arrêter la communication'
            if (input == null) {

                break;
            }

            if (input.equalsIgnoreCase("quit")) {
                out.println("QUIT");
                System.out.println(" Déconnexion...");
                break;
            }

            if (input.startsWith("shoot")) {
                String[] parts = input.split(" ");

                if(parts.length != 3) {
                    System.out.println("Il faut exactemment deux coordonnées");
                    continue;
                }
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);

                // Création d’un message objet
                ShotRequest req = new ShotRequest(x, y);
                envoyer(req);

                // Réception du message objet de réponse
                Message msg = recevoir();
                if (msg instanceof ShotResponse res) {
                    System.out.println(" Résultat du tir : " + res.getResultat() + " sur " + res.getNomBateau());
                } else if (msg != null) {
                    System.out.println(" Message reçu de type " + msg.getRequest().toString());
                } else {
                    System.out.println(" Pas de réponse du serveur.");
                }
            } else {
                System.out.println(" Commande inconnue. Utilisez : shoot x y ou quit");
            }
        }
    }

    public void close() throws IOException {
        if (socket != null) socket.close();
    }

    public static void main(String[] args) {
        ClientTCP client = new ClientTCP();

        try {
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

            System.out.print("Entrer l’adresse IP du serveur : ");
            String ip = console.readLine();

            System.out.print("Entrer le port du serveur : ");
            int port = Integer.parseInt(console.readLine());

            client.connecter(ip, port);
            client.startMessaging();
            client.close();

        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }
}