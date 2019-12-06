package team.magenta.db;
import java.util.List;
public class INode{
    public Property index;
    public List<Integer> rowIds;
    public boolean isInnnerNode;
    public int previousPageNumber;
    public INode(Property index,List<Integer> rowIds){
        this.index = index;
        this.rowIds = rowIds;
    }
}