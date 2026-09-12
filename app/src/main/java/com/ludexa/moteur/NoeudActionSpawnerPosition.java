// haut 1
package com.ludexa.moteur;

public class NoeudActionSpawnerPosition extends NoeudBase {

    public NoeudActionSpawnerPosition() {
        super(genererId(), Traducteur.get("noeud_generer_clone_position"), Traducteur.get("cat_apparence_objets"));
        
        this.ajouterPort(new Port(Traducteur.get("port_entrer"), Port.TYPE_EXECUTION_ENTREE));
        this.ajouterPort(new Port(Traducteur.get("port_suivant"), Port.TYPE_EXECUTION_SORTIE));
        
        // Utilisation propre de l'héritage NoeudBase pour gérer les paramètres avec Traducteur
        this.ajouterParametre(Traducteur.get("param_nom_modele"), "", TYPE_TEXTE_ALPHABETIQUE);
        this.ajouterParametre(Traducteur.get("param_x"), "0", TYPE_NOMBRE);
        this.ajouterParametre(Traducteur.get("param_y"), "0", TYPE_NOMBRE);
    }

    @Override
    public void executer() {
        if (contexteApplication != null) {
            try {
                java.lang.reflect.Field sceneField = contexteApplication.getClass().getField("sceneActive");
                Scene s = (Scene) sceneField.get(contexteApplication);
                
                if (s != null && s.objets != null) {
                    // Récupération sécurisée des paramètres
                    String nomModele = getValeurParametre(Traducteur.get("param_nom_modele"));
                    float posX = 0f;
                    float posY = 0f;
                    
                    try { posX = Float.parseFloat(getValeurParametre(Traducteur.get("param_x"))); } catch (NumberFormatException e) {}
                    try { posY = Float.parseFloat(getValeurParametre(Traducteur.get("param_y"))); } catch (NumberFormatException e) {}
                    
                    ObjetBase modele = null;
                    for (ObjetBase o : s.objets) {
                        if (nomModele.equals(o.nom)) {
                            modele = o;
                            break;
                        }
                    }
                    
                    if (modele != null) {
                        ObjetBase clone = modele.clonerProfond();
                        
                        clone.nom = modele.nom + "_clone_" + System.currentTimeMillis();
                        clone.x = posX;
                        clone.y = posY;
                        clone.visible = true;

                        try {
                            java.lang.reflect.Method methodZ = s.getClass().getMethod("prochainZOrder");
                            clone.zOrder = (int) methodZ.invoke(s);
                        } catch (Exception e) {
                            clone.zOrder = 999;
                        }

                        s.objets.add(clone);
                        
                        // CORRECTION CRITIQUE (Feuille de route) : 
                        // Permet de cibler ce nouveau clone via "Objet Impliqué" dans les nœuds suivants
                        MoteurLogique.dernierObjetImplique = clone;
                    }
                }
            } catch (Exception e) {
                // Ignoré silencieusement pour éviter de crasher la boucle d'exécution (standard du projet)
            }
        }
        propagerExecution(Traducteur.get("port_suivant"));
    }
    
    // Ce nœud n'utilise pas de ciblage d'objet (seulement X et Y),
    // on ne redéfinit donc pas requiertCibleObjet() qui reste à false par défaut.
}
// bas 1
