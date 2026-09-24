package com.glintbox.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hasRoute
import com.glintbox.app.ui.MainViewModel
import com.glintbox.app.ui.about.AboutScreen
import com.glintbox.app.ui.about.PrivacyScreen
import com.glintbox.app.ui.home.HomeScreen
import com.glintbox.app.ui.onboarding.OnboardingScreen
import com.glintbox.app.ui.viewer.ViewerScreen

@Composable
fun GlintboxNavHost(vm: MainViewModel, onboardingDone: Boolean) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val backStack by navController.currentBackStackEntryAsState()
    val onHome = backStack?.destination?.hasRoute(HomeRoute::class) == true

    LaunchedEffect(vm) {
        vm.messages.collect { msg ->
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(context.getString(msg.res, *msg.args.toTypedArray()))
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = if (onboardingDone) HomeRoute else OnboardingRoute,
            enterTransition = { slideInHorizontally(tween(320)) { it / 5 } + fadeIn(tween(320)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(250)) },
            popExitTransition = { slideOutHorizontally(tween(280)) { it / 5 } + fadeOut(tween(280)) },
        ) {
            composable<OnboardingRoute> {
                OnboardingScreen(
                    vm = vm,
                    onFinish = {
                        vm.completeOnboarding()
                        navController.navigate(HomeRoute) {
                            popUpTo(OnboardingRoute) { inclusive = true }
                        }
                    },
                    onOpenPrivacy = { navController.navigate(PrivacyRoute) },
                )
            }
            composable<HomeRoute> {
                HomeScreen(
                    vm = vm,
                    onOpenViewer = { index, saved -> navController.navigate(ViewerRoute(index, saved)) },
                    onOpenAbout = { navController.navigate(AboutRoute) },
                    onOpenPrivacy = { navController.navigate(PrivacyRoute) },
                )
            }
            composable<ViewerRoute>(
                enterTransition = { scaleIn(tween(260), initialScale = 0.92f) + fadeIn(tween(260)) },
                popExitTransition = { scaleOut(tween(220), targetScale = 0.92f) + fadeOut(tween(220)) },
            ) { entry ->
                val route = entry.toRoute<ViewerRoute>()
                ViewerScreen(
                    vm = vm,
                    startIndex = route.startIndex,
                    savedMode = route.savedMode,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<AboutRoute> { AboutScreen(onBack = { navController.popBackStack() }) }
            composable<PrivacyRoute> { PrivacyScreen(onBack = { navController.popBackStack() }) }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (onHome) 96.dp else 88.dp, start = 12.dp, end = 12.dp),
        ) { data ->
            Snackbar(
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shape = MaterialTheme.shapes.medium,
            ) { Text(data.visuals.message) }
        }
    }
}
