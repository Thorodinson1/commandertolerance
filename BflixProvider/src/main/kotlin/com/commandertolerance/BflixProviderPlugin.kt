package com.commandertolerance

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import android.os.Handler

@CloudstreamPlugin
class BflixProviderPlugin : Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerMainAPI(BflixProvider())
        }

    companion object {
        /**
         * Used to make Runnables work properly on Android 21
         * Otherwise you get:
         * ERROR:D8: Invoke-customs are only supported starting with Android O (--min-api 26)
         **/
        inline fun Handler.postFunction(crossinline function: () -> Unit) {
            this.post(object : Runnable {
                override fun run() {
                    function()
                }
            })
        }
    }
}