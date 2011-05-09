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

import littlecms.internal.helper.Utility;
import littlecms.internal.lcms2.cmsCIELCh;
import littlecms.internal.lcms2.cmsCIELab;
import littlecms.internal.lcms2.cmsContext;
import littlecms.internal.lcms2.cmsHPROFILE;
import littlecms.internal.lcms2.cmsHTRANSFORM;
import littlecms.internal.lcms2.cmsPipeline;
import littlecms.internal.lcms2.cmsSAMPLER16;
import littlecms.internal.lcms2.cmsStage;
import littlecms.internal.lcms2.cmsToneCurve;

//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmsgmt
{
	// Auxiliar: append a Lab identity after the given sequence of profiles
	// and return the transform. Lab profile is closed, rest of profiles are kept open.
	public static cmsHTRANSFORM _cmsChain2Lab(cmsContext ContextID, int nProfiles, int InputFormat, int OutputFormat, final int[] Intents, 
			final cmsHPROFILE[] hProfiles, final boolean[] BPC, final double[] AdaptationStates, int dwFlags)
	{
	    cmsHTRANSFORM xform;
	    cmsHPROFILE   hLab;
	    cmsHPROFILE[] ProfileList = new cmsHPROFILE[256];
	    boolean[]     BPCList = new boolean[256];
	    double[]      AdaptationList = new double[256];
	    int[]         IntentList = new int[256];
	    int i;
	    
	    // This is a rather big number and there is no need of dynamic memory
	    // since we are adding a profile, 254 + 1 = 255 and this is the limit
	    if (nProfiles > 254)
	    {
	    	return null;
	    }
	    
	    // The output space
	    hLab = cmsvirt.cmsCreateLab4ProfileTHR(ContextID, null);
	    if (hLab == null)
	    {
	    	return null;
	    }
	    
	    // Create a copy of parameters
	    for (i=0; i < nProfiles; i++)
	    {
	        ProfileList[i]    = hProfiles[i];
	        BPCList[i]        = BPC[i];
	        AdaptationList[i] = AdaptationStates[i];
	        IntentList[i]     = Intents[i];
	    }
	    
	    // Place Lab identity at chain's end.
	    ProfileList[nProfiles]    = hLab;
	    BPCList[nProfiles]        = false;
	    AdaptationList[nProfiles] = 1.0;
	    IntentList[nProfiles]     = lcms2.INTENT_RELATIVE_COLORIMETRIC;
	    
	    // Create the transform
	    xform = cmsxform.cmsCreateExtendedTransform(ContextID, nProfiles + 1, ProfileList, 
	                                       BPCList, 
	                                       IntentList, 
	                                       AdaptationList, 
	                                       null, 0, 
	                                       InputFormat, 
	                                       OutputFormat, 
	                                       dwFlags);
	    
	    cmsio0.cmsCloseProfile(hLab);
	    
	    return xform;
	}
	
	// Compute K -> L* relationship. Flags may include black point compensation. In this case, 
	// the relationship is assumed from the profile with BPC to a black point zero.
	private static cmsToneCurve ComputeKToLstar(cmsContext ContextID, int nPoints, int nProfiles, final int[] Intents, final cmsHPROFILE[] hProfiles, 
			final boolean[] BPC, final double[] AdaptationStates, int dwFlags)
	{
	    cmsToneCurve out = null;
	    int i;
	    cmsHTRANSFORM xform;
	    cmsCIELab Lab = new cmsCIELab();
	    float[] cmyk = new float[4];
	    float[] SampledPoints;
	    
	    xform = _cmsChain2Lab(ContextID, nProfiles, lcms2.TYPE_CMYK_FLT, lcms2.TYPE_Lab_DBL, Intents, hProfiles, BPC, AdaptationStates, dwFlags);
	    if (xform == null)
	    {
	    	return null;
	    }
	    
	    SampledPoints = new float[nPoints];
	    
	    for (i=0; i < nPoints; i++)
	    {
	        cmyk[0] = 0;
	        cmyk[1] = 0;
	        cmyk[2] = 0;
	        cmyk[3] = (float)((i * 100.0) / (nPoints-1));
	        
	        cmsxform.cmsDoTransform(xform, cmyk, Lab, 1);
	        SampledPoints[i]= (float)(1.0 - Lab.L / 100.0); // Negate K for easier operation
	    }
	    
	    out = cmsgamma.cmsBuildTabulatedToneCurveFloat(ContextID, nPoints, SampledPoints);
	    
	    cmsxform.cmsDeleteTransform(xform);
	    
	    return out;
	}
	
	// Compute Black tone curve on a CMYK -> CMYK transform. This is done by
	// using the proof direction on both profiles to find K->L* relationship
	// then joining both curves. dwFlags may include black point compensation.
	public static cmsToneCurve _cmsBuildKToneCurve(cmsContext ContextID, int nPoints, int nProfiles, final int[] Intents, final cmsHPROFILE[] hProfiles, 
			final boolean[] BPC, final double[] AdaptationStates, int dwFlags)
	{
	    cmsToneCurve in, out, KTone;
	    
	    // Make sure CMYK -> CMYK
	    if (cmsio0.cmsGetColorSpace(hProfiles[0]) != lcms2.cmsSigCmykData || cmsio0.cmsGetColorSpace(hProfiles[nProfiles-1])!= lcms2.cmsSigCmykData)
	    {
	    	return null;
	    }
	    
	    // Make sure last is an output profile
	    if (cmsio0.cmsGetDeviceClass(hProfiles[nProfiles - 1]) != lcms2.cmsSigOutputClass)
	    {
	    	return null;
	    }
	    
	    // Create individual curves. BPC works also as each K to L* is
	    // computed as a BPC to zero black point in case of L*
	    in  = ComputeKToLstar(ContextID, nPoints, nProfiles - 1, Intents, hProfiles, BPC, AdaptationStates, dwFlags);
	    if (in == null)
	    {
	    	return null;
	    }
	    
	    out = ComputeKToLstar(ContextID, nPoints, 1, 
	                            new int[]{Intents[nProfiles - 1]}, 
	                            new cmsHPROFILE[]{hProfiles[nProfiles - 1]}, 
	                            new boolean[]{BPC[nProfiles - 1]}, 
	                            new double[]{AdaptationStates[nProfiles - 1]}, 
	                            dwFlags);
	    if (out == null)
	    {
	        cmsgamma.cmsFreeToneCurve(in);
	        return null;
	    }
	    
	    // Build the relationship. This effectively limits the maximum accuracy to 16 bits, but 
	    // since this is used on black-preserving LUTs, we are not loosing  accuracy in any case 
	    KTone = cmsgamma.cmsJoinToneCurve(ContextID, in, out, nPoints);
	    
	    // Get rid of components
	    cmsgamma.cmsFreeToneCurve(in); cmsgamma.cmsFreeToneCurve(out);
	    
	    // Something went wrong...
	    if (KTone == null)
	    {
	    	return null;
	    }
	    
	    // Make sure it is monotonic    
	    if (!cmsgamma.cmsIsToneCurveMonotonic(KTone))
	    {
	    	cmsgamma.cmsFreeToneCurve(KTone);
	        return null;
	    }
	    
	    return KTone;
	}
	
	// Gamut LUT Creation -----------------------------------------------------------------------------------------
	
	// Used by gamut & softproofing
	
	private static class GAMUTCHAIN
	{
		public cmsHTRANSFORM hInput;				// From whatever input color space. 16 bits to DBL
		public cmsHTRANSFORM hForward, hReverse;	// Transforms going from Lab to colorant and back
	    public double Thereshold;					// The thereshold after which is considered out of gamut
	}
	
	// This sampler does compute gamut boundaries by comparing original
	// values with a transform going back and forth. Values above ERR_THERESHOLD 
	// of maximum are considered out of gamut.
	
	private static final int ERR_THERESHOLD = 5;
	
	private static final cmsSAMPLER16 GamutSampler = new cmsSAMPLER16()
	{
		public int run(short[] In, short[] Out, Object Cargo)
		{
			GAMUTCHAIN t = (GAMUTCHAIN)Cargo;
		    cmsCIELab LabIn1 = new cmsCIELab(), LabOut1 = new cmsCIELab();  
		    cmsCIELab LabIn2 = new cmsCIELab(), LabOut2 = new cmsCIELab();      
		    float[] Proof = new float[lcms2.cmsMAXCHANNELS], Proof2 = new float[lcms2.cmsMAXCHANNELS];
		    double dE1, dE2, ErrorRatio;
		    
		    // Assume in-gamut by default.
		    dE1 = 0.;
		    dE2 = 0;
		    ErrorRatio = 1.0;
		    
		    // Convert input to Lab
		    if (t.hInput != null)
		    {
		    	cmsxform.cmsDoTransform(t.hInput, In, LabIn1, 1);
		    }
		    
		    // converts from PCS to colorant. This always
		    // does return in-gamut values, 
		    cmsxform.cmsDoTransform(t.hForward, LabIn1, Proof, 1);
		    
		    // Now, do the inverse, from colorant to PCS.
		    cmsxform.cmsDoTransform(t.hReverse, Proof, LabOut1, 1);
		    
		    LabIn2.L = LabOut1.L;
		    LabIn2.a = LabOut1.a;
		    LabIn2.b = LabOut1.b;
		    
		    // Try again, but this time taking Check as input
		    cmsxform.cmsDoTransform(t.hForward, LabOut1, Proof2, 1);
		    cmsxform.cmsDoTransform(t.hReverse, Proof2, LabOut2, 1);
		    
		    // Take difference of direct value
		    dE1 = cmspcs.cmsDeltaE(LabIn1, LabOut1);
		    
		    // Take difference of converted value
		    dE2 = cmspcs.cmsDeltaE(LabIn2, LabOut2);
		    
		    // if dE1 is small and dE2 is small, value is likely to be in gamut
		    if (dE1 < t.Thereshold && dE2 < t.Thereshold)
		    {
		    	Out[0] = 0;
		    }
		    else
		    {
		        // if dE1 is small and dE2 is big, undefined. Assume in gamut
		        if (dE1 < t.Thereshold && dE2 > t.Thereshold)
		        {
		        	Out[0] = 0;
		        }
		        else
		        {
		            // dE1 is big and dE2 is small, clearly out of gamut
		            if (dE1 > t.Thereshold && dE2 < t.Thereshold)
		            {
		            	Out[0] = (short)lcms2_internal._cmsQuickFloor((dE1 - t.Thereshold) + 0.5);
		            }
		            else
		            {
		                // dE1 is big and dE2 is also big, could be due to perceptual mapping
		                // so take error ratio
		                if (dE2 == 0.0)
		                {
		                	ErrorRatio = dE1;
		                }
		                else
		                {
		                	ErrorRatio = dE1 / dE2;
		                }
		                
		                if (ErrorRatio > t.Thereshold)
		                {
		                	Out[0] = (short)lcms2_internal._cmsQuickFloor((ErrorRatio - t.Thereshold) + 0.5);
		                }
		                else
		                {
		                	Out[0] = 0;
		                }
		            }
		        }
		    }
		    
		    return 1;
		}
	};
	
	// Does compute a gamut LUT going back and forth across pcs -> relativ. colorimetric intent -> pcs
	// the dE obtained is then annotated on the LUT. Values truely out of gamut are clipped to dE = 0xFFFE
	// and values changed are supposed to be handled by any gamut remapping, so, are out of gamut as well.
	//
	// **WARNING: This algorithm does assume that gamut remapping algorithms does NOT move in-gamut colors,
	// of course, many perceptual and saturation intents does not work in such way, but relativ. ones should.
	
	public static cmsPipeline _cmsCreateGamutCheckPipeline(cmsContext ContextID, cmsHPROFILE[] hProfiles, boolean[] BPC, int[] Intents, double[] AdaptationStates, 
			int nGamutPCSposition, cmsHPROFILE hGamut)
	{
		cmsHPROFILE hLab;
		cmsPipeline Gamut;
		cmsStage CLUT;
		int dwFormat;
		GAMUTCHAIN Chain;
		int nChannels, nGridpoints;
		int ColorSpace;
		int i;    
		cmsHPROFILE[] ProfileList = new cmsHPROFILE[256];
		boolean[] BPCList = new boolean[256];
		double[] AdaptationList = new double[256];
		int[] IntentList = new int[256];
		
		Chain = new GAMUTCHAIN();
		
		if (nGamutPCSposition <= 0 || nGamutPCSposition > 255)
		{
			cmserr.cmsSignalError(ContextID, lcms2.cmsERROR_RANGE, Utility.LCMS_Resources.getString(LCMSResource.CMSGMT_PCS_WRONG_POS), 
					new Object[]{new Integer(nGamutPCSposition)});
			return null;
		}
		
		hLab = cmsvirt.cmsCreateLab4ProfileTHR(ContextID, null);
		if (hLab == null)
		{
			return null;
		}
		
		// The figure of merit. On matrix-shaper profiles, should be almost zero as
		// the conversion is pretty exact. On LUT based profiles, different resolutions
		// of input and output CLUT may result in differences. 
		
		if (cmsio1.cmsIsMatrixShaper(hGamut))
		{
			Chain.Thereshold = 1.0;
		}
		else
		{
			Chain.Thereshold = ERR_THERESHOLD;
		}
		
		// Create a copy of parameters
		for (i=0; i < nGamutPCSposition; i++)
		{
			ProfileList[i]    = hProfiles[i];
			BPCList[i]        = BPC[i];
			AdaptationList[i] = AdaptationStates[i];
			IntentList[i]     = Intents[i];
		}
		
		// Fill Lab identity
		ProfileList[nGamutPCSposition] = hLab;
		BPCList[nGamutPCSposition] = false;
		AdaptationList[nGamutPCSposition] = 1.0;
		Intents[nGamutPCSposition] = lcms2.INTENT_RELATIVE_COLORIMETRIC;
		
		ColorSpace  = cmsio0.cmsGetColorSpace(hGamut);  
		
		nChannels   = cmspcs.cmsChannelsOf(ColorSpace);     
		nGridpoints = cmspcs._cmsReasonableGridpointsByColorspace(ColorSpace, lcms2.cmsFLAGS_HIGHRESPRECALC);
		dwFormat    = (lcms2.CHANNELS_SH(nChannels)|(2 << lcms2.BYTES_SHIFT_VALUE));
		
		// 16 bits to Lab double
		Chain.hInput = cmsxform.cmsCreateExtendedTransform(ContextID, 
			nGamutPCSposition + 1, 
			ProfileList, 
			BPCList, 
			Intents, 
			AdaptationList, 
			null, 0, 
			dwFormat, lcms2.TYPE_Lab_DBL, 
			lcms2.cmsFLAGS_NOCACHE);
		
		// Does create the forward step. Lab double to cmsFloat32Number
		dwFormat    = ((1 << lcms2.FLOAT_SHIFT_VALUE)|lcms2.CHANNELS_SH(nChannels)|(4 << lcms2.BYTES_SHIFT_VALUE));
		Chain.hForward = cmsxform.cmsCreateTransformTHR(ContextID,
			hLab, lcms2.TYPE_Lab_DBL, 
			hGamut, dwFormat, 
			lcms2.INTENT_RELATIVE_COLORIMETRIC,
			lcms2.cmsFLAGS_NOCACHE);
		
		// Does create the backwards step
		Chain.hReverse = cmsxform.cmsCreateTransformTHR(ContextID, hGamut, dwFormat, 
			hLab, lcms2.TYPE_Lab_DBL,                                      
			lcms2.INTENT_RELATIVE_COLORIMETRIC,
			lcms2.cmsFLAGS_NOCACHE);
		
		// All ok?
		if (Chain.hForward != null && Chain.hReverse != null)
		{
			// Go on, try to compute gamut LUT from PCS. This consist on a single channel containing 
			// dE when doing a transform back and forth on the colorimetric intent. 
			
			Gamut = cmslut.cmsPipelineAlloc(ContextID, 3, 1);
			
			if (Gamut != null)
			{
				CLUT = cmslut.cmsStageAllocCLut16bit(ContextID, nGridpoints, nChannels, 1, null);
				cmslut.cmsPipelineInsertStage(Gamut, lcms2.cmsAT_BEGIN, CLUT);
				
				cmslut.cmsStageSampleCLut16bit(CLUT, GamutSampler, Chain, 0);
			}
		}
		else
		{
			Gamut = null; // Didn't work...
		}
		
		// Free all needed stuff.
		if (Chain.hInput != null)
		{
			cmsxform.cmsDeleteTransform(Chain.hInput);
		}
		if (Chain.hForward != null)
		{
			cmsxform.cmsDeleteTransform(Chain.hForward);
		}
		if (Chain.hReverse != null)
		{
			cmsxform.cmsDeleteTransform(Chain.hReverse);
		}
		if (hLab != null)
		{
			cmsio0.cmsCloseProfile(hLab);
		}
		
		// And return computed hull
		return Gamut;
	}
	
	// Total Area Coverage estimation ----------------------------------------------------------------
	
	private static class cmsTACestimator
	{
		public int nOutputChans;
		public cmsHTRANSFORM hRoundTrip;
		public float MaxTAC;
		public float[] MaxInput;
	    
	    public cmsTACestimator()
	    {
	    	MaxInput = new float[lcms2.cmsMAXCHANNELS];
	    }
	}
	
	// This callback just accounts the maximum ink dropped in the given node. It does not populate any
	// memory, as the destination table is NULL. Its only purpose it to know the global maximum.
	private static final cmsSAMPLER16 EstimateTAC = new cmsSAMPLER16()
	{
		public int run(short[] In, short[] Out, Object Cargo)
		{
			cmsTACestimator bp = (cmsTACestimator)Cargo;
		    float[] RoundTrip = new float[lcms2.cmsMAXCHANNELS];
		    int i;
		    float Sum;
		    
		    // Evaluate the xform
		    cmsxform.cmsDoTransform(bp.hRoundTrip, In, RoundTrip, 1);
		    
		    // All all amounts of ink
		    for (Sum=0, i=0; i < bp.nOutputChans; i++)
		    {
		    	Sum += RoundTrip[i];
		    }
		    
		    // If above maximum, keep track of input values
		    if (Sum > bp.MaxTAC)
		    {
		    	bp.MaxTAC = Sum;
		    	
		    	for (i=0; i < bp.nOutputChans; i++)
		    	{
		    		bp.MaxInput[i] = (In[i] & 0xFFFF);
		    	}
		    }
		    
		    return 1;
		}
	};
	
	// Detect Total area coverage of the profile
	public static double cmsDetectTAC(cmsHPROFILE hProfile)
	{
	    cmsTACestimator bp = new cmsTACestimator();
	    int dwFormatter;
	    int[] GridPoints = new int[lcms2_plugin.MAX_INPUT_DIMENSIONS];
	    cmsHPROFILE hLab;
	    cmsContext ContextID = cmsio0.cmsGetProfileContextID(hProfile);
	    
	    // TAC only works on output profiles
	    if (cmsio0.cmsGetDeviceClass(hProfile) != lcms2.cmsSigOutputClass)
	    {        
	        return 0;       
	    }
	    
	    // Create a fake formatter for result
	    dwFormatter = cmspack.cmsFormatterForColorspaceOfProfile(hProfile, 4, true);
	    
	    bp.nOutputChans = lcms2.T_CHANNELS(dwFormatter);
	    bp.MaxTAC = 0;    // Initial TAC is 0
	    
	    //  for safety
	    if (bp.nOutputChans >= lcms2.cmsMAXCHANNELS)
	    {
	    	return 0;
	    }
	    
	    hLab = cmsvirt.cmsCreateLab4ProfileTHR(ContextID, null);
	    if (hLab == null)
	    {
	    	return 0;
	    }
	    // Setup a roundtrip on perceptual intent in output profile for TAC estimation
	    bp.hRoundTrip = cmsxform.cmsCreateTransformTHR(ContextID, hLab, lcms2.TYPE_Lab_16, hProfile, dwFormatter, lcms2.INTENT_PERCEPTUAL, 
	    		lcms2.cmsFLAGS_NOOPTIMIZE|lcms2.cmsFLAGS_NOCACHE);
	    
	    cmsio0.cmsCloseProfile(hLab);  
	    if (bp.hRoundTrip == null)
	    {
	    	return 0;
	    }
	    
	    // For L* we only need black and white. For C* we need many points
	    GridPoints[0] = 6;
	    GridPoints[1] = 74;
	    GridPoints[2] = 74;
	    
		if (!cmslut.cmsSliceSpace16(3, GridPoints, EstimateTAC, bp))
		{
			bp.MaxTAC = 0;
		}
		
	    cmsxform.cmsDeleteTransform(bp.hRoundTrip);
	    
	    // Results in %
	    return bp.MaxTAC;
	}
	
	// Carefully, clamp on CIELab space.
	
	public static boolean cmsDesaturateLab(cmsCIELab Lab, double amax, double amin, double bmax, double bmin)
	{
		// Whole Luma surface to zero
		
		if (Lab.L < 0)
		{
			Lab.L = Lab.a = Lab.b = 0.0;
			return false;
		}
		
		// Clamp white, DISCARD HIGHLIGHTS. This is done
		// in such way because icc spec doesn't allow the
		// use of L>100 as a highlight means.
		
		if (Lab.L > 100)
		{
			Lab.L = 100;
		}
		
		// Check out gamut prism, on a, b faces
		
		if (Lab.a < amin || Lab.a > amax|| Lab.b < bmin || Lab.b > bmax)
		{
			cmsCIELCh LCh = new cmsCIELCh();
			double h, slope;
			
			// Falls outside a, b limits. Transports to LCh space,
			// and then do the clipping
			
			if (Lab.a == 0.0) // Is hue exactly 90?
			{
				// atan will not work, so clamp here
				Lab.b = Lab.b < 0 ? bmin : bmax;
				return true;
			}
			
			cmspcs.cmsLab2LCh(LCh, Lab);
			
			slope = Lab.b / Lab.a;
			h = LCh.h;
			
			// There are 4 zones
			
			if ((h >= 0.0 && h < 45.0) || (h >= 315 && h <= 360.0))
			{
				// clip by amax
				Lab.a = amax;
				Lab.b = amax * slope;
			}
			else
			{
				if (h >= 45.0 && h < 135.0)
				{
					// clip by bmax
					Lab.b = bmax;
					Lab.a = bmax / slope;
				}
				else
				{
					if (h >= 135.0 && h < 225.0)
					{
						// clip by amin
						Lab.a = amin;
						Lab.b = amin * slope;
					}
					else
					{
						if (h >= 225.0 && h < 315.0)
						{
							// clip by bmin
							Lab.b = bmin;
							Lab.a = bmin / slope;
						}
						else
						{
							cmserr.cmsSignalError(null, lcms2.cmsERROR_RANGE, Utility.LCMS_Resources.getString(LCMSResource.CMSGMT_INVALID_ANGLE), null);
							return false;
						}
					}
				}
			}
		}
		
		return true;
	}
}
