// OpponentLeftMessage.java
package message;

import etats.MessegeType;
//message envoyer lors d'une deconnexion pendant le match ou refus de rematch
public class OpponentLeftMessage extends Message {
    private final String reason; // "disconnected", "declined_rematch"

    public OpponentLeftMessage(String reason) {
        this.type = MessegeType.OPPONENT_LEFT;
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }


    //Format du message retourné :{type : OPPENENT_LEFT_MESSAGE ,  reason : disconnected/declined_rematch }
    @Override
    public String serialize() {
        return "{\"type\":\"OPPONENT_LEFT\",\"reason\":\"" + reason + "\"}";
    }

    //Cette fonction permet de transformé une châine de carcatére en un objet de type OpponentLeftMessage
    public static OpponentLeftMessage fromJson(String json) {
        // On enlève les { } et "
        String cleaned = json.replace("{", "")
                .replace("}", "")
                .replace("\"", "");
        //type : OPPENENT_LEFT_MESSAGE ,  reason : disconnected/declined_rematch
        String[] fields = cleaned.split(",");
        String reason = "unknown";

        for (String f : fields) {
            String[] kv = f.split(":", 2);
            if (kv.length != 2) continue;
            if (kv[0].trim().equals("reason")) {
                reason = kv[1].trim();
            }
        }

        return new OpponentLeftMessage(reason);
    }
}