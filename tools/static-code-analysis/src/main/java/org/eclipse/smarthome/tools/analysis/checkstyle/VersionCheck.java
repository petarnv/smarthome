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

import org.apache.ivy.osgi.core.BundleRequirement;
import org.apache.ivy.osgi.core.ManifestParser;

import com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck;

/**
 * Checks if the MANIFEST.MF file has any version constraints on package imports.
 *
 * @author Petar Valchev
 *
 */
public class VersionCheck extends AbstractFileSetCheck {
    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_KEY = "version.used";
    public static final String MANIFEST_EXTENSTION = "MF";
    
    public VersionCheck() {
        setFileExtensions(MANIFEST_EXTENSTION);
    }
    
    @Override
    protected void processFiltered(File file, List<String> lines) {
        try {
            Set<BundleRequirement> imports = ManifestParser.parseManifest(file).getRequirements();
            for(BundleRequirement requirement : imports){
                if(requirement.getVersion() != null){
                    log(findLineNumber(lines, requirement.getName()), MSG_KEY, new Integer(0));
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
