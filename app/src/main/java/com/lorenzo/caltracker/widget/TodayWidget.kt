package com.lorenzo.caltracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.lorenzo.caltracker.CalTrackerApp
import com.lorenzo.caltracker.MainActivity
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.domain.Nutrients
import com.lorenzo.caltracker.domain.totalNutrients
import com.lorenzo.caltracker.ui.formatWhole
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/** Home-screen widget: today's calories against the goal, and the three macros. Tap opens the app. */
class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as CalTrackerApp).container
        val totals = container.logRepository.observeDay(LocalDate.now()).first().totalNutrients()
        val goals = container.settingsRepository.goals.first()
        provideContent {
            GlanceTheme { Content(totals, goals) }
        }
    }

    @Composable
    private fun Content(totals: Nutrients, goals: Goals?) {
        val onSurface = GlanceTheme.colors.onSurface
        val muted = GlanceTheme.colors.onSurfaceVariant
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Today", style = TextStyle(color = muted, fontSize = 12.sp))
            Text(
                if (goals != null) "${totals.kcal.formatWhole()} / ${goals.kcal.formatWhole()} kcal" else "${totals.kcal.formatWhole()} kcal",
                style = TextStyle(color = onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold),
            )
            if (goals != null) {
                val left = goals.kcal - totals.kcal
                Text(
                    if (left >= 0) "${left.formatWhole()} left" else "${(-left).formatWhole()} over",
                    style = TextStyle(color = if (left >= 0) muted else GlanceTheme.colors.error, fontSize = 13.sp),
                )
                Spacer(GlanceModifier.height(8.dp))
                Row(GlanceModifier.fillMaxWidth()) {
                    Macro("P", totals.protein, goals.protein, PROTEIN, GlanceModifier.defaultWeight())
                    Spacer(GlanceModifier.width(8.dp))
                    Macro("C", totals.carbs, goals.carbs, CARBS, GlanceModifier.defaultWeight())
                    Spacer(GlanceModifier.width(8.dp))
                    Macro("F", totals.fat, goals.fat, FAT, GlanceModifier.defaultWeight())
                }
            }
        }
    }

    @Composable
    private fun Macro(label: String, value: Double, goal: Double, color: ColorProvider, modifier: GlanceModifier) {
        Column(modifier) {
            Text("$label ${value.formatWhole()}/${goal.formatWhole()}", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp))
            Spacer(GlanceModifier.height(2.dp))
            LinearProgressIndicator(
                progress = if (goal > 0) (value / goal).toFloat().coerceIn(0f, 1f) else 0f,
                modifier = GlanceModifier.fillMaxWidth(),
                color = if (goal > 0 && value > goal) GlanceTheme.colors.error else color,
                backgroundColor = GlanceTheme.colors.surfaceVariant,
            )
        }
    }

    companion object {
        // Same macro colours as the app (light / dark steps).
        private val PROTEIN = ColorProvider(day = Color(0xFF2A78D6), night = Color(0xFF3987E5))
        private val CARBS = ColorProvider(day = Color(0xFFEB6834), night = Color(0xFFD95926))
        private val FAT = ColorProvider(day = Color(0xFF1BAF7A), night = Color(0xFF199E70))

        suspend fun refresh(context: Context) = TodayWidget().updateAll(context)
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
