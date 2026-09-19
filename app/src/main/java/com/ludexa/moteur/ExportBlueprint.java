// haut 1
package com.ludexa.moteur;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Mise en forme du script exporté (fenêtre "Résumé du Blueprint") : titres, champs et sorties des nœuds
// du catalogue affichés dans la langue de l'application. Les anciens nœuds gardent leur affichage.
public class ExportBlueprint {

    // Titre de chaque événement : les classes d'événements écrivent leur titre en dur (souvent en français) ;
    // on l'affiche donc d'après la clé de traduction que la palette utilise pour la même classe.
    private static final Map<String, String> TITRES_EVENEMENTS = new HashMap<>();
    static {
        TITRES_EVENEMENTS.put("NoeudEventStart", "noeud_au_demarrage");
        TITRES_EVENEMENTS.put("NoeudEventChaqueImage", "noeud_chaque_image");
        TITRES_EVENEMENTS.put("NoeudEventFinClic", "noeud_fin_de_clic");
        TITRES_EVENEMENTS.put("NoeudEventClicObjet", "noeud_au_clic_sur_objet");
        TITRES_EVENEMENTS.put("NoeudEventFinClicObjet", "noeud_fin_clic_sur_objet");
        TITRES_EVENEMENTS.put("NoeudEventMaintenuObjet", "noeud_event_maintenu_objet");
        TITRES_EVENEMENTS.put("NoeudEventDebutGlisser", "noeud_debut_de_glisser");
        TITRES_EVENEMENTS.put("NoeudEventFinGlisser", "noeud_fin_de_glisser");
        TITRES_EVENEMENTS.put("NoeudEventCollisionAB", "noeud_collision_ab");
        TITRES_EVENEMENTS.put("NoeudEventEntreeZone", "noeud_entree_de_zone");
        TITRES_EVENEMENTS.put("NoeudEventSortieZone", "noeud_sortie_de_zone");
        TITRES_EVENEMENTS.put("NoeudEventSurvolObjet", "noeud_au_survol");
        TITRES_EVENEMENTS.put("NoeudEventFinSurvol", "noeud_fin_de_survol");
        TITRES_EVENEMENTS.put("NoeudEventChoc", "noeud_au_choc_physique");
        TITRES_EVENEMENTS.put("NoeudEventVariableChange", "noeud_quand_variable_change");
        TITRES_EVENEMENTS.put("NoeudEventPersonnalise", "noeud_evenement_local");
        TITRES_EVENEMENTS.put("NoeudEventBoutonAction", "noeud_au_clic_action_aventure");
        TITRES_EVENEMENTS.put("NoeudEventCollisionTag", "noeud_si_objet_touche_tag");
    }

    // Traduit une clé ; si elle n'existe pas dans les fichiers de langue (texte déjà traduit, ancien nœud),
    // la garde telle quelle.
    public static String traduireOuBrut(String cle) {
        if (cle == null) return "";
        String t = Traducteur.get(cle);
        return t.equals("[" + cle + "]") ? cle : t;
    }

    // Titre à afficher pour un nœud : celui de son événement, ou son nom traduit.
    public static String titre(NoeudBase noeud) {
        String cle = TITRES_EVENEMENTS.get(noeud.getClass().getSimpleName());
        if (cle != null) return Traducteur.get(cle);
        return traduireOuBrut(noeud.nom);
    }

    public static String formaterNoeud(NoeudBase noeud) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(titre(noeud)).append("]");

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
