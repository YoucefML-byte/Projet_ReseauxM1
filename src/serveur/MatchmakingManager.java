package serveur;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Singleton qui gère le matchmaking des joueurs PvP
 */
public class MatchmakingManager {

    private static MatchmakingManager instance;

    // File d'attente des joueurs cherchant un match
    private final Queue<WaitingPlayer> waitingQueue;

    // Map des parties PvP actives : gameId -> PvPGameService
    private final Map<String, PvPGameService> activeGames;

    // Map des joueurs vers leur partie : clientId -> gameId
    private final Map<String, String> playerToGame;

    private MatchmakingManager() {
        this.waitingQueue = new ConcurrentLinkedQueue<>();
        this.activeGames = new ConcurrentHashMap<>();
        this.playerToGame = new ConcurrentHashMap<>();
    }

    public static synchronized MatchmakingManager getInstance() {
        if (instance == null) {
            instance = new MatchmakingManager();
        }
        return instance;
    }

    /**
     * Ajouter un joueur dans la file d'attente
     * @return le joueur avec qui il est matché, ou null si en attente
     */
    public synchronized WaitingPlayer addPlayerToQueue(String clientId, String username, ClientHandler handler) {
        System.out.println("🔍 [Matchmaking] " + username + " rejoint la file d'attente");
        System.out.println("[DEBUG Serveur] clientId = " + clientId); // 🔥 ICI
        System.out.println("[DEBUG Serveur] username = " + username);

        WaitingPlayer newPlayer = new WaitingPlayer(clientId, username, handler);
        System.out.println("[DEBUG Serveur] newPlayer créé - username = " + newPlayer.username); // 🔥 ICI

        // 🔥 DEBUG
        System.out.println("[DEBUG] newPlayer.username = " + newPlayer.username);

        WaitingPlayer opponent = waitingQueue.poll();

        if (opponent == null) {
            waitingQueue.add(newPlayer);
            System.out.println("⏳ [Matchmaking] " + username + " est en attente...");
            return null;
        }else {
            System.out.println("[DEBUG Serveur] Adversaire trouvé dans la file"); // 🔥 ICI
            System.out.println("[DEBUG Serveur] opponent.clientId = " + opponent.clientId); // 🔥 ICI
            System.out.println("[DEBUG Serveur] opponent.username = " + opponent.username); // 🔥 ICI
            System.out.println("[DEBUG Serveur] opponent.handler = " + (opponent.handler != null ? "OK" : "NULL"));
            // Un adversaire est trouvé !
            System.out.println("✅ [Matchmaking] Match trouvé : " + newPlayer.username + " vs " + opponent.username);

            // Créer une nouvelle partie PvP
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

            // Enregistrer la partie dans les handlers avec référence croisée
            newPlayer.handler.setPvPGame(game, true, opponent.handler);   // joueur 1
            opponent.handler.setPvPGame(game, false, newPlayer.handler);  // joueur 2

            return opponent;
        }
    }

    /**
     * Récupérer la partie PvP d'un joueur
     */
    public PvPGameService getGameForPlayer(String clientId) {
        String gameId = playerToGame.get(clientId);
        if (gameId == null) return null;
        return activeGames.get(gameId);
    }

    /**
     * Retirer un joueur du matchmaking (déconnexion)
     */
    public synchronized void removePlayer(String clientId) {
        // Retirer de la file d'attente
        waitingQueue.removeIf(wp -> wp.clientId.equals(clientId));

        // Retirer de la partie active
        String gameId = playerToGame.remove(clientId);
        if (gameId != null) {
            PvPGameService game = activeGames.get(gameId);
            if (game != null) {
                System.out.println("⚠️ [Matchmaking] Joueur " + clientId + " déconnecté d'une partie PvP");

                // 🔥 Trouver l'adversaire et le notifier
                for (Map.Entry<String, String> entry : playerToGame.entrySet()) {
                    if (entry.getValue().equals(gameId) && !entry.getKey().equals(clientId)) {
                        // On a trouvé l'adversaire
                        String opponentId = entry.getKey();
                        System.out.println("📤 [Matchmaking] Notification de l'adversaire : " + opponentId);

                        // L'adversaire sera notifié par son ClientHandler.cleanup()
                        // On supprime juste sa référence à cette partie
                        playerToGame.remove(opponentId);
                        break;
                    }
                }
            }
            activeGames.remove(gameId);
        }

        System.out.println("🗑️ [Matchmaking] Joueur " + clientId + " retiré du système");
    }

    /**
     * Supprimer une partie terminée
     */
    public void removeGame(String clientId) {
        String gameId = playerToGame.remove(clientId);
        if (gameId != null) {
            activeGames.remove(gameId);
            System.out.println("🗑️ [Matchmaking] Partie PvP supprimée");
        }
    }

    /**
     * Obtenir le nombre de joueurs en attente
     */
    public int getWaitingPlayersCount() {
        return waitingQueue.size();
    }

    /**
     * Obtenir le nombre de parties actives
     */
    public int getActiveGamesCount() {
        return activeGames.size();
    }

    /**
     * Classe interne pour représenter un joueur en attente
     */
    public static class WaitingPlayer {
        public final String clientId;
        public final String username;
        public final ClientHandler handler;

        public WaitingPlayer(String clientId, String username, ClientHandler handler) {
            this.clientId = clientId;
            this.username = username;
            this.handler = handler;
        }
    }
}