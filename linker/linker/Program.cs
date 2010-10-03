using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;

namespace linker
{
    class Program
    {
        static void Main(string[] args)
        {
            if (args.Length < 2)
            {
#if ORIGINAL_LINKER
                Console.WriteLine("linker <tag/type> <input file>");
#else
                Console.WriteLine("linker <lookup file> <input file>");
#endif
                return;
            }
#if ORIGINAL_LINKER
            bool tag = args[0].ToLower().Equals("tag");
#else
            FileStream lookFs = new FileStream(args[0], FileMode.Open);
#endif
            FileStream inFs = new FileStream(args[1], FileMode.Open);
            string outPath = string.Format("{0}Output.txt", Path.Combine(Path.GetDirectoryName(args[1]), Path.GetFileNameWithoutExtension(args[1])));
            FileStream outFs = new FileStream(outPath, FileMode.Create);
#if ORIGINAL_LINKER
            convert(inFs, outFs, tag);
#else
            process(inFs, outFs, lookFs);
            lookFs.Close();
#endif
            outFs.Flush();
            outFs.Close();
            inFs.Close();
        }

#if ORIGINAL_LINKER
        private static void writeGeneralFormatHead(StreamWriter sw, string type, string name)
        {
            sw.WriteLine("static {0} {1};", type, name);
            sw.WriteLine();
            sw.WriteLine("static");
            sw.WriteLine('{');
            sw.Write("\t{0} ", type);
        }

        private static void writeGeneralFormatBody(StreamWriter sw, string type, string tempName, string body, bool cont, bool tab)
        {
            if (tab)
            {
                sw.Write('\t');
            }
            sw.Write("{0} = new {1}({2}", tempName, type, body);
            if (cont)
            {
                sw.WriteLine(", {0});", tempName);
            }
            else
            {
                sw.WriteLine(", null);");
            }
        }

        private static void writeGeneralFormatEnd(StreamWriter sw, string type, string name, string tempName, string body, bool cont, bool tab)
        {
            if (tab)
            {
                sw.Write('\t');
            }
            sw.Write("{0} = new {1}({2}", name, type, body);
            if (cont)
            {
                sw.WriteLine(", {0});", tempName);
            }
            else
            {
                sw.WriteLine(", null);");
            }
            sw.WriteLine('}');

        }

        private static void convert(Stream ins, Stream outs, bool tag)
        {
            StreamReader sr = new StreamReader(ins);
            StreamWriter sw = new StreamWriter(outs);

            string tagCountName = sr.ReadLine();
            string tagTypeName = sr.ReadLine();
            string tagName = sr.ReadLine();
            string tagTempName = sr.ReadLine();

            writeGeneralFormatHead(sw, tagTypeName, tagName);

            string line = null;
            List<string> bodies = new List<string>();
            while ((line = sr.ReadLine()) != null)
            {
                bodies.Add(tag ? parseTag(line) : parseType(line));
            }
            bodies.Reverse();

            int count = 1;
            bool cont = false;
            bool tab = false;
            for (int i = 0; i < bodies.Count - 1; i++)
            {
                string body = bodies[i];
                if (body == null)
                {
                    sw.WriteLine("\t");
                    continue;
                }
                count++;
                cont = body.Substring(body.IndexOf('|') + 1).StartsWith("&");
                body = body.Substring(0, body.IndexOf('|'));
                writeGeneralFormatBody(sw, tagTypeName, tagTempName, body, cont, tab);

                tab = true;
            }

            string b = bodies[bodies.Count - 1];
            b = b.Substring(0, b.IndexOf('|'));
            writeGeneralFormatEnd(sw, tagTypeName, tagName, tagTempName, b, cont, tab);

            sw.WriteLine();
            sw.WriteLine("public static final int {0} = {1};", tagCountName, count);

            sw.Flush();
        }

        private static string parseTag(string text)
        {
            StringBuilder sb = new StringBuilder();

            text = text.Trim();
            if (text.Length == 0)
            {
                return null;
            }
            else
            {
                text = text.Remove(0, 2);
                string temp = text.Substring(0, text.IndexOf(','));
                sb.Append(temp); //tag id
                text = text.Remove(0, temp.Length + 1).Trim();
                if (text.StartsWith("null", true, null))
                {
                    sb.Append(", null,");
                    text = text.Substring(text.IndexOf(',') + 1).Trim();
                }
                else
                {
                    temp = text.Substring(0, text.Substring(0, text.LastIndexOf('}') - 1).LastIndexOf('}') + 1).Trim();
                    text = text.Remove(0, temp.Length + 1).Trim(); //continue?
                    temp = temp.Remove(0, 1);
                    sb.AppendFormat(", cmsTagDescriptor({0}, ", temp.Substring(0, temp.IndexOf(',')).Trim()); //elemCount
                    temp = temp.Remove(0, temp.IndexOf('{') + 1).Trim();
                    string tagGroup = temp.Substring(0, temp.IndexOf('}'));

                    string[] tags = tagGroup.Split(new char[] { ',' }, StringSplitOptions.RemoveEmptyEntries);
                    sb.Append("new int[]{");
                    for (int i = 0; i < tags.Length; i++)
                    {
                        sb.AppendFormat("lcms2.{0}", tags[i].Trim()); //types
                        if (i < (tags.Length - 1))
                        {
                            sb.Append(", ");
                        }
                    }
                    sb.Append("}, ");

                    temp = temp.Remove(0, temp.IndexOf(',', temp.IndexOf('}')) + 1).Trim();
                    temp = temp.Remove(temp.IndexOf('}')); //process
                    if (temp.ToLower().Equals("null"))
                    {
                        sb.Append("null)");
                    }
                    else
                    {
                        writeDelegate(sb, "cmsTagDescriptor.tagDesDecideType", "int", "double ICCVersion, final Object Data", "ICCVersion, Data", temp);
                        sb.Append(')');
                    }
                }
                sb.AppendFormat("|{0}", text);
            }

            return sb.ToString();
        }

        private static void writeDelegate(StringBuilder sb, string name, string ret, string fullArgs, string args, string function)
        {
            sb.AppendFormat("new {2}{1}\t{{{1}\t\tpublic {3} run({4}){1}\t\t{{{1}\t\t\treturn {0}({5});{1}\t\t}}{1}\t}}", function, Environment.NewLine, name, ret, fullArgs, args);
        }

        private static string parseType(string text)
        {
            StringBuilder sb = new StringBuilder();

            text = text.Trim();
            if (text.Length == 0)
            {
                return null;
            }
            else
            {
                text = text.Remove(0, 1);
                int index = text.IndexOf('(') + 1;
                if (text.StartsWith("TYPE_HANDLER"))
                {
                    typeParse(sb, text.Substring(index, text.LastIndexOf(')') - index).Trim());
                }
                else if (text.StartsWith("TYPE_MPE_HANDLER"))
                {
                    typeParseMPE(sb, text.Substring(index, text.LastIndexOf(')') - index).Trim());
                }
                else
                {
                    sb.AppendFormat("/*{0}*/|", text.Substring(0, text.LastIndexOf(')') + 1).Trim());
                }
                sb.Append(text.Substring(text.LastIndexOf(')') + 2).Trim());
            }

            return sb.ToString();
        }

        private static void typeParse(StringBuilder sb, string text)
        {
            string type = text.Substring(0, text.IndexOf(',')).Trim();
            string name = text.Substring(text.IndexOf(',') + 1).Trim();
            typeWriteTypeHandlerStart(sb, type, name);
            writeDelegate(sb, "cmsTagTypeHandler.tagHandlerDupPtr", "Object", "cmsTagTypeHandler self, final Object Ptr, int n", "self, Ptr, n", string.Format("Type_{0}_Dup", name));
            sb.Append(", ");
            writeDelegate(sb, "cmsTagTypeHandler.tagHandlerFreePtr", "Object", "cmsTagTypeHandler self, Object Ptr", "self, Ptr", string.Format("Type_{0}_Free", name));
            typeWriteTypeHandlerEnd(sb);
        }

        private static void typeParseMPE(StringBuilder sb, string text)
        {
            string type = text.Substring(0, text.IndexOf(',')).Trim();
            string name = text.Substring(text.IndexOf(',') + 1).Trim();
            typeWriteTypeHandlerStart(sb, type, name);
            sb.Append("GenericMPEdupImpl, GenericMPEfreeImpl");
            typeWriteTypeHandlerEnd(sb);
        }

        private static void typeWriteTypeHandlerStart(StringBuilder sb, string type, string name)
        {
            sb.AppendFormat("new cmsTagTypeHandler(lcms2.{0}, ", type);
            writeDelegate(sb, "cmsTagTypeHandler.tagHandlerReadPtr", "Object", "cmsTagTypeHandler self, cmsIOHANDLER io, int[] nItems, int SizeOfTag", "self, io, nItems, SizeOfTag", string.Format("Type_{0}_Read", name));
            sb.Append(", ");
            writeDelegate(sb, "cmsTagTypeHandler.tagHandlerReadPtr", "boolean", "cmsTagTypeHandler self, cmsIOHANDLER io, Object Ptr, int nItems", "self, io, Ptr, nitems", string.Format("Type_{0}_Write", name));
            sb.Append(", ");
        }

        private static void typeWriteTypeHandlerEnd(StringBuilder sb)
        {
            sb.Append(")|");
        }
#else
        private class LookupSheet
        {
            private List<OperationElement> elements;

            public LookupSheet(Stream st)
            {
                StreamReader sr = new StreamReader(st);
                string line;
                //Get the lookup sheet, skipping everything else
                while ((line = sr.ReadLine()) != null && line.Equals("$OP SLUF", StringComparison.CurrentCultureIgnoreCase)) ;
                List<string> lines = new List<string>();
                lines.Add(line);
                while ((line = sr.ReadLine()) != null && !line.Equals("$OP ELUF", StringComparison.CurrentCultureIgnoreCase))
                {
                    if (!line.StartsWith("##"))
                    {
                        lines.Add(line);
                    }
                }
                //Remove blank lines
                int i = 0;
                while (true)
                {
                    if (string.IsNullOrWhiteSpace(lines[i]))
                    {
                        lines.RemoveAt(i);
                        i = 0;
                    }
                    else
                    {
                        i++;
                        if (i >= lines.Count)
                        {
                            break;
                        }
                    }
                }
                //Load the elements using reflection
                elements = loadElements<OperationElement>(lines, null, ref i, true);
            }

            public OperationElement getOp(string name)
            {
                //Lookup sheet elements
                foreach (OperationElement op in elements)
                {
                    if (op.Name.Equals(name))
                    {
                        return op;
                    }
                    else if (op is TYP)
                    {
                        //Search inside the type to see if 
                        TYP typ = op as TYP;
                        OperationElement el = searchOp(typ, name);
                        if (el != null)
                        {
                            return el;
                        }
                    }
                }
                //Predefined elements
                foreach (FIE op in predefinedElements)
                {
                    if (op.Type.Equals(name))
                    {
                        return op;
                    }
                }
                return null;
            }

            private OperationElement searchOp(TYP typ, string name)
            {
                string nName = string.Format("{0}.{1}", typ.Name, name);
                if (typ.DelegateCount > 0)
                {
                    for (int i = 0; i < typ.DelegateCount; i++)
                    {
                        DEL del = typ.getDelegates(i);
                        if (del.Name.Equals(nName))
                        {
                            return del;
                        }
                    }
                }
                if (typ.TypeCount > 0)
                {
                    for (int i = 0; i < typ.TypeCount; i++)
                    {
                        TYP type = typ.getType(i);
                        if (type.Name.Equals(nName))
                        {
                            return type;
                        }
                    }
                }
                return null;
            }

            private static List<OperationElement> predefinedElements;

            static LookupSheet()
            {
                predefinedElements = new List<OperationElement>();
                List<string> lines = new List<string>();

                //byte
                lines.Add("$AT TYPE byte");
                lines.Add("$AT ATT $predef$");
                predefinedElements.Add(new FIE(null, lines));

                //short
                lines[0] = "$AT TYPE short";
                predefinedElements.Add(new FIE(null, lines));

                //char
                lines[0] = "$AT TYPE char";
                predefinedElements.Add(new FIE(null, lines));

                //int
                lines[0] = "$AT TYPE int";
                predefinedElements.Add(new FIE(null, lines));

                //long
                lines[0] = "$AT TYPE long";
                predefinedElements.Add(new FIE(null, lines));

                //float
                lines[0] = "$AT TYPE float";
                predefinedElements.Add(new FIE(null, lines));

                //double
                lines[0] = "$AT TYPE double";
                predefinedElements.Add(new FIE(null, lines));

                //boolean
                lines[0] = "$AT TYPE boolean";
                predefinedElements.Add(new FIE(null, lines));

                //Object
                lines[0] = "$AT TYPE Object";
                predefinedElements.Add(new FIE(null, lines));

                //String
                lines[0] = "$AT TYPE String";
                predefinedElements.Add(new FIE(null, lines));
            }
        }

        private static List<T> loadElements<T>(List<string> lines, string outerType, ref int i, bool op)
        {
            if (op)
            {
                return loadOpElements<T>(lines, outerType, ref i);
            }
            else
            {
                return loadAtElements<T>(lines, ref i);
            }
        }

        private static List<T> loadOpElements<T>(List<string> lines, string outerType, ref int i)
        {
            List<T> elements = new List<T>();
            List<string> tempLines = new List<string>();
            for (i = 0; i < lines.Count; i++)
            {
                //Process
                if (lines[i].Trim().StartsWith("$OP S", StringComparison.CurrentCultureIgnoreCase))
                {
                    string type = lines[i++].Trim().Substring(5).Trim();
                    Type t = Type.GetType("linker.Program+" + type.ToUpper());
                    if (t == null)
                    {
                        throw new InvalidDataException("Unsupported op: " + type);
                    }
                    string start = string.Format("$OP S{0}", type).ToUpper();
                    string end = string.Format("$OP E{0}", type).ToUpper();
                    int inset = 0;
                    for (int k = i; k < lines.Count; k++, i++)
                    {
                        if (lines[k].Trim().StartsWith(start, StringComparison.CurrentCultureIgnoreCase))
                        {
                            inset++;
                        }
                        else if (lines[k].Trim().StartsWith(end, StringComparison.CurrentCultureIgnoreCase))
                        {
                            if (inset == 0)
                            {
                                elements.Add((T)Activator.CreateInstance(t, outerType, tempLines));
                                tempLines.Clear();
                                break;
                            }
                            inset--;
                        }
                        tempLines.Add(lines[k]);
                    }
                }
                else if (!lines[i].Trim().StartsWith("$AT", StringComparison.CurrentCultureIgnoreCase))
                {
                    throw new InvalidDataException("Unsupported value: " + lines[i]);
                }
            }
            return elements;
        }

        private static List<T> loadAtElements<T>(List<string> lines, ref int k)
        {
            List<T> elements = new List<T>();
            int inset = 0;
            for (int i = k; i < lines.Count; i++)
            {
                //Process
                if (inset == 0)
                {
                    if (lines[i].Trim().StartsWith("$AT", StringComparison.CurrentCultureIgnoreCase))
                    {
                        string data = lines[i].Trim().Substring(3).Trim();
                        string type = data.Substring(0, data.IndexOf(' ')).Trim();
                        data = data.Substring(type.Length).Trim();
                        Type t = Type.GetType("linker.Program+" + type.ToUpper());
                        if (t == null)
                        {
                            throw new InvalidDataException("Unsupported at: " + type);
                        }
                        elements.Add((T)Activator.CreateInstance(t, data));
                        k++;
                    }
                    else if (!lines[i].Trim().StartsWith("$OP S", StringComparison.CurrentCultureIgnoreCase))
                    {
                        throw new InvalidDataException("Unsupported value: " + lines[i]);
                    }
                    else if (lines[i].Trim().StartsWith("$OP E", StringComparison.CurrentCultureIgnoreCase))
                    {
                        inset--;
                    }
                    else
                    {
                        inset++;
                    }
                }
                else if (lines[i].Trim().StartsWith("$OP E", StringComparison.CurrentCultureIgnoreCase))
                {
                    inset--;
                }
                else if (lines[i].Trim().StartsWith("$OP S", StringComparison.CurrentCultureIgnoreCase))
                {
                    inset++;
                }
            }
            return elements;
        }

        #region Operations

        private abstract class OperationElement
        {
            protected string name;

            public string Name
            {
                get
                {
                    return this.name;
                }
                internal set
                {
                    this.name = value;
                }
            }

            public override string ToString()
            {
                return this.name;
            }

            public abstract void process(StreamWriter sw, LookupSheet lookup, string typeData, ref int tab);
        }

        private class TYP : OperationElement
        {
            private List<FIE> fields;
            private List<TYP> types;
            private List<DEL> delegates;

            public TYP(string outerType, List<string> lines)
            {
                int i = 0;
                List<AttributeElement> ats = loadElements<AttributeElement>(lines, null, ref i, false);
                if (ats.Count > 0)
                {
                    bool name = false;
                    foreach (AttributeElement at in ats)
                    {
                        if (at is NAME)
                        {
                            if (name)
                            {
                                throw new InvalidOperationException("NAME already exists");
                            }
                            if (outerType != null)
                            {
                                this.name = string.Format("{0}.{1}", outerType, ((NAME)at).Name);
                            }
                            else
                            {
                                this.name = ((NAME)at).Name;
                            }
                            name = true;
                        }
                        else
                        {
                            throw new InvalidDataException("Unknown attribute: " + at.GetType());
                        }
                    }
                }
                List<OperationElement> ops = loadElements<OperationElement>(lines, this.name, ref i, true);
                if (ops.Count > 0)
                {
                    foreach (OperationElement op in ops)
                    {
                        if (op is FIE)
                        {
                            if (fields == null)
                            {
                                fields = new List<FIE>();
                            }
                            fields.Add((FIE)op);
                        }
                        else if (op is TYP)
                        {
                            if (types == null)
                            {
                                types = new List<TYP>();
                            }
                            types.Add((TYP)op);
                        }
                        else if (op is DEL)
                        {
                            if (delegates == null)
                            {
                                delegates = new List<DEL>();
                            }
                            delegates.Add((DEL)op);
                        }
                        else
                        {
                            throw new InvalidDataException("Unknown operation: " + op.GetType());
                        }
                    }
                }
            }

            public int FieldCount
            {
                get
                {
                    return fields == null ? 0 : fields.Count;
                }
            }

            public FIE getField(int index)
            {
                return fields == null ? null : fields[index];
            }

            public int TypeCount
            {
                get
                {
                    return types == null ? 0 : types.Count;
                }
            }

            public TYP getType(int index)
            {
                return types == null ? null : types[index];
            }

            public int DelegateCount
            {
                get
                {
                    return delegates == null ? 0 : delegates.Count;
                }
            }

            public DEL getDelegates(int index)
            {
                return delegates == null ? null : delegates[index];
            }

            public bool Redundent
            {
                get
                {
                    if (fields.Count > 0)
                    {
                        return fields[fields.Count-1].Type.Equals(this.name);
                    }
                    return false;
                }
            }

            public override string ToString()
            {
                return string.Format("TYP: {0}", base.ToString());
            }

            public override void process(StreamWriter sw, LookupSheet lookup, string typeData, ref int tab)
            {
                writeElement(sw, lookup, typeData, this, false, ref tab);
            }
        }

        private class DEL : OperationElement
        {
            private FIE returnType;
            private List<FIE> args;

            public DEL(string outerType, List<string> lines)
            {
                int i = 0;
                List<AttributeElement> ats = loadElements<AttributeElement>(lines, null, ref i, false);
                if (ats.Count > 0)
                {
                    bool name = false;
                    foreach (AttributeElement at in ats)
                    {
                        if (at is NAME)
                        {
                            if (name)
                            {
                                throw new InvalidOperationException("NAME already exists");
                            }
                            if (outerType != null)
                            {
                                this.name = string.Format("{0}.{1}", outerType, ((NAME)at).Name);
                            }
                            else
                            {
                                this.name = ((NAME)at).Name;
                            }
                            name = true;
                        }
                        else
                        {
                            throw new InvalidDataException("Unknown attribute: " + at.GetType());
                        }
                    }
                }
                List<OperationElement> ops = loadElements<OperationElement>(lines, this.name, ref i, true);
                if (ops.Count > 0)
                {
                    foreach (OperationElement op in ops)
                    {
                        if (op is FIE)
                        {
                            FIE field = op as FIE;
                            if (field.Name.EndsWith("$ret", StringComparison.CurrentCultureIgnoreCase))
                            {
                                if (this.returnType != null)
                                {
                                    throw new InvalidOperationException("Return type already exists");
                                }
                                this.returnType = field;
                            }
                            else
                            {
                                if (args == null)
                                {
                                    args = new List<FIE>();
                                }
                                args.Add(field);
                            }
                        }
                        else
                        {
                            throw new InvalidDataException("Unknown operation: " + op.GetType());
                        }
                    }
                }
            }

            public FIE ReturnType
            {
                get
                {
                    return returnType;
                }
            }

            public int ArgCount
            {
                get
                {
                    return args == null ? 0 : args.Count;
                }
            }

            public FIE getArg(int index)
            {
                return args == null ? null : args[index];
            }

            public override string ToString()
            {
                return string.Format("DEL: {0}", base.ToString());
            }

            public override void process(StreamWriter sw, LookupSheet lookup, string typeData, ref int tab)
            {
                //Generate the tab element
                StringBuilder bu = new StringBuilder(new string('\t', tab));

                //Write delegate
                sw.WriteLine("new {0}()", this.name);
                sw.WriteLine("{0}{{", bu.ToString());

                tab++;
                bu.Append('\t');
                sw.Write(bu.ToString());

                //Write function call
                sw.Write("public ");
                if (this.returnType != null)
                {
                    sw.Write("{0} run(", this.returnType.Type);
                }
                else
                {
                    sw.Write("void run(");
                }

                for (int i = 0; i < this.args.Count; i++)
                {
                    FIE fie = this.args[i];
                    if (fie.Name.EndsWith("$ret", StringComparison.CurrentCultureIgnoreCase))
                    {
                        continue;
                    }
                    if (fie.Strict != null)
                    {
                        sw.Write(fie.Strict);
                        sw.Write(' ');
                    }
                    sw.Write("{0}{1} {2}", fie.Type, fie.Array != null ? fie.Array : string.Empty, fie.Name.Substring(fie.Name.LastIndexOf('.') + 1).Trim());
                    if (i != (this.args.Count - 1))
                    {
                        sw.Write(", ");
                    }
                }
                sw.WriteLine(')');

                sw.WriteLine("{0}{{", bu.ToString());

                tab++;
                bu.Append('\t');
                sw.Write(bu.ToString());

                //Write call function
                if (this.returnType != null)
                {
                    sw.Write("return ");
                }
                sw.Write("{0}(", typeData);
                for (int i = 0; i < this.args.Count; i++)
                {
                    FIE fie = this.args[i];
                    if (fie.Name.EndsWith("$ret", StringComparison.CurrentCultureIgnoreCase))
                    {
                        continue;
                    }
                    sw.Write(fie.Name.Substring(fie.Name.LastIndexOf('.') + 1).Trim());
                    if (i != (this.args.Count - 1))
                    {
                        sw.Write(", ");
                    }
                }
                sw.WriteLine(");");

                //Close brackets
                tab--;
                bu.Remove(bu.Length - 1, 1);
                sw.WriteLine("{0}}}", bu.ToString());

                tab--;
                bu.Remove(bu.Length - 1, 1);
                sw.Write("{0}}}", bu.ToString());
            }
        }

        private class PRE : OperationElement
        {
            public PREPROC proc;

            public PRE(string outerType, List<string> lines)
            {
                if (lines.Count > 1)
                {
                    throw new InvalidDataException("PRE cannot be more then 1 line");
                }
                int i = 0;
                proc = (PREPROC)loadElements<AttributeElement>(lines, null, ref i, false)[0];
                if (outerType != null)
                {
                    proc.Name = string.Format("{0}.{1}", outerType, proc.Name);
                }
                this.name = proc.Name;
            }

            public bool SkipPreproc
            {
                get
                {
                    return proc.Format == null;
                }
            }

            public override string ToString()
            {
                return string.Format("PRE: {0}", base.ToString());
            }

            public override void process(StreamWriter sw, LookupSheet lookup, string typeData, ref int tab)
            {
                //Replace () with {}
                typeData = typeData.Replace('(', '{');
                typeData = typeData.Replace(')', '}');

                string[] parts = breakIntoElements(typeData);
                object[] args = new object[parts.Length];
                for (int i = 0; i < args.Length; i++)
                {
                    args[i] = parts[i];
                }
                sw.Write(preprocessor(lookup, string.Format(proc.Format, args)));
            }
        }

        private class FIE : OperationElement
        {
            private string type, strict, array;

            public FIE(string outerType, List<string> lines)
            {
                int i = 0;
                List<AttributeElement> ats = loadElements<AttributeElement>(lines, null, ref i, false);
                if (ats.Count > 0)
                {
                    bool name = false;
                    bool type = false;
                    bool extra = false;
                    foreach (AttributeElement at in ats)
                    {
                        if (at is NAME)
                        {
                            if (name)
                            {
                                throw new InvalidOperationException("NAME already exists");
                            }
                            if (outerType != null)
                            {
                                this.name = string.Format("{0}.{1}", outerType, ((NAME)at).Name);
                            }
                            else
                            {
                                this.name = ((NAME)at).Name;
                            }
                            name = true;
                        }
                        else if (at is TYPE)
                        {
                            if (type)
                            {
                                throw new InvalidOperationException("TYPE already exists");
                            }
                            this.type = ((TYPE)at).Type;
                            this.array = ((TYPE)at).Array;
                            type = true;
                        }
                        else if (at is ATT)
                        {
                            if (extra)
                            {
                                throw new InvalidOperationException("ATT already exists");
                            }
                            this.strict = ((ATT)at).DirectAttributes;
                            extra = true;
                        }
                        else
                        {
                            throw new InvalidDataException("Unknown attribute: " + at.GetType());
                        }
                    }
                }
            }

            public string Type
            {
                get
                {
                    return this.type;
                }
                set
                {
                    this.type = value;
                }
            }

            public string Strict
            {
                get
                {
                    return this.strict;
                }
                set
                {
                    this.strict = value;
                }
            }

            public string Array
            {
                get
                {
                    return this.array;
                }
                set
                {
                    this.array = value;
                }
            }

            public override string ToString()
            {
                return string.Format("FIE: {0}", base.ToString());
            }

            public override void process(StreamWriter sw, LookupSheet lookup, string typeData, ref int tab)
            {
                if (this.strict.Equals("$predef$"))
                {
                    //Predefined types
                    double d;
                    if (double.TryParse(typeData, out d) || this.Type.Equals("Object") || !char.IsLetter(typeData[0]) || this.Type.Equals("boolean"))
                    {
                        if (this.Type.Equals("boolean"))
                        {
                            sw.Write(typeData.ToLower());
                        }
                        else
                        {
                            sw.Write(typeData);
                        }
                    }
                    else
                    {
                        sw.Write("lcms2.{0}", typeData);
                    }
                }
                else
                {
                    Console.WriteLine("WARNING: Only predefined types are supported for FIEs right now.");
                }
            }
        }

        #endregion

        #region Attributes

        private interface AttributeElement
        {
        }

        private class NAME : AttributeElement
        {
            public string Name { get; set; }

            public NAME(string input)
            {
                this.Name = input;
            }
        }

        private class TYPE : AttributeElement
        {
            public string Type { get; set; }
            public string Array { get; set; }

            public TYPE(string input)
            {
                if (input.IndexOfAny(new char[] { '[', ']' }) >= 0)
                {
                    this.Type = input.Substring(0, input.IndexOf('[')).Trim();
                    this.Array = input.Substring(input.IndexOf('[')).Trim();
                }
                else
                {
                    this.Type = input;
                }
            }
        }

        private class PREPROC : AttributeElement
        {
            public string Name { get; set; }
            public List<string> inputNames;
            public string Code, Format;

            public PREPROC(string input)
            {
                input = input.Trim();
                bool skip = input.StartsWith("$$");
                if (skip)
                {
                    input = input.Substring(2).Trim();
                }
                int index = input.IndexOf('(');
                this.Name = input.Substring(0, index).Trim();
                string[] args = input.Substring(index + 1, (input.IndexOf(')', index) - index) - 1).Trim().Split(',');
                index = 0;
                for (int i = 0; i < args.Length; i++)
                {
                    index += args[i].Length;
                    args[i] = args[i].Trim();
                }
                inputNames = new List<string>(args);
                this.Code = input.Substring(this.Name.Length + index + 3).Trim();

                if (!skip)
                {
                    //Now convert the code to a format
                    StringBuilder bu = new StringBuilder();
                    string[] stringReplace1 = new string[this.inputNames.Count];
                    string[] stringReplace2 = new string[this.inputNames.Count];
                    for (int i = 0; i < this.inputNames.Count; i++)
                    {
                        stringReplace1[i] = string.Format("##{0}##", this.inputNames[i]);
                        stringReplace2[i] = string.Format("({0})", this.inputNames[i]);
                    }
                    for (int i = 0; i < this.Code.Length; i++)
                    {
                        char c = this.Code[i];
                        if (c == '{')
                        {
                            bu.Append("{{");
                        }
                        else if (c == '}')
                        {
                            bu.Append("}}");
                        }
                        else
                        {
                            bool replace = false;
                            for (int k = 0; k < stringReplace1.Length; k++)
                            {
                                if (this.Code.IndexOf(stringReplace1[k], i) == i)
                                {
                                    bu.AppendFormat("{{{0}}}", k);
                                    replace = true;
                                    i += this.inputNames[k].Length + 3;
                                    break;
                                }
                                else if (this.Code.IndexOf(stringReplace2[k], i) == i)
                                {
                                    //Could be part of another preprocessor
                                    if (i > 0)
                                    {
                                        if (!char.IsLetter(this.Code[i - 1]))
                                        {
                                            bu.AppendFormat("{{{0}}}", k);
                                            replace = true;
                                            i += this.inputNames[k].Length + 1;
                                        }
                                        else
                                        {
                                            //Part of another preprocessor, need to replace the inner args
                                            bu.AppendFormat("({{{0}}})", k);
                                            replace = true;
                                            i += this.inputNames[k].Length + 1;
                                        }
                                        break;
                                    }
                                }
                            }
                            if (!replace)
                            {
                                bu.Append(c);
                            }
                        }
                    }
                    this.Format = bu.ToString();
                }
            }
        }

        private class ATT : AttributeElement
        {
            public string DirectAttributes { get; set; }

            public ATT(string input)
            {
                this.DirectAttributes = input;
            }
        }

        #endregion

        private static void process(Stream inFs, Stream outFs, Stream lookFs)
        {
            LookupSheet sheet = new LookupSheet(lookFs);
            StreamReader sr = new StreamReader(inFs);
            StreamWriter sw = new StreamWriter(outFs);
            //Parse the input file
            string opCountName = sr.ReadLine().Trim();
            string elementType = sr.ReadLine().Trim();
            string elementName = sr.ReadLine().Trim();
            List<string> elements = new List<string>();
            string line = sr.ReadLine();
            bool hasTempType = false; //If another element is included (prefixed by $) then it is considered a temp name and the type is expected to be a redundent type. In other words the last argument is the same as the actual type.
            string tempName = null;
            if (line != null && !string.IsNullOrWhiteSpace(line) && line.Trim().StartsWith("$"))
            {
                hasTempType = true;
                tempName = line.Trim().Substring(1).Trim();
            }
            else
            {
                elements.Add(line.Trim());
            }
            //Load all the lines (comments included)
            while ((line = sr.ReadLine()) != null)
            {
                elements.Add(line.Trim());
            }

            //Strip out comments
            bool multiLine = false;
            for (int i = 0; i < elements.Count; i++)
            {
                bool change = false;
                int index = elements[i].IndexOf("/*");
                if (!multiLine)
                {
                    if (index >= 0)
                    {
                        multiLine = true;
                        elements[i] = elements[i].Substring(0, index);
                        change = true;
                    }
                    else
                    {
                        index = elements[i].IndexOf("//");
                        if (index >= 0)
                        {
                            elements[i] = elements[i].Substring(0, index);
                            change = true;
                        }
                    }
                    if (change && string.IsNullOrWhiteSpace(elements[i]))
                    {
                        elements.RemoveAt(i);
                        i--;
                    }
                }
                else
                {
                    index = elements[i].IndexOf("*/");
                    if (index >= 0)
                    {
                        elements[i] = elements[i].Substring(index);
                        if (string.IsNullOrWhiteSpace(elements[i]))
                        {
                            elements.RemoveAt(i);
                            i--;
                        }
                    }
                    else
                    {
                        elements.RemoveAt(i);
                        i--;
                    }
                }
            }

            //Double check that the type is a redundent type and setup so it runs properly
            string rawType = elementType.Contains("[") ? elementType.Substring(0, elementType.IndexOf('[')) : elementType;
            OperationElement type = sheet.getOp(rawType);
            if (type == null)
            {
                Console.WriteLine("FAIL: Type: {0} is not found.", rawType);
                return;
            }
            if (!(type is TYP))
            {
                Console.WriteLine("FAIL: Type: {0} is not a registered TYP.", rawType);
                return;
            }
            TYP typ = type as TYP;
            if (!typ.Redundent && hasTempType)
            {
                Console.WriteLine("FAIL: Type: {0} is not redundent yet input file is redundent.", rawType);
                return;
            }
            if ((hasTempType = typ.Redundent) && tempName == null)
            {
                tempName = "tempElement";
            }

            if (hasTempType)
            {
                elements.Reverse();
            }
            inProcess(sw, sheet, opCountName, elementName, elementType, elements.ToArray(), hasTempType, tempName);
        }
        
        private static void inProcess(StreamWriter sw, LookupSheet lookup, string opCountName, string elementName, string elementType, string[] elements, bool redundent, string tempName)
        {
            bool array = elementType.Contains("[");
            //Redundent types don't need array markers
            if (redundent && array)
            {
                elementType = elementType.Substring(0, elementType.IndexOf('[')).Trim();
                array = false;
            }

            //Write the "header" to the type
            sw.WriteLine("static {0} {1};", elementType, elementName);
            sw.WriteLine();
            sw.WriteLine("static");
            sw.WriteLine('{');
            sw.Flush();

            int tab = 1;
            int opCount = 0;
            TYP elementTyp = (TYP)lookup.getOp(elementType.Contains("[") ? elementType.Substring(0, elementType.IndexOf('[')) : elementType);
            if (elementTyp == null)
            {
                Console.WriteLine("FAIL: Couldn't find element type: {0}", elementType);
                return;
            }
            //Write each element
            for (int i = 0; i < elements.Length; i++)
            {
                //Write tab
                sw.Write(new string('\t', tab));

                string element = elements[i];
                if (!string.IsNullOrWhiteSpace(element))
                {
                    //Do the initial processing for redundent types
                    if (redundent)
                    {
                        if (i == (elements.Length - 1))
                        {
                            sw.Write("{0} = ", elementName);
                        }
                        else
                        {
                            if (opCount == 0)
                            {
                                sw.Write("{0} {1} = ", elementType, tempName);
                            }
                            else
                            {
                                sw.Write("{0} = ", tempName);
                            }
                        }
                    }
                    else
                    {
                        if (opCount == 0)
                        {
                            sw.WriteLine("{0} = new {1}[]{{", elementName, array ? elementType.Substring(0,elementType.IndexOf('[')) : elementType);
                            tab++;

                            //Rewrite tab
                            sw.Write(new string('\t', tab));
                        }
                    }

                    //Process format and write
                    writeElement(sw, lookup, element, elementTyp, redundent, ref tab);
                    opCount++;

                    //Finish the element
                    if (redundent)
                    {
                        //Redundent types don't get finished, we finish them here
                        StringBuilder bu = new StringBuilder();
                        if (elementTyp.FieldCount > 0)
                        {
                            bu.Append(", ");
                        }
                        if (element.Substring(element.LastIndexOf(',') + 1).Trim().StartsWith("NULL", StringComparison.CurrentCultureIgnoreCase))
                        {
                            bu.Append("null");
                        }
                        else
                        {
                            bu.Append(tempName);
                        }
                        bu.Append(");");
                        sw.Write(bu.ToString());
                    }
                    else
                    {
                        if (i == (elements.Length - 1))
                        {
                            //sw.WriteLine(',');
                            sw.WriteLine();
                            tab--;

                            //Rewrite the tab and finish the array
                            sw.Write(new string('\t', tab));
                            sw.Write("};");
                        }
                        else
                        {
                            sw.Write(',');
                        }
                    }
                }

                sw.WriteLine();
                sw.Flush();
            }

            //Write the "footer" to the type
            sw.WriteLine('}');
            sw.WriteLine();
            sw.WriteLine("public static final int {0} = {1};", opCountName, opCount);
            sw.Flush();
        }

        private static void writeElement(StreamWriter sw, LookupSheet lookup, string element, TYP type, bool redundent, ref int tab)
        {
            //Write out the element, if the element is redundent then the constructor should not be completed
            sw.Write("new {0}(", type.Name);

            //Write out arguments, if any
            if (type.FieldCount > 0)
            {
                //Run the preprocessor if it is needed
                element = preprocessor(lookup, element);

                string[] elements = breakIntoElements(element);
                int dif = (redundent ? 1 : 0);
                bool tooMany = false;
                if (elements.Length != type.FieldCount)
                {
                    if (tooMany = (elements.Length > type.FieldCount))
                    {
                        Console.WriteLine("WARNING: More elements then what is supported, extra elements will be commented out.");
                    }
                    else
                    {
                        Console.WriteLine("WARNING: Not enough elements then what is needed, necessery elements that are missing will be replaced with null. This could cause errors.");
                    }
                }

                for (int i = 0; i < (type.FieldCount - dif); i++)
                {
                    FIE field = type.getField(i);
                    if (elements.Length > i)
                    {
                        string inElement = elements[i];
                        if (inElement.Equals("NULL", StringComparison.CurrentCultureIgnoreCase))
                        {
                            sw.Write("null");
                        }
                        else
                        {
                            processType(sw, lookup, inElement, field, ref tab);
                        }
                    }
                    else
                    {
                        sw.Write("null");
                    }
                    if (i != (elements.Length - (dif + 1)))
                    {
                        sw.Write(", ");
                    }
                }

                if (tooMany)
                {
                    StringBuilder bu = new StringBuilder();
                    bu.Append("/*");
                    for (int i = type.FieldCount; i < elements.Length; i++)
                    {
                        bu.Append(elements[i]);
                        if (i != (elements.Length - 1))
                        {
                            bu.Append(", ");
                        }
                    }
                    bu.Append("*/");
                    sw.Write(bu.ToString());
                }
            }
            if (!redundent)
            {
                sw.Write(")");
            }
        }

        private static string preprocessor(LookupSheet lookup, string element)
        {
            StringBuilder bu = new StringBuilder();

            while (!string.IsNullOrWhiteSpace(element))
            {
                if (isPreprocessor(element))
                {
                    string preProcName = element.Substring(0, element.IndexOf('('));
                    string preProcValue = null;
                    element = element.Substring(preProcName.Length);
                    int count = 0;
                    for (int i = 0; i < element.Length; i++)
                    {
                        char c = element[i];
                        switch (c)
                        {
                            case '(':
                                count++;
                                break;
                            case ')':
                                count--;
                                break;
                        }
                        if (count == 0)
                        {
                            i++;
                            preProcValue = element.Substring(0, i).Trim();
                            element = element.Substring(i);
                            element = element.Trim();
                            if (preProcValue != null)
                            {
                                PRE pre = (PRE)lookup.getOp(preProcName);
                                if (pre == null)
                                {
                                    Console.WriteLine("WARNING: Cannot find {0} for preprocessor. Skipping", preProcName);
                                    return null;
                                }
                                else if (pre.SkipPreproc)
                                {
                                    bu.Append(preProcName);
                                    bu.Append(preProcValue);
                                }
                                else
                                {
                                    MemoryStream mem = new MemoryStream();
                                    int tab = 0;
                                    StreamWriter sw = new StreamWriter(mem);
                                    pre.process(sw, lookup, preProcValue, ref tab);
                                    sw.Flush();
                                    mem.Position = 0;
                                    for (long k = 0; k < mem.Length; k++)
                                    {
                                        bu.Append((char)mem.ReadByte());
                                    }
                                    mem.Close();
                                }
                            }
                            else
                            {
                                Console.WriteLine("WARNING: Preprocessor failed, cannot compute. Skipping.");
                            }
                            break;
                        }
                    }
                }
                else
                {
                    bu.Append(element[0]);
                    element = element.Substring(1);
                }
            }

            return bu.ToString();
        }

        private static bool isPreprocessor(string element)
        {
            int count = 0;
            for (int i = 0; i < element.Length; i++)
            {
                switch (count)
                {
                    case 0:
                        //Just started
                        if (i > 0)
                        {
                            return false;
                        }
                        if (char.IsLetter(element[i]))
                        {
                            count++;
                        }
                        break;
                    case 1:
                        char c = element[i];
                        if (!char.IsLetterOrDigit(c))
                        {
                            switch (c)
                            {
                                case '(':
                                    count++;
                                    break;
                                case '_':
                                    break;
                                default:
                                    return false;
                            }
                        }
                        break;
                    case 2:
                        if (element.IndexOf(')', i) >= 0)
                        {
                            return true;
                        }
                        break;
                }
            }
            return false;
        }

        private static string[] breakIntoElements(string element)
        {
            List<string> elements = new List<string>();
            if (element.StartsWith("{") && (element.EndsWith("}") || element.EndsWith(",")))
            {
                int inner = 0;
                int len = 0;
                if (element.EndsWith(","))
                {
                    element = element.Substring(1, element.LastIndexOf(',') - 1).Trim();
                    //Could still have '}'
                    if (element.EndsWith("}"))
                    {
                        element = element.Substring(0, element.LastIndexOf('}')).Trim();
                    }
                }
                else
                {
                    element = element.Substring(1, element.LastIndexOf('}') - 1).Trim();
                }
                while (!string.IsNullOrWhiteSpace(element))
                {
                    if (len == element.Length)
                    {
                        elements.Add(element);
                        break;
                    }
                    char c = element[len++];
                    switch (c)
                    {
                        case ',':
                            if (inner == 0)
                            {
                                elements.Add(element.Substring(0, --len).Trim());
                                element = element.Substring(len + 1).Trim();
                                len = 0;
                            }
                            break;
                        case '{':
                            inner++;
                            break;
                        case '}':
                            inner--;
                            break;
                    }
                }
            }
            else
            {
                Console.WriteLine("WARNING: Not valid input value. Element needs to be formatted as such \"{ /*some code*/ }\" (though the spaces between the brackets and code are not needed).");
            }
            return elements.ToArray();
        }

        private static void processType(StreamWriter sw, LookupSheet lookup, string typeData, FIE field, ref int tab)
        {
            OperationElement element = lookup.getOp(field.Type);
            if (element == null)
            {
                Console.WriteLine("WARNING: Cannot find \"{0}\", skipping.", field.Type);
            }
            else
            {
                if (field.Array != null)
                {
                    if (!field.Array.Equals("[]"))
                    {
                        Console.WriteLine("WARNING: Only single dimention arrays are supported right now.");
                    }
                    else
                    {
                        string[] elements = breakIntoElements(typeData);
                        sw.Write("new {0}[]{{", field.Type);
                        for (int i = 0; i < elements.Length; i++)
                        {
                            element.process(sw, lookup, elements[i], ref tab);
                            if (i != (elements.Length - 1))
                            {
                                sw.Write(", ");
                            }
                        }
                        sw.Write("}");
                    }
                }
                else
                {
                    element.process(sw, lookup, typeData, ref tab);
                }
            }
        }
#endif
    }
}
