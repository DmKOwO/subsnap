package com.example.subsnap.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ScreenshotStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createTestScreenshotFile(name: String, content: String = "fake_image_data"): File {
        val file = File(tempFolder.root, name)
        file.writeText(content)
        return file
    }

    @Test
    fun deleteScreenshot_movesFileToTrash_andHidesFromScreenshotsFlow() = runTest {
        val file1 = createTestScreenshotFile("snap1.webp")
        createTestScreenshotFile("snap2.webp")

        val storage = ScreenshotStorage(baseDir = tempFolder.root)
        assertEquals(2, storage.screenshots.value.size)

        val item1 = storage.screenshots.value.find { it.file.name == "snap1.webp" }
        assertNotNull(item1)

        val deleted = storage.deleteScreenshot(item1!!.id)
        assertNotNull(deleted)
        assertEquals(item1.id, deleted!!.id)

        // Storage flow updated to 1 item
        assertEquals(1, storage.screenshots.value.size)
        assertFalse(file1.exists())
        assertTrue(File(tempFolder.root, ".trash_snap1.webp").exists())
    }

    @Test
    fun undoDelete_restoresOriginalFile_andFlowItem() = runTest {
        val file1 = createTestScreenshotFile("snap1.webp")
        val storage = ScreenshotStorage(baseDir = tempFolder.root)

        val item1 = storage.screenshots.value.first()
        storage.deleteScreenshot(item1.id)
        assertEquals(0, storage.screenshots.value.size)
        assertFalse(file1.exists())

        val undone = storage.undoDelete(item1.id)
        assertTrue(undone)
        assertTrue(file1.exists())
        assertEquals(1, storage.screenshots.value.size)
        assertFalse(File(tempFolder.root, ".trash_snap1.webp").exists())
    }

    @Test
    fun undoDelete_withMismatchedId_returnsFalseAndDoesNotRestore() = runTest {
        createTestScreenshotFile("snap1.webp")
        val storage = ScreenshotStorage(baseDir = tempFolder.root)

        val item1 = storage.screenshots.value.first()
        storage.deleteScreenshot(item1.id)

        val undone = storage.undoDelete("different_id")
        assertFalse(undone)
        assertFalse(item1.file.exists())
        assertTrue(File(tempFolder.root, ".trash_snap1.webp").exists())
    }

    @Test
    fun purgeTrash_deletesTrashFilePermanently() = runTest {
        val file1 = createTestScreenshotFile("snap1.webp")
        val storage = ScreenshotStorage(baseDir = tempFolder.root)

        val item1 = storage.screenshots.value.first()
        storage.deleteScreenshot(item1.id)
        val trashFile = File(tempFolder.root, ".trash_snap1.webp")
        assertTrue(trashFile.exists())

        storage.purgeTrash(item1.id)
        assertFalse(trashFile.exists())
        assertFalse(file1.exists())

        // Undo after purge returns false
        val undone = storage.undoDelete(item1.id)
        assertFalse(undone)
    }

    @Test
    fun rapidDeletion_purgesPreviousTrash_andProtectsNewItemFromOldSnackbarDismiss() = runTest {
        val file1 = createTestScreenshotFile("snap1.webp")
        val file2 = createTestScreenshotFile("snap2.webp")
        val storage = ScreenshotStorage(baseDir = tempFolder.root)

        val item1 = storage.screenshots.value.find { it.file.name == "snap1.webp" }!!
        val item2 = storage.screenshots.value.find { it.file.name == "snap2.webp" }!!

        // 1. Delete item 1
        storage.deleteScreenshot(item1.id)
        val trashFile1 = File(tempFolder.root, ".trash_snap1.webp")
        assertTrue(trashFile1.exists())

        // 2. Immediately delete item 2 before snackbar 1 finished
        storage.deleteScreenshot(item2.id)
        val trashFile2 = File(tempFolder.root, ".trash_snap2.webp")
        assertTrue(trashFile2.exists())

        // Verified: Item 1 trash file was purged upon deleting item 2, no disk leak!
        assertFalse(trashFile1.exists())

        // 3. Snackbar 1 times out / is dismissed, triggering purgeTrash(item1.id)
        storage.purgeTrash(item1.id)

        // Verified: Item 2 trash file is NOT deleted because id mismatched!
        assertTrue(trashFile2.exists())

        // 4. User clicks undo on snackbar 2
        val undone2 = storage.undoDelete(item2.id)
        assertTrue(undone2)
        assertTrue(file2.exists())
        assertFalse(trashFile2.exists())
        assertEquals(1, storage.screenshots.value.size)
    }

    @Test
    fun cleanupStaleTrash_purgesOrphanedTrashFilesOnStartup() = runTest {
        createTestScreenshotFile(".trash_old1.webp")
        createTestScreenshotFile(".trash_old2.jpg")
        val regularFile = createTestScreenshotFile("regular.webp")

        ScreenshotStorage(baseDir = tempFolder.root)

        // Init runs cleanupStaleTrash()
        assertFalse(File(tempFolder.root, ".trash_old1.webp").exists())
        assertFalse(File(tempFolder.root, ".trash_old2.jpg").exists())
        assertTrue(regularFile.exists())
    }
}
