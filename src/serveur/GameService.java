package serveur;

import bâteaux.*;
import etats.Orientation;
import etats.ResultatTir;
import etats.ShipType;
import grille.Grille;
import grille.Grille.TirResult;
import joueur.Joueur;
import message.ShotRequest;
import message.ShotResponse;
import message.PlaceShipRequest;

import java.util.Random;

public class GameService {

    private static final GameService INSTANCE = new GameService();
    public static GameService getInstance() { return INSTANCE; }

    private final Joueur joueurClient;
    private final Joueur joueurServeur;
    private final Random random = new Random();

    private GameService() {
        int taille = 10;

        joueurClient = new Joueur("Client", taille);
        joueurServeur = new Joueur("Serveur", taille);

        // ✅ On place SEULEMENT les bateaux du SERVEUR ici
        Grille gServ = joueurServeur.getGrillePerso();
        gServ.placer(new PorteAvion(0, 4), 1, 1, Orientation.HORIZONTAL);
        gServ.placer(new Croiseur(0, 3), 3, 4, Orientation.VERTICAL);
        gServ.placer(new ContreTorpilleur(0, 2), 6, 2, Orientation.HORIZONTAL);
        gServ.placer(new Torpilleur(0, 1), 8, 7, Orientation.VERTICAL);

        System.out.println("⚓ Grille du SERVEUR (bateaux du serveur) :");
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
     * Tir du client sur le serveur.
     * Le serveur répond avec le résultat du tir du client,
     * mais en interne il joue aussi un tir sur la grille du client.
     */
    // ✅ tir du client sur la grille du serveur (comme avant)
    public synchronized ShotResponse processShot(ShotRequest req) {
        int x = req.getX();
        int y = req.getY();

        Grille grilleServ = joueurServeur.getGrillePerso();
        TirResult tr = grilleServ.tirer(x, y);

        ResultatTir resultat = tr.getResultat();
        String nomBateau = (tr.getBateau() != null) ? tr.getBateau().getNom() : null;

        System.out.println("🎯 Tir du client en (" + x + "," + y + ") -> " + resultat
                + (nomBateau != null ? (" sur " + nomBateau) : ""));

        System.out.println("Grille du SERVEUR après ce tir :");
        grilleServ.afficher();

        return new ShotResponse(resultat, nomBateau);
    }

    /**
     * Le serveur tire au hasard sur la grille du client.
     * On met à jour :
     *  - la grille perso du client (ses bateaux qui se font toucher)
     *  - la grille de tirs du serveur (ce qu'il sait du client)
     */
    private void effectuerTirServeurSurClient() {
        Grille grilleClient = joueurClient.getGrillePerso();
        Grille grilleTirsServeur = joueurServeur.getGrilleTirs();

        int taille = 10; // on sait que la grille est 10x10 dans ce setup
        int x, y;

        // Cherche une case où le serveur n'a pas encore tiré
        while (true) {
            x = random.nextInt(taille);
            y = random.nextInt(taille);

            // On regarde dans la mémo de tirs du serveur
            // (simplifié : si tirMemo[x][y] est null, c'est qu'on n'a pas tiré ici)
            // On ne peut pas accéder directement à tirMemo car il est private,
            // donc on se contente ici de faire un tir et d'accepter ALREADY_TRIED si c'est un doublon.
            // Variante simple : sortir de la boucle dès qu'on a un tir non ALREADY_TRIED.

            TirResult tr = grilleClient.tirer(x, y); // tir sur les bateaux du client
            ResultatTir res = tr.getResultat();
            String nomBateau = (tr.getBateau() != null) ? tr.getBateau().getNom() : null;

            // On enregistre le tir sur la grille de tirs du serveur
            grilleTirsServeur.marquerResultatTir(x, y, res);

            System.out.println("🔥 Tir du SERVEUR en (" + x + "," + y + ") sur la grille du CLIENT");
            System.out.println("   → Résultat : " + res + (nomBateau != null ? (" sur " + nomBateau) : ""));
            System.out.println("🗺️ Grille du CLIENT après le tir du serveur :");
            grilleClient.afficher();
            System.out.println("------------------------------------");
            break;
        }
    }
}
