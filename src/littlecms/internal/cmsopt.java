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
import littlecms.internal.lcms2.cmsPipeline;
import littlecms.internal.lcms2.cmsSAMPLER16;
import littlecms.internal.lcms2.cmsStage;
import littlecms.internal.lcms2.cmsToneCurve;
import littlecms.internal.lcms2_plugin._cmsInterpFn16;
import littlecms.internal.lcms2_plugin._cmsDupUserDataFn;
import littlecms.internal.lcms2_plugin._cmsOPTeval16Fn;
import littlecms.internal.lcms2_plugin._cmsFreeUserDataFn;
import littlecms.internal.lcms2_plugin._cmsOPToptimizeFn;
import littlecms.internal.lcms2_plugin._cmsStageCLutData;
import littlecms.internal.lcms2_plugin._cmsStageMatrixData;
import littlecms.internal.lcms2_plugin._cmsStageToneCurvesData;
import littlecms.internal.lcms2_plugin.cmsInterpParams;
import littlecms.internal.lcms2_plugin.cmsMAT3;
import littlecms.internal.lcms2_plugin.cmsPluginBase;
import littlecms.internal.lcms2_plugin.cmsPluginOptimization;
import littlecms.internal.lcms2_plugin.cmsVEC3;
import littlecms.internal.helper.Utility;
import littlecms.internal.helper.VirtualPointer;

//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmsopt
{
	// Optimization for 8 bits, Shaper-CLUT (3 inputs only)
	private static class Prelin8Data
	{
		public cmsContext ContextID;
		
		public cmsInterpParams p; // Tetrahedrical interpolation parameters. This is a not-owned pointer.
	    
		public short[] rx, ry, rz;
		public int[] X0, Y0, Z0; // Precomputed nodes and offsets for 8-bit input data
		
		public Prelin8Data()
		{
			rx = new short[256];
			ry = new short[256];
			rz = new short[256];
			X0 = new int[256];
			Y0 = new int[256];
			Z0 = new int[256];
		}
	}
	
	// Generic optimization for 16 bits Shaper-CLUT-Shaper (any inputs)
	private static class Prelin16Data
	{
		public cmsContext ContextID;
		
	    // Number of channels
		public int nInputs;
		public int nOutputs;
	    
	    // Since there is no limitation of the output number of channels, this buffer holding the connexion CLUT-shaper
	    // has to be dynamically allocated. This is not the case of first step shaper-CLUT, which is limited to max inputs
		public short[] StageDEF;
	    
		public _cmsInterpFn16[] EvalCurveIn16; // The maximum number of input channels is known in advance
		public cmsInterpParams[] ParamsCurveIn16;
	    
		public _cmsInterpFn16 EvalCLUT; // The evaluator for 3D grid
		public cmsInterpParams CLUTparams; // (not-owned pointer)
	    
		public _cmsInterpFn16[] EvalCurveOut16; // Points to an array of curve evaluators in 16 bits (not-owned pointer)
		public cmsInterpParams[]  ParamsCurveOut16; // Points to an array of references to interpolation params (not-owned pointer)
		
		public Prelin16Data()
		{
			EvalCurveIn16 = new _cmsInterpFn16[lcms2_plugin.MAX_INPUT_DIMENSIONS];
			ParamsCurveIn16 = new cmsInterpParams[lcms2_plugin.MAX_INPUT_DIMENSIONS];
		}
	}
	
	// Optimization for matrix-shaper in 8 bits. Numbers are operated in n.14 signed, tables are stored in 1.14 fixed
	
	private static int DOUBLE_TO_1FIXED14(double x)
	{
		return ((int)Math.floor((x) * 16384.0 + 0.5));
	}
	
	private static class MatShaper8Data
	{
		public cmsContext ContextID;
		
		public int[] Shaper1R; // from 0..255 to 1.14  (0.0...1.0)
		public int[] Shaper1G;
		public int[] Shaper1B;
	    
		public int[][] Mat; // n.14 to n.14 (needs a saturation after that)
		public int[] Off;
	    
		public short[] Shaper2R; // 1.14 to 0..255
		public short[] Shaper2G;
		public short[] Shaper2B;
		
		public MatShaper8Data()
		{
			Shaper1R = new int[256];
			Shaper1G = new int[256];
			Shaper1B = new int[256];
			
			Mat = new int[3][3];
			Off = new int[3];
			
			Shaper2R = new short[16385];
			Shaper2G = new short[16385];
			Shaper2B = new short[16385];
		}
	}
	
	// Curves, optimization is shared between 8 and 16 bits
	
	private static class Curves16Data
	{
		public cmsContext ContextID;
		
		public int nCurves;                  // Number of curves
	    public int nElements;                // Elements in curves
	    public short[][] Curves;     // Points to a dynamically allocated array
	}
	
	// Simple optimizations ----------------------------------------------------------------------------------------------------------
	
	//NOTE: The following 3 functions are modified from the original code so that _RemoveElement actually removes the element. Instead of removing "the" element,
	//it removes the "next" element so that references can be maintained.
	
	private static class ChainHolder
	{
		public ChainHolder next;
		public cmsStage cur;
		public ChainHolder prev;
		
		public static ChainHolder MakeChain(cmsStage firstStage)
		{
			ChainHolder root;
			ChainHolder chain = root = new ChainHolder();
			chain.prev = null;
			chain.cur = firstStage;
			while(firstStage != null && firstStage.Next != null)
			{
				ChainHolder t = new ChainHolder();
				firstStage = t.cur = (cmsStage)firstStage.Next;
				t.prev = chain;
				chain.next = t;
				chain = t;
			}
			return root;
		}
		
		public cmsStage First()
		{
			if(prev != null)
			{
				return prev.First();
			}
			return cur;
		}
	}
	
	// Remove an element in linked chain
	private static void _RemoveElement(ChainHolder head)
	{
		cmsStage mpe = head.cur;
		head.cur = null;
		
		//Remove current element, replace with next element
		if(head.next != null)
		{
			head.cur = head.next.cur;
			head.next = head.next.next;
		}
		//Make sure previous element (both the actual element and chain holder element) is modified for next node (not current node)
		if(head.prev != null)
		{
			head.prev.cur.Next = head.cur;
			if(head.prev.next.cur == null)
			{
				head.prev.next = null;
			}
		}
		
		cmslut.cmsStageFree(mpe);
	}
	
	// Remove all identities in chain. Note that pt actually is a double pointer to the element that holds the pointer.
	private static boolean _Remove1Op(cmsPipeline Lut, int UnaryOp)
	{
		ChainHolder pt = ChainHolder.MakeChain(Lut.Elements);
		boolean AnyOpt = false;
		
		while(pt.cur != null)
		{
			if(pt.cur.Implements == UnaryOp)
			{
				_RemoveElement(pt);
				AnyOpt = true;
			}
			else
			{
				if(pt.next == null)
				{
					break;
				}
				pt = pt.next;
			}
		}
		
		if(AnyOpt)
		{
			Lut.Elements = pt.First();
		}
		
		return AnyOpt;
	}
	
	// Same, but only if two adjacent elements are found
	private static boolean _Remove2Op(cmsPipeline Lut, int Op1, int Op2)
	{
		ChainHolder pt1;
		ChainHolder pt2;
		boolean AnyOpt = false;
		
		pt1 = ChainHolder.MakeChain(Lut.Elements);
		if(pt1.cur == null)
		{
			return AnyOpt;
		}
		
		while(pt1.cur != null)
		{
			pt2 = pt1.next;
			if(pt2 == null || pt2.cur == null)
			{
				break;
			}
			
			if(pt1.cur.Implements == Op1 && pt2.cur.Implements == Op2)
			{
				_RemoveElement(pt2);
	            _RemoveElement(pt1);
	            AnyOpt = true;
			}
			else
			{
				if(pt1.next == null)
				{
					break;
				}
				pt1 = pt1.next;
			}
		}
		
		if(AnyOpt)
		{
			Lut.Elements = pt1.First();
		}
		
		return AnyOpt;
	}
	
	// Preoptimize just gets rif of no-ops coming paired. Conversion from v2 to v4 followed
	// by a v4 to v2 and vice-versa. The elements are then discarded.
	private static boolean PreOptimize(cmsPipeline Lut)
	{
	    boolean AnyOpt = false, Opt;
	    
	    AnyOpt = false;
	    
	    do
	    {
	        Opt = false;
	        
	        // Remove all identities
	        Opt |= _Remove1Op(Lut, lcms2.cmsSigIdentityElemType);
	        
	        // Remove XYZ2Lab followed by Lab2XYZ
	        Opt |= _Remove2Op(Lut, lcms2.cmsSigXYZ2LabElemType, lcms2.cmsSigLab2XYZElemType);
	        
	        // Remove Lab2XYZ followed by XYZ2Lab
	        Opt |= _Remove2Op(Lut, lcms2.cmsSigLab2XYZElemType, lcms2.cmsSigXYZ2LabElemType);
	        
	        // Remove V4 to V2 followed by V2 to V4
	        Opt |= _Remove2Op(Lut, lcms2.cmsSigLabV4toV2, lcms2.cmsSigLabV2toV4);
	        
	        // Remove V2 to V4 followed by V4 to V2
	        Opt |= _Remove2Op(Lut, lcms2.cmsSigLabV2toV4, lcms2.cmsSigLabV4toV2);
	        
	        if (Opt)
	        {
	        	AnyOpt = true;
	        }
	        
	    } while (Opt);
	    
	    return AnyOpt;
	}
	
	private static final _cmsInterpFn16 Eval16nop1D = new _cmsInterpFn16()
	{
		public void run(short[] Input, short[] Output, cmsInterpParams p)
		{
			Output[0] = Input[0];
		}
	};
	
	private static final _cmsOPTeval16Fn PrelinEval16 = new _cmsOPTeval16Fn()
	{
		public void run(short[] Input, short[] Output, final Object D)
		{
			Prelin16Data p16 = (Prelin16Data)D;
		    short[] StageABC = new short[lcms2_plugin.MAX_INPUT_DIMENSIONS];
		    int i;
		    
		    short[] tempIn = new short[1];
		    short[] tempOut = new short[1];
		    for (i = 0; i < p16.nInputs; i++)
		    {
		    	tempIn[0] = Input[i];
		        p16.EvalCurveIn16[i].run(tempIn, tempOut, p16.ParamsCurveIn16[i]);
		        StageABC[i] = tempOut[0];
		    }
		    
		    p16.EvalCLUT.run(StageABC, p16.StageDEF, p16.CLUTparams);
		    
		    for (i = 0; i < p16.nOutputs; i++)
		    {
		    	tempIn[0] = p16.StageDEF[i];
		        p16.EvalCurveOut16[i].run(tempIn, tempOut, p16.ParamsCurveOut16[i]);
		        Output[i] = tempOut[0];
		    }
		}
	};
	
	private static final _cmsFreeUserDataFn PrelinOpt16free = new _cmsFreeUserDataFn()
	{
		public void run(cmsContext ContextID, Object ptr)
		{
			Prelin16Data p16 = (Prelin16Data)ptr;
			
			/*
		    cmserr._cmsFree(ContextID, p16.StageDEF);
		    cmserr._cmsFree(ContextID, p16.EvalCurveOut16);
		    cmserr._cmsFree(ContextID, p16.ParamsCurveOut16);
		    
		    cmserr._cmsFree(ContextID, p16);
		    */
			p16.StageDEF = null;
			p16.EvalCurveOut16 = null;
			p16.ParamsCurveOut16 = null;
		}
	};
	
	private static final _cmsDupUserDataFn Prelin16dup = new _cmsDupUserDataFn()
	{
		public Object run(cmsContext ContextID, final Object ptr)
		{
			Prelin16Data p16 = (Prelin16Data)ptr;
		    Prelin16Data Duped = new Prelin16Data();
		    
		    Duped.StageDEF = new short[p16.nOutputs];
		    Duped.EvalCurveOut16 = new _cmsInterpFn16[p16.nOutputs];
		    System.arraycopy(p16.EvalCurveOut16, 0, Duped.EvalCurveOut16, 0, p16.nOutputs);
		    Duped.ParamsCurveOut16 = new cmsInterpParams[p16.nOutputs];
		    System.arraycopy(p16.ParamsCurveOut16, 0, Duped.ParamsCurveOut16, 0, p16.nOutputs);
		    
		    return Duped;
		}
	};
	
	private static Prelin16Data PrelinOpt16alloc(cmsContext ContextID, final cmsInterpParams ColorMap, int nInputs, cmsToneCurve[] In, int nOutputs, cmsToneCurve[] Out)
	{
	    int i;
	    Prelin16Data p16 = new Prelin16Data();
	    
	    p16.nInputs = nInputs;
	    p16.nOutputs = nOutputs;
	    
	    for (i = 0; i < nInputs; i++)
	    {
	        if (In == null)
	        {
	            p16.ParamsCurveIn16[i] = null;
	            p16.EvalCurveIn16[i] = Eval16nop1D;
	        }
	        else
	        {
	            p16.ParamsCurveIn16[i] = In[i].InterpParams;
	            p16.EvalCurveIn16[i] = p16.ParamsCurveIn16[i].Interpolation.get16();
	        }
	    }
	    
	    p16.CLUTparams = ColorMap;
	    p16.EvalCLUT   = ColorMap.Interpolation.get16();
	    
	    p16.StageDEF = new short[p16.nOutputs];
	    p16.EvalCurveOut16 = new _cmsInterpFn16[nOutputs];
	    p16.ParamsCurveOut16 = new cmsInterpParams[nOutputs];
	    
	    for (i = 0; i < nOutputs; i++)
	    {
	        if (Out == null)
	        {
	            p16.ParamsCurveOut16[i] = null;
	            p16.EvalCurveOut16[i] = Eval16nop1D;
	        }
	        else
	        {
	            p16.ParamsCurveOut16[i] = Out[i].InterpParams;
	            p16.EvalCurveOut16[i] = p16.ParamsCurveOut16[i].Interpolation.get16();
	        }
	    }

	    return p16;
	}
	
	// Resampling ---------------------------------------------------------------------------------
	
	private static final int PRELINEARIZATION_POINTS = 4096;
	
	// Sampler implemented by another LUT. This is a clean way to precalculate the devicelink 3D CLUT for
	// almost any transform. We use floating point precision and then convert from floating point to 16 bits.
	private static final cmsSAMPLER16 XFormSampler16 = new cmsSAMPLER16()
	{
		public int run(short[] In, short[] Out, Object Cargo)
		{
			cmsPipeline Lut = (cmsPipeline)Cargo;
		    float[] InFloat = new float[lcms2.cmsMAXCHANNELS], OutFloat = new float[lcms2.cmsMAXCHANNELS];
		    int i;
		    
		    lcms2_internal._cmsAssert(Lut.InputChannels < lcms2.cmsMAXCHANNELS, "Lut.InputChannels < lcms2.cmsMAXCHANNELS");
		    lcms2_internal._cmsAssert(Lut.OutputChannels < lcms2.cmsMAXCHANNELS, "Lut.OutputChannels < lcms2.cmsMAXCHANNELS");
		    
		    // From 16 bit to floating point
		    for (i = 0; i < Lut.InputChannels; i++)
		    {
		    	InFloat[i] = ((In[i] & 0xFFFF) / 65535f);
		    }
		    
		    // Evaluate in floating point
		    cmslut.cmsPipelineEvalFloat(InFloat, OutFloat, Lut);
		    
		    // Back to 16 bits representation
		    for (i = 0; i < Lut.OutputChannels; i++)
		    {
		    	Out[i] = lcms2_internal._cmsQuickSaturateWord(OutFloat[i] * 65535.0);
		    }
		    
		    // Always succeed
		    return 1;
		}
	};
	
	// Try to see if the curves of a given MPE are linear
	private static boolean AllCurvesAreLinear(cmsStage mpe)
	{
	    cmsToneCurve[] Curves;
	    int i, n;
	    
	    Curves = cmslut._cmsStageGetPtrToCurveSet(mpe);
	    if (Curves == null)
	    {
	    	return false;
	    }
	    
	    n = cmslut.cmsStageOutputChannels(mpe);
	    
	    for (i = 0; i < n; i++)
	    {
	        if (!cmsgamma.cmsIsToneCurveLinear(Curves[i]))
	        {
	        	return false;
	        }
	    }
	    
	    return true;
	}
	
	// This function replaces a specific node placed in "At" by the "Value" numbers. Its purpose
	// is to fix scum dot on broken profiles/transforms. Works on 1, 3 and 4 channels
	private static boolean PatchLUT(cmsStage CLUT, short[] At, short[] Value, int nChannelsOut, int nChannelsIn)
	{
	    _cmsStageCLutData Grid = (_cmsStageCLutData)CLUT.Data;
	    cmsInterpParams p16  = Grid.Params;
	    double px, py, pz, pw;
	    int x0, y0, z0, w0;
	    int i, index;
	    
	    if (CLUT.Type != lcms2.cmsSigCLutElemType)
	    {
	        cmserr.cmsSignalError(CLUT.ContextID, lcms2.cmsERROR_INTERNAL, Utility.LCMS_Resources.getString(LCMSResource.CMSOPT_PATCHLUT_ON_NONLUT), null);
	        return false;
	    }
	    
	    px = ((double)At[0] * (p16.Domain[0])) / 65535.0;
	    py = ((double)At[1] * (p16.Domain[1])) / 65535.0;
	    pz = ((double)At[2] * (p16.Domain[2])) / 65535.0;
	    pw = ((double)At[3] * (p16.Domain[3])) / 65535.0;
	    
	    x0 = (int)Math.floor(px);
	    y0 = (int)Math.floor(py);
	    z0 = (int)Math.floor(pz);
	    w0 = (int)Math.floor(pw);
	    
	    if (nChannelsIn == 4)
	    {
	        if (((px - x0) != 0) ||
	            ((py - y0) != 0) ||
	            ((pz - z0) != 0) ||
	            ((pw - w0) != 0))
	        {
	        	return false; // Not on exact node
	        }
	        
	        index = p16.opta[3] * x0 +
	            p16.opta[2] * y0 +
	            p16.opta[1] * z0 +
	            p16.opta[0] * w0;
	    }
	    else
	    {
	        if (nChannelsIn == 3)
	        {
	            if (((px - x0) != 0) ||
	                ((py - y0) != 0) ||
	                ((pz - z0) != 0))
	            {
	            	return false; // Not on exact node
	            }
	            
	            index = p16.opta[2] * x0 +
	                p16.opta[1] * y0 +
	                p16.opta[0] * z0;
	        }
	        else
	        {
	            if (nChannelsIn == 1)
	            {
	                if (((px - x0) != 0))
	                {
	                	return false; // Not on exact node
	                }
	                
	                index = p16.opta[0] * x0;
	            }
	            else
	            {
	                cmserr.cmsSignalError(CLUT.ContextID, lcms2.cmsERROR_INTERNAL, Utility.LCMS_Resources.getString(LCMSResource.CMSOPT_NUM_CHANNELS_NOT_SUPPORT_PATCHLUT), new Object[]{new Integer(nChannelsIn)});
	                return false;
	            }
	        }
	    }
	    
		VirtualPointer.TypeProcessor proc = Grid.Tab.getProcessor();
		int pos = Grid.Tab.getPosition();
        for (i = 0; i < nChannelsOut; i++)
        {
        	Grid.Tab.setPosition((index + i) * 2);
        	proc.write(Value[i]);
        }
        Grid.Tab.setPosition(pos);
        
        return true;
	}
	
	// Auxiliar, to see if two values are equal or very different
	private static boolean WhitesAreEqual(int n, short[] White1, short[] White2)
	{
	    int i;
	    
	    for (i = 0; i < n; i++)
	    {
	    	if (Math.abs((White1[i] & 0xFFFF) - (White2[i] & 0xFFFF)) > 0xf000)
	    	{
	    		return true; // Values are so extremely different that the fixup should be avoided
	    	}
	        if (White1[i] != White2[i])
	        {
	        	return false;
	        }
	    }
	    return true;
	}
	
	// Locate the node for the white point and fix it to pure white in order to avoid scum dot.
	private static boolean FixWhiteMisalignment(cmsPipeline Lut, int EntryColorSpace, int ExitColorSpace)
	{
		short[] WhitePointIn, WhitePointOut;
	    short[] WhiteIn = new short[lcms2.cmsMAXCHANNELS], WhiteOut = new short[lcms2.cmsMAXCHANNELS], ObtainedOut = new short[lcms2.cmsMAXCHANNELS];
	    int i, nOuts, nIns;
	    int[] temp = new int[1];
	    cmsStage PreLin = null, CLUT = null, PostLin = null;
	    
	    if (!cmspcs._cmsEndPointsBySpace(EntryColorSpace, null, null, temp))
	    {
	    	return false;
	    }
	    WhitePointIn = new short[nIns = temp[0]];
	    if (!cmspcs._cmsEndPointsBySpace(EntryColorSpace, WhitePointIn, null, temp))
	    {
	    	return false;
	    }
	    if(nIns != temp[0])
	    {
	    	return false;
	    }
	    
	    if (!cmspcs._cmsEndPointsBySpace(ExitColorSpace, null, null, temp))
	    {
	    	return false;
	    }
	    WhitePointOut = new short[nOuts = temp[0]];
	    if (!cmspcs._cmsEndPointsBySpace(ExitColorSpace, WhitePointOut, null, temp))
	    {
	    	return false;
	    }
	    if(nOuts != temp[0])
	    {
	    	return false;
	    }
	    
	    // It needs to be fixed?
	    if (Lut.InputChannels != nIns)
	    {
	    	return false;
	    }
	    if (Lut.OutputChannels != nOuts)
	    {
	    	return false;
	    }
	    
	    cmslut.cmsPipelineEval16(WhitePointIn, ObtainedOut, Lut);
	    
	    if (WhitesAreEqual(nOuts, WhitePointOut, ObtainedOut))
	    {
	    	return true; // whites already match
	    }
	    
	    // Check if the LUT comes as Prelin, CLUT or Postlin. We allow all combinations
	    Object[] args = new Object[3 * 2];
	    args[0] = new Integer(lcms2.cmsSigCurveSetElemType);
	    args[1] = new Integer(lcms2.cmsSigCLutElemType);
	    args[2] = new Integer(lcms2.cmsSigCurveSetElemType);
	    if (!cmslut.cmsPipelineCheckAndRetreiveStages(Lut, 3, args))
	    {
	    	//args[0] = new Integer(lcms2.cmsSigCurveSetElemType);
        	//args[1] = new Integer(lcms2.cmsSigCLutElemType);
	        if (!cmslut.cmsPipelineCheckAndRetreiveStages(Lut, 2, args))
	        {
	        	args[0] = new Integer(lcms2.cmsSigCLutElemType);
	        	args[1] = new Integer(lcms2.cmsSigCurveSetElemType);
	            if (!cmslut.cmsPipelineCheckAndRetreiveStages(Lut, 2, args))
	            {
	            	//args[0] = new Integer(lcms2.cmsSigCLutElemType);
	                if (!cmslut.cmsPipelineCheckAndRetreiveStages(Lut, 1, args))
	                {
	                    return false;
	                }
	                else
	                {
	                	CLUT = (cmsStage)args[1];
	                }
	            }
	            else
		        {
	            	CLUT = (cmsStage)args[2];
	            	PostLin = (cmsStage)args[3];
		        }
	        }
	        else
	        {
	        	PreLin = (cmsStage)args[2];
		    	CLUT = (cmsStage)args[3];
	        }
	    }
	    else
	    {
	    	PreLin = (cmsStage)args[3];
	    	CLUT = (cmsStage)args[4];
	    	PostLin = (cmsStage)args[5];
	    }

	    // We need to interpolate white points of both, pre and post curves
	    if (PreLin != null)
	    {
	        cmsToneCurve[] Curves = cmslut._cmsStageGetPtrToCurveSet(PreLin);
	        
	        for (i=0; i < nIns; i++)
	        {
	            WhiteIn[i] = cmsgamma.cmsEvalToneCurve16(Curves[i], WhitePointIn[i]);
	        }
	    }
	    else
	    {
	        for (i=0; i < nIns; i++)
	        {
	        	WhiteIn[i] = WhitePointIn[i];
	        }
	    }
	    
	    // If any post-linearization, we need to find how is represented white before the curve, do
	    // a reverse interpolation in this case.
	    if (PostLin != null)
	    {
	        cmsToneCurve[] Curves = cmslut._cmsStageGetPtrToCurveSet(PostLin);
	        
	        for (i=0; i < nOuts; i++)
	        {
	            cmsToneCurve InversePostLin = cmsgamma.cmsReverseToneCurve(Curves[i]);
	            WhiteOut[i] = cmsgamma.cmsEvalToneCurve16(InversePostLin, WhitePointOut[i]);
	            cmsgamma.cmsFreeToneCurve(InversePostLin);
	        }
	    }
	    else
	    {
	        for (i=0; i < nOuts; i++)
	        {
	        	WhiteOut[i] = WhitePointOut[i];
	        }
	    }
	    
	    // Ok, proceed with patching. May fail and we don't care if it fails
	    PatchLUT(CLUT, WhiteIn, WhiteOut, nOuts, nIns);
	    
	    return true;
	}
	
	// -----------------------------------------------------------------------------------------------------------------------------------------------
	// This function creates simple LUT from complex ones. The generated LUT has an optional set of
	// prelinearization curves, a CLUT of nGridPoints and optional postlinearization tables.
	// These curves have to exist in the original LUT in order to be used in the simplified output.
	// Caller may also use the flags to allow this feature.
	// LUTS with all curves will be simplified to a single curve. Parametric curves are lost.
	// This function should be used on 16-bits LUTS only, as floating point losses precision when simplified
	// -----------------------------------------------------------------------------------------------------------------------------------------------
	
	private static final _cmsOPToptimizeFn OptimizeByResampling = new _cmsOPToptimizeFn()
	{
		public boolean run(cmsPipeline[] Lut, int Intent, int[] InputFormat, int[] OutputFormat, int[] dwFlags)
		{
			cmsPipeline Src;
		    cmsPipeline Dest;
		    cmsStage mpe;
		    cmsStage CLUT;
		    cmsStage KeepPreLin = null, KeepPostLin = null;
		    cmsStage[] temp = new cmsStage[1];
		    int nGridPoints;
		    int ColorSpace, OutputColorSpace;
		    cmsStage NewPreLin = null;
		    cmsStage NewPostLin = null;
		    _cmsStageCLutData DataCLUT;
		    cmsToneCurve[] DataSetIn;
		    cmsToneCurve[] DataSetOut;
		    Prelin16Data p16;
		    
		    // This is a loosy optimization! does not apply in floating-point cases
		    if (cmspack._cmsFormatterIsFloat(InputFormat[0]) || cmspack._cmsFormatterIsFloat(OutputFormat[0]))
		    {
		    	return false;
		    }
		    
		    ColorSpace       = cmspcs._cmsICCcolorSpace(lcms2.T_COLORSPACE(InputFormat[0]));
		    OutputColorSpace = cmspcs._cmsICCcolorSpace(lcms2.T_COLORSPACE(OutputFormat[0]));
		    nGridPoints      = cmspcs._cmsReasonableGridpointsByColorspace(ColorSpace, dwFlags[0]);
		    
		    // For empty LUTs, 2 points are enough
		    if (cmslut.cmsPipelineStageCount(Lut[0]) == 0)
		    {
		    	nGridPoints = 2;
		    }
		    
		    Src = Lut[0];
		    
		    // Named color pipelines cannot be optimized either
		    for (mpe = cmslut.cmsPipelineGetPtrToFirstStage(Src);
		    		mpe != null;
		    		mpe = cmslut.cmsStageNext(mpe))
		    {
		    	if (cmslut.cmsStageType(mpe) == lcms2.cmsSigNamedColorElemType)
		    	{
		    		return false;
		    	}
		    }
		    
		    // Allocate an empty LUT
		    Dest =  cmslut.cmsPipelineAlloc(Src.ContextID, Src.InputChannels, Src.OutputChannels);
		    if (Dest == null)
		    {
		    	return false;
		    }
		    
		    // Prelinearization tables are kept unless indicated by flags
		    if ((dwFlags[0] & lcms2.cmsFLAGS_CLUT_PRE_LINEARIZATION) != 0)
		    {
		        // Get a pointer to the prelinearization element
		        cmsStage PreLin = cmslut.cmsPipelineGetPtrToFirstStage(Src);
		        
		        // Check if suitable
		        if (PreLin.Type == lcms2.cmsSigCurveSetElemType)
		        {
		            // Maybe this is a linear tram, so we can avoid the whole stuff
		            if (!AllCurvesAreLinear(PreLin))
		            {
		                // All seems ok, proceed.
		                NewPreLin = cmslut.cmsStageDup(PreLin);
		                cmslut.cmsPipelineInsertStage(Dest, lcms2.cmsAT_BEGIN, NewPreLin);
		                
		                // Remove prelinearization. Since we have duplicated the curve
		                // in destination LUT, the sampling shoud be applied after this stage.
		                temp[0] = KeepPreLin;
		                cmslut.cmsPipelineUnlinkStage(Src, lcms2.cmsAT_BEGIN, temp);
		                KeepPreLin = temp[0];
		            }
		        }
		    }
		    
		    // Allocate the CLUT
		    CLUT = cmslut.cmsStageAllocCLut16bit(Src.ContextID, nGridPoints, Src.InputChannels, Src.OutputChannels, null);
		    if (CLUT == null)
		    {
		    	return false;
		    }
		    
		    // Add the CLUT to the destination LUT
		    cmslut.cmsPipelineInsertStage(Dest, lcms2.cmsAT_END, CLUT);
		    
		    // Postlinearization tables are kept unless indicated by flags
		    if ((dwFlags[0] & lcms2.cmsFLAGS_CLUT_POST_LINEARIZATION) != 0)
		    {
		        // Get a pointer to the postlinearization if present
		        cmsStage PostLin = cmslut.cmsPipelineGetPtrToLastStage(Src);
		        
		        // Check if suitable
		        if (cmslut.cmsStageType(PostLin) == lcms2.cmsSigCurveSetElemType)
		        {
		            // Maybe this is a linear tram, so we can avoid the whole stuff
		            if (!AllCurvesAreLinear(PostLin))
		            {
		                // All seems ok, proceed.
		                NewPostLin = cmslut.cmsStageDup(PostLin);
		                cmslut.cmsPipelineInsertStage(Dest, lcms2.cmsAT_END, NewPostLin);
		                
		                // In destination LUT, the sampling shoud be applied after this stage.
		                temp[0] = KeepPostLin;
		                cmslut.cmsPipelineUnlinkStage(Src, lcms2.cmsAT_END, temp);
		                KeepPostLin = temp[0];
		            }
		        }
		    }

		    // Now its time to do the sampling. We have to ignore pre/post linearization
		    // The source LUT whithout pre/post curves is passed as parameter.
		    if (!cmslut.cmsStageSampleCLut16bit(CLUT, XFormSampler16, Src, 0))
		    {
		        // Ops, something went wrong, Restore stages
		        if (KeepPreLin != null)
		        {
		        	cmslut.cmsPipelineInsertStage(Src, lcms2.cmsAT_BEGIN, KeepPreLin);
		        }
		        if (KeepPostLin != null)
		        {
		        	cmslut.cmsPipelineInsertStage(Src, lcms2.cmsAT_END, KeepPostLin);
		        }
		        cmslut.cmsPipelineFree(Dest);
		        return false;
		    }
		    
		    // Done.
		    
		    if (KeepPreLin != null)
		    {
		    	cmslut.cmsStageFree(KeepPreLin);
		    }
		    if (KeepPostLin != null)
		    {
		    	cmslut.cmsStageFree(KeepPostLin);
		    }
		    cmslut.cmsPipelineFree(Src);
		    
		    DataCLUT = (_cmsStageCLutData)CLUT.Data;
		    
		    if (NewPreLin == null)
		    {
		    	DataSetIn = null;
		    }
		    else
		    {
		    	DataSetIn = ((_cmsStageToneCurvesData)NewPreLin.Data).TheCurves;
		    }

		    if (NewPostLin == null)
		    {
		    	DataSetOut = null;
		    }
		    else
		    {
		    	DataSetOut = ((_cmsStageToneCurvesData)NewPostLin.Data).TheCurves;
		    }
		    
		    if (DataSetIn == null && DataSetOut == null)
		    {
		    	final _cmsInterpFn16 interp = DataCLUT.Params.Interpolation.get16();
		    	_cmsOPTeval16Fn eval;
		    	if(interp instanceof _cmsOPTeval16Fn)
		    	{
		    		eval = (_cmsOPTeval16Fn)interp;
		    	}
		    	else
		    	{
		    		eval = new _cmsOPTeval16Fn()
		    		{
		    			public void run(short[] In, short[] Out, Object Data)
		    			{
		    				interp.run(In, Out, (cmsInterpParams)Data);
						}
					};
		    	}
		        cmslut._cmsPipelineSetOptimizationParameters(Dest, eval, DataCLUT.Params, null, null);
		    }
		    else
		    {
		        p16 = PrelinOpt16alloc(Dest.ContextID, DataCLUT.Params, Dest.InputChannels, DataSetIn, Dest.OutputChannels, DataSetOut);
		        
		        cmslut._cmsPipelineSetOptimizationParameters(Dest, PrelinEval16, p16, PrelinOpt16free, Prelin16dup);
		    }
		    
		    // Don't fix white on absolute colorimetric
		    if (Intent == lcms2.INTENT_ABSOLUTE_COLORIMETRIC)
		    {
		    	dwFlags[0] |= lcms2.cmsFLAGS_NOWHITEONWHITEFIXUP;
		    }
		    
		    if ((dwFlags[0] & lcms2.cmsFLAGS_NOWHITEONWHITEFIXUP) == 0)
		    {
		        FixWhiteMisalignment(Dest, ColorSpace, OutputColorSpace);
		    }
		    
		    Lut[0] = Dest;
		    return true;
		}
	};
	
	// -----------------------------------------------------------------------------------------------------------------------------------------------
	// Fixes the gamma balancing of transform. This is described in my paper "Prelinearization Stages on
	// Color-Management Application-Specific Integrated Circuits (ASICs)" presented at NIP24. It only works
	// for RGB transforms. See the paper for more details
	// -----------------------------------------------------------------------------------------------------------------------------------------------
	
	// Normalize endpoints by slope limiting max and min. This assures endpoints as well.
	// Descending curves are handled as well.
	private static void SlopeLimiting(cmsToneCurve g)
	{
	    int BeginVal, EndVal;
	    int AtBegin = (int)Math.floor(g.nEntries * 0.02 + 0.5);	// Cutoff at 2%
	    int AtEnd   = g.nEntries - AtBegin - 1;					// And 98%
	    double Val, Slope, beta;
	    int i;
	    
	    if (cmsgamma.cmsIsToneCurveDescending(g))
	    {
	        BeginVal = 0xffff; EndVal = 0;
	    }
	    else
	    {
	        BeginVal = 0; EndVal = 0xffff;
	    }
	    
	    // Compute slope and offset for begin of curve
	    Val   = g.Table16[AtBegin];
	    Slope = (Val - BeginVal) / AtBegin;
	    beta  = Val - Slope * AtBegin;
	    
	    for (i=0; i < AtBegin; i++)
	    {
	    	g.Table16[i] = lcms2_internal._cmsQuickSaturateWord(i * Slope + beta);
	    }
	    
	    // Compute slope and offset for the end
	    Val   = g.Table16[AtEnd];
	    Slope = (EndVal - Val) / AtBegin; // AtBegin holds the X interval, which is same in both cases
	    beta  = Val - Slope * AtEnd;
	    
	    for (i = AtEnd; i < g.nEntries; i++)
	    {
	    	g.Table16[i] = lcms2_internal._cmsQuickSaturateWord(i * Slope + beta);
	    }
	}
	
	// Precomputes tables for 8-bit on input devicelink.
	private static Prelin8Data PrelinOpt8alloc(cmsContext ContextID, final cmsInterpParams p, cmsToneCurve[] G)
	{
	    int i;
	    short[] Input = new short[3];
	    int v1, v2, v3;
	    Prelin8Data p8;
	    
	    p8 = new Prelin8Data();
	    
	    // Since this only works for 8 bit input, values comes always as x * 257,
	    // we can safely take msb byte (x << 8 + x)
	    
	    for (i=0; i < 256; i++)
	    {
	        if (G != null)
	        {
	            // Get 16-bit representation
	            Input[0] = cmsgamma.cmsEvalToneCurve16(G[0], lcms2_internal.FROM_8_TO_16(i));
	            Input[1] = cmsgamma.cmsEvalToneCurve16(G[1], lcms2_internal.FROM_8_TO_16(i));
	            Input[2] = cmsgamma.cmsEvalToneCurve16(G[2], lcms2_internal.FROM_8_TO_16(i));
	        }
	        else
	        {
	            Input[0] = lcms2_internal.FROM_8_TO_16(i);
	            Input[1] = lcms2_internal.FROM_8_TO_16(i);
	            Input[2] = lcms2_internal.FROM_8_TO_16(i);
	        }
	        
	        // Move to 0..1.0 in fixed domain
	        v1 = lcms2_internal._cmsToFixedDomain(Input[0] * p.Domain[0]);
	        v2 = lcms2_internal._cmsToFixedDomain(Input[1] * p.Domain[1]);
	        v3 = lcms2_internal._cmsToFixedDomain(Input[2] * p.Domain[2]);
	        
	        // Store the precalculated table of nodes
	        p8.X0[i] = (p.opta[2] * lcms2_internal.FIXED_TO_INT(v1));
	        p8.Y0[i] = (p.opta[1] * lcms2_internal.FIXED_TO_INT(v2));
	        p8.Z0[i] = (p.opta[0] * lcms2_internal.FIXED_TO_INT(v3));
	        
	        // Store the precalculated table of offsets
	        p8.rx[i] = (short)lcms2_internal.FIXED_REST_TO_INT(v1);
	        p8.ry[i] = (short)lcms2_internal.FIXED_REST_TO_INT(v2);
	        p8.rz[i] = (short)lcms2_internal.FIXED_REST_TO_INT(v3);
	    }
	    
	    p8.ContextID = ContextID;
	    p8.p = p;
	    
	    return p8;
	}
	
	private static final _cmsFreeUserDataFn Prelin8free = new _cmsFreeUserDataFn()
	{
		public void run(cmsContext ContextID, Object ptr)
		{
			//cmserr._cmsFree(ContextID, ptr);
		}
	};
	
	private static final _cmsDupUserDataFn Prelin8dup = new _cmsDupUserDataFn()
	{
		public Object run(cmsContext ContextID, Object ptr)
		{
			//return cmserr._cmsDupMem(ContextID, ptr, sizeof(Prelin8Data));
			Prelin8Data dup = new Prelin8Data();
			Prelin8Data ori = (Prelin8Data)ptr;
			dup.ContextID = ori.ContextID;
			dup.p = ori.p;
			System.arraycopy(ori.rx, 0, dup.rx, 0, 256);
			System.arraycopy(ori.ry, 0, dup.ry, 0, 256);
			System.arraycopy(ori.rz, 0, dup.rz, 0, 256);
			System.arraycopy(ori.X0, 0, dup.X0, 0, 256);
			System.arraycopy(ori.Y0, 0, dup.Y0, 0, 256);
			System.arraycopy(ori.Z0, 0, dup.Z0, 0, 256);
			return dup;
		}
	};
	
	// A optimized interpolation for 8-bit input.
	private static int DENS(int i, int j, int k, short[] LutTable, int OutChan)
	{
		return LutTable[i+j+k+OutChan];
	}
	private static final _cmsOPTeval16Fn PrelinEval8 = new _cmsOPTeval16Fn()
	{
		public void run(short[] Input, short[] Output, final Object D)
		{
			byte r, g, b;
		    int rx, ry, rz;
		    int c0, c1, c2, c3, Rest;
		    int OutChan;
		    int X0, X1, Y0, Y1, Z0, Z1;
		    Prelin8Data p8 = (Prelin8Data)D;
		    final cmsInterpParams p = p8.p;
		    int TotalOut = p.nOutputs;
		    final short[] LutTable = (short[])p.Table;
		    
		    r = (byte)(Input[0] >> 8);
		    g = (byte)(Input[1] >> 8);
		    b = (byte)(Input[2] >> 8);
		    
		    X0 = X1 = p8.X0[r];
		    Y0 = Y1 = p8.Y0[g];
		    Z0 = Z1 = p8.Z0[b];
		    
		    rx = p8.rx[r];
		    ry = p8.ry[g];
		    rz = p8.rz[b];
		    
		    X1 = X0 + ((rx == 0) ? 0 : p.opta[2]);
		    Y1 = Y0 + ((ry == 0) ? 0 : p.opta[1]);
		    Z1 = Z0 + ((rz == 0) ? 0 : p.opta[0]);
		    
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
		        
		        Rest = c1 * rx + c2 * ry + c3 * rz + 0x8001;
		        Output[OutChan] = (short)(c0 + ((Rest + (Rest>>16))>>16));
		    }
		}
	};
	
	// Curves that contain wide empty areas are not optimizeable
	private static boolean IsDegenerated(final cmsToneCurve g)
	{
	    int i, Zeros = 0, Poles = 0;
	    int nEntries = g.nEntries;
	    
	    for (i=0; i < nEntries; i++)
	    {
	        if (g.Table16[i] == 0x0000)
	        {
	        	Zeros++;
	        }
	        if (g.Table16[i] == (short)0xffff)
	        {
	        	Poles++;
	        }
	    }
	    
	    if (Zeros == 1 && Poles == 1)
	    {
	    	return false; // For linear tables
	    }
	    if (Zeros > (nEntries / 4))
	    {
	    	return true; // Degenerated, mostly zeros
	    }
	    if (Poles > (nEntries / 4))
	    {
	    	return true; // Degenerated, mostly poles
	    }
	    
	    return false;
	}
	
	// --------------------------------------------------------------------------------------------------------------
	// We need xput over here
	
	private static final _cmsOPToptimizeFn OptimizeByComputingLinearization = new _cmsOPToptimizeFn()
	{
		public boolean run(cmsPipeline[] Lut, int Intent, int[] InputFormat, int[] OutputFormat, int[] dwFlags)
		{
			cmsPipeline OriginalLut;
		    int nGridPoints;
		    cmsToneCurve[] Trans = new cmsToneCurve[lcms2.cmsMAXCHANNELS], TransReverse = new cmsToneCurve[lcms2.cmsMAXCHANNELS];
		    int t, i;
		    float v;
		    float[] In = new float[lcms2.cmsMAXCHANNELS], Out = new float[lcms2.cmsMAXCHANNELS];
		    boolean lIsSuitable, lIsLinear;
		    cmsPipeline OptimizedLUT = null, LutPlusCurves = null;
		    cmsStage OptimizedCLUTmpe;
		    int ColorSpace, OutputColorSpace;
		    cmsStage OptimizedPrelinMpe;
		    cmsStage mpe;
		    cmsToneCurve[] OptimizedPrelinCurves;
		    _cmsStageCLutData OptimizedPrelinCLUT;
		    
		    // This is a loosy optimization! does not apply in floating-point cases
		    if (cmspack._cmsFormatterIsFloat(InputFormat[0]) || cmspack._cmsFormatterIsFloat(OutputFormat[0]))
		    {
		    	return false;
		    }
		    
		    // Only on RGB
		    if (lcms2.T_COLORSPACE(InputFormat[0])  != lcms2.PT_RGB)
		    {
		    	return false;
		    }
		    if (lcms2.T_COLORSPACE(OutputFormat[0]) != lcms2.PT_RGB)
		    {
		    	return false;
		    }
		    
		    // On 16 bits, user has to specify the feature
		    if (!cmspack._cmsFormatterIs8bit(InputFormat[0]))
		    {
		        if ((dwFlags[0] & lcms2.cmsFLAGS_CLUT_PRE_LINEARIZATION) == 0)
			    {
			    	return false;
			    }
		    }
		    
		    OriginalLut = Lut[0];
		    
		    // Named color pipelines cannot be optimized either
		    for (mpe = cmslut.cmsPipelineGetPtrToFirstStage(OriginalLut);
		    		mpe != null;
		    		mpe = cmslut.cmsStageNext(mpe))
		    {
		    	if (cmslut.cmsStageType(mpe) == lcms2.cmsSigNamedColorElemType)
		    	{
		    		return false;
		    	}
		    }
		    
		    ColorSpace       = cmspcs._cmsICCcolorSpace(lcms2.T_COLORSPACE(InputFormat[0]));
		    OutputColorSpace = cmspcs._cmsICCcolorSpace(lcms2.T_COLORSPACE(OutputFormat[0]));
		    nGridPoints      = cmspcs._cmsReasonableGridpointsByColorspace(ColorSpace, dwFlags[0]);
		    
		    for (t = 0; t < OriginalLut.InputChannels; t++)
		    {
		        Trans[t] = cmsgamma.cmsBuildTabulatedToneCurve16(OriginalLut.ContextID, PRELINEARIZATION_POINTS, null);
		        if (Trans[t] == null)
		        {
		        	for (t = 0; t < OriginalLut.InputChannels; t++)
				    {
				        if (Trans[t] != null)
				        {
				        	cmsgamma.cmsFreeToneCurve(Trans[t]);
				        }
				        if (TransReverse[t] != null)
				        {
				        	cmsgamma.cmsFreeToneCurve(TransReverse[t]);
				        }
				    }
				    
				    if (LutPlusCurves != null)
				    {
				    	cmslut.cmsPipelineFree(LutPlusCurves);
				    }
				    if (OptimizedLUT != null)
				    {
				    	cmslut.cmsPipelineFree(OptimizedLUT);
				    }
				    
				    return false;
		        }
		    }

		    // Populate the curves
		    for (i=0; i < PRELINEARIZATION_POINTS; i++)
		    {
		        v = (float)(((double)i) / (PRELINEARIZATION_POINTS - 1));
		        
		        // Feed input with a gray ramp
		        for (t=0; t < OriginalLut.InputChannels; t++)
		        {
		        	In[t] = v;
		        }
		        
		        // Evaluate the gray value
		        cmslut.cmsPipelineEvalFloat(In, Out, OriginalLut);
		        
		        // Store result in curve
		        for (t=0; t < OriginalLut.InputChannels; t++)
		        {
		        	Trans[t].Table16[i] = lcms2_internal._cmsQuickSaturateWord(Out[t] * 65535.0);
		        }
		    }
		    
		    // Slope-limit the obtained curves
		    for (t = 0; t < OriginalLut.InputChannels; t++)
		    {
		    	SlopeLimiting(Trans[t]);
		    }
		    
		    // Check for validity
		    lIsSuitable = true;
		    lIsLinear   = true;
		    for (t=0; (lIsSuitable && (t < OriginalLut.InputChannels)); t++)
		    {
		        // Exclude if already linear
		        if (!cmsgamma.cmsIsToneCurveLinear(Trans[t]))
		        {
		        	lIsLinear = false;
		        }
		        
		        // Exclude if non-monotonic
		        if (!cmsgamma.cmsIsToneCurveMonotonic(Trans[t]))
		        {
		        	lIsSuitable = false;
		        }
		        
		        if (IsDegenerated(Trans[t]))
		        {
		        	lIsSuitable = false;
		        }
		    }
		    
		    // If it is not suitable, just quit
		    if (!lIsSuitable)
		    {
		    	for (t = 0; t < OriginalLut.InputChannels; t++)
			    {
			        if (Trans[t] != null)
			        {
			        	cmsgamma.cmsFreeToneCurve(Trans[t]);
			        }
			        if (TransReverse[t] != null)
			        {
			        	cmsgamma.cmsFreeToneCurve(TransReverse[t]);
			        }
			    }
			    
			    return false;
		    }
		    
		    // Invert curves if possible
		    for (t = 0; t < OriginalLut.InputChannels; t++)
		    {
		        TransReverse[t] = cmsgamma.cmsReverseToneCurveEx(PRELINEARIZATION_POINTS, Trans[t]);
		        if (TransReverse[t] == null)
		        {
		        	for (t = 0; t < OriginalLut.InputChannels; t++)
				    {
				        if (Trans[t] != null)
				        {
				        	cmsgamma.cmsFreeToneCurve(Trans[t]);
				        }
				        if (TransReverse[t] != null)
				        {
				        	cmsgamma.cmsFreeToneCurve(TransReverse[t]);
				        }
				    }
				    
				    if (LutPlusCurves != null)
				    {
				    	cmslut.cmsPipelineFree(LutPlusCurves);
				    }
				    if (OptimizedLUT != null)
				    {
				    	cmslut.cmsPipelineFree(OptimizedLUT);
				    }
				    
				    return false;
		        }
		    }
		    
		    // Now inset the reversed curves at the begin of transform
		    LutPlusCurves = cmslut.cmsPipelineDup(OriginalLut);
		    if (LutPlusCurves == null)
		    {
		    	for (t = 0; t < OriginalLut.InputChannels; t++)
			    {
			        if (Trans[t] != null)
			        {
			        	cmsgamma.cmsFreeToneCurve(Trans[t]);
			        }
			        if (TransReverse[t] != null)
			        {
			        	cmsgamma.cmsFreeToneCurve(TransReverse[t]);
			        }
			    }
			    
			    return false;
		    }
		    
		    cmslut.cmsPipelineInsertStage(LutPlusCurves, lcms2.cmsAT_BEGIN, cmslut.cmsStageAllocToneCurves(OriginalLut.ContextID, OriginalLut.InputChannels, TransReverse));
		    
		    // Create the result LUT
		    OptimizedLUT = cmslut.cmsPipelineAlloc(OriginalLut.ContextID, OriginalLut.InputChannels, OriginalLut.OutputChannels);
		    if (OptimizedLUT == null)
		    {
		    	for (t = 0; t < OriginalLut.InputChannels; t++)
			    {
			        if (Trans[t] != null)
			        {
			        	cmsgamma.cmsFreeToneCurve(Trans[t]);
			        }
			        if (TransReverse[t] != null)
			        {
			        	cmsgamma.cmsFreeToneCurve(TransReverse[t]);
			        }
			    }
			    
			    if (LutPlusCurves != null)
			    {
			    	cmslut.cmsPipelineFree(LutPlusCurves);
			    }
			    
			    return false;
		    }
		    
		    OptimizedPrelinMpe = cmslut.cmsStageAllocToneCurves(OriginalLut.ContextID, OriginalLut.InputChannels, Trans);
		    
		    // Create and insert the curves at the beginning
		    cmslut.cmsPipelineInsertStage(OptimizedLUT, lcms2.cmsAT_BEGIN, OptimizedPrelinMpe);
		    
		    // Allocate the CLUT for result
		    OptimizedCLUTmpe = cmslut.cmsStageAllocCLut16bit(OriginalLut.ContextID, nGridPoints, OriginalLut.InputChannels, OriginalLut.OutputChannels, null);
		    
		    // Add the CLUT to the destination LUT
		    cmslut.cmsPipelineInsertStage(OptimizedLUT, lcms2.cmsAT_END, OptimizedCLUTmpe);
		    
		    // Resample the LUT
		    if (!cmslut.cmsStageSampleCLut16bit(OptimizedCLUTmpe, XFormSampler16, LutPlusCurves, 0))
		    {
		    	for (t = 0; t < OriginalLut.InputChannels; t++)
			    {
			        if (Trans[t] != null)
			        {
			        	cmsgamma.cmsFreeToneCurve(Trans[t]);
			        }
			        if (TransReverse[t] != null)
			        {
			        	cmsgamma.cmsFreeToneCurve(TransReverse[t]);
			        }
			    }
			    
			    if (LutPlusCurves != null)
			    {
			    	cmslut.cmsPipelineFree(LutPlusCurves);
			    }
			    if (OptimizedLUT != null)
			    {
			    	cmslut.cmsPipelineFree(OptimizedLUT);
			    }
			    
			    return false;
		    }
		    
		    // Free resources
		    for (t = 0; t < OriginalLut.InputChannels; t++)
		    {
		        if (Trans[t] != null)
		        {
		        	cmsgamma.cmsFreeToneCurve(Trans[t]);
		        }
		        if (TransReverse[t] != null)
		        {
		        	cmsgamma.cmsFreeToneCurve(TransReverse[t]);
		        }
		    }
		    
		    cmslut.cmsPipelineFree(LutPlusCurves);
		    
		    OptimizedPrelinCurves = cmslut._cmsStageGetPtrToCurveSet(OptimizedPrelinMpe);
		    OptimizedPrelinCLUT   = (_cmsStageCLutData)OptimizedCLUTmpe.Data;
		    
		    // Set the evaluator if 8-bit
		    if (cmspack._cmsFormatterIs8bit(InputFormat[0]))
		    {
		        Prelin8Data p8 = PrelinOpt8alloc(OptimizedLUT.ContextID, OptimizedPrelinCLUT.Params, OptimizedPrelinCurves);
		        if (p8 == null)
		        {
		        	return false;
		        }
		        
		        cmslut._cmsPipelineSetOptimizationParameters(OptimizedLUT, PrelinEval8, p8, Prelin8free, Prelin8dup);
		    }
		    else
		    {
		        Prelin16Data p16 = PrelinOpt16alloc(OptimizedLUT.ContextID, OptimizedPrelinCLUT.Params, 3, OptimizedPrelinCurves, 3, null);
		        if (p16 == null)
		        {
		        	return false;
		        }
		        
		        cmslut._cmsPipelineSetOptimizationParameters(OptimizedLUT, PrelinEval16, p16, PrelinOpt16free, Prelin16dup);
		    }
		    
		    // Don't fix white on absolute colorimetric
		    if (Intent == lcms2.INTENT_ABSOLUTE_COLORIMETRIC)
		    {
		    	dwFlags[0] |= lcms2.cmsFLAGS_NOWHITEONWHITEFIXUP;
		    }
		    
		    if ((dwFlags[0] & lcms2.cmsFLAGS_NOWHITEONWHITEFIXUP) == 0)
		    {
		        if (!FixWhiteMisalignment(OptimizedLUT, ColorSpace, OutputColorSpace)) {

		            return false;
		        }
		    }
		    
		    // And return the obtained LUT
		    
		    cmslut.cmsPipelineFree(OriginalLut);
		    Lut[0] = OptimizedLUT;
		    return true;
		}
	};
	
	// Curves optimizer ------------------------------------------------------------------------------------------------------------------
	
	private static final _cmsFreeUserDataFn CurvesFree = new _cmsFreeUserDataFn()
	{
		public void run(cmsContext ContextID, Object ptr)
		{
			Curves16Data Data = (Curves16Data)ptr;
			int i;
			
			for (i=0; i < Data.nCurves; i++)
			{
				Data.Curves[i] = null;
				//_cmsFree(ContextID, Data.Curves[i]);
			}
			
			Data.Curves = null;
			//_cmsFree(ContextID, Data.Curves);
			//_cmsFree(ContextID, ptr);
		}
	};
	
	private static final _cmsDupUserDataFn CurvesDup = new _cmsDupUserDataFn()
	{
		public Object run(cmsContext ContextID, Object ptr)
		{
			//Curves16Data Data = _cmsDupMem(ContextID, ptr, sizeof(Curves16Data));
			Curves16Data Data = new Curves16Data();
			Curves16Data ori = (Curves16Data)ptr;
			Data.ContextID = ori.ContextID;
			Data.nCurves = ori.nCurves;
			Data.nElements = ori.nElements;
		    int i;
		    
		    /*
		    if (Data == null)
		    {
		    	return null;
		    }
		    */
		    
		    Data.Curves = new short[Data.nCurves][];
		    //Data.Curves = _cmsDupMem(ContextID, Data.Curves, Data.nCurves * sizeof(cmsUInt16Number*));
		    
		    for (i=0; i < Data.nCurves; i++)
		    {
		        //Data.Curves[i] = _cmsDupMem(ContextID, Data.Curves[i], Data.nElements * sizeof(cmsUInt16Number));
		    	Data.Curves[i] = Arrays.copy(Data.Curves[i], 0, Data.nElements);
		    }
		    
		    return Data;
		}
	};
	
	// Precomputes tables for 8-bit on input devicelink.
	private static Curves16Data CurvesAlloc(cmsContext ContextID, int nCurves, int nElements, cmsToneCurve[] G)
	{
	    int i, j;
	    Curves16Data c16;
	    
	    c16 = new Curves16Data();
	    
	    c16.nCurves = nCurves;
	    c16.nElements = nElements;
	    
	    c16.Curves = new short[nCurves][];
	    
	    for (i=0; i < nCurves; i++)
	    {
	        c16.Curves[i] = new short[nElements];
	        
	        if (nElements == 256)
	        {
	            for (j=0; j < nElements; j++)
	            {
	                c16.Curves[i][j] = cmsgamma.cmsEvalToneCurve16(G[i], lcms2_internal.FROM_8_TO_16(j));
	            }
	        }
	        else
	        {
	            for (j=0; j < nElements; j++)
	            {
	                c16.Curves[i][j] = cmsgamma.cmsEvalToneCurve16(G[i], (short)j);
	            }
	        }
	    }
	    
	    return c16;
	}
	
	private static final _cmsOPTeval16Fn FastEvaluateCurves8 = new _cmsOPTeval16Fn()
	{
		public void run(short[] In, short[] Out, Object D)
		{
			Curves16Data Data = (Curves16Data)D;
		    int x;
		    int i;
		    
		    for (i=0; i < Data.nCurves; i++)
		    {
		    	x = (In[i] >> 8) & 0xFF; //Since no unsigned byte exists it is more efficient to use an int
		    	Out[i] = Data.Curves[i][x];
		    }
		}
	};
	
	private static final _cmsOPTeval16Fn FastEvaluateCurves16 = new _cmsOPTeval16Fn()
	{
		public void run(short[] In, short[] Out, Object D)
		{
			Curves16Data Data = (Curves16Data)D;
		    int i;
		    
		    for (i=0; i < Data.nCurves; i++)
		    {
		    	Out[i] = Data.Curves[i][In[i] & 0xFFFF];
		    }
		}
	};
	
	private static final _cmsOPTeval16Fn FastIdentity16 = new _cmsOPTeval16Fn()
	{
		public void run(short[] In, short[] Out, Object D)
		{
			cmsPipeline Lut = (cmsPipeline)D;
		    int i;
		    
		    for (i=0; i < Lut.InputChannels; i++)
		    {
		    	Out[i] = In[i];
		    }
		}
	};
	
	// If the target LUT holds only curves, the optimization procedure is to join all those
	// curves together. That only works on curves and does not work on matrices.
	private static final _cmsOPToptimizeFn OptimizeByJoiningCurves = new _cmsOPToptimizeFn()
	{
		public boolean run(cmsPipeline[] Lut, int Intent, int[] InputFormat, int[] OutputFormat, int[] dwFlags)
		{
			cmsToneCurve[] GammaTables = null;
		    float[] InFloat = new float[lcms2.cmsMAXCHANNELS], OutFloat = new float[lcms2.cmsMAXCHANNELS];
		    int i, j;
		    cmsPipeline Src = Lut[0];
		    cmsPipeline Dest = null;
		    cmsStage mpe;
		    cmsStage ObtainedCurves = null;
		    
		    // This is a loosy optimization! does not apply in floating-point cases
		    if (cmspack._cmsFormatterIsFloat(InputFormat[0]) || cmspack._cmsFormatterIsFloat(OutputFormat[0]))
		    {
		    	return false;
		    }
		    
		    //  Only curves in this LUT?
		    for (mpe = cmslut.cmsPipelineGetPtrToFirstStage(Src); mpe != null; mpe = cmslut.cmsStageNext(mpe))
		    {
		    	if (cmslut.cmsStageType(mpe) != lcms2.cmsSigCurveSetElemType)
			    {
			    	return false;
			    }
		    }
		    
		    // Allocate an empty LUT
		    Dest = cmslut.cmsPipelineAlloc(Src.ContextID, Src.InputChannels, Src.OutputChannels);
		    if (Dest == null)
		    {
		    	return false;
		    }
		    
		    // Create target curves
		    GammaTables = new cmsToneCurve[Src.InputChannels];
		    
		    for (i=0; i < Src.InputChannels; i++)
		    {
		        GammaTables[i] = cmsgamma.cmsBuildTabulatedToneCurve16(Src.ContextID, PRELINEARIZATION_POINTS, null);
		        if (GammaTables[i] == null)
			    {
			    	if (ObtainedCurves != null)
				    {
				    	cmslut.cmsStageFree(ObtainedCurves);
				    }
				    if (GammaTables != null)
				    {
				        for (i=0; i < Src.InputChannels; i++)
				        {
				            if (GammaTables[i] != null)
				            {
				            	cmsgamma.cmsFreeToneCurve(GammaTables[i]);
				            }
				        }
				        
				        //_cmsFree(Src.ContextID, GammaTables);
				    }
				    
				    if (Dest != null)
				    {
				    	cmslut.cmsPipelineFree(Dest);
				    }
				    return false;
			    }
		    }
		    
		    // Compute 16 bit result by using floating point
		    for (i=0; i < PRELINEARIZATION_POINTS; i++)
		    {
		        for (j=0; j < Src.InputChannels; j++)
		        {
		        	InFloat[j] = (float)(((double)i) / (PRELINEARIZATION_POINTS - 1));
		        }
		        
		        cmslut.cmsPipelineEvalFloat(InFloat, OutFloat, Src);
		        
		        for (j=0; j < Src.InputChannels; j++)
		        {
		        	GammaTables[j].Table16[i] = lcms2_internal._cmsQuickSaturateWord(OutFloat[j] * 65535.0);
		        }
		    }

		    ObtainedCurves = cmslut.cmsStageAllocToneCurves(Src.ContextID, Src.InputChannels, GammaTables);
		    if (ObtainedCurves == null)
		    {
			    if (GammaTables != null)
			    {
			        for (i=0; i < Src.InputChannels; i++)
			        {
			            if (GammaTables[i] != null)
			            {
			            	cmsgamma.cmsFreeToneCurve(GammaTables[i]);
			            }
			        }
			        
			        //_cmsFree(Src.ContextID, GammaTables);
			    }
			    
			    if (Dest != null)
			    {
			    	cmslut.cmsPipelineFree(Dest);
			    }
			    return false;
		    }

		    for (i=0; i < Src.InputChannels; i++)
		    {
		        cmsgamma.cmsFreeToneCurve(GammaTables[i]);
		        GammaTables[i] = null;
		    }
		    
		    if (GammaTables != null)
		    {
		    	//_cmsFree(Src.ContextID, GammaTables);
		    	GammaTables = null;
		    }
		    
		    // Maybe the curves are linear at the end
		    if (!AllCurvesAreLinear(ObtainedCurves))
		    {
		        cmslut.cmsPipelineInsertStage(Dest, lcms2.cmsAT_BEGIN, ObtainedCurves);
		        
		        // If the curves are to be applied in 8 bits, we can save memory
		        if (cmspack._cmsFormatterIs8bit(InputFormat[0]))
		        {
		        	_cmsStageToneCurvesData Data = (_cmsStageToneCurvesData)ObtainedCurves.Data;
		        	Curves16Data c16 = CurvesAlloc(Dest.ContextID, Data.nCurves, 256, Data.TheCurves);
		        	
		        	dwFlags[0] |= lcms2.cmsFLAGS_NOCACHE;
		            cmslut._cmsPipelineSetOptimizationParameters(Dest, FastEvaluateCurves8, c16, CurvesFree, CurvesDup);
		        }
		        else
		        {
		        	_cmsStageToneCurvesData Data = (_cmsStageToneCurvesData)cmslut.cmsStageData(ObtainedCurves);
		        	Curves16Data c16 = CurvesAlloc(Dest.ContextID, Data.nCurves, 65536, Data.TheCurves);
		        	
		        	dwFlags[0] |= lcms2.cmsFLAGS_NOCACHE;
		        	cmslut._cmsPipelineSetOptimizationParameters(Dest, FastEvaluateCurves16, c16, CurvesFree, CurvesDup);
		        }
		    }
		    else
		    {
		        // LUT optimizes to nothing. Set the identity LUT
		        cmslut.cmsStageFree(ObtainedCurves);
		        
		        cmslut.cmsPipelineInsertStage(Dest, lcms2.cmsAT_BEGIN, cmslut.cmsStageAllocIdentity(Dest.ContextID, Src.InputChannels));
		        
		        dwFlags[0] |= lcms2.cmsFLAGS_NOCACHE;
		        cmslut._cmsPipelineSetOptimizationParameters(Dest, FastIdentity16, Dest, null, null);
		    }
		    
		    // We are done.
		    cmslut.cmsPipelineFree(Src);
		    Lut[0] = Dest;
		    return true;
		}
	};
	
	// -------------------------------------------------------------------------------------------------------------------------------------
	// LUT is Shaper - Matrix - Matrix - Shaper, which is very frequent when combining two matrix-shaper profiles
	
	private static final _cmsFreeUserDataFn FreeMatShaper = new _cmsFreeUserDataFn()
	{
		public void run(cmsContext ContextID, Object Data)
		{
			if (Data != null)
			{
				//_cmsFree(ContextID, Data);
			}
		}
	};
	
	private static final _cmsDupUserDataFn DupMatShaper = new _cmsDupUserDataFn()
	{
		public Object run(cmsContext ContextID, final Object Data)
		{
			MatShaper8Data dup = new MatShaper8Data();
			MatShaper8Data ori = (MatShaper8Data)Data;
			dup.ContextID = ori.ContextID;
			System.arraycopy(ori.Shaper1R, 0, dup.Shaper1R, 0, 256);
			System.arraycopy(ori.Shaper1G, 0, dup.Shaper1G, 0, 256);
			System.arraycopy(ori.Shaper1B, 0, dup.Shaper1B, 0, 256);
			for(int i = 0; i < 3; i++)
			{
				System.arraycopy(ori.Mat[i], 0, dup.Mat[i], 0, 3);
			}
			System.arraycopy(ori.Off, 0, dup.Off, 0, 3);
			System.arraycopy(ori.Shaper2R, 0, dup.Shaper2R, 0, 16385);
			System.arraycopy(ori.Shaper2G, 0, dup.Shaper2G, 0, 16385);
			System.arraycopy(ori.Shaper2B, 0, dup.Shaper2B, 0, 16385);
			return dup;
		}
	};
	
	// A fast matrix-shaper evaluator for 8 bits. This is a bit tricky since I'm using 1.14 signed fixed point
	// to accomplish some performance. Actually it takes 256x3 16 bits tables and 16385 x 3 tables of 8 bits,
	// in total about 50K, and the performance boost is huge!
	private static final _cmsOPTeval16Fn MatShaperEval16 = new _cmsOPTeval16Fn()
	{
		public void run(short[] In, short[] Out, Object D)
		{
			MatShaper8Data p = (MatShaper8Data)D;
		    int l1, l2, l3, r, g, b;
		    int ri, gi, bi;
		    
		    // In this case (and only in this case!) we can use this simplification since
		    // In[] is assured to come from a 8 bit number. (a << 8 | a)
		    ri = In[0] & 0xFF;
		    gi = In[1] & 0xFF;
		    bi = In[2] & 0xFF;
		    
		    // Across first shaper, which also converts to 1.14 fixed point
		    r = p.Shaper1R[ri];
		    g = p.Shaper1G[gi];
		    b = p.Shaper1B[bi];
		    
		    // Evaluate the matrix in 1.14 fixed point
		    l1 =  (p.Mat[0][0] * r + p.Mat[0][1] * g + p.Mat[0][2] * b + p.Off[0] + 0x2000) >> 14;
		    l2 =  (p.Mat[1][0] * r + p.Mat[1][1] * g + p.Mat[1][2] * b + p.Off[1] + 0x2000) >> 14;
		    l3 =  (p.Mat[2][0] * r + p.Mat[2][1] * g + p.Mat[2][2] * b + p.Off[2] + 0x2000) >> 14;
		    
		    // Now we have to clip to 0..1.0 range
		    ri = (l1 < 0) ? 0 : ((l1 > 16384) ? 16384 : l1);
		    gi = (l2 < 0) ? 0 : ((l2 > 16384) ? 16384 : l2);
		    bi = (l3 < 0) ? 0 : ((l3 > 16384) ? 16384 : l3);
		    
		    // And across second shaper,
		    Out[0] = p.Shaper2R[ri];
		    Out[1] = p.Shaper2G[gi];
		    Out[2] = p.Shaper2B[bi];
		}
	};
	
	// This table converts from 8 bits to 1.14 after applying the curve
	private static void FillFirstShaper(int[] Table, cmsToneCurve Curve)
	{
	    int i;
	    float R, y;
	    
	    for (i=0; i < 256; i++)
	    {
	        R = (i * (1f / 255f));
	        y = cmsgamma.cmsEvalToneCurveFloat(Curve, R);
	        
	        Table[i] = DOUBLE_TO_1FIXED14(y);
	    }
	}
	
	// This table converts form 1.14 (being 0x4000 the last entry) to 8 bits after applying the curve
	private static void FillSecondShaper(short[] Table, cmsToneCurve Curve, boolean Is8BitsOutput)
	{
	    int i;
	    float R, Val;
	    
	    for (i=0; i < 16385; i++)
	    {
	        R   = (i * (1f / 16384f));
	        Val = cmsgamma.cmsEvalToneCurveFloat(Curve, R); // Val comes 0..1.0
	        
	        if (Is8BitsOutput)
	        {
	            // If 8 bits output, we can optimize further by computing the / 257 part.
	            // first we compute the resulting byte and then we store the byte times
	            // 257. This quantization allows to round very quick by doing a >> 8, but
	            // since the low byte is always equal to msb, we can do a & 0xff and this works!
	            short w = lcms2_internal._cmsQuickSaturateWord(Val * 65535.0);
	            byte b = lcms2_internal.FROM_16_TO_8(w);
	            
	            Table[i] = lcms2_internal.FROM_8_TO_16(b);
	        }
	        else
	        {
	        	Table[i] = lcms2_internal._cmsQuickSaturateWord(Val * 65535.0);
	        }
	    }
	}
	
	// Compute the matrix-shaper structure
	private static boolean SetMatShaper(cmsPipeline Dest, cmsToneCurve[] Curve1, cmsMAT3 Mat, cmsVEC3 Off, cmsToneCurve[] Curve2, int[] OutputFormat)
	{
	    MatShaper8Data p;
	    int i, j;
	    boolean Is8Bits = cmspack._cmsFormatterIs8bit(OutputFormat[0]);
	    
	    // Allocate a big chuck of memory to store precomputed tables
	    p = new MatShaper8Data();
	    
	    p.ContextID = Dest.ContextID;
	    
	    // Precompute tables
	    FillFirstShaper(p.Shaper1R, Curve1[0]);
	    FillFirstShaper(p.Shaper1G, Curve1[1]);
	    FillFirstShaper(p.Shaper1B, Curve1[2]);
	    
	    FillSecondShaper(p.Shaper2R, Curve2[0], Is8Bits);
	    FillSecondShaper(p.Shaper2G, Curve2[1], Is8Bits);
	    FillSecondShaper(p.Shaper2B, Curve2[2], Is8Bits);
	    
	    // Convert matrix to nFixed14. Note that those values may take more than 16 bits as
	    for (i=0; i < 3; i++)
	    {
	        for (j=0; j < 3; j++)
	        {
	            p.Mat[i][j] = DOUBLE_TO_1FIXED14(Mat.v[i].n[j]);
	        }
	    }
	    
	    for (i=0; i < 3; i++)
	    {
	        if (Off == null)
	        {
	            p.Off[i] = 0;
	        }
	        else
	        {
	            p.Off[i] = DOUBLE_TO_1FIXED14(Off.n[i]);
	        }
	    }
	    
	    // Mark as optimized for faster formatter
	    if (Is8Bits)
	    {
	    	OutputFormat[0] |= lcms2.OPTIMIZED_SH(1);
	    }
	    
	    // Fill function pointers
	    cmslut._cmsPipelineSetOptimizationParameters(Dest, MatShaperEval16, p, FreeMatShaper, DupMatShaper);
	    return true;
	}
	
	//  8 bits on input allows matrix-shaper boot up to 25 Mpixels per second on RGB. That's fast!
	// TODO: Allow a third matrix for abs. colorimetric
	private static final _cmsOPToptimizeFn OptimizeMatrixShaper = new _cmsOPToptimizeFn()
	{
		public boolean run(cmsPipeline[] Lut, int Intent, int[] InputFormat, int[] OutputFormat, int[] dwFlags)
		{
			cmsStage Curve1, Curve2;
		    cmsStage Matrix1, Matrix2;
		    _cmsStageMatrixData Data1;
		    _cmsStageMatrixData Data2;
		    cmsMAT3 res = new cmsMAT3();
		    boolean IdentityMat;
		    cmsPipeline Dest, Src;
		    
		    // Only works on RGB to RGB
		    if (lcms2.T_CHANNELS(InputFormat[0]) != 3 || lcms2.T_CHANNELS(OutputFormat[0]) != 3)
		    {
		    	return false;
		    }
		    
		    // Only works on 8 bit input
		    if (!cmspack._cmsFormatterIs8bit(InputFormat[0]))
		    {
		    	return false;
		    }
		    
		    // Seems suitable, proceed
		    Src = Lut[0];
		    
		    // Check for shaper-matrix-matrix-shaper structure, that is what this optimizer stands for
		    Object[] args = new Object[4 * 2];
		    args[0] = new Integer(lcms2.cmsSigCurveSetElemType);
		    args[1] = new Integer(lcms2.cmsSigMatrixElemType);
		    args[2] = new Integer(lcms2.cmsSigMatrixElemType);
		    args[3] = new Integer(lcms2.cmsSigCurveSetElemType);
		    if (!cmslut.cmsPipelineCheckAndRetreiveStages(Src, 4, args))
		    {
		    	return false;
		    }
		    Curve1 = (cmsStage)args[4];
		    Matrix1 = (cmsStage)args[5];
		    Matrix2 = (cmsStage)args[6];
		    Curve2 = (cmsStage)args[7];
		    
		    // Get both matrices
		    Data1 = (_cmsStageMatrixData)cmslut.cmsStageData(Matrix1);
		    Data2 = (_cmsStageMatrixData)cmslut.cmsStageData(Matrix2);
		    
		    // Input offset should be zero
		    if (Data1.Offset != null)
		    {
		    	return false;
		    }
		    
		    // Multiply both matrices to get the result
		    cmsMAT3 temp1 = new cmsMAT3();
		    cmsMAT3 temp2 = new cmsMAT3();
		    cmsmtrx._cmsMAT3set(temp1, Data2.Double, 0);
		    cmsmtrx._cmsMAT3set(temp2, Data1.Double, 0);
		    cmsmtrx._cmsMAT3per(res, temp1, temp2);
		    
		    // Now the result is in res + Data2 -> Offset. Maybe is a plain identity?
		    IdentityMat = false;
		    if (cmsmtrx._cmsMAT3isIdentity(res) && Data2.Offset == null)
		    {
		        // We can get rid of full matrix
		        IdentityMat = true;
		    }
		    
		    // Allocate an empty LUT
		    Dest = cmslut.cmsPipelineAlloc(Src.ContextID, Src.InputChannels, Src.OutputChannels);
		    if (Dest == null)
		    {
		    	return false;
		    }
		    
		    // Assamble the new LUT
		    cmslut.cmsPipelineInsertStage(Dest, lcms2.cmsAT_BEGIN, cmslut.cmsStageDup(Curve1));
		    if (!IdentityMat)
		    {
		    	double[] resMat = new double[3 * 3];
		    	cmsmtrx._cmsMAT3get(res, resMat, 0);
		    	cmslut.cmsPipelineInsertStage(Dest, lcms2.cmsAT_END, cmslut.cmsStageAllocMatrix(Dest.ContextID, 3, 3, resMat, Data2.Offset));
		    }
		    cmslut.cmsPipelineInsertStage(Dest, lcms2.cmsAT_END, cmslut.cmsStageDup(Curve2));
		    
		    // If identity on matrix, we can further optimize the curves, so call the join curves routine
		    if (IdentityMat)
		    {
		    	cmsPipeline[] temp3 = new cmsPipeline[]{Dest};
		        OptimizeByJoiningCurves.run(temp3, Intent, InputFormat, OutputFormat, dwFlags);
		        Dest = temp3[0];
		    }
		    else
		    {
		        _cmsStageToneCurvesData mpeC1 = (_cmsStageToneCurvesData)cmslut.cmsStageData(Curve1);
		        _cmsStageToneCurvesData mpeC2 = (_cmsStageToneCurvesData)cmslut.cmsStageData(Curve2);
		        
		        // In this particular optimization, cach� does not help as it takes more time to deal with
		        // the cach� that with the pixel handling
		        dwFlags[0] |= lcms2.cmsFLAGS_NOCACHE;
		        
		        // Setup the optimizarion routines
		        cmsVEC3 vecTmp = new cmsVEC3();
		        cmsmtrx._cmsVEC3set(vecTmp, Data2.Offset, 0);
		        SetMatShaper(Dest, mpeC1.TheCurves, res, vecTmp, mpeC2.TheCurves, OutputFormat);
		    }
		    
		    cmslut.cmsPipelineFree(Src);
		    Lut[0] = Dest;
		    return true;
		}
	};
	
	// -------------------------------------------------------------------------------------------------------------------------------------
	// Optimization plug-ins
	
	// List of optimizations
	private static class _cmsOptimizationCollection
	{
		public _cmsOPToptimizeFn OptimizePtr;
		public _cmsOptimizationCollection Next;
		
		public _cmsOptimizationCollection()
		{
		}
		
		public _cmsOptimizationCollection(_cmsOPToptimizeFn OptimizePtr, _cmsOptimizationCollection Next)
		{
			this.OptimizePtr = OptimizePtr;
			this.Next = Next;
		}
	}
	
	// The built-in list. We currently implement 4 types of optimizations. Joining of curves, matrix-shaper, linearization and resampling
	private static final _cmsOptimizationCollection DefaultOptimization;
	static
	{
		_cmsOptimizationCollection temp = new _cmsOptimizationCollection(OptimizeByResampling, null);
		temp = new _cmsOptimizationCollection(OptimizeByComputingLinearization, temp);
		temp = new _cmsOptimizationCollection(OptimizeMatrixShaper, temp);
		DefaultOptimization = new _cmsOptimizationCollection(OptimizeByJoiningCurves, temp);
	}
	
	// The linked list head
	private static _cmsOptimizationCollection OptimizationCollection;
	
	private static final long OPTIMIZATION_COLLECTION_UID = 0x9BD12673EA887CD2L;
	
	// Register new ways to optimize
	public static boolean _cmsRegisterOptimizationPlugin(cmsPluginBase Data)
	{
	    cmsPluginOptimization Plugin = (cmsPluginOptimization)Data;
	    _cmsOptimizationCollection fl;
	    
	    if (Data == null)
	    {
	        OptimizationCollection = DefaultOptimization;
	        Utility.singletonStorageSet(OPTIMIZATION_COLLECTION_UID, OptimizationCollection);
	        return true;
	    }
	    
	    // Optimizer callback is required
	    if (Plugin.OptimizePtr == null)
	    {
	    	return false;
	    }
	    
	    fl = new _cmsOptimizationCollection();
	    
	    // Copy the parameters
	    fl.OptimizePtr = Plugin.OptimizePtr;
	    
	    // Keep linked list
	    fl.Next = OptimizationCollection;
	    OptimizationCollection = fl;
	    Utility.singletonStorageSet(OPTIMIZATION_COLLECTION_UID, OptimizationCollection);
	    
	    // All is ok
	    return true;
	}
	
	// The entry point for LUT optimization
	public static boolean _cmsOptimizePipeline(cmsPipeline[] PtrLut, int Intent, int[] InputFormat, int[] OutputFormat, int[] dwFlags)
	{
	    _cmsOptimizationCollection Opts;
	    boolean AnySuccess = false;
	    
	    // A CLUT is being asked, so force this specific optimization
	    if ((dwFlags[0] & lcms2.cmsFLAGS_FORCE_CLUT) != 0)
	    {
	        PreOptimize(PtrLut[0]);
	        return OptimizeByResampling.run(PtrLut, Intent, InputFormat, OutputFormat, dwFlags);
	    }
	    
	    // Anything to optimize?
	    if ((PtrLut[0]).Elements == null)
	    {
	        cmslut._cmsPipelineSetOptimizationParameters(PtrLut[0], FastIdentity16, PtrLut[0], null, null);
	        return true;
	    }
	    
	    // Try to get rid of identities and trivial conversions.
	    AnySuccess = PreOptimize(PtrLut[0]);
	    
	    // After removal do we end with an identity?
	    if ((PtrLut[0]).Elements == null)
	    {
	        cmslut._cmsPipelineSetOptimizationParameters(PtrLut[0], FastIdentity16, PtrLut[0], null, null);
	        return true;
	    }
	    
	    // Do not optimize, keep all precision
	    if ((dwFlags[0] & lcms2.cmsFLAGS_NOOPTIMIZE) != 0)
	    {
	    	return false;
	    }
	    
	    // Try built-in optimizations and plug-in
	    if(OptimizationCollection == null)
	    {
	    	Object obj;
	    	if((obj = Utility.singletonStorageGet(OPTIMIZATION_COLLECTION_UID)) != null)
	    	{
	    		OptimizationCollection = (_cmsOptimizationCollection)obj;
	    	}
	    	else
	    	{
	    		OptimizationCollection = DefaultOptimization;
	    	}
	    }
	    for (Opts = OptimizationCollection; Opts != null; Opts = Opts.Next)
	    {
	    	// If one schema succeeded, we are done
	    	if (Opts.OptimizePtr.run(PtrLut, Intent, InputFormat, OutputFormat, dwFlags))
	    	{
	    		return true; // Optimized!
	    	}
	    }
	    
	    // Only simple optimizations succeeded
	    return AnySuccess;
	}
}
