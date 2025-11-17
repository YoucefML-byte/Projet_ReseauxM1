package serveur;

import bâteaux.*;
import etats.Orientation;
import etats.ResultatTir;
import grille.Grille;
import grille.Grille.TirResult;
import joueur.Joueur;
import message.ServerShotMessage;
import message.ShotRequest;
import message.ShotResponse;
import message.PlaceShipRequest;

import java.util.ArrayList;
import java.util.List;

public class GameService {

    private Joueur joueurClient;
    private Joueur joueurServeur;

    private final int taille = 10;
    private final java.util.Random random = new java.util.Random();

    private boolean gameOver;
    private String winner;

    private final String clientId; // Pour identifier le client dans les logs
    private String clientUsername = "Joueur";


    public GameService(String clientId) {
        this.clientId = clientId;

    }

    //----------------------------------------------------------------------------------

    /**
     * Cette méthode sert à definir le pseudo du joueur en lui affectant la chaîne passé en paramétre qui est le pseudo
     * */
    public synchronized void setClientUsername(String username) {
        this.clientUsername = username;
        System.out.println("👤 [" + clientId + "] Pseudo défini : " + username);
        // Initialiser la partie APRÈS avoir défini le pseudo
        resetGame();
    }


    /**
     * Cette methode sert à initialisé une partie elle est en synchronized car le serveur initialise une partie à la fois
     * */
    public synchronized void resetGame() {
        System.out.println("🔄 [" + clientUsername + "] Réinitialisation de la partie...");
        gameOver = false;
        winner = null;

        //on utilise le pseudo du joueur
        joueurClient = new Joueur(clientUsername, taille);
        joueurServeur = new Joueur("Serveur", taille);

        placerAleatoire(joueurServeur, new PorteAvion());
        placerAleatoire(joueurServeur, new Croiseur());
        placerAleatoire(joueurServeur, new ContreTorpilleur());
        placerAleatoire(joueurServeur, new Torpilleur());

        System.out.println("⚓ [" + clientUsername + "] Nouvelle grille du SERVEUR :");
        joueurServeur.getGrillePerso().afficher();
    }

    /**
     * Cette methode sert à placer les bâteaux du client coté serveur comme ca il sait ou le client à placer ses bâteau
     * */
    public synchronized boolean placeClientShip(PlaceShipRequest req) {
        // Si la partie n'est pas initialisée
        if (joueurClient == null || joueurServeur == null) {
            System.out.println("⚠️ [" + clientUsername + "] Partie non initialisée, initialisation...");
            resetGame();
        }

        Bâteau b = switch (req.getShipType()) {
            case PORTE_AVION       -> new PorteAvion();
            case CROISEUR          -> new Croiseur();
            case CONTRE_TORPILLEUR -> new ContreTorpilleur();
            case TORPILLEUR        -> new Torpilleur();
        };

        boolean horizontal = (req.getOrientation() == Orientation.HORIZONTAL);
        boolean ok = joueurClient.placerBateau(b, req.getX(), req.getY(), horizontal);

        System.out.println("[" + clientUsername + "] Placement bateau (" + req.getShipType()
                + ") en X=" + (req.getX()+1) + " Y=" + (req.getY()+1) + " "
                + (horizontal ? "H" : "V") + " -> " + (ok ? "OK" : "ECHEC"));

        if (ok) {
            System.out.println("[" + clientUsername + "] Grille de " + clientUsername + " :");
            joueurClient.getGrillePerso().afficher();
        }

        return ok;
    }


    /**
     * Cette mééthode sert à traiter le tire recu
     * */
    public synchronized RoundResult processShot(ShotRequest req) {

        // Si la partie n'est pas initialisée
        if (joueurClient == null || joueurServeur == null) {
            System.out.println("⚠️ [" + clientUsername + "] Partie non initialisée, initialisation...");
            resetGame();
        }

        if (gameOver) {
            System.out.println("⚠️ [" + clientUsername + "] Tir ignoré : partie terminée");
            ShotResponse neutral = new ShotResponse(ResultatTir.MISS, null);
            List<ServerShotMessage> emptyList = new ArrayList<>();
            emptyList.add(new ServerShotMessage(-1, -1, ResultatTir.MISS, null, true, winner));
            return new RoundResult(neutral, emptyList);
        }

        int x = req.getX();
        int y = req.getY();

        System.out.println("🎯 [" + clientUsername + "] Tir de " + clientUsername + " en X=" + (x+1) + " Y=" + (y+1));

        // 1) Tir du client sur le serveur
        Grille grilleServ = joueurServeur.getGrillePerso();
        TirResult trClient = grilleServ.tirer(x, y);

        ResultatTir resultatClient = trClient.getResultat();
        String nomBateauClient = (trClient.getBateau() != null) ? trClient.getBateau().getNom() : null;

        joueurClient.getGrilleTirs().marquerResultatTir(x, y, resultatClient);

        System.out.println("   → [" + clientUsername + "] Résultat : " + resultatClient
                + (nomBateauClient != null ? (" sur " + nomBateauClient) : ""));

        // 2) Check : le serveur a-t-il perdu ?
        if (joueurServeur.aPerdu()) {
            gameOver = true;
            winner = clientUsername; // 🔥 MODIFIÉ : Utiliser le pseudo
            System.out.println("\n════════════════════════════════════");
            System.out.println("💥 [" + clientUsername + "] 🎉 " + clientUsername.toUpperCase() + " A GAGNÉ LA PARTIE ! 🎉");
            System.out.println("════════════════════════════════════\n");

            ShotResponse clientRes = new ShotResponse(resultatClient, nomBateauClient);
            List<ServerShotMessage> serverShots = new ArrayList<>();
            serverShots.add(new ServerShotMessage(-1, -1, ResultatTir.MISS, null, true, winner));
            return new RoundResult(clientRes, serverShots);
        }

        // 3) Si le client a touché, le serveur NE tire PAS
        if (resultatClient == ResultatTir.HIT || resultatClient == ResultatTir.SUNK) {
            System.out.println("✨ [" + clientUsername + "] " + clientUsername + " a touché, il rejoue");

            ShotResponse clientRes = new ShotResponse(resultatClient, nomBateauClient);
            List<ServerShotMessage> serverShots = new ArrayList<>();
            serverShots.add(new ServerShotMessage(-1, -1, ResultatTir.MISS, null, false, null));
            return new RoundResult(clientRes, serverShots);
        }

        // 4) Le client a raté : le serveur tire
        System.out.println("💧 [" + clientUsername + "] " + clientUsername + " a raté, tour du serveur");
        List<ServerShotMessage> serverShots = effectuerTirsServeurConsecutifs();

        ShotResponse clientRes = new ShotResponse(resultatClient, nomBateauClient);
        return new RoundResult(clientRes, serverShots);
    }

    /**
     * Cette permet au serveur de tirer encore tant qu'il a touché un bâteau et retourner la liste des resultat de chaque tir
     * */
    private List<ServerShotMessage> effectuerTirsServeurConsecutifs() {
        List<ServerShotMessage> shots = new ArrayList<>();
        Grille grilleClient = joueurClient.getGrillePerso();
        Grille grilleTirsServeur = joueurServeur.getGrilleTirs();

        while (true) {
            int x, y;
            ResultatTir res;
            String nomBateau = null;

            while (true) {
                x = random.nextInt(taille);
                y = random.nextInt(taille);

                TirResult tr = grilleClient.tirer(x, y);
                res = tr.getResultat();

                if (res == ResultatTir.ALREADY_TRIED || res == ResultatTir.OUT_OF_BOUNDS) {
                    continue;
                }

                nomBateau = (tr.getBateau() != null) ? tr.getBateau().getNom() : null;
                grilleTirsServeur.marquerResultatTir(x, y, res);

                System.out.println("🔥 [" + clientUsername + "] SERVEUR tire en (" + x + "," + y + ")");
                System.out.println("   → [" + clientUsername + "] Résultat : " + res + (nomBateau != null ? (" sur " + nomBateau) : ""));
                break;
            }

            // Check si le client a perdu
            boolean clientLost = joueurClient.aPerdu();
            if (clientLost) {
                gameOver = true;
                winner = "SERVER";
                System.out.println("\n════════════════════════════════════");
                System.out.println("💀 [" + clientUsername + "] 😞 LE SERVEUR A GAGNÉ ! " + clientUsername.toUpperCase() + " A PERDU 😞");
                System.out.println("════════════════════════════════════\n");
                shots.add(new ServerShotMessage(x, y, res, nomBateau, true, winner));
                return shots;
            }

            shots.add(new ServerShotMessage(x, y, res, nomBateau, false, null));

            if (res == ResultatTir.MISS) {
                System.out.println("💧 [" + clientUsername + "] Serveur a raté, tour de " + clientUsername);
                return shots;
            }

            System.out.println("✨ [" + clientUsername + "] Serveur a touché, il tire encore");
        }
    }

    /**
     * Cette méthode permet de placer les bâteau du serveur de maniere aléatroire dans des endroit valides
     * */
    private void placerAleatoire(Joueur joueur, Bâteau bateau) {
        while (true) {
            boolean horizontal = random.nextBoolean();
            int longueur = bateau.getLongueur();

            int maxX = horizontal ? (taille - longueur) : (taille - 1);
            int maxY = horizontal ? (taille - 1) : (taille - longueur);

            int x = random.nextInt(maxX + 1);
            int y = random.nextInt(maxY + 1);

            boolean ok = joueur.placerBateau(bateau, x, y, horizontal);
            if (ok) {
                System.out.println("[" + clientUsername + "] " + bateau.getNom() + " placé en X=" + (x+1) + " Y=" + (y+1) + " "
                        + (horizontal ? "HORIZONTAL" : "VERTICAL"));
                break;
            }
        }
    }

    /**
     * Cette classe encapsule le réponse au tir du client et le resultat de tout les tirs consécutifs du serveur
     * comme ca le client peut mettre à jour sa grille de tir et sa grille perso aprés les tirs
     * */
    public static class RoundResult {
        private final ShotResponse clientResponse;
        private final List<ServerShotMessage> serverShots;

        public RoundResult(ShotResponse clientResponse, List<ServerShotMessage> serverShots) {
            this.clientResponse = clientResponse;
            this.serverShots = serverShots;
        }

        public ShotResponse getClientResponse() { return clientResponse; }
        public List<ServerShotMessage> getServerShots() { return serverShots; }
    }
}