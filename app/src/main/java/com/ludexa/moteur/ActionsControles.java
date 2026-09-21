// haut 1
package com.ludexa.moteur;

// Ce que font les nœuds liés aux commandes tactiles (joystick, bouton d'action).
// Ils sont décrits dans assets/catalogue_noeuds.json.
//
// Une action retourne le nom du port de sortie à suivre, ou null pour la sortie normale.
public class ActionsControles {

    // Le joystick doit être poussé au-delà de ce seuil (0 à 1) pour compter comme utilisé.
    private static final float SEUIL_JOYSTICK = 0.2f;

    // Nœud « Si joystick » : une sortie par direction (haut, bas, gauche, droite) et une pour le repos.
    static String siJoystick(NoeudGenerique n) {
        float x = GestionnaireControles.joyDirX;
        float y = GestionnaireControles.joyDirY;
        float force = (float) Math.sqrt(x * x + y * y);
        if (force < SEUIL_JOYSTICK) return "port_repos";
        // la direction dominante l'emporte (le bas de l'écran est positif)
        if (Math.abs(x) > Math.abs(y)) return x > 0 ? "port_droite" : "port_gauche";
        return y > 0 ? "port_bas" : "port_haut";
    }
}
// bas 1
