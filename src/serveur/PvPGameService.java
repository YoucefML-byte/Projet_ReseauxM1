package serveur;

import bâteaux.Bâteau;
import etats.Orientation;
import etats.ResultatTir;
import etats.ShipType;
import grille.Grille;
import grille.Grille.TirResult;
import joueur.Joueur;
import message.*;

import java.io.PrintWriter;

public class PvPGameService {

    private Joueur joueur1;
    private Joueur joueur2;

    private PrintWriter out1; // Pour envoyer messages au joueur 1
    private PrintWriter out2; // Pour envoyer messages au joueur 2

    private boolean tourJoueur1 = true; // true = tour de J1, false = tour de J2

    private boolean gameOver = false;
    private String winner = null;

    private final int taille = 10;

    // Compteurs de bateaux placés
    private int bateauxPlacesJ1 = 0;
    private int bateauxPlacesJ2 = 0;
    private final int NOMBRE_BATEAUX = 4;

    // 🔥 NOUVEAU : Gestion des rematch
    private boolean player1WantsRematch = false;
    private boolean player2WantsRematch = false;
    private boolean rematchInProgress = false;

    public PvPGameService(String username1, String username2, PrintWriter out1, PrintWriter out2) {
        this.joueur1 = new Joueur(username1, taille);
        this.joueur2 = new Joueur(username2, taille);
        this.out1 = out1;
        this.out2 = out2;

        System.out.println("🎮 Partie PvP créée : " + username1 + " vs " + username2);
    }

    /**
     * Placer un bateau pour un joueur
     */
    public synchronized boolean placeShip(PlaceShipRequest req, boolean isPlayer1) {
        Joueur joueur = isPlayer1 ? joueur1 : joueur2;

        Bâteau b = switch (req.getShipType()) {
            case PORTE_AVION       -> new bâteaux.PorteAvion();
            case CROISEUR          -> new bâteaux.Croiseur();
            case CONTRE_TORPILLEUR -> new bâteaux.ContreTorpilleur();
            case TORPILLEUR        -> new bâteaux.Torpilleur();
        };

        boolean horizontal = (req.getOrientation() == Orientation.HORIZONTAL);
        boolean ok = joueur.placerBateau(b, req.getX(), req.getY(), horizontal);

        if (ok) {
            if (isPlayer1) {
                bateauxPlacesJ1++;
            } else {
                bateauxPlacesJ2++;
            }

            System.out.println("[PvP] " + joueur.getNom() + " a placé " + req.getShipType()
                    + " (" + (isPlayer1 ? bateauxPlacesJ1 : bateauxPlacesJ2) + "/" + NOMBRE_BATEAUX + ")");

            // Vérifier si les deux joueurs ont fini de placer
            if (bateauxPlacesJ1 == NOMBRE_BATEAUX && bateauxPlacesJ2 == NOMBRE_BATEAUX) {
                notifierDebutPartie();
            }
        }

        return ok;
    }

    /**
     * Notifier les deux joueurs que la partie commence
     */
    private void notifierDebutPartie() {
        System.out.println("🎮 [PvP] Les deux joueurs sont prêts ! La partie commence.");
        System.out.println("🎯 [PvP] " + joueur1.getNom() + " commence !");

        // Les messages sont maintenant envoyés par le ClientHandler
        // après la dernière PlaceShipRequest
    }

    /**
     * Traiter un tir d'un joueur
     */
    public synchronized PvPRoundResult processShot(ShotRequest req, boolean fromPlayer1) {
        if (gameOver) {
            System.out.println("⚠️ [PvP] Tir ignoré : partie terminée");
            return new PvPRoundResult(
                    new ShotResponse(ResultatTir.MISS, null, true, winner),
                    new OpponentShotMessage(-1, -1, ResultatTir.MISS, null, true, winner, false),
                    true
            );
        }

        // Vérifier que c'est le bon tour
        if ((fromPlayer1 && !tourJoueur1) || (!fromPlayer1 && tourJoueur1)) {
            String joueurActuel = tourJoueur1 ? joueur1.getNom() : joueur2.getNom();
            String tireur_nom = fromPlayer1 ? joueur1.getNom() : joueur2.getNom();
            System.out.println("⚠️ [PvP] " + tireur_nom + " essaie de tirer mais c'est le tour de " + joueurActuel);

            // Retourner un message indiquant que ce n'est pas son tour
            return new PvPRoundResult(
                    new ShotResponse(ResultatTir.MISS, null, false, null),
                    new OpponentShotMessage(-1, -1, ResultatTir.MISS, null, false, null, false),
                    false
            );
        }

        Joueur tireur = fromPlayer1 ? joueur1 : joueur2;
        Joueur cible = fromPlayer1 ? joueur2 : joueur1;

        int x = req.getX();
        int y = req.getY();

        System.out.println("🎯 [PvP] " + tireur.getNom() + " tire en (" + (x+1) + "," + (y+1) + ") sur " + cible.getNom());

        // Tirer sur la grille de la cible
        Grille grilleCible = cible.getGrillePerso();
        TirResult tr = grilleCible.tirer(x, y);

        ResultatTir resultat = tr.getResultat();
        String nomBateau = (tr.getBateau() != null) ? tr.getBateau().getNom() : null;

        // 🔥 SI LE TIR EST INVALIDE (ALREADY_TRIED, OUT_OF_BOUNDS), NE PAS ENVOYER DE MESSAGE À L'ADVERSAIRE
        if (resultat == ResultatTir.ALREADY_TRIED || resultat == ResultatTir.OUT_OF_BOUNDS) {
            System.out.println("   → [PvP] Tir invalide : " + resultat + " - Pas de message à l'adversaire");

            // Le tireur garde son tour et reçoit juste un ShotResponse
            return new PvPRoundResult(
                    new ShotResponse(resultat, nomBateau, false, null),
                    null,  // 🔥 PAS DE MESSAGE POUR L'ADVERSAIRE
                    false
            );
        }

        tireur.getGrilleTirs().marquerResultatTir(x, y, resultat);

        System.out.println("   → [PvP] Résultat : " + resultat + (nomBateau != null ? (" sur " + nomBateau) : ""));

        // 🔥 DÉBOGGAGE : Afficher l'état des bateaux
        if (tr.getBateau() != null) {
            System.out.println("   → [PvP] Bateau : " + tr.getBateau().getNom() +
                    " - Points restants : " + tr.getBateau().getPointsRestants() +
                    " - Coulé : " + tr.getBateau().estCoule());
        };

        // Vérifier si la cible a perdu
        if (cible.aPerdu()) {
            gameOver = true;
            winner = tireur.getNom();
            System.out.println("\n╔═══════════════════════════════════╗");
            System.out.println("🎉 [PvP] " + winner.toUpperCase() + " A GAGNÉ LA PARTIE ! 🎉");
            System.out.println("╚═══════════════════════════════════╝\n");

            return new PvPRoundResult(
                    new ShotResponse(resultat, nomBateau, true, winner),  // 🔥 gameOver=true
                    new OpponentShotMessage(x, y, resultat, nomBateau, true, winner, false),
                    true
            );
        }

        // Déterminer qui joue ensuite
        boolean tireurRejoue = (resultat == ResultatTir.HIT || resultat == ResultatTir.SUNK);

        if (tireurRejoue) {
            System.out.println("✨ [PvP] " + tireur.getNom() + " a touché ! Il rejoue.");
            // Le tour ne change pas
        } else {
            System.out.println("💧 [PvP] " + tireur.getNom() + " a raté ! C'est au tour de " + cible.getNom());
            tourJoueur1 = !tourJoueur1; // Changer de tour
        }

        return new PvPRoundResult(
                new ShotResponse(resultat, nomBateau, false, null),  // 🔥 gameOver=false
                new OpponentShotMessage(x, y, resultat, nomBateau, false, null, !tireurRejoue),
                false
        );
    }

    /**
     * Réinitialiser la partie
     */
    public synchronized void resetGame() {
        System.out.println("🔄 [PvP] Réinitialisation de la partie " + joueur1.getNom() + " vs " + joueur2.getNom());

        gameOver = false;
        winner = null;
        tourJoueur1 = true;
        bateauxPlacesJ1 = 0;
        bateauxPlacesJ2 = 0;

        // 🔥 Réinitialiser les rematch
        player1WantsRematch = false;
        player2WantsRematch = false;
        rematchInProgress = false;

        String username1 = joueur1.getNom();
        String username2 = joueur2.getNom();

        joueur1 = new Joueur(username1, taille);
        joueur2 = new Joueur(username2, taille);
    }

    /**
     * 🔥 NOUVEAU : Gérer la demande de rematch d'un joueur
     * @return "WAITING" si on attend l'autre joueur, "ACCEPTED" si les deux veulent, "READY" si on peut commencer
     */
    public synchronized String requestRematch(boolean fromPlayer1) {
        if (fromPlayer1) {
            player1WantsRematch = true;
            System.out.println("🔄 [PvP] " + joueur1.getNom() + " veut rejouer");
        } else {
            player2WantsRematch = true;
            System.out.println("🔄 [PvP] " + joueur2.getNom() + " veut rejouer");
        }

        // Les deux veulent rejouer ?
        if (player1WantsRematch && player2WantsRematch) {
            rematchInProgress = true;
            System.out.println("✅ [PvP] Les deux joueurs veulent rejouer !");
            return "ACCEPTED";
        } else {
            return "WAITING";
        }
    }

    /**
     * 🔥 NOUVEAU : Annuler la demande de rematch (déconnexion ou refus)
     */
    public synchronized void cancelRematch(boolean fromPlayer1) {
        String playerName = fromPlayer1 ? joueur1.getNom() : joueur2.getNom();
        System.out.println("❌ [PvP] " + playerName + " a annulé/refusé le rematch");

        player1WantsRematch = false;
        player2WantsRematch = false;
        rematchInProgress = false;
    }

    /**
     * 🔥 NOUVEAU : Vérifier si un rematch est en cours
     */
    public synchronized boolean isRematchInProgress() {
        return rematchInProgress;
    }

    /**
     * 🔥 NOUVEAU : Vérifier si l'adversaire veut rejouer
     */
    public synchronized boolean doesOpponentWantRematch(boolean checkingPlayer1) {
        return checkingPlayer1 ? player2WantsRematch : player1WantsRematch;
    }

    public String getJoueur1Name() {
        return joueur1.getNom();
    }

    public String getJoueur2Name() {
        return joueur2.getNom();
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public String getWinner() {
        return winner;
    }

    public boolean isBothPlayersReady() {
        return bateauxPlacesJ1 == NOMBRE_BATEAUX && bateauxPlacesJ2 == NOMBRE_BATEAUX;
    }

    /**
     * Classe pour retourner le résultat d'un tour PvP
     */
    public static class PvPRoundResult {
        private final ShotResponse shooterResponse;      // Réponse pour le tireur
        private final OpponentShotMessage opponentMsg;   // Message pour l'adversaire
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