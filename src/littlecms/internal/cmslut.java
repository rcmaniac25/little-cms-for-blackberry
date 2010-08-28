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

import littlecms.internal.lcms2.cmsPipeline;
import littlecms.internal.lcms2.cmsStage;
import littlecms.internal.lcms2_internal._cmsStage_struct;

//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class cmslut
{
	//TODO #30-101
	
	public static boolean cmsPipelineCheckAndRetreiveStages(final cmsPipeline Lut, int n, Object[] stages)
	{
		int i;
	    cmsStage mpe;
	    int Type;
	    int stageL = 0;
	    
	    // Make sure same number of elements
	    if (cmsPipelineStageCount(Lut) != n)
	    {
	    	return false;
	    }
	    
	    // Iterate across asked types
	    mpe = Lut.Elements;
	    for (i = 0; i < n; i++)
	    {
	        // Get asked type
	    	Type = ((Integer)stages[stageL++]).intValue();
	        if (mpe.Type != Type)
	        {
	            // Mismatch. We are done.
	            return false;
	        }
	        mpe = (cmsStage)mpe.Next;
	    }
	    
	    // Found a combination, fill pointers if not NULL
	    mpe = Lut.Elements;
	    for (i = 0; i < n; i++)
	    {
	    	stages[stageL++] = mpe; //Usually checks to make sure destination not null but no need to here
	        mpe = (cmsStage)mpe.Next;
	    }
	    
	    return true;
	}
	
	//TODO #152
}
