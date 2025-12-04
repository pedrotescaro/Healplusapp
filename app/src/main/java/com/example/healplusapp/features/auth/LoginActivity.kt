package com.example.healplusapp.features.auth

import android.os.Bundle
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
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var signInClient: SignInClient

    private val googleLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK && data != null) {
            try {
                val cred = GoogleIdTokenCredential.createFrom(data.extras!!)
                val token = cred.idToken
                val firebaseCred = GoogleAuthProvider.getCredential(token, null)
                auth.signInWithCredential(firebaseCred).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        startActivity(android.content.Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Falha no login com Google", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (_: Exception) {
                Toast.makeText(this, "Erro ao obter credencial do Google", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Login com Google cancelado", Toast.LENGTH_SHORT).show()
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
        if (clientId.isBlank() || clientId == "453362559960-ccrm0tggg00mdeo6r7lh8jh4utdpjk68.apps.googleusercontent.com") {
            Toast.makeText(this, "Configure o clientId do Google no Firebase", Toast.LENGTH_LONG).show()
            return
        }
        val request = GetSignInIntentRequest.builder()
            .setServerClientId(clientId)
            .build()
        signInClient.getSignInIntent(request)
            .addOnSuccessListener { pendingIntent ->
                val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                googleLauncher.launch(intentSenderRequest)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Falha ao obter intenção de login: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }
}
