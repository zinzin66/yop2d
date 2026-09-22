// haut 1
package com.ludexa.moteur;

import android.content.Context;

// Calcule le résultat d'une formule dans l'éditeur (sans lancer le jeu), pour l'afficher sous le champ.
// Les valeurs sont celles de la scène en cours d'édition (position de départ des objets, valeur initiale des variables).
public class ApercuFormule {

    public static class Resultat {
        public final boolean ok;      // false = la formule contient une erreur
        public final String texte;    // le résultat, ou le message d'erreur ("" si la formule est vide)

        public Resultat(boolean ok, String texte) {
            this.ok = ok;
            this.texte = texte;
        }
    }

    public static Resultat calculer(String formule, ObjetBase objetDuNoeud, Scene scene, Context contexte) {
        if (formule == null) return new Resultat(true, "");
        String f = formule.trim();
        if (f.isEmpty() || f.equals("=")) return new Resultat(true, "");

        // On simule le jeu le temps du calcul : la scène éditée devient la scène courante.
        Scene avantScene = NoeudBase.sceneActiveCourante;
        Scene avantHud = NoeudBase.sceneHudActiveCourante;
        Context avantContexte = NoeudBase.contexteApplication;
        NoeudBase.sceneActiveCourante = scene;
        NoeudBase.sceneHudActiveCourante = null;
        // Les variables globales sont lues depuis la fenêtre de l'éditeur qui a ouvert ce nœud.
        if (contexte instanceof FournisseurDonneesJeu || contexte instanceof InterfaceEditeur) NoeudBase.contexteApplication = contexte;
        try {
            return new Resultat(true, decrire(Evaluateur.evaluer(f, objetDuNoeud)));
        } catch (Evaluateur.ErreurFormule e) {
            return new Resultat(false, e.getMessage());
        } catch (RuntimeException e) {
            return new Resultat(false, e.toString());
        } finally {
            NoeudBase.sceneActiveCourante = avantScene;
            NoeudBase.sceneHudActiveCourante = avantHud;
            NoeudBase.contexteApplication = avantContexte;
        }
    }

    private static String decrire(Object valeur) {
        if (valeur instanceof ObjetBase) return "objet « " + ((ObjetBase) valeur).nom + " »";
        if (valeur instanceof String) return "\"" + valeur + "\"";
        if (valeur instanceof Boolean) return ((Boolean) valeur) ? "true" : "false";
        if (valeur instanceof Number) return Evaluateur.formaterNombre(((Number) valeur).doubleValue());
        return String.valueOf(valeur);
    }
}
// bas 1
