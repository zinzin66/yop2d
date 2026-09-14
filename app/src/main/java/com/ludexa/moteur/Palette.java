// haut 1
package com.ludexa.moteur;

import android.graphics.Color;
import android.widget.ImageView;

public class Palette {
    // Couleurs de base (thème Godot)
    public static int fondPanneaux = Color.parseColor("#1A1F26");
    public static int enTeteDialogues = Color.parseColor("#242A33");
    public static int bordure = Color.parseColor("#3A4048");
    public static int texteNormal = Color.parseColor("#FFFFFF");
    public static int texteSelectionne = Color.parseColor("#ECEEF0");
    public static int boutonNormal = Color.parseColor("#252B33");
    public static int boutonSurvol = Color.parseColor("#2F3B45");
    public static int canvasFond = Color.parseColor("#20252D");
    public static int canvasGrille = Color.parseColor("#161A20");

    public static int fondListe = Color.parseColor("#20252D");
    public static int fondNormal = Color.parseColor("#1A1F26");

    // Couleur dédiée aux icônes monochromes
    public static int iconeNormal = Color.parseColor("#C9CCD1");
    public static int iconeSurvol = Color.parseColor("#5DCAA5");

    // Nouveaux champs (thème Godot)
    public static int texteSecondaire = Color.parseColor("#9AA0A8");
    public static int texteDesactive = Color.parseColor("#8A8F98");
    public static int accentTeal = Color.parseColor("#5DCAA5");
    public static int accentBleu = Color.parseColor("#85B7EB");
    public static int accentAmbre = Color.parseColor("#FAC775");
    public static int fondVignette = Color.parseColor("#161A20");
    public static int fondSelection = Color.parseColor("#2F3B45");

    public static void appliquerCouleurIcone(ImageView icone, int couleur) {
        icone.setColorFilter(couleur);
    }
}
// bas 1
