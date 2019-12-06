package team.magenta.db;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Condition;
public class TableMetaData{
    public int rowCount;
    public List<Row> columnData;
    public List<ColumnMetaData> columnNameProperties;
    public List<String> colNames;
    public String tableName;
    public boolean tableExistence;
    public int rootPageNumber;
    public int finalRowId;
    public TableMetaData(String tableName){
        this.tableName = tableName;
        tableExistence = false;
        try {
            RandomAccessFile davisbaseTablesInventory = new RandomAccessFile(DavisBasePrompt.getTBLFilePath(DavisBaseBinaryFile.tablesTable), "r");            
            int rootPageNo = DavisBaseBinaryFile.retrievePageNumber(davisbaseTablesInventory);
            BPlusTree bplusTree = new BPlusTree(davisbaseTablesInventory, rootPageNo,tableName);
            for (Integer pageNo : bplusTree.getAllLeavesOfTree()) {
               Page page = new Page(davisbaseTablesInventory, pageNo);
               for (Row row : page.getPageRecords()) {
                  if (new String(row.getProperties().get(0).cellValue).equals(tableName)) {
                    this.rootPageNumber = Integer.parseInt(row.getProperties().get(3).cellValue);
                    rowCount = Integer.parseInt(row.getProperties().get(1).cellValue);
                    tableExistence = true;
                     break;
                  }
               }
               if(tableExistence)
                break;
            }
            davisbaseTablesInventory.close();
            if(tableExistence){
               loadColumnData();
            } else {
               throw new Exception("Table does not exist.");
            }
            
         } catch (Exception e) {
        	 System.out.println(e.getMessage());
         }
    }
    public List<Integer> getOrdinalPostions(List<String> cols){
		List<Integer> location = new ArrayList<>();
		for(String column :cols){
			location.add(colNames.indexOf(column));
        }
        return location;
    }
private void loadColumnData() {
	    try {
	       RandomAccessFile davisbaseTablesInventory = new RandomAccessFile(DavisBasePrompt.getTBLFilePath(DavisBaseBinaryFile.columnsTable), "r");
		   int rootPageNo = DavisBaseBinaryFile.retrievePageNumber(davisbaseTablesInventory);
		   columnData = new ArrayList<>();
		   columnNameProperties = new ArrayList<>();
		   colNames = new ArrayList<>();
		   BPlusTree bPlusTree = new BPlusTree(davisbaseTablesInventory, rootPageNo,tableName);
		   for (Integer pageNo : bPlusTree.getAllLeavesOfTree()) {
		     Page page = new Page(davisbaseTablesInventory, pageNo);              
		      for (Row record : page.getPageRecords()) {                
		         if (record.getProperties().get(0).cellValue.equals(tableName)) {{
		               columnData.add(record);
		               colNames.add(record.getProperties().get(1).cellValue);
		               ColumnMetaData colInfo = new ColumnMetaData(tableName, DataType.get(record.getProperties().get(2).cellValue), record.getProperties().get(1).cellValue, record.getProperties().get(6).cellValue.equals("YES"), record.getProperties().get(4).cellValue.equals("YES"), Short.parseShort(record.getProperties().get(3).cellValue));                                         
		            if(record.getProperties().get(5).cellValue.equals("PRI"))
		                  colInfo.setPrimaryKey();                   
		            columnNameProperties.add(colInfo);
		            }
		         }
		      }
		   }
		   davisbaseTablesInventory.close();
		} catch (Exception e) {
		   System.out.println("Failure while getting data for " + tableName);
	        }
	  
     }
   public boolean columnExists(List<String> cols) {
    if(cols.size() == 0)
       return true;
    List<String> pCols =new ArrayList<String>();
    for (ColumnMetaData column_name_attr : columnNameProperties) {
       if (pCols.contains(column_name_attr.name))
    	   pCols.remove(column_name_attr.name);
    }
    return pCols.isEmpty();
 }
 public void updateMetaData(){
   try{
	   	 RandomAccessFile table = new RandomAccessFile(DavisBasePrompt.getTBLFilePath(tableName), "r");
         Integer rootPageNumber = DavisBaseBinaryFile.retrievePageNumber(table);
         table.close();
         RandomAccessFile davisbaseTablesInventory = new RandomAccessFile(DavisBasePrompt.getTBLFilePath(DavisBaseBinaryFile.tablesTable), "rw");
         DavisBaseBinaryFile tablesBFile = new DavisBaseBinaryFile(davisbaseTablesInventory);
         TableMetaData tablesMetaData = new TableMetaData(DavisBaseBinaryFile.tablesTable);       
         Criteria criteria = new Criteria(DataType.TEXT);
         criteria.setColumn("table_name");
         criteria.ordinal = 0;
         criteria.setCriteria(tableName);
         criteria.setOperation("=");
         List<String> columns = Arrays.asList("record_count","root_page");
         List<String> latestValues = new ArrayList<>();
         latestValues.add(new Integer(rowCount).toString());
         latestValues.add(new Integer(rootPageNumber).toString());
         tablesBFile.updateRows(tablesMetaData,criteria,columns,latestValues);                                         
         davisbaseTablesInventory.close();
   }
   catch(IOException e){
      System.out.println("! Error updating meta data for " + tableName);
   }
 }
 public boolean validateInsert(List<Property> row) throws IOException{
	 RandomAccessFile table = new RandomAccessFile(DavisBasePrompt.getTBLFilePath(tableName), "r");
	 DavisBaseBinaryFile file = new DavisBaseBinaryFile(table);
     for(int i=0;i<columnNameProperties.size();i++){
        Criteria criteria = new Criteria(columnNameProperties.get(i).dataType);
        criteria.NameOfColumn = columnNameProperties.get(i).name;
        criteria.ordinal = i;
        criteria.setOperation("=");
        if(columnNameProperties.get(i).isDistinct){
        	criteria.setCriteria(row.get(i).cellValue);
            if(file.rowExsistence(this, Arrays.asList(columnNameProperties.get(i).name), criteria)){
            	System.out.println("Column "+ columnNameProperties.get(i).name + " should be unique." );
            	table.close();
            	return false;
            }
        }
      }
     table.close();
	     return true;
	 }
}