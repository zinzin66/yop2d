package com.ludexa.moteur;

import java.util.List;

public class NoeudEventFinClicObjet extends NoeudBase {

    public NoeudEventFinClicObjet() {
        // La clé de traduction et le type de nœud
        super(genererId(), "noeud_fin_clic_sur_objet", "Événement");
        
        // Un événement n'a qu'un port de sortie, pas de port d'entrée
        this.ajouterPort(new Port("Suivant", Port.TYPE_EXECUTION_SORTIE));
    }

    @Override
    public boolean requiertCibleObjet() {
        return true; // Demande à l'utilisateur de choisir sur quel objet écouter le relâchement
    }

    /* 
     * INSTRUCTIONS D'INTÉGRATION :
     * 1. Ouvre ton fichier NoeudEventClicObjet.java
     * 2. Copie la méthode qui gère l'événement (elle s'appelle probablement 
     *    onTouchEvent(MotionEvent event), verifierDeclenchement(), ou executerEvenement())
     * 3. Colle-la ici.
     * 4. Modifie la ligne qui vérifie l'action. 
     *    Change "event.getAction() == MotionEvent.ACTION_DOWN" 
     *    par "event.getAction() == MotionEvent.ACTION_UP"
     * 5. Assure-toi que la méthode contient bien propagerExecution("Suivant"); quand la condition est remplie.
     */
}
