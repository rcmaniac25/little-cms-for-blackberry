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

import net.rim.device.api.util.Arrays;
import littlecms.internal.helper.Utility;
import littlecms.internal.helper.VirtualPointer;
import littlecms.internal.lcms2.cmsContext;
import littlecms.internal.lcms2.cmsHPROFILE;
import littlecms.internal.lcms2.cmsHTRANSFORM;
import littlecms.internal.lcms2.cmsNAMEDCOLORLIST;
import littlecms.internal.lcms2.cmsPipeline;
import littlecms.internal.lcms2_internal._cmsTRANSFORM;
import littlecms.internal.lcms2_internal._cmsTransformFn;

/**
 * Transformations stuff
 */
//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmsxform
{
	// Alarm codes for 16-bit transformations, because the fixed range of containers there are
	// no values left to mark out of gamut. volatile is C99 per 6.2.5
	private static volatile short[] Alarm = new short[lcms2.cmsMAXCHANNELS];
	private static volatile double GlobalAdaptationState = 0;
	
	// The adaptation state may be defaulted by this function. If you don't like it, use the extended transform routine
	public static double cmsSetAdaptationState(double d)
	{
	    double OldVal = GlobalAdaptationState;
	    
	    if (d >= 0)
	    {
	    	GlobalAdaptationState = d;
	    }
	    
	    return OldVal;  
	}
	
	// Alarm codes are always global
	public static void cmsSetAlarmCodes(short[] NewAlarm)
	{
	    int i;
	    
	    lcms2_internal._cmsAssert(NewAlarm != null, "NewAlarm != null");
	    
	    for (i = 0; i < lcms2.cmsMAXCHANNELS; i++)
	    {
	    	Alarm[i] = NewAlarm[i];
	    }
	}
	
	// You can get the codes cas well
	public static void cmsGetAlarmCodes(short[] OldAlarm)
	{
	    int i;
	    
	    lcms2_internal._cmsAssert(OldAlarm != null, "OldAlarm != null");
	    
	    for (i=0; i < lcms2.cmsMAXCHANNELS; i++)
	    {
	    	OldAlarm[i] = Alarm[i];
	    }
	}
	
	// Get rid of transform resources
	public static void cmsDeleteTransform(cmsHTRANSFORM hTransform)
	{
	    _cmsTRANSFORM p = (_cmsTRANSFORM)hTransform;
	    
	    lcms2_internal._cmsAssert(p != null, "p != null");
	    
	    if (p.GamutCheck != null)
	    {
	    	cmslut.cmsPipelineFree(p.GamutCheck);
	    }
	    
	    if (p.Lut != null)
	    {
	    	cmslut.cmsPipelineFree(p.Lut);
	    }
	    
	    if (p.InputColorant != null)
	    {
	    	cmsnamed.cmsFreeNamedColorList(p.InputColorant);
	    }
	    
	    if (p.OutputColorant != null)
	    {
	    	cmsnamed.cmsFreeNamedColorList(p.OutputColorant);
	    }
	    
	    if (p.Sequence != null)
	    {
	    	cmsnamed.cmsFreeProfileSequenceDescription(p.Sequence);
	    }
	    
	    lcms2_internal.LCMS_FREE_LOCK(p.rwlock);
	}
	
	// Apply transform
	public static void cmsDoTransform(cmsHTRANSFORM Transform, final Object InputBuffer, Object OutputBuffer, int Size)
	{
		_cmsTRANSFORM p = (_cmsTRANSFORM)Transform;
	    
		VirtualPointer inBuf = buffer2vp(InputBuffer);
		VirtualPointer outBuf = buffer2vp(OutputBuffer);
	    p.xform.run(p, inBuf, outBuf, Size);
	    vp2buffer(outBuf, OutputBuffer);
	}
	
	private static VirtualPointer buffer2vp(final Object buffer)
	{
		if(buffer == null)
		{
			return null;
		}
		if(buffer instanceof VirtualPointer)
		{
			return (VirtualPointer)buffer;
		}
		return new VirtualPointer(buffer);
	}
	
	private static void vp2buffer(VirtualPointer vp, Object buffer)
	{
		if(vp == null)
		{
			return;
		}
		if(vp == buffer)
		{
			return; //Buffer is a VirtualPointer
		}
		//TODO: figure out how to convert "vp" back to buffer.
	}
	
	// Transform routines ----------------------------------------------------------------------------------------------------------
	
	// Float xform converts floats. Since there are no performance issues, one routine does all job, including gamut check.
	// Note that because extended range, we can use a -1.0 value for out of gamut in this case.
	private static final _cmsTransformFn FloatXFORM = new _cmsTransformFn()
	{	
		public void run(_cmsTRANSFORM p, VirtualPointer in, VirtualPointer out, int Size)
		{
			VirtualPointer accum;
			VirtualPointer output;
		    float[] fIn = new float[lcms2.cmsMAXCHANNELS], fOut = new float[lcms2.cmsMAXCHANNELS];
		    float[] OutOfGamut = new float[1];
		    int i, j;
		    
		    accum  = in;
		    output = out;
		    
		    for (i = 0; i < Size; i++)
		    {
		        accum = p.FromInputFloat.run(p, fIn, accum, Size);
		        
		        // Any gamut chack to do?
		        if (p.GamutCheck != null)
		        {
		            // Evaluate gamut marker.
		        	cmslut.cmsPipelineEvalFloat( fIn, OutOfGamut, p.GamutCheck);
		            
		            // Is current color out of gamut?
		            if (OutOfGamut[0] > 0.0)
		            {
		                // Certainly, out of gamut
		                for (j = 0; j < lcms2.cmsMAXCHANNELS; j++)
		                {
		                	fOut[j] = -1f;
		                }
		            }
		            else
		            {
		                // No, proceed normally
		            	cmslut.cmsPipelineEvalFloat(fIn, fOut, p.Lut); 
		            }
		        }
		        else
		        {
		            // No gamut check at all
		        	cmslut.cmsPipelineEvalFloat(fIn, fOut, p.Lut);     
		        }
		        
		        // Back to asked representation
		        output = p.ToOutputFloat.run(p, fOut, output, Size);
		    }
		}
	};
	
	// 16 bit precision -----------------------------------------------------------------------------------------------------------
	
	// Null transformation, only applies formatters. No caché
	private static final _cmsTransformFn NullXFORM = new _cmsTransformFn()
	{	
		public void run(_cmsTRANSFORM p, VirtualPointer in, VirtualPointer out, int Size)
		{
			VirtualPointer accum;
			VirtualPointer output;
		    short[] wIn = new short[lcms2.cmsMAXCHANNELS];
		    int i, n;
		    
		    accum  = in;
		    output = out;
		    n = Size; // Buffer len
		    
		    for (i = 0; i < n; i++)
		    {
		        accum  = p.FromInput.run(p, wIn, accum, Size);
		        output = p.ToOutput.run(p, wIn, output, Size);
		    }
		}
	};
	
	// No gamut check, no cache, 16 bits
	private static final _cmsTransformFn PrecalculatedXFORM = new _cmsTransformFn()
	{	
		public void run(_cmsTRANSFORM p, VirtualPointer in, VirtualPointer out, int Size)
		{
			VirtualPointer accum;
			VirtualPointer output;
			short[] wIn = new short[lcms2.cmsMAXCHANNELS], wOut = new short[lcms2.cmsMAXCHANNELS];
		    int i, n;
		    
		    accum  = in;
		    output = out;
		    n = Size;
		    
		    for (i = 0; i < n; i++)
		    {
		        accum = p.FromInput.run(p, wIn, accum, Size);
		        p.Lut.Eval16Fn.run(wIn, wOut, p.Lut.Data);
		        output = p.ToOutput.run(p, wOut, output, Size);
		    }
		}
	};
	
	// Auxiliar: Handle precalculated gamut check
	private static void TransformOnePixelWithGamutCheck(_cmsTRANSFORM p, final short[] wIn, short[] wOut)
	{
	    short[] wOutOfGamut = new short[1];
	    
	    p.GamutCheck.Eval16Fn.run(wIn, wOutOfGamut, p.GamutCheck.Data);   
	    if (wOutOfGamut[0] >= 1)
	    {
	        short i;
	        
	        for (i = 0; i < p.Lut.OutputChannels; i++)
	        {
	        	wOut[i] = Alarm[i];                      
	        }
	    }
	    else
	    {
	    	p.Lut.Eval16Fn.run(wIn, wOut, p.Lut.Data);   
	    }
	}
	
	// Gamut check, No caché, 16 bits.
	private static final _cmsTransformFn PrecalculatedXFORMGamutCheck = new _cmsTransformFn()
	{	
		public void run(_cmsTRANSFORM p, VirtualPointer in, VirtualPointer out, int Size)
		{
			VirtualPointer accum;
			VirtualPointer output;
			short[] wIn = new short[lcms2.cmsMAXCHANNELS], wOut = new short[lcms2.cmsMAXCHANNELS];
		    int i, n;
		    
		    accum  = in;
		    output = out;
		    n = Size; // Buffer len
		    
		    for (i = 0; i < n; i++)
		    {
		    	accum = p.FromInput.run(p, wIn, accum, Size);
		        TransformOnePixelWithGamutCheck(p, wIn, wOut);
		        output = p.ToOutput.run(p, wOut, output, Size);
		    }
		}
	};
	
	// No gamut check, Caché, 16 bits, 
	private static final _cmsTransformFn CachedXFORM = new _cmsTransformFn()
	{	
		public void run(_cmsTRANSFORM p, VirtualPointer in, VirtualPointer out, int Size)
		{
			VirtualPointer accum;
			VirtualPointer output;
		    short[] wIn = new short[lcms2.cmsMAXCHANNELS], wOut = new short[lcms2.cmsMAXCHANNELS];
		    int i, n;
		    short[] CacheIn = new short[lcms2.cmsMAXCHANNELS], CacheOut = new short[lcms2.cmsMAXCHANNELS];
		    
		    accum  = in;
		    output = out;
		    n = Size; // Buffer len
		    
//#ifndef CMS_DONT_USE_PTHREADS
		    synchronized(p.rwlock.lock)
		    {
//#endif
		    System.arraycopy(p.CacheIn, 0, CacheIn, 0, lcms2.cmsMAXCHANNELS);
		    System.arraycopy(p.CacheOut, 0, CacheOut, 0, lcms2.cmsMAXCHANNELS);
//#ifndef CMS_DONT_USE_PTHREADS
		    }
//#endif
		    
		    for (i = 0; i < n; i++)
		    {
		        accum = p.FromInput.run(p, wIn, accum, Size);
		        
		        if (Arrays.equals(CacheIn, wIn))
		        {
		            System.arraycopy(CacheOut, 0, wOut, 0, lcms2.cmsMAXCHANNELS);
		        }
		        else
		        {
		            p.Lut.Eval16Fn.run(wIn, wOut, p.Lut.Data);
		            
		            System.arraycopy(wIn, 0, CacheIn, 0, lcms2.cmsMAXCHANNELS);
				    System.arraycopy(wOut, 0, CacheOut, 0, lcms2.cmsMAXCHANNELS);
		        }
		        
		        output = p.ToOutput.run(p, wOut, output, Size);            
		    }
		    
//#ifndef CMS_DONT_USE_PTHREADS
		    synchronized(p.rwlock.lock)
		    {
//#endif
		    System.arraycopy(CacheIn, 0, p.CacheIn, 0, lcms2.cmsMAXCHANNELS);
		    System.arraycopy(CacheOut, 0, p.CacheOut, 0, lcms2.cmsMAXCHANNELS);
//#ifndef CMS_DONT_USE_PTHREADS
		    }
//#endif
		}
	};
	
	// All those nice features together
	private static final _cmsTransformFn CachedXFORMGamutCheck = new _cmsTransformFn()
	{	
		public void run(_cmsTRANSFORM p, VirtualPointer in, VirtualPointer out, int Size)
		{
			VirtualPointer accum;
			VirtualPointer output;
		    short[] wIn = new short[lcms2.cmsMAXCHANNELS], wOut = new short[lcms2.cmsMAXCHANNELS];
		    int i, n;
		    short[] CacheIn = new short[lcms2.cmsMAXCHANNELS], CacheOut = new short[lcms2.cmsMAXCHANNELS];
		    
		    accum  = in;
		    output = out;
		    n = Size; // Buffer len
		    
//#ifndef CMS_DONT_USE_PTHREADS
		    synchronized(p.rwlock.lock)
		    {
//#endif
		    System.arraycopy(p.CacheIn, 0, CacheIn, 0, lcms2.cmsMAXCHANNELS);
		    System.arraycopy(p.CacheOut, 0, CacheOut, 0, lcms2.cmsMAXCHANNELS);
//#ifndef CMS_DONT_USE_PTHREADS
		    }
//#endif
		    
		    for (i = 0; i < n; i++)
		    {
		        accum = p.FromInput.run(p, wIn, accum, Size);
		        
		        if (Arrays.equals(CacheIn, wIn))
		        {
		            System.arraycopy(CacheOut, 0, wOut, 0, lcms2.cmsMAXCHANNELS);
		        }
		        else
		        {
		        	TransformOnePixelWithGamutCheck(p, wIn, wOut);
		            System.arraycopy(wIn, 0, CacheIn, 0, lcms2.cmsMAXCHANNELS);
				    System.arraycopy(wOut, 0, CacheOut, 0, lcms2.cmsMAXCHANNELS);
		        }
		        
		        output = p.ToOutput.run(p, wOut, output, Size);            
		    }
		    
//#ifndef CMS_DONT_USE_PTHREADS
		    synchronized(p.rwlock.lock)
		    {
//#endif
		    System.arraycopy(CacheIn, 0, p.CacheIn, 0, lcms2.cmsMAXCHANNELS);
		    System.arraycopy(CacheOut, 0, p.CacheOut, 0, lcms2.cmsMAXCHANNELS);
//#ifndef CMS_DONT_USE_PTHREADS
		    }
//#endif
		}
	};
	
	// Allocate transform struct and set it to defaults
	private static _cmsTRANSFORM AllocEmptyTransform(cmsContext ContextID, int InputFormat, int OutputFormat, int dwFlags)
	{
		// Allocate needed memory
	    _cmsTRANSFORM p = new _cmsTRANSFORM();
	    
	    // Check whatever this is a true floating point transform
	    if (cmspack._cmsFormatterIsFloat(InputFormat) && cmspack._cmsFormatterIsFloat(OutputFormat))
	    {
	        // Get formatter function always return a valid union, but the contents of this union may be NULL.
	        p.FromInputFloat = cmspack._cmsGetFormatter(InputFormat,  lcms2_plugin.cmsFormatterInput, lcms2_plugin.CMS_PACK_FLAGS_FLOAT).getFloat();
	        p.ToOutputFloat  = cmspack._cmsGetFormatter(OutputFormat, lcms2_plugin.cmsFormatterOutput, lcms2_plugin.CMS_PACK_FLAGS_FLOAT).getFloat();
	        
	        if (p.FromInputFloat == null || p.ToOutputFloat == null)
	        {
	            cmserr.cmsSignalError(ContextID, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSXFORM_UNSUPPORTED_RASTER_FORMAT), null);
	            return null;
	        }
	        
	        // Float transforms don't use caché, always are non-NULL
	        p.xform = FloatXFORM;
	    }
	    else
	    {
	        p.FromInput = cmspack._cmsGetFormatter(InputFormat,  lcms2_plugin.cmsFormatterInput, lcms2_plugin.CMS_PACK_FLAGS_16BITS).get16();
	        p.ToOutput  = cmspack._cmsGetFormatter(OutputFormat, lcms2_plugin.cmsFormatterOutput, lcms2_plugin.CMS_PACK_FLAGS_16BITS).get16();

	        if (p.FromInput == null || p.ToOutput == null)
	        {
	            cmserr.cmsSignalError(ContextID, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSXFORM_UNSUPPORTED_RASTER_FORMAT), null);
	            return null;
	        }
	        
	        if ((dwFlags & lcms2.cmsFLAGS_NULLTRANSFORM) != 0)
	        {
	            p.xform = NullXFORM;
	        }
	        else
	        {
	            if ((dwFlags & lcms2.cmsFLAGS_NOCACHE) != 0)
	            {
	                if ((dwFlags & lcms2.cmsFLAGS_GAMUTCHECK) != 0)
	                {
	                	p.xform = PrecalculatedXFORMGamutCheck; // Gamut check, no caché
	                }
	                else
	                {
	                	p.xform = PrecalculatedXFORM; // No caché, no gamut check
	                }
	            }
	            else
	            {
	                if ((dwFlags & lcms2.cmsFLAGS_GAMUTCHECK) != 0)
	                {
	                	p.xform = CachedXFORMGamutCheck; // Gamut check, caché
	                }
	                else
	                {
	                	p.xform = CachedXFORM; // No gamut check, caché
	                }
	            }
	        }
	    }
	    
	    // Create a mutex for shared memory
	    lcms2_internal.LCMS_CREATE_LOCK(p.rwlock);
	    
	    p.InputFormat     = InputFormat;
	    p.OutputFormat    = OutputFormat;
	    p.dwOriginalFlags = dwFlags;
	    p.ContextID       = ContextID;
	    return p;
	}
	
	private static boolean GetXFormColorSpaces(int nProfiles, cmsHPROFILE[] hProfiles, int[] Input, int[] Output) 
	{    
	    int ColorSpaceIn, ColorSpaceOut;   
	    int PostColorSpace;   
	    int i;
	    
		if (hProfiles[0] == null)
		{
			return false;
		}
		
	    Input[0] = PostColorSpace = cmsio0.cmsGetColorSpace(hProfiles[0]);
	    
	    for (i = 0; i < nProfiles; i++)
	    {
	        cmsHPROFILE hProfile = hProfiles[i];
	        
	        boolean lIsInput = (PostColorSpace != lcms2.cmsSigXYZData) &&
	                       (PostColorSpace != lcms2.cmsSigLabData);
	        
			if (hProfile == null)
			{
				return false;
			}
			
	        if (lIsInput)
	        {
	            ColorSpaceIn    = cmsio0.cmsGetColorSpace(hProfile);
	            ColorSpaceOut   = cmsio0.cmsGetPCS(hProfile);
	        }
	        else
	        {
	            ColorSpaceIn    = cmsio0.cmsGetPCS(hProfile);
	            ColorSpaceOut   = cmsio0.cmsGetColorSpace(hProfile);
	        }
	        
	        PostColorSpace = ColorSpaceOut;     
	    }
	    
	    Output[0] = PostColorSpace;
	    
		return true;
	}
	
	// Check colorspace
	private static boolean IsProperColorSpace(int Check, int dwFormat)
	{
	    int Space1 = lcms2.T_COLORSPACE(dwFormat);
	    int Space2 = cmspcs._cmsLCMScolorSpace(Check);
	    
	    if (Space1 == lcms2.PT_ANY)
	    {
	    	return true;
	    }
	    if (Space1 == Space2)
	    {
	    	return true;
	    }
	    
	    if (Space1 == lcms2.PT_LabV2 && Space2 == lcms2.PT_Lab)
	    {
	    	return true;
	    }
	    if (Space1 == lcms2.PT_Lab   && Space2 == lcms2.PT_LabV2)
	    {
	    	return true;
	    }
	    
	    return false;
	}
	
	// ----------------------------------------------------------------------------------------------------------------
	
	// New to lcms 2.0 -- have all parameters available.
	public static cmsHTRANSFORM cmsCreateExtendedTransform(cmsContext ContextID, int nProfiles, cmsHPROFILE[] hProfiles, boolean[] BPC, int[] Intents, 
			double[] AdaptationStates, cmsHPROFILE hGamutProfile, int nGamutPCSposition, int InputFormat, int OutputFormat, int dwFlags)
	{
		_cmsTRANSFORM xform;
	    boolean FloatTransform;
	    int[] EntryColorSpace = new int[1];
	    int[] ExitColorSpace = new int[1];
	    cmsPipeline Lut;
	    int LastIntent = Intents[nProfiles-1];
	    
	    // If gamut check is requested, make sure we have a gamut profile
	    if ((dwFlags & lcms2.cmsFLAGS_GAMUTCHECK) != 0)
	    {
	        if (hGamutProfile == null)
	        {
	        	dwFlags &= ~lcms2.cmsFLAGS_GAMUTCHECK;
	        }
	    }
	    
	    // On floating point transforms, inhibit optimizations 
	    FloatTransform = (cmspack._cmsFormatterIsFloat(InputFormat) && cmspack._cmsFormatterIsFloat(OutputFormat));
	    
	    if (cmspack._cmsFormatterIsFloat(InputFormat) || cmspack._cmsFormatterIsFloat(OutputFormat))
	    {
	    	dwFlags |= lcms2.cmsFLAGS_NOCACHE;
	    }
	    
	    // Mark entry/exit spaces
		if (!GetXFormColorSpaces(nProfiles, hProfiles, EntryColorSpace, ExitColorSpace))
		{
			cmserr.cmsSignalError(ContextID, lcms2.cmsERROR_NULL, Utility.LCMS_Resources.getString(LCMSResource.CMSXFORM_NULL_PROFILES), null);
			return null;
		}
		
	    // Check if proper colorspaces
	    if (!IsProperColorSpace(EntryColorSpace[0], InputFormat))
	    {        
	    	cmserr.cmsSignalError(ContextID, lcms2.cmsERROR_COLORSPACE_CHECK, Utility.LCMS_Resources.getString(LCMSResource.CMSXFORM_WRONG_COLORSPACE_IN), null);
	        return null;
	    }
	    
	    if (!IsProperColorSpace(ExitColorSpace[0], OutputFormat))
	    {
	    	cmserr.cmsSignalError(ContextID, lcms2.cmsERROR_COLORSPACE_CHECK, Utility.LCMS_Resources.getString(LCMSResource.CMSXFORM_WRONG_COLORSPACE_OUT), null);
	        return null;
	    }
	    
	    // Create a pipeline with all transformations
	    Lut = cmscnvrt._cmsLinkProfiles(ContextID, nProfiles, Intents, hProfiles, BPC, AdaptationStates, dwFlags);
	    if (Lut == null)
	    {
	    	cmserr.cmsSignalError(ContextID, lcms2.cmsERROR_NOT_SUITABLE, Utility.LCMS_Resources.getString(LCMSResource.CMSXFORM_CANT_LINK_PROFILES), null);   
	        return null;
	    }
	    
	    // Optimize the LUT if possible
	    int[] temp1 = new int[]{InputFormat};
	    int[] temp2 = new int[]{OutputFormat};
	    int[] temp3 = new int[]{dwFlags};
	    cmsPipeline[] temp4 = new cmsPipeline[]{Lut};
	    cmsopt._cmsOptimizePipeline(temp4, LastIntent, temp1, temp2, temp3);
	    InputFormat = temp1[0];
	    OutputFormat = temp2[0];
	    dwFlags = temp3[0];
	    Lut = temp4[4];
	    
	    // All seems ok
	    xform = AllocEmptyTransform(ContextID, InputFormat, OutputFormat, dwFlags);
	    if (xform == null)
	    {
	        cmslut.cmsPipelineFree(Lut);
	        return null;
	    }
	    
	    // Keep values
	    xform.EntryColorSpace = EntryColorSpace[0];
	    xform.ExitColorSpace  = ExitColorSpace[0];
	    xform.Lut             = Lut;
	    
	    
	    // Create a gamut check LUT if requested
	    if (hGamutProfile != null && ((dwFlags & lcms2.cmsFLAGS_GAMUTCHECK) != 0))
	    {
	        xform.GamutCheck  = cmsgmt._cmsCreateGamutCheckPipeline(ContextID, hProfiles, 
	                                                        BPC, Intents, 
	                                                        AdaptationStates, 
	                                                        nGamutPCSposition, 
	                                                        hGamutProfile);
	    }
	    
	    // Try to read input and output colorant table
	    if (cmsio0.cmsIsTag(hProfiles[0], lcms2.cmsSigColorantTableTag))
	    {
	        // Input table can only come in this way.       
	        xform.InputColorant = cmsnamed.cmsDupNamedColorList((cmsNAMEDCOLORLIST)cmsio0.cmsReadTag(hProfiles[0], lcms2.cmsSigColorantTableTag));
	    }
	    
	    // Output is a little bit more complex.    
	    if (cmsio0.cmsGetDeviceClass(hProfiles[nProfiles-1]) == lcms2.cmsSigLinkClass)
	    {
	        // This tag may exist only on devicelink profiles.        
	        if (cmsio0.cmsIsTag(hProfiles[nProfiles-1], lcms2.cmsSigColorantTableOutTag))
	        {
	            // It may be NULL if error
	            xform.OutputColorant = cmsnamed.cmsDupNamedColorList((cmsNAMEDCOLORLIST)cmsio0.cmsReadTag(hProfiles[nProfiles-1], lcms2.cmsSigColorantTableOutTag));
	        }
	    }
	    else
	    {
	        if (cmsio0.cmsIsTag(hProfiles[nProfiles-1], lcms2.cmsSigColorantTableTag))
	        {
	            xform.OutputColorant = cmsnamed.cmsDupNamedColorList((cmsNAMEDCOLORLIST)cmsio0.cmsReadTag(hProfiles[nProfiles-1], lcms2.cmsSigColorantTableTag));
	        }
	    }
	    
	    // Store the sequence of profiles
	    if ((dwFlags & lcms2.cmsFLAGS_KEEP_SEQUENCE) != 0)
	    {
	        xform.Sequence = cmsio1._cmsCompileProfileSequence(ContextID, nProfiles, hProfiles);
	    }
	    else
	    {
	    	xform.Sequence = null;
	    }
	    
	    // If this is a cached transform, init first value, which is zero (16 bits only)
	    if ((dwFlags & lcms2.cmsFLAGS_NOCACHE) == 0)
	    {
	    	Arrays.zero(xform.CacheIn); //Just as a precaution though it is probably a new array and thus it is zeroed on creation
	        
	        if (xform.GamutCheck != null)
	        {
	            TransformOnePixelWithGamutCheck(xform, xform.CacheIn, xform.CacheOut);
	        }
	        else
	        {
	            xform.Lut.Eval16Fn.run(xform.CacheIn, xform.CacheOut, xform.Lut.Data);  
	        }
	    }
	    
	    return (cmsHTRANSFORM)xform;
	}
	
	// Multiprofile transforms: Gamut check is not available here, as it is unclear from which profile the gamut comes.
	
	public static cmsHTRANSFORM cmsCreateMultiprofileTransformTHR(cmsContext ContextID, cmsHPROFILE[] hProfiles, int nProfiles, int InputFormat, int OutputFormat, 
			int Intent, int dwFlags)
	{
		int i;
	    boolean[] BPC = new boolean[256];
	    int[] Intents = new int[256];
	    double[] AdaptationStates = new double[256];
	    
	    if (nProfiles <= 0 || nProfiles > 255)
	    {
	        cmserr.cmsSignalError(ContextID, lcms2.cmsERROR_RANGE, Utility.LCMS_Resources.getString(LCMSResource.CMSXFORM_WRONG_PROFILE_COUNT), new Object[]{new Integer(nProfiles)});
	        return null;
	    }
	    
	    boolean BPC_value = (dwFlags & lcms2.cmsFLAGS_BLACKPOINTCOMPENSATION) != 0;
	    for (i = 0; i < nProfiles; i++)
	    {
	        BPC[i] = BPC_value;
	        Intents[i] = Intent;
	        AdaptationStates[i] = GlobalAdaptationState;
	    }
	    
	    return cmsCreateExtendedTransform(ContextID, nProfiles, hProfiles, BPC, Intents, AdaptationStates, null, 0, InputFormat, OutputFormat, dwFlags);
	}
	
	public static cmsHTRANSFORM cmsCreateMultiprofileTransform(cmsHPROFILE[] hProfiles, int nProfiles, int InputFormat, int OutputFormat, int Intent, int dwFlags)
	{
		if (nProfiles <= 0 || nProfiles > 255)
		{
	         cmserr.cmsSignalError(null, lcms2.cmsERROR_RANGE, Utility.LCMS_Resources.getString(LCMSResource.CMSXFORM_WRONG_PROFILE_COUNT), new Object[]{new Integer(nProfiles)});
	         return null;
	    }
		
	    return cmsCreateMultiprofileTransformTHR(cmsio0.cmsGetProfileContextID(hProfiles[0]), hProfiles, nProfiles, InputFormat, OutputFormat, Intent, dwFlags);
	}
	
	public static cmsHTRANSFORM cmsCreateTransformTHR(cmsContext ContextID, cmsHPROFILE Input, int InputFormat, cmsHPROFILE Output, int OutputFormat, int Intent, 
			int dwFlags)
	{
		cmsHPROFILE[] hArray = new cmsHPROFILE[2];
		
	    hArray[0] = Input;
	    hArray[1] = Output;
	    
	    return cmsCreateMultiprofileTransformTHR(ContextID, hArray, Output == null ? 1 : 2, InputFormat, OutputFormat, Intent, dwFlags);
	}
	
	public static cmsHTRANSFORM cmsCreateTransform(cmsHPROFILE Input, int InputFormat, cmsHPROFILE Output, int OutputFormat, int Intent, int dwFlags)
	{
		return cmsCreateTransformTHR(cmsio0.cmsGetProfileContextID(Input), Input, InputFormat, Output, OutputFormat, Intent, dwFlags);
	}
	
	public static cmsHTRANSFORM cmsCreateProofingTransformTHR(cmsContext ContextID, cmsHPROFILE InputProfile, int InputFormat, cmsHPROFILE OutputProfile, 
			int OutputFormat, cmsHPROFILE ProofingProfile, int nIntent, int ProofingIntent, int dwFlags)
	{
		cmsHPROFILE[] hArray = new cmsHPROFILE[4];
	    int[] Intents = new int[4];
	    boolean[] BPC = new boolean[4];
	    double[] Adaptation = new double[4];
	    boolean DoBPC = (dwFlags & lcms2.cmsFLAGS_BLACKPOINTCOMPENSATION) != 0;
	    
	    hArray[0]  = InputProfile; hArray[1] = ProofingProfile; hArray[2]  = ProofingProfile;                     hArray[3] = OutputProfile;
	    Intents[0] = nIntent;      Intents[1] = nIntent;        Intents[2] = lcms2.INTENT_RELATIVE_COLORIMETRIC;  Intents[3] = ProofingIntent;
	    BPC[0]     = DoBPC;        BPC[1] = DoBPC;              BPC[2] = false;                                   BPC[3] = false;
	    
	    Adaptation[0] = Adaptation[1] = Adaptation[2] = Adaptation[3] = GlobalAdaptationState;
	    
	    if ((dwFlags & (lcms2.cmsFLAGS_SOFTPROOFING|lcms2.cmsFLAGS_GAMUTCHECK)) == 0)
	    {
	    	return cmsCreateTransformTHR(ContextID, InputProfile, InputFormat, OutputProfile, OutputFormat, nIntent, dwFlags);
	    }
	    
	    return cmsCreateExtendedTransform(ContextID, 4, hArray, BPC, Intents, Adaptation, ProofingProfile, 1, InputFormat, OutputFormat, dwFlags);
	}
	
	public static cmsHTRANSFORM cmsCreateProofingTransform(cmsHPROFILE InputProfile, int InputFormat, cmsHPROFILE OutputProfile, int OutputFormat, 
			cmsHPROFILE ProofingProfile, int nIntent, int ProofingIntent, int dwFlags)
	{
		return cmsCreateProofingTransformTHR(cmsio0.cmsGetProfileContextID(InputProfile), InputProfile, InputFormat, OutputProfile, OutputFormat, ProofingProfile, 
				nIntent, ProofingIntent, dwFlags);
	}
	
	// Grab the ContextID from an open transform. Returns NULL if a NULL transform is passed
	public static cmsContext cmsGetTransformContextID(cmsHTRANSFORM hTransform)
	{
	    _cmsTRANSFORM xform = (_cmsTRANSFORM)hTransform;
	    
	    if (xform == null)
	    {
	    	return null;
	    }
	    return xform.ContextID;
	}
}
