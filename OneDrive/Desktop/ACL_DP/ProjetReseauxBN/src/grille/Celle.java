package grille;

import bâteaux.Bâteau;
import coordonnées.Coordonnées;
import etats.CelleState;

public class Celle {
    Coordonnées coord;
    CelleState state;
    // pour voir si la case contient un bateau et quand on tire sur cette case voir quel bâteau on touché (son type)
    Bâteau bâteau;



    public Celle(Coordonnées coord, Bâteau bâteau ) {
        this.coord = coord;
        this.state = CelleState.SHIP;
        this.bâteau = bâteau;
    }

    public Celle(int x,int y ) {
        this.coord = new Coordonnées(x, y);
        this.state = CelleState.EMPTY;
    }

    public Celle(int x, int y ,Bâteau bâteau) {
        this.coord = new Coordonnées(x, y);
        this.state = CelleState.SHIP;
    }

    public CelleState getState() {
        return state;
    }
    public void setState(CelleState state) {
        this.state = state;
    }


    public Coordonnées getCoord() {
        return coord;
    }
    public void setCoord(Coordonnées coord) {
        this.coord = coord;
    }

    public Bâteau getBâteau() {
        return bâteau;
    }


    public void setBâteau(Bâteau bâteau) {

        this.bâteau = bâteau;
        //si il contennait rien
        this.state = CelleState.SHIP;
    }

    public boolean isEmpty() {
        return state.equals(CelleState.EMPTY);
    }

    public boolean hasShip() {
        return state.equals(CelleState.SHIP) && bâteau!=null;
    }

    public boolean estLibre() {
        return state == CelleState.EMPTY;
    }

    @Override
    public String toString() {
        return "Case [coord=" + coord.toString() + ", etat=" + state + "]";
    }

    public String representationCase(){
        switch (state) {
            case EMPTY:
                return " ";
            case SHIP:
                return "B";
            case HIT:
                return "X";
            case MISS:
                return "O";
            default:
                return "";
        }
    }
}
