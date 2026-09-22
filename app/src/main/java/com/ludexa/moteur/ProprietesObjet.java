// haut 1
package com.ludexa.moteur;

import java.util.Arrays;
import java.util.List;

// Propriétés d'un objet utilisables dans une formule (ex : player.x) ou modifiables par un nœud.
// La liste est écrite explicitement (et non lue par réflexion sur les champs) : c'est plus sûr
// dans l'APK exporté, et on n'expose que ce qui est utile à un créateur de jeu.
public class ProprietesObjet {

    // Noms proposés à l'utilisateur (panneau d'aide), dans cet ordre.
    public static final List<String> NOMS = Arrays.asList(
            "x", "y", "largeur", "hauteur", "rotation", "opacite", "visible",
            "echelleX", "echelleY", "vitesseX", "vitesseY", "texte", "tag", "nom", "progression", "z", "touche",
            "animation", "image", "animee");

    // Propriétés que l'on peut aussi MODIFIER. Le nom reste en lecture seule :
    // le renommage passe par l'inspecteur, qui vérifie les doublons.
    // "touche", "animation", "image" et "animee" sont aussi en lecture seule : ce sont les nœuds d'animation
    // (et le doigt) qui les règlent.
    public static final List<String> NOMS_MODIFIABLES = Arrays.asList(
            "x", "y", "largeur", "hauteur", "rotation", "opacite", "visible",
            "echelleX", "echelleY", "vitesseX", "vitesseY", "texte", "tag", "progression", "z");

    // Nom officiel (minuscules, sans accents) d'une propriété, alias compris. null si inconnue.
    public static String cle(String nom) {
        if (nom == null) return null;
        switch (Evaluateur.normaliser(nom)) {
            case "x": return "x";
            case "y": return "y";
            case "largeur": return "largeur";
            case "hauteur": return "hauteur";
            case "rotation": return "rotation";
            case "opacite": case "alpha": return "opacite";
            case "visible": return "visible";
            case "echellex": case "scalex": return "echellex";
            case "echelley": case "scaley": return "echelley";
            case "vitessex": return "vitessex";
            case "vitessey": return "vitessey";
            case "texte": case "contenutexte": return "texte";
            case "tag": return "tag";
            case "nom": return "nom";
            case "progression": case "progressionactuelle": return "progression";
            case "z": case "zorder": return "z";
            case "touche": return "touche";
            case "animation": case "animationactive": return "animation";
            case "image": case "frame": case "imagecourante": return "image";
            case "animee": case "animationencours": return "animee";
            default: return null;
        }
    }

    public static boolean existe(String nom) {
        return cle(nom) != null;
    }

    // Valeur d'une propriété : Double, Boolean ou String. null si la propriété n'existe pas.
    public static Object lire(ObjetBase o, String nom) {
        String k = cle(nom);
        if (o == null || k == null) return null;
        switch (k) {
            case "x": return (double) o.x;
            case "y": return (double) o.y;
            case "largeur": return (double) o.largeur;
            case "hauteur": return (double) o.hauteur;
            case "rotation": return (double) o.rotation;
            case "opacite": return (double) o.alpha;
            case "visible": return o.visible;
            case "echellex": return (double) o.scaleX;
            case "echelley": return (double) o.scaleY;
            case "vitessex": return (double) o.vitesseX;
            case "vitessey": return (double) o.vitesseY;
            case "texte": return o.contenuTexte != null ? o.contenuTexte : "";
            case "tag": return o.tag != null ? o.tag : "";
            case "nom": return o.nom != null ? o.nom : "";
            case "progression": return (double) o.progressionActuelle;
            case "z": return (double) o.zOrder;
            case "touche": return o.estTouche;
            case "animation": return o.animationActive != null ? o.animationActive : "";
            case "image": return (double) o.frameCourante;
            case "animee": return o.animationEnCours;
            default: return null;
        }
    }

    // Modifie une propriété en convertissant la valeur. Retourne false si la propriété
    // n'existe pas ou n'est pas modifiable.
    public static boolean ecrire(ObjetBase o, String nom, Object valeur) {
        String k = cle(nom);
        if (o == null || k == null) return false;
        switch (k) {
            case "x": o.x = (float) Evaluateur.enNombre(valeur); return true;
            case "y": o.y = (float) Evaluateur.enNombre(valeur); return true;
            case "largeur": o.largeur = (float) Evaluateur.enNombre(valeur); return true;
            case "hauteur": o.hauteur = (float) Evaluateur.enNombre(valeur); return true;
            case "rotation": o.rotation = (float) Evaluateur.enNombre(valeur); return true;
            case "opacite":
                o.alpha = (float) Math.max(0.0, Math.min(1.0, Evaluateur.enNombre(valeur)));
                return true;
            case "visible": o.visible = Evaluateur.enBooleen(valeur); return true;
            case "echellex": o.scaleX = (float) Evaluateur.enNombre(valeur); return true;
            case "echelley": o.scaleY = (float) Evaluateur.enNombre(valeur); return true;
            case "vitessex": o.vitesseX = (float) Evaluateur.enNombre(valeur); return true;
            case "vitessey": o.vitesseY = (float) Evaluateur.enNombre(valeur); return true;
            case "texte": o.contenuTexte = Evaluateur.enTexte(valeur); return true;
            case "tag": o.tag = Evaluateur.enTexte(valeur); return true;
            case "progression":
                o.progressionActuelle = (float) Math.max(o.progressionMin,
                        Math.min(o.progressionMax, Evaluateur.enNombre(valeur)));
                return true;
            case "z": o.zOrder = (int) Math.round(Evaluateur.enNombre(valeur)); return true;
            default: return false;
        }
    }
}
// bas 1
