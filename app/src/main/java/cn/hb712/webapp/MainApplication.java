package cn.hb712.webapp;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by xujun on 2018/1/25.
 */

public class MainApplication extends Application {
    private static final String TAG = MainApplication.class.getSimpleName();
    public static final String appIP = "114.115.217.241"; //"172.17.80.219";
    public static final String appPort = "";//"";//"8080";
    public static final String appAddress = appIP + (appPort.isEmpty()? "" : ":" + appPort);
    public static final String baseUrl = "http://" + appAddress;


    private static final String SETTINGS_USERNAME = "staff_id";
    private static final String SETTINGS_PASSWORD = "passwd";
    private WebServiceClient mWebServiceClient;

    public static MainApplication getInstance(){
        return singleton;
    }

    private static MainApplication singleton;

    SharedPreferences mSettings;

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
        mSettings = this.getSharedPreferences(MainApplication.appAddress, 0);
        mWebServiceClient = new WebServiceClient();
    }

    public void saveUsername(String username) {
        Log.d(TAG, "saveUsername: " + username);
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString(MainApplication.SETTINGS_USERNAME, username);
        editor.apply();
    }

    public void savePassword(String password){
        Log.d(TAG, "savePassword: " + password);
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString(MainApplication.SETTINGS_PASSWORD, password);
        editor.apply();
    }

    public void removePassword() {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.remove(MainApplication.SETTINGS_PASSWORD);
        editor.apply();
    }

    public String getUsername() {
        return mSettings.getString(SETTINGS_USERNAME, "");
    }

    public String getPassword() {
        return mSettings.getString(SETTINGS_PASSWORD, "");
    }

    public WebServiceClient getWebServiceClient() {
        return mWebServiceClient;
    }
}
