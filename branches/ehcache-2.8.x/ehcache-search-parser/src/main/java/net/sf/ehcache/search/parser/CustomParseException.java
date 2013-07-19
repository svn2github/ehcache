/**
 * Copyright Terracotta, Inc. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package net.sf.ehcache.search.parser;

import net.sf.echache.search.parser.ParseException;
import net.sf.echache.search.parser.Token;

/**
 * The Class CustomParseException.
 */
public class CustomParseException extends ParseException {

  public enum Message {
    SINGLE_QUOTE("Error parsing quoted string: "), BOOLEAN_CAST("Error parsing boolean literal:"), BYTE_CAST(
        "Error parsing byte literal:"), SHORT_LITERAL("Error parsing short literal:"), INT_LITERAL("Error parsing integer literal:"), 
        LONG_LITERAL("Error parsing long literal:"), DOUBLE_LITERAL("Error parsing double literal:"),
        DATE_LITERAL("Error parsing date literal:"), SQLDATE_LITERAL("Error parsing sqldate literal:"),
        HEX_LITERAL("Error parsing hex literal:"), STRING_LITERAL("Error parsing string literal:"), 
        CLASS_LITERAL("Error parsing class literal:"), ENUM_LITERAL("Error parsing enum literal:"),
        MEMBER_LITERAL("Error parsing member literal:"),;

    private String msg;

    Message(String msg) {
      this.msg = msg;
    }

    public String getMessage() {
      return msg;
    }
  }

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 5082041880401542754L;

  /**
   * Instantiates a new custom parse exception.
   * 
   * @param tokenMgr the token mgr
   * @param tok the tok
   * @param message the message
   */
  public CustomParseException(Token tok, String message) {
    super(makeMessage(tok, message));
    currentToken = tok;
  }

  /**
   * Instantiates a new custom parse exception.
   * 
   * @param tokenMgr the token mgr
   * @param tok the tok
   * @param t the t
   */
  public CustomParseException(Token tok, Throwable t) {
    this(tok, t.getMessage());
  }

  public CustomParseException(ParseException pe) {
    super(pe.getMessage());
    currentToken = pe.currentToken;
    expectedTokenSequences = pe.expectedTokenSequences;
    tokenImage = pe.tokenImage;
  }

  /**
   * Make message.
   * 
   * @param tokenMgr the token mgr
   * @param tok the tok
   * @param message the message
   * @return the string
   */
  public static String makeMessage(Token tok, String message) {
    if (tok != null) {
      int lineNum = tok.beginLine;
      int charPos = tok.beginColumn;
      return "Parse error at line " + lineNum + ", column " + charPos + ": " + message;
    } else {
      return "Parse error: " + message;
    }
  }

  public static CustomParseException factory(Token tok, Throwable t) {
    if (t instanceof ParseException) { return new CustomParseException((ParseException) t); }
    return new CustomParseException(tok, t);
  }

  public static CustomParseException factory(Token tok, Message msg) {
    return new CustomParseException(tok, msg.getMessage()+(tok==null?"":tok.image));
  }
}
