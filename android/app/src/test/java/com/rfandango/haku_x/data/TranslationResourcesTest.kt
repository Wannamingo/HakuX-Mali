package com.rfandango.haku_x.data

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

class TranslationResourcesTest {
  private fun resources(folder: String): Map<String, Element> {
    val factory = DocumentBuilderFactory.newInstance()
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    return File("src/main/res/$folder").listFiles()!!.filter { it.extension == "xml" }.flatMap { file ->
      val children = factory.newDocumentBuilder().parse(file).documentElement.childNodes
      (0 until children.length).mapNotNull { children.item(it) as? Element }
        .filter { it.tagName in listOf("string", "plurals") && it.getAttribute("translatable") != "false" }
        .map { it.getAttribute("name") to it }
    }.toMap()
  }
  private val format = Regex("%[0-9]+\\$(?:[0-9]*\\.)?[0-9]*[a-zA-Z]|%%")
  private fun placeholders(text: String) = format.findAll(text).map { it.value }.sorted().toList()

  @Test fun russianCoversEveryEnglishResourceAndFormatArgument() {
    val english = resources("values")
    val russian = resources("values-ru")
    assertEquals(english.keys, russian.keys)
    for ((name, en) in english) {
      val ru = russian.getValue(name)
      assertEquals(name, en.tagName, ru.tagName)
      val example = if (en.tagName == "plurals") en.getElementsByTagName("item").item(0).textContent else en.textContent
      if (ru.tagName == "plurals") {
        val items = ru.getElementsByTagName("item")
        val quantities = (0 until items.length).map { (items.item(it) as Element).getAttribute("quantity") }.toSet()
        assertTrue(name, quantities.containsAll(listOf("one", "few", "many", "other")))
        for (i in 0 until items.length) assertEquals(name, placeholders(example), placeholders(items.item(i).textContent))
      } else assertEquals(name, placeholders(example), placeholders(ru.textContent))
    }
  }
}
