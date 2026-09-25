// haut 1
package com.ludexa.moteur;

import java.util.ArrayList;
import java.util.List;

// Événement « Au choc physique » : se déclenche quand l'objet cible subit un choc dans le moteur physique.
// La cible est gérée par NoeudBase (case commune nomCibleObjet) : ça marche aussi dans un prefab,
// avec « objet impliqué », et après destruction / recréation d'objets.
public class NoeudEventChoc extends NoeudBase {

    public NoeudEventChoc() {
        super(genererId(), "noeud_au_choc_physique", "Événements");
        this.ajouterPort(new Port("Sortie", Port.TYPE_EXECUTION_SORTIE));
    }

    @Override
    public void executer() {
        propagerExecution("Sortie");
    }

    @Override
    public List<String> getNomsParametres() { return new ArrayList<>(); }

    @Override
    public String getValeurParametre(String nom) { return ""; }

    @Override
    public void setValeurParametre(String nom, String valeur) { }

    @Override
    public boolean requiertCibleObjet() { return true; }

    // On n'enregistre que le nom, dans la case commune : l'objet est retrouvé à chaque fois par NoeudBase.getCibleObjet()
    @Override
    public void setCibleObjet(ObjetBase objet) {
        this.nomCibleObjet = (objet != null) ? objet.nom : null;
    }
}
// bas 1
