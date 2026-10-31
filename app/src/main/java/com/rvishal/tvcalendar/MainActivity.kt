package com.rvishal.tvcalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.rvishal.tvcalendar.ui.agenda.AgendaScreen
import com.rvishal.tvcalendar.ui.day.DayScreen
import com.rvishal.tvcalendar.ui.month.MonthScreen
import com.rvishal.tvcalendar.ui.settings.SettingsScreen
import com.rvishal.tvcalendar.ui.theme.TvCalendarTheme
import com.rvishal.tvcalendar.ui.week.WeekScreen
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TvCalendarTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav(rememberNavController())
                }
            }
        }
    }
}

@Composable
fun AppNav(nav: NavHostController) {
    NavHost(navController = nav, startDestination = "month") {
        composable("month") { MonthScreen(nav) }
        composable("week") { WeekScreen(nav) }
        composable("agenda") { AgendaScreen(nav) }
        composable(
            "day/{dayStart}",
            arguments = listOf(navArgument("dayStart") { type = NavType.LongType }),
        ) { backStack ->
            DayScreen(nav, backStack.arguments!!.getLong("dayStart"))
        }
        composable("settings") { SettingsScreen(nav) }
    }
}
