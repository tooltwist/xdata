package com.tooltwist.fastJson;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.tooltwist.fastJson.FastJsonBlockOfNodes;
import com.tooltwist.fastJson.FastJsonException;
import com.tooltwist.fastJson.FastJsonNodes;
import com.tooltwist.xdata.XD;
import com.tooltwist.xdata.XDException;
import com.tooltwist.xdata.XDNumberFormatException;
import com.tooltwist.xdata.XIterator;
import com.tooltwist.xdata.XDCallback;
import com.tooltwist.xdata.XSelector;

/**
 * Parse a JSON text object, without creating large numbers of Java objects.
 * 
 * http://www.json.org
 * http://www.ietf.org/rfc/rfc4627.txt
 * 
 * @author philipcallender
 *
 */
public class FastJson implements XSelector, Iterable<XSelector> {
	
	public static final int ROOT_NODE = -1;
	
	protected static final int TYPE_OBJECT = 1;		// {...}
	protected static final int TYPE_ARRAY = 2;		// [...]
	protected static final int TYPE_STRING = 3;		// "..."
	protected static final int TYPE_NUMBER = 4;
	protected static final int TYPE_TRUE = 5;			// true
	protected static final int TYPE_FALSE = 6;		// false
	protected static final int TYPE_NULL = 7;			// null

	// Special meaning offsets for use while parsing strings and values
	private static final int NAME_OFFSET_PARSING_ROOT = -10;
	private static final int NAME_OFFSET_PARSING_NAME = -11;
	private static final int NAME_OFFSET_PARSING_ARRAY = -12;

	// Special characters
	private static final char ESCAPE = 0x5c;	// backslash
	private static final char QUOTE = 0x22;		// single quote
	
	// Blocks of nodes
	private FastJsonBlockOfNodes firstBlock = new FastJsonBlockOfNodes(0);
	
	// Total number of nodes within these blocks. Note that each block fills, before using the next.
	private int	numNodes = 0;
	
	// Values used while parsing
	private char json[];
	private int offsetWhileParsing;
	private int lineCnt;
	private int lineOffset;
	
	private int indentLevel;
	private static final int MAX_LEVELS_OF_INDENT = 100;
	private int lastNodeAtEachIndentLevel[];
	
	// A selection path, broken into segments.
	protected class PathSegments {
		String[] name;
		int[] index;
		
		public String toString() {
			String s = "[ ";
			String sep = "";
			for (int i = 0; i < name.length; i++) {
				s += sep + name[i];
				if (index[i] >= 0)
					s += "[" + index[i] + "]";
				sep = ", ";
			}
			s += " ]";
			return s;
		}
	};

	public FastJson(String json) throws FastJsonException
	{
		init(json.toCharArray());
	}

	public FastJson(char json[]) throws FastJsonException
	{
		init(json);
	}
	
	public FastJson(File file, boolean useUnicode) throws FastJsonException
	{
		String path = file.getAbsolutePath();
		long fileSize = file.length();
		char arr[] = new char[(int)fileSize];
		FileInputStream is = null;
		InputStreamReader in = null;
		try {
			is = new FileInputStream(path);
			if (useUnicode)
			{
//				in = new InputStreamReader(is, "UTF-16");
				in = new InputStreamReader(is, "UTF-8");
			}
			else
				in = new InputStreamReader(is);
			int num = in.read(arr);
			if (num != fileSize)
				throw new FastJsonException("Did not read entire file");
		}
		catch (IOException e)
		{
			System.err.println("Cannot load JSON from file '"+path+"': " + e);
		}
		finally
		{
			try {
				if (in != null)
					in.close();
				if (is != null)
					is.close();
			} catch (IOException e) { }
		}

		init(arr);
	}

	void init(char json[]) throws FastJsonException
	{
		this.json = json;
		this.offsetWhileParsing = 0;
		this.lineCnt = 1;
		this.lineOffset = 0;
		
		indentLevel = 0;
		lastNodeAtEachIndentLevel = new int[MAX_LEVELS_OF_INDENT];
		lastNodeAtEachIndentLevel[0] = -1;
		
		try {
			skipSpaces();
			if (json[offsetWhileParsing] == '{')
				parseObject(0, NAME_OFFSET_PARSING_ROOT);
			else if (json[offsetWhileParsing] == '[')
				parseArray(0, NAME_OFFSET_PARSING_ROOT);
			else
				syntax("Expected '{' or '['");
			
			//ZZZ Perhaps we should check there's nothing left.
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			syntax("Unexpected end of JSON");
		}
	}

	
	private void parseObject(int indent, int offsetOfName) throws FastJsonException
	{
/*
System.out.println("-----------------------------------------------");
System.out.println("parseObject("+offset+")");
System.out.println("offset:"+offset+", line: "+lineCnt);
System.out.println(thisLine()+"\n"+positionMarker(offset));
System.out.println(new String(json, offset, json.length-offset));
*/
//		FastJsonBlockOfNodes block = firstBlock.getBlock(nodeNum, false);
//		int index = nodeNum % FastJsonBlockOfNodes.SIZE;
		
/*
		if (offsetOfName == NAME_OFFSET_PARSING_ROOT) {
			System.out.println("                 NEW NODE: OBJECT(root)");
		} else if (offsetOfName == NAME_OFFSET_PARSING_ARRAY) {
			System.out.println("                 NEW NODE: OBJECT(in array)");
		} else if (offsetOfName >= 0) {
			System.out.println("                 NEW NODE: OBJECT(" + offsetOfName + ")");
		} else {
			// This should not be possible
			syntax("Internal error 82604");
		}
*/

		skipSpaces();
		if (json[offsetWhileParsing] != '{')
			syntax("Expected '{'");
		offsetWhileParsing++;
		
		// Check for an empty object
		if (json[offsetWhileParsing] == '}') {
			// End of the object
			offsetWhileParsing++;
//System.out.println("Empty Object.");
			return;
		}
		
		// Loop around getting name / value pairs.
		for ( ; ; ) {
			skipSpaces();
		
			// Read the name
			if (json[offsetWhileParsing] != '\"')
				syntax("Expected '\"'");
			int offsetOfNameForThisProperty = offsetWhileParsing + 1;
			parseString(indent, NAME_OFFSET_PARSING_NAME);

			// Expect ':'
			skipSpaces();
			if (json[offsetWhileParsing] != ':')
				syntax("Expected ':'");
			offsetWhileParsing++;

			// Expect a value
			skipSpaces();
			parseValue(indent, offsetOfNameForThisProperty);
			
			// Expect either ',' or '}'
			skipSpaces();
			if (json[offsetWhileParsing] == '}') {
				offsetWhileParsing++;
//				System.out.println("End of Object.");
				return;
			}
			
			if (json[offsetWhileParsing] != ',')
				syntax("Expected ',' or '}'");
			offsetWhileParsing++;
		}
		
		
	}
	
	
	/*
	 * Parse an array
	 * 
	 * RFC 4627                          JSON                         July 2006
	 * 
	 * 2.3.  Arrays

   An array structure is represented as square brackets surrounding zero
   or more values (or elements).  Elements are separated by commas.

      array = begin-array [ value *( value-separator value ) ] end-array

	 */
	private void parseArray(int indent, int offsetOfName) throws FastJsonException
	{
/*
System.out.println("-----------------------------------------------");
System.out.println("parseArray("+offset+")");
System.out.println("offset:"+offset+", line: "+lineCnt);
System.out.println(thisLine()+"\n"+positionMarker(offset));
System.out.println(new String(json, offset, json.length-offset));
*/
//		FastJsonBlockOfNodes block = firstBlock.getBlock(nodeNum, false);
//		int index = nodeNum % FastJsonBlockOfNodes.SIZE;
		
		
		// Check we have an array
		skipSpaces();
		if (json[offsetWhileParsing] != '[') {
			syntax("Expected '['");
		}
		offsetWhileParsing++;
		
/*
		// Looks like we have an array. Register it's value.
		if (offsetOfName == NAME_OFFSET_PARSING_NAME) {
			// Just ignore this. It's not actually a value.
		} else if (offsetOfName == NAME_OFFSET_PARSING_ROOT) {
			System.out.println("                 ROOT NODE: ARRAY(" + offset + ")");
		} else if (offsetOfName == NAME_OFFSET_PARSING_ARRAY) {
			System.out.println("                 NEW NODE: ARRAY(in array, " + offset + ")");
		} else if (offsetOfName >= 0) {
			System.out.println("                 NEW NODE: ARRAY(" + offsetOfName + ", " + offset + ")");
		} else {
			// This should not be possible
			syntax("Internal error 82602");
		}
*/
		
		// Check for an empty array
		skipSpaces();
		if (json[offsetWhileParsing] == ']') {
			// End of the array
//System.out.println("End of Array");
			offsetWhileParsing++;
			return;
		}


		// Parse the contents of the array.
		for ( ; ; ) {
			parseValue(indent, NAME_OFFSET_PARSING_ARRAY);
			
			skipSpaces();
			if (json[offsetWhileParsing] == ']') {
				// End of the array
//System.out.println("End of Array");
				offsetWhileParsing++;
				return;
			}
			
			if (json[offsetWhileParsing] != ',') {
				syntax("Expected ',' or ']'");
			}
			
			offsetWhileParsing++;
		}
	}

	/**
	 * Parse a number
	 * 
	 * 2.4.  Numbers

   The representation of numbers is similar to that used in most
   programming languages.  A number contains an integer component that
   may be prefixed with an optional minus sign, which may be followed by
   a fraction part and/or an exponent part.

   Octal and hex forms are not allowed.  Leading zeros are not allowed.

   A fraction part is a decimal point followed by one or more digits.

   An exponent part begins with the letter E in upper or lowercase,
   which may be followed by a plus or minus sign.  The E and optional
   sign are followed by one or more digits.

   Numeric values that cannot be represented as sequences of digits
   (such as Infinity and NaN) are not permitted.



Crockford                    Informational                      [Page 3]
 
RFC 4627                          JSON                         July 2006


         number = [ minus ] int [ frac ] [ exp ]

         decimal-point = %x2E       ; .

         digit1-9 = %x31-39         ; 1-9

         e = %x65 / %x45            ; e E

         exp = e [ minus / plus ] 1*DIGIT

         frac = decimal-point 1*DIGIT

         int = zero / ( digit1-9 *DIGIT )

         minus = %x2D               ; -

         plus = %x2B                ; +

         zero = %x30                ; 0

	 * 
	 * @param nodeNum
	 * @param indent
	 * @throws FastJsonException
	 */
	private void parseNumber(int indent, int offsetOfName) throws FastJsonException
	{
/*
System.out.println("-----------------------------------------------");
System.out.println("parseObject("+offset+")");
System.out.println("offset:"+offset+", line: "+lineCnt);
System.out.println(thisLine()+"\n"+positionMarker(offset));
System.out.println(new String(json, offset, json.length-offset));
*/
//		FastJsonBlockOfNodes block = firstBlock.getBlock(numNodes, false);
//		int index = numNodes % FastJsonBlockOfNodes.SIZE;
//		numNodes++;
		
		// Look for a negative sign
		skipSpaces();
		int startOffset = offsetWhileParsing;
//		boolean is_negative = false;
		if (json[offsetWhileParsing] == '-') {
			offsetWhileParsing++;
//			is_negative = true;
		}
		
		// Get the integer part
//		long int_part = 0;
		char digit = json[offsetWhileParsing];
		if (digit == '0') {
			// Leading zero can only be for number zero.
			offsetWhileParsing++;
//			int_part = 0;
		} else if (digit >= '1' && digit <= '9') {
			offsetWhileParsing++;
			while (json[offsetWhileParsing] >= '0' && json[offsetWhileParsing] <= '9') {
				offsetWhileParsing++;
			}
		}
		
		// See if there is a decimal part
		if (json[offsetWhileParsing] == '.') {
			offsetWhileParsing++;
			while (json[offsetWhileParsing] >= '0' && json[offsetWhileParsing] <= '9') {
				offsetWhileParsing++;
			}
		}
		
		// See if there is an exponent part
		if (json[offsetWhileParsing] == 'e' || json[offsetWhileParsing] == 'E') {
			offsetWhileParsing++;
			if (json[offsetWhileParsing] == '+' || json[offsetWhileParsing] == '-') {
				offsetWhileParsing++;
			}
			while (json[offsetWhileParsing] >= '0' && json[offsetWhileParsing] <= '9') {
				offsetWhileParsing++;
			}
		}
		
//		String num = new String(json, startOffset, offset - startOffset);
//		System.out.println("Number is " + num);
		registerNode(TYPE_NUMBER, offsetOfName, startOffset, offsetWhileParsing);
	}

	private void registerNode(int type, int offsetOfName, int startOffset, int endOffset) {

		// Get the position for the new node.
		int nodeId = numNodes++;

		// Let the previous node at this level of indent know that
		// this is the next node at the same level of indent.
		int lastAtThisIndent = lastNodeAtEachIndentLevel[indentLevel];
		if (lastAtThisIndent >= 0) {
			FastJsonBlockOfNodes prevBlock = firstBlock.getBlock(lastAtThisIndent, false);
			int prevIndex = lastAtThisIndent % FastJsonBlockOfNodes.SIZE;
			prevBlock.nextNodeAtThisLevel[prevIndex] = nodeId;
		}
		lastNodeAtEachIndentLevel[indentLevel] = nodeId;

		// Get the block and index for the new node
		FastJsonBlockOfNodes block = firstBlock.getBlock(nodeId, true);
		int indexWithinBlock = nodeId % FastJsonBlockOfNodes.SIZE;

		// Set the details in this node
		block.type[indexWithinBlock] = type;
		block.line[indexWithinBlock] = lineCnt;
		block.indent[indexWithinBlock] = indentLevel;
		block.offsetOfName[indexWithinBlock] = offsetOfName;
		block.offsetOfValue[indexWithinBlock] = startOffset;
		block.offsetOfValueEnd[indexWithinBlock] = endOffset;
		block.nextNodeAtThisLevel[indexWithinBlock] = -1;
	}

	/**
	 * Parse a String
	 * 
	 * RFC 4627                          JSON                         July 2006


         string = quotation-mark *char quotation-mark

         char = unescaped /
                escape (
                    %x22 /          ; "    quotation mark  U+0022
                    %x5C /          ; \    reverse solidus U+005C
                    %x2F /          ; /    solidus         U+002F
                    %x62 /          ; b    backspace       U+0008
                    %x66 /          ; f    form feed       U+000C
                    %x6E /          ; n    line feed       U+000A
                    %x72 /          ; r    carriage return U+000D
                    %x74 /          ; t    tab             U+0009
                    %x75 4HEXDIG )  ; uXXXX                U+XXXX

         escape = %x5C              ; \

         quotation-mark = %x22      ; "

         unescaped = %x20-21 / %x23-5B / %x5D-10FFFF

	 */
	private void parseString(int indent, int offsetOfName) throws FastJsonException
	{
/*
System.out.println("-----------------------------------------------");
System.out.println("parseString("+offset+")");
System.out.println("offset:"+offset+", line: "+lineCnt);
System.out.println(thisLine()+"\n"+positionMarker(offset));
System.out.println(new String(json, offset, json.length-offset));
*/

		
		skipSpaces();
		if (json[offsetWhileParsing] != QUOTE)
			syntax("Expected '\"'");
		offsetWhileParsing++;

		// Skip over the string
		int startOffset = offsetWhileParsing;
//		StringBuilder string = new StringBuilder();
		for ( ; ; ) {
			char c = json[offsetWhileParsing];
			
			if (c == '\"') {
				
				// Must be the end quote
//System.out.println("String is [" + string.toString() + "]");
				
				// Looks like we have a string. Register it's value.		
				if (offsetOfName == NAME_OFFSET_PARSING_NAME) {
					// Just ignore this. It's not actually a value.
				} else if (offsetOfName == NAME_OFFSET_PARSING_ARRAY) {
//					System.out.println("                 NEW NODE: STRING(in array)");
					registerNode(TYPE_STRING, offsetOfName, startOffset, offsetWhileParsing);
				} else if (offsetOfName >= 0) {
//					System.out.println("                 NEW NODE: STRING(" + offsetOfName + ")");
					registerNode(TYPE_STRING, offsetOfName, startOffset, offsetWhileParsing);
				} else {
					// This should not be possible
					syntax("Internal error 82603");
				}

				offsetWhileParsing++;
				return;
				
			} else if (c == ESCAPE) {
				
				// An escaped character
				offsetWhileParsing++;
				c = json[offsetWhileParsing];
				switch (c) {
				case '\"':
//					string.append('\"');
					offsetWhileParsing++;
					break;
				case '\\':
//					string.append('\\');
					offsetWhileParsing++;
					break;
				case '/':
//					string.append('/');
					offsetWhileParsing++;
					break;
				case 'b':
//					string.append('\b');
					offsetWhileParsing++;
					break;
				case 'f':
//					string.append('\f');
					offsetWhileParsing++;
					break;
				case 'n':
//					string.append('\n');
					offsetWhileParsing++;
					break;
				case 'r':
//					string.append('\r');
					offsetWhileParsing++;
					break;
				case 't':
//					string.append('\t');
					offsetWhileParsing++;
					break;
				case 'u':
					// Unicode
					char c1 = json[offsetWhileParsing+1];
					char c2 = json[offsetWhileParsing+2];
					char c3 = json[offsetWhileParsing+3];
					char c4 = json[offsetWhileParsing+4];
					if (isHex(c1) && isHex(c2) && isHex(c3) && isHex(c4)) {
//						int unicodeChar = ((hex(c1) * 16 + hex(c2)) * 16 + hex(c3)) * 16 + c4;
//						string.append(unicodeChar);
					} else {
						syntax("Expected unicode \\uXXXX");
					}
					offsetWhileParsing += 4;
					break;
				}
			}
			else if (
					c >= 0x20 && c <= 0x21
					||
					c >= 0x23 && c <= 0x5B
					||
					/*ZZZZZZZZZZZZZZZ Should handle Unicode */
					c >= 0x5D /* && c <= 0x10FFFF */
			) {
				
				// An unescaped character
//				string.append(c);
				offsetWhileParsing++;
			} else {
				
				// Something invalid
				syntax("Invalid string");
			}
		}
	}
	

	/*
	 * RFC 4627                          JSON                         July 2006
	 * 2.1.  Values

   A JSON value MUST be an object, array, number, or string, or one of
   the following three literal names:

      false null true


   The literal names MUST be lowercase.  No other literal names are
   allowed.

         value = false / null / true / object / array / number / string

         false = %x66.61.6c.73.65   ; false

         null  = %x6e.75.6c.6c      ; null

         true  = %x74.72.75.65      ; true

	 */
	private void parseValue(int endTag, int offsetOfName) throws FastJsonException
	{
/*
System.out.println("-----------------------------------------------");
System.out.println("parseValue("+offset+")");
System.out.println("offset:"+offset+", line: "+lineCnt);
System.out.println(thisLine()+"\n"+positionMarker(offset));
System.out.println(new String(json, offset, json.length-offset));
*/

		skipSpaces();
		if (json[offsetWhileParsing] == '\"') {
			// Quote, so this is a string
			parseString(endTag, offsetOfName);
		} else if (json[offsetWhileParsing]=='f' && json[offsetWhileParsing+1]=='a' && json[offsetWhileParsing+2]=='l' && json[offsetWhileParsing+3]=='s' && json[offsetWhileParsing+4]=='e') {
			// "false"
//System.out.println("Value is 'false'");
			registerNode(TYPE_FALSE, offsetOfName, offsetWhileParsing, offsetWhileParsing + 5);
			offsetWhileParsing += 5;
		} else if (json[offsetWhileParsing]=='n' && json[offsetWhileParsing+1]=='u' && json[offsetWhileParsing+2]=='l' && json[offsetWhileParsing+3]=='l') {
			// "null"
//System.out.println("Value is 'null'");
			registerNode(TYPE_NULL, offsetOfName, offsetWhileParsing, offsetWhileParsing + 4);
			offsetWhileParsing += 4;
		} else if (json[offsetWhileParsing]=='t' && json[offsetWhileParsing+1]=='r' && json[offsetWhileParsing+2]=='u' && json[offsetWhileParsing+3]=='e') {
			// "true"
//System.out.println("Value is 'true'");
			registerNode(TYPE_TRUE, offsetOfName, offsetWhileParsing, offsetWhileParsing + 4);
			offsetWhileParsing += 4;
		} else if (json[offsetWhileParsing] == '{') {
			// Value is an object
			registerNode(TYPE_OBJECT, offsetOfName, offsetWhileParsing, offsetWhileParsing);
			indent();
			parseObject(-1, offsetOfName);
			unindent();
		} else if (json[offsetWhileParsing] == '[') {
			// Value is an array
			registerNode(TYPE_ARRAY, offsetOfName, offsetWhileParsing, offsetWhileParsing);
			indent();
			parseArray(-1, offsetOfName);
			unindent();
		} else if (json[offsetWhileParsing] == '-' || (json[offsetWhileParsing] >= '0' && json[offsetWhileParsing] <= '9')) {
			parseNumber(-1, offsetOfName);
		} else {
			//ZZZZZZZZZZZZZZZ handle other types
			syntax("Expected value.");
		}
//System.out.println("End of Value.");
	}

	private void indent() {
		indentLevel++;
		lastNodeAtEachIndentLevel[indentLevel] = -1;
	}

	private void unindent() {
//		lastNodeAtEachIndentLevel[indentLevel] = -1;
		indentLevel--;
	}

	private boolean isHex(char c) {
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
	}

	private int hex(char c) {
		if (c >= '0' && c <= '9') {
			return (c - '0');
		} else if (c >= 'a' && c <= 'f') {
			return 10 + (c - 'a');
		} else if (c >= 'a' && c <= 'f') {
			return 10 + (c - 'A');
		}
		return 0; // Never gets here, because we already checked using isHex()
	}

	private void skipSpaces()
	{
		for ( ; isSpace(json[offsetWhileParsing]); offsetWhileParsing++)
			if (json[offsetWhileParsing] == '\n')
			{
				lineCnt++;
				lineOffset = offsetWhileParsing + 1;
			}
	}
	
	private boolean isSpace(char c) {
		return (c==' ' || c=='\t' || c=='\n' || c=='\r');
	}
	

	void syntax(String error) throws FastJsonException
	{
		String str = "JSON parse error on line "+lineCnt+": "+error+"\n"
			+ thisLine() + "\n"
			+ positionMarker(offsetWhileParsing);
		throw new FastJsonException(str);
	}
	
	private String thisLine()
	{
		// Find the end of the line
		int pos = lineOffset; 
		for ( ; pos < json.length && json[pos] != '\n'; pos++)
			;
		return new String(json, lineOffset, pos - lineOffset);
	}
	
	private String positionMarker(int errorPos)
	{
		String spacer = "";
		for (int pos = lineOffset; pos < errorPos; pos++)
			if (json[pos] == '\t')
				spacer += "\t";
			else
				spacer += " ";
		spacer += "^";
		return spacer;
	}

	/**
	 * Read a name, which is assumed to contain no errors because the JSON is already parsed.
	 * @param offsetOfName
	 * @return
	 * @throws FastJsonException
	 */
	private String readNameString(int offset)
	{
		while (isSpace(json[offset]))
			offset++;
		if (json[offset] == QUOTE) // Assume this to be true
			offset++;

		// Skip over the string
		StringBuilder string = new StringBuilder();
		for ( ; ; ) {
			
			char c = json[offset];
			
			if (c == '\"') {
				
				// Must be the end quote
				return string.toString();
				
			} else if (c == ESCAPE) {
				
				// An escaped character
				offset++;
				c = json[offset];
				switch (c) {
				case '\"':
					string.append('\"');
					offset++;
					break;
				case '\\':
					string.append('\\');
					offset++;
					break;
				case '/':
					string.append('/');
					offset++;
					break;
				case 'b':
					string.append('\b');
					offset++;
					break;
				case 'f':
					string.append('\f');
					offset++;
					break;
				case 'n':
					string.append('\n');
					offset++;
					break;
				case 'r':
					string.append('\r');
					offset++;
					break;
				case 't':
					string.append('\t');
					offset++;
					break;
				case 'u':
					// Unicode
					char c1 = json[offset+1];
					char c2 = json[offset+2];
					char c3 = json[offset+3];
					char c4 = json[offset+4];
					if (isHex(c1) && isHex(c2) && isHex(c3) && isHex(c4)) {
						int unicodeChar = ((hex(c1) * 16 + hex(c2)) * 16 + hex(c3)) * 16 + c4;
						string.append(unicodeChar);
					} else {
						// Can't happen, because this has previously passed parsing.
					}
					offset += 4;
					break;
				}
			}
			else if (
					c >= 0x20 && c <= 0x21
					||
					c >= 0x23 && c <= 0x5B
					||
					/*ZZZZZZZZZZZZZZZ Should handle Unicode */
					c >= 0x5D /* && c <= 0x10FFFF */
			) {
				
				// An unescaped character
				string.append(c);
				offset++;
			} else {
				
				// Can't happen, because this has previously passed parsing.
			}
		}
	}


	protected String getValue(int nodeNum, boolean includeNestedNodeValues)// throws FastJSONException
	{
		StringBuffer buffer = new StringBuffer();
		getValue(buffer, nodeNum, includeNestedNodeValues);
		return buffer.toString();
	}

	private void getValue(StringBuffer buffer, int nodeNum, boolean includeNestedNodeValues)
	{
		FastJsonBlockOfNodes block = firstBlock.getBlock(nodeNum, false);
		int index = nodeNum % FastJsonBlockOfNodes.SIZE;

		int valueStart = block.offsetOfValue[index];
		int valueEnd = block.offsetOfValueEnd[index];		

		// Add the content after the last child
		deEscapeAndAddToBuffer(buffer, valueStart, valueEnd);
	}
	
	private void deEscapeAndAddToBuffer(StringBuffer value, int start, int end)
	{
		if (end <= start)
			return;
		//ZZZZ Should de-escape value
		//value.append(json, start, end-start);
		
		String str = new String(json, start, end - start);
//		String str = json.substring(start, end);
		str = org.apache.commons.lang3.StringEscapeUtils.unescapeJava(str);
		value.append(str);
		
//		for (int offset = start; offset < end; )
//		{
//			if (json[offset] == '&')
//			{
//				if (json[offset+1]=='g' && json[offset+2]=='t' && json[offset+3]==';')
//				{
//					// &gt;
//					value.append('>');
//					offset += 4;
//					continue;
//				}
//				else if (json[offset+1]=='l' && json[offset+2]=='t' && json[offset+3]==';')
//				{
//					// &lt;
//					value.append('<');
//					offset += 4;
//					continue;
//				}
//				else if (json[offset+1]=='a' && json[offset+2]=='m' && json[offset+3]=='p' && json[offset+4]==';')
//				{
//					// &amp;
//					value.append('&');
//					offset += 5;
//					continue;
//				}
//				else if (json[offset+1]=='a' && json[offset+2]=='p' && json[offset+3]=='o' && json[offset+4]=='s' && json[offset+5]==';')
//				{
//					// &apos;
//					value.append('\'');
//					offset += 6;
//					continue;
//				}
//				else if (json[offset+1]=='q' && json[offset+2]=='u' && json[offset+3]=='o' && json[offset+4]=='t' && json[offset+5]==';')
//				{
//					// &quot;
//					value.append('"');
//					offset += 6;
//					continue;
//				}
//				else if (json[offset+1] == '#')
//				{
//					int num = 0;
//					int i;
//					boolean isValid = false;
//					boolean isHex;
//					if (json[offset+2] == 'x')
//					{
//						isHex = true;
//						i = 3;
//					}
//					else
//					{
//						isHex = false;
//						i = 2;
//					}
//					
//					for ( ; isValid; i++)
//					{
//						if (offset + i >= json.length)
//						{
//							isValid = false;
//							break;
//						}
//						
//						char c = json[offset + i];
//						if (c == ';')
//						{
//							if (i < 2 || (isHex && i < 3))
//								isValid = false;
//							break;
//						}
//						else if (c >= '0' && c <= '9')
//						{
//							num = (num * (isHex?16:10)) + (c - '0');
//						}
//						else if (isHex && (c >= 'a' && c <= 'f'))
//						{
//							num = num * 16 + (c - 'a' + 10);
//						}
//						else if (isHex && (c >= 'A' && c <= 'F'))
//						{
//							num = num * 16 + (c - 'A' + 10);
//						}
//						else
//						{
//							isValid = true;
//							break;
//						}
//					}
//						
//					if (isValid)
//					{
//						int[] codePoints = { num };
//						String string = new String(codePoints, 0, 1);
//						value.append(string);
//						offset += i + 1;
//						continue;
//					}
//				}
//				
//				// Treat this escape sequence as regular characters
//			}
//			
//			// Just accept the next character
//			value.append(json[offset]);
//			offset++;
//		}
	}

	public String getText(String xpath) throws FastJsonException
	{
		String text = getText(ROOT_NODE, xpath, 0);
		return text;
	}

	public String getText(String xpath, int occurance) throws FastJsonException
	{
		String text = getText(ROOT_NODE, xpath, occurance);
		return text;
	}

	public String getText(int parentNodeNum, String xpath)// throws FastJSONException
	{
		String text = getText(parentNodeNum, xpath, 0);
		return text;
	}

	public String getText(int parentNodeNum, String xpath, int occurance)
	{
		if (xpath == ".")
			return getValue(parentNodeNum, true); // Need to remove escape sequences

		// Find the startpoint of the search
//		int parentIndent = 0;
		int parentIndent = -1;
		int firstChildNodeNum = 0;
		if (parentNodeNum != ROOT_NODE) {
			FastJsonBlockOfNodes block = firstBlock.getBlock(parentNodeNum, false);
			int index = parentNodeNum % FastJsonBlockOfNodes.SIZE;
			parentIndent = block.indent[index];
			firstChildNodeNum = parentNodeNum + 1;
		}
		int childIndent = parentIndent + 1;

		// If the search starts with //, look anywhere below the start point
		FastJsonNodes list = new FastJsonNodes(this);
		if (xpath.startsWith("//")) {
			// Look anywhere in the hierarchy
			xpath = xpath.substring(2);
			PathSegments parts = splitSelectionPathIntoSegments(xpath);
			if (parts == null)
				return "";
			findNodesAnywhereBelow(firstChildNodeNum, childIndent, parts, 0, list, occurance);
		} else {
			// Look at the specific path
			PathSegments parts = splitSelectionPathIntoSegments(xpath);
			if (parts == null)
				return "";
			findNodes(firstChildNodeNum, childIndent, parts, 0, 0, list, occurance);
		}

		list.first();
		if (list.next()) {
			int nodeNum = list.getNodeNum();
			return getValue(nodeNum, true); // Need to remove escape sequences
		}
		return "";
	}

	public FastJsonNodes getNodes(String xpath) throws XDException
	{
		return getNodes(ROOT_NODE, xpath);
	}

	public FastJsonNodes getNodes(int parentNodeNum, String xpath)
	{
		// Find the startpoint
//		int parentIndent = 0;
		int parentIndent = -1;
		int firstChildNodeNum = 0;
		if (parentNodeNum != ROOT_NODE)
		{
			FastJsonBlockOfNodes block = firstBlock.getBlock(parentNodeNum, false);
			int index = parentNodeNum % FastJsonBlockOfNodes.SIZE;
			parentIndent = block.indent[index];
			firstChildNodeNum = parentNodeNum + 1;
		}
		int childIndent = parentIndent + 1;
		
		FastJsonNodes list = new FastJsonNodes(this);
		if (xpath.startsWith("//"))
		{
			// Look anywhere in the hierarchy
			xpath = xpath.substring(2);
			PathSegments parts = splitSelectionPathIntoSegments(xpath);
			if (parts == null)
				return null;
			findNodesAnywhereBelow(firstChildNodeNum, childIndent, parts, 0, list, -1);
		}
		else
		{
			// Look at the specific path
			PathSegments parts = splitSelectionPathIntoSegments(xpath);
			if (parts == null)
				return null;
			findNodes(firstChildNodeNum, childIndent, parts, 0, 0, list, -1);
		}
		return list;
	}

	/**
	 * Recursively look down through the tree for a match to the xpath. This is used to
	 * match XPaths starting with //, where the start point of the match can be anywhere.
	 * 
	 * @param childNodeNum
	 * @param requiredIndent
	 * @param parts
	 * @param cntMatches
	 * @param list
	 * @param occurrence
	 * @return
	 */
	private int findNodesAnywhereBelow(int childNodeNum, int requiredIndent, PathSegments parts, int cntMatches, FastJsonNodes list, int occurrence)
	{
		if (childNodeNum >= numNodes)
			return cntMatches;

		for ( ; ; )
		{
			if (childNodeNum < 0)
				return cntMatches;
			FastJsonBlockOfNodes block = firstBlock.getBlock(childNodeNum, false);
			int index = childNodeNum % FastJsonBlockOfNodes.SIZE;

			int indent = block.indent[index];
			if (indent != requiredIndent)
				return cntMatches; // This will happen if the parent node has no children.

			// Look for a match at this node
			cntMatches = findNodes(childNodeNum, requiredIndent, parts, 0, cntMatches, list, occurrence);
			if (cntMatches < 0)
				return -1; // Found the occurrence we are after

			// Look for matches within the children
			cntMatches = findNodesAnywhereBelow(childNodeNum+1, requiredIndent+1, parts, cntMatches, list, occurrence);
			if (cntMatches < 0)
				return -1; // Found the occurrence we are after
			
			// Move on to the next child
			childNodeNum = block.nextNodeAtThisLevel[index];
		}
	}

	/**
	 * The method recursively searches for nodes that match the specified selection path / occurrence.
	 * 
	 * Each time this method is called, it checks the nodes under a parent against a specific segment. The
	 * <code>childNodeNum</code> specifies the first of the child nodes (which has an indent of
	 * <code>requiredIndent</code>). The code loops around for each of the child nodes, checking the
	 * following:
	 * 
	 * [1] Does the name match the current segment? (exact match or *)
	 * 
	 * [2] Does the index match the required index for this segment? (exact match or -1)
	 * 
	 * [3] If further segments need to be matched, check the children of the node next segment of the path again.
	 * 
	 * [4] If there are no further path segments to be checked, the node is a match.
	 * 
	 * [5] If requiredOccurance is not specified, and the node to the list and continue checking.
	 * 
	 * [6] If the requiredOccurrance is specified, and totalMatches equals requiredOccurrence, we have a node we
	 * 	need, so add it to the list and return -1.
	 *  
	 * Always return the total number of matches so far so we can keep count, except when the required
	 * occurrence has been found (if it was specified), in which case return -1.
	 *  
	 * 
	 * @param childNodeNum
	 *  The first node at the new level. We'll skip from node to node at this level using node.next.
	 * @param requiredIndent
	 * 	The indent in FastJson in the depth in the hierarchy. It doesn't match up exactly with xpaths
	 * 	because some node types (objects) jump down two levels.
	 * @param segments
	 * 	The segments making up the path.
	 * @param segmentIndex
	 * 	The index into segments that we are currently checking. This method is recursive. In the first
	 * 	invocation, level should be zero.
	 * @param totalMatches
	 * 	The total number of matches of the full path that we've found so far. This is passed around through
	 * 	the various recurring, so we can work out when a specific 'occurrence' has been reached.
	 * @param list
	 * 	The list to place the matching nodes into. 
	 * @param requiredOccurrence
	 * 	If non-negative, only place the occurrence'th node into <code>list</code>.
	 *  @return
	 *  	Total number of matches so far.
	 */
	protected int findNodes(int childNodeNum, int requiredIndent, PathSegments segments, int segmentIndex, int totalMatches, FastJsonNodes list, int requiredOccurrence)
	{
		if (childNodeNum >= numNodes)
			return totalMatches;

		int cntNameMatch = -1;
		for ( ; ; )
		{
			if (childNodeNum < 0)
				return totalMatches;
			FastJsonBlockOfNodes block = firstBlock.getBlock(childNodeNum, false);
			int index = childNodeNum % FastJsonBlockOfNodes.SIZE;

			int indent = block.indent[index];
			int type = block.type[index];
			int offsetOfName = block.offsetOfName[index];
			if (indent != requiredIndent)
				return totalMatches;

			// Find the next node at the same level as this node (stop when we reach it)
			int nextChildAtThisLevelNodeNum = block.nextNodeAtThisLevel[index]; 
			
			// See if the name matches the part of the XPATH at this level
//String name = new String(json, offsetOfName, 5);
			if (nameMatchesPathSegment(offsetOfName, segments, segmentIndex))
			{
				// [1] The name matches.
				
				// See what type of node this is.
				if (type == TYPE_ARRAY) {
					// This is an array - treat each element in the array as
					// if it was this node (i.e. it matches the required name).
					// The first element will be the node directly after the array node.
					int elementNodeNum = childNodeNum + 1;
					if (
							elementNodeNum >= numNodes // After entire JSON document
							||
							(nextChildAtThisLevelNodeNum >= 0 && elementNodeNum >= nextChildAtThisLevelNodeNum) // Run out of nodes beneath the child. 
					) {
						// There are no nodes within this child.
						return 0;
					}
					for ( ; ; ) {
						cntNameMatch++;

						if (elementNodeNum < 0)
							return totalMatches;
						FastJsonBlockOfNodes elementBlock = firstBlock.getBlock(elementNodeNum, false);
						int elementIndex = elementNodeNum % FastJsonBlockOfNodes.SIZE;
						
						// See if the index matches
						if (indexMatchesPathSegmentIndex(cntNameMatch, segments, segmentIndex))
						{
							// [2] We have the right name and index
							// Have we matched all the parts in the path?
							if (haveMoreSegments(segments, segmentIndex)) {
								
								// [3] We have more path segments to check.
								// Check the children of this node against the next segment.
								totalMatches = findNodes(elementNodeNum+1, requiredIndent+2, segments, segmentIndex+1, totalMatches, list, requiredOccurrence);
								if (totalMatches < 0)
									return -1; // already found the required occurrence.
							} else {
								
								// [4] We've matched name/index for all path segments.								
								// See if this is the occurrence we're after.
								if (requiredOccurrence < 0) {
									// [5] We're accepting every occurrence.
									list.addNode(elementNodeNum);
								} else if (requiredOccurrence == totalMatches) {
									// [6] We have the occurrence we're after.
									list.addNode(elementNodeNum);
									return -1;
								}
								totalMatches++;
							}
						}

						// Move on to the next child element in the array
						elementNodeNum = elementBlock.nextNodeAtThisLevel[elementIndex]; 
					}
					
				} else if (type == TYPE_OBJECT) {
					// This is an object.
					cntNameMatch++;
					
					// See if the index matches
					if (indexMatchesPathSegmentIndex(cntNameMatch, segments, segmentIndex)) {
	
						// [2] We have the right name and index
						// Have we matched all the parts in the path?
						if (haveMoreSegments(segments, segmentIndex)) {
							
							// [3] We have more path segments to check.
							// Check the children of this node against the next segment.
							totalMatches = findNodes(childNodeNum+1, requiredIndent+1, segments, segmentIndex+1, totalMatches, list, requiredOccurrence);
							if (totalMatches < 0)
								return -1; // already found the required occurance			
						} else {

							// [4] We've matched name/index for all path segments.								
							// See if this is the occurrence we're after.
							if (requiredOccurrence < 0) {
								// [5] We're accepting every occurrence.
								list.addNode(childNodeNum);
							} else if (requiredOccurrence == totalMatches) {
								// [6] We have the occurrence we're after.
								list.addNode(childNodeNum);
								return -1;
							}
							totalMatches++;
						}
					}
					
				} else {
					// This is a primitive data type. We can't go down any levels deeper.
					cntNameMatch++;
					
					// See if the index matches
					if (indexMatchesPathSegmentIndex(cntNameMatch, segments, segmentIndex)) {
					
						// [2] We have the right name and index
						// Have we matched all the parts in the path?
						if (haveMoreSegments(segments, segmentIndex)) {
							
							// [3] We have more path segments to check.
							// However, there are no properties within this property, so we can't match them.
	//						int abc = 123;
						} else {

							// [4] We've matched name/index for all path segments.								
							// See if this is the occurrence we're after.
							if (requiredOccurrence < 0) {
								// [5] We're accepting every occurrence.
								list.addNode(childNodeNum);
							} else if (requiredOccurrence == totalMatches) {
								// [6] We have the occurrence we're after.
								list.addNode(childNodeNum);
								return -1;
							}
							totalMatches++;
						}
					}
				}
			}
			
			// Move on to the next child
			childNodeNum = block.nextNodeAtThisLevel[index];
		}
	}

	/**
	 * Return true if the name of the node matches the required name for this segment of the selection path.
	 * @param offsetOfNodeName
	 * @param partOfXpath
	 * @return
	 */
	private boolean nameMatchesPathSegment(int offsetOfNodeName, PathSegments segments, int segmentIndex) {
//String zzzName = new String(json, offsetOfNodeName, 10);
		// The node has no name
		if (offsetOfNodeName < 0)
			return false;

		// Get the required segment name
		String requiredName = segments.name[segmentIndex];

		// The path segment matches any name
		if (requiredName.equals("*"))
			return true;
		
		// Check that the part name matches the node name.
		// The node name is terminated by double quote.
		char[] segmentName = requiredName.toCharArray();
		for (int i = 0; ; i++) {
			// See if we're at the end of the part name, or the node name
			char nameChar = json[offsetOfNodeName + i];
			boolean endOfNodeName = (nameChar == '"');
			boolean endOfPartName= (i == segmentName.length);
			
			if ( !endOfNodeName && !endOfPartName) {
				// We're not at the end of either the xpath part or the node name.
				// Compare the next character
				char partChar = segmentName[i];
				if (partChar != nameChar)
					return false; // different names
			} else {
				// At the end of something.
				boolean endOfBoth = (endOfNodeName && endOfPartName);
				return endOfBoth;
			}
		}
	}

	/**
	 * Return true if this node matches the required index for this segment.
	 * 
	 * @param cntNameMatch
	 * @param segments
	 * @param segmentIndex
	 * @return
	 */
	private boolean indexMatchesPathSegmentIndex(int cntNameMatch, PathSegments segments, int segmentIndex) {
	
		// If no index was provided, match all nodes with the right name (segment="abc")
		int requiredIndex = segments.index[segmentIndex];
		if (requiredIndex < 0)
			return true;
		
		// See if we have the requested index (segment="abc[123]")
		if (cntNameMatch == requiredIndex)
			return true;
		
		return false;
	}

	/**
	 * Return true if there are more segments in the path after this segment.
	 * 
	 * @param segments
	 * @param segmentIndex
	 * @return
	 */
	private boolean haveMoreSegments(PathSegments segments, int segmentIndex) {
		return (segmentIndex < (segments.name.length - 1));
	}

	/**
	 * Split a selection path into segments.
	 * For example "/abc/def[123]/ghi" becomes two arrays:
	 * 	name = [ "abc", "def", "ghi"
	 * 	index = [ -1, 123, -1 ]
	 * 
	 * <p>
	 * This object avoids using a list of {name,index} because we're shooting for speed
	 * (trying to minimize collection traversal) and trying to reduce object creation.
	 * 
	 * @param xpath
	 * 	A selection path (eg. "/abc/def[123]")
	 * @return
	 * 	A PathSegments object, containing the names and indexes, or null if the path is invalid.
	 */
	protected PathSegments splitSelectionPathIntoSegments(String xpath)
	{
		// Ignore any initial '/'
		if (xpath.startsWith("/"))
			xpath = xpath.substring(1);

		// Ignore any initial './'
		if (xpath.startsWith("./"))
			xpath = xpath.substring(2);

		// Split the path into segments, placing the name and index of each segment into a list.
		List<String> nameList = new ArrayList<String>();
		List<Integer> indexList = new ArrayList<Integer>();
		try {
			for ( ; ; )
			{
				int pos = xpath.indexOf("/");
				if (pos < 0)
				{
					splitSegment(xpath, nameList, indexList);
					break;
				}
				String segment = xpath.substring(0, pos);
				xpath = xpath.substring(pos + 1);
				splitSegment(segment, nameList, indexList);
			}
		} catch (XDException e) {
			return null;
		}
		
		// Convert the name and index lists into a single PathSegments object.
		PathSegments segments = new PathSegments();
		int numSegments = nameList.size();
		segments.name = new String[numSegments];
		segments.index = new int[numSegments];
		for (int i = 0; i < numSegments; i++) {
			String name = nameList.get(i);
			Integer index = indexList.get(i);
			
			segments.name[i] = name;
			segments.index[i] = index.intValue();
		}
		return segments;
	}
	
	/**
	 * Split a string like abc[123] into it's name and index components, and add each to the provided lists.
	 * 
	 * @param segment
	 * @param nameList
	 * @param indexList
	 * @throws XDException 
	 */
	private void splitSegment(String segment, List<String> nameList, List<Integer> indexList) throws XDException {
		
		// See if an array index is specified.
		int pos = segment.indexOf('[');
		String name;
		int index;
		if (pos < 0) {
			// no index is provided, use an index of -1 to indicate no index.
			name = segment;
			index = -1;
		} else {
			// Split the segment into name and index.
			name = segment.substring(0, pos);
			String tmp = segment.substring(pos + 1);
			if ( !tmp.endsWith("]"))
				throw new XDException("Invalid index in selection path (" + segment + ")");
			tmp = tmp.substring(0, tmp.length() - 1);
			
			// Parse the integer and chec it's valid
			try {
				index = Integer.parseInt(tmp);
			} catch (NumberFormatException e) {
				index = -1; // force an error
			}
			if (index < 0)
				throw new XDException("Invalid index in selection path (" + segment + ")");
		}
		
		// Add the name and index to the provided lists
		nameList.add(name);
		indexList.add(new Integer(index));
	}

//	public String getJson()
//	{
//		return new String(this.json);
//	}

	protected String getFieldName(int nodeNum) {
		FastJsonBlockOfNodes block = firstBlock.getBlock(nodeNum, false);
		int index = nodeNum % FastJsonBlockOfNodes.SIZE;
		int nameOffset = block.offsetOfName[index];
		
		String name = readNameString(nameOffset);
		return name;
	}

	//--------------------------------------------------------------------------------------------------------------------
	// Methods for the XDSelector interface

	@Override
	public String getString(String xpath) throws XDException {
		try {
			String string = getText(xpath);
			return string;
		} catch (Exception e) {
			XDException exception = new XDException(e.getMessage());
			exception.setStackTrace(e.getStackTrace());
			throw exception;
		}
	}

	@Override
	public String getString(String xpath, String defaultValue) throws XDException {
		String string = getString(xpath);
		if (string.equals(""))
			return defaultValue;
		return string;
	}

	@Override
	public int getInteger(String xpath) throws XDNumberFormatException, XDException {
		return XD.getInteger(this, xpath);
	}

	@Override
	public int getInteger(String xpath, int defaultValue) throws XDNumberFormatException, XDException {
		return XD.getInteger(this, xpath, defaultValue);
	}

	@Override
	public boolean getBoolean(String xpath) throws XDException {
		return XD.getBoolean(this, xpath);
	}

	@Override
	public boolean getBoolean(String xpath, boolean defaultValue) throws XDException {
		return XD.getBoolean(this, xpath, defaultValue);
	}


	//--------------------------------------------------------------------------------------------------------------------
	// Return the number of records that can be iterated over.

	@Override
	public int size() {
		return 1;
	}

	
	//--------------------------------------------------------------------------------------------------------------------
	// Iterate over this object using first, next.
	
	private boolean beenToFirst = false;

	/**
	 * This data type does not provide a list of records, so the {@link #next()} method only returns true once.<p>
	 * To iterate over elements within this object, use {@link #select(String)} or one of the {@link #foreach(String)} methods.
	 */
	@Override
	public void first() {
		beenToFirst = false;
	}

	/**
	 * This data type does not provide a list of records, so this method only returns true until {@link #next()} is called.<p> 
	 * To iterate over elements within this object, use {@link #select(String)} or one of the {@link #foreach(String)} methods.
	 */
	@Override
	public boolean hasNext() {
		if (beenToFirst)
			return false;
		return true;
	}

	/**
	 * This data type does not provide a list of records, so this method only returns true one time. Actually it serves
	 * no purpose other than to allow iterators to access the data that can be accessed directly. For example,
	 * <pre>
	 * FastJson data = ...;
	 * String value = data.string("./name");
	 * </pre>
	 * will return exactly the same as:
	 * <pre>
	 * FastJson data = ...;
	 * while (data.next()) {
	 *   String value = data.string("./name");
	 * }
	 * </pre>
	 * <p>
	 * To iterate over a list of elements <i>within</i> this data object, use {@link #select(String)} or one of the {@link #foreach(String)} methods.
	 */
	@Override
	public boolean next() {
		if (beenToFirst)
			return false;
		beenToFirst = true;
		return true;
	}

	@Override
	public int currentIndex() {
		return 0;
	}

	@Override
	public boolean setCurrentIndex(int index) throws XDException {
		if (index == 0)
			return true;
		return false;
	}

	@Override
	public String currentName() {
		String name = getFieldName(0);
		return name;
	}
	
	//--------------------------------------------------------------------------------------------------------------------
	// Iterate over this object using a Java iterator
	
	@Override
	public Iterator<XSelector> iterator() {
		return new XIterator(this);
	}

	
	
	//--------------------------------------------------------------------------------------------------------------------
	// Select elements within this data object

	@Override
	public XSelector select(String xpath) throws XDException {
		return getNodes(xpath);
	}

	
	//--------------------------------------------------------------------------------------------------------------------
	// Select and iterate using a callback

	@Override
	public void foreach(String xpath, XDCallback callback) throws XDException {
		foreach(xpath, callback, null);
	}

	@Override
	public void foreach(String xpath, Object userData, XDCallback callback) throws XDException {
		try {
			XSelector list = this.getNodes(xpath);
			for (int index = 0; list.hasNext(); index++) {
				list.next();
				callback.next(list, index, userData);
			}
		} catch (Exception e) {
			XDException exception = new XDException(e.getMessage());
			exception.setStackTrace(e.getStackTrace());
			throw exception;
		}
	}

	
	//--------------------------------------------------------------------------------------------------------------------
	// Select and iterate using a Java iterator
	
	@Override
	public Iterable<XSelector> foreach(String xpath) throws XDException {
		FastJsonNodes list = this.getNodes(xpath);
		return list;
	}

	/**
	 * List the hierarchy of nodes to standard output.
	 */
	public void debugDump() {
		firstBlock.debugDump(json, numNodes);
	}
}
