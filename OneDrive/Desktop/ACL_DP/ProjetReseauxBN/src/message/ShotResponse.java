package message;

import etats.MessegeType;
import etats.ResultatTir;

public class ShotResponse extends Message {
    private ResultatTir resultat;
    private String nomBâteau;

    //ce bloc est exécuté qu'une seule et c'est lors de la compilation du fichier donc même si on crée plusieur instance ce bloc la ne sera pas executer

    static {
        //ici on fait ShotRequest::fromString pour faire réference à la fonction fromString de ShotRequest alors que si on faissait
        // ShotRequest.fromString ca aurait exécuter la méthode alors que nous on veut juste une référence de cette méthodr comme ca on sait qui appeler
        register(MessegeType.SHOT_RESPONSE.name(), ShotRequest::fromString);
    }

    public ShotResponse(ResultatTir resultat, String nomBâteau) {
        super(MessegeType.SHOT_RESPONSE);
        this.resultat = resultat;
        this.nomBâteau = nomBâteau;
    }

    public ResultatTir getResultat() {
        return resultat;
    }
    private void setResultat(ResultatTir resultat) {
        this.resultat = resultat;
    }
    public String getNomBateau() {
        return nomBâteau;
    }
    private void setNomBateau(String nomBateau) {
        this.nomBâteau = nomBateau;
    }

    @Override
    public String serialize() {
        return "SHOT_RESPONSE|resultat:" + resultat + ";nomBateau:" + nomBâteau;
    }


    public static Message fromString(String raw) {
        String data = raw.split("\\|")[1];
        String[] parts = data.split(";");

        ResultatTir res = ResultatTir.valueOf(parts[0].split(":")[1]);
        String name = parts[1].split(":")[1];

        return new ShotResponse(res, name);
    }
}
