package awele.bot.competitor.mlp;

/**
 * Fonction d'activation Sigmoïde - Classe singleton
 * Utilisée sur la couche cachée du MLP
 * Adaptée de l'architecture de Alexandre Blansché
 */
public class SigmoidFunction extends ActivationFunction {

    private static SigmoidFunction instance = null;

    public static SigmoidFunction getInstance() {
        if (SigmoidFunction.instance == null)
            SigmoidFunction.instance = new SigmoidFunction();
        return SigmoidFunction.instance;
    }

    /**
     * Calcul de la sigmoïde : 1 / (1 + e^-x)
     */
    @Override
    public double getActivation(double input) {
        return 1.0 / (1.0 + Math.exp(-input));
    }

    /**
     * Dérivée de la sigmoïde : sortie * (1 - sortie)
     */
    @Override
    public double getDerivative(double output) {
        return output * (1.0 - output);
    }
}