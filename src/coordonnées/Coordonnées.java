package coordonnées;

public class Coordonnées {
    private String X;

    public Coordonnées(String X) {
        if(X.length()!=2){
            throw new IllegalArgumentException("La coordonnées n'est pas valide");
        }
        char c1 =X.charAt(0);
        char c2 =X.charAt(1);
        if(!(c1>='A' && c1<='K')){
            throw new IllegalArgumentException("Le premier caractére n'est pas valide");
        }
        if(!(c2>='0' && c1<='9')){
            throw new IllegalArgumentException("Le deuxième caractére n'est pas valide");
        }

        this.X = X;
    }

    public String getX() {
        return X;
    }

}
