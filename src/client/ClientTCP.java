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
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Joueur joueur;
    private boolean isPvPMode = false;
    private String opponentName = null;

    public ClientTCP(String nomJoueur, int tailleGrille) {
        this.joueur = new Joueur(nomJoueur, tailleGrille);
    }

    // ========================================
    // CONNEXION ET COMMUNICATION
    // ========================================

    public void connecter(String ip, int port) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(ip, port), 3000);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        System.out.println("Connecté au serveur : " + ip + ":" + port);
    }

    public void envoyer(Message msg) {
        out.println(msg.serialize());
    }

    public Message recevoir() {
        try {
            String raw = in.readLine();
            if (raw == null) {
                System.out.println("Connexion fermée par le serveur.");
                return null;
            }
            return Message.deserialize(raw);
        } catch (RuntimeException parseEx) {
            return null;
        } catch (IOException e) {
            System.err.println("Erreur réception : " + e.getMessage());
            return null;
        }
    }

    private void viderBuffer() {
        try {
            while (in.ready()) {
                in.readLine();
            }
        } catch (IOException ignored) {}
    }

    public void close() {
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        if (out != null) out.close();
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    // ========================================
    // BOUCLE DE JEU
    // ========================================

    public void startMessaging() throws IOException {
        startMessagingWithTurn(true);
    }

    public void startMessagingWithTurn(boolean monTourInitial) throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        boolean monTour = monTourInitial;

        while (true) {
            System.out.println();

            if (monTour) {
                afficherMenuTour();
            } else {
                System.out.println("⏳ Tour de " + (isPvPMode ? opponentName : "l'adversaire") + "...");
            }

            // En mode PvP, attendre les messages adverses
            if (isPvPMode && !monTour) {
                Message msg = recevoir();

                // 🔥 CORRECTION : Gérer la déconnexion
                if (msg == null) {
                    System.out.println("\n❌ Connexion perdue avec le serveur.");
                    System.out.println("🎉 Vous gagnez par forfait !");
                    close();
                    return;
                }

                if (msg instanceof OpponentLeftMessage leftMsg) {
                    gererDeconnexionAdversaire(leftMsg, console);
                    return;
                }
                if (msg instanceof OpponentShotMessage opShot) {
                    handleOpponentShot(opShot);
                    if (opShot.isGameOver()) {
                        handleGameOver(opShot.getWinner());
                        if (!proposerRejouer(console)) return;
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
                // 🔥 CORRECTION : Utiliser Boolean pour distinguer fin de partie
                Boolean resultat = traiterTir(input, monTour, console);
                if (resultat == null) {
                    // Partie terminée
                    return;
                }
                monTour = resultat;
            }
        }
    }

    private void afficherMenuTour() {
        System.out.println("🎯 C'EST VOTRE TOUR !");
        System.out.println("Commandes : shoot x y | show | quit");
        System.out.print("> ");
    }

    private Boolean traiterTir(String input, boolean monTour, BufferedReader console) throws IOException {
        String[] parts = input.split("\\s+");
        if (parts.length != 3) {
            System.out.println("Format: shoot X Y");
            return monTour;
        }

        int x, y;
        try {
            x = Integer.parseInt(parts[1]) - 1;
            y = Integer.parseInt(parts[2]) - 1;
        } catch (NumberFormatException nfe) {
            System.out.println("Coordonnées invalides");
            return monTour;
        }

        if (x < 0 || x > 9 || y < 0 || y > 9) {
            System.out.println("Les coordonnées doivent être entre 1 et 10.");
            return monTour;
        }

        envoyer(new ShotRequest(x, y));

        if (isPvPMode) {
            return traiterTirPvP(x, y, console);
        } else {
            return traiterTirBot(x, y, console);
        }
    }

    private Boolean traiterTirPvP(int x, int y, BufferedReader console) throws IOException {
        Message msg = recevoir();

        // 🔥 CORRECTION : Gérer la déconnexion
        if (msg == null) {
            System.out.println("\n❌ Connexion perdue avec le serveur.");
            close();
            return null;
        }

        if (!(msg instanceof ShotResponse res)) return true;

        ResultatTir resultat = res.getResultat();
        System.out.println("Ton tir en (" + (x+1) + "," + (y+1) + ") : " + resultat
                + (res.getNomBateau() != null ? " sur " + res.getNomBateau() : ""));

        // 🔥 CORRECTION : Ne pas enregistrer ALREADY_TRIED (ça écrase le vrai résultat)
        if (resultat != ResultatTir.ALREADY_TRIED && resultat != ResultatTir.OUT_OF_BOUNDS) {
            joueur.enregistrerResultatTir(x, y, resultat);
        }

        if (res.isGameOver()) {
            handleGameOver(res.getWinner());
            proposerRejouer(console);
            return null; // null = partie terminée
        }

        if (resultat == ResultatTir.ALREADY_TRIED || resultat == ResultatTir.OUT_OF_BOUNDS) {
            System.out.println("⚠️ Tir invalide, réessayez.");
            return true;
        }

        if (resultat == ResultatTir.HIT || resultat == ResultatTir.SUNK) {
            System.out.println("✨ TOUCHÉ ! Vous rejouez !");
            return true;
        } else {
            System.out.println("💧 Raté... Tour de " + opponentName);
            return false;
        }
    }


    private Boolean traiterTirBot(int x, int y, BufferedReader console) throws IOException {
        // Recevoir la réponse au tir du joueur
        Message msg = recevoir();

        // Ignorer messages résiduels
        while (msg != null && !(msg instanceof ShotResponse)) {
            msg = recevoir();
        }

        if (msg == null) {
            System.out.println("\n❌ Connexion perdue avec le serveur.");
            close();
            return null;
        }

        if (!(msg instanceof ShotResponse res)) return true;

        ResultatTir resultat = res.getResultat();
        System.out.println("Ton tir en (" + (x + 1) + "," + (y + 1) + ") : " + resultat
                + (res.getNomBateau() != null ? " sur " + res.getNomBateau() : ""));

        // 🔥 CORRECTION : Ne pas enregistrer ALREADY_TRIED (ça écrase le vrai résultat)
        if (resultat != ResultatTir.ALREADY_TRIED && resultat != ResultatTir.OUT_OF_BOUNDS) {
            joueur.enregistrerResultatTir(x, y, resultat);
        }

        // 🔥 CORRECTION : Vérifier IMMÉDIATEMENT si le joueur a gagné
        if (res.isGameOver()) {
            afficherFinPartie(true);
            proposerRejouer(console);
            return null; // Partie terminée
        }

        // 🔥 CORRECTION : Si tir invalide, le joueur rejoue
        if (resultat == ResultatTir.ALREADY_TRIED || resultat == ResultatTir.OUT_OF_BOUNDS) {
            System.out.println("⚠️ Tir invalide, réessayez.");
            return true;
        }

        boolean rejoue = (resultat == ResultatTir.HIT || resultat == ResultatTir.SUNK);
        System.out.println(rejoue ? "✨ TOUCHÉ ! Vous rejouez !" : "💧 Raté... Tour de l'adversaire.");

        // 🔥 Si le joueur a touché, il rejoue immédiatement
        if (rejoue) {
            return true;
        }

        // 🔥 Le joueur a raté : attendre le(s) tir(s) du serveur
        Message msg2 = recevoir();
        if (msg2 == null) {
            System.out.println("\n❌ Connexion perdue avec le serveur.");
            close();
            return null;
        }

        if (!(msg2 instanceof ServerShotMessage sshot)) {
            return false; // Pas de tir serveur, tour suivant
        }

        // Vérifier game over avant de traiter les tirs
        if (sshot.isGameOver() && sshot.getWinner() != null && !sshot.getWinner().equals("SERVER")) {
            afficherFinPartie(true);
            proposerRejouer(console);
            return null;
        }

        // Traiter les tirs consécutifs du serveur
        if (sshot.getX() >= 0 && sshot.getY() >= 0) {
            boolean partieTerminee = traiterTirsServeurConsecutifs(sshot, console);
            if (partieTerminee) {
                return null; // Partie terminée
            }
        }

        // 🔥 CORRECTION : Après les tirs du serveur, c'est au tour du joueur
        return true;
    }

        private boolean traiterTirsServeurConsecutifs(ServerShotMessage sshot, BufferedReader console) throws IOException {
        while (true) {
            int ex = sshot.getX(), ey = sshot.getY();
            ResultatTir resAdv = sshot.getResultat();

            System.out.println("\n>> L'ennemi a tiré en (" + (ex+1) + "," + (ey+1) + ") : " + resAdv
                    + (sshot.getNomBateau() != null ? " sur " + sshot.getNomBateau() : ""));
            joueur.recevoirTir(ex, ey);

            if (sshot.isGameOver()) {
                afficherFinPartie("SERVER".equals(sshot.getWinner()));
                proposerRejouer(console);
                return true; // Partie terminée
            }

            if (resAdv == ResultatTir.HIT || resAdv == ResultatTir.SUNK) {
                System.out.println("💥 L'adversaire a touché ! Il tire encore...");
                Message nextMsg = recevoir();

                if (nextMsg == null) {
                    System.out.println("\n❌ Connexion perdue.");
                    close();
                    return true;
                }

                if (nextMsg instanceof ServerShotMessage) {
                    sshot = (ServerShotMessage) nextMsg;
                } else {
                    break;
                }
            } else {
                System.out.println("🎯 L'adversaire a raté ! C'est à votre tour !");
                break;
            }
        }
        return false; // Partie continue
    }

    // ========================================
    // GESTION DES ÉVÉNEMENTS
    // ========================================

    private void handleGameOver(String winner) {
        afficherFinPartie(winner.equals(joueur.getNom()));
    }

    private void afficherFinPartie(boolean victoire) {
        System.out.println("\n╔═══════════════════════════════════╗");
        System.out.println("         FIN DE LA PARTIE");
        System.out.println("╚═══════════════════════════════════╝");
        System.out.println(victoire ? "🎉 FÉLICITATIONS ! VOUS AVEZ GAGNÉ ! 🎉"
                : "😞 VOUS AVEZ PERDU 😞");
        System.out.println("╚═══════════════════════════════════╝\n");
    }

    private void handleOpponentShot(OpponentShotMessage opShot) {
        if (opShot.getX() >= 0 && opShot.getY() >= 0) {
            System.out.println("\n>> " + opponentName + " a tiré en ("
                    + (opShot.getX()+1) + "," + (opShot.getY()+1) + ") : " + opShot.getResultat()
                    + (opShot.getNomBateau() != null ? " sur " + opShot.getNomBateau() : ""));
            joueur.recevoirTir(opShot.getX(), opShot.getY());
        }
    }

    private void gererDeconnexionAdversaire(OpponentLeftMessage leftMsg, BufferedReader console) throws IOException {
        System.out.println("\n⚠️ ═══════════════════════════════════");
        String raison = leftMsg.getReason().equals("disconnected")
                ? "Votre adversaire s'est déconnecté.\n   🎉 Vous gagnez par forfait !"
                : "Votre adversaire ne souhaite pas rejouer.";
        System.out.println("   " + raison);
        System.out.println("═══════════════════════════════════\n");
        proposerRejouer(console);
    }

    // ========================================
    // CHOIX ET MATCHMAKING
    // ========================================

    private boolean proposerRejouer(BufferedReader console) throws IOException {
        while (true) {
            System.out.print("\nVoulez-vous rejouer ? (o/n) : ");
            String rep = console.readLine();
            if (rep == null || rep.trim().equalsIgnoreCase("n") || rep.trim().equalsIgnoreCase("non")) {
                System.out.println("\n👋 Merci d'avoir joué ! À bientôt !");
                out.println("QUIT");
                close();
                return false;
            }

            if (rep.trim().equalsIgnoreCase("o") || rep.trim().equalsIgnoreCase("oui")) {
                return gererChoixRejouer(console);
            }

            System.out.println("⚠️  Réponse invalide. Tapez 'o' ou 'n'.");
        }
    }

    private boolean gererChoixRejouer(BufferedReader console) throws IOException {
        if (isPvPMode) {
            return gererRejouerPvP(console);
        } else {
            return gererRejouerBot(console);
        }
    }

    private boolean gererRejouerPvP(BufferedReader console) throws IOException {
        System.out.println("\n💭 Options :");
        System.out.println("1. Rejouer contre " + opponentName);
        System.out.println("2. Jouer contre le BOT");
        System.out.println("3. Chercher un nouvel adversaire");
        System.out.print("Votre choix : ");

        int choix = lireChoix(console, 1);

        switch (choix) {
            case 2: return changerVersBot(console);
            case 3: return chercherNouvelAdversaire(console);
            default: return gererRematchPvP(console);
        }
    }

    private boolean gererRejouerBot(BufferedReader console) throws IOException {
        System.out.println("\n💭 Options :");
        System.out.println("1. Rejouer contre le BOT");
        System.out.println("2. Jouer contre un JOUEUR (PvP)");
        System.out.print("Votre choix : ");

        int choix = lireChoix(console, 1);

        if (choix == 2) {
            return changerVersPvP(console);
        } else {
            System.out.println("\n🔄 Préparation d'une nouvelle partie...");
            envoyer(new NewGameRequest());
            recevoir();
            demarrerNouvellePartie(console);
            return true;
        }
    }

    private int lireChoix(BufferedReader console, int defaut) {
        try {
            String choixStr = console.readLine();
            return choixStr != null ? Integer.parseInt(choixStr.trim()) : defaut;
        } catch (Exception e) {
            return defaut;
        }
    }

    private boolean changerVersBot(BufferedReader console) throws IOException {
        System.out.println("\n🔄 Changement vers BOT...");
        isPvPMode = false;
        opponentName = null;
        envoyer(new GameModeRequest("BOT"));
        recevoir();
        demarrerNouvellePartie(console);
        return true;
    }

    private boolean changerVersPvP(BufferedReader console) throws IOException {
        System.out.println("\n🔄 Changement vers PvP...");
        isPvPMode = true;
        envoyer(new GameModeRequest("PVP"));
        attendreMatchmaking();
        demarrerNouvellePartie(console);
        return true;
    }

    private boolean chercherNouvelAdversaire(BufferedReader console) throws IOException {
        System.out.println("\n🔄 Recherche d'un nouvel adversaire...");
        opponentName = null;
        envoyer(new GameModeRequest("PVP"));
        attendreMatchmaking();
        demarrerNouvellePartie(console);
        return true;
    }

    private void attendreMatchmaking() throws IOException {
        System.out.println("🔍 Recherche d'un adversaire...");
        viderBuffer();

        while (true) {
            Message msg = recevoir();
            if (msg instanceof MatchmakingResponse mr) {
                if (mr.getStatus().equals("FOUND")) {
                    opponentName = mr.getOpponentName();
                    System.out.println("\n✅ Adversaire trouvé : " + opponentName);
                    System.out.println("🎮 Partie PvP : " + joueur.getNom() + " vs " + opponentName + "\n");
                    return;
                } else if (mr.getStatus().equals("WAITING")) {
                    System.out.println("⏳ En attente...");
                }
            }
        }
    }

    private boolean gererRematchPvP(BufferedReader console) throws IOException {
        System.out.println("\n🔄 Demande de rematch...");
        envoyer(new NewGameRequest());
        System.out.println("⏳ Enn attente de la réponse (30s max)");

        long timeout = 45000, startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeout) {
            if (in.ready()) {
                Message response = recevoir();
                if (response instanceof RematchResponse rr) {
                    if (rr.getStatus().equals("ACCEPTED")) {
                        System.out.println("✅ " + rr.getMessage());
                        demarrerNouvellePartie(console);
                        return true;
                    } else if (rr.getStatus().equals("OPPONENT_LEFT")) {
                        System.out.println("❌ " + rr.getMessage());
                        return gererApresRefusRematch(console);
                    }
                } else if (response instanceof OpponentLeftMessage) {
                    return gererApresRefusRematch(console);
                }
            }
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }

        System.out.println("\n⏰ Timeout. Adversaire non disponible.");
        return gererApresRefusRematch(console);
    }

    private boolean gererApresRefusRematch(BufferedReader console) throws IOException {
        System.out.println("\n💭 Options :");
        System.out.println("1. Chercher un nouvel adversaire");
        System.out.println("2. Jouer contre le BOT");
        System.out.println("3. Quitter");
        System.out.print("Votre choix : ");

        String choix = console.readLine();
        if (choix == null || choix.trim().isEmpty()) choix = "3";

        switch (choix.trim()) {
            case "1": return chercherNouvelAdversaire(console);
            case "2": return changerVersBot(console);
            default:
                System.out.println("\n👋 À bientôt !");
                out.println("QUIT");
                return false;
        }
    }

    // ========================================
    // PLACEMENT DES BATEAUX
    // ========================================

    private void phasePlacement() throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("     PHASE DE PLACEMENT DES BATEAUX");
        System.out.println("═══════════════════════════════════════════");
        System.out.println("Bateaux : PorteAvion(5), Croiseur(4),");
        System.out.println("          ContreTorpilleur(3), Torpilleur(2)");
        System.out.println("Coordonnées de 1 à 10 (grille 10x10).");
        System.out.println("═══════════════════════════════════════════\n");

        placerUnBateau(console, new PorteAvion(), "PorteAvion", 5, ShipType.PORTE_AVION);
        placerUnBateau(console, new Croiseur(), "Croiseur", 4, ShipType.CROISEUR);
        placerUnBateau(console, new ContreTorpilleur(), "ContreTorpilleur", 3, ShipType.CONTRE_TORPILLEUR);
        placerUnBateau(console, new Torpilleur(), "Torpilleur", 2, ShipType.TORPILLEUR);

        System.out.println("\n✓ Tous les bateaux placés !");
        System.out.println("\n═══ Votre grille ═══");
        joueur.getGrillePerso().afficher();
        System.out.println("═══════════════════════════════════\n");
    }

    private void placerUnBateau(BufferedReader console, Bâteau bateau, String nom, int longueur, ShipType shipType) throws IOException {
        while (true) {
            System.out.println("\n📍 Placement du " + nom + " (longueur " + longueur + ")");
            System.out.print("X Y orientation (ex: 2 3 H) : ");
            String ligne = console.readLine();
            if (ligne == null) return;

            String[] parts = ligne.trim().split("\\s+");
            if (parts.length != 3) {
                System.out.println("❌ Format invalide. Exemple: 2 3 H");
                continue;
            }

            int x, y;
            try {
                x = Integer.parseInt(parts[0]) - 1;
                y = Integer.parseInt(parts[1]) - 1;
            } catch (NumberFormatException e) {
                System.out.println("❌ Coordonnées invalides.");
                continue;
            }

            if (x < 0 || x > 9 || y < 0 || y > 9) {
                System.out.println("❌ Coordonnées entre 1 et 10.");
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
                System.out.println("❌ Orientation: H ou V.");
                continue;
            }

            if (!joueur.placerBateau(bateau, x, y, horizontal)) {
                System.out.println("❌ Placement impossible. Essayez ailleurs.");
            } else {
                System.out.println(nom + " placé en (" + (x+1) + "," + (y+1) + ") " + (horizontal ? "H" : "V"));
                envoyer(new PlaceShipRequest(shipType, x, y, orientation));
                recevoir();
                joueur.getGrillePerso().afficher();
                break;
            }
        }
    }

    private void afficherVueJoueur() {
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("     VUE DU JOUEUR " + joueur.getNom());
        System.out.println("═══════════════════════════════════════");
        System.out.println("\n📋 Votre grille :");
        joueur.getGrillePerso().afficher();
        System.out.println("\n🎯 Grille de tirs :");
        joueur.getGrilleTirs().afficher();
        System.out.println();
    }

    // ========================================
    // DÉMARRAGE DE PARTIE
    // ========================================

    private boolean choisirModeJeu(BufferedReader console) throws IOException {
        System.out.println("\n╔═══════════════════════════════════════╗");
        System.out.println("        SÉLECTION DU MODE");
        System.out.println("╚═══════════════════════════════════════╝");
        System.out.println("1. Jouer contre le BOT (IA)");
        System.out.println("2. Jouer contre un JOUEUR (PvP)");
        System.out.print("\nVotre choix : ");

        int choix = lireChoix(console, 1);

        if (choix == 2) {
            isPvPMode = true;
            envoyer(new GameModeRequest("PVP"));
            viderBuffer();
            attendreMatchmaking();
        } else {
            isPvPMode = false;
            envoyer(new GameModeRequest("BOT"));
            viderBuffer();
            recevoir();
            System.out.println("\n🤖 Mode BOT sélectionné\n");
        }

        return true;
    }

    private void demarrerNouvellePartie(BufferedReader console) throws IOException {
        this.joueur = new Joueur(joueur.getNom(), 10);
        phasePlacement();

        if (isPvPMode) {
            System.out.println("\n⏳ En attente de " + opponentName + "...");
            long timeout = 15000, startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < timeout) {
                if (in.ready()) {
                    Message msg = recevoir();
                    if (msg instanceof GameStartMessage startMsg) {
                        System.out.println("\n🎮 LA PARTIE COMMENCE !");
                        System.out.println(startMsg.isYourTurn() ? "🎯 Vous commencez !"
                                : "⏳ " + opponentName + " commence.");
                        startMessagingWithTurn(startMsg.isYourTurn());
                        return;
                    } else if (msg instanceof OpponentLeftMessage) {
                        System.out.println("❌ Adversaire déconnecté.");
                        gererApresRefusRematch(console);
                        return;
                    }
                }
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }

            System.out.println("⚠️ Timeout. Adversaire déconnecté.");
            gererApresRefusRematch(console);
        } else {
            System.out.println("\n🎮 Partie contre le BOT !\n");
            viderBuffer();
            startMessaging();
        }
    }

    // ========================================
    // MAIN
    // ========================================

    public static void main(String[] args) {
        ClientTCP client = new ClientTCP("Client", 10);

        try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            System.out.println("╔═══════════════════════════════════════╗");
            System.out.println("        JEU DE BATAILLE NAVALE");
            System.out.println("╚═══════════════════════════════════════╝\n");

            System.out.print("IP du serveur : ");
            String ip = console.readLine();
            System.out.print("Port du serveur : ");
            int port = Integer.parseInt(console.readLine());

            client.connecter(ip, port);

            System.out.print("\nVotre pseudo : ");
            String pseudo = console.readLine();
            if (pseudo == null || pseudo.trim().isEmpty()) pseudo = "Joueur";

            client.envoyer(new SetUsernameRequest(pseudo.trim()));
            client.recevoir();
            System.out.println("✓ Bienvenue " + pseudo + " !\n");

            client.joueur = new Joueur(pseudo.trim(), 10);
            client.choisirModeJeu(console);
            client.demarrerNouvellePartie(console);

        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
            e.printStackTrace();
        } finally {
            client.close();
        }
    }
}