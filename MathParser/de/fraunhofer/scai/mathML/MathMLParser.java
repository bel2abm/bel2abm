
package de.fraunhofer.scai.mathML;

import java.io.*;
import java.util.Hashtable;

/**
 * @comment uses Tilman Walther's and Martin Wilke's substitution table idea in a modified way
 */
public class MathMLParser {
	
	protected final String SUBSTITUTIONS_FILE = "substitutions.txt";
	protected final String COMMENT_PREFIX = "**";
	
	/**
	 * The place holder for blocks in substitutions. If a substitution contains
	 * a block place holder it is replaced by the LaTeX representation of
	 * the followig block.<br>
	 * Syntax: PH_BLOCKSTART + blockNumber + PH_BLOCKEND, e.g. '#BLOCK1#'.
	 */
	protected final String PH_BLOCK_START = "%BLOCK";
	protected final char PH_BLOCK_END = '%';
	
	protected final char[] specialCharacters = {'%','#','_','$'};
	protected final char[] leftBraces  = {'(','{','['};
	protected final char[] rightBraces = {')','{',']'};
	
	protected Hashtable substitutions;
	protected StringBuffer result;
	protected StringBuffer strBuf;
	protected int pos;
	protected boolean wrappedEntities;
	protected boolean skipUnknownEntities;
	
	protected String nextTag;
	protected StringBuffer tagBuf = new StringBuffer(200);  
	protected StringBuffer entity = new StringBuffer(32); 
	protected StringBuffer entitySubst = new StringBuffer(32); 

	
	/**
	 * Generates the substitution table from the default file path in
	 * field SUBSTITUTIONS_FILE.
	 * 
	 * @throws FileNotFoundException if no file was found at the default position
	 * @throws IOException if an I/O error occurs
	 */
	public MathMLParser() throws IOException {
		substitutions = getSubstitutionTable(SUBSTITUTIONS_FILE);
	}
	
	
	/**
	 * Generates the substitution table from the given file path.
	 * 
	 * @param substitutionsFile the path of the text file with the substitution table.
	 * @throws FileNotFoundException if the file could not be found
	 * @throws IOException if an I/O error occurs
	 */
	public MathMLParser(String substitutionsFile) throws IOException {
		substitutions = getSubstitutionTable(substitutionsFile);
	}
	
	
	public String parse(StringBuffer strBuf, boolean wrappedEntities, boolean skipUnknownEntities) {
		if (strBuf != null) {
			this.strBuf = strBuf;
			this.wrappedEntities = wrappedEntities;
			this.skipUnknownEntities = skipUnknownEntities;
			
			result = new StringBuffer(strBuf.length());
	
			pos = 0;
			try {
				while (strBuf.indexOf("<", pos) != -1) {
					parseBlock(getNextTag());
					skipFollowingTag();
				}
				return result.toString();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			return null; 
		}
		return null;
	}
	
	
	 /* 
	 * @param areaEnd the end of the area to parse
	 * @return a StringBuffer with the LaTeX representation of the input
	 * @throws Exception if an error occurs while parsing
	 */
	void parseBlock(String startTag) throws Exception {
		
		String endTag = generateEndTag(startTag);
		int blockEnd = getBlockEnd(startTag, endTag);
		
		StringBuffer substBuf;
		String blockContent;
		boolean inside = true;
		
		int blockNumber = 0;
		int prevBlockNumber;
		
		while (pos <= blockEnd) {
			
			int i = pos;
			while ((i <= blockEnd) && (strBuf.charAt(i) != '<')) i++;
			
			if ((startTag != endTag) && (i > blockEnd)) {
				
				if (inside) {
					blockContent = strBuf.substring(pos, blockEnd+1);
					result.append(parseBlockContent(blockContent));
					pos = pos + blockContent.length();
					blockContent = null;
				}
				else {
					pos = blockEnd+1;
				}
			}
			else {
				inside = false;
				
				if ((substBuf = (StringBuffer) substitutions.get(startTag)) != null) {
					
					int phIndex;
					int substIndex = 0;
					
					while ( ((phIndex = substBuf.indexOf(PH_BLOCK_START, substIndex)) > -1) && (pos < blockEnd)) { // blockEnd-1 ??
						
						while (substIndex < phIndex) {
							result.append(substBuf.charAt(substIndex));
							substIndex++;
						}
						substIndex += PH_BLOCK_START.length();
						
						int blockNumberIndex = substIndex;
						while (substBuf.charAt(substIndex) != PH_BLOCK_END) {
							substIndex++;
						}
						
						prevBlockNumber = blockNumber;
						
						String blockNumberStr = substBuf.substring(blockNumberIndex, substIndex);
						
						if (blockNumberStr.equals("S")) {
							
							substIndex++;
							
							skipBlocks((1 - prevBlockNumber) - 1);
													
							while ((strBuf.substring(pos, blockEnd+1)).indexOf('<') != -1) {
								nextTag = getNextTag();
								parseBlock(nextTag);
								skipFollowingTag();
							}
						}
						else {
							
							try {
								blockNumber = Integer.parseInt(blockNumberStr);
							}
							catch (NumberFormatException nfe) {
								throw new Exception("Parsing error at character "+pos+": Unparseable block number in substitution.");
							}
						
							substIndex++;
							
							skipBlocks((blockNumber - prevBlockNumber) - 1);
													
							nextTag = getNextTag();
							parseBlock(nextTag);
							
							skipFollowingTag();
						}
					}
					
					while (substIndex < substBuf.length()) {
						result.append(substBuf.charAt(substIndex));
						substIndex++;
					}
					
					pos = blockEnd + endTag.length();
				}
				else {
					
					while ((strBuf.substring(pos, blockEnd+1)).indexOf('<') != -1) {
						nextTag = getNextTag();
						parseBlock(nextTag);
						skipFollowingTag();
					}
				}
			}
		}
		pos = blockEnd;
	}
	

	/**
	 * Jumps to the next tag, reads it into 'startTag' an generates the corresponding 'endTag'.
	 */
	String getNextTag() {
		
		while (strBuf.charAt(pos) != '<') {
			pos++;
		}

		tagBuf.setLength(0);
					
		while (strBuf.charAt(pos) != '>') {
			tagBuf.append(strBuf.charAt(pos));
			pos++;
		}
		pos++;
		tagBuf.append('>');
		
		return tagBuf.toString();
	}
	
	
	/**
	 * Generates an end tag corresponding to the given 'startTag'.
	 * 
	 * @param startTag the start tag to generate an end tag from
	 * @return the end tag for the given start tag
	 */
	String generateEndTag(String startTag) {
		
		if (startTag.charAt(tagBuf.length()-2) != '/') {
			
			if (startTag.indexOf(' ') > -1) {
				return new String("</"+startTag.substring(1, startTag.indexOf(' '))+">");
			}
			else {
				return new String("</"+startTag.substring(1, startTag.length()));
			}
		}
		else {
			return startTag;
		}
	}
	
	
	/**
	 * Skips all characters up to the end of the next tag.
	 */
	void skipFollowingTag() {
		while (strBuf.charAt(pos) != '>') {
			pos++;
		}
		pos++;
	}
	
	
	/**
	 * Skips (back and forth) a given number of blocks from the actual position.
	 * 
	 * @param blocksToSkip the number of blocks to skip
	 */
	void skipBlocks(int blocksToSkip) {
		
		if (blocksToSkip > 0) {
			for (int i = 0; i < blocksToSkip; i++) {
				
				String startTag = getNextTag();
				String endTag = generateEndTag(startTag);
				
				pos = getBlockEnd(startTag, endTag);
				
				if (endTag != null) {
					pos = pos + endTag.length();
				}
				else {
					pos = pos + startTag.length();
				}
			}
		}
		else if (blocksToSkip < 0) {
			
			for (int i = 0; i > blocksToSkip; i--) {
				
				int subBlocks = 1;
	
				while (strBuf.charAt(pos) != '>') {
					pos--;
				}
		
				tagBuf.setLength(0);
								
				while (strBuf.charAt(pos) != '<') {
					tagBuf.append(strBuf.charAt(pos));
					pos--;				
				}
				tagBuf.append('<');
				
				tagBuf.reverse();
				String blockEndTag = new String(tagBuf);
				String blockStartTag = new String(tagBuf.deleteCharAt(1));
	
				do {
					while (strBuf.charAt(pos) != '>') {
						pos--;
					}
		
					tagBuf.setLength(0);
								
					while (strBuf.charAt(pos) != '<') {
						tagBuf.append(strBuf.charAt(pos));
						pos--;				
					}
					tagBuf.append('<');
					tagBuf.reverse();
								
					if (tagBuf.indexOf(" ") > -1) tagBuf.delete(tagBuf.indexOf(" "), tagBuf.length()-1);
					
					if (tagBuf.toString().equals(blockStartTag)) {
						subBlocks--;
					}
					else {
						if (tagBuf.toString().equals(blockEndTag)) {
							subBlocks++;
						}
					}
				} while ( (subBlocks > 0) || (!(tagBuf.toString().equals(blockStartTag))) );
			}
		}
	}
	
	
	/**
	 * Returns the end index of the block defined by the 'startTag' parameter skipping
	 * all subblocks. The end index is the position of the character before the closing
	 * tag of the block.
	 * 
	 * @param startTag the tag that opened the block
	 * @param endTag the end tag to seek
	 * @return the index of the closing tag
	 */
	int getBlockEnd(String startTag, String endTag) {
		
		if (startTag != endTag) {
			
			int pos2 = pos;
			int subBlocks = 1;
			
			if (startTag.indexOf(' ') > -1) {
				startTag = startTag.substring(0, startTag.indexOf(' ')) + '>';
			}
			
			do {
				while (strBuf.charAt(pos2) != '<') {
					pos2++;
				}
	
				tagBuf.setLength(0);
							
				while (strBuf.charAt(pos2) != '>') {
					tagBuf.append(strBuf.charAt(pos2));
					pos2++;				
				}
				tagBuf.append('>');
							
				if (tagBuf.toString().equals(endTag)) {
					subBlocks--;
				}
				else { 
					if (tagBuf.indexOf(" ") > -1) tagBuf.delete(tagBuf.indexOf(" "), tagBuf.length()-1);
								
					if (tagBuf.toString().equals(startTag)) {
						subBlocks++;
					}
				}
			} while ( (subBlocks > 0) || (!(tagBuf.toString().equals(endTag))) );
			
			return (pos2 - endTag.length());
		}
		
		return pos - startTag.length();
	}
	
	
	/**
	 * Parses a String into Latex syntax and returns it.
	 * 
	 * @param s the string to parse
	 * @return the representation of the given string
	 * @throws Exception if HTML wrapped entities were expected but not found
	 */
	String parseBlockContent(String s) throws Exception {
		
		int sbIndex = 0;
		StringBuffer sb = new StringBuffer(s);
		
		while ((sbIndex = sb.indexOf("\\", sbIndex)) > -1) {
			sb.insert(sbIndex+1, "backslash");
			sbIndex = sbIndex + 10;
		}
		
		// replace braces
		for (int i = 0; i < leftBraces.length; i++) {
			sbIndex = 0;
			while ((sbIndex = sb.indexOf(String.valueOf(leftBraces[i]), sbIndex)) > -1) {
				sb.insert(sbIndex, "\\left");
				sbIndex = sbIndex + 6;
			}
		}
		
		for (int i = 0; i < rightBraces.length; i++) {
			sbIndex = 0;
			while ((sbIndex = sb.indexOf(String.valueOf(rightBraces[i]), sbIndex)) > -1) {
				sb.insert(sbIndex, "\\right");
				sbIndex = sbIndex + 7;
			}
		}
		
		
		// replace special characters
		for (int i = 0; i < specialCharacters.length; i++) {
			sbIndex = 0;
			while ((sbIndex = sb.indexOf(String.valueOf(specialCharacters[i]), sbIndex)) > -1) {
				sb.insert(sbIndex, '\\');
				sbIndex = sbIndex + 2;
			}
		}
		
		// replace Entities
		sbIndex = 0;
		while ((sbIndex = sb.indexOf("&", sbIndex)) > -1) {
			
			entity.setLength(0);
			
			while (sb.charAt(sbIndex) != ';') {
				entity.append(sb.charAt(sbIndex));
				sbIndex++;
			}
			entity.append(';');
			sbIndex++;
			
			if (wrappedEntities && entity.toString().equals("&amp;")) {
				
				sb.delete(sbIndex - 4, sbIndex);
				sbIndex = sbIndex - 5;
				
				entity.setLength(0);
				
				try {
					while (sb.charAt(sbIndex) != ';') {
						entity.append(sb.charAt(sbIndex));
						sbIndex++;
					}
				}
				catch (StringIndexOutOfBoundsException sioobe) {
					throw new Exception("Parsing error at character "+pos+": MathML code is not HTML wrapped.");
				}
				
				entity.append(';');
				sbIndex++;
			}
			
			if ((entitySubst = (StringBuffer) substitutions.get(entity.toString())) != null) {
				sb.delete(sbIndex - entity.length(), sbIndex);
				sbIndex = sbIndex - entity.length();
				sb.insert(sbIndex, entitySubst);
				sbIndex = sbIndex + entitySubst.length();
				sb.insert(sbIndex, " ");
				sbIndex++;
			}
			else {
				if (skipUnknownEntities) {
					sb.delete(sbIndex - entity.length(), sbIndex);
					sbIndex = sbIndex - entity.length();
					sb.insert(sbIndex, " ");
					sbIndex++;
				}
				else {
					sb.insert(sbIndex - entity.length(), "NOTFOUND:'");
					sbIndex = sbIndex + 10;
					sb.insert(sbIndex, "' ");
					sbIndex = sbIndex + 2;
				}
			}
		}
		
		sbIndex = 0;
		while ((sbIndex = sb.indexOf("&", sbIndex)) > -1) {
			sb.insert(sbIndex, '\\');
			sbIndex = sbIndex + 2;
		}
		
		sbIndex = 0;
		while ((sbIndex = sb.indexOf("�", sbIndex)) > -1) {
			sb.replace(sbIndex, (sbIndex+1), "\\protect\"a");
			sbIndex = sbIndex + 10;
		}

		sbIndex = 0;
		while ((sbIndex = sb.indexOf("�", sbIndex)) > -1) {
			sb.replace(sbIndex, (sbIndex+1), "\\protect\"A");
			sbIndex = sbIndex + 10;
		}

		sbIndex = 0;
		while ((sbIndex = sb.indexOf("�", sbIndex)) > -1) {
			sb.replace(sbIndex, (sbIndex+1), "\\protect\"o");
			sbIndex = sbIndex + 10;
		}

		sbIndex = 0;
		while ((sbIndex = sb.indexOf("�", sbIndex)) > -1) {
			sb.replace(sbIndex, (sbIndex+1), "\\protect\"O");
			sbIndex = sbIndex + 10;
		}

		sbIndex = 0;
		while ((sbIndex = sb.indexOf("�", sbIndex)) > -1) {
			sb.replace(sbIndex, (sbIndex+1), "\\protect\"u");
			sbIndex = sbIndex + 10;
		}

		sbIndex = 0;
		while ((sbIndex = sb.indexOf("�", sbIndex)) > -1) {
			sb.replace(sbIndex, (sbIndex+1), "\\protect\"U");
			sbIndex = sbIndex + 10;
		}

		sbIndex = 0;
		while ((sbIndex = sb.indexOf("�", sbIndex)) > -1) {
			sb.replace(sbIndex, (sbIndex+1), "\\protect\"s");
			sbIndex = sbIndex + 10;
		}

		
		return sb.toString();
	}
	
	
	/**
	 * Parses the substitution table from the given file.
	 * 
	 * @param filePath the path of the substitutions file
	 * @return a hashtable containing all found substitutions 
	 * @throws IOException if an I/O error occurs
	 */
	Hashtable getSubstitutionTable(String fileName)
	throws IOException {
		
		BufferedReader file;
		
		try {
			file = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
		}
		catch (FileNotFoundException fnfe) {
			try {
				file = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(fileName)));
			}
			catch (NullPointerException npe) {
				throw new IOException("The substitutions file ("+fileName+") could not be read.");
			}
		}
	
		Hashtable substitutionTable = new Hashtable();
		int lineNo = 0;
		String line;
	
		String tmpStr;
		String[] subStrings;
	
		try
		{
			while ((line = file.readLine()) != null)
			{
				lineNo++;
			
				if ((!line.startsWith(COMMENT_PREFIX)) && (line.indexOf('\t') > -1)) {
					
					tmpStr = line.replaceAll("\t+", "\t");
					
					if (tmpStr.indexOf('\t') == tmpStr.lastIndexOf('\t')) {
						subStrings = tmpStr.split("\t");
						try {
							substitutionTable.put(subStrings[0], new StringBuffer(subStrings[1]));
						}
						catch (ArrayIndexOutOfBoundsException aioobe) {
							aioobe.printStackTrace();
							throw new IOException("Error in substitutions file ("+fileName+") at line "+lineNo);
						}
					}
				}
			}
		}
		catch (IOException ioe)
		{
			throw ioe;
		}
		finally
		{
			try
			{
				file.close();
			}
			catch (IOException ioe)
			{
				ioe.printStackTrace();				
			}
		}
		
		return substitutionTable;
	}
}
