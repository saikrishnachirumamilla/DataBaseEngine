package team.magenta.db;
import java.io.File;
public class ColumnMetaData{
	
    public DataType dataType;    
    public String name;
    public boolean isDistinct;
    public boolean isNull;
    public Short location;
    public boolean hasIndex;
    public String tableName;
    public boolean primaryKey;
    ColumnMetaData(){
    	
    }
    ColumnMetaData(String tableName,DataType dataType,String name,boolean isDistinct,boolean isNull,short location){
        this.dataType = dataType;
        this.name = name;
        this.isDistinct = isDistinct;
        this.isNull = isNull;
        this.location = location;
        this.tableName = tableName;
        this.hasIndex = (new File(DavisBasePrompt.getNDXFilePath(tableName, name)).exists());
    }
    public void setPrimaryKey(){
    	primaryKey = true;
    }
}