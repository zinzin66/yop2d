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

    static Scene sceneContenant(ObjetBase objet) {
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
    // CLONAGE
    // ------------------------------------------------------------------

    // Crée un clone du modèle dans la scène du modèle (nom unique, devant les autres objets) et le retourne.
    // Retourne null si aucune scène n'est disponible.
    static ObjetBase creerClone(ObjetBase modele) {
        Scene scene = sceneContenant(modele);
        if (scene == null) scene = NoeudBase.sceneActiveCourante;
        if (scene == null || scene.objets == null) return null;
        ObjetBase clone = modele.clonerProfond();
        clone.nom = modele.nom + "_clone_" + System.currentTimeMillis();
        clone.visible = true;
        int z = 0;
        for (ObjetBase o : scene.objets) z = Math.max(z, o.zOrder);
        clone.zOrder = z + 1;
        scene.objets.add(clone);
        return clone;
    }

    // Objet A = point d'apparition, objet B = modèle à cloner.
    // Le clone est centré sur le point d'apparition, avec son angle, devant les autres objets.
    static void cloner(NoeudGenerique n) {
        ObjetBase pointApparition = n.getCibleObjet();
        ObjetBase modele = n.getCibleObjetB();
        if (pointApparition == null || modele == null) return;
        ObjetBase clone = creerClone(modele);
        if (clone == null) return;
        clone.x = pointApparition.x + (pointApparition.largeur / 2f) - (clone.largeur / 2f);
        clone.y = pointApparition.y + (pointApparition.hauteur / 2f) - (clone.hauteur / 2f);
        clone.rotation = pointApparition.rotation;
    }

    // Objet B = modèle à cloner ; le clone apparaît à la position X/Y et devient l'"objet impliqué".
    static void clonerPosition(NoeudGenerique n) {
        ObjetBase modele = n.getCibleObjetB();
        if (modele == null) return;
        double x = n.nombre("x");
        double y = n.nombre("y");
        ObjetBase clone = creerClone(modele);
        if (clone == null) return;
        clone.x = (float) x;
        clone.y = (float) y;
        MoteurLogique.dernierObjetImplique = clone;
    }

// haut 1
    // Objet A = point de départ, objet B = cible (pour "Vers la cible"), champ "modele" = nom de l'objet à tirer.
    // Le projectile avance tout seul à la vitesse donnée, dans la direction choisie.
    static void tirer(NoeudGenerique n) {
        ObjetBase depart = n.getCibleObjet();
        if (depart == null) return;
        String nomModele = n.texte("modele").trim();
        ObjetBase modele = Evaluateur.trouverObjet(nomModele);
        if (modele == null) throw new IllegalStateException("Modèle introuvable : « " + nomModele + " »");
        double vitesse = n.nombre("vitesse");
        ObjetBase clone = creerClone(modele);
        if (clone == null) return;
        clone.x = depart.x + (depart.largeur / 2f) - (clone.largeur / 2f);
        clone.y = depart.y + (depart.hauteur / 2f) - (clone.hauteur / 2f);

        String direction = n.texteBrut("direction");
        float angle = depart.rotation;
        if ("→".equals(direction)) {
            angle = 0f;
        } else if ("↓".equals(direction)) {
            angle = 90f;
        } else if ("←".equals(direction)) {
            angle = 180f;
        } else if ("↑".equals(direction)) {
            angle = -90f;
        } else if ("Vers la cible".equals(direction)) {
            ObjetBase cible = n.getCibleObjetB();
            if (cible != null) {
                float dx = (cible.x + cible.largeur / 2f) - (clone.x + clone.largeur / 2f);
                float dy = (cible.y + cible.hauteur / 2f) - (clone.y + clone.hauteur / 2f);
                angle = (float) Math.toDegrees(Math.atan2(dy, dx));
            }
        }
        // sinon ("Angle de départ" ou valeur inconnue) : angle déjà réglé sur depart.rotation

        clone.rotation = angle;
        clone.vitesseAvanceContinue = (float) vitesse;
    }
    // ------------------------------------------------------------------
    // EFFETS, LIENS, VERROU
    // ------------------------------------------------------------------

    // Filtre couleur / mode de fusion : le rendu lit ce texte (Aucun, Noir et Blanc, Sepia, Additif...).
    static void definirFiltre(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        objet.filtreCouleur = n.texteBrut("filtre");
    }

    // Retourne l'objet : Horizontal inverse la gauche/droite, Vertical le haut/bas.
    static void miroir(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        String axe = n.texteBrut("axe");
        if ("Horizontal".equals(axe)) objet.scaleX = -objet.scaleX;
        else if ("Vertical".equals(axe)) objet.scaleY = -objet.scaleY;
    }

    static void verrouiller(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        objet.estVerrouille = n.booleen("valeur");
    }

    // Parallaxe : 1 = suit la caméra normalement, 0.5 = défile deux fois moins vite.
    static void definirParallaxe(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        objet.facteurParallaxe = (float) n.nombre("facteur");
    }

    // Objet A (enfant) suit l'objet B (parent).
    static void lierObjets(NoeudGenerique n) {
        ObjetBase enfant = n.getCibleObjet();
        ObjetBase parent = n.getCibleObjetB();
        if (enfant == null || parent == null) return;
        if (!enfant.id.equals(parent.id)) enfant.parentId = parent.id;
    }

    // La couleur est transmise telle quelle au rendu.
    static void surbrillance(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        objet.surbrillanceActive = "Activer".equals(n.texteBrut("etat"));
        objet.couleurSurbrillance = n.texteBrut("couleur");
    }

    // Le rendu fait clignoter l'objet ; ce nœud règle seulement l'état, la vitesse et la durée (0 = sans fin).
    static void clignotement(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        if ("Désactiver".equals(n.texteBrut("etat"))) {
            objet.clignotementActif = false;
            objet.etatVisibleClignotement = true;
            return;
        }
        long vitesse = Math.round(n.nombre("vitesse"));
        long duree = Math.round(n.nombre("duree"));
        objet.clignotementActif = true;
        objet.tempsDebutClignotement = System.currentTimeMillis();
        objet.clignotementVitesseMs = vitesse > 0 ? vitesse : 500;
        objet.clignotementDureeTotalMs = Math.max(0, duree);
    }
}
// bas 1
