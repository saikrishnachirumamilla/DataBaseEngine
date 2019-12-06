package team.magenta.db;

import java.io.RandomAccessFile;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.SortedMap;

import java.util.ArrayList;
import java.lang.Math.*;
import java.util.Arrays;
import static java.lang.System.out;
import java.util.List;
import java.util.Map;

public class DavisBaseBinaryFile {

   public static String columnsTable = "davisbase_columns";
   public static String tablesTable = "davisbase_tables";
   public static boolean showRowId = false;
   public static boolean startedDataStore = false;

   static int pageSizePower = 9; 
   static int pageSize = (int) Math.pow(2, pageSizePower);
   RandomAccessFile file;
   
   public DavisBaseBinaryFile(RandomAccessFile file) {
      this.file = file;
   }

   public boolean rowExsistence(TableMetaData tablemetaData, List<String> colNames, Criteria criteria) throws IOException{
	   
   BPlusTree objectOfBPTree = new BPlusTree(file, tablemetaData.rootPageNumber, tablemetaData.tableName);
   for(Integer pageNumber :  objectOfBPTree.getAllLeavesOfTree(criteria)){
         Page page = new Page(file,pageNumber);
         for(Row row : page.getPageRecords()){
            if(criteria!=null){
               if(!criteria.checkCriteria(row.getProperties().get(criteria.ordinal).cellValue))
                  continue;
            }
           return true;
         }
   }
   return false;

   }


   
   public int updateRows(TableMetaData tablemetaData,Criteria criteria, List<String> colNames, List<String> updateValues) throws IOException{
      int count = 0;


      List<Integer> ordinal = tablemetaData.getOrdinalPostions(colNames);
      int k=0;
      Map<Integer,Property> eachValueMap = new HashMap<>();

      for(String eachUpdateValue:updateValues){
           int i = ordinal.get(k);

         try{
        	 eachValueMap.put(i,new Property(tablemetaData.columnNameProperties.get(i).dataType,eachUpdateValue));
                      }
                      catch (Exception e) {
							System.out.println("! Invalid data format for " + tablemetaData.colNames.get(i) + " values: "+ eachUpdateValue);
							return count;
						}

         k++;
      }

      BPlusTree objectOfBPTree = new BPlusTree(file, tablemetaData.rootPageNumber,tablemetaData.tableName);
   
      for(Integer pageNumber :  objectOfBPTree.getAllLeavesOfTree(criteria)){
            short dNumber = 0;
            Page page = new Page(file,pageNumber);
            for(Row row : page.getPageRecords()){
               if(criteria!=null){
                  if(!criteria.checkCriteria(row.getProperties().get(criteria.ordinal).cellValue))
                     continue;
               }
               count++;
               for(int i :eachValueMap.keySet()){
                  Property oVal = row.getProperties().get(i);
                  int rowId = row.rowId;
                  if((row.getProperties().get(i).dataType == DataType.TEXT
                   && row.getProperties().get(i).cellValue.length() == eachValueMap.get(i).cellValue.length())
                     || (row.getProperties().get(i).dataType != DataType.NULL && row.getProperties().get(i).dataType != DataType.TEXT)
                  ){
                     page.recordUpdate(row,i,eachValueMap.get(i).cellValueLarge);
                  }
                  else{
                     page.tableRowDeletion(tablemetaData.tableName ,Integer.valueOf(row.pageHeaderIValue - dNumber).shortValue());
                     dNumber++;
                     List<Property> props = row.getProperties();
                     Property prop = props.get(i);
                     props.remove(i);
                     prop = eachValueMap.get(i);
                     props.add(i, prop);
                     rowId =  page.insertTableRow(tablemetaData.tableName , props);
                }
                
                if(tablemetaData.columnNameProperties.get(i).hasIndex && criteria!=null){
                  RandomAccessFile iF = new RandomAccessFile(DavisBasePrompt.getNDXFilePath(tablemetaData.columnNameProperties.get(i).tableName, tablemetaData.columnNameProperties.get(i).name), "rw");
                  BTree objectOfBtree = new BTree(iF);
                  objectOfBtree.deleteNode(oVal,row.rowId);
                  objectOfBtree.insertNode(eachValueMap.get(i), rowId);
                  iF.close();
                }
                
               }
             }
      }
    
      if(!tablemetaData.tableName.equals(tablesTable) && !tablemetaData.tableName.equals(columnsTable))
          System.out.println(count+" rows updated.");
          
         return count;

   }

   public void selectRows(TableMetaData tablemetaData, List<String> colNames, Criteria criteria) throws IOException{
   
   List<Integer> ordinalList = tablemetaData.getOrdinalPostions(colNames);
   System.out.println();
   List<Integer> pLocation = new ArrayList<>();
   int pLen = 0;
   pLocation.add(pLen);
   int tPLen =0;
   if(showRowId){
      System.out.print("rowid");
      System.out.print(DavisBasePrompt.line(" ",5));
      pLocation.add(10);
      tPLen +=10;
   }

   
   for(int i:ordinalList){
      String columnName = tablemetaData.columnNameProperties.get(i).name;
      pLen = Math.max(columnName.length(),tablemetaData.columnNameProperties.get(i).dataType.getPrintOffset()) + 5;
      pLocation.add(pLen);
      System.out.print(columnName);
      System.out.print(DavisBasePrompt.line(" ",pLen - columnName.length() ));
      tPLen +=pLen;
   }
       System.out.println();
       System.out.println(DavisBasePrompt.line("-",tPLen));

   BPlusTree objectBPtree= new BPlusTree(file, tablemetaData.rootPageNumber,tablemetaData.tableName);
    String pVal ="";
   for(Integer pageNumber : objectBPtree.getAllLeavesOfTree(criteria)){
         Page page = new Page(file,pageNumber);
         for(Row row : page.getPageRecords()){
            if(criteria!=null){
               if(!criteria.checkCriteria(row.getProperties().get(criteria.ordinal).cellValue))
                  continue;
            }
            int attCount = 0;
            if(showRowId){
            	pVal = Integer.valueOf(row.rowId).toString();
                  System.out.print(pVal);
                  System.out.print(DavisBasePrompt.line(" ",pLocation.get(++attCount) - pVal.length()));
            }
            for(int i :ordinalList){
            	pVal = row.getProperties().get(i).cellValue;
               System.out.print(pVal);
               System.out.print(DavisBasePrompt.line(" ",pLocation.get(++attCount) - pVal.length()));
            }
            System.out.println();
         }
   }
    System.out.println();
   }
       
   public static int retrievePageNumber(RandomAccessFile bF) {
     int rootpage = 0;
      try {   
         for (int i = 0; i < bF.length() / DavisBaseBinaryFile.pageSize; i++) {
            bF.seek(i * DavisBaseBinaryFile.pageSize + 0x0A);
            int a =bF.readInt();
          
            if (a == -1) {
               return i;
            }
         }
         return rootpage;
      } catch (Exception e) {
         out.println("error while getting root page no ");
         out.println(e);
      }
      return -1;

   }


   public static void runDataToBase() {
      try {
         File dataPath = new File("data");
         dataPath.mkdir();
         String[] exsistingTableFiles;
         exsistingTableFiles = dataPath.list();
         for (int i = 0; i < exsistingTableFiles.length; i++) {
            File eachExsistingFile = new File(dataPath, exsistingTableFiles[i]);
            eachExsistingFile.delete();
         }
      } catch (SecurityException e) {
         out.println("data Directory creation error!");
         out.println(e);
      }
      try {
         
         int presentPageNumber = 0;
         RandomAccessFile davisbaseTablesInventory = new RandomAccessFile(DavisBasePrompt.getTBLFilePath(tablesTable), "rw");
         Page.newPageAddition(davisbaseTablesInventory, TypeOfPage.LEAF, -1, -1);
         Page page = new Page(davisbaseTablesInventory,presentPageNumber);
         page.insertTableRow(tablesTable,Arrays.asList(new Property[] { 
               new Property(DataType.TEXT, DavisBaseBinaryFile.tablesTable),
               new Property(DataType.INT, "2"), 
               new Property(DataType.SMALLINT, "0"),
               new Property(DataType.SMALLINT, "0") 
               }));
         page.insertTableRow(tablesTable,Arrays.asList(new Property[] {
               new Property(DataType.TEXT, DavisBaseBinaryFile.columnsTable),
                new Property(DataType.INT, "11"),
               new Property(DataType.SMALLINT, "0"),
                new Property(DataType.SMALLINT, "2") }));
         davisbaseTablesInventory.close();
      } catch (Exception e) {
         out.println("database_tables file creation not occurred");
         out.println(e);
      }
      try {
         RandomAccessFile davisbaseColumnsInventory = new RandomAccessFile(DavisBasePrompt.getTBLFilePath(columnsTable), "rw");
         Page.newPageAddition(davisbaseColumnsInventory, TypeOfPage.LEAF, -1, -1);
         Page page = new Page(davisbaseColumnsInventory, 0);

         short ordinal = 1;
         page.insertNewColumn(new ColumnMetaData(tablesTable,DataType.TEXT, "table_name", true, false, ordinal++));
         page.insertNewColumn(new ColumnMetaData(tablesTable,DataType.INT, "record_count", false, false, ordinal++));
         page.insertNewColumn(new ColumnMetaData(tablesTable,DataType.SMALLINT, "avg_length", false, false, ordinal++));
         page.insertNewColumn(new ColumnMetaData(tablesTable,DataType.SMALLINT, "root_page", false, false, ordinal++));
      

         ordinal = 1;

         page.insertNewColumn(new ColumnMetaData(columnsTable,DataType.TEXT, "table_name", false, false, ordinal++));
         page.insertNewColumn(new ColumnMetaData(columnsTable,DataType.TEXT, "column_name", false, false, ordinal++));
         page.insertNewColumn(new ColumnMetaData(columnsTable,DataType.SMALLINT, "data_type", false, false, ordinal++));
         page.insertNewColumn(new ColumnMetaData(columnsTable,DataType.SMALLINT, "ordinal_position", false, false, ordinal++));
         page.insertNewColumn(new ColumnMetaData(columnsTable,DataType.TEXT, "is_nullable", false, false, ordinal++));
         page.insertNewColumn(new ColumnMetaData(columnsTable,DataType.SMALLINT, "column_key", false, true, ordinal++));
         page.insertNewColumn(new ColumnMetaData(columnsTable,DataType.SMALLINT, "is_unique", false, false, ordinal++));

         davisbaseColumnsInventory.close();
         startedDataStore = true;
      } catch (Exception e) {
         out.println("database_columns file creation not occured!");
         out.println(e);
      }
   }
}



