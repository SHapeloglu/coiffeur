package com.kuafor.app.ui.navigation

/** Navigasyon argüman anahtarları. Route string'leri yalnızca bu dosyada tanımlanır. */
const val NAV_ARG_SHOP_ID = "shopId"
const val NAV_ARG_BARBER_ID = "barberId"

/** Tüm ekranların route tanımları. Yeni ekran eklerken buraya ekleyin. */
sealed class Screen(val route: String) {

    object Splash : Screen("splash")

    object Home : Screen("home")

    object ShopDetail : Screen("shop/{$NAV_ARG_SHOP_ID}") {
        fun createRoute(shopId: String) = "shop/$shopId"
    }

    object Booking : Screen("booking/{$NAV_ARG_SHOP_ID}/{$NAV_ARG_BARBER_ID}") {
        fun createRoute(shopId: String, barberId: String) = "booking/$shopId/$barberId"
    }

    object Appointments : Screen("appointments")
}
