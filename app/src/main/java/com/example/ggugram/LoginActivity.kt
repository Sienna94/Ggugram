package com.example.ggugram

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


class LoginActivity : AppCompatActivity() {
    var auth: FirebaseAuth? = null
    var googleSignInClient: GoogleSignInClient? = null
    var GOOGLE_LOGIN_CODE = 9001
    var callbackManager : CallbackManager? = null //facebook 로그인 결과 가져오는 콜백
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()
        btn_login.setOnClickListener {
            signin_signup()
        }
        //GOOGLE
        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        btn_google_login.setOnClickListener {
            //1st step
            googleLogin()
            //facebook hash값 확인위해 임시로 넣어줌
            //printHashKey()
        }
        //FACEBOOK
        btn_facebook_login.setOnClickListener {
            //1st step
            facebookLogin()
        }
        //facebook 콜백
        callbackManager = CallbackManager.Factory.create()
    }
    // Hash Key: Ok6oj/VRWZLaHE2wr/QHdQiBv/k=

    //facebook login
    open fun printHashKey() {
        try {
            val info: PackageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md: MessageDigest = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey: String = String(Base64.encode(md.digest(), 0))
                Log.i("TAG", "printHashKey() Hash Key: $hashKey")
            }
        } catch (e: NoSuchAlgorithmException) {
            Log.e("TAG", "printHashKey()", e)
        } catch (e: Exception) {
            Log.e("TAG", "printHashKey()", e)
        }
    }
    //페북 로그인 function
    fun facebookLogin(){
        LoginManager.getInstance()
            .logInWithReadPermissions(this, Arrays.asList("public_profile", "email")) //facebook에서 받을 권한 요청
        LoginManager.getInstance()
            .registerCallback(callbackManager, object : FacebookCallback<LoginResult>{ //object를 implement해서 function들을 추가해준 것.
                override fun onSuccess(result: LoginResult?) { //로그인이 성공하면 facebook 데이터를 firebase에 넘김
                    //2nd step
                    handleFacebookAccessToken(result?.accessToken) //아래에 function 만들어 둠.
                }
                override fun onCancel() {
                }
                override fun onError(error: FacebookException?) {
                }
            })
    }
    fun handleFacebookAccessToken(token : AccessToken?) { //토큰 받아주는 파라미터
        var credential = FacebookAuthProvider.getCredential(token?.token!!)
        auth?.signInWithCredential(credential) //바로 firebase로 넘겨줌 (이후 구글 로그인과 동일)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) { //3rd step 응답값을 받아서 페이지를 이동시킴
                    //Login ok: 아이디와 패스워드가 일치할 때 (로그인 성공시) 메인으로 넘어가는 코드
                    Log.d("ggl", "firebaseAuthWithGoogle: succeed")
                    moveMainPage(task.result?.user)
                } else {
                    //Login fail : 아이디 패스워드 불일치 ->show the error msg
                    Log.d("ggl", "firebaseAuthWithGoogle: failed")
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG)
                        .show() //에러나면 toast 띄우기
                }
            }
    }

    //구글 로그인 function
    fun googleLogin() {
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_LOGIN_CODE) {
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result != null) {
                if (result.isSuccess) {
                    Log.d("ggl", "onActivityResult: if 2")
                    var account = result.signInAccount
                    //2nd step
                    if (account != null) {
                        Log.d("ggl", "onActivityResult: if 3")
                        firebaseAuthWithGoogle(account)
                    }
                }
            }
        }
    }

    fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        var credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //Login ok: 아이디와 패스워드가 일치할 때 (로그인 성공시) 메인으로 넘어가는 코드
                    Log.d("ggl", "firebaseAuthWithGoogle: succeed")
                    moveMainPage(task.result?.user)
                } else {
                    //Login fail : 아이디 패스워드 불일치 ->show the error msg
                    Log.d("ggl", "firebaseAuthWithGoogle: failed")
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG)
                        .show() //에러나면 toast 띄우기
                }
            }

    }

    fun signin_signup() {
        auth?.createUserWithEmailAndPassword(et_email.text.toString(), et_pwd.text.toString())
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //creating a user account 회원 계정 만들기:1
                    moveMainPage(task.result?.user) //계정이 만들어지면 넘어가도록
                } else if (task.exception!!.message.isNullOrEmpty()) {
                    //작동하지 않는 부분 Show the error message:2
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG)
                        .show() //에러나면 toast 띄우기
                } else {
                    //Login if you have account (1계정이 이미 있고, 2오류가 없으면 로그인 진행)
                    signinEmail()
                }
            }
    }

    fun signinEmail() { //이메일 로그인 메소드
        //signup과 같은 부분이라 복붙.
        auth?.signInWithEmailAndPassword(et_email.text.toString(), et_pwd.text.toString())
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //Login ok: 아이디와 패스워드가 일치할 때
                    //로그인 성공시 메인으로 넘어가는 코드
                    moveMainPage(task.result?.user)
                } else {
                    //Login fail : 아이디 패스워드 불일치 ->show the error msg
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG)
                        .show() //에러나면 toast 띄우기
                }
            }
    }

    fun moveMainPage(user: FirebaseUser?) { //로그인 성공시 메인페이지로 넘어가는 메소드
        if (user != null) { // 파이어베이스에 유저 상태가 있을 경우 넘어감
            startActivity(Intent(this, MainActivity::class.java)) //인텐트 사용해서 넘어감
        }
    }
}