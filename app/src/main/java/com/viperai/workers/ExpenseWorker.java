package com.viperai.workers;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.rudra.myassistant.ExpenseRepository;
import com.viperai.notifications.NotificationConstants;
import com.viperai.notifications.NotificationHelper;
import java.util.List;

public class ExpenseWorker extends Worker {
    private final ExpenseRepository repository;

    public ExpenseWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.repository = new ExpenseRepository(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences notifPrefs = getApplicationContext().getSharedPreferences(NotificationConstants.PREFS_NAME, Context.MODE_PRIVATE);
        if (!notifPrefs.getBoolean(NotificationConstants.PREF_NOTIF_EXPENSES, true)) {
            return Result.success();
        }

        // Fetch expenses older than 24 hours (86,400,000 ms)
        List<ExpenseRepository.ExpenseNotificationDto> expenses = repository.getPendingExpensesOlderThan(86400000);
        for (ExpenseRepository.ExpenseNotificationDto e : expenses) {
            // showExpenseNotification handles the 24h per-expense throttle internally via SharedPreferences
            NotificationHelper.showExpenseNotification(getApplicationContext(), e.id, e.owedTo, e.amount);
        }

        return Result.success();
    }
}
