// haut 1
package com.ludexa.moteur;

import android.graphics.Color;

public class StyleTitre {
    public String nom;
    public String modeRemplissage = "FILL_AND_STROKE"; // Peut être "FILL", "STROKE", "FILL_AND_STROKE", "TEXTURE", "TEXTURE_AND_STROKE"
    
    // Base
    public float epaisseurContour = 4f;
    public int couleurContour = Color.BLACK;
    public boolean utiliserDegrade = false;
    public int couleurDegrade1 = Color.WHITE;
    public int couleurDegrade2 = Color.GRAY;
    
    // Ombre classique
    public boolean ombreActive = false;
    public float ombreRayon = 5f;
    public float ombreDx = 5f;
    public float ombreDy = 5f;
    public int ombreCouleur = Color.BLACK;
    
    // Espacement et Alignement
    public float espacementLettres = 0.05f; 
    public float multiplicateurLignes = 1.2f;
    public String alignement = "CENTRE"; // "GAUCHE", "CENTRE", "DROITE"
    
    // Texture
    public String cheminTexture = null;
    
    // Néon / Lueur externe
    public boolean neonActif = false;
    public float neonRayon = 10f;
    public int neonCouleur = Color.CYAN;
    
    // Relief / Emboss 3D
    public boolean reliefActif = false;
    public float reliefElevation = 3f;
    
    // Déformations
    public float inclinaison = 0f; // Valeur SkewX (ex: -0.25f pour italique)
    public float courbure = 0f; // Offset Y pour l'arc de cercle

    public StyleTitre() {}

    public StyleTitre(String nom) {
        this.nom = nom;
    }
}
// bas 1
