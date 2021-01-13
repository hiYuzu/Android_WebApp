package cn.hb712.webapp;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

/**
 * Created by xujun on 2018/2/5.
 */

public class WebViewFragment extends Fragment {

    public interface WebViewHandlerProvider{
        public void onReceivedError(int errorCode, String description, String failingUrl);
        public boolean onOverrideUrlLoading(String url);
    }

    private static final String TAG = WebViewFragment.class.getSimpleName();
    private WebView mWebView;
    private Bundle mPageInfo;
    private WebViewHandlerProvider mProvider;


    public static WebViewFragment newInstance(Bundle args) {
        WebViewFragment fragment = new WebViewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageInfo = getArguments();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Activity activity = getActivity();
        try{
            mProvider = (WebViewHandlerProvider)activity;
        }catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement WebViewHandlerProvider!");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_web_view, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mWebView = view.findViewById(R.id.fragment_web_view);
        mWebView.getSettings().setJavaScriptEnabled(true);
        //mWebView.getSettings().setDomStorageEnabled(true);
        //mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.setWebViewClient(new WebViewClient(){

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mProvider.onReceivedError(error.getErrorCode(), String.valueOf(error.getDescription()), request.getUrl().getPath());
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                mProvider.onReceivedError(errorCode, description, failingUrl);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String urlPath = request.getUrl().getPath();
                Log.d(TAG, "shouldOverrideUrlLoading: will load " + urlPath);
                return mProvider.onOverrideUrlLoading(urlPath) && super.shouldOverrideUrlLoading(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                String urlPath = Uri.parse(url).getPath();
                Log.d(TAG, "shouldOverrideUrlLoading: will load " + urlPath);
                return mProvider.onOverrideUrlLoading(urlPath) && super.shouldOverrideUrlLoading(view, url);
            }
        });

        if(mWebViewSavedState != null) {
            mWebView.restoreState(mWebViewSavedState);
        }else if(savedInstanceState != null) {
            mWebView.restoreState(savedInstanceState);
        }else {
            loadPage();
        }
    }

    Bundle mWebViewSavedState;

    @Override
    public void onResume() {
        super.onResume();
        mWebView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mWebView.onPause();
        mWebViewSavedState = new Bundle();
        mWebView.saveState(mWebViewSavedState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mWebView!= null) {
            mWebView.saveState(outState);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }
    private void loadPage() {
        String url = mPageInfo.getString("url");
        Log.d(TAG, "onStart: load url : " + url);
        mWebView.loadUrl(url );
    }

}
