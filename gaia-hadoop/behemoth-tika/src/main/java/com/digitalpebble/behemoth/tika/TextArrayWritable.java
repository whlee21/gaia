 package com.digitalpebble.behemoth.tika;
 
 import java.io.DataInput;
 import java.io.DataOutput;
 import java.io.IOException;
 import org.apache.hadoop.io.Writable;
 
 public class TextArrayWritable
   implements Writable
 {
   private String[] array;
 
   public TextArrayWritable(String[] array)
   {
     this.array = array;
   }
 
   public String[] getArray() {
     return this.array;
   }
 
   public void setArray(String[] array) {
     this.array = array;
   }
 
   public void write(DataOutput dataOutput) throws IOException
   {
     if (this.array != null) {
       dataOutput.writeInt(this.array.length);
       for (int i = 0; i < this.array.length; i++)
         dataOutput.writeUTF(this.array[i]);
     }
     else {
       dataOutput.writeInt(0);
     }
   }
 
   public void readFields(DataInput dataInput) throws IOException
   {
     int len = dataInput.readInt();
     if (len > 0) {
       this.array = new String[len];
       for (int i = 0; i < len; i++)
         this.array[i] = dataInput.readUTF();
     }
   }
 }

