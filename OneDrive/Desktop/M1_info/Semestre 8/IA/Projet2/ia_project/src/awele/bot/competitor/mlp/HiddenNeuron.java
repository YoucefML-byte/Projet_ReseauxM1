package awele.bot.competitor.mlp;

import java.util.Random;

/**
 * @author Alexandre Blansché
 * Neurone d'une couche cachée ou de la couche de sortie
 */
public class HiddenNeuron extends Neuron
{
    /** Génération pseudo-aléatoire de nombre */
    private static Random random = new Random (System.currentTimeMillis ());

    /** Fonction d'activation */
    private ActivationFunction activationFunction;

    /** Couche de neurone précédente */
    private Neuron [] previousLayer;

    /** Poids des connexions des neurones de la couche précédente */
    private double [] weights;

    /** Gradient de l'erreur du neurone */
    private double gradient;

    /**
     * @param previousLayer Les neurones de la couche précédente
     * @param activationFunction La fonction d'activation du neurone
     */
    public HiddenNeuron (Neuron [] previousLayer, ActivationFunction activationFunction)
    {
        /* On affecte la fonction d'activation */
        this.activationFunction = activationFunction;
        /* On récupère les neurones de la couche précédente... */
        this.previousLayer = new Neuron [previousLayer.length + 1];
        for (int i = 0; i < previousLayer.length; i++)
            this.previousLayer [i] = previousLayer [i];
        /* ... et le neurone de biais */
        this.previousLayer [previousLayer.length] = BiasNeuron.getInstance ();


        this.weights = new double [this.previousLayer.length];
        double scale = Math.sqrt(2.0 / previousLayer.length);
        for (int i = 0; i < this.weights.length; i++)
            this.weights [i] = random.nextGaussian() * scale;
    }

    /**
     * @param previousLayer Les neurones de la couche précédente
     * La fonction d'activation par défaut est la fonction sigmoïde
     */
    public HiddenNeuron (Neuron [] previousLayer)
    {
        this (previousLayer, SigmoidFunction.getInstance ());
    }

    /**
     * @return La fonction d'activation
     */
    public ActivationFunction getActivationFunction ()
    {
        return this.activationFunction;
    }

    /**
     * @return L'erreur de ce neurone à propager lors de l'apprentissage
     */
    public double getGradient ()
    {
        return this.gradient;
    }

    /**
     * @return Le nombre d'entrées du neurone
     */
    public int getNbInputs ()
    {
        return this.weights.length;
    }

    /**
     * @param index L'indice du neurone auquel on veut accéder
     * @return Le neurone de la couche précédente
     */
    public Neuron getPreviousNeuron (int index)
    {
        return this.previousLayer [index];
    }

    /**
     * @param index L'indice du poids auquel on veut accéder
     * @return Le poids de la connexion
     */
    public double getWeight (int index)
    {
        return this.weights [index];
    }

    /**
     * @param gradient L'erreur du neurone
     */
    public void setGradient (double gradient)
    {
        this.gradient = gradient;
    }

    /**
     * @param weight Le nouveau poids de la connexion
     * @param index L'indice du poids que l'on souhaite changer
     */
    public void setWeight (double weight, int index)
    {
        this.weights [index] = weight;
    }
}
