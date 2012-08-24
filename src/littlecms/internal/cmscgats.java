//#preprocessor

//---------------------------------------------------------------------------------
//
//  Little Color Management System
//  Copyright (c) 1998-2011 Marti Maria Saguer
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

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
//#ifndef BlackBerrySDK4.5.0
import net.rim.device.api.util.MathUtilities;
//#endif
import littlecms.internal.helper.Stream;
import littlecms.internal.helper.Utility;
import littlecms.internal.helper.VirtualPointer;
import littlecms.internal.lcms2.cmsContext;
import littlecms.internal.lcms2.cmsHANDLE;

/**
 * IT8.7 / CGATS.17-200x handling
 */
//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmscgats
{
	private static final int MAXID     =  128; // Max length of identifier
	private static final int MAXSTR    = 1024; // Max length of string
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
    	public KEYVALUE			Next;
    	public String			Keyword;	// Name of variable
        public KEYVALUE			NextSubkey;	// If key is a dictionary, points to the next item
        public String			Subkey;		// If key is a dictionary, points to the subkey name
        public VirtualPointer	Value;		// Points to value
        public int				WriteAs;	// How to write the value
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
    	public char[] SheetType;                     // The first row of the IT8 (the type)
    	
    	public int            nSamples, nPatches;    // Cols, Rows
    	public int            SampleID;              // Pos of ID
        
    	public KEYVALUE[]      HeaderList;           // The properties
        
    	public VirtualPointer[] DataFormat;          // The binary stream descriptor
    	public VirtualPointer[] Data;                // The binary stream
    	
    	public TABLE()
    	{
    		SheetType = new char[MAXSTR];
    	}
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
    private static class cmsIT8 implements cmsHANDLE
    {
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
        
        public VirtualPointer Source; // Points to loc. being parsed
        public int lineno; // line counter for error reporting
        
        public FILECTX[] FileStack; // Stack of files being parsed
        public int IncludeSP; // Include Stack Pointer
        
        public VirtualPointer MemoryBlock; // The stream if holded in memory
        
        public char[] DoubleFormatter; // Printf-like 'cmsFloat64Number' formatter
        
        public cmsContext ContextID; // The threading context
    	
    	public cmsIT8()
    	{
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
																// measurement. Allowed values are �black�, �white�, or {"na".
		
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
        														// values are {"yes�, �white�, �none� or �na�.
		
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
    	return Utility.isalnum(c) || ismiddle(c);
    }
    
    // Checks whatsever if c is a valid identifier first char.
    private static boolean isfirstidchar(int c)
    {
         return !Utility.isdigit(c) && ismiddle(c);
    }
    
    // Guess whether the supplied path looks like an absolute path
    private static boolean isabsolutepath(final String path)
    {
    	//This only expects file paths
    	
    	//First check general properties
    	if(path == null)
        {
        	return false;
        }
        if (path.length() == 0 || path.charAt(0) == 0)
        {
        	return false;
        }
        
        //Check if it has the proper protocol format for a file
        if(!path.startsWith("file://"))
        {
        	return false;
        }
        if(path.charAt(7) != '/')
        {
        	return false;
        }
        
        //Finally check that it isn't relative (this will throw an exception if relative)
        try
        {
        	Connection con = Connector.open(path, Connector.READ);
        	con.close();
        }
        catch(Exception e)
        {
        	return false;
        }
        return true;
    }
    
    // Makes a file path based on a given reference path
    // NOTE: this function doesn't check if the path exists or even if it's legal
    private static boolean BuildAbsolutePath(final String relPath, final String basePath, StringBuffer buffer, int MaxLen)
    {
        int len;
        char[] temp = new char[MaxLen - 1];
        
        // Already absolute?
        if (isabsolutepath(relPath))
        {
        	Utility.strncpy(buffer, relPath, MaxLen);
        	if(buffer.length() > MaxLen-1)
        	{
        		buffer.setCharAt(MaxLen-1, '\0');
        	}
            return true;
        }
        
        // No, search for last
        Utility.strncpy(buffer, basePath, MaxLen);
        if(buffer.length() > MaxLen-1)
    	{
    		buffer.setCharAt(MaxLen-1, '\0');
    	}
        
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
        Utility.strncpy(buffer, len + 1, relPath, 0, MaxLen - len);
        
        return true;
    }
    
    // Make sure no exploit is being even tried
    private static String NoMeta(final String str)
    {
        if (str.indexOf('%') != -1)
        {
        	return Utility.LCMS_Resources.getString(LCMSResource.CGATS_BAD_FORMAT_STR);
        }
        return str;
    }
    
    // Syntax error
    private static boolean SynError(cmsIT8 it8, final String Txt, Object[] args)
    {
    	StringBuffer Buffer = new StringBuffer(256);
    	StringBuffer ErrMsg = new StringBuffer(1024);
        
        Utility.vsnprintf(Buffer, 255, Txt, args);
        
        Utility.vsnprintf(ErrMsg, 1023, Utility.LCMS_Resources.getString(LCMSResource.CGATS_SynError_FORMAT), new Object[]{it8.FileStack[it8.IncludeSP].FileName, new Integer(it8.lineno), Buffer});
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
    
    // Read Next character from stream
    private static void NextCh(cmsIT8 it8)
    {
        if (it8.FileStack[it8.IncludeSP].Stream != null)
        {
            it8.ch = it8.FileStack[it8.IncludeSP].Stream.readByte();
            
            if (it8.FileStack[it8.IncludeSP].Stream.eof())
            {
                if (it8.IncludeSP > 0)
                {
                    it8.FileStack[it8.IncludeSP--].Stream.close();
                    it8.ch = ' '; // Whitespace to be ignored
                }
                else
                {
                	it8.ch = 0; // EOF
                }
            }
        }
        else
        {
            it8.ch = it8.Source.getProcessor().readInt8() & 0xFF;
            if (it8.ch != 0)
            {
            	it8.Source.movePosition(1);
            }
        }
    }
    
    // Try to see if current identifier is a keyword, if so return the referred symbol
    private static int BinSrchKey(final String id)
    {
        int l = 1;
        int r = NUMKEYS;
        int x, res;
        
        while (r >= l)
        {
            x = (l+r)/2;
            res = cmserr.cmsstrcasecmp(id, TabKeys[x-1].id);
            if (res == 0)
            {
            	return TabKeys[x-1].sy;
            }
            if (res < 0)
            {
            	r = x - 1;
            }
            else
            {
            	l = x + 1;
            }
        }
        
        return SNONE;
    }
    
    // 10 ^n
    private static double xpow10(int n)
    {
//#ifndef BlackBerrySDK4.5.0
    	return MathUtilities.pow(10, n);
//#else
    	return Utility.pow(10, n);
//#endif
    }
    
//  Reads a Real number, tries to follow from integer number
    private static void ReadReal(cmsIT8 it8, int inum)
    {
    	it8.dnum = inum;
    	
        while (Utility.isdigit(it8.ch))
        {
            it8.dnum = it8.dnum * 10.0 + (it8.ch - '0');
            NextCh(it8);
        }
        
        if (it8.ch == '.') // Decimal point
        {
            double frac = 0.0; // fraction
            int prec = 0; // precision
            
            NextCh(it8); // Eats dec. point
            
            while (Utility.isdigit(it8.ch))
            {
                frac = frac * 10.0 + (it8.ch - '0');
                prec++;
                NextCh(it8);
            }
            
            it8.dnum = it8.dnum + (frac / xpow10(prec));
        }
        
        // Exponent, example 34.00E+20
        if (Character.toUpperCase((char)it8.ch) == 'E')
        {
            int e;
            int sgn;
            
            NextCh(it8); sgn = 1;
            
            if (it8.ch == '-')
            {
                sgn = -1; NextCh(it8);
            }
            else
            {
                if (it8.ch == '+')
                {
                    sgn = +1;
                    NextCh(it8);
                }
                
                e = 0;
                while (Utility.isdigit(it8.ch))
                {
                    if (e * 10.0 < Integer.MAX_VALUE)
                    {
                    	e = e * 10 + (it8.ch - '0');
                    }
                    
                    NextCh(it8);
                }
                
                e = sgn*e;
                it8.dnum = it8.dnum * xpow10(e);
            }
        }
    }
    
    // Parses a float number
    // This can not call directly atof because it uses locale dependant
    // parsing, while CCMX files always use . as decimal separator
    private static double ParseFloatNumber(final String Buffer)
    {
    	double dnum = 0.0;
    	int sign = 1;
    	int BufferPtr = 0;
    	char[] BufferChars;
    	if(Buffer == null)
    	{
    		BufferChars = new char[]{'\0'};
    	}
    	else
    	{
    		if(Buffer.endsWith("\0"))
    		{
    			BufferChars = Buffer.toCharArray();
    		}
    		else
    		{
    			BufferChars = new char[Buffer.length()];
    			BufferChars[BufferChars.length - 1] = '\0';
    			Buffer.getChars(0, BufferChars.length, BufferChars, 0);
    		}
    	}
    	
    	if (BufferChars[BufferPtr] == '-' || BufferChars[BufferPtr] == '+')
    	{
    		sign = (BufferChars[BufferPtr] == '-') ? -1 : 1;
    		BufferPtr++;
    	}
    	
    	while (BufferChars[BufferPtr] != '\0' && Utility.isdigit((int)BufferChars[BufferPtr]))
    	{
    		dnum = dnum * 10.0 + (BufferChars[BufferPtr] - '0');
    		if (BufferChars[BufferPtr] != '\0')
    		{
    			BufferPtr++;
    		}
    	}
    	
    	if (BufferChars[BufferPtr] == '.')
    	{
    		double frac = 0.0; // fraction
    		int prec = 0;      // precission
    		
    		if (BufferChars[BufferPtr] != '\0')
    		{
    			BufferPtr++;
    		}
    		
    		while (BufferChars[BufferPtr] != '\0' && Utility.isdigit((int)BufferChars[BufferPtr]))
    		{
    			frac = frac * 10.0 + (BufferChars[BufferPtr] - '0');
    			prec++;
    			if (BufferChars[BufferPtr] != '\0')
    			{
    				BufferPtr++;
    			}
    		}
    		
    		dnum = dnum + (frac / xpow10(prec));
    	}
    	
    	// Exponent, example 34.00E+20
    	if (BufferChars[BufferPtr] != '\0' && Character.toUpperCase(BufferChars[BufferPtr]) == 'E')
    	{
    		int e;
    		int sgn;
    		
    		if (BufferChars[BufferPtr] != '\0')
    		{
    			BufferPtr++;
    		}
    		sgn = 1;
    		
    		if (BufferChars[BufferPtr] == '-')
    		{
    			sgn = -1;
    			if (BufferChars[BufferPtr] != '\0')
    			{
    				BufferPtr++;
    			}
    		}
    		else
    		{
    			if (BufferChars[BufferPtr] == '+')
    			{
    				sgn = +1;
    				if (BufferChars[BufferPtr] != '\0')
    				{
    					BufferPtr++;
    				}
    			}
    		}
    		
    		e = 0;
    		while (BufferChars[BufferPtr] != '\0' && Utility.isdigit((int)BufferChars[BufferPtr]))
    		{
    			if ((double)e * 10L < Integer.MAX_VALUE)
    			{
    				e = e * 10 + (BufferChars[BufferPtr] - '0');
    			}
    			
    			if (BufferChars[BufferPtr] != '\0')
    			{
    				BufferPtr++;
    			}
    		}
    		e = sgn * e;
    		dnum = dnum * xpow10(e);
    	}
    	
    	return sign * dnum;
    }
    
    // Reads next symbol
    private static void InSymbol(cmsIT8 it8)
    {
        char[] idptr = null;
        int idptrPos = 0;
        int k;
        int key;
        int sng;
        
        do
        {
            while (isseparator(it8.ch))
            {
            	NextCh(it8);
            }
            
            if (isfirstidchar(it8.ch)) // Identifier
            {
                k = 0;
                idptr = it8.id;
                idptrPos = 0;
                
                do
                {
                    if (++k < MAXID)
                    {
                    	idptr[idptrPos++] = (char)it8.ch;
                    }
                    
                    NextCh(it8);
                } while (isidchar(it8.ch));
                
                idptr[idptrPos] = '\0';
                
                key = BinSrchKey(Utility.cstringCreation(it8.id));
                if (key == SNONE)
                {
                	it8.sy = SIDENT;
                }
                else
                {
                	it8.sy = key;
                }
            }
            else // Is a number?
            {
                if (Utility.isdigit(it8.ch) || it8.ch == '.' || it8.ch == '-' || it8.ch == '+')
                {
                    int sign = 1;
                    
                    if (it8.ch == '-')
                    {
                        sign = -1;
                        NextCh(it8);
                    }
                    
                    it8.inum = 0;
                    it8.sy = SINUM;
                    
                    if (it8.ch == '0') // 0xnnnn (Hexa) or 0bnnnn (Binary)
                    {
                        NextCh(it8);
                        if (Character.toUpperCase((char)it8.ch) == 'X')
                        {
                            int j;
                            
                            NextCh(it8);
                            while (Utility.isxdigit(it8.ch))
                            {
                                it8.ch = Character.toUpperCase((char)it8.ch);
                                if (it8.ch >= 'A' && it8.ch <= 'F')
                                {
                                	j = it8.ch -'A'+10;
                                }
                                else
                                {
                                	j = it8.ch - '0';
                                }
                                
                                if (it8.inum * 16L > (long)Integer.MAX_VALUE)
                                {
                                    SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_BAD_HEX_NUM), null);
                                    return;
                                }
                                
                                it8.inum = it8.inum * 16 + j;
                                NextCh(it8);
                            }
                            return;
                        }
                        
                        if (Character.toUpperCase((char)it8.ch) == 'B') // Binary
                        {
                            int j;
                            
                            NextCh(it8);
                            while (it8.ch == '0' || it8.ch == '1')
                            {
                                j = it8.ch - '0';
                                
                                if (it8.inum * 2L > (long)Integer.MAX_VALUE)
                                {
                                    SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_BAD_BIN_NUM), null);
                                    return;
                                }
                                
                                it8.inum = it8.inum * 2 + j;
                                NextCh(it8);
                            }
                            return;
                        }
                    }
                    
                    while (Utility.isdigit(it8.ch))
                    {
                        if (it8.inum * 10L > (long)Integer.MAX_VALUE)
                        {
                            ReadReal(it8, it8.inum);
                            it8.sy = SDNUM;
                            it8.dnum *= sign;
                            return;
                        }
                        
                        it8.inum = it8.inum * 10 + (it8.ch - '0');
                        NextCh(it8);
                    }
                    
                    if (it8.ch == '.')
                    {
                        ReadReal(it8, it8.inum);
                        it8.sy = SDNUM;
                        it8.dnum *= sign;
                        return;
                    }
                    
                    it8.inum *= sign;
                    
                    // Special case. Numbers followed by letters are taken as identifiers
                    
                    if (isidchar(it8.ch))
                    {
                        if (it8.sy == SINUM)
                        {
                        	Utility.sprintf(it8.id, "%d", new Object[]{new Integer(it8.inum)});
                        }
                        else
                        {
                        	Utility.sprintf(it8.id, Utility.cstringCreation(it8.DoubleFormatter), new Object[]{new Double(it8.dnum)});
                        }
                        
                        idptrPos = k = (int)Utility.strlen(it8.id);
                        do
                        {
                            if (++k < MAXID)
                            {
                            	idptr[idptrPos++] = (char)it8.ch;
                            }
                            
                            NextCh(it8);
                        } while (isidchar(it8.ch));
                        
                        idptr[idptrPos] = '\0';
                        it8.sy = SIDENT;
                    }
                    return;
                }
                else
                {
                    switch (it8.ch)
                    {
			            // EOF marker -- ignore it
			            case 0x1A:
			                NextCh(it8);
			                break;
			            // Eof stream markers
			            case 0:
			            case -1:
			                it8.sy = SEOF;
			                break;
			            // Next line
			            case '\n':
			                NextCh(it8);
			                it8.sy = SEOLN;
			                it8.lineno++;
			                break;
			            // Comment
			            case '#':
			                NextCh(it8);
			                while (it8.ch != 0 && it8.ch != '\n')
			                {
			                	NextCh(it8);
			                }
			                
			                it8.sy = SCOMMENT;
			                break;
			            // String.
			            case '\'':
			            case '\"':
			                idptr = it8.str;
			                sng = it8.ch;
			                k = 0;
			                NextCh(it8);
			                
			                while (k < MAXSTR && it8.ch != sng)
			                {
			                    if (it8.ch == '\n'|| it8.ch == '\r')
			                    {
			                    	k = MAXSTR+1;
			                    }
			                    else
			                    {
			                        idptr[idptrPos++] = (char)it8.ch;
			                        NextCh(it8);
			                        k++;
			                    }
			                }
			                
			                it8.sy = SSTRING;
			                idptr[idptrPos] = '\0';
			                NextCh(it8);
			                break;
			            default:
			                SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_UNRECOGNIZED_CHAR), new Object[]{new Integer(it8.ch)});
			                return;
	                }
                }
            }
        } while (it8.sy == SCOMMENT);
        
        // Handle the include special token
        
        if (it8.sy == SINCLUDE)
        {
	        FILECTX FileNest;
	        
	        if(it8.IncludeSP >= (MAXINCLUDE-1))
	        {
	            SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_TO_MANY_RECUR), null);
	            return;
	        }
	        
	        InSymbol(it8);
	        if (!Check(it8, SSTRING, Utility.LCMS_Resources.getString(LCMSResource.CGATS_FILE_EXPECTED)))
	        {
	        	return;
	        }
	        
	        FileNest = it8.FileStack[it8.IncludeSP + 1];
	        if(FileNest == null)
	        {
	            FileNest = it8.FileStack[it8.IncludeSP + 1] = new FILECTX();
	            //if(FileNest == NULL)
	            //  TODO: how to manage out-of-memory conditions?
	        }
	        
	        if (BuildAbsolutePath(Utility.cstringCreation(it8.str), it8.FileStack[it8.IncludeSP].FileName.toString(), FileNest.FileName, lcms2.cmsMAX_PATH-1) == false)
	        {
	            SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_FILE_TOO_LONG), null);
	            return;
	        }
	        
	        FileNest.Stream = Stream.fopen(FileNest.FileName.toString(), "r");
	        if (FileNest.Stream == null)
	        {
                SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.FILE_NOT_FOUND), new Object[]{FileNest.FileName});
                return;
	        }
	        it8.IncludeSP++;
	        
	        it8.ch = ' ';
	        InSymbol(it8);
        }
    }
    
    // Checks end of line separator
    private static boolean CheckEOLN(cmsIT8 it8)
    {
        if (!Check(it8, SEOLN, Utility.LCMS_Resources.getString(LCMSResource.CGATS_EXPECTED_SEP)))
        {
        	return false;
        }
        while (it8.sy == SEOLN)
        {
        	InSymbol(it8);
        }
        return true;
    }
    
    // Skip a symbol
    
    private static void Skip(cmsIT8 it8, int sy)
    {
        if (it8.sy == sy && it8.sy != SEOF)
        {
        	InSymbol(it8);
        }
    }
    
    // Skip multiple EOLN
    private static void SkipEOLN(cmsIT8 it8)
    {
        while (it8.sy == SEOLN)
        {
        	InSymbol(it8);
        }
    }
    
    // Returns a string holding current value
    private static boolean GetVal(cmsIT8 it8, char[] Buffer, int max, final String ErrorTitle)
    {
        switch (it8.sy)
        {
	        case SIDENT:  Utility.strncpy(Buffer, it8.id, max);
	                      Buffer[max-1]=0;
	                      break;
	        case SINUM:   Utility.vsnprintf(Buffer, max, "%d", new Object[]{new Integer(it8.inum)}); break;
	        case SDNUM:   Utility.vsnprintf(Buffer, max, Utility.cstringCreation(it8.DoubleFormatter), new Object[]{new Double(it8.dnum)}); break;
	        case SSTRING: Utility.strncpy(Buffer, it8.str, max);
	                      Buffer[max-1] = 0;
	                      break;
	        default:
	             return SynError(it8, "%s", new Object[]{ErrorTitle});
        }
        
        Buffer[max] = 0;
        return true;
    }
    
    // ---------------------------------------------------------- Table
    
    private static TABLE GetTable(cmsIT8 it8)
    {
       if ((it8.nTable >= it8.TablesCount))
       {
    	   SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_TABLE_OUT_SEQ), new Object[]{new Integer(it8.nTable)});
    	   return it8.Tab[0];
       }
       
       return it8.Tab[it8.nTable];
    }
    
    // ---------------------------------------------------------- Memory management
    
    // Frees an allocator and owned memory
    public static void cmsIT8Free(cmsHANDLE hIT8)
    {
    	cmsIT8 it8 = (cmsIT8)hIT8;
    	
        if (it8 == null)
        {
        	return;
        }
        
        if (it8.MemorySink != null)
        {
            OWNEDMEM p;
            OWNEDMEM n;
            
            for (p = it8.MemorySink; p != null; p = n)
            {
                n = p.Next;
                if (p.Ptr != null && p.Ptr instanceof VirtualPointer)
                {
                	cmserr._cmsFree(it8.ContextID, (VirtualPointer)p.Ptr);
                }
            }
        }
        
        if (it8.MemoryBlock != null)
        {
        	cmserr._cmsFree(it8.ContextID, it8.MemoryBlock);
        }
    }
    
    // Allocates a chunk of data, keep linked list
    private static VirtualPointer AllocBigBlock(cmsIT8 it8, int size)
    {
        OWNEDMEM ptr1;
        VirtualPointer ptr = cmserr._cmsMallocZero(it8.ContextID, size);
        
        if (ptr != null)
        {
            ptr1 = new OWNEDMEM();
            
            ptr1.Ptr       = ptr;
            ptr1.Next      = it8.MemorySink;
            it8.MemorySink = ptr1;
        }
        
        return ptr;
    }
    
    // Suballocator.
    private static VirtualPointer AllocChunk(cmsIT8 it8, int size)
    {
        int Free = it8.Allocator.BlockSize - it8.Allocator.Used;
        VirtualPointer ptr;
        
        size = lcms2_internal._cmsALIGNMEM(size);
        
        if (size > Free)
        {
            if (it8.Allocator.BlockSize == 0)
            {
            	it8.Allocator.BlockSize = 20*1024;
            }
            else
            {
            	it8.Allocator.BlockSize *= 2;
            }
            
            if (it8.Allocator.BlockSize < size)
            {
            	it8.Allocator.BlockSize = size;
            }
            
            it8.Allocator.Used = 0;
            it8.Allocator.Block = AllocBigBlock(it8, it8.Allocator.BlockSize);
        }
        
        ptr = new VirtualPointer(it8.Allocator.Block, it8.Allocator.Used);
        it8.Allocator.Used += size;
        
        return ptr;
    }
    
    // Allocates a string
    private static VirtualPointer AllocString(cmsIT8 it8, final String str)
    {
        int Size = Utility.strlen(str)+1;
        VirtualPointer ptr;
        
        ptr = AllocChunk(it8, Size);
        if (ptr != null)
        {
        	Utility.strncpy(ptr, str, Size-1);
        }
        
        return ptr;
    }
    
    // Searches through linked list
    
    private static boolean IsAvailableOnList(KEYVALUE p, final String Key, final String Subkey, KEYVALUE[] LastPtr)
    {
        if (LastPtr != null)
        {
        	LastPtr[0] = p;
        }
        
        for (;  p != null; p = p.Next)
        {
        	if (LastPtr != null)
            {
            	LastPtr[0] = p;
            }
            if (Key.charAt(0) != '#') // Comments are ignored
            {
                if (cmserr.cmsstrcasecmp(Key, p.Keyword) == 0)
                {
                	break;
                }
            }
        }
        
        if (p == null)
        {
        	return false;
        }
        
        if (Subkey == null)
        {
        	return true;
        }
        
        for (; p != null; p = p.NextSubkey)
        {
        	if (LastPtr != null)
            {
            	LastPtr[0] = p;
            }
        	
            if (cmserr.cmsstrcasecmp(Subkey, p.Subkey) == 0)
            {
            	return true;
            }
        }
        
        return false;
    }
    
    // Add a property into a linked list
    private static KEYVALUE AddToList(cmsIT8 it8, KEYVALUE[] Head, final String Key, final String Subkey, final String xValue, int WriteAs)
    {
        KEYVALUE[] p = new KEYVALUE[1];
        KEYVALUE last;
        
        // Check if property is already in list
        
        if (IsAvailableOnList(Head[0], Key, Subkey, p))
        {
            // This may work for editing properties
        	
            //return SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_DUP_KEY), new Object[]{Key});
        }
        else
        {
            last = p[0];
            
            // Allocate the container
            p[0] = new KEYVALUE(); //Normally uses AllocChunk but it is cumbersome to work with
            
            // Store name and value
            p[0].Keyword = AllocString(it8, Key).getProcessor().readString();
            p[0].Subkey = (Subkey == null) ? null : AllocString(it8, Subkey).getProcessor().readString();
            
            // Keep the container in our list
            if (Head[0] == null)
            {
                Head[0] = p[0];
            }
            else
            {
                if (Subkey != null && last != null)
                {
                    last.NextSubkey = p[0];
                    
                    // If Subkey is not null, then last is the last property with the same key,
                    // but not necessarily is the last property in the list, so we need to move
                    // to the actual list end
                    while (last.Next != null)
                    {
                    	last = last.Next;
                    }
                }
                
                if (last != null)
                {
                	last.Next = p[0];
                }
            }
            
            p[0].Next = null;
            p[0].NextSubkey = null;
        }
        
        p[0].WriteAs = WriteAs;
        
        if (xValue != null)
        {
            p[0].Value = AllocString(it8, xValue);
        }
        else
        {
            p[0].Value = null;
        }
        
        return p[0];
    }
    
    private static KEYVALUE AddAvailableProperty(cmsIT8 it8, final String Key, int as)
    {
        return AddToList(it8, it8.ValidKeywords, Key, null, null, as);
    }
    
    private static KEYVALUE AddAvailableSampleID(cmsIT8 it8, final String Key)
    {
        return AddToList(it8, it8.ValidSampleID, Key, null, null, WRITE_UNCOOKED);
    }
    
    private static void AllocTable(cmsIT8 it8)
    {
        TABLE t;
        
        t = it8.Tab[it8.TablesCount];
        if(t == null)
        {
        	t = it8.Tab[it8.TablesCount] = new TABLE();
        }
        
        t.HeaderList = new KEYVALUE[1];
        t.DataFormat = null;
        t.Data       = null;
        
        it8.TablesCount++;
    }
    
    public static int cmsIT8SetTable(cmsHANDLE IT8, int nTable)
    {
         cmsIT8 it8 = (cmsIT8)IT8;
         
         if (nTable >= it8.TablesCount)
         {
             if (nTable == it8.TablesCount)
             {
                 AllocTable(it8);
             }
             else
             {
                 SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_TABLE_OUT_SEQ), new Object[]{new Integer(nTable)});
                 return -1;
             }
         }
         
         it8.nTable = nTable;
         
         return nTable;
    }
    
    // Init an empty container
    public static cmsHANDLE cmsIT8Alloc(cmsContext ContextID)
    {
        cmsIT8 it8;
        int i; //Could possible be a unsigned int (as is the original) but it's used for values that are less then 41
        
        it8 = new cmsIT8();
        
        AllocTable(it8);
        
        it8.MemoryBlock = null;
        it8.MemorySink  = null;
        
        it8.nTable = 0;
        
        it8.ContextID = ContextID;
        it8.Allocator = new SUBALLOCATOR();
        it8.Allocator.Used = 0;
        it8.Allocator.Block = null;
        it8.Allocator.BlockSize = 0;
        
        it8.ValidKeywords = new KEYVALUE[1];
        it8.ValidSampleID = new KEYVALUE[1];
        
        it8.sy = SNONE;
        it8.ch = ' ';
        it8.Source = null;
        it8.inum = 0;
        it8.dnum = 0.0;
        
        it8.FileStack[0] = new FILECTX();
        it8.IncludeSP = 0;
        it8.lineno = 1;
        
        System.arraycopy(DEFAULT_DBL_FORMAT.toCharArray(), 0, it8.DoubleFormatter, 0, DEFAULT_DBL_FORMAT.length());
        it8.DoubleFormatter[DEFAULT_DBL_FORMAT.length()] = 0;
        cmsIT8SetSheetType((cmsHANDLE)it8, "CGATS.17");
        
        // Initialize predefined properties & data
        
        for (i = 0; i < NUMPREDEFINEDPROPS; i++)
        {
        	AddAvailableProperty(it8, PredefinedProperties[i].id, PredefinedProperties[i].as);
        }
        
        for (i = 0; i < NUMPREDEFINEDSAMPLEID; i++)
        {
        	AddAvailableSampleID(it8, PredefinedSampleID[i]);
        }
        
        return it8;
    }
    
    public static String cmsIT8GetSheetType(cmsHANDLE hIT8)
    {
    	return Utility.cstringCreation(GetTable((cmsIT8)hIT8).SheetType);
    }
    
    public static boolean cmsIT8SetSheetType(cmsHANDLE hIT8, final String Type)
    {
    	TABLE t = GetTable((cmsIT8)hIT8);
    	
    	Utility.strncpy(t.SheetType, Type.toCharArray(), MAXSTR-1);
    	t.SheetType[MAXSTR-1] = 0;
    	return true;
    }
    
    public static boolean cmsIT8SetComment(cmsHANDLE hIT8, final String Val)
    {
        cmsIT8 it8 = (cmsIT8)hIT8;
        
        if (Val == null)
        {
        	return false;
        }
        if (Val.length() == 0 || Utility.strlen(Val) == 0)
        {
        	return false;
        }
        
        return AddToList(it8, GetTable(it8).HeaderList, "# ", null, Val, WRITE_UNCOOKED) != null;
    }
    
    // Sets a property
    public static boolean cmsIT8SetPropertyStr(cmsHANDLE hIT8, final String Key, final String Val)
    {
        cmsIT8 it8 = (cmsIT8)hIT8;
        
        if (Val == null)
        {
        	return false;
        }
        if (Val.length() == 0 || Utility.strlen(Val) == 0)
        {
        	return false;
        }
        
        return AddToList(it8, GetTable(it8).HeaderList, Key, null, Val, WRITE_STRINGIFY) != null;
    }
    
    public static boolean cmsIT8SetPropertyDbl(cmsHANDLE hIT8, final String cProp, double Val)
    {
        cmsIT8 it8 = (cmsIT8)hIT8;
        char[] Buffer = new char[1024];
        
        Utility.sprintf(Buffer, Utility.cstringCreation(it8.DoubleFormatter), new Object[]{new Double(Val)});
        
        return AddToList(it8, GetTable(it8).HeaderList, cProp, null, Utility.cstringCreation(Buffer), WRITE_UNCOOKED) != null;
    }
    
    public static boolean cmsIT8SetPropertyHex(cmsHANDLE hIT8, final String cProp, int Val)
    {
        cmsIT8 it8 = (cmsIT8)hIT8;
        char[] Buffer = new char[1024];
        
        Utility.sprintf(Buffer, "%d", new Object[]{new Integer(Val)});
        
        return AddToList(it8, GetTable(it8).HeaderList, cProp, null, Utility.cstringCreation(Buffer), WRITE_HEXADECIMAL) != null;
    }
    
    public static boolean cmsIT8SetPropertyUncooked(cmsHANDLE hIT8, final String Key, final String Buffer)
    {
        cmsIT8 it8 = (cmsIT8)hIT8;
        
        return AddToList(it8, GetTable(it8).HeaderList, Key, null, Buffer, WRITE_UNCOOKED) != null;
    }
    
    public static boolean cmsIT8SetPropertyMulti(cmsHANDLE hIT8, final String Key, final String SubKey, final String Buffer)
    {
        cmsIT8 it8 = (cmsIT8)hIT8;
        
        return AddToList(it8, GetTable(it8).HeaderList, Key, SubKey, Buffer, WRITE_PAIR) != null;
    }
    
    // Gets a property
    public static String cmsIT8GetProperty(cmsHANDLE hIT8, final String Key)
    {
        cmsIT8 it8 = (cmsIT8)hIT8;
        KEYVALUE[] p = new KEYVALUE[1];
        
        if (IsAvailableOnList(GetTable(it8).HeaderList[0], Key, null, p))
        {
            return p[0].Value.getProcessor().readString();
        }
        return null;
    }
    
    public static double cmsIT8GetPropertyDbl(cmsHANDLE hIT8, final String cProp)
    {
        final String v = cmsIT8GetProperty(hIT8, cProp);
        
        return ParseFloatNumber(v);
    }
    
    public static String cmsIT8GetPropertyMulti(cmsHANDLE hIT8, final String Key, final String SubKey)
    {
    	cmsIT8 it8 = (cmsIT8)hIT8;
        KEYVALUE[] p = new KEYVALUE[1];
        
        if (IsAvailableOnList(GetTable(it8).HeaderList[0], Key, SubKey, p))
        {
            return p[0].Value.getProcessor().readString();
        }
        return null;
    }
    
    // ----------------------------------------------------------------- Datasets
    
    private static void AllocateDataFormat(cmsIT8 it8)
    {
        TABLE t = GetTable(it8);
        
        if (t.DataFormat != null)
        {
        	return; // Already allocated
        }
        
        t.nSamples = (int)cmsIT8GetPropertyDbl(it8, "NUMBER_OF_FIELDS");
        
        if (t.nSamples <= 0)
        {
            SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_ALLOC_DAT_FORM_UNK_COUNT), null);
            t.nSamples = 10;
        }
        
        t.DataFormat = new VirtualPointer[t.nSamples + 1];
        /*
        if (t.DataFormat == null)
        {
            SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_ALLOC_DAT_FORM_CANT_ALLOC), null);
        }
        */
    }
    
    private static VirtualPointer GetDataFormat(cmsIT8 it8, int n)
    {
        TABLE t = GetTable(it8);
        
        if (t.DataFormat != null)
        {
        	return t.DataFormat[n];
        }
        return null;
    }
    
    private static boolean SetDataFormat(cmsIT8 it8, int n, final String label)
    {
        TABLE t = GetTable(it8);
        
        if (t.DataFormat == null)
        {
        	AllocateDataFormat(it8);
        }
        
        if (n > t.nSamples)
        {
            SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_EXCEED_KEY_COUNT), null);
            return false;
        }
        
        if (t.DataFormat != null)
        {
            t.DataFormat[n] = AllocString(it8, label);
        }
        
        return true;
    }
    
    public static boolean cmsIT8SetDataFormat(cmsHANDLE h, int n, final String Sample)
    {
    	cmsIT8 it8 = (cmsIT8)h;
    	return SetDataFormat(it8, n, Sample);
    }
    
    private static void AllocateDataSet(cmsIT8 it8)
    {
        TABLE t = GetTable(it8);
        
        if (t.Data != null)
        {
        	return; // Already allocated
        }
        
        t.nSamples = Integer.parseInt(cmsIT8GetProperty(it8, "NUMBER_OF_FIELDS"));
        t.nPatches = Integer.parseInt(cmsIT8GetProperty(it8, "NUMBER_OF_SETS"));
        
        t.Data = new VirtualPointer[(t.nSamples + 1) * (t.nPatches + 1)];
        /*
        if (t.Data == null)
        {
            SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_ALLOC_DAT_CANT_ALLOC), null);
        }
        */
    }
    
    private static VirtualPointer GetData(cmsIT8 it8, int nSet, int nField)
    {
        TABLE t = GetTable(it8);
        int nSamples = t.nSamples;
        int nPatches = t.nPatches;
        
        if (nSet >= nPatches || nField >= nSamples)
        {
        	return null;
        }
        
        if (t.Data == null)
        {
        	return null;
        }
        return t.Data[nSet * nSamples + nField];
    }
    
    private static boolean SetData(cmsIT8 it8, int nSet, int nField, final String Val)
    {
        TABLE t = GetTable(it8);

        if (t.Data == null)
        {
        	AllocateDataSet(it8);
        }
        
        if (t.Data == null)
        {
        	return false;
        }
        
        if (nSet > t.nPatches || nSet < 0)
        {
        	return SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_DATA_OUTOFRANGE_FORMAT_PATCH), new Object[]{new Integer(nSet), new Integer(t.nPatches)});
        }
        
        if (nField > t.nSamples || nField < 0)
        {
        	return SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_DATA_OUTOFRANGE_FORMAT_SAMPLES), new Object[]{new Integer(nField), new Integer(t.nSamples)});
        }
        
        t.Data[nSet * t.nSamples + nField] = AllocString(it8, Val);
        return true;
    }
    
    // --------------------------------------------------------------- File I/O
    
    // Writes a string to file
    private static void WriteStr(SAVESTREAM f, String str)
    {
        int len;
        
        if (str == null)
        {
        	str = " ";
        }
        
        // Length to write
        len = Utility.strlen(str);
        f.Used += len;
        
        if (f.stream != null) // Should I write it to a file?
        {
        	if (f.stream.write(str.getBytes(), 0, 1, len) != len)
            {
                cmserr.cmsSignalError(null, lcms2.cmsERROR_WRITE, Utility.LCMS_Resources.getString(LCMSResource.CGATS_FILE_WRITE_ERROR), null);
                return;
            }
        }
        else // Or to a memory block?
        {
            if (f.Base != null && !f.Base.isFree()) // Am I just counting the bytes?
            {
                if (f.Used > f.Max)
                {
                     cmserr.cmsSignalError(null, lcms2.cmsERROR_WRITE, Utility.LCMS_Resources.getString(LCMSResource.CGATS_MEM_WRITE_ERROR), null);
                     return;
                }
                
                f.Ptr.getProcessor().write(str, false, false, false);
                f.Ptr.movePosition(len);
            }
        }
    }
    
    // Write formatted
    
    private static void Writef(SAVESTREAM f, final String frm, Object[] args)
    {
        char[] Buffer = new char[4096];
        
        Utility.vsnprintf(Buffer, 4095, frm, args);
        Buffer[4095] = 0;
        WriteStr(f, Utility.cstringCreation(Buffer));
    }
    
    // Writes full header
    private static void WriteHeader(cmsIT8 it8, SAVESTREAM fp)
    {
        KEYVALUE p;
        TABLE t = GetTable(it8);
        
        // Writes the type
        WriteStr(fp, Utility.cstringCreation(t.SheetType));
        WriteStr(fp, "\n");
        
        for (p = t.HeaderList[0]; (p != null); p = p.Next)
        {
            if (p.Keyword.charAt(0) == '#')
            {
                VirtualPointer Pt;
                
                WriteStr(fp, "#\n# ");
                for (Pt = (VirtualPointer)p.Value; Pt.readRaw() != 0; Pt.movePosition(1))
                {
                    Writef(fp, "%c", new Object[]{new Character((char)Pt.readRaw())});
                    
                    if (Pt.readRaw() == '\n')
                    {
                        WriteStr(fp, "# ");
                    }
                }
                
                WriteStr(fp, "\n#\n");
                continue;
            }
            
            if (!IsAvailableOnList(it8.ValidKeywords[0], p.Keyword, null, null))
            {
//#ifdef CMS_STRICT_CGATS
                WriteStr(fp, "KEYWORD\t\"");
                WriteStr(fp, p.Keyword);
                WriteStr(fp, "\"\n");
//#endif

                AddAvailableProperty(it8, p.Keyword, WRITE_UNCOOKED);
            }
            
            WriteStr(fp, p.Keyword);
            if (p.Value != null)
            {
                switch (p.WriteAs)
                {
	                case WRITE_UNCOOKED:
	                        Writef(fp, "\t%s", new Object[]{p.Value});
	                        break;
	                        
	                case WRITE_STRINGIFY:
	                        Writef(fp, "\t\"%s\"", new Object[]{p.Value});
	                        break;
	                        
	                case WRITE_HEXADECIMAL:
	                        Writef(fp, "\t0x%X", new Object[]{Integer.valueOf(p.Value.getProcessor().readString())});
	                        break;
	                        
	                case WRITE_BINARY:
	                        Writef(fp, "\t0x%B", new Object[]{Integer.valueOf(p.Value.getProcessor().readString())});
	                        break;
	                        
	                case WRITE_PAIR:
	                        Writef(fp, "\t\"%s,%s\"", new Object[]{p.Subkey, p.Value});
	                        break;
	                        
	                default: SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_UNK_WRITEMODE), new Object[]{new Integer(p.WriteAs)});
	                         return;
                }
            }
            
            WriteStr(fp, "\n");
        }
    }
    
    // Writes the data format
    private static void WriteDataFormat(SAVESTREAM fp, cmsIT8 it8)
    {
        int i, nSamples;
        TABLE t = GetTable(it8);
        
        if (t.DataFormat == null)
        {
        	return;
        }
        
        WriteStr(fp, "BEGIN_DATA_FORMAT\n");
        WriteStr(fp, " ");
        nSamples = Integer.parseInt(cmsIT8GetProperty(it8, "NUMBER_OF_FIELDS"));
        
        for (i = 0; i < nSamples; i++)
        {
        	WriteStr(fp, t.DataFormat[i].getProcessor().readString());
        	WriteStr(fp, ((i == (nSamples-1)) ? "\n" : "\t"));
        }
        
        WriteStr (fp, "END_DATA_FORMAT\n");
    }
    
    // Writes data array
    private static void WriteData(SAVESTREAM fp, cmsIT8 it8)
    {
    	int  i, j;
    	TABLE t = GetTable(it8);
    	
    	if (t.Data == null)
    	{
    		return;
    	}
    	
    	WriteStr (fp, "BEGIN_DATA\n");
    	
    	t.nPatches = Integer.parseInt(cmsIT8GetProperty(it8, "NUMBER_OF_SETS"));
    	
    	for (i = 0; i < t.nPatches; i++)
    	{
    		WriteStr(fp, " ");
    		
    		for (j = 0; j < t.nSamples; j++)
    		{
    			VirtualPointer ptr = t.Data[i*t.nSamples+j];
    			
    			if (ptr == null)
    			{
    				WriteStr(fp, "\"\"");
    			}
    			else
    			{
    				// If value contains whitespace, enclose within quote
    				
    				String str = ptr.getProcessor().readString();
    				if (str.indexOf(' ') >= 0)
    				{
    					WriteStr(fp, "\"");
    					WriteStr(fp, str);
    					WriteStr(fp, "\"");
    				}
    				else
    				{
    					WriteStr(fp, str);
    				}
    			}
    			
    			WriteStr(fp, ((j == (t.nSamples-1)) ? "\n" : "\t"));
    		}
    	}
    	WriteStr (fp, "END_DATA\n");
    }
    
    // Saves whole file
    public static boolean cmsIT8SaveToFile(cmsHANDLE hIT8, String cFileName)
    {
        SAVESTREAM sd = new SAVESTREAM();
        int i;
        cmsIT8 it8 = (cmsIT8)hIT8;
        
        cFileName = cFileName.substring(0, Utility.strlen(cFileName));
        sd.stream = Stream.fopen(cFileName, "w");
        if (sd.stream == null)
        {
        	return false;
        }
        
        for (i = 0; i < it8.TablesCount; i++)
        {
        	cmsIT8SetTable(hIT8, i);
        	WriteHeader(it8, sd);
        	WriteDataFormat(sd, it8);
        	WriteData(sd, it8);
        }
        
        if (sd.stream.close() != 0)
        {
        	return false;
        }
        
        return true;
    }
    
    // Saves to memory
    public static boolean cmsIT8SaveToMem(cmsHANDLE hIT8, byte[] MemPtr, int[] BytesNeeded)
    {
        SAVESTREAM sd = new SAVESTREAM();
        int i;
        cmsIT8 it8 = (cmsIT8)hIT8;
        
        sd.stream = null;
        sd.Base   = new VirtualPointer(MemPtr, true);
        sd.Ptr    = new VirtualPointer(sd.Base);
        
        sd.Used = 0;
        
        if (!sd.Base.isFree())
        {
        	sd.Max = BytesNeeded[0]; // Write to memory?
        }
        else
        {
        	sd.Max = 0;              // Just counting the needed bytes
        }
        
        for (i = 0; i < it8.TablesCount; i++)
        {
        	cmsIT8SetTable(hIT8, i);
        	WriteHeader(it8, sd);
        	WriteDataFormat(sd, it8);
        	WriteData(sd, it8);
        }
        
        sd.Used++; // The \0 at the very end
        
        if (!sd.Base.isFree())
        {
        	sd.Ptr.writeRaw(0);
        }
        
        BytesNeeded[0] = sd.Used;
        
        return true;
    }
    
    // -------------------------------------------------------------- Higer level parsing
    
    private static boolean DataFormatSection(cmsIT8 it8)
    {
        int iField = 0;
        TABLE t = GetTable(it8);
        
        InSymbol(it8); // Eats "BEGIN_DATA_FORMAT"
        CheckEOLN(it8);
        
        while (it8.sy != SEND_DATA_FORMAT &&
            it8.sy != SEOLN &&
            it8.sy != SEOF &&
            it8.sy != SSYNERROR)
        {
        	if (it8.sy != SIDENT)
        	{
        		return SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_SAMPLE_TYPE_EXPECTED), null);
        	}
        	
        	if (!SetDataFormat(it8, iField, Utility.cstringCreation(it8.id)))
        	{
        		return false;
        	}
        	iField++;
        	
        	InSymbol(it8);
        	SkipEOLN(it8);
        }
        
        SkipEOLN(it8);
        Skip(it8, SEND_DATA_FORMAT);
        SkipEOLN(it8);
        
        if (iField != t.nSamples)
        {
        	SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_FIELD_COUNT_MISMATCH), new Object[]{new Integer(t.nSamples), new Integer(iField)});
        }
        
        return true;
    }
    
    private static boolean DataSection (cmsIT8 it8)
    {
        int  iField = 0;
        int  iSet   = 0;
        char[] Buffer = new char[256];
        TABLE t = GetTable(it8);
        
        InSymbol(it8); // Eats "BEGIN_DATA"
        CheckEOLN(it8);
        
        if (t.Data == null)
        {
        	AllocateDataSet(it8);
        }
        
        while (it8.sy != SEND_DATA && it8.sy != SEOF)
        {
            if (iField >= t.nSamples)
            {
                iField = 0;
                iSet++;
            }
            
            if (it8.sy != SEND_DATA && it8.sy != SEOF)
            {
                if (!GetVal(it8, Buffer, 255, Utility.LCMS_Resources.getString(LCMSResource.CGATS_SAMPLE_DATA_EXPECTED)))
                {
                	return false;
                }
                
                if (!SetData(it8, iSet, iField, Utility.cstringCreation(Buffer)))
                {
                	return false;
                }
                
                iField++;
                
                InSymbol(it8);
                SkipEOLN(it8);
            }
        }
        
        SkipEOLN(it8);
        Skip(it8, SEND_DATA);
        SkipEOLN(it8);
        
        // Check for data completion.
        
        if ((iSet+1) != t.nPatches)
        {
        	return SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_SET_COUNT_MISMATCH), new Object[]{new Integer(t.nPatches), new Integer(iSet+1)});
        }
        
        return true;
    }
    
    private static boolean HeaderSection(cmsIT8 it8)
    {
        char[] VarName = new char[MAXID];
        char[] Buffer = new char[MAXSTR];
        KEYVALUE Key;
        
        while (it8.sy != SEOF &&
        		it8.sy != SSYNERROR &&
        		it8.sy != SBEGIN_DATA_FORMAT &&
        		it8.sy != SBEGIN_DATA)
        {
        	switch (it8.sy)
        	{
	            case SKEYWORD:
                    InSymbol(it8);
                    if (!GetVal(it8, Buffer, MAXSTR-1, Utility.LCMS_Resources.getString(LCMSResource.CGATS_KEYWORD_EXPECTED)))
                    {
                    	return false;
                    }
                    if (AddAvailableProperty(it8, Utility.cstringCreation(Buffer), WRITE_UNCOOKED) == null)
                    {
                    	return false;
                    }
                    InSymbol(it8);
                    break;
	                    
	            case SDATA_FORMAT_ID:
                    InSymbol(it8);
                    if (!GetVal(it8, Buffer, MAXSTR-1, Utility.LCMS_Resources.getString(LCMSResource.CGATS_KEYWORD_EXPECTED)))
                    {
                    	return false;
                    }
                    if (AddAvailableSampleID(it8, Utility.cstringCreation(Buffer)) == null)
                    {
                    	return false;
                    }
                    InSymbol(it8);
                    break;
	                    
	            case SIDENT:
	            	Utility.strncpy(VarName, it8.id, MAXID-1);
                    VarName[MAXID-1] = 0;
                    
                    KEYVALUE[] tempKey = new KEYVALUE[1];
                    if (!IsAvailableOnList(it8.ValidKeywords[0], Utility.cstringCreation(VarName), null, tempKey))
                    {
//#ifdef CMS_STRICT_CGATS
                    	return SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_UNDEFINED_KEYWORD), new Object[]{VarName});
//#else
                        Key = AddAvailableProperty(it8, Utility.cstringCreation(VarName), WRITE_UNCOOKED);
                        if (Key == null)
	                    {
	                    	return false;
	                    }
//#endif
                    }
                    Key = tempKey[0];
                    
                    InSymbol(it8);
                    if (!GetVal(it8, Buffer, MAXSTR-1, Utility.LCMS_Resources.getString(LCMSResource.CGATS_DATA_PROPERTY_EXPECTED)))
                    {
                    	return false;
                    }
                    
                    if(Key.WriteAs != WRITE_PAIR)
                    {
                        AddToList(it8, GetTable(it8).HeaderList, Utility.cstringCreation(VarName), null, Utility.cstringCreation(Buffer),
                        		(it8.sy == SSTRING) ? WRITE_STRINGIFY : WRITE_UNCOOKED);
                    }
                    else
                    {
                    	int max = Buffer.length;
                        int Subkey;
                        int Nextkey;
                        if (it8.sy != SSTRING)
                        {
                        	return SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_INVALID_VALUE_FOR_PROP_PARAM), new Object[]{Buffer, VarName});
                        }
                        String BufferStr = Utility.cstringCreation(Buffer);
                        
                        // chop the string as a list of "subkey, value" pairs, using ';' as a separator
                        for (Subkey = 0; Subkey < max && Subkey >= 0; Subkey = Nextkey)
                        {
                            int Value, temp;
                            
                            //  identify token pair boundary
                            Nextkey = BufferStr.indexOf(';', Subkey);
                            if(Nextkey == -1)
                            {
                            	Buffer[Nextkey++] = '\0';
                            }
                            
                            // for each pair, split the subkey and the value
                            Value = BufferStr.lastIndexOf(',', Subkey);
                            if(Value == -1)
                            {
                            	return SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_INVALID_VALUE_FOR_PROP), new Object[]{VarName});
                            }
                            
                            // gobble the spaces before the coma, and the coma itself
                            temp = Value++;
                            do
                            {
                            	Buffer[temp--] = '\0';
                            }while(temp >= Subkey && Buffer[temp] == ' ');
                            
                            // gobble any space at the right
                            temp = Value + Utility.strlen(Buffer, Value) - 1;
                            while(Buffer[temp] == ' ')
                            {
                            	Buffer[temp--] = '\0';
                            }
                            
                            // trim the strings from the left
                            char[] spanChars = new char[]{' '};
                            Subkey += Utility.strspn(Buffer, Subkey, spanChars);
                            Value += Utility.strspn(Buffer, Value, spanChars);
                            
                            if(Buffer[Subkey] == 0 || Buffer[Value] == 0)
                            {
                            	return SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_INVALID_VALUE_FOR_PROP), new Object[]{VarName});
                            }
                            AddToList(it8, GetTable(it8).HeaderList, Utility.cstringCreation(VarName), Utility.cstringCreation(Buffer, Subkey),
                            		Utility.cstringCreation(Buffer, Value), WRITE_PAIR);
                        }
                    }
                    
                    InSymbol(it8);
                    break;
	                    
	            case SEOLN:
            		break;
	            
	            default:
                    return SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_EXPECTED_KEYWORD_IDENTIFIER), null);
            }
        	
        	SkipEOLN(it8);
        }
        
        return true;
    }
    
    private static void ReadType(cmsIT8 it8, char[] SheetType, int SheetTypePtr)
    {
    	// First line is a very special case.
    	
    	while (isseparator(it8.ch))
    	{
    		NextCh(it8);
    	}
    	
    	while (it8.ch != '\r' && it8.ch != '\n' && it8.ch != '\t' && it8.ch != -1)
    	{
    		SheetType[SheetTypePtr++] = (char)it8.ch;
    		NextCh(it8);
    	}
        
    	SheetType[SheetTypePtr++] = 0;
    }
    
    private static boolean ParseIT8(cmsIT8 it8, boolean nosheet)
    {
        if (!nosheet)
        {
            ReadType(it8, it8.Tab[0].SheetType, 0);
        }
    	
        InSymbol(it8);
        
        SkipEOLN(it8);
        
        while (it8.sy != SEOF && it8.sy != SSYNERROR)
        {
        	switch (it8.sy)
        	{
                case SBEGIN_DATA_FORMAT:
                    if (!DataFormatSection(it8))
                    {
                    	return false;
                    }
                    break;
                    
                case SBEGIN_DATA:
                    if (!DataSection(it8))
                    {
                    	return false;
                    }
                    
                    if (it8.sy != SEOF)
                    {
                        AllocTable(it8);
                        it8.nTable = it8.TablesCount - 1;
                        
                        // Read sheet type if present. We only support identifier and string.
                        // <ident> <eoln> is a type string
                        // anything else, is not a type string
                        if (!nosheet)
                        {
                        	if (it8.sy == SIDENT)
                        	{
                        		// May be a type sheet or may be a prop value statement. We cannot use insymbol in
                                // this special case...
                        		while (isseparator(it8.ch))
                        		{
                        			NextCh(it8);
                        		}
                        		
                        		// If a newline is found, then this is a type string
                                if (it8.ch == '\n')
                                {
                                	cmsIT8SetSheetType(it8, Utility.cstringCreation(it8.id));
                                	InSymbol(it8);
                                }
                                else
                                {
                                    // It is not. Just continue
                                    cmsIT8SetSheetType(it8, "");
                                }
                        	}
                            else
                            {
                            	// Validate quoted strings
                                if (it8.sy == SSTRING)
                                {
                                	cmsIT8SetSheetType(it8, Utility.cstringCreation(it8.str));
                                	InSymbol(it8);
                                }
                            }
                        }
                    }
                    break;
                    
                case SEOLN:
                    SkipEOLN(it8);
                    break;
                    
                default:
                    if (!HeaderSection(it8))
                    {
                    	return false;
                    }
        	}
        }
        
        return (it8.sy != SSYNERROR);
    }
    
    // Init usefull pointers
    
    private static void CookPointers(cmsIT8 it8)
    {
        int idField, i;
        VirtualPointer Fld;
        int j;
        int nOldTable = it8.nTable;
        
        for (j = 0; j < it8.TablesCount; j++)
        {
        	TABLE t = it8.Tab[j];
        	
        	t.SampleID = 0;
        	it8.nTable = j;
        	
        	for (idField = 0; idField < t.nSamples; idField++)
        	{
        		if (t.DataFormat == null)
        		{
        			SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_UNDEFINED_DATA_FORMAT), null);
        			return;
        		}
        		
        		Fld = t.DataFormat[idField];
        		if (Fld == null)
        		{
        			continue;
        		}
        		
        		if (cmserr.cmsstrcasecmp(Fld.getProcessor().readString(), "SAMPLE_ID") == 0)
        		{
        			t.SampleID = idField;
        			
        			for (i = 0; i < t.nPatches; i++)
        			{
        				VirtualPointer Data = GetData(it8, i, idField);
        				if (Data != null)
        				{
        					char[] Buffer = new char[256];
        					
        					Utility.strncpy(Buffer, Data, 255);
        					Buffer[255] = 0;
        					
        					if (Utility.strlen(Buffer) <= Utility.strlen(Data))
        					{
        						Utility.strcpy(Data, Buffer);
        					}
        					else
        					{
        						SetData(it8, i, idField, Utility.cstringCreation(Buffer));
        					}
        				}
                    }
        		}
        		
        		// "LABEL" is an extension. It keeps references to forward table
        		
        		if ((cmserr.cmsstrcasecmp(Fld.getProcessor().readString(), "LABEL") == 0) || Fld.readRaw() == '$' )
        		{
        			// Search for table references...
        			for (i = 0; i < t.nPatches; i++)
        			{
        				VirtualPointer Label = GetData(it8, i, idField);
        				
        				if (Label != null)
        				{
        					int k;
        					
        					// This is the label, search for a table containing
        					// this property
        					
        					for (k = 0; k < it8.TablesCount; k++)
        					{
        						TABLE Table = it8.Tab[k];
        						KEYVALUE[] p = new KEYVALUE[1];
        						
        						if (IsAvailableOnList(Table.HeaderList[0], Label.getProcessor().readString(), null, p))
        						{
        							// Available, keep type and table
        							char[] Buffer = new char[256];
        							
        							Object Type  = p[0].Value;
        							int nTable = k;
        							
        							Utility.vsnprintf(Buffer, 255, "%s %d %s", new Object[]{Label, new Integer(nTable), Type});
        							
        							SetData(it8, i, idField, Utility.cstringCreation(Buffer));
        						}
        					}
        				}
        			}
        		}
        	}
        }
        
        it8.nTable = nOldTable;
    }
    
    // Try to infere if the file is a CGATS/IT8 file at all. Read first line
    // that should be something like some printable characters plus a \n
    // returns 0 if this is not like a CGATS, or an integer otherwise. This integer is the number of words in first line?
    private static int IsMyBlock(byte[] Buffer, int n)
    {
        int words = 1;
        boolean space = false, quot = false;
        int i;
        
        if (n < 10)
        {
        	return 0; // Too small
        }
        
        if (n > 132)
        {
        	n = 132;
        }
        
        for (i = 1; i < n; i++)
        {
            switch(Buffer[i])
            {
	            case '\n':
	            case '\r':
	            	return (quot || (words > 2)) ? 0 : words;
	            case '\t':
	            case ' ':
	                if(!quot && !space)
	                {
	                	space = true;
	                }
	                break;
	            case '\"':
	                quot = !quot;
	                break;
	            default:
	                if (Buffer[i] < 32)
	                {
	                	return 0;
	                }
	                if (Buffer[i] > 127)
	                {
	                	return 0;
	                }
	                words += space ? 1 : 0;
	                space = false;
	                break;
            }
        }
        
        return 0;
    }
    
    private static boolean IsMyFile(final String FileName)
    {
       Stream fp;
       int Size;
       byte[] Ptr = new byte[133];
       
       fp = Stream.fopen(FileName, "r");
       if (fp == null)
       {
           cmserr.cmsSignalError(null, lcms2.cmsERROR_FILE, Utility.LCMS_Resources.getString(LCMSResource.FILE_NOT_FOUND), new Object[]{FileName});
           return false;
       }
       
       Size = (int)fp.read(Ptr, 0, 1, 132);
       
       if (fp.close() != 0)
       {
    	   return false;
       }
       
       Ptr[Size] = '\0';
       
       return IsMyBlock(Ptr, Size) != 0;
    }
    
    // ---------------------------------------------------------- Exported routines
    
    public static cmsHANDLE cmsIT8LoadFromMem(cmsContext ContextID, byte[] Ptr, int len)
    {
        cmsHANDLE hIT8;
        cmsIT8 it8;
    	boolean type;
    	
    	lcms2_internal._cmsAssert(Ptr != null, "Ptr != null");
    	lcms2_internal._cmsAssert(len != 0, "len != 0");
    	
        type = IsMyBlock(Ptr, len) != 0;
        if (!type)
        {
        	return null;
        }
        
        hIT8 = cmsIT8Alloc(ContextID);
        if (hIT8 == null)
        {
        	return null;
        }
        
        it8 = (cmsIT8)hIT8;
        it8.MemoryBlock = cmserr._cmsMalloc(ContextID, len + 1);
        
        Utility.strncpy(it8.MemoryBlock, Ptr, len);
        it8.MemoryBlock.writeRaw(0, len);
        
        Utility.strncpy(it8.FileStack[0].FileName, "", lcms2.cmsMAX_PATH-1);
        it8.Source = it8.MemoryBlock;
        
        if (!ParseIT8(it8, !type))
        {
            cmsIT8Free(hIT8);
            return null;
        }
        
        CookPointers(it8);
        it8.nTable = 0;
        
        cmserr._cmsFree(ContextID, it8.MemoryBlock);
        it8.MemoryBlock = null;
        
        return hIT8;
    }
    
    public static cmsHANDLE cmsIT8LoadFromFile(cmsContext ContextID, String cFileName)
    {
    	cmsHANDLE hIT8;
    	cmsIT8 it8;
    	boolean type;
    	
    	lcms2_internal._cmsAssert(cFileName != null, "cFileName != null");
    	
    	cFileName = cFileName.substring(0, Utility.strlen(cFileName));
    	type = IsMyFile(cFileName);
    	if (!type)
    	{
    		return null;
    	}
    	
    	hIT8 = cmsIT8Alloc(ContextID);
    	it8 = (cmsIT8)hIT8;
    	if (hIT8 == null)
    	{
    		return null;
    	}
    	
    	it8.FileStack[0].Stream = Stream.fopen(cFileName, "r");
    	
    	if (it8.FileStack[0].Stream == null)
    	{
    		cmsIT8Free(hIT8);
    		return null;
    	}
    	
    	Utility.strncpy(it8.FileStack[0].FileName, cFileName, lcms2.cmsMAX_PATH-1);
    	if(it8.FileStack[0].FileName.length() < lcms2.cmsMAX_PATH)
    	{
    		it8.FileStack[0].FileName.append('\0');
    	}
    	else
    	{
    		it8.FileStack[0].FileName.setLength(lcms2.cmsMAX_PATH);
    		it8.FileStack[0].FileName.setCharAt(lcms2.cmsMAX_PATH-1, '\0');
    	}
        
        if (!ParseIT8(it8, !type))
        {
        	it8.FileStack[0].Stream.close();
        	cmsIT8Free(hIT8);
        	return null;
        }
        
        CookPointers(it8);
        it8.nTable = 0;
        
        if (it8.FileStack[0].Stream.close() != 0)
        {
        	cmsIT8Free(hIT8);
        	return null;
    	}
        
        return hIT8;
    }
    
    public static int cmsIT8EnumDataFormat(cmsHANDLE hIT8, VirtualPointer[][] SampleNames)
    {
    	cmsIT8 it8 = (cmsIT8)hIT8;
    	TABLE t;
    	
    	lcms2_internal._cmsAssert(hIT8 != null, "hIT8 != null");
    	
    	t = GetTable(it8);
    	
    	if (SampleNames != null)
    	{
    		SampleNames[0] = t.DataFormat;
    	}
    	return t.nSamples;
    }
    
    public static int cmsIT8EnumProperties(cmsHANDLE hIT8, String[][] PropertyNames)
    {
        cmsIT8 it8 = (cmsIT8)hIT8;
        KEYVALUE p;
        int n;
        String[] Props;
        TABLE t;
    	
    	lcms2_internal._cmsAssert(hIT8 != null, "hIT8 != null");
    	
    	t = GetTable(it8);
    	
        // Pass#1 - count properties
    	
        n = 0;
        for (p = t.HeaderList[0];  p != null; p = p.Next)
        {
            n++;
        }
        
        Props = new String[n];
        
        // Pass#2 - Fill pointers
        n = 0;
        for (p = t.HeaderList[0];  p != null; p = p.Next)
        {
            Props[n++] = p.Keyword;
        }
        
        PropertyNames[0] = Props;
        return n;
    }
    
    public static int cmsIT8EnumPropertyMulti(cmsHANDLE hIT8, final String cProp, final String[][] SubpropertyNames)
    {
        cmsIT8 it8 = (cmsIT8)hIT8;
        KEYVALUE tmp;
        KEYVALUE[] p = new KEYVALUE[1];
        int n;
        final String[] Props;
        TABLE t;
    	
    	lcms2_internal._cmsAssert(hIT8 != null, "hIT8 != null");
    	
    	t = GetTable(it8);
    	
        if(!IsAvailableOnList(t.HeaderList[0], cProp, null, p))
        {
            SubpropertyNames[0] = null;
            return 0;
        }
        
        // Pass#1 - count properties
        
        n = 0;
        for (tmp = p[0];  tmp != null; tmp = tmp.NextSubkey)
        {
            if(tmp.Subkey != null)
            {
            	n++;
            }
        }
        
        Props = new String[n];
        
        // Pass#2 - Fill pointers
        n = 0;
        for (tmp = p[0];  tmp != null; tmp = tmp.NextSubkey)
        {
            if(tmp.Subkey != null)
            {
            	Props[n++] = p[0].Subkey;
            }
        }
        
        SubpropertyNames[0] = Props;
        return n;
    }
    
    private static int LocatePatch(cmsIT8 it8, final String cPatch)
    {
        int i;
        String data;
        TABLE t = GetTable(it8);
        
        for (i = 0; i < t.nPatches; i++)
        {
            data = GetData(it8, i, t.SampleID).getProcessor().readString();
            
            if (data != null)
            {
            	if (cmserr.cmsstrcasecmp(data, cPatch) == 0)
            	{
            		return i;
            	}
            }
        }
        
        // SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_COULDNT_FIND_PATCH), new Object[]{cPatch});
        return -1;
    }
    
    private static int LocateEmptyPatch(cmsIT8 it8)
    {
        int i;
        VirtualPointer data;
        TABLE t = GetTable(it8);

        for (i = 0; i < t.nPatches; i++)
        {
            data = GetData(it8, i, t.SampleID);
            
            if (data == null)
            {
            	return i;
            }
        }
        
        return -1;
    }
    
    private static int LocateSample(cmsIT8 it8, final String cSample)
    {
        int i;
        String fld;
        TABLE t = GetTable(it8);
        
        for (i = 0; i < t.nSamples; i++)
        {
            fld = GetDataFormat(it8, i).getProcessor().readString();
            if (cmserr.cmsstrcasecmp(fld, cSample) == 0)
            {
            	return i;
            }
        }
        
        return -1;
    }
    
    public static int cmsIT8FindDataFormat(cmsHANDLE hIT8, final String cSample)
    {
        cmsIT8 it8 = (cmsIT8)hIT8;
        
    	lcms2_internal._cmsAssert(hIT8 != null, "hIT8 != null");
    	
        return LocateSample(it8, cSample);
    }
    
    public static String cmsIT8GetDataRowCol(cmsHANDLE hIT8, int row, int col)
    {
        cmsIT8 it8 = (cmsIT8)hIT8;
        
        lcms2_internal._cmsAssert(hIT8 != null, "hIT8 != null");
        
        VirtualPointer vp = GetData(it8, row, col);
        return vp == null ? null : vp.getProcessor().readString();
    }
    
    public static double cmsIT8GetDataRowColDbl(cmsHANDLE hIT8, int row, int col)
    {
        final String Buffer;
        
        Buffer = cmsIT8GetDataRowCol(hIT8, row, col);
        
        return ParseFloatNumber(Buffer);
    }
    
    public static boolean cmsIT8SetDataRowCol(cmsHANDLE hIT8, int row, int col, final String Val)
    {
        cmsIT8 it8 = (cmsIT8)hIT8;
        
        lcms2_internal._cmsAssert(hIT8 != null, "hIT8 != null");
    	
        return SetData(it8, row, col, Val);
    }
    
    public static boolean cmsIT8SetDataRowColDbl(cmsHANDLE hIT8, int row, int col, double Val)
    {
        cmsIT8 it8 = (cmsIT8)hIT8;
        StringBuffer Buff = new StringBuffer(256);
        
        lcms2_internal._cmsAssert(hIT8 != null, "hIT8 != null");
        
        Utility.sprintf(Buff, Utility.cstringCreation(it8.DoubleFormatter), new Object[]{new Double(Val)});
        
        String dat = Buff.toString();
        return SetData(it8, row, col, dat.substring(0, Utility.strlen(dat)));
    }
    
    public static String cmsIT8GetData(cmsHANDLE hIT8, final String cPatch, final String cSample)
    {
        cmsIT8 it8 = (cmsIT8)hIT8;
        int iField, iSet;
        
        lcms2_internal._cmsAssert(hIT8 != null, "hIT8 != null");
        
        iField = LocateSample(it8, cSample);
        if (iField < 0)
        {
            return null;
        }
        
        iSet = LocatePatch(it8, cPatch);
        if (iSet < 0)
        {
        	return null;
        }
        
        VirtualPointer vp = GetData(it8, iSet, iField);
        return vp == null ? null : vp.getProcessor().readString();
    }
    
    public static double cmsIT8GetDataDbl(cmsHANDLE it8, final String cPatch, final String cSample)
    {
    	final String Buffer;
    	
        Buffer = cmsIT8GetData(it8, cPatch, cSample);
        
        return ParseFloatNumber(Buffer);
    }
    
    public static boolean cmsIT8SetData(cmsHANDLE hIT8, final String cPatch, final String cSample, final String Val)
    {
        cmsIT8 it8 = (cmsIT8)hIT8;
        int iField, iSet;
        TABLE t;
    	
        lcms2_internal._cmsAssert(hIT8 != null, "hIT8 != null");
        
    	t = GetTable(it8);
    	
        iField = LocateSample(it8, cSample);
        
        if (iField < 0)
        {
        	return false;
        }
        
        if (t.nPatches == 0)
        {
            AllocateDataFormat(it8);
            AllocateDataSet(it8);
            CookPointers(it8);
        }
        
        if (cmserr.cmsstrcasecmp(cSample, "SAMPLE_ID") == 0)
        {
            iSet = LocateEmptyPatch(it8);
            if (iSet < 0)
            {
                return SynError(it8, Utility.LCMS_Resources.getString(LCMSResource.CGATS_COULDNT_ADD_PATCH), new Object[]{cPatch});
            }
            
            iField = t.SampleID;
        }
        else
        {
            iSet = LocatePatch(it8, cPatch);
            if (iSet < 0)
            {
                return false;
            }
        }
        
        return SetData(it8, iSet, iField, Val);
    }
    
    public static boolean cmsIT8SetDataDbl(cmsHANDLE hIT8, final String cPatch, final String cSample, double Val)
    {
        cmsIT8 it8 = (cmsIT8)hIT8;
        StringBuffer Buff = new StringBuffer(256);
        
        lcms2_internal._cmsAssert(hIT8 != null, "hIT8 != null");
        
        Utility.vsnprintf(Buff, 255, Utility.cstringCreation(it8.DoubleFormatter), new Object[]{new Double(Val)});
        
        String dat = Buff.toString();
        return cmsIT8SetData(hIT8, cPatch, cSample, dat.substring(0, Utility.strlen(dat)));
    }
    
    // Buffer should get MAXSTR at least
    
    public static String cmsIT8GetPatchName(cmsHANDLE hIT8, int nPatch, StringBuffer buffer)
    {
    	cmsIT8 it8 = (cmsIT8)hIT8;
    	TABLE t;
    	VirtualPointer Data;
    	
    	lcms2_internal._cmsAssert(hIT8 != null, "hIT8 != null");
    	
    	t = GetTable(it8);
    	Data = GetData(it8, nPatch, t.SampleID);
    	
    	if (Data == null)
    	{
    		return null;
    	}
    	if (buffer == null)
    	{
    		return Data.getProcessor().readString();
    	}
    	
    	buffer.ensureCapacity(MAXSTR);
    	Utility.strncpy(buffer, Data, MAXSTR-1);
    	if(buffer.length() > MAXSTR-1)
    	{
    		buffer.setCharAt(MAXSTR-1, '\0');
    	}
    	String str = buffer.toString();
    	return str.substring(0, Utility.strlen(str));
    }
    
    public static int cmsIT8GetPatchByName(cmsHANDLE hIT8, final String cPatch)
    {
    	lcms2_internal._cmsAssert(hIT8 != null, "hIT8 != null");
    	
        return LocatePatch((cmsIT8)hIT8, cPatch);
    }
    
    public static int cmsIT8TableCount(cmsHANDLE hIT8)
    {
    	cmsIT8 it8 = (cmsIT8)hIT8;
    	
    	lcms2_internal._cmsAssert(hIT8 != null, "hIT8 != null");
    	
    	return it8.TablesCount;
    }
    
    // This handles the "LABEL" extension.
    // Label, nTable, Type
    
    public static int cmsIT8SetTableByLabel(cmsHANDLE hIT8, final String cSet, String cField, String ExpectedType)
    {
        final String cLabelFld;
        char[] Type = new char[256], Label = new char[256];
        int[] nTable = new int[1];
        
        lcms2_internal._cmsAssert(hIT8 != null, "hIT8 != null");
        
        if (cField != null && cField.charAt(0) == 0)
        {
        	cField = "LABEL";
        }
        
        if (cField == null)
        {
        	cField = "LABEL";
        }
        
        cLabelFld = cmsIT8GetData(hIT8, cSet, cField);
        if (cLabelFld == null)
        {
        	return -1;
        }
        
        if (Utility.sscanf(cLabelFld, "%255s %d %255s", new Object[]{Label, nTable, Type}) != 3)
        {
        	return -1;
        }
        
        if (ExpectedType != null && ExpectedType.charAt(0) == 0)
        {
        	ExpectedType = null;
        }

        if (ExpectedType != null)
        {
            if (cmserr.cmsstrcasecmp(Utility.cstringCreation(Type), ExpectedType) != 0)
            {
            	return -1;
            }
        }
        
        return cmsIT8SetTable(hIT8, nTable[0]);
    }
    
    public static boolean cmsIT8SetIndexColumn(cmsHANDLE hIT8, final String cSample)
    {
    	cmsIT8 it8 = (cmsIT8)hIT8;
    	int pos;
    	
    	lcms2_internal._cmsAssert(hIT8 != null, "hIT8 != null");
    	
    	pos = LocateSample(it8, cSample);
    	if(pos == -1)
    	{
    		return false;
    	}
    	
    	it8.Tab[it8.nTable].SampleID = pos;
    	return true;
    }


    public static void cmsIT8DefineDblFormat(cmsHANDLE hIT8, final String Formatter)
    {
        cmsIT8 it8 = (cmsIT8)hIT8;
        
        lcms2_internal._cmsAssert(hIT8 != null, "hIT8 != null");
        
        if (Formatter == null)
        {
        	Utility.strcpy(it8.DoubleFormatter, DEFAULT_DBL_FORMAT);
        }
        else
        {
        	Utility.strcpy(it8.DoubleFormatter, Formatter);
        }
        
        it8.DoubleFormatter[it8.DoubleFormatter.length-1] = 0;
    }
}
