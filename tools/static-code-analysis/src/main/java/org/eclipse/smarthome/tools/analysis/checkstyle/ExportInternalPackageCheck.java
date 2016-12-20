/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.tools.analysis.checkstyle;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Set;

import org.apache.ivy.osgi.core.ExportPackage;
import org.apache.ivy.osgi.core.ManifestParser;

import com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

/**
 * Checks if a manifest file exports internal packages.
 *
 * @author Svilen Valkanov
 *
 */
public class ExportInternalPackageCheck extends AbstractFileSetCheck {
    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_KEY = "exported.internal.package";
    public static final String MANIFEST_EXTENSTION = "MF";

    public ExportInternalPackageCheck() {
        setFileExtensions(MANIFEST_EXTENSTION);
    }

    @Override
    public void beginProcessing(String charset) {
        setFileExtensions(MANIFEST_EXTENSTION);
    }
    
    @Override
    protected void processFiltered(File file, List<String> lines) throws CheckstyleException {
        try {
            Set<ExportPackage> exports = ManifestParser.parseManifest(file).getExports();
            for(ExportPackage export : exports){
                if(export.toString().contains(".internal")){
                    log(findLineNumber(lines, export.toString()), MSG_KEY, new Integer(0));
                }
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
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
