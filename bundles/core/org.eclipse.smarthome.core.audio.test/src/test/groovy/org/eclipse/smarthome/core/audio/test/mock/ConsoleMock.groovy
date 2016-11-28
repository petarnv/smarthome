/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.core.audio.test.mock

import org.eclipse.smarthome.io.console.Console

public class ConsoleMock implements Console{
    public String consoleOutput

    @Override
    public void print(String s) {
    }

    @Override
    public void println(String s) {
        consoleOutput = s
    }

    @Override
    public void printUsage(String s) {
    }
}
