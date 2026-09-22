// haut 1
package com.ludexa.moteur;

// Ce que font les nœuds Créer un objet, Combinaison d'objets et Si hors écran.
// Ils sont décrits dans assets/catalogue_noeuds.json.
//
// Une action retourne le nom du port de sortie à suivre, ou null pour la sortie normale.
public class ActionsObjets2 {

    private static long derniereAlerte = 0;

    private static void alerte(String nomNoeud, String message) {
        long maintenant = System.currentTimeMillis();
        if (maintenant - derniereAlerte < 2500) return;
        derniereAlerte = maintenant;
        String projet = NoeudBase.cheminProjetCourant;
        if (projet != null) DiagLogger.log(projet, "ALERTE " + nomNoeud + " : " + message);
    }

    // Nœud « Créer un objet » : clone l'objet choisi (cible A, le modèle) à la position donnée, dans la scène active.
    static String creerObjet(NoeudGenerique n) {
        ObjetBase modele = n.getCibleObjet();
        Scene scene = NoeudBase.sceneActiveCourante;
        if (modele == null) {
            alerte("creer_objet", "aucun objet modèle choisi (cible A)");
            return null;
        }
        if (scene == null || scene.objets == null) return null;
        ObjetBase clone = modele.clonerProfond();
        clone.id = java.util.UUID.randomUUID().toString();
        clone.x = (float) n.nombre("x");
        clone.y = (float) n.nombre("y");
        clone.zOrder = scene.prochainZOrder();
        scene.ajouterObjet(clone);
        return null;
    }

    // Nœud « Combinaison » : remplace les objets A et B par un clone de l'objet modèle (choisi par son nom,
    // dans la scène active), à la position de A. Si le modèle est introuvable, rien n'est détruit.
    static String combinaison(NoeudGenerique n) {
        ObjetBase objA = n.getCibleObjet();
        ObjetBase objB = n.getCibleObjetB();
        if (objA == null || objB == null) {
            alerte("combinaison", "objet A ou B manquant");
            return null;
        }
        Scene scene = NoeudBase.sceneActiveCourante;
        if (scene == null || scene.objets == null) return null;

        String nomResultat = n.texteBrut("resultat").trim();
        ObjetBase modele = null;
        for (ObjetBase o : scene.objets) {
            if (nomResultat.equals(o.nom)) { modele = o; break; }
        }
        if (modele == null) {
            alerte("combinaison", "objet résultat introuvable : « " + nomResultat + " »");
            return null;
        }

        ObjetBase clone = modele.clonerProfond();
        clone.id = java.util.UUID.randomUUID().toString();
        clone.x = objA.x;
        clone.y = objA.y;
        clone.visible = true;
        clone.zOrder = scene.prochainZOrder();
        scene.objets.add(clone);
        scene.objets.remove(objA);
        scene.objets.remove(objB);
        return null;
    }

    // Nœud « Si hors écran » : vrai si l'objet est entièrement hors de l'écran, au-delà d'une marge.
    static String siHorsEcran(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) {
            alerte("si_hors_ecran", "aucun objet choisi (cible A)");
            return "port_faux";
        }
        float marge = (float) n.nombre("marge");
        boolean horsX = (objet.x + objet.largeur < -marge) || (objet.x > ConfigurationJeu.LARGEUR_JEU + marge);
        boolean horsY = (objet.y + objet.hauteur < -marge) || (objet.y > ConfigurationJeu.HAUTEUR_JEU + marge);
        return (horsX || horsY) ? "port_vrai" : "port_faux";
    }
}
// bas 1
