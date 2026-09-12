// haut 1
package com.ludexa.moteur;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class EditeurTexteStyleDialog extends Dialog {

    private String cheminProjet;
    private List<StyleTitre> listeStyles = new ArrayList<>();
    private StyleTitre styleCourant = null;
    private android.graphics.Typeface policeApercu = android.graphics.Typeface.DEFAULT; 

    private LinearLayout panelGauche;
    private ScrollView panelDroit;
    private ApercuTexteView vueApercu;
    private Button btnPoliceApercu;

    private Spinner spinnerStyles;
    private EditText champNomStyle;
    
    // Nouveaux contrôles visuels (Boutons segmentés)
    private ImageButton btnAlignGauche, btnAlignCenter, btnAlignDroite;
    private ImageButton btnFill, btnStroke, btnFillStroke, btnTextureOnly, btnTextureStroke;
    
    // Nouveaux contrôles visuels (Curseurs)
    private SeekBar sbEspacementLettres, sbEspacementLignes, sbInclinaison, sbCourbure, sbEpaisseur;
    private TextView tvValLettres, tvValLignes, tvValInclinaison, tvValCourbure, tvValEpaisseur;
    
    private Button btnTexture, btnCouleurContour;
    private CheckBox cbDegrade;
    private Button btnCouleurDeg1, btnCouleurDeg2;
    
    // Conteneurs dynamiques pour masquer/afficher
    private LinearLayout containerNeon, containerOmbre, containerRelief;
    private CheckBox cbOmbre, cbNeon, cbRelief;
    
    private SeekBar sbNeonRayon, sbOmbreRayon, sbOmbreDx, sbOmbreDy, sbReliefElevation;
    private TextView tvValNeonRayon, tvValOmbreRayon, tvValOmbreDx, tvValOmbreDy, tvValReliefElevation;
    private Button btnCouleurNeon, btnCouleurOmbre;

    private boolean isUpdatingUI = false;

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

    public EditeurTexteStyleDialog(Context context, String cheminProjet) {
        super(context);
        this.cheminProjet = cheminProjet;
        setTitle(Traducteur.get("titre_editeur_styles"));

        chargerStyles();
        if (listeStyles.isEmpty()) {
            listeStyles.add(new StyleTitre(Traducteur.get("style_nouveau_defaut")));
        }
        styleCourant = listeStyles.get(0);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(Palette.fondPanneaux);
        root.setPadding(dp(10), dp(10), dp(10), dp(10));

        // --- PANEL GAUCHE (Aperçu) ---
        panelGauche = new LinearLayout(context);
        panelGauche.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpGauche = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.65f);
        lpGauche.setMargins(0, 0, dp(8), 0);
        panelGauche.setLayoutParams(lpGauche);
        panelGauche.setBackground(fond(Palette.fondNormal, Palette.bordure, 12));
        
        TextView titreApercu = new TextView(context);
        titreApercu.setText(Traducteur.get("style_apercu"));
        titreApercu.setTextColor(Palette.texteSelectionne);
        titreApercu.setTextSize(16f);
        titreApercu.setTypeface(null, android.graphics.Typeface.BOLD);
        titreApercu.setGravity(Gravity.CENTER_VERTICAL);
        titreApercu.setBackground(fond(Palette.enTeteDialogues, Palette.bordure, 10));
        titreApercu.setPadding(dp(14), dp(10), dp(14), dp(10));
        panelGauche.addView(titreApercu);

        vueApercu = new ApercuTexteView(context);
        vueApercu.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        vueApercu.setBackground(fond(Palette.canvasFond, Palette.bordure, 10));
        panelGauche.addView(vueApercu);

        btnPoliceApercu = new Button(context);
        btnPoliceApercu.setText(Traducteur.get("style_btn_police_apercu"));
        btnPoliceApercu.setBackground(fond(Palette.boutonNormal, Palette.bordure, 10));
        btnPoliceApercu.setTextColor(Palette.texteNormal);
        btnPoliceApercu.setTextSize(14f);
        btnPoliceApercu.setAllCaps(false);
        btnPoliceApercu.setOnClickListener(v -> {
            File dossierPolices = new File(cheminProjet, "assets_ludexa/Fonts");
            List<String> polices = listerFichiersLocales(dossierPolices, "assets_ludexa/Fonts/", ".ttf", ".otf");
            if (polices.isEmpty()) { Toast.makeText(context, Traducteur.get("insp_aucune_police"), Toast.LENGTH_SHORT).show(); return; }
            List<String> options = new ArrayList<>();
            options.add(Traducteur.get("insp_police_defaut"));
            options.addAll(polices);
            new AlertDialog.Builder(context)
                .setTitle(Traducteur.get("insp_titre_select_police"))
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) policeApercu = android.graphics.Typeface.DEFAULT;
                    else {
                        try {
                            policeApercu = android.graphics.Typeface.createFromFile(new File(cheminProjet, options.get(which)));
                        } catch (Exception e) { policeApercu = android.graphics.Typeface.DEFAULT; }
                    }
                    vueApercu.invalidate();
                }).show();
        });
        LinearLayout.LayoutParams lpBtnPol = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpBtnPol.setMargins(dp(8), dp(8), dp(8), dp(8));
        panelGauche.addView(btnPoliceApercu, lpBtnPol);
// bas 1
// haut 2
        // --- PANEL DROIT (Contrôles) ---
        panelDroit = new ScrollView(context);
        panelDroit.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.35f));
        panelDroit.setBackground(fond(Palette.fondNormal, Palette.bordure, 12));

        LinearLayout contenuDroit = new LinearLayout(context);
        contenuDroit.setOrientation(LinearLayout.VERTICAL);
        contenuDroit.setPadding(dp(14), dp(12), dp(14), dp(16));

        // 1. SÉLECTION
        Button btnNouveau = new Button(context);
        btnNouveau.setText(Traducteur.get("style_btn_nouveau"));
        btnNouveau.setBackground(fond(Palette.boutonNormal, Palette.bordure, 10)); // Correction couleur verte
        btnNouveau.setTextColor(Palette.texteNormal);
        btnNouveau.setTextSize(14f);
        btnNouveau.setAllCaps(false);
        btnNouveau.setOnClickListener(v -> {
            StyleTitre nv = new StyleTitre(Traducteur.get("style_nouveau_defaut") + " " + (listeStyles.size() + 1));
            listeStyles.add(nv);
            styleCourant = nv;
            rafraichirSpinnerStyles();
            rafraichirUI();
        });
        contenuDroit.addView(btnNouveau);

        spinnerStyles = new Spinner(context);
        spinnerStyles.setBackground(fond(Palette.fondListe, Palette.bordure, 10));
        spinnerStyles.setPadding(dp(10), dp(10), dp(10), dp(10));
        contenuDroit.addView(spinnerStyles);
        
        spinnerStyles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isUpdatingUI) { styleCourant = listeStyles.get(position); rafraichirUI(); }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 2. NOM & ALIGNEMENT
        champNomStyle = new EditText(context);
        champNomStyle.setHint(Traducteur.get("style_hint_nom"));
        champNomStyle.addTextChangedListener(creerWatcher(texte -> { styleCourant.nom = texte; rafraichirSpinnerStylesSansTrigger(); }));
        contenuDroit.addView(genererLigneLabelChamp(context, Traducteur.get("style_label_nom"), champNomStyle));

        // Boutons Alignement
        LinearLayout ligneAlign = genererLigneLabel(context, Traducteur.get("style_label_alignement"));
        btnAlignGauche = creerBoutonIcone(context, "format_align_left_24px");
        btnAlignCenter = creerBoutonIcone(context, "format_align_center_24px");
        btnAlignDroite = creerBoutonIcone(context, "format_align_right_24px");
        
        btnAlignGauche.setOnClickListener(v -> { if(!isUpdatingUI) { styleCourant.alignement = "GAUCHE"; rafraichirUI(); }});
        btnAlignCenter.setOnClickListener(v -> { if(!isUpdatingUI) { styleCourant.alignement = "CENTRE"; rafraichirUI(); }});
        btnAlignDroite.setOnClickListener(v -> { if(!isUpdatingUI) { styleCourant.alignement = "DROITE"; rafraichirUI(); }});
        
        ligneAlign.addView(btnAlignGauche); ligneAlign.addView(btnAlignCenter); ligneAlign.addView(btnAlignDroite);
        contenuDroit.addView(ligneAlign);

        // 3. ESPACEMENT & DEFORMATION (SeekBars)
        ajouterSeparateur(context, contenuDroit, Traducteur.get("style_sep_espacement"));

        SeekBar[] sbTmp = new SeekBar[1]; TextView[] tvTmp = new TextView[1];
        
        contenuDroit.addView(genererLigneCurseur(context, "style_label_lettres", -1f, 3f, 0.1f, sbTmp, tvTmp, val -> { styleCourant.espacementLettres = val; vueApercu.invalidate(); }));
        sbEspacementLettres = sbTmp[0]; tvValLettres = tvTmp[0];

        contenuDroit.addView(genererLigneCurseur(context, "style_label_lignes", 0.5f, 3f, 0.1f, sbTmp, tvTmp, val -> { styleCourant.multiplicateurLignes = val; vueApercu.invalidate(); }));
        sbEspacementLignes = sbTmp[0]; tvValLignes = tvTmp[0];

        contenuDroit.addView(genererLigneCurseur(context, "style_label_inclinaison", -1.5f, 1.5f, 0.1f, sbTmp, tvTmp, val -> { styleCourant.inclinaison = val; vueApercu.invalidate(); }));
        sbInclinaison = sbTmp[0]; tvValInclinaison = tvTmp[0];

        contenuDroit.addView(genererLigneCurseur(context, "style_label_courbure", -500f, 500f, 10f, sbTmp, tvTmp, val -> { styleCourant.courbure = val; vueApercu.invalidate(); }));
        sbCourbure = sbTmp[0]; tvValCourbure = tvTmp[0];

        // 4. REMPLISSAGE
        ajouterSeparateur(context, contenuDroit, Traducteur.get("style_sep_remplissage"));
        
        LinearLayout ligneRemp = genererLigneLabel(context, Traducteur.get("style_label_mode"));
        btnFill = creerBoutonIcone(context, "format_color_fill_24px");
        btnStroke = creerBoutonIcone(context, "border_color_24px");
        btnFillStroke = creerBoutonIcone(context, "format_shapes_24px");
        btnTextureOnly = creerBoutonIcone(context, "texture_24px");
        btnTextureStroke = creerBoutonIcone(context, "layers_24px");
        
        btnFill.setOnClickListener(v -> { if(!isUpdatingUI) { styleCourant.modeRemplissage = "FILL"; rafraichirUI(); }});
        btnStroke.setOnClickListener(v -> { if(!isUpdatingUI) { styleCourant.modeRemplissage = "STROKE"; rafraichirUI(); }});
        btnFillStroke.setOnClickListener(v -> { if(!isUpdatingUI) { styleCourant.modeRemplissage = "FILL_AND_STROKE"; rafraichirUI(); }});
        btnTextureOnly.setOnClickListener(v -> { if(!isUpdatingUI) { styleCourant.modeRemplissage = "TEXTURE"; rafraichirUI(); }});
        btnTextureStroke.setOnClickListener(v -> { if(!isUpdatingUI) { styleCourant.modeRemplissage = "TEXTURE_AND_STROKE"; rafraichirUI(); }});
        
        ligneRemp.addView(btnFill); ligneRemp.addView(btnStroke); ligneRemp.addView(btnFillStroke); 
        ligneRemp.addView(btnTextureOnly); ligneRemp.addView(btnTextureStroke);
        contenuDroit.addView(ligneRemp);

        btnTexture = new Button(context);
        btnTexture.setText(Traducteur.get("style_btn_texture"));
        btnTexture.setBackground(fond(Palette.boutonNormal, Palette.bordure, 10));
        btnTexture.setTextColor(Palette.texteNormal);
        btnTexture.setTextSize(14f);
        btnTexture.setAllCaps(false);
        btnTexture.setOnClickListener(v -> {
            File dossierImages = new File(cheminProjet, "assets_ludexa/Images");
            List<String> images = listerFichiersLocales(dossierImages, "assets_ludexa/Images/", ".png", ".jpg", ".webp");
            if (images.isEmpty()) { Toast.makeText(context, Traducteur.get("insp_aucune_image"), Toast.LENGTH_SHORT).show(); return; }
            List<String> options = new ArrayList<>();
            options.add(Traducteur.get("valeur_aucune"));
            options.addAll(images);
            new AlertDialog.Builder(context)
                .setTitle(Traducteur.get("style_titre_select_texture"))
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) styleCourant.cheminTexture = null;
                    else styleCourant.cheminTexture = options.get(which);
                    vueApercu.invalidate();
                }).show();
        });
        contenuDroit.addView(btnTexture);

        // 5. CONTOUR & DÉGRADÉ
        ajouterSeparateur(context, contenuDroit, Traducteur.get("style_sep_contour"));
        
        contenuDroit.addView(genererLigneCurseur(context, "style_label_epaisseur", 0f, 20f, 1f, sbTmp, tvTmp, val -> { styleCourant.epaisseurContour = val; vueApercu.invalidate(); }));
        sbEpaisseur = sbTmp[0]; tvValEpaisseur = tvTmp[0];

        btnCouleurContour = new Button(context);
        btnCouleurContour.setText(Traducteur.get("style_btn_couleur_contour"));
        btnCouleurContour.setTextColor(Palette.texteNormal);
        btnCouleurContour.setTextSize(13f);
        btnCouleurContour.setAllCaps(false);
        btnCouleurContour.setOnClickListener(v -> afficherColorPicker(context, styleCourant.couleurContour, couleur -> { styleCourant.couleurContour = couleur; rafraichirUI(); }));
        contenuDroit.addView(btnCouleurContour);

        cbDegrade = new CheckBox(context);
        cbDegrade.setText(Traducteur.get("style_cb_degrade"));
        cbDegrade.setTextColor(Palette.texteNormal);
        cbDegrade.setTextSize(14f);
        cbDegrade.setPadding(dp(8), dp(10), dp(8), dp(10));
        cbDegrade.setOnCheckedChangeListener((btn, isChecked) -> { if(!isUpdatingUI){ styleCourant.utiliserDegrade = isChecked; vueApercu.invalidate(); }});
        contenuDroit.addView(cbDegrade);

        btnCouleurDeg1 = new Button(context);
        btnCouleurDeg1.setText(Traducteur.get("style_btn_deg_haut"));
        btnCouleurDeg1.setTextColor(Palette.texteNormal);
        btnCouleurDeg1.setTextSize(13f);
        btnCouleurDeg1.setAllCaps(false);
        btnCouleurDeg1.setOnClickListener(v -> afficherColorPicker(context, styleCourant.couleurDegrade1, couleur -> { styleCourant.couleurDegrade1 = couleur; rafraichirUI(); }));
        contenuDroit.addView(btnCouleurDeg1);

        btnCouleurDeg2 = new Button(context);
        btnCouleurDeg2.setText(Traducteur.get("style_btn_deg_bas"));
        btnCouleurDeg2.setTextColor(Palette.texteNormal);
        btnCouleurDeg2.setTextSize(13f);
        btnCouleurDeg2.setAllCaps(false);
        btnCouleurDeg2.setOnClickListener(v -> afficherColorPicker(context, styleCourant.couleurDegrade2, couleur -> { styleCourant.couleurDegrade2 = couleur; rafraichirUI(); }));
        contenuDroit.addView(btnCouleurDeg2);

        // 6. NEON (Dynamique)
        ajouterSeparateur(context, contenuDroit, Traducteur.get("style_sep_neon"));
        cbNeon = new CheckBox(context);
        cbNeon.setText(Traducteur.get("style_cb_neon"));
        cbNeon.setTextColor(Palette.texteNormal);
        cbNeon.setTextSize(14f);
        cbNeon.setPadding(dp(8), dp(10), dp(8), dp(10));
        cbNeon.setOnCheckedChangeListener((btn, isChecked) -> { if(!isUpdatingUI){ styleCourant.neonActif = isChecked; containerNeon.setVisibility(isChecked ? View.VISIBLE : View.GONE); vueApercu.invalidate(); }});
        contenuDroit.addView(cbNeon);

        containerNeon = new LinearLayout(context);
        containerNeon.setOrientation(LinearLayout.VERTICAL);
        
        containerNeon.addView(genererLigneCurseur(context, "style_label_rayon", 0f, 50f, 1f, sbTmp, tvTmp, val -> { styleCourant.neonRayon = val; vueApercu.invalidate(); }));
        sbNeonRayon = sbTmp[0]; tvValNeonRayon = tvTmp[0];

        btnCouleurNeon = new Button(context);
        btnCouleurNeon.setText(Traducteur.get("style_btn_couleur_neon"));
        btnCouleurNeon.setTextColor(Palette.texteNormal);
        btnCouleurNeon.setTextSize(13f);
        btnCouleurNeon.setAllCaps(false);
        btnCouleurNeon.setOnClickListener(v -> afficherColorPicker(context, styleCourant.neonCouleur, couleur -> { styleCourant.neonCouleur = couleur; rafraichirUI(); }));
        containerNeon.addView(btnCouleurNeon);
        contenuDroit.addView(containerNeon);

        // 7. OMBRE (Dynamique)
        ajouterSeparateur(context, contenuDroit, Traducteur.get("style_sep_ombre"));
        cbOmbre = new CheckBox(context);
        cbOmbre.setText(Traducteur.get("style_cb_ombre"));
        cbOmbre.setTextColor(Palette.texteNormal);
        cbOmbre.setTextSize(14f);
        cbOmbre.setPadding(dp(8), dp(10), dp(8), dp(10));
        cbOmbre.setOnCheckedChangeListener((btn, isChecked) -> { if(!isUpdatingUI){ styleCourant.ombreActive = isChecked; containerOmbre.setVisibility(isChecked ? View.VISIBLE : View.GONE); vueApercu.invalidate(); }});
        contenuDroit.addView(cbOmbre);

        containerOmbre = new LinearLayout(context);
        containerOmbre.setOrientation(LinearLayout.VERTICAL);
        
        containerOmbre.addView(genererLigneCurseur(context, "style_label_rayon", 0f, 30f, 1f, sbTmp, tvTmp, val -> { styleCourant.ombreRayon = val; vueApercu.invalidate(); }));
        sbOmbreRayon = sbTmp[0]; tvValOmbreRayon = tvTmp[0];

        containerOmbre.addView(genererLigneCurseur(context, "style_label_dx", -50f, 50f, 1f, sbTmp, tvTmp, val -> { styleCourant.ombreDx = val; vueApercu.invalidate(); }));
        sbOmbreDx = sbTmp[0]; tvValOmbreDx = tvTmp[0];

        containerOmbre.addView(genererLigneCurseur(context, "style_label_dy", -50f, 50f, 1f, sbTmp, tvTmp, val -> { styleCourant.ombreDy = val; vueApercu.invalidate(); }));
        sbOmbreDy = sbTmp[0]; tvValOmbreDy = tvTmp[0];

        btnCouleurOmbre = new Button(context);
        btnCouleurOmbre.setText(Traducteur.get("style_btn_couleur_ombre"));
        btnCouleurOmbre.setTextColor(Palette.texteNormal);
        btnCouleurOmbre.setTextSize(13f);
        btnCouleurOmbre.setAllCaps(false);
        btnCouleurOmbre.setOnClickListener(v -> afficherColorPicker(context, styleCourant.ombreCouleur, couleur -> { styleCourant.ombreCouleur = couleur; rafraichirUI(); }));
        containerOmbre.addView(btnCouleurOmbre);
        contenuDroit.addView(containerOmbre);
        
        // 8. RELIEF (Dynamique)
        ajouterSeparateur(context, contenuDroit, Traducteur.get("style_sep_relief"));
        cbRelief = new CheckBox(context);
        cbRelief.setText(Traducteur.get("style_cb_relief"));
        cbRelief.setTextColor(Palette.texteNormal);
        cbRelief.setTextSize(14f);
        cbRelief.setPadding(dp(8), dp(10), dp(8), dp(10));
        cbRelief.setOnCheckedChangeListener((btn, isChecked) -> { if(!isUpdatingUI){ styleCourant.reliefActif = isChecked; containerRelief.setVisibility(isChecked ? View.VISIBLE : View.GONE); vueApercu.invalidate(); }});
        contenuDroit.addView(cbRelief);
        
        containerRelief = new LinearLayout(context);
        containerRelief.setOrientation(LinearLayout.VERTICAL);
        
        containerRelief.addView(genererLigneCurseur(context, "style_label_elevation", 0f, 20f, 1f, sbTmp, tvTmp, val -> { styleCourant.reliefElevation = val; vueApercu.invalidate(); }));
        sbReliefElevation = sbTmp[0]; tvValReliefElevation = tvTmp[0];
        contenuDroit.addView(containerRelief);

        panelDroit.addView(contenuDroit);
        root.addView(panelGauche);
        root.addView(panelDroit);

        LinearLayout layoutPrincipal = new LinearLayout(context);
        layoutPrincipal.setOrientation(LinearLayout.VERTICAL);
        layoutPrincipal.setBackgroundColor(Palette.fondPanneaux);
        layoutPrincipal.addView(root, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button btnFermer = new Button(context);
        btnFermer.setText(Traducteur.get("bouton_fermer_sauvegarder"));
        btnFermer.setBackground(fond(Palette.boutonNormal, Palette.texteSelectionne, 12));
        btnFermer.setTextColor(Palette.texteNormal);
        btnFermer.setTextSize(15f);
        btnFermer.setAllCaps(false);
        btnFermer.setPadding(dp(12), dp(12), dp(12), dp(12));
        btnFermer.setOnClickListener(v -> {
            sauvegarderStyles();
            dismiss();
        });
        layoutPrincipal.addView(btnFermer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(layoutPrincipal);

        Window window = getWindow();
        if (window != null) {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            window.setLayout((int) (metrics.widthPixels * 0.95), (int) (metrics.heightPixels * 0.95));
        }

        rafraichirSpinnerStyles();
        rafraichirUI();
    }
// bas 2
  // haut 3
    private void rafraichirUI() {
        isUpdatingUI = true;
        champNomStyle.setText(styleCourant.nom);
        
        // Mise à jour visuelle des boutons Alignement
        ImageButton[] btnsAlign = {btnAlignGauche, btnAlignCenter, btnAlignDroite};
        int idxAlign = "GAUCHE".equals(styleCourant.alignement) ? 0 : ("DROITE".equals(styleCourant.alignement) ? 2 : 1);
        updateBoutonsIcones(btnsAlign, idxAlign);
        
        // Mise à jour visuelle des boutons Remplissage
        ImageButton[] btnsRemp = {btnFill, btnStroke, btnFillStroke, btnTextureOnly, btnTextureStroke};
        int idxRemp = 0;
        if ("STROKE".equals(styleCourant.modeRemplissage)) idxRemp = 1;
        if ("FILL_AND_STROKE".equals(styleCourant.modeRemplissage)) idxRemp = 2;
        if ("TEXTURE".equals(styleCourant.modeRemplissage)) idxRemp = 3;
        if ("TEXTURE_AND_STROKE".equals(styleCourant.modeRemplissage)) idxRemp = 4;
        updateBoutonsIcones(btnsRemp, idxRemp);

        // Mise à jour des SeekBars
        setCurseurValeur(sbEspacementLettres, tvValLettres, styleCourant.espacementLettres, -1f, 0.1f);
        setCurseurValeur(sbEspacementLignes, tvValLignes, styleCourant.multiplicateurLignes, 0.5f, 0.1f);
        setCurseurValeur(sbInclinaison, tvValInclinaison, styleCourant.inclinaison, -1.5f, 0.1f);
        setCurseurValeur(sbCourbure, tvValCourbure, styleCourant.courbure, -500f, 10f);
        setCurseurValeur(sbEpaisseur, tvValEpaisseur, styleCourant.epaisseurContour, 0f, 1f);
        
        cbDegrade.setChecked(styleCourant.utiliserDegrade);
        
        cbNeon.setChecked(styleCourant.neonActif);
        containerNeon.setVisibility(styleCourant.neonActif ? View.VISIBLE : View.GONE);
        setCurseurValeur(sbNeonRayon, tvValNeonRayon, styleCourant.neonRayon, 0f, 1f);
        
        cbOmbre.setChecked(styleCourant.ombreActive);
        containerOmbre.setVisibility(styleCourant.ombreActive ? View.VISIBLE : View.GONE);
        setCurseurValeur(sbOmbreRayon, tvValOmbreRayon, styleCourant.ombreRayon, 0f, 1f);
        setCurseurValeur(sbOmbreDx, tvValOmbreDx, styleCourant.ombreDx, -50f, 1f);
        setCurseurValeur(sbOmbreDy, tvValOmbreDy, styleCourant.ombreDy, -50f, 1f);
        
        cbRelief.setChecked(styleCourant.reliefActif);
        containerRelief.setVisibility(styleCourant.reliefActif ? View.VISIBLE : View.GONE);
        setCurseurValeur(sbReliefElevation, tvValReliefElevation, styleCourant.reliefElevation, 0f, 1f);

        btnCouleurContour.setBackground(fond(styleCourant.couleurContour, Palette.bordure, 10));
        btnCouleurDeg1.setBackground(fond(styleCourant.couleurDegrade1, Palette.bordure, 10));
        btnCouleurDeg2.setBackground(fond(styleCourant.couleurDegrade2, Palette.bordure, 10));
        btnCouleurNeon.setBackground(fond(styleCourant.neonCouleur, Palette.bordure, 10));
        btnCouleurOmbre.setBackground(fond(styleCourant.ombreCouleur, Palette.bordure, 10));

        isUpdatingUI = false;
        vueApercu.invalidate();
    }

    private void rafraichirSpinnerStyles() {
        isUpdatingUI = true;
        List<String> noms = new ArrayList<>();
        for (StyleTitre st : listeStyles) noms.add(st.nom);
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, noms) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent); tv.setTextColor(Palette.texteNormal); return tv;
            }
            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent); tv.setTextColor(Palette.texteNormal); tv.setBackgroundColor(Palette.fondNormal); tv.setPadding(dp(16), dp(16), dp(16), dp(16)); return tv;
            }
        };
        spinnerStyles.setAdapter(adapter);
        spinnerStyles.setSelection(listeStyles.indexOf(styleCourant));
        isUpdatingUI = false;
    }

    private void rafraichirSpinnerStylesSansTrigger() {
        int currentIndex = spinnerStyles.getSelectedItemPosition();
        List<String> noms = new ArrayList<>();
        for (StyleTitre st : listeStyles) noms.add(st.nom);
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerStyles.getAdapter();
        adapter.clear(); adapter.addAll(noms); adapter.notifyDataSetChanged();
        spinnerStyles.setSelection(currentIndex, false);
    }

    // Séparateur épuré, sans boite (juste texte + ligne)
    private void ajouterSeparateur(Context ctx, LinearLayout parent, String texte) {
        TextView sep = new TextView(ctx);
        sep.setText(texte.toUpperCase());
        sep.setTextColor(Palette.texteSelectionne);
        sep.setTypeface(null, android.graphics.Typeface.BOLD);
        sep.setTextSize(14f);
        sep.setPadding(dp(4), dp(16), dp(4), dp(4));
        parent.addView(sep);
        
        View ligne = new View(ctx);
        ligne.setBackgroundColor(Palette.bordure);
        ligne.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        parent.addView(ligne);
    }

    private LinearLayout genererLigneLabelChamp(Context ctx, String label, EditText champ) {
        LinearLayout ligne = genererLigneLabel(ctx, label);
        champ.setTextColor(Palette.texteNormal);
        champ.setHintTextColor(Palette.bordure);
        champ.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        champ.setTextSize(14f);
        champ.setBackground(fond(Palette.canvasFond, Palette.bordure, 10));
        champ.setPadding(dp(10), dp(10), dp(10), dp(10));
        ligne.addView(champ);
        return ligne;
    }

    private LinearLayout genererLigneLabel(Context ctx, String label) {
        LinearLayout ligne = new LinearLayout(ctx);
        ligne.setOrientation(LinearLayout.HORIZONTAL);
        ligne.setGravity(Gravity.CENTER_VERTICAL);
        ligne.setPadding(0, dp(6), 0, dp(6));
        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextColor(Palette.texteNormal);
        tv.setTextSize(13f);
        tv.setLayoutParams(new LinearLayout.LayoutParams(dp(80), ViewGroup.LayoutParams.WRAP_CONTENT));
        ligne.addView(tv);
        return ligne;
    }

    private LinearLayout genererLigneCurseur(Context ctx, String labelKey, float min, float max, float pas, SeekBar[] sbOut, TextView[] tvOut, java.util.function.Consumer<Float> action) {
        LinearLayout ligne = genererLigneLabel(ctx, Traducteur.get(labelKey));
        
        SeekBar sb = new SeekBar(ctx);
        sb.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        int maxProgress = Math.max(1, (int)((max - min) / pas));
        sb.setMax(maxProgress);
        
        TextView tvVal = new TextView(ctx);
        tvVal.setTextColor(Palette.texteNormal);
        tvVal.setTextSize(12f);
        tvVal.setGravity(Gravity.RIGHT);
        tvVal.setLayoutParams(new LinearLayout.LayoutParams(dp(40), ViewGroup.LayoutParams.WRAP_CONTENT));
        
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float val = min + (progress * pas);
                val = Math.round(val * 100f) / 100f; // Eviter la précision flottante 0.3000004
                tvVal.setText(String.format(java.util.Locale.US, "%.1f", val));
                if (!isUpdatingUI) action.accept(val);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) { vueApercu.invalidate(); }
        });
        
        sbOut[0] = sb;
        tvOut[0] = tvVal;
        
        ligne.addView(sb);
        ligne.addView(tvVal);
        return ligne;
    }

    private void setCurseurValeur(SeekBar sb, TextView tv, float val, float min, float pas) {
        if (sb == null) return;
        int progress = (int)((val - min) / pas);
        if (progress < 0) progress = 0;
        if (progress > sb.getMax()) progress = sb.getMax();
        sb.setProgress(progress);
        tv.setText(String.format(java.util.Locale.US, "%.1f", val));
    }

    private ImageButton creerBoutonIcone(Context ctx, String iconName) {
        ImageButton btn = new ImageButton(ctx);
        int resId = ctx.getResources().getIdentifier(iconName, "drawable", ctx.getPackageName());
        if (resId != 0) btn.setImageResource(resId);
        btn.setColorFilter(Palette.texteNormal);
        btn.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(40), 1f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        btn.setLayoutParams(lp);
        btn.setBackground(fond(Palette.fondNormal, Palette.bordure, 8));
        return btn;
    }
    
    private void updateBoutonsIcones(ImageButton[] boutons, int activeIndex) {
        for (int i = 0; i < boutons.length; i++) {
            boutons[i].setBackground(fond(i == activeIndex ? Palette.boutonNormal : Palette.fondNormal, Palette.bordure, 8));
        }
    }

    private TextWatcher creerWatcher(java.util.function.Consumer<String> action) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { if (!isUpdatingUI) action.accept(s.toString()); }
        };
    }

    private void chargerStyles() {
        listeStyles.clear();
        File fichier = new File(cheminProjet, "assets_ludexa/Textes/styles_titres.json");
        if (fichier.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(fichier));
                StringBuilder sb = new StringBuilder();
                String ligne;
                while ((ligne = br.readLine()) != null) sb.append(ligne);
                br.close();
                
                Type type = new TypeToken<List<StyleTitre>>(){}.getType();
                List<StyleTitre> charge = new Gson().fromJson(sb.toString(), type);
                if (charge != null) listeStyles.addAll(charge);
            } catch (Exception e) {}
        }
    }

    private void sauvegarderStyles() {
        File dir = new File(cheminProjet, "assets_ludexa/Textes");
        if (!dir.exists()) dir.mkdirs();
        File fichier = new File(dir, "styles_titres.json");
        try {
            FileWriter fw = new FileWriter(fichier);
            fw.write(new Gson().toJson(listeStyles));
            fw.close();
        } catch (Exception e) {}
    }
    
    private List<String> listerFichiersLocales(File dir, String cheminBase, String... extensions) {
        List<String> resultats = new ArrayList<>();
        if (dir != null && dir.exists() && dir.isDirectory()) {
            File[] fichiers = dir.listFiles();
            if (fichiers != null) {
                for (File f : fichiers) {
                    if (f.isDirectory()) {
                        resultats.addAll(listerFichiersLocales(f, cheminBase + f.getName() + "/", extensions));
                    } else {
                        String nom = f.getName().toLowerCase();
                        for (String ext : extensions) {
                            if (nom.endsWith(ext)) { resultats.add(cheminBase + f.getName()); break; }
                        }
                    }
                }
            }
        }
        return resultats;
    }
// bas 3
// haut 4
    private void afficherColorPicker(Context context, int couleurInitiale, java.util.function.Consumer<Integer> onColorSelected) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(Traducteur.get("insp_titre_select_couleur"));

        LinearLayout layoutMain = new LinearLayout(context);
        layoutMain.setOrientation(LinearLayout.VERTICAL);
        layoutMain.setPadding(dp(16), dp(16), dp(16), dp(16));
        layoutMain.setBackground(fond(Palette.fondPanneaux, Palette.bordure, 12));

        LinearLayout layoutTop = new LinearLayout(context);
        layoutTop.setOrientation(LinearLayout.HORIZONTAL);
        layoutTop.setGravity(Gravity.CENTER_VERTICAL);

        View previewColor = new View(context);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        previewParams.setMargins(0, 0, dp(12), 0);
        previewColor.setLayoutParams(previewParams);
        
        final float[] currentHsv = new float[3];
        Color.colorToHSV(couleurInitiale, currentHsv);
        
        android.graphics.drawable.GradientDrawable fondPreview = new android.graphics.drawable.GradientDrawable();
        fondPreview.setColor(couleurInitiale);
        fondPreview.setCornerRadius(dp(10));
        fondPreview.setStroke(dp(1), Palette.bordure);
        previewColor.setBackground(fondPreview);

        EditText champHex = new EditText(context);
        champHex.setSingleLine(true);
        champHex.setText(String.format("#%06X", (0xFFFFFF & couleurInitiale)));
        champHex.setTextColor(Palette.texteNormal);
        champHex.setHintTextColor(Palette.bordure);
        champHex.setBackground(fond(Palette.fondNormal, Palette.bordure, 10));
        champHex.setTextSize(14f);
        champHex.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        champHex.setLayoutParams(lp);
        champHex.setFilters(new android.text.InputFilter[] { new android.text.InputFilter.LengthFilter(7) });

        layoutTop.addView(previewColor);
        layoutTop.addView(champHex);
        layoutMain.addView(layoutTop);

        final boolean[] isUpdating = {false};

        View spectreView = new View(context) {
            private Paint paintHue = new Paint();
            private Paint paintVal = new Paint();
            private Paint indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override protected void onDraw(Canvas canvas) {
                int[] hueColors = {Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED};
                paintHue.setShader(new LinearGradient(0, 0, getWidth(), 0, hueColors, null, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, getWidth(), getHeight(), paintHue);
                paintVal.setShader(new LinearGradient(0, 0, 0, getHeight(), Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, getWidth(), getHeight(), paintVal);

                indicatorPaint.setColor(Color.WHITE);
                indicatorPaint.setStyle(Paint.Style.STROKE);
                indicatorPaint.setStrokeWidth(dp(2));
                
                float x = (currentHsv[0] / 360f) * getWidth();
                float y = (1f - currentHsv[2]) * getHeight();
                
                canvas.drawCircle(x, y, dp(10), indicatorPaint);
                indicatorPaint.setColor(Color.BLACK);
                canvas.drawCircle(x, y, dp(11), indicatorPaint);
            }

            @Override public boolean onTouchEvent(MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                    float x = Math.max(0, Math.min(event.getX(), getWidth()));
                    float y = Math.max(0, Math.min(event.getY(), getHeight()));
                    currentHsv[0] = (x / getWidth()) * 360f;
                    currentHsv[1] = 1f; currentHsv[2] = 1f - (y / getHeight());
                    int newColor = Color.HSVToColor(currentHsv);
                    
                    isUpdating[0] = true;
                    champHex.setText(String.format("#%06X", (0xFFFFFF & newColor)));
                    isUpdating[0] = false;
                    ((android.graphics.drawable.GradientDrawable)previewColor.getBackground()).setColor(newColor);
                    invalidate(); return true;
                }
                return super.onTouchEvent(event);
            }
        };
        
        LinearLayout.LayoutParams spectreParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(160));
        spectreParams.setMargins(0, dp(16), 0, dp(16));
        spectreView.setLayoutParams(spectreParams);
        layoutMain.addView(spectreView);

        champHex.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isUpdating[0]) return;
                try {
                    String hexStr = s.toString();
                    if (!hexStr.startsWith("#")) hexStr = "#" + hexStr;
                    if (hexStr.length() == 7) {
                        int parsedColor = Color.parseColor(hexStr);
                        Color.colorToHSV(parsedColor, currentHsv);
                        ((android.graphics.drawable.GradientDrawable)previewColor.getBackground()).setColor(parsedColor);
                        spectreView.invalidate();
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        });

        HorizontalScrollView scrollPalette = new HorizontalScrollView(context);
        scrollPalette.setHorizontalScrollBarEnabled(false);
        LinearLayout layoutPalette = new LinearLayout(context);
        layoutPalette.setOrientation(LinearLayout.HORIZONTAL);
        
        int[] couleursRapides = { Color.WHITE, Color.BLACK, Palette.texteSelectionne, Palette.boutonNormal, Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.parseColor("#FFA500"), Color.parseColor("#808080") };
        for (int c : couleursRapides) {
            View pastille = new View(context);
            LinearLayout.LayoutParams pastilleParams = new LinearLayout.LayoutParams(dp(40), dp(40));
            pastilleParams.setMargins(0, 0, dp(12), 0);
            pastille.setLayoutParams(pastilleParams);
            
            android.graphics.drawable.GradientDrawable bgPastille = new android.graphics.drawable.GradientDrawable();
            bgPastille.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            bgPastille.setColor(c);
            bgPastille.setStroke(dp(1), Palette.bordure);
            pastille.setBackground(bgPastille);
            
            pastille.setOnClickListener(vp -> {
                Color.colorToHSV(c, currentHsv);
                isUpdating[0] = true;
                champHex.setText(String.format("#%06X", (0xFFFFFF & c)));
                isUpdating[0] = false;
                ((android.graphics.drawable.GradientDrawable)previewColor.getBackground()).setColor(c);
                spectreView.invalidate();
            });
            layoutPalette.addView(pastille);
        }
        scrollPalette.addView(layoutPalette);
        layoutMain.addView(scrollPalette);

        builder.setView(layoutMain);
        builder.setPositiveButton(Traducteur.get("bouton_ok"), (dialog, which) -> {
            try {
                String finalHex = champHex.getText().toString();
                if (!finalHex.startsWith("#")) finalHex = "#" + finalHex;
                onColorSelected.accept(Color.parseColor(finalHex));
            } catch (Exception e) { onColorSelected.accept(Color.HSVToColor(currentHsv)); }
        });
        builder.setNegativeButton(Traducteur.get("bouton_annuler"), null);
        builder.show();
    }

    private void dessinerLigneDeTexte(Canvas canvas, String ligne, float x, float y, float courbure, Paint paint) {
        if (courbure != 0) {
            android.graphics.Path path = new android.graphics.Path();
            float txtLargeur = paint.measureText(ligne);
            float startX = x;
            if (paint.getTextAlign() == Paint.Align.CENTER) startX = x - (txtLargeur / 2f);
            else if (paint.getTextAlign() == Paint.Align.RIGHT) startX = x - txtLargeur;
            
            path.moveTo(startX, y);
            path.quadTo(startX + (txtLargeur / 2f), y + courbure, startX + txtLargeur, y);
            canvas.drawTextOnPath(ligne, path, 0, 0, paint);
        } else {
            canvas.drawText(ligne, x, y, paint);
        }
    }

    private class ApercuTexteView extends View {
        private Paint paintTexte;
        private Paint paintQuad;
        private Shader textShaderTexture = null;
        private String derniereTexture = null;

        public ApercuTexteView(Context context) {
            super(context);
            paintTexte = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintTexte.setTextSize(dp(50));
            
            paintQuad = new Paint();
            paintQuad.setColor(Palette.canvasGrille);
            paintQuad.setStyle(Paint.Style.STROKE);
            paintQuad.setStrokeWidth(1f);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Palette.canvasFond);
            
            int spacing = dp(20);
            for (int i = 0; i < getWidth(); i += spacing) canvas.drawLine(i, 0, i, getHeight(), paintQuad);
            for (int j = 0; j < getHeight(); j += spacing) canvas.drawLine(0, j, getWidth(), j, paintQuad);

            if (styleCourant == null) return;
            
            String texteTest = "Yop2D\nMoteur";
            
            paintTexte.setTypeface(policeApercu);
            paintTexte.setLetterSpacing(styleCourant.espacementLettres);
            paintTexte.setTextSkewX(styleCourant.inclinaison);

            float cx = getWidth() / 2f;
            float cyStart = (getHeight() / 2f) - ((paintTexte.descent() + paintTexte.ascent()) / 2f) - (paintTexte.getTextSize() / 2f);
            
            Paint.Align align = Paint.Align.LEFT;
            float xPos = dp(20); 
            if ("CENTRE".equals(styleCourant.alignement)) { align = Paint.Align.CENTER; xPos = cx; }
            else if ("DROITE".equals(styleCourant.alignement)) { align = Paint.Align.RIGHT; xPos = getWidth() - dp(20); }
            paintTexte.setTextAlign(align);

            if (styleCourant.cheminTexture != null && styleCourant.modeRemplissage.contains("TEXTURE")) {
                if (!styleCourant.cheminTexture.equals(derniereTexture) || textShaderTexture == null) {
                    try {
                        File imgFile = new File(cheminProjet, styleCourant.cheminTexture);
                        if (imgFile.exists()) {
                            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                            textShaderTexture = new android.graphics.BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
                            derniereTexture = styleCourant.cheminTexture;
                        }
                    } catch(Exception e){}
                }
            } else {
                textShaderTexture = null;
                derniereTexture = null;
            }

            float hauteurLigne = paintTexte.getTextSize() * styleCourant.multiplicateurLignes;
            String[] lignes = texteTest.split("\n");
            float currentY = cyStart;

            for (String ligne : lignes) {
                
                // PASSE 0 : NEON
                if (styleCourant.neonActif && styleCourant.neonRayon > 0) {
                    paintTexte.setStyle(Paint.Style.FILL);
                    paintTexte.setColor(styleCourant.neonCouleur);
                    paintTexte.setMaskFilter(new android.graphics.BlurMaskFilter(styleCourant.neonRayon, android.graphics.BlurMaskFilter.Blur.NORMAL));
                    dessinerLigneDeTexte(canvas, ligne, xPos, currentY, styleCourant.courbure, paintTexte);
                    paintTexte.setMaskFilter(null);
                }

                // PASSE 1 : OMBRE
                if (styleCourant.ombreActive) {
                    paintTexte.setStyle(Paint.Style.FILL);
                    paintTexte.setColor(Color.WHITE); 
                    paintTexte.setShadowLayer(styleCourant.ombreRayon, styleCourant.ombreDx, styleCourant.ombreDy, styleCourant.ombreCouleur);
                    dessinerLigneDeTexte(canvas, ligne, xPos, currentY, styleCourant.courbure, paintTexte);
                    paintTexte.clearShadowLayer();
                }

                // PASSE 2 : CONTOUR
                if (styleCourant.modeRemplissage.contains("STROKE")) {
                    paintTexte.setStyle(Paint.Style.STROKE);
                    paintTexte.setStrokeWidth(styleCourant.epaisseurContour);
                    paintTexte.setStrokeJoin(Paint.Join.ROUND);
                    paintTexte.setColor(styleCourant.couleurContour);
                    dessinerLigneDeTexte(canvas, ligne, xPos, currentY, styleCourant.courbure, paintTexte);
                }

                // PASSE 3 : REMPLISSAGE
                if (styleCourant.modeRemplissage.contains("FILL") || styleCourant.modeRemplissage.contains("TEXTURE")) {
                    
                    if (styleCourant.reliefActif) {
                        paintTexte.setStyle(Paint.Style.FILL);
                        paintTexte.setColor(Color.BLACK);
                        paintTexte.setAlpha(200);
                        dessinerLigneDeTexte(canvas, ligne, xPos + styleCourant.reliefElevation, currentY + styleCourant.reliefElevation, styleCourant.courbure, paintTexte);
                        
                        paintTexte.setColor(Color.WHITE);
                        paintTexte.setAlpha(200);
                        dessinerLigneDeTexte(canvas, ligne, xPos - (styleCourant.reliefElevation/2f), currentY - (styleCourant.reliefElevation/2f), styleCourant.courbure, paintTexte);
                    }

                    paintTexte.setStyle(Paint.Style.FILL);
                    paintTexte.setColor(Color.WHITE); 
                    paintTexte.setAlpha(255); 
                    
                    if (styleCourant.modeRemplissage.contains("TEXTURE") && textShaderTexture != null) {
                        paintTexte.setShader(textShaderTexture);
                    } else if (styleCourant.utiliserDegrade) {
                        Shader textShader = new LinearGradient(0, currentY - paintTexte.getTextSize(), 0, currentY,
                                new int[]{styleCourant.couleurDegrade1, styleCourant.couleurDegrade2},
                                null, Shader.TileMode.CLAMP);
                        paintTexte.setShader(textShader);
                    }
                    
                    dessinerLigneDeTexte(canvas, ligne, xPos, currentY, styleCourant.courbure, paintTexte);
                    paintTexte.setShader(null);
                }
                
                currentY += hauteurLigne;
            }
        }
    }
}
// bas 4






