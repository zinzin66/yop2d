package com.ludexa.moteur;

import android.content.Context;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

// Statistiques d'utilisation anonymes (Aptabase, serveur européen).
// Envoie un seul signal "yop2d_ouvert" par ouverture de l'appli :
// version de Yop2D, version d'Android, langue de l'appareil. Aucune donnée personnelle.
// Le résultat (réussi / échoué) est écrit dans le journal général (appui long sur le numéro de version).
public class Statistiques {

    private static final String CLE_APP = "A-EU-3140288404";
    private static final String ADRESSE = "https://eu.aptabase.com/api/v0/events";

    // Empêche d'envoyer deux fois pendant la même ouverture (ex : changement de langue qui recharge l'écran)
    private static boolean dejaEnvoye = false;

    // Dossier où est rangé le journal général (hors de tout projet)
    public static String cheminJournal(Context contexte) {
        return contexte.getApplicationContext().getFilesDir().getAbsolutePath();
    }

    public static void signalerOuverture(Context contexte) {
        if (dejaEnvoye) return;
        dejaEnvoye = true;

        final Context app = contexte.getApplicationContext();
        final String cheminLog = cheminJournal(app);

        new Thread(() -> {
            HttpURLConnection connexion = null;
            try {
                // Numéro de version de Yop2D
                String versionNom = "";
                long versionCode = 0;
                try {
                    android.content.pm.PackageInfo info = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
                    versionNom = info.versionName;
                    versionCode = (Build.VERSION.SDK_INT >= 28) ? info.getLongVersionCode() : info.versionCode;
                } catch (Exception ignore) {}

                // Heure au format attendu par Aptabase
                SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                formatDate.setTimeZone(TimeZone.getTimeZone("UTC"));

                // Numéro de session tiré au hasard à chaque ouverture (ne permet pas de reconnaître quelqu'un)
                String session = (System.currentTimeMillis() / 1000)
                        + String.format(Locale.US, "%08d", new Random().nextInt(100000000));

                JSONObject infosSysteme = new JSONObject();
                infosSysteme.put("locale", Locale.getDefault().toLanguageTag());
                infosSysteme.put("osName", "Android");
                infosSysteme.put("osVersion", Build.VERSION.RELEASE);
                infosSysteme.put("isDebug", false);
                infosSysteme.put("appVersion", versionNom);
                infosSysteme.put("appBuildNumber", String.valueOf(versionCode));
                infosSysteme.put("sdkVersion", "yop2d-java@1.0");

                JSONObject evenement = new JSONObject();
                evenement.put("timestamp", formatDate.format(new Date()));
                evenement.put("sessionId", session);
                evenement.put("eventName", "yop2d_ouvert");
                evenement.put("systemProps", infosSysteme);
                evenement.put("props", new JSONObject());

                JSONArray liste = new JSONArray();
                liste.put(evenement);
                byte[] contenu = liste.toString().getBytes("UTF-8");

                connexion = (HttpURLConnection) new URL(ADRESSE).openConnection();
                connexion.setRequestMethod("POST");
                connexion.setConnectTimeout(10000);
                connexion.setReadTimeout(10000);
                connexion.setDoOutput(true);
                connexion.setRequestProperty("Content-Type", "application/json");
                connexion.setRequestProperty("App-Key", CLE_APP);

                OutputStream sortie = connexion.getOutputStream();
                sortie.write(contenu);
                sortie.close();

                int code = connexion.getResponseCode();
                if (code >= 200 && code < 300) {
                    DiagLogger.log(cheminLog, "STATS envoi réussi (code " + code + ", version " + versionNom + ")");
                } else {
                    DiagLogger.log(cheminLog, "STATS envoi refusé par le serveur (code " + code + ")");
                }
            } catch (Exception e) {
                // Pas d'internet, serveur injoignable... l'appli continue normalement
                DiagLogger.log(cheminLog, "STATS échec : " + e.getMessage());
            } finally {
                if (connexion != null) connexion.disconnect();
            }
        }).start();
    }
}
