//#preprocessor

//---------------------------------------------------------------------------------
//
//  Little Color Management System
//  Copyright (c) 1998-2010 Marti Maria Saguer
//
// Permission is hereby granted, free of charge, to any person obtaining 
// a copy of this software and associated documentation files (the "Software"), 
// to deal in the Software without restriction, including without limitation 
// the rights to use, copy, modify, merge, publish, distribute, sublicense, 
// and/or sell copies of the Software, and to permit persons to whom the Software 
// is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in 
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO 
// THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE 
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION 
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
//---------------------------------------------------------------------------------
//
package littlecms.internal;

import littlecms.internal.helper.Stream;
import littlecms.internal.helper.TextFormatting;
import littlecms.internal.helper.VirtualPointer;
import littlecms.internal.lcms2.cmsContext;

/**
 * IT8.7 / CGATS.17-200x handling
 */
//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmscgats
{
	private static final int MAXID     =  128; // Max lenght of identifier
	private static final int MAXSTR    = 1024; // Max lenght of string
	private static final int MAXTABLES =  255; // Max Number of tables in a single stream
	private static final int MAXINCLUDE=   20; // Max number of nested includes
	
	private static String DEFAULT_DBL_FORMAT = "%.10g"; // Double formatting
	
	//Originally had preprocessor for Windows and not Windows. This is not Windows so only include that option.
	private static final char DIR_CHAR = '/';
	
	// Symbols
	//enum SYMBOL
    public static final int SNONE = 0;
    public static final int SINUM = SNONE + 1;      // Integer
    public static final int SDNUM = SINUM + 1;      // Real
    public static final int SIDENT = SDNUM + 1;     // Identifier
    public static final int SSTRING = SIDENT + 1;   // string
    public static final int SCOMMENT = SSTRING + 1; // comment
    public static final int SEOLN = SCOMMENT + 1;   // End of line
    public static final int SEOF = SEOLN + 1;       // End of stream
    public static final int SSYNERROR = SEOF + 1;   // Syntax error found on stream
    
    // Keywords
    
    public static final int SBEGIN_DATA = SSYNERROR + 1;
    public static final int SBEGIN_DATA_FORMAT = SBEGIN_DATA + 1;
    public static final int SEND_DATA = SBEGIN_DATA_FORMAT + 1;
    public static final int SEND_DATA_FORMAT = SEND_DATA + 1;
    public static final int SKEYWORD = SEND_DATA_FORMAT + 1;
    public static final int SDATA_FORMAT_ID = SKEYWORD + 1;
    public static final int SINCLUDE = SDATA_FORMAT_ID + 1;
    
    // How to write the value
    //enum WRITEMODE
    public static final int WRITE_UNCOOKED = 0;
    public static final int WRITE_STRINGIFY = WRITE_UNCOOKED + 1;
    public static final int WRITE_HEXADECIMAL = WRITE_STRINGIFY + 1;
    public static final int WRITE_BINARY = WRITE_HEXADECIMAL + 1;
    public static final int WRITE_PAIR = WRITE_BINARY + 1;
    
    // Linked list of variable names
    private static class KEYVALUE
    {
    	public KEYVALUE Next;
    	public String   Keyword;       // Name of variable
        public KEYVALUE NextSubkey;    // If key is a dictionary, points to the next item
        public String   Subkey;        // If key is a dictionary, points to the subkey name
        public Object   Value;         // Points to value
        public int      WriteAs;       // How to write the value
    }
    
    // Linked list of memory chunks (Memory sink)
    private static class OWNEDMEM
    {
    	public OWNEDMEM Next;
    	public Object Ptr; // Point to value
    }
    
    // Suballocator
    private static class SUBALLOCATOR
    {
    	public VirtualPointer Block;
    	public int BlockSize;
        public int Used;
    }
    
    // Table. Each individual table can hold properties and rows & cols
    private static class TABLE
    {
    	public int            nSamples, nPatches;    // Cols, Rows
    	public int            SampleID;              // Pos of ID
        
    	public KEYVALUE[]      HeaderList;           // The properties
        
    	public VirtualPointer[] DataFormat;          // The binary stream descriptor
    	public VirtualPointer[] Data;                // The binary stream
    }
    
    // File stream being parsed
    private static class FILECTX
    {
    	public StringBuffer FileName; // File name if being readed from file
        public Stream Stream;         // File stream or NULL if holded in memory
        
        public FILECTX()
        {
        	FileName = new StringBuffer(lcms2.cmsMAX_PATH);
        }
    }
    
    // This struct hold all information about an open IT8 handler.
    private static class cmsIT8
    {
    	public char[] SheetType; // The first row of the IT8 (the type)
    	
    	public int TablesCount; // How many tables in this stream
        public int nTable; // The actual table
        
        public TABLE[] Tab;
        
        // Memory management
        public OWNEDMEM MemorySink; // The storage backend
        public SUBALLOCATOR Allocator; // String suballocator -- just to keep it fast
        
        // Parser state machine
        public int sy; // Current symbol
        public int ch; // Current character
        
        public int inum; // integer value
        public double dnum; // real value
        public char[] id; // identifier
        public char[] str; // string
        
        // Allowed keywords & datasets. They have visibility on whole stream
        public KEYVALUE[] ValidKeywords;
        public KEYVALUE[] ValidSampleID;
        
        public int Source; // Points to loc. being parsed
        public int lineno; // line counter for error reporting
        
        public FILECTX[] FileStack; // Stack of files being parsed
        public int IncludeSP; // Include Stack Pointer
        
        public VirtualPointer MemoryBlock; // The stream if holded in memory
        
        public char[] DoubleFormatter; // Printf-like 'cmsFloat64Number' formatter
        
        public cmsContext ContextID; // The threading context
    	
    	public cmsIT8()
    	{
    		SheetType = new char[MAXSTR];
    		
    		Tab = new TABLE[MAXTABLES];
    		
    		id = new char[MAXID];
    		str = new char[MAXSTR];
    		
    		FileStack = new FILECTX[MAXINCLUDE];
    		
    		DoubleFormatter = new char[MAXID];
    	}
    }
    
    // The stream for save operations
    private static class SAVESTREAM
    {
    	public Stream stream; // For save-to-file behaviour
    	
    	public VirtualPointer Base;
    	public VirtualPointer Ptr; // For save-to-mem behaviour
    	public int Used;
    	public int Max;
    }
    
    // ------------------------------------------------------ cmsIT8 parsing routines
    
    // A keyword
    private static class KEYWORD
    {
    	public final String id;
        public int sy;
        
        public KEYWORD(final String id, int sy)
        {
        	this.id = id;
        	this.sy = sy;
        }
    }
    
    // The keyword->symbol translation table. Sorting is required.
    private static final KEYWORD[] TabKeys = new KEYWORD[]{
		new KEYWORD("$INCLUDE", SINCLUDE), // This is an extension!
		new KEYWORD(".INCLUDE", SINCLUDE), // This is an extension!
		
		new KEYWORD("BEGIN_DATA", SBEGIN_DATA),
		new KEYWORD("BEGIN_DATA_FORMAT", SBEGIN_DATA_FORMAT),
		new KEYWORD("DATA_FORMAT_IDENTIFIER", SDATA_FORMAT_ID),
		new KEYWORD("END_DATA", SEND_DATA),
		new KEYWORD("END_DATA_FORMAT", SEND_DATA_FORMAT),
		new KEYWORD("KEYWORD", SKEYWORD)
	};
    
    private static final int NUMKEYS = 8;
    
    // Predefined properties
    
    // A property
    private static class PROPERTY
    {
    	public final String id; // The identifier
        public int as; // How is supposed to be written
        
        public PROPERTY(final String id, int as)
        {
        	this.id = id;
        	this.as = as;
        }
    }
    
    private static final PROPERTY[] PredefinedProperties = new PROPERTY[]{
		new PROPERTY("NUMBER_OF_FIELDS", WRITE_UNCOOKED),		// Required - NUMBER OF FIELDS
		new PROPERTY("NUMBER_OF_SETS", WRITE_UNCOOKED),			// Required - NUMBER OF SETS
		new PROPERTY("ORIGINATOR", WRITE_STRINGIFY),			// Required - Identifies the specific system, organization or individual that created the data file.
		new PROPERTY("FILE_DESCRIPTOR", WRITE_STRINGIFY),		// Required - Describes the purpose or contents of the data file.
		new PROPERTY("CREATED", WRITE_STRINGIFY),				// Required - Indicates date of creation of the data file.
		new PROPERTY("DESCRIPTOR", WRITE_STRINGIFY),			// Required  - Describes the purpose or contents of the data file.
		new PROPERTY("DIFFUSE_GEOMETRY", WRITE_STRINGIFY),		// The diffuse geometry used. Allowed values are "sphere" or "opal".
		new PROPERTY("MANUFACTURER", WRITE_STRINGIFY),
		new PROPERTY("MANUFACTURE", WRITE_STRINGIFY),			// Some broken Fuji targets does store this value
		new PROPERTY("PROD_DATE", WRITE_STRINGIFY),				// Identifies year and month of production of the target in the form yyyy:mm.
		new PROPERTY("SERIAL", WRITE_STRINGIFY),				// Uniquely identifies individual physical target.
		
		new PROPERTY("MATERIAL", WRITE_STRINGIFY),				// Identifies the material on which the target was produced using a code
																// uniquely identifying th e material. This is intend ed to be used for IT8.7
																// physical targets only (i.e . IT8.7/1 a nd IT8.7/2).
		
		new PROPERTY("INSTRUMENTATION", WRITE_STRINGIFY),		// Used to report the specific instrumentation used (manufacturer and
																// model number) to generate the data reported. This data will often
														        // provide more information about the particular data collected than an
														        // extensive list of specific details. This is particularly important for
														        // spectral data or data derived from spectrophotometry.
		
		new PROPERTY("MEASUREMENT_SOURCE", WRITE_STRINGIFY),	// Illumination used for spectral measurements. This data helps provide
																// a guide to the potential for issues of paper fluorescence, etc.
		
		new PROPERTY("PRINT_CONDITIONS", WRITE_STRINGIFY),		// Used to define the characteristics of the printed sheet being reported.
																// Where standard conditions have been defined (e.g., SWOP at nominal)
														        // named conditions may suffice. Otherwise, detailed information is
														        // needed.
		
		new PROPERTY("SAMPLE_BACKING", WRITE_STRINGIFY),		// Identifies the backing material used behind the sample during
																// measurement. Allowed values are “black”, “white”, or {"na".
		
		new PROPERTY("CHISQ_DOF", WRITE_STRINGIFY),				// Degrees of freedom associated with the Chi squared statistic
		
		// below properties are new in recent specs:
		
		new PROPERTY("MEASUREMENT_GEOMETRY", WRITE_STRINGIFY),	// The type of measurement, either reflection or transmission, should be indicated 
														        // along with details of the geometry and the aperture size and shape. For example, 
														        // for transmission measurements it is important to identify 0/diffuse, diffuse/0, 
														        // opal or integrating sphere, etc. For reflection it is important to identify 0/45, 
														        // 45/0, sphere (specular included or excluded), etc.
		
		new PROPERTY("FILTER", WRITE_STRINGIFY),				// Identifies the use of physical filter(s) during measurement. Typically used to 
        														// denote the use of filters such as none, D65, Red, Green or Blue.
		
		new PROPERTY("POLARIZATION", WRITE_STRINGIFY),			// Identifies the use of a physical polarization filter during measurement. Allowed 
        														// values are {"yes”, “white”, “none” or “na”.
		
		new PROPERTY("WEIGHTING_FUNCTION", WRITE_PAIR),			// Indicates such functions as: the CIE standard observer functions used in the 
														        // calculation of various data parameters (2 degree and 10 degree), CIE standard 
														        // illuminant functions used in the calculation of various data parameters (e.g., D50,
														        // D65, etc.), density status response, etc. If used there shall be at least one 
														        // name-value pair following the WEIGHTING_FUNCTION tag/keyword. The first attribute 
														        // in the set shall be {"name" and shall identify the particular parameter used.
														        // The second shall be {"value" and shall provide the value associated with that name. 
														        // For ASCII data, a string containing the Name and Value attribute pairs shall follow 
														        // the weighting function keyword. A semi-colon separates attribute pairs from each 
														        // other and within the attribute the name and value are separated by a comma.
		
		new PROPERTY("COMPUTATIONAL_PARAMETER", WRITE_PAIR),	// Parameter that is used in computing a value from measured data. Name is the name 
														        // of the calculation, parameter is the name of the parameter used in the calculation 
														        // and value is the value of the parameter.
		
		new PROPERTY("TARGET_TYPE", WRITE_STRINGIFY),			// The type of target being measured, e.g. IT8.7/1, IT8.7/3, user defined, etc.
		
		new PROPERTY("COLORANT", WRITE_STRINGIFY),				// Identifies the colorant(s) used in creating the target.
		
		new PROPERTY("TABLE_DESCRIPTOR", WRITE_STRINGIFY),		// Describes the purpose or contents of a data table.
		
		new PROPERTY("TABLE_NAME", WRITE_STRINGIFY)				// Provides a short name for a data table.
	};
    
    private static final int NUMPREDEFINEDPROPS = 26;
    
    // Predefined sample types on dataset
    private static final String[] PredefinedSampleID = {
        "SAMPLE_ID",      // Identifies sample that data represents
        "STRING",         // Identifies label, or other non-machine readable value. 
                          // Value must begin and end with a " symbol
        
        "CMYK_C",         // Cyan component of CMYK data expressed as a percentage
        "CMYK_M",         // Magenta component of CMYK data expressed as a percentage
        "CMYK_Y",         // Yellow component of CMYK data expressed as a percentage
        "CMYK_K",         // Black component of CMYK data expressed as a percentage
        "D_RED",          // Red filter density
        "D_GREEN",        // Green filter density
        "D_BLUE",         // Blue filter density
        "D_VIS",          // Visual filter density
        "D_MAJOR_FILTER", // Major filter d ensity
        "RGB_R",          // Red component of RGB data
        "RGB_G",          // Green component of RGB data
        "RGB_B",          // Blue com ponent of RGB data
        "SPECTRAL_NM",    // Wavelength of measurement expressed in nanometers
        "SPECTRAL_PCT",   // Percentage reflectance/transmittance
        "SPECTRAL_DEC",   // Reflectance/transmittance
        "XYZ_X",          // X component of tristimulus data
        "XYZ_Y",          // Y component of tristimulus data
        "XYZ_Z",          // Z component of tristimulus data
        "XYY_X",          // x component of chromaticity data
        "XYY_Y",          // y component of chromaticity data
        "XYY_CAPY",       // Y component of tristimulus data
        "LAB_L",          // L* component of Lab data
        "LAB_A",          // a* component of Lab data
        "LAB_B",          // b* component of Lab data
        "LAB_C",          // C*ab component of Lab data
        "LAB_H",          // hab component of Lab data
        "LAB_DE",         // CIE dE
        "LAB_DE_94",      // CIE dE using CIE 94
        "LAB_DE_CMC",     // dE using CMC
        "LAB_DE_2000",    // CIE dE using CIE DE 2000
        "MEAN_DE",        // Mean Delta E (LAB_DE) of samples compared to batch average
                          // (Used for data files for ANSI IT8.7/1 and IT8.7/2 targets)
        "STDEV_X",        // Standard deviation of X (tristimulus data)
        "STDEV_Y",        // Standard deviation of Y (tristimulus data)
        "STDEV_Z",        // Standard deviation of Z (tristimulus data)
        "STDEV_L",        // Standard deviation of L*
        "STDEV_A",        // Standard deviation of a*
        "STDEV_B",        // Standard deviation of b*
        "STDEV_DE",       // Standard deviation of CIE dE
        "CHI_SQD_PAR"};   // The average of the standard deviations of L*, a* and b*. It is
                          // used to derive an estimate of the chi-squared parameter which is
                          // recommended as the predictor of the variability of dE
    
    private static final int NUMPREDEFINEDSAMPLEID = 41;
    

    // Checks if c is a separator
    private static boolean isseparator(int c)
    {
            return (c == ' ') || (c == '\t') || (c == '\r');
    }
    
    // Checks whatever if c is a valid identifier char
    private static boolean ismiddle(int c)
    {
       return (!isseparator(c) && (c != '#') && (c !='\"') && (c != '\'') && (c > 32) && (c < 127));
    }
    
    // Checks whatsever if c is a valid identifier middle char.
    private static boolean isidchar(int c)
    {
       return isalnum(c) || ismiddle(c);
    }
    
    private static boolean isalnum(int c)
    {
    	return Character.isDigit((char)c) || (((c >= 'a') || (c <= 'z')) || ((c >= 'A') || (c <= 'Z')));
    }
    
    // Checks whatsever if c is a valid identifier first char.
    private static boolean isfirstidchar(int c)
    {
         return !isdigit(c) && ismiddle(c);
    }
    
    private static boolean isdigit(int c)
    {
    	return Character.isDigit((char)c);
    }
    
    // Guess whether the supplied path looks like an absolute path
    private static boolean isabsolutepath(final String path)
    {
    	//TODO: This will not work on BlackBerry, redo
    	
        char[] ThreeChars = new char[3];
        
        if(path == null)
        {
        	return false;
        }
        if (path.length() == 0 || path.charAt(0) == 0)
        {
        	return false;
        }
        
        path.getChars(0, 3, ThreeChars, 0);
        
        return ThreeChars[0] == DIR_CHAR;
    }
    
    // Makes a file path based on a given reference path
    // NOTE: this function doesn't check if the path exists or even if it's legal
    private static boolean BuildAbsolutePath(final String relPath, final String basePath, StringBuffer buffer, int MaxLen)
    {
    	//XXX Make sure this works proberly
        int len;
        char[] temp = new char[MaxLen - 1];
        
        // Already absolute?
        if (isabsolutepath(relPath))
        {
        	relPath.getChars(0, MaxLen - 1, temp, 0);
        	buffer.append(temp);
            return true;
        }
        
        // No, search for last
        basePath.getChars(0, MaxLen - 1, temp, 0);
        buffer.append(temp);
        
        len = buffer.toString().lastIndexOf(DIR_CHAR);
        if (len == -1)
        {
        	return false; // Is not absolute and has no separators??
        }
        
        if (len >= MaxLen)
        {
        	return false;
        }
        
        // No need to assure zero terminator over here
        len = MaxLen - len;
        relPath.getChars(0, len, temp, 0);
        buffer.append(temp, 0, len);
        
        return true;
    }
    
    // Make sure no exploit is being even tried
    private static String NoMeta(final String str)
    {
        if (str.indexOf('%') != -1)
        {
        	return "**** CORRUPTED FORMAT STRING ***";
        }
        return str;
    }
    
    // Syntax error
    private static boolean SynError(cmsIT8 it8, final String Txt, Object[] args)
    {
    	StringBuffer Buffer = new StringBuffer(new String(new char[256]));
    	StringBuffer ErrMsg = new StringBuffer(new String(new char[1024]));
        
        TextFormatting.vsnprintf(Buffer, 255, Txt, args);
        
        TextFormatting.vsnprintf(ErrMsg, 1023, "%s: Line %d, %s", new Object[]{it8.FileStack[it8.IncludeSP].FileName, new Integer(it8.lineno), Buffer});
        it8.sy = SSYNERROR;
        cmserr.cmsSignalError(it8.ContextID, lcms2.cmsERROR_CORRUPTION_DETECTED, "%s", new Object[]{ErrMsg});
        return false;
    }
    
    // Check if current symbol is same as specified. issue an error else.
    private static boolean Check(cmsIT8 it8, int sy, final String Err)
    {
    	if (it8.sy != sy)
    	{
    		return SynError(it8, NoMeta(Err), null);
    	}
    	return true;
    }
    
    //TODO #478
}
