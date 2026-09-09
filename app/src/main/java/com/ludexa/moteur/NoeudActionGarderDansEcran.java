// haut 1
package com.ludexa.moteur;

public class NoeudActionGarderDansEcran extends NoeudBase {

    public NoeudActionGarderDansEcran() {
        super(genererId(), Traducteur.get("noeud_garder_ecran"), Traducteur.get("cat_apparence_objets"));
        
        this.ajouterPort(new Port(Traducteur.get("port_entrer"), Port.TYPE_EXECUTION_ENTREE));
        this.ajouterPort(new Port(Traducteur.get("port_suivant"), Port.TYPE_EXECUTION_SORTIE));
        
        this.ajouterParametre(Traducteur.get("param_marge"), "0", TYPE_NOMBRE);
    }

    @Override
    public void executer() {
        ObjetBase obj = getCibleObjet();
        
        if (obj != null) {
            float marge = 0f;
            try {
                String strMarge = getValeurParametre(Traducteur.get("param_marge"));
                if (strMarge != null && !strMarge.isEmpty()) marge = Float.parseFloat(strMarge);
            } catch (Exception e) {}

            float minX = marge;
            float maxX = ConfigurationJeu.LARGEUR_JEU - obj.largeur - marge;
            float minY = marge;
            float maxY = ConfigurationJeu.HAUTEUR_JEU - obj.hauteur - marge;

            if (obj.x < minX) obj.x = minX;
            if (obj.x > maxX) obj.x = maxX;
            if (obj.y < minY) obj.y = minY;
            if (obj.y > maxY) obj.y = maxY;
        }
        
        propagerExecution(Traducteur.get("port_suivant"));
    }

    @Override
    public boolean requiertCibleObjet() { return true; }
}
// bas 1
