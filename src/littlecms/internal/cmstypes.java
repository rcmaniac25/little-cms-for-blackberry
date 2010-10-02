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

import net.rim.device.api.util.Arrays;
import littlecms.internal.helper.BitConverter;
import littlecms.internal.helper.VirtualPointer;
import littlecms.internal.lcms2.cmsCIEXYZ;
import littlecms.internal.lcms2.cmsCIExyYTRIPLE;
import littlecms.internal.lcms2.cmsDateTimeNumber;
import littlecms.internal.lcms2.cmsICCData;
import littlecms.internal.lcms2.cmsICCMeasurementConditions;
import littlecms.internal.lcms2.cmsIOHANDLER;
import littlecms.internal.lcms2.cmsMLU;
import littlecms.internal.lcms2.cmsScreening;
import littlecms.internal.lcms2.cmsContext;
import littlecms.internal.lcms2.cmsICCViewingConditions;
import littlecms.internal.lcms2.cmsPipeline;
import littlecms.internal.lcms2.cmsSEQ;
import littlecms.internal.lcms2.cmsPSEQDESC;
import littlecms.internal.lcms2.cmsNAMEDCOLORLIST;
import littlecms.internal.lcms2.cmsStage;
import littlecms.internal.lcms2.cmsToneCurve;
import littlecms.internal.lcms2.cmsCurveSegment;
import littlecms.internal.lcms2.cmsUcrBg;
import littlecms.internal.lcms2_plugin.cmsPluginTag;
import littlecms.internal.lcms2_internal._cmsStageCLutData;
import littlecms.internal.lcms2_internal._cmsStageMatrixData;
import littlecms.internal.lcms2_internal._cmsStageToneCurvesData;
import littlecms.internal.lcms2_plugin._cmsTagBase;
import littlecms.internal.lcms2_plugin.cmsPluginBase;
import littlecms.internal.lcms2_plugin.cmsPluginTagType;
import littlecms.internal.lcms2_plugin.cmsTagDescriptor;
import littlecms.internal.lcms2_plugin.cmsTagTypeHandler;
import littlecms.internal.lcms2_plugin.cmsMAT3;

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
//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmstypes
{
	// Some types are not internal, thus they need to return public (normal) types.
	// RAW_C support implemented but return type is expected to be cmsUInt8Number[16] (byte[lcms2.cmsMAXCHANNELS]) so just do that.
	// If RAW_C processing is desired then remove the comment marker "//-" or in the case of the preprocessor components replace '-' with '#'
	
	static
	{
		setupMPEtypes();
		setupTypes();
		setupTags();
	}
	
	// Some broken types
	public static final int cmsCorbisBrokenXYZtype = 0x17A505B8;
	public static final int cmsMonacoBrokenCurveType = 0x9478ee00;
	
	// This is the linked list that keeps track of the defined types
	public static class _cmsTagTypeLinkedList
	{
		public static final int SIZE = cmsTagTypeHandler.SIZE;
		
		public cmsTagTypeHandler Handler;
		public _cmsTagTypeLinkedList Next;
		
		public _cmsTagTypeLinkedList(cmsTagTypeHandler Handler, _cmsTagTypeLinkedList Next)
		{
			this.Handler = Handler;
			this.Next = Next;
		}
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
	    boolean rc = false;
	    byte[] Filler = new byte[68];
	    
	    // Get the len of string
	    len = cmsMLUgetASCII(mlu, lcms2.cmsNoLanguage, lcms2.cmsNoCountry, null, 0);
	    
	    // From ICC3.4: It has been found that textDescription Type can contain misaligned data
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
	    	cmsnamed.cmsMLUgetASCII(mlu, lcms2.cmsNoLanguage, lcms2.cmsNoCountry, Text, len * /*sizeof(char)*/1);
	    	cmsnamed.cmsMLUgetWide(mlu, lcms2.cmsNoLanguage, lcms2.cmsNoCountry, Wide, len * /*sizeof(wchar_t)*/2);
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
	    	cmserr.cmsSignalError(self.ContextID, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSTYPES_UNK_PARAM_CURVE_TYPE), new Object[]{new Short(Type[0])});
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
	    	cmserr.cmsSignalError(self.ContextID, 0, Utility.LCMS_Resources.getString(LCMSResource.CMSTYPES_CURVE_CANT_BE_WRITTEN), null);          
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
	
	private static Object Type_DateTime_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
		VirtualPointer timestamp = new VirtualPointer(cmsDateTimeNumber.SIZE);
	    Calendar NewDateTime;
	    
	    nItems[0] = 0;
	    NewDateTime = Calendar.getInstance(); //This would normally use _cmsMalloc but because of the internal (I.O.W. no source code for RIM impl) nature of it and the way it is going to be used, it is easier to just do this.
	    
	    if (io.vpRead(io, timestamp, /*sizeof(cmsDateTimeNumber)*/cmsDateTimeNumber.SIZE, 1) != 1)
	    {
	    	return null;
	    }
	    
	    cmsplugin._cmsDecodeDateTimeNumber(timestamp.getProcessor().readObject(cmsDateTimeNumber.class), NewDateTime);
	    
	    nItems[0] = 1;
	    return NewDateTime;
	}
	
	private static boolean Type_DateTime_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		Calendar DateTime = (Calendar)Ptr;
	    cmsDateTimeNumber timestamp = new cmsDateTimeNumber();
	    
	    cmsplugin._cmsEncodeDateTimeNumber(timestamp, DateTime);
	    if (!io.vpWrite(io, /*sizeof(cmsDateTimeNumber)*/cmsDateTimeNumber.SIZE, new VirtualPointer(timestamp)))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	private static Object Type_DateTime_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
		return cmserr._cmsDupMem(self.ContextID, new VirtualPointer(Ptr), /*sizeof(Calendar)*/8).getProcessor().readObject(Calendar.class);
	}
	
	private static void Type_DateTime_Free(cmsTagTypeHandler self, Object Ptr)
	{
		//Would free memory but using native built in type
	}
	
	// ********************************************************************************
	// Type icMeasurementType
	// ********************************************************************************
	
	/*
	The measurementType information refers only to the internal profile data and is
	meant to provide profile makers an alternative to the default measurement 
	specifications.
	*/
	
	private static Object Type_Measurement_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
		cmsICCMeasurementConditions mc = new cmsICCMeasurementConditions();
		
		int[] temp = new int[1];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;
	    }
	    mc.Observer = temp[0];
	    if (!cmsplugin._cmsReadXYZNumber(io, mc.Backing))
	    {
	    	return null;
	    }
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;
	    }
	    mc.Geometry = temp[0];
	    double[] temp2 = new double[1];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp2))
	    {
	    	return null;
	    }
	    mc.Flare = temp2[0];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;
	    }
	    mc.IlluminantType = temp[0];
	    
	    nItems[0] = 1;
	    return cmserr._cmsDupMem(self.ContextID, new VirtualPointer(mc), /*sizeof(cmsICCMeasurementConditions)*/cmsICCMeasurementConditions.SIZE).getProcessor().readObject(cmsICCMeasurementConditions.class);
	}
	
	private static boolean Type_Measurement_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		cmsICCMeasurementConditions mc =(cmsICCMeasurementConditions)Ptr;
	    
	    if (!cmsplugin._cmsWriteUInt32Number(io, mc.Observer))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteXYZNumber(io, mc.Backing))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, mc.Geometry))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWrite15Fixed16Number(io, mc.Flare))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, mc.IlluminantType))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	private static Object Type_Measurement_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
		return cmserr._cmsDupMem(self.ContextID, new VirtualPointer(Ptr), /*sizeof(cmsICCMeasurementConditions)*/cmsICCMeasurementConditions.SIZE).getProcessor().readObject(cmsICCMeasurementConditions.class);
	}
	
	private static void Type_Measurement_Free(cmsTagTypeHandler self, Object Ptr)
	{
		//Would free memory but never actually allocating memory (except for dup/read functions which is not a real issue)
	}
	
	// ********************************************************************************
	// Type cmsSigMultiLocalizedUnicodeType
	// ********************************************************************************
	
	//
	//   Do NOT trust SizeOfTag as there is an issue on the definition of profileSequenceDescTag. See the TechNote from 
	//   Max Derhak and Rohit Patil about this: basically the size of the string table should be guessed and cannot be
	//   taken from the size of tag if this tag is embedded as part of bigger structures (profileSequenceDescTag, for instance)
	//
	// FIXME: this doesn't work if sizeof(wchat_t) != 2  !!!
	
	private static Object Type_MLU_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
		cmsMLU mlu;
	    int Count, RecLen, NumOfWchar;       
	    int SizeOfHeader;
	    int Len, Offset;
	    int i;
	    VirtualPointer Block;
	    int BeginOfThisString, EndOfThisString, LargestPosition;
	    int[] temp = new int[1];
	    
	    nItems[0] = 0;
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;           
	    }
	    Count = temp[0];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;           
	    }
	    RecLen = temp[0];
	    
	    if (RecLen != 12)
	    {
	    	cmserr.cmsSignalError(self.ContextID, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSTYPES_LOCALIZED_UNICODE_TYPE_INVALID_LEN), null);
	        return null;
	    }
	    
	    mlu = cmsnamed.cmsMLUalloc(self.ContextID, Count);
	    if (mlu == null)
	    {
	    	return null;
	    }
	    
	    mlu.UsedEntries = Count;
	    
	    SizeOfHeader = 12 * Count + /*sizeof(_cmsTagBase)*/_cmsTagBase.SIZE;
	    LargestPosition = 0;
	    
	    short[] temp2 = new short[1];
	    for (i = 0; i < Count; i++)
	    {
	        if (!cmsplugin._cmsReadUInt16Number(io, temp2))
	        {
	        	if (mlu != null)
	    	    {
	    	    	cmsnamed.cmsMLUfree(mlu);
	    	    }
	    	    return null;
	        }
	        mlu.Entries[i].Language = temp2[0];
	        if (!cmsplugin._cmsReadUInt16Number(io, temp2))
	        {
	        	if (mlu != null)
	    	    {
	    	    	cmsnamed.cmsMLUfree(mlu);
	    	    }
	    	    return null;
	        }
	        mlu.Entries[i].Country = temp2[0];
	        
	        // Now deal with Len and offset.
	        if (!cmsplugin._cmsReadUInt32Number(io, temp))
	        {
	        	if (mlu != null)
	    	    {
	    	    	cmsnamed.cmsMLUfree(mlu);
	    	    }
	    	    return null;
	        }
	        Len = temp[0];
	        mlu.Entries[i].Len = Len;
	        
	        if (!cmsplugin._cmsReadUInt32Number(io, temp))
	        {
	        	if (mlu != null)
	    	    {
	    	    	cmsnamed.cmsMLUfree(mlu);
	    	    }
	    	    return null;
	        }
	        Offset = temp[0];
	        
	        BeginOfThisString = Offset - SizeOfHeader - 8;
	        mlu.Entries[i].StrW = BeginOfThisString;
	        
	        // To guess maximum size, add offset + len
	        EndOfThisString = BeginOfThisString + Len;
	        if (EndOfThisString > LargestPosition)
	        {
	        	LargestPosition = EndOfThisString;
	        }
	    }
	    
	    // Now read the remaining of tag and fill all strings. Substract the directory
	    SizeOfTag = LargestPosition;
	    
	    Block = cmserr._cmsMalloc(self.ContextID, SizeOfTag);
	    if (Block == null)
	    {
	    	if (mlu != null)
		    {
		    	cmsnamed.cmsMLUfree(mlu);
		    }
		    return null;
	    }
	    
	    NumOfWchar = SizeOfTag / /*sizeof(cmsUInt16Number)*/2;
	    
	    if (!cmsplugin._cmsReadUInt16Array(io, NumOfWchar, Block))
	    {
	    	if (mlu != null)
		    {
		    	cmsnamed.cmsMLUfree(mlu);
		    }
		    return null;
	    }
	    mlu.MemPool = Block;
	    mlu.PoolSize = SizeOfTag;
	    mlu.PoolUsed = SizeOfTag;
	    
	    nItems[0] = 1;
	    return mlu;
	}
	
	private static boolean Type_MLU_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		cmsMLU mlu =(cmsMLU)Ptr;
	    int HeaderSize, Offset;
	    int i;
	    
	    if (!cmsplugin._cmsWriteUInt32Number(io, mlu.UsedEntries))
	    {
	    	return false;           
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, 12))
	    {
	    	return false;
	    }
	          
	    HeaderSize = 12 * mlu.UsedEntries + /*sizeof(_cmsTagBase)*/_cmsTagBase.SIZE;
	    
	    for (i = 0; i < mlu.UsedEntries; i++)
	    {
	        if (!cmsplugin._cmsWriteUInt16Number(io, mlu.Entries[i].Language))
	        {
	        	return false;           
	        }
	        if (!cmsplugin._cmsWriteUInt16Number(io, mlu.Entries[i].Country))
	        {
	        	return false;  
	        }
	        if (!cmsplugin._cmsWriteUInt32Number(io, mlu.Entries[i].Len))
	        {
	        	return false;
	        }
	        
	        Offset =  mlu.Entries[i].StrW + HeaderSize + 8;
	        
	        if (!cmsplugin._cmsWriteUInt32Number(io, Offset))
	        {
	        	return false;               
	        }
	    }
	    
	    if (!cmsplugin._cmsWriteUInt16Array(io, mlu.PoolUsed / /*sizeof(cmsUInt16Number)*/2, mlu.MemPool))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	private static Object Type_MLU_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
		return cmsnamed.cmsMLUdup((cmsMLU)Ptr);
	}
	
	private static void Type_MLU_Free(cmsTagTypeHandler self, Object Ptr)
	{
		cmsnamed.cmsMLUfree((cmsMLU)Ptr);
	}
	
	// ********************************************************************************
	// Type cmsSigLut8Type
	// ********************************************************************************
	
	// Decide which LUT type to use on writting
	private static int DecideLUTtypeA2B(double ICCVersion, final Object Data)
	{
	    cmsPipeline Lut = (cmsPipeline)Data;
	    
	    if (ICCVersion < 4.0)
	    {
	        if (Lut.SaveAs8Bits)
	        {
	        	return lcms2.cmsSigLut8Type;
	        }
	        return lcms2.cmsSigLut16Type;
	    }
	    else
	    {
	         return lcms2.cmsSigLutAtoBType;
	    }
	}
	
	private static int DecideLUTtypeB2A(double ICCVersion, final Object Data)
	{
	    cmsPipeline Lut = (cmsPipeline)Data;
	    
	    if (ICCVersion < 4.0)
	    {
	        if (Lut.SaveAs8Bits)
	        {
	        	return lcms2.cmsSigLut8Type;
	        }
	        return lcms2.cmsSigLut16Type;
	    }
	    else
	    {
	         return lcms2.cmsSigLutBtoAType;
	    }
	}
	
	/*
	This structure represents a colour transform using tables of 8-bit precision. 
	This type contains four processing elements: a 3 by 3 matrix (which shall be 
	the identity matrix unless the input colour space is XYZ), a set of one dimensional 
	input tables, a multidimensional lookup table, and a set of one dimensional output 
	tables. Data is processed using these elements via the following sequence:
	(matrix) -> (1d input tables)  -> (multidimensional lookup table - CLUT) -> (1d output tables)
	
	Byte Position   Field Length (bytes)  Content Encoded as...
	8                  1          Number of Input Channels (i)    uInt8Number
	9                  1          Number of Output Channels (o)   uInt8Number
	10                 1          Number of CLUT grid points (identical for each side) (g) uInt8Number
	11                 1          Reserved for padding (fill with 00h)
	
	12..15             4          Encoded e00 parameter   s15Fixed16Number
	*/
	
	// Read 8 bit tables as gamma functions
	private static boolean Read8bitTables(cmsContext ContextID, cmsIOHANDLER io, cmsPipeline lut, int nChannels)
	{
	    cmsStage mpe;
	    VirtualPointer Temp = null;
	    int i, j;
	    cmsToneCurve[] Tables = new cmsToneCurve[lcms2.cmsMAXCHANNELS];
	    
	    if (nChannels > lcms2.cmsMAXCHANNELS)
	    {
	    	return false;
	    }
	    
	    Temp = cmserr._cmsMalloc(ContextID, 256);
	    if (Temp == null)
	    {
	    	return false;
	    }
	    
	    for (i = 0; i < nChannels; i++)
	    {
	        Tables[i] = cmsgamma.cmsBuildTabulatedToneCurve16(ContextID, 256, null);
	        if (Tables[i] == null)
	        {
	        	for (i = 0; i < nChannels; i++)
	    	    {
	    	        if (Tables[i] != null)
	    	        {
	    	        	cmsgamma.cmsFreeToneCurve(Tables[i]);
	    	        }
	    	    }
	    	    
	    	    if (Temp != null)
	    	    {
	    	    	cmserr._cmsFree(ContextID, Temp);
	    	    }
	    	    return false;
	        }
	    }
	    
	    VirtualPointer.TypeProcessor proc = Temp.getProcessor();
	    for (i = 0; i < nChannels; i++)
	    {
	        if (io.vpRead(io, Temp, 256, 1) != 1)
	        {
	        	for (i = 0; i < nChannels; i++)
	    	    {
	    	        if (Tables[i] != null)
	    	        {
	    	        	cmsgamma.cmsFreeToneCurve(Tables[i]);
	    	        }
	    	    }
	    	    
	    	    if (Temp != null)
	    	    {
	    	    	cmserr._cmsFree(ContextID, Temp);
	    	    }
	    	    return false;
	        }
	        
	        for (j = 0; j < 256; j++)
	        {
	        	Tables[i].Table16[j] = lcms2_internal.FROM_8_TO_16(proc.readInt8(true));
	        }
	        Temp.setPosition(0);
	    }
	    
	    cmserr._cmsFree(ContextID, Temp);
	    
	    mpe = cmslut.cmsStageAllocToneCurves(ContextID, nChannels, Tables);
	    if (mpe == null)
	    {
	    	for (i = 0; i < nChannels; i++)
		    {
		        if (Tables[i] != null)
		        {
		        	cmsgamma.cmsFreeToneCurve(Tables[i]);
		        }
		    }
		    
		    if (Temp != null)
		    {
		    	cmserr._cmsFree(ContextID, Temp);
		    }
		    return false;
	    }
	    
	    cmslut.cmsPipelineInsertStage(lut, lcms2.cmsAT_END, mpe);
	    
	    for (i = 0; i < nChannels; i++)
	    {
	    	cmsgamma.cmsFreeToneCurve(Tables[i]);
	    }
	    
	    return true;
	}
	
	private static boolean Write8bitTables(cmsContext ContextID, cmsIOHANDLER io, int n, _cmsStageToneCurvesData Tables)
	{
	    int j;
	    int i;
	    byte val;
	    
	    for (i = 0; i < n; i++)
	    {
	        if (Tables != null)
	        {
	            if (Tables.TheCurves[i].nEntries != 256)
	            {
	            	cmserr.cmsSignalError(ContextID, lcms2.cmsERROR_RANGE, Utility.LCMS_Resources.getString(LCMSResource.CMSTYPES_LUT8_NOT_ENOUGH_ELEMENTS), null);
	                return false;
	            }
	        }
	        
	        for (j = 0; j < 256; j++)
	        {
	            if (Tables != null)
	            {
	            	val = lcms2_internal.FROM_16_TO_8(Tables.TheCurves[i].Table16[j]);
	            }
	            else
	            {
	            	val = (byte)j;
	            }
	            
	            if (!cmsplugin._cmsWriteUInt8Number(io, val))
	            {
	            	return false;
	            }
	        }
	    }
	    return true;
	}
	
	private static int uipow(int a, int b)
	{
	    long rv = 1;
	    long aL = a & 0xFFFFFFFFL;
	    for (; b > 0; b--)
	    {
	    	rv *= aL;
	    }
	    return (int)rv;
	}
	
	// That will create a MPE LUT with Matrix, pre tables, CLUT and post tables. 
	// 8 bit lut may be scaled easely to v4 PCS, but we need also to properly adjust
	// PCS on BToAxx tags and AtoB if abstract. We need to fix input direction.
	
	private static Object Type_LUT8_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
		byte[] InputChannels = new byte[1], OutputChannels = new byte[1], CLUTpoints = new byte[1];
	    VirtualPointer Temp = null;
	    cmsPipeline NewLUT = null;
	    cmsStage mpemat, mpeclut;
	    int nTabSize, i;    
	    double[] Matrix = new double[3*3];
	    
	    nItems[0] = 0;
	    
	    if (!cmsplugin._cmsReadUInt8Number(io, InputChannels))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    if (!cmsplugin._cmsReadUInt8Number(io, OutputChannels))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    if (!cmsplugin._cmsReadUInt8Number(io, CLUTpoints))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    
	    // Padding
	    if (!cmsplugin._cmsReadUInt8Number(io, null))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    
	    // Do some checking
	    
	    if (InputChannels[0] > lcms2.cmsMAXCHANNELS)
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    if (OutputChannels[0] > lcms2.cmsMAXCHANNELS)
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    
	    // Allocates an empty Pipeline
	    NewLUT = cmslut.cmsPipelineAlloc(self.ContextID, InputChannels[0], OutputChannels[0]);
	    if (NewLUT == null)
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    
	    // Read the Matrix
	    double[] temp = new double[1];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    Matrix[0] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    Matrix[1] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    Matrix[2] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    Matrix[3] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    Matrix[4] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    Matrix[5] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    Matrix[6] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    Matrix[7] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    Matrix[8] = temp[0];
	    
	    // Only operates if not identity...
	    cmsMAT3 temp2 = new cmsMAT3();
	    cmsmtrx._cmsMAT3set(temp2, Matrix, 0);
	    if ((InputChannels == 3) && !cmsmtrx._cmsMAT3isIdentity(temp2))
	    {
	        mpemat = cmslut.cmsStageAllocMatrix(self.ContextID, 3, 3, Matrix, null);
	        if (mpemat == null)
	        {
	        	if (NewLUT != null)
	    	    {
	    	    	cmslut.cmsPipelineFree(NewLUT);
	    	    }
	    	    return null;
	        }
	        cmslut.cmsPipelineInsertStage(NewLUT, lcms2.cmsAT_BEGIN, mpemat);
	    }
	    
	    // Get input tables
	    if (!Read8bitTables(self.ContextID, io,  NewLUT, InputChannels[0]))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    
	    // Get 3D CLUT
	    nTabSize = (OutputChannels[0] * uipow(CLUTpoints[0], InputChannels[0]));
	    if (nTabSize > 0)
	    {
	    	int PtrW = 0;
	        short[] T;
	        int Tsize;
	        
	        Tsize = nTabSize * /*sizeof(cmsUInt16Number)*/2;
	        
	        T  = new short[nTabSize];
	        
	        Temp = cmserr._cmsMalloc(self.ContextID, nTabSize);             
	        if (Temp == null)
	        {
	        	if (NewLUT != null)
	    	    {
	    	    	cmslut.cmsPipelineFree(NewLUT);
	    	    }
	    	    return null;
	        }
	        
	        if (io.vpRead(io, Temp, nTabSize, 1) != 1)
	        {
	        	if (NewLUT != null)
	    	    {
	    	    	cmslut.cmsPipelineFree(NewLUT);
	    	    }
	    	    return null;
	        }
	        
	        for (i = 0; i < nTabSize; i++)
	        {
	            T[PtrW++] = lcms2_internal.FROM_8_TO_16(Temp.getProcessor().readInt8(true));
	        }
	        cmserr._cmsFree(self.ContextID, Temp);
	        Temp = null;
	        
	        mpeclut = cmslut.cmsStageAllocCLut16bit(self.ContextID, CLUTpoints[0], InputChannels[0], OutputChannels[0], T);
	        if (mpeclut == null)
	        {
	        	if (NewLUT != null)
	    	    {
	    	    	cmslut.cmsPipelineFree(NewLUT);
	    	    }
	    	    return null;
	        }
	        cmslut.cmsPipelineInsertStage(NewLUT, lcms2.cmsAT_END, mpeclut);
	    }
	    
	    // Get output tables
	    if (!Read8bitTables(self.ContextID, io,  NewLUT, OutputChannels[0]))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    
	    nItems[0] = 1;
	    return NewLUT;
	}
	
	// We only allow a specific MPE structure: Matrix plus prelin, plus clut, plus post-lin.
	private static boolean Type_LUT8_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		int j, nTabSize;
	    byte val;
	    cmsPipeline NewLUT = (cmsPipeline)Ptr;
	    cmsStage mpe;
	    _cmsStageToneCurvesData PreMPE = null, PostMPE = null;
	    _cmsStageMatrixData MatMPE = null;
	    _cmsStageCLutData clut = null;
	    int clutPoints;
	    
	    // Disassemble the LUT into components.
	    mpe = NewLUT.Elements;
	    if (mpe.Type == lcms2.cmsSigMatrixElemType)
	    {
	        MatMPE = (_cmsStageMatrixData)mpe.Data;
	        mpe = mpe.Next;
	    }
	    
	    if (mpe != null && mpe.Type == lcms2.cmsSigCurveSetElemType)
	    {
	        PreMPE = (_cmsStageToneCurvesData)mpe.Data;
	        mpe = mpe.Next;
	    }
	    
	    if (mpe != null && mpe.Type == lcms2.cmsSigCLutElemType)
	    {
	        clut  = (_cmsStageCLutData)mpe.Data;
	        mpe = mpe.Next;
	    }
	    
	    if (mpe != null && mpe.Type == lcms2.cmsSigCurveSetElemType)
	    {
	        PostMPE = (_cmsStageToneCurvesData)mpe.Data;
	        mpe = mpe.Next;
	    }
	    
	    // That should be all
	    if (mpe != null)
	    {
	        cmserr.cmsSignalError(mpe.ContextID, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSTYPES_LUT_CANT_BE_LUT8), null);
	        return false;
	    }
	    
	    if (clut == null)
	    {
	    	clutPoints = 0;
	    }
	    else
	    {
	    	clutPoints = clut.Params.nSamples[0];
	    }
	    
	    if (!cmsplugin._cmsWriteUInt8Number(io, (byte)NewLUT.InputChannels))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt8Number(io, (byte)NewLUT.OutputChannels))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt8Number(io, (byte)clutPoints))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt8Number(io, 0))
	    {
	    	return false; // Padding
	    }
	    
	    if (MatMPE != null)
	    {
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, MatMPE.Double[0]))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, MatMPE.Double[1]))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, MatMPE.Double[2]))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, MatMPE.Double[3]))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, MatMPE.Double[4]))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, MatMPE.Double[5]))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, MatMPE.Double[6]))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, MatMPE.Double[7]))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, MatMPE.Double[8]))
		    {
		    	return false;
		    }
	    }
	    else
	    {
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, 1))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, 0))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, 0))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, 0))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, 1))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, 0))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, 0))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, 0))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, 1))
		    {
		    	return false;
		    }
	    }
	    
	    // The prelinearization table
	    if (!Write8bitTables(self.ContextID, io, NewLUT.InputChannels, PreMPE))
	    {
	    	return false;
	    }
	    
	    nTabSize = (NewLUT.OutputChannels * uipow(clutPoints, NewLUT.InputChannels));
	    
	    // The 3D CLUT.
	    if (clut != null)
	    {
	    	VirtualPointer.TypeProcessor proc = clut.Tab.getProcessor();
	        for (j = 0; j < nTabSize; j++)
	        {
	            val = lcms2_internal.FROM_16_TO_8(proc.readInt16(true));
	            if (!cmsplugin._cmsWriteUInt8Number(io, val))
	    	    {
	            	clut.Tab.setPosition(0);
	    	    	return false;
	    	    }
	        }
	        clut.Tab.setPosition(0);
	    }
	    
	    // The postlinearization table
	    if (!Write8bitTables(self.ContextID, io, NewLUT.OutputChannels, PostMPE))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	private static Object Type_LUT8_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
		return cmslut.cmsPipelineDup((cmsPipeline)Ptr);
	}
	
	private static void Type_LUT8_Free(cmsTagTypeHandler self, Object Ptr)
	{
		cmslut.cmsPipelineFree((cmsPipeline)Ptr);
	}
	
	// ********************************************************************************
	// Type cmsSigLut16Type
	// ********************************************************************************
	
	// Read 16 bit tables as gamma functions
	private static boolean Read16bitTables(cmsContext ContextID, cmsIOHANDLER io, cmsPipeline lut, int nChannels, int nEntries)
	{
		cmsStage mpe;
	    int i;
	    cmsToneCurve[] Tables = new cmsToneCurve[lcms2.cmsMAXCHANNELS];
	    
	    // Maybe an empty table? (this is a lcms extension)
	    if (nEntries <= 0)
	    {
	    	return true;
	    }
	    
	    // Check for malicious profiles
	    if (nChannels > lcms2.cmsMAXCHANNELS)
	    {
	    	return false;
	    }
	    
	    for (i = 0; i < nChannels; i++)
	    {
	        Tables[i] = cmsgamma.cmsBuildTabulatedToneCurve16(ContextID, nEntries, null);
	        if (Tables[i] == null)
	        {
	        	for (i = 0; i < nChannels; i++)
	    	    {
	    	        if (Tables[i] != null)
	    	        {
	    	        	cmsgamma.cmsFreeToneCurve(Tables[i]);
	    	        }
	    	    }
	    	    
	    	    return false;
	        }
	        
	        if (!cmsplugin._cmsReadUInt16Array(io, nEntries, Tables[i].Table16))
	        {
	        	for (i = 0; i < nChannels; i++)
	    	    {
	    	        if (Tables[i] != null)
	    	        {
	    	        	cmsgamma.cmsFreeToneCurve(Tables[i]);
	    	        }
	    	    }
	    	    
	    	    return false;
	        }
	    }
	    
	    // Add the table (which may certainly be an identity, but this is up to the optimizer, not the reading code)
	    mpe = cmslut.cmsStageAllocToneCurves(ContextID, nChannels, Tables);
	    if (mpe == null)
	    {
	    	for (i = 0; i < nChannels; i++)
		    {
		        if (Tables[i] != null)
		        {
		        	cmsgamma.cmsFreeToneCurve(Tables[i]);
		        }
		    }
		    
		    return false;
	    }
	    
	    cmslut.cmsPipelineInsertStage(lut, lcms2.cmsAT_END, mpe);
	    
	    for (i = 0; i < nChannels; i++)
	    {
	    	cmsgamma.cmsFreeToneCurve(Tables[i]);
	    }
	    
	    return true;
	}
	
	private static boolean Write16bitTables(cmsContext ContextID, cmsIOHANDLER io, _cmsStageToneCurvesData Tables)
	{
		int j;
	    int i;
	    short val;
	    int nEntries = 256;
	    
	    nEntries = Tables.TheCurves[0].nEntries;
	    
	    for (i = 0; i < Tables.nCurves; i++)
	    {
	        for (j = 0; j < nEntries; j++)
	        {
	            if (Tables != null)
	            {
	            	val = Tables.TheCurves[i].Table16[j];
	            }
	            else
	            {
	            	val = cmslut._cmsQuantizeVal(j, nEntries);
	            }
	            
	            if (!cmsplugin._cmsWriteUInt16Number(io, val))
	            {
	            	return false;
	            }
	        }
	    }
	    return true;
	}
	
	private static Object Type_LUT16_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
		byte[] InputChannels = new byte[1], OutputChannels = new byte[1], CLUTpoints = new byte[1];
		cmsPipeline NewLUT = null;
		cmsStage mpemat, mpeclut;
	    int nTabSize;    
	    double[] Matrix = new double[3*3];
	    short[] InputEntries = new short[1], OutputEntries = new short[1];
	    
	    nItems[0] = 0;
	    
	    if (!cmsplugin._cmsReadUInt8Number(io, InputChannels))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    if (!cmsplugin._cmsReadUInt8Number(io, OutputChannels))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    if (!cmsplugin._cmsReadUInt8Number(io, CLUTpoints))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    
	    // Padding
	    if (!cmsplugin._cmsReadUInt8Number(io, null))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    
	    // Do some checking
	    if (CLUTpoints[0] > 100)
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    if (InputChannels[0] > lcms2.cmsMAXCHANNELS)
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    if (OutputChannels[0] > lcms2.cmsMAXCHANNELS)
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    
	    // Allocates an empty LUT
	    NewLUT = cmslut.cmsPipelineAlloc(self.ContextID, InputChannels[0], OutputChannels[0]);
	    if (NewLUT == null)
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    
	    // Read the Matrix
	    double[] temp = new double[1];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    Matrix[0] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    Matrix[1] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    Matrix[2] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    Matrix[3] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    Matrix[4] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    Matrix[5] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    Matrix[6] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    Matrix[7] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    Matrix[8] = temp[0];
	    
	    // Only operates on 3 channels
	    
	    cmsMAT3 temp2 = new cmsMAT3();
	    cmsmtrx._cmsMAT3set(temp2, Matrix, 0);
	    if ((InputChannels == 3) && !cmsmtrx._cmsMAT3isIdentity(temp2))
	    {
	        mpemat = cmslut.cmsStageAllocMatrix(self.ContextID, 3, 3, Matrix, null);
	        if (mpemat == null)
	        {
	        	if (NewLUT != null)
	    	    {
	    	    	cmslut.cmsPipelineFree(NewLUT);
	    	    }
	    	    return null;
	        }
	        cmslut.cmsPipelineInsertStage(NewLUT, lcms2.cmsAT_END, mpemat);
	    }
	    
	    if (!cmsplugin._cmsReadUInt16Number(io, InputEntries))
	    {
	    	return null;
	    }
	    if (!cmsplugin._cmsReadUInt16Number(io, OutputEntries))
	    {
	    	return null;
	    }
	    
	    // Get input tables
	    if (!Read16bitTables(self.ContextID, io,  NewLUT, InputChannels[0], InputEntries[0]))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    
	    // Get 3D CLUT
	    nTabSize = (OutputChannels[0] * uipow(CLUTpoints[0], InputChannels[0]));
	    if (nTabSize > 0)
	    {
	    	short[] T;
	    	
	        T  = new short[nTabSize];
	        
	        if (!cmsplugin._cmsReadUInt16Array(io, nTabSize, T))
	        {
	        	if (NewLUT != null)
			    {
			    	cmslut.cmsPipelineFree(NewLUT);
			    }
			    return null;
	        }
	        
	        mpeclut = cmslut.cmsStageAllocCLut16bit(self.ContextID, CLUTpoints[0], InputChannels[0], OutputChannels[0], T);
	        if (mpeclut == null)
	        {
	        	if (NewLUT != null)
			    {
			    	cmslut.cmsPipelineFree(NewLUT);
			    }
			    return null;
	        }
	        cmslut.cmsPipelineInsertStage(NewLUT, lcms2.cmsAT_END, mpeclut);
	    }
	    
	    // Get output tables
	    if (!Read16bitTables(self.ContextID, io,  NewLUT, OutputChannels[0], OutputEntries[0]))
	    {
	    	if (NewLUT != null)
		    {
		    	cmslut.cmsPipelineFree(NewLUT);
		    }
		    return null;
	    }
	    
	    nItems[0] = 1;
	    return NewLUT;
	}
	
	// We only allow some specific MPE structures: Matrix plus prelin, plus clut, plus post-lin. 
	// Some empty defaults are created for missing parts
	
	private static boolean Type_LUT16_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		int nTabSize;
	    cmsPipeline NewLUT = (cmsPipeline)Ptr;
	    cmsStage mpe;
	    _cmsStageToneCurvesData PreMPE = null, PostMPE = null;
	    _cmsStageMatrixData MatMPE = null;
	    _cmsStageCLutData clut = null;
	    int InputChannels, OutputChannels, clutPoints;
	    
	    // Disassemble the LUT into components.
	    mpe = NewLUT.Elements;
	    if (mpe != null && mpe.Type == lcms2.cmsSigMatrixElemType)
	    {
	        MatMPE = (_cmsStageMatrixData)mpe.Data;
	        mpe = mpe.Next;
	    }
	    
	    if (mpe != null && mpe.Type == lcms2.cmsSigCurveSetElemType)
	    {
	        PreMPE = (_cmsStageToneCurvesData)mpe.Data;
	        mpe = mpe.Next;
	    }
	    
	    if (mpe != null && mpe.Type == lcms2.cmsSigCLutElemType)
	    {
	        clut  = (_cmsStageCLutData)mpe.Data;
	        mpe = mpe.Next;
	    }
	    
	    if (mpe != null && mpe.Type == lcms2.cmsSigCurveSetElemType)
	    {
	        PostMPE = (_cmsStageToneCurvesData)mpe.Data;
	        mpe = mpe.Next;
	    }
	    
	    // That should be all
	    if (mpe != null)
	    {
	        cmserr.cmsSignalError(mpe.ContextID, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSTYPES_LUT_CANT_BE_LUT16), null);
	        return false;
	    }
	    
	    InputChannels  = cmslut.cmsPipelineInputChannels(NewLUT);
	    OutputChannels = cmslut.cmsPipelineOutputChannels(NewLUT);
	    
	    if (clut == null)
	    {
	    	clutPoints = 0;
	    }
	    else
	    {
	    	clutPoints = clut.Params.nSamples[0];
	    }
	    
	    if (!cmsplugin._cmsWriteUInt8Number(io, (byte)InputChannels))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt8Number(io, (byte)OutputChannels))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt8Number(io, (byte)clutPoints))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt8Number(io, 0))
	    {
	    	return false; // Padding
	    }
	    
	    if (MatMPE != null)
	    {
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, MatMPE.Double[0]))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, MatMPE.Double[1]))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, MatMPE.Double[2]))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, MatMPE.Double[3]))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, MatMPE.Double[4]))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, MatMPE.Double[5]))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, MatMPE.Double[6]))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, MatMPE.Double[7]))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, MatMPE.Double[8]))
		    {
		    	return false;
		    }
	    }
	    else
	    {
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, 1))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, 0))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, 0))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, 0))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, 1))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, 0))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, 0))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, 0))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, 1))
		    {
		    	return false;
		    }
	    }
	    
	    if (PreMPE != null)
	    {
	        if (!cmsplugin._cmsWriteUInt16Number(io, (short)PreMPE.TheCurves[0].nEntries))
	        {
	        	return false;
	        }
	    }
	    else
	    {
	    	if (!cmsplugin._cmsWriteUInt16Number(io, 0))
	    	{
	    		return false;
	    	}
	    }
	    
	    if (PostMPE != null)
	    {
	        if (!cmsplugin._cmsWriteUInt16Number(io, (short)PostMPE.TheCurves[0].nEntries))
	        {
	        	return false;
	        }
	    }
	    else
	    {
	        if (!cmsplugin._cmsWriteUInt16Number(io, 0))
	        {
	        	return false;
	        }
	    }
	    
	    // The prelinearization table
	    
	    if (PreMPE != null)
	    {
	        if (!Write16bitTables(self.ContextID, io, PreMPE))
	        {
	        	return false;
	        }
	    }
	    
	    nTabSize = (OutputChannels * uipow(clutPoints, InputChannels));
	    
	    // The 3D CLUT.
	    if (clut != null)
	    {
	        if (!cmsplugin._cmsWriteUInt16Array(io, nTabSize, clut.Tab))
	        {
	        	return FALSE;
	        }
	    }
	    
	    // The postlinearization table
	    if (PostMPE != null)
	    {
	        if (!Write16bitTables(self.ContextID, io, PostMPE))
	        {
	        	return false;
	        }
	    }
	    
	    return true;
	}
	
	private static Object Type_LUT16_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
		return cmslut.cmsPipelineDup((cmsPipeline)Ptr);
	}
	
	private static void Type_LUT16_Free(cmsTagTypeHandler self, Object Ptr)
	{
		cmslut.cmsPipelineFree((cmsPipeline)Ptr);
	}
	
	// ********************************************************************************
	// Type cmsSigLutAToBType
	// ********************************************************************************
	
	// V4 stuff. Read matrix for LutAtoB and LutBtoA
	
	private static cmsStage ReadMatrix(cmsTagTypeHandler self, cmsIOHANDLER io, int Offset)
	{
		double[] dMat = new double[3*3];
	    double[] dOff = new double[3];
	    cmsStage Mat;
	    
	    // Go to address 
	    if (!io.Seek.run(io, Offset))
	    {
	    	return null;
	    }
	    
	    // Read the Matrix
	    double[] temp = new double[1];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	return null;
	    }
	    dMat[0] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	return null;
	    }
	    dMat[1] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	return null;
	    }
	    dMat[2] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	return null;
	    }
	    dMat[3] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	return null;
	    }
	    dMat[4] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	return null;
	    }
	    dMat[5] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	return null;
	    }
	    dMat[6] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	return null;
	    }
	    dMat[7] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	return null;
	    }
	    dMat[8] = temp[0];
	    
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	return null;
	    }
	    dOff[0] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	return null;
	    }
	    dOff[1] = temp[0];
	    if (!cmsplugin._cmsRead15Fixed16Number(io, temp))
	    {
	    	return null;
	    }
	    dOff[2] = temp[0];
	    
	    Mat = cmslut.cmsStageAllocMatrix(self.ContextID, 3, 3, dMat, dOff);
	    
	    return Mat;
	}
	
	//  V4 stuff. Read CLUT part for LutAtoB and LutBtoA
	
	private static cmsStage ReadCLUT(cmsTagTypeHandler self, cmsIOHANDLER io, int Offset, int InputChannels, int OutputChannels)
	{
		byte[] gridPoints8 = new byte[lcms2.cmsMAXCHANNELS]; // Number of grid points in each dimension.  
	    int[] GridPoints = new int[lcms2.cmsMAXCHANNELS];
	    int i;
	    byte[] Precision = new byte[1];
	    cmsStage CLUT;
	    _cmsStageCLutData Data;
	    
	    if (!io.Seek.run(io, Offset))
	    {
	    	return null;
	    }
	    if (io.Read.run(io, gridPoints8, lcms2.cmsMAXCHANNELS, 1) != 1)
	    {
	    	return null;
	    }
	    
	    for (i = 0; i < lcms2.cmsMAXCHANNELS; i++)
	    {
	    	GridPoints[i] = gridPoints8[i] & 0xFFFF;
	    }
	    
	    if (!cmsplugin._cmsReadUInt8Number(io, Precision))
	    {
	    	return null;
	    }
	    
	    if (!cmsplugin._cmsReadUInt8Number(io, null))
	    {
	    	return null;
	    }
	    if (!cmsplugin._cmsReadUInt8Number(io, null))
	    {
	    	return null;
	    }
	    if (!cmsplugin._cmsReadUInt8Number(io, null))
	    {
	    	return null;
	    }
	    
	    CLUT = cmslut.cmsStageAllocCLut16bitGranular(self.ContextID, GridPoints, InputChannels, OutputChannels, null);
	    Data = (_cmsStageCLutData)CLUT.Data;
	    
	    // Precision can be 1 or 2 bytes
	    if (Precision[0] == 1)
	    {
	    	byte[] v = new byte[1];
	    	
	    	VirtualPointer.TypeProcessor proc = Data.Tab.getProcessor();
	        for (i = 0; i < Data.nEntries; i++)
	        {
	        	if (io.Read.run(io, v, /*sizeof(cmsUInt8Number)*/1, 1) != 1)
	        	{
	        		Data.Tab.setPosition(0);
	        		return null;
	        	}
	        	proc.write(lcms2_internal.FROM_8_TO_16(v), true);
	        }
	        Data.Tab.setPosition(0);
	    }
	    else
	    {
	        if (Precision[0] == 2)
	        {
	            if (!cmsplugin._cmsReadUInt16Array(io, Data.nEntries, Data.Tab))
	            {
	            	return null;
	            }
	        }
	        else
		    {
		        cmserr.cmsSignalError(self.ContextID, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSTYPES_UNK_PRECISION), new Object[]{new Integer(Precision[0])}); 
		        return null;
		    }
	    }
	    
	    return CLUT;
	}
	
	private static cmsToneCurve ReadEmbeddedCurve(cmsTagTypeHandler self, cmsIOHANDLER io)
	{
	    int BaseType;
	    int[] nItems = new int[1];
	    
	    BaseType = cmsplugin._cmsReadTypeBase(io);       
	    switch (BaseType)
	    {
            case lcms2.cmsSigCurveType:
                return (cmsToneCurve)Type_Curve_Read(self, io, nItems, 0);
            case lcms2.cmsSigParametricCurveType:
                return (cmsToneCurve)Type_ParametricCurve_Read(self, io, nItems, 0);
            default: 
	            {
	                StringBuffer String = new StringBuffer(4);
	                
	                cmserr._cmsTagSignature2String(String, BaseType);
	                cmserr.cmsSignalError(self.ContextID, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSTYPES_UNK_CURVE_TYPE), new Object[]{String});
	            }
	            return null;
	    }
	}
	
	// Read a set of curves from specific offset
	private static cmsStage ReadSetOfCurves(cmsTagTypeHandler self, cmsIOHANDLER io, int Offset, int nCurves)
	{
		cmsToneCurve[] Curves = new cmsToneCurve[lcms2.cmsMAXCHANNELS];
		int i;
	    cmsStage Lin;
	    
	    if (nCurves > lcms2.cmsMAXCHANNELS)
	    {
	    	return false;
	    }
	    
	    if (!io.Seek.run(io, Offset))
	    {
	    	return false;
	    }
	    
	    for (i = 0; i < nCurves; i++)
	    {
	        Curves[i] = ReadEmbeddedCurve(self, io);                     
	        if (Curves[i] == null)
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsReadAlignment(io))
		    {
		    	return false;
		    }
	    }
	    
	    Lin = cmslut.cmsStageAllocToneCurves(self.ContextID, nCurves, Curves);
	    
	    for (i = 0; i < nCurves; i++)
	    {
	    	cmsgamma.cmsFreeToneCurve(Curves[i]);
	    }
	    
	    return Lin;
	}
	
	// LutAtoB type 
	
	// This structure represents a colour transform. The type contains up to five processing 
	// elements which are stored in the AtoBTag tag in the following order: a set of one 
	// dimensional curves, a 3 by 3 matrix with offset terms, a set of one dimensional curves, 
	// a multidimensional lookup table, and a set of one dimensional output curves.
	// Data are processed using these elements via the following sequence:
	//
	//("A" curves) -> (multidimensional lookup table - CLUT) -> ("M" curves) -> (matrix) -> ("B" curves).
	//
	/*
	It is possible to use any or all of these processing elements. At least one processing element 
	must be included.Only the following combinations are allowed:
	
	B
	M - Matrix - B
	A - CLUT - B
	A - CLUT - M - Matrix - B
	
	*/
	
	private static Object Type_LUTA2B_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
		int      BaseOffset;
	    byte     inputChan;      // Number of input channels
	    byte     outputChan;     // Number of output channels
	    int      offsetB;        // Offset to first "B" curve
	    int      offsetMat;      // Offset to matrix
	    int      offsetM;        // Offset to first "M" curve
	    int      offsetC;        // Offset to CLUT
	    int      offsetA;        // Offset to first "A" curve
	    cmsStage mpe;
	    cmsPipeline NewLUT = null;
	    
	    BaseOffset = io.Tell.run(io) - /*sizeof(_cmsTagBase)*/_cmsTagBase.SIZE;
	    
	    byte[] temp = new byte[1];
	    if (!cmsplugin._cmsReadUInt8Number(io, temp))
	    {
	    	return null;
	    }
	    inputChan = temp[0];
	    if (!cmsplugin._cmsReadUInt8Number(io, temp))
	    {
	    	return null;
	    }
	    outputChan = temp[0];
	    
	    if (!cmsplugin._cmsReadUInt16Number(io, null))
	    {
	    	return null;
	    }
	    
	    int[] temp2 = new int[1];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp2))
	    {
	    	return null;
	    }
	    offsetB = temp2[0];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp2))
	    {
	    	return null;
	    }
	    offsetMat = temp2[0];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp2))
	    {
	    	return null;
	    }
	    offsetM = temp2[0];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp2))
	    {
	    	return null;
	    }
	    offsetC = temp2[0];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp2))
	    {
	    	return null;
	    }
	    offsetA = temp2[0];
	    
	    // Allocates an empty LUT
	    NewLUT = cmsPipelineAlloc(self.ContextID, inputChan, outputChan);
	    if (NewLUT == null)
	    {
	    	return null;
	    }
	    
	    if (offsetA!= 0)
	    {
	        mpe = ReadSetOfCurves(self, io, BaseOffset + offsetA, inputChan);
	        cmslut.cmsPipelineInsertStage(NewLUT, lcms2.cmsAT_END, mpe);
	    }
	    
	    if (offsetC != 0)
	    {
	        mpe = ReadCLUT(self, io, BaseOffset + offsetC, inputChan, outputChan);
	        cmslut.cmsPipelineInsertStage(NewLUT, lcms2.cmsAT_END, mpe);
	    }
	    
	    if (offsetM != 0)
	    {
	        mpe = ReadSetOfCurves(self, io, BaseOffset + offsetM, outputChan);
	        cmslut.cmsPipelineInsertStage(NewLUT, lcms2.cmsAT_END, mpe);
	    }
	    
	    if (offsetMat != 0)
	    {           
	        mpe = ReadMatrix(self, io, BaseOffset + offsetMat);
	        cmslut.cmsPipelineInsertStage(NewLUT, lcms2.cmsAT_END, mpe);
	    }
	    
	    if (offsetB != 0)
	    {                                        
	        mpe = ReadSetOfCurves(self, io, BaseOffset + offsetB, outputChan);
	        cmslut.cmsPipelineInsertStage(NewLUT, lcms2.cmsAT_END, mpe);
	    }
	    
	    nItems[0] = 1;
	    return NewLUT;
	}
	
	// Write a set of curves
	private static boolean WriteMatrix(cmsTagTypeHandler self, cmsIOHANDLER io, cmsStage mpe)
	{   
	    _cmsStageMatrixData m = (_cmsStageMatrixData)mpe.Data;
	    
	    // Write the Matrix
	    if (!cmsplugin._cmsWrite15Fixed16Number(io, m.Double[0]))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWrite15Fixed16Number(io, m.Double[1]))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWrite15Fixed16Number(io, m.Double[2]))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWrite15Fixed16Number(io, m.Double[3]))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWrite15Fixed16Number(io, m.Double[4]))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWrite15Fixed16Number(io, m.Double[5]))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWrite15Fixed16Number(io, m.Double[6]))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWrite15Fixed16Number(io, m.Double[7]))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWrite15Fixed16Number(io, m.Double[8]))
	    {
	    	return false;
	    }
	    
	    if (!cmsplugin._cmsWrite15Fixed16Number(io, m.Offset[0]))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWrite15Fixed16Number(io, m.Offset[1]))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWrite15Fixed16Number(io, m.Offset[2]))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	// Write a set of curves
	private static boolean WriteSetOfCurves(cmsTagTypeHandler self, cmsIOHANDLER io, int Type, cmsStage mpe)
	{   
	    int i, n;
	    int CurrentType;
	    cmsToneCurve[] Curves;
	    
	    n      = cmslut.cmsStageOutputChannels(mpe);
	    Curves = cmslut._cmsStageGetPtrToCurveSet(mpe);
	    
	    for (i = 0; i < n; i++)
	    {
	        // If this is a table-based curve, use curve type even on V4
	        CurrentType = Type;
	        
	        if (Curves[i].nSegments == 0)
	        {
	        	CurrentType = lcms2.cmsSigCurveType;
	        }
	        else
	        {
	        	if (Curves[i].Segments[0].Type < 0)
	        	{
	        		CurrentType = lcms2.cmsSigCurveType;
	        	}
	        }
	        
	        if (!cmsplugin._cmsWriteTypeBase(io, CurrentType))
	        {
	        	return false;
	        }
	        
	        switch (CurrentType)
	        {
	            case lcms2.cmsSigCurveType:
	                if (!Type_Curve_Write(self, io, Curves[i], 1))
	                {
	                	return false;
	                }
	                break;
	            case lcms2.cmsSigParametricCurveType:
	                if (!Type_ParametricCurve_Write(self, io, Curves[i], 1))
	                {
	                	return false;
	                }
	                break;
	            default:
	                {
	                	StringBuffer String = new StringBuffer(4);
	                    
	                    cmserr._cmsTagSignature2String(String, Type);
	                    cmserr.cmsSignalError(self.ContextID, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSTYPES_UNK_CURVE_TYPE), new Object[]{String});
	                }
	                return false;
	        }
	        
	        if (!cmsplugin._cmsWriteAlignment(io))
	        {
	        	return false;
	        }
	    }
	    
	    return true;
	}
	
	private static boolean WriteCLUT(cmsTagTypeHandler self, cmsIOHANDLER io, byte Precision, cmsStage mpe)
	{
		byte[] gridPoints = new byte[lcms2.cmsMAXCHANNELS]; // Number of grid points in each dimension.
	    int i;    
	    _cmsStageCLutData CLUT = ( _cmsStageCLutData)mpe.Data;
	    
	    for (i = 0; i < CLUT.Params.nInputs; i++)
	    {
	    	gridPoints[i] = (byte)CLUT.Params.nSamples[i];
	    }
	    
	    if (!io.Write.run(io, lcms2.cmsMAXCHANNELS * /*sizeof(cmsUInt8Number)*/1, gridPoints))
	    {
	    	return false;
	    }
	    
	    if (!cmsplugin._cmsWriteUInt8Number(io, Precision))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt8Number(io, 0))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt8Number(io, 0))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt8Number(io, 0))
	    {
	    	return false;
	    }
	    
	    // Precision can be 1 or 2 bytes
	    if (Precision == 1)
	    {
	    	VirtualPointer.TypeProcessor proc = CLUT.Tab.getProcessor();
	        for (i = 0; i < CLUT.nEntries; i++)
	        {
	            if (!cmsplugin._cmsWriteUInt8Number(io, lcms2_internal.FROM_16_TO_8(proc.readInt16(true))))
	            {
	            	CLUT.Tab.setPosition(0);
	            	return false;                
	            }
	        }
	        CLUT.Tab.setPosition(0);
	    }
	    else
	    {
	        if (Precision == 2)
	        {
	            if (!cmsplugin._cmsWriteUInt16Array(io, CLUT.nEntries, CLUT.Tab))
	            {
	            	return false;
	            }
	        }
	        else
	        {
	        	cmserr.cmsSignalError(self.ContextID, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSTYPES_UNK_PRECISION), new Object[]{new Byte(Precision)});
	            return false;
	        }
	    }
	    
	    if (!cmsplugin._cmsWriteAlignment(io))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	private static boolean Type_LUTA2B_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		cmsPipeline Lut = (cmsPipeline)Ptr;
	    int inputChan, outputChan;
	    cmsStage A = null, B = null, M = null;
	    cmsStage Matrix = null;
	    cmsStage CLUT = null;
	    int offsetB = 0, offsetMat = 0, offsetM = 0, offsetC = 0, offsetA = 0;
	    int BaseOffset, DirectoryPos, CurrentPos;
	    
	    // Get the base for all offsets
	    BaseOffset = io.Tell.run(io) - /*sizeof(_cmsTagBase)*/_cmsTagBase.SIZE;
	    
	    if (Lut.Elements != null)
	    {
	    	Object[] args = new Object[5 * 2];
	    	args[0] = new Integer(lcms2.cmsSigCurveSetElemType);
	        if (!cmslut.cmsPipelineCheckAndRetreiveStages(Lut, 1, args))
	        {
	        	//args[0] = new Integer(lcms2.cmsSigCurveSetElemType);
	        	args[1] = new Integer(lcms2.cmsSigMatrixElemType);
	        	args[2] = new Integer(lcms2.cmsSigCurveSetElemType);
	            if (!cmslut.cmsPipelineCheckAndRetreiveStages(Lut, 3, args))
	            {
	            	//args[0] = new Integer(lcms2.cmsSigCurveSetElemType);
		        	args[1] = new Integer(lcms2.cmsSigCLutElemType);
		        	//args[2] = new Integer(lcms2.cmsSigCurveSetElemType);
	                if (!cmslut.cmsPipelineCheckAndRetreiveStages(Lut, 3, args))
	                {
	                	//args[0] = new Integer(lcms2.cmsSigCurveSetElemType);
			        	//args[1] = new Integer(lcms2.cmsSigCLutElemType);
			        	//args[2] = new Integer(lcms2.cmsSigCurveSetElemType);
			        	args[3] = new Integer(lcms2.cmsSigMatrixElemType);
			        	args[4] = new Integer(lcms2.cmsSigCurveSetElemType);
	                    if (!cmslut.cmsPipelineCheckAndRetreiveStages(Lut, 5, args))
	                    {
	                    	lcms2.cmsSignalError(self.ContextID, lcms2.cmsERROR_NOT_SUITABLE, Utility.LCMS_Resources.getString(LCMSResource.CMSTYPES_LUT_CANT_BE_LUTATOB), null);
	                    	return false;
	                    }
	                    else
				        {
				        	A = (cmsStage)args[5];
				        	CLUT = (cmsStage)args[6];
				        	M = (cmsStage)args[7];
				        	Matrix = (cmsStage)args[8];
				        	B = (cmsStage)args[9];
				        }
	                }
	                else
			        {
			        	A = (cmsStage)args[3];
			        	CLUT = (cmsStage)args[4];
			        	B = (cmsStage)args[5];
			        }
	            }
	            else
		        {
		        	M = (cmsStage)args[3];
		        	Matrix = (cmsStage)args[4];
		        	B = (cmsStage)args[5];
		        }
	        }
	        else
	        {
	        	B = (cmsStage)args[1];
	        }
	    }
	    
	    // Get input, output channels
	    inputChan  = cmslut.cmsPipelineInputChannels(Lut);
	    outputChan = cmslut.cmsPipelineOutputChannels(Lut);
	    
	    // Write channel count
	    if (!cmsplugin._cmsWriteUInt8Number(io, (byte)inputChan))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt8Number(io, (byte)outputChan))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt16Number(io, 0))
	    {
	    	return false;
	    }
	    
	    // Keep directory to be filled latter
	    DirectoryPos = io.Tell.run(io);
	    
	    // Write the directory
	    if (!cmsplugin._cmsWriteUInt32Number(io, 0))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, 0))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, 0))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, 0))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, 0))
	    {
	    	return false;
	    }

	    if (A != null)
	    {
	        offsetA = io.Tell.run(io) - BaseOffset;
	        if (!WriteSetOfCurves(self, io, lcms2.cmsSigParametricCurveType, A))
		    {
		    	return false;
		    }
	    }
	    
	    if (CLUT != null)
	    {
	        offsetC = io.Tell.run(io) - BaseOffset;
	        if (!WriteCLUT(self, io, Lut.SaveAs8Bits ? 1 : 2, CLUT))
		    {
		    	return false;
		    }
	    }
	    
	    if (M != null)
	    {
	        offsetM = io.Tell.run(io) - BaseOffset;
	        if (!WriteSetOfCurves(self, io, lcms2.cmsSigParametricCurveType, M))
		    {
		    	return false;
		    }
	    }
	    
	    if (Matrix != null)
	    {
	        offsetMat = io.Tell.run(io) - BaseOffset;
	        if (!WriteMatrix(self, io, Matrix))
		    {
		    	return false;
		    }
	    }
	    
	    if (B != null)
	    {
	        offsetB = io.Tell.run(io) - BaseOffset;
	        if (!WriteSetOfCurves(self, io, lcms2.cmsSigParametricCurveType, B))
		    {
		    	return false;
		    }
	    }
	    
	    CurrentPos = io.Tell.run(io);
	    
	    if (!io.Seek.run(io, DirectoryPos))
	    {
	    	return false;
	    }

	    if (!cmsplugin._cmsWriteUInt32Number(io, offsetB))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, offsetMat))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, offsetM))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, offsetC))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, offsetA))
	    {
	    	return false;
	    }
	    
	    if (!io.Seek.run(io, CurrentPos))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	private static Object Type_LUTA2B_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
		return cmslut.cmsPipelineDup((cmsPipeline)Ptr);
	}
	
	private static void Type_LUTA2B_Free(cmsTagTypeHandler self, Object Ptr)
	{
		cmslut.cmsPipelineFree((cmsPipeline)Ptr);
	}
	
	// LutBToA type
	
	private static Object Type_LUTB2A_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
		byte     inputChan;      // Number of input channels
	    byte     outputChan;     // Number of output channels
		int      BaseOffset;     // Actual position in file
	    int      offsetB;        // Offset to first "B" curve
	    int      offsetMat;      // Offset to matrix
	    int      offsetM;        // Offset to first "M" curve
	    int      offsetC;        // Offset to CLUT
	    int      offsetA;        // Offset to first "A" curve
	    cmsStage mpe;
	    cmsPipeline NewLUT = null;
	    
	    BaseOffset = io.Tell.run(io) - /*sizeof(_cmsTagBase)*/_cmsTagBase.SIZE;
	    
	    byte[] temp = new byte[1];
	    if (!cmsplugin._cmsReadUInt8Number(io, temp))
	    {
	    	return null;
	    }
	    inputChan = temp[0];
	    if (!cmsplugin._cmsReadUInt8Number(io, temp))
	    {
	    	return null;
	    }
	    outputChan = temp[0];
	    
	    // Padding
	    if (!cmsplugin._cmsReadUInt16Number(io, null))
	    {
	    	return null;
	    }
	    
	    int[] temp2 = new int[1];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp2))
	    {
	    	return null;
	    }
	    offsetB = temp2[0];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp2))
	    {
	    	return null;
	    }
	    offsetMat = temp2[0];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp2))
	    {
	    	return null;
	    }
	    offsetM = temp2[0];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp2))
	    {
	    	return null;
	    }
	    offsetC = temp2[0];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp2))
	    {
	    	return null;
	    }
	    offsetA = temp2[0];
	    
	    // Allocates an empty LUT
	    NewLUT = cmsPipelineAlloc(self.ContextID, inputChan, outputChan);
	    if (NewLUT == null)
	    {
	    	return null;
	    }
	    
	    if (offsetB != 0)
	    {                                        
	        mpe = ReadSetOfCurves(self, io, BaseOffset + offsetB, outputChan);
	        cmslut.cmsPipelineInsertStage(NewLUT, lcms2.cmsAT_END, mpe);
	    }
	    
	    if (offsetMat != 0)
	    {           
	        mpe = ReadMatrix(self, io, BaseOffset + offsetMat);
	        cmslut.cmsPipelineInsertStage(NewLUT, lcms2.cmsAT_END, mpe);
	    }
	    
	    if (offsetM != 0)
	    {
	        mpe = ReadSetOfCurves(self, io, BaseOffset + offsetM, outputChan);
	        cmslut.cmsPipelineInsertStage(NewLUT, lcms2.cmsAT_END, mpe);
	    }
	    
	    if (offsetC != 0)
	    {
	        mpe = ReadCLUT(self, io, BaseOffset + offsetC, inputChan, outputChan);
	        cmslut.cmsPipelineInsertStage(NewLUT, lcms2.cmsAT_END, mpe);
	    }
	    
	    if (offsetA!= 0)
	    {
	        mpe = ReadSetOfCurves(self, io, BaseOffset + offsetA, inputChan);
	        cmslut.cmsPipelineInsertStage(NewLUT, lcms2.cmsAT_END, mpe);
	    }
	    
	    nItems[0] = 1;
	    return NewLUT;
	}
	
	/*
	B
	B - Matrix - M
	B - CLUT - A
	B - Matrix - M - CLUT - A
	*/
	
	private static boolean Type_LUTB2A_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		cmsPipeline Lut = (cmsPipeline)Ptr;
	    int inputChan, outputChan;
	    cmsStage A = null, B = null, M = null;
	    cmsStage Matrix = null;
	    cmsStage CLUT = null;
	    int offsetB = 0, offsetMat = 0, offsetM = 0, offsetC = 0, offsetA = 0;
	    int BaseOffset, DirectoryPos, CurrentPos;
	    
	    
	    BaseOffset = io.Tell.run(io) - /*sizeof(_cmsTagBase)*/_cmsTagBase.SIZE;
	    
	    if (Lut.Elements != null)
	    {
	    	Object[] args = new Object[5 * 2];
	    	args[0] = new Integer(lcms2.cmsSigCurveSetElemType);
	        if (!cmslut.cmsPipelineCheckAndRetreiveStages(Lut, 1, args))
	        {
	        	//args[0] = new Integer(lcms2.cmsSigCurveSetElemType);
	        	args[1] = new Integer(lcms2.cmsSigMatrixElemType);
	        	args[2] = new Integer(lcms2.cmsSigCurveSetElemType);
	            if (!cmslut.cmsPipelineCheckAndRetreiveStages(Lut, 3, args))
	            {
	            	//args[0] = new Integer(lcms2.cmsSigCurveSetElemType);
		        	args[1] = new Integer(lcms2.cmsSigCLutElemType);
		        	//args[2] = new Integer(lcms2.cmsSigCurveSetElemType);
	                if (!cmslut.cmsPipelineCheckAndRetreiveStages(Lut, 3, args))
	                {
	                	//args[0] = new Integer(lcms2.cmsSigCurveSetElemType);
	                	args[1] = new Integer(lcms2.cmsSigMatrixElemType);
			        	//args[2] = new Integer(lcms2.cmsSigCurveSetElemType);
			        	args[3] = new Integer(lcms2.cmsSigCLutElemType);
			        	args[4] = new Integer(lcms2.cmsSigCurveSetElemType);
	                    if (!cmslut.cmsPipelineCheckAndRetreiveStages(Lut, 5, args))
	                    {
	                    	lcms2.cmsSignalError(self.ContextID, lcms2.cmsERROR_NOT_SUITABLE, Utility.LCMS_Resources.getString(LCMSResource.CMSTYPES_LUT_CANT_BE_LUTBTOA), null);
	                    	return false;
	                    }
	                    else
				        {
				        	B = (cmsStage)args[5];
				        	Matrix = (cmsStage)args[6];
				        	M = (cmsStage)args[7];
				        	CLUT = (cmsStage)args[8];
				        	A = (cmsStage)args[9];
				        }
	                }
	                else
			        {
			        	B = (cmsStage)args[3];
			        	CLUT = (cmsStage)args[4];
			        	A = (cmsStage)args[5];
			        }
	            }
	            else
		        {
		        	B = (cmsStage)args[3];
		        	Matrix = (cmsStage)args[4];
		        	M = (cmsStage)args[5];
		        }
	        }
	        else
	        {
	        	B = (cmsStage)args[1];
	        }
	    }
	    
	    // Get input, output channels
	    inputChan  = cmslut.cmsPipelineInputChannels(Lut);
	    outputChan = cmslut.cmsPipelineOutputChannels(Lut);
	    
	    if (!cmsplugin._cmsWriteUInt8Number(io, (byte)inputChan))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt8Number(io, (byte)outputChan))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt16Number(io, 0))
	    {
	    	return false;
	    }
	    
	    DirectoryPos = io.Tell.run(io);
	    
	    if (!cmsplugin._cmsWriteUInt32Number(io, 0))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, 0))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, 0))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, 0))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, 0))
	    {
	    	return false;
	    }

	    if (A != null)
	    {
	        offsetA = io.Tell.run(io) - BaseOffset;
	        if (!WriteSetOfCurves(self, io, lcms2.cmsSigParametricCurveType, A))
		    {
		    	return false;
		    }
	    }
	    
	    if (CLUT != null)
	    {
	        offsetC = io.Tell.run(io) - BaseOffset;
	        if (!WriteCLUT(self, io, Lut.SaveAs8Bits ? 1 : 2, CLUT))
		    {
		    	return false;
		    }
	    }
	    
	    if (M != null)
	    {
	        offsetM = io.Tell.run(io) - BaseOffset;
	        if (!WriteSetOfCurves(self, io, lcms2.cmsSigParametricCurveType, M))
		    {
		    	return false;
		    }
	    }
	    
	    if (Matrix != null)
	    {
	        offsetMat = io.Tell.run(io) - BaseOffset;
	        if (!WriteMatrix(self, io, Matrix))
		    {
		    	return false;
		    }
	    }
	    
	    if (B != null)
	    {
	        offsetB = io.Tell.run(io) - BaseOffset;
	        if (!WriteSetOfCurves(self, io, lcms2.cmsSigParametricCurveType, B))
		    {
		    	return false;
		    }
	    }
	    
	    CurrentPos = io.Tell.run(io);
	    
	    if (!io.Seek.run(io, DirectoryPos))
	    {
	    	return false;
	    }

	    if (!cmsplugin._cmsWriteUInt32Number(io, offsetB))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, offsetMat))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, offsetM))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, offsetC))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, offsetA))
	    {
	    	return false;
	    }
	    
	    if (!io.Seek.run(io, CurrentPos))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	private static Object Type_LUTB2A_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
		return cmslut.cmsPipelineDup((cmsPipeline)Ptr);
	}
	
	private static void Type_LUTB2A_Free(cmsTagTypeHandler self, Object Ptr)
	{
		cmslut.cmsPipelineFree((cmsPipeline)Ptr);
	}
	
	// ********************************************************************************
	// Type cmsSigColorantTableType
	// ********************************************************************************
	/*
	The purpose of this tag is to identify the colorants used in the profile by a 
	unique name and set of XYZ or L*a*b* values to give the colorant an unambiguous 
	value. The first colorant listed is the colorant of the first device channel of
	a lut tag. The second colorant listed is the colorant of the second device channel
	of a lut tag, and so on.
	*/
	
	private static Object Type_ColorantTable_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
		int i, Count;
	    cmsNAMEDCOLORLIST List;
	    VirtualPointer Name = new VirtualPointer(34);
	    short[] PCS = new short[3];
	    
	    int[] temp = new int[1];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;
	    }
	    Count = temp[0];
	    
	    if (Count > lcms2.cmsMAXCHANNELS)
	    {
	        cmserr.cmsSignalError(self.ContextID, lcms2.cmsERROR_RANGE, Utility.LCMS_Resources.getString(LCMSResource.CMSTYPES_TOO_MANY_COLORANTS), new Object[]{new Integer(Count)});
	        return null;
	    }
	    
	    List = cmsnamed.cmsAllocNamedColorList(self.ContextID, Count, 0, "", "");
	    for (i = 0; i < Count; i++)
	    {
	        if (io.vpRead(io, Name, 32, 1) != 1)
	        {
	        	nItems[0] = 0;
	    	    cmsnamed.cmsFreeNamedColorList(List);
	    	    return null;
	        }
	        
	        if (!cmsplugin._cmsReadUInt16Array(io, 3, PCS))
	        {
	        	nItems[0] = 0;
	    	    cmsnamed.cmsFreeNamedColorList(List);
	    	    return null;
	        }
	        
	        if (!cmsnamed.cmsAppendNamedColor(List, Name.getProcessor().readString(false, false), PCS, null))
	        {
	        	nItems[0] = 0;
	    	    cmsnamed.cmsFreeNamedColorList(List);
	    	    return null;
	        }
	    }

	    nItems[0] = 1;
	    return List;
	}
	
	// Saves a colorant table. It is using the named color structure for simplicity sake
	private static boolean Type_ColorantTable_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		cmsNAMEDCOLORLIST NamedColorList = (cmsNAMEDCOLORLIST)Ptr; 
	    int i, nColors;
	    
	    nColors = cmsnamed.cmsNamedColorCount(NamedColorList);
	    
	    if (!cmsplugin._cmsWriteUInt32Number(io, nColors))
	    {
	    	return false;
	    }

	    for (i = 0; i < nColors; i++)
	    {
	    	StringBuffer root = new StringBuffer(new String(new char[33]));
	        short[] PCS = new short[3];
	        
	        if (!cmsnamed.cmsNamedColorInfo(NamedColorList, i, root, null, null, PCS, null))
	        {
	        	return false;
	        }
	        root.setCharAt(32, '\0');
	        
	        if (!io.Write.run(io, 32, root.toString().getBytes()))
	        {
	        	return false;
	        }
	        if (!cmsplugin._cmsWriteUInt16Array(io, 3, PCS))
	        {
	        	return false;
	        }
	    }
	    
	    return true;
	}
	
	private static Object Type_ColorantTable_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
		cmsNAMEDCOLORLIST nc = (cmsNAMEDCOLORLIST)Ptr;
	    return cmsnamed.cmsDupNamedColorList(nc);
	}
	
	private static void Type_ColorantTable_Free(cmsTagTypeHandler self, Object Ptr)
	{
		cmsnamed.cmsFreeNamedColorList((cmsNAMEDCOLORLIST)Ptr);
	}
	
	// ********************************************************************************
	// Type cmsSigNamedColor2Type
	// ********************************************************************************
	//
	//The namedColor2Type is a count value and array of structures that provide color 
	//coordinates for 7-bit ASCII color names. For each named color, a PCS and optional 
	//device representation of the color are given. Both representations are 16-bit values. 
	//The device representation corresponds to the header’s “color space of data” field. 
	//This representation should be consistent with the “number of device components”
	//field in the namedColor2Type. If this field is 0, device coordinates are not provided.
	//The PCS representation corresponds to the header’s PCS field. The PCS representation 
	//is always provided. Color names are fixed-length, 32-byte fields including null 
	//termination. In order to maintain maximum portability, it is strongly recommended 
	//that special characters of the 7-bit ASCII set not be used.
	
	private static Object Type_NamedColor_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
		int vendorFlag;									// Bottom 16 bits for ICC use 
		int count;										// Count of named colors 
		int nDeviceCoords;								// Num of device coordinates 
	    VirtualPointer prefix = new VirtualPointer(32);	// Prefix for each color name 
	    VirtualPointer suffix = new VirtualPointer(32);	// Suffix for each color name 
	    cmsNAMEDCOLORLIST v;
	    int i;
	    
	    nItems[0] = 0;
	    int[] temp = new int[1];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;
	    }
	    vendorFlag = temp[0];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;
	    }
	    count = temp[0];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;
	    }
	    nDeviceCoords = temp[0];
	    
	    if (io.vpRead(io, prefix, 32, 1) != 1)
	    {
	    	return null;
	    }
	    if (io.vpRead(io, suffix, 32, 1) != 1)
	    {
	    	return null;
	    }
	    
	    prefix.writeRaw(0, 31);
	    suffix.writeRaw(0, 31);
	    
	    v = cmsnamed.cmsAllocNamedColorList(self.ContextID, count, nDeviceCoords, prefix, suffix);
	    if (v == null)
	    {
	    	return null;
	    }
	    
	    if (nDeviceCoords > lcms2.cmsMAXCHANNELS)
	    {
	        cmserr.cmsSignalError(self.ContextID, lcms2.cmsERROR_RANGE, Utility.LCMS_Resources.getString(LCMSResource.CMSTYPES_TOO_MANY_DEVICE_COORD), new Object[]{new Integer(nDeviceCoords)});
	        return false;
	    }
	    for (i = 0; i < count; i++)
	    {
	        short[] PCS = new short[3];
	        short[] Colorant = new short[lcms2.cmsMAXCHANNELS];
	        VirtualPointer Root = new VirtualPointer(33);
	        
	        if (io.vpRead(io, Root, 32, 1) != 1)
		    {
		    	return null;
		    }
	        if (!cmsplugin._cmsReadUInt16Array(io, 3, PCS))
	        {
	        	cmsnamed.cmsFreeNamedColorList(v);
	    	    return null;
	        }
	        if (!cmsplugin._cmsReadUInt16Array(io, nDeviceCoords, Colorant))
	        {
	        	cmsnamed.cmsFreeNamedColorList(v);
	    	    return null;
	        }
	        
	        if (!cmsnamed.cmsAppendNamedColor(v, Root.getProcessor().readString(false, false), PCS, Colorant))
	        {
	        	cmsnamed.cmsFreeNamedColorList(v);
	    	    return null;
	        }
	    }
	    
	    nItems[0] = 1;
	    return v;
	}
	
	// Saves a named color list into a named color profile
	private static boolean Type_NamedColor_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		cmsNAMEDCOLORLIST NamedColorList = (cmsNAMEDCOLORLIST)Ptr;
		VirtualPointer prefix = new VirtualPointer(32); // Prefix for each color name 
		VirtualPointer suffix = new VirtualPointer(32); // Suffix for each color name 
	    int i, nColors;
	    
	    nColors = cmsnamed.cmsNamedColorCount(NamedColorList);
	    
	    if (!cmsplugin._cmsWriteUInt32Number(io, 0))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, nColors))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, NamedColorList.ColorantCount))
	    {
	    	return false;
	    }
	    
	    prefix.getProcessor().write(NamedColorList.Prefix, false, false, false);
	    suffix.getProcessor().write(NamedColorList.Suffix, false, false, false);
	    
	    if (!io.vpWrite(io, 32, prefix))
	    {
	    	return false;
	    }
	    if (!io.vpWrite(io, 32, suffix))
	    {
	    	return false;
	    }
	    
	    for (i = 0; i < nColors; i++)
	    {
	    	short[] PCS = new short[3];
	    	short[] Colorant = new short[lcms2.cmsMAXCHANNELS];
	    	StringBuffer Root = new StringBuffer(new String(new char[33]));
	    	
	    	if (!cmsnamed.cmsNamedColorInfo(NamedColorList, i, Root, null, null, PCS, Colorant))
		    {
		    	return false;
		    }
	    	if (!io.Write.run(io, 32, Root.toString().getBytes()))
		    {
		    	return false;
		    }
	    	if (!cmsplugin._cmsWriteUInt16Array(io, 3, PCS))
		    {
		    	return false;
		    }
	    	if (!cmsplugin._cmsWriteUInt16Array(io, NamedColorList.ColorantCount, Colorant))
		    {
		    	return false;
		    }
	    }
	    
	    return true;
	}
	
	private static Object Type_NamedColor_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
		cmsNAMEDCOLORLIST nc = (cmsNAMEDCOLORLIST)Ptr;
		
	    return cmsnamed.cmsDupNamedColorList(nc);
	}
	
	private static void Type_NamedColor_Free(cmsTagTypeHandler self, Object Ptr)
	{
		cmsnamed.cmsFreeNamedColorList((cmsNAMEDCOLORLIST)Ptr);
	}
	
	// ********************************************************************************
	// Type cmsSigProfileSequenceDescType
	// ********************************************************************************
	
	private static boolean ReadEmbeddedText(cmsTagTypeHandler self, cmsIOHANDLER io, cmsMLU[] mlu, int SizeOfTag)
	{
		int BaseType;
	    int[] nItems = new int[1];
	    
	    BaseType = cmsplugin._cmsReadTypeBase(io); 
	    
	    switch (BaseType)
	    {
	    	case lcms2.cmsSigTextType:
	        	if (mlu[0] != null)
	        	{
	        		cmsnamed.cmsMLUfree(mlu[0]);
	        	}
	        	mlu[0] = (cmsMLU)Type_Text_Read(self, io, nItems, SizeOfTag);
	        	return (mlu[0] != null);
	    	case lcms2.cmsSigTextDescriptionType:
	        	if (mlu[0] != null)
	        	{
	        		cmsnamed.cmsMLUfree(mlu[0]);
	        	}
	        	mlu[0] =  (cmsMLU)Type_Text_Description_Read(self, io, nItems, SizeOfTag);
	        	return (mlu[0] != null);
	        	
	        	/*
	        	TBD: Size is needed for MLU, and we have no idea on which is the available size
	        	*/
	    	case lcms2.cmsSigMultiLocalizedUnicodeType:
	    		if (mlu[0] != null)
	    		{
	    			cmsnamed.cmsMLUfree(mlu[0]);
	    		}
	    		mlu[0] =  (cmsMLU)Type_MLU_Read(self, io, nItems, SizeOfTag);
	    		return (mlu[0] != null);
    		default:
    			return false;
	    }
	}
	
	private static Object Type_ProfileSequenceDesc_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
		cmsSEQ OutSeq;
	    int i, Count;
	    
	    nItems[0] = 0;
	    
	    int[] temp = new int[1];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;
	    }
	    Count = temp[0];
	    SizeOfTag -= /*sizeof(cmsUInt32Number)*/4;
	    
	    OutSeq = cmsnamed.cmsAllocProfileSequenceDescription(self.ContextID, Count);
	    if (OutSeq == null)
	    {
	    	return null;
	    }
	    
	    OutSeq.n = Count;
	    
	    // Get structures as well
	    
	    cmsMLU[] temp2 = new cmsMLU[1];
	    long[] temp3 = new long[1];
	    for (i = 0; i < Count; i++)
	    {
	        cmsPSEQDESC sec = OutSeq.seq[i];
	        
	        temp[0] = sec.deviceMfg;
	        if (!cmsplugin._cmsReadUInt32Number(io, temp))
		    {
		    	return null;
		    }
	        sec.deviceMfg = temp[0];
	        SizeOfTag -= /*sizeof(cmsUInt32Number)*/4;
	        
	        temp[0] = sec.deviceModel;
	        if (!cmsplugin._cmsReadUInt32Number(io, temp))
		    {
		    	return null;
		    }
	        sec.deviceModel = temp[0];
	        SizeOfTag -= /*sizeof(cmsUInt32Number)*/4;
	        
	        temp3[0] = sec.attributes;
	        if (!cmsplugin._cmsReadUInt64Number(io, temp3))
		    {
		    	return null;
		    }
	        sec.attributes = temp3[0];
	        SizeOfTag -= /*sizeof(cmsUInt64Number)*/8;
	        
	        temp[0] = sec.technology;
	        if (!cmsplugin._cmsReadUInt32Number(io, temp))
		    {
		    	return null;
		    }
	        sec.technology = temp[0];
	        SizeOfTag -= /*sizeof(cmsUInt32Number)*/4;
	        
	        temp2[0] = sec.Manufacturer;
	        if (!ReadEmbeddedText(self, io, temp2, SizeOfTag))
		    {
		    	return null;
		    }
	        sec.Manufacturer = temp2[0];
	        temp2[0] = sec.Model;
	        if (!ReadEmbeddedText(self, io, temp2, SizeOfTag))
		    {
		    	return null;
		    }
	        sec.Model = temp2[0];
	    }
	    
	    nItems[0] = 1;
	    return OutSeq;
	}
	
	// Aux--Embed a text description type. It can be of type text description or multilocalized unicode
	private static boolean SaveDescription(cmsTagTypeHandler self, cmsIOHANDLER io, cmsMLU Text)
	{
	    if (Text == null)
	    {
	        // Placeholder for a null entry     
	        if (!cmsplugin._cmsWriteTypeBase(io, lcms2.cmsSigTextDescriptionType))
	        {
	        	return false;
	        }
	        return Type_Text_Description_Write(self, io, null, 1);
	    }
	    
	    if (Text.UsedEntries <= 1)
	    {
	        if (!cmsplugin._cmsWriteTypeBase(io, lcms2.cmsSigTextDescriptionType))
	        {
	        	return false;
	        }
	        return Type_Text_Description_Write(self, io, Text, 1);
	    }
	    else
	    {
	        if (!cmsplugin._cmsWriteTypeBase(io, lcms2.cmsSigMultiLocalizedUnicodeType))
	        {
	        	return false;
	        }
	        return Type_MLU_Write(self, io, Text, 1);
	    }
	}
	
	private static boolean Type_ProfileSequenceDesc_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		cmsSEQ Seq = (cmsSEQ)Ptr;
	    int i;
	    
	    if (!cmsplugin._cmsWriteUInt32Number(io, Seq.n))
	    {
	    	return false;
	    }
	    
	    for (i = 0; i < Seq.n; i++)
	    {
	        cmsPSEQDESC sec = Seq.seq[i];
	        
	        if (!cmsplugin._cmsWriteUInt32Number(io, sec.deviceMfg))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWriteUInt32Number(io, sec.deviceModel))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWriteUInt64Number(io, sec.attributes))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWriteUInt32Number(io, sec.technology))
		    {
		    	return false;
		    }
	        
	        if (!SaveDescription(self, io, sec.Manufacturer))
		    {
		    	return false;
		    }
	        if (!SaveDescription(self, io, sec.Model))
		    {
		    	return false;
		    }
	    }
	    
	    return true;
	}
	
	private static Object Type_ProfileSequenceDesc_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
		return cmsnamed.cmsDupProfileSequenceDescription((cmsSEQ)Ptr);
	}
	
	private static void Type_ProfileSequenceDesc_Free(cmsTagTypeHandler self, Object Ptr)
	{
		cmsnamed.cmsFreeProfileSequenceDescription((cmsSEQ)Ptr);
	}
	
	// ********************************************************************************
	// Type cmsSigProfileSequenceIdType
	// ********************************************************************************
	/*
	In certain workflows using ICC Device Link Profiles, it is necessary to identify the 
	original profiles that were combined to create the Device Link Profile.
	This type is an array of structures, each of which contains information for 
	identification of a profile used in a sequence
	*/
	
	private static final PositionTableEntryFn ReadSeqID = new PositionTableEntryFn()
	{
		public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, Object Cargo, int n, int SizeOfTag)
		{
			cmsSEQ OutSeq = (cmsSEQ)Cargo;
		    cmsPSEQDESC seq = OutSeq.seq[n];
		    
		    if (io.Read.run(io, seq.ProfileID.data, 16, 1) != 1)
		    {
		    	return false;
		    }
		    cmsMLU[] temp = new cmsMLU[]{seq.Description};
		    if (!ReadEmbeddedText(self, io, temp, SizeOfTag))
		    {
		    	return false;
		    }
		    seq.Description = temp[0];
		    
		    return true;
		}
	};
	
	private static Object Type_ProfileSequenceId_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
		cmsSEQ OutSeq;
	    int Count;
	    int BaseOffset;
	    
	    nItems[0] = 0;

	    // Get actual position as a basis for element offsets
	    BaseOffset = io.Tell.run(io) - /*sizeof(_cmsTagBase)*/_cmsTagBase.SIZE;
	    
	    // Get table count
	    int[] temp = new int[1];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;
	    }
	    Count = temp[0];
	    SizeOfTag -= /*sizeof(cmsUInt32Number)*/4;
	    
	    // Allocate an empty structure
	    OutSeq = cmsnamed.cmsAllocProfileSequenceDescription(self.ContextID, Count);
	    if (OutSeq == null)
	    {
	    	return null;
	    }
	    
	    // Read the position table
	    if (!ReadPositionTable(self, io, Count, BaseOffset, OutSeq, ReadSeqID))
	    {
	    	cmsnamed.cmsFreeProfileSequenceDescription(OutSeq);
	        return null;
	    }
	    
	    // Success
	    nItems[0] = 1;
	    return OutSeq;
	}
	
	private static final PositionTableEntryFn WriteSeqID = new PositionTableEntryFn()
	{
		public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, Object Cargo, int n, int SizeOfTag)
		{
			cmsSEQ Seq = (cmsSEQ)Cargo;
		    
		    if (!io.Write.run(io, 16, Seq.seq[n].ProfileID.data))
		    {
		    	return false;
		    }
		    
		    // Store here the MLU
		    if (!SaveDescription(self, io, Seq.seq[n].Description))
		    {
		    	return false;
		    }
		    
		    return true;
		}
	};
	
	private static boolean Type_ProfileSequenceId_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
	    cmsSEQ Seq = (cmsSEQ)Ptr;
	    int BaseOffset;
	    
	    // Keep the base offset
	    BaseOffset = io.Tell.run(io) - /*sizeof(_cmsTagBase)*/_cmsTagBase.SIZE;
	    
	    // This is the table count
	    if (!cmsplugin._cmsWriteUInt32Number(io, Seq.n))
	    {
	    	return false;
	    }
	    
	    // This is the position table and content
	    if (!WritePositionTable(self, io, 0, Seq.n, BaseOffset, Seq, WriteSeqID))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	private static Object Type_ProfileSequenceId_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
		return cmsnamed.cmsDupProfileSequenceDescription((cmsSEQ)Ptr);
	}
	
	private static void Type_ProfileSequenceId_Free(cmsTagTypeHandler self, Object Ptr)
	{
		cmsnamed.cmsFreeProfileSequenceDescription((cmsSEQ)Ptr);
	}
	
	// ********************************************************************************
	// Type cmsSigUcrBgType
	// ********************************************************************************
	/*
	This type contains curves representing the under color removal and black
	generation and a text string which is a general description of the method used
	for the ucr/bg.
	*/
	
	private static Object Type_UcrBg_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
		cmsUcrBg n = new cmsUcrBg();
	    int CountUcr, CountBg;
	    VirtualPointer ASCIIString;
	    
	    nItems[0] = 0;
	    
	    // First curve is Under color removal
	    int[] temp = new int[1];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;
	    }
	    CountUcr = temp[0];
	    SizeOfTag -= /*sizeof(cmsUInt32Number)*/4;
	    
	    n.Ucr = cmsgamma.cmsBuildTabulatedToneCurve16(self.ContextID, CountUcr, null);
	    if (n.Ucr == null)
	    {
	    	return null;
	    }
	    
	    if (!cmsplugin._cmsReadUInt16Array(io, CountUcr, n.Ucr.Table16))
	    {
	    	return null;
	    }
	    SizeOfTag -= CountUcr * /*sizeof(cmsUInt16Number)*/2;
	    
	    // Second curve is Black generation
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;
	    }
	    CountBg = temp[0];
	    SizeOfTag -= /*sizeof(cmsUInt32Number)*/4;
	    
	    n.Bg = cmsBuildTabulatedToneCurve16(self.ContextID, CountBg, null);
	    if (n.Bg == null)
	    {
	    	return null;
	    }
	    if (!cmsplugin._cmsReadUInt16Array(io, CountBg, n.Bg.Table16))
	    {
	    	return null;
	    }
	    SizeOfTag -= CountBg * /*sizeof(cmsUInt16Number)*/2;
	    
	    // Now comes the text. The length is specified by the tag size
	    n.Desc = cmsnamed.cmsMLUalloc(self.ContextID, 1);
	    ASCIIString = cmsplugin._cmsMalloc(self.ContextID, /*sizeof(cmsUInt8Number)*/1*(SizeOfTag + 1));
	    if (io.vpRead(io, ASCIIString, /*sizeof(char)*/1, SizeOfTag) != SizeOfTag)
	    {
	    	return null;
	    }
	    ASCIIString.writeRaw(0, SizeOfTag);
	    cmsnamed.cmsMLUsetASCII(n.Desc, lcms2.cmsNoLanguage, lcms2.cmsNoCountry, ASCIIString.getProcessor().readString(false, false));
	    cmsplugin._cmsFree(self.ContextID, ASCIIString);
	    
	    nItems[0] = 1;
	    return n;
	}
	
	private static boolean Type_UcrBg_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		cmsUcrBg Value = (cmsUcrBg)Ptr;
	    int TextSize;
	    StringBuffer Text;
	    
	    // First curve is Under color removal
	    if (!cmsplugin._cmsWriteUInt32Number(io, Value.Ucr.nEntries))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt16Array(io, Value.Ucr.nEntries, Value.Ucr.Table16))
	    {
	    	return false;
	    }
	    
	    // Then black generation    
	    if (!cmsplugin._cmsWriteUInt32Number(io, Value.Bg.nEntries))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt16Array(io, Value.Bg.nEntries, Value.Bg.Table16))
	    {
	    	return false;
	    }
	    
	    // Now comes the text. The length is specified by the tag size
	    TextSize = cmsnamed.cmsMLUgetASCII(Value.Desc, lcms2.cmsNoLanguage, lcms2.cmsNoCountry, null, 0);
	    Text = new StringBuffer(TextSize);
	    if (cmsnamed.cmsMLUgetASCII(Value.Desc, lcms2.cmsNoLanguage, lcms2.cmsNoCountry, Text, TextSize) != TextSize)
	    {
	    	return false;
	    }
	    
	    if (!io.Write.run(io, TextSize, Text.toString().getBytes()))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	private static Object Type_UcrBg_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
		cmsUcrBg Src = (cmsUcrBg)Ptr;
	    cmsUcrBg NewUcrBg = new cmsUcrBg();
	    
	    NewUcrBg.Bg   = cmsgamma.cmsDupToneCurve(Src.Bg);
	    NewUcrBg.Ucr  = cmsgamma.cmsDupToneCurve(Src.Ucr);
	    NewUcrBg.Desc = cmsnamed.cmsMLUdup(Src.Desc);
	    
	    return NewUcrBg;
	}
	
	private static void Type_UcrBg_Free(cmsTagTypeHandler self, Object Ptr)
	{
		cmsUcrBg Src = (cmsUcrBg)Ptr;
		
		if (Src.Ucr != null)
		{
			cmsgamma.cmsFreeToneCurve(Src.Ucr);
		}
		if (Src.Bg != null)
		{
			cmsgamma.cmsFreeToneCurve(Src.Bg);
		}
		if (Src.Desc != null)
		{
			cmsnamed.cmsMLUfree(Src.Desc);
		}
	}
	
	// ********************************************************************************
	// Type cmsSigCrdInfoType
	// ********************************************************************************
	
	/*
	This type contains the PostScript product name to which this profile corresponds
	and the names of the companion CRDs. Recall that a single profile can generate
	multiple CRDs. It is implemented as a MLU being the language code "PS" and then
	country varies for each element:
	
	                nm: PostScript product name
	                #0: Rendering intent 0 CRD name
	                #1: Rendering intent 1 CRD name
	                #2: Rendering intent 2 CRD name
	                #3: Rendering intent 3 CRD name
	*/
	
	// Auxiliar, read an string specified as count + string
	private static boolean ReadCountAndSting(cmsTagTypeHandler self, cmsIOHANDLER io, cmsMLU mlu, int[] SizeOfTag, final String Section)
	{
	    int Count;
	    VirtualPointer Text;
	    
	    if (SizeOfTag[0] < /*sizeof(cmsUInt32Number)*/4)
	    {
	    	return false;
	    }
	    
	    int[] temp = new int[1];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return false;
	    }
	    Count = temp[0];
	    
	    if (SizeOfTag[0] < Count + /*sizeof(cmsUInt32Number)*/4)
	    {
	    	return false;
	    }
	    Text = cmserr._cmsMalloc(self.ContextID, Count+1);
	    if (Text == null)
	    {
	    	return false;
	    }
	    
	    if (io.vpRead(io, Text, /*sizeof(cmsUInt8Number)*/1, Count) != Count)
	    {
	    	cmserr._cmsFree(self.ContextID, Text);
	        return false;
	    }
	    
	    Text.writeRaw(0, Count);
	    
	    cmsnamed.cmsMLUsetASCII(mlu, "PS", Section, Text.getProcessor().readString(false, false));
	    cmserr._cmsFree(self.ContextID, Text);
	    
	    SizeOfTag[0] -= (Count + /*sizeof(cmsUInt32Number)*/4);
	    return true;    
	}
	
	private static boolean WriteCountAndSting(cmsTagTypeHandler self, cmsIOHANDLER io, cmsMLU mlu, final String Section)
	{
		int TextSize;
		StringBuffer Text;    
	    
	    TextSize = cmsnamed.cmsMLUgetASCII(mlu, "PS", Section, null, 0);
	    Text = new StringBuffer(TextSize);
	    
	    if (!cmsplugin._cmsWriteUInt32Number(io, TextSize))
	    {
	    	return false;
	    }
	    
	    if (cmsnamed.cmsMLUgetASCII(mlu, "PS", Section, Text, TextSize) == 0)
	    {
	    	return false;
	    }
	    
	    if (!io.Write.run(io, TextSize, Text.toString().getBytes()))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	private static Object Type_CrdInfo_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
		cmsMLU mlu = cmsnamed.cmsMLUalloc(self.ContextID, 5);
		
	    nItems[0] = 0;
	    int[] tSize = new int[]{SizeOfTag};
	    if (!ReadCountAndSting(self, io, mlu, tSize, "nm"))
	    {
	    	cmsnamed.cmsMLUfree(mlu);
		    return null;
	    }
	    if (!ReadCountAndSting(self, io, mlu, tSize, "#0"))
	    {
	    	cmsnamed.cmsMLUfree(mlu);
		    return null;
	    }
	    if (!ReadCountAndSting(self, io, mlu, tSize, "#1"))
	    {
	    	cmsnamed.cmsMLUfree(mlu);
		    return null;
	    }
	    if (!ReadCountAndSting(self, io, mlu, tSize, "#2"))
	    {
	    	cmsnamed.cmsMLUfree(mlu);
		    return null;
	    }
	    if (!ReadCountAndSting(self, io, mlu, tSize, "#3"))
	    {
	    	cmsnamed.cmsMLUfree(mlu);
		    return null;
	    }
	    
	    nItems[0] = 1;
	    return mlu;
	}
	
	private static boolean Type_CrdInfo_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		cmsMLU mlu = (cmsMLU)Ptr;
		
	    if (!WriteCountAndSting(self, io, mlu, "nm"))
	    {
	    	return false;
	    }
	    if (!WriteCountAndSting(self, io, mlu, "#0"))
	    {
	    	return false;
	    }
	    if (!WriteCountAndSting(self, io, mlu, "#1"))
	    {
	    	return false;
	    }
	    if (!WriteCountAndSting(self, io, mlu, "#2"))
	    {
	    	return false;
	    }
	    if (!WriteCountAndSting(self, io, mlu, "#3"))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	private static Object Type_CrdInfo_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
		return cmsnamed.cmsMLUdup((cmsMLU)Ptr);
	}
	
	private static void Type_CrdInfo_Free(cmsTagTypeHandler self, Object Ptr)
	{
		cmsnamed.cmsMLUfree((cmsMLU)Ptr);
	}
	
	// ********************************************************************************
	// Type cmsSigScreeningType
	// ********************************************************************************
	//
	//The screeningType describes various screening parameters including screen
	//frequency, screening angle, and spot shape.
	
	private static Object Type_Screening_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
		cmsScreening sc = null;
	    int i;
	    
	    sc = new cmsScreening();
	    
	    nItems[0] = 0;
	    
	    int[] temp = new int[1];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;
	    }
	    sc.Flag = temp[0];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;
	    }
	    sc.nChannels = temp[0];
	    
	    double[] temp2 = new double[1];
	    for (i = 0; i < sc.nChannels; i++)
	    {
	        if (!cmsplugin._cmsRead15Fixed16Number(io, temp2))
		    {
		    	return null;
		    }
	        sc.Channels[i].Frequency = temp2[0];
	        if (!cmsplugin._cmsRead15Fixed16Number(io, temp2))
		    {
		    	return null;
		    }
	        sc.Channels[i].ScreenAngle = temp2[0];
	        if (!cmsplugin._cmsReadUInt32Number(io, temp))
		    {
		    	return null;
		    }
	        sc.Channels[i].SpotShape = temp[0];
	    }
	    
	    nItems[0] = 1;
	    
	    return sc;
	}
	
	private static boolean Type_Screening_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		cmsScreening sc = (cmsScreening)Ptr; 
	    int i;
	    
	    if (!cmsplugin._cmsWriteUInt32Number(io, sc.Flag))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, sc.nChannels))
	    {
	    	return false;
	    }
	    
	    for (i = 0; i < sc.nChannels; i++)
	    {
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, sc.Channels[i].Frequency))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWrite15Fixed16Number(io, sc.Channels[i].ScreenAngle))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWriteUInt32Number(io, sc.Channels[i].SpotShape))
		    {
		    	return false;
		    }
	    }
	    
	    return true;
	}
	
	private static Object Type_Screening_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
		return cmserr._cmsDupMem(self.ContextID, new VirtualPointer(Ptr), /*sizeof(cmsScreening)*/cmsScreening.SIZE).getProcessor().readObject(cmsScreening.class);
	}
	
	private static void Type_Screening_Free(cmsTagTypeHandler self, Object Ptr)
	{
		//Would free memory but never actually allocating memory (except for dup/read functions which is not a real issue)
	}
	
	// ********************************************************************************
	// Type cmsSigViewingConditionsType
	// ********************************************************************************
	//
	//This type represents a set of viewing condition parameters including: 
	//CIE ’absolute’ illuminant white point tristimulus values and CIE ’absolute’ 
	//surround tristimulus values.
	
	private static Object Type_ViewingConditions_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
		cmsICCViewingConditions vc = null;
	    
	    vc = new cmsICCViewingConditions();
	    
	    nItems[0] = 0;
	    
	    if (!cmsplugin._cmsReadXYZNumber(io, vc.IlluminantXYZ))
	    {
	    	return null;
	    }
	    if (!cmsplugin._cmsReadXYZNumber(io, vc.SurroundXYZ))
	    {
	    	return null;
	    }
	    int[] temp = new int[1];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;
	    }
	    vc.IlluminantType = temp[0];
	    
	    nItems[0] = 1;
	    
	    return vc;
	}
	
	private static boolean Type_ViewingConditions_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		cmsICCViewingConditions sc = (cmsICCViewingConditions)Ptr; 
        
	    if (!cmsplugin._cmsWriteXYZNumber(io, sc.IlluminantXYZ))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteXYZNumber(io, sc.SurroundXYZ))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, sc.IlluminantType))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	private static Object Type_ViewingConditions_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
		return cmserr._cmsDupMem(self.ContextID, new VirtualPointer(Ptr), /*sizeof(cmsICCViewingConditions)*/cmsScreening.SIZE).getProcessor().readObject(cmsICCViewingConditions.class);
	}
	
	private static void Type_ViewingConditions_Free(cmsTagTypeHandler self, Object Ptr)
	{
		//Would free memory but never actually allocating memory (except for dup/read functions which is not a real issue)
	}
	
	// ********************************************************************************
	// Type cmsSigMultiProcessElementType
	// ********************************************************************************
	
	private static Object GenericMPEdup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
		return cmslut.cmsStageDup((cmsStage)Ptr);
	}
	
	private static void GenericMPEfree(cmsTagTypeHandler self, Object Ptr)
	{
		cmslut.cmsStageFree((cmsStage)Ptr);
	}
	
	// Each curve is stored in one or more curve segments, with break-points specified between curve segments.
	// The first curve segment always starts at –Infinity, and the last curve segment always ends at +Infinity. The
	// first and last curve segments shall be specified in terms of a formula, whereas the other segments shall be
	// specified either in terms of a formula, or by a sampled curve.
	
	// Read an embedded segmented curve
	private static cmsToneCurve ReadSegmentedCurve(cmsTagTypeHandler self, cmsIOHANDLER io)
	{
		int ElementSig;
	    int i, j;
	    short nSegments;
	    cmsCurveSegment[] Segments;
	    cmsToneCurve Curve;
	    float PrevBreak = -1E22F; // - infinite
	    
	    // Take signature and channels for each element.
	    int[] temp = new int[1];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;
	    }
	    ElementSig = temp[0];
	    
	    // That should be a segmented curve
	    if (ElementSig != lcms2.cmsSigSegmentedCurve)
	    {
	    	return null;
	    }
	    
	    if (!cmsplugin._cmsReadUInt32Number(io, null))
	    {
	    	return null;
	    }
	    /*
	    short[] temp2 = new short[1];
	    if (!cmsplugin._cmsReadUInt16Number(io, temp2))
	    {
	    	return null;
	    }
	    nSegments = temp2[0];
	    if (!cmsplugin._cmsReadUInt16Number(io, null))
	    {
	    	return null;
	    }
	    */
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;
	    }
	    nSegments = (short)temp[0];
	    Segments = new cmsCurveSegment[nSegments];
	    
	    // Read breakpoints
	    //float[] temp3 = new float[1];
	    for (i = 0; i < nSegments - 1; i++)
	    {
	    	Segments[i].x0 = PrevBreak;
	    	//if (!cmsplugin._cmsReadFloat32Number(io, temp3))
    		if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    	{
	    		return null;
	    	}
	    	//Segments[i].x1 = temp3[0];
    		Segments[i].x1 = Float.intBitsToFloat(temp[0]);
	    	PrevBreak = Segments[i].x1;
	    }
	    
	    Segments[nSegments-1].x0 = PrevBreak;
	    Segments[nSegments-1].x1 = 1E22F; // A big float number
	    
	    // Read segments
	    for (i = 0; i < nSegments; i++)
	    {
	    	if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    	{
	    		return null;
	    	}
	    	ElementSig = temp[0];
	    	if (!cmsplugin._cmsReadUInt32Number(io, null))
	    	{
	    		return null;
	    	}
	    	
	    	switch (ElementSig)
	    	{
	    		case lcms2.cmsSigFormulaCurveSeg:
		    		{
		    			short Type;
		    			int[] ParamsByType = {4, 5, 5 };
		    			
		    			/*
		                if (!cmsplugin._cmsReadUInt16Number(io, temp2))
		                {
		                	return null;
		                }
		                Type = temp2[0];
		                if (!cmsplugin._cmsReadUInt16Number(io, null))
		                {
		                	return null;
		                }
		                */
		    			if (!cmsplugin._cmsReadUInt32Number(io, temp))
		                {
		                	return null;
		                }
		    			Type = (short)temp[0];
		                
		                Segments[i].Type = Type + 6;
		                if (Type > 2)
		                {
		                	return null;
		                }
		                
		                for (j = 0; j < ParamsByType[Type]; j++)
		                {
		                	float f;
		                	/*
		                	if (!cmsplugin._cmsReadFloat32Number(io, temp3))
		                	{
		                		return null;
		                	}
		                	f = temp3[0];
		                	*/
		                	if (!cmsplugin._cmsReadUInt32Number(io, temp))
		                	{
		                		return null;
		                	}
		                	f = Float.intBitsToFloat(temp[0]);
		                    Segments[i].Params[j] = f;
		                }
	                }
	                break;
                case lcms2.cmsSigSampledCurveSeg:
                	{
                		int Count;
                		
                		if (!cmsplugin._cmsReadUInt32Number(io, temp))
		                {
		                	return null;
		                }
                		Count = temp[0];
                		
                		Segments[i].nGridPoints = Count;
                		Segments[i].SampledPoints = new float[Count];
                		
                		for (j = 0; j < Count; j++)
                		{
                			/*
                			if (!cmsplugin._cmsReadFloat32Number(io, temp3))
                			{
                				return null;
                			}
                			Segments[i].SampledPoints[j] = temp3[0];
                			*/
                			if (!cmsplugin._cmsReadUInt32Number(io, temp))
                			{
                				return null;
                			}
                			Segments[i].SampledPoints[j] = Float.intBitsToFloat(temp[0]);
                		}
	                }
	                break;
	            default:
	                {
	                	StringBuffer String = new StringBuffer(4);
	                	
	                	cmserr._cmsTagSignature2String(String, ElementSig);
	                	cmserr.cmsSignalError(self.ContextID, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSTYPES_UNK_CURVE_ELEMENT_TYPE_NOT_FOUND), new Object[]{String});
	                }
	                return null;
	         }
	    }
	    
	    Curve = cmsgamma.cmsBuildSegmentedToneCurve(self.ContextID, nSegments, Segments);
	    
	    return Curve;
	}
	
	private static final PositionTableEntryFn ReadMPECurve = new PositionTableEntryFn()
	{
		public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, Object Cargo, int n, int SizeOfTag)
		{
			cmsToneCurve[] GammaTables = (cmsToneCurve[])Cargo;
		      
		      GammaTables[n] = ReadSegmentedCurve(self, io);
		      return (GammaTables[n] != null);
		}
	};
	
	private static Object Type_MPEcurve_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
		cmsStage mpe = null;
	    short InputChans, OutputChans;
	    int i, BaseOffset;
	    cmsToneCurve[] GammaTables;
	    
	    nItems[0] = 0;
	    
	    // Get actual position as a basis for element offsets
	    BaseOffset = io.Tell.run(io) - /*sizeof(_cmsTagBase)*/_cmsTagBase.SIZE;
	    
	    short[] temp = new short[1];
	    if (!cmsplugin._cmsReadUInt16Number(io, temp))
	    {
	    	return null;
	    }
	    InputChans = temp[0];
	    if (!cmsplugin._cmsReadUInt16Number(io, temp))
	    {
	    	return null;
	    }
	    OutputChans = temp[0];
	    
	    if (InputChans != OutputChans)
	    {
	    	return null;
	    }
	    
	    GammaTables = new cmsToneCurve[InputChans];
	    
	    if (ReadPositionTable(self, io, InputChans, BaseOffset, GammaTables, ReadMPECurve))
	    {
	        mpe = cmslut.cmsStageAllocToneCurves(self.ContextID, InputChans, GammaTables);
	    }
	    else
	    {
	        mpe = null;
	    }
	    
	    for (i = 0; i < InputChans; i++)
	    {
	        if (GammaTables[i] != null)
	        {
	        	cmsgamma.cmsFreeToneCurve(GammaTables[i]);
	        }
	    }
	    
	    nItems[0] = (mpe != null) ? 1 : 0;
	    return mpe;
	}
	
	// Write a single segmented curve. NO CHECK IS PERFORMED ON VALIDITY
	private static boolean WriteSegmentedCurve(cmsIOHANDLER io, cmsToneCurve g)
	{
	    int i, j;
	    cmsCurveSegment[] Segments = g.Segments;
	    int nSegments = g.nSegments;
	    
	    if (!cmsplugin._cmsWriteUInt32Number(io, lcms2.cmsSigSegmentedCurve))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, 0))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt16Number(io, (short)nSegments))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt16Number(io, 0))
	    {
	    	return false;
	    }
	    
	    // Write the break-points
	    for (i = 0; i < nSegments - 1; i++)
	    {
	        if (!cmsplugin._cmsWriteFloat32Number(io, Segments[i].x1))
	        {
	        	return false;
	        }
	    }
	    
	    // Write the segments
	    for (i = 0; i < g.nSegments; i++)
	    {
	        cmsCurveSegment ActualSeg = Segments[i];
	        
	        if (ActualSeg.Type == 0)
	        {
	            // This is a sampled curve
	            if (!cmsplugin._cmsWriteUInt32Number(io, lcms2.cmsSigSampledCurveSeg))
	            {
	            	return false;
	            }
	            if (!cmsplugin._cmsWriteUInt32Number(io, 0))
	            {
	            	return false;
	            }
	            if (!cmsplugin._cmsWriteUInt32Number(io, ActualSeg.nGridPoints))
	            {
	            	return false;
	            }
	            
	            for (j = 0; j < g.Segments[i].nGridPoints; j++)
	            {
	                if (!cmsplugin._cmsWriteFloat32Number(io, ActualSeg.SampledPoints[j]))
		            {
		            	return false;
		            }
	            }
	        }
	        else
	        {
	            int Type;
	            int[] ParamsByType = { 4, 5, 5 };
	            
	            // This is a formula-based
	            if (!cmsplugin._cmsWriteUInt32Number(io, lcms2.cmsSigFormulaCurveSeg))
		        {
		        	return false;
		        }
	            if (!cmsplugin._cmsWriteUInt32Number(io, 0))
		        {
		        	return false;
		        }
	            
	            // We only allow 1, 2 and 3 as types
	            Type = ActualSeg.Type - 6;
	            if (Type > 2 || Type < 0)
		        {
		        	return false;
		        }
	            
	            if (!cmsplugin._cmsWriteUInt16Number(io, (short)Type))
		        {
		        	return false;
		        }
	            if (!cmsplugin._cmsWriteUInt16Number(io, 0))
		        {
		        	return false;
		        }
	            
	            for (j = 0; j < ParamsByType[Type]; j++)
	            {
	                if (!cmsplugin._cmsWriteFloat32Number(io, (float)ActualSeg.Params[j]))
	    	        {
	    	        	return false;
	    	        }
	            }
	        }
	        
	        // It seems there is no need to align. Code is here, and for safety commented out
	        /*
	        if (!cmsplugin._cmsWriteAlignment(io))
	        {
	        	return false;
	        }
	        */
	    }
	    
	    return true;
	}
	
	private static final PositionTableEntryFn WriteMPECurve = new PositionTableEntryFn()
	{
		public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, Object Cargo, int n, int SizeOfTag)
		{
			_cmsStageToneCurvesData Curves  = (_cmsStageToneCurvesData)Cargo;
			
		    return WriteSegmentedCurve(io, Curves.TheCurves[n]);
		}
	};
	
	// Write a curve, checking first for validity
	private static boolean Type_MPEcurve_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		int BaseOffset;
	    cmsStage mpe = (cmsStage)Ptr;
	    _cmsStageToneCurvesData Curves = (_cmsStageToneCurvesData)mpe.Data;
	    
	    BaseOffset = io.Tell.run(io) - /*sizeof(_cmsTagBase)*/_cmsTagBase.SIZE;
	    
	    // Write the header. Since those are curves, input and output channels are same
	    if (!cmsplugin._cmsWriteUInt16Number(io, (short)mpe.InputChannels))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt16Number(io, (short)mpe.InputChannels))
	    {
	    	return false;
	    }
	    
	    if (!WritePositionTable(self, io, 0, mpe.InputChannels, BaseOffset, Curves, WriteMPECurve))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	// The matrix is organized as an array of PxQ+Q elements, where P is the number of input channels to the
	// matrix, and Q is the number of output channels. The matrix elements are each float32Numbers. The array
	// is organized as follows:
	// array = [e11, e12, …, e1P, e21, e22, …, e2P, …, eQ1, eQ2, …, eQP, e1, e2, …, eQ]
	
	private static Object Type_MPEmatrix_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
		cmsStage mpe;
	    short InputChans, OutputChans;
	    int nElems, i;  
	    double[] Matrix;
	    double[] Offsets;
	    
	    short[] temp = new short[1];
	    if (!cmsplugin._cmsReadUInt16Number(io, temp))
	    {
	    	return null;
	    }
	    InputChans = temp[0];
	    if (!cmsplugin._cmsReadUInt16Number(io, temp))
	    {
	    	return null;
	    }
	    OutputChans = temp[0];
	    
	    nElems = InputChans * OutputChans;
	    
	    // Input and output chans may be ANY (up to 0xffff)
	    Matrix = new double[nElems];
	    
	    Offsets = new double[OutputChans];
	    
	    float[] temp2 = new float[1];
	    for (i = 0; i < nElems; i++)
	    {
	        if (!cmsplugin._cmsReadFloat32Number(io, temp2))
		    {
		    	return null;
		    }
	        Matrix[i] = temp2[0];
	    }
	    
	    for (i = 0; i < OutputChans; i++)
	    {
	        if (!cmsplugin._cmsReadFloat32Number(io, temp2))
		    {
		    	return null;
		    }
	        Offsets[i] = temp2[0];
	    }
	    
	    mpe = cmslut.cmsStageAllocMatrix(self.ContextID, OutputChans, InputChans, Matrix, Offsets);
	    
	    nItems[0] = 1;
	    
	    return mpe;
	}
	
	private static boolean Type_MPEmatrix_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		int i, nElems;
	    cmsStage mpe = (cmsStage)Ptr;
	    _cmsStageMatrixData Matrix = (_cmsStageMatrixData)mpe.Data;
	    
	    if (!cmsplugin._cmsWriteUInt16Number(io, (short)mpe.InputChannels))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt16Number(io, (short)mpe.OutputChannels))
	    {
	    	return false;
	    }
	    
	    nElems = mpe.InputChannels * mpe.OutputChannels;
	    
	    for (i = 0; i < nElems; i++)
	    {
	        if (!cmsplugin._cmsWriteFloat32Number(io, (float)Matrix.Double[i]))
		    {
		    	return false;
		    }
	    }
	    
	    for (i = 0; i < mpe.OutputChannels; i++)
	    {
	        if (Matrix.Offset == null)
	        {
	        	if (!cmsplugin._cmsWriteFloat32Number(io, 0))
	    	    {
	    	    	return false;
	    	    }
	        }
	        else
	        {
	        	if (!cmsplugin._cmsWriteFloat32Number(io, (float)Matrix.Offset[i]))
	       	    {
	       	    	return false;
	       	    }
	        }
	    }
	    
	    return true;
	}
	
	private static Object Type_MPEclut_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
		cmsStage mpe = null;
	    short InputChans, OutputChans;
	    byte[] Dimensions8 = new byte[16];
	    int i, nMaxGrids;
	    int[] GridPoints = new int[lcms2_plugin.MAX_INPUT_DIMENSIONS];
	    _cmsStageCLutData clut;
	    
	    short[] temp = new short[1];
	    if (!cmsplugin._cmsReadUInt16Number(io, temp))
	    {
	    	return null;
	    }
	    InputChans = temp[0];
	    if (!cmsplugin._cmsReadUInt16Number(io, temp))
	    {
	    	return null;
	    }
	    OutputChans = temp[0];
	    
	    if (io.Read.run(io, Dimensions8, /*sizeof(cmsUInt8Number)*/1, 16) != 16)
	    {
	    	nItems[0] = 0;
		    if (mpe != null)
		    {
		    	cmslut.cmsStageFree(mpe);
		    }
		    return null;
	    }
	    
	    // Copy MAX_INPUT_DIMENSIONS at most. Expand to cmsUInt32Number
	    nMaxGrids = InputChans > lcms2_plugin.MAX_INPUT_DIMENSIONS ? lcms2_plugin.MAX_INPUT_DIMENSIONS : InputChans;
	    for (i = 0; i < nMaxGrids; i++)
	    {
	    	GridPoints[i] = Dimensions8[i];
	    }
	    
	    // Allocate the true CLUT
	    mpe = cmslut.cmsStageAllocCLutFloatGranular(self.ContextID, GridPoints, InputChans, OutputChans, null);
	    if (mpe == null)
	    {
	    	nItems[0] = 0;
		    if (mpe != null)
		    {
		    	cmslut.cmsStageFree(mpe);
		    }
		    return null;
	    }
	    
	    // Read the data
	    clut = (_cmsStageCLutData)mpe.Data;
	    float[] temp2 = new float[1];
	    VirtualPointer.TypeProcessor proc = clut.Tab.getProcessor();
	    int pos = clut.Tab.getPosition();
	    for (i = 0; i < clut.nEntries; i++)
	    {
	        if (!cmsplugin._cmsReadFloat32Number(io, temp2))
	        {
	        	nItems[0] = 0;
	    	    if (mpe != null)
	    	    {
	    	    	cmslut.cmsStageFree(mpe);
	    	    }
	    	    return null;
	        }
	        proc.write(temp2[0], true);
	    }
	    clut.Tab.setPosition(pos);
	    
	    nItems[0] = 1;    
	    return mpe;
	}
	
	// Write a CLUT in floating point
	private static boolean Type_MPEclut_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		byte[] Dimensions8 = new byte[16];
	    int i;
	    cmsStage mpe = (cmsStage)Ptr;
	    _cmsStageCLutData clut = (_cmsStageCLutData)mpe.Data;
	    
	    // Check for maximum number of channels
	    if (mpe.InputChannels > 15)
	    {
	    	return false;
	    }
	    
	    // Only floats are supported in MPE
	    if (!clut.HasFloatValues)
	    {
	    	return false;
	    }
	    
	    if (!cmsplugin._cmsWriteUInt16Number(io, (short)mpe.InputChannels))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt16Number(io, (short)mpe.OutputChannels))
	    {
	    	return false;
	    }
	    
	    for (i = 0; i < mpe.InputChannels; i++)
	    {
	    	Dimensions8[i] = (byte)clut.Params.nSamples[i];
	    }
	    
	    if (!io.Write.run(io, 16, Dimensions8))
	    {
	    	return false;
	    }
	    
	    VirtualPointer.TypeProcessor proc = clut.Tab.getProcessor();
	    int pos = clut.Tab.getPosition();
	    for (i = 0; i < clut.nEntries; i++)
	    {
	        if (!cmsplugin._cmsWriteFloat32Number(io, proc.readFloat(true)))
		    {
		    	return false;
		    }
	    }
	    clut.Tab.setPosition(pos);

	    return true;
	}
	
	private static _cmsTagTypeLinkedList SupportedMPEtypes;
	
	private static void setupMPEtypes()
	{
		_cmsTagTypeLinkedList tempMPETypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigCLutElemType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_MPEclut_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_MPEclut_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return GenericMPEdup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				GenericMPEfree(self, Ptr);
			}
		}), null);
		tempMPETypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigMatrixElemType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_MPEmatrix_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_MPEmatrix_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return GenericMPEdup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				GenericMPEfree(self, Ptr);
			}
		}), tempMPETypes);
		tempMPETypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigCurveSetElemType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_MPEcurve_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_MPEcurve_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return GenericMPEdup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				GenericMPEfree(self, Ptr);
			}
		}), tempMPETypes);
		
		tempMPETypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigEAcsElemType, null, null, null, null), tempMPETypes);		// Ignore those elements for now
		SupportedMPEtypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigBAcsElemType, null, null, null, null), tempMPETypes);	// (That's what the spec says)
	}
	
	private static final int DEFAULT_MPE_TYPE_COUNT = 5;
	
	private static final PositionTableEntryFn ReadMPEElem = new PositionTableEntryFn()
	{
		public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, Object Cargo, int n, int SizeOfTag)
		{
			int ElementSig;
		    cmsTagTypeHandler TypeHandler;
		    cmsStage mpe = null;
		    int nItems;
		    cmsPipeline NewLUT = (cmsPipeline)Cargo;
		    
		    // Take signature and channels for each element.
		    int[] temp = new int[1];
		    if (!cmsplugin._cmsReadUInt32Number(io, temp))
		    {
		    	return false;
		    }
		    ElementSig = temp[0];
		    
		    // The reserved placeholder
		    if (!cmsplugin._cmsReadUInt32Number(io, null))
		    {
		    	return false;
		    }
		    
		    // Read diverse MPE types
		    TypeHandler = GetHandler(ElementSig, SupportedMPEtypes);
		    if (TypeHandler == null)
		    {
		        StringBuffer String = new StringBuffer(4);
		        
		        cmserr._cmsTagSignature2String(String, ElementSig);
		        
		        // An unknown element was found. 
		        cmserr.cmsSignalError(self.ContextID, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSTYPES_UNK_MPE_TYPE_NOT_FOUND), new Object[]{String});
		        return false;
		    }
		    
		    // If no read method, just ignore the element (valid for cmsSigBAcsElemType and cmsSigEAcsElemType)
		    // Read the MPE. No size is given
		    if (TypeHandler.ReadPtr != null)
		    {
		        // This is a real element which should be read and processed
		    	temp[0] = nItems;
		        mpe = (cmsStage)TypeHandler.ReadPtr.run(self, io, temp, SizeOfTag);
		        nItems = temp[0];
		        if (mpe == null)
			    {
			    	return false;
			    }
		        
		        // All seems ok, insert element
		        cmslut.cmsPipelineInsertStage(NewLUT, lcms2.cmsAT_END, mpe);
		    }
		    
		    return true;
		}
	};
	
	// This is the main dispatcher for MPE
	private static Object Type_MPE_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
		short InputChans, OutputChans;
	    int[] ElementCount = new int[1];
	    cmsPipeline NewLUT = null;
	    int BaseOffset;
	    
	    // Get actual position as a basis for element offsets
	    BaseOffset = io.Tell.run(io) - /*sizeof(_cmsTagBase)*/_cmsTagBase.SIZE;
	    
	    // Read channels and element count
	    short[] temp = new short[1];
	    if (!cmsplugin._cmsReadUInt16Number(io, temp))
	    {
	    	return null;
	    }
	    InputChans = temp[0];
	    if (!cmsplugin._cmsReadUInt16Number(io, temp))
	    {
	    	return null;
	    }
	    OutputChans = temp[0];
	    
	    // Allocates an empty LUT
	    NewLUT = cmslut.cmsPipelineAlloc(self.ContextID, InputChans, OutputChans);
	    if (NewLUT == null)
	    {
	    	return null;
	    }
	    
	    if (!cmsplugin._cmsReadUInt32Number(io, ElementCount))
	    {
	    	return null
	    }
	    
	    if (!ReadPositionTable(self, io, ElementCount[0], BaseOffset, NewLUT, ReadMPEElem))
	    {
	        if (NewLUT != null)
	        {
	        	cmslut.cmsPipelineFree(NewLUT);
	        }
	        nItems[0] = 0;
	        return null;
	    }
	    
	    // Success
	    nItems[0] = 1;
	    return NewLUT;
	}
	
	// This one is a liitle bit more complex, so we don't use position tables this time.
	private static boolean Type_MPE_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		int i, BaseOffset, DirectoryPos, CurrentPos;
	    int inputChan, outputChan;
	    int ElemCount, Before;
	    int[] ElementOffsets = null, ElementSizes = null;
	    int ElementSig;
	    cmsPipeline Lut = (cmsPipeline)Ptr;
	    cmsStage Elem = Lut.Elements;
	    cmsTagTypeHandler TypeHandler;
	    
	    BaseOffset = io.Tell.run(io) - /*sizeof(_cmsTagBase)*/_cmsTagBase.SIZE;
	    
	    inputChan  = cmslut.cmsPipelineInputChannels(Lut);
	    outputChan = cmslut.cmsPipelineOutputChannels(Lut);
	    ElemCount  = cmslut.cmsPipelineStageCount(Lut);
	    
	    ElementOffsets = new int[ElemCount];
	    
	    ElementSizes = new int[ElemCount];
	    
	    // Write the head
	    if (!cmsplugin._cmsWriteUInt16Number(io, (short)inputChan))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt16Number(io, (short)outputChan))
	    {
	    	return false;
	    }
	    if (!cmsplugin._cmsWriteUInt32Number(io, (short)ElemCount))
	    {
	    	return false;
	    }
	    
	    DirectoryPos = io.Tell.run(io);
	    
	    // Write a fake directory to be filled latter on
	    for (i = 0; i < ElemCount; i++)
	    {
	        if (!cmsplugin._cmsWriteUInt32Number(io, 0)) // Offset 
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWriteUInt32Number(io, 0)) // size
		    {
		    	return false;
		    }
	    }
	    
	    // Write each single tag. Keep track of the size as well.
	    for (i = 0; i < ElemCount; i++)
	    {
	        ElementOffsets[i] = io.Tell.run(io) - BaseOffset;
	        
	        ElementSig = Elem.Type;
	        
	        TypeHandler = GetHandler(ElementSig, SupportedMPEtypes);
	        if (TypeHandler == null)
	        {
	        	StringBuffer String = new StringBuffer(4);
	                
                cmserr._cmsTagSignature2String(String, ElementSig);
                
                // An unknow element was found.
                cmserr.cmsSignalError(self.ContextID, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSTYPES_UNK_MPE_TYPE), new Object[]{String});
                return false;
	        }
	        
	        if (!cmsplugin._cmsWriteUInt32Number(io, ElementSig))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWriteUInt32Number(io, 0))
		    {
		    	return false;
		    }
	        Before = io.Tell.run(io);
	        if (!TypeHandler.WritePtr.run(self, io, Elem, 1))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWriteAlignment(io))
		    {
		    	return false;
		    }
	        
	        ElementSizes[i] = io.Tell.run(io) - Before;
	        
	        Elem = Elem.Next;
	    }
	    
	    // Write the directory
	    CurrentPos = io.Tell.run(io);
	    
	    if (!io.Seek.run(io, DirectoryPos))
	    {
	    	return false;
	    }
	    
	    for (i = 0; i < ElemCount; i++)
	    {
	        if (!cmsplugin._cmsWriteUInt32Number(io, ElementOffsets[i]))
		    {
		    	return false;
		    }
	        if (!cmsplugin._cmsWriteUInt32Number(io, ElementSizes[i]))
		    {
		    	return false;
		    }
	    }
	    
	    if (!io.Seek.run(io, CurrentPos))
	    {
	    	return false;
	    }
	    
	    return true;
	}
	
	private static Object Type_MPE_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
		return cmslut.cmsPipelineDup((cmsPipeline)Ptr);
	}
	
	private static void Type_MPE_Free(cmsTagTypeHandler self, Object Ptr)
	{
		cmslut.cmsPipelineFree((cmsPipeline)Ptr);
	}
	
	// ********************************************************************************
	// Type cmsSigVcgtType
	// ********************************************************************************
	
	private static final int cmsVideoCardGammaTableType = 0;
	private static final int cmsVideoCardGammaFormulaType = 1;
	
	// Used internally
	private static class _cmsVCGTGAMMA
	{
		public double Gamma;
		public double Min;
		public double Max;
	}
	
	private static Object Type_vcgt_Read(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag)
	{
		int TagType, n, i;
	    cmsToneCurve[] Curves;
	    
	    nItems[0] = 0;
	    
	    // Read tag type
	    int[] temp = new int[1];
	    if (!cmsplugin._cmsReadUInt32Number(io, temp))
	    {
	    	return null;
	    }
	    TagType = temp[0];
	    
	    // Allocate space for the array
	    Curves = new cmsToneCurve[3];
	    
	    short[] temp2 = new short[1];
	    // There are two possible flavors
	    switch (TagType)
	    {
		    // Gamma is stored as a table
		    case cmsVideoCardGammaTableType: 
		    {
		    	short nChannels, nElems, nBytes;
		    	
		    	// Check channel count, which should be 3 (we don't support monochrome this time)
		    	if (!cmsplugin._cmsReadUInt16Number(io, temp2))
		    	{
		    		// Regret, free all resources
		    		cmsgamma.cmsFreeToneCurveTriple(Curves);
		    	    return null;
		    	}
		    	nChannels = temp2[0];
		    	
		    	if (nChannels != 3)
		    	{
		    		cmserr.cmsSignalError(self.ContextID, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSTYPES_INVALID_VCGT_CHANNELS), new Object[]{new Short(nChannels)});
		    		// Regret, free all resources
		    		cmsgamma.cmsFreeToneCurveTriple(Curves);
		    	    return null;
		    	}
		    	
		    	// Get Table element count and bytes per element
		    	if (!cmsplugin._cmsReadUInt16Number(io, temp2))
		    	{
		    		// Regret, free all resources
		    		cmsgamma.cmsFreeToneCurveTriple(Curves);
		    	    return null;
		    	}
		    	nElems = temp2[0];
		    	if (!cmsplugin._cmsReadUInt16Number(io, temp2))
		    	{
		    		// Regret, free all resources
		    		cmsgamma.cmsFreeToneCurveTriple(Curves);
		    	    return null;
		    	}
		    	nBytes = temp2[0];
		    	
		    	// Populate tone curves
		    	for (n = 0; n < 3; n++)
		    	{
		    		Curves[n] = cmsgamma.cmsBuildTabulatedToneCurve16(self.ContextID, nElems, null);
		    		if (Curves[n] == null)
			    	{
			    		// Regret, free all resources
			    		cmsgamma.cmsFreeToneCurveTriple(Curves);
			    	    return null;
			    	}
		    		
		    		// On depending on byte depth
		    		switch (nBytes)
		    		{
			    		// One byte, 0..255
			    		case 1:
			    			byte[] v = new byte[1];
			    			for (i = 0; i < nElems; i++)
			    			{
			    				if (!cmsplugin._cmsReadUInt8Number(io, v))
			    		    	{
			    		    		// Regret, free all resources
			    		    		cmsgamma.cmsFreeToneCurveTriple(Curves);
			    		    	    return null;
			    		    	}
			    				Curves[n].Table16[i] = lcms2_internal.FROM_8_TO_16(v[0]);
		    				}
			    			break;
		    			// One word 0..65535
		    			case 2:
		    				if (!cmsplugin._cmsReadUInt16Array(io, nElems, Curves[n].Table16))
		    		    	{
		    		    		// Regret, free all resources
		    		    		cmsgamma.cmsFreeToneCurveTriple(Curves);
		    		    	    return null;
		    		    	}
		    				break;
						// Unsupported
						default:
							cmserr.cmsSignalError(self.ContextID, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSTYPES_INVALID_VCGT_BIT_DEPTH), new Object[]{new Integer(nBytes * 8)});
							// Regret, free all resources
	    		    		cmsgamma.cmsFreeToneCurveTriple(Curves);
	    		    	    return null;
		    		}
		    	} // For all 3 channels
		    }
		    break;
		    
		    // In this case, gamma is stored as a formula
		    case cmsVideoCardGammaFormulaType:
		    {
		    	_cmsVCGTGAMMA[] Colorant = new _cmsVCGTGAMMA[3];
		    	
		    	double[] Params = new double[10];
		    	// Populate tone curves
		    	for (n = 0; n < 3; n++)
		    	{
		    		if (!_cmsRead15Fixed16Number(io, Params))
			    	{
			    		// Regret, free all resources
			    		cmsgamma.cmsFreeToneCurveTriple(Curves);
			    	    return null;
			    	}
		    		Colorant[n].Gamma = Params[0];
		    		if (!_cmsRead15Fixed16Number(io, Params))
			    	{
			    		// Regret, free all resources
			    		cmsgamma.cmsFreeToneCurveTriple(Curves);
			    	    return null;
			    	}
		    		Colorant[n].Min = Params[0];
		    		if (!_cmsRead15Fixed16Number(io, Params))
			    	{
			    		// Regret, free all resources
			    		cmsgamma.cmsFreeToneCurveTriple(Curves);
			    	    return null;
			    	}
		    		Colorant[n].Max = Params[0];
		    		
		            // Parametric curve type 5 is:
		            // Y = (aX + b)^Gamma + e | X >= d
		            // Y = cX + f             | X < d
		    		
		            // vcgt formula is:
		            // Y = (Max – Min) * (X ^ Gamma) + Min
		            
		            // So, the translation is
		            // a = (Max – Min) ^ ( 1 / Gamma) 
		            // e = Min
		            // b=c=d=f=0
		    		
		    		Params[0] = Colorant[n].Gamma;
		    		Params[1] = pow((Colorant[n].Max - Colorant[n].Min), (1.0 / Colorant[n].Gamma));
		    		Params[2] = 0;
		    		Params[3] = 0;
		    		Params[4] = 0;
		    		Params[5] = Colorant[n].Min;
		    		Params[6] = 0;
		    		
		    		Curves[n] = cmsgamma.cmsBuildParametricToneCurve(self.ContextID, 5, Params);
		    		if (Curves[n] == null)
			    	{
			    		// Regret, free all resources
			    		cmsgamma.cmsFreeToneCurveTriple(Curves);
			    	    return null;
			    	}
		    	}
		    }
		    break;
		    
		    // Unsupported
		    default:
		    	cmserr.cmsSignalError(self.ContextID, lcms2.cmsERROR_UNKNOWN_EXTENSION, Utility.LCMS_Resources.getString(LCMSResource.CMSTYPES_INVALID_VCGT_TAG_TYPE), new Object[]{new Integer(TagType)});
	    		// Regret, free all resources
	    		cmsgamma.cmsFreeToneCurveTriple(Curves);
	    	    return null;
	    }
	    
	    nItems[0] = 1;
	    return Curves;
	}
	
	// We don't support all flavors, only 16bits tables and formula
	private static boolean Type_vcgt_Write(cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems)
	{
		cmsToneCurve[] Curves = (cmsToneCurve[])Ptr;
		int i, j;
		
		if (cmsgamma.cmsGetToneCurveParametricType(Curves[0]) == 5 &&
			cmsgamma.cmsGetToneCurveParametricType(Curves[1]) == 5 &&
			cmsgamma.cmsGetToneCurveParametricType(Curves[2]) == 5)
		{
			if (!cmsplugin._cmsWriteUInt32Number(io, cmsVideoCardGammaFormulaType))
			{
				return false;
			}
			
			// Save parameters
			_cmsVCGTGAMMA v = new _cmsVCGTGAMMA();
			for (i = 0; i < 3; i++)
			{
				v.Gamma = Curves[i].Segments[0].Params[0];
				v.Min   = Curves[i].Segments[0].Params[5];
				v.Max   = pow(Curves[i].Segments[0].Params[1], v.Gamma) + v.Min;
				
				if (!cmsplugin._cmsWrite15Fixed16Number(io, v.Gamma))
				{
					return false;
				}
				if (!cmsplugin._cmsWrite15Fixed16Number(io, v.Min))
				{
					return false;
				}
				if (!cmsplugin._cmsWrite15Fixed16Number(io, v.Max))
				{
					return false;
				}
			}
		}
		else
		{
			// Always store as a table of 256 words
			if (!cmsplugin._cmsWriteUInt32Number(io, cmsVideoCardGammaTableType))
			{
				return false;
			}
			if (!cmsplugin._cmsWriteUInt16Number(io, 3))
			{
				return false;
			}
			if (!cmsplugin._cmsWriteUInt16Number(io, 256))
			{
				return false;
			}
			if (!cmsplugin._cmsWriteUInt16Number(io, 2))
			{
				return false;
			}
			
			for (i = 0; i < 3; i++)
			{
				for (j = 0; j < 256; j++)
				{
					float v = cmsgamma.cmsEvalToneCurveFloat(Curves[i], (float)(j / 255.0));
					short n = lcms2_internal._cmsQuickSaturateWord(v * 65535.0);
					
					if (!cmsplugin._cmsWriteUInt16Number(io, n))
					{
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	private static Object Type_vcgt_Dup(cmsTagTypeHandler self, final Object Ptr, int n)
	{
		cmsToneCurve[] OldCurves = (cmsToneCurve[])Ptr;
		cmsToneCurve[] NewCurves;
		
		NewCurves = new cmsToneCurve[3];
		
		NewCurves[0] = cmsgamma.cmsDupToneCurve(OldCurves[0]);
	    NewCurves[1] = cmsgamma.cmsDupToneCurve(OldCurves[1]);
		NewCurves[2] = cmsgamma.cmsDupToneCurve(OldCurves[2]);
		
	    return NewCurves;
	}
	
	private static void Type_vcgt_Free(cmsTagTypeHandler self, Object Ptr)
	{
		cmsgamma.cmsFreeToneCurveTriple((cmsToneCurve[])Ptr);
	}
	
	// ********************************************************************************
	// Type support main routines
	// ********************************************************************************
	
	// This is the list of built-in types
	private static _cmsTagTypeLinkedList SupportedTagTypes;
	
	private static void setupTypes()
	{
		_cmsTagTypeLinkedList tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigVcgtType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_vcgt_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_vcgt_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_vcgt_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_vcgt_Free(self, Ptr);
			}
		}), null);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigProfileSequenceIdType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_ProfileSequenceId_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_ProfileSequenceId_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_ProfileSequenceId_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_ProfileSequenceId_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsMonacoBrokenCurveType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_Curve_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_Curve_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_Curve_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_Curve_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsCorbisBrokenXYZtype, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_XYZ_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_XYZ_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_XYZ_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_XYZ_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigXYZType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_XYZ_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_XYZ_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_XYZ_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_XYZ_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigViewingConditionsType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_ViewingConditions_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_ViewingConditions_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_ViewingConditions_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_ViewingConditions_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigScreeningType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_Screening_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_Screening_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_Screening_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_Screening_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigMultiProcessElementType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_MPE_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_MPE_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_MPE_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_MPE_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigCrdInfoType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_CrdInfo_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_CrdInfo_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_CrdInfo_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_CrdInfo_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigUcrBgType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_UcrBg_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_UcrBg_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_UcrBg_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_UcrBg_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigLutBtoAType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_LUTB2A_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_LUTB2A_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_LUTB2A_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_LUTB2A_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigLutAtoBType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_LUTA2B_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_LUTA2B_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_LUTA2B_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_LUTA2B_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigDataType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_Data_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_Data_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_Data_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_Data_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigMeasurementType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_Measurement_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_Measurement_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_Measurement_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_Measurement_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigSignatureType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_Signature_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_Signature_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_Signature_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_Signature_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigProfileSequenceDescType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_ProfileSequenceDesc_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_ProfileSequenceDesc_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_ProfileSequenceDesc_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_ProfileSequenceDesc_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigMultiLocalizedUnicodeType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_MLU_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_MLU_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_MLU_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_MLU_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigNamedColor2Type, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_NamedColor_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_NamedColor_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_NamedColor_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_NamedColor_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigColorantTableType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_ColorantTable_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_ColorantTable_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_ColorantTable_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_ColorantTable_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigLut16Type, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_LUT16_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_LUT16_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_LUT16_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_LUT16_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigLut8Type, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_LUT8_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_LUT8_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_LUT8_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_LUT8_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigDateTimeType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_DateTime_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_DateTime_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_DateTime_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_DateTime_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigParametricCurveType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_ParametricCurve_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_ParametricCurve_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_ParametricCurve_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_ParametricCurve_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigCurveType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_Curve_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_Curve_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_Curve_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_Curve_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigTextDescriptionType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_Text_Description_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_Text_Description_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_Text_Description_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_Text_Description_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigTextType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_Text_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_Text_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_Text_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_Text_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigU16Fixed16ArrayType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_U16Fixed16_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_U16Fixed16_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_U16Fixed16_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_U16Fixed16_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigS15Fixed16ArrayType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_S15Fixed16_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_S15Fixed16_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_S15Fixed16_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_S15Fixed16_Free(self, Ptr);
			}
		}), tempTypes);
		tempTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigColorantOrderType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_ColorantOrderType_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_ColorantOrderType_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_ColorantOrderType_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_ColorantOrderType_Free(self, Ptr);
			}
		}), tempTypes);
		SupportedTagTypes = new _cmsTagTypeLinkedList(new cmsTagTypeHandler(lcms2.cmsSigChromaticityType, new cmsTagTypeHandler.tagHandlerReadPtr()
		{
			public Object run(cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, SizeOfTag int)
			{
				return Type_Chromaticity_Read(self, io, nItems, int);
			}
		}, new cmsTagTypeHandler.tagHandlerWritePtr()
		{
			public boolean run(cmsTagTypeHandler self, cmsIOHANDLER io, int Ptr, int nItems)
			{
				return Type_Chromaticity_Write(self, io, Ptr, nItems);
			}
		}, new cmsTagTypeHandler.tagHandlerDupPtr()
		{
			public Object run(cmsTagTypeHandler self, final Object Ptr, int n)
			{
				return Type_Chromaticity_Dup(self, Ptr, n);
			}
		}, new cmsTagTypeHandler.tagHandlerFreePtr()
		{
			public void run(cmsTagTypeHandler self, Object Ptr)
			{
				Type_Chromaticity_Free(self, Ptr);
			}
		}), tempTypes);
	}
	
	private static final int DEFAULT_TAG_TYPE_COUNT = 30;
	
	// Both kind of plug-ins share same structure
	public static boolean _cmsRegisterTagTypePlugin(cmsPluginBase Data)
	{
	    return RegisterTypesPlugin(Data, SupportedTagTypes, DEFAULT_TAG_TYPE_COUNT);
	}
	
	public static boolean _cmsRegisterMultiProcessElementPlugin(cmsPluginBase Data)
	{
	    return RegisterTypesPlugin(Data, SupportedMPEtypes, DEFAULT_MPE_TYPE_COUNT);
	}
	
	// Wrapper for tag types
	public static cmsTagTypeHandler _cmsGetTagTypeHandler(int sig)
	{
	    return GetHandler(sig, SupportedTagTypes);
	}
	
	// ********************************************************************************
	// Tag support main routines
	// ********************************************************************************
	
	private static class _cmsTagLinkedList
	{
		public int Signature;
        public cmsTagDescriptor Descriptor;
        public _cmsTagLinkedList Next;
        
        public _cmsTagLinkedList(int Signature, cmsTagDescriptor Descriptor, _cmsTagLinkedList Next)
        {
        	this.Signature = Signature;
        	this.Descriptor = Descriptor;
        	this.Next = Next;
        }
	}

	// This is the list of built-in tags
	private static _cmsTagLinkedList SupportedTags;
	
	private static void setupTags()
	{
		_cmsTagLinkedList tempTag = new _cmsTagLinkedList(lcms2.cmsSigProfileSequenceIdTag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigProfileSequenceIdType}, null), null);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigVcgtTag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigVcgtType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigScreeningTag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigScreeningType}, null), tempTag);
		
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigViewingConditionsTag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigViewingConditionsType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigScreeningDescTag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigTextDescriptionType}, null), tempTag);
		
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigBToD3Tag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigMultiProcessElementType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigBToD2Tag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigMultiProcessElementType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigBToD1Tag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigMultiProcessElementType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigBToD0Tag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigMultiProcessElementType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigDToB3Tag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigMultiProcessElementType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigDToB2Tag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigMultiProcessElementType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigDToB1Tag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigMultiProcessElementType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigDToB0Tag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigMultiProcessElementType}, null), tempTag);
		
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigCrdInfoTag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigCrdInfoType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigUcrBgTag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigUcrBgType}, null), tempTag);
		
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigViewingCondDescTag, new cmsTagDescriptor(1, 3, new int{lcms2.cmsSigTextDescriptionType, lcms2.cmsSigMultiLocalizedUnicodeType, lcms2.cmsSigTextType}, new cmsTagDescriptor.tagDesDecideType()
		{
			public int run(double ICCVersion, final Object Data)
			{
				return DecideTextDescType(ICCVersion, Data);
			}
		}), tempTag);
		
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigPs2RenderingIntentTag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigDataType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigPs2CSATag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigDataType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigPs2CRD3Tag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigDataType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigPs2CRD2Tag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigDataType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigPs2CRD1Tag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigDataType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigPs2CRD0Tag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigDataType}, null), tempTag);
		
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigMeasurementTag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigMeasurementType}, null), tempTag);
		
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigSaturationRenderingIntentGamutTag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigSignatureType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigPerceptualRenderingIntentGamutTag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigSignatureType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigColorimetricIntentImageStateTag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigSignatureType}, null), tempTag);
		
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigTechnologyTag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigSignatureType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigProfileSequenceDescTag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigProfileSequenceDescType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigProfileDescriptionTag, new cmsTagDescriptor(1, 3, new int{lcms2.cmsSigTextDescriptionType, lcms2.cmsSigMultiLocalizedUnicodeType, lcms2.cmsSigTextType}, new cmsTagDescriptor.tagDesDecideType()
		{
			public int run(double ICCVersion, final Object Data)
			{
				return DecideTextDescType(ICCVersion, Data);
			}
		}), tempTag);
		
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigPreview2Tag, new cmsTagDescriptor(1, 3, new int{lcms2.cmsSigLut16Type, lcms2.cmsSigLutBtoAType, lcms2.cmsSigLut8Type}, new cmsTagDescriptor.tagDesDecideType()
		{
			public int run(double ICCVersion, final Object Data)
			{
				return DecideLUTtypeB2A(ICCVersion, Data);
			}
		}), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigPreview1Tag, new cmsTagDescriptor(1, 3, new int{lcms2.cmsSigLut16Type, lcms2.cmsSigLutBtoAType, lcms2.cmsSigLut8Type}, new cmsTagDescriptor.tagDesDecideType()
		{
			public int run(double ICCVersion, final Object Data)
			{
				return DecideLUTtypeB2A(ICCVersion, Data);
			}
		}), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigPreview0Tag, new cmsTagDescriptor(1, 3, new int{lcms2.cmsSigLut16Type, lcms2.cmsSigLutBtoAType, lcms2.cmsSigLut8Type}, new cmsTagDescriptor.tagDesDecideType()
		{
			public int run(double ICCVersion, final Object Data)
			{
				return DecideLUTtypeB2A(ICCVersion, Data);
			}
		}), tempTag);
		
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigNamedColor2Tag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigNamedColor2Type}, null), tempTag);
		
		// Allow corbis  and its broken XYZ type
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigMediaWhitePointTag, new cmsTagDescriptor(1, 2, new int{lcms2.cmsSigXYZType, lcms2.cmsCorbisBrokenXYZtype}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigMediaBlackPointTag, new cmsTagDescriptor(1, 2, new int{lcms2.cmsSigXYZType, lcms2.cmsCorbisBrokenXYZtype}, null), tempTag);
		
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigLuminanceTag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigXYZType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigGrayTRCTag, new cmsTagDescriptor(1, 2, new int{lcms2.cmsSigCurveType, lcms2.cmsSigParametricCurveType}, new cmsTagDescriptor.tagDesDecideType()
		{
			public int run(double ICCVersion, final Object Data)
			{
				return DecideCurveType(ICCVersion, Data);
			}
		}), tempTag);
		
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigGamutTag, new cmsTagDescriptor(1, 3, new int{lcms2.cmsSigLut16Type, lcms2.cmsSigLutBtoAType, lcms2.cmsSigLut8Type}, new cmsTagDescriptor.tagDesDecideType()
		{
			public int run(double ICCVersion, final Object Data)
			{
				return DecideLUTtypeB2A(ICCVersion, Data);
			}
		}), tempTag);
		
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigDeviceModelDescTag, new cmsTagDescriptor(1, 3, new int{lcms2.cmsSigTextDescriptionType, lcms2.cmsSigMultiLocalizedUnicodeType, lcms2.cmsSigTextType}, new cmsTagDescriptor.tagDesDecideType()
		{
			public int run(double ICCVersion, final Object Data)
			{
				return DecideTextDescType(ICCVersion, Data);
			}
		}), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigDeviceMfgDescTag, new cmsTagDescriptor(1, 3, new int{lcms2.cmsSigTextDescriptionType, lcms2.cmsSigMultiLocalizedUnicodeType, lcms2.cmsSigTextType}, new cmsTagDescriptor.tagDesDecideType()
		{
			public int run(double ICCVersion, final Object Data)
			{
				return DecideTextDescType(ICCVersion, Data);
			}
		}), tempTag);
		
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigDateTimeTag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigDateTimeType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigCopyrightTag, new cmsTagDescriptor(1, 3, new int{lcms2.cmsSigTextType, lcms2.cmsSigMultiLocalizedUnicodeType, lcms2.cmsSigTextDescriptionType}, new cmsTagDescriptor.tagDesDecideType()
		{
			public int run(double ICCVersion, final Object Data)
			{
				return DecideTextType(ICCVersion, Data);
			}
		}), tempTag);
		
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigColorantTableOutTag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigColorantTableType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigColorantTableTag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigColorantTableType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigColorantOrderTag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigColorantOrderType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigChromaticityTag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigChromaticityType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigChromaticAdaptationTag, new cmsTagDescriptor(9, 1, new int{lcms2.cmsSigS15Fixed16ArrayType}, null), tempTag);
		
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigCharTargetTag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigTextType}, null), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigCalibrationDateTimeTag, new cmsTagDescriptor(1, 1, new int{lcms2.cmsSigDateTimeType}, null), tempTag);
		
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigBlueTRCTag, new cmsTagDescriptor(1, 3, new int{lcms2.cmsSigCurveType, lcms2.cmsSigParametricCurveType, lcms2.cmsMonacoBrokenCurveType}, new cmsTagDescriptor.tagDesDecideType()
		{
			public int run(double ICCVersion, final Object Data)
			{
				return DecideCurveType(ICCVersion, Data);
			}
		}), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigGreenTRCTag, new cmsTagDescriptor(1, 3, new int{lcms2.cmsSigCurveType, lcms2.cmsSigParametricCurveType, lcms2.cmsMonacoBrokenCurveType}, new cmsTagDescriptor.tagDesDecideType()
		{
			public int run(double ICCVersion, final Object Data)
			{
				return DecideCurveType(ICCVersion, Data);
			}
		}), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigRedTRCTag, new cmsTagDescriptor(1, 3, new int{lcms2.cmsSigCurveType, lcms2.cmsSigParametricCurveType, lcms2.cmsMonacoBrokenCurveType}, new cmsTagDescriptor.tagDesDecideType()
		{
			public int run(double ICCVersion, final Object Data)
			{
				return DecideCurveType(ICCVersion, Data);
			}
		}), tempTag);
		
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigBlueColorantTag, new cmsTagDescriptor(1, 2, new int{lcms2.cmsSigXYZType, lcms2.cmsCorbisBrokenXYZtype}, new cmsTagDescriptor.tagDesDecideType()
		{
			public int run(double ICCVersion, final Object Data)
			{
				return DecideXYZtype(ICCVersion, Data);
			}
		}), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigGreenColorantTag, new cmsTagDescriptor(1, 2, new int{lcms2.cmsSigXYZType, lcms2.cmsCorbisBrokenXYZtype}, new cmsTagDescriptor.tagDesDecideType()
		{
			public int run(double ICCVersion, final Object Data)
			{
				return DecideXYZtype(ICCVersion, Data);
			}
		}), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigRedColorantTag, new cmsTagDescriptor(1, 2, new int{lcms2.cmsSigXYZType, lcms2.cmsCorbisBrokenXYZtype}, new cmsTagDescriptor.tagDesDecideType()
		{
			public int run(double ICCVersion, final Object Data)
			{
				return DecideXYZtype(ICCVersion, Data);
			}
		}), tempTag);
		
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigBToA2Tag, new cmsTagDescriptor(1, 3, new int{lcms2.cmsSigLut16Type, lcms2.cmsSigLutBtoAType, lcms2.cmsSigLut8Type}, new cmsTagDescriptor.tagDesDecideType()
		{
			public int run(double ICCVersion, final Object Data)
			{
				return DecideLUTtypeB2A(ICCVersion, Data);
			}
		}), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigBToA1Tag, new cmsTagDescriptor(1, 3, new int{lcms2.cmsSigLut16Type, lcms2.cmsSigLutBtoAType, lcms2.cmsSigLut8Type}, new cmsTagDescriptor.tagDesDecideType()
		{
			public int run(double ICCVersion, final Object Data)
			{
				return DecideLUTtypeB2A(ICCVersion, Data);
			}
		}), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigBToA0Tag, new cmsTagDescriptor(1, 3, new int{lcms2.cmsSigLut16Type, lcms2.cmsSigLutBtoAType, lcms2.cmsSigLut8Type}, new cmsTagDescriptor.tagDesDecideType()
		{
			public int run(double ICCVersion, final Object Data)
			{
				return DecideLUTtypeB2A(ICCVersion, Data);
			}
		}), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigAToB2Tag, new cmsTagDescriptor(1, 3, new int{lcms2.cmsSigLut16Type, lcms2.cmsSigLutAtoBType, lcms2.cmsSigLut8Type}, new cmsTagDescriptor.tagDesDecideType()
		{
			public int run(double ICCVersion, final Object Data)
			{
				return DecideLUTtypeA2B(ICCVersion, Data);
			}
		}), tempTag);
		tempTag = new _cmsTagLinkedList(lcms2.cmsSigAToB1Tag, new cmsTagDescriptor(1, 3, new int{lcms2.cmsSigLut16Type, lcms2.cmsSigLutAtoBType, lcms2.cmsSigLut8Type}, new cmsTagDescriptor.tagDesDecideType()
		{
			public int run(double ICCVersion, final Object Data)
			{
				return DecideLUTtypeA2B(ICCVersion, Data);
			}
		}), tempTag);
		SupportedTags = new _cmsTagLinkedList(lcms2.cmsSigAToB0Tag, new cmsTagDescriptor(1, 3, new int{lcms2.cmsSigLut16Type, lcms2.cmsSigLutAtoBType, lcms2.cmsSigLut8Type}, new cmsTagDescriptor.tagDesDecideType()
		{
			public int run(double ICCVersion, final Object Data)
			{
				return DecideLUTtypeA2B(ICCVersion, Data);
			}
		}), tempTag);
	}
	
	/*
    Not supported                 Why
    =======================       =========================================                
    cmsSigOutputResponseTag   ==> WARNING, POSSIBLE PATENT ON THIS SUBJECT!                
    cmsSigNamedColorTag       ==> Deprecated                                 
    cmsSigDataTag             ==> Ancient, unused             
    cmsSigDeviceSettingsTag   ==> Deprecated, useless     
	 */
	
	private static final int DEFAULT_TAG_COUNT = 61;
	
	private static void findLinkedListAtIndex(_cmsTagLinkedList list, int index)
	{
		for(int i = index; i >= 0 && list != null; i--)
		{
			list = list.Next;
		}
		return null;
	}
	
	public static boolean _cmsRegisterTagPlugin(cmsPluginBase Data)
	{
	    cmsPluginTag Plugin = (cmsPluginTag)Data;
	    _cmsTagLinkedList pt, Anterior;
	    
	    if (Data == null)
	    {
	    	findLinkedListAtIndex(SupportedTags, DEFAULT_TAG_COUNT-1).Next = null;
	        return true;
	    }
	    
	    pt = Anterior = SupportedTags; 
	    while (pt != null)
	    {
	        if (Plugin.Signature == pt.Signature)
	        {
	            pt.Descriptor = Plugin.Descriptor; // Replace old behaviour
	            return true;
	        }   
	        
	        Anterior = pt;          
	        pt = pt.Next;
	    }
	    
	    pt = new _cmsTagLinkedList();
	    
	    pt.Signature  = Plugin.Signature;
	    pt.Descriptor = Plugin.Descriptor;  
	    pt.Next       = null;
	    
	    if (Anterior != null)
	    {
	    	Anterior.Next = pt;
	    }
	    
	    return true;
	}
	
	// Return a descriptor for a given tag or NULL
	public static cmsTagDescriptor _cmsGetTagDescriptor(int sig)
	{
	    _cmsTagLinkedList pt;
	    
	    for (pt = SupportedTags; pt != null; pt = pt.Next)
	    {
	    	if (sig == pt.Signature)
	    	{
	    		return pt.Descriptor;
	    	}
	    }
	    
	    return null;
	}
}
