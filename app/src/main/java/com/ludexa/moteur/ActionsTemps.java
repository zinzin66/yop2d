// haut 1
package com.ludexa.moteur;

// Ce que font les nœuds de la catégorie Temps : Attendre, Répéter, Compte à rebours, Arrêter un minuteur,
// Vitesse du jeu (ralenti), Pause et Reprendre. Les minuteurs eux-mêmes vivent dans HorlogeJeu.
//
// Une action retourne le nom du port de sortie à suivre, ou null pour la sortie normale.
public class ActionsTemps {

    // Nom de port qui n'existe pas : le nœud ne continue pas tout de suite (la suite viendra plus tard, par un minuteur).
    static final String AUCUNE_SUITE = "aucune_suite";

    // Lit un champ en nombre. La virgule est acceptée comme séparateur : 0,25 vaut 0.25.
    private static double nombreTolerant(NoeudGenerique n, String champ) {
        String brut = n.texteBrut(champ).trim();
        if (brut.matches("-?\\d+,\\d+")) brut = brut.replace(',', '.');
        return Evaluateur.nombre(brut, n.getCibleObjet());
    }

    // Clé d'un minuteur : son nom s'il en a un, sinon le nœud lui-même.
    private static String cle(NoeudGenerique n) {
        String nom = n.texte("nom").trim();
        return nom.isEmpty() ? "@" + n.id : nom;
    }

    // Attendre : la suite est jouée après la durée donnée.
    static String attendre(NoeudGenerique n) {
        double duree = nombreTolerant(n, "duree");
        if (duree <= 0) return null;   // pas d'attente : on continue tout de suite
        HorlogeJeu.Minuteur m = new HorlogeJeu.Minuteur();
        m.noeud = n;
        m.type = "attendre";
        HorlogeJeu.planifier(m, duree);
        return AUCUNE_SUITE;
    }

    // Répéter : "Tout de suite" part immédiatement, puis "À chaque fois" toutes les X secondes, puis "Terminé".
    // Si ce minuteur tourne déjà, le nœud ne fait rien (pas de doublon, même placé sous « À chaque image »).
    static String repeter(NoeudGenerique n) {
        String cle = cle(n);
        if (HorlogeJeu.trouver(cle) != null) return AUCUNE_SUITE;
        double delai = Math.max(0.02, nombreTolerant(n, "delai"));
        int fois = (int) Math.round(nombreTolerant(n, "repetitions"));
        HorlogeJeu.Minuteur m = new HorlogeJeu.Minuteur();
        m.cle = cle;
        m.noeud = n;
        m.type = "repeter";
        m.intervalle = delai;
        m.restant = fois > 0 ? fois : -1;
        HorlogeJeu.planifier(m, delai);
        return "port_tout_de_suite";
    }

    // Compte à rebours : la variable baisse de 1 chaque seconde, jusqu'à 0.
    static String compteARebours(NoeudGenerique n) {
        Variable v = n.getCibleVariable();
        if (v == null) return AUCUNE_SUITE;   // l'alerte « aucune variable choisie » est déjà dans le journal
        String type = v.type == null ? "" : v.type;
        if (!type.equals("CHIFFRE") && !type.equals("ENTIER")) {
            throw new IllegalStateException("Compte à rebours : la variable « " + v.nom + " » doit être un nombre (Chiffre ou Entier)");
        }
        String cle = cle(n);
        if (HorlogeJeu.trouver(cle) != null) return AUCUNE_SUITE;
        if (!n.texteBrut("depart").trim().isEmpty()) {
            v.valeur = Evaluateur.convertirPourVariable(v, nombreTolerant(n, "depart"));
        }
        if (Evaluateur.enNombre(Evaluateur.valeurDe(v)) <= 0) return "port_termine";
        HorlogeJeu.Minuteur m = new HorlogeJeu.Minuteur();
        m.cle = cle;
        m.noeud = n;
        m.type = "compte";
        m.intervalle = 1.0;
        HorlogeJeu.planifier(m, 1.0);
        return AUCUNE_SUITE;
    }

    // Arrêter un minuteur : celui qui porte ce nom, ou tous s'il n'y a pas de nom.
    static String arreterMinuteur(NoeudGenerique n) {
        HorlogeJeu.arreter(n.texte("nom").trim());
        return null;
    }

    // Vitesse du jeu : 1 = normal, 0.5 = ralenti, 2 = accéléré (de 0 à 4).
    static String vitesseJeu(NoeudGenerique n) {
        HorlogeJeu.definirVitesse((float) nombreTolerant(n, "vitesse"));
        return null;
    }

    static String pause(boolean enPause) {
        HorlogeJeu.mettreEnPause(enPause);
        return null;
    }
}
// bas 1
