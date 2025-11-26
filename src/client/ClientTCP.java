package client;

import bâteaux.*;
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

    private Socket socket;// la socket de communication
    private BufferedReader in; // le flux d'entrée
    private PrintWriter out;// le flux de sortie

    private Joueur joueur;//le joueur représenter par ce clientTCP
    private boolean isPvPMode = false;
    private String opponentName = null;

    public ClientTCP() {
        this("Client", 10);
    }

    public ClientTCP(String nomJoueur, int tailleGrille) {
        this.joueur = new Joueur(nomJoueur, tailleGrille);
    }

  //----------------------------------------------------------------------------------------------------------

    /**
     * Cette fonction permet de faire une demande de connexion au serveur qui comme adresse ip ip est port comme port
     * */
    public void connecter(String ip, int port) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(ip, port), 3000);

        in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        System.out.println("Connecté au serveur : " + ip + ":" + port);
    }

    /**
     * Cette fonction sert envoyer des message vers le serveur en encaplsulant le message sous fomre de chaine de carcteres
     * */

    public void envoyer(Message msg) {
        String texte = msg.serialize();
        out.println(texte);
    }

    /**
     * cette fonction sert à gerer la reception des message du serveur
     * */
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


    /**
     * Cette fonction permet de gerer le dialogue entre le joueur et le serveur tout au long de la session
     * */
    public void startMessaging() throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        boolean monTour = true;

        while (true) {
            System.out.println();

            if (monTour) {
                System.out.println("🎯 C'EST VOTRE TOUR !");
                System.out.println("Commandes :");
                System.out.println("  shoot x y  -> tirer");
                System.out.println("  show       -> afficher les grilles");
                System.out.println("  quit       -> quitter");
                System.out.print("> ");
            } else {
                if (isPvPMode) {
                    System.out.println("⏳ Tour de " + opponentName + "... Attente...");
                } else {
                    System.out.println("⏳ Tour de l'adversaire...");
                }
            }

            // En mode PvP, si ce n'est pas notre tour, on attend un message
            if (isPvPMode && !monTour) {
                Message msg = recevoir();
                if (msg instanceof OpponentShotMessage opShot) {
                    handleOpponentShot(opShot);
                    monTour = opShot.isYourTurn();

                    if (opShot.isGameOver()) {
                        handleGameOver(opShot.getWinner());
                        if (!proposerRejouer(console)) {
                            return;
                        }
                        monTour = true;
                    }
                }
                continue;
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
                break;
            }

            if (input.toLowerCase().startsWith("shoot")) {
                if (!monTour) {
                    System.out.println("❌ Ce n'est pas votre tour !");
                    continue;
                }

                String[] parts = input.split("\\s+");
                if (parts.length != 3) {
                    System.out.println("Format: shoot X Y");
                    continue;
                }

                int x, y;
                try {
                    x = Integer.parseInt(parts[1]) - 1;
                    y = Integer.parseInt(parts[2]) - 1;
                } catch (NumberFormatException nfe) {
                    System.out.println("Coordonnées invalides");
                    continue;
                }

                if (x < 0 || x > 9 || y < 0 || y > 9) {
                    System.out.println("Les coordonnées doivent être entre 1 et 10.");
                    continue;
                }

                ShotRequest req = new ShotRequest(x, y);
                envoyer(req);

                // ========================================
                // GESTION DES RÉPONSES : MODE PVP vs BOT
                // ========================================

                if (isPvPMode) {
                    // ===== MODE PVP =====
                    // 1) Recevoir ShotResponse
                    Message msg = recevoir();
                    if (msg instanceof ShotResponse res) {
                        ResultatTir resultat = res.getResultat();
                        System.out.println("Ton tir en (" + (x+1) + "," + (y+1) + ") : "
                                + resultat + (res.getNomBateau() != null ? " sur " + res.getNomBateau() : ""));

                        joueur.enregistrerResultatTir(x, y, resultat);

                        // 2) Recevoir OpponentShotMessage
                        Message opMsg = recevoir();
                        if (opMsg instanceof OpponentShotMessage opShot) {
                            if (opShot.isGameOver()) {
                                handleGameOver(opShot.getWinner());
                                if (!proposerRejouer(console)) {
                                    return;
                                }
                                monTour = true;
                            } else {
                                monTour = !opShot.isYourTurn();
                                if (resultat == ResultatTir.HIT || resultat == ResultatTir.SUNK) {
                                    System.out.println("✨ TOUCHÉ ! Vous rejouez !");
                                } else {
                                    System.out.println("💧 Raté... Tour de " + opponentName);
                                }
                            }
                        }
                    }

                } else {
                    // ===== MODE BOT =====
                    // 1) Recevoir ShotResponse (résultat du tir client)
                    Message msg = recevoir();

                    while (msg != null && !(msg instanceof ShotResponse)) {
                        System.out.println("[Debug] Message ignoré: " + msg.getClass().getSimpleName());
                        msg = recevoir();
                    }

                    if (!(msg instanceof ShotResponse)) {
                        System.out.println("Erreur: ShotResponse attendu, reçu: " + (msg != null ? msg.getType() : "null"));
                        continue;
                    }

                    ShotResponse res = (ShotResponse) msg;
                    ResultatTir resultat = res.getResultat();

                    System.out.println("Ton tir en (" + (x+1) + "," + (y+1) + ") : "
                            + resultat + (res.getNomBateau() != null ? " sur " + res.getNomBateau() : ""));

                    joueur.enregistrerResultatTir(x, y, resultat);

                    // Vérifier si on rejoue
                    if (resultat == ResultatTir.HIT || resultat == ResultatTir.SUNK) {
                        System.out.println("✨ TOUCHÉ ! Vous rejouez !");
                        monTour = true;
                    } else {
                        System.out.println("💧 Raté... C'est au tour de l'adversaire.");
                        monTour = false;
                    }

                    // 2) Recevoir ServerShotMessage
                    Message msg2 = recevoir();
                    if (!(msg2 instanceof ServerShotMessage)) {
                        System.out.println("Erreur: ServerShotMessage attendu, reçu: " + (msg2 != null ? msg2.getType() : "null"));
                        continue;
                    }

                    ServerShotMessage sshot = (ServerShotMessage) msg2;

                    // Vérifier si le joueur a gagné
                    if (sshot.isGameOver() && sshot.getWinner() != null && !sshot.getWinner().equals("SERVER")) {
                        System.out.println("\n╔═══════════════════════════════════╗");
                        System.out.println("         FIN DE LA PARTIE");
                        System.out.println("╚═══════════════════════════════════╝");
                        System.out.println("🎉 FÉLICITATIONS " + sshot.getWinner().toUpperCase() + " ! VOUS AVEZ GAGNÉ ! 🎉");
                        System.out.println("╚═══════════════════════════════════╝\n");

                        if (!proposerRejouer(console)) {
                            return;
                        }
                        monTour = true;
                        continue;
                    }

                    // Traiter les tirs du serveur
                    if (sshot.getX() >= 0 && sshot.getY() >= 0) {
                        // Boucle pour gérer les tirs consécutifs de l'adversaire
                        while (true) {
                            int ex = sshot.getX();
                            int ey = sshot.getY();

                            ResultatTir resAdversaire = sshot.getResultat();
                            System.out.println("\n>> L'ennemi a tiré en (" + (ex+1) + "," + (ey+1) + ") : "
                                    + resAdversaire
                                    + (sshot.getNomBateau() != null ? " sur " + sshot.getNomBateau() : ""));
                            joueur.recevoirTir(ex, ey);

                            // Vérifier si la partie est terminée
                            if (sshot.isGameOver()) {
                                System.out.println("\n╔═══════════════════════════════════╗");
                                System.out.println("         FIN DE LA PARTIE");
                                System.out.println("╚═══════════════════════════════════╝");

                                if ("SERVER".equals(sshot.getWinner())) {
                                    System.out.println("😞 LE SERVEUR A GAGNÉ... VOUS AVEZ PERDU 😞");
                                } else {
                                    System.out.println("🎉 FÉLICITATIONS " + sshot.getWinner().toUpperCase() + " ! VOUS AVEZ GAGNÉ ! 🎉");
                                }
                                System.out.println("╚═══════════════════════════════════╝\n");

                                if (!proposerRejouer(console)) {
                                    return;
                                }
                                monTour = true;
                                break;
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
                                break;
                            }
                        }
                    }
                    // else: le serveur n'a pas tiré (x=-1), vous gardez votre tour
                }
            }
        }
    }

    private void handleGameOver(String winner) {
        System.out.println("\n╔═══════════════════════════════════╗");
        System.out.println("         FIN DE LA PARTIE");
        System.out.println("╚═══════════════════════════════════╝");

        if (winner.equals(joueur.getNom())) {
            System.out.println("🎉 FÉLICITATIONS ! VOUS AVEZ GAGNÉ ! 🎉");
        } else {
            System.out.println("😞 " + winner.toUpperCase() + " A GAGNÉ... 😞");
        }
        System.out.println("╚═══════════════════════════════════╝\n");
    }

    private void handleOpponentShot(OpponentShotMessage opShot) {
        if (opShot.getX() >= 0 && opShot.getY() >= 0) {
            ResultatTir res = opShot.getResultat();
            System.out.println("\n>> " + opponentName + " a tiré en ("
                    + (opShot.getX()+1) + "," + (opShot.getY()+1) + ") : " + res
                    + (opShot.getNomBateau() != null ? " sur " + opShot.getNomBateau() : ""));

            joueur.recevoirTir(opShot.getX(), opShot.getY());
        }
    }

    /**
     * Propose au joueur de rejouer ou de quitter aprés la fin d'une partie
     * @return true si le joueur veut rejouer, false s'il veut quitter
     */
    /**
     * Propose au joueur de rejouer ou de quitter après la fin d'une partie
     * @return true si le joueur veut rejouer, false s'il veut quitter
     */
    private boolean proposerRejouer(BufferedReader console) throws IOException {
        while (true) {
            System.out.print("\nVoulez-vous rejouer ? (o/n) : ");
            String rep = console.readLine();

            if (rep == null) {
                out.println("QUIT");
                return false;
            }

            rep = rep.trim().toLowerCase();

            if (rep.equals("o") || rep.equals("oui") || rep.equals("y") || rep.equals("yes")) {

                if (isPvPMode) {
                    // === MODE PVP ===
                    System.out.println("\n💭 Voulez-vous :");
                    System.out.println("1. Rejouer contre " + opponentName + " (PvP)");
                    System.out.println("2. Rejouer contre le BOT");
                    System.out.println("3. Chercher un nouvel adversaire (PvP)");
                    System.out.print("Votre choix : ");

                    String choixStr = console.readLine();
                    int choix = 1;
                    try {
                        choix = Integer.parseInt(choixStr.trim());
                    } catch (NumberFormatException e) {
                        choix = 1;
                    }

                    if (choix == 2) {
                        // BOT
                        System.out.println("\n🔄 Changement de mode vers BOT...");
                        isPvPMode = false;
                        opponentName = null;

                        GameModeRequest modeReq = new GameModeRequest("BOT");
                        envoyer(modeReq);

                        Message botResponse = recevoir();
                        System.out.println("✓ Mode BOT activé\n");

                        demarrerNouvellePartie(console);
                        return true; // 🔥 RETOURNER ICI

                    } else if (choix == 3) {
                        // Nouvel adversaire PvP
                        System.out.println("\n🔄 Recherche d'un nouvel adversaire...");
                        opponentName = null;

                        GameModeRequest modeReq = new GameModeRequest("PVP");
                        envoyer(modeReq);

                        System.out.println("🔍 Recherche d'un adversaire...");

                        boolean matchFound = false;
                        while (!matchFound) {
                            Message matchMsg = recevoir();

                            if (matchMsg instanceof MatchmakingResponse matchResp) {
                                if (matchResp.getStatus().equals("WAITING")) {
                                    System.out.println("⏳ En attente d'un adversaire...");

                                } else if (matchResp.getStatus().equals("FOUND")) {
                                    opponentName = matchResp.getOpponentName();
                                    System.out.println("\n✅ Adversaire trouvé : " + opponentName);
                                    matchFound = true;
                                }
                            }
                        }

                        if (opponentName == null || opponentName.isEmpty()) {
                            System.out.println("❌ Erreur : Impossible de récupérer le nom de l'adversaire");
                            return false;
                        }

                        System.out.println("\n🎮 Partie PvP : " + joueur.getNom() + " vs " + opponentName);
                        System.out.println("═══════════════════════════════════\n");

                        demarrerNouvellePartie(console);
                        return true; // 🔥 RETOURNER ICI

                    } else {
                        // Rejouer contre le même (choix 1)
                        boolean result = gererRematchPvP(console);
                        return result; // 🔥 RETOURNER ICI
                    }

                } else {
                    // === MODE BOT ===
                    System.out.println("\n💭 Voulez-vous :");
                    System.out.println("1. Rejouer contre le BOT");
                    System.out.println("2. Jouer contre un JOUEUR (PvP)");
                    System.out.print("Votre choix : ");

                    String choixStr = console.readLine();
                    int choix = 1;
                    try {
                        choix = Integer.parseInt(choixStr.trim());
                    } catch (NumberFormatException e) {
                        choix = 1;
                    }

                    if (choix == 2) {
                        // === CHANGER VERS PVP ===
                        System.out.println("\n🔄 Changement de mode vers PvP...");
                        isPvPMode = true; // 🔥 Activer le mode PvP

                        GameModeRequest modeReq = new GameModeRequest("PVP");
                        envoyer(modeReq);

                        System.out.println("🔍 Recherche d'un adversaire...");

                        // Attendre le matchmaking
                        boolean matchFound = false;
                        while (!matchFound) {
                            Message matchMsg = recevoir();

                            if (matchMsg instanceof MatchmakingResponse matchResp) {
                                if (matchResp.getStatus().equals("WAITING")) {
                                    System.out.println("⏳ En attente d'un adversaire...");

                                } else if (matchResp.getStatus().equals("FOUND")) {
                                    opponentName = matchResp.getOpponentName();
                                    System.out.println("\n✅ Adversaire trouvé : " + opponentName);
                                    matchFound = true;
                                }
                            }
                        }

                        if (opponentName == null || opponentName.isEmpty()) {
                            System.out.println("❌ Erreur : Impossible de récupérer le nom de l'adversaire");
                            return false;
                        }

                        System.out.println("\n🎮 Partie PvP : " + joueur.getNom() + " vs " + opponentName);
                        System.out.println("═══════════════════════════════════\n");

                        demarrerNouvellePartie(console);
                        return true; // 🔥 RETOURNER ICI

                    } else {
                        // === REJOUER EN MODE BOT ===
                        System.out.println("\n🔄 Préparation d'une nouvelle partie...");

                        NewGameRequest ng = new NewGameRequest();
                        envoyer(ng);

                        Message ack = recevoir();
                        if (ack != null) {
                            System.out.println("✓ Serveur : nouvelle partie prête.");
                        }

                        demarrerNouvellePartie(console);
                        return true;
                    }
                }

            } else if (rep.equals("n") || rep.equals("non") || rep.equals("no")) {
                System.out.println("\n👋 Merci d'avoir joué ! À bientôt !");
                out.println("QUIT");
                return false;

            } else {
                System.out.println("⚠️  Réponse invalide. Veuillez taper 'o' pour rejouer ou 'n' pour quitter.");

            }
        }
    }

    /**
     *  Vide le buffer de lecture pour éviter les messages résiduels aprés une fin de partie
     */
    private void viderBuffer() {
        try {
            // Lire tous les messages en attente sans les traiter
            while (in.ready()) {
                String trash = in.readLine();
                if (trash != null) {
                    System.out.println("[Debug] Message résiduel vidé : " + trash);
                }
            }
        } catch (IOException e) {
            // Ignorer les erreurs de vidage
        }
    }

    /**
     * Cette methode permet de lancer la phase de placememnt des bâteaux du joueur
     * */
    private void phasePlacement() throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("     PHASE DE PLACEMENT DES BATEAUX");
        System.out.println("═══════════════════════════════════════════");
        System.out.println("Tu vas placer : PorteAvion (5), Croiseur (4),");
        System.out.println("                ContreTorpilleur (3), Torpilleur (2)");
        System.out.println("Les coordonnées vont de 1 à 10 (grille 10x10).");
        System.out.println("═══════════════════════════════════════════\n");

        placerUnBateau(console, new PorteAvion(), "PorteAvion", 5, ShipType.PORTE_AVION);
        placerUnBateau(console, new Croiseur(), "Croiseur", 4, ShipType.CROISEUR);
        placerUnBateau(console, new ContreTorpilleur(), "ContreTorpilleur", 3, ShipType.CONTRE_TORPILLEUR);
        placerUnBateau(console, new Torpilleur(), "Torpilleur", 2, ShipType.TORPILLEUR);

        System.out.println("\n✓ Tous les bateaux sont placés et envoyés au serveur !");
        System.out.println("\n═══ Ta grille personnelle ═══");
        joueur.getGrillePerso().afficher();
        System.out.println("═══════════════════════════════\n");
    }

    /**
     * Cette methode permet de placer un bâteau sur la grille perso du joueur qui est entrain de placer ses bâteaux
     * */
    private void placerUnBateau(BufferedReader console, Bâteau bateau, String nom, int longueur, ShipType shipType) throws IOException {
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


            // x = colonne (horizontal), y = ligne (vertical)
            boolean ok = joueur.placerBateau(bateau, x, y, horizontal);
            if (!ok) {
                System.out.println("❌ Impossible de placer ici (débordement ou chevauchement). Essaie ailleurs.");
            } else {
                System.out.println(nom + " placé en X=" + (x+1) + " Y=" + (y+1) + " " + (horizontal ? "HORIZONTAL" : "VERTICAL"));

                PlaceShipRequest placeMsg = new PlaceShipRequest(shipType, x, y, orientation);
                envoyer(placeMsg);

                Message response = recevoir();
                System.out.println("   → Serveur a confirmé le placement");

                joueur.getGrillePerso().afficher();
                break;
            }
        }
    }

    /**
     * Cette méthode permet d'afficher la grille de tir du joueur et sa grille personnels la ou il a placer ses bâteaux
     * */
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


    /**
     * Cette methode permet de femret la connexion entre le client et le serveur
     * */
    public void close() {
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        if (out != null) out.close();
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    /**
     * Démarre la boucle de jeu avec un tour initial spécifié
     * Utilisé en mode PvP pour démarrer avec le bon tour
     */
    public void startMessagingWithTurn(boolean monTourInitial) throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        boolean monTour = monTourInitial;

        while (true) {
            System.out.println();

            if (monTour) {
                System.out.println("🎯 C'EST VOTRE TOUR !");
                System.out.println("Commandes :");
                System.out.println("  shoot x y  -> tirer");
                System.out.println("  show       -> afficher les grilles");
                System.out.println("  quit       -> quitter");
                System.out.print("> ");
            } else {
                if (isPvPMode) {
                    System.out.println("⏳ Tour de " + opponentName + "... Attente...");
                } else {
                    System.out.println("⏳ Tour de l'adversaire...");
                }
            }

            // En mode PvP, si ce n'est pas notre tour, on attend un message
            if (isPvPMode && !monTour) {
                Message msg = recevoir();

                // 🔥 NOUVEAU : Gérer la déconnexion de l'adversaire
                if (msg instanceof OpponentLeftMessage leftMsg) {
                    System.out.println("\n⚠️ ══════════════════════════════════");

                    if (leftMsg.getReason().equals("disconnected")) {
                        System.out.println("   Votre adversaire s'est déconnecté.");
                        System.out.println("   🎉 Vous gagnez par forfait !");
                    } else if (leftMsg.getReason().equals("declined_rematch")) {
                        System.out.println("   Votre adversaire ne souhaite pas rejouer.");
                    } else {
                        System.out.println("   La partie ne peut pas continuer.");
                    }

                    System.out.println("══════════════════════════════════\n");

                    if (!proposerRejouer(console)) {
                        return;
                    }
                    return;
                }
                if (msg instanceof OpponentShotMessage opShot) {
                    handleOpponentShot(opShot);

                    // 🔥 AJOUT ICI : Gérer la fin de partie quand l'adversaire gagne
                    if (opShot.isGameOver()) {
                        handleGameOver(opShot.getWinner());

                        if (!proposerRejouer(console)) {
                            return; // Quitter le jeu
                        }

                        // Si on arrive ici, une nouvelle partie a déjà démarré
                        return;
                    }

                    monTour = opShot.isYourTurn();
                }
                continue;
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
                break;
            }

            if (input.toLowerCase().startsWith("shoot")) {
                if (!monTour) {
                    System.out.println("❌ Ce n'est pas votre tour !");
                    continue;
                }

                String[] parts = input.split("\\s+");
                if (parts.length != 3) {
                    System.out.println("Format: shoot X Y");
                    continue;
                }

                int x, y;
                try {
                    x = Integer.parseInt(parts[1]) - 1;
                    y = Integer.parseInt(parts[2]) - 1;
                } catch (NumberFormatException nfe) {
                    System.out.println("Coordonnées invalides");
                    continue;
                }

                if (x < 0 || x > 9 || y < 0 || y > 9) {
                    System.out.println("Les coordonnées doivent être entre 1 et 10.");
                    continue;
                }

                ShotRequest req = new ShotRequest(x, y);
                envoyer(req);

                // Mode PvP uniquement
                if (isPvPMode) {
                    // 1) Recevoir ShotResponse
                    Message msg = recevoir();
                    System.out.println("[Debug Client] Message reçu: " + (msg != null ? msg.getClass().getSimpleName() : "null"));

                    if (msg instanceof ShotResponse res) {
                        System.out.println("[Debug Client] Résultat: " + res.getResultat());
                        System.out.println("[Debug Client] GameOver: " + res.isGameOver());
                        System.out.println("[Debug Client] Winner: " + res.getWinner());

                        ResultatTir resultat = res.getResultat();
                        System.out.println("Ton tir en (" + (x+1) + "," + (y+1) + ") : "
                                + resultat + (res.getNomBateau() != null ? " sur " + res.getNomBateau() : ""));

                        joueur.enregistrerResultatTir(x, y, resultat);

                        // 🔥 Vérifier si la partie est terminée (vous avez gagné)
                        if (res.isGameOver()) {
                            handleGameOver(res.getWinner());

                            if (!proposerRejouer(console)) {
                                return;
                            }
                            return;
                        }

                        // 🔥 Déterminer le prochain tour selon le résultat
                        if (resultat == ResultatTir.HIT || resultat == ResultatTir.SUNK) {
                            System.out.println("✨ TOUCHÉ ! Vous rejouez !");
                            monTour = true;
                        } else if (resultat == ResultatTir.MISS) {
                            System.out.println("💧 Raté... Tour de " + opponentName);
                            monTour = false;
                        } else if (resultat == ResultatTir.ALREADY_TRIED || resultat == ResultatTir.OUT_OF_BOUNDS) {
                            // 🔥 TIR INVALIDE : Vous gardez votre tour, PAS de message adversaire
                            System.out.println("⚠️ Tir invalide, réessayez.");
                            monTour = true;
                            // 🔥 NE PAS ATTENDRE DE MESSAGE SUPPLÉMENTAIRE
                            continue; // Retour au début de la boucle
                        }
                    } else if (msg instanceof OpponentShotMessage) {
                        // 🔥 CAS SPÉCIAL : On a reçu OpponentShotMessage au lieu de ShotResponse
                        // Cela signifie qu'on a essayé de tirer alors que ce n'était pas notre tour
                        System.out.println("⚠️ Ce n'est pas votre tour !");
                        monTour = false;

                        // Traiter le tir de l'adversaire
                        OpponentShotMessage opShot = (OpponentShotMessage) msg;
                        handleOpponentShot(opShot);

                        if (opShot.isGameOver()) {
                            handleGameOver(opShot.getWinner());
                            if (!proposerRejouer(console)) {
                                return;
                            }
                            return;
                        }

                        monTour = opShot.isYourTurn();
                        continue;
                    }
                }
            }
        }
    }
    /**
     * Permet au joueur de choisir le mode de jeu (BOT ou PVP)
     * @return true si le choix a réussi, false sinon
     */
    /**
     * Permet au joueur de choisir le mode de jeu (BOT ou PVP)
     * @return true si le choix a réussi, false sinon
     */
    private boolean choisirModeJeu(BufferedReader console) throws IOException {
        System.out.println("\n╔═══════════════════════════════════════╗");
        System.out.println("        SÉLECTION DU MODE");
        System.out.println("╚═══════════════════════════════════════╝");
        System.out.println("1. Jouer contre le BOT (IA)");
        System.out.println("2. Jouer contre un JOUEUR (PvP)");
        System.out.print("\nVotre choix : ");

        String choixStr = console.readLine();
        if (choixStr == null) {
            return false;
        }

        int choix = 1;
        try {
            choix = Integer.parseInt(choixStr.trim());
        } catch (NumberFormatException e) {
            System.out.println("Choix invalide, mode BOT sélectionné par défaut.");
        }

        if (choix == 2) {
            // Mode PvP
            isPvPMode = true;
            GameModeRequest modeReq = new GameModeRequest("PVP");
            envoyer(modeReq);

            System.out.println("\n🔍 Recherche d'un adversaire...");

            // 🔥 VIDER LE BUFFER
            viderBuffer();

            // Attendre la réponse du matchmaking
            Message matchMsg = recevoir();
            if (matchMsg instanceof MatchmakingResponse matchResp) {
                if (matchResp.getStatus().equals("WAITING")) {
                    System.out.println("⏳ En attente d'un adversaire...");

                    // Attendre qu'un adversaire soit trouvé
                    while (true) {
                        Message msg = recevoir();
                        if (msg instanceof MatchmakingResponse mr && mr.getStatus().equals("FOUND")) {
                            opponentName = mr.getOpponentName();
                            System.out.println("\n✅ Adversaire trouvé : " + opponentName);
                            break;
                        }
                    }
                } else if (matchResp.getStatus().equals("FOUND")) {
                    opponentName = matchResp.getOpponentName();
                    System.out.println("\n✅ Adversaire trouvé : " + opponentName);
                }
            }

            System.out.println("\n🎮 Partie PvP : " + joueur.getNom() + " vs " + opponentName);
            System.out.println("═══════════════════════════════════════\n");

        } else {
            // Mode BOT
            isPvPMode = false;
            opponentName = null;
            GameModeRequest modeReq = new GameModeRequest("BOT");
            envoyer(modeReq);

            // 🔥 VIDER LE BUFFER
            viderBuffer();

            Message botResponse = recevoir();
            System.out.println("\n🤖 Mode BOT sélectionné");
            System.out.println("═══════════════════════════════════════\n");
        }

        return true;
    }

    /**
     * Démarre une nouvelle partie (placement + jeu)
     */
    /**
     * Démarre une nouvelle partie (placement + jeu)
     */
    private void demarrerNouvellePartie(BufferedReader console) throws IOException {
        // Réinitialiser le joueur
        this.joueur = new Joueur(joueur.getNom(), 10);

        // Phase de placement
        phasePlacement();

        if (isPvPMode) {
            // ===== MODE PVP =====
            System.out.println("\n⏳ En attente que " + opponentName + " termine son placement...");
            //1
            // 🔥 Timeout de 15 secondes pour éviter l'attente infinie
            long timeout = 15000;
            long startTime = System.currentTimeMillis();

            Message gameStartMsg = null;
            while (System.currentTimeMillis() - startTime < timeout) {
                try {
                    if (in.ready()) {
                        gameStartMsg = recevoir();
                        if (gameStartMsg instanceof GameStartMessage) {
                            break;
                        } else if (gameStartMsg instanceof OpponentLeftMessage) {
                            System.out.println("❌ Votre adversaire s'est déconnecté pendant le placement.");
                            gererApresRefusRematch(console);
                            return;
                        }
                    }
                } catch (IOException e) {
                    // Erreur de lecture
                }

                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }

            if (gameStartMsg instanceof GameStartMessage startMsg) {
                System.out.println("\n╔═══════════════════════════════════════╗");
                System.out.println("       LA PARTIE COMMENCE !");
                System.out.println("╚═══════════════════════════════════════╝");

                if (startMsg.isYourTurn()) {
                    System.out.println("🎯 C'est à vous de commencer !\n");
                } else {
                    System.out.println("⏳ " + startMsg.getOpponentName() + " commence. Attendez votre tour...\n");
                }

                // Démarrer avec le bon tour
                startMessagingWithTurn(startMsg.isYourTurn());
            } else {
                System.out.println("⚠️ Erreur: Timeout ou message GAME_START non reçu");
                System.out.println("Votre adversaire semble s'être déconnecté.");
                gererApresRefusRematch(console);
            }

        } else {
            // ===== MODE BOT =====
            System.out.println("\n🎮 La partie contre le BOT commence !\n");

            // 🔥 VIDER LE BUFFER AVANT DE COMMENCER
            viderBuffer();

            // Démarrer normalement en mode BOT
            startMessaging();
        }
    }

    private boolean gererRematchPvP(BufferedReader console) throws IOException {
        System.out.println("\n🔄 Demande de rematch envoyée à " + opponentName + "...");

        NewGameRequest ng = new NewGameRequest();
        envoyer(ng);

        System.out.println("⏳ En attente de la réponse de " + opponentName + "...");
        System.out.println("   (Maximum 30 secondes)");

        long timeout = 30000;
        long startTime = System.currentTimeMillis();
        boolean waitingMessageShown = false;

        while (System.currentTimeMillis() - startTime < timeout) {
            Message response = null;

            try {
                if (in.ready()) {
                    response = recevoir();
                }
            } catch (Exception e) {
                // Erreur de lecture
            }

            if (response instanceof RematchResponse rematchResp) {
                if (rematchResp.getStatus().equals("WAITING")) {
                    if (!waitingMessageShown) {
                        System.out.println("⏳ " + rematchResp.getMessage());
                        waitingMessageShown = true;
                    }

                } else if (rematchResp.getStatus().equals("ACCEPTED")) {
                    System.out.println("✅ " + rematchResp.getMessage());
                    demarrerNouvellePartie(console);
                    return true;

                } else if (rematchResp.getStatus().equals("OPPONENT_LEFT")) {
                    System.out.println("❌ " + rematchResp.getMessage());
                    return gererApresRefusRematch(console);
                }

            } else if (response instanceof OpponentLeftMessage) {
                System.out.println("❌ Votre adversaire s'est déconnecté.");
                return gererApresRefusRematch(console);
            }

            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }

        // Timeout
        System.out.println("\n⏰ Timeout : Aucune réponse après 30 secondes.");
        System.out.println("Votre adversaire ne souhaite probablement pas rejouer.");

        // 🔥 NE PAS envoyer QUIT ici, juste proposer les options
        return gererApresRefusRematch(console);
    }

    /**
     * 🔥 NOUVEAU : Gérer les options après un refus/timeout de rematch
     */
    private boolean gererApresRefusRematch(BufferedReader console) throws IOException {
        System.out.println("\n💭 Que voulez-vous faire ?");
        System.out.println("1. Chercher un nouvel adversaire (PvP)");
        System.out.println("2. Jouer contre le BOT");
        System.out.println("3. Quitter");
        System.out.print("Votre choix : ");

        String subChoice = console.readLine();

        // 🔥 Si l'utilisateur ne répond rien ou appuie juste sur Entrée
        if (subChoice == null || subChoice.trim().isEmpty()) {
            System.out.println("\n👋 Aucun choix effectué. Déconnexion...");
            out.println("QUIT");
            return false;
        }

        switch (subChoice.trim()) {
            case "1":
                System.out.println("\n🔄 Recherche d'un nouvel adversaire (PvP)...");

                opponentName = null; // Réinitialiser

                GameModeRequest modeReq = new GameModeRequest("PVP");
                envoyer(modeReq);

                System.out.println("🔍 Recherche d'un adversaire...");

                // 🔥 Attendre le matchmaking correctement
                boolean matchFound = false;
                while (!matchFound) {
                    Message matchMsg = recevoir();
                    System.out.println("[DEBUG Client] Message reçu: " + (matchMsg != null ? matchMsg.getClass().getSimpleName() : "null")); // 🔥 ICI
                    if (matchMsg instanceof MatchmakingResponse matchResp) {
                        System.out.println("[DEBUG Client] Status: " + matchResp.getStatus()); // 🔥 ICI
                        System.out.println("[DEBUG Client] OpponentName: " + matchResp.getOpponentName());
                        if (matchResp.getStatus().equals("WAITING")) {
                            System.out.println("⏳ En attente d'un adversaire...");

                        } else if (matchResp.getStatus().equals("FOUND")) {

                            opponentName = matchResp.getOpponentName();
                            System.out.println("[DEBUG Client] opponentName assigné: " + opponentName);
                            System.out.println("\n✅ Adversaire trouvé : " + opponentName);
                            matchFound = true;
                        }

                    }
                }
                System.out.println("[DEBUG Client] Avant vérification - opponentName = " + opponentName);
                // 🔥 Vérifier que opponentName n'est pas null
                if (opponentName == null || opponentName.isEmpty()) {
                    System.out.println("❌ Erreur : Nom de l'adversaire non récupéré");
                    return false;
                }

                System.out.println("\n🎮 Partie PvP : " + joueur.getNom() + " vs " + opponentName);
                System.out.println("═══════════════════════════════════\n");

                demarrerNouvellePartie(console);
                return true;

            case "2":
                System.out.println("\n🔄 Changement de mode vers BOT...");
                isPvPMode = false;
                opponentName = null;

                modeReq = new GameModeRequest("BOT");
                envoyer(modeReq);

                Message botResp = recevoir();
                System.out.println("✓ Mode BOT activé\n");

                demarrerNouvellePartie(console);
                return true;

            case "3":
            default:
                System.out.println("\n👋 Merci d'avoir joué ! À bientôt !");
                out.println("QUIT");
                return false;
        }
    }

    public static void main(String[] args) {
        ClientTCP client = new ClientTCP("Client", 10);

        try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            System.out.println("╔═══════════════════════════════════╗");
            System.out.println("        JEU DE BATAILLE NAVALE");
            System.out.println("╚═══════════════════════════════════╝\n");

            System.out.print("Entrer l'adresse IP du serveur : ");
            String ip = console.readLine();

            System.out.print("Entrer le port du serveur : ");
            int port = Integer.parseInt(console.readLine());

            client.connecter(ip, port);

            // Demander le pseudo
            System.out.print("\nEntrez votre pseudo : ");
            String pseudo = console.readLine();
            if (pseudo == null || pseudo.trim().isEmpty()) {
                pseudo = "Joueur";
            }
            pseudo = pseudo.trim();

            // Envoyer le pseudo au serveur
            SetUsernameRequest usernameMsg = new SetUsernameRequest(pseudo);
            client.envoyer(usernameMsg);

            Message response = client.recevoir();
            System.out.println("✓ Bienvenue " + pseudo + " !\n");

            client.joueur = new Joueur(pseudo, 10);

            // Choisir le mode de jeu
            client.choisirModeJeu(console);

            // Démarrer la première partie
            client.demarrerNouvellePartie(console);

        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
            e.printStackTrace();
        } finally {
            client.close();
        }
    }
}