package com.example.newsheadlines

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.example.newsheadlines.ui.theme.NewsHeadlinesTheme
import kotlinx.coroutines.launch

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("onboarding_complete", false)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContent {
            val isDarkTheme = prefs.getBoolean("dark_theme", false)
            NewsHeadlinesTheme(darkTheme = isDarkTheme) { OnboardingScreen(prefs) }
        }
    }

    @Composable
    fun OnboardingScreen(prefs: android.content.SharedPreferences) {
        val context = LocalContext.current
        val pagerState = rememberPagerState(pageCount = { 3 })
        val scope = rememberCoroutineScope()
        val pages = listOf(
            OnboardingPage("Welcome", "Stay informed with InfoSync!"),
            OnboardingPage("Engage", "Read, rate, and comment on articles."),
            OnboardingPage("Contribute", "Journalists can publish, admins approve!")
        )

        Scaffold(backgroundColor = MaterialTheme.colors.background) { paddingValues ->
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = pages[page].title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = MaterialTheme.colors.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = pages[page].description,
                            fontSize = 16.sp,
                            color = MaterialTheme.colors.onBackground,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (pagerState.currentPage > 0) {
                        Button(
                            onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Previous") }
                    } else {
                        Spacer(modifier = Modifier.width(0.dp))
                    }
                    Button(
                        onClick = {
                            if (pagerState.currentPage < pages.size - 1) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            } else {
                                prefs.edit { putBoolean("onboarding_complete", true) }
                                context.startActivity(Intent(context, LoginActivity::class.java))
                                (context as ComponentActivity).finish()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(if (pagerState.currentPage == pages.size - 1) "Get Started" else "Next") }
                }
            }
        }
    }

    data class OnboardingPage(val title: String, val description: String)
}