package com.rolesencia.diceroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.rolesencia.diceroller.ui.screen.DiceRollerScreen
import com.rolesencia.diceroller.ui.theme.DiceRollerTheme
import com.rolesencia.diceroller.ui.viewmodel.DiceRollerViewModel
import com.rolesencia.diceroller.ui.viewmodel.DiceRollerViewModelFactory

class MainActivity : ComponentActivity() {

    // A nivel Activity para que el Splash sepa cuándo terminó de cargar DataStore.
    private val viewModel: DiceRollerViewModel by viewModels { DiceRollerViewModelFactory(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value.estaCargando }
        enableEdgeToEdge()
        setContent {
            DiceRollerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DiceRollerScreen(viewModel)
                }
            }
        }
    }
}
