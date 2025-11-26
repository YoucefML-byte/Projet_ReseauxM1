// OpponentLeftMessage.java
package message;

import etats.MessegeType;

public class OpponentLeftMessage extends Message {
    private final String reason; // "disconnected", "declined_rematch", etc.

    public OpponentLeftMessage(String reason) {
        this.type = MessegeType.OPPONENT_LEFT;
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String serialize() {
        return "{\"type\":\"OPPONENT_LEFT\",\"reason\":\"" + reason + "\"}";
    }

    public static OpponentLeftMessage fromJson(String json) {
        String cleaned = json.replace("{", "")
                .replace("}", "")
                .replace("\"", "");

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