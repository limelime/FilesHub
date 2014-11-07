package net.xngo.fileshub.test.db;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import net.xngo.fileshub.Main;
import net.xngo.fileshub.Utils;
import net.xngo.fileshub.db.Manager;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.xngo.fileshub.db.Trash;
import net.xngo.fileshub.db.Connection;
import net.xngo.fileshub.struct.Document;
import net.xngo.fileshub.test.helpers.Data;
import net.xngo.utils.java.math.Random;

public class TrashTest
{
  private static final boolean DEBUG = true;
  
  
  private final int randomInt = java.lang.Math.abs(Random.Int())+1; // Must be positive.
  private AtomicInteger atomicInt = new AtomicInteger(randomInt);   // Must set initial value to more than 0. Duid can't be 0.  
  private Trash trash = new Trash();
  
  @BeforeClass
  public void DatabaseCreation()
  {
    // Make sure that the database file is created.
    Manager manager = new Manager();
    manager.createDbStructure();
  }
  
  @Test(description="Search by filename without wildcard: Test SQL query.")
  public void searchDocsByFilenameNotWildcardQuery()
  {
    //*** Prepare data: Add unique file in Trash table.
    File uniqueFile = Data.createTempFile("searchDocsByFilenameNotWildcard");
    Document trashDoc = new Document(uniqueFile);
    trashDoc.hash = Utils.getHash(uniqueFile);
    trashDoc.uid = atomicInt.get(); // Fake duid.
    this.trash.addDoc(trashDoc);
    
    //*** Main test: Search by filename without wildcard
    this.trash.searchDocsByFilename(uniqueFile.getName());
    
    
    //*** Validations: Check the sql query has equal sign.
    Connection connection = Main.connection;
    String expectedSQLquery = String.format("SELECT duid, canonical_path, filename, last_modified, hash, comment FROM Trash WHERE filename = ? : %s", uniqueFile.getName());
    assertEquals(connection.getQueryString(), expectedSQLquery);
    
    //*** Clean up.
    uniqueFile.delete();    
  }
  
  @Test(description="Search by filename with wildcard: Test SQL query.")
  public void searchDocsByFilenameWildcardQuery()
  {
    //*** Prepare data: Add unique file in Trash table.
    File uniqueFile = Data.createTempFile("searchDocsByFilenameWildcardQuery");
    Document trashDoc = new Document(uniqueFile);
    trashDoc.hash = Utils.getHash(uniqueFile);
    trashDoc.uid = atomicInt.get(); // Fake duid.
    this.trash.addDoc(trashDoc);
    
    //*** Main test: Search by filename with wildcard
    final String filenameWildcard         = uniqueFile.getName()+"*";
    final String expectedFilenameWildcard = uniqueFile.getName()+"%";
    this.trash.searchDocsByFilename(filenameWildcard);
    
    
    //*** Validations: Check the sql query has the LIKE
    Connection connection = Main.connection;
    String expectedSQLquery = String.format("SELECT uid, canonical_path, filename, last_modified, hash, comment FROM Trash WHERE filename like ? : %s", expectedFilenameWildcard);
    assertEquals(connection.getQueryString(), expectedSQLquery);
    
    //*** Clean up.
    uniqueFile.delete();    
  }
  
  @Test(description="Search by filename with multiple adjacent wildcards: Test SQL query.")
  public void searchDocsByFilenameAdjacentWildcardQuery()
  {
    //*** Prepare data: Add unique file in Trash table.
    File uniqueFile = Data.createTempFile("searchDocsByFilenameAdjacentWildcardQuery");
    Document trashDoc = new Document(uniqueFile);
    trashDoc.hash = Utils.getHash(uniqueFile);
    trashDoc.uid = atomicInt.get(); // Fake duid.
    this.trash.addDoc(trashDoc);
    
    //*** Main test: Search by filename with wildcard
    final String filenameWildcard         = uniqueFile.getName()+"**";
    final String expectedFilenameWildcard = uniqueFile.getName()+"%";
    this.trash.searchDocsByFilename(filenameWildcard);
    
    
    //*** Validations: Check the sql query has not adjacent %.
    Connection connection = Main.connection;
    String expectedSQLquery = String.format("SELECT duid, canonical_path, filename, last_modified, hash, comment FROM Trash WHERE filename like ? : %s", expectedFilenameWildcard);
    assertEquals(connection.getQueryString(), expectedSQLquery);

    //*** Clean up.
    uniqueFile.delete();    
  }  
  
  
  @Test(description="Search by exact filename.")
  public void searchDocsByFilenameExact()
  {
    //*** Prepare data: Add unique file in Trash table.
    File uniqueFile = Data.createTempFile("searchDocsByFilenameExact");
    Document trashDoc = new Document(uniqueFile);
    trashDoc.hash = Utils.getHash(uniqueFile);
    trashDoc.uid = atomicInt.get(); // Fake duid.
    this.trash.addDoc(trashDoc);
    
    //*** Main test: Search by exact filename.
    List<Document> searchResults = this.trash.searchDocsByFilename(uniqueFile.getName());
    
    //*** Validations: Filename should be found.
    assertEquals(searchResults.size(), 1, String.format("%s should be found in Trash.", uniqueFile.getName()));
    assertEquals(searchResults.get(0).filename, uniqueFile.getName());
    
    //*** Clean up.
    uniqueFile.delete();
  }
  
  @Test(description="Search by filename using wildcard.")
  public void searchDocsByFilenameUsingWildcard()
  {
    if(this.DEBUG)
    {
      try
      {
        Main.connection.setAutoCommit(true);
      }
      catch(SQLException ex) { ex.printStackTrace(); }
    }
    
    //*** Prepare data: Add multiple files in Trash table.
    ArrayList<File> files = new ArrayList<File>();
    final String filenamePattern = "searchDocsByFilenameUsingWildcard_TrashTable_";
    for(int i=0; i<7; i++ )
    {
      File uniqueFile = Data.createTempFile(filenamePattern+i);
      Document trashDoc = new Document(uniqueFile);
      trashDoc.hash = Utils.getHash(uniqueFile);
      trashDoc.uid = atomicInt.get(); // Fake duid.
      this.trash.addDoc(trashDoc);
    }
    
    //*** Main test: Search by exact filename.
    List<Document> searchResults = this.trash.searchDocsByFilename("*ByFilename*UsingWildcard*TrashTable");
    
    //*** Validations: Filename should be found.
    assertEquals(searchResults.size(), files.size(), "Number of files found in Trash table not expected.");
    for(int i=0; i<files.size(); i++)
    {
      assertNotEquals(searchResults.get(i).filename.indexOf(filenamePattern), -1, String.format("Pattern '%s' not found in %s.", filenamePattern, searchResults.get(i).filename));
    }
    
    //*** Clean up.
    for(File file: files)
    {
      file.delete();
    }

  } 
  
  
  
}