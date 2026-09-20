// haut 1
package com.ludexa.moteur;

// Actions utilitaires : écrire dans le journal de la fenêtre de debug...
public class ActionsOutils {

    // Écrit le message (texte, ou formule commençant par =) dans le journal, sur une ligne "JOURNAL ...".
    static void journaliser(NoeudGenerique n) {
        String chemin = NoeudBase.cheminProjetCourant;
        if (chemin == null && NoeudBase.contexteApplication instanceof InterfaceEditeur) {
            chemin = ((InterfaceEditeur) NoeudBase.contexteApplication).cheminProjet;
        }
        if (chemin == null) return;
        DiagLogger.log(chemin, "JOURNAL " + n.texte("message"));
    }
}
// bas 1
