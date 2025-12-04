package message;

import etats.MessegeType;


//envoyé au serveur une fois que le joeur à choisie le mod de jeu auquel il veut joeur

public class GameModeRequest extends Message {
    private final String mode; // "BOT" ou "PVP"

    public GameModeRequest(String mode) {
        this.type = MessegeType.GAME_MODE;
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }

    //Format du message retourné :{type : GAME_MODE ,  mode : BOT/PVP }
    @Override
    public String serialize() {
        return "{\"type\":\"GAME_MODE\",\"mode\":\"" + mode + "\"}";
    }

    public static GameModeRequest fromJson(String json) {
        // On enlève les { } et "
        String cleaned = json.replace("{", "")
                .replace("}", "")
                .replace("\"", "");
        //type : GAME_MODE ,  mode : BOT/PVP
        String[] fields = cleaned.split(",");
        String mode = "BOT";

        //extraction des informations
        for (String f : fields) {
            String[] kv = f.split(":", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String value = kv[1].trim();
            //extraction du mode de jeu selectionné
            if (key.equals("mode")) {
                mode = value;
            }
        }

        return new GameModeRequest(mode);
    }
}
