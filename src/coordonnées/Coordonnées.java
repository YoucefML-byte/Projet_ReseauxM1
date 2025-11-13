package coordonnées;

public class Coordonnées {

    private int x;
    private int y;

    public Coordonnées(int x, int y) {
        if(x<0 || y<0){
            throw new IllegalArgumentException("Les coordonnées doivent être positif");
        }
        this.x = x;
        this.y = y;
    }
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }
    public void setY(int y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordonnées)) return false;
        Coordonnées that = (Coordonnées) o;
        return x == that.x && y == that.y;
    }

    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}