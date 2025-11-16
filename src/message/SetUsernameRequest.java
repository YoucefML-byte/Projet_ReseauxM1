package message;

import etats.MessegeType;

public class SetUsernameRequest extends Message {

    private final String username;

    public SetUsernameRequest(String username) {
        this.type = MessegeType.SET_USERNAME;
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String serialize() {
        return "{\"type\":\"SET_USERNAME\",\"username\":\"" + username + "\"}";
    }

    public static SetUsernameRequest fromJson(String json) {
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
}