package com.sjtuopennetwork.shareit.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.auth0.android.jwt.JWT;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.auth.api.signin.HuaweiIdSignIn;
import com.huawei.hms.auth.api.signin.HuaweiIdSignInClient;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.support.api.hwid.HuaweiIdSignInOptions;
import com.huawei.hms.support.api.hwid.HuaweiIdStatusCodes;
import com.huawei.hms.support.api.hwid.SignInHuaweiId;
import com.sjtuopennetwork.shareit.share.HomeActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    //持久化存储
    SharedPreferences pref;

    //内存数据
    boolean isLogin;

    //华为ID
    private HuaweiIdSignInClient mSignInClient;
    private HuaweiIdSignInOptions mSignInOptions;
    String myclientid = "218779032643175488";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref=getSharedPreferences("txtl",MODE_PRIVATE);

        //查SharedPreference中"isLogin"判断登录状态，如果未登录则直接拉起华为ID登录界面。如果已登录则跳转到HomeActivity
        isLogin=pref.getBoolean("isLogin",false); //如果没有这个字段就是首次打开
        if(isLogin){ //如果已经登录直接跳转到主界面
            Intent toHomeActivity=new Intent(this, HomeActivity.class);
            startActivity(toHomeActivity);
            finish();
        }else{ //如果未登录
            // TODO: 2019/10/28  在这里拉起华为登录页面

            mSignInOptions = new HuaweiIdSignInOptions.Builder(HuaweiIdSignInOptions.DEFAULT_SIGN_IN).build();
            mSignInClient= HuaweiIdSignIn.getClient(MainActivity.this,mSignInOptions);
            startActivityForResult(mSignInClient.getSignInIntent(), 8888);



            //如果有了华为ID登录，这一块代码要删掉
//            String myname="null";
//            String avatarpath="null";
//            String openid="null";
//
//            //写入SharedPreference
//            SharedPreferences.Editor editor=pref.edit();
//            editor.putString("myname",myname);
//            editor.putString("avatarpath",avatarpath);
//            editor.putString("openid",openid);
//            editor.putBoolean("isLogin",true);
//            editor.commit();
//
//            Intent toHomeActivity=new Intent(this, HomeActivity.class);
//            startActivity(toHomeActivity);
//            finish();

        }
    }

    //从华为ID返回到结果之后，将结果写入到SharedPreference
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {


        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 8888) {
            Task<SignInHuaweiId> signInHuaweiIdTask = HuaweiIdSignIn.getSignedInAccountFromIntent(data);
            if (signInHuaweiIdTask.isSuccessful()) {
                //登录成功，获取用户的华为帐号信息和ID Token
                SignInHuaweiId huaweiAccount = signInHuaweiIdTask.getResult();
                System.out.println("====================================1" +huaweiAccount.getIdToken());
                System.out.println("====================================2" + huaweiAccount.toString());

                //已经调用华为授权，使用方法：requestIdToken(String clientId)返回IDtoken，验证其有效性；
                String clientId = myclientid;
                String id_token =huaweiAccount.getIdToken();
                System.out.println("====================================3" + huaweiAccount.getIdToken());
                JWT jwt= new JWT(id_token);
                String issuer = jwt.getIssuer();
                System.out.println("====================================issuer" + issuer);

                //IDToken有效性验证完成，调用requestId() 返回OpenId，与textile账号关联
                String openid = huaweiAccount.getOpenId();
                System.out.println("=================================4"+openid);

                 Intent toHomeActivity=new Intent(this, HomeActivity.class);
                  startActivity(toHomeActivity);
                finish();

            }
            else{
                int status = ((ApiException) signInHuaweiIdTask.getException()).getStatusCode();
                System.out.println("=============================signIn failed: "+status);
                System.out.println("=============================Result code: "+resultCode);
                //  Log.i(TAG, "signIn failed: " + status);
                if (status == HuaweiIdStatusCodes.SIGN_IN_UNLOGIN) {
                    //      Log.i(TAG, "Account not logged in");
                    //Account not logged in , try to sign in with getSignInIntent()
                    System.out.println("=============================Account not logged in");
                } else if (status == HuaweiIdStatusCodes.SIGN_IN_AUTH) {
                    //      Log.i(TAG, "Account not authorized");
                    //Account not authorized  , try to authorize with getSignInIntent()
                    System.out.println("=============================Account not authorized");
                } else if (status == HuaweiIdStatusCodes.SIGN_IN_CHECK_PASSWORD) {
                    //Huawei Account Need Password Check
                    System.out.println("=============================Account Need Password Check");
                } else if (status == HuaweiIdStatusCodes.SIGN_IN_NETWORK_ERROR) {
                    //Network exception, please CP handle it by itsel
                    System.out.println("=============================SIGN_IN_NETWORK_ERROR");
                } else {
                    //other exception, please CP handle it by itsel
                    System.out.println("=============================other_error");
                }
            }
        }


        // TODO: 2019/10/28 将华为ID的头像存储到文件，并将用户名、头像路径、openid存储到下面这三个变量中
//        String myname="null";
//        String avatarpath="null";
//        String openid="null";
//
//        //写入SharedPreference
//        SharedPreferences.Editor editor=pref.edit();
//        editor.putString("myname",myname);
//        editor.putString("avatarpath",avatarpath);
//        editor.putString("openid",openid);
//        editor.putBoolean("isLogin",true);
//        editor.commit();
//
//        Intent toHomeActivity=new Intent(this, HomeActivity.class);
//        startActivity(toHomeActivity);
//        finish();
  }

    /**
     * 发送Get请求到服务器
     * @param strUrlPath:接口地址（带参数）
     * @return
     */
    public static String getServiceInfo(String strUrlPath){
        String strResult = "";
        try {
            URL url = null;
            try {
                url = new URL(strUrlPath);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection)url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            StringBuffer buffer = new StringBuffer();
            String line = "";
            while ((line = in.readLine()) != null){
                buffer.append(line);
            }
            strResult = buffer.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return strResult;
    }
}



