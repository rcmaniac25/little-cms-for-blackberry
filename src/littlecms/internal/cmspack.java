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
	    
	    a = (x << 8 | x) >> 8;  // * 257 / 256
	    if ( a > 0xffff)
	    {
	    	return (short)0xffff;
	    }
	    return (short)a;
	}
	
	// * 0xf00 / 0xffff = * 256 / 257
	private static short FomLabV4ToLabV2(short x) 
	{
	    return (short)(((x << 8) + 0x80) / 257);
	}
	
	public static class cmsFormatters16
	{
		public int Type;
		public int Mask;
		public cmsFormatter16 Frm;
		
		public cmsFormatters16(int Type, int Mask, cmsFormatter16 frm)
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
		
		public cmsFormattersFloat(int Type, int Mask, cmsFormatterFloat frm)
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
	
	// Does almost everything but is slow
	
	private static VirtualPointer UnrollChunkyBytes(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		int nChan          = lcms2.T_CHANNELS(info.InputFormat);
	    boolean DoSwap     = lcms2.T_DOSWAP(info.InputFormat);
	    boolean Reverse    = lcms2.T_FLAVOR(info.InputFormat);
	    boolean SwapFirst  = lcms2.T_SWAPFIRST(info.InputFormat);
	    int Extra          = lcms2.T_EXTRA(info.InputFormat);
	    boolean ExtraFirst = DoSwap && !SwapFirst;
	    short v;
	    int i;
	    
	    if (ExtraFirst)
	    {
	    	accum.movePosition(Extra);
	    }
	    
	    VirtualPointer.TypeProcessor proc = accum.getProcessor();
	    for (i = 0; i < nChan; i++)
	    {
	        int index = DoSwap ? (nChan - i - 1) : i;
	        
	        v = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); 
	        v = Reverse ? REVERSE_FLAVOR_16(v) : v;
	        wIn[index] = v;
	    }
	    
	    if (!ExtraFirst)
	    {
	    	accum.movePosition(Extra);
	    }
	    
	    if (Extra == 0 && SwapFirst)
	    {
	        short tmp = wIn[0];
	        
	        System.arraycopy(wIn, 1, wIn, 0, (nChan-1)/* * sizeof(cmsUInt16Number)*/);
	        wIn[nChan-1] = tmp;
	    }
	    
	    return accum;
	}
	
	// Extra channels are just ignored because come in the next planes
	private static VirtualPointer UnrollPlanarBytes(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		int nChan       = lcms2.T_CHANNELS(info.InputFormat);
	    boolean DoSwap  = lcms2.T_DOSWAP(info.InputFormat);
	    boolean Reverse = lcms2.T_FLAVOR(info.InputFormat);
	    int i;
	    int Init = accum.getPosition();

	    if (DoSwap)
	    {
	        accum.movePosition(lcms2.T_EXTRA(info.InputFormat) * Stride);
	    }
	    
	    VirtualPointer.TypeProcessor proc = accum.getProcessor();
	    for (i = 0; i < nChan; i++)
	    {
	        int index = DoSwap ? (nChan - i - 1) : i;
	        short v = lcms2_internal.FROM_8_TO_16(proc.readInt8());
	        
	        wIn[index] = Reverse ? REVERSE_FLAVOR_16(v) : v;
	        accum.movePosition(Stride);
	    }
	    
	    accum.setPosition(Init + 1);
	    return accum;
	}
	
	// Special cases, provided for performance
	private static VirtualPointer Unroll4Bytes(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		wIn[0] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // C
	    wIn[1] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // M
	    wIn[2] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // Y
	    wIn[3] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // K
	    
	    return accum;
	}
	
	private static VirtualPointer Unroll4BytesReverse(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		wIn[0] = lcms2_internal.FROM_8_TO_16(REVERSE_FLAVOR_8(proc.readInt8(true))); // C
	    wIn[1] = lcms2_internal.FROM_8_TO_16(REVERSE_FLAVOR_8(proc.readInt8(true))); // M
	    wIn[2] = lcms2_internal.FROM_8_TO_16(REVERSE_FLAVOR_8(proc.readInt8(true))); // Y
	    wIn[3] = lcms2_internal.FROM_8_TO_16(REVERSE_FLAVOR_8(proc.readInt8(true))); // K
	    
	    return accum;
	}
	
	private static VirtualPointer Unroll4BytesSwapFirst(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		wIn[3] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // K
	    wIn[0] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // C
	    wIn[1] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // M
	    wIn[2] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // Y
	    
	    return accum;
	}
	
	// KYMC
	private static VirtualPointer Unroll4BytesSwap(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		wIn[3] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // K
	    wIn[2] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // Y
	    wIn[1] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // M
	    wIn[0] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // C
	    
	    return accum;
	}
	
	private static VirtualPointer Unroll4BytesSwapSwapFirst(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		wIn[2] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // K
	    wIn[1] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // Y
	    wIn[0] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // M
	    wIn[3] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // C
	    
	    return accum;
	}
	
	private static VirtualPointer Unroll3Bytes(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		wIn[0] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // R
	    wIn[1] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // G
	    wIn[2] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // B
	    
	    return accum;
	}
	
	private static VirtualPointer Unroll3BytesSkip1Swap(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		accum.movePosition(1); // A
		wIn[2] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // B
	    wIn[1] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // G
	    wIn[0] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // R
	    
	    return accum;
	}
	
	private static VirtualPointer Unroll3BytesSkip1SwapFirst(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		accum.movePosition(1); // A
		wIn[0] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // R
	    wIn[1] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // G
	    wIn[2] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // B
	    
	    return accum;
	}
	
	//BGR
	private static VirtualPointer Unroll3BytesSwap(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		wIn[2] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // B
	    wIn[1] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // G
	    wIn[0] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true)); // R
	    
	    return accum;
	}
	
	private static VirtualPointer UnrollLabV2_8(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		wIn[0] = FomLabV2ToLabV4(lcms2_internal.FROM_8_TO_16(proc.readInt8(true))); // L
	    wIn[1] = FomLabV2ToLabV4(lcms2_internal.FROM_8_TO_16(proc.readInt8(true))); // a
	    wIn[2] = FomLabV2ToLabV4(lcms2_internal.FROM_8_TO_16(proc.readInt8(true))); // b
	    
	    return accum;
	}
	
	private static VirtualPointer UnrollALabV2_8(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		accum.movePosition(1); // A
		wIn[0] = FomLabV2ToLabV4(lcms2_internal.FROM_8_TO_16(proc.readInt8(true))); // L
	    wIn[1] = FomLabV2ToLabV4(lcms2_internal.FROM_8_TO_16(proc.readInt8(true))); // a
	    wIn[2] = FomLabV2ToLabV4(lcms2_internal.FROM_8_TO_16(proc.readInt8(true))); // b
	    
	    return accum;
	}
	
	private static VirtualPointer UnrollLabV2_16(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		wIn[0] = FomLabV2ToLabV4(proc.readInt16(true)); // L
	    wIn[1] = FomLabV2ToLabV4(proc.readInt16(true)); // a
	    wIn[2] = FomLabV2ToLabV4(proc.readInt16(true)); // b
	    
	    return accum;
	}
	
	// Monochrome + alpha. Alpha is lost
	private static VirtualPointer Unroll2Bytes(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		wIn[0] = wIn[1] = wIn[2] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true));      // L
	    wIn[3] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true));                        // alpha
	    return accum;
	}
	
	private static VirtualPointer Unroll2ByteSwapFirst(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		wIn[3] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true));                       // alpha
		wIn[0] = wIn[1] = wIn[2] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true));     // L
	    return accum;
	}
	
	// Monochrome duplicates L into RGB for null-transforms
	private static VirtualPointer Unroll1Byte(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		wIn[0] = wIn[1] = wIn[2] = lcms2_internal.FROM_8_TO_16(accum.getProcessor().readInt8(true));      // L
	    return accum;
	}
	
	private static VirtualPointer Unroll1ByteSkip2(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		wIn[0] = wIn[1] = wIn[2] = lcms2_internal.FROM_8_TO_16(accum.getProcessor().readInt8(true));      // L
		accum.movePosition(2);
	    return accum;
	}
	
	private static VirtualPointer Unroll1ByteReversed(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		wIn[0] = wIn[1] = wIn[2] = REVERSE_FLAVOR_16(lcms2_internal.FROM_8_TO_16(accum.getProcessor().readInt8(true)));      // L
	    return accum;
	}
	
	private static VirtualPointer UnrollAnyWords(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		int nChan          = lcms2.T_CHANNELS(info.InputFormat);
		boolean SwapEndian = lcms2.T_ENDIAN16(info.InputFormat);
	    boolean DoSwap     = lcms2.T_DOSWAP(info.InputFormat);
	    boolean Reverse    = lcms2.T_FLAVOR(info.InputFormat);
	    boolean SwapFirst  = lcms2.T_SWAPFIRST(info.InputFormat);
	    int Extra          = lcms2.T_EXTRA(info.InputFormat);
	    boolean ExtraFirst = DoSwap && !SwapFirst;
	    int i;
	    
	    if (ExtraFirst)
	    {
	        accum.movePosition(Extra * /*sizeof(cmsUInt16Number)*/2);
	    }
	    
	    VirtualPointer.TypeProcessor proc = accum.getProcessor();
	    for (i = 0; i < nChan; i++)
	    {
	        int index = DoSwap ? (nChan - i - 1) : i;
	        short v = proc.readInt16(true);
	        
	        if (SwapEndian)
	        {
	        	v = CHANGE_ENDIAN(v);
	        }
	        
	        wIn[index] = Reverse ? REVERSE_FLAVOR_16(v) : v;
	    }
	    
	    if (!ExtraFirst)
	    {
	    	accum.movePosition(Extra * /*sizeof(cmsUInt16Number)*/2);
	    }
	    
	    if (Extra == 0 && SwapFirst)
	    {
	        short tmp = wIn[0];
	        
	        System.arraycopy(wIn, 1, wIn, 0, (nChan-1)/* * sizeof(cmsUInt16Number)*/);
	        wIn[nChan-1] = tmp;
	    }
	    
	    return accum;
	}
	
	private static VirtualPointer UnrollPlanarWords(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		int nChan          = lcms2.T_CHANNELS(info.InputFormat);
		boolean DoSwap     = lcms2.T_DOSWAP(info.InputFormat);
	    boolean Reverse    = lcms2.T_FLAVOR(info.InputFormat);
	    boolean SwapEndian = lcms2.T_ENDIAN16(info.InputFormat);
	    int i;
	    int Init = accum.getPosition();
	    
	    if (DoSwap)
	    {
	        accum.movePosition(lcms2.T_EXTRA(info.InputFormat) * Stride * /*sizeof(cmsUInt16Number)*/2);
	    }
	    
	    VirtualPointer.TypeProcessor proc = accum.getProcessor();
	    for (i = 0; i < nChan; i++)
	    {
	        int index = DoSwap ? (nChan - i - 1) : i;
	        short v = proc.readInt16();
	        
	        if (SwapEndian)
	        {
	        	v = CHANGE_ENDIAN(v);
	        }
	        
	        wIn[index] = Reverse ? REVERSE_FLAVOR_16(v) : v;
	        
	        accum.movePosition(Stride * /*sizeof(cmsUInt16Number)*/2);
	    }
	    
	    accum.setPosition(Init + /*sizeof(cmsUInt16Number)*/2);
	    return accum;
	}
	
	private static VirtualPointer Unroll4Words(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		wIn[0] = proc.readInt16(true); // C
	    wIn[1] = proc.readInt16(true); // M
	    wIn[2] = proc.readInt16(true); // Y
	    wIn[3] = proc.readInt16(true); // K
	    
	    return accum;
	}
	
	private static VirtualPointer Unroll4WordsReverse(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		wIn[0] = REVERSE_FLAVOR_16(proc.readInt16(true)); // C
	    wIn[1] = REVERSE_FLAVOR_16(proc.readInt16(true)); // M
	    wIn[2] = REVERSE_FLAVOR_16(proc.readInt16(true)); // Y
	    wIn[3] = REVERSE_FLAVOR_16(proc.readInt16(true)); // K
	    
	    return accum;
	}
	
	private static VirtualPointer Unroll4WordsSwapFirst(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		wIn[3] = proc.readInt16(true); // K
	    wIn[0] = proc.readInt16(true); // C
	    wIn[1] = proc.readInt16(true); // M
	    wIn[2] = proc.readInt16(true); // Y
	    
	    return accum;
	}
	
	// KYMC
	private static VirtualPointer Unroll4WordsSwap(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		wIn[3] = proc.readInt16(true); // K
	    wIn[2] = proc.readInt16(true); // Y
	    wIn[1] = proc.readInt16(true); // M
	    wIn[0] = proc.readInt16(true); // C
	    
	    return accum;
	}
	
	private static VirtualPointer Unroll4WordsSwapSwapFirst(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		wIn[2] = proc.readInt16(true); // K
	    wIn[1] = proc.readInt16(true); // Y
	    wIn[0] = proc.readInt16(true); // M
	    wIn[3] = proc.readInt16(true); // C
	    
	    return accum;
	}
	
	private static VirtualPointer Unroll3Words(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		wIn[0] = proc.readInt16(true); // C R
	    wIn[1] = proc.readInt16(true); // M G
	    wIn[2] = proc.readInt16(true); // Y B
	    return accum;
	}
	
	private static VirtualPointer Unroll3WordsSwap(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		wIn[2] = proc.readInt16(true); // C R
	    wIn[1] = proc.readInt16(true); // M G
	    wIn[0] = proc.readInt16(true); // Y B
	    return accum;
	}
	
	private static VirtualPointer Unroll3WordsSkip1Swap(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		accum.movePosition(2); //A
		wIn[2] = proc.readInt16(true); // C R
	    wIn[1] = proc.readInt16(true); // M G
	    wIn[0] = proc.readInt16(true); // Y B
	    
	    return accum;
	}
	
	private static VirtualPointer Unroll3WordsSkip1SwapFirst(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		accum.movePosition(2); //A
		wIn[0] = proc.readInt16(true); // C R
	    wIn[1] = proc.readInt16(true); // M G
	    wIn[2] = proc.readInt16(true); // Y B
	    
	    return accum;
	}
	
	private static VirtualPointer Unroll1Word(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		wIn[0] = wIn[1] = wIn[2] = accum.getProcessor().readInt16(true); // L
	    return accum;
	}
	
	private static VirtualPointer Unroll1WordReversed(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		wIn[0] = wIn[1] = wIn[2] = REVERSE_FLAVOR_16(accum.getProcessor().readInt16(true));
	    return accum;
	}
	
	private static VirtualPointer Unroll1WordSkip3(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		wIn[0] = wIn[1] = wIn[2] = accum.getProcessor().readInt16();
		
		accum.movePosition(8);
	    return accum;
	}
	
	private static VirtualPointer Unroll2Words(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		wIn[0] = wIn[1] = wIn[2] = proc.readInt16(true); // L
		wIn[3] = proc.readInt16(true);                   // alpha
		
	    return accum;
	}
	
	private static VirtualPointer Unroll2WordSwapFirst(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		wIn[3] = proc.readInt16(true);                   // alpha
		wIn[0] = wIn[1] = wIn[2] = proc.readInt16(true); // L
		
	    return accum;
	}
	
	// This is a conversion of Lab double to 16 bits
	private static VirtualPointer UnrollLabDoubleTo16(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		if (lcms2.T_PLANAR(info.InputFormat))
		{
			int pos = accum.getPosition();
			
	        cmsCIELab Lab = new cmsCIELab();
	        
	        Lab.L = proc.readDouble();
	        accum.movePosition(Stride * 8);
	        Lab.a = proc.readDouble();
	        accum.movePosition(Stride * 8);
	        Lab.b = proc.readDouble();
	        
	        cmspcs.cmsFloat2LabEncoded(wIn, Lab);
	        accum.setPosition(pos + /*sizeof(cmsFloat64Number)*/8);
	    }
	    else
	    {
	    	cmspcs.cmsFloat2LabEncoded(wIn, (cmsCIELab)proc.readObject(cmsCIELab.class, true));
	    }
		return accum;
	}
	
	// This is a conversion of XYZ double to 16 bits
	private static VirtualPointer UnrollXYZDoubleTo16(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
		if (lcms2.T_PLANAR(info.InputFormat))
		{
			int pos = accum.getPosition();
			
			cmsCIEXYZ XYZ = new cmsCIEXYZ();
	        
			XYZ.X = proc.readDouble();
	        accum.movePosition(Stride * 8);
	        XYZ.Y = proc.readDouble();
	        accum.movePosition(Stride * 8);
	        XYZ.Z = proc.readDouble();
	        
	        cmspcs.cmsFloat2XYZEncoded(wIn, XYZ);
	        accum.setPosition(pos + /*sizeof(cmsFloat64Number)*/8);
	    }
	    else
	    {
	    	cmspcs.cmsFloat2XYZEncoded(wIn, (cmsCIEXYZ)proc.readObject(cmsCIEXYZ.class, true));
	    }
		return accum;
	}
	
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
	
	// Inks does come in percentage, remaining cases are between 0..1.0, again to 16 bits
	private static VirtualPointer UnrollDoubleTo16(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
	    int nChan      = lcms2.T_CHANNELS(info.InputFormat);
	    boolean Planar = lcms2.T_PLANAR(info.InputFormat);
	    int i;
	    double v;
	    double maximum = IsInkSpace(info.InputFormat) ? 655.35 : 65535.0;
	    
	    int pos = accum.getPosition();
	    VirtualPointer.TypeProcessor Inks = accum.getProcessor();
	    for (i = 0; i < nChan; i++)
	    {
	        if (Planar)
	        {
	        	accum.setPosition(pos + ((i * Stride) * 8));
	        	v = Inks.readDouble();
	        }
	        else
	        {
	        	accum.setPosition(pos + (i * 8));
	        	v = Inks.readDouble();
	        }
	        
	        wIn[i] = lcms2_internal._cmsQuickSaturateWord(v * maximum);
	    }
	    
	    if (lcms2.T_PLANAR(info.InputFormat))
	    {
	    	accum.setPosition(pos + /*sizeof(cmsFloat64Number)*/8);
	    }
	    else
	    {
	    	accum.setPosition(pos + (nChan + lcms2.T_EXTRA(info.InputFormat)) * /*sizeof(cmsFloat64Number)*/8);
	    }
	    return accum;
	}
	
	private static VirtualPointer UnrollFloatTo16(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
	    int nChan      = lcms2.T_CHANNELS(info.InputFormat);
	    boolean Planar = lcms2.T_PLANAR(info.InputFormat);
	    int i;
	    float v;
	    double maximum = IsInkSpace(info.InputFormat) ? 655.35 : 65535.0;
	    
	    int pos = accum.getPosition();
	    VirtualPointer.TypeProcessor Inks = accum.getProcessor();
	    for (i = 0; i < nChan; i++)
	    {
	        if (Planar)
	        {
	        	accum.setPosition(pos + ((i * Stride) * 4));
	        	v = Inks.readFloat();
	        }
	        else
	        {
	        	accum.setPosition(pos + (i * 4));
	        	v = Inks.readFloat();
	        }
	        
	        wIn[i] = lcms2_internal._cmsQuickSaturateWord(v * maximum);
	    }
	    
	    if (lcms2.T_PLANAR(info.InputFormat))
	    {
	    	accum.setPosition(pos + /*sizeof(cmsFloat32Number)*/4);
	    }
	    else
	    {
	    	accum.setPosition(pos + (nChan + lcms2.T_EXTRA(info.InputFormat)) * /*sizeof(cmsFloat32Number)*/4);
	    }
	    return accum;
	}
	
	// For 1 channel, we need to duplicate data (it comes in 0..1.0 range)
	private static VirtualPointer UnrollDouble1Chan(_cmsTRANSFORM info, short[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		wIn[0] = wIn[1] = wIn[2] = lcms2_internal._cmsQuickSaturateWord(accum.getProcessor().readDouble(true) * 65535.0);
		
		return accum;
	}
	
	//-------------------------------------------------------------------------------------------------------------------
	
	// True cmsFloat32Number transformation. 
	
	// For anything going from cmsFloat32Number
	private static VirtualPointer UnrollFloatsToFloat(_cmsTRANSFORM info, float[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		int nChan      = lcms2.T_CHANNELS(info.InputFormat);
	    boolean Planar = lcms2.T_PLANAR(info.InputFormat);
	    int i;
	    double maximum = IsInkSpace(info.InputFormat) ? 100.0 : 1.0;
	    
	    int pos = accum.getPosition();
	    VirtualPointer.TypeProcessor Inks = accum.getProcessor();
	    for (i = 0; i < nChan; i++)
	    {
	        if (Planar)
	        {
	        	accum.setPosition(pos + ((i * Stride) * 4));
	        	wIn[i] = (float)(Inks.readFloat() / maximum);
	        }
	        else
	        {
	        	accum.setPosition(pos + (i * 4));
	        	wIn[i] = (float)(Inks.readFloat() / maximum);
	        }
	    }
	    
	    if (lcms2.T_PLANAR(info.InputFormat))
	    {
	    	accum.setPosition(pos + /*sizeof(cmsFloat32Number)*/4);
	    }
	    else
	    {
	    	accum.setPosition(pos + (nChan + lcms2.T_EXTRA(info.InputFormat)) * /*sizeof(cmsFloat32Number)*/4);
	    }
	    return accum;
	}
	
	// For anything going from double
	private static VirtualPointer UnrollDoublesToFloat(_cmsTRANSFORM info, float[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		int nChan      = lcms2.T_CHANNELS(info.InputFormat);
	    boolean Planar = lcms2.T_PLANAR(info.InputFormat);
	    int i;
	    double maximum = IsInkSpace(info.InputFormat) ? 100.0 : 1.0;
	    
	    int pos = accum.getPosition();
	    VirtualPointer.TypeProcessor Inks = accum.getProcessor();
	    for (i = 0; i < nChan; i++)
	    {
	        if (Planar)
	        {
	        	accum.setPosition(pos + ((i * Stride) * 8));
	        	wIn[i] = (float)(Inks.readDouble() / maximum);
	        }
	        else
	        {
	        	accum.setPosition(pos + (i * 8));
	        	wIn[i] = (float)(Inks.readDouble() / maximum);
	        }
	    }
	    
	    if (lcms2.T_PLANAR(info.InputFormat))
	    {
	    	accum.setPosition(pos + /*sizeof(cmsFloat64Number)*/8);
	    }
	    else
	    {
	    	accum.setPosition(pos + (nChan + lcms2.T_EXTRA(info.InputFormat)) * /*sizeof(cmsFloat64Number)*/8);
	    }
	    return accum;
	}
	
	// From Lab double to cmsFloat32Number
	private static VirtualPointer UnrollLabDoubleToFloat(_cmsTRANSFORM info, float[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
	    if (lcms2.T_PLANAR(info.InputFormat))
	    {
	    	int pos = accum.getPosition();
	        wIn[0] = (float)(proc.readDouble() / 100.0);         // from 0..100 to 0..1
	        accum.setPosition(pos + (Stride * 8));
	        wIn[1] = (float)((proc.readDouble() + 128) / 255.0); // form -128..+127 to 0..1
	        accum.setPosition(pos + ((Stride * 2) * 8));
	        wIn[2] = (float)((proc.readDouble() + 128) / 255.0);
	        
	        accum.setPosition(pos + /*sizeof(cmsFloat64Number)*/8);
	    }
	    else
	    {
	        wIn[0] = (float)(proc.readDouble(true) / 100.0);            // from 0..100 to 0..1
	        wIn[1] = (float)((proc.readDouble(true) + 128) / 255.0);    // form -128..+127 to 0..1
	        wIn[2] = (float)((proc.readDouble(true) + 128) / 255.0);
	    }
	    
	    return accum;
	}
	
	// From Lab double to cmsFloat32Number
	private static VirtualPointer UnrollLabFloatToFloat(_cmsTRANSFORM info, float[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
	    if (lcms2.T_PLANAR(info.InputFormat))
	    {
	    	int pos = accum.getPosition();
	        wIn[0] = (float)(proc.readFloat() / 100.0);         // from 0..100 to 0..1
	        accum.setPosition(pos + (Stride * 4));
	        wIn[1] = (float)((proc.readFloat() + 128) / 255.0); // form -128..+127 to 0..1
	        accum.setPosition(pos + ((Stride * 2) * 4));
	        wIn[2] = (float)((proc.readFloat() + 128) / 255.0);
	        
	        accum.setPosition(pos + /*sizeof(cmsFloat32Number)*/4);
	    }
	    else
	    {
	        wIn[0] = (float)(proc.readFloat(true) / 100.0);            // from 0..100 to 0..1
	        wIn[1] = (float)((proc.readFloat(true) + 128) / 255.0);    // form -128..+127 to 0..1
	        wIn[2] = (float)((proc.readFloat(true) + 128) / 255.0);
	    }
	    
	    return accum;
	}
	
	// 1.15 fixed point, that means maximum value is MAX_ENCODEABLE_XYZ (0xFFFF)
	private static VirtualPointer UnrollXYZDoubleToFloat(_cmsTRANSFORM info, float[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
	    if (lcms2.T_PLANAR(info.InputFormat))
	    {
	    	int pos = accum.getPosition();
	        wIn[0] = (float)(proc.readDouble() / lcms2_internal.MAX_ENCODEABLE_XYZ);
	        accum.setPosition(pos + (Stride * 8));
	        wIn[1] = (float)(proc.readDouble() / lcms2_internal.MAX_ENCODEABLE_XYZ);
	        accum.setPosition(pos + ((Stride * 2) * 8));
	        wIn[2] = (float)(proc.readDouble() / lcms2_internal.MAX_ENCODEABLE_XYZ);
	        
	        accum.setPosition(pos + /*sizeof(cmsFloat64Number)*/8);
	    }
	    else
	    {
	        wIn[0] = (float)(proc.readDouble(true) / lcms2_internal.MAX_ENCODEABLE_XYZ);
	        wIn[1] = (float)(proc.readDouble(true) / lcms2_internal.MAX_ENCODEABLE_XYZ);
	        wIn[2] = (float)(proc.readDouble(true) / lcms2_internal.MAX_ENCODEABLE_XYZ);
	    }
	    
	    return accum;
	}
	
	private static VirtualPointer UnrollXYZFloatToFloat(_cmsTRANSFORM info, float[] wIn, VirtualPointer accum, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		accum = new VirtualPointer(accum);
//#endif
		
		VirtualPointer.TypeProcessor proc = accum.getProcessor();
	    if (lcms2.T_PLANAR(info.InputFormat))
	    {
	    	int pos = accum.getPosition();
	        wIn[0] = (float)(proc.readFloat() / lcms2_internal.MAX_ENCODEABLE_XYZ);
	        accum.setPosition(pos + (Stride * 4));
	        wIn[1] = (float)(proc.readFloat() / lcms2_internal.MAX_ENCODEABLE_XYZ);
	        accum.setPosition(pos + ((Stride * 2) * 4));
	        wIn[2] = (float)(proc.readFloat() / lcms2_internal.MAX_ENCODEABLE_XYZ);
	        
	        accum.setPosition(pos + /*sizeof(cmsFloat32Number)*/4);
	    }
	    else
	    {
	        wIn[0] = (float)(proc.readFloat(true) / lcms2_internal.MAX_ENCODEABLE_XYZ);
	        wIn[1] = (float)(proc.readFloat(true) / lcms2_internal.MAX_ENCODEABLE_XYZ);
	        wIn[2] = (float)(proc.readFloat(true) / lcms2_internal.MAX_ENCODEABLE_XYZ);
	    }
	    
	    return accum;
	}
	
	// Packing routines -----------------------------------------------------------------------------------------------------------
	
	// Generic chunky for byte
	
	private static VirtualPointer PackAnyBytes(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		int nChan         = lcms2.T_CHANNELS(info.OutputFormat);
	    boolean DoSwap    = lcms2.T_DOSWAP(info.OutputFormat);
	    boolean Reverse   = lcms2.T_FLAVOR(info.OutputFormat);
	    int Extra         = lcms2.T_EXTRA(info.OutputFormat);
	    boolean SwapFirst = lcms2.T_SWAPFIRST(info.OutputFormat);
	    boolean ExtraFirst= DoSwap && !SwapFirst;
	    VirtualPointer swap1;
	    byte v = 0;
	    int i;
	    
	    swap1 = new VirtualPointer(output);
	    
	    if (ExtraFirst)
	    {
	    	output.movePosition(Extra);
	    }
	    
	    VirtualPointer.TypeProcessor proc = output.getProcessor();
	    for (i = 0; i < nChan; i++)
	    {
	        int index = DoSwap ? (nChan - i - 1) : i;
	        
	        v = lcms2_internal.FROM_16_TO_8(wOut[index]);
	        
	        if (Reverse)
	        {
	            v = REVERSE_FLAVOR_8(v);
	        }
	        
	        proc.write(v, true);
	    }
	    
	    if (!ExtraFirst)
	    {
	    	output.movePosition(Extra);
	    }
	    
	    if (Extra == 0 && SwapFirst)
	    {
	    	swap1.memmove(1, 0, nChan-1);
	    	swap1.writeRaw(v & 0xFF);
	    }
	    
	    return output;
	}
	
	private static VirtualPointer PackAnyWords(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		int nChan          = lcms2.T_CHANNELS(info.OutputFormat);
		boolean SwapEndian = lcms2.T_ENDIAN16(info.InputFormat);
	    boolean DoSwap     = lcms2.T_DOSWAP(info.OutputFormat);
	    boolean Reverse    = lcms2.T_FLAVOR(info.OutputFormat);
	    int Extra          = lcms2.T_EXTRA(info.OutputFormat);
	    boolean SwapFirst  = lcms2.T_SWAPFIRST(info.OutputFormat);
	    boolean ExtraFirst = DoSwap && !SwapFirst;
	    VirtualPointer swap1;
	    short v = 0;
	    int i;
	    
	    swap1 = new VirtualPointer(output);
	    
	    if (ExtraFirst)
	    {
	    	output.movePosition(Extra * /*sizeof(cmsUInt16Number)*/2);
	    }
	    
	    VirtualPointer.TypeProcessor proc = output.getProcessor();
	    for (i = 0; i < nChan; i++)
	    {
	        int index = DoSwap ? (nChan - i - 1) : i;
	        
	        v = wOut[index];
	        
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
	    	output.movePosition(Extra * /*sizeof(cmsUInt16Number)*/2);
	    }
	    
	    if (Extra == 0 && SwapFirst)
	    {
	        swap1.memmove(1, 0, (nChan-1) * /*sizeof(cmsUInt16Number)*/2);
	    	swap1.writeRaw(v & 0xFF);
	    }
	    
	    return output;
	}
	
	private static VirtualPointer PackPlanarBytes(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		int nChan       = lcms2.T_CHANNELS(info.OutputFormat);
		boolean DoSwap  = lcms2.T_DOSWAP(info.OutputFormat);
	    boolean Reverse = lcms2.T_FLAVOR(info.OutputFormat);
	    int i;
	    int Init = output.getPosition();
	    
	    VirtualPointer.TypeProcessor proc = output.getProcessor();
	    for (i = 0; i < nChan; i++)
	    {
	        int index = DoSwap ? (nChan - i - 1) : i;
	        byte v = lcms2_internal.FROM_16_TO_8(wOut[index]);
	        
	        proc.write((byte)(Reverse ? REVERSE_FLAVOR_8(v) : v));
	        output.movePosition(Stride);
	    }
	    
	    output.setPosition(Init + 1);
	    return output;
	}
	
	private static VirtualPointer PackPlanarWords(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		int nChan          = lcms2.T_CHANNELS(info.OutputFormat);
	    boolean DoSwap     = lcms2.T_DOSWAP(info.OutputFormat);
	    boolean Reverse    = lcms2.T_FLAVOR(info.OutputFormat);
	    boolean SwapEndian = lcms2.T_ENDIAN16(info.OutputFormat);
	    int i;
	    int Init = output.getPosition();
	    short v;
	    
	    if (DoSwap)
	    {
	    	output.movePosition(lcms2.T_EXTRA(info.OutputFormat) * Stride * /*sizeof(cmsUInt16Number)*/2);
	    }
	    
	    VirtualPointer.TypeProcessor proc = output.getProcessor();
	    for (i=0; i < nChan; i++)
	    {
	        int index = DoSwap ? (nChan - i - 1) : i;
	        
	        v = wOut[index];
	        
	        if (SwapEndian)
	        {
	        	v = CHANGE_ENDIAN(v);
	        }

	        if (Reverse)
	        {
	        	v =  REVERSE_FLAVOR_16(v);
	        }
	        
	        proc.write(v);
	        output.movePosition(Stride * /*sizeof(cmsUInt16Number)*/2);
	    }
	    
	    output.setPosition(Init + /*sizeof(cmsUInt16Number)*/2);
	    return output;
	}
	
	// CMYKcm (unrolled for speed)
	private static VirtualPointer Pack6Bytes(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[0]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[1]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[2]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[3]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[4]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[5]), true);
	    
	    return output;
	}
	
	// KCMYcm
	
	private static VirtualPointer Pack6BytesSwap(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[5]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[4]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[3]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[2]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[1]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[0]), true);
	    
	    return output;
	}
	
	// CMYKcm
	private static VirtualPointer Pack6Words(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(wOut[0], true);
		proc.write(wOut[1], true);
		proc.write(wOut[2], true);
		proc.write(wOut[3], true);
		proc.write(wOut[4], true);
		proc.write(wOut[5], true);
	    
	    return output;
	}
	
	// KCMYcm
	private static VirtualPointer Pack6WordsSwap(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(wOut[5], true);
		proc.write(wOut[4], true);
		proc.write(wOut[3], true);
		proc.write(wOut[2], true);
		proc.write(wOut[1], true);
		proc.write(wOut[0], true);
	    
	    return output;
	}
	
	private static VirtualPointer Pack4Bytes(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[0]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[1]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[2]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[3]), true);
	    
	    return output;
	}
	
	private static VirtualPointer Pack4BytesReverse(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(REVERSE_FLAVOR_8(lcms2_internal.FROM_16_TO_8(wOut[0])), true);
		proc.write(REVERSE_FLAVOR_8(lcms2_internal.FROM_16_TO_8(wOut[1])), true);
		proc.write(REVERSE_FLAVOR_8(lcms2_internal.FROM_16_TO_8(wOut[2])), true);
		proc.write(REVERSE_FLAVOR_8(lcms2_internal.FROM_16_TO_8(wOut[3])), true);
	    
	    return output;
	}
	
	private static VirtualPointer Pack4BytesSwapFirst(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[3]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[0]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[1]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[2]), true);
	    
	    return output;
	}
	
	// ABGR
	private static VirtualPointer Pack4BytesSwap(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[3]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[2]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[1]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[0]), true);
	    
	    return output;
	}
	
	private static VirtualPointer Pack4BytesSwapSwapFirst(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[2]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[1]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[0]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[3]), true);
	    
	    return output;
	}
	
	private static VirtualPointer Pack4Words(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(wOut[0], true);
		proc.write(wOut[1], true);
		proc.write(wOut[2], true);
		proc.write(wOut[3], true);
	    
	    return output;
	}
	
	private static VirtualPointer Pack4WordsReverse(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(REVERSE_FLAVOR_16(wOut[0]), true);
		proc.write(REVERSE_FLAVOR_16(wOut[1]), true);
		proc.write(REVERSE_FLAVOR_16(wOut[2]), true);
		proc.write(REVERSE_FLAVOR_16(wOut[3]), true);
	    
	    return output;
	}
	
	// ABGR
	private static VirtualPointer Pack4WordsSwap(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(wOut[3], true);
		proc.write(wOut[2], true);
		proc.write(wOut[1], true);
		proc.write(wOut[0], true);
	    
	    return output;
	}
	
	// CMYK
	private static VirtualPointer Pack4WordsBigEndian(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(CHANGE_ENDIAN(wOut[0]), true);
		proc.write(CHANGE_ENDIAN(wOut[1]), true);
		proc.write(CHANGE_ENDIAN(wOut[2]), true);
		proc.write(CHANGE_ENDIAN(wOut[3]), true);
	    
	    return output;
	}
	
	private static VirtualPointer PackLabV2_8(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(lcms2_internal.FROM_16_TO_8(FomLabV4ToLabV2(wOut[0])), true);
		proc.write(lcms2_internal.FROM_16_TO_8(FomLabV4ToLabV2(wOut[1])), true);
		proc.write(lcms2_internal.FROM_16_TO_8(FomLabV4ToLabV2(wOut[2])), true);
	    
	    return output;
	}
	
	private static VirtualPointer PackALabV2_8(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		output.movePosition(1);
		proc.write(lcms2_internal.FROM_16_TO_8(FomLabV4ToLabV2(wOut[0])), true);
		proc.write(lcms2_internal.FROM_16_TO_8(FomLabV4ToLabV2(wOut[1])), true);
		proc.write(lcms2_internal.FROM_16_TO_8(FomLabV4ToLabV2(wOut[2])), true);
	    
	    return output;
	}
	
	private static VirtualPointer PackLabV2_16(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(FomLabV4ToLabV2(wOut[0]), true);
		proc.write(FomLabV4ToLabV2(wOut[1]), true);
		proc.write(FomLabV4ToLabV2(wOut[2]), true);
	    
	    return output;
	}
	
	private static VirtualPointer Pack3Bytes(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[0]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[1]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[2]), true);
	    
	    return output;
	}
	
	private static VirtualPointer Pack3BytesOptimized(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write((byte)(wOut[0] & 0xFF), true);
		proc.write((byte)(wOut[1] & 0xFF), true);
		proc.write((byte)(wOut[2] & 0xFF), true);
	    
	    return output;
	}
	
	private static VirtualPointer Pack3BytesSwap(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[2]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[1]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[0]), true);
	    
	    return output;
	}
	
	private static VirtualPointer Pack3BytesSwapOptimized(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write((byte)(wOut[2] & 0xFF), true);
		proc.write((byte)(wOut[1] & 0xFF), true);
		proc.write((byte)(wOut[0] & 0xFF), true);
	    
	    return output;
	}
	
	private static VirtualPointer Pack3Words(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(wOut[0], true);
		proc.write(wOut[1], true);
		proc.write(wOut[2], true);
	    
	    return output;
	}
	
	private static VirtualPointer Pack3WordsSwap(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(wOut[2], true);
		proc.write(wOut[1], true);
		proc.write(wOut[0], true);
	    
	    return output;
	}
	
	private static VirtualPointer Pack3WordsBigEndian(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(CHANGE_ENDIAN(wOut[0]), true);
		proc.write(CHANGE_ENDIAN(wOut[1]), true);
		proc.write(CHANGE_ENDIAN(wOut[2]), true);
	    
	    return output;
	}
	
	private static VirtualPointer Pack3BytesAndSkip1(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[0]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[1]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[2]), true);
		output.movePosition(1);
	    
	    return output;
	}
	
	private static VirtualPointer Pack3BytesAndSkip1Optimized(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write((byte)(wOut[0] & 0xFF), true);
		proc.write((byte)(wOut[1] & 0xFF), true);
		proc.write((byte)(wOut[2] & 0xFF), true);
		output.movePosition(1);
	    
	    return output;
	}
	
	private static VirtualPointer Pack3BytesAndSkip1SwapFirst(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		output.movePosition(1);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[0]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[1]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[2]), true);
	    
	    return output;
	}
	
	private static VirtualPointer Pack3BytesAndSkip1SwapFirstOptimized(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		output.movePosition(1);
		proc.write((byte)(wOut[0] & 0xFF), true);
		proc.write((byte)(wOut[1] & 0xFF), true);
		proc.write((byte)(wOut[2] & 0xFF), true);
	    
	    return output;
	}
	
	private static VirtualPointer Pack3BytesAndSkip1Swap(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		output.movePosition(1);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[2]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[1]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[0]), true);
	    
	    return output;
	}
	
	private static VirtualPointer Pack3BytesAndSkip1SwapOptimized(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		output.movePosition(1);
		proc.write((byte)(wOut[2] & 0xFF), true);
		proc.write((byte)(wOut[1] & 0xFF), true);
		proc.write((byte)(wOut[0] & 0xFF), true);
	    
	    return output;
	}
	
	private static VirtualPointer Pack3BytesAndSkip1SwapSwapFirst(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[2]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[1]), true);
		proc.write(lcms2_internal.FROM_16_TO_8(wOut[0]), true);
		output.movePosition(1);
	    
	    return output;
	}
	
	private static VirtualPointer Pack3BytesAndSkip1SwapSwapFirstOptimized(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write((byte)(wOut[2] & 0xFF), true);
		proc.write((byte)(wOut[1] & 0xFF), true);
		proc.write((byte)(wOut[0] & 0xFF), true);
		output.movePosition(1);
	    
	    return output;
	}
	
	private static VirtualPointer Pack3WordsAndSkip1(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(wOut[0], true);
		proc.write(wOut[1], true);
		proc.write(wOut[2], true);
		output.movePosition(2 + 2);
	    
	    return output;
	}
	
	private static VirtualPointer Pack3WordsAndSkip1Swap(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		output.movePosition(2);
		proc.write(wOut[2], true);
		proc.write(wOut[1], true);
		proc.write(wOut[0], true);
		output.movePosition(2);
	    
	    return output;
	}
	
	private static VirtualPointer Pack3WordsAndSkip1SwapFirst(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		output.movePosition(2);
		proc.write(wOut[0], true);
		proc.write(wOut[1], true);
		proc.write(wOut[2], true);
		output.movePosition(2);
	    
	    return output;
	}
	
	private static VirtualPointer Pack3WordsAndSkip1SwapSwapFirst(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor proc = output.getProcessor();
		proc.write(wOut[2], true);
		proc.write(wOut[1], true);
		proc.write(wOut[0], true);
		output.movePosition(2 + 2);
	    
	    return output;
	}
	
	private static VirtualPointer Pack1Byte(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		output.getProcessor().write(lcms2_internal.FROM_16_TO_8(wOut[0]), true);
		return output;
	}
	
	private static VirtualPointer Pack1ByteReversed(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		output.getProcessor().write(lcms2_internal.FROM_16_TO_8(REVERSE_FLAVOR_16(wOut[0])), true);
		return output;
	}
	
	private static VirtualPointer Pack1ByteSkip1(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		output.getProcessor().write(lcms2_internal.FROM_16_TO_8(wOut[0]), true);
		output.movePosition(1);
		return output;
	}
	
	private static VirtualPointer Pack1ByteSkip1SwapFirst(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		output.movePosition(1);
		output.getProcessor().write(lcms2_internal.FROM_16_TO_8(wOut[0]), true);
		
		return output;
	}
	
	private static VirtualPointer Pack1Word(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		output.getProcessor().write(wOut[0], true);
		
		return output;
	}
	
	private static VirtualPointer Pack1WordReversed(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		output.getProcessor().write(REVERSE_FLAVOR_16(wOut[0]), true);
		
		return output;
	}
	
	private static VirtualPointer Pack1WordBigEndian(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		output.getProcessor().write(CHANGE_ENDIAN(wOut[0]), true);
		
		return output;
	}
	
	private static VirtualPointer Pack1WordSkip1(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		output.getProcessor().write(wOut[0]);
		output.movePosition(2 + 2);
		
		return output;
	}
	
	private static VirtualPointer Pack1WordSkip1SwapFirst(_cmsTRANSFORM info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		output.movePosition(2);
		output.getProcessor().write(wOut[0], true);
		
		return output;
	}
	
	// Unencoded Float values -- don't try optimize speed
	private static VirtualPointer PackLabDoubleFrom16(_cmsTRANSFORM Info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		cmsCIELab Lab = new cmsCIELab();
		cmspcs.cmsLabEncoded2Float(Lab, wOut);
		if (lcms2.T_PLANAR(Info.OutputFormat))
		{
	        int Out = output.getPosition();
	        
	        VirtualPointer.TypeProcessor proc = output.getProcessor();
	        proc.write(Lab.L);
	        output.setPosition(Out + (Stride * 8));
	        proc.write(Lab.a);
	        output.setPosition(Out + ((Stride * 2) * 8));
	        proc.write(Lab.b);
	        
	        output.setPosition(Out + /*sizeof(cmsFloat64Number)*/8);
	    }
	    else
	    {
	    	output.getProcessor().write(Lab);
	    	output.movePosition(/*sizeof(cmsCIELab)*/cmsCIELab.SIZE + lcms2.T_EXTRA(Info.OutputFormat) * /*sizeof(cmsFloat64Number)*/8);           
	    }
		return output;
	}
	
	private static VirtualPointer PackXYZDoubleFrom16(_cmsTRANSFORM Info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		cmsCIEXYZ XYZ = new cmsCIEXYZ();
		cmspcs.cmsXYZEncoded2Float(XYZ, wOut);
		if (lcms2.T_PLANAR(Info.OutputFormat))
		{
	        int Out = output.getPosition();
	        
	        VirtualPointer.TypeProcessor proc = output.getProcessor();
	        proc.write(XYZ.X);
	        output.setPosition(Out + (Stride * 8));
	        proc.write(XYZ.Y);
	        output.setPosition(Out + ((Stride * 2) * 8));
	        proc.write(XYZ.Z);
	        
	        output.setPosition(Out + /*sizeof(cmsFloat64Number)*/8);
	    }
	    else
	    {
	    	output.getProcessor().write(XYZ);
	    	output.movePosition(/*sizeof(cmsCIEXYZ)*/cmsCIEXYZ.SIZE + lcms2.T_EXTRA(Info.OutputFormat) * /*sizeof(cmsFloat64Number)*/8);           
	    }
		return output;
	}
	
	private static VirtualPointer PackDoubleFrom16(_cmsTRANSFORM Info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		int Inks = output.getPosition();
	    int nChan = lcms2.T_CHANNELS(Info.OutputFormat);
	    int i;
	    double maximum = IsInkSpace(Info.InputFormat) ? 655.35 : 65535.0;
	    
	    VirtualPointer.TypeProcessor proc = output.getProcessor();
	    if (lcms2.T_PLANAR(Info.OutputFormat))
	    {
	        for (i = 0; i < nChan; i++)
	        {
	        	output.setPosition(Inks + ((i * Stride) * 8));
	        	proc.write(wOut[i] / maximum);
	        }
	        
	        output.setPosition(Inks + /*sizeof(cmsFloat64Number)*/8);
	    }
	    else
	    {
	        for (i = 0; i < nChan; i++)
	        {
	        	output.setPosition(Inks + (i * 8));
	        	proc.write(wOut[i] / maximum);
	        }
	        
	        output.setPosition(Inks + (nChan + lcms2.T_EXTRA(Info.OutputFormat)) * /*sizeof(cmsFloat64Number)*/8);
	    }
		
		return output;
	}
	
	private static VirtualPointer PackFloatFrom16(_cmsTRANSFORM Info, short[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		int Inks = output.getPosition();
	    int nChan = lcms2.T_CHANNELS(Info.OutputFormat);
	    int i;
	    double maximum = IsInkSpace(Info.InputFormat) ? 655.35 : 65535.0;
	    
	    VirtualPointer.TypeProcessor proc = output.getProcessor();
	    if (lcms2.T_PLANAR(Info.OutputFormat))
	    {
	        for (i = 0; i < nChan; i++)
	        {
	        	output.setPosition(Inks + ((i * Stride) * 4));
	        	proc.write((float)(wOut[i] / maximum));
	        }
	        
	        output.setPosition(Inks + /*sizeof(cmsFloat32Number)*/4);
	    }
	    else
	    {
	        for (i = 0; i < nChan; i++)
	        {
	        	output.setPosition(Inks + (i * 4));
	        	proc.write((float)(wOut[i] / maximum));
	        }
	        
	        output.setPosition(Inks + (nChan + lcms2.T_EXTRA(Info.OutputFormat)) * /*sizeof(cmsFloat32Number)*/4);
	    }
		
		return output;
	}
	
	// --------------------------------------------------------------------------------------------------------
	
	private static VirtualPointer PackChunkyFloatsFromFloat(_cmsTRANSFORM info, float[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		int nChan          = lcms2.T_CHANNELS(info.OutputFormat);
	    boolean DoSwap     = lcms2.T_DOSWAP(info.OutputFormat);
	    boolean Reverse    = lcms2.T_FLAVOR(info.OutputFormat);
	    int Extra          = lcms2.T_EXTRA(info.OutputFormat);
	    boolean SwapFirst  = lcms2.T_SWAPFIRST(info.OutputFormat);
	    boolean ExtraFirst = DoSwap && !SwapFirst;
	    double maximum     = IsInkSpace(info.OutputFormat) ? 100.0 : 1.0;
	    VirtualPointer swap1;
	    double v = 0;
	    int i;
	    
	    swap1 = new VirtualPointer(output);
	    
	    if (ExtraFirst)
	    {
	    	output.movePosition(Extra * /*sizeof(cmsFloat32Number)*/4);
	    }
	    
	    VirtualPointer.TypeProcessor proc = output.getProcessor();
	    for (i = 0; i < nChan; i++)
	    {
	        int index = DoSwap ? (nChan - i - 1) : i;
	        
	        v = wOut[index] * maximum;
	        
	        if (Reverse)
	        {
	        	v = maximum - v;
	        }
	        
	        proc.write((float)v, true);
	    }
	    
	    if (!ExtraFirst)
	    {
	    	output.movePosition(Extra * /*sizeof(cmsFloat32Number)*/4);
	    }
	    
	    if (Extra == 0 && SwapFirst)
	    {
	    	swap1.memmove(1 * 4, 0, (nChan-1)* /*sizeof(cmsFloat32Number)*/4);
	    	swap1.getProcessor().write((float)v);
	    }
		
		return output;
	}
	
	private static VirtualPointer PackPlanarFloatsFromFloat(_cmsTRANSFORM info, float[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		int nChan       = lcms2.T_CHANNELS(info.OutputFormat);
		boolean DoSwap  = lcms2.T_DOSWAP(info.OutputFormat);
	    boolean Reverse = lcms2.T_FLAVOR(info.OutputFormat);
	    int i;
	    int Init = output.getPosition();
	    double maximum = IsInkSpace(info.OutputFormat) ? 100.0 : 1.0;
	    double v;
	    
	    if (DoSwap)
	    {
	        output.movePosition(lcms2.T_EXTRA(info.OutputFormat) * Stride * /*sizeof(cmsFloat32Number)*/4);
	    }
	    
	    VirtualPointer.TypeProcessor proc = output.getProcessor();
	    for (i=0; i < nChan; i++)
	    {
	        int index = DoSwap ? (nChan - i - 1) : i;
	        
	        v = wOut[index] * maximum;
	        
	        if (Reverse)
	        {
	        	v =  maximum - v;
	        }
	        
	        proc.write((float)v);
	        output.movePosition(Stride * /*sizeof(cmsFloat32Number)*/4);
	    }
	    
	    output.setPosition(Init + /*sizeof(cmsFloat32Number)*/4);
	    return output;
	}
	
	private static VirtualPointer PackChunkyDoublesFromFloat(_cmsTRANSFORM info, float[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		int nChan          = lcms2.T_CHANNELS(info.OutputFormat);
	    boolean DoSwap     = lcms2.T_DOSWAP(info.OutputFormat);
	    boolean Reverse    = lcms2.T_FLAVOR(info.OutputFormat);
	    int Extra          = lcms2.T_EXTRA(info.OutputFormat);
	    boolean SwapFirst  = lcms2.T_SWAPFIRST(info.OutputFormat);
	    boolean ExtraFirst = DoSwap && !SwapFirst;
	    double maximum     = IsInkSpace(info.OutputFormat) ? 100.0 : 1.0;
	    VirtualPointer swap1;
	    double v = 0;
	    int i;
	    
	    swap1 = new VirtualPointer(output);
	    
	    if (ExtraFirst)
	    {
	    	output.movePosition(Extra * /*sizeof(cmsFloat64Number)*/8);
	    }
	    
	    VirtualPointer.TypeProcessor proc = output.getProcessor();
	    for (i = 0; i < nChan; i++)
	    {
	        int index = DoSwap ? (nChan - i - 1) : i;
	        
	        v = wOut[index] * maximum;
	        
	        if (Reverse)
	        {
	        	v = maximum - v;
	        }
	        
	        proc.write(v, true);
	    }
	    
	    if (!ExtraFirst)
	    {
	    	output.movePosition(Extra * /*sizeof(cmsFloat64Number)*/8);
	    }
	    
	    if (Extra == 0 && SwapFirst)
	    {
	    	swap1.memmove(1 * 8, 0, (nChan-1)* /*sizeof(cmsFloat64Number)*/8);
	    	swap1.getProcessor().write(v);
	    }
		
		return output;
	}
	
	private static VirtualPointer PackPlanarDoublesFromFloat(_cmsTRANSFORM info, float[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		int nChan       = lcms2.T_CHANNELS(info.OutputFormat);
		boolean DoSwap  = lcms2.T_DOSWAP(info.OutputFormat);
	    boolean Reverse = lcms2.T_FLAVOR(info.OutputFormat);
	    int i;
	    int Init = output.getPosition();
	    double maximum = IsInkSpace(info.OutputFormat) ? 100.0 : 1.0;
	    double v;
	    
	    if (DoSwap)
	    {
	        output.movePosition(lcms2.T_EXTRA(info.OutputFormat) * Stride * /*sizeof(cmsFloat64Number)*/8);
	    }
	    
	    VirtualPointer.TypeProcessor proc = output.getProcessor();
	    for (i=0; i < nChan; i++)
	    {
	        int index = DoSwap ? (nChan - i - 1) : i;
	        
	        v = wOut[index] * maximum;
	        
	        if (Reverse)
	        {
	        	v =  maximum - v;
	        }
	        
	        proc.write(v);
	        output.movePosition(Stride * /*sizeof(cmsFloat64Number)*/8);
	    }
	    
	    output.setPosition(Init + /*sizeof(cmsFloat64Number)*/8);
	    return output;
	}
	
	private static VirtualPointer PackLabFloatFromFloat(_cmsTRANSFORM Info, float[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor Out = output.getProcessor();
	    if (lcms2.T_PLANAR(Info.OutputFormat))
	    {
	    	int pos = output.getPosition();
	    	Out.write((float)(wOut[0] * 100.0));
	    	output.setPosition(pos + (Stride * 4));
	    	Out.write((float)(wOut[1] * 255.0 - 128.0));
	    	output.setPosition(pos + ((Stride * 2) * 4));
	    	Out.write((float)(wOut[2] * 255.0 - 128.0));
	        
	        output.setPosition(pos + /*sizeof(cmsFloat32Number)*/4);
	    }
	    else
	    {
	    	Out.write((float)(wOut[0] * 100.0), true);
	    	Out.write((float)(wOut[1] * 255.0 - 128.0), true);
	    	Out.write((float)(wOut[2] * 255.0 - 128.0), true);
	    	
	    	output.movePosition(lcms2.T_EXTRA(Info.OutputFormat) * /*sizeof(cmsFloat32Number)*/4);
	    }
	    
	    return output;
	}
	
	private static VirtualPointer PackLabDoubleFromFloat(_cmsTRANSFORM Info, float[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor Out = output.getProcessor();
	    if (lcms2.T_PLANAR(Info.OutputFormat))
	    {
	    	int pos = output.getPosition();
	    	Out.write(wOut[0] * 100.0);
	    	output.setPosition(pos + (Stride * 8));
	    	Out.write(wOut[1] * 255.0 - 128.0);
	    	output.setPosition(pos + ((Stride * 2) * 8));
	    	Out.write(wOut[2] * 255.0 - 128.0);
	        
	        output.setPosition(pos + /*sizeof(cmsFloat64Number)*/8);
	    }
	    else
	    {
	    	Out.write(wOut[0] * 100.0, true);
	    	Out.write(wOut[1] * 255.0 - 128.0, true);
	    	Out.write(wOut[2] * 255.0 - 128.0, true);
	    	
	    	output.movePosition(lcms2.T_EXTRA(Info.OutputFormat) * /*sizeof(cmsFloat64Number)*/8);
	    }
	    
	    return output;
	}
	
	// From 0..1 range to 0..MAX_ENCODEABLE_XYZ
	private static VirtualPointer PackXYZFloatFromFloat(_cmsTRANSFORM Info, float[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor Out = output.getProcessor();
	    if (lcms2.T_PLANAR(Info.OutputFormat))
	    {
	    	int pos = output.getPosition();
	    	Out.write((float)(wOut[0] * lcms2_internal.MAX_ENCODEABLE_XYZ));
	    	output.setPosition(pos + (Stride * 4));
	    	Out.write((float)(wOut[1] * lcms2_internal.MAX_ENCODEABLE_XYZ));
	    	output.setPosition(pos + ((Stride * 2) * 4));
	    	Out.write((float)(wOut[2] * lcms2_internal.MAX_ENCODEABLE_XYZ));
	        
	        output.setPosition(pos + /*sizeof(cmsFloat32Number)*/4);
	    }
	    else
	    {
	    	Out.write((float)(wOut[0] * lcms2_internal.MAX_ENCODEABLE_XYZ), true);
	    	Out.write((float)(wOut[1] * lcms2_internal.MAX_ENCODEABLE_XYZ), true);
	    	Out.write((float)(wOut[2] * lcms2_internal.MAX_ENCODEABLE_XYZ), true);
	    	
	    	output.movePosition(lcms2.T_EXTRA(Info.OutputFormat) * /*sizeof(cmsFloat32Number)*/4);
	    }
	    
	    return output;
	}
	
	// Same, but convert to double
	private static VirtualPointer PackXYZDoubleFromFloat(_cmsTRANSFORM Info, float[] wOut, VirtualPointer output, int Stride)
	{
//#ifdef CMS_REALLOC_PTR
		output = new VirtualPointer(output);
//#endif
		
		VirtualPointer.TypeProcessor Out = output.getProcessor();
	    if (lcms2.T_PLANAR(Info.OutputFormat))
	    {
	    	int pos = output.getPosition();
	    	Out.write(wOut[0] * lcms2_internal.MAX_ENCODEABLE_XYZ);
	    	output.setPosition(pos + (Stride * 8));
	    	Out.write(wOut[1] * lcms2_internal.MAX_ENCODEABLE_XYZ);
	    	output.setPosition(pos + ((Stride * 2) * 8));
	    	Out.write(wOut[2] * lcms2_internal.MAX_ENCODEABLE_XYZ);
	        
	        output.setPosition(pos + /*sizeof(cmsFloat64Number)*/8);
	    }
	    else
	    {
	    	Out.write(wOut[0] * lcms2_internal.MAX_ENCODEABLE_XYZ, true);
	    	Out.write(wOut[1] * lcms2_internal.MAX_ENCODEABLE_XYZ, true);
	    	Out.write(wOut[2] * lcms2_internal.MAX_ENCODEABLE_XYZ, true);
	    	
	    	output.movePosition(lcms2.T_EXTRA(Info.OutputFormat) * /*sizeof(cmsFloat64Number)*/8);
	    }
	    
	    return output;
	}
	
	// ----------------------------------------------------------------------------------------------------------------
	
	private static cmsFormatters16[] InputFormatters16;
	
	private static void setupInputFormatter16()
	{
		InputFormatters16 = new cmsFormatters16[]{
			//			    Type                             Mask                              Function
		    //  ----------------------------   ------------------------------------  ----------------------------
			new cmsFormatters16(lcms2.TYPE_Lab_DBL, ANYPLANAR, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return UnrollLabDoubleTo16(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16(lcms2.TYPE_XYZ_DBL, ANYPLANAR, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return UnrollXYZDoubleTo16(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16(lcms2.TYPE_GRAY_DBL, 0, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return UnrollDouble1Chan(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((1 << lcms2.FLOAT_SHIFT_VALUE)|(0 << lcms2.BYTES_SHIFT_VALUE), ANYCHANNELS|ANYPLANAR|ANYEXTRA|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return UnrollDoubleTo16(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((1 << lcms2.FLOAT_SHIFT_VALUE)|(4 << lcms2.BYTES_SHIFT_VALUE), ANYCHANNELS|ANYPLANAR|ANYEXTRA|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return UnrollFloatTo16(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll1Byte(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(2 << lcms2.EXTRA_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll1ByteSkip2(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.FLAVOR_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll1ByteReversed(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			new cmsFormatters16((2 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll2Bytes(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((2 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll2ByteSwapFirst(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			new cmsFormatters16(lcms2.TYPE_LabV2_8, 0, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return UnrollLabV2_8(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16(lcms2.TYPE_ALabV2_8, 0, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return UnrollALabV2_8(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16(lcms2.TYPE_LabV2_16, 0, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return UnrollLabV2_16(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll3Bytes(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll3BytesSwap(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll3BytesSkip1Swap(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll3BytesSkip1SwapFirst(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll4Bytes(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.FLAVOR_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll4BytesReverse(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll4BytesSwapFirst(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll4BytesSwap(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll4BytesSwapSwapFirst(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			new cmsFormatters16((1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.PLANAR_SHIFT_VALUE), ANYFLAVOR|ANYSWAP|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return UnrollPlanarBytes(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((1 << lcms2.BYTES_SHIFT_VALUE), ANYFLAVOR|ANYSWAPFIRST|ANYSWAP|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return UnrollChunkyBytes(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll1Word(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.FLAVOR_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll1WordReversed(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(3 << lcms2.EXTRA_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll1WordSkip3(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			new cmsFormatters16((2 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll2Words(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((2 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll2WordSwapFirst(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll3Words(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll4Words(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll3WordsSwap(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll3WordsSkip1SwapFirst(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll3WordsSkip1Swap(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.FLAVOR_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll4WordsReverse(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll4WordsSwapFirst(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll4WordsSwap(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Unroll4WordsSwapSwapFirst(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			
			new cmsFormatters16((2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.PLANAR_SHIFT_VALUE), ANYFLAVOR|ANYSWAP|ANYENDIAN|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return UnrollPlanarWords(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((2 << lcms2.BYTES_SHIFT_VALUE), ANYFLAVOR|ANYSWAPFIRST|ANYSWAP|ANYENDIAN|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return UnrollAnyWords(CMMcargo, Values, Buffer, Stride);
				}
			})
		};
	}
	
	private static final int DEFAULT_FORM_IN_16_COUNT = 40;
	
	private static cmsFormattersFloat[] InputFormattersFloat;
	
	private static void setupInputFormatterFloat()
	{
		InputFormattersFloat = new cmsFormattersFloat[]{
		    //    Type                                          Mask                  Function
		    //  ----------------------------   ------------------------------------  ----------------------------
			new cmsFormattersFloat(lcms2.TYPE_Lab_DBL, ANYPLANAR, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					return UnrollLabDoubleToFloat(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormattersFloat(lcms2.TYPE_Lab_FLT, ANYPLANAR, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					return UnrollLabFloatToFloat(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormattersFloat(lcms2.TYPE_XYZ_DBL, ANYPLANAR, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					return UnrollXYZDoubleToFloat(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormattersFloat(lcms2.TYPE_XYZ_FLT, ANYPLANAR, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					return UnrollXYZFloatToFloat(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			new cmsFormattersFloat((1 << lcms2.FLOAT_SHIFT_VALUE)|(4 << lcms2.BYTES_SHIFT_VALUE), ANYPLANAR|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					return UnrollFloatsToFloat(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormattersFloat((1 << lcms2.FLOAT_SHIFT_VALUE)|(0 << lcms2.BYTES_SHIFT_VALUE), ANYPLANAR|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					return UnrollDoublesToFloat(CMMcargo, Values, Buffer, Stride);
				}
			})
		};
	}
	
	private static final int DEFAULT_FORM_IN_FLOAT_COUNT = 6;
	
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
	
	private static cmsFormatters16[] OutputFormatters16;
	
	private static void setupOutputFormatter16()
	{
		OutputFormatters16 = new cmsFormatters16[]{
			//			    Type                                          Mask                  Function
			//  ----------------------------   ------------------------------------  ----------------------------
				
			new cmsFormatters16(lcms2.TYPE_Lab_DBL, ANYPLANAR, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return PackLabDoubleFrom16(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16(lcms2.TYPE_XYZ_DBL, ANYPLANAR, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return PackXYZDoubleFrom16(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((1 << lcms2.FLOAT_SHIFT_VALUE)|(0 << lcms2.BYTES_SHIFT_VALUE), ANYCHANNELS|ANYPLANAR|ANYEXTRA|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return PackDoubleFrom16(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((1 << lcms2.FLOAT_SHIFT_VALUE)|(4 << lcms2.BYTES_SHIFT_VALUE), ANYCHANNELS|ANYPLANAR|ANYEXTRA|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return PackFloatFrom16(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack1Byte(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack1ByteSkip1(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack1ByteSkip1SwapFirst(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.FLAVOR_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack1ByteReversed(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			new cmsFormatters16(lcms2.TYPE_LabV2_8, 0, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return PackLabV2_8(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16(lcms2.TYPE_ALabV2_8, 0, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return PackALabV2_8(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16(lcms2.TYPE_LabV2_16, 0, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return PackLabV2_16(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.OPTIMIZED_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack3BytesOptimized(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.OPTIMIZED_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack3BytesAndSkip1Optimized(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE)|(1 << lcms2.OPTIMIZED_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack3BytesAndSkip1SwapFirstOptimized(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE)|(1 << lcms2.OPTIMIZED_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack3BytesAndSkip1SwapSwapFirstOptimized(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.OPTIMIZED_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack3BytesAndSkip1SwapOptimized(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE)|(1 << lcms2.OPTIMIZED_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack3BytesSwapOptimized(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			
			
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack3Bytes(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack3BytesAndSkip1(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack3BytesAndSkip1SwapFirst(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack3BytesAndSkip1SwapSwapFirst(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack3BytesAndSkip1Swap(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack3BytesSwap(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((6 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack6Bytes(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((6 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack6BytesSwap(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack4Bytes(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.FLAVOR_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack4BytesReverse(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack4BytesSwapFirst(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack4BytesSwap(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack4BytesSwapSwapFirst(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			new cmsFormatters16((1 << lcms2.BYTES_SHIFT_VALUE), ANYFLAVOR|ANYSWAPFIRST|ANYSWAP|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return PackAnyBytes(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((1 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.PLANAR_SHIFT_VALUE), ANYFLAVOR|ANYSWAP|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return PackPlanarBytes(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack1Word(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack1WordSkip1(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack1WordSkip1SwapFirst(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.FLAVOR_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack1WordReversed(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((1 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.ENDIAN16_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack1WordBigEndian(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack3Words(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack3WordsSwap(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.ENDIAN16_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack3WordsBigEndian(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack3WordsAndSkip1(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack3WordsAndSkip1Swap(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack3WordsAndSkip1SwapFirst(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			new cmsFormatters16((3 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.EXTRA_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE)|(1 << lcms2.SWAPFIRST_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack3WordsAndSkip1SwapSwapFirst(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack4Words(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.FLAVOR_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack4WordsReverse(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack4WordsSwap(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((4 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.ENDIAN16_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack4WordsBigEndian(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			new cmsFormatters16((6 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack6Words(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((6 << lcms2.CHANNELS_SHIFT_VALUE)|(2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.DOSWAP_SHIFT_VALUE), ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return Pack6WordsSwap(CMMcargo, Values, Buffer, Stride);
				}
			}),
			
			new cmsFormatters16((2 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.PLANAR_SHIFT_VALUE), ANYFLAVOR|ANYENDIAN|ANYSWAP|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return PackPlanarWords(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormatters16((2 << lcms2.BYTES_SHIFT_VALUE), ANYFLAVOR|ANYSWAPFIRST|ANYSWAP|ANYENDIAN|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatter16()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride)
				{
					return PackAnyWords(CMMcargo, Values, Buffer, Stride);
				}
			})
		};
	}
	
	private static final int DEFAULT_FORM_OUT_16_COUNT = 52;
	
	private static cmsFormattersFloat[] OutputFormattersFloat;
	
	private static void setupOutputFormatterFloat()
	{
		OutputFormattersFloat = new cmsFormattersFloat[]{
			new cmsFormattersFloat(lcms2.TYPE_Lab_FLT, ANYPLANAR, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					return PackLabFloatFromFloat(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormattersFloat(lcms2.TYPE_XYZ_FLT, ANYPLANAR, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					return PackXYZFloatFromFloat(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormattersFloat(lcms2.TYPE_Lab_DBL, ANYPLANAR, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					return PackLabDoubleFromFloat(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormattersFloat(lcms2.TYPE_XYZ_DBL, ANYPLANAR, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					return PackXYZDoubleFromFloat(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormattersFloat((1 << lcms2.FLOAT_SHIFT_VALUE)|(4 << lcms2.BYTES_SHIFT_VALUE), ANYFLAVOR|ANYSWAPFIRST|ANYSWAP|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					return PackChunkyFloatsFromFloat(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormattersFloat((1 << lcms2.FLOAT_SHIFT_VALUE)|(4 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.PLANAR_SHIFT_VALUE), ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					return PackPlanarFloatsFromFloat(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormattersFloat((1 << lcms2.FLOAT_SHIFT_VALUE)|(0 << lcms2.BYTES_SHIFT_VALUE), ANYFLAVOR|ANYSWAPFIRST|ANYSWAP|ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					return PackChunkyDoublesFromFloat(CMMcargo, Values, Buffer, Stride);
				}
			}),
			new cmsFormattersFloat((1 << lcms2.FLOAT_SHIFT_VALUE)|(0 << lcms2.BYTES_SHIFT_VALUE)|(1 << lcms2.PLANAR_SHIFT_VALUE), ANYEXTRA|ANYCHANNELS|ANYSPACE, new cmsFormatterFloat()
			{
				public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride)
				{
					return PackPlanarDoublesFromFloat(CMMcargo, Values, Buffer, Stride);
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
