package com.viperai.assistant.bridge;

import com.rudra.myassistant.ConnectRepository;
import com.rudra.myassistant.ExpenseRepository;
import com.rudra.myassistant.ReminderRepository;
import com.rudra.myassistant.NoteRepository;
import java.util.ArrayList;
import java.util.List;

public class AssistantFeatureBridge {
    private final ConnectRepository connectRepository;
    private final ExpenseRepository expenseRepository;
    private final ReminderRepository reminderRepository;
    private final NoteRepository noteRepository;

    public AssistantFeatureBridge(ConnectRepository connectRepository,
                                  ExpenseRepository expenseRepository,
                                  ReminderRepository reminderRepository,
                                  NoteRepository noteRepository) {
        this.connectRepository = connectRepository;
        this.expenseRepository = expenseRepository;
        this.reminderRepository = reminderRepository;
        this.noteRepository = noteRepository;
    }

    public List<String> getNearbyFriends() {
        try {
            return connectRepository.getNearbyFriendsSummary();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<String> getPendingExpenses() {
        try {
            return expenseRepository.getPendingExpenseSummary();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<String> getTodayReminders() {
        try {
            return reminderRepository.getTodayReminderSummary();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<String> getRecentNotes() {
        try {
            return noteRepository.getRecentNotesSummary();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
