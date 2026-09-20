// haut 1
package com.ludexa.moteur;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// L'horloge du jeu. Elle décide de la pause et du ralenti, et elle déclenche les minuteurs
// (Attendre, Répéter, Compte à rebours). VueJeu l'appelle une fois par image, au début de onDraw().
//
// Le "temps du jeu" avance normalement, plus lentement en ralenti, et pas du tout en pause :
// les minuteurs suivent donc la pause et le ralenti.
// Tout repart de zéro à chaque nouvelle partie et à chaque changement de scène.
public class HorlogeJeu {

    // Un minuteur en attente.
    static class Minuteur {
        String cle = "";              // nom choisi dans le nœud (ou "@" + identifiant du nœud) ; vide pour un Attendre
        NoeudGenerique noeud;         // le nœud à qui appartient ce minuteur
        String type = "attendre";     // "attendre", "repeter" ou "compte"
        double echeance = 0;          // moment (en secondes de jeu) où il se déclenche
        double intervalle = 1;        // secondes entre deux déclenchements
        int restant = -1;             // déclenchements restants ; -1 = sans fin
        boolean annule = false;
    }

    public static double tempsJeu = 0;     // secondes de jeu écoulées
    public static float vitesse = 1f;      // 1 = normal, 0.5 = ralenti, 2 = accéléré
    public static boolean enPause = false;

    private static final int MAX_MINUTEURS = 500;
    private static final List<Minuteur> minuteurs = new ArrayList<>();
    private static double accumulateur = 0;
    private static long derniereImageNs = 0;
    private static long derniereAlerteMs = 0;
    private static Scene sceneConnue = null;

    // Tout remettre à zéro : plus de minuteur, temps à 0, vitesse normale, pas de pause.
    public static void reinitialiser() {
        for (Minuteur m : minuteurs) m.annule = true;
        minuteurs.clear();
        tempsJeu = 0;
        vitesse = 1f;
        enPause = false;
        accumulateur = 0;
        derniereImageNs = 0;
        sceneConnue = NoeudBase.sceneActiveCourante;
    }

    // Nouvelle partie ou changement de scène : la scène en cours n'est plus celle qu'on connaît, on repart de zéro.
    static void verifierScene() {
        if (NoeudBase.sceneActiveCourante != sceneConnue) reinitialiser();
    }

    public static void mettreEnPause(boolean pause) {
        verifierScene();
        enPause = pause;
    }

    public static void definirVitesse(float nouvelle) {
        verifierScene();
        if (Float.isNaN(nouvelle)) nouvelle = 1f;
        vitesse = Math.max(0f, Math.min(4f, nouvelle));
    }

    // Appelée une fois par image. Fait avancer le temps du jeu, déclenche les minuteurs arrivés à échéance,
    // et retourne le nombre de "pas de jeu" à jouer à cette image :
    // 0 en pause ou en ralenti, 1 normalement, 2 ou plus si le jeu est accéléré.
    public static int imageSuivante() {
        return imageSuivante(System.nanoTime());
    }

    static int imageSuivante(long maintenantNs) {
        verifierScene();
        double dt = (derniereImageNs == 0) ? 0 : (maintenantNs - derniereImageNs) / 1e9;
        derniereImageNs = maintenantNs;
        if (dt > 0.1) dt = 0.1;               // après une interruption (téléphone en veille...), pas de grand saut
        if (enPause) return 0;
        tempsJeu += dt * vitesse;
        declencherMinuteurs();
        if (enPause) return 0;
        accumulateur += vitesse;
        int pas = (int) accumulateur;
        accumulateur -= pas;
        if (pas > 4) {
            pas = 4;
            accumulateur = 0;
        }
        return pas;
    }

    // Programme un minuteur : il se déclenchera dans "delai" secondes de jeu.
    static void planifier(Minuteur m, double delai) {
        verifierScene();
        if (minuteurs.size() >= MAX_MINUTEURS) {
            long maintenant = System.currentTimeMillis();
            if (maintenant - derniereAlerteMs > 2500) {
                derniereAlerteMs = maintenant;
                journal("ALERTE minuteurs : " + MAX_MINUTEURS + " minuteurs en même temps, le nouveau est ignoré"
                        + " (un Attendre ou un Répéter est-il placé sous « À chaque image » ?)");
            }
            return;
        }
        m.echeance = tempsJeu + delai;
        minuteurs.add(m);
    }

    // Le minuteur en cours qui porte ce nom (ou null).
    static Minuteur trouver(String cle) {
        verifierScene();
        for (Minuteur m : minuteurs) {
            if (!m.annule && !m.cle.isEmpty() && m.cle.equals(cle)) return m;
        }
        return null;
    }

    // Arrête le minuteur de ce nom ; nom vide = tous les minuteurs (Attendre compris).
    static void arreter(String nom) {
        verifierScene();
        for (Minuteur m : minuteurs) {
            if (nom.isEmpty() || nom.equals(m.cle)) m.annule = true;
        }
        retirerAnnules();
    }

    private static void retirerAnnules() {
        Iterator<Minuteur> it = minuteurs.iterator();
        while (it.hasNext()) {
            if (it.next().annule) it.remove();
        }
    }

    private static void declencherMinuteurs() {
        if (minuteurs.isEmpty()) return;
        for (Minuteur m : new ArrayList<>(minuteurs)) {
            if (m.annule || m.echeance > tempsJeu) continue;
            declencher(m);
        }
        retirerAnnules();
    }

    // L'état du minuteur est mis à jour AVANT de suivre les sorties : si la suite l'arrête ou le relance, c'est pris en compte.
    private static void declencher(Minuteur m) {
        try {
            if (m.type.equals("attendre")) {
                m.annule = true;
                m.noeud.propagerExecution("port_suivant");
            } else if (m.type.equals("repeter")) {
                if (m.restant > 0) m.restant--;
                boolean fini = (m.restant == 0);
                if (fini) m.annule = true;
                else suivant(m);
                m.noeud.propagerExecution("port_a_chaque_fois");
                if (fini) m.noeud.propagerExecution("port_termine");
            } else {
                Variable v = m.noeud.getCibleVariable();
                if (v == null) {
                    m.annule = true;
                    return;
                }
                double valeur = Math.max(0, Evaluateur.enNombre(Evaluateur.valeurDe(v)) - 1);
                v.valeur = Evaluateur.convertirPourVariable(v, valeur);
                boolean fini = (valeur <= 0);
                if (fini) m.annule = true;
                else suivant(m);
                m.noeud.propagerExecution("port_chaque_seconde");
                if (fini) m.noeud.propagerExecution("port_termine");
            }
        } catch (RuntimeException e) {
            m.annule = true;
            journal("ERREUR minuteur " + m.noeud.cle + " : " + e);
        }
    }

    // Prochain déclenchement ; si le jeu a pris du retard, on ne rattrape pas les déclenchements manqués.
    private static void suivant(Minuteur m) {
        m.echeance += m.intervalle;
        if (m.echeance <= tempsJeu) m.echeance = tempsJeu + m.intervalle;
    }

    private static void journal(String message) {
        String chemin = NoeudBase.cheminProjetCourant;
        if (chemin == null && NoeudBase.contexteApplication instanceof InterfaceEditeur) {
            chemin = ((InterfaceEditeur) NoeudBase.contexteApplication).cheminProjet;
        }
        if (chemin != null) DiagLogger.log(chemin, message);
    }
}
// bas 1
