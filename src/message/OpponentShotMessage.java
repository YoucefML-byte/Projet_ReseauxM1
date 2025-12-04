package message;

import etats.MessegeType;
import etats.ResultatTir;
//tir de l'ennemi (un vrai joueur)
public class OpponentShotMessage extends Message {
    //coordonnées du tir
    private final int x;
    private final int y;

    private final ResultatTir resultat;//resultat du tir ennemi
    private final String nomBateau;//nom du bâteau touché
    private final boolean gameOver;
    private final String winner;
    private final boolean yourTurn; // Indiquequi commence en premier

    public OpponentShotMessage(int x, int y, ResultatTir resultat, String nomBateau,
                               boolean gameOver, String winner, boolean yourTurn) {
        this.type = MessegeType.OPPONENT_SHOT;
        this.x = x;
        this.y = y;
        this.resultat = resultat;
        this.nomBateau = nomBateau;
        this.gameOver = gameOver;
        this.winner = winner;
        this.yourTurn = yourTurn;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public ResultatTir getResultat() { return resultat; }
    public String getNomBateau() { return nomBateau; }
    public boolean isGameOver() { return gameOver; }
    public String getWinner() { return winner; }
    public boolean isYourTurn() { return yourTurn; }

    //Format du message retourné :{type : OPPENENT_SHOT_MESSAGE ,  x : 4 , y : 6 , resultat : MISS/..., nomBateau : PorteAvion/..., gameOver : true/false, winner : Alice, yourTurn: true/false }
    @Override
    public String serialize() {
        String nb = (nomBateau == null) ? "null" : ("\"" + nomBateau + "\"");
        String win = (winner == null) ? "null" : ("\"" + winner + "\"");

        return "{\"type\":\"OPPONENT_SHOT\",\"x\":" + x +
                ",\"y\":" + y +
                ",\"resultat\":\"" + resultat.name() + "\"," +
                "\"nomBateau\":" + nb + "," +
                "\"gameOver\":" + gameOver + "," +
                "\"winner\":" + win + "," +
                "\"yourTurn\":" + yourTurn +
                "}";
    }
    //Cette fonction permet de transformé une châine de carcatére en un objet de type OpponentShotMessage
    public static OpponentShotMessage fromJson(String json) {

        // On enlève les { } et "
        String cleaned = json.replace("{", "")
                .replace("}", "")
                .replace("\"", "");
        //type : OPPENENT_SHOT_MESSAGE ,  x : 4 , y : 6 , resultat : MISS/..., nomBateau : PorteAvion/..., gameOver : true/false, winner : Alice, yourTurn: true/false
        String[] fields = cleaned.split(",");
        int x = 0, y = 0;
        ResultatTir res = ResultatTir.MISS;
        String nom = null;
        boolean gameOver = false;
        String winner = null;
        boolean yourTurn = false;

        for (String f : fields) {
            String[] kv = f.split(":", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String value = kv[1].trim();
            //extraction de la bonne information
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
                case "yourTurn" -> yourTurn = Boolean.parseBoolean(value);
            }
        }

        return new OpponentShotMessage(x, y, res, nom, gameOver, winner, yourTurn);
    }
}