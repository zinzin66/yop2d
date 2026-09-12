// haut 1
package com.ludexa.moteur;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;

public class DialogueCreationObjets extends Dialog {
    private InterfaceEditeur editeur;
    private CanvasEditeur canvas;
    private PanneauRessources panneau;

    public DialogueCreationObjets(Context context, InterfaceEditeur editeur, CanvasEditeur canvas, PanneauRessources panneau) {
        super(context);
        this.editeur = editeur;
        this.canvas = canvas;
        this.panneau = panneau;
        initUI();
    }

    private void initUI() {
        setTitle(Traducteur.get("titre_dialogue_objets"));
        
        LinearLayout mainLayout = new LinearLayout(getContext());
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        styliserDialogue(mainLayout);
        
        ScrollView scroll = new ScrollView(getContext());
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        
        LinearLayout conteneurCategories = new LinearLayout(getContext());
        conteneurCategories.setOrientation(LinearLayout.VERTICAL);

        // --- CATEGORIE 1 : FORMES DE BASE ---
        ImageButton btnCarre = creerBouton(R.drawable.square_24px);
        btnCarre.setOnClickListener(v -> creerObjet("carre", Traducteur.get("obj_prefix_carre")));

        ImageButton btnRond = creerBouton(R.drawable.circle_24px);
        btnRond.setOnClickListener(v -> creerObjet("rond", Traducteur.get("obj_prefix_rond")));

        ImageButton btnImage = creerBouton(R.drawable.add_photo_alternate_24px);
        btnImage.setOnClickListener(v -> creerObjet("image", Traducteur.get("obj_prefix_image")));

        ImageButton btnZone = creerBouton(R.drawable.activity_zone_24px);
        btnZone.setOnClickListener(v -> creerObjet("zone", Traducteur.get("obj_prefix_zone")));

        conteneurCategories.addView(creerSection(Traducteur.get("cat_formes_base"), btnCarre, btnRond, btnImage, btnZone));

        // --- CATEGORIE 2 : INTERFACE & HUD ---
        ImageButton btnTexte = creerBouton(R.drawable.title_24px);
        btnTexte.setOnClickListener(v -> creerObjet("texte", Traducteur.get("obj_prefix_texte")));

        ImageButton btnTitreStylise = creerBouton(R.drawable.brand_family_24px);
        btnTitreStylise.setOnClickListener(v -> creerObjet("titre_stylise", Traducteur.get("obj_prefix_titre")));

        ImageButton btnBouton = creerBouton(R.drawable.buttons_alt_24px);
        btnBouton.setOnClickListener(v -> creerObjet("bouton", Traducteur.get("obj_prefix_bouton")));

        ImageButton btnDialogue = creerBouton(R.drawable.chat_24px);
        btnDialogue.setOnClickListener(v -> creerGroupeDialogue());

        ImageButton btnJoystick = creerBouton(R.drawable.trackpad_input_24px);
        btnJoystick.setOnClickListener(v -> creerObjet("joystick", Traducteur.get("obj_prefix_joystick")));

        ImageButton btnAction = creerBouton(R.drawable.center_focus_weak_24px);
        btnAction.setOnClickListener(v -> creerObjet("bouton_action", Traducteur.get("obj_prefix_btnaction")));

        conteneurCategories.addView(creerSection(Traducteur.get("cat_ui_hud"), btnTexte, btnTitreStylise, btnBouton, btnDialogue, btnJoystick, btnAction));

        // --- CATEGORIE 3 : PREFABS ---
        ImageButton btnPrefab = creerBouton(R.drawable.display_add_24px);
        btnPrefab.setOnClickListener(v -> lancerCreationPrefab());

        conteneurCategories.addView(creerSection(Traducteur.get("cat_prefabs"), btnPrefab));

        // Assemblage
        scroll.addView(conteneurCategories);
        mainLayout.addView(scroll);
        
        Button btnFermer = new Button(getContext());
        btnFermer.setText(Traducteur.get("bouton_annuler"));
        btnFermer.setTextColor(Palette.texteNormal);
        btnFermer.setBackground(fond(Palette.boutonNormal, Palette.bordure, 8));
        btnFermer.setPadding(dp(16), dp(12), dp(16), dp(12));
        LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpBtn.setMargins(0, dp(16), 0, 0);
        btnFermer.setLayoutParams(lpBtn);
        btnFermer.setOnClickListener(v -> dismiss());
        mainLayout.addView(btnFermer);

        setContentView(mainLayout);
    }
// bas 1

// haut 2
    private View creerSection(String titre, ImageButton... boutons) {
        LinearLayout section = new LinearLayout(getContext());
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(0, 0, 0, dp(12));

        TextView tvTitre = new TextView(getContext());
        tvTitre.setText(titre);
        tvTitre.setTextColor(Palette.texteSelectionne);
        tvTitre.setTextSize(15f);
        tvTitre.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitre.setPadding(dp(4), dp(8), dp(4), dp(8));
        section.addView(tvTitre);

        HorizontalScrollView hScroll = new HorizontalScrollView(getContext());
        hScroll.setHorizontalScrollBarEnabled(false);

        LinearLayout ligne = new LinearLayout(getContext());
        ligne.setOrientation(LinearLayout.HORIZONTAL);

        for (ImageButton btn : boutons) {
            ligne.addView(btn);
        }

        hScroll.addView(ligne);
        section.addView(hScroll);
        return section;
    }

    private ImageButton creerBouton(int resId) {
        ImageButton btn = new ImageButton(getContext());
        btn.setImageResource(resId);
        btn.setBackground(fond(Palette.boutonNormal, Palette.bordure, 8));
        btn.setPadding(dp(16), dp(16), dp(16), dp(16));
        btn.setColorFilter(Palette.iconeNormal);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(64), dp(64));
        lp.setMargins(dp(4), dp(4), dp(8), dp(4));
        btn.setLayoutParams(lp);
        return btn;
    }

    private void finaliserCreationObjet(ObjetBase nouveau) {
        nouveau.zOrder = editeur.sceneActive.prochainZOrder();
        editeur.sceneActive.ajouterObjet(nouveau);
        canvas.invalidate();
        panneau.rafraichirArborescence();
        if ("scene_instance".equals(nouveau.type) || "titre_stylise".equals(nouveau.type)) {
            canvas.setObjetSelectionne(nouveau);
        }
        this.dismiss();
    }

    private void creerObjet(String type, String prefixe) {
        String nomUnique = genererNomUnique(prefixe, editeur.sceneActive);
        ObjetBase nouveau = null;

        switch (type) {
            case "carre":
                nouveau = new ObjetBase(nomUnique, 150f, 150f, 80f, 80f);
                nouveau.type = "carre";
                break;
            case "rond":
                nouveau = new ObjetBase(nomUnique, 100f, 200f, 90f, 90f);
                nouveau.type = "rond";
                break;
            case "image":
                nouveau = new ObjetBase(nomUnique, 150f, 150f, 100f, 100f);
                nouveau.type = "image";
                break;
            case "zone":
                nouveau = new ObjetBase(nomUnique, 150f, 150f, 100f, 100f);
                nouveau.type = "zone";
                nouveau.estZoneDeClic = true;
                nouveau.couleur = Color.argb(120, 255, 152, 0);
                break;
            case "texte":
                nouveau = new ObjetBase(nomUnique, 200f, 100f, 120f, 40f);
                nouveau.type = "texte";
                break;
            case "titre_stylise":
                nouveau = new ObjetBase(nomUnique, 200f, 150f, 300f, 80f);
                nouveau.type = "titre_stylise";
                nouveau.contenuTexte = Traducteur.get("texte_titre_defaut");
                nouveau.couleur = Color.WHITE;
                nouveau.tailleFonte = 40f;
                nouveau.afficherFondColore = false;
                break;
            case "bouton":
                nouveau = new ObjetBase(nomUnique, 150f, 150f, 120f, 50f);
                nouveau.type = "bouton";
                nouveau.estZoneDeClic = true;
                break;
            case "joystick":
                nouveau = new ObjetBase(nomUnique, 50f, 400f, 160f, 160f);
                nouveau.type = "joystick";
                nouveau.afficherFondColore = false;
                nouveau.couleur = Color.argb(100, 200, 200, 200);
                break;
            case "bouton_action":
                nouveau = new ObjetBase(nomUnique, 600f, 450f, 100f, 100f);
                nouveau.type = "bouton_action";
                nouveau.afficherFondColore = false;
                nouveau.couleur = Color.argb(150, 255, 100, 100);
                break;
        }

        if (nouveau != null) {
            finaliserCreationObjet(nouveau);
        }
    }
// bas 2

// haut 3
    private void creerGroupeDialogue() {
        String nomFond = genererNomUnique(Traducteur.get("obj_prefix_boitedialogue"), editeur.sceneActive);
        ObjetBase fond = new ObjetBase(nomFond, 50f, 300f, 700f, 150f);
        fond.type = "image";
        fond.couleur = Color.argb(220, 30, 30, 30);
        fond.afficherFondColore = true;
        fond.zOrder = editeur.sceneActive.prochainZOrder();
        editeur.sceneActive.ajouterObjet(fond);
        
        String nomTexte = genererNomUnique(Traducteur.get("obj_prefix_textedialogue"), editeur.sceneActive);
        ObjetBase texte = new ObjetBase(nomTexte, 20f, 20f, 600f, 110f);
        texte.type = "texte";
        texte.contenuTexte = Traducteur.get("texte_dialogue_defaut");
        texte.couleur = Color.WHITE;
        texte.tailleFonte = 20f;
        texte.parentId = fond.id;
        texte.zOrder = editeur.sceneActive.prochainZOrder();
        editeur.sceneActive.ajouterObjet(texte);
        
        String nomBtn = genererNomUnique(Traducteur.get("obj_prefix_btnfermer"), editeur.sceneActive);
        ObjetBase btn = new ObjetBase(nomBtn, 640f, 10f, 40f, 40f);
        btn.type = "bouton";
        btn.estZoneDeClic = true;
        btn.couleur = Color.parseColor("#E53935");
        btn.afficherFondColore = true;
        btn.parentId = fond.id;
        btn.zOrder = editeur.sceneActive.prochainZOrder();
        editeur.sceneActive.ajouterObjet(btn);

        canvas.invalidate();
        panneau.rafraichirArborescence();
        Toast.makeText(getContext(), Traducteur.get("toast_groupe_dialogue_cree"), Toast.LENGTH_SHORT).show();
        this.dismiss();
    }

    private void lancerCreationPrefab() {
        List<Scene> autresScenes = new ArrayList<>();
        if (editeur.listeScenes != null) {
            for (Scene s : editeur.listeScenes) {
                if (s != editeur.sceneActive) autresScenes.add(s);
            }
        }
        if (autresScenes.isEmpty()) { 
            Toast.makeText(getContext(), Traducteur.get("erreur_aucune_autre_scene"), Toast.LENGTH_SHORT).show(); 
            return; 
        }
        
        Dialog dialogScene = new Dialog(getContext());
        dialogScene.setTitle(Traducteur.get("titre_select_scene_liee"));
        LinearLayout layoutDialog = new LinearLayout(getContext());
        layoutDialog.setOrientation(LinearLayout.VERTICAL);
        styliserDialogue(layoutDialog);
        
        for (Scene s : autresScenes) {
            Button btnScene = new Button(getContext());
            btnScene.setText(s.nom);
            btnScene.setTextColor(Palette.texteNormal);
            btnScene.setBackground(fond(Palette.fondListe, Palette.bordure, 8));
            btnScene.setPadding(dp(12), dp(12), dp(12), dp(12));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(8));
            btnScene.setLayoutParams(lp);
            
            btnScene.setOnClickListener(vScene -> {
                String nomUnique = genererNomUnique(Traducteur.get("obj_prefix_prefab"), editeur.sceneActive);
                android.graphics.RectF limites = canvas.calculerLimitesScene(s);
                float initLargeur = Math.max(50f, limites.right);
                float initHauteur = Math.max(50f, limites.bottom);
                
                ObjetBase nouveau = new ObjetBase(nomUnique, 150f, 150f, initLargeur, initHauteur);
                nouveau.type = "scene_instance";
                nouveau.sceneLieeId = s.id;
                nouveau.afficherFondColore = true;
                nouveau.couleur = Color.argb(120, 100, 150, 255); 
                
                dialogScene.dismiss();
                finaliserCreationObjet(nouveau);
            });
            layoutDialog.addView(btnScene);
        }
        
        Button btnAnnuler = new Button(getContext());
        btnAnnuler.setText(Traducteur.get("bouton_annuler"));
        btnAnnuler.setTextColor(Palette.texteNormal);
        btnAnnuler.setBackground(fond(Palette.boutonNormal, Palette.bordure, 8));
        btnAnnuler.setPadding(dp(16), dp(12), dp(16), dp(12));
        btnAnnuler.setOnClickListener(vAnnuler -> dialogScene.dismiss());
        layoutDialog.addView(btnAnnuler);
        
        dialogScene.setContentView(layoutDialog);
        dialogScene.show();
    }

    // --- Utilitaires dupliqués localement pour isoler le composant ---
    private int dp(int valeur) {
        return (int) (valeur * getContext().getResources().getDisplayMetrics().density);
    }

    private android.graphics.drawable.GradientDrawable fond(int couleurFond, int couleurBordure, int rayon) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(couleurFond);
        g.setCornerRadius(dp(rayon));
        g.setStroke(dp(1), couleurBordure);
        return g;
    }

    private void styliserDialogue(LinearLayout layout) {
        layout.setBackgroundColor(Palette.fondPanneaux);
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));
    }

    private String genererNomUnique(String prefixe, Scene scene) {
        if (scene == null || scene.objets == null) return prefixe + " 1";
        int compteur = 1;
        String nom;
        boolean existe;
        do {
            nom = prefixe + " " + compteur;
            existe = false;
            for (ObjetBase obj : scene.objets) {
                if (nom.equals(obj.nom)) {
                    existe = true;
                    break;
                }
            }
            compteur++;
        } while (existe);
        return nom;
    }
}
// bas 3
                                          
