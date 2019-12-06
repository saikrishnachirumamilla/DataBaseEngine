package team.magenta.db;
public class Criteria {
	
	public int ordinal;
    public DataType dataType;
    String NameOfColumn;
    private Operation operation;
    String valueComparision;
    boolean not;
    
    public Criteria(DataType dataType) {
        this.dataType = dataType;
    }
    public static String[] operatorsDefined = { "<=", ">=", "<>", ">", "<", "=" };
    public static Operation getOperation(String operation) {
        switch (operation) {
        case ">":
            return Operation.GREATERTHAN;
        case "<":
            return Operation.LESSTHAN;
        case "=":
            return Operation.EQUALTO;
        case ">=":
            return Operation.GREATERTHANOREQUAL;
        case "<=":
            return Operation.LESSTHANOREQUAL;
        case "<>":
            return Operation.NOTEQUAL;
        default:
            System.out.println("Invalid operator \"" + operation + "\"");
            return Operation.INVALID;
        }
    }
    public static int compareData(String query1, String query2, DataType dataType) {
        if (dataType == DataType.TEXT) {
            return query1.toLowerCase().compareTo(query2);
        }else if (dataType == DataType.NULL) {
            if (query1 == query2) {
                return 0;
            }
            else if (query1.toLowerCase().equals("null")) {
                return 1;
            }
            else {
                return -1;
            }
        }else {
            return Long.valueOf(Long.parseLong(query1) - Long.parseLong(query2)).intValue();
        }
    }
    private boolean operationChange(Operation operation,int change){
        switch (operation) {
            case LESSTHANOREQUAL:
            return change <= 0;
        case GREATERTHANOREQUAL:
            return change >= 0;
        case NOTEQUAL:
            return change != 0;
        case LESSTHAN:
            return change < 0;
        case GREATERTHAN:
            return change > 0;
        case EQUALTO:
            return change == 0;
        default:
            return false;
        }
    }
    public boolean checkCriteria(String content) {
    	Operation operation = getOperation();
        if(content.toLowerCase().equals("null") || valueComparision.toLowerCase().equals("null")) {
            return operationChange(operation,compareData(content,valueComparision,DataType.NULL));
        }
        if (dataType == DataType.TEXT || dataType == DataType.NULL) {
            return CompareString(content, operation);
        }else {
            switch (operation) {
            case EQUALTO:
                return Long.parseLong(content) == Long.parseLong(valueComparision);
            case NOTEQUAL:
                return Long.parseLong(content) != Long.parseLong(valueComparision);
            case LESSTHAN:
                return Long.parseLong(content) < Long.parseLong(valueComparision);
            case GREATERTHAN:
                return Long.parseLong(content) > Long.parseLong(valueComparision);
            case LESSTHANOREQUAL:
                return Long.parseLong(content) <= Long.parseLong(valueComparision);
            case GREATERTHANOREQUAL:
                return Long.parseLong(content) >= Long.parseLong(valueComparision);
            default:
                return false;
            }
        }
    }
    
    private Operation notOperator() {
        switch (this.operation) {
        case LESSTHANOREQUAL:
            return Operation.GREATERTHAN;
        case GREATERTHANOREQUAL:
            return Operation.LESSTHAN;
        case NOTEQUAL:
            return Operation.EQUALTO;
        case LESSTHAN:
            return Operation.GREATERTHANOREQUAL;
        case GREATERTHAN:
            return Operation.LESSTHANOREQUAL;
        case EQUALTO:
            return Operation.NOTEQUAL;
        default:
            System.out.println("Invalid operator \"" + this.operation + "\"");
            return Operation.INVALID;
        }
    }
    
    public void setCriteria(String criteria) {
        this.valueComparision = criteria;
        this.valueComparision = valueComparision.replace("'", "");
        this.valueComparision = valueComparision.replace("\"", "");
    }
    public void setColumn(String col) {
        this.NameOfColumn = col;
    }
    public void setOperation(String operator) {
        this.operation = getOperation(operator);
    }
    public void setNot(boolean not) {
        this.not = not;
    }
    public Operation getOperation() {
        if (not) {
        	return notOperator();
        }
        else {
        	return this.operation;
        }
    }
    
    private boolean CompareString(String content, Operation operation) {
        return operationChange(operation,content.toLowerCase().compareTo(valueComparision));
    }
}