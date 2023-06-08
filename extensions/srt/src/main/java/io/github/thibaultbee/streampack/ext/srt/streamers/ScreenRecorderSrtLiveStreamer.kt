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
package io.github.thibaultbee.streampack.ext.srt.streamers

import android.app.Service
import android.content.Context
import io.github.thibaultbee.streampack.data.BitrateRegulatorConfig
import io.github.thibaultbee.streampack.ext.srt.internal.endpoints.SrtProducer
import io.github.thibaultbee.streampack.ext.srt.regulator.srt.SrtBitrateRegulator
import io.github.thibaultbee.streampack.ext.srt.services.ScreenRecorderSrtLiveService
import io.github.thibaultbee.streampack.ext.srt.streamers.interfaces.ISrtLiveStreamer
import io.github.thibaultbee.streampack.internal.muxers.ts.TSMuxer
import io.github.thibaultbee.streampack.internal.muxers.ts.data.TsServiceInfo
import io.github.thibaultbee.streampack.internal.utils.Scheduler
import io.github.thibaultbee.streampack.internal.utils.extensions.defaultTsServiceInfo
import io.github.thibaultbee.streampack.listeners.OnConnectionListener
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import io.github.thibaultbee.streampack.regulator.IBitrateRegulatorFactory
import io.github.thibaultbee.streampack.streamers.bases.BaseScreenRecorderStreamer
import io.github.thibaultbee.streampack.streamers.live.BaseScreenRecorderLiveStreamer

/**
 * [BaseScreenRecorderStreamer] that sends microphone and screen frames to a remote Secure Reliable
 * Transport (SRT) device.
 * To run this streamer while application is on background, you have to extend a [Service].
 * To simplify the integration, a service is provided in [ScreenRecorderSrtLiveService].
 *
 * As an example, see `demo-screenrecorder`.
 *
 * @param context application context
 * @param enableAudio [Boolean.true] to also capture audio. False to disable audio capture.
 * @param tsServiceInfo MPEG-TS service description
 * @param bitrateRegulatorFactory a [IBitrateRegulatorFactory] implementation. Use it to customized bitrate regulator.  If bitrateRegulatorConfig is not null, bitrateRegulatorFactory must not be null.
 * @param bitrateRegulatorConfig bitrate regulator configuration. If bitrateRegulatorFactory is not null, bitrateRegulatorConfig must not be null.
 * @param initialOnErrorListener initialize [OnErrorListener]
 * @param initialOnConnectionListener initialize [OnConnectionListener]
 */
class ScreenRecorderSrtLiveStreamer(
    context: Context,
    enableAudio: Boolean = true,
    tsServiceInfo: TsServiceInfo = context.defaultTsServiceInfo,
    bitrateRegulatorFactory: IBitrateRegulatorFactory? = null,
    bitrateRegulatorConfig: BitrateRegulatorConfig? = null,
    initialOnErrorListener: OnErrorListener? = null,
    initialOnConnectionListener: OnConnectionListener? = null
) : BaseScreenRecorderLiveStreamer(
    context = context,
    enableAudio = enableAudio,
    muxer = TSMuxer().apply { addService(tsServiceInfo) },
    endpoint = SrtProducer(),
    initialOnErrorListener = initialOnErrorListener,
    initialOnConnectionListener = initialOnConnectionListener
),
    ISrtLiveStreamer {

    /**
     * Bitrate regulator. Calls regularly by [scheduler]. Don't call it otherwise or you might break regulation.
     */
    private val bitrateRegulator = bitrateRegulatorConfig?.let { config ->
        bitrateRegulatorFactory?.newBitrateRegulator(
            config,
            { settings.video.bitrate = it },
            { settings.audio.bitrate = it }
        ) as SrtBitrateRegulator
    }

    /**
     * Scheduler for bitrate regulation
     */
    private val scheduler = Scheduler(500) {
        bitrateRegulator?.update(srtProducer.stats, settings.video.bitrate, settings.audio.bitrate)
            ?: throw UnsupportedOperationException("Scheduler runs but no bitrate regulator set")
    }

    private val srtProducer = endpoint as SrtProducer

    override fun getBitrate(): Long {
        return srtProducer.stats.byteSent
    }

    /**
     * Get/set SRT stream ID.
     * **See:** [SRT Socket Options](https://github.com/Haivision/srt/blob/master/docs/API/API-socket-options.md#srto_streamid)
     */
    override var streamId: String
        /**
         * Get SRT stream ID
         * @return stream ID
         */
        get() = srtProducer.streamId
        /**
         * @param value stream ID
         */
        set(value) {
            srtProducer.streamId = value
        }

    /**
     * Get/set SRT passphrase.
     * **See:** [SRT Socket Options](https://github.com/Haivision/srt/blob/master/docs/API/API-socket-options.md#srto_passphrase)
     */
    override var passPhrase: String
        /**
         * Get SRT passphrase
         * @return passphrase
         */
        get() = srtProducer.passPhrase
        /**
         * @param value passphrase
         */
        set(value) {
            srtProducer.passPhrase = value
        }

    /**
     * Get/set bidirectional latency in milliseconds.
     * **See:** [SRT Socket Options](https://github.com/Haivision/srt/blob/master/docs/API/API-socket-options.md#SRTO_LATENCY)
     */
    override var latency: Int
        /**
         * Get latency in milliseconds
         * @return latency
         */
        get() = srtProducer.latency
        /**
         * @param value latency in milliseconds
         */
        set(value) {
            srtProducer.latency = value
        }

    /**
     * Connect to an SRT server.
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @param ip server ip
     * @param port server port
     * @throws Exception if connection has failed or configuration has failed
     */
    override suspend fun connect(ip: String, port: Int) {
        srtProducer.connect(ip, port)
    }

    /**
     * Same as [BaseScreenRecorderLiveStreamer.startStream] but also starts bitrate regulator.
     */
    override fun startStream() {
        if (bitrateRegulator != null) {
            scheduler.start()
        }
        super.startStream()
    }

    /**
     * Same as [BaseScreenRecorderLiveStreamer.startStream] but also starts bitrate regulator.
     */
    override suspend fun startStream(url: String) {
        if (bitrateRegulator != null) {
            scheduler.start()
        }
        super.startStream(url)
    }

    /**
     * Connect to an SRT server and start stream.
     * Same as calling [connect], then [startStream].
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @param ip server ip
     * @param port server port
     * @throws Exception if connection has failed or configuration has failed or [startStream] has failed too.
     */
    override suspend fun startStream(ip: String, port: Int) {
        connect(ip, port)
        startStream()
    }

    /**
     * Same as [BaseScreenRecorderLiveStreamer.stopStream] but also stops bitrate regulator.
     */
    override fun stopStream() {
        scheduler.cancel()
        super.stopStream()
    }
}