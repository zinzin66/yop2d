// haut 1
package com.ludexa.moteur;

public class NoeudConditionSiObjetVisible extends NoeudBase {

    public NoeudConditionSiObjetVisible() {
        // Utilisation stricte du Traducteur (Règle Absolue N°2)
        super(genererId(), Traducteur.get("noeud_si_objet_visible"), Traducteur.get("cat_logique_conditions"));
        this.ajouterPort(new Port(Traducteur.get("port_entrer"), Port.TYPE_EXECUTION_ENTREE));
        this.ajouterPort(new Port(Traducteur.get("port_vrai"), Port.TYPE_EXECUTION_SORTIE));
        this.ajouterPort(new Port(Traducteur.get("port_faux"), Port.TYPE_EXECUTION_SORTIE));
    }

    @Override
    public void executer() {
        // AUCUN SHADOWING : appel direct à la méthode sécurisée de NoeudBase
        ObjetBase obj = getCibleObjet();
        
        if (obj != null) {
            if (obj.visible) {
                propagerExecution(Traducteur.get("port_vrai"));
            } else {
                propagerExecution(Traducteur.get("port_faux"));
            }
        } else {
            // Sécurité : si l'objet n'existe plus, on considère qu'il n'est pas visible
            propagerExecution(Traducteur.get("port_faux"));
        }
    }

    @Override
    public boolean requiertCibleObjet() { 
        return true; 
    }
    
    // RÈGLE ABSOLUE N°1 APPLIQUÉE : 
    // Suppression totale des variables locales "cible" et "nomCibleObjet"
    // Suppression totale de la redéfinition de setCibleObjet() et getCibleObjet()
    
    // Les méthodes getNomsParametres(), getValeurParametre(), etc., ont aussi 
    // été retirées car NoeudBase les gère déjà parfaitement par défaut quand 
    // il n'y a pas de paramètres dynamiques.
}
// bas 1
