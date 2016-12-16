/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.tools.analysis.checkstyle.test;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.eclipse.smarthome.tools.analysis.checkstyle.VersionCheck;
import org.junit.Test;

import com.google.checkstyle.test.base.BaseCheckTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.Configuration;

/**
 * Tests for {@link VersionCheck}
 *
 * @author Petar Valchev
 *
 */
public class VersionCheckTest extends BaseCheckTestSupport {
    @Override
    protected String getPath(String fileName) throws IOException {
        return new File(
                "src/test/resources/org/eclipse/smarthome/tools/analysis/checkstyle/test" + File.separator + fileName)
                        .getCanonicalPath();
    }

    @Test
    public void testManifestFileThatExportsInternalPackage() {
        DefaultConfiguration config = createCheckConfig(VersionCheck.class);
        String testFileName = "VERSION_MANIFEST.MF";
        int lineNumber = 11;

        String[] expectedMessages = { lineNumber + ": " + VersionCheck.MSG_KEY };

        String filePath = null;
        try {
            filePath = getPath(testFileName);
        } catch (IOException e) {
            fail("An exception was thrown, while trying to get the path of the file" + testFileName  + ": " + e);
        }

        Integer[] warnList = { lineNumber };

        try {
            verify(config, filePath, expectedMessages, warnList);
        } catch (Exception e) {
            fail("An exception was thrown, while trying to verify logged message: " + e);
        }
    }

    @Override
    protected DefaultConfiguration createCheckerConfig(Configuration config) {
        DefaultConfiguration dc = new DefaultConfiguration("root");
        dc.addChild(config);
        return dc;
    }
}
