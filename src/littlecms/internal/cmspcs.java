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

//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmspcs
{
	//  inter PCS conversions XYZ <-> CIE L* a* b*
	/*


	       CIE 15:2004 CIELab is defined as:

	       L* = 116*f(Y/Yn) - 16                     0 <= L* <= 100
	       a* = 500*[f(X/Xn) - f(Y/Yn)]
	       b* = 200*[f(Y/Yn) - f(Z/Zn)]

	       and

	              f(t) = t^(1/3)                     1 >= t >  (24/116)^3
	                     (841/108)*t + (16/116)      0 <= t <= (24/116)^3


	       Reverse transform is:

	       X = Xn*[a* / 500 + (L* + 16) / 116] ^ 3   if (X/Xn) > (24/116)
	         = Xn*(a* / 500 + L* / 116) / 7.787      if (X/Xn) <= (24/116)



	       PCS in Lab2 is encoded as:

	              8 bit Lab PCS:

	                     L*      0..100 into a 0..ff byte.
	                     a*      t + 128 range is -128.0  +127.0
	                     b*

	             16 bit Lab PCS:

	                     L*     0..100  into a 0..ff00 word.
	                     a*     t + 128  range is  -128.0  +127.9961
	                     b*



	Interchange Space   Component     Actual Range        Encoded Range
	CIE XYZ             X             0 -> 1.99997        0x0000 -> 0xffff
	CIE XYZ             Y             0 -> 1.99997        0x0000 -> 0xffff
	CIE XYZ             Z             0 -> 1.99997        0x0000 -> 0xffff

	Version 2,3
	-----------

	CIELAB (16 bit)     L*            0 -> 100.0          0x0000 -> 0xff00
	CIELAB (16 bit)     a*            -128.0 -> +127.996  0x0000 -> 0x8000 -> 0xffff
	CIELAB (16 bit)     b*            -128.0 -> +127.996  0x0000 -> 0x8000 -> 0xffff


	Version 4
	---------

	CIELAB (16 bit)     L*            0 -> 100.0          0x0000 -> 0xffff
	CIELAB (16 bit)     a*            -128.0 -> +127      0x0000 -> 0x8080 -> 0xffff
	CIELAB (16 bit)     b*            -128.0 -> +127      0x0000 -> 0x8080 -> 0xffff

	*/
	
	//TODO: #90-865
	
	public static int cmsChannelsOf(int ColorSpace)
	{
	    switch (ColorSpace)
	    {
		    case lcms2.cmsSigGrayData:
		    	return 1;
		    case lcms2.cmsSig2colorData:
		    	return 2;
		    case lcms2.cmsSigXYZData:
		    case lcms2.cmsSigLabData:
		    case lcms2.cmsSigLuvData:
		    case lcms2.cmsSigYCbCrData:
		    case lcms2.cmsSigYxyData:
		    case lcms2.cmsSigRgbData:   
		    case lcms2.cmsSigHsvData:
		    case lcms2.cmsSigHlsData:
		    case lcms2.cmsSigCmyData: 
		    case lcms2.cmsSig3colorData:
		    	return 3;
		    case lcms2.cmsSigLuvKData:
		    case lcms2.cmsSigCmykData:
		    case lcms2.cmsSig4colorData:
		    	return 4;
		    case lcms2.cmsSigMCH5Data:
		    case lcms2.cmsSig5colorData:
		    	return 5;
		    case lcms2.cmsSigMCH6Data:   
		    case lcms2.cmsSig6colorData:
		    	return 6;
		    case lcms2.cmsSigMCH7Data:
		    case lcms2.cmsSig7colorData:
		    	return  7;
		    case lcms2.cmsSigMCH8Data:
		    case lcms2.cmsSig8colorData:
		    	return  8;
		    case lcms2.cmsSigMCH9Data:
		    case lcms2.cmsSig9colorData:
		    	return  9;
		    case lcms2.cmsSigMCHAData:
		    case lcms2.cmsSig10colorData:
		    	return 10;
		    case lcms2.cmsSigMCHBData:
		    case lcms2.cmsSig11colorData:
		    	return 11;
		    case lcms2.cmsSigMCHCData:
		    case lcms2.cmsSig12colorData:
		    	return 12;
		    case lcms2.cmsSigMCHDData:
		    case lcms2.cmsSig13colorData:
		    	return 13;
		    case lcms2.cmsSigMCHEData:
		    case lcms2.cmsSig14colorData:
		    	return 14;
		    case lcms2.cmsSigMCHFData:
		    case lcms2.cmsSig15colorData:
		    	return 15;
		    default:
		    	return 3;
	    }
	}
}
