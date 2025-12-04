package joueur;

import bâteaux.Bâteau;
import etats.ResultatTir;
import grille.Grille;

import java.util.ArrayList;
import java.util.List;

public class Joueur {

    private final String nom;              //le nom du joueur
    private final Grille grillePerso;    // où sont ses bateaux
    private final Grille grilleTirs;     // ce qu'il sait de l'adversaire
    private final List<Bâteau> bateaux;  // ses bateaux

    public Joueur(String nom, int tailleGrille) {
        this.nom = nom;
        this.grillePerso = new Grille(tailleGrille);
        this.grilleTirs = new Grille(tailleGrille);
        this.bateaux = new ArrayList<>();
    }

    public String getNom() {
        return nom;
    }

    public Grille getGrillePerso() {
        return grillePerso;
    }

    public Grille getGrilleTirs() {
        return grilleTirs;
    }

    public List<Bâteau> getBateaux() {
        return bateaux;
    }

    /**
     * cette fonction permet au joueur de placer un bâteau sur sa grille
     */
    public boolean placerBateau(Bâteau bateau, int x, int y, boolean horizontal) {
        boolean ok = grillePerso.placerBateau(bateau, x, y, horizontal);
        if (ok) {
            bateaux.add(bateau);
        }
        return ok;
    }

    /**
     * Cette fonction permet au joueur de recevoir un tir de l'adversaire pour pouvoir mettre à jours sa grille personnels
     * la ou y a ses bâteaux à jours
     */
    public ResultatTir recevoirTir(int x, int y) {
        return grillePerso.tirerSurMoi(x, y);
    }

    /**
     * cette fonction sert à enregistrer les tirs effectué par le joueur
     * pour savoir àaprés si il a déja tiré sur cette case ou non
     */
    public void enregistrerResultatTir(int x, int y, ResultatTir resultat) {
        grilleTirs.marquerResultatTir(x, y, resultat);
    }

    /**
     * cette fonction sert à indiquer si le joueur à perdu ou non
     * */
    public boolean aPerdu() {
        if (bateaux.isEmpty()) {
            // Aucun bateau enregistré → on considère qu'il n'a pas encore perdu
            return false;
        }
        for (Bâteau b : bateaux) {
            //si y a un bâteau qui n a pas encore coulé ca veut il n a pas perdu encore
            if (!b.estCoule()) {
                return false;
            }
        }
        return true;
    }

    /**
    * cette fonction permet d'afficher la grille ou le joueur à placer ces bâteaux
    * et aussi la grille ou il doit tirer
    * */
    public void afficherGrilles() {
        System.out.println("Grille de " + nom + " (ses bateaux) :");
        grillePerso.afficher();

        System.out.println("Grille de tirs de " + nom + " :");
        grilleTirs.afficher();
    }
}