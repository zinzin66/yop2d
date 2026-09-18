// haut 1
package com.ludexa.moteur;

import java.util.ArrayList;
import java.util.List;

// Actions du moteur sur les objets : taille, image, couleur, destruction, clonage.
// (Appelées par ActionsMoteur ; chaque action lit ses champs avec n.nombre(...), n.texte(...).)
public class ActionsObjets {
  
// Définit l'échelle : 1 = taille normale, 2 = deux fois plus grand.
    static void definirEchelle(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        double x = n.nombre("x");
        double y = n.nombre("y");
        objet.scaleX = (float) x;
        objet.scaleY = (float) y;
    }

    // Change l'image de l'objet ; un champ vide efface l'image.
    static void changerImage(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        String chemin = n.texte("image").trim();
        objet.cheminImage = chemin.isEmpty() ? null : chemin;
    }

    // ------------------------------------------------------------------
    // COULEUR : accepte le nom choisi dans la fenêtre (dans la langue de l'application),
    // les noms français/anglais simples, ou un code #RRGGBB / #AARRGGBB.
    // ------------------------------------------------------------------

    private static final String[] CLES_COULEURS = {
            "couleur_bleu_defaut", "couleur_rouge", "couleur_vert", "couleur_noir",
            "couleur_blanc", "couleur_jaune", "couleur_magenta", "couleur_cyan"};
    private static final int[] VALEURS_COULEURS = {
            0xFF0000FF, 0xFFFF0000, 0xFF00FF00, 0xFF000000,
            0xFFFFFFFF, 0xFFFFFF00, 0xFFFF00FF, 0xFF00FFFF};
    private static final String[][] NOMS_SIMPLES = {
            {"bleu", "blue"}, {"rouge", "red"}, {"vert", "green"}, {"noir", "black"},
            {"blanc", "white"}, {"jaune", "yellow"}, {"magenta"}, {"cyan"}};

    static void modifierCouleur(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        String saisie = n.texte("couleur");
        Integer couleur = resoudreCouleur(saisie);
        if (couleur == null) throw new IllegalStateException("Couleur inconnue : " + saisie);
        objet.couleur = couleur;
    }

    static Integer resoudreCouleur(String saisie) {
        if (saisie == null) return null;
        String s = saisie.trim();
        if (s.isEmpty()) return null;
        if (s.startsWith("#")) {
            try {
                String hex = s.substring(1);
                if (hex.length() == 6) return (int) (0xFF000000L | Long.parseLong(hex, 16));
                if (hex.length() == 8) return (int) Long.parseLong(hex, 16);
            } catch (NumberFormatException e) {
                return null;
            }
            return null;
        }
        String cherche = Evaluateur.normaliser(s);
        for (int i = 0; i < CLES_COULEURS.length; i++) {
            if (cherche.equals(Evaluateur.normaliser(Traducteur.get(CLES_COULEURS[i])))) return VALEURS_COULEURS[i];
            for (String nom : NOMS_SIMPLES[i]) {
                if (cherche.equals(nom)) return VALEURS_COULEURS[i];
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // DESTRUCTION
    // ------------------------------------------------------------------

    private static Scene sceneContenant(ObjetBase objet) {
        Scene s = NoeudBase.sceneActiveCourante;
        if (s != null && s.objets != null && s.objets.contains(objet)) return s;
        s = NoeudBase.sceneHudActiveCourante;
        if (s != null && s.objets != null && s.objets.contains(objet)) return s;
        return null;
    }

    static void detruire(NoeudGenerique n) {
        detruireObjet(n.getCibleObjet());
    }

    // Neutralise l'objet (invisible, sans physique, hors écran) puis le retire de sa scène, avec ses enfants.
    static void detruireObjet(ObjetBase objet) {
        if (objet == null) return;
        Scene scene = sceneContenant(objet);
        objet.visible = false;
        objet.estPhysique = false;
        objet.estZoneDeClic = false;
        objet.estRamassable = false;
        objet.x = -99999;
        objet.y = -99999;
        if (scene == null) return;

        List<ObjetBase> enfants = new ArrayList<>();
        for (ObjetBase o : scene.objets) {
            if (objet.id != null && objet.id.equals(o.parentId)) enfants.add(o);
        }
        for (ObjetBase enfant : enfants) detruireObjet(enfant);
        scene.objets.remove(objet);
    }

    // Détruit tous les objets qui portent ce tag (majuscules ignorées), dans la scène et le HUD.
    static void detruireParTag(NoeudGenerique n) {
        String tag = n.texte("tag").trim();
        if (tag.isEmpty()) return;
        detruireParTag(NoeudBase.sceneActiveCourante, tag);
        detruireParTag(NoeudBase.sceneHudActiveCourante, tag);
    }

    private static void detruireParTag(Scene scene, String tag) {
        if (scene == null || scene.objets == null) return;
        List<ObjetBase> aDetruire = new ArrayList<>();
        for (ObjetBase o : scene.objets) {
            if (o.tag != null && tag.equalsIgnoreCase(o.tag.trim())) aDetruire.add(o);
        }
        for (ObjetBase o : aDetruire) detruireObjet(o);
    }

    // ------------------------------------------------------------------
    // CLONAGE : objet A = point d'apparition, objet B = modèle à cloner.
    // Le clone est centré sur le point d'apparition, avec son angle, devant les autres objets.
    // ------------------------------------------------------------------

    static void cloner(NoeudGenerique n) {
        ObjetBase pointApparition = n.getCibleObjet();
        ObjetBase modele = n.getCibleObjetB();
        if (pointApparition == null || modele == null) return;
        Scene scene = sceneContenant(modele);
        if (scene == null) scene = NoeudBase.sceneActiveCourante;
        if (scene == null || scene.objets == null) return;

        ObjetBase clone = modele.clonerProfond();
        clone.nom = modele.nom + "_clone_" + System.currentTimeMillis();
        clone.x = pointApparition.x + (pointApparition.largeur / 2f) - (clone.largeur / 2f);
        clone.y = pointApparition.y + (pointApparition.hauteur / 2f) - (clone.hauteur / 2f);
        clone.rotation = pointApparition.rotation;
        clone.visible = true;
        int z = 0;
        for (ObjetBase o : scene.objets) z = Math.max(z, o.zOrder);
        clone.zOrder = z + 1;
        scene.objets.add(clone);
    }
}
// bas 1
