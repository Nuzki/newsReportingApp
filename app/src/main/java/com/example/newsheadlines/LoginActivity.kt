package com.example.newsheadlines

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LoginActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private var userListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        setContent { LoginScreen(this, auth) }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            navigateToNextActivity()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        userListener?.let { FirebaseDatabase.getInstance().getReference("users").removeEventListener(it) }
    }

    private fun navigateToNextActivity() {
        val user = auth.currentUser
        if (user != null) {
            val database = FirebaseDatabase.getInstance()
            userListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userData = snapshot.child(user.uid).getValue(User::class.java)
                    val intent = if (userData?.isAdmin == true) {
                        Intent(this@LoginActivity, AdminDashboardActivity::class.java)
                    } else {
                        Intent(this@LoginActivity, MainPage::class.java)
                    }
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
                override fun onCancelled(error: DatabaseError) {}
            }.also { database.getReference("users").addListenerForSingleValueEvent(it) }
        }
    }

    @Composable
    fun LoginScreen(context: ComponentActivity, auth: FirebaseAuth) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }
        val scaffoldState = rememberScaffoldState()

        Scaffold(
            scaffoldState = scaffoldState,
            backgroundColor = MaterialTheme.colors.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo_2),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .padding(vertical = 16.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    leadingIcon = { Icon(Icons.Default.Person, "Email Icon", tint = MaterialTheme.colors.primary) },
                    placeholder = { Text("Email") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colors.primary,
                        unfocusedBorderColor = Color(0xFFB0BEC5),
                        backgroundColor = MaterialTheme.colors.surface
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    leadingIcon = { Icon(Icons.Default.Lock, "Password Icon", tint = MaterialTheme.colors.primary) },
                    placeholder = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colors.primary,
                        unfocusedBorderColor = Color(0xFFB0BEC5),
                        backgroundColor = MaterialTheme.colors.surface
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (error.isNotEmpty()) {
                    LaunchedEffect(error) { scaffoldState.snackbarHostState.showSnackbar(error) }
                }
                Button(
                    onClick = {
                        if (email.isEmpty() || password.isEmpty()) {
                            error = "Please fill all fields"
                            return@Button
                        }
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    navigateToNextActivity()
                                } else {
                                    error = "Login failed: ${task.exception?.message}"
                                }
                            }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp).padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                ) { Text("Log In", fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onPrimary, fontSize = 16.sp) }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text("Don’t have an account? ", color = MaterialTheme.colors.onSurface, fontSize = 14.sp)
                    TextButton(onClick = { context.startActivity(Intent(context, RegistrationActivity::class.java)) }) {
                        Text("Register", fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}