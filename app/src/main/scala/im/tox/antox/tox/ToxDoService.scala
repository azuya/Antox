
package im.tox.antox.tox

import android.app.Service
import android.content.{Context, Intent}
import android.net.ConnectivityManager
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log

class ToxDoService extends Service() {

  private var serviceThread: Thread = _

  private var keepRunning: Boolean = true

  override def onCreate() {
    if (!ToxSingleton.isInited) {
      ToxSingleton.initTox(getApplicationContext)
      Log.d("ToxDoService", "Initting ToxSingleton")
    }
    keepRunning = true
    val start = new Runnable() {

      override def run() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext)
        while (keepRunning) {
          val connManager = getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnectivityManager]
          val wifiOnly = preferences.getBoolean("wifi_only", true)
          val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
          val connected = !(wifiOnly && !mWifi.isConnected)

          if (!connected) {
            try {
              Thread.sleep(10000)
            } catch {
              case e: Exception =>
            }
          } else {
            try {
              Thread.sleep(50)
              ToxSingleton.tox.iteration()
            } catch {
              case e: Exception =>
            }
          }
        }
      }
    }
    serviceThread = new Thread(start)
    serviceThread.start()
  }

  override def onBind(intent: Intent): IBinder = null

  override def onStartCommand(intent: Intent, flags: Int, id: Int): Int = Service.START_STICKY

  override def onDestroy() {
    super.onDestroy()
    keepRunning = false
    serviceThread.interrupt()
    ToxSingleton.save()
    ToxSingleton.isInited = false
    Log.d("ToxDoService", "onDestroy() called")
  }
}
