package message;

import etats.MessegeType;

public abstract class Message {
    protected MessegeType type; // le type du message SHOT_REQUEST, SHOT_RESPONSE, PLACE_SHIP....

    public MessegeType getType() { return type; }

    /**
     *    cette fonction permet de transormer un objet de type Message sous forme d'une châine de caractére
     *    pour être envoyer au client/servveur
     */
    public abstract String serialize();

    /**
     * Cette fonction est appelé à chaque reception d'un message par le client/serveur
     * qui est sous forme de châine de caractére pour être tranformer sous forme d'un objet de type Message
     * pour faciliter le traitement/extraction des informations du message par le client/serveur
     * */
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
        } else if (s.contains("\"type\":\"SET_USERNAME\"")) {
            return SetUsernameRequest.fromJson(s);
        } else if (s.contains("\"type\":\"GAME_MODE\"")) {
            return GameModeRequest.fromJson(s);
        } else if (s.contains("\"type\":\"MATCHMAKING\"")) {
            return MatchmakingResponse.fromJson(s);
        } else if (s.contains("\"type\":\"OPPONENT_SHOT\"")) {
            return OpponentShotMessage.fromJson(s);
        } else if (s.contains("\"type\":\"GAME_START\"")) {
            return GameStartMessage.fromJson(s);
        }else if (s.contains("\"type\":\"OPPONENT_LEFT\"")) {
            return OpponentLeftMessage.fromJson(s);
        }else if (s.contains("\"type\":\"REMATCH_RESPONSE\"")) {
            return RematchResponse.fromJson(s);
        }else {
            throw new IllegalArgumentException("Type de message inconnu : " + s);
        }
    }
}