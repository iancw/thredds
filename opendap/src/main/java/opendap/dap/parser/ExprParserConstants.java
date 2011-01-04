/* Generated By:JavaCC: Do not edit this line. ExprParserConstants.java */
package opendap.dap.parser;

public interface ExprParserConstants {

  int EOF = 0;
  int EQUAL = 5;
  int NOT_EQUAL = 6;
  int GREATER = 7;
  int GREATER_EQL = 8;
  int LESS = 9;
  int LESS_EQL = 10;
  int REGEXP = 11;
  int LBRACKET = 12;
  int RBRACKET = 13;
  int COLON = 14;
  int COMMA = 15;
  int AMPERSAND = 16;
  int LPAREN = 17;
  int RPAREN = 18;
  int LBRACE = 19;
  int RBRACE = 20;
  int SEPARATOR = 21;
  int WORD = 22;
  int STRINGCONST = 23;
  int INTCONST = 24;
  int FLOATCONST = 25;
  int VAR = 26;
  int FUNCTION = 27;

  int DEFAULT = 0;

  // Restricted print set
  String[] operatorImage = {
	    null, null, null, null, null,
	    "=", "!=", ">", ">=", "<", "<=", "=~",
	    null, null, null, null, null,
	    null, null, null, null, null,
	    null, null, null, null, null};

  String[] tokenImage = {
    "<EOF>",
    "\" \"",
    "\"\\t\"",
    "\"\\n\"",
    "\"\\r\"",
    "\"=\"",
    "\"!=\"",
    "\">\"",
    "\">=\"",
    "\"<\"",
    "\"<=\"",
    "\"=~\"",
    "\"[\"",
    "\"]\"",
    "\":\"",
    "\",\"",
    "\"&\"",
    "\"(\"",
    "\")\"",
    "\"{\"",
    "\"}\"",
    "\".\"",
    "<WORD>",
    "<STRING>",
    "<NUMBER>",
    "<VAR>",
    "<FUNCTION>"
  };

}
