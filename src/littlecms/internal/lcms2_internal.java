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

import java.util.Calendar;

import littlecms.internal.helper.VirtualPointer;

//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class lcms2_internal extends lcms2_plugin
{
	// Some needed constants
	public static final double M_PI = Math.PI;
	public static final double M_LOG10E = 0.434294481903251827651;
	
	// Alignment of ICC file format uses 4 bytes (cmsUInt32Number)
	public static int _cmsALIGNLONG(int x)
	{
		return (((x)+(/*sizeof(cmsUInt32Number)*/4-1)) & ~(/*sizeof(cmsUInt32Number)*/4-1));
	}
	
	// Alignment to memory pointer
	public static int _cmsALIGNMEM(int x)
	{
		return (((x)+(/*sizeof(void *)*/VirtualPointer.SIZE - 1)) & ~(/*sizeof(void *)*/VirtualPointer.SIZE - 1));
	}
	
	// Maximum encodeable values in floating point
	public static final double MAX_ENCODEABLE_XYZ = (1.0 + 32767.0/32768.0);
	public static final double MIN_ENCODEABLE_ab2 = (-128.0);
	public static final double MAX_ENCODEABLE_ab2 = ((65535.0/256.0) - 128.0);
	public static final double MIN_ENCODEABLE_ab4 = (-128.0);
	public static final double MAX_ENCODEABLE_ab4 = (127.0);
	
	// Maximum of channels for internal pipeline evaluation
	public static final int MAX_STAGE_CHANNELS = 128;
	
	// A fast way to convert from/to 16 <-> 8 bits
	public static short FROM_8_TO_16(int rgb)
	{
		return (short)(((rgb & 0xFF) << 8)|(rgb & 0xFF));
	}
	public static byte FROM_16_TO_8(short rgb)
	{
		return (byte)((((rgb & 0xFFFF) * 65281 + 8388608) >> 24) & 0xFF);
	}
	
	public static void _cmsAssert(boolean test, String exp)
	{
//#ifdef DEBUG | CMS_ENABLE_ASSERT
		if(!test)
		{
			new Throwable(exp).printStackTrace();
			System.exit(1);
		}
//#endif
	}
	
	//---------------------------------------------------------------------------------
	
	// Determinant lower than that are assumed zero (used on matrix invert)
	public static final double MATRIX_DET_TOLERANCE = 0.0001;
	
	//---------------------------------------------------------------------------------
	
	// Fixed point
	public static int FIXED_TO_INT(int x)
	{
		return (x)>>16;
	}
	public static int FIXED_REST_TO_INT(int x)
	{
		return (x)&0xFFFF;
	}
	public static int ROUND_FIXED_TO_INT(int x)
	{
		return ((x)+0x8000)>>16;
	}
	
	public static int _cmsToFixedDomain(int a)
	{
		return a + ((a + 0x7fff) / 0xffff);
	}
	public static int _cmsFromFixedDomain(int a)
	{
		return a - ((a + 0x7fff) >> 16);
	}
	
	// -----------------------------------------------------------------------------------------------------------
	
//#ifndef CMS_DONT_USE_FAST_FLOOR
	private static final double _lcms_double2fixmagic = 68719476736.0 * 1.5;  // 2^36 * 1.5, (52-16=36) uses limited precision to floor
//#endif
	
	// Fast floor conversion logic. Thanks to Sree Kotay and Stuart Nixon
	// note than this only works in the range ..-32767...+32767 because
	// mantissa is interpreted as 15.16 fixed point.
	// The union is to avoid pointer aliasing overoptimization.
	public static int _cmsQuickFloor(double val)
	{
//#ifdef CMS_DONT_USE_FAST_FLOOR
	    return (int)Math.floor(val);
//#else
	    //Java is always little endian with regards to storing and handling bytes (I/O being the exception)
	    return (int)(Double.doubleToLongBits(val + _lcms_double2fixmagic) >> 48);
//#endif
	}
	
	// Fast floor restricted to 0..65535.0
	public static short _cmsQuickFloorWord(double d)
	{
	    return (short)(_cmsQuickFloor(d - 32767.0) + 32767);
	}
	
	// Floor to word, taking care of saturation
	public static short _cmsQuickSaturateWord(double d)
	{
	    d += 0.5;
	    if (d <= 0)
	    {
	    	return 0;
	    }
	    if (d >= 65535.0)
	    {
	    	return (short)0xffff;
	    }
	    
	    return _cmsQuickFloorWord(d);
	}
	
	// Plug-In registering ---------------------------------------------------------------

	// Specialized function for plug-in memory management. No pairing free() since whole pool is freed at once.
	public static Object _cmsPluginMalloc(int size)
	{
		return cmsplugin._cmsPluginMalloc(size);
	}
	
	// Memory management
	public static boolean _cmsRegisterMemHandlerPlugin(cmsPluginBase Plugin)
	{
		return cmserr._cmsRegisterMemHandlerPlugin(Plugin);
	}
	
	// Interpolation
	public static boolean _cmsRegisterInterpPlugin(cmsPluginBase Plugin)
	{
		return cmsintrp._cmsRegisterInterpPlugin(Plugin);
	}
	
	// Parametric curves
	public static boolean _cmsRegisterParametricCurvesPlugin(cmsPluginBase Plugin)
	{
		return cmsgamma._cmsRegisterParametricCurvesPlugin(Plugin);
	}
	
	// Formatters management
	public static boolean _cmsRegisterFormattersPlugin(cmsPluginBase Plugin)
	{
		return cmspack._cmsRegisterFormattersPlugin(Plugin);
	}
	
	// Tag type management
	public static boolean _cmsRegisterTagTypePlugin(cmsPluginBase Plugin)
	{
		return cmstypes._cmsRegisterTagTypePlugin(Plugin);
	}
	
	// Tag management
	public static boolean _cmsRegisterTagPlugin(cmsPluginBase Plugin)
	{
		return cmstypes._cmsRegisterTagPlugin(Plugin);
	}
	
	// Intent management
	public static boolean _cmsRegisterRenderingIntentPlugin(cmsPluginBase Plugin)
	{
		return cmscnvrt._cmsRegisterRenderingIntentPlugin(Plugin);
	}
	
	// Multi Process elements
	public static boolean _cmsRegisterMultiProcessElementPlugin(cmsPluginBase Plugin)
	{
		return cmstypes._cmsRegisterMultiProcessElementPlugin(Plugin);
	}
	
	// Optimization
	public static boolean _cmsRegisterOptimizationPlugin(cmsPluginBase Plugin)
	{
		return cmsopt._cmsRegisterOptimizationPlugin(Plugin);
	}
	
	// Transform
	public static boolean _cmsRegisterTransformPlugin(cmsPluginBase Plugin)
	{
		return cmsxform._cmsRegisterTransformPlugin(Plugin);
	}
	
	// ---------------------------------------------------------------------------------------------------------
	
	// Suballocators. Those are blocks of memory that is freed at the end on whole block.
	public static class _cmsSubAllocator_chunk
	{
		public static final int SIZE = VirtualPointer.SIZE + 4 + 4 + 4; //The last 4 is pointer
		
		public VirtualPointer Block;
		public int BlockSize;
	    public int Used;
	    
	    public _cmsSubAllocator_chunk next;
	}
	
	public static class _cmsSubAllocator
	{
		public static final int SIZE = _cmsSubAllocator_chunk.SIZE;
		
		public cmsContext ContextID;
		public _cmsSubAllocator_chunk h;
	}
	
	public static _cmsSubAllocator _cmsCreateSubAlloc(cmsContext ContextID, int Initial)
	{
		return cmserr._cmsCreateSubAlloc(ContextID, Initial);
	}
	public static void _cmsSubAllocDestroy(_cmsSubAllocator s)
	{
		cmserr._cmsSubAllocDestroy(s);
	}
	public static VirtualPointer _cmsSubAlloc(_cmsSubAllocator s, int size)
	{
		return cmserr._cmsSubAlloc(s, size);
	}
	
	// ----------------------------------------------------------------------------------

	// MLU internal representation
	public static class _cmsMLUentry
	{
		public short Language;
		public short Country;
	    
		/** Offset to current unicode string*/
		public int StrW;
		/** Length in bytes*/
		public int Len;
	}
	
	//XXX Data types that are located in lcms2_internal but are used under a different name in lcms2 need to use the "default" accessor to prevent the values from being accessible.
	public static class _cms_MLU_struct
	{
		cmsContext ContextID;
		
	    // The directory
	    int AllocatedEntries;
	    int UsedEntries;
	    /** Array of pointers to strings allocated in MemPool*/
	    _cmsMLUentry[] Entries;
	    
	    // The Pool
	    /** The maximum allocated size*/
	    int PoolSize;
	    /** The used size*/
	    int PoolUsed;
	    /** Pointer to begin of memory pool*/
	    VirtualPointer MemPool;
	}
	
	// Named color list internal representation
	public static class _cmsNAMEDCOLOR
	{
		public StringBuffer Name;
	    public short[] PCS;
	    public short[] DeviceColorant;
	    
	    public _cmsNAMEDCOLOR()
	    {
	    	Name = new StringBuffer(cmsMAX_PATH);
	    	PCS = new short[3];
	    	DeviceColorant = new short[cmsMAXCHANNELS];
	    }
	}
	
	public static class _cms_NAMEDCOLORLIST_struct
	{
		int nColors;
	    int Allocated;
	    int ColorantCount;
	    
	    StringBuffer Prefix; // Prefix and suffix are defined to be 32 characters at most
	    StringBuffer Suffix;
	    
	    _cmsNAMEDCOLOR[] List;
	    
	    cmsContext ContextID;
	    
	    public _cms_NAMEDCOLORLIST_struct()
	    {
	    	Prefix = new StringBuffer(32);
	    	Suffix = new StringBuffer(32);
	    }
	}
	
	// ----------------------------------------------------------------------------------
	
	// This is the internal struct holding profile details.
	
	// Maximum supported tags in a profile
	public static final int MAX_TABLE_TAG = 100;
	
	public static class _cmsICCPROFILE implements cmsHPROFILE
	{
		//Calendar, header values, tag count, iswrite
		public static final int SIZE = (4 * 6) + (4 * 8) + 8 + cmsProfileID.SIZE + 4 + (MAX_TABLE_TAG * ((4 * 4) + 1)) + 1;
		
		// I/O handler
		public cmsIOHANDLER IOhandler;
	    
	    // The thread ID
		public cmsContext ContextID;
	    
	    // Creation time
		public Calendar Created;
	    
	    // Only most important items found in ICC profiles
		public int Version;
		public int DeviceClass;
		public int ColorSpace;
		public int PCS;
		public int RenderingIntent;
		public int flags;
		public int manufacturer, model;
		public long attributes;
	    
		public cmsProfileID ProfileID;
	    
	    // Dictionary
		public int TagCount;
	    public int[] TagNames;
	    public int[] TagLinked; // The tag to which is linked (0=none)
	    public int[] TagSizes; // Size on disk
	    public int[] TagOffsets;
	    public boolean[] TagSaveAsRaw; // True to write uncooked
	    public Object[] TagPtrs;
	    public cmsTagTypeHandler[] TagTypeHandlers; // Same structure may be serialized on different types depending on profile version, so we keep track of the type handler for each tag in the list.
	    
	    // Special
	    public boolean IsWrite;
	    
	    public _cmsICCPROFILE()
	    {
	    	TagNames = new int[MAX_TABLE_TAG];
	    	TagLinked = new int[MAX_TABLE_TAG];
	    	TagSizes = new int[MAX_TABLE_TAG];
	    	TagOffsets = new int[MAX_TABLE_TAG];
	    	TagSaveAsRaw = new boolean[MAX_TABLE_TAG];
	    	TagPtrs = new Object[MAX_TABLE_TAG];
	    	TagTypeHandlers = new cmsTagTypeHandler[MAX_TABLE_TAG];
	    	ProfileID = new cmsProfileID();
	    }
	}
	
	// IO helpers for profiles
	public static boolean _cmsReadHeader(_cmsICCPROFILE Icc)
	{
		return cmsio0._cmsReadHeader(Icc);
	}
	
	public static boolean _cmsWriteHeader(_cmsICCPROFILE Icc, int UsedSpace)
	{
		return cmsio0._cmsWriteHeader(Icc, UsedSpace);
	}
	
	public static int _cmsSearchTag(_cmsICCPROFILE Icc, int sig, boolean lFollowLinks)
	{
		return cmsio0._cmsSearchTag(Icc, sig, lFollowLinks);
	}
	
	// Tag types
	public static cmsTagTypeHandler _cmsGetTagTypeHandler(int sig)
	{
		return cmstypes._cmsGetTagTypeHandler(sig);
	}
	
	public static int _cmsGetTagTrueType(cmsHPROFILE hProfile, int sig)
	{
		return cmsio0._cmsGetTagTrueType(hProfile, sig);
	}
	
	public static cmsTagDescriptor _cmsGetTagDescriptor(int sig)
	{
		return cmstypes._cmsGetTagDescriptor(sig);
	}
	
	// Error logging ---------------------------------------------------------------------------------------------------------
	
	public static void _cmsTagSignature2String(StringBuffer String, int sig)
	{
		cmserr._cmsTagSignature2String(String, sig);
	}
	
	// Interpolation ---------------------------------------------------------------------------------------------------------
	
	public static cmsInterpParams _cmsComputeInterpParams(cmsContext ContextID, int nSamples, int InputChan, int OutputChan, final Object Table, int dwFlags)
	{
		return cmsintrp._cmsComputeInterpParams(ContextID, nSamples, InputChan, OutputChan, Table, dwFlags);
	}
	
	public static cmsInterpParams _cmsComputeInterpParamsEx(cmsContext ContextID, final int[] nSamples, int InputChan, int OutputChan, final Object Table, int dwFlags)
	{
		return cmsintrp._cmsComputeInterpParamsEx(ContextID, nSamples, InputChan, OutputChan, Table, dwFlags);
	}
	
	public static void _cmsFreeInterpParams(cmsInterpParams p)
	{
		cmsintrp._cmsFreeInterpParams(p);
	}
	
	public static boolean _cmsSetInterpolationRoutine(cmsInterpParams p)
	{
		return cmsintrp._cmsSetInterpolationRoutine(p);
	}
	
	// Curves ----------------------------------------------------------------------------------------------------------------

	// This struct holds information about a segment, plus a pointer to the function that implements the evaluation.
	// In the case of table-based, Eval pointer is set to NULL
	
	// The gamma function main structure
	public static class _cms_curve_struct
	{
		/** Private optimizations for interpolation*/
		cmsInterpParams InterpParams;
		
		/** Number of segments in the curve. Zero for a 16-bit based tables*/
	    int nSegments;
	    /** The segments*/
	    cmsCurveSegment[] Segments;
	    /** Array of private optimizations for interpolation in table-based segments*/
	    cmsInterpParams[] SegInterp;
	    
	    /** Evaluators (one per segment)*/
	    cmsParametricCurveEvaluator[] Evals;
	    
	    // 16 bit Table-based representation follows
	    /** Number of table elements*/
	    int nEntries;
	    /** The table itself.*/
	    short[] Table16;
	}
	
	//  Pipelines & Stages ---------------------------------------------------------------------------------------------
	
	// A single stage
	public static class _cmsStage_struct
	{
		cmsContext ContextID;
		
		/** Identifies the stage*/
	    int Type;
	    /** Identifies the *function* of the stage (for optimizations)*/
	    int Implements;
	    
	    /** Input channels -- for optimization purposes*/
	    int InputChannels;
	    /** Output channels -- for optimization purposes*/
	    int OutputChannels;
	    
	    /** Points to fn that evaluates the stage (always in floating point)*/
	    _cmsStageEvalFn EvalPtr;
	    /** Points to a fn that duplicates the *data* of the stage*/
	    _cmsStageDupElemFn DupElemPtr;
	    /** Points to a fn that sets the *data* of the stage free*/
	    _cmsStageFreeElemFn FreePtr;
	    
	    // A generic pointer to whatever memory needed by the stage
	    Object Data;
	    
	    // Maintains linked list (used internally)
	    _cmsStage_struct Next;
	}
	
	// Special Stages (cannot be saved)
	public static cmsStage _cmsStageAllocLab2XYZ(cmsContext ContextID)
	{
		return cmslut._cmsStageAllocLab2XYZ(ContextID);
	}
	
	public static cmsStage _cmsStageAllocXYZ2Lab(cmsContext ContextID)
	{
		return cmslut._cmsStageAllocXYZ2Lab(ContextID);
	}
	
	public static cmsStage _cmsStageAllocLabPrelin(cmsContext ContextID)
	{
		return cmslut._cmsStageAllocLabPrelin(ContextID);
	}
	
	public static cmsStage _cmsStageAllocLabV2ToV4(cmsContext ContextID)
	{
		return cmslut._cmsStageAllocLabV2ToV4(ContextID);
	}
	
	public static cmsStage _cmsStageAllocLabV2ToV4curves(cmsContext ContextID)
	{
		return cmslut._cmsStageAllocLabV2ToV4curves(ContextID);
	}
	
	public static cmsStage _cmsStageAllocLabV4ToV2(cmsContext ContextID)
	{
		return cmslut._cmsStageAllocLabV4ToV2(ContextID);
	}
	
	public static cmsStage _cmsStageAllocNamedColor(cmsNAMEDCOLORLIST NamedColorList, boolean UsePCS)
	{
		return cmsnamed._cmsStageAllocNamedColor(NamedColorList, UsePCS);
	}
	
	public static cmsStage _cmsStageAllocIdentityCurves(cmsContext ContextID, int nChannels)
	{
		return cmslut._cmsStageAllocIdentityCurves(ContextID, nChannels);
	}
	
	public static cmsStage _cmsStageAllocIdentityCLut(cmsContext ContextID, int nChan)
	{
		return cmslut._cmsStageAllocIdentityCLut(ContextID, nChan);
	}
	
	public static cmsStage _cmsStageNormalizeFromLabFloat(cmsContext ContextID)
	{
		return cmslut._cmsStageNormalizeFromLabFloat(ContextID);
	}
	
	public static cmsStage _cmsStageNormalizeFromXyzFloat(cmsContext ContextID)
	{
		return cmslut._cmsStageNormalizeFromXyzFloat(ContextID);
	}
	
	public static cmsStage _cmsStageNormalizeToLabFloat(cmsContext ContextID)
	{
		return cmslut._cmsStageNormalizeToLabFloat(ContextID);
	}
	
	public static cmsStage _cmsStageNormalizeToXyzFloat(cmsContext ContextID)
	{
		return cmslut._cmsStageNormalizeToXyzFloat(ContextID);
	}
	
	// For curve set only
	public static cmsToneCurve[] _cmsStageGetPtrToCurveSet(final cmsStage mpe)
	{
		return cmslut._cmsStageGetPtrToCurveSet(mpe);
	}
	
	// Pipeline Evaluator (in floating point)
	public static interface _cmsPipelineEvalFloatFn
	{
		public void run(final float[] In, float[] Out, final Object Data);
	}
	
	public static class _cmsPipeline_struct
	{
		/** Points to elements chain*/
		cmsStage Elements;
	    int InputChannels, OutputChannels;
	    
	    // Data & evaluators
	    Object Data;
	    
	    _cmsOPTeval16Fn Eval16Fn;
	    _cmsPipelineEvalFloatFn EvalFloatFn;
	    _cmsFreeUserDataFn FreeDataFn;
	    _cmsDupUserDataFn DupDataFn;
	    
	    /** Environment*/
	    cmsContext ContextID;
	    
	    /** Implementation-specific: save as 8 bits if possible*/
	    boolean SaveAs8Bits;
	}
	
	// LUT reading & creation -------------------------------------------------------------------------------------------
	
	// Read tags using low-level function, provide necessary glue code to adapt versions, etc. All those return a brand new copy
	// of the LUTS, since ownership of original is up to the profile. The user should free allocated resources.
	
	public static cmsPipeline _cmsReadInputLUT(cmsHPROFILE hProfile, int Intent)
	{
		return cmsio1._cmsReadInputLUT(hProfile, Intent);
	}
	
	public static cmsPipeline _cmsReadOutputLUT(cmsHPROFILE hProfile, int Intent)
	{
		return cmsio1._cmsReadOutputLUT(hProfile, Intent);
	}
	
	public static cmsPipeline _cmsReadDevicelinkLUT(cmsHPROFILE hProfile, int Intent)
	{
		return cmsio1._cmsReadDevicelinkLUT(hProfile, Intent);
	}
	
	// Special values
	public static boolean _cmsReadMediaWhitePoint(cmsCIEXYZ Dest, cmsHPROFILE hProfile)
	{
		return cmsio1._cmsReadMediaWhitePoint(Dest, hProfile);
	}
	
	public static boolean _cmsReadCHAD(cmsMAT3 Dest, cmsHPROFILE hProfile)
	{
		return cmsio1._cmsReadCHAD(Dest, hProfile);
	}
	
	// Profile linker --------------------------------------------------------------------------------------------------
	
	public static cmsPipeline _cmsLinkProfiles(cmsContext ContextID, int nProfiles, int[] TheIntents, cmsHPROFILE[] hProfiles, boolean[] BPC, double[] AdaptationStates,
			int dwFlags)
	{
		return cmscnvrt._cmsLinkProfiles(ContextID, nProfiles, TheIntents, hProfiles, BPC, AdaptationStates, dwFlags);
	}
	
	// Sequence --------------------------------------------------------------------------------------------------------
	
	public static cmsSEQ _cmsReadProfileSequence(cmsHPROFILE hProfile)
	{
		return cmsio1._cmsReadProfileSequence(hProfile);
	}
	
	public static boolean _cmsWriteProfileSequence(cmsHPROFILE hProfile, final cmsSEQ seq)
	{
		return cmsio1._cmsWriteProfileSequence(hProfile, seq);
	}
	
	public static cmsSEQ _cmsCompileProfileSequence(cmsContext ContextID, int nProfiles, cmsHPROFILE[] hProfiles)
	{
		return cmsio1._cmsCompileProfileSequence(ContextID, nProfiles, hProfiles);
	}
	
	// LUT optimization ------------------------------------------------------------------------------------------------

	public static short _cmsQuantizeVal(double i, int MaxSamples)
	{
		return cmslut._cmsQuantizeVal(i, MaxSamples);
	}
	
	public static int _cmsReasonableGridpointsByColorspace(int Colorspace, int dwFlags)
	{
		return cmspcs._cmsReasonableGridpointsByColorspace(Colorspace, dwFlags);
	}
	
	public static boolean _cmsEndPointsBySpace(int Space, short[] White, short[] Black, int[] nOutputs)
	{
		return cmspcs._cmsEndPointsBySpace(Space, White, Black, nOutputs);
	}
	
	public static boolean _cmsOptimizePipeline(cmsPipeline[] Lut, int Intent, int[] InputFormat, int[] OutputFormat, int[] dwFlags)
	{
		return cmsopt._cmsOptimizePipeline(Lut, Intent, InputFormat, OutputFormat, dwFlags);
	}
	
	// Hi level LUT building ----------------------------------------------------------------------------------------------

	public static cmsPipeline _cmsCreateGamutCheckPipeline(cmsContext ContextID, cmsHPROFILE[] hProfiles, boolean[] BPC, int[] Intents, double[] AdaptationStates,
			int nGamutPCSposition, cmsHPROFILE hGamut)
	{
		return cmsgmt._cmsCreateGamutCheckPipeline(ContextID, hProfiles, BPC, Intents, AdaptationStates, nGamutPCSposition, hGamut);
	}
	
	// Formatters ------------------------------------------------------------------------------------------------------------
	
	public static final int cmsFLAGS_CAN_CHANGE_FORMATTER = 0x02000000; // Allow change buffer format
	
	public static boolean _cmsFormatterIsFloat(int Type)
	{
		return cmspack._cmsFormatterIsFloat(Type);
	}
	
	public static boolean _cmsFormatterIs8bit(int Type)
	{
		return cmspack._cmsFormatterIs8bit(Type);
	}
	
	/**
	 * @param Type Specific type, i.e. TYPE_RGB_8
	 */
	public static cmsFormatter _cmsGetFormatter(int Type, int Dir, int dwFlags)
	{
		return cmspack._cmsGetFormatter(Type, Dir, dwFlags);
	}
	
//#ifndef CMS_NO_HALF_SUPPORT
	
	// Half float
	public static float _cmsHalf2Float(short h)
	{
		return cmshalf._cmsHalf2Float(h);
	}
	
	public static short _cmsFloat2Half(float flt)
	{
		return cmshalf._cmsFloat2Half(flt);
	}
	
//#endif
	
	// Transform logic ------------------------------------------------------------------------------------------------------
	
	public static class _cmsCACHE
	{
		// 1-pixel cache (16 bits only)
		public short[] CacheIn;
		public short[] CacheOut;
	    
	    public _cmsCACHE()
	    {
	    	CacheIn = new short[cmsMAXCHANNELS];
	    	CacheOut = new short[cmsMAXCHANNELS];
	    }
	    
	    public void copyTo(_cmsCACHE cache)
	    {
	    	System.arraycopy(CacheIn, 0, cache.CacheIn, 0, cmsMAXCHANNELS);
	    	System.arraycopy(CacheOut, 0, cache.CacheOut, 0, cmsMAXCHANNELS);
	    }
	}
	
	// Transformation
	public static class _cmsTRANSFORM implements cmsHTRANSFORM
	{
		public int InputFormat, OutputFormat; // Keep formats for further reference
		
	    // Points to transform code
		public _cmsTransformFn xform;
	    
	    // Formatters, cannot be embedded into LUT because cache
		public cmsFormatter16 FromInput;
		public cmsFormatter16 ToOutput;
		
		public cmsFormatterFloat FromInputFloat;
		public cmsFormatterFloat ToOutputFloat;
	    
		// 1-pixel cache seed for zero as input (16 bits, read only)
	    public _cmsCACHE Cache;
	    
	    // A Pipeline holding the full (optimized) transform
	    public cmsPipeline Lut;
	    
	    // A Pipeline holding the gamut check. It goes from the input space to bilevel
	    public cmsPipeline GamutCheck;
	    
	    // Colorant tables
	    public cmsNAMEDCOLORLIST InputColorant; // Input Colorant table
	    public cmsNAMEDCOLORLIST OutputColorant; // Colorant table (for n chans > CMYK)
	    
	    // Informational only
	    public int EntryColorSpace;
	    public int ExitColorSpace;
	    
	    // Profiles used to create the transform
	    public cmsSEQ Sequence;
	    
	    public int dwOriginalFlags;
	    public double AdaptationState;
	    
	    // The intent of this transform. That is usually the last intent in the profilechain, but may differ
	    public int RenderingIntent;
	    
	    // An id that uniquely identifies the running context. May be null.
	    public cmsContext ContextID;
	    
	    // A user-defined pointer that can be used to store data for transform plug-ins
	    public Object UserData;
	    public _cmsFreeUserDataFn FreeUserData;
	    
	    public _cmsTRANSFORM()
	    {
	    	Cache = new _cmsCACHE();
	    }
	}
	
	// --------------------------------------------------------------------------------------------------
	
	public static cmsHTRANSFORM _cmsChain2Lab(cmsContext ContextID, int nProfiles, int InputFormat, int OutputFormat, final int[] Intents, final cmsHPROFILE[] hProfiles,
			final boolean[] BPC, final double[] AdaptationStates, int dwFlags)
	{
		return cmsgmt._cmsChain2Lab(ContextID, nProfiles, InputFormat, OutputFormat, Intents, hProfiles, BPC, AdaptationStates, dwFlags);
	}
	
	public static cmsToneCurve _cmsBuildKToneCurve(cmsContext ContextID, int nPoints, int nProfiles, final int[] Intents, final cmsHPROFILE[] hProfiles, final boolean[] BPC,
			final double[] AdaptationStates, int dwFlags)
	{
		return cmsgmt._cmsBuildKToneCurve(ContextID, nPoints, nProfiles, Intents, hProfiles, BPC, AdaptationStates, dwFlags);
	}
	
	public static boolean _cmsAdaptationMatrix(cmsMAT3 r, final cmsMAT3 ConeMatrix, final cmsCIEXYZ FromIll, final cmsCIEXYZ ToIll)
	{
		return cmswtpnt._cmsAdaptationMatrix(r, ConeMatrix, FromIll, ToIll);
	}
	
	public static boolean _cmsBuildRGB2XYZtransferMatrix(cmsMAT3 r, final cmsCIExyY WhitePoint, final cmsCIExyYTRIPLE Primaries)
	{
		return cmswtpnt._cmsBuildRGB2XYZtransferMatrix(r, WhitePoint, Primaries);
	}
}
