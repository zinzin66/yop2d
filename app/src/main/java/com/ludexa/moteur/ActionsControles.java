// haut 1
package com.ludexa.moteur;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

// Ce que font les nœuds liés aux commandes tactiles (joystick, bouton d'action) et à la caméra.
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

    // Nœud « Fixer la caméra » : la caméra suit l'objet choisi.
    // Sans objet choisi, la caméra ne suit plus rien.
    static String fixerCamera(NoeudGenerique n) {
        ObjetBase cible = n.getCibleObjet();
        if (cible == null) {
            GestionnaireControles.cameraCibleId = null;
            return null;
        }
        GestionnaireControles.cameraCibleId = cible.id;
        GestionnaireControles.cameraSuitAxeX = n.booleen("suivre_x");
        GestionnaireControles.cameraSuitAxeY = n.booleen("suivre_y");
        GestionnaireControles.parallaxeUniquementX = n.booleen("parallaxe_x");
        return null;
    }

    // Nœud « Élasticité de la caméra » : vitesse à laquelle la caméra rattrape sa cible (0.01 = très souple, 1 = collée).
    static String elasticiteCamera(NoeudGenerique n) {
        double v = n.nombre("vitesse");
        if (v < 0.001) v = 0.001;
        if (v > 1.0) v = 1.0;
        VueJeu.vitesseSuiviCamera = (float) v;
        return null;
    }

    // Nœud « Tremblement de caméra » : secoue l'écran pendant une durée (en millisecondes).
    static String tremblementCamera(NoeudGenerique n) {
        double intensite = n.nombre("intensite");
        double duree = n.nombre("duree");
        if (intensite < 0) intensite = 0;
        if (duree < 0) duree = 0;
        VueJeu.tremblementIntensite = (float) intensite;
        VueJeu.tremblementFin = System.currentTimeMillis() + (long) duree;
        return null;
    }

    // Nœud « Vibration » : fait vibrer l'appareil pendant une durée (en millisecondes).
    // Sans moteur de vibration, il ne se passe rien.
    static String vibration(NoeudGenerique n) {
        Context contexte = NoeudBase.contexteApplication;
        long duree = (long) n.nombre("duree");
        if (contexte == null || duree <= 0) return null;
        Vibrator vibreur = (Vibrator) contexte.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibreur == null || !vibreur.hasVibrator()) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibreur.vibrate(VibrationEffect.createOneShot(duree, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibreur.vibrate(duree);
        }
        return null;
    }
}
// bas 1
