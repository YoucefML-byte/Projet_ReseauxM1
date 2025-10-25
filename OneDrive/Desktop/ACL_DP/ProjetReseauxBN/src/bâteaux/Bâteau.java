package bâteaux;

public class Bâteau {
    private String nom;
    private String CoordonnéeDépart;
    private String CoordonnéDeFin;
    private int longeurBâteau;



    public Bâteau(String nom, String CordoonéeDépart, String CoordonnéDeFin, int longeurBâteau){
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
    public String getCoordonnéeDépart(){
        return CoordonnéeDépart;
    }

    private void setCoordonnéeDépart(String CoordDépart){
        this.CoordonnéeDépart = CoordDépart;
    }

    public String getCoordonnéDeFin(){
        return CoordonnéDeFin;
    }

    private void setCoordonnéDeFin(String coordonnéDeFin) {
        this.CoordonnéDeFin = coordonnéDeFin;
    }

    public int getLongeur(){
        return longeurBâteau;
    }

    private void setLongeur(int longeur){
        this.longeurBâteau = longeur;
    }
}
