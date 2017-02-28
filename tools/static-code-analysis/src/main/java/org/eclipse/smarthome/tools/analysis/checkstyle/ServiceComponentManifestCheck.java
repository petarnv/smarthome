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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
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

    private List<String> regexServiceComponents = new ArrayList<>();
    private List<String> explicitlyDeclaredServiceComponents = new ArrayList<>();
    private List<String> componentXmlFiles = new ArrayList<>();
    
    private String manifestPath;
    
    public String[] excludedSubfolders;

    // A configuration property for excluded subfolders from the OSGI-INF directory.
    public void setExcludedSubfolders(String[] excludedSubfolders) {
        this.excludedSubfolders = excludedSubfolders;
    }

    public ServiceComponentManifestCheck() {
        /*logger.info("Executing the ServiceComponentManifestCheck: "
                + "Check if all the declarative services are included in the " + MANIFEST_FILE_NAME);*/
        setFileExtensions(MANIFEST_EXTENSTION, XML_EXTENSION);
    }

    @Override
    protected void processFiltered(File file, List<String> lines) throws CheckstyleException {
        String fileName = file.getName();
        String filePath = file.getPath();
        manifestPath = filePath;
        if (filePath.contains(OSGI_INF_DIRECTORY_NAME)) {
            if (!isFileInExcludedDirectory(file)) {
                componentXmlFiles.add(fileName);
            }
        }

        if (fileName.equals(MANIFEST_FILE_NAME)) {
            try {
                Manifest manifest = new Manifest(new FileInputStream(file));
                Attributes attributes = manifest.getMainAttributes();
                String serviceComponentHeaderValue = attributes.getValue(SERVICE_COMPONENT_HEADER);
                
                int serviceComponentHeaderLineNumber = findLineNumber(lines, SERVICE_COMPONENT_HEADER, 0);
                if(serviceComponentHeaderValue != null){
                    List<String> serviceComponentsList = Arrays.asList(serviceComponentHeaderValue.trim().split(","));
                    for(String serviceComponent : serviceComponentsList){
                        File serviceComponentFile = new File(serviceComponent);
                        String serviceComponentParentDirectoryName = serviceComponentFile.getParentFile().getName();
                        
                        if(!serviceComponentParentDirectoryName.equals(OSGI_INF_DIRECTORY_NAME)){
                            String wrongDirectoryMessage = String.format(WRONG_DIRECTORY_MESSAGE, serviceComponentParentDirectoryName);
                            logMessage(serviceComponentHeaderLineNumber, wrongDirectoryMessage);
                        }
                        
                        String serviceComponentName = serviceComponentFile.getName();
                        if(!serviceComponentName.endsWith(XML_EXTENSION)){
                            logMessage(serviceComponentHeaderLineNumber, WRONG_EXTENSION_MESSAGE);
                        } else if(serviceComponentName.contains("*")){
                            regexServiceComponents.add(serviceComponentName);
                        } else {
                            explicitlyDeclaredServiceComponents.add(serviceComponentName);
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Problem occurred while parsing the file " + filePath, e);
            }
        }
    }
    
    @Override
    public void finishProcessing() {
        //System.out.println("regexServiceComponents: " + regexServiceComponents);
        //System.out.println("explicitlyDeclaredServiceComponents: " + explicitlyDeclaredServiceComponents);
        //System.out.println("componentXmlFiles: " + componentXmlFiles);
        
        for(String pattern : regexServiceComponents){
            String patternWithoutExtension = StringUtils.substringBefore(pattern, XML_EXTENSION);
            
            if(!patternWithoutExtension.equals("*")){
                System.out.println("patternWithoutExtension: " + patternWithoutExtension);
                Pattern p = Pattern.compile(patternWithoutExtension);
                Matcher m = p.matcher("serviceTestFile1.xml");
                while(m.find()) {
                    System.out.println("MATCH!");
                }
            }
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
    
    private void logMessage(int line, String message) {
        MessageDispatcher dispatcher = getMessageDispatcher();
        dispatcher.fireFileStarted(manifestPath);
        log(line, message, MANIFEST_FILE_NAME);
        fireErrors(manifestPath);
        dispatcher.fireFileFinished(manifestPath);
    }
}
