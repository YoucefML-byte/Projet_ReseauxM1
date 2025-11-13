package serveur;

import bâteaux.*;
import etats.Orientation;
import etats.ResultatTir;
import etats.ShipType;
import grille.Grille;
import grille.Grille.TirResult;
import joueur.Joueur;
import message.ServerShotMessage;
import message.ShotRequest;
import message.ShotResponse;
import message.PlaceShipRequest;

import java.util.Random;

public class GameService {

    private static final GameService INSTANCE = new GameService();
    public static GameService getInstance() { return INSTANCE; }

    private final Joueur joueurClient;   // bateaux du client (connus du serveur, via PLACE_SHIP)
    private final Joueur joueurServeur;  // bateaux du serveur
    private final Random random = new Random();
    private final int taille = 10;


    private GameService() {
        joueurClient = new Joueur("Client", taille);
        joueurServeur = new Joueur("Serveur", taille);

        placerAleatoire(joueurServeur, new PorteAvion(0, 4));
        placerAleatoire(joueurServeur, new Croiseur(0, 3));
        placerAleatoire(joueurServeur, new ContreTorpilleur(0, 2));
        placerAleatoire(joueurServeur, new Torpilleur(0, 1));

        System.out.println("⚓ Grille du SERVEUR (ses bateaux) :");
        joueurServeur.getGrillePerso().afficher();
    }

    // ✅ appelé quand le serveur reçoit un PLACE_SHIP du client
    public synchronized boolean placeClientShip(PlaceShipRequest req) {
        Bâteau b = switch (req.getShipType()) {
            case PORTE_AVION       -> new PorteAvion(0, 4);
            case CROISEUR          -> new Croiseur(0, 3);
            case CONTRE_TORPILLEUR -> new ContreTorpilleur(0, 2);
            case TORPILLEUR        -> new Torpilleur(0, 1);
        };

        boolean horizontal = (req.getOrientation() == Orientation.HORIZONTAL);
        boolean ok = joueurClient.placerBateau(b, req.getX(), req.getY(), horizontal);

        System.out.println("Placement bateau CLIENT (" + req.getShipType()
                + ") en (" + req.getX() + "," + req.getY() + ") "
                + (horizontal ? "H" : "V") + " -> " + (ok ? "OK" : "ECHEC"));

        if (ok) {
            System.out.println("Grille du CLIENT (connue du serveur) :");
            joueurClient.getGrillePerso().afficher();
        }

        return ok;
    }

    /**
     * Tir du CLIENT sur le SERVEUR.
     * Ensuite le SERVEUR tire sur le CLIENT.
     */
    public synchronized RoundResult processShot(ShotRequest req) {
        int x = req.getX();
        int y = req.getY();

        System.out.println("🎯 Tir du CLIENT en (" + x + "," + y + ") sur la grille du SERVEUR");

        // Tir du client sur la grille du serveur
        Grille grilleServ = joueurServeur.getGrillePerso();
        TirResult trClient = grilleServ.tirer(x, y);

        ResultatTir resultatClient = trClient.getResultat();
        String nomBateauClient = (trClient.getBateau() != null) ? trClient.getBateau().getNom() : null;

        // Mémoriser le tir dans la grille de tirs du client (côté serveur)
        joueurClient.getGrilleTirs().marquerResultatTir(x, y, resultatClient);

        System.out.println("   → Résultat du tir du client : " + resultatClient
                + (nomBateauClient != null ? " sur " + nomBateauClient : ""));
        System.out.println("🗺️ Grille du SERVEUR après le tir du client :");
        grilleServ.afficher();

        // Tir du serveur sur le client → on récupère un ServerShotMessage
        ServerShotMessage serverShot = effectuerTirServeurSurClient();

        // Vérifier fin de partie (optionnel)
        if (joueurServeur.aPerdu()) {
            System.out.println("💥 Tous les bateaux du SERVEUR sont coulés : le CLIENT a gagné !");
        }
        if (joueurClient.aPerdu()) {
            System.out.println("💀 Tous les bateaux du CLIENT sont coulés : le SERVEUR a gagné !");
        }

        // Réponse pour le client : son tir + le tir de l’ennemi
        ShotResponse clientRes = new ShotResponse(resultatClient, nomBateauClient);
        return new RoundResult(clientRes, serverShot);
    }



    /**
     * Le serveur tire au hasard sur la grille perso du CLIENT.
     */
    private ServerShotMessage effectuerTirServeurSurClient() {
        Grille grilleClient = joueurClient.getGrillePerso();
        Grille grilleTirsServeur = joueurServeur.getGrilleTirs();

        int x, y;
        while (true) {
            x = random.nextInt(taille);
            y = random.nextInt(taille);

            TirResult tr = grilleClient.tirer(x, y);
            ResultatTir res = tr.getResultat();

            if (res == ResultatTir.ALREADY_TRIED || res == ResultatTir.OUT_OF_BOUNDS) {
                continue; // on choisit une autre case
            }

            String nomBateau = (tr.getBateau() != null) ? tr.getBateau().getNom() : null;

            // Mémoriser sur la grille de tirs du serveur
            grilleTirsServeur.marquerResultatTir(x, y, res);

            System.out.println("🔥 Tir du SERVEUR en (" + x + "," + y + ") sur la grille du CLIENT");
            System.out.println("   → Résultat : " + res + (nomBateau != null ? (" sur " + nomBateau) : ""));
            System.out.println("🗺️ Grille du CLIENT après le tir du serveur :");
            grilleClient.afficher();
            System.out.println("------------------------------------");

            // On retourne le message à envoyer au client
            return new ServerShotMessage(x, y, res, nomBateau);
        }
    }


    /**
     * Place un bateau aléatoirement sur la grille perso d'un joueur,
     * sans sortir de la grille et sans chevaucher les autres bateaux.
     */
    private void placerAleatoire(Joueur joueur, Bâteau bateau) {
        while (true) {
            boolean horizontal = random.nextBoolean();

            int longueur = bateau.getLongueur();

            // On calcule les bornes max possibles pour x et y selon l'orientation
            int maxX = horizontal ? (taille - longueur) : (taille - 1);
            int maxY = horizontal ? (taille - 1) : (taille - longueur);

            int x = random.nextInt(maxX + 1);
            int y = random.nextInt(maxY + 1);

            boolean ok = joueur.placerBateau(bateau, x, y, horizontal);
            if (ok) {
                System.out.println("Serveur : " + bateau.getNom() + " placé en (" + x + "," + y + ") "
                        + (horizontal ? "HORIZONTAL" : "VERTICAL"));
                break;
            }
            // sinon, on recommence le while avec d'autres coordonnées
        }


    }

    public static class RoundResult {
        private final ShotResponse clientResponse;
        private final ServerShotMessage serverShot;

        public RoundResult(ShotResponse clientResponse, ServerShotMessage serverShot) {
            this.clientResponse = clientResponse;
            this.serverShot = serverShot;
        }

        public ShotResponse getClientResponse() { return clientResponse; }
        public ServerShotMessage getServerShot() { return serverShot; }
    }



}
