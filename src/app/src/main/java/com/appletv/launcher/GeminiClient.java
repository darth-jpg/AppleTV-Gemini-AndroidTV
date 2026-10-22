package com.appletv.launcher;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeminiClient {
    private static final String TAG = "GeminiClient";
    // Replace with your free Google Gemini API Key from https://aistudio.google.com/
    private static final String API_KEY = "YOUR_GEMINI_API_KEY_HERE";
    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent?key=" + API_KEY;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private static final String SYSTEM_INSTRUCTION =
            "És o Gemini, o Assistente de Inteligência Artificial do Apple TV Launcher para Android TV.\n" +
            "Responde SEMPRE e EXCLUSIVAMENTE num formato JSON válido com esta estrutura exata:\n" +
            "{\n" +
            "  \"speech\": \"frase curta, simpática e natural em português europeu para falar em voz alta\",\n" +
            "  \"action\": \"launch_app | sleep_timer | switch_hdmi | set_wallpaper | search_youtube | none\",\n" +
            "  \"param\": \"valor do parametro (nome da app, minutos, porta hdmi, etc)\",\n" +
            "  \"badge\": \"resumo curto de 3 a 5 palavras para mostrar num chip visual\"\n" +
            "}\n" +
            "Ações suportadas:\n" +
            "- launch_app: param = 'plex' | 'netflix' | 'smarttube' | 'prime' | 'max' | 'steamlink' | 'luna' | 'settings'\n" +
            "- sleep_timer: param = '15' | '30' | '45' | '60' | '90' | '0' (0 cancela o temporizador)\n" +
            "- switch_hdmi: param = '1' | '2' | '3'\n" +
            "- set_wallpaper: param = 'day' | 'sunset' | 'night'\n" +
            "- search_youtube: param = 'texto da pesquisa'\n" +
            "- none: para recomendações de filmes, séries, perguntas de cultura geral ou conversa casual.";

    public interface GeminiCallback {
        void onSuccess(String speech, String action, String param, String badge);
        void onError(String errorMessage);
    }

    public static void ask(final String userQuery, final GeminiCallback callback) {
        if ("YOUR_GEMINI_API_KEY_HERE".equals(API_KEY) || API_KEY.trim().isEmpty()) {
            MAIN_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    callback.onError("Chave API do Gemini não configurada. Define a tua chave gratuita em GeminiClient.java");
                }
            });
            return;
        }
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject requestJson = new JSONObject();

                    // System Instruction
                    JSONObject sysPart = new JSONObject();
                    sysPart.put("text", SYSTEM_INSTRUCTION);
                    JSONObject sysContent = new JSONObject();
                    sysContent.put("parts", new JSONArray().put(sysPart));
                    requestJson.put("systemInstruction", sysContent);

                    // User Content
                    JSONObject userPart = new JSONObject();
                    userPart.put("text", userQuery);
                    JSONObject userContent = new JSONObject();
                    userContent.put("role", "user");
                    userContent.put("parts", new JSONArray().put(userPart));

                    JSONArray contentsArray = new JSONArray();
                    contentsArray.put(userContent);
                    requestJson.put("contents", contentsArray);

                    // Generation Config (enforce JSON output)
                    JSONObject genConfig = new JSONObject();
                    genConfig.put("responseMimeType", "application/json");
                    genConfig.put("temperature", 0.3);
                    requestJson.put("generationConfig", genConfig);

                    String postData = requestJson.toString();

                    URL url = new URL(ENDPOINT);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(12000);
                    conn.setDoOutput(true);

                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = postData.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }

                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300)
                            ? conn.getInputStream()
                            : conn.getErrorStream();

                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line.trim());
                        }
                    }

                    String rawResponse = sb.toString();
                    Log.d(TAG, "Gemini Response (" + responseCode + "): " + rawResponse);

                    if (responseCode != 200) {
                        notifyError(callback, "Erro " + responseCode + " da API Gemini");
                        return;
                    }

                    JSONObject resObj = new JSONObject(rawResponse);
                    JSONArray candidates = resObj.optJSONArray("candidates");
                    if (candidates == null || candidates.length() == 0) {
                        notifyError(callback, "Nenhuma resposta gerada.");
                        return;
                    }

                    JSONObject candidate = candidates.getJSONObject(0);
                    JSONObject content = candidate.getJSONObject("content");
                    JSONArray parts = content.getJSONArray("parts");
                    String text = parts.getJSONObject(0).getString("text");

                    // Parse JSON payload returned by model
                    JSONObject actionObj = new JSONObject(text);
                    final String speech = actionObj.optString("speech", "Com certeza.");
                    final String action = actionObj.optString("action", "none");
                    final String param = actionObj.optString("param", "");
                    final String badge = actionObj.optString("badge", "");

                    MAIN_HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onSuccess(speech, action, param, badge);
                            }
                        }
                    });

                } catch (final Exception e) {
                    Log.e(TAG, "Error querying Gemini", e);
                    notifyError(callback, "Falha na ligação: " + e.getMessage());
                }
            }
        });
    }

    private static void notifyError(final GeminiCallback callback, final String message) {
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onError(message);
                }
            }
        });
    }
}
