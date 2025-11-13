package client;

import etats.ResultatTir;
import joueur.Joueur;
import message.Message;
import message.ServerShotMessage;
import message.ShotRequest;
import message.ShotResponse;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientTCP {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private Joueur joueur; //  le joueur associé à ce client

    public ClientTCP() {
        this("Client", 10); // nom par défaut, grille 10x10
    }

    public ClientTCP(String nomJoueur, int tailleGrille) {
        this.joueur = new Joueur(nomJoueur, tailleGrille);
    }

    public void connecter(String ip, int port) throws IOException {
        socket = new Socket();
        // Timeout de connexion (3s) pour éviter de “pendre”
        socket.connect(new InetSocketAddress(ip, port), 3000);



        in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        System.out.println("Connecté au serveur : " + ip + ":" + port);
    }

    public void envoyer(Message msg) {
        String texte = msg.serialize();  // objet → JSON
        out.println(texte);
        System.out.println(" Envoyé : " + texte);
    }

    public Message recevoir() {
        try {
            String raw = in.readLine();  // une ligne JSON
            if (raw == null) {
                System.out.println(" Connexion fermée par le serveur.");
                return null;
            }
            System.out.println(" Reçu : " + raw);

            try {
                return Message.deserialize(raw);  // JSON → objet
            } catch (RuntimeException parseEx) {
                // Si ce n'est pas un JSON Message valide (ex: {"type":"ERROR"} non géré, ou "bye bye")
                System.out.println(" Réponse non standard: " + raw);
                return null;
            }
        } catch (IOException e) {
            System.err.println("Erreur réception : " + e.getMessage());
            return null;
        }
    }

    public void startMessaging() throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        while (true) {
            System.out.println();
            System.out.println("Commandes :");
            System.out.println("  shoot x y  -> tirer sur l'ennemi");
            System.out.println("  show       -> afficher les grilles");
            System.out.println("  quit       -> quitter");
            System.out.print("> ");
            String input = console.readLine();

            // ctrl+D / ctrl+Z → quitter proprement
            if (input == null) break;

            input = input.trim();

            if (input.equalsIgnoreCase("show")) {
                afficherVueJoueur();
                continue;
            }
            if (input.equalsIgnoreCase("quit")) {
                out.println("QUIT");
                // Lire une éventuelle réponse (“bye bye” JSON/texte), sans bloquer l’utilisateur
                Message maybe = recevoir(); // ok si null
                System.out.println(" Déconnexion...");
                break;
            }

            if (input.toLowerCase().startsWith("shoot")) {
                String[] parts = input.split("\\s+");
                if (parts.length != 3) {
                    System.out.println(" Il faut exactement deux coordonnées: shoot x y");
                    continue;
                }

                int x, y;
                try {
                    x = Integer.parseInt(parts[1]);
                    y = Integer.parseInt(parts[2]);
                } catch (NumberFormatException nfe) {
                    System.out.println(" Les coordonnées doivent être des entiers.");
                    continue;
                }
                if (x < 0 || y < 0) {
                    System.out.println(" Coordonnées négatives interdites.");
                    continue;
                }

                // Envoi du tir
                ShotRequest req = new ShotRequest(x, y);
                envoyer(req);

                // Lecture de la réponse
                // 1) Réponse sur TON tir
                Message msg = recevoir();
                if (msg instanceof ShotResponse res) {
                    System.out.println("Ton tir en (" + x + "," + y + ") : "
                            + res.getResultat()
                            + (res.getNomBateau() != null ? " sur " + res.getNomBateau() : ""));

                    // Mise à jour de la grille de tirs (ennemi)
                    joueur.enregistrerResultatTir(x, y, res.getResultat());
                } else {
                    System.out.println("Réponse inattendue après tir (1) : " + (msg != null ? msg.getType() : "null"));
                }

                // 2) Message sur le tir de l'ENNEMI
                Message msg2 = recevoir();
                if (msg2 instanceof ServerShotMessage sshot) {
                    int ex = sshot.getX();
                    int ey = sshot.getY();

                    System.out.println(">> L'ennemi a tiré en (" + ex + "," + ey + ") : "
                            + sshot.getResultat()
                            + (sshot.getNomBateau() != null ? " sur " + sshot.getNomBateau() : ""));

                    // On applique ce tir sur NOTRE grille perso locale, pour l'affichage
                    joueur.recevoirTir(ex, ey); // appelle grillePerso.tirerSurMoi()

                } else if (msg2 != null) {
                    System.out.println("Réponse inattendue après tir (2) : " + msg2.getType());
                } else {
                    System.out.println("Pas d'info sur le tir de l'ennemi.");
                }

            } else {
                System.out.println(" Commande inconnue. Utilisez : shoot x y ou quit");
            }
        }
    }

    private void phasePlacement() throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("=== PHASE DE PLACEMENT DES BATEAUX ===");
        System.out.println("Tu vas placer : PorteAvion (5), Croiseur (4), ContreTorpilleur (3), Torpilleur (2)");
        System.out.println("Les coordonnées vont de 0 à 9 (pour une grille 10x10).");

        // PorteAvion
        placerUnBateau(console, new bâteaux.PorteAvion(0, 0), "PorteAvion", 5);

        // Croiseur
        placerUnBateau(console, new bâteaux.Croiseur(0, 0), "Croiseur", 4);

        // ContreTorpilleur
        placerUnBateau(console, new bâteaux.ContreTorpilleur(0, 0), "ContreTorpilleur", 3);

        // Torpilleur
        placerUnBateau(console, new bâteaux.Torpilleur(0, 0), "Torpilleur", 2);

        System.out.println("=== Ta grille perso après placement ===");
        joueur.getGrillePerso().afficher();
    }

    private void placerUnBateau(BufferedReader console, bâteaux.Bâteau bateau, String nom, int longueur) throws IOException {
        while (true) {
            System.out.println("\nPlacement du " + nom + " (longueur " + longueur + ")");
            System.out.print("Entrer x y orientation(H/V) (ex: 2 3 H) : ");
            String ligne = console.readLine();
            if (ligne == null) {
                System.out.println("Entrée interrompue, placement annulé.");
                return;
            }
            String[] parts = ligne.trim().split("\\s+");
            if (parts.length != 3) {
                System.out.println("Format invalide. Exemple correct: 2 3 H");
                continue;
            }

            int x, y;
            try {
                x = Integer.parseInt(parts[0]);
                y = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                System.out.println("x et y doivent être des entiers.");
                continue;
            }

            boolean horizontal;
            if (parts[2].equalsIgnoreCase("H")) {
                horizontal = true;
            } else if (parts[2].equalsIgnoreCase("V")) {
                horizontal = false;
            } else {
                System.out.println("Orientation invalide. Utilise H ou V.");
                continue;
            }

            boolean ok = joueur.placerBateau(bateau, x, y, horizontal);
            if (!ok) {
                System.out.println("Impossible de placer ici (débordement ou chevauchement). Essaie ailleurs.");
            } else {
                System.out.println(nom + " placé en (" + x + "," + y + ") " + (horizontal ? "HORIZONTAL" : "VERTICAL"));
                joueur.getGrillePerso().afficher();
                break;
            }
        }
    }

    private void afficherVueJoueur() {
        System.out.println("\n=== VUE DU JOUEUR " + joueur.getNom() + " ===");

        System.out.println("Votre grille (vos bateaux) :");
        joueur.getGrillePerso().afficher();

        System.out.println("\nGrille de tirs (où vous avez tiré sur l'ennemi) :");
        joueur.getGrilleTirs().afficher();

        System.out.println();
    }



    public void close() {
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        if (out != null) out.close();
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public static void main(String[] args) {
        ClientTCP client = new ClientTCP("Client", 10);

        try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            System.out.print("Entrer l’adresse IP du serveur : ");
            String ip = console.readLine();

            System.out.print("Entrer le port du serveur : ");
            int port = Integer.parseInt(console.readLine());

            client.connecter(ip, port);
            // 🔹 On place d'abord les bateaux du joueur
            client.phasePlacement();
            client.startMessaging();
        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
        } finally {
            client.close();
        }
    }
}
