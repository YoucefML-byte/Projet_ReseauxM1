package message;

import etats.MessegeType;

public class ShotRequest extends Message {
    private final int x;
    private final int y;

    public ShotRequest(int x, int y) {
        this.type = MessegeType.SHOT_REQUEST;
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    @Override
    public String serialize() {
        // On garde un JSON simple et toujours dans le même format
        return "{\"type\":\"SHOT_REQUEST\",\"x\":" + x + ",\"y\":" + y + "}";
    }

    //  version très simple, qui suppose que le JSON vient DE NOTRE serialize()
    public static ShotRequest fromJson(String json) {
        // On enlève les { } et "
        String cleaned = json.replace("{", "")
                .replace("}", "")
                .replace("\"", "");

        // type:SHOT_REQUEST,x:3,y:5
        String[] fields = cleaned.split(",");
        int x = 0, y = 0;
        for (String f : fields) {
            String[] kv = f.split(":");
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String value = kv[1].trim();
            if (key.equals("x")) {
                x = Integer.parseInt(value);
            } else if (key.equals("y")) {
                y = Integer.parseInt(value);
            }
        }
        return new ShotRequest(x, y);
    }
}
