package com.yoyuzh.cliplink.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceivedReceiver : BroadcastReceiver() {
    @Inject lateinit var handler: SmsReceivedHandler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handler.handle(
                    Telephony.Sms.Intents.getMessagesFromIntent(intent).map { message ->
                        InboundSmsMessage(
                            sender = message.originatingAddress.orEmpty(),
                            body = message.messageBody.orEmpty(),
                            receivedAtMillis = message.timestampMillis
                        )
                    }
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
