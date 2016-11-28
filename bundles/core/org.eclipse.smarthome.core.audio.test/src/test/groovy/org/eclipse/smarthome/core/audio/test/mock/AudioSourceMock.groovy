/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.core.audio.test.mock

import org.eclipse.smarthome.core.audio.AudioException
import org.eclipse.smarthome.core.audio.AudioFormat
import org.eclipse.smarthome.core.audio.AudioSource
import org.eclipse.smarthome.core.audio.AudioStream

public class AudioSourceMock implements AudioSource {

    @Override
    public String getId() {
        return "testSourceId";
    }

    @Override
    public String getLabel(Locale locale) {
        return "testSourceLabel";
    }

    @Override
    public Set<AudioFormat> getSupportedFormats() {
        return null;
    }

    @Override
    public AudioStream getInputStream(AudioFormat format) throws AudioException {
        return null;
    }
}
