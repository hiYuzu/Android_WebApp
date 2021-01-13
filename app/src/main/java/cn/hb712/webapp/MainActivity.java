package cn.hb712.webapp;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import cn.hb712.webapp.Utils.WebViewCookiesUtils;
import cn.hb712.webapp.page.NoScrollViewPager;
import cn.hb712.webapp.service.NotificationService;

public class MainActivity extends AppCompatActivity implements WebViewFragment.WebViewHandlerProvider {
    public static final String EXTRA_USERNAME = "staff_id";
    public static final String EXTRA_PASSWORD = "passwd";

    private static final int REQUEST_SETTINGS = 0;
    private static final int REQUEST_UPGRADE = 1;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String BaseUrl = MainApplication.baseUrl;
    private static final String mainPath  = "/UserController/toMobileMain";
    private static final String mainUrl  = BaseUrl + mainPath;
    private static final String loginPath  = "/loginPhone.html";
    private static final String loginUrl  = BaseUrl + loginPath;

    private boolean mExitFlag = false;
    private static final long ExitDelay = 2000;

    private ActionBar mActionBar;

    private MainPagerAdapter mPagerAdapter;
    private TabLayout mTabLayout;

    BroadcastReceiver mUpgradeCompleteReceiver ;
    private long mUpgradeDownloadReferenceId;
    private DownloadManager mDownloadManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActionBar = getSupportActionBar();

        assert mActionBar != null;
        mActionBar.setHomeButtonEnabled(false);

        mTabLayout = findViewById(R.id.tabs);
        NoScrollViewPager mViewPager = findViewById(R.id.fragment_container);

        FragmentManager mFragmentManager = getSupportFragmentManager();

        mPagerAdapter = new MainPagerAdapter(mFragmentManager);

        mViewPager.setAdapter(mPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager, true);

        mDownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        mUpgradeCompleteReceiver  = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if(referenceId != mUpgradeDownloadReferenceId) {
                    return;
                }

                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(referenceId);

                Cursor c = mDownloadManager.query(query);

                if(c.moveToFirst()) {
                    int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    if(DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)){
                        String uriString = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
                        Log.d(TAG, "Download App Complete: " + uriString);

                        installNewApk(Uri.parse("file://" + uriString));
                    }
                }


            }
        };

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(mUpgradeCompleteReceiver, filter);

        Intent params = getIntent();
        String username = params.getExtras().getString(MainActivity.EXTRA_USERNAME);
        String password = params.getExtras().getString(MainActivity.EXTRA_PASSWORD);

        loadMainView(username, password);

        onCheckUpdate(false);

        // 启动状态栏通知Service
        Intent intent = new Intent(this, NotificationService.class);
        intent.setAction("tcb.SERVICE");
        startService(intent);
    }

    private void installNewApk(Uri data) {
        Intent installIntent =  new Intent(Intent.ACTION_VIEW)
                .setDataAndType(data, "application/vnd.android.package-archive");
        installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(installIntent);
    }

    private  void onCheckUpdate(final boolean manualCheck) {
        PackageInfo info;
        String appid;
        try{
            info = getPackageManager().getPackageInfo(getPackageName(), 0);
            appid = info.packageName;
            Log.d(TAG, "checkUpdate: " + appid);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return;
        }

        MainApplication.getInstance().getWebServiceClient().runCheckUpdateTask(new WebServiceClient.TaskHandler() {
            @Override
            public void onStart() {

            }

            @Override
            public void onSuccess(JSONObject obj) {
                Log.d(TAG, "CheckUpdate onSuccess: " + obj.toString());

                PackageInfo info;
                try{
                    info = getPackageManager().getPackageInfo(getPackageName(), 0);
                    int versionCode  = info.versionCode;
                    String versionName = info.versionName;
                    Log.d(TAG, "checkUpdate: CurrentVersionCode" + String.valueOf(versionCode));
                    int newVersionCode = obj.getInt("sysNumber");
                    String newVersionName = obj.getString("sysVersion");
                    String newVersionDesc = obj.getString("sysMemo");
                    String downloadUrl = obj.getString("sysUrl");

                    if(newVersionCode > versionCode)
                    {
                        showUpgradeDialog(versionName, newVersionName, newVersionDesc, downloadUrl);
                    }
                    else
                    {
                        if(manualCheck) {
                            Toast.makeText(MainActivity.this, "已经是最新版本", Toast.LENGTH_SHORT).show();
                        }
                    }

                } catch (PackageManager.NameNotFoundException | JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "升级检查发生错误，请稍后再试", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailed(String errMsg) {
                if(manualCheck) {
                    Toast.makeText(MainActivity.this, "升级检查发生错误，请稍后再试", Toast.LENGTH_SHORT).show();
                }
            }
        }, appid);
    }

    private void showUpgradeDialog(String versionName, String newVersionName, String newVersionDesc, String downloadUrl) {
        Intent updateDialogIntent = new Intent(MainActivity.this, UpgradeDialog.class);
        updateDialogIntent.putExtra("currentVersion", versionName);
        updateDialogIntent.putExtra("newVersion", newVersionName);
        updateDialogIntent.putExtra("newVersionDesc", newVersionDesc);
        updateDialogIntent.putExtra("downloadUrl", downloadUrl);
        startActivityForResult(updateDialogIntent, REQUEST_UPGRADE);
    }

    private  void loadMainView(final String username, final String password){
        MainApplication.getInstance().getWebServiceClient().runGetMainViewTask(new WebServiceClient.TaskHandler() {
            @Override
            public void onStart() {

            }

            @Override
            public void onSuccess(JSONObject obj) {
                Log.d(TAG, "loadMainView onSuccess: " + obj.toString());
                loadMainView(obj);
            }

            @Override
            public void onFailed(String errMsg) {

            }
        }, username, password);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        int groupId = 0;

        int menuItemOrder = Menu.NONE;
        int menuItemText = R.string.settings;

        MenuItem menuItem = menu.add(groupId, REQUEST_SETTINGS, menuItemOrder, menuItemText);
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuItem.setIcon(R.drawable.action_settings);
        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivityForResult(intent, REQUEST_SETTINGS);
                return true;
            }
        });

        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case REQUEST_SETTINGS:
                onSettingsActivityResult(resultCode, data);
                break;
            case REQUEST_UPGRADE:
                onUpgradeActivityResult(resultCode, data);
            default:
                break;
        }

    }

    private void onUpgradeActivityResult(int resultCode, Intent data) {
        if(resultCode != RESULT_OK)
        {
            return;
        }

        String downloadUrl = data.getExtras().getString("downloadUrl","");
        if(downloadUrl.isEmpty())
        {
            return;
        }


        Uri uri = Uri.parse(downloadUrl);

        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.addRequestHeader("Cookie", WebViewCookiesUtils.getWebViewCookies(MainApplication.baseUrl));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION);
        //request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);


        mUpgradeDownloadReferenceId = mDownloadManager.enqueue(request);

    }

    private void onSettingsActivityResult(int resultCode, Intent data) {
        if(resultCode != RESULT_OK)
        {
            return;
        }

        Bundle extras = data.getExtras();

        int operationCode = -1;
        if (extras != null) {
            operationCode = extras.getInt(SettingsActivity.SETTINGS_OPERATION, -1);
        }

        switch (operationCode)
        {
            case SettingsActivity.SETTINGS_OPERATION_LOGOUT:
                onLogout();
                break;
            case SettingsActivity.SETTINGS_OPERATION_CHECK_UPDATE:
                onCheckUpdate(true);
                break;
            default:
                break;
        }
    }

    private void onLogout() {
        gotoLoginActivity();
    }


    private void gotoLoginActivity() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
            if (mExitFlag) {
                finish();
            } else {
                mExitFlag = true;
                resetExitFlag();
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
}

    private Handler mResetExitHandler = new Handler();
    private Runnable mResetExitAction = new Runnable() {
        @Override
        public void run() {
            mExitFlag = false;
        }
    };

    private void resetExitFlag() {
        Toast.makeText(this, "再按一次退出", (int) ExitDelay).show();
        mResetExitHandler.removeCallbacks(mResetExitAction);
        mResetExitHandler.postDelayed(mResetExitAction, ExitDelay);
    }



    private Drawable loadImageFromWeb(String icon_url) {
        @SuppressLint("StaticFieldLeak")
        AsyncTask<String, Void, Drawable> loadImageFromWebTask = new AsyncTask<String, Void, Drawable>() {
            @Override
            protected Drawable doInBackground(String... strings) {
                String icon_url = strings[0];
                try {
                    InputStream is = (InputStream) new URL(icon_url).getContent();
                    return Drawable.createFromStream(is, "src name");
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }.execute(icon_url);

        try {
            return loadImageFromWebTask.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }

    }



    private  class MainPagerAdapter extends FragmentPagerAdapter{

        List<Bundle> mPageInfos;

        MainPagerAdapter(FragmentManager fm) {
            super(fm);
            mPageInfos = new ArrayList<>();
        }


        void clear() {
            mPageInfos.clear();
        }

        void addItem(Bundle pageInfo) {
            mPageInfos.add(pageInfo);
        }

        @Override
        public int getCount() {
            return mPageInfos.size();
        }


        @Override
        public Fragment getItem(int position) {
            Bundle pageInfo = mPageInfos.get(position);
            return WebViewFragment.newInstance(pageInfo);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mPageInfos.get(position).getString("name");
        }
    }



        @Override
        public void onReceivedError(int errorCode, String description, String failingUrl) {
            String msg = String.format("Oh no! load %s Failed : %s", failingUrl, description);
            Log.d(TAG, "onReceivedError: " + msg);
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
        }

    @Override
    public boolean onOverrideUrlLoading(String url) {
        String urlPath = Uri.parse(url).getPath();
        if(urlPath.compareTo(loginPath) == 0)
        {
            gotoLoginActivity();
            return false;
        }
        return true;
    }


    private void loadMainView(JSONObject viewJson) {
        try {
            String title = viewJson.getString("title");

            String icon_url =  BaseUrl + viewJson.getString("logoUrl");

            setTitle(title, icon_url);

            JSONArray sub_pages = viewJson.getJSONArray("rows");
            mPagerAdapter.clear();
            for (int i = 0; i < sub_pages.length(); i++) {
                JSONObject sub_page = sub_pages.getJSONObject(i);
                String sub_page_name = sub_page.getString("mobileName");
                String sub_page_url = sub_page.getString("mobileUrl");
                Bundle pageInfo = new Bundle();
                pageInfo.putString("name", sub_page_name);
                pageInfo.putString("url", BaseUrl+sub_page_url);
                mPagerAdapter.addItem(pageInfo);
            }
            mPagerAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void setTitle(String title, String icon_url) {
        mActionBar.setTitle(title);
        int height = mActionBar.getHeight();
        if(!icon_url.isEmpty()){
            Drawable logo = loadImageFromWeb(icon_url);
            if(logo != null) {

                Drawable showLogo = zoomDrawableForHeight(logo, height);

                mActionBar.setDisplayShowHomeEnabled(true);
                mActionBar.setIcon(showLogo);
            }
        }
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        // 取 drawable 的长宽
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();

        // 取 drawable 的颜色格式
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                : Bitmap.Config.RGB_565;
        // 建立对应 bitmap
        Bitmap bitmap = Bitmap.createBitmap(w, h, config);
        // 建立对应 bitmap 的画布
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        // 把 drawable 内容画到画布中
        drawable.draw(canvas);
        return bitmap;
    }

    public static Drawable zoomDrawable(Drawable drawable, int w, int h) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        // drawable转换成bitmap
        Bitmap oldbmp = drawableToBitmap(drawable);
        // 创建操作图片用的Matrix对象
        Matrix matrix = new Matrix();
        // 计算缩放比例
        float sx = ((float) w / width);
        float sy = ((float) h / height);
        // 设置缩放比例
        matrix.postScale(sx, sy);
        // 建立新的bitmap，其内容是对原bitmap的缩放后的图
        Bitmap newbmp = Bitmap.createBitmap(oldbmp, 0, 0, width, height,
                matrix, true);
        return new BitmapDrawable(newbmp);
    }

    public static Drawable zoomDrawableForHeight(Drawable drawable, int h) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        // drawable转换成bitmap
        Bitmap oldbmp = drawableToBitmap(drawable);
        // 创建操作图片用的Matrix对象
        Matrix matrix = new Matrix();
        // 计算缩放比例
        float sy = ((float) h / height);
        // 设置缩放比例
        matrix.postScale(sy, sy);
        // 建立新的bitmap，其内容是对原bitmap的缩放后的图
        Bitmap newbmp = Bitmap.createBitmap(oldbmp, 0, 0, width, height,
                matrix, true);
        return new BitmapDrawable(newbmp);
    }
}
