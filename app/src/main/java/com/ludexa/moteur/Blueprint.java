// haut 1
package com.ludexa.moteur;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Blueprint {
    public List<NoeudBase> noeuds;
    public List<Lien> liens;
    
    public Map<String, Float> noeudsX;
    public Map<String, Float> noeudsY;

    public Blueprint() {
        this.noeuds = new ArrayList<>();
        this.liens = new ArrayList<>();
        this.noeudsX = new HashMap<>();
        this.noeudsY = new HashMap<>();
    }

    public void ajouterNoeud(NoeudBase noeud, float x, float y) {
        this.noeuds.add(noeud);
        this.noeudsX.put(noeud.id, x);
        this.noeudsY.put(noeud.id, y);
    }

    public void ajouterLien(NoeudBase depart, String portS, NoeudBase arrivee, String portE) {
        Lien l = new Lien(depart, portS, arrivee, portE);
        this.liens.add(l);
        depart.connecterPort(portS, arrivee, portE);
    }

    public static class Lien {
        public NoeudBase noeudDepart;
        public String portSortieNom;
        public NoeudBase noeudArrivee;
        public String portEntreeNom;
        
        public Lien(NoeudBase depart, String portS, NoeudBase arrivee, String portE) {
            this.noeudDepart = depart;
            this.portSortieNom = portS;
            this.noeudArrivee = arrivee;
            this.portEntreeNom = portE;
        }
    }

    public String toJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        BlueprintDTO dto = new BlueprintDTO();

        for (NoeudBase n : noeuds) {
            NoeudDTO ndto = new NoeudDTO();
            ndto.id = n.id;
            ndto.classeType = n.getClass().getName();
            
            if (n instanceof NoeudGenerique) ndto.cleNoeud = ((NoeudGenerique) n).cle;
            ndto.x = noeudsX.containsKey(n.id) ? noeudsX.get(n.id) : 0f;
            ndto.y = noeudsY.containsKey(n.id) ? noeudsY.get(n.id) : 0f;

            if (n.aDesParametresEditables() && n.getNomsParametres() != null) {
                for (String paramNom : n.getNomsParametres()) {
                    ndto.parametres.put(paramNom, n.getValeurParametre(paramNom));
                }
            }
            
            // CORRECTION BUG SAUVEGARDE CIBLE :
            // On lit nomCibleObjet DIRECTEMENT plutôt que de dépendre de getCibleObjet().
            // getCibleObjet() résout l'objet par réflexion (contexteApplication.sceneActive)
            // et peut échouer silencieusement selon le nœud, ce qui faisait perdre la cible
            // au moment même de la sauvegarde JSON (ex: NoeudActionClignotement).
            // nomCibleObjet, lui, est déjà fiable en mémoire dès la sélection dans l'éditeur.
            if (n.requiertCibleObjet()) {
                if (n.nomCibleObjet != null && !n.nomCibleObjet.isEmpty()) {
                    ndto.cibleNom = n.nomCibleObjet;
                } else if (n.getCibleObjet() != null) {
                    ndto.cibleNom = n.getCibleObjet().nom;
                }
            }
            if (n.requiertCibleObjetB()) {
                if (n.nomCibleObjetB != null && !n.nomCibleObjetB.isEmpty()) {
                    ndto.cibleNomB = n.nomCibleObjetB;
                } else if (n.getCibleObjetB() != null) {
                    ndto.cibleNomB = n.getCibleObjetB().nom;
                }
            }

            // CORRECTION BUG VARIABLE (même pattern que cibleNom/cibleNomB ci-dessus) :
            // AVANT : on appelait n.getCibleVariable() qui fait une résolution complexe
            // (scène active, réflexion sur contexteApplication) et peut échouer
            // silencieusement -> cibleVariableNom restait null dans le JSON, donc le lien
            // était perdu DES LA SAUVEGARDE, avant même le rechargement.
            // MAINTENANT : on lit nomCibleVariable directement (déjà fiable en mémoire
            // dès la sélection dans l'éditeur, voir NoeudBase.setCibleVariable()).
            if (n.requiertCibleVariable()) {
                if (n.nomCibleVariable != null && !n.nomCibleVariable.isEmpty()) {
                    ndto.cibleVariableNom = n.nomCibleVariable;
                } else if (n.getCibleVariable() != null) {
                    ndto.cibleVariableNom = n.getCibleVariable().nom;
                }
            }

            if (n.requiertCibleScene() && n.getCibleScene() != null) ndto.cibleSceneNom = n.getCibleScene().nom;

            for (Port p : n.portsEntree) {
                if (p.valeurSaisie != null && !p.valeurSaisie.isEmpty()) {
                    PortDTO pdto = new PortDTO();
                    pdto.nom = p.nom;
                    pdto.valeurSaisie = p.valeurSaisie;
                    ndto.portsEntree.add(pdto);
                }
            }
            dto.noeuds.add(ndto);
        }

        for (Lien l : liens) {
            LienDTO ldto = new LienDTO();
            ldto.idDepart = l.noeudDepart.id;
            ldto.portDepart = l.portSortieNom;
            ldto.idArrivee = l.noeudArrivee.id;
            ldto.portArrivee = l.portEntreeNom;
            // NOUVEAU : on note aussi la position des ports, qui ne change jamais avec la langue
            ldto.indexPortDepart = trouverIndexPort(l.noeudDepart.portsSortie, l.portSortieNom);
            ldto.indexPortArrivee = trouverIndexPort(l.noeudArrivee.portsEntree, l.portEntreeNom);
            dto.liens.add(ldto);
        }

        return gson.toJson(dto);
    }
// bas 1

// haut 2
    public static Blueprint fromJson(String json, Scene scene) {
        Gson gson = new Gson();
        BlueprintDTO dto = gson.fromJson(json, BlueprintDTO.class);
        Blueprint bp = new Blueprint();
        String cheminLog = NoeudBase.cheminProjetCourant;

        if (dto == null) {
            DiagLogger.log(cheminLog, "ERREUR script illisible (JSON invalide)");
            return bp;
        }

        Map<String, NoeudBase> dictionnaireNoeuds = new HashMap<>();

        for (NoeudDTO ndto : dto.noeuds) {
            try {
                NoeudBase n = FabriqueNoeuds.creerDepuisSauvegarde(ndto.classeType, ndto.cleNoeud);
                n.id = ndto.id;

                if (ndto.parametres != null) {
                    for (Map.Entry<String, String> entry : ndto.parametres.entrySet()) n.setValeurParametre(entry.getKey(), entry.getValue());
                }
                
                // CORRECTION BUG MEMOIRE CONTEXTUELLE :
                // Si le JSON contient le mot-clé, on le restaure directement sur nomCibleObjet
                // au lieu de chercher un objet physique du même nom dans la scène (qui n'existe pas).
                if ("__OBJET_IMPLIQUE__".equals(ndto.cibleNom)) {
                    n.setCibleObjet(null);
                    n.nomCibleObjet = "__OBJET_IMPLIQUE__";
                } else if (ndto.cibleNom != null) {
                    // CORRECTION : on restaure toujours le nom, même si l'objet physique
                    // n'est pas (encore) trouvable dans scene.objets à cet instant. Avant,
                    // si la recherche échouait, la cible était perdue silencieusement.
                    n.nomCibleObjet = ndto.cibleNom;
                    if (scene != null && scene.objets != null) {
                        for (ObjetBase obj : scene.objets) {
                            if (ndto.cibleNom.equals(obj.nom)) { n.setCibleObjet(obj); break; }
                        }
                    }
                }
                
                if ("__OBJET_IMPLIQUE__".equals(ndto.cibleNomB)) {
                    n.setCibleObjetB(null);
                    n.nomCibleObjetB = "__OBJET_IMPLIQUE__";
                } else if (ndto.cibleNomB != null) {
                    // CORRECTION : même filet de sécurité pour l'objet B
                    n.nomCibleObjetB = ndto.cibleNomB;
                    if (scene != null && scene.objets != null) {
                        for (ObjetBase obj : scene.objets) {
                            if (ndto.cibleNomB.equals(obj.nom)) { n.setCibleObjetB(obj); break; }
                        }
                    }
                }
                
                // CORRECTION BUG VARIABLE (chargement) :
                // AVANT : n.nomCibleVariable n'était affecté QUE si setCibleVariable() était
                // appelé, lui-même appelé QUE si cibleTrouvee != null. Si la variable était
                // une variable D'OBJET (ex: viealien) elle n'était jamais cherchée ici (seule
                // la scène et les globales l'étaient) -> cibleTrouvee restait null ->
                // n.nomCibleVariable restait null -> perte totale de la référence texte,
                // même si elle avait été correctement écrite dans le JSON.
                // MAINTENANT : le nom brut est restauré INCONDITIONNELLEMENT sur le champ,
                // exactement comme pour cibleNom/cibleNomB. La résolution de l'objet Variable
                // réel (cibleVariableResolue) reste une best-effort en plus, mais même si elle
                // échoue ici, NoeudBase.getCibleVariable() sait déjà retenter au runtime via
                // nomCibleVariable (recherche objet cible -> scène -> tous objets -> globales).
                if (ndto.cibleVariableNom != null && !ndto.cibleVariableNom.isEmpty()) {
                    n.nomCibleVariable = ndto.cibleVariableNom;

                    Variable cibleTrouvee = null;

                    // 1. Variables de l'objet ciblé par CE nœud (le cas "viealien" sur Alien)
                    ObjetBase objetCibleDuNoeud = n.getCibleObjet();
                    if (objetCibleDuNoeud != null && objetCibleDuNoeud.variablesLocales != null) {
                        for (Variable v : objetCibleDuNoeud.variablesLocales) {
                            if (ndto.cibleVariableNom.equals(v.nom)) { cibleTrouvee = v; break; }
                        }
                    }

                    // 2. Variables de la scène
                    if (cibleTrouvee == null && scene != null && scene.variablesLocales != null) {
                        for (Variable v : scene.variablesLocales) { if (ndto.cibleVariableNom.equals(v.nom)) { cibleTrouvee = v; break; } }
                    }

                    // 3. Fallback : n'importe quel objet de la scène (au cas où le nœud ne
                    // cible pas directement l'objet propriétaire de la variable)
                    if (cibleTrouvee == null && scene != null && scene.objets != null) {
                        for (ObjetBase obj : scene.objets) {
                            if (obj.variablesLocales == null) continue;
                            for (Variable v : obj.variablesLocales) {
                                if (ndto.cibleVariableNom.equals(v.nom)) { cibleTrouvee = v; break; }
                            }
                            if (cibleTrouvee != null) break;
                        }
                    }

                    // 4. Variables globales
                    if (cibleTrouvee == null && NoeudBase.contexteApplication != null) {
                        try {
                            java.lang.reflect.Field field = NoeudBase.contexteApplication.getClass().getField("variablesGlobales");
                            @SuppressWarnings("unchecked")
                            List<Variable> globales = (List<Variable>) field.get(NoeudBase.contexteApplication);
                            if (globales != null) {
                                for (Variable v : globales) { if (ndto.cibleVariableNom.equals(v.nom)) { cibleTrouvee = v; break; } }
                            }
                        } catch (Exception e) {}
                    }

                    if (cibleTrouvee != null) {
                        n.setCibleVariable(cibleTrouvee);
                        // setCibleVariable() réécrit nomCibleVariable avec cibleTrouvee.nom,
                        // ce qui est sans danger ici car c'est censé être la même valeur.
                    }
                }
                
                if (ndto.cibleSceneNom != null && NoeudBase.contexteApplication != null) {
                    try {
                        java.lang.reflect.Field field = NoeudBase.contexteApplication.getClass().getField("listeScenes");
                        @SuppressWarnings("unchecked")
                        List<Scene> scenes = (List<Scene>) field.get(NoeudBase.contexteApplication);
                        if (scenes != null) {
                            for (Scene s : scenes) { if (ndto.cibleSceneNom.equals(s.nom)) { n.setCibleScene(s); break; } }
                        }
                    } catch (Exception e) {}
                }

                for (PortDTO pdto : ndto.portsEntree) {
                    for (Port p : n.portsEntree) {
                        if (p.nom.equals(pdto.nom)) { p.valeurSaisie = pdto.valeurSaisie; break; }
                    }
                }

                bp.ajouterNoeud(n, ndto.x, ndto.y);
                dictionnaireNoeuds.put(n.id, n);
            } catch (Exception e) {
                DiagLogger.log(cheminLog, "ERREUR creation du noeud " + ndto.classeType + " : " + e);
            }
        }

        for (LienDTO ldto : dto.liens) {
            NoeudBase dep = dictionnaireNoeuds.get(ldto.idDepart);
            NoeudBase arr = dictionnaireNoeuds.get(ldto.idArrivee);
            if (dep != null && arr != null) {
                // Les noms de ports peuvent etre traduits : on retrouve le vrai port meme si la langue a change
                String portDep = resoudreNomPort(dep.portsSortie, ldto.portDepart, ldto.indexPortDepart);
                String portArr = resoudreNomPort(arr.portsEntree, ldto.portArrivee, ldto.indexPortArrivee);
                bp.ajouterLien(dep, portDep, arr, portArr);
            }
        }
        return bp;
    }

    public static Blueprint fromJson(String json) {
        return fromJson(json, null);
    }

    // Position d'un port dans sa liste (retrouve par son nom actuel). null si introuvable.
    private static Integer trouverIndexPort(List<Port> ports, String nom) {
        if (ports == null || nom == null) return null;
        for (int i = 0; i < ports.size(); i++) {
            if (nom.equals(ports.get(i).nom)) return i;
        }
        return null;
    }

    // Retrouve le nom ACTUEL d'un port, meme si le nom sauvegarde est dans une autre langue :
    // 1. par son nom exact (cas normal) ; 2. sinon par sa position sauvegardee ;
    // 3. sinon, s'il n'y a qu'un seul port de ce cote, celui-la (anciens projets).
    private static String resoudreNomPort(List<Port> ports, String nomSauvegarde, Integer indexSauvegarde) {
        if (ports == null || ports.isEmpty()) return nomSauvegarde;
        if (nomSauvegarde != null) {
            for (Port p : ports) {
                if (nomSauvegarde.equals(p.nom)) return nomSauvegarde;
            }
        }
        if (indexSauvegarde != null && indexSauvegarde >= 0 && indexSauvegarde < ports.size()) {
            return ports.get(indexSauvegarde).nom;
        }
        if (ports.size() == 1) return ports.get(0).nom;
        return nomSauvegarde;
    }

    private static class BlueprintDTO {
        List<NoeudDTO> noeuds = new ArrayList<>();
        List<LienDTO> liens = new ArrayList<>();
    }

    private static class NoeudDTO {
        String id;
        String classeType;
        String cleNoeud;
        float x;
        float y;
        List<PortDTO> portsEntree = new ArrayList<>();
        Map<String, String> parametres = new HashMap<>(); 
        String cibleNom; 
        String cibleNomB; 
        String cibleVariableNom; 
        String cibleSceneNom;
    }

    private static class PortDTO {
        String nom;
        String valeurSaisie;
    }

    private static class LienDTO {
        String idDepart;
        String portDepart;
        String idArrivee;
        String portArrivee;
        // NOUVEAU : positions des ports (Integer et non int : absent = null pour les anciens projets)
        Integer indexPortDepart;
        Integer indexPortArrivee;
    }
}
// bas 2


