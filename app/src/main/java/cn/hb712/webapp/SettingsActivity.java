package cn.hb712.webapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class SettingsActivity extends Activity {
    public static final String SETTINGS_OPERATION = "operation";
    public static final int SETTINGS_OPERATION_LOGOUT = 0;
    public static final int SETTINGS_OPERATION_CHECK_UPDATE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        FrameLayout layout = findViewById(R.id.settings_layout);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        Button logoutButton = findViewById(R.id.logout);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultData = new Intent();
                resultData.putExtra(SETTINGS_OPERATION, SETTINGS_OPERATION_LOGOUT);
                setResult(RESULT_OK, resultData);
                finish();

            }
        });

        Button checkUpdateButton = findViewById(R.id.update);
        checkUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultData = new Intent();
                resultData.putExtra(SETTINGS_OPERATION, SETTINGS_OPERATION_CHECK_UPDATE);
                setResult(RESULT_OK, resultData);
                finish();

            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        this.overridePendingTransition(R.anim.activity_close, 0);
    }
}
