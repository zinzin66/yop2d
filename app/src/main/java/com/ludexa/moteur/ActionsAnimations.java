// haut 1
package com.ludexa.moteur;

// Ce que font les nœuds d'animation : jouer, arrêter, mettre en pause, reprendre, changer la vitesse.
// Ils sont décrits dans assets/catalogue_noeuds.json ; les images de l'animation sont avancées par VueJeu.
//
// Une action retourne le nom du port de sortie à suivre, ou null pour la sortie normale.
public class ActionsAnimations {

    // Nœud « Jouer animation » : lance l'animation choisie, à la vitesse donnée (images par seconde).
    // Si c'est déjà l'animation en cours, elle continue sans redémarrer : on peut donc appeler ce nœud
    // à chaque image (par exemple selon la direction du joystick) sans qu'elle reste bloquée sur sa première image.
    static String jouer(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return null;
        String nom = n.texteBrut("animation").trim();
        if (nom.isEmpty()) return null;

        if (objet.animationActive == null || !nom.equals(objet.animationActive)) {
            objet.animationActive = nom;
            objet.frameCourante = 0;
            objet.dernierTempsFrame = System.currentTimeMillis();
        }

        int fps = (int) Math.round(n.nombre("vitesse"));
        objet.vitesseFps = fps > 0 ? fps : 8;
        objet.boucleAnimation = n.booleen("boucle");
        objet.animationEnCours = true;
        return null;
    }

    // Nœud « Arrêter l'animation » : arrêt complet, retour à la première image, l'animation active est oubliée.
    static String arreter(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return null;
        objet.animationEnCours = false;
        objet.frameCourante = 0;
        objet.animationActive = null;
        objet.dernierTempsFrame = 0;
        return null;
    }

    // Nœud « Pause de l'animation » : elle s'arrête sur l'image en cours.
    static String pause(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return null;
        objet.animationEnCours = false;
        return null;
    }

    // Nœud « Reprendre l'animation » : elle repart de l'image en cours, sans sauter d'image.
    static String reprendre(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return null;
        objet.animationEnCours = true;
        objet.dernierTempsFrame = System.currentTimeMillis();
        return null;
    }

    // Nœud « Vitesse de l'animation » : change les images par seconde sans toucher au reste.
    static String vitesse(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return null;
        int fps = (int) Math.round(n.nombre("vitesse"));
        if (fps > 0) objet.vitesseFps = fps;
        return null;
    }
}
// bas 1
