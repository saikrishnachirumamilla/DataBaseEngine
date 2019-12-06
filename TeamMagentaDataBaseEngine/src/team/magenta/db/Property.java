package team.magenta.db;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
public class Property{
	
    public String cellValue;
	public byte[] cellValueSmall;
    public Byte[] cellValueLarge;
    public DataType dataType;
    @SuppressWarnings("deprecation")
	Property(DataType dataType, byte[] cellValueSmall){
        this.dataType = dataType;
        this.cellValueSmall = cellValueSmall;
	    try{
	      switch(dataType)
	      {
	         case NULL:
	            this.cellValue= "NULL";
	            break;
	        case TINYINT: 
	        	this.cellValue = Byte.valueOf(ByteConvertor.byteFromByteArray(cellValueSmall)).toString(); 
	        	break;
	        case SMALLINT: 
	        	this.cellValue = Short.valueOf(ByteConvertor.shortFromByteArray(cellValueSmall)).toString(); 
	        	break;
	        case INT: 
	        	this.cellValue = Integer.valueOf(ByteConvertor.intFromByteArray(cellValueSmall)).toString(); 
	        	break;
	        case BIGINT: 
	        	this.cellValue =  Long.valueOf(ByteConvertor.longFromByteArray(cellValueSmall)).toString(); 
	        	break;
	        case FLOAT: 
	        	this.cellValue = Float.valueOf(ByteConvertor.floatFromByteArray(cellValueSmall)).toString(); 
	        	break;
	        case DOUBLE: 
	        	this.cellValue = Double.valueOf(ByteConvertor.doubleFromByteArray(cellValueSmall)).toString(); 
	        	break;
	        case YEAR: 
	        	this.cellValue = Integer.valueOf((int)Byte.valueOf(ByteConvertor.byteFromByteArray(cellValueSmall))+2000).toString(); 
	        	break;
	        case TIME:
	            int milliseconds = ByteConvertor.intFromByteArray(cellValueSmall) % 86400000;
	            int seconds = milliseconds / 1000;
	            int hours = seconds / 3600;
	            int remainingSeconds = seconds % 3600;
	            int minutes = remainingSeconds / 60;
	            int leftOverSeconds = remainingSeconds % 60;
	            this.cellValue = String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", leftOverSeconds);
	            break;
	        case DATETIME:
	            Date rawdatetime = new Date(Long.valueOf(ByteConvertor.longFromByteArray(cellValueSmall)));
	            this.cellValue = String.format("%02d", rawdatetime.getYear()+1900) + "-" + String.format("%02d", rawdatetime.getMonth()+1)+ "-" + String.format("%02d", rawdatetime.getDate()) + "_" + String.format("%02d", rawdatetime.getHours()) + ":"+ String.format("%02d", rawdatetime.getMinutes()) + ":" + String.format("%02d", rawdatetime.getSeconds());
	            break;
	        case DATE:
	            Date rawdate = new Date(Long.valueOf(ByteConvertor.longFromByteArray(cellValueSmall)));
	            this.cellValue = String.format("%02d", rawdate.getYear()+1900) + "-" + String.format("%02d", rawdate.getMonth()+1)+ "-" + String.format("%02d", rawdate.getDate());
	            break;
	        case TEXT: this.cellValue = new String(cellValueSmall, "UTF-8"); break;
	         default:
	         this.cellValue= new String(cellValueSmall, "UTF-8"); break;
	      }
	         this.cellValueLarge = ByteConvertor.byteToBytes(cellValueSmall);
	    } catch(Exception ex) {
	        System.out.println("Property Exception");
	    }
    }
    Property(DataType dataType,String cellValue) throws Exception {
        this.dataType = dataType;
        this.cellValue = cellValue;
        try {
            switch(dataType)
            {
               case NULL:
                  this.cellValueSmall = null; 
                  break;
              case TINYINT: 
            	  this.cellValueSmall = new byte[]{ Byte.parseByte(cellValue)}; 
            	  break;
              case SMALLINT: 
            	  this.cellValueSmall = ByteConvertor.shortTobytes(Short.parseShort(cellValue)); 
            	  break;
              case INT: 
            	  this.cellValueSmall = ByteConvertor.intTobytes(Integer.parseInt(cellValue)); 
            	  break;
              case BIGINT: 
            	  this.cellValueSmall =  ByteConvertor.longTobytes(Long.parseLong(cellValue)); 
            	  break;
              case FLOAT: 
            	  this.cellValueSmall = ByteConvertor.floatTobytes(Float.parseFloat(cellValue)); 
            	  break;
              case DOUBLE: 
            	  this.cellValueSmall = ByteConvertor.doubleTobytes(Double.parseDouble(cellValue)); 
            	  break;
              case YEAR: 
            	  this.cellValueSmall = new byte[] { (byte) (Integer.parseInt(cellValue) - 2000) }; 
            	  break;
              case TIME: 
            	  this.cellValueSmall = ByteConvertor.intTobytes(Integer.parseInt(cellValue)); 
            	  break;
              case DATETIME:
                  SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
                  Date time = simpleDateFormat.parse(cellValue);  
                  this.cellValueSmall = ByteConvertor.longTobytes(time.getTime());              
                  break;
              case DATE:
                  SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyy-MM-dd");
                  Date date = simpleDateFormat1.parse(cellValue);  
                  this.cellValueSmall = ByteConvertor.longTobytes(date.getTime());              
                  break;
              case TEXT: 
            	  this.cellValueSmall = cellValue.getBytes(); 
            	  break;
               default:
            	   this.cellValueSmall = cellValue.getBytes(StandardCharsets.US_ASCII); 
            	   break;
            }
            this.cellValueLarge = ByteConvertor.byteToBytes(cellValueSmall);  
        } catch (Exception e) {
            System.out.println("Cannot convert " + cellValue + " to " + dataType.toString());
            throw e;
        }
    }
   
}