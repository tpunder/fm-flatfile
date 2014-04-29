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

import org.scalatest.FunSuite
import org.scalatest.Matchers
import java.io.StringWriter

final class TestFlatFileWriter extends FunSuite with Matchers {
  val default = FlatFileWriterOptions()
  val csv = FlatFileWriterOptions(sep = ",")
  val tsv = FlatFileWriterOptions(sep = "\t")
  
  test("Basic Row Writing") {
    Seq(default, csv).foreach { options =>
      checkRow(options, Seq("foo"), "foo")
      checkRow(options, Seq("foo","bar"), "foo,bar")
      checkRow(options, Seq("foo","bar","baz"), "foo,bar,baz")
    }
    
    checkRow(tsv, Seq("foo"), "foo")
    checkRow(tsv, Seq("foo","bar"), "foo\tbar")
    checkRow(tsv, Seq("foo","bar","baz"), "foo\tbar\tbaz")
  }
  
  test("Quoting/Escaping") {
    checkRow(csv, Seq("Hello \" World"), "\"Hello \"\" World\"")
    checkRow(csv, Seq("\"Hello World\""), "\"\"\"Hello World\"\"\"")
    
    checkRow(csv, Seq("foo","b\"ar","baz"), "foo,\"b\"\"ar\",baz")
    checkRow(tsv, Seq("foo","b\"ar","baz"), "foo\t\"b\"\"ar\"\tbaz")
    
    checkRow(csv, Seq("foo","b\"ar","b\naz"), "foo,\"b\"\"ar\",\"b\naz\"")
    checkRow(tsv, Seq("foo","b\"ar","b\naz"), "foo\t\"b\"\"ar\"\t\"b\naz\"")
  }
  
  private def checkMulti(options: FlatFileWriterOptions)(f: FlatFileWriter => Unit)(expected: String) {
    val sw = new StringWriter
    val ffw = new FlatFileWriter(sw, options)
    ffw.writeHeaders()
    f(ffw)
    sw.toString() should equal (expected)
  }
  
  private def checkRow(options: FlatFileWriterOptions, row: Seq[String], expected: String) {
    checkMulti(options){ _.writeRow(row) }(expected+"\n")
  }
  
  test("CSV Multi Row Writes - No Headers") {
    checkMulti(FlatFileWriterOptions(headers = None, trailingNewline = false)){ writer =>
      writer.writeRow(Seq("foo"))
      writer.writeRow(Seq("foo","bar"))
      writer.writeRow(Seq("foo","bar","baz"))
      writer.writeRow(Seq("Hello, World","foo "," bar"))
    }("""
foo
foo,bar
foo,bar,baz
"Hello, World",foo , bar
""".trim)

  }
  
  test("CSV Multi Row Writes - No Headers - Trailing Newlines") {
    checkMulti(FlatFileWriterOptions(headers = None, trailingNewline = true)){ writer =>
      writer.writeRow(Seq("foo"))
      writer.writeRow(Seq("foo","bar"))
      writer.writeRow(Seq("foo","bar","baz"))
      writer.writeRow(Seq("Hello, World","foo "," bar"))
    }("""
foo
foo,bar
foo,bar,baz
"Hello, World",foo , bar
""".trim+"\n")

  }
  
  test("CSV Multi Row Writes - Headers") {
    checkMulti(FlatFileWriterOptions(headers = Some(Vector("one","two","three")), trailingNewline = false)){ writer =>
      writer.write("one" -> "foo")
      writer.write("one" -> "foo", "two" -> "bar")
      writer.write("one" -> "foo", "two" -> "bar", "three" -> "baz")
      writer.write("one" -> "Hello, World", "two" -> "foo ", "three" -> " bar")
    }("""
one,two,three
foo,,
foo,bar,
foo,bar,baz
"Hello, World",foo , bar
""".trim)
  }
  
  test("TSV Multi Row Writes - Headers") {
    val tab = "\t"
    
    checkMulti(FlatFileWriterOptions(sep = "\t", headers = Some(Vector("one","two","three")), trailingNewline = false)){ writer =>
      writer.writeRow(Seq("foo"))
      writer.writeRow(Seq("foo","bar"))
      writer.writeRow(Seq("foo","bar","baz"))
      writer.writeRow(Seq("Hello, World","foo "," bar"))
    }(s"""
one${tab}two${tab}three
foo
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
      ("foo3","bar3","baz3")
    )
    
    rows.foreach { case (one, two, three) =>
      ffw.write("one" -> one, "two" -> two, "three" -> three)
    }
    
    sw.toString() should equal("""
one,two,three
foo1,bar1,baz1
foo2,bar2,baz2
foo3,bar3,baz3
""".trim)
  }
  
}