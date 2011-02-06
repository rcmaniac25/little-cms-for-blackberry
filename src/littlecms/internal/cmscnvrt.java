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
import littlecms.internal.lcms2.cmsCIELab;
import littlecms.internal.lcms2.cmsCIEXYZ;
import littlecms.internal.lcms2.cmsCIExyY;
import littlecms.internal.lcms2.cmsContext;
import littlecms.internal.lcms2.cmsHPROFILE;
import littlecms.internal.lcms2.cmsHTRANSFORM;
import littlecms.internal.lcms2.cmsPipeline;
import littlecms.internal.lcms2.cmsSAMPLER16;
import littlecms.internal.lcms2.cmsStage;
import littlecms.internal.lcms2.cmsToneCurve;
import littlecms.internal.lcms2_plugin.cmsIntentFn;
import littlecms.internal.lcms2_plugin.cmsMAT3;
import littlecms.internal.lcms2_plugin.cmsPluginBase;
import littlecms.internal.lcms2_plugin.cmsPluginRenderingIntent;
import littlecms.internal.lcms2_plugin.cmsVEC3;

//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmscnvrt
{
	// This is a structure holding implementations for all supported intents.
	private static class cmsIntentsList
	{
		public int Intent;
		public String Description;
	    public cmsIntentFn Link;
	    public cmsIntentsList Next;
	    
	    public cmsIntentsList()
	    {
	    	
	    }
	    
	    public cmsIntentsList(int Intent, String Description, cmsIntentFn Link, cmsIntentsList Next)
	    {
	    	this.Intent = Intent;
	    	this.Description = Description;
	    	this.Link = Link;
	    	this.Next = Next;
	    }
	}
	
	private static final long INTENDS_UID = 0xF3C13ED7EE94FB24L;
	
	// Search the list for a suitable intent. Returns NULL if not found
	private static cmsIntentsList SearchIntent(int Intent)
	{
	    cmsIntentsList pt;
	    
	    if(Intents == null)
	    {
		    Object obj;
		    if((obj = Utility.singletonStorageGet(INTENDS_UID)) != null)
			{
		    	Intents = (cmsIntentsList)obj;
			}
			else
			{
				Intents = DefaultIntents;
				Utility.singletonStorageSet(INTENDS_UID, Intents);
			}
	    }
	    for (pt = Intents; pt != null; pt = pt.Next)
	    {
	    	if (pt.Intent == Intent)
	    	{
	    		return pt;
	    	}
	    }
	    return null;
	}
	
	// Black point compensation. Implemented as a linear scaling in XYZ. Black points 
	// should come relative to the white point. Fills an matrix/offset element m
	// which is organized as a 4x4 matrix.
	private static void ComputeBlackPointCompensation(final cmsCIEXYZ BlackPointIn, final cmsCIEXYZ BlackPointOut, cmsMAT3 m, cmsVEC3 off)
	{
		double ax, ay, az, bx, by, bz, tx, ty, tz;
		
		// Now we need to compute a matrix plus an offset m and of such of
		// [m]*bpin + off = bpout
		// [m]*D50  + off = D50
		//
		// This is a linear scaling in the form ax+b, where
		// a = (bpout - D50) / (bpin - D50)
		// b = - D50* (bpout - bpin) / (bpin - D50)
		
		tx = 1 / (BlackPointIn.X - lcms2.cmsD50_XYZ.X);
		ty = 1 / (BlackPointIn.Y - lcms2.cmsD50_XYZ.Y);
		tz = 1 / (BlackPointIn.Z - lcms2.cmsD50_XYZ.Z);
		
		ax = (BlackPointOut.X - lcms2.cmsD50_XYZ.X) * tx;
		ay = (BlackPointOut.Y - lcms2.cmsD50_XYZ.Y) * ty;
		az = (BlackPointOut.Z - lcms2.cmsD50_XYZ.Z) * tz;
		
		bx = - lcms2.cmsD50_XYZ.X * (BlackPointOut.X - BlackPointIn.X) * tx;
		by = - lcms2.cmsD50_XYZ.Y * (BlackPointOut.Y - BlackPointIn.Y) * ty;
		bz = - lcms2.cmsD50_XYZ.Z * (BlackPointOut.Z - BlackPointIn.Z) * tz;
		
		cmsmtrx._cmsVEC3init(m.v[0], ax, 0,  0);
		cmsmtrx._cmsVEC3init(m.v[1], 0, ay,  0);
		cmsmtrx._cmsVEC3init(m.v[2], 0,  0,  az);
		cmsmtrx._cmsVEC3init(off, bx, by, bz);
	}
	
	// Approximate a blackbody illuminant based on CHAD information
	private static double CHAD2Temp(final cmsMAT3 Chad)
	{
	    // Convert D50 across CHAD to get the absolute white point
		cmsVEC3 d = new cmsVEC3(), s = new cmsVEC3();
		cmsCIEXYZ Dest = new cmsCIEXYZ();
		cmsCIExyY DestChromaticity = new cmsCIExyY();
		double[] TempK = new double[1];
		
		s.n[lcms2_plugin.VX] = lcms2.cmsD50_XYZ.X;
	    s.n[lcms2_plugin.VY] = lcms2.cmsD50_XYZ.Y;
	    s.n[lcms2_plugin.VZ] = lcms2.cmsD50_XYZ.Z;
	    
	    cmsmtrx._cmsMAT3eval(d, Chad, s);
	    
	    Dest.X = d.n[lcms2_plugin.VX];
	    Dest.Y = d.n[lcms2_plugin.VY];
	    Dest.Z = d.n[lcms2_plugin.VZ];
	    
	    cmspcs.cmsXYZ2xyY(DestChromaticity, Dest);
	    
	    if (!cmswtpnt.cmsTempFromWhitePoint(TempK, DestChromaticity))
	    {
	    	return -1.0;
	    }
	    
	    return TempK[0];
	}
	
	// Compute a CHAD based on a given temperature
	private static void Temp2CHAD(cmsMAT3 Chad, double Temp)
	{
	    cmsCIEXYZ White = new cmsCIEXYZ();
	    cmsCIExyY ChromaticityOfWhite = new cmsCIExyY();
	    
	    cmswtpnt.cmsWhitePointFromTemp(ChromaticityOfWhite, Temp);  
	    cmspcs.cmsxyY2XYZ(White, ChromaticityOfWhite);
	    cmswtpnt._cmsAdaptationMatrix(Chad, null, lcms2.cmsD50_XYZ, White);

	}
	
	// Join scalings to obtain relative input to absolute and then to relative output.
	// Result is stored in a 3x3 matrix
	private static boolean ComputeAbsoluteIntent(double AdaptationState, final cmsCIEXYZ WhitePointIn, final cmsMAT3 ChromaticAdaptationMatrixIn, 
			final cmsCIEXYZ WhitePointOut, final cmsMAT3 ChromaticAdaptationMatrixOut, cmsMAT3 m)
	{
	    cmsMAT3 Scale, m1, m2, m3;
	    
	    // Adaptation state
	    if (AdaptationState == 1.0)
	    {
	        // Observer is fully adapted. Keep chromatic adaptation. 
	        // That is the standard V4 behaviour
	    	cmsmtrx._cmsVEC3init(m.v[0], WhitePointIn.X / WhitePointOut.X, 0, 0);
	    	cmsmtrx._cmsVEC3init(m.v[1], 0, WhitePointIn.Y / WhitePointOut.Y, 0);
	    	cmsmtrx._cmsVEC3init(m.v[2], 0, 0, WhitePointIn.Z / WhitePointOut.Z);
	    }
	    else
	    {
	    	Scale = new cmsMAT3();
	    	
	        // Incomplete adaptation. This is an advanced feature.
	    	cmsmtrx._cmsVEC3init(Scale.v[0], WhitePointIn.X / WhitePointOut.X, 0, 0);
	    	cmsmtrx._cmsVEC3init(Scale.v[1], 0,  WhitePointIn.Y / WhitePointOut.Y, 0);
	    	cmsmtrx._cmsVEC3init(Scale.v[2], 0, 0,  WhitePointIn.Z / WhitePointOut.Z);
	        
	        m1 = ChromaticAdaptationMatrixIn;
	        m2 = new cmsMAT3();
	        if (!cmsmtrx._cmsMAT3inverse(m1, m2))
	        {
	        	return false; 
	        }
	        m3 = new cmsMAT3();
	        cmsmtrx._cmsMAT3per(m3, m2, Scale);
	        
	        // m3 holds CHAD from input white to D50 times abs. col. scaling
	        if (AdaptationState == 0.0)
	        {
	            // Observer is not adapted, undo the chromatic adaptation
	        	cmsmtrx._cmsMAT3per(m, m3, ChromaticAdaptationMatrixOut);
	        }
	        else
	        {
	            cmsMAT3 MixedCHAD;
	            double TempSrc, TempDest, Temp;
	            
	            TempSrc  = CHAD2Temp(ChromaticAdaptationMatrixIn);  // K for source white
	            TempDest = CHAD2Temp(ChromaticAdaptationMatrixOut); // K for dest white
	            
	            if (TempSrc < 0.0 || TempDest < 0.0)
	            {
	            	return false; // Something went wrong
	            }
	            
	            if (cmsmtrx._cmsMAT3isIdentity(Scale) && Math.abs(TempSrc - TempDest) < 0.01)
	            {
	            	cmsmtrx._cmsMAT3identity(m);
	                return true;
	            }
	            
	            Temp = AdaptationState * TempSrc + (1.0 - AdaptationState) * TempDest;
	            
	            // Get a CHAD from D50 to whatever output temperature. This replaces output CHAD
	            MixedCHAD = new cmsMAT3();
	            Temp2CHAD(MixedCHAD, Temp);
	            
	            cmsmtrx._cmsMAT3per(m, m3, MixedCHAD);
	        }
	    }
	    return true;
	}
	
	// Just to see if m matrix should be applied
	private static boolean IsEmptyLayer(cmsMAT3 m, cmsVEC3 off)
	{
	    double diff = 0;
	    cmsMAT3 Ident = new cmsMAT3();
	    int i;
	    
	    if (m == null && off == null)
	    {
	    	return true; // NULL is allowed as an empty layer
	    }
	    if (m == null && off != null)
	    {
	    	return false; // This is an internal error
	    }
	    
	    cmsmtrx._cmsMAT3identity(Ident);
	    
	    double[] mVals = new double[3*3];
	    cmsmtrx._cmsMAT3get(m, mVals, 0);
	    double[] identVals = new double[3*3];
	    cmsmtrx._cmsMAT3get(Ident, identVals, 0);
	    for (i=0; i < 3*3; i++)
	    {
	    	diff += Math.abs(mVals[i] - identVals[i]);
	    }
	    
	    for (i=0; i < 3; i++)
	    {
	    	diff += Math.abs(off.n[i]);
	    }
	    
	    return (diff < 0.002);
	}
	
	// Compute the conversion layer
	private static boolean ComputeConversion(int i, cmsHPROFILE[] hProfiles, int Intent, boolean BPC, double AdaptationState, cmsMAT3 m, cmsVEC3 off)
	{
	    int k;
	    
	    // m  and off are set to identity and this is detected latter on
	    cmsmtrx._cmsMAT3identity(m);
	    cmsmtrx._cmsVEC3init(off, 0, 0, 0);
	    
	    // If intent is abs. colorimetric,
	    if (Intent == lcms2.INTENT_ABSOLUTE_COLORIMETRIC)
	    {
	        cmsCIEXYZ WhitePointIn = new cmsCIEXYZ(), WhitePointOut = new cmsCIEXYZ();
	        cmsMAT3 ChromaticAdaptationMatrixIn = new cmsMAT3(), ChromaticAdaptationMatrixOut = new cmsMAT3();  
	        
	        cmsio1._cmsReadMediaWhitePoint(WhitePointIn,  hProfiles[i-1]);
	        cmsio1._cmsReadCHAD(ChromaticAdaptationMatrixIn, hProfiles[i-1]);
	        
	        cmsio1._cmsReadMediaWhitePoint(WhitePointOut,  hProfiles[i]);
	        cmsio1._cmsReadCHAD(ChromaticAdaptationMatrixOut, hProfiles[i]);
	        
	        if (!ComputeAbsoluteIntent(AdaptationState, 
	                                  WhitePointIn,  ChromaticAdaptationMatrixIn, 
	                                  WhitePointOut, ChromaticAdaptationMatrixOut, m))
	        {
	        	return false;
	        }
	    }
	    else
	    {
	        // Rest of intents may apply BPC.
	    	
	        if (BPC)
	        {
	            cmsCIEXYZ BlackPointIn = new cmsCIEXYZ(), BlackPointOut = new cmsCIEXYZ();
	            
	            cmssamp.cmsDetectBlackPoint(BlackPointIn,  hProfiles[i-1], Intent, 0);
	            cmssamp.cmsDetectBlackPoint(BlackPointOut, hProfiles[i], Intent, 0);
	            
	            // If black points are equal, then do nothing
	            if (BlackPointIn.X != BlackPointOut.X ||
	                BlackPointIn.Y != BlackPointOut.Y ||
	                BlackPointIn.Z != BlackPointOut.Z)
	            {
	            	ComputeBlackPointCompensation(BlackPointIn, BlackPointOut, m, off);
	            }
	        }
	    }
	    
	    // Offset should be adjusted because the encoding. We encode XYZ normalized to 0..1.0,
	    // to do that, we divide by MAX_ENCODEABLE_XZY. The conversion stage goes XYZ -> XYZ so
	    // we have first to convert from encoded to XYZ and then convert back to encoded. 
	    // y = Mx + Off
	    // x = x'c
	    // y = M x'c + Off
	    // y = y'c; y' = y / c
	    // y' = (Mx'c + Off) /c = Mx' + (Off / c) 
	    
	    for (k=0; k < 3; k++)
	    {
	        off.n[k] /= lcms2_internal.MAX_ENCODEABLE_XYZ;
	    }
	    
	    return true;
	}
	
	// Add a conversion stage if needed. If a matrix/offset m is given, it applies to XYZ space 
	private static boolean AddConversion(cmsPipeline Result, int InPCS, int OutPCS, cmsMAT3 m, cmsVEC3 off)
	{
		double[] m_as_dbl = new double[3*3];
		cmsmtrx._cmsMAT3get(m, m_as_dbl, 0);
		double[] off_as_dbl = new double[3];
		cmsmtrx._cmsVEC3get(off, off_as_dbl, 0);
		
	    // Handle PCS mismatches. A specialized stage is added to the LUT in such case
	    switch (InPCS)
	    {
	        case lcms2.cmsSigXYZData: // Input profile operates in XYZ
	            switch (OutPCS)
	            {
		            case lcms2.cmsSigXYZData:  // XYZ -> XYZ
		                if (!IsEmptyLayer(m, off))
		                {
		                	cmslut.cmsPipelineInsertStage(Result, lcms2.cmsAT_END, cmslut.cmsStageAllocMatrix(Result.ContextID, 3, 3, m_as_dbl, off_as_dbl));
		                }
		                break;
		            case lcms2.cmsSigLabData:  // XYZ -> Lab
		                if (!IsEmptyLayer(m, off))
		                {
		                	cmslut.cmsPipelineInsertStage(Result, lcms2.cmsAT_END, cmslut.cmsStageAllocMatrix(Result.ContextID, 3, 3, m_as_dbl, off_as_dbl));
		                }
		                cmslut.cmsPipelineInsertStage(Result, lcms2.cmsAT_END, cmslut._cmsStageAllocXYZ2Lab(Result.ContextID));
		                break;
		            default:
		                return false; // Colorspace mismatch
	                }
	                break;
	        case lcms2.cmsSigLabData: // Input profile operates in Lab
	            switch (OutPCS)
	            {
		            case lcms2.cmsSigXYZData:  // Lab -> XYZ
		            	cmslut.cmsPipelineInsertStage(Result, lcms2.cmsAT_END, cmslut._cmsStageAllocLab2XYZ(Result.ContextID));
		                if (!IsEmptyLayer(m, off))
		                {
		                	cmslut.cmsPipelineInsertStage(Result, lcms2.cmsAT_END, cmslut.cmsStageAllocMatrix(Result.ContextID, 3, 3, m_as_dbl, off_as_dbl));
		                }
		                break;
		            case lcms2.cmsSigLabData: // Lab -> Lab
		                if (!IsEmptyLayer(m, off))
		                {            
		                	cmslut.cmsPipelineInsertStage(Result, lcms2.cmsAT_END, cmslut._cmsStageAllocLab2XYZ(Result.ContextID));        
		                    cmslut.cmsPipelineInsertStage(Result, lcms2.cmsAT_END, cmslut.cmsStageAllocMatrix(Result.ContextID, 3, 3, m_as_dbl, off_as_dbl));
		                    cmslut.cmsPipelineInsertStage(Result, lcms2.cmsAT_END, cmslut._cmsStageAllocXYZ2Lab(Result.ContextID));
		                }
		                break;
		            default:
		                return false; // Mismatch
	            }
	            break;
	            // On colorspaces other than PCS, check for same space
	        default:
	            if (InPCS != OutPCS)
	            {
	            	return false;
	            }
	            break;
	    }
	    
	    return true;
	}
	
	// Is a given space compatible with another?
	private static boolean ColorSpaceIsCompatible(int a, int b)
	{
	    // If they are same, they are compatible.
	    if (a == b)
	    {
	    	return true;
	    }
	    
	    // Check for XYZ/Lab. Those spaces are interchangeable as they can be computed one from other.
	    if ((a == lcms2.cmsSigXYZData) && (b == lcms2.cmsSigLabData))
	    {
	    	return true;
	    }
	    if ((a == lcms2.cmsSigLabData) && (b == lcms2.cmsSigXYZData))
	    {
	    	return true;
	    }
	    
	    return false;
	}
	
	// This is the default routine for ICC-style intents. A user may decide to override it by using a plugin. 
	// Supported intents are perceptual, relative colorimetric, saturation and ICC-absolute colorimetric
	
	// Default handler for ICC-style intents
	private static final cmsIntentFn DefaultICCintents = new cmsIntentFn()
	{
		public cmsPipeline run(cmsContext ContextID, int nProfiles, int[] TheIntents, cmsHPROFILE[] hProfiles, boolean[] BPC, double[] AdaptationStates, int dwFlags)
		{
			cmsPipeline Lut, Result;
		    cmsHPROFILE hProfile;
		    cmsMAT3 m = null;
		    cmsVEC3 off = null;
		    int ColorSpaceIn, ColorSpaceOut, CurrentColorSpace; 
		    int ClassSig;
		    int i, Intent;
		    
			// For safety
			if (nProfiles == 0)
			{
				return null;
			}
			
		    // Allocate an empty LUT for holding the result. 0 as channel count means 'undefined'
		    Result = cmslut.cmsPipelineAlloc(ContextID, 0, 0);
		    if (Result == null)
		    {
		    	return null;
		    }
		    
		    CurrentColorSpace = cmsio0.cmsGetColorSpace(hProfiles[0]);
		    
		    for (i=0; i < nProfiles; i++)
		    {
		        boolean lIsDeviceLink, lIsInput;
		        
		        hProfile      = hProfiles[i];
		        ClassSig      = cmsio0.cmsGetDeviceClass(hProfile);
		        lIsDeviceLink = (ClassSig == lcms2.cmsSigLinkClass || ClassSig == lcms2.cmsSigAbstractClass );
		        
		        // First profile is used as input unless devicelink or abstract
				if ((i == 0) && !lIsDeviceLink)
				{
					lIsInput = true;
				}
				else
				{
					// Else use profile in the input direction if current space is not PCS
					lIsInput = (CurrentColorSpace != lcms2.cmsSigXYZData) && (CurrentColorSpace != lcms2.cmsSigLabData);
				}
				
		        Intent = TheIntents[i];
		        
		        if (lIsInput || lIsDeviceLink)
		        {
		            ColorSpaceIn    = cmsio0.cmsGetColorSpace(hProfile);
		            ColorSpaceOut   = cmsio0.cmsGetPCS(hProfile);
		        }
		        else
		        {
		            ColorSpaceIn    = cmsio0.cmsGetPCS(hProfile);
		            ColorSpaceOut   = cmsio0.cmsGetColorSpace(hProfile);
		        }
		        
		        if (!ColorSpaceIsCompatible(ColorSpaceIn, CurrentColorSpace))
		        {
		            cmserr.cmsSignalError(ContextID, lcms2.cmsERROR_COLORSPACE_CHECK, Utility.LCMS_Resources.getString(LCMSResource.CMSCNVRT_COLORSPACE_MISMATCH), null);
		            if (Result != null)
				    {
				    	cmslut.cmsPipelineFree(Result);
				    }
				    return null;
		        }
		        
		        // If devicelink is found, then no custom intent is allowed and we can 
		        // read the LUT to be applied. Settings don't apply here.       
		        if (lIsDeviceLink)
		        {
		            // Get the involved LUT from the profile
		            Lut = cmsio1._cmsReadDevicelinkLUT(hProfile, Intent);
		            if (Lut == null)
		            {
		            	if (Result != null)
		    		    {
		    		    	cmslut.cmsPipelineFree(Result);
		    		    }
		    		    return null;
		            }
		            
		            if(m == null)
		            {
			            m = new cmsMAT3();
			            off = new cmsVEC3();
		            }
		            // What about abstract profiles?
		            if (ClassSig == lcms2.cmsSigAbstractClass && i > 0)
		            {
		            	if (!ComputeConversion(i, hProfiles, Intent, BPC[i], AdaptationStates[i], m, off))
			            {
			            	if (Result != null)
			    		    {
			    		    	cmslut.cmsPipelineFree(Result);
			    		    }
			    		    return null;
			            }
		             }
		             else
		             {
		                cmsmtrx._cmsMAT3identity(m);
		                cmsmtrx._cmsVEC3init(off, 0, 0, 0);
		             }
		             
		            if (!AddConversion(Result, CurrentColorSpace, ColorSpaceIn, m, off))
		            {
		            	if (Result != null)
		    		    {
		    		    	cmslut.cmsPipelineFree(Result);
		    		    }
		    		    return null;
		            }
		        }
		        else
		        {
		            if (lIsInput)
		            {
		                // Input direction means non-pcs connection, so proceed like devicelinks
		                Lut = cmsio1._cmsReadInputLUT(hProfile, Intent);
		                if (Lut == null)
			            {
			            	if (Result != null)
			    		    {
			    		    	cmslut.cmsPipelineFree(Result);
			    		    }
			    		    return null;
			            }
		            }
		            else
		            {
		                // Output direction means PCS connection. Intent may apply here
		                Lut = cmsio1._cmsReadOutputLUT(hProfile, Intent); 
		                if (Lut == null)
			            {
			            	if (Result != null)
			    		    {
			    		    	cmslut.cmsPipelineFree(Result);
			    		    }
			    		    return null;
			            }
		                
		                if(m == null)
		                {
			                m = new cmsMAT3();
			                off = new cmsVEC3();
		                }
		                if (!ComputeConversion(i, hProfiles, Intent, BPC[i], AdaptationStates[i], m, off))
			            {
			            	if (Result != null)
			    		    {
			    		    	cmslut.cmsPipelineFree(Result);
			    		    }
			    		    return null;
			            }
		                if (!AddConversion(Result, CurrentColorSpace, ColorSpaceIn, m, off))
			            {
			            	if (Result != null)
			    		    {
			    		    	cmslut.cmsPipelineFree(Result);
			    		    }
			    		    return null;
			            }
		            }
		        }
		        
		        // Concatenate to the output LUT
		        cmslut.cmsPipelineCat(Result, Lut);
		        cmslut.cmsPipelineFree(Lut);
		        
		        // Update current space
		        CurrentColorSpace = ColorSpaceOut;
		    }
		    
		    return Result;
		}
	};
	
	// Wrapper for DLL calling convention
	public static cmsPipeline _cmsDefaultICCintents(cmsContext ContextID, int nProfiles, int[] TheIntents, cmsHPROFILE[] hProfiles, boolean[] BPC, double[] AdaptationStates, int dwFlags)
	{
	    return DefaultICCintents.run(ContextID, nProfiles, TheIntents, hProfiles, BPC, AdaptationStates, dwFlags);
	}
	
	// Black preserving intents ---------------------------------------------------------------------------------------------
	
	// Translate black-preserving intents to ICC ones
	private static int TranslateNonICCIntents(int Intent)
	{
	    switch (Intent)
	    {
	        case lcms2.INTENT_PRESERVE_K_ONLY_PERCEPTUAL:
	        case lcms2.INTENT_PRESERVE_K_PLANE_PERCEPTUAL:
	            return lcms2.INTENT_PERCEPTUAL; 
	        case lcms2.INTENT_PRESERVE_K_ONLY_RELATIVE_COLORIMETRIC:
	        case lcms2.INTENT_PRESERVE_K_PLANE_RELATIVE_COLORIMETRIC:
	            return lcms2.INTENT_RELATIVE_COLORIMETRIC;
	        case lcms2.INTENT_PRESERVE_K_ONLY_SATURATION:
	        case lcms2.INTENT_PRESERVE_K_PLANE_SATURATION:
	            return lcms2.INTENT_SATURATION;
	        default:
	        	return Intent;
	    }
	}
	
	// Sampler for Black-only preserving CMYK->CMYK transforms
	
	private static class GrayOnlyParams
	{
		public cmsPipeline cmyk2cmyk;	// The original transform
		public cmsToneCurve KTone;		// Black-to-black tone curve
	}
	
	// Preserve black only if that is the only ink used
	private static final cmsSAMPLER16 BlackPreservingGrayOnlySampler = new cmsSAMPLER16()
	{
		public int run(short[] In, short[] Out, Object Cargo)
		{
			GrayOnlyParams bp = (GrayOnlyParams)Cargo;
			
		    // If going across black only, keep black only
		    if (In[0] == 0 && In[1] == 0 && In[2] == 0)
		    {
		        // TAC does not apply because it is black ink!
		        Out[0] = Out[1] = Out[2] = 0;
		        Out[3] = cmsgamma.cmsEvalToneCurve16(bp.KTone, In[3]);
		        return 1;
		    }
		    
		    // Keep normal transform for other colors
		    bp.cmyk2cmyk.Eval16Fn.run(In, Out, bp.cmyk2cmyk.Data);
		    return 1;
		}
	};
	
	// This is the entry for black-preserving K-only intents, which are non-ICC. Last profile have to be a output profile
	// to do the trick (no devicelinks allowed at that position)
	
	// This is the entry for black-preserving K-only intents, which are non-ICC
	private static final cmsIntentFn BlackPreservingKOnlyIntents = new cmsIntentFn()
	{
		public cmsPipeline run(cmsContext ContextID, int nProfiles, int[] TheIntents, cmsHPROFILE[] hProfiles, boolean[] BPC, double[] AdaptationStates, int dwFlags)
		{
			GrayOnlyParams bp;
		    cmsPipeline Result;
		    int[] ICCIntents = new int[256];
		    cmsStage CLUT;
		    int i, nGridPoints;
		    
		    // Sanity check
		    if (nProfiles < 1 || nProfiles > 255)
		    {
		    	return null;
		    }
		    
		    // Translate black-preserving intents to ICC ones
		    for (i=0; i < nProfiles; i++)
		    {
		    	ICCIntents[i] = TranslateNonICCIntents(TheIntents[i]); 
		    }
		    
		    // Check for non-cmyk profiles
		    if (cmsio0.cmsGetColorSpace(hProfiles[0]) != lcms2.cmsSigCmykData || cmsio0.cmsGetColorSpace(hProfiles[nProfiles-1]) != lcms2.cmsSigCmykData)
		    {
		    	return DefaultICCintents.run(ContextID, nProfiles, ICCIntents, hProfiles, BPC, AdaptationStates, dwFlags);
		    }
		    
		    bp = new GrayOnlyParams();
		    
		    // Allocate an empty LUT for holding the result
		    Result = cmslut.cmsPipelineAlloc(ContextID, 4, 4);
		    if (Result == null)
		    {
		    	return null;
		    }
		    
		    // Create a LUT holding normal ICC transform
		    bp.cmyk2cmyk = DefaultICCintents.run(ContextID, 
		        nProfiles,
		        ICCIntents, 
		        hProfiles, 
		        BPC,
		        AdaptationStates,
		        dwFlags);
		    
		    if (bp.cmyk2cmyk == null)
		    {
			    if (bp.KTone != null)
			    {
			    	cmsgamma.cmsFreeToneCurve(bp.KTone);
			    }
			    if (Result != null)
			    {
			    	cmslut.cmsPipelineFree(Result);
			    }
			    return null;
		    }
		    
		    // Now, compute the tone curve
		    bp.KTone = cmsgmt._cmsBuildKToneCurve(ContextID, 
		        4096, 
		        nProfiles,
		        ICCIntents, 
		        hProfiles, 
		        BPC,
		        AdaptationStates,
		        dwFlags);
		    
		    if (bp.KTone == null)
		    {
		    	if (bp.cmyk2cmyk != null)
			    {
			    	cmslut.cmsPipelineFree(bp.cmyk2cmyk);
			    }
			    if (bp.KTone != null)
			    {
			    	cmsgamma.cmsFreeToneCurve(bp.KTone);
			    }
			    if (Result != null)
			    {
			    	cmslut.cmsPipelineFree(Result);
			    }
			    return null;
		    }
		    
		    // How many gridpoints are we going to use?
		    nGridPoints = cmspcs._cmsReasonableGridpointsByColorspace(lcms2.cmsSigCmykData, dwFlags);
		    
		    // Create the CLUT. 16 bits
		    CLUT = cmslut.cmsStageAllocCLut16bit(ContextID, nGridPoints, 4, 4, null);
		    if (CLUT == null)
		    {
		    	if (bp.cmyk2cmyk != null)
			    {
			    	cmslut.cmsPipelineFree(bp.cmyk2cmyk);
			    }
			    if (bp.KTone != null)
			    {
			    	cmsgamma.cmsFreeToneCurve(bp.KTone);
			    }
			    if (Result != null)
			    {
			    	cmslut.cmsPipelineFree(Result);
			    }
			    return null;
		    }
		    
		    // This is the one and only MPE in this LUT
		    cmslut.cmsPipelineInsertStage(Result, lcms2.cmsAT_BEGIN, CLUT);
		    
		    // Sample it. We cannot afford pre/post linearization this time.
		    if (!cmslut.cmsStageSampleCLut16bit(CLUT, BlackPreservingGrayOnlySampler, bp, 0))
		    {
		    	if (bp.cmyk2cmyk != null)
			    {
			    	cmslut.cmsPipelineFree(bp.cmyk2cmyk);
			    }
			    if (bp.KTone != null)
			    {
			    	cmsgamma.cmsFreeToneCurve(bp.KTone);
			    }
			    if (Result != null)
			    {
			    	cmslut.cmsPipelineFree(Result);
			    }
			    return null;
		    }
		    
		    // Get rid of xform and tone curve
		    cmslut.cmsPipelineFree(bp.cmyk2cmyk);
		    cmsgamma.cmsFreeToneCurve(bp.KTone);
		    
		    return Result;
		}
	};
	
	// K Plane-preserving CMYK to CMYK ------------------------------------------------------------------------------------
	
	private static class PreserveKPlaneParams
	{
		public cmsPipeline		cmyk2cmyk;     // The original transform
		public cmsHTRANSFORM	hProofOutput;  // Output CMYK to Lab (last profile)
		public cmsHTRANSFORM	cmyk2Lab;      // The input chain
		public cmsToneCurve		KTone;         // Black-to-black tone curve
		public cmsPipeline		LabK2cmyk;     // The output profile
		public double			MaxError;
		
		public cmsHTRANSFORM	hRoundTrip;               
	    public double			MaxTAC;
	}
	
	// The CLUT will be stored at 16 bits, but calculations are performed at cmsFloat32Number precision
	private static final cmsSAMPLER16 BlackPreservingSampler = new cmsSAMPLER16()
	{
		public int run(short[] In, short[] Out, Object Cargo)
		{
			int i;
		    float[] Inf = new float[4], Outf = new float[4];
		    float[] LabK = new float[4];   
		    double SumCMY, SumCMYK, Error, Ratio;
		    cmsCIELab ColorimetricLab, BlackPreservingLab;
		    PreserveKPlaneParams bp = (PreserveKPlaneParams)Cargo;
		    
		    // Convert from 16 bits to floating point
		    for (i=0; i < 4; i++)
		    {
		    	Inf[i] = ((In[i] & 0xFFFF) / 65535f);
		    }
		    
		    // Get the K across Tone curve
		    LabK[3] = cmsgamma.cmsEvalToneCurveFloat(bp.KTone, Inf[3]);
		    
		    // If going across black only, keep black only
		    if (In[0] == 0 && In[1] == 0 && In[2] == 0)
		    {
		        Out[0] = Out[1] = Out[2] = 0;
		        Out[3] = lcms2_internal._cmsQuickSaturateWord(LabK[3] * 65535.0);
		        return 1;
		    }
		    
		    // Try the original transform, 
		    cmslut.cmsPipelineEvalFloat(Inf, Outf, bp.cmyk2cmyk);  
		    
		    // Store a copy of the floating point result into 16-bit
		    for (i=0; i < 4; i++)
		    {
		    	Out[i] = lcms2_internal._cmsQuickSaturateWord(Outf[i] * 65535.0);
		    }
		    
		    // Maybe K is already ok (mostly on K=0)
		    if (Math.abs(Outf[3] - LabK[3]) < (3f / 65535f))
		    {
		        return 1;
		    }
		    
		    // K differ, mesure and keep Lab measurement for further usage
		    // this is done in relative colorimetric intent
		    ColorimetricLab = new cmsCIELab();
		    cmsxform.cmsDoTransform(bp.hProofOutput, Out, ColorimetricLab, 1);
		    
		    // Is not black only and the transform doesn't keep black.
		    // Obtain the Lab of output CMYK. After that we have Lab + K
		    cmsxform.cmsDoTransform(bp.cmyk2Lab, Outf, LabK, 1);
		    
		    // Obtain the corresponding CMY using reverse interpolation 
		    // (K is fixed in LabK[3])
		    if (!cmslut.cmsPipelineEvalReverseFloat(LabK, Outf, Outf, bp.LabK2cmyk))
		    {
		        // Cannot find a suitable value, so use colorimetric xform
		        // which is already stored in Out[]
		        return 1;
		    }
		    
		    // Make sure to pass thru K (which now is fixed)
		    Outf[3] = LabK[3];
		    
		    // Apply TAC if needed    
		    SumCMY   = Outf[0]  + Outf[1] + Outf[2];
		    SumCMYK  = SumCMY + Outf[3];
		    
		    if (SumCMYK > bp.MaxTAC)
		    {
		        Ratio = 1 - ((SumCMYK - bp.MaxTAC) / SumCMY);
		        if (Ratio < 0)
		        {
		        	Ratio = 0;
		        }
		    }
		    else
		    {
		    	Ratio = 1.0;
		    }
		    
		    Out[0] = lcms2_internal._cmsQuickSaturateWord(Outf[0] * Ratio * 65535.0);     // C
		    Out[1] = lcms2_internal._cmsQuickSaturateWord(Outf[1] * Ratio * 65535.0);     // M
		    Out[2] = lcms2_internal._cmsQuickSaturateWord(Outf[2] * Ratio * 65535.0);     // Y
		    Out[3] = lcms2_internal._cmsQuickSaturateWord(Outf[3] * 65535.0);
		    
		    // Estimate the error (this goes 16 bits to Lab DBL)
		    BlackPreservingLab = new cmsCIELab();
		    cmsxform.cmsDoTransform(bp.hProofOutput, Out, BlackPreservingLab, 1);
		    Error = cmspcs.cmsDeltaE(ColorimetricLab, BlackPreservingLab);
		    if (Error > bp.MaxError)
		    {
		    	bp.MaxError = Error;
		    }
		    
		    return 1;
		}
	};
	
	// This is the entry for black-plane preserving, which are non-ICC. Again, Last profile have to be a output profile
	// to do the trick (no devicelinks allowed at that position)
	
	// This is the entry for black-plane preserving, which are non-ICC
	private static final cmsIntentFn BlackPreservingKPlaneIntents = new cmsIntentFn()
	{
		public cmsPipeline run(cmsContext ContextID, int nProfiles, int[] TheIntents, cmsHPROFILE[] hProfiles, boolean[] BPC, double[] AdaptationStates, int dwFlags)
		{
			PreserveKPlaneParams bp;
		    cmsPipeline Result = null;
		    int[] ICCIntents = new int[256];
		    cmsStage CLUT;
		    int i, nGridPoints;
		    cmsHPROFILE hLab;
		    
		    // Sanity check
		    if (nProfiles < 1 || nProfiles > 255)
		    {
		    	return null;
		    }
		    
		    // Translate black-preserving intents to ICC ones
		    for (i=0; i < nProfiles; i++)
		    {
		    	ICCIntents[i] = TranslateNonICCIntents(TheIntents[i]); 
		    }
		    
		    // Check for non-cmyk profiles
		    if (cmsio0.cmsGetColorSpace(hProfiles[0]) != lcms2.cmsSigCmykData || cmsio0.cmsGetColorSpace(hProfiles[nProfiles-1]) != lcms2.cmsSigCmykData)
		    {
		    	return DefaultICCintents.run(ContextID, nProfiles, ICCIntents, hProfiles, BPC, AdaptationStates, dwFlags);
		    }
		    
		    // Allocate an empty LUT for holding the result
		    Result = cmslut.cmsPipelineAlloc(ContextID, 4, 4);
		    if (Result == null)
		    {
		    	return null;
		    }
		    
		    bp = new PreserveKPlaneParams();
		    
		    // We need the input LUT of the last profile, assuming this one is responsible of
		    // black generation. This LUT will be seached in inverse order.
		    bp.LabK2cmyk = cmsio1._cmsReadInputLUT(hProfiles[nProfiles-1], lcms2.INTENT_RELATIVE_COLORIMETRIC);
		    if (bp.LabK2cmyk == null)
		    {
			    if (bp.cmyk2Lab != null)
			    {
			    	cmsxform.cmsDeleteTransform(bp.cmyk2Lab);       
			    }
			    if (bp.hProofOutput != null)
			    {
			    	cmsxform.cmsDeleteTransform(bp.hProofOutput);
			    }
			    
			    if (bp.KTone != null)
			    {
			    	cmsgamma.cmsFreeToneCurve(bp.KTone);   
			    }
			    if (bp.LabK2cmyk != null)
			    {
			    	cmslut.cmsPipelineFree(bp.LabK2cmyk);
			    }
			    
			    return Result;
		    }
		    
		    // Get total area coverage (in 0..1 domain)
		    bp.MaxTAC = cmsgmt.cmsDetectTAC(hProfiles[nProfiles-1]) / 100.0;
		    
		    // Create a LUT holding normal ICC transform
		    bp.cmyk2cmyk = DefaultICCintents.run(ContextID,
		                                         nProfiles,
		                                         ICCIntents, 
		                                         hProfiles, 
		                                         BPC,
		                                         AdaptationStates,
		                                         dwFlags);
		    
		    // Now the tone curve
		    bp.KTone = cmsgmt._cmsBuildKToneCurve(ContextID, 4096, nProfiles,
		                                   ICCIntents, 
		                                   hProfiles, 
		                                   BPC, 
		                                   AdaptationStates,
		                                   dwFlags);
		    
		    // To measure the output, Last profile to Lab
		    hLab = cmsvirt.cmsCreateLab4ProfileTHR(ContextID, null);
		    bp.hProofOutput = cmsxform.cmsCreateTransformTHR(ContextID, hProfiles[nProfiles-1], 
		                                         (4 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE), hLab, lcms2.TYPE_Lab_DBL, 
		                                         lcms2.INTENT_RELATIVE_COLORIMETRIC, 
		                                         lcms2.cmsFLAGS_NOCACHE|lcms2.cmsFLAGS_NOOPTIMIZE);
		    
		    // Same as anterior, but lab in the 0..1 range
		    bp.cmyk2Lab = cmsxform.cmsCreateTransformTHR(ContextID, hProfiles[nProfiles-1], 
		                                         (1 << lcms2.FLOAT_SHIFT_VALUE)|(4 << lcms2.CHANNELS_SHIFT_VALUE)|(4 << lcms2.BYTES_SHIFT_VALUE), hLab, 
												 (1 << lcms2.FLOAT_SHIFT_VALUE)|(3 << lcms2.CHANNELS_SHIFT_VALUE)|(4 << lcms2.BYTES_SHIFT_VALUE), 
												 lcms2.INTENT_RELATIVE_COLORIMETRIC, 
		                                         lcms2.cmsFLAGS_NOCACHE|lcms2.cmsFLAGS_NOOPTIMIZE);
		    cmsio0.cmsCloseProfile(hLab);
		    
		    // Error estimation (for debug only)
		    bp.MaxError = 0;
		    
		    // How many gridpoints are we going to use?
		    nGridPoints = cmspcs._cmsReasonableGridpointsByColorspace(lcms2.cmsSigCmykData, dwFlags);
		    
		    CLUT = cmslut.cmsStageAllocCLut16bit(ContextID, nGridPoints, 4, 4, null);
		    if (CLUT == null)
		    {
		    	if (bp.cmyk2cmyk != null)
			    {
			    	cmslut.cmsPipelineFree(bp.cmyk2cmyk);
			    }
			    if (bp.cmyk2Lab != null)
			    {
			    	cmsxform.cmsDeleteTransform(bp.cmyk2Lab);       
			    }
			    if (bp.hProofOutput != null)
			    {
			    	cmsxform.cmsDeleteTransform(bp.hProofOutput);
			    }
			    
			    if (bp.KTone != null)
			    {
			    	cmsgamma.cmsFreeToneCurve(bp.KTone);   
			    }
			    if (bp.LabK2cmyk != null)
			    {
			    	cmslut.cmsPipelineFree(bp.LabK2cmyk);
			    }
			    
			    return Result;
		    }
		    
		    cmslut.cmsPipelineInsertStage(Result, lcms2.cmsAT_BEGIN, CLUT);
		    
		    cmslut.cmsStageSampleCLut16bit(CLUT, BlackPreservingSampler, bp, 0);
		    
		    if (bp.cmyk2cmyk != null)
		    {
		    	cmslut.cmsPipelineFree(bp.cmyk2cmyk);
		    }
		    if (bp.cmyk2Lab != null)
		    {
		    	cmsxform.cmsDeleteTransform(bp.cmyk2Lab);       
		    }
		    if (bp.hProofOutput != null)
		    {
		    	cmsxform.cmsDeleteTransform(bp.hProofOutput);
		    }
		    
		    if (bp.KTone != null)
		    {
		    	cmsgamma.cmsFreeToneCurve(bp.KTone);   
		    }
		    if (bp.LabK2cmyk != null)
		    {
		    	cmslut.cmsPipelineFree(bp.LabK2cmyk);
		    }
		    
		    return Result;
		}
	};
	
	// Built-in intents
	private static final cmsIntentsList DefaultIntents;
	static
	{
		cmsIntentsList tempIntents = new cmsIntentsList(lcms2.INTENT_PRESERVE_K_PLANE_SATURATION, "Saturation preserving black plane", BlackPreservingKPlaneIntents, null);
		tempIntents = new cmsIntentsList(lcms2.INTENT_PRESERVE_K_PLANE_RELATIVE_COLORIMETRIC, "Relative colorimetric preserving black plane", BlackPreservingKPlaneIntents, tempIntents);
		tempIntents = new cmsIntentsList(lcms2.INTENT_PRESERVE_K_PLANE_PERCEPTUAL, "Perceptual preserving black plane", BlackPreservingKPlaneIntents, tempIntents);
		tempIntents = new cmsIntentsList(lcms2.INTENT_PRESERVE_K_ONLY_SATURATION, "Saturation preserving black ink", BlackPreservingKOnlyIntents, tempIntents);
		tempIntents = new cmsIntentsList(lcms2.INTENT_PRESERVE_K_ONLY_RELATIVE_COLORIMETRIC, "Relative colorimetric preserving black ink", BlackPreservingKOnlyIntents, tempIntents);
		tempIntents = new cmsIntentsList(lcms2.INTENT_PRESERVE_K_ONLY_PERCEPTUAL, "Perceptual preserving black ink", BlackPreservingKOnlyIntents, tempIntents);
		tempIntents = new cmsIntentsList(lcms2.INTENT_ABSOLUTE_COLORIMETRIC, "Absolute colorimetric", DefaultICCintents, tempIntents);
		tempIntents = new cmsIntentsList(lcms2.INTENT_SATURATION, "Saturation", DefaultICCintents, tempIntents);
		tempIntents = new cmsIntentsList(lcms2.INTENT_RELATIVE_COLORIMETRIC, "Relative colorimetric", DefaultICCintents, tempIntents);
		DefaultIntents = new cmsIntentsList(lcms2.INTENT_PERCEPTUAL, "Perceptual", DefaultICCintents, tempIntents);
	}
	
	// A pointer to the begining of the list
	private static cmsIntentsList Intents;
	
	// Link routines ------------------------------------------------------------------------------------------------------
	
	// Link several profiles to obtain a single LUT modelling the whole color transform. Intents, Black point 
	// compensation and Adaptation parameters may vary across profiles. BPC and Adaptation refers to the PCS
	// after the profile. I.e, BPC[0] refers to connexion between profile(0) and profile(1)
	
	// Chain several profiles into a single LUT. It just checks the parameters and then calls the handler
	// for the first intent in chain. The handler may be user-defined. Is up to the handler to deal with the 
	// rest of intents in chain. A maximum of 255 profiles at time are supported, which is pretty reasonable.
	public static cmsPipeline _cmsLinkProfiles(cmsContext ContextID, int nProfiles, int[] TheIntents, cmsHPROFILE[] hProfiles, boolean[] BPC, 
			double[] AdaptationStates, int dwFlags)
	{
		int i;
		cmsIntentsList Intent;
		
		// Make sure a reasonable number of profiles is provided
		if (nProfiles <= 0 || nProfiles > 255)
		{
			cmserr.cmsSignalError(ContextID, lcms2.cmsERROR_RANGE, Utility.LCMS_Resources.getString(LCMSResource.CMSCNVRT_COULDNT_LINK_PROFILES), new Object[]{new Integer(nProfiles)});
			return null;
		}
		
		for (i=0; i < nProfiles; i++)
		{
			// Check if black point is really needed or allowed. Note that 
			// following Adobe's document:
			// BPC does not apply to devicelink profiles, nor to abs colorimetric, 
			// and applies always on V4 perceptual and saturation.
			
			if (TheIntents[i] == lcms2.INTENT_ABSOLUTE_COLORIMETRIC)
			{
				BPC[i] = false;
			}
			
			if (TheIntents[i] == lcms2.INTENT_PERCEPTUAL || TheIntents[i] == lcms2.INTENT_SATURATION)
			{
				// Force BPC for V4 profiles in perceptual and saturation
				if (cmsio0.cmsGetProfileVersion(hProfiles[i]) >= 4.0)
				{
					BPC[i] = true;
				}
			}
		}
		
		// Search for a handler. The first intent in the chain defines the handler. That would
		// prevent using multiple custom intents in a multiintent chain, but the behaviour of
		// this case would present some issues if the custom intent tries to do things like
		// preserve primaries. This solution is not perfect, but works well on most cases.
		
		Intent = SearchIntent(TheIntents[0]);
		if (Intent == null)
		{
			cmserr.cmsSignalError(ContextID, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSCNVRT_UNSUPPORTED_INTENT), new Object[]{new Integer(TheIntents[0])});
			return null;
		}
		
		// Call the handler
		return Intent.Link.run(ContextID, nProfiles, TheIntents, hProfiles, BPC, AdaptationStates, dwFlags);
	}
	
	// -------------------------------------------------------------------------------------------------
	
	// Get information about available intents. nMax is the maximum space for the supplied "Codes" 
	// and "Descriptions" the function returns the total number of intents, which may be greater 
	// than nMax, although the matrices are not populated beyond this level.
	public static int cmsGetSupportedIntents(int nMax, int[] Codes, String[] Descriptions)
	{
	    cmsIntentsList pt;
	    int nIntents;
	    
	    if(Intents == null)
	    {
	    	Object obj;
		    if((obj = Utility.singletonStorageGet(INTENDS_UID)) != null)
			{
		    	Intents = (cmsIntentsList)obj;
			}
			else
			{
				Intents = DefaultIntents;
				Utility.singletonStorageSet(INTENDS_UID, Intents);
			}
	    }
	    for (nIntents=0, pt = Intents; pt != null; pt = pt.Next)
	    {
	        if (nIntents < nMax)
	        {
	            if (Codes != null)
	            {
	            	Codes[nIntents] = pt.Intent;
	            }
	            
	            if (Descriptions != null)
	            {
	            	Descriptions[nIntents] = pt.Description;
	            }
	        }
	        
	        nIntents++;
	    }
	    
	    return nIntents;
	}
	
	// The plug-in registration. User can add new intents or override default routines
	public static boolean _cmsRegisterRenderingIntentPlugin(cmsPluginBase Data)
	{
	    cmsPluginRenderingIntent Plugin = (cmsPluginRenderingIntent)Data;
	    cmsIntentsList fl;
	    
	    // Do we have to reset the intents?
	    if (Data == null)
	    {
	       Intents = DefaultIntents;
	       Utility.singletonStorageSet(INTENDS_UID, Intents);
	       return true;
	    }
	    
	    fl = SearchIntent(Plugin.Intent);
	    
	    if (fl == null)
	    {
	        fl = new cmsIntentsList();
	    }
	    
	    fl.Intent  = Plugin.Intent;
	    fl.Description = Plugin.Description.toString();
	    if(fl.Description.length() > 255)
	    {
	    	fl.Description = fl.Description.substring(0, 255);
	    }
	    
	    fl.Link    = Plugin.Link;
	    
	    fl.Next = Intents;
	    Intents = fl;
	    Utility.singletonStorageSet(INTENDS_UID, Intents);
	    
	    return true;
	}
}
