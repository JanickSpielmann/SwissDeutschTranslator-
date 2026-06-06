package ch.bfh.swissdeutschtranslator;

import android.content.Context;
import androidx.preference.PreferenceManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OpenAiService {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4.1";
    private static final MediaType JSON = MediaType.get("application/json");

    private final OkHttpClient client = new OkHttpClient();
    private final Context context;

    public interface TranslationCallback {
        void onSuccess(String translation);
        void onError(String errorMessage);
    }

    public OpenAiService(Context context) {
        this.context = context;
    }

    public void translate(String inputText, TranslationCallback callback) {

        /**
         * API-Key aus SharedPreferences lesen. Der Key muss in den App-Einstellungen eingegeben werden.
         */
        String apiKey = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString("openai_api_key", "");

        if (apiKey.isEmpty()) {
            callback.onError(context.getString(R.string.error_no_api_key));
            return;
        }

        /**
         * System-Message definiert die Rolle des Modells (Übersetzer ins Berndeutsch) und die Anweisungen.
          */
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content",
                "Du bist ein Experte für Schweizerdeutsch, speziell Berndeutsch. " +
                        "Übersetze den folgenden hochdeutschen Text in authentisches Berndeutsch. " +
                        "Verwende typische Berndeutsche Ausdrücke und Grammatik, z.B.: " +
                        "'nicht' wird zu 'nid', 'ich' wird zu 'i', " +
                        "'was' wird zu 'was', 'gut' wird zu 'guet', " +
                        "'haben' wird zu 'haa', 'sein' wird zu 'sii', " +
                        "'gehen' wird zu 'gaa', 'kommen' wird zu 'cho', " +
                        "'jetzt' wird zu 'jitz', 'etwas' wird zu 'öppis', " +
                        "'nichts' wird zu 'nüt', 'viel' wird zu 'viu'. " +
                        "Gib nur die Übersetzung zurück, ohne Erklärungen." +
                        "Zahlen sollten auch auf Schweizerdeutsch umgeschrieben werden, " +
                        "z.B. 'eins' oder '1' wird zu 'eis', 'zwei' oder '2' wird zu 'zwo', usw. " );
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", inputText);

        JsonArray messages = new JsonArray();
        messages.add(systemMessage);
        messages.add(userMessage);

        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.add("messages", messages);
        body.addProperty("max_tokens", 500); // Begrenzung der Antwortlänge und Vermeidung von zu langen Antworten, sowie hät die Kosten im Griff, da die Kosten pro Token berechnet werden.

        RequestBody requestBody = RequestBody.create(body.toString(), JSON);

        /**
         * HTTP-Request mit OkHttp erstellen. Die API-Key wird im Authorization-Header übergeben, der Body enthält die JSON-Daten mit den Nachrichten.
         * Die Anfrage wird asynchron ausgeführt, um die UI nicht zu blockieren. Bei Erfolg wird die Antwort geparst und die Übersetzung zurückgegeben, bei Fehlern wird eine Fehlermeldung über den Callback zurückgegeben.
         *
         */
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();

        // Asynchroner Call
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(context.getString(R.string.error_api_call));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError(context.getString(R.string.error_api_call));
                    return;
                }
                String responseBody = response.body().string();
                String translation = parseTranslation(responseBody);
                callback.onSuccess(translation);
            }
        });
    }

    /**
     * Die Methode parseTranslation extrahiert die Übersetzung aus der JSON-Antwort der OpenAI API. Sie navigiert durch die JSON-Struktur, um den Inhalt der Antwort zu erhalten, und gibt diesen als String zurück.
     * @param json Die JSON-Antwort der OpenAI API als String.
     * @return Die extrahierte Übersetzung als String.
     */
    private String parseTranslation(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return root
                .getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString()
                .trim();
    }
}