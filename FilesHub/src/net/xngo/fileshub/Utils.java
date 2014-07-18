package net.xngo.fileshub;

import java.io.File;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import net.jpountz.xxhash.StreamingXXHash32;
import net.jpountz.xxhash.XXHashFactory;


/**
 * 
 * @author Xuan Ngo
 *
 */
public class Utils
{
  /**
   * Get all files from a directory and its sub-directories.
   * @param directoryPath to be listed
   */
  public ArrayList<File> getAllFiles(String directoryPath)
  {
    
    ArrayList<File> allFiles = new ArrayList<File>();
    
    File directory = new File(directoryPath);
    
    // Get all the files from a directory
    File[] fList = directory.listFiles();
    
    for (File file : fList)
    {
      if (file.isFile())
      {
          allFiles.add(file);
          return allFiles;
      }
      else if (file.isDirectory())
      {
        getAllFiles(file.getAbsolutePath());
      }
    }
    
    return allFiles;
  }
  
  /**
   * Get the hash(ID) of the file.
   * Note: -XXHash32 is chosen because it claims to be fast.
   *       -Check what is the collision rate of XXHash32 algorithm 
   *              because StreamingXXHash32.getValue() return an integer, 
   *              which has a limit of 2,147,483,648.
   * @param file
   * @return      the hash as string
   */
  public String getHash(File file)
  {
    XXHashFactory factory = XXHashFactory.fastestInstance();
    int seed = 0x9747b28c;  // used to initialize the hash value, use whatever
                            // value you want, but always the same
    StreamingXXHash32 hash32 = factory.newStreamingHash32(seed);
  
    try
    {
      byte[] bufferBlock = new byte[8192]; // 8192 bytes
      FileInputStream fileInputStream = new FileInputStream(file);
  
      int read;
      while ((read = fileInputStream.read(bufferBlock))!=-1) 
      {
        hash32.update(bufferBlock, 0, read);
      }
      
      fileInputStream.close();
      return hash32.getValue()+""; // Force to be a string so that if we can change to use another hashing algorithm.
      
    }
    catch(UnsupportedEncodingException ex)
    {
      System.out.println(ex);
    }
    catch(IOException ex)
    {
      System.out.println(ex);
    }
    
    return null;
  }
  
  /**
   * Source: http://examples.javacodegeeks.com/core-java/io/file/4-ways-to-copy-file-in-java/
   */
  public void copyFileUsingFileStreams(File source, File dest)
  {
    try 
    {
      InputStream input = new FileInputStream(source);
      OutputStream output = new FileOutputStream(dest);
      byte[] buf = new byte[8192];
      int bytesRead;
      while ((bytesRead = input.read(buf)) > 0) 
      {
        output.write(buf, 0, bytesRead);
      }
      
      input.close();
      output.close();      
    } 
    catch(FileNotFoundException ex)
    {
      System.out.println(ex);
    }
    catch(IOException ex)
    {
      System.out.println(ex);
    }      
  }
  
  
  /**
   * Source: http://examples.javacodegeeks.com/core-java/io/file/4-ways-to-copy-file-in-java/
   */
  public void copyFileUsingFileChannels(File source, File dest)
  {
    FileChannel inputChannel = null;
    FileChannel outputChannel = null;
    try 
    {
      inputChannel = new FileInputStream(source).getChannel();
      outputChannel = new FileOutputStream(dest).getChannel();
      outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
      
      inputChannel.close();
      outputChannel.close();      
    }
    catch(FileNotFoundException ex)
    {
      System.out.println(ex);
    }
    catch(IOException ex)
    {
      System.out.println(ex);
    }     
  }
  
  /**
   * copyNhash() combine the copying and the hashing process at the same time so that it only fetches data once from hard drive.
   * TODO Modify code to use FileChannel class. It is much faster.
   * 
   * @param source
   * @param dest
   * @return
   */
  public String copyNhash(File source, File dest)
  {
    XXHashFactory factory = XXHashFactory.fastestInstance();
    int seed = 0x9747b28c;  // used to initialize the hash value, use whatever
                            // value you want, but always the same
    StreamingXXHash32 hash32 = factory.newStreamingHash32(seed);
  
    try
    {
      byte[] bufferBlock = new byte[8192]; // 8192 bytes
      FileInputStream fileInputStream = new FileInputStream(source);
      OutputStream fileOutputStream = new FileOutputStream(dest);
  
      int read;
      while ((read = fileInputStream.read(bufferBlock))!=-1) 
      {
        hash32.update(bufferBlock, 0, read);  // Hash
        
        fileOutputStream.write(bufferBlock, 0, read); // Copy the file.
      }
      
      fileInputStream.close();
      fileOutputStream.close();
      return hash32.getValue()+""; // Force to be a string so that if we can change to use another hashing algorithm.
      
    }
    catch(UnsupportedEncodingException ex)
    {
      System.out.println(ex);
    }
    catch(IOException ex)
    {
      System.out.println(ex);
    }
    
    return null;    
  }
  
}