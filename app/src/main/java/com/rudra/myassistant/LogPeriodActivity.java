package com.rudra.myassistant;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.UserSessionManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LogPeriodActivity extends BaseEdgeToEdgeActivity {

    private ImageView checkHadFlow, checkNoFlow, checkLight, checkMedium, checkHeavy;
    private boolean hadFlow = false, noFlow = false;
    private String flowLevel = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_period);

        TextView dateText = findViewById(R.id.current_date_text);
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault());
        dateText.setText("Today, " + sdf.format(new Date()));

        checkHadFlow = findViewById(R.id.check_had_flow);
        checkNoFlow = findViewById(R.id.check_no_flow);
        checkLight = findViewById(R.id.check_light);
        checkMedium = findViewById(R.id.check_medium);
        checkHeavy = findViewById(R.id.check_heavy);

        findViewById(R.id.item_had_flow).setOnClickListener(v -> selectFlowStatus(true));
        findViewById(R.id.item_no_flow).setOnClickListener(v -> selectFlowStatus(false));

        findViewById(R.id.item_light).setOnClickListener(v -> selectFlowLevel("Light"));
        findViewById(R.id.item_medium).setOnClickListener(v -> selectFlowLevel("Medium"));
        findViewById(R.id.item_heavy).setOnClickListener(v -> selectFlowLevel("Heavy"));

        findViewById(R.id.save_button).setOnClickListener(v -> {
            if (noFlow) {
                logFlow("None");
            } else if (hadFlow && !flowLevel.isEmpty()) {
                logFlow(flowLevel);
            } else {
                finish();
            }
        });
    }

    private void logFlow(String flow) {
        UserSessionManager sessionManager = new UserSessionManager(this);
        String userId = sessionManager.getUserId();
        FirestoreRepository repo = new FirestoreRepository();
        
        Map<String, Object> data = new HashMap<>();
        data.put("flow", flow);
        data.put("date", new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date()));
        
        repo.logPeriodData(userId, data).addOnSuccessListener(v -> {
            android.widget.Toast.makeText(this, flow + " flow logged", android.widget.Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void selectFlowStatus(boolean isHadFlow) {
        if (isHadFlow) {
            hadFlow = true;
            noFlow = false;
            checkHadFlow.setImageResource(R.drawable.ic_circle_filled);
            checkNoFlow.setImageResource(R.drawable.ic_circle_outline);
        } else {
            hadFlow = false;
            noFlow = true;
            flowLevel = "";
            checkHadFlow.setImageResource(R.drawable.ic_circle_outline);
            checkNoFlow.setImageResource(R.drawable.ic_circle_filled);
            updateFlowLevelUI();
        }
    }

    private void selectFlowLevel(String level) {
        hadFlow = true;
        noFlow = false;
        flowLevel = level;
        checkHadFlow.setImageResource(R.drawable.ic_circle_filled);
        checkNoFlow.setImageResource(R.drawable.ic_circle_outline);
        updateFlowLevelUI();
    }

    private void updateFlowLevelUI() {
        checkLight.setImageResource(flowLevel.equals("Light") ? R.drawable.ic_circle_filled : R.drawable.ic_circle_outline);
        checkMedium.setImageResource(flowLevel.equals("Medium") ? R.drawable.ic_circle_filled : R.drawable.ic_circle_outline);
        checkHeavy.setImageResource(flowLevel.equals("Heavy") ? R.drawable.ic_circle_filled : R.drawable.ic_circle_outline);
    }
}
