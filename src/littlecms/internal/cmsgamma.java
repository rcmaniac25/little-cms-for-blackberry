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

import net.rim.device.api.util.MathUtilities;
import littlecms.internal.helper.Utility;
import littlecms.internal.lcms2.cmsContext;
import littlecms.internal.lcms2.cmsCurveSegment;
import littlecms.internal.lcms2.cmsToneCurve;
import littlecms.internal.lcms2_plugin.cmsInterpParams;
import littlecms.internal.lcms2_plugin.cmsParametricCurveEvaluator;
import littlecms.internal.lcms2_plugin.cmsPluginBase;
import littlecms.internal.lcms2_plugin.cmsPluginParametricCurves;

/**
 * Tone curves are powerful constructs that can contain curves specified in diverse ways. 
 * The curve is stored in segments, where each segment can be sampled or specified by parameters.
 * a 16.bit simplification of the *whole* curve is kept for optimization purposes. For float operation, 
 * each segment is evaluated separately. Plug-ins may be used to define new parametric schemes, 
 * each plug-in may define up to MAX_TYPES_IN_LCMS_PLUGIN functions types. For defining a function, 
 * the plug-in should provide the type id, how many parameters each type has, and a pointer to
 * a procedure that evaluates the function. In the case of reverse evaluation, the evaluator will 
 * be called with the type id as a negative value, and a sampled version of the reversed curve 
 * will be built.
 */
//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmsgamma
{
	// ----------------------------------------------------------------- Implementation
	// Maxim number of nodes
	public static final int MAX_NODES_IN_CURVE   = 4097;
	public static final float MINUS_INF          = (-1E22F);
	public static final float PLUS_INF           = (+1E22F);
	
	// The list of supported parametric curves
	private static class _cmsParametricCurvesCollection
	{
		public int nFunctions;							// Number of supported functions in this chunk
		public int[] FunctionTypes;						// The identification types
		public int[] ParameterCount;					// Number of parameters for each function
		public cmsParametricCurveEvaluator Evaluator;	// The evaluator
		
	    public _cmsParametricCurvesCollection Next;		// Next in list
	    
	    public _cmsParametricCurvesCollection()
	    {
	    	this.FunctionTypes = new int[lcms2_plugin.MAX_TYPES_IN_LCMS_PLUGIN];
	    	this.ParameterCount = new int[lcms2_plugin.MAX_TYPES_IN_LCMS_PLUGIN];
	    }
	    
	    public _cmsParametricCurvesCollection(int nFunctions, int[] FunctionTypes, int[] ParameterCount, cmsParametricCurveEvaluator Evaluator, 
	    		_cmsParametricCurvesCollection Next)
	    {
	    	this();
	    	this.nFunctions = nFunctions;
	    	System.arraycopy(FunctionTypes, 0, this.FunctionTypes, 0, nFunctions);
	    	System.arraycopy(ParameterCount, 0, this.ParameterCount, 0, nFunctions);
	    	this.Evaluator = Evaluator;
	    	this.Next = Next;
	    }
	}
	
	private static final long PARAMETRIC_CURVES_UID = 0x9EFD118D3D891666L;
	
	// As a way to install new parametric curves
	public static boolean _cmsRegisterParametricCurvesPlugin(cmsPluginBase Data)
	{
	    cmsPluginParametricCurves Plugin = (cmsPluginParametricCurves)Data;
	    _cmsParametricCurvesCollection fl;
	    
	    if (Data == null)
	    {
	    	ParametricCurves = DefaultCurves;
	    	Utility.singletonStorageSet(PARAMETRIC_CURVES_UID, ParametricCurves);
	    	return true;
	    }
	    
	    fl = new _cmsParametricCurvesCollection();
	    
	    // Copy the parameters
	    fl.Evaluator  = Plugin.Evaluator;
	    fl.nFunctions = Plugin.nFunctions;
	    
	    // Make sure no mem overwrites
	    if (fl.nFunctions > lcms2_plugin.MAX_TYPES_IN_LCMS_PLUGIN)
	    {
	    	fl.nFunctions = lcms2_plugin.MAX_TYPES_IN_LCMS_PLUGIN;
	    }
	    
	    // Copy the data
	    System.arraycopy(Plugin.FunctionTypes, 0, fl.FunctionTypes, 0, fl.nFunctions);
	    System.arraycopy(Plugin.ParameterCount, 0, fl.ParameterCount, 0, fl.nFunctions);
	    
	    // Keep linked list
	    fl.Next = ParametricCurves;
	    ParametricCurves = fl;
	    Utility.singletonStorageSet(PARAMETRIC_CURVES_UID, ParametricCurves);
	    
	    // All is ok
	    return true;
	}
	
	// Search in type list, return position or -1 if not found
	private static int IsInSet(int Type, _cmsParametricCurvesCollection c)
	{
	    int i;
	    
	    for (i=0; i < c.nFunctions; i++)
	    {
	    	if (Math.abs(Type) == c.FunctionTypes[i])
	    	{
	    		return i;
	    	}
	    }
	    
	    return -1;
	}
	
	// Search for the collection which contains a specific type
	private static _cmsParametricCurvesCollection GetParametricCurveByType(int Type, int[] index)
	{
	    _cmsParametricCurvesCollection c;
	    int Position;
	    
	    if(ParametricCurves == null)
	    {
	    	Object obj;
	    	if((obj = Utility.singletonStorageGet(PARAMETRIC_CURVES_UID)) != null)
	    	{
	    		ParametricCurves = (_cmsParametricCurvesCollection)obj;
	    	}
	    	else
	    	{
	    		ParametricCurves = DefaultCurves;
	    		Utility.singletonStorageSet(PARAMETRIC_CURVES_UID, ParametricCurves);
	    	}
	    }
	    for (c = ParametricCurves; c != null; c = c.Next)
	    {
	        Position = IsInSet(Type, c);
	        
	        if (Position != -1)
	        {
	            if (index != null)
	            {
	            	index[0] = Position;
	            }
	            return c;
	        }
	    }
	    
	    return null;
	}
	
	// Low level allocate, which takes care of memory details. nEntries may be zero, and in this case 
	// no optimation curve is computed. nSegments may also be zero in the inverse case, where only the
	// optimization curve is given. Both features simultaneously is an error
	private static cmsToneCurve AllocateToneCurveStruct(cmsContext ContextID, int nEntries, int nSegments, final cmsCurveSegment[] Segments, final short[] Values)
	{
	    cmsToneCurve p;
	    int i;
	    
	    // We allow huge tables, which are then restricted for smoothing operations
	    if (nEntries > 65530 || nEntries < 0)
	    {
	        cmserr.cmsSignalError(ContextID, lcms2.cmsERROR_RANGE, Utility.LCMS_Resources.getString(LCMSResource.CMSGAMMA_TOO_MANY_ENTRIES), null);
	        return null;
	    }
	    
	    if (nEntries <= 0 && nSegments <= 0)
	    {
	        cmserr.cmsSignalError(ContextID, lcms2.cmsERROR_RANGE, Utility.LCMS_Resources.getString(LCMSResource.CMSGAMMA_NO_SEGMENTS), null);
	        return null;
	    }
	    
	    // Allocate all required pointers, etc.
	    p = new cmsToneCurve();
	    
	    // In this case, there are no segments
	    if (nSegments <= 0)
	    {
	        p.Segments = null;
	        p.Evals = null;
	    }
	    else
	    {
	        p.Segments = new cmsCurveSegment[nSegments];
	        for(i = 0; i < nSegments; i++)
	        {
	        	p.Segments[i] = new cmsCurveSegment();
	        }
	        
	        p.Evals = new cmsParametricCurveEvaluator[nSegments];
	    }
	    
	    p.nSegments = nSegments;
	    
	    // This 16-bit table contains a limited precision representation of the whole curve and is kept for
	    // increasing xput on certain operations.
	    if (nEntries <= 0)
	    {
	        p.Table16 = null;
	    }
	    else
	    {
	       p.Table16 = new short[nEntries];
	    }
	    
	    p.nEntries = nEntries;
	    
	    // Initialize members if requested
	    if (Values != null && (nEntries > 0))
	    {
	    	System.arraycopy(Values, 0, p.Table16, 0, nEntries);
	    	/*
	        for (i=0; i < nEntries; i++)
	        {
	        	p.Table16[i] = Values[i];
	        }
	        */
	    }
	    
	    // Initialize the segments stuff. The evaluator for each segment is located and a pointer to it
	    // is placed in advance to maximize performance.
	    if (Segments != null && (nSegments > 0))
	    {
	        _cmsParametricCurvesCollection c;
	        
	        p.SegInterp = new cmsInterpParams[nSegments];
	        
	        for (i=0; i< nSegments; i++)
	        {
	            // Type 0 is a special marker for table-based curves
	            if (Segments[i].Type == 0)
	            {
	            	p.SegInterp[i] = cmsintrp._cmsComputeInterpParams(ContextID, Segments[i].nGridPoints, 1, 1, null, lcms2_plugin.CMS_LERP_FLAGS_FLOAT);
	            }
	            
	            cmsCurveSegment nSeg = p.Segments[i];
	            cmsCurveSegment oSeg = Segments[i];
	            nSeg.x0 = oSeg.x0;
	            nSeg.x1 = oSeg.x1;
	            nSeg.Type = oSeg.Type;
	            nSeg.nGridPoints = oSeg.nGridPoints;
	            if(oSeg.Type != 0)
	            {
	            	System.arraycopy(oSeg.Params, 0, nSeg.Params, 0, 10);
	            }
	            else
	            {
	            	nSeg.SampledPoints = new float[oSeg.nGridPoints];
	            	if(oSeg.nGridPoints > 0 && oSeg.SampledPoints != null)
	            	{
	            		System.arraycopy(oSeg.SampledPoints, 0, nSeg.SampledPoints, 0, oSeg.nGridPoints);
	            	}
	            }
	            
	            if (Segments[i].Type == 0 && Segments[i].SampledPoints != null)
	            {
	            	p.Segments[i].SampledPoints = new float[Segments[i].nGridPoints];
	            	System.arraycopy(Segments[i].SampledPoints, 0, p.Segments[i].SampledPoints, 0, Segments[i].nGridPoints);
	            }
	            else
	            {
	            	p.Segments[i].SampledPoints = null;
	            }
	            
	            c = GetParametricCurveByType(Segments[i].Type, null);
	            if (c != null)
	            {
	            	p.Evals[i] = c.Evaluator;
	            }
	        }
	    }
	    
	    p.InterpParams = cmsintrp._cmsComputeInterpParams(ContextID, p.nEntries, 1, 1, p.Table16, lcms2_plugin.CMS_LERP_FLAGS_16BITS);
	    return p;
	}
	
	// Parametric Fn using floating point
	private static final cmsParametricCurveEvaluator DefaultEvalParametricFn = new cmsParametricCurveEvaluator()
	{
		public double run(int Type, double[] Params, double R)
		{
			double e, Val, disc;
			
		    switch (Type)
		    {
			    // X = Y ^ Gamma
			    case 1:
			        if (R < 0)
			        {
			        	Val = 0;
			        }
			        else
			        {
//#ifndef BlackBerrySDK4.5.0
			    		Val = MathUtilities.pow(R, Params[0]);
//#else
		    			Val = Utility.pow(R, Params[0]);
//#endif
			        }
			        break;
			        
			    // Type 1 Reversed: X = Y ^1/gamma
			    case -1:
			        if (R < 0)
			        {
			        	Val = 0;
			        }
			        else
			        {
//#ifndef BlackBerrySDK4.5.0
			    		Val = MathUtilities.pow(R, 1/Params[0]);
//#else
		    			Val = Utility.pow(R, 1/Params[0]);
//#endif
			        }
			        break;
			        
			    // CIE 122-1966
			    // Y = (aX + b)^Gamma  | X >= -b/a
			    // Y = 0               | else
			    case 2:
			        disc = -Params[2] / Params[1];
			        
			        if (R >= disc )
			        {
			        	e = Params[1]*R + Params[2];
			        	
			            if (e > 0)
			            {
//#ifndef BlackBerrySDK4.5.0
			            	Val = MathUtilities.pow(e, Params[0]);
//#else
		    				Val = Utility.pow(e, Params[0]);
//#endif
			            }
			            else
			            {
			            	Val = 0;
			            }
			        }
			        else
			        {
			        	Val = 0;
			        }
			        break;
			        
			     // Type 2 Reversed
			     // X = (Y ^1/g  - b) / a
			     case -2:
			         if (R < 0)
			         {
			        	 Val = 0;
			         }
			         else
			         {
//#ifndef BlackBerrySDK4.5.0
			        	 Val = (MathUtilities.pow(R, 1.0/Params[0]) - Params[2]) / Params[1];
//#else
			        	 Val = (Utility.pow(R, 1.0/Params[0]) - Params[2]) / Params[1];
//#endif
			         }
			         
			         if (Val < 0)
			         {
			        	 Val = 0;                            
			         }
			         break;
			         
			    // IEC 61966-3
			    // Y = (aX + b)^Gamma | X <= -b/a
			    // Y = c              | else
			    case 3:
			        disc = -Params[2] / Params[1];
			        if (disc < 0)
			        {
			        	disc = 0;
			        }
			        
			        if (R >= disc)
			        {
			        	e = Params[1]*R + Params[2];
			        	
			            if (e > 0)
			            {
//#ifndef BlackBerrySDK4.5.0
			            	Val = MathUtilities.pow(e, Params[0]) + Params[3];
//#else
			            	Val = Utility.pow(e, Params[0]) + Params[3];
//#endif
			            }
			            else
			            {
			            	Val = 0;
			            }
			        }
			        else
			        {
			        	Val = Params[3];
			        }
			        break;
			        
			    // Type 3 reversed
			    // X=((Y-c)^1/g - b)/a      | (Y>=c)
			    // X=-b/a                   | (Y<c) 
			    case -3:
			        if (R >= Params[3])
			        {
			        	e = R - Params[3];
			        	
			            if (e > 0)
			            {
//#ifndef BlackBerrySDK4.5.0
			            	Val = (MathUtilities.pow(e, 1/Params[0]) - Params[2]) / Params[1];
//#else
			            	Val = (Utility.pow(e, 1/Params[0]) - Params[2]) / Params[1];
//#endif
			            }
			            else
			            {
			            	Val = 0;
			            }
			        }
			        else
			        {
			        	Val = -Params[2] / Params[1];
			        }
			        break;
			        
			    // IEC 61966-2.1 (sRGB)
			    // Y = (aX + b)^Gamma | X >= d
			    // Y = cX             | X < d
			    case 4:
			        if (R >= Params[4])
			        {
			        	e = Params[1]*R + Params[2];
			        	
			            if (e > 0)
			            {
//#ifndef BlackBerrySDK4.5.0
			            	Val = MathUtilities.pow(e, Params[0]);
//#else
		    				Val = Utility.pow(e, Params[0]);
//#endif
			            }
			            else
			            {
			            	Val = 0;
			            }
			        }
			        else
			        {
			        	Val = R * Params[3];
			        }
			        break;
			        
			    // Type 4 reversed
			    // X=((Y^1/g-b)/a)    | Y >= (ad+b)^g
			    // X=Y/c              | Y< (ad+b)^g
			    case -4:
			        e = Params[1] * Params[4] + Params[2];
			        if (e < 0)
			        {
			        	disc = 0;
			        }
			        else
			        {
//#ifndef BlackBerrySDK4.5.0
		            	disc = MathUtilities.pow(e, Params[0]);
//#else
		            	disc = Utility.pow(e, Params[0]);
//#endif
			        }
			        
			        if (R >= disc)
			        {
//#ifndef BlackBerrySDK4.5.0
			        	Val = (MathUtilities.pow(R, 1.0/Params[0]) - Params[2]) / Params[1];
//#else
			        	Val = (Utility.pow(R, 1.0/Params[0]) - Params[2]) / Params[1];
//#endif
			        }
			        else
			        {
			            Val = R / Params[3];
			        }
			        break;
			        
			    // Y = (aX + b)^Gamma + e | X >= d
			    // Y = cX + f             | X < d
			    case 5:
			        if (R >= Params[4])
			        {
			        	e = Params[1]*R + Params[2];
			        	
			            if (e > 0)
			            {
//#ifndef BlackBerrySDK4.5.0
			            	Val = MathUtilities.pow(e, Params[0]) + Params[5];
//#else
			            	Val = Utility.pow(e, Params[0]) + Params[5];
//#endif
			            }
			            else
			            {
			            	Val = 0;
			            }
			        }        
			        else
			        {
			        	Val = R*Params[3] + Params[6];
			        }
			        break;
			        
			    // Reversed type 5
			    // X=((Y-e)1/g-b)/a   | Y >=(ad+b)^g+e), cd+f
			    // X=(Y-f)/c          | else
			    case -5:
			        disc = Params[3] * Params[4] + Params[6];
			        if (R >= disc)
			        {
			        	e = R - Params[5];
			            if (e < 0)
			            {
			            	Val = 0;
			            }
			            else
			            {
//#ifndef BlackBerrySDK4.5.0
			            	Val = (MathUtilities.pow(e, 1.0/Params[0]) - Params[2]) / Params[1];
//#else
			            	Val = (Utility.pow(e, 1.0/Params[0]) - Params[2]) / Params[1];
//#endif
			            }
			        }
			        else
			        {
			            Val = (R - Params[6]) / Params[3];
			        }
			        break;
			        
			    // Types 6,7,8 comes from segmented curves as described in ICCSpecRevision_02_11_06_Float.pdf
			    // Type 6 is basically identical to type 5 without d
			    
			    // Y = (a * X + b) ^ Gamma + c
			    case 6:
			        e = Params[1]*R + Params[2];
			        
			        if (e < 0)
			        {
			        	Val = 0;
			        }
			        else
			        {
//#ifndef BlackBerrySDK4.5.0
			        	Val = MathUtilities.pow(e, Params[0]) + Params[3];
//#else
			        	Val = Utility.pow(e, Params[0]) + Params[3];
//#endif
			        }
			        break;
			        
			    // ((Y - c) ^1/Gamma - b) / a                        
			    case -6:
			        e = R - Params[3];
			        if (e < 0)
			        {
			        	Val = 0;
			        }
			        else
			        {
//#ifndef BlackBerrySDK4.5.0
			        	Val = (MathUtilities.pow(e, 1.0/Params[0]) - Params[2]) / Params[1];
//#else
			        	Val = (Utility.pow(e, 1.0/Params[0]) - Params[2]) / Params[1];
//#endif
			        }
			        break;
			        
			    // Y = a * log (b * X^Gamma + c) + d
			    case 7:
//#ifndef BlackBerrySDK4.5.0
			    	e = Params[2] * MathUtilities.pow(R, Params[0]) + Params[3];
//#else
			    	e = Params[2] * Utility.pow(R, Params[0]) + Params[3];
//#endif
    				if (e <= 0)
    				{
    					Val = 0;
    				}
    				else
    				{
    					Val = Params[1]*Utility.log10(e) + Params[4];
    				}
    				break;
			       
			    // (Y - d) / a = log(b * X ^Gamma + c)
			    // pow(10, (Y-d) / a) = b * X ^Gamma + c
			    // pow((pow(10, (Y-d) / a) - c) / b, 1/g) = X 
			    case -7:
//#ifndef BlackBerrySDK4.5.0
			    	Val = MathUtilities.pow((MathUtilities.pow(10.0, (R-Params[4]) / Params[1]) - Params[3]) / Params[2], 1.0 / Params[0]);
//#else
			    	Val = Utility.pow((Utility.pow(10.0, (R-Params[4]) / Params[1]) - Params[3]) / Params[2], 1.0 / Params[0]);
//#endif
			    	break;
			       
			   //Y = a * b^(c*X+d) + e          
			   case 8:
//#ifndef BlackBerrySDK4.5.0
				   Val = (Params[0] * MathUtilities.pow(Params[1], Params[2] * R + Params[3]) + Params[4]);
//#else
				   Val = (Params[0] * Utility.pow(Params[1], Params[2] * R + Params[3]) + Params[4]);
//#endif
			       break;
			       
			   // Y = (log((y-e) / a) / log(b) - d ) / c
			   // a=0, b=1, c=2, d=3, e=4,
			   case -8:
			       disc = R - Params[4];
			       if (disc < 0)
		    	   {
			    	   Val = 0;
		    	   }
			       else
			       {
			    	   Val = (MathUtilities.log(disc / Params[0]) / MathUtilities.log(Params[1]) - Params[3]) / Params[2];         
			       }
			       break;
			       
			   // S-Shaped: (1 - (1-x)^1/g)^1/g                    
			   case 108:
//#ifndef BlackBerrySDK4.5.0
				   Val = MathUtilities.pow(1.0 - MathUtilities.pow(1 - R, 1/Params[0]), 1/Params[0]);
//#else
				   Val = Utility.pow(1.0 - Utility.pow(1 - R, 1/Params[0]), 1/Params[0]);
//#endif
				   break;
				   
			    // y = (1 - (1-x)^1/g)^1/g
			    // y^g = (1 - (1-x)^1/g)
			    // 1 - y^g = (1-x)^1/g
			    // (1 - y^g)^g = 1 - x
			    // 1 - (1 - y^g)^g
			    case -108:
//#ifndef BlackBerrySDK4.5.0
			    	Val = 1 - MathUtilities.pow(1 - MathUtilities.pow(R, Params[0]), Params[0]);
//#else
			    	Val = 1 - Utility.pow(1 - Utility.pow(R, Params[0]), Params[0]);
//#endif
			        break;
			        
			    default:
			        // Unsupported parametric curve. Should never reach here
			        return 0;
		    }
		    
		    return Val;
		}
	};
	
	// The built-in list
	private static final _cmsParametricCurvesCollection DefaultCurves = new _cmsParametricCurvesCollection(
			9,											// # of curve types
			new int[]{ 1, 2, 3, 4, 5, 6, 7, 8, 108 },	// Parametric curve ID
			new int[]{ 1, 3, 4, 5, 7, 4, 5, 5, 1 },		// Parameters by type
			DefaultEvalParametricFn,					// Evaluator
			null										// Next in chain
			);
	
	// The linked list head
	private static _cmsParametricCurvesCollection ParametricCurves;
	
	// Evaluate a segmented funtion for a single value. Return -1 if no valid segment found .
	// If fn type is 0, perform an interpolation on the table 
	private static double EvalSegmentedFn(final cmsToneCurve g, double R)
	{
	    int i;
	    
	    for (i = g.nSegments-1; i >= 0 ; --i)
	    {
	        // Check for domain
	        if ((R > g.Segments[i].x0) && (R <= g.Segments[i].x1))
	        {
	            // Type == 0 means segment is sampled
	            if (g.Segments[i].Type == 0)
	            {
	                float R1 = (float)(R - g.Segments[i].x0);
	                float[] Out = new float[1];
	                
	                // Setup the table (TODO: clean that)
	                g.SegInterp[i].Table = g.Segments[i].SampledPoints;
	                
	                g.SegInterp[i].Interpolation.getFloat().run(new float[]{R1}, Out, g.SegInterp[i]);
	                
	                return Out[0];
	            }
	            else
	            {
	            	return g.Evals[i].run(g.Segments[i].Type, g.Segments[i].Params, R);
	            }
	        }
	    }
	    
	    return MINUS_INF;
	}
	
	// Create an empty gamma curve, by using tables. This specifies only the limited-precision part, and leaves the
	// floating point description empty.
	public static cmsToneCurve cmsBuildTabulatedToneCurve16(cmsContext ContextID, int nEntries, final short[] Values)
	{
	    return AllocateToneCurveStruct(ContextID, nEntries, 0, null, Values);
	}
	
	private static int EntriesByGamma(double Gamma)
	{
	    if (Math.abs(Gamma - 1.0) < 0.001)
	    {
	    	return 2;
	    }
	    return 4096;
	}
	
	// Create a segmented gamma, fill the table
	public static cmsToneCurve cmsBuildSegmentedToneCurve(cmsContext ContextID, int nSegments, final cmsCurveSegment[] Segments)
	{
	    int i;
	    double R, Val;
	    cmsToneCurve g;
	    int nGridPoints = 4096;
	    
	    lcms2_internal._cmsAssert(Segments != null, "Segments != null");
	    
	    // Optimizatin for identity curves. 
	    if (nSegments == 1 && Segments[0].Type == 1)
	    {
	    	nGridPoints = EntriesByGamma(Segments[0].Params[0]);
	    }
	    
	    g = AllocateToneCurveStruct(ContextID, nGridPoints, nSegments, Segments, null);
	    if (g == null)
	    {
	    	return null;
	    }
	    
	    // Once we have the floating point version, we can approximate a 16 bit table of 4096 entries
	    // for performance reasons. This table would normally not be used except on 8/16 bits transforms.
	    for (i=0; i < nGridPoints; i++)
	    {
	        R = ((double)i) / (nGridPoints-1);
	        
	        Val = EvalSegmentedFn(g, R);
	        
	        // Round and saturate
	        g.Table16[i] = lcms2_internal._cmsQuickSaturateWord(Val * 65535.0);
	    }
	    
	    return g;
	}
	
	// Use a segmented curve to store the floating point table
	public static cmsToneCurve cmsBuildTabulatedToneCurveFloat(cmsContext ContextID, int nEntries, final float[] values)
	{
	    cmsCurveSegment[] Seg = new cmsCurveSegment[2];
	    
	    // Initialize segmented curve part up to 0
	    Seg[0] = new cmsCurveSegment();
	    Seg[0].x0 = -1;
	    Seg[0].x1 = 0;
	    Seg[0].Type = 6;
	    
	    Seg[0].Params[0] = 1;
	    Seg[0].Params[1] = 0;
	    Seg[0].Params[2] = 0;
	    Seg[0].Params[3] = 0;
	    Seg[0].Params[4] = 0;
	    
	    // From zero to any
	    Seg[1] = new cmsCurveSegment();
	    Seg[1].x0 = 0;
	    Seg[1].x1 = 1f;
	    Seg[1].Type = 0;
	    
	    Seg[1].nGridPoints = nEntries;
	    Seg[1].SampledPoints = values;
	    
	    return cmsBuildSegmentedToneCurve(ContextID, 2, Seg);
	}
	
	// Parametric curves
	//
	// Parameters goes as: Curve, a, b, c, d, e, f
	// Type is the ICC type +1
	// if type is negative, then the curve is analyticaly inverted
	public static cmsToneCurve cmsBuildParametricToneCurve(cmsContext ContextID, int Type, final double[] Params)
	{
	    cmsCurveSegment Seg0;
	    int[] Pos = new int[1];
	    int size;
	    _cmsParametricCurvesCollection c = GetParametricCurveByType(Type, Pos);
	    
	    lcms2_internal._cmsAssert(Params != null, "Params != null");
	    
	    if (c == null)
	    {
	    	cmserr.cmsSignalError(ContextID, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSGAMMA_INVALID_CURVE_TYPE), new Object[]{new Integer(Type)});     
	        return null;
	    }
	    
	    Seg0 = new cmsCurveSegment();
	    
	    Seg0.x0   = MINUS_INF;
	    Seg0.x1   = PLUS_INF;
	    Seg0.Type = Type;
	    
	    size = c.ParameterCount[Pos[0]];
	    System.arraycopy(Params, 0, Seg0.Params, 0, size);
	    
	    return cmsBuildSegmentedToneCurve(ContextID, 1, new cmsCurveSegment[]{Seg0});
	}
	
	// Build a gamma table based on gamma constant
	public static cmsToneCurve cmsBuildGamma(cmsContext ContextID, double Gamma)
	{
	    return cmsBuildParametricToneCurve(ContextID, 1, new double[]{Gamma});
	}
	
	// Free all memory taken by the gamma curve
	public static void cmsFreeToneCurve(cmsToneCurve Curve)
	{
	    cmsContext ContextID;
	    
	    if (Curve == null)
	    {
	    	return;
	    }
	    
	    ContextID = Curve.InterpParams.ContextID;
	    
	    cmsintrp._cmsFreeInterpParams(Curve.InterpParams);
	    
	    if (Curve.Table16 != null)
	    {
	    	//_cmsFree(ContextID, Curve.Table16);
	    	Curve.Table16 = null;
	    }
	    
	    if (Curve.Segments != null)
	    {
	        int i;
	        
	        for (i=0; i < Curve.nSegments; i++)
	        {
	            if (Curve.Segments[i].SampledPoints != null)
	            {
	                //_cmsFree(ContextID, Curve.Segments[i].SampledPoints);
	            	Curve.Segments[i].SampledPoints = null;
	            }
	            
	            if (Curve.SegInterp[i] != null)
	            {
	            	cmsintrp._cmsFreeInterpParams(Curve.SegInterp[i]);
	            }
	        }
	        
	        //_cmsFree(ContextID, Curve.Segments);
	        Curve.Segments = null;
	        //_cmsFree(ContextID, Curve.SegInterp);
	        Curve.SegInterp = null;
	    }

	    if (Curve.Evals != null)
	    {
	    	//_cmsFree(ContextID, Curve.Evals);
	    	Curve.Evals = null;
	    }
	    
	    /*
	    if (Curve != null)
	    {
	    	_cmsFree(ContextID, Curve);
	    }
	    */
	}
	
	// Utility function, free 3 gamma tables
	public static void cmsFreeToneCurveTriple(cmsToneCurve[] Curve)
	{
	    lcms2_internal._cmsAssert(Curve != null, "Curve != null");
	    
	    if (Curve[0] != null)
	    {
	    	cmsFreeToneCurve(Curve[0]);
	    }
	    if (Curve[1] != null)
	    {
	    	cmsFreeToneCurve(Curve[1]);
	    }
	    if (Curve[2] != null)
	    {
	    	cmsFreeToneCurve(Curve[2]);
	    }
	    
	    Curve[0] = Curve[1] = Curve[2] = null;
	}
	
	// Duplicate a gamma table
	public static cmsToneCurve cmsDupToneCurve(final cmsToneCurve In)
	{   
	    if (In == null)
	    {
	    	return null;
	    }
	    
	    return  AllocateToneCurveStruct(In.InterpParams.ContextID, In.nEntries, In.nSegments, In.Segments, In.Table16);
	}
	
	// Joins two curves for X and Y. Curves should be monotonic.
	// We want to get 
	//
	//	      y = Y^-1(X(t)) 
	//
	public static cmsToneCurve cmsJoinToneCurve(cmsContext ContextID, final cmsToneCurve X, final cmsToneCurve Y, int nResultingPoints)
	{
	    cmsToneCurve out = null;
	    cmsToneCurve Yreversed = null;
	    float t, x;
	    float[] Res = null;
	    int i;
	    
	    lcms2_internal._cmsAssert(X != null, "X != null");
	    lcms2_internal._cmsAssert(Y != null, "Y != null");
	    
	    Yreversed = cmsReverseToneCurveEx(nResultingPoints, Y);
	    if (Yreversed == null)
	    {
	    	return out;
	    }
	    
	    Res = new float[nResultingPoints];
	    
	    //Iterate
	    for (i=0; i < nResultingPoints; i++)
	    {
	        t = ((float)i) / (nResultingPoints-1);
	        x = cmsEvalToneCurveFloat(X,  t);
	        Res[i] = cmsEvalToneCurveFloat(Yreversed, x);
	    }
	    
	    // Allocate space for output
	    out = cmsBuildTabulatedToneCurveFloat(ContextID, nResultingPoints, Res);
	    
	    if (Yreversed != null)
	    {
	    	cmsFreeToneCurve(Yreversed);
	    }
	    
	    return out;
	}
	
	// Get the surrounding nodes. This is tricky on non-monotonic tables 
	private static int GetInterval(double In, final short[] LutTable, final cmsInterpParams p)
	{   
	    int i;
	    int y0, y1;
	    
	    // A 1 point table is not allowed
	    if (p.Domain[0] < 1)
	    {
	    	return -1;
	    }
	    
	    // Let's see if ascending or descending. 
	    if (LutTable[0] < LutTable[p.Domain[0]])
	    {
	        // Table is overall ascending
	        for (i=p.Domain[0]-1; i >=0; --i)
	        {
	            y0 = LutTable[i]; 
	            y1 = LutTable[i+1];
	            
	            if (y0 <= y1) // Increasing
	            {
	                if (In >= y0 && In <= y1)
	                {
	                	return i;
	                }
	            }
	            else
	            {
	                if (y1 < y0) // Decreasing
	                {
	                    if (In >= y1 && In <= y0)
	                    {
	                    	return i;
	                    }
	                }
	            }
	        }
	    }
	    else
	    {
	        // Table is overall descending
	        for (i=0; i < p.Domain[0]; i++)
	        {
	            y0 = LutTable[i]; 
	            y1 = LutTable[i+1];
	            
	            if (y0 <= y1) // Increasing
	            {
	                if (In >= y0 && In <= y1)
	                {
	                	return i;
	                }
	            }
	            else
	            {
	                if (y1 < y0) // Decreasing
	                {
	                    if (In >= y1 && In <= y0)
	                    {
	                    	return i;
	                    }
	                }
	            }
	        }
	    }
	    
	    return -1;
	}
	
	// Reverse a gamma table
	public static cmsToneCurve cmsReverseToneCurveEx(int nResultSamples, final cmsToneCurve InCurve)
	{
	    cmsToneCurve out;
	    double a = 1, b = 0, y, x1, y1, x2, y2;
	    int i, j;
	    boolean Ascending;
	    
	    lcms2_internal._cmsAssert(InCurve != null, "InCurve != null");
	    
	    // Try to reverse it analytically whatever possible
	    if (InCurve.nSegments == 1 && InCurve.Segments[0].Type > 0 && InCurve.Segments[0].Type <= 5)
	    {
	        return cmsBuildParametricToneCurve(InCurve.InterpParams.ContextID, 
	                                       -(InCurve.Segments[0].Type), 
	                                       InCurve.Segments[0].Params);
	    }
	    
	    // Nope, reverse the table. 
	    out = cmsBuildTabulatedToneCurve16(InCurve.InterpParams.ContextID, nResultSamples, null);
	    if (out == null)
	    {
	    	return null;
	    }
	    
	    // We want to know if this is an ascending or descending table
	    Ascending = !cmsIsToneCurveDescending(InCurve);
	    
	    // Iterate across Y axis
	    for (i=0; i <  nResultSamples; i++)
	    {
	        y = i * 65535.0 / (nResultSamples - 1);
	        
	        // Find interval in which y is within. 
	        j = GetInterval(y, InCurve.Table16, InCurve.InterpParams);
	        if (j >= 0)
	        {
	            // Get limits of interval
	            x1 = InCurve.Table16[j]; 
	            x2 = InCurve.Table16[j+1];
	            
	            y1 = (j * 65535.0) / (InCurve.nEntries - 1);
	            y2 = ((j+1) * 65535.0 ) / (InCurve.nEntries - 1);
	            
	            // If collapsed, then use any
	            if (x1 == x2)
	            {
	                out.Table16[i] = lcms2_internal._cmsQuickSaturateWord(Ascending ? y2 : y1);
	                continue;
	            }
	            else
	            {
	                // Interpolate      
	                a = (y2 - y1) / (x2 - x1);
	                b = y2 - a * x2;
	            }
	        }
	        
	        out.Table16[i] = lcms2_internal._cmsQuickSaturateWord(a* y + b);
	    }
	    
	    return out;
	}
	
	// Reverse a gamma table
	public static cmsToneCurve cmsReverseToneCurve(final cmsToneCurve InGamma)
	{
	    lcms2_internal._cmsAssert(InGamma != null, "InGamma != null");
	    
	    return cmsReverseToneCurveEx(InGamma.nEntries, InGamma);
	}
	
	// From: Eilers, P.H.C. (1994) Smoothing and interpolation with finite
	// differences. in: Graphic Gems IV, Heckbert, P.S. (ed.), Academic press.
	//
	// Smoothing and interpolation with second differences.
	//
	//   Input:  weights (w), data (y): vector from 1 to m.
	//   Input:  smoothing parameter (lambda), length (m).
	//   Output: smoothed vector (z): vector from 1 to m.
	
	private static boolean smooth2(cmsContext ContextID, float[] w, float[] y, float[] z, float lambda, int m)
	{
	    int i, i1, i2;
	    float[] c, d, e;
	    
	    c = new float[MAX_NODES_IN_CURVE];
	    d = new float[MAX_NODES_IN_CURVE];
	    e = new float[MAX_NODES_IN_CURVE];
	    
	    d[1] = w[1] + lambda;
	    c[1] = -2 * lambda / d[1];
	    e[1] = lambda /d[1];
	    z[1] = w[1] * y[1];
	    d[2] = w[2] + 5 * lambda - d[1] * c[1] *  c[1];
	    c[2] = (-4 * lambda - d[1] * c[1] * e[1]) / d[2];
	    e[2] = lambda / d[2];
	    z[2] = w[2] * y[2] - c[1] * z[1];
	    
	    for (i = 3; i < m - 1; i++)
	    {
	        i1 = i - 1; i2 = i - 2;
	        d[i]= w[i] + 6 * lambda - c[i1] * c[i1] * d[i1] - e[i2] * e[i2] * d[i2];
	        c[i] = (-4 * lambda -d[i1] * c[i1] * e[i1])/ d[i];
	        e[i] = lambda / d[i];
	        z[i] = w[i] * y[i] - c[i1] * z[i1] - e[i2] * z[i2];
	    }
	    
	    i1 = m - 2; i2 = m - 3;
	    
	    d[m - 1] = w[m - 1] + 5 * lambda -c[i1] * c[i1] * d[i1] - e[i2] * e[i2] * d[i2];
	    c[m - 1] = (-2 * lambda - d[i1] * c[i1] * e[i1]) / d[m - 1];
	    z[m - 1] = w[m - 1] * y[m - 1] - c[i1] * z[i1] - e[i2] * z[i2];
	    i1 = m - 1; i2 = m - 2;
	    
	    d[m] = w[m] + lambda - c[i1] * c[i1] * d[i1] - e[i2] * e[i2] * d[i2];
	    z[m] = (w[m] * y[m] - c[i1] * z[i1] - e[i2] * z[i2]) / d[m];
	    z[m - 1] = z[m - 1] / d[m - 1] - c[m - 1] * z[m];
	    
	    for (i = m - 2; 1<= i; i--)
	    {
	    	z[i] = z[i] / d[i] - c[i] * z[i + 1] - e[i] * z[i + 2];
	    }
	    
	    return true;
	}
	
	// Smooths a curve sampled at regular intervals. 
	public static boolean cmsSmoothToneCurve(cmsToneCurve Tab, double lambda)
	{
	    float[] w = new float[MAX_NODES_IN_CURVE], y = new float[MAX_NODES_IN_CURVE], z = new float[MAX_NODES_IN_CURVE];
	    int i, nItems, Zeros, Poles;
	    
	    if (Tab == null)
	    {
	    	return false;
	    }
	    
	    if (cmsIsToneCurveLinear(Tab))
	    {
	    	return false; // Nothing to do
	    }
	    
	    nItems = Tab.nEntries;
	    
	    if (nItems >= MAX_NODES_IN_CURVE)
	    {
	        cmserr.cmsSignalError(Tab.InterpParams.ContextID, lcms2.cmsERROR_RANGE, Utility.LCMS_Resources.getString(LCMSResource.CMSGAMMA_TOO_MANY_POINTS), null);
	        return false;
	    }
	    
	    for (i=0; i < nItems; i++)
	    {
	        y[i+1] = Tab.Table16[i];
	        w[i+1] = 1f;
	    }
	    
	    if (!smooth2(Tab.InterpParams.ContextID, w, y, z, (float)lambda, nItems))
	    {
	    	return false;
	    }
	    
	    // Do some reality - checking...
	    Zeros = Poles = 0;
	    for (i=nItems; i > 1; --i)
	    {
	        if (z[i] == 0)
	        {
	        	Zeros++;
	        }
	        if (z[i] >= 65535)
	        {
	        	Poles++;
	        }
	        if (z[i] < z[i-1])
	        {
	        	return false; // Non-Monotonic
	        }
	    }
	    
	    if (Zeros > (nItems / 3))
	    {
	    	return false; // Degenerated, mostly zeros
	    }
	    if (Poles > (nItems / 3))
	    {
	    	return false; // Degenerated, mostly poles
	    }
	    
	    // Seems ok
	    for (i=0; i < nItems; i++)
	    {
	        // Clamp to cmsUInt16Number
	        Tab.Table16[i] = lcms2_internal._cmsQuickSaturateWord(z[i+1]);
	    }
	    
	    return true;
	}
	
	// Is a table linear? Do not use parametric since we cannot guarantee some weird parameters resulting
	// in a linear table. This way assures it is linear in 12 bits, which should be enought in most cases.
	public static boolean cmsIsToneCurveLinear(final cmsToneCurve Curve)
	{
	    int i;
	    int diff;
	    
	    lcms2_internal._cmsAssert(Curve != null, "Curve != null");
	    
	    for (i=0; i < Curve.nEntries; i++)
	    {
	        diff = Math.abs((int)Curve.Table16[i] - (int)cmslut._cmsQuantizeVal(i, Curve.nEntries));
	        if (diff > 0x0f)
	        {
	        	return false;
	        }
	    }
	    
	    return true;
	}
	
	// Same, but for monotonicity
	public static boolean cmsIsToneCurveMonotonic(final cmsToneCurve t)
	{
	    int n;
	    int i, last;
	    
	    lcms2_internal._cmsAssert(t != null, "t != null");
	    
	    n    = t.nEntries;
	    last = t.Table16[n-1];
	    
	    for (i = n-2; i >= 0; --i)
	    {
	        if (t.Table16[i] > last)
	        {
	        	return false;
	        }
	        else
	        {
	        	last = t.Table16[i];
	        }
	    }
	    
	    return true;
	}
	
	// Same, but for descending tables
	public static boolean cmsIsToneCurveDescending(final cmsToneCurve t)
	{
	    lcms2_internal._cmsAssert(t != null, "t != null");
	    
	    return t.Table16[0] > t.Table16[t.nEntries-1];
	}
	
	// Another info fn: is out gamma table multisegment?
	public static boolean cmsIsToneCurveMultisegment(final cmsToneCurve t)
	{
		lcms2_internal._cmsAssert(t != null, "t != null");
		
	    return t.nSegments > 1;
	}
	
	public static int cmsGetToneCurveParametricType(final cmsToneCurve t)
	{
		lcms2_internal._cmsAssert(t != null, "t != null");
		
	    if (t.nSegments != 1)
	    {
	    	return 0;
	    }
	    return t.Segments[0].Type;
	}
	
	// We need accuracy this time
	public static float cmsEvalToneCurveFloat(final cmsToneCurve Curve, float v)
	{
		lcms2_internal._cmsAssert(Curve != null, "Curve != null");
		
	    // Check for 16 bits table. If so, this is a limited-precision tone curve
	    if (Curve.nSegments == 0)
	    {
	        short In, Out;
	        
	        In = (short)lcms2_internal._cmsQuickSaturateWord(v * 65535.0);
	        Out = cmsEvalToneCurve16(Curve, In);
	        
	        return ((Out & 0xFFFF) * (1 / 65535f));
	    }
	    
	    return (float)EvalSegmentedFn(Curve, v);
	}
	
	// We need xput over here
	public static short cmsEvalToneCurve16(final cmsToneCurve Curve, short v)
	{
		short[] out = new short[1];
		
		lcms2_internal._cmsAssert(Curve != null, "Curve != null");
		
	    Curve.InterpParams.Interpolation.get16().run(new short[]{v}, out, Curve.InterpParams);
	    return out[0];
	}
	
	// Least squares fitting.
	// A mathematical procedure for finding the best-fitting curve to a given set of points by 
	// minimizing the sum of the squares of the offsets ("the residuals") of the points from the curve. 
	// The sum of the squares of the offsets is used instead of the offset absolute values because 
	// this allows the residuals to be treated as a continuous differentiable quantity. 
	//
	// y = f(x) = x ^ g
	//
	// R  = (yi - (xi^g))
	// R2 = (yi - (xi^g))2
	// SUM R2 = SUM (yi - (xi^g))2
	// 
	// dR2/dg = -2 SUM x^g log(x)(y - x^g)    
	// solving for dR2/dg = 0 
	// 
	// g = 1/n * SUM(log(y) / log(x)) 
	
	public static double cmsEstimateGamma(final cmsToneCurve t, double Precision)
	{
		double gamma, sum, sum2;
		double n, x, y, Std;
	    int i;
	    
	    lcms2_internal._cmsAssert(t != null, "t != null");
	    
	    sum = sum2 = n = 0;
	    
	    // Excluding endpoints   
	    for (i=1; i < (MAX_NODES_IN_CURVE-1); i++)
	    {
	        x = ((double)i) / (MAX_NODES_IN_CURVE-1);
	        y = cmsEvalToneCurveFloat(t, (float)x);
	        
	        // Avoid 7% on lower part to prevent 
	        // artifacts due to linear ramps
	        
	        if (y > 0. && y < 1. && x > 0.07)
	        {
	            gamma = MathUtilities.log(y) / MathUtilities.log(x);
	            sum  += gamma;
	            sum2 += gamma * gamma;
	            n++;
	        }
	    }
	    
	    // Take a look on SD to see if gamma isn't exponential at all
	    Std = Math.sqrt((n * sum2 - sum * sum) / (n*(n-1)));
	    
	    if (Std > Precision)
	    {
	    	return -1.0;
	    }
	    
	    return (sum / n);   // The mean
	}
}
