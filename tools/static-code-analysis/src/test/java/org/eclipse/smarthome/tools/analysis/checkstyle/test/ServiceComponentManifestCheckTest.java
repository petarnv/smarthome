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
 * @author Petar Valchev - Changed the verifyServiceComponentHeader() method
 *
 */
public class ServiceComponentManifestCheckTest extends AbstractStaticCheckTest {
    private static final String CHECK_TEST_DIRECTORY = "serviceComponentManifestCheckTest" + File.separator;
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
                lineNumber, String.format(ServiceComponentManifestCheck.WRONG_DIRECTORY_MESSAGE, "TEST")/*, 
                lineNumber, String.format(ServiceComponentManifestCheck.WRONG_DIRECTORY_MESSAGE, "OSG-INF")*/);

        verifyServiceComponentHeader("manifest_wrong_services_directory", expectedMessages);
    }

    @Test
    public void testServicesExtensionsInManifest() throws Exception {
        int lineNumber = 11;
        String[] expectedMessages = generateExpectedMessages(
                lineNumber, ServiceComponentManifestCheck.WRONG_EXTENSION_MESSAGE);

        verifyServiceComponentHeader("manifest_wrong_services_extensions", expectedMessages);
    }

    @Test
    public void testManifestNoServiceComponent() throws Exception {
        int lineNumber = 0;
        String[] expectedMessages = generateExpectedMessages(
                lineNumber, String.format(ServiceComponentManifestCheck.NOT_INCLUDED_SERVICE_MESSAGE, "serviceFromSubFolder.xml"),
                lineNumber, String.format(ServiceComponentManifestCheck.NOT_INCLUDED_SERVICE_MESSAGE, "serviceTestFileOne.xml"),
                lineNumber, String.format(ServiceComponentManifestCheck.NOT_INCLUDED_SERVICE_MESSAGE, "serviceTestFileTwo.xml"));

        verifyServiceComponentHeader("manifest_no_service_component", expectedMessages);
    }

    @Test
    public void testManifestIncludedServices() throws Exception {
        int lineNumber = 11;
        String[] expectedMessages = generateExpectedMessages(
                /*lineNumber, String.format(ServiceComponentManifestCheck.NOT_INCLUDED_SERVICE_MESSAGE, "serviceFromSubFolder.xml"),*/
                lineNumber, String.format(ServiceComponentManifestCheck.NOT_INCLUDED_SERVICE_MESSAGE, "serviceTestFileThree.xml"));
        
        verifyServiceComponentHeader("manifest_not_included_services", expectedMessages);
    }

    @Test
    public void testManifestSeparatelyIncludeServices() throws Exception {
        int lineNumber = 11;
        String[] expectedMessages = generateExpectedMessages(
                lineNumber, ServiceComponentManifestCheck.WARNING_MESSAGE);
        
        verifyServiceComponentHeader("manifest_separately_included_services", expectedMessages);
    }

    // TODO - separate tests for this case
    @Test
    public void testManifestRegexIncludedServices() throws Exception {
        int lineNumber = 11;
        String[] expectedMessages = generateExpectedMessages(
                /*lineNumber, String.format(ServiceComponentManifestCheck.NOT_EXISTING_SERVICE_MESSAGE, "service.xml"),*/
                lineNumber, ServiceComponentManifestCheck.REGEX_INCLUDED_SERVICE,
                lineNumber, ServiceComponentManifestCheck.WARNING_MESSAGE/*,
                lineNumber, ServiceComponentManifestCheck.REGEX_INCLUDED_SERVICE*/);
        
        verifyServiceComponentHeader("manifest_regex_included_services", expectedMessages);
    }

    @Test
    public void testServicesInSubFolder() throws Exception {
        int lineNumber = 11;
        String[] expectedMessages = generateExpectedMessages(
                lineNumber, String.format(ServiceComponentManifestCheck.WRONG_DIRECTORY_MESSAGE, "services"));
        
        verifyServiceComponentHeader("manifest_subfolder_services", expectedMessages);
    }

    @Test
    public void testManifestNotExistentServices() throws Exception {
        int lineNumber = 11;
        String[] expectedMessages = generateExpectedMessages(
                lineNumber, ServiceComponentManifestCheck.WARNING_MESSAGE,
                lineNumber, String.format(ServiceComponentManifestCheck.NOT_EXISTING_SERVICE_MESSAGE, "serviceTestFileFour.xml"));
        
        verifyServiceComponentHeader("manifest_not_existent_services", expectedMessages);
    }

    @Test
    public void testNotExistentOsgiFolder() throws Exception {
        int lineNumberOfFirstMessage = 11;
        int lineNumberOFSecondMessage = 11;
        String[] expectedMessages = generateExpectedMessages(
                lineNumberOfFirstMessage, String.format(ServiceComponentManifestCheck.NOT_EXISTING_SERVICE_MESSAGE, "serviceTestFileOne.xml"),
                lineNumberOFSecondMessage, String.format(ServiceComponentManifestCheck.NOT_EXISTING_SERVICE_MESSAGE, "serviceTestFileTwo.xml"));

        verifyServiceComponentHeader("manifest_not_existent_osgi_folder", expectedMessages);
    }

    @Test
    public void testManifestAllServicesIncluded() throws Exception {
        String[] expectedMessages = CommonUtils.EMPTY_STRING_ARRAY;
        verifyServiceComponentHeader("manifest_include_all_services", expectedMessages);
    }

    @Test
    public void testExcludedFolderForServicesInOsgiInf() throws Exception {
        String[] expectedMessages = CommonUtils.EMPTY_STRING_ARRAY;
        verifyServiceComponentHeader("excluded_folder_for_services_in_osgi_inf", expectedMessages);
    }

    @Test
    public void testTwiceIncludedService() throws Exception {
        int lineNumber = 11;
        String[] expectedMessages = generateExpectedMessages(
                lineNumber, ServiceComponentManifestCheck.REPEATED_SERVICE_MESSAGE, 
                lineNumber, ServiceComponentManifestCheck.WARNING_MESSAGE);
        
        verifyServiceComponentHeader("manifest_twice_included_service", expectedMessages);
    }

    @Test
    public void testIncludedSubfolderServices() throws Exception {
        int lineNumber = 11;
        String[] expectedMessages = generateExpectedMessages(lineNumber,
                String.format(ServiceComponentManifestCheck.WRONG_DIRECTORY_MESSAGE, "services"), lineNumber,
                String.format(ServiceComponentManifestCheck.WRONG_DIRECTORY_MESSAGE, "services"), lineNumber,
                ServiceComponentManifestCheck.WARNING_MESSAGE);

        verifyServiceComponentHeader("manifest_included_subfolder_services", expectedMessages);
    }
    
    @Override
    protected DefaultConfiguration createCheckerConfig(Configuration config) {
        DefaultConfiguration configParent = new DefaultConfiguration("root");
        configParent.addChild(config);
        return configParent;
    }

    private void verifyServiceComponentHeader(String testDirectoryName, String[] expectedMessages) throws Exception {
        String testDirectoryPath = getPath(CHECK_TEST_DIRECTORY + testDirectoryName);
        File testDirectory = new File(testDirectoryPath);
        String testFilePath = testDirectory.getPath() + File.separator + MANIFEST_RELATIVE_PATH;
        File[] testFiles = listFilesForDirectory(testDirectory, new ArrayList<File>());

        verify(createChecker(config), testFiles, testFilePath, expectedMessages);
    }
}
