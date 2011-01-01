//#preprocessor

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

import java.io.PrintStream;
import java.util.Vector;

import littlecms.internal.LCMSResource;

import net.rim.device.api.util.Arrays;
import net.rim.device.api.util.StringUtilities;
/*
//#ifndef BlackBerrySDK4.5.0
import net.rim.device.api.util.MathUtilities;
//#endif
 */

//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class PrintUtility
{
	/* There are many components to printf that are implementation specific.
	 * 
	 * This mostly follows the specification mentioned http://www.cplusplus.com/reference/clibrary/cstdio/printf/
	 * 
	 * Implementation specific components:
	 * Precision:
	 * 	If no precision is mentioned then the default precision is 6.
	 * 
	 * Formats:
	 * 	f, F: Fixed-point printing, if the number is an integer then the trailing zeros are removed unless the '#' flag is used which case the decimal and one zero 
	 * 		will be retained. Also if it is an integer and a precision is specified then it will follow the precision instead of removing the decimal.
	 *	e, E: Same as 'f' and 'F' but with exponent notation (+/-d.ddd e-/+00).
	 *	p: Bit of inside information, it prints out the position the pointer is currently at. A new VirtualPointer will be 0000:0000. Moving 16 bytes in will print
	 *		out 0000:000F.
	 */
	
	public static int output(PrintStream out, int max, final String format, Object[] args, Object[][] formatCache)
	{
		if(max == 0)
        {
			//Regardless of if there is a format or not, nothing is going to get returned.
        	return 0;
        }
		Object[] formats;
		if(formatCache != null && formatCache.length > 0 && formatCache[0] != null)
		{
			formats = formatCache[0];
		}
		else
		{
			formats = breakFormat(format, null);
			if(formatCache != null && formatCache.length > 0)
			{
				formatCache[0] = formats;
			}
		}
        if (max < 0)
        {
            max = Integer.MAX_VALUE;
        }
        int count = 0;
        int len = formats.length;
        int argLen = args == null ? 0 : args.length;
        int elLen;
        int argPos = 0;
        for(int i = 0; i < len; i++)
        {
	        Object obj = formats[i];
	        String str = null;
	        if(obj instanceof String)
	        {
		        str = (String)obj;
	        }
	        else
	        {
		        FormatElement form = (FormatElement)obj;
                if (form == null)
                {
                	throw new IllegalArgumentException(Utility.LCMS_Resources.getString(LCMSResource.BAD_STRING_FORMAT));
                }
		        int req = form.requires();
		        if(req > 0)
		        {
			        Long one;
			        Long two = null;
			        if(argPos + req < argLen)
			        {
				        one = getAsLong(args[argPos++]);
				        if(req == 2)
				        {
					        two = getAsLong(args[argPos++]);
				        }
				        form.setInputValue(one, two);
			        }
			        else
			        {
				        argPos += req; //Do this so that it only prints the format out
			        }
		        }
                if (form.getFormat().endsWith("n"))
                {
                    ((int[])args[argPos++])[0] = count;
                }
                else
                {
                    if (argPos >= argLen)
                    {
                        str = form.getFormat();
                    }
                    else
                    {
                        str = form.format(form.takesArg() ? (form.hasArgLocation() ? args[form.argLocation()] : args[argPos++]) : null);
                    }
                }
	        }
            if (str != null)
            {
                elLen = str.length();
                if (elLen + count > max)
                {
                    out.print(str.substring(0, max));
                    count = max;
                    break;
                }
                else
                {
                    out.print(str);
                    count += elLen;
                }
            }
        }
        if(formatCache != null && formatCache.length > 0)
		{
        	for(int i = 0; i < len; i++)
        	{
        		if(formats[i] instanceof FormatElement)
        		{
        			((FormatElement)formats[i]).reset();
        		}
        	}
		}
        return count;
	}
	
	public static int sscanf(final String str, final String format, Object[] argptr, Object[][] formatCache)
    {
		boolean doValidate = true;
		Object[] formats;
		if(formatCache != null && formatCache.length > 0 && formatCache[0] != null)
		{
			doValidate = false;
			formats = formatCache[0];
		}
		else
		{
			formats = breakFormat(format, null);
			if(formatCache != null && formatCache.length > 0)
			{
				formatCache[0] = formats;
			}
		}
        char[] chars = str.toCharArray();
        int slen = str.length();
        int len = formats.length;
        int argLen = argptr.length;
        int[] tempVals = new int[2]; //index 0 is "str pos", index 1 is "arg pos"
        //Simplify formats even more, this will not be cached because if the cache was used on output it would create an invalid formatted element
        for (int i = 0; i < len; i++)
        {
            Object obj = formats[i];
            if (obj instanceof String)
            {
                String tmp = (String)obj;
                tmp = tmp.trim();
                if (tmp.length() == 0)
                {
                    Object[] nForms = new Object[len - 1];
                    System.arraycopy(formats, 0, nForms, 0, i);
                    System.arraycopy(formats, i + 1, nForms, i, len - (i + 1));
                    len = nForms.length;
                    i--;
                    formats = nForms;
                }
                else
                {
                    Vector nEl = null;
                    int l = tmp.length();
                    for (int k = 0; k < l; k++)
                    {
                        if (isWhiteSpace(tmp.charAt(k)))
                        {
                            if (nEl == null)
                            {
                                nEl = new Vector();
                            }
                            nEl.addElement(tmp.substring(0, k).trim());
                            tmp = tmp.substring(k).trim();
                            k = 0;
                            l = tmp.length();
                        }
                    }
                    if (nEl != null)
                    {
                        //Get forgotten element
                        nEl.addElement(tmp);
                        int nElSize;
                        Object[] nForms = new Object[len + (nElSize = nEl.size()) - 1];
                        System.arraycopy(formats, 0, nForms, 0, i);
                        Object[] tObj = new Object[nElSize];
                        nEl.copyInto(tObj);
                        System.arraycopy(tObj, 0, nForms, i, nElSize);
                        System.arraycopy(formats, i + 1, nForms, i + nElSize, len - (i + 1));
                        len = nForms.length;
                        i += nElSize - 1;
                        formats = nForms;
                    }
                    else
                    {
                        formats[i] = tmp;
                    }
                }
            }
        }
        //Process str
        for (int i = 0; i < len && tempVals[0] < slen; i++)
        {
            //Skip Whitespace
            while (isWhiteSpace(chars[tempVals[0]]))
            {
                tempVals[0]++;
            }
            if (tempVals[0] >= slen)
            {
                break;
            }
            
            //Process elements
            Object obj = formats[i];
            if (obj instanceof String)
            {
                String tmp = (String)obj;
                if (str.indexOf(tmp, tempVals[0]) != tempVals[0])
                {
                    break;
                }
                tempVals[0] += tmp.length();
            }
            else
            {
                FormatElement form = (FormatElement)obj;
                if (form == null)
                {
                	throw new IllegalArgumentException(Utility.LCMS_Resources.getString(LCMSResource.BAD_STRING_FORMAT));
                }
                if(doValidate)
                {
                	if(!validateUnformatter(form.getFormat()))
                	{
                		//Invalid format, return
                		if(formatCache != null && formatCache.length > 0)
                		{
                			formatCache[0] = null;
                		}
                		break;
                	}
                }
                int sp = tempVals[0];
                int ap = tempVals[1];
                form.unformat(str, argptr, tempVals);
                if (sp == tempVals[0] && ap == tempVals[1])
                {
                    //This means something went wrong, no changes to the input String or arguments occured
                    break;
                }
                //count += (tempVals[1] - ap); //This is simply the number of arguments read
                if(tempVals[1] >= argLen)
                {
                	break; //Reached the max number of args that can be parsed, no need to keep processing
                }
            }
        }
        if(formatCache != null && formatCache.length > 0)
		{
        	for(int i = 0; i < len; i++)
        	{
        		if(formats[i] instanceof FormatElement)
        		{
        			((FormatElement)formats[i]).reset();
        		}
        	}
		}
        return tempVals[1];
    }
	
	private static boolean validateUnformatter(String form)
	{
		//TODO: Figure out how to do a general validation of format
		return false;
	}
	
	static boolean isWhiteSpace(char c)
    {
        switch (c)
        {
            case ' ':
            case '\n':
            case '\r':
            case '\t':
            case '\0':
                return true;
            default:
                return false;
        }
    }
	
	private static Long getAsLong(Object arg)
	{
		if(arg instanceof Long)
		{
			return (Long)arg;
		}
		long l;
		if(arg instanceof Byte)
		{
			l = ((Byte)arg).byteValue() & 0xFF;
		}
		else if(arg instanceof Short)
		{
			l = ((Short)arg).shortValue() & 0xFFFF;
		}
		else if(arg instanceof Integer)
		{
			l = ((Integer)arg).longValue() & 0xFFFFFFFFL;
		}
		else
		{
			l = 0; //Invalid
		}
		return new Long(l);
	}
	
	private static final long SPECIFIERS_UID = 0x318AE6E8A41FCF70L;
	private static final long FLAGS_UID = 0x94E998EE54BD00EL;
	private static final long WIDTH_PRECISION_UID = 0xFF217872C5D4CDDL;
	private static final long LENGTH_UID = 0x9EF53E8BFE248140L;
	private static final long FULL_FORMAT_UID = 0x3024E7CCD507CBE0L;
	
	private static String SPECIFIERS;
	private static String FLAGS;
	private static String WIDTH_PRECISION;
	private static String LENGTH;
	private static String FULL_FORMAT;
	
	static
	{
		String temp = (String)Utility.singletonStorageGet(SPECIFIERS_UID);
		if(temp == null)
		{
			SPECIFIERS = "cdieEfFgGosuxXpn";
			FLAGS = "-+ #0";
			WIDTH_PRECISION = "123456789*0"; //Zero is added at end so that when FULL_FORMAT is generated there isn't two zeros in the format. It wouldn't cause an error but it would be one more char to check that isn't needed.
			LENGTH = "hlLzjt";
			FULL_FORMAT = FLAGS + WIDTH_PRECISION.substring(0, 9) + '.' + LENGTH + SPECIFIERS;
			Utility.singletonStorageSet(SPECIFIERS_UID, SPECIFIERS);
			Utility.singletonStorageSet(FLAGS_UID, FLAGS);
			Utility.singletonStorageSet(WIDTH_PRECISION_UID, WIDTH_PRECISION);
			Utility.singletonStorageSet(LENGTH_UID, LENGTH);
			Utility.singletonStorageSet(FULL_FORMAT_UID, FULL_FORMAT);
		}
		else
		{
			SPECIFIERS = temp;
			FLAGS = (String)Utility.singletonStorageGet(FLAGS_UID);
			WIDTH_PRECISION = (String)Utility.singletonStorageGet(WIDTH_PRECISION_UID);
			LENGTH = (String)Utility.singletonStorageGet(LENGTH_UID);
			FULL_FORMAT = (String)Utility.singletonStorageGet(FULL_FORMAT_UID);
		}
	}
	
	public static Object[] breakFormat(final String format, String[][] args)
	{
		StringBuffer bu = new StringBuffer();
        Vector parts = new Vector();
        int len = format.length();
        boolean inFormat = false;
        int section = -1;
        Vector argList = args == null ? null : new Vector(6);
        for (int i = 0; i < len; i++)
        {
            char c = format.charAt(i);
            if (inFormat)
            {
                //First remove any arg location parameter
                int argPosIdPos = format.indexOf('$', i);
                if (argPosIdPos >= 0)
                {
                    //Not very efficient but works well
                    String sub = format.substring(i, argPosIdPos);
                    int inLen = sub.length();
                    int k;
                    for (k = 0; k < inLen; k++)
                    {
                        if (!Character.isDigit(sub.charAt(k)))
                        {
                            break;
                        }
                    }
                    if (k == inLen)
                    {
                        if (argList != null)
                        {
                            argList.addElement(sub);
                        }
                        else
                        {
                            bu.append(sub);
                            bu.append('$');
                        }
                        i += inLen;
                        continue;
                    }
                }
                if (matchesChar(c, FULL_FORMAT)) //Valid identifiers
                {
                    bu.append(c);
                    switch (section)
                    {
                        case -1: //Bad format
                        	throw new IllegalArgumentException(Utility.LCMS_Resources.getString(LCMSResource.BAD_STRING_FORMAT));
                        case 0: //General (everything is possible)
                            if (matchesChar(c, SPECIFIERS))
                            {
                                //Found the end, exit
                                section = -1;
                                String str = bu.toString();
                                if (argList != null)
                                {
                                	for(int j = 0; j < 4; j++)
                                	{
                                		argList.addElement(null);
                                	}
                                    argList.addElement(str.substring(1));
                                }
                                else
                                {
                                    //If we don't do this it will become redundent and we will get a stack overflow
                                    parts.addElement(FormatElement.getFormatter(str));
                                }
                                bu.setLength(0);
                                inFormat = false;
                            }
                            else if (matchesChar(c, FLAGS))
                            {
                                if (argList != null)
                                {
                                    argList.addElement(bu.toString().substring(1));
                                }
                                section++; //Found flag section, now to check for next section
                            }
                            else if (matchesChar(c, WIDTH_PRECISION))
                            {
                                if (argList != null)
                                {
                                    argList.addElement(null);
                                    argList.addElement(bu.toString().substring(1));
                                }
                                section += 2; //Found width section, now to check for next section
                            }
                            else if (c == '.')
                            {
                                if (i + 1 < len)
                                {
                                    if (matchesChar(format.charAt(i + 1), WIDTH_PRECISION))
                                    {
                                        if (argList != null)
                                        {
                                        	for(int j = 0; j < 2; j++)
                                        	{
                                        		argList.addElement(null);
                                        	}
                                            argList.addElement(bu.toString().substring(1));
                                        }
                                        section += 3; //Found precision section, now to check for next section
                                    }
                                    else if (!matchesChar(format.charAt(i + 1), SPECIFIERS))
                                    {
                                        throw new IllegalArgumentException(Utility.LCMS_Resources.getString(LCMSResource.BAD_STRING_FORMAT));
                                    }
                                }
                                else
                                {
                                    throw new IllegalArgumentException(Utility.LCMS_Resources.getString(LCMSResource.BAD_STRING_FORMAT));
                                }
                            }
                            else if (matchesChar(c, LENGTH))
                            {
                                if (argList != null)
                                {
                                	for(int j = 0; j < 3; j++)
                                	{
                                		argList.addElement(null);
                                	}
                                    argList.addElement(bu.toString().substring(1));
                                }
                                section += 4; //Found length section, now to check for next section
                            }
                            else
                            {
                                throw new IllegalArgumentException(Utility.LCMS_Resources.getString(LCMSResource.BAD_STRING_FORMAT));
                            }
                            break;
                        case 1: //Flags
                            if (matchesChar(c, SPECIFIERS))
                            {
                                //Found the end, exit
                                section = -1;
                                String str = bu.toString();
                                if (argList != null)
                                {
                                	for(int j = 0; j < 3; j++)
                                	{
                                		argList.addElement(null);
                                	}
                                    argList.addElement(c + "");
                                }
                                else
                                {
                                    //If we don't do this it will become redundent and we will get a stack overflow
                                    parts.addElement(FormatElement.getFormatter(str));
                                }
                                bu.setLength(0);
                                inFormat = false;
                            }
                            else if (matchesChar(c, FLAGS))
                            {
                                if (argList != null)
                                {
                                	argList.setElementAt(((String)argList.elementAt(0)) + c, 0);
                                }
                                continue; //Still looking at flag values
                            }
                            else if (matchesChar(c, WIDTH_PRECISION))
                            {
                                if (argList != null)
                                {
                                	argList.addElement(c + "");
                                }
                                section++; //Found width section, now to check for next section
                            }
                            else if (c == '.')
                            {
                                if (i + 1 < len)
                                {
                                    if (matchesChar(format.charAt(i + 1), WIDTH_PRECISION))
                                    {
                                        if (argList != null)
                                        {
                                            argList.addElement(null);
                                            argList.addElement(c + "");
                                        }
                                        section += 2; //Found precision section, now to check for next section
                                    }
                                    else if (!matchesChar(format.charAt(i + 1), SPECIFIERS))
                                    {
                                        throw new IllegalArgumentException(Utility.LCMS_Resources.getString(LCMSResource.BAD_STRING_FORMAT));
                                    }
                                }
                                else
                                {
                                    throw new IllegalArgumentException(Utility.LCMS_Resources.getString(LCMSResource.BAD_STRING_FORMAT));
                                }
                            }
                            else if (matchesChar(c, LENGTH))
                            {
                                if (argList != null)
                                {
                                	for(int j = 0; j < 2; j++)
                                	{
                                		argList.addElement(null);
                                	}
                                    argList.addElement(c + "");
                                }
                                section += 3; //Found length section, now to check for next section
                            }
                            else
                            {
                                throw new IllegalArgumentException(Utility.LCMS_Resources.getString(LCMSResource.BAD_STRING_FORMAT));
                            }
                            break;
                        case 2: //Width
                            if (matchesChar(c, SPECIFIERS))
                            {
                                //Found the end, exit
                                section = -1;
                                String str = bu.toString();
                                if (argList != null)
                                {
                                	for(int j = 0; j < 2; j++)
                                	{
                                		argList.addElement(null);
                                	}
                                    argList.addElement(c + "");
                                }
                                else
                                {
                                    //If we don't do this it will become redundent and we will get a stack overflow
                                    parts.addElement(FormatElement.getFormatter(str));
                                }
                                bu.setLength(0);
                                inFormat = false;
                            }
                            else if (matchesChar(c, WIDTH_PRECISION))
                            {
                                if (argList != null)
                                {
                                    argList.setElementAt(((String)argList.elementAt(1)) + c, 1);
                                }
                                continue; //Still looking at width values
                            }
                            else if (c == '.')
                            {
                                if (i + 1 < len)
                                {
                                    if (matchesChar(format.charAt(i + 1), WIDTH_PRECISION))
                                    {
                                        if (argList != null)
                                        {
                                            argList.addElement(c + "");
                                        }
                                        section++; //Found precision section, now to check for next section
                                    }
                                    else if (!matchesChar(c, SPECIFIERS))
                                    {
                                        throw new IllegalArgumentException(Utility.LCMS_Resources.getString(LCMSResource.BAD_STRING_FORMAT));
                                    }
                                }
                                else
                                {
                                    throw new IllegalArgumentException(Utility.LCMS_Resources.getString(LCMSResource.BAD_STRING_FORMAT));
                                }
                            }
                            else if (matchesChar(c, LENGTH))
                            {
                                if (argList != null)
                                {
                                    argList.addElement(null);
                                    argList.addElement(c + "");
                                }
                                section += 2; //Found length section, now to check for next section
                            }
                            else
                            {
                                throw new IllegalArgumentException(Utility.LCMS_Resources.getString(LCMSResource.BAD_STRING_FORMAT));
                            }
                            break;
                        case 3: //Precision
                            if (matchesChar(c, SPECIFIERS))
                            {
                                //Found the end, exit
                                section = -1;
                                String str = bu.toString();
                                if (argList != null)
                                {
                                    argList.addElement(null);
                                    argList.addElement(c + "");
                                }
                                else
                                {
                                    //If we don't do this it will become redundent and we will get a stack overflow
                                    parts.addElement(FormatElement.getFormatter(str));
                                }
                                bu.setLength(0);
                                inFormat = false;
                            }
                            else if (matchesChar(c, WIDTH_PRECISION))
                            {
                                if (argList != null)
                                {
                                    argList.setElementAt(((String)argList.elementAt(2)) + c, 2);
                                }
                                continue; //Still looking at precision values
                            }
                            else if (matchesChar(c, LENGTH))
                            {
                                if (argList != null)
                                {
                                    argList.addElement(c + "");
                                }
                                section++; //Found length section, now to check for next section
                            }
                            else
                            {
                                throw new IllegalArgumentException(Utility.LCMS_Resources.getString(LCMSResource.BAD_STRING_FORMAT));
                            }
                            break;
                        case 4: //Length
                            if (matchesChar(c, SPECIFIERS))
                            {
                                //Found the end, exit
                                section = -1;
                                String str = bu.toString();
                                if (argList != null)
                                {
                                    argList.addElement(c + "");
                                }
                                else
                                {
                                    //If we don't do this it will become redundant and we will get a stack overflow
                                    parts.addElement(FormatElement.getFormatter(str));
                                }
                                bu.setLength(0);
                                inFormat = false;
                            }
                            else
                            {
                                throw new IllegalArgumentException(Utility.LCMS_Resources.getString(LCMSResource.BAD_STRING_FORMAT));
                            }
                            break;
                    }
                }
                if (!inFormat && argList != null)
                {
                	String[] argListAr = new String[argList.size()];
                	argList.copyInto(argListAr);
                    args[0] = argListAr;
                }
            }
            else
            {
                if (c == '%')
                {
                    if (i + 1 < len)
                    {
                        if (format.charAt(i + 1) == '%')
                        {
                            i++;
                            bu.append('%');
                        }
                        else
                        {
                            inFormat = true;
                            if (bu.length() > 0)
                            {
                                parts.addElement(bu.toString());
                                bu.setLength(0);
                            }
                            bu.append('%');
                            section = 0; //Used to determine what part of the format is being checked
                        }
                    }
                    else
                    {
                        throw new IllegalArgumentException(Utility.LCMS_Resources.getString(LCMSResource.BAD_STRING_FORMAT));
                    }
                }
                else
                {
                    bu.append(c);
                }
            }
        }
        if (bu.length() > 0)
        {
            if (inFormat)
            {
                parts.addElement(FormatElement.getFormatter(bu.toString()));
            }
            else
            {
                parts.addElement(bu.toString());
            }
        }
        Object[] partsAr = new Object[parts.size()];
        parts.copyInto(partsAr);
        return partsAr;
	}
	
	private static boolean matchesChar(char c, String chars2match)
    {
        int len = chars2match.length();
        for (int i = 0; i < len; i++)
        {
            if (chars2match.charAt(i) == c)
            {
                return true;
            }
        }
        return false;
    }
	
	public static abstract class FormatElement
	{
		protected String format;
		
        protected FormatElement(String format)
        {
            this.format = format;
        }
        
        public abstract String format(Object obj);
        
        public abstract void unformat(String value, Object[] refO, int[] vals);
        
        public String getFormat()
        {
            return format;
        }
        
        public abstract void setInputValue(Long one, Long two);
        
        public abstract boolean takesArg();
        
        public abstract int requires();
        
        public abstract int argLocation();
        
        public abstract String getNullParamOutput();
        
        public abstract void reset();
        
        public boolean hasArgLocation()
        {
            return argLocation() >= 0;
        }
        
        public static FormatElement getFormatter(String form)
        {
            if (form.charAt(0) != '%')
            {
                return null;
            }
            switch (form.charAt(form.length() - 1))
            {
                case 'c': //Character
                case 's': //String
                    return new StringFormatElement(form);
                case 'd':
                case 'i': //Signed decimal integer
                case 'o': //Signed octal
                case 'x':
                case 'X': //Unsigned hexadecimal integer
                case 'u': //Unsigned decimal integer
                    return new IntFormatElement(form);
                case 'e':
                case 'E': //Scientific notation
                case 'g':
                case 'G': //Takes the smaller output of 'f' and 'e'/'E'
                case 'f':
                case 'F': //Decimal floating point
                    return new FloatFormatElement(form);
                case 'p': //Pointer address
                    return new PointerFormatElement(form);
            }
	        return new GenericFormatElement(form);
        }
        
        protected void argError(String formatter, String defValue, Class element)
        {
        	System.err.println(formatter + Utility.LCMS_Resources.getString(LCMSResource.PRINTUTIL_UNK_ARG) + defValue + ". Arg:" + element);
        }
        
        public String ToString()
        {
            return format;
        }
	}
	
	private static abstract class GeneralFormatElement extends FormatElement
    {
        private boolean arg, lengthDoubleSize;
        protected String flags;
        protected char length, type;
        protected int width, precision, requiresInput, argPos;
        
        public GeneralFormatElement(String format)
        {
        	super(format);
        	reset();
            this.arg = true; //Not sure why this should be included but could be useful in the future or depending on implementation.
            parseFormat();
        }
        
        public void reset()
        {
        	this.precision = -1;
            this.width = -1;
        }
        
        private void parseFormat()
        {
            String[][] parts = new String[1][];
            PrintUtility.breakFormat(this.format, parts);
            String[] elements = parts[0];
            int pos = 0;
            if (elements.length == 6)
            {
                pos++;
                argPos = Integer.parseInt(elements[0]) - 1;
            }
            else
            {
                argPos = -1;
            }
            if (elements[pos++] != null)
            {
                //Flags
                this.flags = elements[pos - 1];
            }
            if (elements[pos++] != null)
            {
                //Width
                String el = elements[pos - 1];
                int loc;
                if ((loc = el.indexOf('*')) >= 0)
                {
                    requiresInput = 1;
                }
                if (el.length() > loc + 1)
                {
                    width = Integer.parseInt(loc >= 0 ? el.substring(loc + 1) : el);
                }
            }
            if (elements[pos++] != null)
            {
                //Precision
                String el = elements[pos - 1];
                if (el.indexOf('*') >= 0)
                {
                    requiresInput++;
                    if (requiresInput == 1)
                    {
                        //No first element, need to make sure only second element is retrieved
                        requiresInput |= 1 << 2;
                    }
                }
                else
                {
                    precision = Integer.parseInt(el.substring(1));
                }
            }
            if (elements[pos++] != null)
            {
                //Length
                String el = elements[pos - 1];
                char c1 = el.charAt(0);
                if (el.length() > 1)
                {
                	char c2 = el.charAt(1);
                    if (c1 != c2 || (c1 != 'h' || c1 != 'l'))
                    {
                    	throw new IllegalArgumentException(Utility.LCMS_Resources.getString(LCMSResource.BAD_STRING_FORMAT));
                    }
                    this.lengthDoubleSize = true;
                }
                this.length = c1;
            }
            type = elements[pos].charAt(0);
        }

        public String format(Object obj)
        {
            String str = inFormat(obj);
            if (flags != null)
            {
                if (flags.indexOf('-') >= 0)
                {
                    if (flags.indexOf('0') >= 0)
                    {
                    	throw new IllegalArgumentException(Utility.LCMS_Resources.getString(LCMSResource.BAD_STRING_FORMAT));
                    }
                    //Left align
                    if (str.length() < width)
                    {
                    	char[] chars = new char[width - str.length()];
                    	Arrays.fill(chars, ' ');
                        str += new String(chars);
                    }
                }
                else if (flags.indexOf('0') >= 0)
                {
                    if (str.length() < width)
                    {
                    	char[] chars = new char[width - str.length()];
                    	Arrays.fill(chars, '0');
                        str = new String(chars) + str;
                    }
                }
            }
            else
            {
                if (str.length() < width)
                {
                	char[] chars = new char[width - str.length()];
                	Arrays.fill(chars, ' ');
                    str = new String(chars) + str;
                }
            }
            return str;
        }
        
        public abstract String inFormat(Object obj);

        public void setInputValue(Long one, Long two)
        {
            if (one != null)
            {
                if ((requiresInput & (1 << 2)) != 0)
                {
                    precision = (int)one.longValue();
                }
                else
                {
                    width = (int)one.longValue();
                }
            }
            if (two != null)
            {
                precision = (int)two.longValue();
            }
        }

        public boolean takesArg()
        {
            return arg;
        }

        public int requires()
        {
            return requiresInput & 3;
        }

        public int argLocation()
        {
            return argPos;
        }
    }
	
	private static class GenericFormatElement extends GeneralFormatElement
    {
        public GenericFormatElement(String format)
        {
        	super(format);
        }

        public String inFormat(Object obj)
        {
        	argError("GenericFormat", "$format", obj.getClass());
            return this.format;
        }
        
        public String getNullParamOutput()
        {
        	return inFormat(null);
        }

        public void unformat(String value, Object[] refO, int[] vals)
        {
            throw new UnsupportedOperationException();
        }
    }
	
	private static class StringFormatElement extends GeneralFormatElement
    {
        public StringFormatElement(String format)
        {
        	super(format);
        }

        public String inFormat(Object obj)
        {
            boolean charType = this.type == 'c';
            String str = null;
            if (obj instanceof String)
            {
                str = (String)obj;
            }
            else if (obj instanceof StringBuffer)
            {
                str = ((StringBuffer)obj).toString();
                int len = Utility.strlen(str);
                if(len != str.length())
                {
                	str = str.substring(0, len);
                }
            }
            if (str == null)
            {
                if (obj instanceof char[])
                {
                    if (this.length == 'l')
                    {
                        str = new String((char[])obj, 0, Utility.strlen((char[])obj));
                    }
                    else
                    {
                        char[] chars = (char[])obj;
                        int len;
                        byte[] nBytes = new byte[len = charType ? 1 : Utility.strlen(chars)];
                        for (int i = 0; i < len; i++)
                        {
                            nBytes[i] = (byte)chars[i];
                        }
                        str = new String(nBytes);
                    }
                }
                else if (obj instanceof Character)
                {
                	char c = ((Character)obj).charValue();
                	if(this.length != 'l')
                	{
                		c = (char)(c & 0xFF);
                	}
                    str = c + "";
                }
                else if (obj instanceof Byte || obj instanceof Short || obj instanceof Integer || obj instanceof Long)
                {
                    long val;
                    int mask = this.length == 'l' ? 0xFFFF : 0xFF;
                    if (obj instanceof Byte)
                    {
                        val = ((Byte)obj).byteValue() & mask;
                    }
                    else if (obj instanceof Short)
                    {
                        val = ((Short)obj).shortValue() & mask;
                    }
                    else if (obj instanceof Integer)
                    {
                        val = ((Integer)obj).intValue() & mask;
                    }
                    else
                    {
                        val = ((Long)obj).longValue() & mask;
                    }
                    str = ((char)val) + "";
                }
                else if (obj instanceof VirtualPointer)
                {
                	VirtualPointer.TypeProcessor proc = ((VirtualPointer)obj).getProcessor();
                	char c = this.length != 'l' ? ((char)proc.readInt8()) : proc.readChar();
                	str = c + "";
                }
                else
                {
                	argError("StringFormat", "obj.toString()", obj.getClass());
                    str = obj.toString(); //This will return ASCII
                }
            }
            else if (this.length != 'l')
            {
                char[] chars = str.toCharArray();
                int len;
                byte[] nBytes = new byte[len = charType ? 1 : chars.length];
                for (int i = 0; i < len; i++)
                {
                    nBytes[i] = (byte)chars[i];
                }
                str = new String(nBytes);
            }
            if (charType)
            {
                if (str.length() > 1)
                {
                    str = str.substring(0, 1);
                }
            }
            else if (this.precision >= 0 && this.type != 'c')
            {
                if (str.length() > this.precision)
                {
                    str = str.substring(0, this.precision);
                }
            }
            return str;
        }
        
        public String getNullParamOutput()
        {
        	return this.type == 'c' ? "\0" : "(null)";
        }

        public void unformat(String value, Object[] refO, int[] vals)
        {
        	int w = this.width;
            if (w < 0)
            {
                w = 1;
            }
            int len = value.length();
            int org = vals[0];
            if (this.type == 'c')
            {
                char[] items = new char[w];
                value.getChars(org, org + w, items, 0);
                vals[0] += w;
                int t = vals[1] + w;
                if (this.requiresInput == 1)
                {
                    return;
                }
                for (int i = vals[1], e = 0; i < t; i++, e++)
                {
                    vals[1]++;
                    Object obj = refO[i];
                    if (obj == null || !obj.getClass().isArray())
                    {
                    	if(obj != null)
                    	{
                    		if(!(obj instanceof VirtualPointer || obj instanceof StringBuffer))
                    		{
                    			return;
                    		}
                    	}
                		else
                		{
                			return;
                		}
                    }
                    if (obj instanceof char[])
                    {
                        ((char[])obj)[0] = items[e];
                    }
                    else if(obj instanceof byte[])
                    {
                    	((byte[])obj)[0] = (byte)items[e];
                    }
                    else if(obj instanceof short[])
                    {
                    	((short[])obj)[0] = (short)items[e];
                    }
                    else if(obj instanceof int[])
                    {
                    	((int[])obj)[0] = items[e];
                    }
                    else if(obj instanceof long[])
                    {
                    	((long[])obj)[0] = items[e];
                    }
                    else if(obj instanceof StringBuffer)
                    {
                    	StringBuffer buf = (StringBuffer)obj;
                    	if(buf.length() == buf.capacity())
                    	{
                    		buf.setCharAt(0, items[e]);
                    	}
                    	else
                    	{
                    		buf.append(items[e]);
                    	}
                    }
                    else if(obj instanceof VirtualPointer)
                    {
                    	VirtualPointer.TypeProcessor proc = ((VirtualPointer)obj).getProcessor();
                    	proc.write(items[e]);
                    }
                    else
                    {
                    	argError("StringFormat", "null", obj.getClass());
                    }
                }
            }
            else
            {
                for (w = 0; w < len; w++)
                {
                    if (PrintUtility.isWhiteSpace(value.charAt(org + w)))
                    {
                        break;
                    }
                }
                if (this.width < w && this.width != -1)
                {
                    w = this.width;
                }
                vals[0] += w;
                if (this.requiresInput == 1)
                {
                    return; //Skip argument
                }
                Object obj = refO[vals[1]];
                vals[1]++;
                if (obj == null || !obj.getClass().isArray())
                {
                	if(obj != null)
                	{
                		if(!(obj instanceof VirtualPointer || obj instanceof StringBuffer))
                		{
                			return;
                		}
                	}
            		else
            		{
            			return;
            		}
                }
                String sVal = value.substring(org, org + w);
                if (obj instanceof String[])
                {
                    ((String[])refO)[0] = sVal;
                }
                else if (obj instanceof char[])
                {
                    System.arraycopy(sVal.toCharArray(), 0, (char[])obj, 0, w);
                }
                else if(obj instanceof byte[])
                {
                	System.arraycopy(sVal.getBytes(), 0, (byte[])obj, 0, w);
                }
                else if(obj instanceof short[])
                {
                	short[] sh = (short[])obj;
                	char[] ch = sVal.toCharArray();
                	for(int i = 0; i < w; i++)
                	{
                		sh[i] = (short)ch[i];
                	}
                }
                else if(obj instanceof StringBuffer)
                {
                	StringBuffer buf = (StringBuffer)obj;
                	if(buf.length() == buf.capacity())
                	{
                		Utility.strncpy(buf, sVal, w);
                	}
                	else
                	{
                		buf.append(sVal);
                	}
                }
                else if(obj instanceof VirtualPointer)
                {
                	VirtualPointer.TypeProcessor proc = ((VirtualPointer)obj).getProcessor();
                	proc.write(sVal);
                }
                else
                {
                	argError("StringFormat", "null", obj.getClass());
                }
            }
        }
    }
	
	private static class IntFormatElement extends GeneralFormatElement
    {
        private boolean signed;
        private boolean basicType;

        public IntFormatElement(String format)
        {
        	super(format);
            switch (this.type)
            {
                default:
                case 'd':
                case 'i':
                    //Signed decimal integer
                    signed = true;
                    basicType = true;
                    break;
                case 'o':
                    //Signed octal
                    signed = true;
                    basicType = false;
                    break;
                case 'x':
                case 'X':
                    //Unsigned hexadecimal integer
                    signed = false;
                    basicType = false;
                    break;
                case 'u':
                    //Unsigned decimal integer
                    signed = false;
                    basicType = true;
                    break;
            }
        }

        public String inFormat(Object obj)
        {
            StringBuffer bu = new StringBuffer();
            long value = 0;
            switch(this.length)
        	{
        		default:
        			if(obj instanceof Byte)
                	{
                		value = ((Byte)obj).byteValue();
                	}
                	else if(obj instanceof Short)
                	{
                		value = ((Short)obj).shortValue();
                	}
                	else if(obj instanceof Integer)
                	{
                		value = ((Integer)obj).intValue();
                	}
                	else if(obj instanceof Long)
                	{
                		value = ((Long)obj).longValue();
                	}
                	else if(obj instanceof Float)
                	{
                		value = Float.floatToIntBits(((Float)obj).floatValue());
                	}
                	else if(obj instanceof Double)
                	{
                		value = Double.doubleToLongBits(((Double)obj).doubleValue());
                	}
                	else if(obj instanceof VirtualPointer)
                	{
                		value = ((VirtualPointer)obj).getProcessor().readInt32();
                	}
                	else
                	{
                		argError("IntFormat", "0", obj.getClass());
                		value = 0;
                	}
        			break;
        		case 'h':
        			if(super.lengthDoubleSize)
        			{
        				value = ((Character)obj).charValue();
        			}
        			else
        			{
        				value = ((Short)obj).shortValue();
        			}
        			break;
        		case 'z':
        		case 'j':
        		case 't':
        			value = ((Integer)obj).intValue();
        			break;
        		case 'l':
        			if(super.lengthDoubleSize)
        			{
        				value = ((Long)obj).longValue();
        			}
        			else
        			{
        				value = ((Integer)obj).longValue();
        			}
        			break;
        	}
            if (this.flags != null)
            {
                if (value >= 0)
                {
                    if (flags.indexOf(' ') >= 0)
                    {
                        bu.append(' ');
                    }
                    else if (flags.indexOf('+') >= 0)
                    {
                        bu.append('+');
                    }
                }
            }
            String str;
            if (signed)
            {
                if (basicType)
                {
                    str = Long.toString(value);
                    if (str.length() < this.length)
                    {
                    	char[] chars = new char[this.length - str.length()];
                    	Arrays.fill(chars, '0');
                        bu.append(chars);
                    }
                    bu.append(str);
                }
                else
                {
                	str = Long.toString(value, 8);
                    if (flags != null && flags.indexOf('#') >= 0 && value != 0)
                    {
                        bu.append('0');
                    }
                    if (str.length() + bu.length() < this.length)
                    {
                    	char[] chars = new char[(this.length + bu.length()) - str.length()];
                    	Arrays.fill(chars, '0');
                        bu.append(chars);
                    }
                    bu.append(str);
                }
            }
            else
            {
                if (basicType)
                {
                	str = ulongToString(value);
                	if (str.length() < this.length)
                    {
                    	char[] chars = new char[this.length - str.length()];
                    	Arrays.fill(chars, '0');
                        bu.append(chars);
                    }
                    bu.append(str);
                }
                else
                {
                	str = Long.toString(value, 16); //TODO: Double check this, if the number is negative it prints a negative sign. Should not do that.
                    if (flags != null && flags.indexOf('#') >= 0 && value != 0)
                    {
                        bu.append('0');
                        bu.append(this.type);
                    }
                    if (str.length() + bu.length() < this.length)
                    {
                    	char[] chars = new char[(this.length + bu.length()) - str.length()];
                    	Arrays.fill(chars, '0');
                        bu.append(chars);
                    }
                    bu.append(str);
                }
            }
            return bu.toString();
        }
        
        private String ulongToString(long _val)
        {
        	//Not very efficient but works well
        	StringBuffer bu = new StringBuffer();
            if ((_val & 0x8000000000000000L) != 0)
            {
            	_val = _val & 0x7FFFFFFFFFFFFFFFL;
                if (_val != 0L)
                {
                	long value = _val;
                    //Get the value as an array
                    byte[] result = new byte[] { 0, 9, 2, 2, 3, 3, 7, 2, 0, 3, 6, 8, 5, 4, 7, 7, 5, 8, 0, 8 };
                    
                    byte[] addValues = new byte[19];
                    int i = 19;
                    while (value != 0)
                    {
                        addValues[--i] = (byte)(value % 10);
                        value /= 10;
                    }
                    
                    //Add "addValues" to result
                    int a = 18;
                    int r = 19;
                    int rem = 0;
                    for (int k = 19; k >= 0; k--, r--, a--)
                    {
                        byte add = (byte)(a >= i ? addValues[a] : 0);
                        byte val = (byte)(result[r] + add + rem);
                        rem = 0;
                        if (val >= 10)
                        {
                            rem = 1;
                            val -= 10;
                        }
                        result[r] = val;
                    }
                    
                    //Convert array to string
                    boolean writing = false;
                    for (int k = 0; k < 20; k++)
                    {
                        if (writing)
                        {
                            bu.append(result[k]);
                        }
                        else if (result[k] != 0)
                        {
                            writing = true;
                            k--;
                        }
                    }
                }
                else
                {
                    bu.append("9223372036854775808");
                }
            }
            else
            {
                //Simply write the value
                bu.append(_val);
            }
            return bu.toString();
        }
        
        public String getNullParamOutput()
        {
        	return "0";
        }
        
        public void unformat(String value, Object[] refO, int[] vals)
        {
        	int len = value.length();
            int org = vals[0];
            int w;
            boolean sign = false;
            char c;
            char type = this.type;
            boolean basicType = this.basicType;
            boolean signed = this.signed;
            for (w = 0; w < len; w++)
            {
                c = value.charAt(org + w);
                if (!sign)
                {
                    if (Character.isDigit(c))
                    {
                        sign = true;
                        if (type == 'i' && c == '0')
                        {
                            //We might be on to something
                            if (w + 1 < len)
                            {
                                if (value.charAt(org + w + 1) == 'x')
                                {
                                    //A ha, hex
                                    type = 'Z'; //Do this to trick the rest of the parser into reading hex
                                    basicType = false;
                                    if (signed && c == '-')
                                    {
                                        //Hmm, if this is a hex integer it can't be negative
                                        return;
                                    }
                                    signed = false;
                                    w++;
                                }
                                else
                                {
                                    //Since it wasn't hex then it must be octal
                                    type = 'o';
                                    basicType = false;
                                    signed = true;
                                }
                            }
                        }
                        else
                        {
                            w--;
                        }
                    }
                    else if (c == '+' || (signed && c == '-'))
                    {
                        sign = true;
                        if (type == 'i')
                        {
                            //Do a quick check to see what the next value is
                            if (w + 1 < len)
                            {
                                if (value.charAt(org + w + 1) == '0')
                                {
                                    w++;
                                    //We might be on to something
                                    if (w + 1 < len)
                                    {
                                        if (value.charAt(org + w + 1) == 'x')
                                        {
                                            //A ha, hex
                                            type = 'Z'; //Do this to trick the rest of the parser into reading hex
                                            basicType = false;
                                            if (signed && c == '-')
                                            {
                                                //Hmm, if this is a hex integer it can't be negative
                                                return;
                                            }
                                            signed = false;
                                            w++;
                                        }
                                        else
                                        {
                                            //Since it wasn't hex then it must be octal
                                            type = 'o';
                                            basicType = false;
                                            signed = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        return;
                    }
                }
                else
                {
                    if (Character.isDigit(c))
                    {
                        if (!basicType && signed && c > '7')
                        {
                            //Octal
                            break;
                        }
                        continue;
                    }
                    else if (!basicType)
                    {
                        if (!signed)
                        {
                            //Hex
                            if (type <= 'X')
                            {
                                //Upper case
                                if (c >= 'A' && c <= 'F')
                                {
                                    continue;
                                }
                            }
                            else if (type <= 'x')
                            {
                                //Lower case
                                if (c >= 'a' && c <= 'f')
                                {
                                    continue;
                                }
                            }
                            else if (type == 'Z')
                            {
                                //Dynamic hex
                                if ((c >= 'A' && c <= 'F') && (c >= 'a' && c <= 'f'))
                                {
                                    continue;
                                }
                            }
                        }
                    }
                    break;
                }
            }
            vals[0] += w;
            if (this.requiresInput == 1)
            {
                return;
            }
            String stval = value.substring(org, org + w);
            short sval = 0;
            int ival = 0;
            long lval = 0;
            try
            {
                //TODO, do some preliminary size checks, if a number is 800 chars it's not going to get parsed (unless every digit is a zero or something similar)
                switch (type)
                {
                    case 'd':
                    case 'i':
                        //Signed decimal integer
                        switch (this.length)
                        {
                            default:
                            	ival = Integer.parseInt(stval);
                                break;
                            case 'h':
                                sval = Short.parseShort(stval);
                                break;
                            case 'l':
                                lval = Long.parseLong(stval);
                                break;
                        }
                        break;
                    case 'o':
                        //Signed octal
                    	switch (this.length)
                        {
                            default:
                            	ival = Integer.parseInt(stval, 8);
                                break;
                            case 'h':
                                sval = Short.parseShort(stval, 8);
                                break;
                            case 'l':
                                lval = Long.parseLong(stval, 8);
                                break;
                        }
                        break;
                        
                    case 'x':
                    case 'X':
                    case 'Z': //Small hack
                        //Unsigned hexadecimal integer
                    	switch (this.length)
                        {
                    	//TODO
                            default:
                            	ival = Integer.parseInt(stval, 16);
                                break;
                            case 'h':
                                sval = Short.parseShort(stval, 16);
                                break;
                            case 'l':
                                lval = Long.parseLong(stval, 16);
                                break;
                        }
                        break;
                    case 'u':
                        //Unsigned decimal integer
                    	switch (this.length)
                        {
                            default:
                            	ival = (int)(Long.parseLong(stval) & 0xFFFFFFFF);
                                break;
                            case 'h':
                                sval = (short)(Integer.parseInt(stval) & 0xFFFF);
                                break;
                            case 'l':
                            	//TODO
                                lval = Long.parseLong(stval);
                                break;
                        }
                        break;
                }
            }
            catch (NumberFormatException e)
            {
                return;
            }
            Object obj = refO[vals[1]];
            if (obj == null || !obj.getClass().isArray())
            {
            	if(obj != null)
            	{
            		if(!(obj instanceof VirtualPointer))
            		{
            			return;
            		}
            	}
            	else
            	{
            		return;
            	}
            }
            vals[1]++;
            boolean written = false;
            switch (this.length)
            {
                default:
                    if (obj instanceof int[])
                    {
                        written = true;
                        ((int[])obj)[0] = ival;
                    }
                    else if(obj instanceof VirtualPointer)
                    {
                    	written = true;
                    	((VirtualPointer)obj).getProcessor().write(ival);
                    }
                    break;
                case 'h':
                    if (obj instanceof short[])
                    {
                        written = true;
                        ((short[])obj)[0] = sval;
                    }
                    else if(obj instanceof VirtualPointer)
                    {
                    	written = true;
                    	((VirtualPointer)obj).getProcessor().write(sval);
                    }
                    break;
                case 'l':
                    if (obj instanceof long[])
                    {
                        written = true;
                        ((long[])obj)[0] = lval;
                    }
                    else if(obj instanceof VirtualPointer)
                    {
                    	written = true;
                    	((VirtualPointer)obj).getProcessor().write(lval);
                    }
                    break;
            }
            if (!written)
            {
                //Error
                vals[1]--;
                vals[0] -= w;
            }
        }
    }
	
	private static class FloatFormatElement extends GeneralFormatElement
    {
		private static final int DEFAULT_PRECISION = 6; //Nearly every reference says 6 except www.cplusplus.com
		
		private static char decimalPoint;
		
		static
		{
			decimalPoint = Double.toString(1.1).charAt(1);
		}
		
        public FloatFormatElement(String format)
        {
        	super(format);
        }
        
        public String inFormat(Object obj)
        {
        	double value = 0;
            if (this.length == 'L')
            {
                value = Double.longBitsToDouble(((Long)obj).longValue());
            }
            else
            {
            	if (obj instanceof Float)
	            {
	                value = ((Float)obj).doubleValue();
	            }
	            else if(obj instanceof Double)
	            {
	                value = ((Double)obj).doubleValue();
	            }
	            else if (obj instanceof VirtualPointer)
	            {
	            	value = ((VirtualPointer)obj).getProcessor().readDouble();
	            }
	            else
	            {
	            	argError("FloatFormat", "0", obj.getClass());
	        		value = 0;
	            }
            }
            
            //Determine what to do
            StringBuffer fixedBuf = null;
            StringBuffer expBuf = null;
            boolean caps = type <= 'Z';
            boolean alt = this.flags != null && this.flags.indexOf('#') >= 0;
            if (type == 'g' || type == 'G')
            {
                fixedBuf = new StringBuffer();
                expBuf = new StringBuffer();
            }
            else if (type == 'f' || type == 'F')
            {
                fixedBuf = new StringBuffer();
            }
            else if (type == 'e' || type == 'E')
            {
                expBuf = new StringBuffer();
            }
            
            //To string for the specified type
            if (fixedBuf != null)
            {
            	if (this.flags != null)
                {
                    if (value >= 0)
                    {
                        if (flags.indexOf(' ') >= 0)
                        {
                        	fixedBuf.append(' ');
                        }
                        else if (flags.indexOf('+') >= 0)
                        {
                        	fixedBuf.append('+');
                        }
                    }
                }
                writeFloatingPoint(fixedBuf, value, this.precision);
            }
            if (expBuf != null)
            {
                if (fixedBuf == null)
                {
                	if (this.flags != null)
                    {
                        if (value >= 0)
                        {
                            if (flags.indexOf(' ') >= 0)
                            {
                            	expBuf.append(' ');
                            }
                            else if (flags.indexOf('+') >= 0)
                            {
                            	expBuf.append('+');
                            }
                        }
                    }
                    writeFloatingPoint(expBuf, value, this.precision);
                }
                else
                {
                    //expBuf.append(fixedBuf.toString());
                	StringUtilities.append(expBuf, fixedBuf);
                }
                
                String temp = expBuf.toString();
                int tlen = temp.length();
            	int index = temp.indexOf(decimalPoint);
            	
            	//Get the exponent
            	int exp = 0;
            	int off = Character.isDigit(temp.charAt(0)) ? 0 : 1;
            	int i;
            	if(index == 1 + off)
            	{
            		//Either don't adjust decimal or move it backwards
            		if(temp.charAt(off) != '0')
            		{
            			//Ok, move decimal backwards
            			for(i = index + 1; i < tlen; i++)
            			{
            				if(temp.charAt(i) != '0')
            				{
            					exp = i - index;
            					break;
            				}
            			}
            		}
            	}
            	else
            	{
            		//Move decimal forwards
            		for(i = off; i < index; i++)
        			{
            			if(temp.charAt(i) != '0')
        				{
        					exp = index - i - 1;
        					break;
        				}
        			}
            	}
            	
            	//Start writing out the value
            	expBuf.setLength(0); //Reset the StringBuffer
            	if(off == 1)
            	{
            		//If a formatter or sign was added then make sure the final value has it too.
            		expBuf.append(temp.charAt(0));
            	}
            	StringUtilities.append(expBuf, temp, off, index - off); //Copy whole number
            	StringUtilities.append(expBuf, temp, index + 1, tlen - ((index * 2) + 1)); //Copy decimal (subtracting what are now extra precision chars)
            	//Trim zeros before finishing formatting
            	for(i = tlen - 1; i > off; i--)
        		{
        			if(temp.charAt(i) != '0')
        			{
        				break;
        			}
        		}
        		if(((i - 1) == off) && (this.precision == -1))
        		{
        			//Only remove zeros if it is an integer and the precision isn't predefined
        			if(alt)
        			{
        				//Adjust
        				expBuf.delete(off + 2, expBuf.length());
        				expBuf.insert(off + 1, decimalPoint); //Insert the decimal point
        			}
        			else
        			{
        				expBuf.delete(off + 1, expBuf.length());
        			}
        		}
        		else
        		{
        			expBuf.insert(off + 1, decimalPoint); //Insert the decimal point
        		}
            	
            	//Now write out the exponent
            	expBuf.append('e');
            	expBuf.append(exp < 0 ? '-' : '+');
            	exp = Math.abs(exp);
            	if(exp < 10)
            	{
            		//Must be at least 2 digits for the exponent
            		expBuf.append('0');
            	}
            	expBuf.append(exp);
            }
            if (fixedBuf != null)
            {
            	String temp = fixedBuf.toString();
            	int index = temp.indexOf(decimalPoint);
            	
            	//Trim the decimal if possible
        		int len = temp.length();
        		int i;
        		for(i = index + 1; i < len; i++)
        		{
        			if(temp.charAt(i) != '0')
        			{
        				break;
        			}
        		}
        		if(((i - 1) == (len - 1)) && (this.precision == -1))
        		{
        			//Only remove zeros if it is an integer and the precision isn't predefined
        			if(alt)
        			{
        				fixedBuf.delete(index + 2, len);
        			}
        			else
        			{
        				fixedBuf.delete(index, len);
        			}
        		}
            }
            
            //Return the correct type
            /*
             * f: fixed-point, if no decimal portion [integer] exists then trailing zeros and no decimal point exists (alt: decimal point always used)
             * e: exponent form, always 2 digits in exponent, if no decimal portion [integer] exists then trailing zeros and no decimal point exists (alt: decimal point always used)
             * g: smaller of either f/e, trailing zeros removed, if integer then decimal point removed (alt: trailing zeros not removed, always has decimal point)
             */
            String result;
            if (type == 'g' || type == 'G')
            {
            	//TODO: Convert fixed and exp. buffers to general format
            	
            	//Compare length. Shortest one is returned.
                StringBuffer builder = fixedBuf.length() < expBuf.length() ? fixedBuf : expBuf;
                result = builder.toString();
            }
            else
            {
                result = fixedBuf != null ? fixedBuf.toString() : expBuf.toString();
            }
            if (caps)
            {
                result = result.toUpperCase();
            }
            return result;
        }
        
        private static void writeFloatingPoint(StringBuffer buf, double v, int p)
        {
            if (Double.isNaN(v) || Double.isInfinite(v))
            {
                if (Double.isNaN(v))
                {
                    buf.append("nan");
                }
                else if (v == Double.NEGATIVE_INFINITY)
                {
                    buf.append("-inf");
                }
                else
                {
                    buf.append("inf");
                }
                return;
            }
            long temp = Double.doubleToLongBits(v);
            int exp = ((int)((temp & 0x7FF0000000000000L) >>> 52)) - 1023;
            if((temp & 0x8000000000000000L) == 0x8000000000000000L)
            {
            	buf.append('-');
            }
            long mantissa = (temp & ((1L << 52) - 1)) | (exp == -1023 ? 0 : (1L << 52));
            exp -= 52;
            if (p < 0)
            {
                p = DEFAULT_PRECISION;
            }
            
            if (exp == 0 || mantissa == 0)
            {
                buf.append(mantissa);
            }
            else
            {
                if (Math.abs(exp) >= 63)
                {
                    if (exp > 0)
                    {
                        //Create the floating point value by multiplication (mantissa * 2^exp), using really big numbers
                        //-Precision is not needed since it only covers decimal point
                        new LargeNumber(mantissa).multiply(new LargeNumber(exp)).toString(buf);
                        buf.append(decimalPoint);
                        char[] chars = new char[p];
                        Arrays.fill(chars, '0');
                        buf.append(chars);
                        
                        /*
                        double value = mantissa;
                        for (int i = exp; i > 0; i--)
                        {
                            value *= 2;
                        }
                         */
                    }
                    else
                    {
                        //Create the floating point value by division (mantissa / 2^exp), using really big numbers
                        LargeNumber man = new LargeNumber(mantissa);
                        LargeNumber exponent = new LargeNumber(exp);
                        
                        LargeNumber ten = new LargeNumber(10L);
                        LargeNumber rem;
                        LargeNumber[] mod = null;
                        //Whole number
                        if (man.greaterThenOrEqual(exponent))
                        {
                            if (mod == null)
                            {
                                mod = new LargeNumber[1];
                            }
                            man.divideAndMod(exponent, mod).toString(buf);
                            rem = mod[0].multiply(ten);
                        }
                        else
                        {
                            buf.append('0');
                            rem = man.multiply(ten);
                        }
                        //Decimal point
                        if (!rem.zero() && p >= 0)
                        {
                            buf.append(decimalPoint);
                            while (p-- >= 0)
                            {
                                if (rem.greaterThenOrEqual(exponent))
                                {
                                    if (mod == null)
                                    {
                                        mod = new LargeNumber[1];
                                    }
                                    rem.divideAndMod(exponent, mod).toString(buf);
                                    if(p >= 0)
                                    {
                                    	rem = mod[0].multiply(ten);
                                    }
                                }
                                else
                                {
                                    buf.append('0');
                                    if(p >= 0 && !rem.zero())
                                    {
                                    	rem = rem.multiply(ten);
                                    }
                                }
                            }
                        }
                        else
                        {
                        	buf.append(decimalPoint);
                        	char[] chars = new char[p];
                            Arrays.fill(chars, '0');
                            buf.append(chars);
                        }
                        
                        //Round the value to the specified precision
                        if(!rem.zero()) //Do these zero checks to avoid unnecessary memory allocation and execution
                        {
                        	if (rem.greaterThenOrEqual(exponent))
                        	{
	                        	int carry = (int)((rem.divide(exponent).longValue() + 1) / 10); //Rough rounding
	                        	String temps = buf.toString();
		                        for(int i = temps.length() - 1; i >= 0; i--)
		                        {
		                        	if(carry == 0)
		                        	{
		                        		break;
		                        	}
		                        	char c = temps.charAt(i);
		                        	if(c == decimalPoint)
		                        	{
		                        		continue;
		                        	}
		                        	int tempV = (c - '0') + carry;
		                        	carry = 0;
		                        	if(tempV > 9)
		                        	{
		                        		buf.setCharAt(i, (char)((tempV % 10) + '0'));
		                        		carry = tempV / 10;
		                        	}
		                        	else
		                        	{
		                        		buf.setCharAt(i, (char)(tempV + '0'));
		                        	}
		                        }
                        	}
                        }
                        
                        /*
                        double value = mantissa;
                        for (int i = exp; i < 0; i++)
                        {
                            value /= 2;
                        }
                         */
                    }
                }
                else
                {
                    //Built in
                	/*
//#ifndef BlackBerrySDK4.5.0
                    long twoPower = (long)MathUtilities.pow(2, Math.abs(exp)); //Bit shifting more efficient
//#else
                    long twoPower = (long)Utility.pow(2, Math.abs(exp)); //Bit shifting more efficient
//#endif
                     */
                	
                    //Ints only
                	long twoPower = 1L << Math.abs(exp);
                    if (exp > 0)
                    {
                        long result = mantissa * twoPower;
                        if (result < 0) //Possible highest exponent before overflow occurs: 10 (0x1FFFFFFFFFFFFF * 2^10 = 0x7FFFFFFFFFFFFC00, 0x3FF diff before overflow. 0x1FFFFFFFFFFFFF * 2^11 = 1844674407370954956, which is overflow for a signed long)
                        {
                            //Overflow
                            new LargeNumber(mantissa).multiply(new LargeNumber(exp)).toString(buf);
                        }
                        else
                        {
                            buf.append(result);
                        }
                        buf.append(decimalPoint);
                        char[] chars = new char[p];
                        Arrays.fill(chars, '0');
                        buf.append(chars);
                    }
                    else
                    {
                        //Create the floating point value by division (mantissa / 2^exp)
                        //-Make the whole number
                        buf.append(mantissa / twoPower);
                        long rem = (mantissa % twoPower) * 10;
                        //Now calculate the decimal portion if necessary
                        if (rem != 0 && p >= 0)
                        {
                            buf.append(decimalPoint);
                            while (p-- >= 0) //Precision only affects the decimal portion so calculate until the number precision digits is met
                            {
                                buf.append(rem / twoPower);
                                if(p >= 0 && rem != 0)
                                {
                                	rem = (rem % twoPower) * 10;
                                }
                            }
                        }
                        else
                        {
                        	buf.append(decimalPoint);
                        	char[] chars = new char[p];
                            Arrays.fill(chars, '0');
                            buf.append(chars);
                        }
                        
                        //Round the value to the specified precision
                        if(rem != 0) //Do these zero checks to avoid unnecessary memory allocation and execution
                        {
	                        int carry = (int)(((rem / twoPower) + 1) / 10); //Rough rounding
	                        if(carry != 0)
	                        {
		                        String temps = buf.toString();
		                        for(int i = temps.length() - 1; i >= 0; i--)
		                        {
		                        	if(carry == 0)
		                        	{
		                        		break;
		                        	}
		                        	char c = temps.charAt(i);
		                        	if(c == decimalPoint)
		                        	{
		                        		continue;
		                        	}
		                        	int tempV = (c - '0') + carry;
		                        	carry = 0;
		                        	if(tempV > 9)
		                        	{
		                        		buf.setCharAt(i, (char)((tempV % 10) + '0'));
		                        		carry = tempV / 10;
		                        	}
		                        	else
		                        	{
		                        		buf.setCharAt(i, (char)(tempV + '0'));
		                        	}
		                        }
	                        }
                        }
                    }
                }
            }
        }
        
        public String getNullParamOutput()
        {
        	switch(this.type)
        	{
        		default:
	        	case 'f':
	        	case 'F':
	        	case 'g':
	        	case 'G':
	        		return "0";
	        	case 'e':
	        		return "0e+00";
	        	case 'E':
	        		return "0E+00";
        	}
        }

        public void unformat(String value, Object[] refO, int[] vals)
        {
        	int len = value.length();
            int org = vals[0];
            int w;
            boolean sign = false;
            char c;
            for (w = 0; w < len; w++)
            {
                c = value.charAt(org + w);
                if (!sign)
                {
                    if (Character.isDigit(c))
                    {
                        sign = true;
                        w--;
                    }
                    else if (c == '+' || c == '-')
                    {
                        sign = true;
                    }
                    else
                    {
                        return;
                    }
                }
                else
                {
                    if (c == '.' && (w < len && Character.isDigit(value.charAt(org + w + 1))))
                    {
                        //Decimal and correct format
                        w++;
                        continue;
                    }
                    else if (Character.isDigit(c))
                    {
                        continue;
                    }
                    char type = this.type;
                    boolean caps = type <= 'Z'; //Lower case is after upper case
                    if (type == 'f' || type == 'F')
                    {
                        //Not a digit? End
                        break;
                    }
                    switch (type)
                    {
                        case 'g':
                        case 'G':
                        case 'e':
                        case 'E':
                            if (c == '+' || c == '-')
                            {
                                continue;
                            }
                            else if ((caps && c == 'E') || (!caps && c == 'e'))
                            {
                                continue;
                            }
                            break;
                    }
                    break;
                }
            }
            vals[0] += w;
            if (this.requiresInput == 1)
            {
                return;
            }
            float valf = 0;
            double vald = 0;
            try
            {
                if (this.length == 'l' || this.length == 'L')
                {
                    vald = Double.parseDouble(value.substring(org, org + w));
                }
                else
                {
                    valf = Float.parseFloat(value.substring(org, org + w));
                }
            }
            catch (NumberFormatException e)
            {
                return;
            }
            Object obj = refO[vals[1]];
            if (obj == null || !obj.getClass().isArray())
            {
            	if(obj != null)
            	{
            		if(!(obj instanceof VirtualPointer))
            		{
            			return;
            		}
            	}
            	else
            	{
            		return;
            	}
            }
            vals[1]++;
            boolean written = false;
            if (this.length == 'l' || this.length == 'L')
            {
                if (obj instanceof double[])
                {
                    written = true;
                    ((double[])obj)[0] = vald;
                }
                else if(obj instanceof VirtualPointer)
                {
                	written = true;
                	((VirtualPointer)obj).getProcessor().write(vald);
                }
            }
            else
            {
                if (obj instanceof float[])
                {
                    written = true;
                    ((float[])obj)[0] = valf;
                }
                else if(obj instanceof VirtualPointer)
                {
                	written = true;
                	((VirtualPointer)obj).getProcessor().write(valf);
                }
            }
            if (!written)
            {
                //Error
                vals[1]--;
                vals[0] -= w;
            }
        }
    }
	
	private static class PointerFormatElement extends GeneralFormatElement
    {
        public PointerFormatElement(String format)
        {
        	super(format);
        }

        public String inFormat(Object obj)
        {
        	if(obj instanceof VirtualPointer)
        	{
        		long pos = ((VirtualPointer)obj).getPosition() & 0xFFFFFFFFL;
        		StringBuffer buf = new StringBuffer();
        		buf.append(Long.toString(pos, 16));
        		if(buf.length() < 8)
        		{
        			char[] chars = new char[8 - buf.length()];
        			Arrays.fill(chars, '0');
        			buf.insert(0, chars);
        		}
        		buf.insert(4, ':');
        		return buf.toString();
        	}
        	else
        	{
        		argError("PointerFormat", Utility.LCMS_Resources.getString(LCMSResource.PRINTUTIL_NULL_POINTER_ERR), obj.getClass());
        	}
        	return "0000:0000";
        }
        
        public String getNullParamOutput()
        {
        	return "0000:0000";
        }

        public void unformat(String value, Object[] refO, int[] vals)
        {
            throw new UnsupportedOperationException();
        }
    }
}
