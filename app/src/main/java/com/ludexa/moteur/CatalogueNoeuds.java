// haut 1
package com.ludexa.moteur;

import android.content.Context;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Catalogue des nœuds : lit assets/catalogue_noeuds.json, qui décrit chaque nœud
// (titre, catégorie, champs, action). Ajouter un nœud = ajouter une entrée dans ce fichier.
//
// Un nœud du catalogue est décrit ainsi :
//   "cle"        nom interne, stable, jamais traduit            (obligatoire)
//   "action"     action du moteur à lancer (voir ActionsMoteur)  (par défaut : la clé)
//   "categorie"  clé de traduction d'une catégorie existante
//   "nomCle"     clé de traduction déjà existante du titre       (sinon "nom" ci-dessous)
//   "nom"        titre : un texte, ou {"fr": "...", "en": "..."}
//   "objet", "objetB", "variable"   true = le nœud a un bouton "Cible objet A / B / variable"
//   "sorties"    ["suivant"] par défaut ; ["vrai","faux"] pour une condition
//   "champs"     [{"cle","type","defaut","nom","options"}]
//                type : "formule" (calcul), "texte" (texte libre, ou calcul s'il commence par =), "choix" (liste)
public class CatalogueNoeuds {

    public static class Champ {
        public String cle;        // nom interne du champ, ex : "x"
        public String nomParam;   // clé de sauvegarde ET de traduction : "<noeud>.<champ>", ex : "deplacer.x"
        public String type = "formule";
        public String defaut = "";
        public List<String> options = new ArrayList<>();   // pour "choix" ; "@proprietes" = propriétés modifiables d'un objet
    }

    public static class Definition {
        public String cle;
        public String cleNom;        // clé de traduction du titre
        public String categorie;     // clé de traduction de la catégorie
        public String action;
        public boolean objet, objetB, variable;
        public List<String> sorties = new ArrayList<>();   // noms des ports de sortie, ex : "port_suivant"
        public List<Champ> champs = new ArrayList<>();
    }

    private static final Map<String, Definition> definitions = new LinkedHashMap<>();
    private static boolean charge = false;
    public static volatile String erreur = null;

    // Toutes les définitions, dans l'ordre du fichier.
    public static synchronized List<Definition> toutes() {
        chargerSiBesoin();
        return new ArrayList<>(definitions.values());
    }

    public static synchronized Definition trouver(String cle) {
        chargerSiBesoin();
        return definitions.get(cle);
    }

    // Pour les tests ou un rechargement manuel.
    public static synchronized void reinitialiser() {
        definitions.clear();
        charge = false;
        erreur = null;
    }

    private static void chargerSiBesoin() {
        if (charge) return;
        Context contexte = NoeudBase.contexteApplication;
        if (contexte == null) return; // l'application n'est pas encore prête : on réessaiera
        try {
            InputStream flux = contexte.getAssets().open("catalogue_noeuds.json");
            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            byte[] tampon = new byte[4096];
            int lu;
            while ((lu = flux.read(tampon)) != -1) sortie.write(tampon, 0, lu);
            flux.close();
            analyser(new String(sortie.toByteArray(), StandardCharsets.UTF_8));
            erreur = null;
        } catch (Exception e) {
            definitions.clear();
            erreur = "Catalogue de nœuds illisible : " + e;
            try {
                Toast.makeText(contexte, erreur, Toast.LENGTH_LONG).show();
            } catch (RuntimeException ignore) {
                // pas d'affichage possible à cet instant
            }
            if (NoeudBase.cheminProjetCourant != null) DiagLogger.log(NoeudBase.cheminProjetCourant, "CATALOGUE " + erreur);
        }
        charge = true;
    }

    // Lit le texte JSON du catalogue (public pour pouvoir le tester).
    public static synchronized void analyser(String texte) throws Exception {
        definitions.clear();
        JSONObject racine = new JSONObject(texte);
        JSONArray noeuds = racine.getJSONArray("noeuds");
        for (int i = 0; i < noeuds.length(); i++) {
            JSONObject n = noeuds.getJSONObject(i);
            Definition d = new Definition();
            d.cle = n.getString("cle");
            if (definitions.containsKey(d.cle)) throw new Exception("Nœud en double dans le catalogue : " + d.cle);
            d.action = n.optString("action", d.cle);
            d.categorie = n.optString("categorie", "cat_apparence_objets");
            d.objet = n.optBoolean("objet", false);
            d.objetB = n.optBoolean("objetB", false);
            d.variable = n.optBoolean("variable", false);

            String nomCle = n.optString("nomCle", "");
            if (!nomCle.isEmpty()) {
                d.cleNom = nomCle;
            } else {
                d.cleNom = "noeud_" + d.cle;
                enregistrerTraductions(d.cleNom, n.opt("nom"));
            }

            JSONArray sorties = n.optJSONArray("sorties");
            if (sorties == null || sorties.length() == 0) {
                d.sorties.add("port_suivant");
            } else {
                for (int k = 0; k < sorties.length(); k++) d.sorties.add("port_" + sorties.getString(k));
            }

            JSONArray champs = n.optJSONArray("champs");
            if (champs != null) {
                for (int k = 0; k < champs.length(); k++) {
                    JSONObject c = champs.getJSONObject(k);
                    Champ champ = new Champ();
                    champ.cle = c.getString("cle");
                    champ.nomParam = d.cle + "." + champ.cle;
                    champ.type = c.optString("type", "formule");
                    champ.defaut = c.optString("defaut", "");
                    JSONArray options = c.optJSONArray("options");
                    if (options != null) {
                        for (int m = 0; m < options.length(); m++) champ.options.add(options.getString(m));
                    }
                    enregistrerTraductions(champ.nomParam, c.opt("nom"));
                    d.champs.add(champ);
                }
            }
            definitions.put(d.cle, d);
        }
    }

    // Donne à Traducteur les textes du catalogue (utilisés quand le fichier de langue n'a pas la clé).
    private static void enregistrerTraductions(String cle, Object nom) {
        Map<String, String> parLangue = new LinkedHashMap<>();
        if (nom instanceof String) {
            parLangue.put("fr", (String) nom);
        } else if (nom instanceof JSONObject) {
            JSONObject o = (JSONObject) nom;
            Iterator<String> langues = o.keys();
            while (langues.hasNext()) {
                String langue = langues.next();
                parLangue.put(langue, o.optString(langue, ""));
            }
        }
        if (!parLangue.isEmpty()) Traducteur.ajouterSecours(cle, parLangue);
    }
}
// bas 1
