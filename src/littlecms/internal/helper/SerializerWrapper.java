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
package littlecms.internal.helper;

public abstract class SerializerWrapper implements VirtualPointer.Serializer
{
    private boolean _canWrap;
    private Class _class;

    protected SerializerWrapper(boolean canWrap, Class class2match)
    {
        this._canWrap = canWrap;
        this._class = class2match;
    }

    public int serialize(VirtualPointer vp, Object val)
    {
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
        if (val == null || val.length < 1)
        {
            return VirtualPointer.Serializer.STATUS_REQUIRES_OBJECT;
        }
        if (vp.getAllocLen() < getSerializedSize(val[0]) + vp.getPosition())
        {
            return VirtualPointer.Serializer.STATUS_NEED_MORE_DATA;
        }
        return inDeserialize(vp, val);
    }

    public abstract int inSerialize(VirtualPointer vp, Object val);

    public abstract int inDeserialize(VirtualPointer vp, Object[] val);

    public abstract int getSerializedSize(Object val);

    public boolean canProcess(Object val)
    {
        if (val == null)
        {
            return false;
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
