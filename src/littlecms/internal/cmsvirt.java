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
import littlecms.internal.lcms2.cmsCIELCh;
import littlecms.internal.lcms2.cmsCIELab;
import littlecms.internal.lcms2.cmsCIEXYZ;
import littlecms.internal.lcms2.cmsCIEXYZTRIPLE;
import littlecms.internal.lcms2.cmsCIExyY;
import littlecms.internal.lcms2.cmsCIExyYTRIPLE;
import littlecms.internal.lcms2.cmsContext;
import littlecms.internal.lcms2.cmsHPROFILE;
import littlecms.internal.lcms2.cmsHTRANSFORM;
import littlecms.internal.lcms2.cmsMLU;
import littlecms.internal.lcms2.cmsNAMEDCOLORLIST;
import littlecms.internal.lcms2.cmsPipeline;
import littlecms.internal.lcms2.cmsSAMPLER16;
import littlecms.internal.lcms2.cmsSEQ;
import littlecms.internal.lcms2.cmsStage;
import littlecms.internal.lcms2.cmsToneCurve;
import littlecms.internal.lcms2_internal._cmsTRANSFORM;
import littlecms.internal.lcms2_plugin.cmsMAT3;

/**
 * Virtual (built-in) profiles
 */
//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmsvirt
{
	private static boolean SetTextTags(cmsHPROFILE hProfile, final String Description)
	{
	    cmsMLU DescriptionMLU, CopyrightMLU;
	    boolean rc = false;
	    cmsContext ContextID = cmsio0.cmsGetProfileContextID(hProfile);
	    
	    DescriptionMLU  = cmsnamed.cmsMLUalloc(ContextID, 1);
	    CopyrightMLU    = cmsnamed.cmsMLUalloc(ContextID, 1);
	    
	    if (DescriptionMLU == null || CopyrightMLU == null)
	    {
	    	if (DescriptionMLU != null)
		    {
	    		cmsnamed.cmsMLUfree(DescriptionMLU);
		    }
		    if (CopyrightMLU != null)
		    {
		    	cmsnamed.cmsMLUfree(CopyrightMLU);
		    }
	    	return rc;
	    }
	    
	    if (!cmsnamed.cmsMLUsetWide(DescriptionMLU, "en", "US", Description))
	    {
	    	if (DescriptionMLU != null)
		    {
	    		cmsnamed.cmsMLUfree(DescriptionMLU);
		    }
		    if (CopyrightMLU != null)
		    {
		    	cmsnamed.cmsMLUfree(CopyrightMLU);
		    }
	    	return rc;
	    }
	    if (!cmsnamed.cmsMLUsetWide(CopyrightMLU, "en", "US", "No copyright, use freely"))
	    {
	    	if (DescriptionMLU != null)
		    {
	    		cmsnamed.cmsMLUfree(DescriptionMLU);
		    }
		    if (CopyrightMLU != null)
		    {
		    	cmsnamed.cmsMLUfree(CopyrightMLU);
		    }
	    	return rc;
	    }
	    
	    if (!cmsio0.cmsWriteTag(hProfile, lcms2.cmsSigProfileDescriptionTag, DescriptionMLU))
	    {
	    	if (DescriptionMLU != null)
		    {
	    		cmsnamed.cmsMLUfree(DescriptionMLU);
		    }
		    if (CopyrightMLU != null)
		    {
		    	cmsnamed.cmsMLUfree(CopyrightMLU);
		    }
	    	return rc;
	    }
	    if (!cmsio0.cmsWriteTag(hProfile, lcms2.cmsSigCopyrightTag, CopyrightMLU))
	    {
	    	if (DescriptionMLU != null)
		    {
	    		cmsnamed.cmsMLUfree(DescriptionMLU);
		    }
		    if (CopyrightMLU != null)
		    {
		    	cmsnamed.cmsMLUfree(CopyrightMLU);
		    }
	    	return rc;
	    }
	    
	    rc = true;
	    
	    if (DescriptionMLU != null)
	    {
	    	cmsnamed.cmsMLUfree(DescriptionMLU);
	    }
	    if (CopyrightMLU != null)
	    {
	    	cmsnamed.cmsMLUfree(CopyrightMLU);
	    }
	    return rc;
	}
	
	private static boolean SetSeqDescTag(cmsHPROFILE hProfile, final String Model)
	{
	    boolean rc = false;
	    cmsContext ContextID = cmsio0.cmsGetProfileContextID(hProfile);
	    cmsSEQ Seq = cmsnamed.cmsAllocProfileSequenceDescription(ContextID, 1);
	    
	    if (Seq == null)
	    {
	    	return false;
	    }
	    
	    Seq.seq[0].deviceMfg = 0;
	    Seq.seq[0].deviceModel = 0;
	    
	    Seq.seq[0].attributes = 0;
	    
	    Seq.seq[0].technology = 0;
	    
	    cmsnamed.cmsMLUsetASCII(Seq.seq[0].Manufacturer, lcms2.cmsNoLanguage, lcms2.cmsNoCountry, "Little CMS");
	    cmsnamed.cmsMLUsetASCII(Seq.seq[0].Model, lcms2.cmsNoLanguage, lcms2.cmsNoCountry, Model);
	    
	    if (!cmsio1._cmsWriteProfileSequence(hProfile, Seq))
	    {
	    	if (Seq != null)
		    {
		    	cmsnamed.cmsFreeProfileSequenceDescription(Seq);
		    }
		    
		    return rc;
	    }
	    
	    rc = true;
	    
	    if (Seq != null)
	    {
	    	cmsnamed.cmsFreeProfileSequenceDescription(Seq);
	    }
	    
	    return rc;
	}
	
	// This function creates a profile based on White point, primaries and
	// transfer functions.
	public static cmsHPROFILE cmsCreateRGBProfileTHR(cmsContext ContextID, final cmsCIExyY WhitePoint, final cmsCIExyYTRIPLE Primaries, 
			final cmsToneCurve[] TransferFunction)
	{
	    cmsHPROFILE hICC;   
	    cmsMAT3 MColorants;
	    cmsCIEXYZTRIPLE Colorants;
	    cmsCIExyY MaxWhite;
	    cmsMAT3 CHAD;
	    cmsCIEXYZ WhitePointXYZ;
	    
	    hICC = cmsio0.cmsCreateProfilePlaceholder(ContextID);
	    if (hICC == null) // can't allocate
	    {
	    	return null;
	    }
	    
	    cmsio0.cmsSetProfileVersion(hICC, 4.2);
	    
	    cmsio0.cmsSetDeviceClass(hICC, lcms2.cmsSigDisplayClass);
	    cmsio0.cmsSetColorSpace(hICC,  lcms2.cmsSigRgbData);
	    cmsio0.cmsSetPCS(hICC,         lcms2.cmsSigXYZData);
	    
	    cmsio0.cmsSetHeaderRenderingIntent(hICC, lcms2.INTENT_PERCEPTUAL);
	    
	    // Implement profile using following tags:
	    //
	    //  1 cmsSigProfileDescriptionTag
	    //  2 cmsSigMediaWhitePointTag
	    //  3 cmsSigRedColorantTag
	    //  4 cmsSigGreenColorantTag
	    //  5 cmsSigBlueColorantTag
	    //  6 cmsSigRedTRCTag
	    //  7 cmsSigGreenTRCTag
	    //  8 cmsSigBlueTRCTag
	    //  9 Chromatic adaptation Tag
	    // This conforms a standard RGB DisplayProfile as says ICC, and then I add (As per addendum II)
	    // 10 cmsSigChromaticityTag
	    
	    if (!SetTextTags(hICC, "RGB built-in"))
	    {
	    	if (hICC != null)
		    {
		    	cmsio0.cmsCloseProfile(hICC);
		    }
		    return null;
	    }
	    
	    if (WhitePoint != null)
	    {
	        if (!cmsio0.cmsWriteTag(hICC, lcms2.cmsSigMediaWhitePointTag, lcms2.cmsD50_XYZ))
		    {
		    	if (hICC != null)
			    {
			    	cmsio0.cmsCloseProfile(hICC);
			    }
			    return null;
		    }
	        
	        WhitePointXYZ = new cmsCIEXYZ();
	        cmspcs.cmsxyY2XYZ(WhitePointXYZ, WhitePoint);
	        CHAD = new cmsMAT3();
	        cmswtpnt._cmsAdaptationMatrix(CHAD, null, WhitePointXYZ, lcms2.cmsD50_XYZ);     
	        
	        // This is a V4 tag, but many CMM does read and understand it no matter which version       
	        if (!cmsio0.cmsWriteTag(hICC, lcms2.cmsSigChromaticAdaptationTag, CHAD))
		    {
		    	if (hICC != null)
			    {
			    	cmsio0.cmsCloseProfile(hICC);
			    }
			    return null;
		    }
	    }
	    
	    if (WhitePoint != null && Primaries != null)
	    {
	    	MaxWhite = new cmsCIExyY();
	        MaxWhite.x =  WhitePoint.x;
	        MaxWhite.y =  WhitePoint.y;
	        MaxWhite.Y =  1.0;
	        
	        MColorants = new cmsMAT3();
	        if (!cmswtpnt._cmsBuildRGB2XYZtransferMatrix(MColorants, MaxWhite, Primaries))
		    {
		    	if (hICC != null)
			    {
			    	cmsio0.cmsCloseProfile(hICC);
			    }
			    return null;
		    }
	        
	        Colorants = new cmsCIEXYZTRIPLE();
	        Colorants.Red.X   = MColorants.v[0].n[0];
	        Colorants.Red.Y   = MColorants.v[1].n[0];
	        Colorants.Red.Z   = MColorants.v[2].n[0];
	        
	        Colorants.Green.X = MColorants.v[0].n[1];
	        Colorants.Green.Y = MColorants.v[1].n[1];
	        Colorants.Green.Z = MColorants.v[2].n[1];
	        
	        Colorants.Blue.X  = MColorants.v[0].n[2];
	        Colorants.Blue.Y  = MColorants.v[1].n[2];
	        Colorants.Blue.Z  = MColorants.v[2].n[2];
	        
	        if (!cmsio0.cmsWriteTag(hICC, lcms2.cmsSigRedColorantTag, Colorants.Red))
		    {
		    	if (hICC != null)
			    {
			    	cmsio0.cmsCloseProfile(hICC);
			    }
			    return null;
		    }
	        if (!cmsio0.cmsWriteTag(hICC, lcms2.cmsSigBlueColorantTag, Colorants.Blue))
		    {
		    	if (hICC != null)
			    {
			    	cmsio0.cmsCloseProfile(hICC);
			    }
			    return null;
		    }
	        if (!cmsio0.cmsWriteTag(hICC, lcms2.cmsSigGreenColorantTag, Colorants.Green))
		    {
		    	if (hICC != null)
			    {
			    	cmsio0.cmsCloseProfile(hICC);
			    }
			    return null;
		    }
	    }
	    
	    if (TransferFunction != null)
	    {
	        if (!cmsio0.cmsWriteTag(hICC, lcms2.cmsSigRedTRCTag, TransferFunction[0]))
		    {
		    	if (hICC != null)
			    {
			    	cmsio0.cmsCloseProfile(hICC);
			    }
			    return null;
		    }
	        if (!cmsio0.cmsWriteTag(hICC, lcms2.cmsSigGreenTRCTag, TransferFunction[1]))
		    {
		    	if (hICC != null)
			    {
			    	cmsio0.cmsCloseProfile(hICC);
			    }
			    return null;
		    }
	        if (!cmsio0.cmsWriteTag(hICC, lcms2.cmsSigBlueTRCTag, TransferFunction[2]))
		    {
		    	if (hICC != null)
			    {
			    	cmsio0.cmsCloseProfile(hICC);
			    }
			    return null;
		    }
	    }
	    
	    if (Primaries != null)
	    {
	        if (!cmsio0.cmsWriteTag(hICC, lcms2.cmsSigChromaticityTag, Primaries))
		    {
		    	if (hICC != null)
			    {
			    	cmsio0.cmsCloseProfile(hICC);
			    }
			    return null;
		    }
	    }
	    
	    return hICC;
	}
	
	public static cmsHPROFILE cmsCreateRGBProfile(final cmsCIExyY WhitePoint, final cmsCIExyYTRIPLE Primaries, final cmsToneCurve[] TransferFunction)
	{
		return cmsCreateRGBProfileTHR(null, WhitePoint, Primaries, TransferFunction);
	}
	
	// This function creates a profile based on White point and transfer function.
	public static cmsHPROFILE cmsCreateGrayProfileTHR(cmsContext ContextID, final cmsCIExyY WhitePoint, final cmsToneCurve TransferFunction)
	{
	    cmsHPROFILE hICC;
	    cmsCIEXYZ tmp;              
	    
	    hICC = cmsio0.cmsCreateProfilePlaceholder(ContextID);
	    if (hICC == null) // can't allocate
	    {
	    	return null;
	    }
	    
	    cmsio0.cmsSetProfileVersion(hICC, 4.2);
	    
	    cmsio0.cmsSetDeviceClass(hICC, lcms2.cmsSigDisplayClass);
	    cmsio0.cmsSetColorSpace(hICC,  lcms2.cmsSigGrayData);
	    cmsio0.cmsSetPCS(hICC,         lcms2.cmsSigXYZData);
	    cmsio0.cmsSetHeaderRenderingIntent(hICC, lcms2.INTENT_PERCEPTUAL);
	    
	    // Implement profile using following tags:
	    //
	    //  1 cmsSigProfileDescriptionTag
	    //  2 cmsSigMediaWhitePointTag
	    //  3 cmsSigGrayTRCTag
	    
	    // This conforms a standard Gray DisplayProfile 
	    
	    // Fill-in the tags
	    
	    if (!SetTextTags(hICC, "gray built-in"))
	    {
	    	if (hICC != null)
		    {
		    	cmsio0.cmsCloseProfile(hICC);
		    }
		    return null;
	    }
	    
	    if (WhitePoint != null)
	    {
	    	tmp = new cmsCIEXYZ();
	        cmspcs.cmsxyY2XYZ(tmp, WhitePoint);
	        if (!cmsio0.cmsWriteTag(hICC, lcms2.cmsSigMediaWhitePointTag, tmp))
		    {
		    	if (hICC != null)
			    {
			    	cmsio0.cmsCloseProfile(hICC);
			    }
			    return null;
		    }
	    }
	    
	    if (TransferFunction != null)
	    {
	        if (!cmsio0.cmsWriteTag(hICC, lcms2.cmsSigGrayTRCTag, TransferFunction))
		    {
		    	if (hICC != null)
			    {
			    	cmsio0.cmsCloseProfile(hICC);
			    }
			    return null;
		    }
	    }
	    
	    return hICC;
	}
	
	public static cmsHPROFILE cmsCreateGrayProfile(final cmsCIExyY WhitePoint, final cmsToneCurve TransferFunction)
	{
	    return cmsCreateGrayProfileTHR(null, WhitePoint, TransferFunction);
	}
	
	// This is a devicelink operating in the target colorspace with as many transfer functions as components
	
	public static cmsHPROFILE cmsCreateLinearizationDeviceLinkTHR(cmsContext ContextID, int ColorSpace, final cmsToneCurve[] TransferFunctions)
	{
	    cmsHPROFILE hICC;
	    cmsPipeline Pipeline;
	    cmsStage Lin;
	    int nChannels;
	    
	    hICC = cmsio0.cmsCreateProfilePlaceholder(ContextID);
	    if (hICC == null)
	    {
	    	return null;
	    }
	    
	    cmsio0.cmsSetProfileVersion(hICC, 4.2);
	    
	    cmsio0.cmsSetDeviceClass(hICC, lcms2.cmsSigLinkClass);
	    cmsio0.cmsSetColorSpace(hICC,  ColorSpace);
	    cmsio0.cmsSetPCS(hICC,         ColorSpace);
	    
	    cmsio0.cmsSetHeaderRenderingIntent(hICC, lcms2.INTENT_PERCEPTUAL); 
	    
	    // Set up channels
	    nChannels = cmspcs.cmsChannelsOf(ColorSpace);
	    
	    // Creates a Pipeline with prelinearization step only
	    Pipeline = cmslut.cmsPipelineAlloc(ContextID, nChannels, nChannels);
	    if (Pipeline == null)
	    {
	    	if (hICC != null)
		    {
		    	cmsio0.cmsCloseProfile(hICC);
		    }
		    
		    return null;
	    }
	    
	    // Copy tables to Pipeline
	    Lin = cmslut.cmsStageAllocToneCurves(ContextID, nChannels, TransferFunctions);
	    if (Lin == null)
	    {
	    	if (hICC != null)
		    {
		    	cmsio0.cmsCloseProfile(hICC);
		    }
		    
		    return null;
	    }
	    
	    cmslut.cmsPipelineInsertStage(Pipeline, lcms2.cmsAT_BEGIN, Lin);
	    
	    // Create tags       
	    if (!SetTextTags(hICC, "Linearization built-in"))
	    {
	    	if (hICC != null)
		    {
		    	cmsio0.cmsCloseProfile(hICC);
		    }
		    
		    return null;
	    }
	    if (!cmsio0.cmsWriteTag(hICC, lcms2.cmsSigAToB0Tag, Pipeline))
	    {
	    	if (hICC != null)
		    {
		    	cmsio0.cmsCloseProfile(hICC);
		    }
		    
		    return null;
	    }
	    if (!SetSeqDescTag(hICC, "Linearization built-in"))
	    {
	    	if (hICC != null)
		    {
		    	cmsio0.cmsCloseProfile(hICC);
		    }
		    
		    return null;
	    }
	    
	    // Pipeline is already on virtual profile
	    cmslut.cmsPipelineFree(Pipeline);
	    
	    // Ok, done
	    return hICC;
	}
	
	public static cmsHPROFILE cmsCreateLinearizationDeviceLink(int ColorSpace, final cmsToneCurve[] TransferFunctions)
	{
	    return cmsCreateLinearizationDeviceLinkTHR(null, ColorSpace, TransferFunctions);
	}
	
	// Ink-limiting algorithm
	//
	//  Sum = C + M + Y + K 
	//  If Sum > InkLimit 
	//	        Ratio= 1 - (Sum - InkLimit) / (C + M + Y)
	//	        if Ratio <0 
	//	              Ratio=0
	//	        endif     
	//	     Else 
	//	         Ratio=1
	//	     endif
	//
	//	     C = Ratio * C
	//	     M = Ratio * M
	//	     Y = Ratio * Y
	//	     K: Does not change
	
	private static final cmsSAMPLER16 InkLimitingSampler = new cmsSAMPLER16()
	{
		public int run(short[] In, short[] Out, Object Cargo)
		{
			double InkLimit = ((double[])Cargo)[0];
		    double SumCMY, SumCMYK, Ratio;
		    
		    InkLimit = (InkLimit * 655.35);
		    
		    SumCMY   = In[0]  + In[1] + In[2];
		    SumCMYK  = SumCMY + In[3];      
		    
		    if (SumCMYK > InkLimit)
		    {
		        Ratio = 1 - ((SumCMYK - InkLimit) / SumCMY);
		        if (Ratio < 0)
		        {
		        	Ratio = 0;
		        }
		    }
		    else
		    {
		    	Ratio = 1;
		    }
		    
		    Out[0] = lcms2_internal._cmsQuickSaturateWord(In[0] * Ratio);	// C
		    Out[1] = lcms2_internal._cmsQuickSaturateWord(In[1] * Ratio);	// M
		    Out[2] = lcms2_internal._cmsQuickSaturateWord(In[2] * Ratio);	// Y
		    
		    Out[3] = In[3];													// K (untouched)
		    
		    return 1;
		}
	};
	
	// This is a devicelink operating in CMYK for ink-limiting
	
	public static cmsHPROFILE cmsCreateInkLimitingDeviceLinkTHR(cmsContext ContextID, int ColorSpace, double Limit)
	{
	    cmsHPROFILE hICC;
	    cmsPipeline LUT;
	    cmsStage CLUT;
	    int nChannels;
	    
	    if (ColorSpace != lcms2.cmsSigCmykData)
	    {
	        cmserr.cmsSignalError(ContextID, lcms2.cmsERROR_COLORSPACE_CHECK, Utility.LCMS_Resources.getString(LCMSResource.CMSVIRT_INKLIMIT_ONLY_SUPPORT_CMYK), null);
	        return null;
	    }
	    
	    if (Limit < 0.0 || Limit > 400)
	    {
	        cmserr.cmsSignalError(ContextID, lcms2.cmsERROR_RANGE, Utility.LCMS_Resources.getString(LCMSResource.CMSVIRT_INKLIMIT_OUTOFRANGE), null);
	        if (Limit < 0)
	        {
	        	Limit = 0;
	        }
	        if (Limit > 400)
	        {
	        	Limit = 400;
	        }
	    }
	    
	    hICC = cmsio0.cmsCreateProfilePlaceholder(ContextID);
	    if (hICC == null) // can't allocate
	    {
	    	return null;
	    }
	    
	    cmsio0.cmsSetProfileVersion(hICC, 4.2);
	    
	    cmsio0.cmsSetDeviceClass(hICC, lcms2.cmsSigLinkClass);
	    cmsio0.cmsSetColorSpace(hICC,  ColorSpace);
	    cmsio0.cmsSetPCS(hICC,         ColorSpace);
	    
	    cmsio0.cmsSetHeaderRenderingIntent(hICC, lcms2.INTENT_PERCEPTUAL);
	    
	    // Creates a Pipeline with 3D grid only
	    LUT = cmslut.cmsPipelineAlloc(ContextID, 4, 4);
	    if (LUT == null)
	    {
		    if (hICC != null)
		    {
		    	cmsio0.cmsCloseProfile(hICC);
		    }
		    
		    return null;
	    }
	    
	    nChannels = cmspcs.cmsChannelsOf(ColorSpace);
	    
	    CLUT = cmslut.cmsStageAllocCLut16bit(ContextID, 17, nChannels, nChannels, null);
	    if (CLUT == null)
	    {
	    	if (LUT != null)
		    {
		    	cmslut.cmsPipelineFree(LUT);
		    }
		    
		    if (hICC != null)
		    {
		    	cmsio0.cmsCloseProfile(hICC);
		    }
		    
		    return null;
	    }

	    if (!cmslut.cmsStageSampleCLut16bit(CLUT, InkLimitingSampler, new double[]{Limit}, 0))
	    {
	    	if (LUT != null)
		    {
		    	cmslut.cmsPipelineFree(LUT);
		    }
		    
		    if (hICC != null)
		    {
		    	cmsio0.cmsCloseProfile(hICC);
		    }
		    
		    return null;
	    }
	    
	    cmslut.cmsPipelineInsertStage(LUT, lcms2.cmsAT_BEGIN, cmslut._cmsStageAllocIdentityCurves(ContextID, nChannels));  
	    cmslut.cmsPipelineInsertStage(LUT, lcms2.cmsAT_END, CLUT);
	    cmslut.cmsPipelineInsertStage(LUT, lcms2.cmsAT_END, cmslut._cmsStageAllocIdentityCurves(ContextID, nChannels));
	    
	    // Create tags
	    if (!SetTextTags(hICC, "ink-limiting built-in"))
	    {
	    	if (LUT != null)
		    {
		    	cmslut.cmsPipelineFree(LUT);
		    }
		    
		    if (hICC != null)
		    {
		    	cmsio0.cmsCloseProfile(hICC);
		    }
		    
		    return null;
	    }
	    
	    if (!cmsio0.cmsWriteTag(hICC, lcms2.cmsSigAToB0Tag, LUT))
	    {
	    	if (LUT != null)
		    {
		    	cmslut.cmsPipelineFree(LUT);
		    }
		    
		    if (hICC != null)
		    {
		    	cmsio0.cmsCloseProfile(hICC);
		    }
		    
		    return null;
	    }
	    if (!SetSeqDescTag(hICC, "ink-limiting built-in"))
	    {
	    	if (LUT != null)
		    {
		    	cmslut.cmsPipelineFree(LUT);
		    }
		    
		    if (hICC != null)
		    {
		    	cmsio0.cmsCloseProfile(hICC);
		    }
		    
		    return null;
	    }
	    
	    // cmsPipeline is already on virtual profile
	    cmslut.cmsPipelineFree(LUT);
	    
	    // Ok, done
	    return hICC;
	}
	
	public static cmsHPROFILE cmsCreateInkLimitingDeviceLink(int ColorSpace, double Limit)
	{
	    return cmsCreateInkLimitingDeviceLinkTHR(null, ColorSpace, Limit);
	}
	
	// Creates a fake Lab identity.
	public static cmsHPROFILE cmsCreateLab2ProfileTHR(cmsContext ContextID, final cmsCIExyY WhitePoint)
	{
	    cmsHPROFILE hProfile;        
	    cmsPipeline LUT = null;
	    
	    hProfile = cmsCreateRGBProfileTHR(ContextID, WhitePoint == null ? lcms2.cmsD50_xyY : WhitePoint, null, null);
	    if (hProfile == null)
	    {
	    	return null;
	    }
	    
	    cmsio0.cmsSetProfileVersion(hProfile, 2.1);
	    
	    cmsio0.cmsSetDeviceClass(hProfile, lcms2.cmsSigAbstractClass);
	    cmsio0.cmsSetColorSpace(hProfile,  lcms2.cmsSigLabData);
	    cmsio0.cmsSetPCS(hProfile,         lcms2.cmsSigLabData);
	    
	    if (!SetTextTags(hProfile, "Lab identity built-in"))
	    {
		    if (hProfile != null)
		    {
		    	cmsio0.cmsCloseProfile(hProfile);
		    }
		    
		    return null;
	    }
	    
	    // An identity LUT is all we need
	    LUT = cmslut.cmsPipelineAlloc(ContextID, 3, 3);
	    if (LUT == null)
	    {
		    if (hProfile != null)
		    {
		    	cmsio0.cmsCloseProfile(hProfile);
		    }
		    
		    return null;
	    }

	    cmslut.cmsPipelineInsertStage(LUT, lcms2.cmsAT_BEGIN, cmslut._cmsStageAllocIdentityCLut(ContextID, 3));

	    if (!cmsio0.cmsWriteTag(hProfile, lcms2.cmsSigAToB0Tag, LUT))
	    {
	    	if (LUT != null)
		    {
		    	cmslut.cmsPipelineFree(LUT);
		    }
		    
		    if (hProfile != null)
		    {
		    	cmsio0.cmsCloseProfile(hProfile);
		    }
		    
		    return null;
	    }
	    cmslut.cmsPipelineFree(LUT);

	    return hProfile;
	}
	
	public static cmsHPROFILE cmsCreateLab2Profile(final cmsCIExyY WhitePoint)
	{
	    return cmsCreateLab2ProfileTHR(null, WhitePoint);
	}
	
	// Creates a fake Lab V4 identity.
	public static cmsHPROFILE cmsCreateLab4ProfileTHR(cmsContext ContextID, final cmsCIExyY WhitePoint)
	{
	    cmsHPROFILE hProfile;
	    cmsPipeline LUT = null;
	    
	    hProfile = cmsCreateRGBProfileTHR(ContextID, WhitePoint == null ? lcms2.cmsD50_xyY : WhitePoint, null, null);
	    if (hProfile == null)
	    {
	    	return null;
	    }
	    
	    cmsio0.cmsSetProfileVersion(hProfile, 4.2);
	    
	    cmsio0.cmsSetDeviceClass(hProfile, lcms2.cmsSigAbstractClass);
	    cmsio0.cmsSetColorSpace(hProfile,  lcms2.cmsSigLabData);
	    cmsio0.cmsSetPCS(hProfile,         lcms2.cmsSigLabData);
	    
	    if (!SetTextTags(hProfile, "Lab identity built-in"))
	    {
	    	if (LUT != null)
		    {
		    	cmslut.cmsPipelineFree(LUT);
		    }
		    
		    if (hProfile != null)
		    {
		    	cmsio0.cmsCloseProfile(hProfile);
		    }
		    
		    return null;
	    }
	    
	    // An empty LUTs is all we need
	    LUT = cmslut.cmsPipelineAlloc(ContextID, 3, 3);
	    if (LUT == null)
	    {
		    if (hProfile != null)
		    {
		    	cmsio0.cmsCloseProfile(hProfile);
		    }
		    
		    return null;
	    }
	    
	    cmslut.cmsPipelineInsertStage(LUT, lcms2.cmsAT_BEGIN, cmslut._cmsStageAllocIdentityCurves(ContextID, 3));
	    
	    if (!cmsio0.cmsWriteTag(hProfile, lcms2.cmsSigAToB0Tag, LUT))
	    {
	    	if (LUT != null)
		    {
		    	cmslut.cmsPipelineFree(LUT);
		    }
		    
		    if (hProfile != null)
		    {
		    	cmsio0.cmsCloseProfile(hProfile);
		    }
		    
		    return null;
	    }
	    cmslut.cmsPipelineFree(LUT);
	    
	    return hProfile;
	}
	
	public static cmsHPROFILE cmsCreateLab4Profile(final cmsCIExyY WhitePoint)
	{
	    return cmsCreateLab4ProfileTHR(null, WhitePoint);
	}
	
	// Creates a fake XYZ identity
	public static cmsHPROFILE cmsCreateXYZProfileTHR(cmsContext ContextID)
	{
	    cmsHPROFILE hProfile;
	    cmsPipeline LUT = null;
	    
	    hProfile = cmsCreateRGBProfileTHR(ContextID, lcms2.cmsD50_xyY, null, null);
	    if (hProfile == null)
	    {
	    	return null;
	    }
	    
	    lcms2.cmsSetProfileVersion(hProfile, 4.2);
	    
	    lcms2.cmsSetDeviceClass(hProfile, lcms2.cmsSigAbstractClass);
	    lcms2.cmsSetColorSpace(hProfile,  lcms2.cmsSigXYZData);
	    lcms2.cmsSetPCS(hProfile,         lcms2.cmsSigXYZData);
	    
	    if (!SetTextTags(hProfile, "XYZ identity built-in"))
	    {
	    	if (LUT != null)
		    {
		    	cmslut.cmsPipelineFree(LUT);
		    }
		    
		    if (hProfile != null)
		    {
		    	cmsio0.cmsCloseProfile(hProfile);
		    }
		    
		    return null;
	    }
	    
	    // An identity LUT is all we need
	    LUT = cmslut.cmsPipelineAlloc(ContextID, 3, 3);
	    if (LUT == null)
	    {
		    if (hProfile != null)
		    {
		    	cmsio0.cmsCloseProfile(hProfile);
		    }
		    
		    return null;
	    }
	    
	    cmslut.cmsPipelineInsertStage(LUT, lcms2.cmsAT_BEGIN, cmslut._cmsStageAllocIdentityCurves(ContextID, 3));
	    
	    if (!cmsio0.cmsWriteTag(hProfile, lcms2.cmsSigAToB0Tag, LUT))
	    {
	    	if (LUT != null)
		    {
		    	cmslut.cmsPipelineFree(LUT);
		    }
		    
		    if (hProfile != null)
		    {
		    	cmsio0.cmsCloseProfile(hProfile);
		    }
		    
		    return null;
	    }
	    cmslut.cmsPipelineFree(LUT);
	    
	    return hProfile;
	}
	
	public static cmsHPROFILE cmsCreateXYZProfile()
	{
	    return cmsCreateXYZProfileTHR(null);
	}
	
	//sRGB Curves are defined by:
	//
	//If  R’sRGB,G’sRGB, B’sRGB < 0.04045
	//
	//	    R =  R’sRGB / 12.92
	//	    G =  G’sRGB / 12.92
	//	    B =  B’sRGB / 12.92
	//
	//
	//else if  R’sRGB,G’sRGB, B’sRGB >= 0.04045
	//
	//	    R = ((R’sRGB + 0.055) / 1.055)^2.4
	//	    G = ((G’sRGB + 0.055) / 1.055)^2.4
	//	    B = ((B’sRGB + 0.055) / 1.055)^2.4
	
	private static cmsToneCurve Build_sRGBGamma(cmsContext ContextID)
	{
	    double[] Parameters = new double[5];
	    
	    Parameters[0] = 2.4;
	    Parameters[1] = 1. / 1.055;
	    Parameters[2] = 0.055 / 1.055;
	    Parameters[3] = 1. / 12.92;
	    Parameters[4] = 0.04045;    
	    
	    return cmsgamma.cmsBuildParametricToneCurve(ContextID, 4, Parameters);
	}
	
	// Create the ICC virtual profile for sRGB space 
	public static cmsHPROFILE cmsCreate_sRGBProfileTHR(cmsContext ContextID)
	{
		cmsCIExyY D65 = new cmsCIExyY();
		cmsCIExyYTRIPLE Rec709Primaries = new cmsCIExyYTRIPLE(new double[]{0.6400, 0.3300, 1.0, 0.3000, 0.6000, 1.0, 0.1500, 0.0600, 1.0});
		cmsToneCurve[] Gamma22 = new cmsToneCurve[3];
		cmsHPROFILE  hsRGB;
		
		cmswtpnt.cmsWhitePointFromTemp(D65, 6504);
		Gamma22[0] = Gamma22[1] = Gamma22[2] = Build_sRGBGamma(ContextID);
		if (Gamma22[0] == null)
		{
			return null;
		}
		
		hsRGB = cmsCreateRGBProfileTHR(ContextID, D65, Rec709Primaries, Gamma22);
		cmsgamma.cmsFreeToneCurve(Gamma22[0]);
		if (hsRGB == null)
		{
			return null;
		}
		
		if (!SetTextTags(hsRGB, "sRGB built-in"))
		{
			cmsio0.cmsCloseProfile(hsRGB);
			return null;
		}
		
		return hsRGB;
	}

	public static cmsHPROFILE cmsCreate_sRGBProfile()
	{
	    return cmsCreate_sRGBProfileTHR(null);
	}
	
	private static class BCHSWADJUSTS
	{
		public double Brightness;
		public double Contrast;
		public double Hue;
		public double Saturation;
		public cmsCIEXYZ WPsrc, WPdest;
	}
	
	private static final cmsSAMPLER16 bchswSampler = new cmsSAMPLER16()
	{
		public int run(short[] In, short[] Out, Object Cargo)
		{
			cmsCIELab LabIn = new cmsCIELab(), LabOut = new cmsCIELab();
		    cmsCIELCh LChIn = new cmsCIELCh(), LChOut = new cmsCIELCh();
		    cmsCIEXYZ XYZ = new cmsCIEXYZ();
		    BCHSWADJUSTS bchsw = (BCHSWADJUSTS)Cargo;
		    
		    cmspcs.cmsLabEncoded2Float(LabIn, In);
		    
		    cmspcs.cmsLab2LCh(LChIn, LabIn);
		    
		    // Do some adjusts on LCh
		    
		    LChOut.L = LChIn.L * bchsw.Contrast + bchsw.Brightness;
		    LChOut.C = LChIn.C + bchsw.Saturation;
		    LChOut.h = LChIn.h + bchsw.Hue;
		    
		    cmspcs.cmsLCh2Lab(LabOut, LChOut);
		    
		    // Move white point in Lab
		    
		    cmspcs.cmsLab2XYZ(bchsw.WPsrc,  XYZ, LabOut);
		    cmspcs.cmsXYZ2Lab(bchsw.WPdest, LabOut, XYZ);
		    
		    // Back to encoded
		    
		    cmspcs.cmsFloat2LabEncoded(Out, LabOut);
		    
		    return 1;
		}
	};
	
	// Creates an abstract profile operating in Lab space for Brightness,
	// contrast, Saturation and white point displacement

	public static cmsHPROFILE cmsCreateBCHSWabstractProfileTHR(cmsContext ContextID, int nLUTPoints, double Bright, double Contrast, double Hue, double Saturation, 
			int TempSrc, int TempDest)
	{
		cmsHPROFILE hICC;
		cmsPipeline Pipeline;
		BCHSWADJUSTS bchsw = new BCHSWADJUSTS();
		cmsCIExyY WhitePnt = new cmsCIExyY();
		cmsStage CLUT;
		int[] Dimensions = new int[lcms2_internal.MAX_INPUT_DIMENSIONS];
		int i;
		
		bchsw.Brightness = Bright;
		bchsw.Contrast   = Contrast;
		bchsw.Hue        = Hue;
		bchsw.Saturation = Saturation;
		
		cmswtpnt.cmsWhitePointFromTemp(WhitePnt, TempSrc);
		bchsw.WPsrc = new cmsCIEXYZ();
		cmspcs.cmsxyY2XYZ(bchsw.WPsrc, WhitePnt);
		
		cmswtpnt.cmsWhitePointFromTemp(WhitePnt, TempDest);
		bchsw.WPdest = new cmsCIEXYZ();
		cmspcs.cmsxyY2XYZ(bchsw.WPdest, WhitePnt);
		
		hICC = cmsio0.cmsCreateProfilePlaceholder(ContextID);
		if (hICC == null) // can't allocate
		{
			return null;
		}
		
		cmsio0.cmsSetDeviceClass(hICC, lcms2.cmsSigAbstractClass);
		cmsio0.cmsSetColorSpace(hICC,  lcms2.cmsSigLabData);
		cmsio0.cmsSetPCS(hICC,         lcms2.cmsSigLabData);
		
		cmsio0.cmsSetHeaderRenderingIntent(hICC, lcms2.INTENT_PERCEPTUAL);
		
		// Creates a Pipeline with 3D grid only
		Pipeline = cmslut.cmsPipelineAlloc(ContextID, 3, 3);
		if (Pipeline == null)
		{
			cmsio0.cmsCloseProfile(hICC);
			return null;
		}
		
		for (i=0; i < lcms2_internal.MAX_INPUT_DIMENSIONS; i++)
		{
			Dimensions[i] = nLUTPoints;
		}
		CLUT = cmslut.cmsStageAllocCLut16bitGranular(ContextID, Dimensions, 3, 3, null);
		if (CLUT == null)
		{
			return null;
		}
		
		if (!cmslut.cmsStageSampleCLut16bit(CLUT, bchswSampler, bchsw, 0))
		{
			// Shouldn't reach here
			cmslut.cmsPipelineFree(Pipeline);
			cmsio0.cmsCloseProfile(hICC);
			return null;
		}
		
		cmslut.cmsPipelineInsertStage(Pipeline, lcms2.cmsAT_END, CLUT);
		
		// Create tags
		
		if (!SetTextTags(hICC, "BCHS built-in"))
		{
			return null;    
		}
		
		cmsio0.cmsWriteTag(hICC, lcms2.cmsSigMediaWhitePointTag, lcms2.cmsD50_XYZ);
		
		cmsio0.cmsWriteTag(hICC, lcms2.cmsSigAToB0Tag, Pipeline);
		
		// Pipeline is already on virtual profile
		cmslut.cmsPipelineFree(Pipeline);
		
		// Ok, done
		return hICC;
	}
	
	public static cmsHPROFILE cmsCreateBCHSWabstractProfile(int nLUTPoints, double Bright, double Contrast, double Hue, double Saturation, int TempSrc, int TempDest)
	{
	    return cmsCreateBCHSWabstractProfileTHR(null, nLUTPoints, Bright, Contrast, Hue, Saturation, TempSrc, TempDest);
	}
	
	// Creates a fake NULL profile. This profile return 1 channel as always 0. 
	// Is useful only for gamut checking tricks
	public static cmsHPROFILE cmsCreateNULLProfileTHR(cmsContext ContextID)
	{
	    cmsHPROFILE hProfile;
	    cmsPipeline LUT = null;
	    cmsStage PostLin;
	    cmsToneCurve EmptyTab;
	    short[] Zero = new short[2];
	    
	    hProfile = cmsio0.cmsCreateProfilePlaceholder(ContextID);
	    if (hProfile == null) // can't allocate
	    {
	    	return null;
	    }
	    
	    cmsio0.cmsSetProfileVersion(hProfile, 4.2);
	    
	    if (!SetTextTags(hProfile, "NULL profile built-in"))
	    {
	    	if (LUT != null)
		    {
		    	cmslut.cmsPipelineFree(LUT);
		    }
		    
		    if (hProfile != null)
		    {
		    	cmsio0.cmsCloseProfile(hProfile);
		    }
		    
		    return null;
	    }
	    
	    cmsio0.cmsSetDeviceClass(hProfile, lcms2.cmsSigOutputClass);
	    cmsio0.cmsSetColorSpace(hProfile,  lcms2.cmsSigGrayData);
	    cmsio0.cmsSetPCS(hProfile,         lcms2.cmsSigLabData);
	    
	    // An empty LUTs is all we need
	    LUT = cmslut.cmsPipelineAlloc(ContextID, 1, 1);
	    if (LUT == null)
	    {
	    	if (LUT != null)
		    {
		    	cmslut.cmsPipelineFree(LUT);
		    }
		    
		    if (hProfile != null)
		    {
		    	cmsio0.cmsCloseProfile(hProfile);
		    }
		    
		    return null;
	    }
	    
	    EmptyTab = cmsgamma.cmsBuildTabulatedToneCurve16(ContextID, 2, Zero);       
	    PostLin = cmslut.cmsStageAllocToneCurves(ContextID, 1, new cmsToneCurve[]{EmptyTab});
	    cmsgamma.cmsFreeToneCurve(EmptyTab);
	    
	    cmslut.cmsPipelineInsertStage(LUT, lcms2.cmsAT_END, PostLin);
	    
	    if (!cmsio0.cmsWriteTag(hProfile, lcms2.cmsSigBToA0Tag, LUT))
	    {
	    	if (LUT != null)
		    {
		    	cmslut.cmsPipelineFree(LUT);
		    }
		    
		    if (hProfile != null)
		    {
		    	cmsio0.cmsCloseProfile(hProfile);
		    }
		    
		    return null;
	    }
	    if (!cmsio0.cmsWriteTag(hProfile, lcms2.cmsSigMediaWhitePointTag, lcms2.cmsD50_XYZ))
	    {
	    	if (LUT != null)
		    {
		    	cmslut.cmsPipelineFree(LUT);
		    }
		    
		    if (hProfile != null)
		    {
		    	cmsio0.cmsCloseProfile(hProfile);
		    }
		    
		    return null;
	    }
	    
	    cmslut.cmsPipelineFree(LUT);       
	    return hProfile;
	}
	
	public static cmsHPROFILE cmsCreateNULLProfile()
	{
	    return cmsCreateNULLProfileTHR(null);
	}
	
	private static boolean IsPCS(int ColorSpace)
	{
	    return (ColorSpace == lcms2.cmsSigXYZData || ColorSpace == lcms2.cmsSigLabData);
	}
	
	private static void FixColorSpaces(cmsHPROFILE hProfile, int ColorSpace, int PCS, int dwFlags)
	{
	    if ((dwFlags & lcms2.cmsFLAGS_GUESSDEVICECLASS) != 0)
	    {
	    	if (IsPCS(ColorSpace) && IsPCS(PCS))
	    	{
	    		cmsio0.cmsSetDeviceClass(hProfile, lcms2.cmsSigAbstractClass);
	    		cmsio0.cmsSetColorSpace(hProfile,  ColorSpace);
	    		cmsio0.cmsSetPCS(hProfile,         PCS);
	    		return;
	    	}
	    	
	    	if (IsPCS(ColorSpace) && !IsPCS(PCS))
	    	{
	    		cmsio0.cmsSetDeviceClass(hProfile, lcms2.cmsSigOutputClass);
	    		cmsio0.cmsSetPCS(hProfile,         ColorSpace);
	    		cmsio0.cmsSetColorSpace(hProfile,  PCS);
	    		return;
	    	}
	    	
	    	if (IsPCS(PCS) && !IsPCS(ColorSpace))
	    	{
	    		cmsio0.cmsSetDeviceClass(hProfile, lcms2.cmsSigInputClass);
	    		cmsio0.cmsSetColorSpace(hProfile,  ColorSpace);
	    		cmsio0.cmsSetPCS(hProfile,         PCS);
	    		return;
	    	}
	    }
	    
	    cmsio0.cmsSetDeviceClass(hProfile, lcms2.cmsSigLinkClass);
	    cmsio0.cmsSetColorSpace(hProfile,  ColorSpace);
	    cmsio0.cmsSetPCS(hProfile,         PCS);
	}
	
	// This function creates a named color profile dumping all the contents of transform to a single profile
	// In this way, LittleCMS may be used to "group" several named color databases into a single profile.
	// It has, however, several minor limitations. PCS is always Lab, which is not very critic since this
	// is the normal PCS for named color profiles.
	static
	cmsHPROFILE CreateNamedColorDevicelink(cmsHTRANSFORM xform)
	{
	    _cmsTRANSFORM v = (_cmsTRANSFORM)xform;
	    cmsHPROFILE hICC = null;  
	    int i, nColors;
	    cmsNAMEDCOLORLIST nc2 = null, Original = null;
	    
	    // Create an empty placeholder
	    hICC = cmsio0.cmsCreateProfilePlaceholder(v.ContextID);
	    if (hICC == null)
	    {
	    	return null;
	    }
	    
	    // Critical information
	    cmsio0.cmsSetDeviceClass(hICC, lcms2.cmsSigNamedColorClass);
	    cmsio0.cmsSetColorSpace(hICC, v.ExitColorSpace);
	    cmsio0.cmsSetPCS(hICC, lcms2.cmsSigLabData);
	    
	    // Tag profile with information
	    if (!SetTextTags(hICC, "Named color devicelink"))
	    {
	    	if (hICC != null)
		    {
		    	cmsio0.cmsCloseProfile(hICC);
		    }
		    return null;
	    }
	    
	    Original = cmsnamed.cmsGetNamedColorList(xform);
	    if (Original == null)
	    {
	    	if (hICC != null)
		    {
		    	cmsio0.cmsCloseProfile(hICC);
		    }
		    return null;
	    }
	    
	    nColors = cmsnamed.cmsNamedColorCount(Original);
	    nc2     = cmsnamed.cmsDupNamedColorList(Original);
	    if (nc2 == null)
	    {
	    	if (hICC != null)
		    {
		    	cmsio0.cmsCloseProfile(hICC);
		    }
		    return null;
	    }
	    
	    // Colorant count now depends on the output space 
	    nc2.ColorantCount = cmslut.cmsPipelineOutputChannels(v.Lut);
	    
	    // Apply the transfor to colorants.
	    for (i=0; i < nColors; i++)
	    {
	        cmsxform.cmsDoTransform(xform, new int[]{i}, nc2.List[i].DeviceColorant, 1);
	    }
	    
	    if (!cmsio0.cmsWriteTag(hICC, lcms2.cmsSigNamedColor2Tag, nc2))
	    {
	    	if (hICC != null)
		    {
		    	cmsio0.cmsCloseProfile(hICC);
		    }
		    return null;
	    }
	    cmsnamed.cmsFreeNamedColorList(nc2);
	    
	    return hICC;
	}
	
	// This structure holds information about which MPU can be stored on a profile based on the version
	
	private static class cmsAllowedLUT
	{
		public boolean IsV4;   // Is a V4 tag?
	    public int LutType;    // The LUT type
	    public int nTypes;     // Number of types (up to 5)
	    public int[] MpeTypes; // 5 is the maximum number
	    
	    public cmsAllowedLUT(boolean IsV4, int LutType, int nTypes, int[] MpeTypes)
	    {
	    	this.IsV4 = IsV4;
	    	this.LutType = LutType;
	    	this.nTypes = nTypes;
	    	this.MpeTypes = new int[5];
	    	System.arraycopy(MpeTypes, 0, this.MpeTypes, 0, nTypes);
	    }
	}
	
	private static cmsAllowedLUT[] AllowedLUTTypes;
	
	static
	{
		AllowedLUTTypes = new cmsAllowedLUT[]{
			new cmsAllowedLUT(false, lcms2.cmsSigLut16Type, 4, new int[]{lcms2.cmsSigMatrixElemType, lcms2.cmsSigCurveSetElemType, lcms2.cmsSigCLutElemType, lcms2.cmsSigCurveSetElemType}),
			new cmsAllowedLUT(false, lcms2.cmsSigLut16Type, 3, new int[]{lcms2.cmsSigCurveSetElemType, lcms2.cmsSigCLutElemType, lcms2.cmsSigCurveSetElemType}),
			new cmsAllowedLUT(true, lcms2.cmsSigLutAtoBType, 1, new int[]{lcms2.cmsSigCurveSetElemType}),
			new cmsAllowedLUT(true, lcms2.cmsSigLutAtoBType, 3, new int[]{lcms2.cmsSigCurveSetElemType, lcms2.cmsSigMatrixElemType, lcms2.cmsSigCurveSetElemType}),
			new cmsAllowedLUT(true, lcms2.cmsSigLutAtoBType, 3, new int[]{lcms2.cmsSigCurveSetElemType, lcms2.cmsSigCLutElemType, lcms2.cmsSigCurveSetElemType}),
			new cmsAllowedLUT(true, lcms2.cmsSigLutAtoBType, 5, new int[]{lcms2.cmsSigCurveSetElemType, lcms2.cmsSigCLutElemType, lcms2.cmsSigCurveSetElemType, lcms2.cmsSigMatrixElemType, lcms2.cmsSigCurveSetElemType}),
			new cmsAllowedLUT(true, lcms2.cmsSigLutBtoAType, 1, new int[]{lcms2.cmsSigCurveSetElemType}),
			new cmsAllowedLUT(true, lcms2.cmsSigLutBtoAType, 3, new int[]{lcms2.cmsSigCurveSetElemType, lcms2.cmsSigMatrixElemType, lcms2.cmsSigCurveSetElemType}),
			new cmsAllowedLUT(true, lcms2.cmsSigLutBtoAType, 3, new int[]{lcms2.cmsSigCurveSetElemType, lcms2.cmsSigCLutElemType, lcms2.cmsSigCurveSetElemType}),
			new cmsAllowedLUT(true, lcms2.cmsSigLutBtoAType, 5, new int[]{lcms2.cmsSigCurveSetElemType, lcms2.cmsSigMatrixElemType, lcms2.cmsSigCurveSetElemType, lcms2.cmsSigCLutElemType, lcms2.cmsSigCurveSetElemType})
		};
	}

	private static final int SIZE_OF_ALLOWED_LUT = 10;
	
	// Check a single entry
	private static boolean CheckOne(final cmsAllowedLUT Tab, final cmsPipeline Lut)
	{
	    cmsStage mpe;
	    int n;
	    
	    for (n=0, mpe = Lut.Elements; mpe != null; mpe = (cmsStage)mpe.Next, n++)
	    {
	        if (n > Tab.nTypes)
	        {
	        	return false;
	        }
	        if (cmslut.cmsStageType(mpe) != Tab.MpeTypes[n])
	        {
	        	return false;             
	        }
	    }
	    
	    return (n == Tab.nTypes);
	}
	
	private static cmsAllowedLUT FindCombination(final cmsPipeline Lut, boolean IsV4)
	{
	    int n;
	    
	    for (n=0; n < SIZE_OF_ALLOWED_LUT; n++)
	    {
	        final cmsAllowedLUT Tab = AllowedLUTTypes[n];
	        
	        if (IsV4 ^ Tab.IsV4)
	        {
	        	continue;
	        }
	        if (CheckOne(Tab, Lut))
	        {
	        	return Tab;
	        }
	    }
	    
	    return null;
	}
	
	// Does convert a transform into a device link profile
	public static cmsHPROFILE cmsTransform2DeviceLink(cmsHTRANSFORM hTransform, double Version, int dwFlags)
	{
	    cmsHPROFILE hProfile = null;
	    int FrmIn, FrmOut, ChansIn, ChansOut;
	    int ColorSpaceBitsIn, ColorSpaceBitsOut;
	    _cmsTRANSFORM xform = (_cmsTRANSFORM)hTransform;
	    cmsPipeline LUT = null;
	    cmsStage mpe;
	    cmsContext ContextID = cmsxform.cmsGetTransformContextID(hTransform);
	    cmsAllowedLUT AllowedLUT;
	    
	    lcms2_internal._cmsAssert(hTransform != null, "hTransform != null");
	    
	    // Get the first mpe to check for named color
	    mpe = cmslut.cmsPipelineGetPtrToFirstStage(xform.Lut);
	    
	    // Check if is a named color transform
	    if (mpe != null)
	    {
	        if (cmslut.cmsStageType(mpe) == lcms2.cmsSigNamedColorElemType)
	        {
	            return CreateNamedColorDevicelink(hTransform);
	        }
	    }
	    
	    // First thing to do is to get a copy of the transformation
	    LUT = cmslut.cmsPipelineDup(xform.Lut);
	    if (LUT == null)
	    {
	    	return null;
	    }
	    
	    // Time to fix the Lab2/Lab4 issue.
	    if ((xform.EntryColorSpace == lcms2.cmsSigLabData) && (Version < 4.0))
	    {
	    	cmslut.cmsPipelineInsertStage(LUT, lcms2.cmsAT_BEGIN, cmslut._cmsStageAllocLabV2ToV4curves(ContextID));        
	    }
	    
	    // On the output side too
	    if ((xform.ExitColorSpace) == lcms2.cmsSigLabData && (Version < 4.0)) {

	    	cmslut.cmsPipelineInsertStage(LUT, lcms2.cmsAT_END, cmslut._cmsStageAllocLabV4ToV2(ContextID));        
	    }
	    
	    // Optimize the LUT and precalculate a devicelink
	    
	    ChansIn  = cmspcs.cmsChannelsOf(xform.EntryColorSpace);
	    ChansOut = cmspcs.cmsChannelsOf(xform.ExitColorSpace);
	    
	    ColorSpaceBitsIn  = cmspcs._cmsLCMScolorSpace(xform.EntryColorSpace);
	    ColorSpaceBitsOut = cmspcs._cmsLCMScolorSpace(xform.ExitColorSpace);
	    
	    FrmIn  = lcms2.COLORSPACE_SH(ColorSpaceBitsIn) | lcms2.CHANNELS_SH(ChansIn)|(2 << lcms2.BYTES_SHIFT_VALUE);
	    FrmOut = lcms2.COLORSPACE_SH(ColorSpaceBitsOut) | lcms2.CHANNELS_SH(ChansOut)|(2 << lcms2.BYTES_SHIFT_VALUE);
	    
	    // Check if the profile/version can store the result
	    if ((dwFlags & lcms2.cmsFLAGS_FORCE_CLUT) != 0)
	    {
	    	AllowedLUT = null;
	    }
	    else
	    {
	    	AllowedLUT = FindCombination(LUT, Version >= 4.0);
	    }
	    
	    if (AllowedLUT == null)
	    {
	        // Try to optimize
	    	int[] temp1 = new int[]{FrmIn};
		    int[] temp2 = new int[]{FrmOut};
		    int[] temp3 = new int[]{dwFlags};
		    cmsPipeline[] temp4 = new cmsPipeline[]{LUT};
	        cmsopt._cmsOptimizePipeline(temp4, xform.RenderingIntent, temp1, temp2, temp3);
	        FrmIn = temp1[0];
	        FrmOut = temp2[0];
	        dwFlags = temp3[0];
	        LUT = temp4[0];
	        AllowedLUT = FindCombination(LUT, Version >= 4.0);
	    }
	    
	    // If no way, then force CLUT that for sure can be written
	    if (AllowedLUT == null)
	    {
	        dwFlags |= lcms2.cmsFLAGS_FORCE_CLUT;
	        int[] temp1 = new int[]{FrmIn};
		    int[] temp2 = new int[]{FrmOut};
		    int[] temp3 = new int[]{dwFlags};
		    cmsPipeline[] temp4 = new cmsPipeline[]{LUT};
	        cmsopt._cmsOptimizePipeline(temp4, xform.RenderingIntent, temp1, temp2, temp3);
	        FrmIn = temp1[0];
	        FrmOut = temp2[0];
	        dwFlags = temp3[0];
	        LUT = temp4[0];
	        
	        // Put identity curves if needed
	        if (cmslut.cmsPipelineStageCount(LUT) == 1)
	        {
	        	cmslut.cmsPipelineInsertStage(LUT, lcms2.cmsAT_BEGIN, cmslut._cmsStageAllocIdentityCurves(ContextID, ChansIn));    
	        	cmslut.cmsPipelineInsertStage(LUT, lcms2.cmsAT_END,   cmslut._cmsStageAllocIdentityCurves(ContextID, ChansOut));   
	        }
	        
	        AllowedLUT = FindCombination(LUT, Version >= 4.0);
	    }
	    
	    // Somethings is wrong...
	    if (AllowedLUT == null)
	    {
	    	if (LUT != null)
		    {
		    	cmslut.cmsPipelineFree(LUT);       
		    }
		    cmsio0.cmsCloseProfile(hProfile);
		    return null;
	    }
	    
	    hProfile = cmsio0.cmsCreateProfilePlaceholder(ContextID);
	    if (hProfile == null) // can't allocate
	    {
	    	if (LUT != null)
		    {
		    	cmslut.cmsPipelineFree(LUT);       
		    }
		    cmsio0.cmsCloseProfile(hProfile);
		    return null;
	    }
	    
	    cmsio0.cmsSetProfileVersion(hProfile, Version);
	    
	    FixColorSpaces(hProfile, xform.EntryColorSpace, xform.ExitColorSpace, dwFlags);   
	    
	    if ((dwFlags & lcms2.cmsFLAGS_8BITS_DEVICELINK) != 0)
	    {
	    	cmslut.cmsPipelineSetSaveAs8bitsFlag(LUT, true);
	    }
	    
	    // Tag profile with information
	    if (!SetTextTags(hProfile, "devicelink"))
	    {
	    	if (LUT != null)
		    {
		    	cmslut.cmsPipelineFree(LUT);       
		    }
		    cmsio0.cmsCloseProfile(hProfile);
		    return null;
	    }
	    
	    if (cmsio0.cmsGetDeviceClass(hProfile) == lcms2.cmsSigOutputClass)
	    {
	        if (!cmsio0.cmsWriteTag(hProfile, lcms2.cmsSigBToA0Tag, LUT))
	        {
	        	if (LUT != null)
	    	    {
	    	    	cmslut.cmsPipelineFree(LUT);       
	    	    }
	    	    cmsio0.cmsCloseProfile(hProfile);
	    	    return null;
	        }
	    }
	    else
	    {
	        if (!cmsio0.cmsWriteTag(hProfile, lcms2.cmsSigAToB0Tag, LUT))
	        {
	        	if (LUT != null)
	    	    {
	    	    	cmslut.cmsPipelineFree(LUT);       
	    	    }
	    	    cmsio0.cmsCloseProfile(hProfile);
	    	    return null;
	        }
	    }
	    
	    if (xform.InputColorant != null)
	    {
	    	if (!cmsio0.cmsWriteTag(hProfile, lcms2.cmsSigColorantTableTag, xform.InputColorant))
	    	{
	    		if (LUT != null)
	    	    {
	    	    	cmslut.cmsPipelineFree(LUT);       
	    	    }
	    	    cmsio0.cmsCloseProfile(hProfile);
	    	    return null;
	    	}
	    }
	    
	    if (xform.OutputColorant != null)
	    {
	    	if (!cmsio0.cmsWriteTag(hProfile, lcms2.cmsSigColorantTableOutTag, xform.OutputColorant))
	    	{
	    		if (LUT != null)
	    	    {
	    	    	cmslut.cmsPipelineFree(LUT);       
	    	    }
	    	    cmsio0.cmsCloseProfile(hProfile);
	    	    return null;
	    	}
	    }
	    
	    if (xform.Sequence != null)
	    {
	        if (!cmsio1._cmsWriteProfileSequence(hProfile, xform.Sequence))
	        {
	        	if (LUT != null)
	    	    {
	    	    	cmslut.cmsPipelineFree(LUT);       
	    	    }
	    	    cmsio0.cmsCloseProfile(hProfile);
	    	    return null;
	        }
	    }
	    
	    cmslut.cmsPipelineFree(LUT);
	    return hProfile;
	}
}
