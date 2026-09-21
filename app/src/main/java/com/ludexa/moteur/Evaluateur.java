// haut 1
package com.ludexa.moteur;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

// Lecteur de valeurs : transforme le texte d'un champ (ex : "player.x + 5") en nombre,
// vrai/faux ou texte. Il sert à TOUS les nœuds du catalogue.
//
// Ce que l'on peut écrire dans un champ :
//   nombres      5   -3.5   +5   .5
//   calculs      + - * / %   et les parenthèses
//   variables    score        (recherche : objet du nœud, scène, HUD, globales, puis n'importe quel objet)
//   objets       player.x     [Mon Objet].y     alien.vie (variable propre à un objet)
//   spécial      implique.x   (l'objet impliqué dans la collision ou le clic en cours)
//   fonctions    random(1,100) abs() sqrt() sin() cos() round() floor() ceil() int() min() max()
//                clamp(v,min,max) distance(a,b) angle(a,b)
//   comparaisons == != < <= > >=   (un simple = marche aussi)      logique && || !   true false
//   texte        "Score : " + score
// Les angles sont en degrés : 0 = vers la droite, 90 = vers le bas (comme à l'écran).
public class Evaluateur {

    public static class ErreurFormule extends RuntimeException {
        public ErreurFormule(String message) {
            super(message);
        }
    }

    // Dernier message d'erreur d'une formule (pour affichage / diagnostic). null si aucune.
    public static volatile String derniereErreur = null;
    private static long dernierSignalement = 0;

    private interface Expr {
        Object eval(ObjetBase ctx);
    }

    private static final Map<String, Expr> CACHE = new ConcurrentHashMap<>();
    private static final Random RNG = new Random();

    // ------------------------------------------------------------------
    // POINTS D'ENTRÉE (les nœuds utilisent ceux-là)
    // ctx = l'objet principal du nœud (ses variables sont cherchées en premier), peut être null.
    // En cas d'erreur : valeur par défaut (0, false, "") + message noté dans le journal (limité à 1 toutes les 500 ms).
    // ------------------------------------------------------------------

    public static double nombre(String formule, ObjetBase ctx) {
        if (formule == null || formule.trim().isEmpty()) return 0.0;
        try {
            return enNombre(evaluer(formule, ctx));
        } catch (RuntimeException e) {
            signaler(formule, e);
            return 0.0;
        }
    }

    public static boolean booleen(String formule, ObjetBase ctx) {
        if (formule == null || formule.trim().isEmpty()) return false;
        try {
            return enBooleen(evaluer(formule, ctx));
        } catch (RuntimeException e) {
            signaler(formule, e);
            return false;
        }
    }

    // Pour les champs de texte : le texte est pris tel quel, sauf s'il commence par "="
    // (comme dans un tableur) : "= "Score : " + score" est alors calculé.
    public static String texteOuFormule(String saisie, ObjetBase ctx) {
        if (saisie == null) return "";
        String t = saisie.trim();
        if (!t.startsWith("=")) return saisie;
        try {
            return enTexte(evaluer(t, ctx));
        } catch (RuntimeException e) {
            signaler(saisie, e);
            return "";
        }
    }

    // Version qui signale l'erreur à l'appelant (utile pour un bouton "tester la formule").
    public static Object evaluer(String formule, ObjetBase ctx) {
        String f = formule.trim();
        if (f.startsWith("=")) f = f.substring(1).trim();
        return analyser(f).eval(ctx);
    }

    private static void signaler(String formule, RuntimeException e) {
        String message = (e instanceof ErreurFormule) ? e.getMessage() : e.toString();
        derniereErreur = "[" + formule + "] " + message;
        long maintenant = System.currentTimeMillis();
        if (maintenant - dernierSignalement < 500) return;
        dernierSignalement = maintenant;
        String chemin = NoeudBase.cheminProjetCourant;
        if (chemin != null) DiagLogger.log(chemin, "FORMULE erreur " + derniereErreur);
    }

    private static Expr analyser(String formule) {
        Expr e = CACHE.get(formule);
        if (e != null) return e;
        try {
            e = new Analyseur(decouper(formule)).analyserTout();
        } catch (ErreurFormule ex) {
            final String message = ex.getMessage();
            e = c -> { throw new ErreurFormule(message); };
        }
        if (CACHE.size() > 500) CACHE.clear();
        CACHE.put(formule, e);
        return e;
    }

    // ------------------------------------------------------------------
    // RECHERCHE DES VARIABLES ET DES OBJETS (un seul endroit pour tout le moteur)
    // ------------------------------------------------------------------

    public static ObjetBase trouverObjet(String nom) {
        return trouverObjet(nom, 2);
    }

    // 3 niveaux de tolérance (voir memeNom) ; modeMax limite la recherche pour aller plus vite.
    private static ObjetBase trouverObjet(String nom, int modeMax) {
        if (nom == null || nom.isEmpty()) return null;
        for (int mode = 0; mode <= modeMax; mode++) {
            ObjetBase o = chercherObjet(NoeudBase.sceneActiveCourante, nom, mode);
            if (o == null) o = chercherObjet(NoeudBase.sceneHudActiveCourante, nom, mode);
            if (o != null) return o;
        }
        return null;
    }

    // mode 0 = nom exact, 1 = sans tenir compte des majuscules, 2 = sans les accents non plus
    private static boolean memeNom(String recherche, String candidat, int mode) {
        if (candidat == null) return false;
        if (mode == 0) return recherche.equals(candidat);
        if (mode == 1) return recherche.equalsIgnoreCase(candidat);
        return normaliser(recherche).equals(normaliser(candidat));
    }

    private static ObjetBase chercherObjet(Scene scene, String nom, int mode) {
        if (scene == null || scene.objets == null) return null;
        for (ObjetBase o : scene.objets) {
            if (memeNom(nom, o.nom, mode)) return o;
        }
        return null;
    }

    // Ordre de recherche (identique à celui des anciens nœuds) :
    // variables de l'objet du nœud, scène, HUD, globales, puis variables de n'importe quel objet.
    public static Variable trouverVariable(String nom, ObjetBase ctx) {
        return trouverVariable(nom, ctx, 2);
    }

    private static Variable trouverVariable(String nom, ObjetBase ctx, int modeMax) {
        if (nom == null || nom.isEmpty()) return null;
        Scene scene = NoeudBase.sceneActiveCourante;
        Scene hud = NoeudBase.sceneHudActiveCourante;
        List<Variable> globales = obtenirGlobales();

        for (int mode = 0; mode <= modeMax; mode++) {
            Variable v = null;
            if (ctx != null) v = chercherVariable(ctx.variablesLocales, nom, mode);
            if (v == null && scene != null) v = chercherVariable(scene.variablesLocales, nom, mode);
            if (v == null && hud != null) v = chercherVariable(hud.variablesLocales, nom, mode);
            if (v == null) v = chercherVariable(globales, nom, mode);
            if (v == null) v = chercherVariableDansObjets(scene, nom, mode);
            if (v == null) v = chercherVariableDansObjets(hud, nom, mode);
            if (v != null) return v;
        }
        return null;
    }

    private static Variable chercherVariable(List<Variable> liste, String nom, int mode) {
        if (liste == null) return null;
        for (Variable v : liste) {
            if (memeNom(nom, v.nom, mode)) return v;
        }
        return null;
    }

    private static Variable chercherVariableDansObjets(Scene scene, String nom, int mode) {
        if (scene == null || scene.objets == null) return null;
        for (ObjetBase o : scene.objets) {
            Variable v = chercherVariable(o.variablesLocales, nom, mode);
            if (v != null) return v;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Variable> obtenirGlobales() {
        try {
            List<Variable> l = NoeudBase.getVariablesGlobalesDisponibles();
            if (l != null && !l.isEmpty()) return l;
        } catch (RuntimeException e) {
            // on essaie les autres chemins
        }
        Object contexte = NoeudBase.contexteApplication;
        if (contexte == null) return null;
        if (contexte instanceof InterfaceEditeur) {
            List<Variable> l = ((InterfaceEditeur) contexte).variablesGlobales;
            if (l != null) return l;
        }
        try {
            java.lang.reflect.Field champ = contexte.getClass().getField("variablesGlobales");
            return (List<Variable>) champ.get(contexte);
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // VALEURS ET CONVERSIONS
    // ------------------------------------------------------------------

    // Valeur d'une variable selon son TYPE (une variable Chiffre qui contient le texte "6" donne bien 6).
    public static Object valeurDe(Variable v) {
        Object val = v.valeur;
        String t = v.type;
        if ("CHIFFRE".equals(t) || "ENTIER".equals(t)) {
            if (val == null) return 0.0;
            if (val instanceof Number) return ((Number) val).doubleValue();
            try {
                return Double.parseDouble(val.toString().trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        if ("BOOLEEN".equals(t)) return enBooleen(val);
        if ("LISTE_INVENTAIRE".equals(t)) return (val instanceof List) ? (double) ((List<?>) val).size() : 0.0;
        return val == null ? "" : val.toString();
    }

    // Convertit une valeur au type de la variable avant de l'y écrire (ne met jamais du texte dans un nombre).
    public static Object convertirPourVariable(Variable v, Object valeur) {
        String t = v.type;
        if ("CHIFFRE".equals(t)) return (float) enNombre(valeur);
        if ("ENTIER".equals(t)) return (int) Math.round(enNombre(valeur));
        if ("BOOLEEN".equals(t)) return enBooleen(valeur);
        if ("LISTE_INVENTAIRE".equals(t)) return v.valeur;
        return enTexte(valeur);
    }

    public static double enNombre(Object v) {
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof Boolean) return ((Boolean) v) ? 1.0 : 0.0;
        if (v instanceof String) {
            try {
                return Double.parseDouble(((String) v).trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                throw new ErreurFormule("« " + v + " » n'est pas un nombre");
            }
        }
        if (v instanceof ObjetBase) {
            throw new ErreurFormule("« " + ((ObjetBase) v).nom + " » est un objet : ajoute .x, .y, .largeur...");
        }
        throw new ErreurFormule("Valeur vide");
    }

    public static boolean enBooleen(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Number) return ((Number) v).doubleValue() != 0.0;
        if (v instanceof String) {
            String s = ((String) v).trim().toLowerCase(Locale.ROOT);
            return s.equals("true") || s.equals("vrai") || s.equals("oui");
        }
        return true;
    }

    public static String enTexte(Object v) {
        if (v == null) return "";
        if (v instanceof String) return (String) v;
        if (v instanceof Number) return formaterNombre(((Number) v).doubleValue());
        if (v instanceof ObjetBase) return ((ObjetBase) v).nom != null ? ((ObjetBase) v).nom : "";
        return v.toString();
    }

    // 5.0 -> "5" ; 2.50 -> "2.5" ; 0.1+0.2 -> "0.3"
    public static String formaterNombre(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) return String.valueOf((long) d);
        String s = String.format(Locale.US, "%.4f", d);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }

    // Minuscules, sans accents : "Opacité" -> "opacite"
    public static String normaliser(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
// bas 1


  // haut 2
    // ------------------------------------------------------------------
    // LECTURE DU TEXTE : découpage en morceaux (jetons)
    // ------------------------------------------------------------------

    private static class Jeton {
        static final int NOMBRE = 0, TEXTE = 1, NOM = 2, OP = 3;
        final int type;
        final String texte;
        final double nombre;
        final boolean crochets; // nom écrit entre [ ] : jamais un mot réservé

        Jeton(int type, String texte, double nombre, boolean crochets) {
            this.type = type;
            this.texte = texte;
            this.nombre = nombre;
            this.crochets = crochets;
        }
    }

    private static List<Jeton> decouper(String s) {
        List<Jeton> jetons = new ArrayList<>();
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }

            if (Character.isDigit(c) || (c == '.' && i + 1 < n && Character.isDigit(s.charAt(i + 1)))) {
                int debut = i;
                while (i < n && Character.isDigit(s.charAt(i))) i++;
                if (i < n && s.charAt(i) == '.') {
                    i++;
                    while (i < n && Character.isDigit(s.charAt(i))) i++;
                }
                jetons.add(new Jeton(Jeton.NOMBRE, s.substring(debut, i), Double.parseDouble(s.substring(debut, i)), false));
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                int debut = i;
                while (i < n && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_')) i++;
                jetons.add(new Jeton(Jeton.NOM, s.substring(debut, i), 0, false));
                continue;
            }
            if (c == '[') {
                int fin = s.indexOf(']', i + 1);
                if (fin < 0) throw new ErreurFormule("Il manque le crochet ] fermant");
                jetons.add(new Jeton(Jeton.NOM, s.substring(i + 1, fin).trim(), 0, true));
                i = fin + 1;
                continue;
            }
            if (c == '"' || c == '\'') {
                int fin = s.indexOf(c, i + 1);
                if (fin < 0) throw new ErreurFormule("Il manque le guillemet fermant");
                jetons.add(new Jeton(Jeton.TEXTE, s.substring(i + 1, fin), 0, false));
                i = fin + 1;
                continue;
            }
            if (i + 1 < n) {
                String deux = s.substring(i, i + 2);
                if (deux.equals("&&") || deux.equals("||") || deux.equals("==")
                        || deux.equals("!=") || deux.equals("<=") || deux.equals(">=")) {
                    jetons.add(new Jeton(Jeton.OP, deux, 0, false));
                    i += 2;
                    continue;
                }
            }
            if (c == '≠') { jetons.add(new Jeton(Jeton.OP, "!=", 0, false)); i++; continue; }
            if (c == '≥') { jetons.add(new Jeton(Jeton.OP, ">=", 0, false)); i++; continue; }
            if (c == '≤') { jetons.add(new Jeton(Jeton.OP, "<=", 0, false)); i++; continue; }
            if ("+-*/%(),.!<>=".indexOf(c) >= 0) {
                jetons.add(new Jeton(Jeton.OP, String.valueOf(c), 0, false));
                i++;
                continue;
            }
            throw new ErreurFormule("Caractère inattendu : " + c);
        }
        return jetons;
    }

    // ------------------------------------------------------------------
    // LECTURE DU TEXTE : construction de la formule (priorités : || puis && puis == puis < puis + puis * puis - unaire)
    // ------------------------------------------------------------------

    private static final List<String> FONCTIONS = java.util.Arrays.asList(
            "random", "abs", "sqrt", "sin", "cos", "round", "floor", "ceil", "int", "entier",
            "min", "max", "clamp", "distance", "angle");

    private static class Analyseur {
        private final List<Jeton> j;
        private int p = 0;

        Analyseur(List<Jeton> jetons) {
            this.j = jetons;
        }

        Expr analyserTout() {
            if (j.isEmpty()) throw new ErreurFormule("Formule vide");
            Expr e = ou();
            if (p < j.size()) throw new ErreurFormule("Inattendu : " + j.get(p).texte);
            return e;
        }

        private boolean estOp(String op) {
            return p < j.size() && j.get(p).type == Jeton.OP && j.get(p).texte.equals(op);
        }

        private boolean prendreOp(String op) {
            if (estOp(op)) { p++; return true; }
            return false;
        }

        private Expr ou() {
            Expr g = et();
            while (prendreOp("||")) {
                final Expr a = g;
                final Expr b = et();
                g = c -> enBooleen(a.eval(c)) || enBooleen(b.eval(c));
            }
            return g;
        }

        private Expr et() {
            Expr g = egalite();
            while (prendreOp("&&")) {
                final Expr a = g;
                final Expr b = egalite();
                g = c -> enBooleen(a.eval(c)) && enBooleen(b.eval(c));
            }
            return g;
        }

        private Expr egalite() {
            Expr g = comparaison();
            while (true) {
                final boolean egal;
                if (prendreOp("==") || prendreOp("=")) egal = true;
                else if (prendreOp("!=")) egal = false;
                else return g;
                final Expr a = g;
                final Expr b = comparaison();
                g = c -> egal == sontEgaux(a.eval(c), b.eval(c));
            }
        }

        private Expr comparaison() {
            Expr g = somme();
            while (true) {
                final String op;
                if (prendreOp("<=")) op = "<=";
                else if (prendreOp(">=")) op = ">=";
                else if (prendreOp("<")) op = "<";
                else if (prendreOp(">")) op = ">";
                else return g;
                final Expr a = g;
                final Expr b = somme();
                g = c -> {
                    int r = comparer(a.eval(c), b.eval(c));
                    switch (op) {
                        case "<=": return r <= 0;
                        case ">=": return r >= 0;
                        case "<": return r < 0;
                        default: return r > 0;
                    }
                };
            }
        }

        private Expr somme() {
            Expr g = produit();
            while (true) {
                final boolean plus;
                if (prendreOp("+")) plus = true;
                else if (prendreOp("-")) plus = false;
                else return g;
                final Expr a = g;
                final Expr b = produit();
                if (plus) {
                    g = c -> additionner(a.eval(c), b.eval(c));
                } else {
                    g = c -> enNombre(a.eval(c)) - enNombre(b.eval(c));
                }
            }
        }

        private Expr produit() {
            Expr g = unaire();
            while (true) {
                final String op;
                if (prendreOp("*")) op = "*";
                else if (prendreOp("/")) op = "/";
                else if (prendreOp("%")) op = "%";
                else return g;
                final Expr a = g;
                final Expr b = unaire();
                g = c -> {
                    double x = enNombre(a.eval(c));
                    double y = enNombre(b.eval(c));
                    switch (op) {
                        case "*": return x * y;
                        case "/":
                            if (y == 0) throw new ErreurFormule("Division par zéro");
                            return x / y;
                        default:
                            if (y == 0) throw new ErreurFormule("Modulo par zéro");
                            return x % y;
                    }
                };
            }
        }

        private Expr unaire() {
            if (prendreOp("-")) {
                final Expr a = unaire();
                return c -> -enNombre(a.eval(c));
            }
            if (prendreOp("+")) {
                final Expr a = unaire();
                return c -> enNombre(a.eval(c));
            }
            if (prendreOp("!")) {
                final Expr a = unaire();
                return c -> !enBooleen(a.eval(c));
            }
            return primaire();
        }

        private Expr primaire() {
            if (p >= j.size()) throw new ErreurFormule("La formule se termine trop tôt");
            Jeton t = j.get(p++);

            if (t.type == Jeton.NOMBRE) {
                final Double valeur = t.nombre;
                return c -> valeur;
            }
            if (t.type == Jeton.TEXTE) {
                final String valeur = t.texte;
                return c -> valeur;
            }
            if (t.type == Jeton.OP && t.texte.equals("(")) {
                Expr e = ou();
                if (!prendreOp(")")) throw new ErreurFormule("Il manque la parenthèse )");
                return e;
            }
            if (t.type == Jeton.NOM) {
                // fonction : nom(...)
                if (!t.crochets && estOp("(")) {
                    final String f = normaliser(t.texte);
                    if (!FONCTIONS.contains(f)) throw new ErreurFormule("Fonction inconnue : " + t.texte);
                    p++; // la parenthèse ouvrante
                    final List<Expr> args = new ArrayList<>();
                    if (!prendreOp(")")) {
                        args.add(ou());
                        while (prendreOp(",")) args.add(ou());
                        if (!prendreOp(")")) throw new ErreurFormule("Il manque la parenthèse ) de " + t.texte);
                    }
                    return c -> {
                        Object[] valeurs = new Object[args.size()];
                        for (int i = 0; i < valeurs.length; i++) valeurs[i] = args.get(i).eval(c);
                        return appelerFonction(f, valeurs);
                    };
                }
                // propriété : objet.propriete
                if (estOp(".") && p + 1 < j.size() && j.get(p + 1).type == Jeton.NOM) {
                    p++; // le point
                    final String nomObjet = t.texte;
                    final String propriete = j.get(p++).texte;
                    return c -> lireProprieteObjet(nomObjet, propriete);
                }
                // nom seul : variable, objet, vrai/faux...
                final String nom = t.texte;
                final boolean crochets = t.crochets;
                return c -> resoudreNom(nom, crochets, c);
            }
            throw new ErreurFormule("Inattendu : " + t.texte);
        }
    }

// bas 2

// haut 3
    // ------------------------------------------------------------------
    // CALCULS
    // ------------------------------------------------------------------

    private static Object resoudreNom(String nom, boolean crochets, ObjetBase ctx) {
        String bas = normaliser(nom);
        if (!crochets) {
            if (bas.equals("true") || bas.equals("vrai")) return Boolean.TRUE;
            if (bas.equals("false") || bas.equals("faux")) return Boolean.FALSE;
        }
        // recherches rapides d'abord (nom exact, majuscules ignorées), la recherche sans accents en dernier
        Variable v = trouverVariable(nom, ctx, 1);
        if (v != null) return valeurDe(v);
        if (bas.equals("implique")) return objetImplique();
        ObjetBase o = trouverObjet(nom, 1);
        if (o != null) return o;
        if (!crochets) {
            if (bas.equals("pi")) return Math.PI;
            // valeurs du jeu : elles passent après les variables et les objets (une variable "temps" reste prioritaire)
            if (bas.equals("temps")) return HorlogeJeu.tempsJeu;
            if (bas.equals("vitesse")) return (double) HorlogeJeu.vitesse;
        }
        v = trouverVariable(nom, ctx, 2);
        if (v != null) return valeurDe(v);
        o = trouverObjet(nom, 2);
        if (o != null) return o;
        throw new ErreurFormule("Inconnu : « " + nom + " » (ni variable, ni objet)");
    }

    private static ObjetBase objetImplique() {
        ObjetBase o = MoteurLogique.dernierObjetImplique;
        if (o == null) throw new ErreurFormule("Aucun objet impliqué pour le moment");
        return o;
    }

    // ecran.largeur et ecran.hauteur : la taille de l'écran de jeu (réglée dans le menu Réglages)
    private static Object lireProprieteEcran(String propriete) {
        String p = normaliser(propriete);
        if (p.equals("largeur")) return (double) ConfigurationJeu.LARGEUR_JEU;
        if (p.equals("hauteur")) return (double) ConfigurationJeu.HAUTEUR_JEU;
        throw new ErreurFormule("« ecran » n'a pas de « " + propriete + " » : utilise ecran.largeur ou ecran.hauteur");
    }

    // doigt.x, doigt.y et doigt.appuye : le premier doigt posé sur l'écran de jeu (0,0 = coin en haut à gauche)
    private static Object lireProprieteDoigt(String propriete) {
        String p = normaliser(propriete);
        if (p.equals("x")) return (double) GestionnaireControles.doigtX;
        if (p.equals("y")) return (double) GestionnaireControles.doigtY;
        if (p.equals("appuye")) return GestionnaireControles.doigtAppuye ? Boolean.TRUE : Boolean.FALSE;
        throw new ErreurFormule("« doigt » n'a pas de « " + propriete + " » : utilise doigt.x, doigt.y ou doigt.appuye");
    }

    // joystick.x, joystick.y, joystick.force, joystick.angle et joystick.actif
    // x et y vont de -1 à 1 (0 = au repos, le bas de l'écran est positif), force de 0 à 1,
    // angle de 0 à 360 degrés (0 = droite, 90 = bas), et vaut 0 au repos : utilise joystick.actif pour savoir s'il est touché
    private static Object lireProprieteJoystick(String propriete) {
        String p = normaliser(propriete);
        double jx = GestionnaireControles.joyDirX;
        double jy = GestionnaireControles.joyDirY;
        if (p.equals("x")) return jx;
        if (p.equals("y")) return jy;
        if (p.equals("force")) return Math.min(1.0, Math.sqrt(jx * jx + jy * jy));
        if (p.equals("actif")) return (jx != 0 || jy != 0) ? Boolean.TRUE : Boolean.FALSE;
        if (p.equals("angle")) {
            if (jx == 0 && jy == 0) return 0.0;
            double deg = Math.toDegrees(Math.atan2(jy, jx));
            return deg < 0 ? deg + 360.0 : deg;
        }
        throw new ErreurFormule("« joystick » n'a pas de « " + propriete + " » : utilise joystick.x, joystick.y, joystick.force, joystick.angle ou joystick.actif");
    }

    private static Object lireProprieteObjet(String nomObjet, String propriete) {
        if (normaliser(nomObjet).equals("ecran") && trouverObjet(nomObjet, 1) == null) {
            return lireProprieteEcran(propriete);
        }
        if (normaliser(nomObjet).equals("doigt") && trouverObjet(nomObjet, 1) == null) {
            return lireProprieteDoigt(propriete);
        }
        if (normaliser(nomObjet).equals("joystick") && trouverObjet(nomObjet, 1) == null) {
            return lireProprieteJoystick(propriete);
        }
        ObjetBase o = normaliser(nomObjet).equals("implique") ? objetImplique() : trouverObjet(nomObjet);
        if (o == null) throw new ErreurFormule("Objet introuvable : « " + nomObjet + " »");
        Object valeur = ProprietesObjet.lire(o, propriete);
        if (valeur != null) return valeur;
        for (int mode = 0; mode < 3; mode++) {
            Variable v = chercherVariable(o.variablesLocales, propriete, mode);
            if (v != null) return valeurDe(v);
        }
        throw new ErreurFormule("« " + o.nom + " » n'a ni propriété ni variable « " + propriete + " »");
    }

    private static Object additionner(Object a, Object b) {
        if (a instanceof String || b instanceof String) return enTexte(a) + enTexte(b);
        return enNombre(a) + enNombre(b);
    }

    private static boolean sontEgaux(Object a, Object b) {
        if (a instanceof ObjetBase || b instanceof ObjetBase) return a == b;
        if (a instanceof Boolean && b instanceof Boolean) return a.equals(b);
        if (a instanceof String || b instanceof String) return enTexte(a).equals(enTexte(b));
        return Math.abs(enNombre(a) - enNombre(b)) < 1e-9;
    }

    private static int comparer(Object a, Object b) {
        if (a instanceof String && b instanceof String) return ((String) a).compareTo((String) b);
        double x = enNombre(a);
        double y = enNombre(b);
        return x < y ? -1 : (x > y ? 1 : 0);
    }

    private static void verifierNombreArguments(String nom, Object[] a, int min, int max) {
        if (a.length < min || a.length > max) {
            String attendu = (min == max) ? String.valueOf(min) : (max == Integer.MAX_VALUE ? min + " ou plus" : min + " à " + max);
            throw new ErreurFormule(nom + "() attend " + attendu + " valeur(s)");
        }
    }

    // Centre d'un objet (approximation : sans tenir compte de l'échelle)
    private static double centreX(ObjetBase o) {
        return o.x + o.largeur / 2.0;
    }

    private static double centreY(ObjetBase o) {
        return o.y + o.hauteur / 2.0;
    }

    private static double[] deuxPoints(String nom, Object[] a) {
        if (a.length == 2 && a[0] instanceof ObjetBase && a[1] instanceof ObjetBase) {
            ObjetBase o1 = (ObjetBase) a[0];
            ObjetBase o2 = (ObjetBase) a[1];
            return new double[]{centreX(o1), centreY(o1), centreX(o2), centreY(o2)};
        }
        if (a.length == 4) {
            return new double[]{enNombre(a[0]), enNombre(a[1]), enNombre(a[2]), enNombre(a[3])};
        }
        throw new ErreurFormule(nom + "() attend deux objets : " + nom + "(player, ennemi) ou quatre nombres (x1,y1,x2,y2)");
    }

    private static Object appelerFonction(String nom, Object[] a) {
        switch (nom) {
            case "random": {
                if (a.length == 0) return RNG.nextDouble();
                verifierNombreArguments(nom, a, 2, 2);
                double bas = enNombre(a[0]);
                double haut = enNombre(a[1]);
                if (bas > haut) { double tmp = bas; bas = haut; haut = tmp; }
                if (bas == Math.rint(bas) && haut == Math.rint(haut)) {
                    return bas + RNG.nextInt((int) (haut - bas) + 1);
                }
                return bas + RNG.nextDouble() * (haut - bas);
            }
            case "abs":
                verifierNombreArguments(nom, a, 1, 1);
                return Math.abs(enNombre(a[0]));
            case "sqrt": {
                verifierNombreArguments(nom, a, 1, 1);
                double x = enNombre(a[0]);
                if (x < 0) throw new ErreurFormule("sqrt() d'un nombre négatif");
                return Math.sqrt(x);
            }
            case "sin":
                verifierNombreArguments(nom, a, 1, 1);
                return Math.sin(Math.toRadians(enNombre(a[0])));
            case "cos":
                verifierNombreArguments(nom, a, 1, 1);
                return Math.cos(Math.toRadians(enNombre(a[0])));
            case "round":
                verifierNombreArguments(nom, a, 1, 1);
                return (double) Math.round(enNombre(a[0]));
            case "floor":
                verifierNombreArguments(nom, a, 1, 1);
                return Math.floor(enNombre(a[0]));
            case "ceil":
                verifierNombreArguments(nom, a, 1, 1);
                return Math.ceil(enNombre(a[0]));
            case "int":
            case "entier": {
                verifierNombreArguments(nom, a, 1, 1);
                double x = enNombre(a[0]);
                return x < 0 ? Math.ceil(x) : Math.floor(x);
            }
            case "min":
            case "max": {
                verifierNombreArguments(nom, a, 1, Integer.MAX_VALUE);
                double r = enNombre(a[0]);
                for (int i = 1; i < a.length; i++) {
                    double x = enNombre(a[i]);
                    r = nom.equals("min") ? Math.min(r, x) : Math.max(r, x);
                }
                return r;
            }
            case "clamp": {
                verifierNombreArguments(nom, a, 3, 3);
                double v = enNombre(a[0]);
                double bas = enNombre(a[1]);
                double haut = enNombre(a[2]);
                return Math.max(bas, Math.min(haut, v));
            }
            case "distance": {
                double[] p = deuxPoints(nom, a);
                return Math.sqrt((p[2] - p[0]) * (p[2] - p[0]) + (p[3] - p[1]) * (p[3] - p[1]));
            }
            case "angle": {
                double[] p = deuxPoints(nom, a);
                double deg = Math.toDegrees(Math.atan2(p[3] - p[1], p[2] - p[0]));
                return deg < 0 ? deg + 360.0 : deg;
            }
            default:
                throw new ErreurFormule("Fonction inconnue : " + nom);
        }
    }
}
// bas 3
