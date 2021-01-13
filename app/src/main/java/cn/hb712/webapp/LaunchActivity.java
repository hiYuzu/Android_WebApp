package cn.hb712.webapp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import cn.hb712.webapp.Utils.WebViewCookiesUtils;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class LaunchActivity extends Activity {
    private static final String TAG = LaunchActivity.class.getSimpleName();

    private static final int LauncherDelay = 3000;
    private TextView mMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_launch);

        View mContentView = findViewById(R.id.fullscreen_content);
        mMessage = findViewById(R.id.launch_message);
        TextView mVersion = findViewById(R.id.version_text);
        PackageInfo info;
        try {
            info = getPackageManager().getPackageInfo(getPackageName(), 0);

            String versionStr = info.versionName;
            if(!versionStr.isEmpty()) {
                mVersion.setText(String.format("Version: %s", versionStr));
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }



        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);


        doLoginAction();
    }

    private Handler mLoginHandle = new Handler();
    private Runnable mLoginAction = new Runnable() {
        @Override
        public void run() {
            String username = MainApplication.getInstance().getUsername();
            String password = MainApplication.getInstance().getPassword();

            if(username.isEmpty() || password.isEmpty()) {
                gotoLoginActivity();
                return;
            }

            login(username, password);

        }

    };

    private void login(final String username, final String password) {
        MainApplication.getInstance().getWebServiceClient().runLoginTask(new WebServiceClient.TaskHandler() {
            @Override
            public void onStart() {
                mMessage.setText("正在登录...");
            }

            @Override
            public void onSuccess(JSONObject obj) {
                Log.d(TAG, "Login onSuccess: " + obj.toString());
                try {
                    String displayUser = obj.getString("DisplayUsername");
                    WebViewCookiesUtils.saveCookie(MainApplication.baseUrl, "EnvDustUser", displayUser);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                gotoMainActivity(username, password);
            }

            @Override
            public void onFailed(String errMsg) {
                gotoLoginActivity();
            }
        },username, password);

    }

    private void gotoMainActivity(String username, String password) {
        Intent intent = new Intent(LaunchActivity.this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_USERNAME, username);
        intent.putExtra(MainActivity.EXTRA_PASSWORD, password);
        startActivity(intent);
        LaunchActivity.this.finish();
    }

    private void gotoLoginActivity() {
        Intent intent = new Intent(LaunchActivity.this, LoginActivity.class);
        startActivity(intent);
        LaunchActivity.this.finish();
    }


    private void doLoginAction() {
        mLoginHandle.removeCallbacks(mLoginAction);
        mLoginHandle.postDelayed(mLoginAction, LauncherDelay);
    }




}
