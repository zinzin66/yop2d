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

    private LinearLayout panelGauche;
    private ScrollView panelDroit;
    private ApercuTexteView vueApercu;

    // Champs de l'interface droite
    private Spinner spinnerStyles;
    private EditText champNomStyle;
    private Spinner spinnerRemplissage;
    private EditText champEpaisseur;
    private Button btnCouleurContour;
    private CheckBox cbDegrade;
    private Button btnCouleurDeg1, btnCouleurDeg2;
    private CheckBox cbOmbre;
    private EditText champOmbreRayon, champOmbreDx, champOmbreDy;
    private Button btnCouleurOmbre;

    private boolean isUpdatingUI = false; // Anti-boucle infinie

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
        root.setPadding(dp(8), dp(8), dp(8), dp(8));

        // --- GAUCHE (70%) : APERÇU ---
        panelGauche = new LinearLayout(context);
        panelGauche.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpGauche = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.7f);
        lpGauche.setMargins(0, 0, dp(8), 0);
        panelGauche.setLayoutParams(lpGauche);
        panelGauche.setBackground(fond(Palette.fondNormal, Palette.bordure, 12));
        
        TextView titreApercu = new TextView(context);
        titreApercu.setText(Traducteur.get("style_apercu"));
        titreApercu.setTextColor(Palette.texteSelectionne);
        titreApercu.setPadding(dp(12), dp(12), dp(12), dp(12));
        panelGauche.addView(titreApercu);

        vueApercu = new ApercuTexteView(context);
        vueApercu.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        panelGauche.addView(vueApercu);

        // --- DROITE (30%) : RÉGLAGES ---
        panelDroit = new ScrollView(context);
        panelDroit.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.3f));
        panelDroit.setBackground(fond(Palette.fondNormal, Palette.bordure, 12));

        LinearLayout contenuDroit = new LinearLayout(context);
        contenuDroit.setOrientation(LinearLayout.VERTICAL);
        contenuDroit.setPadding(dp(12), dp(12), dp(12), dp(12));

        // 1. SÉLECTION DU STYLE
        Button btnNouveau = new Button(context);
        btnNouveau.setText(Traducteur.get("style_btn_nouveau"));
        btnNouveau.setBackground(fond(Color.parseColor("#4CAF50"), Palette.bordure, 8));
        btnNouveau.setTextColor(Color.WHITE);
        btnNouveau.setOnClickListener(v -> {
            StyleTitre nv = new StyleTitre(Traducteur.get("style_nouveau_defaut") + " " + (listeStyles.size() + 1));
            listeStyles.add(nv);
            styleCourant = nv;
            rafraichirSpinnerStyles();
            rafraichirUI();
        });
        contenuDroit.addView(btnNouveau);

        spinnerStyles = new Spinner(context);
        spinnerStyles.setBackground(fond(Palette.fondListe, Palette.bordure, 8));
        spinnerStyles.setPadding(0, dp(8), 0, dp(8));
        contenuDroit.addView(spinnerStyles);
        
        spinnerStyles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isUpdatingUI) {
                    styleCourant = listeStyles.get(position);
                    rafraichirUI();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 2. NOM DU STYLE
        champNomStyle = new EditText(context);
        champNomStyle.setTextColor(Palette.texteNormal);
        champNomStyle.setHint(Traducteur.get("style_hint_nom"));
        champNomStyle.addTextChangedListener(creerWatcher(texte -> { styleCourant.nom = texte; rafraichirSpinnerStylesSansTrigger(); }));
        contenuDroit.addView(genererLigneLabelChamp(context, Traducteur.get("style_label_nom"), champNomStyle));

        // 3. REMPLISSAGE
        ajouterSeparateur(context, contenuDroit, Traducteur.get("style_sep_remplissage"));
        
        spinnerRemplissage = new Spinner(context);
        ArrayAdapter<String> adapterRemplissage = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, 
            new String[]{"FILL_AND_STROKE", "FILL", "STROKE"});
        spinnerRemplissage.setAdapter(adapterRemplissage);
        spinnerRemplissage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isUpdatingUI) { styleCourant.modeRemplissage = (String) parent.getItemAtPosition(position); vueApercu.invalidate(); }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        contenuDroit.addView(spinnerRemplissage);

        // 4. CONTOUR
        ajouterSeparateur(context, contenuDroit, Traducteur.get("style_sep_contour"));
        champEpaisseur = new EditText(context);
        champEpaisseur.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        champEpaisseur.addTextChangedListener(creerWatcher(texte -> { try { styleCourant.epaisseurContour = Float.parseFloat(texte); vueApercu.invalidate(); } catch(Exception e){} }));
        contenuDroit.addView(genererLigneLabelChamp(context, Traducteur.get("style_label_epaisseur"), champEpaisseur));

        btnCouleurContour = new Button(context);
        btnCouleurContour.setText(Traducteur.get("style_btn_couleur_contour"));
        btnCouleurContour.setOnClickListener(v -> afficherColorPicker(context, styleCourant.couleurContour, couleur -> { styleCourant.couleurContour = couleur; rafraichirUI(); }));
        contenuDroit.addView(btnCouleurContour);

        // 5. DEGRADE
        ajouterSeparateur(context, contenuDroit, Traducteur.get("style_sep_degrade"));
        cbDegrade = new CheckBox(context);
        cbDegrade.setText(Traducteur.get("style_cb_degrade"));
        cbDegrade.setTextColor(Palette.texteNormal);
        cbDegrade.setOnCheckedChangeListener((btn, isChecked) -> { if(!isUpdatingUI){ styleCourant.utiliserDegrade = isChecked; vueApercu.invalidate(); }});
        contenuDroit.addView(cbDegrade);

        btnCouleurDeg1 = new Button(context);
        btnCouleurDeg1.setText(Traducteur.get("style_btn_deg_haut"));
        btnCouleurDeg1.setOnClickListener(v -> afficherColorPicker(context, styleCourant.couleurDegrade1, couleur -> { styleCourant.couleurDegrade1 = couleur; rafraichirUI(); }));
        contenuDroit.addView(btnCouleurDeg1);

        btnCouleurDeg2 = new Button(context);
        btnCouleurDeg2.setText(Traducteur.get("style_btn_deg_bas"));
        btnCouleurDeg2.setOnClickListener(v -> afficherColorPicker(context, styleCourant.couleurDegrade2, couleur -> { styleCourant.couleurDegrade2 = couleur; rafraichirUI(); }));
        contenuDroit.addView(btnCouleurDeg2);

        // 6. OMBRE
        ajouterSeparateur(context, contenuDroit, Traducteur.get("style_sep_ombre"));
        cbOmbre = new CheckBox(context);
        cbOmbre.setText(Traducteur.get("style_cb_ombre"));
        cbOmbre.setTextColor(Palette.texteNormal);
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
        btnCouleurOmbre.setOnClickListener(v -> afficherColorPicker(context, styleCourant.ombreCouleur, couleur -> { styleCourant.ombreCouleur = couleur; rafraichirUI(); }));
        contenuDroit.addView(btnCouleurOmbre);

        panelDroit.addView(contenuDroit);

        root.addView(panelGauche);
        root.addView(panelDroit);

        LinearLayout layoutPrincipal = new LinearLayout(context);
        layoutPrincipal.setOrientation(LinearLayout.VERTICAL);
        layoutPrincipal.setBackgroundColor(Palette.fondPanneaux);
        layoutPrincipal.addView(root, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button btnFermer = new Button(context);
        btnFermer.setText(Traducteur.get("bouton_fermer_sauvegarder"));
        btnFermer.setBackground(fond(Color.parseColor("#3F51B5"), Palette.bordure, 8));
        btnFermer.setTextColor(Color.WHITE);
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
// bas 1

// haut 2
    private void rafraichirUI() {
        isUpdatingUI = true;
        
        champNomStyle.setText(styleCourant.nom);
        
        int spinnerPos = 0;
        if (styleCourant.modeRemplissage.equals("FILL")) spinnerPos = 1;
        if (styleCourant.modeRemplissage.equals("STROKE")) spinnerPos = 2;
        spinnerRemplissage.setSelection(spinnerPos);
        
        champEpaisseur.setText(String.valueOf(styleCourant.epaisseurContour));
        cbDegrade.setChecked(styleCourant.utiliserDegrade);
        cbOmbre.setChecked(styleCourant.ombreActive);
        champOmbreRayon.setText(String.valueOf(styleCourant.ombreRayon));
        champOmbreDx.setText(String.valueOf(styleCourant.ombreDx));
        champOmbreDy.setText(String.valueOf(styleCourant.ombreDy));

        // Mise à jour visuelle des boutons de couleurs
        btnCouleurContour.setBackgroundColor(styleCourant.couleurContour);
        btnCouleurDeg1.setBackgroundColor(styleCourant.couleurDegrade1);
        btnCouleurDeg2.setBackgroundColor(styleCourant.couleurDegrade2);
        btnCouleurOmbre.setBackgroundColor(styleCourant.ombreCouleur);

        isUpdatingUI = false;
        vueApercu.invalidate();
    }

    private void rafraichirSpinnerStyles() {
        isUpdatingUI = true;
        List<String> noms = new ArrayList<>();
        for (StyleTitre st : listeStyles) noms.add(st.nom);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, noms);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStyles.setAdapter(adapter);
        spinnerStyles.setSelection(listeStyles.indexOf(styleCourant));
        isUpdatingUI = false;
    }

    private void rafraichirSpinnerStylesSansTrigger() {
        int currentIndex = spinnerStyles.getSelectedItemPosition();
        List<String> noms = new ArrayList<>();
        for (StyleTitre st : listeStyles) noms.add(st.nom);
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerStyles.getAdapter();
        adapter.clear();
        adapter.addAll(noms);
        adapter.notifyDataSetChanged();
        spinnerStyles.setSelection(currentIndex, false);
    }

    private void ajouterSeparateur(Context ctx, LinearLayout parent, String texte) {
        TextView sep = new TextView(ctx);
        sep.setText(texte);
        sep.setTextColor(Palette.texteSelectionne);
        sep.setTypeface(null, android.graphics.Typeface.BOLD);
        sep.setPadding(0, dp(16), 0, dp(8));
        parent.addView(sep);
    }

    private LinearLayout genererLigneLabelChamp(Context ctx, String label, EditText champ) {
        LinearLayout ligne = new LinearLayout(ctx);
        ligne.setOrientation(LinearLayout.HORIZONTAL);
        ligne.setGravity(Gravity.CENTER_VERTICAL);
        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextColor(Palette.texteNormal);
        tv.setLayoutParams(new LinearLayout.LayoutParams(dp(80), ViewGroup.LayoutParams.WRAP_CONTENT));
        champ.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        champ.setBackground(fond(Palette.canvasFond, Palette.bordure, 8));
        champ.setPadding(dp(8), dp(8), dp(8), dp(8));
        ligne.addView(tv);
        ligne.addView(champ);
        return ligne;
    }

    private TextWatcher creerWatcher(java.util.function.Consumer<String> action) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isUpdatingUI) action.accept(s.toString());
            }
        };
    }
// bas 2

// haut 3
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

    // Mini-ColorPicker robuste intégré
    private void afficherColorPicker(Context context, int couleurInitiale, java.util.function.Consumer<Integer> onColorSelected) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(Traducteur.get("insp_titre_select_couleur"));
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));
        
        final float[] hsv = new float[3];
        Color.colorToHSV(couleurInitiale, hsv);
        
        View preview = new View(context);
        preview.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        preview.setBackgroundColor(couleurInitiale);
        layout.addView(preview);
        
        View spectre = new View(context) {
            Paint pHue = new Paint(), pVal = new Paint();
            @Override protected void onDraw(Canvas canvas) {
                int[] hueColors = {Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED};
                pHue.setShader(new LinearGradient(0, 0, getWidth(), 0, hueColors, null, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, getWidth(), getHeight(), pHue);
                pVal.setShader(new LinearGradient(0, 0, 0, getHeight(), Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, getWidth(), getHeight(), pVal);
            }
            @Override public boolean onTouchEvent(MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                    hsv[0] = (Math.max(0, Math.min(event.getX(), getWidth())) / getWidth()) * 360f;
                    hsv[1] = 1f; 
                    hsv[2] = 1f - (Math.max(0, Math.min(event.getY(), getHeight())) / getHeight());
                    preview.setBackgroundColor(Color.HSVToColor(hsv));
                    return true;
                }
                return super.onTouchEvent(event);
            }
        };
        spectre.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(200)));
        layout.addView(spectre);
        
        builder.setView(layout);
        builder.setPositiveButton("OK", (dialog, which) -> onColorSelected.accept(Color.HSVToColor(hsv)));
        builder.setNegativeButton(Traducteur.get("bouton_annuler"), null);
        builder.show();
    }

    // --- LA VUE D'APERÇU SUR MESURE ---
    private class ApercuTexteView extends View {
        private Paint paintTexte;
        private Paint paintQuad;

        public ApercuTexteView(Context context) {
            super(context);
            paintTexte = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintTexte.setTextSize(dp(60));
            paintTexte.setTextAlign(Paint.Align.CENTER);
            
            paintQuad = new Paint();
            paintQuad.setColor(Color.parseColor("#333333"));
            paintQuad.setStyle(Paint.Style.STROKE);
            paintQuad.setStrokeWidth(1f);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Color.parseColor("#1E1E1E")); // Fond grille sombre
            
            int spacing = dp(20);
            for (int i = 0; i < getWidth(); i += spacing) canvas.drawLine(i, 0, i, getHeight(), paintQuad);
            for (int j = 0; j < getHeight(); j += spacing) canvas.drawLine(0, j, getWidth(), j, paintQuad);

            if (styleCourant == null) return;
            String texteTest = "Yop2D";
            float cx = getWidth() / 2f;
            float cy = (getHeight() / 2f) - ((paintTexte.descent() + paintTexte.ascent()) / 2f);

            // OMBRE
            if (styleCourant.ombreActive) {
                paintTexte.setShadowLayer(styleCourant.ombreRayon, styleCourant.ombreDx, styleCourant.ombreDy, styleCourant.ombreCouleur);
            } else {
                paintTexte.clearShadowLayer();
            }

            // CONTOUR
            if ("STROKE".equals(styleCourant.modeRemplissage) || "FILL_AND_STROKE".equals(styleCourant.modeRemplissage)) {
                paintTexte.setStyle(Paint.Style.STROKE);
                paintTexte.setStrokeWidth(styleCourant.epaisseurContour);
                paintTexte.setStrokeJoin(Paint.Join.ROUND);
                paintTexte.setColor(styleCourant.couleurContour);
                canvas.drawText(texteTest, cx, cy, paintTexte);
            }

            paintTexte.clearShadowLayer();

            // REMPLISSAGE
            if ("FILL".equals(styleCourant.modeRemplissage) || "FILL_AND_STROKE".equals(styleCourant.modeRemplissage)) {
                paintTexte.setStyle(Paint.Style.FILL);
                if (styleCourant.utiliserDegrade) {
                    Shader textShader = new LinearGradient(cx, cy - paintTexte.getTextSize(), cx, cy,
                            new int[]{styleCourant.couleurDegrade1, styleCourant.couleurDegrade2},
                            null, Shader.TileMode.CLAMP);
                    paintTexte.setShader(textShader);
                } else {
                    paintTexte.setShader(null);
                    paintTexte.setColor(Color.WHITE); // Couleur par défaut dans l'éditeur (car géré par l'objet sur le canvas)
                }
                canvas.drawText(texteTest, cx, cy, paintTexte);
                paintTexte.setShader(null);
            }
        }
    }
}
// bas 3
