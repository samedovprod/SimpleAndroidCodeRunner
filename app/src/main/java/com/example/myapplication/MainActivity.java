package com.example.myapplication;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    EditText codeEditText;
    TextView resultTextView;
    Button runButton;
    EditText inputEditText;
    boolean isInput = false;
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Spinner languageSpinner;
    private static final String URL = "PUT_URL";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        codeEditText = findViewById(R.id.codeEditText);
        resultTextView = findViewById(R.id.resultTextView);
        runButton = findViewById(R.id.runButton);
        languageSpinner = findViewById(R.id.languageSpinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.languages,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);

        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedLanguage = languageSpinner.getSelectedItem().toString();

                if ("python".equals(selectedLanguage)) {
                    codeEditText.setText("print('Hello, World!')");
                } else if ("java".equals(selectedLanguage)) {
                    codeEditText.setText("public class HelloWorld {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, World!\");\n    }\n}");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        runButton.setOnClickListener(v -> {
            String selectedLanguage = languageSpinner.getSelectedItem().toString();
            String code = codeEditText.getText().toString();
            executeCode(selectedLanguage, code, null);
        });
    }

    private void executeCode(String selectedLanguage, String code, String userInput) {
        executorService.execute(() -> {
            String result = sendCode(selectedLanguage, code, userInput);
            runOnUiThread(() -> Result(result));
        });
    }



    private String sendCode(String selectedLanguage, String code, String userInput) {
        try {
            URL obj = new URL(URL);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            StringBuilder postData = new StringBuilder();
            postData.append("language=").append(selectedLanguage);
            postData.append("&code=").append(URLEncoder.encode(code, "UTF-8"));

            if (userInput != null && !userInput.isEmpty()) {
                postData.append("&user_input=").append(URLEncoder.encode(userInput, "UTF-8"));
            }

            try (OutputStream os = connection.getOutputStream()) {
                os.write(postData.toString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (Scanner scanner = new Scanner(connection.getInputStream())) {
                    scanner.useDelimiter("\\A");
                    return scanner.hasNext() ? scanner.next() : "";
                }
            } else {
                return "Error";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Error";
        }
    }


    private void Result(String result) {
        if ("input_required".equals(result.trim())) {
            Input();
        } else {
            resultTextView.setVisibility(View.VISIBLE);
            resultTextView.setText(result);
            isInput = false;
        }
    }

    private void Input() {
        resultTextView.setVisibility(View.GONE);

        if (inputEditText == null) {
            inputEditText = new EditText(this);
            inputEditText.setInputType(InputType.TYPE_CLASS_TEXT);
            inputEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
            ((ViewGroup) resultTextView.getParent()).addView(inputEditText);
        } else {
            inputEditText.setVisibility(View.VISIBLE);
        }

        inputEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String userInput = inputEditText.getText().toString();
                String selectedLanguage = languageSpinner.getSelectedItem().toString();
                String code = codeEditText.getText().toString();
                executeCode(selectedLanguage, code, userInput);

                ((ViewGroup) inputEditText.getParent()).removeView(inputEditText);
                inputEditText = null;
                resultTextView.setVisibility(View.VISIBLE);

                return true;
            }
            return false;
        });

    }
}
