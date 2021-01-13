package cn.hb712.webapp;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class UpgradeDialog extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upgrade_dialog);

        final Intent intent = getIntent();
        String currentVersionStr = intent.getExtras().getString("currentVersion", "");
        TextView currentVersion = findViewById(R.id.current_version);
        currentVersion.setText(currentVersionStr);

        String newVersionStr = intent.getExtras().getString("newVersion", "");
        TextView newVersion = findViewById(R.id.new_version);
        newVersion.setText(newVersionStr);

        String newVersionDescStr = intent.getExtras().getString("newVersionDesc", "");
        TextView newVersionDesc = findViewById(R.id.new_version_desc);
        newVersionDesc.setText(newVersionDescStr);

        Button upgradeButton = findViewById(R.id.upgrade);
        upgradeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultData = new Intent();
                resultData.putExtra("downloadUrl",
                        intent.getExtras().getString("downloadUrl", ""));
                setResult(RESULT_OK, resultData);
                finish();

            }
        });

        Button cancelButton = findViewById(R.id.cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        FrameLayout layout = findViewById(R.id.upgrade_layout);

        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });


    }
}
