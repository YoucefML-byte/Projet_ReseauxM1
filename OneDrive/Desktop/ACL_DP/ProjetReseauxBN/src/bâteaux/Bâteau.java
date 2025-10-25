package bâteaux;

import coordonnées.Coordonnées;

public class Bâteau {
    private String nom;
    private int CoordonnéeDépart;
    private int CoordonnéDeFin;
    private int longeurBâteau;
    private int pointsRestants;



    public Bâteau(String nom, int CordoonéeDépart, int CoordonnéDeFin, int longeurBâteau){
        this.nom = nom;
        this.CoordonnéeDépart = CordoonéeDépart;
        this.CoordonnéDeFin = CoordonnéDeFin;

        this.longeurBâteau = longeurBâteau;
    }

    public String getNom(){
        return nom;
    }

    private void setNom(String nom){
        this.nom = nom;
    }

    public int getCoordonnéeDépart(){
        return CoordonnéeDépart;
    }

    private void setCoordonnéeDépart(int CoordDépart){
        this.CoordonnéeDépart = CoordDépart;
    }

    public int getCoordonnéDeFin(){
        return CoordonnéDeFin;
    }

    private void setCoordonnéDeFin(int coordonnéDeFin) {
        this.CoordonnéDeFin = coordonnéDeFin;
    }

    public int getLongueur(){
        return longeurBâteau;
    }

    private void setLongueur(int longeur){
        this.longeurBâteau = longeur;
    }

    public int getPointsRestants(){
        return pointsRestants;
    }
    public void setPointsRestants(int pointsRestants){
        this.pointsRestants = pointsRestants;
    }

    public void toucher() {
        pointsRestants--;
    }

    public boolean estCoule() {
        return pointsRestants <= 0;
    }

}
