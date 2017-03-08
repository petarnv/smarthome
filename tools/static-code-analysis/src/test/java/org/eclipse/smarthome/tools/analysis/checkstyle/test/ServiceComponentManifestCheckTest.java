/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.tools.analysis.checkstyle.test;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.smarthome.tools.analysis.checkstyle.ServiceComponentManifestCheck;
import org.eclipse.smarthome.tools.analysis.checkstyle.api.AbstractStaticCheckTest;
import org.junit.BeforeClass;
import org.junit.Test;

import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.utils.CommonUtils;

/**
 * Tests for {@link ServiceComponentManifestCheck}
 *
 * @author Aleksandar Kovachev - Initial contribution
 * @author Petar Valchev - Changed the verifyServiceComponentHeader() method and some of the test methods
 *
 */
public class ServiceComponentManifestCheckTest extends AbstractStaticCheckTest {
    private static final String CHECK_TEST_DIRECTORY = "serviceComponentManifestCheckTest";
    private static final String MANIFEST_RELATIVE_PATH = "META-INF" + File.separator + "MANIFEST.MF";
    
    private static DefaultConfiguration config;

    @BeforeClass
    public static void createConfiguration() {
        config = createCheckConfig(ServiceComponentManifestCheck.class);
        config.addAttribute("excludedSubfolders", "blueprint");
        config.addAttribute("excludedSubfolders", "app");
    }

    @Test
    public void testWrongServicesDirectoryInManifest() throws Exception {
        int lineNumber = 11;
        String[] expectedMessages = generateExpectedMessages(
                lineNumber, String.format(ServiceComponentManifestCheck.WRONG_DIRECTORY_MESSAGE, "wrong_directory"));

        verifyServiceComponentHeader("wrong_service_directory_in_manifest", expectedMessages);
    }
    
    @Test
    public void testNonExistentServiceDirectory() throws Exception {
        int lineNumber = 11;
        String[] expectedMessages = generateExpectedMessages(
                lineNumber, String.format(ServiceComponentManifestCheck.WRONG_DIRECTORY_MESSAGE, "non_existent_directory"));

        verifyServiceComponentHeader("non_existent_service_directory_in_manifest", expectedMessages);
    }

    @Test
    public void testWrongServiceExtensionsInManifest() throws Exception {
        int lineNumber = 11;
        String[] expectedMessages = generateExpectedMessages(
                lineNumber, String.format(ServiceComponentManifestCheck.WRONG_EXTENSION_MESSAGE, "htmlService.html"),
                lineNumber, String.format(ServiceComponentManifestCheck.WRONG_EXTENSION_MESSAGE, "txtService.txt"));

        verifyServiceComponentHeader("wrong_service_extension", expectedMessages);
    }

    @Test
    public void testMissingServiceComponentHeaderInManifest() throws Exception {
        int lineNumber = 0;
        String[] expectedMessages = generateExpectedMessages(
                lineNumber, String.format(ServiceComponentManifestCheck.NOT_INCLUDED_SERVICE_MESSAGE, "testServiceFromSubFolder.xml"),
                lineNumber, String.format(ServiceComponentManifestCheck.NOT_INCLUDED_SERVICE_MESSAGE, "testServiceOne.xml"),
                lineNumber, String.format(ServiceComponentManifestCheck.NOT_INCLUDED_SERVICE_MESSAGE, "testServiceTwo.xml"));

        verifyServiceComponentHeader("missing_service_component_in_manifest", expectedMessages);
    }

    @Test
    public void testMissingServicesInManifest() throws Exception {
        int lineNumber = 11;
        String[] expectedMessages = generateExpectedMessages(
                lineNumber, String.format(ServiceComponentManifestCheck.NOT_INCLUDED_SERVICE_MESSAGE, "testServiceFromSubFolder.xml"),
                lineNumber, String.format(ServiceComponentManifestCheck.NOT_INCLUDED_SERVICE_MESSAGE, "testServiceThree.xml"),
                lineNumber, ServiceComponentManifestCheck.BEST_APPROACH_MESSAGE);
        
        verifyServiceComponentHeader("not_included_services_in_manifest", expectedMessages);
    }

    @Test
    public void testManifestExplicitlyIncludeServices() throws Exception {
        int lineNumber = 11;
        String[] expectedMessages = generateExpectedMessages(
                lineNumber, ServiceComponentManifestCheck.BEST_APPROACH_MESSAGE);
        
        verifyServiceComponentHeader("explicitly_included_services_in_manifest", expectedMessages);
    }

    @Test
    public void testManifestRegexIncludedServices() throws Exception {
        int lineNumber = 11;
        String[] expectedMessages = generateExpectedMessages(
                lineNumber, ServiceComponentManifestCheck.BEST_APPROACH_MESSAGE);
        
        verifyServiceComponentHeader("regex_included_service_in_manifest", expectedMessages);
    }
    
    @Test
    public void testNotMatchingRegex() throws Exception{
        int lineNumber = 11;
        String[] expectedMessages = generateExpectedMessages(
                lineNumber, String.format(ServiceComponentManifestCheck.NOT_MATCHING_REGEX_MESSAGE, "nonExistentService*.xml"),
                lineNumber, ServiceComponentManifestCheck.BEST_APPROACH_MESSAGE,
                lineNumber, String.format(ServiceComponentManifestCheck.NOT_INCLUDED_SERVICE_MESSAGE, "testServiceOne.xml"),
                lineNumber, String.format(ServiceComponentManifestCheck.NOT_INCLUDED_SERVICE_MESSAGE, "testServiceTwo.xml"));
        
        verifyServiceComponentHeader("not_matching_regex_in_manifest", expectedMessages);
    }

    @Test
    public void testServicesInSubdirectory() throws Exception {
        int lineNumber = 11;
        String[] expectedMessages = generateExpectedMessages(
                lineNumber, String.format(ServiceComponentManifestCheck.WRONG_DIRECTORY_MESSAGE, "subdirectory"));
        
        verifyServiceComponentHeader("subdirectory_services", expectedMessages);
    }

    @Test
    public void testExplicitlyIncludedSubfolderServices() throws Exception {
        int lineNumber = 11;
        String[] expectedMessages = generateExpectedMessages(
                lineNumber, String.format(ServiceComponentManifestCheck.WRONG_DIRECTORY_MESSAGE, "subdirectory"), 
                lineNumber, String.format(ServiceComponentManifestCheck.WRONG_DIRECTORY_MESSAGE, "subdirectory"), 
                lineNumber, ServiceComponentManifestCheck.BEST_APPROACH_MESSAGE);

        verifyServiceComponentHeader("included_subfolder_services", expectedMessages);
    }
    
    @Test
    public void testNonExistentService() throws Exception {
        int lineNumber = 11;
        String[] expectedMessages = generateExpectedMessages(
                lineNumber, ServiceComponentManifestCheck.BEST_APPROACH_MESSAGE,
                lineNumber, String.format(ServiceComponentManifestCheck.NOT_EXISTING_SERVICE_MESSAGE, "testServiceFour.xml"));
        
        verifyServiceComponentHeader("non_existent_service_in_manifest", expectedMessages);
    }

    @Test
    public void testCorrectlyIncludedServicesInManifest() throws Exception {
        String[] expectedMessages = CommonUtils.EMPTY_STRING_ARRAY;
        verifyServiceComponentHeader("correctly_included_services_in_manifest", expectedMessages);
    }

    @Test
    public void testExcludedServicesDirectory() throws Exception {
        int lineNumber = 11;
        
        String[] expectedMessages = generateExpectedMessages(
                lineNumber, String.format(ServiceComponentManifestCheck.NOT_EXISTING_SERVICE_MESSAGE, "app.xml"),
                lineNumber, String.format(ServiceComponentManifestCheck.NOT_EXISTING_SERVICE_MESSAGE, "blueprint.xml"),
                lineNumber, String.format(ServiceComponentManifestCheck.WRONG_DIRECTORY_MESSAGE, "app"), 
                lineNumber, String.format(ServiceComponentManifestCheck.WRONG_DIRECTORY_MESSAGE, "blueprint"), 
                lineNumber, ServiceComponentManifestCheck.BEST_APPROACH_MESSAGE);
        verifyServiceComponentHeader("excluded_services_directory", expectedMessages);
    }

    @Test
    public void testRepeatedService() throws Exception {
        int lineNumber = 11;
        String[] expectedMessages = generateExpectedMessages(
                lineNumber, ServiceComponentManifestCheck.REPEATED_SERVICE_MESSAGE);
        
        verifyServiceComponentHeader("repeated_service_in_manifest", expectedMessages);
    }

    @Override
    protected DefaultConfiguration createCheckerConfig(Configuration config) {
        DefaultConfiguration configParent = new DefaultConfiguration("root");
        configParent.addChild(config);
        return configParent;
    }

    private void verifyServiceComponentHeader(String testDirectoryName, String[] expectedMessages) throws Exception {
        String testDirectoryPath = getPath(CHECK_TEST_DIRECTORY + File.separator + testDirectoryName);
        File testDirectory = new File(testDirectoryPath);
        String testFilePath = testDirectory.getPath() + File.separator + MANIFEST_RELATIVE_PATH;
        File[] testFiles = listFilesForDirectory(testDirectory, new ArrayList<File>());

        verify(createChecker(config), testFiles, testFilePath, expectedMessages);
    }
}
