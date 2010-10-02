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

import littlecms.internal.helper.Utility;
import littlecms.internal.lcms2.cmsCIEXYZ;
import littlecms.internal.lcms2.cmsContext;
import littlecms.internal.lcms2.cmsHPROFILE;
import littlecms.internal.lcms2.cmsMLU;
import littlecms.internal.lcms2.cmsPSEQDESC;
import littlecms.internal.lcms2.cmsPipeline;
import littlecms.internal.lcms2.cmsProfileID;
import littlecms.internal.lcms2.cmsSEQ;
import littlecms.internal.lcms2.cmsToneCurve;
import littlecms.internal.lcms2_plugin.cmsMAT3;

/** Read tags using low-level functions, provides necessary glue code to adapt versions, etc.*/
//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmsio1
{
	// LUT tags
	private static final int[] Device2PCS16 =  {lcms2.cmsSigAToB0Tag,	// Perceptual
		lcms2.cmsSigAToB1Tag,											// Relative colorimetric
		lcms2.cmsSigAToB2Tag,											// Saturation
		lcms2.cmsSigAToB1Tag };											// Absolute colorimetric
	
	private static final int[] Device2PCSFloat = {lcms2.cmsSigDToB0Tag,	// Perceptual
		lcms2.cmsSigDToB1Tag,											// Relative colorimetric
		lcms2.cmsSigDToB2Tag,											// Saturation
		lcms2.cmsSigDToB3Tag };											// Absolute colorimetric
	
	private static final int[] PCS2Device16 = {lcms2.cmsSigBToA0Tag,	// Perceptual
		lcms2.cmsSigBToA1Tag,											// Relative colorimetric
		lcms2.cmsSigBToA2Tag,											// Saturation
		lcms2.cmsSigBToA1Tag };											// Absolute colorimetric
	
	private static final int[] PCS2DeviceFloat = {lcms2.cmsSigBToD0Tag,	// Perceptual
		lcms2.cmsSigBToD1Tag,											// Relative colorimetric
		lcms2.cmsSigBToD2Tag,											// Saturation
		lcms2.cmsSigBToD3Tag };											// Absolute colorimetric
	
	// Factors to convert from 1.15 fixed point to 0..1.0 range and vice-versa
	private static final double InpAdj = (1.0/lcms2_internal.MAX_ENCODEABLE_XYZ); // (65536.0/(65535.0*2.0))
	private static final double OutpAdj = (lcms2_internal.MAX_ENCODEABLE_XYZ); // ((2.0*65535.0)/65536.0)
	
	// Several resources for gray conversions.
	private static final double[] GrayInputMatrix = { (InpAdj*lcms2.cmsD50X), (InpAdj*lcms2.cmsD50Y), (InpAdj*lcms2.cmsD50Z) };
	private static final double[] OneToThreeInputMatrix = { 1, 1, 1 };   
	private static final double[] PickYMatrix = { 0, (OutpAdj*lcms2.cmsD50Y), 0 };   
	private static final double[] PickLstarMatrix = { 1, 0, 0 };
	
	// Get a media white point fixing some issues found in certain old profiles
	public static boolean _cmsReadMediaWhitePoint(cmsCIEXYZ Dest, cmsHPROFILE hProfile)
	{
	    cmsCIEXYZ Tag;
	    
	    lcms2_internal._cmsAssert(Dest != null, "Dest != null");
	    
	    Tag = (cmsCIEXYZ)cmsio0.cmsReadTag(hProfile, lcms2.cmsSigMediaWhitePointTag);
	    
	    // If no wp, take D50
	    if (Tag == null)
	    {
	    	Dest.X = lcms2.cmsD50X;
	    	Dest.Y = lcms2.cmsD50Y;
	    	Dest.Z = lcms2.cmsD50Z;
	        return true;
	    }
	    
	    // V2 display profiles should give D50
	    if (cmsio0.cmsGetEncodedICCversion(hProfile) < 0x4000000)
	    {
	        if (cmsio0.cmsGetDeviceClass(hProfile) == lcms2.cmsSigDisplayClass)
	        {
	        	Dest.X = lcms2.cmsD50X;
		    	Dest.Y = lcms2.cmsD50Y;
		    	Dest.Z = lcms2.cmsD50Z;
	            return true;            
	        }
	    }
	    
	    // All seems ok
	    Dest.X = Tag.X;
    	Dest.Y = Tag.Y;
    	Dest.Z = Tag.Z;
	    return true;
	}
	
	// Chromatic adaptation matrix. Fix some issues as well
	public static boolean _cmsReadCHAD(cmsMAT3 Dest, cmsHPROFILE hProfile)
	{
	    cmsMAT3 Tag;
	    
	    lcms2_internal._cmsAssert(Dest != null, "Dest != null");
	    
	    Tag = (cmsMAT3)cmsio0.cmsReadTag(hProfile, lcms2.cmsSigChromaticAdaptationTag);
	    
	    if (Tag != null)
	    {
	    	double[] temp = new double[3*3];
	    	cmsmtrx._cmsMAT3get(Tag, temp, 0);
	    	cmsmtrx._cmsMAT3set(Dest, temp, 0);
	        return true;
	    }
	    
	    // No CHAD available, default it to identity
	    cmsmtrx._cmsMAT3identity(Dest);
	    
	    // V2 display profiles should give D50
	    if (cmsio0.cmsGetEncodedICCversion(hProfile) < 0x4000000)
	    {
	        if (cmsio0.cmsGetDeviceClass(hProfile) == lcms2.cmsSigDisplayClass)
	        {
	            cmsCIEXYZ White = (cmsCIEXYZ)cmsio0.cmsReadTag(hProfile, lcms2.cmsSigMediaWhitePointTag);
	            
	            if (White == null)
	            {
	            	cmsmtrx._cmsMAT3identity(Dest);
	                return true;
	            }
	            
	            return cmswtpnt._cmsAdaptationMatrix(Dest, null, lcms2.cmsD50_XYZ, White);
	        }
	    }
	    
	    return true;
	}
	
	// Auxiliar, read colorants as a MAT3 structure. Used by any function that needs a matrix-shaper
	public static boolean ReadICCMatrixRGB2XYZ(cmsMAT3 r, cmsHPROFILE hProfile)
	{
	    cmsCIEXYZ PtrRed, PtrGreen, PtrBlue;
	    
	    lcms2_internal._cmsAssert(r != null, "r != null");
	    
	    PtrRed   = (cmsCIEXYZ)cmsio0.cmsReadTag(hProfile, lcms2.cmsSigRedColorantTag);
	    PtrGreen = (cmsCIEXYZ)cmsio0.cmsReadTag(hProfile, lcms2.cmsSigGreenColorantTag);
	    PtrBlue  = (cmsCIEXYZ)cmsio0.cmsReadTag(hProfile, lcms2.cmsSigBlueColorantTag);
	    
	    if (PtrRed == null || PtrGreen == null || PtrBlue == null)
	    {
	    	return false;
	    }
	    
	    cmsmtrx._cmsVEC3init(r.v[0], PtrRed.X, PtrGreen.X, PtrBlue.X);
	    cmsmtrx._cmsVEC3init(r.v[1], PtrRed.Y, PtrGreen.Y, PtrBlue.Y);
	    cmsmtrx._cmsVEC3init(r.v[2], PtrRed.Z, PtrGreen.Z, PtrBlue.Z);
	    
	    return true;
	}
	
	// Gray input pipeline
	private static cmsPipeline BuildGrayInputMatrixPipeline(cmsHPROFILE hProfile)
	{
	    cmsToneCurve GrayTRC;
	    cmsPipeline Lut;
	    cmsContext ContextID = cmsio0.cmsGetProfileContextID(hProfile);
	    
	    GrayTRC = (cmsToneCurve)cmsio0.cmsReadTag(hProfile, lcms2.cmsSigGrayTRCTag);
	    if (GrayTRC == null)
	    {
	    	return null;
	    }
	    
	    Lut = cmslut.cmsPipelineAlloc(ContextID, 1, 3);
	    if (Lut == null)
	    {
	    	return null;
	    }
	    
	    if (cmsio0.cmsGetPCS(hProfile) == lcms2.cmsSigLabData)
	    {
	        // In this case we implement the profile as an  identity matrix plus 3 tone curves
	        short[] Zero = { (short)0x8080, (short)0x8080 };
	        cmsToneCurve EmptyTab;
	        cmsToneCurve[] LabCurves = new cmsToneCurve[3];
	        
	        EmptyTab = cmsgamma.cmsBuildTabulatedToneCurve16(ContextID, 2, Zero); 
	        
	        if (EmptyTab == null)
	        {
	        	cmslut.cmsPipelineFree(Lut);
	        	return null;
	        }
	        
	        LabCurves[0] = GrayTRC;
	        LabCurves[1] = EmptyTab;
	        LabCurves[2] = EmptyTab;
	        
	        cmslut.cmsPipelineInsertStage(Lut, lcms2.cmsAT_END, cmslut.cmsStageAllocMatrix(ContextID, 3,  1, OneToThreeInputMatrix, null));
	        cmslut.cmsPipelineInsertStage(Lut, lcms2.cmsAT_END, cmslut.cmsStageAllocToneCurves(ContextID, 3, LabCurves));
	        
	        cmsgamma.cmsFreeToneCurve(EmptyTab);
	    }
	    else
	    {
	    	cmslut.cmsPipelineInsertStage(Lut, lcms2.cmsAT_END, cmslut.cmsStageAllocToneCurves(ContextID, 1, new cmsToneCurve[]{GrayTRC}));
	    	cmslut.cmsPipelineInsertStage(Lut, lcms2.cmsAT_END, cmslut.cmsStageAllocMatrix(ContextID, 3,  1, GrayInputMatrix, null));
	    }
	    
	    return Lut;
	}
	
	// RGB Matrix shaper
	private static cmsPipeline BuildRGBInputMatrixShaper(cmsHPROFILE hProfile)
	{
	    cmsPipeline Lut;
	    cmsMAT3 Mat = new cmsMAT3();
	    cmsToneCurve[] Shapes = new cmsToneCurve[3];
	    cmsContext ContextID = cmsio0.cmsGetProfileContextID(hProfile);
	    int i, j;
	    
	    if (!ReadICCMatrixRGB2XYZ(Mat, hProfile))
	    {
	    	return null;
	    }
	    
	    // XYZ PCS in encoded in 1.15 format, and the matrix output comes in 0..0xffff range, so
	    // we need to adjust the output by a factor of (0x10000/0xffff) to put data in 
	    // a 1.16 range, and then a >> 1 to obtain 1.15. The total factor is (65536.0)/(65535.0*2)
	    
	    for (i=0; i < 3; i++)
	    {
	        for (j=0; j < 3; j++)
	        {
	        	Mat.v[i].n[j] *= InpAdj;
	        }
	    }
	    
	    Shapes[0] = (cmsToneCurve)cmsio0.cmsReadTag(hProfile, lcms2.cmsSigRedTRCTag);        
	    Shapes[1] = (cmsToneCurve)cmsio0.cmsReadTag(hProfile, lcms2.cmsSigGreenTRCTag);
	    Shapes[2] = (cmsToneCurve)cmsio0.cmsReadTag(hProfile, lcms2.cmsSigBlueTRCTag);
	    
	    if (Shapes[0] == null || Shapes[1] == null || Shapes[2] == null)
	    {
	    	return null;
	    }
	    
	    Lut = cmslut.cmsPipelineAlloc(ContextID, 3, 3);
	    if (Lut != null)
	    {
	    	cmslut.cmsPipelineInsertStage(Lut, lcms2.cmsAT_END, cmslut.cmsStageAllocToneCurves(ContextID, 3, Shapes));
	    	double[] vals = new double[3*3];
	    	cmsmtrx._cmsMAT3get(Mat, vals, 0);
	    	cmslut.cmsPipelineInsertStage(Lut, lcms2.cmsAT_END, cmslut.cmsStageAllocMatrix(ContextID, 3, 3, vals, null));
	    }
	    
	    return Lut;
	}
	
	// Read and create a BRAND NEW MPE LUT from a given profile. All stuff dependent of version, etc
	// is adjusted here in order to create a LUT that takes care of all those details
	public static cmsPipeline _cmsReadInputLUT(cmsHPROFILE hProfile, int Intent)
	{
	    int OriginalType;
	    int tag16    = Device2PCS16[Intent];
	    int tagFloat = Device2PCSFloat[Intent];
	    cmsContext ContextID = cmsio0.cmsGetProfileContextID(hProfile);
	    
	    if (cmsio0.cmsIsTag(hProfile, tagFloat)) // Float tag takes precedence
	    {
	        // Floating point LUT are always V4, so no adjustment is required
	        return cmslut.cmsPipelineDup((cmsPipeline)cmsio0.cmsReadTag(hProfile, tagFloat));
	    }
	    
	    // Revert to perceptual if no tag is found
	    if (!cmsio0.cmsIsTag(hProfile, tag16))
	    {
	        tag16 = Device2PCS16[0];
	    }
	    
	    if (cmsio0.cmsIsTag(hProfile, tag16)) // Is there any LUT-Based table?
	    {
	        // Check profile version and LUT type. Do the necessary adjustments if needed
	    	
	        // First read the tag
	        cmsPipeline Lut = (cmsPipeline)cmsio0.cmsReadTag(hProfile, tag16);
	        if (Lut == null)
	        {
	        	return null;
	        }
	        
	        // After reading it, we have now info about the original type
	        OriginalType = cmsio0._cmsGetTagTrueType(hProfile, tag16);
	        
	        // The profile owns the Lut, so we need to copy it
	        Lut = cmslut.cmsPipelineDup(Lut);
	        
	        // We need to adjust data only for Lab16 on output
	        if (OriginalType != lcms2.cmsSigLut16Type || cmsio0.cmsGetPCS(hProfile) != lcms2.cmsSigLabData)
	        {
	        	return Lut;
	        }
	        
	        // Add a matrix for conversion V2 to V4 Lab PCS
	        cmslut.cmsPipelineInsertStage(Lut, lcms2.cmsAT_END, cmslut._cmsStageAllocLabV2ToV4(ContextID));
	        return Lut;
	    }
	    
	    // Lut was not found, try to create a matrix-shaper
	    
	    // Check if this is a grayscale profile.
	    if (cmsio0.cmsGetColorSpace(hProfile) == lcms2.cmsSigGrayData)
	    {
	        // if so, build appropiate conversion tables. 
	        // The tables are the PCS iluminant, scaled across GrayTRC
	        return BuildGrayInputMatrixPipeline(hProfile);              
	    }
	    
	    // Not gray, create a normal matrix-shaper 
	    return BuildRGBInputMatrixShaper(hProfile);
	}
	
	// ---------------------------------------------------------------------------------------------------------------
	
	// Gray output pipeline. 
	// XYZ -> Gray or Lab -> Gray. Since we only know the GrayTRC, we need to do some assumptions. Gray component will be
	// given by Y on XYZ PCS and by L* on Lab PCS, Both across inverse TRC curve.
	// The complete pipeline on XYZ is Matrix[3:1] -> Tone curve and in Lab Matrix[3:1] -> Tone Curve as well.
	
	private static cmsPipeline BuildGrayOutputPipeline(cmsHPROFILE hProfile)
	{
	    cmsToneCurve GrayTRC, RevGrayTRC;
	    cmsPipeline Lut;
	    cmsContext ContextID = cmsio0.cmsGetProfileContextID(hProfile);
	    
	    GrayTRC = (cmsToneCurve)cmsio0.cmsReadTag(hProfile, lcms2.cmsSigGrayTRCTag);       
	    if (GrayTRC == null)
	    {
	    	return null;
	    }
	    
	    RevGrayTRC = cmsgamma.cmsReverseToneCurve(GrayTRC);
	    if (RevGrayTRC == null)
	    {
	    	return null;
	    }
	    
	    Lut = cmslut.cmsPipelineAlloc(ContextID, 3, 1);
	    if (Lut == null)
	    {
	    	cmsgamma.cmsFreeToneCurve(RevGrayTRC);
	        return null;
	    }
	    
	    if (cmsio0.cmsGetPCS(hProfile) == lcms2.cmsSigLabData)
	    {
	    	cmslut.cmsPipelineInsertStage(Lut, lcms2.cmsAT_END, cmslut.cmsStageAllocMatrix(ContextID, 1,  3, PickLstarMatrix, null));
	    }
	    else
	    {
	    	cmslut.cmsPipelineInsertStage(Lut, lcms2.cmsAT_END, cmslut.cmsStageAllocMatrix(ContextID, 1,  3, PickYMatrix, null));
	    }
	    
	    cmslut.cmsPipelineInsertStage(Lut, lcms2.cmsAT_END, cmslut.cmsStageAllocToneCurves(ContextID, 1, new cmsToneCurve[]{RevGrayTRC}));
	    cmsgamma.cmsFreeToneCurve(RevGrayTRC);
	    
	    return Lut;
	}
	
	private static cmsPipeline BuildRGBOutputMatrixShaper(cmsHPROFILE hProfile)
	{
	    cmsPipeline Lut;
	    cmsToneCurve[] Shapes = new cmsToneCurve[3], InvShapes = new cmsToneCurve[3];
	    cmsMAT3 Mat = new cmsMAT3(), Inv = new cmsMAT3();
	    int i, j;
	    cmsContext ContextID = cmsio0.cmsGetProfileContextID(hProfile);
	    
	    if (!ReadICCMatrixRGB2XYZ(Mat, hProfile))
	    {
	    	return null;
	    }
	    
	    if (!cmsmtrx._cmsMAT3inverse(Mat, Inv))
	    {
	    	return null;
	    }
	    
	    // XYZ PCS in encoded in 1.15 format, and the matrix input should come in 0..0xffff range, so
	    // we need to adjust the input by a << 1 to obtain a 1.16 fixed and then by a factor of 
	    // (0xffff/0x10000) to put data in 0..0xffff range. Total factor is (2.0*65535.0)/65536.0;
	    
	    for (i=0; i < 3; i++)
	    {
	        for (j=0; j < 3; j++)
	        {
	            Inv.v[i].n[j] *= OutpAdj;
	        }
	    }
	    
	    Shapes[0] = (cmsToneCurve)cmsio0.cmsReadTag(hProfile, lcms2.cmsSigRedTRCTag);        
	    Shapes[1] = (cmsToneCurve)cmsio0.cmsReadTag(hProfile, lcms2.cmsSigGreenTRCTag);
	    Shapes[2] = (cmsToneCurve)cmsio0.cmsReadTag(hProfile, lcms2.cmsSigBlueTRCTag);
	    
	    if (Shapes[0] == null || Shapes[1] == null || Shapes[2] == null)
	    {
	    	return null;
	    }
	    
	    InvShapes[0] = cmsgamma.cmsReverseToneCurve(Shapes[0]);
	    InvShapes[1] = cmsgamma.cmsReverseToneCurve(Shapes[1]);
	    InvShapes[2] = cmsgamma.cmsReverseToneCurve(Shapes[2]);
	    
	    if (InvShapes[0] == null || InvShapes[1] == null || InvShapes[2] == null)
	    {
	    	return null;
	    }
	    
	    Lut = cmslut.cmsPipelineAlloc(ContextID, 3, 3);
	    if (Lut != null)
	    {
	    	double[] vals = new double[3*3];
	    	cmsmtrx._cmsMAT3get(Inv, vals, 0);
	    	cmslut.cmsPipelineInsertStage(Lut, lcms2.cmsAT_END, cmslut.cmsStageAllocMatrix(ContextID, 3, 3, vals, null));
	    	cmslut.cmsPipelineInsertStage(Lut, lcms2.cmsAT_END, cmslut.cmsStageAllocToneCurves(ContextID, 3, InvShapes));
	    }
	    
	    cmsgamma.cmsFreeToneCurveTriple(InvShapes);
	    return Lut;
	}
	
	// Create an output MPE LUT from agiven profile. Version mismatches are handled here
	public static cmsPipeline _cmsReadOutputLUT(cmsHPROFILE hProfile, int Intent)
	{
	    int OriginalType;
	    int tag16    = PCS2Device16[Intent];
	    int tagFloat = PCS2DeviceFloat[Intent];
	    cmsContext ContextID = cmsio0.cmsGetProfileContextID(hProfile);
	    
	    if (cmsio0.cmsIsTag(hProfile, tagFloat)) // Float tag takes precedence
	    {
	        // Floating point LUT are always V4, so no adjustment is required
	        return cmslut.cmsPipelineDup((cmsPipeline)cmsio0.cmsReadTag(hProfile, tagFloat));
	    }
	    
	    // Revert to perceptual if no tag is found
	    if (!cmsio0.cmsIsTag(hProfile, tag16))
	    {
	        tag16 = PCS2Device16[0];
	    }
	    
	    if (cmsio0.cmsIsTag(hProfile, tag16)) // Is there any LUT-Based table?
	    {
	        // Check profile version and LUT type. Do the necessary adjustments if needed
	    	
	        // First read the tag
	        cmsPipeline Lut = (cmsPipeline)cmsio0.cmsReadTag(hProfile, tag16);
	        if (Lut == null)
	        {
	        	return null;
	        }
	        
	        // After reading it, we have info about the original type
	        OriginalType = cmsio0._cmsGetTagTrueType(hProfile, tag16);
	        
	        // The profile owns the Lut, so we need to copy it
	        Lut = cmslut.cmsPipelineDup(Lut);
	        
	        // We need to adjust data only for Lab and Lut16 type
	        if (OriginalType != lcms2.cmsSigLut16Type || cmsio0.cmsGetPCS(hProfile) != lcms2.cmsSigLabData)
	        {
	        	return Lut;
	        }
	        
	        // Add a matrix for conversion V4 to V2 Lab PCS
	        cmslut.cmsPipelineInsertStage(Lut, lcms2.cmsAT_BEGIN, cmslut._cmsStageAllocLabV4ToV2(ContextID));
	        return Lut;
	    }   
	    
	    // Lut not found, try to create a matrix-shaper
	    
	    // Check if this is a grayscale profile.
	    if (cmsio0.cmsGetColorSpace(hProfile) == lcms2.cmsSigGrayData)
	    {
	    	// if so, build appropiate conversion tables.
	    	// The tables are the PCS iluminant, scaled across GrayTRC
	    	return BuildGrayOutputPipeline(hProfile);
	    }
	    
	    // Not gray, create a normal matrix-shaper 
	    return BuildRGBOutputMatrixShaper(hProfile);
	}
	
	// ---------------------------------------------------------------------------------------------------------------
	
	// This one includes abstract profiles as well. Matrix-shaper cannot be obtained on that device class. The 
	// tag name here may default to AToB0
	public static cmsPipeline _cmsReadDevicelinkLUT(cmsHPROFILE hProfile, int Intent)
	{
	    cmsPipeline Lut;
	    int OriginalType;
	    int tag16    = Device2PCS16[Intent];
	    int tagFloat = Device2PCSFloat[Intent];
	    cmsContext ContextID = cmsio0.cmsGetProfileContextID(hProfile);
	    
	    if (cmsio0.cmsIsTag(hProfile, tagFloat)) // Float tag takes precedence
	    {
	        // Floating point LUT are always V4, no adjustment is required
	        return cmslut.cmsPipelineDup((cmsPipeline)cmsio0.cmsReadTag(hProfile, tagFloat));
	    }
	    
	    tagFloat = Device2PCSFloat[0];
	    if (cmsio0.cmsIsTag(hProfile, tagFloat))
	    {
	        return cmslut.cmsPipelineDup((cmsPipeline)cmsio0.cmsReadTag(hProfile, tagFloat));
	    }
	    
	    if (!cmsio0.cmsIsTag(hProfile, tag16)) // Is there any LUT-Based table?
	    {
	        tag16    = Device2PCS16[0];
	        if (!cmsio0.cmsIsTag(hProfile, tag16))
	        {
	        	return null;        
	        }
	    }
	    
	    // Check profile version and LUT type. Do the necessary adjustments if needed
	    
	    // Read the tag
	    Lut = (cmsPipeline)cmsio0.cmsReadTag(hProfile, tag16);
	    if (Lut == null)
	    {
	    	return null;
	    }
	    
	    // The profile owns the Lut, so we need to copy it
	    Lut = cmslut.cmsPipelineDup(Lut);
	    
	    // After reading it, we have info about the original type
	    OriginalType =  cmsio0._cmsGetTagTrueType(hProfile, tag16);
	    
	    // We need to adjust data for Lab16 on output
	    if (OriginalType != lcms2.cmsSigLut16Type)
	    {
	    	return Lut;
	    }
	    
	    // Here it is possible to get Lab on both sides
	    
	    if (cmsio0.cmsGetPCS(hProfile) == lcms2.cmsSigLabData)
	    {
	    	cmslut.cmsPipelineInsertStage(Lut, lcms2.cmsAT_BEGIN, cmslut._cmsStageAllocLabV4ToV2(ContextID));
	    }
	    
	    if (cmsio0.cmsGetColorSpace(hProfile) == lcms2.cmsSigLabData)
	    {
	    	cmslut.cmsPipelineInsertStage(Lut, lcms2.cmsAT_END, cmslut._cmsStageAllocLabV2ToV4(ContextID));
	    }
	    
	    return Lut;
	}
	
	// ---------------------------------------------------------------------------------------------------------------

	// Returns TRUE if the profile is implemented as matrix-shaper
	public static boolean cmsIsMatrixShaper(cmsHPROFILE hProfile)
	{
	    switch (cmsio0.cmsGetColorSpace(hProfile))
	    {
		    case lcms2.cmsSigGrayData:
		        return cmsio0.cmsIsTag(hProfile, lcms2.cmsSigGrayTRCTag);
		    case lcms2.cmsSigRgbData:
		        return (cmsio0.cmsIsTag(hProfile, lcms2.cmsSigRedColorantTag) &&
		        		cmsio0.cmsIsTag(hProfile, lcms2.cmsSigGreenColorantTag) &&
		        		cmsio0.cmsIsTag(hProfile, lcms2.cmsSigBlueColorantTag) &&
		        		cmsio0.cmsIsTag(hProfile, lcms2.cmsSigRedTRCTag) &&
		        		cmsio0.cmsIsTag(hProfile, lcms2.cmsSigGreenTRCTag) &&
		                cmsio0.cmsIsTag(hProfile, lcms2.cmsSigBlueTRCTag));
		    default:
		        return false;
	    }
	}
	
	// Returns TRUE if the intent is implemented as CLUT
	public static boolean cmsIsCLUT(cmsHPROFILE hProfile, int Intent, int UsedDirection)
	{    
	    final int[] TagTable;
	    
	    // For devicelinks, the supported intent is that one stated in the header
	    if (cmsio0.cmsGetDeviceClass(hProfile) == lcms2.cmsSigLinkClass)
	    {
	    	return (cmsio0.cmsGetHeaderRenderingIntent(hProfile) == Intent);
	    }
	    
	    switch (UsedDirection)
	    {
	       case lcms2.LCMS_USED_AS_INPUT:
	    	   TagTable = Device2PCS16;
	    	   break;
	       case lcms2.LCMS_USED_AS_OUTPUT:
	    	   TagTable = PCS2Device16;
	    	   break; 
	       // For proofing, we need rel. colorimetric in output. Let's do some recursion
	       case lcms2.LCMS_USED_AS_PROOF: 
	           return cmsIsIntentSupported(hProfile, Intent, lcms2.LCMS_USED_AS_INPUT) &&
	                  cmsIsIntentSupported(hProfile, lcms2.INTENT_RELATIVE_COLORIMETRIC, lcms2.LCMS_USED_AS_OUTPUT);
	       default:
	           cmserr.cmsSignalError(cmsio0.cmsGetProfileContextID(hProfile), lcms2.cmsERROR_RANGE, Utility.LCMS_Resources.getString(LCMSResource.CMSIO1_UNEXPECTED_DIRECTION), new Object[]{new Integer(UsedDirection)});
	           return false;
	    }
	    
	    return cmsio0.cmsIsTag(hProfile, TagTable[Intent]);
	}
	
	// Return info about supported intents
	public static boolean cmsIsIntentSupported(cmsHPROFILE hProfile, int Intent, int UsedDirection)
	{
	    if (cmsIsCLUT(hProfile, Intent, UsedDirection))
	    {
	    	return true;
	    }
	    
	    // Is there any matrix-shaper? If so, the intent is supported. This is a bit odd, since V2 matrix shaper
	    // does not fully support relative colorimetric because they cannot deal with non-zero black points, but
	    // many profiles claims that, and this is certainly not true for V4 profiles. Lets answer "yes" no matter
	    // the accuracy would be less than optimal in rel.col and v2 case.
	    
	    return cmsIsMatrixShaper(hProfile);
	}
	
	// ---------------------------------------------------------------------------------------------------------------
	
	// Read both, profile sequence description and profile sequence id if present. Then combine both to
	// create qa unique structure holding both. Shame on ICC to store things in such complicated way.
	
	public static cmsSEQ _cmsReadProfileSequence(cmsHPROFILE hProfile)
	{
	    cmsSEQ ProfileSeq;
	    cmsSEQ ProfileId;
	    cmsSEQ NewSeq;
	    int i;
	    
	    // Take profile sequence description first
	    ProfileSeq = (cmsSEQ)cmsio0.cmsReadTag(hProfile, lcms2.cmsSigProfileSequenceDescTag);
	    
	    // Take profile sequence ID
	    ProfileId  = (cmsSEQ)cmsio0.cmsReadTag(hProfile, lcms2.cmsSigProfileSequenceIdTag);

	    if (ProfileSeq == null && ProfileId == null)
	    {
	    	return null;
	    }
	    
	    if (ProfileSeq == null)
	    {
	    	return cmsnamed.cmsDupProfileSequenceDescription(ProfileId);
	    }
	    if (ProfileId  == null)
	    {
	    	return cmsnamed.cmsDupProfileSequenceDescription(ProfileSeq);
	    }
	    
	    // We have to mix both together. For that they must agree 
	    if (ProfileSeq.n != ProfileId.n)
	    {
	    	return cmsnamed.cmsDupProfileSequenceDescription(ProfileSeq);
	    }
	    
	    NewSeq = cmsnamed.cmsDupProfileSequenceDescription(ProfileSeq);
	    
	    // Ok, proceed to the mixing
	    for (i=0; i < ProfileSeq.n; i++)
	    {
	    	System.arraycopy(ProfileId.seq[i].ProfileID.data, 0, NewSeq.seq[i].ProfileID.data, 0, cmsProfileID.SIZE);
	        NewSeq.seq[i].Description = cmsnamed.cmsMLUdup(ProfileId.seq[i].Description);
	    }
	    
	    return NewSeq;
	}
	
	// Dump the contents of profile sequence in both tags (if v4 available)
	public static boolean _cmsWriteProfileSequence(cmsHPROFILE hProfile, final cmsSEQ seq)
	{
	    if (!cmsio0.cmsWriteTag(hProfile, lcms2.cmsSigProfileSequenceDescTag, seq))
	    {
	    	return false;
	    }
	    
	    if (cmsio0.cmsGetProfileVersion(hProfile) >= 4.0)
	    {
	    	if (!cmsio0.cmsWriteTag(hProfile, lcms2.cmsSigProfileSequenceIdTag, seq))
	    	{
	    		return false;
	    	}
	    }
	    
	    return true;
	}
	
	// Auxiliar, read and duplicate a MLU if found.
	private static cmsMLU GetMLUFromProfile(cmsHPROFILE h, int sig)
	{
	    cmsMLU mlu = (cmsMLU)cmsio0.cmsReadTag(h, sig);
	    if (mlu == null)
	    {
	    	return null;
	    }
	    
	    return cmsnamed.cmsMLUdup(mlu);
	}
	
	// Create a sequence description out of an array of profiles
	public static cmsSEQ _cmsCompileProfileSequence(cmsContext ContextID, int nProfiles, cmsHPROFILE[] hProfiles)
	{
	    int i;
	    cmsSEQ seq = cmsnamed.cmsAllocProfileSequenceDescription(ContextID, nProfiles);
	    
	    if (seq == null)
	    {
	    	return null;
	    }
	    
	    for (i=0; i < nProfiles; i++)
	    {
	        cmsPSEQDESC ps = seq.seq[i];
	        cmsHPROFILE h = hProfiles[i];
	        Integer techpt;
	        
	        long[] tmp = new long[]{ps.attributes};
	        cmsio0.cmsGetHeaderAttributes(h, tmp);
	        ps.attributes = tmp[0];
	        cmsio0.cmsGetHeaderProfileID(h, ps.ProfileID);       
	        ps.deviceMfg   = cmsio0.cmsGetHeaderManufacturer(h);
	        ps.deviceModel = cmsio0.cmsGetHeaderModel(h);
	        
	        techpt = (Integer)cmsio0.cmsReadTag(h, lcms2.cmsSigTechnologyTag);
	        if (techpt == null)
	        {
	        	ps.technology =  0;
	        }
	        else
	        {
	        	ps.technology = techpt.intValue();
	        }
	        
	        ps.Manufacturer = GetMLUFromProfile(h, lcms2.cmsSigDeviceMfgDescTag);
	        ps.Model        = GetMLUFromProfile(h, lcms2.cmsSigDeviceModelDescTag);
	        ps.Description  = GetMLUFromProfile(h, lcms2.cmsSigProfileDescriptionTag);
	    }
	    
	    return seq;
	}
	
	// -------------------------------------------------------------------------------------------------------------------
	
	private static cmsMLU GetInfo(cmsHPROFILE hProfile, int Info)
	{
	    int sig;
	    
	    switch (Info)
	    {
		    case lcms2.cmsInfoDescription:
		        sig = lcms2.cmsSigProfileDescriptionTag; 
		        break;
		    case lcms2.cmsInfoManufacturer:
		        sig = lcms2.cmsSigDeviceMfgDescTag;
		        break;
		    case lcms2.cmsInfoModel:
		        sig = lcms2.cmsSigDeviceModelDescTag;
		        break;
		    case lcms2.cmsInfoCopyright:
		        sig = lcms2.cmsSigCopyrightTag;
		        break;
		    default:
		    	return null;
	    }
	    
	    return (cmsMLU)cmsio0.cmsReadTag(hProfile, sig);
	}
	
	public static int cmsGetProfileInfo(cmsHPROFILE hProfile, int Info, final String LanguageCode, final String CountryCode, StringBuffer Buffer, int BufferSize)
	{
		final cmsMLU mlu = GetInfo(hProfile, Info);
		if (mlu == null)
		{
			return 0;
		}
		
		return cmsnamed.cmsMLUgetWide(mlu, LanguageCode, CountryCode, Buffer, BufferSize);
	}
	
	public static int cmsGetProfileInfoASCII(cmsHPROFILE hProfile, int Info, final String LanguageCode, final String CountryCode, StringBuffer Buffer, int BufferSize)
	{
		final cmsMLU mlu = GetInfo(hProfile, Info);
		if (mlu == null)
		{
			return 0;
		}
		
		return cmsnamed.cmsMLUgetASCII(mlu, LanguageCode, CountryCode, Buffer, BufferSize);
	}
}
