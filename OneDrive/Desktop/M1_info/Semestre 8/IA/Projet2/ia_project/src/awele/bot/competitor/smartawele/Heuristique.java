package awele.bot.competitor.smartawele;

import awele.core.Board;

/**
 * Heuristique d'évaluation de position
 * Rôle : Évaluer la qualité d'une position pour un joueur donné
 * Retourne une valeur normalisée entre -1 et 1
 */
public class Heuristique {

    // Poids de chaque critère
    private static final double POIDS_AVANTAGE_SCORE   = 1.0;
    private static final double POIDS_RICHESSE_CAMP    = 0.3;
    private static final double POIDS_MENACES_ADVERSES = 0.4;
    private static final double POIDS_MOBILITE         = 0.2;

    // Valeur maximale pour la normalisation
    private static final double VALEUR_MAX = 48.0 * POIDS_AVANTAGE_SCORE
            + 48.0 * POIDS_RICHESSE_CAMP
            + 6.0  * POIDS_MENACES_ADVERSES
            + 6.0  * POIDS_MOBILITE;

    // -------------------------------------------------------
    // Évaluation d'un plateau du point de vue d'un joueur donné
    // joueurDeReference : le joueur POUR LEQUEL on évalue (pas forcément le currentPlayer)
    // Retourne une valeur entre -1 (très mauvais) et 1 (très bon)
    // -------------------------------------------------------
    public double evaluer(Board plateau, int joueurDeReference) {

        int joueurAdverse = Board.otherPlayer(joueurDeReference);

        // Récupérer les trous selon le joueur de référence
        // On doit accéder aux trous via currentPlayer, donc on adapte
        int currentPlayer = plateau.getCurrentPlayer();

        int[] trousJoueur;
        int[] trousAdversaire;

        if (currentPlayer == joueurDeReference) {
            // Le joueur de référence est le joueur courant
            trousJoueur     = plateau.getPlayerHoles();
            trousAdversaire = plateau.getOpponentHoles();
        } else {
            // Le joueur de référence est l'adversaire du joueur courant
            trousJoueur     = plateau.getOpponentHoles();
            trousAdversaire = plateau.getPlayerHoles();
        }

        // Critère 1 : avantage en score capturé
        int scoreJoueur   = plateau.getScore(joueurDeReference);
        int scoreAdversaire = plateau.getScore(joueurAdverse);
        double avantageSurScore = scoreJoueur - scoreAdversaire;

        // Critère 2 : richesse du camp du joueur de référence
        double grainesMonCamp = 0;
        for (int trou = 0; trou < Board.NB_HOLES; trou++)
            grainesMonCamp += trousJoueur[trou];

        // Critère 3 : menaces adverses (trous adverses à 1 ou 2 graines = capturables)
        double menacesAdverses = 0;
        for (int trou = 0; trou < Board.NB_HOLES; trou++) {
            int graines = trousAdversaire[trou];
            if (graines == 1 || graines == 2)
                menacesAdverses++;
        }

        // Critère 4 : mobilité du joueur de référence
        boolean[] coupsValides = plateau.validMoves(joueurDeReference);
        double mobilite = 0;
        for (int trou = 0; trou < Board.NB_HOLES; trou++)
            if (coupsValides[trou])
                mobilite++;

        // Score heuristique brut
        double scoreHeuristique = POIDS_AVANTAGE_SCORE   *  avantageSurScore
                + POIDS_RICHESSE_CAMP    *  grainesMonCamp
                - POIDS_MENACES_ADVERSES *  menacesAdverses
                + POIDS_MOBILITE         *  mobilite;

        return normaliser(scoreHeuristique);
    }

    // -------------------------------------------------------
    // Normalisation entre -1 et 1
    // -------------------------------------------------------
    private double normaliser(double valeur) {
        double valeurBornee = Math.max(-VALEUR_MAX, Math.min(VALEUR_MAX, valeur));
        return valeurBornee / VALEUR_MAX;
    }
}