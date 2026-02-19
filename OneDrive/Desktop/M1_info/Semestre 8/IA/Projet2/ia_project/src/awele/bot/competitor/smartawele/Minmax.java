package awele.bot.competitor.smartawele;

import awele.core.Board;
import awele.core.InvalidBotException;

/**
 * Minimax avec élagage Alpha-Beta
 * Rôle : Anticiper la réponse immédiate de l'adversaire
 * Profondeur : 2 (mon coup + réponse adverse)
 */
public class Minmax {

    private static final int PROFONDEUR_MAX = 2;
    private Heuristique heuristique;

    public Minmax() {
        this.heuristique = new Heuristique();
    }

    // -------------------------------------------------------
    // Point d'entrée : évaluer la position future pour le joueurDeReference
    // -------------------------------------------------------
    public double evaluer(Board plateau, int joueurDeReference) {
        return minimax(plateau, plateau.getCurrentPlayer(), joueurDeReference,
                PROFONDEUR_MAX, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                plateau.getCurrentPlayer() == joueurDeReference);
    }

    // -------------------------------------------------------
    // Algorithme Minimax récursif avec élagage Alpha-Beta
    // -------------------------------------------------------
    private double minimax(Board plateau, int joueurActuel, int joueurDeReference,
                           int profondeur, double alpha, double beta,
                           boolean estMaximiseur) {

        // Cas de base : profondeur atteinte
        if (profondeur == 0) {
            return heuristique.evaluer(plateau, joueurDeReference);
        }

        // Vérifier s'il existe des coups valides
        boolean[] coupsValides = plateau.validMoves(joueurActuel);
        boolean aucunCoupValide = true;
        for (int trou = 0; trou < Board.NB_HOLES; trou++) {
            if (coupsValides[trou]) {
                aucunCoupValide = false;
                break;
            }
        }

        if (aucunCoupValide) {
            return heuristique.evaluer(plateau, joueurDeReference);
        }

        if (estMaximiseur) {

            // Notre tour : chercher le score maximum
            double meilleurScore = Double.NEGATIVE_INFINITY;

            for (int trou = 0; trou < Board.NB_HOLES; trou++) {
                if (!coupsValides[trou]) continue;

                Board positionSuivante = simulerCoup(plateau, joueurActuel, trou);
                if (positionSuivante == null) continue;

                int joueurSuivant = Board.otherPlayer(joueurActuel);
                double score = minimax(positionSuivante, joueurSuivant, joueurDeReference,
                        profondeur - 1, alpha, beta, false);

                meilleurScore = Math.max(meilleurScore, score);
                alpha = Math.max(alpha, meilleurScore);
                if (beta <= alpha) break; // élagage beta
            }

            return meilleurScore;

        } else {

            // Tour adverse : chercher le score minimum
            double pireScore = Double.POSITIVE_INFINITY;

            for (int trou = 0; trou < Board.NB_HOLES; trou++) {
                if (!coupsValides[trou]) continue;

                Board positionSuivante = simulerCoup(plateau, joueurActuel, trou);
                if (positionSuivante == null) continue;

                int joueurSuivant = Board.otherPlayer(joueurActuel);
                double score = minimax(positionSuivante, joueurSuivant, joueurDeReference,
                        profondeur - 1, alpha, beta, true);

                pireScore = Math.min(pireScore, score);
                beta = Math.min(beta, pireScore);
                if (beta <= alpha) break; // élagage alpha
            }

            return pireScore;
        }
    }

    // -------------------------------------------------------
    // Simuler un coup et retourner le nouveau plateau
    // -------------------------------------------------------
    private Board simulerCoup(Board plateau, int joueur, int trou) {
        double[] decision = new double[Board.NB_HOLES];
        decision[trou] = 1.0;
        try {
            return plateau.playMoveSimulationBoard(joueur, decision);
        } catch (InvalidBotException e) {
            return null;
        }
    }
}