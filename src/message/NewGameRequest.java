package message;

import etats.MessegeType;

public class NewGameRequest extends Message {

    public NewGameRequest() {
        this.type = MessegeType.NEW_GAME;
    }

    @Override
    public String serialize() {
        return "{\"type\":\"NEW_GAME\"}";
    }

    public static NewGameRequest fromJson(String json) {
        // pas de champs à parser, on s'en fiche du contenu
        return new NewGameRequest();
    }
}
