// haut 1
package com.ludexa.moteur;

// Point unique de création d'un nœud : utilisé par le canevas (dépôt, duplication) et par Blueprint (rechargement).
public class FabriqueNoeuds {

    // type = "cle:deplacer" pour un nœud du catalogue, ou "NoeudActionToast" pour un nœud écrit en Java.
    public static NoeudBase creer(String type) throws Exception {
        if (type.startsWith("cle:")) {
            return new NoeudGenerique(type.substring(4));
        }
        Class<?> clazz = Class.forName("com.ludexa.moteur." + type);
        return (NoeudBase) clazz.newInstance();
    }

    // Recrée un nœud à partir de sa sauvegarde : cleNoeud est renseignée pour un nœud du catalogue.
    public static NoeudBase creerDepuisSauvegarde(String classeType, String cleNoeud) throws Exception {
        if (cleNoeud != null && !cleNoeud.isEmpty()) {
            return new NoeudGenerique(cleNoeud);
        }
        Class<?> clazz = Class.forName(classeType);
        return (NoeudBase) clazz.newInstance();
    }

    // Nœud neuf du même genre que le modèle (pour la duplication) : mêmes clé/classe, sans ses valeurs.
    public static NoeudBase copierVide(NoeudBase modele) throws Exception {
        if (modele instanceof NoeudGenerique) {
            return new NoeudGenerique(((NoeudGenerique) modele).cle);
        }
        return (NoeudBase) modele.getClass().newInstance();
    }
}
// bas 1
