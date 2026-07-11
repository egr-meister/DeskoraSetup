package com.deskora.setup

import com.deskora.setup.data.AppJson
import com.deskora.setup.model.AppData
import com.deskora.setup.model.DeskSetup
import com.deskora.setup.model.SetupItem
import com.deskora.setup.model.SetupItemType
import com.deskora.setup.util.BuiltInTemplates
import com.deskora.setup.util.Search
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataAndSearchTest {

    @Test
    fun builtInTemplates_haveStableIdsAndNoDuplicates() {
        val all = BuiltInTemplates.all()
        val ids = all.map { it.id }
        assertEquals(ids.size, ids.toSet().size) // unique
        assertTrue(BuiltInTemplates.WORK_ID in ids)
        assertTrue(BuiltInTemplates.STUDY_ID in ids)
        assertTrue(BuiltInTemplates.GAMING_ID in ids)
        assertTrue(BuiltInTemplates.BLANK_ID in ids)
        all.forEach { assertFalse(it.userCreated) }
    }

    @Test
    fun builtInTemplates_useGenericLabelsOnly() {
        val words = BuiltInTemplates.all().flatMap { it.defaultItems }.map { it.name.lowercase() }
        // Ensure no obvious brand-like tokens are present.
        val banned = listOf("apple", "dell", "logitech", "samsung", "sony", "hp", "asus", "razer")
        assertTrue(words.none { w -> banned.any { w.contains(it) } })
    }

    @Test
    fun decodeListSafely_recoversFromCorruptElements() {
        val serializer = SetupItem.serializer()
        val goodJson = AppJson.instance.encodeToString(
            ListSerializer(serializer),
            listOf(SetupItem(id = "a", setupId = "s", name = "Monitor"))
        )
        assertEquals(1, AppJson.decodeListSafely(goodJson, serializer).size)

        // Totally corrupt JSON -> empty, no crash.
        assertTrue(AppJson.decodeListSafely("{not json", serializer).isEmpty())
        assertTrue(AppJson.decodeListSafely("", serializer).isEmpty())
        assertTrue(AppJson.decodeListSafely(null, serializer).isEmpty())

        // Array with one valid and one malformed element -> keeps the valid one.
        val mixed = """[{"id":"a","setupId":"s","name":"Ok"}, 12345]"""
        val recovered = AppJson.decodeListSafely(mixed, serializer)
        assertEquals(1, recovered.size)
        assertEquals("a", recovered.first().id)
    }

    @Test
    fun activeSetup_fallsBackWhenActiveIdMissing() {
        val data = AppData(
            setups = listOf(DeskSetup(id = "s1", name = "One"), DeskSetup(id = "s2", name = "Two")),
            settings = com.deskora.setup.model.AppSettings(activeSetupId = "missing")
        )
        assertNotNull(data.activeSetup)
        assertEquals("s1", data.activeSetup?.id)
    }

    @Test
    fun activeSetup_skipsArchived() {
        val data = AppData(
            setups = listOf(DeskSetup(id = "s1", name = "One", archived = true), DeskSetup(id = "s2", name = "Two")),
            settings = com.deskora.setup.model.AppSettings(activeSetupId = null)
        )
        assertEquals("s2", data.activeSetup?.id)
    }

    @Test
    fun search_filtersByNameAndType() {
        val setup = DeskSetup(id = "s", name = "Main")
        val data = AppData(
            setups = listOf(setup),
            items = listOf(
                SetupItem(id = "i1", setupId = "s", name = "Left Monitor", itemType = SetupItemType.Monitor),
                SetupItem(id = "i2", setupId = "s", name = "Keyboard", itemType = SetupItemType.Keyboard)
            )
        )
        val r = Search.run(data, "s", "monitor")
        assertEquals(1, r.items.size)
        assertEquals("i1", r.items.first().id)

        val empty = Search.run(data, "s", "")
        assertTrue(empty.isEmpty)
    }
}
