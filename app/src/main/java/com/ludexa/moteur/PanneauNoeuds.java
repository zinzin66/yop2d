// haut 1
package com.ludexa.moteur;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.Map;

public class PanneauNoeuds extends ScrollView {

    private LinearLayout conteneurSections;
    private ImageButton boutonMasquer;
    private TextView titrePanneau;
    private boolean estOuvert = true;

    public PanneauNoeuds(Context context) {
        super(context);
        init(context);
    }

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

    private void styliserTitreCategorie(Button btn) {
        btn.setBackgroundColor(Color.TRANSPARENT);
        btn.setTextColor(Palette.texteSelectionne);
        btn.setTextSize(14f);
        btn.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        btn.setPadding(dp(8), dp(10), dp(8), dp(6));
        btn.setAllCaps(false);
    }

    private void init(Context context) {
        setBackgroundColor(Palette.fondPanneaux); 
        setLayoutParams(new LinearLayout.LayoutParams(dp(240), LinearLayout.LayoutParams.MATCH_PARENT));

        LinearLayout layoutPrincipal = new LinearLayout(context);
        layoutPrincipal.setOrientation(LinearLayout.VERTICAL);

        LinearLayout enTete = new LinearLayout(context);
        enTete.setOrientation(LinearLayout.HORIZONTAL);
        enTete.setGravity(Gravity.CENTER_VERTICAL);
        enTete.setBackground(fond(Palette.enTeteDialogues, Palette.bordure, 0));
        enTete.setPadding(dp(8), dp(8), dp(8), dp(8));

        titrePanneau = new TextView(context);
        titrePanneau.setText(Traducteur.get("panneau_noeuds_titre"));
        titrePanneau.setTextColor(Palette.texteSelectionne);
        titrePanneau.setTextSize(14f);
        LinearLayout.LayoutParams paramsTitre = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        titrePanneau.setLayoutParams(paramsTitre);

        boutonMasquer = new ImageButton(context);
        boutonMasquer.setImageResource(R.drawable.chevron_left_24px);
        boutonMasquer.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        boutonMasquer.setColorFilter(Palette.iconeNormal);
        boutonMasquer.setBackground(fond(Palette.boutonNormal, Palette.bordure, 8));
        boutonMasquer.setPadding(dp(8), dp(8), dp(8), dp(8));
        boutonMasquer.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));

        enTete.addView(titrePanneau);
        enTete.addView(boutonMasquer);
        layoutPrincipal.addView(enTete);

        conteneurSections = new LinearLayout(context);
        conteneurSections.setOrientation(LinearLayout.VERTICAL);
        conteneurSections.setPadding(dp(4), dp(4), dp(4), dp(4));

        Map<String, List<RegistreNoeuds.InfoNoeud>> categories = RegistreNoeuds.getNoeudsParCategorie();
        
        for (Map.Entry<String, List<RegistreNoeuds.InfoNoeud>> entry : categories.entrySet()) {
            String nomCat = entry.getKey();
            List<RegistreNoeuds.InfoNoeud> noeuds = entry.getValue();
            
            Button btnCat = new Button(context);
            // CORRECTION : On retire le Traducteur.get ici car nomCat est déjà traduit par le Registre
            btnCat.setText(nomCat + " ▼");
            styliserTitreCategorie(btnCat);
            
            LinearLayout conteneurCat = new LinearLayout(context);
            conteneurCat.setOrientation(LinearLayout.VERTICAL);
            conteneurCat.setPadding(dp(16), dp(8), dp(8), dp(16));
            
            for (RegistreNoeuds.InfoNoeud info : noeuds) {
                TextView item = creerItemNoeud(context, info.libelle, info.classeType);
                conteneurCat.addView(item);
            }
            
            btnCat.setOnClickListener(v -> {
                if (conteneurCat.getVisibility() == View.VISIBLE) {
                    conteneurCat.setVisibility(View.GONE);
                    btnCat.setText(nomCat + " ▶");
                } else {
                    conteneurCat.setVisibility(View.VISIBLE);
                    btnCat.setText(nomCat + " ▼");
                }
            });
            
            conteneurSections.addView(btnCat);
            conteneurSections.addView(conteneurCat);
        }

        layoutPrincipal.addView(conteneurSections);

        boutonMasquer.setOnClickListener(v -> {
            estOuvert = !estOuvert;
            if (estOuvert) {
                conteneurSections.setVisibility(View.VISIBLE);
                titrePanneau.setVisibility(View.VISIBLE);
                boutonMasquer.setImageResource(R.drawable.chevron_left_24px);
                setLayoutParams(new LinearLayout.LayoutParams(dp(240), LinearLayout.LayoutParams.MATCH_PARENT));
            } else {
                conteneurSections.setVisibility(View.GONE);
                titrePanneau.setVisibility(View.GONE);
                boutonMasquer.setImageResource(R.drawable.chevron_right_24px);
                setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
            }
            requestLayout();
        });

        addView(layoutPrincipal);
    }

    private TextView creerItemNoeud(Context context, String libelle, String typeClasse) {
        TextView item = new TextView(context);
        // CORRECTION : On retire le Traducteur.get ici car libelle est déjà traduit par le Registre
        item.setText(libelle);
        item.setTextColor(Palette.texteNormal);
        item.setTextSize(13f);
        item.setPadding(dp(10), dp(10), dp(10), dp(10));
        item.setBackground(fond(Palette.fondNormal, Palette.bordure, 6));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(6));
        item.setLayoutParams(params);

        item.setOnLongClickListener(v -> {
            ClipData data = ClipData.newPlainText("typeNoeud", typeClasse);
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
            v.startDragAndDrop(data, shadowBuilder, v, 0);
            return true;
        });

        item.setOnClickListener(v -> {
            Toast.makeText(context, Traducteur.get("toast_glisser_noeud"), Toast.LENGTH_SHORT).show();
        });

        return item;
    }
}
// bas 1
