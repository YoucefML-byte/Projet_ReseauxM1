package message;

import etats.MessegeType;
import etats.ResultatTir;

public class ServerShotMessage extends Message {

    private final int x;
    private final int y;
    private final ResultatTir resultat;
    private final String nomBateau; // peut être null si il rate

    private final boolean gameOver;
    private final String winner; // "CLIENT", "SERVER" ou null

    public ServerShotMessage(int x, int y, ResultatTir resultat, String nomBateau,
                             boolean gameOver, String winner) {
        this.type = MessegeType.SERVER_SHOT;
        this.x = x;
        this.y = y;
        this.resultat = resultat;
        this.nomBateau = nomBateau;
        this.gameOver = gameOver;
        this.winner = winner;
    }
    //------------------------------------------------------------------------------

    public int getX() { return x; }
    public int getY() { return y; }
    public ResultatTir getResultat() { return resultat; }
    public String getNomBateau() { return nomBateau; }
    public boolean isGameOver() { return gameOver; }
    public String getWinner() { return winner; }

    //-------------------------------------------------------------------------------

    //message renvoyé par la méthode : {type : SERVER_SHOT , x : 10 , y : 5 , resultat : MISS/SUNK... , nomBateau : PORTE_AVION , gameOver = false , win = serveur }
    @Override
    public String serialize() {
        String nb = (nomBateau == null) ? "null" : ("\"" + nomBateau + "\"");
        String win = (winner == null) ? "null" : ("\"" + winner + "\"");

        return "{\"type\":\"SERVER_SHOT\",\"x\":" + x +
                ",\"y\":" + y +
                ",\"resultat\":\"" + resultat.name() + "\"," +
                "\"nomBateau\":" + nb + "," +
                "\"gameOver\":" + gameOver + "," +
                "\"winner\":" + win +
                "}";
    }

    // Renvoie un objet ServerShotMessage à partir d'une châine de caractéres si elle respecte bien le format vu en haut
    public static ServerShotMessage fromJson(String json) {
        String cleaned = json.replace("{", "")
                .replace("}", "")
                .replace("\"", "");

        // type:SERVER_SHOT,x:3,y:5,resultat:HIT,nomBateau:Croiseur,gameOver:true,winner:CLIENT
        String[] fields = cleaned.split(",");
        int x = 0, y = 0;
        ResultatTir res = ResultatTir.MISS;
        String nom = null;
        boolean gameOver = false;
        String winner = null;

        for (String f : fields) {
            String[] kv = f.split(":", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String value = kv[1].trim();

            switch (key) {
                case "x" -> x = Integer.parseInt(value);
                case "y" -> y = Integer.parseInt(value);
                case "resultat" -> res = ResultatTir.valueOf(value);
                case "nomBateau" -> {
                    if (!value.equals("null") && !value.isEmpty()) nom = value;
                }
                case "gameOver" -> gameOver = Boolean.parseBoolean(value);
                case "winner" -> {
                    if (!value.equals("null") && !value.isEmpty()) winner = value;
                }
                default -> {}
            }
        }

        return new ServerShotMessage(x, y, res, nom, gameOver, winner);
    }
}
