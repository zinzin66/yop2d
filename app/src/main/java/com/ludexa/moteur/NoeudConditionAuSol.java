// haut 1
package com.ludexa.moteur;

public class NoeudConditionAuSol extends NoeudBase {

    public NoeudConditionAuSol() {
        super(genererId(), Traducteur.get("noeud_condition_au_sol"), Traducteur.get("cat_physique"));
        this.ajouterPort(new Port(Traducteur.get("port_entrer"), Port.TYPE_EXECUTION_ENTREE));
        this.ajouterPort(new Port(Traducteur.get("port_vrai"), Port.TYPE_EXECUTION_SORTIE));
        this.ajouterPort(new Port(Traducteur.get("port_faux"), Port.TYPE_EXECUTION_SORTIE));
    }

    @Override
    public void executer() {
        ObjetBase obj = getCibleObjet();
        boolean auSol = false;

        if (obj != null && contexteApplication != null) {
            try {
                Scene scene = null;
                java.lang.reflect.Field sceneField = contexteApplication.getClass().getField("sceneActive");
                scene = (Scene) sceneField.get(contexteApplication);
                if (scene == null) scene = sceneActiveCourante;

                if (scene != null && scene.objets != null) {
                    float basObjet = obj.y + obj.hauteur;
                    float margeTolerance = 5f; // Tolérance pour considérer qu'on est "posé"

                    for (ObjetBase autre : scene.objets) {
                        if (autre == obj || !autre.visible || !autre.estStatique) continue;

                        // Vérifier l'alignement horizontal
                        boolean auDessusX = (obj.x < autre.x + autre.largeur) && (obj.x + obj.largeur > autre.x);
                        
                        // Vérifier si le bas de notre objet touche le haut de l'autre objet
                        boolean toucheY = (basObjet >= autre.y - margeTolerance) && (basObjet <= autre.y + margeTolerance);

                        if (auDessusX && toucheY) {
                            auSol = true;
                            break;
                        }
                    }
                }
            } catch (Exception e) {}
        }

        if (auSol) {
            propagerExecution(Traducteur.get("port_vrai"));
        } else {
            propagerExecution(Traducteur.get("port_faux"));
        }
    }

    @Override
    public boolean requiertCibleObjet() { return true; }
}
// bas 1
