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

import littlecms.internal.lcms2.cmsCIELab;
import littlecms.internal.lcms2.cmsCIEXYZ;
import littlecms.internal.lcms2.cmsContext;
import littlecms.internal.lcms2.cmsPipeline;
import littlecms.internal.lcms2.cmsSAMPLER16;
import littlecms.internal.lcms2.cmsSAMPLERFLOAT;
import littlecms.internal.lcms2.cmsStage;
import littlecms.internal.lcms2.cmsToneCurve;
import littlecms.internal.lcms2_internal._cmsPipelineEvalFloatFn;
import littlecms.internal.lcms2_plugin._cmsDupUserDataFn;
import littlecms.internal.lcms2_plugin._cmsOPTeval16Fn;
import littlecms.internal.lcms2_plugin._cmsFreeUserDataFn;
import littlecms.internal.lcms2_plugin._cmsStageCLutData;
import littlecms.internal.lcms2_plugin._cmsStageDupElemFn;
import littlecms.internal.lcms2_plugin._cmsStageEvalFn;
import littlecms.internal.lcms2_plugin._cmsStageFreeElemFn;
import littlecms.internal.lcms2_plugin._cmsStageMatrixData;
import littlecms.internal.lcms2_plugin._cmsStageToneCurvesData;
import littlecms.internal.lcms2_plugin.cmsMAT3;
import littlecms.internal.lcms2_plugin.cmsVEC3;
import littlecms.internal.helper.VirtualPointer;

//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmslut
{
	// Allocates an empty multi profile element
	public static cmsStage _cmsStageAllocPlaceholder(cmsContext ContextID, int Type, int InputChannels, int OutputChannels, _cmsStageEvalFn EvalPtr,
			_cmsStageDupElemFn DupElemPtr, _cmsStageFreeElemFn FreePtr, Object Data)
	{
	    cmsStage ph = new cmsStage();
	    
	    ph.ContextID = ContextID;
	    
	    ph.Type       = Type;
	    ph.Implements = Type;   // By default, no clue on what is implementing
	    
	    ph.InputChannels  = InputChannels;
	    ph.OutputChannels = OutputChannels;
	    ph.EvalPtr        = EvalPtr;
	    ph.DupElemPtr     = DupElemPtr;
	    ph.FreePtr        = FreePtr;
	    ph.Data           = Data;
	    
	    return ph;
	}
	
	private static final _cmsStageEvalFn EvaluateIdentity = new _cmsStageEvalFn()
	{
		public void run(float[] In, float[] Out, cmsStage mpe)
		{
			System.arraycopy(In, 0, Out, 0, mpe.InputChannels);
		}
	};
	
	public static cmsStage cmsStageAllocIdentity(cmsContext ContextID, int nChannels)
	{
	    return _cmsStageAllocPlaceholder(ContextID, lcms2.cmsSigIdentityElemType, nChannels, nChannels, EvaluateIdentity, null, null, null);
	}
	
	// Conversion functions. From floating point to 16 bits
	private static void FromFloatTo16(final float[] In, short[] Out, int n)
	{
	    int i;
	    
	    for (i=0; i < n; i++)
	    {
	        Out[i] = lcms2_internal._cmsQuickSaturateWord(In[i] * 65535.0);
	    }
	}
	
	// From 16 bits to floating point
	private static void From16ToFloat(final short[] In, float[] Out, int n)
	{
	    int i;
	    
	    for (i=0; i < n; i++)
	    {
	        Out[i] = (In[i] & 0xFFFF) * (1F / 65535.0F);
	    }
	}
	
	// This function is quite useful to analyze the structure of a LUT and retrieve the MPE elements
	// that conform the LUT. It should be called with the LUT, the number of expected elements and
	// then a list of expected types followed with a list of cmsFloat64Number pointers to MPE elements. If
	// the function founds a match with current pipeline, it fills the pointers and returns TRUE
	// if not, returns FALSE without touching anything. Setting pointers to NULL does bypass
	// the storage process.
	public static boolean cmsPipelineCheckAndRetreiveStages(final cmsPipeline Lut, int n, Object[] stages)
	{
		int i;
	    cmsStage mpe;
	    int Type;
	    int stageL = 0;
	    
	    // Make sure same number of elements
	    if (cmsPipelineStageCount(Lut) != n)
	    {
	    	return false;
	    }
	    
	    // Iterate across asked types
	    mpe = Lut.Elements;
	    for (i = 0; i < n; i++)
	    {
	        // Get asked type
	    	Type = ((Integer)stages[stageL++]).intValue();
	        if (mpe.Type != Type)
	        {
	            // Mismatch. We are done.
	            return false;
	        }
	        mpe = (cmsStage)mpe.Next;
	    }
	    
	    // Found a combination, fill pointers if not NULL
	    mpe = Lut.Elements;
	    for (i = 0; i < n; i++)
	    {
	    	stages[stageL++] = mpe; //Usually checks to make sure destination not null but no need to here
	        mpe = (cmsStage)mpe.Next;
	    }
	    
	    return true;
	}
	
	// Below there are implementations for several types of elements. Each type may be implemented by a
	// evaluation function, a duplication function, a function to free resources and a constructor.
	
	// *************************************************************************************************
	// Type cmsSigCurveSetElemType (curves)
	// *************************************************************************************************
	
	public static cmsToneCurve[] _cmsStageGetPtrToCurveSet(final cmsStage mpe)
	{
	    _cmsStageToneCurvesData Data = (_cmsStageToneCurvesData)mpe.Data;
	    
	    return Data.TheCurves;
	}
	
	private static final _cmsStageEvalFn EvaluateCurves = new _cmsStageEvalFn()
	{
		public void run(final float[] In, float[] Out, final cmsStage mpe)
		{
			_cmsStageToneCurvesData Data;
		    int i;
			
			lcms2_internal._cmsAssert(mpe != null, "mpe != null");
			
			Data = (_cmsStageToneCurvesData)mpe.Data;
		    if (Data == null)
		    {
		    	return;
		    }
		    
		    if (Data.TheCurves == null)
		    {
		    	return;
		    }
		    
		    for (i=0; i < Data.nCurves; i++)
		    {
		    	Out[i] = cmsgamma.cmsEvalToneCurveFloat(Data.TheCurves[i], In[i]);
		    }
		}
	};
	
	private static final _cmsStageFreeElemFn CurveSetElemTypeFree = new _cmsStageFreeElemFn()
	{
		public void run(cmsStage mpe)
		{
			_cmsStageToneCurvesData Data;
		    int i;
		    
		    lcms2_internal._cmsAssert(mpe != null, "mpe != null");
		    
		    Data = (_cmsStageToneCurvesData)mpe.Data;
		    if (Data == null)
		    {
		    	return;
		    }
		    
		    if (Data.TheCurves != null)
		    {
		        for (i=0; i < Data.nCurves; i++)
		        {
		            if (Data.TheCurves[i] != null)
		            {
		            	cmsgamma.cmsFreeToneCurve(Data.TheCurves[i]);
		            }
		        }
		    }
		    Data.TheCurves = null;
		    mpe.Data = null;
		    //_cmsFree(mpe.ContextID, Data.TheCurves);
		    //_cmsFree(mpe.ContextID, Data);
		}
	};
	
	private static final _cmsStageDupElemFn CurveSetDup = new _cmsStageDupElemFn()
	{
		public Object run(cmsStage mpe)
		{
			_cmsStageToneCurvesData Data = (_cmsStageToneCurvesData)mpe.Data;
		    _cmsStageToneCurvesData NewElem;
		    int i;
		    
		    NewElem = new _cmsStageToneCurvesData();
		    
		    NewElem.nCurves   = Data.nCurves;
		    NewElem.TheCurves = new cmsToneCurve[NewElem.nCurves];
		    
		    for (i=0; i < NewElem.nCurves; i++)
		    {
		        // Duplicate each curve. It may fail.
		        NewElem.TheCurves[i] = cmsgamma.cmsDupToneCurve(Data.TheCurves[i]);
		        if (NewElem.TheCurves[i] == null)
		        {
		        	if (NewElem.TheCurves != null)
				    {
				        for (i=0; i < NewElem.nCurves; i++)
				        {
				            if (NewElem.TheCurves[i] != null)
				            {
				            	cmsgamma.cmsFreeToneCurve(Data.TheCurves[i]);
				            }
				        }
				    }
				    Data.TheCurves = null;
				    NewElem = null;
				    //_cmsFree(mpe.ContextID, Data.TheCurves);
				    //_cmsFree(mpe.ContextID, NewElem);
				    return null;
		        }
		    }
		    return NewElem;
		}
	};
	
	// Curves == NULL forces identity curves
	public static cmsStage cmsStageAllocToneCurves(cmsContext ContextID, int nChannels, final cmsToneCurve[] Curves)
	{
	    int i;
	    _cmsStageToneCurvesData NewElem;
	    cmsStage NewMPE;
	    
	    NewMPE = _cmsStageAllocPlaceholder(ContextID, lcms2.cmsSigCurveSetElemType, nChannels, nChannels, EvaluateCurves, CurveSetDup, CurveSetElemTypeFree, null );
	    if (NewMPE == null)
	    {
	    	return null;
	    }
	    
	    NewElem = new _cmsStageToneCurvesData();
	    
	    NewMPE.Data = NewElem;
	    
	    NewElem.nCurves   = nChannels;
	    NewElem.TheCurves = new cmsToneCurve[nChannels];
	    
	    for (i=0; i < nChannels; i++)
	    {
	        if (Curves == null)
	        {
	            NewElem.TheCurves[i] = cmsgamma.cmsBuildGamma(ContextID, 1.0);
	        }
	        else
	        {
	            NewElem.TheCurves[i] = cmsgamma.cmsDupToneCurve(Curves[i]);
	        }
	        
	        if (NewElem.TheCurves[i] == null)
	        {
	            cmsStageFree(NewMPE);
	            return null;
	        }
	    }
	    
	    return NewMPE;
	}
	
	// Create a bunch of identity curves
	public static cmsStage _cmsStageAllocIdentityCurves(cmsContext ContextID, int nChannels)
	{
	    cmsStage mpe = cmsStageAllocToneCurves(ContextID, nChannels, null);
	    
	    if (mpe == null)
	    {
	    	return null;
	    }
	    mpe.Implements = lcms2.cmsSigIdentityElemType;
	    return mpe;
	}
	
	// *************************************************************************************************
	// Type cmsSigMatrixElemType (Matrices)
	// *************************************************************************************************
	
	// Special care should be taken here because precision loss. A temporary cmsFloat64Number buffer is being used
	private static final _cmsStageEvalFn EvaluateMatrix = new _cmsStageEvalFn()
	{
		public void run(float[] In, float[] Out, cmsStage mpe)
		{
			int i, j;
		    _cmsStageMatrixData Data = (_cmsStageMatrixData)mpe.Data;
		    double Tmp;
		    
		    // Input is already in 0..1.0 notation
		    for (i=0; i < mpe.OutputChannels; i++)
		    {
		        Tmp = 0;
		        for (j=0; j < mpe.InputChannels; j++)
		        {
		            Tmp += In[j] * Data.Double[i*mpe.InputChannels + j];
		        }
		        
		        if (Data.Offset != null)
		        {
		        	Tmp += Data.Offset[i];
		        }
		        
		        Out[i] = (float)Tmp;
		    }
		    
		    // Output in 0..1.0 domain
		}
	};
	
	// Duplicate a yet-existing matrix element
	private static final _cmsStageDupElemFn MatrixElemDup = new _cmsStageDupElemFn()
	{
		public Object run(cmsStage mpe)
		{
			_cmsStageMatrixData Data = (_cmsStageMatrixData)mpe.Data;
		    _cmsStageMatrixData NewElem;
		    int sz;
		    
		    NewElem = new _cmsStageMatrixData();
		    
		    sz = mpe.InputChannels * mpe.OutputChannels;
		    
		    NewElem.Double = new double[sz];
		    System.arraycopy(Data.Double, 0, NewElem.Double, 0, sz);
		    
		    if (Data.Offset != null)
		    {
		        NewElem.Offset = new double[mpe.OutputChannels];
		        System.arraycopy(Data.Offset, 0, NewElem.Offset, 0, mpe.OutputChannels);
		    }
		    
		    return NewElem;
		}
	};
	
	private static final _cmsStageFreeElemFn MatrixElemTypeFree = new _cmsStageFreeElemFn()
	{
		public void run(cmsStage mpe)
		{
			_cmsStageMatrixData Data = (_cmsStageMatrixData)mpe.Data;
		    if (Data.Double != null)
		    {
		    	Data.Double = null;
		    	//_cmsFree(mpe.ContextID, Data.Double);
		    }
		    
		    if (Data.Offset != null)
		    {
		    	Data.Offset = null;
		    	//_cmsFree(mpe.ContextID, Data.Offset);
		    }
		    
		    //_cmsFree(mpe.ContextID, mpe.Data);
		}
	};
	
	public static cmsStage cmsStageAllocMatrix(cmsContext ContextID, int Rows, int Cols, final double[] Matrix, final double[] Offset)
	{
		int i, n;
		_cmsStageMatrixData NewElem;
		cmsStage NewMPE;
		
		n = Rows * Cols;
		
		// Check for overflow
		if (n == 0)
		{
			return null;
		}
	    if (n >= 0xffffffffL / Cols)
	    {
	    	return null;
	    }
	    if (n >= 0xffffffffL / Rows)
	    {
	    	return null;
	    }
		if (n < Rows || n < Cols)
		{
			return null;
		}
		
		NewMPE = _cmsStageAllocPlaceholder(ContextID, lcms2.cmsSigMatrixElemType, Cols, Rows, EvaluateMatrix, MatrixElemDup, MatrixElemTypeFree, null);
		if (NewMPE == null)
		{
			return null;
		}
		
		NewElem = new _cmsStageMatrixData();
		
		NewElem.Double = new double[n];
		
		for (i=0; i < n; i++)
		{
			NewElem.Double[i] = Matrix[i];
		}
		
		
		if (Offset != null)
		{
			NewElem.Offset = new double[Cols];
			
			for (i=0; i < Cols; i++)
			{
				NewElem.Offset[i] = Offset[i];
			}
		}
		
		NewMPE.Data = NewElem;
		return NewMPE;
	}
	
	// *************************************************************************************************
	// Type cmsSigCLutElemType
	// *************************************************************************************************
	
	// Evaluate in true floating point
	private static final _cmsStageEvalFn EvaluateCLUTfloat = new _cmsStageEvalFn()
	{
		public void run(float[] In, float[] Out, cmsStage mpe)
		{
			_cmsStageCLutData Data = (_cmsStageCLutData)mpe.Data;
			
		    Data.Params.Interpolation.getFloat().run(In, Out, Data.Params);
		}
	};
	
	// Convert to 16 bits, evaluate, and back to floating point
	private static final _cmsStageEvalFn EvaluateCLUTfloatIn16 = new _cmsStageEvalFn()
	{
		public void run(float[] In, float[] Out, cmsStage mpe)
		{
			_cmsStageCLutData Data = (_cmsStageCLutData)mpe.Data;
		    short[] In16 = new short[lcms2_internal.MAX_STAGE_CHANNELS], Out16 = new short[lcms2_internal.MAX_STAGE_CHANNELS];
		    
		    lcms2_internal._cmsAssert(mpe.InputChannels  <= lcms2_internal.MAX_STAGE_CHANNELS, "mpe.InputChannels  <= lcms2_internal.MAX_STAGE_CHANNELS");
		    lcms2_internal._cmsAssert(mpe.OutputChannels <= lcms2_internal.MAX_STAGE_CHANNELS, "mpe.OutputChannels <= lcms2_internal.MAX_STAGE_CHANNELS");
		    
		    FromFloatTo16(In, In16, mpe.InputChannels);
		    Data.Params.Interpolation.get16().run(In16, Out16, Data.Params);
		    From16ToFloat(Out16, Out,  mpe.OutputChannels);
		}
	};
	
	// Given an hypercube of b dimensions, with Dims[] number of nodes by dimension, calculate the total amount of nodes
	private static int CubeSize(final int[] Dims, int b)
	{
	    int rv, dim;
	    
	    lcms2_internal._cmsAssert(Dims != null, "Dims != null");
	    
	    for (rv = 1; b > 0; b--)
	    {
	        dim = Dims[b-1];
	        if (dim == 0)
	        {
	        	return 0; // Error
	        }
	        
	        rv *= dim;
	        
	        // Check for overflow
	        if (rv > 0xffffffffL / dim)
	        {
	        	return 0;
	        }
	    }
	    
	    return rv;
	}
	
	private static final _cmsStageDupElemFn CLUTElemDup = new _cmsStageDupElemFn()
	{
		public Object run(cmsStage mpe)
		{
			_cmsStageCLutData Data = (_cmsStageCLutData)mpe.Data;
		    _cmsStageCLutData NewElem;
		    
		    NewElem = new _cmsStageCLutData();
		    
		    NewElem.nEntries       = Data.nEntries;
		    NewElem.HasFloatValues = Data.HasFloatValues;
		    
		    if (Data.Tab != null && !Data.Tab.isFree())
		    {
		        if (Data.HasFloatValues)
		        {
		        	NewElem.Tab = cmserr._cmsDupMem(mpe.ContextID, Data.Tab, Data.nEntries * /*sizeof(cmsFloat32Number)*/4);
		        }
		        else
		        {
		        	NewElem.Tab = cmserr._cmsDupMem(mpe.ContextID, Data.Tab, Data.nEntries * /*sizeof(cmsUInt16Number)*/2);
		        }
		    }
		    
		    NewElem.Params   = cmsintrp._cmsComputeInterpParamsEx(mpe.ContextID,
		                                                   Data.Params.nSamples,
		                                                   Data.Params.nInputs,
		                                                   Data.Params.nOutputs,
		                                                   NewElem.Tab,
		                                                   Data.Params.dwFlags);
		    
		    return NewElem;
		}
	};
	
	private static final _cmsStageFreeElemFn CLutElemTypeFree = new _cmsStageFreeElemFn()
	{
		public void run(cmsStage mpe)
		{
			_cmsStageCLutData Data = (_cmsStageCLutData)mpe.Data;
		    
		    // Already empty
		    if (Data == null)
		    {
		    	return;
		    }
		    
		    // This works for both types
		    if (Data.Tab != null && !Data.Tab.isFree())
		    {
		    	cmserr._cmsFree(mpe.ContextID, Data.Tab);
		    }
		    
		    cmsintrp._cmsFreeInterpParams(Data.Params);
		    //_cmsFree(mpe.ContextID, mpe.Data);
		    mpe.Data = null;
		}
	};
	
	// Allocates a 16-bit multidimensional CLUT. This is evaluated at 16-bit precision. Table may have different
	// granularity on each dimension.
	public static cmsStage cmsStageAllocCLut16bitGranular(cmsContext ContextID, final int[] clutPoints, int inputChan, int outputChan, final short[] Table)
	{
		int i, n;
		_cmsStageCLutData NewElem;
		cmsStage NewMPE;
		
		NewMPE = _cmsStageAllocPlaceholder(ContextID, lcms2.cmsSigCLutElemType, inputChan, outputChan,
		        EvaluateCLUTfloatIn16, CLUTElemDup, CLutElemTypeFree, null );
		
		if (NewMPE == null)
		{
			return null;
		}
		
		NewElem = new _cmsStageCLutData();
		
		NewMPE.Data = NewElem;
		
		NewElem.nEntries = n = outputChan * CubeSize(clutPoints, inputChan);
		NewElem.HasFloatValues = false;
		
		if (n == 0)
		{
	        cmsStageFree(NewMPE);
	        return null;
	    }
		
		NewElem.Tab  = cmserr._cmsCalloc(ContextID, n, /*sizeof(cmsUInt16Number)*/2);
		if (NewElem.Tab == null)
		{
			return null;
		}
		
		if (Table != null)
		{
			VirtualPointer.TypeProcessor proc = NewElem.Tab.getProcessor();
			int pos = NewElem.Tab.getPosition();
			for (i=0; i < n; i++)
			{
				proc.write(Table[i], true);
			}
			NewElem.Tab.setPosition(pos);
		}
		
		NewElem.Params = cmsintrp._cmsComputeInterpParamsEx(ContextID, clutPoints, inputChan, outputChan, NewElem.Tab, lcms2_plugin.CMS_LERP_FLAGS_16BITS);
		if (NewElem.Params == null)
		{
			return null;
		}
		
		return NewMPE;
	}
	
	public static cmsStage cmsStageAllocCLut16bit(cmsContext ContextID, int nGridPoints, int inputChan, int outputChan, final short[] Table)
	{
		int[] Dimensions = new int[lcms2_plugin.MAX_INPUT_DIMENSIONS];
		int i;
		
		// Our resulting LUT would be same gridpoints on all dimensions
		for (i=0; i < lcms2_plugin.MAX_INPUT_DIMENSIONS; i++)
		{
			Dimensions[i] = nGridPoints;
		}
		
		return cmsStageAllocCLut16bitGranular(ContextID, Dimensions, inputChan, outputChan, Table);
	}
	
	
	public static cmsStage cmsStageAllocCLutFloat(cmsContext ContextID, int nGridPoints, int inputChan, int outputChan, final float[] Table)
	{
		int[] Dimensions = new int[lcms2_plugin.MAX_INPUT_DIMENSIONS];
		int i;
		
		// Our resulting LUT would be same gridpoints on all dimensions
		for (i=0; i < lcms2_plugin.MAX_INPUT_DIMENSIONS; i++)
		{
			Dimensions[i] = nGridPoints;
		}
		
		return cmsStageAllocCLutFloatGranular(ContextID, Dimensions, inputChan, outputChan, Table);
	}
	
	public static cmsStage cmsStageAllocCLutFloatGranular(cmsContext ContextID, final int[] clutPoints, int inputChan, int outputChan, final float[] Table)
	{
		int i, n;
		_cmsStageCLutData NewElem;
		cmsStage NewMPE;
		
		lcms2_internal._cmsAssert(clutPoints != null, "clutPoints != null");
		
		NewMPE = _cmsStageAllocPlaceholder(ContextID, lcms2.cmsSigCLutElemType, inputChan, outputChan,
		        EvaluateCLUTfloat, CLUTElemDup, CLutElemTypeFree, null );
		
		if (NewMPE == null)
		{
			return null;
		}
		
		NewElem = new _cmsStageCLutData();
		
		NewMPE.Data = NewElem;

	    // There is a potential integer overflow on conputing n and nEntries.
		NewElem.nEntries = n = outputChan * CubeSize(clutPoints, inputChan);
		NewElem.HasFloatValues = true;
		
		if (n == 0)
		{
	        cmsStageFree(NewMPE);
	        return null;
	    }
		
		NewElem.Tab  = cmserr._cmsCalloc(ContextID, n, /*sizeof(cmsFloat32Number)*/4);
		if (NewElem.Tab == null)
		{
			return null;
		}
		
		if (Table != null)
		{
			VirtualPointer.TypeProcessor proc = NewElem.Tab.getProcessor();
			int pos = NewElem.Tab.getPosition();
			for (i=0; i < n; i++)
			{
				proc.write(Table[i], true);
			}
			NewElem.Tab.setPosition(pos);
		}
		
		NewElem.Params = cmsintrp._cmsComputeInterpParamsEx(ContextID, clutPoints, inputChan, outputChan, NewElem.Tab, lcms2_plugin.CMS_LERP_FLAGS_FLOAT);
		if (NewElem.Params == null)
		{
			return null;
		}
		
		return NewMPE;
	}
	
	private static final cmsSAMPLER16 IdentitySampler = new cmsSAMPLER16()
	{
		public int run(short[] In, short[] Out, Object Cargo)
		{
			int nChan = ((Integer)Cargo).intValue();
		    int i;
		    
		    for (i=0; i < nChan; i++)
		    {
		    	Out[i] = In[i];
		    }
		    
		    return 1;
		}
	};
	
	// Creates an MPE that just copies input to output
	public static cmsStage _cmsStageAllocIdentityCLut(cmsContext ContextID, int nChan)
	{
	    int[] Dimensions = new int[lcms2_plugin.MAX_INPUT_DIMENSIONS];
	    cmsStage mpe ;
	    int i;
	    
	    for (i=0; i < lcms2_plugin.MAX_INPUT_DIMENSIONS; i++)
	    {
	    	Dimensions[i] = 2;
	    }
	    
	    mpe = cmsStageAllocCLut16bitGranular(ContextID, Dimensions, nChan, nChan, null);
	    if (mpe == null)
	    {
	    	return null;
	    }
	    
	    if (!cmsStageSampleCLut16bit(mpe, IdentitySampler, new Integer(nChan), 0))
	    {
	        cmsStageFree(mpe);
	        return null;
	    }
	    
	    mpe.Implements = lcms2.cmsSigIdentityElemType;
	    return mpe;
	}
	
	// Quantize a value 0 <= i < MaxSamples to 0..0xffff
	public static short _cmsQuantizeVal(double i, int MaxSamples)
	{
	    double x;
	    
	    x = (i * 65535.0) / (MaxSamples - 1);
	    return lcms2_internal._cmsQuickSaturateWord(x);
	}
	
	// This routine does a sweep on whole input space, and calls its callback
	// function on knots. returns TRUE if all ok, FALSE otherwise.
	public static boolean cmsStageSampleCLut16bit(cmsStage mpe, cmsSAMPLER16 Sampler, Object Cargo, int dwFlags)
	{
	    int i, t, nTotalPoints, index, rest;
	    int nInputs, nOutputs;
	    int[] nSamples;
	    short[] In = new short[lcms2.cmsMAXCHANNELS], Out = new short[lcms2_internal.MAX_STAGE_CHANNELS];
	    _cmsStageCLutData clut;
	    
	    if (mpe == null)
	    {
	    	return false;
	    }
	    
	    clut = (_cmsStageCLutData)mpe.Data;
	    
	    if (clut == null)
	    {
	    	return false;
	    }
	    
	    nSamples = clut.Params.nSamples;
	    nInputs  = clut.Params.nInputs;
	    nOutputs = clut.Params.nOutputs;
	    
	    if (nInputs >= lcms2.cmsMAXCHANNELS)
	    {
	    	return false;
	    }
	    if (nOutputs >= lcms2_internal.MAX_STAGE_CHANNELS)
	    {
	    	return false;
	    }
	    
	    nTotalPoints = CubeSize(nSamples, nInputs);
	    if (nTotalPoints == 0)
	    {
	    	return false;
	    }
	    
	    index = 0;
	    
	    for (i = 0; i < nTotalPoints; i++)
	    {
	        rest = i;
	        for (t = nInputs-1; t >=0; --t)
	        {
	            int Colorant = rest % nSamples[t];
	            
	            rest /= nSamples[t];
	            
	            In[t] = _cmsQuantizeVal(Colorant, nSamples[t]);
	        }
	        
	        if (clut.Tab != null)
	        {
	        	VirtualPointer.TypeProcessor proc = clut.Tab.getProcessor();
	        	int pos = clut.Tab.getPosition();
	            for (t=0; t < nOutputs; t++)
	            {
	            	clut.Tab.setPosition(pos + ((index + t) * 2));
	            	Out[t] = proc.readInt16();
	            }
	            clut.Tab.setPosition(pos);
	        }
	        
	        if (Sampler.run(In, Out, Cargo) == 0)
	        {
	        	return false;
	        }
	        
	        if ((dwFlags & lcms2.SAMPLER_INSPECT) == 0)
	        {
	            if (clut.Tab != null)
	            {
	            	VirtualPointer.TypeProcessor proc = clut.Tab.getProcessor();
	            	int pos = clut.Tab.getPosition();
	                for (t=0; t < nOutputs; t++)
	                {
	                	clut.Tab.setPosition(pos + ((index + t) * 2));
	                	proc.write(Out[t]);
	                }
	                clut.Tab.setPosition(pos);
	            }
	        }

	        index += nOutputs;
	    }
	    
	    return true;
	}
	
	// Same as anterior, but for floating point
	public static boolean cmsStageSampleCLutFloat(cmsStage mpe, cmsSAMPLERFLOAT Sampler, Object Cargo, int dwFlags)
	{
	    int i, t, nTotalPoints, index, rest;
	    int nInputs, nOutputs;
	    int[] nSamples;
	    float[] In = new float[lcms2.cmsMAXCHANNELS], Out = new float[lcms2_internal.MAX_STAGE_CHANNELS];
	    _cmsStageCLutData clut = (_cmsStageCLutData)mpe.Data;
	    
	    nSamples = clut.Params.nSamples;
	    nInputs  = clut.Params.nInputs;
	    nOutputs = clut.Params.nOutputs;
	    
	    if (nInputs >= lcms2.cmsMAXCHANNELS)
	    {
	    	return false;
	    }
	    if (nOutputs >= lcms2_internal.MAX_STAGE_CHANNELS)
	    {
	    	return false;
	    }
	    
	    nTotalPoints = CubeSize(nSamples, nInputs);
	    if (nTotalPoints == 0)
	    {
	    	return false;
	    }
	    
	    index = 0;
	    for (i = 0; i < nTotalPoints; i++)
	    {
	        rest = i;
	        for (t = nInputs-1; t >=0; --t)
	        {
	        	int Colorant = rest % nSamples[t];
	        	
	            rest /= nSamples[t];
	            
	            In[t] = (_cmsQuantizeVal(Colorant, nSamples[t]) * (1f / 65535f));
	        }
	        
	        if (clut.Tab != null)
	        {
	        	VirtualPointer.TypeProcessor proc = clut.Tab.getProcessor();
	        	int pos = clut.Tab.getPosition();
	            for (t=0; t < nOutputs; t++)
	            {
	            	clut.Tab.setPosition(pos + ((index + t) * 4));
	            	Out[t] = proc.readFloat();
	            }
	            clut.Tab.setPosition(pos);
	        }

	        if (Sampler.run(In, Out, Cargo) == 0)
	        {
	        	return false;
	        }
	        
	        if ((dwFlags & lcms2.SAMPLER_INSPECT) == 0)
	        {
	            if (clut.Tab != null)
	            {
	            	VirtualPointer.TypeProcessor proc = clut.Tab.getProcessor();
	            	int pos = clut.Tab.getPosition();
	                for (t=0; t < nOutputs; t++)
	                {
	                	clut.Tab.setPosition(pos + ((index + t) * 4));
	                	proc.write(Out[t]);
	                }
	                clut.Tab.setPosition(pos);
	            }
	        }

	        index += nOutputs;
	    }
	    
	    return true;
	}
	
	// This routine does a sweep on whole input space, and calls its callback
	// function on knots. returns TRUE if all ok, FALSE otherwise.
	public static boolean cmsSliceSpace16(int nInputs, final int[] clutPoints, cmsSAMPLER16 Sampler, Object Cargo)
	{
	    int i, t, nTotalPoints, rest;
	    short[] In = new short[lcms2.cmsMAXCHANNELS];
	    
	    if (nInputs >= lcms2.cmsMAXCHANNELS)
	    {
	    	return false;
	    }
	    
	    nTotalPoints = CubeSize(clutPoints, nInputs);
	    if (nTotalPoints == 0)
	    {
	    	return false;
	    }
	    
	    for (i = 0; i < nTotalPoints; i++)
	    {
	        rest = i;
	        for (t = nInputs-1; t >=0; --t)
	        {
	            int Colorant = (int)(((long)rest) % (clutPoints[t] & 0xFFFFFFFFL));
	            
	            rest /= (clutPoints[t] & 0xFFFFFFFFL);
	            In[t] = _cmsQuantizeVal(Colorant, clutPoints[t]);
	        }
	        
	        if (Sampler.run(In, null, Cargo) == 0)
	        {
	        	return false;
	        }
	    }
	    
	    return true;
	}
	
	public static boolean cmsSliceSpaceFloat(int nInputs, final int[] clutPoints, cmsSAMPLERFLOAT Sampler, Object Cargo)
	{
	    int i, t, nTotalPoints, rest;
	    float[] In = new float[lcms2.cmsMAXCHANNELS];
	    
	    if (nInputs >= lcms2.cmsMAXCHANNELS)
	    {
	    	return false;
	    }
	    
	    nTotalPoints = CubeSize(clutPoints, nInputs);
	    if (nTotalPoints == 0)
	    {
	    	return false;
	    }
	    
	    for (i = 0; i < nTotalPoints; i++)
	    {
	        rest = i;
	        for (t = nInputs-1; t >=0; --t)
	        {
	        	int Colorant = rest % clutPoints[t];
	        	
	            rest /= clutPoints[t];
	            In[t] = (_cmsQuantizeVal(Colorant, clutPoints[t]) * (1f / 65535f));
	        }
	        
	        if (Sampler.run(In, null, Cargo) == 0)
	        {
	        	return false;
	        }
	    }
	    
	    return true;
	}
	
	// ********************************************************************************
	// Type cmsSigLab2XYZElemType
	// ********************************************************************************
	
	private static final _cmsStageEvalFn EvaluateLab2XYZ = new _cmsStageEvalFn()
	{
		public void run(float[] In, float[] Out, cmsStage mpe)
		{
			cmsCIELab Lab = new cmsCIELab();
		    cmsCIEXYZ XYZ = new cmsCIEXYZ();
		    final double XYZadj = 1.0 / lcms2_internal.MAX_ENCODEABLE_XYZ;
		    
		    // V4 rules
		    Lab.L = In[0] * 100.0;
		    Lab.a = In[1] * 255.0 - 128.0;
		    Lab.b = In[2] * 255.0 - 128.0;
		    
		    cmspcs.cmsLab2XYZ(null, XYZ, Lab);
		    
		    // From XYZ, range 0..19997 to 0..1.0, note that 1.99997 comes from 0xffff
		    // encoded as 1.15 fixed point, so 1 + (32767.0 / 32768.0)
		    
		    Out[0] = (float)(XYZ.X * XYZadj);
		    Out[1] = (float)(XYZ.Y * XYZadj);
		    Out[2] = (float)(XYZ.Z * XYZadj);
		}
	};
	
	// No dup or free routines needed, as the structure has no pointers in it.
	public static cmsStage _cmsStageAllocLab2XYZ(cmsContext ContextID)
	{
	    return _cmsStageAllocPlaceholder(ContextID, lcms2.cmsSigLab2XYZElemType, 3, 3, EvaluateLab2XYZ, null, null, null);
	}
	
	// ********************************************************************************
	
	// v2 L=100 is supposed to be placed on 0xFF00. There is no reasonable
	// number of gridpoints that would make exact match. However, a prelinearization
	// of 258 entries, would map 0xFF00 exactly on entry 257, and this is good to avoid scum dot.
	// Almost all what we need but unfortunately, the rest of entries should be scaled by
	// (255*257/256) and this is not exact.
	
	public static cmsStage _cmsStageAllocLabV2ToV4curves(cmsContext ContextID)
	{
	    cmsStage mpe;
	    cmsToneCurve[] LabTable = new cmsToneCurve[3];
	    int i, j;
	    
	    LabTable[0] = cmsgamma.cmsBuildTabulatedToneCurve16(ContextID, 258, null);
	    LabTable[1] = cmsgamma.cmsBuildTabulatedToneCurve16(ContextID, 258, null);
	    LabTable[2] = cmsgamma.cmsBuildTabulatedToneCurve16(ContextID, 258, null);
	    
	    for (j=0; j < 3; j++)
	    {
	        if (LabTable[j] == null)
	        {
	            cmsgamma.cmsFreeToneCurveTriple(LabTable);
	            return null;
	        }
	        
	        // We need to map * (0xffff / 0xff00), thats same as (257 / 256)
	        // So we can use 258-entry tables to do the trick (i / 257) * (255 * 257) * (257 / 256);
	        for (i=0; i < 257; i++)
	        {
	        	LabTable[j].Table16[i] = (short)((i * 0xffff + 0x80) >> 8);
	        }
	        
	        LabTable[j].Table16[257] = (short)0xffff;
	    }
	    
	    mpe = cmsStageAllocToneCurves(ContextID, 3, LabTable);
	    cmsgamma.cmsFreeToneCurveTriple(LabTable);
	    
	    mpe.Implements = lcms2.cmsSigLabV2toV4;
	    return mpe;
	}
	
	// ********************************************************************************
	
	// Matrix-based conversion, which is more accurate, but slower and cannot properly be saved in devicelink profiles
	public static cmsStage _cmsStageAllocLabV2ToV4(cmsContext ContextID)
	{
	    final double[] V2ToV4 = { 65535.0/65280.0, 0, 0,
	                                     0, 65535.0/65280.0, 0,
	                                     0, 0, 65535.0/65280.0
	                                     };
	    
	    cmsStage mpe = cmsStageAllocMatrix(ContextID, 3, 3, V2ToV4, null);
	    
	    if (mpe == null)
	    {
	    	return mpe;
	    }
	    mpe.Implements = lcms2.cmsSigLabV2toV4;
	    return mpe;
	}
	
	// Reverse direction
	public static cmsStage _cmsStageAllocLabV4ToV2(cmsContext ContextID)
	{
	    final double[] V4ToV2 = { 65280.0/65535.0, 0, 0,
	                                     0, 65280.0/65535.0, 0,
	                                     0, 0, 65280.0/65535.0
	                                     };
	    
	     cmsStage mpe = cmsStageAllocMatrix(ContextID, 3, 3, V4ToV2, null);
	     
	    if (mpe == null)
	    {
	    	return mpe;
	    }
	    mpe.Implements = lcms2.cmsSigLabV4toV2;
	    return mpe;
	}
	
	// To Lab to float. Note that the MPE gives numbers in normal Lab range
	// and we need 0..1.0 range for the formatters
	// L* : 0...100 => 0...1.0  (L* / 100)
	// ab* : -128..+127 to 0..1  ((ab* + 128) / 255)
	
	public static cmsStage _cmsStageNormalizeFromLabFloat(cmsContext ContextID)
	{
		final double[] a1 = {
	        1.0/100.0, 0, 0,
	        0, 1.0/255.0, 0,
	        0, 0, 1.0/255.0
	    };
	    
		final double[] o1 = {
	        0,
	        128.0/255.0,
	        128.0/255.0
	    };
	    
	    return cmsStageAllocMatrix(ContextID, 3, 3, a1, o1);
	}
	
	public static cmsStage _cmsStageNormalizeFromXyzFloat(cmsContext ContextID)
	{
		final double[] a1 = {
	        1.0/100.0, 0, 0,
	        0, 1.0/100.0, 0,
	        0, 0, 1.0/100.0
	    };
	    
	    return cmsStageAllocMatrix(ContextID, 3, 3, a1, null);
	}
	
	public static cmsStage _cmsStageNormalizeToLabFloat(cmsContext ContextID)
	{
		final double[] a1 = {
	        100.0, 0, 0,
	        0, 255.0, 0,
	        0, 0, 255.0
	    };
		
		final double[] o1 = {
	        0,
	        -128.0,
	        -128.0
	    };
		
	    return cmsStageAllocMatrix(ContextID, 3, 3, a1, o1);
	}
	
	public static cmsStage _cmsStageNormalizeToXyzFloat(cmsContext ContextID)
	{
		final double[] a1 = {
	        100.0, 0, 0,
	        0, 100.0, 0,
	        0, 0, 100.0
	    };
	    
	    return cmsStageAllocMatrix(ContextID, 3, 3, a1, null);
	}
	
	// ********************************************************************************
	// Type cmsSigXYZ2LabElemType
	// ********************************************************************************
	
	private static final _cmsStageEvalFn EvaluateXYZ2Lab = new _cmsStageEvalFn()
	{
		public void run(float[] In, float[] Out, cmsStage mpe)
		{
			cmsCIELab Lab = new cmsCIELab();
		    cmsCIEXYZ XYZ = new cmsCIEXYZ();
		    final double XYZadj = lcms2_internal.MAX_ENCODEABLE_XYZ;
		    
		    // From 0..1.0 to XYZ
		    
		    XYZ.X = In[0] * XYZadj;
		    XYZ.Y = In[1] * XYZadj;
		    XYZ.Z = In[2] * XYZadj;
		    
		    cmspcs.cmsXYZ2Lab(null, Lab, XYZ);
		    
		    // From V4 Lab to 0..1.0
		    
		    Out[0] = (float)(Lab.L / 100.0);
		    Out[1] = (float)((Lab.a + 128.0) / 255.0);
		    Out[2] = (float)((Lab.b + 128.0) / 255.0);
		}
	};
	
	public static cmsStage _cmsStageAllocXYZ2Lab(cmsContext ContextID)
	{
	    return _cmsStageAllocPlaceholder(ContextID, lcms2.cmsSigXYZ2LabElemType, 3, 3, EvaluateXYZ2Lab, null, null, null);
	}
	
	// ********************************************************************************
	
	// For v4, S-Shaped curves are placed in a/b axis to increase resolution near gray
	
	public static cmsStage _cmsStageAllocLabPrelin(cmsContext ContextID)
	{
	    cmsToneCurve[] LabTable = new cmsToneCurve[3];
	    double[] Params =  {2.4};
	    
	    LabTable[0] = cmsgamma.cmsBuildGamma(ContextID, 1.0);
	    LabTable[1] = cmsgamma.cmsBuildParametricToneCurve(ContextID, 108, Params);
	    LabTable[2] = cmsgamma.cmsBuildParametricToneCurve(ContextID, 108, Params);
	    
	    return cmsStageAllocToneCurves(ContextID, 3, LabTable);
	}
	
	// Free a single MPE
	public static void cmsStageFree(cmsStage mpe)
	{
	    if (mpe.FreePtr != null)
	    {
	    	mpe.FreePtr.run(mpe);
	    }
	    
	    //_cmsFree(mpe.ContextID, mpe);
	}
	
	public static int cmsStageInputChannels(final cmsStage mpe)
	{
	    return mpe.InputChannels;
	}
	
	public static int cmsStageOutputChannels(final cmsStage mpe)
	{
	    return mpe.OutputChannels;
	}
	
	public static int cmsStageType(final cmsStage mpe)
	{
	    return mpe.Type;
	}
	
	public static Object cmsStageData(final cmsStage mpe)
	{
	    return mpe.Data;
	}
	
	public static cmsStage cmsStageNext(final cmsStage mpe)
	{
	    return (cmsStage)mpe.Next;
	}
	
	// Duplicates an MPE
	public static cmsStage cmsStageDup(cmsStage mpe)
	{
	    cmsStage NewMPE;
	    
	    if (mpe == null)
	    {
	    	return null;
	    }
	    NewMPE = _cmsStageAllocPlaceholder(mpe.ContextID, mpe.Type, mpe.InputChannels, mpe.OutputChannels, mpe.EvalPtr, mpe.DupElemPtr, mpe.FreePtr, null);
	    if (NewMPE == null)
	    {
	    	return null;
	    }
	    
	    NewMPE.Implements = mpe.Implements;
	    
	    if (mpe.DupElemPtr != null)
	    {
	    	NewMPE.Data = mpe.DupElemPtr.run(mpe);
	    }
	    else
	    {
	    	NewMPE.Data = null;
	    }
	    
	    return NewMPE;
	}
	
	// ***********************************************************************************************************
	
	// This function sets up the channel count
	
	private static void BlessLUT(cmsPipeline lut)
	{
	    // We can set the input/ouput channels only if we have elements.
	    if (lut.Elements != null)
	    {
	        cmsStage First, Last;
	        
	        First = cmsPipelineGetPtrToFirstStage(lut);
	        Last  = cmsPipelineGetPtrToLastStage(lut);
	        
	        if (First != null)
	        {
	        	lut.InputChannels = First.InputChannels;
	        }
	        if (Last != null)
	        {
	        	lut.OutputChannels = Last.OutputChannels;
	        }
	    }
	}
	
	// Default to evaluate the LUT on 16 bit-basis. Precision is retained.
	private static final _cmsOPTeval16Fn _LUTeval16 = new _cmsOPTeval16Fn()
	{
		public void run(short[] In, short[] Out, Object D)
		{
			cmsPipeline lut = (cmsPipeline)D;
		    cmsStage mpe;
		    float[][] Storage = new float[2][lcms2_internal.MAX_STAGE_CHANNELS];
		    int Phase = 0, NextPhase;
		    
		    From16ToFloat(In, Storage[Phase], lut.InputChannels);
		    
		    for (mpe = lut.Elements; mpe != null; mpe = (cmsStage)mpe.Next)
		    {
		    	NextPhase = Phase ^ 1;
		    	mpe.EvalPtr.run(Storage[Phase], Storage[NextPhase], mpe);
		    	Phase = NextPhase;
		    }
		    
		    FromFloatTo16(Storage[Phase], Out, lut.OutputChannels);
		}
	};
	
	// Does evaluate the LUT on cmsFloat32Number-basis.
	private static final _cmsPipelineEvalFloatFn _LUTevalFloat = new _cmsPipelineEvalFloatFn()
	{
		public void run(float[] In, float[] Out, Object D)
		{
			cmsPipeline lut = (cmsPipeline)D;
		    cmsStage mpe;
		    float[][] Storage = new float[2][lcms2_internal.MAX_STAGE_CHANNELS];
		    int Phase = 0, NextPhase;
		    
		    System.arraycopy(In, 0, Storage[Phase], 0, lut.InputChannels);
		    
		    for (mpe = lut.Elements; mpe != null; mpe = (cmsStage)mpe.Next)
		    {
		    	NextPhase = Phase ^ 1;
		    	mpe.EvalPtr.run(Storage[Phase], Storage[NextPhase], mpe);
		    	Phase = NextPhase;
		    }
		    
		    System.arraycopy(Storage[Phase], 0, Out, 0, lut.OutputChannels);
		}
	};
	
	// LUT Creation & Destruction
	
	public static cmsPipeline cmsPipelineAlloc(cmsContext ContextID, int InputChannels, int OutputChannels)
	{
		cmsPipeline NewLUT;
		
		if (InputChannels >= lcms2.cmsMAXCHANNELS || OutputChannels >= lcms2.cmsMAXCHANNELS)
		{
			return null;
		}
		
		NewLUT = new cmsPipeline();
		
		NewLUT.InputChannels  = InputChannels;
		NewLUT.OutputChannels = OutputChannels;
		
		NewLUT.Eval16Fn    = _LUTeval16;
		NewLUT.EvalFloatFn = _LUTevalFloat;
		NewLUT.DupDataFn   = null;
		NewLUT.FreeDataFn  = null;
		NewLUT.Data        = NewLUT;
		
		NewLUT.ContextID    = ContextID;
		
		BlessLUT(NewLUT);
		
		return NewLUT;
	}
	
	public static cmsContext cmsGetPipelineContextID(final cmsPipeline lut)
	{
	    lcms2_internal._cmsAssert(lut != null, "lut != null");
	    return lut.ContextID;
	}
	
	public static int cmsPipelineInputChannels(final cmsPipeline lut)
	{
		lcms2_internal._cmsAssert(lut != null, "lut != null");
	    return lut.InputChannels;
	}
	
	public static int cmsPipelineOutputChannels(final cmsPipeline lut)
	{
		lcms2_internal._cmsAssert(lut != null, "lut != null");
	    return lut.OutputChannels;
	}
	
	// Free a profile elements LUT
	public static void cmsPipelineFree(cmsPipeline lut)
	{
	    cmsStage mpe, Next;
	    
	    if (lut == null)
	    {
	    	return;
	    }
	    
	    for (mpe = lut.Elements; mpe != null; mpe = Next)
	    {
	    	Next = (cmsStage)mpe.Next;
	    	cmsStageFree(mpe);
	    }
	    
	    if (lut.FreeDataFn != null)
	    {
	    	lut.FreeDataFn.run(lut.ContextID, lut.Data);
	    }
	    
	    //_cmsFree(lut.ContextID, lut);
	}
	
	// Default to evaluate the LUT on 16 bit-basis.
	public static void cmsPipelineEval16(final short[] In, short[] Out, final cmsPipeline lut)
	{
		lcms2_internal._cmsAssert(lut != null, "lut != null");
	    lut.Eval16Fn.run(In, Out, lut.Data);
	}
	
	// Does evaluate the LUT on cmsFloat32Number-basis.
	public static void cmsPipelineEvalFloat(final float[] In, float[] Out, final cmsPipeline lut)
	{
		lcms2_internal._cmsAssert(lut != null, "lut != null");
	    lut.EvalFloatFn.run(In, Out, lut);
	}
	
	// Duplicates a LUT
	public static cmsPipeline cmsPipelineDup(final cmsPipeline lut)
	{
	    cmsPipeline NewLUT;
	    cmsStage NewMPE, Anterior = null, mpe;
	    boolean First = true;
	    
	    if (lut == null)
	    {
	    	return null;
	    }
	    
	    NewLUT = cmsPipelineAlloc(lut.ContextID, lut.InputChannels, lut.OutputChannels);
	    for (mpe = lut.Elements; mpe != null; mpe = (cmsStage)mpe.Next)
	    {
	    	NewMPE = cmsStageDup(mpe);
	    	
	    	if (NewMPE == null)
	    	{
	    		cmsPipelineFree(NewLUT);
	    		return null;
	    	}
	    	
	    	if (First)
	    	{
	    		NewLUT.Elements = NewMPE;
	    		First = false;
	    	}
	    	else
	    	{
	    		Anterior.Next = NewMPE;
	    	}
	    	
	    	Anterior = NewMPE;
	    }
	    
	    NewLUT.Eval16Fn    = lut.Eval16Fn;
	    NewLUT.EvalFloatFn = lut.EvalFloatFn;
	    NewLUT.DupDataFn  = lut.DupDataFn;
	    NewLUT.FreeDataFn = lut.FreeDataFn;
	    
	    if (NewLUT.DupDataFn != null)
	    {
	    	NewLUT.Data = NewLUT.DupDataFn.run(lut.ContextID, lut.Data);
	    }
	    
	    NewLUT.SaveAs8Bits    = lut.SaveAs8Bits;
	    
	    BlessLUT(NewLUT);
	    return NewLUT;
	}
	
	public static void cmsPipelineInsertStage(cmsPipeline lut, int loc, cmsStage mpe)
	{
	    cmsStage Anterior = null, pt;
	    
	    lcms2_internal._cmsAssert(lut != null, "lut != null");
	    lcms2_internal._cmsAssert(mpe != null, "mpe != null");
	    
	    switch (loc)
	    {
	        case lcms2.cmsAT_BEGIN:
	            mpe.Next = lut.Elements;
	            lut.Elements = mpe;
	            break;
	        case lcms2.cmsAT_END:
	            if (lut.Elements == null)
	            {
	            	lut.Elements = mpe;
	            }
	            else
	            {
	                for (pt = lut.Elements; pt != null; pt = (cmsStage)pt.Next)
	                {
	                	Anterior = pt;
	                }
	                
	                Anterior.Next = mpe;
	                mpe.Next = null;
	            }
	            break;
	        default:;
	    }
	    
	    BlessLUT(lut);
	}
	
	// Unlink an element and return the pointer to it
	public static void cmsPipelineUnlinkStage(cmsPipeline lut, int loc, cmsStage[] mpe)
	{
	    cmsStage Anterior, pt, Last;
	    cmsStage Unlinked = null;
	    
	    // If empty LUT, there is nothing to remove
	    if (lut.Elements == null)
	    {
	        if (mpe != null)
	        {
	        	mpe[0] = null;
	        }
	        return;
	    }
	    
	    // On depending on the strategy...
	    switch (loc)
	    {
	        case lcms2.cmsAT_BEGIN:
	            {
	                cmsStage elem = lut.Elements;
	                
	                lut.Elements = (cmsStage)elem.Next;
	                elem.Next = null;
	                Unlinked = elem;
	            }
	            break;
	        case lcms2.cmsAT_END:
	            Anterior = Last = null;
	            for (pt = lut.Elements; pt != null; pt = (cmsStage)pt.Next)
	            {
	            	Anterior = Last;
	            	Last = pt;
	            }
	            
	            Unlinked = Last;  // Next already points to NULL
	            
	            // Truncate the chain
	            if (Anterior != null)
	            {
	            	Anterior.Next = null;
	            }
	            else
	            {
	            	lut.Elements = null;
	            }
	            break;
	        default:;
	    }
	    
	    if (mpe != null)
	    {
	    	mpe[0] = Unlinked;
	    }
	    else
	    {
	    	cmsStageFree(Unlinked);
	    }
	    
	    BlessLUT(lut);
	}
	
	// Concatenate two LUT into a new single one
	public static boolean cmsPipelineCat(cmsPipeline l1, final cmsPipeline l2)
	{
	    cmsStage mpe, NewMPE;
	    
	    // If both LUTS does not have elements, we need to inherit
	    // the number of channels
	    if (l1.Elements == null && l2.Elements == null)
	    {
	        l1.InputChannels  = l2.InputChannels;
	        l1.OutputChannels = l2.OutputChannels;
	    }
	    
	    // Cat second
	    for (mpe = l2.Elements; mpe != null; mpe = (cmsStage)mpe.Next)
	    {
	    	// We have to dup each element
	    	NewMPE = cmsStageDup(mpe);
	    	
	    	if (NewMPE == null)
	    	{
	    		return false;
	    	}
	    	
	    	cmsPipelineInsertStage(l1, lcms2.cmsAT_END, NewMPE);
	    }
	    
	    BlessLUT(l1);
	    return true;
	}
	
	public static boolean cmsPipelineSetSaveAs8bitsFlag(cmsPipeline lut, boolean On)
	{
	    boolean Anterior = lut.SaveAs8Bits;
	    
	    lut.SaveAs8Bits = On;
	    return Anterior;
	}
	
	public static cmsStage cmsPipelineGetPtrToFirstStage(final cmsPipeline lut)
	{
	    return lut.Elements;
	}
	
	public static cmsStage cmsPipelineGetPtrToLastStage(final cmsPipeline lut)
	{
	    cmsStage mpe, Anterior = null;
	    
	    for (mpe = lut.Elements; mpe != null; mpe = (cmsStage)mpe.Next)
	    {
	    	Anterior = mpe;
	    }
	    
	    return Anterior;
	}
	
	public static int cmsPipelineStageCount(final cmsPipeline lut)
	{
	    cmsStage mpe;
	    int n;
	    
	    for (n=0, mpe = lut.Elements; mpe != null; mpe = (cmsStage)mpe.Next)
	    {
	    	n++;
	    }
	    
	    return n;
	}
	
	// This function may be used to set the optional evalueator and a block of private data. If private data is being used, an optional
	// duplicator and free functions should also be specified in order to duplicate the LUT construct. Use NULL to inhibit such functionality.
	public static void _cmsPipelineSetOptimizationParameters(cmsPipeline Lut, _cmsOPTeval16Fn Eval16, Object PrivateData, _cmsFreeUserDataFn FreePrivateDataFn,
			_cmsDupUserDataFn DupPrivateDataFn)
	{
	    Lut.Eval16Fn = Eval16;
	    Lut.DupDataFn = DupPrivateDataFn;
	    Lut.FreeDataFn = FreePrivateDataFn;
	    Lut.Data = PrivateData;
	}
	
	// ----------------------------------------------------------- Reverse interpolation
	// Here's how it goes. The derivative Df(x) of the function f is the linear
	// transformation that best approximates f near the point x. It can be represented
	// by a matrix A whose entries are the partial derivatives of the components of f
	// with respect to all the coordinates. This is know as the Jacobian
	//
	// The best linear approximation to f is given by the matrix equation:
	//
	// y-y0 = A (x-x0)
	//
	// So, if x0 is a good "guess" for the zero of f, then solving for the zero of this
	// linear approximation will give a "better guess" for the zero of f. Thus let y=0,
	// and since y0=f(x0) one can solve the above equation for x. This leads to the
	// Newton's method formula:
	//
	// xn+1 = xn - A-1 f(xn)
	//
	// where xn+1 denotes the (n+1)-st guess, obtained from the n-th guess xn in the
	// fashion described above. Iterating this will give better and better approximations
	// if you have a "good enough" initial guess.
	
	private static final float JACOBIAN_EPSILON       = 0.001f;
	private static final int INVERSION_MAX_ITERATIONS = 30;
	
	// Increment with reflexion on boundary
	private static void IncDelta(float[] Val, int index)
	{
	    if (Val[index] < (1 - JACOBIAN_EPSILON))
	    {
	    	Val[index] += JACOBIAN_EPSILON;
	    }
	    else
	    {
	    	Val[index] -= JACOBIAN_EPSILON;
	    }
	}
	
	// Euclidean distance between two vectors of n elements each one
	private static float EuclideanDistance(float[] a, float[] b, int n)
	{
	    float sum = 0;
	    int i;
	    
	    for (i=0; i < n; i++)
	    {
	        float dif = b[i] - a[i];
	        sum += dif * dif;
	    }
	    
	    return (float)Math.sqrt(sum);
	}
	
	// Evaluate a LUT in reverse direction. It only searches on 3->3 LUT. Uses Newton method
	//
	// x1 <- x - [J(x)]^-1 * f(x)
	//
	// lut: The LUT on where to do the search
	// Target: LabK, 3 values of Lab plus destination K which is fixed
	// Result: The obtained CMYK
	// Hint:   Location where begin the search
	
	public static boolean cmsPipelineEvalReverseFloat(float[] Target, float[] Result, float[] Hint, final cmsPipeline lut)
	{
		int i, j;
		double error, LastError = 1E20;
		float[] fx = new float[4], x = new float[4], xd = new float[4], fxd = new float[4];
		cmsVEC3 tmp = new cmsVEC3(), tmp2 = new cmsVEC3();
		cmsMAT3 Jacobian = new cmsMAT3();
		double[] LastResult = new double[4];
		
		// Only 3->3 and 4->3 are supported
		if (lut.InputChannels != 3 && lut.InputChannels != 4)
		{
			return false;
		}
		if (lut.OutputChannels != 3)
		{
			return false;
		}
		
		// Mark result of -1
		LastResult[0] = LastResult[1] = LastResult[2] = -1.0f;
		
		// Take the hint as starting point if specified
		if (Hint == null)
		{
			// Begin at any point, we choose 1/3 of CMY axis
			x[0] = x[1] = x[2] = 0.3f;
		}
		else
		{
			// Only copy 3 channels from hint...
			for (j=0; j < 3; j++)
			{
				x[j] = Hint[j];
			}
		}
		
		// If Lut is 4-dimensions, then grab target[3], which is fixed
		if (lut.InputChannels == 4)
		{
			x[3] = Target[3];
		}
		else
		{
			x[3] = 0; // To keep lint happy
		}
		
		// Iterate
		for (i = 0; i < INVERSION_MAX_ITERATIONS; i++)
		{
			// Get beginning fx
			cmsPipelineEvalFloat(x, fx, lut);
			
			// Compute error
			error = EuclideanDistance(fx, Target, 3);
			
			// If not convergent, return last safe value
			if (error >= LastError)
			{
				break;
			}
			
			// Keep latest values
			LastError = error;
			for (j=0; j < lut.InputChannels; j++)
			{
				Result[j] = x[j];
			}
			
			// Found an exact match?
			if (error <= 0)
			{
				break;
			}
			
			// Obtain slope (the Jacobian)
			
			for (j = 0; j < 3; j++)
			{
				xd[0] = x[0];
				xd[1] = x[1];
				xd[2] = x[2];
				xd[3] = x[3];  // Keep fixed channel
				
				IncDelta(xd, j);
				
				cmsPipelineEvalFloat(xd, fxd, lut);
				
				Jacobian.v[0].n[j] = ((fxd[0] - fx[0]) * (1 / JACOBIAN_EPSILON));
				Jacobian.v[1].n[j] = ((fxd[1] - fx[1]) * (1 / JACOBIAN_EPSILON));
				Jacobian.v[2].n[j] = ((fxd[2] - fx[2]) * (1 / JACOBIAN_EPSILON));
			}
			
			// Solve system
			tmp2.n[0] = fx[0] - Target[0];
			tmp2.n[1] = fx[1] - Target[1];
			tmp2.n[2] = fx[2] - Target[2];
			
			if (!cmsmtrx._cmsMAT3solve(tmp, Jacobian, tmp2))
			{
				return false;
			}
			
			// Move our guess
			x[0] -= (float)tmp.n[0];
			x[1] -= (float)tmp.n[1];
			x[2] -= (float)tmp.n[2];
			
			// Some clipping....
			for (j=0; j < 3; j++)
			{
				if (x[j] < 0)
				{
					x[j] = 0;
				}
				else
				{
					if (x[j] > 1.0)
					{
						x[j] = 1;
					}
				}
			}
		}
		
		return true;
	}
}
