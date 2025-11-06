package grille;

import bâteaux.Bâteau;
import etats.CelleState;

public class Grille {
    private final int taille;
    private final Celle[][] cellules;

    public Grille(int taille) {
        this.taille = taille;
        cellules = new Celle[taille][taille];
        for (int i = 0; i < taille; i++)
            for (int j = 0; j < taille; j++)
                cellules[i][j] = new Celle(i, j);
    }

    public boolean placerBateau(Bâteau bateau, int x, int y, boolean horizontal) {
        int longueur = bateau.getLongueur();

        if (horizontal) {
            if (y + longueur > taille) return false;
            for (int j = 0; j < longueur; j++)
                if (!cellules[x][y + j].estLibre()) return false;
            for (int j = 0; j < longueur; j++)
                //pou savoir quel est le bâteau qui occupe cette case
                cellules[x][y + j].setBâteau(bateau);
        } else {
            if (x + longueur > taille) return false;
            for (int i = 0; i < longueur; i++)
                if (!cellules[x + i][y].estLibre()) return false;
            for (int i = 0; i < longueur; i++)
                cellules[x + i][y].setBâteau(bateau);
        }
        return true;
    }
    //il manque la fonction qui gere les résultat aprés le tir d'un joueur

    public void afficher() {
        for (int i = 0; i < taille; i++) {
            for (int j = 0; j < taille; j++) {
                System.out.print(cellules[i][j] + " ");
            }
            System.out.println();
        }
    }
}