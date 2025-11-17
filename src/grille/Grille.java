package grille;

import bâteaux.Bâteau;
import etats.Orientation;
import etats.ResultatTir;

public class Grille {
    private final int largeur;
    private final int hauteur;

    private final Bâteau[][] cases;  //une tableau à deux dimensions qui stock les coordonnées des bâteaux du placés du client
    private final boolean[][] dejaTire;//une tableau à deux dimensions qui stock les coordonnées des endroit ou le joueur à déja tiré
    private final ResultatTir[][] tirMemo;// //une tableau à deux dimensions qui stock les résultats des tirs ou le joueur à tiré

    public Grille(int taille) {
        this(taille, taille);
    }

    public Grille(int largeur, int hauteur) {
        this.largeur = largeur;
        this.hauteur = hauteur;
        this.cases = new Bâteau[largeur][hauteur];
        this.dejaTire = new boolean[largeur][hauteur];
        this.tirMemo = new ResultatTir[largeur][hauteur];
    }

    //--------------------------------------------------------------------------------------------

    /**
        Cette méthode pertmet de verifier si le coordonnées x et y ne dépasse pas la grille
     */
    private boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < largeur && y < hauteur;
    }

    /**
     *     Permet de placer un bâteau selon une orientation sur la grille sur à partir des coordonnées x et y
     */
    public boolean placer(Bâteau b, int x, int y, Orientation orientation) {
        int L = b.getLongueur();

        //  Vérif bornes + chevauchements
        for (int i = 0; i < L; i++) {
            int xi = (orientation == Orientation.HORIZONTAL) ? x + i : x;
            int yi = (orientation == Orientation.HORIZONTAL) ? y : y + i;
            if (!inBounds(xi, yi)) return false;
            if (cases[xi][yi] != null) return false;
        }

        // 2) On positionne le bâteau
        for (int i = 0; i < L; i++) {
            int xi = (orientation == Orientation.HORIZONTAL) ? x + i : x;
            int yi = (orientation == Orientation.HORIZONTAL) ? y : y + i;
            cases[xi][yi] = b;
        }
        return true;
    }


    /**
     * Fonction qui permet le placememnt d'un bâteau en faissant appel à la fonction precedente juste ici l'oriantation c'est
     * un bouleen car l'utilsateur si il tape horizontal ca va se traduire par un true et aprés on le traduit en une orientation
     * */
    public boolean placerBateau(Bâteau b, int x, int y, boolean horizontal) {
        return placer(b, x, y, horizontal ? Orientation.HORIZONTAL : Orientation.VERTICAL);
    }

    /**
     *  Cette focntion permet d'ffectuer un tir et de renvoyer le resultat de ce dernier sous forme d'objet TirResult
     * */
    public TirResult tirer(int x, int y) {
        if (!inBounds(x, y)) {
            return new TirResult(ResultatTir.OUT_OF_BOUNDS, null);
        }
        if (dejaTire[x][y]) {
            return new TirResult(ResultatTir.ALREADY_TRIED, cases[x][y]);
        }
        dejaTire[x][y] = true;

        Bâteau b = cases[x][y];
        if (b == null) {
            tirMemo[x][y] = ResultatTir.MISS;
            return new TirResult(ResultatTir.MISS, null);
        }

        b.toucher();
        if (b.estCoule()) {
            tirMemo[x][y] = ResultatTir.SUNK;
            return new TirResult(ResultatTir.SUNK, b);
        } else {
            tirMemo[x][y] = ResultatTir.HIT;
            return new TirResult(ResultatTir.HIT, b);
        }
    }
    /**
     * Permet de voir le resultat du tir recu par l'ennemi sur notre grille personnel
     * */
    public ResultatTir tirerSurMoi(int x, int y) {
        return tirer(x, y).getResultat();
    }


    /**
     * Permet d'enregistrer le resultat du tir effectué
    */
    public void marquerResultatTir(int x, int y, ResultatTir resultat) {
        if (!inBounds(x, y)) return;
        tirMemo[x][y] = resultat;
    }

    /**
     * Cette fonction permet d'afficher la grille
     * */
    public void afficher() {
        // En-tête : X (horizontal) de 1 à 10
        System.out.print("  X: "); // Label pour l'axe X
        for (int col = 1; col <= largeur; col++) {
            if (col <= 10) {
                System.out.print(" "+col );
            }
        }
        System.out.println();

        System.out.print("     ");
        for (int col = 1; col <= largeur; col++) {
            System.out.print("--");
        }
        System.out.println();

        // Lignes : Y (vertical) de 1 à 10
        for (int yIdx = 0; yIdx < hauteur; yIdx++) {
            int rowLabel = yIdx + 1;

            // Y: suivi du numéro de ligne
            if (rowLabel < 10) {
                System.out.print("Y:" + rowLabel + " | ");
            } else {
                System.out.print("Y:" + rowLabel + "| ");
            }

            StringBuilder sb = new StringBuilder();

            for (int xIdx = 0; xIdx < largeur; xIdx++) {
                char c = '.';

                if (cases[xIdx][yIdx] != null) {
                    if (dejaTire[xIdx][yIdx]) {
                        c = 'X';
                    } else {
                        c = 'B';
                    }
                } else {
                    if (dejaTire[xIdx][yIdx]) {
                        c = 'o';
                    } else if (tirMemo[xIdx][yIdx] != null) {
                        ResultatTir r = tirMemo[xIdx][yIdx];
                        if (r == ResultatTir.MISS) c = 'o';
                        else if (r == ResultatTir.HIT || r == ResultatTir.SUNK) c = 'X';
                    }
                }

                sb.append(c).append(' ');
            }

            System.out.println(sb.toString());
        }
    }

    /**
     * C'est une classe qui contient toutes les informations necessaire sur le tir effectué
     * */
    public static class TirResult {
        private final ResultatTir resultat;
        private final Bâteau bateau;

        public TirResult(ResultatTir resultat, Bâteau bateau) {
            this.resultat = resultat;
            this.bateau = bateau;
        }
        public ResultatTir getResultat() { return resultat; }
        public Bâteau getBateau() { return bateau; }
    }
}