package team.magenta.db;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class Page {

  public TypeOfPage typeOfPage;
  short contentNumber = 0;
  public int pageNumber;
  short contentOffset;
  public int nextPage;
  public int parentPageNumber;
  private List<Row> rows;
  boolean reloadTableRows = false;
  long pageBegin;
  int endRow;
  int spaceavailability;
  RandomAccessFile randomAccessFile;
  List<TableRow> leftChild;
  public DataType indexDataType;
  public TreeSet<Long> longIndexValues;
  public TreeSet<String> stringIndexValues;
  public HashMap<String,IList> indexPointer;
  private Map<Integer,Row> mapOfRecords;
  
  public Page(RandomAccessFile randomAccessFile, int pageNumber) {
    try {
	      this.pageNumber = pageNumber;
	      indexDataType = null;
	      longIndexValues = new TreeSet<>();
	      stringIndexValues = new TreeSet<>();
	      indexPointer = new HashMap<String,IList>();
	      mapOfRecords = new HashMap<>();
	      this.randomAccessFile = randomAccessFile;
	      endRow = 0;
	      pageBegin = DavisBaseBinaryFile.pageSize * pageNumber;
	      randomAccessFile.seek(pageBegin);
	      typeOfPage = TypeOfPage.get(randomAccessFile.readByte());
	      randomAccessFile.readByte(); 
	      contentNumber = randomAccessFile.readShort();
	      contentOffset = randomAccessFile.readShort();
	      spaceavailability = contentOffset - 0x10 - (contentNumber *2); 
	      nextPage = randomAccessFile.readInt();
	      parentPageNumber = randomAccessFile.readInt();
	      randomAccessFile.readShort();
	      if (typeOfPage == TypeOfPage.LEAF)
	        populateTable();
	      if(typeOfPage == TypeOfPage.INNER)
	        leftChildrenPlacement();
	      if(typeOfPage == TypeOfPage.INDEXOFINNER || typeOfPage == TypeOfPage.INDEXOFLEAF)
	    	  populateIndexRows();
	    } catch (IOException ex) {
      System.out.println(ex.getMessage());
    }
  }

  public List<String> fetchIndexValues(){
      List<String> listOfStringIndexValues = new ArrayList<>();
      if(stringIndexValues.size() > 0)
    	  listOfStringIndexValues.addAll(Arrays.asList(stringIndexValues.toArray(new String[stringIndexValues.size()])));
       if(longIndexValues.size() > 0){
        Long[] lArray = longIndexValues.toArray(new Long[longIndexValues.size()]);
                for(int i=0;i<lArray.length;i++){
                	listOfStringIndexValues.add(lArray[i].toString());
        }
            }
              return listOfStringIndexValues;
  }

  public boolean isRoot(){
    return parentPageNumber == -1;
  }
  
  

  public static TypeOfPage getTypeOfPage(RandomAccessFile randomAccessFile,int pageNumber) throws IOException{
    try {
      int pageStart = DavisBaseBinaryFile.pageSize * pageNumber;
      randomAccessFile.seek(pageStart);
      return  TypeOfPage.get(randomAccessFile.readByte()); 
    } catch (IOException ex) {
      System.out.println("! Error while getting the page type " + ex.getMessage());
      throw ex;
    }
  }

  public static int newPageAddition(RandomAccessFile randomAccessFile,TypeOfPage typeOfPage, int nextPage, int parentPageNumber){
    try {
      int pageNumber = Long.valueOf((randomAccessFile.length()/DavisBaseBinaryFile.pageSize)).intValue();
      randomAccessFile.setLength(randomAccessFile.length() + DavisBaseBinaryFile.pageSize);
      randomAccessFile.seek(DavisBaseBinaryFile.pageSize * pageNumber);
      randomAccessFile.write(typeOfPage.getValue());
      randomAccessFile.write(0x00); 
      randomAccessFile.writeShort(0);
      randomAccessFile.writeShort((short)(DavisBaseBinaryFile.pageSize));
      randomAccessFile.writeInt(nextPage);
      randomAccessFile.writeInt(parentPageNumber);
      return pageNumber;
    } 
    catch (IOException ex) {
        System.out.println(ex.getMessage());
        return -1;
    }
  }

  public void recordUpdate(Row row,int ordinalPosition,Byte[] newValue) throws IOException{
	  randomAccessFile.seek(pageBegin + row.rowOffset + 7);
    int valueOffset = 0;
    for(int i=0;i<ordinalPosition;i++){
    
      valueOffset+= DataType.getLength((byte)randomAccessFile.readByte());
    }
    randomAccessFile.seek(pageBegin + row.rowOffset + 7 + row.columTypes.length + valueOffset);
    randomAccessFile.write(ByteConvertor.Bytestobytes(newValue));     
  }


  public void insertNewColumn(ColumnMetaData data) throws IOException{
    try {
    	insertTableRow(DavisBaseBinaryFile.columnsTable, Arrays.asList(new Property[] { 
        new Property(DataType.TEXT, data.tableName),
        new Property(DataType.TEXT, data.name),
        new Property(DataType.TEXT, data.dataType.toString()),
        new Property(DataType.SMALLINT, data.location.toString()), 
        new Property(DataType.TEXT, data .isNull ? "YES":"NO"),data.primaryKey ? 
        new Property(DataType.TEXT, "PRI") : new Property(DataType.NULL, "NULL") ,
        new Property(DataType.TEXT, data.isDistinct ? "YES": "NO")
       })); 
    } catch (Exception e) {
      System.out.println("! Could not add column");
    }
  }
 
public int insertTableRow(String tableName,List<Property> properties) throws IOException{
      List<Byte> colDT = new ArrayList<Byte>();
      List<Byte> rowBody = new ArrayList<Byte>();

      TableMetaData metaData  = null;
      if(DavisBaseBinaryFile.startedDataStore){
        metaData = new TableMetaData(tableName);
        if(!metaData.validateInsert(properties))
            return -1;
      }

      for(Property property : properties){
        rowBody.addAll(Arrays.asList(property.cellValueLarge));
        if(property.dataType == DataType.TEXT){
        	colDT.add(Integer.valueOf(DataType.TEXT.getValue() + (new String(property.cellValue).length())).byteValue());
          }
        else{
        	colDT.add(property.dataType.getValue());
          }
        }
      endRow++;
        short LoadSize = Integer.valueOf(rowBody.size() + colDT.size() + 1).shortValue();       
        List<Byte> rowHeader = new ArrayList<>();
        rowHeader.addAll(Arrays.asList(ByteConvertor.shortToBytes(LoadSize)));
        rowHeader.addAll(Arrays.asList(ByteConvertor.intToBytes(endRow)));
        rowHeader.add(Integer.valueOf(colDT.size()).byteValue());
        rowHeader.addAll(colDT);
        newpagerowAddition(rowHeader.toArray(new Byte[rowHeader.size()]), rowBody.toArray(new Byte[rowBody.size()]));
        reloadTableRows = true;
	    if(DavisBaseBinaryFile.startedDataStore){
	       metaData.rowCount++;
	       metaData.updateMetaData();
	    }
	    return endRow;
  }
  
	public List<Row> getPageRecords(){
		if(reloadTableRows)
		   populateTable();
		reloadTableRows = false;
    	return rows;
	}

  private void pageRowDeletion(short rowIndex){
    try{

      for (int i = rowIndex + 1; i < contentNumber; i++) { 
    	  randomAccessFile.seek(pageBegin + 0x10 + (i *2) );
    	  short cellStart = randomAccessFile.readShort();
      
    	  if(cellStart == 0)
    		  continue;
          
    	  randomAccessFile.seek(pageBegin + 0x10 + ((i-1) *2));
    	  randomAccessFile.writeShort(cellStart);
      }
      contentNumber--;
      randomAccessFile.seek(pageBegin + 2); 
      randomAccessFile.writeShort(contentNumber);
    }
    catch(IOException e){
      System.out.println(e.getMessage());
    }
  }

  public void tableRowDeletion(String tableName, short rowIndex){
	pageRowDeletion(rowIndex);
    TableMetaData metaData = new TableMetaData(tableName);    
    metaData.rowCount --;
    metaData.updateMetaData();
    reloadTableRows = true;
  }

  private void newpagerowAddition(Byte[] rowHeader, Byte[] rowBody) throws IOException{
      if(rowHeader.length + rowBody.length + 4 > spaceavailability){
        try{
          if(typeOfPage == TypeOfPage.LEAF || typeOfPage == TypeOfPage.INNER){
        	  overflowHandler();
           }
          else{  
        	  indexOverflowhandler();
            return;
          }
        }
        catch(IOException e){
          System.out.println("! Error while handling overflow");
        }
      }
    short cellStart =  contentOffset;                    
    short newCellStart  = Integer.valueOf((cellStart - rowBody.length  - rowHeader.length - 2)).shortValue();
    randomAccessFile.seek(pageNumber * DavisBaseBinaryFile.pageSize + newCellStart);
    randomAccessFile.write(ByteConvertor.Bytestobytes(rowHeader));
    randomAccessFile.write(ByteConvertor.Bytestobytes(rowBody));
    randomAccessFile.seek(pageBegin + 0x10 + (contentNumber * 2));
    randomAccessFile.writeShort(newCellStart);
    contentOffset = newCellStart;
    randomAccessFile.seek(pageBegin + 4); randomAccessFile.writeShort(contentOffset);
    contentNumber++;
    randomAccessFile.seek(pageBegin + 2); randomAccessFile.writeShort(contentNumber);    
    spaceavailability = contentOffset - 0x10 - (contentNumber *2);
   }
    
   private boolean idxPageCleaned;

   private void indexOverflowhandler() throws IOException{
     if(typeOfPage == TypeOfPage.INDEXOFLEAF){
       if(parentPageNumber == -1){
    	   parentPageNumber = newPageAddition(randomAccessFile, TypeOfPage.INDEXOFINNER, pageNumber , -1);
       }
       int newLeftLeafPageNo = newPageAddition(randomAccessFile, TypeOfPage.INDEXOFLEAF,pageNumber, parentPageNumber);
       setParent(parentPageNumber);
       INode incomingInsertTemp = this.incomingInsert;
       Page leftLeafPage = new Page(randomAccessFile, newLeftLeafPageNo);
       INode toInsertParentIndexNode = splitRecordsperPage(leftLeafPage);
       Page parentPage = new Page(randomAccessFile,parentPageNumber);
       int comparisonResult= Criteria.compareData(incomingInsertTemp.index.cellValue,toInsertParentIndexNode.index.cellValue,incomingInsert.index.dataType);  
       if(comparisonResult == 0){
          toInsertParentIndexNode.rowIds.addAll(incomingInsertTemp.rowIds);
          parentPage.insertNewIndex(toInsertParentIndexNode,newLeftLeafPageNo);
          pageShift(parentPage);
          return;
       }else if(comparisonResult < 0){
          leftLeafPage.insertNewIndex(incomingInsertTemp);
          pageShift(leftLeafPage);
       }else{
    	   insertNewIndex(incomingInsertTemp);
       }
       parentPage.insertNewIndex(toInsertParentIndexNode,newLeftLeafPageNo);
      }else{
	       if(contentNumber < 3 && !idxPageCleaned){
	          idxPageCleaned = true;
	          String[] indexValuesTemp = fetchIndexValues().toArray(new String[fetchIndexValues().size()]);
	          HashMap<String, IList> indexValuePointerTemp = (HashMap<String, IList>) indexPointer.clone();
	          INode incomingInsertTemp = this.incomingInsert;
	          cleanupPage();
	          for (int i = 0; i < indexValuesTemp.length; i++) {
	        	  insertNewIndex(indexValuePointerTemp.get(indexValuesTemp[i]).getINode(),indexValuePointerTemp.get(indexValuesTemp[i]).previousPageNumber);
	          }
	          insertNewIndex(incomingInsertTemp);
	          return;
	       }
       if(idxPageCleaned){
         System.out.println("Limit reached.");
         return;
       }
       if(parentPageNumber == -1){
    	   parentPageNumber = newPageAddition(randomAccessFile, TypeOfPage.INDEXOFINNER, pageNumber , -1);
       }
       int newLeftInteriorPageNo = newPageAddition(randomAccessFile, TypeOfPage.INDEXOFINNER, pageNumber, parentPageNumber );
       setParent(parentPageNumber);
       INode incomingInsertTemp = this.incomingInsert;
       Page leftInteriorPage = new Page(randomAccessFile, newLeftInteriorPageNo); 
       INode toInsertParentIndexNode = splitRecordsperPage(leftInteriorPage);
       Page parentPage = new Page(randomAccessFile,parentPageNumber);
       int comparisonResult= Criteria.compareData(incomingInsertTemp.index.cellValue,toInsertParentIndexNode.index.cellValue,incomingInsert.index.dataType);
       Page middleOrphan = new Page(randomAccessFile,toInsertParentIndexNode.previousPageNumber);
       middleOrphan.setParent(parentPageNumber);
       leftInteriorPage.setNextPageNo(middleOrphan.pageNumber);
       if(comparisonResult == 0){
          toInsertParentIndexNode.rowIds.addAll(incomingInsertTemp.rowIds);
          parentPage.insertNewIndex(toInsertParentIndexNode,newLeftInteriorPageNo);
          pageShift(parentPage);
          return;
       }
       else if(comparisonResult < 0){
        leftInteriorPage.insertNewIndex(incomingInsertTemp);
        pageShift(leftInteriorPage);
       }
       else{  
    	   insertNewIndex(incomingInsertTemp);
       }   
       parentPage.insertNewIndex(toInsertParentIndexNode,newLeftInteriorPageNo);
     }     
    }
    
    private void cleanupPage() throws IOException {
      contentNumber = 0;
      contentOffset = Long.valueOf(DavisBaseBinaryFile.pageSize).shortValue();
      spaceavailability = contentOffset - 0x10 - (contentNumber * 2);
      byte[] leftoverspace = new byte[512-16];
      Arrays.fill(leftoverspace, (byte) 0 );
      randomAccessFile.seek(pageBegin + 16);
      randomAccessFile.write(leftoverspace);
      randomAccessFile.seek(pageBegin + 2);
      randomAccessFile.writeShort(contentNumber);
      randomAccessFile.seek(pageBegin + 4);
      randomAccessFile.writeShort(contentOffset);
      longIndexValues = new TreeSet<>();
      stringIndexValues = new TreeSet<>();
      indexPointer = new HashMap<>();
    }

  private INode splitRecordsperPage(Page newpreviousPage) throws IOException {
    try{
	    int middle = fetchIndexValues().size() / 2;
	    String[] indexValuesTemp = fetchIndexValues().toArray(new String[fetchIndexValues().size()]);
	    INode toInsertParentIndexNode = indexPointer.get(indexValuesTemp[middle]).getINode();
	    toInsertParentIndexNode.previousPageNumber = indexPointer.get(indexValuesTemp[middle]).previousPageNumber;
	    HashMap<String, IList> indexValuePointerTemp = (HashMap<String, IList>) indexPointer.clone();
	    for (int i = 0; i < middle; i++) {
	      newpreviousPage.insertNewIndex(indexValuePointerTemp.get(indexValuesTemp[i]).getINode(),indexValuePointerTemp.get(indexValuesTemp[i]).previousPageNumber);
	    }
	    cleanupPage();
	    stringIndexValues = new TreeSet<>();
	    longIndexValues = new TreeSet<>();
	    indexPointer = new HashMap<String, IList>();
	    for(int i=middle+1;i<indexValuesTemp.length;i++){  
	    	insertNewIndex(indexValuePointerTemp.get(indexValuesTemp[i]).getINode(),indexValuePointerTemp.get(indexValuesTemp[i]).previousPageNumber);
	    }
	  
	    return toInsertParentIndexNode;
    }catch(IOException e){
    	throw e;
    }
  }

  private void overflowHandler() throws IOException{
    if(typeOfPage == TypeOfPage.LEAF){
        int newRightLeafPageNo = newPageAddition(randomAccessFile,typeOfPage,-1,-1);
        if(parentPageNumber == -1){           
           int newParentPageNo = newPageAddition(randomAccessFile, TypeOfPage.INNER,
            newRightLeafPageNo, -1);
           setNextPageNo(newRightLeafPageNo);
          setParent(newParentPageNo);
          Page newParentPage = new Page(randomAccessFile,newParentPageNo);
          newParentPageNo = newParentPage.tablePreviousChildaddition(pageNumber,endRow);
          newParentPage.setNextPageNo(newRightLeafPageNo);
          Page newLeafPage = new Page(randomAccessFile,newRightLeafPageNo);
          newLeafPage.setParent(newParentPageNo);
          pageShift(newLeafPage);
        }else{
          Page parentPage = new Page(randomAccessFile,parentPageNumber);
          parentPageNumber = parentPage.tablePreviousChildaddition(pageNumber,endRow);
          parentPage.setNextPageNo(newRightLeafPageNo);
          setNextPageNo(newRightLeafPageNo);
          Page newLeafPage = new Page(randomAccessFile,newRightLeafPageNo);
          newLeafPage.setParent(parentPageNumber);
          pageShift(newLeafPage);
        }
      }else {
    	  int newRightLeafPageNo = newPageAddition(randomAccessFile,typeOfPage,-1,-1);           
          int latestParentPageNo = newPageAddition(randomAccessFile, TypeOfPage.INNER,newRightLeafPageNo, -1);
          setNextPageNo(newRightLeafPageNo);
          setParent(latestParentPageNo);
          Page newParentPage = new Page(randomAccessFile,latestParentPageNo);
          latestParentPageNo = newParentPage.tablePreviousChildaddition(pageNumber,endRow);
          newParentPage.setNextPageNo(newRightLeafPageNo);
          Page newLeafPage = new Page(randomAccessFile,newRightLeafPageNo);
          newLeafPage.setParent(latestParentPageNo);
          pageShift(newLeafPage);
      }
  }


  private int tablePreviousChildaddition(int previousChildPageNumber,int rowId) throws IOException{
    for( TableRow row: leftChild){
      if(row.rowId == rowId)
        return pageNumber;
    }
    if(typeOfPage == TypeOfPage.INNER){
      List<Byte> rowHeader= new ArrayList<>();
      List<Byte> rowBody= new ArrayList<>();
      rowHeader.addAll(Arrays.asList(ByteConvertor.intToBytes(previousChildPageNumber)));
      rowBody.addAll(Arrays.asList(ByteConvertor.intToBytes(rowId)));
      newpagerowAddition(rowHeader.toArray(new Byte[rowHeader.size()]),rowBody.toArray(new Byte[rowBody.size()]));
    }
   return pageNumber;
  }

  private void pageShift(Page latestPage){
	typeOfPage = latestPage.typeOfPage;
	contentNumber = latestPage.contentNumber;
	pageNumber = latestPage.pageNumber;
    contentOffset = latestPage.contentOffset;
    nextPage = latestPage.nextPage;
    parentPageNumber = latestPage.parentPageNumber;
    leftChild = latestPage.leftChild;
    stringIndexValues = latestPage.stringIndexValues;
    longIndexValues = latestPage.longIndexValues;
    indexPointer = latestPage.indexPointer;
    rows = latestPage.rows;
    pageBegin = latestPage.pageBegin;
    spaceavailability = latestPage.spaceavailability;
  }

 public void setParent(int parentPageNumber) throws IOException{
	 randomAccessFile.seek(DavisBaseBinaryFile.pageSize * pageNumber + 0x0A);
	 randomAccessFile.writeInt(parentPageNumber);
     this.parentPageNumber = parentPageNumber;
 }

 public void setNextPageNo(int nextPageNumber) throws IOException{
	 randomAccessFile.seek(DavisBaseBinaryFile.pageSize * pageNumber + 0x06);
	 randomAccessFile.writeInt(nextPageNumber);
     this.nextPage = nextPageNumber;
 }

 public void removeIndex(INode node) throws IOException{
	pageRowDeletion(indexPointer.get(node.index.cellValue).pageHeaderIValue);
	populateIndexRows();
    updateHeaderOffset();
 }

 public void insertNewIndex(INode node) throws IOException{
	 insertNewIndex(node,-1);
 }

 private INode incomingInsert;
 
 public void insertNewIndex(INode node,int previousPageNumber) throws IOException{
  incomingInsert = node;
  incomingInsert.previousPageNumber = previousPageNumber;
  List<Integer> rowIds = new ArrayList<>();  
  List<String> ixValues = fetchIndexValues();
  if(fetchIndexValues().contains(node.index.cellValue)){
	  previousPageNumber = indexPointer.get(node.index.cellValue).previousPageNumber;
      incomingInsert.previousPageNumber = previousPageNumber;
      rowIds = indexPointer.get(node.index.cellValue).rowIds;
      rowIds.addAll(incomingInsert.rowIds);
      incomingInsert.rowIds = rowIds;
      pageRowDeletion(indexPointer.get(node.index.cellValue).pageHeaderIValue);
      if(indexDataType == DataType.TEXT || indexDataType == null)
    	  stringIndexValues.remove(node.index.cellValue);
      else
    	  longIndexValues.remove(Long.parseLong(node.index.cellValue));
  }
    rowIds.addAll(node.rowIds);
    rowIds = new ArrayList<>(new HashSet<>(rowIds));
    List<Byte> recordHead = new ArrayList<>(); 
    List<Byte> rowBody = new ArrayList<>();    
    rowBody.addAll(Arrays.asList(Integer.valueOf(rowIds.size()).byteValue()));
    if(node.index.dataType == DataType.TEXT)
      rowBody.add(Integer.valueOf(node.index.dataType.getValue() + node.index.cellValue.length()).byteValue());
    else
      rowBody.add(node.index.dataType.getValue());
    rowBody.addAll(Arrays.asList(node.index.cellValueLarge));
    for(int i=0;i<rowIds.size();i++){
      rowBody.addAll(Arrays.asList(ByteConvertor.intToBytes(rowIds.get(i))));
    } 
    short payload = Integer.valueOf(rowBody.size()).shortValue();
    if(typeOfPage == TypeOfPage.INDEXOFINNER)
        recordHead.addAll(Arrays.asList(ByteConvertor.intToBytes(previousPageNumber)));
    recordHead.addAll(Arrays.asList(ByteConvertor.shortToBytes(payload)));
    newpagerowAddition(recordHead.toArray(new Byte[recordHead.size()]),rowBody.toArray(new Byte[rowBody.size()]));
    populateIndexRows();
    updateHeaderOffset();    
 }
 
private void updateHeaderOffset(){
  try {
	  randomAccessFile.seek(pageBegin + 0x10);
	  for(String indexVal : fetchIndexValues()){
		  randomAccessFile.writeShort(indexPointer.get(indexVal).offsetValue);
	  }
	}catch (IOException ex) {
	  System.out.println(ex.getMessage());
	}
}

  private void populateTable() {
    short LoadSize = 0;
    byte noOfcolumns = 0;
    rows = new ArrayList<Row>();
    mapOfRecords =  new HashMap<>();
    try {
      for (short i = 0; i < contentNumber; i++) {
    	  randomAccessFile.seek(pageBegin + 0x10 + (i *2) );
        short cellStart = randomAccessFile.readShort();
        if(cellStart == 0)
          continue;
        randomAccessFile.seek(pageBegin + cellStart);
        LoadSize = randomAccessFile.readShort();
        int rowId = randomAccessFile.readInt();
        noOfcolumns = randomAccessFile.readByte();        
        if(endRow < rowId) endRow = rowId;        
        byte[] colDatatypes = new byte[noOfcolumns];
        byte[] rowBody = new byte[LoadSize - noOfcolumns - 1];
        randomAccessFile.read(colDatatypes);
        randomAccessFile.read(rowBody);
        Row record = new Row(i, rowId, cellStart, colDatatypes, rowBody);
        rows.add(record);
        mapOfRecords.put(rowId, record);
      }
    } catch (IOException ex) {
      System.out.println(ex.getMessage());
    }
  }

private void leftChildrenPlacement(){
  try {
    leftChild = new ArrayList<>();
    int leftChildPageNumber = 0;
    int rowId =0;
    for (int i = 0; i < contentNumber; i++) {
    	randomAccessFile.seek(pageBegin + 0x10 + (i *2) );
      short cellStart = randomAccessFile.readShort();
      if(cellStart == 0)
        continue;
      randomAccessFile.seek(pageBegin + cellStart);
      leftChildPageNumber = randomAccessFile.readInt();
      rowId = randomAccessFile.readInt();
      leftChild.add(new TableRow(rowId, leftChildPageNumber));
    }
  } catch (IOException ex) {
    System.out.println(ex.getMessage());
  }
}

private void populateIndexRows(){
  try {
    longIndexValues = new TreeSet<>();
    stringIndexValues = new TreeSet<>();
    indexPointer = new HashMap<>();
    int previousPageNumber = -1;
    byte rowIdCount = 0;
    byte dataType = 0;
    for (short i = 0; i < contentNumber; i++) {
    	randomAccessFile.seek(pageBegin + 0x10 + (i *2) );
      short cellStart = randomAccessFile.readShort();
      if(cellStart == 0)
        continue;
      randomAccessFile.seek(pageBegin + cellStart);
      if(typeOfPage == TypeOfPage.INDEXOFINNER)
    	  previousPageNumber = randomAccessFile.readInt();
      short payload = randomAccessFile.readShort();
      rowIdCount = randomAccessFile.readByte();
      dataType = randomAccessFile.readByte();
      if(indexDataType == null && DataType.get(dataType) != DataType.NULL)
    	  indexDataType = DataType.get(dataType);
      byte[] indexValue = new byte[DataType.getLength(dataType)];
      randomAccessFile.read(indexValue);
      List<Integer> lstRowIds = new ArrayList<>();
      for(int j=0;j<rowIdCount;j++)
      {
          lstRowIds.add(randomAccessFile.readInt());
      }
      IList record = new IList(i, DataType.get(dataType),rowIdCount, indexValue, lstRowIds,previousPageNumber,nextPage,pageNumber,cellStart);
      if(indexDataType == DataType.TEXT || indexDataType == null)
    	  stringIndexValues.add(record.getINode().index.cellValue);
      else
    	  longIndexValues.add(Long.parseLong(record.getINode().index.cellValue));
      indexPointer.put(record.getINode().index.cellValue, record);
    }
  } catch (IOException ex) {
    System.out.println(ex.getMessage());
  }
} 
}

