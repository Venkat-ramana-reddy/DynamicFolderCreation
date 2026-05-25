package com.factoryattendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.factoryattendance.data.preferences.AppPreferences
import com.factoryattendance.ui.navigation.AppNavHost
import com.factoryattendance.ui.theme.AttendanceTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by appPreferences.isDarkMode.collectAsState(initial = false)
            AttendanceTheme(darkTheme = isDarkMode) {
                AppNavHost()
            }
        }
    }
}
