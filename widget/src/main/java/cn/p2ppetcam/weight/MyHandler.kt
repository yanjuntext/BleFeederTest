package cn.p2ppetcam.weight

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference

open class MyHandler(activity: Context, looper: Looper) : Handler(looper) {
    val activityWeakReference = WeakReference(activity)
}