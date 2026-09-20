// haut 1
package com.ludexa.moteur;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// Fenêtre d'édition d'un nœud : cibles (objet A/B, variable, scène), champs, clavier, panneau d'insertion.
// Pour un nœud du catalogue (NoeudGenerique), les champs acceptent des formules : le panneau de gauche insère
// player.x, les variables et les fonctions, et le résultat est affiché sous le champ. Les autres nœuds
// (événements, anciens nœuds) gardent le comportement habituel.
public class EditeurNoeudDialog extends Dialog {

    private interface Choix {
        void choisi(int index);
    }

    private final Context context;
    private final NoeudBase noeud;
    private final Scene scene;
    private final boolean modeFormule;

    private String champActif = null;
    private EditText champSaisie;
    private TextView txtResume;
    private TextView txtApercu;
    private Button btnFormule;
    private LinearLayout conteneurClavier;
    private LinearLayout conteneurBooleen;
    private LinearLayout barreParams;

    private int dp(int valeur) {
        return (int) (valeur * context.getResources().getDisplayMetrics().density);
    }

    private android.graphics.drawable.GradientDrawable fond(int couleurFond, int couleurBordure, int rayon) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(couleurFond);
        g.setCornerRadius(dp(rayon));
        g.setStroke(dp(1), couleurBordure);
        return g;
    }

    public EditeurNoeudDialog(Context context, NoeudBase noeud, Scene scene, Runnable onValidate) {
        super(context);
        this.context = context;
        this.noeud = noeud;
        this.scene = scene;
        this.modeFormule = noeud instanceof NoeudGenerique;
        TextesEditeur.enregistrer();
        setTitle(Traducteur.get("noeud_edit_valeur") + " - " + Traducteur.get(noeud.nom));

        // ----- colonne de droite : cibles, champs, clavier
        LinearLayout colonneDroite = new LinearLayout(context);
        colonneDroite.setOrientation(LinearLayout.VERTICAL);
        colonneDroite.setBackground(fond(Palette.fondPanneaux, Palette.bordure, 12));
        colonneDroite.setPadding(dp(12), dp(12), dp(12), dp(12));

        HorizontalScrollView scrollCibles = new HorizontalScrollView(context);
        LinearLayout rangeeCibles = new LinearLayout(context);
        rangeeCibles.setOrientation(LinearLayout.HORIZONTAL);
        rangeeCibles.setPadding(0, 0, 0, dp(10));
        if (noeud.requiertCibleObjet()) ajouterSelectionObjet(rangeeCibles, false);
        if (noeud.requiertCibleObjetB()) ajouterSelectionObjet(rangeeCibles, true);
        if (noeud.requiertCibleVariable()) ajouterSelectionVariable(rangeeCibles);
        if (noeud.requiertCibleScene()) ajouterSelectionScene(rangeeCibles);
        scrollCibles.addView(rangeeCibles);
        colonneDroite.addView(scrollCibles);

        barreParams = new LinearLayout(context);
        barreParams.setOrientation(LinearLayout.HORIZONTAL);
        barreParams.setPadding(0, dp(2), 0, dp(8));
        colonneDroite.addView(barreParams);

        txtResume = new TextView(context);
        txtResume.setTextColor(Palette.texteSelectionne);
        txtResume.setTextSize(15f);
        txtResume.setBackground(fond(Palette.enTeteDialogues, Palette.bordure, 10));
        txtResume.setPadding(dp(12), dp(9), dp(12), dp(9));
        txtResume.getPaint().setFakeBoldText(true);
        colonneDroite.addView(txtResume);

        champSaisie = new EditText(context);
        champSaisie.setTextColor(Palette.texteNormal);
        champSaisie.setHintTextColor(Palette.bordure);
        champSaisie.setBackground(fond(Palette.canvasFond, Palette.bordure, 10));
        champSaisie.setTextSize(17f);
        champSaisie.setGravity(Gravity.TOP | Gravity.START);
        champSaisie.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams lpChamp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(80));
        lpChamp.setMargins(0, dp(6), 0, dp(6));
        champSaisie.setLayoutParams(lpChamp);
        colonneDroite.addView(champSaisie);

        txtApercu = new TextView(context);
        txtApercu.setTextSize(15f);
        txtApercu.setBackground(fond(Palette.fondNormal, Palette.bordure, 10));
        txtApercu.setPadding(dp(12), dp(8), dp(12), dp(8));
        txtApercu.setVisibility(View.GONE);
        LinearLayout.LayoutParams lpApercu = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpApercu.setMargins(0, 0, 0, dp(6));
        txtApercu.setLayoutParams(lpApercu);
        colonneDroite.addView(txtApercu);

        btnFormule = new Button(context);
        btnFormule.setText(Traducteur.get("editeur_bouton_formule"));
        btnFormule.setAllCaps(false);
        btnFormule.setTextSize(13f);
        btnFormule.setTextColor(Palette.texteNormal);
        btnFormule.setBackground(fond(Palette.boutonNormal, Palette.bordure, 8));
        btnFormule.setMinHeight(0);
        btnFormule.setMinimumHeight(0);
        btnFormule.setPadding(dp(14), dp(8), dp(14), dp(8));
        btnFormule.setVisibility(View.GONE);
        LinearLayout.LayoutParams lpFormule = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpFormule.setMargins(0, 0, 0, dp(6));
        btnFormule.setLayoutParams(lpFormule);
        btnFormule.setOnClickListener(v -> {
            AideSaisie.Modification m = AideSaisie.basculerEnFormule(champSaisie.getText().toString());
            champSaisie.setText(m.texte);
            champSaisie.setSelection(Math.min(m.curseur, champSaisie.getText().length()));
        });
        colonneDroite.addView(btnFormule);

        conteneurClavier = new LinearLayout(context);
        conteneurClavier.setOrientation(LinearLayout.VERTICAL);
        conteneurClavier.setBackground(fond(Palette.fondNormal, Palette.bordure, 12));
        conteneurClavier.setPadding(dp(6), dp(8), dp(6), dp(8));
        construirePave();
        colonneDroite.addView(conteneurClavier);

        conteneurBooleen = new LinearLayout(context);
        conteneurBooleen.setOrientation(LinearLayout.HORIZONTAL);
        conteneurBooleen.setGravity(Gravity.CENTER);
        conteneurBooleen.setBackground(fond(Palette.fondNormal, Palette.bordure, 12));
        conteneurBooleen.setPadding(dp(8), dp(10), dp(8), dp(10));
        conteneurBooleen.setVisibility(View.GONE);
        construireBooleens();
        colonneDroite.addView(conteneurBooleen);

        ScrollView scrollDroit = new ScrollView(context);
        scrollDroit.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scrollDroit.setFillViewport(true);
        scrollDroit.addView(colonneDroite);
        LinearLayout wrapperDroite = new LinearLayout(context);
        wrapperDroite.setOrientation(LinearLayout.VERTICAL);
        wrapperDroite.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.5f));
        wrapperDroite.addView(scrollDroit);

        // ----- colonne de gauche : panneau d'insertion
        LinearLayout colonneGauche = new LinearLayout(context);
        colonneGauche.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpGauche = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.8f);
        lpGauche.setMargins(0, 0, dp(8), 0);
        colonneGauche.setLayoutParams(lpGauche);
        colonneGauche.setBackground(fond(Palette.fondNormal, Palette.bordure, 12));
        colonneGauche.addView(construirePanneau());

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(Palette.fondPanneaux);
        root.setPadding(dp(8), dp(8), dp(8), dp(4));
        root.addView(colonneGauche);
        root.addView(wrapperDroite);

        // ----- barre du bas
        LinearLayout bottomBar = new LinearLayout(context);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        bottomBar.setBackgroundColor(Palette.fondPanneaux);
        bottomBar.setPadding(dp(12), dp(10), dp(12), dp(12));
        Button btnFermer = new Button(context);
        btnFermer.setText(Traducteur.get("bouton_fermer"));
        btnFermer.setAllCaps(false);
        btnFermer.setTextSize(15f);
        btnFermer.setBackground(fond(Palette.boutonNormal, Palette.bordure, 10));
        btnFermer.setTextColor(Palette.texteNormal);
        btnFermer.setMinHeight(0);
        btnFermer.setMinimumHeight(0);
        btnFermer.setPadding(dp(24), dp(12), dp(24), dp(12));
        btnFermer.setOnClickListener(v -> {
            if (onValidate != null) onValidate.run();
            dismiss();
        });
        bottomBar.addView(btnFermer);

        LinearLayout grandLayout = new LinearLayout(context);
        grandLayout.setOrientation(LinearLayout.VERTICAL);
        grandLayout.setBackgroundColor(Palette.fondPanneaux);
        grandLayout.addView(root, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        grandLayout.addView(bottomBar);
        setContentView(grandLayout);

        Window window = getWindow();
        if (window != null) {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            window.setLayout((int) (metrics.widthPixels * 0.95), (int) (metrics.heightPixels * 0.90));
            // le clavier ne s'ouvre qu'au toucher du champ, et la fenêtre se redimensionne autour de lui
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        // ----- champs et comportement
        champSaisie.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (champActif != null) {
                    noeud.setValeurParametre(champActif, AideSaisie.valeurAStocker(noeud, champActif, s.toString()));
                    mettreAJourResume();
                    mettreAJourAideChamp();
                }
            }
        });
        champSaisie.setOnClickListener(v -> {
            if (champActif != null) ouvrirSelecteur(noeud.getTypeEditeurParametre(champActif));
        });
        champSaisie.setOnFocusChangeListener((v, aLeFocus) -> {
            if (aLeFocus && clavierNatifAutorise()) afficherClavierNatif();
        });

        List<String> params = noeud.getNomsParametres();
        if (params != null && !params.isEmpty()) {
            construireBarreParams(params);
            chargerChamp(params.get(0), false);
        } else {
            // Nœud sans champ à remplir (ex : Traverser l'écran) : seule sa cible compte
            champSaisie.setVisibility(View.GONE);
            conteneurClavier.setVisibility(View.GONE);
            conteneurBooleen.setVisibility(View.GONE);
            btnFormule.setVisibility(View.GONE);
            txtApercu.setText(Traducteur.get("editeur_aucun_champ"));
            txtApercu.setTextColor(Palette.texteNormal);
            txtApercu.setVisibility(View.VISIBLE);
        }
        mettreAJourResume();
    }

    // Le champ est préparé avant l'affichage de la fenêtre ; on le recharge une fois la fenêtre affichée,
    // pour que le curseur, le focus et le clavier soient dans le même état que lorsqu'on change de champ.
    @Override
    protected void onStart() {
        super.onStart();
        champSaisie.post(() -> {
            if (champActif != null) chargerChamp(champActif, false);
        });
    }

    private boolean clavierNatifAutorise() {
        if (champActif == null) return false;
        String type = noeud.getTypeEditeurParametre(champActif);
        if (estSelecteur(type) || "TYPE_BOOLEEN".equals(type)) return false;
        return NoeudBase.TYPE_TEXTE_ALPHABETIQUE.equals(type) || noeud.utiliseClavierTexte();
    }

    private void afficherClavierNatif() {
        Object service = context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (service instanceof InputMethodManager) ((InputMethodManager) service).showSoftInput(champSaisie, InputMethodManager.SHOW_IMPLICIT);
    }

    // Position d'écriture : le curseur si le champ est actif, sinon la fin du texte.
    private int[] positionEcriture() {
        int longueur = champSaisie.getText().length();
        int debut = champSaisie.getSelectionStart();
        int fin = champSaisie.getSelectionEnd();
        if (!champSaisie.hasFocus() || debut < 0 || fin < 0) return new int[]{longueur, longueur};
        return new int[]{Math.min(debut, fin), Math.max(debut, fin)};
    }

    // ------------------------------------------------------------------
    // CHAMPS
    // ------------------------------------------------------------------

    private void construireBarreParams(List<String> params) {
        for (final String paramName : params) {
            Button btnParam = new Button(context);
            btnParam.setText(Traducteur.get(paramName));
            btnParam.setTag(paramName);
            btnParam.setAllCaps(false);
            btnParam.setTextSize(13f);
            btnParam.setTextColor(Palette.texteNormal);
            btnParam.setBackground(fond(Palette.boutonNormal, Palette.bordure, 8));
            btnParam.setMinHeight(0);
            btnParam.setMinimumHeight(0);
            btnParam.setPadding(dp(14), dp(8), dp(14), dp(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(8), 0);
            btnParam.setLayoutParams(lp);
            btnParam.setOnClickListener(v -> chargerChamp(paramName, true));
            barreParams.addView(btnParam);
        }
    }

    // Affiche le champ demandé ; si c'est une liste (couleur, image, tag...), l'ouvre quand c'est l'utilisateur qui l'a touché.
    private void chargerChamp(String paramName, boolean parUtilisateur) {
        champActif = paramName;
        appliquerTypeEditeur();   // d'abord le type de champ (il peut replacer le curseur)...
        champSaisie.setText(AideSaisie.texteAffiche(noeud, paramName));
        champSaisie.setSelection(champSaisie.getText().length());   // ...puis le texte, avec le curseur à la fin
        for (int i = 0; i < barreParams.getChildCount(); i++) {
            View enfant = barreParams.getChildAt(i);
            if (enfant instanceof Button && enfant.getTag() != null) {
                boolean actif = enfant.getTag().toString().equals(champActif);
                enfant.setBackground(fond(actif ? Color.parseColor("#4CAF50") : Palette.boutonNormal, Palette.bordure, 8));
            }
        }
        champSaisie.setHint(AideSaisie.champTexte(noeud, champActif) ? Traducteur.get("editeur_astuce_texte") : "");
        mettreAJourAideChamp();
        if (parUtilisateur && estSelecteur(noeud.getTypeEditeurParametre(champActif))) champSaisie.performClick();
    }

    private static boolean estSelecteur(String type) {
        return NoeudBase.TYPE_COULEUR.equals(type) || NoeudBase.TYPE_CHOIX_LISTE.equals(type)
                || NoeudBase.TYPE_CHOIX_IMAGE.equals(type) || NoeudBase.TYPE_CHOIX_DIALOGUE.equals(type)
                || NoeudBase.TYPE_CHOIX_SON.equals(type) || NoeudBase.TYPE_CHOIX_FONCTION.equals(type)
                || "CHOIX_ANIMATION".equals(type) || "TYPE_CHOIX_TAG".equals(type);
    }

    // Bouton "Passer en formule", aperçu du résultat sous le champ.
    private void mettreAJourAideChamp() {
        String texte = champSaisie.getText().toString();
        btnFormule.setVisibility(champActif != null && AideSaisie.peutPasserEnFormule(noeud, champActif, texte) ? View.VISIBLE : View.GONE);

        if (champActif != null && AideSaisie.champEstCalcul(noeud, champActif, texte)) {
            ApercuFormule.Resultat r = ApercuFormule.calculer(texte, noeud.getCibleObjet(), scene, context);
            if (r.texte.isEmpty()) {
                txtApercu.setVisibility(View.GONE);
            } else {
                txtApercu.setText(r.ok ? "= " + r.texte : "⚠ " + r.texte);
                txtApercu.setTextColor(Color.parseColor(r.ok ? "#4CAF50" : "#FF9800"));
                txtApercu.setVisibility(View.VISIBLE);
            }
        } else {
            txtApercu.setVisibility(View.GONE);
        }
    }

    // Insère dans le champ (à la place de la sélection). Dans un champ de texte, le champ passe en formule.
    private void inserer(String insertion) {
        if (champActif == null) return;
        if (estSelecteur(noeud.getTypeEditeurParametre(champActif))) return;
        int[] position = positionEcriture();
        AideSaisie.Modification m = AideSaisie.inserer(champSaisie.getText().toString(), position[0], position[1], insertion, AideSaisie.champTexte(noeud, champActif));
        champSaisie.setText(m.texte);
        champSaisie.setSelection(Math.min(m.curseur, champSaisie.getText().length()));
    }

    private void appliquerTypeEditeur() {
        String type = (champActif != null) ? noeud.getTypeEditeurParametre(champActif) : NoeudBase.TYPE_TEXTE_LIBRE;

        if (estSelecteur(type)) {
            champSaisie.setFocusable(false);
            champSaisie.setFocusableInTouchMode(false);
            champSaisie.setClickable(true);
            champSaisie.setShowSoftInputOnFocus(false);
            champSaisie.setInputType(InputType.TYPE_NULL);
            conteneurClavier.setVisibility(View.GONE);
            conteneurBooleen.setVisibility(View.GONE);
        } else if ("TYPE_BOOLEEN".equals(type)) {
            champSaisie.setFocusable(false);
            champSaisie.setFocusableInTouchMode(false);
            champSaisie.setClickable(true);
            champSaisie.setShowSoftInputOnFocus(false);
            champSaisie.setInputType(InputType.TYPE_NULL);
            conteneurClavier.setVisibility(View.GONE);
            conteneurBooleen.setVisibility(View.VISIBLE);
        } else {
            champSaisie.setFocusable(true);
            champSaisie.setFocusableInTouchMode(true);
            champSaisie.setClickable(true);
            if (NoeudBase.TYPE_TEXTE_ALPHABETIQUE.equals(type)) {
                champSaisie.setShowSoftInputOnFocus(true);
                champSaisie.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                conteneurClavier.setVisibility(View.GONE);
                conteneurBooleen.setVisibility(View.GONE);
                champSaisie.requestFocus();
            } else if (noeud.utiliseClavierTexte()) {
                // clavier natif + pavé code + vrai/faux (nœuds Condition, Visibilité, formules...)
                champSaisie.setShowSoftInputOnFocus(true);
                champSaisie.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                conteneurClavier.setVisibility(View.VISIBLE);
                conteneurBooleen.setVisibility(View.VISIBLE);
            } else {
                champSaisie.setShowSoftInputOnFocus(false);
                champSaisie.setInputType(InputType.TYPE_NULL);
                conteneurClavier.setVisibility(View.VISIBLE);
                conteneurBooleen.setVisibility(View.GONE);
            }
        }
    }
// bas 1

// haut 2
    // ------------------------------------------------------------------
    // CIBLES : objet A / objet B / variable / scène
    // ------------------------------------------------------------------

    private Button creerBoutonSelectionCible(String texte) {
        Button btn = new Button(context);
        btn.setText(texte);
        btn.setAllCaps(false);
        btn.setTextSize(14f);
        btn.setBackground(fond(Palette.boutonNormal, Palette.bordure, 8));
        btn.setTextColor(Color.parseColor("#FFD700"));
        btn.setMinHeight(0);
        btn.setMinimumHeight(0);
        btn.setPadding(dp(14), dp(9), dp(14), dp(9));
        return btn;
    }

    private TextView creerTextViewAfficheurCible() {
        TextView txt = new TextView(context);
        txt.setPadding(dp(8), dp(4), dp(16), dp(4));
        txt.setTextSize(15f);
        return txt;
    }

    private void mettreAJourAfficheurCible(TextView txt, String valeur) {
        if (valeur == null || valeur.trim().isEmpty()) {
            txt.setText(Traducteur.get("valeur_aucune"));
            txt.setTextColor(Color.parseColor("#888888"));
            txt.setTypeface(null, android.graphics.Typeface.ITALIC);
        } else {
            txt.setText(valeur);
            txt.setTextColor(Palette.texteSelectionne);
            txt.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }

    private void ajouterCoupleALaRangee(LinearLayout rangee, Button btn, TextView txt) {
        LinearLayout couple = new LinearLayout(context);
        couple.setOrientation(LinearLayout.HORIZONTAL);
        couple.setGravity(Gravity.CENTER_VERTICAL);
        couple.setBackground(fond(Palette.fondNormal, Palette.bordure, 10));
        couple.setPadding(dp(6), dp(6), dp(6), dp(6));
        LinearLayout.LayoutParams lpCouple = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpCouple.setMargins(0, 0, dp(8), 0);
        couple.setLayoutParams(lpCouple);
        couple.addView(btn);
        couple.addView(txt);
        rangee.addView(couple);
    }

    private void ajouterSelectionObjet(LinearLayout rangee, final boolean estB) {
        final TextView afficheur = creerTextViewAfficheurCible();
        Button bouton = creerBoutonSelectionCible(Traducteur.get(estB ? "noeud_cible_objet_b" : "noeud_cible_objet_a"));

        String nomMemorise = estB ? noeud.nomCibleObjetB : noeud.nomCibleObjet;
        ObjetBase objetActuel = estB ? noeud.getCibleObjetB() : noeud.getCibleObjet();
        String affiche;
        if ("__OBJET_IMPLIQUE__".equals(nomMemorise)) affiche = Traducteur.get("noeud_objet_implique_long");
        else if (nomMemorise != null && !nomMemorise.isEmpty()) affiche = nomMemorise;
        else affiche = (objetActuel != null) ? objetActuel.nom : null;
        mettreAJourAfficheurCible(afficheur, affiche);

        bouton.setOnClickListener(v -> {
            if (scene == null || scene.objets == null) return;
            String[] noms = new String[scene.objets.size() + 2];
            noms[0] = Traducteur.get("valeur_aucune");
            noms[1] = Traducteur.get("noeud_objet_implique_long");
            for (int i = 0; i < scene.objets.size(); i++) noms[i + 2] = scene.objets.get(i).nom;
            afficherChoix(Traducteur.get(estB ? "noeud_choisir_cible_objet_b" : "noeud_choisir_cible_objet_a"), noms, which -> {
                ObjetBase choisi = (which >= 2) ? scene.objets.get(which - 2) : null;
                String nom = (which == 0) ? null : (which == 1 ? "__OBJET_IMPLIQUE__" : choisi.nom);
                if (estB) {
                    noeud.setCibleObjetB(choisi);
                    noeud.nomCibleObjetB = nom;
                } else {
                    noeud.setCibleObjet(choisi);
                    noeud.nomCibleObjet = nom;
                }
                mettreAJourAfficheurCible(afficheur, which == 1 ? Traducteur.get("noeud_objet_implique_long") : nom);
                mettreAJourResume();
                mettreAJourAideChamp();
            });
        });
        ajouterCoupleALaRangee(rangee, bouton, afficheur);
    }

    private void ajouterSelectionVariable(LinearLayout rangee) {
        final TextView afficheur = creerTextViewAfficheurCible();
        Button bouton = creerBoutonSelectionCible(Traducteur.get("noeud_cible_variable"));
        String nomAffiche = (noeud.nomCibleVariable != null && !noeud.nomCibleVariable.isEmpty())
                ? noeud.nomCibleVariable
                : (noeud.getCibleVariable() != null ? noeud.getCibleVariable().nom : null);
        mettreAJourAfficheurCible(afficheur, nomAffiche);

        bouton.setOnClickListener(v -> {
            final List<String> noms = new ArrayList<>();
            final List<Variable> refs = new ArrayList<>();
            noms.add(Traducteur.get("valeur_aucune"));
            refs.add(null);
            if (scene != null && scene.variablesLocales != null) {
                for (Variable var : scene.variablesLocales) {
                    noms.add(var.nom + " (" + Traducteur.get("variable_locale") + ")");
                    refs.add(var);
                }
            }
            if (scene != null && scene.objets != null) {
                List<String> dejaVus = new ArrayList<>();
                for (ObjetBase obj : scene.objets) {
                    if (obj.variablesLocales == null) continue;
                    for (Variable var : obj.variablesLocales) {
                        if (!dejaVus.contains(var.nom)) {
                            noms.add(var.nom + " (" + obj.nom + ")");
                            refs.add(var);
                            dejaVus.add(var.nom);
                        }
                    }
                }
            }
            List<Variable> globales = variablesGlobales();
            if (globales != null) {
                for (Variable var : globales) {
                    noms.add(var.nom + " (" + Traducteur.get("variable_globale") + ")");
                    refs.add(var);
                }
            }
            afficherChoix(Traducteur.get("noeud_choisir_cible_variable"), noms.toArray(new String[0]), which -> {
                Variable var = refs.get(which);
                noeud.setCibleVariable(var);
                mettreAJourAfficheurCible(afficheur, var != null ? var.nom : null);
                mettreAJourResume();
                mettreAJourAideChamp();
            });
        });
        ajouterCoupleALaRangee(rangee, bouton, afficheur);
    }

    private void ajouterSelectionScene(LinearLayout rangee) {
        final TextView afficheur = creerTextViewAfficheurCible();
        Button bouton = creerBoutonSelectionCible(Traducteur.get("noeud_cible_scene"));
        mettreAJourAfficheurCible(afficheur, noeud.getCibleScene() != null ? noeud.getCibleScene().nom : null);
        bouton.setOnClickListener(v -> {
            final List<Scene> scenes = scenesDisponibles();
            if (scenes == null || scenes.isEmpty()) return;
            String[] noms = new String[scenes.size()];
            for (int i = 0; i < scenes.size(); i++) noms[i] = scenes.get(i).nom;
            afficherChoix(Traducteur.get("noeud_choisir_cible_scene"), noms, which -> {
                Scene choisie = scenes.get(which);
                noeud.setCibleScene(choisie);
                mettreAJourAfficheurCible(afficheur, choisie.nom);
                mettreAJourResume();
            });
        });
        ajouterCoupleALaRangee(rangee, bouton, afficheur);
    }

    @SuppressWarnings("unchecked")
    private List<Variable> variablesGlobales() {
        if (context instanceof FournisseurDonneesJeu) return ((FournisseurDonneesJeu) context).getVariablesGlobales();
        if (NoeudBase.contexteApplication != null) {
            try {
                java.lang.reflect.Field champ = NoeudBase.contexteApplication.getClass().getField("variablesGlobales");
                return (List<Variable>) champ.get(NoeudBase.contexteApplication);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Scene> scenesDisponibles() {
        if (context instanceof FournisseurDonneesJeu) return ((FournisseurDonneesJeu) context).getListeScenes();
        if (NoeudBase.contexteApplication != null) {
            try {
                java.lang.reflect.Field champ = NoeudBase.contexteApplication.getClass().getField("listeScenes");
                return (List<Scene>) champ.get(NoeudBase.contexteApplication);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private String cheminProjet() {
        if (context instanceof InterfaceBlueprint) return ((InterfaceBlueprint) context).cheminProjet;
        if (context instanceof InterfaceEditeur) return ((InterfaceEditeur) context).cheminProjet;
        return null;
    }

    // ------------------------------------------------------------------
    // SÉLECTEURS (tag, couleur, liste, image, son, dialogue, fonction, animation)
    // ------------------------------------------------------------------

    private void afficherChoix(String titre, String[] items, final Choix action) {
        new android.app.AlertDialog.Builder(context).setTitle(titre)
                .setItems(items, (d, which) -> action.choisi(which)).show();
    }

    private void ecrireDansChamp(String texte) {
        champSaisie.setText(texte);
        champSaisie.setSelection(champSaisie.getText().length());
    }

    private void demanderTexte(String titre, String valeurInitiale) {
        final EditText saisie = new EditText(context);
        saisie.setText(valeurInitiale);
        new android.app.AlertDialog.Builder(context).setTitle(titre).setView(saisie)
                .setPositiveButton(Traducteur.get("bouton_ok"), (d, w) -> ecrireDansChamp(saisie.getText().toString().trim()))
                .setNegativeButton(Traducteur.get("bouton_annuler"), null)
                .show();
    }

    private void ouvrirSelecteur(String type) {
        String chemin = cheminProjet();
        switch (type) {
            case "TYPE_CHOIX_TAG":
                choisirTag();
                break;
            case NoeudBase.TYPE_COULEUR:
                choisirCouleur();
                break;
            case NoeudBase.TYPE_CHOIX_LISTE:
                choisirOption();
                break;
            case NoeudBase.TYPE_CHOIX_IMAGE:
                if (chemin != null) choisirFichier(Traducteur.get("noeud_choisir_image"), new File(chemin, "assets_ludexa/Images"),
                        "assets_ludexa/Images/", Traducteur.get("noeud_aucune_image"), ".png", ".jpg");
                break;
            case NoeudBase.TYPE_CHOIX_SON:
                if (chemin != null) choisirFichier(Traducteur.get("noeud_choisir_audio"), new File(chemin, "assets_ludexa/Sons"),
                        "assets_ludexa/Sons/", Traducteur.get("noeud_aucun_fichier_audio"), ".mp3", ".wav", ".ogg");
                break;
            case NoeudBase.TYPE_CHOIX_DIALOGUE:
                if (chemin != null) choisirDansListe(Traducteur.get("noeud_choisir_dialogue"),
                        lireCles(new File(chemin, "assets_ludexa/Textes/dialogues.txt")), Traducteur.get("noeud_aucune_cle_dialogue"));
                break;
            case "CHOIX_ANIMATION":
                if (chemin != null) choisirDansListe(Traducteur.get("noeud_choisir_animation"),
                        lireCles(new File(chemin, "assets_ludexa/Textes/animations.txt")), Traducteur.get("noeud_aucune_animation"));
                break;
            case NoeudBase.TYPE_CHOIX_FONCTION:
                if (chemin != null) choisirDansListe(Traducteur.get("noeud_choisir_fonction"),
                        listerFonctions(new File(chemin, "fonctions")), Traducteur.get("noeud_aucune_fonction"));
                break;
            default:
                break;
        }
    }

    private void choisirTag() {
        final List<String> tags = NoeudBase.getTagsDisponibles();
        tags.add(Traducteur.get("noeud_tag_manuel"));
        afficherChoix(Traducteur.get("noeud_choisir_tag"), tags.toArray(new String[0]), which -> {
            if (which == tags.size() - 1) demanderTexte(Traducteur.get("noeud_saisir_tag"), champSaisie.getText().toString());
            else ecrireDansChamp(tags.get(which));
        });
    }

    private void choisirCouleur() {
        final String[] noms = {
                Traducteur.get("couleur_bleu_defaut"), Traducteur.get("couleur_rouge"), Traducteur.get("couleur_vert"),
                Traducteur.get("couleur_noir"), Traducteur.get("couleur_blanc"), Traducteur.get("couleur_jaune"),
                Traducteur.get("couleur_magenta"), Traducteur.get("couleur_cyan")};
        // Les nœuds du catalogue acceptent aussi un code #RRGGBB
        final String[] items = new String[noms.length + (modeFormule ? 1 : 0)];
        System.arraycopy(noms, 0, items, 0, noms.length);
        if (modeFormule) items[noms.length] = Traducteur.get("editeur_couleur_perso");
        afficherChoix(Traducteur.get("noeud_choisir_couleur"), items, which -> {
            if (which < noms.length) ecrireDansChamp(noms[which]);
            else demanderTexte(Traducteur.get("editeur_couleur_perso_titre"), "#");
        });
    }

    // Liste de choix : les libellés (traduits) sont affichés, la valeur stable est enregistrée par le champ.
    private void choisirOption() {
        final List<String> libelles = AideSaisie.libellesOptions(noeud.getOptionsChoixListe(champActif));
        afficherChoix(Traducteur.get("noeud_choisir_option"), libelles.toArray(new String[0]), which -> ecrireDansChamp(libelles.get(which)));
    }

    // Fichiers d'un dossier ; le premier choix ("aucune") vide le champ.
    private void choisirFichier(String titre, File dossier, String prefixe, String aucun, String... extensions) {
        final List<String> valeurs = new ArrayList<>();
        final List<String> libelles = new ArrayList<>();
        libelles.add(aucun);
        valeurs.add("");
        if (dossier.exists() && dossier.isDirectory()) {
            File[] fichiers = dossier.listFiles();
            if (fichiers != null) {
                java.util.Arrays.sort(fichiers);
                for (File f : fichiers) {
                    if (f.isDirectory()) continue;
                    String nom = f.getName().toLowerCase();
                    for (String extension : extensions) {
                        if (nom.endsWith(extension)) {
                            libelles.add(prefixe + f.getName());
                            valeurs.add(prefixe + f.getName());
                            break;
                        }
                    }
                }
            }
        }
        afficherChoix(titre, libelles.toArray(new String[0]), which -> ecrireDansChamp(valeurs.get(which)));
    }

    // Liste de noms ; s'il n'y en a aucun, un seul élément "aucun" qui ne change rien.
    private void choisirDansListe(String titre, final List<String> noms, final String aucun) {
        if (noms.isEmpty()) noms.add(aucun);
        afficherChoix(titre, noms.toArray(new String[0]), which -> {
            if (!noms.get(which).equals(aucun)) ecrireDansChamp(noms.get(which));
        });
    }

    // Clés d'un fichier "cle = texte" (lignes vides et commentaires // ignorés).
    private List<String> lireCles(File fichier) {
        List<String> cles = new ArrayList<>();
        if (!fichier.exists()) return cles;
        try {
            java.io.BufferedReader lecteur = new java.io.BufferedReader(new java.io.FileReader(fichier));
            String ligne;
            while ((ligne = lecteur.readLine()) != null) {
                ligne = ligne.trim();
                if (ligne.isEmpty() || ligne.startsWith("//")) continue;
                int egal = ligne.indexOf('=');
                if (egal > 0) cles.add(ligne.substring(0, egal).trim());
            }
            lecteur.close();
        } catch (Exception e) {
            // fichier illisible : la liste reste vide
        }
        return cles;
    }

    private List<String> listerFonctions(File dossier) {
        List<String> noms = new ArrayList<>();
        if (dossier.exists() && dossier.isDirectory()) {
            File[] fichiers = dossier.listFiles();
            if (fichiers != null) {
                for (File f : fichiers) {
                    if (f.getName().endsWith(".json")) noms.add(f.getName().replace(".json", ""));
                }
            }
        }
        return noms;
    }
// bas 2

// haut 3
    // ------------------------------------------------------------------
    // PAVÉ DE CODE ET VRAI/FAUX
    // ------------------------------------------------------------------

    private void construirePave() {
        String[][] touchesCode = {
                {"1", "2", "3", "DEL"},
                {"4", "5", "6", "ESPACE"},
                {"7", "8", "9", "\""},
                {".", "0", "+", "-"},
                {"*", "/", "(", ")"},
                {">", "<", "=", "!"},
                {"||", "&&", "", ""},
                {"==", "!=", ">=", "<="},
                {"%", ",", "true", "false"}
        };
        int marge = dp(3);
        for (String[] ligne : touchesCode) {
            LinearLayout rangee = new LinearLayout(context);
            rangee.setOrientation(LinearLayout.HORIZONTAL);
            rangee.setGravity(Gravity.CENTER);
            for (final String touche : ligne) {
                Button btn = new Button(context);
                btn.setText(touche.equals("ESPACE") ? Traducteur.get("clavier_espace") : touche);
                btn.setAllCaps(false);
                btn.setTextColor(Palette.texteNormal);
                btn.setBackground(fond(Palette.boutonNormal, Palette.bordure, 8));
                btn.setMinHeight(0);
                btn.setMinimumHeight(0);
                btn.setPadding(0, dp(12), 0, dp(12));
                btn.setTextSize(14f);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(marge, marge, marge, marge);
                btn.setLayoutParams(lp);

                if (touche.isEmpty()) {
                    btn.setVisibility(View.INVISIBLE);
                } else {
                    if (touche.equals("DEL")) btn.setBackground(fond(Color.parseColor("#5c2323"), Palette.bordure, 8));
                    btn.setOnClickListener(v -> {
                        int[] position = positionEcriture();
                        int debut = position[0];
                        int fin = position[1];
                        if (touche.equals("DEL")) {
                            if (debut > 0 && debut == fin) champSaisie.getText().delete(debut - 1, debut);
                            else if (debut != fin) champSaisie.getText().delete(Math.min(debut, fin), Math.max(debut, fin));
                        } else {
                            String insertion = touche.equals("ESPACE") ? " " : touche;
                            champSaisie.getText().replace(Math.min(debut, fin), Math.max(debut, fin), insertion, 0, insertion.length());
                        }
                    });
                }
                rangee.addView(btn);
            }
            conteneurClavier.addView(rangee);
        }
    }

    private void construireBooleens() {
        int marge = dp(5);
        Button btnVrai = new Button(context);
        btnVrai.setText(Traducteur.get("noeud_bool_vrai"));
        btnVrai.setAllCaps(false);
        btnVrai.setBackground(fond(Color.parseColor("#4CAF50"), Palette.bordure, 10));
        btnVrai.setTextColor(Palette.texteNormal);
        btnVrai.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lpVrai = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpVrai.setMargins(marge, marge, marge, marge);
        btnVrai.setLayoutParams(lpVrai);
        btnVrai.setOnClickListener(v -> ecrireDansChamp("true"));

        Button btnFaux = new Button(context);
        btnFaux.setText(Traducteur.get("noeud_bool_faux"));
        btnFaux.setAllCaps(false);
        btnFaux.setBackground(fond(Color.parseColor("#F44336"), Palette.bordure, 10));
        btnFaux.setTextColor(Palette.texteNormal);
        btnFaux.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lpFaux = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpFaux.setMargins(marge, marge, marge, marge);
        btnFaux.setLayoutParams(lpFaux);
        btnFaux.setOnClickListener(v -> ecrireDansChamp("false"));

        conteneurBooleen.addView(btnVrai);
        conteneurBooleen.addView(btnFaux);
    }

    // ------------------------------------------------------------------
    // PANNEAU D'INSERTION (colonne de gauche)
    // Nœud du catalogue : objets et leurs propriétés, variables, tags, fonctions -> insère player.x, score...
    // Autres nœuds : liste simple des noms (comportement habituel).
    // ------------------------------------------------------------------

    private View construirePanneau() {
        LinearLayout liste = new LinearLayout(context);
        liste.setOrientation(LinearLayout.VERTICAL);
        liste.setPadding(dp(8), dp(8), dp(8), dp(12));
        for (AideSaisie.Section section : AideSaisie.construire(scene, variablesGlobales())) {
            if (!modeFormule && section.cleTitre.equals("editeur_section_fonctions")) continue;
            ajouterSection(liste, section);
        }
        ScrollView defilement = new ScrollView(context);
        defilement.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        defilement.setFillViewport(true);
        defilement.addView(liste);
        return defilement;
    }

    private void ajouterSection(LinearLayout liste, AideSaisie.Section section) {
        final String titre = Traducteur.get(section.cleTitre);
        final Button entete = new Button(context);
        entete.setText((section.ouverte ? "▾ " : "▸ ") + titre);
        entete.setAllCaps(false);
        entete.setTextSize(15f);
        entete.setTypeface(null, android.graphics.Typeface.BOLD);
        entete.setTextColor(Palette.texteSelectionne);
        entete.setBackground(fond(Palette.enTeteDialogues, Palette.bordure, 10));
        entete.setMinHeight(0);
        entete.setMinimumHeight(0);
        entete.setPadding(dp(12), dp(9), dp(12), dp(9));
        LinearLayout.LayoutParams lpEntete = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpEntete.setMargins(0, dp(10), 0, 0);
        entete.setLayoutParams(lpEntete);
        liste.addView(entete);

        final LinearLayout contenu = new LinearLayout(context);
        contenu.setOrientation(LinearLayout.VERTICAL);
        contenu.setVisibility(section.ouverte ? View.VISIBLE : View.GONE);
        // L'"objet impliqué" ne sert que dans les formules
        List<AideSaisie.Element> visibles = new ArrayList<>();
        for (AideSaisie.Element element : section.elements) {
            if (modeFormule || !element.insertion.equals("implique")) visibles.add(element);
        }
        if (visibles.isEmpty()) {
            TextView aucun = new TextView(context);
            aucun.setText(Traducteur.get("valeur_aucune"));
            aucun.setTextColor(Color.parseColor("#888888"));
            aucun.setTextSize(13f);
            aucun.setPadding(dp(12), dp(4), 0, 0);
            contenu.addView(aucun);
        }
        for (AideSaisie.Element element : visibles) ajouterElement(contenu, element);
        liste.addView(contenu);

        entete.setOnClickListener(v -> {
            boolean ouvert = contenu.getVisibility() == View.VISIBLE;
            contenu.setVisibility(ouvert ? View.GONE : View.VISIBLE);
            entete.setText((ouvert ? "▸ " : "▾ ") + titre);
        });
    }

    // Un objet ouvre un sous-menu (son nom, ses propriétés, ses variables) ; les autres éléments s'insèrent directement.
    private void ajouterElement(LinearLayout parent, final AideSaisie.Element element) {
        final boolean sousMenu = modeFormule && !element.enfants.isEmpty();
        Button bouton = boutonPanneau(element.libelle + (sousMenu ? "  ›" : ""), Palette.texteNormal, 0);
        bouton.setOnClickListener(v -> {
            if (sousMenu) afficherSousMenu(element);
            else inserer(modeFormule ? element.insertion : element.brut);
        });
        if (sousMenu) {
            bouton.setOnLongClickListener(v -> {
                afficherSousMenu(element);
                return true;
            });
        }
        parent.addView(bouton);
    }

    private void afficherSousMenu(AideSaisie.Element element) {
        final List<String> libelles = new ArrayList<>();
        final List<String> insertions = new ArrayList<>();
        libelles.add(element.insertion);
        insertions.add(element.insertion);
        for (AideSaisie.Element enfant : element.enfants) {
            libelles.add(enfant.libelle);
            insertions.add(enfant.insertion);
        }
        afficherChoix(element.libelle, libelles.toArray(new String[0]), which -> inserer(insertions.get(which)));
    }

    private Button boutonPanneau(String texte, int couleurTexte, int retraitDp) {
        Button b = new Button(context);
        b.setText(texte);
        b.setAllCaps(false);
        b.setTextSize(14f);
        b.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        b.setTextColor(couleurTexte);
        b.setBackground(fond(Palette.boutonNormal, Palette.bordure, 8));
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(12), dp(9), dp(12), dp(9));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(retraitDp), dp(4), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    // ------------------------------------------------------------------
    // RÉSUMÉ AU-DESSUS DU CHAMP
    // ------------------------------------------------------------------

    private String nomObjetAffiche(String nomMemorise, ObjetBase objet) {
        if ("__OBJET_IMPLIQUE__".equals(nomMemorise)) return Traducteur.get("noeud_objet_implique");
        if (nomMemorise != null && !nomMemorise.isEmpty()) return nomMemorise;
        return (objet != null && objet.nom != null) ? objet.nom : "[?]";
    }

    private String nomVariableAffiche() {
        if (noeud.nomCibleVariable != null && !noeud.nomCibleVariable.isEmpty()) return noeud.nomCibleVariable;
        return (noeud.getCibleVariable() != null && noeud.getCibleVariable().nom != null) ? noeud.getCibleVariable().nom : "[?]";
    }

    private String resumeParametres() {
        StringBuilder sb = new StringBuilder();
        if (noeud.getNomsParametres() != null) {
            for (String p : noeud.getNomsParametres()) {
                String val = AideSaisie.texteAffiche(noeud, p);
                if (val != null && !val.isEmpty()) {
                    if (sb.length() > 0) sb.append(" | ");
                    sb.append(Traducteur.get(p)).append(": ").append(val);
                }
            }
        }
        return sb.toString();
    }

    private void mettreAJourResume() {
        boolean comparaisonGenerique = false;
        if (noeud.requiertCibleVariable() && noeud.getNomsParametres() != null) {
            comparaisonGenerique = noeud.getNomsParametres().contains("Opérateur")
                    && noeud.getNomsParametres().contains("Valeur de comparaison");
        }

        if (noeud instanceof NoeudEventCollisionAB || noeud instanceof NoeudConditionSiObjetToucheZone) {
            txtResume.setVisibility(View.VISIBLE);
            txtResume.setText(Traducteur.get("resume_interaction") + " : "
                    + nomObjetAffiche(noeud.nomCibleObjet, noeud.getCibleObjet()) + " <-> "
                    + nomObjetAffiche(noeud.nomCibleObjetB, noeud.getCibleObjetB()));
        } else if (noeud.nom.equals("Condition") || comparaisonGenerique) {
            txtResume.setVisibility(View.VISIBLE);
            String op = noeud.getValeurParametre("Opérateur");
            if (op == null || op.isEmpty()) op = "=";
            String val = noeud.getValeurParametre("Valeur de comparaison");
            if (val == null) val = "";
            txtResume.setText(Traducteur.get("resume_expression") + " : " + nomVariableAffiche() + " " + op + " " + val);
        } else if (noeud.requiertCibleVariable()) {
            txtResume.setVisibility(View.VISIBLE);
            txtResume.setText(Traducteur.get("resume_action") + " : " + nomVariableAffiche() + " = " + resumeParametres());
        } else if (noeud.requiertCibleObjet()) {
            txtResume.setVisibility(View.VISIBLE);
            String parametres = resumeParametres();
            txtResume.setText(Traducteur.get("resume_action_objet") + " : "
                    + nomObjetAffiche(noeud.nomCibleObjet, noeud.getCibleObjet())
                    + (parametres.isEmpty() ? "" : " -> " + parametres));
        } else {
            txtResume.setVisibility(View.GONE);
        }
    }
}
// bas 3
