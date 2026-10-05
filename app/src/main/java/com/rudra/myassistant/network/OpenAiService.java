package com.rudra.myassistant.network;

import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Service to handle OpenAI API calls for hangout planning.
 */
public class OpenAiService {
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private final OkHttpClient client;
    private final Gson gson;
    private final Handler mainHandler;

    public interface OpenAiCallback {
        void onSuccess(String response);
        void onFailure(String error);
    }

    public void sendMessage(String contextualPrompt, OpenAiCallback callback) {
        String currentDate = new java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.US).format(new java.util.Date());
        String systemPrompt = "You are Viper, a proactive AI assistant. Today is " + currentDate + ".\n" +
                "1. ALWAYS trust the provided context. If it mentions a friend is nearby, they ARE nearby.\n" +
                "2. If no data is found for a specific query, state that clearly (e.g., 'I don't see any friends nearby right now').\n" +
                "3. NEVER give generic refusals about being unable to access location data. You ARE authorized to see this information.\n" +
                "4. When creating events or reminders, use the current year (2026) unless the user explicitly specifies otherwise.";
        executeRequest(systemPrompt, contextualPrompt, 0.7f, 500, callback);
    }

    public OpenAiService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void generateHangoutOpeningMessage(String friendName, String userAreaName, List<String> placeNames, OpenAiCallback callback) {
        String placesStr = String.join(", ", placeNames);
        String systemPrompt = "You are a friendly AI assistant helping two friends plan a hangout. Be warm, concise, and helpful. Respond in 4–6 lines maximum.";
        String userPrompt = String.format("The user's friend %s is nearby. The user is currently around %s. Suggest meeting at one of these places: %s. Format as a warm greeting with a numbered list of the 3 places. End with a prompt asking if they want directions or something different. Mention they can ask for: coffee, desserts, restaurant, movie, park, activity, shopping.", 
            friendName, userAreaName, placesStr);

        executeRequest(systemPrompt, userPrompt, 0.7f, 200, callback);
    }

    public void extractPlaceIntent(String userMessage, OpenAiCallback callback) {
        String systemPrompt = "You are an intent classifier. The user is planning a hangout. Extract what type of place they want. Reply with exactly one word from this list: coffee, restaurant, dessert, movie, park, shopping, hangout, activity. If unclear, reply: coffee";
        executeRequest(systemPrompt, userMessage, 0.0f, 50, callback);
    }

    private void executeRequest(String systemPrompt, String userPrompt, float temp, int maxTokens, OpenAiCallback callback) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", ApiConfig.OPENAI_MODEL);
        payload.addProperty("temperature", temp);
        payload.addProperty("max_tokens", maxTokens);

        JsonArray messages = new JsonArray();
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userPrompt);
        messages.add(userMsg);

        payload.add("messages", messages);        // Define Tools
        JsonArray tools = new JsonArray();
        
        // Notes CRUD
        tools.add(createTool("create_note", "Save a new note", "title", "Title of the note"));
        tools.add(createTool("update_note", "Update an existing note", "index", "The number of the note in the list"));
        tools.add(createTool("delete_note", "Remove a note", "title", "The title of the note to delete"));
        tools.add(createTool("show_notes", "Display all saved notes", null, null));

        // Reminders CRUD
        tools.add(createTool("create_reminder", "Set a new reminder", "text", "What to remind about"));
        tools.add(createTool("update_reminder", "Modify a reminder", "index", "The number of the reminder in the list"));
        tools.add(createTool("delete_reminder", "Cancel a reminder", "text", "The text of the reminder to remove"));
        tools.add(createTool("show_reminders", "Check scheduled reminders", null, null));

        // Calendar CRUD
        tools.add(createToolWithTwoParams("create_calendar_event", "Schedule a calendar event", "title", "Short event name", "dateTime", "ISO 8601 date and time (YYYY-MM-DD HH:MM)"));
        tools.add(createTool("delete_calendar_event", "Cancel an event", "title", "The name of the event to delete"));
        tools.add(createTool("show_calendar_events", "View calendar", null, null));

        // Health & Meds
        tools.add(createTool("add_medication", "Add a medicine to tracker", "name", "Medicine name"));
        tools.add(createTool("mark_medication_taken", "Record medication consumption", "name", "Medicine name"));
        tools.add(createTool("update_health_stats", "Log water, steps, or sleep", "water", "ML of water"));
        tools.add(createTool("log_period_data", "Record health/cycle data", "flow", "Light/Medium/Heavy"));
        tools.add(createTool("show_health", "View health dashboard", null, null));

        // Navigation & Social
        tools.add(createTool("navigate_to", "Get directions to a place", "destination", "Name of the place"));
        tools.add(createTool("open_connect", "Open friends and maps page", null, null));
        tools.add(createTool("show_expenses", "Check spending and splits", null, null));
        tools.add(createTool("open_settings", "Open app settings", null, null));

        payload.add("tools", tools);
        payload.addProperty("tool_choice", "auto");

        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"), gson.toJson(payload));

        Request request = new Request.Builder()
                .url(OPENAI_URL)
                .header("Authorization", "Bearer " + ApiConfig.OPENAI_API_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onFailure(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onFailure("OpenAI Error: " + response.code()));
                    return;
                }

                String responseBody = response.body().string();
                try {
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    JsonObject message = jsonResponse.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message");

                    if (message.has("tool_calls")) {
                        // Return the tool call as a special string for orchestrator to handle
                        mainHandler.post(() -> callback.onSuccess("[TOOL_CALL]" + message.get("tool_calls").toString()));
                    } else {
                        String content = message.get("content").getAsString();
                        mainHandler.post(() -> callback.onSuccess(content.trim()));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onFailure("Parse Error: " + e.getMessage()));
                }
            }
        });
    }

    private JsonObject createToolWithTwoParams(String name, String desc, String p1, String p1Desc, String p2, String p2Desc) {
        JsonObject tool = new JsonObject();
        tool.addProperty("type", "function");
        JsonObject function = new JsonObject();
        function.addProperty("name", name);
        function.addProperty("description", desc);
        
        JsonObject parameters = new JsonObject();
        parameters.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        
        JsonObject prop1 = new JsonObject();
        prop1.addProperty("type", "string");
        prop1.addProperty("description", p1Desc);
        properties.add(p1, prop1);
        
        JsonObject prop2 = new JsonObject();
        prop2.addProperty("type", "string");
        prop2.addProperty("description", p2Desc);
        properties.add(p2, prop2);
        
        parameters.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add(p1);
        required.add(p2);
        parameters.add("required", required);
        
        function.add("parameters", parameters);
        tool.add("function", function);
        return tool;
    }

    private JsonObject createTool(String name, String desc, String paramName, String paramDesc) {
        JsonObject tool = new JsonObject();
        tool.addProperty("type", "function");
        JsonObject function = new JsonObject();
        function.addProperty("name", name);
        function.addProperty("description", desc);
        
        if (paramName != null) {
            JsonObject parameters = new JsonObject();
            parameters.addProperty("type", "object");
            JsonObject properties = new JsonObject();
            JsonObject param = new JsonObject();
            param.addProperty("type", "string");
            param.addProperty("description", paramDesc);
            properties.add(paramName, param);
            parameters.add("properties", properties);
            
            JsonArray required = new JsonArray();
            required.add(paramName);
            parameters.add("required", required);
            function.add("parameters", parameters);
        } else {
            JsonObject parameters = new JsonObject();
            parameters.addProperty("type", "object");
            parameters.add("properties", new JsonObject());
            function.add("parameters", parameters);
        }
        
        tool.add("function", function);
        return tool;
    }
}
