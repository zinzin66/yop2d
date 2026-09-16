// haut 1
package com.ludexa.moteur;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DialogueConfigTileset extends Dialog {

    private Context ctx;
    private ObjetBase objetTileset;
    private String cheminProjet;

    private Bitmap bitmapTileset;
    private int colonnesCalculees = 0;
    private int lignesCalculees = 0;
    private int nbTuilesCalculees = 0;

    private TextView tvImageActuelle;
    private EditText champLargeurTuile, champHauteurTuile, champMarge, champDecalage;
    private EditText champLargeurGrille, champHauteurGrille;
    private TextView tvNbTuiles;
    private GridLayout grilleVignettes;

    public DialogueConfigTileset(Context context, ObjetBase objetTileset, String cheminProjet) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.ctx = context;
        this.objetTileset = objetTileset;
        this.cheminProjet = cheminProjet;
        initUI();
        chargerImageActuelle();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && getWindow() != null) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }

    private int dp(int valeur) {
        return (int) (valeur * ctx.getResources().getDisplayMetrics().density);
    }

    private android.graphics.drawable.GradientDrawable fond(int couleurFond, int couleurBordure, int rayon) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(couleurFond);
        g.setCornerRadius(dp(rayon));
        g.setStroke(dp(1), couleurBordure);
        return g;
    }

    private void styliserSection(LinearLayout contenu) {
        contenu.setBackground(fond(Palette.fondNormal, Palette.bordure, 10));
        contenu.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        contenu.setLayoutParams(lp);
    }

    private void styliserSousTitre(TextView t) {
        t.setTextColor(Palette.texteSelectionne);
        t.setTextSize(15f);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(0, 0, 0, dp(10));
    }

    private void styliserLabel(TextView t) {
        t.setTextColor(Palette.texteNormal);
        t.setTextSize(13f);
        t.setPadding(dp(2), dp(6), dp(2), dp(4));
    }

    private void styliserChampFlexible(EditText champ) {
        champ.setTextColor(Palette.texteNormal);
        champ.setHintTextColor(Palette.bordure);
        champ.setBackground(fond(Palette.fondNormal, Palette.bordure, 8));
        champ.setPadding(dp(12), dp(10), dp(12), dp(10));
        champ.setTextSize(15f);
        champ.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(3), dp(3), dp(3), dp(3));
        champ.setLayoutParams(lp);
    }

    private void styliserBouton(Button b) {
        b.setAllCaps(false);
        b.setTextColor(Palette.texteNormal);
        b.setTextSize(14f);
        b.setBackground(fond(Palette.boutonNormal, Palette.bordure, 8));
        b.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(3), dp(4), dp(3), dp(4));
        b.setLayoutParams(lp);
    }

    private void styliserBoutonPleineLargeur(Button b, int couleurFond) {
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15f);
        b.setBackground(fond(couleurFond, Palette.bordure, 8));
        b.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(3), dp(6), dp(3), dp(3));
        b.setLayoutParams(lp);
    }
// bas 1

// haut 2
    private void initUI() {
        LinearLayout layoutPrincipal = new LinearLayout(ctx);
        layoutPrincipal.setOrientation(LinearLayout.VERTICAL);
        layoutPrincipal.setBackgroundColor(Palette.fondPanneaux);
        layoutPrincipal.setPadding(dp(12), dp(12), dp(12), dp(12));
        layoutPrincipal.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(dp(12), dp(12), dp(12), dp(12) + insets.getSystemWindowInsetBottom());
            return insets;
        });
        layoutPrincipal.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // --- EN-TETE ---
        LinearLayout enTete = new LinearLayout(ctx);
        enTete.setOrientation(LinearLayout.HORIZONTAL);
        enTete.setGravity(Gravity.CENTER_VERTICAL);
        enTete.setBackground(fond(Palette.enTeteDialogues, Palette.bordure, 8));
        enTete.setPadding(dp(6), dp(4), dp(6), dp(4));
        LinearLayout.LayoutParams lpEntete = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpEntete.setMargins(0, 0, 0, dp(10));
        enTete.setLayoutParams(lpEntete);

        ImageButton btnFermerCroix = new ImageButton(ctx);
        btnFermerCroix.setImageResource(R.drawable.exit_to_app_24px);
        btnFermerCroix.setBackground(null);
        btnFermerCroix.setColorFilter(Palette.texteDesactive);
        btnFermerCroix.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams lpCroix = new LinearLayout.LayoutParams(dp(40), dp(40));
        lpCroix.setMargins(0, 0, dp(10), 0);
        btnFermerCroix.setLayoutParams(lpCroix);
        btnFermerCroix.setOnClickListener(v -> dismiss());
        enTete.addView(btnFermerCroix);

        TextView titre = new TextView(ctx);
        titre.setText(Traducteur.get("titre_dialogue_config_tileset"));
        titre.setTextColor(Palette.texteSelectionne);
        titre.setTextSize(17f);
        titre.setTypeface(null, android.graphics.Typeface.BOLD);
        enTete.addView(titre);

        layoutPrincipal.addView(enTete);

        ScrollView scroll = new ScrollView(ctx);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout corps = new LinearLayout(ctx);
        corps.setOrientation(LinearLayout.VERTICAL);

        // --- SECTION IMAGE ---
        LinearLayout blocImage = new LinearLayout(ctx);
        blocImage.setOrientation(LinearLayout.VERTICAL);
        styliserSection(blocImage);

        TextView sepImage = new TextView(ctx);
        sepImage.setText(Traducteur.get("tileset_section_image"));
        styliserSousTitre(sepImage);
        blocImage.addView(sepImage);

        Button btnChoisirImage = new Button(ctx);
        btnChoisirImage.setText(Traducteur.get("tileset_choisir_image"));
        styliserBouton(btnChoisirImage);
        btnChoisirImage.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        btnChoisirImage.setOnClickListener(v -> ouvrirSelecteurImage());
        blocImage.addView(btnChoisirImage);

        tvImageActuelle = new TextView(ctx);
        tvImageActuelle.setTextColor(Palette.texteNormal);
        tvImageActuelle.setTextSize(13f);
        tvImageActuelle.setPadding(dp(2), dp(8), dp(2), dp(2));
        blocImage.addView(tvImageActuelle);

        corps.addView(blocImage);

        // --- SECTION DECOUPE ---
        LinearLayout blocDecoupe = new LinearLayout(ctx);
        blocDecoupe.setOrientation(LinearLayout.VERTICAL);
        styliserSection(blocDecoupe);

        TextView sepDecoupe = new TextView(ctx);
        sepDecoupe.setText(Traducteur.get("tileset_section_decoupe"));
        styliserSousTitre(sepDecoupe);
        blocDecoupe.addView(sepDecoupe);

        TextView labelTailleTuile = new TextView(ctx);
        labelTailleTuile.setText(Traducteur.get("tileset_largeur_tuile") + " / " + Traducteur.get("tileset_hauteur_tuile"));
        styliserLabel(labelTailleTuile);
        blocDecoupe.addView(labelTailleTuile);

        LinearLayout ligneTaille = new LinearLayout(ctx);
        ligneTaille.setOrientation(LinearLayout.HORIZONTAL);
        champLargeurTuile = new EditText(ctx);
        champLargeurTuile.setHint(Traducteur.get("tileset_largeur_tuile"));
        styliserChampFlexible(champLargeurTuile);
        champHauteurTuile = new EditText(ctx);
        champHauteurTuile.setHint(Traducteur.get("tileset_hauteur_tuile"));
        styliserChampFlexible(champHauteurTuile);
        ligneTaille.addView(champLargeurTuile);
        ligneTaille.addView(champHauteurTuile);
        blocDecoupe.addView(ligneTaille);

        TextView labelMargeDecalage = new TextView(ctx);
        labelMargeDecalage.setText(Traducteur.get("tileset_marge") + " / " + Traducteur.get("tileset_decalage"));
        styliserLabel(labelMargeDecalage);
        blocDecoupe.addView(labelMargeDecalage);

        LinearLayout ligneMarge = new LinearLayout(ctx);
        ligneMarge.setOrientation(LinearLayout.HORIZONTAL);
        champMarge = new EditText(ctx);
        champMarge.setHint(Traducteur.get("tileset_marge"));
        styliserChampFlexible(champMarge);
        champDecalage = new EditText(ctx);
        champDecalage.setHint(Traducteur.get("tileset_decalage"));
        styliserChampFlexible(champDecalage);
        ligneMarge.addView(champMarge);
        ligneMarge.addView(champDecalage);
        blocDecoupe.addView(ligneMarge);

        Button btnRecalculer = new Button(ctx);
        btnRecalculer.setText(Traducteur.get("tileset_recalculer"));
        styliserBouton(btnRecalculer);
        btnRecalculer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        btnRecalculer.setOnClickListener(v -> {
            appliquerChampsDecoupe();
            calculerGrille();
            redimensionnerTuilesSolides();
            rafraichirApercuTuiles();
        });
        blocDecoupe.addView(btnRecalculer);

        corps.addView(blocDecoupe);

        // --- SECTION DIMENSIONS DE LA CARTE ---
        LinearLayout blocDimensions = new LinearLayout(ctx);
        blocDimensions.setOrientation(LinearLayout.VERTICAL);
        styliserSection(blocDimensions);

        TextView sepDimensions = new TextView(ctx);
        sepDimensions.setText(Traducteur.get("tileset_section_dimensions"));
        styliserSousTitre(sepDimensions);
        blocDimensions.addView(sepDimensions);

        LinearLayout ligneGrille = new LinearLayout(ctx);
        ligneGrille.setOrientation(LinearLayout.HORIZONTAL);
        champLargeurGrille = new EditText(ctx);
        champLargeurGrille.setHint(Traducteur.get("tileset_largeur_grille"));
        styliserChampFlexible(champLargeurGrille);
        champHauteurGrille = new EditText(ctx);
        champHauteurGrille.setHint(Traducteur.get("tileset_hauteur_grille"));
        styliserChampFlexible(champHauteurGrille);
        ligneGrille.addView(champLargeurGrille);
        ligneGrille.addView(champHauteurGrille);
        blocDimensions.addView(ligneGrille);

        corps.addView(blocDimensions);

        // --- SECTION TUILES SOLIDES ---
        LinearLayout blocSolides = new LinearLayout(ctx);
        blocSolides.setOrientation(LinearLayout.VERTICAL);
        styliserSection(blocSolides);

        TextView sepSolides = new TextView(ctx);
        sepSolides.setText(Traducteur.get("tileset_section_solides"));
        styliserSousTitre(sepSolides);
        blocSolides.addView(sepSolides);

        TextView labelSolides = new TextView(ctx);
        labelSolides.setText(Traducteur.get("tileset_marquer_solide"));
        styliserLabel(labelSolides);
        blocSolides.addView(labelSolides);

        tvNbTuiles = new TextView(ctx);
        tvNbTuiles.setTextColor(Palette.texteSecondaire);
        tvNbTuiles.setTextSize(12f);
        tvNbTuiles.setPadding(dp(2), 0, dp(2), dp(8));
        blocSolides.addView(tvNbTuiles);

        LinearLayout ligneBoutonsSolides = new LinearLayout(ctx);
        ligneBoutonsSolides.setOrientation(LinearLayout.HORIZONTAL);
        Button btnToutSolide = new Button(ctx);
        btnToutSolide.setText(Traducteur.get("tileset_tout_solide"));
        styliserBouton(btnToutSolide);
        btnToutSolide.setOnClickListener(v -> {
            if (objetTileset.tuilesSolides == null) return;
            java.util.Arrays.fill(objetTileset.tuilesSolides, true);
            rafraichirApercuTuiles();
        });
        Button btnToutLibre = new Button(ctx);
        btnToutLibre.setText(Traducteur.get("tileset_tout_libre"));
        styliserBouton(btnToutLibre);
        btnToutLibre.setOnClickListener(v -> {
            if (objetTileset.tuilesSolides == null) return;
            java.util.Arrays.fill(objetTileset.tuilesSolides, false);
            rafraichirApercuTuiles();
        });
        ligneBoutonsSolides.addView(btnToutSolide);
        ligneBoutonsSolides.addView(btnToutLibre);
        blocSolides.addView(ligneBoutonsSolides);

        grilleVignettes = new GridLayout(ctx);
        grilleVignettes.setColumnCount(6);
        LinearLayout.LayoutParams lpGrille = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpGrille.setMargins(0, dp(8), 0, 0);
        grilleVignettes.setLayoutParams(lpGrille);
        blocSolides.addView(grilleVignettes);

        corps.addView(blocSolides);

        scroll.addView(corps);
        layoutPrincipal.addView(scroll);

        Button btnValider = new Button(ctx);
        btnValider.setText(Traducteur.get("tileset_valider"));
        styliserBoutonPleineLargeur(btnValider, Color.parseColor("#4CAF50"));
        btnValider.setOnClickListener(v -> {
            appliquerChampsDecoupe();
            appliquerDimensionsCarte();
            dismiss();
        });
        layoutPrincipal.addView(btnValider);

        setContentView(layoutPrincipal);
    }
// bas 2

  // haut 3
    private void chargerImageActuelle() {
        champLargeurTuile.setText(String.valueOf(objetTileset.largeurTuilePx));
        champHauteurTuile.setText(String.valueOf(objetTileset.hauteurTuilePx));
        champMarge.setText(String.valueOf(objetTileset.margeTuilePx));
        champDecalage.setText(String.valueOf(objetTileset.decalageTuilePx));
        champLargeurGrille.setText(String.valueOf(objetTileset.largeurGrille));
        champHauteurGrille.setText(String.valueOf(objetTileset.hauteurGrille));

        if (objetTileset.cheminTileset != null && cheminProjet != null) {
            tvImageActuelle.setText(objetTileset.cheminTileset);
            try {
                File fichierImage = new File(cheminProjet, objetTileset.cheminTileset);
                bitmapTileset = BitmapFactory.decodeFile(fichierImage.getAbsolutePath());
            } catch (Exception e) {
                bitmapTileset = null;
            }
        } else {
            tvImageActuelle.setText(Traducteur.get("tileset_aucune_image"));
            bitmapTileset = null;
        }

        calculerGrille();
        redimensionnerTuilesSolides();
        rafraichirApercuTuiles();
    }

    private void appliquerChampsDecoupe() {
        try { objetTileset.largeurTuilePx = Math.max(1, Integer.parseInt(champLargeurTuile.getText().toString().trim())); } catch (Exception ignored) {}
        try { objetTileset.hauteurTuilePx = Math.max(1, Integer.parseInt(champHauteurTuile.getText().toString().trim())); } catch (Exception ignored) {}
        try { objetTileset.margeTuilePx = Math.max(0, Integer.parseInt(champMarge.getText().toString().trim())); } catch (Exception ignored) {}
        try { objetTileset.decalageTuilePx = Math.max(0, Integer.parseInt(champDecalage.getText().toString().trim())); } catch (Exception ignored) {}
    }

    private void calculerGrille() {
        if (bitmapTileset == null) {
            colonnesCalculees = 0;
            lignesCalculees = 0;
            nbTuilesCalculees = 0;
            return;
        }
        int imgW = bitmapTileset.getWidth();
        int imgH = bitmapTileset.getHeight();
        int lt = Math.max(1, objetTileset.largeurTuilePx);
        int ht = Math.max(1, objetTileset.hauteurTuilePx);
        int marge = Math.max(0, objetTileset.margeTuilePx);
        int decalage = Math.max(0, objetTileset.decalageTuilePx);

        colonnesCalculees = Math.max(0, (imgW - decalage * 2 + marge) / (lt + marge));
        lignesCalculees = Math.max(0, (imgH - decalage * 2 + marge) / (ht + marge));
        nbTuilesCalculees = colonnesCalculees * lignesCalculees;

        tvNbTuiles.setText(nbTuilesCalculees + " " + Traducteur.get("tileset_nb_tuiles"));
    }

    private void redimensionnerTuilesSolides() {
        boolean[] ancien = objetTileset.tuilesSolides;
        boolean[] nouveau = new boolean[nbTuilesCalculees];
        if (ancien != null) {
            for (int i = 0; i < Math.min(ancien.length, nouveau.length); i++) {
                nouveau[i] = ancien[i];
            }
        }
        objetTileset.tuilesSolides = nouveau;
    }

    private void rafraichirApercuTuiles() {
        grilleVignettes.removeAllViews();
        if (bitmapTileset == null || nbTuilesCalculees <= 0) return;

        int lt = objetTileset.largeurTuilePx;
        int ht = objetTileset.hauteurTuilePx;
        int marge = objetTileset.margeTuilePx;
        int decalage = objetTileset.decalageTuilePx;

        for (int index = 0; index < nbTuilesCalculees; index++) {
            int col = index % colonnesCalculees;
            int row = index / colonnesCalculees;
            int x = decalage + col * (lt + marge);
            int y = decalage + row * (ht + marge);
            ajouterVignetteTuile(index, x, y, lt, ht);
        }
    }

    private void ajouterVignetteTuile(int index, int x, int y, int largeurPx, int hauteurPx) {
        boolean estSolide = objetTileset.tuilesSolides != null && index < objetTileset.tuilesSolides.length && objetTileset.tuilesSolides[index];

        FrameLayout carte = new FrameLayout(ctx);
        carte.setBackground(fond(Palette.fondVignette, estSolide ? Palette.accentAmbre : Palette.bordure, 8));
        GridLayout.LayoutParams lpCarte = new GridLayout.LayoutParams();
        lpCarte.width = dp(60);
        lpCarte.height = dp(60);
        lpCarte.setMargins(dp(3), dp(3), dp(3), dp(3));
        carte.setLayoutParams(lpCarte);

        ImageView miniature = new ImageView(ctx);
        miniature.setLayoutParams(new FrameLayout.LayoutParams(dp(58), dp(58)));
        miniature.setScaleType(ImageView.ScaleType.FIT_XY);
        try {
            int xClamp = Math.max(0, Math.min(x, bitmapTileset.getWidth() - 1));
            int yClamp = Math.max(0, Math.min(y, bitmapTileset.getHeight() - 1));
            int wClamp = Math.max(1, Math.min(largeurPx, bitmapTileset.getWidth() - xClamp));
            int hClamp = Math.max(1, Math.min(hauteurPx, bitmapTileset.getHeight() - yClamp));
            Bitmap sousTuile = Bitmap.createBitmap(bitmapTileset, xClamp, yClamp, wClamp, hClamp);
            miniature.setImageBitmap(sousTuile);
        } catch (Exception ignored) {}
        carte.addView(miniature);

        if (estSolide) {
            ImageView badgeSolide = new ImageView(ctx);
            badgeSolide.setImageResource(R.drawable.activity_zone_24px);
            badgeSolide.setColorFilter(Palette.accentAmbre);
            FrameLayout.LayoutParams lpBadge = new FrameLayout.LayoutParams(dp(20), dp(20));
            lpBadge.gravity = Gravity.TOP | Gravity.END;
            badgeSolide.setLayoutParams(lpBadge);
            carte.addView(badgeSolide);
        }

        carte.setOnClickListener(v -> {
            if (objetTileset.tuilesSolides == null || index >= objetTileset.tuilesSolides.length) return;
            objetTileset.tuilesSolides[index] = !objetTileset.tuilesSolides[index];
            rafraichirApercuTuiles();
        });

        grilleVignettes.addView(carte);
    }

    private void appliquerDimensionsCarte() {
        int nouvelleLargeur = objetTileset.largeurGrille;
        int nouvelleHauteur = objetTileset.hauteurGrille;
        try { nouvelleLargeur = Math.max(1, Integer.parseInt(champLargeurGrille.getText().toString().trim())); } catch (Exception ignored) {}
        try { nouvelleHauteur = Math.max(1, Integer.parseInt(champHauteurGrille.getText().toString().trim())); } catch (Exception ignored) {}

        int[][] ancienneGrille = objetTileset.grilleTuiles;
        int[][] nouvelleGrilleTableau = new int[nouvelleHauteur][nouvelleLargeur];
        for (int[] ligne : nouvelleGrilleTableau) java.util.Arrays.fill(ligne, -1);
        if (ancienneGrille != null) {
            for (int ligneY = 0; ligneY < Math.min(ancienneGrille.length, nouvelleHauteur); ligneY++) {
                if (ancienneGrille[ligneY] == null) continue;
                for (int colX = 0; colX < Math.min(ancienneGrille[ligneY].length, nouvelleLargeur); colX++) {
                    nouvelleGrilleTableau[ligneY][colX] = ancienneGrille[ligneY][colX];
                }
            }
        }
        objetTileset.grilleTuiles = nouvelleGrilleTableau;
        objetTileset.largeurGrille = nouvelleLargeur;
        objetTileset.hauteurGrille = nouvelleHauteur;

        objetTileset.largeur = nouvelleLargeur * (float) objetTileset.largeurTuilePx;
        objetTileset.hauteur = nouvelleHauteur * (float) objetTileset.hauteurTuilePx;
    }

    private void ouvrirSelecteurImage() {
        if (cheminProjet == null) return;
        File dossierImages = new File(cheminProjet, "assets_ludexa/Images");
        List<String> images = listerImagesLocales(dossierImages, "assets_ludexa/Images/");
        if (images.isEmpty()) {
            Toast.makeText(ctx, Traducteur.get("insp_aucune_image"), Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(ctx)
            .setTitle(Traducteur.get("tileset_choisir_image"))
            .setItems(images.toArray(new String[0]), (dialog, which) -> {
                objetTileset.cheminTileset = images.get(which);
                chargerImageActuelle();
            }).show();
    }

    private List<String> listerImagesLocales(File dir, String cheminBase) {
        List<String> resultats = new ArrayList<>();
        if (dir != null && dir.exists() && dir.isDirectory()) {
            File[] fichiers = dir.listFiles();
            if (fichiers != null) {
                for (File f : fichiers) {
                    if (f.isDirectory()) {
                        resultats.addAll(listerImagesLocales(f, cheminBase + f.getName() + "/"));
                    } else {
                        String nom = f.getName().toLowerCase();
                        if (nom.endsWith(".png") || nom.endsWith(".jpg") || nom.endsWith(".jpeg") || nom.endsWith(".webp")) {
                            resultats.add(cheminBase + f.getName());
                        }
                    }
                }
            }
        }
        return resultats;
    }
}
// bas 3



  

