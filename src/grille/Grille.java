package grille;

import bâteaux.Bâteau;
import etats.Orientation;
import etats.ResultatTir;

public class Grille {
    private final int largeur;
    private final int hauteur;

    private final Bâteau[][] cases;
    private final boolean[][] dejaTire;
    private final ResultatTir[][] tirMemo;

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

    private boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < largeur && y < hauteur;
    }

    public boolean placer(Bâteau b, int x, int y, Orientation orientation) {
        int L = b.getLongueur();

        // 1) Vérif bornes + chevauchements
        for (int i = 0; i < L; i++) {
            int xi = (orientation == Orientation.HORIZONTAL) ? x + i : x;
            int yi = (orientation == Orientation.HORIZONTAL) ? y : y + i;
            if (!inBounds(xi, yi)) return false;
            if (cases[xi][yi] != null) return false;
        }

        // 2) Pose
        for (int i = 0; i < L; i++) {
            int xi = (orientation == Orientation.HORIZONTAL) ? x + i : x;
            int yi = (orientation == Orientation.HORIZONTAL) ? y : y + i;
            cases[xi][yi] = b;
        }
        return true;
    }

    public boolean placerBateau(Bâteau b, int x, int y, boolean horizontal) {
        return placer(b, x, y, horizontal ? Orientation.HORIZONTAL : Orientation.VERTICAL);
    }

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

    public ResultatTir tirerSurMoi(int x, int y) {
        return tirer(x, y).getResultat();
    }

    public void marquerResultatTir(int x, int y, ResultatTir resultat) {
        if (!inBounds(x, y)) return;
        tirMemo[x][y] = resultat;
    }

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