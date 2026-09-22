// haut 1
package com.ludexa.moteur;

// Ce que font les nœuds Créer un objet, Combinaison d'objets et Si hors écran.
// Ils sont décrits dans assets/catalogue_noeuds.json.
// Le clonage et la destruction réutilisent ActionsObjets.creerClone() et ActionsObjets.detruireObjet(),
// les mêmes méthodes fiables que le reste du moteur (elles trouvent la bonne scène, HUD compris,
// et neutralisent l'objet avant de le retirer).
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

    // Nœud « Créer un objet » : clone l'objet choisi (cible A, le modèle) à la position donnée.
    static String creerObjet(NoeudGenerique n) {
        ObjetBase modele = n.getCibleObjet();
        if (modele == null) {
            alerte("creer_objet", "aucun objet modèle choisi (cible A)");
            return null;
        }
        ObjetBase clone = ActionsObjets.creerClone(modele);
        if (clone == null) {
            alerte("creer_objet", "aucune scène disponible pour créer l'objet");
            return null;
        }
        clone.x = (float) n.nombre("x");
        clone.y = (float) n.nombre("y");
        MoteurLogique.dernierObjetImplique = clone;
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

        float x = objA.x;
        float y = objA.y;
        ActionsObjets.detruireObjet(objA);
        ActionsObjets.detruireObjet(objB);

        ObjetBase clone = ActionsObjets.creerClone(modele);
        if (clone != null) {
            clone.x = x;
            clone.y = y;
            MoteurLogique.dernierObjetImplique = clone;
        }
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
