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

import java.util.Calendar;

import littlecms.internal.helper.BitConverter;
import littlecms.internal.helper.Utility;
import littlecms.internal.helper.VirtualPointer;
import littlecms.internal.lcms2.cmsCIEXYZ;
import littlecms.internal.lcms2.cmsDateTimeNumber;
import littlecms.internal.lcms2.cmsEncodedXYZNumber;
import littlecms.internal.lcms2.cmsIOHANDLER;
import littlecms.internal.lcms2_internal._cmsSubAllocator;
import littlecms.internal.lcms2_plugin._cmsTagBase;
import littlecms.internal.lcms2_plugin.cmsPluginBase;

//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmsplugin
{
	// ----------------------------------------------------------------------------------
	// Encoding & Decoding support functions
	// ----------------------------------------------------------------------------------
	
	// Little-Endian to Big-Endian
	
	// Adjust a word value after being readed/ before being written from/to an ICC profile
	public static short _cmsAdjustEndianess16(short Word)
	{
//#ifndef CMS_USE_BIG_ENDIAN
		
		byte[] pByte = BitConverter.getBytes(Word);
	    byte tmp;
	    
	    tmp = pByte[0];
	    pByte[0] = pByte[1];
	    pByte[1] = tmp;
	    
	    Word = BitConverter.toInt16(pByte, 0);
//#endif
	    
	    return Word;
	}
	
	// Transports to properly encoded values - note that icc profiles does use big endian notation.
	
	// 1 2 3 4
	// 4 3 2 1
	
	public static int _cmsAdjustEndianess32(int DWord)
	{
//#ifndef CMS_USE_BIG_ENDIAN
		
		byte[] pByte = BitConverter.getBytes(DWord);
		int pByteIndex = 0;
	    byte temp1;
	    byte temp2;
	    
	    temp1 = pByte[pByteIndex++];
	    temp2 = pByte[pByteIndex++];
	    pByte[pByteIndex - 1] = pByte[pByteIndex];
	    pByte[pByteIndex++] = temp2;
	    pByte[pByteIndex - 3] = pByte[pByteIndex];
	    pByte[pByteIndex] = temp1;
	    
	    DWord = BitConverter.toInt32(pByte, 0);
//#endif
	    return DWord;
	}
	
	// 1 2 3 4 5 6 7 8
	// 8 7 6 5 4 3 2 1
	
	public static void _cmsAdjustEndianess64(long[] Result, long QWord)
	{
//#ifndef CMS_USE_BIG_ENDIAN
	    
		byte[] pIn  = BitConverter.getBytes(QWord);
	    byte[] pOut = new byte[8];
	    
	    lcms2_internal._cmsAssert(Result != null, "Result != null");
	    
	    pOut[7] = pIn[0];
	    pOut[6] = pIn[1];
	    pOut[5] = pIn[2];
	    pOut[4] = pIn[3];
	    pOut[3] = pIn[4];
	    pOut[2] = pIn[5];
	    pOut[1] = pIn[6];
	    pOut[0] = pIn[7];
	    
	    Result[0] = BitConverter.toInt64(pOut, 0);
	    
//#else
	    
	    lcms2_internal._cmsAssert(Result != null, "Result != null");
	    
	    Result[0] = QWord;
//#endif
	}
	
	// Auxiliar -- read 8, 16 and 32-bit numbers
	public static boolean _cmsReadUInt8Number(cmsIOHANDLER io, byte[] n)
	{
	    byte[] tmp = new byte[1];
	    
	    lcms2_internal._cmsAssert(io != null, "io != null");

	    if (io.Read.run(io, tmp, /*sizeof(cmsUInt8Number)*/1, 1) != 1)
	    {
	    	return false;   
	    }
	    
	    if (n != null)
	    {
	    	n[0] = tmp[0];
	    }
	    return true;
	}
	
	public static boolean _cmsReadUInt16Number(cmsIOHANDLER io, short[] n)
	{
	    byte[] tmp = new byte[2];
	    
	    lcms2_internal._cmsAssert(io != null, "io != null");
	    
	    if (io.Read.run(io, tmp, /*sizeof(cmsUInt16Number)*/2, 1) != 1)
	    {
	    	return false;   
	    }
	    
	    if (n != null)
	    {
	    	n[0] = _cmsAdjustEndianess16(BitConverter.toInt16(tmp, 0));
	    }
	    return true;
	}
	
	public static boolean _cmsReadUInt16Array(cmsIOHANDLER io, int n, short[] Array)
	{
	    int i;
	    
	    lcms2_internal._cmsAssert(io != null, "io != null");
	    
	    short[] temp = null;
	    for (i = 0; i < n; i++)
	    {
	        if (Array != null)
	        {
	        	if(temp == null)
	        	{
	        		temp = new short[1];
	        	}
	            if (!_cmsReadUInt16Number(io, temp))
	            {
	            	return false;
	            }
	            Array[i] = temp[0];
	        }
	        else
	        {
	            if (!_cmsReadUInt16Number(io, null))
	            {
	            	return false;
	            }
	        }
	    }
	    return true;
	}
	
	//Little helper function in case a VirtualPointer is used.
	public static boolean _cmsReadUInt16Array(cmsIOHANDLER io, int n, VirtualPointer vp, boolean reverseEndian)
	{
	    int i;
	    
	    lcms2_internal._cmsAssert(io != null, "io != null");
	    
	    int pos = 0;
	    VirtualPointer.TypeProcessor proc = null;
	    if(vp != null)
	    {
	    	pos = vp.getPosition();
	    	proc = vp.getProcessor();
	    }
	    short[] temp = null;
	    for (i = 0; i < n; i++)
	    {
	        if (vp != null)
	        {
	        	if(temp == null)
	        	{
	        		temp = new short[1];
	        	}
	            if (!_cmsReadUInt16Number(io, temp))
	            {
	            	if(vp != null)
	        	    {
	        	    	vp.setPosition(pos);
	        	    }
	            	return false;
	            }
	            proc.write(reverseEndian ? _cmsAdjustEndianess16(temp[0]) : temp[0], true);
	        }
	        else
	        {
	            if (!_cmsReadUInt16Number(io, null))
	            {
	            	return false;
	            }
	        }
	    }
	    if(vp != null)
	    {
	    	vp.setPosition(pos);
	    }
	    return true;
	}
	
	public static boolean _cmsReadUInt32Number(cmsIOHANDLER io, int[] n)
	{
	    byte[] tmp = new byte[4];
	    
	    lcms2_internal._cmsAssert(io != null, "io != null");
	    
	    if (io.Read.run(io, tmp, /*sizeof(cmsUInt32Number)*/4, 1) != 1)
	    {
	    	return false;   
	    }
	    
	    if (n != null)
	    {
	    	n[0] = _cmsAdjustEndianess32(BitConverter.toInt32(tmp, 0));
	    }
	    return true;
	}
	
	public static boolean _cmsReadFloat32Number(cmsIOHANDLER io, float[] n)
	{
	    byte[] tmp = new byte[4];
	    
	    lcms2_internal._cmsAssert(io != null, "io != null");
	    
	    if (io.Read.run(io, tmp, /*sizeof(cmsFloat32Number)*/4, 1) != 1)
	    {
	    	return false;   
	    }
	    
	    if (n != null)
	    {
	    	n[0] = Float.intBitsToFloat(_cmsAdjustEndianess32(BitConverter.toInt32(tmp, 0)));
	    }
	    return true;
	}
	
	public static boolean _cmsReadUInt64Number(cmsIOHANDLER io, long[] n)
	{
	    byte[] tmp = new byte[8];
	    
	    lcms2_internal._cmsAssert(io != null, "io != null");
	    
	    if (io.Read.run(io, tmp, /*sizeof(cmsUInt64Number)*/8, 1) != 1)
	    {
	    	return false;   
	    }
	    
	    if (n != null)
	    {
	    	_cmsAdjustEndianess64(n, BitConverter.toInt64(tmp, 0));
	    }
	    return true;
	}
	
	public static boolean _cmsRead15Fixed16Number(cmsIOHANDLER io, double[] n)
	{
		return _cmsRead15Fixed16Number(io, n, true);
	}
	
	public static boolean _cmsRead15Fixed16Number(cmsIOHANDLER io, double[] n, boolean reverseEndian)
	{
		byte[] tmp = new byte[4];
	    
	    lcms2_internal._cmsAssert(io != null, "io != null");
	    
	    if (io.Read.run(io, tmp, /*sizeof(cmsUInt32Number)*/4, 1) != 1)
	    {
	    	return false;   
	    }
	    
	    if (n != null)
	    {
	    	int t = BitConverter.toInt32(tmp, 0);
	    	if(reverseEndian)
	    	{
	    		t = _cmsAdjustEndianess32(t);
	    	}
	        n[0] = _cms15Fixed16toDouble(t);
	    }
	    
	    return true;
	}
	
	// Jun-21-2000: Some profiles (those that comes with W2K) comes
	// with the media white (media black?) x 100. Add a sanity check
	
	private static void NormalizeXYZ(cmsCIEXYZ Dest)
	{
	    while (Dest.X > 2.0 && Dest.Y > 2.0 && Dest.Z > 2.0)
	    {
	    	Dest.X /= 10.0;
	    	Dest.Y /= 10.0;
	    	Dest.Z /= 10.0;
	    }
	}
	
	public static boolean _cmsReadXYZNumber(cmsIOHANDLER io, cmsCIEXYZ XYZ)
	{
	    cmsEncodedXYZNumber xyz;
	    VirtualPointer vp = new VirtualPointer(cmsEncodedXYZNumber.SIZE);
	    
	    lcms2_internal._cmsAssert(io != null, "io != null");
	    
	    if (io.vpRead(io, vp, /*sizeof(cmsEncodedXYZNumber)*/cmsEncodedXYZNumber.SIZE, 1) != 1)
	    {
	    	return false;
	    }
	    
	    if (XYZ != null)
	    {
	    	xyz = (cmsEncodedXYZNumber)vp.getProcessor().readObject(cmsEncodedXYZNumber.class);
	    	
	        XYZ.X = _cms15Fixed16toDouble(_cmsAdjustEndianess32(xyz.X));
	        XYZ.Y = _cms15Fixed16toDouble(_cmsAdjustEndianess32(xyz.Y));
	        XYZ.Z = _cms15Fixed16toDouble(_cmsAdjustEndianess32(xyz.Z));
	        
	        NormalizeXYZ(XYZ);
	    }
	    return true;
	}
	
	public static boolean _cmsWriteUInt8Number(cmsIOHANDLER io, byte n)
	{
		lcms2_internal._cmsAssert(io != null, "io != null");
		
	    if (!io.Write.run(io, /*sizeof(cmsUInt8Number)*/1, new byte[]{n}))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	public static boolean _cmsWriteUInt16Number(cmsIOHANDLER io, short n)
	{
	    byte[] tmp;
	    
	    lcms2_internal._cmsAssert(io != null, "io != null");
	    
	    tmp = BitConverter.getBytes(_cmsAdjustEndianess16(n));
	    if (!io.Write.run(io, /*sizeof(cmsUInt16Number)*/2, tmp))
	    {
	    	return false;   
	    }
	    
	    return true;
	}
	
	public static boolean _cmsWriteUInt16Array(cmsIOHANDLER io, int n, final short[] Array)
	{
	    int i;
	    
	    lcms2_internal._cmsAssert(io != null, "io != null");
	    lcms2_internal._cmsAssert(Array != null, "Array != null");
	    
	    for (i = 0; i < n; i++)
	    {
	        if (!_cmsWriteUInt16Number(io, Array[i]))
	        {
	        	return false;
	        }
	    }
	    
	    return true;
	}
	
	//Little helper function in case a VirtualPointer is used.
	public static boolean _cmsWriteUInt16Array(cmsIOHANDLER io, int n, final VirtualPointer vp, boolean reverseEndian)
	{
	    int i;
	    short t;
	    
	    lcms2_internal._cmsAssert(io != null, "io != null");
	    lcms2_internal._cmsAssert(vp != null, "vp != null");
	    
	    VirtualPointer.TypeProcessor proc = vp.getProcessor();
	    int pos = vp.getPosition();
	    for (i = 0; i < n; i++)
	    {
	    	t = proc.readInt16(true);
	        if (!_cmsWriteUInt16Number(io, reverseEndian ? _cmsAdjustEndianess16(t) : t))
	        {
	        	vp.setPosition(pos);
	        	return false;
	        }
	    }
	    vp.setPosition(pos);
	    
	    return true;
	}
	
	public static boolean _cmsWriteUInt32Number(cmsIOHANDLER io, int n)
	{
	    byte[] tmp;
	    
	    lcms2_internal._cmsAssert(io != null, "io != null");
	    
	    tmp = BitConverter.getBytes(_cmsAdjustEndianess32(n));
	    if (!io.Write.run(io, /*sizeof(cmsUInt32Number)*/4, tmp))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	public static boolean _cmsWriteFloat32Number(cmsIOHANDLER io, float n)
	{
	    byte[] tmp;
	    
	    lcms2_internal._cmsAssert(io != null, "io != null");
	    
	    tmp = BitConverter.getBytes(_cmsAdjustEndianess32(Float.floatToIntBits(n)));
	    if (!io.Write.run(io, /*sizeof(cmsUInt32Number)*/4, tmp))
	    {
	    	return false;   
	    }
	    
	    return true;
	}
	
	public static boolean _cmsWriteUInt64Number(cmsIOHANDLER io, long n)
	{
	    byte[] tmp;
	    
	    lcms2_internal._cmsAssert(io != null, "io != null");
	    long[] endianTemp = new long[1];
	    
	    _cmsAdjustEndianess64(endianTemp, n);
	    tmp = BitConverter.getBytes(endianTemp[0]);
	    if (!io.Write.run(io, /*sizeof(cmsUInt64Number)*/8, tmp))
	    {
	    	return false;   
	    }
	    
	    return true;
	}
	
	public static boolean _cmsWrite15Fixed16Number(cmsIOHANDLER io, double n)
	{
		return _cmsWrite15Fixed16Number(io, n, false);
	}
	
	public static boolean _cmsWrite15Fixed16Number(cmsIOHANDLER io, double n, boolean reverseEndian)
	{
	    byte[] tmp;
	    
	    lcms2_internal._cmsAssert(io != null, "io != null");
	    
	    int t = _cmsDoubleTo15Fixed16(n);
	    if(reverseEndian)
	    {
	    	t = _cmsAdjustEndianess32(t);
	    }
	    tmp = BitConverter.getBytes(t);
	    if (!io.Write.run(io, /*sizeof(cmsUInt32Number)*/4, tmp))
	    {
	    	return false;   
	    }
	    
	    return true;
	}
	
	public static boolean _cmsWriteXYZNumber(cmsIOHANDLER io, final cmsCIEXYZ XYZ)
	{
	    cmsEncodedXYZNumber xyz = new cmsEncodedXYZNumber();
	    
	    lcms2_internal._cmsAssert(io != null, "io != null");
	    lcms2_internal._cmsAssert(XYZ != null, "XYZ != null");
	    
	    xyz.X = _cmsAdjustEndianess32(_cmsDoubleTo15Fixed16(XYZ.X));
	    xyz.Y = _cmsAdjustEndianess32(_cmsDoubleTo15Fixed16(XYZ.Y));
	    xyz.Z = _cmsAdjustEndianess32(_cmsDoubleTo15Fixed16(XYZ.Z));
	    
	    return io.vpWrite(io, /*sizeof(cmsEncodedXYZNumber)*/cmsEncodedXYZNumber.SIZE, new VirtualPointer(xyz));
	}
	
	// from Fixed point 8.8 to double
	public static double _cms8Fixed8toDouble(short fixed8)
	{
		byte msb, lsb;
		
		lsb = (byte)(fixed8 & 0xff);
		msb = (byte)((fixed8 >> 8) & 0xff);
		
		return (msb + (lsb / 256.0));
	}
	
	public static short _cmsDoubleTo8Fixed8(double val)
	{
	    int GammaFixed32 = _cmsDoubleTo15Fixed16(val);
	    return  (short)((GammaFixed32 >> 8) & 0xFFFF);       
	}
	
	// from Fixed point 15.16 to double
	public static double _cms15Fixed16toDouble(int fix32)
	{
	    double floater, sign, mid;
	    int Whole, FracPart;
	    
	    sign  = (fix32 < 0 ? -1 : 1);
	    fix32 = Math.abs(fix32);
	    
	    Whole     = (fix32 >> 16) & 0xffff;
	    FracPart  = (fix32 & 0xffff);
	    
	    mid     = FracPart / 65536.0;
	    floater = Whole + mid;
	    
	    return sign * floater;
	}
	
	// from double to Fixed point 15.16 
	public static int _cmsDoubleTo15Fixed16(double v)
	{
	    return ((int)Math.floor((v) * 65536.0 + 0.5));
	}
	
	// Date/Time functions
	
	public static void _cmsDecodeDateTimeNumber(final cmsDateTimeNumber Source, Calendar Dest)
	{
	    lcms2_internal._cmsAssert(Dest != null, "Dest != null");
	    lcms2_internal._cmsAssert(Source != null, "Source != null");
	    
	    Dest.set(Calendar.SECOND, _cmsAdjustEndianess16(Source.seconds));
	    Dest.set(Calendar.MINUTE, _cmsAdjustEndianess16(Source.minutes));
	    Dest.set(Calendar.HOUR_OF_DAY, _cmsAdjustEndianess16(Source.hours));
	    Dest.set(Calendar.DAY_OF_MONTH, _cmsAdjustEndianess16(Source.day));
	    Dest.set(Calendar.MONTH, _cmsAdjustEndianess16(Source.month) - 1);
	    Dest.set(Calendar.YEAR, _cmsAdjustEndianess16(Source.year));
	}
	
	public static void _cmsEncodeDateTimeNumber(cmsDateTimeNumber Dest, final Calendar Source)
	{
		lcms2_internal._cmsAssert(Dest != null, "Dest != null");
		lcms2_internal._cmsAssert(Source != null, "Source != null");
	    
	    Dest.seconds = _cmsAdjustEndianess16((short)Source.get(Calendar.SECOND));
	    Dest.minutes = _cmsAdjustEndianess16((short)Source.get(Calendar.MINUTE));
	    Dest.hours   = _cmsAdjustEndianess16((short)Source.get(Calendar.HOUR_OF_DAY));
	    Dest.day     = _cmsAdjustEndianess16((short)Source.get(Calendar.DAY_OF_MONTH));
	    Dest.month   = _cmsAdjustEndianess16((short)(Source.get(Calendar.MONTH) + 1));
	    Dest.year    = _cmsAdjustEndianess16((short)(Source.get(Calendar.YEAR)));
	}
	
	// Read base and return type base
	public static int _cmsReadTypeBase(cmsIOHANDLER io)
	{
		VirtualPointer Base = new VirtualPointer(_cmsTagBase.SIZE);
	    
		lcms2_internal._cmsAssert(io != null, "io != null");
		
	    if (io.vpRead(io, Base, /*sizeof(_cmsTagBase)*/_cmsTagBase.SIZE, 1) != 1)
	    {
	    	return 0;
	    }
	    
	    return _cmsAdjustEndianess32(Base.getProcessor().readInt32()); //Equivalent of Base.sig
	}
	
	// Setup base marker
	public static boolean _cmsWriteTypeBase(cmsIOHANDLER io, int sig)
	{
		VirtualPointer Base = new VirtualPointer(_cmsTagBase.SIZE);
		
	    lcms2_internal._cmsAssert(io != null, "io != null");
	    
	    Base.getProcessor().write(_cmsAdjustEndianess32(sig));
	    return io.vpWrite(io, /*sizeof(_cmsTagBase)*/_cmsTagBase.SIZE, Base);
	}
	
	public static boolean _cmsReadAlignment(cmsIOHANDLER io)
	{
	    byte[] Buffer = new byte[4];
	    int NextAligned, At;
	    int BytesToNextAlignedPos;
	    
	    lcms2_internal._cmsAssert(io != null, "io != null");
	    
	    At = io.Tell.run(io);
	    NextAligned = lcms2_internal._cmsALIGNLONG(At);
	    BytesToNextAlignedPos = NextAligned - At;
	    if (BytesToNextAlignedPos == 0)
	    {
	    	return true;
	    }
	    if (BytesToNextAlignedPos > 4)
	    {
	    	return false;
	    }
	    
	    return (io.Read.run(io, Buffer, BytesToNextAlignedPos, 1) == 1);
	}
	
	public static boolean _cmsWriteAlignment(cmsIOHANDLER io)
	{
		byte[] Buffer = new byte[4];
	    int NextAligned, At;
	    int BytesToNextAlignedPos;
	    
	    lcms2_internal._cmsAssert(io != null, "io != null");
	    
	    At = io.Tell.run(io);
	    NextAligned = lcms2_internal._cmsALIGNLONG(At);
	    BytesToNextAlignedPos = NextAligned - At;
	    if (BytesToNextAlignedPos == 0)
	    {
	    	return true;
	    }
	    if (BytesToNextAlignedPos > 4)
	    {
	    	return false;
	    }
	    
	    return io.Write.run(io, BytesToNextAlignedPos, Buffer);
	}
	
	// To deal with text streams. 2K at most
	public static boolean _cmsIOPrintf(cmsIOHANDLER io, final String frm, Object[] args)
	{
		int len;
		StringBuffer Buffer = new StringBuffer(new String(new char[2048]));
		boolean rc;
		
		lcms2_internal._cmsAssert(io != null, "io != null");
		lcms2_internal._cmsAssert(frm != null, "frm != null");
		
		len = Utility.vsnprintf(Buffer, 2047, frm, args);
		if (len < 0)
		{
			return false; // Truncated, which is a fatal error for us
		}
		
		try
		{
			rc = io.Write.run(io, len, Buffer.toString().getBytes("US-ASCII"));
		}
		catch(Exception e)
		{
			rc = false;
		}
		
		return rc;
	}
	
	// Plugin memory management -------------------------------------------------------------------------------------------------
	
	private static _cmsSubAllocator PluginPool = null;
	
	private static final long PLUGIN_POOL_UID = 0L;
	
	static
	{
		Object obj;
		if((obj = Utility.singletonStorageGet(PLUGIN_POOL_UID)) != null)
		{
			PluginPool = (_cmsSubAllocator)obj;
		}
		else
		{
			PluginPool = null;
		}
	}
	
	// Specialized malloc for plug-ins, that is freed upon exit.
	public static Object _cmsPluginMalloc(int size)
	{
	    if (PluginPool == null)
	    {
	    	PluginPool = cmserr._cmsCreateSubAlloc(null, 4 * 1024);
	    	Utility.singletonStorageSet(PLUGIN_POOL_UID, PluginPool);
	    }
	    
	    return cmserr._cmsSubAlloc(PluginPool, size);
	}
	
	// Main plug-in dispatcher
	public static boolean cmsPlugin(cmsPluginBase Plug_in)
	{
	    cmsPluginBase Plugin;
	    
	    for (Plugin = Plug_in; Plugin != null; Plugin = Plugin.Next)
	    {
	    	if (Plugin.Magic != lcms2_plugin.cmsPluginMagicNumber)
	    	{
	    		cmserr.cmsSignalError(null, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSPLUGIN_UNRECOGNIZED_PLUGIN), null);
                return false;
	    	}
	    	
			if (Plugin.ExpectedVersion > lcms2.LCMS_VERSION)
			{
				cmserr.cmsSignalError(null, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSPLUGIN_NEED_HIGHER_VERSION), 
						new Object[]{new Integer(Plugin.ExpectedVersion), new Integer(lcms2.LCMS_VERSION)});
				return false;
			}
			
            switch (Plugin.Type)
            {
                case lcms2_plugin.cmsPluginMemHandlerSig:
                    if (!cmserr._cmsRegisterMemHandlerPlugin(Plugin))
                    {
                    	return false;
                    }
                    break;
                case lcms2_plugin.cmsPluginInterpolationSig:
                    if (!cmsintrp._cmsRegisterInterpPlugin(Plugin))
                    {
                    	return false;
                    }
                    break;
                case lcms2_plugin.cmsPluginTagTypeSig:
                    if (!cmstypes._cmsRegisterTagTypePlugin(Plugin))
                    {
                    	return false;
                    }
                    break;
                case lcms2_plugin.cmsPluginTagSig:
                    if (!cmstypes._cmsRegisterTagPlugin(Plugin))
                    {
                    	return false;
                    }
                    break;
                case lcms2_plugin.cmsPluginFormattersSig:
                    if (!cmspack._cmsRegisterFormattersPlugin(Plugin))
                    {
                    	return false;
                    }
                    break;
                case lcms2_plugin.cmsPluginRenderingIntentSig:
                    if (!cmscnvrt._cmsRegisterRenderingIntentPlugin(Plugin))
                    {
                    	return false;
                    }
                    break;
                case lcms2_plugin.cmsPluginParametricCurveSig:
                    if (!cmsgamma._cmsRegisterParametricCurvesPlugin(Plugin))
                    {
                    	return false;
                    }
                    break;
                case lcms2_plugin.cmsPluginMultiProcessElementSig:
                    if (!cmstypes._cmsRegisterMultiProcessElementPlugin(Plugin))
                    {
                    	return false;
                    }
                    break;
                case lcms2_plugin.cmsPluginOptimizationSig:
                    if (!cmsopt._cmsRegisterOptimizationPlugin(Plugin))
                    {
                    	return false;
                    }
                    break;
                default:
                    cmserr.cmsSignalError(null, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSPLUGIN_UNRECOGNIZED_PLUGIN_TYPE), 
                    		new Object[]{new Integer(Plugin.Type)});
                    return false;
            }
	    }
	    
	    // Keep a reference to the plug-in
	    return true;
	}
	
	// Revert all plug-ins to default
	public static void cmsUnregisterPlugins()
	{
		cmserr._cmsRegisterMemHandlerPlugin(null);
		cmsintrp._cmsRegisterInterpPlugin(null);
		cmstypes._cmsRegisterTagTypePlugin(null);
		cmstypes._cmsRegisterTagPlugin(null);
		cmspack._cmsRegisterFormattersPlugin(null);
		cmscnvrt._cmsRegisterRenderingIntentPlugin(null);
		cmsgamma._cmsRegisterParametricCurvesPlugin(null);
		cmstypes._cmsRegisterMultiProcessElementPlugin(null);
		cmsopt._cmsRegisterOptimizationPlugin(null);
	    
	    if (PluginPool != null)
	    {
	    	cmserr._cmsSubAllocDestroy(PluginPool);
	    }
	    
	    PluginPool = null;
	}
}
