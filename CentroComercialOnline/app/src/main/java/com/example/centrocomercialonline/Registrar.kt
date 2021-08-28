package com.example.centrocomercialonline

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.*

class Registrar : AppCompatActivity() {
    private val GOOGLE_SIGN_IN = 100
    private val callbackManager = CallbackManager.Factory.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrarse)

        val botonIrRegistrarDatos = findViewById<Button>(R.id.registrarse1)
        val loginGoogle = findViewById<ImageView>(R.id.imageViewGoogle)
        val loginFacebook = findViewById<ImageView>(R.id.imageViewFacebook)

        var email = findViewById<EditText>(R.id.email)
        var password = findViewById<EditText>(R.id.password)
        var username = findViewById<EditText>(R.id.username)


        botonIrRegistrarDatos.setOnClickListener {
            if(email.text.isNotEmpty() && password.text.isNotEmpty() && username.text.isNotEmpty()){
                FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email.text.toString(),
                    password.text.toString()).addOnCompleteListener{
                        if(it.isSuccessful){
                            showUserInformation(it.result?.user?.email ?: "",ProviderType.BASIC)
                        }else{
                            showAlert()
                        }
                    }
            }

            }

        loginGoogle.setOnClickListener {
            val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val googleClient = GoogleSignIn.getClient(this, googleConf)
            googleClient.signOut()
            startActivityForResult(googleClient.signInIntent, GOOGLE_SIGN_IN)
        }

        loginFacebook.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email"))
            LoginManager.getInstance().registerCallback(callbackManager,
            object: FacebookCallback<LoginResult>{
                override fun onSuccess(result: LoginResult?) {
                    result?.let {
                        val token = it.accessToken
                        val credential = FacebookAuthProvider.getCredential(token.token)
                        FirebaseAuth.getInstance().signInWithCredential(credential)
                            .addOnCompleteListener {
                                if (it.isSuccessful) {
                                    showUserInformation(it.result?.user?.email ?: "", ProviderType.FACEBOOK)
                                } else {
                                    showAlert()
                                }
                            }
                    }
                }

                override fun onCancel() {

                }

                override fun onError(error: FacebookException?) {
                    showAlert()
                }

            })
        }

        session()





    }


    private fun session(){
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email =prefs.getString("email",null)
        val provider = prefs.getString("provider",null)
        val registrar = findViewById<ConstraintLayout>(R.id.containerRegistrar)

        if(email != null && provider !=null){
            registrar.visibility = View.VISIBLE
            showUserInformation(email, ProviderType.valueOf(provider))
        }
    }


    fun showAlert(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("Se ha producido un error autenticando el usuario")
        builder.setPositiveButton("Aceptar",null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    fun showUserInformation(email:String, provider: ProviderType){
        val UserInformationIntent: Intent = Intent(this, PerfilUsuario::class.java).apply {
            putExtra("email",email)
            putExtra("provider",provider.name)
        }
        startActivity(UserInformationIntent)
    }

    override fun onStart(){
        super.onStart()
        val registrar = findViewById<ConstraintLayout>(R.id.containerRegistrar)
        registrar.visibility = View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?){
        callbackManager.onActivityResult(requestCode,resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == GOOGLE_SIGN_IN){
            val task  = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if(account != null){
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener {
                        if(it.isSuccessful){
                            showUserInformation(account.email ?: "",ProviderType.GOOGLE)
                        }else{
                            showAlert()
                        }
                    }
                }
            } catch (e:ApiException){
                showAlert()
            }
        }

    }



}
