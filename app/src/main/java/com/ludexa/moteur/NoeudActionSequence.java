package com.ludexa.moteur;

import java.util.List;

public class NoeudActionSequence extends NoeudBase {

    public NoeudActionSequence() {
        // "cat_logique_conditions" est la catégorie "Logique & Conditions"
        super(genererId(), Traducteur.get("noeud_sequence"), Traducteur.get("cat_logique_conditions"));
        
        // Port d'entrée (Utilise la clé existante "port_entree")
        this.ajouterPort(new Port(Traducteur.get("port_entree"), Port.TYPE_EXECUTION_ENTREE));
        
        // Ports de sortie numérotés en dur (universels)
        this.ajouterPort(new Port("1", Port.TYPE_EXECUTION_SORTIE));
        this.ajouterPort(new Port("2", Port.TYPE_EXECUTION_SORTIE));
    }

    @Override
    public void executer() {
        propagerExecution("1");
        propagerExecution("2");
    }

    @Override
    public List<String> getNomsParametres() { return null; }
    
    @Override
    public String getValeurParametre(String nom) { return ""; }
    
    @Override
    public void setValeurParametre(String nom, String valeur) {}

    @Override
    public boolean requiertCibleObjet() { return false; }
}
