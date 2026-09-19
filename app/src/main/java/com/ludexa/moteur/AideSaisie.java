// haut 1
package com.ludexa.moteur;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

// Contenu du panneau d'aide de la fenêtre d'édition : ce que l'on peut insérer dans un champ
// (objets et leurs propriétés, variables, tags, fonctions).
// Uniquement de la logique, sans aucune vue : l'affichage est dans EditeurNoeudDialog.
// Contient aussi les décisions de la fenêtre (le champ est-il une formule ? un texte ?) et les libellés des listes de choix.
public class AideSaisie {

    // Une ligne du panneau : ce qui est affiché, ce qui est inséré, et ses sous-lignes (ex : les propriétés d'un objet).
    public static class Element {
        public final String libelle;
        public final String insertion;
        public final List<Element> enfants = new ArrayList<>();

        public final List<Element> enfants = new ArrayList<>();
        public String brut;   // nom seul, inséré tel quel dans les champs des anciens nœuds

        public Element(String libelle, String insertion) {
            this.libelle = libelle;
            this.insertion = insertion;
            this.brut = libelle;
        }
    }

    // Un groupe repliable du panneau.
    public static class Section {
        public final String cleTitre;   // clé de traduction du titre
        public final boolean ouverte;   // ouverte au départ ?
        public final List<Element> elements = new ArrayList<>();

        public Section(String cleTitre, boolean ouverte) {
            this.cleTitre = cleTitre;
            this.ouverte = ouverte;
        }
    }

    // Résultat d'une insertion : le nouveau texte du champ et la position du curseur.
    public static class Modification {
        public final String texte;
        public final int curseur;

        public Modification(String texte, int curseur) {
            this.texte = texte;
            this.curseur = curseur;
        }
    }

    private static final Pattern IDENTIFIANT = Pattern.compile("^[\\p{L}_][\\p{L}\\p{N}_]*$");
    private static final String[] MOTS_RESERVES = {"true", "false", "vrai", "faux", "pi", "implique", "impliqué"};

    private static final String[][] FONCTIONS = {
            {"random(1, 100)", "random(1, 100)"},
            {"distance(a, b)", "distance("},
            {"angle(a, b)", "angle("},
            {"abs(x)", "abs("},
            {"min(a, b)", "min("},
            {"max(a, b)", "max("},
            {"clamp(v, min, max)", "clamp("},
            {"sqrt(x)", "sqrt("},
            {"sin(°)", "sin("},
            {"cos(°)", "cos("},
            {"round(x)", "round("},
            {"floor(x)", "floor("},
            {"ceil(x)", "ceil("},
            {"int(x)", "int("},
            {"pi", "pi"},
            {"true", "true"},
            {"false", "false"}};

    // Nom tel qu'il doit être écrit dans une formule : entre crochets s'il contient des espaces,
    // des tirets ou tout autre signe, ou s'il ressemble à un mot réservé.
    public static String nomPourFormule(String nom) {
        if (nom == null) return "";
        if (!IDENTIFIANT.matcher(nom).matches()) return "[" + nom + "]";
        for (String reserve : MOTS_RESERVES) {
            if (reserve.equalsIgnoreCase(nom)) return "[" + nom + "]";
        }
        return nom;
    }

    // L'objet, avec ses propriétés (player.x...) et ses variables propres (alien.vie).
    public static Element elementObjet(String nom, List<Variable> variablesPropres) {
        String base = nomPourFormule(nom);
        Element e = new Element(nom, base);
        for (String propriete : ProprietesObjet.NOMS) {
            e.enfants.add(new Element(propriete, base + "." + propriete));
        }
        if (variablesPropres != null) {
            for (Variable v : variablesPropres) {
                if (v.nom == null || v.nom.isEmpty()) continue;
                e.enfants.add(new Element(v.nom + " (var)", base + "." + nomPourFormule(v.nom)));
            }
        }
        return e;
    }

    // Les sections du panneau, dans l'ordre d'affichage.
    public static List<Section> construire(Scene scene, List<Variable> globales) {
        List<Section> sections = new ArrayList<>();

        Section objets = new Section("noeud_objets_insertion", true);
        // L'objet impliqué (collision, clic...) : "implique" s'écrit sans crochets
        Element implique = new Element(Traducteur.get("noeud_objet_implique_long"), "implique");
        for (String propriete : ProprietesObjet.NOMS) {
            implique.enfants.add(new Element(propriete, "implique." + propriete));
        }
        objets.elements.add(implique);
        if (scene != null && scene.objets != null) {
            for (ObjetBase o : scene.objets) {
                if (o.nom == null || o.nom.isEmpty()) continue;
                objets.elements.add(elementObjet(o.nom, o.variablesLocales));
            }
        }
        sections.add(objets);

        Section variables = new Section("noeud_vars_locales_insertion", false);
        if (scene != null && scene.variablesLocales != null) {
            for (Variable v : scene.variablesLocales) {
                if (v.nom != null && !v.nom.isEmpty()) variables.elements.add(new Element(v.nom, nomPourFormule(v.nom)));
            }
        }
        sections.add(variables);

        // Variables de la scène, puis variables propres à chaque objet (insérées sous la forme objet.variable)
        Section variables = new Section("noeud_vars_locales_insertion", true);
        if (scene != null && scene.variablesLocales != null) {
            for (Variable v : scene.variablesLocales) {
                if (v.nom != null && !v.nom.isEmpty()) variables.elements.add(new Element(v.nom, nomPourFormule(v.nom)));
            }
        }
        if (scene != null && scene.objets != null) {
            for (ObjetBase o : scene.objets) {
                if (o.nom == null || o.nom.isEmpty() || o.variablesLocales == null) continue;
                for (Variable v : o.variablesLocales) {
                    if (v.nom == null || v.nom.isEmpty()) continue;
                    Element e = new Element(v.nom + " (" + o.nom + ")", nomPourFormule(o.nom) + "." + nomPourFormule(v.nom));
                    e.brut = v.nom;
                    variables.elements.add(e);
                }
            }
        }
        sections.add(variables);

        Section tags = new Section("noeud_tags_insertion", false);
        if (scene != null && scene.objets != null) {
            List<String> vus = new ArrayList<>();
            for (ObjetBase o : scene.objets) {
                if (o.tag == null) continue;
                String t = o.tag.trim();
                if (!t.isEmpty() && !vus.contains(t)) {
                    vus.add(t);
                    tags.elements.add(new Element(t, "\"" + t.replace("\"", "'") + "\""));
                }
            }
        }
        sections.add(tags);

        Section fonctions = new Section("editeur_section_fonctions", false);
        for (String[] f : FONCTIONS) fonctions.elements.add(new Element(f[0], f[1]));
        sections.add(fonctions);

        return sections;
    }

    // ------------------------------------------------------------------
    // INSERTION DANS LE CHAMP
    // ------------------------------------------------------------------

    public static boolean estFormule(String texte) {
        return texte != null && texte.trim().startsWith("=");
    }

    // Insère à la place de la sélection (debut..fin). Dans un champ de TEXTE simple, insérer une valeur
    // fait passer le champ en formule : "Score : " + insertion devient ="Score : " + score.
    public static Modification inserer(String texte, int debut, int fin, String insertion, boolean champTexte) {
        if (texte == null) texte = "";
        if (insertion == null) insertion = "";
        if (champTexte && !estFormule(texte)) {
            String litteral = texte.replace("\"", "'");
            String nouveau = litteral.isEmpty() ? "=" + insertion : "=\"" + litteral + "\" + " + insertion;
            return new Modification(nouveau, nouveau.length());
        }
        int a = Math.max(0, Math.min(debut, texte.length()));
        int b = Math.max(0, Math.min(fin, texte.length()));
        if (a > b) { int t = a; a = b; b = t; }
        String nouveau = texte.substring(0, a) + insertion + texte.substring(b);
        return new Modification(nouveau, a + insertion.length());
    }

    // Bouton "Passer en formule" : ="texte" (ou = seul si le champ est vide).
    public static Modification basculerEnFormule(String texte) {
        if (texte == null) texte = "";
        if (estFormule(texte)) return new Modification(texte, texte.length());
        String litteral = texte.replace("\"", "'");
        String nouveau = litteral.isEmpty() ? "=" : "=\"" + litteral + "\"";
        return new Modification(nouveau, nouveau.length());
    }

    // ------------------------------------------------------------------
    // DÉCISIONS DE LA FENÊTRE
    // ------------------------------------------------------------------

    // Vrai quand le champ "valeur" reçoit un texte écrit tel quel (variable de type Texte, propriété texte ou tag).
    static boolean valeurEstTexte(NoeudGenerique n) {
        CatalogueNoeuds.Definition d = CatalogueNoeuds.trouver(n.cle);
        if (d == null) return false;
        if (d.action.equals("modifier_propriete")) {
            String propriete = ProprietesObjet.cle(n.texteBrut("propriete"));
            return "texte".equals(propriete) || "tag".equals(propriete);
        }
        if (d.action.equals("modifier_variable") || d.action.equals("ajouter_variable")) {
            Variable v = n.getCibleVariable();
            return v != null && "TEXTE".equals(v.type);
        }
        return false;
    }

    // Champ de TEXTE d'un nœud du catalogue : écrit tel quel, sauf s'il commence par "=".
    public static boolean champTexte(NoeudBase noeud, String champ) {
        if (!(noeud instanceof NoeudGenerique) || champ == null) return false;
        String type = noeud.getTypeEditeurParametre(champ);
        return NoeudBase.TYPE_TEXTE_ALPHABETIQUE.equals(type)
                || (NoeudBase.TYPE_TEXTE_LIBRE.equals(type) && valeurEstTexte((NoeudGenerique) noeud));
    }

    // Faut-il calculer et afficher le résultat sous le champ ?
    public static boolean champEstCalcul(NoeudBase noeud, String champ, String texte) {
        if (!(noeud instanceof NoeudGenerique) || champ == null || texte == null) return false;
        if (estFormule(texte)) return NoeudBase.TYPE_TEXTE_LIBRE.equals(noeud.getTypeEditeurParametre(champ))
                || NoeudBase.TYPE_TEXTE_ALPHABETIQUE.equals(noeud.getTypeEditeurParametre(champ));
        return NoeudBase.TYPE_TEXTE_LIBRE.equals(noeud.getTypeEditeurParametre(champ)) && !champTexte(noeud, champ);
    }

    // Le bouton "Passer en formule (=)" est proposé pour un champ de texte qui n'est pas encore une formule.
    public static boolean peutPasserEnFormule(NoeudBase noeud, String champ, String texte) {
        return champTexte(noeud, champ) && !estFormule(texte);
    }

    // ------------------------------------------------------------------
    // LISTES DE CHOIX : chaque option a une VALEUR stable (enregistrée) et un LIBELLÉ traduit (affiché).
    // Les libellés viennent du dictionnaire "libelles" de catalogue_noeuds.json.
    // ------------------------------------------------------------------

    public static String libelle(String valeur) {
        if (valeur == null) return "";
        String cle = "option." + valeur;
        String t = Traducteur.get(cle);
        return t.equals("[" + cle + "]") ? valeur : t;
    }

    public static List<String> libellesOptions(List<String> valeurs) {
        List<String> libelles = new ArrayList<>();
        if (valeurs != null) for (String v : valeurs) libelles.add(libelle(v));
        return libelles;
    }

    public static String valeurPourLibelle(List<String> options, String texte) {
        if (options != null) {
            for (String o : options) {
                if (libelle(o).equals(texte)) return o;
            }
        }
        return texte;
    }

    // Texte à montrer dans le champ pour la valeur enregistrée.
    public static String texteAffiche(NoeudBase noeud, String champ) {
        String valeur = noeud.getValeurParametre(champ);
        if (valeur == null) return "";
        return NoeudBase.TYPE_CHOIX_LISTE.equals(noeud.getTypeEditeurParametre(champ)) ? libelle(valeur) : valeur;
    }

    // Valeur à enregistrer quand le champ affiche ce texte.
    public static String valeurAStocker(NoeudBase noeud, String champ, String texteSaisi) {
        if (NoeudBase.TYPE_CHOIX_LISTE.equals(noeud.getTypeEditeurParametre(champ))) {
            return valeurPourLibelle(noeud.getOptionsChoixListe(champ), texteSaisi);
        }
        return texteSaisi;
    }
}
// bas 1
