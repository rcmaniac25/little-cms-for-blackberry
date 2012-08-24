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

import net.rim.device.api.util.Arrays;
import littlecms.internal.helper.BitConverter;
import littlecms.internal.helper.VirtualPointer;
import littlecms.internal.lcms2.cmsContext;
import littlecms.internal.lcms2.cmsHANDLE;
import littlecms.internal.lcms2.cmsHPROFILE;
import littlecms.internal.lcms2.cmsProfileID;
import littlecms.internal.lcms2_internal._cmsICCPROFILE;

//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmsmd5
{
	public static class _cmsMD5 implements cmsHANDLE
	{
		public int[] buf;
		public int[] bits;
	    public byte[] in;
	    public cmsContext ContextID;
	    
	    public _cmsMD5()
	    {
	    	buf = new int[4];
	    	bits = new int[2];
	    	in = new byte[64];
	    }
	}
	
	private static interface MD5_Proc
	{
		public int run(int x, int y, int z);
	}
	
	private static final MD5_Proc F1 = new MD5_Proc()
	{
		public int run(int x, int y, int z)
		{
			return (z ^ (x & (y ^ z)));
		}
	};
	
	private static final MD5_Proc F2 = new MD5_Proc()
	{
		public int run(int x, int y, int z)
		{
			return F1.run(z, x, y);
		}
	};
	
	private static final MD5_Proc F3 = new MD5_Proc()
	{
		public int run(int x, int y, int z)
		{
			return (x ^ y ^ z);
		}
	};
	
	private static final MD5_Proc F4 = new MD5_Proc()
	{
		public int run(int x, int y, int z)
		{
			return (y ^ (x | ~z));
		}
	};
	
	private static void STEP(MD5_Proc f, int[] w, int[] x, int[] y, int[] z, int data, int s)
	{
		int t = w[0];
		t += (f.run(x[0], y[0], z[0])) + data;
		t = (t<<s) | (t>>>(32-s));
		t += x[0];
		w[0] = t;
	}
	
	private static void MD5_Transform(int[] buf, byte[] in)
	{
		//Slow an inefficient but a easy workaround
		int len = Math.min(in.length / 4, 16);
		int[] inI = new int[16];
		for(int i = 0, v = 0; v < len; i += 4, v++)
		{
			inI[v] = cmsplugin._cmsAdjustEndianess32(BitConverter.toInt32(in, i));
		}
		MD5_Transform(buf, inI);
		for(int i = 0, v = 0; v < len; i += 4, v++)
		{
			System.arraycopy(BitConverter.getBytes(cmsplugin._cmsAdjustEndianess32(inI[v])), 0, in, i, 4);
		}
	}
	
	private static void MD5_Transform(int[] buf, int[] in)
	{
		//Takes in a fixed size arguments, do these checks as a precaution
		lcms2_internal._cmsAssert(buf.length >= 4, "buf.length >= 4");
		lcms2_internal._cmsAssert(in.length >= 16, "in.length >= 16");
		
		int[] a = new int[]{buf[0]};
		int[] b = new int[]{buf[1]};
		int[] c = new int[]{buf[2]};
		int[] d = new int[]{buf[3]};
		
		STEP(F1, a, b, c, d, in[0] + 0xd76aa478, 7);
	    STEP(F1, d, a, b, c, in[1] + 0xe8c7b756, 12);
	    STEP(F1, c, d, a, b, in[2] + 0x242070db, 17);
	    STEP(F1, b, c, d, a, in[3] + 0xc1bdceee, 22);
	    STEP(F1, a, b, c, d, in[4] + 0xf57c0faf, 7);
	    STEP(F1, d, a, b, c, in[5] + 0x4787c62a, 12);
	    STEP(F1, c, d, a, b, in[6] + 0xa8304613, 17);
	    STEP(F1, b, c, d, a, in[7] + 0xfd469501, 22);
	    STEP(F1, a, b, c, d, in[8] + 0x698098d8, 7);
	    STEP(F1, d, a, b, c, in[9] + 0x8b44f7af, 12);
	    STEP(F1, c, d, a, b, in[10] + 0xffff5bb1, 17);
	    STEP(F1, b, c, d, a, in[11] + 0x895cd7be, 22);
	    STEP(F1, a, b, c, d, in[12] + 0x6b901122, 7);
	    STEP(F1, d, a, b, c, in[13] + 0xfd987193, 12);
	    STEP(F1, c, d, a, b, in[14] + 0xa679438e, 17);
	    STEP(F1, b, c, d, a, in[15] + 0x49b40821, 22);
	    
	    STEP(F2, a, b, c, d, in[1] + 0xf61e2562, 5);
	    STEP(F2, d, a, b, c, in[6] + 0xc040b340, 9);
	    STEP(F2, c, d, a, b, in[11] + 0x265e5a51, 14);
	    STEP(F2, b, c, d, a, in[0] + 0xe9b6c7aa, 20);
	    STEP(F2, a, b, c, d, in[5] + 0xd62f105d, 5);
	    STEP(F2, d, a, b, c, in[10] + 0x02441453, 9);
	    STEP(F2, c, d, a, b, in[15] + 0xd8a1e681, 14);
	    STEP(F2, b, c, d, a, in[4] + 0xe7d3fbc8, 20);
	    STEP(F2, a, b, c, d, in[9] + 0x21e1cde6, 5);
	    STEP(F2, d, a, b, c, in[14] + 0xc33707d6, 9);
	    STEP(F2, c, d, a, b, in[3] + 0xf4d50d87, 14);
	    STEP(F2, b, c, d, a, in[8] + 0x455a14ed, 20);
	    STEP(F2, a, b, c, d, in[13] + 0xa9e3e905, 5);
	    STEP(F2, d, a, b, c, in[2] + 0xfcefa3f8, 9);
	    STEP(F2, c, d, a, b, in[7] + 0x676f02d9, 14);
	    STEP(F2, b, c, d, a, in[12] + 0x8d2a4c8a, 20);
	    
	    STEP(F3, a, b, c, d, in[5] + 0xfffa3942, 4);
	    STEP(F3, d, a, b, c, in[8] + 0x8771f681, 11);
	    STEP(F3, c, d, a, b, in[11] + 0x6d9d6122, 16);
	    STEP(F3, b, c, d, a, in[14] + 0xfde5380c, 23);
	    STEP(F3, a, b, c, d, in[1] + 0xa4beea44, 4);
	    STEP(F3, d, a, b, c, in[4] + 0x4bdecfa9, 11);
	    STEP(F3, c, d, a, b, in[7] + 0xf6bb4b60, 16);
	    STEP(F3, b, c, d, a, in[10] + 0xbebfbc70, 23);
	    STEP(F3, a, b, c, d, in[13] + 0x289b7ec6, 4);
	    STEP(F3, d, a, b, c, in[0] + 0xeaa127fa, 11);
	    STEP(F3, c, d, a, b, in[3] + 0xd4ef3085, 16);
	    STEP(F3, b, c, d, a, in[6] + 0x04881d05, 23);
	    STEP(F3, a, b, c, d, in[9] + 0xd9d4d039, 4);
	    STEP(F3, d, a, b, c, in[12] + 0xe6db99e5, 11);
	    STEP(F3, c, d, a, b, in[15] + 0x1fa27cf8, 16);
	    STEP(F3, b, c, d, a, in[2] + 0xc4ac5665, 23);
	    
	    STEP(F4, a, b, c, d, in[0] + 0xf4292244, 6);
	    STEP(F4, d, a, b, c, in[7] + 0x432aff97, 10);
	    STEP(F4, c, d, a, b, in[14] + 0xab9423a7, 15);
	    STEP(F4, b, c, d, a, in[5] + 0xfc93a039, 21);
	    STEP(F4, a, b, c, d, in[12] + 0x655b59c3, 6);
	    STEP(F4, d, a, b, c, in[3] + 0x8f0ccc92, 10);
	    STEP(F4, c, d, a, b, in[10] + 0xffeff47d, 15);
	    STEP(F4, b, c, d, a, in[1] + 0x85845dd1, 21);
	    STEP(F4, a, b, c, d, in[8] + 0x6fa87e4f, 6);
	    STEP(F4, d, a, b, c, in[15] + 0xfe2ce6e0, 10);
	    STEP(F4, c, d, a, b, in[6] + 0xa3014314, 15);
	    STEP(F4, b, c, d, a, in[13] + 0x4e0811a1, 21);
	    STEP(F4, a, b, c, d, in[4] + 0xf7537e82, 6);
	    STEP(F4, d, a, b, c, in[11] + 0xbd3af235, 10);
	    STEP(F4, c, d, a, b, in[2] + 0x2ad7d2bb, 15);
	    STEP(F4, b, c, d, a, in[9] + 0xeb86d391, 21);
	    
	    buf[0] += a[0];
	    buf[1] += b[0];
	    buf[2] += c[0];
	    buf[3] += d[0];
	}
	
	// Create a MD5 object
	private static cmsHANDLE MD5alloc(cmsContext ContextID)
	{
		_cmsMD5 ctx = new _cmsMD5();
		
		ctx.ContextID = ContextID;
		
	    ctx.buf[0] = 0x67452301;
	    ctx.buf[1] = 0xefcdab89;
	    ctx.buf[2] = 0x98badcfe;
	    ctx.buf[3] = 0x10325476;
	    
	    ctx.bits[0] = 0;
	    ctx.bits[1] = 0;
		
		return ctx;
	}
	
	private static void MD5add(cmsHANDLE Handle, VirtualPointer buf, int len)
	{
		_cmsMD5 ctx = (_cmsMD5)Handle;
	    int t;
	    
	    t = ctx.bits[0];
	    if ((ctx.bits[0] = t + (len << 3)) < t)
	    {
	    	ctx.bits[1]++;
	    }
	    
	    ctx.bits[1] += len >> 29;
	    
	    t = (t >> 3) & 0x3f;
	    
	    if (t != 0)
	    {
	        t = 64 - t;
	        if (len < t)
	        {
	        	buf.readRaw(ctx.in, t, len);
	            return;
	        }
	        
	        buf.readRaw(ctx.in, t, t);
	        //byteReverse(ctx.in, 16); byteReverse is not needed on little-endian BlackBerry
	        
	        MD5_Transform(ctx.buf, ctx.in);
	        buf.movePosition(t);
	        len -= t;
	    }
	    
	    while (len >= 64)
	    {
	    	buf.readRaw(ctx.in, 0, 64);
	        //byteReverse(ctx.in, 16); byteReverse is not needed on little-endian BlackBerry
	        MD5_Transform(ctx.buf, ctx.in);
	        buf.movePosition(64);
	        len -= 64;
	    }
	    
	    buf.readRaw(ctx.in, 0, len);
	}
	
	// Destroy the object and return the checksum
	private static void MD5finish(cmsProfileID ProfileID, cmsHANDLE Handle)
	{
		_cmsMD5 ctx = (_cmsMD5)Handle;
	    int count;
	    int p;
	    
	    count = (ctx.bits[0] >> 3) & 0x3F;
	    
	    p = count;
	    ctx.in[p++] = (byte)0x80;
	    
	    count = 64 - 1 - count;
	    
	    if (count < 8)
	    {
	    	Arrays.fill(ctx.in, (byte)0, p, count);
	        //byteReverse(ctx.in, 16); byteReverse is not needed on little-endian BlackBerry
	        MD5_Transform(ctx.buf, ctx.in);
	        
	        Arrays.fill(ctx.in, (byte)0, 0, 56);
	    }
	    else
	    {
	    	Arrays.fill(ctx.in, (byte)0, p, count - 8);
	    }
	    //byteReverse(ctx.in, 14); byteReverse is not needed on little-endian BlackBerry
	    
	    System.arraycopy(BitConverter.getBytes(cmsplugin._cmsAdjustEndianess32(ctx.bits[0])), 0, ctx.in, 14 * 4, 4);
	    System.arraycopy(BitConverter.getBytes(cmsplugin._cmsAdjustEndianess32(ctx.bits[1])), 0, ctx.in, 15 * 4, 4);
	    
	    MD5_Transform(ctx.buf, ctx.in);
	    
	    //byteReverse(ctx.buf, 4); byteReverse is not needed on little-endian BlackBerry
	    ProfileID.setID32(ctx.buf); //Originally done with ID8 but not using pointers
	}
	
	// Assuming io points to an ICC profile, compute and store MD5 checksum
	// In the header, rendering intentent, attributes and ID should be set to zero
	// before computing MD5 checksum (per 6.1.13 in ICC spec)
	
	public static boolean cmsMD5computeID(cmsHPROFILE hProfile)
	{
		cmsContext ContextID;
	    int[] BytesNeeded = new int[1];
	    VirtualPointer Mem = null;
	    cmsHANDLE MD5 = null;
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    _cmsICCPROFILE Keep;
	    
		lcms2_internal._cmsAssert(hProfile != null, "hProfile != null");
		
	    ContextID = cmsio0.cmsGetProfileContextID(hProfile);
	    
	    // Save a copy of the profile header
	    VirtualPointer vp = new VirtualPointer(Icc);
	    Keep = (_cmsICCPROFILE)vp.getProcessor().readObject(_cmsICCPROFILE.class);
	    //Copy over the components that weren't copied
	    Keep.IOhandler = Icc.IOhandler;
	    Keep.ContextID = Icc.ContextID;
	    System.arraycopy(Icc.TagPtrs, 0, Keep.TagPtrs, 0, lcms2_internal.MAX_TABLE_TAG);
	    System.arraycopy(Icc.TagTypeHandlers, 0, Keep.TagTypeHandlers, 0, lcms2_internal.MAX_TABLE_TAG);
	    
	    // Set RI, attributes and ID
	    Icc.attributes = 0;
	    Icc.RenderingIntent = 0;
	    Arrays.zero(Icc.ProfileID.data);
	    
	    // Compute needed storage
	    if (!cmsio0.cmsSaveProfileToMem(hProfile, null, BytesNeeded))
	    {
	    	// Free resources as something went wrong
		    vp.getProcessor().readObject(_cmsICCPROFILE.class, false, Icc);
        	//Copy over the components that weren't copied
    	    Icc.IOhandler = Keep.IOhandler;
    	    Icc.ContextID = Keep.ContextID;
    	    System.arraycopy(Keep.TagPtrs, 0, Icc.TagPtrs, 0, lcms2_internal.MAX_TABLE_TAG);
    	    System.arraycopy(Keep.TagTypeHandlers, 0, Icc.TagTypeHandlers, 0, lcms2_internal.MAX_TABLE_TAG);
		    return false;
	    }
	    
	    // Allocate memory
	    Mem = cmserr._cmsMalloc(ContextID, BytesNeeded[0]);
	    if (Mem == null)
	    {
	    	// Free resources as something went wrong
		    vp.getProcessor().readObject(_cmsICCPROFILE.class, false, Icc);
        	//Copy over the components that weren't copied
    	    Icc.IOhandler = Keep.IOhandler;
    	    Icc.ContextID = Keep.ContextID;
    	    System.arraycopy(Keep.TagPtrs, 0, Icc.TagPtrs, 0, lcms2_internal.MAX_TABLE_TAG);
    	    System.arraycopy(Keep.TagTypeHandlers, 0, Icc.TagTypeHandlers, 0, lcms2_internal.MAX_TABLE_TAG);
		    return false;
	    }
	    
	    // Save to temporary storage
	    byte[] temp = new byte[BytesNeeded[0]];
	    if (!cmsio0.cmsSaveProfileToMem(hProfile, temp, BytesNeeded))
	    {
	    	// Free resources as something went wrong
		    if (Mem != null)
		    {
		    	cmserr._cmsFree(ContextID, Mem);
		    }
		    vp.getProcessor().readObject(_cmsICCPROFILE.class, false, Icc);
        	//Copy over the components that weren't copied
    	    Icc.IOhandler = Keep.IOhandler;
    	    Icc.ContextID = Keep.ContextID;
    	    System.arraycopy(Keep.TagPtrs, 0, Icc.TagPtrs, 0, lcms2_internal.MAX_TABLE_TAG);
    	    System.arraycopy(Keep.TagTypeHandlers, 0, Icc.TagTypeHandlers, 0, lcms2_internal.MAX_TABLE_TAG);
		    return false;
	    }
	    Mem.writeRaw(temp, 0, BytesNeeded[0]);
	    
	    // Create MD5 object
	    MD5 = MD5alloc(ContextID);
	    if (MD5 == null)
	    {
	    	// Free resources as something went wrong
		    if (Mem != null)
		    {
		    	cmserr._cmsFree(ContextID, Mem);
		    }
		    vp.getProcessor().readObject(_cmsICCPROFILE.class, false, Icc);
        	//Copy over the components that weren't copied
    	    Icc.IOhandler = Keep.IOhandler;
    	    Icc.ContextID = Keep.ContextID;
    	    System.arraycopy(Keep.TagPtrs, 0, Icc.TagPtrs, 0, lcms2_internal.MAX_TABLE_TAG);
    	    System.arraycopy(Keep.TagTypeHandlers, 0, Icc.TagTypeHandlers, 0, lcms2_internal.MAX_TABLE_TAG);
		    return false;
	    }
	    
	    // Add all bytes
	    MD5add(MD5, Mem, BytesNeeded[0]);
	    
	    // Temp storage is no longer needed
	    cmserr._cmsFree(ContextID, Mem);
	    
	    // Restore header
	    vp.getProcessor().readObject(_cmsICCPROFILE.class, false, Icc);
	    //Copy over the components that weren't copied
	    Icc.IOhandler = Keep.IOhandler;
	    Icc.ContextID = Keep.ContextID;
	    System.arraycopy(Keep.TagPtrs, 0, Icc.TagPtrs, 0, lcms2_internal.MAX_TABLE_TAG);
	    System.arraycopy(Keep.TagTypeHandlers, 0, Icc.TagTypeHandlers, 0, lcms2_internal.MAX_TABLE_TAG);
	    
	    // And store the ID
	    MD5finish(Icc.ProfileID, MD5);
	    return true;
	}
}
