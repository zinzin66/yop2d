// haut 1
package com.ludexa.moteur;

public class NoeudConditionSiObjetToucheZone extends NoeudBase {

    public NoeudConditionSiObjetToucheZone() {
        super(genererId(), Traducteur.get("noeud_si_objet_a_touche_zone_b"), Traducteur.get("cat_logique_conditions"));
        this.ajouterPort(new Port(Traducteur.get("port_entrer"), Port.TYPE_EXECUTION_ENTREE));
        this.ajouterPort(new Port(Traducteur.get("port_vrai"), Port.TYPE_EXECUTION_SORTIE));
        this.ajouterPort(new Port(Traducteur.get("port_faux"), Port.TYPE_EXECUTION_SORTIE));
    }

    @Override
    public void executer() {
        boolean collision = false;
        ObjetBase objA = getCibleObjet();
        ObjetBase objB = getCibleObjetB();
        
        if (objA != null && objB != null && objA.visible && objB.visible) {
            // Logique AABB standard : fonctionne en éditeur ET dans le jeu final
            boolean chevauchementX = (objA.x < objB.x + objB.largeur) && (objA.x + objA.largeur > objB.x);
            boolean chevauchementY = (objA.y < objB.y + objB.hauteur) && (objA.y + objA.hauteur > objB.y);
            
            if (chevauchementX && chevauchementY) {
                collision = true;
            }
        }
        
        if (collision) {
            propagerExecution(Traducteur.get("port_vrai"));
        } else {
            propagerExecution(Traducteur.get("port_faux"));
        }
    }

    @Override
    public boolean requiertCibleObjet() { return true; }
    
    @Override
    public boolean requiertCibleObjetB() { return true; }
    
    // AUCUN SHADOWING : getCibleObjet() et getCibleObjetB() sont gérés par NoeudBase
}
// bas 1
