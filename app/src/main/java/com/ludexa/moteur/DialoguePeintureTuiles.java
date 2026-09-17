// haut 1
package com.ludexa.moteur;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DialoguePeintureTuiles extends Dialog {

    private Context ctx;
    private ObjetBase objetTileset;
    private String cheminProjet;

    private VueGrillePeinture vueGrille;
    private LinearLayout contenuTiroirDeplie;
    private ImageButton btnBasculerTiroir;
    private ImageView miniatureSelectionCollapsed;
    private LinearLayout conteneurVignettesPalette;

    private boolean tiroirDeplie = false;
    private int indexTuileSelectionnee = 0;
    private boolean modeGomme = false;

    public DialoguePeintureTuiles(Context context, ObjetBase objetTileset, String cheminProjet) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.ctx = context;
        this.objetTileset = objetTileset;
        this.cheminProjet = cheminProjet;
        initUI();
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

    private void styliserBoutonIcone(ImageButton btn) {
        btn.setBackground(fond(Palette.boutonNormal, Palette.bordure, 8));
        btn.setPadding(dp(10), dp(10), dp(10), dp(10));
        btn.setColorFilter(Palette.iconeNormal);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44), dp(44));
        lp.setMargins(dp(3), dp(4), dp(3), dp(4));
        btn.setLayoutParams(lp);
    }
// bas 1
// haut 2
    private void initUI() {
        LinearLayout layoutPrincipal = new LinearLayout(ctx);
        layoutPrincipal.setOrientation(LinearLayout.VERTICAL);
        layoutPrincipal.setBackgroundColor(Palette.fondPanneaux);
        layoutPrincipal.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // --- EN-TETE ---
        LinearLayout enTete = new LinearLayout(ctx);
        enTete.setOrientation(LinearLayout.HORIZONTAL);
        enTete.setGravity(Gravity.CENTER_VERTICAL);
        enTete.setBackground(fond(Palette.enTeteDialogues, Palette.bordure, 0));
        enTete.setPadding(dp(6), dp(6), dp(6), dp(6));

        ImageButton btnFermer = new ImageButton(ctx);
        btnFermer.setImageResource(R.drawable.exit_to_app_24px);
        btnFermer.setBackground(null);
        btnFermer.setColorFilter(Palette.texteDesactive);
        LinearLayout.LayoutParams lpFermer = new LinearLayout.LayoutParams(dp(40), dp(40));
        lpFermer.setMargins(0, 0, dp(10), 0);
        btnFermer.setLayoutParams(lpFermer);
        btnFermer.setOnClickListener(v -> dismiss());
        enTete.addView(btnFermer);

        TextView titre = new TextView(ctx);
        titre.setText(Traducteur.get("titre_dialogue_peinture_tuiles"));
        titre.setTextColor(Palette.texteSelectionne);
        titre.setTextSize(16f);
        titre.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lpTitre = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titre.setLayoutParams(lpTitre);
        enTete.addView(titre);

        ImageButton btnAnnulerAction = new ImageButton(ctx);
        btnAnnulerAction.setImageResource(R.drawable.undo_24px);
        styliserBoutonIcone(btnAnnulerAction);
        btnAnnulerAction.setOnClickListener(v -> vueGrille.annulerDerniereAction());
        enTete.addView(btnAnnulerAction);

        layoutPrincipal.addView(enTete);

        // --- ZONE CANVAS + BOUTONS ZOOM FLOTTANTS ---
        FrameLayout zoneCanvas = new FrameLayout(ctx);
        zoneCanvas.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        vueGrille = new VueGrillePeinture(ctx);
        zoneCanvas.addView(vueGrille, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout blocZoom = new LinearLayout(ctx);
        blocZoom.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams lpZoom = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpZoom.gravity = Gravity.END | Gravity.TOP;
        lpZoom.setMargins(0, dp(12), dp(12), 0);
        blocZoom.setLayoutParams(lpZoom);

        ImageButton btnZoomIn = new ImageButton(ctx);
        btnZoomIn.setImageResource(R.drawable.zoom_in_24px);
        styliserBoutonIcone(btnZoomIn);
        btnZoomIn.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        btnZoomIn.setOnClickListener(v -> vueGrille.appliquerZoom(1.2f));
        blocZoom.addView(btnZoomIn);

        ImageButton btnZoomOut = new ImageButton(ctx);
        btnZoomOut.setImageResource(R.drawable.zoom_out_24px);
        styliserBoutonIcone(btnZoomOut);
        btnZoomOut.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        btnZoomOut.setOnClickListener(v -> vueGrille.appliquerZoom(0.8f));
        blocZoom.addView(btnZoomOut);

        ImageButton btnRecentrer = new ImageButton(ctx);
        btnRecentrer.setImageResource(R.drawable.center_focus_weak_24px);
        styliserBoutonIcone(btnRecentrer);
        btnRecentrer.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        btnRecentrer.setOnClickListener(v -> vueGrille.centrerVue());
        blocZoom.addView(btnRecentrer);

        zoneCanvas.addView(blocZoom);
        layoutPrincipal.addView(zoneCanvas);

        // --- TIROIR DE TUILES (bas, coulissant) ---
        LinearLayout tiroir = new LinearLayout(ctx);
        tiroir.setOrientation(LinearLayout.VERTICAL);
        tiroir.setBackground(fond(Palette.fondPanneaux, Palette.bordure, 0));
        tiroir.setPadding(dp(10), dp(6), dp(10), dp(10));

        LinearLayout ligneCollapsed = new LinearLayout(ctx);
        ligneCollapsed.setOrientation(LinearLayout.HORIZONTAL);
        ligneCollapsed.setGravity(Gravity.CENTER_VERTICAL);
        ligneCollapsed.setPadding(dp(4), dp(6), dp(4), dp(6));

        miniatureSelectionCollapsed = new ImageView(ctx);
        LinearLayout.LayoutParams lpMini = new LinearLayout.LayoutParams(dp(40), dp(40));
        lpMini.setMargins(0, 0, dp(10), 0);
        miniatureSelectionCollapsed.setLayoutParams(lpMini);
        miniatureSelectionCollapsed.setBackground(fond(Palette.fondVignette, Palette.accentTeal, 6));
        miniatureSelectionCollapsed.setScaleType(ImageView.ScaleType.FIT_XY);
        ligneCollapsed.addView(miniatureSelectionCollapsed);

        TextView labelTuiles = new TextView(ctx);
        labelTuiles.setText(Traducteur.get("peinture_tuiles_label"));
        labelTuiles.setTextColor(Palette.texteNormal);
        labelTuiles.setTextSize(14f);
        LinearLayout.LayoutParams lpLabel = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        labelTuiles.setLayoutParams(lpLabel);
        ligneCollapsed.addView(labelTuiles);

        btnBasculerTiroir = new ImageButton(ctx);
        btnBasculerTiroir.setImageResource(R.drawable.unfold_more_24px);
        styliserBoutonIcone(btnBasculerTiroir);
        btnBasculerTiroir.setOnClickListener(v -> basculerTiroir());
        ligneCollapsed.addView(btnBasculerTiroir);

        tiroir.addView(ligneCollapsed);

        contenuTiroirDeplie = new LinearLayout(ctx);
        contenuTiroirDeplie.setOrientation(LinearLayout.VERTICAL);
        contenuTiroirDeplie.setVisibility(View.GONE);

        LinearLayout ligneOutils = new LinearLayout(ctx);
        ligneOutils.setOrientation(LinearLayout.HORIZONTAL);
        ligneOutils.setPadding(0, dp(6), 0, dp(6));

        Button btnPeindre = new Button(ctx);
        btnPeindre.setText(Traducteur.get("peinture_outil_peindre"));
        styliserBouton(btnPeindre);
        Button btnGomme = new Button(ctx);
        btnGomme.setText(Traducteur.get("peinture_outil_gomme"));
        styliserBouton(btnGomme);

        btnPeindre.setOnClickListener(v -> {
            modeGomme = false;
            btnPeindre.setBackground(fond(Palette.accentTeal, Palette.bordure, 8));
            btnGomme.setBackground(fond(Palette.boutonNormal, Palette.bordure, 8));
        });
        btnGomme.setOnClickListener(v -> {
            modeGomme = true;
            btnGomme.setBackground(fond(Palette.accentTeal, Palette.bordure, 8));
            btnPeindre.setBackground(fond(Palette.boutonNormal, Palette.bordure, 8));
        });
        btnPeindre.setBackground(fond(Palette.accentTeal, Palette.bordure, 8));

        ligneOutils.addView(btnPeindre);
        ligneOutils.addView(btnGomme);
        contenuTiroirDeplie.addView(ligneOutils);

        HorizontalScrollView scrollPalette = new HorizontalScrollView(ctx);
        scrollPalette.setHorizontalScrollBarEnabled(false);
        conteneurVignettesPalette = new LinearLayout(ctx);
        conteneurVignettesPalette.setOrientation(LinearLayout.HORIZONTAL);
        scrollPalette.addView(conteneurVignettesPalette);
        contenuTiroirDeplie.addView(scrollPalette);

        tiroir.addView(contenuTiroirDeplie);
        layoutPrincipal.addView(tiroir);

        setContentView(layoutPrincipal);

        chargerPalette();
    }

    private void basculerTiroir() {
        tiroirDeplie = !tiroirDeplie;
        contenuTiroirDeplie.setVisibility(tiroirDeplie ? View.VISIBLE : View.GONE);
        btnBasculerTiroir.setImageResource(tiroirDeplie ? R.drawable.unfold_less_24px : R.drawable.unfold_more_24px);
    }
// bas 2


// haut 3
    private void chargerPalette() {
        conteneurVignettesPalette.removeAllViews();
        if (objetTileset.cheminTileset == null || cheminProjet == null) return;

        Bitmap bmpTileset;
        try {
            File imgFile = new File(cheminProjet, objetTileset.cheminTileset);
            bmpTileset = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        } catch (Exception e) {
            bmpTileset = null;
        }
        if (bmpTileset == null) return;

        int lt = Math.max(1, objetTileset.largeurTuilePx);
        int ht = Math.max(1, objetTileset.hauteurTuilePx);
        int marge = Math.max(0, objetTileset.margeTuilePx);
        int decalage = Math.max(0, objetTileset.decalageTuilePx);
        int colonnes = Math.max(1, (bmpTileset.getWidth() - decalage * 2 + marge) / (lt + marge));
        int lignes = Math.max(1, (bmpTileset.getHeight() - decalage * 2 + marge) / (ht + marge));
        int nbTuiles = colonnes * lignes;

        for (int index = 0; index < nbTuiles; index++) {
            int col = index % colonnes;
            int row = index / colonnes;
            int x = decalage + col * (lt + marge);
            int y = decalage + row * (ht + marge);
            ajouterVignettePalette(bmpTileset, index, x, y, lt, ht);
        }

        mettreAJourMiniatureCollapsed(bmpTileset);
    }

    private void ajouterVignettePalette(Bitmap bmpTileset, int index, int x, int y, int largeurPx, int hauteurPx) {
        boolean estSolide = objetTileset.tuilesSolides != null && index < objetTileset.tuilesSolides.length && objetTileset.tuilesSolides[index];
        boolean estSelectionnee = (index == indexTuileSelectionnee);

        FrameLayout carte = new FrameLayout(ctx);
        int couleurBordure = estSelectionnee ? Palette.accentTeal : (estSolide ? Palette.accentAmbre : Palette.bordure);
        carte.setBackground(fond(Palette.fondVignette, couleurBordure, 8));
        LinearLayout.LayoutParams lpCarte = new LinearLayout.LayoutParams(dp(56), dp(56));
        lpCarte.setMargins(dp(3), dp(3), dp(3), dp(3));
        carte.setLayoutParams(lpCarte);

        ImageView miniature = new ImageView(ctx);
        miniature.setLayoutParams(new FrameLayout.LayoutParams(dp(52), dp(52)));
        miniature.setScaleType(ImageView.ScaleType.FIT_XY);
        try {
            int xClamp = Math.max(0, Math.min(x, bmpTileset.getWidth() - 1));
            int yClamp = Math.max(0, Math.min(y, bmpTileset.getHeight() - 1));
            int wClamp = Math.max(1, Math.min(largeurPx, bmpTileset.getWidth() - xClamp));
            int hClamp = Math.max(1, Math.min(hauteurPx, bmpTileset.getHeight() - yClamp));
            Bitmap sousTuile = Bitmap.createBitmap(bmpTileset, xClamp, yClamp, wClamp, hClamp);
            miniature.setImageBitmap(sousTuile);
        } catch (Exception ignored) {}
        carte.addView(miniature);

        carte.setOnClickListener(v -> {
            indexTuileSelectionnee = index;
            modeGomme = false;
            chargerPalette();
        });

        conteneurVignettesPalette.addView(carte);
    }

    private void mettreAJourMiniatureCollapsed(Bitmap bmpTileset) {
        int lt = Math.max(1, objetTileset.largeurTuilePx);
        int ht = Math.max(1, objetTileset.hauteurTuilePx);
        int marge = Math.max(0, objetTileset.margeTuilePx);
        int decalage = Math.max(0, objetTileset.decalageTuilePx);
        int colonnes = Math.max(1, (bmpTileset.getWidth() - decalage * 2 + marge) / (lt + marge));

        int col = indexTuileSelectionnee % colonnes;
        int row = indexTuileSelectionnee / colonnes;
        int x = decalage + col * (lt + marge);
        int y = decalage + row * (ht + marge);
        try {
            int xClamp = Math.max(0, Math.min(x, bmpTileset.getWidth() - 1));
            int yClamp = Math.max(0, Math.min(y, bmpTileset.getHeight() - 1));
            int wClamp = Math.max(1, Math.min(lt, bmpTileset.getWidth() - xClamp));
            int hClamp = Math.max(1, Math.min(ht, bmpTileset.getHeight() - yClamp));
            Bitmap sousTuile = Bitmap.createBitmap(bmpTileset, xClamp, yClamp, wClamp, hClamp);
            miniatureSelectionCollapsed.setImageBitmap(sousTuile);
        } catch (Exception ignored) {}
    }
// bas 3

// haut 4
    private class VueGrillePeinture extends View {

        private Bitmap bmpTileset;
        private int colonnesTileset = 1;
        private float niveauZoom = 1.0f;
        private float cameraX = 0f;
        private float cameraY = 0f;
        private boolean centrageInitialFait = false;
        private boolean enTrainDePeindre = false;
        private Float dernierFocusX = null;
        private Float dernierFocusY = null;

        private List<int[][]> historique = new ArrayList<>();

        private Paint paintGrille = new Paint();
        private Paint paintTuile = new Paint();
        private Paint paintSolide = new Paint();
        private ScaleGestureDetector scaleDetector;

        public VueGrillePeinture(Context context) {
            super(context);
            paintGrille.setStyle(Paint.Style.STROKE);
            paintGrille.setStrokeWidth(1f);
            paintGrille.setColor(Palette.bordure);
            paintSolide.setStyle(Paint.Style.STROKE);
            paintSolide.setColor(Palette.accentAmbre);
            scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    niveauZoom *= detector.getScaleFactor();
                    niveauZoom = Math.max(0.3f, Math.min(niveauZoom, 4.0f));
                    invalidate();
                    return true;
                }
            });
            chargerTileset();
        }

        private void chargerTileset() {
            if (objetTileset.cheminTileset == null || cheminProjet == null) return;
            try {
                File imgFile = new File(cheminProjet, objetTileset.cheminTileset);
                bmpTileset = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            } catch (Exception e) {
                bmpTileset = null;
            }
            if (bmpTileset != null) {
                int lt = Math.max(1, objetTileset.largeurTuilePx);
                int marge = Math.max(0, objetTileset.margeTuilePx);
                int decalage = Math.max(0, objetTileset.decalageTuilePx);
                colonnesTileset = Math.max(1, (bmpTileset.getWidth() - decalage * 2 + marge) / (lt + marge));
            }
        }

        public void appliquerZoom(float facteur) {
            niveauZoom *= facteur;
            niveauZoom = Math.max(0.3f, Math.min(niveauZoom, 4.0f));
            invalidate();
        }

        public void centrerVue() {
            if (getWidth() == 0 || getHeight() == 0) return;
            float contenuLargeur = objetTileset.largeurGrille * objetTileset.largeurTuilePx * niveauZoom;
            float contenuHauteur = objetTileset.hauteurGrille * objetTileset.hauteurTuilePx * niveauZoom;
            cameraX = (getWidth() - contenuLargeur) / 2f;
            cameraY = (getHeight() - contenuHauteur) / 2f;
            invalidate();
        }

        public void annulerDerniereAction() {
            if (historique.isEmpty()) return;
            int[][] derniere = historique.remove(historique.size() - 1);
            objetTileset.grilleTuiles = derniere;
            invalidate();
        }

        private void enregistrerHistorique() {
            if (objetTileset.grilleTuiles == null) return;
            int[][] copie = new int[objetTileset.grilleTuiles.length][];
            for (int i = 0; i < copie.length; i++) {
                copie[i] = objetTileset.grilleTuiles[i] != null ? objetTileset.grilleTuiles[i].clone() : null;
            }
            historique.add(copie);
            if (historique.size() > 15) historique.remove(0);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Palette.fondNormal);

            if (!centrageInitialFait && getWidth() > 0 && getHeight() > 0) {
                centrerVue();
                centrageInitialFait = true;
            }

            canvas.save();
            canvas.translate(cameraX, cameraY);
            canvas.scale(niveauZoom, niveauZoom);

            int lt = Math.max(1, objetTileset.largeurTuilePx);
            int ht = Math.max(1, objetTileset.hauteurTuilePx);
            int nbCols = objetTileset.largeurGrille;
            int nbLignes = objetTileset.hauteurGrille;

            if (objetTileset.grilleTuiles != null && bmpTileset != null) {
                for (int row = 0; row < objetTileset.grilleTuiles.length; row++) {
                    int[] ligne = objetTileset.grilleTuiles[row];
                    if (ligne == null) continue;
                    for (int col = 0; col < ligne.length; col++) {
                        int indexTuile = ligne[col];
                        if (indexTuile < 0) continue;
                        dessinerTuile(canvas, indexTuile, col, row, lt, ht);
                    }
                }
            }

            paintGrille.setStrokeWidth(1f / niveauZoom);
            for (int col = 0; col <= nbCols; col++) {
                float xg = col * lt;
                canvas.drawLine(xg, 0, xg, nbLignes * ht, paintGrille);
            }
            for (int row = 0; row <= nbLignes; row++) {
                float yg = row * ht;
                canvas.drawLine(0, yg, nbCols * lt, yg, paintGrille);
            }

            canvas.restore();
        }

        private void dessinerTuile(Canvas canvas, int indexTuile, int col, int row, int lt, int ht) {
            int marge = Math.max(0, objetTileset.margeTuilePx);
            int decalage = Math.max(0, objetTileset.decalageTuilePx);
            int colSource = indexTuile % colonnesTileset;
            int rowSource = indexTuile / colonnesTileset;
            int xSource = decalage + colSource * (lt + marge);
            int ySource = decalage + rowSource * (ht + marge);

            int xClamp = Math.max(0, Math.min(xSource, bmpTileset.getWidth() - 1));
            int yClamp = Math.max(0, Math.min(ySource, bmpTileset.getHeight() - 1));
            int wClamp = Math.max(1, Math.min(lt, bmpTileset.getWidth() - xClamp));
            int hClamp = Math.max(1, Math.min(ht, bmpTileset.getHeight() - yClamp));

            Rect rectSource = new Rect(xClamp, yClamp, xClamp + wClamp, yClamp + hClamp);
            float destX = col * (float) lt;
            float destY = row * (float) ht;
            RectF rectDest = new RectF(destX, destY, destX + lt, destY + ht);
            canvas.drawBitmap(bmpTileset, rectSource, rectDest, paintTuile);

            boolean estSolide = objetTileset.tuilesSolides != null && indexTuile < objetTileset.tuilesSolides.length && objetTileset.tuilesSolides[indexTuile];
            if (estSolide) {
                paintSolide.setStrokeWidth(2f / niveauZoom);
                canvas.drawRect(rectDest, paintSolide);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            scaleDetector.onTouchEvent(event);
            int action = event.getActionMasked();
            int pointerCount = event.getPointerCount();

            if (pointerCount >= 2) {
                float focusX = (event.getX(0) + event.getX(1)) / 2f;
                float focusY = (event.getY(0) + event.getY(1)) / 2f;
                if (dernierFocusX == null) {
                    dernierFocusX = focusX;
                    dernierFocusY = focusY;
                } else if (action == MotionEvent.ACTION_MOVE) {
                    cameraX += (focusX - dernierFocusX);
                    cameraY += (focusY - dernierFocusY);
                    dernierFocusX = focusX;
                    dernierFocusY = focusY;
                    invalidate();
                }
                enTrainDePeindre = false;
                return true;
            } else {
                dernierFocusX = null;
                dernierFocusY = null;
            }

            if (scaleDetector.isInProgress()) return true;

            float x = event.getX();
            float y = event.getY();

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    enregistrerHistorique();
                    enTrainDePeindre = true;
                    peindreALaPosition(x, y);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (enTrainDePeindre) peindreALaPosition(x, y);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    enTrainDePeindre = false;
                    return true;
            }
            return super.onTouchEvent(event);
        }

        private void peindreALaPosition(float ex, float ey) {
            if (objetTileset.grilleTuiles == null || bmpTileset == null) return;
            float contentX = (ex - cameraX) / niveauZoom;
            float contentY = (ey - cameraY) / niveauZoom;
            int col = (int) Math.floor(contentX / (float) objetTileset.largeurTuilePx);
            int row = (int) Math.floor(contentY / (float) objetTileset.hauteurTuilePx);
            if (col < 0 || row < 0 || col >= objetTileset.largeurGrille || row >= objetTileset.hauteurGrille) return;
            if (row >= objetTileset.grilleTuiles.length || objetTileset.grilleTuiles[row] == null) return;
            if (col >= objetTileset.grilleTuiles[row].length) return;

            int nouvelIndex = modeGomme ? -1 : indexTuileSelectionnee;
            if (objetTileset.grilleTuiles[row][col] != nouvelIndex) {
                objetTileset.grilleTuiles[row][col] = nouvelIndex;
                invalidate();
            }
        }
    }
}
// bas 4




  

  


