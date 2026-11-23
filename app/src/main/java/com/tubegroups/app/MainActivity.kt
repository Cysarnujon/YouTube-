package com.tubegroups.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tubegroups.app.ui.TubeGroupsApp
import com.tubegroups.app.ui.TubeGroupsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TubeGroupsTheme {
                TubeGroupsApp()
            }
        }
    }
}
