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
        this.pointsRestants = longeurBâteau;
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
    //on décremente les nb de pts de vies ssi il en reste encore
    public void toucher() { if (pointsRestants > 0) pointsRestants--; }

    //si il na plus de pts de vies ca veut dire il a coulé
    public boolean estCoule() {
        return pointsRestants <= 0;
    }

}