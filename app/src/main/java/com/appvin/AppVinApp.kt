package com.appvin

import android.app.Application
import com.appvin.data.AppDatabase
import com.appvin.data.CellarRepository

class AppVinApp : Application() {
    val repository by lazy { CellarRepository(this, AppDatabase.create(this).cellarDao()) }
}
