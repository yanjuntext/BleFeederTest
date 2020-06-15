package com.jk.blefeeder.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.lang.Exception

object LocalHelper {

    val LOCAL_INTENT = Settings.ACTION_LOCATION_SOURCE_SETTINGS

    fun isLocationEnable(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                val locationMode =
                    Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE);
                locationMode != Settings.Secure.LOCATION_MODE_OFF
            } catch (e: Exception) {
                false
            }
        } else {
            !Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.LOCATION_PROVIDERS_ALLOWED
            ).isNullOrBlank()
        }
    }

}