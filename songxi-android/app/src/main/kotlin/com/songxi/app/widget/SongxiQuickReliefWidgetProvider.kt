package com.songxi.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.songxi.app.MainActivity
import com.songxi.app.R
import com.songxi.app.SongxiIntents

/**
 * 4x2 Xuan-paper Home-Screen Widget ("松息 · 今日一息桌面微件").
 * Launches [MainActivity] with [SongxiIntents.EXTRA_FROM_REMINDER_QUICK_START] = true,
 * bypassing splash ads and entering the 90-second seated sigh relief routine in 1 tap.
 */
class SongxiQuickReliefWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(widgetId, buildRemoteViews(context, widgetId))
        }
    }

    companion object {
        fun buildQuickStartIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(SongxiIntents.EXTRA_FROM_REMINDER_QUICK_START, true)
            }

        fun buildRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
            val launchIntent = buildQuickStartIntent(context)
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return RemoteViews(context.packageName, R.layout.widget_songxi_quick_relief).apply {
                setOnClickPendingIntent(R.id.widget_root_container, pendingIntent)
                setOnClickPendingIntent(R.id.widget_start_cta, pendingIntent)
            }
        }

        fun requestRefreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, SongxiQuickReliefWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                for (id in ids) {
                    manager.updateAppWidget(id, buildRemoteViews(context, id))
                }
            }
        }
    }
}
