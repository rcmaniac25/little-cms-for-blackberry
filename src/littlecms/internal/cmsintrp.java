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
import littlecms.internal.lcms2.cmsContext;
import littlecms.internal.lcms2_plugin._cmsInterpFn16;
import littlecms.internal.lcms2_plugin._cmsInterpFnFloat;
import littlecms.internal.lcms2_plugin.cmsInterpFnFactory;
import littlecms.internal.lcms2_plugin.cmsInterpFunction;
import littlecms.internal.lcms2_plugin.cmsInterpParams;
import littlecms.internal.lcms2_plugin.cmsPluginBase;
import littlecms.internal.lcms2_plugin.cmsPluginInterpolation;

/**
 * This module incorporates several interpolation routines, for 1, 3, 4, 5, 6, 7 and 8 channels on input and
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
	        return true;
	    }
	    
	    // Set replacement functions
	    Interpolators = Plugin.InterpolatorsFactory;  
	    return true;
	}
	
	// Set the interpolation method
	private static boolean _cmsSetInterpolationRoutine(cmsInterpParams p)
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
	    p.Table     = Table;
	    p.ContextID  = ContextID;
	    
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
		    final short[] LutTable = (short[])p.Table;
		    
		    // if last value... 
		    if ((Value[0] & 0xFFFF) == 0xffff)
		    {
		        Output[0] = LutTable[p.Domain[0]];
		        return;
		    }
		    
		    val3 = p.Domain[0] * Value[0];
		    val3 = lcms2_internal._cmsToFixedDomain(val3);	// To fixed 15.16
		    
		    cell0 = lcms2_internal.FIXED_TO_INT(val3);		// Cell is 16 MSB bits
		    rest  = lcms2_internal.FIXED_REST_TO_INT(val3);	// Rest is 16 LSB bits
		    
		    y0 = LutTable[cell0];
		    y1 = LutTable[cell0+1];
		    
		    Output[0] = LinearInterp(rest, y0, y1);
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
			final short[] LutTable = (short[])p16.Table;
			
			v = Input[0] * p16.Domain[0];
			fk = lcms2_internal._cmsToFixedDomain(v);
			
			k0 = lcms2_internal.FIXED_TO_INT(fk);
			rk = (short)lcms2_internal.FIXED_REST_TO_INT(fk);
			
			k1 = k0 + ((Input[0] & 0xFFFF) != 0xFFFF ? 1 : 0);
			
			K0 = p16.opta[0] * k0;
			K1 = p16.opta[0] * k1;
			
			for (OutChan=0; OutChan < p16.nOutputs; OutChan++)
			{
				Output[OutChan] = LinearInterp(rk, LutTable[K0+OutChan], LutTable[K1+OutChan]);
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
		    
		    cell0 *= p.opta[0];
		    cell1 *= p.opta[0];
		    
		    for (OutChan=0; OutChan < p.nOutputs; OutChan++)
		    {
		    	y0 = LutTable[cell0 + OutChan];
		    	y1 = LutTable[cell1 + OutChan];
		    	
		    	Output[OutChan] = y0 + (y1 - y0) * rest;
		    }
		}
	};
	
	private static float DENS(int i,int j,int k,float[] LutTable,int OutChan)
	{
		return (LutTable[(i)+(j)+(k)+OutChan]);
	}
	private static int DENS(int i,int j,int k,short[] LutTable,int OutChan)
	{
		return DENS(i,j,k,LutTable,0,OutChan);
	}
	private static int DENS(int i,int j,int k,short[] LutTable,int tableOffset,int OutChan)
	{
		return (LutTable[(i)+(j)+(k)+OutChan]);
	}
	
	// Trilinear interpolation (16 bits) - float version
	private static final _cmsInterpFnFloat TrilinearInterpFloat = new _cmsInterpFnFloat()
	{
		private float LERP(float a,float l,float h)
		{
			return ((l)+(((h)-(l))*(a)));
		}
		
		public void run(float[] Input, float[] Output, cmsInterpParams p)
		{
			final float[] LutTable = (float[])p.Table; 
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
		    
		    px = Input[0] * p.Domain[0];
		    py = Input[1] * p.Domain[1];
		    pz = Input[2] * p.Domain[2];
		    
		    x0 = (int)lcms2_internal._cmsQuickFloor(px); fx = px - x0;
		    y0 = (int)lcms2_internal._cmsQuickFloor(py); fy = py - y0;
		    z0 = (int)lcms2_internal._cmsQuickFloor(pz); fz = pz - z0;
		    
		    X0 = p.opta[2] * x0;
		    X1 = X0 + (Input[0] >= 1.0 ? 0 : p.opta[2]);
		    
		    Y0 = p.opta[1] * y0;
		    Y1 = Y0 + (Input[1] >= 1.0 ? 0 : p.opta[1]);
		    
		    Z0 = p.opta[0] * z0;
		    Z1 = Z0 + (Input[2] >= 1.0 ? 0 : p.opta[0]);
		    
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
	};
	
	// Trilinear interpolation (16 bits) - optimized version
	private static final _cmsInterpFn16 TrilinearInterp16 = new _cmsInterpFn16()
	{
		private int LERP(int a,int l,int h)
		{
			return (short)(l + lcms2_internal.ROUND_FIXED_TO_INT(((h-l)*a)));
		}
		
		public void run(short[] Input, short[] Output, cmsInterpParams p)
		{
			final short[] LutTable = (short[])p.Table;
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
			
			fx = lcms2_internal._cmsToFixedDomain((int)Input[0] * p.Domain[0]);
			x0 = lcms2_internal.FIXED_TO_INT(fx);
			rx = lcms2_internal.FIXED_REST_TO_INT(fx); // Rest in 0..1.0 domain
			
			fy = lcms2_internal._cmsToFixedDomain((int)Input[1] * p.Domain[1]);
			y0 = lcms2_internal.FIXED_TO_INT(fy);
			ry = lcms2_internal.FIXED_REST_TO_INT(fy);
			
			fz = lcms2_internal._cmsToFixedDomain((int)Input[2] * p.Domain[2]);
			z0 = lcms2_internal.FIXED_TO_INT(fz);
			rz = lcms2_internal.FIXED_REST_TO_INT(fz);
			
			X0 = p.opta[2] * x0;
			X1 = X0 + (Input[0] == (short)0xFFFF ? 0 : p.opta[2]);
			
			Y0 = p.opta[1] * y0;
			Y1 = Y0 + (Input[1] == (short)0xFFFF ? 0 : p.opta[1]);
			
			Z0 = p.opta[0] * z0;
			Z1 = Z0 + (Input[2] == (short)0xFFFF ? 0 : p.opta[0]);
			
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
	};
	
	// Tetrahedral interpolation, using Sakamoto algorithm.
	private static final _cmsInterpFnFloat TetrahedralInterpFloat = new _cmsInterpFnFloat()
	{
		public void run(float[] Input, float[] Output, cmsInterpParams p)
		{
			final float[] LutTable = (float[])p.Table; 
		    float      px, py, pz;
		    int        x0, y0, z0,
		               X0, Y0, Z0, X1, Y1, Z1;
		    float      rx, ry, rz;
		    float      c0, c1=0, c2=0, c3=0;
		    int        OutChan, TotalOut;
		    
		    TotalOut   = p.nOutputs;
		    
		    px = Input[0] * p.Domain[0];
		    py = Input[1] * p.Domain[1];
		    pz = Input[2] * p.Domain[2];
		    
		    x0 = (int)lcms2_internal._cmsQuickFloor(px); rx = (px - x0);
		    y0 = (int)lcms2_internal._cmsQuickFloor(py); ry = (py - y0);
		    z0 = (int)lcms2_internal._cmsQuickFloor(pz); rz = (pz - z0);
		    
		    X0 = p.opta[2] * x0;
		    X1 = X0 + (Input[0] >= 1.0 ? 0 : p.opta[2]);
		    
		    Y0 = p.opta[1] * y0;
		    Y1 = Y0 + (Input[1] >= 1.0 ? 0 : p.opta[1]);
		    
		    Z0 = p.opta[0] * z0;
		    Z1 = Z0 + (Input[2] >= 1.0 ? 0 : p.opta[0]);
		    
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
	};
	
	private static final _cmsInterpFn16 TetrahedralInterp16 = new _cmsInterpFn16()
	{
		public void run(short[] Input, short[] Output, cmsInterpParams p)
		{
			final short[] LutTable = (short[])p.Table;
		    int fx, fy, fz;
		    int rx, ry, rz;
		    int x0, y0, z0;
		    int c0, c1, c2, c3, Rest;       
		    int OutChan;
		    int X0, X1, Y0, Y1, Z0, Z1;
		    int TotalOut = p.nOutputs;
		    
		    fx  = lcms2_internal._cmsToFixedDomain((int) Input[0] * p.Domain[0]);
		    fy  = lcms2_internal._cmsToFixedDomain((int) Input[1] * p.Domain[1]);
		    fz  = lcms2_internal._cmsToFixedDomain((int) Input[2] * p.Domain[2]);
		    
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
		        
		        Output[OutChan] = (short)(((short)c0) + lcms2_internal.ROUND_FIXED_TO_INT(lcms2_internal._cmsToFixedDomain(Rest)));
		    }
		}
	};
	
	private static final _cmsInterpFn16 Eval4Inputs = new _cmsInterpFn16()
	{
		public void run(short[] Input, short[] Output, cmsInterpParams p16)
		{
			final short[] LutTable = (short[])p16.Table;
			int LutTablePos = 0;
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
		    
		    fk  = lcms2_internal._cmsToFixedDomain((int)Input[0] * p16.Domain[0]);
		    fx  = lcms2_internal._cmsToFixedDomain((int)Input[1] * p16.Domain[1]);
		    fy  = lcms2_internal._cmsToFixedDomain((int)Input[2] * p16.Domain[2]);
		    fz  = lcms2_internal._cmsToFixedDomain((int)Input[3] * p16.Domain[3]);
		    
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
		    
		    LutTablePos = K0;
		    
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
		        
		        Tmp1[OutChan] = (short)(((short)c0) + lcms2_internal.ROUND_FIXED_TO_INT(lcms2_internal._cmsToFixedDomain(Rest)));
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
		        
		        Tmp2[OutChan] = (short)(((short)c0) + lcms2_internal.ROUND_FIXED_TO_INT(lcms2_internal._cmsToFixedDomain(Rest)));
		    }
		    
		    for (i=0; i < p16.nOutputs; i++)
		    {
		        Output[i] = LinearInterp(rk, Tmp1[i], Tmp2[i]);              
		    }
		}
	};
	
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
			final float[] LutTable = (float[])p.Table;
			float rest;
			float pk;
			int k0, K0, K1;
			float[] T;
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
			
			int temp = LutTable.length - K0;
			T = new float[temp];
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
			final short[] LutTable = (short[])p16.Table;
			int fk;
			int k0, rk;
			int K0, K1;
			short[] T;
			int i;
			short[] Tmp1 = new short[lcms2_internal.MAX_STAGE_CHANNELS], Tmp2 = new short[lcms2_internal.MAX_STAGE_CHANNELS];
			cmsInterpParams p1;
			
			fk = lcms2_internal._cmsToFixedDomain(Input[0] * p16.Domain[0]);
			k0 = lcms2_internal.FIXED_TO_INT(fk);
			rk = lcms2_internal.FIXED_REST_TO_INT(fk);
			
			K0 = p16.opta[4] * k0;
			K1 = p16.opta[4] * (k0 + (Input[0] != (short)0xFFFF ? 1 : 0));
			
			p1 = dupParams(p16);
			System.arraycopy(p16.Domain, 1, p1.Domain, 0, 4);
			
			int temp = LutTable.length - K0;
			T = new short[temp];
			System.arraycopy(LutTable, K0, T, 0, temp);
			p1.Table = T;
			
			short[] tempInput = new short[3];
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
			
			for (i=0; i < p16.nOutputs; i++)
			{
				Output[i] = LinearInterp(rk, Tmp1[i], Tmp2[i]);
			}
		}
	};
	
	private static final _cmsInterpFnFloat Eval5InputsFloat = new _cmsInterpFnFloat()
	{
		public void run(float[] Input, float[] Output, cmsInterpParams p)
		{
			final float[] LutTable = (float[])p.Table;
			float rest;
			float pk;
			int k0, K0, K1;
			float[] T;
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
			
			int temp = LutTable.length - K0;
			T = new float[temp];
			System.arraycopy(LutTable, K0, T, 0, temp);
			p1.Table = T;
			
			float[] tempInput = new float[3];
			System.arraycopy(Input, 1, tempInput, 0, 3);
			Eval4InputsFloat.run(tempInput, Tmp1, p1);
			
			if(K0 != K1)
			{
				temp = LutTable.length - K1;
				T = new float[temp];
				System.arraycopy(LutTable, K1, T, 0, temp);
				p1.Table = T;
			}
			
			Eval4InputsFloat.run(tempInput, Tmp2, p1);
			
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
			final short[] LutTable = (short[])p16.Table;
			int fk;
			int k0, rk;
			int K0, K1;
			short[] T;
			int i;
			short[] Tmp1 = new short[lcms2_internal.MAX_STAGE_CHANNELS], Tmp2 = new short[lcms2_internal.MAX_STAGE_CHANNELS];
			cmsInterpParams p1;
			
			fk = lcms2_internal._cmsToFixedDomain(Input[0] * p16.Domain[0]);
			k0 = lcms2_internal.FIXED_TO_INT(fk);
			rk = lcms2_internal.FIXED_REST_TO_INT(fk);
			
			K0 = p16.opta[5] * k0;
			K1 = p16.opta[5] * (k0 + (Input[0] != (short)0xFFFF ? 1 : 0));
			
			p1 = dupParams(p16);
			System.arraycopy(p16.Domain, 1, p1.Domain, 0, 5);
			
			int temp = LutTable.length - K0;
			T = new short[temp];
			System.arraycopy(LutTable, K0, T, 0, temp);
			p1.Table = T;
			
			short[] tempInput = new short[3];
			System.arraycopy(Input, 1, tempInput, 0, 4);
			Eval5Inputs.run(tempInput, Tmp1, p1);
			
			if(K0 != K1)
			{
				temp = LutTable.length - K1;
				T = new short[temp];
				System.arraycopy(LutTable, K1, T, 0, temp);
				p1.Table = T;
			}
			
			Eval5Inputs.run(tempInput, Tmp2, p1);
			
			for (i=0; i < p16.nOutputs; i++)
			{
				Output[i] = LinearInterp(rk, Tmp1[i], Tmp2[i]);
			}
		}
	};
	
	private static final _cmsInterpFnFloat Eval6InputsFloat = new _cmsInterpFnFloat()
	{
		public void run(float[] Input, float[] Output, cmsInterpParams p)
		{
			final float[] LutTable = (float[])p.Table;
			float rest;
			float pk;
			int k0, K0, K1;
			float[] T;
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
			
			int temp = LutTable.length - K0;
			T = new float[temp];
			System.arraycopy(LutTable, K0, T, 0, temp);
			p1.Table = T;
			
			float[] tempInput = new float[3];
			System.arraycopy(Input, 1, tempInput, 0, 3);
			Eval5InputsFloat.run(tempInput, Tmp1, p1);
			
			if(K0 != K1)
			{
				temp = LutTable.length - K1;
				T = new float[temp];
				System.arraycopy(LutTable, K1, T, 0, temp);
				p1.Table = T;
			}
			
			Eval5InputsFloat.run(tempInput, Tmp2, p1);
			
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
			final short[] LutTable = (short[])p16.Table;
			int fk;
			int k0, rk;
			int K0, K1;
			short[] T;
			int i;
			short[] Tmp1 = new short[lcms2_internal.MAX_STAGE_CHANNELS], Tmp2 = new short[lcms2_internal.MAX_STAGE_CHANNELS];
			cmsInterpParams p1;
			
			fk = lcms2_internal._cmsToFixedDomain(Input[0] * p16.Domain[0]);
			k0 = lcms2_internal.FIXED_TO_INT(fk);
			rk = lcms2_internal.FIXED_REST_TO_INT(fk);
			
			K0 = p16.opta[6] * k0;
			K1 = p16.opta[6] * (k0 + (Input[0] != (short)0xFFFF ? 1 : 0));
			
			p1 = dupParams(p16);
			System.arraycopy(p16.Domain, 1, p1.Domain, 0, 6);
			
			int temp = LutTable.length - K0;
			T = new short[temp];
			System.arraycopy(LutTable, K0, T, 0, temp);
			p1.Table = T;
			
			short[] tempInput = new short[3];
			System.arraycopy(Input, 1, tempInput, 0, 4);
			Eval6Inputs.run(tempInput, Tmp1, p1);
			
			if(K0 != K1)
			{
				temp = LutTable.length - K1;
				T = new short[temp];
				System.arraycopy(LutTable, K1, T, 0, temp);
				p1.Table = T;
			}
			
			Eval6Inputs.run(tempInput, Tmp2, p1);
			
			for (i=0; i < p16.nOutputs; i++)
			{
				Output[i] = LinearInterp(rk, Tmp1[i], Tmp2[i]);
			}
		}
	};
	
	private static final _cmsInterpFnFloat Eval7InputsFloat = new _cmsInterpFnFloat()
	{
		public void run(float[] Input, float[] Output, cmsInterpParams p)
		{
			final float[] LutTable = (float[])p.Table;
			float rest;
			float pk;
			int k0, K0, K1;
			float[] T;
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
			
			int temp = LutTable.length - K0;
			T = new float[temp];
			System.arraycopy(LutTable, K0, T, 0, temp);
			p1.Table = T;
			
			float[] tempInput = new float[3];
			System.arraycopy(Input, 1, tempInput, 0, 3);
			Eval6InputsFloat.run(tempInput, Tmp1, p1);
			
			if(K0 != K1)
			{
				temp = LutTable.length - K1;
				T = new float[temp];
				System.arraycopy(LutTable, K1, T, 0, temp);
				p1.Table = T;
			}
			
			Eval6InputsFloat.run(tempInput, Tmp2, p1);
			
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
			final short[] LutTable = (short[])p16.Table;
			int fk;
			int k0, rk;
			int K0, K1;
			short[] T;
			int i;
			short[] Tmp1 = new short[lcms2_internal.MAX_STAGE_CHANNELS], Tmp2 = new short[lcms2_internal.MAX_STAGE_CHANNELS];
			cmsInterpParams p1;
			
			fk = lcms2_internal._cmsToFixedDomain(Input[0] * p16.Domain[0]);
			k0 = lcms2_internal.FIXED_TO_INT(fk);
			rk = lcms2_internal.FIXED_REST_TO_INT(fk);
			
			K0 = p16.opta[7] * k0;
			K1 = p16.opta[7] * (k0 + (Input[0] != (short)0xFFFF ? 1 : 0));
			
			p1 = dupParams(p16);
			System.arraycopy(p16.Domain, 1, p1.Domain, 0, 7);
			
			int temp = LutTable.length - K0;
			T = new short[temp];
			System.arraycopy(LutTable, K0, T, 0, temp);
			p1.Table = T;
			
			short[] tempInput = new short[3];
			System.arraycopy(Input, 1, tempInput, 0, 4);
			Eval7Inputs.run(tempInput, Tmp1, p1);
			
			if(K0 != K1)
			{
				temp = LutTable.length - K1;
				T = new short[temp];
				System.arraycopy(LutTable, K1, T, 0, temp);
				p1.Table = T;
			}
			
			Eval7Inputs.run(tempInput, Tmp2, p1);
			
			for (i=0; i < p16.nOutputs; i++)
			{
				Output[i] = LinearInterp(rk, Tmp1[i], Tmp2[i]);
			}
		}
	};
	
	private static final _cmsInterpFnFloat Eval8InputsFloat = new _cmsInterpFnFloat()
	{
		public void run(float[] Input, float[] Output, cmsInterpParams p)
		{
			final float[] LutTable = (float[])p.Table;
			float rest;
			float pk;
			int k0, K0, K1;
			float[] T;
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
			
			int temp = LutTable.length - K0;
			T = new float[temp];
			System.arraycopy(LutTable, K0, T, 0, temp);
			p1.Table = T;
			
			float[] tempInput = new float[3];
			System.arraycopy(Input, 1, tempInput, 0, 3);
			Eval7InputsFloat.run(tempInput, Tmp1, p1);
			
			if(K0 != K1)
			{
				temp = LutTable.length - K1;
				T = new float[temp];
				System.arraycopy(LutTable, K1, T, 0, temp);
				p1.Table = T;
			}
			
			Eval7InputsFloat.run(tempInput, Tmp2, p1);
			
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
	private static cmsInterpFnFactory Interpolators = DefaultInterpolatorsFactory;
}
