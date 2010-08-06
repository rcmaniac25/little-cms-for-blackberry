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
	    pt = (_cmsTagTypeLinkedList)cmsplugin._cmsPluginMalloc(/*sizeof(_cmsTagTypeLinkedList)*/_cmsTagTypeLinkedList.SIZE).getDeserializedType(_cmsTagTypeLinkedList.class);
	    if (pt == null)
	    {
	    	return false;
	    }
	    
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
	
	//TODO #121
}
