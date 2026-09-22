// haut 1
package com.ludexa.moteur;

// Événement lancé à chaque image, mais seulement tant que l'objet visé est touché.
public class NoeudEventMaintenuObjet extends NoeudEventChaqueImage {

    public NoeudEventMaintenuObjet() {
        super();
        this.nom = "noeud_event_maintenu_objet";
    }

    @Override
    public void executer() {
        ObjetBase cible = getCibleObjet();
        if (cible != null && cible.estTouche) {
            super.executer();
        }
    }

    @Override
    public boolean requiertCibleObjet() {
        return true;
    }
}
// bas 1
