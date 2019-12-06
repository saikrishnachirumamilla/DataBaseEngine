package team.magenta.db;
import java.util.List;
public class IList{
    public Byte RowIdCount;
    public DataType dataType;
    public Byte[] index;
    public List<Integer> rowIds;
    public short pageHeaderIValue;
    public short offsetValue;
    int previousPageNumber;
    int NextPageNumber;
    int pageNumber;
    private INode iNode;
    IList(short pageHeaderIValue,DataType dataType,Byte RowIdCount, byte[] index, List<Integer> rowIds,int previousPageNumber,int NextPageNumber,int pageNumber,short offsetValue){
        this.offsetValue = offsetValue;
        this.pageHeaderIValue = pageHeaderIValue;
        this.RowIdCount = RowIdCount;
        this.dataType = dataType;
        this.index = ByteConvertor.byteToBytes(index);
        this.rowIds = rowIds;
        iNode = new INode(new Property(this.dataType, index),rowIds);
        this.previousPageNumber = previousPageNumber;
        this.NextPageNumber = NextPageNumber;
        this.pageNumber = pageNumber;
    }
    public INode getINode(){
        return iNode;
    }
}