/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.tools.analysis.checkstyle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck;

/**
 * Checks if the MANIFEST.MF file contains any "Require-Bundle" entries.
 *
 * @author Petar Valchev
 *
 */
public class RequireBundleCheck extends AbstractFileSetCheck {
    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_KEY = "require.bundle.used";
    public static final String MANIFEST_EXTENSTION = "MF";
    public static final String REQUIRE_BUNDLE_HEADER = "Require-Bundle";

    public RequireBundleCheck() {
        setFileExtensions(MANIFEST_EXTENSTION);
    }

    @Override
    protected void processFiltered(File file, List<String> lines) {
        try {
            // We use Manifest class here instead of ManifestParser,
            // because it is easier to get the content of the headers
            // in the MANIFEST.MF
            Manifest manifest = new Manifest(new FileInputStream(file));
            Attributes attributes = manifest.getMainAttributes();
            String requreBundleHeader = attributes.getValue(REQUIRE_BUNDLE_HEADER);
            if(requreBundleHeader != null){
                log(findLineNumber(lines, requreBundleHeader), MSG_KEY, new Integer(0));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int findLineNumber(List<String> lines, String text) {
        int number = 0;
        for (String line : lines) {
            number++;
            if (line.contains(text)) {
                return number;
            }
        }
        return number;
    }
}
