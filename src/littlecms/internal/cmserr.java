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

import littlecms.internal.helper.Stream;
import littlecms.internal.helper.TextFormatting;
import littlecms.internal.helper.VirtualPointer;
import littlecms.internal.lcms2.cmsContext;
import littlecms.internal.lcms2.cmsLogErrorHandlerFunction;
import littlecms.internal.lcms2_internal._cmsSubAllocator;
import littlecms.internal.lcms2_internal._cmsSubAllocator_chunk;
import littlecms.internal.lcms2_plugin.cmsPluginBase;
import littlecms.internal.lcms2_plugin.cmsPluginMemHandler;

class cmserr
{
	// I am so tired about incompatibilities on those functions that here are some replacements
	// that hopefully would be fully portable.
	
	// compare two strings ignoring case
	public static int cmsstrcasecmp(final String s1, final String s2)
	{
		int us1 = 0;
		int us2 = 0;
		final int s1len = s1.length();
		final int s2len = s2.length();
		
		while(Character.toUpperCase(s1.charAt(us1)) == Character.toUpperCase(s2.charAt(us2++)))
		{
			if(++us1 == s1len)
			{
				return 0;
			}
			if(us2 == s2len)
			{
				break;
			}
		}
		return Character.toUpperCase(s1.charAt(us1)) - Character.toUpperCase(s2.charAt(--us2));
	}
	
	// long int because C99 specifies ftell in such way (7.19.9.2)
	public static long cmsfilelength(Stream f)
	{
	    long n;
	    
		if (f.seek(0, Stream.SEEK_END) != 0)
		{		
			return -1;
		}
	    n = f.getPosition();
	    f.seek(0, Stream.SEEK_SET);
	    
	    return n;    
	}
	
	// Memory handling ------------------------------------------------------------------
	//
	// This is the interface to low-level memory management routines. By default a simple
	// wrapping to malloc/free/realloc is provided, although there is a limit on the max
	// amount of memory that can be reclaimed. This is mostly as a safety feature to 
	// prevent bogus or malintentionated code to allocate huge blocks that otherwise lcms
	// would never need.
	
	public static final int MAX_MEMORY_FOR_ALLOC_MB = 128;
	public static final int MAX_MEMORY_FOR_ALLOC = (1024 * 1024 * MAX_MEMORY_FOR_ALLOC_MB);
	
	// User may override this behaviour by using a memory plug-in, which basically replaces
	// the default memory management functions. In this case, no check is performed and it 
	// is up to the plug-in writter to keep in the safe side. There are only three functions 
	// required to be implemented: malloc, realloc and free, although the user may want to 
	// replace the optional mallocZero, calloc and dup as well.
	
	//--memory function is located father down in the source code.
	
	// *********************************************************************************
	
	// This is the default memory allocation function. It does a very coarse 
	// check of amout of memory, just to prevent exploits
	private static VirtualPointer _cmsMallocDefaultFn(cmsContext ContextID, int size)
	{
	    if (size > MAX_MEMORY_FOR_ALLOC || size < 0)
	    {
	    	return null; // Never allow over maximum
	    }
	    
	    return new VirtualPointer(size);
	}
	
	// Generic allocate & zero
	private static VirtualPointer _cmsMallocZeroDefaultFn(cmsContext ContextID, int size)
	{
		return _cmsMalloc(ContextID, size); //Becaus this is Java the allocated arrays are already zeroed out.
	}
	
	// The default free function. The only check performed is against NULL pointers
	private static void _cmsFreeDefaultFn(cmsContext ContextID, VirtualPointer Ptr)
	{
	    // free(NULL) is defined a no-op by C99, therefore it is safe to
	    // avoid the check, but it is here just in case...
		
	    if (Ptr != null)
	    {
	    	Ptr.free();
	    	Ptr = null;
	    }
	}
	
	// The default realloc function. Again it check for exploits. If Ptr is NULL, 
	// realloc behaves the same way as malloc and allocates a new block of size bytes. 
	private static VirtualPointer _cmsReallocDefaultFn(cmsContext ContextID, VirtualPointer Ptr, int size)
	{
	    if (size > MAX_MEMORY_FOR_ALLOC || size < 0)
	    {
	    	return null; // Never realloc over MAX_MEMORY_FOR_ALLOC_MB or below 0
	    }
	    
	    VirtualPointer nPtr = new VirtualPointer(size);
	    byte[] PtrData = Ptr.getBacking();
	    nPtr.writeRaw(PtrData, 0, Math.min(PtrData.length, size));
	    return nPtr;
	}
	
	// The default calloc function. Allocates an array of num elements, each one of size bytes
	// all memory is initialized to zero.
	private static VirtualPointer _cmsCallocDefaultFn(cmsContext ContextID, int num, int size)
	{
	    int Total = num * size;
	    
	    // Check for overflow
	    if (Total < num || Total < size)
	    {
	        return null;
	    }
	    
	    if (Total > MAX_MEMORY_FOR_ALLOC || Total < 0)
	    {
	    	return null; // Never alloc over MAX_MEMORY_FOR_ALLOC_MB or below 0
	    }
	    
	    return _cmsMallocZero(ContextID, Total);
	}
	
	// Generic block duplication
	private static VirtualPointer _cmsDupDefaultFn(cmsContext ContextID, final VirtualPointer Org, int size)
	{
		VirtualPointer mem;
	    
	    if (size > MAX_MEMORY_FOR_ALLOC || size < 0)
	    {
	    	return null; // Never dup over MAX_MEMORY_FOR_ALLOC_MB or below 0
	    }
	    
	    mem = _cmsMalloc(ContextID, size);
	    
	    if (mem != null && Org != null)
	    {
	    	byte[] PtrData = Org.getBacking();
	    	mem.writeRaw(PtrData, 0, Math.min(PtrData.length, size));
	    }
	    
	    return mem;
	}
	
	private static final cmsPluginMemHandler.pluginMallocPtr _cmsMallocDefaultImpl = new cmsPluginMemHandler.pluginMallocPtr()
	{
		public VirtualPointer run(cmsContext ContextID, int size)
		{
			return _cmsMallocDefaultFn(ContextID, size);
		}
	};
	private static final cmsPluginMemHandler.pluginMallocZeroPtr _cmsMallocZeroDefaultImpl = new cmsPluginMemHandler.pluginMallocZeroPtr()
	{
		public VirtualPointer run(cmsContext ContextID, int size)
		{
			return _cmsMallocZeroDefaultFn(ContextID, size);
		}
	};
	private static final cmsPluginMemHandler.pluginFreePtr _cmsFreeDefaultImpl = new cmsPluginMemHandler.pluginFreePtr()
	{
		public void run(cmsContext ContextID, VirtualPointer Ptr)
		{
			_cmsFreeDefaultFn(ContextID, Ptr);
		}
	};
	private static final cmsPluginMemHandler.pluginReallocPtr _cmsReallocDefaultImpl = new cmsPluginMemHandler.pluginReallocPtr()
	{
		public VirtualPointer run(cmsContext ContextID, VirtualPointer Ptr, int NewSize)
		{
			return _cmsReallocDefaultFn(ContextID, Ptr, NewSize);
		}
	};
	private static final cmsPluginMemHandler.pluginCallocPtr _cmsCallocDefaultImpl = new cmsPluginMemHandler.pluginCallocPtr()
	{
		public VirtualPointer run(cmsContext ContextID, int num, int size)
		{
			return _cmsCallocDefaultFn(ContextID, num, size);
		}
	};
	private static final cmsPluginMemHandler.pluginDupPtr _cmsDupDefaultImpl = new cmsPluginMemHandler.pluginDupPtr()
	{
		public VirtualPointer run(cmsContext ContextID, VirtualPointer Org, int size)
		{
			return _cmsDupDefaultFn(ContextID, Org, size);
		}
	};
	
	// Pointers to malloc and _cmsFree functions in current environment
	private static cmsPluginMemHandler.pluginMallocPtr MallocPtr = _cmsMallocDefaultImpl;
	private static cmsPluginMemHandler.pluginMallocZeroPtr MallocZeroPtr = _cmsMallocZeroDefaultImpl;
	private static cmsPluginMemHandler.pluginFreePtr FreePtr = _cmsFreeDefaultImpl;
	private static cmsPluginMemHandler.pluginReallocPtr ReallocPtr = _cmsReallocDefaultImpl; 
	private static cmsPluginMemHandler.pluginCallocPtr CallocPtr = _cmsCallocDefaultImpl;
	private static cmsPluginMemHandler.pluginDupPtr DupPtr = _cmsDupDefaultImpl;
	
	// Plug-in replacement entry
	public static boolean _cmsRegisterMemHandlerPlugin(cmsPluginBase Data)
	{
	    cmsPluginMemHandler Plugin = (cmsPluginMemHandler)Data;
	    
	    // NULL forces to reset to defaults
	    if (Data == null)
	    {
	        MallocPtr    = _cmsMallocDefaultImpl;
	        MallocZeroPtr= _cmsMallocZeroDefaultImpl;
	        FreePtr      = _cmsFreeDefaultImpl;
	        ReallocPtr   = _cmsReallocDefaultImpl; 
	        CallocPtr    = _cmsCallocDefaultImpl;
	        DupPtr       = _cmsDupDefaultImpl;
	        return true;
	    }
	    
		// Check for required callbacks
		if (Plugin.MallocPtr == null || Plugin.FreePtr == null || Plugin.ReallocPtr == null)
		{
			return false;
		}
		
	    // Set replacement functions
	    MallocPtr  = Plugin.MallocPtr;
	    FreePtr    = Plugin.FreePtr;
	    ReallocPtr = Plugin.ReallocPtr;
	    
	    if (Plugin.MallocZeroPtr != null)
	    {
	    	MallocZeroPtr = Plugin.MallocZeroPtr;
	    }
	    if (Plugin.CallocPtr != null)
	    {
	    	CallocPtr = Plugin.CallocPtr;
	    }
	    if (Plugin.DupPtr != null)
	    {
	    	DupPtr = Plugin.DupPtr;
	    }
	    
	    return true;
	}
	
	// Generic allocate
	public static VirtualPointer _cmsMalloc(cmsContext ContextID, int size)
	{
	    return MallocPtr.run(ContextID, size);
	}
	
	// Generic allocate & zero
	public static VirtualPointer _cmsMallocZero(cmsContext ContextID, int size)
	{
	    return MallocZeroPtr.run(ContextID, size);
	}
	
	// Generic calloc
	public static VirtualPointer _cmsCalloc(cmsContext ContextID, int num, int size)
	{
	    return CallocPtr.run(ContextID, num, size);
	}
	
	// Generic reallocate
	public static VirtualPointer _cmsRealloc(cmsContext ContextID, VirtualPointer Ptr, int size)
	{
	    return ReallocPtr.run(ContextID, Ptr, size);
	}
	
	// Generic free memory
	public static void _cmsFree(cmsContext ContextID, VirtualPointer Ptr)
	{
	    if (Ptr != null)
	    {
	    	FreePtr.run(ContextID, Ptr);
	    }
	}
	
	// Generic block duplication
	public static VirtualPointer _cmsDupMem(cmsContext ContextID, final VirtualPointer Org, int size)
	{
	    return DupPtr.run(ContextID, Org, size);
	}
	
	// ********************************************************************************************

	// Sub allocation takes care of many pointers of small size. The memory allocated in
	// this way have be freed at once. Next function allocates a single chunk for linked list
	// I prefer this method over realloc due to the big inpact on xput realloc may have if 
	// memory is being swapped to disk. This approach is safer (although thats not true on any platform)
	static _cmsSubAllocator_chunk _cmsCreateSubAllocChunk(cmsContext ContextID, int Initial)
	{
	    _cmsSubAllocator_chunk chunk;
	    VirtualPointer vp;
	    
	    // Create the container
	    chunk = (_cmsSubAllocator_chunk)(vp = _cmsMallocZero(ContextID, /*sizeof(_cmsSubAllocator_chunk)*/_cmsSubAllocator_chunk.SIZE)).getDeserializedType(_cmsSubAllocator_chunk.class);
	    if (chunk == null)
	    {
	    	return null;
	    }
	    
	    // Initialize values
	    chunk.Block = _cmsMalloc(ContextID, Initial);
	    if (chunk.Block == null)
	    {
	        // Something went wrong
	        _cmsFree(ContextID, vp);
	        return null;
	    }
	    
	    // 20K by default
	    if (Initial == 0)
	    {
	    	Initial = 20 * 1024;
	    }
	    
	    chunk.BlockSize = Initial;
	    chunk.Used      = 0;
	    chunk.next      = null;
	    
	    return chunk;
	}
	
	// The suballocated is nothing but a pointer to the first element in the list. We also keep
	// the thread ID in this structure.
	public static _cmsSubAllocator _cmsCreateSubAlloc(cmsContext ContextID, int Initial)
	{
	    _cmsSubAllocator sub;
	    VirtualPointer vp;
	    
	    // Create the container
	    sub = (_cmsSubAllocator)(vp = _cmsMallocZero(ContextID, /*sizeof(_cmsSubAllocator)*/_cmsSubAllocator.SIZE)).getDeserializedType(_cmsSubAllocator.class);
	    if (sub == null)
	    {
	    	return null;
	    }
	    
	    sub.ContextID = ContextID;
	    
	    sub.h = _cmsCreateSubAllocChunk(ContextID, Initial);
	    if (sub.h == null)
	    {
	        _cmsFree(ContextID, vp);
	        return null;
	    }
	    
	    return sub;
	}
	
	// Get rid of whole linked list
	public static void _cmsSubAllocDestroy(_cmsSubAllocator sub)
	{
	    _cmsSubAllocator_chunk chunk, n;
	    
	    for (chunk = sub.h; chunk != null; chunk = n)
	    {
	        n = chunk.next;
	        if (chunk.Block != null)
	        {
	        	_cmsFree(sub.ContextID, chunk.Block);
	        }
	        _cmsFree(sub.ContextID, VirtualPointer.wrap(chunk));
	    }
	    
	    // Free the header
	    _cmsFree(sub.ContextID, VirtualPointer.wrap(sub));
	}
	
	// Get a pointer to small memory block.
	public static VirtualPointer _cmsSubAlloc(_cmsSubAllocator sub, int size)
	{
	    int Free = sub.h.BlockSize - sub.h.Used;
	    VirtualPointer ptr;
	    
	    size = lcms2_internal._cmsALIGNLONG(size);
	    
	    // Check for memory. If there is no room, allocate a new chunk of double memory size.   
	    if (size > Free)
	    {
	        _cmsSubAllocator_chunk chunk;
	        int newSize;
	        
	        newSize = sub.h.BlockSize * 2;
	        if (newSize < size)
	        {
	        	newSize = size;
	        }
	        
	        chunk = _cmsCreateSubAllocChunk(sub.ContextID, newSize);
	        if (chunk == null)
	        {
	        	return null;
	        }
	        
	        // Link list
	        chunk.next = sub.h;
	        sub.h = chunk;
	    }
	    
	    ptr = new VirtualPointer(sub.h.Block, sub.h.Used);
	    sub.h.Used += size;
	    
	    return ptr;
	}
	
	// Error logging ******************************************************************
	
	// There is no error handling at all. When a funtion fails, it returns proper value.
	// For example, all create functions does return NULL on failure. Other return FALSE
	// It may be interesting, for the developer, to know why the function is failing.
	// for that reason, lcms2 does offer a logging function. This function does recive
	// a ENGLISH string with some clues on what is going wrong. You can show this 
	// info to the end user, or just create some sort of log.
	// The logging function should NOT terminate the program, as this obviously can leave
	// resources. It is the programmer's responsability to check each function return code
	// to make sure it didn't fail.
	
	// Error messages are limited to MAX_ERROR_MESSAGE_LEN
	
	public static final int MAX_ERROR_MESSAGE_LEN = 1024;
	
	// ---------------------------------------------------------------------------------------------------------
	
	private static cmsLogErrorHandlerFunction DefaultLogErrorHandlerImpl = new cmsLogErrorHandlerFunction()
	{
		public void run(cmsContext ContextID, int ErrorCode, String Text)
		{
			DefaultLogErrorHandlerFunction(ContextID, ErrorCode, Text);
		}
	};
	
	// The current handler in actual environment
	private static cmsLogErrorHandlerFunction LogErrorHandler = DefaultLogErrorHandlerImpl;
	
	// The default error logger does nothing.
	private static void DefaultLogErrorHandlerFunction(cmsContext ContextID, int ErrorCode, final String Text)
	{
		// TextFormatting.fprintf(System.err, "[lcms]: %s\n", new Object[]{Text});
		// System.err.flush();
	}
	
	// Change log error
	public static void cmsSetLogErrorHandler(cmsLogErrorHandlerFunction Fn)
	{
	    if (Fn == null)
	    {
	    	LogErrorHandler = DefaultLogErrorHandlerImpl;
	    }
	    else
	    {
	    	LogErrorHandler = Fn;
	    }
	}
	
	// Log an error 
	// ErrorText is a text holding an english description of error.
	public static void cmsSignalError(cmsContext ContextID, int ErrorCode, final String ErrorText, Object[] args)
	{
		StringBuffer Buffer = new StringBuffer(MAX_ERROR_MESSAGE_LEN);
	    
	    TextFormatting.vsnprintf(Buffer, MAX_ERROR_MESSAGE_LEN-1, ErrorText, args);
	    
	    // Call handler
	    LogErrorHandler.run(ContextID, ErrorCode, Buffer.toString());
	}
	
	// Utility function to print signatures
	public static void _cmsTagSignature2String(StringBuffer String, int sig)
	{
	    int be;
	    
	    // Convert to big endian
	    be = cmsplugin._cmsAdjustEndianess32(sig);
	    
	    // Move chars
	    VirtualPointer vp = new VirtualPointer(4);
	    vp.serialize(new Integer(sig), null, false);
	    String.append((String)vp.deserialize(new Serializers.SerializerWrapper()
	    {
	    	//Complex-ish workaround to "memmove(String, &be, 4);". What can you do, VirtualPointer has the int formatting correct. This keeps all "to byte"/"from byte" operations the same.
			public int inDeserialize(byte[] data, int offset, Object[] val)
			{
				StringBuffer buf = new StringBuffer(4);
				for(int i = offset; i < (offset + 4); i++)
				{
					buf.append((char)data[i]);
				}
				val[0] = buf.toString();
			}
			
			public int inSerialize(byte[] data, int offset, Object val)
			{
				//Never called
				return VirtualPointer.Serializer.STATUS_SUCCESS;
			}
			
			public int getSerializedSize(Object val)
			{
				return 4; //Always 4 bytes
			}
		}, false));
	}
}
