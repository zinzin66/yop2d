// haut 1
package com.ludexa.moteur;

import java.util.HashMap;

public class NoeudConditionCooldown extends NoeudBase {

    // Stocke le dernier temps d'exécution, séparé par ID d'objet pour gérer les clones/prefabs
    private transient HashMap<String, Long> derniersTemps = new HashMap<>();

    public NoeudConditionCooldown() {
        super(genererId(), Traducteur.get("noeud_cooldown"), Traducteur.get("cat_logique_conditions"));
        
        this.ajouterPort(new Port(Traducteur.get("port_entrer"), Port.TYPE_EXECUTION_ENTREE));
        this.ajouterPort(new Port(Traducteur.get("port_vrai"), Port.TYPE_EXECUTION_SORTIE));
        this.ajouterPort(new Port(Traducteur.get("port_faux"), Port.TYPE_EXECUTION_SORTIE));
        
        this.ajouterParametre(Traducteur.get("param_delai_ms"), "200", TYPE_NOMBRE);
    }

    @Override
    public void executer() {
        ObjetBase obj = getCibleObjet();
        long tempsActuel = System.currentTimeMillis();
        long delai = 200;
        
        try {
            String strDelai = getValeurParametre(Traducteur.get("param_delai_ms"));
            if (strDelai != null && !strDelai.isEmpty()) {
                delai = (long) Float.parseFloat(strDelai);
            }
        } catch (NumberFormatException e) {}

        boolean tirAutorise = false;

        if (obj != null) {
            if (derniersTemps == null) derniersTemps = new HashMap<>();
            
            long dernierTemps = derniersTemps.containsKey(obj.id) ? derniersTemps.get(obj.id) : 0;
            
            if (tempsActuel - dernierTemps >= delai) {
                derniersTemps.put(obj.id, tempsActuel);
                tirAutorise = true;
            }
        } else {
            // Repli : si aucune cible n'est liée, le cooldown s'applique au nœud lui-même
            if (derniersTemps == null) derniersTemps = new HashMap<>();
            long dernierTemps = derniersTemps.containsKey("global") ? derniersTemps.get("global") : 0;
            
            if (tempsActuel - dernierTemps >= delai) {
                derniersTemps.put("global", tempsActuel);
                tirAutorise = true;
            }
        }

        if (tirAutorise) {
            propagerExecution(Traducteur.get("port_vrai"));
        } else {
            propagerExecution(Traducteur.get("port_faux"));
        }
    }

    @Override
    public boolean requiertCibleObjet() { return true; }
}
// bas 1
