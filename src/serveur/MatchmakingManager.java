package serveur;

import java.util.*;
import java.util.concurrent.*;

public class MatchmakingManager {
    private static MatchmakingManager instance;

    private final Queue<WaitingPlayer> waitingQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, PvPGameService> activeGames = new ConcurrentHashMap<>();
    private final Map<String, String> playerToGame = new ConcurrentHashMap<>();

    private MatchmakingManager() {}

    public static synchronized MatchmakingManager getInstance() {
        if (instance == null) {
            instance = new MatchmakingManager();
        }
        return instance;
    }

    public synchronized WaitingPlayer addPlayerToQueue(String clientId, String username, ClientHandler handler) {
        System.out.println("🔍 [Matchmaking] " + username + " rejoint la file d'attente");

        WaitingPlayer newPlayer = new WaitingPlayer(clientId, username, handler);
        WaitingPlayer opponent = waitingQueue.poll();

        if (opponent == null) {
            waitingQueue.add(newPlayer);
            System.out.println("⏳ [Matchmaking] " + username + " est en attente...");
            return null;
        }

        // Match trouvé !
        System.out.println("✅ [Matchmaking] Match trouvé : " + newPlayer.username + " vs " + opponent.username);

        // Créer partie PvP
        String gameId = UUID.randomUUID().toString();
        PvPGameService game = new PvPGameService(
                newPlayer.username,
                opponent.username,
                newPlayer.handler.getOut(),
                opponent.handler.getOut()
        );

        activeGames.put(gameId, game);
        playerToGame.put(newPlayer.clientId, gameId);
        playerToGame.put(opponent.clientId, gameId);

        // Enregistrer dans les handlers
        newPlayer.handler.setPvPGame(game, true, opponent.handler);
        opponent.handler.setPvPGame(game, false, newPlayer.handler);

        return opponent;
    }

    public synchronized void removePlayer(String clientId) {
        waitingQueue.removeIf(wp -> wp.clientId.equals(clientId));

        String gameId = playerToGame.remove(clientId);
        if (gameId != null) {
            // Trouver et notifier l'adversaire
            playerToGame.entrySet().stream()
                    .filter(e -> e.getValue().equals(gameId) && !e.getKey().equals(clientId))
                    .findFirst()
                    .ifPresent(e -> playerToGame.remove(e.getKey()));

            activeGames.remove(gameId);
            System.out.println("🗑️ [Matchmaking] Joueur " + clientId + " retiré");
        }
    }

    public record WaitingPlayer(String clientId, String username, ClientHandler handler) {}
}