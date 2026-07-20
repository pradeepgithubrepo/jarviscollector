package com.pradeep.jarviscollector.ui.name_selection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pradeep.jarviscollector.navigation.Screen
import com.pradeep.jarviscollector.utils.AppPreferences

/**
 * NameSelectionScreen: shown only once on first launch.
 * Two users are supported: Pradeep and Shobana.
 * Selecting a name persists it via AppPreferences and navigates to Home.
 */
@Composable
fun NameSelectionScreen(
    navController: NavController,
    onOwnerNameChange: (String) -> Unit
) {
    // Hoist LocalContext outside onClick lambdas — composable scope only
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "JARVIS",
                color = Color.White,
                fontSize = 32.sp,
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Who are you?",
                color = Color(0xFFB0B0B3),
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    AppPreferences.setOwnerName(context, "Pradeep")
                    onOwnerNameChange("Pradeep")
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.NameSelection.route) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A90E2)
                )
            ) {
                Text(text = "Pradeep", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    AppPreferences.setOwnerName(context, "Shobana")
                    onOwnerNameChange("Shobana")
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.NameSelection.route) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7B61FF)
                )
            ) {
                Text(text = "Shobana", fontSize = 18.sp)
            }
        }
    }
}
