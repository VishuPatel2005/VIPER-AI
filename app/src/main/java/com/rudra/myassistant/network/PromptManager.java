package com.rudra.myassistant.network;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PromptManager {

    /**
     * Replicates the Tool Definitions from logic.py for OpenAI function calling.
     */
    public static String getAssistantToolsJson() {
        return "[" +
            "{\"type\":\"function\",\"function\":{\"name\":\"create_reminder\",\"description\":\"Create a new reminder. Use this even if text or time is missing; the system will ask for them.\",\"parameters\":{\"type\":\"object\",\"properties\":{\"text\":{\"type\":\"string\",\"description\":\"The description of the reminder. ONLY the task (e.g. 'Presentation'), DO NOT include date/time words here.\"},\"time\":{\"type\":\"string\",\"description\":\"The time/date for the reminder (e.g. 'Saturday at 7am'). EXTRACT this from the raw text.\"}}}}}," +
            "{\"type\":\"function\",\"function\":{\"name\":\"delete_reminder\",\"description\":\"Delete a reminder.\",\"parameters\":{\"type\":\"object\",\"properties\":{\"index\":{\"type\":\"integer\",\"description\":\"1-based index.\"},\"text\":{\"type\":\"string\",\"description\":\"Text of the reminder.\"}}}}}," +
            "{\"type\":\"function\",\"function\":{\"name\":\"show_reminders\",\"description\":\"List all active reminders.\",\"parameters\":{\"type\":\"object\",\"properties\":{}}}}," +
            "{\"type\":\"function\",\"function\":{\"name\":\"create_note\",\"description\":\"Save a new note. Use even if title or content is missing.\",\"parameters\":{\"type\":\"object\",\"properties\":{\"title\":{\"type\":\"string\",\"description\":\"A short title.\"},\"content\":{\"type\":\"string\",\"description\":\"The full content.\"}}}}}," +
            "{\"type\":\"function\",\"function\":{\"name\":\"show_notes\",\"description\":\"List all saved notes.\",\"parameters\":{\"type\":\"object\",\"properties\":{}}}}," +
            "{\"type\":\"function\",\"function\":{\"name\":\"delete_note\",\"description\":\"Delete a note.\",\"parameters\":{\"type\":\"object\",\"properties\":{\"index\":{\"type\":\"integer\",\"description\":\"1-based index.\"},\"title\":{\"type\":\"string\",\"description\":\"Title of the note.\"}}}}}," +
            "{\"type\":\"function\",\"function\":{\"name\":\"update_reminder\",\"description\":\"Update an existing reminder.\",\"parameters\":{\"type\":\"object\",\"properties\":{\"index\":{\"type\":\"integer\",\"description\":\"1-based index.\"},\"text\":{\"type\":\"string\",\"description\":\"New description.\"},\"time\":{\"type\":\"string\",\"description\":\"New time.\"}},\"required\":[\"index\"]}}}," +
            "{\"type\":\"function\",\"function\":{\"name\":\"update_note\",\"description\":\"Update an existing note.\",\"parameters\":{\"type\":\"object\",\"properties\":{\"index\":{\"type\":\"integer\",\"description\":\"1-based index.\"},\"title\":{\"type\":\"string\",\"description\":\"New title.\"},\"content\":{\"type\":\"string\",\"description\":\"New content.\"}},\"required\":[\"index\"]}}}," +
            "{\"type\":\"function\",\"function\":{\"name\":\"create_calendar_event\",\"description\":\"Schedule a new event in the calendar. Use even if title or time is missing.\",\"parameters\":{\"type\":\"object\",\"properties\":{\"title\":{\"type\":\"string\",\"description\":\"Title of the event (e.g. 'Project Meeting'). DO NOT include date/time words here.\"},\"time\":{\"type\":\"string\",\"description\":\"Start time/date for the event (e.g. 'Saturday at 7am'). EXTRACT this from the user message.\"},\"description\":{\"type\":\"string\",\"description\":\"Optional description.\"}}}}}," +
            "{\"type\":\"function\",\"function\":{\"name\":\"show_calendar_events\",\"description\":\"List upcoming calendar events.\",\"parameters\":{\"type\":\"object\",\"properties\":{}}}}," +
            "{\"type\":\"function\",\"function\":{\"name\":\"delete_calendar_event\",\"description\":\"Delete or cancel a calendar event.\",\"parameters\":{\"type\":\"object\",\"properties\":{\"index\":{\"type\":\"integer\",\"description\":\"1-based index.\"},\"title\":{\"type\":\"string\",\"description\":\"Title of the event.\"}}}}}," +
            "{\"type\":\"function\",\"function\":{\"name\":\"add_medication\",\"description\":\"Add a new medication for the user to track.\",\"parameters\":{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\",\"description\":\"Name of the medication.\"},\"dosage\":{\"type\":\"string\",\"description\":\"Dosage (e.g. 500mg, 1 pill).\"},\"time\":{\"type\":\"string\",\"description\":\"Time to take it (e.g. 08:30).\"},\"days\":{\"type\":\"array\",\"items\":{\"type\":\"string\"},\"description\":\"Days of the week or ['Everyday'].\"}}}}}," +
            "{\"type\":\"function\",\"function\":{\"name\":\"show_medications\",\"description\":\"List all current medications and their status.\",\"parameters\":{\"type\":\"object\",\"properties\":{}}}}," +
            "{\"type\":\"function\",\"function\":{\"name\":\"mark_medication_taken\",\"description\":\"Mark a medication as taken or unmark it if specified.\",\"parameters\":{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\",\"description\":\"Name of the medication.\"},\"status\":{\"type\":\"boolean\",\"description\":\"True to mark as taken, False for unmark.\"}},\"required\":[\"name\",\"status\"]}}}," +
            "{\"type\":\"function\",\"function\":{\"name\":\"log_period_data\",\"description\":\"Log menstrual cycle data like flow, symptoms, spotting, or last period dates.\",\"parameters\":{\"type\":\"object\",\"properties\":{\"start_date\":{\"type\":\"string\",\"description\":\"Start date (YYYY-MM-DD).\"},\"end_date\":{\"type\":\"string\",\"description\":\"End date (YYYY-MM-DD).\"},\"flow\":{\"type\":\"string\",\"description\":\"Flow level: Light, Medium, or Heavy.\"},\"symptoms\":{\"type\":\"array\",\"items\":{\"type\":\"string\"},\"description\":\"List of symptoms.\"},\"spotting\":{\"type\":\"boolean\",\"description\":\"Whether spotting occurred.\"},\"date\":{\"type\":\"string\",\"description\":\"Log date (YYYY-MM-DD).\"}}}}}," +
            "{\"type\":\"function\",\"function\":{\"name\":\"update_health_stats\",\"description\":\"Log or update daily health stats like water intake, steps, or sleep.\",\"parameters\":{\"type\":\"object\",\"properties\":{\"water\":{\"type\":\"integer\",\"description\":\"Glasses of water to ADD.\"},\"steps\":{\"type\":\"integer\",\"description\":\"Total steps.\"},\"sleep_hours\":{\"type\":\"number\",\"description\":\"Hours of sleep.\"}}}}}," +
            "{\"type\":\"function\",\"function\":{\"name\":\"remember_personal_detail\",\"description\":\"Store a personal detail about the user for long-term memory (e.g. 'I like spicy food').\",\"parameters\":{\"type\":\"object\",\"properties\":{\"content\":{\"type\":\"string\",\"description\":\"Information to remember.\"},\"memory_type\":{\"type\":\"string\",\"description\":\"Category: preference, birthday, family, work, health, other.\",\"default\":\"preference\"}},\"required\":[\"content\"]}}}" +
            "]";
    }

    public static String getSystemPrompt() {
        String today = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US).format(new Date());
        return "Today's date is " + today + ". " +
               "You are Viper, the core engine of the 'MyAssistant' Android app. " +
               "You manage reminders, notes, calendar, and health data. " +
               "You MUST extract intents and separate the task/title from the time/date. " +
               "Guidelines:\n" +
               "- DO NOT include date/time words in the 'text' or 'title' fields.\n" +
               "- If the user specifies a day of the week, include it in the 'time' field.\n" +
               "- Be EXTREMELY strict about splitting. CALL THE TOOLS.";
    }
}
