package com.glintbox.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute

@Serializable
data object HomeRoute

@Serializable
data class ViewerRoute(val startIndex: Int, val savedMode: Boolean)

@Serializable
data object AboutRoute

@Serializable
data object PrivacyRoute
