package com.ludexa.moteur;

import android.graphics.Color;

public class StyleTitre {
    public String nom;
    public String modeRemplissage = "FILL_AND_STROKE";
    public float epaisseurContour = 4f;
    public int couleurContour = Color.BLACK;
    public boolean utiliserDegrade = false;
    public int couleurDegrade1 = Color.WHITE;
    public int couleurDegrade2 = Color.GRAY;
    public boolean ombreActive = false;
    public float ombreRayon = 5f;
    public float ombreDx = 5f;
    public float ombreDy = 5f;
    public int ombreCouleur = Color.BLACK;
    
    // --- NOUVEAUTÉS : ESPACEMENT ---
    public float espacementLettres = 0.05f; // En EM (0.05 = léger, 0.1 = espacé)
    public float multiplicateurLignes = 1.2f;

    public StyleTitre() {}

    public StyleTitre(String nom) {
        this.nom = nom;
    }
}
