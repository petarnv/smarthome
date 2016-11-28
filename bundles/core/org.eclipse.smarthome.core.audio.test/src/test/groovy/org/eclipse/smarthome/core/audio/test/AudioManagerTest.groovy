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

import org.eclipse.smarthome.config.core.ParameterOption
import org.eclipse.smarthome.core.audio.*
import org.eclipse.smarthome.core.audio.internal.AudioManagerImpl
import org.eclipse.smarthome.core.audio.test.mock.AudioSinkMock
import org.eclipse.smarthome.core.library.types.PercentType
import org.junit.Test

/**
 * OSGi test for {@link AudioManagerImpl}
 *
 * @author Petar Valchev
 *
 */
public class AudioManagerTest extends AudioOSGiTest {
    private enum PlayType{
        STREAM("stream"), FILE("file")

        private String playType

        public PlayType(String playType){
            this.playType = playType
        }

        public String getPlayType(){
            return playType
        }
    }

    @Test
    public void 'audio manager plays byte array audio stream'(){
        audioStream = getByteArrayAudioStream(AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_MP3)
        assertProcessedStream(audioStream, PlayType.STREAM)
    }

    @Test
    public void 'null streams are not processed'(){
        registerSink()

        audioManager.play(null, audioSinkMock.getId())

        waitForAssert({
            assertThat "null stream was processed",
                    audioSinkMock.isStreamProcessed,
                    is(false)
        })
    }

    @Test
    public void 'audio manager plays stream from wav audio files'(){
        audioStream = getFileAudioStream(WAV_FILE_PATH)
        assertProcessedStream(audioStream, PlayType.STREAM)
    }

    @Test
    public void 'audio manager plays stream from mp3 audio files'(){
        audioStream = getFileAudioStream(MP3_FILE_PATH)
        assertProcessedStream(audioStream, PlayType.STREAM)
    }

    @Test
    public void 'audio manager plays wav audio files'(){
        audioStream = getFileAudioStream(WAV_FILE_PATH)
        assertProcessedStream(audioStream, PlayType.FILE)
    }

    @Test
    public void 'audio manager plays mp3 audio files'(){
        audioStream = getFileAudioStream(MP3_FILE_PATH)
        assertProcessedStream(audioStream, PlayType.FILE)
    }

    @Test
    public void 'file is not processed if there is no registered sink'(){
        File file = new File(MP3_FILE_PATH)
        assertThat "The file ${file.getName()} does not exist",
                file.exists(),
                is(true)

        audioSinkMock = new AudioSinkMock()

        audioManager.playFile(file.getName(), audioSinkMock.getId())

        waitForAssert({
            assertThat "the file ${file.getName()} was processed",
                    audioSinkMock.isStreamProcessed,
                    is(false)
        })
    }

    @Test
    public void 'audio manager handles UnsupportedAudioFormatException'(){
        registerSink()

        audioStream = getFileAudioStream(MP3_FILE_PATH)

        audioSinkMock.isUnsupportedAudioFormatExceptionExpected = true
        try{
            audioManager.playFile(audioStream.file.getName(), audioSinkMock.getId())
        } catch (UnsupportedAudioFormatException e){
            fail("An exception $e was thrown, while trying to process a stream")
        }
    }

    @Test
    public void 'audio manager sets the volume of a sink'(){
        registerSink()

        PercentType initialVolume = new PercentType(67)
        PercentType sinkMockVolume = getSinkMockVolume(initialVolume)

        waitForAssert({
            assertThat "the volume of the sink ${audioSinkMock.getId()} was not as expected",
                    sinkMockVolume,
                    is(equalTo(initialVolume))
        })
    }

    @Test
    public void 'the volume of a null sink is zero'(){
        assertThat "the volume was not as expected",
                audioManager.getVolume(null),
                is(equalTo(PercentType.ZERO))
    }

    @Test
    public void 'audio manager sets the volume of not registered sink to zero'(){
        audioSinkMock = new AudioSinkMock()

        PercentType initialVolume = new PercentType(67)
        PercentType sinkMockVolume = getSinkMockVolume(initialVolume)

        waitForAssert({
            assertThat "the volume of the sink ${audioSinkMock.getId()} was not as expected",
                    sinkMockVolume,
                    is(equalTo(PercentType.ZERO))
        })
    }

    @Test
    public void 'setVolume method handles IOException'(){
        registerSink()

        audioSinkMock.isIOExceptionExpected = true

        try{
            audioManager.setVolume(new PercentType(67), audioSinkMock.getId())
        } catch (IOException e){
            fail("An exception $e was thrown, while trying to set the volume of the sink $audioSinkMock.getId()")
        }
    }

    @Test
    public void 'getVolume method handles IOException'(){
        registerSink()

        audioManager.setVolume(new PercentType(67), audioSinkMock.getId())

        audioSinkMock.isIOExceptionExpected = true

        String sinkMockId = audioSinkMock.getId()
        try{
            audioManager.getVolume(sinkMockId)
        } catch (IOException e){
            fail("An exception $e was thrown, while trying to set the volume of the sink $audioSinkMock.getId()")
        }
    }

    @Test
    public void 'source is registered'(){
        assertRegisteredSource(false)
    }

    @Test
    public void 'default source is registered'(){
        assertRegisteredSource(true)
    }

    @Test
    public void 'sink is registered'(){
        assertRegisteredSink(false)
    }

    @Test
    public void 'default sink is registered'(){
        assertRegisteredSink(true)
    }

    @Test
    public void 'sink is added in parameter options'(){
        assertAddedParameterOption(AudioManagerImpl.CONFIG_DEFAULT_SINK)
    }

    @Test
    public void 'source is added in parameter options'(){
        assertAddedParameterOption(AudioManagerImpl.CONFIG_DEFAULT_SOURCE)
    }

    @Test
    public void 'in case of wrong uri, no parameter options are added'(){
        registerSink()

        Collection<ParameterOption> parameterOptions = audioManager.getParameterOptions(URI.create("wrong.uri"), AudioManagerImpl.CONFIG_DEFAULT_SINK, Locale.US)
        assertThat "the parameter options were not as expected",
                parameterOptions,
                is(nullValue())
    }

    @Test
    public void 'audio manager processes multitime streams'(){
        registerSink()
        int streamTimeout = 10
        assertServedStream(streamTimeout)
    }

    @Test
    public void 'audio manager processes one time stream'(){
        registerSink()
        assertServedStream(null)
    }

    @Test
    public void 'audio manager does not process streams if there is no registered sink'(){
        audioSinkMock = new AudioSinkMock()
        int streamTimeout = 10
        assertServedStream(streamTimeout)
    }

    private void assertRegisteredSource(boolean isSourceDefault){
        registerSource()

        if(isSourceDefault){
            audioManager.defaultSource = audioSourceMock.getId()
        } else{
            //just to make sure there is no default source
            audioManager.defaultSource = null
        }

        assertThat "the source ${audioSourceMock.getId()} was not registered",
                audioManager.getSource(),
                is(equalTo(audioSourceMock))
        assertThat "the source ${audioSourceMock.getId()} was not added to the set of sources",
                audioManager.getSourceIds().contains(audioSourceMock.getId()),
                is(true)
        assertThat "the source ${audioSourceMock.getId()} was not added to the set of sources",
                audioManager.getSourceIds(audioSourceMock.getId()).contains(audioSourceMock.getId()),
                is(true)
    }

    private void assertRegisteredSink(boolean isSinkDefault){
        registerSink()

        if(isSinkDefault){
            audioManager.defaultSink = audioSinkMock.getId()
        } else{
            //just to make sure there is no default source
            audioManager.defaultSink = null
        }

        assertThat "the sink ${audioSinkMock.getId()} was not registered",
                audioManager.getSink(),
                is(equalTo(audioSinkMock))
        assertThat "the sink ${audioSinkMock.getId()} was not added to the set of sinks",
                audioManager.getSinkIds().contains(audioSinkMock.getId()),
                is(true)
        assertThat "the sink ${audioSinkMock.getId()} was not added to the set of sinks",
                audioManager.getSinks(audioSinkMock.getId()).contains(audioSinkMock.getId()),
                is(true)
    }

    private void assertProcessedStream(AudioStream audioStream, PlayType playType){
        registerSink()

        waitForAssert({
            assertThat "The format of the audio sink mock was not as expected",
                    audioSinkMock.audioFormat,
                    is(nullValue())
        })

        if(playType.equals(PlayType.STREAM)){
            audioManager.play(audioStream, audioSinkMock.getId())
        } else if(playType.equals(PlayType.FILE)){
            File audioFile = ((FileAudioStream)audioStream).file
            String audioFileName = audioFile.getName()
            audioManager.playFile(audioFileName, audioSinkMock.getId())
        }

        assertCompatibleFormat()
    }

    private PercentType getSinkMockVolume(PercentType initialVolume){
        audioManager.setVolume(initialVolume, audioSinkMock.getId())

        String sinkMockId = audioSinkMock.getId()
        PercentType sinkMockVolume = audioManager.getVolume(sinkMockId)

        return sinkMockVolume
    }

    /**
     * 
     * @param param - either default source or default sink
     */
    private void assertAddedParameterOption(String param){
        String id
        String label
        if(param.equals(AudioManagerImpl.CONFIG_DEFAULT_SINK)){
            registerSink()
            id = audioSinkMock.getId()
            label = audioSinkMock.getLabel()
        } else if(param.equals(AudioManagerImpl.CONFIG_DEFAULT_SOURCE)){
            registerSource()
            id = audioSourceMock.getId()
            label = audioSourceMock.getLabel()
        }

        Collection<ParameterOption> parameterOptions = audioManager.getParameterOptions(URI.create(AudioManagerImpl.CONFIG_URI), param, Locale.US)
        boolean isParameterOptionAdded = false
        for (ParameterOption parameterOption : parameterOptions) {
            if(parameterOption.getValue().equals(id) && parameterOption.getLabel().equals(label)){
                isParameterOptionAdded = true
            }
        }

        assertThat "$param was not added to the parameter options",
                isParameterOptionAdded,
                is(equalTo(true))
    }

    private void assertServedStream(Integer timeInterval){
        initializeAudioServlet()

        String path
        audioStream = getByteArrayAudioStream(AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_PCM_SIGNED)
        if(timeInterval != null){
            path = audioServlet.serve(audioStream, timeInterval)
        } else {
            path = audioServlet.serve(audioStream)
        }
        String url = generateURL(AUDIO_SERVLET_PROTOCOL, AUDIO_SERVLET_HOSTNAME, AUDIO_SERVLET_PORT, path)

        audioManager.stream(url, audioSinkMock.getId())

        if(audioManager.getSink().equals(audioSinkMock)){
            assertThat "The streamed url was not as expected",
                    audioSinkMock.audioStream.getURL(),
                    is(equalTo(url))
        } else {
            assertThat "The sink ${audioSinkMock.getId()} received an unexpected stream",
                    audioSinkMock.audioStream,
                    is(nullValue())
        }
    }
}
