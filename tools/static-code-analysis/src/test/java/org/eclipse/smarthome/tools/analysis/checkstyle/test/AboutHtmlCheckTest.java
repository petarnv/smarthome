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

import org.eclipse.smarthome.tools.analysis.checkstyle.AboutHtmlCheck;
import org.junit.Test;

import com.puppycrawl.tools.checkstyle.BaseCheckTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.utils.CommonUtils;

/**
 * Tests for {@link AboutHtmlCheck}
 *
 * @author Petar Valchev
 *
 */
public class AboutHtmlCheckTest extends BaseCheckTestSupport {
    private final String TEST_RESOURCES_DIRECTORY =  "src/test/resources/org/eclipse/smarthome/tools/analysis/checkstyle/test/";
    private final String FILE_PATH = "/about.html";
    private String[] expectedMessages = new String []{"0: no.about.html.file"};
    
    @Test
    public void testNotExistentAboutHtmlFile() {
        DefaultConfiguration config = createCheckConfig(AboutHtmlCheck.class);
        
        String directoryPath = TEST_RESOURCES_DIRECTORY + "not_existent_about_html_directory";
        
        File dir = new File(directoryPath);
        
        try {
            verify(createChecker(config), dir.listFiles(), FILE_PATH, expectedMessages);
        } catch (Exception e) {
            fail("An exception was thrown, while trying to verify logged message: " + e);
        }
    }
    
    @Test
    public void testValidAboutHtmlFile() {
        DefaultConfiguration config = createCheckConfig(AboutHtmlCheck.class);
        
        String directoryPath = TEST_RESOURCES_DIRECTORY + "valid_about_html_directory";
        
        File dir = new File(directoryPath);
        
        String[] expectedMessages = CommonUtils.EMPTY_STRING_ARRAY;
        try {
            verify(createChecker(config), dir.listFiles(), FILE_PATH, expectedMessages);
        } catch (Exception e) {
            fail("An exception was thrown, while trying to verify logged message: " + e);
        }
    }
    
    @Test
    public void testNotValidAboutHtmlFile() {
        DefaultConfiguration config = createCheckConfig(AboutHtmlCheck.class);
        
        String directoryPath = TEST_RESOURCES_DIRECTORY + "not_valid_about_html_directory";
        
        File dir = new File(directoryPath);
        
        try {
            verify(createChecker(config), dir.listFiles(), FILE_PATH, expectedMessages);
        } catch (Exception e) {
            fail("An exception was thrown, while trying to verify logged message: " + e);
        }
    }
    
    @Test
    public void testAboutHtmlFileWithInvalidName() {
        DefaultConfiguration config = createCheckConfig(AboutHtmlCheck.class);
        
        String directoryPath = TEST_RESOURCES_DIRECTORY + "not_valid_name_about_html_directory";
        
        File dir = new File(directoryPath);
        
        try {
            verify(createChecker(config), dir.listFiles(), FILE_PATH, expectedMessages);
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
