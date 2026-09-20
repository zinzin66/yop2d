// haut 1
package com.ludexa.moteur;

import java.util.LinkedHashMap;
import java.util.Map;

// Textes de la fenêtre d'édition des nœuds, dans les 9 langues.
// Ils servent de secours : si un fichier de langue contient la même clé, c'est lui qui est utilisé.
public class TextesEditeur {

    private static boolean enregistre = false;

    public static synchronized void enregistrer() {
        if (enregistre) return;
        enregistre = true;

        ajouter("editeur_section_fonctions",
                "Fonctions", "Functions", "Funciones", "Funções", "Funktionen", "Funzioni", "Функции", "函数", "関数");
        ajouter("editeur_bouton_formule",
                "Passer en formule (=)", "Switch to formula (=)", "Pasar a fórmula (=)", "Mudar para fórmula (=)",
                "Zur Formel wechseln (=)", "Passa a formula (=)", "Перейти к формуле (=)", "切换为公式 (=)", "数式に切り替え (=)");
        ajouter("editeur_astuce_texte",
                "Texte libre. Commence par = pour utiliser une formule, par exemple : =\"Score : \" + score",
                "Plain text. Start with = to use a formula, e.g. =\"Score: \" + score",
                "Texto libre. Empieza con = para usar una fórmula, p. ej.: =\"Puntos: \" + score",
                "Texto livre. Comece com = para usar uma fórmula, ex.: =\"Pontos: \" + score",
                "Freier Text. Mit = beginnen für eine Formel, z. B.: =\"Punkte: \" + score",
                "Testo libero. Inizia con = per usare una formula, es.: =\"Punti: \" + score",
                "Обычный текст. Начните с =, чтобы использовать формулу, например: =\"Счёт: \" + score",
                "普通文本。以 = 开头可使用公式，例如：=\"分数：\" + score",
                "通常のテキスト。= で始めると数式を使えます。例：=\"スコア：\" + score");
        ajouter("editeur_couleur_perso",
                "Personnalisée (#RRGGBB)…", "Custom (#RRGGBB)…", "Personalizado (#RRGGBB)…", "Personalizada (#RRGGBB)…",
                "Benutzerdefiniert (#RRGGBB)…", "Personalizzato (#RRGGBB)…", "Свой цвет (#RRGGBB)…", "自定义 (#RRGGBB)…", "カスタム (#RRGGBB)…");
        ajouter("editeur_couleur_perso_titre",
                "Code couleur", "Color code", "Código de color", "Código de cor", "Farbcode", "Codice colore", "Код цвета", "颜色代码", "カラーコード");
        ajouter("editeur_aucun_champ",
                "Ce nœud n'a aucune valeur à saisir : choisis simplement sa cible.",
                "This node has no value to enter: just choose its target.",
                "Este nodo no tiene ningún valor que introducir: elige solo su objetivo.",
                "Este nó não tem nenhum valor a preencher: escolha apenas o alvo.",
                "Dieser Knoten hat keinen Wert zum Eingeben: Wähle nur sein Ziel.",
                "Questo nodo non ha valori da inserire: scegli solo il suo bersaglio.",
                "У этого узла нет значений для ввода: просто выберите цель.",
                "此节点没有需要输入的值：只需选择目标。",
                "このノードには入力する値がありません。ターゲットを選ぶだけです。");
    }

    // Ordre des langues : fr, en, es, pt, de, it, ru, zh, ja
    private static void ajouter(String cle, String fr, String en, String es, String pt, String de, String it, String ru, String zh, String ja) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("fr", fr);
        m.put("en", en);
        m.put("es", es);
        m.put("pt", pt);
        m.put("de", de);
        m.put("it", it);
        m.put("ru", ru);
        m.put("zh", zh);
        m.put("ja", ja);
        Traducteur.ajouterSecours(cle, m);
    }
}
// bas 1
