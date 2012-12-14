package com.dinaa.fastXml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * This class is used to provide high speed XML access. In particular, it avoids
 * creating large numbers of XML objects, like the DOM parser. This parser does
 * not provide comprehensive XPATH or DOM operations. If attempts to use complex
 * XPATHS are attempted, a FastXmlBeyondCapabilityException will be thrown.
 * 
 * @author philipcallender
 * @see com.dinaa.XData
 *
 */
public class FastXmlTester
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		String xml = ""
			+ "<xpc>\n"
			+ "<b>hi</b>\n"
			+ "<c/>\n"
			+ "  <d value=\"fred\">< e />\n"
			+ " < / d><f/></a><g/><h>dgd skha euih as sdjdje <i/></h>";
		xml = ""
		+ "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>\n"
		+ "<entity>\n"
		+ "    <tableName>co_company</tableName>\n"
		+ "    <longName>Company Master</longName>\n"
		+ "   <heading>Company Master</heading>\n"
		+ "   <headingAltLang>?????</headingAltLang>\n"
		+ "    <description>Company Master</description>\n"
		+ "   <attribute>\n"
		+ "<!-- hello there\n --> I<!-- haha\nhah --> Hope <!-- kajshd --> you are well\n"
		+ "        <sequence>1</sequence>\n"
		+ "<!-- hello there\n --> I<!-- haha\nhah --> Hope <!-- kajshd --> you are well\n"
		+ "        <name>companyNo</name>\n"
		+ "        <![CDATA[&<<<]]><uniqueKey>Y</uniqueKey>\n"
		+ "        <primaryKey>Y</primaryKey>\n"
		+ "        <description>Company &lt; &gt; &amp; &quot; &apos;number</description>\n"
		+ "        <heading>Company Number</heading>\n"
		+ "        <headingAltLang>????</headingAltLang>\n"
		+ "        <columnHeading>Company Number</columnHeading>\n"
		+ "        <searchDescription>companyNo</searchDescription>\n"
		+ "       <databaseColumn>company_no</databaseColumn>\n"
		+ "        <inSearchList>Y</inSearchList>\n"
		+ "        <searchable>Y</searchable>\n"
		+ "        <setValueMode>N</setValueMode>\n"
		+ "        <typeScopeType>P</typeScopeType>\n"
		+ "        <typeAttribute>INTEGER</typeAttribute>\n"
		+ "    </attribute>\n"
		+ "	    <attribute>\n"
		+ "        <sequence>2</sequence>\n"
		+ "        <name>name</name>\n"
		+ "        <columnLength>100</columnLength>\n"
		+ "        <description>Company <![CDATA[<<<>>>''\"\"&&&]]> name</description>\n"
		+ "        <heading>Company Name</heading>\n"
		+ "        <headingAltLang>???</headingAltLang>\n"
		+ "        <columnHeading>Company Name</columnHeading>\n"
		+ "        <searchDescription>name</searchDescription>\n"
		+ "        <databaseColumn>name</databaseColumn>\n"
		+ "        <inSearchList>Y</inSearchList>\n"
		+ "        <searchable>Y</searchable>\n"
		+ "        <fieldLength>100</fieldLength>\n"
		+ "        <isDescription>Y</isDescription>\n"
		+ "        <multiLanguage>Y</multiLanguage>\n"
		+ "        <setValueMode>N</setValueMode>\n"
		+ "        <typeScopeType>P</typeScopeType>\n"
		+ "        <typeAttribute>VARCHAR</typeAttribute>\n"
		+ "    </attribute>\n"
		+ "    <attribute>\n"
		+ "        <sequence>3</sequence>\n"
		+ "        <name>shortName</name>\n"
		+ "        <columnLength>20</columnLength>\n"
		+ "        <description>Company short name</description>\n"
		+ "        <heading>Company Short Name</heading>\n"
		+ "        <headingAltLang>????</headingAltLang>\n"
		+ "        <columnHeading>Company Short Name</columnHeading>\n"
		+ "        <searchDescription>shortName</searchDescription>\n"
		+ "        <databaseColumn>short_name</databaseColumn>\n"
		+ "        <searchable>Y</searchable>\n"
		+ "        <fieldLength>20</fieldLength>\n"
		+ "        <setValueMode>N</setValueMode>\n"
		+ "        <typeScopeType>P</typeScopeType>\n"
		+ "        <typeAttribute>VARCHAR</typeAttribute>\n"
		+ "    </attribute>\n"
		+ "	</entity>\n";

		
		try
		{
System.out.println("THIS IS GOOD STUFF");

			FastXml tester = new FastXml(xml);
//tester.list();
			FastXmlNodes list = tester.getNodes("/entity/attribute/description");
	
			for (list.first(); list.next(); )
			{
				int nodeNum = list.getNodeNum();
System.out.println("node in list is "+nodeNum);
			}
			
			System.out.println("Desc 0: "+tester.getText("/*/attribute/description", 0));
			System.out.println("Desc 1: "+tester.getText("/*/attribute/description", 1));
			System.out.println("Desc 2: "+tester.getText("/*/attribute/description", 2));
	
//		System.out.println("VALUE 5: "+tester.getRawValue(5));
//		System.out.println("VALUE 8: "+tester.getRawValue(8));
//		System.out.println("VALUE 12: "+tester.getRawValue(12));
//		System.out.println("VALUE 7: "+tester.getRawValue(7, true));

if (1==2)
{

			File dir = new File("/Users/philipcallender/home/itasmDevel/config_core/dataModel");
			File files[] = dir.listFiles();
			for (int i = 0; i < files.length; i++)
			{
				if ( !files[i].getName().startsWith("entity."))
					continue;

				if ((i % 50) == 0)
					System.out.println(""+i+": "+files[i].getName());
				else
					System.out.print(".");
				String path = files[i].getAbsolutePath();
				long l = files[i].length();
				char arr[] = new char[(int)l];
				FileInputStream is = null;
				InputStreamReader in = null;
				try {
					is = new FileInputStream(path);
					/*
					if (DataDictionary.USE_UNICODE)
					{
						in = new InputStreamReader(is, "UTF-16");
//							in = new InputStreamReader(is, "UTF-8");
					}
					else
					{
					*/
						in = new InputStreamReader(is);
					/*
					}
					*/
boolean finish = false;
 					in.read(arr);
					FastXml def = new FastXml(arr);
					String desc = def.getText("/entity/description");
//System.out.println(desc);
					FastXmlNodes nodeList = def.getNodes("/*/attribute");
					for (nodeList.first(); nodeList.next(); )
					{
						desc = nodeList.getText("description", 0);
//System.out.println("  "+desc);
//finish = true;
					}
if (finish)
	break;
//System.out.println(files[i].getName() + ": " + desc);
				} catch (IOException e) {
					System.err.println("Cannot find UiModule definition '"+path+"': " + e);
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
			}
}
		}
		catch (FastXmlException e)
		{
			System.err.println("Exception: " + e);
		}
	}

}