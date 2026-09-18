// haut 1
package com.ludexa.moteur;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Nœud unique qui prend toutes les formes décrites dans le catalogue (assets/catalogue_noeuds.json).
// Il ne contient aucune logique propre : il garde les valeurs saisies, et l'action est faite par ActionsMoteur.
//
// Les "cibles" (objet A, objet B, variable) réutilisent le mécanisme de NoeudBase (nomCibleObjet, nomCibleObjetB,
// nomCibleVariable) : la fenêtre d'édition, la sauvegarde et les prefabs les gèrent donc déjà.
public class NoeudGenerique extends NoeudBase {

    public final String cle;
    private final CatalogueNoeuds.Definition definition;
    private final Map<String, String> valeurs = new LinkedHashMap<>();
    private static long dernierSignalement = 0;

    public NoeudGenerique(String cle) {
        super(genererId(), "?", "Action");
        this.cle = cle;
        this.definition = CatalogueNoeuds.trouver(cle);
        if (definition == null) throw new IllegalArgumentException("Nœud inconnu dans le catalogue : " + cle);
        this.nom = definition.cleNom;
        ajouterPort(new Port("port_entrer", Port.TYPE_EXECUTION_ENTREE));
        for (String sortie : definition.sorties) ajouterPort(new Port(sortie, Port.TYPE_EXECUTION_SORTIE));
        for (CatalogueNoeuds.Champ c : definition.champs) valeurs.put(c.nomParam, c.defaut);
    }

    // ------------------------------------------------------------------
    // EXÉCUTION
    // ------------------------------------------------------------------

    @Override
    public void executer() {
        String sortie = null;
        try {
            sortie = ActionsMoteur.executer(definition.action, this);
        } catch (RuntimeException e) {
            signaler(e);
        }
        if (sortie == null) sortie = definition.sorties.get(0);
        propagerExecution(sortie);
    }

    private void signaler(RuntimeException e) {
        long maintenant = System.currentTimeMillis();
        if (maintenant - dernierSignalement < 500) return;
        dernierSignalement = maintenant;
        if (cheminProjetCourant != null) DiagLogger.log(cheminProjetCourant, "NOEUD erreur cle=" + cle + " : " + e);
    }

    // ------------------------------------------------------------------
    // LECTURE DES CHAMPS (utilisée par ActionsMoteur ; "champ" = la clé interne, ex : "x")
    // ------------------------------------------------------------------

    public String texteBrut(String champ) {
        String v = valeurs.get(definition.cle + "." + champ);
        return v != null ? v : "";
    }

    public double nombre(String champ) {
        return Evaluateur.nombre(texteBrut(champ), getCibleObjet());
    }

    public boolean booleen(String champ) {
        return Evaluateur.booleen(texteBrut(champ), getCibleObjet());
    }

    public String texte(String champ) {
        return Evaluateur.texteOuFormule(texteBrut(champ), getCibleObjet());
    }

    // ------------------------------------------------------------------
    // CHAMPS MODIFIABLES : ce que la fenêtre d'édition et la sauvegarde utilisent
    // ------------------------------------------------------------------

    private CatalogueNoeuds.Champ champ(String nomParam) {
        for (CatalogueNoeuds.Champ c : definition.champs) {
            if (c.nomParam.equals(nomParam)) return c;
        }
        return null;
    }

    @Override
    public List<String> getNomsParametres() {
        return new ArrayList<>(valeurs.keySet());
    }

    @Override
    public String getValeurParametre(String nom) {
        String v = valeurs.get(nom);
        return v != null ? v : "";
    }

    @Override
    public void setValeurParametre(String nom, String valeur) {
        if (valeurs.containsKey(nom)) valeurs.put(nom, valeur == null ? "" : valeur);
    }

    @Override
    public String getTypeEditeurParametre(String nom) {
        CatalogueNoeuds.Champ c = champ(nom);
        if (c == null) return TYPE_TEXTE_LIBRE;
        if ("choix".equals(c.type)) return TYPE_CHOIX_LISTE;
        if ("texte".equals(c.type)) return TYPE_TEXTE_ALPHABETIQUE;
        return TYPE_TEXTE_LIBRE;
    }

    @Override
    public List<String> getOptionsChoixListe(String nom) {
        CatalogueNoeuds.Champ c = champ(nom);
        if (c == null) return new ArrayList<>();
        if (c.options.contains("@proprietes")) return new ArrayList<>(ProprietesObjet.NOMS_MODIFIABLES);
        return new ArrayList<>(c.options);
    }

    @Override
    public boolean utiliseClavierTexte() {
        return true;
    }

    // ------------------------------------------------------------------
    // CIBLES (objet A, objet B, variable)
    // ------------------------------------------------------------------

    @Override
    public boolean requiertCibleObjet() {
        return definition.objet;
    }

    @Override
    public boolean requiertCibleObjetB() {
        return definition.objetB;
    }

    @Override
    public boolean requiertCibleVariable() {
        return definition.variable;
    }

    @Override
    public void setCibleObjet(ObjetBase objet) {
        this.nomCibleObjet = (objet != null) ? objet.nom : null;
    }

    @Override
    public void setCibleObjetB(ObjetBase objet) {
        this.nomCibleObjetB = (objet != null) ? objet.nom : null;
    }

    // Jeu en cours : recherche dans la scène et le HUD ; éditeur : ancien mécanisme de NoeudBase.
    @Override
    public ObjetBase getCibleObjet() {
        if (cibleObjetResolue != null) return cibleObjetResolue;
        if ("__OBJET_IMPLIQUE__".equals(nomCibleObjet)) return MoteurLogique.dernierObjetImplique;
        if (nomCibleObjet == null || nomCibleObjet.isEmpty()) return null;
        ObjetBase o = Evaluateur.trouverObjet(nomCibleObjet);
        return (o != null) ? o : super.getCibleObjet();
    }

    @Override
    public ObjetBase getCibleObjetB() {
        if (cibleObjetBResolue != null) return cibleObjetBResolue;
        if ("__OBJET_IMPLIQUE__".equals(nomCibleObjetB)) return MoteurLogique.dernierObjetImplique;
        if (nomCibleObjetB == null || nomCibleObjetB.isEmpty()) return null;
        ObjetBase o = Evaluateur.trouverObjet(nomCibleObjetB);
        return (o != null) ? o : super.getCibleObjetB();
    }

    // Toujours retrouvée par son nom (pas de mémoire) : les clones d'un prefab ne se mélangent pas.
    @Override
    public Variable getCibleVariable() {
        if (nomCibleVariable == null || nomCibleVariable.isEmpty()) return null;
        Variable v = Evaluateur.trouverVariable(nomCibleVariable, getCibleObjet());
        if (v != null) return v;
        // Éditeur (aucune scène en cours de jeu) : ancien mécanisme de NoeudBase, pour l'affichage
        return (NoeudBase.sceneActiveCourante == null) ? super.getCibleVariable() : null;
    }
}
// bas 1
