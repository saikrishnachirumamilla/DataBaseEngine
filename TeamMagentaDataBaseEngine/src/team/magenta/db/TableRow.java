package team.magenta.db;
public class TableRow{
	
    public int rowId;
    public int leftChildPageNumber;
    public TableRow(int rowId, int leftChildPageNumber){
        this.rowId = rowId;
        this.leftChildPageNumber = leftChildPageNumber;  
    }
}