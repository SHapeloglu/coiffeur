package com.kuafor.app.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kuafor.app.ui.screens.LoginScreen
import com.kuafor.app.ui.viewmodel.AuthViewModel

/**
 * Uygulama NavHost'u. Açılışta oturum durumuna göre Login veya Home'a gidilir.
 * Ekranların çoğu henüz yazılmadığı için kalan route'lar placeholder Composable'a bağlı.
 */
@Composable
fun KuaforNavGraph(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    // Oturum durumu AuthRepository.currentUid'den türetilir (AuthViewModel üzerinden).
    val authViewModel: AuthViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            // Oturum bilgisi senkron okunabilir (FirebaseAuth.currentUser), bu yüzden
            // Splash yalnızca tek seferlik yönlendirme yapar.
            LaunchedEffect(Unit) {
                val target = if (authViewModel.uiState.value.loggedIn) {
                    Screen.Home.route
                } else {
                    Screen.Login.route
                }
                navController.navigate(target) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            PlaceholderScreen(title = "Açılış")
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            // TODO: Yakındaki kuaförler listesi (konum izni + getShops/getNearbyShops).
            PlaceholderScreen(
                title = "Ana Sayfa",
                actions = {
                    Button(onClick = { navController.navigate(Screen.Appointments.route) }) {
                        Text("Randevularım")
                    }
                }
            )
        }

        composable(
            route = Screen.ShopDetail.route,
            arguments = listOf(navArgument(NAV_ARG_SHOP_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val shopId = backStackEntry.arguments?.getString(NAV_ARG_SHOP_ID).orEmpty()
            // TODO: Dükkan detayı, berber ve hizmet listesi. Berber seçilince
            // Screen.Booking.createRoute(shopId, barberId) ile randevu ekranına gidilecek.
            PlaceholderScreen(title = "Dükkan", subtitle = "Dükkan ID: $shopId")
        }

        composable(
            route = Screen.Booking.route,
            arguments = listOf(
                navArgument(NAV_ARG_SHOP_ID) { type = NavType.StringType },
                navArgument(NAV_ARG_BARBER_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val shopId = backStackEntry.arguments?.getString(NAV_ARG_SHOP_ID).orEmpty()
            val barberId = backStackEntry.arguments?.getString(NAV_ARG_BARBER_ID).orEmpty()
            // TODO: BookingViewModel'e bağlanacak; tarih seçici ve slot listesi (bkz. TASK.md).
            PlaceholderScreen(
                title = "Randevu",
                subtitle = "Dükkan ID: $shopId, Berber ID: $barberId"
            )
        }

        composable(Screen.Appointments.route) {
            // TODO: Müşterinin randevu listesi (repository.getMyAppointments).
            PlaceholderScreen(title = "Randevularım")
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String? = null,
    actions: @Composable () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        if (subtitle != null) {
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
        }
        actions()
    }
}
