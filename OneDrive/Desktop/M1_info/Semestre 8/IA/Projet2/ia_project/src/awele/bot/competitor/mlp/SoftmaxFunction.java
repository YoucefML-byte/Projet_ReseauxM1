package awele.bot.competitor.mlp;

/**
 * Fonction d'activation Softmax - Classe singleton
 * Utilisée sur la couche de sortie du MLP
 * Transforme les 6 scores bruts en probabilités qui somment à 1
 * Adaptée de l'architecture de Alexandre Blansché
 */
public class SoftmaxFunction extends ActivationFunction {

    private static SoftmaxFunction instance = null;

    public static SoftmaxFunction getInstance() {
        if (SoftmaxFunction.instance == null)
            SoftmaxFunction.instance = new SoftmaxFunction();
        return SoftmaxFunction.instance;
    }

    /**
     * Non utilisé pour Softmax car elle opère sur un vecteur entier
     * Override obligatoire car méthode abstraite dans ActivationFunction
     */
    @Override
    public double getActivation(double input) {
        return Math.exp(input); // exponentielle brute, utilisée en interne
    }

    /**
     * Calcul du Softmax sur un vecteur de sommes pondérées
     * Override de la méthode vectorielle de ActivationFunction
     * Retourne un tableau de probabilités qui somment à 1
     */
    @Override
    public double[] getActivation(double[] sommes) {

        // Soustraction du max pour stabilité numérique
        // (évite les débordements avec Math.exp sur de grandes valeurs)
        double max = Double.NEGATIVE_INFINITY;
        for (double s : sommes)
            if (s > max) max = s;

        // Calcul des exponentielles
        double[] exponentielles = new double[sommes.length];
        double sommeExp = 0;
        for (int i = 0; i < sommes.length; i++) {
            exponentielles[i] = Math.exp(sommes[i] - max);
            sommeExp += exponentielles[i];
        }

        // Normalisation : chaque valeur divisée par la somme totale
        double[] probabilites = new double[sommes.length];
        for (int i = 0; i < sommes.length; i++)
            probabilites[i] = exponentielles[i] / sommeExp;

        return probabilites;
    }

    /**
     * Dérivée de Softmax combinée avec Cross-Entropy
     * Se simplifie en : erreur brute (cible - sortie)
     */
    @Override
    public double getDerivative(double output) {
        return output; // output ici est directement l'erreur brute
    }
}