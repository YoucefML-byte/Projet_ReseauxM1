package awele.bot.competitor.smartawele;

import java.util.Random;

/**
 * MLP Expert (Policy Network)
 * Architecture : 12 -> 16 -> 6
 * Activation : Sigmoid
 * Rôle : Imiter le joueur expert
 */
public class MLP {

    // Dimensions du réseau
    private static final int NB_ENTREES          = 12; // 6 trous joueur + 6 trous adversaire
    private static final int NB_NEURONES_CACHES  = 16;
    private static final int NB_COUPS            = 6;  // un score par coup possible

    // Poids entre la couche d'entrée et la couche cachée
    private double[][] poidsEntreeCachee;   // [NB_NEURONES_CACHES][NB_ENTREES]
    private double[]   biaisCoucheCachee;   // [NB_NEURONES_CACHES]

    // Poids entre la couche cachée et la couche de sortie
    private double[][] poidsCacheeSortie;   // [NB_COUPS][NB_NEURONES_CACHES]
    private double[]   biaisCoucheSortie;   // [NB_COUPS]

    // Taux d'apprentissage
    private double tauxApprentissage;

    // -------------------------------------------------------
    // Constructeur
    // -------------------------------------------------------
    public MLP(double tauxApprentissage) {
        this.tauxApprentissage  = tauxApprentissage;
        this.poidsEntreeCachee  = new double[NB_NEURONES_CACHES][NB_ENTREES];
        this.biaisCoucheCachee  = new double[NB_NEURONES_CACHES];
        this.poidsCacheeSortie  = new double[NB_COUPS][NB_NEURONES_CACHES];
        this.biaisCoucheSortie  = new double[NB_COUPS];
        initialiserPoidsAleatoires();
    }

    // -------------------------------------------------------
    // Initialisation aléatoire des poids (entre -0.5 et 0.5)
    // -------------------------------------------------------
    private void initialiserPoidsAleatoires() {
        // Seed aléatoire pour varier les initialisations
        Random generateur = new Random();

        for (int neurone = 0; neurone < NB_NEURONES_CACHES; neurone++) {
            biaisCoucheCachee[neurone] = generateur.nextDouble() - 0.5;
            for (int entree = 0; entree < NB_ENTREES; entree++)
                poidsEntreeCachee[neurone][entree] = generateur.nextDouble() - 0.5;
        }

        for (int coup = 0; coup < NB_COUPS; coup++) {
            biaisCoucheSortie[coup] = generateur.nextDouble() - 0.5;
            for (int neurone = 0; neurone < NB_NEURONES_CACHES; neurone++)
                poidsCacheeSortie[coup][neurone] = generateur.nextDouble() - 0.5;
        }
    }

    // -------------------------------------------------------
    // Fonction d'activation sigmoid
    // -------------------------------------------------------
    private double sigmoid(double valeur) {
        return 1.0 / (1.0 + Math.exp(-valeur));
    }

    // -------------------------------------------------------
    // Forward pass : plateau (12 valeurs) -> score par coup (6 valeurs)
    // -------------------------------------------------------
    public double[] predire(double[] plateau) {

        // Calcul des activations de la couche cachée
        double[] activationsCachees = new double[NB_NEURONES_CACHES];
        for (int neurone = 0; neurone < NB_NEURONES_CACHES; neurone++) {
            double somme = biaisCoucheCachee[neurone];
            for (int entree = 0; entree < NB_ENTREES; entree++)
                somme += poidsEntreeCachee[neurone][entree] * plateau[entree];
            activationsCachees[neurone] = sigmoid(somme);
        }

        // Calcul des scores de sortie (un par coup possible)
        double[] scoreParCoup = new double[NB_COUPS];
        for (int coup = 0; coup < NB_COUPS; coup++) {
            double somme = biaisCoucheSortie[coup];
            for (int neurone = 0; neurone < NB_NEURONES_CACHES; neurone++)
                somme += poidsCacheeSortie[coup][neurone] * activationsCachees[neurone];
            scoreParCoup[coup] = sigmoid(somme);
        }

        return scoreParCoup;
    }

    // -------------------------------------------------------
    // Entraînement : forward pass + backpropagation
    // -------------------------------------------------------
    public void entrainer(double[] plateau, double[] coupExpert) {

        // === FORWARD PASS ===

        // Activation couche cachée
        double[] activationsCachees = new double[NB_NEURONES_CACHES];
        for (int neurone = 0; neurone < NB_NEURONES_CACHES; neurone++) {
            double somme = biaisCoucheCachee[neurone];
            for (int entree = 0; entree < NB_ENTREES; entree++)
                somme += poidsEntreeCachee[neurone][entree] * plateau[entree];
            activationsCachees[neurone] = sigmoid(somme);
        }

        // Activation couche de sortie
        double[] scoreParCoup = new double[NB_COUPS];
        for (int coup = 0; coup < NB_COUPS; coup++) {
            double somme = biaisCoucheSortie[coup];
            for (int neurone = 0; neurone < NB_NEURONES_CACHES; neurone++)
                somme += poidsCacheeSortie[coup][neurone] * activationsCachees[neurone];
            scoreParCoup[coup] = sigmoid(somme);
        }

        // === BACKPROPAGATION ===

        // Delta couche de sortie : erreur * dérivée sigmoid
        double[] gradientSortie = new double[NB_COUPS];
        for (int coup = 0; coup < NB_COUPS; coup++) {
            double erreur = coupExpert[coup] - scoreParCoup[coup];
            gradientSortie[coup] = erreur * scoreParCoup[coup] * (1.0 - scoreParCoup[coup]);
        }

        // Delta couche cachée : propagation de l'erreur vers l'arrière
        double[] gradientCachee = new double[NB_NEURONES_CACHES];
        for (int neurone = 0; neurone < NB_NEURONES_CACHES; neurone++) {
            double erreurPropagee = 0.0;
            for (int coup = 0; coup < NB_COUPS; coup++)
                erreurPropagee += poidsCacheeSortie[coup][neurone] * gradientSortie[coup];
            gradientCachee[neurone] = activationsCachees[neurone] * (1.0 - activationsCachees[neurone]) * erreurPropagee;
        }

        // === MISE À JOUR DES POIDS ===

        // Couche cachée -> sortie
        for (int coup = 0; coup < NB_COUPS; coup++) {
            biaisCoucheSortie[coup] += tauxApprentissage * gradientSortie[coup];
            for (int neurone = 0; neurone < NB_NEURONES_CACHES; neurone++)
                poidsCacheeSortie[coup][neurone] += tauxApprentissage * gradientSortie[coup] * activationsCachees[neurone];
        }

        // Entrée -> couche cachée
        for (int neurone = 0; neurone < NB_NEURONES_CACHES; neurone++) {
            biaisCoucheCachee[neurone] += tauxApprentissage * gradientCachee[neurone];
            for (int entree = 0; entree < NB_ENTREES; entree++)
                poidsEntreeCachee[neurone][entree] += tauxApprentissage * gradientCachee[neurone] * plateau[entree];
        }
    }

    // -------------------------------------------------------
    // Construire le vecteur d'entrée normalisé à partir du plateau
    // -------------------------------------------------------
    public static double[] construirePlateau(int[] trousJoueur, int[] trousAdversaire) {
        double[] plateau = new double[NB_ENTREES];
        for (int trou = 0; trou < 6; trou++) {
            plateau[trou]     = trousJoueur[trou]     / 48.0; // normalisé entre 0 et 1
            plateau[trou + 6] = trousAdversaire[trou] / 48.0;
        }
        return plateau;
    }

    // -------------------------------------------------------
    // Construire le vecteur cible one-hot à partir du coup expert
    // getMove() retourne 1 à 6, on soustrait 1 pour avoir 0 à 5
    // ex: coup J3 -> getMove()=3 -> index 2 -> [0, 0, 1, 0, 0, 0]
    // -------------------------------------------------------
    public static double[] construireCibleExpert(int coupJoue) {
        double[] cibleExpert = new double[NB_COUPS]; // tout à 0
        int index = coupJoue - 1;                    // conversion 1-6 vers 0-5
        if (index >= 0 && index < NB_COUPS)
            cibleExpert[index] = 1.0;
        return cibleExpert;
    }
}