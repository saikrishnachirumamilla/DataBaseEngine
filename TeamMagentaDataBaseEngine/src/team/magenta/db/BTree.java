package team.magenta.db;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
public class BTree {
	
    Page rootNode;
    RandomAccessFile randomAccessFile;
    public BTree(RandomAccessFile randomAccessFile) {
        this.randomAccessFile = randomAccessFile;
        this.rootNode = new Page(randomAccessFile, DavisBaseBinaryFile.retrievePageNumber(randomAccessFile));
    }
    private int getNearestPageNumber(Page page, String num) {
        if (page.typeOfPage == TypeOfPage.INDEXOFLEAF) {
            return page.pageNumber;
        } else {
            if (Criteria.compareData(num , page.fetchIndexValues().get(0),page.indexDataType) < 0)
                return getNearestPageNumber(new Page(randomAccessFile,page.indexPointer.get(page.fetchIndexValues().get(0)).previousPageNumber),num);
            else if(Criteria.compareData(num,page.fetchIndexValues().get(page.fetchIndexValues().size()-1),page.indexDataType) > 0)
                return getNearestPageNumber(
                    new Page(randomAccessFile,page.nextPage),num);
            else{
                //perform binary search 
                String closestValue = binarySearchAlgorithm(page.fetchIndexValues().toArray(new String[page.fetchIndexValues().size()]),num,0,page.fetchIndexValues().size() -1,page.indexDataType);
                int i = page.fetchIndexValues().indexOf(closestValue);
                List<String> indexValues = page.fetchIndexValues();
                if(closestValue.compareTo(num) < 0 && i+1 < indexValues.size()){
                    return page.indexPointer.get(indexValues.get(i+1)).previousPageNumber;
                }else if(closestValue.compareTo(num) > 0)
                {
                    return page.indexPointer.get(closestValue).previousPageNumber;
                }else{
                    return page.pageNumber;
                }
            }
        }
    }
    public List<Integer> fetchRowIds(Criteria criteria){
        List<Integer> rowIds = new ArrayList<>();
        Page page = new Page(randomAccessFile,getNearestPageNumber(rootNode, criteria.valueComparision));
        String[] indexValues= page.fetchIndexValues().toArray(new String[page.fetchIndexValues().size()]);        
        Operation operationType = criteria.getOperation();
        for(int i=0;i < indexValues.length;i++)      {
            if(criteria.checkCriteria(page.indexPointer.get(indexValues[i]).getINode().index.cellValue))
                rowIds.addAll(page.indexPointer.get(indexValues[i]).rowIds);
        }          
        if(operationType == Operation.LESSTHAN || operationType == Operation.LESSTHANOREQUAL){
           if(page.typeOfPage == TypeOfPage.INDEXOFLEAF)
               rowIds.addAll(fetchPreviousRowIds(page.parentPageNumber,indexValues[0]));
           else 
                rowIds.addAll(fetchPreviousRowIds(page.pageNumber,criteria.valueComparision));
        }   
        if(operationType == Operation.GREATERTHAN || operationType == Operation.GREATERTHANOREQUAL){
         if(page.typeOfPage == TypeOfPage.INDEXOFLEAF)
            rowIds.addAll(fetchNextRowIds(page.parentPageNumber,indexValues[indexValues.length - 1]));
            else 
              rowIds.addAll(fetchNextRowIds(page.pageNumber,criteria.valueComparision));
        }
        return rowIds;
    }
    private List<Integer> fetchPreviousRowIds(int pageNumber, String index){
        List<Integer> rowIds = new ArrayList<>();
        if(pageNumber == -1)
             return rowIds;
        Page page = new Page(this.randomAccessFile,pageNumber);
        List<String> indexes = Arrays.asList(page.fetchIndexValues().toArray(new String[page.fetchIndexValues().size()]));
      
        for(int i=0;i< indexes.size() && Criteria.compareData(indexes.get(i), index, page.indexDataType) < 0 ;i++){
           
               rowIds.addAll(page.indexPointer.get(indexes.get(i)).getINode().rowIds);
               sumChildIds(page.indexPointer.get(indexes.get(i)).previousPageNumber, rowIds);    
        }
         
         if(page.indexPointer.get(index)!= null)
        	 sumChildIds(page.indexPointer.get(index).previousPageNumber, rowIds);
         return rowIds;
    }
    private List<Integer> fetchNextRowIds(int pageNumber, String index){    
        List<Integer> rowIds = new ArrayList<>();
        if(pageNumber == -1)
            return rowIds;
        Page page = new Page(this.randomAccessFile,pageNumber);
        List<String> indexes = Arrays.asList(page.fetchIndexValues().toArray(new String[page.fetchIndexValues().size()]));
        for(int i=indexes.size() - 1; i >= 0 && Criteria.compareData(indexes.get(i), index, page.indexDataType) > 0; i--){
               rowIds.addAll(page.indexPointer.get(indexes.get(i)).getINode().rowIds);
               sumChildIds(page.nextPage, rowIds);
         }
        if(page.indexPointer.get(index)!= null)
        	sumChildIds(page.indexPointer.get(index).NextPageNumber, rowIds);
        return rowIds;
    }
    private void sumChildIds(int pageNumber,List<Integer> rowIds){
        if(pageNumber == -1)
            return;
        Page page = new Page(this.randomAccessFile, pageNumber);
            for (IList row :page.indexPointer.values()){
                rowIds.addAll(row.rowIds);
                if(page.typeOfPage == TypeOfPage.INDEXOFINNER){
                	sumChildIds(row.previousPageNumber, rowIds);
                	sumChildIds(row.previousPageNumber, rowIds);
                 }
            }  
    }
    public void insertNode(Property property,List<Integer> rowIds){
        try{
            int pageNumber = getNearestPageNumber(rootNode, property.cellValue) ;
            Page page = new Page(randomAccessFile, pageNumber);
            page.insertNewIndex(new INode(property,rowIds));
            }
            catch(IOException e){
                 System.out.println("Insert into index file");
            }
    }
    public void insertNode(Property property,int rowId){
        insertNode(property,Arrays.asList(rowId));
    }
    public void deleteNode(Property property, int rowId){    
        try{
            int pageNumber = getNearestPageNumber(rootNode, property.cellValue) ;
            Page page = new Page(randomAccessFile, pageNumber);
            INode node = page.indexPointer.get(property.cellValue).getINode();
            node.rowIds.remove(node.rowIds.indexOf(rowId));
            page.removeIndex(node);
            if(node.rowIds.size() !=0) {
                page.insertNewIndex(node);
            }
        }catch(IOException e) {
            System.out.println("Exception");
        }
    }
    private String binarySearchAlgorithm(String[] rows,String queryValue,int front, int rear , DataType dataType){
        if(rear - front <= 3){
            int i = front;
            for(i = front;i < rear;i++){
                if(Criteria.compareData(rows[i], queryValue, dataType) < 0) {
                    continue;
                }
                else {
                    break;
                }
            }
            return rows[i];
        }else{
	   		int middle = (rear - front) / 2 + front;
            if(rows[middle].equals(queryValue)) {
                return rows[middle];
            }
            if(Criteria.compareData(rows[middle], queryValue, dataType) < 0) {
                return binarySearchAlgorithm(rows,queryValue,middle + 1,rear,dataType);
            }
	        else {
	            return binarySearchAlgorithm(rows,queryValue,front,middle - 1,dataType);	
	        }
        }     
    }
}