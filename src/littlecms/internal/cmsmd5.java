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
			return F1.run(x, y, z);
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
	
	private static void STEP(MD5_Proc f, int[] w, int[] x, int[] y, int z[], long data, int s)
	{
		long t = w[0];
		t += (f.run(x[0], y[0], z[0]) & 0xFFFFFFFFL) + data;
		t = (t<<s) | (t>>(32-s));
		t += x[0];
		w[0] = (int)t;
	}
	
	private static void MD5_Transform(int[] buf, byte[] in)
	{
		//Slow an inefficient but a easy workaround
		int len = Math.min(in.length / 4, 4);
		int[] inI = new int[len];
		for(int i = 0, v = 0; v < len; i += 4, v++)
		{
			inI[v] = BitConverter.toInt32(in, i);
		}
		MD5_Transform(buf, inI);
		byte[] temp;
		for(int i = 0, v = 0; v < len; i += 4, v++)
		{
			temp = BitConverter.getBytes(inI[v]);
			System.arraycopy(temp, 0, in, i, 4);
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
		
		long[] inV = new long[16];
		for(int i = 0; i < 16; i++)
		{
			inV[i] = in[i] & 0xFFFFFFFFL;
		}
		
		STEP(F1, a, b, c, d, inV[0] + 0xd76aa478L, 7);
	    STEP(F1, d, a, b, c, inV[1] + 0xe8c7b756L, 12);
	    STEP(F1, c, d, a, b, inV[2] + 0x242070dbL, 17);
	    STEP(F1, b, c, d, a, inV[3] + 0xc1bdceeeL, 22);
	    STEP(F1, a, b, c, d, inV[4] + 0xf57c0fafL, 7);
	    STEP(F1, d, a, b, c, inV[5] + 0x4787c62aL, 12);
	    STEP(F1, c, d, a, b, inV[6] + 0xa8304613L, 17);
	    STEP(F1, b, c, d, a, inV[7] + 0xfd469501L, 22);
	    STEP(F1, a, b, c, d, inV[8] + 0x698098d8L, 7);
	    STEP(F1, d, a, b, c, inV[9] + 0x8b44f7afL, 12);
	    STEP(F1, c, d, a, b, inV[10] + 0xffff5bb1L, 17);
	    STEP(F1, b, c, d, a, inV[11] + 0x895cd7beL, 22);
	    STEP(F1, a, b, c, d, inV[12] + 0x6b901122L, 7);
	    STEP(F1, d, a, b, c, inV[13] + 0xfd987193L, 12);
	    STEP(F1, c, d, a, b, inV[14] + 0xa679438eL, 17);
	    STEP(F1, b, c, d, a, inV[15] + 0x49b40821L, 22);
	    
	    STEP(F2, a, b, c, d, inV[1] + 0xf61e2562L, 5);
	    STEP(F2, d, a, b, c, inV[6] + 0xc040b340L, 9);
	    STEP(F2, c, d, a, b, inV[11] + 0x265e5a51L, 14);
	    STEP(F2, b, c, d, a, inV[0] + 0xe9b6c7aaL, 20);
	    STEP(F2, a, b, c, d, inV[5] + 0xd62f105dL, 5);
	    STEP(F2, d, a, b, c, inV[10] + 0x02441453L, 9);
	    STEP(F2, c, d, a, b, inV[15] + 0xd8a1e681L, 14);
	    STEP(F2, b, c, d, a, inV[4] + 0xe7d3fbc8L, 20);
	    STEP(F2, a, b, c, d, inV[9] + 0x21e1cde6L, 5);
	    STEP(F2, d, a, b, c, inV[14] + 0xc33707d6L, 9);
	    STEP(F2, c, d, a, b, inV[3] + 0xf4d50d87L, 14);
	    STEP(F2, b, c, d, a, inV[8] + 0x455a14edL, 20);
	    STEP(F2, a, b, c, d, inV[13] + 0xa9e3e905L, 5);
	    STEP(F2, d, a, b, c, inV[2] + 0xfcefa3f8L, 9);
	    STEP(F2, c, d, a, b, inV[7] + 0x676f02d9L, 14);
	    STEP(F2, b, c, d, a, inV[12] + 0x8d2a4c8aL, 20);
	    
	    STEP(F3, a, b, c, d, inV[5] + 0xfffa3942L, 4);
	    STEP(F3, d, a, b, c, inV[8] + 0x8771f681L, 11);
	    STEP(F3, c, d, a, b, inV[11] + 0x6d9d6122L, 16);
	    STEP(F3, b, c, d, a, inV[14] + 0xfde5380cL, 23);
	    STEP(F3, a, b, c, d, inV[1] + 0xa4beea44L, 4);
	    STEP(F3, d, a, b, c, inV[4] + 0x4bdecfa9L, 11);
	    STEP(F3, c, d, a, b, inV[7] + 0xf6bb4b60L, 16);
	    STEP(F3, b, c, d, a, inV[10] + 0xbebfbc70L, 23);
	    STEP(F3, a, b, c, d, inV[13] + 0x289b7ec6L, 4);
	    STEP(F3, d, a, b, c, inV[0] + 0xeaa127faL, 11);
	    STEP(F3, c, d, a, b, inV[3] + 0xd4ef3085L, 16);
	    STEP(F3, b, c, d, a, inV[6] + 0x04881d05L, 23);
	    STEP(F3, a, b, c, d, inV[9] + 0xd9d4d039L, 4);
	    STEP(F3, d, a, b, c, inV[12] + 0xe6db99e5L, 11);
	    STEP(F3, c, d, a, b, inV[15] + 0x1fa27cf8L, 16);
	    STEP(F3, b, c, d, a, inV[2] + 0xc4ac5665L, 23);
	    
	    STEP(F4, a, b, c, d, inV[0] + 0xf4292244L, 6);
	    STEP(F4, d, a, b, c, inV[7] + 0x432aff97L, 10);
	    STEP(F4, c, d, a, b, inV[14] + 0xab9423a7L, 15);
	    STEP(F4, b, c, d, a, inV[5] + 0xfc93a039L, 21);
	    STEP(F4, a, b, c, d, inV[12] + 0x655b59c3L, 6);
	    STEP(F4, d, a, b, c, inV[3] + 0x8f0ccc92L, 10);
	    STEP(F4, c, d, a, b, inV[10] + 0xffeff47dL, 15);
	    STEP(F4, b, c, d, a, inV[1] + 0x85845dd1L, 21);
	    STEP(F4, a, b, c, d, inV[8] + 0x6fa87e4fL, 6);
	    STEP(F4, d, a, b, c, inV[15] + 0xfe2ce6e0L, 10);
	    STEP(F4, c, d, a, b, inV[6] + 0xa3014314L, 15);
	    STEP(F4, b, c, d, a, inV[13] + 0x4e0811a1L, 21);
	    STEP(F4, a, b, c, d, inV[4] + 0xf7537e82L, 6);
	    STEP(F4, d, a, b, c, inV[11] + 0xbd3af235L, 10);
	    STEP(F4, c, d, a, b, inV[2] + 0x2ad7d2bbL, 15);
	    STEP(F4, b, c, d, a, inV[9] + 0xeb86d391L, 21);
	    
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
	    	int t = p + count;
	    	for(int i = p; i < t; i++)
	    	{
	    		ctx.in[i] = 0;
	    	}
	        //byteReverse(ctx.in, 16); byteReverse is not needed on little-endian BlackBerry
	        MD5_Transform(ctx.buf, ctx.in);
	        
	        for(int i = 0; i < 56; i++)
	    	{
	    		ctx.in[i] = 0;
	    	}
	    }
	    else
	    {
	    	int t = p + (count - 8);
	    	for(int i = p; i < t; i++)
	    	{
	    		ctx.in[i] = 0;
	    	}
	    }
	    //byteReverse(ctx.in, 14); byteReverse is not needed on little-endian BlackBerry
	    
	    byte[] temp = BitConverter.getBytes(ctx.bits[0]);
	    System.arraycopy(temp, 0, ctx.in, 14 * 4, 4);
	    temp = BitConverter.getBytes(ctx.bits[1]);
	    System.arraycopy(temp, 0, ctx.in, 15 * 4, 4);
	    
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
	    VirtualPointer Keep;
	    
		lcms2_internal._cmsAssert(hProfile != null, "hProfile != null");
		
	    ContextID = cmsio0.cmsGetProfileContextID(hProfile);
	    
	    // Save a copy of the profile header
	    Keep = new VirtualPointer(Icc);
	    
	    // Set RI, attributes and ID
	    Icc.attributes = 0;
	    Icc.RenderingIntent = 0;
	    Icc.ProfileID.setID8(new byte[cmsProfileID.SIZE]);
	    
	    // Compute needed storage
	    if (!cmsio0.cmsSaveProfileToMem(hProfile, null, BytesNeeded))
	    {
	    	// Free resources as something went wrong
		    if (Mem != null)
		    {
		    	cmserr._cmsFree(ContextID, Mem);
		    }
		    Keep.getProcessor().readObject(_cmsICCPROFILE.class, false, Icc);
		    return false;
	    }
	    
	    // Allocate memory
	    Mem = cmserr._cmsMalloc(ContextID, BytesNeeded[0]);
	    if (Mem == null)
	    {
	    	// Free resources as something went wrong
		    if (Mem != null)
		    {
		    	cmserr._cmsFree(ContextID, Mem);
		    }
		    Keep.getProcessor().readObject(_cmsICCPROFILE.class, false, Icc);
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
		    Keep.getProcessor().readObject(_cmsICCPROFILE.class, false, Icc);
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
		    Keep.getProcessor().readObject(_cmsICCPROFILE.class, false, Icc);
		    return false;
	    }
	    
	    // Add all bytes
	    MD5add(MD5, Mem, BytesNeeded[0]);
	    
	    // Temp storage is no longer needed
	    cmserr._cmsFree(ContextID, Mem);
	    
	    // Restore header
	    Keep.getProcessor().readObject(_cmsICCPROFILE.class, false, Icc);
	    
	    // And store the ID
	    MD5finish(Icc.ProfileID,  MD5);
	    return true;
	}
}
