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

import net.rim.device.api.util.Arrays;

import littlecms.internal.lcms2.cmsContext;
import littlecms.internal.lcms2.cmsHPROFILE;
import littlecms.internal.lcms2.cmsHTRANSFORM;
import littlecms.internal.lcms2.cmsNAMEDCOLORLIST;
import littlecms.internal.lcms2.cmsPipeline;
import littlecms.internal.lcms2_internal._cmsTRANSFORM;
import littlecms.internal.lcms2_plugin._cmsTransformFactory;
import littlecms.internal.lcms2_plugin._cmsTransformFn;
import littlecms.internal.lcms2_plugin.cmsPluginBase;
import littlecms.internal.lcms2_plugin.cmsPluginTransform;
import littlecms.internal.helper.Utility;
import littlecms.internal.helper.VirtualPointer;

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
	private static volatile short[] Alarm;
	private static volatile double[] GlobalAdaptationState;
	
	// The adaptation state may be defaulted by this function. If you don't like it, use the extended transform routine
	public static double cmsSetAdaptationState(double d)
	{
	    double OldVal = GlobalAdaptationState[0];
	    
	    if (d >= 0)
	    {
	    	GlobalAdaptationState[0] = d;
	    }
	    
	    return OldVal;  
	}
	
	// Alarm codes are always global
	public static void cmsSetAlarmCodes(short[] NewAlarm)
	{
	    int i;
	    
	    lcms2_internal._cmsAssert(NewAlarm != null, "NewAlarm != null");
	    
	    if(NewAlarm.length < lcms2.cmsMAXCHANNELS)
	    {
	    	short[] alarm = new short[lcms2.cmsMAXCHANNELS];
	    	System.arraycopy(NewAlarm, 0, alarm, 0, NewAlarm.length);
	    	NewAlarm = alarm;
	    }
	    
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
	    
	    for (i=OldAlarm.length - 1; i >= 0 && i < lcms2.cmsMAXCHANNELS; i--)
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
	    
	    if (p.UserData != null)
	    {
	    	p.FreeUserData.run(p.ContextID, p.UserData);
	    }
	}
	
	// Apply transform.
	public static void cmsDoTransform(cmsHTRANSFORM Transform, final Object InputBuffer, Object OutputBuffer, int Size)
	{
		_cmsTRANSFORM p = (_cmsTRANSFORM)Transform;
	    
		VirtualPointer inBuf = buffer2vp(InputBuffer);
		VirtualPointer outBuf = buffer2vp(OutputBuffer);
	    p.xform.run(p, inBuf, outBuf, Size, Size);
	    vp2buffer(outBuf, OutputBuffer);
	    if(inBuf != InputBuffer)
	    {
	    	inBuf.free();
	    }
	    if(outBuf != OutputBuffer)
	    {
	    	outBuf.free();
	    }
	}
	
	// Apply transform. 
	public static void cmsDoTransformStride(cmsHTRANSFORM Transform, final Object InputBuffer, Object OutputBuffer, int Size, int Stride)
	{
	    _cmsTRANSFORM p = (_cmsTRANSFORM)Transform;
	    
	    VirtualPointer inBuf = buffer2vp(InputBuffer);
		VirtualPointer outBuf = buffer2vp(OutputBuffer);
	    p.xform.run(p, inBuf, outBuf, Size, Stride);
	    vp2buffer(outBuf, OutputBuffer);
	    if(inBuf != InputBuffer)
	    {
	    	inBuf.free();
	    }
	    if(outBuf != OutputBuffer)
	    {
	    	outBuf.free();
	    }
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
		VirtualPointer.Serializer ser = buffer == null ? null : VirtualPointer.getSerializer(buffer.getClass());
		return new VirtualPointer(buffer, ser);
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
		boolean primitive = false;
		Class btype = buffer.getClass();
		//If it isn't an array then it must be an Object
		if(btype.isArray())
		{
			//Since it is an array we need to figure out if it is a primitive array or not
			primitive = Utility.isPrimitive(buffer);
		}
		if(primitive)
		{
			readPrimitiveArray(vp, buffer);
		}
		else
		{
			//XXX This might not work for read only value types, if that's the case then VirtualPointer should throw the exception
			VirtualPointer.TypeProcessor proc = vp.getProcessor();
			if(btype.isArray())
			{
				Object[] oBuffer = (Object[])buffer;
				proc.readArray(oBuffer[0].getClass(), false, oBuffer.length, buffer);
			}
			else
			{
				proc.readObject(buffer.getClass(), false, buffer);
			}
		}
	}
	
	private static void readPrimitiveArray(VirtualPointer vp, Object buffer)
	{
		//Slow, inefficient method of reading primitives but no other options exist
		if(buffer instanceof byte[])
    	{
    		byte[] val = (byte[])buffer;
    		vp.readByteArray(val, 0, val.length, false, false);
    	}
		else
		{
			VirtualPointer.TypeProcessor proc = vp.getProcessor();
	    	if(buffer instanceof short[])
	    	{
	    		short[] val = (short[])buffer;
	    		proc.readArray(false, "short", val.length, val);
	    	}
	    	else if(buffer instanceof char[])
	    	{
	    		char[] val = (char[])buffer;
	    		proc.readArray(false, "char", val.length, val);
	    	}
	    	else if(buffer instanceof int[])
	    	{
	    		int[] val = (int[])buffer;
	    		proc.readArray(false, "int", val.length, val);
	    	}
	    	else if(buffer instanceof long[])
	    	{
	    		long[] val = (long[])buffer;
	    		proc.readArray(false, "long", val.length, val);
	    	}
	    	else if(buffer instanceof float[])
	    	{
	    		float[] val = (float[])buffer;
	    		proc.readArray(false, "float", val.length, val);
	    	}
	    	else if(buffer instanceof double[])
	    	{
	    		double[] val = (double[])buffer;
	    		proc.readArray(false, "double", val.length, val);
	    	}
	    	else if(buffer instanceof boolean[])
	    	{
	    		boolean[] val = (boolean[])buffer;
	    		proc.readArray(false, "boolean", val.length, val);
	    	}
	    	else if(buffer instanceof String[])
	    	{
	    		String[] val = (String[])buffer;
	    		proc.readArray(false, "String", val.length, val);
	    	}
	    	else if(buffer instanceof VirtualPointer[])
	    	{
	    		VirtualPointer[] val = (VirtualPointer[])buffer;
	    		proc.readArray(false, "VirtualPointer", val.length, val);
	    	}
		}
	}
	
	// Transform routines ----------------------------------------------------------------------------------------------------------
	
	// Float xform converts floats. Since there are no performance issues, one routine does all job, including gamut check.
	// Note that because extended range, we can use a -1.0 value for out of gamut in this case.
	private static final _cmsTransformFn FloatXFORM = new _cmsTransformFn()
	{	
		public void run(_cmsTRANSFORM p, VirtualPointer in, VirtualPointer out, int Size, int Stride)
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
		        accum = p.FromInputFloat.run(p, fIn, accum, Stride);
		        
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
		        output = p.ToOutputFloat.run(p, fOut, output, Stride);
		    }
		}
	};
	
	// 16 bit precision -----------------------------------------------------------------------------------------------------------
	
	// Null transformation, only applies formatters. No caché
	private static final _cmsTransformFn NullXFORM = new _cmsTransformFn()
	{	
		public void run(_cmsTRANSFORM p, VirtualPointer in, VirtualPointer out, int Size, int Stride)
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
		        accum  = p.FromInput.run(p, wIn, accum, Stride);
		        output = p.ToOutput.run(p, wIn, output, Stride);
		    }
		}
	};
	
	// No gamut check, no cache, 16 bits
	private static final _cmsTransformFn PrecalculatedXFORM = new _cmsTransformFn()
	{	
		public void run(_cmsTRANSFORM p, VirtualPointer in, VirtualPointer out, int Size, int Stride)
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
		        accum = p.FromInput.run(p, wIn, accum, Stride);
		        p.Lut.Eval16Fn.run(wIn, wOut, p.Lut.Data);
		        output = p.ToOutput.run(p, wOut, output, Stride);
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
		public void run(_cmsTRANSFORM p, VirtualPointer in, VirtualPointer out, int Size, int Stride)
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
		    	accum = p.FromInput.run(p, wIn, accum, Stride);
		        TransformOnePixelWithGamutCheck(p, wIn, wOut);
		        output = p.ToOutput.run(p, wOut, output, Stride);
		    }
		}
	};
	
	// No gamut check, Caché, 16 bits, 
	private static final _cmsTransformFn CachedXFORM = new _cmsTransformFn()
	{	
		public void run(_cmsTRANSFORM p, VirtualPointer in, VirtualPointer out, int Size, int Stride)
		{
			VirtualPointer accum;
			VirtualPointer output;
		    short[] wIn = new short[lcms2.cmsMAXCHANNELS], wOut = new short[lcms2.cmsMAXCHANNELS];
		    int i, n;
		    lcms2_internal._cmsCACHE Cache = new lcms2_internal._cmsCACHE();
		    
		    accum  = in;
		    output = out;
		    n = Size; // Buffer len
		    
		    // Get copy of zero cache
		    p.Cache.copyTo(Cache);
		    
		    for (i = 0; i < n; i++)
		    {
		        accum = p.FromInput.run(p, wIn, accum, Stride);
		        
		        if (Arrays.equals(Cache.CacheIn, wIn))
		        {
		            System.arraycopy(Cache.CacheOut, 0, wOut, 0, lcms2.cmsMAXCHANNELS);
		        }
		        else
		        {
		            p.Lut.Eval16Fn.run(wIn, wOut, p.Lut.Data);
		            
		            System.arraycopy(wIn, 0, Cache.CacheIn, 0, lcms2.cmsMAXCHANNELS);
				    System.arraycopy(wOut, 0, Cache.CacheOut, 0, lcms2.cmsMAXCHANNELS);
		        }
		        
		        output = p.ToOutput.run(p, wOut, output, Stride);
		    }
		}
	};
	
	// All those nice features together
	private static final _cmsTransformFn CachedXFORMGamutCheck = new _cmsTransformFn()
	{	
		public void run(_cmsTRANSFORM p, VirtualPointer in, VirtualPointer out, int Size, int Stride)
		{
			VirtualPointer accum;
			VirtualPointer output;
		    short[] wIn = new short[lcms2.cmsMAXCHANNELS], wOut = new short[lcms2.cmsMAXCHANNELS];
		    int i, n;
		    lcms2_internal._cmsCACHE Cache = new lcms2_internal._cmsCACHE();
		    
		    accum  = in;
		    output = out;
		    n = Size; // Buffer len
		    
		    // Get copy of zero cache
		    p.Cache.copyTo(Cache);
		    
		    for (i = 0; i < n; i++)
		    {
		        accum = p.FromInput.run(p, wIn, accum, Stride);
		        
		        if (Arrays.equals(Cache.CacheIn, wIn))
		        {
		            System.arraycopy(Cache.CacheOut, 0, wOut, 0, lcms2.cmsMAXCHANNELS);
		        }
		        else
		        {
		        	TransformOnePixelWithGamutCheck(p, wIn, wOut);
		            System.arraycopy(wIn, 0, Cache.CacheIn, 0, lcms2.cmsMAXCHANNELS);
				    System.arraycopy(wOut, 0, Cache.CacheOut, 0, lcms2.cmsMAXCHANNELS);
		        }
		        
		        output = p.ToOutput.run(p, wOut, output, Stride);            
		    }
		}
	};
	
	// -------------------------------------------------------------------------------------------------------------
	
	// List of used-defined transform factories
	private static class _cmsTransformCollection
	{
		public _cmsTransformFactory Factory;
		public _cmsTransformCollection Next;
		
		public _cmsTransformCollection()
		{
		}
		
		public _cmsTransformCollection(_cmsTransformFactory Factory, _cmsTransformCollection Next)
		{
			this.Factory = Factory;
			this.Next = Next;
		}
	}
	
	// The linked list head
	private static _cmsTransformCollection TransformCollection;
	
	// Register new ways to transform
	public static boolean _cmsRegisterTransformPlugin(cmsPluginBase Data)
	{
	    cmsPluginTransform Plugin = (cmsPluginTransform)Data;
	    _cmsTransformCollection fl;
	    
	    if (Data == null)
	    {
	    	// Free the chain. Memory is safely freed at exit
	        TransformCollection = null; 
	        return true;
	    }
	    
	    // Factory callback is required
	    if (Plugin.Factory == null)
	    {
	    	return false;
	    }
	    
	    fl = new _cmsTransformCollection();
	    
	    // Copy the parameters
	    fl.Factory = Plugin.Factory;
	    
	    // Keep linked list
	    fl.Next = TransformCollection;
	    TransformCollection = fl;
	    
	    // All is ok
	    return true;
	}
	
	// returns the pointer defined by the plug-in to store private data
	public static Object _cmsGetTransformUserData(_cmsTRANSFORM CMMcargo)
	{
	    lcms2_internal._cmsAssert(CMMcargo != null, "CMMcargo != null");
	    return CMMcargo.UserData;
	}
	
	// Allocate transform struct and set it to defaults. Ask the optimization plug-in about if those formats are proper
	// for separated transforms. If this is the case,
	private static _cmsTRANSFORM AllocEmptyTransform(cmsContext ContextID, cmsPipeline lut, int Intent, int[] InputFormat, int[] OutputFormat, int[] dwFlags)
	{
		_cmsTransformCollection Plugin;
		
		// Allocate needed memory
	    _cmsTRANSFORM p = new _cmsTRANSFORM();
	    
	    // Store the proposed pipeline
	    p.Lut = lut;
	    
	    // Let's see if any plug-in want to do the transform by itself
	    for (Plugin = TransformCollection;
	    		Plugin != null;
	    		Plugin = Plugin.Next)
	    {
	    	cmsPipeline[] tlut = new cmsPipeline[]{p.Lut};
	    	if (Plugin.Factory.run(p.xform, p.UserData, p.FreeUserData, tlut, InputFormat, OutputFormat, dwFlags))
	    	{
	    		// Last plugin in the declaration order takes control. We just keep
	    		// the original parameters as a logging
	    		p.Lut             = tlut[0];
	    		p.InputFormat     = InputFormat[0];
	    		p.OutputFormat    = OutputFormat[0];
	    		p.dwOriginalFlags = dwFlags[0];
	    		p.ContextID       = ContextID;
	    		return p;
	    	}
	    }
	    
	    // Not suitable for the transform plug-in, let's check  the pipeline plug-in
	    if (p.Lut != null)
	    {
	    	cmsPipeline[] tlut = new cmsPipeline[]{p.Lut};
	    	cmsopt._cmsOptimizePipeline(tlut, Intent, InputFormat, OutputFormat, dwFlags);
	    	p.Lut = tlut[0];
	    }
	    
	    // Check whatever this is a true floating point transform
	    if (cmspack._cmsFormatterIsFloat(InputFormat[0]) && cmspack._cmsFormatterIsFloat(OutputFormat[0]))
	    {
	        // Get formatter function always return a valid union, but the contents of this union may be NULL.
	        p.FromInputFloat = cmspack._cmsGetFormatter(InputFormat[0],  lcms2_plugin.cmsFormatterInput, lcms2_plugin.CMS_PACK_FLAGS_FLOAT).getFloat();
	        p.ToOutputFloat  = cmspack._cmsGetFormatter(OutputFormat[0], lcms2_plugin.cmsFormatterOutput, lcms2_plugin.CMS_PACK_FLAGS_FLOAT).getFloat();
	        dwFlags[0] |= lcms2_internal.cmsFLAGS_CAN_CHANGE_FORMATTER;
	        
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
	    	if (InputFormat[0] == 0 && OutputFormat[0] == 0)
	    	{
	            p.FromInput = p.ToOutput = null;
	            dwFlags[0] |= lcms2_internal.cmsFLAGS_CAN_CHANGE_FORMATTER;
	        }
	        else
	        {
	            int BytesPerPixelInput;
	            
		        p.FromInput = cmspack._cmsGetFormatter(InputFormat[0],  lcms2_plugin.cmsFormatterInput, lcms2_plugin.CMS_PACK_FLAGS_16BITS).get16();
		        p.ToOutput  = cmspack._cmsGetFormatter(OutputFormat[0], lcms2_plugin.cmsFormatterOutput, lcms2_plugin.CMS_PACK_FLAGS_16BITS).get16();
		        
		        if (p.FromInput == null || p.ToOutput == null)
		        {
		            cmserr.cmsSignalError(ContextID, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSXFORM_UNSUPPORTED_RASTER_FORMAT), null);
		            return null;
		        }
		        
		        BytesPerPixelInput = lcms2.T_BYTES(p.InputFormat);
	            if (BytesPerPixelInput == 0 || BytesPerPixelInput >= 2)
	            {
	            	dwFlags[0] |= lcms2_internal.cmsFLAGS_CAN_CHANGE_FORMATTER;
	            }
	        }
	        
	        if ((dwFlags[0] & lcms2.cmsFLAGS_NULLTRANSFORM) != 0)
	        {
	            p.xform = NullXFORM;
	        }
	        else
	        {
	            if ((dwFlags[0] & lcms2.cmsFLAGS_NOCACHE) != 0)
	            {
	                if ((dwFlags[0] & lcms2.cmsFLAGS_GAMUTCHECK) != 0)
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
	                if ((dwFlags[0] & lcms2.cmsFLAGS_GAMUTCHECK) != 0)
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
	    
	    p.InputFormat     = InputFormat[0];
	    p.OutputFormat    = OutputFormat[0];
	    p.dwOriginalFlags = dwFlags[0];
	    
	    p.ContextID       = ContextID;
	    p.UserData        = null;
	    return p;
	}
	
	private static boolean GetXFormColorSpaces(int nProfiles, cmsHPROFILE[] hProfiles, int[] Input, int[] Output) 
	{    
	    int ColorSpaceIn, ColorSpaceOut;   
	    int PostColorSpace;   
	    int i;
	    
	    if (nProfiles <= 0)
	    {
	    	return false;
	    }
		if (hProfiles[0] == null)
		{
			return false;
		}
		
	    Input[0] = PostColorSpace = cmsio0.cmsGetColorSpace(hProfiles[0]);
	    
	    for (i = 0; i < nProfiles; i++)
	    {
	    	int cls;
	        cmsHPROFILE hProfile = hProfiles[i];
	        
	        boolean lIsInput = (PostColorSpace != lcms2.cmsSigXYZData) &&
	                       (PostColorSpace != lcms2.cmsSigLabData);
	        
			if (hProfile == null)
			{
				return false;
			}
			
			cls = cmsio0.cmsGetDeviceClass(hProfile);
			
			if (cls == lcms2.cmsSigNamedColorClass)
			{
	            ColorSpaceIn    = lcms2.cmsSig1colorData;
	            ColorSpaceOut   = (nProfiles > 1) ? cmsio0.cmsGetPCS(hProfile) : cmsio0.cmsGetColorSpace(hProfile);
	        }
	        else
	        {
		        if (lIsInput || (cls == lcms2.cmsSigLinkClass))
		        {
		            ColorSpaceIn    = cmsio0.cmsGetColorSpace(hProfile);
		            ColorSpaceOut   = cmsio0.cmsGetPCS(hProfile);
		        }
		        else
		        {
		            ColorSpaceIn    = cmsio0.cmsGetPCS(hProfile);
		            ColorSpaceOut   = cmsio0.cmsGetColorSpace(hProfile);
		        }
	        }
			
			if (i==0)
			{
				Input[0] = ColorSpaceIn;
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
	    
	    // If it is a fake transform
	    if ((dwFlags & lcms2.cmsFLAGS_NULLTRANSFORM) != 0)
	    {
	        return AllocEmptyTransform(ContextID, null, lcms2.INTENT_PERCEPTUAL, new int[]{InputFormat}, new int[]{OutputFormat}, new int[]{dwFlags});
	    }
	    
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
	    
	    // Check channel count
	    if ((cmspcs.cmsChannelsOf(EntryColorSpace[0]) != lcms2.cmsPipelineInputChannels(Lut)) ||
	    		(cmspcs.cmsChannelsOf(ExitColorSpace[0]) != lcms2.cmsPipelineOutputChannels(Lut)))
	    {
	    	cmserr.cmsSignalError(ContextID, lcms2.cmsERROR_NOT_SUITABLE, "Channel count doesn't match. Profile is corrupted", null);
	        return null;
	    }
	    
	    // All seems ok
	    int[] tdwFlags = new int[]{dwFlags};
	    xform = AllocEmptyTransform(ContextID, Lut, LastIntent, new int[]{InputFormat}, new int[]{OutputFormat}, tdwFlags);
	    if (xform == null)
	    {
	        return null;
	    }
	    dwFlags = tdwFlags[0];
	    
	    // Keep values
	    xform.EntryColorSpace = EntryColorSpace[0];
	    xform.ExitColorSpace  = ExitColorSpace[0];
	    xform.RenderingIntent = Intents[nProfiles-1];
	    
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
	    	Arrays.zero(xform.Cache.CacheIn); //Just as a precaution though it is probably a new array and thus it is zeroed on creation
	        
	        if (xform.GamutCheck != null)
	        {
	            TransformOnePixelWithGamutCheck(xform, xform.Cache.CacheIn, xform.Cache.CacheOut);
	        }
	        else
	        {
	            xform.Lut.Eval16Fn.run(xform.Cache.CacheIn, xform.Cache.CacheOut, xform.Lut.Data);  
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
	        AdaptationStates[i] = GlobalAdaptationState[0];
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
	    
	    Adaptation[0] = Adaptation[1] = Adaptation[2] = Adaptation[3] = GlobalAdaptationState[0];
	    
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
	
	// Grab the input/output formats
	public static int cmsGetTransformInputFormat(cmsHTRANSFORM hTransform)
	{
	    _cmsTRANSFORM xform = (_cmsTRANSFORM)hTransform;
	    
	    if (xform == null)
	    {
	    	return 0;
	    }
	    return xform.InputFormat;
	}
	
	public static int cmsGetTransformOutputFormat(cmsHTRANSFORM hTransform)
	{
	    _cmsTRANSFORM xform = (_cmsTRANSFORM)hTransform;
	    
	    if (xform == null)
	    {
	    	return 0;
	    }
	    return xform.OutputFormat;
	}
	
	// For backwards compatibility
	public static boolean cmsChangeBuffersFormat(cmsHTRANSFORM hTransform, int InputFormat, int OutputFormat)
	{
		_cmsTRANSFORM xform = (_cmsTRANSFORM)hTransform;
	    lcms2_plugin.cmsFormatter16 FromInput, ToOutput;
	    
	    // We only can afford to change formatters if previous transform is at least 16 bits
	    if ((xform.dwOriginalFlags & lcms2_internal.cmsFLAGS_CAN_CHANGE_FORMATTER) == 0)
	    {
	    	cmserr.cmsSignalError(xform.ContextID, lcms2.cmsERROR_NOT_SUITABLE, Utility.LCMS_Resources.getString(LCMSResource.CMSXFORM_CANT_CHANGE_BUFFER_FORMAT), null);
	        return false;
	    }
	    
	    FromInput = cmspack._cmsGetFormatter(InputFormat,  lcms2_plugin.cmsFormatterInput, lcms2_plugin.CMS_PACK_FLAGS_16BITS).get16();
	    ToOutput  = cmspack._cmsGetFormatter(OutputFormat, lcms2_plugin.cmsFormatterOutput, lcms2_plugin.CMS_PACK_FLAGS_16BITS).get16();
	    
	    if (FromInput == null || ToOutput == null)
	    {
	    	cmserr.cmsSignalError(xform.ContextID, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSXFORM_UNSUPPORTED_RASTER_FORMAT), null);
	        return false;
	    }
	    
	    xform.InputFormat  = InputFormat;
	    xform.OutputFormat = OutputFormat;
	    xform.FromInput = FromInput;
	    xform.ToOutput  = ToOutput;
	    return true;
	}
	
	private static final long ALARM_UID = 0x700168E434DDB320L;
	private static final long GAS_UID = 0x63CF45CCC98E05ACL;
	private static final long TRANSFORM_UID = 0x34570BD8864C20EDL;
	
	static
	{
		Object obj;
		if((obj = Utility.singletonStorageGet(ALARM_UID)) != null)
		{
			Alarm = (short[])obj;
			GlobalAdaptationState = (double[])Utility.singletonStorageGet(GAS_UID);
			TransformCollection = (_cmsTransformCollection)Utility.singletonStorageGet(TRANSFORM_UID);
		}
		else
		{
			Alarm = new short[lcms2.cmsMAXCHANNELS];
			Arrays.fill(Alarm, (short)0x7F00, 0, 3);
			GlobalAdaptationState = new double[]{1};
			TransformCollection = null;
			Utility.singletonStorageSet(ALARM_UID, Alarm);
			Utility.singletonStorageSet(GAS_UID, GlobalAdaptationState);
			Utility.singletonStorageSet(TRANSFORM_UID, TransformCollection);
		}
	}
}
