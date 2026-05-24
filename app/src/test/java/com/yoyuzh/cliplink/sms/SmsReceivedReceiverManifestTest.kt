package com.yoyuzh.cliplink.sms

import android.Manifest
import android.provider.Telephony
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsReceivedReceiverManifestTest {
    @Test
    fun `sms receiver accepts system broadcasts and remains permission protected`() {
        val manifest = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File("src/main/AndroidManifest.xml"))

        val receivers = manifest.getElementsByTagName("receiver")
        val receiver = (0 until receivers.length)
            .map { receivers.item(it) }
            .single {
                it.attributes.getNamedItem("android:name").nodeValue == ".sms.SmsReceivedReceiver"
            }
        val attributes = receiver.attributes

        assertEquals("true", attributes.getNamedItem("android:exported").nodeValue)
        assertEquals(Manifest.permission.BROADCAST_SMS, attributes.getNamedItem("android:permission").nodeValue)

        val actions = receiver.childNodes.asElementSequence()
            .filter { it.nodeName == "intent-filter" }
            .flatMap { filter -> filter.childNodes.asElementSequence() }
            .filter { it.nodeName == "action" }
            .map { it.attributes.getNamedItem("android:name").nodeValue }
            .toList()

        assertTrue(actions.contains(Telephony.Sms.Intents.SMS_RECEIVED_ACTION))
    }

    private fun org.w3c.dom.NodeList.asElementSequence(): Sequence<org.w3c.dom.Node> = sequence {
        for (index in 0 until length) {
            yield(item(index))
        }
    }
}
