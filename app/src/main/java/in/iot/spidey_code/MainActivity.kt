package `in`.iot.spidey_code

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import `in`.iot.spidey_code.ui.theme.SpideyCodeTheme
import `in`.iot.spidey_code.view.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        window.setBackgroundDrawableResource(android.R.color.black)
        enableEdgeToEdge()

        setContent {
            SpideyCodeTheme(darkTheme = true) {
                AppNavigation()
            }
        }
    }
}