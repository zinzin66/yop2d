// NoeudActionPauseAnimation.java
package com.ludexa.moteur;

public class NoeudActionPauseAnimation extends NoeudBase {

    public NoeudActionPauseAnimation() {
        super(genererId(), Traducteur.get("noeud_pause_anim"), Traducteur.get("cat_animations"));
        ajouterPort(new Port(Traducteur.get("port_entree"), Port.TYPE_EXECUTION_ENTREE));
        ajouterPort(new Port(Traducteur.get("port_sortie"), Port.TYPE_EXECUTION_SORTIE));
    }

    @Override
    public void executer() {
        ObjetBase cible = getCibleObjet();
        if (cible != null) {
            cible.animationEnCours = false; // Met l'animation en pause
        }
        propagerExecution(Traducteur.get("port_sortie"));
    }

    @Override
    public boolean requiertCibleObjet() { return true; }
}
