package com.ludexa.moteur;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NoeudActionExtrairePropriete extends NoeudBase {

    private String proprieteChoisie = "x"; 

    public NoeudActionExtrairePropriete() {
        // La clé "noeud_extraire_propriete" sera traduite par l'interface globale
        super(genererId(), "noeud_extraire_propriete", "Action");
        this.ajouterPort(new Port("Entrer", Port.TYPE_EXECUTION_ENTREE));
        this.ajouterPort(new Port("Suivant", Port.TYPE_EXECUTION_SORTIE));
    }

    @Override
    public void executer() {
        ObjetBase cible = getCibleObjet();
        Variable varCible = getCibleVariable();

        if (cible != null && varCible != null && proprieteChoisie != null && !proprieteChoisie.isEmpty()) {
            try {
                Field champ = ObjetBase.class.getField(proprieteChoisie);
                Object valeurRecuperee = champ.get(cible);
                varCible.valeur = valeurRecuperee;
            } catch (Exception e) {
                // Ignoré silencieusement pour ne pas faire planter la boucle de jeu
            }
        }
        
        propagerExecution("Suivant");
    }

    @Override
    public List<String> getNomsParametres() {
        // Retourne la clé de traduction. EditeurNoeudDialog fera automatiquement Traducteur.get("param_propriete")
        return Arrays.asList("param_propriete");
    }

    @Override
    public String getValeurParametre(String nom) {
        if ("param_propriete".equals(nom)) return proprieteChoisie;
        return "";
    }

    @Override
    public void setValeurParametre(String nom, String valeur) {
        if ("param_propriete".equals(nom) && valeur != null) {
            proprieteChoisie = valeur;
        }
    }

    @Override
    public String getTypeEditeurParametre(String nom) {
        if ("param_propriete".equals(nom)) return TYPE_CHOIX_LISTE;
        return super.getTypeEditeurParametre(nom);
    }

    @Override
    public List<String> getOptionsChoixListe(String nom) {
        if ("param_propriete".equals(nom)) {
            List<String> options = new ArrayList<>();
            Field[] champs = ObjetBase.class.getFields();
            
            for (Field champ : champs) {
                int modificateurs = champ.getModifiers();
                
                if (!Modifier.isTransient(modificateurs)) {
                    Class<?> type = champ.getType();
                    
                    if (type == float.class || type == int.class || type == long.class || 
                        type == boolean.class || type == String.class) {
                        // Ajoute le nom technique exact de la variable (ex: progressionActuelle)
                        options.add(champ.getName());
                    }
                }
            }
            Collections.sort(options);
            return options;
        }
        return super.getOptionsChoixListe(nom);
    }

    @Override
    public boolean requiertCibleObjet() {
        return true; 
    }

    @Override
    public boolean requiertCibleVariable() {
        return true; 
    }
                }
