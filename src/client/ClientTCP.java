package client;

import etats.ResultatTir;
import etats.Orientation;
import etats.ShipType;
import joueur.Joueur;
import message.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientTCP {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private Joueur joueur;

    public ClientTCP() {
        this("Client", 10);
    }

    public ClientTCP(String nomJoueur, int tailleGrille) {
        this.joueur = new Joueur(nomJoueur, tailleGrille);
    }

    public void connecter(String ip, int port) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(ip, port), 3000);

        in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        System.out.println("Connecté au serveur : " + ip + ":" + port);
    }

    public void envoyer(Message msg) {
        String texte = msg.serialize();
        out.println(texte);
    }

    public Message recevoir() {
        try {
            String raw = in.readLine();
            if (raw == null) {
                System.out.println("Connexion fermée par le serveur.");
                return null;
            }

            try {
                return Message.deserialize(raw);
            } catch (RuntimeException parseEx) {
                System.out.println("Réponse non standard: " + raw);
                return null;
            }
        } catch (IOException e) {
            System.err.println("Erreur réception : " + e.getMessage());
            return null;
        }
    }

    public void startMessaging() throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        boolean monTour = true; // Le client commence

        while (true) {
            System.out.println();

            if (monTour) {
                System.out.println("🎯 C'EST VOTRE TOUR !");
                System.out.println("Commandes :");
                System.out.println("  shoot x y  -> tirer sur l'ennemi");
                System.out.println("  show       -> afficher les grilles");
                System.out.println("  quit       -> quitter");
                System.out.print("> ");
            } else {
                System.out.println("⏳ Tour de l'adversaire... Appuyez sur Entrée pour continuer");
                console.readLine();
            }

            String input = console.readLine();

            if (input == null) break;

            input = input.trim();

            if (input.equalsIgnoreCase("show")) {
                afficherVueJoueur();
                continue;
            }
            if (input.equalsIgnoreCase("quit")) {
                out.println("QUIT");
                Message maybe = recevoir();
                System.out.println("Déconnexion...");
                break;
            }

            if (input.toLowerCase().startsWith("shoot")) {
                if (!monTour) {
                    System.out.println("❌ Ce n'est pas votre tour !");
                    continue;
                }

                String[] parts = input.split("\\s+");
                if (parts.length != 3) {
                    System.out.println("Il faut exactement deux coordonnées: shoot X Y (X=horizontal, Y=vertical, de 1 à 10)");
                    continue;
                }

                int x, y;
                try {
                    // X = horizontal (colonne), Y = vertical (ligne)
                    x = Integer.parseInt(parts[1]) - 1;
                    y = Integer.parseInt(parts[2]) - 1;
                } catch (NumberFormatException nfe) {
                    System.out.println("Les coordonnées doivent être des entiers.");
                    continue;
                }
                if (x < 0 || x > 9 || y < 0 || y > 9) {
                    System.out.println("Les coordonnées doivent être entre 1 et 10.");
                    continue;
                }

                // Envoi du tir
                ShotRequest req = new ShotRequest(x, y);
                envoyer(req);

                // 1) Réponse sur TON tir
                Message msg = recevoir();
                if (msg instanceof ShotResponse res) {
                    ResultatTir resultat = res.getResultat();

                    System.out.println("Ton tir en X=" + (x+1) + " Y=" + (y+1) + " : "
                            + resultat
                            + (res.getNomBateau() != null ? " sur " + res.getNomBateau() : ""));

                    joueur.enregistrerResultatTir(x, y, resultat);

                    // 🔥 NOUVEAU : Vérifier si on rejoue
                    if (resultat == ResultatTir.HIT || resultat == ResultatTir.SUNK) {
                        System.out.println("✨ TOUCHÉ ! Vous rejouez !");
                        monTour = true; // On garde son tour
                    } else {
                        System.out.println("💧 Raté... C'est au tour de l'adversaire.");
                        monTour = false; // On passe la main
                    }
                } else {
                    System.out.println("Réponse inattendue après tir (1) : " + (msg != null ? msg.getType() : "null"));
                }

                // 🔥 TOUJOURS recevoir le message SERVER_SHOT (même si coordonnées = -1,-1)
                Message msg2 = recevoir();
                if (!(msg2 instanceof ServerShotMessage)) {
                    System.out.println("Erreur: devrait recevoir SERVER_SHOT");
                    continue;
                }

                ServerShotMessage sshot = (ServerShotMessage) msg2;

                // ✅ 🔥 VÉRIFIER SI VOUS AVEZ GAGNÉ (avant de traiter les tirs du serveur)
                if (sshot.isGameOver() && sshot.getWinner() != null && !sshot.getWinner().equals("SERVER")) {
                    System.out.println("\n════════════════════════════════════");
                    System.out.println("         FIN DE LA PARTIE");
                    System.out.println("════════════════════════════════════");
                    System.out.println("🎉 FÉLICITATIONS " + sshot.getWinner().toUpperCase() + " ! VOUS AVEZ GAGNÉ ! 🎉");
                    System.out.println("════════════════════════════════════\n");

                    // Proposer de rejouer
                    if (!proposerRejouer(console)) {
                        return;
                    }
                    monTour = true;
                    continue; // Recommencer la boucle de jeu
                }

                // 2) Traiter le(s) message(s) SERVER_SHOT


                // Si x >= 0, c'est un vrai tir du serveur
                if (sshot.getX() >= 0 && sshot.getY() >= 0) {
                    // Boucle pour gérer les tirs consécutifs de l'adversaire
                    while (true) {
                        int ex = sshot.getX();
                        int ey = sshot.getY();

                        ResultatTir resAdversaire = sshot.getResultat();
                        System.out.println("\n>> L'ennemi a tiré en (" + ex + "," + ey + ") : "
                                + resAdversaire
                                + (sshot.getNomBateau() != null ? " sur " + sshot.getNomBateau() : ""));
                        joueur.recevoirTir(ex, ey);

                        // ✅ Vérifier game over IMMÉDIATEMENT
                        if (sshot.isGameOver()) {
                            System.out.println("\n════════════════════════════════════");
                            System.out.println("         FIN DE LA PARTIE");
                            System.out.println("════════════════════════════════════");

                            if ("SERVER".equals(sshot.getWinner())) {
                                System.out.println("😞 LE SERVEUR A GAGNÉ... VOUS AVEZ PERDU 😞");
                            } else {
                                System.out.println("🎉 FÉLICITATIONS " + sshot.getWinner().toUpperCase() + " ! VOUS AVEZ GAGNÉ ! 🎉");
                            }
                            System.out.println("════════════════════════════════════\n");

                            // Proposer de rejouer
                            if (!proposerRejouer(console)) {
                                return;
                            }
                            monTour = true;
                            break; // Sortir de la boucle
                        }

                        // Vérifier si l'adversaire rejoue
                        if (resAdversaire == ResultatTir.HIT || resAdversaire == ResultatTir.SUNK) {
                            System.out.println("💥 L'adversaire a touché ! Il tire encore...");

                            // Attendre le prochain tir du serveur
                            Message nextMsg = recevoir();
                            if (nextMsg instanceof ServerShotMessage) {
                                sshot = (ServerShotMessage) nextMsg;
                            } else {
                                System.out.println("Erreur: SERVER_SHOT attendu");
                                break;
                            }

                        } else {
                            System.out.println("🎯 L'adversaire a raté ! C'est à votre tour !");
                            monTour = true;
                            break; // Sortir de la boucle
                        }
                    }
                } else {
                    // Le serveur n'a pas tiré (x = -1), c'est normal car vous avez touché
                    // Vous gardez votre tour (déjà défini plus haut avec monTour = true)
                }

            } else {
                System.out.println("Commande inconnue. Utilisez : shoot x y, show, ou quit");
            }
        }
    }

    /**
     * Propose au joueur de rejouer ou de quitter
     * @return true si le joueur veut rejouer, false s'il veut quitter
     */
    private boolean proposerRejouer(BufferedReader console) throws IOException {
        while (true) {
            System.out.print("Voulez-vous rejouer ? (o/n) : ");
            String rep = console.readLine();

            if (rep == null) {
                out.println("QUIT");
                return false;
            }

            rep = rep.trim().toLowerCase();

            if (rep.equals("o") || rep.equals("oui") || rep.equals("y") || rep.equals("yes")) {
                System.out.println("\n🔄 Préparation d'une nouvelle partie...");

                NewGameRequest ng = new NewGameRequest();
                envoyer(ng);

                Message ack = recevoir();
                if (ack != null) {
                    System.out.println("✓ Serveur : nouvelle partie prête.");
                }

                this.joueur = new Joueur("Client", 10);
                phasePlacement();

                System.out.println("\n🎮 La nouvelle partie commence !\n");

                return true;

            } else if (rep.equals("n") || rep.equals("non") || rep.equals("no")) {
                System.out.println("\n👋 Merci d'avoir joué ! À bientôt !");
                out.println("QUIT");
                return false;

            } else {
                System.out.println("⚠️  Réponse invalide. Veuillez taper 'o' pour rejouer ou 'n' pour quitter.");
            }
        }
    }

    private void phasePlacement() throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("     PHASE DE PLACEMENT DES BATEAUX");
        System.out.println("═══════════════════════════════════════════");
        System.out.println("Tu vas placer : PorteAvion (5), Croiseur (4),");
        System.out.println("                ContreTorpilleur (3), Torpilleur (2)");
        System.out.println("Les coordonnées vont de 1 à 10 (grille 10x10).");
        System.out.println("═══════════════════════════════════════════\n");

        placerUnBateau(console, new bâteaux.PorteAvion(), "PorteAvion", 5, ShipType.PORTE_AVION);
        placerUnBateau(console, new bâteaux.Croiseur(), "Croiseur", 4, ShipType.CROISEUR);
        placerUnBateau(console, new bâteaux.ContreTorpilleur(), "ContreTorpilleur", 3, ShipType.CONTRE_TORPILLEUR);
        placerUnBateau(console, new bâteaux.Torpilleur(), "Torpilleur", 2, ShipType.TORPILLEUR);

        System.out.println("\n✓ Tous les bateaux sont placés et envoyés au serveur !");
        System.out.println("\n═══ Ta grille personnelle ═══");
        joueur.getGrillePerso().afficher();
        System.out.println("═══════════════════════════════\n");
    }

    private void placerUnBateau(BufferedReader console, bâteaux.Bâteau bateau, String nom, int longueur, ShipType shipType) throws IOException {
        while (true) {
            System.out.println("\n📍 Placement du " + nom + " (longueur " + longueur + ")");
            System.out.print("Entrer X(vertical) Y(horizontal) orientation (ex: 2 3 H) : ");
            String ligne = console.readLine();
            if (ligne == null) {
                System.out.println("Entrée interrompue, placement annulé.");
                return;
            }
            String[] parts = ligne.trim().split("\\s+");
            if (parts.length != 3) {
                System.out.println("❌ Format invalide. Exemple correct: 2 3 H");
                continue;
            }

            int x, y;
            try {
                // L'utilisateur entre X (horizontal) Y (vertical)
                x = Integer.parseInt(parts[0]) - 1;  // horizontal (colonne)
                y = Integer.parseInt(parts[1]) - 1;  // vertical (ligne)
            } catch (NumberFormatException e) {
                System.out.println("❌ Les coordonnées doivent être des entiers.");
                continue;
            }

            if (x < 0 || x > 9 || y < 0 || y > 9) {
                System.out.println("❌ Les coordonnées doivent être entre 1 et 10.");
                continue;
            }

            boolean horizontal;
            Orientation orientation;
            if (parts[2].equalsIgnoreCase("H")) {
                horizontal = true;
                orientation = Orientation.HORIZONTAL;
            } else if (parts[2].equalsIgnoreCase("V")) {
                horizontal = false;
                orientation = Orientation.VERTICAL;
            } else {
                System.out.println("❌ Orientation invalide. Utilise H ou V.");
                continue;
            }

            // 🔥 INVERSER : le tableau est [colonne][ligne] donc on passe (x, y)
            // x = colonne (horizontal), y = ligne (vertical)
            boolean ok = joueur.placerBateau(bateau, x, y, horizontal);
            if (!ok) {
                System.out.println("❌ Impossible de placer ici (débordement ou chevauchement). Essaie ailleurs.");
            } else {
                System.out.println("✓ " + nom + " placé en X=" + (x+1) + " Y=" + (y+1) + " " + (horizontal ? "HORIZONTAL" : "VERTICAL"));

                PlaceShipRequest placeMsg = new PlaceShipRequest(shipType, x, y, orientation);
                envoyer(placeMsg);

                Message response = recevoir();
                System.out.println("   → Serveur a confirmé le placement");

                joueur.getGrillePerso().afficher();
                break;
            }
        }
    }

    private void afficherVueJoueur() {
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("     VUE DU JOUEUR " + joueur.getNom());
        System.out.println("═══════════════════════════════════════");

        System.out.println("\n📋 Votre grille (vos bateaux) :");
        joueur.getGrillePerso().afficher();

        System.out.println("\n🎯 Grille de tirs (où vous avez tiré sur l'ennemi) :");
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
            System.out.println("═══════════════════════════════════════");
            System.out.println("        JEU DE BATAILLE NAVALE");
            System.out.println("═══════════════════════════════════════\n");

            System.out.print("Entrer l'adresse IP du serveur : ");
            String ip = console.readLine();

            System.out.print("Entrer le port du serveur : ");
            int port = Integer.parseInt(console.readLine());

            client.connecter(ip, port);

            // 🔥 NOUVEAU : Demander le pseudo
            System.out.print("\nEntrez votre pseudo : ");
            String pseudo = console.readLine();
            if (pseudo == null || pseudo.trim().isEmpty()) {
                pseudo = "Joueur";
            }
            pseudo = pseudo.trim();

            // Envoyer le pseudo au serveur
            SetUsernameRequest usernameMsg = new SetUsernameRequest(pseudo);
            client.envoyer(usernameMsg);

            // Attendre la confirmation
            Message response = client.recevoir();
            System.out.println("✓ Bienvenue " + pseudo + " !\n");

            // Mettre à jour le joueur avec le bon pseudo
            client.joueur = new Joueur(pseudo, 10);

            // Placement initial des bateaux
            client.phasePlacement();

            // Démarrer la boucle de jeu
            client.startMessaging();

        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
            e.printStackTrace();
        } finally {
            client.close();
        }
    }
}