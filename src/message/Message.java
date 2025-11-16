package message;

import etats.MessegeType;

public abstract class Message {
    protected MessegeType type;
    public MessegeType getType() { return type; }

    public abstract String serialize();

    public static Message deserialize(String raw) {
        String s = raw.trim();

        if (s.contains("\"type\":\"SHOT_REQUEST\"")) {
            return ShotRequest.fromJson(s);
        } else if (s.contains("\"type\":\"SHOT_RESPONSE\"")) {
            return ShotResponse.fromJson(s);
        } else if (s.contains("\"type\":\"PLACE_SHIP\"")) {
            return PlaceShipRequest.fromJson(s);
        } else if (s.contains("\"type\":\"SERVER_SHOT\"")) {
            return ServerShotMessage.fromJson(s);
        } else if (s.contains("\"type\":\"NEW_GAME\"")) {
            return NewGameRequest.fromJson(s);
        } else if (s.contains("\"type\":\"SET_USERNAME\"")) {  // 🔥 NOUVEAU
            return SetUsernameRequest.fromJson(s);
        } else {
            throw new IllegalArgumentException("Type de message inconnu : " + s);
        }
    }
}