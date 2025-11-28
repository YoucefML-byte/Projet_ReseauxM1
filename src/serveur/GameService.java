package serveur;

import bâteaux.*;
import etats.Orientation;
import etats.ResultatTir;
import grille.Grille;
import grille.Grille.TirResult;
import joueur.Joueur;
import message.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameService {
    private Joueur joueurClient;
    private Joueur joueurServeur;

    private final int taille = 10;
    private final Random random = new Random();

    private boolean gameOver;
    private String winner;

    private final String clientUsername;

    public GameService(String username) {
        this.clientUsername = username;
        resetGame();
    }

    public synchronized void resetGame() {
        System.out.println("🔄 [" + clientUsername + "] Réinitialisation de la partie...");
        gameOver = false;
        winner = null;

        joueurClient = new Joueur(clientUsername, taille);
        joueurServeur = new Joueur("Serveur", taille);

        // Placement aléatoire des bateaux du serveur
        placerAleatoire(joueurServeur, new PorteAvion());
        placerAleatoire(joueurServeur, new Croiseur());
        placerAleatoire(joueurServeur, new ContreTorpilleur());
        placerAleatoire(joueurServeur, new Torpilleur());

        // 🔥 AFFICHER LA GRILLE DU SERVEUR (visible uniquement dans la console serveur)
        System.out.println("\n⚓ [" + clientUsername + "] Grille du SERVEUR :");
        joueurServeur.getGrillePerso().afficher();
        System.out.println();
    }

    public synchronized boolean placeClientShip(PlaceShipRequest req) {
        Bâteau b = creerBateau(req.getShipType());
        boolean horizontal = (req.getOrientation() == Orientation.HORIZONTAL);
        boolean ok = joueurClient.placerBateau(b, req.getX(), req.getY(), horizontal);

        if (ok) {
            System.out.println("[" + clientUsername + "] " + req.getShipType() + " placé en "
                    + (req.getX()+1) + "," + (req.getY()+1) + " " + (horizontal ? "H" : "V"));
        }
        return ok;
    }

    public synchronized RoundResult processShot(ShotRequest req) {
        if (gameOver) {
            return creerRoundResultGameOver();
        }

        int x = req.getX();
        int y = req.getY();

        // 1) Tir du client sur le serveur
        TirResult trClient = joueurServeur.getGrillePerso().tirer(x, y);
        ResultatTir resultat = trClient.getResultat();
        String nomBateau = (trClient.getBateau() != null) ? trClient.getBateau().getNom() : null;

        joueurClient.getGrilleTirs().marquerResultatTir(x, y, resultat);

        // 2) Vérifier victoire immédiate
        if (joueurServeur.aPerdu()) {
            return gererVictoireClient(resultat, nomBateau);
        }

        // 3) Client touché = il rejoue
        if (resultat == ResultatTir.HIT || resultat == ResultatTir.SUNK) {
            return new RoundResult(
                    new ShotResponse(resultat, nomBateau),
                    List.of(new ServerShotMessage(-1, -1, ResultatTir.MISS, null, false, null))
            );
        }

        // 4) Client raté = tour du serveur
        return new RoundResult(
                new ShotResponse(resultat, nomBateau),
                effectuerTirsServeurConsecutifs()
        );
    }

    private List<ServerShotMessage> effectuerTirsServeurConsecutifs() {
        List<ServerShotMessage> shots = new ArrayList<>();
        Grille grilleClient = joueurClient.getGrillePerso();

        while (true) {
            // Trouver des coordonnées valides
            int x, y;
            TirResult tr;

            while (true) {
                x = random.nextInt(taille);
                y = random.nextInt(taille);
                tr = grilleClient.tirer(x, y);

                if (tr.getResultat() != ResultatTir.ALREADY_TRIED &&
                        tr.getResultat() != ResultatTir.OUT_OF_BOUNDS) {
                    break;
                }
            }

            ResultatTir res = tr.getResultat();
            String nomBateau = (tr.getBateau() != null) ? tr.getBateau().getNom() : null;
            joueurServeur.getGrilleTirs().marquerResultatTir(x, y, res);

            // Vérifier défaite du client
            if (joueurClient.aPerdu()) {
                gameOver = true;
                winner = "SERVER";
                System.out.println("💀 [" + clientUsername + "] SERVEUR A GAGNÉ !");
                shots.add(new ServerShotMessage(x, y, res, nomBateau, true, winner));
                return shots;
            }

            shots.add(new ServerShotMessage(x, y, res, nomBateau, false, null));

            // Serveur raté = fin de son tour
            if (res == ResultatTir.MISS) {
                return shots;
            }
        }
    }

    private void placerAleatoire(Joueur joueur, Bâteau bateau) {
        while (true) {
            boolean horizontal = random.nextBoolean();
            int longueur = bateau.getLongueur();
            int maxX = horizontal ? (taille - longueur) : (taille - 1);
            int maxY = horizontal ? (taille - 1) : (taille - longueur);
            int x = random.nextInt(maxX + 1);
            int y = random.nextInt(maxY + 1);

            if (joueur.placerBateau(bateau, x, y, horizontal)) {
                break;
            }
        }
    }

    private RoundResult gererVictoireClient(ResultatTir resultat, String nomBateau) {
        gameOver = true;
        winner = clientUsername;
        System.out.println("🎉 [" + clientUsername + "] A GAGNÉ LA PARTIE !");

        return new RoundResult(
                new ShotResponse(resultat, nomBateau, true, winner),
                List.of(new ServerShotMessage(-1, -1, ResultatTir.MISS, null, true, winner))
        );
    }

    private RoundResult creerRoundResultGameOver() {
        return new RoundResult(
                new ShotResponse(ResultatTir.MISS, null),
                List.of(new ServerShotMessage(-1, -1, ResultatTir.MISS, null, true, winner))
        );
    }

    private Bâteau creerBateau(etats.ShipType type) {
        return switch (type) {
            case PORTE_AVION -> new PorteAvion();
            case CROISEUR -> new Croiseur();
            case CONTRE_TORPILLEUR -> new ContreTorpilleur();
            case TORPILLEUR -> new Torpilleur();
        };
    }

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