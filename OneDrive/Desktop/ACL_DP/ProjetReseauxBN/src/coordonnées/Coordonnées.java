package coordonnées;

public class Coordonnées {

    private int x;
    private int Y;

    public Coordonnées(int x, int y) {
        if(x<0 || y<0){
            throw new IllegalArgumentException("Les coordonnées doivent être positif");
        }
        this.x = x;
        this.Y = y;
    }
    public int getX() {
        return x;
    }

    public int getY() {
        return Y;
    }

    public void setX(int x) {
        this.x = x;
    }
    public void setY(int y) {
        this.Y = y;
    }

    public String toString() {
        return "(" + x + ", " + Y + ")";
    }
}
