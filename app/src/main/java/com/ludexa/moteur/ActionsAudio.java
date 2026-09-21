// haut 1
package com.ludexa.moteur;

import java.io.File;

// Ce que font les nœuds Audio : Jouer un son, Jouer la musique, Arrêter la musique.
// Les fichiers se choisissent dans assets_ludexa/Sons du projet ; le vrai travail est fait par GestionnaireAudio.
//
// Une action retourne le nom du port de sortie à suivre, ou null pour la sortie normale.
public class ActionsAudio {

    // Chemin complet du fichier choisi dans le champ "fichier".
    // Retourne null (avec une ligne ALERTE dans le journal) si le champ est vide ou si le fichier n'existe pas.
    private static String cheminFichier(String nomNoeud, NoeudGenerique n) {
        String projet = NoeudBase.cheminProjetCourant;
        if (projet == null) return null;
        String relatif = n.texteBrut("fichier").trim();
        if (relatif.isEmpty()) {
            DiagLogger.log(projet, "ALERTE " + nomNoeud + " : aucun fichier choisi");
            return null;
        }
        File fichier = new File(projet, relatif);
        if (!fichier.exists()) {
            DiagLogger.log(projet, "ALERTE " + nomNoeud + " : fichier introuvable : " + relatif);
            return null;
        }
        return fichier.getAbsolutePath();
    }

    // Nœud « Jouer un son » : un bruitage. Plusieurs peuvent jouer en même temps.
    static String jouerSon(NoeudGenerique n) {
        String chemin = cheminFichier("jouer_son", n);
        if (chemin != null) GestionnaireAudio.jouerSon(chemin);
        return null;
    }

    // Nœud « Jouer la musique » : elle remplace la musique en cours.
    // À ne pas placer sous « À chaque image » : elle repartirait du début à chaque image.
    static String jouerMusique(NoeudGenerique n) {
        String chemin = cheminFichier("jouer_musique", n);
        if (chemin != null) GestionnaireAudio.jouerMusique(chemin, n.booleen("boucle"));
        return null;
    }

    // Nœud « Arrêter la musique ».
    static String arreterMusique(NoeudGenerique n) {
        GestionnaireAudio.arreterMusique();
        return null;
    }
}
// bas 1
