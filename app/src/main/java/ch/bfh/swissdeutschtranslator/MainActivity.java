package ch.bfh.swissdeutschtranslator;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private EditText editTextInput;
    private TextView textViewResult;
    private ProgressBar progressBar;
    private Button buttonTranslate;
    private ImageButton buttonSpeak;
    private OpenAiService openAiService;
    private SpeechRecognizer speechRecognizer;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startListening();
                } else {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        openAiService = new OpenAiService(this);

        editTextInput = findViewById(R.id.editTextInput);
        textViewResult = findViewById(R.id.textViewResult);
        progressBar = findViewById(R.id.progressBar);
        buttonTranslate = findViewById(R.id.buttonTranslate);
        buttonSpeak = findViewById(R.id.buttonSpeak);

        buttonTranslate.setOnClickListener(v -> onTranslateClicked());
        buttonSpeak.setOnClickListener(v -> onSpeakClicked());

        findViewById(R.id.buttonSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class))
        );

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Toast.makeText(MainActivity.this, R.string.listening, Toast.LENGTH_SHORT).show();
                buttonSpeak.setEnabled(false);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    editTextInput.setText(matches.get(0));
                }
                buttonSpeak.setEnabled(true);
            }

            @Override
            public void onError(int error) {
                String errorMessage;
                switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO:
                        errorMessage = getString(R.string.error_speech_audio); break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        errorMessage = getString(R.string.error_speech_client); break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        errorMessage = getString(R.string.error_speech_permission); break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        errorMessage = getString(R.string.error_speech_network); break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        errorMessage = getString(R.string.error_speech_network_timeout); break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        errorMessage = getString(R.string.error_speech_no_match); break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        errorMessage = getString(R.string.error_speech_recognizer_busy); break;
                    case SpeechRecognizer.ERROR_SERVER:
                        errorMessage = getString(R.string.error_speech_server); break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        errorMessage = getString(R.string.error_speech_timeout); break;
                    default:
                        errorMessage = getString(R.string.error_speech_unknown, error); break;
                }
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                buttonSpeak.setEnabled(true);
            }

            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void onSpeakClicked() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startListening();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-CH");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "de-CH");
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speechRecognizer.startListening(intent);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}