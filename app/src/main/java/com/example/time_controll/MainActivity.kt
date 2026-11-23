package com.example.time_controll

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.time_controll.ui.AlarmAppScreen
import com.example.time_controll.ui.theme.TimecontrollTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TimecontrollTheme {
                AlarmAppScreen()
            }
        }
    }
}
