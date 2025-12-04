package com.example.healplusapp.features.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.appcompat.app.AppCompatActivity
import com.example.healplusapp.MainActivity
import com.example.healplusapp.R
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var signInClient: SignInClient

    private val googleLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode != RESULT_OK || result.data == null) {
            Log.d("LoginActivity", "Login cancelado pelo usuário")
            return@registerForActivityResult
        }
        
        try {
            val credential = signInClient.getSignInCredentialFromIntent(result.data)
            val idToken = credential.googleIdToken
            when {
                idToken != null -> {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                Log.d("LoginActivity", "Login bem-sucedido: ${user?.email}")
                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                finish()
                            } else {
                                Log.e("LoginActivity", "Falha no login", task.exception)
                                Toast.makeText(this@LoginActivity, "Falha no login: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                }
                else -> {
                    Log.e("LoginActivity", "ID Token não encontrado")
                    Toast.makeText(this, "Erro ao obter credencial do Google", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: ApiException) {
            Log.e("LoginActivity", "Erro na autenticação", e)
            when (e.statusCode) {
                12501 -> {
                    // Usuário cancelou o login
                    Log.d("LoginActivity", "Login cancelado")
                }
                else -> {
                    Toast.makeText(this, "Erro na autenticação: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Erro inesperado", e)
            Toast.makeText(this, "Erro ao processar login: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        signInClient = Identity.getSignInClient(this)

        findViewById<Button>(R.id.btn_login_email).setOnClickListener { loginEmail() }
        findViewById<Button>(R.id.btn_register_email).setOnClickListener { registerEmail() }
        findViewById<Button>(R.id.btn_google_sign_in).setOnClickListener { loginGoogle() }
    }

    private fun loginEmail() {
        val email = findViewById<EditText>(R.id.input_email).text?.toString()?.trim().orEmpty()
        val pass = findViewById<EditText>(R.id.input_password).text?.toString()?.trim().orEmpty()
        if (email.isEmpty() || pass.length < 6) {
            Toast.makeText(this, "Informe e-mail e senha válidos", Toast.LENGTH_SHORT).show()
            return
        }
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                startActivity(android.content.Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Falha no login: ${task.exception?.localizedMessage ?: ""}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun registerEmail() {
        val email = findViewById<EditText>(R.id.input_email).text?.toString()?.trim().orEmpty()
        val pass = findViewById<EditText>(R.id.input_password).text?.toString()?.trim().orEmpty()
        if (email.isEmpty() || pass.length < 6) {
            Toast.makeText(this, "Informe e-mail e senha válidos", Toast.LENGTH_SHORT).show()
            return
        }
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                startActivity(android.content.Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Falha no registro: ${task.exception?.localizedMessage ?: ""}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loginGoogle() {
        val clientId = getString(R.string.default_web_client_id)
        if (clientId.isBlank() || clientId == "REPLACE_ME_WITH_CLIENT_ID") {
            Toast.makeText(this, "Configure o Client ID do Google no arquivo strings.xml", Toast.LENGTH_LONG).show()
            Log.e("LoginActivity", "Client ID não configurado")
            return
        }
        
        val request = GetSignInIntentRequest.builder()
            .setServerClientId(clientId)
            .build()
            
        signInClient.getSignInIntent(request)
            .addOnSuccessListener { pendingIntent ->
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                    googleLauncher.launch(intentSenderRequest)
                } catch (e: Exception) {
                    Log.e("LoginActivity", "Erro ao lançar intent", e)
                    Toast.makeText(this, "Erro ao iniciar login: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("LoginActivity", "Falha ao obter intent de login", e)
                Toast.makeText(this, "Falha ao obter intenção de login: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }
}
