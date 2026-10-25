package io.github.filipembcruz.banca.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.filipembcruz.banca.data.settings.AppSettings
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WorkScheduler {

    private const val REFRESH = "banca-refresh"
    private const val DIGEST = "banca-digest"

    fun sync(context: Context, s: AppSettings) {
        val wm = WorkManager.getInstance(context)
        if (s.refreshMinutes <= 0) {
            wm.cancelUniqueWork(REFRESH)
        } else {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (s.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresCharging(s.chargingOnly)
                .setRequiresBatteryNotLow(s.pauseOnSaver)
                .build()
            val request = PeriodicWorkRequestBuilder<RefreshWorker>(s.refreshMinutes.coerceAtLeast(15).toLong(), TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            wm.enqueueUniquePeriodicWork(REFRESH, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
        scheduleDigest(context, s)
    }

    fun scheduleDigest(context: Context, s: AppSettings) {
        val wm = WorkManager.getInstance(context)
        if (!s.notifications || !s.digest || s.digestTimes.isEmpty()) {
            wm.cancelUniqueWork(DIGEST)
            return
        }
        val delay = millisUntilNext(s.digestTimes)
        val request = OneTimeWorkRequestBuilder<DigestWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        wm.enqueueUniqueWork(DIGEST, ExistingWorkPolicy.REPLACE, request)
    }

    fun refreshNow(context: Context) {
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<RefreshWorker>().build())
    }

    fun millisUntilNext(times: List<Int>, now: Calendar = Calendar.getInstance()): Long {
        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val sorted = times.sorted()
        val next = sorted.firstOrNull { it > nowMin }
        val minutes = if (next != null) next - nowMin else 24 * 60 - nowMin + sorted.first()
        return minutes * 60_000L - now.get(Calendar.SECOND) * 1000L
    }

    fun inQuietHours(start: Int, end: Int, now: Calendar = Calendar.getInstance()): Boolean {
        val m = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return if (start <= end) m in start until end else m >= start || m < end
    }
}
