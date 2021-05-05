/*
 * Copyright (C) 2021 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.thibaultbee.streampack.app.configuration

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.util.Size
import androidx.preference.PreferenceManager
import com.github.thibaultbee.streampack.app.R

class Configuration(context: Context) {
    private val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
    private val resources = context.resources
    val video = Video(sharedPref, resources)
    val audio = Audio(sharedPref, resources)
    val muxer = Muxer(sharedPref, resources)
    val endpoint = Endpoint(sharedPref, resources)

    class Video(private val sharedPref: SharedPreferences, private val resources: Resources) {
        var encoder: String = resources.getString(R.string.default_video_encoder)
            get() = sharedPref.getString(resources.getString(R.string.video_encoder_key), field)!!


        var fps: Int = 30
            get() = sharedPref.getString(
                resources.getString(R.string.video_fps_key),
                field.toString()
            )!!.toInt()


        var resolution: Size = Size(1280, 720)
            get() {
                val res = sharedPref.getString(
                    resources.getString(R.string.video_resolution_key),
                    field.toString()
                )!!
                val resArray = res.split("x")
                return Size(
                    resArray[0].toInt(),
                    resArray[1].toInt()
                )
            }


        var bitrate: Int = 1500
            get() = sharedPref.getInt(resources.getString(R.string.video_bitrate_key), field)
    }

    class Audio(private val sharedPref: SharedPreferences, private val resources: Resources) {
        var encoder: String = resources.getString(R.string.default_audio_encoder)
            get() = sharedPref.getString(resources.getString(R.string.audio_encoder_key), field)!!

        var channelConfiguration: Int = 12
            get() = sharedPref.getString(
                resources.getString(R.string.audio_channel_configuration_key),
                field.toString()
            )!!.toInt()

        var bitrate: Int = 128000
            get() = sharedPref.getString(
                resources.getString(R.string.audio_bitrate_key),
                field.toString()
            )!!.toInt()

        var sampleRate: Int = 48000
            get() = sharedPref.getString(
                resources.getString(R.string.audio_sample_rate_key),
                field.toString()
            )!!.toInt()


        var byteFormat: Int = 2
            get() = sharedPref.getString(
                resources.getString(R.string.audio_byte_format_key),
                field.toString()
            )!!.toInt()
    }

    class Muxer(private val sharedPref: SharedPreferences, private val resources: Resources) {
        var service: String = resources.getString(R.string.default_muxer_service)
            get() = sharedPref.getString(resources.getString(R.string.muxer_service_key), field)!!

        var provider: String = resources.getString(R.string.default_muxer_provider)
            get() = sharedPref.getString(resources.getString(R.string.muxer_provider_key), field)!!
    }

    class Endpoint(private val sharedPref: SharedPreferences, private val resources: Resources) {
        val file = File(sharedPref, resources)
        val connection = Connection(sharedPref, resources)

        enum class EndpointType {
            FILE,
            SRT
        }

        val enpointType: EndpointType
            get() {
                return if (sharedPref.getBoolean(
                        resources.getString(R.string.endpoint_type_key),
                        true
                    )
                ) {
                    EndpointType.SRT
                } else {
                    EndpointType.FILE
                }
            }

        class File(
            private val sharedPref: SharedPreferences,
            private val resources: Resources
        ) {
            companion object {
                const val TS_FILE_EXTENSION = ".ts"
            }

            var filename: String = ""
                get() = "${
                    sharedPref.getString(
                        resources.getString(R.string.file_name_key),
                        field
                    )!!
                }$TS_FILE_EXTENSION"
        }

        class Connection(
            private val sharedPref: SharedPreferences,
            private val resources: Resources
        ) {
            var ip: String = ""
                get() = sharedPref.getString(resources.getString(R.string.server_ip_key), field)!!

            var port: Int = 9998
                get() = sharedPref.getString(
                    resources.getString(R.string.server_port_key),
                    field.toString()
                )!!.toInt()

        }
    }

}