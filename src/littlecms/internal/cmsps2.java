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

import java.util.Calendar;

import net.rim.device.api.util.Arrays;

import littlecms.internal.helper.Utility;
import littlecms.internal.lcms2.cmsCIELab;
import littlecms.internal.lcms2.cmsCIEXYZ;
import littlecms.internal.lcms2.cmsContext;
import littlecms.internal.lcms2.cmsHPROFILE;
import littlecms.internal.lcms2.cmsHTRANSFORM;
import littlecms.internal.lcms2.cmsIOHANDLER;
import littlecms.internal.lcms2.cmsMLU;
import littlecms.internal.lcms2.cmsNAMEDCOLORLIST;
import littlecms.internal.lcms2.cmsPipeline;
import littlecms.internal.lcms2.cmsSAMPLER16;
import littlecms.internal.lcms2.cmsStage;
import littlecms.internal.lcms2.cmsToneCurve;
import littlecms.internal.lcms2_internal._cmsStageCLutData;
import littlecms.internal.lcms2_internal._cmsStageMatrixData;
import littlecms.internal.lcms2_internal._cmsTRANSFORM;

//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmsps2
{
	// PostScript ColorRenderingDictionary and ColorSpaceArray
	
	private static final int MAXPSCOLS = 60;// Columns on tables
	
	/*
	  Implementation
	  --------------
	
	  PostScript does use XYZ as its internal PCS. But since PostScript 
	  interpolation tables are limited to 8 bits, I use Lab as a way to 
	  improve the accuracy, favoring perceptual results. So, for the creation 
	  of each CRD, CSA the profiles are converted to Lab via a device 
	  link between  profile -> Lab or Lab -> profile. The PS code necessary to
	  convert Lab <-> XYZ is also included.
	
	
	
	  Color Space Arrays (CSA) 
	  ==================================================================================
	
	  In order to obtain precision, code chooses between three ways to implement
	  the device -> XYZ transform. These cases identifies monochrome profiles (often
	  implemented as a set of curves), matrix-shaper and Pipeline-based.
	
	  Monochrome 
	  -----------
	
	  This is implemented as /CIEBasedA CSA. The prelinearization curve is 
	  placed into /DecodeA section, and matrix equals to D50. Since here is
	  no interpolation tables, I do the conversion directly to XYZ
	
	  NOTE: CLUT-based monochrome profiles are NOT supported. So, cmsFLAGS_MATRIXINPUT
	  flag is forced on such profiles.
	
	    [ /CIEBasedA
	      <<            
	            /DecodeA { transfer function } bind
	            /MatrixA [D50]  
	            /RangeLMN [ 0.0 cmsD50X 0.0 cmsD50Y 0.0 cmsD50Z ]
	            /WhitePoint [D50]
	            /BlackPoint [BP]
	            /RenderingIntent (intent)
	      >>
	    ] 
	
	   On simpler profiles, the PCS is already XYZ, so no conversion is required.
	
	 
	   Matrix-shaper based
	   -------------------
	
	   This is implemented both with /CIEBasedABC or /CIEBasedDEF on dependig
	   of profile implementation. Since here there are no interpolation tables, I do 
	   the conversion directly to XYZ
	
	
	
	    [ /CIEBasedABC
	            <<
	                /DecodeABC [ {transfer1} {transfer2} {transfer3} ]
	                /MatrixABC [Matrix]
	                /RangeLMN [ 0.0 cmsD50X 0.0 cmsD50Y 0.0 cmsD50Z ]
	                /DecodeLMN [ { / 2} dup dup ]
	                /WhitePoint [D50]
	                /BlackPoint [BP]
	                /RenderingIntent (intent)
	            >>
	    ] 
	
	
	    CLUT based
	    ----------
	
	     Lab is used in such cases.
	
	    [ /CIEBasedDEF
	            <<
	            /DecodeDEF [ <prelinearization> ]
	            /Table [ p p p [<...>]]
	            /RangeABC [ 0 1 0 1 0 1]
	            /DecodeABC[ <postlinearization> ]
	            /RangeLMN [ -0.236 1.254 0 1 -0.635 1.640 ] 
	               % -128/500 1+127/500 0 1  -127/200 1+128/200 
	            /MatrixABC [ 1 1 1 1 0 0 0 0 -1]
	            /WhitePoint [D50]
	            /BlackPoint [BP]
	            /RenderingIntent (intent)
	    ] 
	
	
	  Color Rendering Dictionaries (CRD)
	  ==================================
	  These are always implemented as CLUT, and always are using Lab. Since CRD are expected to
	  be used as resources, the code adds the definition as well.
	
	  <<
	    /ColorRenderingType 1
	    /WhitePoint [ D50 ]
	    /BlackPoint [BP]
	    /MatrixPQR [ Bradford ]
	    /RangePQR [-0.125 1.375 -0.125 1.375 -0.125 1.375 ]
	    /TransformPQR [            
	    {4 index 3 get div 2 index 3 get mul exch pop exch pop exch pop exch pop } bind
	    {4 index 4 get div 2 index 4 get mul exch pop exch pop exch pop exch pop } bind
	    {4 index 5 get div 2 index 5 get mul exch pop exch pop exch pop exch pop } bind
	    ]
	    /MatrixABC <...>
	    /EncodeABC <...>
	    /RangeABC  <.. used for  XYZ -> Lab>
	    /EncodeLMN
	    /RenderTable [ p p p [<...>]]   
	    
	    /RenderingIntent (Perceptual)
	  >> 
	  /Current exch /ColorRendering defineresource pop
	
	
	  The following stages are used to convert from XYZ to Lab
	  --------------------------------------------------------  
	
	  Input is given at LMN stage on X, Y, Z
	
	  Encode LMN gives us f(X/Xn), f(Y/Yn), f(Z/Zn)
	
	  /EncodeLMN [
	
	    { 0.964200  div dup 0.008856 le {7.787 mul 16 116 div add}{1 3 div exp} ifelse } bind
	    { 1.000000  div dup 0.008856 le {7.787 mul 16 116 div add}{1 3 div exp} ifelse } bind
	    { 0.824900  div dup 0.008856 le {7.787 mul 16 116 div add}{1 3 div exp} ifelse } bind
	
	    ]
	    
	      
	  MatrixABC is used to compute f(Y/Yn), f(X/Xn) - f(Y/Yn), f(Y/Yn) - f(Z/Zn)
	
	  | 0  1  0|
	  | 1 -1  0|
	  | 0  1 -1|
	
	  /MatrixABC [ 0 1 0 1 -1 1 0 0 -1 ]
	
	 EncodeABC finally gives Lab values.
	
	  /EncodeABC [
	    { 116 mul  16 sub 100 div  } bind
	    { 500 mul 128 add 255 div  } bind
	    { 200 mul 128 add 255 div  } bind
	    ]   
	    
	  The following stages are used to convert Lab to XYZ
	  ----------------------------------------------------
	
	    /RangeABC [ 0 1 0 1 0 1]
	    /DecodeABC [ { 100 mul 16 add 116 div } bind
	                 { 255 mul 128 sub 500 div } bind
	                 { 255 mul 128 sub 200 div } bind 
	               ]
	    
	    /MatrixABC [ 1 1 1 1 0 0 0 0 -1]
	    /DecodeLMN [
	                {dup 6 29 div ge {dup dup mul mul} {4 29 div sub 108 841 div mul} ifelse 0.964200 mul} bind
	                {dup 6 29 div ge {dup dup mul mul} {4 29 div sub 108 841 div mul} ifelse } bind
	                {dup 6 29 div ge {dup dup mul mul} {4 29 div sub 108 841 div mul} ifelse 0.824900 mul} bind
	                ]
	
	
	*/
	
	/*
	
	 PostScript algorithms discussion.
	 =========================================================================================================
	
	  1D interpolation algorithm 
	
	
	  1D interpolation (float)
	  ------------------------
	    
	    val2 = Domain * Value;
	
	    cell0 = (int) floor(val2);
	    cell1 = (int) ceil(val2);
	
	    rest = val2 - cell0;
	
	    y0 = LutTable[cell0] ;
	    y1 = LutTable[cell1] ;
	
	    y = y0 + (y1 - y0) * rest;
	
	
	  
	  PostScript code                   Stack
	  ================================================
	
	  {                                 % v
	    <check 0..1.0>
	    [array]                         % v tab
	    dup                             % v tab tab
	    length 1 sub                    % v tab dom
	
	    3 -1 roll                       % tab dom v
	
	    mul                             % tab val2
	    dup                             % tab val2 val2
	    dup                             % tab val2 val2 val2
	    floor cvi                       % tab val2 val2 cell0
	    exch                            % tab val2 cell0 val2
	    ceiling cvi                     % tab val2 cell0 cell1
	
	    3 index                         % tab val2 cell0 cell1 tab  
	    exch                            % tab val2 cell0 tab cell1
	    get                             % tab val2 cell0 y1
	
	    4 -1 roll                       % val2 cell0 y1 tab
	    3 -1 roll                       % val2 y1 tab cell0 
	    get                             % val2 y1 y0 
	
	    dup                             % val2 y1 y0 y0
	    3 1 roll                        % val2 y0 y1 y0 
	
	    sub                             % val2 y0 (y1-y0)
	    3 -1 roll                       % y0 (y1-y0) val2
	    dup                             % y0 (y1-y0) val2 val2
	    floor cvi                       % y0 (y1-y0) val2 floor(val2) 
	    sub                             % y0 (y1-y0) rest
	    mul                             % y0 t1
	    add                             % y
	    65535 div                       % result
	
	  } bind
	
	
	*/
	
	// This struct holds the memory block currently being write
	private static class cmsPsSamplerCargo
	{
		public _cmsStageCLutData Pipeline;
		public cmsIOHANDLER m;
		
		public int FirstComponent;
		public int SecondComponent;
		
		public String PreMaj;
		public String PostMaj;
		public String PreMin;
		public String PostMin;
		
		public boolean FixWhite; // Force mapping of pure white 
		
		public int ColorSpace;  // ColorSpace of profile
	}
	
	private static int[] _cmsPSActualColumn;
	
	private static final long CMS_PS2_COLUMN = 0xBA4B943359533558L;
	
	static
	{
		Object obj;
		if((obj = Utility.singletonStorageGet(CMS_PS2_COLUMN)) != null)
		{
			_cmsPSActualColumn = (int[])obj;
		}
		else
		{
			_cmsPSActualColumn = new int[1];
			Utility.singletonStorageSet(CMS_PS2_COLUMN, _cmsPSActualColumn);
		}
	}
	
	// Convert to byte
	private static byte Word2Byte(short w)
	{
	    return (byte)Math.floor(w / 257.0 + 0.5);
	}
	
	// Convert to byte (using ICC2 notation)
	/*
	private static byte L2Byte(short w)
	{    
		int ww = w + 0x0080;
		
		if ((ww & 0xFFFF) > 0xFFFF)
		{
			return (byte)0xFF;
		}
		
	    return (byte)((short)(ww >> 8) & 0xFF);
	}
	*/
	
	// Write a cooked byte
	
	private static void WriteByte(cmsIOHANDLER m, byte b)
	{
		cmsplugin._cmsIOPrintf(m, "%02x", new Object[]{new Byte(b)});	
		_cmsPSActualColumn[0] += 2;
		
		if (_cmsPSActualColumn[0] > MAXPSCOLS)
		{
			cmsplugin._cmsIOPrintf(m, "\n", null);
			_cmsPSActualColumn[0] = 0;
		}
	}
	
	// ----------------------------------------------------------------- PostScript generation
	
	// Removes offending Carriage returns
	private static String RemoveCR(final String txt)
	{
	    char[] Buffer = new char[2048];
	    int pt;
	    
	    Utility.strncpy(Buffer, txt, 2047);
	    Buffer[2047] = 0;
	    for (pt = 0; Buffer[pt] != 0; pt++)
	    {
	    	if (Buffer[pt] == '\n' || Buffer[pt] == '\r')
	    	{
	    		Buffer[pt] = ' ';
	    	}
	    }
	    
	    return Utility.cstringCreation(Buffer);
	}
	
	private static void EmitHeader(cmsIOHANDLER m, final String Title, cmsHPROFILE hProfile)
	{
	    Calendar timer;
		cmsMLU Description, Copyright;
		StringBuffer DescASCII = new StringBuffer(new String(new char[256])), CopyrightASCII = new StringBuffer(new String(new char[256]));
	    
		timer = Calendar.getInstance();
		
		Description = (cmsMLU)cmsio0.cmsReadTag(hProfile, lcms2.cmsSigProfileDescriptionTag);
		Copyright   = (cmsMLU)cmsio0.cmsReadTag(hProfile, lcms2.cmsSigCopyrightTag);
	    
		if (Description != null)
		{
			cmsnamed.cmsMLUgetASCII(Description, lcms2.cmsNoLanguage, lcms2.cmsNoCountry, DescASCII, 255);
		}
		if (Copyright != null)
		{
			cmsnamed.cmsMLUgetASCII(Copyright, lcms2.cmsNoLanguage, lcms2.cmsNoCountry, CopyrightASCII, 255);
		}
		
	    cmsplugin._cmsIOPrintf(m, "%%!PS-Adobe-3.0\n", null);
	    cmsplugin._cmsIOPrintf(m, "%%\n", null);
	    cmsplugin._cmsIOPrintf(m, "%% %s\n", new Object[]{Title});
	    cmsplugin._cmsIOPrintf(m, "%% Source: %s\n", new Object[]{RemoveCR(DescASCII.toString())});
	    cmsplugin._cmsIOPrintf(m, "%%         %s\n", new Object[]{RemoveCR(CopyrightASCII.toString())});
	    cmsplugin._cmsIOPrintf(m, "%% Created: %s", new Object[]{Utility.ctime(timer)}); // ctime appends a \n!!!
	    cmsplugin._cmsIOPrintf(m, "%%\n", null);
	    cmsplugin._cmsIOPrintf(m, "%%%%BeginResource\n", null);
	}
	
	// Emits White & Black point. White point is always D50, Black point is the device 
	// Black point adapted to D50. 
	
	private static void EmitWhiteBlackD50(cmsIOHANDLER m, cmsCIEXYZ BlackPoint)
	{
	    cmsplugin._cmsIOPrintf(m, "/BlackPoint [%f %f %f]\n", new Object[]{new Double(BlackPoint.X), new Double(BlackPoint.Y), new Double(BlackPoint.Z)});
	    
	    cmsplugin._cmsIOPrintf(m, "/WhitePoint [%f %f %f]\n", new Object[]{new Double(lcms2.cmsD50_XYZ.X), new Double(lcms2.cmsD50_XYZ.Y), new Double(lcms2.cmsD50_XYZ.Z)});
	}
	
	private static void EmitRangeCheck(cmsIOHANDLER m)
	{
		cmsplugin._cmsIOPrintf(m, "dup 0.0 lt { pop 0.0 } if " + 
	                    		  "dup 1.0 gt { pop 1.0 } if ", null);
	}
	
	// Does write the intent
	
	private static void EmitIntent(cmsIOHANDLER m, int RenderingIntent)
	{
	    final String intent;
	    
	    switch (RenderingIntent)
	    {
	        case lcms2.INTENT_PERCEPTUAL:            intent = "Perceptual"; break;
	        case lcms2.INTENT_RELATIVE_COLORIMETRIC: intent = "RelativeColorimetric"; break;
	        case lcms2.INTENT_ABSOLUTE_COLORIMETRIC: intent = "AbsoluteColorimetric"; break;
	        case lcms2.INTENT_SATURATION:            intent = "Saturation"; break;

	        default: intent = "Undefined"; break;
	    }
	    
	    cmsplugin._cmsIOPrintf(m, "/RenderingIntent (%s)\n", new Object[]{intent});    
	}
	
	//
	//  Convert L* to Y
	//
	//      Y = Yn*[ (L* + 16) / 116] ^ 3   if (L*) >= 6 / 29
	//        = Yn*( L* / 116) / 7.787      if (L*) < 6 / 29
	//
	
	/*
	private static void EmitL2Y(cmsIOHANDLER m)
	{
		cmsplugin._cmsIOPrintf(m, 
	            "{ " +
	                "100 mul 16 add 116 div " +             // (L * 100 + 16) / 116
	                 "dup 6 29 div ge "       +             // >= 6 / 29 ?          
	                 "{ dup dup mul mul } "   +             // yes, ^3 and done
	                 "{ 4 29 div sub 108 841 div mul } " +  // no, slope limiting
	            "ifelse } bind ", null); 
	}
	*/
	
	// Lab -> XYZ, see the discussion above
	
	private static void EmitLab2XYZ(cmsIOHANDLER m)
	{
		cmsplugin._cmsIOPrintf(m, "/RangeABC [ 0 1 0 1 0 1]\n", null);
		cmsplugin._cmsIOPrintf(m, "/DecodeABC [\n", null);
		cmsplugin._cmsIOPrintf(m, "{100 mul  16 add 116 div } bind\n", null);
		cmsplugin._cmsIOPrintf(m, "{255 mul 128 sub 500 div } bind\n", null);
	    cmsplugin._cmsIOPrintf(m, "{255 mul 128 sub 200 div } bind\n", null);
	    cmsplugin._cmsIOPrintf(m, "]\n", null);
	    cmsplugin._cmsIOPrintf(m, "/MatrixABC [ 1 1 1 1 0 0 0 0 -1]\n", null);
	    cmsplugin._cmsIOPrintf(m, "/RangeLMN [ -0.236 1.254 0 1 -0.635 1.640 ]\n", null); 
	    cmsplugin._cmsIOPrintf(m, "/DecodeLMN [\n", null);
	    cmsplugin._cmsIOPrintf(m, "{dup 6 29 div ge {dup dup mul mul} {4 29 div sub 108 841 div mul} ifelse 0.964200 mul} bind\n", null);
	    cmsplugin._cmsIOPrintf(m, "{dup 6 29 div ge {dup dup mul mul} {4 29 div sub 108 841 div mul} ifelse } bind\n", null);
	    cmsplugin._cmsIOPrintf(m, "{dup 6 29 div ge {dup dup mul mul} {4 29 div sub 108 841 div mul} ifelse 0.824900 mul} bind\n", null);
	    cmsplugin._cmsIOPrintf(m, "]\n", null);
	}
	
	// Outputs a table of words. It does use 16 bits
	
	private static void Emit1Gamma(cmsIOHANDLER m, cmsToneCurve Table)
	{
	    int i;
	    double gamma;
	    
	    if (Table.nEntries <= 0)
	    {
	    	return; // Empty table
	    }
	    
	    // Suppress whole if identity
	    if (cmsgamma.cmsIsToneCurveLinear(Table))
	    {
	    	return;
	    }
	    
	    // Check if is really an exponential. If so, emit "exp"
		gamma = cmsgamma.cmsEstimateGamma(Table, 0.001);
		if (gamma > 0)
		{
			cmsplugin._cmsIOPrintf(m, "{ %g exp } bind ", new Object[]{new Double(gamma)});
			return;
		}
		
		cmsplugin._cmsIOPrintf(m, "{ ", null);
		
	    // Bounds check
	    EmitRangeCheck(m);
	    
	    // Emit intepolation code
	    
	    // PostScript code                      Stack
	    // ===============                      ========================
	                                            // v
	    cmsplugin._cmsIOPrintf(m, " [", null);
	    
	    for (i=0; i < Table.nEntries; i++)
	    {
	    	cmsplugin._cmsIOPrintf(m, "%d ", new Object[]{new Short(Table.Table16[i])});
	    }
	    
	    cmsplugin._cmsIOPrintf(m, "] ", null);                        // v tab
	    
	    cmsplugin._cmsIOPrintf(m, "dup ", null);                      // v tab tab        
	    cmsplugin._cmsIOPrintf(m, "length 1 sub ", null);             // v tab dom
	    cmsplugin._cmsIOPrintf(m, "3 -1 roll ", null);                // tab dom v
	    cmsplugin._cmsIOPrintf(m, "mul ", null);                      // tab val2
	    cmsplugin._cmsIOPrintf(m, "dup ", null);                      // tab val2 val2
	    cmsplugin._cmsIOPrintf(m, "dup ", null);                      // tab val2 val2 val2
	    cmsplugin._cmsIOPrintf(m, "floor cvi ", null);                // tab val2 val2 cell0
	    cmsplugin._cmsIOPrintf(m, "exch ", null);                     // tab val2 cell0 val2
	    cmsplugin._cmsIOPrintf(m, "ceiling cvi ", null);              // tab val2 cell0 cell1
	    cmsplugin._cmsIOPrintf(m, "3 index ", null);                  // tab val2 cell0 cell1 tab 
	    cmsplugin._cmsIOPrintf(m, "exch ", null);                     // tab val2 cell0 tab cell1
	    cmsplugin._cmsIOPrintf(m, "get ", null);                      // tab val2 cell0 y1
	    cmsplugin._cmsIOPrintf(m, "4 -1 roll ", null);                // val2 cell0 y1 tab
	    cmsplugin._cmsIOPrintf(m, "3 -1 roll ", null);                // val2 y1 tab cell0 
	    cmsplugin._cmsIOPrintf(m, "get ", null);                      // val2 y1 y0 
	    cmsplugin._cmsIOPrintf(m, "dup ", null);                      // val2 y1 y0 y0
	    cmsplugin._cmsIOPrintf(m, "3 1 roll ", null);                 // val2 y0 y1 y0 
	    cmsplugin._cmsIOPrintf(m, "sub ", null);                      // val2 y0 (y1-y0)
	    cmsplugin._cmsIOPrintf(m, "3 -1 roll ", null);                // y0 (y1-y0) val2
	    cmsplugin._cmsIOPrintf(m, "dup ", null);                      // y0 (y1-y0) val2 val2
	    cmsplugin._cmsIOPrintf(m, "floor cvi ", null);                // y0 (y1-y0) val2 floor(val2) 
	    cmsplugin._cmsIOPrintf(m, "sub ", null);                      // y0 (y1-y0) rest
	    cmsplugin._cmsIOPrintf(m, "mul ", null);                      // y0 t1
	    cmsplugin._cmsIOPrintf(m, "add ", null);                      // y
	    cmsplugin._cmsIOPrintf(m, "65535 div ", null);                // result
	    
	    cmsplugin._cmsIOPrintf(m, " } bind ", null);
	}
	
	// Compare gamma table
	
	private static boolean GammaTableEquals(short[] g1, short[] g2, int nEntries)
	{
		if(g1.length == nEntries && g2.length == nEntries)
		{
			return Arrays.equals(g1, g2);
		}
		for(int i = 0; i < nEntries; i++)
		{
			if(g1[i] != g2[i])
			{
				return false;
			}
		}
		return true;
	}
	
	// Does write a set of gamma curves
	
	private static void EmitNGamma(cmsIOHANDLER m, int n, cmsToneCurve[] g)                  
	{
	    int i;
	    
	    for(i=0; i < n; i++)
	    {                
			if (i > 0 && GammaTableEquals(g[i-1].Table16, g[i].Table16, g[i].nEntries))
			{
	            cmsplugin._cmsIOPrintf(m, "dup ", null);
	        }
	        else
	        {    
				Emit1Gamma(m, g[i]);
	        }
	    }
	}
	
	// Following code dumps a LUT onto memory stream
	
	
	// This is the sampler. Intended to work in SAMPLER_INSPECT mode,
	// that is, the callback will be called for each knot with
	//
	//	          In[]  The grid location coordinates, normalized to 0..ffff
	//	          Out[] The Pipeline values, normalized to 0..ffff
	//
	//  Returning a value other than 0 does terminate the sampling process
	//
	//  Each row contains Pipeline values for all but first component. So, I 
	//  detect row changing by keeping a copy of last value of first 
	//  component. -1 is used to mark begining of whole block.
	
	private static final cmsSAMPLER16 OutputValueSampler = new cmsSAMPLER16()
	{
		public int run(short[] In, short[] Out, Object Cargo)
		{
			cmsPsSamplerCargo sc = (cmsPsSamplerCargo)Cargo;
		    int i;
		    
		    if (sc.FixWhite)
		    {
		        if (In[0] == (short)0xFFFF) // Only in L* = 100, ab = [-8..8]
		        {
		            if (((In[1] & 0xFFFF) >= 0x7800 && (In[1] & 0xFFFF) <= 0x8800) &&
		                ((In[2] & 0xFFFF) >= 0x7800 && (In[2] & 0xFFFF) <= 0x8800))
		            {
		                short[] Black;
		                short[] White;
		                int[] nOutputs = new int[1];
		                
		                if (!cmspcs._cmsEndPointsBySpace(sc.ColorSpace, null, null, nOutputs))
		                {
		                	return 0;
		                }
		                White = new short[nOutputs[0]];
		                Black = new short[nOutputs[0]];
		                
		                if (!cmspcs._cmsEndPointsBySpace(sc.ColorSpace, White, Black, nOutputs))
		                {
		                	return 0;
		                }
		                
		                for (i=0; i < nOutputs[0]; i++)
		                {
		                	Out[i] = White[i];
		                }
		            }
		        }
		    }
		    
		    // Hadle the parenthesis on rows
		    
		    if ((In[0] & 0xFFFF) != sc.FirstComponent)
		    {
		    	if (sc.FirstComponent != -1)
		    	{
		    		cmsplugin._cmsIOPrintf(sc.m, sc.PostMin, null);
		    		sc.SecondComponent = -1;
		    		cmsplugin._cmsIOPrintf(sc.m, sc.PostMaj, null);
		    	}
		    	
		    	// Begin block
		    	_cmsPSActualColumn[0] = 0;
		    	
		    	cmsplugin._cmsIOPrintf(sc.m, sc.PreMaj, null);
		    	sc.FirstComponent = (In[0] & 0xFFFF);
		    }
		    
		    if ((In[1] & 0xFFFF) != sc.SecondComponent)
		    {
		    	if (sc.SecondComponent != -1)
		    	{
		    		cmsplugin._cmsIOPrintf(sc.m, sc.PostMin, null);
	    		}
		    	
		    	cmsplugin._cmsIOPrintf(sc.m, sc.PreMin, null);
		    	sc.SecondComponent = (In[1] & 0xFFFF);
		    }
		    
		    // Dump table.
		    
		    for (i=0; i < sc.Pipeline.Params.nOutputs; i++)
		    {
		    	short wWordOut = Out[i];
		    	byte wByteOut; // Value as byte
		    	
		    	// We always deal with Lab4
		    	
		    	wByteOut = Word2Byte(wWordOut);
		    	WriteByte(sc.m, wByteOut);
		    }
		    
		    return 1;
		}
	};
	
	// Writes a Pipeline on memstream. Could be 8 or 16 bits based
	
	private static void WriteCLUT(cmsIOHANDLER m, cmsStage mpe, final String PreMaj, final String PostMaj, final String PreMin, final String PostMin, boolean FixWhite, 
			int ColorSpace)
	{
	    int i;
	    cmsPsSamplerCargo sc = new cmsPsSamplerCargo();
	    
	    sc.FirstComponent = -1;
	    sc.SecondComponent = -1;
		sc.Pipeline = (_cmsStageCLutData)mpe.Data;
	    sc.m   = m;    
	    sc.PreMaj = PreMaj;
	    sc.PostMaj= PostMaj;
	    
	    sc.PreMin   = PreMin;
	    sc.PostMin  = PostMin;    
	    sc.FixWhite = FixWhite;
	    sc.ColorSpace = ColorSpace;
	    
	    cmsplugin._cmsIOPrintf(m, "[", null);
	    
		for (i=0; i < sc.Pipeline.Params.nInputs; i++)
		{
			cmsplugin._cmsIOPrintf(m, " %d ", new Object[]{new Integer(sc.Pipeline.Params.nSamples[i])});
		}
		
		cmsplugin._cmsIOPrintf(m, " [\n", null);
	    
	    cmslut.cmsStageSampleCLut16bit(mpe, OutputValueSampler, sc, lcms2.SAMPLER_INSPECT);
	    
	    cmsplugin._cmsIOPrintf(m, PostMin, null);
	    cmsplugin._cmsIOPrintf(m, PostMaj, null);
	    cmsplugin._cmsIOPrintf(m, "] ", null);
	}
	
	// Dumps CIEBasedA Color Space Array
	
	private static int EmitCIEBasedA(cmsIOHANDLER m, cmsToneCurve Curve, cmsCIEXYZ BlackPoint)
	{
		cmsplugin._cmsIOPrintf(m, "[ /CIEBasedA\n", null);
		cmsplugin._cmsIOPrintf(m, "  <<\n", null);
		
		cmsplugin._cmsIOPrintf(m, "/DecodeA ", null);
		
		Emit1Gamma(m, Curve);
		
		cmsplugin._cmsIOPrintf(m, " \n", null);
		
		cmsplugin._cmsIOPrintf(m, "/MatrixA [ 0.9642 1.0000 0.8249 ]\n", null);
		cmsplugin._cmsIOPrintf(m, "/RangeLMN [ 0.0 0.9642 0.0 1.0000 0.0 0.8249 ]\n", null);
		
		EmitWhiteBlackD50(m, BlackPoint);
		EmitIntent(m, lcms2.INTENT_PERCEPTUAL);
		
		cmsplugin._cmsIOPrintf(m, ">>\n", null);        
		cmsplugin._cmsIOPrintf(m, "]\n", null);
		
		return 1;
	}
	
	// Dumps CIEBasedABC Color Space Array
	
	private static int EmitCIEBasedABC(cmsIOHANDLER m, double[] Matrix, cmsToneCurve[] CurveSet, cmsCIEXYZ BlackPoint)
	{
		int i;
		
		cmsplugin._cmsIOPrintf(m, "[ /CIEBasedABC\n", null);
		cmsplugin._cmsIOPrintf(m, "<<\n", null);
		cmsplugin._cmsIOPrintf(m, "/DecodeABC [ ", null);
		
		EmitNGamma(m, 3, CurveSet);
		
		cmsplugin._cmsIOPrintf(m, "]\n", null);
		
		cmsplugin._cmsIOPrintf(m, "/MatrixABC [ ", null);
		
		for( i=0; i < 3; i++ )
		{
			cmsplugin._cmsIOPrintf(m, "%.6f %.6f %.6f ", new Object[]{new Double(Matrix[0 + 3*i]), new Double(Matrix[1 + 3*i]), new Double(Matrix[2 + 3*i])});		
		}
		
		cmsplugin._cmsIOPrintf(m, "]\n", null);
		
		cmsplugin._cmsIOPrintf(m, "/RangeLMN [ 0.0 0.9642 0.0 1.0000 0.0 0.8249 ]\n", null);
		
		EmitWhiteBlackD50(m, BlackPoint);
		EmitIntent(m, lcms2.INTENT_PERCEPTUAL);
		
		cmsplugin._cmsIOPrintf(m, ">>\n", null);
		cmsplugin._cmsIOPrintf(m, "]\n", null);
		
		return 1;
	}
	
	private static int EmitCIEBasedDEF(cmsIOHANDLER m, cmsPipeline Pipeline, int Intent, cmsCIEXYZ BlackPoint)
	{
	    String PreMaj;
	    String PostMaj;
	    String PreMin, PostMin;
		cmsStage mpe;
		
		mpe = Pipeline.Elements;
		
		switch (cmslut.cmsStageInputChannels(mpe))
		{
		    case 3:
		    	cmsplugin._cmsIOPrintf(m, "[ /CIEBasedDEF\n", null);
		    	PreMaj ="<";
		    	PostMaj= ">\n";
		    	PreMin = PostMin = "";
		    	break;
		    case 4:
		    	cmsplugin._cmsIOPrintf(m, "[ /CIEBasedDEFG\n", null);
		    	PreMaj = "[";
		    	PostMaj = "]\n";
		    	PreMin = "<";
		    	PostMin = ">\n";
		    	break;
		    default:
		    	return 0;
	    }
		
		cmsplugin._cmsIOPrintf(m, "<<\n", null);
	    
		if (cmslut.cmsStageType(mpe) == lcms2.cmsSigCurveSetElemType)
		{
			cmsplugin._cmsIOPrintf(m, "/DecodeDEF [ ", null);
			EmitNGamma(m, cmslut.cmsStageOutputChannels(mpe), cmslut._cmsStageGetPtrToCurveSet(mpe));
			cmsplugin._cmsIOPrintf(m, "]\n", null);
	        
			mpe = (cmsStage)mpe.Next;
	    }
		
		if (cmslut.cmsStageType(mpe) == lcms2.cmsSigCLutElemType)
		{
			cmsplugin._cmsIOPrintf(m, "/Table ", null);
			WriteCLUT(m, mpe, PreMaj, PostMaj, PreMin, PostMin, false, 0);
			cmsplugin._cmsIOPrintf(m, "]\n", null);
	    }
		
	    EmitLab2XYZ(m);
	    EmitWhiteBlackD50(m, BlackPoint);
	    EmitIntent(m, Intent);
	    
	    cmsplugin._cmsIOPrintf(m, "   >>\n", null);       
	    cmsplugin._cmsIOPrintf(m, "]\n", null);
	    
	    return 1;
	}
	
	// Generates a curve from a gray profile
	
	private static cmsToneCurve ExtractGray2Y(cmsContext ContextID, cmsHPROFILE hProfile, int Intent)
	{
	    cmsToneCurve Out = cmsgamma.cmsBuildTabulatedToneCurve16(ContextID, 256, null);
	    cmsHPROFILE hXYZ = cmsvirt.cmsCreateXYZProfile();
	    cmsHTRANSFORM xform = cmsxform.cmsCreateTransformTHR(ContextID, hProfile, lcms2.TYPE_GRAY_8, hXYZ, lcms2.TYPE_XYZ_DBL, Intent, lcms2.cmsFLAGS_NOOPTIMIZE);
	    int i;
	    
	    for (i=0; i < 256; i++)
	    {
	    	byte Gray = (byte)i;
	    	cmsCIEXYZ XYZ = new cmsCIEXYZ();
	    	
	        cmsxform.cmsDoTransform(xform, new Byte(Gray), XYZ, 1);
	        
			Out.Table16[i] = lcms2_internal._cmsQuickSaturateWord(XYZ.Y * 65535.0);
	    }
	    
	    cmsxform.cmsDeleteTransform(xform);
	    cmsio0.cmsCloseProfile(hXYZ);
	    return Out;
	}
	
	// Because PostScript has only 8 bits in /Table, we should use
	// a more perceptually uniform space... I do choose Lab.
	
	private static int WriteInputLUT(cmsIOHANDLER m, cmsHPROFILE hProfile, int Intent, int dwFlags)
	{
	    cmsHPROFILE hLab;
	    cmsHTRANSFORM xform;
	    int nChannels;
	    int InputFormat;
	    int rc;
	    cmsHPROFILE[] Profiles = new cmsHPROFILE[2];
	    cmsCIEXYZ BlackPointAdaptedToD50 = new cmsCIEXYZ();
	    
	    // Does create a device-link based transform. 
	    // The DeviceLink is next dumped as working CSA.
	    
	    InputFormat = cmspack.cmsFormatterForColorspaceOfProfile(hProfile, 2, false);
	    nChannels   = lcms2.T_CHANNELS(InputFormat);
	    
		cmssamp.cmsDetectBlackPoint(BlackPointAdaptedToD50, hProfile, Intent, 0);
		
	 	// Adjust output to Lab4 
	    hLab = cmsvirt.cmsCreateLab4ProfileTHR(m.ContextID, null);
	    
		Profiles[0] = hProfile;
		Profiles[1] = hLab;
		
		xform = cmsxform.cmsCreateMultiprofileTransform(Profiles, 2,  InputFormat, lcms2.TYPE_Lab_DBL, Intent, 0);
		cmsio0.cmsCloseProfile(hLab);
		
		if (xform == null)
		{
			cmserr.cmsSignalError(m.ContextID, lcms2.cmsERROR_COLORSPACE_CHECK, Utility.LCMS_Resources.getString(LCMSResource.CMSPS2_CANT_CREATE_XFORM_LAB), null);
			return 0;
		}
	    
	    // Only 1, 3 and 4 channels are allowed
		
	    switch (nChannels)
	    {
		    case 1:
			    {
			    	cmsToneCurve Gray2Y = ExtractGray2Y(m.ContextID, hProfile, Intent);
			    	EmitCIEBasedA(m, Gray2Y, BlackPointAdaptedToD50);
			    	cmsgamma.cmsFreeToneCurve(Gray2Y);
			    }
			    break;
			    
		    case 3:
		    case 4:
			    {
			    	int OutFrm = lcms2.TYPE_Lab_16;
			    	cmsPipeline DeviceLink;
			    	_cmsTRANSFORM v = (_cmsTRANSFORM)xform;
			    	
			    	DeviceLink = cmslut.cmsPipelineDup(v.Lut);
			    	if (DeviceLink == null)
			    	{
			    		return 0;
			    	}
			    	
			    	dwFlags |= lcms2.cmsFLAGS_FORCE_CLUT;
			    	cmsPipeline[] temp = new cmsPipeline[]{DeviceLink};
			    	cmsopt._cmsOptimizePipeline(new cmsPipeline[]{DeviceLink}, Intent, new int[]{InputFormat}, new int[]{OutFrm}, new int[]{dwFlags});
			    	DeviceLink = temp[0];
			    	
			    	rc = EmitCIEBasedDEF(m, DeviceLink, Intent, BlackPointAdaptedToD50);
			    	cmslut.cmsPipelineFree(DeviceLink);
			    }
			    break;
		    default:
				cmserr.cmsSignalError(m.ContextID, lcms2.cmsERROR_COLORSPACE_CHECK, Utility.LCMS_Resources.getString(LCMSResource.CMSPS2_CSA_INVALID_CHANNEL_COUNT), new Object[]{new Integer(nChannels)});
		        return 0;
	    }
	    
	    cmsxform.cmsDeleteTransform(xform);
	    
	    return 1;
	}
	
	private static double[] GetPtrToMatrix(final cmsStage mpe)
	{
	    _cmsStageMatrixData Data = (_cmsStageMatrixData)mpe.Data;
	    
	    return Data.Double;
	}
	
	// Does create CSA based on matrix-shaper. Allowed types are gray and RGB based
	
	private static int WriteInputMatrixShaper(cmsIOHANDLER m, cmsHPROFILE hProfile, cmsStage Matrix, cmsStage Shaper)
	{
	    int ColorSpace;
	    int rc;
	    cmsCIEXYZ BlackPointAdaptedToD50 = new cmsCIEXYZ();
	    
	    ColorSpace = cmsio0.cmsGetColorSpace(hProfile);
	    
	    cmssamp.cmsDetectBlackPoint(BlackPointAdaptedToD50, hProfile, lcms2.INTENT_RELATIVE_COLORIMETRIC, 0);
	    
	    if (ColorSpace == lcms2.cmsSigGrayData)
	    {
	    	cmsToneCurve[] ShaperCurve = cmslut._cmsStageGetPtrToCurveSet(Shaper);
	    	rc = EmitCIEBasedA(m, ShaperCurve[0], BlackPointAdaptedToD50);
	    }
	    else
	    {
	        if (ColorSpace == lcms2.cmsSigRgbData)
	        {
	            rc = EmitCIEBasedABC(m, GetPtrToMatrix(Matrix), 
				                        cmslut._cmsStageGetPtrToCurveSet(Shaper), 
										BlackPointAdaptedToD50);      
	        }
	        else 
	        {
	        	cmserr.cmsSignalError(m.ContextID, lcms2.cmsERROR_COLORSPACE_CHECK, Utility.LCMS_Resources.getString(LCMSResource.CMSPS2_CSA_INVALID_COLORSPACE), null);
	            return 0;
	        }
	    }
	    
	    return rc;
	}
	
	// Creates a PostScript color list from a named profile data. 
	// This is a HP extension, and it works in Lab instead of XYZ
	
	private static int WriteNamedColorCSA(cmsIOHANDLER m, cmsHPROFILE hNamedColor, int Intent)
	{
	    cmsHTRANSFORM xform;
	    cmsHPROFILE   hLab;
	    int i, nColors;
	    StringBuffer ColorName = new StringBuffer(32);
	    cmsNAMEDCOLORLIST NamedColorList;
	    
		hLab  = cmsvirt.cmsCreateLab4ProfileTHR(m.ContextID, null);
	    xform = cmsxform.cmsCreateTransform(hNamedColor, lcms2.TYPE_NAMED_COLOR_INDEX, hLab, lcms2.TYPE_Lab_DBL, Intent, 0);
	    if (xform == null)
	    {
	    	return 0;
	    }
	    
		NamedColorList = cmsnamed.cmsGetNamedColorList(xform);
	    if (NamedColorList == null)
	    {
	    	return 0;
	    }
	    
	    cmsplugin._cmsIOPrintf(m, "<<\n", null);
	    cmsplugin._cmsIOPrintf(m, "(colorlistcomment) (%s)\n", new Object[]{"Named color CSA"});
	    cmsplugin._cmsIOPrintf(m, "(Prefix) [ (Pantone ) (PANTONE ) ]\n", null);
	    cmsplugin._cmsIOPrintf(m, "(Suffix) [ ( CV) ( CVC) ( C) ]\n", null);

	    nColors = cmsnamed.cmsNamedColorCount(NamedColorList);
	    
	    for (i=0; i < nColors; i++)
	    {
	        short[] In = new short[1];
	        cmsCIELab Lab = new cmsCIELab();
	        
	        In[0] = (short)i;
	        
	        if (!cmsnamed.cmsNamedColorInfo(NamedColorList, i, ColorName, null, null, null, null))
	        {
	        	continue;
	        }
	        
	        cmsxform.cmsDoTransform(xform, In, Lab, 1);     
	        cmsplugin._cmsIOPrintf(m, "  (%s) [ %.3f %.3f %.3f ]\n", new Object[]{ColorName, new Double(Lab.L), new Double(Lab.a), new Double(Lab.b)});
	    }
	    
	    cmsplugin._cmsIOPrintf(m, ">>\n", null);
	    
	    cmsxform.cmsDeleteTransform(xform);
	    cmsio0.cmsCloseProfile(hLab);
	    return 1;
	}
	
	// Does create a Color Space Array on XYZ colorspace for PostScript usage
	private static int GenerateCSA(cmsContext ContextID, cmsHPROFILE hProfile, int Intent, int dwFlags, cmsIOHANDLER mem)
	{	
		int dwBytesUsed;
		cmsPipeline lut = null;
		cmsStage Matrix, Shaper;
		
		// Is a named color profile?
		if (cmsio0.cmsGetDeviceClass(hProfile) == lcms2.cmsSigNamedColorClass)
		{
			if (WriteNamedColorCSA(mem, hProfile, Intent) == 0)
			{
				return 0;
			}
		}
		else
		{
			// Any profile class are allowed (including devicelink), but
			// output (PCS) colorspace must be XYZ or Lab
			int ColorSpace = cmsio0.cmsGetPCS(hProfile);
			
			if (ColorSpace != lcms2.cmsSigXYZData &&
				ColorSpace != lcms2.cmsSigLabData)
			{
				cmserr.cmsSignalError(ContextID, lcms2.cmsERROR_COLORSPACE_CHECK, Utility.LCMS_Resources.getString(LCMSResource.CMSPS2_INVALID_OUT_COLORSPACE), null);
				return 0;
			}
			
			// Read the lut with all necessary conversion stages
			lut = cmsio1._cmsReadInputLUT(hProfile, Intent);
			if (lut == null)
			{
				return 0;
			}
			
			// Tone curves + matrix can be implemented without any LUT
			Object[] args = new Object[2 * 2];
			args[0] = new Integer(lcms2.cmsSigCurveSetElemType);
			args[1] = new Integer(lcms2.cmsSigMatrixElemType);
			if (cmslut.cmsPipelineCheckAndRetreiveStages(lut, 2, args))
			{
				Shaper = (cmsStage)args[2];
				Matrix = (cmsStage)args[3];
				if (WriteInputMatrixShaper(mem, hProfile, Matrix, Shaper) == 0)
				{
					if (lut != null)
					{
						cmslut.cmsPipelineFree(lut);
					}
					return 0;
				}
			}
			else
			{
				// We need a LUT for the rest
				if (WriteInputLUT(mem, hProfile, Intent, dwFlags) == 0)
				{
					if (lut != null)
					{
						cmslut.cmsPipelineFree(lut);
					}
					return 0;
				}
			}
		}
		
		// Done, keep memory usage
		dwBytesUsed = mem.UsedSpace;
		
		// Get rid of LUT
		if (lut != null)
		{
			cmslut.cmsPipelineFree(lut);
		}
		
		// Finally, return used byte count
		return dwBytesUsed;
	}
	
	// ------------------------------------------------------ Color Rendering Dictionary (CRD)
	
	/*

	  Black point compensation plus chromatic adaptation:

	  Step 1 - Chromatic adaptation
	  =============================

	          WPout
	    X = ------- PQR
	          Wpin

	  Step 2 - Black point compensation
	  =================================

	          (WPout - BPout)*X - WPout*(BPin - BPout)
	    out = --------------------------------------- 
	                        WPout - BPin


	  Algorithm discussion
	  ====================
	      
	  TransformPQR(WPin, BPin, WPout, BPout, PQR)

	  Wpin,etc= { Xws Yws Zws Pws Qws Rws }


	  Algorithm             Stack 0...n
	  ===========================================================
	                        PQR BPout WPout BPin WPin
	  4 index 3 get         WPin PQR BPout WPout BPin WPin  
	  div                   (PQR/WPin) BPout WPout BPin WPin   
	  2 index 3 get         WPout (PQR/WPin) BPout WPout BPin WPin   
	  mult                  WPout*(PQR/WPin) BPout WPout BPin WPin   
	  
	  2 index 3 get         WPout WPout*(PQR/WPin) BPout WPout BPin WPin   
	  2 index 3 get         BPout WPout WPout*(PQR/WPin) BPout WPout BPin WPin     
	  sub                   (WPout-BPout) WPout*(PQR/WPin) BPout WPout BPin WPin    
	  mult                  (WPout-BPout)* WPout*(PQR/WPin) BPout WPout BPin WPin   
	          
	  2 index 3 get         WPout (BPout-WPout)* WPout*(PQR/WPin) BPout WPout BPin WPin     
	  4 index 3 get         BPin WPout (BPout-WPout)* WPout*(PQR/WPin) BPout WPout BPin WPin     
	  3 index 3 get         BPout BPin WPout (BPout-WPout)* WPout*(PQR/WPin) BPout WPout BPin WPin
	  
	  sub                   (BPin-BPout) WPout (BPout-WPout)* WPout*(PQR/WPin) BPout WPout BPin WPin     
	  mult                  (BPin-BPout)*WPout (BPout-WPout)* WPout*(PQR/WPin) BPout WPout BPin WPin     
	  sub                   (BPout-WPout)* WPout*(PQR/WPin)-(BPin-BPout)*WPout BPout WPout BPin WPin     

	  3 index 3 get         BPin (BPout-WPout)* WPout*(PQR/WPin)-(BPin-BPout)*WPout BPout WPout BPin WPin     
	  3 index 3 get         WPout BPin (BPout-WPout)* WPout*(PQR/WPin)-(BPin-BPout)*WPout BPout WPout BPin WPin     
	  exch
	  sub                   (WPout-BPin) (BPout-WPout)* WPout*(PQR/WPin)-(BPin-BPout)*WPout BPout WPout BPin WPin       
	  div                
	  
	  exch pop 
	  exch pop
	  exch pop
	  exch pop

	*/
	
	private static void EmitPQRStage(cmsIOHANDLER m, cmsHPROFILE hProfile, boolean DoBPC, boolean lIsAbsolute)
	{
		if (lIsAbsolute)
		{
			// For absolute colorimetric intent, encode back to relative
			// and generate a relative Pipeline
			
			// Relative encoding is obtained across XYZpcs*(D50/WhitePoint)
			
			cmsCIEXYZ White = new cmsCIEXYZ();
			
			cmsio1._cmsReadMediaWhitePoint(White, hProfile);
			
			cmsplugin._cmsIOPrintf(m,"/MatrixPQR [1 0 0 0 1 0 0 0 1 ]\n", null);
			cmsplugin._cmsIOPrintf(m,"/RangePQR [ -0.5 2 -0.5 2 -0.5 2 ]\n", null);
			
			cmsplugin._cmsIOPrintf(m, "%% Absolute colorimetric -- encode to relative to maximize LUT usage\n" +
	                      "/TransformPQR [\n" +
	                      "{0.9642 mul %g div exch pop exch pop exch pop exch pop} bind\n" +
	                      "{1.0000 mul %g div exch pop exch pop exch pop exch pop} bind\n" +
	                      "{0.8249 mul %g div exch pop exch pop exch pop exch pop} bind\n]\n", 
						  new Object[]{new Double(White.X), new Double(White.Y), new Double(White.Z)});
			return;
		}
		
		cmsplugin._cmsIOPrintf(m,"%% Bradford Cone Space\n" +
	                 "/MatrixPQR [0.8951 -0.7502 0.0389 0.2664 1.7135 -0.0685 -0.1614 0.0367 1.0296 ] \n", null);
		
		cmsplugin._cmsIOPrintf(m, "/RangePQR [ -0.5 2 -0.5 2 -0.5 2 ]\n", null);
		
		// No BPC
		
		if (!DoBPC)
		{
			cmsplugin._cmsIOPrintf(m, "%% VonKries-like transform in Bradford Cone Space\n" +
	                      "/TransformPQR [\n" +
	                      "{exch pop exch 3 get mul exch pop exch 3 get div} bind\n" +
	                      "{exch pop exch 4 get mul exch pop exch 4 get div} bind\n" +
	                      "{exch pop exch 5 get mul exch pop exch 5 get div} bind\n]\n", null);
		}
		else
		{
			// BPC
			
			cmsplugin._cmsIOPrintf(m, "%% VonKries-like transform in Bradford Cone Space plus BPC\n" +
	                      "/TransformPQR [\n", null);
			
			cmsplugin._cmsIOPrintf(m, "{4 index 3 get div 2 index 3 get mul " +
	                    "2 index 3 get 2 index 3 get sub mul " +
	                    "2 index 3 get 4 index 3 get 3 index 3 get sub mul sub " +
	                    "3 index 3 get 3 index 3 get exch sub div " +
	                    "exch pop exch pop exch pop exch pop } bind\n", null);
			
			cmsplugin._cmsIOPrintf(m, "{4 index 4 get div 2 index 4 get mul " +
	                    "2 index 4 get 2 index 4 get sub mul " +
	                    "2 index 4 get 4 index 4 get 3 index 4 get sub mul sub " +
	                    "3 index 4 get 3 index 4 get exch sub div " +
	                    "exch pop exch pop exch pop exch pop } bind\n", null);
			
			cmsplugin._cmsIOPrintf(m, "{4 index 5 get div 2 index 5 get mul " +
	                    "2 index 5 get 2 index 5 get sub mul " +
	                    "2 index 5 get 4 index 5 get 3 index 5 get sub mul sub " +
	                    "3 index 5 get 3 index 5 get exch sub div " +
	                    "exch pop exch pop exch pop exch pop } bind\n]\n", null);
		}
	}
	
	private static void EmitXYZ2Lab(cmsIOHANDLER m)
	{
		cmsplugin._cmsIOPrintf(m, "/RangeLMN [ -0.635 2.0 0 2 -0.635 2.0 ]\n", null); 
		cmsplugin._cmsIOPrintf(m, "/EncodeLMN [\n", null);
		cmsplugin._cmsIOPrintf(m, "{ 0.964200  div dup 0.008856 le {7.787 mul 16 116 div add}{1 3 div exp} ifelse } bind\n", null);
		cmsplugin._cmsIOPrintf(m, "{ 1.000000  div dup 0.008856 le {7.787 mul 16 116 div add}{1 3 div exp} ifelse } bind\n", null);
		cmsplugin._cmsIOPrintf(m, "{ 0.824900  div dup 0.008856 le {7.787 mul 16 116 div add}{1 3 div exp} ifelse } bind\n", null);
		cmsplugin._cmsIOPrintf(m, "]\n", null);
		cmsplugin._cmsIOPrintf(m, "/MatrixABC [ 0 1 0 1 -1 1 0 0 -1 ]\n", null);
		cmsplugin._cmsIOPrintf(m, "/EncodeABC [\n", null);
	    
		cmsplugin._cmsIOPrintf(m, "{ 116 mul  16 sub 100 div  } bind\n", null);
		cmsplugin._cmsIOPrintf(m, "{ 500 mul 128 add 256 div  } bind\n", null);
		cmsplugin._cmsIOPrintf(m, "{ 200 mul 128 add 256 div  } bind\n", null);
	    
		cmsplugin._cmsIOPrintf(m, "]\n", null);
	}
	
	// Due to impedance mismatch between XYZ and almost all RGB and CMYK spaces
	// I choose to dump LUTS in Lab instead of XYZ. There is still a lot of wasted
	// space on 3D CLUT, but since space seems not to be a problem here, 33 points
	// would give a reasonable accurancy. Note also that CRD tables must operate in 
	// 8 bits.
	
	private static int WriteOutputLUT(cmsIOHANDLER m, cmsHPROFILE hProfile, int Intent, int dwFlags)
	{
	    cmsHPROFILE hLab;
	    cmsHTRANSFORM xform;
	    int i, nChannels;
	    int OutputFormat;
	    _cmsTRANSFORM v;
	    cmsPipeline DeviceLink;
	    cmsHPROFILE[] Profiles = new cmsHPROFILE[3];
	    cmsCIEXYZ BlackPointAdaptedToD50 = new cmsCIEXYZ();
	    boolean lDoBPC = (dwFlags & lcms2.cmsFLAGS_BLACKPOINTCOMPENSATION) != 0;
	    boolean lFixWhite = (dwFlags & lcms2.cmsFLAGS_NOWHITEONWHITEFIXUP) == 0;
		int InFrm = lcms2.TYPE_Lab_16;
		int RelativeEncodingIntent;
		int ColorSpace;
		
		hLab = cmsvirt.cmsCreateLab4ProfileTHR(m.ContextID, null);
		if (hLab == null)
		{
			return 0;
		}
		
	    OutputFormat = cmspack.cmsFormatterForColorspaceOfProfile(hProfile, 2, false);
		nChannels    = lcms2.T_CHANNELS(OutputFormat);
		
		ColorSpace = cmsio0.cmsGetColorSpace(hProfile);
		
		// For absolute colorimetric, the LUT is encoded as relative in order to preserve precision.
		
	    RelativeEncodingIntent = Intent;
		if (RelativeEncodingIntent == lcms2.INTENT_ABSOLUTE_COLORIMETRIC)
		{
			RelativeEncodingIntent = lcms2.INTENT_RELATIVE_COLORIMETRIC;
		}
		
		// Use V4 Lab always
		Profiles[0] = hLab;
		Profiles[1] = hProfile;
		
		xform = cmsxform.cmsCreateMultiprofileTransformTHR(m.ContextID, 
			                                      Profiles, 2, lcms2.TYPE_Lab_DBL, 
			                                      OutputFormat, RelativeEncodingIntent, 0);
		cmsio0.cmsCloseProfile(hLab);
		
	    if (xform == null)
	    {
	    	cmserr.cmsSignalError(m.ContextID, lcms2.cmsERROR_COLORSPACE_CHECK, Utility.LCMS_Resources.getString(LCMSResource.CMSPS2_CANT_CREATE_XFORM_LAB_CRD), null);
	        return 0;
	    }
	    
	    // Get a copy of the internal devicelink
	    v = (_cmsTRANSFORM)xform;
	    DeviceLink = cmslut.cmsPipelineDup(v.Lut);
		if (DeviceLink == null)
		{
			return 0;
		}
		
		// We need a CLUT
		dwFlags |= lcms2.cmsFLAGS_FORCE_CLUT;
		cmsPipeline[] temp = new cmsPipeline[]{DeviceLink};
		cmsopt._cmsOptimizePipeline(new cmsPipeline[]{DeviceLink}, RelativeEncodingIntent, new int[]{InFrm}, new int[]{OutputFormat}, new int[]{dwFlags});
		DeviceLink = temp[0];
		
	    cmsplugin._cmsIOPrintf(m, "<<\n", null);
	    cmsplugin._cmsIOPrintf(m, "/ColorRenderingType 1\n", null);
	    
	    cmssamp.cmsDetectBlackPoint(BlackPointAdaptedToD50, hProfile, Intent, 0);
	    
	    // Emit headers, etc.
	    EmitWhiteBlackD50(m, BlackPointAdaptedToD50);
	    EmitPQRStage(m, hProfile, lDoBPC, Intent == lcms2.INTENT_ABSOLUTE_COLORIMETRIC);
	    EmitXYZ2Lab(m);
	    
	    // FIXUP: map Lab (100, 0, 0) to perfect white, because the particular encoding for Lab 
	    // does map a=b=0 not falling into any specific node. Since range a,b goes -128..127, 
	    // zero is slightly moved towards right, so assure next node (in L=100 slice) is mapped to
	    // zero. This would sacrifice a bit of highlights, but failure to do so would cause
	    // scum dot. Ouch.
	    
	    if (Intent == lcms2.INTENT_ABSOLUTE_COLORIMETRIC)
	    {
	    	lFixWhite = false;
	    }
	    
	    cmsplugin._cmsIOPrintf(m, "/RenderTable ", null);
	    
	    WriteCLUT(m, cmslut.cmsPipelineGetPtrToFirstStage(DeviceLink), "<", ">\n", "", "", lFixWhite, ColorSpace);
	    
	    cmsplugin._cmsIOPrintf(m, " %d {} bind ", new Object[]{new Integer(nChannels)});
	    
	    for (i=1; i < nChannels; i++)
	    {
	    	cmsplugin._cmsIOPrintf(m, "dup ", null);
	    }
	    
	    cmsplugin._cmsIOPrintf(m, "]\n", null);
	    
	    EmitIntent(m, Intent);
	    
	    cmsplugin._cmsIOPrintf(m, ">>\n", null);
	    
	    if ((dwFlags & lcms2.cmsFLAGS_NODEFAULTRESOURCEDEF) == 0)
	    {
	    	cmsplugin._cmsIOPrintf(m, "/Current exch /ColorRendering defineresource pop\n", null);
	    }
	    
	    cmslut.cmsPipelineFree(DeviceLink);
	    cmsxform.cmsDeleteTransform(xform);
	    
	    return 1;   
	}
	
	// Builds a ASCII string containing colorant list in 0..1.0 range
	private static void BuildColorantList(StringBuffer Colorant, int nColorant, short[] Out)
	{
	    char[] Buff = new char[32];
	    int j;
	    
	    if (nColorant > lcms2.cmsMAXCHANNELS)
	    {
	    	nColorant = lcms2.cmsMAXCHANNELS;
	    }
	    
	    for (j=0; j < nColorant; j++)
	    {
	    	Utility.sprintf(Buff, "%.3f", new Object[]{new Double(Out[j] / 65535.0)});
	    	Colorant.append(Utility.cstringCreation(Buff));
	    	if (j < nColorant -1)
	    	{
	    		Colorant.append(' ');
	    	}
	    }
	}
	
	// Creates a PostScript color list from a named profile data. 
	// This is a HP extension.
	
	private static int WriteNamedColorCRD(cmsIOHANDLER m, cmsHPROFILE hNamedColor, int Intent, int dwFlags)
	{
	    cmsHTRANSFORM xform;    
	    int i, nColors, nColorant;
	    int OutputFormat;
	    StringBuffer ColorName = new StringBuffer(32);
	    StringBuffer Colorant = new StringBuffer(128);
		cmsNAMEDCOLORLIST NamedColorList;
		
	    OutputFormat = cmspack.cmsFormatterForColorspaceOfProfile(hNamedColor, 2, false);
		nColorant    = lcms2.T_CHANNELS(OutputFormat);
		
	    xform = cmsxform.cmsCreateTransform(hNamedColor, lcms2.TYPE_NAMED_COLOR_INDEX, null, OutputFormat, Intent, dwFlags);
	    if (xform == null)
	    {
	    	return 0;
	    }
	    
		NamedColorList = cmsnamed.cmsGetNamedColorList(xform);
		if (NamedColorList == null)
		{
			return 0;
		}
		
	    cmsplugin._cmsIOPrintf(m, "<<\n", null);
	    cmsplugin._cmsIOPrintf(m, "(colorlistcomment) (%s) \n", new Object[]{"Named profile"});
	    cmsplugin._cmsIOPrintf(m, "(Prefix) [ (Pantone ) (PANTONE ) ]\n", null);
	    cmsplugin._cmsIOPrintf(m, "(Suffix) [ ( CV) ( CVC) ( C) ]\n", null);
	    
	    nColors = cmsnamed.cmsNamedColorCount(NamedColorList);
	    
	    for (i=0; i < nColors; i++)
	    {
	        short[] In = new short[1];
	        short[] Out = new short[lcms2.cmsMAXCHANNELS];
	        
	        In[0] = (short)i;
	        
	        if (!cmsnamed.cmsNamedColorInfo(NamedColorList, i, ColorName, null, null, null, null))
	        {
	        	continue;
	        }
	        
	        cmsxform.cmsDoTransform(xform, In, Out, 1);      
	        BuildColorantList(Colorant, nColorant, Out);
	        cmsplugin._cmsIOPrintf(m, "  (%s) [ %s ]\n", new Object[]{ColorName, Colorant});
	    }
	    
	    cmsplugin._cmsIOPrintf(m, "   >>", null);
	    
	    if ((dwFlags & lcms2.cmsFLAGS_NODEFAULTRESOURCEDEF) == 0)
	    {
	    	cmsplugin._cmsIOPrintf(m, " /Current exch /HPSpotTable defineresource pop\n", null);
	    }
	    
	    cmsxform.cmsDeleteTransform(xform);  
	    return 1;
	}
	
	// This one does create a Color Rendering Dictionary. 
	// CRD are always LUT-Based, no matter if profile is
	// implemented as matrix-shaper.
	
	private static int GenerateCRD(cmsContext ContextID, cmsHPROFILE hProfile, int Intent, int dwFlags, cmsIOHANDLER mem)
	{    
		int dwBytesUsed;
		
		if ((dwFlags & lcms2.cmsFLAGS_NODEFAULTRESOURCEDEF) == 0)
		{
			EmitHeader(mem, "Color Rendering Dictionary (CRD)", hProfile);
		}
		
		// Is a named color profile?
		if (cmsio0.cmsGetDeviceClass(hProfile) == lcms2.cmsSigNamedColorClass)
		{
			if (WriteNamedColorCRD(mem, hProfile, Intent, dwFlags) == 0)
			{
				return 0;
			}
		}
		else
		{
			// CRD are always implemented as LUT
			
			if (WriteOutputLUT(mem, hProfile, Intent, dwFlags) == 0)
			{
				return 0;
			}
		}
		
		if ((dwFlags & lcms2.cmsFLAGS_NODEFAULTRESOURCEDEF) == 0)
		{
			cmsplugin._cmsIOPrintf(mem, "%%%%EndResource\n", null);
			cmsplugin._cmsIOPrintf(mem, "\n%% CRD End\n", null);
		}
		
		// Done, keep memory usage
		dwBytesUsed = mem.UsedSpace;
		
		// Finally, return used byte count
		return dwBytesUsed;
	}
	
	public static int cmsGetPostScriptColorResource(cmsContext ContextID, int Type, cmsHPROFILE hProfile, int Intent, int dwFlags, cmsIOHANDLER io)
	{
		int rc;
		
		switch (Type)
		{
			case lcms2.cmsPS_RESOURCE_CSA:
				rc = GenerateCSA(ContextID, hProfile, Intent, dwFlags, io);
				break;
			default:
			case lcms2.cmsPS_RESOURCE_CRD:
				rc = GenerateCRD(ContextID, hProfile, Intent, dwFlags, io);
				break;
		}
		
		return rc;
	}
	
	public static int cmsGetPostScriptCRD(cmsContext ContextID, cmsHPROFILE hProfile, int Intent, int dwFlags, byte[] Buffer, int dwBufferLen)
	{
		cmsIOHANDLER mem;
		int dwBytesUsed;
		
		// Set up the serialization engine
		if (Buffer == null)
		{
			mem = cmsio0.cmsOpenIOhandlerFromNULL(ContextID);
		}
		else
		{
			mem = cmsio0.cmsOpenIOhandlerFromMem(ContextID, Buffer, dwBufferLen, 'w');
		}
		
		if (mem == null)
		{
			return 0;
		}
		
		dwBytesUsed = cmsGetPostScriptColorResource(ContextID, lcms2.cmsPS_RESOURCE_CRD, hProfile, Intent, dwFlags, mem);
		
		// Get rid of memory stream
		cmsio0.cmsCloseIOhandler(mem);
		
		return dwBytesUsed;
	}
	
	// Does create a Color Space Array on XYZ colorspace for PostScript usage
	public static int cmsGetPostScriptCSA(cmsContext ContextID, cmsHPROFILE hProfile, int Intent, int dwFlags, byte[] Buffer, int dwBufferLen)  
	{
		cmsIOHANDLER mem;
		int dwBytesUsed;
		
		// Set up the serialization engine
		if (Buffer == null)
		{
			mem = cmsio0.cmsOpenIOhandlerFromNULL(ContextID);
		}
		else
		{
			mem = cmsio0.cmsOpenIOhandlerFromMem(ContextID, Buffer, dwBufferLen, 'w');
		}
		
		if (mem == null)
		{
			return 0;
		}
		
		dwBytesUsed = cmsGetPostScriptColorResource(ContextID, lcms2.cmsPS_RESOURCE_CSA, hProfile, Intent, dwFlags, mem);
		
		// Get rid of memory stream
		cmsio0.cmsCloseIOhandler(mem);
		
		return dwBytesUsed;
	}
}
