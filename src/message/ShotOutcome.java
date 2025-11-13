package message;

import etats.ResultatTir;
// classe qui contient tout les information aprés un tir
public class ShotOutcome {
    private final int x;
    private final int y;
    private final ResultatTir result; // RATE, TOUCHE, COULE
    private final String shipName;    // nom du bateau si touché
    private final boolean gameOver;
    private final String winner;      // "PLAYER" ou "SERVER"

    public ShotOutcome(int x, int y, ResultatTir result, String shipName, boolean gameOver, String winner) {
        this.x = x;
        this.y = y;
        this.result = result;
        this.shipName = shipName;
        this.gameOver = gameOver;
        this.winner = winner;
    }

    // ---- Getters ----
    public int getX() { return x; }
    public int getY() { return y; }
    public ResultatTir getResult() { return result; }
    public String getShipName() { return shipName; }
    public boolean isGameOver() { return gameOver; }
    public String getWinner() { return winner; }
}

