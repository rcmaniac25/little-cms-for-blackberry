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

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import littlecms.internal.helper.Stream;
import littlecms.internal.helper.Utility;
import littlecms.internal.helper.VirtualPointer;
import littlecms.internal.lcms2.cmsContext;
import littlecms.internal.lcms2.cmsHPROFILE;
import littlecms.internal.lcms2.cmsICCHeader;
import littlecms.internal.lcms2.cmsIOHANDLER;
import littlecms.internal.lcms2.cmsProfileID;
import littlecms.internal.lcms2.cmsTagEntry;
import littlecms.internal.lcms2_internal._cmsICCPROFILE;
import littlecms.internal.lcms2_plugin._cms_io_handler;
import littlecms.internal.lcms2_plugin.cmsTagDescriptor;
import littlecms.internal.lcms2_plugin.cmsTagTypeHandler;

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
//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmsio0
{
	public static class FILENULL
	{
		public static final int SIZE = 4;
		
		public int Pointer; // Points to current location
	}
	
	// The NULL IOhandler creator
	public static cmsIOHANDLER cmsOpenIOhandlerFromNULL(cmsContext ContextID)
	{
		cmsIOHANDLER iohandler = null;
	    FILENULL fm = null;
//#ifdef RAW_C
	    VirtualPointer ioP, fmP;
	    
	    iohandler = (cmsIOHANDLER)(ioP = cmserr._cmsMallocZero(ContextID, /*sizeof(cmsIOHANDLER)*/cmsIOHANDLER.SIZE)).getProcessor().readObject(cmsIOHANDLER.class);
	    if(iohandler == null)
	    {
	    	return null;
	    }
	    
	    fm = (FILENULL)(fmP = cmserr._cmsMallocZero(ContextID, /*sizeof(FILENULL)*/4)).getProcessor().readObject(FILENULL.class);
	    if(fm == null)
	    {
	    	/* If fm is null then it is going to remain null
	    	if(fm != null)
		    {
		    	cmserr._cmsFree(ContextID, fmP);
		    }
		    */
		    if(iohandler != null)
		    {
		    	cmserr._cmsFree(ContextID, ioP);
		    }
		    return null;
	    }
//#else
	    iohandler = new cmsIOHANDLER();
	    fm = new FILENULL();
//#endif
	    
	    fm.Pointer = 0;

	    iohandler.ContextID = ContextID;
	    iohandler.stream  = fm;
	    iohandler.UsedSpace = 0;
	    iohandler.PhysicalFile.setLength(0);
	    
	    //NULLRead
	    iohandler.Read = new _cms_io_handler._ioRead()
	    {
			public int run(_cms_io_handler iohandler, byte[] Buffer, int size, int count)
			{
				FILENULL ResData = (FILENULL)iohandler.stream;
			    
			    int len = size * count;
			    ResData.Pointer += len;
			    return count;
			}
		};
		//NULLSeek
	    iohandler.Seek = new _cms_io_handler._ioSeek()
	    {
			public boolean run(_cms_io_handler iohandler, int offset)
			{
				FILENULL ResData = (FILENULL)iohandler.stream;
				
			    ResData.Pointer = offset; 
			    return true;
			}
		};
		//NULLClose
	    iohandler.Close = new _cms_io_handler._ioClose()
	    {
			public boolean run(_cms_io_handler iohandler)
			{
				FILENULL ResData = (FILENULL)iohandler.stream;
				
//#ifdef RAW_C
				cmserr._cmsFree(iohandler.ContextID, new VirtualPointer(ResData));
				cmserr._cmsFree(iohandler.ContextID, new VirtualPointer(iohandler));
//#endif
			    return true;
			}
		};
		//NULLTell
	    iohandler.Tell = new _cms_io_handler._ioTell()
	    {
			public int run(_cms_io_handler iohandler)
			{
				FILENULL ResData = (FILENULL)iohandler.stream;
			    return ResData.Pointer;
			}
		};
		//NULLWrite
	    iohandler.Write = new _cms_io_handler._ioWrite()
	    {
			public boolean run(_cms_io_handler iohandler, int size, byte[] Buffer)
			{
				FILENULL ResData = (FILENULL)iohandler.stream;
				
			    ResData.Pointer += size;
			    if (ResData.Pointer > iohandler.UsedSpace)
			    {
			    	iohandler.UsedSpace = ResData.Pointer;
			    }
			    
			    return true;
			}
		};
	    
	    return iohandler;
	}
	
	// Memory-based stream --------------------------------------------------------------
	
	// Those functions implements an iohandler which takes a block of memory as storage medium.
	
	public static class FILEMEM
	{
		public static final int SIZE = VirtualPointer.SIZE + 4 + 4 + 1;
		
		/** Points to allocated memory*/
		public VirtualPointer Block;
		/** Size of allocated memory*/
		public int Size;
		/** Points to current location*/
	    public int Pointer;
	    /** As title*/
	    public boolean FreeBlockOnClose;
	}
	
	// Create a iohandler for memory block. AccessMode=='r' assumes the iohandler is going to read, and makes
	// a copy of the memory block for letting user to free the memory after invoking open profile. In write 
	// mode ("w"), Buffere points to the begin of memory block to be written.
	public static cmsIOHANDLER cmsOpenIOhandlerFromMem(cmsContext ContextID, byte[] Buffer, int size, final char AccessMode)
	{
	    cmsIOHANDLER iohandler = null;
	    FILEMEM fm = null;
//#ifdef RAW_C
	    VirtualPointer ioP, fmP;
//#endif
	    
		lcms2_internal._cmsAssert(AccessMode != 0, "AccessMode != 0");
		
//#ifdef RAW_C
	    iohandler = (cmsIOHANDLER)(ioP = cmserr._cmsMallocZero(ContextID, /*sizeof(cmsIOHANDLER)*/cmsIOHANDLER.SIZE)).getProcessor().readObject(cmsIOHANDLER.class);
	    if(iohandler == null)
	    {
	    	return null;
	    }
//#else
	    iohandler = new cmsIOHANDLER();
//#endif
	    
	    switch (AccessMode)
	    {
		    case 'r':
//#ifdef RAW_C
		        fm = (FILEMEM)(fmP = cmserr._cmsMallocZero(ContextID, /*sizeof(FILEMEM)*/FILEMEM.SIZE)).getProcessor().readObject(FILEMEM.class);
		        if(fm == null)
		        {
		        	/* If fm is null then it is going to remain null
		        	if (fm != null)
	        	    {
		        		cmserr._cmsFree(ContextID, fmP);
	        	    }
	        	    */
	        	    if (iohandler != null)
	        	    {
	        	    	cmserr._cmsFree(ContextID, ioP);
	        	    }
	        	    return null;
		        }
//#else
		        fm = new FILEMEM();
//#endif
		        
		        if(Buffer == null)
		        {
		        	cmserr.cmsSignalError(ContextID, lcms2_plugin.cmsERROR_READ, Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_CANT_READ_PROFILE_NULL_PTR), null);
//#ifdef RAW_C
		        	if (fm != null)
	        	    {
		        		cmserr._cmsFree(ContextID, fmP);
	        	    }
	        	    if (iohandler != null)
	        	    {
	        	    	cmserr._cmsFree(ContextID, ioP);
	        	    }
//#endif
	        	    return null;
		        }
		        
		        fm.Block = cmserr._cmsMalloc(ContextID, size);
		        if(fm.Block == null)
		        {
//#ifdef RAW_C
		        	cmserr._cmsFree(ContextID, fmP);
		        	cmserr._cmsFree(ContextID, ioP);
//#endif
		            cmserr.cmsSignalError(ContextID, lcms2_plugin.cmsERROR_READ, Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_CANT_ALLOC), new Object[]{new Integer(size)});
		            return null;
		        }
		        
		        fm.Block.writeRaw(Buffer, 0, size);
		        fm.FreeBlockOnClose = true;
		        fm.Size    = size;
		        fm.Pointer = 0;
		        break;
		    case 'w':
//#ifdef RAW_C
		    	fm = (FILEMEM)(fmP = cmserr._cmsMallocZero(ContextID, /*sizeof(FILEMEM)*/FILEMEM.SIZE)).getProcessor().readObject(FILEMEM.class);
		        if (fm == null)
		        {
		        	if (fm != null)
	        	    {
	        	    	cmserr._cmsFree(ContextID, fmP);
	        	    }
	        	    if (iohandler != null)
	        	    {
	        	    	cmserr._cmsFree(ContextID, ioP);
	        	    }
	        	    return null;
		        }
//#else
		        fm = new FILEMEM();
//#endif
		        
		        fm.Block = new VirtualPointer(Buffer, true);
		        fm.FreeBlockOnClose = false;
		        fm.Size    = size;
		        fm.Pointer = 0;
		        break;
		    default:
		    	cmserr.cmsSignalError(ContextID, lcms2_plugin.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_UNK_ACCESS_MODE), new Object[]{new Character(AccessMode)});
		        return null;
	    }
	    
	    iohandler.ContextID = ContextID;
	    iohandler.stream  = fm;   
	    iohandler.UsedSpace = 0;
	    iohandler.PhysicalFile.setLength(0);
	    
	    //MemoryRead
	    iohandler.Read = new _cms_io_handler._ioRead()
	    {
			public int run(_cms_io_handler iohandler, byte[] Buffer, int size, int count)
			{
				FILEMEM ResData = (FILEMEM)iohandler.stream;
				
				VirtualPointer Ptr;
				int len = size * count;
				
				if (ResData.Pointer + len > ResData.Size)
				{
			        len = (ResData.Size - ResData.Pointer);
			        cmserr.cmsSignalError(iohandler.ContextID, lcms2_plugin.cmsERROR_READ, Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_CANT_READ_MEM),
			        		new Object[]{new Integer(len), new Integer(count * size)});
			        return 0;
			    }
				
			    Ptr = ResData.Block;
			    Ptr.readRaw(Buffer, 0, len, ResData.Pointer);
			    ResData.Pointer += len;
			    
			    return count;
			}
		};
		// SEEK_CUR is assumed
		//MemorySeek
	    iohandler.Seek = new _cms_io_handler._ioSeek()
	    {
			public boolean run(_cms_io_handler iohandler, int offset)
			{
				FILEMEM ResData = (FILEMEM)iohandler.stream;
				
			    if(offset > ResData.Size)
			    {
			    	cmserr.cmsSignalError(iohandler.ContextID, lcms2_plugin.cmsERROR_SEEK, Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_NOT_ENOUGH_DATA), null);        
			        return false;
			    }
			    
			    ResData.Pointer = offset;
			    return true;
			}
		};
		//MemoryClose
	    iohandler.Close = new _cms_io_handler._ioClose()
	    {
			public boolean run(_cms_io_handler iohandler)
			{
				FILEMEM ResData = (FILEMEM)iohandler.stream;
				
			    if(ResData.FreeBlockOnClose)
			    {
			        if(ResData.Block != null)
			        {
			        	cmserr._cmsFree(iohandler.ContextID, ResData.Block);
			        }
			    }
			    
//#ifdef RAW_C
			    cmserr._cmsFree(iohandler.ContextID, new VirtualPointer(ResData));
			    cmserr._cmsFree(iohandler.ContextID, new VirtualPointer(iohandler));
//#endif
			    
			    return true;
			}
		};
		// Tell for memory
		//MemoryTell
	    iohandler.Tell = new _cms_io_handler._ioTell()
	    {
			public int run(_cms_io_handler iohandler)
			{
				FILEMEM ResData = (FILEMEM)iohandler.stream;
				
				if(ResData == null)
				{
					return 0;
				}
			    return ResData.Pointer;
			}
		};
		// Writes data to memory, also keeps used space for further reference.
		//MemoryWrite
	    iohandler.Write = new _cms_io_handler._ioWrite()
	    {
			public boolean run(_cms_io_handler iohandler, int size, byte[] Ptr)
			{
				FILEMEM ResData = (FILEMEM)iohandler.stream;
				
				if(ResData == null)
				{
					return false; // Housekeeping
				}
				
			    if(size == 0)
			    {
			    	return true; // Write zero bytes is ok, but does nothing
			    }
			    
			    ResData.Block.writeRaw(Ptr, 0, size, ResData.Pointer);
			    ResData.Pointer += size;
			    
			    if(ResData.Pointer > iohandler.UsedSpace)
			    {
			    	iohandler.UsedSpace = ResData.Pointer;
			    }
			    
			    iohandler.UsedSpace += size;
			    
			    return true;
			}
		};
	    
	    return iohandler;
	}
	
	// File-based stream -------------------------------------------------------
	
	//Simple helper function since cmsOpenIOhandlerFromFile and cmsOpenIOhandlerFromStream do the same thing but cmsOpenIOhandlerFromFile creates the stream itself
	private static void setupFileIOHandler(cmsIOHANDLER iohandler, cmsContext ContextID, Object stream)
	{
		iohandler.ContextID = ContextID;
	    iohandler.stream = stream;
	    iohandler.UsedSpace = 0;
	    
	    // Read count elements of size bytes each. Return number of elements read
	    //FileRead
	    iohandler.Read = new _cms_io_handler._ioRead()
	    {
			public int run(_cms_io_handler iohandler, byte[] Buffer, int size, int count)
			{
				int nReaded = (int)((Stream)iohandler.stream).read(Buffer, 0, size, count);
				
			    if (nReaded != count)
			    {
			    	cmserr.cmsSignalError(iohandler.ContextID, lcms2_plugin.cmsERROR_FILE, Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_READ_ERR), 
			    			new Object[]{new Integer(nReaded * size), new Integer(count * size)});
			    	return 0;
			    }
			    
			    return nReaded;
			}
		};
		// Position file pointer in the file
		//FileSeek
	    iohandler.Seek = new _cms_io_handler._ioSeek()
	    {
			public boolean run(_cms_io_handler iohandler, int offset)
			{
				if (((Stream)iohandler.stream).seek(offset, Stream.SEEK_SET) != 0)
				{
					cmserr.cmsSignalError(iohandler.ContextID, lcms2_plugin.cmsERROR_FILE, Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_SEEK_ERROR), null);
					return false;
				}
				return true;
			}
		};
		// Closes the file
		//FileClose
	    iohandler.Close = new _cms_io_handler._ioClose()
	    {
			public boolean run(_cms_io_handler iohandler)
			{
				if (((Stream)iohandler.stream).close() != 0)
				{
					return false;
				}
//#ifdef RAW_C
				cmserr._cmsFree(iohandler.ContextID, new VirtualPointer(iohandler));
//#endif
			    return true;
			}
		};
		// Returns file pointer position
		//FileTell
	    iohandler.Tell = new _cms_io_handler._ioTell()
	    {
			public int run(_cms_io_handler iohandler)
			{
				return (int)((Stream)iohandler.stream).getPosition();
			}
		};
		// Writes data to stream, also keeps used space for further reference. Returns TRUE on success, FALSE on error
		//FileWrite
	    iohandler.Write = new _cms_io_handler._ioWrite()
	    {
			public boolean run(_cms_io_handler iohandler, int size, byte[] Ptr)
			{
				if (size == 0)
				{
					return true;  // We allow to write 0 bytes, but nothing is written
				}
				
				iohandler.UsedSpace += size;
				return ((Stream)iohandler.stream).write(Ptr, 0, size, 1) == 1;
			}
		};
	}
	
	// Create a iohandler for disk based files. if FileName is NULL, then 'stream' member is also set
	// to NULL and no real writting is performed. This only happens in writting access mode
	public static cmsIOHANDLER cmsOpenIOhandlerFromFile(cmsContext ContextID, final String FileName, final char AccessMode)
	{
	    cmsIOHANDLER iohandler = null;
	    Stream fm = null;
//#ifdef RAW_C
	    VirtualPointer ioP;
		
	    iohandler = (cmsIOHANDLER)(ioP = cmserr._cmsMallocZero(ContextID, /*sizeof(cmsIOHANDLER)*/cmsIOHANDLER.SIZE)).getProcessor().readObject(cmsIOHANDLER.class);
	    if (iohandler == null)
	    {
	    	return null;
	    }
//#else
	    iohandler = new cmsIOHANDLER();
//#endif
	    
	    switch (AccessMode)
	    {
		    case 'r':
		        fm = Stream.fopen(FileName, 'r');
		        if (fm == null)
		        {
//#ifdef RAW_C
		        	cmserr._cmsFree(ContextID, ioP);
//#endif
		        	cmserr.cmsSignalError(ContextID, lcms2_plugin.cmsERROR_FILE, Utility.LCMS_Resources.getString(LCMSResource.FILE_NOT_FOUND), new Object[]{FileName});
		            return null;
		        }
		        break;
		    case 'w':
		        fm = Stream.fopen(FileName, 'w');
		        if (fm == null)
		        {
//#ifdef RAW_C
		        	cmserr._cmsFree(ContextID, ioP);
//#endif
		            cmserr.cmsSignalError(ContextID, lcms2_plugin.cmsERROR_FILE, Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_COULDNT_CREATE_FILE), new Object[]{FileName});
		            return null;
		        }
		        break;
		    default:
//#ifdef RAW_C
		    	cmserr._cmsFree(ContextID, ioP);
//#endif
		    	cmserr.cmsSignalError(ContextID, lcms2_plugin.cmsERROR_FILE, Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_UNK_ACCESS_MODE), new Object[]{new Character(AccessMode)});
		        return null;
	    }
	    
	    setupFileIOHandler(iohandler, ContextID, fm);
	    
	    // Keep track of the original file
	    if (FileName != null) 
	    {
	    	iohandler.PhysicalFile.setLength(0);
	    	iohandler.PhysicalFile.append(FileName);
	    }
	    
	    return iohandler;
	}
	
	// Create a iohandler for stream based files 
	public static cmsIOHANDLER cmsOpenIOhandlerFromStream(cmsContext ContextID, Stream Stream)
	{
	    cmsIOHANDLER iohandler = null;
		
//#ifdef RAW_C
	    iohandler = (cmsIOHANDLER)cmserr._cmsMallocZero(ContextID, /*sizeof(cmsIOHANDLER)*/cmsIOHANDLER.SIZE).getProcessor().readObject(cmsIOHANDLER.class);
	    if (iohandler == null)
	    {
	    	return null;
	    }
//#else
	    iohandler = new cmsIOHANDLER();
//#endif
	    
	    setupFileIOHandler(iohandler, ContextID, Stream);
	    
	    iohandler.PhysicalFile.setLength(0);
	    
	    return iohandler;
	}
	
	// Close an open IO handler
	public static boolean cmsCloseIOhandler(cmsIOHANDLER io)
	{
	    return io.Close.run(io);
	}
	
	// -------------------------------------------------------------------------------------------------------

	// Creates an empty structure holding all required parameters
	public static cmsHPROFILE cmsCreateProfilePlaceholder(cmsContext ContextID)
	{
		Date now = new Date();
//#ifdef RAW_C
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)cmserr._cmsMallocZero(ContextID, /*sizeof(_cmsICCPROFILE)*/_cmsICCPROFILE.SIZE).getProcessor().readObject(_cmsICCPROFILE.class);
	    if (Icc == null)
	    {
	    	return null;
	    }
//#else
	    _cmsICCPROFILE Icc = new _cmsICCPROFILE();
	    Icc.Created = Calendar.getInstance();
//#endif
	    
	    Icc.ContextID = ContextID;
	    
	    // Set it to empty
	    Icc.TagCount = 0;
	    
	    // Set default version 
	    Icc.Version = 0x02100000;
	    
	    // Set creation date/time
	    Icc.Created.setTime(now);
	    
	    // Return the handle
	    return (cmsHPROFILE)Icc;
	}
	
	public static cmsContext cmsGetProfileContextID(cmsHPROFILE hProfile)
	{
	     _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;  
	     
	    if (Icc == null)
	    {
	    	return null;
	    }
	    return Icc.ContextID;
	}
	
	// Return the number of tags
	public static int cmsGetTagCount(cmsHPROFILE hProfile)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;  
		if (Icc == null)
		{
			return -1;
		}
		
	    return Icc.TagCount;
	}
	
	// Return the tag signature of a given tag number
	public static int cmsGetTagSignature(cmsHPROFILE hProfile, int n)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;    
	    
	    if (n > Icc.TagCount)
	    {
	    	return 0; // Mark as not available     
	    }
	    if (n >= lcms2_internal.MAX_TABLE_TAG)
	    {
	    	return 0; // As double check
	    }
	    
	    return Icc.TagNames[n];
	}
	
	private static int SearchOneTag(_cmsICCPROFILE Profile, int sig)
	{
		for (int i = 0; i < Profile.TagCount; i++)
		{
			if (sig == Profile.TagNames[i])
			{
				return i;
			}
		}
		
		return -1;
	}
	
	// Search for a specific tag in tag dictionary. Returns position or -1 if tag not found.
	// If followlinks is turned on, then the position of the linked tag is returned
	public static int _cmsSearchTag(_cmsICCPROFILE Icc, int sig, boolean lFollowLinks)
	{
		int n;
		int LinkedSig;
		
		do
		{
			// Search for given tag in ICC profile directory
			n = SearchOneTag(Icc, sig);
			if (n < 0)
			{
				return -1; // Not found
			}
			
			if (!lFollowLinks)
			{
				return n; // Found, don't follow links
			}
			
			// Is this a linked tag?
			LinkedSig = Icc.TagLinked[n];
			
			// Yes, follow link
			if (LinkedSig != 0)
			{
				sig = LinkedSig;
			}
		} while (LinkedSig != 0);
		
		return n;
	}
	
	// Create a new tag entry

	private static boolean _cmsNewTag(_cmsICCPROFILE Icc, int sig, int[] NewPos)
	{
		// Search for the tag
	    int i = _cmsSearchTag(Icc, sig, false);
	    
	    // Now let's do it easy. If the tag has been already written, that's an error
	    if (i >= 0)
	    {
	    	cmserr.cmsSignalError(Icc.ContextID, lcms2_plugin.cmsERROR_ALREADY_DEFINED, Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_TAG_ALREADY_EXISTS), new Object[]{new Integer(sig)});
	        return false;
	    }
	    else 
	    {
	        // New one
	        
	        if (Icc.TagCount >= lcms2_internal.MAX_TABLE_TAG)
	        {
	        	cmserr.cmsSignalError(Icc.ContextID, lcms2_plugin.cmsERROR_RANGE, Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_TO_MANY_TAGS), new Object[]{new Integer(lcms2_internal.MAX_TABLE_TAG)});
	            return false;
	        }
	        
			NewPos[0] = Icc.TagCount;
	        Icc.TagCount++;
	    }
	    
		return true;
	}
	
	// Check existence
	public static boolean cmsIsTag(cmsHPROFILE hProfile, int sig)
	{
		_cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
		return _cmsSearchTag(Icc, sig, false) >= 0;
	}
	
	// Read profile header and validate it
	public static boolean _cmsReadHeader(_cmsICCPROFILE Icc)
	{
		cmsTagEntry Tag;
	    cmsICCHeader Header;
	    int HeaderSize;
	    cmsIOHANDLER io = Icc.IOhandler;
	    int TagCount;
	    VirtualPointer vp;
	    long[] temp;
	    int[] temp2;
	    
	    // Read the header
	    vp = new VirtualPointer(cmsICCHeader.SIZE);
	    if (io.vpRead(io, vp, /*sizeof(cmsICCHeader)*/cmsICCHeader.SIZE, 1) != 1)
	    {
	        return false;
	    }
	    Header = (cmsICCHeader)vp.getProcessor().readObject(cmsICCHeader.class);
	    
	    // Validate file as an ICC profile
	    if (cmsplugin._cmsAdjustEndianess32(Header.magic) != lcms2.cmsMagicNumber)
	    {
	    	cmserr.cmsSignalError(Icc.ContextID, lcms2_plugin.cmsERROR_BAD_SIGNATURE, Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_NOT_ICC_PROFILE), null);
	        return false;
	    }
	    
	    // Adjust endianess of the used parameters
	    Icc.DeviceClass     = cmsplugin._cmsAdjustEndianess32(Header.deviceClass);
	    Icc.ColorSpace      = cmsplugin._cmsAdjustEndianess32(Header.colorSpace);
	    Icc.PCS             = cmsplugin._cmsAdjustEndianess32(Header.pcs);
	    Icc.RenderingIntent = cmsplugin._cmsAdjustEndianess32(Header.renderingIntent);
	    Icc.flags           = cmsplugin._cmsAdjustEndianess32(Header.flags);
	    Icc.manufacturer    = cmsplugin._cmsAdjustEndianess32(Header.manufacturer);
	    Icc.model           = cmsplugin._cmsAdjustEndianess32(Header.model);
	    temp = new long[1];
	    cmsplugin._cmsAdjustEndianess64(temp, Header.attributes);
	    Icc.attributes = temp[0];
	    Icc.Version         = cmsplugin._cmsAdjustEndianess32(Header.version);
	    
	    // Get size as reported in header
	    HeaderSize = cmsplugin._cmsAdjustEndianess32(Header.size); //Figure out why this keeps coming out negative
	    
	    // Get creation date/time
	    cmsplugin._cmsDecodeDateTimeNumber(Header.date, Icc.Created);
	    
	    // The profile ID are 32 raw bytes
	    Icc.ProfileID = new cmsProfileID();
	    Icc.ProfileID.setID8(Header.profileID.getID8());
	    
	    
	    // Read tag directory
	    temp2 = new int[1];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp2))
	    {
	    	return false;
	    }
	    TagCount = temp2[0];
	    if (TagCount > lcms2_internal.MAX_TABLE_TAG)
	    {
	    	cmserr.cmsSignalError(Icc.ContextID, lcms2_plugin.cmsERROR_RANGE, Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_TO_MANY_TAGS), new Object[]{new Integer(TagCount)});
	        return false;
	    }
	    
	    // Read tag directory
	    Icc.TagCount = 0;
	    Tag = new cmsTagEntry();
	    for (int i = 0; i < TagCount; i++)
	    {
	        if (!cmsplugin._cmsReadUInt32Number(io, temp2))
	        {
	        	return false;
	        }
	        Tag.sig = temp2[0];
	        if (!cmsplugin._cmsReadUInt32Number(io, temp2))
	        {
	        	return false;
	        }
	        Tag.offset = temp2[0];
	        if (!cmsplugin._cmsReadUInt32Number(io, temp2))
	        {
	        	return false;
	        }
	        Tag.size = temp2[0];
	        
	        // Perform some sanity check. Offset + size should fall inside file.
	        if (Tag.offset + Tag.size > HeaderSize)
	        {
	        	continue;
	        }
	        
	        Icc.TagNames[Icc.TagCount]   = Tag.sig;
	        Icc.TagOffsets[Icc.TagCount] = Tag.offset;
	        Icc.TagSizes[Icc.TagCount]   = Tag.size;
	        
	        // Search for links
	        for (int j = 0; j < Icc.TagCount; j++)
	        {
	            if ((Icc.TagOffsets[j] == Tag.offset) && (Icc.TagSizes[j] == Tag.size))
	            {
	                Icc.TagLinked[Icc.TagCount] = Icc.TagNames[j];
	            }
	        }
	        
	        Icc.TagCount++;
	    }
	    
	    return true;
	}
	
	// Saves profile header
	public static boolean _cmsWriteHeader(_cmsICCPROFILE Icc, int UsedSpace)
	{
	    cmsICCHeader Header = new cmsICCHeader();
	    cmsTagEntry Tag = new cmsTagEntry();
	    int Count = 0;
	    long[] temp;
	    
	    Header.size        = cmsplugin._cmsAdjustEndianess32(UsedSpace);
	    Header.cmmId       = cmsplugin._cmsAdjustEndianess32(lcms2.lcmsSignature);
	    Header.version     = cmsplugin._cmsAdjustEndianess32(Icc.Version);
	    
	    Header.deviceClass = cmsplugin._cmsAdjustEndianess32(Icc.DeviceClass);
	    Header.colorSpace  = cmsplugin._cmsAdjustEndianess32(Icc.ColorSpace);
	    Header.pcs         = cmsplugin._cmsAdjustEndianess32(Icc.PCS);
	    
	    //   NOTE: in v4 Timestamp must be in UTC rather than in local time
	    cmsplugin._cmsEncodeDateTimeNumber(Header.date, Icc.Created);
	    
	    Header.magic       = cmsplugin._cmsAdjustEndianess32(lcms2.cmsMagicNumber);
	    
	    Header.platform    = cmsplugin._cmsAdjustEndianess32(lcms2.cmsSigRim);
	    
	    Header.flags        = cmsplugin._cmsAdjustEndianess32(Icc.flags);
	    Header.manufacturer = cmsplugin._cmsAdjustEndianess32(Icc.manufacturer);
	    Header.model        = cmsplugin._cmsAdjustEndianess32(Icc.model);
	    
	    temp = new long[1];
	    cmsplugin._cmsAdjustEndianess64(temp, Icc.attributes);
	    Header.attributes = temp[0];
	    
	    // Rendering intent in the header (for embedded profiles)
	    Header.renderingIntent = cmsplugin._cmsAdjustEndianess32(Icc.RenderingIntent);
	    
	    // Illuminant is always D50
	    Header.illuminant.X = (short)cmsplugin._cmsAdjustEndianess32(cmsplugin._cmsDoubleTo15Fixed16(lcms2.cmsD50_XYZ.X));
	    Header.illuminant.Y = (short)cmsplugin._cmsAdjustEndianess32(cmsplugin._cmsDoubleTo15Fixed16(lcms2.cmsD50_XYZ.Y));
	    Header.illuminant.Z = (short)cmsplugin._cmsAdjustEndianess32(cmsplugin._cmsDoubleTo15Fixed16(lcms2.cmsD50_XYZ.Z));
	    
	    // Created by LittleCMS (that's me!)
	    Header.creator      = cmsplugin._cmsAdjustEndianess32(lcms2.lcmsSignature);
	    
	    // Set profile ID. Endianess is always big endian   
	    Header.profileID.setID8(Icc.ProfileID.getID8());
	    
	    // Dump the header
	    VirtualPointer vp = new VirtualPointer(Header);
	    if (!Icc.IOhandler.vpWrite(Icc.IOhandler, /*sizeof(cmsICCHeader)*/cmsICCHeader.SIZE, vp))
	    {
	    	return false;
	    }
	    
	    // Saves Tag directory
	    
	    // Get true count
	    for (int i = 0;  i < Icc.TagCount; i++)
	    {
	        if (Icc.TagNames[i] != 0)
	        {
	        	Count++;
	        }
	    }
	    
	    // Store number of tags
	    if (!cmsplugin._cmsWriteUInt32Number(Icc.IOhandler, Count))
	    {
	    	return false;
	    }
	    
	    VirtualPointer.TypeProcessor proc = vp.getProcessor();
	    for (int i = 0; i < Icc.TagCount; i++)
	    {
	        if (Icc.TagNames[i] == 0)
	        {
	        	continue;   // It is just a placeholder
	        }
	        
	        Tag.sig    = cmsplugin._cmsAdjustEndianess32(Icc.TagNames[i]);
	        Tag.offset = cmsplugin._cmsAdjustEndianess32(Icc.TagOffsets[i]);
	        Tag.size   = cmsplugin._cmsAdjustEndianess32(Icc.TagSizes[i]);
	        
	        proc.write(Tag);
	        if (!Icc.IOhandler.vpWrite(Icc.IOhandler, /*sizeof(cmsTagEntry)*/cmsTagEntry.SIZE, vp))
	        {
	        	return false;
	        }
	    }
	    
	    return true;
	}
	
	// ----------------------------------------------------------------------- Set/Get several struct members
	
	
	public static int cmsGetHeaderRenderingIntent(cmsHPROFILE hProfile)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    return Icc.RenderingIntent;
	}
	
	public static void cmsSetHeaderRenderingIntent(cmsHPROFILE hProfile, int RenderingIntent)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    Icc.RenderingIntent = RenderingIntent;
	}
	
	public static int cmsGetHeaderFlags(cmsHPROFILE hProfile)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    return Icc.flags;
	}
	
	public static void cmsSetHeaderFlags(cmsHPROFILE hProfile, int Flags)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    Icc.flags = Flags;
	}
	
	public static int cmsGetHeaderManufacturer(cmsHPROFILE hProfile)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    return Icc.manufacturer;
	}
	
	public static void cmsSetHeaderManufacturer(cmsHPROFILE hProfile, int manufacturer)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    Icc.manufacturer = manufacturer;
	}
	
	public static int cmsGetHeaderModel(cmsHPROFILE hProfile)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    return Icc.model;
	}
	
	public static void cmsSetHeaderModel(cmsHPROFILE hProfile, int model)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    Icc.manufacturer = model;
	}
	
	
	public static void cmsGetHeaderAttributes(cmsHPROFILE hProfile, long[] Flags)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    Flags[0] = Icc.attributes;
	}
	
	public static void cmsSetHeaderAttributes(cmsHPROFILE hProfile, long Flags)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    Icc.attributes = Flags;
	}
	
	public static void cmsGetHeaderProfileID(cmsHPROFILE hProfile, cmsProfileID ProfileID)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    ProfileID.setID8(Icc.ProfileID.getID8());
	}
	
	public static void cmsSetHeaderProfileID(cmsHPROFILE hProfile, cmsProfileID ProfileID)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    Icc.ProfileID.setID8(ProfileID.getID8());
	}
	
	public static boolean cmsGetHeaderCreationDateTime(cmsHPROFILE hProfile, Calendar Dest)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    Dest.setTime(Icc.Created.getTime());
	    Dest.setTimeZone(Icc.Created.getTimeZone());
	    return true;
	}
	
	public static int cmsGetPCS(cmsHPROFILE hProfile)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    return Icc.PCS;
	}
	
	public static void cmsSetPCS(cmsHPROFILE hProfile, int pcs)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    Icc.PCS = pcs;
	}
	
	public static int cmsGetColorSpace(cmsHPROFILE hProfile)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    return Icc.ColorSpace;
	}
	
	public static void cmsSetColorSpace(cmsHPROFILE hProfile, int sig)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    Icc.ColorSpace = sig;
	}
	
	public static int cmsGetDeviceClass(cmsHPROFILE hProfile)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    return Icc.DeviceClass;
	}
	
	public static void cmsSetDeviceClass(cmsHPROFILE hProfile, int sig)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    Icc.DeviceClass = sig;
	}
	
	public static int cmsGetEncodedICCversion(cmsHPROFILE hProfile)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    return Icc.Version;
	}
	
	public static void cmsSetEncodedICCversion(cmsHPROFILE hProfile, int Version)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    Icc.Version = Version;
	}
	
	// Get an hexadecimal number with same digits as v
	private static int BaseToBase(int in, int BaseIn, int BaseOut)
	{
	    byte[] Buff = new byte[100];
	    int len;
	    int out = 0;
	    
	    for (len = 0; in > 0 && len < 100; len++)
	    {
	        Buff[len] = (byte)(in % BaseIn);
	        in /= BaseIn;
	    }
	    
	    for (int i = len - 1; i >= 0; --i)
	    {
	        out = out * BaseOut + Buff[i];
	    }
	    
	    return out;
	}
	
	public static void cmsSetProfileVersion(cmsHPROFILE hProfile, double Version)
	{
		_cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
		
	    // 4.2 -> 0x4200000
		
	    Icc.Version = BaseToBase((int)Math.floor(Version * 100.0), 10, 16) << 16;
	}

	public static double cmsGetProfileVersion(cmsHPROFILE hProfile)
	{
		_cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    int n = Icc.Version >> 16;
	    
	    return BaseToBase(n, 16, 10) / 100.0;
	}
	// --------------------------------------------------------------------------------------------------------------
	
	// Create profile from IOhandler
	public static cmsHPROFILE cmsOpenProfileFromIOhandlerTHR(cmsContext ContextID, cmsIOHANDLER io)
	{
	    _cmsICCPROFILE NewIcc;
	    cmsHPROFILE hEmpty = cmsCreateProfilePlaceholder(ContextID);
	    
	    if (hEmpty == null)
	    {
	    	return null;
	    }
	    
	    NewIcc = (_cmsICCPROFILE)hEmpty;
	    
	    NewIcc.IOhandler = io;
	    if (!_cmsReadHeader(NewIcc))
	    {
	    	cmsCloseProfile(hEmpty);
		    return null;
	    }
	    return hEmpty;
	}
	
	// Create profile from disk file
	public static cmsHPROFILE cmsOpenProfileFromFileTHR(cmsContext ContextID, final String lpFileName, final char sAccess)
	{
	    _cmsICCPROFILE NewIcc;
	    cmsHPROFILE hEmpty = cmsCreateProfilePlaceholder(ContextID);
	    
	    if (hEmpty == null)
	    {
	    	return null;
	    }
	    
	    NewIcc = (_cmsICCPROFILE)hEmpty;
	    
	    NewIcc.IOhandler = cmsOpenIOhandlerFromFile(ContextID, lpFileName, sAccess);
	    if (NewIcc.IOhandler == null)
	    {
	    	cmsCloseProfile(hEmpty);
		    return null;
	    }
	    
	    if (sAccess == 'W' || sAccess == 'w')
	    {
	        NewIcc.IsWrite = true;
	        
	        return hEmpty;
	    }
	    
	    if (!_cmsReadHeader(NewIcc))
	    {
	    	cmsCloseProfile(hEmpty);
		    return null;
	    }
	    return hEmpty;
	}
	
	public static cmsHPROFILE cmsOpenProfileFromFile(final String ICCProfile, final char sAccess)
	{
	    return cmsOpenProfileFromFileTHR(null, ICCProfile, sAccess);
	}
	
	public static cmsHPROFILE cmsOpenProfileFromStreamTHR(cmsContext ContextID, Stream ICCProfile, final char sAccess)
	{
	    _cmsICCPROFILE NewIcc;
	    cmsHPROFILE hEmpty = cmsCreateProfilePlaceholder(ContextID);
	    
	    if (hEmpty == null)
	    {
	    	return null;
	    }
	    
	    NewIcc = (_cmsICCPROFILE)hEmpty;
	    
	    NewIcc.IOhandler = cmsOpenIOhandlerFromStream(ContextID, ICCProfile);
	    if (NewIcc.IOhandler == null)
	    {
	    	cmsCloseProfile(hEmpty);
		    return null;
	    }
	    
	    if (sAccess == 'w')
	    {
	        NewIcc.IsWrite = true;       
	        return hEmpty;
	    }
	    
	    if (!_cmsReadHeader(NewIcc))
	    {
	    	cmsCloseProfile(hEmpty);
		    return null;    
	    }
	    return hEmpty;

	}

	public static cmsHPROFILE cmsOpenProfileFromStream(Stream ICCProfile, final char sAccess)
	{
	    return cmsOpenProfileFromStreamTHR(null, ICCProfile, sAccess);
	}
	
	// Open from memory block
	public static cmsHPROFILE cmsOpenProfileFromMemTHR(cmsContext ContextID, final byte[] MemPtr, int dwSize)
	{
	    _cmsICCPROFILE NewIcc;
	    cmsHPROFILE hEmpty;
	    
	    hEmpty = cmsCreateProfilePlaceholder(ContextID);
	    if (hEmpty == null)
	    {
	    	return null;
	    }
	    
	    NewIcc = (_cmsICCPROFILE)hEmpty;
	    
		// Ok, in this case const void* is casted to void* just because open IO handler 
		// shares read and writing modes. Don't abuse this feature!
	    NewIcc.IOhandler = cmsOpenIOhandlerFromMem(ContextID, MemPtr, dwSize, 'r');
	    if (NewIcc.IOhandler == null)
	    {
	    	cmsCloseProfile(hEmpty);
		    return null;
	    }
	    
	    if (!_cmsReadHeader(NewIcc))
	    {
	    	cmsCloseProfile(hEmpty);
		    return null;
	    }
	    
	    return hEmpty;
	}
	
	public static cmsHPROFILE cmsOpenProfileFromMem(final byte[] MemPtr, int dwSize)
	{
	    return cmsOpenProfileFromMemTHR(null, MemPtr, dwSize);
	}
	
	// Dump tag contents. If the profile is being modified, untouched tags are copied from FileOrig
	private static boolean SaveTags(_cmsICCPROFILE Icc, _cmsICCPROFILE FileOrig)
	{
	    Object Data;
	    byte[] DataByte;
	    int Begin;
	    cmsIOHANDLER io = Icc.IOhandler;
	    cmsTagDescriptor TagDescriptor;
	    int TypeBase;
	    cmsTagTypeHandler TypeHandler;
	    
	    for (int i = 0; i < Icc.TagCount; i++)
	    {
	        if (Icc.TagNames[i] == 0)
	        {
	        	continue;
	        }
	        
	        // Linked tags are not written
	        if (Icc.TagLinked[i] != 0)
	        {
	        	continue;
	        }
	        
	        Icc.TagOffsets[i] = Begin = io.UsedSpace;
	        
	        Data = Icc.TagPtrs[i];
	        
	        if (Data == null)
	        {
	            // Reach here if we are copying a tag from a disk-based ICC profile which has not been modified by user. 
	            // In this case a blind copy of the block data is performed
	            if (FileOrig != null && Icc.TagOffsets[i] != 0)
	            {
	                int TagSize   = FileOrig.TagSizes[i];
	                int TagOffset = FileOrig.TagOffsets[i];
	                VirtualPointer Mem;
	                
	                if (!FileOrig.IOhandler.Seek.run(FileOrig.IOhandler, TagOffset))
	                {
	                	return false;
	                }
	                
	                Mem = cmserr._cmsMalloc(Icc.ContextID, TagSize);
	                if (Mem == null)
	                {
	                	return false;
	                }
	                
	                if (FileOrig.IOhandler.vpRead(FileOrig.IOhandler, Mem, TagSize, 1) != 1)
	                {
	                	return false;
	                }
	                if (!io.vpWrite(io, TagSize, Mem))
	                {
	                	return false;
	                }
	                cmserr._cmsFree(Icc.ContextID, Mem);
	                
	                Icc.TagSizes[i] = (io.UsedSpace - Begin);
	                
	                
	                // Align to 32 bit boundary.
	                if (!cmsplugin._cmsWriteAlignment(io))
	                {
	                	return false;                   
	                }
	            }
	            
	            continue;
	        }
	        
	        // Should this tag be saved as RAW? If so, tagsizes should be specified in advance (no further cooking is done)
	        if (Icc.TagSaveAsRaw[i])
	        {
	            if (!io.vpWrite(io, Icc.TagSizes[i], (VirtualPointer)Data))
	            {
	            	return false;
	            }
	        }
	        else
	        {
	            // Search for support on this tag
	            TagDescriptor = cmstypes._cmsGetTagDescriptor(Icc.TagNames[i]);
	            if (TagDescriptor == null)
	            {
	            	continue; // Unsupported, ignore it
	            }
	            
	            TypeHandler = Icc.TagTypeHandlers[i];
	            
	            if (TypeHandler == null)
	            {
	            	cmserr.cmsSignalError(Icc.ContextID, lcms2_plugin.cmsERROR_INTERNAL, 
	            			Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_NO_TAG_HANDLER), new Object[]{new Integer(Icc.TagNames[i])});
	                continue;
	            }
	            
	            TypeBase = TypeHandler.Signature;
	            if (!cmsplugin._cmsWriteTypeBase(io, TypeBase))
	            {
	            	return false;
	            }
	            
	            if (!TypeHandler.WritePtr.run(TypeHandler, io, Data, TagDescriptor.ElemCount))
	            {
	            	StringBuffer String = new StringBuffer(4);
					
	            	cmserr._cmsTagSignature2String(String, TypeBase);
	            	cmserr.cmsSignalError(Icc.ContextID, lcms2_plugin.cmsERROR_WRITE, Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_CANT_WRITE_TYPE), 
	            			new Object[]{String});
	                return false;
	            }
	        }
	        
	        Icc.TagSizes[i] = (io.UsedSpace - Begin);
	        
	        // Align to 32 bit boundary.
	        if (!cmsplugin._cmsWriteAlignment(io))
	        {
	        	return false;                   
	        }
	    }
	    
	    return true;
	}
	
	// Fill the offset and size fields for all linked tags
	private static boolean SetLinks(_cmsICCPROFILE Icc)
	{
	    for (int i = 0; i < Icc.TagCount; i++)
	    {
	        int lnk = Icc.TagLinked[i];
	        if (lnk != 0)
	        {
	            int j = _cmsSearchTag(Icc, lnk, false);
	            if (j >= 0)
	            {
	                Icc.TagOffsets[i] = Icc.TagOffsets[j];
	                Icc.TagSizes[i]   = Icc.TagSizes[j];
	            }
	        }
	    }
	    
	    return true;
	}
	
	// Low-level save to IOHANDLER. It returns the number of bytes used to
	// store the profile, or zero on error. io may be NULL and in this case
	// no data is written--only sizes are calculated
	public static int cmsSaveProfileToIOhandler(cmsHPROFILE hProfile, cmsIOHANDLER io)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    _cmsICCPROFILE Keep;
	    cmsIOHANDLER PrevIO;
	    int UsedSpace;
	    cmsContext ContextID;
	    
	    //Manual equivalent of "memmove(&Keep, Icc, sizeof(_cmsICCPROFILE));"
	    VirtualPointer vp = new VirtualPointer(Icc);
	    Keep = (_cmsICCPROFILE)vp.getProcessor().readObject(_cmsICCPROFILE.class);
	    
	    ContextID = cmsGetProfileContextID(hProfile);
	    PrevIO = Icc.IOhandler = cmsOpenIOhandlerFromNULL(ContextID);
	    if (PrevIO == null)
	    {
	    	return 0;
	    }
	    
	    // Pass #1 does compute offsets
	    
	    if (!_cmsWriteHeader(Icc, 0))
	    {
	    	return 0;
	    }
	    if (!SaveTags(Icc, Keep))
	    {
	    	return 0;
	    }

	    UsedSpace = PrevIO.UsedSpace;
	    
	    // Pass #2 does save to iohandler
	    
	    if (io != null)
	    {
	        Icc.IOhandler = io;
	        if (!SetLinks(Icc))
	        {
	        	cmsCloseIOhandler(PrevIO);
	        	//This is the equivilant of doing "memmove(&Icc, Keep, sizeof(_cmsICCPROFILE));" only it only copies over values that could have changed
	        	vp.getProcessor().readObject(_cmsICCPROFILE.class, false, Icc);
	    	    return 0;
	        }
	        if (!_cmsWriteHeader(Icc, UsedSpace))
	        {
	        	cmsCloseIOhandler(PrevIO);
	        	vp.getProcessor().readObject(_cmsICCPROFILE.class, false, Icc);
	    	    return 0;
	        }
	        if (!SaveTags(Icc, Keep))
	        {
	        	cmsCloseIOhandler(PrevIO);
	        	vp.getProcessor().readObject(_cmsICCPROFILE.class, false, Icc);
	    	    return 0;
	        }
	    }
	    
	    vp.getProcessor().readObject(_cmsICCPROFILE.class, false, Icc);
	    if (!cmsCloseIOhandler(PrevIO))
	    {
	    	return 0;
	    }
	    
	    return UsedSpace;
	}
	
	//C remove function
	private static int remove(final String filename)
	{
		int result = -1;
		try
		{
			FileConnection file = (FileConnection)Connector.open(filename, Connector.WRITE);
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
	
	// Low-level save to disk. 
	public static boolean cmsSaveProfileToFile(cmsHPROFILE hProfile, final String FileName)
	{   
	    cmsContext ContextID = cmsGetProfileContextID(hProfile);
	    cmsIOHANDLER io = cmsOpenIOhandlerFromFile(ContextID, FileName, 'w');
	    boolean rc;
	    
	    if (io == null)
	    {
	    	return false;
	    }
	    
	    rc = (cmsSaveProfileToIOhandler(hProfile, io) != 0);
	    rc &= cmsCloseIOhandler(io);
	    
	    if (!rc) // remove() is C99 per 7.19.4.1
	    {
	    	remove(FileName); // We have to IGNORE return value in this case
	    }
	    return rc;
	}
	
	// Same as anterior, but for streams
	public static boolean cmsSaveProfileToStream(cmsHPROFILE hProfile, Stream Stream)
	{
	    boolean rc;
	    cmsContext ContextID = cmsGetProfileContextID(hProfile);
	    cmsIOHANDLER io = cmsOpenIOhandlerFromStream(ContextID, Stream);
	    
	    if (io == null)
	    {
	    	return false;
	    }
	    
	    rc = (cmsSaveProfileToIOhandler(hProfile, io) != 0);
	    rc &= cmsCloseIOhandler(io);
	    
	    return rc;
	}
	
	// Same as anterior, but for memory blocks. In this case, a NULL as MemPtr means calculate needed space only
	public static boolean cmsSaveProfileToMem(cmsHPROFILE hProfile, byte[] MemPtr, int[] BytesNeeded)
	{
	    boolean rc;
	    cmsIOHANDLER io;
	    cmsContext ContextID = cmsGetProfileContextID(hProfile);
	    
	    // Should we just calculate the needed space?
	    if (MemPtr == null)
	    {
	    	BytesNeeded[0] = cmsSaveProfileToIOhandler(hProfile, null);
	    	return true;
	    }
	    
	    // That is a real write operation
	    io =  cmsOpenIOhandlerFromMem(ContextID, MemPtr, BytesNeeded[0], 'w');
	    if (io == null)
	    {
	    	return false;
	    }
	    
	    rc = (cmsSaveProfileToIOhandler(hProfile, io) != 0);
	    rc &= cmsCloseIOhandler(io);
	    
	    return rc;
	}
	
	// Closes a profile freeing any involved resources
	public static boolean cmsCloseProfile(cmsHPROFILE hProfile)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    boolean rc = true;
	    
	    if (Icc == null)
	    {
	    	return false;
	    }
	    
	    // Was open in write mode?   
	    if (Icc.IsWrite)
	    {
	        Icc.IsWrite = false; // Assure no further writing
	        rc &= cmsSaveProfileToFile(hProfile, Icc.IOhandler.PhysicalFile.toString());        
	    }
	    
	    for (int i = 0; i < Icc.TagCount; i++)
	    {
	        if (Icc.TagPtrs[i] != null)
	        {
	            cmsTagTypeHandler TypeHandler = Icc.TagTypeHandlers[i];
	            
	            if (TypeHandler != null)
	            {
	            	TypeHandler.FreePtr.run(TypeHandler, Icc.TagPtrs[i]);       
	            }
	            else
	            {
	            	cmserr._cmsFree(Icc.ContextID, new VirtualPointer(Icc.TagPtrs[i]));
	            }
	        }
	    }
	    
	    if (Icc.IOhandler != null)
	    {      
	        rc &= cmsCloseIOhandler(Icc.IOhandler);
	    }
	    
//#ifdef RAW_C
	    cmserr._cmsFree(Icc.ContextID, new VirtualPointer(Icc)); // Free placeholder memory
//#endif
	    return rc;
	}
	
	// -------------------------------------------------------------------------------------------------------------------
	
	// Returns TRUE if a given tag is supported by a plug-in
	private static boolean IsTypeSupported(cmsTagDescriptor TagDescriptor, int Type)
	{
	    int nMaxTypes = TagDescriptor.nSupportedTypes;
	    if (nMaxTypes >= lcms2_plugin.MAX_TYPES_IN_LCMS_PLUGIN)
	    {
	    	nMaxTypes = lcms2_plugin.MAX_TYPES_IN_LCMS_PLUGIN;
	    }
	    
	    for (int i=0; i < nMaxTypes; i++)
	    {
	        if (Type == TagDescriptor.SupportedTypes[i])
	        {
	        	return true; 
	        }
	    }
	    
	    return false;
	}
	
	// That's the main read function
	public static Object cmsReadTag(cmsHPROFILE hProfile, int sig)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile; 
	    cmsIOHANDLER io = Icc.IOhandler;
	    cmsTagTypeHandler TypeHandler;
	    cmsTagDescriptor  TagDescriptor;
	    int BaseType;
	    int Offset, TagSize;
	    int[] ElemCount = new int[1];
	    int n;
	    
		n = _cmsSearchTag(Icc, sig, true);
		if (n < 0)
		{
			return null; // Not found, return NULL
		}
		
		// If the element is already in memory, return the pointer
		if (Icc.TagPtrs[n] != null)
		{
			if (Icc.TagSaveAsRaw[n])
			{
				return null; // We don't support read raw tags as cooked
			}
			return Icc.TagPtrs[n];
		}
		
		// We need to read it. Get the offset and size to the file
	    Offset    = Icc.TagOffsets[n];
	    TagSize   = Icc.TagSizes[n]; 
	    
	    // Seek to its location
	    if (!io.Seek.run(io, Offset))
	    {
	    	return null;
	    }
	    
	    // Search for support on this tag
	    TagDescriptor = cmstypes._cmsGetTagDescriptor(sig);
	    if (TagDescriptor == null)
	    {
	    	return null; // Unsupported. 
	    }
	    
	    // if supported, get type and check if in list
	    BaseType = cmsplugin._cmsReadTypeBase(io);
	    if (BaseType == 0)
	    {
	    	return null;
	    }
	    
	    if (!IsTypeSupported(TagDescriptor, BaseType))
	    {
	    	return null;
	    }
	    
	    TagSize -= 8; // Already read by the type base logic
	    
	    // Get type handler
	    TypeHandler = cmstypes._cmsGetTagTypeHandler(BaseType);
	    if (TypeHandler == null)
	    {
	    	return null;
	    }
	    
	    // Read the tag
	    Icc.TagTypeHandlers[n] = TypeHandler;
	    Icc.TagPtrs[n] = TypeHandler.ReadPtr.run(TypeHandler, io, ElemCount, TagSize);
	    
	    // The tag type is supported, but something wrong happend and we cannot read the tag.
	    // let know the user about this (although it is just a warning)
	    if (Icc.TagPtrs[n] == null)
	    {
	    	StringBuffer String = new StringBuffer(4);
			
        	cmserr._cmsTagSignature2String(String, sig);
	        cmserr.cmsSignalError(Icc.ContextID, lcms2_plugin.cmsERROR_CORRUPTION_DETECTED, Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_CORRUPTED_TAG), new Object[]{String});
	        return null;
	    }
	    
	    // This is a weird error that may be a symptom of something more serious, the number of
	    // stored item is actually less than the number of required elements. 
	    if (ElemCount[0] < TagDescriptor.ElemCount)
	    {
	    	StringBuffer String = new StringBuffer(4);
			
        	cmserr._cmsTagSignature2String(String, sig);
        	cmserr.cmsSignalError(Icc.ContextID, lcms2_plugin.cmsERROR_CORRUPTION_DETECTED, Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_INCONSISTANT_COUNT), 
        			new Object[]{String, new Integer(TagDescriptor.ElemCount), new Integer(ElemCount[0])});
	    }
	    
	    // Return the data
	    return Icc.TagPtrs[n];
	}
	
	// Get true type of data
	public static int _cmsGetTagTrueType(cmsHPROFILE hProfile, int sig)
	{
		_cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile; 
		cmsTagTypeHandler TypeHandler;	
		int n;
		
		// Search for given tag in ICC profile directory
		n = _cmsSearchTag(Icc, sig, true);
		if (n < 0)
		{
			return 0; // Not found, return NULL
		}
		
		// Get the handler. The true type is there
		TypeHandler = Icc.TagTypeHandlers[n];
		return TypeHandler.Signature;
	}
	
	// Write a single tag. This just keeps track of the tak into a list of "to be written". If the tag is already
	// in that list, the previous version is deleted.
	public static boolean cmsWriteTag(cmsHPROFILE hProfile, int sig, final Object data)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;  
	    cmsTagTypeHandler TypeHandler = null;
	    cmsTagDescriptor TagDescriptor = null;
	    int Type;
	    int i;
	    double Version;
	    
	    if (data == null)
	    {
	    	cmserr.cmsSignalError(cmsGetProfileContextID(hProfile), lcms2_plugin.cmsERROR_NULL, Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_CANT_WRITE_NULL), null);
	    	return false;
	    }
	    
	    i = _cmsSearchTag(Icc, sig, false);
	    if (i >=0)
	    {
	        if (Icc.TagPtrs[i] != null)
	        {
	            // Already exists. Free previous version
	            if (Icc.TagSaveAsRaw[i])
	            {
	            	cmserr._cmsFree(Icc.ContextID, (VirtualPointer)Icc.TagPtrs[i]);
	            }
	            else
	            {
	                TypeHandler = Icc.TagTypeHandlers[i];
	                TypeHandler.FreePtr.run(TypeHandler, Icc.TagPtrs[i]);       
	            }
	        }
	    }
	    else
	    {
	        // New one
	        i = Icc.TagCount;
	        
	        if (i >= lcms2_internal.MAX_TABLE_TAG)
	        {
	            cmserr.cmsSignalError(Icc.ContextID, lcms2_plugin.cmsERROR_RANGE, Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_TO_MANY_TAGS), 
	            		new Object[]{new Integer(lcms2_internal.MAX_TABLE_TAG)});
	            return false;
	        }
	        
	        Icc.TagCount++;
	    }
	    
	    // This is not raw
	    Icc.TagSaveAsRaw[i] = false;
	    
	    // This is not a link
	    Icc.TagLinked[i] = 0;
	    
	    // Get information about the TAG. 
	    TagDescriptor = cmstypes._cmsGetTagDescriptor(sig);
	    if (TagDescriptor == null)
	    {
	    	cmserr.cmsSignalError(Icc.ContextID, lcms2_plugin.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_UNSUPPORTED_TAG), 
	    			new Object[]{new Integer(sig)});
	        return false;
	    }
	    
	    // Now we need to know which type to use. It depends on the version. 
	    Version = cmsGetProfileVersion(hProfile);
	    if (TagDescriptor.DecideType != null)
	    {
	        // Let the tag descriptor to decide the type base on depending on
	        // the data. This is useful for example on parametric curves, where 
	        // curves specified by a table cannot be saved as parametric and needs
	        // to be revented to single v2-curves, even on v4 profiles.
	    	
	        Type = TagDescriptor.DecideType.run(Version, data);
	    }
	    else
	    {
	        Type = TagDescriptor.SupportedTypes[0];
	    }
	    
	    // Does the tag support this type?
	    if (!IsTypeSupported(TagDescriptor, Type))
	    {
	        cmserr.cmsSignalError(Icc.ContextID, lcms2_plugin.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_UNSUPPORTED_TYPE_FOR_TAG), 
	        		new Object[]{new Integer(Type), new Integer(sig)});
	        return false;
	    }
	    
	    // Does we have a handler for this type?
	    TypeHandler = cmstypes._cmsGetTagTypeHandler(Type);
	    if (TypeHandler == null)
	    {
	        cmserr.cmsSignalError(Icc.ContextID, lcms2_plugin.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_UNSUPPORTED_TYPE_FOR_TAG), 
	        		new Object[]{new Integer(Type), new Integer(sig)});
	        return false; // Should never happen
	    }
	    
	    // Fill fields on icc structure
	    Icc.TagTypeHandlers[i]  = TypeHandler;
	    Icc.TagNames[i]         = sig;
	    Icc.TagSizes[i]         = 0;
	    Icc.TagOffsets[i]       = 0;
	    Icc.TagPtrs[i]          = TypeHandler.DupPtr.run(TypeHandler, data, TagDescriptor.ElemCount); 
	    
	    if (Icc.TagPtrs[i] == null)
	    {
	        TypeHandler.DupPtr.run(TypeHandler, data, TagDescriptor.ElemCount);
	        cmserr.cmsSignalError(Icc.ContextID, lcms2_plugin.cmsERROR_CORRUPTION_DETECTED, Utility.LCMS_Resources.getString(LCMSResource.CMSIO0_MALFORMED_TYPE_FOR_TAG), 
	        		new Object[]{new Integer(Type), new Integer(sig)});
	        
	        return false;
	    }
	    
	    return true;
	}
	
	// Read and write raw data. The only way those function would work and keep consistence with normal read and write 
	// is to do an additional step of serialization. That means, readRaw would issue a normal read and then convert the obtained
	// data to raw bytes by using the "write" serialization logic. And vice-versa. I know this may end in situations where
	// raw data written does not exactly correspond with the raw data proposed to cmsWriteRaw data, but this approach allows
	// to write a tag as raw data and the read it as handled.

	public static int cmsReadRawTag(cmsHPROFILE hProfile, int sig, byte[] data, int BufferSize)
	{
		_cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;  
	    Object Object; 
	    int i;
	    cmsIOHANDLER MemIO;
	    cmsTagTypeHandler TypeHandler = null;
	    cmsTagDescriptor TagDescriptor = null;
	    int rc;
	    int Offset, TagSize;
	    
		// Search for given tag in ICC profile directory
		i = _cmsSearchTag(Icc, sig, true);
		if (i < 0)
		{
			return 0; // Not found, return 0
		}
		
		// It is already read?
	    if (Icc.TagPtrs[i] == null)
	    {
	        // No yet, get original position
	        Offset   = Icc.TagOffsets[i];
	        TagSize  = Icc.TagSizes[i];
	        
	        // read the data directly, don't keep copy
			if (data != null)
			{
				if (BufferSize < TagSize)
				{
					TagSize = BufferSize;
				}
				
	            if (!Icc.IOhandler.Seek.run(Icc.IOhandler, Offset))
	            {
	            	return 0;
	            }
	            if (Icc.IOhandler.Read.run(Icc.IOhandler, data, 1, TagSize) == 0)
	            {
	            	return 0;
	            }
			}
			
	        return Icc.TagSizes[i];
	    }
	    
	    // The data has been already read, or written. But wait!, maybe the user choose to save as
	    // raw data. In this case, return the raw data directly
	    if (Icc.TagSaveAsRaw[i])
	    {
			if (data != null)
			{
				TagSize  = Icc.TagSizes[i];
				if (BufferSize < TagSize)
				{
					TagSize = BufferSize;
				}
				
				((VirtualPointer)Icc.TagPtrs[i]).readByteArray(data, 0, TagSize, false, false);
			}
			
	        return Icc.TagSizes[i];
	    }
	    
	    // Already readed, or previously set by cmsWriteTag(). We need to serialize that 
	    // data to raw in order to maintain consistency.
	    Object = cmsReadTag(hProfile, sig);
	    if (Object == null)
	    {
	    	return 0;
	    }
	    
	    // Now we need to serialize to a memory block: just use a memory iohandler
	    
		if (data == null)
		{
			MemIO = cmsOpenIOhandlerFromNULL(cmsGetProfileContextID(hProfile));
		}
		else
		{
			MemIO = cmsOpenIOhandlerFromMem(cmsGetProfileContextID(hProfile), data, BufferSize, 'w');
		}
	    if (MemIO == null)
	    {
	    	return 0;
	    }
	    
	    // Obtain type handling for the tag
	    TypeHandler = Icc.TagTypeHandlers[i];
	    TagDescriptor = cmstypes._cmsGetTagDescriptor(sig);
	    
	    // Serialize
	    if (!TypeHandler.WritePtr.run(TypeHandler, MemIO, Object, TagDescriptor.ElemCount))
	    {
	    	return 0;
	    }
	    
	    // Get Size and close
	    rc = MemIO.Tell.run(MemIO);
	    cmsCloseIOhandler(MemIO); // Ignore return code this time
	    
	    return rc;
	}
	
	// Similar to the anterior. This function allows to write directly to the ICC profile any data, without
	// checking anything. As a rule, mixing Raw with cooked doesn't work, so writing a tag as raw and then reading 
	// it as cooked without serializing does result into an error. If that is wha you want, you will need to dump
	// the profile to memory or disk and then reopen it.
	public static boolean cmsWriteRawTag(cmsHPROFILE hProfile, int sig, final byte[] data, int Size)
	{
	    _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;  
	    int i;
	    int[] temp = new int[1];
	    
		if (!_cmsNewTag(Icc, sig, temp))
		{
			return false;
		}
		i = temp[0];
		
	    // Mark the tag as being written as RAW
	    Icc.TagSaveAsRaw[i] = true;
	    Icc.TagNames[i]     = sig;
	    Icc.TagLinked[i]    = 0;
	    
	    // Keep a copy of the block
	    Icc.TagPtrs[i]  = cmserr._cmsDupMem(Icc.ContextID, new VirtualPointer(data), Size);
	    Icc.TagSizes[i] = Size;
	    
	    return true;
	}
	
	// Using this function you can collapse several tag entries to the same block in the profile
	public static boolean cmsLinkTag(cmsHPROFILE hProfile, int sig, int dest)
	{
	     _cmsICCPROFILE Icc = (_cmsICCPROFILE)hProfile;
	    int i;
	    int[] temp = new int[1];
	    
		if (!_cmsNewTag(Icc, sig, temp))
		{
			return false;
		}
		i = temp[0];
		
	    // Keep necessary information
	    Icc.TagSaveAsRaw[i] = false;
	    Icc.TagNames[i]     = sig;
	    Icc.TagLinked[i]    = dest;
	    
	    Icc.TagPtrs[i]    = null;
	    Icc.TagSizes[i]   = 0;
	    Icc.TagOffsets[i] = 0;
	    
	    return true;
	}
}
