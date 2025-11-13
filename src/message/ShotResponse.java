package message;

import etats.MessegeType;
import etats.ResultatTir;

public class ShotResponse extends Message {
    private final ResultatTir resultat;   // ton enum
    private final String nomBateau;       // peut être null

    public ShotResponse(ResultatTir resultat, String nomBateau) {
        this.type = MessegeType.SHOT_RESPONSE;
        this.resultat = resultat;
        this.nomBateau = nomBateau;
    }

    // version utilisée dans Message.deserialize(String res, String nom)
    public ShotResponse(String resultat, String nomBateau) {
        this(ResultatTir.valueOf(resultat), nomBateau);
    }

    public ResultatTir getResultat() { return resultat; }
    public String getNomBateau() { return nomBateau; }

    @Override
    public String serialize() {
        String nb = (nomBateau == null) ? "null" : ("\"" + nomBateau + "\"");
        return "{\"type\":\"SHOT_RESPONSE\",\"resultat\":\"" + resultat.name()
                + "\",\"nomBateau\":" + nb + "}";
    }

    public static ShotResponse fromJson(String json) {
        String cleaned = json.replace("{", "")
                .replace("}", "")
                .replace("\"", "");

        // type:SHOT_RESPONSE,resultat:HIT,nomBateau:Croiseur
        String[] fields = cleaned.split(",");
        String resStr = null;
        String nom = null;

        for (String f : fields) {
            String[] kv = f.split(":", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String value = kv[1].trim();
            if (key.equals("resultat")) {
                resStr = value;
            } else if (key.equals("nomBateau")) {
                if (value.equals("null") || value.isEmpty()) nom = null;
                else nom = value;
            }
        }

        if (resStr == null) {
            throw new IllegalArgumentException("resultat manquant dans ShotResponse JSON");
        }
        return new ShotResponse(ResultatTir.valueOf(resStr), nom);
    }
}
