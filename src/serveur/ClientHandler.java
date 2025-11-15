package serveur;

import etats.ResultatTir;
import message.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final GameService game = GameService.getInstance();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Client-" + socket.getInetAddress().getHostAddress());
        System.out.println("Thread démarré pour le client : " + socket.getInetAddress());

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

                        // 2) 🔥 Envoyer TOUS les tirs du serveur (un ou plusieurs)
                        List<ServerShotMessage> serverShots = round.getServerShots();  // ✅ getServerShots() avec un "s"
                        for (ServerShotMessage shot : serverShots) {
                            out.println(shot.serialize());
                        }
                    } else if (msg instanceof PlaceShipRequest ps) {
                        boolean ok = game.placeClientShip(ps);
                        out.println("{\"type\":\"PLACE_SHIP_RESPONSE\",\"ok\":" + ok + "}");

                    } else if (msg instanceof NewGameRequest) {
                        System.out.println("🔨 NEW_GAME reçu du client, on reset la partie.");
                        game.resetGame();
                        out.println("{\"type\":\"NEW_GAME_RESPONSE\",\"ok\":true}");
                    } else {
                        out.println(errorJson("UNSUPPORTED_TYPE", "message non pris en charge"));
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur serveur (" + socket.getInetAddress() + ") : " + e.getMessage());
        } finally {
            try { socket.close(); } catch (Exception ignore) {}
            System.out.println("Connexion fermée avec : " + socket.getInetAddress());
        }
    }

    private static String errorJson(String code, String msg) {
        return "{\"type\":\"ERROR\",\"code\":\"" + code + "\",\"msg\":\"" + msg + "\"}";
    }
}