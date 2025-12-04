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

    //message renvoyé par la méthode : {type : RematchResponse , status : WAITING/ACCEPTED , message : Demande de rematch accepté/...}
    @Override
    public String serialize() {
        String msg = (message == null) ? "null" : ("\"" + message + "\"");
        return "{\"type\":\"REMATCH_RESPONSE\",\"status\":\"" + status + "\",\"message\":" + msg + "}";
    }
    //Renvoie un objet RematchResponse à partir d'une châine de caractéres si elle respecte bien le format vu en haut
    public static RematchResponse fromJson(String json) {
        // On enlève les { } et "
        String cleaned = json.replace("{", "")
                .replace("}", "")
                .replace("\"", "");
        // type : RematchResponse , status : WAITING/ACCEPTED , message : Demande de rematch accepté
        String[] fields = cleaned.split(",");
        String status = "WAITING";
        String message = null;

        for (String f : fields) {
            String[] kv = f.split(":", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String value = kv[1].trim();

            //extraction de la bonne information
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