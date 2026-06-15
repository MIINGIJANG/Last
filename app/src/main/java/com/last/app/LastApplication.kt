package com.last.app

import android.app.Application
import android.content.Context.MODE_PRIVATE
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.last.app.data.repository.LastRepository
import org.osmdroid.config.Configuration

class LastApplication : Application() {

    lateinit var repository: LastRepository
        private set

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ko-KR"))
        initOsmDroid()
        repository = LastRepository(this)
    }

    private fun initOsmDroid() {
        val preferences = getSharedPreferences("osmdroid", MODE_PRIVATE)
        Configuration.getInstance().apply {
            load(this@LastApplication, preferences)
            userAgentValue = packageName
            tileDownloadThreads = 2
        }
    }

    companion object {
        fun repositoryOf(context: android.content.Context): LastRepository {
            return (context.applicationContext as LastApplication).repository
        }
    }
}
