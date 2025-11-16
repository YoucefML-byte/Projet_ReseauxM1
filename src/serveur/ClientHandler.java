package serveur;

import message.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final GameService game; // 🔥 CHAQUE CLIENT A SA PROPRE INSTANCE
    private final String clientId;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.clientId = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        // 🔥 CRÉER UNE NOUVELLE INSTANCE DE GAMESERVICE POUR CE CLIENT
        this.game = new GameService(clientId);
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Client-" + clientId);
        System.out.println("🎮 [" + clientId + "] Nouvelle connexion - Partie créée");

        try {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(new java.io.OutputStreamWriter(
                         socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true)) {

                String raw;
                while ((raw = in.readLine()) != null) {
                    String line = raw.trim();
                    if (line.isEmpty()) continue;
                    if (line.length() > 4096) {
                        out.println(errorJson("PAYLOAD_TOO_LARGE", "message trop long"));
                        continue;
                    }

                    if (line.equalsIgnoreCase("quit")) {
                        System.out.println("👋 [" + clientId + "] Client se déconnecte");
                        out.println("{\"type\":\"INFO\",\"msg\":\"bye bye\"}");
                        break;
                    }

                    Message msg;
                    try {
                        msg = Message.deserialize(line);
                    } catch (Exception ex) {
                        out.println(errorJson("BAD_REQUEST", "format invalide"));
                        continue;
                    }

                    if (msg instanceof ShotRequest req) {
                        GameService.RoundResult round = game.processShot(req);

                        // 1) Envoyer la réponse sur le tir du client
                        out.println(round.getClientResponse().serialize());

                        // 2) Envoyer TOUS les tirs du serveur
                        List<ServerShotMessage> serverShots = round.getServerShots();
                        for (ServerShotMessage shot : serverShots) {
                            out.println(shot.serialize());
                        }

                    } else if (msg instanceof PlaceShipRequest ps) {
                        boolean ok = game.placeClientShip(ps);
                        out.println("{\"type\":\"PLACE_SHIP_RESPONSE\",\"ok\":" + ok + "}");

                    } else if (msg instanceof NewGameRequest) {
                        System.out.println("🔨 [" + clientId + "] NEW_GAME demandé");
                        game.resetGame();
                        out.println("{\"type\":\"NEW_GAME_RESPONSE\",\"ok\":true}");
                    } else {
                        out.println(errorJson("UNSUPPORTED_TYPE", "message non pris en charge"));
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ [" + clientId + "] Erreur : " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (Exception ignore) {}
            System.out.println("🔌 [" + clientId + "] Connexion fermée - Partie terminée");
        }
    }

    private static String errorJson(String code, String msg) {
        return "{\"type\":\"ERROR\",\"code\":\"" + code + "\",\"msg\":\"" + msg + "\"}";
    }
}