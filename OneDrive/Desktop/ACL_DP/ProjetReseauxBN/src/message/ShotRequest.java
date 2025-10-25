package message;

import etats.MessegeType;

public class ShotRequest extends Message {

    int x;
    int y;
    //ce bloc est exécuté qu'une seule et c'est lors de la compilation du fichier donc même si on crée plusieur instance ce bloc la ne sera pas executer

    static {
        register(MessegeType.SHOT_REQUEST.name(), ShotRequest::fromString);
    }

    public ShotRequest(int x, int y) {
        super(MessegeType.SHOT_REQUEST);
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }
    public void setY(int y) {
        this.y = y;
    }


    @Override
    public String serialize() {
        return "SHOT_REQUEST|x:" + x + ";y:" + y;
    }


    public static Message fromString(String raw) {
        String[] parts = raw.split("\\|")[1].split(";");
        if(parts.length > 2){
            throw new IllegalArgumentException("Il faut exactemment 2 coordonnées");
        }else{

            int x = Integer.parseInt(parts[0].split(":")[1]);
            int y = Integer.parseInt(parts[1].split(":")[1]);
            return new ShotRequest(x, y);
        }
    }


}
