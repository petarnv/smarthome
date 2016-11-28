/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.core.audio.test

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.*

import org.eclipse.smarthome.core.audio.*
import org.eclipse.smarthome.core.audio.internal.AudioConsoleCommandExtension
import org.eclipse.smarthome.core.audio.test.mock.ConsoleMock
import org.eclipse.smarthome.io.console.Console
import org.eclipse.smarthome.io.console.extensions.ConsoleCommandExtension
import org.junit.Before
import org.junit.Test

/**
 * OSGi test for {@link AudioConsoleCommandExtension}
 *
 * @author Petar Valchev
 *
 */
public class AudioConsoleTest extends AudioOSGiTest {
    private AudioConsoleCommandExtension audioConsoleCommandExtension
    private Console consoleMock = new ConsoleMock()

    @Before
    public void setUp(){
        registerSink()
        audioConsoleCommandExtension = getAudioConsoleCommandExtension()
    }

    @Test
    public void 'audio console plays file'(){
        audioStream = getFileAudioStream(WAV_FILE_PATH)

        String[] args = [audioConsoleCommandExtension.SUBCMD_PLAY, audioStream.file.getName()]
        audioConsoleCommandExtension.execute(args, consoleMock)

        assertCompatibleFormat()
    }

    @Test
    public void 'audio console plays file for a specified sink'(){
        audioStream = getFileAudioStream(WAV_FILE_PATH)

        String[] args = [audioConsoleCommandExtension.SUBCMD_PLAY, audioSinkMock.getId(), audioStream.file.getName()]
        audioConsoleCommandExtension.execute(args, consoleMock)

        assertCompatibleFormat()
    }

    @Test
    public void 'audio console plays stream'(){
        initializeAudioServlet()

        audioStream = getByteArrayAudioStream(AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_PCM_SIGNED)

        String path = audioServlet.serve(audioStream)
        String url = generateURL(AUDIO_SERVLET_PROTOCOL, AUDIO_SERVLET_HOSTNAME, AUDIO_SERVLET_PORT, path)

        String[] args = [audioConsoleCommandExtension.SUBCMD_STREAM, url]
        audioConsoleCommandExtension.execute(args, consoleMock)

        assertThat "The streamed url was not as expected",
                audioSinkMock.audioStream.getURL(),
                is(equalTo(url))
    }

    @Test
    public void 'audio console plays stream for a specified sink'(){
        initializeAudioServlet()

        audioStream = getByteArrayAudioStream(AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_PCM_SIGNED)

        String path = audioServlet.serve(audioStream)
        String url = generateURL(AUDIO_SERVLET_PROTOCOL, AUDIO_SERVLET_HOSTNAME, AUDIO_SERVLET_PORT, path)

        String[] args = [audioConsoleCommandExtension.SUBCMD_STREAM, audioSinkMock.getId(), url]
        audioConsoleCommandExtension.execute(args, consoleMock)

        assertThat "The streamed url was not as expected",
                audioSinkMock.audioStream.getURL(),
                is(equalTo(url))
    }

    @Test
    public void 'audio console lists sinks'(){
        String[] args = [audioConsoleCommandExtension.SUBCMD_SINKS]
        audioConsoleCommandExtension.execute(args, consoleMock)

        waitForAssert({
            assertThat "The listed sink was not as expected",
                    consoleMock.consoleOutput,
                    is(equalTo(audioSinkMock.getId()))
        })
    }

    @Test
    public void 'audio console lists sources'(){
        registerSource()

        String[] args = [audioConsoleCommandExtension.SUBCMD_SOURCES]
        audioConsoleCommandExtension.execute(args, consoleMock)

        waitForAssert({
            assertThat "The listed source was not as expected",
                    consoleMock.consoleOutput,
                    is(equalTo(audioSourceMock.getId()))
        })
    }

    protected AudioConsoleCommandExtension getAudioConsoleCommandExtension(){
        audioConsoleCommandExtension = new AudioConsoleCommandExtension()
        registerService(audioConsoleCommandExtension, ConsoleCommandExtension.class.getName())

        audioConsoleCommandExtension = getService(ConsoleCommandExtension)
        assertThat "Could not get AudioConsoleCommandExtension",
                audioConsoleCommandExtension,
                is(notNullValue())
        assertThat "Could not get AudioConsoleCommandExtension's usages",
                audioConsoleCommandExtension.getUsages(),
                is(notNullValue())

        return audioConsoleCommandExtension
    }
}
