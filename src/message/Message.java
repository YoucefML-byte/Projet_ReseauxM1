package message;

import etats.MessegeType;

public abstract class Message {
    protected MessegeType type;
    public MessegeType getType() { return type; }

    // Chaque sous-classe sait se sérialiser
    public abstract String serialize();

    // On détecte juste le type et on délègue
    public static Message deserialize(String raw) {
        String s = raw.trim();

        if (s.contains("\"type\":\"SHOT_REQUEST\"")) {
            return ShotRequest.fromJson(s);

        } else if (s.contains("\"type\":\"SHOT_RESPONSE\"")) {
            return ShotResponse.fromJson(s);

        } else if (s.contains("\"type\":\"PLACE_SHIP\"")) {   // 👈 AJOUT
            return PlaceShipRequest.fromJson(s);

        } else {
            throw new IllegalArgumentException("Type de message inconnu : " + s);
        }
    }
}
