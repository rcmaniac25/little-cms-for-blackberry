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
import littlecms.internal.lcms2_plugin._cmsInterpFn16;
import littlecms.internal.lcms2_plugin._cmsInterpFnFloat;
import littlecms.internal.lcms2_plugin.cmsInterpFnFactory;
import littlecms.internal.lcms2_plugin.cmsInterpFunction;
import littlecms.internal.lcms2_plugin.cmsInterpParams;
import littlecms.internal.lcms2_plugin.cmsPluginBase;
import littlecms.internal.lcms2_plugin.cmsPluginInterpolation;

/**
 * This module incorporates several interpolation routines, for 1 to 8 channels on input and
 * up to 65535 channels on output. The user may change those by using the interpolation plug-in
 */
//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmsintrp
{
	// Main plug-in entry
	public static boolean _cmsRegisterInterpPlugin(cmsPluginBase Data)
	{
	    cmsPluginInterpolation Plugin = (cmsPluginInterpolation)Data;
	    
	    if (Data == null)
	    {
	        Interpolators = DefaultInterpolatorsFactory;
	        Utility.singletonStorageSet(INTERPOLATORS_UID, Interpolators);
	        return true;
	    }
	    
	    // Set replacement functions
	    Interpolators = Plugin.InterpolatorsFactory;
	    Utility.singletonStorageSet(INTERPOLATORS_UID, Interpolators);
	    return true;
	}
	
	// Set the interpolation method
	public static boolean _cmsSetInterpolationRoutine(cmsInterpParams p)
	{
	    // Invoke factory, possibly in the Plug-in
	    p.Interpolation = Interpolators.run(p.nInputs, p.nOutputs, p.dwFlags);
	    
	    // If unsupported by the plug-in, go for the LittleCMS default. 
	    // If happens only if an extern plug-in is being used
	    if (!p.Interpolation.hasValue())
	    {
	    	p.Interpolation = DefaultInterpolatorsFactory.run(p.nInputs, p.nOutputs, p.dwFlags);
	    }
	    
	    // Check for valid interpolator (we just check one member of the union)
	    if (!p.Interpolation.hasValue())
	    {
	    	return false;
	    }
	    return true;
	}
	
	// This function precalculates as many parameters as possible to speed up the interpolation.
	public static cmsInterpParams _cmsComputeInterpParamsEx(cmsContext ContextID, final int[] nSamples, int InputChan, int OutputChan, final Object Table, int dwFlags)
	{
	    cmsInterpParams p;
	    int i;
	    
	    // Check for maximum inputs
	    if (InputChan > lcms2_internal.MAX_INPUT_DIMENSIONS)
	    {
	    	cmserr.cmsSignalError(ContextID, lcms2.cmsERROR_RANGE, Utility.LCMS_Resources.getString(LCMSResource.CMSINTRP_TOO_MANY_INPUT_CHANNELS), 
	    			new Object[]{new Integer(InputChan), new Integer(lcms2_internal.MAX_INPUT_DIMENSIONS)});
	    	return null;
	    }
	    
	    // Creates an empty object
	    p = new cmsInterpParams();
	    
	    // Keep original parameters
	    p.dwFlags  = dwFlags;
	    p.nInputs  = InputChan;
	    p.nOutputs = OutputChan;
	    p.Table    = Table;
	    p.ContextID= ContextID;
	    
	    // Fill samples per input direction and domain (which is number of nodes minus one)
	    for (i=0; i < InputChan; i++)
	    {
	        p.nSamples[i] = nSamples[i];
	        p.Domain[i]   = nSamples[i] - 1;
	    }
	    
	    // Compute factors to apply to each component to index the grid array
	    p.opta[0] = p.nOutputs;
	    for (i=1; i < InputChan; i++)
	    {
	    	p.opta[i] = p.opta[i-1] * nSamples[InputChan-i];
	    	//XXX If these values overflow and become negative it will screw up the rest of the processing. Workaround: unknown
	    }
	    
	    if (!_cmsSetInterpolationRoutine(p))
	    {
	    	cmserr.cmsSignalError(ContextID, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSINTRP_UNSUP_INTERP), 
	    			new Object[]{new Integer(InputChan), new Integer(OutputChan)});
	        return null;
	    }
	    
	    // All seems ok
	    return p;
	}
	
	// This one is a wrapper on the anterior, but assuming all directions have same number of nodes
	public static cmsInterpParams _cmsComputeInterpParams(cmsContext ContextID, int nSamples, int InputChan, int OutputChan, final Object Table, int dwFlags)
	{
	    int i;
	    int[] Samples = new int[lcms2_internal.MAX_INPUT_DIMENSIONS];
	    
	    // Fill the auxiliar array
	    for (i=0; i < lcms2_internal.MAX_INPUT_DIMENSIONS; i++)
	    {
	    	Samples[i] = nSamples;
	    }
	    
	    // Call the extended function
	    return _cmsComputeInterpParamsEx(ContextID, Samples, InputChan, OutputChan, Table, dwFlags);
	}
	
	// Free all associated memory
	public static void _cmsFreeInterpParams(cmsInterpParams p)
	{
	    if (p != null)
	    {
	    	p.Domain = null;
	    	p.Interpolation = null;
	    	p.nSamples = null;
	    	p.opta = null;
	    	p.Table = null;
	    	//_cmsFree(p.ContextID, p);
	    }
	}
	
	// Inline fixed point interpolation 
	private static short LinearInterp(int a, int l, int h)
	{
	    int dif = (int)(h - l) * a + 0x8000;
	    dif = (dif >> 16) + l;
	    return (short)(dif);
	}
	
	//  Linear interpolation (Fixed-point optimized)
	private static final _cmsInterpFn16 LinLerp1D = new _cmsInterpFn16()
	{
		public void run(short[] Value, short[] Output, cmsInterpParams p)
		{
			short y1, y0;
		    int cell0, rest;
		    int val3;
		    if(p.Table instanceof VirtualPointer)
		    {
			    final VirtualPointer.TypeProcessor LutTable = ((VirtualPointer)p.Table).getProcessor();
			    final int pos = LutTable.getPosition();
			    
			    // if last value...
			    if ((Value[0] & 0xFFFF) == 0xffff)
			    {
			    	LutTable.setPosition(pos + (p.Domain[0] * 2));
			    	Output[0] = LutTable.readInt16();
			    	LutTable.setPosition(pos);
			        return;
			    }
			    
			    val3 = p.Domain[0] * (Value[0] & 0xFFFF);
			    val3 = lcms2_internal._cmsToFixedDomain(val3);	// To fixed 15.16
			    
			    cell0 = lcms2_internal.FIXED_TO_INT(val3);		// Cell is 16 MSB bits
			    rest  = lcms2_internal.FIXED_REST_TO_INT(val3);	// Rest is 16 LSB bits
			    
			    LutTable.setPosition(pos + (cell0 * 2));
			    y0 = LutTable.readInt16(true);
			    y1 = LutTable.readInt16();
			    LutTable.setPosition(pos);
		    }
		    else
		    {
		    	final short[] LutTable = (short[])p.Table;
			    
			    // if last value...
			    if ((Value[0] & 0xFFFF) == 0xffff)
			    {
			    	Output[0] = LutTable[p.Domain[0]];
			        return;
			    }
			    
			    val3 = p.Domain[0] * (Value[0] & 0xFFFF);
			    val3 = lcms2_internal._cmsToFixedDomain(val3);	// To fixed 15.16
			    
			    cell0 = lcms2_internal.FIXED_TO_INT(val3);		// Cell is 16 MSB bits
			    rest  = lcms2_internal.FIXED_REST_TO_INT(val3);	// Rest is 16 LSB bits
			    
			    y0 = LutTable[cell0];
			    y1 = LutTable[cell0+1];
		    }
		    Output[0] = LinearInterp(rest, y0 & 0xFFFF, y1 & 0xFFFF);
		}
	};
	
	// Floating-point version of 1D interpolation
	private static final _cmsInterpFnFloat LinLerp1Dfloat = new _cmsInterpFnFloat()
	{
		public void run(float[] Value, float[] Output, cmsInterpParams p)
		{
			float y1, y0;
			float val2, rest;
			int cell0, cell1;
			if(p.Table instanceof VirtualPointer)
			{
				final VirtualPointer.TypeProcessor LutTable = ((VirtualPointer)p.Table).getProcessor();
			    final int pos = LutTable.getPosition();
				
				// if last value...
				if (Value[0] == 1.0)
				{
					LutTable.setPosition(pos + (p.Domain[0] * 4));
			    	Output[0] = LutTable.readFloat();
			    	LutTable.setPosition(pos);
					return;
				}
				
				val2 = p.Domain[0] * Value[0];
				
				cell0 = (int)Math.floor(val2);
				cell1 = (int)Math.ceil(val2);
				
				// Rest is 16 LSB bits
				rest = val2 - cell0;
				
				LutTable.setPosition(pos + (cell0 * 4));
				y0 = LutTable.readFloat();
				LutTable.setPosition(pos + (cell1 * 4));
				y1 = LutTable.readFloat();
				LutTable.setPosition(pos);
			}
			else
			{
				final float[] LutTable = (float[])p.Table;
				
				// if last value...
				if (Value[0] == 1.0)
				{
			    	Output[0] = LutTable[p.Domain[0]];
					return;
				}
				
				val2 = p.Domain[0] * Value[0];
				
				cell0 = (int)Math.floor(val2);
				cell1 = (int)Math.ceil(val2);
				
				// Rest is 16 LSB bits
				rest = val2 - cell0;
				
				y0 = LutTable[cell0];
				y1 = LutTable[cell1];
			}
			
			Output[0] = y0 + (y1 - y0) * rest;
		}
	};
	
	// Eval gray LUT having only one input channel
	private static final _cmsInterpFn16 Eval1Input = new _cmsInterpFn16()
	{
		public void run(short[] Input, short[] Output, cmsInterpParams p16)
		{
			int fk;
			int k0, k1, rk, K0, K1;
			int v;
			int OutChan;
			
			v = (Input[0] & 0xFFFF) * p16.Domain[0];
			fk = lcms2_internal._cmsToFixedDomain(v);
			
			k0 = lcms2_internal.FIXED_TO_INT(fk);
			rk = lcms2_internal.FIXED_REST_TO_INT(fk);
			
			k1 = k0 + ((Input[0] & 0xFFFF) != 0xFFFF ? 1 : 0);
			
			K0 = p16.opta[0] * k0;
			K1 = p16.opta[0] * k1;
			
			if(p16.Table instanceof VirtualPointer)
			{
				final VirtualPointer.TypeProcessor LutTable = ((VirtualPointer)p16.Table).getProcessor();
			    final int pos = LutTable.getPosition();
				
				for (OutChan=0; OutChan < p16.nOutputs; OutChan++)
				{
					LutTable.setPosition(pos + ((K0+OutChan) * 2));
					short s1 = LutTable.readInt16();
					LutTable.setPosition(pos + ((K1+OutChan) * 2));
					Output[OutChan] = LinearInterp(rk, s1 & 0xFFFF, LutTable.readInt16() & 0xFFFF);
				}
				LutTable.setPosition(pos);
			}
			else
			{
				final short[] LutTable = (short[])p16.Table;
				
				for (OutChan=0; OutChan < p16.nOutputs; OutChan++)
				{
					Output[OutChan] = LinearInterp(rk, LutTable[K0+OutChan] & 0xFFFF, LutTable[K1+OutChan] & 0xFFFF);
				}
			}
		}
	};
	
	// Eval gray LUT having only one input channel
	private static final _cmsInterpFnFloat Eval1InputFloat = new _cmsInterpFnFloat()
	{
		public void run(float[] Value, float[] Output, cmsInterpParams p)
		{
			float y1, y0;
			float val2, rest;
		    int cell0, cell1;
		    int OutChan;
		    if(p.Table instanceof VirtualPointer)
		    {
			    final VirtualPointer.TypeProcessor LutTable = ((VirtualPointer)p.Table).getProcessor();
			    final int pos = LutTable.getPosition();
			    
			    // if last value...
			    if (Value[0] == 1.0)
			    {
			    	LutTable.setPosition(pos + (p.Domain[0] * 4));
			    	Output[0] = LutTable.readFloat();
			    	LutTable.setPosition(pos);
			    	return;
			    }
			    
			    val2 = p.Domain[0] * Value[0];
			    
			    cell0 = (int)Math.floor(val2);
			    cell1 = (int)Math.ceil(val2);
			    
			    // Rest is 16 LSB bits
			    rest = val2 - cell0;
			    
			    cell0 *= p.opta[0];
			    cell1 *= p.opta[0];
			    
			    for (OutChan=0; OutChan < p.nOutputs; OutChan++)
			    {
			    	LutTable.setPosition(pos + ((cell0 + OutChan) * 4));
			    	y0 = LutTable.readFloat();
			    	LutTable.setPosition(pos + ((cell1 + OutChan) * 4));
			    	y1 = LutTable.readFloat();
			    	
			    	Output[OutChan] = y0 + (y1 - y0) * rest;
			    }
			    LutTable.setPosition(pos);
		    }
		    else
		    {
		    	final short[] LutTable = (short[])p.Table;
			    
			    // if last value...
			    if (Value[0] == 1.0)
			    {
			    	Output[0] = LutTable[p.Domain[0]];
			    	return;
			    }
			    
			    val2 = p.Domain[0] * Value[0];
			    
			    cell0 = (int)Math.floor(val2);
			    cell1 = (int)Math.ceil(val2);
			    
			    // Rest is 16 LSB bits
			    rest = val2 - cell0;
			    
			    cell0 *= p.opta[0];
			    cell1 *= p.opta[0];
			    
			    for (OutChan=0; OutChan < p.nOutputs; OutChan++)
			    {
			    	y0 = LutTable[cell0 + OutChan];
			    	y1 = LutTable[cell1 + OutChan];
			    	
			    	Output[OutChan] = y0 + (y1 - y0) * rest;
			    }
		    }
		}
	};
	
	//VirtualPointer
	private static float DENSF(int i,int j,VirtualPointer.TypeProcessor LutTable,int pos,int OutChan)
	{
		return DENSF(i, j, 0, LutTable, pos, OutChan);
	}
	
	private static float DENSF(int i,int j,int k,VirtualPointer.TypeProcessor LutTable,int pos,int OutChan)
	{
		LutTable.setPosition(pos + (((i)+(j)+(k)+OutChan) * 4));
		return LutTable.readFloat();
	}
	
	private static int DENSS(int i,int j,VirtualPointer.TypeProcessor LutTable,int pos,int OutChan)
	{
		return DENSS(i, j, 0, LutTable, pos, OutChan);
	}
	
	private static int DENSS(int i,int j,int k,VirtualPointer.TypeProcessor LutTable,int pos,int OutChan)
	{
		LutTable.setPosition(pos + (((i)+(j)+(k)+OutChan) * 2));
		return LutTable.readInt16() & 0xFFFF;
	}
	
	//Short
	private static int DENS(int i,int j,short[] LutTable,int OutChan)
	{
		return DENS(i,j,0,LutTable,0,OutChan);
	}
	
	private static int DENS(int i,int j,int k,short[] LutTable,int OutChan)
	{
		return DENS(i,j,k,LutTable,0,OutChan);
	}
	
	private static int DENS(int i,int j,int k,short[] LutTable,int tableOffset,int OutChan)
	{
		return (LutTable[(i)+(j)+(k)+OutChan+tableOffset]) & 0xFFFF; 
	}
	//Float
	private static float DENS(int i,int j,float[] LutTable,int OutChan)
	{
		return DENS(i, j, 0, LutTable, OutChan);
	}
	
	private static float DENS(int i,int j,int k,float[] LutTable,int OutChan)
	{
		return (LutTable[(i)+(j)+(k)+OutChan]); 
	}
	
	// Bilinear interpolation (16 bits) - cmsFloat32Number version
	private static final _cmsInterpFnFloat BilinearInterpFloat = new _cmsInterpFnFloat()
	{
		private float LERP(float a,float l,float h)
		{
			return ((l)+(((h)-(l))*(a)));
		}
		
		public void run(float[] Input, float[] Output, cmsInterpParams p)
		{
		    float      px, py;
		    int        x0, y0,
		               X0, Y0, X1, Y1;
		    int        TotalOut, OutChan;
		    float      fx, fy,
		        d00, d01, d10, d11,
		        dx0, dx1,
		        dxy;
		    
		    TotalOut   = p.nOutputs;
		    px = Input[0] * p.Domain[0];
		    py = Input[1] * p.Domain[1];
		    
		    x0 = (int)lcms2_internal._cmsQuickFloor(px); fx = px - (float) x0;
		    y0 = (int)lcms2_internal._cmsQuickFloor(py); fy = py - (float) y0;
		    
		    X0 = p.opta[1] * x0;
		    X1 = X0 + (Input[0] >= 1.0 ? 0 : p.opta[1]);
		    
		    Y0 = p.opta[0] * y0;
		    Y1 = Y0 + (Input[1] >= 1.0 ? 0 : p.opta[0]);
		    
		    if(p.Table instanceof VirtualPointer)
		    {
		    	final VirtualPointer.TypeProcessor LutTable = ((VirtualPointer)p.Table).getProcessor();
			    final int pos = LutTable.getPosition();
			    
			    for (OutChan = 0; OutChan < TotalOut; OutChan++)
			    {
			        d00 = DENSF(X0, Y0, LutTable, pos, OutChan);
			        d01 = DENSF(X0, Y1, LutTable, pos, OutChan);
			        d10 = DENSF(X1, Y0, LutTable, pos, OutChan);
			        d11 = DENSF(X1, Y1, LutTable, pos, OutChan);
			        
			        dx0 = LERP(fx, d00, d10);
			        dx1 = LERP(fx, d01, d11);
			        
			        dxy = LERP(fy, dx0, dx1);
			        
			        Output[OutChan] = dxy;
			    }
			    
			    LutTable.setPosition(pos);
		    }
		    else
		    {
			    final float[] LutTable = (float[])p.Table;
			    
			    for (OutChan = 0; OutChan < TotalOut; OutChan++)
			    {
			        d00 = DENS(X0, Y0, LutTable, OutChan);
			        d01 = DENS(X0, Y1, LutTable, OutChan);
			        d10 = DENS(X1, Y0, LutTable, OutChan);
			        d11 = DENS(X1, Y1, LutTable, OutChan);
			        
			        dx0 = LERP(fx, d00, d10);
			        dx1 = LERP(fx, d01, d11);
			        
			        dxy = LERP(fy, dx0, dx1);
			        
			        Output[OutChan] = dxy;
			    }
		    }
		}
	};
	
	// Bilinear interpolation (16 bits) - optimized version
	private static final _cmsInterpFn16 BilinearInterp16 = new _cmsInterpFn16()
	{
		private int LERP(int a,int l,int h)
		{
			return (l + lcms2_internal.ROUND_FIXED_TO_INT(((h-l)*a))) & 0xFFFF;
		}
		
		public void run(short[] Input, short[] Output, cmsInterpParams p)
		{
			int        OutChan, TotalOut;
			int        fx, fy;
			int        rx, ry;
			int        x0, y0;
			int        X0, X1, Y0, Y1;
			int        d00, d01, d10, d11,
		                      dx0, dx1,
		                      dxy;
			
		    TotalOut   = p.nOutputs;
		    
		    fx = lcms2_internal._cmsToFixedDomain((Input[0] & 0xFFFF) * p.Domain[0]);
		    x0  = lcms2_internal.FIXED_TO_INT(fx);
		    rx  = lcms2_internal.FIXED_REST_TO_INT(fx);    // Rest in 0..1.0 domain
		    
		    
		    fy = lcms2_internal._cmsToFixedDomain((Input[1] & 0xFFFF) * p.Domain[1]);
		    y0  = lcms2_internal.FIXED_TO_INT(fy);
		    ry  = lcms2_internal.FIXED_REST_TO_INT(fy);
		    
		    
		    X0 = p.opta[1] * x0;
		    X1 = X0 + (Input[0] == (short)0xFFFF ? 0 : p.opta[1]);
		    
		    Y0 = p.opta[0] * y0;
		    Y1 = Y0 + (Input[1] == (short)0xFFFF ? 0 : p.opta[0]);
		    
		    if(p.Table instanceof VirtualPointer)
		    {
		    	final VirtualPointer.TypeProcessor LutTable = ((VirtualPointer)p.Table).getProcessor();
			    final int pos = LutTable.getPosition();
			    
			    for (OutChan = 0; OutChan < TotalOut; OutChan++)
			    {
			    	d00 = DENSS(X0, Y0, LutTable, pos, OutChan);
			        d01 = DENSS(X0, Y1, LutTable, pos, OutChan);
			        d10 = DENSS(X1, Y0, LutTable, pos, OutChan);
			        d11 = DENSS(X1, Y1, LutTable, pos, OutChan);
			        
			        dx0 = LERP(rx, d00, d10);
			        dx1 = LERP(rx, d01, d11);
			        
			        dxy = LERP(ry, dx0, dx1);
			        
			        Output[OutChan] = (short)dxy;
			    }
			    
			    LutTable.setPosition(pos);
		    }
		    else
		    {
			    final short[] LutTable = (short[])p.Table;
			    
			    for (OutChan = 0; OutChan < TotalOut; OutChan++)
			    {
			    	d00 = DENS(X0, Y0, LutTable, OutChan);
			        d01 = DENS(X0, Y1, LutTable, OutChan);
			        d10 = DENS(X1, Y0, LutTable, OutChan);
			        d11 = DENS(X1, Y1, LutTable, OutChan);
			        
			        dx0 = LERP(rx, d00, d10);
			        dx1 = LERP(rx, d01, d11);
			        
			        dxy = LERP(ry, dx0, dx1);
			        
			        Output[OutChan] = (short)dxy;
			    }
		    }
		}
	};
	
	// Trilinear interpolation (16 bits) - float version
	private static final _cmsInterpFnFloat TrilinearInterpFloat = new _cmsInterpFnFloat()
	{
		private float LERP(float a,float l,float h)
		{
			return ((l)+(((h)-(l))*(a)));
		}
		
		public void run(float[] Input, float[] Output, cmsInterpParams p)
		{
		    float      px, py, pz;
		    int        x0, y0, z0,
		               X0, Y0, Z0, X1, Y1, Z1;
		    int        TotalOut, OutChan;
		    float      fx, fy, fz,
		        d000, d001, d010, d011,
		        d100, d101, d110, d111,
		        dx00, dx01, dx10, dx11,
		        dxy0, dxy1, dxyz;
		    
		    TotalOut = p.nOutputs;
		    
		    // We need some clipping here
		    px = Input[0];
		    py = Input[1];
		    pz = Input[2];

		    if (px < 0)
		    {
		    	px = 0; 
		    }
		    if (px > 1)
		    {
		    	px = 1;
		    }
		    if (py < 0)
		    {
		    	py = 0; 
		    }
		    if (py > 1)
		    {
		    	py = 1;
		    }
		    if (pz < 0)
		    {
		    	pz = 0; 
		    }
		    if (pz > 1)
		    {
		    	pz = 1;
		    }

		    px *= p.Domain[0];
		    py *= p.Domain[1];
		    pz *= p.Domain[2];
		    
		    x0 = (int)lcms2_internal._cmsQuickFloor(px); fx = px - x0;
		    y0 = (int)lcms2_internal._cmsQuickFloor(py); fy = py - y0;
		    z0 = (int)lcms2_internal._cmsQuickFloor(pz); fz = pz - z0;
		    
		    X0 = p.opta[2] * x0;
		    X1 = X0 + (Input[0] >= 1.0 ? 0 : p.opta[2]);
		    
		    Y0 = p.opta[1] * y0;
		    Y1 = Y0 + (Input[1] >= 1.0 ? 0 : p.opta[1]);
		    
		    Z0 = p.opta[0] * z0;
		    Z1 = Z0 + (Input[2] >= 1.0 ? 0 : p.opta[0]);
		    
		    if(p.Table instanceof VirtualPointer)
		    {
			    final VirtualPointer.TypeProcessor LutTable = ((VirtualPointer)p.Table).getProcessor();
			    final int pos = LutTable.getPosition();
			    
			    for (OutChan = 0; OutChan < TotalOut; OutChan++)
			    {
			        d000 = DENSF(X0, Y0, Z0, LutTable, pos, OutChan);
			        d001 = DENSF(X0, Y0, Z1, LutTable, pos, OutChan);
			        d010 = DENSF(X0, Y1, Z0, LutTable, pos, OutChan);
			        d011 = DENSF(X0, Y1, Z1, LutTable, pos, OutChan);
			        
			        d100 = DENSF(X1, Y0, Z0, LutTable, pos, OutChan);
			        d101 = DENSF(X1, Y0, Z1, LutTable, pos, OutChan);
			        d110 = DENSF(X1, Y1, Z0, LutTable, pos, OutChan);
			        d111 = DENSF(X1, Y1, Z1, LutTable, pos, OutChan);
			        
			        dx00 = LERP(fx, d000, d100);
			        dx01 = LERP(fx, d001, d101);
			        dx10 = LERP(fx, d010, d110);
			        dx11 = LERP(fx, d011, d111);
			        
			        dxy0 = LERP(fy, dx00, dx10);
			        dxy1 = LERP(fy, dx01, dx11);
			        
			        dxyz = LERP(fz, dxy0, dxy1);
			        
			        Output[OutChan] = dxyz;
			    }
			    LutTable.setPosition(pos);
		    }
		    else
		    {
		    	final float[] LutTable = (float[])p.Table;
			    
			    for (OutChan = 0; OutChan < TotalOut; OutChan++)
			    {
			        d000 = DENS(X0, Y0, Z0, LutTable, OutChan);
			        d001 = DENS(X0, Y0, Z1, LutTable, OutChan);
			        d010 = DENS(X0, Y1, Z0, LutTable, OutChan);
			        d011 = DENS(X0, Y1, Z1, LutTable, OutChan);
			        
			        d100 = DENS(X1, Y0, Z0, LutTable, OutChan);
			        d101 = DENS(X1, Y0, Z1, LutTable, OutChan);
			        d110 = DENS(X1, Y1, Z0, LutTable, OutChan);
			        d111 = DENS(X1, Y1, Z1, LutTable, OutChan);
			        
			        dx00 = LERP(fx, d000, d100);
			        dx01 = LERP(fx, d001, d101);
			        dx10 = LERP(fx, d010, d110);
			        dx11 = LERP(fx, d011, d111);
			        
			        dxy0 = LERP(fy, dx00, dx10);
			        dxy1 = LERP(fy, dx01, dx11);
			        
			        dxyz = LERP(fz, dxy0, dxy1);
			        
			        Output[OutChan] = dxyz;
			    }
		    }
		}
	};
	
	// Trilinear interpolation (16 bits) - optimized version
	private static final _cmsInterpFn16 TrilinearInterp16 = new _cmsInterpFn16()
	{
		private int LERP(int a,int l,int h)
		{
			return (l + lcms2_internal.ROUND_FIXED_TO_INT(((h-l)*a))) & 0xFFFF;
		}
		
		public void run(short[] Input, short[] Output, cmsInterpParams p)
		{
			int			OutChan, TotalOut;
			int			fx, fy, fz;
			int			rx, ry, rz;
			int			x0, y0, z0;
			int			X0, X1, Y0, Y1, Z0, Z1;
			int			d000, d001, d010, d011,
	                      d100, d101, d110, d111,
	                      dx00, dx01, dx10, dx11,
	                      dxy0, dxy1, dxyz;
			
			TotalOut   = p.nOutputs;
			
			fx = lcms2_internal._cmsToFixedDomain((Input[0] & 0xFFFF) * p.Domain[0]);
			x0 = lcms2_internal.FIXED_TO_INT(fx);
			rx = lcms2_internal.FIXED_REST_TO_INT(fx); // Rest in 0..1.0 domain
			
			fy = lcms2_internal._cmsToFixedDomain((Input[1] & 0xFFFF) * p.Domain[1]);
			y0 = lcms2_internal.FIXED_TO_INT(fy);
			ry = lcms2_internal.FIXED_REST_TO_INT(fy);
			
			fz = lcms2_internal._cmsToFixedDomain((Input[2] & 0xFFFF) * p.Domain[2]);
			z0 = lcms2_internal.FIXED_TO_INT(fz);
			rz = lcms2_internal.FIXED_REST_TO_INT(fz);
			
			X0 = p.opta[2] * x0;
			X1 = X0 + (Input[0] == (short)0xFFFF ? 0 : p.opta[2]);
			
			Y0 = p.opta[1] * y0;
			Y1 = Y0 + (Input[1] == (short)0xFFFF ? 0 : p.opta[1]);
			
			Z0 = p.opta[0] * z0;
			Z1 = Z0 + (Input[2] == (short)0xFFFF ? 0 : p.opta[0]);
			
			if(p.Table instanceof VirtualPointer)
			{
				final VirtualPointer.TypeProcessor LutTable = ((VirtualPointer)p.Table).getProcessor();
			    final int pos = LutTable.getPosition();
				
				for (OutChan = 0; OutChan < TotalOut; OutChan++)
				{
			        d000 = DENSS(X0, Y0, Z0, LutTable, pos, OutChan);
			        d001 = DENSS(X0, Y0, Z1, LutTable, pos, OutChan);
			        d010 = DENSS(X0, Y1, Z0, LutTable, pos, OutChan);
			        d011 = DENSS(X0, Y1, Z1, LutTable, pos, OutChan);
			        
			        d100 = DENSS(X1, Y0, Z0, LutTable, pos, OutChan);
			        d101 = DENSS(X1, Y0, Z1, LutTable, pos, OutChan);
			        d110 = DENSS(X1, Y1, Z0, LutTable, pos, OutChan);
			        d111 = DENSS(X1, Y1, Z1, LutTable, pos, OutChan);
			        
			        dx00 = LERP(rx, d000, d100);
			        dx01 = LERP(rx, d001, d101);
			        dx10 = LERP(rx, d010, d110);
			        dx11 = LERP(rx, d011, d111);
			        
			        dxy0 = LERP(ry, dx00, dx10);
			        dxy1 = LERP(ry, dx01, dx11);
			        
			        dxyz = LERP(rz, dxy0, dxy1);
			        
			        Output[OutChan] = (short)dxyz;
				}
				LutTable.setPosition(pos);
			}
			else
			{
				final short[] LutTable = (short[])p.Table;
				
				for (OutChan = 0; OutChan < TotalOut; OutChan++)
				{
			        d000 = DENS(X0, Y0, Z0, LutTable, OutChan);
			        d001 = DENS(X0, Y0, Z1, LutTable, OutChan);
			        d010 = DENS(X0, Y1, Z0, LutTable, OutChan);
			        d011 = DENS(X0, Y1, Z1, LutTable, OutChan);
			        
			        d100 = DENS(X1, Y0, Z0, LutTable, OutChan);
			        d101 = DENS(X1, Y0, Z1, LutTable, OutChan);
			        d110 = DENS(X1, Y1, Z0, LutTable, OutChan);
			        d111 = DENS(X1, Y1, Z1, LutTable, OutChan);
			        
			        dx00 = LERP(rx, d000, d100);
			        dx01 = LERP(rx, d001, d101);
			        dx10 = LERP(rx, d010, d110);
			        dx11 = LERP(rx, d011, d111);
			        
			        dxy0 = LERP(ry, dx00, dx10);
			        dxy1 = LERP(ry, dx01, dx11);
			        
			        dxyz = LERP(rz, dxy0, dxy1);
			        
			        Output[OutChan] = (short)dxyz;
				}
			}
		}
	};
	
	// Tetrahedral interpolation, using Sakamoto algorithm.
	private static final _cmsInterpFnFloat TetrahedralInterpFloat = new _cmsInterpFnFloat()
	{
		public void run(float[] Input, float[] Output, cmsInterpParams p)
		{
		    float      px, py, pz;
		    int        x0, y0, z0,
		               X0, Y0, Z0, X1, Y1, Z1;
		    float      rx, ry, rz;
		    float      c0, c1=0, c2=0, c3=0;
		    int        OutChan, TotalOut;
		    
		    TotalOut   = p.nOutputs;
		    
		    // We need some clipping here
		    px = Input[0];
		    py = Input[1];
		    pz = Input[2];

		    if (px < 0)
		    {
		    	px = 0; 
		    }
		    if (px > 1)
		    {
		    	px = 1;
		    }
		    if (py < 0)
		    {
		    	py = 0; 
		    }
		    if (py > 1)
		    {
		    	py = 1;
		    }
		    if (pz < 0)
		    {
		    	pz = 0; 
		    }
		    if (pz > 1)
		    {
		    	pz = 1;
		    }

		    px *= p.Domain[0];
		    py *= p.Domain[1];
		    pz *= p.Domain[2];
		    
		    x0 = (int)lcms2_internal._cmsQuickFloor(px); rx = (px - x0);
		    y0 = (int)lcms2_internal._cmsQuickFloor(py); ry = (py - y0);
		    z0 = (int)lcms2_internal._cmsQuickFloor(pz); rz = (pz - z0);
		    
		    X0 = p.opta[2] * x0;
		    X1 = X0 + (Input[0] >= 1.0 ? 0 : p.opta[2]);
		    
		    Y0 = p.opta[1] * y0;
		    Y1 = Y0 + (Input[1] >= 1.0 ? 0 : p.opta[1]);
		    
		    Z0 = p.opta[0] * z0;
		    Z1 = Z0 + (Input[2] >= 1.0 ? 0 : p.opta[0]);
		    
		    if(p.Table instanceof VirtualPointer)
		    {
			    final VirtualPointer.TypeProcessor LutTable = ((VirtualPointer)p.Table).getProcessor();
			    final int pos = LutTable.getPosition();
			    
			    for (OutChan=0; OutChan < TotalOut; OutChan++)
			    {
			    	// These are the 6 Tetrahedral
			    	
			        c0 = DENSF(X0, Y0, Z0, LutTable, pos, OutChan);
			        
			        if (rx >= ry && ry >= rz)
			        {
			            c1 = DENSF(X1, Y0, Z0, LutTable, pos, OutChan) - c0;
			            c2 = DENSF(X1, Y1, Z0, LutTable, pos, OutChan) - DENSF(X1, Y0, Z0, LutTable, pos, OutChan);
			            c3 = DENSF(X1, Y1, Z1, LutTable, pos, OutChan) - DENSF(X1, Y1, Z0, LutTable, pos, OutChan);
			        }
			        else
			        {
			            if (rx >= rz && rz >= ry)
			            {            
			                c1 = DENSF(X1, Y0, Z0, LutTable, pos, OutChan) - c0;
			                c2 = DENSF(X1, Y1, Z1, LutTable, pos, OutChan) - DENSF(X1, Y0, Z1, LutTable, pos, OutChan);
			                c3 = DENSF(X1, Y0, Z1, LutTable, pos, OutChan) - DENSF(X1, Y0, Z0, LutTable, pos, OutChan);
			            }
			            else
			            {
			                if (rz >= rx && rx >= ry)
			                {
			                    c1 = DENSF(X1, Y0, Z1, LutTable, pos, OutChan) - DENSF(X0, Y0, Z1, LutTable, pos, OutChan);
			                    c2 = DENSF(X1, Y1, Z1, LutTable, pos, OutChan) - DENSF(X1, Y0, Z1, LutTable, pos, OutChan);
			                    c3 = DENSF(X0, Y0, Z1, LutTable, pos, OutChan) - c0;
			                }
			                else
			                {
			                    if (ry >= rx && rx >= rz)
			                    {
			                        c1 = DENSF(X1, Y1, Z0, LutTable, pos, OutChan) - DENSF(X0, Y1, Z0, LutTable, pos, OutChan);
			                        c2 = DENSF(X0, Y1, Z0, LutTable, pos, OutChan) - c0;
			                        c3 = DENSF(X1, Y1, Z1, LutTable, pos, OutChan) - DENSF(X1, Y1, Z0, LutTable, pos, OutChan);
			                    }
			                    else
			                    {
			                        if (ry >= rz && rz >= rx)
			                        {
			                            c1 = DENSF(X1, Y1, Z1, LutTable, pos, OutChan) - DENSF(X0, Y1, Z1, LutTable, pos, OutChan);
			                            c2 = DENSF(X0, Y1, Z0, LutTable, pos, OutChan) - c0;
			                            c3 = DENSF(X0, Y1, Z1, LutTable, pos, OutChan) - DENSF(X0, Y1, Z0, LutTable, pos, OutChan);
			                        }
			                        else
			                        {
			                            if (rz >= ry && ry >= rx)
			                            {             
			                                c1 = DENSF(X1, Y1, Z1, LutTable, pos, OutChan) - DENSF(X0, Y1, Z1, LutTable, pos, OutChan);
			                                c2 = DENSF(X0, Y1, Z1, LutTable, pos, OutChan) - DENSF(X0, Y0, Z1, LutTable, pos, OutChan);
			                                c3 = DENSF(X0, Y0, Z1, LutTable, pos, OutChan) - c0;
			                            }
			                            else
			                            {
			                                c1 = c2 = c3 = 0;                               
			                            }
			                        }
			                    }
			                }
			            }
			        }
			        
			        Output[OutChan] = c0 + c1 * rx + c2 * ry + c3 * rz;
			    }
			    LutTable.setPosition(pos);
		    }
		    else
		    {
		    	final float[] LutTable = (float[])p.Table;
			    
			    for (OutChan=0; OutChan < TotalOut; OutChan++)
			    {
			    	// These are the 6 Tetrahedral
			    	
			        c0 = DENS(X0, Y0, Z0, LutTable, OutChan);
			        
			        if (rx >= ry && ry >= rz)
			        {
			            c1 = DENS(X1, Y0, Z0, LutTable, OutChan) - c0;
			            c2 = DENS(X1, Y1, Z0, LutTable, OutChan) - DENS(X1, Y0, Z0, LutTable, OutChan);
			            c3 = DENS(X1, Y1, Z1, LutTable, OutChan) - DENS(X1, Y1, Z0, LutTable, OutChan);
			        }
			        else
			        {
			            if (rx >= rz && rz >= ry)
			            {            
			                c1 = DENS(X1, Y0, Z0, LutTable, OutChan) - c0;
			                c2 = DENS(X1, Y1, Z1, LutTable, OutChan) - DENS(X1, Y0, Z1, LutTable, OutChan);
			                c3 = DENS(X1, Y0, Z1, LutTable, OutChan) - DENS(X1, Y0, Z0, LutTable, OutChan);
			            }
			            else
			            {
			                if (rz >= rx && rx >= ry)
			                {
			                    c1 = DENS(X1, Y0, Z1, LutTable, OutChan) - DENS(X0, Y0, Z1, LutTable, OutChan);
			                    c2 = DENS(X1, Y1, Z1, LutTable, OutChan) - DENS(X1, Y0, Z1, LutTable, OutChan);
			                    c3 = DENS(X0, Y0, Z1, LutTable, OutChan) - c0;
			                }
			                else
			                {
			                    if (ry >= rx && rx >= rz)
			                    {
			                        c1 = DENS(X1, Y1, Z0, LutTable, OutChan) - DENS(X0, Y1, Z0, LutTable, OutChan);
			                        c2 = DENS(X0, Y1, Z0, LutTable, OutChan) - c0;
			                        c3 = DENS(X1, Y1, Z1, LutTable, OutChan) - DENS(X1, Y1, Z0, LutTable, OutChan);
			                    }
			                    else
			                    {
			                        if (ry >= rz && rz >= rx)
			                        {
			                            c1 = DENS(X1, Y1, Z1, LutTable, OutChan) - DENS(X0, Y1, Z1, LutTable, OutChan);
			                            c2 = DENS(X0, Y1, Z0, LutTable, OutChan) - c0;
			                            c3 = DENS(X0, Y1, Z1, LutTable, OutChan) - DENS(X0, Y1, Z0, LutTable, OutChan);
			                        }
			                        else
			                        {
			                            if (rz >= ry && ry >= rx)
			                            {             
			                                c1 = DENS(X1, Y1, Z1, LutTable, OutChan) - DENS(X0, Y1, Z1, LutTable, OutChan);
			                                c2 = DENS(X0, Y1, Z1, LutTable, OutChan) - DENS(X0, Y0, Z1, LutTable, OutChan);
			                                c3 = DENS(X0, Y0, Z1, LutTable, OutChan) - c0;
			                            }
			                            else
			                            {
			                                c1 = c2 = c3 = 0;                               
			                            }
			                        }
			                    }
			                }
			            }
			        }
			        
			        Output[OutChan] = c0 + c1 * rx + c2 * ry + c3 * rz;
			    }
		    }
		}
	};
	
	private final static class TetrahedralInterp16Impl implements _cmsInterpFn16, lcms2_internal._cmsOPTeval16Fn
	{
		public void run(short[] In, short[] Out, Object Data)
		{
			run(In, Out, (cmsInterpParams)Data);
		}
		
		public void run(short[] Input, short[] Output, cmsInterpParams p)
		{
			int fx, fy, fz;
		    int rx, ry, rz;
		    int x0, y0, z0;
		    int c0, c1, c2, c3, Rest;       
		    int OutChan;
		    int X0, X1, Y0, Y1, Z0, Z1;
		    int TotalOut = p.nOutputs;
		    
		    fx  = lcms2_internal._cmsToFixedDomain((Input[0] & 0xFFFF) * p.Domain[0]);
		    fy  = lcms2_internal._cmsToFixedDomain((Input[1] & 0xFFFF) * p.Domain[1]);
		    fz  = lcms2_internal._cmsToFixedDomain((Input[2] & 0xFFFF) * p.Domain[2]);
		    
		    x0  = lcms2_internal.FIXED_TO_INT(fx);
		    y0  = lcms2_internal.FIXED_TO_INT(fy); 
		    z0  = lcms2_internal.FIXED_TO_INT(fz);
		    
		    rx  = lcms2_internal.FIXED_REST_TO_INT(fx);
		    ry  = lcms2_internal.FIXED_REST_TO_INT(fy);
		    rz  = lcms2_internal.FIXED_REST_TO_INT(fz);
		    
		    X0 = p.opta[2] * x0;
		    X1 = X0 + (Input[0] == (short)0xFFFF ? 0 : p.opta[2]);
		    
		    Y0 = p.opta[1] * y0;
		    Y1 = Y0 + (Input[1] == (short)0xFFFF ? 0 : p.opta[1]);
		    
		    Z0 = p.opta[0] * z0;
		    Z1 = Z0 + (Input[2] == (short)0xFFFF ? 0 : p.opta[0]);
		    
		    if(p.Table instanceof VirtualPointer)
		    {
			    final VirtualPointer.TypeProcessor LutTable = ((VirtualPointer)p.Table).getProcessor();
			    final int pos = LutTable.getPosition();
			    
			    // These are the 6 Tetrahedral
			    for (OutChan=0; OutChan < TotalOut; OutChan++)
			    {
			        c0 = DENSS(X0, Y0, Z0, LutTable, pos, OutChan);
			        
			        if (rx >= ry && ry >= rz)
			        {
			            c1 = DENSS(X1, Y0, Z0, LutTable, pos, OutChan) - c0;
			            c2 = DENSS(X1, Y1, Z0, LutTable, pos, OutChan) - DENSS(X1, Y0, Z0, LutTable, pos, OutChan);
			            c3 = DENSS(X1, Y1, Z1, LutTable, pos, OutChan) - DENSS(X1, Y1, Z0, LutTable, pos, OutChan);
			        }
			        else
			        {
			            if (rx >= rz && rz >= ry)
			            {            
			                c1 = DENSS(X1, Y0, Z0, LutTable, pos, OutChan) - c0;
			                c2 = DENSS(X1, Y1, Z1, LutTable, pos, OutChan) - DENSS(X1, Y0, Z1, LutTable, pos, OutChan);
			                c3 = DENSS(X1, Y0, Z1, LutTable, pos, OutChan) - DENSS(X1, Y0, Z0, LutTable, pos, OutChan);
			            }
			            else
			            {
			                if (rz >= rx && rx >= ry)
			                {
			                    c1 = DENSS(X1, Y0, Z1, LutTable, pos, OutChan) - DENSS(X0, Y0, Z1, LutTable, pos, OutChan);
			                    c2 = DENSS(X1, Y1, Z1, LutTable, pos, OutChan) - DENSS(X1, Y0, Z1, LutTable, pos, OutChan);
			                    c3 = DENSS(X0, Y0, Z1, LutTable, pos, OutChan) - c0;                            
			                }
			                else
			                {
			                    if (ry >= rx && rx >= rz)
			                    {
			                        c1 = DENSS(X1, Y1, Z0, LutTable, pos, OutChan) - DENSS(X0, Y1, Z0, LutTable, pos, OutChan);
			                        c2 = DENSS(X0, Y1, Z0, LutTable, pos, OutChan) - c0;
			                        c3 = DENSS(X1, Y1, Z1, LutTable, pos, OutChan) - DENSS(X1, Y1, Z0, LutTable, pos, OutChan);
			                    }
			                    else
			                    {
			                        if (ry >= rz && rz >= rx)
			                        {
			                            c1 = DENSS(X1, Y1, Z1, LutTable, pos, OutChan) - DENSS(X0, Y1, Z1, LutTable, pos, OutChan);
			                            c2 = DENSS(X0, Y1, Z0, LutTable, pos, OutChan) - c0;
			                            c3 = DENSS(X0, Y1, Z1, LutTable, pos, OutChan) - DENSS(X0, Y1, Z0, LutTable, pos, OutChan);
			                        }
			                        else
			                        {
			                            if (rz >= ry && ry >= rx)
			                            {             
			                                c1 = DENSS(X1, Y1, Z1, LutTable, pos, OutChan) - DENSS(X0, Y1, Z1, LutTable, pos, OutChan);
			                                c2 = DENSS(X0, Y1, Z1, LutTable, pos, OutChan) - DENSS(X0, Y0, Z1, LutTable, pos, OutChan);
			                                c3 = DENSS(X0, Y0, Z1, LutTable, pos, OutChan) - c0;
			                            }
			                            else
			                            {
			                                c1 = c2 = c3 = 0;                               
			                            }
			                        }
			                    }
			                }
			            }
			        }
			        
			        Rest = c1 * rx + c2 * ry + c3 * rz;
			        
			        Output[OutChan] = (short)(c0 + lcms2_internal.ROUND_FIXED_TO_INT(lcms2_internal._cmsToFixedDomain(Rest)));
			    }
			    LutTable.setPosition(pos);
		    }
		    else
		    {
		    	final short[] LutTable = (short[])p.Table;
			    
			    // These are the 6 Tetrahedral
			    for (OutChan=0; OutChan < TotalOut; OutChan++)
			    {
			        c0 = DENS(X0, Y0, Z0, LutTable, OutChan);
			        
			        if (rx >= ry && ry >= rz)
			        {
			            c1 = DENS(X1, Y0, Z0, LutTable, OutChan) - c0;
			            c2 = DENS(X1, Y1, Z0, LutTable, OutChan) - DENS(X1, Y0, Z0, LutTable, OutChan);
			            c3 = DENS(X1, Y1, Z1, LutTable, OutChan) - DENS(X1, Y1, Z0, LutTable, OutChan);
			        }
			        else
			        {
			            if (rx >= rz && rz >= ry)
			            {            
			                c1 = DENS(X1, Y0, Z0, LutTable, OutChan) - c0;
			                c2 = DENS(X1, Y1, Z1, LutTable, OutChan) - DENS(X1, Y0, Z1, LutTable, OutChan);
			                c3 = DENS(X1, Y0, Z1, LutTable, OutChan) - DENS(X1, Y0, Z0, LutTable, OutChan);
			            }
			            else
			            {
			                if (rz >= rx && rx >= ry)
			                {
			                    c1 = DENS(X1, Y0, Z1, LutTable, OutChan) - DENS(X0, Y0, Z1, LutTable, OutChan);
			                    c2 = DENS(X1, Y1, Z1, LutTable, OutChan) - DENS(X1, Y0, Z1, LutTable, OutChan);
			                    c3 = DENS(X0, Y0, Z1, LutTable, OutChan) - c0;                            
			                }
			                else
			                {
			                    if (ry >= rx && rx >= rz)
			                    {
			                        c1 = DENS(X1, Y1, Z0, LutTable, OutChan) - DENS(X0, Y1, Z0, LutTable, OutChan);
			                        c2 = DENS(X0, Y1, Z0, LutTable, OutChan) - c0;
			                        c3 = DENS(X1, Y1, Z1, LutTable, OutChan) - DENS(X1, Y1, Z0, LutTable, OutChan);
			                    }
			                    else
			                    {
			                        if (ry >= rz && rz >= rx)
			                        {
			                            c1 = DENS(X1, Y1, Z1, LutTable, OutChan) - DENS(X0, Y1, Z1, LutTable, OutChan);
			                            c2 = DENS(X0, Y1, Z0, LutTable, OutChan) - c0;
			                            c3 = DENS(X0, Y1, Z1, LutTable, OutChan) - DENS(X0, Y1, Z0, LutTable, OutChan);
			                        }
			                        else
			                        {
			                            if (rz >= ry && ry >= rx)
			                            {             
			                                c1 = DENS(X1, Y1, Z1, LutTable, OutChan) - DENS(X0, Y1, Z1, LutTable, OutChan);
			                                c2 = DENS(X0, Y1, Z1, LutTable, OutChan) - DENS(X0, Y0, Z1, LutTable, OutChan);
			                                c3 = DENS(X0, Y0, Z1, LutTable, OutChan) - c0;
			                            }
			                            else
			                            {
			                                c1 = c2 = c3 = 0;                               
			                            }
			                        }
			                    }
			                }
			            }
			        }
			        
			        Rest = c1 * rx + c2 * ry + c3 * rz;
			        
			        Output[OutChan] = (short)(c0 + lcms2_internal.ROUND_FIXED_TO_INT(lcms2_internal._cmsToFixedDomain(Rest)));
			    }
		    }
		}
	}
	
	private static final _cmsInterpFn16 TetrahedralInterp16 = new TetrahedralInterp16Impl();
	
	private final static class Eval4InputsImpl implements _cmsInterpFn16, lcms2_internal._cmsOPTeval16Fn
	{
		public void run(short[] In, short[] Out, Object Data)
		{
			run(In, Out, (cmsInterpParams)Data);
		}
		
		public void run(short[] Input, short[] Output, cmsInterpParams p16)
		{
			int fk;
		    int k0, rk;
		    int K0, K1;
		    int fx, fy, fz;
		    int rx, ry, rz;
		    int x0, y0, z0;
		    int X0, X1, Y0, Y1, Z0, Z1;
		    int i;
		    int c0, c1, c2, c3, Rest;       
		    int OutChan;
		    short[] Tmp1 = new short[lcms2_internal.MAX_STAGE_CHANNELS], Tmp2 = new short[lcms2_internal.MAX_STAGE_CHANNELS];
		    
		    fk  = lcms2_internal._cmsToFixedDomain((Input[0] & 0xFFFF) * p16.Domain[0]);
		    fx  = lcms2_internal._cmsToFixedDomain((Input[1] & 0xFFFF) * p16.Domain[1]);
		    fy  = lcms2_internal._cmsToFixedDomain((Input[2] & 0xFFFF) * p16.Domain[2]);
		    fz  = lcms2_internal._cmsToFixedDomain((Input[3] & 0xFFFF) * p16.Domain[3]);
		    
		    k0  = lcms2_internal.FIXED_TO_INT(fk);
		    x0  = lcms2_internal.FIXED_TO_INT(fx);
		    y0  = lcms2_internal.FIXED_TO_INT(fy);
		    z0  = lcms2_internal.FIXED_TO_INT(fz);
		    
		    rk  = lcms2_internal.FIXED_REST_TO_INT(fk);
		    rx  = lcms2_internal.FIXED_REST_TO_INT(fx);
		    ry  = lcms2_internal.FIXED_REST_TO_INT(fy);
		    rz  = lcms2_internal.FIXED_REST_TO_INT(fz);
		    
		    K0 = p16.opta[3] * k0;
		    K1 = K0 + (Input[0] == (short)0xFFFF ? 0 : p16.opta[3]);
		    
		    X0 = p16.opta[2] * x0;
		    X1 = X0 + (Input[1] == (short)0xFFFF ? 0 : p16.opta[2]);
		    
		    Y0 = p16.opta[1] * y0;
		    Y1 = Y0 + (Input[2] == (short)0xFFFF ? 0 : p16.opta[1]);
		    
		    Z0 = p16.opta[0] * z0;
		    Z1 = Z0 + (Input[3] == (short)0xFFFF ? 0 : p16.opta[0]);
		    
		    if(p16.Table instanceof VirtualPointer)
		    {
			    final VirtualPointer.TypeProcessor LutTable = ((VirtualPointer)p16.Table).getProcessor();
			    final int pos = LutTable.getPosition();
			    
			    int tpos = pos + (K0 * 2);
			    
			    for (OutChan=0; OutChan < p16.nOutputs; OutChan++)
			    {
			        c0 = DENSS(X0, Y0, Z0, LutTable, tpos, OutChan);
			        
			        if (rx >= ry && ry >= rz)
			        {
			            c1 = DENSS(X1, Y0, Z0, LutTable, tpos, OutChan) - c0;
			            c2 = DENSS(X1, Y1, Z0, LutTable, tpos, OutChan) - DENSS(X1, Y0, Z0, LutTable, tpos, OutChan);
			            c3 = DENSS(X1, Y1, Z1, LutTable, tpos, OutChan) - DENSS(X1, Y1, Z0, LutTable, tpos, OutChan);
			        }
			        else
			        {
			            if (rx >= rz && rz >= ry)
			            {            
			                c1 = DENSS(X1, Y0, Z0, LutTable, tpos, OutChan) - c0;
			                c2 = DENSS(X1, Y1, Z1, LutTable, tpos, OutChan) - DENSS(X1, Y0, Z1, LutTable, tpos, OutChan);
			                c3 = DENSS(X1, Y0, Z1, LutTable, tpos, OutChan) - DENSS(X1, Y0, Z0, LutTable, tpos, OutChan);
			            }
			            else
			            {
			                if (rz >= rx && rx >= ry)
			                {
			                    c1 = DENSS(X1, Y0, Z1, LutTable, tpos, OutChan) - DENSS(X0, Y0, Z1, LutTable, tpos, OutChan);
			                    c2 = DENSS(X1, Y1, Z1, LutTable, tpos, OutChan) - DENSS(X1, Y0, Z1, LutTable, tpos, OutChan);
			                    c3 = DENSS(X0, Y0, Z1, LutTable, tpos, OutChan) - c0;
			                }
			                else
			                {
			                    if (ry >= rx && rx >= rz)
			                    {
			                        c1 = DENSS(X1, Y1, Z0, LutTable, tpos, OutChan) - DENSS(X0, Y1, Z0, LutTable, tpos, OutChan);
			                        c2 = DENSS(X0, Y1, Z0, LutTable, tpos, OutChan) - c0;
			                        c3 = DENSS(X1, Y1, Z1, LutTable, tpos, OutChan) - DENSS(X1, Y1, Z0, LutTable, tpos, OutChan);
			                    }
			                    else
			                    {
			                        if (ry >= rz && rz >= rx)
			                        {
			                            c1 = DENSS(X1, Y1, Z1, LutTable, tpos, OutChan) - DENSS(X0, Y1, Z1, LutTable, tpos, OutChan);
			                            c2 = DENSS(X0, Y1, Z0, LutTable, tpos, OutChan) - c0;
			                            c3 = DENSS(X0, Y1, Z1, LutTable, tpos, OutChan) - DENSS(X0, Y1, Z0, LutTable, tpos, OutChan);
			                        }
			                        else
			                        {
			                            if (rz >= ry && ry >= rx)
			                            {             
			                                c1 = DENSS(X1, Y1, Z1, LutTable, tpos, OutChan) - DENSS(X0, Y1, Z1, LutTable, tpos, OutChan);
			                                c2 = DENSS(X0, Y1, Z1, LutTable, tpos, OutChan) - DENSS(X0, Y0, Z1, LutTable, tpos, OutChan);
			                                c3 = DENSS(X0, Y0, Z1, LutTable, tpos, OutChan) - c0;
			                            }
			                            else 
			                            {
			                                c1 = c2 = c3 = 0;                               
			                            }
			                        }
			                    }
			                }
			            }
			        }
			        
			        Rest = c1 * rx + c2 * ry + c3 * rz;
			        
			        Tmp1[OutChan] = (short)(c0 + lcms2_internal.ROUND_FIXED_TO_INT(lcms2_internal._cmsToFixedDomain(Rest)));
			    }
			    
			    tpos = pos + (K1 * 2);
			    
			    for (OutChan=0; OutChan < p16.nOutputs; OutChan++)
			    {
			        c0 = DENSS(X0, Y0, Z0, LutTable, tpos, OutChan);
			        
			        if (rx >= ry && ry >= rz)
			        {
			            c1 = DENSS(X1, Y0, Z0, LutTable, tpos, OutChan) - c0;
			            c2 = DENSS(X1, Y1, Z0, LutTable, tpos, OutChan) - DENSS(X1, Y0, Z0, LutTable, tpos, OutChan);
			            c3 = DENSS(X1, Y1, Z1, LutTable, tpos, OutChan) - DENSS(X1, Y1, Z0, LutTable, tpos, OutChan);
			        }
			        else
			        {
			            if (rx >= rz && rz >= ry)
			            {
			                c1 = DENSS(X1, Y0, Z0, LutTable, tpos, OutChan) - c0;
			                c2 = DENSS(X1, Y1, Z1, LutTable, tpos, OutChan) - DENSS(X1, Y0, Z1, LutTable, tpos, OutChan);
			                c3 = DENSS(X1, Y0, Z1, LutTable, tpos, OutChan) - DENSS(X1, Y0, Z0, LutTable, tpos, OutChan);
			            }
			            else
			            {
			                if (rz >= rx && rx >= ry)
			                {
			                    c1 = DENSS(X1, Y0, Z1, LutTable, tpos, OutChan) - DENSS(X0, Y0, Z1, LutTable, tpos, OutChan);
			                    c2 = DENSS(X1, Y1, Z1, LutTable, tpos, OutChan) - DENSS(X1, Y0, Z1, LutTable, tpos, OutChan);
			                    c3 = DENSS(X0, Y0, Z1, LutTable, tpos, OutChan) - c0;
			                }
			                else
			                {
			                    if (ry >= rx && rx >= rz)
			                    {
			                        c1 = DENSS(X1, Y1, Z0, LutTable, tpos, OutChan) - DENSS(X0, Y1, Z0, LutTable, tpos, OutChan);
			                        c2 = DENSS(X0, Y1, Z0, LutTable, tpos, OutChan) - c0;
			                        c3 = DENSS(X1, Y1, Z1, LutTable, tpos, OutChan) - DENSS(X1, Y1, Z0, LutTable, tpos, OutChan);
			                    }
			                    else
			                    {
			                        if (ry >= rz && rz >= rx)
			                        {
			                            c1 = DENSS(X1, Y1, Z1, LutTable, tpos, OutChan) - DENSS(X0, Y1, Z1, LutTable, tpos, OutChan);
			                            c2 = DENSS(X0, Y1, Z0, LutTable, tpos, OutChan) - c0;
			                            c3 = DENSS(X0, Y1, Z1, LutTable, tpos, OutChan) - DENSS(X0, Y1, Z0, LutTable, tpos, OutChan);
			                        }
			                        else
			                        {
			                            if (rz >= ry && ry >= rx)
			                            {             
			                                c1 = DENSS(X1, Y1, Z1, LutTable, tpos, OutChan) - DENSS(X0, Y1, Z1, LutTable, tpos, OutChan);
			                                c2 = DENSS(X0, Y1, Z1, LutTable, tpos, OutChan) - DENSS(X0, Y0, Z1, LutTable, tpos, OutChan);
			                                c3 = DENSS(X0, Y0, Z1, LutTable, tpos, OutChan) - c0;
			                            }
			                            else
			                            {
			                                c1 = c2 = c3 = 0;                               
			                            }
			                        }
			                    }
			                }
			            }
			        }
			        
			        Rest = c1 * rx + c2 * ry + c3 * rz;
			        
			        Tmp2[OutChan] = (short)(c0 + lcms2_internal.ROUND_FIXED_TO_INT(lcms2_internal._cmsToFixedDomain(Rest)));
			    }
			    LutTable.setPosition(pos);
		    }
		    else
		    {
		    	final short[] LutTable = (short[])p16.Table;
			    int LutTablePos = K0;
			    
			    for (OutChan=0; OutChan < p16.nOutputs; OutChan++)
			    {
			        c0 = DENS(X0, Y0, Z0, LutTable, LutTablePos, OutChan);
			        
			        if (rx >= ry && ry >= rz)
			        {
			            c1 = DENS(X1, Y0, Z0, LutTable, LutTablePos, OutChan) - c0;
			            c2 = DENS(X1, Y1, Z0, LutTable, LutTablePos, OutChan) - DENS(X1, Y0, Z0, LutTable, LutTablePos, OutChan);
			            c3 = DENS(X1, Y1, Z1, LutTable, LutTablePos, OutChan) - DENS(X1, Y1, Z0, LutTable, LutTablePos, OutChan);
			        }
			        else
			        {
			            if (rx >= rz && rz >= ry)
			            {            
			                c1 = DENS(X1, Y0, Z0, LutTable, LutTablePos, OutChan) - c0;
			                c2 = DENS(X1, Y1, Z1, LutTable, LutTablePos, OutChan) - DENS(X1, Y0, Z1, LutTable, LutTablePos, OutChan);
			                c3 = DENS(X1, Y0, Z1, LutTable, LutTablePos, OutChan) - DENS(X1, Y0, Z0, LutTable, LutTablePos, OutChan);
			            }
			            else
			            {
			                if (rz >= rx && rx >= ry)
			                {
			                    c1 = DENS(X1, Y0, Z1, LutTable, LutTablePos, OutChan) - DENS(X0, Y0, Z1, LutTable, LutTablePos, OutChan);
			                    c2 = DENS(X1, Y1, Z1, LutTable, LutTablePos, OutChan) - DENS(X1, Y0, Z1, LutTable, LutTablePos, OutChan);
			                    c3 = DENS(X0, Y0, Z1, LutTable, LutTablePos, OutChan) - c0;
			                }
			                else
			                {
			                    if (ry >= rx && rx >= rz)
			                    {
			                        c1 = DENS(X1, Y1, Z0, LutTable, LutTablePos, OutChan) - DENS(X0, Y1, Z0, LutTable, LutTablePos, OutChan);
			                        c2 = DENS(X0, Y1, Z0, LutTable, LutTablePos, OutChan) - c0;
			                        c3 = DENS(X1, Y1, Z1, LutTable, LutTablePos, OutChan) - DENS(X1, Y1, Z0, LutTable, LutTablePos, OutChan);
			                    }
			                    else
			                    {
			                        if (ry >= rz && rz >= rx)
			                        {
			                            c1 = DENS(X1, Y1, Z1, LutTable, LutTablePos, OutChan) - DENS(X0, Y1, Z1, LutTable, LutTablePos, OutChan);
			                            c2 = DENS(X0, Y1, Z0, LutTable, LutTablePos, OutChan) - c0;
			                            c3 = DENS(X0, Y1, Z1, LutTable, LutTablePos, OutChan) - DENS(X0, Y1, Z0, LutTable, LutTablePos, OutChan);
			                        }
			                        else
			                        {
			                            if (rz >= ry && ry >= rx)
			                            {             
			                                c1 = DENS(X1, Y1, Z1, LutTable, LutTablePos, OutChan) - DENS(X0, Y1, Z1, LutTable, LutTablePos, OutChan);
			                                c2 = DENS(X0, Y1, Z1, LutTable, LutTablePos, OutChan) - DENS(X0, Y0, Z1, LutTable, LutTablePos, OutChan);
			                                c3 = DENS(X0, Y0, Z1, LutTable, LutTablePos, OutChan) - c0;
			                            }
			                            else 
			                            {
			                                c1 = c2 = c3 = 0;                               
			                            }
			                        }
			                    }
			                }
			            }
			        }
			        
			        Rest = c1 * rx + c2 * ry + c3 * rz;
			        
			        Tmp1[OutChan] = (short)(c0 + lcms2_internal.ROUND_FIXED_TO_INT(lcms2_internal._cmsToFixedDomain(Rest)));
			    }
			    
			    LutTablePos = K1;
			    
			    for (OutChan=0; OutChan < p16.nOutputs; OutChan++)
			    {
			        c0 = DENS(X0, Y0, Z0, LutTable, LutTablePos, OutChan);
			        
			        if (rx >= ry && ry >= rz)
			        {
			            c1 = DENS(X1, Y0, Z0, LutTable, LutTablePos, OutChan) - c0;
			            c2 = DENS(X1, Y1, Z0, LutTable, LutTablePos, OutChan) - DENS(X1, Y0, Z0, LutTable, LutTablePos, OutChan);
			            c3 = DENS(X1, Y1, Z1, LutTable, LutTablePos, OutChan) - DENS(X1, Y1, Z0, LutTable, LutTablePos, OutChan);
			        }
			        else
			        {
			            if (rx >= rz && rz >= ry)
			            {
			                c1 = DENS(X1, Y0, Z0, LutTable, LutTablePos, OutChan) - c0;
			                c2 = DENS(X1, Y1, Z1, LutTable, LutTablePos, OutChan) - DENS(X1, Y0, Z1, LutTable, LutTablePos, OutChan);
			                c3 = DENS(X1, Y0, Z1, LutTable, LutTablePos, OutChan) - DENS(X1, Y0, Z0, LutTable, LutTablePos, OutChan);
			            }
			            else
			            {
			                if (rz >= rx && rx >= ry)
			                {
			                    c1 = DENS(X1, Y0, Z1, LutTable, LutTablePos, OutChan) - DENS(X0, Y0, Z1, LutTable, LutTablePos, OutChan);
			                    c2 = DENS(X1, Y1, Z1, LutTable, LutTablePos, OutChan) - DENS(X1, Y0, Z1, LutTable, LutTablePos, OutChan);
			                    c3 = DENS(X0, Y0, Z1, LutTable, LutTablePos, OutChan) - c0;
			                }
			                else
			                {
			                    if (ry >= rx && rx >= rz)
			                    {
			                        c1 = DENS(X1, Y1, Z0, LutTable, LutTablePos, OutChan) - DENS(X0, Y1, Z0, LutTable, LutTablePos, OutChan);
			                        c2 = DENS(X0, Y1, Z0, LutTable, LutTablePos, OutChan) - c0;
			                        c3 = DENS(X1, Y1, Z1, LutTable, LutTablePos, OutChan) - DENS(X1, Y1, Z0, LutTable, LutTablePos, OutChan);
			                    }
			                    else
			                    {
			                        if (ry >= rz && rz >= rx)
			                        {
			                            c1 = DENS(X1, Y1, Z1, LutTable, LutTablePos, OutChan) - DENS(X0, Y1, Z1, LutTable, LutTablePos, OutChan);
			                            c2 = DENS(X0, Y1, Z0, LutTable, LutTablePos, OutChan) - c0;
			                            c3 = DENS(X0, Y1, Z1, LutTable, LutTablePos, OutChan) - DENS(X0, Y1, Z0, LutTable, LutTablePos, OutChan);
			                        }
			                        else
			                        {
			                            if (rz >= ry && ry >= rx)
			                            {             
			                                c1 = DENS(X1, Y1, Z1, LutTable, LutTablePos, OutChan) - DENS(X0, Y1, Z1, LutTable, LutTablePos, OutChan);
			                                c2 = DENS(X0, Y1, Z1, LutTable, LutTablePos, OutChan) - DENS(X0, Y0, Z1, LutTable, LutTablePos, OutChan);
			                                c3 = DENS(X0, Y0, Z1, LutTable, LutTablePos, OutChan) - c0;
			                            }
			                            else
			                            {
			                                c1 = c2 = c3 = 0;                               
			                            }
			                        }
			                    }
			                }
			            }
			        }
			        
			        Rest = c1 * rx + c2 * ry + c3 * rz;
			        
			        Tmp2[OutChan] = (short)(c0 + lcms2_internal.ROUND_FIXED_TO_INT(lcms2_internal._cmsToFixedDomain(Rest)));
			    }
		    }
		    
		    for (i=0; i < p16.nOutputs; i++)
		    {
		        Output[i] = LinearInterp(rk, Tmp1[i] & 0xFFFF, Tmp2[i] & 0xFFFF);
		    }
		}
	}
	
	private static final _cmsInterpFn16 Eval4Inputs = new Eval4InputsImpl();
	
	private static cmsInterpParams dupParams(cmsInterpParams p)
	{
		cmsInterpParams p1 = new cmsInterpParams();
		p1.ContextID = p.ContextID;
		p1.Domain = Arrays.copy(p.Domain);
		p1.dwFlags = p.dwFlags;
		p1.Interpolation = p.Interpolation;
		p1.nInputs = p.nInputs;
		p1.nOutputs = p.nOutputs;
		p1.nSamples = p.nSamples;
		p1.opta = p.opta;
		p1.Table = p.Table;
		return p1;
	}
	
	// For more that 3 inputs (i.e., CMYK)
	// evaluate two 3-dimensional interpolations and then linearly interpolate between them.
	
	private static final _cmsInterpFnFloat Eval4InputsFloat = new _cmsInterpFnFloat()
	{
		public void run(float[] Input, float[] Output, cmsInterpParams p)
		{
			float rest;
			float pk;
			int k0, K0, K1;
			int i;
			float[] Tmp1 = new float[lcms2_internal.MAX_STAGE_CHANNELS], Tmp2 = new float[lcms2_internal.MAX_STAGE_CHANNELS];
			cmsInterpParams p1;
			
			pk = Input[0] * p.Domain[0];
			k0 = lcms2_internal._cmsQuickFloor(pk);
			rest = pk - k0;
			
			K0 = p.opta[3] * k0;
			K1 = K0 + (Input[0] >= 1.0 ? 0 : p.opta[3]);
			
			p1 = dupParams(p);
			System.arraycopy(p.Domain, 1, p1.Domain, 0, 3);
			
			if(p.Table instanceof VirtualPointer)
			{
				final VirtualPointer LutTable = (VirtualPointer)p.Table;
				
				p1.Table = new VirtualPointer(LutTable, K0 * 4);
				
				float[] tempInput = new float[3];
				System.arraycopy(Input, 1, tempInput, 0, 3);
				TetrahedralInterpFloat.run(tempInput, Tmp1, p1);
				
				((VirtualPointer)p1.Table).setPosition(LutTable.getPosition() + (K1 * 4));
				
				TetrahedralInterpFloat.run(tempInput, Tmp2, p1);
			}
			else
			{
				final float[] LutTable = (float[])p.Table;
				
				int temp = LutTable.length - K0;
				float[] T = new float[temp];
				System.arraycopy(LutTable, K0, T, 0, temp);
				p1.Table = T;
				
				float[] tempInput = new float[3];
				System.arraycopy(Input, 1, tempInput, 0, 3);
				TetrahedralInterpFloat.run(tempInput, Tmp1, p1);
				
				if(K0 != K1)
				{
					temp = LutTable.length - K1;
					T = new float[temp];
					System.arraycopy(LutTable, K1, T, 0, temp);
					p1.Table = T;
				}
				TetrahedralInterpFloat.run(tempInput, Tmp2, p1);
			}
			
			for (i=0; i < p.nOutputs; i++)
			{
				float y0 = Tmp1[i];
				float y1 = Tmp2[i];
				
				Output[i] = y0 + (y1 - y0) * rest;
			}
		}
	};
	
	private static final _cmsInterpFn16 Eval5Inputs = new _cmsInterpFn16()
	{
		public void run(short[] Input, short[] Output, cmsInterpParams p16)
		{
			int fk;
			int k0, rk;
			int K0, K1;
			int i;
			short[] Tmp1 = new short[lcms2_internal.MAX_STAGE_CHANNELS], Tmp2 = new short[lcms2_internal.MAX_STAGE_CHANNELS];
			cmsInterpParams p1;
			
			fk = lcms2_internal._cmsToFixedDomain((Input[0] & 0xFFFF) * p16.Domain[0]);
			k0 = lcms2_internal.FIXED_TO_INT(fk);
			rk = lcms2_internal.FIXED_REST_TO_INT(fk);
			
			K0 = p16.opta[4] * k0;
			K1 = p16.opta[4] * (k0 + (Input[0] != (short)0xFFFF ? 1 : 0));
			
			p1 = dupParams(p16);
			System.arraycopy(p16.Domain, 1, p1.Domain, 0, 4);
			
			if(p16.Table instanceof VirtualPointer)
			{
				final VirtualPointer LutTable = (VirtualPointer)p16.Table;
				
				p1.Table = new VirtualPointer(LutTable, K0 * 2);
				
				short[] tempInput = new short[4];
				System.arraycopy(Input, 1, tempInput, 0, 4);
				Eval4Inputs.run(tempInput, Tmp1, p1);
				
				((VirtualPointer)p1.Table).setPosition(LutTable.getPosition() + (K1 * 2));
				
				Eval4Inputs.run(tempInput, Tmp2, p1);
			}
			else
			{
				final short[] LutTable = (short[])p16.Table;
				
				int temp = LutTable.length - K0;
				short[] T = new short[temp];
				System.arraycopy(LutTable, K0, T, 0, temp);
				p1.Table = T;
				
				short[] tempInput = new short[4];
				System.arraycopy(Input, 1, tempInput, 0, 4);
				Eval4Inputs.run(tempInput, Tmp1, p1);
				
				if(K0 != K1)
				{
					temp = LutTable.length - K1;
					T = new short[temp];
					System.arraycopy(LutTable, K1, T, 0, temp);
					p1.Table = T;
				}
				Eval4Inputs.run(tempInput, Tmp2, p1);
			}
			
			for (i=0; i < p16.nOutputs; i++)
			{
				Output[i] = LinearInterp(rk, Tmp1[i] & 0xFFFF, Tmp2[i] & 0xFFFF);
			}
		}
	};
	
	private static final _cmsInterpFnFloat Eval5InputsFloat = new _cmsInterpFnFloat()
	{
		public void run(float[] Input, float[] Output, cmsInterpParams p)
		{
			float rest;
			float pk;
			int k0, K0, K1;
			int i;
			float[] Tmp1 = new float[lcms2_internal.MAX_STAGE_CHANNELS], Tmp2 = new float[lcms2_internal.MAX_STAGE_CHANNELS];
			cmsInterpParams p1;
			
			pk = Input[0] * p.Domain[0];
			k0 = lcms2_internal._cmsQuickFloor(pk);
			rest = pk - k0;
			
			K0 = p.opta[4] * k0;
			K1 = K0 + (Input[0] >= 1.0 ? 0 : p.opta[4]);
			
			p1 = dupParams(p);
			System.arraycopy(p.Domain, 1, p1.Domain, 0, 4);
			
			if(p.Table instanceof VirtualPointer)
			{
				final VirtualPointer LutTable = (VirtualPointer)p.Table;
				
				p1.Table = new VirtualPointer(LutTable, K0 * 4);
				
				float[] tempInput = new float[4];
				System.arraycopy(Input, 1, tempInput, 0, 4);
				Eval4InputsFloat.run(tempInput, Tmp1, p1);
				
				((VirtualPointer)p1.Table).setPosition(LutTable.getPosition() + (K1 * 4));
				
				Eval4InputsFloat.run(tempInput, Tmp2, p1);
			}
			else
			{
				final float[] LutTable = (float[])p.Table;
				
				int temp = LutTable.length - K0;
				float[] T = new float[temp];
				System.arraycopy(LutTable, K0, T, 0, temp);
				p1.Table = T;
				
				float[] tempInput = new float[4];
				System.arraycopy(Input, 1, tempInput, 0, 4);
				Eval4InputsFloat.run(tempInput, Tmp1, p1);
				
				if(K0 != K1)
				{
					temp = LutTable.length - K1;
					T = new float[temp];
					System.arraycopy(LutTable, K1, T, 0, temp);
					p1.Table = T;
				}
				Eval4InputsFloat.run(tempInput, Tmp2, p1);
			}
			
			for (i=0; i < p.nOutputs; i++)
			{
				float y0 = Tmp1[i];
				float y1 = Tmp2[i];
				
				Output[i] = y0 + (y1 - y0) * rest;
			}
		}
	};
	
	private static final _cmsInterpFn16 Eval6Inputs = new _cmsInterpFn16()
	{
		public void run(short[] Input, short[] Output, cmsInterpParams p16)
		{
			int fk;
			int k0, rk;
			int K0, K1;
			int i;
			short[] Tmp1 = new short[lcms2_internal.MAX_STAGE_CHANNELS], Tmp2 = new short[lcms2_internal.MAX_STAGE_CHANNELS];
			cmsInterpParams p1;
			
			fk = lcms2_internal._cmsToFixedDomain((Input[0] & 0xFFFF) * p16.Domain[0]);
			k0 = lcms2_internal.FIXED_TO_INT(fk);
			rk = lcms2_internal.FIXED_REST_TO_INT(fk);
			
			K0 = p16.opta[5] * k0;
			K1 = p16.opta[5] * (k0 + (Input[0] != (short)0xFFFF ? 1 : 0));
			
			p1 = dupParams(p16);
			System.arraycopy(p16.Domain, 1, p1.Domain, 0, 5);
			
			if(p16.Table instanceof VirtualPointer)
			{
				final VirtualPointer LutTable = (VirtualPointer)p16.Table;
				
				p1.Table = new VirtualPointer(LutTable, K0 * 2);
				
				short[] tempInput = new short[5];
				System.arraycopy(Input, 1, tempInput, 0, 5);
				Eval5Inputs.run(tempInput, Tmp1, p1);
				
				((VirtualPointer)p1.Table).setPosition(LutTable.getPosition() + (K1 * 2));
				
				Eval5Inputs.run(tempInput, Tmp2, p1);
			}
			else
			{
				final short[] LutTable = (short[])p16.Table;
				
				int temp = LutTable.length - K0;
				short[] T = new short[temp];
				System.arraycopy(LutTable, K0, T, 0, temp);
				p1.Table = T;
				
				short[] tempInput = new short[5];
				System.arraycopy(Input, 1, tempInput, 0, 5);
				Eval5Inputs.run(tempInput, Tmp1, p1);
				
				if(K0 != K1)
				{
					temp = LutTable.length - K1;
					T = new short[temp];
					System.arraycopy(LutTable, K1, T, 0, temp);
					p1.Table = T;
				}
				Eval5Inputs.run(tempInput, Tmp2, p1);
			}
			
			for (i=0; i < p16.nOutputs; i++)
			{
				Output[i] = LinearInterp(rk, Tmp1[i] & 0xFFFF, Tmp2[i] & 0xFFFF);
			}
		}
	};
	
	private static final _cmsInterpFnFloat Eval6InputsFloat = new _cmsInterpFnFloat()
	{
		public void run(float[] Input, float[] Output, cmsInterpParams p)
		{
			float rest;
			float pk;
			int k0, K0, K1;
			int i;
			float[] Tmp1 = new float[lcms2_internal.MAX_STAGE_CHANNELS], Tmp2 = new float[lcms2_internal.MAX_STAGE_CHANNELS];
			cmsInterpParams p1;
			
			pk = Input[0] * p.Domain[0];
			k0 = lcms2_internal._cmsQuickFloor(pk);
			rest = pk - k0;
			
			K0 = p.opta[5] * k0;
			K1 = K0 + (Input[0] >= 1.0 ? 0 : p.opta[5]);
			
			p1 = dupParams(p);
			System.arraycopy(p.Domain, 1, p1.Domain, 0, 5);
			
			if(p.Table instanceof VirtualPointer)
			{
				final VirtualPointer LutTable = (VirtualPointer)p.Table;
				
				p1.Table = new VirtualPointer(LutTable, K0 * 4);
				
				float[] tempInput = new float[5];
				System.arraycopy(Input, 1, tempInput, 0, 5);
				Eval5InputsFloat.run(tempInput, Tmp1, p1);
				
				((VirtualPointer)p1.Table).setPosition(LutTable.getPosition() + (K1 * 4));
				
				Eval5InputsFloat.run(tempInput, Tmp2, p1);
			}
			else
			{
				final float[] LutTable = (float[])p.Table;
				
				int temp = LutTable.length - K0;
				float[] T = new float[temp];
				System.arraycopy(LutTable, K0, T, 0, temp);
				p1.Table = T;
				
				float[] tempInput = new float[5];
				System.arraycopy(Input, 1, tempInput, 0, 5);
				Eval5InputsFloat.run(tempInput, Tmp1, p1);
				
				if(K0 != K1)
				{
					temp = LutTable.length - K1;
					T = new float[temp];
					System.arraycopy(LutTable, K1, T, 0, temp);
					p1.Table = T;
				}
				Eval5InputsFloat.run(tempInput, Tmp2, p1);
			}
			
			for (i=0; i < p.nOutputs; i++)
			{
				float y0 = Tmp1[i];
				float y1 = Tmp2[i];
				
				Output[i] = y0 + (y1 - y0) * rest;
			}
		}
	};
	
	private static final _cmsInterpFn16 Eval7Inputs = new _cmsInterpFn16()
	{
		public void run(short[] Input, short[] Output, cmsInterpParams p16)
		{
			int fk;
			int k0, rk;
			int K0, K1;
			int i;
			short[] Tmp1 = new short[lcms2_internal.MAX_STAGE_CHANNELS], Tmp2 = new short[lcms2_internal.MAX_STAGE_CHANNELS];
			cmsInterpParams p1;
			
			fk = lcms2_internal._cmsToFixedDomain((Input[0] & 0xFFFF) * p16.Domain[0]);
			k0 = lcms2_internal.FIXED_TO_INT(fk);
			rk = lcms2_internal.FIXED_REST_TO_INT(fk);
			
			K0 = p16.opta[6] * k0;
			K1 = p16.opta[6] * (k0 + (Input[0] != (short)0xFFFF ? 1 : 0));
			
			p1 = dupParams(p16);
			System.arraycopy(p16.Domain, 1, p1.Domain, 0, 6);
			
			if(p16.Table instanceof VirtualPointer)
			{
				final VirtualPointer LutTable = (VirtualPointer)p16.Table;
				
				p1.Table = new VirtualPointer(LutTable, K0 * 2);
				
				short[] tempInput = new short[6];
				System.arraycopy(Input, 1, tempInput, 0, 6);
				Eval6Inputs.run(tempInput, Tmp1, p1);
				
				((VirtualPointer)p1.Table).setPosition(LutTable.getPosition() + (K1 * 2));
				
				Eval6Inputs.run(tempInput, Tmp2, p1);
			}
			else
			{
				final short[] LutTable = (short[])p16.Table;
				
				int temp = LutTable.length - K0;
				short[] T = new short[temp];
				System.arraycopy(LutTable, K0, T, 0, temp);
				p1.Table = T;
				
				short[] tempInput = new short[6];
				System.arraycopy(Input, 1, tempInput, 0, 6);
				Eval6Inputs.run(tempInput, Tmp1, p1);
				
				if(K0 != K1)
				{
					temp = LutTable.length - K1;
					T = new short[temp];
					System.arraycopy(LutTable, K1, T, 0, temp);
					p1.Table = T;
				}
				Eval6Inputs.run(tempInput, Tmp2, p1);
			}
			
			for (i=0; i < p16.nOutputs; i++)
			{
				Output[i] = LinearInterp(rk, Tmp1[i] & 0xFFFF, Tmp2[i] & 0xFFFF);
			}
		}
	};
	
	private static final _cmsInterpFnFloat Eval7InputsFloat = new _cmsInterpFnFloat()
	{
		public void run(float[] Input, float[] Output, cmsInterpParams p)
		{
			float rest;
			float pk;
			int k0, K0, K1;
			int i;
			float[] Tmp1 = new float[lcms2_internal.MAX_STAGE_CHANNELS], Tmp2 = new float[lcms2_internal.MAX_STAGE_CHANNELS];
			cmsInterpParams p1;
			
			pk = Input[0] * p.Domain[0];
			k0 = lcms2_internal._cmsQuickFloor(pk);
			rest = pk - k0;
			
			K0 = p.opta[6] * k0;
			K1 = K0 + (Input[0] >= 1.0 ? 0 : p.opta[6]);
			
			p1 = dupParams(p);
			System.arraycopy(p.Domain, 1, p1.Domain, 0, 6);
			
			if(p.Table instanceof VirtualPointer)
			{
				final VirtualPointer LutTable = (VirtualPointer)p.Table;
				
				p1.Table = new VirtualPointer(LutTable, K0 * 4);
				
				float[] tempInput = new float[6];
				System.arraycopy(Input, 1, tempInput, 0, 6);
				Eval6InputsFloat.run(tempInput, Tmp1, p1);
				
				((VirtualPointer)p1.Table).setPosition(LutTable.getPosition() + (K1 * 4));
				
				Eval6InputsFloat.run(tempInput, Tmp2, p1);
			}
			else
			{
				final float[] LutTable = (float[])p.Table;
				
				int temp = LutTable.length - K0;
				float[] T = new float[temp];
				System.arraycopy(LutTable, K0, T, 0, temp);
				p1.Table = T;
				
				float[] tempInput = new float[6];
				System.arraycopy(Input, 1, tempInput, 0, 6);
				Eval6InputsFloat.run(tempInput, Tmp1, p1);
				
				if(K0 != K1)
				{
					temp = LutTable.length - K1;
					T = new float[temp];
					System.arraycopy(LutTable, K1, T, 0, temp);
					p1.Table = T;
				}
				Eval6InputsFloat.run(tempInput, Tmp2, p1);
			}
			
			for (i=0; i < p.nOutputs; i++)
			{
				float y0 = Tmp1[i];
				float y1 = Tmp2[i];
				
				Output[i] = y0 + (y1 - y0) * rest;
			}
		}
	};
	
	private static final _cmsInterpFn16 Eval8Inputs = new _cmsInterpFn16()
	{
		public void run(short[] Input, short[] Output, cmsInterpParams p16)
		{
			int fk;
			int k0, rk;
			int K0, K1;
			int i;
			short[] Tmp1 = new short[lcms2_internal.MAX_STAGE_CHANNELS], Tmp2 = new short[lcms2_internal.MAX_STAGE_CHANNELS];
			cmsInterpParams p1;
			
			fk = lcms2_internal._cmsToFixedDomain((Input[0] & 0xFFFF) * p16.Domain[0]);
			k0 = lcms2_internal.FIXED_TO_INT(fk);
			rk = lcms2_internal.FIXED_REST_TO_INT(fk);
			
			K0 = p16.opta[7] * k0;
			K1 = p16.opta[7] * (k0 + (Input[0] != (short)0xFFFF ? 1 : 0));
			
			p1 = dupParams(p16);
			System.arraycopy(p16.Domain, 1, p1.Domain, 0, 7);
			
			if(p16.Table instanceof VirtualPointer)
			{
				final VirtualPointer LutTable = (VirtualPointer)p16.Table;
				
				p1.Table = new VirtualPointer(LutTable, K0 * 2);
				
				short[] tempInput = new short[7];
				System.arraycopy(Input, 1, tempInput, 0, 7);
				Eval7Inputs.run(tempInput, Tmp1, p1);
				
				((VirtualPointer)p1.Table).setPosition(LutTable.getPosition() + (K1 * 2));
				
				Eval7Inputs.run(tempInput, Tmp2, p1);
			}
			else
			{
				final short[] LutTable = (short[])p16.Table;
				
				int temp = LutTable.length - K0;
				short[] T = new short[temp];
				System.arraycopy(LutTable, K0, T, 0, temp);
				p1.Table = T;
				
				short[] tempInput = new short[7];
				System.arraycopy(Input, 1, tempInput, 0, 7);
				Eval7Inputs.run(tempInput, Tmp1, p1);
				
				if(K0 != K1)
				{
					temp = LutTable.length - K1;
					T = new short[temp];
					System.arraycopy(LutTable, K1, T, 0, temp);
					p1.Table = T;
				}
				Eval7Inputs.run(tempInput, Tmp2, p1);
			}
			
			for (i=0; i < p16.nOutputs; i++)
			{
				Output[i] = LinearInterp(rk, Tmp1[i] & 0xFFFF, Tmp2[i] & 0xFFFF);
			}
		}
	};
	
	private static final _cmsInterpFnFloat Eval8InputsFloat = new _cmsInterpFnFloat()
	{
		public void run(float[] Input, float[] Output, cmsInterpParams p)
		{
			float rest;
			float pk;
			int k0, K0, K1;
			int i;
			float[] Tmp1 = new float[lcms2_internal.MAX_STAGE_CHANNELS], Tmp2 = new float[lcms2_internal.MAX_STAGE_CHANNELS];
			cmsInterpParams p1;
			
			pk = Input[0] * p.Domain[0];
			k0 = lcms2_internal._cmsQuickFloor(pk);
			rest = pk - k0;
			
			K0 = p.opta[7] * k0;
			K1 = K0 + (Input[0] >= 1.0 ? 0 : p.opta[7]);
			
			p1 = dupParams(p);
			System.arraycopy(p.Domain, 1, p1.Domain, 0, 7);
			
			if(p.Table instanceof VirtualPointer)
			{
				final VirtualPointer LutTable = (VirtualPointer)p.Table;
				
				p1.Table = new VirtualPointer(LutTable, K0 * 4);
				
				float[] tempInput = new float[7];
				System.arraycopy(Input, 1, tempInput, 0, 7);
				Eval7InputsFloat.run(tempInput, Tmp1, p1);
				
				((VirtualPointer)p1.Table).setPosition(LutTable.getPosition() + (K1 * 4));
				
				Eval7InputsFloat.run(tempInput, Tmp2, p1);
			}
			else
			{
				final float[] LutTable = (float[])p.Table;
				
				int temp = LutTable.length - K0;
				float[] T = new float[temp];
				System.arraycopy(LutTable, K0, T, 0, temp);
				p1.Table = T;
				
				float[] tempInput = new float[7];
				System.arraycopy(Input, 1, tempInput, 0, 7);
				Eval7InputsFloat.run(tempInput, Tmp1, p1);
				
				if(K0 != K1)
				{
					temp = LutTable.length - K1;
					T = new float[temp];
					System.arraycopy(LutTable, K1, T, 0, temp);
					p1.Table = T;
				}
				Eval7InputsFloat.run(tempInput, Tmp2, p1);
			}
			
			for (i=0; i < p.nOutputs; i++)
			{
				float y0 = Tmp1[i];
				float y1 = Tmp2[i];
				
				Output[i] = y0 + (y1 - y0) * rest;
			}
		}
	};
	
	private static final cmsInterpFnFactory DefaultInterpolatorsFactory = new cmsInterpFnFactory()
	{
		public cmsInterpFunction run(int nInputChannels, int nOutputChannels, int dwFlags)
		{
			cmsInterpFunction Interpolation = new cmsInterpFunction();
		    boolean IsFloat     = (dwFlags & lcms2_plugin.CMS_LERP_FLAGS_FLOAT) != 0;
		    boolean IsTrilinear = (dwFlags & lcms2_plugin.CMS_LERP_FLAGS_TRILINEAR) != 0;
		    
		    // Safety check
		    if (nInputChannels >= 4 && nOutputChannels >= lcms2_internal.MAX_STAGE_CHANNELS)
		    {
		    	return Interpolation;
		    }
		    
		    switch (nInputChannels)
		    {
		    	case 1: // Gray LUT / linear
		    		Interpolation.set(nOutputChannels == 1 ? 
		    				(IsFloat ? (Object)LinLerp1Dfloat : LinLerp1D) :
		    				(IsFloat ? (Object)Eval1InputFloat : Eval1Input));
		    		break;
		    	case 2: // Duotone
		    		Interpolation.set(IsFloat ? (Object)BilinearInterpFloat : BilinearInterp16);
		    		break;
	    		case 3: // RGB et al
	    			Interpolation.set(IsTrilinear ? 
		    				(IsFloat ? (Object)TrilinearInterpFloat : TrilinearInterp16) :
		    				(IsFloat ? (Object)TetrahedralInterpFloat : TetrahedralInterp16));
	    			break;
    			case 4: // CMYK lut
    				Interpolation.set(IsFloat ? (Object)Eval4InputsFloat : Eval4Inputs);
    				break;
				case 5: // 5 Inks
					Interpolation.set(IsFloat ? (Object)Eval5InputsFloat : Eval5Inputs);
					break;
				case 6: // 6 Inks
					Interpolation.set(IsFloat ? (Object)Eval6InputsFloat : Eval6Inputs);
					break;
				case 7: // 7 inks
					Interpolation.set(IsFloat ? (Object)Eval7InputsFloat : Eval7Inputs);
					break;
				case 8: // 8 inks
					Interpolation.set(IsFloat ? (Object)Eval8InputsFloat : Eval8Inputs);
					break;
				default:
					Interpolation.set(null);
					break;
		    }
		    
		    return Interpolation;
		}
	};
	
	// This is the default factory
	private static cmsInterpFnFactory Interpolators;
	
	private static final long INTERPOLATORS_UID = 0x133F1C6D4325EFFAL;
	
	static
	{
		Object obj;
		if((obj = Utility.singletonStorageGet(INTERPOLATORS_UID)) != null)
		{
			Interpolators = (cmsInterpFnFactory)obj;
		}
		else
		{
			Interpolators = DefaultInterpolatorsFactory;
			Utility.singletonStorageSet(INTERPOLATORS_UID, Interpolators);
		}
	}
}
