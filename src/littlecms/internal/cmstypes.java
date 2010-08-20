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
import littlecms.internal.lcms2.cmsCIEXYZ;
import littlecms.internal.lcms2.cmsCIExyYTRIPLE;
import littlecms.internal.lcms2.cmsICCData;
import littlecms.internal.lcms2.cmsIOHANDLER;
import littlecms.internal.lcms2.cmsMLU;
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
	// Some types are not internal, thus they need to return public (normal) types.
	// RAW_C support implemented but return type is expected to be cmsUInt8Number[16] (byte[lcms2.cmsMAXCHANNELS]) so just do that.
	// If RAW_C processing is desired then remove the comment marker "//-" or in the case of the preprocessor components replace '-' with '#'
	
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
//-ifdef RAW_C
//-		VirtualPointer xyz;
//-		cmsCIEXYZ temp;
//-else
	    cmsCIEXYZ xyz;
//-endif
	    
	    nItems[0] = 0;
//-ifdef RAW_C
//-	    xyz = cmserr._cmsMallocZero(self.ContextID, /*sizeof(cmsCIEXYZ)*/cmsCIEXYZ.SIZE));
//-	    if (xyz == null)
//-	    {
//-	    	return null;
//-	    }
//-	    temp = new cmsCIEXYZ();
//-else
	    xyz = new cmsCIEXYZ();
//-endif
	    
//-ifdef RAW_C
//-	    if (!cmsplugin._cmsReadXYZNumber(io, temp))
//-else
	    if (!cmsplugin._cmsReadXYZNumber(io, xyz))
//-endif
	    {
//-ifdef RAW_C
//-	    	cmserr._cmsFree(self.ContextID, xyz);
//-endif
	        return null;
	    }
//-ifdef RAW_C
//-	    xyz.getProcessor().write(temp);
//-endif
	    
	    nItems[0] = 1;
	    return xyz;
	}
	
	private static boolean Type_XYZ_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
//-ifdef xyz
//-		VirtualPointer vp = (VirtualPointer)Ptr;
//-		return cmsplugin._cmsWriteXYZNumber(io, (cmsCIEXYZ)vp.getProcessor().readObject(cmsCIEXYZ.class));
//-else
	    return cmsplugin._cmsWriteXYZNumber(io, (cmsCIEXYZ)Ptr);
//-endif
	}
	
	private static Object Type_XYZ_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
//-ifdef RAW_C
//-	    return cmserr._cmsDupMem(self.ContextID, (VirtualPointer)Ptr, /*sizeof(cmsCIEXYZ)*/cmsCIEXYZ.SIZE);
//-else
	    cmsCIEXYZ xyz = new cmsCIEXYZ();
	    cmsCIEXYZ oxyz = (cmsCIEXYZ)Ptr;
	    xyz.X = oxyz.X;
	    xyz.Y = oxyz.Y;
	    xyz.Z = oxyz.Z;
	    return xyz;
//-endif
	}

	private static void Type_XYZ_Free(cmsTagTypeHandler self, Object Ptr)
	{
//-ifdef RAW_C
//-	    cmserr._cmsFree(self.ContextID, (VirtualPointer)Ptr);
//-endif
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
	
	// ********************************************************************************
	// Type cmsSigColorantOrderType
	// ********************************************************************************
	
	// This is an optional tag which specifies the laydown order in which colorants will 
	// be printed on an n-colorant device. The laydown order may be the same as the 
	// channel generation order listed in the colorantTableTag or the channel order of a 
	// colour space such as CMYK, in which case this tag is not needed. When this is not 
	// the case (for example, ink-towers sometimes use the order KCMY), this tag may be 
	// used to specify the laydown order of the colorants.
	
	private static Object Type_ColorantOrderType_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
//-ifdef RAW_C
//-		VirtualPointer ColorantOrder;
//-else
		byte[] ColorantOrder;
//-endif
	    int Count;
	    int[] temp = new int[1];
	    
	    nItems[0] = 0;
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;
	    }
	    Count = temp[0];
	    if (Count > lcms2.cmsMAXCHANNELS)
	    {
	    	return null;
	    }
	    
//-ifdef RAW_C
//-	    ColorantOrder = cmserr._cmsCalloc(self.ContextID, lcms2.cmsMAXCHANNELS, /*sizeof(cmsUInt8Number)*/1);
//-		if (ColorantOrder == null)
//-	    {
//-	    	return null;
//-	    }
//-else
	    ColorantOrder = new byte[lcms2.cmsMAXCHANNELS];
//-endif
	    
	    // We use FF as end marker
//-ifdef RAW_C
//-	    ColorantOrder.set(0xFF, lcms2.cmsMAXCHANNELS * /*sizeof(cmsUInt8Number)*/1);
//-else
	    Arrays.fill(ColorantOrder, (byte)0xFF);
//-endif
	    
//-ifdef RAW_C
//-	    if (io.vpRead(io, ColorantOrder, /*sizeof(cmsUInt8Number)*/1, Count) != Count)
//-	    {
//-	    	cmserr._cmsFree(self.ContextID, ColorantOrder);
//-else
	    if (io.Read.run(io, ColorantOrder, /*sizeof(cmsUInt8Number)*/1, Count) != Count)
	    {
//-endif
	        return null;
	    }
	    
	    nItems[0] = 1;
	    return ColorantOrder;
	}
	
	private static boolean Type_ColorantOrderType_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
//-ifdef RAW_C
//-		VirtualPointer ColorantOrder = (VirtualPointer)Ptr;
//-		int pos = ColorantOrder.getPosition();
//-		VirtualPointer.TypeProcessor proc = ColorantOrder.getProcessor();
//-else
		byte[] ColorantOrder = (byte[])Ptr;
//-endif
	    int i, sz, Count;
	    
	    // Get the length
	    for (Count = i = 0; i < lcms2.cmsMAXCHANNELS; i++)
	    {
//-ifdef RAW_C
//-	        if (proc.readInt8(true) != 0xFF)
//-else
	        if (ColorantOrder[i] != (byte)0xFF)
//-endif
	        {
	        	Count++;
	        }
	    }
//=ifdef RAW_C
//-	    ColorantOrder.setPosition(pos);
//-endif
	    
	    if (!cmsplugin._cmsWriteUInt32Number(io, Count))
	    {
	    	return false;
	    }
	    
	    sz = Count/* * sizeof(cmsUInt8Number)*/;
//-ifdef RAW_C
//-	    if (!io.vpWrite(io, sz, ColorantOrder))
//-else
    	if (!io.Write.run(io, sz, ColorantOrder))
//-endif
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	private static Object Type_ColorantOrderType_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
//-ifdef RAW_C
//-		return cmserr._cmsDupMem(self.ContextID, (VirtualPointer)Ptr, lcms2.cmsMAXCHANNELS/* * sizeof(cmsUInt8Number)*/);
//-else
		return Arrays.copy((byte[])Ptr);
//-endif
	}
	
	private static void Type_ColorantOrderType_Free(cmsTagTypeHandler self, Object Ptr)
	{
//-ifdef RAW_C
//-		cmserr._cmsFree(self.ContextID, (VirtualPointer)Ptr);
//-endif
	}
	
	// ********************************************************************************
	// Type cmsSigS15Fixed16ArrayType
	// ********************************************************************************
	// This type represents an array of generic 4-byte/32-bit fixed point quantity. 
	// The number of values is determined from the size of the tag.
	
	private static Object Type_S15Fixed16_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
//#ifdef RAW_C
		VirtualPointer array_double;
		VirtualPointer.TypeProcessor proc;
//#else
		double[] array_double;
//#endif
	    int i, n;
	    double[] temp = new double[1];
	    
	    nItems[0] = 0;
	    n = SizeOfTag / /*sizeof(cmsUInt32Number)*/4;
//#ifdef RAW_C
	    array_double = cmserr._cmsCalloc(self.ContextID, n, /*sizeof(cmsFloat64Number)*/8);
	    if (array_double == null)
	    {
	    	return null;
	    }
	    proc = array_double.getProcessor();
//#else
	    array_double = new double[n];
//#endif
	    
	    for (i = 0; i < n; i++)
	    {
	        if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	        {
//#ifdef RAW_C
	            cmserr._cmsFree(self.ContextID, array_double);
//#endif
	            return null;
	        }
//#ifdef RAW_C
	        proc.write(temp[0], true);
//#else
	        array_double[i] = temp[0];
//#endif
	    }
//#ifdef RAW_C
	    array_double.setPosition(0);
//#endif
	    
	    nItems[0] = n;
	    return array_double;
	}
	
	private static boolean Type_S15Fixed16_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
//#ifdef RAW_C
		VirtualPointer Value = (VirtualPointer)Ptr;
		VirtualPointer.TypeProcessor proc = Value.getProcessor();
		int pos = Value.getPosition();
//#else
		double[] Value = (double[])Ptr;
//#endif
	    int i;
	    
	    for (i = 0; i < nItems; i++)
	    {
//#ifdef RAW_C
	    	if (!cmsplugin._cmsWrite15Fixed16Number(io, proc.readDouble(true)))
//#else
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, Value[i]))
//#endif
	        {
	        	return false;  
	        }
	    }
//#ifdef RAW_C
	    Value.setPosition(pos);
//#endif
	    
	    return true;
	}
	
	private static Object Type_S15Fixed16_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
//#ifdef RAW_C
		return cmserr._cmsDupMem(self.ContextID, (VirtualPointer)Ptr, n * /*sizeof(cmsFloat64Number)*/8);
//#else
		double[] oval = (double[])Ptr;
		double[] dup = new double[oval.length];
		System.arraycopy(oval, 0, dup, 0, oval.length);
		return dup;
//#endif
	}
	
	private static void Type_S15Fixed16_Free(cmsTagTypeHandler self, Object Ptr)
	{
//#ifdef RAW_C
		cmserr._cmsFree(self.ContextID, (VirtualPointer)Ptr);
//#endif
	}
	
	// ********************************************************************************
	// Type cmsSigU16Fixed16ArrayType
	// ********************************************************************************
	// This type represents an array of generic 4-byte/32-bit quantity. 
	// The number of values is determined from the size of the tag.
	
	private static Object Type_U16Fixed16_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
//#ifdef RAW_C
		VirtualPointer array_double;
		VirtualPointer.TypeProcessor proc;
//#else
		double[] array_double;
//#endif
	    int[] v = new int[1];
	    int i, n;
	    
	    nItems[0] = 0;
	    n = SizeOfTag / /*sizeof(cmsUInt32Number)*/4;
//#ifdef RAW_C
	    array_double = cmserr._cmsCalloc(self.ContextID, n, /*sizeof(cmsFloat64Number)*/8);
	    if (array_double == null)
	    {
	    	return null;
	    }
	    proc = array_double.getProcessor();
//#else
	    array_double = new double[n];
//#endif
	    
	    for (i = 0; i < n; i++)
	    {
	        if (!cmsplugin._cmsReadUInt32Number(io, v))
	        {
//#ifdef RAW_C
	            cmserr._cmsFree(self.ContextID, array_double);
//#endif
	            return null;
	        }
	        
	        // Convert to double
//#ifdef RAW_C
	        proc.write((double)(v[0] / 65536.0), true);
	    }
	    array_double.setPosition(0);
//#else
	    	array_double[i] = (double)(v[0] / 65536.0);
		}
//#endif
	    
	    nItems[0] = n;
	    return array_double;
	}
        
    private static boolean Type_U16Fixed16_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
    {
//#ifdef RAW_C
    	VirtualPointer Value = (VirtualPointer)Ptr;
    	VirtualPointer.TypeProcessor proc = Value.getProcessor();
    	int pos = Value.getPosition();
//#else
    	double[] Value = (double[])Ptr;
//#endif
        int i;
        
        for (i = 0; i < nItems; i++)
        {
//#ifdef RAW_C
            int v = (int)Math.floor(proc.readDouble(true)*65536.0 + 0.5);
//#else
            int v = (int)Math.floor(Value[i]*65536.0 + 0.5);
//#endif
            
            if (!cmsplugin._cmsWriteUInt32Number(io, v))
            {
            	return false;    
            }
        }
//#ifdef RAW_C
        Value.setPosition(pos);
//#endif
        
        return true;
    }
    
    private static Object Type_U16Fixed16_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
//#ifdef RAW_C
		return cmserr._cmsDupMem(self.ContextID, (VirtualPointer)Ptr, n * /*sizeof(cmsFloat64Number)*/8);
//#else
		double[] oval = (double[])Ptr;
		double[] dup = new double[oval.length];
		System.arraycopy(oval, 0, dup, 0, oval.length);
		return dup;
//#endif
	}
	
	private static void Type_U16Fixed16_Free(cmsTagTypeHandler self, Object Ptr)
	{
//#ifdef RAW_C
		cmserr._cmsFree(self.ContextID, (VirtualPointer)Ptr);
//#endif
	}
	
	// ********************************************************************************
	// Type cmsSigSignatureType
	// ********************************************************************************
	//
	// The signatureType contains a four-byte sequence, Sequences of less than four 
	// characters are padded at the end with spaces, 20h. 
	// Typically this type is used for registered tags that can be displayed on many 
	// development systems as a sequence of four characters.
	
	private static Object Type_Signature_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
//-ifdef RAW_C
//-		VirtualPointer SigPtr = cmserr._cmsMalloc(self.ContextID, /*sizeof(cmsSignature)*/4);
//-	    if (SigPtr == null)
//-	    {
//-	    	return null;
//-	    }
//-else
	    Integer SigPtr;
//-endif
	    
	    int[] temp = new int[1];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;
	    }
//-ifdef RAW_C
//-	    SigPtr.getProcessor().write(temp[0]);
//-else
	    SigPtr = new Integer(temp[0]);
//-endif
	    nItems[0] = 1;
	    
	    return SigPtr;
	}
	
	private static boolean Type_Signature_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
//-ifdef RAW_C
//-		VirtualPointer SigPtr = (VirtualPointer)Ptr; 
//-	    
//-	    return cmsplugin._cmsWriteUInt32Number(io, SigPtr.getProcessor().readInt32());
//-else
	    Integer SigPtr = (Integer)Ptr; 
	    
	    return cmsplugin._cmsWriteUInt32Number(io, SigPtr.intValue());
//-endif
	}
	
	private static Object Type_Signature_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
//-ifdef RAW_C
//-		return cmserr._cmsDupMem(self.ContextID, (VirtualPointer)Ptr, n * /*sizeof(cmsSignature)*/4);
//-else
		return new Integer(((Integer)Ptr).intValue());
//-endif
	}
	
	private static void Type_Signature_Free(cmsTagTypeHandler self, Object Ptr)
	{
//-ifdef RAW_C
//-		cmserr._cmsFree(self.ContextID, (VirtualPointer)Ptr);
//-endif
	}
	
	// ********************************************************************************
	// Type cmsSigTextType
	// ********************************************************************************
	//
	// The textType is a simple text structure that contains a 7-bit ASCII text string. 
	// The length of the string is obtained by subtracting 8 from the element size portion 
	// of the tag itself. This string must be terminated with a 00h byte.
	
	private static Object Type_Text_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
//#ifdef RAW_C
		VirtualPointer Text = null;
//#else
		byte[] Text = null;
//#endif
		cmsMLU mlu = null;
		
		// Create a container
	    mlu = cmsnamed.cmsMLUalloc(self.ContextID, 1);
	    if (mlu == null)
	    {
	    	return null;
	    }
	    
	    nItems[0] = 0;
	    
	    // We need to store the "\0" at the end, so +1
//#ifdef RAW_C
	    Text = cmserr._cmsMalloc(self.ContextID, SizeOfTag + 1);
	    if (Text == null)
	    {
	    	if (mlu != null)
		    {
	    		cmsnamed.cmsMLUfree(mlu);
		    }
		    if (Text != null)
		    {
		    	cmserr._cmsFree(self.ContextID, Text);
		    }
		    
		    return null;
	    }
//#else
	    Text = new byte[SizeOfTag + 1];
//#endif
	    
//#ifdef RAW_C
	    if (io.vpRead(io, Text, /*sizeof(char)*/1, SizeOfTag) != SizeOfTag)
//#else
    	if (io.Read.run(io, Text, /*sizeof(char)*/1, SizeOfTag) != SizeOfTag)
//#endif
	    {
	    	if (mlu != null)
		    {
	    		cmsnamed.cmsMLUfree(mlu);
		    }
//#ifdef RAW_C
		    if (Text != null)
		    {
		    	cmserr._cmsFree(self.ContextID, Text);
		    }
//#endif
		    
		    return null;
	    }
	    
	    // Make sure text is properly ended
//#ifdef RAW_C
	    Text.setPosition(SizeOfTag);
	    Text.getProcessor().write((byte)0);
	    Text.setPosition(0);
//#else
	    Text[SizeOfTag] = 0;
//#endif
	    nItems[0] = 1;
	    
	    // Keep the result
//#ifdef RAW_C
	    if (!cmsnamed.cmsMLUsetASCII(mlu, lcsm2.cmsNoLanguage, lcsm2.cmsNoCountry, Text.getProcessor().readString(false, false)))
//#else
    	if (!cmsnamed.cmsMLUsetASCII(mlu, lcsm2.cmsNoLanguage, lcsm2.cmsNoCountry, new String(Text))) //It's ASCII so it should be just fine
//#endif
	    {
	    	if (mlu != null)
		    {
	    		cmsnamed.cmsMLUfree(mlu);
		    }
//#ifdef RAW_C
		    if (Text != null)
		    {
		    	cmserr._cmsFree(self.ContextID, Text);
		    }
//#endif
		    
		    return null;
	    }
	    
//#ifdef RAW_C
	    cmserr._cmsFree(self.ContextID, Text);
//#endif
	    return mlu;
	}
	
	// The conversion implies to choose a language. So, we choose the actual language.
	private static boolean Type_Text_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		cmsMLU mlu = (cmsMLU)Ptr; 
	    int size;
	    boolean rc;
	    StringBuffer Text; //Normally would do a VirtualPointer/byte[] impl but cmsMLUgetASCII takes a StringBuffer
	    
	    // Get the size of the string. Note there is an extra "\0" at the end
	    size = cmsnamed.cmsMLUgetASCII(mlu, lcms2.cmsNoLanguage, lcms2.cmsNoCountry, null, 0);
	    if (size == 0)
	    {
	    	return false; // Cannot be zero!
	    }
	    
	    // Create memory
	    Text = new StringBuffer(size);
	    cmsnamed.cmsMLUgetASCII(mlu, lcms2.cmsNoLanguage, lcms2.cmsNoCountry, Text, size);
	    
	    // Write it, including separator
	    rc = io.Write.run(io, size, Text.toString().getBytes()); //getBytes should return an ISO-8859-1 encoded array, which if only ASCII is returned will be an ASCII array
	    
	    return rc;
	}
	
	private static Object Type_Text_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
		return cmsnamed.cmsMLUdup((cmsMLU)Ptr);
	}
	
	private static void Type_Text_Free(cmsTagTypeHandler self, Object Ptr)
	{
		cmsMLU mlu = (cmsMLU)Ptr;
		cmsnamed.cmsMLUfree(mlu);
	}
	
	private static int DecideTextType(double ICCVersion, final Object Data)
	{
	    if (ICCVersion >= 4.0)
	    {
	    	return lcms2.cmsSigMultiLocalizedUnicodeType;
	    }
	    
	    return lcms2.cmsSigTextType;
	}
	
	// ********************************************************************************
	// Type cmsSigDataType
	// ********************************************************************************
	
	// General purpose data type
	private static Object Type_Data_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
//#ifdef RAW_C
		VirtualPointer BinData;
//#else
		cmsICCData BinData;
//#endif
	    int LenOfData;
	    
	    nItems[0] = 0;
	    LenOfData = SizeOfTag - /*sizeof(cmsUInt32Number)*/4;
	    
//#ifdef RAW_C
	    BinData = cmserr._cmsMalloc(self.ContextID, /*sizeof(cmsICCData)*/cmsICCData.SIZE); //cmsICCData usually has at least 1 byte of data and the rest is just "taked on" so this would read "sizeof(cmsICCData) + LenOfData - 1", this is a little different
	    if (BinData == null)
	    {
	    	return null;
	    }
//#else
	    BinData = new cmsICCData();
//#endif
	    
//#ifdef RAW_C
	    BinData.getProcessor().write(LenOfData, true);
//#else
	    BinData.len = LenOfData;
//#endif
	    int[] temp = new int[1];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
//#ifdef RAW_C
	    	cmserr._cmsFree(self.ContextID, BinData);
//#endif
	    	return null;
	    }
//#ifdef RAW_C
	    BinData.getProcessor().write(temp[0], true);
//#else
	    BinData.flag = temp[0];
//#endif
	    
	    VirtualPointer data = cmserr._cmsMalloc(self.ContextID, LenOfData);
	    if(data == null)
	    {
//#ifdef RAW_C
	    	cmserr._cmsFree(self.ContextID, BinData);
//#endif
	    	return null;
	    }
//#ifdef RAW_C
	    BinData.getProcessor().write(data);
	    BinData.setPosition(0);
//#else
	    BinData.data = data;
//#endif
	    if (io.vpRead(io, data, /*sizeof(cmsUInt8Number)*/1, LenOfData) != LenOfData)
	    {
	    	cmserr._cmsFree(self.ContextID, data);
//#ifdef RAW_C
	    	cmserr._cmsFree(self.ContextID, BinData);
//#endif
	        return null;
	    }
	    
	    nItems[0] = 1;
	    
	    return BinData;
	}
	
	private static boolean Type_Data_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
//#ifdef RAW_C
		VirtualPointer BinData = (VirtualPointer)Ptr;
		BinData.setPosition(4);
//#else
		cmsICCData BinData = (cmsICCData)Ptr;
//#endif
	    
//#ifdef RAW_C
		if (!cmsplugin._cmsWriteUInt32Number(io, BinData.getProcessor().readInt32(true)))
//#else
		if (!cmsplugin._cmsWriteUInt32Number(io, BinData.flag))
//#endif
		{
			return false;
		}
		
//#ifdef RAW_C
		VirtualPointer data = BinData.getProcessor().readVirtualPointer();
		BinData.setPosition(0);
		return io.vpWrite(io, BinData.getProcessor().readInt32(), data);
//#else
		return io.vpWrite(io, BinData.len, BinData.data);
//#endif
	}
	
	private static Object Type_Data_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
//#ifdef RAW_C
		VirtualPointer BinData = (VirtualPointer)Ptr;
		int len = BinData.getProcessor().readInt32();
		
		VirtualPointer dup = cmserr._cmsDupMem(self.ContextID, BinData, cmsICCData.SIZE);
		
		BinData.setPosition(4 * 2);
		
		dup.setPosition(4 * 2);
		dup.getProcessor().write(cmserr._cmsDupMem(self.ContextID, BinData.getProcessor().readVirtualPointer(), len));
		dup.setPosition(0);
		
		BinData.setPosition(0);
//#else
		cmsICCData BinData = (cmsICCData)Ptr;
		cmsICCData dup = new cmsICCData();
		
		dup.len = BinData.len;
		dup.flag = BinData.flag;
		dup.data = cmserr._cmsDupMem(self.ContextID, dup.data, BinData.len);
//#endif
		
		return dup;
	}
	
	private static void Type_Data_Free(cmsTagTypeHandler self, Object Ptr)
	{
//#ifdef RAW_C
		cmserr._cmsFree(self.ContextID, (VirtualPointer)Ptr);
//#else
		cmserr._cmsFree(self.ContextID, ((cmsICCData)Ptr).data);
//#endif
	}
	
	// ********************************************************************************
	// Type cmsSigTextDescriptionType
	// ********************************************************************************
	
	private static Object Type_Text_Description_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
//#ifdef RAW_C
		VirtualPointer Text = null;
//#else
		byte[] Text = null;
//#endif
	    cmsMLU mlu = null;
	    int AsciiCount;
	    int i, UnicodeCode, UnicodeCount;
	    short ScriptCodeCode, Dummy;
	    byte ScriptCodeCount;
	    
	    nItems[0] = 0;
	    
	    //  One dword should be there
	    if (SizeOfTag < /*sizeof(cmsUInt32Number)*/4)
	    {
	    	return null;
	    }
	    
	    int[] temp = new int[1];
	    
	    // Read len of ASCII
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;
	    }
	    AsciiCount = temp[0];
	    SizeOfTag -= /*sizeof(cmsUInt32Number)*/4;
	    
	    // Check for size
	    if (SizeOfTag < AsciiCount)
	    {
	    	return null; 
	    }
	    
	    // All seems Ok, allocate the container
	    mlu = cmsnamed.cmsMLUalloc(self.ContextID, 1);
	    if (mlu == null)
	    {
	    	return null;
	    }
	    
	    // As many memory as size of tag
//#ifdef RAW_C
	    Text = cmserr._cmsMalloc(self.ContextID, AsciiCount + 1);
	    if (Text == null)
	    {
		    if (Text != null)
		    {
		    	cmserr._cmsFree(self.ContextID, Text);
		    }
		    if (mlu != null)
		    {
		    	cmsnamed.cmsMLUfree(mlu);
		    }
		    return null;
	    }
//#else
	    Text = new byte[AsciiCount + 1];
//#endif
	    
	    // Read it
//#ifdef RAW_C
	    if (io.vpRead(io, Text, /*sizeof(char)*/1, AsciiCount) != AsciiCount)
//#else
    	if (io.Read.run(io, Text, /*sizeof(char)*/1, AsciiCount) != AsciiCount)
//#endif
	    {
//#ifdef RAW_C
		    if (Text != null)
		    {
		    	cmserr._cmsFree(self.ContextID, Text);
		    }
//#endif
		    if (mlu != null)
		    {
		    	cmsnamed.cmsMLUfree(mlu);
		    }
		    return null;
	    }
	    SizeOfTag -= AsciiCount;
	    
	    // Make sure there is a terminator
//#ifdef RAW_C
	    Text.setPosition(AsciiCount);
	    Text.getProcessor().write((byte)0);
	    Text.setPosition(0);
//#else
	    Text[AsciiCount] = 0;
//#endif
	    
	    // Set the MLU entry. From here we can be tolerant to wrong types
//#ifdef RAW_C
	    if (!cmsnamed.cmsMLUsetASCII(mlu, lcms2.cmsNoLanguage, lcms2.cmsNoCountry, Text.getProcessor().readString(false, false)))
//#else
    	if (!cmsnamed.cmsMLUsetASCII(mlu, lcms2.cmsNoLanguage, lcms2.cmsNoCountry, new String(Text)))
//#endif
	    {
//#ifdef RAW_C
		    if (Text != null)
		    {
		    	cmserr._cmsFree(self.ContextID, Text);
		    }
//#endif
		    if (mlu != null)
		    {
		    	cmsnamed.cmsMLUfree(mlu);
		    }
		    return null;
	    }
//#ifdef RAW_C
	    cmserr._cmsFree(self.ContextID, Text);
//#endif
	    Text = null;
	    
	    // Skip Unicode code
	    if (SizeOfTag < 2 * /*sizeof(cmsUInt32Number)*/4)
	    {
	    	nItems[0] = 1;
		    return mlu;
	    }
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	nItems[0] = 1;
		    return mlu;
	    }
	    UnicodeCode = temp[0];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	nItems[0] = 1;
		    return mlu;
	    }
	    UnicodeCount = temp[0];
	    SizeOfTag -= 2 * /*sizeof(cmsUInt32Number)*/4;
	    
	    if (SizeOfTag < UnicodeCount * /*sizeof(cmsUInt16Number)*/2)
	    {
	    	nItems[0] = 1;
		    return mlu;
	    }
	    
	    byte[] temp = new byte[2]; //Used for skipping some values
	    for (i = 0; i < UnicodeCount; i++)
	    {
	        if (!io.Read.run(io, temp, /*sizeof(cmsUInt16Number)*/2, 1))
	        {
	        	nItems[0] = 1;
	    	    return mlu;
	        }
	    }
	    SizeOfTag -= UnicodeCount * /*sizeof(cmsUInt16Number)*/2;
	    
	    // Skip ScriptCode code if present. Some buggy profiles does have less
	    // data that stricttly required. We need to skip it as this type may come 
	    // embedded in other types.
	    
	    short[] temp2 = new short[1];
	    if (SizeOfTag >= /*sizeof(cmsUInt16Number)*/2 + /*sizeof(cmsUInt8Number)*/1 + 67)
	    {
	        if (!cmsplugin._cmsReadUInt16Number(io, temp2))
	        {
	        	nItems[0] = 1;
	    	    return mlu;
	        }
	        ScriptCodeCode = temp2[0];
	        if (!cmsplugin._cmsReadUInt8Number(io,  temp))
	        {
	        	nItems[0] = 1;
	    	    return mlu;                     
	        }
	        ScriptCodeCount = temp[0];
	        
	        // Skip rest of tag
	        for (i = 0; i < 67; i++)
	        {
	            if (!io.Read.run(io, temp, /*sizeof(cmsUInt8Number)*/1, 1))
	            {
//#ifdef RAW_C
	        	    if (Text != null)
	        	    {
	        	    	cmserr._cmsFree(self.ContextID, Text);
	        	    }
//#endif
	        	    if (mlu != null)
	        	    {
	        	    	cmsnamed.cmsMLUfree(mlu);
	        	    }
	        	    return null;
	            }
	        }
	    }

	    nItems[0] = 1;
	    return mlu;
	}
	
	// This tag can come IN UNALIGNED SIZE. In order to prevent issues, we force zeros on description to align it
	private static boolean Type_Text_Description_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		cmsMLU mlu = (cmsMLU)Ptr;
	    StringBuffer Text = null;
	    StringBuffer Wide = null;
	    int len, len_aligned, len_filler_alignment;
	    boolean rc = FALSE;
	    byte[] Filler = new byte[68];
	    
	    // Get the len of string
	    len = cmsMLUgetASCII(mlu, lcms2.cmsNoLanguage, lcms2.cmsNoCountry, null, 0);
	    
	    // From ICC3.4: It has been found that textDescriptionType can contain misaligned data
	    //(see clause 4.1 for the definition of “aligned”). Because the Unicode language
	    // code and Unicode count immediately follow the ASCII description, their
	    // alignment is not correct if the ASCII count is not a multiple of four. The
	    // ScriptCode code is misaligned when the ASCII count is odd. Profile reading and
	    // writing software must be written carefully in order to handle these alignment
	    // problems.
	    
	    // Compute an aligned size
	    len_aligned = lcms2_internal._cmsALIGNLONG(len);
	    len_filler_alignment = len_aligned - len;
	    
	    // Null strings
	    if (len <= 0)
	    {
	    	Text = new StringBuffer("\0");
	    	Wide - new StringBuffer("\0");
	    }
	    else
	    {
	        // Create independent buffers
	    	Text = new StringBuffer(new char[len]);
	    	Wide - new StringBuffer(new char[len]);
	        
	        // Get both representations. 
	    	cmsnamed.cmsMLUgetASCII(mlu, cmsNoLanguage, cmsNoCountry, Text, len * /*sizeof(char)*/1);
	    	cmsnamed.cmsMLUgetWide(mlu,  cmsNoLanguage, cmsNoCountry, Wide, len * /*sizeof(wchar_t)*/2);
	    }
	    
	    // * cmsUInt32Number       count;          * Description length
	    // * cmsInt8Number         desc[count]     * NULL terminated ascii string
	    // * cmsUInt32Number       ucLangCode;     * UniCode language code
	    // * cmsUInt32Number       ucCount;        * UniCode description length
	    // * cmsInt16Number        ucDesc[ucCount];* The UniCode description
	    // * cmsUInt16Number       scCode;         * ScriptCode code
	    // * cmsUInt8Number        scCount;        * ScriptCode count
	    // * cmsInt8Number         scDesc[67];     * ScriptCode Description
	    
	    if (!cmsplugin._cmsWriteUInt32Number(io, len_aligned))
	    {
	    	return rc;
	    }
	    if (!io.Write.run(io, len, Text.toString().getBytes()))
	    {
	    	return rc;
	    }
	    if (!io.Write.run(io, len_filler_alignment, Filler))
	    {
	    	return rc;
	    }
	    
	    if (!cmsplugin._cmsWriteUInt32Number(io, 0))
	    {
	    	return rc; // ucLanguageCode
	    }
	    
	    // This part is tricky: we need an aligned tag size, and the ScriptCode part
	    // takes 70 bytes, so we need 2 extra bytes to do the alignment
	    
	    if (!cmsplugin._cmsWriteUInt32Number(io, len_aligned + 1))
	    {
	    	return rc;
	    }
	    
		// Note that in some compilers sizeof(cmsUInt16Number) != sizeof(wchar_t)
	    if (!_cmsWriteWCharArray(io, len, Wide.toString().toCharArray()))
	    {
	    	return rc;            
	    }
	    if (!cmsplugin._cmsWriteUInt16Array(io, len_filler_alignment + 1, new short[len_filler_alignment + 1]))
	    {
	    	return rc;   
	    }
	    
	    // ScriptCode Code & count (unused)
	    if (!cmsplugin._cmsWriteUInt16Number(io, 0))
	    {
	    	return rc;
	    }
	    if (!cmsplugin._cmsWriteUInt8Number(io, 0))
	    {
	    	return rc;
	    }
	    
	    if (!io.Write.run(io, 67, Filler))
	    {
	    	return rc;
	    }
	    
	    rc = true;
	    
	    return rc;
	}
	
	private static Object Type_Text_Description_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
		return cmsnamed.cmsMLUdup((cmsMLU)Ptr);
	}
	
	private static void Type_Text_Description_Free(cmsTagTypeHandler self, Object Ptr)
	{
		cmsMLU mlu = (cmsMLU)Ptr;
		
		cmsnamed.cmsMLUfree(mlu);
	}
	
	private static int DecideTextDescType(double ICCVersion, final Object Data)
	{
	    if (ICCVersion >= 4.0)
	    {
	    	return lcms2.cmsSigMultiLocalizedUnicodeType;
	    }
	    
	    return lcms2.cmsSigTextDescriptionType;
	}
	
	// ********************************************************************************
	// Type cmsSigCurveType
	// ********************************************************************************
	
	private static Object Type_Curve_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
		int Count;
	    cmsToneCurve NewGamma;
	    short[] Linear = { 0, 0xffff };
	    int[] temp = new int[1];
	    
	    nItems[0] = 0;
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;
	    }
	    Count = temp[0];

	    switch (Count)
	    {
		    case 0: // Linear
		    	NewGamma = cmsgamma.cmsBuildTabulatedToneCurve16(self.ContextID, 2, Linear);
		    	if (NewGamma == null)
		    	{
		    		return null;
		    	}
		    	nItems[0] = 1;
		    	return NewGamma;
		    case 1: // Specified as the exponent of gamma function
		    {
		    	short[] SingleGammaFixed = new short[1];
                double[] SingleGamma;
                
                if (!cmsplugin._cmsReadUInt16Number(io, SingleGammaFixed))
                {
                	return null;
                }
                SingleGamma = new double[]{cmsplugin._cms8Fixed8toDouble(SingleGammaFixed[0])};
                
                nItems[0] = 1;
                return cmsgamma.cmsBuildParametricToneCurve(self.ContextID, 1, SingleGamma);
		    }
		    default: // Curve
		    	NewGamma = cmsBuildTabulatedToneCurve16(self.ContextID, Count, null);
		    	if (NewGamma == null)
		    	{
		    		return null;
		    	}
		    	
		    	if (!cmsplugin._cmsReadUInt16Array(io, Count, NewGamma.Table16))
		    	{
		    		return null;
		    	}
		    	
		    	nItems[0] = 1;
		    	return NewGamma;
	    }
	}
	
	private static boolean Type_Curve_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		cmsToneCurve Curve = (cmsToneCurve)Ptr;
		
	    if (Curve.nSegments == 1 && Curve.Segments[0].Type == 1)
	    {
	    	// Single gamma, preserve number
	    	short SingleGammaFixed = cmsplugin._cmsDoubleTo8Fixed8(Curve.Segments[0].Params[0]);
	    	
	    	if (!cmsplugin._cmsWriteUInt32Number(io, 1))
	    	{
	    		return false;
	    	}
	    	if (!cmsplugin._cmsWriteUInt16Number(io, SingleGammaFixed))
	    	{
	    		return false;
	    	}
	    	return true;
	    }
	    
	    if (!cmsplugin._cmsWriteUInt32Number(io, Curve.nEntries))
	    {
	    	return false; 
	    }
	    return cmsplugin._cmsWriteUInt16Array(io, Curve.nEntries, Curve.Table16);
	}
	
	private static Object Type_Curve_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
		return cmsgamma.cmsDupToneCurve((cmsToneCurve)Ptr);
	}
	
	private static void Type_Curve_Free(cmsTagTypeHandler self, Object Ptr)
	{
		cmsToneCurve gamma = (cmsToneCurve)Ptr;
		
		cmsgamma.cmsFreeToneCurve(gamma);
	}
	
	// ********************************************************************************
	// Type cmsSigParametricCurveType
	// ********************************************************************************
	
	// Decide which curve type to use on writting
	private static int DecideCurveType(double ICCVersion, final Object Data)
	{
	    cmsToneCurve Curve = (cmsToneCurve)Data;
	    
	    if (ICCVersion < 4.0)
	    {
	    	return lcms2.cmsSigCurveType;
	    }
	    if (Curve.nSegments != 1)
	    {
	    	return lcms2.cmsSigCurveType; // Only 1-segment curves can be saved as parametric
	    }
	    if (Curve.Segments[0].Type < 0)
	    {
	    	return lcms2.cmsSigCurveType; // Only non-inverted curves
	    }
	    
	    return lcms2.cmsSigParametricCurveType;
	}
	
	private static Object Type_ParametricCurve_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
		final int[] ParamsByType = { 1, 3, 4, 5, 7 };
	    double Params = new double[10];
	    short[] Type = new short[1];
	    int i, n;
	    cmsToneCurve NewGamma;
	    
	    if (!cmsplugin._cmsReadUInt16Number(io, Type))
	    {
	    	return null;
	    }
	    if (!cmsplugin._cmsReadUInt16Number(io, null))
	    {
	    	return null; // Reserved
	    }

	    if (Type[0] > 4)
	    {
	    	cmserr.cmsSignalError(self.ContextID, lcms2.cmsERROR_UNKNOWN_EXTENSION, "Unknown parametric curve type '%d'", new Object[]{new Short(Type[0])});
	        return null;
	    }
	    
	    n = ParamsByType[Type[0]];
	    
	    double[] temp = new double[1];
	    for (i = 0; i < n; i++)
	    {
	        if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	        {
	        	return null;      
	        }
	        Params[i] = temp[0];
	    }
	    
	    NewGamma = cmsgamma.cmsBuildParametricToneCurve(self.ContextID, Type + 1, Params);
	    
	    nItems[0] = 1;
	    return NewGamma;
	}
	
	private static boolean Type_ParametricCurve_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		cmsToneCurve Curve = (cmsToneCurve)Ptr;
	    int i, nParams;
	    final int ParamsByType[] = { 0, 1, 3, 4, 5, 7 };
	    
	    if (Curve.nSegments > 1 || Curve.Segments[0].Type < 1)
	    {
	    	cmserr.cmsSignalError(self.ContextID, 0, "Multisegment or Inverted parametric curves cannot be written", null);          
	        return false;
	    }
	    
	    nParams = ParamsByType[Curve.Segments[0].Type];
	    
	    if (!cmsplugin._cmsWriteUInt16Number(io, (short)(Curve.Segments[0].Type - 1)))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt16Number(io, 0))
	    {
	    	return false; // Reserved
	    }
	    
	    for (i = 0; i < nParams; i++)
	    {
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, Curve.Segments[0].Params[i]))
	        {
	        	return false;        
	        }
	    }
	    
	    return true;
	}
	
	private static Object Type_ParametricCurve_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
		return cmsgamma.cmsDupToneCurve((cmsToneCurve)Ptr);
	}
	
	private static void Type_ParametricCurve_Free(cmsTagTypeHandler self, Object Ptr)
	{
		cmsToneCurve gamma = (cmsToneCurve)Ptr;
		
		cmsgamma.cmsFreeToneCurve(gamma);
	}
	
	// ********************************************************************************
	// Type cmsSigDateTimeType
	// ********************************************************************************
	
	// A 12-byte value representation of the time and date, where the byte usage is assigned 
	// as specified in table 1. The actual values are encoded as 16-bit unsigned integers 
	// (uInt16Number - see 5.1.6).
	//
	// All the dateTimeNumber values in a profile shall be in Coordinated Universal Time 
	// (UTC, also known as GMT or ZULU Time). Profile writers are required to convert local
	// time to UTC when setting these values. Programmes that display these values may show 
	// the dateTimeNumber as UTC, show the equivalent local time (at current locale), or 
	// display both UTC and local versions of the dateTimeNumber.
	
	//TODO #1253
}
