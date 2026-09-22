// haut 1
package com.ludexa.moteur;

// Événement lancé à chaque image, mais seulement tant qu'un doigt est posé sur l'écran de jeu (n'importe où).
public class NoeudEventDoigtAppuye extends NoeudEventChaqueImage {

    public NoeudEventDoigtAppuye() {
        super();
        this.nom = Traducteur.get("noeud_event_doigt_appuye");
    }

    @Override
    public void executer() {
        if (GestionnaireControles.doigtAppuye) {
            super.executer();
        }
    }
}
// bas 1
