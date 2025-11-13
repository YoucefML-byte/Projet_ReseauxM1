package message;

import etats.MessegeType;
import etats.ResultatTir;

public class ServerShotMessage extends Message {

    private final int x;
    private final int y;
    private final ResultatTir resultat;
    private final String nomBateau; // peut être null

    public ServerShotMessage(int x, int y, ResultatTir resultat, String nomBateau) {
        this.type = MessegeType.SERVER_SHOT;
        this.x = x;
        this.y = y;
        this.resultat = resultat;
        this.nomBateau = nomBateau;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public ResultatTir getResultat() { return resultat; }
    public String getNomBateau() { return nomBateau; }

    @Override
    public String serialize() {
        String nb = (nomBateau == null) ? "null" : ("\"" + nomBateau + "\"");
        return "{\"type\":\"SERVER_SHOT\",\"x\":" + x +
                ",\"y\":" + y +
                ",\"resultat\":\"" + resultat.name() + "\"," +
                "\"nomBateau\":" + nb + "}";
    }

    // Même style que ShotRequest.fromJson : on nettoie et on split
    public static ServerShotMessage fromJson(String json) {
        String cleaned = json.replace("{", "")
                .replace("}", "")
                .replace("\"", "");

        // type:SERVER_SHOT,x:3,y:5,resultat:HIT,nomBateau:Croiseur
        String[] fields = cleaned.split(",");
        int x = 0, y = 0;
        ResultatTir res = ResultatTir.MISS;
        String nom = null;

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
                    if (!value.equals("null") && !value.isEmpty()) {
                        nom = value;
                    }
                }
                default -> {}
            }
        }

        return new ServerShotMessage(x, y, res, nom);
    }
}
