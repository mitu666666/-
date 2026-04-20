package com.kapi.ledgerroast

import android.app.Application
import com.kapi.ledgerroast.reminder.KapiNotification

class KapiLedgerRoastApp : Application() {
    override fun onCreate() {
        super.onCreate()
        KapiNotification.ensureChannel(this)
    }
}
