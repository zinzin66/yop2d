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
    private long derniereAlerte = 0;
    private transient Scene cibleSceneResolue;   // la scène choisie dans le nœud (Changer de scène, Ouvrir un HUD...)
    private String nomCibleScene;

    public NoeudGenerique(String cle) {
        super(genererId(), "?", "Action");
        this.cle = cle;
        this.definition = CatalogueNoeuds.trouver(cle);
        if (definition == null) throw new IllegalArgumentException("Nœud inconnu dans le catalogue : " + cle);
        // NOUVEAU : cleNom est une CLÉ de traduction (ex : "noeud_creer_objet"), jamais le texte affiché.
        // Sans Traducteur.get(), le nœud affichait cette clé brute sur le canvas, dans toutes les langues.
        this.nom = Traducteur.get(definition.cleNom);
        ajouterPort(new Port("port_entrer", Port.TYPE_EXECUTION_ENTREE));
        for (String sortie : definition.sorties) ajouterPort(new Port(sortie, Port.TYPE_EXECUTION_SORTIE));
        for (CatalogueNoeuds.Champ c : definition.champs) valeurs.put(c.nomParam, c.defaut);
    }

    // ------------------------------------------------------------------
    // EXÉCUTION
    // ------------------------------------------------------------------

    @Override
    public void executer() {
        Evaluateur.derniereErreur = null;
        verifierCibles();
        String sortie = null;
        try {
            sortie = ActionsMoteur.executer(definition.action, this);
        } catch (RuntimeException e) {
            signaler(e);
        }
        if (Evaluateur.derniereErreur != null) alerte(Evaluateur.derniereErreur);
        if (sortie == null && !definition.sorties.isEmpty()) sortie = definition.sorties.get(0);
        if (sortie != null) propagerExecution(sortie);   // un nœud sans sortie termine le script
    }

    private void signaler(RuntimeException e) {
        alerte(e.getMessage() != null ? e.getMessage() : e.toString());
        long maintenant = System.currentTimeMillis();
        if (maintenant - dernierSignalement < 500) return;
        dernierSignalement = maintenant;
        String chemin = cheminJournal();
        if (chemin != null) DiagLogger.log(chemin, "NOEUD erreur cle=" + cle + " : " + e);
    }

    // ------------------------------------------------------------------
    // DIAGNOSTIC : le journal de la fenêtre de debug reçoit une ligne ALERTE quand quelque chose empêche le nœud
    // de travailler (objet ou variable introuvable, formule fausse...).
    // ------------------------------------------------------------------

    private boolean dansEditeur() {
        return contexteApplication instanceof InterfaceEditeur;
    }

    // Dossier du projet où écrire le journal (celui du moteur, sinon celui de l'éditeur).
    private static String cheminJournal() {
        if (cheminProjetCourant != null) return cheminProjetCourant;
        if (contexteApplication instanceof InterfaceEditeur) return ((InterfaceEditeur) contexteApplication).cheminProjet;
        return null;
    }

    // ALERTE : quelque chose empêche le nœud de faire son travail (au plus une ligne toutes les 2,5 secondes par nœud)
    private void alerte(String message) {
        long maintenant = System.currentTimeMillis();
        if (maintenant - derniereAlerte < 2500) return;
        derniereAlerte = maintenant;
        String chemin = cheminJournal();
        if (chemin != null) DiagLogger.log(chemin, "ALERTE " + cle + " : " + message);
    }

    private static boolean cibleChoisie(String nom) {
        return nom != null && !nom.isEmpty();
    }

    // Signale une cible non choisie ou introuvable dans la scène qui tourne (test dans l'éditeur seulement).
    private void verifierCibles() {
        if (!dansEditeur()) return;
        if (definition.objet) {
            if (!cibleChoisie(nomCibleObjet)) {
                if (!definition.variable) alerte("aucun objet choisi (bouton jaune de cible A)");
            } else if (getCibleObjet() == null) {
                alerte("objet introuvable : « " + nomCibleObjet + " »");
            }
        }
        if (definition.objetB) {
            if (!cibleChoisie(nomCibleObjetB)) alerte("aucun objet choisi (bouton jaune de cible B)");
            else if (getCibleObjetB() == null) alerte("objet introuvable : « " + nomCibleObjetB + " »");
        }
        if (definition.variable) {
            if (!cibleChoisie(nomCibleVariable)) alerte("aucune variable choisie");
            else if (getCibleVariable() == null) alerte("variable introuvable : « " + nomCibleVariable + " »");
        }
    }

    // ------------------------------------------------------------------
    // LECTURE DES CHAMPS (utilisée par ActionsMoteur ; "champ" = la clé interne, ex : "x")
    // ------------------------------------------------------------------

    public String texteBrut(String champ) {
        String constante = definition.constantes.get(champ);
        if (constante != null) return constante;
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
        switch (c.type) {
            case "texte": return TYPE_TEXTE_ALPHABETIQUE;
            case "choix": return TYPE_CHOIX_LISTE;
            case "couleur": return TYPE_COULEUR;
            case "image": return TYPE_CHOIX_IMAGE;
            case "son": return TYPE_CHOIX_SON;
            case "dialogue": return TYPE_CHOIX_DIALOGUE;
            case "fonction": return TYPE_CHOIX_FONCTION;
            case "tag": return "TYPE_CHOIX_TAG";
            case "animation": return "CHOIX_ANIMATION";
            default: return TYPE_TEXTE_LIBRE;   // "formule"
        }
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

    @Override
    public boolean requiertCibleScene() {
        return definition.scene;
    }

    @Override
    public void setCibleScene(Scene scene) {
        this.cibleSceneResolue = scene;
        this.nomCibleScene = (scene != null) ? scene.nom : null;
    }

    // La scène choisie ; si elle n'est pas encore résolue, on la retrouve par son nom dans les scènes du projet.
    @Override
    public Scene getCibleScene() {
        if (cibleSceneResolue == null && nomCibleScene != null) cibleSceneResolue = ActionsScene.trouverScene(nomCibleScene);
        return cibleSceneResolue;
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
