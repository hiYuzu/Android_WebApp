package cn.hb712.webapp;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
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

/**
 * Created by xujun on 2018/2/7.
 */

public class WebServiceClient {

    private static final String TAG = WebServiceClient.class.getSimpleName();
    private static final String loginUrl = MainApplication.baseUrl + "/UserController/validateUser";
    private AbstractHttpClient mHttpClient;

    public interface TaskHandler {
        void onStart();
        void onSuccess(JSONObject obj);
        void onFailed(String errMsg);
    }

    AbstractHttpClient getHttpClient() {
        return mHttpClient;
    }

    WebServiceClient(){
        mHttpClient = new DefaultHttpClient();

    }

    void runLoginTask(TaskHandler handler, String username, String password) {
        (new LoginTask(this, handler)).execute(username, password);
    }

    void runGetMainViewTask(TaskHandler handler, String username, String password) {
        (new GetMobilePageTask(this, handler)).execute(username, password);
    }

    void runCheckUpdateTask(TaskHandler handler, String appId) {
        (new CheckUpdateTask(this, handler)).execute(appId);
    }

    public void runGetAlarmTask(TaskHandler handler, String alarmId) {
        (new GetAlarmTask(this, handler)).execute(alarmId);
    }

    private static final String RESULT = "result";
    private static final String SUCCESS_ENTITY = "entity";
    private static final String ERROR_MSG = "error_msg";

    private static abstract class BaseTask extends AsyncTask<String, String, Bundle> {
        private TaskHandler mHandler;
        private WebServiceClient mClient;

        static class TaskException extends Exception {
            TaskException(String message) {
                super(message);
            }
        }

        BaseTask(WebServiceClient serviceClient, TaskHandler handler) {
            super();
            mClient = serviceClient;
            mHandler = handler;
        }

        @Override
        protected void onPostExecute(Bundle bundle) {
            super.onPostExecute(bundle);
            boolean result = bundle.getBoolean(RESULT);
            if(result) {
                try {
                    JSONObject retObj = new JSONObject(bundle.getString(SUCCESS_ENTITY, ""));
                    mHandler.onSuccess(retObj);
                } catch (JSONException e) {
                    e.printStackTrace();
                    mHandler.onFailed("内部错误：1003");
                }
            } else {
                String errMsg = bundle.getString(ERROR_MSG, "");
                mHandler.onFailed(errMsg);
            }

        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        protected Bundle doInBackground(String... strings) {
            Bundle result = new Bundle();
            AbstractHttpClient hc = mClient.getHttpClient();

            try {
                HttpPost hp = new HttpPost(getActionUrl());
                HttpEntity entity = createRequestEntity(strings);
                Log.d(TAG, "BaseTask: Request Entity: " + EntityUtils.toString(entity) );
                hp.setEntity(entity);

                onPreRequest(hc, hp);
                HttpResponse hr = hc.execute(hp);
                onPostedRequest(hc, hp);

                int statusCode = hr.getStatusLine().getStatusCode();
                if(statusCode != HttpStatus.SC_OK) {
                    result.putBoolean(RESULT, true);
                    result.putString(ERROR_MSG,onResponseFailed(statusCode));
                } else {
                    HttpEntity resEntity = hr.getEntity();
                    String resultStr = EntityUtils.toString(resEntity);
                    Log.d(TAG, "BaseTask: Response Entity: " + resultStr );
                    result.putBoolean(RESULT, true);
                    result.putString(SUCCESS_ENTITY, onResponseSuccess(resultStr));
                }

            } catch (ClientProtocolException e) {
                e.printStackTrace();
                result.putBoolean(RESULT, false);
                result.putString(ERROR_MSG, "内部错误: 1001");
            } catch (IOException e) {
                e.printStackTrace();
                result.putBoolean(RESULT, false);
                result.putString(ERROR_MSG, "无法连接到服务器，请检查网络配置");
            } catch (TaskException e) {
                e.printStackTrace();
                Log.d(TAG, e.getMessage());
                result.putBoolean(RESULT, false);
                result.putString(ERROR_MSG, e.getMessage());
            }

            return result;
        }



        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mHandler.onStart();
        }

        protected abstract HttpEntity createRequestEntity(String... strings) throws TaskException;
        protected abstract String getActionUrl();

        protected void onPreRequest(AbstractHttpClient httpClient, HttpRequest request) throws TaskException {
        }

        protected void onPostedRequest(AbstractHttpClient httpClient, HttpRequest request) throws TaskException {

        }

        protected String onResponseSuccess(String resultStr) throws TaskException {
            return resultStr;
        }

        String onResponseFailed(int responseCode) throws TaskException {
            return "服务器响应异常 :" + String.valueOf(responseCode);
        }
    }

    private static class LoginTask extends BaseTask{

        LoginTask(WebServiceClient serviceClient, TaskHandler handler) {
            super(serviceClient, handler);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            cn.hb712.webapp.Utils.WebViewCookiesUtils.clearAllCookies();
        }

        @Override
        protected HttpEntity createRequestEntity(String... strings) throws TaskException {
            String username = strings[0];
            String password = strings[1];
            return Utils.buildLoginEntity(username, password);
        }

        @Override
        protected String getActionUrl() {
            return loginUrl;
        }

        @Override
        protected void onPreRequest(AbstractHttpClient httpClient, HttpRequest request) throws TaskException {
            super.onPreRequest(httpClient, request);
            httpClient.getCookieStore().clear();
        }

        @Override
        protected void onPostedRequest(AbstractHttpClient httpClient, HttpRequest request) throws TaskException {
            super.onPostedRequest(httpClient, request);
            cn.hb712.webapp.Utils.WebViewCookiesUtils.syncCookieFromHttpClient(MainApplication.baseUrl, httpClient);
        }

        @Override
        protected String onResponseSuccess(String resultStr) throws TaskException {
            super.onResponseSuccess(resultStr);
            try {
                JSONObject responseObj = new JSONObject(resultStr);

                if (!responseObj.getBoolean("result")) {
                    String errorMsg = responseObj.getString("detail");
                    throw new TaskException("认证失败:" + errorMsg);
                }

                String user = responseObj.getString("detail");
                JSONObject retObj = new JSONObject();
                retObj.put("DisplayUsername", user);
                return retObj.toString();
            } catch (JSONException e) {
                e.printStackTrace();
                throw new TaskException("解析认证返回信息失败");
            }
        }

    }


    private static class GetMobilePageTask extends BaseTask{
        private static final String actionUrl = MainApplication.baseUrl + "/MobilePageController/getMobilePage";

        GetMobilePageTask(WebServiceClient serviceClient, TaskHandler handler) {
            super(serviceClient,handler);
        }

        @Override
        protected HttpEntity createRequestEntity(String... strings) throws TaskException {
            String username = strings[0];
            String password = strings[1];
            return Utils.buildLoginEntity(username, password);
        }

        @Override
        protected String getActionUrl() {
            return actionUrl;
        }


    }

    private static class CheckUpdateTask extends BaseTask {
        private static final String actionUrl = MainApplication.baseUrl + "/SystemController/querySystem";

        CheckUpdateTask(WebServiceClient serviceClient, TaskHandler handler) {
            super(serviceClient, handler);
        }

        @Override
        protected HttpEntity createRequestEntity(String... strings) throws TaskException {
            String appId = strings[0];
            return Utils.buildCheckUpdateEntity(appId);
        }

        @Override
        protected String getActionUrl() {
            return actionUrl;
        }
    }

    private static class GetAlarmTask extends BaseTask{
        private static final String actionUrl = MainApplication.baseUrl + "/MobileController/getDeviceMobileAlarmWithinDay";

        GetAlarmTask(WebServiceClient serviceClient, TaskHandler handler) {
            super(serviceClient,handler);
        }

        @Override
        protected HttpEntity createRequestEntity(String... strings) throws TaskException {
            String alarmId = strings[0];
            return Utils.buildAlarmEntity(alarmId);
        }

        @Override
        protected String getActionUrl() {
            return actionUrl;
        }


    }

    static class Utils {
        private static HttpEntity buildLoginEntity(String username, String password) throws BaseTask.TaskException {
            List<NameValuePair> valuePairs = new ArrayList<>();
            valuePairs.add(new BasicNameValuePair("userCode", username));
            valuePairs.add(new BasicNameValuePair("userPassword", password));

            try{
                return new UrlEncodedFormEntity(valuePairs);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                throw new BaseTask.TaskException("内部错误：1000");
            }
        }
        private static HttpEntity buildCheckUpdateEntity(String appId) throws BaseTask.TaskException {
            List<NameValuePair> valuePairs = new ArrayList<>();
            valuePairs.add(new BasicNameValuePair("sysCode", appId));

            try {
                return new UrlEncodedFormEntity(valuePairs);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                throw new BaseTask.TaskException("内部错误：1000");
            }
        }
        private static HttpEntity buildAlarmEntity(String alarmId) throws BaseTask.TaskException {
            List<NameValuePair> valuePairs = new ArrayList<>();
            valuePairs.add(new BasicNameValuePair("alarmId", alarmId));

            try {
                return new UrlEncodedFormEntity(valuePairs);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                throw new BaseTask.TaskException("内部错误：1000");
            }
        }
    }
}
