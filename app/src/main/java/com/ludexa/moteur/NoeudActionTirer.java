package com.ludexa.moteur;

import java.util.Arrays;
import java.util.List;

public class NoeudActionTirer extends NoeudBase {
    
    // Cible A : Le point de départ (ex: Le Canon)
    private transient ObjetBase cible; 
    private String nomCibleObjet;
    
    // Cible B : L'objet à viser (ex: Le Joueur)
    private transient ObjetBase cibleB; 
    private String nomCibleObjetB;

    private String nomModele = ""; 
    private String modeDirection; 
    private String vitesseStr = "10.0"; // Valeur par défaut textuelle

    public NoeudActionTirer() {
        super(genererId(), Traducteur.get("noeud_tirer_nom"), Traducteur.get("cat_apparence_objets"));
        
        this.modeDirection = Traducteur.get("opt_angle_depart");

        this.ajouterPort(new Port(Traducteur.get("port_entrer"), Port.TYPE_EXECUTION_ENTREE));
        this.ajouterPort(new Port(Traducteur.get("port_suivant"), Port.TYPE_EXECUTION_SORTIE));
    }

    @Override
    public void executer() {
        ObjetBase spawnPoint = getCibleObjet();
        if (spawnPoint != null && contexteApplication != null) {
            try {
                java.lang.reflect.Field sceneField = contexteApplication.getClass().getField("sceneActive");
                Scene s = (Scene) sceneField.get(contexteApplication);
                if (s != null && s.objets != null) {
                    
                    ObjetBase modele = null;
                    for (ObjetBase o : s.objets) {
                        if (nomModele.equals(o.nom)) {
                            modele = o;
                            break;
                        }
                    }
                    
                    if (modele != null) {
                        ObjetBase clone = modele.clonerProfond();
                        
                        // 1. Positionnement au centre du point de départ
                        clone.x = spawnPoint.x + (spawnPoint.largeur / 2f) - (clone.largeur / 2f);
                        clone.y = spawnPoint.y + (spawnPoint.hauteur / 2f) - (clone.hauteur / 2f);
                        
                        // 2. Calcul de la rotation selon le mode
                        if (modeDirection != null && modeDirection.equals(Traducteur.get("opt_vers_cible"))) {
                            ObjetBase targetB = getCibleObjetB();
                            if (targetB != null) {
                                float centreCloneX = clone.x + (clone.largeur / 2f);
                                float centreCloneY = clone.y + (clone.hauteur / 2f);
                                float centreCibleX = targetB.x + (targetB.largeur / 2f);
                                float centreCibleY = targetB.y + (targetB.hauteur / 2f);
                                
                                clone.rotation = (float) Math.toDegrees(Math.atan2(centreCibleY - centreCloneY, centreCibleX - centreCloneX));
                            } else {
                                clone.rotation = spawnPoint.rotation;
                            }
                        } else {
                            clone.rotation = spawnPoint.rotation;
                        }
                        
                        // 3. Application de la vitesse
                        try {
                            clone.vitesseAvanceContinue = Float.parseFloat(vitesseStr);
                        } catch (NumberFormatException e) {
                            clone.vitesseAvanceContinue = 0f;
                        }

                        clone.visible = true;

                        // 4. Ajout et Z-Order
                        try {
                            java.lang.reflect.Method methodZ = s.getClass().getMethod("prochainZOrder");
                            clone.zOrder = (int) methodZ.invoke(s);
                        } catch (Exception e) {
                            clone.zOrder = 999;
                        }

                        s.objets.add(clone);
                    }
                }
            } catch (Exception e) {}
        }
        propagerExecution(Traducteur.get("port_suivant"));
    }

    @Override
    public List<String> getNomsParametres() { 
        return Arrays.asList(
            Traducteur.get("param_nom_modele"), 
            Traducteur.get("param_mode_direction"),
            Traducteur.get("param_vitesse")
        ); 
    }

    @Override
    public String getValeurParametre(String nom) {
        if (nom.equals(Traducteur.get("param_nom_modele"))) return nomModele;
        if (nom.equals(Traducteur.get("param_mode_direction"))) return modeDirection;
        if (nom.equals(Traducteur.get("param_vitesse"))) return vitesseStr;
        return "";
    }

    @Override
    public void setValeurParametre(String nom, String valeur) {
        if (nom.equals(Traducteur.get("param_nom_modele"))) nomModele = valeur;
        else if (nom.equals(Traducteur.get("param_mode_direction"))) modeDirection = valeur;
        else if (nom.equals(Traducteur.get("param_vitesse"))) vitesseStr = valeur;
    }

    @Override
    public String getTypeEditeurParametre(String nomParametre) { 
        if (nomParametre.equals(Traducteur.get("param_nom_modele"))) return TYPE_TEXTE_ALPHABETIQUE;
        if (nomParametre.equals(Traducteur.get("param_mode_direction"))) return TYPE_CHOIX_LISTE; 
        if (nomParametre.equals(Traducteur.get("param_vitesse"))) return TYPE_TEXTE_LIBRE; 
        return TYPE_TEXTE_LIBRE; 
    }
    
    @Override
    public List<String> getOptionsChoixListe(String nomParametre) {
        if (nomParametre.equals(Traducteur.get("param_mode_direction"))) {
            return Arrays.asList(
                Traducteur.get("opt_angle_depart"),
                Traducteur.get("opt_vers_cible")
            );
        }
        return super.getOptionsChoixListe(nomParametre);
    }

    // --- CIBLE A : Point de départ ---
    @Override
    public boolean requiertCibleObjet() { return true; }

    @Override
    public void setCibleObjet(ObjetBase objet) {
        this.cible = objet;
        this.nomCibleObjet = (objet != null) ? objet.nom : null;
    }

    @Override
    public ObjetBase getCibleObjet() {
        if (cible == null && nomCibleObjet != null && contexteApplication != null) {
            try {
                java.lang.reflect.Field sceneField = contexteApplication.getClass().getField("sceneActive");
                Scene s = (Scene) sceneField.get(contexteApplication);
                if (s != null && s.objets != null) {
                    for (ObjetBase o : s.objets) {
                        if (nomCibleObjet.equals(o.nom)) { cible = o; break; }
                    }
                }
            } catch (Exception e) {}
        }
        return cible;
    }

    // --- CIBLE B : Objet à viser ---
    @Override
    public boolean requiertCibleObjetB() { return true; }

    @Override
    public void setCibleObjetB(ObjetBase objet) {
        this.cibleB = objet;
        this.nomCibleObjetB = (objet != null) ? objet.nom : null;
    }

    @Override
    public ObjetBase getCibleObjetB() {
        if (cibleB == null && nomCibleObjetB != null && contexteApplication != null) {
            try {
                java.lang.reflect.Field sceneField = contexteApplication.getClass().getField("sceneActive");
                Scene s = (Scene) sceneField.get(contexteApplication);
                if (s != null && s.objets != null) {
                    for (ObjetBase o : s.objets) {
                        if (nomCibleObjetB.equals(o.nom)) { cibleB = o; break; }
                    }
                }
            } catch (Exception e) {}
        }
        return cibleB;
    }
}
