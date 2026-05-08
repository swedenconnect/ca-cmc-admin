/*
 * Copyright 2024-2026.  Agency for Digital Government (DIGG)
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

package se.swedenconnect.ca.cmcclient.ca.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link RequestData#validateString(String)}.
 *
 * <p>The validator is the front-line defence against HTML/JavaScript content sneaking into
 * X.509 certificate attribute values, so these tests pin down both halves of its contract:
 * legitimate values pass through unchanged, and any input that could express an HTML tag,
 * an HTML entity, a JavaScript Unicode escape, or an ASCII control character is rejected
 * with {@link IllegalArgumentException}.
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
class RequestDataTest {

  // ---------------------------------------------------------------------------------------
  // Accepted: legitimate values that should pass through unchanged
  // ---------------------------------------------------------------------------------------

  @ParameterizedTest(name = "accepts [{0}]")
  @ValueSource(strings = {
      // phone numbers — the original failure that started all this
      "+46 8 555 12345",
      "+46812345",
      "0046 8 555 12345",
      "08-555 12345",
      // names with apostrophes, parentheses, hyphens
      "Smith O'Brien",
      "(SE) 559912-3456",
      "Post och Telestyrelsen",
      // names containing a bare '&' that does not pair with a ';' in the same word
      "AT&T Corporation",
      "R&D Group AB",
      "Procter & Gamble",
      "Smith & Jones AB",
      "Smith & Jones; foo",          // & and ; in different words — not entity-shaped
      // email-like values (the previous '@'-substitution hack is no longer needed)
      "user@example.com",
      // Swedish/Unicode letters in legitimate names
      "Försäkringskassan",
      "Östergötland län",
      "Åland",
      // backslashes that are NOT followed by 'u'
      "C:\\Documents\\bar",
      // at-the-edge length and whitespace
      "abc def",
      "abc  def",                    // double space
      "",                            // empty (validator only rejects null and over-limit)
  })
  void accepts_legitimateValues(final String value) {
    assertEquals(value, RequestData.validateString(value));
    // The two-arg overload must behave identically; the 'email' flag is now ignored.
    assertEquals(value, RequestData.validateString(value, true));
    assertEquals(value, RequestData.validateString(value, false));
  }

  @Test
  void accepts_atLengthLimit() {
    final String maxLen = "a".repeat(250);
    assertEquals(maxLen, RequestData.validateString(maxLen));
  }

  // ---------------------------------------------------------------------------------------
  // Rejected: literal HTML/XML tag delimiters
  // ---------------------------------------------------------------------------------------

  @ParameterizedTest(name = "rejects html tag fragment [{0}]")
  @ValueSource(strings = {
      "<script>alert(1)</script>",
      "<b>bold</b>",
      "Hello<world>",
      "<",
      ">",
      "abc<def",
      "abc>def",
      "no closing <tag",
      "stray closing tag/>",
  })
  void rejects_htmlTagDelimiters(final String value) {
    assertThrows(IllegalArgumentException.class, () -> RequestData.validateString(value));
  }

  // ---------------------------------------------------------------------------------------
  // Rejected: HTML entities — named, decimal-numeric, hex-numeric, mid-word, malformed
  // ---------------------------------------------------------------------------------------

  @ParameterizedTest(name = "rejects html entity [{0}]")
  @ValueSource(strings = {
      // standalone named entities
      "&amp;",
      "&lt;",
      "&gt;",
      "&quot;",
      "&LT;",                        // case variations
      "&Lt;",
      // decimal numeric entities
      "&#60;",
      "&#62;",
      "&#0000060;",                  // leading zeros
      // hex numeric entities
      "&#x3C;",
      "&#X3C;",
      "&#x003C;",
      // entities embedded in surrounding text
      "Smith &amp; Jones",
      "Smith &lt;script&gt; Jones",
      "foo &amp;",
      "&amp; foo",
      // mid-word entities — '&' and ';' in the same whitespace-delimited word
      "Smith&amp;Jones",
      "Smith&lt;Jones",
      "Smith&#x3C;Jones",
      "Foo&Bar;baz",
      "Smith&;Jones",
      // degenerate forms
      "&;",
  })
  void rejects_htmlEntities(final String value) {
    assertThrows(IllegalArgumentException.class, () -> RequestData.validateString(value));
  }

  // ---------------------------------------------------------------------------------------
  // Rejected: JavaScript / JSON Unicode escape prefix
  //
  // NOTE on quoting: each runtime test string in this group is six characters that begin
  // with a literal backslash followed by 'u'. To get that runtime string out of a Java
  // source string literal it must be written with a doubled backslash (e.g. "\\u003c").
  // A single backslash form would be processed by Java's source-level Unicode-escape
  // pre-processor and either become the actual codepoint or fail to compile.
  // ---------------------------------------------------------------------------------------

  @ParameterizedTest(name = "rejects unicode escape [{0}]")
  @ValueSource(strings = {
      "\\u003c",
      "\\U003c",
      "\\u{3C}",
      "Smith\\u003cJones",
      "prefix \\u00e5 suffix",
      "C:\\users\\foo",              // deliberately collateral — Windows path, see note below
  })
  void rejects_unicodeEscapeSequences(final String value) {
    assertThrows(IllegalArgumentException.class, () -> RequestData.validateString(value));
  }

  // ---------------------------------------------------------------------------------------
  // Rejected: ASCII control characters (C0 range and DEL)
  // ---------------------------------------------------------------------------------------

  @Test
  void rejects_tabCharacter() {
    assertThrows(IllegalArgumentException.class, () -> RequestData.validateString("x\ty"));
  }

  @Test
  void rejects_lineFeedCharacter() {
    assertThrows(IllegalArgumentException.class, () -> RequestData.validateString("x\ny"));
  }

  @Test
  void rejects_carriageReturnCharacter() {
    assertThrows(IllegalArgumentException.class, () -> RequestData.validateString("x\ry"));
  }

  @Test
  void rejects_nulCharacter() {
    assertThrows(IllegalArgumentException.class, () -> RequestData.validateString("x\u0000y"));
  }

  @Test
  void rejects_lowestC0Character() {
    assertThrows(IllegalArgumentException.class, () -> RequestData.validateString("x\u0001y"));
  }

  @Test
  void rejects_highestC0Character() {
    assertThrows(IllegalArgumentException.class, () -> RequestData.validateString("x\u001fy"));
  }

  @Test
  void rejects_delCharacter() {
    assertThrows(IllegalArgumentException.class, () -> RequestData.validateString("x\u007fy"));
  }

  // ---------------------------------------------------------------------------------------
  // Rejected: edge cases (null and over-length)
  // ---------------------------------------------------------------------------------------

  @Test
  void rejects_null() {
    assertThrows(IllegalArgumentException.class, () -> RequestData.validateString(null));
  }

  @Test
  void rejects_overLength() {
    final String tooLong = "a".repeat(251);
    assertThrows(IllegalArgumentException.class, () -> RequestData.validateString(tooLong));
  }

  // ---------------------------------------------------------------------------------------
  // Sanity: the validator returns the input unchanged (no escaping or trimming)
  // ---------------------------------------------------------------------------------------

  @Test
  void preserves_inputBytes() {
    final String input = "  Procter & Gamble  ";   // surrounding spaces preserved
    assertEquals(input, RequestData.validateString(input));
  }
}
