package ch.bfh.swissdeutschtranslator;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText editTextInput;
    private TextView textViewResult;
    private ProgressBar progressBar;
    private Button buttonTranslate;
    private OpenAiService openAiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        openAiService = new OpenAiService(this);

        editTextInput = findViewById(R.id.editTextInput);
        textViewResult = findViewById(R.id.textViewResult);
        progressBar = findViewById(R.id.progressBar);
        buttonTranslate = findViewById(R.id.buttonTranslate);

        buttonTranslate.setOnClickListener(v -> onTranslateClicked());

        findViewById(R.id.buttonSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class))
        );
    }

    private void onTranslateClicked() {
        String input = editTextInput.getText().toString().trim();

        if (input.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_input, Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        textViewResult.setText("");
        buttonTranslate.setEnabled(false);

        openAiService.translate(input, new OpenAiService.TranslationCallback() {
            @Override
            public void onSuccess(String translation) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    buttonTranslate.setEnabled(true);
                    textViewResult.setText(translation);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    buttonTranslate.setEnabled(true);
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}