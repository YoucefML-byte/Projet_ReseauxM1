package awele.bot.competitor.smartawele;

import awele.bot.CompetitorBot;
import awele.core.Board;
import awele.core.InvalidBotException;
import awele.data.AweleData;
import awele.data.AweleObservation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bot Awélé - Architecture Hybride à 3 Composants
 * Score(coup) = 0.30 * MLP(coup) + 0.45 * Heuristique(position_future) + 0.25 * Minimax(position_future)
 */
public class MonBot extends CompetitorBot {

    // -------------------------------------------------------
    // Coefficients de pondération des 3 composants
    // -------------------------------------------------------
    private static final double POIDS_MLP         = 0.25;
    private static final double POIDS_HEURISTIQUE = 0.47;
    private static final double POIDS_MINIMAX     = 0.38;

    // -------------------------------------------------------
    // Paramètres d'entraînement du MLP
    // -------------------------------------------------------
    private static final double TAUX_APPRENTISSAGE = 0.05;
    private static final int    NB_EPOQUES         = 40000;

    // -------------------------------------------------------
    // Les 3 composants
    // -------------------------------------------------------
    private MLP         mlp;
    private Heuristique heuristique;
    private Minmax     minimax;

    // -------------------------------------------------------
    // Constructeur : identification du bot
    // -------------------------------------------------------
    public MonBot() throws InvalidBotException {
        addAuthor("Youcef Melakhessou");
        addAuthor("Abd'Naim Oubaassine");
        setBotName("SmartAwele");
    }

    // -------------------------------------------------------
    // Apprentissage : exécuté une seule fois au chargement
    // -------------------------------------------------------
    @Override
    public void learn() {

        // Initialiser les 3 composants
        mlp         = new MLP(TAUX_APPRENTISSAGE);
        heuristique = new Heuristique();
        minimax     = new Minmax();

        // Charger les 303 observations
        AweleData donneesOriginales = AweleData.getInstance();
        List<AweleObservation> donnees = new ArrayList<>(donneesOriginales);

        System.out.println("Début de l'entraînement du MLP sur " + donnees.size() + " observations...");

        // Entraîner le MLP sur NB_EPOQUES époques
        for (int epoque = 0; epoque < NB_EPOQUES; epoque++) {

            // Mélanger les données à chaque époque
            Collections.shuffle(donnees);

            double erreurTotale = 0.0;

            for (AweleObservation observation : donnees) {

                // Construire le vecteur d'entrée normalisé
                double[] plateau = MLP.construirePlateau(
                        observation.getPlayerHoles(),
                        observation.getOppenentHoles()
                );

                // Construire la cible one-hot du coup expert
                double[] cibleExpert = MLP.construireCibleExpert(observation.getMove());

                // Entraîner le MLP
                mlp.entrainer(plateau, cibleExpert);

                // Calculer l'erreur pour suivi
                double[] prediction = mlp.predire(plateau);
                for (int coup = 0; coup < 6; coup++) {
                    double diff = cibleExpert[coup] - prediction[coup];
                    erreurTotale += diff * diff;
                }
            }

            // Afficher l'erreur toutes les 500 époques
            if (epoque % 500 == 0) {
                System.out.println("Epoque " + epoque + " / " + NB_EPOQUES
                        + " - Erreur : " + String.format("%.4f", erreurTotale));
            }
        }

        System.out.println("Entraînement terminé !");
    }

    // -------------------------------------------------------
    // Initialisation avant chaque partie
    // -------------------------------------------------------
    @Override
    public void initialize() {
        // Rien à initialiser entre les parties
    }

    // -------------------------------------------------------
    // Finalisation après chaque partie
    // -------------------------------------------------------
    @Override
    public void finish() {
        // Rien à faire
    }

    // -------------------------------------------------------
    // Prise de décision : retourne un score pour chacun des 6 coups
    // -------------------------------------------------------
    @Override
    public double[] getDecision(Board board) {

        int joueurActuel = board.getCurrentPlayer();
        double[] scoreFinal = new double[Board.NB_HOLES];

        // Scores du MLP pour la position actuelle
        double[] plateau = MLP.construirePlateau(
                board.getPlayerHoles(),
                board.getOpponentHoles()
        );
        double[] scoresMLP = mlp.predire(plateau);

        // Coups valides
        boolean[] coupsValides = board.validMoves(joueurActuel);

        for (int trou = 0; trou < Board.NB_HOLES; trou++) {

            if (!coupsValides[trou]) {
                scoreFinal[trou] = Double.NEGATIVE_INFINITY;
                continue;
            }

            // Simuler le coup pour obtenir la position future
            Board positionFuture = simulerCoup(board, joueurActuel, trou);
            if (positionFuture == null) {
                scoreFinal[trou] = Double.NEGATIVE_INFINITY;
                continue;
            }

            // Composant 1 : score MLP
            double scoreMLP = scoresMLP[trou];

            // Composant 2 : heuristique du point de vue du joueurActuel
            double scoreHeuristique = heuristique.evaluer(positionFuture, joueurActuel);

            // Composant 3 : minimax du point de vue du joueurActuel
            double scoreMinimax = minimax.evaluer(positionFuture, joueurActuel);

            // Combinaison pondérée
            scoreFinal[trou] = POIDS_MLP         * scoreMLP
                    + POIDS_HEURISTIQUE * scoreHeuristique
                    + POIDS_MINIMAX     * scoreMinimax;
        }

        return scoreFinal;
    }

    // -------------------------------------------------------
    // Simuler un coup sur une copie du plateau
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