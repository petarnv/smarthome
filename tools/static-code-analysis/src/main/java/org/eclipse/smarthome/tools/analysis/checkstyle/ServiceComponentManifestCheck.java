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
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
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

    public static final String BEST_APPROACH_MESSAGE = "A good approach is to use " + XML_METADATA_STATEMENT
            + " instead of including the services metadata files separately.";
    public static final String WRONG_DIRECTORY_MESSAGE = "Incorrect directory for services - %s. "
            + "The services metadata files must be placed directly in " + OSGI_INF_DIRECTORY_NAME + " directory.";
    public static final String WRONG_EXTENSION_MESSAGE = "The serice %s is with invalid extension."
            + "Only XML metadata files for services description are expected in the " + OSGI_INF_DIRECTORY_NAME + " directory.";
    public static final String NOT_INCLUDED_SERVICE_MESSAGE = "The service %s is not included in the "
            + MANIFEST_FILE_NAME + " file. Are you sure that there is no need to be included?";
    public static final String NOT_EXISTING_SERVICE_MESSAGE = "The service %s does not exists in the "
            + OSGI_INF_DIRECTORY_NAME + " folder.";
    public static final String REPEATED_SERVICE_MESSAGE = "If you are using OSGI-INF/*.xml, do not include any of the services explicitly. "
            + "Otherwise they will be included more than once.";
    public static final String NOT_MATCHING_REGEX_MESSAGE = "The service component %s does not match any of the exisitng services.";

    private List<String> manifestServiceComponents = new ArrayList<>();
    private List<String> componentXmlFiles = new ArrayList<>();
    
    private boolean loggedBestApproachMessage = false;
    
    private String serviceComponentHeaderValue;
    private int serviceComponentHeaderLineNumber;
    private String manifestPath;
    
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
        if (file.getPath().contains(OSGI_INF_DIRECTORY_NAME)) {
            if (!isFileInExcludedDirectory(file)) {
                componentXmlFiles.add(file.getName());
            }
        }

        if (file.getName().equals(MANIFEST_FILE_NAME)) {
            verifyManifest(file, lines);
        }
    }

    @Override
    public void finishProcessing() {
        verifyManifestRegexServiceComponents();
        verifyManifestExplicitlyDeclaredServices();
    }

    private void verifyManifest(File file, List<String> lines){
        manifestPath = file.getPath();
        try {
            Manifest manifest = new Manifest(new FileInputStream(file));
            Attributes attributes = manifest.getMainAttributes();
            
            serviceComponentHeaderValue = attributes.getValue(SERVICE_COMPONENT_HEADER);
            serviceComponentHeaderLineNumber = findLineNumber(lines, SERVICE_COMPONENT_HEADER, 0);
            
            if(serviceComponentHeaderValue != null){
                List<String> serviceComponentsList = Arrays.asList(serviceComponentHeaderValue.trim().split(","));
                for(String serviceComponent : serviceComponentsList){
                    File serviceComponentFile = new File(serviceComponent);
                    String serviceComponentParentDirectoryName = serviceComponentFile.getParentFile().getName();
                    
                    if(!serviceComponentParentDirectoryName.equals(OSGI_INF_DIRECTORY_NAME)){
                        // if the parent directory of the service is not OSGI-INF
                        logMessage(serviceComponentHeaderLineNumber, String.format(WRONG_DIRECTORY_MESSAGE, serviceComponentParentDirectoryName));
                    } 
                    
                    String serviceComponentName = serviceComponentFile.getName();
                    if(!serviceComponentName.endsWith(XML_EXTENSION)){
                        // if the extension of the service is not .xml
                        logMessage(serviceComponentHeaderLineNumber, String.format(WRONG_EXTENSION_MESSAGE, serviceComponentName));
                    } else {
                        manifestServiceComponents.add(serviceComponentName);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Problem occurred while parsing the file " + file.getPath(), e);
        }
    }
    
    private void verifyManifestRegexServiceComponents(){
        Iterator<String> manifestServiceComponentsIterator = manifestServiceComponents.iterator();
        
        while(manifestServiceComponentsIterator.hasNext()){
            String manifestServiceComponent = manifestServiceComponentsIterator.next();
            String manifestServiceComponentName = StringUtils.substringBefore(manifestServiceComponent, XML_EXTENSION);
            
            if(manifestServiceComponentName.contains("*")){
                // if a wildcard is used in the service component declaration
                
                if(manifestServiceComponentName.equals("*")){
                    if(manifestServiceComponents.size() > 1){
                        // if there is any explicit service declaration in addition to *.xml
                        logMessage(serviceComponentHeaderLineNumber, REPEATED_SERVICE_MESSAGE);
                    }
                    
                    // if the service component is declared as *.xml, all the services are included
                    // and there is no need of further comparison of the two lists
                    manifestServiceComponents.clear();
                    componentXmlFiles.clear();
                    break;
                } else {
                    // if a wildcard other than *.xml is used
                    logBestApproachMessage();
                    
                    Pattern pattern = Pattern.compile(manifestServiceComponentName);
                    boolean matchedPattern = false;
                    
                    Iterator<String> componentXmlFilesIterator = componentXmlFiles.iterator();
                    while(componentXmlFilesIterator.hasNext()){
                        String componentXml = componentXmlFilesIterator.next();
                        Matcher matcher = pattern.matcher(componentXml);
                        if(matcher.find()){
                            // if any of the services matches the manifest service component regex,
                            // remove them from the list, so that we can verify only the service components, 
                            // that are declared with their full name later
                            componentXmlFilesIterator.remove();
                            
                            if(!matchedPattern){
                                matchedPattern = true;
                            }
                        }
                    }
                    
                    if(!matchedPattern){
                        logMessage(serviceComponentHeaderLineNumber, String.format(NOT_MATCHING_REGEX_MESSAGE, manifestServiceComponent));
                    }
                    
                    // remove the regex service component definition, 
                    // so that we can verify only the service components, 
                    // that are declared with their full name later
                    manifestServiceComponentsIterator.remove();
                }
            } else {
                // if no wildcard is used and the service is declared explicitly
                logBestApproachMessage();
            }
        }
    }
    
    private void verifyManifestExplicitlyDeclaredServices(){
        // list in which we will store all the common elements of 
        // manifestServiceComponents and componentXmlFiles
        List<String> intersection = new ArrayList<>(manifestServiceComponents);
        intersection.retainAll(componentXmlFiles);
        
        // log a message for every not included service in the manifest
        componentXmlFiles.removeAll(intersection);
        for(String service : componentXmlFiles){
            if(serviceComponentHeaderLineNumber == -1){
                // if there is no Service-Component header
                logMessage(0, String.format(ServiceComponentManifestCheck.NOT_INCLUDED_SERVICE_MESSAGE, service));
            } else {
                logMessage(serviceComponentHeaderLineNumber, String.format(ServiceComponentManifestCheck.NOT_INCLUDED_SERVICE_MESSAGE, service));
            }
        }
        
        // log a message for every service component definition, 
        // that does not have a corresponding service
        manifestServiceComponents.removeAll(intersection);
        for(String service : manifestServiceComponents){
            logMessage(serviceComponentHeaderLineNumber, String.format(ServiceComponentManifestCheck.NOT_EXISTING_SERVICE_MESSAGE, service));
        }
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
    
    private void logBestApproachMessage(){
        if(!loggedBestApproachMessage){
            logMessage(serviceComponentHeaderLineNumber, BEST_APPROACH_MESSAGE);
            loggedBestApproachMessage = true;
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
