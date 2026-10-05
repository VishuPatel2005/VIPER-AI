package com.rudra.myassistant;

import android.content.Context;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ExpenseRepository {
    private final FirebaseFirestore db;
    private final String currentUserId;

    public ExpenseRepository(Context context) {
        this.db = FirebaseFirestore.getInstance();
        this.currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
    }

    public List<String> getPendingExpenseSummary() {
        List<String> summary = new ArrayList<>();
        if (currentUserId == null) return summary;

        try {
            // Check balances where user owes or is owed
            QuerySnapshot balancesAsA = Tasks.await(db.collection("balances")
                    .whereEqualTo("uidA", currentUserId).get(), 10, TimeUnit.SECONDS);
            QuerySnapshot balancesAsB = Tasks.await(db.collection("balances")
                    .whereEqualTo("uidB", currentUserId).get(), 10, TimeUnit.SECONDS);

            processBalances(balancesAsA, "A", summary);
            processBalances(balancesAsB, "B", summary);

        } catch (Exception e) {
            android.util.Log.e("ExpenseRepo", "Error fetching expense summary", e);
        }
        return summary;
    }

    private void processBalances(QuerySnapshot snapshot, String myRole, List<String> summary) throws Exception {
        for (DocumentSnapshot doc : snapshot) {
            Double netAmount = doc.getDouble("netAmount");
            if (netAmount == null || Math.abs(netAmount) < 0.01) continue;

            String friendUid = myRole.equals("A") ? doc.getString("uidB") : doc.getString("uidA");
            DocumentSnapshot friendDoc = Tasks.await(db.collection("users").document(friendUid).get(), 5, TimeUnit.SECONDS);
            String friendName = friendDoc.getString("name");
            if (friendName == null) friendName = "Someone";

            // netAmount positive: uidA owes uidB
            // If I am A and netAmount > 0: I owe friend
            // If I am A and netAmount < 0: Friend owes me
            // If I am B and netAmount > 0: Friend owes me
            // If I am B and netAmount < 0: I owe friend

            boolean iOwe;
            if (myRole.equals("A")) {
                iOwe = netAmount > 0;
            } else {
                iOwe = netAmount < 0;
            }

            double absAmount = Math.abs(netAmount);
            if (iOwe) {
                summary.add("You owe " + friendName + " " + absAmount);
            } else {
                summary.add(friendName + " owes you " + absAmount);
            }
        }
    }

    public static class ExpenseNotificationDto {
        public String id, owedTo, amount;
        public ExpenseNotificationDto(String id, String owedTo, String amount) {
            this.id = id; this.owedTo = owedTo; this.amount = amount;
        }
    }

    public List<ExpenseNotificationDto> getPendingExpensesOlderThan(long ageMillis) {
        List<ExpenseNotificationDto> list = new ArrayList<>();
        if (currentUserId == null) return list;

        try {
            long threshold = System.currentTimeMillis() - ageMillis;
            QuerySnapshot snapshot = Tasks.await(db.collection("expenses")
                    .whereEqualTo("owedBy", currentUserId)
                    .whereEqualTo("status", "PENDING")
                    .get(), 10, TimeUnit.SECONDS);

            for (DocumentSnapshot doc : snapshot) {
                Long createdAt = doc.getLong("createdAt");
                if (createdAt != null && createdAt < threshold) {
                    list.add(new ExpenseNotificationDto(
                        doc.getId(),
                        doc.getString("owedToName"),
                        String.valueOf(doc.get("amount"))
                    ));
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ExpenseRepo", "Error fetching pending expenses", e);
        }
        return list;
    }
}
