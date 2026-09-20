// haut 1
package com.ludexa.moteur;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RegistreNoeuds {
    public static class InfoNoeud {
        public String libelle;
        public String categorie;
        public String classeType;

        public InfoNoeud(String libelle, String categorie, String classeType) {
            this.libelle = libelle;
            this.categorie = categorie;
            this.classeType = classeType;
        }
    }

    public static final List<InfoNoeud> REGISTRE = new ArrayList<>();

    public static void initialiser() {
        REGISTRE.clear();

        // ÉVÉNEMENTS (écrits en Java : le moteur les déclenche par leur classe)
        REGISTRE.add(new InfoNoeud(Traducteur.get("noeud_au_demarrage"), Traducteur.get("cat_evenements"), "NoeudEventStart"));
        REGISTRE.add(new InfoNoeud(Traducteur.get("noeud_chaque_image"), Traducteur.get("cat_evenements"), "NoeudEventChaqueImage"));
        REGISTRE.add(new InfoNoeud(Traducteur.get("noeud_fin_de_clic"), Traducteur.get("cat_evenements"), "NoeudEventFinClic"));
        REGISTRE.add(new InfoNoeud(Traducteur.get("noeud_au_clic_sur_objet"), Traducteur.get("cat_evenements"), "NoeudEventClicObjet"));
        REGISTRE.add(new InfoNoeud(Traducteur.get("noeud_fin_clic_sur_objet"), Traducteur.get("cat_evenements"), "NoeudEventFinClicObjet"));
        REGISTRE.add(new InfoNoeud(Traducteur.get("noeud_event_maintenu_objet"), Traducteur.get("cat_evenements"), "NoeudEventMaintenuObjet"));
        REGISTRE.add(new InfoNoeud(Traducteur.get("noeud_debut_de_glisser"), Traducteur.get("cat_evenements"), "NoeudEventDebutGlisser"));
        REGISTRE.add(new InfoNoeud(Traducteur.get("noeud_fin_de_glisser"), Traducteur.get("cat_evenements"), "NoeudEventFinGlisser"));
        REGISTRE.add(new InfoNoeud(Traducteur.get("noeud_collision_ab"), Traducteur.get("cat_evenements"), "NoeudEventCollisionAB"));
        REGISTRE.add(new InfoNoeud(Traducteur.get("noeud_entree_de_zone"), Traducteur.get("cat_evenements"), "NoeudEventEntreeZone"));
        REGISTRE.add(new InfoNoeud(Traducteur.get("noeud_sortie_de_zone"), Traducteur.get("cat_evenements"), "NoeudEventSortieZone"));
        REGISTRE.add(new InfoNoeud(Traducteur.get("noeud_au_survol"), Traducteur.get("cat_evenements"), "NoeudEventSurvolObjet"));
        REGISTRE.add(new InfoNoeud(Traducteur.get("noeud_fin_de_survol"), Traducteur.get("cat_evenements"), "NoeudEventFinSurvol"));
        REGISTRE.add(new InfoNoeud(Traducteur.get("noeud_au_choc_physique"), Traducteur.get("cat_evenements"), "NoeudEventChoc"));
        REGISTRE.add(new InfoNoeud(Traducteur.get("noeud_quand_variable_change"), Traducteur.get("cat_evenements"), "NoeudEventVariableChange"));
        REGISTRE.add(new InfoNoeud(Traducteur.get("noeud_evenement_local"), Traducteur.get("cat_evenements"), "NoeudEventPersonnalise"));
        REGISTRE.add(new InfoNoeud(Traducteur.get("noeud_au_clic_action_aventure"), Traducteur.get("cat_evenements"), "NoeudEventBoutonAction"));
        REGISTRE.add(new InfoNoeud(Traducteur.get("noeud_si_objet_touche_tag"), Traducteur.get("cat_evenements"), "NoeudEventCollisionTag"));

        // TOUS LES AUTRES NŒUDS : décrits dans assets/catalogue_noeuds.json
        for (CatalogueNoeuds.Definition d : CatalogueNoeuds.toutes()) {
            REGISTRE.add(new InfoNoeud(Traducteur.get(d.cleNom), Traducteur.get(d.categorie), "cle:" + d.cle));
        }
    }

    public static Map<String, List<InfoNoeud>> getNoeudsParCategorie() {
        if (REGISTRE.isEmpty()) {
            initialiser();
        }
        Map<String, List<InfoNoeud>> map = new LinkedHashMap<>();
        for (InfoNoeud info : REGISTRE) {
            if (!map.containsKey(info.categorie)) {
                map.put(info.categorie, new ArrayList<>());
            }
            map.get(info.categorie).add(info);
        }
        return map;
    }
}
// bas 1
