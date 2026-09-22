// haut 1
package com.ludexa.moteur;

// Ce que font les nœuds Sauvegarder l'état, Restaurer l'état et Fondu.
// Ils sont décrits dans assets/catalogue_noeuds.json.
//
// Une action retourne le nom du port de sortie à suivre, ou null pour la sortie normale.
public class ActionsPartie {

    private static long derniereAlerte = 0;

    private static void alerte(String nomNoeud, String message) {
        long maintenant = System.currentTimeMillis();
        if (maintenant - derniereAlerte < 2500) return;
        derniereAlerte = maintenant;
        String projet = NoeudBase.cheminProjetCourant;
        if (projet != null) DiagLogger.log(projet, "ALERTE " + nomNoeud + " : " + message);
    }

    // Nœud « Sauvegarder l'état » : mémorise les objets et variables de la scène active, et les variables globales.
    static String sauvegarderEtat(NoeudGenerique n) {
        Scene scene = NoeudBase.sceneActiveCourante;
        if (scene == null) return null;
        GestionnaireEtat.sauvegarderEtat(scene, NoeudBase.getVariablesGlobalesDisponibles());
        return null;
    }

    // Nœud « Restaurer l'état » : reprend la dernière sauvegarde de la scène active. Si aucune sauvegarde
    // n'existe encore, rien ne change et une ALERTE est écrite au journal.
    static String restaurerEtat(NoeudGenerique n) {
        Scene scene = NoeudBase.sceneActiveCourante;
        if (scene == null) return null;
        if (!GestionnaireEtat.aUneSauvegarde(scene)) {
            alerte("restaurer_etat", "aucune sauvegarde n'existe encore pour cette scène (utilise d'abord « Sauvegarder l'état »)");
            return null;
        }
        GestionnaireEtat.restaurerEtat(scene, NoeudBase.getVariablesGlobalesDisponibles());
        return null;
    }

    // Nœud « Fondu » : fait varier l'opacité de l'objet vers une valeur cible, en douceur, en secondes de jeu
    // (pause et ralenti compris). Ne bloque pas la suite : pour attendre la fin, ajoute un « Attendre » de la même durée.
    static String fondu(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) {
            alerte("fondu", "aucun objet choisi (cible A)");
            return null;
        }
        float cible = (float) n.nombre("cible");
        double duree = n.nombre("duree");
        FondusEnCours.demarrer(objet, cible, duree);
        return null;
    }
}
// bas 1
