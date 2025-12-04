package message;

import etats.MessegeType;

public class NewGameRequest extends Message {

    public NewGameRequest() {
        this.type = MessegeType.NEW_GAME;
    }

    //Format du message {type : NEW_GAME}
    @Override
    public String serialize() {
        return "{\"type\":\"NEW_GAME\"}";
    }

    /**
     * Cette fonction recoit une chaîne de caractre qui respecte le format precedent et le transforme en un objet NewGameRequest
     */
    public static NewGameRequest fromJson(String json) {
        // pas de champs à parser car on veut juste lancer une nouvelle partie donc pas d'information necessaire
        return new NewGameRequest();
    }
}
