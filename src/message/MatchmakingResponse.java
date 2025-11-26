package message;

import etats.MessegeType;

public class MatchmakingResponse extends Message {
    private final String status; // "WAITING", "FOUND", "ERROR"
    private final String opponentName; // peut être null

    public MatchmakingResponse(String status, String opponentName) {
        this.type = MessegeType.MATCHMAKING;
        this.status = status;
        this.opponentName = opponentName;
    }

    public String getStatus() {
        return status;
    }

    public String getOpponentName() {
        return opponentName;
    }

    @Override
    public String serialize() {
        String opponent = (opponentName == null) ? "null" : ("\"" + opponentName + "\"");
        return "{\"type\":\"MATCHMAKING\",\"status\":\"" + status + "\",\"opponentName\":" + opponent + "}";
    }

    public static MatchmakingResponse fromJson(String json) {
        String cleaned = json.replace("{", "")
                .replace("}", "")
                .replace("\"", "");

        String[] fields = cleaned.split(",");
        String status = "WAITING";
        String opponentName = null;

        for (String f : fields) {
            String[] kv = f.split(":", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String value = kv[1].trim();

            switch (key) {
                case "status" -> status = value;
                case "opponentName" -> {
                    if (!value.equals("null") && !value.isEmpty()) {
                        opponentName = value;
                    }
                }
            }
        }

        return new MatchmakingResponse(status, opponentName);
    }
}