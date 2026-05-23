package com.studyflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.studyflow.app.data.repository.SettingsRepository
import com.studyflow.app.navigation.NavGraph
import com.studyflow.app.navigation.bottomNavItems
import com.studyflow.app.ui.theme.StudyFlowTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.ui.unit.dp
import javax.inject.Inject
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsState by settingsRepository.getUserSettings().collectAsStateWithLifecycle(initialValue = null)
            val darkTheme = settingsState?.isDarkTheme ?: isSystemInDarkTheme()
            val isRecoveryMode = settingsState?.isRecoveryMode ?: false
            val studyState = settingsState?.currentStudyState ?: "FOCUS"

            StudyFlowTheme(darkTheme = darkTheme, isRecoveryMode = isRecoveryMode, studyState = studyState) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        val isPrimaryScreen = bottomNavItems.any { it.route == currentRoute }
                        if (isPrimaryScreen) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(
                                            elevation = 12.dp,
                                            shape = RoundedCornerShape(20.dp),
                                            clip = false,
                                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        )
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .border(
                                            border = BorderStroke(
                                                1.dp,
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = if (darkTheme) 0.12f else 0.35f),
                                                        Color.White.copy(alpha = if (darkTheme) 0.04f else 0.1f)
                                                    )
                                                )
                                            ),
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    bottomNavItems.forEach { item ->
                                        val isSelected = currentRoute == item.route
                                        val scale by animateFloatAsState(
                                            targetValue = if (isSelected) 1.25f else 1.0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            ),
                                            label = "tab_scale"
                                        )

                                        val iconAlpha by animateFloatAsState(
                                            targetValue = if (isSelected) 1.0f else 0.6f,
                                            label = "tab_alpha"
                                        )

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    if (currentRoute != item.route) {
                                                        navController.navigate(item.route) {
                                                            popUpTo(navController.graph.findStartDestination().id) {
                                                                saveState = true
                                                            }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                }
                                                .padding(vertical = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                if (isSelected) {
                                                    // Glow background behind icon
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .background(
                                                                brush = Brush.radialGradient(
                                                                    colors = listOf(
                                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                                                        Color.Transparent
                                                                    )
                                                                ),
                                                                shape = RoundedCornerShape(16.dp)
                                                            )
                                                    )
                                                }
                                                Icon(
                                                    imageVector = item.icon,
                                                    contentDescription = item.title,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = iconAlpha),
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .scale(scale)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = item.title,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = iconAlpha),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
