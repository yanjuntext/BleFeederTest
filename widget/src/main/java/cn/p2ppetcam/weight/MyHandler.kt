package cn.p2ppetcam.weight

import android.app.Activity
import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference

open class MyHandler(activity: Activity, looper: Looper) : Handler(looper) {
    val activityWeakReference = WeakReference(activity)
}