// NoeudConditionSiObjetToucheZone.java
package com.ludexa.moteur;

import java.util.ArrayList;
import java.util.List;

public class NoeudConditionSiObjetToucheZone extends NoeudBase {

    public NoeudConditionSiObjetToucheZone() {
        super(genererId(), "Si objet A touche zone B", "Logique");
        this.ajouterPort(new Port("Entrer", Port.TYPE_EXECUTION_ENTREE));
        this.ajouterPort(new Port("Vrai", Port.TYPE_EXECUTION_SORTIE));
        this.ajouterPort(new Port("Faux", Port.TYPE_EXECUTION_SORTIE));
    }

    @Override
    public void executer() {
        boolean collision = false;
        ObjetBase objetCible = getCibleObjet();
        ObjetBase objetCibleB = getCibleObjetB();
        
        if (objetCible != null && objetCibleB != null && contexteApplication != null) {
            try {
                if (contexteApplication instanceof InterfaceEditeur) {
                    InterfaceEditeur editeur = (InterfaceEditeur) contexteApplication;
                    // Utilisation du getter pour remplacer l'accès direct au champ privé vueJeu
                    if (editeur.getVueJeu() != null && editeur.sceneActive != null) {
                        collision = UtilCollision.rectanglesSeChevauchent(
                            objetCible, editeur.sceneActive.objets, 
                            objetCibleB, editeur.sceneActive.objets, 
                            editeur.getVueJeu()
                        );
                    }
                }
            } catch (Exception e) {}
        }
        
        if (collision) {
            propagerExecution("Vrai");
        } else {
            propagerExecution("Faux");
        }
    }

    @Override
    public List<String> getNomsParametres() { return new ArrayList<>(); }
    @Override
    public String getValeurParametre(String nom) { return ""; }
    @Override
    public void setValeurParametre(String nom, String valeur) {}

    @Override
    public boolean requiertCibleObjet() { return true; }

    // -- Mécanisme DÉDIÉ Cible Objet B --
    @Override
    public boolean requiertCibleObjetB() { return true; }
}
