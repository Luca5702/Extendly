package com.songxi.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.songxi.app.widget.SongxiQuickReliefWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SongxiQuickReliefWidgetTest {

    @Test
    fun quickReliefWidgetBuildsSplashExempt90sDeepLinkAndRemoteViews() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val intent = SongxiQuickReliefWidgetProvider.buildQuickStartIntent(context)
        assertTrue(intent.getBooleanExtra(SongxiIntents.EXTRA_FROM_REMINDER_QUICK_START, false))
        assertEquals(MainActivity::class.java.name, intent.component?.className)

        val remoteViews = SongxiQuickReliefWidgetProvider.buildRemoteViews(context, appWidgetId = 101)
        assertNotNull(remoteViews)
        assertEquals(R.layout.widget_songxi_quick_relief, remoteViews.layoutId)
    }
}
