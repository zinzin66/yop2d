// haut 1
package com.ludexa.moteur;

import java.util.ArrayList;
import java.util.List;

public class MoteurPhysique {
    private static final float GRAVITE = 0.8f;
    private static final float VITESSE_MAX_CHUTE = 25f;
    private static final float SEUIL_MINIMUM_REBOND = 1.5f;

    // Retourne la liste des objets ayant subi un choc lors de cette frame
    public List<ObjetBase> mettreAJour(List<ObjetBase> objets) {
        List<ObjetBase> objetsEnChoc = new ArrayList<>();

        // Phase 1 : Application de la gravité
        for (ObjetBase obj : objets) {
            if (obj.estPhysique && !obj.estStatique) {
                
                // Application de la gravité modulée par l'échelle de l'objet
                obj.vitesseY += (GRAVITE * obj.graviteScale);
                
                if (obj.vitesseY > VITESSE_MAX_CHUTE) {
                    obj.vitesseY = VITESSE_MAX_CHUTE;
                }
                obj.y += obj.vitesseY;
            }
        }

        // Phase 2 : Résolution des collisions (ancrage au centre)
        for (ObjetBase dynamique : objets) {
            if (dynamique.estPhysique && !dynamique.estStatique) {
                
                float dynDemiHauteur = (dynamique.hauteur * Math.abs(dynamique.scaleY)) / 2f;
                boolean aEuUnChoc = false;

                for (ObjetBase statique : objets) {
                    if (dynamique == statique) continue;

                    if (statique.estPhysique && statique.estStatique) {
                        if (testerCollisionAABB(dynamique, statique)) {
                            
                            // On ne déclenche l'arrêt type plateforme que si la gravité est active
                            if (dynamique.vitesseY >= 0 && dynamique.graviteScale != 0f) {
                                float statCentreY = statique.y + (statique.hauteur / 2f);
                                float statDemiHauteur = (statique.hauteur * Math.abs(statique.scaleY)) / 2f;
                                float statHaut = statCentreY - statDemiHauteur;

                                dynamique.y = statHaut - (dynamique.hauteur / 2f) - dynDemiHauteur;

                                // Rebond ou arrêt
                                if (dynamique.vitesseY > SEUIL_MINIMUM_REBOND) {
                                    dynamique.vitesseY = -dynamique.vitesseY * dynamique.rebond;
                                    aEuUnChoc = true; // Impact fort (rebond)
                                } else {
                                    if (dynamique.vitesseY > 0.1f) {
                                        aEuUnChoc = true; // Impact faible (arrêt final)
                                    }
                                    dynamique.vitesseY = 0f;
                                }
                            }
                        }
                    }
                }

                // Phase 2bis : Résolution des collisions avec les Décors en tuiles (murs solides, 4 directions)
                for (ObjetBase autreObjet : objets) {
                    if (dynamique == autreObjet) continue;
                    if ("tile_layer".equals(autreObjet.type)) {
                        if (resoudreCollisionTuile(dynamique, autreObjet)) {
                            aEuUnChoc = true;
                        }
                    }
                }

                if (aEuUnChoc) {
                    objetsEnChoc.add(dynamique);
                }
            }
        }
        return objetsEnChoc;
    }

    // Teste et corrige la position d'un objet dynamique contre les cases solides d'un Décor en tuiles.
    // Repousse l'objet hors du mur, du côté où le chevauchement est le plus faible (axe le moins pénétré),
    // et applique un rebond (comme la collision objet-contre-objet) si la vitesse d'impact dépasse le seuil.
    // Prend en compte le scaleX/scaleY du Décor pour que la collision reste juste même si le Décor est
    // temporairement mis à l'échelle (l'éditeur remet normalement le scale à 1 après un redimensionnement).
    private boolean resoudreCollisionTuile(ObjetBase dynamique, ObjetBase tileLayer) {
        if (tileLayer.grilleTuiles == null || tileLayer.tuilesSolides == null) return false;

        float ltMonde = Math.max(1, tileLayer.largeurTuilePx) * Math.max(0.01f, Math.abs(tileLayer.scaleX));
        float htMonde = Math.max(1, tileLayer.hauteurTuilePx) * Math.max(0.01f, Math.abs(tileLayer.scaleY));
        boolean unChocDetecte = false;

        float dynDemiLargeur = (dynamique.largeur * Math.abs(dynamique.scaleX)) / 2f;
        float dynDemiHauteur = (dynamique.hauteur * Math.abs(dynamique.scaleY)) / 2f;

        float dCentreX = dynamique.x + dynamique.largeur / 2f;
        float dCentreY = dynamique.y + dynamique.hauteur / 2f;
        float dGauche = dCentreX - dynDemiLargeur;
        float dDroite = dCentreX + dynDemiLargeur;
        float dHaut = dCentreY - dynDemiHauteur;
        float dBas = dCentreY + dynDemiHauteur;

        int colMin = Math.max(0, (int) Math.floor((dGauche - tileLayer.x) / ltMonde) - 1);
        int colMax = Math.min(tileLayer.largeurGrille - 1, (int) Math.floor((dDroite - tileLayer.x) / ltMonde) + 1);
        int rowMin = Math.max(0, (int) Math.floor((dHaut - tileLayer.y) / htMonde) - 1);
        int rowMax = Math.min(tileLayer.hauteurGrille - 1, (int) Math.floor((dBas - tileLayer.y) / htMonde) + 1);

        for (int row = rowMin; row <= rowMax; row++) {
            if (row < 0 || row >= tileLayer.grilleTuiles.length || tileLayer.grilleTuiles[row] == null) continue;
            for (int col = colMin; col <= colMax; col++) {
                if (col < 0 || col >= tileLayer.grilleTuiles[row].length) continue;

                int indexTuile = tileLayer.grilleTuiles[row][col];
                if (indexTuile < 0) continue;
                if (indexTuile >= tileLayer.tuilesSolides.length || !tileLayer.tuilesSolides[indexTuile]) continue;

                float caseGauche = tileLayer.x + col * ltMonde;
                float caseDroite = caseGauche + ltMonde;
                float caseHaut = tileLayer.y + row * htMonde;
                float caseBas = caseHaut + htMonde;

                dCentreX = dynamique.x + dynamique.largeur / 2f;
                dCentreY = dynamique.y + dynamique.hauteur / 2f;
                dGauche = dCentreX - dynDemiLargeur;
                dDroite = dCentreX + dynDemiLargeur;
                dHaut = dCentreY - dynDemiHauteur;
                dBas = dCentreY + dynDemiHauteur;

                if (dGauche < caseDroite && dDroite > caseGauche && dHaut < caseBas && dBas > caseHaut) {
                    float chevauchementX = Math.min(dDroite, caseDroite) - Math.max(dGauche, caseGauche);
                    float chevauchementY = Math.min(dBas, caseBas) - Math.max(dHaut, caseHaut);

                    if (chevauchementX < chevauchementY) {
                        if (dCentreX < caseGauche + ltMonde / 2f) {
                            dynamique.x -= chevauchementX;
                        } else {
                            dynamique.x += chevauchementX;
                        }
                        if (Math.abs(dynamique.vitesseX) > SEUIL_MINIMUM_REBOND) {
                            dynamique.vitesseX = -dynamique.vitesseX * dynamique.rebond;
                        } else {
                            dynamique.vitesseX = 0f;
                        }
                    } else {
                        if (dCentreY < caseHaut + htMonde / 2f) {
                            dynamique.y -= chevauchementY;
                        } else {
                            dynamique.y += chevauchementY;
                        }
                        if (Math.abs(dynamique.vitesseY) > SEUIL_MINIMUM_REBOND) {
                            dynamique.vitesseY = -dynamique.vitesseY * dynamique.rebond;
                        } else {
                            dynamique.vitesseY = 0f;
                        }
                    }
                    unChocDetecte = true;
                }
            }
        }
        return unChocDetecte;
    }

    private boolean testerCollisionAABB(ObjetBase a, ObjetBase b) {
        float aCentreX = a.x + (a.largeur / 2f);
        float aCentreY = a.y + (a.hauteur / 2f);
        float aDemiLargeur = (a.largeur * Math.abs(a.scaleX)) / 2f;
        float aDemiHauteur = (a.hauteur * Math.abs(a.scaleY)) / 2f;

        float aGauche = aCentreX - aDemiLargeur;
        float aDroite = aCentreX + aDemiLargeur;
        float aHaut = aCentreY - aDemiHauteur;
        float aBas = aCentreY + aDemiHauteur;

        float bCentreX = b.x + (b.largeur / 2f);
        float bCentreY = b.y + (b.hauteur / 2f);
        float bDemiLargeur = (b.largeur * Math.abs(b.scaleX)) / 2f;
        float bDemiHauteur = (b.hauteur * Math.abs(b.scaleY)) / 2f;

        float bGauche = bCentreX - bDemiLargeur;
        float bDroite = bCentreX + bDemiLargeur;
        float bHaut = bCentreY - bDemiHauteur;
        float bBas = bCentreY + bDemiHauteur;

        return (aGauche < bDroite &&
                aDroite > bGauche &&
                aHaut < bBas &&
                aBas > bHaut);
    }
}
// bas 1
