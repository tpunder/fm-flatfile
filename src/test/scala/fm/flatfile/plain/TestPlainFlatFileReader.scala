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
package fm.flatfile.plain

import org.scalatest.FunSuite
import org.scalatest.Matchers
import java.io.StringReader
import fm.common.MultiUseResource
import fm.flatfile.{FlatFileReader, FlatFileReaderException, FlatFileReaderOptions}

final class TestPlainFlatFileReader extends FunSuite with Matchers {
  val csvParser = new StandardFlatFileRowParser(sep=",", quote="'")
  val csvCommentParser = new StandardFlatFileRowParser(sep=",", quote="'", comment="#")
  val tsvParser = new StandardFlatFileRowParser(sep="\t", quote="'")
  val tsvParserReparseBadQuotes = new StandardFlatFileRowParser(sep="\t", quote="'", strictQuoteEscaping=true)
  val tsvWithoutQuoteParser = new StandardFlatFileRowParser(sep="\t", quote="")
  val weirdParser = new StandardFlatFileRowParser(sep="|-|", quote="==")

  test("parsePlainColumnValue") {
    checkPlainParse(csvParser, "", "")
    checkPlainParse(csvParser, ",", "")
    checkPlainParse(csvParser, ",,", "")
    checkPlainParse(csvParser, " ", " ")
    checkPlainParse(csvParser, " ,", " ")
    checkPlainParse(csvParser, "asd", "asd")
    checkPlainParse(csvParser, "asd,qwe", "asd")
    checkPlainParse(csvParser, "asd,qwe", "qwe", 4)

    checkPlainParse(tsvParser, "", "")
    checkPlainParse(tsvParser, "\t", "")
    checkPlainParse(tsvParser, "\t\t", "")
    checkPlainParse(tsvParser, " ", " ")
    checkPlainParse(tsvParser, " \t", " ")
    checkPlainParse(tsvParser, "asd", "asd")
    checkPlainParse(tsvParser, "asd\tqwe", "asd")
    checkPlainParse(tsvParser, "asd\tqwe", "qwe", 4)

    checkPlainParse(weirdParser, "", "")
    checkPlainParse(weirdParser, "|-|", "")
    checkPlainParse(weirdParser, "|-||-|", "")
    checkPlainParse(weirdParser, " ", " ")
    checkPlainParse(weirdParser, " |-|", " ")
    checkPlainParse(weirdParser, "asd", "asd")
    checkPlainParse(weirdParser, "asd|-|qwe", "asd")
    checkPlainParse(weirdParser, "asd|-|qwe", "qwe", 6)
  }

  test("parseQuotedColumnValue") {
    checkQuotedParse(csvParser, "''", "")
    checkQuotedParse(csvParser, "'',", "")
    checkQuotedParse(csvParser, "'''',", "'")
    checkQuotedParse(csvParser, "'asd'", "asd")
    checkQuotedParse(csvParser, "'asd',asd", "asd")
    checkQuotedParse(csvParser, "'a''sd',asd", "a'sd")
    checkQuotedParse(csvParser, "'a'sd',asd", "a'sd")
    
    checkQuotedParse(csvParser, "'a\\'sd',asd", "a'sd") // Hacked support for \" style escaping

    checkQuotedParse(tsvParser, "''", "")
    checkQuotedParse(tsvParser, "''\t", "")
    checkQuotedParse(tsvParser, "''''\t", "'")
    checkQuotedParse(tsvParser, "'asd'", "asd")
    checkQuotedParse(tsvParser, "'asd'\tasd", "asd")
    checkQuotedParse(tsvParser, "'a''sd'\tasd", "a'sd")
    checkQuotedParse(tsvParser, "'a'sd'\tasd", "a'sd")
    
    checkQuotedParse(tsvParser, "'a\\'sd'\tasd", "a'sd") // Hacked support for \" style escaping

    checkQuotedParse(weirdParser, "====", "")
    checkQuotedParse(weirdParser, "====|-|", "")
    checkQuotedParse(weirdParser, "========|-|", "==")
    checkQuotedParse(weirdParser, "==asd==", "asd")
    checkQuotedParse(weirdParser, "==asd==|-|asd", "asd")
    checkQuotedParse(weirdParser, "==a====sd==|-|asd", "a==sd")
    checkQuotedParse(weirdParser, "==a==sd==|-|asd", "a==sd")
    
    checkQuotedParse(weirdParser, "==a\\==sd==|-|asd", "a==sd") // Hacked support for \" style escaping
  }

  test("parseRow") {
    checkParseRow(csvParser, "", Vector.empty)
    checkParseRow(csvParser, "one", Vector("one"))
    checkParseRow(csvParser, "one,two,three", Vector("one","two","three"))
    checkParseRow(csvParser, " one , two , three ", Vector(" one "," two "," three "))
    checkParseRow(csvParser, " 'one' , 'two' , 'three' ", Vector("one","two","three"))
    checkParseRow(csvParser, "a,b,c", Vector("a","b","c"))
    checkParseRow(csvParser, "'one,two',three", Vector("one,two","three"))
    checkParseRow(csvParser, "'on'e,t'wo',three", Vector("on'e,t'wo","three"))
    checkParseRow(csvParser, "'one'',''two',three", Vector("one','two","three"))

    // Blank Column CSV
    checkParseRow(csvParser, "one,two,", Vector("one","two",""))
    checkParseRow(csvParser, "one,two,,", Vector("one","two","",""))
    checkParseRow(csvParser, "one,,three", Vector("one","","three"))
    checkParseRow(csvParser, ",two,three", Vector("","two","three"))

    // Blank Column CSV Quoted
    checkParseRow(csvParser, "'one','two',''", Vector("one","two",""))
    checkParseRow(csvParser, "'one','two',", Vector("one","two",""))
    checkParseRow(csvParser, "'one','two',,", Vector("one","two","",""))
    checkParseRow(csvParser, "'one','','three'", Vector("one","","three"))
    checkParseRow(csvParser, "'','two','three'", Vector("","two","three"))

    // Blank Column TSV
    checkParseRow(tsvParser, "one\ttwo\t", Vector("one","two",""))
    checkParseRow(tsvParser, "one\ttwo\t\t", Vector("one","two","",""))
    checkParseRow(tsvParser, "one\t\tthree", Vector("one","","three"))
    checkParseRow(tsvParser, "\ttwo\tthree", Vector("","two","three"))

    // Blank Column TSV Quoted
    checkParseRow(tsvParser, "'one'\t'two'\t''", Vector("one","two",""))
    checkParseRow(tsvParser, "'one'\t'two'\t", Vector("one","two",""))
    checkParseRow(tsvParser, "'one'\t'two'\t\t", Vector("one","two","",""))
    checkParseRow(tsvParser, "'one'\t''\t'three'", Vector("one","","three"))
    checkParseRow(tsvParser, "''\t'two'\t'three'", Vector("","two","three"))

    // Blank Column Weird
    checkParseRow(weirdParser, "one|-|two|-|", Vector("one","two",""))
    checkParseRow(weirdParser, "one|-|two|-||-|", Vector("one","two","",""))
    checkParseRow(weirdParser, "one|-||-|three", Vector("one","","three"))
    checkParseRow(weirdParser, "|-|two|-|three", Vector("","two","three"))

    // Blank Column Weird Quoted
    checkParseRow(weirdParser, "==one==|-|==two==|-|====", Vector("one","two",""))
    checkParseRow(weirdParser, "==one==|-|==two==|-|", Vector("one","two",""))
    checkParseRow(weirdParser, "==one==|-|==two==|-||-|", Vector("one","two","",""))
    checkParseRow(weirdParser, "==one==|-|====|-|==three==", Vector("one","","three"))
    checkParseRow(weirdParser, "====|-|==two==|-|==three==", Vector("","two","three"))

    // Multiline testing
    checkParseRow(csvParser, "one,two,three,'four\nfive'", Vector("one","two","three","four\nfive"))
    checkParseRow(csvParser, "one,two,three,'four\nfive\nsix'", Vector("one","two","three","four\nfive\nsix"))
    checkParseRow(csvParser, "one,two,three,'four,\n,five \n six',seven", Vector("one","two","three","four,\n,five \n six","seven"))
    checkParseRow(csvParser, "one,two,three,'four,\n\n\n,five \n\n six',seven", Vector("one","two","three","four,\n\n\n,five \n\n six","seven"))

    // Trailing Sep in MultiLine
    checkParseRow(csvParser, "one,two,three,'four\nfive',", Vector("one","two","three","four\nfive",""))
    checkParseRow(csvParser, "one,two,three,'four\nfive',,", Vector("one","two","three","four\nfive","",""))
    checkParseRow(csvParser, "one,two,three,'four\nfive',''", Vector("one","two","three","four\nfive",""))

    checkParseRow(tsvParser, "", Vector.empty)
    checkParseRow(tsvParser, "one", Vector("one"))
    checkParseRow(tsvParser, "one\ttwo\tthree", Vector("one","two","three"))
    checkParseRow(tsvParser, " one \t two \t three ", Vector(" one "," two "," three "))
    checkParseRow(tsvParser, " 'one' \t 'two' \t 'three' ", Vector("one","two","three"))
    checkParseRow(tsvParser, "a\tb\tc", Vector("a","b","c"))
    checkParseRow(tsvParser, "'one\ttwo'\tthree", Vector("one\ttwo","three"))
    checkParseRow(tsvParser, "'on'e\tt'wo'\tthree", Vector("on'e\tt'wo","three"))
    checkParseRow(tsvParser, "'one''\t''two'\tthree", Vector("one'\t'two","three"))

    // extra tab checking to make sure we aren't skipping over tabs as whitespace
    checkParseRow(tsvParser, " one \t two \t three \t\t five", Vector(" one "," two "," three ", "", " five"))
    checkParseRow(tsvParser, " one \t two \t three \t \t five", Vector(" one "," two "," three ", " ", " five"))

    // Multiline testing
    checkParseRow(tsvParser, "one\ttwo\tthree\t'four\nfive'", Vector("one","two","three","four\nfive"))
    checkParseRow(tsvParser, "one\ttwo\tthree\t'four\nfive\nsix'", Vector("one","two","three","four\nfive\nsix"))
    checkParseRow(tsvParser, "one\ttwo\tthree\t'four\t\n\tfive \n six'\tseven", Vector("one","two","three","four\t\n\tfive \n six","seven"))
    checkParseRow(tsvParser, "one\ttwo\tthree\t'four\t\n\n\n\tfive \n\n six'\tseven", Vector("one","two","three","four\t\n\n\n\tfive \n\n six","seven"))

    checkParseRow(weirdParser, "", Vector.empty)
    checkParseRow(weirdParser, "one", Vector("one"))
    checkParseRow(weirdParser, "one|-|two|-|three", Vector("one","two","three"))
    checkParseRow(weirdParser, " one |-| two |-| three ", Vector(" one "," two "," three "))
    checkParseRow(weirdParser, " ==one== |-| ==two== |-| ==three== ", Vector("one","two","three"))
    checkParseRow(weirdParser, "a|-|b|-|c", Vector("a","b","c"))
    checkParseRow(weirdParser, "==one|-|two==|-|three", Vector("one|-|two","three"))
    checkParseRow(weirdParser, "==on==e|-|t==wo==|-|three", Vector("on==e|-|t==wo","three"))
    checkParseRow(weirdParser, "==one====|-|====two==|-|three", Vector("one==|-|==two","three"))

    // Multiline testing
    checkParseRow(weirdParser, "one|-|two|-|three|-|==four\nfive==", Vector("one","two","three","four\nfive"))
    checkParseRow(weirdParser, "one|-|two|-|three|-|==four\nfive\nsix==", Vector("one","two","three","four\nfive\nsix"))
    checkParseRow(weirdParser, "one|-|two|-|three|-|==four|-|\n|-|five\nsix==|-|seven", Vector("one","two","three","four|-|\n|-|five\nsix","seven"))
    checkParseRow(weirdParser, "one|-|two|-|three|-|==four|-|\n\n\n|-|five\n\nsix==|-|seven", Vector("one","two","three","four|-|\n\n\n|-|five\n\nsix","seven"))

    // TSV Parsing without quotes
    checkParseRow(tsvWithoutQuoteParser, "", Vector.empty)
    checkParseRow(tsvWithoutQuoteParser, "one", Vector("one"))
    checkParseRow(tsvWithoutQuoteParser, "one\ttwo\tthree", Vector("one","two","three"))
    checkParseRow(tsvWithoutQuoteParser, " one \t two \t three ", Vector(" one "," two "," three "))
    checkParseRow(tsvWithoutQuoteParser, " 'one' \t 'two' \t 'three' ", Vector(" 'one' "," 'two' "," 'three' "))
    checkParseRow(tsvWithoutQuoteParser, " \"one\" \t \"two\" \t \"three\" ", Vector(" \"one\" "," \"two\" "," \"three\" "))
    checkParseRow(tsvWithoutQuoteParser, "a\tb\tc", Vector("a","b","c"))
    checkParseRow(tsvWithoutQuoteParser, "'one\ttwo'\tthree", Vector("'one","two'","three"))
    checkParseRow(tsvWithoutQuoteParser, "'on'e\tt'wo'\tthree", Vector("'on'e","t'wo'","three"))
    checkParseRow(tsvWithoutQuoteParser, "'one''\t''two'\tthree", Vector("'one''","''two'","three"))
  }
  
  test("reparse bad quotes") {
    // This doesn't work since the field starts with a quote character.  Not sure if there is a good
    // way to automatically handle this or not.  If we find another unescaped quote in the field
    // then don't treat it as a quoted value?
    checkParseRow(tsvParserReparseBadQuotes, "'one' foo bar 'two' foo bar", Vector("'one' foo bar 'two' foo bar"))
    
    // This will use unescaped quotes in a quoted field
    checkParseRow(tsvParser, "one' foo bar 'two' foo bar", Vector("one' foo bar 'two' foo bar"))
  }

//  test("parseRow control characters") {
//    checkParseRow(csvParser, "one,two,three\r", Vector("one","two","three"))
//    checkParseRow(csvParser, "\u0000one,t\u0000wo,th\u0000ree\r", Vector("one","two","three"))
//    checkParseRow(csvParser, "\u0000one,two,three\r\nfoo", Vector("one","two","three"))
//  }

  test("autoDetectSepAndQuote") {
    testAutoDetect(",", "\"", "foo,bar,baz")
    testAutoDetect(",", "\"", "foo ,bar ,baz")
    testAutoDetect(",", "\"", "foo, bar, baz")
    testAutoDetect(",", "\"", "foo , bar , baz")
    testAutoDetect(",", "|", "|foo|,|bar|,|baz|")
    testAutoDetect("\t", "\"", "foo\tbar\tbaz")
    testAutoDetect(",", "\"", "SKU,Price,Images,Description,Category,Weight,Manufacturer,MPN,UPC,Vehicle Compatibility")
    testAutoDetect("|-|", "\"", "brand_name|-|sku|-|sku_merchant|-|price1|-|price2|-|price3|-|shipping1|-|shipping2|-|shipping3|-|handling1|-|handling2|-|handling3")
    testAutoDetect(",", "|", "|.57|,|RAYBESTOS|,|H1089|,|RAY|")
    
    testAutoDetect("\t","'", "'ANC'\t'ANCO WIPER PRODUCTS'\t'N-13R'\t6.49\t0.00\t'Wiper Blade Refill'")
    
    // Some fields quoted
    //testAutoDetect("|", "'", "123|'foo'|'bar'")
    //testAutoDetect("|", "'", "123|'foo'|bar")
    
    // 123AutoParts feed with some fields quoted
    testAutoDetect("|", "\"", """1|"Cardone / A-1 Cardone, Master Cylinder Part # 10-1236"|33219|123AutoParts.com|http://www.shareasale.com/r.cfm?b=313808&u=YOURUSERID&m=33219&urllink=www.123autoparts.com/searchitem.epc%3Flookfor%3D101236|http://images.wrenchead.com/smartpages/partinfo_resize/A1C/101236-rit.jpg|http://images.wrenchead.com/smartpages/partinfo_resize/A1C/101236-rit.jpg|49.04|153.25|Auto/Boat/Plane|Parts|"CADILLAC Brake/Wheel Bearing  Master Cyl, Booster, Switch  w/Bendix Brake Booster; OE Bendix Boosters Are Usually Painted Black;Reman Process Removes OE Finish; Years:1965-1966; Per Car Qty:1; Note: This may also fit many different years and makes including Chevy, Ford, Dodge, Honda, Nissan, BMW, Acura, Mercedes, Lexus, Audi, and more. Click to view detailed listing."||||||2011-08-29 20:24:54|instock|Cardone / A-1 Cardone|10-1236|||Master Cylinder||082617003742|AA1101236|||||||1|||||||||||||||||""")
  }

  test("autoDetectSepAndQuote 2 column") {
    testAutoDetect(",","\"", "foo,bar")
    testAutoDetect(",","\"", "foo ,bar")
    testAutoDetect(",","\"", "foo, bar")
    testAutoDetect(",","\"", "foo , bar ")
    testAutoDetect(",","|", "|foo|,|bar|")
    testAutoDetect("\t","\"", "foo\tbar")
  }

  test("Comments") {
    csvCommentParser.parseRow("#foo|bar|baz") should equal(Vector())
    csvCommentParser.parseRow("#one+two+three") should equal(Vector())
    csvCommentParser.parseRow("foo,bar,baz") should equal(Vector("foo","bar","baz"))
    csvCommentParser.parseRow("#asdasdsad") should equal(Vector())
    csvCommentParser.parseRow("one,two,three") should equal(Vector("one","two","three"))

    testAutoDetect(",","\"", "foo,bar,baz", "#")
  }

    val csvExample = """
name, price ,qty
foo ,123,1
bar,789
""".trim

  test("FlatFileReaderRowParser - DefaultOptions") {
    val reader = makeFlatFileReader(csvExample, FlatFileReaderOptions())
    checkCSVExample(reader)
  }
    
  test("FlatFileReaderRowParser - Header match check - Good") {
    val reader = makeFlatFileReader(csvExample, FlatFileReaderOptions(headers = Some(Vector("name","price","qty"))))
    checkCSVExample(reader)
  }
  
  test("FlatFileReaderRowParser - Header match check - Fail - Spelling") {
    val reader = makeFlatFileReader(csvExample, FlatFileReaderOptions(headers = Some(Vector("name","price","quantity"))))
    an [FlatFileReaderException.MissingHeaders] should be thrownBy { checkCSVExample(reader) }
  }
  
  test("FlatFileReaderRowParser - Header match check - Fail - Order") {
    val reader = makeFlatFileReader(csvExample, FlatFileReaderOptions(headers = Some(Vector("qty","name","price"))))
    an [FlatFileReaderException.MissingHeaders] should be thrownBy { checkCSVExample(reader) }
  }
  
  test("csvExample - LineNumbers") {
    val reader = makeFlatFileReader(csvExample, FlatFileReaderOptions())
    reader.toVector.map{ _.lineNumber } should equal (Vector(2,3))
  }
  
  private def checkCSVExample(reader: FlatFileReader): Unit = {
    reader.head.headers should equal(Vector("name","price","qty"))
    
    reader.toVector.map{ _.values } should equal (Vector(
      Vector("foo","123","1"),
      Vector("bar","789","")
    ))
  }

  val headerDetectionCSVExample = """
foo, bar, asd
1, 2, 3
name, price ,qty
foo ,123,1
bar,789
""".trim

  test("Header Detection - ALL") {
    val reader = makeFlatFileReader(headerDetectionCSVExample, FlatFileReaderOptions(headerDetection = FlatFileReaderOptions.AutoDetectAllHeaders(Seq(" NAME ", "pRiCe"))))
    checkCSVExample(reader)
    reader.toVector.map{ _.lineNumber } should equal (Vector(4,5))
  }
  
  test("Header Detection - ANY") {
    val reader = makeFlatFileReader(headerDetectionCSVExample, FlatFileReaderOptions(headerDetection = FlatFileReaderOptions.AutoDetectAnyHeaders(Seq(" NaMe ", "doesnotexist", "anotherdoesnotexist"))))
    checkCSVExample(reader)
    reader.toVector.map{ _.lineNumber } should equal (Vector(4,5))
  }
  
  test("Header Detection - CUSTOM") {
    val reader = makeFlatFileReader(headerDetectionCSVExample, FlatFileReaderOptions(headerDetection = FlatFileReaderOptions.CustomHeaderDetection{ _.contains("name") }))
    checkCSVExample(reader)
    reader.toVector.map{ _.lineNumber } should equal (Vector(4,5))
  }
  
  test("Header Detection - ALL - FAIL") {
    val reader = makeFlatFileReader(headerDetectionCSVExample, FlatFileReaderOptions(headerDetection = FlatFileReaderOptions.AutoDetectAllHeaders(Seq("name", "price", "qty", "asd"))))
    an [FlatFileReaderException.MissingHeaders] should be thrownBy { checkCSVExample(reader) }
  }
  
  test("Header Detection - ANY - FAIL") {
    val reader = makeFlatFileReader(headerDetectionCSVExample, FlatFileReaderOptions(headerDetection = FlatFileReaderOptions.AutoDetectAnyHeaders(Seq("blahblah", "doesnotexist", "anotherdoesnotexist"))))
    an [FlatFileReaderException.MissingHeaders] should be thrownBy { checkCSVExample(reader) }
  }
  
  test("Header Detection - CUSTOM - FAIL") {
    val reader = makeFlatFileReader(headerDetectionCSVExample, FlatFileReaderOptions(headerDetection = FlatFileReaderOptions.CustomHeaderDetection{ _.contains("blahblah") }))
    an [FlatFileReaderException.MissingHeaders] should be thrownBy { checkCSVExample(reader) }
  }
  
  val headerTransformCSVExample = """
foo, bar, asd
1, 2, 3
name_qqq, price_qqq ,qty_qqq
foo ,123,1
bar,789
""".trim

  private def headerTransform(row: IndexedSeq[String]): IndexedSeq[String] = row.map{ _.stripSuffix("_qqq") } 

  test("Header Detection with Transform - ALL") {
    val reader = makeFlatFileReader(headerTransformCSVExample, FlatFileReaderOptions(headerTransform = headerTransform, headerDetection = FlatFileReaderOptions.AutoDetectAllHeaders(Seq(" NAME ", "pRiCe"))))
    checkCSVExample(reader)
    reader.toVector.map{ _.lineNumber } should equal (Vector(4,5))
  }
  
  test("Header Detection with Transform - ANY") {
    val reader = makeFlatFileReader(headerTransformCSVExample, FlatFileReaderOptions(headerTransform = headerTransform, headerDetection = FlatFileReaderOptions.AutoDetectAnyHeaders(Seq(" NaMe ", "doesnotexist", "anotherdoesnotexist"))))
    checkCSVExample(reader)
    reader.toVector.map{ _.lineNumber } should equal (Vector(4,5))
  }
  
  test("Header Detection with Transform - CUSTOM") {
    val reader = makeFlatFileReader(headerTransformCSVExample, FlatFileReaderOptions(headerTransform = headerTransform, headerDetection = FlatFileReaderOptions.CustomHeaderDetection{ _.contains("name") }))
    checkCSVExample(reader)
    reader.toVector.map{ _.lineNumber } should equal (Vector(4,5))
  }
  
    val csvExampleWithDuplicateHeaders = """
name, qty ,qty, NAME
foo ,123,1,asd
bar,789,3, ewq
""".trim

  test("Duplicate Headers - Not Reading Field - No Exception") {
    val reader = makeFlatFileReader(csvExampleWithDuplicateHeaders, FlatFileReaderOptions.default)
    reader.toIndexedSeq
  }
   
  test("Duplicate Headers - Reading Field - qty") {
    val reader = makeFlatFileReader(csvExampleWithDuplicateHeaders, FlatFileReaderOptions.default)
    
    an [FlatFileReaderException.DuplicateHeaders] should be thrownBy { reader.toIndexedSeq.map{ _("qty") } }
    an [FlatFileReaderException.DuplicateHeaders] should be thrownBy { reader.toIndexedSeq.map{ _("QTY") } }
  }
    
  test("Duplicate Headers - Reading Field - NaMe") {
    val reader = makeFlatFileReader(csvExampleWithDuplicateHeaders, FlatFileReaderOptions.default)
    an [FlatFileReaderException.DuplicateHeaders] should be thrownBy { reader.toIndexedSeq.map{ _("NaMe") } }
  }
  
  test("Duplicate Headers - Reading Field - name") {
    val reader = makeFlatFileReader(csvExampleWithDuplicateHeaders, FlatFileReaderOptions.default)
    reader.toIndexedSeq.map{ _("name") } should equal (Vector("foo", "bar"))
  }
  
  test("Duplicate Headers - Reading Field - NAME") {
    val reader = makeFlatFileReader(csvExampleWithDuplicateHeaders, FlatFileReaderOptions.default)
    reader.toIndexedSeq.map{ _("NAME") } should equal (Vector("asd", "ewq"))
  }
  
  test("FlatFileReaderRowParser - control chars") {
    val reader = makeFlatFileReader("na\rme, pr\u0000ice ,qty\nfo\u0000o ,123\u0000,1\nbar,789", FlatFileReaderOptions())

    reader.head.headers should equal(Vector("name","price","qty"))
    
    reader.toVector.map{ _.values } should equal (Vector(
      Vector("foo","123","1"),
      Vector("bar","789","")
    ))
  }

  test("FlatFileReaderRowParser - enforceColumnCount = true") {
    val reader = makeFlatFileReader(csvExample, FlatFileReaderOptions(enforceColumnCount=true))

    reader.head.headers should equal(Vector("name","price","qty"))
    
    an [FlatFileReaderException.ColumnCountMismatch] should be thrownBy { reader.toVector.map{ _.values } }
    
    reader.withTries.map{ _.isSuccess }.toVector should equal (Vector(true, false))
    
    an [FlatFileReaderException.ColumnCountMismatch] should be thrownBy { reader.withTries.toVector(1).get }
    
//    reader.next.values should equal(Vector("foo","123","1"))
//    evaluating { reader.next.values } should produce [AssertionError]
//    reader.next should equal(null)
  }

  test("FlatFileReaderRowParser - allowLessColumns, trimValues = false") {
    val reader = makeFlatFileReader(csvExample, FlatFileReaderOptions(allowLessColumns=true,trimValues=false))

    reader.head.headers should equal(Vector("name","price","qty"))
    
    reader.toVector.map{ _.values } should equal (Vector(
      Vector("foo ","123","1"),
      Vector("bar","789","")
    ))
  }

  test("FlatFileReaderRowParser - allowLessColumns, noPad") {
    val reader = makeFlatFileReader(csvExample, FlatFileReaderOptions(allowLessColumns=true,addMissingValues=false))

    reader.head.headers should equal(Vector("name","price","qty"))
    
    reader.toVector.map{ _.values } should equal (Vector(
      Vector("foo","123","1"),
      Vector("bar","789")
    ))
  }
  
  test("FlatFileReaderRowParser - Tab Sep") {
    // Some of the code was using .isBlank/.isNotBlank and since a tab counts as blank it was
    // messing up some of the auto-detection code and not using the specified sep
    val reader = makeFlatFileReader("1,2,3", FlatFileReaderOptions(hasHeaders=false, sep="\t", quote=None))

    reader.toVector.map{ _.values } should equal (Vector(
      Vector("1,2,3")
    ))
  }

  val skipLinesEx = """
skip
me
name,price,qty
foo,123,1
bar,789,2
skip
also
""".trim

  test("Skip Lines leading/trailing") {
    val reader = makeFlatFileReader(skipLinesEx, FlatFileReaderOptions(skipLines=2, skipTrailingLines=2))

    reader.head.headers should equal(Vector("name","price","qty"))
    
    reader.toVector.map{ _.values } should equal (Vector(
      Vector("foo","123","1"),
      Vector("bar","789","2")
    ))
    
    reader.toVector.map{ _.lineNumber } should equal (Vector(4,5))
  }
  
  test("Skip Lines leading/trailing newline at end") {
    val reader = makeFlatFileReader(skipLinesEx+"\n", FlatFileReaderOptions(skipLines=2, skipTrailingLines=2))

    reader.head.headers should equal(Vector("name","price","qty"))
    
    reader.toVector.map{ _.values } should equal (Vector(
      Vector("foo","123","1"),
      Vector("bar","789","2")
    ))
    
    reader.toVector.map{ _.lineNumber } should equal (Vector(4,5))
  }

  val emptyLinesEx = """name,price,qty

foo,123,1

bar,789,2

 

"""

  test("Trailing Empty Lines test") {
    val reader = makeFlatFileReader(emptyLinesEx, FlatFileReaderOptions())

    reader.head.headers should equal(Vector("name","price","qty"))
    
    reader.toVector.map{ _.values } should equal (Vector(
      Vector("foo","123","1"),
      Vector("bar","789","2")
    ))
    
    reader.toVector.map{ _.lineNumber } should equal (Vector(3,5))
  }


val csvHeaderAndRowParsing = """
name, price| ,q@t#y
foo ,123,1
""".trim

  test("StandardFlatFileRowReader") {
    val reader = FlatFileReader(new StringReader(csvHeaderAndRowParsing))
    val row = reader.head

    row.hasCol("name") should equal(true)
    row.hasCol("NAME") should equal(true)
    row.hasCol("^n!a@m#e$") should equal(true)
    row.hasCol("price") should equal(true)
    row.hasCol("price|") should equal(true)
    row.hasCol("|P|R|I|C|E|") should equal(true)
    row.hasCol("qty") should equal(true)
    row.hasCol("QTY|") should equal(true)

    row("name") should equal("foo")
    row("NAME") should equal("foo")
    row("^n!a@m#e$") should equal("foo")
    row("price") should equal("123")
    row("price|") should equal("123")
    row("|P|R|I|C|E|") should equal("123")
    row("qty") should equal("1")
    row("QTY|") should equal("1")
  }

  val multiLineEx = """
name,price,qty
foo,123,"1 
 bar,789,2"
asd,qwe,zxc
""".trim

  test("Multiline Parsing") {
    val reader = makeFlatFileReader(multiLineEx, FlatFileReaderOptions())

    reader.head.headers should equal(Vector("name","price","qty"))
    
    reader.toVector.map{ _.values } should equal (Vector(
      Vector("foo","123","1 \n bar,789,2"),
      Vector("asd","qwe","zxc")
    ))
    
    reader.toVector.map{ _.lineNumber } should equal (Vector(2,4))
  }

  def checkPlainParse(parser: StandardFlatFileRowParser, restOfLine: String, expected: String, idx: Int = 0) {
    val buffer = new StringBuilder
    buffer ++= restOfLine

    val result = new StringBuilder
    parser.parsePlainColumnValue(buffer, idx, result)

    result.toString should equal(expected)
  }

  def checkQuotedParse(parser: StandardFlatFileRowParser, restOfLine: String, expected: String, idx: Int = 0) {
    val buffer = new StringBuilder
    buffer ++= restOfLine

    val result = new StringBuilder
    parser.parseQuotedColumnValue(buffer, idx, result)

    result.toString should equal(expected)
  }

  def checkParseRow(parser: StandardFlatFileRowParser, line: String, expected: IndexedSeq[String]) {
    parser.parseRow(line) should equal(expected)
  }

  def testAutoDetect(sep: String, quote: String, line: String, comment: String = null) {
    val (detectedSep,detectedQuote) = StandardFlatFileRowParser.autoDetectSepAndQuote(line, comment)
    
    detectedSep should equal (sep)
    detectedQuote.getOrElse(StandardFlatFileRowParser.DefaultQuote) should equal (quote)
  }

  def makeFlatFileReader(str: String, options: FlatFileReaderOptions): FlatFileReader = {
    PlainFlatFileReader(MultiUseResource(new StringReader(str)), options)
  }
}
