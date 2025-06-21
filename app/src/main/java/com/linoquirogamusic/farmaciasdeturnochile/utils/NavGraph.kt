package com.linoquirogamusic.farmaciasdeturnochile.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import com.linoquirogamusic.farmaciasdeturnochile.screens.SplashScreenContent
import com.linoquirogamusic.farmaciasdeturnochile.viewmodels.MapsViewModel


sealed class Screen(val route:String){
    object Splash : Screen("splash_screen")
    object Home : Screen("maps_screen")
}

@Composable
fun SetupNavGraph(navController: NavHostController,
                  context: Context,
                  mapsVm: MapsViewModel,
                  movetoMap: @Composable () -> Unit,
                  onPermissionGranted: () -> Unit){
    NavHost(navController = navController,
        startDestination = Screen.Splash.route){

        composable(route = Screen.Splash.route){
            SplashScreenContent(
                context = context,
                mapsVm = mapsVm,
                onDataExtracted = {
                    navController.popBackStack()
                    navController.navigate(Screen.Home.route)
                },
                onPermissionGranted = {
                    onPermissionGranted()
                })
        }
        composable(route = Screen.Home.route){
            movetoMap()
        }

    }
}