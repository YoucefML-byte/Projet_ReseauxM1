package joueur;

import bâteaux.Bâteau;
import etats.ResultatTir;
import grille.Grille;

import java.util.ArrayList;
import java.util.List;

public class Joueur {

    private final String nom;
    private final Grille grillePerso;    // où sont SES bateaux
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
     * Le joueur veut placer un bateau :
     * il INDique où, mais c'est la grille qui fait le travail.
     */
    public boolean placerBateau(Bâteau bateau, int x, int y, boolean horizontal) {
        boolean ok = grillePerso.placerBateau(bateau, x, y, horizontal);
        if (ok) {
            bateaux.add(bateau);
        }
        return ok;
    }

    /**
     * Le joueur SUBIT un tir de l'adversaire sur sa grille perso.
     * On retourne le résultat du tir (TOUCHE / RATE / COULE).
     */
    public ResultatTir recevoirTir(int x, int y) {
        return grillePerso.tirerSurMoi(x, y); // à implémenter côté Grille
    }

    /**
     * Le joueur TIRE sur l'adversaire.
     * On enregistre le résultat sur sa grille de tirs.
     */
    public void enregistrerResultatTir(int x, int y, ResultatTir resultat) {
        grilleTirs.marquerResultatTir(x, y, resultat); // idem, méthode côté Grille
    }

    /**
     * Savoir si le joueur a perdu (tous ses bateaux sont coulés).
     */
    public boolean aPerdu() {
        for (Bâteau b : bateaux) {
            if (!b.estCoule()) {
                return false;
            }
        }
        return true;
    }

    public void afficherGrilles() {
        System.out.println("Grille de " + nom + " (ses bateaux) :");
        grillePerso.afficher();

        System.out.println("Grille de tirs de " + nom + " :");
        grilleTirs.afficher();
    }
}
