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

import littlecms.internal.lcms2.cmsContext;
import littlecms.internal.lcms2.cmsPipeline;
import littlecms.internal.lcms2.cmsSAMPLER16;
import littlecms.internal.lcms2.cmsStage;
import littlecms.internal.lcms2.cmsToneCurve;
import littlecms.internal.lcms2_plugin._cmsInterpFn16;
import littlecms.internal.lcms2_plugin._cmsOPTdupDataFn;
import littlecms.internal.lcms2_plugin._cmsOPTeval16Fn;
import littlecms.internal.lcms2_plugin._cmsOPTfreeDataFn;
import littlecms.internal.lcms2_plugin.cmsInterpParams;

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
	
	//XXX NOTE: The following 3 functions are modified from the original code so that _RemoveElement actually removes the element.
	
	// Remove an element in linked chain
	private static void _RemoveElement(cmsStage head)
	{
	    cmsStage mpe = (cmsStage)head.Next;
	    cmsStage next = (cmsStage)mpe.Next;
	    head.Next = next;
	    cmslut.cmsStageFree(mpe);
	}
	
	// Remove all identities in chain. Note that pt actually is a double pointer to the element that holds the pointer. 
	private static boolean _Remove1Op(cmsPipeline Lut, int UnaryOp)
	{
		cmsStage pt = Lut.Elements;
	    boolean AnyOpt = false;
	    boolean initialIm = false;
	    
	    if(pt == null)
	    {
	    	return AnyOpt;
	    }
	    
	    cmsStage previous = pt;
	    pt = (cmsStage)pt.Next;
	    initialIm = pt.Implements == UnaryOp;
	    
	    while (pt != null)
	    {
	        if ((pt).Implements == UnaryOp)
	        {
	            _RemoveElement(previous);
	            AnyOpt = true;
	        }
	        else
	        {
	        	previous = pt;
	        	pt = (cmsStage)((pt).Next);
	        }
	    }
	    
	    if(initialIm)
	    {
	    	pt = Lut.Elements;
	    	Lut.Elements = (cmsStage)pt.Next;
	    	cmslut.cmsStageFree(pt);
	    }
	    
	    return AnyOpt;
	}
	
	// Same, but only if two adjacent elements are found
	private static boolean _Remove2Op(cmsPipeline Lut, int Op1, int Op2)
	{   
	    cmsStage pt1;
	    cmsStage pt2;
	    boolean AnyOpt = false;
	    boolean initialIm = false;
	    
	    pt1 = Lut.Elements;
	    if (pt1 == null)
	    {
	    	return AnyOpt;
	    }
	    
	    cmsStage previous = pt1;
	    pt1 = (cmsStage)pt1.Next;
	    initialIm = pt1.Implements == Op1;
	    
	    while (pt1 != null)
	    {
	        pt2 = (cmsStage)((pt1).Next);
	        if (pt2 == null)
	        {
	        	return AnyOpt;
	        }
	        
	        if ((pt1).Implements == Op1 && (pt2).Implements == Op2)
	        {
	            _RemoveElement(pt1);
	            _RemoveElement(previous);
	            AnyOpt = true;
	        }
	        else
	        {
	        	pt1 = (cmsStage)((pt1).Next);            
	        }
	    }
	    
	    if(initialIm)
	    {
	    	pt1 = Lut.Elements;
	    	Lut.Elements = (cmsStage)pt1.Next;
	    	cmslut.cmsStageFree(pt1);
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
	
	private static final _cmsOPTfreeDataFn PrelinOpt16free = new _cmsOPTfreeDataFn()
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
	
	private static final _cmsOPTdupDataFn Prelin16dup = new _cmsOPTdupDataFn()
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
		    	InFloat[i] = (In[i] / 65535f);
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
	
	//TODO #369
}
