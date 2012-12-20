package com.tooltwist.fastJson;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import com.tooltwist.fastJson.FastJsonBlockOfNodes;
import com.tooltwist.fastJson.FastJsonException;
import com.tooltwist.fastJson.FastJsonNodes;
import com.tooltwist.xdata.X2DataException;
import com.tooltwist.xdata.X2DataIterator;
import com.tooltwist.xdata.XIteratorCallback;
import com.tooltwist.xdata.XSelectable;

/**
 * Parse a JSON text object, without creating large numbers of Java objects.
 * 
 * http://www.json.org
 * http://www.ietf.org/rfc/rfc4627.txt
 * 
 * @author philipcallender
 *
 */
public class FastJson implements XSelectable, Iterable<XSelectable> {
	
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
	
	// Blocks of nodes
	private FastJsonBlockOfNodes firstBlock = new FastJsonBlockOfNodes(0);
	private int	numNodes = 0;
	
	// Values used while parsing
	private char json[];
	private int offset;
	private int lineCnt;
	private int lineOffset;
	
	private int indentLevel;
	private static final int MAX_LEVELS_OF_INDENT = 100;
	private int lastNodeAtEachIndentLevel[];
	

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
			System.err.println("Cannot load XML from file '"+path+"': " + e);
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
		this.offset = 0;
		this.lineCnt = 1;
		this.lineOffset = 0;
		
		indentLevel = 0;
		lastNodeAtEachIndentLevel = new int[MAX_LEVELS_OF_INDENT];
		lastNodeAtEachIndentLevel[0] = -1;
		
		try {
			skipSpaces();
			if (json[offset] == '{')
				parseObject(0, 0, NAME_OFFSET_PARSING_ROOT);
			else if (json[offset] == '[')
				parseArray(0, 0, NAME_OFFSET_PARSING_ROOT);
			else
				syntax("Expected '{' or '['");
			
			//ZZZ Perhaps we should check there's nothing left.
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			syntax("Unexpected end of XML");
		}
	}

	
	private void parseObject(int nodeNumZZ, int indent, int offsetOfName) throws FastJsonException
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
		if (json[offset] != '{')
			syntax("Expected '{'");
		offset++;
		
		// Check for an empty object
		if (json[offset] == '}') {
			// End of the object
			offset++;
System.out.println("Empty Object.");
			return;
		}
		
		// Loop around getting name / value pairs.
		for ( ; ; ) {
			skipSpaces();
		
			// Read the name
			if (json[offset] != '\"')
				syntax("Expected '\"'");
			int offsetOfNameForThisProperty = offset + 1;
			parseString(nodeNumZZ, indent, NAME_OFFSET_PARSING_NAME);

			// Expect ':'
			skipSpaces();
			if (json[offset] != ':')
				syntax("Expected ':'");
			offset++;

			// Expect a value
			skipSpaces();
			parseValue(nodeNumZZ, indent, offsetOfNameForThisProperty);
			
			// Expect either ',' or '}'
			skipSpaces();
			if (json[offset] == '}') {
				offset++;
//				System.out.println("End of Object.");
				return;
			}
			
			if (json[offset] != ',')
				syntax("Expected ',' or '}'");
			offset++;
		}
		
		
/*
		// Keep parsing the content and any tags within it, until we reach an end tag
		int previousNodeAtThisLevel = -1;
		for ( ; ; )
		{
			if (offset >= json.length)
			{
				if (nodeNum < 0)
					return;
				String name = block.name[index];
				syntax("Unexpected end of XML (tag "+name+" is incomplete)");
			}
			
//			skipSpaces();
//			if (json[offset] == '{') {
//				parseObject(int nodeNum, indent + 1);
//			} else {
//				syntax('Expected '{'"')
//			}
			
			
			
			
			
			if (json[offset] == '<')
			{
				int start = offset;
				offset++;
				skipSpaces();

				// See if this tag terminates itself ( i.e. <name..../> )
				if (json[offset] == '/')
				{
					offset++;
					parseEndTag(nodeNum, start);
					return;
				}

				int newNodeNum = -1;
				if (json[offset] == '?')
				{
					// This a special tag ( i.e. <?....?> )
					offset++;
					newNodeNum = parseEncodingTag(indent + 1, start);
				}
				else if (json[offset] == '!')
				{
					boolean gotIt = false;
					try {
						offset++;
						if (json[offset]=='-' && json[offset+1]=='-')
						{
							// This a comment ( <!-- .... --> )
							offset += 2;
							newNodeNum = parseComment(indent + 1, start);
							gotIt = true;
						}
						else if (
								json[offset]=='['
								&& json[offset+1]=='C'
								&& json[offset+2]=='D'
								&& json[offset+3]=='A'
								&& json[offset+4]=='T'
								&& json[offset+5]=='A'
								&& json[offset+6]=='['
						)
						{
							// This a cdata ( <![CDATA[ .... ]]> )
							offset += 7;
							newNodeNum = parseCdata(indent + 1, start);
							gotIt = true;
						}
					}
					catch (ArrayIndexOutOfBoundsException e) { }
					if ( !gotIt)
						syntax("Expected <!-- or <![CDATA[");
				}
				else
				{
					// This a new tag ( i.e. <name...> )
					newNodeNum = parseTag(indent + 1, start);
				}
				
				if (previousNodeAtThisLevel >= 0)
				{
					FastJsonBlockOfNodes prevBlock = firstBlock.getBlock(previousNodeAtThisLevel, false);
					int prevIndex = previousNodeAtThisLevel % FastJsonBlockOfNodes.SIZE;
					prevBlock.nextNodeAtThisLevel[prevIndex] = newNodeNum;
				}
				previousNodeAtThisLevel = newNodeNum;
/*
System.out.println("-----------------------------------------------");
System.out.println("parseContents("+offset+") continuing");
System.out.println("offset:"+offset+", line: "+lineCnt);
System.out.println(thisLine()+"\n"+positionMarker(offset));
System.out.println(new String(json, offset, json.length-offset));
* /
			}
			else if (json[offset] == '&')
			{
				if (json[offset+1]=='g' && json[offset+2]=='t' && json[offset+3]==';')
				{
					// &gt;
					offset += 4;
				}
				else if (json[offset+1]=='l' && json[offset+2]=='t' && json[offset+3]==';')
				{
					// &lt;
					offset += 4;
				}
				else if (json[offset+1]=='a' && json[offset+2]=='m' && json[offset+3]=='p' && json[offset+4]==';')
				{
					// &amp;
					offset += 5;
				}
				else if (json[offset+1]=='a' && json[offset+2]=='p' && json[offset+3]=='o' && json[offset+4]=='s' && json[offset+5]==';')
				{
					// &apos;
					offset += 6;
				}
				else if (json[offset+1]=='q' && json[offset+2]=='u' && json[offset+3]=='o' && json[offset+4]=='t' && json[offset+5]==';')
				{
					// &quot;
					offset += 6;
				}
				else if (json[offset+1] == '#')
				{
					int i = 2;
					boolean isHex = false;
					if (json[offset + i] == 'x')
					{
						i++; // hex
						isHex = true;
					}
					
					// Skip to the end of the number
					boolean isValid = true;
					for ( ; isValid; i++)
					{
						if (offset + i > json.length)
						{
							isValid = false;
							break;
						}

						char c = json[offset + i];
						if (
							(c >= '0' && c <= '9') // Decimal char
							||
							isHex && ((c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f')) // Hex char
						)
						{
							// is a valid digit
						}
						else
						{
							if (c != ';')
								isValid = false;
							offset += i + 1;
							break;
						}
						
					}
					if ( !isValid)
						syntax("Expected &#1239; or &#x0123f;");
				}
				else
					syntax("Expected one of &gt; &lt; &amp; &apos; or &quot;");
			}
			else
			{
				// Normal character
				if (json[offset] == '\n')
				{
					lineCnt++;
					lineOffset = offset + 1;
				}
				offset++;
			}
		}
		*/
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
	private void parseArray(int nodeNum, int indent, int offsetOfName) throws FastJsonException
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
		if (json[offset] != '[') {
			syntax("Expected '['");
		}
		offset++;
		
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

		// Parse the contents of the array.
		for ( ; ; ) {
			parseValue(nodeNum, indent, NAME_OFFSET_PARSING_ARRAY);
			
			skipSpaces();
			if (json[offset] == ']') {
				// End of the array
//System.out.println("End of Array");
				offset++;
				return;
			}
			
			if (json[offset] != ',') {
				syntax("Expected ',' or ']'");
			}
			
			offset++;
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
	private void parseNumber(int nodeNumZZZ, int indent, int offsetOfName) throws FastJsonException
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
		int startOffset = offset;
		boolean is_negative = false;
		if (json[offset] == '-') {
			offset++;
			is_negative = true;
		}
		
		// Get the integer part
		long int_part = 0;
		char digit = json[offset];
		if (digit == '0') {
			// Leading zero can only be for number zero.
			offset++;
			int_part = 0;
		} else if (digit >= '1' && digit <= '9') {
			offset++;
			while (json[offset] >= '0' && json[offset] <= '9') {
				offset++;
			}
		}
		
		// See if there is a decimal part
		if (json[offset] == '.') {
			offset++;
			while (json[offset] >= '0' && json[offset] <= '9') {
				offset++;
			}
		}
		
		// See if there is an exponent part
		if (json[offset] == 'e' || json[offset] == 'E') {
			offset++;
			if (json[offset] == '+' || json[offset] == '-') {
				offset++;
			}
			while (json[offset] >= '0' && json[offset] <= '9') {
				offset++;
			}
		}
		
//		String num = new String(json, startOffset, offset - startOffset);
//		System.out.println("Number is " + num);
		registerNode(TYPE_NUMBER, offsetOfName, startOffset, offset);
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
		FastJsonBlockOfNodes block = firstBlock.getBlock(nodeId, false);
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
	private void parseString(int nodeNumZZZ, int indent, int offsetOfName) throws FastJsonException
	{
/*
System.out.println("-----------------------------------------------");
System.out.println("parseString("+offset+")");
System.out.println("offset:"+offset+", line: "+lineCnt);
System.out.println(thisLine()+"\n"+positionMarker(offset));
System.out.println(new String(json, offset, json.length-offset));
*/

		char ESCAPE = 0x5c;
		char QUOTE = 0x22;
		
		skipSpaces();
		if (json[offset] != QUOTE)
			syntax("Expected '\"'");
		offset++;

		// Skip over the string
		int startOffset = offset;
		StringBuilder string = new StringBuilder();
		for ( ; ; ) {
			char c = json[offset];
			
			if (c == '\"') {
				
				// Must be the end quote
//System.out.println("String is [" + string.toString() + "]");
				
				// Looks like we have a string. Register it's value.		
				if (offsetOfName == NAME_OFFSET_PARSING_NAME) {
					// Just ignore this. It's not actually a value.
				} else if (offsetOfName == NAME_OFFSET_PARSING_ARRAY) {
//					System.out.println("                 NEW NODE: STRING(in array)");
					registerNode(TYPE_STRING, offsetOfName, startOffset, offset);
				} else if (offsetOfName >= 0) {
//					System.out.println("                 NEW NODE: STRING(" + offsetOfName + ")");
					registerNode(TYPE_STRING, offsetOfName, startOffset, offset);
//					FastJsonBlockOfNodes block = firstBlock.getBlock(numNodes, false);
//					int index = numNodes % FastJsonBlockOfNodes.SIZE;
//					lastNodeAtEachIndentLevel[indentLevel] = numNodes;
//					numNodes++;
//					block.type[index] = TYPE_STRING;
//					block.offsetOfName[index] = offsetOfName;
//					block.offsetOfValue[index] = startOffset;
//					block.offsetOfValueEnd[index] = offset;
				} else {
					// This should not be possible
					syntax("Internal error 82603");
				}

				offset++;
				return;
				
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
						syntax("Expected unicode \\uXXXX");
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
	private void parseValue(int nodeNum, int endTag, int offsetOfName) throws FastJsonException
	{
/*
System.out.println("-----------------------------------------------");
System.out.println("parseValue("+offset+")");
System.out.println("offset:"+offset+", line: "+lineCnt);
System.out.println(thisLine()+"\n"+positionMarker(offset));
System.out.println(new String(json, offset, json.length-offset));
*/

		skipSpaces();
		if (json[offset] == '\"') {
			// Quote, so this is a string
			parseString(nodeNum, endTag, offsetOfName);
		} else if (json[offset]=='f' && json[offset+1]=='a' && json[offset+2]=='l' && json[offset+3]=='s' && json[offset+4]=='e') {
			// "false"
//System.out.println("Value is 'false'");
			registerNode(TYPE_FALSE, offsetOfName, offset, offset + 5);
			offset += 5;
		} else if (json[offset]=='n' && json[offset+1]=='u' && json[offset+2]=='l' && json[offset+3]=='l') {
			// "null"
//System.out.println("Value is 'null'");
			registerNode(TYPE_NULL, offsetOfName, offset, offset + 4);
			offset += 4;
		} else if (json[offset]=='t' && json[offset+1]=='r' && json[offset+2]=='u' && json[offset+3]=='e') {
			// "true"
//System.out.println("Value is 'true'");
			registerNode(TYPE_TRUE, offsetOfName, offset, offset + 4);
			offset += 4;
		} else if (json[offset] == '{') {
			// Value is an object
			registerNode(TYPE_OBJECT, offsetOfName, offset, offset);
			indent();
			parseObject(nodeNum, -1, offsetOfName);
			unindent();
		} else if (json[offset] == '[') {
			// Value is an array
			registerNode(TYPE_ARRAY, offsetOfName, offset, offset);
			indent();
			parseArray(nodeNum, -1, offsetOfName);
			unindent();
		} else if (json[offset] == '-' || (json[offset] >= '0' && json[offset] <= '9')) {
			parseNumber(nodeNum, -1, offsetOfName);
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

	private int hex(char c) throws FastJsonException {
		if (c >= '0' && c <= '9') {
			return (c - '0');
		} else if (c >= 'a' && c <= 'f') {
			return 10 + (c - 'a');
		} else if (c >= 'a' && c <= 'f') {
			return 10 + (c - 'A');
		}
		syntax("Invalid hex character '" + c + "'."); // Should not be possible
		return 0; // Never gets here
	}

	/*
	private int parseTag(int indent, int startTag) throws FastJSONException
	{
		String name = null;
		try {
			int nodeNum = numNodes++;
			FastJsonBlockOfNodes block = firstBlock.getBlock(nodeNum, true);
			int index = nodeNum % FastJsonBlockOfNodes.SIZE;
/*
System.out.println("-----------------------------------------------");
System.out.println("parseTag("+offset+")");
System.out.println("offset:"+offset+", line: "+lineCnt);
System.out.println(thisLine()+"\n"+positionMarker(startTag));
System.out.println(new String(json, offset, json.length-offset));
* /
			skipSpaces();
			name = parseName("tag name");

			for ( ; ; )
			{
				skipSpaces();
				
				// See if we're at the end of a self-terminating tag: <name.../>
				if (json[offset] == '/')
				{
					offset++;
					skipSpaces();
					if (json[offset] != '>')
						syntax("Expected '>' after '/' in tag <"+name+"/>");
					offset++;

					block.type[index] = TYPE_TAG;
					block.indent[index] = indent;
					block.line[index] = lineCnt;
					block.name[index] = name;
					block.startTag[index] = startTag;
					block.afterStartTag[index] = offset;
					block.endTag[index] = offset;
					block.afterEndTag[index] = offset;
					block.nextNodeAtThisLevel[index] = -1;
					return nodeNum;
				}
				
				// See if we're at the end of a normal tag: <name...>
				if (json[offset] == '>')
				{
					// <name>
					offset++;

					block.type[index] = TYPE_TAG;
					block.indent[index] = indent;
					block.line[index] = lineCnt;
					block.name[index] = name;
					block.startTag[index] = startTag;
					block.afterStartTag[index] = offset;
					// endTag and afterEndTag will be set by parseEndTag()
					block.nextNodeAtThisLevel[index] = -1;
					parseContents(nodeNum, indent);
					return nodeNum;
				}
				
				// Skip over an attribute name
				String attrib = parseName("attribute name in tag <"+name+" ...>");
				skipSpaces();
				if (json[offset] != '=')
					syntax("Expected '=' after attribute name '"+attrib+"' in tag <"+name+" ...>");
				offset++;
				skipSpaces();
				if (json[offset] == '"')
					skipString("Invalid value for attribute '"+attrib+"' in tag <"+name+" ...>", '"');
				else if (json[offset] == '\'')
					skipString("Invalid value for attribute '"+attrib+"' in tag <"+name+" ...>", '\'');
				else
					syntax("Expected a value for attribute '"+attrib+"' in tag <"+name+" ...>");
			}
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			if (name == null)
				syntax("Invalid start tag: unexpected end of XML");
			else
				syntax("Unterminated tag <"+name+"... >");
			return -1; // Cannot actually get here because syntax() always throws an exception
		}
	}
	
	private int parseEncodingTag(int indent, int startTag) throws FastJSONException
	{
		// Already read < and ?
		skipSpaces();
		String name = parseName("tag name");

		// Read attribute definitions, up to the end of the tag
		for ( ; ; )
		{
			skipSpaces();
			if (json[offset] == '?' && json[offset+1] == '>')
			{
				offset += 2;
				break;
			}
			
			// Skip over name="value"
			String attrib = parseName("attribute");
			skipSpaces();
			if (json[offset] != '=')
				syntax("Expected '=' after attribute name '"+attrib+"' in tag <?"+name+" ...?>");
			offset++;
			skipSpaces();
			if (json[offset] != '"')
				syntax("Expected a value for attribute '"+attrib+"' in tag <?"+name+" ...?>");
			skipString("Invalid value for attribute '"+attrib+"' in tag <?"+name+" ...?>", '"');
		}
		int nodeNum = numNodes++;
		FastJsonBlockOfNodes block = firstBlock.getBlock(nodeNum, true);
		int index = nodeNum % FastJsonBlockOfNodes.SIZE;
		
		block.type[index] = TYPE_HEADER;
		block.indent[index] = indent;
		block.line[index] = lineCnt;
		block.name[index] = null;
		block.startTag[index] = startTag;
		block.afterStartTag[index] = offset;
		block.endTag[index] = offset;
		block.afterEndTag[index] = offset;
		block.nextNodeAtThisLevel[index] = -1;
		return nodeNum;
	}
	
	private int parseComment(int indent, int startTag) throws FastJSONException
	{
		// Already read <!--
		for ( ; ; offset++)
		{
			if (json[offset]=='-' && json[offset+1]=='-' && json[offset+2]=='>')
			{
				offset += 3;
				break;
			}
			if (json[offset] == '\n')
			{
				lineCnt++;
				lineOffset = offset + 1;
			}
		}
		int nodeNum = numNodes++;
		FastJsonBlockOfNodes block = firstBlock.getBlock(nodeNum, true);
		int index = nodeNum % FastJsonBlockOfNodes.SIZE;
		
		block.type[index] = TYPE_COMMENT;
		block.indent[index] = indent;
		block.line[index] = lineCnt;
		block.name[index] = null;
		block.startTag[index] = startTag;
		block.afterStartTag[index] = offset;
		block.endTag[index] = offset;
		block.afterEndTag[index] = offset;
		block.nextNodeAtThisLevel[index] = -1;
		return nodeNum;
	}
	
	private int parseCdata(int indent, int startTag)
	{
		// Already read <![CDATA[
		int afterStartTag = offset;
		for ( ; ; offset++)
		{
			if (json[offset] == '\n')
			{
				lineCnt++;
				lineOffset = 0;
			}
			else if (json[offset]==']' && json[offset+1]==']' && json[offset+2]=='>')
				break;
		}
		int endTag = offset;
		offset += 3;
		
		int nodeNum = numNodes++;
		FastJsonBlockOfNodes block = firstBlock.getBlock(nodeNum, true);
		int index = nodeNum % FastJsonBlockOfNodes.SIZE;

		block.type[index] = TYPE_CDATA;
		block.indent[index] = indent;
		block.line[index] = lineCnt;
		block.name[index] = null;
		block.startTag[index] = startTag;
		block.afterStartTag[index] = afterStartTag;
		block.endTag[index] = endTag;
		block.afterEndTag[index] = offset;
		block.nextNodeAtThisLevel[index] = -1;
		return nodeNum;
	}
	
	private void parseEndTag(int nodeNum, int endTag) throws FastJSONException
	{
/*
System.out.println("-----------------------------------------------");
System.out.println("parseEndTag("+offset+")");
System.out.println("offset:"+offset+", line: "+lineCnt);
System.out.println(thisLine()+"\n"+positionMarker(offset));
System.out.println(new String(json, offset, json.length-offset));
* /
		FastJsonBlockOfNodes block = firstBlock.getBlock(nodeNum, false);
		int index = nodeNum % FastJsonBlockOfNodes.SIZE;

		// The < and / have already been checked, so check here
		// that the end tag has the correct name and nothing else.
		skipSpaces();
		String name = parseName("end tag name");
		if (nodeNum < 0)
			syntax("Unmatched end tag </"+name+">");
		if ( !name.equals(block.name[index]))
			syntax("Tag <"+block.name[index]+"... > has mismatched end tag </"+name+">");
		skipSpaces();
		if (json[offset] != '>')
			syntax("Error in end tag </"+name+"...>");
		offset++;
		
		block.endTag[index] = endTag;
		block.afterEndTag[index] = offset;
	}
	*/

	private void skipSpaces()
	{
		for ( ; json[offset]==' ' || json[offset]=='\t' || json[offset]=='\n' || json[offset]=='\r'; offset++)
			if (json[offset] == '\n')
			{
				lineCnt++;
				lineOffset = offset + 1;
			}
	}
	
//	private String parseName(String typeOfName) throws FastJSONException
//	{
//		// Check the first character is valid
//		int start = offset;
//		char c = json[offset];
//		if ((c != '_') && (c<'a' || c>'z') && (c<'A' || c>'Z'))
//			syntax("Invalid " + typeOfName);
//		
//		// Read the rest of the name
//		for (offset++; ; offset++)
//		{
//			c = json[offset];
//			if (c==' ' || c=='\t' || c=='\n' || c=='\r' || c=='/' || c=='>' || c=='=')
//				return new String(json, start, offset-start);
//		}
//	}
	
//	private String skipString(String errorStr, char endChar) throws FastJSONException
//	{
//		StringBuffer value = new StringBuffer();
//		if (json[offset] != '"')
//			syntax(errorStr);
//		for (offset++; ; offset++)
//		{
//			char c = json[offset];
//			if (c == endChar)
//			{
//				offset++;
//				return value.toString();
//			}
//			if (c == '\\')
//			{
//				offset++;
//				c = json[offset];
//				switch (c)
//				{
//				case '\n':
//				case '\r':
//					syntax(errorStr);
//					break;
//				case 'n':	value.append('\n'); break;
//				case 'r':	value.append('\r'); break;
//				case 't':	value.append('\t'); break;
//				default:		value.append(c); break;
//				}
//			}
//			else
//				value.append(c);
//		}
//	}

	void syntax(String error) throws FastJsonException
	{
		String str = "Xml parse error on line "+lineCnt+": "+error+"\n"
			+ thisLine() + "\n"
			+ positionMarker(offset);
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

//	public String getTagName(int nodeNum)
//	{
//		FastJsonBlockOfNodes block = firstBlock.getBlock(nodeNum, false);
//		int index = nodeNum % FastJsonBlockOfNodes.SIZE;
//		return block.name[index];
//	}

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

//		int type = block.type[index];
//		int indent = block.indent[index];
		int valueStart = block.offsetOfValue[index];
		int valueEnd = block.offsetOfValueEnd[index];
		
//		// See if there are any child nodes
//		int childNodeNum = nodeNum + 1;
//		while (childNodeNum > 0 && childNodeNum < numNodes)
//		{
//			FastJsonBlockOfNodes childBlock = firstBlock.getBlock(childNodeNum, false);
//			int childIndex = childNodeNum % FastJsonBlockOfNodes.SIZE;
//
//			int childType = childBlock.type[childIndex];
//			int childIndent = childBlock.indent[childIndex];
//			int childStartTag = childBlock.startTag[childIndex];
//			int childAfterEndTag = childBlock.afterEndTag[childIndex];
//			int nextChildNode = childBlock.nextNodeAtThisLevel[childIndex];
//			if (childIndent < 0)
//			{
//				// Skip over the comment or special tag
//				childNodeNum++;
//				deEscapeAndAddToBuffer(buffer, contentPos, childStartTag);
//				contentPos = childAfterEndTag;
//				continue;
//			}
//			if (childIndent <= indent)
//				break; // Should only be possible if there are no children
//			
//			// Add the content before this child to the value
//			deEscapeAndAddToBuffer(buffer, contentPos, childStartTag);
//			
//			// Perhaps add the child's value to the buffer
////			if (childType==TYPE_CDATA || (childType==TYPE_TAG && includeNestedNodeValues))
////				getValue(buffer, childNodeNum, includeNestedNodeValues);
//			contentPos = childAfterEndTag;
//			
//			// Look at the next child node
//			childNodeNum = nextChildNode; 
//		}
		

		// Add the content after the last child
		deEscapeAndAddToBuffer(buffer, valueStart, valueEnd);
	}
	
	private void deEscapeAndAddToBuffer(StringBuffer value, int start, int end)
	{
		if (end <= start)
			return;
		//ZZZZ Should de-escape value
		value.append(json, start, end-start);
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
		return getText(ROOT_NODE, xpath, 0);
	}

	public String getText(String xpath, int occurance) throws FastJsonException
	{
		return getText(ROOT_NODE, xpath, occurance);
	}

	public String getText(int parentNodeNum, String xpath)// throws FastJSONException
	{
		return getText(parentNodeNum, xpath, 0);
	}

	//ZZZZZQQQ Make this work

	public String getText(int parentNodeNum, String xpath, int occurance)// throws FastJSONException
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
			String parts[] = splitXPath(xpath);
			findNodesAnywhereBelow(firstChildNodeNum, childIndent, parts, 0, list, occurance);
		} else {
			// Look at the specific path
			String parts[] = splitXPath(xpath);
			findNodes(firstChildNodeNum, childIndent, parts, 0, 0, list, occurance);
		}

		list.first();
		if (list.next()) {
			int nodeNum = list.getNodeNum();
			return getValue(nodeNum, true); // Need to remove escape sequences
		}
		return "";
	}

	public FastJsonNodes getNodes(String xpath)
	{
		return getNodes(ROOT_NODE, xpath);
	}

	//ZZZZZQQQ Make this work
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
			xpath = xpath.substring(2);
			String parts[] = splitXPath(xpath);
			findNodesAnywhereBelow(firstChildNodeNum, childIndent, parts, 0, list, -1);
		}
		else
		{
			String parts[] = splitXPath(xpath);
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
	 * @param occurrance
	 * @return
	 */
	private int findNodesAnywhereBelow(int childNodeNum, int requiredIndent, String parts[], int cntMatches, FastJsonNodes list, int occurrance)
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
				return cntMatches;

			// Look for a match at this node
			cntMatches = findNodes(childNodeNum, requiredIndent, parts, 0, cntMatches, list, occurrance);
			if (cntMatches < 0)
				return -1; // Found the occurrance we are after

			// Look for matches within the children
			cntMatches = findNodesAnywhereBelow(childNodeNum+1, requiredIndent+1, parts, cntMatches, list, occurrance);
			if (cntMatches < 0)
				return -1; // Found the occurrance we are after
			
			// Move on to the next child
			childNodeNum = block.nextNodeAtThisLevel[index];
		}
	}

	/*
	 * This method needs to work iteratively - a recursive method would need to
	 * return the results and also
	 * 
	 *  @return
	 *  	Number of matches so far.
	 */
	protected int findNodes(int childNodeNum, int requiredIndent, String parts[], int level, int cntMatches, FastJsonNodes list, int occurrance)
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
//			String name = block.name[index];
			int type = block.type[index];
			int offsetOfName = block.offsetOfName[index];
			if (indent != requiredIndent)
				return cntMatches;
			
			// See if the name matches the part of the XPATH at this level
//String name = new String(json, offsetOfName, 5);
			if (nodeNameMatchesPartOfXpath(offsetOfName, parts[level]))
			{
				// The name matches. See what type of node this is.
				if (type == TYPE_ARRAY) {
					// This is an array - treat each element in the array as
					// if it was this node (i.e. it matches the required name).
					// The first element will be the node directly after the array node.
					int elementNodeNum = childNodeNum + 1;
					for ( ; ; ) {
						if (elementNodeNum < 0)
							return cntMatches;
						FastJsonBlockOfNodes elementBlock = firstBlock.getBlock(elementNodeNum, false);
						int elementIndex = elementNodeNum % FastJsonBlockOfNodes.SIZE;

						// Have we matched all the parts in the path?
						if (level == parts.length - 1)
						{
							// Match all parts of the path so accept this element/node.
							if (occurrance < 0)
								list.addNode(elementNodeNum);
							else if (occurrance == cntMatches)
							{
								list.addNode(elementNodeNum);
								return -1;
							}
							cntMatches++;
						} else {
							// No, we have more levels in the path. See if the children of
							// this element in the array match those parts of the path.
							cntMatches = findNodes(elementNodeNum+1, requiredIndent+2, parts, level+1, cntMatches, list, occurrance);
							if (cntMatches < 0)
								return -1; // already found the required occurance			
						}

						// Move on to the next element in the array
						elementNodeNum = elementBlock.nextNodeAtThisLevel[elementIndex]; 
					}
					
				} else if (type == TYPE_OBJECT) {
					// This is an object.
					// Have we matched all the parts in the path?
					if (level == parts.length - 1)
					{
						// Yes, we've matched all parts of the path so accept this element/node.
						if (occurrance < 0)
							list.addNode(childNodeNum);
						else if (occurrance == cntMatches)
						{
							list.addNode(childNodeNum);
							return -1;
						}
						cntMatches++;
					} else {
						// No, we have more levels in the path. See if the children of
						// this element in the array match those parts of the path.
						cntMatches = findNodes(childNodeNum+1, requiredIndent+1, parts, level+1, cntMatches, list, occurrance);
						if (cntMatches < 0)
							return -1; // already found the required occurance			
					}
				} else {
					// This is a primitive data type. We can't go down any levels deeper.
					// Have we matched all the parts in the path?
					if (level == parts.length - 1)
					{
						// Yes, we've matched all parts of the path so accept this element/node.
						if (occurrance < 0)
							list.addNode(childNodeNum);
						else if (occurrance == cntMatches)
						{
							list.addNode(childNodeNum);
							return -1;
						}
						cntMatches++;
					} else {
						// No, we have more levels in the path, but there are no properties
						// within this property, so we can't match them.
//						int abc = 123;
					}
				}

				
//				// Have we matched everything?
//				if (level == parts.length - 1)
//				{
//						if (occurrance < 0)
//							list.addNode(childNodeNum);
//						else if (occurrance == cntMatches)
//						{
//							list.addNode(childNodeNum);
//							return -1;
//						}
//						cntMatches++;
//				} else {
//
//					// Still more parts to match - only possible if this node is an array or an object
//					if (type == TYPE_OBJECT) {
//						
//						// Is an object - the properties are at the next indent level
//						cntMatches = findNodes(childNodeNum+1, requiredIndent+1, parts, level+1, cntMatches, list, occurrance);
//						if (cntMatches < 0)
//							return -1; // already found the required occurance
//	
//	//					cntMatches = findNodes_checkNextLevel(childNodeNum, requiredIndent + 1, parts, level, cntMatches, list, occurrance);
//	//					if (cntMatches < 0)
//	//						return cntMatches; // already found the required occurance
//						
//						
//					} else if (type == TYPE_ARRAY) {
//						// Check each element in the array, as they all have the same name.
//						// The first element will be the node directly after the array node.
//						int elementNodeNum = childNodeNum + 1;
//						for ( ; ; ) {
//							FastJsonBlockOfNodes elementBlock = firstBlock.getBlock(elementNodeNum, false);
//							int elementIndex = elementNodeNum % FastJsonBlockOfNodes.SIZE;
//							
//							// Check the children of this element in the array
//							cntMatches = findNodes(elementNodeNum+1, requiredIndent+2, parts, level+1, cntMatches, list, occurrance);
//							if (cntMatches < 0)
//								return -1; // already found the required occurance			
//							
//							// Move on to the next element in the array
//							elementNodeNum = elementBlock.nextNodeAtThisLevel[elementIndex]; 
//						}
//					} else {
//						// No match here
//					}
//				}
			}
			
			// Move on to the next child
			childNodeNum = block.nextNodeAtThisLevel[index];
		}
	}

	private boolean nodeNameMatchesPartOfXpath(int offsetOfNodeName, String partOfXpath) {
//String zzzName = new String(json, offsetOfNodeName, 10);
		// The node has no name
		if (offsetOfNodeName < 0)
			return false;
		
		// The part matches any name
		if (partOfXpath.equals("*"))
			return true;
		
		// Check that the part name matches the node name.
		// The node name is terminated by double quote.
		char[] partName = partOfXpath.toCharArray();
		for (int i = 0; ; i++) {
			// See if we're at the end of the part name, or the node name
			char nameChar = json[offsetOfNodeName + i];
			boolean endOfNodeName = (nameChar == '"');
			boolean endOfPartName= (i == partName.length);
			
			if ( !endOfNodeName && !endOfPartName) {
				// We're not at the end of either the xpath part or the node name.
				// Compare the next character
				char partChar = partName[i];
				if (partChar != nameChar)
					return false; // different names
			} else {
				// At the end of something.
				boolean endOfBoth = (endOfNodeName && endOfPartName);
				return endOfBoth;
			}
		}
	}

	protected String[] splitXPath(String xpath)
	{
		// Ignore any initial '/'
		if (xpath.startsWith("/"))
			xpath = xpath.substring(1);

		// Ignore any initial './'
		if (xpath.startsWith("./"))
			xpath = xpath.substring(2);
		
		Vector<String> list = new Vector<String>();
		for ( ; ; )
		{
			int pos = xpath.indexOf("/");
			if (pos < 0)
			{
				list.add(xpath);
				break;
			}
			String part = xpath.substring(0, pos);
			list.add(part);
			xpath = xpath.substring(pos + 1);
		}
		String arr[] = new String[list.size()];
		Enumeration<String> enumx = list.elements();
		for (int i = 0; enumx.hasMoreElements(); i++)
		{
			String part = (String) enumx.nextElement();
			arr[i] = part;
		}
		return arr;
	}
	
	public String getXml()
	{
		return new String(this.json);
	}

	//--------------------------------------------------------------------------------------------------------------------
	// Methods for the XSelectable interface

	public String string(String xpath) throws X2DataException {
		try {
			return getText(xpath);
		} catch (Exception e) {
			X2DataException exception = new X2DataException(e.getMessage());
			exception.setStackTrace(e.getStackTrace());
			throw exception;
		}
	}


	//--------------------------------------------------------------------------------------------------------------------
	// Return the number of records that can be iterated over.

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
	public void first() {
		beenToFirst = false;
	}

	/**
	 * This data type does not provide a list of records, so this method only returns true until {@link #next()} is called.<p> 
	 * To iterate over elements within this object, use {@link #select(String)} or one of the {@link #foreach(String)} methods.
	 */
	public boolean hasNext() {
		if (beenToFirst)
			return false;
		return true;
	}

	/**
	 * This data type does not provide a list of records, so this method only returns true one time. Actually it serves
	 * no purpose other than to allow iterators to access the data that can be accessed directly. For example,
	 * <pre>
	 * FastXml data = ...;
	 * String value = data.string("./name");
	 * </pre>
	 * will return exactly the same as:
	 * <pre>
	 * FastXml data = ...;
	 * while (data.next()) {
	 *   String value = data.string("./name");
	 * }
	 * </pre>
	 * <p>
	 * To iterate over a list of elements <i>within</i> this data object, use {@link #select(String)} or one of the {@link #foreach(String)} methods.
	 */
	public boolean next() {
		if (beenToFirst)
			return false;
		beenToFirst = true;
		return true;
	}

	
	//--------------------------------------------------------------------------------------------------------------------
	// Iterate over this object using a Java iterator
	
	public Iterator<XSelectable> iterator() {
		return new X2DataIterator(this);
	}

	
	
	//--------------------------------------------------------------------------------------------------------------------
	// Select elements within this data object

	public XSelectable select(String xpath) {
		return getNodes(xpath);
	}

	
	//--------------------------------------------------------------------------------------------------------------------
	// Select and iterate using a callback

	public void foreach(String xpath, XIteratorCallback callback) throws X2DataException {
		foreach(xpath, callback, null);
	}

	public void foreach(String xpath, Object userData, XIteratorCallback callback) throws X2DataException {
		try {
			XSelectable list = this.getNodes(xpath);
			for (int index = 0; list.hasNext(); index++) {
				list.next();
				callback.next(list, index, userData);
			}
		} catch (Exception e) {
			X2DataException exception = new X2DataException(e.getMessage());
			exception.setStackTrace(e.getStackTrace());
			throw exception;
		}
	}

	
	//--------------------------------------------------------------------------------------------------------------------
	// Select and iterate using a Java iterator
	
	public Iterable<XSelectable> foreach(String xpath) throws X2DataException {
		FastJsonNodes list = this.getNodes(xpath);
		return list;
	}

	public void list() {
		firstBlock.list(json, numNodes);
	}
}
