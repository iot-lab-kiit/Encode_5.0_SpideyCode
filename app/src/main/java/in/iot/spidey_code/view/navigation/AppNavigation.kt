package `in`.iot.spidey_code.view.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import `in`.iot.spidey_code.data.model.FilterType
import `in`.iot.spidey_code.view.screens.CameraScreen
import `in`.iot.spidey_code.view.screens.ReviewScreen
import `in`.iot.spidey_code.view.screens.SplashScreen
import `in`.iot.spidey_code.view.screens.VideoReviewScreen

object Routes {
    const val SPLASH = "splash"
    const val CAMERA = "camera/{filterType}"
    const val REVIEW = "review/{filterType}?imageUri={imageUri}"
    const val VIDEO_REVIEW = "video_review/{filterType}?videoUri={videoUri}"

    fun createCameraRoute(filterType: FilterType = FilterType.CLASSIC_MASK): String = "camera/${filterType.name}"
    fun createReviewRoute(filterType: FilterType, imageUri: String): String =
        "review/${filterType.name}?imageUri=${Uri.encode(imageUri)}"
    fun createVideoReviewRoute(filterType: FilterType, videoUri: String): String =
        "video_review/${filterType.name}?videoUri=${Uri.encode(videoUri)}"
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier
    ) {
        composable(route = Routes.SPLASH) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Routes.createCameraRoute(FilterType.CLASSIC_MASK)) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.CAMERA,
            arguments = listOf(
                navArgument("filterType") {
                    type = NavType.StringType
                    defaultValue = FilterType.CLASSIC_MASK.name
                }
            )
        ) { backStackEntry ->
            val filterName = backStackEntry.arguments?.getString("filterType")
            val filterType = filterName?.let {
                runCatching { FilterType.valueOf(it) }.getOrDefault(FilterType.CLASSIC_MASK)
            } ?: FilterType.CLASSIC_MASK

            CameraScreen(
                selectedFilter = filterType,
                onNavigateToReview = { selectedFilter, imageUri ->
                    navController.navigate(Routes.createReviewRoute(selectedFilter, imageUri))
                },
                onNavigateToVideoReview = { selectedFilter, videoUri ->
                    navController.navigate(Routes.createVideoReviewRoute(selectedFilter, videoUri))
                }
            )
        }

        composable(
            route = Routes.REVIEW,
            arguments = listOf(
                navArgument("filterType") {
                    type = NavType.StringType
                    defaultValue = FilterType.CLASSIC_MASK.name
                },
                navArgument("imageUri") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val filterName = backStackEntry.arguments?.getString("filterType")
            val imageUri = backStackEntry.arguments?.getString("imageUri")
            val filterType = filterName?.let {
                runCatching { FilterType.valueOf(it) }.getOrDefault(FilterType.CLASSIC_MASK)
            } ?: FilterType.CLASSIC_MASK

            ReviewScreen(
                selectedFilter = filterType,
                imageUri = imageUri,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.VIDEO_REVIEW,
            arguments = listOf(
                navArgument("filterType") {
                    type = NavType.StringType
                    defaultValue = FilterType.CLASSIC_MASK.name
                },
                navArgument("videoUri") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val filterName = backStackEntry.arguments?.getString("filterType")
            val videoUri = backStackEntry.arguments?.getString("videoUri")
            val filterType = filterName?.let {
                runCatching { FilterType.valueOf(it) }.getOrDefault(FilterType.CLASSIC_MASK)
            } ?: FilterType.CLASSIC_MASK

            VideoReviewScreen(
                selectedFilter = filterType,
                videoUri = videoUri,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
