// haut 1
package com.ludexa.moteur;

// Ce que font les nœuds d'animation : jouer, arrêter, mettre en pause, reprendre, changer la vitesse.
// Ils sont décrits dans assets/catalogue_noeuds.json ; les images de l'animation sont avancées par VueJeu.
//
// Les images avancent selon le temps du JEU (elles se figent en pause et ralentissent au ralenti),
// sauf pour les objets du HUD qui gardent l'horloge réelle. Ici, dernierTempsFrame = 0 veut dire
// « commencer à compter au prochain affichage » : c'est VueJeu qui choisit la bonne horloge.
//
// Chaque nœud écrit une ligne "ANIM" dans le journal du projet (diagnostic).
//
// Une action retourne le nom du port de sortie à suivre, ou null pour la sortie normale.
public class ActionsAnimations {

    private static void journal(String message) {
        DiagLogger.log(NoeudBase.cheminProjetCourant, "ANIM " + message);
    }

    private static String nomObjet(ObjetBase objet) {
        return objet != null && objet.nom != null ? objet.nom : "?";
    }

    // Nœud « Jouer animation » : lance l'animation choisie, à la vitesse donnée (images par seconde).
    // Si c'est déjà l'animation en cours, elle continue sans redémarrer : on peut donc appeler ce nœud
    // à chaque image (par exemple selon la direction du joystick) sans qu'elle reste bloquée sur sa première image.
    static String jouer(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) { journal("jouer : objet cible introuvable"); return null; }
        String nom = n.texteBrut("animation").trim();
        if (nom.isEmpty()) { journal("jouer " + nomObjet(objet) + " : aucun nom d'animation"); return null; }

        boolean nouvelle = objet.animationActive == null || !nom.equals(objet.animationActive);
        if (nouvelle) {
            objet.animationActive = nom;
            objet.frameCourante = 0;
            objet.dernierTempsFrame = 0;
        }

        double vitesseLue = n.nombre("vitesse");
        int fps = (int) Math.round(vitesseLue);
        objet.vitesseFps = fps > 0 ? fps : 8;
        objet.boucleAnimation = n.booleen("boucle");
        objet.animationEnCours = true;
        if (nouvelle) {
            journal("jouer " + nomObjet(objet) + " anim=" + nom + " vitesse lue=" + vitesseLue
                    + " fps=" + objet.vitesseFps + " boucle=" + objet.boucleAnimation
                    + " connue=" + objet.animations.containsKey(nom));
        }
        return null;
    }

    // Nœud « Arrêter l'animation » : arrêt complet, retour à la première image, l'animation active est oubliée.
    static String arreter(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) { journal("arreter : objet cible introuvable"); return null; }
        objet.animationEnCours = false;
        objet.frameCourante = 0;
        objet.animationActive = null;
        objet.dernierTempsFrame = 0;
        journal("arreter " + nomObjet(objet));
        return null;
    }

    // Nœud « Pause de l'animation » : elle s'arrête sur l'image en cours.
    static String pause(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) { journal("pause : objet cible introuvable"); return null; }
        objet.animationEnCours = false;
        journal("pause " + nomObjet(objet) + " image=" + objet.frameCourante);
        return null;
    }

    // Nœud « Reprendre l'animation » : elle repart de l'image en cours, sans sauter d'image.
    static String reprendre(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) { journal("reprendre : objet cible introuvable"); return null; }
        objet.animationEnCours = true;
        objet.dernierTempsFrame = 0;
        journal("reprendre " + nomObjet(objet) + " anim=" + objet.animationActive + " image=" + objet.frameCourante);
        return null;
    }

    // Nœud « Vitesse de l'animation » : change les images par seconde sans toucher au reste.
    static String vitesse(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) { journal("vitesse : objet cible introuvable"); return null; }
        double vitesseLue = n.nombre("vitesse");
        int fps = (int) Math.round(vitesseLue);
        if (fps > 0) objet.vitesseFps = fps;
        journal("vitesse " + nomObjet(objet) + " vitesse lue=" + vitesseLue + " fps maintenant=" + objet.vitesseFps);
        return null;
    }
}
// bas 1
