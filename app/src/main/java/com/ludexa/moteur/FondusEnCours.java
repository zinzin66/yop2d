// haut 1
package com.ludexa.moteur;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// Les fondus (« Fondu ») en cours. Chacun fait varier l'opacité (alpha) d'un objet vers une valeur cible,
// pendant une durée exprimée en secondes de JEU (HorlogeJeu.tempsJeu) : il s'arrête donc en pause
// et ralentit avec le jeu, comme DeplacementsGlisses.
// VueJeu appelle avancer() une fois par image, juste après l'horloge.
public class FondusEnCours {

    private static class Fondu {
        ObjetBase objet;
        float depart, arrivee;
        double debut;   // tempsJeu au moment du départ
        double duree;   // secondes de jeu
    }

    private static final List<Fondu> enCours = new ArrayList<>();
    private static Scene sceneConnue = null;

    private static void verifierScene() {
        if (NoeudBase.sceneActiveCourante != sceneConnue) {
            enCours.clear();
            sceneConnue = NoeudBase.sceneActiveCourante;
        }
    }

    // Lance le fondu de l'objet vers cette opacité (0 à 1). Un fondu déjà en cours sur cet objet est remplacé.
    // Une durée nulle ou négative règle l'opacité tout de suite.
    static void demarrer(ObjetBase objet, float cible, double duree) {
        if (objet == null) return;
        verifierScene();
        retirer(objet);
        cible = Math.max(0f, Math.min(1f, cible));
        if (duree <= 0) {
            objet.alpha = cible;
            return;
        }
        Fondu f = new Fondu();
        f.objet = objet;
        f.depart = objet.alpha;
        f.arrivee = cible;
        f.debut = HorlogeJeu.tempsJeu;
        f.duree = duree;
        enCours.add(f);
    }

    static void retirer(ObjetBase objet) {
        Iterator<Fondu> it = enCours.iterator();
        while (it.hasNext()) {
            if (it.next().objet == objet) it.remove();
        }
    }

    static void avancer() {
        verifierScene();
        if (enCours.isEmpty()) return;
        Iterator<Fondu> it = enCours.iterator();
        while (it.hasNext()) {
            Fondu f = it.next();
            double progression = (HorlogeJeu.tempsJeu - f.debut) / f.duree;
            if (progression >= 1.0) {
                f.objet.alpha = f.arrivee;
                it.remove();
            } else if (progression > 0) {
                f.objet.alpha = (float) (f.depart + (f.arrivee - f.depart) * progression);
            }
        }
    }
}
// bas 1
