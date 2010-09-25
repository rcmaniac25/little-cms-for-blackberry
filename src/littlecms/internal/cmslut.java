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

import littlecms.internal.helper.VirtualPointer;
import littlecms.internal.lcms2.cmsContext;
import littlecms.internal.lcms2.cmsPipeline;
import littlecms.internal.lcms2.cmsStage;
import littlecms.internal.lcms2.cmsToneCurve;
import littlecms.internal.lcms2_internal._cmsStageCLutData;
import littlecms.internal.lcms2_internal._cmsStageMatrixData;
import littlecms.internal.lcms2_internal._cmsStageToneCurvesData;
import littlecms.internal.lcms2_internal._cmsStage_struct;
import littlecms.internal.lcms2_plugin._cmsStageDupElemFn;
import littlecms.internal.lcms2_plugin._cmsStageEvalFn;
import littlecms.internal.lcms2_plugin._cmsStageFreeElemFn;

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
	        Out[i] = In[i] * (1F / 65535.0F);
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
	
	private static cmsToneCurve[] _cmsStageGetPtrToCurveSet(final cmsStage mpe)
	{
	    _cmsStageToneCurvesData Data = (_cmsStageToneCurvesData)mpe.Data;
	    
	    return Data.TheCurves;
	}
	
	private static final _cmsStageEvalFn EvaluateCurves = new _cmsStageEvalFn()
	{
		public void run(final float[] In, float[] Out, final cmsStage mpe)
		{
			_cmsStageToneCurvesData Data = (_cmsStageToneCurvesData)mpe.Data;
		    int i;
		    
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
			_cmsStageToneCurvesData Data = (_cmsStageToneCurvesData)mpe.Data;
		    int i;
		    
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
	    
	    NewMPE.Data = NewElem;
	    
	    return NewMPE;
	}
	
	// Create a bunch of identity curves
	private static cmsStage _cmsStageAllocIdentityCurves(cmsContext ContextID, int nChannels)
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
	    int rv;
	    
	    for (rv = 1; b > 0; b--)
	    {
	    	rv *= Dims[b-1];
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
		
		NewElem.nEntries = n = outputChan * CubeSize(clutPoints, inputChan);
		NewElem.HasFloatValues = false;
		
		NewElem.Tab  = cmserr._cmsCalloc(ContextID, n, /*sizeof(cmsUInt16Number)*/2);
		if (NewElem.Tab == null)
		{
			return null;
		}
		
		if (Table != null)
		{
			VirtualPointer.TypeProcessor proc = NewElem.Tab.getProcessor();
			for (i=0; i < n; i++)
			{
				proc.write(Table[i]);
			}
			NewElem.Tab.setPosition(0);
		}
		
		NewElem.Params = cmsintrp._cmsComputeInterpParamsEx(ContextID, clutPoints, inputChan, outputChan, NewElem.Tab, lcms2_plugin.CMS_LERP_FLAGS_16BITS);
		if (NewElem.Params == null)
		{
			return null;
		}
		
		NewMPE.Data = NewElem;
		
		return NewMPE;
	}
	
	//TODO #557
}
