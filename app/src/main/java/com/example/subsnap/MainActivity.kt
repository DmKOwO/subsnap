package com.example.subsnap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.subsnap.theme.SubSnapTheme

class MainActivity : ComponentActivity() {

    companion object {
        @Volatile
        var isAppInForeground: Boolean = false
            private set
    }

    override fun onStart() {
        super.onStart()
        isAppInForeground = true
    }

    override fun onStop() {
        super.onStop()
        isAppInForeground = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            SubSnapTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainNavigation()
                }
            }
        }
    }
}
