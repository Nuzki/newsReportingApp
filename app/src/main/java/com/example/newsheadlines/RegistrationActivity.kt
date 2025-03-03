package com.example.newsheadlines

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase

class RegistrationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RegistrationScreen(this) }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun RegistrationScreen(context: Context) {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var role by remember { mutableStateOf("user") }
        var roleExpanded by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf("") }
        var showPinDialog by remember { mutableStateOf(false) }
        var pinInput by remember { mutableStateOf("") }
        val scaffoldState = rememberScaffoldState()

        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance()

        fun registerUser() {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(username).build())
                            ?.addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    Log.d("Registration", "Profile updated with username: $username")
                                } else {
                                    Log.e("Registration", "Profile update failed: ${profileTask.exception?.message}")
                                }
                            }
                        if (role != "user") {
                            val functions = Firebase.functions
                            val data = hashMapOf("uid" to user?.uid, "role" to role)
                            functions.getHttpsCallable("setUserRole").call(data)
                                .addOnSuccessListener {
                                    Log.d("Registration", "Custom claim set: $role")
                                    // Fallback: Also save to database
                                    val userData = User(
                                        uid = user?.uid ?: "",
                                        username = username,
                                        email = email,
                                        isAdmin = role == "admin",
                                        isJournalist = role == "journalist",
                                        isUser = role == "user"
                                    )
                                    database.getReference("users").child(user!!.uid).setValue(userData)
                                        .addOnSuccessListener {
                                            Log.d("Registration", "User data saved as fallback")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("Registration", "Failed to save fallback: ${e.message}")
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Registration", "Failed to set custom claim: ${e.message}")
                                    error = "Failed to set role: ${e.message}"
                                    // Fallback on failure
                                    val userData = User(
                                        uid = user?.uid ?: "",
                                        username = username,
                                        email = email,
                                        isAdmin = role == "admin",
                                        isJournalist = role == "journalist",
                                        isUser = role == "user"
                                    )
                                    database.getReference("users").child(user!!.uid).setValue(userData)
                                        .addOnSuccessListener {
                                            Log.d("Registration", "User data saved as fallback after claim failure")
                                        }
                                        .addOnFailureListener { e2 ->
                                            Log.e("Registration", "Fallback failed: ${e2.message}")
                                        }
                                }
                        } else {
                            // Set "user" role for regular users
                            val functions = Firebase.functions
                            functions.getHttpsCallable("setUserRole").call(hashMapOf("uid" to user?.uid, "role" to "user"))
                                .addOnSuccessListener { Log.d("Registration", "User role set") }
                                .addOnFailureListener { e ->
                                    Log.e("Registration", "Failed to set user role: ${e.message}")
                                    // Fallback
                                    database.getReference("users").child(user!!.uid).setValue(User(uid = user.uid, username = username, email = email, isUser = true))
                                }
                        }
                        context.startActivity(Intent(context, LoginActivity::class.java))
                        finish()
                    } else {
                        error = "Registration failed: ${task.exception?.message}"
                        Log.e("Registration", "Auth failed: ${task.exception?.message}")
                    }
                }
        }

        if (showPinDialog && (role == "journalist" || role == "admin")) {
            AlertDialog(
                onDismissRequest = { showPinDialog = false },
                title = { Text("Enter PIN for $role") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { pinInput = it },
                            label = { Text("PIN") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        if (error.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = error, color = Color.Red)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val requiredPin = if (role == "journalist") "248650" else "2486507193"
                        if (pinInput == requiredPin) {
                            registerUser()
                        } else {
                            error = "Incorrect PIN"
                        }
                    }) { Text("Confirm") }
                },
                dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("Cancel") } }
            )
        }

        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                TopAppBar(
                    title = { Text("Register", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    backgroundColor = MaterialTheme.colors.primary,
                    contentColor = MaterialTheme.colors.onPrimary
                )
            }
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
                    painter = painterResource(id = R.drawable.signup),
                    contentDescription = "Register",
                    modifier = Modifier.size(120.dp).clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    leadingIcon = { Icon(Icons.Default.Person, "Username") },
                    placeholder = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    leadingIcon = { Icon(Icons.Default.Email, "Email") },
                    placeholder = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    leadingIcon = { Icon(Icons.Default.Lock, "Password") },
                    placeholder = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = !roleExpanded }
                ) {
                    OutlinedTextField(
                        value = role.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                        listOf("user", "journalist", "admin").forEach { r ->
                            DropdownMenuItem(
                                content = { Text(r.replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    role = r
                                    roleExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (error.isNotEmpty()) {
                    LaunchedEffect(error) { scaffoldState.snackbarHostState.showSnackbar(error) }
                }
                Button(
                    onClick = {
                        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
                            error = "Please fill all fields"
                        } else if (role == "user") {
                            registerUser()
                        } else {
                            showPinDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Register") }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { context.startActivity(Intent(context, LoginActivity::class.java)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Go to Login") }
            }
        }
    }
}