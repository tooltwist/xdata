package com.tooltwist.fastXml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.tooltwist.xdata.XD;
import com.tooltwist.xdata.XDException;
import com.tooltwist.xdata.XDNumberFormatException;
import com.tooltwist.xdata.XIterator;
import com.tooltwist.xdata.XDCallback;
import com.tooltwist.xdata.XSelector;

/**
 * This class is used to provide high speed XML access. In particular, it avoids creating large numbers of XML objects, like the DOM parser. This parser does not provide comprehensive XPATH or DOM operations.
 * 
 * @author philipcallender
 * @see com.dinaa.XData
 * 
 */
public class FastXml implements XSelector, Iterable<XSelector> {
	public static final int ROOT_NODE = -1;

	private static final int TYPE_HEADER = 1; // <?xml ... ?>
	private static final int TYPE_COMMENT = 2; // <!-- ... -->
	private static final int TYPE_CDATA = 3; // <![CDATA[ ... ]]
	private static final int TYPE_TAG = 4; // <name ... > or <name ... />

	private FastXmlBlockOfNodes firstBlock = new FastXmlBlockOfNodes(0);
	private int numNodes = 0;

	private char xml[];
	private int offset;
	private int lineCnt;
	private int lineOffset;
	
	// A selection path, broken into segments.
	protected class PathSegments {
		String[] name;
		int[] index;
	};

	public FastXml(String xml) throws FastXmlException {
		init(xml.toCharArray());
	}

	public FastXml(char xml[]) throws FastXmlException {
		init(xml);
	}

	public FastXml(File file, boolean useUnicode) throws FastXmlException {
		String path = file.getAbsolutePath();
		long fileSize = file.length();
		char arr[] = new char[(int) fileSize];
		FileInputStream is = null;
		InputStreamReader in = null;
		try {
			is = new FileInputStream(path);
			if (useUnicode) {
				in = new InputStreamReader(is, "UTF-16");
				// in = new InputStreamReader(is, "UTF-8");
			} else
				in = new InputStreamReader(is);
			int num = in.read(arr);
			if (num != fileSize)
				throw new FastXmlException("Did not read entire file");
		} catch (IOException e) {
			System.err.println("Cannot load XML from file '" + path + "': " + e);
		} finally {
			try {
				if (in != null)
					in.close();
				if (is != null)
					is.close();
			} catch (IOException e) {
			}
		}

		init(arr);
	}

	void init(char xml[]) throws FastXmlException {
		this.xml = xml;
		this.offset = 0;
		this.lineCnt = 1;
		this.lineOffset = 0;

		try {
			parseContents(-1, 0);
		} catch (ArrayIndexOutOfBoundsException e) {
			syntax("Unexpected end of XML");
		}
	}

	private void parseContents(int nodeNum, int indent) throws FastXmlException {
		/*
		 * System.out.println("-----------------------------------------------"); System.out.println("parseContents("+offset+")"); System.out.println("offset:"+offset+", line: "+lineCnt); System.out.println(thisLine()+"\n"+positionMarker(offset)); System.out.println(new String(xml, offset, xml.length-offset));
		 */
		FastXmlBlockOfNodes block = firstBlock.getBlock(nodeNum, false);
		int index = nodeNum % FastXmlBlockOfNodes.SIZE;

		// Keep parsing the content and any tags within it, until we reach an end tag
		int previousNodeAtThisLevel = -1;
		for (;;) {
			if (offset >= xml.length) {
				if (nodeNum < 0)
					return;
				String name = block.name[index];
				syntax("Unexpected end of XML (tag " + name + " is incomplete)");
			}
			if (xml[offset] == '<') {
				int start = offset;
				offset++;
				skipSpaces();

				// See if this tag terminates itself ( i.e. <name..../> )
				if (xml[offset] == '/') {
					offset++;
					parseEndTag(nodeNum, start);
					return;
				}

				int newNodeNum = -1;
				if (xml[offset] == '?') {
					// This a special tag ( i.e. <?....?> )
					offset++;
					newNodeNum = parseEncodingTag(indent + 1, start);
				} else if (xml[offset] == '!') {
					boolean gotIt = false;
					try {
						offset++;
						if (xml[offset] == '-' && xml[offset + 1] == '-') {
							// This a comment ( <!-- .... --> )
							offset += 2;
							newNodeNum = parseComment(indent + 1, start);
							gotIt = true;
						} else if (xml[offset] == '[' && xml[offset + 1] == 'C' && xml[offset + 2] == 'D' && xml[offset + 3] == 'A' && xml[offset + 4] == 'T' && xml[offset + 5] == 'A' && xml[offset + 6] == '[') {
							// This a cdata ( <![CDATA[ .... ]]> )
							offset += 7;
							newNodeNum = parseCdata(indent + 1, start);
							gotIt = true;
						}
					} catch (ArrayIndexOutOfBoundsException e) {
					}
					if (!gotIt)
						syntax("Expected <!-- or <![CDATA[");
				} else {
					// This a new tag ( i.e. <name...> )
					newNodeNum = parseTag(indent + 1, start);
				}

				if (previousNodeAtThisLevel >= 0) {
					FastXmlBlockOfNodes prevBlock = firstBlock.getBlock(previousNodeAtThisLevel, false);
					int prevIndex = previousNodeAtThisLevel % FastXmlBlockOfNodes.SIZE;
					prevBlock.nextNodeAtThisLevel[prevIndex] = newNodeNum;
				}
				previousNodeAtThisLevel = newNodeNum;
				/*
				 * System.out.println("-----------------------------------------------"); System.out.println("parseContents("+offset+") continuing"); System.out.println("offset:"+offset+", line: "+lineCnt); System.out.println(thisLine()+"\n"+positionMarker(offset)); System.out.println(new String(xml, offset, xml.length-offset));
				 */
			} else if (xml[offset] == '&') {
				if (xml[offset + 1] == 'g' && xml[offset + 2] == 't' && xml[offset + 3] == ';') {
					// &gt;
					offset += 4;
				} else if (xml[offset + 1] == 'l' && xml[offset + 2] == 't' && xml[offset + 3] == ';') {
					// &lt;
					offset += 4;
				} else if (xml[offset + 1] == 'a' && xml[offset + 2] == 'm' && xml[offset + 3] == 'p' && xml[offset + 4] == ';') {
					// &amp;
					offset += 5;
				} else if (xml[offset + 1] == 'a' && xml[offset + 2] == 'p' && xml[offset + 3] == 'o' && xml[offset + 4] == 's' && xml[offset + 5] == ';') {
					// &apos;
					offset += 6;
				} else if (xml[offset + 1] == 'q' && xml[offset + 2] == 'u' && xml[offset + 3] == 'o' && xml[offset + 4] == 't' && xml[offset + 5] == ';') {
					// &quot;
					offset += 6;
				} else if (xml[offset + 1] == '#') {
					int i = 2;
					boolean isHex = false;
					if (xml[offset + i] == 'x') {
						i++; // hex
						isHex = true;
					}

					// Skip to the end of the number
					boolean isValid = true;
					for (; isValid; i++) {
						if (offset + i > xml.length) {
							isValid = false;
							break;
						}

						char c = xml[offset + i];
						if ((c >= '0' && c <= '9') // Decimal char
								|| isHex && ((c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f')) // Hex char
						) {
							// is a valid digit
						} else {
							if (c != ';')
								isValid = false;
							offset += i + 1;
							break;
						}

					}
					if (!isValid)
						syntax("Expected &#1239; or &#x0123f;");
				} else
					syntax("Expected one of &gt; &lt; &amp; &apos; or &quot;");
			} else {
				// Normal character
				if (xml[offset] == '\n') {
					lineCnt++;
					lineOffset = offset + 1;
				}
				offset++;
			}
		}
	}

	private int parseTag(int indent, int startTag) throws FastXmlException {
		String name = null;
		try {
			int nodeNum = numNodes++;
			FastXmlBlockOfNodes block = firstBlock.getBlock(nodeNum, true);
			int index = nodeNum % FastXmlBlockOfNodes.SIZE;
			/*
			 * System.out.println("-----------------------------------------------"); System.out.println("parseTag("+offset+")"); System.out.println("offset:"+offset+", line: "+lineCnt); System.out.println(thisLine()+"\n"+positionMarker(startTag)); System.out.println(new String(xml, offset, xml.length-offset));
			 */
			skipSpaces();
			name = parseName("tag name");

			for (;;) {
				skipSpaces();

				// See if we're at the end of a self-terminating tag: <name.../>
				if (xml[offset] == '/') {
					offset++;
					skipSpaces();
					if (xml[offset] != '>')
						syntax("Expected '>' after '/' in tag <" + name + "/>");
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
				if (xml[offset] == '>') {
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
				String attrib = parseName("attribute name in tag <" + name + " ...>");
				skipSpaces();
				if (xml[offset] != '=')
					syntax("Expected '=' after attribute name '" + attrib + "' in tag <" + name + " ...>");
				offset++;
				skipSpaces();
				if (xml[offset] == '"')
					skipString("Invalid value for attribute '" + attrib + "' in tag <" + name + " ...>", '"');
				else if (xml[offset] == '\'')
					skipString("Invalid value for attribute '" + attrib + "' in tag <" + name + " ...>", '\'');
				else
					syntax("Expected a value for attribute '" + attrib + "' in tag <" + name + " ...>");
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			if (name == null)
				syntax("Invalid start tag: unexpected end of XML");
			else
				syntax("Unterminated tag <" + name + "... >");
			return -1; // Cannot actually get here because syntax() always throws an exception
		}
	}

	private int parseEncodingTag(int indent, int startTag) throws FastXmlException {
		// Already read < and ?
		skipSpaces();
		String name = parseName("tag name");

		// Read attribute definitions, up to the end of the tag
		for (;;) {
			skipSpaces();
			if (xml[offset] == '?' && xml[offset + 1] == '>') {
				offset += 2;
				break;
			}

			// Skip over name="value"
			String attrib = parseName("attribute");
			skipSpaces();
			if (xml[offset] != '=')
				syntax("Expected '=' after attribute name '" + attrib + "' in tag <?" + name + " ...?>");
			offset++;
			skipSpaces();
			if (xml[offset] != '"')
				syntax("Expected a value for attribute '" + attrib + "' in tag <?" + name + " ...?>");
			skipString("Invalid value for attribute '" + attrib + "' in tag <?" + name + " ...?>", '"');
		}
		int nodeNum = numNodes++;
		FastXmlBlockOfNodes block = firstBlock.getBlock(nodeNum, true);
		int index = nodeNum % FastXmlBlockOfNodes.SIZE;

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

	private int parseComment(int indent, int startTag) throws FastXmlException {
		// Already read <!--
		for (;; offset++) {
			if (xml[offset] == '-' && xml[offset + 1] == '-' && xml[offset + 2] == '>') {
				offset += 3;
				break;
			}
			if (xml[offset] == '\n') {
				lineCnt++;
				lineOffset = offset + 1;
			}
		}
		int nodeNum = numNodes++;
		FastXmlBlockOfNodes block = firstBlock.getBlock(nodeNum, true);
		int index = nodeNum % FastXmlBlockOfNodes.SIZE;

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

	private int parseCdata(int indent, int startTag) {
		// Already read <![CDATA[
		int afterStartTag = offset;
		for (;; offset++) {
			if (xml[offset] == '\n') {
				lineCnt++;
				lineOffset = 0;
			} else if (xml[offset] == ']' && xml[offset + 1] == ']' && xml[offset + 2] == '>')
				break;
		}
		int endTag = offset;
		offset += 3;

		int nodeNum = numNodes++;
		FastXmlBlockOfNodes block = firstBlock.getBlock(nodeNum, true);
		int index = nodeNum % FastXmlBlockOfNodes.SIZE;

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

	private void parseEndTag(int nodeNum, int endTag) throws FastXmlException {
		/*
		 * System.out.println("-----------------------------------------------"); System.out.println("parseEndTag("+offset+")"); System.out.println("offset:"+offset+", line: "+lineCnt); System.out.println(thisLine()+"\n"+positionMarker(endTag)); System.out.println(new String(xml, offset, xml.length-offset));
		 */
		FastXmlBlockOfNodes block = firstBlock.getBlock(nodeNum, false);
		int index = nodeNum % FastXmlBlockOfNodes.SIZE;

		// The < and / have already been checked, so check here
		// that the end tag has the correct name and nothing else.
		skipSpaces();
		String name = parseName("end tag name");
		if (nodeNum < 0)
			syntax("Unmatched end tag </" + name + ">");
		if (!name.equals(block.name[index]))
			syntax("Tag <" + block.name[index] + "... > has mismatched end tag </" + name + ">");
		skipSpaces();
		if (xml[offset] != '>')
			syntax("Error in end tag </" + name + "...>");
		offset++;

		block.endTag[index] = endTag;
		block.afterEndTag[index] = offset;
	}

	private void skipSpaces() {
		for (; xml[offset] == ' ' || xml[offset] == '\t' || xml[offset] == '\n' || xml[offset] == '\r'; offset++)
			if (xml[offset] == '\n') {
				lineCnt++;
				lineOffset = offset + 1;
			}
	}

	private String parseName(String typeOfName) throws FastXmlException {
		// Check the first character is valid
		int start = offset;
		char c = xml[offset];
		if ((c != '_') && (c < 'a' || c > 'z') && (c < 'A' || c > 'Z'))
			syntax("Invalid " + typeOfName);

		// Read the rest of the name
		for (offset++;; offset++) {
			c = xml[offset];
			if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '/' || c == '>' || c == '=')
				return new String(xml, start, offset - start);
		}
	}

	private String skipString(String errorStr, char endChar) throws FastXmlException {
		StringBuffer value = new StringBuffer();
		if (xml[offset] != '"')
			syntax(errorStr);
		for (offset++;; offset++) {
			char c = xml[offset];
			if (c == endChar) {
				offset++;
				return value.toString();
			}
			if (c == '\\') {
				offset++;
				c = xml[offset];
				switch (c) {
				case '\n':
				case '\r':
					syntax(errorStr);
					break;
				case 'n':
					value.append('\n');
					break;
				case 'r':
					value.append('\r');
					break;
				case 't':
					value.append('\t');
					break;
				default:
					value.append(c);
					break;
				}
			} else
				value.append(c);
		}
	}

	void syntax(String error) throws FastXmlException {
		String str = "XML parse error on line " + lineCnt + ": " + error + "\n" + thisLine() + "\n" + positionMarker(offset);
		throw new FastXmlException(str);
	}

	private String thisLine() {
		// Find the end of the line
		int pos = lineOffset;
		for (; pos < xml.length && xml[pos] != '\n'; pos++)
			;
		return new String(xml, lineOffset, pos - lineOffset);
	}

	private String positionMarker(int errorPos) {
		String spacer = "";
		for (int pos = lineOffset; pos < errorPos; pos++)
			if (xml[pos] == '\t')
				spacer += "\t";
			else
				spacer += " ";
		spacer += "^";
		return spacer;
	}

	protected String getNameOfNode(int nodeNum) {
		FastXmlBlockOfNodes block = firstBlock.getBlock(nodeNum, false);
		int index = nodeNum % FastXmlBlockOfNodes.SIZE;
		return block.name[index];
	}

	protected String getValue(int nodeNum, boolean includeNestedNodeValues)// throws FastXmlException
	{
		StringBuffer buffer = new StringBuffer();
		getValue(buffer, nodeNum, includeNestedNodeValues);
		return buffer.toString();
	}

	private void getValue(StringBuffer buffer, int nodeNum, boolean includeNestedNodeValues) {
		FastXmlBlockOfNodes block = firstBlock.getBlock(nodeNum, false);
		int index = nodeNum % FastXmlBlockOfNodes.SIZE;

		int type = block.type[index];
		int indent = block.indent[index];
		int afterStartTag = block.afterStartTag[index];
		int endTag = block.endTag[index];
		if (type == TYPE_COMMENT || type == TYPE_HEADER)
			return;
		if (type == TYPE_CDATA) {
			buffer.append(xml, afterStartTag, endTag - afterStartTag);
			return;
		}

		// This is a normal tag
		int contentPos = afterStartTag;

		// See if there are any child nodes
		int childNodeNum = nodeNum + 1;
		while (childNodeNum > 0 && childNodeNum < numNodes) {
			FastXmlBlockOfNodes childBlock = firstBlock.getBlock(childNodeNum, false);
			int childIndex = childNodeNum % FastXmlBlockOfNodes.SIZE;

			int childType = childBlock.type[childIndex];
			int childIndent = childBlock.indent[childIndex];
			int childStartTag = childBlock.startTag[childIndex];
			int childAfterEndTag = childBlock.afterEndTag[childIndex];
			int nextChildNode = childBlock.nextNodeAtThisLevel[childIndex];
			if (childIndent < 0) {
				// Skip over the comment or special tag
				childNodeNum++;
				deEscapeAndAddToBuffer(buffer, contentPos, childStartTag);
				contentPos = childAfterEndTag;
				continue;
			}
			if (childIndent <= indent)
				break; // Should only be possible if there are no children

			// Add the content before this child to the value
			deEscapeAndAddToBuffer(buffer, contentPos, childStartTag);

			// Perhaps add the child's value to the buffer
			if (childType == TYPE_CDATA || (childType == TYPE_TAG && includeNestedNodeValues))
				getValue(buffer, childNodeNum, includeNestedNodeValues);
			contentPos = childAfterEndTag;

			// Look at the next child node
			childNodeNum = nextChildNode;
		}

		// Add the content after the last child
		deEscapeAndAddToBuffer(buffer, contentPos, endTag);
	}

	private void deEscapeAndAddToBuffer(StringBuffer value, int start, int end) {
		for (int offset = start; offset < end;) {
			if (xml[offset] == '&') {
				if (xml[offset + 1] == 'g' && xml[offset + 2] == 't' && xml[offset + 3] == ';') {
					// &gt;
					value.append('>');
					offset += 4;
					continue;
				} else if (xml[offset + 1] == 'l' && xml[offset + 2] == 't' && xml[offset + 3] == ';') {
					// &lt;
					value.append('<');
					offset += 4;
					continue;
				} else if (xml[offset + 1] == 'a' && xml[offset + 2] == 'm' && xml[offset + 3] == 'p' && xml[offset + 4] == ';') {
					// &amp;
					value.append('&');
					offset += 5;
					continue;
				} else if (xml[offset + 1] == 'a' && xml[offset + 2] == 'p' && xml[offset + 3] == 'o' && xml[offset + 4] == 's' && xml[offset + 5] == ';') {
					// &apos;
					value.append('\'');
					offset += 6;
					continue;
				} else if (xml[offset + 1] == 'q' && xml[offset + 2] == 'u' && xml[offset + 3] == 'o' && xml[offset + 4] == 't' && xml[offset + 5] == ';') {
					// &quot;
					value.append('"');
					offset += 6;
					continue;
				} else if (xml[offset + 1] == '#') {
					int num = 0;
					int i;
					boolean isValid = false;
					boolean isHex;
					if (xml[offset + 2] == 'x') {
						isHex = true;
						i = 3;
					} else {
						isHex = false;
						i = 2;
					}

					for (; isValid; i++) {
						if (offset + i >= xml.length) {
							isValid = false;
							break;
						}

						char c = xml[offset + i];
						if (c == ';') {
							if (i < 2 || (isHex && i < 3))
								isValid = false;
							break;
						} else if (c >= '0' && c <= '9') {
							num = (num * (isHex ? 16 : 10)) + (c - '0');
						} else if (isHex && (c >= 'a' && c <= 'f')) {
							num = num * 16 + (c - 'a' + 10);
						} else if (isHex && (c >= 'A' && c <= 'F')) {
							num = num * 16 + (c - 'A' + 10);
						} else {
							isValid = true;
							break;
						}
					}

					if (isValid) {
						int[] codePoints = { num };
						String string = new String(codePoints, 0, 1);
						value.append(string);
						offset += i + 1;
						continue;
					}
				}

				// Treat this escape sequence as regular characters
			}

			// Just accept the next character
			value.append(xml[offset]);
			offset++;
		}
	}

	public String getText(String xpath) throws FastXmlException {
		return getText(ROOT_NODE, xpath, 0);
	}

	public String getText(String xpath, int occurance) throws FastXmlException {
		return getText(ROOT_NODE, xpath, occurance);
	}

	public FastXmlNodes getNodes(String xpath) {
		return getNodes(ROOT_NODE, xpath);
	}

	public String getText(int parentNodeNum, String xpath)// throws FastXmlException
	{
		return getText(parentNodeNum, xpath, 0);
	}

	public String getText(int parentNodeNum, String xpath, int occurance)
	{
		if (xpath == ".")
			return getValue(parentNodeNum, true); // Need to remove escape sequences

		// Find the startpoint of the search
		int parentIndent = 0;
		int firstChildNodeNum = 0;
		if (parentNodeNum != ROOT_NODE) {
			FastXmlBlockOfNodes block = firstBlock.getBlock(parentNodeNum, false);
			int index = parentNodeNum % FastXmlBlockOfNodes.SIZE;
			parentIndent = block.indent[index];
			firstChildNodeNum = parentNodeNum + 1;
		}
		int childIndent = parentIndent + 1;

		// If the search starts with //, look anywhere below the start point
		FastXmlNodes list = new FastXmlNodes(this);
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
		if (list.hasNext()) {
			list.next();
			int nodeNum = list.getNodeNum();
			return getValue(nodeNum, true); // Need to remove escape sequences
		}
		return "";
	}

	public FastXmlNodes getNodes(int parentNodeNum, String xpath) {
		// Find the startpoint
		int parentIndent = 0;
		int firstChildNodeNum = 0;
		if (parentNodeNum != ROOT_NODE) {
			FastXmlBlockOfNodes block = firstBlock.getBlock(parentNodeNum, false);
			int index = parentNodeNum % FastXmlBlockOfNodes.SIZE;
			parentIndent = block.indent[index];
			firstChildNodeNum = parentNodeNum + 1;
		}
		int childIndent = parentIndent + 1;

		FastXmlNodes list = new FastXmlNodes(this);
		if (xpath.startsWith("//")) {
			// Look anywhere in the hierarchy
			xpath = xpath.substring(2);
			PathSegments parts = splitSelectionPathIntoSegments(xpath);
			if (parts == null)
				return null;
			findNodesAnywhereBelow(firstChildNodeNum, childIndent, parts, 0, list, -1);
		} else {
			// Look at the specific path
			PathSegments parts = splitSelectionPathIntoSegments(xpath);
			if (parts == null)
				return null;
			findNodes(firstChildNodeNum, childIndent, parts, 0, 0, list, -1);
		}
		return list;
	}

	/**
	 * Recursively look down through the tree for a match to the xpath. This is used to match XPaths starting with //, where the start point of the match can be anywhere.
	 * 
	 * @param childNodeNum
	 * @param requiredIndent
	 * @param parts
	 * @param cntMatches
	 * @param list
	 * @param occurrence
	 * @return
	 */
	private int findNodesAnywhereBelow(int childNodeNum, int requiredIndent, PathSegments parts, int cntMatches, FastXmlNodes list, int occurrence) {
		if (childNodeNum >= numNodes)
			return cntMatches;

		for (;;) {
			if (childNodeNum < 0)
				return cntMatches;
			FastXmlBlockOfNodes block = firstBlock.getBlock(childNodeNum, false);
			int index = childNodeNum % FastXmlBlockOfNodes.SIZE;

			int indent = block.indent[index];
			if (indent != requiredIndent)
				return cntMatches; // This will happen if the parent node has no children.

			// Look for a match at this node
			cntMatches = findNodes(childNodeNum, requiredIndent, parts, 0, cntMatches, list, occurrence);
			if (cntMatches < 0)
				return -1; // Found the occurrence we are after

			// Look for matches within the children
			cntMatches = findNodesAnywhereBelow(childNodeNum + 1, requiredIndent + 1, parts, cntMatches, list, occurrence);
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
	protected int findNodes(int childNodeNum, int requiredIndent, PathSegments segments, int segmentIndex, int totalMatches, FastXmlNodes list, int requiredOccurrence) {
		if (childNodeNum >= numNodes)
			return totalMatches;

		int cntNameMatch = -1;
		for ( ; ; ) {
			if (childNodeNum < 0)
				return totalMatches;
			FastXmlBlockOfNodes block = firstBlock.getBlock(childNodeNum, false);
			int index = childNodeNum % FastXmlBlockOfNodes.SIZE;

			int indent = block.indent[index];
			String name = block.name[index];
			if (indent != requiredIndent)
				return totalMatches;

			// See if the name matches the part of the XPATH at this level
			if (nameMatchesPathSegment(name, segments, segmentIndex))
			{
				// [1] The name matches.
				cntNameMatch++;
				
				// See if the index matches
				if (indexMatchesPathSegmentIndex(cntNameMatch, segments, segmentIndex)) {
					
					// [2] We have the right name and index
					// Have we matched all the parts in the path?
					if (haveMoreSegments(segments, segmentIndex)) {

						// [3] We have more path segments to check.
						// Check the children of this node against the next segment.
						totalMatches = findNodes(childNodeNum + 1, requiredIndent + 1, segments, segmentIndex + 1, totalMatches, list, requiredOccurrence);
						if (totalMatches < 0)
							return totalMatches; // already found the required occurance
	
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

			// Move on to the next child
			childNodeNum = block.nextNodeAtThisLevel[index];
		}
	}

	private boolean nameMatchesPathSegment(String name, PathSegments segments, int segmentIndex) {
		
		if (name == null)
			return false;

		// Get the required segment name
		String requiredName = segments.name[segmentIndex];

		// The path segment matches any name
		if (requiredName.equals("*"))
			return true;
		
		// See if the name matches the path segment
		if (name.equals(requiredName))
			return true;

		return false;
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

	public String getXml() {
		return new String(this.xml);
	}

	//--------------------------------------------------------------------------------------------------------------------
	// Methods for accessing data.
	
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
		if (string.trim().equals(""))
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

//	@Override
//	public String getString(String xpath, int occurance) throws FastXmlException {
//		return getText(ROOT_NODE, xpath, occurance);
//	}


	//--------------------------------------------------------------------------------------------------------------------
	// Iterate over this object using first, next.
	
	private boolean beenToFirst = false;

	public int size() {
		return 1;
	}

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
	 * This selector does not provide a list of records, so this method only returns true one time. Actually it serves
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
		FastXmlBlockOfNodes block = firstBlock.getBlock(0, false);
		return block.name[0];
	}

	
	//--------------------------------------------------------------------------------------------------------------------
	// Iterate over this object using a Java iterator
	
	public Iterator<XSelector> iterator() {
		return new XIterator(this);
	}
	
	
	//--------------------------------------------------------------------------------------------------------------------
	// Select elements within this data object

	public XSelector select(String xpath) {
		return getNodes(xpath);
	}

	
	//--------------------------------------------------------------------------------------------------------------------
	// Select and iterate using a callback

	public void foreach(String xpath, XDCallback callback) throws XDException {
		foreach(xpath, callback, null);
	}

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
	
	public Iterable<XSelector> foreach(String xpath) throws XDException {
		FastXmlNodes list = this.getNodes(xpath);
		return list;
	}

	// private void list()
	// {
	// firstBlock.list(numNodes);
	// }

}
