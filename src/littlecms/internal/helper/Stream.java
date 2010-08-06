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
package littlecms.internal.helper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
import net.rim.device.api.io.Seekable;
//#endif

/**
 * Generic data stream for files and the likes.
 */
public abstract class Stream
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
	implements Seekable
//#endif
{
	public static final int SEEK_SET = 0;
	public static final int SEEK_CUR = 1;
	public static final int SEEK_END = 2;
	
	public abstract long getPosition();
	
	public void setPosition(long pos)
	{
		seek(pos, SEEK_SET);
	}
	
	public abstract int seek(long offset, int origin);
	
	/**
	 * @param buffer The location data should be read to.
	 * @param offset The offset in the buffer to where data should start being written to.
	 * @param size The size of an item to read
	 * @param count The number of items of <i>size</i> length to read.
	 * @return Number of items read in
	 */
	public abstract long read(byte[] buffer, int offset, int size, int count);
	
	/**
	 * @param buffer The location data should be read from.
	 * @param offset The offset in the buffer to where data should start being read from.
	 * @param size The size of an item to write
	 * @param count The number of items of <i>size</i> length to write.
	 * @return Number of items written
	 */
	public abstract long write(byte[] buffer, int offset, int size, int count);
	
	public abstract int close();
	
	public static Stream fopen(final String filename, final char mode)
	{
		return new FileStream(filename, mode);
	}
	
	private static class FileStream extends Stream
	{
		private FileConnection file;
		private InputStream in;
		private OutputStream out;
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
		private Seekable inSeek;
		private Seekable outSeek;
//#else
		private long pos;
//#endif
		
		public FileStream(final String filename, final char mode)
		{
			int iMode = Connector.READ_WRITE;
			switch(mode)
			{
				case 'r':
					iMode = Connector.READ;
					break;
				case 'w':
					iMode = Connector.WRITE;
					break;
			}
			try
			{
				file = (FileConnection)Connector.open(filename, iMode);
				if((iMode & Connector.READ) != 0)
				{
					in = file.openInputStream();
				}
				if((iMode & Connector.WRITE) != 0)
				{
					out = file.openOutputStream();
				}
			}
			catch (IOException e)
			{
			}
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
			if((iMode & Connector.READ) != 0)
			{
				inSeek = (Seekable)in;
			}
			if((iMode & Connector.WRITE) != 0)
			{
				outSeek = (Seekable)out;
			}
//#else
			pos = 0;
//#endif
		}
		
		public String getFilename()
		{
			return file.getURL();
		}
		
		public int getMode()
		{
			int mode = 0;
			if(in != null)
			{
				mode |= Connector.READ;
			}
			if(out != null)
			{
				mode |= Connector.WRITE;
			}
			return mode;
		}
		
		public long write(byte[] buffer, int offset, int size, int count)
		{
			if(out == null)
			{
				return 0;
			}
			count = Math.min(buffer.length / size, count) * size;
			int written = 0;
			try
			{
				out.write(buffer, offset, count);
				out.flush();
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
				pos += count;
//#endif
				if(in != null)
				{
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
					inSeek.setPosition(inSeek.getPosition() + count);
//#else
					in.read(new byte[count]);
//#endif
				}
			}
			catch(IOException ioe)
			{
				written = 0;
			}
			return written / size;
		}
		
		public int seek(long offset, int origin)
		{
			if(offset == 0)
			{
				return 0; //No change
			}
			long absPos = getPosition();
			switch(origin)
			{
				case SEEK_SET:
					absPos = offset;
					break;
				case SEEK_CUR:
					absPos += offset;
					break;
				case SEEK_END:
					try
					{
						absPos = file.fileSize() + offset;
					}
					catch(IOException ioe)
					{
					}
					break;
			}
			try
			{
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
				if(in != null)
				{
					inSeek.setPosition(absPos);
				}
				if(out != null)
				{
					outSeek.setPosition(absPos);
				}
//#else
				if(getPosition() < absPos)
				{
					if(in != null)
					{
						in.close();
						in = file.openInputStream();
						in.read(new byte[(int)absPos]);
					}
					if(out != null)
					{
						out.flush();
						out.close();
						out = file.openOutputStream(absPos);
					}
				}
				else
				{
					if(in != null)
					{
						in.read(new byte[(int)(absPos - pos)]);
					}
					if(out != null)
					{
						out.flush();
						out.close();
						out = file.openOutputStream(absPos);
					}
				}
				pos = absPos;
//#endif
			}
			catch(IOException ioe)
			{
			}
			return getPosition() == absPos ? 0 : 1;
		}
		
		public long read(byte[] buffer, int offset, int size, int count)
		{
			if(in != null)
			{
				return 0;
			}
			count = Math.min(buffer.length / size, count) * size;
			int read;
			try
			{
				read = in.read(buffer, offset, count);
//#ifdef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
				pos += read;
//#endif
				if(out != null)
				{
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
					outSeek.setPosition(outSeek.getPosition() + read);
//#else
					out.flush();
					out.close();
					out = file.openOutputStream(pos);
//#endif
				}
			}
			catch(IOException ioe)
			{
				read = 0;
			}
			return read / size;
		}
		
		public long getPosition()
		{
//#ifndef BlackBerrySDK4.5.0 | BlackBerrySDK4.6.0 | BlackBerrySDK4.6.1 | BlackBerrySDK4.7.0
			try
			{
				if(in != null)
				{
					return inSeek.getPosition();
				}
				else
				{
					return outSeek.getPosition();
				}
			}
			catch(IOException ioe)
			{
			}
//#else
			return this.pos;
//#endif
		}
		
		public int close()
		{
			try
			{
				if(in != null)
				{
					in.close();
				}
				if(out != null)
				{
					out.close();
				}
				file.close();
			}
			catch (IOException e)
			{
				return -1;
			}
			return 0;
		}
	}
}