package com.pradeep.jarviscollector.network

import android.util.Log

import com.pradeep.jarviscollector.model.MobileSignal

import org.json.JSONArray
import org.json.JSONObject

import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL


object JarvisApiClient {

    private const val SERVER_URL =
        "http://YOUR_LAPTOP_IP:8000/mobile/signals"


    suspend fun syncSignals(
        deviceId: String,
        signals: List<MobileSignal>
    ): Boolean {

        return try {

            val payload =
                JSONObject()

            payload.put(
                "device_id",
                deviceId
            )

            val signalArray =
                JSONArray()

            signals.forEach {

                    signal ->

                val signalJson =
                    JSONObject()

                signalJson.put(
                    "source",
                    signal.source
                )

                signalJson.put(
                    "sender",
                    signal.sender
                )

                signalJson.put(
                    "message",
                    signal.message
                )

                signalJson.put(
                    "timestamp",
                    signal.timestamp
                )

                signalArray.put(
                    signalJson
                )
            }

            payload.put(
                "signals",
                signalArray
            )

            val url =
                URL(
                    SERVER_URL
                )

            val connection =
                url.openConnection()
                        as HttpURLConnection

            connection.requestMethod =
                "POST"

            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            connection.doOutput = true

            val writer =
                OutputStreamWriter(
                    connection.outputStream
                )

            writer.write(
                payload.toString()
            )

            writer.flush()
            writer.close()

            val responseCode =
                connection.responseCode

            Log.d(
                "JARVIS_SYNC",
                "Response=$responseCode"
            )

            responseCode == 200

        } catch (
            ex: Exception
        ) {

            ex.printStackTrace()

            false
        }
    }
}