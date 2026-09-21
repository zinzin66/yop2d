// haut 1
package com.ludexa.moteur;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

// Ce que font les nœuds Scène et HUD : changer de scène, recommencer, ouvrir ou fermer un HUD,
// placer ou détruire des copies d'une scène, quitter le jeu. Ils sont décrits dans assets/catalogue_noeuds.json.
//
// Une action retourne le nom du port de sortie à suivre, ou null pour la sortie normale.
public class ActionsScene {

    // Le jeu qui tourne (dans l'éditeur en test, ou dans le jeu exporté).
    private static VueJeu vue() {
        Context c = NoeudBase.contexteApplication;
        VueJeu vue = null;
        if (c instanceof InterfaceEditeur) vue = ((InterfaceEditeur) c).getVueJeu();
        else if (c instanceof RunnerActivity) vue = ((RunnerActivity) c).getVueJeu();
        if (vue == null) throw new IllegalStateException("le jeu n'est pas lancé");
        return vue;
    }

    // Les scènes du projet.
    static List<Scene> scenes() {
        Context c = NoeudBase.contexteApplication;
        List<Scene> liste = null;
        if (c instanceof InterfaceEditeur) liste = ((InterfaceEditeur) c).listeScenes;
        else if (c instanceof RunnerActivity) liste = ((RunnerActivity) c).listeScenes;
        return liste != null ? liste : new ArrayList<Scene>();
    }

    // Retrouve une scène par son nom (sans tenir compte des majuscules).
    static Scene trouverScene(String nom) {
        if (nom == null) return null;
        String recherche = nom.trim();
        for (Scene s : scenes()) {
            if (s.nom != null && s.nom.trim().equalsIgnoreCase(recherche)) return s;
        }
        return null;
    }

    private static Scene sceneChoisie(NoeudGenerique n) {
        Scene s = n.getCibleScene();
        if (s == null) throw new IllegalStateException("aucune scène choisie dans ce nœud");
        return s;
    }

    // Aller à une scène. Si c'est la scène où l'on est déjà, elle recommence.
    private static void allerA(Scene cible) {
        VueJeu vue = vue();
        Scene actuelle = NoeudBase.sceneActiveCourante;
        if (actuelle != null && cible.id != null && cible.id.equals(actuelle.id)) vue.recommencerScene();
        else vue.chargerNouvelleScene(cible);
    }

    static String changerScene(NoeudGenerique n) {
        allerA(sceneChoisie(n));
        return null;
    }

    static String allerSceneNom(NoeudGenerique n) {
        String nom = n.texte("nom").trim();
        if (nom.isEmpty()) throw new IllegalStateException("aucun nom de scène");
        Scene cible = trouverScene(nom);
        if (cible == null) throw new IllegalStateException("scène introuvable : « " + nom + " »");
        allerA(cible);
        return null;
    }

    // Scène suivante ou précédente, dans l'ordre de la liste des scènes. La constante "decalage" vaut 1 ou -1.
    static String sceneRelative(NoeudGenerique n) {
        int decalage = n.texteBrut("decalage").trim().equals("-1") ? -1 : 1;
        List<Scene> liste = scenes();
        Scene actuelle = NoeudBase.sceneActiveCourante;
        int position = -1;
        for (int i = 0; i < liste.size(); i++) {
            Scene s = liste.get(i);
            if (actuelle != null && s.id != null && s.id.equals(actuelle.id)) {
                position = i;
                break;
            }
        }
        if (position < 0) throw new IllegalStateException("scène actuelle introuvable dans la liste des scènes");
        int cible = position + decalage;
        if (cible < 0 || cible >= liste.size()) {
            throw new IllegalStateException(decalage > 0
                    ? "pas de scène suivante : « " + actuelle.nom + " » est la dernière"
                    : "pas de scène précédente : « " + actuelle.nom + " » est la première");
        }
        vue().chargerNouvelleScene(liste.get(cible));
        return null;
    }

    static String recommencerScene(NoeudGenerique n) {
        vue().recommencerScene();
        return null;
    }

    static String ouvrirHud(NoeudGenerique n) {
        Scene hud = sceneChoisie(n);
        Context c = NoeudBase.contexteApplication;
        if (c instanceof InterfaceEditeur) ((InterfaceEditeur) c).ouvrirHUD(hud);
        else if (c instanceof RunnerActivity) ((RunnerActivity) c).ouvrirHUD(hud);
        return null;
    }

    static String fermerHud(NoeudGenerique n) {
        Context c = NoeudBase.contexteApplication;
        if (c instanceof InterfaceEditeur) ((InterfaceEditeur) c).fermerHUD();
        else if (c instanceof RunnerActivity) ((RunnerActivity) c).fermerHUD();
        return null;
    }

    // Place une copie de la scène choisie dans le jeu, à la position X, Y.
    static String placerCopieScene(NoeudGenerique n) {
        Scene modele = sceneChoisie(n);
        vue().instancierScene(modele, (float) n.nombre("x"), (float) n.nombre("y"));
        return null;
    }

    static String detruireCopiesScene(NoeudGenerique n) {
        vue().detruireInstances(sceneChoisie(n));
        return null;
    }

    // Dans le jeu exporté, l'application se ferme. Dans l'éditeur, on utilise le bouton stop.
    static String quitterJeu(NoeudGenerique n) {
        Context c = NoeudBase.contexteApplication;
        if (c instanceof RunnerActivity) {
            ((RunnerActivity) c).finish();
        } else if (NoeudBase.cheminProjetCourant != null) {
            DiagLogger.log(NoeudBase.cheminProjetCourant,
                    "ALERTE Quitter le jeu : en test dans l'éditeur, utilise le bouton stop (le jeu exporté, lui, se ferme)");
        }
        return null;
    }
}
// bas 1
