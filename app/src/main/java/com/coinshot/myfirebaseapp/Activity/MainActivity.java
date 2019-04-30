package com.coinshot.myfirebaseapp.Activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.textclassifier.TextLinks;
import android.widget.Button;
import android.widget.Toast;

import com.coinshot.myfirebaseapp.Model.Push;
import com.coinshot.myfirebaseapp.Model.Response;
import com.coinshot.myfirebaseapp.R;
import com.coinshot.myfirebaseapp.Service.FCMService;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;


public class MainActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 100;
    private static final int RC_SIGN_OUT = 300;

    private final String TO = "fsoY-AyqeTk:APA91bGVA42yhPFyfRtREnOJFt7RmQGi4Aj-PLqt1tYLiSo6Nz-bfsUjbtfiEFCDWItrnbPey0tn2Kd1ErqscP8a2EP_pvHDPPw2Veao9x2Th8x_S7X7fxRStlmTtWyYzGpDMh_11D5e";
    private final String KEY = "AAAA0qxo8nQ:APA91bEdUQrgLU-UiYUi7veXJFXPvKuj7VukwjjVUyEEPXnwJi4K2vC3-X92mlKrLAfZgo9a829waphnWFf16E081kI2GyMpt3-ksvPCPTGWTvfNDNYLXsE0JGQllmUITqafxGoQJ8iF";
    private final String FCM_URL = "https://fcm.googleapis.com/fcm/send";
    private final String TITLE = "로그인 알림";
    private String message = "";

    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;
    private GoogleApiClient mGoogleApiClient;
    private GoogleSignInOptions gso;
    private CallbackManager callbackManager;

    Button googleLoginButton;
    LoginButton facebookLoginButton;

    final String TAG = "LOGIN";
    //final FCMService fcmService = FCMService.retrofit.create(FCMService.class);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        callbackManager = CallbackManager.Factory.create();

        googleLoginButton = findViewById(R.id.googleLoginButton);
        facebookLoginButton = findViewById(R.id.facebookLoginButton);

        facebookLoginButton.setReadPermissions("email", "public_profile");

        firebaseAuth = FirebaseAuth.getInstance();

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Log.d(TAG, "Login fail");
                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();


        googleSignInClient = GoogleSignIn.getClient(this,gso);

        // 구글 로그인 버튼
        googleLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        // 페이스북 로그인 버튼
        facebookLoginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess : " + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {
                Log.e(TAG, error.toString());
            }
        });

        // 디바이스 토큰값
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if(!task.isSuccessful()){
                            Log.w(TAG, "getInstanceID failed", task.getException());
                            return;
                        }
                        String token = task.getResult().getToken();

                        Log.d(TAG, token);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 페이스북 CallbackManager
        callbackManager.onActivityResult(requestCode, resultCode, data);

        // 구글 로그인 처리
        if(requestCode == RC_SIGN_IN){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try{
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            }catch (ApiException e){

            }
        }else if(requestCode == RC_SIGN_OUT){
            signOut();
        }
    }

    // 구글 로그인 처리
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct){
        Log.d(TAG,"firebaseAuthWithGoogle : " + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "signInCredential : success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            String email = user.getEmail();
                            message = "구글로 로그인 했습니다.";
                            NetworkTask networkTask = new NetworkTask(message);
                            networkTask.execute();

                            Toast.makeText(MainActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(getApplicationContext(), LogoutActivity.class);
                            intent.putExtra("email", email);
                            intent.putExtra("type","google");
                            startActivityForResult(intent,RC_SIGN_OUT);

                        }else{
                            Log.e(TAG, "signInCredential:failure", task.getException());
                        }
                    }
                });
    }

    // 페이스북 로그인 처리
    private void handleFacebookAccessToken(AccessToken token){
        Log.d(TAG, "handleFacebookAccessToken : " + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "signInWithCredentialFacebook : success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            String email = "";
                            message = "페이스북으로 로그인 했습니다.";
                            NetworkTask networkTask = new NetworkTask(message);
                            networkTask.execute();

                            if(user.getEmail() != null){
                                email = user.getEmail();
                            }else{
                                email = "None";
                            }

                            Toast.makeText(MainActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(getApplicationContext(), LogoutActivity.class);
                            intent.putExtra("email", email);
                            intent.putExtra("type", "facebook");
                            startActivity(intent);

                        }else{
                            Log.e(TAG, "signInWithCredential : failure", task.getException());
                            Toast.makeText(getApplicationContext(), "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void signIn(){
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signOut(){
        mGoogleApiClient.connect();
        mGoogleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {
                firebaseAuth.signOut();
                if(mGoogleApiClient.isConnected()){
                    Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            if(status.isSuccess()){
                                Log.d(TAG, "User logged out");
                                Toast.makeText(getApplicationContext(), "구글 로그아웃 성공", Toast.LENGTH_SHORT).show();
                            }else{
                                Log.d(TAG,"User log out failed");
                            }
                        }
                    });
                }
            }

            @Override
            public void onConnectionSuspended(int i) {
                Log.d(TAG, "Google API Client Connection Suspended");
            }
        });
    }
/*
    private void setMessage(String to, String title, String message){
        fcmService.postFCMBody(new Push(to,title,message)).enqueue(new Callback<Response>() {
            @Override
            public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                if(response.isSuccessful()){
                    String repo = response.body().toString();
                    Log.d(TAG, "response.getSuccess : " + repo.toString());
                }else{
                    Log.d(TAG, "body 없음" );
                }
            }

            @Override
            public void onFailure(Call<Response> call, Throwable t) {
                Log.d(TAG, "onFailure 실행됨"+ t.toString());
            }
        });
    }
*/
    public class NetworkTask extends AsyncTask<Void, Void, String>{
        private String msg;

        public NetworkTask(String msg){
            this.msg = msg;
        }
        @Override
        protected String doInBackground(Void... voids) {
            try{
                // FMC 메시지 생성
                JSONObject root = new JSONObject();
                JSONObject data = new JSONObject();
                data.put("title", TITLE);
                data.put("message", message);
                root.put("to",TO);
                root.put("data", data);

                Log.d(TAG,root.toString());
                URL url = new URL(FCM_URL);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                con.setDoInput(true);
                con.addRequestProperty("Authorization", "key=" + KEY);
                con.addRequestProperty("Content-Type","application/json");
                OutputStream os = con.getOutputStream();
                os.write(root.toString().getBytes("utf-8"));
                os.flush();
                con.getResponseCode();
                Log.d(TAG,con.toString());
            }catch(Exception e){
                e.printStackTrace();
            }
            return null;
        }
    }
}
