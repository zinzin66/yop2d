// haut 1
package com.ludexa.moteur;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

// Les réglages du jeu : résolution et cadence. Ils sont enregistrés dans le dossier du projet
// (fichier reglages.json), donc ils suivent le projet : sauvegarde, export ZIP et APK.
public class ConfigurationJeu {

    public static final int LARGEUR_PAR_DEFAUT = 1920;
    public static final int HAUTEUR_PAR_DEFAUT = 1080;
    public static final int CADENCE_PAR_DEFAUT = 60;

    public static int LARGEUR_JEU = LARGEUR_PAR_DEFAUT;
    public static int HAUTEUR_JEU = HAUTEUR_PAR_DEFAUT;

    // Nombre de "pas de jeu" par seconde : 30, 60, 90 ou 120. 0 = automatique (un pas par image de l'écran).
    // Avec 60, le jeu va à la même vitesse sur tous les écrans, qu'ils soient à 60, 90 ou 120 Hz.
    public static int CADENCE = CADENCE_PAR_DEFAUT;

    private static final String NOM_FICHIER = "reglages.json";

    // Lit les réglages du projet. Sans fichier (ou avec un fichier abîmé), les valeurs par défaut s'appliquent :
    // un projet ne garde donc jamais les réglages du projet précédent.
    public static void charger(String cheminProjet) {
        LARGEUR_JEU = LARGEUR_PAR_DEFAUT;
        HAUTEUR_JEU = HAUTEUR_PAR_DEFAUT;
        CADENCE = CADENCE_PAR_DEFAUT;
        if (cheminProjet == null) return;
        File fichier = new File(cheminProjet, NOM_FICHIER);
        if (!fichier.exists()) return;
        try {
            BufferedReader lecteur = new BufferedReader(new FileReader(fichier));
            StringBuilder texte = new StringBuilder();
            String ligne;
            while ((ligne = lecteur.readLine()) != null) texte.append(ligne);
            lecteur.close();
            JSONObject json = new JSONObject(texte.toString());
            int largeur = json.optInt("largeur", LARGEUR_PAR_DEFAUT);
            int hauteur = json.optInt("hauteur", HAUTEUR_PAR_DEFAUT);
            if (largeur > 0 && largeur <= 20000) LARGEUR_JEU = largeur;
            if (hauteur > 0 && hauteur <= 20000) HAUTEUR_JEU = hauteur;
            int cadence = json.optInt("cadence", CADENCE_PAR_DEFAUT);
            if (cadence == 0 || cadence == 30 || cadence == 60 || cadence == 90 || cadence == 120) CADENCE = cadence;
        } catch (Exception e) {
            DiagLogger.log(cheminProjet, "ERREUR " + NOM_FICHIER + " illisible, réglages par défaut : " + e);
        }
    }

    // Enregistre les réglages actuels dans le dossier du projet.
    public static void sauvegarder(String cheminProjet) {
        if (cheminProjet == null) return;
        try {
            JSONObject json = new JSONObject();
            json.put("largeur", LARGEUR_JEU);
            json.put("hauteur", HAUTEUR_JEU);
            json.put("cadence", CADENCE);
            FileWriter ecrivain = new FileWriter(new File(cheminProjet, NOM_FICHIER));
            ecrivain.write(json.toString(2));
            ecrivain.close();
        } catch (Exception e) {
            DiagLogger.log(cheminProjet, "ERREUR " + NOM_FICHIER + " non enregistré : " + e);
        }
    }
}
// bas 1
