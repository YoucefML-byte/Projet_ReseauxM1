package bâteaux;

public class Bâteau {
    private final String nom;
    private final int longueur;
    private int pointsRestants;

    public Bâteau(String nom, int longueur) {
        this.nom = nom;
        this.longueur = longueur;
        this.pointsRestants = longueur;
    }

    public String getNom() {
        return nom;
    }

    public int getLongueur() {
        return longueur;
    }

    public void toucher() {
        if (pointsRestants > 0) {
            pointsRestants--;
        }
    }

    public boolean estCoule() {
        return pointsRestants <= 0;
    }
}