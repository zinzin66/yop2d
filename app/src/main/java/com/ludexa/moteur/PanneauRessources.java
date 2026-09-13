// haut 1
package com.ludexa.moteur;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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

public class PanneauRessources extends LinearLayout {

    private LinearLayout conteneurFichiers;
    private LinearLayout conteneurScenes;
    private LinearLayout conteneurArborescence;
    private LinearLayout conteneurArborescenceDossiers;
    private LinearLayout conteneurListeAssets;
    private LinearLayout conteneurVariables;
    private LinearLayout conteneurFonctions; 
    
    private File rootAssetsDir;
    private File rootFonctionsDir; 
    private CanvasEditeur canvasEditeur;
    private ObjetBase objetSelectionne;
    private Variable variableSelectionnee;
    private String fonctionSelectionnee; 
    
    private File currentFolderSelected;
    private File currentAssetSelected;
    private String cheminProjet;

    private ScrollView scrollPanneau;
    private LinearLayout.LayoutParams paramsOuvert;
    private LinearLayout.LayoutParams paramsFerme;
    private Button boutonMasquer;

    public PanneauRessources(Context context, CanvasEditeur canvasEditeur, String cheminProjet) {
        super(context);
        this.canvasEditeur = canvasEditeur;
        this.cheminProjet = cheminProjet;
        setOrientation(LinearLayout.VERTICAL);
        setBackgroundColor(Palette.fondPanneaux);

        if (cheminProjet != null) {
            rootAssetsDir = new File(cheminProjet, "assets_ludexa");
            if (!rootAssetsDir.exists()) rootAssetsDir.mkdirs();
            new File(rootAssetsDir, "Images").mkdirs();
            new File(rootAssetsDir, "Sons").mkdirs();
            new File(rootAssetsDir, "Fonts").mkdirs();
            new File(rootAssetsDir, "Textes").mkdirs();
            
            rootFonctionsDir = new File(cheminProjet, "fonctions"); 
            if (!rootFonctionsDir.exists()) rootFonctionsDir.mkdirs(); 
        }

        paramsOuvert = new LinearLayout.LayoutParams(500, LinearLayout.LayoutParams.MATCH_PARENT);
        paramsFerme = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        this.setLayoutParams(paramsOuvert);

        LinearLayout entetePanneau = new LinearLayout(context);
        entetePanneau.setOrientation(LinearLayout.HORIZONTAL);
        entetePanneau.setPadding(dp(12), dp(10), dp(12), dp(10));
        entetePanneau.setBackgroundColor(Palette.enTeteDialogues);
        entetePanneau.setGravity(Gravity.CENTER_VERTICAL);

        boutonMasquer = new Button(context);
        boutonMasquer.setText("<"); 
        boutonMasquer.setAllCaps(false);
        boutonMasquer.setTextColor(Palette.iconeNormal);
        boutonMasquer.setBackground(fond(Palette.boutonNormal, Palette.bordure, 8));
        boutonMasquer.setPadding(dp(10), dp(6), dp(10), dp(6));
        boutonMasquer.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(40)));

        TextView titrePanneau = new TextView(context);
        titrePanneau.setText(Traducteur.get("panneau_ress_titre"));
        titrePanneau.setTextSize(17f);
        titrePanneau.setLetterSpacing(0.08f);
        titrePanneau.setTypeface(null, android.graphics.Typeface.BOLD);
        titrePanneau.setTextColor(Palette.texteSelectionne);
        titrePanneau.setPadding(dp(10), 0, 0, 0);
        LinearLayout.LayoutParams paramsTitre = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titrePanneau.setLayoutParams(paramsTitre);

        entetePanneau.addView(boutonMasquer);
        entetePanneau.addView(titrePanneau);
        addView(entetePanneau);

        scrollPanneau = new ScrollView(context);
        scrollPanneau.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout contenuScroll = new LinearLayout(context);
        contenuScroll.setOrientation(LinearLayout.VERTICAL);
        contenuScroll.setPadding(dp(8), dp(8), dp(8), dp(8));

        contenuScroll.addView(creerSectionScenes(context));
        // L'appel à creerSectionObjets a été supprimé ici
        contenuScroll.addView(creerSectionArborescence(context));
        contenuScroll.addView(creerSectionAssets(context));
        contenuScroll.addView(creerSectionVariables(context));
        contenuScroll.addView(creerSectionFonctions(context)); 

        scrollPanneau.addView(contenuScroll);
        addView(scrollPanneau);

        boutonMasquer.setOnClickListener(v -> {
            if (scrollPanneau.getVisibility() == View.VISIBLE) {
                scrollPanneau.setVisibility(View.GONE);
                titrePanneau.setVisibility(View.GONE);
                boutonMasquer.setText(">");
                this.setLayoutParams(paramsFerme);
            } else {
                scrollPanneau.setVisibility(View.VISIBLE);
                titrePanneau.setVisibility(View.VISIBLE);
                boutonMasquer.setText("<");
                this.setLayoutParams(paramsOuvert);
            }
        });
    }
// bas 1

// haut 2
    private int dp(int valeur) {
        return (int) (valeur * getResources().getDisplayMetrics().density);
    }

    private android.graphics.drawable.GradientDrawable fond(int couleurFond, int couleurBordure, int rayon) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(couleurFond);
        g.setCornerRadius(dp(rayon));
        g.setStroke(dp(1), couleurBordure);
        return g;
    }

    private void styliserTitreSection(Button btn) {
        btn.setBackgroundColor(Color.TRANSPARENT);
        btn.setTextColor(Palette.texteSelectionne);
        btn.setTextSize(16f);
        btn.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        btn.setPadding(dp(8), dp(16), dp(8), dp(8));
        btn.setAllCaps(false);
    }

    private void styliserContenuSection(LinearLayout contenu) {
        contenu.setBackground(fond(Palette.fondNormal, Palette.bordure, 8));
        contenu.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        contenu.setLayoutParams(lp);
    }

    private void styliserBoutonIcone(ImageButton btn) {
        btn.setBackground(fond(Palette.boutonNormal, Palette.bordure, 8));
        btn.setPadding(dp(12), dp(12), dp(12), dp(12));
        btn.setColorFilter(Palette.iconeNormal);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        btn.setLayoutParams(lp);
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
// bas 2

// haut 3
    private View creerSectionScenes(Context context) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);

        Button btnTitre = new Button(context);
        btnTitre.setText(Traducteur.get("panneau_ress_scenes") + " ▼");
        styliserTitreSection(btnTitre);

        LinearLayout contenu = new LinearLayout(context);
        contenu.setOrientation(LinearLayout.VERTICAL);
        styliserContenuSection(contenu);

        conteneurScenes = new LinearLayout(context);
        conteneurScenes.setOrientation(LinearLayout.VERTICAL);
        conteneurScenes.setPadding(0, 0, 0, dp(8));
        contenu.addView(conteneurScenes);

        LinearLayout zoneBoutons = new LinearLayout(context);
        zoneBoutons.setOrientation(LinearLayout.HORIZONTAL);

        ImageButton btnCreer = new ImageButton(context);
        btnCreer.setImageResource(R.drawable.add_24px);
        styliserBoutonIcone(btnCreer);
        btnCreer.setOnClickListener(v -> afficherPopupCreerScene(context));

        ImageButton btnRenommer = new ImageButton(context);
        btnRenommer.setImageResource(R.drawable.edit_square_24px);
        styliserBoutonIcone(btnRenommer);
        btnRenommer.setOnClickListener(v -> {
            InterfaceEditeur editeur = (InterfaceEditeur) context;
            afficherPopupRenommerScene(context, editeur.sceneActive);
        });

        ImageButton btnSupprimer = new ImageButton(context);
        btnSupprimer.setImageResource(R.drawable.delete_24px);
        styliserBoutonIcone(btnSupprimer);
        btnSupprimer.setOnClickListener(v -> {
            InterfaceEditeur editeur = (InterfaceEditeur) context;
            afficherPopupSupprimerScene(context, editeur.sceneActive);
        });

        zoneBoutons.addView(btnCreer);
        zoneBoutons.addView(btnRenommer);
        zoneBoutons.addView(btnSupprimer);

        contenu.addView(zoneBoutons);
        rafraichirScenes();

        btnTitre.setOnClickListener(v -> {
            if (contenu.getVisibility() == View.VISIBLE) {
                contenu.setVisibility(View.GONE);
                btnTitre.setText(Traducteur.get("panneau_ress_scenes") + " ▶");
            } else {
                contenu.setVisibility(View.VISIBLE);
                btnTitre.setText(Traducteur.get("panneau_ress_scenes") + " ▼");
            }
        });

        section.addView(btnTitre);
        section.addView(contenu);
        return section;
    }

    public void rafraichirScenes() {
        if (conteneurScenes == null) return;
        conteneurScenes.removeAllViews();
        InterfaceEditeur editeur = (InterfaceEditeur) getContext();

        if (editeur.listeScenes != null) {
            for (Scene s : editeur.listeScenes) {
                TextView nomScene = new TextView(getContext());
                nomScene.setText(s.nom);
                if (s == editeur.sceneActive) {
                    nomScene.setTextColor(Palette.texteSelectionne);
                    nomScene.setBackground(fond(Palette.fondListe, Palette.bordure, 8));
                } else {
                    nomScene.setTextColor(Palette.texteNormal);
                }
                nomScene.setPadding(dp(10), dp(10), dp(10), dp(10));
                nomScene.setTextSize(15f);
                
                nomScene.setOnClickListener(v -> {
                    editeur.changerScene(s);
                    rafraichirArborescence();
                });

                conteneurScenes.addView(nomScene);
            }
        }
    }

    private void afficherPopupCreerScene(Context context) {
        Dialog dialog = new Dialog(context);
        dialog.setTitle(Traducteur.get("popup_creer_scene_titre"));
        LinearLayout layoutDialog = new LinearLayout(context);
        layoutDialog.setOrientation(LinearLayout.VERTICAL);
        styliserDialogue(layoutDialog);
        EditText champTexte = new EditText(context);
        champTexte.setHint(Traducteur.get("hint_entrez_nom"));
        styliserChampDialogue(champTexte);
        layoutDialog.addView(champTexte);
        LinearLayout zoneBoutons = new LinearLayout(context);
        zoneBoutons.setOrientation(LinearLayout.HORIZONTAL);
        ImageButton btnValider = new ImageButton(context);
        btnValider.setImageResource(R.drawable.save_24px);
        styliserBoutonIcone(btnValider);
        btnValider.setOnClickListener(v -> {
            String nom = champTexte.getText().toString().trim();
            if(!nom.isEmpty()) {
                InterfaceEditeur editeur = (InterfaceEditeur) context;
                if (editeur.listeScenes != null) {
                    for (Scene s : editeur.listeScenes) {
                        if (s.nom != null && s.nom.trim().equalsIgnoreCase(nom)) {
                            new AlertDialog.Builder(context).setTitle(Traducteur.get("insp_titre_impossible")).setMessage(Traducteur.get("erreur_scene_existe")).setPositiveButton(Traducteur.get("bouton_ok"), null).show();
                            return;
                        }
                    }
                }
                editeur.creerScene(nom);
                Toast.makeText(context, Traducteur.get("toast_scene_creee") + nom, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
        ImageButton btnAnnuler = new ImageButton(context);
        btnAnnuler.setImageResource(R.drawable.undo_24px);
        styliserBoutonIcone(btnAnnuler);
        btnAnnuler.setOnClickListener(v -> dialog.dismiss());
        zoneBoutons.addView(btnValider);
        zoneBoutons.addView(btnAnnuler);
        layoutDialog.addView(zoneBoutons);
        dialog.setContentView(layoutDialog);
        dialog.show();
    }

    private void afficherPopupRenommerScene(Context context, Scene scene) {
        Dialog dialog = new Dialog(context);
        dialog.setTitle(Traducteur.get("popup_renommer_scene_titre"));
        LinearLayout layoutDialog = new LinearLayout(context);
        layoutDialog.setOrientation(LinearLayout.VERTICAL);
        styliserDialogue(layoutDialog);
        EditText champTexte = new EditText(context);
        champTexte.setText(scene.nom);
        styliserChampDialogue(champTexte);
        layoutDialog.addView(champTexte);
        LinearLayout zoneBoutons = new LinearLayout(context);
        zoneBoutons.setOrientation(LinearLayout.HORIZONTAL);
        ImageButton btnValider = new ImageButton(context);
        btnValider.setImageResource(R.drawable.save_24px);
        styliserBoutonIcone(btnValider);
        btnValider.setOnClickListener(v -> {
            String nouveauNom = champTexte.getText().toString().trim();
            if(nouveauNom.isEmpty()) return;
            InterfaceEditeur editeur = (InterfaceEditeur) context;
            if (editeur.listeScenes != null) {
                for (Scene s : editeur.listeScenes) {
                    if (s != scene && s.nom != null && s.nom.trim().equalsIgnoreCase(nouveauNom)) {
                        new AlertDialog.Builder(context).setTitle(Traducteur.get("insp_titre_impossible")).setMessage(Traducteur.get("erreur_scene_existe")).setPositiveButton(Traducteur.get("bouton_ok"), null).show();
                        return;
                    }
                }
            }
            scene.nom = nouveauNom;
            rafraichirScenes();
            dialog.dismiss();
        });
        ImageButton btnAnnuler = new ImageButton(context);
        btnAnnuler.setImageResource(R.drawable.undo_24px);
        styliserBoutonIcone(btnAnnuler);
        btnAnnuler.setOnClickListener(v -> dialog.dismiss());
        zoneBoutons.addView(btnValider);
        zoneBoutons.addView(btnAnnuler);
        layoutDialog.addView(zoneBoutons);
        dialog.setContentView(layoutDialog);
        dialog.show();
    }

    private void afficherPopupSupprimerScene(Context context, Scene scene) {
        Dialog dialog = new Dialog(context);
        dialog.setTitle(Traducteur.get("popup_supprimer_scene_titre"));
        LinearLayout layoutDialog = new LinearLayout(context);
        layoutDialog.setOrientation(LinearLayout.VERTICAL);
        styliserDialogue(layoutDialog);
        TextView txtMessage = new TextView(context);
        txtMessage.setText(Traducteur.get("msg_supprimer_scene_1") + scene.nom + Traducteur.get("msg_supprimer_scene_2"));
        txtMessage.setTextColor(Palette.texteNormal);
        txtMessage.setTextSize(15f);
        txtMessage.setPadding(0, 0, 0, dp(14));
        layoutDialog.addView(txtMessage);
        LinearLayout zoneBoutons = new LinearLayout(context);
        zoneBoutons.setOrientation(LinearLayout.HORIZONTAL);
        ImageButton btnOui = new ImageButton(context);
        btnOui.setImageResource(R.drawable.save_24px);
        styliserBoutonIcone(btnOui);
        btnOui.setOnClickListener(v -> {
            InterfaceEditeur editeur = (InterfaceEditeur) context;
            if (editeur.listeScenes.size() <= 1) {
                Toast.makeText(context, Traducteur.get("erreur_supprimer_seule_scene"), Toast.LENGTH_SHORT).show();
            } else {
                editeur.listeScenes.remove(scene);
                if (editeur.sceneActive == scene) editeur.changerScene(editeur.listeScenes.get(0));
                else rafraichirScenes();
                rafraichirArborescence();
            }
            dialog.dismiss();
        });
        ImageButton btnNon = new ImageButton(context);
        btnNon.setImageResource(R.drawable.undo_24px);
        styliserBoutonIcone(btnNon);
        btnNon.setOnClickListener(v -> dialog.dismiss());
        zoneBoutons.addView(btnOui);
        zoneBoutons.addView(btnNon);
        layoutDialog.addView(zoneBoutons);
        dialog.setContentView(layoutDialog);
        dialog.show();
    }
// bas 3

// haut 4
    private View creerSectionArborescence(Context context) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);

        Button btnTitre = new Button(context);
        btnTitre.setText(Traducteur.get("panneau_ress_arborescence") + " ▼");
        styliserTitreSection(btnTitre);

        LinearLayout contenu = new LinearLayout(context);
        contenu.setOrientation(LinearLayout.VERTICAL);
        styliserContenuSection(contenu);

        conteneurArborescence = new LinearLayout(context);
        conteneurArborescence.setOrientation(LinearLayout.VERTICAL);
        contenu.addView(conteneurArborescence);

        btnTitre.setOnClickListener(v -> {
            if (contenu.getVisibility() == View.VISIBLE) {
                contenu.setVisibility(View.GONE);
                btnTitre.setText(Traducteur.get("panneau_ress_arborescence") + " ▶");
            } else {
                contenu.setVisibility(View.VISIBLE);
                btnTitre.setText(Traducteur.get("panneau_ress_arborescence") + " ▼");
                rafraichirArborescence();
            }
        });

        section.addView(btnTitre);
        section.addView(contenu);
        return section;
    }

    public void setObjetSelectionne(ObjetBase objet) {
        this.objetSelectionne = objet;
        rafraichirArborescence();
    }

    public void rafraichirArborescence() {
        if (conteneurArborescence == null) return;
        conteneurArborescence.removeAllViews();

        InterfaceEditeur editeur = (InterfaceEditeur) getContext();
        if (editeur.sceneActive != null && editeur.sceneActive.objets != null) {
            for (int i = 0; i < editeur.sceneActive.objets.size(); i++) {
                ObjetBase obj = editeur.sceneActive.objets.get(i);

                TextView txtObjet = new TextView(getContext());
                txtObjet.setText("• " + obj.nom);
                txtObjet.setTextColor(obj == objetSelectionne ? Palette.texteSelectionne : Palette.texteNormal);
                txtObjet.setPadding(dp(10), dp(9), dp(10), dp(9));
                txtObjet.setTextSize(14f);
                if (obj == objetSelectionne) {
                    txtObjet.setBackground(fond(Palette.fondListe, Palette.bordure, 8));
                }

                txtObjet.setOnClickListener(v -> {
                    objetSelectionne = obj;
                    canvasEditeur.setObjetSelectionne(obj);
                    rafraichirArborescence();
                });

                conteneurArborescence.addView(txtObjet);
            }
        }

        if (conteneurArborescence.getChildCount() == 0) {
            TextView txtVide = new TextView(getContext());
            txtVide.setText(Traducteur.get("msg_aucun_objet_scene"));
            txtVide.setTextColor(Palette.bordure);
            txtVide.setTextSize(13f);
            txtVide.setPadding(dp(10), dp(10), dp(10), dp(10));
            conteneurArborescence.addView(txtVide);
        }
    }

    private View creerSectionAssets(Context context) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);

        Button btnTitre = new Button(context);
        btnTitre.setText(Traducteur.get("panneau_ress_assets"));
        styliserTitreSection(btnTitre);
        btnTitre.setOnClickListener(v -> {
            DialogueGestionAssets dialog = new DialogueGestionAssets(context, canvasEditeur, cheminProjet);
            dialog.show();
        });

        section.addView(btnTitre);
        return section;
    }
// bas 4

// haut 5
// bas 5

// haut 6
// bas 6

// haut 7
    private void afficherEditeurTexteGeant(Context context) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        
        LinearLayout layoutDialog = new LinearLayout(context);
        layoutDialog.setOrientation(LinearLayout.VERTICAL);
        layoutDialog.setBackgroundColor(Palette.fondPanneaux);
        layoutDialog.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView titre = new TextView(context);
        titre.setText(Traducteur.get("titre_editeur_script"));
        titre.setTextColor(Palette.texteSelectionne);
        titre.setTextSize(18f);
        titre.setTypeface(null, android.graphics.Typeface.BOLD);
        titre.setPadding(0, 0, 0, dp(10));
        layoutDialog.addView(titre);

        ScrollView scroll = new ScrollView(context);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scroll.setLayoutParams(scrollParams);
        scroll.setFillViewport(true);

        EditText champTexte = new EditText(context);
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

        LinearLayout zoneBoutons = new LinearLayout(context);
        zoneBoutons.setOrientation(LinearLayout.HORIZONTAL);
        zoneBoutons.setGravity(Gravity.END);
        zoneBoutons.setPadding(0, dp(10), 0, 0);

        Button btnAnnuler = new Button(context);
        btnAnnuler.setText(Traducteur.get("bouton_annuler"));
        btnAnnuler.setTextColor(Palette.texteNormal);
        btnAnnuler.setBackground(fond(Palette.boutonNormal, Palette.bordure, 8));
        btnAnnuler.setPadding(dp(16), dp(10), dp(16), dp(10));
        btnAnnuler.setOnClickListener(v -> dialog.dismiss());

        Button btnSauvegarder = new Button(context);
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
                Toast.makeText(context, Traducteur.get("toast_script_sauvegarde"), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } catch (Exception e) {
                Toast.makeText(context, Traducteur.get("erreur_sauvegarde"), Toast.LENGTH_LONG).show();
            }
        });

        zoneBoutons.addView(btnAnnuler);
        zoneBoutons.addView(btnSauvegarder);
        layoutDialog.addView(zoneBoutons);

        dialog.setContentView(layoutDialog);
        dialog.show();
    }

    private View creerSectionVariables(Context context) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);

        Button btnTitre = new Button(context);
        btnTitre.setText(Traducteur.get("panneau_ress_variables") + " ▼");
        styliserTitreSection(btnTitre);

        LinearLayout contenu = new LinearLayout(context);
        contenu.setOrientation(LinearLayout.VERTICAL);
        styliserContenuSection(contenu);

        conteneurVariables = new LinearLayout(context);
        conteneurVariables.setOrientation(LinearLayout.VERTICAL);
        conteneurVariables.setPadding(0, 0, 0, dp(8));
        contenu.addView(conteneurVariables);

        LinearLayout zoneBoutons = new LinearLayout(context);
        zoneBoutons.setOrientation(LinearLayout.HORIZONTAL);

        ImageButton btnCreer = new ImageButton(context);
        btnCreer.setImageResource(R.drawable.add_24px);
        styliserBoutonIcone(btnCreer);
        btnCreer.setOnClickListener(v -> afficherPopupCreerVariable(context));

        ImageButton btnRenommer = new ImageButton(context);
        btnRenommer.setImageResource(R.drawable.edit_square_24px);
        styliserBoutonIcone(btnRenommer);
        btnRenommer.setOnClickListener(v -> {
            if (variableSelectionnee != null) afficherPopupRenommerVariable(context, variableSelectionnee);
        });

        ImageButton btnSupprimer = new ImageButton(context);
        btnSupprimer.setImageResource(R.drawable.delete_24px);
        styliserBoutonIcone(btnSupprimer);
        btnSupprimer.setOnClickListener(v -> {
            if (variableSelectionnee != null) afficherPopupSupprimerVariable(context, variableSelectionnee);
        });

        zoneBoutons.addView(btnCreer);
        zoneBoutons.addView(btnRenommer);
        zoneBoutons.addView(btnSupprimer);

        contenu.addView(zoneBoutons);
        rafraichirVariables();

        btnTitre.setOnClickListener(v -> {
            if (contenu.getVisibility() == View.VISIBLE) {
                contenu.setVisibility(View.GONE);
                btnTitre.setText(Traducteur.get("panneau_ress_variables") + " ▶");
            } else {
                contenu.setVisibility(View.VISIBLE);
                btnTitre.setText(Traducteur.get("panneau_ress_variables") + " ▼");
            }
        });

        section.addView(btnTitre);
        section.addView(contenu);
        return section;
    }

    public void rafraichirVariables() {
        if (conteneurVariables == null) return;
        conteneurVariables.removeAllViews();
        InterfaceEditeur editeur = (InterfaceEditeur) getContext();

        if (editeur.variablesGlobales != null) {
            for (Variable var : editeur.variablesGlobales) ajouterVueVariable(var);
        }
        if (editeur.sceneActive != null && editeur.sceneActive.variablesLocales != null) {
            for (Variable var : editeur.sceneActive.variablesLocales) ajouterVueVariable(var);
        }
    }

    private void ajouterVueVariable(Variable var) {
        Context context = getContext();
        LinearLayout conteneurLigne = new LinearLayout(context);
        conteneurLigne.setOrientation(LinearLayout.VERTICAL);
        conteneurLigne.setPadding(0, dp(3), 0, dp(3));
        
        TextView nomVariable = new TextView(context);
        
        String labelType;
        switch (var.type) {
            case "BOOLEEN": labelType = Traducteur.get("var_type_booleen"); break;
            case "CHIFFRE": labelType = Traducteur.get("var_type_chiffre"); break;
            case "ENTIER": labelType = Traducteur.get("var_type_entier"); break;
            case "LISTE_INVENTAIRE": labelType = Traducteur.get("var_type_liste"); break;
            default: labelType = Traducteur.get("var_type_texte"); break;
        }
        
        String labelScope = var.scope.equals("GLOBALE") ? Traducteur.get("var_scope_globale") : Traducteur.get("var_scope_locale");
        
        String texteValeur;
        if (var.type.equals("LISTE_INVENTAIRE") && var.valeur instanceof java.util.List) {
            texteValeur = ((java.util.List<?>) var.valeur).size() + Traducteur.get("var_objets");
        } else {
            texteValeur = String.valueOf(var.valeur);
        }
        
        nomVariable.setText(var.nom + " [" + labelScope + ", " + labelType + "] = " + texteValeur);
        
        if (var == variableSelectionnee) {
            nomVariable.setTextColor(Palette.texteSelectionne);
            nomVariable.setBackground(fond(Palette.fondListe, Palette.bordure, 8));
        } else {
            nomVariable.setTextColor(var.scope.equals("GLOBALE") ? Color.parseColor("#ADD8E6") : Color.parseColor("#90EE90"));
        }
        
        nomVariable.setPadding(dp(10), dp(8), dp(10), dp(8));
        nomVariable.setTextSize(14f);

        nomVariable.setOnClickListener(v -> {
            variableSelectionnee = var;
            rafraichirVariables();
        });

        conteneurLigne.addView(nomVariable);
        conteneurVariables.addView(conteneurLigne);
    }
// bas 7

// haut 8
    private void afficherPopupCreerVariable(Context context) {
        Dialog dialog = new Dialog(context);
        dialog.setTitle(Traducteur.get("popup_creer_var_titre"));
        LinearLayout layoutDialog = new LinearLayout(context);
        layoutDialog.setOrientation(LinearLayout.VERTICAL);
        styliserDialogue(layoutDialog);
        
        EditText champTexte = new EditText(context);
        champTexte.setHint(Traducteur.get("hint_nom_var"));
        styliserChampDialogue(champTexte);
        layoutDialog.addView(champTexte);
        
        TextView txtScope = new TextView(context);
        txtScope.setText(Traducteur.get("label_portee"));
        txtScope.setTextColor(Palette.texteSelectionne);
        txtScope.setPadding(0, dp(12), 0, dp(4));
        layoutDialog.addView(txtScope);
        
        Spinner spinnerScope = new Spinner(context);
        String[] scopeArray = {Traducteur.get("var_scope_locale"), Traducteur.get("var_scope_globale")};
        ArrayAdapter<String> adapterScope = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, scopeArray) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(Palette.texteNormal);
                return tv;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(Palette.texteNormal);
                tv.setBackgroundColor(Palette.fondNormal);
                tv.setPadding(dp(16), dp(16), dp(16), dp(16));
                return tv;
            }
        };
        spinnerScope.setAdapter(adapterScope);
        spinnerScope.setBackground(fond(Palette.fondNormal, Palette.bordure, 8));
        layoutDialog.addView(spinnerScope);
        
        TextView txtType = new TextView(context);
        txtType.setText(Traducteur.get("label_type"));
        txtType.setTextColor(Palette.texteSelectionne);
        txtType.setPadding(0, dp(12), 0, dp(4));
        layoutDialog.addView(txtType);
        
        Spinner spinnerType = new Spinner(context);
        String[] typeArray = {Traducteur.get("var_type_chiffre"), Traducteur.get("var_type_entier"), Traducteur.get("var_type_texte"), Traducteur.get("var_type_booleen"), Traducteur.get("var_type_liste")};
        ArrayAdapter<String> adapterType = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, typeArray) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(Palette.texteNormal);
                return tv;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(Palette.texteNormal);
                tv.setBackgroundColor(Palette.fondNormal);
                tv.setPadding(dp(16), dp(16), dp(16), dp(16));
                return tv;
            }
        };
        spinnerType.setAdapter(adapterType);
        spinnerType.setBackground(fond(Palette.fondNormal, Palette.bordure, 8));
        layoutDialog.addView(spinnerType);
        
        EditText champValeurInit = new EditText(context);
        champValeurInit.setHint(Traducteur.get("hint_valeur_init"));
        styliserChampDialogue(champValeurInit);
        layoutDialog.addView(champValeurInit);
        
        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spinnerType.getSelectedItem().toString().equals(Traducteur.get("var_type_liste"))) champValeurInit.setVisibility(View.GONE);
                else champValeurInit.setVisibility(View.VISIBLE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        LinearLayout zoneBoutons = new LinearLayout(context);
        zoneBoutons.setOrientation(LinearLayout.HORIZONTAL);
        
        ImageButton btnValider = new ImageButton(context);
        btnValider.setImageResource(R.drawable.save_24px);
        styliserBoutonIcone(btnValider);
        btnValider.setOnClickListener(v -> {
            String nom = champTexte.getText().toString().trim();
            if(nom.isEmpty()) return;
            
            String scopeSelect = spinnerScope.getSelectedItem().toString().equals(Traducteur.get("var_scope_globale")) ? "GLOBALE" : "LOCALE";
            String typeSelectText = spinnerType.getSelectedItem().toString();
            String typeSelect = "CHIFFRE";
            
            if (typeSelectText.equals(Traducteur.get("var_type_texte"))) typeSelect = "TEXTE";
            if (typeSelectText.equals(Traducteur.get("var_type_booleen"))) typeSelect = "BOOLEEN";
            if (typeSelectText.equals(Traducteur.get("var_type_entier"))) typeSelect = "ENTIER";
            if (typeSelectText.equals(Traducteur.get("var_type_liste"))) typeSelect = "LISTE_INVENTAIRE";
            
            InterfaceEditeur editeur = (InterfaceEditeur) context;
            
            if (scopeSelect.equals("GLOBALE")) {
                for (Variable vExistant : editeur.variablesGlobales) {
                    if (vExistant.nom.equals(nom)) return;
                }
            } else {
                for (Variable vExistant : editeur.sceneActive.variablesLocales) {
                    if (vExistant.nom.equals(nom)) return;
                }
            }
            
            Variable nouvelleVar = new Variable(nom, scopeSelect, typeSelect);
            String valInitTexte = champValeurInit.getText().toString().trim();
            
            if (!valInitTexte.isEmpty()) {
                if (typeSelect.equals("CHIFFRE")) {
                    try { nouvelleVar.valeur = Float.parseFloat(valInitTexte); } catch (Exception e) { nouvelleVar.valeur = 0f; }
                } else if (typeSelect.equals("TEXTE")) {
                    nouvelleVar.valeur = valInitTexte;
                } else if (typeSelect.equals("BOOLEEN")) {
                    String cleanVal = valInitTexte.toLowerCase();
                    nouvelleVar.valeur = (cleanVal.equals("oui") || cleanVal.equals("vrai") || cleanVal.equals("true"));
                } else if (typeSelect.equals("ENTIER")) {
                    try { nouvelleVar.valeur = Integer.parseInt(valInitTexte); } catch (Exception e) { nouvelleVar.valeur = 0; }
                }
            }
            
            if (scopeSelect.equals("GLOBALE")) editeur.variablesGlobales.add(nouvelleVar);
            else editeur.sceneActive.variablesLocales.add(nouvelleVar);
            
            rafraichirVariables();
            dialog.dismiss();
        });
        
        ImageButton btnAnnuler = new ImageButton(context);
        btnAnnuler.setImageResource(R.drawable.undo_24px);
        styliserBoutonIcone(btnAnnuler);
        btnAnnuler.setOnClickListener(v -> dialog.dismiss());
        
        zoneBoutons.addView(btnValider);
        zoneBoutons.addView(btnAnnuler);
        layoutDialog.addView(zoneBoutons);
        
        dialog.setContentView(layoutDialog);
        dialog.show();
    }

    private void afficherPopupRenommerVariable(Context context, Variable var) {
        Dialog dialog = new Dialog(context);
        dialog.setTitle(Traducteur.get("popup_renommer_var"));
        LinearLayout layoutDialog = new LinearLayout(context);
        layoutDialog.setOrientation(LinearLayout.VERTICAL);
        styliserDialogue(layoutDialog);
        EditText champTexte = new EditText(context);
        champTexte.setText(var.nom);
        styliserChampDialogue(champTexte);
        layoutDialog.addView(champTexte);
        LinearLayout zoneBoutons = new LinearLayout(context);
        zoneBoutons.setOrientation(LinearLayout.HORIZONTAL);
        ImageButton btnValider = new ImageButton(context);
        btnValider.setImageResource(R.drawable.save_24px);
        styliserBoutonIcone(btnValider);
        btnValider.setOnClickListener(v -> {
            String nouveauNom = champTexte.getText().toString().trim();
            if(nouveauNom.isEmpty()) return;
            InterfaceEditeur editeur = (InterfaceEditeur) context;
            if (var.scope.equals("GLOBALE")) {
                for (Variable vExistant : editeur.variablesGlobales) {
                    if (vExistant != var && vExistant.nom.equals(nouveauNom)) return;
                }
            } else {
                for (Variable vExistant : editeur.sceneActive.variablesLocales) {
                    if (vExistant != var && vExistant.nom.equals(nouveauNom)) return;
                }
            }
            var.nom = nouveauNom;
            rafraichirVariables();
            dialog.dismiss();
        });
        ImageButton btnAnnuler = new ImageButton(context);
        btnAnnuler.setImageResource(R.drawable.undo_24px);
        styliserBoutonIcone(btnAnnuler);
        btnAnnuler.setOnClickListener(v -> dialog.dismiss());
        zoneBoutons.addView(btnValider);
        zoneBoutons.addView(btnAnnuler);
        layoutDialog.addView(zoneBoutons);
        dialog.setContentView(layoutDialog);
        dialog.show();
    }

    private void afficherPopupSupprimerVariable(Context context, Variable var) {
        Dialog dialog = new Dialog(context);
        dialog.setTitle(Traducteur.get("popup_supprimer_var"));
        LinearLayout layoutDialog = new LinearLayout(context);
        layoutDialog.setOrientation(LinearLayout.VERTICAL);
        styliserDialogue(layoutDialog);
        TextView txtMessage = new TextView(context);
        txtMessage.setText(Traducteur.get("msg_suppr_var_1") + var.nom + Traducteur.get("msg_suppr_var_2"));
        txtMessage.setTextColor(Palette.texteNormal);
        txtMessage.setTextSize(15f);
        txtMessage.setPadding(0, 0, 0, dp(14));
        layoutDialog.addView(txtMessage);
        LinearLayout zoneBoutons = new LinearLayout(context);
        zoneBoutons.setOrientation(LinearLayout.HORIZONTAL);
        ImageButton btnOui = new ImageButton(context);
        btnOui.setImageResource(R.drawable.save_24px);
        styliserBoutonIcone(btnOui);
        btnOui.setOnClickListener(v -> {
            InterfaceEditeur editeur = (InterfaceEditeur) context;
            if (var.scope.equals("GLOBALE")) editeur.variablesGlobales.remove(var);
            else editeur.sceneActive.variablesLocales.remove(var);
            if (var == variableSelectionnee) variableSelectionnee = null;
            rafraichirVariables();
            dialog.dismiss();
        });
        ImageButton btnNon = new ImageButton(context);
        btnNon.setImageResource(R.drawable.undo_24px);
        styliserBoutonIcone(btnNon);
        btnNon.setOnClickListener(v -> dialog.dismiss());
        zoneBoutons.addView(btnOui);
        zoneBoutons.addView(btnNon);
        layoutDialog.addView(zoneBoutons);
        dialog.setContentView(layoutDialog);
        dialog.show();
    }
// bas 8
// haut 9
    private View creerSectionFonctions(Context context) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);

        Button btnTitre = new Button(context);
        btnTitre.setText(Traducteur.get("panneau_ress_fonctions") + " ▼");
        styliserTitreSection(btnTitre);

        LinearLayout contenu = new LinearLayout(context);
        contenu.setOrientation(LinearLayout.VERTICAL);
        styliserContenuSection(contenu);

        conteneurFonctions = new LinearLayout(context);
        conteneurFonctions.setOrientation(LinearLayout.VERTICAL);
        conteneurFonctions.setPadding(0, 0, 0, dp(8));
        contenu.addView(conteneurFonctions);

        LinearLayout zoneBoutons = new LinearLayout(context);
        zoneBoutons.setOrientation(LinearLayout.HORIZONTAL);

        ImageButton btnCreer = new ImageButton(context);
        btnCreer.setImageResource(R.drawable.add_24px);
        styliserBoutonIcone(btnCreer);
        btnCreer.setOnClickListener(v -> afficherPopupCreerFonction(context));

        ImageButton btnEditer = new ImageButton(context);
        btnEditer.setImageResource(R.drawable.account_tree_24px); 
        styliserBoutonIcone(btnEditer);
        btnEditer.setOnClickListener(v -> {
            if (fonctionSelectionnee != null) {
                android.content.Intent intent = new android.content.Intent(context, InterfaceBlueprint.class);
                intent.putExtra("cheminProjet", cheminProjet);
                intent.putExtra("modeFonction", true);
                intent.putExtra("nomFonction", fonctionSelectionnee);
                InterfaceBlueprint.sceneACharger = null; 
                context.startActivity(intent);
            }
        });

        ImageButton btnRenommer = new ImageButton(context);
        btnRenommer.setImageResource(R.drawable.edit_square_24px);
        styliserBoutonIcone(btnRenommer);
        btnRenommer.setOnClickListener(v -> {
            if (fonctionSelectionnee != null) afficherPopupRenommerFonction(context, fonctionSelectionnee);
        });

        ImageButton btnSupprimer = new ImageButton(context);
        btnSupprimer.setImageResource(R.drawable.delete_24px);
        styliserBoutonIcone(btnSupprimer);
        btnSupprimer.setOnClickListener(v -> {
            if (fonctionSelectionnee != null) afficherPopupSupprimerFonction(context, fonctionSelectionnee);
        });

        zoneBoutons.addView(btnCreer);
        zoneBoutons.addView(btnEditer);
        zoneBoutons.addView(btnRenommer);
        zoneBoutons.addView(btnSupprimer);

        contenu.addView(zoneBoutons);
        rafraichirFonctions();

        btnTitre.setOnClickListener(v -> {
            if (contenu.getVisibility() == View.VISIBLE) {
                contenu.setVisibility(View.GONE);
                btnTitre.setText(Traducteur.get("panneau_ress_fonctions") + " ▶");
            } else {
                contenu.setVisibility(View.VISIBLE);
                btnTitre.setText(Traducteur.get("panneau_ress_fonctions") + " ▼");
            }
        });

        section.addView(btnTitre);
        section.addView(contenu);
        return section;
    }

    public void rafraichirFonctions() {
        if (conteneurFonctions == null || rootFonctionsDir == null) return;
        conteneurFonctions.removeAllViews();

        File[] fichiers = rootFonctionsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (fichiers != null) {
            java.util.Arrays.sort(fichiers, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
            for (File f : fichiers) {
                String nomFonc = f.getName().replace(".json", "");
                
                TextView nomView = new TextView(getContext());
                nomView.setText("ƒ " + nomFonc);
                if (nomFonc.equals(fonctionSelectionnee)) {
                    nomView.setTextColor(Palette.texteSelectionne);
                    nomView.setBackground(fond(Palette.fondListe, Palette.bordure, 8));
                } else {
                    nomView.setTextColor(Color.parseColor("#E040FB")); 
                }
                nomView.setPadding(dp(10), dp(8), dp(10), dp(8));
                nomView.setTextSize(14f);

                nomView.setOnClickListener(v -> {
                    fonctionSelectionnee = nomFonc;
                    rafraichirFonctions();
                });

                conteneurFonctions.addView(nomView);
            }
        }
    }
// bas 9

// haut 10
    private void afficherPopupCreerFonction(Context context) {
        Dialog dialog = new Dialog(context);
        dialog.setTitle(Traducteur.get("popup_creer_fonction"));
        LinearLayout layoutDialog = new LinearLayout(context);
        layoutDialog.setOrientation(LinearLayout.VERTICAL);
        styliserDialogue(layoutDialog);
        
        EditText champTexte = new EditText(context);
        champTexte.setHint(Traducteur.get("hint_nom_fonction"));
        styliserChampDialogue(champTexte);
        layoutDialog.addView(champTexte);
        
        LinearLayout zoneBoutons = new LinearLayout(context);
        zoneBoutons.setOrientation(LinearLayout.HORIZONTAL);
        
        ImageButton btnValider = new ImageButton(context);
        btnValider.setImageResource(R.drawable.save_24px);
        styliserBoutonIcone(btnValider);
        btnValider.setOnClickListener(v -> {
            String nom = champTexte.getText().toString().trim();
            if(!nom.isEmpty()) {
                File nvFichier = new File(rootFonctionsDir, nom + ".json");
                if (nvFichier.exists()) {
                    Toast.makeText(context, Traducteur.get("erreur_fonction_existe"), Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    nvFichier.createNewFile();
                    FileOutputStream fos = new FileOutputStream(nvFichier);
                    fos.write("{\"noeuds\":[],\"liens\":[]}".getBytes());
                    fos.close();
                    
                    fonctionSelectionnee = nom;
                    rafraichirFonctions();
                    dialog.dismiss();
                } catch(Exception e) {
                    Toast.makeText(context, Traducteur.get("erreur_creation"), Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        ImageButton btnAnnuler = new ImageButton(context);
        btnAnnuler.setImageResource(R.drawable.undo_24px);
        styliserBoutonIcone(btnAnnuler);
        btnAnnuler.setOnClickListener(v -> dialog.dismiss());
        
        zoneBoutons.addView(btnValider);
        zoneBoutons.addView(btnAnnuler);
        layoutDialog.addView(zoneBoutons);
        dialog.setContentView(layoutDialog);
        dialog.show();
    }

    private void afficherPopupRenommerFonction(Context context, String oldNom) {
        Dialog dialog = new Dialog(context);
        dialog.setTitle(Traducteur.get("popup_renommer_fonction"));
        LinearLayout layoutDialog = new LinearLayout(context);
        layoutDialog.setOrientation(LinearLayout.VERTICAL);
        styliserDialogue(layoutDialog);
        
        EditText champTexte = new EditText(context);
        champTexte.setText(oldNom);
        styliserChampDialogue(champTexte);
        layoutDialog.addView(champTexte);
        
        LinearLayout zoneBoutons = new LinearLayout(context);
        zoneBoutons.setOrientation(LinearLayout.HORIZONTAL);
        
        ImageButton btnValider = new ImageButton(context);
        btnValider.setImageResource(R.drawable.save_24px);
        styliserBoutonIcone(btnValider);
        btnValider.setOnClickListener(v -> {
            String nouveauNom = champTexte.getText().toString().trim();
            if(!nouveauNom.isEmpty() && !nouveauNom.equals(oldNom)) {
                File oldFichier = new File(rootFonctionsDir, oldNom + ".json");
                File newFichier = new File(rootFonctionsDir, nouveauNom + ".json");
                if (newFichier.exists()) {
                    Toast.makeText(context, Traducteur.get("erreur_nom_pris"), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (oldFichier.renameTo(newFichier)) {
                    fonctionSelectionnee = nouveauNom;
                    rafraichirFonctions();
                    dialog.dismiss();
                }
            }
        });
        
        ImageButton btnAnnuler = new ImageButton(context);
        btnAnnuler.setImageResource(R.drawable.undo_24px);
        styliserBoutonIcone(btnAnnuler);
        btnAnnuler.setOnClickListener(v -> dialog.dismiss());
        
        zoneBoutons.addView(btnValider);
        zoneBoutons.addView(btnAnnuler);
        layoutDialog.addView(zoneBoutons);
        dialog.setContentView(layoutDialog);
        dialog.show();
    }

    private void afficherPopupSupprimerFonction(Context context, String nomFonc) {
        Dialog dialog = new Dialog(context);
        dialog.setTitle(Traducteur.get("popup_supprimer_fonction"));
        LinearLayout layoutDialog = new LinearLayout(context);
        layoutDialog.setOrientation(LinearLayout.VERTICAL);
        styliserDialogue(layoutDialog);
        
        TextView txtMessage = new TextView(context);
        txtMessage.setText(Traducteur.get("msg_suppr_fonction_1") + nomFonc + Traducteur.get("msg_suppr_fonction_2"));
        txtMessage.setTextColor(Palette.texteNormal);
        txtMessage.setTextSize(15f);
        txtMessage.setPadding(0, 0, 0, dp(14));
        layoutDialog.addView(txtMessage);
        
        LinearLayout zoneBoutons = new LinearLayout(context);
        zoneBoutons.setOrientation(LinearLayout.HORIZONTAL);
        
        ImageButton btnOui = new ImageButton(context);
        btnOui.setImageResource(R.drawable.save_24px);
        styliserBoutonIcone(btnOui);
        btnOui.setOnClickListener(v -> {
            File f = new File(rootFonctionsDir, nomFonc + ".json");
            if (f.exists()) f.delete();
            fonctionSelectionnee = null;
            rafraichirFonctions();
            dialog.dismiss();
        });
        
        ImageButton btnNon = new ImageButton(context);
        btnNon.setImageResource(R.drawable.undo_24px);
        styliserBoutonIcone(btnNon);
        btnNon.setOnClickListener(v -> dialog.dismiss());
        
        zoneBoutons.addView(btnOui);
        zoneBoutons.addView(btnNon);
        layoutDialog.addView(zoneBoutons);
        dialog.setContentView(layoutDialog);
        dialog.show();
    }
}
// bas 10

    
    

