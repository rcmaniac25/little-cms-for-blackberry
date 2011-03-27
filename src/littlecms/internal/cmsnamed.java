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

import littlecms.internal.helper.BitConverter;
import littlecms.internal.helper.Utility;
import littlecms.internal.helper.VirtualPointer;
import littlecms.internal.lcms2.cmsContext;
import littlecms.internal.lcms2.cmsHTRANSFORM;
import littlecms.internal.lcms2.cmsMLU;
import littlecms.internal.lcms2.cmsNAMEDCOLORLIST;
import littlecms.internal.lcms2.cmsPSEQDESC;
import littlecms.internal.lcms2.cmsProfileID;
import littlecms.internal.lcms2.cmsSEQ;
import littlecms.internal.lcms2.cmsStage;
import littlecms.internal.lcms2_internal._cmsMLUentry;
import littlecms.internal.lcms2_internal._cmsNAMEDCOLOR;
import littlecms.internal.lcms2_internal._cmsTRANSFORM;
import littlecms.internal.lcms2_plugin._cmsStageDupElemFn;
import littlecms.internal.lcms2_plugin._cmsStageEvalFn;
import littlecms.internal.lcms2_plugin._cmsStageFreeElemFn;

/** Multilocalized unicode objects. That is an attempt to encapsulate i18n.*/
//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmsnamed
{
	// Allocates an empty multi localizad unicode object
	public static cmsMLU cmsMLUalloc(cmsContext ContextID, int nItems)
	{
		cmsMLU mlu;
		
		// nItems should be positive if given
		if (nItems <= 0)
		{
			nItems = 2;
		}
		
		// Create the container
		mlu = new cmsMLU();
		
		mlu.ContextID = ContextID;
		
		// Create entry array
		mlu.Entries = new _cmsMLUentry[nItems];
		
		// Ok, keep indexes up to date
		mlu.AllocatedEntries    = nItems;
		mlu.UsedEntries         = 0;
		
		return mlu;
	}
	
	// Grows a mempool table for a MLU. Each time this function is called, mempool size is multiplied times two. 
	private static boolean GrowMLUpool(cmsMLU mlu)
	{
		int size;
		VirtualPointer NewPtr;
		
		// Sanity check
		if (mlu == null)
		{
			return false;
		}
		
		if (mlu.PoolSize == 0)
		{
			size = 256;
		}
		else
		{
			size = mlu.PoolSize * 2;
		}
		
		// Check for overflow
		if (size < mlu.PoolSize)
		{
			return false;
		}
		
		// Reallocate the pool
		NewPtr = cmserr._cmsRealloc(mlu.ContextID, mlu.MemPool, size);
		if (NewPtr == null)
		{
			return false;
		}
		
		mlu.MemPool  = NewPtr;
		mlu.PoolSize = size;
		
		return true;
	}
	
	// Grows a entry table for a MLU. Each time this function is called, table size is multiplied times two. 
	private static boolean GrowMLUtable(cmsMLU mlu)
	{
	    int AllocatedEntries;
	    _cmsMLUentry[] NewPtr;
	    
		// Sanity check
		if (mlu == null)
		{
			return false;
		}
		
	    AllocatedEntries = mlu.AllocatedEntries * 2;
	    
		// Check for overflow
		if (AllocatedEntries < mlu.AllocatedEntries)
		{
			return false;
		}
		
		// Reallocate the memory
		NewPtr = new _cmsMLUentry[AllocatedEntries];
		System.arraycopy(mlu.Entries, 0, NewPtr, 0, mlu.AllocatedEntries);
	    
	    mlu.Entries          = NewPtr;
	    mlu.AllocatedEntries = AllocatedEntries;
	    
	    return true;
	}
	
	// Search for a specific entry in the structure. Language and Country are used. 
	private static int SearchMLUEntry(cmsMLU mlu, short LanguageCode, short CountryCode)
	{
	    int i;
		
		// Sanity check
		if (mlu == null)
		{
			return -1;
		}
		
		// Iterate whole table
	    for (i=0; i < mlu.UsedEntries; i++)
	    {
	        if (mlu.Entries[i].Country  == CountryCode && mlu.Entries[i].Language == LanguageCode)
	        {
	        	return i;
	        }
	    }
	    
		// Not found
	    return -1;
	}
	
	// Add a block of characters to the intended MLU. Language and country are specified. 
	// Only one entry for Language/country pair is allowed.
	private static boolean AddMLUBlock(cmsMLU mlu, int size, final char[] Block, short LanguageCode, short CountryCode)
	{
	    int Offset;
	    VirtualPointer Ptr;
	    
		// Sanity check
		if (mlu == null)
		{
			return false;
		}
		
	    // Is there any room available?
	    if (mlu.UsedEntries >= mlu.AllocatedEntries)
	    {
	        if (!GrowMLUtable(mlu))
	        {
	        	return false;
	        }
	    }
	    
	    // Only one ASCII string
	    if (SearchMLUEntry(mlu, LanguageCode, CountryCode) >= 0)
	    {
	    	return false; // Only one  is allowed!
	    }
	    
	    // Check for size
		while ((mlu.PoolSize - mlu.PoolUsed) < size)
		{
			if (!GrowMLUpool(mlu))
			{
				return false;
			}
		}
		
	    Offset = mlu.PoolUsed;
	    
		Ptr = new VirtualPointer(mlu.MemPool);
		Ptr.movePosition(Offset);
		
		// Set the entry
		Ptr.getProcessor().write(Block, size);
	    mlu.PoolUsed += size;
	    
	    mlu.Entries[mlu.UsedEntries] = new _cmsMLUentry();
	    mlu.Entries[mlu.UsedEntries].StrW     = Offset;
	    mlu.Entries[mlu.UsedEntries].Len      = size;
	    mlu.Entries[mlu.UsedEntries].Country  = CountryCode;
	    mlu.Entries[mlu.UsedEntries].Language = LanguageCode;
	    mlu.UsedEntries++;
	    
	    return true;
	}
	
	private static short codeAsShort(final String code)
	{
		byte[] values = new byte[3];
		int len = code.length();
		for(int i = 0; i < 3 && i < len; i++)
		{
			values[i] = (byte)code.charAt(i);
		}
		//Reverse first two bytes
		byte b = values[0];
		values[0] = values[1];
		values[1] = b;
		//Just make sure that the last byte is a zero. This is not really needed but is for safety
		if(values[2] != 0)
		{
			throw new OutOfMemoryError();
		}
		return BitConverter.toInt16(values, 0);
	}
	
	// Add an ASCII entry. 
	public static boolean cmsMLUsetASCII(cmsMLU mlu, final String LanguageCode, final String CountryCode, final String ASCIIString)
	{
	    int i, len = Utility.strlen(ASCIIString)+1;
	    char[] WStr;
	    boolean rc;
	    short Lang  = cmsplugin._cmsAdjustEndianess16(codeAsShort(LanguageCode));
	    short Cntry = cmsplugin._cmsAdjustEndianess16(codeAsShort(CountryCode));
	    
	    if (mlu == null)
	    {
	    	return false;
	    }
	    
	    WStr = new char[len];

	    for (i=0; i < len; i++)
	    {
	    	WStr[i] = (char)((byte)ASCIIString.charAt(i)); //Since it is ASCII, make sure it is ASCII
	    }
	    
	    rc = AddMLUBlock(mlu, len * /*sizeof(wchar_t)*/2, WStr, Lang, Cntry);
	    
	    return rc;
	}
	
	// We don't need any wcs support library -- PORTER NOTE: Replaced with Utility
	/*
	private static int mywcslen(final String s)
	{
	    int p;

		p = 0;
	    while (s.charAt(p) != '\0')
	    {
	    	p++;
	    }
	    
	    return p;
	}
	*/
	
	// Add a wide entry
	public static boolean cmsMLUsetWide(cmsMLU mlu, final String LanguageCode, final String CountryCode, final String WideString)
	{
		short Lang  = cmsplugin._cmsAdjustEndianess16(codeAsShort(LanguageCode));
	    short Cntry = cmsplugin._cmsAdjustEndianess16(codeAsShort(CountryCode));
	    int len;
	    
	    if (mlu == null)
	    {
	    	return false;
	    }
		if (WideString == null)
	    {
	    	return false;
	    }
		
	    len = (int)(/*mywcslen(WideString)*/Utility.strlen(WideString) + 1) * /*sizeof(wchar_t)*/2;
	    return AddMLUBlock(mlu, len, WideString.toCharArray(), Lang, Cntry);
	}
	
	// Duplicating a MLU is as easy as copying all members
	public static cmsMLU cmsMLUdup(final cmsMLU mlu)
	{
		cmsMLU NewMlu = null;

		// Duplicating a NULL obtains a NULL
		if (mlu == null)
		{
			return null;
		}
		
		NewMlu = cmsMLUalloc(mlu.ContextID, mlu.UsedEntries);
		if (NewMlu == null)
		{
			return null;
		}
		
		// Should never happen
		if (NewMlu.AllocatedEntries < mlu.UsedEntries)
		{
			if (NewMlu != null)
			{
				cmsMLUfree(NewMlu);
			}
			return null;
		}
		
		// Sanitize...
		if (NewMlu.Entries == null || mlu.Entries == null)
		{
			if (NewMlu != null)
			{
				cmsMLUfree(NewMlu);
			}
			return null;
		}
		
		//Copy entities manually so that they aren't affected by changes to the original entities
		for(int i = 0; i < mlu.UsedEntries; i++)
		{
			NewMlu.Entries[i] = new _cmsMLUentry();
			NewMlu.Entries[i].StrW = mlu.Entries[i].StrW;
			NewMlu.Entries[i].Len = mlu.Entries[i].Len;
			NewMlu.Entries[i].Country = mlu.Entries[i].Country;
			NewMlu.Entries[i].Language = mlu.Entries[i].Language;
		}
		//System.arraycopy(mlu.Entries, 0, NewMlu.Entries, 0, mlu.UsedEntries);
		NewMlu.UsedEntries = mlu.UsedEntries;
		
		// The MLU may be empty
		if (mlu.PoolUsed == 0)
		{
			NewMlu.MemPool = null;
		}
		else
		{
			// It is not empty
			NewMlu.MemPool = cmserr._cmsMalloc(mlu.ContextID, mlu.PoolUsed);
			if (NewMlu.MemPool == null)
			{
				if (NewMlu != null)
				{
					cmsMLUfree(NewMlu);
				}
				return null;
			}
		}
		
		NewMlu.PoolSize = mlu.PoolUsed;
		
		if (NewMlu.MemPool == null || mlu.MemPool == null)
		{
			if (NewMlu != null)
			{
				cmsMLUfree(NewMlu);
			}
			return null;
		}
		
		NewMlu.MemPool.writeRaw(mlu.MemPool, 0, mlu.PoolUsed);
		NewMlu.PoolUsed = mlu.PoolUsed;
		
		return NewMlu;
	}
	
	// Free any used memory
	public static void cmsMLUfree(cmsMLU mlu)
	{
		if (mlu != null)
		{
			if (mlu.Entries != null)
			{
				mlu.Entries = null;
				//cmserr._cmsFree(mlu.ContextID, mlu.Entries);
			}
			if (mlu.MemPool != null)
			{
				cmserr._cmsFree(mlu.ContextID, mlu.MemPool);
			}
			
			//cmserr._cmsFree(mlu.ContextID, mlu);
		}
	}
	
	// The algorithm first searches for an exact match of country and language, if not found it uses 
	// the Language. If none is found, first entry is used instead.
	private static String _cmsMLUgetWide(final cmsMLU mlu, int[] len, short LanguageCode, short CountryCode, short[] UsedLanguageCode, short[] UsedCountryCode)
	{
	    int i;
	    int Best = -1;
		_cmsMLUentry v;
		
	    if (mlu == null)
	    {
	    	return null;
	    }
	    
	    if (mlu.AllocatedEntries <= 0)
	    {
	    	return null;
	    }
	    
	    int pos = mlu.MemPool.getPosition();
	    VirtualPointer.TypeProcessor proc = mlu.MemPool.getProcessor();
	    for (i=0; i < mlu.UsedEntries; i++)
	    {
	        v = mlu.Entries[i];
	        
	        if (v.Language == LanguageCode)
	        {
	            if (Best == -1)
	            {
	            	Best = i;
	            }
	            
	            if (v.Country == CountryCode)
	            {
					if (UsedLanguageCode != null)
					{
						UsedLanguageCode[0] = v.Language;
					}
					if (UsedCountryCode != null)
					{
						UsedCountryCode[0] = v.Country;
					}
					
	                if (len != null)
	                {
	                	len[0] = v.Len;
	                }
	                
	                mlu.MemPool.movePosition(v.StrW);
	                String tempResult = proc.readString(false, true); // Found exact match
	                mlu.MemPool.setPosition(pos);
	                return tempResult;
	            }
	        }
	    }
	    
	    // No string found. Return First one
	    if (Best == -1)
	    {
	    	Best = 0;
	    }
	    
	    v = mlu.Entries[Best];
	    
	    if (UsedLanguageCode != null)
		{
			UsedLanguageCode[0] = v.Language;
		}
		if (UsedCountryCode != null)
		{
			UsedCountryCode[0] = v.Country;
		}
		
        if (len != null)
        {
        	len[0] = v.Len;
        }
        
        mlu.MemPool.movePosition(v.StrW);
        String tempResult = proc.readString(false, true); // Found exact match
        mlu.MemPool.setPosition(pos);
        return tempResult;
	}
	
	// Obtain an ASCII representation of the wide string. Setting buffer to NULL returns the len
	public static int cmsMLUgetASCII(cmsMLU mlu, final String LanguageCode, final String CountryCode, StringBuffer Buffer, int BufferSize)
	{
	    final String Wide;
	    int StrLen[] = new int[1];
	    int ASCIIlen, i;
	    
	    short Lang  = cmsplugin._cmsAdjustEndianess16(codeAsShort(LanguageCode));
	    short Cntry = cmsplugin._cmsAdjustEndianess16(codeAsShort(CountryCode));
	    
		// Sanitize
	    if (mlu == null)
	    {
	    	return 0;
	    }
	    
	    // Get WideChar
	    Wide = _cmsMLUgetWide(mlu, StrLen, Lang, Cntry, null, null);
	    if (Wide == null)
	    {
	    	return 0;
	    }
	    
	    ASCIIlen = StrLen[0] / /*sizeof(wchar_t)*/2;
	    
	    // Maybe we want only to know the len?
	    if (Buffer == null)
	    {
	    	return ASCIIlen + 1; // Note the zero at the end
	    }
	    
	    // No buffer size means no data
	    if (BufferSize <= 0)
	    {
	    	return 0;
	    }
	    
	    // Some clipping may be required
	    if (BufferSize < ASCIIlen + 1)
	    {
	    	ASCIIlen = BufferSize - 1;
	    }
	    
	    // Process each character
	    int appendPos = Buffer.length();
	    char c;
	    for (i=0; i < ASCIIlen; i++)
	    {
	    	if (i >= (ASCIIlen - 1) || Wide.charAt(i) == '\0')
	    	{
	    		c = '\0';
	    	}
	    	else
	    	{
	    		c = (char)((byte)Wide.charAt(i));
	    	}
	    	if(i >= appendPos)
        	{
        		Buffer.append(c);
        	}
        	else
        	{
        		Buffer.setCharAt(i, c);
        	}
	    }
	    
		// We put a termination "\0"
	    if(ASCIIlen >= appendPos)
	    {
	    	Buffer.append('\0');
	    }
	    else
	    {
	    	Buffer.setCharAt(ASCIIlen, '\0');
	    }
	    return ASCIIlen + 1;
	}
	
	// Obtain a wide representation of the MLU, on depending on current locale settings 
	public static int cmsMLUgetWide(cmsMLU mlu, final String LanguageCode, final String CountryCode, StringBuffer Buffer, int BufferSize)
	{
	    final String Wide;
	    int[] StrLen = new int[1];
	    
	    short Lang  = cmsplugin._cmsAdjustEndianess16(codeAsShort(LanguageCode));
	    short Cntry = cmsplugin._cmsAdjustEndianess16(codeAsShort(CountryCode));
	    
		// Sanitize
	    if (mlu == null)
	    {
	    	return 0;
	    }
	    
	    Wide = _cmsMLUgetWide(mlu, StrLen, Lang, Cntry, null, null);
	    if (Wide == null)
	    {
	    	return 0;
	    }
	    
	    // Maybe we want only to know the len?
	    if (Buffer == null)
	    {
	    	return StrLen[0] + /*sizeof(wchar_t)*/2;
	    }
	    
	    // No buffer size means no data
	    if (BufferSize <= 0)
	    {
	    	return 0;
	    }
	    
	    // Some clipping may be required
	    if (BufferSize < StrLen[0] + /*sizeof(wchar_t)*/2)
	    {
	    	StrLen[0] = BufferSize - /*sizeof(wchar_t)*/2;
	    }
	    
	    Utility.strncpy(Buffer, Wide, StrLen[0] / /*sizeof(wchar_t)*/2);
	    if(StrLen[0] / /*sizeof(wchar_t)*/2 <= Buffer.length())
	    {
	    	Buffer.append('\0');
	    }
	    else
	    {
	    	Buffer.setCharAt(StrLen[0] / /*sizeof(wchar_t)*/2, '\0');
	    }
		
	    return StrLen[0] + /*sizeof(wchar_t)*/2;
	}
	
	private static void shortToCode(final short code, StringBuffer buf)
	{
		byte[] values = BitConverter.getBytes(code);
		int appendPos = buf.length();
		for(int i = 0; i < 2; i++)
		{
			if(i < appendPos)
			{
				buf.setCharAt(i, (char)values[i]);
			}
			else
			{
				buf.append((char)values[i]);
			}
		}
		if(buf.length() > 2)
		{
			buf.setCharAt(2, '\0');
		}
	}
	
	// Get also the language and country
	public static boolean cmsMLUgetTranslation(cmsMLU mlu, final String LanguageCode, final String CountryCode, StringBuffer ObtainedLanguage, StringBuffer ObtainedCountry)
	{
		final String Wide;
		
		short Lang  = cmsplugin._cmsAdjustEndianess16(codeAsShort(LanguageCode));
	    short Cntry = cmsplugin._cmsAdjustEndianess16(codeAsShort(CountryCode));
	    short[] ObtLang = new short[1], ObtCode = new short[1];
	    
		// Sanitize
	    if (mlu == null)
	    {
	    	return false;
	    }
	    
	    Wide = _cmsMLUgetWide(mlu, null, Lang, Cntry, ObtLang, ObtCode);
		if (Wide == null)
	    {
	    	return false;
	    }
	    
		// Get used language and code
		shortToCode(cmsplugin._cmsAdjustEndianess16(ObtLang[0]), ObtainedLanguage);
		shortToCode(cmsplugin._cmsAdjustEndianess16(ObtCode[0]), ObtainedCountry);
		return true;
	}
	
	// Named color lists --------------------------------------------------------------------------------------------
	
	// Grow the list to keep at least NumElements
	private static boolean GrowNamedColorList(cmsNAMEDCOLORLIST v)
	{           
	    int size;
	    _cmsNAMEDCOLOR[] NewPtr;
	    
	    if (v == null)
	    {
	    	return false;
	    }
	    
	    if (v.Allocated == 0)
	    {
	    	size = 64; // Initial guess
	    }
	    else
	    {
	    	size = v.Allocated * 2;
	    }
	    
	    NewPtr = new _cmsNAMEDCOLOR[size];
	    if(v.List != null)
	    {
	    	System.arraycopy(v.List, 0, NewPtr, 0, v.Allocated);
	    }
	    
	    v.List      = NewPtr;
	    v.Allocated = size;
	    return true;
	}
	
	// Allocate a list for n elements
	public static cmsNAMEDCOLORLIST cmsAllocNamedColorList(cmsContext ContextID, int n, int ColorantCount, final String Prefix, final String Suffix)
	{
	    cmsNAMEDCOLORLIST v = new cmsNAMEDCOLORLIST();
	    
	    /*
	    if (v == null)
	    {
	    	return null;
	    }
	    */
	    
	    v.List		= null;
	    v.nColors	= 0;
	    v.ContextID	= ContextID;
	    
	    while (v.Allocated < n)
	    {
	    	GrowNamedColorList(v);
	    }
	    
	    Utility.strncpy(v.Prefix, Prefix, Utility.strlen(Prefix));
	    Utility.strncpy(v.Suffix, Suffix, Utility.strlen(Suffix));
	    v.ColorantCount = ColorantCount;
	    
	    return v;
	}
	
	// Free a list
	public static void cmsFreeNamedColorList(cmsNAMEDCOLORLIST v)
	{               
	    if (v.List != null)
	    {
	    	v.List = null;
	    	//_cmsFree(v.ContextID, v.List);
	    }
	    if (v != null)
	    {
	    	v.Prefix = null;
	    	v.Suffix = null;
	    	//_cmsFree(v.ContextID, v);
	    }
	}
	
	public static cmsNAMEDCOLORLIST cmsDupNamedColorList(final cmsNAMEDCOLORLIST v)
	{
	    cmsNAMEDCOLORLIST NewNC;
	    
	    if (v == null)
	    {
	    	return null;
	    }
	    
	    NewNC = cmsAllocNamedColorList(v.ContextID, v.nColors, v.ColorantCount, v.Prefix.toString(), v.Suffix.toString());
	    if (NewNC == null)
	    {
	    	return null;
	    }
	    
	    // For really large tables we need this
	    while (NewNC.Allocated < v.Allocated)
	    {
	    	GrowNamedColorList(NewNC);
	    }
	    
	    NewNC.Prefix.append(v.Prefix.toString());
	    NewNC.Suffix.append(v.Suffix.toString());
	    NewNC.ColorantCount = v.ColorantCount;
	    System.arraycopy(v.List, 0, NewNC.List, 0, v.nColors);
	    NewNC.nColors = v.nColors;
	    return NewNC;
	}
	
	// Append a color to a list. List pointer may change if reallocated
	public static boolean cmsAppendNamedColor(cmsNAMEDCOLORLIST NamedColorList, final String Name, short[] PCS, short[] Colorant)
	{    
	    int i;
	    
	    if (NamedColorList == null)
	    {
	    	return false;
	    }
	    
	    if (NamedColorList.nColors + 1 > NamedColorList.Allocated)
	    {
	        if (!GrowNamedColorList(NamedColorList))
	        {
	        	return false;
	        }
	    }
	    
	    NamedColorList.List[NamedColorList.nColors] = new _cmsNAMEDCOLOR();
	    for (i=0; i < NamedColorList.ColorantCount; i++)
	    {
	    	NamedColorList.List[NamedColorList.nColors].DeviceColorant[i] = Colorant == null? 0 : Colorant[i];
	    }
	    
	    for (i=0; i < 3; i++)
	    {
	    	NamedColorList.List[NamedColorList.nColors].PCS[i] = PCS == null ? 0 : PCS[i];
	    }
	    
	    StringBuffer buf = NamedColorList.List[NamedColorList.nColors].Name;
	    if (Name != null)
	    {
	    	Utility.strncpy(buf, Name, Utility.strlen(buf));
	    }
	    else
	    {
	    	if(buf.length() > 0)
	    	{
	    		buf.setCharAt(0, '\0');
	    	}
	    	else
	    	{
	    		buf.append('\0');
	    	}
	    }
	    
	    NamedColorList.nColors++;
	    return true;
	}
	
	// Returns number of elements 
	public static int cmsNamedColorCount(final cmsNAMEDCOLORLIST NamedColorList)
	{
		if (NamedColorList == null)
		{
			return 0;
		}
		return NamedColorList.nColors;
	}
	
	// Info aboout a given color
	public static boolean cmsNamedColorInfo(final cmsNAMEDCOLORLIST NamedColorList, int nColor, StringBuffer Name, StringBuffer Prefix, StringBuffer Suffix, 
			short[] PCS, short[] Colorant)
	{
		String str;
		
	    if (NamedColorList == null)
	    {
	    	return false;
	    }
	    
	    if (nColor >= cmsNamedColorCount(NamedColorList))
	    {
	    	return false;
	    }
	    
	    if (Name != null)
	    {
	    	str = NamedColorList.List[nColor].Name.toString();
	    	Utility.strncpy(Name, str, Utility.strlen(str));
	    }
	    if (Prefix != null)
	    {
	    	str = NamedColorList.Prefix.toString();
	    	Utility.strncpy(Prefix, str, Utility.strlen(str));
	    }
	    if (Suffix != null)
	    {
	    	str = NamedColorList.Suffix.toString();
	    	Utility.strncpy(Suffix, str, Utility.strlen(str));
	    }
	    if (PCS != null)
	    {
	    	System.arraycopy(NamedColorList.List[nColor].PCS, 0, PCS, 0, 3);
	    }
	    
	    if (Colorant != null)
	    {
	    	System.arraycopy(NamedColorList.List[nColor].DeviceColorant, 0, Colorant, 0, NamedColorList.ColorantCount);
	    }
	    
	    return true;
	}
	
	// Search for a given color name (no prefix or suffix)
	public static int cmsNamedColorIndex(final cmsNAMEDCOLORLIST NamedColorList, final String Name)
	{    
	    int i, n;
	    
	    if (NamedColorList == null)
	    {
	    	return -1;
	    }
	    n = cmsNamedColorCount(NamedColorList);
	    for (i=0; i < n; i++)
	    {
	        if (cmserr.cmsstrcasecmp(Name, NamedColorList.List[i].Name.toString()) == 0)
	        {
	        	return i;
	        }
	    }
	    
	    return -1;
	}
	
	// MPE support -----------------------------------------------------------------------------------------------------------------
	
	private static final _cmsStageFreeElemFn FreeNamedColorList = new _cmsStageFreeElemFn()
	{
		public void run(cmsStage mpe)
		{
			cmsNAMEDCOLORLIST List = (cmsNAMEDCOLORLIST)mpe.Data;
		    cmsFreeNamedColorList(List);
		}
	};
	
	private static final _cmsStageDupElemFn DupNamedColorList = new _cmsStageDupElemFn()
	{
		public Object run(cmsStage mpe)
		{
			cmsNAMEDCOLORLIST List = (cmsNAMEDCOLORLIST)mpe.Data;
		    return cmsDupNamedColorList(List);
		}
	};
	
	private static final _cmsStageEvalFn EvalNamedColor = new _cmsStageEvalFn()
	{
		public void run(float[] In, float[] Out, cmsStage mpe)
		{
			cmsNAMEDCOLORLIST NamedColorList = (cmsNAMEDCOLORLIST)mpe.Data;
		    short index = (short)lcms2_internal._cmsQuickSaturateWord(In[0] * 65535.0);
		    int j;
		    
		    if (index >= NamedColorList.nColors)
		    {
		        cmserr.cmsSignalError(NamedColorList.ContextID, lcms2.cmsERROR_RANGE, Utility.LCMS_Resources.getString(LCMSResource.CMSNAMED_COLOR_OUTOFRANGE), new Object[]{new Integer(index)});
		    }
		    else
		    {
		        for (j=0; j < NamedColorList.ColorantCount; j++)
		        {
		        	Out[j] = (NamedColorList.List[index].DeviceColorant[j] * (1f / 65535f));      
		        }
		    }
		}
	};
	
	// Named color lookup element
	public static cmsStage _cmsStageAllocNamedColor(cmsNAMEDCOLORLIST NamedColorList)
	{
	    return cmslut._cmsStageAllocPlaceholder(NamedColorList.ContextID, 
			                           lcms2.cmsSigNamedColorElemType, 
									   1, 3,
									   EvalNamedColor,
									   DupNamedColorList,
									   FreeNamedColorList,
									   cmsDupNamedColorList(NamedColorList));
	  
	}
	
	// Retrieve the named color list from a transform. Should be first element in the LUT
	public static cmsNAMEDCOLORLIST cmsGetNamedColorList(cmsHTRANSFORM xform)
	{
	    _cmsTRANSFORM v = (_cmsTRANSFORM)xform;
	    cmsStage mpe  = v.Lut.Elements;
	    
	    if (mpe.Type != lcms2.cmsSigNamedColorElemType)
	    {
	    	return null;
	    }
	    return (cmsNAMEDCOLORLIST)mpe.Data;
	}
	
	// Profile sequence description routines -------------------------------------------------------------------------------------
	
	public static cmsSEQ cmsAllocProfileSequenceDescription(cmsContext ContextID, int n)
	{
	    cmsSEQ Seq;
	    int i;
	    
	    if (n == 0)
	    {
	    	return null;
	    }
	    
	    // In a absolutely arbitrary way, I hereby decide to allow a maxim of 255 profiles linked
	    // in a devicelink. It makes not sense anyway and may be used for exploits, so let's close the door!
	    if (n > 255)
	    {
	    	return null;
	    }
	    
	    Seq = new cmsSEQ();
	    
	    Seq.ContextID = ContextID;
	    Seq.seq      = new cmsPSEQDESC[n];
	    Seq.n        = n;
	    
	    for (i=0; i < n; i++)
	    {
	    	Seq.seq[i] = new cmsPSEQDESC();
	        Seq.seq[i].Manufacturer = null;
	        Seq.seq[i].Model        = null;
	        Seq.seq[i].Description  = null;
	    }
	    
	    return Seq;
	}
	
	public static void cmsFreeProfileSequenceDescription(cmsSEQ pseq)
	{
	    int i;
	    
	    for (i=0; i < pseq.n; i++)
	    {
	        if (pseq.seq[i].Manufacturer != null)
	        {
	        	cmsMLUfree(pseq.seq[i].Manufacturer);
	        }
	        if (pseq.seq[i].Model != null)
	        {
	        	cmsMLUfree(pseq.seq[i].Model);
	        }
	        if (pseq.seq[i].Description != null)
	        {
	        	cmsMLUfree(pseq.seq[i].Description);
	        }
	    }
	    
	    if (pseq.seq != null)
	    {
	    	pseq.seq = null;
	    	//_cmsFree(pseq.ContextID, pseq.seq);
	    }
	    //_cmsFree(pseq.ContextID, pseq);
	}
	
	public static cmsSEQ cmsDupProfileSequenceDescription(final cmsSEQ pseq)
	{
	    cmsSEQ NewSeq;
	    int i;
	    
	    if (pseq == null)
	    {
	    	return null;
	    }
	    
	    NewSeq = new cmsSEQ();
	    
	    NewSeq.seq = new cmsPSEQDESC[pseq.n];
	    
	    NewSeq.ContextID = pseq.ContextID;
	    NewSeq.n        = pseq.n;
	    
	    for (i=0; i < pseq.n; i++)
	    {
	    	NewSeq.seq[i] = new cmsPSEQDESC();
	    	NewSeq.seq[i].attributes = pseq.seq[i].attributes;
	    	
	        NewSeq.seq[i].deviceMfg   = pseq.seq[i].deviceMfg;
	        NewSeq.seq[i].deviceModel = pseq.seq[i].deviceModel;
	        System.arraycopy(pseq.seq[i].ProfileID.data, 0, NewSeq.seq[i].ProfileID.data, 0, cmsProfileID.SIZE);
	        NewSeq.seq[i].technology  = pseq.seq[i].technology;
	        
	        NewSeq.seq[i].Manufacturer = cmsMLUdup(pseq.seq[i].Manufacturer);
	        NewSeq.seq[i].Model        = cmsMLUdup(pseq.seq[i].Model);
	        NewSeq.seq[i].Description  = cmsMLUdup(pseq.seq[i].Description);
	    }
	    
	    return NewSeq;
	}
}
