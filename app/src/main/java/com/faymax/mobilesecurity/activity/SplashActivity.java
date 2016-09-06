package com.faymax.mobilesecurity.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.faymax.mobilesecurity.R;
import com.faymax.mobilesecurity.utils.StreamUtil;
import com.faymax.mobilesecurity.utils.ToastUtil;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "Splash";
    private static final int UPDATE_VERSION = 100;
    private static final int ENTER_HOME = 101;
    private static final int URL_ERROR = 102;
    private static final int IO_ERROR = 103;
    private static final int JSON_ERROR = 104;
    private TextView textView;
    private int mLocalVersionCode;
    private String mUrl;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_VERSION:
                    //弹出对话框，提示用户更新
                    showUpdateDialog();
                    break;
                case ENTER_HOME:
                    //进入程序主界面
                    enterHome();
                    break;
                case URL_ERROR:
                    ToastUtil.show(getApplicationContext(), "url异常");
                    enterHome();
                    break;
                case IO_ERROR:
                    ToastUtil.show(getApplicationContext(), "读取异常");
                    enterHome();
                    break;
                case JSON_ERROR:
                    ToastUtil.show(getApplicationContext(), "json解析异常");
                    enterHome();
                    break;
            }
        }
    };
    private String mVersionDes;
    private String mDownloadUrl;

    private void showUpdateDialog() {
        //对话框依赖于activity存在
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setTitle("升级提醒");
        builder.setMessage(mVersionDes);
        builder.setPositiveButton("立即更新", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //下载apk
                downloadApk();
            }
        });
        builder.setNegativeButton("稍后再说", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //取消对话框
                enterHome();
            }
        });
        AlertDialog dialog = builder.show();
    }

    private void downloadApk() {
        //apk下载地址，放置路径
        //判断sd卡是否可用
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // 获取sd卡路径
            String path = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + File.separator + "mobilesecurity.apk";
            //发送请求，获取apk，放置到指定位置
            HttpUtils httpUtils = new HttpUtils();
            //发送请求，传递参数
            httpUtils.download(mDownloadUrl, path, new RequestCallBack<File>() {
                @Override
                public void onSuccess(ResponseInfo<File> responseInfo) {
                    File file = responseInfo.result;
                    Log.d(TAG, "下载成功");
                }

                @Override
                public void onFailure(HttpException e, String s) {
                    Log.d(TAG, "下载失败");
                }

                @Override
                public void onStart() {
                    super.onStart();
                }

                @Override
                public void onLoading(long total, long current, boolean isUploading) {
                    super.onLoading(total, current, isUploading);
                    Log.d(TAG, "开始下载");
                    Log.d(TAG, "total + " + total);
                    Log.d(TAG, "current + " + current);
                }
            });

        }
    }

    /**
     * 进入应用程序主界面
     */
    private void enterHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        //开启新的界面后，将导航界面关闭
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //初始化View
        initView();
        //初始化数据
        initData();
    }

    /**
     * 初始化UI方法
     */
    private void initView() {
        textView = (TextView) findViewById(R.id.id_text_version);
    }

    /**
     * 获取数据方法
     */
    private void initData() {
        textView.setText("版本名称 " + getVersionName());
        //检测是否有更新 本地版本号对比服务器版本号
        //获取本地版本号
        mLocalVersionCode = getVersionCode();
        //获取服务器版本号（客户端请求，服务端响应）
        /*
         * 更新的版本名称
         * 新版本的描述信息
         * 服务器版本号
         * 新版本apk下载地址
         */
        checkVersion();
    }

    /**
     * 检测版本号
     */
    private void checkVersion() {
        mUrl = "http://10.0.2.2:8080/update.json";
        new Thread(new Runnable() {

            @Override
            public void run() {
                Message msg = Message.obtain();
                long start = System.currentTimeMillis();
                //发送请求获取数据
                try {
                    //封装url地址
                    URL url = new URL(mUrl);
                    //开启一个链接
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    //设置常见请求参数（请求头）
                    connection.setConnectTimeout(2000);
                    connection.setReadTimeout(2000);
//                    connection.setRequestMethod("POST");
                    //获取请求成功响应码
                    if (connection.getResponseCode() == 200) {
                        //以流的形式获取数据
                        InputStream is = connection.getInputStream();
                        //将流转换成字符串
                        String json = StreamUtil.stream2String(is);
                        JSONObject jsonObject = new JSONObject(json);
                        String versionName = jsonObject.getString("versionName");
                        String versionCode = jsonObject.getString("versionCode");
                        mVersionDes = jsonObject.getString("versionDes");
                        mDownloadUrl = jsonObject.getString("downloadUrl");
                        Log.d(TAG, versionName);
                        Log.d(TAG, versionCode);
                        Log.d(TAG, mVersionDes);
                        Log.d(TAG, mDownloadUrl);
                        if (mLocalVersionCode < Integer.parseInt(versionCode)) {
                            msg.what = UPDATE_VERSION;
                        } else {
                            msg.what = ENTER_HOME;
                        }

                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    msg.what = URL_ERROR;
                } catch (IOException e) {
                    e.printStackTrace();
                    msg.what = IO_ERROR;
                } catch (JSONException e) {
                    e.printStackTrace();
                    msg.what = JSON_ERROR;
                } finally {
                    long end = System.currentTimeMillis();
                    if (end - start < 4000) try {
                        Thread.sleep(4000-(end - start));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mHandler.sendMessage(msg);
                }
            }
        }).start();
    }

    /**
     * 返回版本号，非0即成功
     * @return
     */
    private int getVersionCode() {
        //包管理者对象
        PackageManager pm = getPackageManager();
        //从包管理者对象中获取指定包名的基本信息：版本名称，版本号
        try {
            PackageInfo packageInfo = pm.getPackageInfo(getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 获取版本名称：清单文件
     * @return 应用版本名称 返回null代表异常
     */
    public String getVersionName() {
        //包管理者对象
        PackageManager pm = getPackageManager();
        //从包管理者对象中获取指定包名的基本信息：版本名称，版本号
        try {
            PackageInfo packageInfo = pm.getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
