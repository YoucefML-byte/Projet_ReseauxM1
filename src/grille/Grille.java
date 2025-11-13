package grille;

import bâteaux.Bâteau;
import etats.Orientation;
import etats.ResultatTir;

public class Grille {
    private final int largeur;
    private final int hauteur;

    // Quel bateau occupe chaque case (null si vide)
    private final Bâteau[][] cases;
    // A-t-on déjà tiré ici ?
    private final boolean[][] dejaTire;

    // Mémo des résultats (utile pour la grille de TIRS d’un joueur)
    private final ResultatTir[][] tirMemo;

    // === NOUVEAU: constructeur carré pour Grille(taille) ===
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

    /** Place un bateau si possible. Retourne true si ok, false sinon (débordement/chevauchement). */
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

    // === NOUVEAU: alias attendu par Joueur ===
    public boolean placerBateau(Bâteau b, int x, int y, boolean horizontal) {
        return placer(b, x, y, horizontal ? Orientation.HORIZONTAL : Orientation.VERTICAL);
    }

    /** Applique un tir et retourne le résultat + le bateau (ou null si MISS). */
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
            // Mémo côté “grillePerso” (optionnel) : marquer MISS
            tirMemo[x][y] = ResultatTir.MISS;
            return new TirResult(ResultatTir.MISS, null);
        }

        b.toucher();
        if (b.estCoule()) {
            tirMemo[x][y] = ResultatTir.SUNK; // mémo local
            return new TirResult(ResultatTir.SUNK, b);
        } else {
            tirMemo[x][y] = ResultatTir.HIT;  // mémo local
            return new TirResult(ResultatTir.HIT, b);
        }
    }

    // === NOUVEAU: attendu par Joueur (juste le résultat, pas le bateau) ===
    public ResultatTir tirerSurMoi(int x, int y) {
        return tirer(x, y).getResultat();
    }

    // === NOUVEAU: pour la grille de TIRS d’un joueur ===
    public void marquerResultatTir(int x, int y, ResultatTir resultat) {
        if (!inBounds(x, y)) return;
        // On ne force pas dejaTire ici, on stocke juste l’info “connue” par le joueur tireur
        tirMemo[x][y] = resultat;
    }

    // === NOUVEAU: affichage simple (ASCII) ===
    public void afficher() {
        // Légende:
        // 'B' = bateau non touché (sur grillePerso)
        // 'X' = touché
        // 'o' = manqué
        // '.' = inconnu/vide

        // --- En-tête : numéros de colonnes (1..largeur) ---
        System.out.print("     "); // marge gauche
        for (int col = 1; col <= largeur; col++) {
            System.out.print(col + " ");
        }
        System.out.println();

        System.out.print("    ");
        for (int col = 1; col <= largeur; col++) {
            System.out.print("--");
        }
        System.out.println();

        // --- Lignes ---
        for (int yIdx = 0; yIdx < hauteur; yIdx++) {
            int rowLabel = yIdx + 1;  // ce qu'on affiche (1..hauteur)

            // numéro de ligne aligné
            if (rowLabel < 10) {
                System.out.print(" " + rowLabel + " | ");
            } else {
                System.out.print(rowLabel + " | ");
            }

            StringBuilder sb = new StringBuilder();

            for (int xIdx = 0; xIdx < largeur; xIdx++) {
                char c = '.';

                if (cases[xIdx][yIdx] != null) {
                    // Il y a un bateau ici
                    if (dejaTire[xIdx][yIdx]) {
                        c = 'X'; // touché/coulé
                    } else {
                        c = 'B'; // bateau non touché (visible sur grillePerso)
                    }
                } else {
                    // Pas de bateau ici
                    if (dejaTire[xIdx][yIdx]) {
                        c = 'o'; // tiré et raté
                    } else if (tirMemo[xIdx][yIdx] != null) {
                        // Sur la grille des tirs (adversaire), on affiche ce qu'on sait
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



    /** Petit record/POJO pour renvoyer (résultat, bateau). */
    public static class TirResult {
        private final ResultatTir resultat;
        private final Bâteau bateau; // peut être null

        public TirResult(ResultatTir resultat, Bâteau bateau) {
            this.resultat = resultat;
            this.bateau = bateau;
        }
        public ResultatTir getResultat() { return resultat; }
        public Bâteau getBateau() { return bateau; }
    }
}
