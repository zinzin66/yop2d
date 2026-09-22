// haut 1
package com.ludexa.moteur;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

// Ce que font les nœuds « Afficher un dialogue », « Appeler une fonction » et « Appeler un événement ».
// Ils sont décrits dans assets/catalogue_noeuds.json.
//
// Une action retourne le nom du port de sortie à suivre, ou null pour la sortie normale.
public class ActionsAppels {

    // Appels imbriqués (une fonction ou un événement qui en appelle un autre) : au-delà, on s'arrête
    // pour ne pas figer le jeu si quelque chose s'appelle lui-même.
    private static final int PROFONDEUR_MAX = 20;
    private static int profondeur = 0;

    private static void alerte(String nomNoeud, String message) {
        String projet = NoeudBase.cheminProjetCourant;
        if (projet != null) DiagLogger.log(projet, "ALERTE " + nomNoeud + " : " + message);
    }

    private static String lireFichier(File fichier) throws Exception {
        StringBuilder texte = new StringBuilder();
        try (BufferedReader lecteur = new BufferedReader(new InputStreamReader(new FileInputStream(fichier), StandardCharsets.UTF_8))) {
            String ligne;
            while ((ligne = lecteur.readLine()) != null) texte.append(ligne).append('\n');
        }
        return texte.toString();
    }

    // ------------------------------------------------------------------
    // AFFICHER UN DIALOGUE
    // ------------------------------------------------------------------

    // Écrit dans le texte de l'objet le message qui correspond à la clé, dans assets_ludexa/Textes/dialogues.txt
    // (lignes « cle=message »). Si la clé est introuvable, la clé elle-même est affichée et le journal reçoit une ALERTE.
    static String afficherDialogue(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return null;
        String cle = n.texteBrut("cle").trim();
        if (cle.isEmpty()) {
            alerte("afficher_dialogue", "aucune clé de dialogue choisie");
            return null;
        }
        String message = chercherDialogue(cle);
        if (message == null) {
            alerte("afficher_dialogue", "clé introuvable dans dialogues.txt : « " + cle + " » (affichée telle quelle)");
            message = cle;
        }
        ProprietesObjet.ecrire(objet, "texte", message);
        return null;
    }

    private static String chercherDialogue(String cle) {
        String projet = NoeudBase.cheminProjetCourant;
        if (projet == null) return null;
        File fichier = new File(projet, "assets_ludexa/Textes/dialogues.txt");
        if (!fichier.exists()) return null;
        try {
            for (String ligne : lireFichier(fichier).split("\n")) {
                ligne = ligne.trim();
                if (ligne.isEmpty() || ligne.startsWith("//")) continue;
                int egal = ligne.indexOf('=');
                if (egal > 0 && ligne.substring(0, egal).trim().equals(cle)) {
                    return ligne.substring(egal + 1).trim();
                }
            }
        } catch (Exception e) {
            // fichier illisible : on considère la clé comme introuvable
        }
        return null;
    }

    // ------------------------------------------------------------------
    // APPELER UNE FONCTION
    // ------------------------------------------------------------------

    // Lance le script du fichier fonctions/<nom>.json du projet, à partir de son nœud de départ,
    // puis continue. Le fichier est relu à chaque appel : évite de placer ce nœud sous « À chaque image ».
    static String appelerFonction(NoeudGenerique n) {
        String nom = n.texteBrut("fonction").trim();
        if (nom.isEmpty()) {
            alerte("appeler_fonction", "aucune fonction choisie");
            return null;
        }
        String projet = NoeudBase.cheminProjetCourant;
        Scene scene = NoeudBase.sceneActiveCourante;
        if (projet == null || scene == null) return null;

        File fichier = new File(projet, "fonctions/" + nom + ".json");
        if (!fichier.exists()) {
            alerte("appeler_fonction", "fonction introuvable : « " + nom + " »");
            return null;
        }
        if (profondeur >= PROFONDEUR_MAX) {
            alerte("appeler_fonction", "trop d'appels imbriqués (" + PROFONDEUR_MAX + ") : une fonction s'appelle-t-elle elle-même ?");
            return null;
        }

        profondeur++;
        try {
            Blueprint sousBlueprint = Blueprint.fromJson(lireFichier(fichier), scene);
            NoeudBase depart = null;
            if (sousBlueprint != null && sousBlueprint.noeuds != null) {
                for (NoeudBase noeud : sousBlueprint.noeuds) {
                    if (noeud instanceof NoeudEventStart || "Début".equals(noeud.nom)) {
                        depart = noeud;
                        break;
                    }
                }
            }
            if (depart == null) {
                alerte("appeler_fonction", "aucun nœud de départ dans la fonction « " + nom + " »");
            } else {
                depart.executer();
            }
        } catch (Exception e) {
            alerte("appeler_fonction", "erreur dans la fonction « " + nom + " » : " + e);
        } finally {
            profondeur--;
        }
        return null;
    }

    // ------------------------------------------------------------------
    // APPELER UN ÉVÉNEMENT
    // ------------------------------------------------------------------

    // Lance l'événement local de ce nom, placé dans le MÊME script que ce nœud, puis continue.
    // Les majuscules et les espaces autour du nom sont ignorés.
    static String appelerEvenement(NoeudGenerique n) {
        String nom = n.texteBrut("evenement").trim();
        if (nom.isEmpty()) {
            alerte("appeler_evenement", "aucun nom d'événement");
            return null;
        }
        if (profondeur >= PROFONDEUR_MAX) {
            alerte("appeler_evenement", "trop d'appels imbriqués (" + PROFONDEUR_MAX + ") : un événement s'appelle-t-il lui-même ?");
            return null;
        }

        boolean trouve;
        profondeur++;
        try {
            trouve = MoteurLogique.appelerEvenementLocal(n, nom);
        } finally {
            profondeur--;
        }

        if (!trouve) {
            java.util.List<String> noms = MoteurLogique.nomsEvenementsLocaux(n);
            if (noms == null) {
                alerte("appeler_evenement", "script de ce nœud introuvable (un événement local ne s'appelle pas depuis une fonction)");
            } else if (noms.isEmpty()) {
                alerte("appeler_evenement", "événement « " + nom + " » introuvable : ce script n'a aucun événement local");
            } else {
                alerte("appeler_evenement", "événement « " + nom + " » introuvable. Événements de ce script : " + noms);
            }
        }
        return null;
    }
}
// bas 1
