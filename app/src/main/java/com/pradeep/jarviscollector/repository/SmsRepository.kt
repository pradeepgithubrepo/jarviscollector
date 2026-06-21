package com.pradeep.jarviscollector.repository

import android.content.Context
import android.provider.Telephony

import com.pradeep.jarviscollector.model.MobileSignal

object SmsRepository {

    fun readRecentSms(
        context: Context
    ): List<MobileSignal> {

        val signals =
            mutableListOf<MobileSignal>()

        val cursor =
            context.contentResolver.query(

                Telephony.Sms.Inbox.CONTENT_URI,

                null,

                null,

                null,

                "date DESC"
            )

        cursor?.use {

            while (
                it.moveToNext()
            ) {

                val sender =
                    it.getString(
                        it.getColumnIndexOrThrow(
                            Telephony.Sms.ADDRESS
                        )
                    ) ?: ""

                val body =
                    it.getString(
                        it.getColumnIndexOrThrow(
                            Telephony.Sms.BODY
                        )
                    ) ?: ""

                val date =
                    it.getLong(
                        it.getColumnIndexOrThrow(
                            Telephony.Sms.DATE
                        )
                    )

                signals.add(

                    MobileSignal(

                        deviceId =
                            "pradeep_phone",

                        source =
                            "sms",

                        sender =
                            sender,

                        message =
                            body,

                        timestamp =
                            date
                    )
                )
            }
        }

        return signals
    }
}