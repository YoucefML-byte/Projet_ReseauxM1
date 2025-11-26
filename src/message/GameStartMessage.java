package message;

import etats.MessegeType;

/**
 * Message envoyé quand les deux joueurs ont fini de placer leurs bateaux
 * Indique qui commence (yourTurn = true/false)
 */
public class GameStartMessage extends Message {
    private final boolean yourTurn;
    private final String opponentName;

    public GameStartMessage(boolean yourTurn, String opponentName) {
        this.type = MessegeType.GAME_START;
        this.yourTurn = yourTurn;
        this.opponentName = opponentName;
    }

    public boolean isYourTurn() {
        return yourTurn;
    }

    public String getOpponentName() {
        return opponentName;
    }

    @Override
    public String serialize() {
        String opponent = (opponentName == null) ? "null" : ("\"" + opponentName + "\"");
        return "{\"type\":\"GAME_START\",\"yourTurn\":" + yourTurn + ",\"opponentName\":" + opponent + "}";
    }

    public static GameStartMessage fromJson(String json) {
        String cleaned = json.replace("{", "")
                .replace("}", "")
                .replace("\"", "");

        String[] fields = cleaned.split(",");
        boolean yourTurn = false;
        String opponentName = null;

        for (String f : fields) {
            String[] kv = f.split(":", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String value = kv[1].trim();

            switch (key) {
                case "yourTurn" -> yourTurn = Boolean.parseBoolean(value);
                case "opponentName" -> {
                    if (!value.equals("null") && !value.isEmpty()) {
                        opponentName = value;
                    }
                }
            }
        }

        return new GameStartMessage(yourTurn, opponentName);
    }
}

// ========================================
// À AJOUTER dans MessegeType.java (enum)
// ========================================
// GAME_START

// ========================================
// À AJOUTER dans Message.java - méthode deserialize()
// ========================================
/*
else if (s.contains("\"type\":\"GAME_START\"")) {
    return GameStartMessage.fromJson(s);
}
*/