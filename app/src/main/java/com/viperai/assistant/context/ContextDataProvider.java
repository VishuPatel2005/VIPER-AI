package com.viperai.assistant.context;

import java.util.List;

public class ContextDataProvider {

    public String buildContext(String intent, String userQuery, List<String> dataItems) {
        StringBuilder sb = new StringBuilder();
        String currentDate = new java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.US).format(new java.util.Date());
        sb.append("Today's Date: ").append(currentDate).append("\n");
        sb.append("User intent: ").append(intent).append("\n");
        sb.append("User query: ").append(userQuery).append("\n\n");

        if (dataItems == null || dataItems.isEmpty()) {
            sb.append("No specific data found. Answer based on general knowledge and app context.");
        } else {
            sb.append("Relevant data:\n");
            for (String item : dataItems) {
                sb.append("- ").append(item).append("\n");
            }
            sb.append("\nUsing the above data, answer the user's question directly and factually.");
        }

        return sb.toString();
    }
}
