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
    private Spinner spinnerAlignement;
    private EditText champEspacementLettres, champEspacementLignes;
    private EditText champInclinaison, champCourbure;
    
    private Spinner spinnerRemplissage;
    private Button btnTexture;
    private EditText champEpaisseur;
    private Button btnCouleurContour;
    private CheckBox cbDegrade;
    private Button btnCouleurDeg1, btnCouleurDeg2;
    
    private CheckBox cbOmbre;
    private EditText champOmbreRayon, champOmbreDx, champOmbreDy;
    private Button btnCouleurOmbre;
    
    private CheckBox cbNeon;
    private EditText champNeonRayon;
    private Button btnCouleurNeon;
    
    private CheckBox cbRelief;
    private EditText champReliefElevation;

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
        btnNouveau.setBackground(fond(Color.parseColor("#2E7D46"), Palette.bordure, 10));
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

        spinnerAlignement = new Spinner(context);
        ArrayAdapter<String> adapterAlign = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, new String[]{"GAUCHE", "CENTRE", "DROITE"}) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent); tv.setTextColor(Palette.texteNormal); return tv;
            }
            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent); tv.setTextColor(Palette.texteNormal); tv.setBackgroundColor(Palette.fondNormal); tv.setPadding(dp(16), dp(16), dp(16), dp(16)); return tv;
            }
        };
        spinnerAlignement.setBackground(fond(Palette.fondListe, Palette.bordure, 10));
        spinnerAlignement.setPadding(dp(10), dp(10), dp(10), dp(10));
        spinnerAlignement.setAdapter(adapterAlign);
        spinnerAlignement.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isUpdatingUI) { styleCourant.alignement = (String) parent.getItemAtPosition(position); vueApercu.invalidate(); }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        contenuDroit.addView(spinnerAlignement);
        // 3. ESPACEMENT & DEFORMATION
        ajouterSeparateur(context, contenuDroit, Traducteur.get("style_sep_espacement"));

        champEspacementLettres = new EditText(context);
        champEspacementLettres.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        champEspacementLettres.addTextChangedListener(creerWatcher(texte -> { try { styleCourant.espacementLettres = Float.parseFloat(texte); vueApercu.invalidate(); } catch(Exception e){} }));
        contenuDroit.addView(genererLigneLabelChamp(context, Traducteur.get("style_label_lettres"), champEspacementLettres));

        champEspacementLignes = new EditText(context);
        champEspacementLignes.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        champEspacementLignes.addTextChangedListener(creerWatcher(texte -> { try { styleCourant.multiplicateurLignes = Float.parseFloat(texte); vueApercu.invalidate(); } catch(Exception e){} }));
        contenuDroit.addView(genererLigneLabelChamp(context, Traducteur.get("style_label_lignes"), champEspacementLignes));
// bas 1
// haut 2
        
        champInclinaison = new EditText(context);
        champInclinaison.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        champInclinaison.addTextChangedListener(creerWatcher(texte -> { try { styleCourant.inclinaison = Float.parseFloat(texte); vueApercu.invalidate(); } catch(Exception e){} }));
        contenuDroit.addView(genererLigneLabelChamp(context, Traducteur.get("style_label_inclinaison"), champInclinaison));

        champCourbure = new EditText(context);
        champCourbure.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        champCourbure.addTextChangedListener(creerWatcher(texte -> { try { styleCourant.courbure = Float.parseFloat(texte); vueApercu.invalidate(); } catch(Exception e){} }));
        contenuDroit.addView(genererLigneLabelChamp(context, Traducteur.get("style_label_courbure"), champCourbure));

        // 4. REMPLISSAGE (Texture & Couleur)
        ajouterSeparateur(context, contenuDroit, Traducteur.get("style_sep_remplissage"));
        
        spinnerRemplissage = new Spinner(context);
        ArrayAdapter<String> adapterRemplissage = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, new String[]{"FILL_AND_STROKE", "FILL", "STROKE", "TEXTURE_AND_STROKE", "TEXTURE"}) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent); tv.setTextColor(Palette.texteNormal); return tv;
            }
            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent); tv.setTextColor(Palette.texteNormal); tv.setBackgroundColor(Palette.fondNormal); tv.setPadding(dp(16), dp(16), dp(16), dp(16)); return tv;
            }
        };
        spinnerRemplissage.setBackground(fond(Palette.fondListe, Palette.bordure, 10));
        spinnerRemplissage.setPadding(dp(10), dp(10), dp(10), dp(10));
        spinnerRemplissage.setAdapter(adapterRemplissage);
        spinnerRemplissage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isUpdatingUI) { styleCourant.modeRemplissage = (String) parent.getItemAtPosition(position); vueApercu.invalidate(); }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        contenuDroit.addView(spinnerRemplissage);

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

        // 5. CONTOUR
        ajouterSeparateur(context, contenuDroit, Traducteur.get("style_sep_contour"));
        champEpaisseur = new EditText(context);
        champEpaisseur.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        champEpaisseur.addTextChangedListener(creerWatcher(texte -> { try { styleCourant.epaisseurContour = Float.parseFloat(texte); vueApercu.invalidate(); } catch(Exception e){} }));
        contenuDroit.addView(genererLigneLabelChamp(context, Traducteur.get("style_label_epaisseur"), champEpaisseur));

        btnCouleurContour = new Button(context);
        btnCouleurContour.setText(Traducteur.get("style_btn_couleur_contour"));
        btnCouleurContour.setTextColor(Palette.texteNormal);
        btnCouleurContour.setTextSize(13f);
        btnCouleurContour.setAllCaps(false);
        btnCouleurContour.setOnClickListener(v -> afficherColorPicker(context, styleCourant.couleurContour, couleur -> { styleCourant.couleurContour = couleur; rafraichirUI(); }));
        contenuDroit.addView(btnCouleurContour);

        // 6. DEGRADE
        ajouterSeparateur(context, contenuDroit, Traducteur.get("style_sep_degrade"));
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

        // 7. NEON (NOUVEAU)
        ajouterSeparateur(context, contenuDroit, Traducteur.get("style_sep_neon"));
        cbNeon = new CheckBox(context);
        cbNeon.setText(Traducteur.get("style_cb_neon"));
        cbNeon.setTextColor(Palette.texteNormal);
        cbNeon.setTextSize(14f);
        cbNeon.setPadding(dp(8), dp(10), dp(8), dp(10));
        cbNeon.setOnCheckedChangeListener((btn, isChecked) -> { if(!isUpdatingUI){ styleCourant.neonActif = isChecked; vueApercu.invalidate(); }});
        contenuDroit.addView(cbNeon);

        champNeonRayon = new EditText(context);
        champNeonRayon.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        champNeonRayon.addTextChangedListener(creerWatcher(texte -> { try { styleCourant.neonRayon = Float.parseFloat(texte); vueApercu.invalidate(); } catch(Exception e){} }));
        contenuDroit.addView(genererLigneLabelChamp(context, Traducteur.get("style_label_rayon"), champNeonRayon));

        btnCouleurNeon = new Button(context);
        btnCouleurNeon.setText(Traducteur.get("style_btn_couleur_neon"));
        btnCouleurNeon.setTextColor(Palette.texteNormal);
        btnCouleurNeon.setTextSize(13f);
        btnCouleurNeon.setAllCaps(false);
        btnCouleurNeon.setOnClickListener(v -> afficherColorPicker(context, styleCourant.neonCouleur, couleur -> { styleCourant.neonCouleur = couleur; rafraichirUI(); }));
        contenuDroit.addView(btnCouleurNeon);

        // 8. OMBRE
        ajouterSeparateur(context, contenuDroit, Traducteur.get("style_sep_ombre"));
        cbOmbre = new CheckBox(context);
        cbOmbre.setText(Traducteur.get("style_cb_ombre"));
        cbOmbre.setTextColor(Palette.texteNormal);
        cbOmbre.setTextSize(14f);
        cbOmbre.setPadding(dp(8), dp(10), dp(8), dp(10));
        cbOmbre.setOnCheckedChangeListener((btn, isChecked) -> { if(!isUpdatingUI){ styleCourant.ombreActive = isChecked; vueApercu.invalidate(); }});
        contenuDroit.addView(cbOmbre);

        champOmbreRayon = new EditText(context);
        champOmbreRayon.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        champOmbreRayon.addTextChangedListener(creerWatcher(texte -> { try { styleCourant.ombreRayon = Float.parseFloat(texte); vueApercu.invalidate(); } catch(Exception e){} }));
        contenuDroit.addView(genererLigneLabelChamp(context, Traducteur.get("style_label_rayon"), champOmbreRayon));

        champOmbreDx = new EditText(context);
        champOmbreDx.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        champOmbreDx.addTextChangedListener(creerWatcher(texte -> { try { styleCourant.ombreDx = Float.parseFloat(texte); vueApercu.invalidate(); } catch(Exception e){} }));
        contenuDroit.addView(genererLigneLabelChamp(context, Traducteur.get("style_label_dx"), champOmbreDx));

        champOmbreDy = new EditText(context);
        champOmbreDy.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        champOmbreDy.addTextChangedListener(creerWatcher(texte -> { try { styleCourant.ombreDy = Float.parseFloat(texte); vueApercu.invalidate(); } catch(Exception e){} }));
        contenuDroit.addView(genererLigneLabelChamp(context, Traducteur.get("style_label_dy"), champOmbreDy));

        btnCouleurOmbre = new Button(context);
        btnCouleurOmbre.setText(Traducteur.get("style_btn_couleur_ombre"));
        btnCouleurOmbre.setTextColor(Palette.texteNormal);
        btnCouleurOmbre.setTextSize(13f);
        btnCouleurOmbre.setAllCaps(false);
        btnCouleurOmbre.setOnClickListener(v -> afficherColorPicker(context, styleCourant.ombreCouleur, couleur -> { styleCourant.ombreCouleur = couleur; rafraichirUI(); }));
        contenuDroit.addView(btnCouleurOmbre);
        
        // 9. RELIEF (NOUVEAU)
        ajouterSeparateur(context, contenuDroit, Traducteur.get("style_sep_relief"));
        cbRelief = new CheckBox(context);
        cbRelief.setText(Traducteur.get("style_cb_relief"));
        cbRelief.setTextColor(Palette.texteNormal);
        cbRelief.setTextSize(14f);
        cbRelief.setPadding(dp(8), dp(10), dp(8), dp(10));
        cbRelief.setOnCheckedChangeListener((btn, isChecked) -> { if(!isUpdatingUI){ styleCourant.reliefActif = isChecked; vueApercu.invalidate(); }});
        contenuDroit.addView(cbRelief);
        
        champReliefElevation = new EditText(context);
        champReliefElevation.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        champReliefElevation.addTextChangedListener(creerWatcher(texte -> { try { styleCourant.reliefElevation = Float.parseFloat(texte); vueApercu.invalidate(); } catch(Exception e){} }));
        contenuDroit.addView(genererLigneLabelChamp(context, Traducteur.get("style_label_elevation"), champReliefElevation));

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
    private void rafraichirUI() {
        isUpdatingUI = true;
        
        champNomStyle.setText(styleCourant.nom);
        champEspacementLettres.setText(String.valueOf(styleCourant.espacementLettres));
        champEspacementLignes.setText(String.valueOf(styleCourant.multiplicateurLignes));
        champInclinaison.setText(String.valueOf(styleCourant.inclinaison));
        champCourbure.setText(String.valueOf(styleCourant.courbure));
        
        int spinnerAlignPos = 1; // CENTRE par defaut
        if ("GAUCHE".equals(styleCourant.alignement)) spinnerAlignPos = 0;
        if ("DROITE".equals(styleCourant.alignement)) spinnerAlignPos = 2;
        spinnerAlignement.setSelection(spinnerAlignPos);
        
        int spinnerPos = 0;
        if (styleCourant.modeRemplissage.equals("FILL")) spinnerPos = 1;
        if (styleCourant.modeRemplissage.equals("STROKE")) spinnerPos = 2;
        if (styleCourant.modeRemplissage.equals("TEXTURE_AND_STROKE")) spinnerPos = 3;
        if (styleCourant.modeRemplissage.equals("TEXTURE")) spinnerPos = 4;
        spinnerRemplissage.setSelection(spinnerPos);
// bas 2
// haut 3
        
        champEpaisseur.setText(String.valueOf(styleCourant.epaisseurContour));
        cbDegrade.setChecked(styleCourant.utiliserDegrade);
        
        cbNeon.setChecked(styleCourant.neonActif);
        champNeonRayon.setText(String.valueOf(styleCourant.neonRayon));
        
        cbOmbre.setChecked(styleCourant.ombreActive);
        champOmbreRayon.setText(String.valueOf(styleCourant.ombreRayon));
        champOmbreDx.setText(String.valueOf(styleCourant.ombreDx));
        champOmbreDy.setText(String.valueOf(styleCourant.ombreDy));
        
        cbRelief.setChecked(styleCourant.reliefActif);
        champReliefElevation.setText(String.valueOf(styleCourant.reliefElevation));

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

    private void ajouterSeparateur(Context ctx, LinearLayout parent, String texte) {
        TextView sep = new TextView(ctx);
        sep.setText(texte);
        sep.setTextColor(Palette.texteSelectionne);
        sep.setTypeface(null, android.graphics.Typeface.BOLD);
        sep.setTextSize(15f);
        sep.setBackground(fond(Palette.enTeteDialogues, Palette.bordure, 8));
        sep.setPadding(dp(10), dp(8), dp(10), dp(8));
        parent.addView(sep);
    }

    private LinearLayout genererLigneLabelChamp(Context ctx, String label, EditText champ) {
        LinearLayout ligne = new LinearLayout(ctx);
        ligne.setOrientation(LinearLayout.HORIZONTAL);
        ligne.setGravity(Gravity.CENTER_VERTICAL);
        ligne.setPadding(0, dp(6), 0, dp(6));
        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextColor(Palette.texteNormal);
        tv.setTextSize(13f);
        tv.setLayoutParams(new LinearLayout.LayoutParams(dp(80), ViewGroup.LayoutParams.WRAP_CONTENT));
        
        champ.setTextColor(Palette.texteNormal);
        champ.setHintTextColor(Palette.bordure);
        champ.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        champ.setTextSize(14f);
        champ.setBackground(fond(Palette.canvasFond, Palette.bordure, 10));
        champ.setPadding(dp(10), dp(10), dp(10), dp(10));
        
        ligne.addView(tv); ligne.addView(champ);
        return ligne;
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
// bas 3

  // haut 4
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
// a modifier
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

                // PASSE 3 : REMPLISSAGE (AVEC RELIEF MANUEL 3D !)
                if (styleCourant.modeRemplissage.contains("FILL") || styleCourant.modeRemplissage.contains("TEXTURE")) {
                    
                    // --- NOUVEAU RELIEF MANUEL ---
                    if (styleCourant.reliefActif) {
                        // Extrusion/Ombre portée solide (bas-droite)
                        paintTexte.setStyle(Paint.Style.FILL);
                        paintTexte.setColor(Color.BLACK);
                        paintTexte.setAlpha(200);
                        dessinerLigneDeTexte(canvas, ligne, xPos + styleCourant.reliefElevation, currentY + styleCourant.reliefElevation, styleCourant.courbure, paintTexte);
                        
                        // Lumière biseau (haut-gauche)
                        paintTexte.setColor(Color.WHITE);
                        paintTexte.setAlpha(200);
                        dessinerLigneDeTexte(canvas, ligne, xPos - (styleCourant.reliefElevation/2f), currentY - (styleCourant.reliefElevation/2f), styleCourant.courbure, paintTexte);
                    }

                    // Couleur normale du texte
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


        

    
