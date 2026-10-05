package com.rudra.myassistant;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.UserSessionManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LogSpottingActivity extends BaseEdgeToEdgeActivity {

    private boolean hadSpotting = false;
    private ImageView checkHadSpotting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_spotting);

        TextView dateText = findViewById(R.id.current_date_text);
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault());
        dateText.setText("Today, " + sdf.format(new Date()));

        checkHadSpotting = findViewById(R.id.check_had_spotting);

        findViewById(R.id.item_had_spotting).setOnClickListener(v -> {
            hadSpotting = !hadSpotting;
            checkHadSpotting.setImageResource(hadSpotting ? R.drawable.ic_circle_filled : R.drawable.ic_circle_outline);
        });

        findViewById(R.id.save_button).setOnClickListener(v -> logSpotting(hadSpotting));
    }

    private void logSpotting(boolean hadSpotting) {
        UserSessionManager sessionManager = new UserSessionManager(this);
        String userId = sessionManager.getUserId();
        FirestoreRepository repo = new FirestoreRepository();
        
        Map<String, Object> data = new HashMap<>();
        data.put("spotting", hadSpotting);
        data.put("date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        
        repo.logPeriodData(userId, data).addOnSuccessListener(v -> {
            android.widget.Toast.makeText(this, "Spotting logged", android.widget.Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
