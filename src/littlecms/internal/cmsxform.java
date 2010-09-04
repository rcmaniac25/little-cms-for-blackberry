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

import littlecms.internal.helper.VirtualPointer;
import littlecms.internal.lcms2.cmsHTRANSFORM;
import littlecms.internal.lcms2_internal._cmsTRANSFORM;

/**
 * Transformations stuff
 */
//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmsxform
{
	//TODO: #32-94
	
	// Apply transform
	public static void cmsDoTransform(cmsHTRANSFORM Transform, final Object InputBuffer, Object OutputBuffer, int Size)
	{
		_cmsTRANSFORM p = (_cmsTRANSFORM)Transform;
	    
		VirtualPointer inBuf = buffer2vp(InputBuffer);
		VirtualPointer outBuf = buffer2vp(OutputBuffer);
	    p.xform.run(p, inBuf, outBuf, Size);
	    vp2buffer(outBuf, OutputBuffer);
	}
	
	private static VirtualPointer buffer2vp(final Object buffer)
	{
		if(buffer == null)
		{
			return null;
		}
		if(!buffer.getClass().isArray())
		{
			return ref2vp(buffer);
		}
		Class clazz = buffer.getClass();
		Class nClass = null;
		try
		{
			nClass = Class.forName(getBaseClass(clazz.getName()));
		}
		catch (ClassNotFoundException e)
		{
		}
		if(nClass.isArray())
		{
			return null;
		}
		//TODO
		return null;
	}
	
	private static VirtualPointer ref2vp(final Object ref)
	{
		//TODO
		return null;
	}
	
	private static String getBaseClass(String clazz)
	{
		//Not sure if this will work, will know later
		boolean array = false;
		boolean obj = false;
		int len = clazz.length();
		for(int i = 0; i < len; i++)
		{
			if(!array)
			{
				if(clazz.charAt(i) == '[')
				{
					array = true;
				}
			}
			else if(!obj)
			{
				if(clazz.charAt(i) != '[')
				{
					obj = true;
				}
			}
			else
			{
				return clazz.substring(i);
			}
		}
		return clazz;
	}
	
	private static void vp2buffer(VirtualPointer vp, Object buffer)
	{
		if(vp == null)
		{
			return;
		}
		//TODO
	}
	
	//TODO: #109
}
