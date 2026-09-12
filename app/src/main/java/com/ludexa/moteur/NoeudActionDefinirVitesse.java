// haut 1
package com.ludexa.moteur;

public class NoeudActionDefinirVitesse extends NoeudBase {

    public NoeudActionDefinirVitesse() {
        super(genererId(), Traducteur.get("noeud_definir_vitesse"), Traducteur.get("cat_physique"));
        
        this.ajouterPort(new Port(Traducteur.get("port_entrer"), Port.TYPE_EXECUTION_ENTREE));
        this.ajouterPort(new Port(Traducteur.get("port_suivant"), Port.TYPE_EXECUTION_SORTIE));
        
        // Paramètres pour définir explicitement X et Y
        this.ajouterParametre(Traducteur.get("param_vitesse_x"), "0", TYPE_NOMBRE);
        this.ajouterParametre(Traducteur.get("param_vitesse_y"), "0", TYPE_NOMBRE);
    }

    @Override
    public void executer() {
        ObjetBase obj = getCibleObjet();
        
        if (obj != null) {
            try {
                String strX = getValeurParametre(Traducteur.get("param_vitesse_x"));
                if (strX != null && !strX.isEmpty()) {
                    obj.vitesseX = Float.parseFloat(strX);
                }
            } catch (NumberFormatException e) {
                // Ignore silencieusement en cas d'erreur de parsing
            }
            
            try {
                String strY = getValeurParametre(Traducteur.get("param_vitesse_y"));
                if (strY != null && !strY.isEmpty()) {
                    obj.vitesseY = Float.parseFloat(strY);
                }
            } catch (NumberFormatException e) {
                // Ignore silencieusement en cas d'erreur de parsing
            }
        }
        
        propagerExecution(Traducteur.get("port_suivant"));
    }

    @Override
    public boolean requiertCibleObjet() { 
        return true; 
    }
    
    // RÈGLE ABSOLUE N°1 : AUCUNE redéfinition de getCibleObjet / setCibleObjet.
}
// bas 1
