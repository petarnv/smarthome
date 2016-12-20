package org.eclipse.smarthome.tools.analysis.checkstyle;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.BundleRequirement;
import org.apache.ivy.osgi.core.ManifestParser;
import org.apache.ivy.osgi.util.Version;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.MessageDispatcher;

public class PomXmlCheck extends AbstractFileSetCheck {
    private List<File> pomXmlFiles = new ArrayList<>();
    
    private String pomVersion;
    private int pomVersionLine;
    private String manifestVersion;
    
    private String pomArtifactId;
    private int pomArtifactIdLine;
    private String manifestBundleSymbolicName;
    
    private static final String XML_EXTENSION = "xml";
    private static final String MANIFEST_EXTENSION = "MF";
    
    public PomXmlCheck(){
        setFileExtensions(XML_EXTENSION, MANIFEST_EXTENSION);
    }

    @Override
    protected void processFiltered(File file, List<String> lines) throws CheckstyleException {
        
        System.out.println("file: " + file);
        try {
            if (file.getName().equals("pom.xml")) {
                pomXmlFiles.add(file);
                DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = domFactory.newDocumentBuilder();
                Document doc = builder.parse(file);
                XPathFactory xPathfactory = XPathFactory.newInstance();
                XPath xpath = xPathfactory.newXPath();
                
                XPathExpression versionExpr = xpath.compile("/project/parent/version/text()");
                Object versionResult = versionExpr.evaluate(doc, XPathConstants.NODESET);
                NodeList versionNodes = (NodeList) versionResult;
                for (int i = 0; i < versionNodes.getLength(); i++) {
                    String nodeName = versionNodes.item(i).getParentNode().getNodeName();
                    if (nodeName.equals("version")) {
                        String nodeValue = versionNodes.item(i).getNodeValue();
                        System.out.println("version: " + nodeValue);
                        Pattern pattern = Pattern.compile("^\\d+[.]\\d+[.]\\d+");
                        Matcher matcher = pattern.matcher(nodeValue);
                        if(matcher.find()){
                            pomVersion = matcher.group();
                        }
                        pomVersionLine = findLineNumber(lines, nodeValue);
                    }
                }

                XPathExpression artifactIdExpr = xpath.compile("/project/artifactId/text()");
                Object artifactIdResult = artifactIdExpr.evaluate(doc, XPathConstants.NODESET);
                NodeList artifactIdNodes = (NodeList) artifactIdResult;
                for (int i = 0; i < artifactIdNodes.getLength(); i++) {
                    String nodeName = artifactIdNodes.item(i).getParentNode().getNodeName();
                    System.out.println("nodeName: " + artifactIdNodes.item(i).getParentNode().getNodeName());
                    if (nodeName.equals("artifactId")) {
                        String nodeValue = artifactIdNodes.item(i).getNodeValue();
                        //System.out.println("nodeValue: " + nodeValue);
                        pomArtifactId = nodeValue;
                        pomArtifactIdLine = findLineNumber(lines, "<artifactId>" + nodeValue + "</artifactId>");
                    }
                }
            } else if (file.getName().equals("MANIFEST.MF")) {
                BundleInfo bundleInfo = ManifestParser.parseManifest(file);
                
                Version version = bundleInfo.getVersion();
                Pattern pattern = Pattern.compile("^\\d+[.]\\d+[.]\\d+");
                Matcher matcher = pattern.matcher(version.toString());
                if(matcher.find()){
                    //System.out.println("matcher: " + matcher.group());
                    manifestVersion = matcher.group();
                }
                
                manifestBundleSymbolicName = bundleInfo.getSymbolicName();
                //manifestVersion = version.toString();
            }
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (XPathExpressionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @Override
    public void finishProcessing() {
        if(pomXmlFiles.size() == 0){
            logMessage("/pom.xml", 0, "pom.xml", "no.pom.xml");
        } else {
            if(!pomVersion.equals(manifestVersion)){
                System.out.println("line: " + pomVersionLine);
                logMessage("/pom.xml", pomVersionLine, "pom.xml", "wrong.version");
            }
            if(!pomArtifactId.equals(manifestBundleSymbolicName)){
                logMessage("/pom.xml", pomArtifactIdLine, "pom.xml", "wrong.artifact.id");
            }
        }
    }

    private void logMessage(String filePath, int line, String fileName, String message) {
        final MessageDispatcher dispatcher = getMessageDispatcher();
        dispatcher.fireFileStarted(filePath);
        log(line, message, fileName);
        fireErrors(filePath);
        dispatcher.fireFileFinished(filePath);
    }
    
    private int findLineNumber(List<String> lines, String text) {
        int number = 0;
        for (String line : lines) {
            number++;
            if (line.contains(text)) {
                return number;
            }
        }
        return number;
    }
    
}
