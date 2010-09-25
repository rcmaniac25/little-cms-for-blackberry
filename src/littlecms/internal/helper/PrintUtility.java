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

//#ifdef CMS_INTERNAL_ACCESS & DEBUG
public
//#endif
final class PrintUtility
{
	public static int output(PrintStream out, int max, final String format, Object[] args)
	{
		Object[] formats = breakFormat(format, null);
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
        return count;
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
	
	private static String SPECIFIERS = "cdieEfFgGosuxXpn";
	private static String FLAGS = "-+ #0";
	private static String WIDTH_PRECISION = "123456789*0"; //Zero is added at end so that when FULL_FORMAT is generated there isn't two zeros in the format. It wouldn't cause an error but it would be one more char to check that isn't needed.
	private static String LENGTH = "hlLzjt";
	private static String FULL_FORMAT = FLAGS + WIDTH_PRECISION.substring(0, 9) + '.' + LENGTH + SPECIFIERS;
	
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

        public abstract void unformat(String value, Object refO);

        public String getFormat()
        {
            return format;
        }

        public abstract void setInputValue(Long one, Long two);

        public abstract boolean takesArg();

        public abstract int requires();

        public abstract int argLocation();
        
        public abstract String getNullParamOutput();

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
                case 'f': //Decimal floating point
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
            this.precision = -1;
            this.width = -1;
            this.arg = true; //Not sure why this should be included but could be useful in the future or depending on implemintation.
            parseFormat();
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
                if (el.indexOf('*') >= 0)
                {
                    requiresInput = 1;
                }
                else
                {
                    width = Integer.parseInt(el);
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

        public void unformat(String value, Object refO)
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
            }
            if (str == null)
            {
                if (obj instanceof char[])
                {
                    if (this.length == 'l')
                    {
                        str = new String((char[])obj);
                    }
                    else
                    {
                        char[] chars = (char[])obj;
                        int len;
                        byte[] nBytes = new byte[len = charType ? 1 : chars.length];
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
            else if (this.precision >= 0)
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

        public void unformat(String value, Object refO)
        {
        	//TODO
            throw new UnsupportedOperationException();
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
                	str = Long.toString(value, 16);
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
                        rem -= rem;
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
                            bu.append((char)(result[k] + '0'));
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
        
        public void unformat(String value, Object refO)
        {
        	//TODO
            throw new UnsupportedOperationException();
        }
    }
	
	private static class FloatFormatElement extends GeneralFormatElement
    {
        public FloatFormatElement(String format)
        {
        	super(format);
        }

        public String inFormat(Object obj)
        {
        	//TODO
            throw new UnsupportedOperationException();
        }
        
        public String getNullParamOutput()
        {
        	//TODO
            throw new UnsupportedOperationException();
        }

        public void unformat(String value, Object refO)
        {
        	//TODO
            throw new UnsupportedOperationException();
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

        public void unformat(String value, Object refO)
        {
            throw new UnsupportedOperationException();
        }
    }
}
