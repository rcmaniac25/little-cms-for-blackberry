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
//@Author Vinnie Simonetti
package littlecms.internal.helper;

import littlecms.internal.LCMSResource;

import net.rim.device.api.util.Arrays;
//#ifdef CMS_USE_NATIVE_BIGINT
import net.rim.device.api.crypto.CryptoByteArrayArithmetic;
import net.rim.device.api.crypto.CryptoInteger;;
//#endif

/**
 * Large number representation. Only handles unsigned integers.
 */
public final class LargeNumber
{
	private byte[] value;
    private Boolean mzero, mone;
	
	//Non-representation specific
    public LargeNumber(LargeNumber num)
    {
    	this.value = Arrays.copy(num.value);
    }
    
    public int compare(LargeNumber num)
    {
        return compare(this.value, num.value);
    }
    
    private static boolean greaterThenOrEqual(byte[] src1, byte[] src2)
    {
        return compare(src1, src2) >= 0;
    }
    
    public boolean greaterThenOrEqual(LargeNumber num)
    {
        return greaterThenOrEqual(this.value, num.value);
    }
    
    public LargeNumber add(LargeNumber num)
    {
        if (this.zero() || num.zero())
        {
            return new LargeNumber(0L);
        }
        if (this.zero())
        {
            return new LargeNumber(num);
        }
        else if (num.zero())
        {
            return new LargeNumber(this);
        }
        byte[] result = new byte[Math.max(this.value.length, num.value.length) + 1];
        add(this.value, num.value, result);
        return new LargeNumber(result);
    }
    
    public LargeNumber subtract(LargeNumber num)
    {
        if (this.zero() || num.zero())
        {
            return new LargeNumber(0L);
        }
        if (this.zero())
        {
            return new LargeNumber(num);
        }
        else if (num.zero())
        {
            return new LargeNumber(this);
        }
        byte[] result = new byte[Math.max(this.value.length, num.value.length)];
        subtract(this.value, num.value, result);
        return new LargeNumber(result);
    }
    
    public LargeNumber multiply(LargeNumber num)
    {
        if (this.zero() || num.zero())
        {
            return new LargeNumber(0L);
        }
        if (this.one())
        {
            return new LargeNumber(num);
        }
        else if (num.one())
        {
            return new LargeNumber(this);
        }
        byte[] result = new byte[(Math.max(this.value.length, num.value.length) * 2)]; //Numbers are expected to double in size (1b * 1b = 2b, 2b * 2b = 4b, etc.)
        multiply(this.value, num.value, result);
        return new LargeNumber(result);
    }
    
    public LargeNumber divideAndMod(LargeNumber num, LargeNumber[] mod)
    {
        if (this.zero())
        {
            if (mod != null)
            {
                mod[0] = new LargeNumber(0L);
            }
            return new LargeNumber(0L);
        }
        else if (num.zero())
        {
        	int result = 1 / 0;
        }
        else if (num.one())
        {
            if (mod != null)
            {
                mod[0] = new LargeNumber(0L);
            }
            return new LargeNumber(this);
        }
        byte[] result = new byte[this.value.length];
        byte[] mresult = mod != null ? new byte[this.value.length] : null;
        division(this.value, num.value, result, mresult);
        if (mod != null)
        {
            mod[0] = new LargeNumber(mresult);
        }
        return new LargeNumber(result);
    }
    
    public LargeNumber divide(LargeNumber num)
    {
        return divideAndMod(num, null);
    }
    
    public LargeNumber mod(LargeNumber num)
    {
        LargeNumber[] mod = new LargeNumber[1];
        divideAndMod(num, mod); //Wastes some resources but is much faster at processing
        return mod[0];

        //Cheap (in code), expensive (in resource and CPU usage) manner of execution
        //return this.subtract(this.divide(num).multiply(this));
    }
    
    private static boolean equals(byte[] src1, byte[] src2)
    {
        return compare(src1, src2) == 0;
    }
    
    public boolean equals(Object obj)
    {
    	if (obj instanceof LargeNumber)
        {
            return equals(this.value, ((LargeNumber)obj).value);
        }
        return false;
    }
    
    public int hashCode()
    {
    	int hash = 0;
    	if(this.mzero != null)
    	{
    		hash += this.mzero.hashCode();
    	}
    	if(this.mone != null)
    	{
    		hash += this.mone.hashCode();
    	}
    	for(int i = this.value.length - 1; i >= 0; i--)
    	{
    		hash += this.value[i];
    	}
    	return hash;
    }
    
    public String toString()
    {
    	return toString(null);
    }
    
    public String toString(StringBuffer builder)
    {
        if (value != null)
        {
            if (builder == null)
            {
                builder = new StringBuffer();
            }
            if (this.zero())
            {
                builder.append('0');
            }
            else if (this.one())
            {
                builder.append('1');
            }
            else
            {
                //All operations are done backwords. So the number 123 is stored 321.
            	StringBuffer power2 = new StringBuffer("1");
            	StringBuffer result = new StringBuffer("0");
                int[] mask = new int[] { 1, 2, 4, 8, 16, 32, 64, 128 };
                int bit = 0;
                int max = (this.value.length * 8) - 1;
                int temp;
//#ifdef CMS_USE_NATIVE_BIGINT
                byte[] tempBytes = new byte[this.value.length];
                for(int i = this.value.length - 1, j = 0; i >= 0; i--, j++)
                {
                	tempBytes[j] = this.value[i];
                }
//#endif
                while (bit <= max)
                {
                    //value[exp / 8] = (byte)(1 << (exp % 8));
                    temp = mask[bit % 8];
//#ifdef CMS_USE_NATIVE_BIGINT
                    if ((tempBytes[bit / 8] & temp) == temp)
//#else
                    if ((value[bit / 8] & temp) == temp)
//#endif
                    {
                        addString(power2, result);
                    }
                    bit++;
                    multiplyStringBy2(power2);
                }
                for (int i = result.length() - 1; i >= 0; i--)
                {
                    builder.append(result.charAt(i));
                }
            }
            return builder.toString();
        }
        return super.toString();
    }
    
    private static void addString(StringBuffer src, StringBuffer dest)
    {
        int slen = src.length();
        int dlen = dest.length();
        int carry = 0;
        int value;
        for (int i = 0; i < slen; i++)
        {
            if (i < dlen)
            {
                value = (src.charAt(i) - '0') + (dest.charAt(i) - '0') + carry;
                carry = 0;
                if (value > 9)
                {
                	dest.setCharAt(i, (char)((value % 10) + '0'));
                    carry = value / 10;
                }
                else
                {
                	dest.setCharAt(i, (char)(value + '0'));
                }
            }
            else
            {
                if (carry == 0)
                {
                    dest.append(src.toString().substring(i));
                    break; //Don't need to continue
                }
                else
                {
                    value = (src.charAt(i) - '0') + carry;
                    carry = 0;
                    if (value > 9)
                    {
                        dest.append(value % 10);
                        carry = value / 10;
                        slen++;
                    }
                    else
                    {
                        dest.append(value);
                    }
                }
            }
        }
        if (carry != 0)
        {
            dest.append(carry);
        }
    }
    
    private static void multiplyStringBy2(StringBuffer number)
    {
        int len = number.length();
        int carry = 0;
        int value;
        for (int i = 0; i < len; i++)
        {
            value = number.charAt(i) - '0';
            value = (value * 2) + carry;
            carry = 0;
            if (value > 9)
            {
            	number.setCharAt(i, (char)((value % 10) + '0'));
                carry = 1;
            }
            else
            {
            	number.setCharAt(i, (char)(value + '0'));
            }
        }
        if (carry != 0)
        {
            number.append(carry);
        }
    }
	
	//Representation specific
//#ifdef CMS_USE_NATIVE_BIGINT
    public LargeNumber(int exp)
    {
    	exp = Math.abs(exp);
        if (exp > 1024)
        {
        	throw new IllegalArgumentException(Utility.LCMS_Resources.getString(LCMSResource.LARGENUM_INVALID_EXP));
        }
        this.value = CryptoByteArrayArithmetic.createArray(exp);
    }
    
    public LargeNumber(long value)
    {
    	this.value = CryptoByteArrayArithmetic.valueOf(value);
    }
    
    private LargeNumber(byte[] data)
    {
    	this.value = CryptoByteArrayArithmetic.trim(data);
    }
    
    private static int compare(byte[] src1, byte[] src2)
    {
    	return new CryptoInteger(src1).compareTo(new CryptoInteger(src2));
    }
    
    public boolean zero()
    {
        if (this.mzero != null)
        {
            return this.mzero.booleanValue();
        }
        boolean result = CryptoByteArrayArithmetic.isZero(this.value);
        this.mzero = new Boolean(result);
        return result;
    }
    
    public boolean one()
    {
        if (this.mone != null)
        {
            return this.mone.booleanValue();
        }
        boolean result = CryptoByteArrayArithmetic.isOne(this.value);
        this.mone = new Boolean(result);
        return result;
    }
    
    private static void add(byte[] src1, byte[] src2, byte[] dest)
    {
    	CryptoByteArrayArithmetic.add(src1, src2, dest.length * 8, dest);
    }
    
    private static void subtract(byte[] src1, byte[] src2, byte[] dest)
    {
    	CryptoByteArrayArithmetic.subtract(src1, src2, dest.length * 8, dest);
    }
    
    private static void multiply(byte[] src1, byte[] src2, byte[] dest)
    {
    	CryptoByteArrayArithmetic.multiply(src1, src2, dest.length * 8, dest);
    }
    
    private static void division(byte[] numerator, byte[] denominator, byte[] dest, byte[] mdest)
    {
    	try
    	{
    		CryptoByteArrayArithmetic.divide(numerator, denominator, dest, mdest);
    	}
    	catch(ArithmeticException e)
    	{
    	}
    }
//#else
    public LargeNumber(int exp)
    {
    	exp = Math.abs(exp);
        if (exp > 1024)
        {
        	throw new IllegalArgumentException(Utility.LCMS_Resources.getString(LCMSResource.LARGENUM_INVALID_EXP));
        }
        this.value = new byte[(exp / 8) + 1];
        this.value[exp / 8] = (byte)(1 << (exp % 8));
    }
    
    public LargeNumber(long value)
    {
    	this.value = BitConverter.getBytes(value);
    	//Do to the manner in which BitConverter has been written, the result needs to be reversed
    	if(value != 0) //Don't need to reverse the number zero
    	{
    		byte[] properByteOrder = new byte[this.value.length];
    		for(int i = this.value.length - 1, j = 0; i >= 0; i--, j++)
            {
    			properByteOrder[j] = this.value[i];
            }
    		this.value = properByteOrder;
    	}
        //Trim value to speed up functions
        int i;
        for (i = this.value.length - 1; i >= 0; i--)
        {
            if (this.value[i] != 0)
            {
                break;
            }
        }
        //Make sure that it doesn't try to just copy the long value
        if (i < 7)
        {
        	this.value = Arrays.copy(this.value, 0, i + 1);
        }
    }
    
    private LargeNumber(byte[] data)
    {
    	//Trim value to speed up functions
        int i, mlen = data.length - 1;
        for (i = mlen; i >= 0; i--)
        {
            if (data[i] != 0)
            {
                break;
            }
        }
        if (i < mlen)
        {
        	this.value = Arrays.copy(data, 0, i + 1);
        }
        else
        {
            this.value = data;
        }
    }
    
    private static int compare(byte[] src1, byte[] src2)
    {
        int nlen = src2.length;
        int tlen = src1.length;
        if (nlen != tlen)
        {
            //If more items exist then check to see if the other has a value
            byte[] snum = nlen > tlen ? src2 : src1;
            boolean greater = nlen < tlen;
            int slen = snum.length;
            int limit = slen - (slen - (nlen < tlen ? nlen : tlen));
            for (int i = slen - 1; i >= limit; i--)
            {
                if (snum[i] != 0)
                {
                    return greater ? 1 : -1;
                }
            }
        }
        //Check the body of the two numbers
        int size = Math.min(nlen, tlen);
        for (int i = size - 1; i >= 0; i--)
        {
            if (src2[i] != src1[i])
            {
                if (src2[i] > src1[i])
                {
                    return -1; //'num' is greater then this number
                }
                return 1; //this is greater then 'num'
            }
        }
        return 0;
    }
    
    public boolean zero()
    {
        if (this.mzero != null)
        {
            return this.mzero.booleanValue();
        }
        int len = this.value.length;
        boolean result = true;
        for (int i = 0; i < len; i++)
        {
            if (this.value[i] != 0)
            {
                result = false;
                break;
            }
        }
        this.mzero = new Boolean(result);
        return result;
    }
    
    public boolean one()
    {
        if (this.mone != null)
        {
            return this.mone.booleanValue();
        }
        boolean result = this.value[0] == 1;
        int len = this.value.length;
        for (int i = 1; i < len; i++)
        {
            if (this.value[i] != 0)
            {
                result = false;
                break;
            }
        }
        this.mone = new Boolean(result);
        return result;
    }
    
    private static void add(byte[] src1, byte[] src2, byte[] dest)
    {
        int carry = 0;
        int len1 = src1.length;
        int len2 = src2.length;
        int max = Math.min(len1, len2);
        int temp;
        for (int i = 0; i < max; i++)
        {
            temp = (src1[i] & 0xFF) + (src2[i] & 0xFF) + carry;
            carry = 0;
            if (temp > 0xFF)
            {
                carry = temp >>> 8;
            }
            dest[i] = (byte)(temp & 0xFF);
        }
        if (Math.max(len1, len2) > max)
        {
            byte[] value = len1 > len2 ? src1 : src2;
            len1 = value.length;
            while (max < len1)
            {
                temp = (value[max] & 0xFF) + carry;
                carry = 0;
                if (temp > 0xFF)
                {
                    carry = temp >>> 8;
                }
                dest[max++] = (byte)(temp & 0xFF);
            }
        }
        while (carry != 0)
        {
            dest[max++] = (byte)(carry & 0xFF);
            carry >>>= 8;
        }
    }
    
    private static void subtract(byte[] src1, byte[] src2, byte[] dest)
    {
        int comp = compare(src1, src2);
        if (comp < 0)
        {
            throw new IllegalArgumentException(Utility.LCMS_Resources.getString(LCMSResource.LARGENUM_SRC1_LESST_SRC2));
        }
        else if (comp == 0)
        {
            //1 - 1 = 0
        	Arrays.zero(dest);
        }
        else
        {
            //2 - 1 = 1
            int len1 = src1.length;
            int len2 = src2.length;
            int max = Math.min(len1, len2);
            byte[] src1c = Arrays.copy(src1);
            int v1, v2;
            for (int i = 0; i < max; i++)
            {
                v1 = (src1c[i] & 0xFF);
                v2 = (src2[i] & 0xFF);
                if (v1 >= v2)
                {
                    dest[i] = (byte)(v1 - v2);
                }
                else
                {
                    dest[i] = (byte)((0x100 - v2) + v1); //Do this to get the proper value...
                    
                    //...now get the 0x100 (AKA borrowing a bit)
                    for (int k = i + 1; k < len1; k++)
                    {
                        if (src1c[k] != 0)
                        {
                            //Found it
                            src1c[k]--;
                            break;
                        }
                        else
                        {
                            //Hmm, no bit. Well since I need to borrow a bit anyway I will "credit" the byte and take it from the next byte.
                            src1c[k] = (byte)0xFF;
                        }
                    }
                }
            }
            if (Math.max(len1, len2) > max)
            {
                if (Math.max(len1, len2) <= dest.length) //Only copy the higher values if dest is large enough to hold it
                {
                    byte[] value = len1 > len2 ? src1c : src2;
                    System.arraycopy(value, max, dest, max, value.length - max);
                }
            }
        }
    }
    
    private static void multiply(byte[] src1, byte[] src2, byte[] dest)
    {
        int tlen = src1.length;
        int nlen = src2.length;
        int multi, temp, blen;
        byte[] buffer = new byte[blen = dest.length]; //Numbers are offset before being added. The offset numbers will go here
        for (int i = 0; i < nlen; i++)
        {
            multi = src2[i] & 0xFF;
            for (int k = 0; k < tlen; k++)
            {
                //Zero out the off-buffer
                for (int j = blen - 1; j >= 0; j--)
                {
                    buffer[j] = 0;
                }
                //Calculate the value
                temp = (src1[k] & 0xFF) * multi;
                if (temp > 255)
                {
                    buffer[i + k] = (byte)(temp & 0xFF);
                    buffer[i + k + 1] = (byte)((temp >> 8) & 0xFF);
                }
                else
                {
                    buffer[i + k] = (byte)temp;
                }
                //Add the temporary result to the final buffer
                add(dest, buffer, dest);
            }
        }
    }
    
    private static void division(byte[] numerator, byte[] denominator, byte[] dest, byte[] mdest)
    {
        boolean modExists = mdest != null;
        int comp = compare(numerator, denominator);
        if (comp < 0)
        {
            throw new IllegalArgumentException(Utility.LCMS_Resources.getString(LCMSResource.LARGENUM_SRC1_LESST_SRC2));
        }
        else if (comp == 0)
        {
            //Values are equal
        	Arrays.zero(dest);
        	if (modExists)
            {
        		Arrays.zero(mdest);
            }
            dest[0] = 1;
        }
        else
        {
            //src1 is greater then src2, actually do the division process this time
        	
            //Get array lengths
            int dlen = dest.length;
            if (!modExists)
            {
                mdest = new byte[dlen];
            }
            int len1 = numerator.length;
            int len2 = denominator.length;
            
            //Get values
            if (modExists)
            {
                //No need to zero out the value if it had to be created
            	Arrays.zero(mdest);
            }
            System.arraycopy(numerator, 0, mdest, 0, len1); //mdest is the remainder of division. So copy src1 to this, then use it for subtraction to get the divident. Then what is left in mdest is the remaineder of the division operation
            int blen;
            byte[] buffer = new byte[blen = (dlen + len2 - 1)];
            byte[] buffer2 = new byte[blen];
            
            //Process
            for (int i = len1 - 1; i >= 0; i--)
            {
                //Zero out the buffers
            	Arrays.zero(buffer);
            	Arrays.zero(buffer2);
                //Get the adjusted value
                System.arraycopy(mdest, i, buffer, i, len1 - i);
                System.arraycopy(denominator, 0, buffer2, i, len2);
                comp = compare(buffer, buffer2);
                //Calculate the value
                if (comp == 0)
                {
                    dest[i] = 1;
                    subtract(mdest, buffer2, mdest);
                }
                else if (comp > 0)
                {
                    //Prepare some values for processing
                    int bPos = 7;
                    
                    //Shift the denominator left for processing
                    for (int b = len2 - 1 + i; b >= 0; b--)
                    {
                        int tval = (buffer2[b] & 0xFF) << 7;
                        buffer2[b] = (byte)(tval & 0xFF);
                        buffer2[b + 1] |= (byte)((tval >> 8) & 0xFF);
                    }
                    
                    //Process
                    for (; bPos >= 0; bPos--)
                    {
                        //Determine if the den. is less then the num.
                        if (compare(buffer, buffer2) >= 0)
                        {
                            //Set the actual bit
                            dest[bPos / 8] |= (byte)(1 << (bPos % 8));

                            //Subtract
                            subtract(mdest, buffer2, mdest); //One for the remained
                            if (bPos > 0) //Only need to do this if another loop was going to occur
                            {
                            	System.arraycopy(mdest, i, buffer, i, len1 - i);
                            }
                        }
                        if (bPos > 0)
                        {
                            //Shift the den. right one bit
                            for (int b = i; b <= len2 + i; b++)
                            {
                                if (b != 0)
                                {
                                    //Carry over a bit
                                    buffer2[b - 1] |= (byte)((buffer2[b] & 0x01) << 7);
                                }
                                buffer2[b] >>>= 1;
                            }
                        }
                    }
                    
                    /*Cheap (in code), expensive (in processing and resources)
                    byte multi = 1;
                    byte[] temp = new byte[blen];
                    byte[] temp2 = new byte[blen];
                    System.arraycopy(buffer2, 0, temp2, 0, blen);
                    while (compare(buffer, temp2) > 0)
                    {
                        temp2 = new byte[blen];
                        System.arraycopy(buffer2, 0, temp, 0, blen);
                        multiply(temp, new byte[] { multi++ }, temp2);
                    }
                    multi--;
                    System.arraycopy(buffer2, 0, temp2, 0, blen);
                    dest[i] = --multi;
                    multiply(buffer2, new byte[] { --multi }, temp2);
                    subtract(mdest, temp2, mdest);
                    //*/
                }
            }
        }
    }
//#endif
}
