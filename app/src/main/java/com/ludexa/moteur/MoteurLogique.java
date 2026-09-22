// haut 1 02 09 2026 bis
package com.ludexa.moteur;

import java.lang.ref.WeakReference;
import java.util.List;

public class MoteurLogique {
    
    public static ObjetBase dernierObjetImplique = null;
    
    private Blueprint blueprintActif;
    private String cheminProjet;

    // Throttle pour éviter le spam de logs à chaque frame (cause de plantage)
    private long dernierLogCollisionCheck = 0;

    // Les moteurs en vie (celui de la scène, celui du HUD) : sert à retrouver le script d'un nœud pour « Appeler un événement ».
    // Références faibles : un moteur abandonné (changement de scène) peut être libéré normalement.
    private static final java.util.List<WeakReference<MoteurLogique>> moteursVivants = new java.util.ArrayList<>();

    public MoteurLogique(Blueprint blueprint) {
        this.blueprintActif = blueprint;
        purgerMoteursLibres();
        moteursVivants.add(new WeakReference<>(this));
    }

    public void setCheminProjet(String cheminProjet) {
        this.cheminProjet = cheminProjet;
    }

    private void logDiag(String message) {
        if (cheminProjet != null) DiagLogger.log(cheminProjet, message);
    }
    
    // NOUVEAU : Méthodes pour injecter et nettoyer la logique des Prefabs dynamiquement
    public void ajouterNoeudAuBlueprintActif(NoeudBase noeud) {
        if (blueprintActif != null && blueprintActif.noeuds != null) {
            blueprintActif.noeuds.add(noeud);
        }
    }
    
    public void nettoyerNoeudsParTag(String tag) {
        if (blueprintActif == null || blueprintActif.noeuds == null || tag == null) return;
        java.util.Iterator<NoeudBase> it = blueprintActif.noeuds.iterator();
        while (it.hasNext()) {
            NoeudBase n = it.next();
            if (n.categorie != null && n.categorie.contains(tag)) {
                it.remove();
            }
        }
    }
    // --------------------------------------------------------------------------------

    // --------------------------------------------------------------------------------
    // ÉVÉNEMENTS LOCAUX : « Appeler un événement » retrouve le script du nœud appelant, puis l'événement par son nom.
    // --------------------------------------------------------------------------------

    private static void purgerMoteursLibres() {
        java.util.Iterator<WeakReference<MoteurLogique>> it = moteursVivants.iterator();
        while (it.hasNext()) {
            if (it.next().get() == null) it.remove();
        }
    }

    // Le moteur dont le script contient ce nœud (le même objet, pas une copie) ; null si aucun.
    private static MoteurLogique moteurDe(NoeudBase noeud) {
        if (noeud == null) return null;
        for (WeakReference<MoteurLogique> ref : new java.util.ArrayList<>(moteursVivants)) {
            MoteurLogique moteur = ref.get();
            if (moteur == null || moteur.blueprintActif == null || moteur.blueprintActif.noeuds == null) continue;
            for (NoeudBase n : new java.util.ArrayList<>(moteur.blueprintActif.noeuds)) {
                if (n == noeud) return moteur;
            }
        }
        return null;
    }

    // Noms des événements locaux du script qui contient ce nœud ; null si on ne retrouve pas ce script
    // (par exemple un nœud placé dans une fonction).
    public static java.util.List<String> nomsEvenementsLocaux(NoeudBase appelant) {
        MoteurLogique moteur = moteurDe(appelant);
        if (moteur == null) return null;
        java.util.List<String> noms = new java.util.ArrayList<>();
        for (NoeudBase n : new java.util.ArrayList<>(moteur.blueprintActif.noeuds)) {
            if (n instanceof NoeudEventPersonnalise) noms.add(((NoeudEventPersonnalise) n).getNomEvenement());
        }
        return noms;
    }

    // Lance l'événement local de ce nom, dans le même script que le nœud appelant.
    // Les majuscules et les espaces autour du nom sont ignorés. Retourne false si le script ou l'événement est introuvable.
    public static boolean appelerEvenementLocal(NoeudBase appelant, String nom) {
        MoteurLogique moteur = moteurDe(appelant);
        if (moteur == null || nom == null) return false;
        String voulu = nom.trim();
        for (NoeudBase n : new java.util.ArrayList<>(moteur.blueprintActif.noeuds)) {
            if (n instanceof NoeudEventPersonnalise) {
                String nomN = ((NoeudEventPersonnalise) n).getNomEvenement();
                if (nomN != null && voulu.equalsIgnoreCase(nomN.trim())) {
                    n.executer();
                    return true;
                }
            }
        }
        return false;
    }
    // --------------------------------------------------------------------------------

    public void executerDemarrage() {
        if (blueprintActif == null || blueprintActif.noeuds == null) return;
        java.util.List<NoeudBase> copieNoeuds = new java.util.ArrayList<>(blueprintActif.noeuds);
        for (NoeudBase noeud : copieNoeuds) {
            if (noeud instanceof NoeudEventStart) {
                noeud.executer();
            }
        }
    }

    public void executerEvenement(Class<? extends NoeudBase> typeEvenement) {
        if (blueprintActif == null || blueprintActif.noeuds == null) return;
        java.util.List<NoeudBase> copieNoeuds = new java.util.ArrayList<>(blueprintActif.noeuds);
        for (NoeudBase noeud : copieNoeuds) {
            if (typeEvenement.isInstance(noeud)) {
                noeud.executer();
            }
        }
    }

    public void executerEvenementSurObjet(Class<? extends NoeudBase> typeEvenement, ObjetBase objetTouche) {
        if (blueprintActif == null || blueprintActif.noeuds == null || objetTouche == null) return;
        java.util.List<NoeudBase> copieNoeuds = new java.util.ArrayList<>(blueprintActif.noeuds);
        for (NoeudBase noeud : copieNoeuds) {
            if (typeEvenement.isInstance(noeud) && noeud.requiertCibleObjet()) {
                ObjetBase cible = noeud.getCibleObjet();
                if (cible != null && cible.id.equals(objetTouche.id)) {
                    noeud.executer();
                }
            }
        }
    }

    public void verifierCollisions(VueJeu vueJeu, List<ObjetBase> objetsContexte) {
        if (blueprintActif == null || blueprintActif.noeuds == null || objetsContexte == null) return;

        long tempsActuel = System.currentTimeMillis();
        boolean doitLoguerCetteFrame = (tempsActuel - dernierLogCollisionCheck > 500);
        if (doitLoguerCetteFrame) {
            dernierLogCollisionCheck = tempsActuel;
        }
        
        java.util.List<NoeudBase> copieNoeuds = new java.util.ArrayList<>(blueprintActif.noeuds);
        for (NoeudBase noeud : copieNoeuds) {
            if (noeud instanceof NoeudEventCollisionAB) {
                NoeudEventCollisionAB noeudCol = (NoeudEventCollisionAB) noeud;
                ObjetBase objA = noeudCol.getCibleObjet();
                ObjetBase objB = noeudCol.getCibleObjetB();
                
                if (objA != null && objB != null) {
                    boolean enCollision = UtilCollision.rectanglesSeChevauchent(objA, objetsContexte, objB, objetsContexte, vueJeu);
                    
                    if (enCollision && !noeudCol.isEtaitEnCollision()) {
                        noeudCol.setEtaitEnCollision(true);
                        noeudCol.executer();
                    } else if (!enCollision && noeudCol.isEtaitEnCollision()) {
                        noeudCol.setEtaitEnCollision(false);
                    }
                }
            } else if (noeud instanceof NoeudEventSortieZone) {
                NoeudEventSortieZone noeudSortie = (NoeudEventSortieZone) noeud;
                ObjetBase objA = noeudSortie.getCibleObjet();
                ObjetBase objB = noeudSortie.getCibleObjetB();
                
                if (objA != null && objB != null) {
                    boolean enCollision = UtilCollision.rectanglesSeChevauchent(objA, objetsContexte, objB, objetsContexte, vueJeu);
                    
                    if (enCollision && !noeudSortie.isEtaitEnCollision()) {
                        noeudSortie.setEtaitEnCollision(true);
                    } else if (!enCollision && noeudSortie.isEtaitEnCollision()) {
                        noeudSortie.setEtaitEnCollision(false);
                        noeudSortie.executer();
                    }
                }
            } 
            else if (noeud instanceof NoeudEventCollisionTag) {
                NoeudEventCollisionTag noeudTag = (NoeudEventCollisionTag) noeud;
                ObjetBase objA = noeudTag.getCibleObjet();
                String cibleTag = noeudTag.getTagCible();

                if (doitLoguerCetteFrame) {
                    logDiag("COLLISION_TAG_CHECK noeud=" + noeud.id
                            + " objA=" + (objA != null ? (objA.nom + " id=" + objA.id) : "NULL")
                            + " cibleTag='" + cibleTag + "'");
                }

                if (objA != null && cibleTag != null && !cibleTag.trim().isEmpty()) {

                    java.util.Set<String> idsEnCollisionCetteFrame = new java.util.HashSet<>();
                    java.util.List<ObjetBase> objetsADeclencher = new java.util.ArrayList<>();
                    java.util.List<ObjetBase> copieContexte = new java.util.ArrayList<>(objetsContexte);

                    for (ObjetBase objB : copieContexte) {
                        if (objA != objB && objB.tag != null && cibleTag.trim().equalsIgnoreCase(objB.tag.trim())) {
                            if (UtilCollision.rectanglesSeChevauchent(objA, objetsContexte, objB, objetsContexte, vueJeu)) {
                                idsEnCollisionCetteFrame.add(objB.id);

                                if (!noeudTag.isEnCollisionAvec(objB.id)) {
                                    noeudTag.marquerEnCollision(objB.id);
                                    objetsADeclencher.add(objB);
                                }
                            }
                        }
                    }

                    java.util.Iterator<String> it = noeudTag.getObjetsEnCollisionActuels().iterator();
                    while (it.hasNext()) {
                        String idSuivi = it.next();
                        if (!idsEnCollisionCetteFrame.contains(idSuivi)) {
                            logDiag("COLLISION_TAG FIN_CONTACT: id=" + idSuivi + " tag=" + cibleTag);
                            it.remove();
                        }
                    }

                    for (ObjetBase objB : objetsADeclencher) {
                        MoteurLogique.dernierObjetImplique = objB;
                        logDiag("COLLISION_TAG DECLENCHEMENT: objA=" + objA.nom
                            + " objB=" + objB.nom + " (id=" + objB.id + ") -> executer()");
                        try {
                            noeudTag.executer();
                        } catch (Exception e) {
                            logDiag("ERREUR COLLISION_TAG executer(): " + e.toString());
                        }
                    }
                }
            }
        }
    }

    public void verifierVariablesChangees() {
        if (blueprintActif == null || blueprintActif.noeuds == null) return;
        
        java.util.List<NoeudBase> copieNoeuds = new java.util.ArrayList<>(blueprintActif.noeuds);
        for (NoeudBase noeud : copieNoeuds) {
            if (noeud instanceof NoeudEventVariableChange) {
                ((NoeudEventVariableChange) noeud).verifierEtDeclencher();
            }
        }
    }
}
// bas 1
