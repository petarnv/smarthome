package org.eclipse.smarthome.core.service

import static java.nio.file.StandardWatchEventKinds.*
import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.*
import static org.junit.matchers.JUnitMatchers.*

import java.nio.file.Path
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.nio.file.WatchEvent.Kind

import org.apache.commons.lang.SystemUtils
import org.eclipse.smarthome.test.OSGiTest
import org.junit.After
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

/**
 * Test for {@link AbstractWatchService}.
 *
 * @author Dimitar Ivanov
 *
 */
class AbstractWatchServiceTest extends OSGiTest {

    def static WATCHED_DIRECTORY = "watchDirectory"

    // Fail if no event has been received within the given timeout
    def static NO_EVENT_TIMEOUT_IN_SECONDS = 3;

    RelativeWatchService watchService

    @BeforeClass
    static void setUpBeforeClass(){
        File watchDir = new File(WATCHED_DIRECTORY);
        watchDir.mkdirs()
    }

    @AfterClass
    static void tearDownClass(){
        File watchedDirectory = new File(WATCHED_DIRECTORY);
        watchedDirectory.deleteDir()
    }

    @After
    public void tearDown(){
        watchService.deactivate()
        clearWatchedDir()
        watchService.allFullEvents.clear()
    }

    void clearWatchedDir(){
        File watchedDirectory = new File(WATCHED_DIRECTORY)
        watchedDirectory.listFiles().each { File mockedFile ->
            mockedFile.isFile() ? mockedFile.delete() : mockedFile.deleteDir()
        }
    }

    @Test
    void 'AbstractWatchQueueReader processWatchEvent path in root provided with correct arguments'() {
        watchService = new RelativeWatchService(WATCHED_DIRECTORY,true,false)

        // File created in the watched directory
        assertByRelativePath("rootWatchFile")
    }

    @Test
    void 'AbstractWatchQueueReader processWatchEvent path in subdir provided with correct arguments'() {
        watchService = new RelativeWatchService(WATCHED_DIRECTORY,true,false)

        // File created in a subdirectory of the watched directory
        assertByRelativePath("subdir" + File.separatorChar + "subDirWatchFile")
    }

    @Test
    void 'AbstractWatchQueueReader processWatchEvent path in subsubdir provided with correct arguments'() {
        watchService = new RelativeWatchService(WATCHED_DIRECTORY,true,false)

        // File created in a sub sub directory of the watched directory
        assertByRelativePath("subDir" + File.separatorChar + "subSubDir" + File.separatorChar + "innerWatchFile")
    }

    @Test
    void 'same file names in root and subdir are correctly processed'(){
        watchService = new RelativeWatchService(WATCHED_DIRECTORY,true,false)

        def fileName = "duplicateFile"
        def innerFileName = "duplicateDir" + File.separatorChar + fileName

        File innerfile = new File(WATCHED_DIRECTORY + File.separatorChar + innerFileName)

        // Make all the directories needed
        innerfile.getParentFile().mkdirs()

        // Activate the service when the subdir is also present. Else the subdir will not be registered
        watchService.activate()

        boolean isCreated = innerfile.createNewFile()
        assertThat "The file '$innerfile.absolutePath' was not created successfully", isCreated, is(true)

        // Assure that the ordering of the events will be always the same
        sleep 200

        File file = new File(WATCHED_DIRECTORY + File.separatorChar + fileName)
        isCreated = file.createNewFile()
        assertThat "The file '$file.absolutePath' was not created successfully", isCreated, is(true)

        def expectedEvents = 2
        waitForAssert({assertThat "Exactly two watch events were expected within $NO_EVENT_TIMEOUT_IN_SECONDS seconds, but were: " + watchService.allFullEvents, watchService.allFullEvents.size(), is(expectedEvents)},NO_EVENT_TIMEOUT_IN_SECONDS*1000,1000)

        FullEvent innerFileEvent = watchService.allFullEvents[0]
        assertThat "The inner file '$innerfile.absolutePath' creation was not detected. All events detected: " + watchService.allFullEvents,innerFileEvent.eventKind,is(ENTRY_CREATE)
        assertThat "The path of the first detected event should be for $innerFileName", innerFileEvent.eventPath.toString(), is(innerFileName)

        FullEvent fileEvent = watchService.allFullEvents[1]
        assertThat "The root file '$file.absolutePath' creation  was not detected. All events detected: " + watchService.allFullEvents,fileEvent.eventKind,is(ENTRY_CREATE)
        assertThat "The path of the second event should be for $fileName", fileEvent.eventPath.toString(), is(fileName)
    }

    @Test
    void 'subirs are registered and modifications are watched, folder is created after service activation'(){
        // Watch subdirectories and their modifications
        // RelativeWatchService#processWatchEvent() provides implementation for watching folders after service activation
        watchService = new RelativeWatchService(WATCHED_DIRECTORY,true,true)

        watchService.activate()
        def subDirName = "subDir"
        def fileName = "innerFile"
        def innerFileName = subDirName + File.separatorChar + fileName
        File innerFile = new File(WATCHED_DIRECTORY + File.separatorChar + innerFileName)

        // Make subdirectories
        innerFile.getParentFile().mkdirs()

        assertDirectoryCraeteEventIsProcessed(subDirName)

        watchService.allFullEvents.clear();

        boolean isCreated = innerFile.createNewFile()
        assertThat "The file '$innerFile.absolutePath' was not created successfully", isCreated, is(true)

        assertAllEventsAreProcessed(subDirName,innerFile, innerFileName)
    }

    @Test
    void 'subdirs are not registered and modifications are watched, folder is created after service activation'(){
        // Do not watch subdirectories, but watch their modifications
        // RelativeWatchService#processWatchEvent() provides implementation for watching folders after service activation
        watchService = new RelativeWatchService(WATCHED_DIRECTORY,false,true)
        watchService.activate()

        def subDirName = "subDir"
        def fileName = "innerFile"
        def innerFileName = subDirName + File.separatorChar + fileName

        File innerFile = new File(WATCHED_DIRECTORY + File.separatorChar + innerFileName)

        // Make subdirectories
        innerFile.getParentFile().mkdirs()

        assertDirectoryCraeteEventIsProcessed(subDirName)

        watchService.allFullEvents.clear();

        boolean isCreated = innerFile.createNewFile()
        assertThat "The file '$innerFile.absolutePath' was not created successfully", isCreated, is(true)

        assertDirectoryModifyEventIsProcessed(subDirName)
    }

    @Test
    void 'subdirs are not registered and modifications are not watched, folder is created after service activation' () {
        // Do not watch subdirectories and do not watch their modifications
        // RelativeWatchService#processWatchEvent() provides implementation for watching folders after service activation
        watchService = new RelativeWatchService(WATCHED_DIRECTORY,false,false)
        watchService.activate()

        def subDirName = "subDir"
        def fileName = "innerFile"
        def innerFileName = subDirName + File.separatorChar + fileName

        File innerFile = new File(WATCHED_DIRECTORY + File.separatorChar + innerFileName)

        // Make subdirectories
        innerFile.getParentFile().mkdirs()

        // Consequent creation and deletion in order to generate any watch events for the subdirectory
        boolean isCreated = innerFile.createNewFile()
        assertThat "The file '$innerFile.absolutePath' was not created successfully", isCreated, is(true)

        boolean isDeleted = innerFile.delete()
        assertThat "Inner file is not deleted", isDeleted, is(true)

        assertNoEventsAreProcessed()
    }

    @Test
    void 'subdirs are registered, but dirs modifications are not watched, folder is created after service activation'() {
        // Watch subdirectories, but not their modifications
        // RelativeWatchService#processWatchEvent() provides implementation for watching folders after service activation
        watchService = new RelativeWatchService(WATCHED_DIRECTORY,true,false)

        watchService.activate()

        def subDirName = "subDir"
        def fileName = "innerFile"
        def innerFileName = subDirName + File.separatorChar + fileName

        File innerFile = new File(WATCHED_DIRECTORY + File.separatorChar + innerFileName)

        // Make subdirectories
        innerFile.getParentFile().mkdirs()

        //Create file in the subdirectory
        boolean isCreated = innerFile.createNewFile()
        assertThat "The file '$innerFile.absolutePath' was not created successfully", isCreated, is(true)

        //The subdirectory is created after the service initialization. As no modifications are watched the new subdir can not be registered
        assertNoEventsAreProcessed()
    }

    @Test
    void 'subdirs are registered and modifications watched'(){
        // Watch subdirectories and their modifications
        watchService = new RelativeWatchService(WATCHED_DIRECTORY,true,true)

        def subDirName = "correctlyWatchedSubDir"
        def fileName = "correctSubDirInnerFile"
        def innerFileName = subDirName + File.separatorChar + fileName

        File innerFile = new File(WATCHED_DIRECTORY + File.separatorChar + innerFileName)

        // Make all the subdirectories before running the service
        innerFile.getParentFile().mkdirs()

        watchService.activate()

        boolean isCreated = innerFile.createNewFile()
        assertThat "The file '$innerFile.absolutePath' was not created successfully", isCreated, is(true)

        assertAllEventsAreProcessed(subDirName,innerFile,innerFileName)
    }

    @Test
    void 'subdirs are not registered and modifications are not watched'(){
        // Do not watch the subdirectories of the root directory
        watchService = new RelativeWatchService(WATCHED_DIRECTORY,false,false)

        def innerFileName = "watchRequestSubDir"+ File.separatorChar + "watchRequestInnerFile"

        File innerFile = new File(WATCHED_DIRECTORY + File.separatorChar + innerFileName)
        innerFile.getParentFile().mkdirs()

        watchService.activate()

        // Consequent creation and deletion in order to generate any watch events for the subdirectory
        boolean isCreated = innerFile.createNewFile()
        assertThat "The file '$innerFile.absolutePath' was not created successfully", isCreated, is(true)

        boolean isDeleted = innerFile.delete()
        assertThat "Inner file is not deleted", isDeleted, is(true)

        assertNoEventsAreProcessed()
    }

    @Test
    void 'subdirs are registered, but dirs modifications are not watched'() {
        // Do watch the subdirectories of the root directory, but do not watch directory modifications
        watchService = new RelativeWatchService(WATCHED_DIRECTORY,true,false)

        def innerFileName = "watchRequestSubDir"+ File.separatorChar + "watchRequestInnerFile"
        File innerFile = new File(WATCHED_DIRECTORY + File.separatorChar + innerFileName)
        // Make all the subdirectories before running the service
        innerFile.getParentFile().mkdirs()

        watchService.activate()

        boolean isCreated = innerFile.createNewFile()
        assertThat "The file '$innerFile.absolutePath' was not created successfully", isCreated, is(true)

        assertFileCreateEventIsProcessed(innerFile,innerFileName)

        watchService.allFullEvents.clear();
        
        assertNoEventsAreProcessed()        
    }

    @Test
    void 'subdirs are not registered, but dirs modifications are watched'(){
        // Do not watch the subdirectories of the root directory, but watch directory modifications
        watchService = new RelativeWatchService(WATCHED_DIRECTORY,false,true)

        def subDirName = "subDirModifications"
        def innerFileName = subDirName + File.separatorChar + "modificationsInnerFile"

        File innerFile = new File(WATCHED_DIRECTORY + File.separatorChar + innerFileName)
        innerFile.getParentFile().mkdirs()

        watchService.activate()

        boolean isCreated = innerFile.createNewFile()
        assertThat "The file '$innerFile.absolutePath' was not created successfully", isCreated, is(true)

        // Wait for possible watch event for the maximum timeout
        sleep NO_EVENT_TIMEOUT_IN_SECONDS*1000

        assertDirectoryModifyEventIsProcessed(subDirName)

        // Clear the asserted event
        watchService.allFullEvents.clear()

        boolean isDeleted = innerFile.delete()
        assertThat "Inner file '$innerFile.absolutePath' is not deleted", isDeleted, is(true)

        assertNoEventsAreProcessed()        
    }
    void assertNoEventsAreProcessed(){
        // Wait for a possible event for the maximum timeout
        sleep NO_EVENT_TIMEOUT_IN_SECONDS*1000

        assertThat "No watch events are expected, but were: " + watchService.allFullEvents, watchService.allFullEvents.size(), is(0)
    }

    void assertAllEventsAreProcessed(def subDirName,def innerFile, def innerFileName){
        //This could vary across different platforms. For more information see "Platform dependencies" section in the WatchService documentation
        def expectedEvents = SystemUtils.IS_OS_WINDOWS ? 2 : 1
        waitForAssert({assertThat "Exactly $expectedEvents watch events were expected within $NO_EVENT_TIMEOUT_IN_SECONDS seconds, but were: " + watchService.allFullEvents, watchService.allFullEvents.size(), is(expectedEvents)},NO_EVENT_TIMEOUT_IN_SECONDS*1000,1000)

        FullEvent fileEvent = watchService.allFullEvents[0]
        assertThat "File '$innerFile.absolutePath' creation was not detected. All events detected: " + watchService.allFullEvents, fileEvent.eventKind, is(ENTRY_CREATE)
        assertThat "File '$innerFile.absolutePath' name expected in the modified event. All events detected: " + watchService.allFullEvents, fileEvent.eventPath.toString(), is(innerFileName)

        if(SystemUtils.IS_OS_WINDOWS){
            FullEvent dirEvent = watchService.allFullEvents[1]
            assertThat "Directory $subDirName modification was not detected. All events detected: " + watchService.allFullEvents, dirEvent.eventKind, is(ENTRY_MODIFY)
            assertThat "Subdirectory was not found in the modified event", dirEvent.eventPath.toString(), is(subDirName)
        }
    }

    void assertDirectoryCraeteEventIsProcessed(def subDirName) {
        //Single event for directory creation is present
        def expectedEvents = 1
        waitForAssert({assertThat "Exactly $expectedEvents watch events were expected within $NO_EVENT_TIMEOUT_IN_SECONDS seconds, but were: " + watchService.allFullEvents, watchService.allFullEvents.size(), is(expectedEvents)},NO_EVENT_TIMEOUT_IN_SECONDS*1000,1000)
        FullEvent event = watchService.allFullEvents[0]
        assertThat "Directory $subDirName craetion was not detected. All events detected: " + watchService.allFullEvents, event.eventKind, is(ENTRY_CREATE)
        assertThat "Subdirectory was not found in the creation event", event.eventPath.toString(), is(subDirName)
    }

    void assertFileCreateEventIsProcessed(def innerFile, def innerFileName) {
        //Single event for file creation is present
        def expectedEvents = 1
        waitForAssert({assertThat "Exactly $expectedEvents watch events were expected within $NO_EVENT_TIMEOUT_IN_SECONDS seconds, but were: " + watchService.allFullEvents, watchService.allFullEvents.size(), is(expectedEvents)},NO_EVENT_TIMEOUT_IN_SECONDS*1000,1000)
        FullEvent fileEvent = watchService.allFullEvents[0]
        assertThat "File '$innerFile.absolutePath' creation was not detected. All events detected: " + watchService.allFullEvents, fileEvent.eventKind, is(ENTRY_CREATE)
        assertThat "File '$innerFile.absolutePath' name expected in the modified event. All events detected: " + watchService.allFullEvents, fileEvent.eventPath.toString(), is(innerFileName)
    }

    void assertDirectoryModifyEventIsProcessed(def subDirName) {
        //Create file is not detected, only the modification event is detected
        def expectedEvents = SystemUtils.IS_OS_WINDOWS ? 1 : 0
        waitForAssert({assertThat "Exactly $expectedEvents watch events were expected within $NO_EVENT_TIMEOUT_IN_SECONDS seconds, but were: " + watchService.allFullEvents, watchService.allFullEvents.size(), is(expectedEvents)},NO_EVENT_TIMEOUT_IN_SECONDS*1000,1000)

        if(SystemUtils.IS_OS_WINDOWS){
            FullEvent fileEvent = watchService.allFullEvents[0]
            assertThat "Directory $subDirName modification was not detected. All events detected: " + watchService.allFullEvents, fileEvent.eventKind, is(ENTRY_MODIFY)
            assertThat "Subdirectory was not found in the modified event", fileEvent.eventPath.toString(), is(subDirName)
        } else {
            assertThat "No events are expected if not Windows OS" + watchService.allFullEvents, watchService.allFullEvents.size(), is(0)
        }
    }

    void assertByRelativePath(String fileName) {
        File file = new File(WATCHED_DIRECTORY + File.separatorChar + fileName)
        file.getParentFile().mkdirs()

        assertThat "The file '$file.absolutePath' should not be present before the watch service activation", file.exists(), is(false)

        // We have to be sure that all the subdirectories of the watched directory are created when the watched service is activated
        watchService.activate()

        boolean isCreated = file.createNewFile()
        assertThat "The file '$file.absolutePath' was not created successfully", isCreated, is(true)
        /*
         * In some OS, like MacOS, creating an empty file is not related to sending an ENTRY_CREATE event.
         * So, it's necessary to put some initial content in that file.
         */
        if(!SystemUtils.IS_OS_WINDOWS) {
            println "MAC OS"
            file << "Initial content"
            sleep 10000
        }
        fullEventAssertionsByKind(fileName, ENTRY_CREATE, false)

        // File modified
        file << "Additional content"
        fullEventAssertionsByKind(fileName, ENTRY_MODIFY, false)

        // File deleted
        boolean isDeleted = file.delete()
        assertThat "Test file '$file.absolutePath' is not deleted", isDeleted, is(true)
        fullEventAssertionsByKind(fileName, ENTRY_DELETE, true)
    }

    void fullEventAssertionsByKind(String fileName, kind, osSpecific){
        waitForAssert({
            assertThat "At least one watch event is expected within $NO_EVENT_TIMEOUT_IN_SECONDS seconds.",
                    watchService.allFullEvents.size() >= 1,
                    is(true)},NO_EVENT_TIMEOUT_IN_SECONDS*1000,1000);

        if(osSpecific && kind.equals(ENTRY_DELETE)){
            // There is possibility that one more modify event is triggered on some OS
            // so sleep a bit extra time
            sleep 500
            cleanUpOsSpecificModifyEvent()
        }

        waitForAssert {assertThat "Exactly one event of kind $kind for file $fileName is expected shortly after the file has been altered. Here are the found events: " + watchService.allFullEvents, watchService.allFullEvents.size(), is(1)}
        FullEvent fullEvent = watchService.allFullEvents[0]

        assertThat "The path of the processed $kind event should be relative to the watched directory", fullEvent.eventPath.toString(), is(fileName)
        assertThat "An event of corresponding kind $kind is expected for file $fileName", fullEvent.eventKind, is(kind)
        assertThat "At least one watch event of kind $kind is expected", fullEvent.watchEvent.count() >= 1, is(true)
        assertThat "The watch event kind should be the same as the kind provided", fullEvent.watchEvent.kind(), is(fullEvent.eventKind)
        def fileNameOnly = fileName.contains(File.separatorChar.toString()) ? fileName.substring(fileName.lastIndexOf(File.separatorChar.toString()) + 1,fileName.length()) : fileName
        assertThat "The watch event context have to be the file name only", fullEvent.watchEvent.context().toString(),is(fileNameOnly)

        // Clear all the asserted events
        watchService.allFullEvents.clear()
    }

    /**
     * Cleanup the OS specific ENTRY_MODIFY event as it will not be needed for the assertion
     */
    private cleanUpOsSpecificModifyEvent() {
        // As the implementation of the watch events is OS specific, it can happen that when the file is deleted two events are fired - ENTRY_MODIFY followed by an ENTRY_DELETE
        // This is usually observed on Windows and below is the workaround
        // Related discussion in StackOverflow: http://stackoverflow.com/questions/28201283/watchservice-windows-7-when-deleting-a-file-it-fires-both-entry-modify-and-e
        boolean isDeletedWithPrecedingModify = watchService.allFullEvents.size() == 2 && watchService.allFullEvents[0].eventKind.equals(ENTRY_MODIFY)
        if(isDeletedWithPrecedingModify){
            // Remove the ENTRY_MODIFY element as it is not needed
            watchService.allFullEvents.remove(0)
        }
    }

    class RelativeWatchService extends AbstractWatchService{

        String rootWatchPath

        boolean watchSubDirs

        boolean watchDirectoryChanges

        // Synchronize list as several watcher threads can write into it
        def allFullEvents = [].asSynchronized()

        RelativeWatchService(String rootPath, boolean watchSubDirectories, boolean watchDirChanges){
            rootWatchPath = rootPath
            watchSubDirs = watchSubDirectories
            watchDirectoryChanges = watchDirChanges
        }


        @Override
        protected AbstractWatchQueueReader buildWatchQueueReader(WatchService watchServiceImpl, Path toWatch,Map<WatchKey, Path> registeredKeys) {
            def queueReader = new AbstractWatchQueueReader(watchServiceImpl, toWatch,registeredKeys) {
                        @Override
                        protected void processWatchEvent(WatchEvent<?> event, Kind<?> kind, Path path) {
                            println "kind: " + kind
                            FullEvent fullEvent = new FullEvent(event,kind,path)
                            println "fullEvent kind: " + fullEvent.eventKind
                            if (kind.equals(ENTRY_CREATE)) {
                                Path relativePath = baseWatchedDir.resolve(path);
                                if (relativePath.toFile().isDirectory() && watchSubDirectories()) {
                                    registerAllDirectoriesInternal(relativePath, registeredKeys);
                                }
                            }
                            allFullEvents << fullEvent
                        }
                    };
            queueReader.setWatchingDirectoryChanges(watchDirectoryChanges)
            return queueReader

        }

        @Override
        protected String getSourcePath() {
            return rootWatchPath
        }

        @Override
        protected WatchKey registerDirectory(Path path) throws IOException {
            WatchKey registrationKey = path.register(watchService,ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
            return registrationKey
        }

        @Override
        protected boolean watchSubDirectories() {
            return watchSubDirs;
        }
    }

    class FullEvent{
        WatchEvent<?> watchEvent
        Kind<?> eventKind
        Path eventPath

        public FullEvent(WatchEvent<?> event, Kind<?> kind, Path path){
            watchEvent = event
            eventKind = kind
            eventPath = path
        }

        @Override
        public String toString() {
            return "Watch Event: count " + watchEvent.count +  "; kind: " + eventKind + "; path: " + eventPath;
        }
    }
}
