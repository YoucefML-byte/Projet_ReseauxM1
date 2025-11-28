package serveur;

import grille.Grille.*;
import bâteaux.Bâteau;
import etats.Orientation;
import etats.ResultatTir;
import grille.Grille;
import joueur.Joueur;
import message.OpponentShotMessage;
import message.PlaceShipRequest;
import message.ShotRequest;
import message.ShotResponse;

import java.io.PrintWriter;

class PvPGameService {
    private Joueur joueur1, joueur2;
    private PrintWriter out1, out2;
    private boolean tourJoueur1 = true;
    private boolean gameOver = false;
    private String winner = null;
    private final int taille = 10;
    private int bateauxPlacesJ1 = 0, bateauxPlacesJ2 = 0;
    private final int NOMBRE_BATEAUX = 4;
    private boolean player1WantsRematch = false, player2WantsRematch = false;

    public PvPGameService(String username1, String username2, PrintWriter out1, PrintWriter out2) {
        this.joueur1 = new Joueur(username1, taille);
        this.joueur2 = new Joueur(username2, taille);
        this.out1 = out1;
        this.out2 = out2;
        System.out.println("🎮 Partie PvP créée : " + username1 + " vs " + username2);
    }

    public synchronized boolean placeShip(PlaceShipRequest req, boolean isPlayer1) {
        Joueur joueur = isPlayer1 ? joueur1 : joueur2;
        Bâteau b = creerBateau(req.getShipType());
        boolean horizontal = (req.getOrientation() == Orientation.HORIZONTAL);
        boolean ok = joueur.placerBateau(b, req.getX(), req.getY(), horizontal);

        if (ok) {
            if (isPlayer1) bateauxPlacesJ1++;
            else bateauxPlacesJ2++;

            System.out.println("[PvP] " + joueur.getNom() + " a placé " + req.getShipType());
        }
        return ok;
    }

    public synchronized PvPRoundResult processShot(ShotRequest req, boolean fromPlayer1) {
        if (gameOver) {
            return creerRoundResultGameOver();
        }

        // Vérifier que c'est le bon tour
        if ((fromPlayer1 && !tourJoueur1) || (!fromPlayer1 && tourJoueur1)) {
            return new PvPRoundResult(
                    new ShotResponse(ResultatTir.MISS, null, false, null),
                    new OpponentShotMessage(-1, -1, ResultatTir.MISS, null, false, null, false),
                    false
            );
        }

        Joueur tireur = fromPlayer1 ? joueur1 : joueur2;
        Joueur cible = fromPlayer1 ? joueur2 : joueur1;

        // Effectuer le tir
        TirResult tr = cible.getGrillePerso().tirer(req.getX(), req.getY());
        ResultatTir resultat = tr.getResultat();
        String nomBateau = (tr.getBateau() != null) ? tr.getBateau().getNom() : null;

        // Tir invalide = pas de message à l'adversaire
        if (resultat == ResultatTir.ALREADY_TRIED || resultat == ResultatTir.OUT_OF_BOUNDS) {
            return new PvPRoundResult(
                    new ShotResponse(resultat, nomBateau, false, null),
                    null,
                    false
            );
        }

        tireur.getGrilleTirs().marquerResultatTir(req.getX(), req.getY(), resultat);

        // Vérifier victoire
        if (cible.aPerdu()) {
            gameOver = true;
            winner = tireur.getNom();
            System.out.println("🎉 [PvP] " + winner + " A GAGNÉ !");

            return new PvPRoundResult(
                    new ShotResponse(resultat, nomBateau, true, winner),
                    new OpponentShotMessage(req.getX(), req.getY(), resultat, nomBateau, true, winner, false),
                    true
            );
        }

        // Gérer changement de tour
        boolean tireurRejoue = (resultat == ResultatTir.HIT || resultat == ResultatTir.SUNK);
        if (!tireurRejoue) {
            tourJoueur1 = !tourJoueur1;
        }

        return new PvPRoundResult(
                new ShotResponse(resultat, nomBateau, false, null),
                new OpponentShotMessage(req.getX(), req.getY(), resultat, nomBateau, false, null, !tireurRejoue),
                false
        );
    }

    public synchronized void resetGame() {
        gameOver = false;
        winner = null;
        tourJoueur1 = true;
        bateauxPlacesJ1 = 0;
        bateauxPlacesJ2 = 0;
        player1WantsRematch = false;
        player2WantsRematch = false;

        String username1 = joueur1.getNom();
        String username2 = joueur2.getNom();
        joueur1 = new Joueur(username1, taille);
        joueur2 = new Joueur(username2, taille);
    }

    public synchronized String requestRematch(boolean fromPlayer1) {
        if (fromPlayer1) player1WantsRematch = true;
        else player2WantsRematch = true;

        return (player1WantsRematch && player2WantsRematch) ? "ACCEPTED" : "WAITING";
    }

    public synchronized void cancelRematch(boolean fromPlayer1) {
        player1WantsRematch = false;
        player2WantsRematch = false;
    }

    public synchronized boolean doesOpponentWantRematch(boolean checkingPlayer1) {
        return checkingPlayer1 ? player2WantsRematch : player1WantsRematch;
    }

    private PvPRoundResult creerRoundResultGameOver() {
        return new PvPRoundResult(
                new ShotResponse(ResultatTir.MISS, null, true, winner),
                new OpponentShotMessage(-1, -1, ResultatTir.MISS, null, true, winner, false),
                true
        );
    }

    private Bâteau creerBateau(etats.ShipType type) {
        return switch (type) {
            case PORTE_AVION -> new bâteaux.PorteAvion();
            case CROISEUR -> new bâteaux.Croiseur();
            case CONTRE_TORPILLEUR -> new bâteaux.ContreTorpilleur();
            case TORPILLEUR -> new bâteaux.Torpilleur();
        };
    }

    public String getJoueur1Name() { return joueur1.getNom(); }
    public String getJoueur2Name() { return joueur2.getNom(); }
    public boolean isBothPlayersReady() { return bateauxPlacesJ1 == NOMBRE_BATEAUX && bateauxPlacesJ2 == NOMBRE_BATEAUX; }

    public static class PvPRoundResult {
        private final ShotResponse shooterResponse;
        private final OpponentShotMessage opponentMsg;
        private final boolean gameOver;

        public PvPRoundResult(ShotResponse shooterResponse, OpponentShotMessage opponentMsg, boolean gameOver) {
            this.shooterResponse = shooterResponse;
            this.opponentMsg = opponentMsg;
            this.gameOver = gameOver;
        }

        public ShotResponse getShooterResponse() { return shooterResponse; }
        public OpponentShotMessage getOpponentMsg() { return opponentMsg; }
        public boolean isGameOver() { return gameOver; }
    }
}