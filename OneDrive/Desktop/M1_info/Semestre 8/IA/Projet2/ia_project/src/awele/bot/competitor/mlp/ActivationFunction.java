package awele.bot.competitor.mlp;

/**
 * Classe abstraite pour la fonction d'activation d'un neurone
 * Adaptée de l'architecture de Alexandre Blansché
 */
public abstract class ActivationFunction {

    /**
     * Activation scalaire (utilisée par Sigmoid sur la couche cachée)
     * @param input La somme pondérée d'un neurone
     * @return L'activation du neurone
     */
    public abstract double getActivation(double input);

    /**
     * Activation vectorielle (utilisée par Softmax sur la couche de sortie)
     * Par défaut, applique getActivation() sur chaque élément
     * Softmax override cette méthode pour un calcul global
     * @param inputs Les sommes pondérées de tous les neurones de la couche
     * @return Les activations de tous les neurones de la couche
     */
    public double[] getActivation(double[] inputs) {
        double[] sorties = new double[inputs.length];
        for (int i = 0; i < inputs.length; i++)
            sorties[i] = this.getActivation(inputs[i]);
        return sorties;
    }

    /**
     * Dérivée de la fonction d'activation (pour la backpropagation)
     * @param output La valeur de sortie du neurone
     * @return La dérivée de l'activation
     */
    public abstract double getDerivative(double output);
}