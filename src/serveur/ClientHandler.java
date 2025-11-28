package serveur;

import message.*;
import serveur.PvPGameService.*;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final String clientId;

    private GameService botGame;
    private PvPGameService pvpGame;
    private boolean isPvPMode = false;
    private boolean isPlayer1InPvP;
    private ClientHandler opponentHandler;

    private String username = "Joueur";
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.clientId = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }

    public PrintWriter getOut() { return out; }
    public String getUsername() { return username; }

    public void setPvPGame(PvPGameService game, boolean isPlayer1, ClientHandler opponent) {
        this.pvpGame = game;
        this.isPvPMode = true;
        this.isPlayer1InPvP = isPlayer1;
        this.opponentHandler = opponent;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Client-" + clientId);
        System.out.println("🎮 [" + clientId + "] Nouvelle connexion");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            String raw;
            while ((raw = in.readLine()) != null) {
                String line = raw.trim();
                if (line.isEmpty() || line.length() > 4096) continue;

                if (line.equalsIgnoreCase("quit")) {
                    System.out.println("👋 [" + username + "] Déconnexion");
                    out.println("{\"type\":\"INFO\",\"msg\":\"bye bye\"}");
                    break;
                }

                try {
                    traiterMessage(Message.deserialize(line));
                } catch (Exception ex) {
                    out.println(jsonError("BAD_REQUEST", "format invalide"));
                }
            }
        } catch (Exception e) {
            System.err.println("❌ [" + username + "] Erreur : " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void traiterMessage(Message msg) {
        switch (msg) {
            case SetUsernameRequest r -> {
                username = r.getUsername();
                System.out.println("👤 [" + clientId + "] Pseudo : " + username);
                out.println("{\"type\":\"INFO\",\"msg\":\"Username set\"}");
            }
            case GameModeRequest r -> handleGameModeRequest(r);
            case PlaceShipRequest r -> handlePlaceShipRequest(r);
            case ShotRequest r -> handleShotRequest(r);
            case NewGameRequest ignored -> handleNewGameRequest();
            default -> out.println(jsonError("UNSUPPORTED_TYPE", "message non pris en charge"));
        }
    }

    private void handleGameModeRequest(GameModeRequest req) {
        System.out.println("🎮 [" + username + "] Mode : " + req.getMode());

        if (req.getMode().equals("BOT")) {
            isPvPMode = false;
            botGame = new GameService(username);
            out.println("{\"type\":\"INFO\",\"msg\":\"Mode BOT sélectionné.\"}");
        } else if (req.getMode().equals("PVP")) {
            isPvPMode = true;
            var opponent = MatchmakingManager.getInstance().addPlayerToQueue(clientId, username, this);

            if (opponent == null) {
                out.println(new MatchmakingResponse("WAITING", null).serialize());
            } else {
                out.println(new MatchmakingResponse("FOUND", opponent.username()).serialize());
                opponent.handler().getOut().println(new MatchmakingResponse("FOUND", username).serialize());
                System.out.println("✅ [Matchmaking] " + username + " vs " + opponent.username());
            }
        }
    }

    private void handlePlaceShipRequest(PlaceShipRequest ps) {
        boolean ok = false;

        if (isPvPMode && pvpGame != null) {
            ok = pvpGame.placeShip(ps, isPlayer1InPvP);
            out.println("{\"type\":\"PLACE_SHIP_RESPONSE\",\"ok\":" + ok + "}");

            if (ok && pvpGame.isBothPlayersReady()) {
                envoyerGameStart();
            }
        } else if (botGame != null) {
            ok = botGame.placeClientShip(ps);
            out.println("{\"type\":\"PLACE_SHIP_RESPONSE\",\"ok\":" + ok + "}");
        }

        if (!ok && botGame == null && pvpGame == null) {
            out.println("{\"type\":\"PLACE_SHIP_RESPONSE\",\"ok\":false}");
        }
    }

    private void envoyerGameStart() {
        String p1Name = pvpGame.getJoueur1Name();
        String p2Name = pvpGame.getJoueur2Name();

        if (isPlayer1InPvP) {
            out.println(new GameStartMessage(true, p2Name).serialize());
            if (opponentHandler != null) {
                opponentHandler.getOut().println(new GameStartMessage(false, p1Name).serialize());
            }
        } else {
            out.println(new GameStartMessage(false, p1Name).serialize());
            if (opponentHandler != null) {
                opponentHandler.getOut().println(new GameStartMessage(true, p2Name).serialize());
            }
        }
    }

    private void handleShotRequest(ShotRequest req) {
        if (isPvPMode && pvpGame != null) {
            PvPGameService.PvPRoundResult result = pvpGame.processShot(req, isPlayer1InPvP);
            out.println(result.getShooterResponse().serialize());

            if (result.getOpponentMsg() != null && opponentHandler != null) {
                opponentHandler.getOut().println(result.getOpponentMsg().serialize());
            }
        } else if (botGame != null) {
            GameService.RoundResult round = botGame.processShot(req);
            out.println(round.getClientResponse().serialize());
            round.getServerShots().forEach(shot -> out.println(shot.serialize()));
        }
    }

    private void handleNewGameRequest() {
        System.out.println("🔨 [" + username + "] NEW_GAME");

        if (isPvPMode && pvpGame != null) {
            String status = pvpGame.requestRematch(isPlayer1InPvP);

            if (status.equals("WAITING")) {
                out.println(new RematchResponse("WAITING", "En attente...").serialize());
            } else if (status.equals("ACCEPTED")) {
                pvpGame.resetGame();
                out.println(new RematchResponse("ACCEPTED", "Rematch accepté !").serialize());
                if (opponentHandler != null) {
                    opponentHandler.getOut().println(new RematchResponse("ACCEPTED", "Rematch accepté !").serialize());
                }
            }
        } else if (botGame != null) {
            botGame.resetGame();
            out.println("{\"type\":\"NEW_GAME_RESPONSE\",\"ok\":true}");
        }
    }

    private void cleanup() {
        System.out.println("🧹 [" + username + "] Nettoyage...");

        try { socket.close(); } catch (Exception ignored) {}

        if (isPvPMode && pvpGame != null && opponentHandler != null) {
            try {
                boolean opponentWaiting = pvpGame.doesOpponentWantRematch(!isPlayer1InPvP);

                if (opponentWaiting) {
                    opponentHandler.getOut().println(new RematchResponse("OPPONENT_LEFT", "Adversaire parti.").serialize());
                    pvpGame.cancelRematch(!isPlayer1InPvP);
                } else {
                    opponentHandler.getOut().println(new OpponentLeftMessage("disconnected").serialize());
                }
            } catch (Exception e) {
                System.err.println("⚠️ Erreur notification : " + e.getMessage());
            }
        }

        if (isPvPMode || pvpGame != null) {
            try {
                MatchmakingManager.getInstance().removePlayer(clientId);
            } catch (Exception e) {
                System.err.println("⚠️ Erreur matchmaking : " + e.getMessage());
            }
        }

        System.out.println("🔌 [" + username + "] Fermé\n");
    }

    private static String jsonError(String code, String msg) {
        return "{\"type\":\"ERROR\",\"code\":\"" + code + "\",\"msg\":\"" + msg + "\"}";
    }
}