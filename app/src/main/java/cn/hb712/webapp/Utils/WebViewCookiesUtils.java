package cn.hb712.webapp.Utils;

import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.AbstractHttpClient;

import java.util.List;

import cn.hb712.webapp.WebServiceClient;

/**
 * Created by xujun on 2018/2/7.
 */

public class WebViewCookiesUtils {
    private static final String TAG = WebViewCookiesUtils.class.getSimpleName();

    public static void saveCookie(String url, String key, String value) {
        CookieManager cookieManager = CookieManager.getInstance();
        String cookieStr = String.format("%s=%s", key, JavaScriptEscapeUtils.escape(value));
        Log.d(TAG, "saveCookie: save cookie \"" + cookieStr +"\"" );
        cookieManager.setCookie(url, cookieStr);
        cookieManager.flush();
    }


    public static void syncCookieFromHttpClient(String url, AbstractHttpClient hc) {
        Log.d(TAG, "syncCookieFromHttpClient: " + url);
        CookieManager cookieManager = CookieManager.getInstance() ;
        List<Cookie> cookies = hc.getCookieStore().getCookies();
        if(!cookies.isEmpty())
        {
            for (Cookie cookie : cookies) {
                String cookieStr = cookie.getName() + "=" + cookie.getValue();
                Log.d(TAG, "syncCookieFromHttpClient: save cookie \"" + cookieStr +"\"" );
                cookieManager.setCookie(url, cookieStr);
            }
        }
        cookieManager.flush();
    }

    public static void clearAllCookies() {
        CookieManager cookieManager = CookieManager.getInstance();
        Log.d(TAG, "clearAllCookies: ");
        cookieManager.removeAllCookies(new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean value) {
                Log.d(TAG, "Remove Cookie onReceiveValue: " + String.valueOf(value));
            }
        });
        cookieManager.flush();
    }

    public static String getWebViewCookies(String url) {
        CookieManager cookieManager = CookieManager.getInstance();
        return cookieManager.getCookie(url);
    }
}
