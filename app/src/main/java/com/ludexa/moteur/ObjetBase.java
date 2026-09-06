public ObjetBase clonerProfond() {
    ObjetBase copie = new ObjetBase();

    // IMPORTANT : ne pas recopier l'ID du modèle.
    // Le constructeur de ObjetBase() a déjà créé un nouvel ID unique.

    copie.nom = this.nom;
    copie.x = this.x;
    copie.y = this.y;
    copie.largeur = this.largeur;
    copie.hauteur = this.hauteur;
    copie.zOrder = this.zOrder;
    copie.visible = this.visible;
    copie.estDeplacable = this.estDeplacable;
    copie.estVerrouille = this.estVerrouille;
    copie.estRamassable = this.estRamassable;
    copie.estZoneDeClic = this.estZoneDeClic;
    copie.couleur = this.couleur;
    copie.cheminImage = this.cheminImage;
    copie.cheminImagePresse = this.cheminImagePresse;
    copie.cheminImageDesactive = this.cheminImageDesactive;
    copie.estDesactive = this.estDesactive;
    copie.cibleJoystickId = this.cibleJoystickId;
    copie.sceneLieeId = this.sceneLieeId;

    copie.surchargesVariables = new HashMap<>(this.surchargesVariables);

    copie.filtreCouleur = this.filtreCouleur;
    copie.clignotementActif = this.clignotementActif;
    copie.clignotementVitesseMs = this.clignotementVitesseMs;
    copie.clignotementDureeTotalMs = this.clignotementDureeTotalMs;
    copie.tempsDebutClignotement = this.tempsDebutClignotement;
    copie.etatVisibleClignotement = this.etatVisibleClignotement;

    copie.surbrillanceActive = this.surbrillanceActive;
    copie.couleurSurbrillance = this.couleurSurbrillance;

    copie.vitesseAvanceContinue = this.vitesseAvanceContinue;
    copie.idCiblePoursuite = this.idCiblePoursuite;
    copie.vitessePoursuite = this.vitessePoursuite;
    copie.fuiteActive = this.fuiteActive;

    copie.intentionDeplacementX = this.intentionDeplacementX;
    copie.intentionDeplacementY = this.intentionDeplacementY;
    copie.facteurParallaxe = this.facteurParallaxe;

    copie.tag = this.tag;
    copie.type = this.type;
    copie.afficherFondColore = this.afficherFondColore;
    copie.contenuTexte = this.contenuTexte;
    copie.cheminPolice = this.cheminPolice;
    copie.tailleFonte = this.tailleFonte;

    copie.scaleX = this.scaleX;
    copie.scaleY = this.scaleY;
    copie.rotation = this.rotation;
    copie.parentId = this.parentId;
    copie.alpha = this.alpha;

    copie.estPhysique = this.estPhysique;
    copie.estStatique = this.estStatique;
    copie.vitesseY = this.vitesseY;
    copie.rebond = this.rebond;
    copie.graviteScale = this.graviteScale;

    copie.sautillementActif = this.sautillementActif;
    copie.sautillementIntensite = this.sautillementIntensite;
    copie.sautillementDureeMs = this.sautillementDureeMs;
    copie.tempsDebutSautillement = this.tempsDebutSautillement;
    copie.sautillementInfiniMouvement = this.sautillementInfiniMouvement;

    copie.ancienneX = this.x;
    copie.ancienneY = this.y;

    copie.variablesLocales = new ArrayList<>();

    if (this.variablesLocales != null) {
        for (Variable v : this.variablesLocales) {
            Variable nouvVar = new Variable(v.nom, v.type, "");
            nouvVar.valeur = v.valeur;
            copie.variablesLocales.add(nouvVar);
        }
    }

    for (Map.Entry<String, List<String>> entry : this.animations.entrySet()) {
        copie.animations.put(
            entry.getKey(),
            new ArrayList<>(entry.getValue())
        );
    }

    copie.animationActive = this.animationActive;
    copie.frameCourante = this.frameCourante;
    copie.dernierTempsFrame = this.dernierTempsFrame;
    copie.vitesseFps = this.vitesseFps;
    copie.boucleAnimation = this.boucleAnimation;
    copie.animationEnCours = this.animationEnCours;

    return copie;
}
