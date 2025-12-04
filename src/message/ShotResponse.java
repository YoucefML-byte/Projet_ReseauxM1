package message;

import etats.MessegeType;
import etats.ResultatTir;

public class ShotResponse extends Message {
    private final ResultatTir resultat;//resultat du tir
    private final String nomBateau;
    private final boolean gameOver; //pour indiquer si il a perdue ou pas
    private final String winner; //pout indiquer si il a gagné la partie ou pas

    // Constructeur principal avec gameOver et winner
    public ShotResponse(ResultatTir resultat, String nomBateau, boolean gameOver, String winner) {
        this.type = MessegeType.SHOT_RESPONSE;
        this.resultat = resultat;
        this.nomBateau = nomBateau;
        this.gameOver = gameOver;
        this.winner = winner;
    }

    // Constructeur sans gameOver
    public ShotResponse(ResultatTir resultat, String nomBateau) {
        this(resultat, nomBateau, false, null);
    }

    // Constructeur utilisé dans Message.deserialize(String res, String nom)
    public ShotResponse(String resultat, String nomBateau) {
        this(ResultatTir.valueOf(resultat), nomBateau, false, null);
    }

    public ResultatTir getResultat() { return resultat; }
    public String getNomBateau() { return nomBateau; }
    public boolean isGameOver() { return gameOver; }
    public String getWinner() { return winner; }

    //Format du message renvoyer par la méthode : { type : SHOT_RESPONSE , resultat : HIT,MISS... , nomBateau : PorteAvion, Croiseur ..., gameOver : false, winner : Joueur1}
    @Override
    public String serialize() {
        String nb = (nomBateau == null) ? "null" : ("\"" + nomBateau + "\"");
        String win = (winner == null) ? "null" : ("\"" + winner + "\"");

        return "{\"type\":\"SHOT_RESPONSE\",\"resultat\":\"" + resultat.name()
                + "\",\"nomBateau\":" + nb
                + ",\"gameOver\":" + gameOver
                + ",\"winner\":" + win + "}";
    }
    // Renvoie un objet ShotResponse à partir d'une châine de caractéres si elle respecte bien le format vu en haut
    public static ShotResponse fromJson(String json) {
        // On enlève les { } et "
        String cleaned = json.replace("{", "")
                .replace("}", "")
                .replace("\"", "");

        // type:SHOT_RESPONSE,resultat:HIT,nomBateau:Croiseur,gameOver:true,winner:Alice
        String[] fields = cleaned.split(",");
        String resStr = null;
        String nom = null;
        boolean gameOver = false;
        String winner = null;

        for (String f : fields) {
            String[] kv = f.split(":", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String value = kv[1].trim();

            //extraction des information selon la clé
            switch (key) {
                case "resultat" -> resStr = value;
                case "nomBateau" -> {
                    if (!value.equals("null") && !value.isEmpty()) nom = value;
                }
                case "gameOver" -> gameOver = Boolean.parseBoolean(value);
                case "winner" -> {
                    if (!value.equals("null") && !value.isEmpty()) winner = value;
                }
            }
        }

        if (resStr == null) {
            throw new IllegalArgumentException("resultat manquant dans ShotResponse JSON");
        }
        return new ShotResponse(ResultatTir.valueOf(resStr), nom, gameOver, winner);
    }
}