// haut 1
package com.ludexa.moteur;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// Les déplacements « Glisser vers » en cours. Chaque déplacement va d'une position de départ à une position d'arrivée,
// pendant une durée exprimée en secondes de JEU (HorlogeJeu.tempsJeu) : il s'arrête donc en pause et ralentit avec le jeu.
// VueJeu appelle avancer() une fois par image, juste après l'horloge.
public class DeplacementsGlisses {

    private static class Deplacement {
        ObjetBase objet;
        float departX, departY;
        float arriveeX, arriveeY;
        double debut;   // tempsJeu au moment du départ
        double duree;   // secondes de jeu
    }

    private static final List<Deplacement> enCours = new ArrayList<>();
    private static Scene sceneConnue = null;

    // Une nouvelle scène : plus aucun déplacement en cours.
    private static void verifierScene() {
        if (NoeudBase.sceneActiveCourante != sceneConnue) {
            enCours.clear();
            sceneConnue = NoeudBase.sceneActiveCourante;
        }
    }

    // Lance le glissement de l'objet vers (x, y). Un glissement déjà en cours sur cet objet est remplacé.
    // Une durée nulle ou négative place l'objet tout de suite.
    static void demarrer(ObjetBase objet, float x, float y, double duree) {
        if (objet == null) return;
        verifierScene();
        retirer(objet);
        if (duree <= 0) {
            objet.x = x;
            objet.y = y;
            return;
        }
        Deplacement d = new Deplacement();
        d.objet = objet;
        d.departX = objet.x;
        d.departY = objet.y;
        d.arriveeX = x;
        d.arriveeY = y;
        d.debut = HorlogeJeu.tempsJeu;
        d.duree = duree;
        enCours.add(d);
    }

    // Arrête le glissement d'un objet (il reste où il est).
    static void retirer(ObjetBase objet) {
        Iterator<Deplacement> it = enCours.iterator();
        while (it.hasNext()) {
            if (it.next().objet == objet) it.remove();
        }
    }

    // Appelée une fois par image : place chaque objet selon le temps de jeu écoulé.
    static void avancer() {
        verifierScene();
        if (enCours.isEmpty()) return;
        Iterator<Deplacement> it = enCours.iterator();
        while (it.hasNext()) {
            Deplacement d = it.next();
            double progression = (HorlogeJeu.tempsJeu - d.debut) / d.duree;
            if (progression >= 1.0) {
                d.objet.x = d.arriveeX;
                d.objet.y = d.arriveeY;
                it.remove();
            } else if (progression > 0) {
                d.objet.x = (float) (d.departX + (d.arriveeX - d.departX) * progression);
                d.objet.y = (float) (d.departY + (d.arriveeY - d.departY) * progression);
            }
        }
    }
}
// bas 1
