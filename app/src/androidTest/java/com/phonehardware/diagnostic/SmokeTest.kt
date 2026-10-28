package com.phonehardware.diagnostic

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @Test fun applicationContextIsPresent() {
        val ctx=InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull(ctx.getSystemService(android.content.Context.SENSOR_SERVICE))
    }
}
