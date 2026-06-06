package ch.bfh.swissdeutschtranslator;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {

    private EditText editTextApiKey;
    private TextView textViewCurrentKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        editTextApiKey = findViewById(R.id.editTextApiKey);
        textViewCurrentKey = findViewById(R.id.textViewCurrentKey);

        // Bestehenden Key laden und anzeigen
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String existingKey = prefs.getString("openai_api_key", "");
        if (!existingKey.isEmpty()) {
            showKey(existingKey);
        }

        findViewById(R.id.buttonSaveKey).setOnClickListener(v -> {
            String key = editTextApiKey.getText().toString().trim();
            if (key.isEmpty()) {
                Toast.makeText(this, R.string.error_no_api_key, Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit().putString("openai_api_key", key).apply();
            Toast.makeText(this, R.string.api_key_saved, Toast.LENGTH_SHORT).show();
            showKey(key);
        });

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
    }

    /**
     * Zeigt den API-Key in maskierter Form an (nur die ersten und letzten 4 Zeichen sichtbar).
     * @param key Der zensierte API-Key, der angezeigt werden soll.
     */
    private void showKey(String key) {
        String masked;
        if (key.length() > 8) {
            masked = key.substring(0, 4) + "****" + key.substring(key.length() - 4);
        } else {
            masked = "****";
        }
        textViewCurrentKey.setText(getString(R.string.current_key_label) + masked);
        textViewCurrentKey.setVisibility(android.view.View.VISIBLE);
    }
}