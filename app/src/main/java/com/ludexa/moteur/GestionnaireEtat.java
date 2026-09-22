// haut 1
package com.ludexa.moteur;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GestionnaireEtat {
    
    // Dictionnaires pour stocker le JSON des objets et des variables pour chaque ID de scène
    private static Map<String, String> cacheObjets = new HashMap<>();
    private static Map<String, String> cacheVariables = new HashMap<>();

    // État de DÉPART de chaque scène, mémorisé la première fois qu'elle devient la scène active :
    // c'est lui que retrouve le nœud "Recommencer la scène".
    private static Map<String, String> departObjets = new HashMap<>();
    private static Map<String, String> departVariables = new HashMap<>();
    private static Gson gson = new Gson();

    // Appelé au démarrage du mode Play pour repartir à zéro
    public static void viderCache() {
        cacheObjets.clear();
        cacheVariables.clear();
        departObjets.clear();
        departVariables.clear();
    }

    // Mémorise l'état de départ de la scène. Une seule fois par partie : les passages suivants ne changent rien.
    public static void memoriserEtatInitial(Scene scene) {
        if (scene == null || scene.id == null || departObjets.containsKey(scene.id)) return;
        departObjets.put(scene.id, gson.toJson(scene.objets));
        departVariables.put(scene.id, gson.toJson(scene.variablesLocales));
    }

    // Remet les objets et les variables locales de la scène dans leur état de départ,
    // et oublie l'état mémorisé pendant la partie. Retourne false si l'état de départ est inconnu (la scène n'est alors pas touchée).
    public static boolean reinitialiserScene(Scene scene) {
        if (scene == null || scene.id == null || !departObjets.containsKey(scene.id)) return false;
        cacheObjets.remove(scene.id);
        cacheVariables.remove(scene.id);

        Type typeListObjets = new TypeToken<ArrayList<ObjetBase>>(){}.getType();
        List<ObjetBase> objets = gson.fromJson(departObjets.get(scene.id), typeListObjets);
        if (objets != null) scene.objets = objets;

        Type typeListVariables = new TypeToken<ArrayList<Variable>>(){}.getType();
        List<Variable> variables = gson.fromJson(departVariables.get(scene.id), typeListVariables);
        if (variables != null) scene.variablesLocales = variables;
        return true;
    }

    public static void sauvegarderEtat(Scene scene) {
        if (scene == null || scene.id == null) return;
        
        // On sauvegarde la liste des objets
        String jsonObjets = gson.toJson(scene.objets);
        cacheObjets.put(scene.id, jsonObjets);
        
        // On sauvegarde les variables locales
        String jsonVariables = gson.toJson(scene.variablesLocales);
        cacheVariables.put(scene.id, jsonVariables);
    }

    public static void restaurerEtat(Scene scene) {
        if (scene == null || scene.id == null) return;

        // Restauration des objets s'ils existent dans le cache
        if (cacheObjets.containsKey(scene.id)) {
            String jsonObjets = cacheObjets.get(scene.id);
            Type typeListObjets = new TypeToken<ArrayList<ObjetBase>>(){}.getType();
            List<ObjetBase> objetsRestaures = gson.fromJson(jsonObjets, typeListObjets);
            if (objetsRestaures != null) {
                scene.objets = objetsRestaures;
            }
        }

        // Restauration des variables locales si elles existent dans le cache
        if (cacheVariables.containsKey(scene.id)) {
            String jsonVariables = cacheVariables.get(scene.id);
            Type typeListVariables = new TypeToken<ArrayList<Variable>>(){}.getType();
            List<Variable> variablesRestaurees = gson.fromJson(jsonVariables, typeListVariables);
            if (variablesRestaurees != null) {
                scene.variablesLocales = variablesRestaurees;
            }
        }
    }
}
// bas 1
