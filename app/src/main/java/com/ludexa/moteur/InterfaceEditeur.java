// haut 1
package com.ludexa.moteur;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Type;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class InterfaceEditeur extends Activity implements FournisseurDonneesJeu {

    public static final List<Handler> handlersActifs = new ArrayList<>();

    public String cheminProjet; 

    public List<Scene> listeScenes = new ArrayList<>();
    public List<Variable> variablesGlobales = new ArrayList<>(); 
    public Scene sceneActive;
    
    public Scene sceneHudActive = null;
    
    private VueJeu vueJeu;

    public VueJeu getVueJeu() {
        return this.vueJeu;
    }
    
    private List<Scene> listeScenesBackup;
    private Scene sceneActiveBackup;
    private Scene sceneHudActiveBackup;
    private List<Variable> variablesGlobalesBackup;

    private CanvasEditeur canvasEditeur;
    private PanneauRessources panneauRessources;
    private InspecteurProprietes menuInspecteur;
    
    public Stack<Commande> undoStack = new Stack<>();
    public Stack<Commande> redoStack = new Stack<>();

    private LinearLayout layoutPrincipal;
    private boolean enModeJeu = false;
    private TextView texteNomSceneBandeau;

    public static final int REQUEST_CODE_IMPORT_ASSET = 1001;

    private int dp(float valeur) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, valeur, getResources().getDisplayMetrics()));
    }

    private GradientDrawable fond(int couleur, int rayonDp, int couleurBordure, int epaisseurDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(couleur);
        g.setCornerRadius(dp(rayonDp));
        if (epaisseurDp > 0) {
            g.setStroke(dp(epaisseurDp), couleurBordure);
        }
        return g;
    }

    private void styliserBoutonBandeau(ImageButton b) {
        b.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        b.setBackground(fond(Palette.boutonNormal, 6, Palette.bordure, 1));
        b.setPadding(dp(6), dp(6), dp(6), dp(6));
        Palette.appliquerCouleurIcone(b, Palette.iconeNormal);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(38), dp(38));
        lp.setMargins(0, 0, dp(6), 0);
        lp.gravity = Gravity.CENTER_VERTICAL;
        b.setLayoutParams(lp);
    }

    private View separateurVertical() {
        View s = new View(this);
        s.setBackgroundColor(Palette.bordure);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(1), dp(26));
        lp.setMargins(dp(4), 0, dp(10), 0);
        lp.gravity = Gravity.CENTER_VERTICAL;
        s.setLayoutParams(lp);
        return s;
    }
    
    public void ouvrirHUD(Scene scene) {
        this.sceneHudActive = scene;
        Blueprint blueprintHud = null;
        if (scene != null && cheminProjet != null) {
            try {
                File dossierLogique = new File(cheminProjet, "logique");
                File fileBlueprintHud = new File(dossierLogique, scene.id + ".json");
                if (fileBlueprintHud.exists()) {
                    BufferedReader brHud = new BufferedReader(new FileReader(fileBlueprintHud));
                    StringBuilder sbHud = new StringBuilder();
                    String ligneHud;
                    while ((ligneHud = brHud.readLine()) != null) {
                        sbHud.append(ligneHud);
                    }
                    brHud.close();
                    blueprintHud = Blueprint.fromJson(sbHud.toString(), scene);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (vueJeu != null) {
            vueJeu.ouvrirHudDynamique(scene, blueprintHud);
        }
        Toast.makeText(this, Traducteur.get("hud_ouvert") + (scene != null ? scene.nom : Traducteur.get("valeur_aucune")), Toast.LENGTH_SHORT).show();
    }

    public void fermerHUD() {
        this.sceneHudActive = null;
        if (vueJeu != null) {
            vueJeu.setSceneHud(null);
        }
        Toast.makeText(this, Traducteur.get("hud_ferme"), Toast.LENGTH_SHORT).show();
    }

    public void ajouterCommande(Commande c) {
        undoStack.push(c);
        redoStack.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NoeudBase.contexteApplication = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NoeudBase.contexteApplication = this;

        cheminProjet = getIntent().getStringExtra("cheminProjet");

        layoutPrincipal = new LinearLayout(this);
        layoutPrincipal.setOrientation(LinearLayout.VERTICAL);
        layoutPrincipal.setBackgroundColor(Palette.fondNormal);
        layoutPrincipal.setPadding(dp(8), dp(8), dp(8), dp(8));

        LinearLayout bandeauHaut = new LinearLayout(this);
        bandeauHaut.setOrientation(LinearLayout.HORIZONTAL);
        bandeauHaut.setGravity(Gravity.CENTER_VERTICAL);
        bandeauHaut.setPadding(dp(6), dp(6), dp(6), dp(6));
        bandeauHaut.setBackground(fond(Palette.fondPanneaux, 8, Palette.bordure, 1));

        ImageButton boutonAjouterObjetGauche = new ImageButton(this);
        boutonAjouterObjetGauche.setImageResource(R.drawable.add_24px);
        styliserBoutonBandeau(boutonAjouterObjetGauche);
        boutonAjouterObjetGauche.setOnClickListener(v -> {
            DialogueCreationObjets dialog = new DialogueCreationObjets(InterfaceEditeur.this, InterfaceEditeur.this, canvasEditeur, panneauRessources);
            dialog.show();
        });
        bandeauHaut.addView(boutonAjouterObjetGauche);

        bandeauHaut.addView(separateurVertical());

        LinearLayout blocNomScene = new LinearLayout(this);
        blocNomScene.setOrientation(LinearLayout.HORIZONTAL);
        blocNomScene.setGravity(Gravity.CENTER_VERTICAL);
        blocNomScene.setBackground(fond(Palette.boutonNormal, 6, Palette.bordure, 1));
        blocNomScene.setPadding(dp(10), dp(6), dp(8), dp(6));
        LinearLayout.LayoutParams lpBlocNomScene = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(38));
        lpBlocNomScene.setMargins(0, 0, dp(6), 0);
        lpBlocNomScene.gravity = Gravity.CENTER_VERTICAL;
        blocNomScene.setLayoutParams(lpBlocNomScene);

        TextView texteNomSceneActive = new TextView(this);
        texteNomSceneActive.setText(sceneActive != null ? sceneActive.nom : Traducteur.get("valeur_aucune"));
        texteNomSceneActive.setTextSize(14f);
        texteNomSceneActive.setTextColor(Palette.texteSelectionne);
        texteNomSceneActive.setPadding(0, 0, dp(4), 0);
        this.texteNomSceneBandeau = texteNomSceneActive;
        blocNomScene.addView(texteNomSceneActive);

        ImageView chevronNomScene = new ImageView(this);
        chevronNomScene.setImageResource(R.drawable.unfold_more_24px);
        Palette.appliquerCouleurIcone(chevronNomScene, Palette.iconeNormal);
        chevronNomScene.setLayoutParams(new LinearLayout.LayoutParams(dp(18), dp(18)));
        blocNomScene.addView(chevronNomScene);

        blocNomScene.setOnClickListener(v -> afficherMenuScene(v));
        blocNomScene.setOnLongClickListener(v -> {
            if (sceneActive != null) afficherPopupRenommerScene(sceneActive);
            return true;
        });

        bandeauHaut.addView(blocNomScene);

        bandeauHaut.addView(separateurVertical());

        ImageButton boutonSauvegarde = new ImageButton(this);
        boutonSauvegarde.setImageResource(R.drawable.save_24px);
        styliserBoutonBandeau(boutonSauvegarde);
        boutonSauvegarde.setOnClickListener(v -> sauvegarderProjet());
        bandeauHaut.addView(boutonSauvegarde);

        ImageButton boutonUndo = new ImageButton(this);
        boutonUndo.setImageResource(R.drawable.undo_24px);
        styliserBoutonBandeau(boutonUndo);
        boutonUndo.setOnClickListener(v -> {
            if (!undoStack.isEmpty()) {
                Commande c = undoStack.pop();
                c.annuler();
                redoStack.push(c);
                canvasEditeur.invalidate();
                if (menuInspecteur != null) {
                    menuInspecteur.setSceneActive(sceneActive); 
                    menuInspecteur.afficherObjet(canvasEditeur.getObjetSelectionne());
                }
            }
        });
        bandeauHaut.addView(boutonUndo);

        ImageButton boutonRedo = new ImageButton(this);
        boutonRedo.setImageResource(R.drawable.redo_24px);
        styliserBoutonBandeau(boutonRedo);
        boutonRedo.setOnClickListener(v -> {
            if (!redoStack.isEmpty()) {
                Commande c = redoStack.pop();
                c.executer();
                undoStack.push(c);
                canvasEditeur.invalidate();
                if (menuInspecteur != null) {
                    menuInspecteur.setSceneActive(sceneActive); 
                    menuInspecteur.afficherObjet(canvasEditeur.getObjetSelectionne());
                }
            }
        });
        bandeauHaut.addView(boutonRedo);

        bandeauHaut.addView(separateurVertical());
// bas 1
// haut 2
        listeScenes = new ArrayList<>();
        if (cheminProjet != null) {
            try {
                File fileProjet = new File(cheminProjet, "projet_sauvegarde.json");
                if (fileProjet.exists()) {
                    BufferedReader br = new BufferedReader(new FileReader(fileProjet));
                    Type listType = new TypeToken<ArrayList<Scene>>(){}.getType();
                    List<Scene> scenesChargees = new Gson().fromJson(br, listType);
                    br.close();
                    if (scenesChargees != null && !scenesChargees.isEmpty()) {
                        listeScenes.addAll(scenesChargees);
                        
                        for (Scene s : scenesChargees) {
                            if (s.variablesLocales != null) {
                                for (Variable v : s.variablesLocales) {
                                    v.corrigerTypeValeur();
                                }
                            }
                        }
                        
                        for (Scene scene : listeScenes) {
                            int zOrderMaxScene = -1;
                            if (scene.objets != null) {
                                for (ObjetBase obj : scene.objets) {
                                    if (obj.zOrder > zOrderMaxScene) {
                                        zOrderMaxScene = obj.zOrder;
                                    }
                                }
                            }
                            scene.resynchroniserCompteurZOrder(zOrderMaxScene + 1);
                        }
                        
                        sceneActive = listeScenes.get(0);
                        
                        boolean sceneModifiee = false;
                        for (Scene scene : listeScenes) {
                            if (scene.id == null) {
                                scene.id = java.util.UUID.randomUUID().toString();
                                sceneModifiee = true;
                            }
                        }
                        
                        if (sceneModifiee) {
                            try {
                                Gson gson = new Gson();
                                String jsonProjet = gson.toJson(listeScenes);
                                FileWriter writerProjet = new FileWriter(fileProjet);
                                writerProjet.write(jsonProjet);
                                writerProjet.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                
                try {
                    File fileVariablesGlobales = new File(cheminProjet, "variables_globales.json");
                    if (fileVariablesGlobales.exists()) {
                        BufferedReader brVar = new BufferedReader(new FileReader(fileVariablesGlobales));
                        Type listTypeVar = new TypeToken<ArrayList<Variable>>(){}.getType();
                        List<Variable> variablesChargees = new Gson().fromJson(brVar, listTypeVar);
                        brVar.close();
                        if (variablesChargees != null) {
                            variablesGlobales = new ArrayList<>(variablesChargees);
                            
                            for (Variable v : variablesGlobales) {
                                v.corrigerTypeValeur();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        if (listeScenes.isEmpty()) {
            sceneActive = new Scene("SceneDepart");
            listeScenes.add(sceneActive);
        }

        canvasEditeur = new CanvasEditeur(this);
        canvasEditeur.setCheminProjet(cheminProjet); 
        canvasEditeur.setScene(sceneActive);
        canvasEditeur.setEditeur(this);

        if (listeScenes != null) {
            for (Scene s : listeScenes) {
                if (s.objets != null) {
                    for (ObjetBase obj : s.objets) {
                        if ("scene_instance".equals(obj.type) && obj.sceneLieeId != null) {
                            Scene sceneLiee = null;
                            for (Scene searchScene : listeScenes) {
                                if (searchScene.id != null && searchScene.id.equals(obj.sceneLieeId)) {
                                    sceneLiee = searchScene;
                                    break;
                                }
                            }
                            
                            if (sceneLiee != null) {
                                android.graphics.RectF limites = canvasEditeur.calculerLimitesScene(sceneLiee);
                                obj.largeur = Math.max(50f, limites.right);
                                obj.hauteur = Math.max(50f, limites.bottom);
                            }
                        }
                    }
                }
            }
        }

        LinearLayout.LayoutParams paramsCentre = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        paramsCentre.setMargins(dp(8), 0, dp(8), 0);
        canvasEditeur.setLayoutParams(paramsCentre);

        ImageButton boutonZoomMoins = new ImageButton(this);
        boutonZoomMoins.setImageResource(R.drawable.zoom_out_24px);
        styliserBoutonBandeau(boutonZoomMoins);
        boutonZoomMoins.setOnClickListener(v -> canvasEditeur.zoomMoins());
        bandeauHaut.addView(boutonZoomMoins);

        ImageButton boutonZoomReset = new ImageButton(this);
        boutonZoomReset.setImageResource(R.drawable.center_focus_weak_24px);
        styliserBoutonBandeau(boutonZoomReset);
        boutonZoomReset.setOnClickListener(v -> canvasEditeur.zoomReset());
        bandeauHaut.addView(boutonZoomReset);

        ImageButton boutonZoomPlus = new ImageButton(this);
        boutonZoomPlus.setImageResource(R.drawable.zoom_in_24px);
        styliserBoutonBandeau(boutonZoomPlus);
        boutonZoomPlus.setOnClickListener(v -> canvasEditeur.zoomPlus());
        bandeauHaut.addView(boutonZoomPlus);

        ImageButton boutonDeplacerScene = new ImageButton(this);
        boutonDeplacerScene.setImageResource(R.drawable.hand_gesture_24px);
        styliserBoutonBandeau(boutonDeplacerScene);
        boutonDeplacerScene.setOnClickListener(v -> {
            boolean nouveauMode = !canvasEditeur.isPanMode();
            canvasEditeur.setPanMode(nouveauMode);
            boutonDeplacerScene.setBackground(fond(
                    nouveauMode ? Palette.boutonSurvol : Palette.boutonNormal, 6, Palette.bordure, 1));
            Palette.appliquerCouleurIcone(boutonDeplacerScene,
                    nouveauMode ? Palette.iconeSurvol : Palette.iconeNormal);
        });
        bandeauHaut.addView(boutonDeplacerScene);
        
        bandeauHaut.addView(separateurVertical());
        
        ImageButton boutonDeplacerObjet = new ImageButton(this);
        boutonDeplacerObjet.setImageResource(R.drawable.open_with_24px);
        styliserBoutonBandeau(boutonDeplacerObjet);
        boutonDeplacerObjet.setOnClickListener(v -> {
            boolean nouveauMode = !canvasEditeur.isModeDeplacementObjet();
            canvasEditeur.setModeDeplacementObjet(nouveauMode);
            boutonDeplacerObjet.setBackground(fond(
                    nouveauMode ? Palette.boutonSurvol : Palette.boutonNormal, 6, Palette.bordure, 1));
            Palette.appliquerCouleurIcone(boutonDeplacerObjet,
                    nouveauMode ? Palette.iconeSurvol : Palette.iconeNormal);
        });
        bandeauHaut.addView(boutonDeplacerObjet);
        
        ImageButton boutonCopierObjet = new ImageButton(this);
        boutonCopierObjet.setImageResource(R.drawable.content_copy_24px);
        styliserBoutonBandeau(boutonCopierObjet);
        boutonCopierObjet.setOnClickListener(v -> {
            ObjetBase objSel = canvasEditeur.getObjetSelectionne();
            if (objSel != null) {
                ObjetBase copie = objSel.clonerProfond();
                copie.id = java.util.UUID.randomUUID().toString();
                copie.nom = (copie.nom != null ? copie.nom : Traducteur.get("objet_nom_defaut")) + " " + Traducteur.get("projet_copie");
                copie.x += 20;
                copie.y += 20;
                
                copie.zOrder = sceneActive.prochainZOrder(); 
                
                sceneActive.objets.add(copie);
                canvasEditeur.setObjetSelectionne(copie);
                rafraichirArborescence(copie);
                if (menuInspecteur != null) {
                    menuInspecteur.afficherObjet(copie);
                }
                canvasEditeur.invalidate();
                Toast.makeText(this, Traducteur.get("toast_objet_copie"), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, Traducteur.get("toast_select_objet_copie"), Toast.LENGTH_SHORT).show();
            }
        });
        bandeauHaut.addView(boutonCopierObjet);

        bandeauHaut.addView(separateurVertical());

        ImageButton boutonBasculeBlueprint = new ImageButton(this);
        boutonBasculeBlueprint.setImageResource(R.drawable.account_tree_24px);
        styliserBoutonBandeau(boutonBasculeBlueprint);
        boutonBasculeBlueprint.setOnClickListener(v -> {
            InterfaceBlueprint.sceneACharger = this.sceneActive;
            InterfaceBlueprint.variablesGlobalesACharger = this.variablesGlobales; 
            InterfaceBlueprint.listeScenesACharger = this.listeScenes; 
            
            Intent intent = new Intent(InterfaceEditeur.this, InterfaceBlueprint.class);
            intent.putExtra("cheminProjet", cheminProjet); 
            startActivity(intent);
        });
        bandeauHaut.addView(boutonBasculeBlueprint);

        View espaceBandeau = new View(this);
        espaceBandeau.setLayoutParams(new LinearLayout.LayoutParams(0, dp(1), 1f));
        bandeauHaut.addView(espaceBandeau);

        ImageButton boutonQuitter = new ImageButton(this);
        boutonQuitter.setImageResource(R.drawable.exit_to_app_24px);
        styliserBoutonBandeau(boutonQuitter);
        boutonQuitter.setOnClickListener(v -> finish());
        bandeauHaut.addView(boutonQuitter);

        bandeauHaut.addView(separateurVertical());

        ImageButton boutonBuild = new ImageButton(this);
        boutonBuild.setImageResource(R.drawable.build_24px);
        styliserBoutonBandeau(boutonBuild);
        bandeauHaut.addView(boutonBuild);

        ImageButton boutonPlay = new ImageButton(this);
        boutonPlay.setImageResource(R.drawable.play_circle_24px);
        styliserBoutonBandeau(boutonPlay);
        boutonPlay.setBackground(fond(Palette.boutonSurvol, 6, Palette.bordure, 1));
        boutonPlay.setOnClickListener(v -> basculerVersJeu());
        bandeauHaut.addView(boutonPlay);

        LinearLayout zoneMilieu = new LinearLayout(this);
        zoneMilieu.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams paramsMilieu = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        paramsMilieu.setMargins(0, dp(8), 0, 0);
        zoneMilieu.setLayoutParams(paramsMilieu);

        panneauRessources = new PanneauRessources(this, canvasEditeur, cheminProjet);
        menuInspecteur = new InspecteurProprietes(this, sceneActive, canvasEditeur);
        menuInspecteur.setCheminProjet(cheminProjet); 
        canvasEditeur.setInspecteur(menuInspecteur);
        
        zoneMilieu.addView(panneauRessources);
        zoneMilieu.addView(canvasEditeur);
        zoneMilieu.addView(menuInspecteur);

        layoutPrincipal.addView(bandeauHaut);
        layoutPrincipal.addView(zoneMilieu);

        setContentView(layoutPrincipal);
    }

    public void lancerImportAsset(String mimeType) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        startActivityForResult(intent, REQUEST_CODE_IMPORT_ASSET);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_IMPORT_ASSET && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                if (panneauRessources != null) {
                    panneauRessources.traiterImportAsset(data.getData());
                }
            }
        }
    }
// bas 2
// haut 3
    // ici
    private void afficherMenuScene(View ancre) {
        PopupMenu popup = new PopupMenu(this, ancre);
        popup.getMenu().add(0, 1, 0, Traducteur.get("popup_renommer_scene_titre"));
        popup.getMenu().add(0, 2, 1, Traducteur.get("popup_creer_scene_titre"));
        popup.getMenu().add(0, 3, 2, Traducteur.get("popup_supprimer_scene_titre"));
        if (listeScenes != null && listeScenes.size() > 1) {
            for (int i = 0; i < listeScenes.size(); i++) {
                Scene s = listeScenes.get(i);
                if (s != sceneActive) {
                    popup.getMenu().add(1, 100 + i, i + 3, s.nom);
                }
            }
        }
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                if (sceneActive != null) afficherPopupRenommerScene(sceneActive);
                return true;
            } else if (id == 2) {
                afficherPopupCreerScene();
                return true;
            } else if (id == 3) {
                if (sceneActive != null) afficherPopupSupprimerScene(sceneActive);
                return true;
            } else if (id >= 100) {
                int index = id - 100;
                if (index >= 0 && index < listeScenes.size()) {
                    changerScene(listeScenes.get(index));
                }
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void afficherPopupSupprimerScene(Scene scene) {
        if (listeScenes.size() <= 1) {
            Toast.makeText(this, Traducteur.get("erreur_supprimer_seule_scene"), Toast.LENGTH_SHORT).show();
            return;
        }
        new android.app.AlertDialog.Builder(this)
                .setTitle(Traducteur.get("popup_supprimer_scene_titre"))
                .setMessage(Traducteur.get("msg_supprimer_scene_1") + scene.nom + Traducteur.get("msg_supprimer_scene_2"))
                .setPositiveButton(Traducteur.get("bouton_supprimer"), (d, w) -> {
                    listeScenes.remove(scene);
                    if (scene == sceneActive) {
                        changerScene(listeScenes.get(0));
                    } else {
                        panneauRessources.rafraichirScenes();
                    }
                })
                .setNegativeButton(Traducteur.get("bouton_annuler"), null)
                .show();
    }
 // ici bas 
    private void afficherPopupRenommerScene(Scene scene) {
        LinearLayout layoutDialog = new LinearLayout(this);
        layoutDialog.setOrientation(LinearLayout.VERTICAL);
        layoutDialog.setPadding(dp(16), dp(16), dp(16), dp(16));
        layoutDialog.setBackgroundColor(Palette.fondPanneaux);

        EditText champTexte = new EditText(this);
        champTexte.setText(scene.nom);
        champTexte.setTextColor(Palette.texteNormal);
        champTexte.setBackground(fond(Palette.fondNormal, 8, Palette.bordure, 1));
        champTexte.setPadding(dp(12), dp(10), dp(12), dp(10));
        layoutDialog.addView(champTexte);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle(Traducteur.get("popup_renommer_scene_titre"))
                .setView(layoutDialog)
                .setPositiveButton(Traducteur.get("bouton_valider"), (d, w) -> {
                    String nouveauNom = champTexte.getText().toString().trim();
                    if (nouveauNom.isEmpty()) return;
                    for (Scene s : listeScenes) {
                        if (s != scene && s.nom != null && s.nom.trim().equalsIgnoreCase(nouveauNom)) {
                            Toast.makeText(this, Traducteur.get("erreur_scene_existe"), Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    scene.nom = nouveauNom;
                    if (texteNomSceneBandeau != null && scene == sceneActive) {
                        texteNomSceneBandeau.setText(nouveauNom);
                    }
                    panneauRessources.rafraichirScenes();
                })
                .setNegativeButton(Traducteur.get("bouton_annuler"), null)
                .create();
        dialog.show();
    }

    private void afficherPopupCreerScene() {
        LinearLayout layoutDialog = new LinearLayout(this);
        layoutDialog.setOrientation(LinearLayout.VERTICAL);
        layoutDialog.setPadding(dp(16), dp(16), dp(16), dp(16));
        layoutDialog.setBackgroundColor(Palette.fondPanneaux);

        EditText champTexte = new EditText(this);
        champTexte.setHint(Traducteur.get("hint_entrez_nom"));
        champTexte.setTextColor(Palette.texteNormal);
        champTexte.setBackground(fond(Palette.fondNormal, 8, Palette.bordure, 1));
        champTexte.setPadding(dp(12), dp(10), dp(12), dp(10));
        layoutDialog.addView(champTexte);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle(Traducteur.get("popup_creer_scene_titre"))
                .setView(layoutDialog)
                .setPositiveButton(Traducteur.get("bouton_valider"), (d, w) -> {
                    String nom = champTexte.getText().toString().trim();
                    if (nom.isEmpty()) return;
                    for (Scene s : listeScenes) {
                        if (s.nom != null && s.nom.trim().equalsIgnoreCase(nom)) {
                            Toast.makeText(this, Traducteur.get("erreur_scene_existe"), Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    creerScene(nom);
                })
                .setNegativeButton(Traducteur.get("bouton_annuler"), null)
                .create();
        dialog.show();
    }

    private void basculerVersJeu() {
        listeScenesBackup = new ArrayList<>(listeScenes);
        sceneActiveBackup = sceneActive;
        sceneHudActiveBackup = sceneHudActive;
        variablesGlobalesBackup = new ArrayList<>(variablesGlobales);
        
        listeScenes = new ArrayList<>();
        for (Scene s : listeScenesBackup) {
            Scene clone = s.clonerProfond();
            listeScenes.add(clone);
            
            if (s == sceneActiveBackup) {
                sceneActive = clone;
            }
            if (s == sceneHudActiveBackup) {
                sceneHudActive = clone;
            }
        }
        
        variablesGlobales = new ArrayList<>();
        for (Variable v : variablesGlobalesBackup) {
            variablesGlobales.add(v.clonerProfond());
        }

        Blueprint blueprintActif = new Blueprint();
        
        File dossierLogique = new File(cheminProjet, "logique");
        File fileBlueprint = new File(dossierLogique, sceneActive.id + ".json");

        if (fileBlueprint.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(fileBlueprint));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
                String json = sb.toString();
                blueprintActif = Blueprint.fromJson(json, sceneActive);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, Traducteur.get("erreur_lecture_blueprint"), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, Traducteur.get("erreur_aucun_blueprint_sauvegarde"), Toast.LENGTH_LONG).show();
        }

        Blueprint blueprintHud = null;
        if (sceneHudActive != null) {
            File fileBlueprintHud = new File(dossierLogique, sceneHudActive.id + ".json");
            if (fileBlueprintHud.exists()) {
                try {
                    BufferedReader brHud = new BufferedReader(new FileReader(fileBlueprintHud));
                    StringBuilder sbHud = new StringBuilder();
                    String ligneHud;
                    while ((ligneHud = brHud.readLine()) != null) {
                        sbHud.append(ligneHud);
                    }
                    brHud.close();
                    blueprintHud = Blueprint.fromJson(sbHud.toString(), sceneHudActive);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        this.vueJeu = new VueJeu(this, sceneActive, blueprintActif, cheminProjet, sceneHudActive, blueprintHud);
        
        FrameLayout conteneurJeu = new FrameLayout(this);
        conteneurJeu.addView(this.vueJeu, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        ImageButton boutonStop = new ImageButton(this);
        boutonStop.setImageResource(R.drawable.stop_circle_24px);
        boutonStop.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        boutonStop.setPadding(dp(6), dp(6), dp(6), dp(6));
        boutonStop.setBackground(fond(Color.parseColor("#C0392B"), 6, Palette.bordure, 1));
        Palette.appliquerCouleurIcone(boutonStop, Palette.iconeNormal);
        boutonStop.setOnClickListener(v -> revenirAEditeur());

        FrameLayout.LayoutParams paramsStop = new FrameLayout.LayoutParams(dp(38), dp(38));
        paramsStop.gravity = Gravity.TOP | Gravity.END;
        paramsStop.setMargins(0, dp(12), dp(12), 0); 
        
        conteneurJeu.addView(boutonStop, paramsStop);

        setContentView(conteneurJeu);
        enModeJeu = true;
    }
    
    private void revenirAEditeur() {
        for (Handler handler : handlersActifs) {
            handler.removeCallbacksAndMessages(null);
        }
        handlersActifs.clear();

        if (enModeJeu) {
            setContentView(layoutPrincipal);
            enModeJeu = false;
            
            if (listeScenesBackup != null) {
                listeScenes = listeScenesBackup;
                listeScenesBackup = null;
            }
            if (sceneActiveBackup != null) {
                sceneActive = sceneActiveBackup;
                sceneActiveBackup = null;
            }
            
            sceneHudActive = sceneHudActiveBackup;
            sceneHudActiveBackup = null;

            if (variablesGlobalesBackup != null) {
                variablesGlobales = variablesGlobalesBackup;
                variablesGlobalesBackup = null;
            }
            
            canvasEditeur.setScene(sceneActive);
            
            if (menuInspecteur != null) {
                menuInspecteur.setSceneActive(sceneActive); 
            }
            
            panneauRessources.rafraichirScenes();

            canvasEditeur.invalidate();
            if (menuInspecteur != null) {
                menuInspecteur.afficherObjet(canvasEditeur.getObjetSelectionne());
            }
        }
    }
    
    @Override
    public void onBackPressed() {
        if (enModeJeu) {
            revenirAEditeur();
        } else {
            super.onBackPressed();
        }
    }

    public void creerScene(String nom) {
        Scene nouvelleScene = new Scene(nom);
        listeScenes.add(nouvelleScene);
        changerScene(nouvelleScene);
    }

    public void changerScene(Scene scene) {
        this.sceneActive = scene;
        if (texteNomSceneBandeau != null) {
            texteNomSceneBandeau.setText(scene.nom);
        }
        canvasEditeur.setScene(scene);
        canvasEditeur.deselectionner();
        if (menuInspecteur != null) {
            menuInspecteur.setSceneActive(scene); 
            menuInspecteur.afficherObjet(null);
        }
        panneauRessources.rafraichirScenes();
        panneauRessources.rafraichirVariables(); 
        canvasEditeur.invalidate();
    }

    public void rafraichirArborescence(ObjetBase objet) {
        if (panneauRessources != null) {
            panneauRessources.setObjetSelectionne(objet);
        }
    }

    private void sauvegarderProjet() {
        try {
            Gson gson = new Gson();
            String jsonProjet = gson.toJson(listeScenes);
            
            File fileProjet = new File(cheminProjet, "projet_sauvegarde.json");
            
            FileWriter writerProjet = new FileWriter(fileProjet);
            writerProjet.write(jsonProjet);
            writerProjet.close();

            try {
                File fileVariablesGlobales = new File(cheminProjet, "variables_globales.json");
                String jsonVariables = gson.toJson(variablesGlobales);
                FileWriter writerVariables = new FileWriter(fileVariablesGlobales);
                writerVariables.write(jsonVariables);
                writerVariables.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (canvasEditeur != null) {
                String cheminVignette = new File(cheminProjet, "vignette.png").getAbsolutePath();
                canvasEditeur.sauvegarderVignette(cheminVignette);
            }

            Toast.makeText(this, Traducteur.get("toast_projet_sauvegarde"), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, Traducteur.get("erreur_sauvegarde"), Toast.LENGTH_SHORT).show();
        }
    }

    // --- IMPLEMENTATION FOURNISSEUR DONNEES JEU ---
    @Override
    public List<Scene> getListeScenes() {
        return this.listeScenes;
    }

    @Override
    public List<Variable> getVariablesGlobales() {
        return this.variablesGlobales;
    }

    @Override
    public List<String> getTousLesTags() {
        java.util.Set<String> tagsUniques = new java.util.HashSet<>();
        tagsUniques.add("Joueur"); 
        
        int objetsScannes = 0;
        if (this.listeScenes != null) {
            for (Scene s : this.listeScenes) {
                if (s.objets != null) {
                    for (ObjetBase obj : s.objets) {
                        if (obj.tag != null && !obj.tag.trim().isEmpty()) {
                            tagsUniques.add(obj.tag.trim());
                            objetsScannes++;
                        }
                    }
                }
            }
        }
        
        List<String> listeFinale = new ArrayList<>(tagsUniques);
        java.util.Collections.sort(listeFinale);
        
        DiagLogger.log(cheminProjet, "TAGS EXTRAITS : " + listeFinale.size() + " tags uniques trouves parmi " + objetsScannes + " objets tagues dans l'editeur.");
        return listeFinale;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }
}
// bas 3

        
