// haut 1
package com.ludexa.moteur;

import android.widget.Toast;

// Les actions du moteur : ce que fait chaque nœud du catalogue. Chaque action est écrite UNE seule fois, ici.
// Une action retourne le nom du port de sortie à suivre, ou null pour prendre la sortie normale du nœud.
// Les champs se lisent avec n.nombre("x"), n.booleen("condition"), n.texte("message") :
// ces lectures acceptent toutes les formules (player.x + 5, score > 3...).
public class ActionsMoteur {

    public static String executer(String action, NoeudGenerique n) {
        switch (action) {
            case "deplacer": deplacer(n); return null;
            case "pousser": pousser(n); return null;
            case "modifier_variable": modifierVariable(n); return null;
            case "toast": toast(n); return null;
            case "condition": return n.booleen("condition") ? "port_vrai" : "port_faux";
            case "modifier_propriete": modifierPropriete(n); return null;
                case "ajouter_variable": ajouterVariable(n); return null;
            case "limiter_variable": limiterVariable(n); return null;
                case "definir_echelle": ActionsObjets.definirEchelle(n); return null;
            case "changer_image": ActionsObjets.changerImage(n); return null;
            case "modifier_couleur": ActionsObjets.modifierCouleur(n); return null;
            case "detruire": ActionsObjets.detruire(n); return null;
            case "detruire_par_tag": ActionsObjets.detruireParTag(n); return null;
            case "cloner": ActionsObjets.cloner(n); return null;
            default: throw new IllegalStateException("Action inconnue : " + action);
        }
    }

    // Place l'objet à la position donnée (les deux calculs sont faits avant de déplacer).
    private static void deplacer(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        double x = n.nombre("x");
        double y = n.nombre("y");
        objet.x = (float) x;
        objet.y = (float) y;
    }

    // Ajoute une poussée : le moteur la prend en compte à la prochaine image.
    private static void pousser(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        double x = n.nombre("x");
        double y = n.nombre("y");
        objet.intentionDeplacementX += (float) x;
        objet.intentionDeplacementY += (float) y;
    }

    // Écrit dans la variable en respectant son type (jamais de texte dans un nombre).
    private static void modifierVariable(NoeudGenerique n) {
        Variable variable = n.getCibleVariable();
        if (variable == null) return;
        String type = variable.type == null ? "" : variable.type;
        Object valeur;
        switch (type) {
            case "CHIFFRE":
            case "ENTIER":
                valeur = n.nombre("valeur");
                break;
            case "BOOLEEN":
                valeur = n.booleen("valeur");
                break;
            case "TEXTE":
                valeur = n.texte("valeur");
                break;
            default:
                return; // liste d'inventaire : pas modifiable par un nœud "Modifier variable"
        }
        variable.valeur = Evaluateur.convertirPourVariable(variable, valeur);
    }

    // Ajoute la valeur à la variable (addition pour un nombre, ajout à la suite pour un texte).
    private static void ajouterVariable(NoeudGenerique n) {
        Variable variable = n.getCibleVariable();
        if (variable == null) return;
        String type = variable.type == null ? "" : variable.type;
        Object resultat;
        switch (type) {
            case "CHIFFRE":
            case "ENTIER":
                resultat = Evaluateur.enNombre(Evaluateur.valeurDe(variable)) + n.nombre("valeur");
                break;
            case "TEXTE":
                resultat = Evaluateur.enTexte(Evaluateur.valeurDe(variable)) + n.texte("valeur");
                break;
            default:
                return;
        }
        variable.valeur = Evaluateur.convertirPourVariable(variable, resultat);
    }

    // Garde la variable (nombre) entre un minimum et un maximum.
    private static void limiterVariable(NoeudGenerique n) {
        Variable variable = n.getCibleVariable();
        if (variable == null) return;
        String type = variable.type == null ? "" : variable.type;
        if (!type.equals("CHIFFRE") && !type.equals("ENTIER")) return;
        double valeur = Evaluateur.enNombre(Evaluateur.valeurDe(variable));
        double min = n.nombre("min");
        double max = n.nombre("max");
        variable.valeur = Evaluateur.convertirPourVariable(variable, Math.max(min, Math.min(max, valeur)));
    }

    private static void toast(NoeudGenerique n) {
        if (NoeudBase.contexteApplication == null) return;
        Toast.makeText(NoeudBase.contexteApplication, n.texte("message"), Toast.LENGTH_SHORT).show();
    }

    // Modifie une propriété de l'objet (x, y, rotation, opacite, visible, texte...).
    private static void modifierPropriete(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        String brut = n.texteBrut("propriete");
        String propriete = ProprietesObjet.cle(brut);
        if (propriete == null) throw new IllegalStateException("Propriété inconnue : " + brut);
        if (propriete.equals("nom")) throw new IllegalStateException("Propriété en lecture seule : " + brut);
        Object valeur;
        switch (propriete) {
            case "texte":
            case "tag":
                valeur = n.texte("valeur");
                break;
            case "visible":
                valeur = n.booleen("valeur");
                break;
            default:
                valeur = n.nombre("valeur");
                break;
        }
        if (!ProprietesObjet.ecrire(objet, propriete, valeur)) {
            throw new IllegalStateException("Propriété en lecture seule : " + brut);
        }
    }
}
// bas 1
