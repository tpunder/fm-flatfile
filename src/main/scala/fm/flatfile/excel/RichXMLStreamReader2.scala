/*
 * Copyright 2014 Frugal Mechanic (http://frugalmechanic.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fm.flatfile.excel

import org.codehaus.stax2.XMLStreamReader2
import javax.xml.stream.XMLStreamConstants._
import javax.xml.stream.XMLStreamException
import scala.annotation.switch

private[excel] final class RichXMLStreamReader2(val sr: XMLStreamReader2) /* extends AnyVal */ {
  /**
   * Opposite of XMLStreamReader.require()
   */
  def requireNot(tpe: Int): Unit = {
    if (sr.getEventType() == tpe) throw new XMLStreamException(s"Expected current tag to be NOT $tpe", sr.getLocation())
  }
  
  def requireEither(one: Int, two: Int): Unit = {
    val tpe: Int = sr.getEventType()
    if (one != tpe && two != tpe) throw new XMLStreamException(s"Expected current tag to be either $one or $two", sr.getLocation())
  }
  
  @inline def require(cond: Boolean, msg: => String): Unit = {
    if (!cond) throw new XMLStreamException(msg, sr.getLocation())
  }
  
  /**
   * Given a freshly opened XMLStreamReader2, move to the START_ELEMENT of the ROOT element (skipping stuff like DTDs)
   */
  def seekToRootElement(): Unit = seekToRootElement(null)
  
  /**
   * Given a freshly opened XMLStreamReader2, move to the START_ELEMENT of the ROOT element (skipping stuff like DTDs) and require that the root name be the expectedName
   */
  def seekToRootElement(expectedName: String): Unit = {
    require (sr.getDepth() == 0, "Current Depth should be 0!")
    while (sr.getEventType != START_ELEMENT) sr.next()
    if (null != expectedName) sr.require(START_ELEMENT, null, expectedName)
  }
  
  /**
   * Seek to the first parsing event after the START_ELEMENT of the root tag
   */
  def seekFirstEventPastRootElement(): Unit = seekFirstEventPastRootElement(null)
  
  /**
   * Seek to the first parsing event after the START_ELEMENT of the root tag after verifying the root tag name
   */
  def seekFirstEventPastRootElement(expectedName: String): Unit = {
    seekToRootElement(expectedName)
    // We are positioned on the START_ELEMENT of the rootName tag, so advance to the next parsing event
    require(sr.hasNext(), "Missing next parsing event after the ROOT element")
    sr.next()
  }
  
  /**
   * Seek to the first sibling element (the first START_ELEMENT at the current depth) that matching name
   * 
   * e.g.
   * 
   *  <root>
   *    <foo>
   *      <bar></bar>
   *      <target>We don't want this</target>
   *    </foo>
   *    <target>This is what we want</target>
   *  </root>
   *  
   *  A call to seekFirstEventPastRootElement leaves us just after the <root> element.  Then a call
   *  to seekToSiblingElement("target") will skip <foo> and the nested <bar> and <target> elements 
   *  and end up on the second <target> element that is a sibling to <foo> 
   */
  def seekToSiblingElement(name: String): Unit = {
    requireEither(START_ELEMENT, END_ELEMENT)
    
    // If we are at a START_ELEMENT then skip the element to get to the next sibling
    if (sr.getEventType == START_ELEMENT) sr.skipElement()
    
    var done: Boolean = false
    
    while (!done && sr.hasNext) {
      val tpe: Int = sr.next()
      
      if (tpe == START_ELEMENT) {
        if (sr.getLocalName == name) done = true
        else sr.skipElement()
      } else if (tpe == END_ELEMENT) {
        throw new XMLStreamException(s"Couldn't find matching child element: $name", sr.getLocation())
      }
    }
    
    sr.require(START_ELEMENT, null, name)
  }
  
  def seekToChildElement(name: String): Unit = {
    if (trySeekToChildElement(name)) {
      sr.require(START_ELEMENT, null, name)
    } else {
      throw new XMLStreamException(s"Couldn't find matching child element: $name", sr.getLocation())
    }
  }
  
  def trySeekToChildElement(name: String): Boolean = {
    // We should be pointing at a start element
    sr.require(START_ELEMENT, null, null)
    
    while (sr.hasNext) {
      val tpe: Int = sr.next()
      
      if (tpe == START_ELEMENT) {
        if (sr.getLocalName == name) return true
        else sr.skipElement()
      } else if (tpe == END_ELEMENT) {
        // No matching element
        return false
      }
    }
    
    false
  }
  
  def seekToEndOfParentElement(): Unit = {
    requireEither(START_ELEMENT, END_ELEMENT)
    
    val targetDepth: Int = sr.getDepth() - 1
    
    require(targetDepth >= 0, "Already at depth 0")
    
    // If we are at a START_ELEMENT then skip the element to get to the next sibling
    if (sr.getEventType == START_ELEMENT) sr.skipElement()
    
    var done: Boolean = false
    
    while(!done && sr.hasNext) {
      val tpe: Int = sr.next()
      if (tpe == START_ELEMENT) sr.skipElement()
      else if (tpe == END_ELEMENT && sr.getDepth == targetDepth) done = true
    }
    
    
    require(sr.getDepth() == targetDepth, s"Something failed:  Target Depth: $targetDepth  Current Depth: ${sr.getDepth()}") 
    sr.require(END_ELEMENT, null, null)
  }
  
  /**
   * Read the contents of an Element.  Only works at the START_ELEMENT and if the element has no other nested elements
   */
  def readElementText(): String = {
    sr.require(START_ELEMENT, null, null)
    
    var done: Boolean = false
    var text: String = ""
    
    while (!done && sr.hasNext()) {
      val tpe: Int = sr.next()
      
      (tpe: @switch) match {
        case END_ELEMENT => done = true
        case CHARACTERS | CDATA | SPACE => text += sr.getText()
        case ATTRIBUTE | COMMENT => // Ignore
        case _ => require(false, s"Unexpcted Event Type: ${sr.getEventType()}")
      }
      
    }
    
    sr.require(END_ELEMENT, null, null)
    
    text
  }
  
  def tryReadChildElementText(name: String): Option[String] = {
    if (trySeekToChildElement(name)) {
      val text: String = readElementText()
      seekToEndOfParentElement()
      Some(text)
    } else None
  }
  
  def readChildElementText(name: String): String = {
    seekToChildElement(name)
    val text: String = readElementText()
    seekToEndOfParentElement()
    text
  }
  
  /**
   * Seek to the first element with name (at any depth)
   * 
   * Returns the depth that the element was found at
   */
  def seekToAnyMatching(name: String): Int = {
    var depth: Int = 0
    var done: Boolean = false
    
    while (!done && sr.hasNext) {
      val tpe: Int = sr.next()
      if (tpe == START_ELEMENT) {
        depth += 1
        if (sr.getLocalName == name) done = true
      } else if (tpe == END_ELEMENT) {
        depth -= 1
      }
    }
    
    sr.require(START_ELEMENT, null, name)
    depth
  }
  
  def getAttributeValue(name: String): String = sr.getAttributeValue(null, name)
  
  /**
   * Call f() for every START_ELEMENT that satisfies the XPath-like path.
   */
  @inline def foreach[U](path: String)(f: => U): Unit = {
    require(!path.startsWith("/"), s"Paths starting with / are not supports: $path")
    
    val pathParts: Array[String] = path.split('/')
    
    val startingDepth: Int = sr.getDepth()
   
    var done: Boolean = false
    
    while (!done && sr.hasNext) {
      val tpe: Int = sr.next()
      
      if (tpe == START_ELEMENT) {
        val depth: Int = sr.getDepth()
        val pathIdx: Int = depth - startingDepth - 1
        
        if (sr.getLocalName() != pathParts(pathIdx)) sr.skipElement()
        else if (pathIdx == pathParts.length - 1) {
          // Call the user function
          f
          require(sr.getDepth == depth, s"Invalid Usage of Foreach.  Expected to end at the same depth we started!  Depth before calling f: $depth   Depth after calling f:  ${sr.getDepth}")
        }
      } else if (tpe == END_ELEMENT && sr.getDepth == startingDepth) {
        // This is the END_ELEMENT event of the tag we started with so we are done
        done = true
      }
    }

    require(sr.getDepth == startingDepth, "Should have ended at the same depth we started!")
  }

}