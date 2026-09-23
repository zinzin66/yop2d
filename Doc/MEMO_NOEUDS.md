# Yop2D : créer et tester un nœud

## 1. Principe
- Actions et conditions : décrites dans `assets/catalogue_noeuds.json`. Du Java seulement si le nœud apporte un comportement nouveau.
- Événements : restent des classes Java (`NoeudEvent...`), listées dans `RegistreNoeuds.java`. Jamais migrés vers le catalogue (choix de départ, pas un oubli).
- Ordre de livraison : (1) fonction Java, (2) une ligne `case` dans `ActionsMoteur.java` (livrée une seule fois par famille, pas à chaque nœud), (3) bloc JSON dans le catalogue, puis build, puis test.

## 2. Fichiers et rôles
- **ActionsMoteur.java** : le switch de toutes les actions.
- **Actions\*.java** : un fichier par famille (ActionsObjets, ActionsMouvements, ActionsScene, ActionsTemps, ActionsControles, ActionsAnimations, ActionsAudio, ActionsAppels, ActionsLogique, ActionsInventaire, ActionsObjets2, ActionsPartie, ActionsOutils).
- **CatalogueNoeuds.java** : lit le catalogue (`analyser()` : `new JSONObject(texte)`, sans rien retirer — Android tolère nativement les `//` dans le JSON, donc les blocs `// haut N` / `// bas N` ne posent aucun problème).
- **NoeudGenerique.java** : représente un nœud du catalogue. `this.nom = definition.cleNom` (une CLÉ brute, jamais un texte déjà traduit — c'est `CanvasBlueprint` qui appelle `Traducteur.get(noeud.nom)` au moment d'afficher). Fait suivre automatiquement une sortie "ensuite" déclarée dans le catalogue, après la sortie choisie (jamais si le nœud attend, ex Attendre/Répéter).
- **Evaluateur.java** : les formules. **ProprietesObjet.java** : ce qu'on lit après un point (`player.x`).
- **Traducteur.java** : `get(cle)` cherche (1) `lang_XX.json`, (2) une table "secours" alimentée en mémoire par `CatalogueNoeuds` via `ajouterSecours()`, (3) une liste de textes français en dur. Si rien ne correspond : affiche `[la_cle]`.
- **ActionsObjets.java** : `detruireObjet()` et `creerClone()` sont les méthodes FIABLES à réutiliser pour tout nœud qui détruit ou clone un objet — ne jamais manipuler `scene.objets` à la main (bug vécu : échec silencieux, rien détruit, aucune erreur).

## 3. Format d'un nœud du catalogue
- `"cle"` (interne, unique, jamais traduite), `"nom"` (9 langues : fr en es pt de it ru zh ja) **OU** `"nomCle"` (réutilise une clé de traduction déjà existante), `"categorie"`, `"action"` (par défaut : la clé).
- `"objet": true` = cible objet A ; `"objetB": true` = cible B ; `"variable": true` ; `"scene": true`.
- `"sorties"` : par défaut une sortie `"suivant"` ; `[]` = aucune ; sortie nommée `{"cle":"haut","nom":{...}}`. Le port s'appelle `"port_"` + clé. Ajouter `{"cle":"ensuite","nom":{...}}` à la fin d'une liste de sorties donne automatiquement le comportement "Ensuite" (suivi après la sortie choisie), sans rien coder de plus dans le nœud lui-même.
- `"constantes"` : champs fixes, lus en Java avec `n.texteBrut("cle")`. Permet à une même action de servir plusieurs nœuds (ex `pause_jeu` / `reprendre_jeu` → action `"pause"`).
- `"champs"` : `{"cle","type","defaut","nom"}`. Types : `formule`, `texte`, `choix` (+ `"options"`), `couleur`, `image`, `son`, `dialogue`, `fonction`, `tag`, `animation`.
- Un champ true/false : type `"formule"`.
- Une clé ne doit exister qu'UNE fois (doublon = toast d'erreur au démarrage).
- Nombre de sorties FIXE, décidé à la conception (pas de sorties dynamiques selon ce que l'utilisateur tape). Pour un nœud "Selon la valeur" à choix multiple, mieux vaut un nombre de cas raisonnable (6 s'est avéré un bon compromis) que d'essayer d'être illimité ; au-delà, on chaîne un second nœud sur la sortie "Aucun".

## 4. Écrire la fonction Java
- `static String maFonction(NoeudGenerique n)` : retourne `null` (sortie normale), `"port_vrai"`/`"port_faux"`/`"port_<cle>"`, ou `ActionsTemps.AUCUNE_SUITE` (ne continue pas, ex Séquence).
- Lire : `n.nombre("k")`, `n.booleen("k")`, `n.texte("k")`, `n.texteBrut("k")` (les 3 premiers acceptent les formules), `n.getCibleObjet()`, `n.getCibleObjetB()`, `n.getCibleVariable()`.
- Pour détruire/cloner un objet : passer par `ActionsObjets.detruireObjet(objet)` / `ActionsObjets.creerClone(modele)`. Jamais `scene.objets.remove()`/`add()` à la main.
- Scène courante : `NoeudBase.sceneActiveCourante`. HUD : `NoeudBase.sceneHudActiveCourante`. Variables globales : `NoeudBase.getVariablesGlobalesDisponibles()`.
- Ne jamais dessiner ou animer avec l'heure réelle : utiliser `HorlogeJeu.tempsJeu` (suit pause et ralenti). Modèle à suivre pour une transition en douceur : `DeplacementsGlisses.java` / `FondusEnCours.java` (une liste d'"en cours", avancée une fois par image dans `VueJeu.onDraw` juste après `HorlogeJeu.imageSuivante()`).
- Pas de Toast de diagnostic : `DiagLogger.log(cheminProjet, "message")` pour la fenêtre de debug. Alertes utilisateur : préfixe `"ALERTE"`, throttlées (2,5 s) pour ne pas spammer un nœud placé sous "À chaque image".

## 5. Formules disponibles
- `temps`, `vitesse`, `ecran.largeur`/`hauteur`, `doigt.x`/`y`/`appuye`, `joystick.x`/`y`/`force`/`angle`/`actif`, `camera.x`/`y`.
- `chevauche(a,b)`, `plus_proche(a,"tag")`, `au_hasard("tag")` — le résultat d'une fonction peut être suivi de `.propriete` (ex `plus_proche(player,"ennemi").nom`), capacité générale de l'analyseur.
- Fonctions : `random abs sqrt sin cos round floor ceil int min max clamp distance angle`.
- Propriétés d'objet : `x y largeur hauteur rotation opacite visible echelleX echelleY vitesseX vitesseY texte tag nom progression z touche animation image animee`.
- Un champ "formule" avec une variable dedans doit commencer par `=` (ex `"=etat"`, pas `"etat"` tout court, sinon comparaison au mot littéral).

## 6. Méthode de test
- Script JSON complet `{"noeuds":[...],"liens":[...]}` collé avec le bouton `{ }`. Jamais un fragment.
- La cible (objet/variable/scène) N'EST JAMAIS un champ dans `"parametres"` : ce sont des clés séparées au niveau du nœud — `"cibleNom"` (objet A), `"cibleNomB"` (objet B), `"cibleVariableNom"`, `"cibleSceneNom"`.
- Nœud : `{"id":"a1","classeType":"com.ludexa.moteur.NoeudGenerique","cleNoeud":"<cle>","parametres":{"<cle>.<champ>":"valeur"},"cibleNom":"player"}`.
- Événements (pas de `cleNoeud`, `classeType` = nom de la classe Java) : `NoeudEventStart`, `NoeudEventChaqueImage`, `NoeudEventClicObjet` (cibleNom), `NoeudEventFinClicObjet`, `NoeudEventMaintenuObjet`, `NoeudEventDoigtAppuye`, `NoeudEventFinClic`, `NoeudEventCollisionAB` (cibleNom+cibleNomB, port de sortie `"Collision"`), `NoeudEventCollisionTag` (port `"Sortie"`, paramètre top-level `"Tag"` avec majuscule, PAS de format pointé).
- Lien : `{"idDepart":"a1","idArrivee":"a2","indexPortDepart":0,"indexPortArrivee":0}`. `indexPortDepart`/`portDepart` PEUVENT ÊTRE OMIS pour un nœud à sortie/entrée unique (résolu automatiquement à l'index 0) ; à préciser seulement pour un nœud à plusieurs sorties nommées (`"port_vrai"`/`"port_faux"`, `"port_1"`..`"port_6"`/`"port_aucun"`/`"port_ensuite"`...).
- Un texte est une formule s'il commence par `=`. Le nœud `journal` (champ `journal.message`) sert de juge.
- Avant chaque test : lister les objets, variables, HUD à créer ET SAUVEGARDER.
- Un test = une chose, une preuve dans le journal OU une description précise de l'écran. Ne jamais conclure "validé" sans cette preuve.
- Pour un mécanisme jamais testé ensemble (collision par tag, prefabs...) : le développeur construit d'abord lui-même un petit exemple isolé et fonctionnel dans l'app, le décrit ; Claude s'en sert comme modèle prouvé pour généraliser, plutôt que de deviner d'emblée.

## 7. Pièges déjà rencontrés
- Clé inventée à vérifier : `"pause"` n'existe pas (`pause_jeu`, `reprendre_jeu`).
- Variable créée mais pas sauvegardée avec la scène : "variable introuvable".
- Un clic sur un objet du HUD se met dans le script du HUD, pas celui de la scène ; il faut `ouvrir_hud` au démarrage pour que ses objets/boutons s'affichent.
- Objet physique : il tombe par défaut (`activer_physique` `tombe=false` pour le figer).
- Les animations avancent à l'heure réelle : pause et ralenti ne les figent pas (chantier à part, pas commencé).
- Un champ "formule" contenant une variable doit commencer par `=` pour être évalué (sinon comparaison au mot littéral).
- Doublon de clé dans le catalogue : toast au démarrage.
- Cible mise par erreur comme un champ pointé (ex `"modifier_propriete.objet"`) : silencieusement ignorée — toujours une clé séparée (`cibleNom`, etc.).
- Un événement de collision (CollisionAB ou CollisionTag) se redéclenche à chaque image tant que le contact dure, pas une seule fois à l'entrée en contact — prévoir une variable "verrou" pour éviter les déclenchements multiples.

## 8. Fichiers à fournir en début de conversation

**Systématiquement** (base commune à presque tout) :
- Ce mémo
- `catalogue_noeuds.json` (à jour)
- `ActionsMoteur.java`
- `NoeudGenerique.java`

**Selon le sujet abordé** :
- Un nouveau nœud qui lit/écrit une formule, une propriété d'objet → `Evaluateur.java`, `ProprietesObjet.java`
- Un nœud qui détruit, clone ou crée un objet → `ActionsObjets.java` (`detruireObjet`, `creerClone`)
- Un nœud qui cible une scène, change de scène, ouvre un HUD → `ActionsScene.java`, `VueJeu.java`
- Un problème de traduction / affichage de nom → `Traducteur.java`, `RegistreNoeuds.java`, `CatalogueNoeuds.java`
- Un nœud d'ÉVÉNEMENT (pas une action/condition) → `NoeudBase.java` + la classe `NoeudEvent*` concernée
- Un doute sur le format exact d'un nœud déjà migré (champs, sorties) → le bloc correspondant dans `catalogue_noeuds.json` (chercher la `"cle"` dans le fichier)
- Migration d'une famille encore en Java → les anciens fichiers `NoeudAction*`/`NoeudCondition*` de cette famille

Fichier jamais vu qui reviendrait souvent → le demander une fois, puis le noter ici pour ne plus le redemander.

## 9. État du projet
- Migration du catalogue : terminée (97 nœuds, toutes les familles migrées + lot des "si" ciblés).
- Nettoyage : 1ère partie faite (37 anciens fichiers Java supprimés). Restent : toasts du Play, anciens diagnostics VueJeu (`"JOYSTICK cible=..."`).
- Bug d'affichage `[clé]` au lieu du texte traduit : RÉSOLU (lecture de `lang_fr.json` dans `Traducteur.java` corrigée : lecture par blocs en boucle au lieu d'un seul `read()`, qui pouvait tronquer silencieusement le fichier).
- Prochain chantier : mini-tutoriels téléchargeables. Pipeline de génération de projet (zip) validé. Décision : pour un tutoriel simple, pas de HUD — tout dans une seule scène (le HUD n'est utile que si la caméra bouge). Pour une rangée d'ennemis identiques, les prefabs sont adaptés (collision sur clones de prefab corrigée côté moteur). Le nœud "Tirer" existe dans le catalogue (`modele`/`direction`/`vitesse`) — direction : options numériques `"0"`/`"90"`/`"180"`/`"270"` (0=droite, 90=bas, 180=gauche, 270=haut), plus `"Vers la cible"` et `"Angle de départ"` — à utiliser directement plutôt qu'à réinventer.
- Cible spéciale `"cibleNom": "__OBJET_IMPLIQUE__"` : après un événement de collision, cible directement l'objet précis impliqué (utile pour détruire l'ennemi touché sans connaître son nom à l'avance, notamment avec des clones de prefab).
