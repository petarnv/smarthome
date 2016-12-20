package org.eclipse.smarthome.tools.analysis.checkstyle.test;

import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.smarthome.tools.analysis.checkstyle.BuildPropertiesCheck;
import org.junit.Test;

import com.puppycrawl.tools.checkstyle.BaseCheckTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.utils.CommonUtils;

public class BuildPropertiesCheckTest extends BaseCheckTestSupport {
    private final String TEST_RESOURCES_DIRECTORY =  "src/test/resources/org/eclipse/smarthome/tools/analysis/checkstyle/test/";
    private final String FILE_PATH = "/build.properties";
    private String[] expectedMessages = new String []{"0: no.build.properties.file"};

    @Test
    public void testNotExistentBuildPropertiesFile() {
        verifyBuildPropertiesFile("not_existent_build_properties_directory", expectedMessages);
    }
    
    @Test
    public void testValidBuildPropertiesFile() {
        verifyBuildPropertiesFile("valid_build_properties_directory", CommonUtils.EMPTY_STRING_ARRAY);
    }
    
    @Test
    public void testNotValidBinIncludesInBuildPropertiesFile() {
        verifyBuildPropertiesFile("not_valid_bin_includes_in_build_properties_file_directory", expectedMessages);
    }
    
    @Test
    public void testNotValidOutputInBuildPropertiesFile() {
        verifyBuildPropertiesFile("not_valid_output_in_build_properties_file_directory", expectedMessages);
    }
    
    @Test
    public void testNotValidSourceInBuildPropertiesFile() {
        verifyBuildPropertiesFile("not_valid_source_in_build_properties_file_directory", expectedMessages);
    }
    
    @Test
    public void testBuildPropertiesFileWithInvalidName() {
        verifyBuildPropertiesFile("not_valid_name_build_properties_directory", expectedMessages);
    }
    
    @Test
    public void testMissingBinIncludesInBuildPropertiesFile() {
        verifyBuildPropertiesFile("missing_bin_includes_in_build_properties_file_directory", expectedMessages);
    }
    
    @Test
    public void testMissingSourceInBuildPropertiesFile() {
        verifyBuildPropertiesFile("missing_source_in_build_properties_file_directory", expectedMessages);
    }
    
    @Test
    public void testMissingOutputInBuildPropertiesFile() {
        verifyBuildPropertiesFile("missing_source_in_build_properties_file_directory", expectedMessages);
    }
    
    @Override
    protected DefaultConfiguration createCheckerConfig(Configuration config) {
        DefaultConfiguration dc = new DefaultConfiguration("root");
        dc.addChild(config);
        return dc;
    }
    
    private void verifyBuildPropertiesFile(String directory, String[] expectedMessages){
        DefaultConfiguration config = createCheckConfig(BuildPropertiesCheck.class);
        
        String directoryPath = TEST_RESOURCES_DIRECTORY + directory;
        
        File dir = new File(directoryPath);
        
        try {
            verify(createChecker(config), dir.listFiles(), FILE_PATH, expectedMessages);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An exception was thrown, while trying to verify logged message: " + e);
        }
    }
}
