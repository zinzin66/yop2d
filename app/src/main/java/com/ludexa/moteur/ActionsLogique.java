// haut 1
package com.ludexa.moteur;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// Ce que font les nœuds de logique : Séquence et Cooldown.
// Ils sont décrits dans assets/catalogue_noeuds.json.
//
// Une action retourne le nom du port de sortie à suivre, ou null pour la sortie normale.
public class ActionsLogique {

    // Nœud « Séquence » : lance toutes ses sorties, l'une après l'autre, dans l'ordre (1, 2, 3, 4).
    // Une sortie sans lien est simplement ignorée. Le nœud ne suit ensuite aucune autre sortie.
    static String sequence(NoeudGenerique n) {
        for (Port sortie : new ArrayList<>(n.portsSortie)) {
            n.propagerExecution(sortie.nom);
        }
        return ActionsTemps.AUCUNE_SUITE;
    }

    // Derniers passages du Cooldown, en secondes de jeu, pour chaque nœud et chaque objet.
    private static final Map<String, Double> derniers = new HashMap<>();

    // Nœud « Cooldown » : la sortie « Prêt » n'est suivie qu'une fois par délai (en secondes de jeu :
    // la pause et le ralenti sont pris en compte). Sinon, la sortie « Pas encore » est suivie.
    // Avec un objet choisi, chaque objet a son propre compte à rebours ; sans objet, il est commun à tous.
    static String cooldown(NoeudGenerique n) {
        double delai = n.nombre("delai");
        ObjetBase objet = n.getCibleObjet();
        String cle = n.id + "|" + (objet != null ? objet.id : "global");
        double maintenant = HorlogeJeu.tempsJeu;
        Double dernier = derniers.get(cle);
        // Le temps de jeu repart de zéro à chaque changement de scène : un dernier passage situé dans le futur est périmé.
        boolean pret = (dernier == null || maintenant < dernier || maintenant - dernier >= delai);
        if (pret) {
            if (derniers.size() > 500) derniers.clear();
            derniers.put(cle, maintenant);
            return "port_pret";
        }
        return "port_attente";
    }
}
// bas 1
