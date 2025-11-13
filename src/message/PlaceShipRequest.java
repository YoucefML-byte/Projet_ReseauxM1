package message;

import etats.MessegeType;
import etats.Orientation;
import etats.ShipType;

public class PlaceShipRequest extends Message {

    private final ShipType shipType;
    private final int x;
    private final int y;
    private final Orientation orientation;

    public PlaceShipRequest(ShipType shipType, int x, int y, Orientation orientation) {
        this.type = MessegeType.PLACE_SHIP;
        this.shipType = shipType;
        this.x = x;
        this.y = y;
        this.orientation = orientation;
    }

    public ShipType getShipType() { return shipType; }
    public int getX() { return x; }
    public int getY() { return y; }
    public Orientation getOrientation() { return orientation; }

    @Override
    public String serialize() {
        // JSON simple, cohérent avec le reste
        return String.format(
                "{\"type\":\"PLACE_SHIP\",\"shipType\":\"%s\",\"x\":%d,\"y\":%d,\"orientation\":\"%s\"}",
                shipType.name(), x, y, orientation.name()
        );
    }

    public static PlaceShipRequest fromJson(String json) {

        // Nettoyage comme pour ShotRequest
        String cleaned = json.replace("{", "")
                .replace("}", "")
                .replace("\"", "");

        // type:PLACE_SHIP,shipType:TORPILLEUR,x:5,y:7,orientation:HORIZONTAL
        String[] fields = cleaned.split(",");

        ShipType shipType = null;
        int x = 0, y = 0;
        Orientation orientation = Orientation.HORIZONTAL;

        for (String f : fields) {
            String[] kv = f.split(":");
            if (kv.length != 2) continue;

            String key = kv[0].trim();
            String value = kv[1].trim();

            switch (key) {
                case "shipType" -> shipType = ShipType.valueOf(value);
                case "x"        -> x = Integer.parseInt(value);
                case "y"        -> y = Integer.parseInt(value);
                case "orientation" -> orientation = Orientation.valueOf(value);
                default -> {}
            }
        }

        if (shipType == null) {
            throw new IllegalArgumentException("Champ 'shipType' manquant dans " + json);
        }

        return new PlaceShipRequest(shipType, x, y, orientation);
    }

}
