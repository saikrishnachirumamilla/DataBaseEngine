package team.magenta.db;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
public class BPlusTree {
	
    RandomAccessFile randomAccessFile;
    int rootPageNumber;
    String tableName;
    
    public BPlusTree(RandomAccessFile randomAccessFile, int rootPageNumber, String tableName) {
        this.randomAccessFile = randomAccessFile;
        this.rootPageNumber = rootPageNumber;
        this.tableName = tableName;
    }
    public List<Integer> getAllLeavesOfTree() throws IOException {
        List<Integer> leaves = new ArrayList<Integer>();
        randomAccessFile.seek(rootPageNumber * DavisBaseBinaryFile.pageSize);
        TypeOfPage rootPageType = TypeOfPage.get(randomAccessFile.readByte());
        if (rootPageType == TypeOfPage.LEAF) {
            if (!leaves.contains(rootPageNumber))
            	leaves.add(rootPageNumber);
        } else {
        	addLeavesToTree(rootPageNumber, leaves);
        }
        return leaves;
    }
    private void addLeavesToTree(int innerPageNumber, List<Integer> leaves) throws IOException {
        Page innerPage = new Page(randomAccessFile, innerPageNumber);
        for (TableRow previousPage : innerPage.leftChild) {
            if (Page.getTypeOfPage(randomAccessFile, previousPage.leftChildPageNumber) == TypeOfPage.LEAF) {
                if (!leaves.contains(previousPage.leftChildPageNumber))
                	leaves.add(previousPage.leftChildPageNumber);
            } else {
            	addLeavesToTree(previousPage.leftChildPageNumber, leaves);
            }
        }
        if (Page.getTypeOfPage(randomAccessFile, innerPage.nextPage) == TypeOfPage.LEAF) {
            if (!leaves.contains(innerPage.nextPage))
            	leaves.add(innerPage.nextPage);
        } else {
        	addLeavesToTree(innerPage.nextPage, leaves);
        }
    }
    public List<Integer> getAllLeavesOfTree(Criteria criteria) throws IOException {
        if (criteria == null || criteria.getOperation() == Operation.NOTEQUAL || !(new File(DavisBasePrompt.getNDXFilePath(tableName, criteria.NameOfColumn)).exists())){
            return getAllLeavesOfTree();
        }else{
            RandomAccessFile indexOfFile = new RandomAccessFile(DavisBasePrompt.getNDXFilePath(tableName, criteria.NameOfColumn), "r");
            BTree bTree = new BTree(indexOfFile);
            List<Integer> rowIds = bTree.fetchRowIds(criteria);
            Set<Integer> set = new HashSet<Integer>();
            for (int rowId : rowIds) {
            	set.add(getNextPageNumber(rowId, new Page(randomAccessFile, rootPageNumber)));
            } 
            indexOfFile.close();
            return Arrays.asList(set.toArray(new Integer[set.size()]));
        }
    }
    public static int getPageNumberForInsertRows(RandomAccessFile randomAccessFile, int rootPageNumber) {
        Page initialPage = new Page(randomAccessFile, rootPageNumber);
        if (initialPage.typeOfPage != TypeOfPage.LEAF && initialPage.typeOfPage != TypeOfPage.INDEXOFLEAF) {
            return getPageNumberForInsertRows(randomAccessFile, initialPage.nextPage);
        }
        else {
        	return rootPageNumber;
        }
    }
    public int getNextPageNumber(int rowId, Page page) {
        if (page.typeOfPage == TypeOfPage.LEAF) {
        	return page.pageNumber;
        }
        int index = binarySearchAlgorithm(page.leftChild, rowId, 0, page.contentNumber - 1);
        if (rowId < page.leftChild.get(index).rowId) {
            return getNextPageNumber(rowId, new Page(randomAccessFile, page.leftChild.get(index).leftChildPageNumber));
        } else {
        if( index+1 < page.leftChild.size())
            return getNextPageNumber(rowId, new Page(randomAccessFile, page.leftChild.get(index+1).leftChildPageNumber));
        else
           return getNextPageNumber(rowId, new Page(randomAccessFile, page.nextPage));
        }
    }
    private int binarySearchAlgorithm(List<TableRow> tableRow, int queryValue, int front, int rear) {
        if(rear - front <= 2){
            int i = front;
            for(i = front;i < rear;i++){
                if(tableRow.get(i).rowId < queryValue) {
                	continue;
                }else {
                    break;
                }
            }
            return i;
        }else{            
            int middle = (rear - front) / 2 + front;
            if (tableRow.get(middle).rowId == queryValue)
                return middle;
            if (tableRow.get(middle).rowId < queryValue)
                return binarySearchAlgorithm(tableRow, queryValue, middle + 1, rear);
            else
                return binarySearchAlgorithm(tableRow, queryValue, front, middle - 1);
        }
    }
}