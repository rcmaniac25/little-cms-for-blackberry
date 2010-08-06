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
                Console.WriteLine("linker <tag/type> <input file>");
                return;
            }
            bool tag = args[0].ToLower().Equals("tag");
            FileStream inFs = new FileStream(args[1], FileMode.Open);
            string outPath = string.Format("{0}Output.txt", Path.Combine(Path.GetDirectoryName(args[1]), Path.GetFileNameWithoutExtension(args[1])));
            FileStream outFs = new FileStream(outPath, FileMode.Create);
            convert(inFs, outFs, tag);
            outFs.Flush();
            outFs.Close();
            inFs.Close();
        }

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
    }
}
