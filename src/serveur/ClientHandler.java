package serveur;

import message.*;
import serveur.PvPGameService.PvPRoundResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private GameService botGame;        // Pour le mode vs Bot
    private PvPGameService pvpGame;     // Pour le mode PvP
    private boolean isPvPMode = false;
    private boolean isPlayer1InPvP;     // true si joueur 1 dans la partie PvP
    private ClientHandler opponentHandler; // Référence vers l'adversaire en PvP

    private final String clientId;
    private String username = "Joueur";
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.clientId = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }

    public PrintWriter getOut() {
        return out;
    }

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

        try {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {

                out = new PrintWriter(new java.io.OutputStreamWriter(
                        socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true);

                        String raw;
                while ((raw = in.readLine()) != null) {
                    String line = raw.trim();
                    if (line.isEmpty()) continue;
                    if (line.length() > 4096) {
                        out.println(errorJson("PAYLOAD_TOO_LARGE", "message trop long"));
                        continue;
                    }

                    if (line.equalsIgnoreCase("quit")) {
                        System.out.println("👋 [" + username + "] Client se déconnecte");
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

                    // === GESTION DES MESSAGES ===

                    if (msg instanceof SetUsernameRequest usernameReq) {
                        username = usernameReq.getUsername();
                        System.out.println("👤 [" + clientId + "] Pseudo défini : " + username);
                        out.println("{\"type\":\"INFO\",\"msg\":\"Username set to " + username + "\"}");

                    } else if (msg instanceof GameModeRequest modeReq) {
                        handleGameModeRequest(modeReq);

                    } else if (msg instanceof PlaceShipRequest ps) {
                        handlePlaceShipRequest(ps);

                    } else if (msg instanceof ShotRequest req) {
                        handleShotRequest(req);

                    } else if (msg instanceof NewGameRequest) {
                        handleNewGameRequest();

                    } else {
                        out.println(errorJson("UNSUPPORTED_TYPE", "message non pris en charge"));
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ [" + username + "] Erreur : " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void handleGameModeRequest(GameModeRequest req) {
        String mode = req.getMode();
        System.out.println("🎮 [" + username + "] Mode choisi : " + mode);

        if (mode.equals("BOT")) {
            isPvPMode = false;
            botGame = new GameService(clientId);
            botGame.setClientUsername(username);
            out.println("{\"type\":\"INFO\",\"msg\":\"Mode BOT sélectionné. Placez vos bateaux.\"}");

        } else if (mode.equals("PVP")) {
            isPvPMode = true;

            // Ajouter le joueur au matchmaking
            MatchmakingManager mm = MatchmakingManager.getInstance();
            MatchmakingManager.WaitingPlayer opponent = mm.addPlayerToQueue(clientId, username, this);

            if (opponent == null) {
                // En attente d'un adversaire
                MatchmakingResponse response = new MatchmakingResponse("WAITING", null);
                out.println(response.serialize());
            } else {
                // Match trouvé !
                MatchmakingResponse response = new MatchmakingResponse("FOUND", opponent.username);
                out.println(response.serialize());

                // Notifier l'adversaire aussi
                MatchmakingResponse opponentResponse = new MatchmakingResponse("FOUND", username);
                opponent.handler.getOut().println(opponentResponse.serialize());

                System.out.println("✅ [Matchmaking] Partie créée : " + username + " vs " + opponent.username);
            }
        }
    }

    private void handlePlaceShipRequest(PlaceShipRequest ps) {
        boolean ok;

        if (isPvPMode && pvpGame != null) {
            ok = pvpGame.placeShip(ps, isPlayer1InPvP);
            out.println("{\"type\":\"PLACE_SHIP_RESPONSE\",\"ok\":" + ok + "}");

            // 🔥 VÉRIFIER SI LES DEUX JOUEURS ONT FINI DE PLACER
            if (ok && pvpGame.isBothPlayersReady()) {
                System.out.println("🎮 [PvP] Les deux joueurs sont prêts ! La partie commence.");
                System.out.println("🎯 [PvP] " + pvpGame.getJoueur1Name() + " (Player 1) commence.");

                // Envoyer GAME_START aux deux joueurs
                String player1Name = pvpGame.getJoueur1Name();
                String player2Name = pvpGame.getJoueur2Name();

                if (isPlayer1InPvP) {
                    // Ce joueur est Player 1, il commence
                    GameStartMessage startMsg = new GameStartMessage(true, player2Name);
                    out.println(startMsg.serialize());

                    // Notifier Player 2 qu'il attend
                    if (opponentHandler != null) {
                        GameStartMessage opStartMsg = new GameStartMessage(false, player1Name);
                        opponentHandler.getOut().println(opStartMsg.serialize());
                    }
                } else {
                    // Ce joueur est Player 2, il attend
                    GameStartMessage startMsg = new GameStartMessage(false, player1Name);
                    out.println(startMsg.serialize());

                    // Notifier Player 1 qu'il commence
                    if (opponentHandler != null) {
                        GameStartMessage opStartMsg = new GameStartMessage(true, player2Name);
                        opponentHandler.getOut().println(opStartMsg.serialize());
                    }
                }
            }

        } else if (botGame != null) {
            ok = botGame.placeClientShip(ps);
            out.println("{\"type\":\"PLACE_SHIP_RESPONSE\",\"ok\":" + ok + "}");
        } else {
            out.println("{\"type\":\"PLACE_SHIP_RESPONSE\",\"ok\":false}");
        }
    }

    private void handleShotRequest(ShotRequest req) {
        if (isPvPMode && pvpGame != null) {
            // ===== MODE PVP =====
            System.out.println("🎯 [PvP] " + username + " tire en (" + (req.getX() + 1) + "," + (req.getY() + 1) + ")");

            PvPRoundResult result = pvpGame.processShot(req, isPlayer1InPvP);

            // Déboggage
            System.out.println("   [Debug] Résultat: " + result.getShooterResponse().getResultat());
            System.out.println("   [Debug] GameOver: " + result.getShooterResponse().isGameOver());
            System.out.println("   [Debug] Winner: " + result.getShooterResponse().getWinner());
            System.out.println("   [Debug] OpponentMsg is null: " + (result.getOpponentMsg() == null));

            // 1) Envoyer la réponse au tireur
            String shooterMsg = result.getShooterResponse().serialize();
            System.out.println("   [Debug] Message tireur: " + shooterMsg);
            out.println(shooterMsg);

            // 2) Envoyer l'info à l'adversaire SEULEMENT si le tir est valide
            if (result.getOpponentMsg() != null && opponentHandler != null && opponentHandler.getOut() != null) {
                String opponentMsg = result.getOpponentMsg().serialize();
                System.out.println("   [Debug] Message adversaire: " + opponentMsg);
                System.out.println("📤 [PvP] Envoi à " + opponentHandler.username);
                opponentHandler.getOut().println(opponentMsg);
            } else {
                if (result.getOpponentMsg() == null) {
                    System.out.println("   [Debug] Pas de message pour l'adversaire (tir invalide ou erreur)");
                } else if (opponentHandler == null) {
                    System.out.println("⚠️ [PvP] opponentHandler est null !");
                } else if (opponentHandler.getOut() == null) {
                    System.out.println("⚠️ [PvP] opponentHandler.getOut() est null !");
                }
            }

        } else if (botGame != null) {
            // ===== MODE BOT =====
            GameService.RoundResult round = botGame.processShot(req);

            out.println(round.getClientResponse().serialize());

            for (ServerShotMessage shot : round.getServerShots()) {
                out.println(shot.serialize());
            }
        }
    }

    private void handleNewGameRequest() {
        System.out.println("🔨 [" + username + "] NEW_GAME demandé");

        if (isPvPMode && pvpGame != null) {
            // 🔥 NOUVEAU : Gérer le rematch en PvP
            String status = pvpGame.requestRematch(isPlayer1InPvP);

            if (status.equals("WAITING")) {
                // En attente de l'adversaire
                RematchResponse waiting = new RematchResponse("WAITING", "En attente de la réponse de l'adversaire...");
                out.println(waiting.serialize());

            } else if (status.equals("ACCEPTED")) {
                // Les deux veulent rejouer !
                pvpGame.resetGame();

                RematchResponse accepted = new RematchResponse("ACCEPTED", "Votre adversaire a accepté ! Nouvelle partie.");
                out.println(accepted.serialize());

                // Notifier l'adversaire aussi
                if (opponentHandler != null) {
                    RematchResponse opponentAccepted = new RematchResponse("ACCEPTED", "Votre adversaire a accepté ! Nouvelle partie.");
                    opponentHandler.getOut().println(opponentAccepted.serialize());
                }
            }

        } else if (botGame != null) {
            // Mode BOT : simple reset
            botGame.resetGame();
            out.println("{\"type\":\"NEW_GAME_RESPONSE\",\"ok\":true}");
        }
    }

    private void cleanup() {
        System.out.println("🧹 [" + username + "] Début du nettoyage...");

        try {
            socket.close();
            System.out.println("   → Socket fermée");
        } catch (Exception ignore) {
        }

        // 🔥 Notifier l'adversaire en mode PvP
        if (isPvPMode && pvpGame != null && opponentHandler != null && opponentHandler.getOut() != null) {
            try {
                // Vérifier si l'adversaire attend une réponse de rematch
                boolean opponentIsWaiting = isPlayer1InPvP ?
                        pvpGame.doesOpponentWantRematch(true) :
                        pvpGame.doesOpponentWantRematch(false);

                if (opponentIsWaiting) {
                    System.out.println("📤 [" + username + "] L'adversaire attendait un rematch - envoi de OPPONENT_LEFT");
                    RematchResponse declined = new RematchResponse("OPPONENT_LEFT",
                            "Votre adversaire a quitté et ne souhaite pas rejouer.");
                    opponentHandler.getOut().println(declined.serialize());
                    pvpGame.cancelRematch(!isPlayer1InPvP);
                } else {
                    // Déconnexion normale pendant une partie
                    System.out.println("📤 [" + username + "] Envoi de notification de déconnexion");
                    OpponentLeftMessage leftMsg = new OpponentLeftMessage("disconnected");
                    opponentHandler.getOut().println(leftMsg.serialize());
                }

                System.out.println("   → Adversaire notifié");
            } catch (Exception e) {
                System.err.println("   ⚠️ Erreur lors de la notification de l'adversaire : " + e.getMessage());
            }
        }

        // 🔥 IMPORTANT : Retirer le joueur du matchmaking
        if (isPvPMode || pvpGame != null) {
            try {
                MatchmakingManager.getInstance().removePlayer(clientId);
                System.out.println("   → Retiré du matchmaking");
            } catch (Exception e) {
                System.err.println("   ⚠️ Erreur lors du retrait du matchmaking : " + e.getMessage());
            }
        }

        System.out.println("🔌 [" + username + "] Connexion fermée - Nettoyage terminé\n");
    }

    private static String errorJson(String code, String msg) {
        return "{\"type\":\"ERROR\",\"code\":\"" + code + "\",\"msg\":\"" + msg + "\"}";
    }

    public String getUsername() {
        return username;
    }
}