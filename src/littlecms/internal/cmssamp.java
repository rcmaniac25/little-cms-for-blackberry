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
import littlecms.internal.lcms2_plugin.cmsMAT3;
import littlecms.internal.lcms2_plugin.cmsVEC3;

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
	
	// ---------------------------------------------------------------------------------------------------------
	
	// Least Squares Fit of a Quadratic Curve to Data
	// http://www.personal.psu.edu/jhm/f90/lectures/lsq2.html
	
	private static double RootOfLeastSquaresFitQuadraticCurve(int n, double[] x, double[] y) 
	{
		double sum_x = 0, sum_x2 = 0, sum_x3 = 0, sum_x4 = 0;
	    double sum_y = 0, sum_yx = 0, sum_yx2 = 0;
	    double disc;
	    int i;
	    cmsMAT3 m;
	    cmsVEC3 v, res;
	    
	    if (n < 4)
	    {
	    	return 0;
	    }
	    
	    for (i=0; i < n; i++)
	    {
	        double xn = x[i];
	        double yn = y[i];
	        
	        sum_x  += xn;
	        sum_x2 += xn*xn;
	        sum_x3 += xn*xn*xn;
	        sum_x4 += xn*xn*xn*xn;
	        
	        sum_y += yn;
	        sum_yx += yn*xn;
	        sum_yx2 += yn*xn*xn;
	    }
	    
	    m = new cmsMAT3();
	    cmsmtrx._cmsVEC3init(m.v[0], n,      sum_x,  sum_x2);
	    cmsmtrx._cmsVEC3init(m.v[1], sum_x,  sum_x2, sum_x3);
	    cmsmtrx._cmsVEC3init(m.v[2], sum_x2, sum_x3, sum_x4);
	    
	    v = new cmsVEC3();
	    cmsmtrx._cmsVEC3init(v, sum_y, sum_yx, sum_yx2);
	    
	    res = new cmsVEC3();
	    if (!cmsmtrx._cmsMAT3solve(res, m, v))
	    {
	    	return 0;
	    }
	    
	    // y = t x2 + u x + c 
		// x = ( - u + Sqrt( u^2 - 4 t c ) ) / ( 2 t )
	    disc = res.n[1]*res.n[1] - 4.0 * res.n[0] * res.n[2];
	    if (disc < 0)
	    {
	    	return -1;
	    }
	    
	    return ( -1.0 * res.n[1] + Math.sqrt( disc )) / (2.0 * res.n[0]);	
	}
	
	private static boolean IsMonotonic(int n, final double[] Table)
	{
		int i;
		double last;
		
	    last = Table[n-1];
	    
	    for (i = n-2; i >= 0; --i)
	    {
	    	if (Table[i] > last)
	    	{
	    		return false;
	    	}
	        else
	        {
	        	last = Table[i];
	        }
	    }
	    
	    return true;
	}
	
	// Calculates the black point of a destination profile. 
	// This algorithm comes from the Adobe paper disclosing its black point compensation method. 
	public static boolean cmsDetectDestinationBlackPoint(cmsCIEXYZ BlackPoint, cmsHPROFILE hProfile, int Intent, int dwFlags)
	{
		int ColorSpace;
	    cmsHTRANSFORM hRoundTrip = null;
	    cmsCIELab InitialLab, destLab, Lab;
	    
	    double MinL, MaxL;
	    boolean NearlyStraightMidRange = false;
	    double L;
	    double[] x = new double[101], y = new double[101];
	    double lo, hi, NonMonoMin;
	    int n, l, i, NonMonoIndx;
	    
	    // Make sure intent is adequate
	    if (Intent != lcms2.INTENT_PERCEPTUAL &&
	    		Intent != lcms2.INTENT_RELATIVE_COLORIMETRIC &&
	    		Intent != lcms2.INTENT_SATURATION)
	    {
	    	BlackPoint.X = BlackPoint.Y = BlackPoint.Z = 0.0;
	    	return false;
		}
	    
	    // v4 + perceptual & saturation intents does have its own black point, and it is 
	    // well specified enough to use it. Black point tag is deprecated in V4.
	    if ((cmsio0.cmsGetEncodedICCversion(hProfile) >= 0x4000000) &&
	    		(Intent == lcms2.INTENT_PERCEPTUAL || Intent == lcms2.INTENT_SATURATION))
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
	    
	    // Check if the profile is lut based and gray, rgb or cmyk (7.2 in Adobe's document)
	    ColorSpace = cmsio0.cmsGetColorSpace(hProfile);
	    if (!cmsio1.cmsIsCLUT(hProfile, Intent, lcms2.LCMS_USED_AS_OUTPUT ) ||
	    		(ColorSpace != lcms2.cmsSigGrayData && 
	    		ColorSpace != lcms2.cmsSigRgbData && 
	    		ColorSpace != lcms2.cmsSigCmykData))
	    {
	    	// In this case, handle as input case
	    	return cmsDetectBlackPoint(BlackPoint, hProfile, Intent, dwFlags);
	    }
	    
	    // It is one of the valid cases!, presto chargo hocus pocus, go for the Adobe magic
	    
	    // Step 1
	    // ======
	    
	    // Set a first guess, that should work on good profiles.
	    if (Intent == lcms2.INTENT_RELATIVE_COLORIMETRIC)
	    {
	    	cmsCIEXYZ IniXYZ = new cmsCIEXYZ();
	    	
	        // calculate initial Lab as source black point
	        if (!cmsDetectBlackPoint(IniXYZ, hProfile, Intent, dwFlags))
	        {
	            return false;
	        }
	        
	        // convert the XYZ to lab
	        InitialLab = new cmsCIELab();
	        cmspcs.cmsXYZ2Lab(null, InitialLab, IniXYZ);
	    }
	    else
	    {
	    	// set the initial Lab to zero, that should be the black point for perceptual and saturation
	    	InitialLab = new cmsCIELab(0, 0, 0);
	    }
	    
	    // Step 2
	    // ======
	    
	    // Create a roundtrip. Define a Transform BT for all x in L*a*b*
	    hRoundTrip = CreateRoundtripXForm(hProfile, Intent);
	    if (hRoundTrip == null)
	    {
	    	return false;
	    }
	    
	    // Calculate Min L*
	    Lab = new cmsCIELab(InitialLab);
	    Lab.L = 0;
	    destLab = new cmsCIELab();
	    cmsxform.cmsDoTransform(hRoundTrip, Lab, destLab, 1);
	    MinL = destLab.L;
	    
	    // Calculate Max L*
	    Lab = new cmsCIELab(InitialLab);
	    Lab.L = 100;
	    cmsxform.cmsDoTransform(hRoundTrip, Lab, destLab, 1);
	    MaxL = destLab.L;
	    
	    // Step 3
	    // ======
	    
	    // check if quadratic estimation needs to be done.  
	    if (Intent == lcms2.INTENT_RELATIVE_COLORIMETRIC)
	    {
	        // Conceptually, this code tests how close the source l and converted L are to one another in the mid-range
	        // of the values. If the converted ramp of L values is close enough to a straight line y=x, then InitialLab 
	        // is good enough to be the DestinationBlackPoint,        
	        NearlyStraightMidRange = true;
	        
	        for (l=0; l <= 100; l++)
	        {
	            Lab.L = l;
	            Lab.a = InitialLab.a;
	            Lab.b = InitialLab.b;
	            
	            cmsxform.cmsDoTransform(hRoundTrip, Lab, destLab, 1);
	            
	            L = destLab.L;
	            
	            // Check the mid range in 20% after MinL
	            if (L > (MinL + 0.2 * (MaxL - MinL)))
	            {
	                // Is close enough?
	                if (Math.abs(L - l) > 4.0)
	                {
	                    // Too far away, profile is buggy!
	                    NearlyStraightMidRange = false;
	                    break;
	                }
	            }
	        }
	    }
	    else
	    {
	        // Check is always performed for perceptual and saturation intents
	        NearlyStraightMidRange = false;
	    }
	    
	    // If no furter checking is needed, we are done
	    if (NearlyStraightMidRange)
	    {
	    	cmspcs.cmsLab2XYZ(null, BlackPoint, InitialLab);          
	        cmsxform.cmsDeleteTransform(hRoundTrip);
	        return true;
	    }
	    
	    // The round-trip curve normally looks like a nearly constant section at the black point, 
	    // with a corner and a nearly straight line to the white point.
	    
	    // STEP 4
	    // =======
	    
	    // find the black point using the least squares error quadratic curve fitting
	    
	    if (Intent == lcms2.INTENT_RELATIVE_COLORIMETRIC)
	    {
	        lo = 0.1;
	        hi = 0.5;
	    }
	    else
	    {
	        // Perceptual and saturation
	        lo = 0.03;
	        hi = 0.25;
	    }
	    
	    // Capture points for the fitting.
	    n = 0;
	    for (l=0; l <= 100; l++)
	    {
	    	double ff;
	    	
	        Lab.L = (double)l;
	        Lab.a = InitialLab.a;
	        Lab.b = InitialLab.b;
	        
	        cmsxform.cmsDoTransform(hRoundTrip, Lab, destLab, 1);
	        
	        ff = (destLab.L - MinL)/(MaxL - MinL);
	        
	        if (ff >= lo && ff < hi)
	        {
	            x[n] = Lab.L;
	            y[n] = ff;
	            n++;
	        }
	    }
	    
		// This part is not on the Adobe paper, but I found is necessary for getting any result.
	    
		if (IsMonotonic(n, y))
		{
			// Monotonic means lower point is stil valid
	        cmspcs.cmsLab2XYZ(null, BlackPoint, InitialLab);
	        cmsxform.cmsDeleteTransform(hRoundTrip);
	        return true;
		}
		
	    // No suitable points, regret and use safer algorithm
	    if (n == 0)
	    {
	        cmsxform.cmsDeleteTransform(hRoundTrip);
	        return cmsDetectBlackPoint(BlackPoint, hProfile, Intent, dwFlags);
	    }
	    
		NonMonoMin = 100;
		NonMonoIndx = 0;
		for (i=0; i < n; i++)
		{
			if (y[i] < NonMonoMin)
			{
				NonMonoIndx = i;
				NonMonoMin = y[i];
			}
		}
		
		Lab.L = x[NonMonoIndx];
		
	    // fit and get the vertex of quadratic curve
	    Lab.L = RootOfLeastSquaresFitQuadraticCurve(n, x, y);
	    
	    if (Lab.L < 0.0 || Lab.L > 50.0) // clip to zero L* if the vertex is negative
	    {
	        Lab.L = 0;
	    }
	    
	    Lab.a = InitialLab.a;
	    Lab.b = InitialLab.b;
	    
	    cmspcs.cmsLab2XYZ(null, BlackPoint, Lab);
	    
	    cmsxform.cmsDeleteTransform(hRoundTrip);
	    return true;
	}
}
