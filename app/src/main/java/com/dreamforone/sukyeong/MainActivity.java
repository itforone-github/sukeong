package com.dreamforone.sukyeong;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.Parcelable;
import android.os.StrictMode;
import android.provider.MediaStore;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URISyntaxException;

import util.BackPressCloseHandler;
import util.Common;
import util.LocationPosition;
import util.NetworkCheck;


public class MainActivity extends AppCompatActivity {


    private final int REQUEST_VIEWER=1000;
    LinearLayout webLayout;
    RelativeLayout networkLayout;
    WebView webView;
    NetworkCheck netCheck;
    Button replayBtn;
    ProgressBar loadingProgress;
    public static boolean execBoolean = true;
    private BackPressCloseHandler backPressCloseHandler;
    boolean isIndex = true;
    private Context mContext;
    private WebView mWebviewPop;
    FrameLayout mContainer;
    String firstUrl = "";
    ExpandableListView menuListView;

    // 파일첨부 관련 변수
    final int FILECHOOSER_NORMAL_REQ_CODE = 1200,FILECHOOSER_LOLLIPOP_REQ_CODE=1300;
    ValueCallback<Uri> filePathCallbackNormal;
    ValueCallback<Uri[]> filePathCallbackLollipop;
    Uri mCapturedImageURI;
    Activity mActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext=this;
        mActivity=this;
        mContainer=(FrameLayout)findViewById(R.id.mContainer);
        if(Build.VERSION.SDK_INT>=24){
            try{
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        setContentView(R.layout.activity_main);


        Intent intent = getIntent();
        firstUrl = getString(R.string.url);
        CookieSyncManager.createInstance(this);
        try{
            if(!intent.getExtras().getString("goUrl").equals("")){
                firstUrl =intent.getExtras().getString("goUrl");
            }
        }catch(Exception e){

        }
        //lat, lng 좌표처리 값 가져오기
        LocationPosition.act=MainActivity.this;
        LocationPosition.setPosition(MainActivity.this);
        setLayout();
    }


    //레이아웃 설정
    public void setLayout() {
        networkLayout = (RelativeLayout) findViewById(R.id.networkLayout);//네트워크 연결이 끊겼을 때 레이아웃 가져오기
        webLayout = (LinearLayout) findViewById(R.id.webLayout);//웹뷰 레이아웃 가져오기
        mContainer=(FrameLayout)findViewById(R.id.mContainer);
        loadingProgress = (ProgressBar)findViewById(R.id.loadingProgress);
        webView = (WebView) findViewById(R.id.webView);//웹뷰 가져오기
        webView.loadUrl(firstUrl);
        webViewSetting();
    }

    @RequiresApi(api = Build.VERSION_CODES.ECLAIR_MR1)
    public void webViewSetting() {
        WebSettings setting = webView.getSettings();//웹뷰 세팅용

        setting.setAllowFileAccess(true);//웹에서 파일 접근 여부
        setting.setAppCacheEnabled(true);//캐쉬 사용여부
        setting.setGeolocationEnabled(true);//위치 정보 사용여부
        setting.setDatabaseEnabled(true);//HTML5에서 db 사용여부
        setting.setDomStorageEnabled(true);//HTML5에서 DOM 사용여부
        setting.setCacheMode(WebSettings.LOAD_NO_CACHE);//캐시 사용모드 LOAD_NO_CACHE는 캐시를 사용않는다는 뜻
        setting.setJavaScriptEnabled(true);//자바스크립트 사용여부
        setting.setJavaScriptCanOpenWindowsAutomatically(true);
        setting.setSupportMultipleWindows(true);//윈도우 창 여러개를 사용할 것인지의 여부 무조건 false로 하는 게 좋음
        setting.setUseWideViewPort(true);//웹에서 view port 사용여부
        webView.setWebChromeClient(chrome);//웹에서 경고창이나 또는 컴펌창을 띄우기 위한 메서드
        webView.setWebViewClient(client);//웹페이지 관련된 메서드 페이지 이동할 때 또는 페이지가 로딩이 끝날 때 주로 쓰임
        setting.setUserAgentString("Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.84 Mobile Safari/537.36");
        if (Build.VERSION.SDK_INT >= 21) {
            setting.setMixedContentMode( WebSettings.MIXED_CONTENT_ALWAYS_ALLOW );
        }
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);

        webView.addJavascriptInterface(new WebJavascriptEvent(), "Android");
        webView.addJavascriptInterface(new WebJavascriptEvent(),"android");

        //네트워크 체킹을 할 때 쓰임
        netCheck = new NetworkCheck(this, this);
        netCheck.setNetworkLayout(networkLayout);
        netCheck.setWebLayout(webLayout);
        netCheck.networkCheck();
        //뒤로가기 버튼을 눌렀을 때 클래스로 제어함
        backPressCloseHandler = new BackPressCloseHandler(this);

        replayBtn=(Button)findViewById(R.id.replayBtn);
        replayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                netCheck.networkCheck();
            }
        });
        if (Build.VERSION.SDK_INT >= 21) {
            setting.setMixedContentMode( WebSettings.MIXED_CONTENT_ALWAYS_ALLOW );
        }

    }

    WebChromeClient chrome;
    {
        chrome = new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                mWebviewPop = new WebView(mContext);
                mWebviewPop.setVerticalScrollBarEnabled(false);
                mWebviewPop.setHorizontalScrollBarEnabled(false);

                mWebviewPop.setWebChromeClient(chrome);
                mWebviewPop.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
                mWebviewPop.getSettings().setJavaScriptEnabled(true);
                mWebviewPop.getSettings().setSavePassword(false);
                mWebviewPop.clearHistory();
                mWebviewPop.clearFormData();
                mWebviewPop.clearCache(true);

                mWebviewPop.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 4.1.1; Galaxy Nexus Build/JRO03C) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Mobile Safari/535.19");
                mWebviewPop.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                mContainer.addView(mWebviewPop);
                mContainer.setVisibility(View.VISIBLE);

                mWebviewPop.setWebViewClient(new WebViewClient(){
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        Log.i("TAG", "onCreateWindow shouldOverrideUrlLoading " + url);

                        if ( view == null || url == null) {
                            // 처리하지 못함
                            return false;
                        }
                        String host=Uri.parse(url).getHost();
                        if( url.startsWith("http://") || url.startsWith("https://") ) {


                            if (host.equals("www.moyeopet.com")) {
                                if (mWebviewPop != null) {
                                    mWebviewPop.setVisibility(View.GONE);
                                    mContainer.removeView(mWebviewPop);
                                    mWebviewPop = null;
                                }
                                return false;
                            }
                            if (host.equals("m.facebook.com") || host.equals("www.facebook.com") || host.equals("facebook.com")) {

                                return false;
                            }
                        }
                        if (url.startsWith("tel:")) {
                            Intent call_phone = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            startActivity(call_phone);
                            return true;

                        } else if (url.startsWith("sms:")) {
                            Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                            startActivity(i);
                            return true;
                        }

                        if ( url.contains("play.google.com") ) {
                            // play.google.com 도메인이면서 App 링크인 경우에는 market:// 로 변경
                            String[] params = url.split("details");
                            if ( params.length > 1 ) {
                                url = "market://details" + params[1];
                                view.getContext().startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url) ));
                                return true;
                            }
                        }

                        if ( url.startsWith("http:") || url.startsWith("https:") ) {
                            // HTTP/HTTPS 요청은 내부에서 처리한다.
                            view.loadUrl(url);
                        } else {
                            Intent intent;

                            try {
                                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                            } catch (URISyntaxException e) {
                                // 처리하지 못함
                                return false;
                            }

                            try {
                                view.getContext().startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                // Intent Scheme인 경우, 앱이 설치되어 있지 않으면 Market으로 연결
                                if ( url.startsWith("intent:") && intent.getPackage() != null) {
                                    url = "market://details?id=" + intent.getPackage();
                                    view.getContext().startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url) ));
                                    return true;
                                } else {
                                    // 처리하지 못함
                                    return false;
                                }
                            }
                        }
                        return true;
                    }

                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        Log.i("TAG", "url " + url);
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);

                    }
                });
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(mWebviewPop);
                resultMsg.sendToTarget();

                return true;


            }
            @Override
            public void onCloseWindow(WebView window) {
                mContainer.removeView(mWebviewPop);
                mContainer.setVisibility(View.GONE);
            }

            //경고창 띄우기
            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("\n" + message + "\n")
                        .setPositiveButton("확인",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        result.confirm();
                                    }
                                }).create().show();
                return true;
            }

            //컴펌 띄우기
            @Override
            public boolean onJsConfirm(WebView view, String url, String message,
                                       final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("\n" + message + "\n")
                        .setPositiveButton("확인",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        result.confirm();
                                    }
                                })
                        .setNegativeButton("취소",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        result.cancel();
                                    }
                                }).create().show();
                return true;
            }

            //현재 위치 정보 사용여부 묻기
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                // Should implement this function.
                final String myOrigin = origin;
                final GeolocationPermissions.Callback myCallback = callback;
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Request message");
                builder.setMessage("Allow current location?");
                builder.setPositiveButton("Allow", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        myCallback.invoke(myOrigin, true, false);
                    }

                });
                builder.setNegativeButton("Decline", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        myCallback.invoke(myOrigin, false, false);
                    }

                });
                AlertDialog alert = builder.create();
                alert.show();
            }

            // For Android < 3.0
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                openFileChooser(uploadMsg, "");
            }

            // For Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
                filePathCallbackNormal = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_NORMAL_REQ_CODE);
            }

            // For Android 4.1+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                openFileChooser(uploadMsg, acceptType);
            }

            // For Android 5.0+
            public boolean onShowFileChooser(
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {
                if (filePathCallbackLollipop != null) {
//                    filePathCallbackLollipop.onReceiveValue(null);
                    filePathCallbackLollipop = null;
                }
                filePathCallbackLollipop = filePathCallback;


                // Create AndroidExampleFolder at sdcard
                File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AndroidExampleFolder");
                if (!imageStorageDir.exists()) {
                    // Create AndroidExampleFolder at sdcard
                    imageStorageDir.mkdirs();
                }

                // Create camera captured image file path and name
                File file = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
                mCapturedImageURI = Uri.fromFile(file);

                Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);

                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");

                // Create file chooser intent
                Intent chooserIntent = Intent.createChooser(i, "Image Chooser");
                // Set camera intent to file chooser
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{captureIntent});

                // On select image call onActivityResult method of activity
                startActivityForResult(chooserIntent, FILECHOOSER_LOLLIPOP_REQ_CODE);
                return true;
            }
        };
    }



    WebViewClient client;

    {
        client = new WebViewClient() {
            //페이지 로딩중일 때 (마시멜로) 6.0 이후에는 쓰지 않음
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                loadingProgress.setVisibility(View.VISIBLE);
                Log.d("url",url);
                String host=Uri.parse(url).getHost();
                if( url.startsWith("http://") || url.startsWith("https://") ) {


                    if (host.equals("www.dreamforone.com")) {
                        if (mWebviewPop != null) {
                            mWebviewPop.setVisibility(View.GONE);
                            mContainer.removeView(mWebviewPop);
                            mWebviewPop = null;
                        }
                        return false;
                    }
                    if (host.equals("m.facebook.com") || host.equals("www.facebook.com") || host.equals("facebook.com")) {

                        return false;
                    }
                    if(url.startsWith("https://open")){
                        loadingProgress.setVisibility(View.GONE);
                        Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY|Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        startActivity(intent);
                        return true;
                    }else if(url.startsWith("http://pf.kakao.com/")){
                        loadingProgress.setVisibility(View.GONE);
                        Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY|Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        startActivity(intent);
                        return true;
                    }else if(url.startsWith("https://open")){
                        loadingProgress.setVisibility(View.GONE);
                        Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY|Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        startActivity(intent);
                        return true;
                    }


                    return true;
                }else if (url.startsWith("tel")) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse(url));
                    loadingProgress.setVisibility(View.GONE);
                    try {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.


                        }
                        startActivity(intent);
                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }



                }else if(url.startsWith("market://")){
                    try {
                        loadingProgress.setVisibility(View.GONE);
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=kr.foryou.ssum"));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        startActivity(intent);
                        return true;
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else if (url.startsWith("intent:")) {
                    loadingProgress.setVisibility(View.GONE);
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        Intent existPackage = getPackageManager().getLaunchIntentForPackage(intent.getPackage());
                        if (existPackage != null) {
                            getBaseContext().startActivity(intent);
                        } else {
                            Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                            marketIntent.setData(Uri.parse("market://details?id=" + intent.getPackage()));
                            startActivity(marketIntent);
                        }
                        return true;
                    } catch (Exception e) {
                        Log.d("error1",e.toString());
                        e.printStackTrace();
                    }
                }


                return true;
            }
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                Log.d("onReceivedSslError", "onReceivedSslError");
                //super.onReceivedSslError(view, handler, error);
            }
            //페이지 로딩이 다 끝났을 때
            @Override
            public void onPageFinished(WebView view, String url) {


                //webLayout.setRefreshing(false);
                loadingProgress.setVisibility(View.GONE);

                if(url.startsWith("https://m.facebook.com/v3.1/dialog/oauth")){
                    if(mWebviewPop!=null)
                    {
                        mContainer.setVisibility(View.GONE);
                        mWebviewPop.setVisibility(View.GONE);
                        mWebviewPop=null;
                    }
                    view.loadUrl(getString(R.string.domain));
                    return;
                }else{
                    webView.loadUrl("javascript:setLatlng('"+LocationPosition.lat+"','"+LocationPosition.lng+"')");
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    CookieSyncManager.getInstance().sync();
                } else {
                    CookieManager.getInstance().flush();
                }

                Log.d("lat", ""+LocationPosition.lat);
                Log.d("url",url);
                Log.d("ss_mb_id", Common.getPref(getApplicationContext(),"ss_mb_id",""));

                //로그인할 때
                if(url.startsWith(getString(R.string.domain)+"bbs/login.php")){
                    view.loadUrl("javascript:fcmKey('"+ Common.TOKEN+"')");
                }
                if (url.equals(getString(R.string.url)) || url.equals(getString(R.string.domain))) {
                    isIndex=true;
                } else {
                    isIndex=false;
                }
                super.onPageFinished(view, url);
            }
            //페이지 오류가 났을 때 6.0 이후에는 쓰이지 않음
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    //쿠키 값 삭제
    public void deleteCookie(){
        CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(webView.getContext());
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.removeSessionCookie();
        cookieManager.removeAllCookie();
        cookieSyncManager.sync();
    }
    //다시 들어왔을 때
    @Override
    protected void onResume() {
        super.onResume();
        webView.resumeTimers();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.getInstance().startSync();
        }
        execBoolean=true;
        try{
            Intent intent=getIntent();
            Uri data=intent.getData();
            Log.d("data111",data.toString());
        }catch (Exception e){

        }
        Log.d("newtork","onResume");
        //netCheck.networkCheck();
    }
    //홈버튼 눌러서 바탕화면 나갔을 때
    @Override
    protected void onPause() {
        super.onPause();
        webView.pauseTimers();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.getInstance().stopSync();
        }
        //firstUrl=webView.getUrl();

        execBoolean=false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        netCheck.stopReciver();
    }

    //뒤로가기를 눌렀을 때
    public void onBackPressed() {
        //super.onBackPressed();

        if(mContainer.getVisibility()==View.VISIBLE){
            mContainer.setVisibility(View.GONE);
        }else {
            if (!isIndex) {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else if (webView.canGoBack() == false) {
                    backPressCloseHandler.onBackPressed();
                }
            } else {
                backPressCloseHandler.onBackPressed();
            }
        }

    }
    //드로우 메뉴 실행하기
    class WebJavascriptEvent{

        @JavascriptInterface
        public void login(String mb_id,String mb_name){
            Common.savePref(MainActivity.this,"mb_id",mb_id);
            Common.savePref(MainActivity.this,"mb_name",mb_name);
        }
        @JavascriptInterface
        public void logout(){
            Common.savePref(MainActivity.this,"mb_id","");
            Common.savePref(MainActivity.this,"mb_name","");
        }

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILECHOOSER_NORMAL_REQ_CODE) {
            if (filePathCallbackNormal == null) return;
            Uri result = (data == null || resultCode != RESULT_OK) ? null : data.getData();
            filePathCallbackNormal.onReceiveValue(result);
            filePathCallbackNormal = null;

        } else if (requestCode == FILECHOOSER_LOLLIPOP_REQ_CODE) {
            Uri[] result = new Uri[0];
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (resultCode == RESULT_OK) {
                    result = (data == null) ? new Uri[]{mCapturedImageURI} : WebChromeClient.FileChooserParams.parseResult(resultCode, data);
                }

                filePathCallbackLollipop.onReceiveValue(result);

            }
        }
    }



}
