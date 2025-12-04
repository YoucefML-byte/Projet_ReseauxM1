package message;

import etats.MessegeType;

public class SetUsernameRequest extends Message {

    private final String username; // le pseudo

    //pour changer le pseudo
    public SetUsernameRequest(String username) {
        this.type = MessegeType.SET_USERNAME;
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    //---------------------------------------------------------------------------------


    //Format du message renvoyer par la méthode : { type : SET_USERNAME , username : Youcef/abd'naim }
    @Override
    public String serialize() {
        return "{\"type\":\"SET_USERNAME\",\"username\":\"" + username + "\"}";
    }

    // Renvoie un objet SetUsernameRequest à partir d'une châine de caractéres si elle respecte bien le format vu en haut
    public static SetUsernameRequest fromJson(String json) {
        // On enlève les { } et "
        String cleaned = json.replace("{", "")
                .replace("}", "")
                .replace("\"", "");

        // type:SET_USERNAME,username:Joueur1
        String[] fields = cleaned.split(",");
        String username = "Joueur";

        for (String f : fields) {
            String[] kv = f.split(":", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String value = kv[1].trim();

            if (key.equals("username")) {
                username = value;
            }
        }

        return new SetUsernameRequest(username);
    }
}//