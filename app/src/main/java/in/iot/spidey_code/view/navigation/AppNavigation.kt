package `in`.iot.spidey_code.view.navigation

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
import `in`.iot.spidey_code.view.screens.GearSelectionScreen
import `in`.iot.spidey_code.view.screens.ReviewScreen

object Routes {
    const val GEAR_SELECTION = "gear_selection"
    const val CAMERA = "camera/{filterType}"
    const val REVIEW = "review/{filterType}"

    fun createCameraRoute(filterType: FilterType): String = "camera/${filterType.name}"
    fun createReviewRoute(filterType: FilterType): String = "review/${filterType.name}"
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.GEAR_SELECTION,
        modifier = modifier
    ) {
        composable(route = Routes.GEAR_SELECTION) {
            GearSelectionScreen(
                onNavigateToCamera = { selectedFilter ->
                    navController.navigate(Routes.createCameraRoute(selectedFilter))
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
                onNavigateToReview = { selected ->
                    navController.navigate(Routes.createReviewRoute(selected))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.REVIEW,
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

            ReviewScreen(
                selectedFilter = filterType,
                onNavigateToGearSelection = {
                    navController.navigate(Routes.GEAR_SELECTION) {
                        popUpTo(Routes.GEAR_SELECTION) { inclusive = true }
                    }
                }
            )
        }
    }
}
