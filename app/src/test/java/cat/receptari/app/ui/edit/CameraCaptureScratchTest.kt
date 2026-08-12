package cat.receptari.app.ui.edit

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CameraCaptureScratchTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `successful capture is consumed once and scratch file is deleted`() {
        val scratch = CameraCaptureScratch(temporaryFolder.newFolder("camera"))
        val fileName = scratch.createFileName(123L)
        val expected = byteArrayOf(1, 2, 3)
        val file = scratch.file(fileName).apply { writeBytes(expected) }

        val actual = scratch.consume(fileName, captured = true)

        assertArrayEquals(expected, actual)
        assertFalse(file.exists())
    }

    @Test
    fun `cancelled capture deletes camera scratch file`() {
        val scratch = CameraCaptureScratch(temporaryFolder.newFolder("camera"))
        val fileName = scratch.createFileName(123L)
        val file = scratch.file(fileName).apply { writeBytes(byteArrayOf(1)) }

        assertNull(scratch.consume(fileName, captured = false))
        assertFalse(file.exists())
    }

    @Test
    fun `abandoned cleanup preserves only current pending capture`() {
        val directory = temporaryFolder.newFolder("camera")
        val scratch = CameraCaptureScratch(directory)
        val abandoned = scratch.file(scratch.createFileName(1L)).apply { createNewFile() }
        val pending = scratch.file(scratch.createFileName(2L)).apply { createNewFile() }
        val unrelated = directory.resolve("keep.txt").apply { createNewFile() }

        scratch.clearAbandoned(pending.name)

        assertFalse(abandoned.exists())
        assertTrue(pending.exists())
        assertTrue(unrelated.exists())
    }
}
