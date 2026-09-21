// haut 1
package com.ludexa.moteur;

import java.util.List;

// Ce que font les nœuds d'inventaire : Ajouter à l'inventaire, Retirer de l'inventaire, Si dans l'inventaire.
// L'inventaire est une variable de type « Liste inventaire » : elle garde l'identifiant de chaque objet ramassé.
// Chaque nœud vise deux choses : l'objet (cible A) et la variable inventaire.
//
// Une action retourne le nom du port de sortie à suivre, ou null pour la sortie normale.
public class ActionsInventaire {

    private static long derniereAlerte = 0;

    // ALERTE au journal, au plus une fois toutes les 2,5 secondes (ce nœud peut être placé sous « À chaque image »).
    private static void alerte(String nomNoeud, String message) {
        long maintenant = System.currentTimeMillis();
        if (maintenant - derniereAlerte < 2500) return;
        derniereAlerte = maintenant;
        String projet = NoeudBase.cheminProjetCourant;
        if (projet != null) DiagLogger.log(projet, "ALERTE " + nomNoeud + " : " + message);
    }

    // La liste de l'inventaire choisi, ou null (avec une ALERTE) si la variable n'est pas une liste d'inventaire.
    @SuppressWarnings("unchecked")
    private static List<String> liste(String nomNoeud, NoeudGenerique n) {
        Variable variable = n.getCibleVariable();
        if (variable == null) return null;   // « aucune variable choisie » est déjà signalé par le nœud lui-même
        if (!"LISTE_INVENTAIRE".equals(variable.type) || !(variable.valeur instanceof List)) {
            alerte(nomNoeud, "la variable « " + variable.nom + " » n'est pas de type Liste inventaire");
            return null;
        }
        return (List<String>) variable.valeur;
    }

    // Nœud « Ajouter à l'inventaire » : l'objet y est mis une seule fois.
    static String ajouter(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) {
            alerte("ajouter_inventaire", "aucun objet choisi (cible A)");
            return null;
        }
        List<String> liste = liste("ajouter_inventaire", n);
        if (liste != null && !liste.contains(objet.id)) liste.add(objet.id);
        return null;
    }

    // Nœud « Retirer de l'inventaire ».
    static String retirer(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) {
            alerte("retirer_inventaire", "aucun objet choisi (cible A)");
            return null;
        }
        List<String> liste = liste("retirer_inventaire", n);
        if (liste != null) liste.remove(objet.id);
        return null;
    }

    // Nœud « Si dans l'inventaire » : Vrai si l'objet est dans la liste, Faux sinon.
    static String siDansInventaire(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) {
            alerte("si_dans_inventaire", "aucun objet choisi (cible A)");
            return "port_faux";
        }
        List<String> liste = liste("si_dans_inventaire", n);
        boolean dedans = (liste != null && liste.contains(objet.id));
        return dedans ? "port_vrai" : "port_faux";
    }
}
// bas 1
