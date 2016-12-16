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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck;
import com.puppycrawl.tools.checkstyle.api.MessageDispatcher;

/**
 * Checks if an about.html file exists and it's valid.
 *
 * @author Petar Valchev
 *
 */
public class AboutHtmlCheck extends AbstractFileSetCheck {
    public List<File> validAboutHtmlFiles = new ArrayList<>();
    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String NO_ABOUT_HTML_FILE_MSG_KEY = "no.about.html.file";
    private static final String HTML_EXTENSTION = "html";
    private static final String ABOUT_HTML_FILE_NAME = "about.html";

    public AboutHtmlCheck() {
        setFileExtensions(HTML_EXTENSTION);
    }

    @Override
    protected void processFiltered(File file, List<String> lines) {
        if (file.getName().equals(ABOUT_HTML_FILE_NAME)) {
            try {
                String validAboutHtmlFilePath = "src/test/resources/org/eclipse/smarthome/tools/analysis/checkstyle/test/valid_about_html_file.html";
                File validAboutHtmlFile = new File(validAboutHtmlFilePath);
                
                byte[] fileByteArray = Files.readAllBytes(file.toPath());
                byte[] aboutHtmlFileByteArray = Files.readAllBytes(validAboutHtmlFile.toPath());
                
                if(Arrays.equals(fileByteArray, aboutHtmlFileByteArray)){
                    validAboutHtmlFiles.add(file);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void finishProcessing() {
        if (validAboutHtmlFiles.size() == 0) {
            logMissingFile("/" + ABOUT_HTML_FILE_NAME, ABOUT_HTML_FILE_NAME, NO_ABOUT_HTML_FILE_MSG_KEY);
        }
    }
    
    private void logMissingFile(String filePath, String fileName, String message) {
        final MessageDispatcher dispatcher = getMessageDispatcher();
        dispatcher.fireFileStarted(filePath);
        log(0, message, fileName);
        fireErrors(filePath);
        dispatcher.fireFileFinished(filePath);
    }

}
