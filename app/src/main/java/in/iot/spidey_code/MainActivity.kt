package `in`.iot.spidey_code

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import `in`.iot.spidey_code.ui.theme.SpideyCodeTheme
import `in`.iot.spidey_code.view.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpideyCodeTheme {
                AppNavigation()
            }
        }
    }
}