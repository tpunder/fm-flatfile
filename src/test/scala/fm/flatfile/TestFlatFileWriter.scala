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
package fm.flatfile

import fm.common.{OutputStreamResource, UTF_8_BOM}
import org.scalatest.FunSuite
import org.scalatest.Matchers
import java.io.{ByteArrayOutputStream, StringWriter}
import java.nio.charset.{Charset, StandardCharsets}

final class TestFlatFileWriter extends FunSuite with Matchers {
  private val default = FlatFileWriterOptions.default
  private val csv = FlatFileWriterOptions(sep = ",")
  private val tsv = FlatFileWriterOptions(sep = "\t")

  private val multiByteString: String = "oneByte: \u0024 twoByte: \u00A2 threeByte: \u20AC fourByteSupplementary: \uD83D\uDCA5"

  test("Basic Row Writing") {
    Seq(default, csv).foreach { options =>
      checkRow(options, Seq("foo"), "foo")
      checkRow(options, Seq("foo","bar"), "foo,bar")
      checkRow(options, Seq("foo","bar","baz"), "foo,bar,baz")
      checkRow(options, Seq(multiByteString), multiByteString)
      checkRow(options, Seq("foo", multiByteString, "bar"), s"foo,$multiByteString,bar")
    }
    
    checkRow(tsv, Seq("foo"), "foo")
    checkRow(tsv, Seq("foo","bar"), "foo\tbar")
    checkRow(tsv, Seq("foo","bar","baz"), "foo\tbar\tbaz")
    checkRow(tsv, Seq(multiByteString), multiByteString)
    checkRow(tsv, Seq("foo", multiByteString, "bar"), s"foo\t$multiByteString\tbar")
  }
  
  test("Quoting/Escaping") {
    checkRow(csv, Seq("Hello \" World"), "\"Hello \"\" World\"")
    checkRow(csv, Seq("\"Hello World\""), "\"\"\"Hello World\"\"\"")
    
    checkRow(csv, Seq("foo","b\"ar","baz"), "foo,\"b\"\"ar\",baz")
    checkRow(tsv, Seq("foo","b\"ar","baz"), "foo\t\"b\"\"ar\"\tbaz")
    
    checkRow(csv, Seq("foo","b\"ar","b\naz"), "foo,\"b\"\"ar\",\"b\naz\"")
    checkRow(tsv, Seq("foo","b\"ar","b\naz"), "foo\t\"b\"\"ar\"\t\"b\naz\"")
  }

  private def checkMultiRow(options: FlatFileWriterOptions)(f: FlatFileWriter => Unit)(expected: String): Unit = {
    checkMultiRowWriter(options)(f)(expected)

    checkMultiRowOutputStream(options, UTF_8_BOM)(f)(expected)
    checkMultiRowOutputStream(options, StandardCharsets.UTF_8)(f)(expected)
    checkMultiRowOutputStream(options, StandardCharsets.UTF_16)(f)(expected)
    checkMultiRowOutputStream(options, StandardCharsets.UTF_16BE)(f)(expected)
    checkMultiRowOutputStream(options, StandardCharsets.UTF_16LE)(f)(expected)
  }

  private def checkMultiRowWriter(options: FlatFileWriterOptions)(f: FlatFileWriter => Unit)(expected: String) {
    val sw = new StringWriter
    val ffw: FlatFileWriter = new FlatFileWriter(sw, options)
    ffw.writeHeaders()
    f(ffw)
    sw.toString() should equal (expected)
  }

  private def checkMultiRowOutputStream(options: FlatFileWriterOptions, charset: Charset)(f: FlatFileWriter => Unit)(expected: String) {
    val os: ByteArrayOutputStream = new ByteArrayOutputStream
    FlatFileWriter(OutputStreamResource.wrap(os), charset, options)(f)

    val bytes: Array[Byte] = if (charset eq UTF_8_BOM) {
      val b: Array[Byte] = os.toByteArray

      // Check for the UTF-8 BOM
      b(0) should equal (0xEF.toByte)
      b(1) should equal (0xBB.toByte)
      b(2) should equal (0xBF.toByte)

      // Strip off the BOM
      b.drop(3)
    } else {
      os.toByteArray
    }

    new String(bytes, charset) should equal (expected)
  }
  
  private def checkRow(options: FlatFileWriterOptions, row: Seq[String], expected: String) {
    checkMultiRow(options){ _.writeRow(row) }(expected+"\n")
  }
  
  test("CSV Multi Row Writes - No Headers") {
    checkMultiRow(FlatFileWriterOptions(headers = None, trailingNewline = false)){ writer =>
      writer.writeRow(Seq("foo"))
      writer.writeRow(Seq("foo","bar"))
      writer.writeRow(Seq("foo","bar","baz"))
      writer.writeRow(Seq(multiByteString))
      writer.writeRow(Seq("Hello, World","foo "," bar"))
    }(s"""
foo
foo,bar
foo,bar,baz
$multiByteString
"Hello, World",foo , bar
""".trim)

  }
  
  test("CSV Multi Row Writes - No Headers - Trailing Newlines") {
    checkMultiRow(FlatFileWriterOptions(headers = None, trailingNewline = true)){ writer =>
      writer.writeRow(Seq("foo"))
      writer.writeRow(Seq("foo","bar"))
      writer.writeRow(Seq("foo","bar","baz"))
      writer.writeRow(Seq(multiByteString))
      writer.writeRow(Seq("Hello, World","foo "," bar"))
    }(s"""
foo
foo,bar
foo,bar,baz
$multiByteString
"Hello, World",foo , bar
""".trim+"\n")

  }
  
  test("CSV Multi Row Writes - Headers") {
    checkMultiRow(FlatFileWriterOptions(headers = Some(Vector("one","two","three")), trailingNewline = false)){ writer =>
      writer.write("one" -> "foo")
      writer.write("one" -> "foo", "two" -> multiByteString)
      writer.write("one" -> "foo", "three" -> multiByteString)
      writer.write("one" -> "foo", "two" -> "bar")
      writer.write("one" -> "foo", "two" -> "bar", "three" -> "baz")
      writer.write("one" -> "Hello, World", "two" -> "foo ", "three" -> " bar")
    }(s"""
one,two,three
foo,,
foo,$multiByteString,
foo,,$multiByteString
foo,bar,
foo,bar,baz
"Hello, World",foo , bar
""".trim)
  }
  
  test("TSV Multi Row Writes - Headers") {
    val tab = "\t"

    checkMultiRow(FlatFileWriterOptions(sep = "\t", headers = Some(Vector("one","two","three")), trailingNewline = false)){ writer =>
      writer.writeRow(Seq("foo"))
      writer.writeRow(Seq(multiByteString))
      writer.writeRow(Seq("foo","bar"))
      writer.writeRow(Seq("foo","bar","baz"))
      writer.writeRow(Seq("Hello, World","foo "," bar"))
    }(s"""
one${tab}two${tab}three
foo
$multiByteString
foo${tab}bar
foo${tab}bar${tab}baz
Hello, World${tab}foo ${tab} bar
""".trim)

  }
  
  test("CSV Multi Row Writes - Auto Headers") {
    val sw = new StringWriter
    val ffw = new FlatFileWriter(sw, FlatFileWriterOptions(trailingNewline = false))
    
    val rows: Vector[(String, String, String)] = Vector(
      ("foo1","bar1","baz1"),
      ("foo2","bar2","baz2"),
      ("foo3","bar3","baz3"),
      (multiByteString,multiByteString,multiByteString)
    )
    
    rows.foreach { case (one, two, three) =>
      ffw.write("one" -> one, "two" -> two, "three" -> three)
    }
    
    sw.toString() should equal(s"""
one,two,three
foo1,bar1,baz1
foo2,bar2,baz2
foo3,bar3,baz3
$multiByteString,$multiByteString,$multiByteString
""".trim)
  }
  
}