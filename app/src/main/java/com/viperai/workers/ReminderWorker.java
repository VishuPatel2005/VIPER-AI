package com.viperai.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.rudra.myassistant.ReminderRepository;
import com.rudra.myassistant.network.ReminderDto;
import com.viperai.notifications.NotificationHelper;
import java.util.List;

public class ReminderWorker extends Worker {
    private final ReminderRepository repository;

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        repository = new ReminderRepository(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        List<ReminderDto> reminders = repository.getRemindersForNotification();
        for (ReminderDto r : reminders) {
            NotificationHelper.showReminderNotification(getApplicationContext(), r.getId(), "Reminder", r.getText());
            repository.markReminderNotified(r.getId());
        }
        return Result.success();
    }
}
