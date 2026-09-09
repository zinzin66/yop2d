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

    private Spinner spinnerStyles;
    private EditText champNomStyle;
    private EditText champEspacementLettres, champEspacementLignes; // NOUVEAU
    private Spinner spinnerRemplissage;
    private EditText champEpaisseur;
    private Button btnCouleurContour;
    private CheckBox cbDegrade;
    private Button btnCouleurDeg1, btnCouleurDeg2;
    private CheckBox cbOmbre;
    private EditText champOmbreRayon, champOmbreDx, champOmbreDy;
    private Button btnCouleurOmbre;

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
        root.setPadding(dp(8), dp(8), dp(8), dp(8));

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

        panelDroit = new ScrollView(context);
        panelDroit.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.3f));
        panelDroit.setBackground(fond(Palette.fondNormal, Palette.bordure, 12));

        LinearLayout contenuDroit = new LinearLayout(context);
        contenuDroit.setOrientation(LinearLayout.VERTICAL);
        contenuDroit.setPadding(dp(12), dp(12), dp(12), dp(12));

        // 1. SÉLECTION
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

        // 2. NOM
        champNomStyle = new EditText(context);
        champNomStyle.setHint(Traducteur.get("style_hint_nom"));
        champNomStyle.addTextChangedListener(creerWatcher(texte -> { styleCourant.nom = texte; rafraichirSpinnerStylesSansTrigger(); }));
        contenuDroit.addView(genererLigneLabelChamp(context, Traducteur.get("style_label_nom"), champNomStyle));

        // 3. ESPACEMENT (NOUVEAU)
        ajouterSeparateur(context, contenuDroit, "Espacement");

        champEspacementLettres = new EditText(context);
        champEspacementLettres.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        champEspacementLettres.addTextChangedListener(creerWatcher(texte -> { try { styleCourant.espacementLettres = Float.parseFloat(texte); vueApercu.invalidate(); } catch(Exception e){} }));
        contenuDroit.addView(genererLigneLabelChamp(context, "Lettres", champEspacementLettres));

        champEspacementLignes = new EditText(context);
        champEspacementLignes.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        champEspacementLignes.addTextChangedListener(creerWatcher(texte -> { try { styleCourant.multiplicateurLignes = Float.parseFloat(texte); vueApercu.invalidate(); } catch(Exception e){} }));
        contenuDroit.addView(genererLigneLabelChamp(context, "Lignes", champEspacementLignes));

        // 4. REMPLISSAGE
        ajouterSeparateur(context, contenuDroit, Traducteur.get("style_sep_remplissage"));
        
        spinnerRemplissage = new Spinner(context);
        ArrayAdapter<String> adapterRemplissage = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, new String[]{"FILL_AND_STROKE", "FILL", "STROKE"}) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(Palette.texteNormal);
                return tv;
            }
            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(Palette.texteNormal);
                tv.setBackgroundColor(Palette.fondNormal);
                tv.setPadding(dp(16), dp(16), dp(16), dp(16));
                return tv;
            }
        };
        spinnerRemplissage.setAdapter(adapterRemplissage);
        spinnerRemplissage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isUpdatingUI) { styleCourant.modeRemplissage = (String) parent.getItemAtPosition(position); vueApercu.invalidate(); }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        contenuDroit.addView(spinnerRemplissage);

        // 5. CONTOUR
        ajouterSeparateur(context, contenuDroit, Traducteur.get("style_sep_contour"));
        champEpaisseur = new EditText(context);
        champEpaisseur.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        champEpaisseur.addTextChangedListener(creerWatcher(texte -> { try { styleCourant.epaisseurContour = Float.parseFloat(texte); vueApercu.invalidate(); } catch(Exception e){} }));
        contenuDroit.addView(genererLigneLabelChamp(context, Traducteur.get("style_label_epaisseur"), champEpaisseur));

        btnCouleurContour = new Button(context);
        btnCouleurContour.setText(Traducteur.get("style_btn_couleur_contour"));
        btnCouleurContour.setOnClickListener(v -> afficherColorPicker(context, styleCourant.couleurContour, couleur -> { styleCourant.couleurContour = couleur; rafraichirUI(); }));
        contenuDroit.addView(btnCouleurContour);

        // 6. DEGRADE
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

        // 7. OMBRE
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
        champEspacementLettres.setText(String.valueOf(styleCourant.espacementLettres));
        champEspacementLignes.setText(String.valueOf(styleCourant.multiplicateurLignes));
        
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
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, noms) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(Palette.texteNormal);
                return tv;
            }
            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(Palette.texteNormal);
                tv.setBackgroundColor(Palette.fondNormal);
                tv.setPadding(dp(16), dp(16), dp(16), dp(16));
                return tv;
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
        
        champ.setTextColor(Palette.texteNormal);
        champ.setHintTextColor(Palette.bordure);
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

    private void afficherColorPicker(Context context, int couleurInitiale, java.util.function.Consumer<Integer> onColorSelected) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(Traducteur.get("insp_titre_select_couleur"));

        LinearLayout layoutMain = new LinearLayout(context);
        layoutMain.setOrientation(LinearLayout.VERTICAL);
        layoutMain.setPadding(dp(16), dp(16), dp(16), dp(16));

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
        fondPreview.setCornerRadius(dp(8));
        fondPreview.setStroke(dp(1), Palette.bordure);
        previewColor.setBackground(fondPreview);

        EditText champHex = new EditText(context);
        champHex.setSingleLine(true);
        champHex.setText(String.format("#%06X", (0xFFFFFF & couleurInitiale)));
        champHex.setTextColor(Palette.texteNormal);
        champHex.setHintTextColor(Palette.bordure);
        champHex.setBackground(fond(Palette.fondNormal, Palette.bordure, 8));
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

            @Override
            protected void onDraw(Canvas canvas) {
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

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                    float x = Math.max(0, Math.min(event.getX(), getWidth()));
                    float y = Math.max(0, Math.min(event.getY(), getHeight()));
                    
                    currentHsv[0] = (x / getWidth()) * 360f;
                    currentHsv[1] = 1f; 
                    currentHsv[2] = 1f - (y / getHeight());
                    
                    int newColor = Color.HSVToColor(currentHsv);
                    
                    isUpdating[0] = true;
                    champHex.setText(String.format("#%06X", (0xFFFFFF & newColor)));
                    isUpdating[0] = false;
                    
                    ((android.graphics.drawable.GradientDrawable)previewColor.getBackground()).setColor(newColor);
                    invalidate();
                    return true;
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
        
        int[] couleursRapides = {
            Color.WHITE, Color.BLACK, 
            Palette.texteSelectionne, Palette.boutonNormal, 
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, 
            Color.CYAN, Color.MAGENTA, Color.parseColor("#FFA500"), Color.parseColor("#808080")
        };
        
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
        builder.setPositiveButton("OK", (dialog, which) -> {
            try {
                String finalHex = champHex.getText().toString();
                if (!finalHex.startsWith("#")) finalHex = "#" + finalHex;
                onColorSelected.accept(Color.parseColor(finalHex));
            } catch (Exception e) {
                onColorSelected.accept(Color.HSVToColor(currentHsv));
            }
        });
        builder.setNegativeButton(Traducteur.get("bouton_annuler"), null);
        builder.show();
    }

    // --- LE RENDU EN 3 PASSES PARFAITES ---
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
            canvas.drawColor(Color.parseColor("#1E1E1E")); 
            
            int spacing = dp(20);
            for (int i = 0; i < getWidth(); i += spacing) canvas.drawLine(i, 0, i, getHeight(), paintQuad);
            for (int j = 0; j < getHeight(); j += spacing) canvas.drawLine(0, j, getWidth(), j, paintQuad);

            if (styleCourant == null) return;
            
            String texteTest = "Yop2D\nMulti-Lignes";
            float cx = getWidth() / 2f;
            float cyStart = (getHeight() / 2f) - ((paintTexte.descent() + paintTexte.ascent()) / 2f) - (paintTexte.getTextSize() / 2f);

            // NOUVEAU : Application de l'espacement
            paintTexte.setLetterSpacing(styleCourant.espacementLettres);
            float hauteurLigne = paintTexte.getTextSize() * styleCourant.multiplicateurLignes;

            String[] lignes = texteTest.split("\n");
            float currentY = cyStart;

            for (String ligne : lignes) {
                
                // PASSE 1 : OMBRE SEULE (sur le fond)
                if (styleCourant.ombreActive) {
                    paintTexte.setStyle(Paint.Style.FILL); // On met le style Fill pour ne pas boursoufler l'ombre !
                    paintTexte.setShadowLayer(styleCourant.ombreRayon, styleCourant.ombreDx, styleCourant.ombreDy, styleCourant.ombreCouleur);
                    canvas.drawText(ligne, cx, currentY, paintTexte);
                    paintTexte.clearShadowLayer(); // On nettoie tout de suite après
                }

                // PASSE 2 : CONTOUR NET
                if ("STROKE".equals(styleCourant.modeRemplissage) || "FILL_AND_STROKE".equals(styleCourant.modeRemplissage)) {
                    paintTexte.setStyle(Paint.Style.STROKE);
                    paintTexte.setStrokeWidth(styleCourant.epaisseurContour);
                    paintTexte.setStrokeJoin(Paint.Join.ROUND);
                    paintTexte.setColor(styleCourant.couleurContour);
                    canvas.drawText(ligne, cx, currentY, paintTexte);
                }

                // PASSE 3 : REMPLISSAGE INTÉRIEUR
                if ("FILL".equals(styleCourant.modeRemplissage) || "FILL_AND_STROKE".equals(styleCourant.modeRemplissage)) {
                    paintTexte.setStyle(Paint.Style.FILL);
                    if (styleCourant.utiliserDegrade) {
                        Shader textShader = new LinearGradient(cx, currentY - paintTexte.getTextSize(), cx, currentY,
                                new int[]{styleCourant.couleurDegrade1, styleCourant.couleurDegrade2},
                                null, Shader.TileMode.CLAMP);
                        paintTexte.setShader(textShader);
                    } else {
                        paintTexte.setColor(Color.WHITE);
                    }
                    canvas.drawText(ligne, cx, currentY, paintTexte);
                    paintTexte.setShader(null);
                }
                
                currentY += hauteurLigne;
            }
        }
    }
}
// bas 3
