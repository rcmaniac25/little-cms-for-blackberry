//---------------------------------------------------------------------------------
//
//  Little Color Management System
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
 * The serializers that are used
 */
public final class Serializers
{
	//This class is located in the "root" internal namespace instead of the helper namespace so that it can access some internal classes
	
	private static boolean init;
	
	static
	{
		init = false;
		
		//Native
		//TODO Calendar
		//TODO StringBuffer
		
		//LCMS
		//TODO FILENULL
		//TODO FILEMEM
		//TODO cmsIOHANDLER
		//TODO _cmsICCPROFILE
		//TODO cmsProfileID
		//TODO StringBuffer
		//TODO cmsICCHeader
		//TODO cmsDateTimeNumber
		//TODO cmsEncodedXYZNumber
		//TODO Stream.FileStream
		//TODO cmsTagEntry
		//TODO _cmsSubAllocator_chunk
		//TODO _cmsSubAllocator
		//TODO _cmsTagTypeLinkedList
		//TODO cmsTagTypeHandler
		//TODO cmsCIEXYZ
		//TODO cmsCIExyYTRIPLE
		//TODO cmsICCData
		//TODO cmsICCMeasurementConditions
		//TODO cmsMAT3
		//TODO cmsVEC3
		//TODO cmsScreening
		//TODO cmsICCViewingConditions
		//TODO cmsCIELab
		
		init = true;
	}
	
	public static boolean initialize()
	{
		return init; //This is simply to cause the static constructor to get invoked if not already invoked
	}
}
