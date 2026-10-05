package com.viperai.assistant;

import android.util.Log;
import com.rudra.myassistant.network.OpenAiService;
import com.viperai.assistant.bridge.AssistantFeatureBridge;
import com.viperai.assistant.context.ContextDataProvider;
import com.viperai.assistant.intent.IntentClassifier;
import com.viperai.assistant.intent.IntentConstants;
import com.viperai.assistant.intent.IntentResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AssistantOrchestrator {
    private static final String TAG = "AssistantOrchestrator";
    
    private final IntentClassifier classifier;
    private final AssistantFeatureBridge bridge;
    private final ContextDataProvider contextProvider;
    private final OpenAiService openAiService;
    private final ExecutorService executor;
    
    private final Map<String, CachedResult> intentCache = new LinkedHashMap<String, CachedResult>(10, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedResult> eldest) {
            return size() > 10;
        }
    };

    private static class CachedResult {
        IntentResult result;
        long timestamp;

        CachedResult(IntentResult result) {
            this.result = result;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public interface OrchestratorCallback {
        void onResponse(String assistantReply);
        void onError(String error);
    }

    public AssistantOrchestrator(AssistantFeatureBridge bridge, OpenAiService openAiService) {
        this.classifier = new IntentClassifier();
        this.bridge = bridge;
        this.contextProvider = new ContextDataProvider();
        this.openAiService = openAiService;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void handleUserMessage(final String message, final OrchestratorCallback callback) {
        executor.execute(() -> {
            try {
                // 1. Intent Detection with Cache
                IntentResult intentRes = getCachedOrFreshIntent(message);
                String intent = intentRes.getIntent();
                Log.d(TAG, "Detected Intent: " + intent + " (Confidence: " + intentRes.getConfidence() + ")");

                // 2. Handle Action intents (creation flows)
                if (IntentConstants.CREATE_NOTE.equals(intent)) {
                    callback.onResponse("[ACTION_CREATE_NOTE]");
                    return;
                }
                if (IntentConstants.CREATE_REMINDER.equals(intent)) {
                    callback.onResponse("[ACTION_CREATE_REMINDER]");
                    return;
                }
                if (IntentConstants.CREATE_CALENDAR_EVENT.equals(intent)) {
                    callback.onResponse("[ACTION_CREATE_EVENT]");
                    return;
                }

                // 3. Data Routing
                List<String> data = new ArrayList<>();
                if (IntentConstants.UNKNOWN.equals(intent)) {
                    // Skip data fetch, send directly to OpenAI
                } else {
                    data = fetchDataForIntent(intent);
                }

                // 4. Build Context
                String contextBlock = contextProvider.buildContext(intent, message, data);

                // 5. Call OpenAI
                openAiService.sendMessage(contextBlock, new OpenAiService.OpenAiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        callback.onResponse(response);
                    }

                    @Override
                    public void onFailure(String error) {
                        callback.onError(error);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Orchestration Error", e);
                callback.onError("Assistant internal error: " + e.getMessage());
            }
        });
    }

    private IntentResult getCachedOrFreshIntent(String message) {
        synchronized (intentCache) {
            CachedResult cached = intentCache.get(message);
            if (cached != null && (System.currentTimeMillis() - cached.timestamp < 30000)) {
                return cached.result;
            }
            IntentResult fresh = classifier.classify(message);
            intentCache.put(message, new CachedResult(fresh));
            return fresh;
        }
    }

    private List<String> fetchDataForIntent(String intent) {
        switch (intent) {
            case IntentConstants.FRIENDS_NEARBY:
            case IntentConstants.PLAN_HANGOUT:
                return bridge.getNearbyFriends();
            case IntentConstants.EXPENSE_STATUS:
                return bridge.getPendingExpenses();
            case IntentConstants.REMINDER_QUERY:
                return bridge.getTodayReminders();
            case IntentConstants.NOTE_QUERY:
                return bridge.getRecentNotes();
            default:
                return new ArrayList<>();
        }
    }

    public void triggerHangoutPlanning(String friendName, String hangoutPrompt, OrchestratorCallback callback) {
        executor.execute(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("System: Deep-link triggered from Proximity Notification.\n");
                sb.append("Current Context: Meeting friend named ").append(friendName).append(".\n");
                sb.append("Suggested Meeting Points:\n").append(hangoutPrompt).append("\n\n");
                sb.append("Assistant, please introduce yourself and ask which of these spots the user prefers, ");
                sb.append("or if they want more details about one. Be warm and efficient.");

                openAiService.sendMessage(sb.toString(), new OpenAiService.OpenAiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        callback.onResponse(response);
                    }

                    @Override
                    public void onFailure(String error) {
                        callback.onError(error);
                    }
                });
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }
}
