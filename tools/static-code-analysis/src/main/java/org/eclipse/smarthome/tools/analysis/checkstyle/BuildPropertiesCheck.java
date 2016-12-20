package org.eclipse.smarthome.tools.analysis.checkstyle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.MessageDispatcher;

public class BuildPropertiesCheck extends AbstractFileSetCheck {
    private List<File> validBuildPropertiesFiles = new ArrayList<>();
    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String NO_BUILD_PROPERTIES_FILE_MSG_KEY = "no.build.properties.file";
    private static final String PROPERTIES_EXTENSTION = "properties";
    private static final String BUILD_PROPERTIES_FILE_NAME = "build.properties";
    
    public BuildPropertiesCheck(){
        setFileExtensions(PROPERTIES_EXTENSTION);
    }
    
    @Override
    protected void processFiltered(File file, List<String> lines) throws CheckstyleException {
        try {
            if (file.getName().equals(BUILD_PROPERTIES_FILE_NAME)) {
                Properties buildProperties = new Properties();
                InputStream buildPropertiesInputStream = new FileInputStream(file.getAbsolutePath());
                buildProperties.load(buildPropertiesInputStream);
                
                int metaInfDirectortyIndex = -1;
                String binIncludesProperty = buildProperties.getProperty("bin.includes");
                if(binIncludesProperty != null){
                    String[] binIncludesValues = binIncludesProperty.split(",");
                    metaInfDirectortyIndex = Arrays.binarySearch(binIncludesValues, "META-INF/");
                }
                
                int targetClassesDirectoryIndex = -1;
                String outputProperty = buildProperties.getProperty("output..");
                if(outputProperty != null){
                    String[] outputValues = outputProperty.split(",");
                    targetClassesDirectoryIndex = Arrays.binarySearch(outputValues, "target/classes/");
                }
                
                int srcMainJavaDirectoryIndex = -1;
                String sourceProperty = buildProperties.getProperty("source..");
                if(sourceProperty != null){
                    String[] sourceValues = sourceProperty.split(",");
                    srcMainJavaDirectoryIndex = Arrays.binarySearch(sourceValues, "src/main/java/");
                }

                if(metaInfDirectortyIndex >= 0 && targetClassesDirectoryIndex >= 0 && srcMainJavaDirectoryIndex >= 0){
                    validBuildPropertiesFiles.add(file);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void finishProcessing() {
        if (validBuildPropertiesFiles.size() == 0) {
            logMissingFile("/" + BUILD_PROPERTIES_FILE_NAME, BUILD_PROPERTIES_FILE_NAME, NO_BUILD_PROPERTIES_FILE_MSG_KEY);
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
