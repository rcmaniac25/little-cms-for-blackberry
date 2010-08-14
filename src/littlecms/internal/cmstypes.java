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

import littlecms.internal.helper.VirtualPointer;
import littlecms.internal.lcms2.cmsCIEXYZ;
import littlecms.internal.lcms2.cmsCIExyYTRIPLE;
import littlecms.internal.lcms2.cmsIOHANDLER;
import littlecms.internal.lcms2_plugin.cmsPluginBase;
import littlecms.internal.lcms2_plugin.cmsPluginTagType;
import littlecms.internal.lcms2_plugin.cmsTagTypeHandler;

/**
 * Tag Serialization  ----------------------------------------------------------------------------- 
 * This file implements every single tag and tag type as described in the ICC spec. Some types
 * have been deprecated, like ncl and Data. There is no implementation for those types as there
 * are no profiles holding them. The programmer can also extend this list by defining his own types
 * by using the appropiate plug-in. There are three types of plug ins regarding that. First type
 * allows to define new tags using any existing type. Next plug-in type allows to define new types
 * and the third one is very specific: allows to extend the number of elements in the multiprofile
 * elements special type. 
 * --------------------------------------------------------------------------------------------------
 */
class cmstypes
{
	// Some broken types
	public static final int cmsCorbisBrokenXYZtype = 0x17A505B8;
	public static final int cmsMonacoBrokenCurveType = 0x9478ee00;
	
	// This is the linked list that keeps track of the defined types
	public static class _cmsTagTypeLinkedList
	{
		public static final int SIZE = cmsTagTypeHandler.SIZE;
		
		public cmsTagTypeHandler Handler;
		public _cmsTagTypeLinkedList Next;
	}
	
	// Register a new type handler. This routine is shared between normal types and MPE
	private static boolean RegisterTypesPlugin(cmsPluginBase Data, _cmsTagTypeLinkedList LinkedList, int DefaultListCount)
	{
	    cmsPluginTagType Plugin = (cmsPluginTagType)Data;
	    _cmsTagTypeLinkedList pt, Anterior = null;
	    
	    // Calling the function with NULL as plug-in would unregister the plug in.
	    if (Data == null)
	    {
	    	getTagTypeListAtIndex(LinkedList, DefaultListCount-1).Next = null;
	        return true;
	    }
	    
	    pt = Anterior = LinkedList; 
	    while (pt != null)
	    {
	        if (Plugin.Handler.Signature == pt.Handler.Signature)
	        {
	            pt.Handler = Plugin.Handler; // Replace old behaviour. 
	            // Note that since no memory is allocated, unregister does not
	            // reset this action. 
	            return true;
	        }   
	        
	        Anterior = pt;          
	        pt = pt.Next;
	    }
	    
	    // Registering happens in plug-in memory pool
//#ifdef RAW_C
	    pt = (_cmsTagTypeLinkedList)cmsplugin._cmsPluginMalloc(/*sizeof(_cmsTagTypeLinkedList)*/_cmsTagTypeLinkedList.SIZE).getDeserializedType(_cmsTagTypeLinkedList.class);
	    if (pt == null)
	    {
	    	return false;
	    }
//#else
	    pt = new _cmsTagTypeLinkedList();
//#endif
	    
	    pt.Handler   = Plugin.Handler;  
	    pt.Next      = null;
	    
	    if (Anterior != null)
	    {
	    	Anterior.Next = pt;
	    }
	    
	    return true;
	}
	
	private static _cmsTagTypeLinkedList getTagTypeListAtIndex(_cmsTagTypeLinkedList list, int index)
	{
		_cmsTagTypeLinkedList pt;
		int count = 0;
	    
	    for (pt = list; pt != null; pt = pt.Next, count++)
	    {
	    	if(count == index)
	    	{
	    		return pt;
	    	}
	    }
	    
	    return null;
	}
	
	// Return handler for a given type or NULL if not found. Shared between normal types and MPE
	private static cmsTagTypeHandler GetHandler(int sig, _cmsTagTypeLinkedList LinkedList)
	{
	    _cmsTagTypeLinkedList pt;
	    
	    for (pt = LinkedList; pt != null; pt = pt.Next)
	    {
	    	if (sig == pt.Handler.Signature)
	    	{
	    		return pt.Handler;
	    	}
	    }
	    
	    return null;
	}
	
	// Auxiliar to convert UTF-32 to UTF-16 in some cases
	private static boolean _cmsWriteWCharArray(cmsIOHANDLER io, int n, final char[] Array)
	{
	    lcms2_internal._cmsAssert(io != null, "io != null");
	    lcms2_internal._cmsAssert(Array != null, "Array != null");
	    
	    for (int i = 0; i < n; i++)
	    {
	        if (!cmsplugin._cmsWriteUInt16Number(io, (short)Array[i]))
	        {
	        	return false;
	        }
	    }
	    
	    return true;
	}
	
	// To deal with position tables
	public static interface PositionTableEntryFn
	{
		public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, Object Cargo, int n, int SizeOfTag);
	}
	
	// Helper function to deal with position tables as decribed in several addendums to ICC spec 4.2
	// A table of n elements is written, where first comes n records containing offsets and sizes and
	// then a block containing the data itself. This allows to reuse same data in more than one entry
	private static boolean ReadPositionTable(cmsTagTypeHandler self, cmsIOHANDLER io, int Count, int BaseOffset, Object Cargo, 
			PositionTableEntryFn ElementFn)
	{
	    int i;
//#ifdef RAW_C
	    VirtualPointer ElementOffsets = null, ElementSizes = null;
	    VirtualPointer.TypeProcessor offProc, sizeProc;
//#else
	    int[] ElementOffsets = null, ElementSizes = null;
//#endif
	    
	    // Let's take the offsets to each element
//#ifdef RAW_C
	    ElementOffsets = cmserr._cmsCalloc(io.ContextID, Count, /*sizeof(cmsUInt32Number *)*/4);
	    if (ElementOffsets == null)
	    {
	    	if (ElementOffsets != null)
		    {
		    	cmserr._cmsFree(io.ContextID, ElementOffsets);
		    }
		    if (ElementSizes != null)
		    {
		    	cmserr._cmsFree(io.ContextID, ElementSizes);
		    }
		    return false;
	    }
	    offProc = ElementOffsets.getProcessor();
//#else
	    ElementOffsets = new int[Count];
//#endif
	    
//#ifdef RAW_C
	    ElementSizes = cmserr._cmsCalloc(io.ContextID, Count, /*sizeof(cmsUInt32Number *)*/4);
	    if (ElementSizes == null)
	    {
	    	if (ElementOffsets != null)
		    {
		    	cmserr._cmsFree(io.ContextID, ElementOffsets);
		    }
		    if (ElementSizes != null)
		    {
		    	cmserr._cmsFree(io.ContextID, ElementSizes);
		    }
		    return false;
	    }
	    sizeProc = ElementSizes.getProcessor();
//#else
	    ElementSizes = new int[Count];
//#endif
	    
	    int[] temp = new int[1];
	    
	    for (i = 0; i < Count; i++)
	    {
	        if (!cmsplugin._cmsReadUInt32Number(io, temp))
	        {
//#ifdef RAW_C
	        	if (ElementOffsets != null)
	    	    {
	    	    	cmserr._cmsFree(io.ContextID, ElementOffsets);
	    	    }
	    	    if (ElementSizes != null)
	    	    {
	    	    	cmserr._cmsFree(io.ContextID, ElementSizes);
	    	    }
//#endif
	    	    return false;
	        }
//#ifdef RAW_C
	        offProc.write(temp[0] + BaseOffset, true);
//#else
	        ElementOffsets[i] = temp[0] + BaseOffset;
//#endif
	        if (!cmsplugin._cmsReadUInt32Number(io, temp))
	        {
//#ifdef RAW_C
	        	if (ElementOffsets != null)
	    	    {
	    	    	cmserr._cmsFree(io.ContextID, ElementOffsets);
	    	    }
	    	    if (ElementSizes != null)
	    	    {
	    	    	cmserr._cmsFree(io.ContextID, ElementSizes);
	    	    }
//#endif
	    	    return false;
	        }
//#ifdef RAW_C
	        sizeProc.write(temp[0], true);
//#else
	        ElementSizes[i] = temp[0];
//#endif
	    }
//#ifdef RAW_C
	    ElementOffsets.setPosition(0);
	    ElementSizes.setPosition(0);
//#endif
	    
	    // Seek to each element and read it
	    for (i = 0; i < Count; i++)
	    {
//#ifdef RAW_C
	        if (!io.Seek.run(io, offProc.readInt32(true)))
	        {
	        	if (ElementOffsets != null)
	    	    {
	    	    	cmserr._cmsFree(io.ContextID, ElementOffsets);
	    	    }
	    	    if (ElementSizes != null)
	    	    {
	    	    	cmserr._cmsFree(io.ContextID, ElementSizes);
	    	    }
//#else
	    	if (!io.Seek.run(io, ElementOffsets[i]))
	    	{
//#endif
	    	    return false;
	        }
	        
	        // This is the reader callback
//#ifdef RAW_C
	        if (!ElementFn.run(self, io, Cargo, i, sizeProc.readInt32(true)))
	        {
	        	if (ElementOffsets != null)
	    	    {
	    	    	cmserr._cmsFree(io.ContextID, ElementOffsets);
	    	    }
	    	    if (ElementSizes != null)
	    	    {
	    	    	cmserr._cmsFree(io.ContextID, ElementSizes);
	    	    }
//#else
    	    if (!ElementFn.run(self, io, Cargo, i, ElementSizes[i]))
	        {
//#endif
	    	    return false;
	        }
	    }

	    // Success
//#ifdef RAW_C
	    if (ElementOffsets != null)
	    {
	    	cmserr._cmsFree(io.ContextID, ElementOffsets);
	    }
	    if (ElementSizes != null)
	    {
	    	cmserr._cmsFree(io.ContextID, ElementSizes);
	    }
//#endif
	    return true;
	}
	        
    // Same as anterior, but for write position tables
    private static boolean WritePositionTable(cmsTagTypeHandler self, cmsIOHANDLER io, int SizeOfTag, int Count, int BaseOffset, Object Cargo, 
    		PositionTableEntryFn ElementFn)
    {
        int i;
        int DirectoryPos, CurrentPos, Before;
//#ifdef RAW_C
        VirtualPointer ElementOffsets = null, ElementSizes = null;
        VirtualPointer.TypeProcessor offProc, sizeProc;
//#else
        int[] ElementOffsets = null, ElementOffsets = null;
//#endif
        
        // Create table
//#ifdef RAW_C
        ElementOffsets = cmserr._cmsCalloc(io.ContextID, Count, /*sizeof(cmsUInt32Number *)*/4);
        if (ElementOffsets == null)
        {
        	if (ElementOffsets != null)
            {
            	cmserr._cmsFree(io.ContextID, ElementOffsets);
            }
            if (ElementSizes != null)
            {
            	cmserr._cmsFree(io.ContextID, ElementSizes);
            }
            return false;
        }
        offProc = ElementOffsets.getProcessor();
//#else
        ElementOffsets = new int[Count];
//#endif
        
//#ifdef RAW_C
        ElementSizes = cmserr._cmsCalloc(io.ContextID, Count, /*sizeof(cmsUInt32Number *)*/4);
        if (ElementSizes == null)
        {
        	if (ElementOffsets != null)
            {
            	cmserr._cmsFree(io.ContextID, ElementOffsets);
            }
            if (ElementSizes != null)
            {
            	cmserr._cmsFree(io.ContextID, ElementSizes);
            }
            return false;
        }
        sizeProc = ElementSizes.getProcessor();
//#else
        ElementSizes = new int[Count];
//#endif
        
        // Keep starting position of curve offsets
        DirectoryPos = io.Tell.run(io);
        
        // Write a fake directory to be filled latter on
        for (i = 0; i < Count; i++)
        {
            if (!cmsplugin._cmsWriteUInt32Number(io, 0))
            {
            	// Offset
//#ifdef RAW_C
            	if (ElementOffsets != null)
                {
                	cmserr._cmsFree(io.ContextID, ElementOffsets);
                }
                if (ElementSizes != null)
                {
                	cmserr._cmsFree(io.ContextID, ElementSizes);
                }
//#endif
                return false;
            }
            if (!cmsplugin._cmsWriteUInt32Number(io, 0))
            {
            	// size
//#ifdef RAW_C
            	if (ElementOffsets != null)
                {
                	cmserr._cmsFree(io.ContextID, ElementOffsets);
                }
                if (ElementSizes != null)
                {
                	cmserr._cmsFree(io.ContextID, ElementSizes);
                }
//#endif
                return false;
            }
        }
        
        // Write each element. Keep track of the size as well.
        for (i = 0; i < Count; i++)
        {
            Before = io.Tell.run(io);
//#ifdef RAW_C
            offProc.write(Before - BaseOffset, true);
//#else
            ElementOffsets[i] = Before - BaseOffset;
//#endif
            
            // Callback to write...
            if (!ElementFn.run(self, io, Cargo, i, SizeOfTag))
            {
//#ifdef RAW_C
            	if (ElementOffsets != null)
                {
                	cmserr._cmsFree(io.ContextID, ElementOffsets);
                }
                if (ElementSizes != null)
                {
                	cmserr._cmsFree(io.ContextID, ElementSizes);
                }
//#endif
                return false;
            }
            
            // Now the size
//#ifdef RAW_C
            sizeProc.write(io.Tell.run(io) - Before, true);
//#else
            ElementSizes[i] = io.Tell.run(io) - Before;
//#endif
        }
        
        // Write the directory
        CurrentPos = io.Tell.run(io);
        if (!io.Seek.run(io, DirectoryPos))
        {
//#ifdef RAW_C
        	if (ElementOffsets != null)
            {
            	cmserr._cmsFree(io.ContextID, ElementOffsets);
            }
            if (ElementSizes != null)
            {
            	cmserr._cmsFree(io.ContextID, ElementSizes);
            }
//#endif
            return false;
        }
        
//#ifdef RAW_C
        ElementOffsets.setPosition(0);
        ElementSizes.setPosition(0);
//#endif
        
        for (i = 0; i < Count; i++)
        {
//#ifndef RAW_C
            if (!cmsplugin._cmsWriteUInt32Number(io, ElementOffsets[i]))
            {
//#else
        	if (!cmsplugin._cmsWriteUInt32Number(io, offProc.readInt32(true)))
            {
            	if (ElementOffsets != null)
                {
                	cmserr._cmsFree(io.ContextID, ElementOffsets);
                }
                if (ElementSizes != null)
                {
                	cmserr._cmsFree(io.ContextID, ElementSizes);
                }
//#endif
                return false;
            }
//#ifndef RAW_C
            if (!cmsplugin._cmsWriteUInt32Number(io, ElementSizes[i]))
            {
//#else
        	if (!cmsplugin._cmsWriteUInt32Number(io, sizeProc.readInt32(true)))
            {
            	if (ElementOffsets != null)
                {
                	cmserr._cmsFree(io.ContextID, ElementOffsets);
                }
                if (ElementSizes != null)
                {
                	cmserr._cmsFree(io.ContextID, ElementSizes);
                }
//#endif
                return false; 
            }
        }
        
        if (!io.Seek.run(io, CurrentPos))
        {
//#ifdef RAW_C
        	if (ElementOffsets != null)
            {
            	cmserr._cmsFree(io.ContextID, ElementOffsets);
            }
            if (ElementSizes != null)
            {
            	cmserr._cmsFree(io.ContextID, ElementSizes);
            }
//#endif
            return false;
        }
        
//#ifdef RAW_C
        if (ElementOffsets != null)
        {
        	cmserr._cmsFree(io.ContextID, ElementOffsets);
        }
        if (ElementSizes != null)
        {
        	cmserr._cmsFree(io.ContextID, ElementSizes);
        }
//#endif
        return true;
    }
    
	// ********************************************************************************
	// Type XYZ. Only one value is allowed
	// ********************************************************************************
	
	//The XYZType contains an array of three encoded values for the XYZ tristimulus
	//values. Tristimulus values must be non-negative. The signed encoding allows for 
	//implementation optimizations by minimizing the number of fixed formats.
    
	private static Object Type_XYZ_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
	    cmsCIEXYZ xyz;
//#ifdef RAW_C
	    VirtualPointer xyzP;
//#endif
	    
	    nItems[0] = 0;
//#ifdef RAW_C
	    xyz = (cmsCIEXYZ)(xyzP = cmserr._cmsMallocZero(self.ContextID, /*sizeof(cmsCIEXYZ)*/cmsCIEXYZ.SIZE)).getProcessor().readObject(cmsCIEXYZ.class);
	    if (xyz == null)
	    {
	    	return null;
	    }
//#else
	    xyz = new cmsCIEXYZ();
//#endif
	    
	    if (!cmsplugin._cmsReadXYZNumber(io, xyz))
	    {
//#ifdef RAW_C
	    	cmserr._cmsFree(self.ContextID, xyzP);
//#endif
	        return null;
	    }
	    
	    nItems[0] = 1;
	    return xyz;
	}
	
	private static boolean Type_XYZ_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
	    return cmsplugin._cmsWriteXYZNumber(io, (cmsCIEXYZ)Ptr);
	}
	
	private static Object Type_XYZ_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
//#ifdef RAW_C
	    return cmserr._cmsDupMem(self.ContextID, new VirtualPointer(Ptr), /*sizeof(cmsCIEXYZ)*/cmsCIEXYZ.SIZE).getProcessor().readObject(cmsCIEXYZ.class);
//#else
	    cmsCIEXYZ xyz = new cmsCIEXYZ();
	    cmsCIEXYZ oxyz = (cmsCIEXYZ)Ptr;
	    xyz.X = oxyz.X;
	    xyz.Y = oxyz.Y;
	    xyz.Z = oxyz.Z;
	    return xyz;
//#endif
	}

	private static void Type_XYZ_Free(cmsTagTypeHandler self, Object Ptr)
	{
//#ifdef RAW_C
	    cmserr._cmsFree(self.ContextID, new VirtualPointer(Ptr));
//#endif
	}
	
	private static int DecideXYZtype(double ICCVersion, final Object Data)
	{
	    return lcms2.cmsSigXYZType;
	}
	
	// ********************************************************************************
	// Type chromaticity. Only one value is allowed
	// ********************************************************************************
	// The chromaticity tag type provides basic chromaticity data and type of 
	// phosphors or colorants of a monitor to applications and utilities.
	
	private static Object Type_Chromaticity_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
	    cmsCIExyYTRIPLE chrm;
	    short nChans, Table;
	    short[] temp;
	    
	    nItems[0] = 0;
//#ifdef RAW_C
	    VirtualPointer chrmP;
	    chrm =  (cmsCIExyYTRIPLE)(chrmP = cmserr._cmsMallocZero(self.ContextID, /*sizeof(cmsCIExyYTRIPLE)*/cmsCIExyYTRIPLE.SIZE)).getProcessor().readObject(cmsCIExyYTRIPLE.class);
	    if (chrm == null)
	    {
	    	return null;
	    }
//#else
	    chrm = new cmsCIExyYTRIPLE();
//#endif
	    
	    temp = new short[1];
	    if (!cmsplugin._cmsReadUInt16Number(io, temp))
	    {
//#ifdef RAW_C
		    cmserr._cmsFree(self.ContextID, chrmP);
//#endif
		    return null;
	    }
	    nChans = temp[0];
	    
	    // Let's recover from a bug introduced in early versions of lcms1
	    if (nChans == 0 && SizeOfTag == 32)
	    {
	        if (!cmsplugin._cmsReadUInt16Number(io, null))
	        {
//#ifdef RAW_C
	        	cmserr._cmsFree(self.ContextID, chrmP);
//#endif
	        	return null;
	        }
	        if (!cmsplugin._cmsReadUInt16Number(io, temp))
	        {
//#ifdef RAW_C
	        	cmserr._cmsFree(self.ContextID, chrmP);
//#endif
	        	return null;
	        }
	        nChans = temp[0];
	    }
	    
	    if (nChans != 3)
	    {
//#ifdef RAW_C
		    cmserr._cmsFree(self.ContextID, chrmP);
//#endif
		    return null;
	    }
	    
	    if (!cmsplugin._cmsReadUInt16Number(io, temp))
	    {
//#ifdef RAW_C
		    cmserr._cmsFree(self.ContextID, chrmP);
//#endif
		    return null;
	    }
	    Table = temp[0];
	    
	    double[] temp2 = new double[1];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp2))
	    {
//#ifdef RAW_C
		    cmserr._cmsFree(self.ContextID, chrmP);
//#endif
		    return null;
	    }
	    chrm.Red.x = temp2[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp2))
	    {
//#ifdef RAW_C
		    cmserr._cmsFree(self.ContextID, chrmP);
//#endif
		    return null;
	    }
	    chrm.Red.y = temp2[0];
	    
	    chrm.Red.Y = 1.0;
	    
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp2))
	    {
//#ifdef RAW_C
		    cmserr._cmsFree(self.ContextID, chrmP);
//#endif
		    return null;
	    }
	    chrm.Green.x = temp2[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp2))
	    {
//#ifdef RAW_C
		    cmserr._cmsFree(self.ContextID, chrmP);
//#endif
		    return null;
	    }
	    chrm.Green.y = temp2[0];
	    
	    chrm.Green.Y = 1.0;
	    
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp2))
	    {
//#ifdef RAW_C
		    cmserr._cmsFree(self.ContextID, chrmP);
//#endif
		    return null;
	    }
	    chrm.Blue.x = temp2[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp2))
	    {
//#ifdef RAW_C
		    cmserr._cmsFree(self.ContextID, chrmP);
//#endif
		    return null;
	    }
	    chrm.Blue.y = temp2[0];
	    
	    chrm.Blue.Y = 1.0;
	    
	    nItems[0] = 1;
	    return chrm;
	}
	
	private static boolean SaveOneChromaticity(double x, double y, cmsIOHANDLER io)
	{
	    if (!cmsplugin._cmsWriteUInt32Number(io, cmsplugin._cmsDoubleTo15Fixed16(x)))
	    {
	    	return false;     
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, cmsplugin._cmsDoubleTo15Fixed16(y)))
	    {
	    	return false;     
	    }
	    
	    return true;
	}
	
	private static boolean Type_Chromaticity_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
	    cmsCIExyYTRIPLE chrm = (cmsCIExyYTRIPLE)Ptr;
	    
	    if (!_cmsWriteUInt16Number(io, 3)) // nChannels
	    {
	    	return false;
	    }
	    if (!_cmsWriteUInt16Number(io, 0)) // nChannels
	    {
	    	return false;
	    }
	    
	    if (!SaveOneChromaticity(chrm.Red.x, chrm.Red.y, io))
	    {
	    	return false;
	    }
	    if (!SaveOneChromaticity(chrm.Green.x, chrm.Green.y, io))
	    {
	    	return false;
	    }
	    if (!SaveOneChromaticity(chrm.Blue.x, chrm.Blue.y, io))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	private static Object Type_Chromaticity_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
//#ifdef RAW_C
	    return cmserr._cmsDupMem(self.ContextID, new VirtualPointer(Ptr), /*sizeof(cmsCIExyYTRIPLE)*/cmsCIExyYTRIPLE.SIZE).getProcessor().readObject(cmsCIExyYTRIPLE.class);
//#else
	    cmsCIExyYTRIPLE xyz = new cmsCIExyYTRIPLE();
	    cmsCIExyYTRIPLE oxyz = (cmsCIExyYTRIPLE)Ptr;
	    xyz.Red.x = oxyz.Red.x;
	    xyz.Red.y = oxyz.Red.y;
	    xyz.Red.Y = oxyz.Red.Y;
	    
	    xyz.Green.x = oxyz.Green.x;
	    xyz.Green.y = oxyz.Green.y;
	    xyz.Green.Y = oxyz.Green.Y;
	    
	    xyz.Blue.x = oxyz.Blue.x;
	    xyz.Blue.y = oxyz.Blue.y;
	    xyz.Blue.Y = oxyz.Blue.Y;
	    return xyz;
//#endif
	}

	private static void Type_Chromaticity_Free(cmsTagTypeHandler self, Object Ptr)
	{
//#ifdef RAW_C
	    cmserr._cmsFree(self.ContextID, new VirtualPointer(Ptr));
//#endif
	}
	
	//TODO #418
}
