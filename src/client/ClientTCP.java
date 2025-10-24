package client;

import java.io.*;
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

    public void startMessaging() throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        String msg;

        while (true) {
            System.out.print("Message à envoyer : ");
            msg = console.readLine();

            out.println(msg);

            String response = in.readLine();
            System.out.println("Réponse serveur : " + response);

            if (msg.trim().equalsIgnoreCase("quit")) {
                System.out.println("Déconnexion…");
                break;
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
