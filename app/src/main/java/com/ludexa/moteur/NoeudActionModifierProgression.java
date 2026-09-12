// haut 1
package com.ludexa.moteur;

public class NoeudActionModifierProgression extends NoeudBase {

    public NoeudActionModifierProgression() {
        super(genererId(), Traducteur.get("noeud_modifier_progression"), Traducteur.get("cat_apparence_objets"));
        this.ajouterPort(new Port(Traducteur.get("port_entree"), Port.TYPE_EXECUTION_ENTREE));
        this.ajouterPort(new Port(Traducteur.get("port_suivant"), Port.TYPE_EXECUTION_SORTIE));
        
        this.ajouterParametre(Traducteur.get("param_nouvelle_progression"), "100", TYPE_NOMBRE);
    }

    @Override
    public void executer() {
        ObjetBase obj = getCibleObjet();
        if (obj != null && "barre_progression".equals(obj.type)) {
            try {
                float nv = Float.parseFloat(getValeurParametre(Traducteur.get("param_nouvelle_progression")));
                if (nv < obj.progressionMin) nv = obj.progressionMin;
                if (nv > obj.progressionMax) nv = obj.progressionMax;
                obj.progressionActuelle = nv;
            } catch (Exception e) {}
        }
        propagerExecution(Traducteur.get("port_suivant"));
    }

    @Override
    public boolean requiertCibleObjet() { return true; }

    @Override
    public boolean utiliseClavierTexte() { return true; }
}
// bas 1
