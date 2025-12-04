package message;

import etats.MessegeType;
//reponse du matchmaking
public class MatchmakingResponse extends Message {
    private final String status; // "WAITING", "FOUND", "ERROR"
    private final String opponentName; // pseudo de l'adversaire

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

    //Format du message retourné :{type : MATCHMAKING ,  status : WAITING/FOUND/ERROR, opponentName: Youcef/abd'naim }
    @Override
    public String serialize() {
        String opponent = (opponentName == null) ? "null" : ("\"" + opponentName + "\"");
        return "{\"type\":\"MATCHMAKING\",\"status\":\"" + status + "\",\"opponentName\":" + opponent + "}";
    }

    //Cette fonction recoit une chaîne de caractre qui respecte le format precedent et le transforme en un objet MatchmakingResponse
    public static MatchmakingResponse fromJson(String json) {
        // On enlève les { } et "
        String cleaned = json.replace("{", "")
                .replace("}", "")
                .replace("\"", "");
        //type : MATCHMAKING ,  status : WAITING/FOUND/ERROR, opponentName: Youcef/abd'naim
        String[] fields = cleaned.split(",");
        String status = "WAITING";
        String opponentName = null;

        for (String f : fields) {
            String[] kv = f.split(":", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String value = kv[1].trim();
            //extraction de la bonne information
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