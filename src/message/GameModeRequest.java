package message;

import etats.MessegeType;

public class GameModeRequest extends Message {

    private final String mode; // "BOT" ou "PLAYER"

    public GameModeRequest(String mode) {
        this.type = MessegeType.GAME_MODE;
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }

    //----------------------------------------------------------------------------

    //Format du message {type : GAME_MODE , mode : BOT/PVP}
    @Override
    public String serialize() {
        return "{\"type\":\"GAME_MODE\",\"mode\":\"" + mode + "\"}";
    }

    /**
     * Cette fonction recoit une chaîne de caractre qui respecte le format precedent et le transforme en un objet GameModeRequest
     * pour le traitment du mode de jeu choisit par le joueur
     */
    public static GameModeRequest fromJson(String json) {
        String cleaned = json.replace("{", "")
                .replace("}", "")
                .replace("\"", "");

        String[] fields = cleaned.split(",");
        String mode = "BOT";

        for (String f : fields) {
            String[] kv = f.split(":", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String value = kv[1].trim();

            if (key.equals("mode")) {
                mode = value;
            }
        }

        return new GameModeRequest(mode);
    }
}