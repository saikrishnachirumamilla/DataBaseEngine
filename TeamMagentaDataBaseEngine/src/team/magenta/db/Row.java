package team.magenta.db;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
public class Row{
    public int rowId;
    public Byte[] columTypes;
    public Byte[] rowContent;
    private List<Property> properties;
    public short rowOffset;
    public short pageHeaderIValue;
    Row(short pageHeaderIValue,int rowId, short rowOffset, byte[] columTypes, byte[] rowContent){
        this.rowId = rowId;
        this.rowContent= ByteConvertor.byteToBytes(rowContent);
        this.columTypes = ByteConvertor.byteToBytes(columTypes);
        this.rowOffset =  rowOffset;
        this.pageHeaderIValue = pageHeaderIValue;
        setProperties();
    }
    public List<Property> getProperties(){
        return properties;
    }
    private void setProperties(){
    	properties = new ArrayList<>();
        int pointer = 0;
        for(Byte columnDataType : columTypes){
             byte[] cellValue = ByteConvertor.Bytestobytes(Arrays.copyOfRange(rowContent,pointer, pointer + DataType.getLength(columnDataType)));
             properties.add(new Property(DataType.get(columnDataType), cellValue));
             pointer =  pointer + DataType.getLength(columnDataType);
        }
    }
    
}