package com.rudra.myassistant.network;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReminderInterpreter {

    public static class InterpretResult {
        public Date date;
        public String cleanText;

        public InterpretResult(Date date, String cleanText) {
            this.date = date;
            this.cleanText = cleanText;
        }
    }

    /**
     * Ports the Python parse_datetime_nlp logic to Java.
     * Extracts date/time info and cleans the task description.
     */
    public static InterpretResult interpret(String text, Date baseDate) {
        Calendar cal = Calendar.getInstance();
        if (baseDate != null) cal.setTime(baseDate);
        
        String cleanText = text;
        String lower = text.toLowerCase();

        // 1. Handle Relative Dates
        if (lower.contains("tomorrow")) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            cleanText = cleanText.replaceAll("(?i)\\btomorrow\\b", "");
        } else if (lower.contains("today")) {
            cleanText = cleanText.replaceAll("(?i)\\btoday\\b", "");
        }

        // 2. Handle Weekdays
        String[] weekdays = {"sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"};
        for (int i = 0; i < weekdays.length; i++) {
            if (lower.contains(weekdays[i])) {
                int currentDay = cal.get(Calendar.DAY_OF_WEEK);
                int targetDay = i + 1;
                int diff = targetDay - currentDay;
                if (diff <= 0) diff += 7;
                cal.add(Calendar.DAY_OF_YEAR, diff);
                cleanText = cleanText.replaceAll("(?i)\\b" + weekdays[i] + "\\b", "");
                break;
            }
        }

        // 3. Handle Time (Simple 9am/7pm/10:30 format)
        Pattern timePattern = Pattern.compile("(\\d{1,2})(:(\\d{2}))?\\s*(am|pm)?", Pattern.CASE_INSENSITIVE);
        Matcher m = timePattern.matcher(lower);
        if (m.find()) {
            int hour = Integer.parseInt(m.group(1));
            int minute = (m.group(3) != null) ? Integer.parseInt(m.group(3)) : 0;
            String ampm = m.group(4);
            
            if (ampm != null) {
                if (ampm.equalsIgnoreCase("pm") && hour < 12) hour += 12;
                else if (ampm.equalsIgnoreCase("am") && hour == 12) hour = 0;
            }
            
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);
            
            cleanText = cleanText.replace(m.group(0), "");
        } else if (baseDate == null) {
            // Default to 9 AM if no time specified
            cal.set(Calendar.HOUR_OF_DAY, 9);
            cal.set(Calendar.MINUTE, 0);
        }

        // Final Cleanup
        cleanText = cleanText.replaceAll("(?i)\\b(on|at|for|remind me to|set a reminder for)\\b", "");
        cleanText = cleanText.replaceAll("\\s+", " ").strip();

        return new InterpretResult(cal.getTime(), cleanText);
    }
}
