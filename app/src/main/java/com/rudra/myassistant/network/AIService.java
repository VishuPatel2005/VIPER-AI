package com.rudra.myassistant.network;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIService {
    private static final String TAG = "AIService";
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final OkHttpClient sharedClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    private static final Gson sharedGson = new Gson();
    private final android.os.Handler mainHandler;

    public interface AIResponseListener {
        void onSuccess(String response);
        void onToolCall(String functionName, JsonObject arguments);
        void onFailure(String error);
    }

    public AIService() {
        this.mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    }

    public void askAssistant(String userMessage, JsonArray history, AIResponseListener listener) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", ApiConfig.OPENAI_MODEL);

        JsonArray messages = new JsonArray();
        
        // System Prompt
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", PromptManager.getSystemPrompt());
        messages.add(systemMsg);

        // History (if any)
        if (history != null) {
            messages.addAll(history);
        }

        // Current Message
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);

        payload.add("messages", messages);
        
        // Tools
        payload.add("tools", sharedGson.fromJson(PromptManager.getAssistantToolsJson(), JsonArray.class));
        payload.addProperty("tool_choice", "auto");

        String json = sharedGson.toJson(payload);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);

        Request request = new Request.Builder()
                .url(OPENAI_URL)
                .header("Authorization", "Bearer " + ApiConfig.OPENAI_API_KEY)
                .post(body)
                .build();

        sharedClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> listener.onFailure(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String err = "OpenAI Error: " + response.code();
                    mainHandler.post(() -> listener.onFailure(err));
                    return;
                }

                String bodyStr = response.body().string();
                mainHandler.post(() -> {
                    try {
                        JsonObject responseJson = sharedGson.fromJson(bodyStr, JsonObject.class);
                        JsonArray choices = responseJson.getAsJsonArray("choices");
                        
                        if (choices != null && choices.size() > 0) {
                            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
                            
                            // Handle Tool Calls
                            if (message.has("tool_calls")) {
                                JsonArray toolCalls = message.getAsJsonArray("tool_calls");
                                if (toolCalls.size() > 0) {
                                    JsonObject toolCall = toolCalls.get(0).getAsJsonObject();
                                    JsonObject function = toolCall.getAsJsonObject("function");
                                    String functionName = function.get("name").getAsString();
                                    JsonObject args = sharedGson.fromJson(function.get("arguments").getAsString(), JsonObject.class);
                                    listener.onToolCall(functionName, args);
                                } else {
                                    listener.onSuccess(message.get("content").getAsString());
                                }
                            } else if (message.has("content") && !message.get("content").isJsonNull()) {
                                listener.onSuccess(message.get("content").getAsString());
                            } else {
                                listener.onFailure("Empty response from AI");
                            }
                        } else {
                            listener.onFailure("No response choices found");
                        }
                    } catch (Exception e) {
                        listener.onFailure("Parsing Error: " + e.getMessage());
                    }
                });
            }
        });
    }
}
