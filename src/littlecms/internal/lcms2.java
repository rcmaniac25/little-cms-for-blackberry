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

import java.util.Calendar;

import littlecms.internal.helper.BitConverter;
import littlecms.internal.helper.Stream;
import littlecms.internal.helper.VirtualPointer;

import net.rim.device.api.util.Arrays;

public class lcms2
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
    public static final int cmsSigUnices                            = 0x2A6E6978; // '*nix' // From argyll -- Not official
    public static final int cmsSigRim                               = 0x52494D20; // 'RIM ' // Not official, created for this library
    
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
    	public static final int SIZE = 4 + 4 + VirtualPointer.SIZE;
    	
    	public int len;
    	public int flag;
    	public VirtualPointer data;
    	
    	public cmsICCData()
    	{
    		len = 0;
    		flag = 0;
    		data = new VirtualPointer(1);
    	}
    }
    
    /** ICC date time*/
    public static class cmsDateTimeNumber
    {
    	public static final int SIZE = (2 * 6);
    	
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
    	public static final int SIZE = (2 * 3);
    	
        public short X;
        public short Y;
        public short Z;
    }
    
    /** Profile ID as computed by MD5 algorithm*/
    public static class cmsProfileID
    {
    	public static final int SIZE = 16;
    	
    	final byte[] data;
    	
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
    			id[i] = BitConverter.toInt16(this.data, k);
    		}
    		return id;
    	}
    	
    	public void setID16(short[] data)
    	{
    		for(int i = 0, k = 0; i < 8; i++, k += 2)
    		{
    			byte[] temp = BitConverter.getBytes(data[i]);
    			System.arraycopy(temp, 0, this.data, k, 2);
    		}
    	}
    	
    	public int[] getID32()
    	{
    		int[] id = new int[4];
    		for(int i = 0, k = 0; i < 4; i++, k += 4)
    		{
    			id[i] = BitConverter.toInt32(this.data, k);
    		}
    		return id;
    	}
    	
    	public void setID32(int[] data)
    	{
    		for(int i = 0, k = 0; i < 4; i++, k += 4)
    		{
    			byte[] temp = BitConverter.getBytes(data[i]);
    			System.arraycopy(temp, 0, this.data, k, 4);
    		}
    	}
    }
    
    // ----------------------------------------------------------------------------------------------
    // ICC profile internal base types. Strictly, shouldn't be declared in this header, but maybe
    // somebody want to use this info for accessing profile header directly, so here it is.
    
    /** Profile header -- it is 32-bit aligned, so no issues are expected on alignment*/
    public static class cmsICCHeader
    {
    	public static final int SIZE = (4 * 6) + cmsDateTimeNumber.SIZE + (4 * 5) + 8 + 4 + cmsEncodedXYZNumber.SIZE + 4 + cmsProfileID.SIZE + 28;
    	
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
        public final byte[]          reserved;
        
        public cmsICCHeader()
        {
        	this.reserved = new byte[28];
        	date = new cmsDateTimeNumber();
        	illuminant = new cmsEncodedXYZNumber();
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
    	public static final int SIZE = 4 * 3;
    	
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
    
    
    // Format of pixel is defined by one int, using bit fields as follows
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
    public static int FLOAT_SH(boolean a){        return FLOAT_SH(a ? 1 : 0);}
    public static int OPTIMIZED_SH(int s){        return ((s) << OPTIMIZED_SHIFT_VALUE);}
    public static int OPTIMIZED_SH(boolean s){    return OPTIMIZED_SH(s ? 1 : 0);}
    public static int COLORSPACE_SH(int s){       return ((s) << COLORSPACE_SHIFT_VALUE);}
    public static int SWAPFIRST_SH(int s){        return ((s) << SWAPFIRST_SHIFT_VALUE);}
    public static int SWAPFIRST_SH(boolean s){    return SWAPFIRST_SH(s ? 1 : 0);}
    public static int FLAVOR_SH(int s){           return ((s) << FLAVOR_SHIFT_VALUE);}
    public static int FLAVOR_SH(boolean s){       return FLAVOR_SH(s ? 1 : 0);}
    public static int PLANAR_SH(int p){           return ((p) << PLANAR_SHIFT_VALUE);}
    public static int PLANAR_SH(boolean p){       return PLANAR_SH(p ? 1 : 0);}
    public static int ENDIAN16_SH(int e){         return ((e) << ENDIAN16_SHIFT_VALUE);}
    public static int ENDIAN16_SH(boolean e){     return ENDIAN16_SH(e ? 1 : 0);}
    public static int DOSWAP_SH(int e){           return ((e) << DOSWAP_SHIFT_VALUE);}
    public static int DOSWAP_SH(boolean e){       return DOSWAP_SH(e ? 1 : 0);}
    public static int EXTRA_SH(int e){            return ((e) << EXTRA_SHIFT_VALUE);}
    public static int CHANNELS_SH(int c){         return ((c) << CHANNELS_SHIFT_VALUE);}
    public static int BYTES_SH(int b){            return ((b) << BYTES_SHIFT_VALUE);}
    
    // These macros unpack format specifiers into integers
    public static boolean T_FLOAT(int a){        return (((a) >> FLOAT_SHIFT_VALUE) & 1) != 0;}
    public static boolean T_OPTIMIZED(int o){    return (((o) >> OPTIMIZED_SHIFT_VALUE) & 1) != 0;}
    public static int T_COLORSPACE(int s){       return (((s) >> COLORSPACE_SHIFT_VALUE) & 31);}
    public static boolean T_SWAPFIRST(int s){    return (((s) >> SWAPFIRST_SHIFT_VALUE) & 1) != 0;}
    public static boolean T_FLAVOR(int s){       return (((s) >> FLAVOR_SHIFT_VALUE) & 1) != 0;}
    public static boolean T_PLANAR(int p){       return (((p) >> PLANAR_SHIFT_VALUE) & 1) != 0;}
    public static boolean T_ENDIAN16(int e){     return (((e) >> ENDIAN16_SHIFT_VALUE) & 1) != 0;}
    public static boolean T_DOSWAP(int e){       return (((e) >> DOSWAP_SHIFT_VALUE) & 1) != 0;}
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
		public static final int SIZE = (8 * 3);
		
		public double X;
		public double Y;
		public double Z;
		
		public cmsCIEXYZ()
		{
			this(0, 0, 0);
		}
		
		public cmsCIEXYZ(double X, double Y, double Z)
		{
			this.X = X;
			this.Y = Y;
			this.Z = Z;
		}
	}
	
	public static class cmsCIExyY
	{
		public static final int SIZE = (8 * 3);
		
		public double x;
		public double y;
		public double Y;
	}
	
	public static class cmsCIELab
	{
		public static final int SIZE = (8 * 3);
		
		public double L;
		public double a;
		public double b;
	}
	
	public static class cmsCIELCh
	{
		public static final int SIZE = (8 * 3);
		
		public double L;
		public double C;
		public double h;
	}
	
	public static class cmsJCh
	{
		public static final int SIZE = (8 * 3);
		
		public double J;
		public double C;
		public double h;
	}
	
	public static class cmsCIEXYZTRIPLE
	{
		public static final int SIZE = (cmsCIEXYZ.SIZE * 3);
		
		public cmsCIEXYZ Red;
		public cmsCIEXYZ Green;
		public cmsCIEXYZ Blue;
	}
	
	public static class cmsCIExyYTRIPLE
	{
		public static final int SIZE = (cmsCIExyY.SIZE * 3);
		
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
		public static final int SIZE = 4 + cmsCIEXYZ.SIZE + 4 + 8 + 4;
		
		/** 0 = unknown, 1=CIE 1931, 2=CIE 1964*/
		public int Observer;
		/** Value of backing*/
        public cmsCIEXYZ Backing;
        /** 0=unknown, 1=45/0, 0/45 2=0d, d/0*/
        public int Geometry;
        /** 0..1.0*/
        public double Flare;
        public int IlluminantType;
        
        public cmsICCMeasurementConditions()
        {
        	Backing = new cmsCIEXYZ();
        }
	}
	
	public static class cmsICCViewingConditions
	{
		public static final int SIZE = (cmsCIEXYZ.SIZE * 2) + 4;
		
		/** Not the same struct as CAM02,*/
		public cmsCIEXYZ IlluminantXYZ;
		/** This is for storing the tag*/
		public cmsCIEXYZ SurroundXYZ;
        /** viewing condition*/
        public int IlluminantType;
        
        public cmsICCViewingConditions()
        {
        	IlluminantXYZ = new cmsCIEXYZ();
        	SurroundXYZ = new cmsCIEXYZ();
        }
	}
	
	// Support of non-standard functions --------------------------------------------------------------------------------------
	
	public static int cmsstrcasecmp(final String s1, final String s2)
	{
		return cmsplugin.cmsstrcasecmp(s1, s2);
	}
	
	public static long cmsfilelength(Stream f)
	{
		return cmsplugin.cmsfilelength(f);
	}
	
	// Plug-In registering  ---------------------------------------------------------------------------------------------------
	
	/**
	 * Declares external extensions to the core engine. The "Plugin" parameter may hold one or several plug-ins, as defined by the plug-in developer.
	 * @param Plugin Pointer to plug-in collection.
	 * @return TRUE on success FALSE on error.
	 */
	public static boolean cmsPlugin(lcms2_plugin.cmsPluginBase Plugin) //This originally took a generic Object but internally would only take cmsPluginBase. Reduce chance of errors.
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
	 * @param TempK Pointer to a user-allocated double variable to receive the resulting temperature.
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
	public static class cmsToneCurve extends lcms2_internal._cms_curve_struct{}
	
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
	
	/**
	 * Composites two tone curves in the form <code>Y<sup>-1</sup>(X(t))</code>
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param X Pointers to tone curve objects.
	 * @param Y Pointers to tone curve objects.
	 * @param nPoints Sample rate for resulting tone curve.
	 * @return Pointer to a newly created tone curve object on success, NULL on error.
	 */
	public static cmsToneCurve cmsJoinToneCurve(cmsContext ContextID, final cmsToneCurve X,  final cmsToneCurve Y, int nPoints)
	{
		return cmsgamma.cmsJoinToneCurve(ContextID, X, Y, nPoints);
	}
	
	/**
	 * Smoothes tone curve according to the lambda parameter. From: Eilers, P.H.C. (1994) Smoothing and interpolation with finite differences. in: Graphic Gems IV, 
	 * Heckbert, P.S. (ed.), Academic press.
	 * @param Tab pointer to a tone curve object.
	 * @param lambda degree of smoothing
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean cmsSmoothToneCurve(cmsToneCurve Tab, double lambda)
	{
		return cmsgamma.cmsSmoothToneCurve(Tab, lambda);
	}
	
	/**
	 * Evaluates the given floating-point number across the given tone curve.
	 * @param Curve pointer to a tone curve object.
	 * @param v floating point number to evaluate
	 * @return Operation result
	 */
	public static float cmsEvalToneCurveFloat(final cmsToneCurve Curve, float v)
	{
		return cmsgamma.cmsEvalToneCurveFloat(Curve, v);
	}
	
	/**
	 * Evaluates the given 16-bit number across the given tone curve. This function is significantly faster than cmsEvalToneCurveFloat, since it uses a pre-computed 
	 * 16-bit lookup table.
	 * @param Curve pointer to a tone curve object.
	 * @param v 16 bit Number to evaluate
	 * @return Operation result
	 */
	public static short cmsEvalToneCurve16(final cmsToneCurve Curve, short v)
	{
		return cmsgamma.cmsEvalToneCurve16(Curve, v);
	}
	
	/**
	 * Returns TRUE if the tone curve contains more than one segment, FALSE if it has only one segment.
	 * @param InGamma pointer to a tone curve object.
	 * @return TRUE or FALSE.
	 */
	public static boolean cmsIsToneCurveMultisegment(final cmsToneCurve InGamma)
	{
		return cmsgamma.cmsIsToneCurveMultisegment(InGamma);
	}
	
	/**
	 * Returns an estimation of cube being an identity (1:1) in the [0..1] domain. Does not take unbounded parts into account. This is just a coarse approximation, with 
	 * no mathematical validity.
	 * @param Curve pointer to a tone curve object.
	 * @return TRUE or FALSE.
	 */
	public static boolean cmsIsToneCurveLinear(final cmsToneCurve Curve)
	{
		return cmsgamma.cmsIsToneCurveLinear(Curve);
	}
	
	/**
	 * Returns an estimation of monotonicity of curve in the [0..1] domain. Does not take unbounded parts into account. This is just a coarse approximation, with no 
	 * mathematical validity.
	 * @param t pointer to a tone curve object.
	 * @return TRUE or FALSE.
	 */
	public static boolean cmsIsToneCurveMonotonic(final cmsToneCurve t)
	{
		return cmsgamma.cmsIsToneCurveMonotonic(t);
	}
	
	/**
	 * Returns TRUE if <code>f(0) &gt; f(1)</code>, FALSE otherwise. Does not take unbounded parts into account.
	 * @param t pointer to a tone curve object.
	 * @return TRUE or FALSE.
	 */
	public static boolean cmsIsToneCurveDescending(final cmsToneCurve t)
	{
		return cmsgamma.cmsIsToneCurveDescending(t);
	}
	
	public static int cmsGetToneCurveParametricType(final cmsToneCurve t)
	{
		return cmsgamma.cmsGetToneCurveParametricType(t);
	}
	
	/**
	 * Estimates the apparent gamma of the tone curve by using least squares fitting to a pure exponential expression in the <code>f(x) = x<sup>y</sup></code>. The 
	 * parameter γ is estimated at the given precision.
	 * @param t pointer to a tone curve object.
	 * @param Precision The maximum standard deviation allowed on the residuals, 0.01 is a fair value, set it to a big number to fit any curve, mo matter how good is 
	 * the fit.
	 * @return The estimated gamma at given precision, or -1.0 if the fitting has less precision.
	 */
	public static double cmsEstimateGamma(final cmsToneCurve t, double Precision)
	{
		return cmsgamma.cmsEstimateGamma(t, Precision);
	}
	
	// Implements pipelines of multi-processing elements -------------------------------------------------------------
	
	// Nothing to see here, move along
	public static class cmsPipeline extends lcms2_internal._cmsPipeline_struct{}
	public static class cmsStage extends lcms2_internal._cmsStage_struct{}
	
	// Those are hi-level pipelines
	/**
	 * Allocates an empty pipeline. Final Input and output channels must be specified at creation time.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param InputChannels Number of channels on input
	 * @param OutputChannels Number of channels on output
	 * @return A pointer to a pipeline on success, NULL on error.
	 */
	public static cmsPipeline cmsPipelineAlloc(cmsContext ContextID, int InputChannels, int OutputChannels)
	{
		return cmslut.cmsPipelineAlloc(ContextID, InputChannels, OutputChannels);
	}
	
	/**
	 * Frees a pipeline and all owned stages.
	 * @param lut Pointer to a pipeline object.
	 */
	public static void cmsPipelineFree(cmsPipeline lut)
	{
		cmslut.cmsPipelineFree(lut);
	}
	
	/**
	 * Duplicates a pipeline object, and all associated resources.
	 * @param Orig Pointer to a pipeline object.
	 * @return A pointer to a pipeline on success, NULL on error.
	 */
	public static cmsPipeline cmsPipelineDup(final cmsPipeline Orig)
	{
		return cmslut.cmsPipelineDup(Orig);
	}
	
	/**
	 * Returns the number of input channels of a given pipeline.
	 * @param lut Pointer to a pipeline object.
	 * @return Number of channels on success, 0 on error.
	 */
	public static int cmsPipelineInputChannels(final cmsPipeline lut)
	{
		return cmslut.cmsPipelineInputChannels(lut);
	}
	
	/**
	 * Returns number of output channels of a given pipeline.
	 * @param lut Pointer to a pipeline object.
	 * @return Number of channels on success, 0 on error.
	 */
	public static int cmsPipelineOutputChannels(final cmsPipeline lut)
	{
		return cmslut.cmsPipelineOutputChannels(lut);
	}
	
	/**
	 * Returns number of stages of a given pipeline.
	 * @param lut Pointer to a pipeline object.
	 * @return Number of stages of pipeline.
	 */
	public static int cmsPipelineStageCount(final cmsPipeline lut)
	{
		return cmslut.cmsPipelineStageCount(lut);
	}
	
	/**
	 * Get a pointer to the first stage in the pipeline, or NULL if pipeline is empty. Intended for iterators.
	 * @param lut Pointer to a pipeline object.
	 * @return A pointer to a pipeline stage on success, NULL on empty pipeline.
	 */
	public static cmsStage cmsPipelineGetPtrToFirstStage(final cmsPipeline lut)
	{
		return cmslut.cmsPipelineGetPtrToFirstStage(lut);
	}
	
	/**
	 * Get a pointer to the first stage in the pipeline, or NULL if pipeline is empty. Intended for iterators.
	 * @param lut Pointer to a pipeline object.
	 * @return A pointer to a pipeline stage on success, NULL on empty pipeline.
	 */
	public static cmsStage cmsPipelineGetPtrToLastStage(final cmsPipeline lut)
	{
		return cmslut.cmsPipelineGetPtrToFirstStage(lut);
	}
	
	
	/**
	 * Evaluates a pipeline usin 16-bit numbers, optionally using the optimized path.
	 * @param In Input values.
	 * @param Out Output values.
	 * @param lut Pointer to a pipeline object.
	 */
	public static void cmsPipelineEval16(final short[] In, short[] Out, final cmsPipeline lut)
	{
		cmslut.cmsPipelineEval16(In, Out, lut);
	}
	
	/**
	 * Evaluates a pipeline using floating point numbers.
	 * @param In Input values.
	 * @param Out Output values.
	 * @param lut Pointer to a pipeline object.
	 */
	public static void cmsPipelineEvalFloat(final float[] In, float[] Out, final cmsPipeline lut)
	{
		cmslut.cmsPipelineEvalFloat(In, Out, lut);
	}
	
	/**
	 * Evaluates a pipeline in the reverse direction, using Newton’s method.
	 * @param Target Input values.
	 * @param Result Output values.
	 * @param Hint Where begin the search
	 * @param lut Pointer to a pipeline object.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean cmsPipelineEvalReverseFloat(float[] Target, float[] Result, float[] Hint, final cmsPipeline lut)
	{
		return cmslut.cmsPipelineEvalReverseFloat(Target, Result, Hint, lut);
	}
	
	/**
	 * Appends pipeline l2 at the end of pipeline l1. Channel count must match.
	 * @param l1 Pointer to a pipeline object.
	 * @param l2 Pointer to a pipeline object.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean cmsPipelineCat(cmsPipeline l1, final cmsPipeline l2)
	{
		return cmslut.cmsPipelineCat(l1, l2);
	}
	
	/**
	 * Sets an internal flag that marks the pipeline to be saved in 8 bit precision. By default all pipelines are saved on 16 bits precision on AtoB/BToA tags and in 
	 * floating point precision on DToB/BToD tags.
	 * @param lut Pointer to a pipeline object.
	 * @param On State of the flag, TRUE=Save as 8 bits, FALSE=Save as 16 bits
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean cmsPipelineSetSaveAs8bitsFlag(cmsPipeline lut, boolean On)
	{
		return cmslut.cmsPipelineSetSaveAs8bitsFlag(lut, On);
	}
	
	// Where to place/locate the stages in the pipeline chain
	public static final int cmsAT_BEGIN = 0;
	public static final int cmsAT_END = cmsAT_BEGIN + 1;
	
	/**
	 * Inserts a stage on either the head or the tail of a given pipeline.
	 * @param lut Pointer to a pipeline object.
	 * @param loc enumerated constant, either {@link #cmsAT_BEGIN} or {@link #cmsAT_END}
	 * @param mpe Pointer to a stage object
	 */
	public static void cmsPipelineInsertStage(cmsPipeline lut, int loc, cmsStage mpe)
	{
		cmslut.cmsPipelineInsertStage(lut, loc, mpe);
	}
	
	/**
	 * Removes the stage from the pipeline. Additionally it can grab the stage <b>without freeing it</b>. To do so, caller must specify a variable to receive a 
	 * pointer to the stage being unlinked. If mpe is NULL, the stage is then removed and freed.
	 * @param lut Pointer to a pipeline object.
	 * @param loc enumerated constant, either {@link #cmsAT_BEGIN} or {@link #cmsAT_END}
	 * @param mpe Pointer to a variable to receive a pointer to the stage object being unlinked. NULL to free the resource automatically.
	 */
	public static void cmsPipelineUnlinkStage(cmsPipeline lut, int loc, cmsStage[] mpe)
	{
		cmslut.cmsPipelineUnlinkStage(lut, loc, mpe);
	}
	
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
	/**
	 * Creates an empty (identity) stage that does no operation. May be needed in order to save the pipeline as AToB/BToA tags in ICC profiles.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param nChannels Number of channels
	 * @return A pointer to a pipeline stage on success, NULL on error.
	 */
	public static cmsStage cmsStageAllocIdentity(cmsContext ContextID, int nChannels)
	{
		return cmslut.cmsStageAllocIdentity(ContextID, nChannels);
	}
	
	/**
	 * Creates a stage that contains nChannels tone curves, one per channel. Setting Curves to NULL forces identity (1:1) curves to be used. The stage keeps and owns a 
	 * private copy of the tone curve objects.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param nChannels Number of Channels of stage
	 * @param Curves Array of tone curves objects, one per channel.
	 * @return A pointer to a pipeline stage on success, NULL on error.
	 */
	public static cmsStage cmsStageAllocToneCurves(cmsContext ContextID, int nChannels, final cmsToneCurve[] Curves)
	{
		return cmslut.cmsStageAllocToneCurves(ContextID, nChannels, Curves);
	}
	
	/**
	 * Creates a stage that contains a matrix plus an optional offset. Note that Matrix is specified in double precision, whilst CLUT has only float precision. That is 
	 * because an ICC profile can encode matrices with far more precision that CLUTS.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param Rows Dimensions of matrix
	 * @param Cols Dimensions of matrix
	 * @param Matrix Points to a matrix of [Rows, Cols]
	 * @param Offset Points to a vector of [Cols], NULL if no offset is to be applied.
	 * @return A pointer to a pipeline stage on success, NULL on error.
	 */
	public static cmsStage cmsStageAllocMatrix(cmsContext ContextID, int Rows, int Cols, final double[] Matrix, final double[] Offset)
	{
		return cmslut.cmsStageAllocMatrix(ContextID, Rows, Cols, Matrix, Offset);
	}
	
	/**
	 * Creates a stage that contains a 16 bits multidimensional lookup table (CLUT). Each dimension has same resolution. The CLUT can be initialized by specifying 
	 * values in Table parameter. The recommended way is to set Table to NULL and use cmsStageSampleCLut16bit with a callback, because this way the implementation is 
	 * independent of the selected number of grid points.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param nGridPoints the number of nodes (same for each component).
	 * @param inputChan Number of input channels.
	 * @param outputChan Number of output channels.
	 * @param Table a pointer to a table of short, holding initial values for nodes. If NULL the CLUT is initialized to zero.
	 * @return A pointer to a pipeline stage on success, NULL on error.
	 */
	public static cmsStage cmsStageAllocCLut16bit(cmsContext ContextID, int nGridPoints, int inputChan, int outputChan, final short[] Table)
	{
		return cmslut.cmsStageAllocCLut16bit(ContextID, nGridPoints, inputChan, outputChan, Table);
	}
	
	/**
	 * Creates a stage that contains a float multidimensional lookup table (CLUT). Each dimension has same resolution. The CLUT can be initialized by specifying values 
	 * in Table parameter. The recommended way is to set Table to NULL and use cmsStageSampleCLutFloat with a callback, because this way the implementation is 
	 * independent of the selected number of grid points.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param nGridPoints the number of nodes (same for each component).
	 * @param inputChan Number of input channels.
	 * @param outputChan Number of output channels.
	 * @param Table a pointer to a table of float, holding initial values for nodes. If NULL the CLUT is initialized to zero.
	 * @return A pointer to a pipeline stage on success, NULL on error.
	 */
	public static cmsStage cmsStageAllocCLutFloat(cmsContext ContextID, int nGridPoints, int inputChan, int outputChan, final float[] Table)
	{
		return cmslut.cmsStageAllocCLutFloat(ContextID, nGridPoints, inputChan, outputChan, Table);
	}
	
	/**
	 * Similar to cmsStageAllocCLut16bit, but it allows different granularity on each CLUT dimension.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param clutPoints Array [inputChan] holding the number of nodes for each component.
	 * @param inputChan Number of input channels.
	 * @param outputChan Number of output channels.
	 * @param Table pointer to a table of short, holding initial values for nodes. If NULL the CLUT is initialized to zero.
	 * @return A pointer to a pipeline stage on success, NULL on error.
	 */
	public static cmsStage cmsStageAllocCLut16bitGranular(cmsContext ContextID, final int[] clutPoints, int inputChan, int outputChan, final short[] Table)
	{
		return cmslut.cmsStageAllocCLut16bitGranular(ContextID, clutPoints, inputChan, outputChan, Table);
	}
	
	/**
	 * Similar to cmsStageAllocCLutFloat, but it allows different granularity on each CLUT dimension.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param clutPoints Array [inputChan] holding the number of nodes for each component.
	 * @param inputChan Number of input channels.
	 * @param outputChan Number of output channels.
	 * @param Table pointer to a table of float, holding initial values for nodes. If NULL the CLUT is initialized to zero.
	 * @return A pointer to a pipeline stage on success, NULL on error.
	 */
	public static cmsStage cmsStageAllocCLutFloatGranular(cmsContext ContextID, final int[] clutPoints, int inputChan, int outputChan, final float[] Table)
	{
		return cmslut.cmsStageAllocCLutFloatGranular(ContextID, clutPoints, inputChan, outputChan, Table);
	}
	
	
	/**
	 * Duplicates a pipeline stage and all associated resources.
	 * @param mpe a pointer to the stage to be duplicated.
	 * @return A pointer to a pipeline stage on success, NULL on error.
	 */
	public static cmsStage cmsStageDup(cmsStage mpe)
	{
		return cmslut.cmsStageDup(mpe);
	}
	
	/**
	 * Destroys a pipeline stage object, freeing any associated resources. The stage should first be unlinked from any pipeline before proceeding to free it.
	 * @param mpe a pointer to a stage object.
	 */
	public static void cmsStageFree(cmsStage mpe)
	{
		cmslut.cmsStageFree(mpe);
	}
	
	/**
	 * Returns next stage in pipeline list, or NULL if end of list. Intended for iterators.
	 * @param mpe a pointer to the actual stage object.
	 * @return A pointer to the next stage in pipeline or NULL on end of list.
	 */
	public static cmsStage cmsStageNext(final cmsStage mpe)
	{
		return cmslut.cmsStageNext(mpe);
	}
	
	
	/**
	 * Returns the number of input channels of a given stage object.
	 * @param mpe a pointer to a stage object.
	 * @return Number of input channels of pipeline stage object.
	 */
	public static int cmsStageInputChannels(final cmsStage mpe)
	{
		return cmslut.cmsStageInputChannels(mpe);
	}
	
	/**
	 * Returns the number of output channels of a given stage object.
	 * @param mpe a pointer to a stage object.
	 * @return Number of output channels of pipeline stage object.
	 */
	public static int cmsStageOutputChannels(final cmsStage mpe)
	{
		return cmslut.cmsStageOutputChannels(mpe);
	}
	
	/**
	 * Returns the type of a given stage object
	 * @param mpe a pointer to a stage object.
	 * @return The type of a given stage object
	 */
	public static int cmsStageType(final cmsStage mpe)
	{
		return cmslut.cmsStageType(mpe);
	}
	
	public static Object cmsStageData(final cmsStage mpe)
	{
		return cmslut.cmsStageData(mpe);
	}
	
	// Sampling
	public static interface cmsSAMPLER16
	{
		public int run(final short[] In, short[] Out, Object Cargo);
	}
	
	public static interface cmsSAMPLERFLOAT
	{
		public int run(final float[] In, float[] Out, Object Cargo);
	}
	
	/** Use this flag to prevent changes being written to destination*/
	public static final int SAMPLER_INSPECT = 0x01000000;
	
	// For CLUT only
	/**
	 * Iterate on all nodes of a given CLUT stage, calling a 16-bit sampler on each node.
	 * @param mpe a pointer to a CLUT stage object.
	 * @param Sampler 16 bit callback to be executed on each node.
	 * @param Cargo Points to a user-supplied data which be transparently passed to the callback.
	 * @param dwFlags Bit-field flags for different options. Only SAMPLER_INSPECT is currently supported.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean cmsStageSampleCLut16bit(cmsStage mpe, cmsSAMPLER16 Sampler, Object Cargo, int dwFlags)
	{
		return cmslut.cmsStageSampleCLut16bit(mpe, Sampler, Cargo, dwFlags);
	}
	
	/**
	 * Iterate on all nodes of a given CLUT stage, calling a float sampler on each node.
	 * @param mpe a pointer to a CLUT stage object.
	 * @param Sampler Floating point callback to be executed on each node.
	 * @param Cargo Points to a user-supplied data which be transparently passed to the callback.
	 * @param dwFlags Bit-field flags for different options. Only SAMPLER_INSPECT is currently supported.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean cmsStageSampleCLutFloat(cmsStage mpe, cmsSAMPLERFLOAT Sampler, Object Cargo, int dwFlags)
	{
		return cmslut.cmsStageSampleCLutFloat(mpe, Sampler, Cargo, dwFlags);
	}
	
	
	// Slicers
	/**
	 * Slices target space executing a 16 bits callback of type cmsSAMPLER16.
	 * @param nInputs Number of components in target space.
	 * @param clutPoints Array [nInputs] holding the division slices for each component.
	 * @param Sampler 16 bit callback to execute on each slice.
	 * @param Cargo Points to a user-supplied data which be transparently passed to the callback.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean cmsSliceSpace16(int nInputs, final int[] clutPoints, cmsSAMPLER16 Sampler, Object Cargo)
	{
		return cmslut.cmsSliceSpaceFloat(nInputs, clutPoints, Sampler, Cargo);
	}
	
	/**
	 * Slices target space executing a floating point callback of type cmsSAMPLERFLOAT.
	 * @param nInputs Number of components in target space.
	 * @param clutPoints Array [nInputs] holding the division slices for each component.
	 * @param Sampler Floating point callback to execute on each slice.
	 * @param Cargo Points to a user-supplied data wich be transparently passed to the callback.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean cmsSliceSpaceFloat(int nInputs, final int[] clutPoints, cmsSAMPLERFLOAT Sampler, Object Cargo)
	{
		return cmslut.cmsSliceSpaceFloat(nInputs, clutPoints, Sampler, Cargo);
	}
	
	// Multilocalized Unicode management ---------------------------------------------------------------------------------------
	
	public static class cmsMLU extends lcms2_internal._cms_MLU_struct{}
	
	//Use of static was determined by RIM development doc that stated that if "static final" then String would be allocated multiple times and need to be loaded multiple times but by using just "static" only one instance is used and always the same memory
	public static String cmsNoLanguage = "\0\0";
	public static String cmsNoCountry = cmsNoLanguage; //Reference since they are the same value and immutable.
	
	/**
	 * Allocates an empty multilocalized unicode object.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @return pointer to a multilocalized unicode object on success, NULL on error.
	 */
	public static cmsMLU cmsMLUalloc(cmsContext ContextID, int nItems)
	{
		return cmsnamed.cmsMLUalloc(ContextID, nItems);
	}
	
	/**
	 * Destroys a multilocalized unicode object, freeing any associated resources.
	 * @param mlu a pointer to a multilocalized unicode object.
	 */
	public static void cmsMLUfree(cmsMLU mlu)
	{
		cmsnamed.cmsMLUfree(mlu);
	}
	
	/**
	 * Duplicates a multilocalized unicode object, and all associated resources.
	 * @param mlu a pointer to a multilocalized unicode object.
	 * @return A pointer to a multilocalized unicode object on success, NULL on error.
	 */
	public static cmsMLU cmsMLUdup(final cmsMLU mlu)
	{
		return cmsnamed.cmsMLUdup(mlu);
	}
	
	/**
	 * Fills an ASCII (7 bit) entry for the given Language and country.
	 * @param mlu a pointer to a multilocalized unicode object.
	 * @param LanguageCode Array of 3 chars describing the language
	 * @param CountryCode Array of 3 chars describing the country
	 * @param ASCIIString String to add.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean cmsMLUsetASCII(cmsMLU mlu, final String LanguageCode, final String CountryCode, final String ASCIIString)
	{
		return cmsnamed.cmsMLUsetASCII(mlu, LanguageCode, CountryCode, ASCIIString);
	}
	
	/**
	 * Fills a UNICODE wide char (16 bit) entry for the given Language and country.
	 * @param mlu a pointer to a multilocalized unicode object.
	 * @param LanguageCode Array of 3 chars describing the language
	 * @param CountryCode Array of 3 chars describing the country
	 * @param WideString String to add.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean cmsMLUsetWide(cmsMLU mlu, final String LanguageCode, final String CountryCode, final String WideString)
	{
		return cmsnamed.cmsMLUsetWide(mlu, LanguageCode, CountryCode, WideString);
	}
	
	/**
	 * Gets an ASCII (7 bit) entry for the given Language and country. Set Buffer to NULL to get the required size.
	 * @param mlu a pointer to a multilocalized unicode object.
	 * @param LanguageCode Array of 3 chars describing the language
	 * @param CountryCode Array of 3 chars describing the country
	 * @param Buffer Pointer to a byte buffer
	 * @param BufferSize Size of given buffer.
	 * @return Number of bytes read into buffer.
	 */
	public static int cmsMLUgetASCII(cmsMLU mlu, final String LanguageCode, final String CountryCode, StringBuffer Buffer, int BufferSize)
	{
		return cmsnamed.cmsMLUgetASCII(mlu, LanguageCode, CountryCode, Buffer, BufferSize);
	}
	
	/**
	 * Gets a UNICODE char (16 bit) entry for the given Language and country. Set Buffer to NULL to get the required size.
	 * @param mlu a pointer to a multilocalized unicode object.
	 * @param LanguageCode Array of 3 chars describing the language
	 * @param CountryCode Array of 3 chars describing the country
	 * @param Buffer Pointer to a char buffer
	 * @param BufferSize Size of given buffer.
	 * @return Number of bytes read into buffer.
	 */
	public static int cmsMLUgetWide(cmsMLU mlu, final String LanguageCode, final String CountryCode, StringBuffer Buffer, int BufferSize)
	{
		return cmsnamed.cmsMLUgetWide(mlu, LanguageCode, CountryCode, Buffer, BufferSize);
	}
	
	/**
	 * Obtains the translation rule for given multilocalized unicode object.
	 * @param mlu a pointer to a multilocalized unicode object.
	 * @param LanguageCode Array of 3 chars describing the language
	 * @param CountryCode Array of 3 chars describing the country
	 * @param ObtainedLanguage Array of 3 chars to get the language translation.
	 * @param ObtainedCountry Array of 3 chars to get the country translation.
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean cmsMLUgetTranslation(cmsMLU mlu, final String LanguageCode, final String CountryCode, String ObtainedLanguage, String ObtainedCountry)
	{
		return cmsnamed.cmsMLUgetTranslation(mlu, LanguageCode, CountryCode, ObtainedLanguage, ObtainedCountry);
	}
	
	// Undercolorremoval & black generation -------------------------------------------------------------------------------------
	
	public static class cmsUcrBg
	{
		public cmsToneCurve Ucr;
		public cmsToneCurve Bg;
		public cmsMLU Desc;
	}
	
	// Screening ----------------------------------------------------------------------------------------------------------------
	
	public static final int cmsPRINTER_DEFAULT_SCREENS = 0x0001;
	public static final int cmsFREQUENCE_UNITS_LINES_CM = 0x0000;
	public static final int cmsFREQUENCE_UNITS_LINES_INCH = 0x0002;
	
	public static final int cmsSPOT_UNKNOWN = 0;
	public static final int cmsSPOT_PRINTER_DEFAULT = 1;
	public static final int cmsSPOT_ROUND = 2;
	public static final int cmsSPOT_DIAMOND = 3;
	public static final int cmsSPOT_ELLIPSE = 4;
	public static final int cmsSPOT_LINE = 5;
	public static final int cmsSPOT_SQUARE = 6;
	public static final int cmsSPOT_CROSS = 7;
	
	public static class cmsScreeningChannel
	{
		public static final int SIZE = (2 * 8) + 4;
		
	    public double Frequency;
	    public double ScreenAngle;
	    public int SpotShape;
	}
	
	public static class cmsScreening
	{
		public static final int SIZE = (cmsMAXCHANNELS * cmsScreeningChannel.SIZE) + (2 * 4);
		
		public int Flag;
		public int nChannels;
		public cmsScreeningChannel[] Channels;
		
		public cmsScreening()
		{
			Channels = new cmsScreeningChannel[cmsMAXCHANNELS];
		}
	}
	
	// Named color -----------------------------------------------------------------------------------------------------------------
	
	public static class cmsNAMEDCOLORLIST extends lcms2_internal._cms_NAMEDCOLORLIST_struct{}
	
	/**
	 * Allocates an empty named color dictionary.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param n Initial number of spot colors in the list
	 * @param ColorantCount Number of channels of device space (i.e, 3 for RGB, 4 for CMYK, etc,)
	 * @param Prefix fixed strings for all spot color names, e.g., “coated”, “system”, …
	 * @param Suffix fixed strings for all spot color names, e.g., “coated”, “system”, …
	 * @return A pointer to a newly created named color list dictionary on success, NULL on error.
	 */
	public static cmsNAMEDCOLORLIST cmsAllocNamedColorList(cmsContext ContextID, int n, int ColorantCount, final String Prefix, final String Suffix)
	{
		return cmsnamed.cmsAllocNamedColorList(ContextID, n, ColorantCount, Prefix, Suffix);
	}
	
	/**
	 * Destroys a Named color list object, freeing any associated resources.
	 * @param v A pointer to a named color list dictionary object.
	 */
	public static void cmsFreeNamedColorList(cmsNAMEDCOLORLIST v)
	{
		cmsnamed.cmsFreeNamedColorList(v);
	}
	
	/**
	 * Duplicates a named color list object, and all associated resources.
	 * @param v A pointer to a named color list dictionary object.
	 * @return A pointer to a newly created named color list dictionary on success, NULL on error.
	 */
	public static cmsNAMEDCOLORLIST cmsDupNamedColorList(final cmsNAMEDCOLORLIST v)
	{
		return cmsnamed.cmsDupNamedColorList(v);
	}
	
	/**
	 * Adds a new spot color to the list. If the number of elements in the list exceeds the initial storage, the list is realloc’ed to accommodate things.
	 * @param v A pointer to a named color list dictionary object.
	 * @param Name The spot color name without any prefix or suffix specified in cmsAllocNamedColorList
	 * @param PCS Encoded PCS coordinates.
	 * @param Colorant Encoded values for device colorant.
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean cmsAppendNamedColor(cmsNAMEDCOLORLIST v, final String Name, short[] PCS, short[] Colorant)
	{
		return cmsnamed.cmsAppendNamedColor(v, Name, PCS, Colorant);
	}
	
	/**
	 * Returns the number of spot colors in a named color list.
	 * @param v A pointer to a named color list dictionary object.
	 * @return the number of spot colors on success, 0 on error.
	 */
	public static int cmsNamedColorCount(final cmsNAMEDCOLORLIST v)
	{
		return cmsnamed.cmsNamedColorCount(v);
	}
	
	/**
	 * Performs a look-up in the dictionary and returns an index on the given color name.
	 * @param v A pointer to a named color list dictionary object.
	 * @return Index on name, or -1 if the spot color is not found.
	 */
	public static int cmsNamedColorIndex(final cmsNAMEDCOLORLIST v, final String Name)
	{
		return cmsnamed.cmsNamedColorIndex(v, Name);
	}
	
	/**
	 * Gets extended information on a spot color, given its index. Required storage is of fixed size.
	 * @param NamedColorList A pointer to a named color list dictionary object.
	 * @param nColor Index to the spot color to retrieve
	 * @param Name Pointer to a 256-char array to get the name, NULL to ignore.
	 * @param Prefix Pointer to a 33-char array to get the prefix, NULL to ignore
	 * @param Suffix Pointer to a 33-char array to get the suffix, NULL to ignore.
	 * @param PCS Pointer to a 3-cmsUInt16Number to get the encoded PCS, NULL to ignore
	 * @param Colorant Pointer to a 16-cmsUInt16Number to get the encoded Colorant, NULL to ignore
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean cmsNamedColorInfo(final cmsNAMEDCOLORLIST NamedColorList, int nColor, StringBuffer Name, StringBuffer Prefix, StringBuffer Suffix, short[] PCS, 
			short[] Colorant)
	{
		return cmsnamed.cmsNamedColorInfo(NamedColorList, nColor, Name, Prefix, Suffix, PCS, Colorant);
	}
	
	//Retrieve named color list from transform
	/**
	 * Retrieve a named color list from a given color transform.
	 * @param xform Handle to a color transform object.
	 * @return A pointer to a named color list dictionary on success, NULL on error.
	 */
	public static cmsNAMEDCOLORLIST cmsGetNamedColorList(cmsHTRANSFORM xform)
	{
		return cmsnamed.cmsGetNamedColorList(xform);
	}
	
	// Profile sequence -----------------------------------------------------------------------------------------------------
	
	// Profile sequence descriptor. Some fields come from profile sequence descriptor tag, others
	// come from Profile Sequence Identifier Tag
	public static class cmsPSEQDESC
	{
		public int deviceMfg;
		public int deviceModel;
		public long attributes;
		public int technology;
		public cmsProfileID ProfileID;
		public cmsMLU Manufacturer;
		public cmsMLU Model;
		public cmsMLU Description;
		
		public cmsPSEQDESC()
		{
			ProfileID = new cmsProfileID();
		}
	}
	
	public static class cmsSEQ
	{
		public int n;
		public cmsContext ContextID;
		public cmsPSEQDESC[] seq;
	}
	
	/**
	 * Creates an empty container for profile sequences.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param n Number of profiles in the sequence
	 * @return A pointer to a profile sequence object on success, NULL on error.
	 */
	public static cmsSEQ cmsAllocProfileSequenceDescription(cmsContext ContextID, int n)
	{
		return cmsnamed.cmsAllocProfileSequenceDescription(ContextID, n);
	}
	
	/**
	 * Duplicates a profile sequence object, and all associated resources.
	 * @param pseq A pointer to a profile sequence object.
	 * @return A pointer to a profile sequence object on success, NULL on error.
	 */
	public static cmsSEQ cmsDupProfileSequenceDescription(final cmsSEQ pseq)
	{
		return cmsnamed.cmsDupProfileSequenceDescription(pseq);
	}
	
	/**
	 * Destroys a profile sequence object, freeing all associated memory.
	 * @param pseq A pointer to a profile sequence object.
	 */
	public static void cmsFreeProfileSequenceDescription(cmsSEQ pseq)
	{
		cmsnamed.cmsFreeProfileSequenceDescription(pseq);
	}
	
	// Access to Profile data ----------------------------------------------------------------------------------------------
	/**
	 * Creates an empty profile object, to be populated by the programmer.
	 * <p>
	 * <strong>WARNING: The profile without adding any information is not directly useable.</strong>
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsCreateProfilePlaceholder(cmsContext ContextID)
	{
		return cmsio0.cmsCreateProfilePlaceholder(ContextID);
	}
	
	/**
	 * Returns the ContextID associated with a given profile.
	 * @param hProfile Handle to a profile object
	 * @return Pointer to a user-defined context cargo or NULL if no context
	 */
	public static cmsContext cmsGetProfileContextID(cmsHPROFILE hProfile)
	{
		return cmsio0.cmsGetProfileContextID(hProfile);
	}
	
	/**
	 * Returns number of tags of a given profile.
	 * @param hProfile Handle to a profile object
	 * @return Number of tags on success, -1 on error.
	 */
	public static int cmsGetTagCount(cmsHPROFILE hProfile)
	{
		return cmsio0.cmsGetTagCount(hProfile);
	}
	
	/**
	 * Returns the signature of a tag located in n position being n a 0-based index: i.e., first tag is indexed with n = 0.
	 * @param hProfile Handle to a profile object
	 * @param n index to a tag position (0-based)
	 * @return The tag signature on success, 0 on error.
	 */
	public static int cmsGetTagSignature(cmsHPROFILE hProfile, int n)
	{
		return cmsio0.cmsGetTagSignature(hProfile, n);
	}
	
	/**
	 * Returns TRUE if a tag with signature sig is found on the profile. Useful to check if a profile contains a given tag.
	 * @param hProfile Handle to a profile object.
	 * @param sig Tag signature
	 * @return TRUE if the tag is found, FALSE otherwise.
	 */
	public static boolean cmsIsTag(cmsHPROFILE hProfile, int sig)
	{
		return cmsio0.cmsIsTag(hProfile, sig);
	}
	
	// Read and write pre-formatted data
	/**
	 * Reads an existing tag with signature sig, parses it and returns a pointer to an object owned by the profile object holding a representation of tag contents.
	 * <p>
	 * LittleCMS will return (if found) a pointer to a structure holding the tag. Simple, but not simpler as the structure is not the contents of the tag, but the 
	 * result of <i>parsing</i> the tag. For example, reading a cmsSigAToB0 tag results as a Pipeline structure ready to be used by all the cmsPipeline functions. The 
	 * memory belongs to the profile and is set free on closing the profile. In this way there are no memory duplicates and you can safely re-use the same tag as many 
	 * times as you wish.
	 * @param hProfile Handle to a profile object.
	 * @param sig Tag signature
	 * @return A pointer to a profile-owned object holding tag contents, or NULL if the signature is not found. Type of object does vary.
	 */
	public static Object cmsReadTag(cmsHPROFILE hProfile, int sig)
	{
		return cmsio0.cmsReadTag(hProfile, sig);
	}
	
	/**
	 * Writes an object to an ICC profile tag, doing all necessary serialization. The obtained tag depends on ICC version number used when creating the profile.
	 * <p>
	 * Writing tags is almost the same as read them, you just specify a pointer to the structure and the tag name and Little CMS will do all serialization for you. 
	 * Process under the hood may be very complex, if you realize v2 and v4 of the ICC spec are using different representations of same structures.
	 * @param hProfile Handle to a profile object
	 * @param sig Tag signature
	 * @param data A pointer to an object holding tag contents. Type of object does vary.
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean cmsWriteTag(cmsHPROFILE hProfile, int sig, final Object data)
	{
		return cmsio0.cmsWriteTag(hProfile, sig, data);
	}
	
	/**
	 * Creates a directory entry on tag sig that points to same location as tag dest. Using this function you can collapse several tag entries to the same block in the 
	 * profile.
	 * @param hProfile Handle to a profile object
	 * @param sig Signature of linking tag
	 * @param dest Signature of linked tag
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean cmsLinkTag(cmsHPROFILE hProfile, int sig, int dest)
	{
		return cmsio0.cmsLinkTag(hProfile, dest);
	}
	
	// Read and write raw data
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
	 * The RAW version does the same as cmsWriteTag but without any interpretation of the data. Please note it is fair easy to deal with “cooked” structures, since 
	 * there are primitives for allocating, deleting and modifying data. For RAW data you are responsible of everything. If you want to deal with a private tag, you 
	 * may want to write a plug-in instead of messing up with raw data.
	 * @param hProfile Handle to a profile object
	 * @param sig Signature of tag to be written
	 * @param data Points to a memory block holding the data.
	 * @param Size Size of data in bytes
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean cmsWriteRawTag(cmsHPROFILE hProfile, iny sig, final byte[] data, iny Size)
	{
		return cmsio0.cmsWriteRawTag(hProfile, sig, data, Size);
	}
	
	// Access header data
	/**
	 * Get header flags of given ICC profile object. The profile flags field does contain flags to indicate various hints for the CMM such as distributed processing 
	 * and caching options. The least-significant 16 bits are reserved for the ICC. Flags in bit positions 0 and 1 shall be used as indicated:
	 * <p>
	 * <table border=1>
	 * <tr><th>Position</th><th>Field Length (bits)</th><th>Field Contents</th></tr>
	 * <tr><td>0</td><td>1</td><td>Embedded Profile (0 if not embedded, 1 if embedded in file)</td></tr>
	 * <tr><td>1</td><td>1</td><td>Profile cannot be used independently from the embedded colour data (set to 1 if true, 0 if false)</td></tr>
	 * </table>
	 * @param hProfile Handle to a profile object
	 * @return Flags field of profile header.
	 */
	public static int cmsGetHeaderFlags(cmsHPROFILE hProfile)
	{
		return cmsio0.cmsGetHeaderFlags(hProfile);
	}
	
	/**
	 * Gets the attribute flags
	 * @param hProfile Handle to a profile object
	 * @param Flags a pointer to a long to receive the flags.
	 */
	public static void cmsGetHeaderAttributes(cmsHPROFILE hProfile, long[] Flags)
	{
		cmsio0.cmsGetHeaderAttributes(hProfile, Flags);
	}
	
	/**
	 * Retrieves the Profile ID stored in the profile header.
	 * @param hProfile Handle to a profile object
	 * @param ProfileID Pointer to a Profile ID union
	 */
	public static void cmsGetHeaderProfileID(cmsHPROFILE hProfile, cmsProfileID ProfileID)
	{
		cmsio0.cmsGetHeaderProfileID(hProfile, ProfileID);
	}
	
	/**
	 * Returns the date and time when profile was created. This is a field stored in profile header.
	 * @param hProfile Handle to a profile object
	 * @param Dest pointer to struct tm object to hold the result.
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean cmsGetHeaderCreationDateTime(cmsHPROFILE hProfile, Calendar Dest)
	{
		return cmsio0.cmsGetHeaderCreationDateTime(hProfile, Dest);
	}
	
	/**
	 * Gets the profile header rendering intent. From the ICC spec: <i>"The rendering intent field shall specify the rendering intent which should be used (or, in the 
	 * case of a Devicelink profile, was used) when this profile is (was) combined with another profile. In a sequence of more than two profiles, it applies to the 
	 * combination of this profile and the next profile in the sequence and not to the entire sequence. Typically, the user or application will set the rendering intent 
	 * dynamically at runtime or embedding time. Therefore, this flag may not have any meaning until the profile is used in some context, e.g. in a Devicelink or an 
	 * embedded source profile."</i>
	 * @param hProfile Handle to a profile object
	 * @return A int holding the intent code
	 */
	public static int cmsGetHeaderRenderingIntent(cmsHPROFILE hProfile)
	{
		return cmsio0.cmsGetHeaderRenderingIntent(hProfile);
	}
	
	/**
	 * Sets header flags of given ICC profile object.
	 * @param hProfile Handle to a profile object.
	 * @param Flags Flags field of profile header.
	 */
	public static void cmsSetHeaderFlags(cmsHPROFILE hProfile, int Flags)
	{
		cmsio0.cmsSetHeaderFlags(hProfile, Flags);
	}
	
	/**
	 * Returns the manufacturer signature as described in the header. This funcionality is widely superseded by the manufaturer tag. Of use only in elder profiles.
	 * @param hProfile Handle to a profile object
	 * @return The profile manufacturer signature stored in the header.
	 */
	public static int cmsGetHeaderManufacturer(cmsHPROFILE hProfile)
	{
		return cmsio0.cmsGetHeaderManufacturer(hProfile);
	}
	
	/**
	 * Sets the manufacturer signature in the header. This funcionality is widely superseded by the manufaturer tag. Of use only in elder profiles.
	 * @param hProfile Handle to a profile object.
	 * @param manufacturer The profile manufacturer signature to store in the header.
	 */
	public static void cmsSetHeaderManufacturer(cmsHPROFILE hProfile, int manufacturer)
	{
		cmsio0.cmsSetHeaderManufacturer(hProfile, manufacturer);
	}
	
	/**
	 * Returns the model signature as described in the header. This funcionality is widely superseded by the model tag. Of use only in elder profiles.
	 * @param hProfile Handle to a profile object
	 * @return The profile model signature stored in the header.
	 */
	public static int cmsGetHeaderModel(cmsHPROFILE hProfile)
	{
		return cmsio0.cmsGetHeaderModel(hProfile);
	}
	
	/**
	 * Sets the model signature in the profile header. This funcionality is widely superseded by the model tag. Of use only in elder profiles.
	 * @param hProfile Handle to a profile object
	 * @param model The profile model signature to store in the header.
	 */
	public static void cmsSetHeaderModel(cmsHPROFILE hProfile, int model)
	{
		cmsio0.cmsSetHeaderModel(hProfile, model);
	}
	
	/**
	 * Sets the attribute flags in the profile header.
	 * @param hProfile Handle to a profile object
	 * @param Flags The flags to be set.
	 */
	public static void cmsSetHeaderAttributes(cmsHPROFILE hProfile, long Flags)
	{
		cmsio0.cmsSetHeaderAttributes(hProfile, Flags);
	}
	
	/**
	 * Replaces the the Profile ID stored in the profile header.
	 * @param hProfile Handle to a profile object
	 * @param ProfileID Pointer to a Profile ID union
	 */
	public static void cmsSetHeaderProfileID(cmsHPROFILE hProfile, cmsProfileID ProfileID)
	{
		cmsio0.cmsSetHeaderProfileID(hProfile, ProfileID);
	}
	
	/**
	 * Sets the profile header rendering intent.
	 * @param hProfile Handle to a profile object
	 * @param RenderingIntent A int holding the intent code.
	 * @see #cmsGetHeaderRenderingIntent(cmsHPROFILE)
	 */
	public static void cmsSetHeaderRenderingIntent(cmsHPROFILE hProfile, int RenderingIntent)
	{
		cmsio0.cmsSetHeaderRenderingIntent(hProfile, RenderingIntent);
	}
	
	/**
	 * Gets the profile connection space used by the given profile, using the ICC convention.
	 * @param hProfile Handle to a profile object
	 * @return Profile's connection color space
	 */
	public static int cmsGetPCS(cmsHPROFILE hProfile)
	{
		return cmsio0.cmsGetPCS(hProfile);
	}
	
	/**
	 * Sets the profile connection space signature in profile header, using ICC convention.
	 * @param hProfile Handle to a profile object
	 * @param pcs Profile's connection color space
	 */
	public static void cmsSetPCS(cmsHPROFILE hProfile, int pcs)
	{
		cmsio0.cmsSetPCS(hProfile, pcs);
	}
	
	/**
	 * Gets the color space used by the given profile, using the ICC convention.
	 * @param hProfile Handle to a profile object
	 * @return Profile's color space
	 */
	public static int cmsGetColorSpace(cmsHPROFILE hProfile)
	{
		return cmsio0.cmsGetColorSpace(hProfile);
	}
	
	/**
	 * Sets the color space signature in profile header, using ICC convention.
	 * @param hProfile Handle to a profile object
	 * @param sig Profile's color space
	 */
	public static void cmsSetColorSpace(cmsHPROFILE hProfile, int sig)
	{
		cmsio0.cmsSetColorSpace(hProfile, sig);
	}
	
	/**
	 * Gets the device class signature from profile header.
	 * @param hProfile Handle to a profile object
	 * @return Device class of profile
	 */
	public static int cmsGetDeviceClass(cmsHPROFILE hProfile)
	{
		return cmsio0.cmsGetDeviceClass(hProfile);
	}
	
	/**
	 * Sets the device class signature in profile header.
	 * @param hProfile Handle to a profile object
	 * @param sig Device class of profile
	 */
	public static void cmsSetDeviceClass(cmsHPROFILE hProfile, int sig)
	{
		cmsio0.cmsSetDeviceClass(hProfile, sig);
	}
	
	/**
	 * Sets the ICC version in profile header. The version given as to this function as a float n.m is properly encoded.
	 * @param hProfile Handle to a profile object
	 * @param Version Profile version in readable floating point format.
	 */
	public static void cmsSetProfileVersion(cmsHPROFILE hProfile, double Version)
	{
		cmsio0.cmsSetProfileVersion(hProfile, Version);
	}
	
	/**
	 * Returns the profile ICC version. The version is decoded to readable floating point format.
	 * @param hProfile Handle to a profile object
	 * @return The profile ICC version, in readable floating point format.
	 */
	public static double cmsGetProfileVersion(cmsHPROFILE hProfile)
	{
		return cmsio0.cmsGetProfileVersion(hProfile);
	}
	
	/**
	 * Returns the profile ICC version in the same format as it is stored in the header.
	 * @param hProfile Handle to a profile object
	 * @return The encoded ICC profile version.
	 */
	public static int cmsGetEncodedICCversion(cmsHPROFILE hProfile)
	{
		return cmsio0.cmsGetEncodedICCversion(hProfile);
	}
	
	/**
	 * Sets the ICC version in profile header, without any decoding.
	 * @param hProfile Handle to a profile object
	 * @param Version Profile version in the same format as it will be stored in profile header.
	 */
	public static void cmsSetEncodedICCversion(cmsHPROFILE hProfile, int Version)
	{
		cmsio0.cmsSetEncodedICCversion(hProfile, Version);
	}
	
	//How profiles may be used
	public static final int LCMS_USED_AS_INPUT = 0;
	public static final int LCMS_USED_AS_OUTPUT = 1;
	public static final int LCMS_USED_AS_PROOF = 2;
	
	/**
	 * Returns TRUE if the requested intent is implemented in the given direction. Little CMS has a fallback strategy that allows to specify any rendering intent when 
	 * creating the transform, but the intent really being used may be another if the requested intent is not implemented.
	 * @param hProfile Handle to a profile object
	 * @param Intent A int holding the intent code
	 * @param UsedDirection Any one of the following: {@link #LCMS_USED_AS_INPUT}, {@link #LCMS_USED_AS_OUTPUT}, {@link #LCMS_USED_AS_PROOF}
	 * @return TRUE if the intent is implemented, FALSE otherwise.
	 */
	public static boolean cmsIsIntentSupported(cmsHPROFILE hProfile, int Intent, int UsedDirection)
	{
		return cmsio1.cmsIsIntentSupported(hProfile, Intent, UsedDirection);
	}
	
	/**
	 * Returns whatever a matrix-shaper is present in the profile. Note that a profile may hold matrix-shaper and CLUT as well.
	 * @param hProfile Handle to a profile object
	 * @return TRUE if the profile holds a matrix-shaper, FALSE otherwise.
	 */
	public static boolean cmsIsMatrixShaper(cmsHPROFILE hProfile)
	{
		return cmsio1.cmsIsMatrixShaper(hProfile);
	}
	
	/**
	 * Returns whatever a CLUT is present in the profile for the given intent and direction.
	 * @param hProfile Handle to a profile object
	 * @param Intent A int holding the intent code
	 * @param UsedDirection Any one of the following: {@link #LCMS_USED_AS_INPUT}, {@link #LCMS_USED_AS_OUTPUT}, {@link #LCMS_USED_AS_PROOF}
	 * @return TRUE if a CLUT is present for given intent and direction, FALSE otherwise.
	 */
	public static boolean cmsIsCLUT(cmsHPROFILE hProfile, int Intent, int UsedDirection)
	{
		return cmsio1.cmsIsCLUT(hProfile, Intent, UsedDirection);
	}
	
	// Translate form/to our notation to ICC
	/**
	 * Converts from Little CMS color space notation to ICC color space notation.
	 * @param OurNotation Little CMS color space notation
	 * @return Corresponding ICC color space notation or -1 on error.
	 */
	public static int _cmsICCcolorSpace(int OurNotation)
	{
		return cmspcs._cmsICCcolorSpace(OurNotation);
	}
	
	/**
	 * Converts from ICC color space notation to Little CMS color space notation.
	 * @param ProfileSpace  ICC color space notation
	 * @return Corresponding Little CMS value or -1 on error.
	 */
	public static int _cmsLCMScolorSpace(int ProfileSpace)
	{
		return cmspcs._cmsLCMScolorSpace(ProfileSpace);
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
	
	// Build a suitable formatter for the colorspace of this profile
	public static int cmsFormatterForColorspaceOfProfile(cmsHPROFILE hProfile, int nBytes, boolean lIsFloat)
	{
		return cmspack.cmsFormatterForColorspaceOfProfile(hProfile, nBytes, lIsFloat);
	}
	
	public static int cmsFormatterForPCSOfProfile(cmsHPROFILE hProfile, int nBytes, boolean lIsFloat)
	{
		return cmspack.cmsFormatterForPCSOfProfile(hProfile, nBytes, lIsFloat);
	}
	
	// Localized info
	public static int cmsInfoDescription  = 0;
	public static int cmsInfoManufacturer = 1;
	public static int cmsInfoModel        = 2;
	public static int cmsInfoCopyright    = 3;
	
	/**
	 * Gets several information strings from the profile, dealing with localization. Strings are returned as chars.
	 * @param hProfile Handle to a profile object
	 * @param Info A selector of which info to return
	 * @param LanguageCode first name language code from ISO-639/2.
	 * @param CountryCod first name region code from ISO-3166.
	 * @param Buffer pointer to a memory block to get the result. NULL to calculate size only
	 * @param BufferSize Amount of byes allocated in Buffer, or 0 to calculate size only.
	 * @return Number of required bytes to hold the result. 0 on error.
	 */
	public static int cmsGetProfileInfo(cmsHPROFILE hProfile, int Info, final String LanguageCode, final String CountryCod, StringBuffer Buffer, int BufferSize)
	{
		return cmsio1.cmsGetProfileInfo(hProfile, Info, LanguageCode, CountryCod, Buffer, BufferSize);
	}
	
	/**
	 * Gets several information strings from the profile, dealing with localization. Strings are returned as ASCII.
	 * @param hProfile Handle to a profile object
	 * @param Info A selector of which info to return
	 * @param LanguageCode first name language code from ISO-639/2.
	 * @param CountryCod first name region code from ISO-3166.
	 * @param Buffer pointer to a memory block to get the result. NULL to calculate size only
	 * @param BufferSize Amount of byes allocated in Buffer, or 0 to calculate size only.
	 * @return Number of required bytes to hold the result. 0 on error.
	 */
	public static int cmsGetProfileInfoASCII(cmsHPROFILE hProfile, int Info, final String LanguageCode, final String CountryCode, StringBuffer Buffer, int BufferSize)
	{
		return cmsio1.cmsGetProfileInfoASCII(hProfile, Info, LanguageCode, CountryCod, Buffer, BufferSize);
	}
	
	// IO handlers ----------------------------------------------------------------------------------------------------------
	
	public static class cmsIOHANDLER extends lcms2_plugin._cms_io_handler{};
	
	/**
	 * Creates an IO handler object from a disk-based file.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param FileName Full path of file resource
	 * @param AccessMode “r” to read, “w” to write.
	 * @return A pointer to an iohandler object on success, NULL on error.
	 */
	public static cmsIOHANDLER cmsOpenIOhandlerFromFile(cmsContext ContextID, final String FileName, final char AccessMode)
	{
		return cmsio0.cmsOpenIOhandlerFromFile(ContextID, FileName, AccessMode);
	}
	
	/**
	 * Creates an IO handler object from an already open stream.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @return A pointer to an iohandler object on success, NULL on error.
	 */
	public static cmsIOHANDLER cmsOpenIOhandlerFromStream(cmsContext ContextID, Stream Stream)
	{
		return cmsio0.cmsOpenIOhandlerFromStream(ContextID, Stream);
	}
	
	/**
	 * Creates an IO handler object from a memory block.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param Buffer Points to a block of contiguous memory containing the data
	 * @param size Buffer's size measured in bytes.
	 * @param AccessMode “r” to read, “w” to write.
	 * @return A pointer to an iohandler object on success, NULL on error.
	 */
	public static cmsIOHANDLER cmsOpenIOhandlerFromMem(cmsContext ContextID, byte[] Buffer, int size, final char AccessMode)
	{
		return cmsio0.cmsOpenIOhandlerFromFile(ContextID, Buffer, size, AccessMode);
	}
	
	/**
	 * Creates a void iohandler object (similar to a file iohandler on /dev/null). All read operations returns 0 bytes and sets the EOF flag. All write operations 
	 * discards the given data.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @return A pointer to an iohandler object on success, NULL on error.
	 */
	public static cmsIOHANDLER cmsOpenIOhandlerFromNULL(cmsContext ContextID)
	{
		return cmsio0.cmsOpenIOhandlerFromNULL(ContextID);
	}
	
	/**
	 * Closes the iohandler object, freeing any associated resources.
	 * @param io A pointer to an iohandler object.
	 * @return TRUE on success, FALSE on error. Note that on file write operations, the real flushing to disk may happen on closing the iohandler, so it is important to 
	 * check the return code.
	 */
	public static boolean cmsCloseIOhandler(cmsIOHANDLER io)
	{
		return cmsio0.cmsCloseIOhandler(io);
	}
	
	// MD5 message digest --------------------------------------------------------------------------------------------------
	
	/**
	 * Computes a MD5 checksum and stores it as Profile ID in the profile header.
	 * @param hProfile Handle to a profile object
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean cmsMD5computeID(cmsHPROFILE hProfile)
	{
		return cmsmd5.cmsMD5computeID(hProfile);
	}
	
	// Profile high level funtions ------------------------------------------------------------------------------------------
	
	/**
	 * Opens a file-based ICC profile returning a handle to it.
	 * @param ICCProfile File name w/ full path.
	 * @param sAccess "r" for normal operation, "w" for profile creation
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsOpenProfileFromFile(final String ICCProfile, final char sAccess)
	{
		return cmsio0.cmsOpenProfileFromFile(ICCProfile, sAccess);
	}
	
	/**
	 * Same as anterior, but allowing a ContextID to be passed through.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param ICCProfile File name w/ full path.
	 * @param sAccess "r" for normal operation, "w" for profile creation
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsOpenProfileFromFileTHR(cmsContext ContextID, final String ICCProfile, final char sAccess)
	{
		return cmsio0.cmsOpenProfileFromFileTHR(ContextID, ICCProfile, sAccess);
	}
	
	/**
	 * Opens a stream-based ICC profile returning a handle to it.
	 * @param ICCProfile stream holding the ICC profile.
	 * @param sAccess "r" for normal operation, "w" for profile creation
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsOpenProfileFromStream(Stream ICCProfile, final char sAccess)
	{
		return cmsio0.cmsOpenProfileFromStream(ICCProfile, sAccess);
	}
	
	/**
	 * Same as anterior, but allowing a ContextID to be passed through.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param ICCProfile stream holding the ICC profile.
	 * @param sAccess "r" for normal operation, "w" for profile creation
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsOpenProfileFromStreamTHR(cmsContext ContextID, Stream ICCProfile, final char sAccess)
	{
		return cmsio0.cmsOpenProfileFromStreamTHR(ContextID, ICCProfile, sAccess);
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
	 * Same as anterior, but allowing a ContextID to be passed through.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param MemPtr Points to a block of contiguous memory containing the profile
	 * @param dwSize Profile's size measured in bytes.
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsOpenProfileFromMemTHR(cmsContext ContextID, final byte[] MemPtr, int dwSize)
	{
		return cmsio0.cmsOpenProfileFromMemTHR(ContextID, MemPtr, dwSize);
	}
	
	/**
	 * Opens a profile, returning a handle to it. The profile access is described by an IOHANDLER. See IO handlers section for further details.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param io Pointer to a serialization object.
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsOpenProfileFromIOhandlerTHR(cmsContext ContextID, cmsIOHANDLER io)
	{
		return cmsio0.cmsOpenProfileFromIOhandlerTHR(ContextID, io);
	}
	
	/**
	 * Closes a profile handle and frees any associated resource. Can return error when creating disk profiles, as this function flushes the data to disk.
	 * @param hProfile Handle to a profile object.
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean cmsCloseProfile(cmsHPROFILE hProfile)
	{
		return cmsio0.cmsCloseProfile(hProfile);
	}
	
	/**
	 * Saves the contents of a profile to a given filename.
	 * @param hProfile Handle to a profile object
	 * @param FileName File name w/ full path.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean cmsSaveProfileToFile(cmsHPROFILE hProfile, final String FileName)
	{
		return cmsio0.cmsSaveProfileToFile(hProfile, FileName);
	}
	
	/**
	 * Saves the contents of a profile to a given stream.
	 * @param hProfile Handle to a profile object
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean cmsSaveProfileToStream(cmsHPROFILE hProfile, Stream Stream)
	{
		return cmsio0.cmsSaveProfileToStream(hProfile, Stream);
	}
	
	/**
	 * Same as anterior, but for memory blocks. In this case, a NULL as MemPtr means to calculate needed space only.
	 * @param hProfile Handle to a profile object.
	 * @param MemPtr Points to a block of contiguous memory with enough space to contain the profile
	 * @param BytesNeeded points to a int, where the function will store profile’s size measured in bytes.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean cmsSaveProfileToMem(cmsHPROFILE hProfile, byte[] MemPtr, int[] BytesNeeded)
	{
		return cmsio0.cmsSaveProfileToMem(hProfile, MemPtr, BytesNeeded);
	}
	
	/**
	 * Low-level save to IOHANDLER. It returns the number of bytes used to store the profile, or zero on error. io may be NULL and in this case no data is written--only 
	 * sizes are calculated.
	 * @param hProfile Handle to a profile object
	 * @param io Pointer to a serialization object.
	 * @return The number of bytes used to store the profile, or zero on error.
	 */
	public static int cmsSaveProfileToIOhandler(cmsHPROFILE hProfile, cmsIOHANDLER io)
	{
		return cmsio0.cmsSaveProfileToIOhandler(hProfile, io);
	}
	
	// Predefined virtual profiles ------------------------------------------------------------------------------------------
	
	/**
	 * Same as anterior, but allowing a ContextID to be passed through.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param WhitePoint The white point of the RGB device or space.
	 * @param Primaries The primaries in xyY of the device or space.
	 * @param TransferFunction 3 tone curves describing the device or space gamma.
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsCreateRGBProfileTHR(cmsContext ContextID, final cmsCIExyY WhitePoint, final cmsCIExyYTRIPLE Primaries, 
			final cmsToneCurve[] TransferFunction)
	{
		return cmsvirt.cmsCreateRGBProfileTHR(ContextID, WhitePoint, Primaries, TransferFunction);
	}
	
	/**
	 * This function creates a RGB profile based on White point, primaries and transfer functions. It populates following tags; this conform 
	 * a standard RGB Display Profile, and then I add (As per addendum II) chromaticity tag.
	 * <p>
	 * <table border=1>
	 * <tr><td>1</td><td>cmsSigProfileDescriptionTag</td></tr>
	 * <tr><td>2</td><td>cmsSigMediaWhitePointTag</td></tr>
	 * <tr><td>3</td><td>cmsSigRedColorantTag</td></tr>
	 * <tr><td>4</td><td>cmsSigGreenColorantTag</td></tr>
	 * <tr><td>5</td><td>cmsSigBlueColorantTag</td></tr>
	 * <tr><td>6</td><td>cmsSigRedTRCTag</td></tr>
	 * <tr><td>7</td><td>cmsSigGreenTRCTag</td></tr>
	 * <tr><td>8</td><td>cmsSigBlueTRCTag</td></tr>
	 * <tr><td>9</td><td>Chromatic adaptation Tag</td></tr>
	 * <tr><td>10</td><td>cmsSigChromaticityTag</td></tr>
	 * </table>
	 * @param WhitePoint The white point of the RGB device or space.
	 * @param Primaries The primaries in xyY of the device or space.
	 * @param TransferFunction 3 tone curves describing the device or space gamma.
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsCreateRGBProfile(final cmsCIExyY WhitePoint, final cmsCIExyYTRIPLE Primaries, final cmsToneCurve[] TransferFunction)
	{
		return cmsvirt.cmsCreateRGBProfile(WhitePoint, Primaries, TransferFunction);
	}
	
	/**
	 * Same as anterior, but allowing a ContextID to be passed through.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param WhitePoint The white point of the gray device or space.
	 * @param TransferFunction tone curve describing the device or space gamma.
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsCreateGrayProfileTHR(cmsContext ContextID, final cmsCIExyY WhitePoint, final cmsToneCurve TransferFunction)
	{
		return cmsvirt.cmsCreateGrayProfileTHR(ContextID, WhitePoint, TransferFunction);
	}
	
	/**
	 * This function creates a gray profile based on White point and transfer function. It populates following tags; this conform a 
	 * standard gray display profile.
	 * <p>
	 * <table border=1>
	 * <tr><td>1</td><td>cmsSigProfileDescriptionTag</td></tr>
	 * <tr><td>2</td><td>cmsSigMediaWhitePointTag</td></tr>
	 * <tr><td>3</td><td>cmsSigGrayTRCTag</td></tr>
	 * </table>
	 * @param WhitePoint The white point of the gray device or space.
	 * @param TransferFunction tone curve describing the device or space gamma.
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsCreateGrayProfile(final cmsCIExyY WhitePoint, final cmsToneCurve TransferFunction)
	{
		return cmsvirt.cmsCreateGrayProfile(WhitePoint, TransferFunction);
	}
	
	/**
	 * Same as anterior, but allowing a ContextID to be passed through.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param ColorSpace Any color space signiture.
	 * @param TransferFunctions tone curves describing the device or space linearization.
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsCreateLinearizationDeviceLinkTHR(cmsContext ContextID, int ColorSpace, final cmsToneCurve[] TransferFunctions)
	{
		return cmsvirt.cmsCreateLinearizationDeviceLinkTHR(ContextID, ColorSpace, TransferFunctions);
	}
	
	/**
	 * This is a devicelink operating in the target colorspace with as many transfer functions as components.
	 * @param ColorSpace Any color space signiture.
	 * @param TransferFunctions tone curves describing the device or space linearization.
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsCreateLinearizationDeviceLink(int ColorSpace, final cmsToneCurve[] TransferFunctions)
	{
		return cmsvirt.cmsCreateLinearizationDeviceLink(ColorSpace, TransferFunctions);
	}
	
	/**
	 * Same as anterior, but allowing a ContextID to be passed through.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param ColorSpace Any color space signiture. Currently only cmsSigCmykData is supported.
	 * @param Limit Amount of ink limiting in % (0..400%)
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsCreateInkLimitingDeviceLinkTHR(cmsContext ContextID, int ColorSpace, double Limit)
	{
		return cmsvirt.cmsCreateInkLimitingDeviceLinkTHR(ContextID, ColorSpace, Limit);
	}
	
	/**
	 * This is a devicelink operating in CMYK for ink-limiting.
	 * <p>
	 * Ink-limiting algorithm:
	 * <br>
	 * <code>
	 * Sum = C + M + Y + K<br>
	 * If Sum > InkLimit<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Ratio= 1 - (Sum - InkLimit) / (C + M + Y)<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;if Ratio <0<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Ratio=0<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;endif<br>
	 * Else<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Ratio=1<br>
	 * endif<br><br>
	 * C = Ratio * C<br>
	 * M = Ratio * M<br>
	 * Y = Ratio * Y<br>
	 * K: Does not change
	 * </code>
	 * @param ColorSpace Any color space signiture. Currently only cmsSigCmykData is supported.
	 * @param Limit Amount of ink limiting in % (0..400%)
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsCreateInkLimitingDeviceLink(int ColorSpace, double Limit)
	{
		return cmsvirt.cmsCreateInkLimitingDeviceLink(ColorSpace, Limit);
	}
	
	/**
	 * Same as anterior, but allowing a ContextID to be passed through.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param WhitePoint Lab reference white. NULL for D50.
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsCreateLab2ProfileTHR(cmsContext ContextID, final cmsCIExyY WhitePoint)
	{
		return cmsvirt.cmsCreateLab2ProfileTHR(ContextID, WhitePoint);
	}
	
	/**
	 * Creates a Lab -> Lab identity, marking it as v2 ICC profile. Adjustments for accomodating PCS endoing shall be done by Little CMS when using this profile.
	 * @param WhitePoint Lab reference white. NULL for D50.
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsCreateLab2Profile(final cmsCIExyY WhitePoint)
	{
		return cmsvirt.cmsCreateLab2Profile(WhitePoint);
	}
	
	/**
	 * Same as anterior, but allowing a ContextID to be passed through.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param WhitePoint Lab reference white. NULL for D50.
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsCreateLab4ProfileTHR(cmsContext ContextID, final cmsCIExyY WhitePoint)
	{
		return cmsvirt.cmsCreateLab4ProfileTHR(ContextID, WhitePoint);
	}
	
	/**
	 * Creates a Lab -> Lab identity, marking it as v4 ICC profile.
	 * @param WhitePoint Lab reference white. NULL for D50.
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsCreateLab4Profile(final cmsCIExyY WhitePoint)
	{
		return cmsvirt.cmsCreateLab4Profile(WhitePoint);
	}
	
	/**
	 * Same as anterior, but allowing a ContextID to be passed through.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsCreateXYZProfileTHR(cmsContext ContextID)
	{
		return cmsvirt.cmsCreateXYZProfileTHR(ContextID);
	}
	
	/**
	 * Creates a XYZ -> XYZ identity, marking it as v4 ICC profile. WhitePoint used in Absolute colorimetric intent is D50.
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsCreateXYZProfile()
	{
		return cmsvirt.cmsCreateXYZProfile();
	}
	
	/**
	 * Same as anterior, but allowing a ContextID to be passed through.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsCreate_sRGBProfileTHR(cmsContext ContextID)
	{
		return cmsvirt.cmsCreate_sRGBProfileTHR(ContextID);
	}
	
	/**
	 * Create an ICC virtual profile for sRGB space. sRGB is a standard RGB color space created cooperatively by HP and Microsoft in 1996 for use on monitors, printers, 
	 * and the Internet.
	 * <p>
	 * <table border=1>
	 * <tr><th>sRGB white point is D65.</th></tr>
	 * <tr><table border=1>
	 * <th>xyY</th><td>0.3127, 0.3291, 1.0</td>
	 * </table></tr>
	 * </table>
	 * <p>
	 * <table border=1>
	 * <tr><th>Primaries are <a href="http://en.wikipedia.org/wiki/Rec._709">ITU-R BT.709-5</a> (xYY)</th></tr>
	 * <tr><table border=1>
	 * <tr><th>R</th><td>0.6400, 0.3300, 1.0</td></tr>
	 * <tr><th>G</th><td>0.3000, 0.6000, 1.0</td></tr>
	 * <tr><th>B</th><td>0.1500, 0.0600, 1.0</td></tr>
	 * </table></tr>
	 * </table>
	 * <p>
	 * sRGB transfer functions are defined by:<br>
	 * <code>
	 * If R’<sub>sRGB</sub>,G’<sub>sRGB</sub>, B’<sub>sRGB</sub> < 0.04045<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;R = R’<sub>sRGB</sub> / 12.92<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;G = G’<sub>sRGB</sub> / 12.92<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;B = B’<sub>sRGB</sub> / 12.92
	 * <p>
	 * else if R’<sub>sRGB</sub>,G’<sub>sRGB</sub>, B’<sub>sRGB</sub> >= 0.04045<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;R = ((R’<sub>sRGB</sub> + 0.055) / 1.055)<sup>2.4</sup><br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;G = ((G’<sub>sRGB</sub> + 0.055) / 1.055)<sup>2.4</sup><br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;B = ((B’<sub>sRGB</sub> + 0.055) / 1.055)<sup>2.4</sup>
	 * </code>
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsCreate_sRGBProfile()
	{
		return cmsvirt.cmsCreate_sRGBProfile();
	}
	
	/**
	 * Same as anterior, but allowing a ContextID to be passed through.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param nLUTPoints Resulting colormap resolution
	 * @param Bright Bright increment. May be negative
	 * @param Contrast Contrast increment. May be negative.
	 * @param Hue Hue displacement in degree.
	 * @param Saturation Saturation increment. May be negative
	 * @param TempSrc Source white point temperatures
	 * @param TempDest Destination white point temperatures
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsCreateBCHSWabstractProfileTHR(cmsContext ContextID, int nLUTPoints, double Bright, double Contrast, double Hue, double Saturation, 
			int TempSrc, int TempDest)
	{
		return cmsvirt.cmsCreateBCHSWabstractProfileTHR(ContextID, nLUTPoints, Bright, Contrast, Hue, Saturation, TempSrc, TempDest);
	}
	
	/**
	 * Creates an abstract devicelink operating in Lab for Bright/Contrast/Hue/Saturation and white point translation. White points are specified as temperatures ºK
	 * @param nLUTPoints Resulting colormap resolution
	 * @param Bright Bright increment. May be negative
	 * @param Contrast Contrast increment. May be negative.
	 * @param Hue Hue displacement in degree.
	 * @param Saturation Saturation increment. May be negative
	 * @param TempSrc Source white point temperatures
	 * @param TempDest Destination white point temperatures
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsCreateBCHSWabstractProfile(int nLUTPoints, double Bright, double Contrast, double Hue, double Saturation, int TempSrc, int TempDest)
	{
		return cmsvirt.cmsCreateBCHSWabstractProfile(nLUTPoints, Bright, Contrast, Hue, Saturation, TempSrc, TempDest);
	}
	
	/**
	 * Same as anterior, but allowing a ContextID to be passed through.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsCreateNULLProfileTHR(cmsContext ContextID)
	{
		return cmsvirt.cmsCreateNULLProfileTHR(ContextID);
	}
	
	/**
	 * Creates a fake NULL profile. This profile return 1 channel as always 0. Is useful only for gamut checking tricks.
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsCreateNULLProfile()
	{
		return cmsvirt.cmsCreateNULLProfile();
	}
	
	// Converts a transform to a devicelink profile
	/**
	 * Generates a device-link profile from a given color transform. This profile can then be used by any other function accepting profile handle. Depending on the 
	 * specified version number, the implementation of the devicelink may vary. Accepted versions are in range 1.0…4.3
	 * @param hTransform Handle to a color transform object.
	 * @param Version The target devicelink version number.
	 * @param dwFlags A combination of bit-field constants (prefix is cmsFLAGS_*).
	 * @return A handle to an ICC profile object on success, NULL on error.
	 */
	public static cmsHPROFILE cmsTransform2DeviceLink(cmsHTRANSFORM hTransform, double Version, int dwFlags)
	{
		return cmsvirt.cmsTransform2DeviceLink(hTransform, Version, dwFlags);
	}
	
	// Intents ----------------------------------------------------------------------------------------------
	
	// ICC Intents
	public static final int INTENT_PERCEPTUAL                            = 0;
	public static final int INTENT_RELATIVE_COLORIMETRIC                 = 1;
	public static final int INTENT_SATURATION                            = 2;
	public static final int INTENT_ABSOLUTE_COLORIMETRIC                 = 3;
	
	// Non-ICC intents
	public static final int INTENT_PRESERVE_K_ONLY_PERCEPTUAL           = 10;
	public static final int INTENT_PRESERVE_K_ONLY_RELATIVE_COLORIMETRIC= 11;
	public static final int INTENT_PRESERVE_K_ONLY_SATURATION           = 12;
	public static final int INTENT_PRESERVE_K_PLANE_PERCEPTUAL          = 13;
	public static final int INTENT_PRESERVE_K_PLANE_RELATIVE_COLORIMETRIC=14;
	public static final int INTENT_PRESERVE_K_PLANE_SATURATION          = 15;
	
	// Call with NULL as parameters to get the intent count
	/**
	 * Fills a table with id-numbers and descriptions for all supported intents. Little CMS plug-in architecture allows to implement user-defined intents; use this 
	 * function to get info about such extended functionality. Call with NULL as parameters to get the intent count
	 * @param nMax Max array elements to fill.
	 * @param Codes Pointer to user-allocated array of int to hold the intent id-numbers
	 * @param Descriptions Pointer to a user allocated array of String to hold the intent names.
	 * @return Supported intents count.
	 */
	public static int cmsGetSupportedIntents(int nMax, int[] Codes, String[] Descriptions)
	{
		return cmscnvrt.cmsGetSupportedIntents(nMax, Codes, Descriptions);
	}
	
	// Flags
	
	/** Inhibit 1-pixel cache*/
	public static final int cmsFLAGS_NOCACHE                = 0x0040;
	/** Inhibit optimizations*/
	public static final int cmsFLAGS_NOOPTIMIZE             = 0x0100;
	/** Don't transform anyway*/
	public static final int cmsFLAGS_NULLTRANSFORM          = 0x0200;
	
	// Proofing flags
	/** Out of Gamut alarm*/
	public static final int cmsFLAGS_GAMUTCHECK             = 0x1000;
	/** Do softproofing*/
	public static final int cmsFLAGS_SOFTPROOFING           = 0x4000;
	
	// Misc
	public static final int cmsFLAGS_BLACKPOINTCOMPENSATION = 0x2000;
	/** Don't fix scum dot*/
	public static final int cmsFLAGS_NOWHITEONWHITEFIXUP    = 0x0004;
	/** Use more memory to give better accurancy*/
	public static final int cmsFLAGS_HIGHRESPRECALC         = 0x0400;
	/** Use less memory to minimize resouces*/
	public static final int cmsFLAGS_LOWRESPRECALC          = 0x0800;
	
	// For devicelink creation
	/** Create 8 bits devicelinks*/
	public static final int cmsFLAGS_8BITS_DEVICELINK       = 0x0008;
	/** Guess device class (for transform2devicelink)*/
	public static final int cmsFLAGS_GUESSDEVICECLASS       = 0x0020;
	/** Keep profile sequence for devicelink creation*/
	public static final int cmsFLAGS_KEEP_SEQUENCE          = 0x0080;
	
	// Specific to a particular optimizations
	/** Force CLUT optimization*/
	public static final int cmsFLAGS_FORCE_CLUT             = 0x0002;
	/** create postlinearization tables if possible*/
	public static final int cmsFLAGS_CLUT_POST_LINEARIZATION= 0x0001;
	/** create prelinearization tables if possible*/
	public static final int cmsFLAGS_CLUT_PRE_LINEARIZATION = 0x0010;
	
	// Fine-tune control over number of gridpoints
	public static int cmsFLAGS_GRIDPOINTS(int n)
	{
		return (n & 0xFF) << 16;
	}
	
	// CRD special
	public static final int cmsFLAGS_NODEFAULTRESOURCEDEF   = 0x01000000;
	
	// Transforms ---------------------------------------------------------------------------------------------------
	
	/**
	 * Same as anterior, but allowing a ContextID to be passed through.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param Input Handle to a profile object capable to work in input direction
	 * @param InputFormat A bit-field format specifier
	 * @param Output Handle to a profile object capable to work in output direction
	 * @param OutputFormat A bit-field format specifier
	 * @param Intent A int holding the intent code
	 * @param dwFlags A combination of bit-field constants (prefix is cmsFLAGS_*).
	 * @return A handle to a transform object on success, NULL on error.
	 */
	public static cmsHTRANSFORM cmsCreateTransformTHR(cmsContext ContextID, cmsHPROFILE Input, int InputFormat, cmsHPROFILE Output, int OutputFormat, int Intent, 
			int dwFlags)
	{
		return cmsxform.cmsCreateTransformTHR(ContextID, Input, InputFormat, Output, OutputFormat, Intent, dwFlags);
	}
	
	/**
	 * Creates a color transform for translating bitmaps.
	 * @param Input Handle to a profile object capable to work in input direction
	 * @param InputFormat A bit-field format specifier
	 * @param Output Handle to a profile object capable to work in output direction
	 * @param OutputFormat A bit-field format specifier
	 * @param Intent A int holding the intent code
	 * @param dwFlags A combination of bit-field constants (prefix is cmsFLAGS_*).
	 * @return A handle to a transform object on success, NULL on error.
	 */
	public static cmsHTRANSFORM cmsCreateTransform(cmsHPROFILE Input, int InputFormat, cmsHPROFILE Output, int OutputFormat, int Intent, int dwFlags)
	{
		return cmsxform.cmsCreateTransform(Input, InputFormat, Output, OutputFormat, Intent, dwFlags);
	}
	
	/**
	 * Same as anterior, but allowing a ContextID to be passed through.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param Input Handle to a profile object capable to work in input direction
	 * @param InputFormat A bit-field format specifier
	 * @param Output Handle to a profile object capable to work in output direction
	 * @param OutputFormat A bit-field format specifier
	 * @param Intent A int holding the intent code
	 * @param ProofingIntent A int holding the intent code
	 * @param dwFlags A combination of bit-field constants (prefix is cmsFLAGS_*).
	 * @return A handle to a transform object on success, NULL on error.
	 */
	public static cmsHTRANSFORM cmsCreateProofingTransformTHR(cmsContext ContextID, cmsHPROFILE Input, int InputFormat, cmsHPROFILE Output, int OutputFormat, 
			cmsHPROFILE Proofing, int Intent, int ProofingIntent, int dwFlags)
	{
		return cmsxform.cmsCreateProofingTransformTHR(ContextID, Input, InputFormat, Output, OutputFormat, Proofing, Intent, ProofingIntent, dwFlags);
	}
	
	/**
	 * Same as cmsCreateTransform(), but including soft-proofing. The obtained transform emulates the device described by the "Proofing" profile. Useful to preview final 
	 * result without rendering to the physical medium. To enable proofing and gamut check you need to include following flags:
	 * <p>
	 * <b>cmsFLAGS_GAMUTCHECK</b>: Color out of gamut are flagged to a fixed color defined by the function cmsSetAlarmCodes
	 * <p>
	 * <b>cmsFLAGS_SOFTPROOFING</b>: does emulate the Proofing device.
	 * @param Input Handle to a profile object capable to work in input direction
	 * @param InputFormat A bit-field format specifier
	 * @param Output Handle to a profile object capable to work in output direction
	 * @param OutputFormat A bit-field format specifier
	 * @param Intent A int holding the intent code
	 * @param ProofingIntent A int holding the intent code
	 * @param dwFlags A combination of bit-field constants (prefix is cmsFLAGS_*).
	 * @return A handle to a transform object on success, NULL on error.
	 */
	public static cmsHTRANSFORM cmsCreateProofingTransform(cmsHPROFILE Input, int InputFormat, cmsHPROFILE Output, int OutputFormat, cmsHPROFILE Proofing, int Intent, 
			int ProofingIntent, int dwFlags)
	{
		return cmsxform.cmsCreateProofingTransform(Input, InputFormat, Output, OutputFormat, Proofing, Intent, ProofingIntent, dwFlags);
	}
	
	/**
	 * Same as anterior, but allowing a ContextID to be passed through.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param hProfiles Array of handles to open profile objects.
	 * @param nProfiles Number of profiles in the array.
	 * @param InputFormat A bit-field format specifier
	 * @param OutputFormat A bit-field format specifier
	 * @param Intent A int holding the intent code
	 * @param dwFlags A combination of bit-field constants (prefix is cmsFLAGS_*).
	 * @return A handle to a transform object on success, NULL on error.
	 */
	public static cmsHTRANSFORM cmsCreateMultiprofileTransformTHR(cmsContext ContextID, cmsHPROFILE[] hProfiles, int nProfiles, int InputFormat, int OutputFormat, 
			int Intent, int dwFlags)
	{
		return cmsxform.cmsCreateMultiprofileTransformTHR(ContextID, hProfiles, nProfiles, InputFormat, OutputFormat, Intent, dwFlags);
	}
	
	/**
	 * @param hProfiles Array of handles to open profile objects.
	 * @param nProfiles Number of profiles in the array.
	 * @param InputFormat A bit-field format specifier
	 * @param OutputFormat A bit-field format specifier
	 * @param Intent A int holding the intent code
	 * @param dwFlags A combination of bit-field constants (prefix is cmsFLAGS_*).
	 * @return A handle to a transform object on success, NULL on error.
	 */
	public static cmsHTRANSFORM cmsCreateMultiprofileTransform(cmsHPROFILE[] hProfiles, int nProfiles, int InputFormat, int OutputFormat, int Intent, int dwFlags)
	{
		return cmsxform.cmsCreateMultiprofileTransform(hProfiles, nProfiles, InputFormat, OutputFormat, Intent, dwFlags);
	}
	
	/**
	 * Extended form of multiprofile color transform creation, exposing all parameters for each profile in the chain. All other transform cration functions are 
	 * wrappers to this call.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param nProfiles Number of profiles in the array.
	 * @param hProfiles Array of handles to open profile objects.
	 * @param BPC Array of black point compensation states
	 * @param Intents A int holding the intent code, as described in Intents section.
	 * @param hGamutProfile Handle to a profile holding gamut information for gamut check. Only used if cmsFLAGS_GAMUTCHECK specified. Set to NULL for no gamut check.
	 * @param nGamutPCSposition Position in the chain of Lab/XYZ PCS to check against gamut profile Only used if cmsFLAGS_GAMUTCHECK specified.
	 * @param InputFormat A bit-field format specifier as described in Formatters section.
	 * @param OutputFormat A bit-field format specifier as described in Formatters section.
	 * @param dwFlags A combination of bit-field constants (prefix is cmsFLAGS_*).
	 * @return A handle to a transform object on success, NULL on error.
	 */
	public static cmsHTRANSFORM cmsCreateExtendedTransform(cmsContext ContextID, int nProfiles, cmsHPROFILE[] hProfiles, boolean[] BPC, int[] Intents, 
			double[] AdaptationStates, cmsHPROFILE hGamutProfile, int nGamutPCSposition, int InputFormat, int OutputFormat, int dwFlags)
	{
		return cmsxform.cmsCreateExtendedTransform(ContextID, nProfiles, hProfiles, BPC, Intents, AdaptationStates, hGamutProfile, nGamutPCSposition, InputFormat,
				OutputFormat, dwFlags);
	}
	
	/**
	 * Closes a transform handle and frees any associated memory. This function does NOT free the profiles used to create the transform.
	 * @param hTransform Handle to a color transform object.
	 */
	public static void cmsDeleteTransform(cmsHTRANSFORM hTransform)
	{
		cmsxform.cmsDeleteTransform(hTransform);
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
	
	/**
	 * Sets the global codes used to mark out-out-gamut on Proofing transforms. Values are meant to be encoded in 16 bits.
	 * @param NewAlarm AlarmCodes: Array [16] of codes. ALL 16 VALUES MUST BE SPECIFIED, set to zero unused channels.
	 */
	public static void cmsSetAlarmCodes(short[] NewAlarm)
	{
		cmsxform.cmsSetAlarmCodes(NewAlarm);
	}
	
	/**
	 * Gets the current global codes used to mark out-out-gamut on Proofing transforms. Values are meant to be encoded in 16 bits.
	 * @param NewAlarm Array [16] of codes. ALL 16 VALUES WILL BE OVERWRITTEN.
	 */
	public static void cmsGetAlarmCodes(short[] NewAlarm)
	{
		cmsxform.cmsGetAlarmCodes(NewAlarm);
	}
	
	// Adaptation state for absolute colorimetric intent
	/**
	 * Sets adaptation state for absolute colorimetric intent, on all but cmsCreateExtendedTransform. Little CMS can handle incomplete 
	 * adaptation states.
	 * @param d Degree on adaptation 0=Not adapted, 1=Complete adaptation, in-between=Partial adaptation. Use negative values to return 
	 * the global state without changing it.
	 * @return Previous global adaptation state.
	 */
	public static double cmsSetAdaptationState(double d)
	{
		return cmsxform.cmsSetAdaptationState(d);
	}
	
	/**
	 * Returns the ContextID associated with a given transform.
	 * @param hTransform Handle to a color transform object.
	 * @return Pointer to a user-defined context cargo or NULL if no context
	 */
	public static cmsContext cmsGetTransformContextID(cmsHTRANSFORM hTransform)
	{
		return cmsxform.cmsGetTransformContextID(hTransform);
	}
	
	// PostScript ColorRenderingDictionary and ColorSpaceArray ----------------------------------------------------
	
	public static final int cmsPS_RESOURCE_CSA = 0;
	public static final int cmsPS_RESOURCE_CRD = cmsPS_RESOURCE_CSA + 1;
	
	// lcms2 unified method to access postscript color resources
	/**
	 * Little CMS 2 unified method to create postscript color resources. Serialization is performed by the given iohandler object.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param Type Either <b>cmsPS_RESOURCE_CSA</b> or <b>cmsPS_RESOURCE_CRD</b>
	 * @param hProfile Handle to a profile object
	 * @param Intent A int holding the intent code
	 * @param dwFlags A combination of bit-field constants (prefix is cmsFLAGS_*).
	 * @param io Pointer to a serialization object.
	 * @return The resource size in bytes on success, 0 on error.
	 */
	public static int cmsGetPostScriptColorResource(cmsContext ContextID, int Type, cmsHPROFILE hProfile, int Intent, int dwFlags, 
			cmsIOHANDLER io)
	{
		return cmsps2.cmsGetPostScriptColorResource(ContextID, Type, hProfile, Intent, dwFlags, io);
	}
	
	/**
	 * A wrapper on cmsGetPostScriptColorResource to simplify CSA generation.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param hProfile Handle to a profile object
	 * @param Intent A int holding the intent code
	 * @param dwFlags A combination of bit-field constants (prefix is cmsFLAGS_*).
	 * @param Buffer Pointer to a user-allocated memory block or NULL. If specified, It should be big enough to hold the generated resource.
	 * @param dwBufferLen Length of Buffer in bytes.
	 * @return The resource size in bytes on success, 0 on error.
	 */
	public static int cmsGetPostScriptCSA(cmsContext ContextID, cmsHPROFILE hProfile, int Intent, int dwFlags, byte[] Buffer, 
			int dwBufferLen)
	{
		return cmsps2.cmsGetPostScriptCSA(ContextID, hProfile, Intent, dwFlags, Buffer, dwBufferLen);
	}
	
	/**
	 * A wrapper on cmsGetPostScriptColorResource to simplify CRD generation.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param hProfile Handle to a profile object
	 * @param Intent A int holding the intent code
	 * @param dwFlags A combination of bit-field constants (prefix is cmsFLAGS_*).
	 * @param Buffer Pointer to a user-allocated memory block or NULL. If specified, It should be big enough to hold the generated resource.
	 * @param dwBufferLen Length of Buffer in bytes.
	 * @return The resource size in bytes on success, 0 on error.
	 */
	public static int cmsGetPostScriptCRD(cmsContext ContextID, cmsHPROFILE hProfile, int Intent, int dwFlags, byte[] Buffer, 
			int dwBufferLen)
	{
		return cmsps2.cmsGetPostScriptCRD(ContextID, hProfile, Intent, dwFlags, Buffer, dwBufferLen);
	}
	
	// IT8.7 / CGATS.17-200x handling -----------------------------------------------------------------------------
	
	/**
	 * Allocates an empty CGATS.17 object.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @return A handle to a CGATS.17 object on success, NULL on error.
	 */
	public static cmsHANDLE cmsIT8Alloc(cmsContext ContextID)
	{
		return cmscgats.cmsIT8Alloc(ContextID);
	}
	
	/**
	 * This function frees the CGATS.17 object. After a call to this function, all memory pointers associated with the object are freed and therefore no longer valid.
	 * @param hIT8 A handle to a CGATS.17 object.
	 */
	public static void cmsIT8Free(cmsHANDLE hIT8)
	{
		cmscgats.cmsIT8Free(hIT8);
	}
	
	// Tables
	/**
	 * This function returns the number of tables found in the current CGATS object.
	 * @param A handle to a CGATS.17 object.
	 * @return The number of tables on success, 0 on error.
	 */
	public static int cmsIT8TableCount(cmsHANDLE hIT8)
	{
		return cmscgats.cmsIT8TableCount(hIT8);
	}
	
	/**
	 * This function positions the IT8 object in a given table, identified by its position. Setting nTable to Table Count + 1 does allocate a new empty table
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param nTable The table number (0 based)
	 * @return The current table number on success, -1 on error.
	 */
	public static int cmsIT8SetTable(cmsHANDLE hIT8, int nTable)
	{
		return cmscgats.cmsIT8SetTable(hIT8, nTable);
	}
	
	// Persistence
	/**
	 * This function allocates a CGATS.17 object and fills it with the contents of cFileName. Used for reading existing CGATS files.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param cFileName The CGATS.17 file name to read/parse
	 * @return A handle to a CGATS.17 on success, NULL on error.
	 */
	public static cmsHANDLE cmsIT8LoadFromFile(cmsContext ContextID, final String cFileName)
	{
		return cmscgats.cmsIT8LoadFromFile(ContextID, cFileName);
	}
	
	/**
	 * Same as anterior, but the IT8/CGATS.13 stream is read from a memory block.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param Ptr Points to a block of contiguous memory containing the CGATS.17 stream.
	 * @param len stream size measured in bytes.
	 * @return A handle to a CGATS.17 on success, NULL on error.
	 */
	public static cmsHANDLE cmsIT8LoadFromMem(cmsContext ContextID, byte[] Ptr, int len)
	{
		return cmscgats.cmsIT8LoadFromMem(ContextID, Ptr, len);
	}
	
	/*
	public static cmsHANDLE cmsIT8LoadFromIOhandler(cmsContext ContextID, cmsIOHANDLER io)
	{
		return cmscgats.cmsIT8LoadFromIOhandler(ContextID, io);
	}
	*/
	
	/**
	 * This function saves a CGATS.17 object to a file.
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param cFileName Destination filename. Existing file will be overwritten if possible.
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean cmsIT8SaveToFile(cmsHANDLE hIT8, final String cFileName)
	{
		return cmscgats.cmsIT8SaveToFile(hIT8, cFileName);
	}
	
	/**
	 * This function saves a CGATS.17 object to a contiguous memory block. Setting MemPtr to NULL forces the function to calculate the needed amount of memory.
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param MemPtr Pointer to a user-allocated memory block or NULL. If specified, It should be big enough to hold the generated resource.
	 * @param BytesNeeded Points to a user-allocated int which will receive the needed memory size in bytes.
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean cmsIT8SaveToMem(cmsHANDLE hIT8, byte[] MemPtr, int[] BytesNeeded)
	{
		return cmscgats.cmsIT8SaveToMem(hIT8, MemPtr, BytesNeeded);
	}
	
	// Properties
	/**
	 * This function returns the type of the IT8 object. Memory is handled by the CGATS.17 object and should not be freed by the user.
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @return A pointer to internal block of memory containing the type on success, NULL on error.
	 */
	public static String cmsIT8GetSheetType(cmsHANDLE hIT8)
	{
		return cmscgats.cmsIT8GetSheetType(hIT8);
	}
	
	/**
	 * This function sets the type of a CGATS.17 object
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param Type The new type
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean cmsIT8SetSheetType(cmsHANDLE hIT8, final String Type)
	{
		return cmscgats.cmsIT8SetSheetType(hIT8, Type);
	}
	
	/**
	 * This function is intended to provide a way automated IT8 creators can embed comments into the file. Comments have no effect, and its only purpose is to document 
	 * any of the file meaning. On this function the calling order is important; as successive calls to cmsIT8SetComment do embed comments in the same order the 
	 * function is being called.
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param cComment The comment to inserted
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean cmsIT8SetComment(cmsHANDLE hIT8, final String cComment)
	{
		return cmscgats.cmsIT8SetSheetType(hIT8, cComment);
	}
	
	/**
	 * Sets a property as a literal string in current table. The string is enclosed in quotes "".
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param cProp A string holding property name.
	 * @param Str The literal string.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean cmsIT8SetPropertyStr(cmsHANDLE hIT8, final String cProp, final String Str)
	{
		return cmscgats.cmsIT8SetPropertyStr(hIT8, cProp, Str);
	}
	
	/**
	 * Sets a property as a double in current table.
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param cProp A string holding property name.
	 * @param Val The data for the intended property as double.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean cmsIT8SetPropertyDbl(cmsHANDLE hIT8, final String cProp, double Val)
	{
		return cmscgats.cmsIT8SetPropertyDbl(hIT8, cProp, Val);
	}
	
	/**
	 * Sets a property as an hexadecimal constant (appends 0x) in current table.
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param cProp A string holding property name.
	 * @param Val The value to be set (32 bits max)
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean cmsIT8SetPropertyHex(cmsHANDLE hIT8, final String cProp, int Val)
	{
		return cmscgats.cmsIT8SetPropertyHex(hIT8, cProp, Val);
	}
	
	/**
	 * Sets a property with no interpretation in current table. No quotes "" are added. No checking is performed, and it is up to the programmer to make sure the string 
	 * is valid.
	 * <p>
	 * Special prefixes:<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;0b : Binary<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;0x : Hexadecimal
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param Key A string holding property name.
	 * @param Buffer A string holding the uncooked value to place in the CGATS file.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean cmsIT8SetPropertyUncooked(cmsHANDLE hIT8, final String Key, final String Buffer)
	{
		return cmscgats.cmsIT8SetPropertyUncooked(hIT8, Key, Buffer);
	}
	
	/**
	 * Gets a property as a literal string in current table. Memory is handled by the CGATS.17 object and should not be freed by the user.
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param cProp A string holding property name.
	 * @return A pointer to internal block of memory containing the data for the intended property on success, NULL on error.
	 */
	public static String cmsIT8GetProperty(cmsHANDLE hIT8, final String cProp)
	{
		return cmscgats.cmsIT8GetProperty(hIT8, cProp);
	}
	
	/**
	 * Gets a property as a double in current table.
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param cProp A string holding property name.
	 * @return The data for the intended property interpreted as double on success, 0 on error.
	 */
	public static double cmsIT8GetPropertyDbl(cmsHANDLE hIT8, final String cProp)
	{
		return cmscgats.cmsIT8GetPropertyDbl(hIT8, cProp);
	}
	
	/**
	 * Enumerates all properties in current table.
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param PropertyNames A pointer to a variable of type String[] which will receive the table of property name strings.
	 * @return The number of properties in current table on success, 0 on error.
	 */
	public static int cmsIT8EnumProperties(cmsHANDLE hIT8, String[] PropertyNames)
	{
		return cmscgats.cmsIT8EnumProperties(hIT8, PropertyNames);
	}
	
	// Datasets
	/**
	 * Gets a cell [row, col] as a literal string in current table. This function is fast since it has not to search columns or rows by name.
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param row The row position of the cell.
	 * @param col The column position of the cell.
	 * @return A pointer to internal block of memory containing the data for the intended cell on success, NULL on error.
	 */
	public static String cmsIT8GetDataRowCol(cmsHANDLE hIT8, int row, int col)
	{
		return cmscgats.cmsIT8GetDataRowCol(hIT8, row, col);
	}
	
	/**
	 * Gets a cell [row, col] as a double in current table. This function is fast since it has not to search columns or rows by name.
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param row The row position of the cell.
	 * @param col The column position of the cell.
	 * @return The data for the intended cell interpreted as double on success, 0 on error.
	 */
	public static double cmsIT8GetDataRowColDbl(cmsHANDLE hIT8, int row, int col)
	{
		return cmscgats.cmsIT8GetDataRowColDbl(hIT8, row, col);
	}
	
	/**
	 * Sets a cell [row, col] as a literal string in current table. This function is fast since it has not to search columns or rows by name.
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param row The row position of the cell.
	 * @param col The column position of the cell.
	 * @param Val The value to be set, as a literal string.
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean cmsIT8SetDataRowCol(cmsHANDLE hIT8, int row, int col, final String Val)
	{
		return cmscgats.cmsIT8SetDataRowCol(hIT8, row, col, Val);
	}
	
	/**
	 * Sets a cell [Patch, Sample] as a double in current table. This function is fast since it has not to search columns or rows by name.
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param row The row position of the cell.
	 * @param col The column position of the cell.
	 * @param Val The value to be set, as a double
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean cmsIT8SetDataRowColDbl(cmsHANDLE hIT8, int row, int col, double Val)
	{
		return cmscgats.cmsIT8SetDataRowColDbl(hIT8, row, col, Val);
	}
	
	/**
	 * Gets a cell [Patch, Sample] as a literal string (uncooked string) in current table. Memory is handled by the CGATS.17 object and 
	 * should not be freed by the user.
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param cPatch The intended patch name (row)
	 * @param cSample The intended sample name (column)
	 * @return A pointer to internal block of memory containing the data for the intended cell on success, NULL on error.
	 */
	public static String cmsIT8GetData(cmsHANDLE hIT8, final String cPatch, final String cSample)
	{
		return cmscgats.cmsIT8GetData(hIT8, cPatch, cSample);
	}
	
	/**
	 * Gets a cell [Patch, Sample] as a double in current table.
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param cPatch The intended patch name (row)
	 * @param cSample The intended sample name (column)
	 * @return The data for the intended cell interpreted as double on success, 0 on error.
	 */
	public static double cmsIT8GetDataDbl(cmsHANDLE hIT8, final String cPatch, final String cSample)
	{
		return cmscgats.cmsIT8GetDataDbl(hIT8, cPatch, cSample);
	}
	
	/**
	 * Sets a cell [Patch, Sample] as a literal string (uncooked string) in current table.
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param cPatch The intended patch name (row)
	 * @param cSample The intended sample name (column)
	 * @param Val The value to be set, as a literal
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean cmsIT8SetData(cmsHANDLE hIT8, final String cPatch, final String cSample, final String Val)
	{
		return cmscgats.cmsIT8SetData(hIT8, cPatch, cSample, Val);
	}
	
	/**
	 * Sets a cell [Patch, Sample] as a cmsFloat64Number in current table.
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param cPatch The intended patch name (row)
	 * @param cSample The intended sample name (column)
	 * @param Val The value to be set, as a double
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean cmsIT8SetDataDbl(cmsHANDLE hIT8, final String cPatch, final String cSample, double Val)
	{
		return cmscgats.cmsIT8SetDataDbl(hIT8, cPatch, cSample, Val);
	}
	
	/**
	 * Returns the position (column) of a given data sample name in current table. First column is 0 (SAMPLE_ID).
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @return Column number if found, -1 if not found
	 */
	public static int cmsIT8FindDataFormat(cmsHANDLE hIT8, final String cSample)
	{
		return cmscgats.cmsIT8FindDataFormat(hIT8, cSample);
	}
	
	/**
	 * Sets column names in current table. First column is 0 (SAMPLE_ID). Special property NUMBER_OF_FIELDS must be set before calling 
	 * this function.
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param n Column to set name
	 * @param Sample Name of data
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean cmsIT8SetDataFormat(cmsHANDLE hIT8, int n, final String Sample)
	{
		return cmscgats.cmsIT8SetDataFormat(hIT8, n, Sample);
	}
	
	/**
	 * Returns an array with pointers to the column names in current table. SampleNames may be NULL to get only the number of column names. 
	 * Memory is associated with the CGATS.17 object, and should not be freed by the user.
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param SampleNames A pointer to a variable of type String[] which will hold the table.
	 * @return The number of column names in table on success, -1 on error.
	 */
	public static int cmsIT8EnumDataFormat(cmsHANDLE hIT8, String[] SampleNames)
	{
		return cmscgats.cmsIT8EnumDataFormat(hIT8, SampleNames);
	}
	
	/**
	 * Fills buffer with the contents of SAMPLE_ID column for the set given in nPatch. That usually corresponds to patch name. Buffer may 
	 * be NULL to get the internal memory block used by the CGATS.17 object. If specified, buffer gets a copy of such block. In this case 
	 * it should have space for at least 1024 characters.
	 * @param hIT8 A handle to a CGATS.17 object.
	 * @param nPatch set number to retrieve name
	 * @param buffer A memory buffer to receive patch name, or NULL to allow function to return internal memory block.
	 * @return A pointer to the patch name, either the user-supplied buffer or an internal memory block. NULL if error.
	 */
	public static String cmsIT8GetPatchName(cmsHANDLE hIT8, int nPatch, StringBuffer buffer)
	{
		return cmscgats.cmsIT8GetPatchName(hIT8, nPatch, buffer);
	}
	
	// The LABEL extension
	public static int cmsIT8SetTableByLabel(cmsHANDLE hIT8, final String cSet, final String cField, final String ExpectedType)
	{
		return cmscgats.cmsIT8SetTableByLabel(hIT8, cSet, cField, ExpectedType);
	}
	
	// Formatter for double
	/**
	 * Sets the format string for float numbers. It uses the “C” sprintf convention. The default format string is "%.10g"
	 * @param A handle to a CGATS.17 object.
	 */
	public static void cmsIT8DefineDblFormat(cmsHANDLE hIT8, final String Formatter)
	{
		cmscgats.cmsIT8DefineDblFormat(hIT8, Formatter);
	}
	
	// Gamut boundary description routines ------------------------------------------------------------------------------
	
	/**
	 * Allocates an empty gamut boundary descriptor with no known points.
	 * @param Pointer to a user-defined context cargo.
	 * @return A handle to a gamut boundary descriptor on success, NULL on error.
	 */
	public static cmsHANDLE cmsGBDAlloc(cmsContext ContextID)
	{
		return cmssm.cmsGBDAlloc(ContextID);
	}
	
	/**
	 * Frees a gamut boundary descriptor and any associated resources.
	 * @param hGBD Handle to a gamut boundary descriptor.
	 */
	public static void cmsGBDFree(cmsHANDLE hGBD)
	{
		cmssm.cmsGBDFree(hGBD);
	}
	
	/**
	 * Adds a new sample point for computing the gamut boundary descriptor. This function can be called as many times as known points. No 
	 * memory or other resurces are wasted by adding new points. The gamut boundary descriptor cannot be checked until cmsGDBCompute() is 
	 * called.
	 * @param hGBD Handle to a gamut boundary descriptor.
	 * @param Lab Pointer to a cmsCIELab value
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean cmsGDBAddPoint(cmsHANDLE hGBD, final cmsCIELab Lab)
	{
		return cmssm.cmsGDBAddPoint(hGBD, Lab);
	}
	
	/**
	 * Computes the gamut boundary descriptor using all know points and interpolating any missing sector(s). Call this function after 
	 * adding all know points with cmsGDBAddPoint() and before using cmsGDBCheckPoint().
	 * @param hGDB Handle to a gamut boundary descriptor.
	 * @param dwFlags reserved (unused). Set it to 0
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean cmsGDBCompute(cmsHANDLE hGDB, int dwFlags)
	{
		return cmssm.cmsGDBCompute(hGDB, dwFlags);
	}
	
	/**
	 * Checks whatever a Lab value is inside a given gamut boundary descriptor.
	 * @param hGBD Handle to a gamut boundary descriptor.
	 * @param Lab Pointer to a cmsCIELab value
	 * @return TRUE if point is inside gamut, FALSE otherwise.
	 */
	public static boolean cmsGDBCheckPoint(cmsHANDLE hGBD, final cmsCIELab Lab)
	{
		return cmssm.cmsGDBCheckPoint(hGBD, Lab);
	}
	
	// Feature detection  ----------------------------------------------------------------------------------------------
	
	// Estimate the black point
	/**
	 * Estimate the black point of a given profile. Used by black point compensation algorithm.
	 * @param BlackPoint Pointer to cmsCIEXYZ object to receive the detected black point.
	 * @param hProfile Handle to a profile object
	 * @param Intent A int holding the intent code
	 * @param dwFlags reserved (unused). Set it to 0
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean cmsDetectBlackPoint(cmsCIEXYZ BlackPoint, cmsHPROFILE hProfile, int Intent, int dwFlags)
	{
		return cmssamp.cmsDetectBlackPoint(BlackPoint, hProfile, Intent, dwFlags);
	}
	
	// Estimate total area coverage
	/**
	 * When several colors are printed on top of each other, there is a limit to the amount of ink that can be put on paper. This maximum 
	 * total dot percentage is referred to as either TIC (Total Ink Coverage) or TAC (Total Area Coverage). This function does estimate 
	 * total area coverage for a given profile in %. Only works on output profiles. On RGB profiles, 400% is returned. TAC is detected 
	 * by subsampling Lab color space on 6x74x74 points.
	 * @param hProfile Handle to a profile object
	 * @return Estimated area coverage in % on success, 0 on error.
	 */
	public static double cmsDetectTAC(cmsHPROFILE hProfile)
	{
		return cmsgmt.cmsDetectTAC(hProfile);
	}
	
	// Poor man's gamut mapping
	/**
	 * @param Lab Pointer to a cmsCIELab value
	 * @param amax Horizontal maximum boundary of gamut rectangle
	 * @param amin Horizontal minimum boundary of gamut rectangle
	 * @param bmax Vertical maximum boundary of gamut rectangle
	 * @param bmin Vertical minimum boundary of gamut rectangle
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean cmsDesaturateLab(cmsCIELab Lab, double amax, double amin, double bmax, double bmin)
	{
		return cmsgmt.cmsDetectTAC(Lab, amax, amin, bmax, bmin);
	}
}
