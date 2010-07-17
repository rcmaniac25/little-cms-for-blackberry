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

/**
 * Generic I/O, tag dictionary management, profile struct
 * <p>
 * IOhandlers are abstractions used by littleCMS to read from whatever file, stream,
 * memory block or any storage. Each IOhandler provides implementations for read,
 * write, seek and tell functions. LittleCMS code deals with IO across those objects.
 * In this way, is easier to add support for new storage media.
 * <p>
 * NULL stream, for taking care of used space -------------------------------------
 * <p>
 * NULL IOhandler basically does nothing but keep track on how many bytes have been
 * written. This is handy when creating profiles, where the file size is needed in the
 * header. Then, whole profile is serialized across NULL IOhandler and a second pass 
 * writes the bytes to the pertinent IOhandler.
 */
final class cmsio0
{
	private static class FILENULL
	{
		public int Pointer; // Points to current location
	}
	
	//TODO: #47-1030
	
	public static lcms2.cmsHPROFILE cmsOpenProfileFromMem(final byte[] MemPtr, int dwSize)
	{
	    return cmsOpenProfileFromMemTHR(null, MemPtr, dwSize);
	}
	
	//TODO: #1038-1531
	
	// Read and write raw data. The only way those function would work and keep consistence with normal read and write 
	// is to do an additional step of serialization. That means, readRaw would issue a normal read and then convert the obtained
	// data to raw bytes by using the "write" serialization logic. And vice-versa. I know this may end in situations where
	// raw data written does not exactly correspond with the raw data proposed to cmsWriteRaw data, but this approach allows
	// to write a tag as raw data and the read it as handled.

	public static int cmsReadRawTag(lcms2.cmsHPROFILE hProfile, int sig, byte[] data, int BufferSize)
	{
		//TODO: #1541-1616
	}
	
	//TODO: #1619
}
