package com.pradeep.jarviscollector.utils

import android.content.Context
import android.os.Environment

import com.pradeep.jarviscollector.model.MobileSignal

import org.json.JSONArray
import org.json.JSONObject

import java.io.File


object JsonExporter {

    fun exportSignals(

        context: Context,

        signals: List<MobileSignal>

    ): String {

        val jsonArray =
            JSONArray()

        signals.forEach {

                signal ->

            val obj =
                JSONObject()

            obj.put(
                "id",
                signal.id
            )

            obj.put(
                "deviceId",
                signal.deviceId
            )

            obj.put(
                "source",
                signal.source
            )

            obj.put(
                "sender",
                signal.sender
            )

            obj.put(
                "message",
                signal.message
            )

            obj.put(
                "timestamp",
                signal.timestamp
            )

            jsonArray.put(
                obj
            )
        }

        val folder = File(

            context.getExternalFilesDir(
                null
            ),

            "jarvis"
        )

        if (
            !folder.exists()
        ) {

            folder.mkdirs()
        }

        val file = File(
            folder,
            "signals.json"
        )

        file.writeText(
            jsonArray.toString(
                4
            )
        )

        return file.absolutePath
    }

    fun exportSignalsAsString(
        signals: List<MobileSignal>
    ): String {

        val jsonArray =
            JSONArray()

        signals.forEach {

                signal ->

            val obj =
                JSONObject()

            obj.put(
                "id",
                signal.id
            )

            obj.put(
                "deviceId",
                signal.deviceId
            )

            obj.put(
                "source",
                signal.source
            )

            obj.put(
                "sender",
                signal.sender
            )

            obj.put(
                "message",
                signal.message
            )

            obj.put(
                "timestamp",
                signal.timestamp
            )

            jsonArray.put(
                obj
            )
        }

        val root =
            JSONObject()

        root.put(
            "generatedAt",
            System.currentTimeMillis()
        )

        root.put(
            "signals",
            jsonArray
        )

        return root.toString(4)
    }

    fun exportHistoricalSignalsAsString(
        signals: List<MobileSignal>
    ): String {
        val jsonArray = JSONArray()
        signals.forEach { signal ->
            val obj = JSONObject()
            obj.put("id", signal.id)
            obj.put("deviceId", signal.deviceId)
            obj.put("source", signal.source)
            obj.put("sender", signal.sender)
            obj.put("message", signal.message)
            obj.put("timestamp", signal.timestamp)
            jsonArray.put(obj)
        }

        val root = JSONObject()
        root.put("type", "historical_backfill")
        root.put("generatedAt", System.currentTimeMillis())
        root.put("signals", jsonArray)

        return root.toString(4)
    }

}