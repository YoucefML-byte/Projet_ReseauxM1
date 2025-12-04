package serveur;

import bâteaux.*;
import etats.Orientation;
import etats.ResultatTir;
import grille.Grille;
import grille.Grille.TirResult;
import joueur.Joueur;
import message.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameService {
    private Joueur joueurClient;//le client
    private Joueur joueurServeur;//le serveur

    private final int taille = 10;//pour la grille
    private final Random random = new Random();

    private boolean gameOver;
    private String winner;

    private final String clientUsername;//pseudo

    public GameService(String username) {
        this.clientUsername = username;
        resetGame();
    }

    /**
     * Cette fonction permet de gerer la reinitialisation de la partie
     * */
    public synchronized void resetGame() {
        System.out.println("🔄 [" + clientUsername + "] Réinitialisation de la partie...");
        gameOver = false;
        winner = null;

        joueurClient = new Joueur(clientUsername, taille);
        joueurServeur = new Joueur("Serveur", taille);

        // Placement aléatoire des bateaux du serveur
        placerAleatoire(joueurServeur, new PorteAvion());
        placerAleatoire(joueurServeur, new Croiseur());
        placerAleatoire(joueurServeur, new ContreTorpilleur());
        placerAleatoire(joueurServeur, new Torpilleur());


        System.out.println("\n⚓ [" + clientUsername + "] Grille du SERVEUR :");
        joueurServeur.getGrillePerso().afficher();
        System.out.println();
    }

    /**
     * Cette fonction permet placer les bâteaux du client
     * */
    public synchronized boolean placeClientShip(PlaceShipRequest req) {
        Bâteau b = creerBateau(req.getShipType());
        boolean horizontal = (req.getOrientation() == Orientation.HORIZONTAL);
        boolean ok = joueurClient.placerBateau(b, req.getX(), req.getY(), horizontal);

        if (ok) {
            System.out.println("[" + clientUsername + "] " + req.getShipType() + " placé en "
                    + (req.getX()+1) + "," + (req.getY()+1) + " " + (horizontal ? "H" : "V"));
        }
        return ok;
    }
    /**
     * Cette fonction permet de traiter les tirs (du joueur et du serveur)
     * */
    public synchronized RoundResult processShot(ShotRequest req) {
        if (gameOver) {
            return creerRoundResultGameOver();
        }

        int x = req.getX();
        int y = req.getY();

        //  Tir du client sur le serveur
        TirResult trClient = joueurServeur.getGrillePerso().tirer(x, y);
        ResultatTir resultat = trClient.getResultat();
        String nomBateau = (trClient.getBateau() != null) ? trClient.getBateau().getNom() : null;

        joueurClient.getGrilleTirs().marquerResultatTir(x, y, resultat);

        //  Vérifier immediatemment di y a victoire
        if (joueurServeur.aPerdu()) {
            return gererVictoireClient(resultat, nomBateau);
        }

        //si le client touche, il rejoue
        if (resultat == ResultatTir.HIT || resultat == ResultatTir.SUNK) {
            return new RoundResult(
                    new ShotResponse(resultat, nomBateau),
                    List.of(new ServerShotMessage(-1, -1, ResultatTir.MISS, null, false, null))
            );
        }

        // 4) Client raté = tour du serveur
        return new RoundResult(
                new ShotResponse(resultat, nomBateau),
                effectuerTirsServeurConsecutifs()
        );
    }

    /**
     * Cette fonction permet au serveur d'effectuer plusieurs dans le cas ou il touche un bâteau du joueur
     * */
    private List<ServerShotMessage> effectuerTirsServeurConsecutifs() {
        List<ServerShotMessage> shots = new ArrayList<>();
        Grille grilleClient = joueurClient.getGrillePerso();

        while (true) {
            // Trouver des coordonnées valides
            int x, y;
            TirResult tr;

            while (true) {
                x = random.nextInt(taille);
                y = random.nextInt(taille);
                tr = grilleClient.tirer(x, y);

                if (tr.getResultat() != ResultatTir.ALREADY_TRIED &&
                        tr.getResultat() != ResultatTir.OUT_OF_BOUNDS) {
                    break;
                }
            }

            ResultatTir res = tr.getResultat();
            String nomBateau = (tr.getBateau() != null) ? tr.getBateau().getNom() : null;
            joueurServeur.getGrilleTirs().marquerResultatTir(x, y, res);

            // Vérifier si le client à perdu
            if (joueurClient.aPerdu()) {
                gameOver = true;
                winner = "SERVER";
                System.out.println("💀 [" + clientUsername + "] SERVEUR A GAGNÉ !");
                shots.add(new ServerShotMessage(x, y, res, nomBateau, true, winner));
                return shots;
            }

            shots.add(new ServerShotMessage(x, y, res, nomBateau, false, null));

            // si le serveur rate son tir alors il arrête le tir
            if (res == ResultatTir.MISS) {
                return shots;
            }
        }
    }

    /**
     * Cette fonction permet de placer les bâteau du serveur
     * */
    private void placerAleatoire(Joueur joueur, Bâteau bateau) {
        while (true) {
            boolean horizontal = random.nextBoolean();
            int longueur = bateau.getLongueur();
            int maxX = horizontal ? (taille - longueur) : (taille - 1);
            int maxY = horizontal ? (taille - 1) : (taille - longueur);
            int x = random.nextInt(maxX + 1);
            int y = random.nextInt(maxY + 1);

            if (joueur.placerBateau(bateau, x, y, horizontal)) {
                break;
            }
        }
    }
    /**
     * Cette fonction permet de verifier et le gerer le cas ou le client gagne la partie
     * */
    private RoundResult gererVictoireClient(ResultatTir resultat, String nomBateau) {
        gameOver = true;
        winner = clientUsername;
        System.out.println("🎉 [" + clientUsername + "] A GAGNÉ LA PARTIE !");

        return new RoundResult(
                new ShotResponse(resultat, nomBateau, true, winner),
                List.of(new ServerShotMessage(-1, -1, ResultatTir.MISS, null, true, winner))
        );
    }

    private RoundResult creerRoundResultGameOver() {
        return new RoundResult(
                new ShotResponse(ResultatTir.MISS, null),
                List.of(new ServerShotMessage(-1, -1, ResultatTir.MISS, null, true, winner))
        );
    }
    /**
     * Cette fonction de creer les bâteaux pour les placer
     * */
    private Bâteau creerBateau(etats.ShipType type) {
        return switch (type) {
            case PORTE_AVION -> new PorteAvion();
            case CROISEUR -> new Croiseur();
            case CONTRE_TORPILLEUR -> new ContreTorpilleur();
            case TORPILLEUR -> new Torpilleur();
        };
    }
    /**
     * Cette classe permet de sotcker les resultats des tirs des du client et du serveur
     * */
    public static class RoundResult {
        private final ShotResponse clientResponse;
        private final List<ServerShotMessage> serverShots;

        public RoundResult(ShotResponse clientResponse, List<ServerShotMessage> serverShots) {
            this.clientResponse = clientResponse;
            this.serverShots = serverShots;
        }

        public ShotResponse getClientResponse() { return clientResponse; }
        public List<ServerShotMessage> getServerShots() { return serverShots; }
    }
}