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

import littlecms.internal.lcms2.cmsCIELab;
import littlecms.internal.lcms2.cmsCIEXYZ;
import littlecms.internal.lcms2.cmsContext;
import littlecms.internal.lcms2.cmsHPROFILE;
import littlecms.internal.lcms2.cmsHTRANSFORM;

/**
 * This file contains routines for resampling and LUT optimization, black point detection and black preservation.
 */
//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmssamp
{
	// Black point detection -------------------------------------------------------------------------
	
	// PCS -> PCS round trip transform, always uses relative intent on the device -> pcs 
	private static cmsHTRANSFORM CreateRoundtripXForm(cmsHPROFILE hProfile, int nIntent)
	{
	    cmsHPROFILE hLab = cmsvirt.cmsCreateLab4Profile(null);
	    cmsHTRANSFORM xform;
	    boolean[] BPC = new boolean[]{ false, false, false, false };
	    double[] States = new double[]{ 1.0, 1.0, 1.0, 1.0 };
	    cmsHPROFILE[] hProfiles = new cmsHPROFILE[4];
	    int[] Intents = new int[4];
	    cmsContext ContextID = cmsio0.cmsGetProfileContextID(hProfile);
	    
	    hProfiles[0] = hLab; hProfiles[1] = hProfile; hProfiles[2] = hProfile; hProfiles[3] = hLab;
	    Intents[0] = lcms2.INTENT_RELATIVE_COLORIMETRIC; Intents[1] = nIntent; Intents[2] = lcms2.INTENT_RELATIVE_COLORIMETRIC; Intents[3] = lcms2.INTENT_RELATIVE_COLORIMETRIC;
	    
	    xform = cmsxform.cmsCreateExtendedTransform(ContextID, 4, hProfiles, BPC, Intents, 
	        States, null, 0, lcms2.TYPE_Lab_DBL, lcms2.TYPE_Lab_DBL, lcms2.cmsFLAGS_NOCACHE|lcms2.cmsFLAGS_NOOPTIMIZE);
	    
	    cmsio0.cmsCloseProfile(hLab);
	    return xform;
	}
	
	// Use darker colorants to obtain black point. This works in the relative colorimetric intent and
	// assumes more ink results in darker colors. No ink limit is assumed.
	private static boolean BlackPointAsDarkerColorant(cmsHPROFILE hInput, int Intent, cmsCIEXYZ BlackPoint, int dwFlags)
	{
	    short[] Black;
	    cmsHTRANSFORM xform;
	    int Space;
	    int[] nChannels;
	    int dwFormat; 
	    cmsHPROFILE hLab;
	    cmsCIELab Lab;
	    cmsCIEXYZ BlackXYZ;        
	    cmsContext ContextID = cmsio0.cmsGetProfileContextID(hInput);
	    
	    // If the profile does not support input direction, assume Black point 0    
	    if (!cmsio1.cmsIsIntentSupported(hInput, Intent, lcms2.LCMS_USED_AS_INPUT))
	    {
	        BlackPoint.X = BlackPoint.Y = BlackPoint.Z = 0.0;
	        return false;
	    }
	    
	    // Create a formatter which has n channels and floating point
	    dwFormat = cmspack.cmsFormatterForColorspaceOfProfile(hInput, 2, false);
	    
	    // Try to get black by using black colorant    
	    Space = cmsio0.cmsGetColorSpace(hInput);
	    
	    // This function is used in this instance to get the number of elements needed
	    nChannels = new int[1];
	    if(!cmspcs._cmsEndPointsBySpace(Space, null, null, nChannels))
	    {
	    	BlackPoint.X = BlackPoint.Y = BlackPoint.Z = 0.0;
	        return false;
	    }
	    Black = new short[nChannels[0]];
	    
	    // This function returns darker colorant in 16 bits for several spaces
	    if (!cmspcs._cmsEndPointsBySpace(Space, null, Black, nChannels))
	    {
	        BlackPoint.X = BlackPoint.Y = BlackPoint.Z = 0.0;
	        return false;
	    }
	    
	    if (nChannels[0] != lcms2.T_CHANNELS(dwFormat))
	    {
	    	BlackPoint.X = BlackPoint.Y = BlackPoint.Z = 0.0;
	        return false;
	    }
	    
	    // Lab will be used as the output space, but lab2 will avoid recursion
	    hLab = cmsvirt.cmsCreateLab2ProfileTHR(ContextID, null);
	    if (hLab == null)
	    {
	    	BlackPoint.X = BlackPoint.Y = BlackPoint.Z = 0.0;
	        return false;
	    }
	    
	    // Create the transform
	    xform = cmsxform.cmsCreateTransformTHR(ContextID, hInput, dwFormat, hLab, lcms2.TYPE_Lab_DBL, Intent, lcms2.cmsFLAGS_NOOPTIMIZE|lcms2.cmsFLAGS_NOCACHE);
	    cmsio0.cmsCloseProfile(hLab);
	    
	    if (xform == null)
	    {
	        // Something went wrong. Get rid of open resources and return zero as black
	        
	    	BlackPoint.X = BlackPoint.Y = BlackPoint.Z = 0.0;
	        return false;
	    }
	    
	    // Convert black to Lab
	    Lab = new cmsCIELab();
	    cmsxform.cmsDoTransform(xform, Black, Lab, 1);
	    
	    // Force it to be neutral, clip to max. L* of 50
	    Lab.a = Lab.b = 0;
	    if (Lab.L > 50)
	    {
	    	Lab.L = 50;
	    }
	    
	    // Free the resources    
	    cmsxform.cmsDeleteTransform(xform);
	    
	    // Convert from Lab (which is now clipped) to XYZ.
	    BlackXYZ = new cmsCIEXYZ();
	    cmspcs.cmsLab2XYZ(null, BlackXYZ, Lab);
	    
	    if (BlackPoint != null)
	    {
	    	BlackPoint.X = BlackXYZ.X;
			BlackPoint.Y = BlackXYZ.Y;
			BlackPoint.Z = BlackXYZ.Z;
	    }
	    
	    return true;
	}
	
	// Get a black point of output CMYK profile, discounting any ink-limiting embedded 
	// in the profile. For doing that, we use perceptual intent in input direction:
	// Lab (0, 0, 0) -> [Perceptual] Profile -> CMYK -> [Rel. colorimetric] Profile -> Lab
	private static boolean BlackPointUsingPerceptualBlack(cmsCIEXYZ BlackPoint, cmsHPROFILE hProfile)
	{    
	    cmsHTRANSFORM hRoundTrip;    
	    cmsCIELab LabIn, LabOut;
	    cmsCIEXYZ BlackXYZ;
	    
	     // Is the intent supported by the profile?
	    if (!cmsio1.cmsIsIntentSupported(hProfile, lcms2.INTENT_PERCEPTUAL, lcms2.LCMS_USED_AS_INPUT))
	    {
	        BlackPoint.X = BlackPoint.Y = BlackPoint.Z = 0.0;
	        return true;
	    }
	        
	    hRoundTrip = CreateRoundtripXForm(hProfile, lcms2.INTENT_PERCEPTUAL);
	    if (hRoundTrip == null)
	    {
	    	BlackPoint.X = BlackPoint.Y = BlackPoint.Z = 0.0;
	        return false;
	    }
	    
	    LabIn = new cmsCIELab();
	    LabIn.L = LabIn.a = LabIn.b = 0;
	    LabOut = new cmsCIELab();
	    cmsxform.cmsDoTransform(hRoundTrip, LabIn, LabOut, 1);
	    
	    // Clip Lab to reasonable limits
	    if (LabOut.L > 50)
	    {
	    	LabOut.L = 50;
	    }
	    LabOut.a = LabOut.b = 0;
	    
	    cmsxform.cmsDeleteTransform(hRoundTrip);
	    
	    // Convert it to XYZ
	    BlackXYZ = new cmsCIEXYZ();
	    cmspcs.cmsLab2XYZ(null, BlackXYZ, LabOut);   
	    
	    if (BlackPoint != null)
	    {
	    	BlackPoint.X = BlackXYZ.X;
			BlackPoint.Y = BlackXYZ.Y;
			BlackPoint.Z = BlackXYZ.Z;
	    }
	    
	    return true;
	}
	
	// This function shouldn't exist at all -- there is such quantity of broken
	// profiles on black point tag, that we must somehow fix chromaticity to 
	// avoid huge tint when doing Black point compensation. This function does
	// just that. There is a special flag for using black point tag, but turned 
	// off by default because it is bogus on most profiles. The detection algorithm 
	// involves to turn BP to neutral and to use only L component.
	
	public static boolean cmsDetectBlackPoint(cmsCIEXYZ BlackPoint, cmsHPROFILE hProfile, int Intent, int dwFlags)
	{    
	    // Zero for black point
	    if (cmsio0.cmsGetDeviceClass(hProfile) == lcms2.cmsSigLinkClass)
	    {
	    	BlackPoint.X = BlackPoint.Y = BlackPoint.Z = 0.0;
	        return false;      
	    }
	    
	    // v4 + perceptual & saturation intents does have its own black point, and it is 
	    // well specified enough to use it. Black point tag is deprecated in V4.
	    
	    if ((cmsio0.cmsGetEncodedICCversion(hProfile) >= 0x4000000) && (Intent == lcms2.INTENT_PERCEPTUAL || Intent == lcms2.INTENT_SATURATION))
	    {
	    	// Matrix shaper share MRC & perceptual intents
	    	if (cmsio1.cmsIsMatrixShaper(hProfile))
	    	{
	    		return BlackPointAsDarkerColorant(hProfile, lcms2.INTENT_RELATIVE_COLORIMETRIC, BlackPoint, 0);
	    	}
	    	
	    	// Get Perceptual black out of v4 profiles. That is fixed for perceptual & saturation intents
	    	BlackPoint.X = lcms2.cmsPERCEPTUAL_BLACK_X;
	    	BlackPoint.Y = lcms2.cmsPERCEPTUAL_BLACK_Y;
	    	BlackPoint.Z = lcms2.cmsPERCEPTUAL_BLACK_Z;
	    	
	    	return true;
	    }
	    
//#ifdef CMS_USE_PROFILE_BLACK_POINT_TAG
	    
	    // v2, v4 rel/abs colorimetric
	    if (cmsio0.cmsIsTag(hProfile, lcms2.cmsSigMediaBlackPointTag) && Intent == lcms2.INTENT_RELATIVE_COLORIMETRIC)
	    {
	    	cmsCIEXYZ BlackPtr, BlackXYZ, UntrustedBlackPoint, TrustedBlackPoint, MediaWhite;
	    	cmsCIELab Lab;
	    	
	    	// If black point is specified, then use it,
	    	
	    	BlackPtr = (cmsCIEXYZ)cmsio0.cmsReadTag(hProfile, lcms2.cmsSigMediaBlackPointTag);
	    	if (BlackPtr != null)
	    	{
	    		BlackXYZ = BlackPtr;
	    		MediaWhite = new cmsCIEXYZ();
	    		cmsio1._cmsReadMediaWhitePoint(MediaWhite, hProfile);
	    		
	    		// Black point is absolute XYZ, so adapt to D50 to get PCS value
	    		UntrustedBlackPoint = new cmsCIEXYZ();
	    		cmswtpnt.cmsAdaptToIlluminant(UntrustedBlackPoint, MediaWhite, lcms2.cmsD50_XYZ, BlackXYZ);
	    		
	    		// Force a=b=0 to get rid of any chroma
	    		Lab = new cmsCIELab();
	    		cmspcs.cmsXYZ2Lab(null, Lab, UntrustedBlackPoint);
	    		Lab.a = Lab.b = 0;
	    		if (Lab.L > 50)
	    		{
	    			Lab.L = 50; // Clip to L* <= 50
	    		}
	    		TrustedBlackPoint = new cmsCIEXYZ();
	    		cmspcs.cmsLab2XYZ(null, TrustedBlackPoint, Lab);
	    		
	    		if (BlackPoint != null)
	    		{
	    			BlackPoint.X = TrustedBlackPoint.X;
	    			BlackPoint.Y = TrustedBlackPoint.Y;
	    			BlackPoint.Z = TrustedBlackPoint.Z;
	    		}
	    		
	    		return true;
	    	}
	    }
//#endif
	    
	    // That is about v2 profiles.
	    
	    // If output profile, discount ink-limiting and that's all
	    if (Intent == lcms2.INTENT_RELATIVE_COLORIMETRIC && (cmsio0.cmsGetDeviceClass(hProfile) == lcms2.cmsSigOutputClass) && 
	    		(cmsio0.cmsGetColorSpace(hProfile) == lcms2.cmsSigCmykData))
	    {
	    	return BlackPointUsingPerceptualBlack(BlackPoint, hProfile);
	    }
	    
	    // Nope, compute BP using current intent.
	    return BlackPointAsDarkerColorant(hProfile, Intent, BlackPoint, dwFlags);
	}
}
