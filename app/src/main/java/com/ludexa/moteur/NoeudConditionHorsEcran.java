// haut 1
package com.ludexa.moteur;

public class NoeudConditionHorsEcran extends NoeudBase {

    public NoeudConditionHorsEcran() {
        super(genererId(), Traducteur.get("noeud_si_hors_ecran"), Traducteur.get("cat_logique_conditions"));
        
        // Utilisation stricte du Traducteur pour les ports (Règle N°2)
        this.ajouterPort(new Port(Traducteur.get("port_entrer"), Port.TYPE_EXECUTION_ENTREE));
        this.ajouterPort(new Port(Traducteur.get("port_vrai"), Port.TYPE_EXECUTION_SORTIE));
        this.ajouterPort(new Port(Traducteur.get("port_faux"), Port.TYPE_EXECUTION_SORTIE));
        
        // Paramètre optionnel pour la distance de sortie de l'écran avant destruction
        this.ajouterParametre(Traducteur.get("param_marge"), "100", TYPE_NOMBRE);
    }

    @Override
    public void executer() {
        // AUCUN SHADOWING : on utilise la méthode native héritée de NoeudBase
        ObjetBase obj = getCibleObjet();
        
        if (obj != null) {
            float marge = 100f; // Valeur par défaut
            
            try {
                String margeStr = getValeurParametre(Traducteur.get("param_marge"));
                if (margeStr != null && !margeStr.isEmpty()) {
                    marge = Float.parseFloat(margeStr);
                }
            } catch (NumberFormatException e) {
                marge = 100f; // Repli en cas d'erreur de saisie
            }

            // Vérification des limites avec ConfigurationJeu
            boolean horsEcranX = (obj.x + obj.largeur < -marge) || (obj.x > ConfigurationJeu.LARGEUR_JEU + marge);
            boolean horsEcranY = (obj.y + obj.hauteur < -marge) || (obj.y > ConfigurationJeu.HAUTEUR_JEU + marge);

            if (horsEcranX || horsEcranY) {
                propagerExecution(Traducteur.get("port_vrai"));
            } else {
                propagerExecution(Traducteur.get("port_faux"));
            }
        } else {
            // Sécurité : si l'objet est introuvable ou déjà détruit, la condition est fausse
            propagerExecution(Traducteur.get("port_faux"));
        }
    }

    @Override
    public boolean requiertCibleObjet() { 
        return true; 
    }
    
    // RÈGLE ABSOLUE N°1 : ON NE REDÉFINIT NI getCibleObjet() NI setCibleObjet()
}
// bas 1
