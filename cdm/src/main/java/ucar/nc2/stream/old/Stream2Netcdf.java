/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.stream.old;

import ucar.ma2.*;
import ucar.nc2.*;

import java.util.List;
import java.util.ArrayList;
import java.io.*;
import java.nio.ByteBuffer;

/**
 * Write a netcdf stream to a NetCDF-3 file.
 *
 * @author caron
 * @since Jul 18, 2007
 */
public class Stream2Netcdf {

  private NetcdfFileWriteable ncfile;
  private Structure record;
  private DataInputStream in;
  private boolean debug = true;

  /**
   * Read a "stream format"  and write it to a Netcdf-3 file
   *
   * @param ncfile write to this NetcdfFileWriteable
   * @param in     read from this stream
   * @throws IOException on i/o error
   * @throws ucar.ma2.InvalidRangeException if youve been bad
   */
  public Stream2Netcdf(NetcdfFileWriteable ncfile, DataInputStream in) throws IOException, InvalidRangeException {
    this.ncfile = ncfile;
    this.in = in;

    byte[] magicb = new byte[8];
    readBytes(magicb);
    String magicS = new String(magicb);
    if (!magicS.equals(StreamWriter.MAGIC_HEADER))
      throw new IllegalArgumentException("Not a NetCDF Stream file");
    String magic = readHeader();

    while (true) {

      try {
        if (magic.equals(StreamWriter.HEADER)) {
          magic = readHeader(); // typically will return MAGIC_DATA
        }

        if (magic.equals(StreamWriter.DATA)) {
          if (ncfile.isDefineMode()) {
            ncfile.create(); // must leave define mode
            if (record != null) {
              ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);  // kludge - ncwrite should accept Structure ??
              record = (Structure) ncfile.findVariable("record");
            }
            //ucar.unidata.io.RandomAccessFile.setDebugAccess(true);
          }

          readData();
          magic = readMagic();

          /* }  else if (magic.equals(StreamWriter.MAGIC_EOF)) {
         break; */

        } else {
          throw new IllegalStateException("BAD MAGIC " + magic);
        }
      } catch (EOFException eof) {
        break;
      }
    }


  }

  String readMagic() throws IOException {
    byte[] magic = new byte[4];
    readBytes(magic);
    String magicS = new String(magic);
    if (magicS.equals(StreamWriter.MAGIC)) return readMagic();
    return magicS;
  }

  private String readHeader() throws IOException {
    Group root = ncfile.getRootGroup();

    // keep reading sections till not a header section.
    while (true) {
      String magic = readMagic();

      if (magic.equals(StreamWriter.HEADER))
        magic = readMagic();

      if (magic.equals(StreamWriter.MAGIC_ATTS))
        readAtts(root.getAttributes());

      else if (magic.equals(StreamWriter.MAGIC_DIMS))
        readDims(root.getDimensions());

      else if (magic.equals(StreamWriter.MAGIC_VARS))
        readVars(root.getVariables());

      else return magic;
    }

  }


  private void readAtts(List<Attribute> atts) throws IOException {
    int natts = readVInt();
    if (natts == 0) return;

    for (int i = 0; i < natts; i++) {
      String name = readString();
      int type = readVInt();
      int nvals = readVInt();
      DataType dt = StreamWriter.getDataType(type);
      Array data = Array.factory(dt, new int[]{nvals});
      readValues(data.getIndexIterator(), dt);
      atts.add(new Attribute(name, data));
    }
  }


  private void readValues(IndexIterator ii, DataType dt) throws IOException {
    if (dt == DataType.BYTE) {
      while (ii.hasNext())
        ii.setByteNext(in.readByte());

    } else if (dt == DataType.CHAR) {
      while (ii.hasNext())
        ii.setCharNext((char) in.readByte());

    } else if (dt == DataType.SHORT) {
      while (ii.hasNext())
        ii.setShortNext(in.readShort());

    } else if (dt == DataType.INT) {
      while (ii.hasNext())
        ii.setIntNext(in.readInt());

    } else if (dt == DataType.FLOAT) {
      while (ii.hasNext())
        ii.setFloatNext(in.readFloat());

    } else if (dt == DataType.DOUBLE) {
      while (ii.hasNext())
        ii.setDoubleNext(in.readDouble());

    } else if (dt == DataType.STRING) {
      while (ii.hasNext())
        ii.setObjectNext(readString());

    } else {
      throw new IllegalStateException("unknown data type == " + dt);
    }
  }

  void readDims(List<Dimension> dims) throws IOException {
    int ndims = readVInt();
    for (int i = 0; i < ndims; i++) {
      String name = readString();
      int length = readVInt();

      int flags = (int) readByte();
      boolean isShared = (flags & 1) != 0;
      boolean isUnlimited = (flags & 2) != 0;
      boolean isVariableLength = (flags & 4) != 0;

      dims.add(new Dimension(name, length, isShared, isUnlimited, isVariableLength));
    }
  }

  void readVars(List<Variable> vars) throws IOException {
    int nvars = readVInt();
    for (int i = 0; i < nvars; i++) {
      String name = readString();
      int type = readVInt();
      DataType dt = StreamWriter.getDataType(type);
      if (debug) System.out.println("  var= " + name + " type = " + type + " dataType = " + dt);

      List<Dimension> dims = new ArrayList<Dimension>();
      readDims(dims);
      List<Attribute> atts = new ArrayList<Attribute>();
      readAtts(atts);

      if (dt == DataType.STRUCTURE) { // LOOK ties this to a netcdf-3 on the other end
        record = new Structure(ncfile, null, null, name);
        record.setDimensions(dims);
        record.getAttributes().addAll(atts);

      } else if (name.startsWith("record.")) {
        String shortName = name.substring(7);

        // add to record as it is
        Variable mv = new Variable(ncfile, null, record, shortName);
        mv.setDataType(dt);
        mv.setDimensions(dims);
        mv.getAttributes().addAll(atts);
        record.addMemberVariable(mv);

        // munge into a top variable for NetcdfFileWriteable
        Dimension udim = record.getDimension(0);
        assert udim != null;
        assert udim.isUnlimited();

        Variable v = new Variable(ncfile, null, null, shortName);
        v.setDataType(dt);
        dims.add(0, udim); // unlimited dimension is first
        v.setDimensions(dims);
        v.getAttributes().addAll(atts);
        vars.add(v);

      } else {
        Variable v = new Variable(ncfile, null, null, name);
        v.setDataType(dt);
        v.setDimensions(dims);
        v.getAttributes().addAll(atts);
        vars.add(v);
      }
    }
  }


  public void readData() throws IOException, InvalidRangeException {
    String name = readString();
    int elemSize = readVInt();
    Section s = readSection();

    Variable v = name.equals("record") ? record : ncfile.findVariable(name);
    assert v != null : "cant find " + name;
    DataType dt = v.getDataType();

    if (debug)
      System.out.println("  var= " + name + " datatype = " + dt + " elemSize= " + elemSize + " section = " + s);

    Array data;
    if (dt == DataType.STRUCTURE) {
      data = readStructureData((Structure) v, s);
      ncfile.write(name, s.getOrigin(), data);
    } else {
      data = Array.factory(dt, s.getShape());
      readValues(data.getIndexIterator(), v.getDataType());
      ncfile.write(name, s.getOrigin(), data);
    }
  }

  private ArrayStructure readStructureData(Structure v, Section s) throws IOException {
    StructureMembers sm = v.makeStructureMembers();
    int offset = 0;
    for (StructureMembers.Member m : sm.getMembers()) {
      m.setDataParam(offset);
      offset += m.getSizeBytes();
    }
    sm.setStructureSize(offset);

    int size = (int) (sm.getStructureSize() * s.computeSize());
    byte[] ba = new byte[size];
    readBytes(ba);
    ByteBuffer bb = ByteBuffer.wrap(ba);
    return new ArrayStructureBB(sm, s.getShape(), bb, 0);
  }


  public Section readSection() throws IOException, InvalidRangeException {
    int rank = readVInt();
    Section s = new Section();
    for (int i = 0; i < rank; i++) {
      int first = readVInt();
      int length = readVInt();
      s.appendRange(new Range(first, first + length - 1));
    }
    return s;
  }

  ////////////////////////////////////////
  // from org.apache.lucene.store.IndexOutput

  private byte readByte() throws IOException {
    return in.readByte();
  }

  private void readBytes(byte[] b) throws IOException {
    in.readFully(b, 0, b.length);
  }

  private int readVInt() throws IOException {
    byte b = readByte();
    int i = b & 0x7F;
    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
      b = readByte();
      i |= (b & 0x7F) << shift;
    }
    return i;
  }

  //private char[] chars;
  public String readString() throws IOException {
    int length = readVInt();
    // if (chars == null || length > chars.length)
    char[] chars = new char[length];
    readChars(chars, 0, length);
    return new String(chars, 0, length);
  }

  /**
   * Reads UTF-8 encoded characters into an array.
   *
   * @param buffer the array to read characters into
   * @param start  the offset in the array to start storing characters
   * @param length the number of characters to read
   */
  public void readChars(char[] buffer, int start, int length) throws IOException {
    final int end = start + length;
    for (int i = start; i < end; i++) {
      byte b = readByte();
      if ((b & 0x80) == 0)
        buffer[i] = (char) (b & 0x7F);
      else if ((b & 0xE0) != 0xE0) {
        buffer[i] = (char) (((b & 0x1F) << 6)
            | (readByte() & 0x3F));
      } else
        buffer[i] = (char) (((b & 0x0F) << 12)
            | ((readByte() & 0x3F) << 6)
            | (readByte() & 0x3F));
    }
  }


}