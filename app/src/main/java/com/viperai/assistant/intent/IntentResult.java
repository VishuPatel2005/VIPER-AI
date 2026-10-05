package com.viperai.assistant.intent;

public class IntentResult {
    private final String intent;
    private final float confidence;
    private final String rawQuery;

    public IntentResult(String intent, float confidence, String rawQuery) {
        this.intent = intent;
        this.confidence = confidence;
        this.rawQuery = rawQuery;
    }

    public String getIntent() {
        return intent;
    }

    public float getConfidence() {
        return confidence;
    }

    public String getRawQuery() {
        return rawQuery;
    }

    @Override
    public String toString() {
        return "IntentResult{" +
                "intent='" + intent + '\'' +
                ", confidence=" + confidence +
                ", rawQuery='" + rawQuery + '\'' +
                '}';
    }
}
