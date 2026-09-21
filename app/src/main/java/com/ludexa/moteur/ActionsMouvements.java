// haut 1
package com.ludexa.moteur;

// Actions de mouvement et de comportement : avancer, poursuivre, fuir, arrêter, rester dans l'écran...
// (Appelées par ActionsMoteur ; chaque action lit ses champs avec n.nombre(...).)
public class ActionsMouvements {

    // Arrête net l'objet : plus de vitesse automatique, de poussée ni de saut en cours.
    static void arreter(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        objet.vitesseAvanceContinue = 0f;
        objet.vitessePoursuite = 0f;
        objet.intentionDeplacementX = 0f;
        objet.intentionDeplacementY = 0f;
        objet.vitesseY = 0f;
        objet.sautillementActif = false;
    }

    // Coupe les comportements automatiques : avance continue, poursuite et fuite.
    static void stopperMouvements(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        objet.vitesseAvanceContinue = 0f;
        objet.idCiblePoursuite = null;
        objet.vitessePoursuite = 0f;
        objet.fuiteActive = false;
    }

    // L'objet avance tout seul, dans la direction où il regarde, à cette vitesse.
    static void avancerContinu(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        objet.vitesseAvanceContinue = (float) n.nombre("vitesse");
    }

    // L'objet A poursuit l'objet B, ou le fuit quand le champ fixe "fuite" vaut true.
    static void poursuivre(NoeudGenerique n) {
        ObjetBase a = n.getCibleObjet();
        ObjetBase b = n.getCibleObjetB();
        if (a == null || b == null) return;
        double vitesse = n.nombre("vitesse");
        a.idCiblePoursuite = b.id;
        a.fuiteActive = "true".equals(n.texteBrut("fuite"));
        a.vitessePoursuite = (float) vitesse;
    }

    // Règle une barre de progression (seuls les objets de ce type sont concernés), entre son minimum et son maximum.
    static void definirProgression(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null || !"barre_progression".equals(objet.type)) return;
        ProprietesObjet.ecrire(objet, "progression", n.nombre("valeur"));
    }

    // Ce qui sort d'un côté de l'écran réapparaît du côté opposé.
    static void traverserEcran(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        if (objet.x > ConfigurationJeu.LARGEUR_JEU) {
            objet.x = -objet.largeur;
        } else if (objet.x + objet.largeur < 0) {
            objet.x = ConfigurationJeu.LARGEUR_JEU;
        }
        if (objet.y > ConfigurationJeu.HAUTEUR_JEU) {
            objet.y = -objet.hauteur;
        } else if (objet.y + objet.hauteur < 0) {
            objet.y = ConfigurationJeu.HAUTEUR_JEU;
        }
    }

    // L'objet ne peut pas sortir de l'écran ; la marge le retient un peu avant le bord.
    static void garderDansEcran(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        float marge = (float) n.nombre("marge");
        float minX = marge;
        float maxX = ConfigurationJeu.LARGEUR_JEU - objet.largeur - marge;
        float minY = marge;
        float maxY = ConfigurationJeu.HAUTEUR_JEU - objet.hauteur - marge;
        if (objet.x < minX) objet.x = minX;
        if (objet.x > maxX) objet.x = maxX;
        if (objet.y < minY) objet.y = minY;
        if (objet.y > maxY) objet.y = maxY;
    }

    // ------------------------------------------------------------------
    // PHYSIQUE ET MOUVEMENT
    // ------------------------------------------------------------------

    // Règle le rebond de l'objet (0 = ne rebondit pas, 1 = rebondit fort).
    static void changerRebond(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        objet.rebond = (float) n.nombre("rebond");
    }

    // Pousse l'objet dans une direction (angle en degrés : 0 = droite, 90 = bas) avec une force.
    static void forceAngle(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        double angle = Math.toRadians(n.nombre("angle"));
        double force = n.nombre("force");
        objet.intentionDeplacementX += (float) (Math.cos(angle) * force);
        objet.intentionDeplacementY += (float) (Math.sin(angle) * force);
    }

    // Projette l'objet vers le haut (valeur négative) ou vers le bas (valeur positive), et le rend libre.
    static void impulsion(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        objet.vitesseY = (float) n.nombre("force");
        objet.estStatique = false;
        objet.estPhysique = true;
    }

    // Rend l'objet physique. tombe = true : il tombe ; tombe = false : il reste fixe (comme un sol).
    static void activerPhysique(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        objet.estPhysique = true;
        objet.estStatique = !n.booleen("tombe");
    }

    // Autorise ou interdit de faire glisser l'objet avec le doigt.
    static void modifierDeplacable(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        objet.estDeplacable = n.booleen("deplacable");
    }

    // Petit rebond visuel. Une intensité de 0 ou moins l'arrête. "infini" : il continue tant que l'objet bouge.
    static void sautiller(NoeudGenerique n) {
        ObjetBase objet = n.getCibleObjet();
        if (objet == null) return;
        float intensite = (float) n.nombre("intensite");
        if (intensite <= 0f) {
            objet.sautillementActif = false;
            return;
        }
        objet.sautillementActif = true;
        objet.sautillementIntensite = intensite;
        objet.sautillementDureeMs = (long) n.nombre("duree");
        objet.sautillementInfiniMouvement = n.booleen("infini");
        objet.tempsDebutSautillement = System.currentTimeMillis();
    }

    // Tourne l'objet A vers l'objet B. Le décalage (degrés) corrige le sens de l'image (0 = elle regarde à droite).
    static void orienterVers(NoeudGenerique n) {
        ObjetBase a = n.getCibleObjet();
        ObjetBase b = n.getCibleObjetB();
        if (a == null || b == null) return;
        float dx = (b.x + b.largeur / 2f) - (a.x + a.largeur / 2f);
        float dy = (b.y + b.hauteur / 2f) - (a.y + a.hauteur / 2f);
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        a.rotation = (float) (angle + n.nombre("decalage"));
    }
}
// bas 1
