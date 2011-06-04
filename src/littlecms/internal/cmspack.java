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
import littlecms.internal.helper.VirtualPointer;
import littlecms.internal.lcms2.cmsCIELab;
import littlecms.internal.lcms2.cmsCIEXYZ;
import littlecms.internal.lcms2.cmsHPROFILE;
import littlecms.internal.lcms2_internal._cmsTRANSFORM;
import littlecms.internal.lcms2_plugin.cmsFormatter;
import littlecms.internal.lcms2_plugin.cmsFormatter16;
import littlecms.internal.lcms2_plugin.cmsFormatterFactory;
import littlecms.internal.lcms2_plugin.cmsFormatterFloat;
import littlecms.internal.lcms2_plugin.cmsPluginBase;
import littlecms.internal.lcms2_plugin.cmsPluginFormatters;

/**
 * This module handles all formats supported by lcms. There are two flavors, 16 bits and 
 * floating point. Floating point is supported only in a subset, those formats holding
 * cmsFloat32Number (4 bytes per component) and double (marked as 0 bytes per component as special 
 * case)
 */
//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmspack
{
	// This macro return words stored as big endian
	private static short CHANGE_ENDIAN(short w){return (short)(((w)<<8)|((w)>>8));}
	
	// These macros handles reversing (negative)
	private static byte REVERSE_FLAVOR_8(byte x){return ((byte)(0xff-(x & 0xFF)));}
	private static short REVERSE_FLAVOR_16(short x){return ((short)(0xffff-(x & 0xFFFF)));}
	
	// * 0xffff / 0xff00 = (255 * 257) / (255 * 256) = 257 / 256
	private static short FomLabV2ToLabV4(short x)
	{
	    int a;
	    
	    a = x & 0xFFFF;
	    a = (a << 8 | a) >> 8;  // * 257 / 256
	    if ( a > 0xffff)
	    {
	    	return (short)0xffff;
	    }
	    return (short)a;
	}
	
	// * 0xf00 / 0xffff = * 256 / 257
	private static short FomLabV4ToLabV2(short x) 
	{
	    return (short)((((x & 0xFFFF) << 8) + 0x80) / 257);
	}
	
	public static class cmsFormatters16
	{
		public int Type;
		public int Mask;
		public cmsFormatter16 Frm;
		
		public cmsFormatters16(int Type, int Mask, cmsFormatter16 Frm)
		{
			this.Type = Type;
			this.Mask = Mask;
			this.Frm = Frm;
		}
	}
	
	public static class cmsFormattersFloat
	{
		public int Type;
		public int Mask;
		public cmsFormatterFloat Frm;
		
		public cmsFormattersFloat(int Type, int Mask, cmsFormatterFloat Frm)
		{
			this.Type = Type;
			this.Mask = Mask;
			this.Frm = Frm;
		}
	}
	
	public static final int ANYSPACE      = (31) << lcms2.COLORSPACE_SHIFT_VALUE;
	public static final int ANYCHANNELS   = (15) << lcms2.CHANNELS_SHIFT_VALUE;
	public static final int ANYEXTRA      = (7) << lcms2.EXTRA_SHIFT_VALUE;
	public static final int ANYPLANAR     = (1) << lcms2.PLANAR_SHIFT_VALUE;
	public static final int ANYENDIAN     = (1) << lcms2.ENDIAN16_SHIFT_VALUE;
	public static final int ANYSWAP       = (1) << lcms2.DOSWAP_SHIFT_VALUE;
	public static final int ANYSWAPFIRST  = (1) << lcms2.SWAPFIRST_SHIFT_VALUE; 
	public static final int ANYFLAVOR     = (1) << lcms2.FLAVOR_SHIFT_VALUE;
	
	// Unpacking routines (16 bits) ----------------------------------------------------------------------------------------
	
	// Check if space is marked as ink
	private static boolean IsInkSpace(int Type)
	{
	    switch (lcms2.T_COLORSPACE(Type))
	    {
		    case lcms2.PT_CMY:
		    case lcms2.PT_CMYK:
		    case lcms2.PT_MCH5:
		    case lcms2.PT_MCH6:
		    case lcms2.PT_MCH7:
		    case lcms2.PT_MCH8:
		    case lcms2.PT_MCH9:
		    case lcms2.PT_MCH10:
		    case lcms2.PT_MCH11:
		    case lcms2.PT_MCH12:
		    case lcms2.PT_MCH13:
		    case lcms2.PT_MCH14:
		    case lcms2.PT_MCH15:
		    	return true;
		    
		    default:
		    	return false;
	    }
	}
	
	// ----------------------------------------------------------------------------------------------------------------
	
	private static cmsFormatters16[] InputFormatters16;
	
	private static void setupInputFormatter16()
	{
		InputFormatters16 = new cmsFormatters16[]{
			//			    Type                             Mask                              Function
		    //  ----------------------------   ------------------------------------  ----------------------------
			new cmsFormatters16(lcms2.TYPE_Lab_DBL, ANYPLANAR|ANYEXTRA, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					// This is a conversion of Lab double to 16 bits
					
					//UnrollLabDoubleTo16
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					if (lcms2.T_PLANAR(CMMcargo.InputFormat))
					{
						int pos = Buffer.getPosition();
						
				        cmsCIELab Lab = new cmsCIELab();
				        
				        Lab.L = proc.readDouble();
				        Buffer.movePosition(Stride * 8);
				        Lab.a = proc.readDouble();
				        Buffer.movePosition(Stride * 8);
				        Lab.b = proc.readDouble();
				        
				        cmspcs.cmsFloat2LabEncoded(Values, Lab);
				        Buffer.setPosition(pos + /*sizeof(cmsFloat64Number)*/8);
				    }
				    else
				    {
				    	cmspcs.cmsFloat2LabEncoded(Values, (cmsCIELab)proc.readObject(cmsCIELab.class, true));
				    	proc.movePosition(lcms2.T_EXTRA(CMMcargo.InputFormat) * /*sizeof(cmsFloat64Number)*/8);
				    }
					return Buffer;
				}
			}),
			new cmsFormatters16(lcms2.TYPE_XYZ_DBL, ANYPLANAR|ANYEXTRA, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					// This is a conversion of XYZ double to 16 bits
					
					//UnrollXYZDoubleTo16
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					if (lcms2.T_PLANAR(CMMcargo.InputFormat))
					{
						int pos = Buffer.getPosition();
						
						cmsCIEXYZ XYZ = new cmsCIEXYZ();
				        
						XYZ.X = proc.readDouble();
						Buffer.movePosition(Stride * 8);
				        XYZ.Y = proc.readDouble();
				        Buffer.movePosition(Stride * 8);
				        XYZ.Z = proc.readDouble();
				        
				        cmspcs.cmsFloat2XYZEncoded(Values, XYZ);
				        Buffer.setPosition(pos + /*sizeof(cmsFloat64Number)*/8);
				    }
				    else
				    {
				    	cmspcs.cmsFloat2XYZEncoded(Values, (cmsCIEXYZ)proc.readObject(cmsCIEXYZ.class, true));
				    	proc.movePosition(lcms2.T_EXTRA(CMMcargo.InputFormat) * /*sizeof(cmsFloat64Number)*/8);
				    }
					return Buffer;
				}
			}),
			new cmsFormatters16(lcms2.TYPE_GRAY_DBL, 0, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					// For 1 channel, we need to duplicate data (it comes in 0..1.0 range)
					
					//UnrollDouble1Chan
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					Values[0] = Values[1] = Values[2] = lcms2_internal._cmsQuickSaturateWord(Buffer.getProcessor().readDouble(true) * 65535.0);
					
					return Buffer;
				}
			}),
			new cmsFormatters16((1 << lcms2.FLOAT_SHIFT_VALUE)|(0 << lcms2.BYTES_SHIFT_VALUE), ANYCHANNELS|ANYPLANAR|ANYEXTRA|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					// Inks does come in percentage, remaining cases are between 0..1.0, again to 16 bits
					
					//UnrollDoubleTo16
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
				    int nChan      = lcms2.T_CHANNELS(CMMcargo.InputFormat);
				    boolean Planar = lcms2.T_PLANAR(CMMcargo.InputFormat);
				    int i;
				    double v;
				    double maximum = IsInkSpace(CMMcargo.InputFormat) ? 655.35 : 65535.0;
				    
				    int pos = Buffer.getPosition();
				    VirtualPointer.TypeProcessor Inks = Buffer.getProcessor();
				    for (i = 0; i < nChan; i++)
				    {
				        if (Planar)
				        {
				        	Buffer.setPosition(pos + ((i * Stride) * 8));
				        	v = Inks.readDouble();
				        }
				        else
				        {
				        	Buffer.setPosition(pos + (i * 8));
				        	v = Inks.readDouble();
				        }
				        
				        Values[i] = lcms2_internal._cmsQuickSaturateWord(v * maximum);
				    }
				    
				    if (lcms2.T_PLANAR(CMMcargo.InputFormat))
				    {
				    	Buffer.setPosition(pos + /*sizeof(cmsFloat64Number)*/8);
				    }
				    else
				    {
				    	Buffer.setPosition(pos + (nChan + lcms2.T_EXTRA(CMMcargo.InputFormat)) * /*sizeof(cmsFloat64Number)*/8);
				    }
				    return Buffer;
				}
			}),
			new cmsFormatters16((1 << lcms2.FLOAT_SHIFT_VALUE)|(4 << lcms2.BYTES_SHIFT_VALUE), ANYCHANNELS|ANYPLANAR|ANYEXTRA|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//UnrollFloatTo16
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
				    int nChan      = lcms2.T_CHANNELS(CMMcargo.InputFormat);
				    boolean Planar = lcms2.T_PLANAR(CMMcargo.InputFormat);
				    int i;
				    float v;
				    double maximum = IsInkSpace(CMMcargo.InputFormat) ? 655.35 : 65535.0;
				    
				    int pos = Buffer.getPosition();
				    VirtualPointer.TypeProcessor Inks = Buffer.getProcessor();
				    for (i = 0; i < nChan; i++)
				    {
				        if (Planar)
				        {
				        	Buffer.setPosition(pos + ((i * Stride) * 4));
				        	v = Inks.readFloat();
				        }
				        else
				        {
				        	Buffer.setPosition(pos + (i * 4));
				        	v = Inks.readFloat();
				        }
				        
				        Values[i] = lcms2_internal._cmsQuickSaturateWord(v * maximum);
				    }
				    
				    if (lcms2.T_PLANAR(CMMcargo.InputFormat))
				    {
				    	Buffer.setPosition(pos + /*sizeof(cmsFloat32Number)*/4);
				    }
				    else
				    {
				    	Buffer.setPosition(pos + (nChan + lcms2.T_EXTRA(CMMcargo.InputFormat)) * /*sizeof(cmsFloat32Number)*/4);
				    }
				    return Buffer;
				}
			}),
			
			
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					// Monochrome duplicates L into RGB for null-transforms
					
					//Unroll1Byte
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					Values[0] = Values[1] = Values[2] = lcms2_internal.FROM_8_TO_16(Buffer.getProcessor().readInt8(true));	// L
				    return Buffer;
				}
			}),
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Unroll1ByteSkip1
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					Values[0] = Values[1] = Values[2] = lcms2_internal.FROM_8_TO_16(Buffer.getProcessor().readInt8(true));	// L
					Buffer.movePosition(1);
				    return Buffer;
				}
			}),
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(2 << lcms2.EXTRA_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Unroll1ByteSkip2
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					Values[0] = Values[1] = Values[2] = lcms2_internal.FROM_8_TO_16(Buffer.getProcessor().readInt8(true));	// L
					Buffer.movePosition(2);
				    return Buffer;
				}
			}),
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.FLAVOR_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Unroll1ByteReversed
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					Values[0] = Values[1] = Values[2] = REVERSE_FLAVOR_16(lcms2_internal.FROM_8_TO_16(Buffer.getProcessor().readInt8(true)));	// L
				    return Buffer;
				}
			}),
			new cmsFormatters16((lcms2.PT_MCH2 << lcms2.COLORSPACE_SHIFT_VALUE)|(2 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE), 0, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					// for duplex
					
					//Unroll2Bytes
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Values[0] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true));	// ch1
					Values[1] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true));	// ch2
				    return Buffer;
				}
			}),
			
			new cmsFormatters16(lcms2.TYPE_LabV2_8, 0, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//UnrollLabV2_8
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Values[0] = FomLabV2ToLabV4(lcms2_internal.FROM_8_TO_16(proc.readInt8(true))); // L
					Values[1] = FomLabV2ToLabV4(lcms2_internal.FROM_8_TO_16(proc.readInt8(true))); // a
					Values[2] = FomLabV2ToLabV4(lcms2_internal.FROM_8_TO_16(proc.readInt8(true))); // b
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16(lcms2.TYPE_ALabV2_8, 0, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//UnrollALabV2_8
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Buffer.movePosition(1); // A
					Values[0] = FomLabV2ToLabV4(lcms2_internal.FROM_8_TO_16(proc.readInt8(true))); // L
					Values[1] = FomLabV2ToLabV4(lcms2_internal.FROM_8_TO_16(proc.readInt8(true))); // a
					Values[2] = FomLabV2ToLabV4(lcms2_internal.FROM_8_TO_16(proc.readInt8(true))); // b
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16(lcms2.TYPE_LabV2_16, 0, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//UnrollLabV2_16
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Values[0] = FomLabV2ToLabV4(proc.readInt16(true)); // L
					Values[1] = FomLabV2ToLabV4(proc.readInt16(true)); // a
					Values[2] = FomLabV2ToLabV4(proc.readInt16(true)); // b
				    
				    return Buffer;
				}
			}),
			
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Unroll3Bytes
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Values[0] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // R
					Values[1] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // G
					Values[2] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // B
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//BGR
					
					//Unroll3BytesSwap
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Values[2] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // B
					Values[1] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // G
					Values[0] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // R
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Unroll3BytesSkip1Swap
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Buffer.movePosition(1); // A
					Values[2] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // B
					Values[1] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // G
					Values[0] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // R
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Unroll3BytesSkip1SwapFirst
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Buffer.movePosition(1); // A
					Values[0] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // R
					Values[1] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // G
					Values[2] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // B
				    
				    return Buffer;
				}
			}),
			
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					// Special cases, provided for performance
					
					//Unroll4Bytes
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Values[0] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // C
					Values[1] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // M
					Values[2] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // Y
					Values[3] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // K
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.FLAVOR_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Unroll4BytesReverse
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Values[0] = lcms2_internal.FROM_8_TO_16(REVERSE_FLAVOR_8(proc.readInt8(true))); // C
					Values[1] = lcms2_internal.FROM_8_TO_16(REVERSE_FLAVOR_8(proc.readInt8(true))); // M
					Values[2] = lcms2_internal.FROM_8_TO_16(REVERSE_FLAVOR_8(proc.readInt8(true))); // Y
					Values[3] = lcms2_internal.FROM_8_TO_16(REVERSE_FLAVOR_8(proc.readInt8(true))); // K
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Unroll4BytesSwapFirst
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Values[3] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // K
					Values[0] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // C
					Values[1] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // M
					Values[2] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // Y
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					// KYMC
					
					//Unroll4BytesSwap
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Values[3] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // K
					Values[2] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // Y
					Values[1] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // M
					Values[0] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // C
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Unroll4BytesSwapSwapFirst
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Values[2] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // K
					Values[1] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // Y
					Values[0] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // M
					Values[3] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // C
				    
				    return Buffer;
				}
			}),
			
			new cmsFormatters16((1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.PLANAR_SHIFT_VALUE), ANYFLAVOR|ANYSWAP|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					// Extra channels are just ignored because come in the next planes
					
					//UnrollPlanarBytes
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					int nChan       = lcms2.T_CHANNELS(CMMcargo.InputFormat);
				    boolean DoSwap  = lcms2.T_DOSWAP(CMMcargo.InputFormat);
				    boolean Reverse = lcms2.T_FLAVOR(CMMcargo.InputFormat);
				    int i;
				    int Init = Buffer.getPosition();

				    if (DoSwap)
				    {
				    	Buffer.movePosition(lcms2.T_EXTRA(CMMcargo.InputFormat) * Stride);
				    }
				    
				    VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
				    for (i = 0; i < nChan; i++)
				    {
				        int index = DoSwap ? (nChan - i - 1) : i;
				        short v = lcms2_internal.FROM_8_TO_16(proc.readInt8());
				        
				        Values[index] = Reverse ? REVERSE_FLAVOR_16(v) : v;
				        Buffer.movePosition(Stride);
				    }
				    
				    Buffer.setPosition(Init + 1);
				    return Buffer;
				}
			}),
			new cmsFormatters16((1 << lcms2.BYTES_SHIFT_VALUE), ANYFLAVOR|ANYSWAPFIRST|ANYSWAP|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					// Does almost everything but is slow
					
					//UnrollChunkyBytes
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					int nChan          = lcms2.T_CHANNELS(CMMcargo.InputFormat);
				    boolean DoSwap     = lcms2.T_DOSWAP(CMMcargo.InputFormat);
				    boolean Reverse    = lcms2.T_FLAVOR(CMMcargo.InputFormat);
				    boolean SwapFirst  = lcms2.T_SWAPFIRST(CMMcargo.InputFormat);
				    int Extra          = lcms2.T_EXTRA(CMMcargo.InputFormat);
				    boolean ExtraFirst = DoSwap && !SwapFirst;
				    short v;
				    int i;
				    
				    if (ExtraFirst)
				    {
				    	Buffer.movePosition(Extra);
				    }
				    
				    VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
				    for (i = 0; i < nChan; i++)
				    {
				        int index = DoSwap ? (nChan - i - 1) : i;
				        
				        v = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); 
				        v = Reverse ? REVERSE_FLAVOR_16(v) : v;
				        Values[index] = v;
				    }
				    
				    if (!ExtraFirst)
				    {
				    	Buffer.movePosition(Extra);
				    }
				    
				    if (Extra == 0 && SwapFirst)
				    {
				        short tmp = Values[0];
				        
				        System.arraycopy(Values, 1, Values, 0, (nChan-1)/* * sizeof(cmsUInt16Number)*/);
				        Values[nChan-1] = tmp;
				    }
				    
				    return Buffer;
				}
			}),
			
			
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Unroll1Word
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					Values[0] = Values[1] = Values[2] = Buffer.getProcessor().readInt16(true); // L
				    return Buffer;
				}
			}),
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.FLAVOR_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Unroll1WordReversed
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					Values[0] = Values[1] = Values[2] = REVERSE_FLAVOR_16(Buffer.getProcessor().readInt16(true));
				    return Buffer;
				}
			}),
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(3 << lcms2.EXTRA_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Unroll1WordSkip3
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					Values[0] = Values[1] = Values[2] = Buffer.getProcessor().readInt16();
					
					Buffer.movePosition(8);
				    return Buffer;
				}
			}),
			
			new cmsFormatters16((2 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Unroll2Words
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Values[0] = proc.readInt16(true);	// ch1
					Values[1] = proc.readInt16(true);	// ch2
					
				    return Buffer;
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Unroll3Words
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Values[0] = proc.readInt16(true); // C R
					Values[1] = proc.readInt16(true); // M G
					Values[2] = proc.readInt16(true); // Y B
				    return Buffer;
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Unroll4Words
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Values[0] = proc.readInt16(true); // C
					Values[1] = proc.readInt16(true); // M
					Values[2] = proc.readInt16(true); // Y
					Values[3] = proc.readInt16(true); // K
				    
				    return Buffer;
				}
			}),
			
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Unroll3WordsSwap
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Values[2] = proc.readInt16(true); // C R
					Values[1] = proc.readInt16(true); // M G
					Values[0] = proc.readInt16(true); // Y B
				    return Buffer;
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Unroll3WordsSkip1SwapFirst
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Buffer.movePosition(2); //A
					Values[0] = proc.readInt16(true); // C R
					Values[1] = proc.readInt16(true); // M G
					Values[2] = proc.readInt16(true); // Y B
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Unroll3WordsSkip1Swap
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Buffer.movePosition(2); //A
					Values[2] = proc.readInt16(true); // C R
					Values[1] = proc.readInt16(true); // M G
					Values[0] = proc.readInt16(true); // Y B
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.FLAVOR_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Unroll4WordsReverse
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Values[0] = REVERSE_FLAVOR_16(proc.readInt16(true)); // C
					Values[1] = REVERSE_FLAVOR_16(proc.readInt16(true)); // M
					Values[2] = REVERSE_FLAVOR_16(proc.readInt16(true)); // Y
					Values[3] = REVERSE_FLAVOR_16(proc.readInt16(true)); // K
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Unroll4WordsSwapFirst
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Values[3] = proc.readInt16(true); // K
					Values[0] = proc.readInt16(true); // C
					Values[1] = proc.readInt16(true); // M
					Values[2] = proc.readInt16(true); // Y
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					// KYMC
					
					//Unroll4WordsSwap
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Values[3] = proc.readInt16(true); // K
					Values[2] = proc.readInt16(true); // Y
				    Values[1] = proc.readInt16(true); // M
				    Values[0] = proc.readInt16(true); // C
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Unroll4WordsSwapSwapFirst
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Values[2] = proc.readInt16(true); // K
					Values[1] = proc.readInt16(true); // Y
					Values[0] = proc.readInt16(true); // M
					Values[3] = proc.readInt16(true); // C
				    
				    return Buffer;
				}
			}),
			
			
			new cmsFormatters16((2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.PLANAR_SHIFT_VALUE), ANYFLAVOR|ANYSWAP|ANYENDIAN|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//UnrollPlanarWords
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					int nChan          = lcms2.T_CHANNELS(CMMcargo.InputFormat);
					boolean DoSwap     = lcms2.T_DOSWAP(CMMcargo.InputFormat);
				    boolean Reverse    = lcms2.T_FLAVOR(CMMcargo.InputFormat);
				    boolean SwapEndian = lcms2.T_ENDIAN16(CMMcargo.InputFormat);
				    int i;
				    int Init = Buffer.getPosition();
				    
				    if (DoSwap)
				    {
				    	Buffer.movePosition(lcms2.T_EXTRA(CMMcargo.InputFormat) * Stride * /*sizeof(cmsUInt16Number)*/2);
				    }
				    
				    VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
				    for (i = 0; i < nChan; i++)
				    {
				        int index = DoSwap ? (nChan - i - 1) : i;
				        short v = proc.readInt16();
				        
				        if (SwapEndian)
				        {
				        	v = CHANGE_ENDIAN(v);
				        }
				        
				        Values[index] = Reverse ? REVERSE_FLAVOR_16(v) : v;
				        
				        Buffer.movePosition(Stride * /*sizeof(cmsUInt16Number)*/2);
				    }
				    
				    Buffer.setPosition(Init + /*sizeof(cmsUInt16Number)*/2);
				    return Buffer;
				}
			}),
			new cmsFormatters16((2 << lcms2.BYTES_SHIFT_VALUE), ANYFLAVOR|ANYSWAPFIRST|ANYSWAP|ANYENDIAN|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//UnrollAnyWords
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					int nChan          = lcms2.T_CHANNELS(CMMcargo.InputFormat);
					boolean SwapEndian = lcms2.T_ENDIAN16(CMMcargo.InputFormat);
				    boolean DoSwap     = lcms2.T_DOSWAP(CMMcargo.InputFormat);
				    boolean Reverse    = lcms2.T_FLAVOR(CMMcargo.InputFormat);
				    boolean SwapFirst  = lcms2.T_SWAPFIRST(CMMcargo.InputFormat);
				    int Extra          = lcms2.T_EXTRA(CMMcargo.InputFormat);
				    boolean ExtraFirst = DoSwap && !SwapFirst;
				    int i;
				    
				    if (ExtraFirst)
				    {
				    	Buffer.movePosition(Extra * /*sizeof(cmsUInt16Number)*/2);
				    }
				    
				    VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
				    for (i = 0; i < nChan; i++)
				    {
				        int index = DoSwap ? (nChan - i - 1) : i;
				        short v = proc.readInt16(true);
				        
				        if (SwapEndian)
				        {
				        	v = CHANGE_ENDIAN(v);
				        }
				        
				        Values[index] = Reverse ? REVERSE_FLAVOR_16(v) : v;
				    }
				    
				    if (!ExtraFirst)
				    {
				    	Buffer.movePosition(Extra * /*sizeof(cmsUInt16Number)*/2);
				    }
				    
				    if (Extra == 0 && SwapFirst)
				    {
				        short tmp = Values[0];
				        
				        System.arraycopy(Values, 1, Values, 0, (nChan-1)/* * sizeof(cmsUInt16Number)*/);
				        Values[nChan-1] = tmp;
				    }
				    
				    return Buffer;
				}
			})
		};
	}
	
	private static final int DEFAULT_FORM_IN_16_COUNT = 39;
	
	// Bit fields set to one in the mask are not compared
	private static cmsFormatter _cmsGetStockInputFormatter(int dwInput, int dwFlags)
	{
	    int i;
	    cmsFormatter fr = new cmsFormatter();
	    
	    if ((dwFlags & lcms2_plugin.CMS_PACK_FLAGS_FLOAT) == 0)
	    {
	        for (i = 0; i < DEFAULT_FORM_IN_16_COUNT; i++)
	        {
	            cmsFormatters16 f = InputFormatters16[i];
	            
	            if ((dwInput & ~f.Mask) == f.Type)
	            {
	            	fr.set(f.Frm);
	                return fr;
	            }
	        }
	    }
	    else
	    {
	        for (i = 0; i < DEFAULT_FORM_IN_FLOAT_COUNT; i++)
	        {
	            cmsFormattersFloat f = InputFormattersFloat[i];
	            
	            if ((dwInput & ~f.Mask) == f.Type)
	            {
	            	fr.set(f.Frm);
	                return fr;
	            }
	        }
	    }
	    
	    fr.set(null);
	    return fr;
	}
	
	// Packing routines -----------------------------------------------------------------------------------------------------------
	
	private static cmsFormatters16[] OutputFormatters16;
	
	private static void setupOutputFormatter16()
	{
		OutputFormatters16 = new cmsFormatters16[]{
			//			    Type                                          Mask                  Function
			//  ----------------------------   ------------------------------------  ----------------------------
				
			new cmsFormatters16(lcms2.TYPE_Lab_DBL, ANYPLANAR|ANYEXTRA, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					// Unencoded Float values -- don't try optimize speed
					
					//PackLabDoubleFrom16
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					cmsCIELab Lab = new cmsCIELab();
					cmspcs.cmsLabEncoded2Float(Lab, Values);
					if (lcms2.T_PLANAR(CMMcargo.OutputFormat))
					{
				        int Out = Buffer.getPosition();
				        
				        VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
				        proc.write(Lab.L);
				        Buffer.setPosition(Out + (Stride * 8));
				        proc.write(Lab.a);
				        Buffer.setPosition(Out + ((Stride * 2) * 8));
				        proc.write(Lab.b);
				        
				        Buffer.setPosition(Out + /*sizeof(cmsFloat64Number)*/8);
				    }
				    else
				    {
				    	Buffer.getProcessor().write(Lab);
				    	Buffer.movePosition(/*sizeof(cmsCIELab)*/cmsCIELab.SIZE + lcms2.T_EXTRA(CMMcargo.OutputFormat) * /*sizeof(cmsFloat64Number)*/8);           
				    }
					return Buffer;
				}
			}),
			new cmsFormatters16(lcms2.TYPE_XYZ_DBL, ANYPLANAR|ANYEXTRA, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//PackXYZDoubleFrom16
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					cmsCIEXYZ XYZ = new cmsCIEXYZ();
					cmspcs.cmsXYZEncoded2Float(XYZ, Values);
					if (lcms2.T_PLANAR(CMMcargo.OutputFormat))
					{
				        int Out = Buffer.getPosition();
				        
				        VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
				        proc.write(XYZ.X);
				        Buffer.setPosition(Out + (Stride * 8));
				        proc.write(XYZ.Y);
				        Buffer.setPosition(Out + ((Stride * 2) * 8));
				        proc.write(XYZ.Z);
				        
				        Buffer.setPosition(Out + /*sizeof(cmsFloat64Number)*/8);
				    }
				    else
				    {
				    	Buffer.getProcessor().write(XYZ);
				    	Buffer.movePosition(/*sizeof(cmsCIEXYZ)*/cmsCIEXYZ.SIZE + lcms2.T_EXTRA(CMMcargo.OutputFormat) * /*sizeof(cmsFloat64Number)*/8);           
				    }
					return Buffer;
				}
			}),
			new cmsFormatters16((1 << lcms2.FLOAT_SHIFT_VALUE)|(0 << lcms2.BYTES_SHIFT_VALUE), ANYCHANNELS|ANYPLANAR|ANYEXTRA|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//PackDoubleFrom16
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					int Inks = Buffer.getPosition();
				    int nChan = lcms2.T_CHANNELS(CMMcargo.OutputFormat);
				    int i;
				    double maximum = IsInkSpace(CMMcargo.OutputFormat) ? 655.35 : 65535.0;
				    
				    VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
				    if (lcms2.T_PLANAR(CMMcargo.OutputFormat))
				    {
				        for (i = 0; i < nChan; i++)
				        {
				        	Buffer.setPosition(Inks + ((i * Stride) * 8));
				        	proc.write((Values[i] & 0xFFFF) / maximum);
				        }
				        
				        Buffer.setPosition(Inks + /*sizeof(cmsFloat64Number)*/8);
				    }
				    else
				    {
				        for (i = 0; i < nChan; i++)
				        {
				        	Buffer.setPosition(Inks + (i * 8));
				        	proc.write((Values[i] & 0xFFFF) / maximum);
				        }
				        
				        Buffer.setPosition(Inks + (nChan + lcms2.T_EXTRA(CMMcargo.OutputFormat)) * /*sizeof(cmsFloat64Number)*/8);
				    }
					
					return Buffer;
				}
			}),
			new cmsFormatters16((1 << lcms2.FLOAT_SHIFT_VALUE)|(4 << lcms2.BYTES_SHIFT_VALUE), ANYCHANNELS|ANYPLANAR|ANYEXTRA|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//PackFloatFrom16
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					int Inks = Buffer.getPosition();
				    int nChan = lcms2.T_CHANNELS(CMMcargo.OutputFormat);
				    int i;
				    double maximum = IsInkSpace(CMMcargo.OutputFormat) ? 655.35 : 65535.0;
				    
				    VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
				    if (lcms2.T_PLANAR(CMMcargo.OutputFormat))
				    {
				        for (i = 0; i < nChan; i++)
				        {
				        	Buffer.setPosition(Inks + ((i * Stride) * 4));
				        	proc.write((float)((Values[i] & 0xFFFF) / maximum));
				        }
				        
				        Buffer.setPosition(Inks + /*sizeof(cmsFloat32Number)*/4);
				    }
				    else
				    {
				        for (i = 0; i < nChan; i++)
				        {
				        	Buffer.setPosition(Inks + (i * 4));
				        	proc.write((float)((Values[i] & 0xFFFF) / maximum));
				        }
				        
				        Buffer.setPosition(Inks + (nChan + lcms2.T_EXTRA(CMMcargo.OutputFormat)) * /*sizeof(cmsFloat32Number)*/4);
				    }
					
					return Buffer;
				}
			}),
			
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack1Byte
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					Buffer.getProcessor().write(lcms2_internal.FROM_16_TO_8(Values[0]), true);
					return Buffer;
				}
			}),
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack1ByteSkip1
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					Buffer.getProcessor().write(lcms2_internal.FROM_16_TO_8(Values[0]), true);
					Buffer.movePosition(1);
					return Buffer;
				}
			}),
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack1ByteSkip1SwapFirst
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					Buffer.movePosition(1);
					Buffer.getProcessor().write(lcms2_internal.FROM_16_TO_8(Values[0]), true);
					
					return Buffer;
				}
			}),
			
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.FLAVOR_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack1ByteReversed
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					Buffer.getProcessor().write(lcms2_internal.FROM_16_TO_8(REVERSE_FLAVOR_16(Values[0])), true);
					return Buffer;
				}
			}),
			
			new cmsFormatters16(lcms2.TYPE_LabV2_8, 0, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//PackLabV2_8
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(lcms2_internal.FROM_16_TO_8(FomLabV4ToLabV2(Values[0])), true);
					proc.write(lcms2_internal.FROM_16_TO_8(FomLabV4ToLabV2(Values[1])), true);
					proc.write(lcms2_internal.FROM_16_TO_8(FomLabV4ToLabV2(Values[2])), true);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16(lcms2.TYPE_ALabV2_8, 0, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//PackALabV2_8
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Buffer.movePosition(1);
					proc.write(lcms2_internal.FROM_16_TO_8(FomLabV4ToLabV2(Values[0])), true);
					proc.write(lcms2_internal.FROM_16_TO_8(FomLabV4ToLabV2(Values[1])), true);
					proc.write(lcms2_internal.FROM_16_TO_8(FomLabV4ToLabV2(Values[2])), true);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16(lcms2.TYPE_LabV2_16, 0, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//PackLabV2_16
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(FomLabV4ToLabV2(Values[0]), true);
					proc.write(FomLabV4ToLabV2(Values[1]), true);
					proc.write(FomLabV4ToLabV2(Values[2]), true);
				    
				    return Buffer;
				}
			}),
			
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.OPTIMIZED_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack3BytesOptimized
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write((byte)(Values[0] & 0xFF), true);
					proc.write((byte)(Values[1] & 0xFF), true);
					proc.write((byte)(Values[2] & 0xFF), true);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.OPTIMIZED_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack3BytesAndSkip1Optimized
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write((byte)(Values[0] & 0xFF), true);
					proc.write((byte)(Values[1] & 0xFF), true);
					proc.write((byte)(Values[2] & 0xFF), true);
					Buffer.movePosition(1);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE)|(1 << lcms2.OPTIMIZED_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack3BytesAndSkip1SwapFirstOptimized
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Buffer.movePosition(1);
					proc.write((byte)(Values[0] & 0xFF), true);
					proc.write((byte)(Values[1] & 0xFF), true);
					proc.write((byte)(Values[2] & 0xFF), true);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE)|(1 << lcms2.OPTIMIZED_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack3BytesAndSkip1SwapSwapFirstOptimized
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write((byte)(Values[2] & 0xFF), true);
					proc.write((byte)(Values[1] & 0xFF), true);
					proc.write((byte)(Values[0] & 0xFF), true);
					Buffer.movePosition(1);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.OPTIMIZED_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack3BytesAndSkip1SwapOptimized
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Buffer.movePosition(1);
					proc.write((byte)(Values[2] & 0xFF), true);
					proc.write((byte)(Values[1] & 0xFF), true);
					proc.write((byte)(Values[0] & 0xFF), true);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE)|(1 << lcms2.OPTIMIZED_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack3BytesSwapOptimized
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write((byte)(Values[2] & 0xFF), true);
					proc.write((byte)(Values[1] & 0xFF), true);
					proc.write((byte)(Values[0] & 0xFF), true);
				    
				    return Buffer;
				}
			}),
			
			
			
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack3Bytes
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(lcms2_internal.FROM_16_TO_8(Values[0]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[1]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[2]), true);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack3BytesAndSkip1
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(lcms2_internal.FROM_16_TO_8(Values[0]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[1]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[2]), true);
					Buffer.movePosition(1);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack3BytesAndSkip1SwapFirst
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Buffer.movePosition(1);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[0]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[1]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[2]), true);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack3BytesAndSkip1SwapSwapFirst
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(lcms2_internal.FROM_16_TO_8(Values[2]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[1]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[0]), true);
					Buffer.movePosition(1);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack3BytesAndSkip1Swap
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Buffer.movePosition(1);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[2]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[1]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[0]), true);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack3BytesSwap
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(lcms2_internal.FROM_16_TO_8(Values[2]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[1]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[0]), true);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((6 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					// CMYKcm (unrolled for speed)
					
					//Pack6Bytes
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(lcms2_internal.FROM_16_TO_8(Values[0]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[1]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[2]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[3]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[4]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[5]), true);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((6 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					// KCMYcm
					
					//Pack6BytesSwap
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(lcms2_internal.FROM_16_TO_8(Values[5]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[4]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[3]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[2]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[1]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[0]), true);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack4Bytes
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(lcms2_internal.FROM_16_TO_8(Values[0]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[1]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[2]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[3]), true);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.FLAVOR_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack4BytesReverse
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(REVERSE_FLAVOR_8(lcms2_internal.FROM_16_TO_8(Values[0])), true);
					proc.write(REVERSE_FLAVOR_8(lcms2_internal.FROM_16_TO_8(Values[1])), true);
					proc.write(REVERSE_FLAVOR_8(lcms2_internal.FROM_16_TO_8(Values[2])), true);
					proc.write(REVERSE_FLAVOR_8(lcms2_internal.FROM_16_TO_8(Values[3])), true);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack4BytesSwapFirst
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(lcms2_internal.FROM_16_TO_8(Values[3]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[0]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[1]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[2]), true);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					// ABGR
					
					//Pack4BytesSwap
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(lcms2_internal.FROM_16_TO_8(Values[3]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[2]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[1]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[0]), true);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack4BytesSwapSwapFirst
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(lcms2_internal.FROM_16_TO_8(Values[2]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[1]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[0]), true);
					proc.write(lcms2_internal.FROM_16_TO_8(Values[3]), true);
				    
				    return Buffer;
				}
			}),
			
			new cmsFormatters16((1 << lcms2.BYTES_SHIFT_VALUE), ANYFLAVOR|ANYSWAPFIRST|ANYSWAP|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					// Generic chunky for byte
					
					//PackAnyBytes
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					int nChan         = lcms2.T_CHANNELS(CMMcargo.OutputFormat);
				    boolean DoSwap    = lcms2.T_DOSWAP(CMMcargo.OutputFormat);
				    boolean Reverse   = lcms2.T_FLAVOR(CMMcargo.OutputFormat);
				    int Extra         = lcms2.T_EXTRA(CMMcargo.OutputFormat);
				    boolean SwapFirst = lcms2.T_SWAPFIRST(CMMcargo.OutputFormat);
				    boolean ExtraFirst= DoSwap && !SwapFirst;
				    VirtualPointer swap1;
				    byte v = 0;
				    int i;
				    
				    swap1 = new VirtualPointer(Buffer);
				    
				    if (ExtraFirst)
				    {
				    	Buffer.movePosition(Extra);
				    }
				    
				    VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
				    for (i = 0; i < nChan; i++)
				    {
				        int index = DoSwap ? (nChan - i - 1) : i;
				        
				        v = lcms2_internal.FROM_16_TO_8(Values[index]);
				        
				        if (Reverse)
				        {
				            v = REVERSE_FLAVOR_8(v);
				        }
				        
				        proc.write(v, true);
				    }
				    
				    if (!ExtraFirst)
				    {
				    	Buffer.movePosition(Extra);
				    }
				    
				    if (Extra == 0 && SwapFirst)
				    {
				    	swap1.memmove(1, 0, nChan-1);
				    	swap1.writeRaw(v & 0xFF);
				    }
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.PLANAR_SHIFT_VALUE), ANYFLAVOR|ANYSWAP|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//PackPlanarBytes
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					int nChan       = lcms2.T_CHANNELS(CMMcargo.OutputFormat);
					boolean DoSwap  = lcms2.T_DOSWAP(CMMcargo.OutputFormat);
				    boolean Reverse = lcms2.T_FLAVOR(CMMcargo.OutputFormat);
				    int i;
				    int Init = Buffer.getPosition();
				    
				    VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
				    for (i = 0; i < nChan; i++)
				    {
				        int index = DoSwap ? (nChan - i - 1) : i;
				        byte v = lcms2_internal.FROM_16_TO_8(Values[index]);
				        
				        proc.write((byte)(Reverse ? REVERSE_FLAVOR_8(v) : v));
				        Buffer.movePosition(Stride);
				    }
				    
				    Buffer.setPosition(Init + 1);
				    return Buffer;
				}
			}),
			
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack1Word
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					Buffer.getProcessor().write(Values[0], true);
					
					return Buffer;
				}
			}),
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack1WordSkip1
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					Buffer.getProcessor().write(Values[0]);
					Buffer.movePosition(2 + 2);
					
					return Buffer;
				}
			}),
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack1WordSkip1SwapFirst
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					Buffer.movePosition(2);
					Buffer.getProcessor().write(Values[0], true);
					
					return Buffer;
				}
			}),
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.FLAVOR_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack1WordReversed
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					Buffer.getProcessor().write(REVERSE_FLAVOR_16(Values[0]), true);
					
					return Buffer;
				}
			}),
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.ENDIAN16_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack1WordBigEndian
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					Buffer.getProcessor().write(CHANGE_ENDIAN(Values[0]), true);
					
					return Buffer;
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack3Words
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(Values[0], true);
					proc.write(Values[1], true);
					proc.write(Values[2], true);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack3WordsSwap
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(Values[2], true);
					proc.write(Values[1], true);
					proc.write(Values[0], true);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.ENDIAN16_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack3WordsBigEndian
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(CHANGE_ENDIAN(Values[0]), true);
					proc.write(CHANGE_ENDIAN(Values[1]), true);
					proc.write(CHANGE_ENDIAN(Values[2]), true);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack3WordsAndSkip1
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(Values[0], true);
					proc.write(Values[1], true);
					proc.write(Values[2], true);
					Buffer.movePosition(2 + 2);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack3WordsAndSkip1Swap
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Buffer.movePosition(2);
					proc.write(Values[2], true);
					proc.write(Values[1], true);
					proc.write(Values[0], true);
					Buffer.movePosition(2);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack3WordsAndSkip1SwapFirst
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					Buffer.movePosition(2);
					proc.write(Values[0], true);
					proc.write(Values[1], true);
					proc.write(Values[2], true);
					Buffer.movePosition(2);
				    
				    return Buffer;
				}
			}),
			
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack3WordsAndSkip1SwapSwapFirst
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(Values[2], true);
					proc.write(Values[1], true);
					proc.write(Values[0], true);
					Buffer.movePosition(2 + 2);
				    
				    return Buffer;
				}
			}),
			
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack4Words
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(Values[0], true);
					proc.write(Values[1], true);
					proc.write(Values[2], true);
					proc.write(Values[3], true);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.FLAVOR_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//Pack4WordsReverse
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(REVERSE_FLAVOR_16(Values[0]), true);
					proc.write(REVERSE_FLAVOR_16(Values[1]), true);
					proc.write(REVERSE_FLAVOR_16(Values[2]), true);
					proc.write(REVERSE_FLAVOR_16(Values[3]), true);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					// ABGR
					
					//Pack4WordsSwap
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(Values[3], true);
					proc.write(Values[2], true);
					proc.write(Values[1], true);
					proc.write(Values[0], true);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.ENDIAN16_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					// CMYK
					
					//Pack4WordsBigEndian
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(CHANGE_ENDIAN(Values[0]), true);
					proc.write(CHANGE_ENDIAN(Values[1]), true);
					proc.write(CHANGE_ENDIAN(Values[2]), true);
					proc.write(CHANGE_ENDIAN(Values[3]), true);
				    
				    return Buffer;
				}
			}),
			
			new cmsFormatters16((6 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					// CMYKcm
					
					//Pack6Words
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(Values[0], true);
					proc.write(Values[1], true);
					proc.write(Values[2], true);
					proc.write(Values[3], true);
					proc.write(Values[4], true);
					proc.write(Values[5], true);
				    
				    return Buffer;
				}
			}),
			new cmsFormatters16((6 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					// KCMYcm
					
					//Pack6WordsSwap
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
					proc.write(Values[5], true);
					proc.write(Values[4], true);
					proc.write(Values[3], true);
					proc.write(Values[2], true);
					proc.write(Values[1], true);
					proc.write(Values[0], true);
				    
				    return Buffer;
				}
			}),
			
			new cmsFormatters16((2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.PLANAR_SHIFT_VALUE), ANYFLAVOR|ANYENDIAN|ANYSWAP|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//PackPlanarWords
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					int nChan          = lcms2.T_CHANNELS(CMMcargo.OutputFormat);
				    boolean DoSwap     = lcms2.T_DOSWAP(CMMcargo.OutputFormat);
				    boolean Reverse    = lcms2.T_FLAVOR(CMMcargo.OutputFormat);
				    boolean SwapEndian = lcms2.T_ENDIAN16(CMMcargo.OutputFormat);
				    int i;
				    int Init = Buffer.getPosition();
				    short v;
				    
				    if (DoSwap)
				    {
				    	Buffer.movePosition(lcms2.T_EXTRA(CMMcargo.OutputFormat) * Stride * /*sizeof(cmsUInt16Number)*/2);
				    }
				    
				    VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
				    for (i=0; i < nChan; i++)
				    {
				        int index = DoSwap ? (nChan - i - 1) : i;
				        
				        v = Values[index];
				        
				        if (SwapEndian)
				        {
				        	v = CHANGE_ENDIAN(v);
				        }

				        if (Reverse)
				        {
				        	v =  REVERSE_FLAVOR_16(v);
				        }
				        
				        proc.write(v);
				        Buffer.movePosition(Stride * /*sizeof(cmsUInt16Number)*/2);
				    }
				    
				    Buffer.setPosition(Init + /*sizeof(cmsUInt16Number)*/2);
				    return Buffer;
				}
			}),
			new cmsFormatters16((2 << lcms2.BYTES_SHIFT_VALUE), ANYFLAVOR|ANYSWAPFIRST|ANYSWAP|ANYENDIAN|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					//PackAnyWords
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					int nChan          = lcms2.T_CHANNELS(CMMcargo.OutputFormat);
					boolean SwapEndian = lcms2.T_ENDIAN16(CMMcargo.InputFormat);
				    boolean DoSwap     = lcms2.T_DOSWAP(CMMcargo.OutputFormat);
				    boolean Reverse    = lcms2.T_FLAVOR(CMMcargo.OutputFormat);
				    int Extra          = lcms2.T_EXTRA(CMMcargo.OutputFormat);
				    boolean SwapFirst  = lcms2.T_SWAPFIRST(CMMcargo.OutputFormat);
				    boolean ExtraFirst = DoSwap && !SwapFirst;
				    VirtualPointer swap1;
				    short v = 0;
				    int i;
				    
				    swap1 = new VirtualPointer(Buffer);
				    
				    if (ExtraFirst)
				    {
				    	Buffer.movePosition(Extra * /*sizeof(cmsUInt16Number)*/2);
				    }
				    
				    VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
				    for (i = 0; i < nChan; i++)
				    {
				        int index = DoSwap ? (nChan - i - 1) : i;
				        
				        v = Values[index];
				        
				        if (SwapEndian)
				        {
				        	v = CHANGE_ENDIAN(v);
				        }
				        
				        if (Reverse)
				        {
				        	v = REVERSE_FLAVOR_16(v);
				        }
				        
				        proc.write(v, true);
				    }
				    
				    if (!ExtraFirst)
				    {
				    	Buffer.movePosition(Extra * /*sizeof(cmsUInt16Number)*/2);
				    }
				    
				    if (Extra == 0 && SwapFirst)
				    {
				        swap1.memmove(1 * 2, 0, (nChan-1) * /*sizeof(cmsUInt16Number)*/2);
				        swap1.getProcessor().write(v);
				    }
				    
				    return Buffer;
				}
			})
		};
	}
	
	private static final int DEFAULT_FORM_OUT_16_COUNT = 52;
	
	//-------------------------------------------------------------------------------------------------------------------
	
	// True float transformation.
	
	private static cmsFormattersFloat[] InputFormattersFloat;
	
	private static void setupInputFormatterFloat()
	{
		InputFormattersFloat = new cmsFormattersFloat[]{
		    //    Type                                          Mask                  Function
		    //  ----------------------------   ------------------------------------  ----------------------------
			new cmsFormattersFloat(lcms2.TYPE_Lab_DBL, ANYPLANAR|ANYEXTRA, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					// From Lab double to cmsFloat32Number
					
					//UnrollLabDoubleToFloat
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
				    if (lcms2.T_PLANAR(CMMcargo.InputFormat))
				    {
				    	int pos = Buffer.getPosition();
				    	Values[0] = (float)(proc.readDouble() / 100.0);			// from 0..100 to 0..1
				        Buffer.setPosition(pos + (Stride * 8));
				        Values[1] = (float)((proc.readDouble() + 128) / 255.0);	// form -128..+127 to 0..1
				        Buffer.setPosition(pos + ((Stride * 2) * 8));
				        Values[2] = (float)((proc.readDouble() + 128) / 255.0);
				        
				        Buffer.setPosition(pos + /*sizeof(cmsFloat64Number)*/8);
				    }
				    else
				    {
				    	Values[0] = (float)(proc.readDouble(true) / 100.0);			// from 0..100 to 0..1
				    	Values[1] = (float)((proc.readDouble(true) + 128) / 255.0);	// form -128..+127 to 0..1
				    	Values[2] = (float)((proc.readDouble(true) + 128) / 255.0);
				    	proc.movePosition(8 * lcms2.T_EXTRA(CMMcargo.InputFormat));
				    }
				    
				    return Buffer;
				}
			}),
			new cmsFormattersFloat(lcms2.TYPE_Lab_FLT, ANYPLANAR|ANYEXTRA, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					// From Lab double to cmsFloat32Number
					
					//UnrollLabFloatToFloat
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
				    if (lcms2.T_PLANAR(CMMcargo.InputFormat))
				    {
				    	int pos = Buffer.getPosition();
				    	Values[0] = (float)(proc.readFloat() / 100.0);			// from 0..100 to 0..1
				        Buffer.setPosition(pos + (Stride * 4));
				        Values[1] = (float)((proc.readFloat() + 128) / 255.0);	// form -128..+127 to 0..1
				        Buffer.setPosition(pos + ((Stride * 2) * 4));
				        Values[2] = (float)((proc.readFloat() + 128) / 255.0);
				        
				        Buffer.setPosition(pos + /*sizeof(cmsFloat32Number)*/4);
				    }
				    else
				    {
				    	Values[0] = (float)(proc.readFloat(true) / 100.0);			// from 0..100 to 0..1
				    	Values[1] = (float)((proc.readFloat(true) + 128) / 255.0);	// form -128..+127 to 0..1
				    	Values[2] = (float)((proc.readFloat(true) + 128) / 255.0);
				    	proc.movePosition(4 * lcms2.T_EXTRA(CMMcargo.InputFormat));
				    }
				    
				    return Buffer;
				}
			}),
			new cmsFormattersFloat(lcms2.TYPE_XYZ_DBL, ANYPLANAR|ANYEXTRA, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					// 1.15 fixed point, that means maximum value is MAX_ENCODEABLE_XYZ (0xFFFF)
					
					//UnrollXYZDoubleToFloat
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
				    if (lcms2.T_PLANAR(CMMcargo.InputFormat))
				    {
				    	int pos = Buffer.getPosition();
				    	Values[0] = (float)(proc.readDouble() / lcms2_internal.MAX_ENCODEABLE_XYZ);
				        Buffer.setPosition(pos + (Stride * 8));
				        Values[1] = (float)(proc.readDouble() / lcms2_internal.MAX_ENCODEABLE_XYZ);
				        Buffer.setPosition(pos + ((Stride * 2) * 8));
				        Values[2] = (float)(proc.readDouble() / lcms2_internal.MAX_ENCODEABLE_XYZ);
				        
				        Buffer.setPosition(pos + /*sizeof(cmsFloat64Number)*/8);
				    }
				    else
				    {
				    	Values[0] = (float)(proc.readDouble(true) / lcms2_internal.MAX_ENCODEABLE_XYZ);
				    	Values[1] = (float)(proc.readDouble(true) / lcms2_internal.MAX_ENCODEABLE_XYZ);
				    	Values[2] = (float)(proc.readDouble(true) / lcms2_internal.MAX_ENCODEABLE_XYZ);
				    	proc.movePosition(8 * lcms2.T_EXTRA(CMMcargo.InputFormat));
				    }
				    
				    return Buffer;
				}
			}),
			new cmsFormattersFloat(lcms2.TYPE_XYZ_FLT, ANYPLANAR|ANYEXTRA, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					//UnrollXYZFloatToFloat
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
				    if (lcms2.T_PLANAR(CMMcargo.InputFormat))
				    {
				    	int pos = Buffer.getPosition();
				    	Values[0] = (float)(proc.readFloat() / lcms2_internal.MAX_ENCODEABLE_XYZ);
				        Buffer.setPosition(pos + (Stride * 4));
				        Values[1] = (float)(proc.readFloat() / lcms2_internal.MAX_ENCODEABLE_XYZ);
				        Buffer.setPosition(pos + ((Stride * 2) * 4));
				        Values[2] = (float)(proc.readFloat() / lcms2_internal.MAX_ENCODEABLE_XYZ);
				        
				        Buffer.setPosition(pos + /*sizeof(cmsFloat32Number)*/4);
				    }
				    else
				    {
				    	Values[0] = (float)(proc.readFloat(true) / lcms2_internal.MAX_ENCODEABLE_XYZ);
				    	Values[1] = (float)(proc.readFloat(true) / lcms2_internal.MAX_ENCODEABLE_XYZ);
				    	Values[2] = (float)(proc.readFloat(true) / lcms2_internal.MAX_ENCODEABLE_XYZ);
				    	proc.movePosition(4 * lcms2.T_EXTRA(CMMcargo.InputFormat));
				    }
				    
				    return Buffer;
				}
			}),
			
			new cmsFormattersFloat((1 << lcms2.FLOAT_SHIFT_VALUE)|(4 << lcms2.BYTES_SHIFT_VALUE), ANYPLANAR|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					// For anything going from cmsFloat32Number
					
					//UnrollFloatsToFloat
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					int nChan      = lcms2.T_CHANNELS(CMMcargo.InputFormat);
				    boolean Planar = lcms2.T_PLANAR(CMMcargo.InputFormat);
				    int i;
				    double maximum = IsInkSpace(CMMcargo.InputFormat) ? 100.0 : 1.0;
				    
				    int pos = Buffer.getPosition();
				    VirtualPointer.TypeProcessor Inks = Buffer.getProcessor();
				    for (i = 0; i < nChan; i++)
				    {
				        if (Planar)
				        {
				        	Buffer.setPosition(pos + ((i * Stride) * 4));
				        	Values[i] = (float)(Inks.readFloat() / maximum);
				        }
				        else
				        {
				        	Buffer.setPosition(pos + (i * 4));
				        	Values[i] = (float)(Inks.readFloat() / maximum);
				        }
				    }
				    
				    if (lcms2.T_PLANAR(CMMcargo.InputFormat))
				    {
				    	Buffer.setPosition(pos + /*sizeof(cmsFloat32Number)*/4);
				    }
				    else
				    {
				    	Buffer.setPosition(pos + (nChan + lcms2.T_EXTRA(CMMcargo.InputFormat)) * /*sizeof(cmsFloat32Number)*/4);
				    }
				    return Buffer;
				}
			}),
			new cmsFormattersFloat((1 << lcms2.FLOAT_SHIFT_VALUE)|(0 << lcms2.BYTES_SHIFT_VALUE), ANYPLANAR|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					// For anything going from double
					
					//UnrollDoublesToFloat
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					int nChan      = lcms2.T_CHANNELS(CMMcargo.InputFormat);
				    boolean Planar = lcms2.T_PLANAR(CMMcargo.InputFormat);
				    int i;
				    double maximum = IsInkSpace(CMMcargo.InputFormat) ? 100.0 : 1.0;
				    
				    int pos = Buffer.getPosition();
				    VirtualPointer.TypeProcessor Inks = Buffer.getProcessor();
				    for (i = 0; i < nChan; i++)
				    {
				        if (Planar)
				        {
				        	Buffer.setPosition(pos + ((i * Stride) * 8));
				        	Values[i] = (float)(Inks.readDouble() / maximum);
				        }
				        else
				        {
				        	Buffer.setPosition(pos + (i * 8));
				        	Values[i] = (float)(Inks.readDouble() / maximum);
				        }
				    }
				    
				    if (lcms2.T_PLANAR(CMMcargo.InputFormat))
				    {
				    	Buffer.setPosition(pos + /*sizeof(cmsFloat64Number)*/8);
				    }
				    else
				    {
				    	Buffer.setPosition(pos + (nChan + lcms2.T_EXTRA(CMMcargo.InputFormat)) * /*sizeof(cmsFloat64Number)*/8);
				    }
				    return Buffer;
				}
			})
		};
	}
	
	private static final int DEFAULT_FORM_IN_FLOAT_COUNT = 6;
	
	// Packing routines -----------------------------------------------------------------------------------------------------------
	
	private static cmsFormattersFloat[] OutputFormattersFloat;
	
	private static void setupOutputFormatterFloat()
	{
		OutputFormattersFloat = new cmsFormattersFloat[]{
			new cmsFormattersFloat(lcms2.TYPE_Lab_FLT, ANYPLANAR|ANYEXTRA, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					//PackLabFloatFromFloat
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor Out = Buffer.getProcessor();
				    if (lcms2.T_PLANAR(CMMcargo.OutputFormat))
				    {
				    	int pos = Buffer.getPosition();
				    	Out.write((float)(Values[0] * 100.0));
				    	Buffer.setPosition(pos + (Stride * 4));
				    	Out.write((float)(Values[1] * 255.0 - 128.0));
				    	Buffer.setPosition(pos + ((Stride * 2) * 4));
				    	Out.write((float)(Values[2] * 255.0 - 128.0));
				        
				    	Buffer.setPosition(pos + /*sizeof(cmsFloat32Number)*/4);
				    }
				    else
				    {
				    	Out.write((float)(Values[0] * 100.0), true);
				    	Out.write((float)(Values[1] * 255.0 - 128.0), true);
				    	Out.write((float)(Values[2] * 255.0 - 128.0), true);
				    	
				    	Buffer.movePosition(lcms2.T_EXTRA(CMMcargo.OutputFormat) * /*sizeof(cmsFloat32Number)*/4);
				    }
				    
				    return Buffer;
				}
			}),
			new cmsFormattersFloat(lcms2.TYPE_XYZ_FLT, ANYPLANAR|ANYEXTRA, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					// From 0..1 range to 0..MAX_ENCODEABLE_XYZ
					
					//PackXYZFloatFromFloat
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor Out = Buffer.getProcessor();
				    if (lcms2.T_PLANAR(CMMcargo.OutputFormat))
				    {
				    	int pos = Buffer.getPosition();
				    	Out.write((float)(Values[0] * lcms2_internal.MAX_ENCODEABLE_XYZ));
				    	Buffer.setPosition(pos + (Stride * 4));
				    	Out.write((float)(Values[1] * lcms2_internal.MAX_ENCODEABLE_XYZ));
				    	Buffer.setPosition(pos + ((Stride * 2) * 4));
				    	Out.write((float)(Values[2] * lcms2_internal.MAX_ENCODEABLE_XYZ));
				        
				    	Buffer.setPosition(pos + /*sizeof(cmsFloat32Number)*/4);
				    }
				    else
				    {
				    	Out.write((float)(Values[0] * lcms2_internal.MAX_ENCODEABLE_XYZ), true);
				    	Out.write((float)(Values[1] * lcms2_internal.MAX_ENCODEABLE_XYZ), true);
				    	Out.write((float)(Values[2] * lcms2_internal.MAX_ENCODEABLE_XYZ), true);
				    	
				    	Buffer.movePosition(lcms2.T_EXTRA(CMMcargo.OutputFormat) * /*sizeof(cmsFloat32Number)*/4);
				    }
				    
				    return Buffer;
				}
			}),
			new cmsFormattersFloat(lcms2.TYPE_Lab_DBL, ANYPLANAR|ANYEXTRA, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					//PackLabDoubleFromFloat
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor Out = Buffer.getProcessor();
				    if (lcms2.T_PLANAR(CMMcargo.OutputFormat))
				    {
				    	int pos = Buffer.getPosition();
				    	Out.write(Values[0] * 100.0);
				    	Buffer.setPosition(pos + (Stride * 8));
				    	Out.write(Values[1] * 255.0 - 128.0);
				    	Buffer.setPosition(pos + ((Stride * 2) * 8));
				    	Out.write(Values[2] * 255.0 - 128.0);
				        
				    	Buffer.setPosition(pos + /*sizeof(cmsFloat64Number)*/8);
				    }
				    else
				    {
				    	Out.write(Values[0] * 100.0, true);
				    	Out.write(Values[1] * 255.0 - 128.0, true);
				    	Out.write(Values[2] * 255.0 - 128.0, true);
				    	
				    	Buffer.movePosition(lcms2.T_EXTRA(CMMcargo.OutputFormat) * /*sizeof(cmsFloat64Number)*/8);
				    }
				    
				    return Buffer;
				}
			}),
			new cmsFormattersFloat(lcms2.TYPE_XYZ_DBL, ANYPLANAR|ANYEXTRA, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					// Same, but convert to double
					
					//PackXYZDoubleFromFloat
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					VirtualPointer.TypeProcessor Out = Buffer.getProcessor();
				    if (lcms2.T_PLANAR(CMMcargo.OutputFormat))
				    {
				    	int pos = Buffer.getPosition();
				    	Out.write(Values[0] * lcms2_internal.MAX_ENCODEABLE_XYZ);
				    	Buffer.setPosition(pos + (Stride * 8));
				    	Out.write(Values[1] * lcms2_internal.MAX_ENCODEABLE_XYZ);
				    	Buffer.setPosition(pos + ((Stride * 2) * 8));
				    	Out.write(Values[2] * lcms2_internal.MAX_ENCODEABLE_XYZ);
				        
				    	Buffer.setPosition(pos + /*sizeof(cmsFloat64Number)*/8);
				    }
				    else
				    {
				    	Out.write(Values[0] * lcms2_internal.MAX_ENCODEABLE_XYZ, true);
				    	Out.write(Values[1] * lcms2_internal.MAX_ENCODEABLE_XYZ, true);
				    	Out.write(Values[2] * lcms2_internal.MAX_ENCODEABLE_XYZ, true);
				    	
				    	Buffer.movePosition(lcms2.T_EXTRA(CMMcargo.OutputFormat) * /*sizeof(cmsFloat64Number)*/8);
				    }
				    
				    return Buffer;
				}
			}),
			new cmsFormattersFloat((1 << lcms2.FLOAT_SHIFT_VALUE)|(4 << lcms2.BYTES_SHIFT_VALUE), ANYFLAVOR|ANYSWAPFIRST|ANYSWAP|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					//PackChunkyFloatsFromFloat
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					int nChan          = lcms2.T_CHANNELS(CMMcargo.OutputFormat);
				    boolean DoSwap     = lcms2.T_DOSWAP(CMMcargo.OutputFormat);
				    boolean Reverse    = lcms2.T_FLAVOR(CMMcargo.OutputFormat);
				    int Extra          = lcms2.T_EXTRA(CMMcargo.OutputFormat);
				    boolean SwapFirst  = lcms2.T_SWAPFIRST(CMMcargo.OutputFormat);
				    boolean ExtraFirst = DoSwap && !SwapFirst;
				    double maximum     = IsInkSpace(CMMcargo.OutputFormat) ? 100.0 : 1.0;
				    VirtualPointer swap1;
				    double v = 0;
				    int i;
				    
				    swap1 = new VirtualPointer(Buffer);
				    
				    if (ExtraFirst)
				    {
				    	Buffer.movePosition(Extra * /*sizeof(cmsFloat32Number)*/4);
				    }
				    
				    VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
				    for (i = 0; i < nChan; i++)
				    {
				        int index = DoSwap ? (nChan - i - 1) : i;
				        
				        v = Values[index] * maximum;
				        
				        if (Reverse)
				        {
				        	v = maximum - v;
				        }
				        
				        proc.write((float)v, true);
				    }
				    
				    if (!ExtraFirst)
				    {
				    	Buffer.movePosition(Extra * /*sizeof(cmsFloat32Number)*/4);
				    }
				    
				    if (Extra == 0 && SwapFirst)
				    {
				    	swap1.memmove(1 * 4, 0, (nChan-1)* /*sizeof(cmsFloat32Number)*/4);
				    	swap1.getProcessor().write((float)v);
				    }
					
					return Buffer;
				}
			}),
			new cmsFormattersFloat((1 << lcms2.FLOAT_SHIFT_VALUE)|(4 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.PLANAR_SHIFT_VALUE), ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					//PackPlanarFloatsFromFloat
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					int nChan       = lcms2.T_CHANNELS(CMMcargo.OutputFormat);
					boolean DoSwap  = lcms2.T_DOSWAP(CMMcargo.OutputFormat);
				    boolean Reverse = lcms2.T_FLAVOR(CMMcargo.OutputFormat);
				    int i;
				    int Init = Buffer.getPosition();
				    double maximum = IsInkSpace(CMMcargo.OutputFormat) ? 100.0 : 1.0;
				    double v;
				    
				    if (DoSwap)
				    {
				    	Buffer.movePosition(lcms2.T_EXTRA(CMMcargo.OutputFormat) * Stride * /*sizeof(cmsFloat32Number)*/4);
				    }
				    
				    VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
				    for (i=0; i < nChan; i++)
				    {
				        int index = DoSwap ? (nChan - i - 1) : i;
				        
				        v = Values[index] * maximum;
				        
				        if (Reverse)
				        {
				        	v =  maximum - v;
				        }
				        
				        proc.write((float)v);
				        Buffer.movePosition(Stride * /*sizeof(cmsFloat32Number)*/4);
				    }
				    
				    Buffer.setPosition(Init + /*sizeof(cmsFloat32Number)*/4);
				    return Buffer;
				}
			}),
			new cmsFormattersFloat((1 << lcms2.FLOAT_SHIFT_VALUE)|(0 << lcms2.BYTES_SHIFT_VALUE), ANYFLAVOR|ANYSWAPFIRST|ANYSWAP|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					//PackChunkyDoublesFromFloat
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					int nChan          = lcms2.T_CHANNELS(CMMcargo.OutputFormat);
				    boolean DoSwap     = lcms2.T_DOSWAP(CMMcargo.OutputFormat);
				    boolean Reverse    = lcms2.T_FLAVOR(CMMcargo.OutputFormat);
				    int Extra          = lcms2.T_EXTRA(CMMcargo.OutputFormat);
				    boolean SwapFirst  = lcms2.T_SWAPFIRST(CMMcargo.OutputFormat);
				    boolean ExtraFirst = DoSwap && !SwapFirst;
				    double maximum     = IsInkSpace(CMMcargo.OutputFormat) ? 100.0 : 1.0;
				    VirtualPointer swap1;
				    double v = 0;
				    int i;
				    
				    swap1 = new VirtualPointer(Buffer);
				    
				    if (ExtraFirst)
				    {
				    	Buffer.movePosition(Extra * /*sizeof(cmsFloat64Number)*/8);
				    }
				    
				    VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
				    for (i = 0; i < nChan; i++)
				    {
				        int index = DoSwap ? (nChan - i - 1) : i;
				        
				        v = Values[index] * maximum;
				        
				        if (Reverse)
				        {
				        	v = maximum - v;
				        }
				        
				        proc.write(v, true);
				    }
				    
				    if (!ExtraFirst)
				    {
				    	Buffer.movePosition(Extra * /*sizeof(cmsFloat64Number)*/8);
				    }
				    
				    if (Extra == 0 && SwapFirst)
				    {
				    	swap1.memmove(1 * 8, 0, (nChan-1)* /*sizeof(cmsFloat64Number)*/8);
				    	swap1.getProcessor().write(v);
				    }
					
					return Buffer;
				}
			}),
			new cmsFormattersFloat((1 << lcms2.FLOAT_SHIFT_VALUE)|(0 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.PLANAR_SHIFT_VALUE), ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					//PackPlanarDoublesFromFloat
					
//#ifdef CMS_REALLOC_PTR
					Buffer = new VirtualPointer(Buffer);
//#endif
					
					int nChan       = lcms2.T_CHANNELS(CMMcargo.OutputFormat);
					boolean DoSwap  = lcms2.T_DOSWAP(CMMcargo.OutputFormat);
				    boolean Reverse = lcms2.T_FLAVOR(CMMcargo.OutputFormat);
				    int i;
				    int Init = Buffer.getPosition();
				    double maximum = IsInkSpace(CMMcargo.OutputFormat) ? 100.0 : 1.0;
				    double v;
				    
				    if (DoSwap)
				    {
				    	Buffer.movePosition(lcms2.T_EXTRA(CMMcargo.OutputFormat) * Stride * /*sizeof(cmsFloat64Number)*/8);
				    }
				    
				    VirtualPointer.TypeProcessor proc = Buffer.getProcessor();
				    for (i=0; i < nChan; i++)
				    {
				        int index = DoSwap ? (nChan - i - 1) : i;
				        
				        v = Values[index] * maximum;
				        
				        if (Reverse)
				        {
				        	v =  maximum - v;
				        }
				        
				        proc.write(v);
				        Buffer.movePosition(Stride * /*sizeof(cmsFloat64Number)*/8);
				    }
				    
				    Buffer.setPosition(Init + /*sizeof(cmsFloat64Number)*/8);
				    return Buffer;
				}
			})
		};
	}
	
	private static final int DEFAULT_FORM_OUT_FLOAT_COUNT = 8;
	
	// Bit fields set to one in the mask are not compared
	private static cmsFormatter _cmsGetStockOutputFormatter(int dwInput, int dwFlags)
	{
	    int i;
	    cmsFormatter fr = new cmsFormatter();
	    
	    if ((dwFlags & lcms2_plugin.CMS_PACK_FLAGS_FLOAT) != 0)
	    {
	        for (i = 0; i < DEFAULT_FORM_OUT_FLOAT_COUNT; i++)
	        {
	            cmsFormattersFloat f = OutputFormattersFloat[i];
	            
	            if ((dwInput & ~f.Mask) == f.Type)
	            {
	            	fr.set(f.Frm);
	                return fr;
	            }
	        }

	    }
	    else
	    {
	        for (i = 0; i < DEFAULT_FORM_OUT_16_COUNT; i++)
	        {
	            cmsFormatters16 f = OutputFormatters16[i];
	            
	            if ((dwInput & ~f.Mask) == f.Type)
	            {
	            	fr.set(f.Frm);
	                return fr;
	            }
	        }
	    }
	    
	    fr.set(null);
	    return fr;
	}
	
	private static class cmsFormattersFactoryList
	{
		public cmsFormatterFactory Factory;
		public cmsFormattersFactoryList Next;
	}
	
	private static cmsFormattersFactoryList FactoryList;
	
	private static final long FACTORY_LIST_UID = 0xD0FFC62FFBB2E7A4L;
	
	private static final long INPUT16 = 0xCC418D6C1E355480L;
	private static final long INPUTFLOAT = 0xB94ADC6E0C7F002CL;
	private static final long OUTPUT16 = 0xE73DFE4ED3EAEAC8L;
	private static final long OUTPUTFLOAT = 0xEE345CAF21C0C592L;
	
	static
	{
		Object obj;
		if((obj = Utility.singletonStorageGet(INPUT16)) != null)
		{
			InputFormatters16 = (cmsFormatters16[])obj;
			InputFormattersFloat = (cmsFormattersFloat[])Utility.singletonStorageGet(INPUTFLOAT);
			OutputFormatters16 = (cmsFormatters16[])Utility.singletonStorageGet(OUTPUT16);
			OutputFormattersFloat = (cmsFormattersFloat[])Utility.singletonStorageGet(OUTPUTFLOAT);
		}
		else
		{
			setupInputFormatter16();
			setupInputFormatterFloat();
			setupOutputFormatter16();
			setupOutputFormatterFloat();
			Utility.singletonStorageSet(INPUT16, InputFormatters16);
			Utility.singletonStorageSet(INPUTFLOAT, InputFormattersFloat);
			Utility.singletonStorageSet(OUTPUT16, OutputFormatters16);
			Utility.singletonStorageSet(OUTPUTFLOAT, OutputFormattersFloat);
		}
		
		if((obj = Utility.singletonStorageGet(FACTORY_LIST_UID)) != null)
		{
			FactoryList = (cmsFormattersFactoryList)obj;
		}
		else
		{
			FactoryList = null;
		}
	}
	
	// Formatters management
	public static boolean _cmsRegisterFormattersPlugin(cmsPluginBase Data)
	{
	    cmsPluginFormatters Plugin = (cmsPluginFormatters)Data;
	    cmsFormattersFactoryList fl;
	    
	    // Reset
		if (Data == null)
		{
			FactoryList = null;
			Utility.singletonStorageSet(FACTORY_LIST_UID, FactoryList);
			return true;
		}
		
		fl = new cmsFormattersFactoryList();
		
	    fl.Factory = Plugin.FormattersFactory;
	    
	    fl.Next = FactoryList;
	    FactoryList = fl;
	    Utility.singletonStorageSet(FACTORY_LIST_UID, FactoryList);
	    
	    return true;
	}
	
	public static cmsFormatter _cmsGetFormatter(int Type, // Specific type, i.e. TYPE_RGB_8
			int Dir, 
            int dwFlags) // Float or 16 bits
	{
		cmsFormattersFactoryList f;
		
		for (f = FactoryList; f != null; f = f.Next)
		{
			cmsFormatter fn = f.Factory.run(Type, Dir, dwFlags);
			if(fn.hasValue())
			{
				return fn;
			}
		}
		
		// Revert to default
		if (Dir == lcms2_plugin.cmsFormatterInput)
		{
			return _cmsGetStockInputFormatter(Type, dwFlags);
		}
		else
		{
			return _cmsGetStockOutputFormatter(Type, dwFlags);
		}
	}
	
	// Return whatever given formatter refers to float values
	public static boolean _cmsFormatterIsFloat(int Type)
	{
		return lcms2.T_FLOAT(Type);
	}
	
	// Return whatever given formatter refers to 8 bits
	public static boolean _cmsFormatterIs8bit(int Type)
	{
	    int Bytes = lcms2.T_BYTES(Type);
	    
	    return (Bytes == 1);
	}
	
	// Build a suitable formatter for the colorspace of this profile
	public static int cmsFormatterForColorspaceOfProfile(cmsHPROFILE hProfile, int nBytes, boolean lIsFloat)
	{
		int ColorSpace     = cmsio0.cmsGetColorSpace(hProfile);
	    int ColorSpaceBits = cmspcs._cmsLCMScolorSpace(ColorSpace);
	    int nOutputChans   = cmspcs.cmsChannelsOf(ColorSpace);
	    int Float          = lIsFloat ? 1 : 0;
	    
	    // Create a fake formatter for result
		return lcms2.FLOAT_SH(Float) | lcms2.COLORSPACE_SH(ColorSpaceBits) | lcms2.BYTES_SH(nBytes) | lcms2.CHANNELS_SH(nOutputChans);
	}
	
	// Build a suitable formatter for the colorspace of this profile
	public static int cmsFormatterForPCSOfProfile(cmsHPROFILE hProfile, int nBytes, boolean lIsFloat)
	{
		int ColorSpace     = cmsio0.cmsGetPCS(hProfile);
	    int ColorSpaceBits = cmspcs._cmsLCMScolorSpace(ColorSpace);
	    int nOutputChans   = cmspcs.cmsChannelsOf(ColorSpace);
	    int Float          = lIsFloat ? 1 : 0;
	    
	    // Create a fake formatter for result
	    return lcms2.FLOAT_SH(Float) | lcms2.COLORSPACE_SH(ColorSpaceBits) | lcms2.BYTES_SH(nBytes) | lcms2.CHANNELS_SH(nOutputChans);
	}
}
