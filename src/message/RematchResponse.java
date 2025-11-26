package message;

import etats.MessegeType;

/**
 * Message envoyé par le serveur pour répondre à une demande de rematch
 */
public class RematchResponse extends Message {
    private final String status; // "WAITING", "ACCEPTED", "DECLINED", "OPPONENT_LEFT"
    private final String message;

    public RematchResponse(String status, String message) {
        this.type = MessegeType.REMATCH_RESPONSE;
        this.status = status;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String serialize() {
        String msg = (message == null) ? "null" : ("\"" + message + "\"");
        return "{\"type\":\"REMATCH_RESPONSE\",\"status\":\"" + status + "\",\"message\":" + msg + "}";
    }

    public static RematchResponse fromJson(String json) {
        String cleaned = json.replace("{", "")
                .replace("}", "")
                .replace("\"", "");

        String[] fields = cleaned.split(",");
        String status = "WAITING";
        String message = null;

        for (String f : fields) {
            String[] kv = f.split(":", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String value = kv[1].trim();

            switch (key) {
                case "status" -> status = value;
                case "message" -> {
                    if (!value.equals("null") && !value.isEmpty()) {
                        message = value;
                    }
                }
            }
        }

        return new RematchResponse(status, message);
    }
}