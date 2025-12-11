package ai.saniou.thread.data.parser.rss

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("rss")
data class Rss(
    val channel: Channel,
    val version: String?,
)

@Serializable
@XmlSerialName("channel")
data class Channel(
    @XmlElement(true)
    val title: String?,
    @XmlElement(true)
    val link: String?,
    @XmlElement(true)
    val description: String?,
    @XmlElement(true)
    @XmlSerialName("item")
    val items: List<Item> = emptyList()
)

@Serializable
@XmlSerialName("item")
data class Item(
    @XmlElement(true)
    val title: String?,
    @XmlElement(true)
    val link: String?,
    @XmlElement(true)
    val description: String?,
    @XmlElement(true)
    @XmlSerialName(
        value = "encoded",
        namespace = "http://purl.org/rss/1.0/modules/content/",
        prefix = "content"
    )
    val contentEncoded: String?,
    @XmlElement(true)
    val pubDate: String?,
    @XmlElement(true)
    @XmlSerialName(
        value = "creator",
        namespace = "http://purl.org/dc/elements/1.1/",
        prefix = "dc"
    )
    val creator: String?,
    @XmlElement(true)
    val category: String? = null
)
