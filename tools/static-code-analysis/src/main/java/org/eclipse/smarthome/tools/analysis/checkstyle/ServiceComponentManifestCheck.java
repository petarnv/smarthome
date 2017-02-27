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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.smarthome.tools.analysis.checkstyle.api.AbstractStaticCheck;

import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.MessageDispatcher;

/**
 *
 * Check if all the declarative services are included in the MANIFEST.MF.
 *
 * @author Aleksandar Kovachev - Initial contribution
 * @author Petar Valchev - Changed the verification of the Service-Component header
 *
 */
public class ServiceComponentManifestCheck extends AbstractStaticCheck {
    private Log logger = LogFactory.getLog(ServiceComponentManifestCheck.class);

    private static final String MANIFEST_EXTENSTION = "MF";
    private static final String XML_EXTENSION = ".xml";
    private static final String MANIFEST_FILE_NAME = "MANIFEST." + MANIFEST_EXTENSTION;
    private static final String SERVICE_COMPONENT_HEADER = "Service-Component";
    private static final String OSGI_INF_DIRECTORY_NAME = "OSGI-INF";
    private static final String XML_METADATA_STATEMENT = OSGI_INF_DIRECTORY_NAME + "/*.xml";

    public static final String WARNING_MESSAGE = "A good approach is to use " + XML_METADATA_STATEMENT
            + " instead of including the services metadata files separately.";
    public static final String WRONG_DIRECTORY_MESSAGE = "The %s directory is unrecognized. The services metadata files must be placed in "
            + OSGI_INF_DIRECTORY_NAME + " directory.";
    public static final String WRONG_EXTENSION_MESSAGE = "Only XML metadata files for services description are expected in the "
            + OSGI_INF_DIRECTORY_NAME + " directory.";
    public static final String NOT_INCLUDED_SERVICE_MESSAGE = "The service %s is not included in the "
            + MANIFEST_FILE_NAME + " file. Are you sure that there is no need to be included?";
    public static final String NOT_EXISTING_SERVICE_MESSAGE = "The service %s does not exists in the "
            + OSGI_INF_DIRECTORY_NAME + " folder.";
    public static final String REPEATED_SERVICE_MESSAGE = "If you are using OSGI-INF/*.xml, do not include any of the services explicitly. "
            + "Otherwise they will be included twice.";
    
    private String manifestPath;

    private List<String> componentXmlFiles = new ArrayList<>();
    private List<String> manifestServiceComponents = new ArrayList<>();
    
    private List<String> manifestFileContent;
    private String serviceComponentHeaderValue;
    
    public String[] excludedSubfolders;

    // A configuration property for excluded subfolders from the OSGI-INF directory.
    public void setExcludedSubfolders(String[] excludedSubfolders) {
        this.excludedSubfolders = excludedSubfolders;
    }

    public ServiceComponentManifestCheck() {
        logger.info("Executing the ServiceComponentManifestCheck: "
                + "Check if all the declarative services are included in the " + MANIFEST_FILE_NAME);
        setFileExtensions(MANIFEST_EXTENSTION, XML_EXTENSION);
    }

    @Override
    protected void processFiltered(File file, List<String> lines) throws CheckstyleException {
        String fileName = file.getName();
        String filePath = file.getPath();
        if (filePath.contains(OSGI_INF_DIRECTORY_NAME)) {
            if (!isFileInExcludedDirectory(file)) {
                componentXmlFiles.add(fileName);
            }
        }

        if (fileName.equals(MANIFEST_FILE_NAME)) {
            manifestFileContent = lines;
            manifestPath = file.getPath();
            try {
                Manifest manifest = new Manifest(new FileInputStream(file));
                Attributes attributes = manifest.getMainAttributes();
                serviceComponentHeaderValue = attributes.getValue(SERVICE_COMPONENT_HEADER);
            } catch (IOException e) {
                logger.error("Problem occurred while parsing the file " + filePath, e);
            }
            verifyServiceComponentHeader();
        }
    }
    
    @Override
    public void finishProcessing() {
        boolean presentManifestServiceComponent = manifestServiceComponents.size() > 0;
        
        boolean wildcardUsedInManifest = serviceComponentHeaderValue != null && serviceComponentHeaderValue.contains("*") ;
        // we are only interested in the services that are explicitly
        // declared in the Service-Component header of the MANIFEST.MF
        boolean presentComponentXmlFiles = componentXmlFiles.size() > 0 && !wildcardUsedInManifest;
        
        boolean validServiceComponentsConfiguration = presentManifestServiceComponent || presentComponentXmlFiles;
        if (validServiceComponentsConfiguration) {
            boolean allServicesAreIcluded = CollectionUtils.isEqualCollection(manifestServiceComponents,
                    componentXmlFiles);
            if (allServicesAreIcluded) {
                // If all the services in the MANIFEST.MF are included
                // separately
                logMessage(findLineNumber(manifestFileContent, SERVICE_COMPONENT_HEADER, 0), WARNING_MESSAGE);
            } else if (manifestServiceComponents.size() > componentXmlFiles.size()) {
                // If there is a service in the MANIFEST.MF that does not exists
                // in the OSGI-INF folder
                manifestServiceComponents.removeAll(componentXmlFiles);
                logMissingServiceDeclaration(manifestServiceComponents, NOT_EXISTING_SERVICE_MESSAGE);
            } else {
                // If there is a service in the OSGI-INF that is not included in
                // the MANIFEST.MF
                if(!wildcardUsedInManifest){
                    componentXmlFiles.removeAll(manifestServiceComponents);
                    logMissingServiceDeclaration(componentXmlFiles, NOT_INCLUDED_SERVICE_MESSAGE);
                }
            }
        }
    }
    
    private void verifyServiceComponentHeader() {
        List<File> serviceComponents = parseServiceComponentHeader();
        if (!serviceComponents.isEmpty()) {
            int serviceComponentHeaderLineNumber = findLineNumber(manifestFileContent, SERVICE_COMPONENT_HEADER, 0);
            
            for (File serviceComponent : serviceComponents) {
                boolean correctServiceComponentNameUsed = serviceComponent.getName().equals("*" + XML_EXTENSION);
                boolean correctServiceComponentDirectoryUsed = serviceComponent.getParentFile().getName().equals(OSGI_INF_DIRECTORY_NAME);
                boolean repeatedService = serviceComponents.size() > 1;
                boolean serviceWithXmlExtension = serviceComponent.getName().endsWith(XML_EXTENSION);
                boolean wildcardUsed = serviceComponent.getName().contains("*");

                if (correctServiceComponentNameUsed && correctServiceComponentDirectoryUsed) {
                    // If the component descriptor entry is OSGI-INF/*.xml
                    if (repeatedService) {
                        // If there is any explicit service declaration in addition to OSGI-INF/*.xml
                        logMessage(serviceComponentHeaderLineNumber, REPEATED_SERVICE_MESSAGE);
                    }
                } else {
                    if (!correctServiceComponentDirectoryUsed) {
                        // If the parent directory of the service component is not OSGI-INF/
                        String wrongDirectoryMessage = String.format(WRONG_DIRECTORY_MESSAGE, serviceComponent.getParentFile().getName());
                        logMessage(serviceComponentHeaderLineNumber, wrongDirectoryMessage);
                    }
                    if (!serviceWithXmlExtension) {
                        // If the service component is with extension other than .xml
                        logMessage(serviceComponentHeaderLineNumber, WRONG_EXTENSION_MESSAGE);
                    }
                    if (!wildcardUsed && serviceWithXmlExtension) {
                        // If the xml service component does not use wildcard in its name
                        manifestServiceComponents.add(serviceComponent.getName());
                    }
                    if (wildcardUsed && serviceWithXmlExtension && !correctServiceComponentNameUsed) {
                        // If a wildcard other than *.xml is used (e.g. service*.xml)
                        logMessage(serviceComponentHeaderLineNumber, WARNING_MESSAGE);
                    }
                }
            }
        }
    }

    private List<File> parseServiceComponentHeader(){
        List<File> serviceComponentFiles = new ArrayList<>();
        if(serviceComponentHeaderValue != null){
            List<String> serviceComponentsList = Arrays.asList(serviceComponentHeaderValue.trim().split(","));
            for(String serviceComponent : serviceComponentsList){
                serviceComponentFiles.add(new File(serviceComponent));
            }
        }
        return serviceComponentFiles;
    }
    
    private boolean isFileInExcludedDirectory(File serviceFile) {
        String osgiInfDirectoryAbsolutePath = serviceFile.getParentFile().getPath();
        String osgiInfDirectoryRelativePath = osgiInfDirectoryAbsolutePath
                .substring(osgiInfDirectoryAbsolutePath.indexOf(OSGI_INF_DIRECTORY_NAME));
        for (String excludedFolder : excludedSubfolders) {
            if (osgiInfDirectoryRelativePath.contains(excludedFolder)) {
                return true;
            }
        }
        return false;
    }

    private void logMissingServiceDeclaration(List<String> missingServices, String message) {
        for (String service : missingServices) {
            int lineNumber = 0;
            if (serviceComponentHeaderValue != null) {
                lineNumber = findLineNumber(manifestFileContent, service, 0);
                if (lineNumber == -1) {
                    // If the service is not included in the MANIFEST.MF, take the
                    // line number of the Service-Component header
                    lineNumber = findLineNumber(manifestFileContent, SERVICE_COMPONENT_HEADER, 0);
                }
            }
            logMessage(lineNumber, String.format(message, service));
        }
    }

    private void logMessage(int line, String message) {
        MessageDispatcher dispatcher = getMessageDispatcher();
        dispatcher.fireFileStarted(manifestPath);
        log(line, message, MANIFEST_FILE_NAME);
        fireErrors(manifestPath);
        dispatcher.fireFileFinished(manifestPath);
    }
}
