package message;

import etats.MessegeType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class Message {
    MessegeType request;

    // registre qui contient pour chaque type de message le constructeur de message qui lui correspond
    private static final Map<String, Function<String, Message>> REGISTRY = new HashMap<>();

    public Message(MessegeType request) {
        this.request = request;

    }

    public MessegeType getRequest() {
        return request;
    }
    public void setRequest(MessegeType request) {
        this.request = request;
    }

    //Ajout dans le registre
    protected static void register(String type, Function<String, Message> constructor) {
        REGISTRY.put(type, constructor);
    }

    /*
    * Cette focntion encapsule le message sous forme de chaine de caractéres pour que le clientTCP puisse l'envoyer
     */

    public abstract String serialize();

    /*
    * Cette fonction permet de convertir la chaîne de caractéres brute en un objet message pour pouvoir le manipuer
    */
    public static Message deserialize(String raw){
        // extraction du type de message
        String type = raw.split("\\|")[0];
        Function<String, Message> creator = REGISTRY.get(type);

        // aucune correspondance
        if (creator == null)
            throw new IllegalArgumentException("Type de message inconnu : " + type);

        // on apelle la méthode fromeString avec la chaine recu et c'est elle qui vas la traiter
        return creator.apply(raw);
    }


}
