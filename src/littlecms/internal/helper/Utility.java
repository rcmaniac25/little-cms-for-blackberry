//#preprocessor

//---------------------------------------------------------------------------------
//
//  Little Color Management System
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
//@Author Vinnie Simonetti
package littlecms.internal.helper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Calendar;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import littlecms.internal.LCMSResource;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.i18n.SimpleDateFormat;
import net.rim.device.api.system.RuntimeStore;
import net.rim.device.api.util.Arrays;
//#ifndef BlackBerrySDK4.5.0
import net.rim.device.api.util.LongVector;
//#endif
import net.rim.device.api.util.MathUtilities;

/**
 * Some simply helper functions for processing. Mainly C functions that don't exist (some due to OS versioning) in the BB API.
 */
public final class Utility
{
	public static final ResourceBundle LCMS_Resources = ResourceBundle.getBundle(LCMSResource.BUNDLE_ID, LCMSResource.BUNDLE_NAME);
	private static final long SINGLETON_STORAGE_ID = 0x2794B93D60A08F80L;
	
//#ifdef BlackBerrySDK4.5.0
	public static double pow(double base, double power)
	{
		return MathUtilities.exp(power * MathUtilities.log(base));
	}
	
	//atan code and constants taken from cmath.tgz (http://www.netlib.org/cephes/)
	
	/*
	Cephes Math Library Release 2.8:  June, 2000
	Copyright 1984, 1995, 2000 by Stephen L. Moshier
	*/
	private static final double[] P = {
	    -8.750608600031904122785E-1,
	    -1.615753718733365076637E1,
	    -7.500855792314704667340E1,
	    -1.228866684490136173410E2,
	    -6.485021904942025371773E1
    };
    private static final double[] Q = {
	    /* 1.000000000000000000000E0, */
	     2.485846490142306297962E1,
	     1.650270098316988542046E2,
	     4.328810604912902668951E2,
	     4.853903996359136964868E2,
	     1.945506571482613964425E2
    };
    
    /* tan( 3*pi/8 ) */
    private static final double T3P8 = 2.41421356237309504880;
    private static final double MOREBITS = 6.123233995736765886130E-17;
    
    //polevl and p1evl taken from Java-ML 0.1.6 (Java Machine Learning Library)(http://java-ml.sourceforge.net/)
    /*
    Copyright (C) 1999 CERN - European Organization for Nuclear Research.
    Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose 
    is hereby granted without fee, provided that the above copyright notice appear in all copies and 
    that both that copyright notice and this permission notice appear in supporting documentation. 
    CERN makes no representations about the suitability of this software for any purpose. 
    It is provided "as is" without expressed or implied warranty.
    */
    private static double p1evl(double x, double[] coef, int N)
    {
        double ans;
        
        ans = x + coef[0];
        
        for (int i = 1; i < N; i++)
        {
            ans = ans * x + coef[i];
        }
        
        return ans;
    }
    
    private static double polevl(double x, double[] coef, int N)
    {
        double ans;
        ans = coef[0];
        
        for (int i = 1; i <= N; i++)
        {
        	ans = ans * x + coef[i];
        }
        
        return ans;
    }
	
	public static double atan(double x)
	{
		// Special cases.
		if (Double.isNaN(x))
		{
			return Double.NaN;
		}
		
		if(Double.isInfinite(x))
		{
			if(x < 0)
			{
				return -PIover2;
			}
			else
			{
				return PIover2;
			}
		}
		
		if (x == 0.0)
		{
			return x;
		}
		
		// Compute the arc tangent.
        double y, z;
        short sign, flag;
        
        //make argument positive and save the sign
        sign = 1;
        if (x < 0.0)
        {
            sign = -1;
            x = -x;
        }
        //range reduction
        flag = 0;
        if (x > T3P8)
        {
            y = PIover2;
            flag = 1;
            x = -(1.0 / x);
        }
        else if (x <= 0.66)
        {
            y = 0.0;
        }
        else
        {
            y = PIover4;
            flag = 2;
            x = (x - 1.0) / (x + 1.0);
        }
        z = x * x;
        z = z * polevl(z, P, 4) / p1evl(z, Q, 5);
        z = x * z + x;
        if (flag == 2)
        {
        	z += 0.5 * MOREBITS;
        }
        else if (flag == 1)
        {
        	z += MOREBITS;
        }
        y = y + z;
        if (sign < 0)
        {
        	y = -y;
        }
        return y;
	}
	
	//Following code taken from J4ME (http://code.google.com/p/j4me/)
	private static final double PIover2 = Math.PI / 2;
	private static final double PIover4 = Math.PI / 4;
	
	public static double atan2(double y, double x)
	{
		// Special cases.
		if ( Double.isNaN(y) || Double.isNaN(x) )
		{
			return Double.NaN;
		}
		else if ( Double.isInfinite(y) )
		{
			if ( y > 0.0 ) // Positive infinity
			{
				if ( Double.isInfinite(x) )
				{
					if ( x > 0.0 )
					{
						return PIover4;
					}
					else
					{
						return 3.0 * PIover4;
					}
				}
				else if ( x != 0.0 )
				{
					return PIover2;
				}
			}
			else  // Negative infinity
			{
				if ( Double.isInfinite(x) )
				{
					if ( x > 0.0 )
					{
						return -PIover4;
					}
					else
					{
						return -3.0 * PIover4;
					}
				}
				else if ( x != 0.0 )
				{
					return -PIover2;
				}
			}
		}
		else if ( y == 0.0 )
		{
			if ( x > 0.0 )
			{
				return y;
			}
			else if ( x < 0.0 )
			{
				return Math.PI;
			}
		}
		else if ( Double.isInfinite(x) )
		{
			if ( x > 0.0 )  // Positive infinity
			{
				if ( y > 0.0 )
				{
					return 0.0;
				}
				else if ( y < 0.0 )
				{
					return -0.0;
				}
			}
			else  // Negative infinity
			{
				if ( y > 0.0 )
				{
					return Math.PI;
				}
				else if ( y < 0.0 )
				{
					return -Math.PI;
				}
			}
		}
		else if ( x == 0.0 )
		{
			if ( y > 0.0 )
			{
				return PIover2;
			}
			else if ( y < 0.0 )
			{
				return -PIover2;
			}
		}
		
		
		// Implementation a simple version ported from a PASCAL implementation:
		//   http://everything2.com/index.pl?node_id=1008481
		
		double arcTangent;
		
		// Use arctan() avoiding division by zero.
		if ( Math.abs(x) > Math.abs(y) )
		{
			arcTangent = atan(y / x);
		}
		else
		{
			arcTangent = atan(x / y); // -PI/4 <= a <= PI/4

			if ( arcTangent < 0 )
			{
				arcTangent = -PIover2 - arcTangent; // a is negative, so we're adding
			}
			else
			{
				arcTangent = PIover2 - arcTangent;
			}
		}
		
		// Adjust result to be from [-PI, PI]
		if ( x < 0 )
		{
			if ( y < 0 )
			{
				arcTangent = arcTangent - Math.PI;
			}
			else
			{
				arcTangent = arcTangent + Math.PI;
			}
		}
		
		return arcTangent;
	}
	//END CODE FROM J4ME
//#endif
	
	public static double log10(double x)
	{
		return MathUtilities.log(x) / MathUtilities.log(10);
	}
	
	private static boolean charNumRangeComp(int c, int lowerEnd, int upperEnd)
    {
    	return Character.isDigit((char)c) || (((c >= 'a') || (c <= lowerEnd)) || ((c >= 'A') || (c <= upperEnd)));
    }
    
	//Check if character is alpha-numeric
	public static boolean isalnum(int c)
    {
    	return charNumRangeComp(c, 'z', 'Z');
    }
    
	//Check if character is a digit
    public static boolean isdigit(int c)
    {
    	return Character.isDigit((char)c);
    }
    
    //Check if character is a hexidecimal digit
    public static boolean isxdigit(int c)
    {
    	return charNumRangeComp(c, 'f', 'F');
    }
    
    //Based off J2SE docs version 6
    public static char forDigit(int digit, int radix)
    {
    	if(radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
    	{
    		return '\0';
    	}
    	if(digit < 0 || digit > radix)
    	{
    		return '\0';
    	}
    	if(digit < 10)
    	{
    		return (char)('0' + digit);
    	}
    	return (char)('a' + digit - 10);
    }
    
    //Convert a Calendar to a String based off ctime
    public static String ctime(Calendar timer)
	{
    	//ctime format: "Www Mmm dd hh:mm:ss yyyy" and appends \n to the end
    	return new SimpleDateFormat(LCMS_Resources.getString(LCMSResource.UTILS_CTIME_FORMAT)).format(timer, new StringBuffer(), null).toString();//Ex: Sun Mar 08 2:10:20 2006
	}
    
    //Singleton functions (taken from PDFRenderer for BlackBerry)
    
    /**
	 * Get a singleton object. This is not the same as just calling RuntimeStore and is managed for memory usage. It will be cleaned up when 
	 * {@link #singltonStorageCleanup()} is called.
	 * @param uid The ID of the object to get.
	 * @return The object (if it was set using {@link #singletonStorageSet(long, Object)}) or null if it doesn't exist or was not set using {@link #singletonStorageSet(long, Object)}.
	 */
	public synchronized static Object singletonStorageGet(long uid)
	{
		RuntimeStore store = RuntimeStore.getRuntimeStore();
		Object obj;
		if((obj = store.get(SINGLETON_STORAGE_ID)) != null)
		{
//#ifndef BlackBerrySDK4.5.0
			LongVector v = (LongVector)obj;
			if(v.contains(uid))
			{
				return store.get(uid);
			}
//#else
			long[] v = (long[])obj;
			int len = v.length;
			boolean hasIndex = false;
			for(int i = 1; i < len; i++)
			{
				if(v[i] == uid)
				{
					hasIndex = true;
					break;
				}
			}
			if(hasIndex)
			{
				return store.get(uid);
			}
//#endif
		}
		return null;
	}
	
	/**
	 * Set a singleton object.
	 * @param uid The ID of the object to set. If this happens to be an object that already exists but was not set using this function then an exception will be thrown.
	 * @param obj The singleton object to set or null if the current object should be removed.
	 * @return The previous object (if it was set using {@link #singletonStorageSet(long, Object)}) or null if it didn't exist or was not set using {@link #singletonStorageSet(long, Object)}.
	 */
	public synchronized static Object singletonStorageSet(long uid, Object obj)
	{
		RuntimeStore store = RuntimeStore.getRuntimeStore();
		Object objS;
//#ifndef BlackBerrySDK4.5.0
		LongVector v;
//#else
		long[] v;
//#endif
		if((objS = store.get(SINGLETON_STORAGE_ID)) != null) //Singleton list exists
		{
//#ifndef BlackBerrySDK4.5.0
			v = (LongVector)objS;
			if(v.contains(uid))
			{
				objS = store.get(uid); //Get previous value
				if(obj != null)
				{
					store.replace(uid, obj); //Replace the current object
				}
				else
				{
					store.remove(uid); //Remove the object
					v.removeElement(uid);
				}
				return objS; //Return previous object
			}
			else if(obj != null) //Does not exist in Singleton list exists, new
			{
				store.put(uid, obj);
				v.addElement(uid);
				return null;
			}
//#else
			v = (long[])objS;
			int len = v.length;
			int index = -1;
			for(int i = 1; i < len; i++)
			{
				if(v[i] == uid)
				{
					index = i;
					break;
				}
			}
			if(index >= 1)
			{
				objS = store.get(uid); //Get previous value
				if(obj != null)
				{
					store.replace(uid, obj); //Replace the current object
				}
				else
				{
					store.remove(uid); //Remove the object
					System.arraycopy(v, index + 1, v, index, (int)((--v[0]) - index));
				}
				return objS; //Return previous object
			}
			else if(obj != null) //Does not exist in Singleton list exists, new
			{
				store.put(uid, obj);
				if(v[0] >= v.length)
				{
					long[] t = new long[v.length * 2];
					System.arraycopy(v, 0, t, 0, v.length);
					v = t;
					store.replace(SINGLETON_STORAGE_ID, v);
				}
				v[(int)(v[0]++)] = uid;
				return null;
			}
//#endif
		}
		if(obj != null) //If the function hasn't returned yet and the object is not null then the Singleton list doesn't exist yet
		{
//#ifndef BlackBerrySDK4.5.0
			v = new LongVector(); //Create the list and add the object
//#else
			v = new long[1 + 4];
			v[0] = 1;
//#endif
			store.put(SINGLETON_STORAGE_ID, v);
			store.put(uid, obj); //Will throw an exception if already there
//#ifndef BlackBerrySDK4.5.0
			v.addElement(uid);
//#else
			v[(int)(v[0]++)] = uid;
//#endif
		}
		return null;
	}
	
	/**
	 * Remove all singleton objects.
	 */
	public synchronized static void singltonStorageCleanup()
	{
		RuntimeStore store = RuntimeStore.getRuntimeStore();
		Object obj;
		if((obj = store.get(SINGLETON_STORAGE_ID)) != null)
		{
//#ifndef BlackBerrySDK4.5.0
			LongVector v = (LongVector)obj;
			store.remove(SINGLETON_STORAGE_ID);
			int len = v.size();
			for(int i = 0; i < len; i++)
			{
				store.remove(v.elementAt(i));
			}
//#else
			long[] v = (long[])obj;
			store.remove(SINGLETON_STORAGE_ID);
			int len = v.length;
			for(int i = 1; i < len; i++)
			{
				store.remove(v[i]);
			}
//#endif
		}
	}
	
	public static boolean isPrimitive(Object obj)
	{
		return isPrimitive(obj, false);
	}
	
	public static boolean isPrimitive(Object obj, boolean readOnlyAllowed)
	{
		if(obj instanceof byte[] || obj instanceof short[] || obj instanceof char[] || obj instanceof int[] || obj instanceof long[] || 
				obj instanceof float[] || obj instanceof double[] || obj instanceof boolean[] || obj instanceof String[] || obj instanceof VirtualPointer[])
		{
			return true;
		}
		if(readOnlyAllowed)
		{
			if(obj instanceof Byte[] || obj instanceof Short[] || obj instanceof Character[] || obj instanceof Integer[] || obj instanceof Long[] || 
					obj instanceof Float[] || obj instanceof Double[] || obj instanceof Boolean[])
			{
				return true;
			}
			if(obj instanceof Byte || obj instanceof Short || obj instanceof Character || obj instanceof Integer || obj instanceof Long || 
					obj instanceof Float || obj instanceof Double || obj instanceof Boolean)
			{
				return true;
			}
		}
		return false;
	}
	
	//C99 remove function
	public static int remove(final String filename)
	{
		int result = -1;
		try
		{
			FileConnection file = (FileConnection)Connector.open(filename, Connector.READ_WRITE);
			if(file.exists())
			{
				file.delete();
			}
			if(!file.exists())
			{
				result = 0;
			}
			file.close();
		}
		catch(IOException e)
		{
			result = -2;
		}
		return result;
	}
    
    //strlen
    
    public static int strlen(String str)
    {
    	return strlen(str.toCharArray());
    }
    
    public static int strlen(char[] chars)
    {
    	return strlen(chars, 0);
    }
    
    public static int strlen(char[] chars, int origin)
    {
    	//This will work on both C style strings and Java strings
    	int len = origin;
    	int aLen = chars.length;
    	while(len < aLen && chars[len++] != 0);
    	if(len == aLen && chars[len - 1] != 0)
    	{
    		len++;
    	}
    	return (len - origin) - 1;
    }
    
    public static int strlen(byte[] chars)
    {
    	return strlen(chars, 0);
    }
    
    public static int strlen(byte[] chars, int origin)
    {
    	//This will work on both C style strings and Java strings
    	int len = origin;
    	int aLen = chars.length;
    	while(len < aLen && chars[len++] != 0);
    	if(len == aLen && chars[len - 1] != 0)
    	{
    		len++;
    	}
    	return (len - origin) - 1;
    }
    
    public static int strlen(VirtualPointer vp)
    {
    	//Very inefficent... deal with it for now
    	int len = vp.getAllocLen();
    	byte[] dat = new byte[len - vp.getPosition()];
    	vp.readByteArray(dat, 0, dat.length, false, false);
    	return strlen(dat, 0);
    }
    
    //strcpy
    
    public static int strcpy(char[] dst, final String src)
    {
    	return strncpy(dst, 0, src.toCharArray(), 0, Math.min(strlen(dst), strlen(src)) + 1);
    }
    
    public static int strcpy(VirtualPointer dst, final char[] src)
    {
    	return strncpy(dst, 0, src, 0, Math.min(strlen(dst), strlen(src)) + 1);
    }
    
    //strncpy
    
    public static int strncpy(char[] dst, final String src, int count)
    {
    	return strncpy(dst, 0, src.toCharArray(), 0, count);
    }
    
    public static int strncpy(StringBuffer dst, final VirtualPointer src, int count)
    {
    	//Not efficent at all but the only way to get from one to the other
    	int len = strlen(src) + 1;
    	byte[] data = new byte[len];
    	src.readByteArray(data, 0, len - 1, false, false);
    	char[] chars = new char[len];
    	for(int i = 0; i < len; i++)
    	{
    		chars[i] = (char)data[i];
    	}
    	return strncpy(dst, 0, chars, 0, count);
    }
    
    public static int strncpy(char[] dst, final VirtualPointer src, int count)
    {
    	//Not efficent at all but the only way to get from one to the other
    	int len = strlen(src) + 1;
    	byte[] data = new byte[len];
    	src.readByteArray(data, 0, len - 1, false, false);
    	char[] chars = new char[len];
    	for(int i = 0; i < len; i++)
    	{
    		chars[i] = (char)data[i];
    	}
    	return strncpy(dst, 0, chars, 0, count);
    }
    
    public static int strncpy(VirtualPointer dst, final String src, int count)
    {
    	return strncpy(dst, 0, src.toCharArray(), 0, count);
    }
    
    public static int strncpy(VirtualPointer dst, final byte[] src, int count)
    {
    	return strncpy(dst, 0, src, 0, count);
    }
    
    public static int strncpy(StringBuffer dst, final String src, int count)
    {
    	return strncpy(dst, 0, src.toCharArray(), 0, count);
    }
    
    public static int strncpy(char[] dst, char[] src, int count)
    {
    	return strncpy(dst, 0, src, 0, count);
    }
    
    public static int strncpy(char[] dst, int dstOffset, char[] src, int srcOffset, int count)
    {
    	int len = Math.min(strlen(src, srcOffset), count);
    	System.arraycopy(src, srcOffset, dst, dstOffset, len);
    	return len;
    }
    
    public static int strncpy(VirtualPointer dst, int dstOffset, char[] src, int srcOffset, int count)
    {
    	int len = Math.min(strlen(src, srcOffset), count);
    	byte[] dat = new byte[len];
    	for(int i = 0; i < len; i++)
    	{
    		dat[i] = (byte)src[srcOffset + i];
    	}
    	dst.writeRaw(dat, 0, len, dstOffset);
    	return len;
    }
    
    public static int strncpy(VirtualPointer dst, int dstOffset, byte[] src, int srcOffset, int count)
    {
    	int len = Math.min(strlen(src, srcOffset), count);
    	dst.writeRaw(src, srcOffset, len, dstOffset);
    	return len;
    }
    
    public static int strncpy(StringBuffer dst, int dstOffset, final String src, int srcOffset, int count)
    {
    	return strncpy(dst, dstOffset, src.toCharArray(), srcOffset, count);
    }
    
    public static int strncpy(StringBuffer dst, int dstOffset, char[] src, int srcOffset, int count)
    {
    	int len = Math.min(strlen(src, srcOffset), count);
    	int appendPos = dst.length();
    	len += dstOffset;
    	for(int d = dstOffset, s = srcOffset; d < len; d++, s++)
    	{
    		if(d >= appendPos)
    		{
    			dst.append(src[s]);
    		}
    		else
    		{
    			dst.setCharAt(d, src[s]);
    		}
    	}
    	return len - dstOffset;
    }
    
    public static int strspn(char[] chars, int origin, char[] charsToMatch)
    {
    	int len = chars.length;
    	int count = 0;
    	for(int i = origin; i < len; i++)
    	{
    		if(Arrays.getIndex(charsToMatch, chars[i]) >= 0)
    		{
    			count++;
    		}
    		else
    		{
    			break;
    		}
    	}
    	return count;
    }
    
    //cstringCreation: creating a String based on how it is treated in C/C++
    
    public static String cstringCreation(byte[] chars)
    {
    	return cstringCreation(chars, 0);
    }
    
    public static String cstringCreation(byte[] chars, int origin)
    {
    	int len = strlen(chars, origin);
    	StringBuffer buf = new StringBuffer(len);
    	len += origin;
    	for(int i = origin; i < len; i++)
    	{
    		buf.append((char)chars[i]);
    	}
    	return buf.toString();
    }
    
    public static String cstringCreation(char[] chars)
    {
    	return cstringCreation(chars, 0);
    }
    
    public static String cstringCreation(char[] chars, int origin)
    {
    	return new String(chars, origin, strlen(chars, origin));
    }
    
    //Print functions
    
    public static int vsnprintf(StringBuffer buffer, int count, final String format, Object[] argptr)
	{
    	return fprintf(new PrintStream(new CharOut(buffer)), count, format, argptr);
	}
    
    public static int vsprintf(char[] buffer, final String format, Object[] argptr)
	{
		return vsnprintf(buffer, -1, format, argptr);
	}
	
	public static int vsnprintf(char[] buffer, int count, final String format, Object[] argptr)
	{
		return fprintf(new PrintStream(new CharOut(buffer)), count, format, argptr);
	}
	
	/* Ignore
	public static int snprintf(StringBuffer buffer, int buff_size, final String format, Object[] argptr)
	{
		//C version is "int snprintf(char* buffer, int buff_size, const char* format, ...)" and simply calls vsnprintf after getting the va_arg. Simply pass the value.
		return vsnprintf(buffer, buff_size, format, argptr);
	}
	*/
	
	public static int sprintf(StringBuffer buffer, final String format, Object[] argptr)
	{
		return fprintf(new PrintStream(new CharOut(buffer)), format, argptr);
	}
	
	public static int sprintf(char[] buffer, final String format, Object[] argptr)
	{
		return fprintf(new PrintStream(new CharOut(buffer)), format, argptr);
	}
	
	public static int printf(final String format, Object[] argptr)
	{
		return fprintf(System.out, format, argptr);
	}
	
	private static class CharOut extends OutputStream
	{
		private StringBuffer strBuf;
		private char[] chars;
		private int pos;
		private int appendPos;
		
		public CharOut(StringBuffer buf)
		{
			this.strBuf = buf;
			this.pos = 0;
			this.appendPos = buf.length();
		}
		
		public CharOut(char[] buf)
		{
			this.chars = buf;
			this.pos = 0;
		}
		
		public void write(int b) throws IOException
		{
			write(new byte[]{(byte)b});
		}
		
		public void write(byte[] b, int off, int len) throws IOException
		{
			len += off;
			for(int i = off; i < len; i++)
			{
				if(strBuf == null)
				{
					this.chars[pos++] = (char)b[i];
					if(pos >= chars.length)
					{
						break;
					}
				}
				else
				{
					if(pos >= appendPos)
					{
						this.strBuf.append((char)b[i]);
						pos++;
					}
					else
					{
						this.strBuf.setCharAt(pos++, (char)b[i]);
					}
				}
			}
		}
	}
	
	private static class StreamOut extends OutputStream
	{
		private Stream st;
		
		public StreamOut(Stream st)
		{
			this.st = st;
		}
		
		public void write(int b) throws IOException
		{
			write(new byte[]{(byte)b}, 0, 1);
		}
		
		public void write(byte[] b, int off, int len) throws IOException
		{
			st.write(b, off, len, 1);
		}
	}
	
	public static int fprintf(PrintStream stream, final String format, Object[] argptr)
	{
		return fprintf(stream, -1, format, argptr);
	}
	
	public static int fprintf(Stream stream, final String format, Object[] argptr)
	{
		return fprintf(new PrintStream(new StreamOut(stream)), -1, format, argptr);
	}
	
	public static int fprintf(PrintStream stream, int count, final String format, Object[] argptr)
	{
		if(format == null)
		{
			return 0;
		}
		/* Not needed anymore
		if(argptr == null)
		{
			argptr = new Object[0];
		}
		*/
		return PrintUtility.output(stream, count, format, argptr, null);
	}
    
	public static int sscanf(final String str, final String format, Object[] argptr)
    {
		int len = 0;
		try
		{
			byte[] data = str.getBytes("UTF-8");
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			len = fscanf(bais, format, argptr);
			bais.close();
		}
		catch(IOException ioe)
		{
		}
    	return len;
    }
	
    public static int fscanf(final InputStream file, final String format, Object[] argptr)
    {
    	return PrintUtility.fscanf(file, format, argptr, null);
    }
}
