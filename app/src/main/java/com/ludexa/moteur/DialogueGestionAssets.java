// haut 1
package com.ludexa.moteur;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DialogueGestionAssets extends Dialog {

    private Context ctx;
    private CanvasEditeur canvasEditeur;
    private String cheminProjet;
    private File rootAssetsDir;

    private File currentFolderSelected;
    private File currentAssetSelected;
    private String filtreTexte = "";

    private LinearLayout conteneurArborescenceDossiers;
    private GridLayout grilleAssets;
    private EditText champRecherche;
    private TextView titreDossierCourant;

    private MediaPlayer mediaPlayer;
    private File fichierSonEnLecture;
    private ImageButton boutonPlaySonActif;

    public DialogueGestionAssets(Context context, CanvasEditeur canvasEditeur, String cheminProjet) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.ctx = context;
        this.canvasEditeur = canvasEditeur;
        this.cheminProjet = cheminProjet;

        if (cheminProjet != null) {
            rootAssetsDir = new File(cheminProjet, "assets_ludexa");
            if (!rootAssetsDir.exists()) rootAssetsDir.mkdirs();
            new File(rootAssetsDir, "Images").mkdirs();
            new File(rootAssetsDir, "Sons").mkdirs();
            new File(rootAssetsDir, "Fonts").mkdirs();
            new File(rootAssetsDir, "Textes").mkdirs();
        }

        initUI();

        setOnDismissListener(d -> arreterSon());
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

    private void styliserDialogue(LinearLayout layout) {
        layout.setBackgroundColor(Palette.fondPanneaux);
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));
    }

    private void styliserChampDialogue(EditText champ) {
        champ.setTextColor(Palette.texteNormal);
        champ.setHintTextColor(Palette.bordure);
        champ.setBackground(fond(Palette.fondNormal, Palette.bordure, 8));
        champ.setPadding(dp(12), dp(10), dp(12), dp(10));
        champ.setTextSize(14f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        champ.setLayoutParams(lp);
    }

    private void styliserBoutonIcone(ImageButton btn) {
        btn.setBackground(fond(Palette.boutonNormal, Palette.bordure, 8));
        btn.setPadding(dp(10), dp(10), dp(10), dp(10));
        btn.setColorFilter(Palette.iconeNormal);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(3), dp(3), dp(3), dp(3));
        btn.setLayoutParams(lp);
    }

    private boolean isRacineIndestructible(File dir) {
        if (dir == null) return false;
        String nom = dir.getName();
        return (dir.getParentFile() != null && dir.getParentFile().equals(rootAssetsDir)) &&
               (nom.equals("Images") || nom.equals("Sons") || nom.equals("Fonts") || nom.equals("Textes"));
    }
// bas 1

// haut 2
    private void initUI() {
        LinearLayout layoutPrincipal = new LinearLayout(ctx);
        layoutPrincipal.setOrientation(LinearLayout.VERTICAL);
        layoutPrincipal.setBackgroundColor(Palette.fondPanneaux);
        layoutPrincipal.setPadding(dp(12), dp(12), dp(12), dp(12));

        TextView titre = new TextView(ctx);
        titre.setText(Traducteur.get("titre_dialogue_assets"));
        titre.setTextColor(Palette.texteSelectionne);
        titre.setTextSize(18f);
        titre.setTypeface(null, android.graphics.Typeface.BOLD);
        titre.setPadding(0, 0, 0, dp(10));
        layoutPrincipal.addView(titre);

        LinearLayout zoneCentrale = new LinearLayout(ctx);
        zoneCentrale.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lpZoneCentrale = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        zoneCentrale.setLayoutParams(lpZoneCentrale);

        // --- COLONNE GAUCHE : DOSSIERS ---
        LinearLayout colonneGauche = new LinearLayout(ctx);
        colonneGauche.setOrientation(LinearLayout.VERTICAL);
        colonneGauche.setBackground(fond(Palette.fondNormal, Palette.bordure, 8));
        colonneGauche.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams lpColGauche = new LinearLayout.LayoutParams(dp(220), ViewGroup.LayoutParams.MATCH_PARENT);
        lpColGauche.setMargins(0, 0, dp(10), 0);
        colonneGauche.setLayoutParams(lpColGauche);

        TextView titreDossiers = new TextView(ctx);
        titreDossiers.setText(Traducteur.get("titre_dossiers"));
        titreDossiers.setTextColor(Palette.texteSelectionne);
        titreDossiers.setTextSize(14f);
        titreDossiers.setTypeface(null, android.graphics.Typeface.BOLD);
        titreDossiers.setPadding(dp(4), 0, dp(4), dp(8));
        colonneGauche.addView(titreDossiers);

        ScrollView scrollDossiers = new ScrollView(ctx);
        scrollDossiers.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        conteneurArborescenceDossiers = new LinearLayout(ctx);
        conteneurArborescenceDossiers.setOrientation(LinearLayout.VERTICAL);
        scrollDossiers.addView(conteneurArborescenceDossiers);
        colonneGauche.addView(scrollDossiers);

        LinearLayout boutonsDossiers = new LinearLayout(ctx);
        boutonsDossiers.setOrientation(LinearLayout.HORIZONTAL);
        boutonsDossiers.setPadding(0, dp(8), 0, 0);

        ImageButton btnAddFolder = new ImageButton(ctx);
        btnAddFolder.setImageResource(R.drawable.add_24px);
        styliserBoutonIcone(btnAddFolder);
        btnAddFolder.setOnClickListener(v -> {
            if (currentFolderSelected != null) afficherPopupNouveauDossier();
        });

        ImageButton btnEditFolder = new ImageButton(ctx);
        btnEditFolder.setImageResource(R.drawable.edit_square_24px);
        styliserBoutonIcone(btnEditFolder);
        btnEditFolder.setOnClickListener(v -> {
            if (currentFolderSelected != null && !isRacineIndestructible(currentFolderSelected)) {
                afficherPopupRenommerDossier(currentFolderSelected);
            }
        });

        ImageButton btnDelFolder = new ImageButton(ctx);
        btnDelFolder.setImageResource(R.drawable.delete_24px);
        styliserBoutonIcone(btnDelFolder);
        btnDelFolder.setOnClickListener(v -> {
            if (currentFolderSelected != null && !isRacineIndestructible(currentFolderSelected)) {
                afficherPopupSupprimerDossier(currentFolderSelected);
            }
        });

        boutonsDossiers.addView(btnAddFolder);
        boutonsDossiers.addView(btnEditFolder);
        boutonsDossiers.addView(btnDelFolder);
        colonneGauche.addView(boutonsDossiers);

        zoneCentrale.addView(colonneGauche);

        // --- COLONNE DROITE : RECHERCHE + GRILLE + ACTIONS ---
        LinearLayout colonneDroite = new LinearLayout(ctx);
        colonneDroite.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpColDroite = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        colonneDroite.setLayoutParams(lpColDroite);

        titreDossierCourant = new TextView(ctx);
        titreDossierCourant.setTextColor(Palette.texteSelectionne);
        titreDossierCourant.setTextSize(15f);
        titreDossierCourant.setTypeface(null, android.graphics.Typeface.BOLD);
        titreDossierCourant.setPadding(dp(4), 0, dp(4), dp(6));
        colonneDroite.addView(titreDossierCourant);

        champRecherche = new EditText(ctx);
        champRecherche.setHint(Traducteur.get("hint_rechercher_asset"));
        styliserChampDialogue(champRecherche);
        champRecherche.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtreTexte = s.toString().trim().toLowerCase();
                rafraichirListeAssets();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        colonneDroite.addView(champRecherche);

        ScrollView scrollGrille = new ScrollView(ctx);
        scrollGrille.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        grilleAssets = new GridLayout(ctx);
        grilleAssets.setColumnCount(4);
        scrollGrille.addView(grilleAssets);
        colonneDroite.addView(scrollGrille);

        LinearLayout boutonsAssets = new LinearLayout(ctx);
        boutonsAssets.setOrientation(LinearLayout.HORIZONTAL);
        boutonsAssets.setPadding(0, dp(8), 0, 0);

        ImageButton btnImportAsset = new ImageButton(ctx);
        btnImportAsset.setImageResource(R.drawable.upload_file_24px);
        styliserBoutonIcone(btnImportAsset);
        btnImportAsset.setOnClickListener(v -> {
            if (currentFolderSelected == null) return;
            String chemin = currentFolderSelected.getAbsolutePath();
            String mime = chemin.contains("/Images") ? "image/*" : "*/*";
            if (ctx instanceof InterfaceEditeur) {
                ((InterfaceEditeur) ctx).setDialogueAssetsActif(this);
                ((InterfaceEditeur) ctx).lancerImportAsset(mime);
            }
        });

        ImageButton btnEditAsset = new ImageButton(ctx);
        btnEditAsset.setImageResource(R.drawable.edit_square_24px);
        styliserBoutonIcone(btnEditAsset);
        btnEditAsset.setOnClickListener(v -> {
            if (currentAssetSelected != null) afficherPopupRenommerAsset(currentAssetSelected);
        });

        ImageButton btnDelAsset = new ImageButton(ctx);
        btnDelAsset.setImageResource(R.drawable.delete_24px);
        styliserBoutonIcone(btnDelAsset);
        btnDelAsset.setOnClickListener(v -> {
            if (currentAssetSelected != null) afficherPopupSupprimerAsset(currentAssetSelected);
        });

        boutonsAssets.addView(btnImportAsset);
        boutonsAssets.addView(btnEditAsset);
        boutonsAssets.addView(btnDelAsset);
        colonneDroite.addView(boutonsAssets);

        zoneCentrale.addView(colonneDroite);
        layoutPrincipal.addView(zoneCentrale);

        // --- BAS : BOUTONS ANNEXES ---
        LinearLayout ligneAnnexes = new LinearLayout(ctx);
        ligneAnnexes.setOrientation(LinearLayout.HORIZONTAL);
        ligneAnnexes.setPadding(0, dp(10), 0, dp(6));

        Button btnEditeurDial = new Button(ctx);
        btnEditeurDial.setText(Traducteur.get("btn_ouvrir_dialogues"));
        btnEditeurDial.setAllCaps(false);
        btnEditeurDial.setTextColor(Color.WHITE);
        btnEditeurDial.setBackground(fond(Color.parseColor("#4CAF50"), Palette.bordure, 8));
        LinearLayout.LayoutParams lpBtnAnnexe = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpBtnAnnexe.setMargins(dp(3), 0, dp(3), 0);
        btnEditeurDial.setLayoutParams(lpBtnAnnexe);
        btnEditeurDial.setOnClickListener(v -> afficherEditeurTexteGeant());
        ligneAnnexes.addView(btnEditeurDial);

        Button btnAnimations = new Button(ctx);
        btnAnimations.setText(Traducteur.get("btn_gerer_animations"));
        btnAnimations.setAllCaps(false);
        btnAnimations.setTextColor(Color.WHITE);
        btnAnimations.setBackground(fond(Color.parseColor("#673AB7"), Palette.bordure, 8));
        LinearLayout.LayoutParams lpBtnAnnexe2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpBtnAnnexe2.setMargins(dp(3), 0, dp(3), 0);
        btnAnimations.setLayoutParams(lpBtnAnnexe2);
        btnAnimations.setOnClickListener(v -> {
            EditeurAnimationsDialog dialog = new EditeurAnimationsDialog(ctx, cheminProjet);
            dialog.show();
        });
        ligneAnnexes.addView(btnAnimations);

        Button btnStylesTitres = new Button(ctx);
        btnStylesTitres.setText(Traducteur.get("btn_gerer_styles"));
        btnStylesTitres.setAllCaps(false);
        btnStylesTitres.setTextColor(Color.WHITE);
        btnStylesTitres.setBackground(fond(Color.parseColor("#E65100"), Palette.bordure, 8));
        LinearLayout.LayoutParams lpBtnAnnexe3 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpBtnAnnexe3.setMargins(dp(3), 0, dp(3), 0);
        btnStylesTitres.setLayoutParams(lpBtnAnnexe3);
        btnStylesTitres.setOnClickListener(v -> {
            EditeurTexteStyleDialog dialog = new EditeurTexteStyleDialog(ctx, cheminProjet);
            dialog.setOnDismissListener(d -> {
                if (canvasEditeur != null) {
                    canvasEditeur.chargerStylesTitresGlobales();
                    canvasEditeur.invalidate();
                }
            });
            dialog.show();
        });
        ligneAnnexes.addView(btnStylesTitres);

        layoutPrincipal.addView(ligneAnnexes);

        Button btnFermer = new Button(ctx);
        btnFermer.setText(Traducteur.get("bouton_fermer"));
        btnFermer.setTextColor(Palette.texteNormal);
        btnFermer.setBackground(fond(Palette.boutonNormal, Palette.bordure, 8));
        btnFermer.setPadding(dp(16), dp(12), dp(16), dp(12));
        btnFermer.setOnClickListener(v -> dismiss());
        layoutPrincipal.addView(btnFermer);

        setContentView(layoutPrincipal);

        currentFolderSelected = new File(rootAssetsDir, "Images");
        rafraichirArborescenceDossiers();
        rafraichirListeAssets();
    }
// bas 2
// haut 3
    private void rafraichirArborescenceDossiers() {
        if (conteneurArborescenceDossiers == null) return;
        conteneurArborescenceDossiers.removeAllViews();
        construireArbreDossiers(rootAssetsDir, -1);
    }

    private void construireArbreDossiers(File dir, int depth) {
        if (dir == null || !dir.exists()) return;

        if (depth >= 0) {
            LinearLayout layoutDossier = new LinearLayout(ctx);
            layoutDossier.setOrientation(LinearLayout.HORIZONTAL);
            layoutDossier.setGravity(Gravity.CENTER_VERTICAL);
            layoutDossier.setPadding(dp(6), dp(4), dp(6), dp(4));
            if (dir.equals(currentFolderSelected)) {
                layoutDossier.setBackground(fond(Palette.fondListe, Palette.bordure, 8));
            }

            if (depth > 0) {
                TextView tvPrefix = new TextView(ctx);
                StringBuilder prefix = new StringBuilder();
                for (int i = 0; i < depth; i++) prefix.append("   ");
                tvPrefix.setText(prefix.toString());
                layoutDossier.addView(tvPrefix);
            }

            ImageView iconeDossier = new ImageView(ctx);
            iconeDossier.setImageResource(R.drawable.folder_open_24px);
            iconeDossier.setColorFilter(dir.equals(currentFolderSelected) ? Palette.iconeSurvol : Palette.iconeNormal);
            iconeDossier.setPadding(0, 0, dp(8), 0);

            TextView tv = new TextView(ctx);
            String cleDossier = "dossier_" + dir.getName().toLowerCase();
            String nomAffiche = Traducteur.get(cleDossier);
            if (nomAffiche.startsWith("[")) nomAffiche = dir.getName();
            tv.setText(nomAffiche);
            tv.setTextColor(dir.equals(currentFolderSelected) ? Palette.texteSelectionne : Palette.texteNormal);
            tv.setPadding(0, dp(6), 0, dp(6));
            tv.setTextSize(14f);

            layoutDossier.addView(iconeDossier);
            layoutDossier.addView(tv);

            layoutDossier.setOnClickListener(v -> {
                currentFolderSelected = dir;
                currentAssetSelected = null;
                arreterSon();
                rafraichirArborescenceDossiers();
                rafraichirListeAssets();
            });
            conteneurArborescenceDossiers.addView(layoutDossier);
        }

        File[] enfants = dir.listFiles();
        if (enfants != null) {
            java.util.Arrays.sort(enfants, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
            for (File f : enfants) {
                if (f.isDirectory()) construireArbreDossiers(f, depth + 1);
            }
        }
    }

    private int typeFichier(File f) {
        String nom = f.getName().toLowerCase();
        if (nom.endsWith(".png") || nom.endsWith(".jpg") || nom.endsWith(".jpeg") || nom.endsWith(".webp")) return 1;
        if (nom.endsWith(".mp3") || nom.endsWith(".wav") || nom.endsWith(".ogg")) return 2;
        if (nom.endsWith(".ttf") || nom.endsWith(".otf")) return 3;
        return 0;
    }

    private void rafraichirListeAssets() {
        if (grilleAssets == null || currentFolderSelected == null) return;
        grilleAssets.removeAllViews();

        String cleDossier = "dossier_" + currentFolderSelected.getName().toLowerCase();
        String nomAffiche = Traducteur.get(cleDossier);
        if (nomAffiche.startsWith("[")) nomAffiche = currentFolderSelected.getName();
        titreDossierCourant.setText(nomAffiche);

        File[] fichiers = currentFolderSelected.listFiles();
        if (fichiers != null) {
            java.util.Arrays.sort(fichiers, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
            for (File f : fichiers) {
                if (f.isDirectory()) continue;
                if (!filtreTexte.isEmpty() && !f.getName().toLowerCase().contains(filtreTexte)) continue;
                ajouterVigneteAsset(f);
            }
        }
    }
// bas 3

// haut 4
    private void ajouterVigneteAsset(File f) {
        LinearLayout carte = new LinearLayout(ctx);
        carte.setOrientation(LinearLayout.VERTICAL);
        carte.setGravity(Gravity.CENTER_HORIZONTAL);
        carte.setPadding(dp(6), dp(6), dp(6), dp(6));
        if (f.equals(currentAssetSelected)) {
            carte.setBackground(fond(Palette.fondListe, Palette.bordure, 8));
        } else {
            carte.setBackground(fond(Palette.fondNormal, Palette.bordure, 8));
        }

        GridLayout.LayoutParams lpCarte = new GridLayout.LayoutParams();
        lpCarte.width = dp(90);
        lpCarte.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lpCarte.setMargins(dp(4), dp(4), dp(4), dp(4));
        carte.setLayoutParams(lpCarte);

        int type = typeFichier(f);
        FrameLayout zoneVisuelle = new FrameLayout(ctx);
        zoneVisuelle.setLayoutParams(new LinearLayout.LayoutParams(dp(70), dp(70)));

        if (type == 1) {
            ImageView miniature = new ImageView(ctx);
            miniature.setLayoutParams(new FrameLayout.LayoutParams(dp(70), dp(70)));
            try {
                Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
                miniature.setImageBitmap(bmp);
            } catch (Exception e) {}
            miniature.setScaleType(ImageView.ScaleType.CENTER_CROP);
            zoneVisuelle.addView(miniature);

        } else if (type == 2) {
            ImageView iconeSon = new ImageView(ctx);
            iconeSon.setLayoutParams(new FrameLayout.LayoutParams(dp(70), dp(70)));
            iconeSon.setImageResource(R.drawable.upload_file_24px);
            iconeSon.setColorFilter(Palette.iconeNormal);
            iconeSon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            iconeSon.setPadding(dp(18), dp(18), dp(18), dp(18));
            zoneVisuelle.addView(iconeSon);

            boolean enLecture = f.equals(fichierSonEnLecture) && mediaPlayer != null;
            ImageButton btnPlay = new ImageButton(ctx);
            btnPlay.setImageResource(enLecture ? R.drawable.stop_circle_24px : R.drawable.play_circle_24px);
            btnPlay.setBackground(null);
            btnPlay.setColorFilter(Palette.iconeSurvol);
            FrameLayout.LayoutParams lpPlay = new FrameLayout.LayoutParams(dp(32), dp(32));
            lpPlay.gravity = Gravity.CENTER;
            btnPlay.setLayoutParams(lpPlay);
            btnPlay.setOnClickListener(v -> {
                if (f.equals(fichierSonEnLecture) && mediaPlayer != null) {
                    arreterSon();
                } else {
                    jouerSon(f, btnPlay);
                }
                rafraichirListeAssets();
            });
            zoneVisuelle.addView(btnPlay);

        } else if (type == 3) {
            ImageView iconePolice = new ImageView(ctx);
            iconePolice.setLayoutParams(new FrameLayout.LayoutParams(dp(70), dp(70)));
            iconePolice.setImageResource(R.drawable.font_download_24px);
            iconePolice.setColorFilter(Palette.iconeNormal);
            iconePolice.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            iconePolice.setPadding(dp(14), dp(14), dp(14), dp(14));
            zoneVisuelle.addView(iconePolice);

        } else {
            ImageView iconeAutre = new ImageView(ctx);
            iconeAutre.setLayoutParams(new FrameLayout.LayoutParams(dp(70), dp(70)));
            iconeAutre.setImageResource(R.drawable.script_24px);
            iconeAutre.setColorFilter(Palette.iconeNormal);
            iconeAutre.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            iconeAutre.setPadding(dp(16), dp(16), dp(16), dp(16));
            zoneVisuelle.addView(iconeAutre);
        }

        carte.addView(zoneVisuelle);

        TextView nom = new TextView(ctx);
        String nomAffiche = f.getName();
        if (nomAffiche.length() > 12) nomAffiche = nomAffiche.substring(0, 10) + "…";
        nom.setText(nomAffiche);
        nom.setTextSize(11f);
        nom.setGravity(Gravity.CENTER);
        nom.setTextColor(f.equals(currentAssetSelected) ? Palette.texteSelectionne : Palette.texteNormal);
        nom.setPadding(0, dp(4), 0, 0);
        nom.setLayoutParams(new LinearLayout.LayoutParams(dp(70), ViewGroup.LayoutParams.WRAP_CONTENT));
        carte.addView(nom);

        carte.setOnClickListener(v -> {
            currentAssetSelected = f;
            rafraichirListeAssets();
        });

        grilleAssets.addView(carte);
    }

    private void jouerSon(File f, ImageButton bouton) {
        arreterSon();
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(f.getAbsolutePath());
            mediaPlayer.setOnCompletionListener(mp -> {
                arreterSon();
                rafraichirListeAssets();
            });
            mediaPlayer.prepare();
            mediaPlayer.start();
            fichierSonEnLecture = f;
            boutonPlaySonActif = bouton;
        } catch (Exception e) {
            Toast.makeText(ctx, Traducteur.get("erreur_lecture_son"), Toast.LENGTH_SHORT).show();
            mediaPlayer = null;
            fichierSonEnLecture = null;
        }
    }

    private void arreterSon() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {}
            mediaPlayer = null;
        }
        fichierSonEnLecture = null;
        boutonPlaySonActif = null;
    }
// bas 4

// haut 5
    public void traiterImportAsset(Uri uri) {
        String nomOriginal = getFileNameFromUri(uri);
        if (nomOriginal == null) nomOriginal = "asset_import";

        String nomBase = nomOriginal;
        String extension = "";
        int dotIdx = nomOriginal.lastIndexOf('.');
        if (dotIdx > 0) {
            nomBase = nomOriginal.substring(0, dotIdx);
            extension = nomOriginal.substring(dotIdx);
        }

        File fichierCible = genererNomFichierUnique(currentFolderSelected, nomBase, extension);

        try (InputStream in = ctx.getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(fichierCible)) {
            byte[] buffer = new byte[1024];
            int lu;
            while ((lu = in.read(buffer)) != -1) {
                out.write(buffer, 0, lu);
            }
            rafraichirListeAssets();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = ctx.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result != null ? result.lastIndexOf('/') : -1;
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private File genererNomFichierUnique(File dossier, String base, String extension) {
        File f = new File(dossier, base + extension);
        if (!f.exists()) return f;
        int index = 1;
        while (true) {
            f = new File(dossier, base + "_" + index + extension);
            if (!f.exists()) return f;
            index++;
        }
    }

    private void supprimerRecursif(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File child : files) supprimerRecursif(child);
            }
        }
        fileOrDirectory.delete();
    }

    private void afficherPopupNouveauDossier() {
        Dialog dialog = new Dialog(ctx);
        dialog.setTitle(Traducteur.get("popup_nouveau_dossier"));
        LinearLayout layoutDialog = new LinearLayout(ctx);
        layoutDialog.setOrientation(LinearLayout.VERTICAL);
        styliserDialogue(layoutDialog);
        EditText champTexte = new EditText(ctx);
        champTexte.setHint(Traducteur.get("hint_nom_dossier"));
        styliserChampDialogue(champTexte);
        layoutDialog.addView(champTexte);
        LinearLayout zoneBoutons = new LinearLayout(ctx);
        zoneBoutons.setOrientation(LinearLayout.HORIZONTAL);
        ImageButton btnValider = new ImageButton(ctx);
        btnValider.setImageResource(R.drawable.save_24px);
        styliserBoutonIcone(btnValider);
        btnValider.setOnClickListener(v -> {
            String nom = champTexte.getText().toString().trim();
            if (!nom.isEmpty()) {
                File nouveauDossier = new File(currentFolderSelected, nom);
                if (!nouveauDossier.exists()) nouveauDossier.mkdirs();
                rafraichirArborescenceDossiers();
                rafraichirListeAssets();
            }
            dialog.dismiss();
        });
        ImageButton btnAnnuler = new ImageButton(ctx);
        btnAnnuler.setImageResource(R.drawable.undo_24px);
        styliserBoutonIcone(btnAnnuler);
        btnAnnuler.setOnClickListener(v -> dialog.dismiss());
        zoneBoutons.addView(btnValider);
        zoneBoutons.addView(btnAnnuler);
        layoutDialog.addView(zoneBoutons);
        dialog.setContentView(layoutDialog);
        dialog.show();
    }

    private void afficherPopupRenommerDossier(File dir) {
        Dialog dialog = new Dialog(ctx);
        dialog.setTitle(Traducteur.get("popup_renommer_dossier"));
        LinearLayout layoutDialog = new LinearLayout(ctx);
        layoutDialog.setOrientation(LinearLayout.VERTICAL);
        styliserDialogue(layoutDialog);
        EditText champTexte = new EditText(ctx);
        champTexte.setText(dir.getName());
        styliserChampDialogue(champTexte);
        layoutDialog.addView(champTexte);
        LinearLayout zoneBoutons = new LinearLayout(ctx);
        zoneBoutons.setOrientation(LinearLayout.HORIZONTAL);
        ImageButton btnValider = new ImageButton(ctx);
        btnValider.setImageResource(R.drawable.save_24px);
        styliserBoutonIcone(btnValider);
        btnValider.setOnClickListener(v -> {
            String nouveauNom = champTexte.getText().toString().trim();
            if (!nouveauNom.isEmpty()) {
                File newFile = new File(dir.getParentFile(), nouveauNom);
                if (!newFile.exists()) {
                    dir.renameTo(newFile);
                    currentFolderSelected = newFile;
                    rafraichirArborescenceDossiers();
                    rafraichirListeAssets();
                }
            }
            dialog.dismiss();
        });
        ImageButton btnAnnuler = new ImageButton(ctx);
        btnAnnuler.setImageResource(R.drawable.undo_24px);
        styliserBoutonIcone(btnAnnuler);
        btnAnnuler.setOnClickListener(v -> dialog.dismiss());
        zoneBoutons.addView(btnValider);
        zoneBoutons.addView(btnAnnuler);
        layoutDialog.addView(zoneBoutons);
        dialog.setContentView(layoutDialog);
        dialog.show();
    }

    private void afficherPopupSupprimerDossier(File dir) {
        Dialog dialog = new Dialog(ctx);
        dialog.setTitle(Traducteur.get("popup_confirmer"));
        LinearLayout layoutDialog = new LinearLayout(ctx);
        layoutDialog.setOrientation(LinearLayout.VERTICAL);
        styliserDialogue(layoutDialog);
        TextView txtMessage = new TextView(ctx);
        txtMessage.setText(Traducteur.get("msg_supprimer_dossier"));
        txtMessage.setTextColor(Palette.texteNormal);
        txtMessage.setTextSize(15f);
        txtMessage.setPadding(0, 0, 0, dp(14));
        layoutDialog.addView(txtMessage);
        LinearLayout zoneBoutons = new LinearLayout(ctx);
        zoneBoutons.setOrientation(LinearLayout.HORIZONTAL);
        ImageButton btnOui = new ImageButton(ctx);
        btnOui.setImageResource(R.drawable.save_24px);
        styliserBoutonIcone(btnOui);
        btnOui.setOnClickListener(v -> {
            supprimerRecursif(dir);
            currentFolderSelected = new File(rootAssetsDir, "Images");
            currentAssetSelected = null;
            rafraichirArborescenceDossiers();
            rafraichirListeAssets();
            dialog.dismiss();
        });
        ImageButton btnNon = new ImageButton(ctx);
        btnNon.setImageResource(R.drawable.undo_24px);
        styliserBoutonIcone(btnNon);
        btnNon.setOnClickListener(v -> dialog.dismiss());
        zoneBoutons.addView(btnOui);
        zoneBoutons.addView(btnNon);
        layoutDialog.addView(zoneBoutons);
        dialog.setContentView(layoutDialog);
        dialog.show();
    }

    private void afficherPopupRenommerAsset(File f) {
        Dialog dialog = new Dialog(ctx);
        dialog.setTitle(Traducteur.get("popup_renommer"));
        LinearLayout layoutDialog = new LinearLayout(ctx);
        layoutDialog.setOrientation(LinearLayout.VERTICAL);
        styliserDialogue(layoutDialog);
        EditText champTexte = new EditText(ctx);
        champTexte.setText(f.getName());
        styliserChampDialogue(champTexte);
        layoutDialog.addView(champTexte);
        LinearLayout zoneBoutons = new LinearLayout(ctx);
        zoneBoutons.setOrientation(LinearLayout.HORIZONTAL);
        ImageButton btnValider = new ImageButton(ctx);
        btnValider.setImageResource(R.drawable.save_24px);
        styliserBoutonIcone(btnValider);
        btnValider.setOnClickListener(v -> {
            String nouveauNom = champTexte.getText().toString().trim();
            if (!nouveauNom.isEmpty()) {
                File newFile = new File(f.getParentFile(), nouveauNom);
                if (!newFile.exists()) {
                    f.renameTo(newFile);
                    currentAssetSelected = newFile;
                    rafraichirListeAssets();
                }
            }
            dialog.dismiss();
        });
        ImageButton btnAnnuler = new ImageButton(ctx);
        btnAnnuler.setImageResource(R.drawable.undo_24px);
        styliserBoutonIcone(btnAnnuler);
        btnAnnuler.setOnClickListener(v -> dialog.dismiss());
        zoneBoutons.addView(btnValider);
        zoneBoutons.addView(btnAnnuler);
        layoutDialog.addView(zoneBoutons);
        dialog.setContentView(layoutDialog);
        dialog.show();
    }

    private void afficherPopupSupprimerAsset(File f) {
        Dialog dialog = new Dialog(ctx);
        dialog.setTitle(Traducteur.get("popup_confirmer"));
        LinearLayout layoutDialog = new LinearLayout(ctx);
        layoutDialog.setOrientation(LinearLayout.VERTICAL);
        styliserDialogue(layoutDialog);
        TextView txtMessage = new TextView(ctx);
        txtMessage.setText(Traducteur.get("msg_supprimer_asset"));
        txtMessage.setTextColor(Palette.texteNormal);
        txtMessage.setTextSize(15f);
        txtMessage.setPadding(0, 0, 0, dp(14));
        layoutDialog.addView(txtMessage);
        LinearLayout zoneBoutons = new LinearLayout(ctx);
        zoneBoutons.setOrientation(LinearLayout.HORIZONTAL);
        ImageButton btnOui = new ImageButton(ctx);
        btnOui.setImageResource(R.drawable.save_24px);
        styliserBoutonIcone(btnOui);
        btnOui.setOnClickListener(v -> {
            if (f.equals(fichierSonEnLecture)) arreterSon();
            f.delete();
            currentAssetSelected = null;
            rafraichirListeAssets();
            dialog.dismiss();
        });
        ImageButton btnNon = new ImageButton(ctx);
        btnNon.setImageResource(R.drawable.undo_24px);
        styliserBoutonIcone(btnNon);
        btnNon.setOnClickListener(v -> dialog.dismiss());
        zoneBoutons.addView(btnOui);
        zoneBoutons.addView(btnNon);
        layoutDialog.addView(zoneBoutons);
        dialog.setContentView(layoutDialog);
        dialog.show();
    }
// bas 5

// haut 6
    private void afficherEditeurTexteGeant() {
        Dialog dialog = new Dialog(ctx, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        LinearLayout layoutDialog = new LinearLayout(ctx);
        layoutDialog.setOrientation(LinearLayout.VERTICAL);
        layoutDialog.setBackgroundColor(Palette.fondPanneaux);
        layoutDialog.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView titre = new TextView(ctx);
        titre.setText(Traducteur.get("titre_editeur_script"));
        titre.setTextColor(Palette.texteSelectionne);
        titre.setTextSize(18f);
        titre.setTypeface(null, android.graphics.Typeface.BOLD);
        titre.setPadding(0, 0, 0, dp(10));
        layoutDialog.addView(titre);

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scroll.setLayoutParams(scrollParams);
        scroll.setFillViewport(true);

        EditText champTexte = new EditText(ctx);
        champTexte.setBackground(fond(Palette.fondNormal, Palette.bordure, 8));
        champTexte.setTextColor(Palette.texteNormal);
        champTexte.setGravity(Gravity.TOP | Gravity.START);
        champTexte.setPadding(dp(12), dp(12), dp(12), dp(12));
        champTexte.setTextSize(14f);
        champTexte.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        File dirTextes = new File(rootAssetsDir, "Textes");
        if (!dirTextes.exists()) dirTextes.mkdirs();
        File fichierDialogues = new File(dirTextes, "dialogues.txt");

        String texteInitial = Traducteur.get("texte_aide_dialogues");

        if (fichierDialogues.exists()) {
            try {
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(fichierDialogues));
                StringBuilder sb = new StringBuilder();
                String ligne;
                while ((ligne = br.readLine()) != null) {
                    sb.append(ligne).append("\n");
                }
                br.close();
                if (sb.length() > 0) {
                    texteInitial = sb.toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        champTexte.setText(texteInitial);
        scroll.addView(champTexte);
        layoutDialog.addView(scroll);

        LinearLayout zoneBoutons = new LinearLayout(ctx);
        zoneBoutons.setOrientation(LinearLayout.HORIZONTAL);
        zoneBoutons.setGravity(Gravity.END);
        zoneBoutons.setPadding(0, dp(10), 0, 0);

        Button btnAnnuler = new Button(ctx);
        btnAnnuler.setText(Traducteur.get("bouton_annuler"));
        btnAnnuler.setTextColor(Palette.texteNormal);
        btnAnnuler.setBackground(fond(Palette.boutonNormal, Palette.bordure, 8));
        btnAnnuler.setPadding(dp(16), dp(10), dp(16), dp(10));
        btnAnnuler.setOnClickListener(v -> dialog.dismiss());

        Button btnSauvegarder = new Button(ctx);
        btnSauvegarder.setText(Traducteur.get("bouton_sauvegarder"));
        btnSauvegarder.setTextColor(Color.WHITE);
        btnSauvegarder.setBackground(fond(Color.parseColor("#4CAF50"), Palette.bordure, 8));
        btnSauvegarder.setPadding(dp(16), dp(10), dp(16), dp(10));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(dp(10), 0, 0, 0);
        btnSauvegarder.setLayoutParams(btnParams);

        btnSauvegarder.setOnClickListener(v -> {
            try {
                java.io.FileWriter fw = new java.io.FileWriter(fichierDialogues);
                fw.write(champTexte.getText().toString());
                fw.close();
                Toast.makeText(ctx, Traducteur.get("toast_script_sauvegarde"), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } catch (Exception e) {
                Toast.makeText(ctx, Traducteur.get("erreur_sauvegarde"), Toast.LENGTH_LONG).show();
            }
        });

        zoneBoutons.addView(btnAnnuler);
        zoneBoutons.addView(btnSauvegarder);
        layoutDialog.addView(zoneBoutons);

        dialog.setContentView(layoutDialog);
        dialog.show();
    }
}
// bas 6

  
  


  

  

