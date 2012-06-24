//---------------------------------------------------------------------------------
//
//  Little Color Management System
//  Copyright (c) 1998-2011 Marti Maria Saguer
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

import littlecms.internal.lcms2.cmsPipeline;
import littlecms.internal.lcms2_internal._cmsTRANSFORM;
import littlecms.internal.helper.VirtualPointer;

/**
 * This is the plug-in header file. Normal LittleCMS clients should not use it.
 * It is provided for plug-in writers that may want to access the support
 * functions to do low level operations. All plug-in related structures
 * are defined here. Including this file forces to include the standard API too.
 */
public class lcms2_plugin extends lcms2
{
	// Vector & Matrix operations -----------------------------------------------------------------------
	
	// Axis of the matrix/array. No specific meaning at all.
	public static final int VX = 0;
	public static final int VY = 1;
	public static final int VZ = 2;
	
	// Vectors
	public static class cmsVEC3
	{
		public static final int SIZE = 8 * 3;
		
		public double[] n;
		
		public cmsVEC3()
		{
			n = new double[3];
		}
	}
	
	// 3x3 Matrix
	public static class cmsMAT3
	{
		public static final int SIZE = cmsVEC3.SIZE * 3;
		
		public cmsVEC3[] v;
		
		public cmsMAT3()
		{
			v = new cmsVEC3[3];
			for(int i = 0; i < 3; i++)
			{
				v[i] = new cmsVEC3();
			}
		}
	}
	
	/**
	 * Populates a vector.
	 * @param r a pointer to a cmsVEC3 object to receive the result
	 * @param x component of the vector
	 * @param y component of the vector
	 * @param z component of the vector
	 */
	public static void _cmsVEC3init(cmsVEC3 r, double x, double y, double z)
	{
		cmsmtrx._cmsVEC3init(r, x, y, z);
	}
	
	/**
	 * Vector subtraction.
	 * @param r a pointer to a cmsVEC3 object to receive the result
	 * @param a A pointer to first cmsVEC3 object.
	 * @param b A pointer to second cmsVEC3 object.
	 */
	public static void _cmsVEC3minus(cmsVEC3 r, final cmsVEC3 a, final cmsVEC3 b)
	{
		cmsmtrx._cmsVEC3minus(r, a, b);
	}
	
	/**
	 * Vector cross product.
	 * @param r a pointer to a cmsVEC3 object to receive the result.
	 * @param u A pointer to first cmsVEC3 object.
	 * @param v A pointer to second cmsVEC3 object.
	 */
	public static void _cmsVEC3cross(cmsVEC3 r, final cmsVEC3 u, final cmsVEC3 v)
	{
		cmsmtrx._cmsVEC3cross(r, u, v);
	}
	
	/**
	 * Vector dot product
	 * @param u A pointer to first cmsVEC3 object.
	 * @param v A pointer to second cmsVEC3 object.
	 * @return Dot product <i>u * v</i>
	 */
	public static double _cmsVEC3dot(final cmsVEC3 u, final cmsVEC3 v)
	{
		return cmsmtrx._cmsVEC3dot(u, v);
	}
	
	/**
	 * Euclidean length of 3D vector
	 * @param a A pointer to first cmsVEC3 object.
	 * @return Euclidean length <i>sqrt(x<sup>2</sup> + y<sup>2</sup> + z<sup>2</sup>)</i>
	 */
	public static double _cmsVEC3length(final cmsVEC3 a)
	{
		return cmsmtrx._cmsVEC3length(a);
	}
	
	/**
	 * Returns euclidean distance between two 3D points.
	 * @param a A pointer to first cmsVEC3 object.
	 * @param b A pointer to second cmsVEC3 object.
	 * @return Euclidean distance <i>sqrt(a<sup>2</sup> + b<sup>2</sup>)</i>
	 */
	public static double _cmsVEC3distance(final cmsVEC3 a, final cmsVEC3 b)
	{
		return cmsmtrx._cmsVEC3distance(a, b);
	}
	
	/**
	 * Fills “a” with identity matrix
	 * @param a A pointer to a cmsMAT3 object.
	 */
	public static void _cmsMAT3identity(cmsMAT3 a)
	{
		cmsmtrx._cmsMAT3identity(a);
	}
	
	/**
	 * Return true if “a” is close enough to be interpreted as identity. Else return false
	 * @param a A pointer to a cmsMAT3 object.
	 * @return TRUE on identity, FALSE on non-identity.
	 */
	public static boolean _cmsMAT3isIdentity(final cmsMAT3 a)
	{
		return cmsmtrx._cmsMAT3isIdentity(a);
	}
	
	/**
	 * Multiply two matrices.
	 * @param r a pointer to a cmsMAT3 object to receive the result.
	 * @param a A pointer to first cmsMAT3 object.
	 * @param b A pointer to second cmsMAT3 object.
	 */
	public static void _cmsMAT3per(cmsMAT3 r, final cmsMAT3 a, final cmsMAT3 b)
	{
		cmsmtrx._cmsMAT3per(r, a, b);
	}
	
	/**
	 * Inverse of a matrix<i>b = a<sup>-1</sup></i>. Returns false if singular matrix
	 * @param a A pointer to the cmsMAT3 to be inverted.
	 * @param b A pointer to a cmsMAT3 object to store the result.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean _cmsMAT3inverse(final cmsMAT3 a, cmsMAT3 b)
	{
		return cmsmtrx._cmsMAT3inverse(a, b);
	}
	
	/**
	 * Solves a system in the form <i>Ax = b</i>. Returns FALSE if singular matrix
	 * @param x a pointer to a cmsVEC3 object to receive the result.
	 * @param a A pointer to first cmsVEC3 object.
	 * @param b A pointer to second cmsVEC3 object.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean _cmsMAT3solve(cmsVEC3 x, cmsMAT3 a, cmsVEC3 b)
	{
		return cmsmtrx._cmsMAT3solve(x, a, b);
	}
	
	/**
	 * Evaluates a matrix and stores the result in “r”.
	 * @param r a pointer to a cmsVEC3 object to receive the result.
	 * @param a A pointer to the cmsMAT3 object containing the transformation matrix.
	 * @param v a pointer to a cmsVEC3 object to be evaluated.
	 */
	public static void _cmsMAT3eval(cmsVEC3 r, final cmsMAT3 a, final cmsVEC3 v)
	{
		cmsmtrx._cmsMAT3eval(r, a, v);
	}
	
	// Error logging  -------------------------------------------------------------------------------------
	
	/**
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param ErrorCode Error family (cmsERROR_*)
	 * @param ErrorText Error description, printf-like
	 * @param args additional printf-like parameters.
	 */
	public static void cmsSignalError(cmsContext ContextID, int ErrorCode, final String ErrorText, Object[] args)
	{
		cmserr.cmsSignalError(ContextID, ErrorCode, ErrorText, args);
	}
	
	// Memory management ----------------------------------------------------------------------------------
	
	/* 
	 * These functions are pretty much pointless since the JVM manages memory, leave these in for now but see later if they should be removed or not. The only reason for
	 * "not" is that it could offer a way to do low-level memory management (say with LowMemoryManager) or statistics and profiling. Not sure how but it could work.
	 * 
	 * Maybe change the functions to be as follows:
	 * _cmsMalloc(cmsContext, int) --> _cmsMalloc(cmsContext, Object)
	 * _cmsMallocZero(cmsContext, int) --> _cmsMallocZero(cmsContext, Object)
	 * _cmsCalloc(cmsContext, int, int) --> _cmsCalloc(cmsContext, Object) //where Object would be an array, if it was primitives then Object[] would throw a cast exception
	 * _cmsRealloc(cmsContext, Object, int) --> _cmsRealloc(cmsContext, Object, Object) //The first is the old, the second is the new
	 * //_cmsFree doesn't need to be changed but _cmsDupMem would be nearly impossible to create unless the function had a duplication/clone function
	 * 
	 * Maybe work around this by using "VirtualPointer", it acts like a "void*" but with "VirtualPointer.Serializer"s any data type can be inside it
	 */
	/**
	 * Allocate size bytes of uninitialized memory.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param size amount of memory to allocate in bytes
	 * @return Pointer to newly allocated block, or NULL on error.
	 */
	public static VirtualPointer _cmsMalloc(cmsContext ContextID, int size)
	{
		return cmserr._cmsMalloc(ContextID, size);
	}
	
	/**
	 * Allocate size bytes of memory. Initialize it to zero.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param size amount of memory to allocate in bytes
	 * @return Pointer to newly allocated block, or NULL on error.
	 */
	public static VirtualPointer _cmsMallocZero(cmsContext ContextID, int size)
	{
		return cmserr._cmsMallocZero(ContextID, size);
	}
	
	/**
	 * Allocate space for an array of num elements each of whose size in bytes is size. The space shall be initialized to all bits 0.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param num number of array elements
	 * @param size Array element size in bytes
	 * @return Pointer to newly allocated block, or NULL on error.
	 */
	public static VirtualPointer _cmsCalloc(cmsContext ContextID, int num, int size)
	{
		return cmserr._cmsCalloc(ContextID, num, size);
	}
	
	/**
	 * The size of the memory block pointed to by the Ptr parameter is changed to the NewSize bytes, expanding or reducing the amount of memory available in the block.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param Ptr pointer to memory block.
	 * @param NewSize number of bytes.
	 * @return Pointer to newly allocated block, or NULL on error.
	 */
	public static VirtualPointer _cmsRealloc(cmsContext ContextID, VirtualPointer Ptr, int NewSize)
	{
		return cmserr._cmsRealloc(ContextID, Ptr, NewSize);
	}
	
	/**
	 * Cause the space pointed to by Ptr to be deallocated; that is, made available for further allocation. If ptr is a null pointer, no action will occur.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param Ptr pointer to memory block.
	 */
	public static void _cmsFree(cmsContext ContextID, VirtualPointer Ptr)
	{
		cmserr._cmsFree(ContextID, Ptr);
	}
	
	/**
	 * Duplicates the contents of memory at “Org” into a new block
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param Org pointer to source memory block.
	 * @param size number of bytes to duplicate.
	 * @return Pointer to newly allocated copy, or NULL on error.
	 */
	public static VirtualPointer _cmsDupMem(cmsContext ContextID, final VirtualPointer Org, int size)
	{
		return cmserr._cmsDupMem(ContextID, Org, size);
	}
	
	// I/O handler ----------------------------------------------------------------------------------
	
	public static class _cms_io_handler
	{
		public static final int SIZE = 4 + cmsMAX_PATH; //Only UsedSpace and PhysicalFile are really serializable
		
		public Object stream; // Associated stream, which is implemented differently depending on media.
		
		public cmsContext ContextID;
		public int UsedSpace;
		public int ReportedSize;
		public StringBuffer PhysicalFile;
	    
	    public _ioRead Read;
	    public _ioSeek Seek;
	    public _ioClose Close;
	    public _ioTell Tell;
	    public _ioWrite Write;
	    
	    public static interface _ioRead
	    {
	    	public int run(_cms_io_handler iohandler, byte[] Buffer, int size, int count);
	    }
	    
	    public static interface _ioSeek
	    {
	    	public boolean run(_cms_io_handler iohandler, int offset);
	    }
	    
	    public static interface _ioClose
	    {
	    	public boolean run(_cms_io_handler iohandler);
	    }
	    
	    public static interface _ioTell
	    {
	    	public int run(_cms_io_handler iohandler);
	    }
	    
	    public static interface _ioWrite
	    {
	    	public boolean run(_cms_io_handler iohandler, int size, final byte[] Buffer);
	    }
	    
	    //Simple read/write helper classes so VirtualPointer can be used as buffers
	    int vpRead(_cms_io_handler iohandler, VirtualPointer Buffer, int size, int count)
	    {
	    	if(Read != null)
	    	{
	    		byte[] temp = new byte[size * count];
	    		int out = Read.run(iohandler, temp, size, count);
	    		Buffer.getProcessor().write(temp);
	    		return out;
	    	}
	    	throw new NullPointerException();
	    }
	    
	    boolean vpWrite(_cms_io_handler iohandler, int size, final VirtualPointer Buffer)
	    {
	    	if(Write != null)
	    	{
	    		byte[] temp = new byte[size];
	    		if(Buffer.readByteArray(temp, 0, size, false, false) != size)
	    		{
	    			return false;
	    		}
	    		return Write.run(iohandler, size, temp);
	    	}
	    	throw new NullPointerException();
	    }
	    
	    public _cms_io_handler()
	    {
	    	PhysicalFile = new StringBuffer(cmsMAX_PATH);
	    }
	}
	
	// Endianess adjust functions
	public static short _cmsAdjustEndianess16(short Word)
	{
		return cmsplugin._cmsAdjustEndianess16(Word);
	}
	
	public static int _cmsAdjustEndianess32(int Value)
	{
		return cmsplugin._cmsAdjustEndianess32(Value);
	}
	
	public static void _cmsAdjustEndianess64(long[] Result, long QWord)
	{
		cmsplugin._cmsAdjustEndianess64(Result, QWord);
	}
	
	// Helper IO functions
	/**
	 * Read data type from the given IOHANDLER.
	 * @param io pointer to the cmsIOHANDLER object.
	 * @param n Pointer to an object to receive the result.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean _cmsReadUInt8Number(cmsIOHANDLER io, byte[] n)
	{
		return cmsplugin._cmsReadUInt8Number(io, n);
	}
	
	/**
	 * Read data type from the given IOHANDLER.
	 * @param io pointer to the cmsIOHANDLER object.
	 * @param n Pointer to an object to receive the result.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean _cmsReadUInt16Number(cmsIOHANDLER io, short[] n)
	{
		return cmsplugin._cmsReadUInt16Number(io, n);
	}
	
	/**
	 * Read data type from the given IOHANDLER.
	 * @param io pointer to the cmsIOHANDLER object.
	 * @param n Pointer to an object to receive the result.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean _cmsReadUInt32Number(cmsIOHANDLER io, int[] n)
	{
		return cmsplugin._cmsReadUInt32Number(io, n);
	}
	
	/**
	 * Read data type from the given IOHANDLER.
	 * @param io pointer to the cmsIOHANDLER object.
	 * @param n Pointer to an object to receive the result.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean _cmsReadFloat32Number(cmsIOHANDLER io, float[] n)
	{
		return cmsplugin._cmsReadFloat32Number(io, n);
	}
	
	/**
	 * Read data type from the given IOHANDLER.
	 * @param io pointer to the cmsIOHANDLER object.
	 * @param n Pointer to an object to receive the result.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean _cmsReadUInt64Number(cmsIOHANDLER io, long[] n)
	{
		return cmsplugin._cmsReadUInt64Number(io, n);
	}
	
	/**
	 * Read data type from the given IOHANDLER.
	 * @param io pointer to the cmsIOHANDLER object.
	 * @param n Pointer to an object to receive the result.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean _cmsRead15Fixed16Number(cmsIOHANDLER io, double[] n)
	{
		return cmsplugin._cmsRead15Fixed16Number(io, n);
	}
	
	/**
	 * Read data type from the given IOHANDLER.
	 * @param io pointer to the cmsIOHANDLER object.
	 * @param XYZ Pointer to an object to receive the result.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean _cmsReadXYZNumber(cmsIOHANDLER io, cmsCIEXYZ XYZ)
	{
		return cmsplugin._cmsReadXYZNumber(io, XYZ);
	}
	
	/**
	 * Read data type from the given IOHANDLER.
	 * @param io pointer to the cmsIOHANDLER object.
	 * @param n Length of Array
	 * @param Array Pointer to an object to receive the result.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean _cmsReadUInt16Array(cmsIOHANDLER io, int n, short[] Array)
	{
		//Use "false" since it isn't expected that a char will be read using this function. (All built in functions that use this use true for a char, false for everything else)
		return cmsplugin._cmsReadUInt16Array(io, n, Array, false);
	}
	
	/**
	 * Write data type to the given IOHANDLER.
	 * @param io pointer to the cmsIOHANDLER object.
	 * @param n Object to write.
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean _cmsWriteUInt8Number(cmsIOHANDLER io, byte n)
	{
		return cmsplugin._cmsWriteUInt8Number(io, n);
	}
	
	/**
	 * Write data type to the given IOHANDLER.
	 * @param io pointer to the cmsIOHANDLER object.
	 * @param n Object to write.
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean _cmsWriteUInt16Number(cmsIOHANDLER io, short n)
	{
		return cmsplugin._cmsWriteUInt16Number(io, n);
	}
	
	/**
	 * Write data type to the given IOHANDLER.
	 * @param io pointer to the cmsIOHANDLER object.
	 * @param n Object to write.
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean _cmsWriteUInt32Number(cmsIOHANDLER io, int n)
	{
		return cmsplugin._cmsWriteUInt32Number(io, n);
	}
	
	/**
	 * Write data type to the given IOHANDLER.
	 * @param io pointer to the cmsIOHANDLER object.
	 * @param n Object to write.
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean _cmsWriteFloat32Number(cmsIOHANDLER io, float n)
	{
		return cmsplugin._cmsWriteFloat32Number(io, n);
	}
	
	/**
	 * Write data type to the given IOHANDLER.
	 * @param io pointer to the cmsIOHANDLER object.
	 * @param n Object to write.
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean _cmsWriteUInt64Number(cmsIOHANDLER io, long n)
	{
		return cmsplugin._cmsWriteUInt64Number(io, n);
	}
	
	/**
	 * Write data type to the given IOHANDLER.
	 * @param io pointer to the cmsIOHANDLER object.
	 * @param n Object to write.
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean _cmsWrite15Fixed16Number(cmsIOHANDLER io, double n)
	{
		return cmsplugin._cmsWrite15Fixed16Number(io, n);
	}
	
	/**
	 * Write data type to the given IOHANDLER.
	 * @param io pointer to the cmsIOHANDLER object.
	 * @param XYZ Object to write.
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean _cmsWriteXYZNumber(cmsIOHANDLER io, final cmsCIEXYZ XYZ)
	{
		return cmsplugin._cmsWriteXYZNumber(io, XYZ);
	}
	
	/**
	 * Write data type to the given IOHANDLER.
	 * @param io pointer to the cmsIOHANDLER object.
	 * @param n Length of array to write
	 * @param Array Object to write.
	 * @return TRUE on success, FALSE on error
	 */
	public static boolean _cmsWriteUInt16Array(cmsIOHANDLER io, int n, final short[] Array)
	{
		//Use "false" since it isn't expected that a char will be written using this function. (All built in functions that use this use true for a char, false for everything else)
		return cmsplugin._cmsWriteUInt16Array(io, n, Array, false);
	}
	
	// ICC base tag
	public static class _cmsTagBase
	{
		public static final int SIZE = 4 + 4;
		
		public int sig;
		public byte[] reserved;
		
		public _cmsTagBase()
		{
			reserved = new byte[4];
		}
	}
	
	// Type base helper functions
	/**
	 * Reads a tag type signature from the given IOHANDLER.
	 * @param io pointer to an cmsIOHANDLER object.
	 * @return tag type signature or 0 on error
	 */
	public static int _cmsReadTypeBase(cmsIOHANDLER io)
	{
		return cmsplugin._cmsReadTypeBase(io);
	}
	
	/**
	 * Writes s a tag type signature to the given IOHANDLER.
	 * @param io pointer to an cmsIOHANDLER object.
	 * @param sig tag type signature to be written.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean _cmsWriteTypeBase(cmsIOHANDLER io, int sig)
	{
		return cmsplugin._cmsWriteTypeBase(io, sig);
	}
	
	// Alignment functions
	/**
	 * Skips bytes on the given IOHANDLER until a 32-bit aligned position.
	 * @param io pointer to a cmsIOHANDLER object.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean _cmsReadAlignment(cmsIOHANDLER io)
	{
		return cmsplugin._cmsReadAlignment(io);
	}
	
	/**
	 * Writes zeros on the given IOHANDLER until a 32-bit aligned position.
	 * @param io pointer to a cmsIOHANDLER object.
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean _cmsWriteAlignment(cmsIOHANDLER io)
	{
		return cmsplugin._cmsWriteAlignment(io);
	}
	
	// To deal with text streams. 2K at most
	/**
	 * Outputs printf-like strings to the given IOHANDLER. To deal with text streams. 2K at most
	 * @param io pointer to cmsIOHANDLER object.
	 * @param frm format string (printf-like)
	 * @param args optional parameters (printf-like)
	 * @return TRUE on success, FALSE on error.
	 */
	public static boolean _cmsIOPrintf(cmsIOHANDLER io, final String frm, Object[] args)
	{
		return cmsplugin._cmsIOPrintf(io, frm, args);
	}
	
	// Fixed point helper functions
	/**
	 * Converts from 8.8 fixed point to double.
	 * @param fixed8 8.8 encoded fixed point value.
	 * @return double holding the value.
	 */
	public static double _cms8Fixed8toDouble(short fixed8)
	{
		return cmsplugin._cms8Fixed8toDouble(fixed8);
	}
	
	/**
	 * Converts from double to 8.8 fixed point, rounding properly.
	 * @param val double holding the value.
	 * @return 8.8 encoded fixed point value.
	 */
	public static short _cmsDoubleTo8Fixed8(double val)
	{
		return cmsplugin._cmsDoubleTo8Fixed8(val);
	}
	
	/**
	 * Converts from 15.16 (signed) fixed point to double.
	 * @param fix32 15.16 (signed) fixed point encoded fixed point value.
	 * @return double holding the value.
	 */
	public static double _cms15Fixed16toDouble(int fix32)
	{
		return cmsplugin._cms15Fixed16toDouble(fix32);
	}
	
	/**
	 * Converts from double to 15.16 fixed point, rounding properly.
	 * @param v double holding the value.
	 * @return 15.16 (signed) fixed point encoded fixed point value.
	 */
	public static int _cmsDoubleTo15Fixed16(double v)
	{
		return cmsplugin._cmsDoubleTo15Fixed16(v);
	}
	
	// Date/time helper functions
	/**
	 * Decodes from the standard Calendar to ICC date and time format.
	 * @param Dest a pointer to a cmsDateTimeNumber structure.
	 * @param Source a pointer to a Calendar structure.
	 */
	public static void _cmsEncodeDateTimeNumber(cmsDateTimeNumber Dest, final Calendar Source)
	{
		cmsplugin._cmsEncodeDateTimeNumber(Dest, Source);
	}
	
	/**
	 * Decodes from ICC date and time format to the standard Calendar.
	 * @param Source a pointer to a cmsDateTimeNumber structure.
	 * @param Dest a pointer to a Calendar structure.
	 */
	public static void _cmsDecodeDateTimeNumber(final cmsDateTimeNumber Source, Calendar Dest)
	{
		cmsplugin._cmsDecodeDateTimeNumber(Source, Dest);
	}
	
	//----------------------------------------------------------------------------------------------------------

	// Plug-in foundation
	public static final int cmsPluginMagicNumber               = 0x61637070; // 'acpp'
	
	public static final int cmsPluginMemHandlerSig             = 0x6D656D48; // 'memH'
	public static final int cmsPluginInterpolationSig          = 0x696E7048; // 'inpH'
	public static final int cmsPluginParametricCurveSig        = 0x70617248; // 'parH'
	public static final int cmsPluginFormattersSig             = 0x66726D48; // 'frmH
	public static final int cmsPluginTagTypeSig                = 0x74797048; // 'typH'
	public static final int cmsPluginTagSig                    = 0x74616748; // 'tagH'
	public static final int cmsPluginRenderingIntentSig        = 0x696E7448; // 'intH'
	public static final int cmsPluginMultiProcessElementSig    = 0x6D706548; // 'mpeH'
	public static final int cmsPluginOptimizationSig           = 0x6F707448; // 'optH'
	public static final int cmsPluginTransformSig              = 0x7A666D48; // 'xfmH'
	
	public static class cmsPluginBase
	{
		/** 'acpp' signature*/
		public int Magic;
		/** Expected version of LittleCMS*/
		public int ExpectedVersion;
		/** Type of plug-in*/
		public int Type;
		/** For multiple plugin definition. NULL for end of list.*/
		public cmsPluginBase Next;
	}
	
	// Maximum number of types in a plugin array
	public static final int MAX_TYPES_IN_LCMS_PLUGIN = 20;
	
	//----------------------------------------------------------------------------------------------------------
	
	// Memory handler. Each new plug-in type replaces current behavior
	public static class cmsPluginMemHandler extends cmsPluginBase
	{
		public static interface pluginMallocPtr
		{
			public VirtualPointer run(cmsContext ContextID, int size);
		}
		
		public static interface pluginFreePtr
		{
			public void run(cmsContext ContextID, VirtualPointer Ptr);
		}
		
		public static interface pluginReallocPtr
		{
			public VirtualPointer run(cmsContext ContextID, VirtualPointer Ptr, int NewSize);
		}
		
		public static interface pluginMallocZeroPtr
		{
			public VirtualPointer run(cmsContext ContextID, int size);
		}
		
		public static interface pluginCallocPtr
		{
			public VirtualPointer run(cmsContext ContextID, int num, int size);
		}
		
		public static interface pluginDupPtr
		{
			public VirtualPointer run(cmsContext ContextID, final VirtualPointer Org, int size);
		}
		
        // Required
        public pluginMallocPtr MallocPtr;
        public pluginFreePtr FreePtr;
        public pluginReallocPtr ReallocPtr;
        
        // Optional
        public pluginMallocZeroPtr MallocZeroPtr;
        public pluginCallocPtr CallocPtr;
        public pluginDupPtr DupPtr;
	}
	
	// ------------------------------------------------------------------------------------------------------------------
	
	// Interpolation callbacks
	
	// 16 bits forward interpolation. This function performs precision-limited linear interpolation
	// and is supposed to be quite fast. Implementation may be tetrahedral or trilinear, and plug-ins may
	// choose to implement any other interpolation algorithm.
	public static interface _cmsInterpFn16
	{
		public void run(final short[] Input, short[] Output, final cmsInterpParams p);
	}
	
	// Floating point forward interpolation. Full precision interpolation using floats. This is not a
	// time critical function. Implementation may be tetrahedral or trilinear, and plug-ins may
	// choose to implement any other interpolation algorithm.
	public static interface _cmsInterpFnFloat
	{
		public void run(final float[] Input, float[] Output, final cmsInterpParams p);
	}
	
	// This type holds a pointer to an interpolator that can be either 16 bits or float
	public static class cmsInterpFunction
	{
		private Object lerp;
		
		public cmsInterpFunction()
		{
			this(null);
		}
		
		public cmsInterpFunction(Object lerp)
		{
			this.lerp = lerp;
		}
		
		/** Forward interpolation in 16 bits*/
		public _cmsInterpFn16 get16()
		{
			return (_cmsInterpFn16)this.lerp;
		}
		
		/** Forward interpolation in floating point*/
		public _cmsInterpFnFloat getFloat()
		{
			return (_cmsInterpFnFloat)this.lerp;
		}
		
		public void set(Object lerp)
		{
			this.lerp = lerp;
		}
		
		public boolean hasValue()
		{
			return this.lerp != null;
		}
	}
	
	// Flags for interpolator selection
	/** The default*/
	public static final int CMS_LERP_FLAGS_16BITS = 0x0000;
	/** Requires different implementation*/
	public static final int CMS_LERP_FLAGS_FLOAT = 0x0001;
	/** Hint only*/
	public static final int CMS_LERP_FLAGS_TRILINEAR = 0x0100;
	
	public static final int MAX_INPUT_DIMENSIONS = 8;
	
	// Interpolation. 16 bits and floating point versions.
	public static class cmsInterpParams
	{
		/** The calling thread*/
		public cmsContext ContextID;
		
		/** Keep original flags*/
		public int dwFlags;
		/** != 1 only in 3D interpolation*/
		public int nInputs;
		/** != 1 only in 3D interpolation*/
		public int nOutputs;
	    
		/** Valid on all kinds of tables*/
		public int[] nSamples;
		/** Domain = nSamples - 1*/
		public int[] Domain;
	    
		/**
		 * Optimization for 3D CLUT. This is the number of nodes premultiplied for each dimension. For example, in 7 nodes, 7, 7^2 , 7^3, 7^4, etc. On non-regular
		 * Samplings may vary according of the number of nodes for each dimension.
		 */
		public int[] opta;
	    
		/** Points to the actual interpolation table*/
		public Object Table;
		/** Points to the function to do the interpolation*/
		public cmsInterpFunction Interpolation;
		
		public cmsInterpParams()
		{
			nSamples = new int[MAX_INPUT_DIMENSIONS];
			Domain = new int[MAX_INPUT_DIMENSIONS];
			opta = new int[MAX_INPUT_DIMENSIONS];
		}
	}
	
	// Interpolators factory
	public static interface cmsInterpFnFactory
	{
		public cmsInterpFunction run(int nInputChannels, int nOutputChannels, int dwFlags);
	}
	
	// The plug-in
	public static class cmsPluginInterpolation extends cmsPluginBase
	{
	    /** Points to a user-supplied function which implements the factory*/
		public cmsInterpFnFactory InterpolatorsFactory;
	}
	
	//----------------------------------------------------------------------------------------------------------
	
	// Parametric curves. A negative type means same function but analytically inverted. Max. number of params is 10
	
	// Evaluator callback for user-suplied parametric curves. May implement more than one type
	public static interface cmsParametricCurveEvaluator
	{
		public double run(int Type, final double[/*should be 10*/] Params, double R);
	}
	
	// Plug-in may implement an arbitrary number of parametric curves
	public static class cmsPluginParametricCurves extends cmsPluginBase
	{
		/** Number of supported functions*/
		public int nFunctions;
		/** The identification types*/
		public int[] FunctionTypes;
		/** Number of parameters for each function*/
		public int[] ParameterCount;
	    
		/** The evaluator*/
		public cmsParametricCurveEvaluator Evaluator;
		
		public cmsPluginParametricCurves()
		{
			FunctionTypes = new int[MAX_TYPES_IN_LCMS_PLUGIN];
			ParameterCount = new int[MAX_TYPES_IN_LCMS_PLUGIN];
		}
	}
	
	//----------------------------------------------------------------------------------------------------------

	// Formatters. This plug-in adds new handlers, replacing them if they already exist. Formatters dealing with
	// cmsFloat32Number (bps = 4) or double (bps = 0) types are requested via FormatterFloat callback. Others come across
	// Formatter16 callback
	
	//_cmsTRANSFORM //XXX This should be replaced somehow. It's not public yet needs to exist somehow for extensibility purposes
	
	public static interface cmsFormatter16
	{
		public VirtualPointer run(_cmsTRANSFORM CMMcargo, short[] Values, VirtualPointer Buffer, int Stride);
	}
	
	public static interface cmsFormatterFloat
	{
		public VirtualPointer run(_cmsTRANSFORM CMMcargo, float[] Values, VirtualPointer Buffer, int Stride);
	}
	
	// This type holds a pointer to a formatter that can be either 16 bits or cmsFloat32Number
	public static class cmsFormatter
	{
		private Object fmt;
		
		public cmsFormatter()
		{
			this(null);
		}
		
		public cmsFormatter(Object fmt)
		{
			this.fmt = fmt;
		}
		
		public cmsFormatter16 get16()
		{
			return (cmsFormatter16)this.fmt;
		}
		
		public cmsFormatterFloat getFloat()
		{
			return (cmsFormatterFloat)this.fmt;
		}
		
		public void set(Object fmt)
		{
			this.fmt = fmt;
		}
		
		public boolean hasValue()
		{
			return this.fmt != null;
		}
	}
	
	public static final int CMS_PACK_FLAGS_16BITS = 0x0000;
	public static final int CMS_PACK_FLAGS_FLOAT = 0x0001;
	
	public static final int cmsFormatterInput = 0;
	public static final int cmsFormatterOutput = 1;
	
	public static interface cmsFormatterFactory
	{
		/**
		 * @param Type Specific type, i.e. TYPE_RGB_8
		 * @param Dir cmsFormatterInput or cmsFormatterOutput
		 * @param dwFlags precision
		 */
		public cmsFormatter run(int Type, int Dir, int dwFlags);
	}
	
	// Plug-in may implement an arbitrary number of formatters
	public static class cmsPluginFormatters extends cmsPluginBase
	{
		public cmsFormatterFactory FormattersFactory;
	}
	
	//----------------------------------------------------------------------------------------------------------
	
	// Tag type handler. Each type is free to return anything it wants, and it is up to the caller to
	// know in advance what is the type contained in the tag.
	public static class cmsTagTypeHandler
	{
		public static final int SIZE = 4;
		
		/** The signature of the type*/
		public int Signature;
		
        // Allocates and reads items
		public static interface tagHandlerReadPtr
        {
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag);
        }
        
        // Writes n Items
		public static interface tagHandlerWritePtr
        {
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems);
        }
        
        // Duplicate an item or array of items
		public static interface tagHandlerDupPtr
        {
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n);
        }
        
        // Free all resources
		public static interface tagHandlerFreePtr
        {
			public void run(cmsTagTypeHandler self, Object Ptr);
        }
        
        public tagHandlerReadPtr ReadPtr;
        public tagHandlerWritePtr WritePtr;
        public tagHandlerDupPtr DupPtr;
        public tagHandlerFreePtr FreePtr;
        
        // Additional parameters used by the calling thread
        public cmsContext ContextID;
        public int ICCVersion;
        
        public cmsTagTypeHandler(int sig, tagHandlerReadPtr read, tagHandlerWritePtr write, tagHandlerDupPtr dup, tagHandlerFreePtr free)
        {
        	this.Signature = sig;
        	this.ReadPtr = read;
        	this.WritePtr = write;
        	this.DupPtr = dup;
        	this.FreePtr = free;
        	this.ContextID = null;
        	this.ICCVersion = 0;
        }
	}
	
	// Each plug-in implements a single type
	public static class cmsPluginTagType extends cmsPluginBase
	{
		public cmsTagTypeHandler Handler;
	}
	
	//----------------------------------------------------------------------------------------------------------
	
	// This is the tag plugin, which identifies tags. For writing, a pointer to function is provided.
	// This function should return the desired type for this tag, given the version of profile
	// and the data being serialized.
	public static class cmsTagDescriptor
	{
		/** If this tag needs an array, how many elements should keep*/
		public int ElemCount;

	    // For reading.
		/** In how many types this tag can come (MAX_TYPES_IN_LCMS_PLUGIN maximum)*/
		public int nSupportedTypes;
	    public int[] SupportedTypes;
	    
	    // For writing
	    public static interface tagDesDecideType
	    {
	    	public int run(double ICCVersion, final Object Data);
	    }
	    
	    public tagDesDecideType DecideType;
	    
	    public cmsTagDescriptor()
	    {
	    	SupportedTypes = new int[MAX_TYPES_IN_LCMS_PLUGIN];
	    }
	    
	    public cmsTagDescriptor(int elemCount, int typeCount, int[] supportedTypes, tagDesDecideType decideType)
	    {
	    	this();
	    	this.ElemCount = elemCount;
	    	this.nSupportedTypes = typeCount;
	    	System.arraycopy(supportedTypes, 0, this.SupportedTypes, 0, this.nSupportedTypes);
	    	this.DecideType = decideType;
	    }
	}
	
	// Plug-in implements a single tag
	public static class cmsPluginTag extends cmsPluginBase
	{
		public int Signature;
		public cmsTagDescriptor Descriptor;
	}
	
	//----------------------------------------------------------------------------------------------------------

	// Custom intents. This function should join all profiles specified in the array in
	// a single LUT. Any custom intent in the chain redirects to custom function. If more than
	// one custom intent is found, the one located first is invoked. Usually users should use only one
	// custom intent, so mixing custom intents in same multiprofile transform is not supported.
	
	public static interface cmsIntentFn
	{
		public cmsPipeline run(cmsContext ContextID, int nProfiles, int[] Intents, cmsHPROFILE[] hProfiles, boolean[] BPC, double[] AdaptationStates, int dwFlags);
	}
	
	// Each plug-in defines a single intent number.
	public static class cmsPluginRenderingIntent extends cmsPluginBase
	{
		public int Intent;
		public cmsIntentFn Link;
		public StringBuffer Description;
		
		public cmsPluginRenderingIntent()
		{
			Description = new StringBuffer(256);
		}
	}
	
	// The default ICC intents (perceptual, saturation, rel.col and abs.col)
	/**
	 * This function implements the standard ICC intents perceptual, relative colorimetric, saturation and absolute colorimetric. Can be used as a basis for custom intents.
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param nProfiles Number of profiles in the chain
	 * @param Intents Intent to apply on each profile to profile joint.
	 * @param hProfiles Handles to open profiles
	 * @param BPC Array of black point compensation states for each profile to profile joint
	 * @param AdaptationStates Array of observer adaptation states for each profile to profile joint.
	 * @param dwFlags color transform flags (prefix is cmsFLAGS_*).
	 * @return A pointer to a newly created pipeline holding the color transform on success, NULL on error.
	 */
	public static cmsPipeline _cmsDefaultICCintents(cmsContext ContextID, int nProfiles, int[] Intents, cmsHPROFILE[] hProfiles, boolean[] BPC, double[] AdaptationStates, 
			int dwFlags)
	{
		return cmscnvrt._cmsDefaultICCintents(ContextID, nProfiles, Intents, hProfiles, BPC, AdaptationStates, dwFlags);
	}
	
	//----------------------------------------------------------------------------------------------------------

	// Pipelines, Multi Process Elements.
	public static interface _cmsStageEvalFn
	{
		public void run(final float[] In, float[] Out, final cmsStage mpe);
	}
	public static interface _cmsStageDupElemFn
	{
		public Object run(cmsStage mpe);
	}
	public static interface _cmsStageFreeElemFn
	{
		public void run(cmsStage mpe);
	}
	
	// This function allocates a generic MPE
	/**
	 * @param ContextID Pointer to a user-defined context cargo.
	 * @param Type Identifier for the new stage type.
	 * @param InputChannels Number of channels for this stage.
	 * @param OutputChannels Number of channels for this stage.
	 * @param EvalPtr Callback to evaluate the stage. Points to fn that evaluates the element (always in floating point)
	 * @param DupElemPtr If user data is being used, callback to duplicate the data. Points to a fn that duplicates the stage
	 * @param FreePtr If user data is being used, callback to set data free. Points to a fn that sets the element free
	 * @param Data Pointer to user-defined data or NULL if no data is needed. A generic pointer to whatever memory needed by the element
	 * @return A pointer to the newly created stage on success, NULL on error.
	 */
	public static cmsStage _cmsStageAllocPlaceholder(cmsContext ContextID, int Type, int InputChannels, int OutputChannels, _cmsStageEvalFn EvalPtr, 
			_cmsStageDupElemFn DupElemPtr, _cmsStageFreeElemFn FreePtr, Object Data)
	{
		return cmslut._cmsStageAllocPlaceholder(ContextID, Type, InputChannels, OutputChannels, EvalPtr, DupElemPtr, FreePtr, Data);
	}
	
	public static class cmsPluginMultiProcessElement extends cmsPluginBase
	{
		public cmsTagTypeHandler Handler;
	}
	
	// Data kept in "Element" member of cmsStage
	
	// Curves
	public static class _cmsStageToneCurvesData
	{
		public int nCurves;
		public cmsToneCurve[] TheCurves;
	}
	
	// Matrix
	public static class _cmsStageMatrixData
	{
		/** floating point for the matrix*/
		public double[] Double;
		/** The offset*/
		public double[] Offset;
	}
	
	// CLUT
	public static class _cmsStageCLutData
	{
		/**
		 * Can have only one of both representations at same time
		 * <p>
		 * Points to the table 16 bits table
		 * <p>
		 * Points to the float table
		 */
		public VirtualPointer Tab;
	    
	    public cmsInterpParams Params;
	    public int nEntries;
	    public boolean HasFloatValues;
	}
	
	//----------------------------------------------------------------------------------------------------------
	// Optimization. Using this plug-in, additional optimization strategies may be implemented.
	// The function should return TRUE if any optimization is done on the LUT, this terminates
	// the optimization  search. Or FALSE if it is unable to optimize and want to give a chance
	// to the rest of optimizers.
	
	public static interface _cmsOPTeval16Fn
	{
		public void run(final short[] In, short[] Out, final Object Data);
	}
	
	public static interface _cmsOPTfreeDataFn
	{
		public void run(cmsContext ContextID, Object Data);
	}
	
	public static interface _cmsOPTdupDataFn
	{
		public Object run(cmsContext ContextID, final Object Data);
	}
	
	public static interface _cmsOPToptimizeFn
	{
		public boolean run(cmsPipeline[] Lut, int Intent, int[] InputFormat, int[] OutputFormat, int[] dwFlags);
	}
	
	// This function may be used to set the optional evaluator and a block of private data. If private data is being used, an optional
	// duplicator and free functions should also be specified in order to duplicate the LUT construct. Use NULL to inhibit such functionality.
	
	public static void _cmsPipelineSetOptimizationParameters(cmsPipeline Lut, _cmsOPTeval16Fn Eval16, Object PrivateData, _cmsOPTfreeDataFn FreePrivateDataFn, 
			_cmsOPTdupDataFn DupPrivateDataFn)
	{
		cmslut._cmsPipelineSetOptimizationParameters(Lut, Eval16, PrivateData, FreePrivateDataFn, DupPrivateDataFn);
	}
	
	public static class cmsPluginOptimization extends cmsPluginBase
	{
		// Optimize entry point
		public _cmsOPToptimizeFn OptimizePtr;
	}
	
	//----------------------------------------------------------------------------------------------------------
	// Full xform
	public static interface _cmsTransformFn
	{
		public void run(_cmsTRANSFORM CMMcargo, final VirtualPointer InputBuffer, VirtualPointer OutputBuffer, int Size, int Stride);
	}
	
	public static interface _cmsTranformFactory
	{
		public boolean run(_cmsTransformFn xform, Object UserData, _cmsOPTfreeDataFn FreeUserData, cmsPipeline[] Lut, int[] InputFormat, int[] OutputFormat, int[] dwFlags);
	}
	
	// Retrieve user data as specified by the factory
	public static Object _cmsGetTransformUserData(_cmsTRANSFORM CMMcargo)
	{
		return cmsxform._cmsGetTransformUserData(CMMcargo);
	}
	
	// FIXME: Those are hacks that should be solved somehow.
	public static Object _cmsOPTgetTransformPipelinePrivateData(_cmsTRANSFORM CMMcargo)
	{
		throw new UnsupportedOperationException();
	}
	
	public static class cmsPluginTransform extends cmsPluginBase
	{
		// Transform entry point
		public _cmsTranformFactory Factory;
	}
}
