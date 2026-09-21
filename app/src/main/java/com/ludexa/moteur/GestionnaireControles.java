// haut 1
package com.ludexa.moteur;

public class GestionnaireControles {
    // --- HUD Aventure (Joystick & Action) ---
    public static boolean modeAventureActif = true; 
    public static float joyDirX = 0f;
    public static float joyDirY = 0f;
    public static boolean isActionPressed = false;
    public static boolean isActionJustPressed = false;

    // --- Doigt (premier doigt posé sur l'écran de jeu) ---
    // Position dans l'écran de jeu : 0,0 = coin en haut à gauche, sans tenir compte de la caméra.
    // Lues par les formules : doigt.x, doigt.y, doigt.appuye
    public static float doigtX = 0f;
    public static float doigtY = 0f;
    public static boolean doigtAppuye = false;

    // --- Système de Caméra ---
    public static String cameraCibleId = null;
    public static float cameraX = 0f;
    public static float cameraY = 0f;
    
    // --- NOUVEAUX REGLAGES DE CAMERA ---
    public static boolean cameraSuitAxeX = true;
    public static boolean cameraSuitAxeY = true;
    public static boolean parallaxeUniquementX = true;
    
    // Bornes de la caméra repoussées à "l'infini" (valeurs extrêmes)
    public static float limiteMinX = -999999f;
    public static float limiteMaxX = 999999f; 
    public static float limiteMinY = -999999f;
    public static float limiteMaxY = 999999f;

    // Réinitialisation propre à chaque lancement du mode Play
    public static void reinitialiser() {
        joyDirX = 0f;
        joyDirY = 0f;
        isActionPressed = false;
        isActionJustPressed = false;
        doigtX = 0f;
        doigtY = 0f;
        doigtAppuye = false;
        reinitialiserCamera();
    }

    // Caméra à zéro : au lancement du Play, et à chaque changement ou redémarrage de scène
    // (la nouvelle scène règle elle-même sa caméra à son démarrage).
    public static void reinitialiserCamera() {
        cameraCibleId = null;
        cameraX = 0f;
        cameraY = 0f;
        
        cameraSuitAxeX = true;
        cameraSuitAxeY = true;
        parallaxeUniquementX = true;
        
        limiteMinX = -999999f;
        limiteMaxX = 999999f;
        limiteMinY = -999999f;
        limiteMaxY = 999999f;
    }
}
// bas 1
