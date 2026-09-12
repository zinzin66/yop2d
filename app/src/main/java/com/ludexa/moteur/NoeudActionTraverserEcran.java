// haut 1
package com.ludexa.moteur;

public class NoeudActionTraverserEcran extends NoeudBase {

    public NoeudActionTraverserEcran() {
        super(genererId(), Traducteur.get("noeud_traverser_ecran"), Traducteur.get("cat_apparence_objets"));
        
        this.ajouterPort(new Port(Traducteur.get("port_entrer"), Port.TYPE_EXECUTION_ENTREE));
        this.ajouterPort(new Port(Traducteur.get("port_suivant"), Port.TYPE_EXECUTION_SORTIE));
    }

    @Override
    public void executer() {
        ObjetBase obj = getCibleObjet();
        
        if (obj != null) {
            // Sortie à droite -> retour à gauche
            if (obj.x > ConfigurationJeu.LARGEUR_JEU) {
                obj.x = -obj.largeur;
            } 
            // Sortie à gauche -> retour à droite
            else if (obj.x + obj.largeur < 0) {
                obj.x = ConfigurationJeu.LARGEUR_JEU;
            }

            // Sortie en bas -> retour en haut
            if (obj.y > ConfigurationJeu.HAUTEUR_JEU) {
                obj.y = -obj.hauteur;
            } 
            // Sortie en haut -> retour en bas
            else if (obj.y + obj.hauteur < 0) {
                obj.y = ConfigurationJeu.HAUTEUR_JEU;
            }
        }
        
        propagerExecution(Traducteur.get("port_suivant"));
    }

    @Override
    public boolean requiertCibleObjet() { return true; }
}
// bas 1
