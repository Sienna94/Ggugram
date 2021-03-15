package com.example.ggugram

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    var auth : FirebaseAuth? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()
        btn_login.setOnClickListener {
            signin_signup()
        }
    }
    fun signin_signup(){
        auth?.createUserWithEmailAndPassword(et_email.text.toString(), et_pwd.text.toString())
                ?.addOnCompleteListener {
                    task ->
                    if(task.isSuccessful){
                        //creating a user account 회원 계정 만들기:1
                        moveMainPage(task.result?.user) //계정이 만들어지면 넘어가도록
                   }else if(task.exception?.message.isNullOrEmpty()) {
                         //작동하지 않는 부분 Show the error message:2
                       Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show() //에러나면 toast 띄우기
                   }else{
                       //Login if you have account (1계정이 이미 있고, 2오류가 없으면 로그인 진행)
                    }
                }
    }
    fun siginEmail(){ //이메일 로그인 메소드
        //signup과 같은 부분이라 복붙.
        auth?.signInWithEmailAndPassword(et_email.text.toString(), et_pwd.text.toString())
                ?.addOnCompleteListener {
                    task ->
                    if(task.isSuccessful){
                        //Login ok: 아이디와 패스워드가 일치할 때
                        //로그인 성공시 메인으로 넘어가는 코드
                        moveMainPage(task.result?.user)
                    }else{
                        //Login fail : 아이디 패스워드 불일치 ->show the error msg
                        Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show() //에러나면 toast 띄우기
                    }
                }
    }
    fun moveMainPage(user:FirebaseUser?){ //로그인 성공시 메인페이지로 넘어가는 메소드
        if(user != null){ // 파이어베이스에 유저 상태가 있을 경우 넘어감
            startActivity(Intent(this, MainActivity::class.java)) //인텐트 사용해서 넘어감
        }
    }
}