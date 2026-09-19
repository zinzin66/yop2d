// haut 1
package com.ludexa.moteur;

import java.util.ArrayList;
import java.util.List;

// Mise en forme du script exporté (fenêtre "Résumé du Blueprint") : titres, champs et sorties des nœuds
// du catalogue affichés dans la langue de l'application. Les anciens nœuds gardent leur affichage.
public class ExportBlueprint {

    // Traduit une clé ; si elle n'existe pas dans les fichiers de langue (texte déjà traduit, ancien nœud),
    // la garde telle quelle.
    public static String traduireOuBrut(String cle) {
        if (cle == null) return "";
        String t = Traducteur.get(cle);
        return t.equals("[" + cle + "]") ? cle : t;
    }

    public static String formaterNoeud(NoeudBase noeud) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(traduireOuBrut(noeud.nom)).append("]");

        List<String> details = new ArrayList<>();

        if (noeud.requiertCibleObjet()) {
            if ("__OBJET_IMPLIQUE__".equals(noeud.nomCibleObjet)) {
                details.add(Traducteur.get("noeud_format_cible") + Traducteur.get("noeud_objet_implique_long"));
            } else if (noeud.nomCibleObjet != null && !noeud.nomCibleObjet.isEmpty()) {
                details.add(Traducteur.get("noeud_format_cible") + noeud.nomCibleObjet);
            } else if (noeud.getCibleObjet() != null) {
                details.add(Traducteur.get("noeud_format_cible") + noeud.getCibleObjet().nom);
            }
        }

        if (noeud.requiertCibleObjetB()) {
            if ("__OBJET_IMPLIQUE__".equals(noeud.nomCibleObjetB)) {
                details.add(Traducteur.get("noeud_format_objet_b") + Traducteur.get("noeud_objet_implique_long"));
            } else if (noeud.nomCibleObjetB != null && !noeud.nomCibleObjetB.isEmpty()) {
                details.add(Traducteur.get("noeud_format_objet_b") + noeud.nomCibleObjetB);
            } else if (noeud.getCibleObjetB() != null) {
                details.add(Traducteur.get("noeud_format_objet_b") + noeud.getCibleObjetB().nom);
            }
        }

        if (noeud.requiertCibleVariable() && noeud.getCibleVariable() != null) {
            details.add(Traducteur.get("noeud_format_var") + noeud.getCibleVariable().nom);
        }
        if (noeud.requiertCibleScene() && noeud.getCibleScene() != null) {
            details.add(Traducteur.get("noeud_format_scene") + noeud.getCibleScene().nom);
        }

        if (noeud.getNomsParametres() != null) {
            for (String param : noeud.getNomsParametres()) {
                String val = AideSaisie.texteAffiche(noeud, param);
                if (val != null && !val.isEmpty()) {
                    details.add(traduireOuBrut(param) + " : " + val);
                }
            }
        }

        if (!details.isEmpty()) {
            sb.append(" | ").append(String.join(" | ", details));
        }
        return sb.toString();
    }

    // La sortie "normale" d'un nœud (suite du script) ne s'écrit pas ; les autres (Vrai, Faux...) s'écrivent (Si Vrai).
    public static boolean estSortieNormale(String nomPort) {
        if (nomPort == null) return true;
        return nomPort.equals("Suivant") || nomPort.equals("Sortie")
                || nomPort.equals("port_suivant") || nomPort.equals("port_sortie")
                || nomPort.equals(Traducteur.get("port_suivant")) || nomPort.equals(Traducteur.get("port_sortie"));
    }

    public static String prefixe(String indentation, String portDeclencheur) {
        if (estSortieNormale(portDeclencheur)) return indentation + "-> ";
        return indentation + "(" + Traducteur.get("blueprint_si") + " " + traduireOuBrut(portDeclencheur) + ") -> ";
    }
}
// bas 1
