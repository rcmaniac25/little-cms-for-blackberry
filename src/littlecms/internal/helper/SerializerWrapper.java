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
//@Author Vinnie Simonetti
package littlecms.internal.helper;

//Helper class so redundant code amongst serializers can be consolidated
public abstract class SerializerWrapper implements VirtualPointer.Serializer
{
    private boolean _canWrap, _canProcessArray;
    private Class _class;
    
    protected SerializerWrapper(boolean canWrap, Class class2match)
    {
    	this(canWrap, false, class2match);
    }
    
    protected SerializerWrapper(boolean canWrap, boolean arraySupport, Class class2match)
    {
        this._canWrap = canWrap;
        this._canProcessArray = arraySupport;
        this._class = class2match;
    }
    
    public int serialize(VirtualPointer vp, Object val)
    {
    	//General checks if there is enough data, the "val" is not null, and the serializer can process the content
        if (vp.getAllocLen() < getSerializedSize(val) + vp.getPosition())
        {
            return VirtualPointer.Serializer.STATUS_NEED_MORE_DATA;
        }
        if (val == null)
        {
            return VirtualPointer.Serializer.STATUS_REQUIRES_OBJECT;
        }
        if (!canProcess(val))
        {
            return VirtualPointer.Serializer.STATUS_UNSUPPORTED;
        }
        return inSerialize(vp, val);
    }
    
    public int deserialize(VirtualPointer vp, Object[] val)
    {
    	//General checks if there is a place to put the object deserialized
        if (val == null || val.length < 1)
        {
            return VirtualPointer.Serializer.STATUS_REQUIRES_OBJECT;
        }
        if (val.length == 1) //Should an alternative case be made for if an array is being deserialized?
        {
        	if(vp.getAllocLen() < getSerializedSize(val[0]) + vp.getPosition())
        	{
        		return VirtualPointer.Serializer.STATUS_NEED_MORE_DATA;
        	}
        }
        return inDeserialize(vp, val);
    }
    
    /** Serialize <i>val</i>. Note: Pointers need to be incremented after writing the data to them. Array support is not necessary.*/
    public abstract int inSerialize(VirtualPointer vp, Object val);
    
    /**
	 * Deserialize a value into <i>val</i>. Note: Pointers need to be incremented after reading the data from them. If <i>val</i>'s length is greater then 1 then
	 * the Serializer should deserialize an array of items. Array support is necessary (specified below):
	 * <p>
	 * The first element is a Integer that specifies how many elements are in the array, if this value is
	 * negative then it is the number of bytes that should be read (simply negate the number). If the element has a fixed size then the size can simply be
	 * divided to get the number of elements, else the Serializer should read until the byte count equals or exceeds the specified size, VirtualPointer will take
	 * care of the rest.
	 * <p>
	 * The second element is the total read in byte count, it is a int[] of one element and should be set before returning out of the function.
	 * <p>
	 * Finally the last element is the data itself, if this is not null then it should be used to set the data. Do not take the first element in the <i>val</i>
	 * array as fact, if this element in the array is smaller then what the first element says then take this element's length instead of the first element. If it
	 * is null then take the first element in <i>val</i> and use that as the length the array should be. Note that if the first parameter is does not end on the
	 * proper bounds for an element the the rest of the element should be made up so as to not mix up data (as if the element went outside of it's allocated
	 * bounds where it is undefined).
	 */
    public abstract int inDeserialize(VirtualPointer vp, Object[] val);
    
    public abstract int getSerializedSize(Object val);
    
    public boolean canProcess(Object val)
    {
    	//Determines if a serializer can process an object based on Class
        if (val == null)
        {
            return false;
        }
        Class clazz = val.getClass();
        if(clazz.isArray())
        {
        	if(!_canProcessArray)
        	{
        		return false;
        	}
        	return this._class.isInstance(((Object[])val)[0]);
        }
        return this._class.isInstance(val);
    }
    
    public boolean canWrap(Object val)
    {
        if (val == null)
        {
            return false;
        }
        return this._canWrap;
    }
}
