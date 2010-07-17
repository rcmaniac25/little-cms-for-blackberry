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
// Version 2.0
//
package littlecms.internal;

import javax.microedition.io.ContentConnection;
import javax.microedition.media.protocol.SourceStream;

import net.rim.device.api.util.Arrays;

public final class lcms2
{
	/** Version/release*/
	public static final int LCMS_VERSION = 2000;
	
	// Some common definitions
	public static final int cmsMAX_PATH = 256;
	
	// D50 XYZ normalized to Y=1.0
	public static final double cmsD50X = 0.9642;
	public static final double cmsD50Y = 1.0;
	public static final double cmsD50Z = 0.8249;
	
	// V4 perceptual black
	public static final double cmsPERCEPTUAL_BLACK_X = 0.00336;
	public static final double cmsPERCEPTUAL_BLACK_Y = 0.0034731;
	public static final double cmsPERCEPTUAL_BLACK_Z = 0.00287;
	
	// Definitions in ICC spec
	/** Magic number to identify an ICC profile*/
	public static final int cmsMagicNumber = 0x61637370; // 'acsp'
	/** Little CMS signature*/
	public static final int lcmsSignature = 0x6c636d73; // 'lcms'
	
	// Base ICC type definitions
	public static final int cmsSigChromaticityType                  = 0x6368726D; // 'chrm'
    public static final int cmsSigColorantOrderType                 = 0x636C726F; // 'clro'
    public static final int cmsSigColorantTableType                 = 0x636C7274; // 'clrt'
    public static final int cmsSigCrdInfoType                       = 0x63726469; // 'crdi'
    public static final int cmsSigCurveType                         = 0x63757276; // 'curv'
    public static final int cmsSigDataType                          = 0x64617461; // 'data'
    public static final int cmsSigDateTimeType                      = 0x6474696D; // 'dtim'
    public static final int cmsSigDeviceSettingsType                = 0x64657673; // 'devs'
    public static final int cmsSigLut16Type                         = 0x6d667432; // 'mft2'
    public static final int cmsSigLut8Type                          = 0x6d667431; // 'mft1'
    public static final int cmsSigLutAtoBType                       = 0x6d414220; // 'mAB '
    public static final int cmsSigLutBtoAType                       = 0x6d424120; // 'mBA '
    public static final int cmsSigMeasurementType                   = 0x6D656173; // 'meas'
    public static final int cmsSigMultiLocalizedUnicodeType         = 0x6D6C7563; // 'mluc'
    public static final int cmsSigMultiProcessElementType           = 0x6D706574; // 'mpet'
    public static final int cmsSigNamedColorType                    = 0x6E636f6C; // 'ncol' -- DEPRECATED!
    public static final int cmsSigNamedColor2Type                   = 0x6E636C32; // 'ncl2'
    public static final int cmsSigParametricCurveType               = 0x70617261; // 'para'
    public static final int cmsSigProfileSequenceDescType           = 0x70736571; // 'pseq'
    public static final int cmsSigProfileSequenceIdType             = 0x70736964; // 'psid'
    public static final int cmsSigResponseCurveSet16Type            = 0x72637332; // 'rcs2'
    public static final int cmsSigS15Fixed16ArrayType               = 0x73663332; // 'sf32'
    public static final int cmsSigScreeningType                     = 0x7363726E; // 'scrn'
    public static final int cmsSigSignatureType                     = 0x73696720; // 'sig '
    public static final int cmsSigTextType                          = 0x74657874; // 'text'
    public static final int cmsSigTextDescriptionType               = 0x64657363; // 'desc'
    public static final int cmsSigU16Fixed16ArrayType               = 0x75663332; // 'uf32'
    public static final int cmsSigUcrBgType                         = 0x62666420; // 'bfd '
    public static final int cmsSigUInt16ArrayType                   = 0x75693136; // 'ui16'
    public static final int cmsSigUInt32ArrayType                   = 0x75693332; // 'ui32'
    public static final int cmsSigUInt64ArrayType                   = 0x75693634; // 'ui64'
    public static final int cmsSigUInt8ArrayType                    = 0x75693038; // 'ui08'
    public static final int cmsSigViewingConditionsType             = 0x76696577; // 'view'
    public static final int cmsSigXYZType                           = 0x58595A20; // 'XYZ '
    public static final int cmsSigVcgtType                          = 0x76636774; // 'vcgt'
    
    // Base ICC tag definitions
    public static final int cmsSigAToB0Tag                          = 0x41324230; // 'A2B0'
    public static final int cmsSigAToB1Tag                          = 0x41324231; // 'A2B1'
    public static final int cmsSigAToB2Tag                          = 0x41324232; // 'A2B2'
    public static final int cmsSigBlueColorantTag                   = 0x6258595A; // 'bXYZ'
    public static final int cmsSigBlueMatrixColumnTag               = 0x6258595A; // 'bXYZ'
    public static final int cmsSigBlueTRCTag                        = 0x62545243; // 'bTRC'
    public static final int cmsSigBToA0Tag                          = 0x42324130; // 'B2A0'
    public static final int cmsSigBToA1Tag                          = 0x42324131; // 'B2A1'
    public static final int cmsSigBToA2Tag                          = 0x42324132; // 'B2A2'
    public static final int cmsSigCalibrationDateTimeTag            = 0x63616C74; // 'calt'
    public static final int cmsSigCharTargetTag                     = 0x74617267; // 'targ'
    public static final int cmsSigChromaticAdaptationTag            = 0x63686164; // 'chad'
    public static final int cmsSigChromaticityTag                   = 0x6368726D; // 'chrm'
    public static final int cmsSigColorantOrderTag                  = 0x636C726F; // 'clro'
    public static final int cmsSigColorantTableTag                  = 0x636C7274; // 'clrt'
    public static final int cmsSigColorantTableOutTag               = 0x636C6F74; // 'clot'
    public static final int cmsSigColorimetricIntentImageStateTag   = 0x63696973; // 'ciis'
    public static final int cmsSigCopyrightTag                      = 0x63707274; // 'cprt'
    public static final int cmsSigCrdInfoTag                        = 0x63726469; // 'crdi'
    public static final int cmsSigDataTag                           = 0x64617461; // 'data'
    public static final int cmsSigDateTimeTag                       = 0x6474696D; // 'dtim'
    public static final int cmsSigDeviceMfgDescTag                  = 0x646D6E64; // 'dmnd'
    public static final int cmsSigDeviceModelDescTag                = 0x646D6464; // 'dmdd'
    public static final int cmsSigDeviceSettingsTag                 = 0x64657673; // 'devs'
    public static final int cmsSigDToB0Tag                          = 0x44324230; // 'D2B0'
    public static final int cmsSigDToB1Tag                          = 0x44324231; // 'D2B1'
    public static final int cmsSigDToB2Tag                          = 0x44324232; // 'D2B2'
    public static final int cmsSigDToB3Tag                          = 0x44324233; // 'D2B3'
    public static final int cmsSigBToD0Tag                          = 0x42324430; // 'B2D0'
    public static final int cmsSigBToD1Tag                          = 0x42324431; // 'B2D1'
    public static final int cmsSigBToD2Tag                          = 0x42324432; // 'B2D2'
    public static final int cmsSigBToD3Tag                          = 0x42324433; // 'B2D3'
    public static final int cmsSigGamutTag                          = 0x67616D74; // 'gamt'
    public static final int cmsSigGrayTRCTag                        = 0x6b545243; // 'kTRC'
    public static final int cmsSigGreenColorantTag                  = 0x6758595A; // 'gXYZ'
    public static final int cmsSigGreenMatrixColumnTag              = 0x6758595A; // 'gXYZ'
    public static final int cmsSigGreenTRCTag                       = 0x67545243; // 'gTRC'
    public static final int cmsSigLuminanceTag                      = 0x6C756d69; // 'lumi'
    public static final int cmsSigMeasurementTag                    = 0x6D656173; // 'meas'
    public static final int cmsSigMediaBlackPointTag                = 0x626B7074; // 'bkpt'
    public static final int cmsSigMediaWhitePointTag                = 0x77747074; // 'wtpt'
    public static final int cmsSigNamedColorTag                     = 0x6E636f6C; // 'ncol' // Deprecated by the ICC
    public static final int cmsSigNamedColor2Tag                    = 0x6E636C32; // 'ncl2'
    public static final int cmsSigOutputResponseTag                 = 0x72657370; // 'resp'
    public static final int cmsSigPerceptualRenderingIntentGamutTag = 0x72696730; // 'rig0'
    public static final int cmsSigPreview0Tag                       = 0x70726530; // 'pre0'
    public static final int cmsSigPreview1Tag                       = 0x70726531; // 'pre1'
    public static final int cmsSigPreview2Tag                       = 0x70726532; // 'pre2'
    public static final int cmsSigProfileDescriptionTag             = 0x64657363; // 'desc'
    public static final int cmsSigProfileSequenceDescTag            = 0x70736571; // 'pseq'
    public static final int cmsSigProfileSequenceIdTag              = 0x70736964; // 'psid'
    public static final int cmsSigPs2CRD0Tag                        = 0x70736430; // 'psd0'
    public static final int cmsSigPs2CRD1Tag                        = 0x70736431; // 'psd1'
    public static final int cmsSigPs2CRD2Tag                        = 0x70736432; // 'psd2'
    public static final int cmsSigPs2CRD3Tag                        = 0x70736433; // 'psd3'
    public static final int cmsSigPs2CSATag                         = 0x70733273; // 'ps2s'
    public static final int cmsSigPs2RenderingIntentTag             = 0x70733269; // 'ps2i'
    public static final int cmsSigRedColorantTag                    = 0x7258595A; // 'rXYZ'
    public static final int cmsSigRedMatrixColumnTag                = 0x7258595A; // 'rXYZ'
    public static final int cmsSigRedTRCTag                         = 0x72545243; // 'rTRC'
    public static final int cmsSigSaturationRenderingIntentGamutTag = 0x72696732; // 'rig2'
    public static final int cmsSigScreeningDescTag                  = 0x73637264; // 'scrd'
    public static final int cmsSigScreeningTag                      = 0x7363726E; // 'scrn'
    public static final int cmsSigTechnologyTag                     = 0x74656368; // 'tech'
    public static final int cmsSigUcrBgTag                          = 0x62666420; // 'bfd '
    public static final int cmsSigViewingCondDescTag                = 0x76756564; // 'vued'
    public static final int cmsSigViewingConditionsTag              = 0x76696577; // 'view'
    public static final int cmsSigVcgtTag                           = 0x76636774; // 'vcgt'
    
    // ICC Technology tag
    public static final int cmsSigDigitalCamera                     = 0x6463616D; // 'dcam'
    public static final int cmsSigFilmScanner                       = 0x6673636E; // 'fscn'
    public static final int cmsSigReflectiveScanner                 = 0x7273636E; // 'rscn'
    public static final int cmsSigInkJetPrinter                     = 0x696A6574; // 'ijet'
    public static final int cmsSigThermalWaxPrinter                 = 0x74776178; // 'twax'
    public static final int cmsSigElectrophotographicPrinter        = 0x6570686F; // 'epho'
    public static final int cmsSigElectrostaticPrinter              = 0x65737461; // 'esta'
    public static final int cmsSigDyeSublimationPrinter             = 0x64737562; // 'dsub'
    public static final int cmsSigPhotographicPaperPrinter          = 0x7270686F; // 'rpho'
    public static final int cmsSigFilmWriter                        = 0x6670726E; // 'fprn'
    public static final int cmsSigVideoMonitor                      = 0x7669646D; // 'vidm'
    public static final int cmsSigVideoCamera                       = 0x76696463; // 'vidc'
    public static final int cmsSigProjectionTelevision              = 0x706A7476; // 'pjtv'
    public static final int cmsSigCRTDisplay                        = 0x43525420; // 'CRT '
    public static final int cmsSigPMDisplay                         = 0x504D4420; // 'PMD '
    public static final int cmsSigAMDisplay                         = 0x414D4420; // 'AMD '
    public static final int cmsSigPhotoCD                           = 0x4B504344; // 'KPCD'
    public static final int cmsSigPhotoImageSetter                  = 0x696D6773; // 'imgs'
    public static final int cmsSigGravure                           = 0x67726176; // 'grav'
    public static final int cmsSigOffsetLithography                 = 0x6F666673; // 'offs'
    public static final int cmsSigSilkscreen                        = 0x73696C6B; // 'silk'
    public static final int cmsSigFlexography                       = 0x666C6578; // 'flex'
    public static final int cmsSigMotionPictureFilmScanner          = 0x6D706673; // 'mpfs'
    public static final int cmsSigMotionPictureFilmRecorder         = 0x6D706672; // 'mpfr'
    public static final int cmsSigDigitalMotionPictureCamera        = 0x646D7063; // 'dmpc'
    public static final int cmsSigDigitalCinemaProjector            = 0x64636A70; // 'dcpj'
    
    // ICC Color spaces
    public static final int cmsSigXYZData                           = 0x58595A20; // 'XYZ '
    public static final int cmsSigLabData                           = 0x4C616220; // 'Lab '
    public static final int cmsSigLuvData                           = 0x4C757620; // 'Luv '
    public static final int cmsSigYCbCrData                         = 0x59436272; // 'YCbr'
    public static final int cmsSigYxyData                           = 0x59787920; // 'Yxy '
    public static final int cmsSigRgbData                           = 0x52474220; // 'RGB '
    public static final int cmsSigGrayData                          = 0x47524159; // 'GRAY'
    public static final int cmsSigHsvData                           = 0x48535620; // 'HSV '
    public static final int cmsSigHlsData                           = 0x484C5320; // 'HLS '
    public static final int cmsSigCmykData                          = 0x434D594B; // 'CMYK'
    public static final int cmsSigCmyData                           = 0x434D5920; // 'CMY '
    public static final int cmsSigMCH1Data                          = 0x4D434831; // 'MCH1'
    public static final int cmsSigMCH2Data                          = 0x4D434832; // 'MCH2'
    public static final int cmsSigMCH3Data                          = 0x4D434833; // 'MCH3'
    public static final int cmsSigMCH4Data                          = 0x4D434834; // 'MCH4'
    public static final int cmsSigMCH5Data                          = 0x4D434835; // 'MCH5'
    public static final int cmsSigMCH6Data                          = 0x4D434836; // 'MCH6'
    public static final int cmsSigMCH7Data                          = 0x4D434837; // 'MCH7'
    public static final int cmsSigMCH8Data                          = 0x4D434838; // 'MCH8'
    public static final int cmsSigMCH9Data                          = 0x4D434839; // 'MCH9'
    public static final int cmsSigMCHAData                          = 0x4D43483A; // 'MCHA'
    public static final int cmsSigMCHBData                          = 0x4D43483B; // 'MCHB'
    public static final int cmsSigMCHCData                          = 0x4D43483C; // 'MCHC'
    public static final int cmsSigMCHDData                          = 0x4D43483D; // 'MCHD'
    public static final int cmsSigMCHEData                          = 0x4D43483E; // 'MCHE'
    public static final int cmsSigMCHFData                          = 0x4D43483F; // 'MCHF'
    public static final int cmsSigNamedData                         = 0x6e6d636c; // 'nmcl'
    public static final int cmsSig1colorData                        = 0x31434C52; // '1CLR'
    public static final int cmsSig2colorData                        = 0x32434C52; // '2CLR'
    public static final int cmsSig3colorData                        = 0x33434C52; // '3CLR'
    public static final int cmsSig4colorData                        = 0x34434C52; // '4CLR'
    public static final int cmsSig5colorData                        = 0x35434C52; // '5CLR'
    public static final int cmsSig6colorData                        = 0x36434C52; // '6CLR'
    public static final int cmsSig7colorData                        = 0x37434C52; // '7CLR'
    public static final int cmsSig8colorData                        = 0x38434C52; // '8CLR'
    public static final int cmsSig9colorData                        = 0x39434C52; // '9CLR'
    public static final int cmsSig10colorData                       = 0x41434C52; // 'ACLR'
    public static final int cmsSig11colorData                       = 0x42434C52; // 'BCLR'
    public static final int cmsSig12colorData                       = 0x43434C52; // 'CCLR'
    public static final int cmsSig13colorData                       = 0x44434C52; // 'DCLR'
    public static final int cmsSig14colorData                       = 0x45434C52; // 'ECLR'
    public static final int cmsSig15colorData                       = 0x46434C52; // 'FCLR'
    public static final int cmsSigLuvKData                          = 0x4C75764B; // 'LuvK'
    
    // ICC Profile Class
    public static final int cmsSigInputClass                        = 0x73636E72; // 'scnr'
    public static final int cmsSigDisplayClass                      = 0x6D6E7472; // 'mntr'
    public static final int cmsSigOutputClass                       = 0x70727472; // 'prtr'
    public static final int cmsSigLinkClass                         = 0x6C696E6B; // 'link'
    public static final int cmsSigAbstractClass                     = 0x61627374; // 'abst'
    public static final int cmsSigColorSpaceClass                   = 0x73706163; // 'spac'
    public static final int cmsSigNamedColorClass                   = 0x6e6d636c; // 'nmcl'
    
    // ICC Platforms
    public static final int cmsSigMacintosh                         = 0x4150504C; // 'APPL'
    public static final int cmsSigMicrosoft                         = 0x4D534654; // 'MSFT'
    public static final int cmsSigSolaris                           = 0x53554E57; // 'SUNW'
    public static final int cmsSigSGI                               = 0x53474920; // 'SGI '
    public static final int cmsSigTaligent                          = 0x54474E54; // 'TGNT'
    public static final int cmsSigUnices                            = 0x2A6E6978; // '*nix'   // From argyll -- Not official
    
    // Reference gamut
    public static final int cmsSigPerceptualReferenceMediumGamut       = 0x70726d67; //'prmg'

    // For cmsSigColorimetricIntentImageStateTag
    public static final int cmsSigSceneColorimetryEstimates            = 0x73636F65; //'scoe'
    public static final int cmsSigSceneAppearanceEstimates             = 0x73617065; //'sape'
    public static final int cmsSigFocalPlaneColorimetryEstimates       = 0x66706365; //'fpce'
    public static final int cmsSigReflectionHardcopyOriginalColorimetry= 0x72686F63; //'rhoc'
    public static final int cmsSigReflectionPrintOutputColorimetry     = 0x72706F63; //'rpoc'
    
    // Multi process elements types
    public static final int cmsSigCurveSetElemType              = 0x63767374; //'cvst'
    public static final int cmsSigMatrixElemType                = 0x6D617466; //'matf'
    public static final int cmsSigCLutElemType                  = 0x636C7574; //'clut'

    public static final int cmsSigBAcsElemType                  = 0x62414353; // 'bACS'
    public static final int cmsSigEAcsElemType                  = 0x65414353; // 'eACS'

    // Custom from here, not in the ICC Spec
    public static final int cmsSigXYZ2LabElemType               = 0x6C327820; // 'l2x '
    public static final int cmsSigLab2XYZElemType               = 0x78326C20; // 'x2l '
    public static final int cmsSigNamedColorElemType            = 0x6E636C20; // 'ncl '
    public static final int cmsSigLabV2toV4                     = 0x32203420; // '2 4 '
    public static final int cmsSigLabV4toV2                     = 0x34203220; // '4 2 '

    // Identities
    public static final int cmsSigIdentityElemType              = 0x69646E20; // 'idn '
    
    // Types of CurveElements
    public static final int cmsSigFormulaCurveSeg               = 0x70617266; // 'parf'
    public static final int cmsSigSampledCurveSeg               = 0x73616D66; // 'samf'
    public static final int cmsSigSegmentedCurve                = 0x63757266; // 'curf'
   
    // Used in ResponseCurveType
    public static final int cmsSigStatusA                  = 0x53746141; //'StaA'
    public static final int cmsSigStatusE                  = 0x53746145; //'StaE'
    public static final int cmsSigStatusI                  = 0x53746149; //'StaI'
    public static final int cmsSigStatusT                  = 0x53746154; //'StaT'
    public static final int cmsSigStatusM                  = 0x5374614D; //'StaM'
    public static final int cmsSigDN                       = 0x444E2020; //'DN  '
    public static final int cmsSigDNP                      = 0x444E2050; //'DN P'
    public static final int cmsSigDNN                      = 0x444E4E20; //'DNN '
    public static final int cmsSigDNNP                     = 0x444E4E50; //'DNNP'

    // Device attributes, currently defined values correspond to the low 4 bytes
    // of the 8 byte attribute quantity
    public static final int cmsReflective   = 0;
    public static final int cmsTransparency = 1;
    public static final int cmsGlossy       = 0;
    public static final int cmsMatte        = 2;
    
    // Common structures in ICC tags
    public static class cmsICCData
    {
    	public int len;
    	public int flag;
    	public final byte[] data;
    	
    	public cmsICCData()
    	{
    		len = 0;
    		flag = 0;
    		data = new byte[1];
    	}
    }
    
    /** ICC date time*/
    public static class cmsDateTimeNumber
    {
    	public short year;
        public short month;
        public short day;
        public short hours;
        public short minutes;
        public short seconds;
    }
    
    /** ICC XYZ*/
    public static class cmsEncodedXYZNumber
    {
        public short X;
        public short Y;
        public short Z;
    }
    
    /** Profile ID as computed by MD5 algorithm*/
    public static class cmsProfileID
    {
    	private final byte[] data;
    	
    	public cmsProfileID()
    	{
    		data = new byte[16];
    	}
    	
    	public byte[] getID8()
    	{
    		return Arrays.copy(data);
    	}
    	
    	public void setID8(byte[] data)
    	{
    		System.arraycopy(data, 0, this.data, 0, Math.min(8, data.length));
    	}
    	
    	public short[] getID16()
    	{
    		short[] id = new short[8];
    		for(int i = 0, k = 0; i < 8; i++, k += 2)
    		{
    			id[i] = getShort(this.data, k);
    		}
    		return id;
    	}
    	
    	public void setID16(short[] data)
    	{
    		for(int i = 0, k = 0; i < 8; i++, k += 2)
    		{
    			getBytes(data[i], this.data, k);
    		}
    	}
    	
    	public int[] getID32()
    	{
    		int[] id = new int[4];
    		for(int i = 0, k = 0; i < 4; i++, k += 4)
    		{
    			id[i] = getInt(this.data, k);
    		}
    		return id;
    	}
    	
    	public void setID32(int[] data)
    	{
    		for(int i = 0, k = 0; i < 4; i++, k += 4)
    		{
    			getBytes(data[i], this.data, k);
    		}
    	}
    }
    
    public static short getShort(byte[] data, int index)
    {
    	return (short)(((data[index + 1] & 0xFF) << 8) | (data[index] & 0xFF));
    }
    
    public static void getBytes(short value, byte[] data, int index)
    {
    	data[index] = (byte)(value & 0xFF);
    	data[index + 1] = (byte)((value >> 8) & 0xFF);
    }
    
    public static int getInt(byte[] data, int index)
    {
    	return ((data[index + 3] & 0xFF) << 24) | ((data[index + 2] & 0xFF) << 16) | ((data[index + 1] & 0xFF) << 8) | (data[index] & 0xFF);
    }
    
    public static void getBytes(int value, byte[] data, int index)
    {
    	data[index] = (byte)(value & 0xFF);
    	data[index + 1] = (byte)((value >> 8) & 0xFF);
    	data[index + 2] = (byte)((value >> 16) & 0xFF);
    	data[index + 3] = (byte)((value >> 24) & 0xFF);
    }
    
    // ----------------------------------------------------------------------------------------------
    // ICC profile internal base types. Strictly, shouldn't be declared in this header, but maybe
    // somebody want to use this info for accessing profile header directly, so here it is.
    
    /** Profile header -- it is 32-bit aligned, so no issues are expected on alignment*/
    public static class cmsICCHeader
    {
    	/** Profile size in bytes*/
    	public int                   size;
    	/** CMM for this profile*/
        public int                   cmmId;
        /** Format version number*/
        public int                   version;
        /** Type of profile*/
        public int                   deviceClass;
        /** Color space of data*/
        public int                   colorSpace;
        /** PCS, XYZ or Lab only*/
        public int                   pcs;
        /** Date profile was created*/
        public cmsDateTimeNumber     date;
        /** Magic Number to identify an ICC profile*/
        public int                   magic;
        /** Primary Platform*/
        public int                   platform;
        /** Various bit settings*/
        public int                   flags;
        /** Device manufacturer*/
        public int                   manufacturer;
        /** Device model number*/
        public int                   model;
        /** Device attributes*/
        public long                  attributes;
        /** Rendering intent*/
        public int                   renderingIntent;
        /** Profile illuminant*/
        public cmsEncodedXYZNumber   illuminant;
        /** Profile creator*/
        public int                   creator;
        /** Profile ID using MD5*/
        public cmsProfileID          profileID;
        /** Reserved for future use*/
        public final byte[]                reserved;
        
        public cmsICCHeader()
        {
        	this.reserved = new byte[28];
        }
    }
    
    /** ICC base tag*/
    public static class cmsTagBase
    {
    	public int sig;
        public final byte[] reserved;
        
        public cmsTagBase()
        {
        	this.reserved = new byte[4];
        }
    }
    
    /** A tag entry in directory*/
    public static class cmsTagEntry
    {
    	/** The tag signature*/
    	public int      sig;
    	/** Start of tag*/
        public int      offset;
        /** Size in bytes*/
        public int      size;
    }
    
    // ----------------------------------------------------------------------------------------------
    
    // Little CMS specific typedefs
    
    /** Void pointer passed through on high-level functions*/
    public static interface cmsContext{} //Context identifier for multithreaded environments
    /** Generic handle*/
    public static interface cmsHANDLE{}
    /** Handle to a profile*/
    public static interface cmsHPROFILE{} //Opaque typedefs to hide internals
    /** Handle to a color transform*/
    public static interface cmsHTRANSFORM{}
    
    /** Maximum number of channels in ICC profiles*/
    public static final int cmsMAXCHANNELS = 16;
    
    
    // Format of pixel is defined by one cmsUInt32Number, using bit fields as follows
    //
    //                A O TTTTT U Y F P X S EEE CCCC BBB
    //
    //                A: Floating point -- With this flag we can differentiate 16 bits as float and as int
    //                O: Optimized -- previous optimization already returns the final 8-bit value
    //                T: Pixeltype
    //                F: Flavor  0=MinIsBlack(Chocolate) 1=MinIsWhite(Vanilla)
    //                P: Planar? 0=Chunky, 1=Planar
    //                X: swap 16 bps endianess?
    //                S: Do swap? ie, BGR, KYMC
    //                E: Extra samples
    //                C: Channels (Samples per pixel)
    //                B: bytes per sample
    //                Y: Swap first - changes ABGR to BGRA and KCMY to CMYK
    
    public static final int FLOAT_SHIFT_VALUE = 22;
    public static final int OPTIMIZED_SHIFT_VALUE = 21;
    public static final int COLORSPACE_SHIFT_VALUE = 16;
    public static final int SWAPFIRST_SHIFT_VALUE = 14;
    public static final int FLAVOR_SHIFT_VALUE = 13;
    public static final int PLANAR_SHIFT_VALUE = 12;
    public static final int ENDIAN16_SHIFT_VALUE = 11;
    public static final int DOSWAP_SHIFT_VALUE = 10;
    public static final int EXTRA_SHIFT_VALUE = 7;
    public static final int CHANNELS_SHIFT_VALUE = 3;
    public static final int BYTES_SHIFT_VALUE = 0;
    
    public static int FLOAT_SH(int a){            return ((a) << FLOAT_SHIFT_VALUE);}
    public static int OPTIMIZED_SH(int s){        return ((s) << OPTIMIZED_SHIFT_VALUE);}
    public static int COLORSPACE_SH(int s){       return ((s) << COLORSPACE_SHIFT_VALUE);}
    public static int SWAPFIRST_SH(int s){        return ((s) << SWAPFIRST_SHIFT_VALUE);}
    public static int FLAVOR_SH(int s){           return ((s) << FLAVOR_SHIFT_VALUE);}
    public static int PLANAR_SH(int p){           return ((p) << PLANAR_SHIFT_VALUE);}
    public static int ENDIAN16_SH(int e){         return ((e) << ENDIAN16_SHIFT_VALUE);}
    public static int DOSWAP_SH(int e){           return ((e) << DOSWAP_SHIFT_VALUE);}
    public static int EXTRA_SH(int e){            return ((e) << EXTRA_SHIFT_VALUE);}
    public static int CHANNELS_SH(int c){         return ((c) << CHANNELS_SHIFT_VALUE);}
    public static int BYTES_SH(int b){            return ((b) << BYTES_SHIFT_VALUE);}
    
    // These macros unpack format specifiers into integers
    public static int T_FLOAT(int a){            return (((a) >> FLOAT_SHIFT_VALUE) & 1);}
    public static int T_OPTIMIZED(int o){        return (((o) >> OPTIMIZED_SHIFT_VALUE) & 1);}
    public static int T_COLORSPACE(int s){       return (((s) >> COLORSPACE_SHIFT_VALUE) & 31);}
    public static int T_SWAPFIRST(int s){        return (((s) >> SWAPFIRST_SHIFT_VALUE) & 1);}
    public static int T_FLAVOR(int s){           return (((s) >> FLAVOR_SHIFT_VALUE) & 1);}
    public static int T_PLANAR(int p){           return (((p) >> PLANAR_SHIFT_VALUE) & 1);}
    public static int T_ENDIAN16(int e){         return (((e) >> ENDIAN16_SHIFT_VALUE) & 1);}
    public static int T_DOSWAP(int e){           return (((e) >> DOSWAP_SHIFT_VALUE) & 1);}
    public static int T_EXTRA(int e){            return (((e) >> EXTRA_SHIFT_VALUE) & 7);}
    public static int T_CHANNELS(int c){         return (((c) >> CHANNELS_SHIFT_VALUE) & 15);}
    public static int T_BYTES(int b){            return (((b) >> BYTES_SHIFT_VALUE) & 7);}
    
    // Pixel types
    public static final int PT_ANY     = 0;    // Don't check colorspace
    										   // 1 & 2 are reserved
    public static final int PT_GRAY    = 3;
    public static final int PT_RGB     = 4;
    public static final int PT_CMY     = 5;
    public static final int PT_CMYK    = 6;
    public static final int PT_YCbCr   = 7;
    public static final int PT_YUV     = 8;     // Lu'v'
    public static final int PT_XYZ     = 9;
    public static final int PT_Lab     = 10;
    public static final int PT_YUVK    = 11;    // Lu'v'K
    public static final int PT_HSV     = 12;
    public static final int PT_HLS     = 13;
    public static final int PT_Yxy     = 14;
    
    public static final int PT_MCH1    = 15;
    public static final int PT_MCH2    = 16;
    public static final int PT_MCH3    = 17;
    public static final int PT_MCH4    = 18;
    public static final int PT_MCH5    = 19;
    public static final int PT_MCH6    = 20;
    public static final int PT_MCH7    = 21;
    public static final int PT_MCH8    = 22;
    public static final int PT_MCH9    = 23;
    public static final int PT_MCH10   = 24;
    public static final int PT_MCH11   = 25;
    public static final int PT_MCH12   = 26;
    public static final int PT_MCH13   = 27;
    public static final int PT_MCH14   = 28;
    public static final int PT_MCH15   = 29;
    
    public static final int PT_LabV2   = 30;    // Identical to PT_Lab, but using the V2 old encoding
    
	// Some (not all!) representations

	/* 
	 * FIND:#define
	 * REPLACE:public static final int
	 * 
	 * Regular expressions where used to convert to current format, syntaxes below. Done with VS which supports "tagged expressions"
	 * 
	 * FIND:{[A-Za-z0-9]+}_SH\({[A-Za-z0-9_]+}\)
	 * REPLACE:(\2 << \1_SHIFT_VALUE)
	 * 
	 * Unused:
	 * FIND: +\(
	 * FIND: = (
	 */
	
	public static final int TYPE_GRAY_8          = ((PT_GRAY << COLORSPACE_SHIFT_VALUE) | (1 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE));
	public static final int TYPE_GRAY_8_REV      = ((PT_GRAY << COLORSPACE_SHIFT_VALUE) | (1 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << FLAVOR_SHIFT_VALUE));
	public static final int TYPE_GRAY_16         = ((PT_GRAY << COLORSPACE_SHIFT_VALUE) | (1 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	public static final int TYPE_GRAY_16_REV     = ((PT_GRAY << COLORSPACE_SHIFT_VALUE) | (1 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << FLAVOR_SHIFT_VALUE));
	public static final int TYPE_GRAY_16_SE      = ((PT_GRAY << COLORSPACE_SHIFT_VALUE) | (1 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	public static final int TYPE_GRAYA_8         = ((PT_GRAY << COLORSPACE_SHIFT_VALUE) | (1 << EXTRA_SHIFT_VALUE) | (1 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE));
	public static final int TYPE_GRAYA_16        = ((PT_GRAY << COLORSPACE_SHIFT_VALUE) | (1 << EXTRA_SHIFT_VALUE) | (1 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	public static final int TYPE_GRAYA_16_SE     = ((PT_GRAY << COLORSPACE_SHIFT_VALUE) | (1 << EXTRA_SHIFT_VALUE) | (1 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	public static final int TYPE_GRAYA_8_PLANAR  = ((PT_GRAY << COLORSPACE_SHIFT_VALUE) | (1 << EXTRA_SHIFT_VALUE) | (1 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	public static final int TYPE_GRAYA_16_PLANAR = ((PT_GRAY << COLORSPACE_SHIFT_VALUE) | (1 << EXTRA_SHIFT_VALUE) | (1 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	
	public static final int TYPE_RGB_8           = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE));
	public static final int TYPE_RGB_8_PLANAR    = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	public static final int TYPE_BGR_8           = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE));
	public static final int TYPE_BGR_8_PLANAR    = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	public static final int TYPE_RGB_16          = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	public static final int TYPE_RGB_16_PLANAR   = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	public static final int TYPE_RGB_16_SE       = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	public static final int TYPE_BGR_16          = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE));
	public static final int TYPE_BGR_16_PLANAR   = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	public static final int TYPE_BGR_16_SE       = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	
	public static final int TYPE_RGBA_8          = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (1 << EXTRA_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE));
	public static final int TYPE_RGBA_8_PLANAR   = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (1 << EXTRA_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	public static final int TYPE_RGBA_16         = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (1 << EXTRA_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	public static final int TYPE_RGBA_16_PLANAR  = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (1 << EXTRA_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	public static final int TYPE_RGBA_16_SE      = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (1 << EXTRA_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	
	public static final int TYPE_ARGB_8          = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (1 << EXTRA_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << SWAPFIRST_SHIFT_VALUE));
	public static final int TYPE_ARGB_16         = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (1 << EXTRA_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << SWAPFIRST_SHIFT_VALUE));
	
	public static final int TYPE_ABGR_8          = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (1 << EXTRA_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE));
	public static final int TYPE_ABGR_16         = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (1 << EXTRA_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE));
	public static final int TYPE_ABGR_16_PLANAR  = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (1 << EXTRA_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	public static final int TYPE_ABGR_16_SE      = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (1 << EXTRA_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	
	public static final int TYPE_BGRA_8          = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (1 << EXTRA_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE) | (1 << SWAPFIRST_SHIFT_VALUE));
	public static final int TYPE_BGRA_16         = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (1 << EXTRA_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE) | (1 << SWAPFIRST_SHIFT_VALUE));
	public static final int TYPE_BGRA_16_SE      = ((PT_RGB << COLORSPACE_SHIFT_VALUE) | (1 << EXTRA_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE) | (1 << SWAPFIRST_SHIFT_VALUE));
	
	public static final int TYPE_CMY_8           = ((PT_CMY << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMY_8_PLANAR    = ((PT_CMY << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	public static final int TYPE_CMY_16          = ((PT_CMY << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMY_16_PLANAR   = ((PT_CMY << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	public static final int TYPE_CMY_16_SE       = ((PT_CMY << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	
	public static final int TYPE_CMYK_8          = ((PT_CMYK << COLORSPACE_SHIFT_VALUE) | (4 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMYKA_8         = ((PT_CMYK << COLORSPACE_SHIFT_VALUE) | (1 << EXTRA_SHIFT_VALUE) | (4 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMYK_8_REV      = ((PT_CMYK << COLORSPACE_SHIFT_VALUE) | (4 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << FLAVOR_SHIFT_VALUE));
	public static final int TYPE_YUVK_8          = TYPE_CMYK_8_REV;
	public static final int TYPE_CMYK_8_PLANAR   = ((PT_CMYK << COLORSPACE_SHIFT_VALUE) | (4 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	public static final int TYPE_CMYK_16         = ((PT_CMYK << COLORSPACE_SHIFT_VALUE) | (4 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMYK_16_REV     = ((PT_CMYK << COLORSPACE_SHIFT_VALUE) | (4 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << FLAVOR_SHIFT_VALUE));
	public static final int TYPE_YUVK_16         = TYPE_CMYK_16_REV;
	public static final int TYPE_CMYK_16_PLANAR  = ((PT_CMYK << COLORSPACE_SHIFT_VALUE) | (4 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	public static final int TYPE_CMYK_16_SE      = ((PT_CMYK << COLORSPACE_SHIFT_VALUE) | (4 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	
	public static final int TYPE_KYMC_8          = ((PT_CMYK << COLORSPACE_SHIFT_VALUE) | (4 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE));
	public static final int TYPE_KYMC_16         = ((PT_CMYK << COLORSPACE_SHIFT_VALUE) | (4 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE));
	public static final int TYPE_KYMC_16_SE      = ((PT_CMYK << COLORSPACE_SHIFT_VALUE) | (4 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	
	public static final int TYPE_KCMY_8          = ((PT_CMYK << COLORSPACE_SHIFT_VALUE) | (4 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << SWAPFIRST_SHIFT_VALUE));
	public static final int TYPE_KCMY_8_REV      = ((PT_CMYK << COLORSPACE_SHIFT_VALUE) | (4 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << FLAVOR_SHIFT_VALUE) | (1 << SWAPFIRST_SHIFT_VALUE));
	public static final int TYPE_KCMY_16         = ((PT_CMYK << COLORSPACE_SHIFT_VALUE) | (4 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << SWAPFIRST_SHIFT_VALUE));
	public static final int TYPE_KCMY_16_REV     = ((PT_CMYK << COLORSPACE_SHIFT_VALUE) | (4 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << FLAVOR_SHIFT_VALUE) | (1 << SWAPFIRST_SHIFT_VALUE));
	public static final int TYPE_KCMY_16_SE      = ((PT_CMYK << COLORSPACE_SHIFT_VALUE) | (4 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE) | (1 << SWAPFIRST_SHIFT_VALUE));
	
	public static final int TYPE_CMYK5_8         = ((PT_MCH5 << COLORSPACE_SHIFT_VALUE) | (5 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMYK5_16        = ((PT_MCH5 << COLORSPACE_SHIFT_VALUE) | (5 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMYK5_16_SE     = ((PT_MCH5 << COLORSPACE_SHIFT_VALUE) | (5 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	public static final int TYPE_KYMC5_8         = ((PT_MCH5 << COLORSPACE_SHIFT_VALUE) | (5 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE));
	public static final int TYPE_KYMC5_16        = ((PT_MCH5 << COLORSPACE_SHIFT_VALUE) | (5 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE));
	public static final int TYPE_KYMC5_16_SE     = ((PT_MCH5 << COLORSPACE_SHIFT_VALUE) | (5 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	public static final int TYPE_CMYK6_8         = ((PT_MCH6 << COLORSPACE_SHIFT_VALUE) | (6 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMYK6_8_PLANAR  = ((PT_MCH6 << COLORSPACE_SHIFT_VALUE) | (6 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	public static final int TYPE_CMYK6_16        = ((PT_MCH6 << COLORSPACE_SHIFT_VALUE) | (6 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMYK6_16_PLANAR = ((PT_MCH6 << COLORSPACE_SHIFT_VALUE) | (6 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	public static final int TYPE_CMYK6_16_SE     = ((PT_MCH6 << COLORSPACE_SHIFT_VALUE) | (6 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	public static final int TYPE_CMYK7_8         = ((PT_MCH7 << COLORSPACE_SHIFT_VALUE) | (7 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMYK7_16        = ((PT_MCH7 << COLORSPACE_SHIFT_VALUE) | (7 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMYK7_16_SE     = ((PT_MCH7 << COLORSPACE_SHIFT_VALUE) | (7 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	public static final int TYPE_KYMC7_8         = ((PT_MCH7 << COLORSPACE_SHIFT_VALUE) | (7 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE));
	public static final int TYPE_KYMC7_16        = ((PT_MCH7 << COLORSPACE_SHIFT_VALUE) | (7 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE));
	public static final int TYPE_KYMC7_16_SE     = ((PT_MCH7 << COLORSPACE_SHIFT_VALUE) | (7 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	public static final int TYPE_CMYK8_8         = ((PT_MCH8 << COLORSPACE_SHIFT_VALUE) | (8 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMYK8_16        = ((PT_MCH8 << COLORSPACE_SHIFT_VALUE) | (8 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMYK8_16_SE     = ((PT_MCH8 << COLORSPACE_SHIFT_VALUE) | (8 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	public static final int TYPE_KYMC8_8         = ((PT_MCH8 << COLORSPACE_SHIFT_VALUE) | (8 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE));
	public static final int TYPE_KYMC8_16        = ((PT_MCH8 << COLORSPACE_SHIFT_VALUE) | (8 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE));
	public static final int TYPE_KYMC8_16_SE     = ((PT_MCH8 << COLORSPACE_SHIFT_VALUE) | (8 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	public static final int TYPE_CMYK9_8         = ((PT_MCH9 << COLORSPACE_SHIFT_VALUE) | (9 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMYK9_16        = ((PT_MCH9 << COLORSPACE_SHIFT_VALUE) | (9 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMYK9_16_SE     = ((PT_MCH9 << COLORSPACE_SHIFT_VALUE) | (9 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	public static final int TYPE_KYMC9_8         = ((PT_MCH9 << COLORSPACE_SHIFT_VALUE) | (9 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE));
	public static final int TYPE_KYMC9_16        = ((PT_MCH9 << COLORSPACE_SHIFT_VALUE) | (9 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE));
	public static final int TYPE_KYMC9_16_SE     = ((PT_MCH9 << COLORSPACE_SHIFT_VALUE) | (9 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	public static final int TYPE_CMYK10_8        = ((PT_MCH10 << COLORSPACE_SHIFT_VALUE) | (10 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMYK10_16       = ((PT_MCH10 << COLORSPACE_SHIFT_VALUE) | (10 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMYK10_16_SE    = ((PT_MCH10 << COLORSPACE_SHIFT_VALUE) | (10 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	public static final int TYPE_KYMC10_8        = ((PT_MCH10 << COLORSPACE_SHIFT_VALUE) | (10 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE));
	public static final int TYPE_KYMC10_16       = ((PT_MCH10 << COLORSPACE_SHIFT_VALUE) | (10 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE));
	public static final int TYPE_KYMC10_16_SE    = ((PT_MCH10 << COLORSPACE_SHIFT_VALUE) | (10 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	public static final int TYPE_CMYK11_8        = ((PT_MCH11 << COLORSPACE_SHIFT_VALUE) | (11 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMYK11_16       = ((PT_MCH11 << COLORSPACE_SHIFT_VALUE) | (11 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMYK11_16_SE    = ((PT_MCH11 << COLORSPACE_SHIFT_VALUE) | (11 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	public static final int TYPE_KYMC11_8        = ((PT_MCH11 << COLORSPACE_SHIFT_VALUE) | (11 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE));
	public static final int TYPE_KYMC11_16       = ((PT_MCH11 << COLORSPACE_SHIFT_VALUE) | (11 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE));
	public static final int TYPE_KYMC11_16_SE    = ((PT_MCH11 << COLORSPACE_SHIFT_VALUE) | (11 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	public static final int TYPE_CMYK12_8        = ((PT_MCH12 << COLORSPACE_SHIFT_VALUE) | (12 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMYK12_16       = ((PT_MCH12 << COLORSPACE_SHIFT_VALUE) | (12 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMYK12_16_SE    = ((PT_MCH12 << COLORSPACE_SHIFT_VALUE) | (12 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	public static final int TYPE_KYMC12_8        = ((PT_MCH12 << COLORSPACE_SHIFT_VALUE) | (12 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE));
	public static final int TYPE_KYMC12_16       = ((PT_MCH12 << COLORSPACE_SHIFT_VALUE) | (12 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE));
	public static final int TYPE_KYMC12_16_SE    = ((PT_MCH12 << COLORSPACE_SHIFT_VALUE) | (12 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	
	// Colorimetric
	public static final int TYPE_XYZ_16          = ((PT_XYZ << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	public static final int TYPE_Lab_8           = ((PT_Lab << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE));
	public static final int TYPE_LabV2_8         = ((PT_LabV2 << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE));
	
	public static final int TYPE_ALab_8          = ((PT_Lab << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << EXTRA_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE));
	public static final int TYPE_ALabV2_8        = ((PT_LabV2 << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << EXTRA_SHIFT_VALUE) | (1 << DOSWAP_SHIFT_VALUE));
	public static final int TYPE_Lab_16          = ((PT_Lab << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	public static final int TYPE_LabV2_16        = ((PT_LabV2 << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	public static final int TYPE_Yxy_16          = ((PT_Yxy << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	
	// YCbCr
	public static final int TYPE_YCbCr_8         = ((PT_YCbCr << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE));
	public static final int TYPE_YCbCr_8_PLANAR  = ((PT_YCbCr << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	public static final int TYPE_YCbCr_16        = ((PT_YCbCr << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	public static final int TYPE_YCbCr_16_PLANAR = ((PT_YCbCr << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	public static final int TYPE_YCbCr_16_SE     = ((PT_YCbCr << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	
	// YUV
	public static final int TYPE_YUV_8           = ((PT_YUV << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE));
	public static final int TYPE_YUV_8_PLANAR    = ((PT_YUV << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	public static final int TYPE_YUV_16          = ((PT_YUV << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	public static final int TYPE_YUV_16_PLANAR   = ((PT_YUV << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	public static final int TYPE_YUV_16_SE       = ((PT_YUV << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	
	// HLS
	public static final int TYPE_HLS_8           = ((PT_HLS << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE));
	public static final int TYPE_HLS_8_PLANAR    = ((PT_HLS << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	public static final int TYPE_HLS_16          = ((PT_HLS << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	public static final int TYPE_HLS_16_PLANAR   = ((PT_HLS << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	public static final int TYPE_HLS_16_SE       = ((PT_HLS << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	
	// HSV
	public static final int TYPE_HSV_8           = ((PT_HSV << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE));
	public static final int TYPE_HSV_8_PLANAR    = ((PT_HSV << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (1 << BYTES_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	public static final int TYPE_HSV_16          = ((PT_HSV << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	public static final int TYPE_HSV_16_PLANAR   = ((PT_HSV << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << PLANAR_SHIFT_VALUE));
	public static final int TYPE_HSV_16_SE       = ((PT_HSV << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE) | (1 << ENDIAN16_SHIFT_VALUE));
	
	// Named color index. Only 16 bits allowed (don't check colorspace)
	public static final int TYPE_NAMED_COLOR_INDEX = ((1 << CHANNELS_SHIFT_VALUE) | (2 << BYTES_SHIFT_VALUE));
	
	// Float formatters.
	public static final int TYPE_XYZ_FLT        = ((1 << FLOAT_SHIFT_VALUE) | (PT_XYZ << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (4 << BYTES_SHIFT_VALUE));
	public static final int TYPE_Lab_FLT        = ((1 << FLOAT_SHIFT_VALUE) | (PT_Lab << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (4 << BYTES_SHIFT_VALUE));
	public static final int TYPE_GRAY_FLT       = ((1 << FLOAT_SHIFT_VALUE) | (PT_GRAY << COLORSPACE_SHIFT_VALUE) | (1 << CHANNELS_SHIFT_VALUE) | (4 << BYTES_SHIFT_VALUE));
	public static final int TYPE_RGB_FLT        = ((1 << FLOAT_SHIFT_VALUE) | (PT_RGB << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (4 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMYK_FLT       = ((1 << FLOAT_SHIFT_VALUE) | (PT_CMYK << COLORSPACE_SHIFT_VALUE) | (4 << CHANNELS_SHIFT_VALUE) | (4 << BYTES_SHIFT_VALUE));
	
	// Floating point formatters.  
	// NOTE THAT 'BYTES' FIELD IS SET TO ZERO ON DLB because 8 bytes overflows the bitfield
	public static final int TYPE_XYZ_DBL        = ((1 << FLOAT_SHIFT_VALUE) | (PT_XYZ << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (0 << BYTES_SHIFT_VALUE));
	public static final int TYPE_Lab_DBL        = ((1 << FLOAT_SHIFT_VALUE) | (PT_Lab << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (0 << BYTES_SHIFT_VALUE));
	public static final int TYPE_GRAY_DBL       = ((1 << FLOAT_SHIFT_VALUE) | (PT_GRAY << COLORSPACE_SHIFT_VALUE) | (1 << CHANNELS_SHIFT_VALUE) | (0 << BYTES_SHIFT_VALUE));
	public static final int TYPE_RGB_DBL        = ((1 << FLOAT_SHIFT_VALUE) | (PT_RGB << COLORSPACE_SHIFT_VALUE) | (3 << CHANNELS_SHIFT_VALUE) | (0 << BYTES_SHIFT_VALUE));
	public static final int TYPE_CMYK_DBL       = ((1 << FLOAT_SHIFT_VALUE) | (PT_CMYK << COLORSPACE_SHIFT_VALUE) | (4 << CHANNELS_SHIFT_VALUE) | (0 << BYTES_SHIFT_VALUE));
    
	// Colorspaces
	public static class cmsCIEXYZ
	{
		public double X;
		public double Y;
		public double Z;
	}
	
	public static class cmsCIExyY
	{
		public double x;
		public double y;
		public double Y;
	}
	
	public static class cmsCIELab
	{
		public double L;
		public double a;
		public double b;
	}
	
	public static class cmsCIELCh
	{
		public double L;
		public double C;
		public double h;
	}
	
	public static class cmsJCh
	{
		public double J;
		public double C;
		public double h;
	}
	
	public static class cmsCIEXYZTRIPLE
	{
		public cmsCIEXYZ Red;
		public cmsCIEXYZ Green;
		public cmsCIEXYZ Blue;
	}
	
	public static class cmsCIExyYTRIPLE
	{
		public cmsCIExyY Red;
		public cmsCIExyY Green;
		public cmsCIExyY Blue;
	}
	
	// Illuminant types for structs below
	public static final int cmsILLUMINANT_TYPE_UNKNOWN=0x0000000;
	public static final int cmsILLUMINANT_TYPE_D50   = 0x0000001;
	public static final int cmsILLUMINANT_TYPE_D65   = 0x0000002;
	public static final int cmsILLUMINANT_TYPE_D93   = 0x0000003;
	public static final int cmsILLUMINANT_TYPE_F2    = 0x0000004;
	public static final int cmsILLUMINANT_TYPE_D55   = 0x0000005;
	public static final int cmsILLUMINANT_TYPE_A     = 0x0000006;
	public static final int cmsILLUMINANT_TYPE_E     = 0x0000007;
	public static final int cmsILLUMINANT_TYPE_F8    = 0x0000008;
	
	public static class cmsICCMeasurementConditions
	{
		/** 0 = unknown, 1=CIE 1931, 2=CIE 1964*/
		public int Observer;
		/** Value of backing*/
        public cmsCIEXYZ Backing;
        /** 0=unknown, 1=45/0, 0/45 2=0d, d/0*/
        public int Geometry;
        /** 0..1.0*/
        public double Flare;
        public int IlluminantType;
	}
	
	public static class cmsICCViewingConditions
	{
		/** Not the same struct as CAM02,*/
		public cmsCIEXYZ IlluminantXYZ;
		/** This is for storing the tag*/
		public cmsCIEXYZ SurroundXYZ;
        /** viewing condition*/
        public int IlluminantType;
	}
	
	// Support of non-standard functions --------------------------------------------------------------------------------------
	
	public static int cmsstrcasecmp(final String s1, final String s2)
	{
		return cmsplugin.cmsstrcasecmp(s1, s2);
	}
	
	public static long cmsfilelength(ContentConnection f) //ContentConnection is the most likley to work and also has been around for a long time so software updates are not needed to get it to work.
	{
		return cmsplugin.cmsfilelength(f);
	}
	
	// Plug-In registering  ---------------------------------------------------------------------------------------------------
	
	/**
	 * Declares external extensions to the core engine. The "Plugin" parameter may hold one or several plug-ins, as defined by the plug-in developer.
	 * @param Plugin Pointer to plug-in collection.
	 * @return TRUE on success FALSE on error.
	 */
	public static boolean cmsPlugin(Object Plugin)
	{
		return cmsplugin.cmsPlugin(Plugin);
	}
	
	/**
	 * This function returns Little CMS to its default state, as no plug-ins were declared. There is no way to unregister a single plug-in, as a single call to 
	 * {@link #cmsPlugin(Object)} function may register many different plug-ins simultaneously, then there is no way to identify which plug-in to unregister.
	 */
	public static void cmsUnregisterPlugins()
	{
		cmsplugin.cmsUnregisterPlugins();
	}
	
	// Error logging ----------------------------------------------------------------------------------------------------------
	
	// There is no error handling at all. When a function fails, it returns proper value.
	// For example, all create functions does return NULL on failure. Other may return FALSE.
	// It may be interesting, for the developer, to know why the function is failing.
	// for that reason, lcms2 does offer a logging function. This function will get
	// an ENGLISH string with some clues on what is going wrong. You can show this
	// info to the end user if you wish, or just create some sort of log on disk.
	// The logging function should NOT terminate the program, as this obviously can leave
	// unfreed resources. It is the programmer's responsibility to check each function
	// return code to make sure it didn't fail.
	
	public static final int cmsERROR_UNDEFINED                  = 0;
	public static final int cmsERROR_FILE                       = 1;
	public static final int cmsERROR_RANGE                      = 2;
	public static final int cmsERROR_INTERNAL                   = 3;
	public static final int cmsERROR_NULL                       = 4;
	public static final int cmsERROR_READ                       = 5;
	public static final int cmsERROR_SEEK                       = 6;
	public static final int cmsERROR_WRITE                      = 7;
	public static final int cmsERROR_UNKNOWN_EXTENSION          = 8;
	public static final int cmsERROR_COLORSPACE_CHECK           = 9;
	public static final int cmsERROR_ALREADY_DEFINED            = 10;
	public static final int cmsERROR_BAD_SIGNATURE              = 11;
	public static final int cmsERROR_CORRUPTION_DETECTED        = 12;
	public static final int cmsERROR_NOT_SUITABLE               = 13;
	
	// Error logger is called with the ContextID when a message is raised. This gives the
	// chance to know which thread is responsible of the warning and any environment associated
	// with it. Non-multithreading applications may safely ignore this parameter.
	// Note that under certain special circumstances, ContextID may be NULL.
	public static interface cmsLogErrorHandlerFunction
	{
		public void run(cmsContext ContextID, int ErrorCode, final String Text);
	}
	
	// Allows user to set any specific logger
	/**
	 * Allows user to set any specific logger. Each time this function is called, the previous logger is replaced. Calling this functin with NULL as parameter, does 
	 * reset the logger to the default Little CMS logger. The default Little CMS logger does nothing.
	 * @param Fn Callback to the logger (user defined function), or NULL to reset Little CMS to its default logger.
	 */
	public static void cmsSetLogErrorHandler(cmsLogErrorHandlerFunction Fn)
	{
		cmserr.cmsSetLogErrorHandler(Fn);
	}
	
	// Conversions --------------------------------------------------------------------------------------------------------------
	
	// Returns pointers to constant structs
	/**
	 * Returns pointer to constant structures. Pointer to constant D50 white point in XYZ space.
	 */
	public static final cmsCIEXYZ cmsD50_XYZ = cmswtpnt.cmsD50_XYZ();
	/**
	 * Returns pointer to constant structures. Pointer to constant D50 white point in xyY space.
	 */
	public static final cmsCIExyY cmsD50_xyY = cmswtpnt.cmsD50_xyY();
	
	// Colorimetric space conversions
	/**
	 * Colorimetric space conversions.
	 * @param Dest Destination value.
	 * @param Source Source value.
	 */
	public static void cmsXYZ2xyY(cmsCIExyY Dest, final cmsCIEXYZ Source)
	{
		cmspcs.cmsXYZ2xyY(Dest, Source);
	}
	
	/**
	 * Colorimetric space conversions.
	 * @param Dest Destination value.
	 * @param Source Source value.
	 */
	public static void cmsxyY2XYZ(cmsCIEXYZ Dest, final cmsCIExyY Source)
	{
		cmspcs.cmsxyY2XYZ(Dest, Source);
	}
	
	/**
	 * Colorimetric space conversions. Setting WhitePoint to NULL forces D50 as white point.
	 * @param Lab Pointer to a cmsCIELab value
	 * @param xyz Pointer to a cmsCIEXYZ value
	 */
	public static void cmsXYZ2Lab(final cmsCIEXYZ WhitePoint, cmsCIELab Lab, final cmsCIEXYZ xyz)
	{
		cmspcs.cmsXYZ2Lab(WhitePoint, Lab, xyz);
	}
	
	/**
	 * Colorimetric space conversions. Setting WhitePoint to NULL forces D50 as white point.
	 * @param Lab Pointer to a cmsCIELab value
	 * @param xyz Pointer to a cmsCIEXYZ value
	 */
	public static void cmsLab2XYZ(final cmsCIEXYZ WhitePoint, cmsCIEXYZ xyz, final cmsCIELab Lab)
	{
		cmspcs.cmsLab2XYZ(WhitePoint, xyz, Lab);
	}
	
	/**
	 * Colorimetric space conversions.
	 * @param LCh Pointer to a cmsCIELab value
	 * @param Lab Pointer to a cmsCIELCh value
	 */
	public static void cmsLab2LCh(cmsCIELCh LCh, final cmsCIELab Lab)
	{
		cmspcs.cmsLab2LCh(LCh, Lab);
	}
	
	/**
	 * Colorimetric space conversions.
	 * @param LCh Pointer to a cmsCIELab value
	 * @param Lab Pointer to a cmsCIELCh value
	 */
	public static void cmsLCh2Lab(cmsCIELab Lab, final cmsCIELCh LCh)
	{
		cmspcs.cmsLCh2Lab(Lab, LCh);
	}
	
	// Encoding /Decoding on PCS
	/**
	 * Decodes a Lab value, encoded on ICC v4 convention to a cmsCIELab value
	 * @param Lab Pointer to a cmsCIELab value
	 * @param wLab Array of 3 <code>short</code> holding the encoded values.
	 */
	public static void cmsLabEncoded2Float(cmsCIELab Lab, final short[] wLab)
	{
		cmspcs.cmsLabEncoded2Float(Lab, wLab);
	}
	
	/**
	 * Decodes a Lab value, encoded on ICC v2 convention to a cmsCIELab value
	 * @param Lab Pointer to a cmsCIELab value
	 * @param wLab Array of 3 <code>short</code> holding the encoded values.
	 */
	public static void cmsLabEncoded2FloatV2(cmsCIELab Lab, final short[] wLab)
	{
		cmspcs.cmsLabEncoded2FloatV2(Lab, wLab);
	}
	
	/**
	 * Encodes a Lab value, from a cmsCIELab value
	 * @param Lab Pointer to a cmsCIELab value
	 * @param wLab Array of 3 short to hold the encoded values.
	 */
	public static void cmsFloat2LabEncoded(short[] wLab, final cmsCIELab Lab)
	{
		cmspcs.cmsFloat2LabEncoded(wLab, Lab);
	}
	
	/**
	 * Encodes a Lab value, from a cmsCIELab value as described in Table 13, to ICC v2 convention.
	 * @param Lab Pointer to a cmsCIELab value
	 * @param wLab Array of 3 short to hold the encoded values.
	 */
	public static void cmsFloat2LabEncodedV2(short[] wLab, final cmsCIELab Lab)
	{
		cmspcs.cmsFloat2LabEncodedV2(wLab, Lab);
	}
	
	/**
	 * Decodes a XYZ value, encoded on ICC convention to a cmsCIEXYZ value
	 * @param fxyz Pointer to a cmsCIEXYZ value
	 * @param XYZ Array of 3 short holding the encoded values.
	 */
	public static void cmsXYZEncoded2Float(cmsCIEXYZ fxyz, final short[] XYZ)
	{
		cmspcs.cmsXYZEncoded2Float(fxyz, XYZ);
	}
	
	/**
	 * Encodes a XYZ value, from a cmsCIELab value
	 * @param XYZ Array of 3 short to hold the encoded values.
	 * @param fXYZ Pointer to a cmsCIEXYZ value
	 */
	public static void cmsFloat2XYZEncoded(short[] XYZ, final cmsCIEXYZ fXYZ)
	{
		cmspcs.cmsFloat2XYZEncoded(XYZ, fXYZ);
	}
	
	// DeltaE metrics
	/**
	 * The L*a*b* color space was devised in 1976 and, at the same time delta-E 1976 (dE76) came into being. If you can imagine attaching a string to a color point in 
	 * 3D Lab space, dE76 describes the sphere that is described by all the possible directions you could pull the string. If you hear people speak of just plain 
	 * 'delta-E' they are probably referring to dE76. It is also known as dE-Lab and dE-ab. One problem with dE76 is that Lab itself is not 'perceptually uniform' as 
	 * its creators had intended. So different amounts of visual color shift in different color areas of Lab might have the same dE76 number. Conversely, the same 
	 * amount of color shift might result in different dE76 values. Another issue is that the eye is most sensitive to hue differences, then chroma and finally 
	 * lightness and dE76 does not take this into account.
	 * @param Lab1 Pointer to first cmsCIELab value
	 * @param Lab2 Pointer to second cmsCIELab value
	 * @return The dE76 metric value.
	 */
	public static double cmsDeltaE(final cmsCIELab Lab1, final cmsCIELab Lab2)
	{
		return cmspcs.cmsDeltaE(Lab1, Lab2);
	}
	
	/**
	 * A technical committee of the CIE (TC1-29) published an equation in 1995 called CIE94. The equation is similar to CMC but the weighting functions are largely 
	 * based on RIT/DuPont tolerance data derived from automotive paint experiments where sample surfaces are smooth. It also has ratios, labeled kL (lightness) and 
	 * Kc (chroma) and the commercial factor (cf) but these tend to be preset in software and are not often exposed for the user (as it is the case in Little CMS).
	 * @param Lab1 Pointer to first cmsCIELab value
	 * @param Lab2 Pointer to second cmsCIELab value
	 * @return The CIE94 dE metric value.
	 */
	public static double cmsCIE94DeltaE(final cmsCIELab Lab1, final cmsCIELab Lab2)
	{
		return cmspcs.cmsCIE94DeltaE(Lab1, Lab2);
	}
	
	/**
	 * BFD delta E metric.
	 * @param Lab1 Pointer to first cmsCIELab value
	 * @param Lab2 Pointer to second cmsCIELab value
	 * @return The dE BFD metric value.
	 */
	public static double cmsBFDdeltaE(final cmsCIELab Lab1, final cmsCIELab Lab2)
	{
		return cmspcs.cmsBFDdeltaE(Lab1, Lab2);
	}
	
	/**
	 * In 1984 the CMC (Colour Measurement Committee of the Society of Dyes and Colourists of Great Britain) developed and adopted an equation based on LCH numbers. 
	 * Intended for the textiles industry, CMC l:c allows the setting of lightness (l) and chroma (c) factors. As the eye is more sensitive to chroma, the 
	 * default ratio for l:c is 2:1 allowing for 2x the difference in lightness than chroma (numbers). There is also a 'commercial factor' (cf) which allows an 
	 * overall varying of the size of the tolerance region according to accuracy requirements. A cf=1.0 means that a delta-E CMC value <1.0 is acceptable.
	 * <p>
	 * CMC l:c is designed to be used with D65 and the CIE Supplementary Observer. Commonly-used values for l:c are 2:1 for acceptability and 1:1 for the threshold of 
	 * imperceptibility.
	 * @param Lab1 Pointer to first cmsCIELab value
	 * @param Lab2 Pointer to second cmsCIELab value
	 * @return The dE CMC metric value.
	 */
	public static double cmsCMCdeltaE(final cmsCIELab Lab1, final cmsCIELab Lab2, double l, double c)
	{
		return cmspcs.cmsCMCdeltaE(Lab1, Lab2, l, c);
	}
	
	/**
	 * Delta-E 2000 is the first major revision of the dE94 equation. Unlike dE94, which assumes that L* correctly reflects the perceived differences in lightness, 
	 * dE2000 varies the weighting of L* depending on where in the lightness range the color falls. dE2000 is still under consideration and does not seem to be widely 
	 * supported in graphics arts applications.
	 * @param Lab1 Pointer to first cmsCIELab value
	 * @param Lab2 Pointer to second cmsCIELab value
	 * @return The CIE2000 dE metric value.
	 */
	public static double cmsCIE2000DeltaE(final cmsCIELab Lab1, final cmsCIELab Lab2, double Kl, double Kc, double Kh)
	{
		return cmspcs.cmsCIE2000DeltaE(Lab1, Lab2, Kl, Kc, Kh);
	}
	
	// Temperature <-> Chromaticity (Black body)
	/**
	 * Correlates a black body chromaticity from given temperature in ºK. Valid range is 4000K-25000K.
	 * @param WhitePoint Pointer to a user-allocated cmsCIExyY variable to receive the resulting chromaticity.
	 * @param TempK Temperature in ºK
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean cmsWhitePointFromTemp(cmsCIExyY WhitePoint, double TempK)
	{
		return cmswtpnt.cmsWhitePointFromTemp(WhitePoint, TempK);
	}
	
	/**
	 * 
	 * @param TempK Pointer to a user-allocated cmsFloat64Number variable to receive the resulting temperature.
	 * @param WhitePoint Target chromaticity in cmsCIExyY
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean cmsTempFromWhitePoint(double[] TempK, final cmsCIExyY WhitePoint)
	{
		return cmswtpnt.cmsTempFromWhitePoint(TempK, WhitePoint);
	}
	
	// Chromatic adaptation
	public static boolean cmsAdaptToIlluminant(cmsCIEXYZ Result, final cmsCIEXYZ SourceWhitePt, final cmsCIEXYZ Illuminant, final cmsCIEXYZ Value)
	{
		return cmswtpnt.cmsAdaptToIlluminant(Result, SourceWhitePt, Illuminant, Value);
	}
	
	// CIECAM02 ---------------------------------------------------------------------------------------------------

	// Viewing conditions. Please note those are CAM model viewing conditions, and not the ICC tag viewing
	// conditions, which I'm naming cmsICCViewingConditions to make differences evident. Unfortunately, the tag
	// cannot deal with surround La, Yb and D value so is basically useless to store CAM02 viewing conditions.
	
	
	public static final int AVG_SURROUND     = 1;
	public static final int DIM_SURROUND     = 2;
	public static final int DARK_SURROUND    = 3;
	public static final int CUTSHEET_SURROUND= 4;
	
	public static final int D_CALCULATE      = (-1);
	
	public static class cmsViewingConditions
	{
		public cmsCIEXYZ whitePoint;
		public double Yb;
		public double La;
		public int surround;
		public double D_value;
	}
	
	/**
	 * Does create a CAM02 object based on given viewing conditions. Such object may be used as a color appearance model and evaluated in forward and reverse 
	 * directions. Degree of chromatic adaptation (d), can be specified in 0...1.0 range, or the model can be instructed to calculate it by using D_CALCULATE constant (-1).
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param pVC Pointer to a structure holding viewing conditions
	 * @return Handle to CAM02 object or NULL on error.
	 */
	public static cmsHANDLE cmsCIECAM02Init(cmsContext ContextID, final cmsViewingConditions pVC)
	{
		return cmscam02.cmsCIECAM02Init(ContextID, pVC);
	}
	
	/**
	 * Terminates a CAM02 object, freeing all involved resources.
	 * @param hModel Handle to a CAM02 object
	 */
	public static void cmsCIECAM02Done(cmsHANDLE hModel)
	{
		cmscam02.cmsCIECAM02Done(hModel);
	}
	
	/**
	 * Evaluates the CAM02 model in the forward direction XYZ -> JCh
	 * @param hModel Handle to a CAM02 object
	 * @param pIn Points to the input XYZ value
	 * @param pOut Points to the output JCh value
	 */
	public static void cmsCIECAM02Forward(cmsHANDLE hModel, final cmsCIEXYZ pIn, cmsJCh pOut)
	{
		cmscam02.cmsCIECAM02Forward(hModel, pIn, pOut);
	}
	
	/**
	 * Evaluates the CAM02 model in the reverse direction JCh -> XYZ
	 * @param hModel Handle to a CAM02 object
	 * @param pIn Points to the input JCh value
	 * @param pOut Points to the output XYZ value
	 */
	public static void cmsCIECAM02Reverse(cmsHANDLE hModel, final cmsJCh pIn, cmsCIEXYZ pOut)
	{
		cmscam02.cmsCIECAM02Reverse(hModel, pIn, pOut);
	}
	
	// Tone curves -----------------------------------------------------------------------------------------
	
	// This describes a curve segment. For a table of supported types, see the manual. User can increase the number of
	// available types by using a proper plug-in. Parametric segments allow 10 parameters at most
	
	public static class cmsCurveSegment
	{
		/** Domain; for x0 < x <= x1*/
		public float x0, x1;
		/** Parametric type, Type == 0 means sampled segment. Negative values are reserved*/
		public int Type;
		/** Parameters if Type != 0*/
		public final double[] Params;
		/** Number of grid points if Type == 0*/
		public int nGridPoints;
		/** Points to an array of floats if Type == 0*/
		public float[] SampledPoints;
		
		public cmsCurveSegment()
		{
			this.Params = new double[10];
			this.SampledPoints = null;
		}
	}
	
	// The internal representation is none of your business.
	//IN: lcms2_internal.java
	
	/**
	 * Builds a tone curve from given segment information.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param nSegments Number of segments
	 * @param Segments Array of cmsCurveSegment
	 * @return Pointer to a newly created tone curve object on success, NULL on error.
	 */
	public static cmsToneCurve cmsBuildSegmentedToneCurve(cmsContext ContextID, int nSegments, final cmsCurveSegment[] Segments)
	{
		return cmsgamma.cmsBuildSegmentedToneCurve(ContextID, nSegments, Segments);
	}
	
	/**
	 * Builds a parametric tone curve
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param Type Number of parametric tone curve
	 * @param Params Array of tone curve parameters
	 * @return Pointer to a newly created tone curve object on success, NULL on error.
	 */
	public static cmsToneCurve cmsBuildParametricToneCurve(cmsContext ContextID, int Type, final double[] Params)
	{
		return cmsgamma.cmsBuildParametricToneCurve(ContextID, Type, Params);
	}
	
	/**
	 * Simplified wrapper to cmsBuildParametricToneCurve. Builds a parametric curve of type 1.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param Gamma Value of gamma exponent
	 * @return Pointer to a newly created tone curve object on success, NULL on error.
	 */
	public static cmsToneCurve cmsBuildGamma(cmsContext ContextID, double Gamma)
	{
		return cmsgamma.cmsBuildGamma(ContextID, Gamma);
	}
	
	/**
	 * Builds a tone curve based on a table of 16-bit values. Tone curves built with this function are restricted to 0…1.0 domain.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param nEntries Number of sample points
	 * @param values Array of samples. Domain is 0…65535 unsigned.
	 * @return Pointer to a newly created tone curve object on success, NULL on error.
	 */
	public static cmsToneCurve cmsBuildTabulatedToneCurve16(cmsContext ContextID, int nEntries, final short[] values)
	{
		return cmsgamma.cmsBuildTabulatedToneCurve16(ContextID, nEntries, values);
	}
	
	/**
	 * Builds a tone curve based on a table of floating point values. Tone curves built with this function are not restricted to 0…1.0 domain.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param nEntries Number of sample points
	 * @param values Array of samples. Domain of samples is 0…1.0
	 * @return Pointer to a newly created tone curve object on success, NULL on error.
	 */
	public static cmsToneCurve cmsBuildTabulatedToneCurveFloat(cmsContext ContextID, int nEntries, final float[] values)
	{
		return cmsgamma.cmsBuildTabulatedToneCurveFloat(ContextID, nEntries, values);
	}
	
	/**
	 * Destroys a tone curve object, freeing any associated resource.
	 * @param Curve pointer to a tone curve object.
	 */
	public static void cmsFreeToneCurve(cmsToneCurve Curve)
	{
		cmsgamma.cmsFreeToneCurve(Curve);
	}
	
	/**
	 * Destroys tree tone curve object placed into an array. This function is equivalent to call three times cmsFreeToneCurve, one per object. It exists because 
	 * conveniency.
	 * @param Curve array to 3 pointers to tone curve objects.
	 */
	public static void cmsFreeToneCurveTriple(cmsToneCurve[] Curve)
	{
		cmsgamma.cmsFreeToneCurve(Curve);
	}
	
	/**
	 * Duplicates a tone curve object, and all associated resources.
	 * @param Src pointer to a tone curve object.
	 * @return Pointer to a newly created tone curve object on success, NULL on error.
	 */
	public static cmsToneCurve cmsDupToneCurve(final cmsToneCurve Src)
	{
		return cmsgamma.cmsDupToneCurve(Src);
	}
	
	/**
	 * Creates a tone curve that is the inverse of given tone curve.
	 * @param InGamma pointer to a tone curve object.
	 * @return Pointer to a newly created tone curve object on success, NULL on error.
	 */
	public static cmsToneCurve cmsReverseToneCurve(final cmsToneCurve InGamma)
	{
		return cmsgamma.cmsReverseToneCurve(InGamma);
	}
	
	/**
	 * Creates a tone curve that is the inverse of given tone curve. In the case it couldn’t be analytically reversed, a tablulated curve of nResultSamples is created.
	 * @param nResultSamples Number of samples to use in the case origin tone curve couldn’t be analytically reversed
	 * @param InGamma pointer to a tone curve object.
	 * @return Pointer to a newly created tone curve object on success, NULL on error.
	 */
	public static cmsToneCurve cmsReverseToneCurveEx(int nResultSamples, final cmsToneCurve InGamma)
	{
		return cmsgamma.cmsReverseToneCurveEx(nResultSamples, InGamma);
	}
	public static cmsToneCurve cmsJoinToneCurve(cmsContext ContextID, final cmsToneCurve X,  final cmsToneCurve Y, int nPoints);
	public static boolean cmsSmoothToneCurve(cmsToneCurve Tab, double lambda);
	public static float cmsEvalToneCurveFloat(final cmsToneCurve Curve, float v);
	public static short cmsEvalToneCurve16(final cmsToneCurve Curve, short v);
	public static boolean cmsIsToneCurveMultisegment(final cmsToneCurve InGamma);
	public static boolean cmsIsToneCurveLinear(final cmsToneCurve Curve);
	public static boolean cmsIsToneCurveMonotonic(final cmsToneCurve t);
	public static boolean cmsIsToneCurveDescending(final cmsToneCurve t);
	public static int cmsGetToneCurveParametricType(final cmsToneCurve t);
	public static double cmsEstimateGamma(final cmsToneCurve t, double Precision);
	//IN: cmsgamma.java
	
	// Implements pipelines of multi-processing elements -------------------------------------------------------------
	
	// Nothing to see here, move along
	//IN: lcms2_internal.java
	
	// Those are hi-level pipelines
	//TODO
	public static cmsPipeline cmsPipelineAlloc(cmsContext ContextID, int InputChannels, int OutputChannels);
	public static void cmsPipelineFree(cmsPipeline lut);
	public static cmsPipeline cmsPipelineDup(final cmsPipeline Orig);
	
	public static int cmsPipelineInputChannels(final cmsPipeline lut);
	public static int cmsPipelineOutputChannels(final cmsPipeline lut);
	
	public static int cmsPipelineStageCount(final cmsPipeline lut);
	public static cmsStage cmsPipelineGetPtrToFirstStage(final cmsPipeline lut);
	public static cmsStage cmsPipelineGetPtrToLastStage(final cmsPipeline lut);
	
	public static void cmsPipelineEval16(final short[] In, short[] Out, final cmsPipeline lut);
	public static void cmsPipelineEvalFloat(final float[] In, float[] Out, final cmsPipeline lut);
	public static boolean cmsPipelineEvalReverseFloat(float[] Target, float[] Result, float[] Hint, final cmsPipeline lut);
	public static boolean cmsPipelineCat(cmsPipeline l1, final cmsPipeline l2);
	public static boolean cmsPipelineSetSaveAs8bitsFlag(cmsPipeline lut, boolean On);
	//IN: cmslut.java
	
	// Where to place/locate the stages in the pipeline chain
	public static final int cmsAT_BEGIN = 0;
	public static final int cmsAT_END = cmsAT_BEGIN + 1;
	
	//TODO
	public static void cmsPipelineInsertStage(cmsPipeline lut, cmsStageLoc loc, cmsStage mpe);
	public static void cmsPipelineUnlinkStage(cmsPipeline lut, cmsStageLoc loc, cmsStage[] mpe);
	//IN: cmslut.java
	
	// This function is quite useful to analyze the structure of a Pipeline and retrieve the Stage elements
	// that conform the Pipeline. It should be called with the Pipeline, the number of expected elements and
	// then a list of expected types followed with a list of double pointers to Stage elements. If
	// the function founds a match with current pipeline, it fills the pointers and returns TRUE
	// if not, returns FALSE without touching anything.
	/**
	 * This function is quite useful to analyze the structure of a Pipeline and retrieve the Stage elements that conform the Pipeline. It should be called with the 
	 * Pipeline, the number of expected stages and then a list of expected types followed with a list of double pointers to Stage elements. If the function founds a 
	 * match with current pipeline, it fills the pointers and returns TRUE if not, returns FALSE without touching anything.
	 * @param Lut Pointer to a pipeline object.
	 * @param n Number of expected stages
	 * @param stages list of types followed by a list of pointers to variables to receive pointers to stage elements
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean cmsPipelineCheckAndRetreiveStages(final cmsPipeline Lut, int n, Object[] stages)
	{
		return cmslut.cmsPipelineCheckAndRetreiveStages(Lut, n, stages);
	}
	
	// Matrix has double precision and CLUT has only float precision. That is because an ICC profile can encode
	// matrices with far more precision that CLUTS
	//TODO
	public static cmsStage cmsStageAllocIdentity(cmsContext ContextID, int nChannels);
	public static cmsStage cmsStageAllocToneCurves(cmsContext ContextID, int nChannels, final cmsToneCurve[] Curves);
	public static cmsStage cmsStageAllocMatrix(cmsContext ContextID, int Rows, int Cols, final double[] Matrix, final double[] Offset);
	
	public static cmsStage cmsStageAllocCLut16bit(cmsContext ContextID, int nGridPoints, int inputChan, int outputChan, final short[] Table);
	public static cmsStage cmsStageAllocCLutFloat(cmsContext ContextID, int nGridPoints, int inputChan, int outputChan, final float[] Table);
	
	public static cmsStage cmsStageAllocCLut16bitGranular(cmsContext ContextID, final int clutPoints[], int inputChan, int outputChan, final short[] Table);
	public static cmsStage cmsStageAllocCLutFloatGranular(cmsContext ContextID, final int clutPoints[], int inputChan, int outputChan, final float[] Table);
	
	public static cmsStage cmsStageDup(cmsStage mpe);
	public static void cmsStageFree(cmsStage mpe);
	public static cmsStage cmsStageNext(final cmsStage mpe);
	
	public static int cmsStageInputChannels(final cmsStage mpe);
	public static int cmsStageOutputChannels(final cmsStage mpe);
	public static int cmsStageType(final cmsStage mpe);
	public static Object cmsStageData(final cmsStage mpe);
	//IN: cmslut.java
	
    //TODO: #1149
	
	/**
	 * Similar to cmsReadTag, but different in two important aspects. 1st, ther memory is not owned by the profile, but for you, so you have to allocate the neccesary 
	 * amount of memory. To know the size, pass NULL as buffer and 0 as buffer size. The function returns the number of needed bytes.
	 * <p>
	 * The second important point is, this is raw data. No processing is performed, so you can effectively read wrong or broken profiles with this function.
	 * Obviously, then you have to interpret all those bytes!
	 * @param hProfile Handle to a profile object
	 * @param sig Signature of tag to be read
	 * @param data Points to a memory block to hold the result.
	 * @param BufferSize Size of memory buffer in bytes
	 * @return Number of bytes readed.
	 */
	public static int cmsReadRawTag(cmsHPROFILE hProfile, int sig, byte[] data, int BufferSize)
	{
		return cmsio0.cmsReadRawTag(hProfile, sig, data, BufferSize);
	}
	
	/**
	 * Returns channel count for a given color space.
	 * @param ColorSpace Any supported colorspace.
	 * @return Number of channels, or 3 on error.
	 */
	public static int cmsChannelsOf(int ColorSpace)
	{
		return cmspcs.cmsChannelsOf(ColorSpace);
	}
	
	/**
	 * Opens an ICC profile which is entirely contained in a memory block. Useful for accessing embedded profiles. MemPtr must point to a buffer of at least dwSize
	 *  bytes. This buffer must hold a full profile image. Memory must be contiguous.
	 * @param MemPtr Points to a block of contiguous memory containing the profile
	 * @param dwSize Profile's size measured in bytes.
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsOpenProfileFromMem(final byte[] MemPtr, int dwSize)
	{
		return cmsio0.cmsOpenProfileFromMem(MemPtr, dwSize);
	}
	
	/**
	 * This function translates bitmaps according of parameters setup when creating the color transform.
	 * @param Transform Handle to a color transform object.
	 * @param InputBuffer A pointer to the input bitmap
	 * @param OutputBuffer A pointer to the output bitmap.
	 * @param Size the number of PIXELS to be transformed.
	 */
	public static void cmsDoTransform(cmsHTRANSFORM Transform, final Object InputBuffer, Object OutputBuffer, int Size)
	{
		cmsxform.cmsDoTransform(Transform, InputBuffer, OutputBuffer, Size);
	}
}
