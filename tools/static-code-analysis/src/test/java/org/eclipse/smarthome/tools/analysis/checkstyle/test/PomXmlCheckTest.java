package org.eclipse.smarthome.tools.analysis.checkstyle.test;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.eclipse.smarthome.tools.analysis.checkstyle.BuildPropertiesCheck;
import org.eclipse.smarthome.tools.analysis.checkstyle.PomXmlCheck;
import org.junit.Test;

import com.puppycrawl.tools.checkstyle.BaseCheckTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.utils.CommonUtils;

public class PomXmlCheckTest extends BaseCheckTestSupport {
    private final String TEST_RESOURCES_DIRECTORY =  "src/test/resources/org/eclipse/smarthome/tools/analysis/checkstyle/test/";
    private final String FILE_PATH = "/pom.xml";
    
    @Test
    public void testValidPom(){
        DefaultConfiguration config = createCheckConfig(PomXmlCheck.class);
        
        String directoryPath = TEST_RESOURCES_DIRECTORY + "valid_pom_xml_directory";
        
        File dir = new File(directoryPath);
        File[] files = listFilesForFolder(dir, new ArrayList<File>());
        try {
            verify(createChecker(config), files, FILE_PATH, CommonUtils.EMPTY_STRING_ARRAY);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An exception was thrown, while trying to verify logged message: " + e);
        }
    }
    
    @Test
    public void testInvalidVersionInPom(){
        DefaultConfiguration config = createCheckConfig(PomXmlCheck.class);
        
        String directoryPath = TEST_RESOURCES_DIRECTORY + "invalid_version_in_pom_xml_directory";
        
        File dir = new File(directoryPath);
        File[] files = listFilesForFolder(dir, new ArrayList<File>());
        try {
            verify(createChecker(config), files, FILE_PATH, new String []{"9: wrong.version"} );
        } catch (Exception e) {
            e.printStackTrace();
            fail("An exception was thrown, while trying to verify logged message: " + e);
        }
    }
    
    @Test
    public void testInvalidartifactIdInPom(){
        DefaultConfiguration config = createCheckConfig(PomXmlCheck.class);
        
        String directoryPath = TEST_RESOURCES_DIRECTORY + "invalid_artifactId_in_pom_xml_directory";
        
        File dir = new File(directoryPath);
        File[] files = listFilesForFolder(dir, new ArrayList<File>());
        try {
            verify(createChecker(config), files, FILE_PATH, new String []{"19: wrong.artifact.id"} );
        } catch (Exception e) {
            e.printStackTrace();
            fail("An exception was thrown, while trying to verify logged message: " + e);
        }
    }
    
    @Test
    public void testMissingPomXml(){
        DefaultConfiguration config = createCheckConfig(PomXmlCheck.class);
        
        String directoryPath = TEST_RESOURCES_DIRECTORY + "missing_pom_xml_directory";
        
        File dir = new File(directoryPath);
        File[] files = listFilesForFolder(dir, new ArrayList<File>());
        try {
            verify(createChecker(config), files, FILE_PATH, new String []{"0: no.pom.xml"} );
        } catch (Exception e) {
            e.printStackTrace();
            fail("An exception was thrown, while trying to verify logged message: " + e);
        }
    }
    
    @Override
    protected DefaultConfiguration createCheckerConfig(Configuration config) {
        DefaultConfiguration dc = new DefaultConfiguration("root");
        dc.addChild(config);
        return dc;
    }
    
    public File[] listFilesForFolder(File folder, ArrayList<File> files) {
        String[] str = new String[]{};
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry, files);
            } else {
                files.add(fileEntry);
            }
        }
        return files.toArray(new File[]{});
    }

}
