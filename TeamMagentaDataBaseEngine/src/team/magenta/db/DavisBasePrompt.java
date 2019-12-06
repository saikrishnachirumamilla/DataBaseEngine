package team.magenta.db;
import java.io.FilenameFilter;
import java.io.RandomAccessFile;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static java.lang.System.out;
public class DavisBasePrompt {
	static String prompt = "teammagneta> ";
	static String version = "v1.0";
	static String copyright = "Team Magneta";
	static boolean isExit = false;
	static long pageSize = 512;
	static Scanner scanner = new Scanner(System.in).useDelimiter(";");
	public static void main(String[] args) {
		splashScreen();
		File data_path = new File("data");
		if (!new File(data_path, DavisBaseBinaryFile.tablesTable + ".tbl").exists()
				|| !new File(data_path, DavisBaseBinaryFile.columnsTable + ".tbl").exists())
			DavisBaseBinaryFile.runDataToBase();
		else
			DavisBaseBinaryFile.startedDataStore = true;
		String userCommand = "";
		while (!isExit) {
			System.out.print(prompt); /* db prompt */
			userCommand = scanner.next().replace("\n", " ").replace("\r", "").trim().toLowerCase(); /* input conversion*/
			parseUserCommand(userCommand);
		}
		System.out.println("Exiting...");
	}
   
	public static void splashScreen() {
		System.out.println(line("*", 80));
		System.out.println("Welcome to Team Magenta Database"); 
		System.out.println("Version " + getVersion());
		System.out.println(getCopyright());
		System.out.println("\nType \"help;\" to display supported commands.");
		System.out.println(line("*", 80));
	}
	public static String line(String s, int num) {
		String a = "";
		for (int i = 0; i < num; i++) {
			a += s;
		}
		return a;
	}
	public static void printCmd(String s) {
		System.out.println("\n\t" + s + "\n");
	}
	public static void printDef(String s) {
		System.out.println("\t\t" + s);
	}
	public static void help() {
		out.println(line("*", 80));
		out.println("SUPPORTED COMMANDS\n");
		out.println("All commands below are case insensitive\n");
		out.println("SHOW TABLES;");
		out.println("Displays all table names in Magenta Database.\n");
		out.println("CREATE TABLE <table_name> (<column_name> <data_type> <not_null> <unique>);");
		out.println("Creates a table with the given column_names.\n");
		out.println("INSERT INTO <table_name> (<column_list>) VALUES (<values_list>);");
		out.println("Inserts a new row into the table.\n");
		out.println("SELECT <column_list|*> FROM <table_name> [WHERE <column_name> = <value>];");
		out.println("Display table rows.\n");
		out.println("UPDATE <table_name> SET <column_name> = <value> [WHERE <condition>];");
		out.println("Update table rows.\n");
		out.println("Delete FROM TABLE <table_name> [WHERE <column_name> = <value>];");
		out.println("Delete table rows.\n");
		out.println("DROP TABLE <table_name>;");
		out.println("Removes table from Magenta Database.\n");
		out.println("VERSION;");
		out.println("Display the Magenta Database version.\n");
		out.println("HELP;");
		out.println("Display help guide for Magenta Database.\n");
		out.println("EXIT;");
		out.println("Exit Magenta Database.\n");
		out.println(line("*", 80));
	}
	/** return the DavisBase version */
	public static String getVersion() {
		return version;
	}
	public static String getCopyright() {
		return copyright;
	}
	public static void displayVersion() {
		System.out.println("Magenta DB Version " + getVersion());
		System.out.println(getCopyright());
	}
	public static void parseUserCommand(String userCommand) {
		ArrayList<String> commandTokens = new ArrayList<String>(Arrays.asList(userCommand.split(" ")));
		switch (commandTokens.get(0)) {
		case "show":
			if (commandTokens.get(1).equals("tables"))
				parseUserCommand("select * from davisbase_tables");
			else if (commandTokens.get(1).equals("rowid")) {
				DavisBaseBinaryFile.showRowId = true;
				System.out.println("hidden nature of rowid is disabled");
			} else
				System.out.println("Can you check your command please \"" + userCommand + "\"");
			break;
		case "select":
			selectString(userCommand);
			break;
		case "drop":
			dropTableQuery(userCommand);
			break;
		case "create":
			if (commandTokens.get(1).equals("table"))
				createTableQuery(userCommand);
			else if (commandTokens.get(1).equals("index"))
				createIndexQuery(userCommand);
			break;
		case "update":
			updateTableQuery(userCommand);
			break;
		case "insert":
			insertTableQuery(userCommand);
			break;
		case "delete":
			deleteTableQuery(userCommand);
			break;
		case "help":
			help();
			break;
		case "version":
			displayVersion();
			break;
		case "exit":
			isExit = true;
			break;
		default:
			System.out.println("Something wrong with your command: \"" + userCommand + "\"");
			break;
		}
	}
	public static void createIndexQuery(String createQuery) {
		ArrayList<String> createQueryTokens = new ArrayList<String>(Arrays.asList(createQuery.split(" ")));
		try {
			if (!createQueryTokens.get(2).equals("on") || !createQuery.contains("(") || !createQuery.contains(")") && createQueryTokens.size() < 4) {
				System.out.println("Please check the query.");
				return;
			}
			String tableName = createQuery.substring(createQuery.indexOf("on") + 3, createQuery.indexOf("(")).trim();
			String columnName = createQuery.substring(createQuery.indexOf("(") + 1, createQuery.indexOf(")")).trim();
			if (new File(DavisBasePrompt.getNDXFilePath(tableName, columnName)).exists()) {
				System.out.println("Index already exsists on this column.");
				return;
			}
			
			RandomAccessFile tF = new RandomAccessFile(getTBLFilePath(tableName), "rw");
			TableMetaData tableMdata = new TableMetaData(tableName);
			if (!tableMdata.tableExistence) {
				System.out.println("Entered wrong table name.");
				tF.close();
				return;
			}
			int indexOfColumn = tableMdata.colNames.indexOf(columnName);
			if (indexOfColumn < 0) {
				System.out.println("Enter a valid column name.");
				tF.close();
				return;
			}
         
			RandomAccessFile iF = new RandomAccessFile(getNDXFilePath(tableName, columnName), "rw");
			Page.newPageAddition(iF, TypeOfPage.INDEXOFLEAF, -1, -1);
			if (tableMdata.rowCount > 0) {
				BPlusTree bPlusOneTree = new BPlusTree(tF, tableMdata.rootPageNumber, tableMdata.tableName);
				for (int pageNo : bPlusOneTree.getAllLeavesOfTree()) {
					Page page = new Page(tF, pageNo);
					BTree nodeBTree = new BTree(iF);
					for (Row eachRow : page.getPageRecords()) {
						nodeBTree.insertNode(eachRow.getProperties().get(indexOfColumn), eachRow.rowId);
					}
				}
			}
			System.out.println("Index is successfully created for the column : " + columnName);
			iF.close();
			tF.close();
		} catch (IOException e) {
			System.out.println("Something went wrong in Index creation.");
			System.out.println(e);
		}
	}
/*drop table method*/
	public static void dropTableQuery(String dropQuery) {
		System.out.println("\t Entered DROP query:\"" + dropQuery + "\"");
		
		String[] dropTokens = dropQuery.split(" ");
		if(!(dropTokens[0].trim().equalsIgnoreCase("DROP") && dropTokens[1].trim().equalsIgnoreCase("TABLE"))) {
			System.out.println("Enter a valid DROP query.");
			return;
		}
		ArrayList<String> dropQueryTokens = new ArrayList<String>(Arrays.asList(dropQuery.split(" ")));
		String tableName = dropQueryTokens.get(2);
		
		deleteTableQuery("delete from table "+ DavisBaseBinaryFile.tablesTable + " where table_name = '"+tableName+"' ");
		deleteTableQuery("delete from table "+ DavisBaseBinaryFile.columnsTable + " where table_name = '"+tableName+"' ");
		File tF = new File("data/"+tableName+".tbl");
        if(tF.delete()){
            System.out.println("Successfully dropped table.");
		}else System.out.println("Tables is not present.");
		
		
		File tmpFile = new File("data/");
		File[] searchFiles = tmpFile.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith(tableName) && name.endsWith("ndx");
			}
		});
		boolean indexFlag = false;
		for (File eachFile : searchFiles) {
			if(eachFile.delete()){
				indexFlag = true;
			}
		}
		if(indexFlag)
			System.out.println("Dropped "+tableName);
		else
			System.out.println("Index not present");
		
		
		
		
	}
/*select function*/
	public static void selectString(String selectInputString) {
		String tableName = "";
		List<String> attributes = new ArrayList<String>();
		ArrayList<String> selectStringTokens = new ArrayList<String>(Arrays.asList(selectInputString.split(" ")));
		int each_token = 0;
		for (each_token = 1; each_token < selectStringTokens.size(); each_token++) {
			if (selectStringTokens.get(each_token).equals("from")) {
				++each_token;
				tableName = selectStringTokens.get(each_token);
				break;
			}
			if (!selectStringTokens.get(each_token).equals("*") && !selectStringTokens.get(each_token).equals(",")) {
				if (selectStringTokens.get(each_token).contains(",")) {
					ArrayList<String> attributes_array = new ArrayList<String>(Arrays.asList(selectStringTokens.get(each_token).split(",")));
					for (String att : attributes_array) {
						attributes.add(att.trim());
					}
				} else
					attributes.add(selectStringTokens.get(each_token));
			}
		}
		TableMetaData table_check = new TableMetaData(tableName);
      if(!table_check.tableExistence){
         System.out.println("Table you requested doesn't exsist.");
         return;
      }
      
		Criteria criteria = null;
		try {
			criteria = gettingWhereQuery(table_check, selectInputString);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return;
		}
		if (attributes.size() == 0) {
			attributes = table_check.colNames;
		}
		try {
			RandomAccessFile tF = new RandomAccessFile(getTBLFilePath(tableName), "r");
			DavisBaseBinaryFile tBF = new DavisBaseBinaryFile(tF);
			tBF.selectRows(table_check, attributes, criteria);
			tF.close();
		} catch (IOException exception) {
			System.out.println("Something went wrong.");
		}
	}
/*update function*/
	public static void updateTableQuery(String updateQuery) {
		ArrayList<String> updateTokens = new ArrayList<String>(Arrays.asList(updateQuery.split(" ")));
		String tableName = updateTokens.get(1);
		List<String> listOfUpdateColumns = new ArrayList<>();
		List<String> listOfValuesForUpdate = new ArrayList<>();
		if (!updateTokens.get(2).equals("set") || !updateTokens.contains("=")) {
			System.out.println("Enter valid UPDATE query.");
			return;
		}
		String setString = updateQuery.split("set")[1].split("where")[0];
		List<String> setValueList = Arrays.asList(setString.split(","));
		for (String item : setValueList) {
			listOfUpdateColumns.add(item.split("=")[0].trim());
			listOfValuesForUpdate.add(item.split("=")[1].trim().replace("\"", "").replace("'", ""));
		}
		TableMetaData mData = new TableMetaData(tableName);
		if (!mData.tableExistence) {
			System.out.println("Please check table name.");
			return;
		}
		if (!mData.columnExists(listOfUpdateColumns)) {
			System.out.println("Please check column name.");
			return;
		}
		Criteria criteria = null;
		try {
			criteria = gettingWhereQuery(mData, updateQuery);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return;
		}
	
		try {
			RandomAccessFile rF = new RandomAccessFile(getTBLFilePath(tableName), "rw");
			DavisBaseBinaryFile bFileDavis = new DavisBaseBinaryFile(rF);
			int rowCountOfEffected = bFileDavis.updateRows(mData, criteria, listOfUpdateColumns, listOfValuesForUpdate);
		
			if(rowCountOfEffected > 0)
			{
			List<Integer> rowidList = new ArrayList<>();
		for(ColumnMetaData eachCol_Attribute : mData.columnNameProperties)
		{
			for(int i=0;i<listOfUpdateColumns.size();i++)
			if(eachCol_Attribute.name.equals(listOfUpdateColumns.get(i)) &&  eachCol_Attribute.hasIndex)
			{
					if(criteria == null) 
					{
					if(rowidList.size() == 0)
					{
						BPlusTree treeObjectOfPlus = new BPlusTree(rF, mData.rootPageNumber, mData.tableName);
						for (int pageNo : treeObjectOfPlus.getAllLeavesOfTree()) {
							Page presentPg = new Page(rF, pageNo);
							for (Row eachRow : presentPg.getPageRecords()) {
								rowidList.add(eachRow.rowId);
							}
						}
					}
						RandomAccessFile iF = new RandomAccessFile(getNDXFilePath(tableName, listOfUpdateColumns.get(i)),"rw");
						Page.newPageAddition(iF, TypeOfPage.INDEXOFLEAF, -1, -1);
						BTree treeObjectOfBt = new BTree(iF);
						treeObjectOfBt.insertNode(new Property(eachCol_Attribute.dataType,listOfValuesForUpdate.get(i)), rowidList);
					}
			}
		}
	}
			rF.close();
	
	} catch (Exception e) {
		out.println("Not able to update " + tableName + " file.");
		out.println(e);
	}
		
	}
	/*insert into table*/
	public static void insertTableQuery(String insertQuery) {
		ArrayList<String> insertQueryTokens = new ArrayList<String>(Arrays.asList(insertQuery.split(" ")));
		if (!insertQueryTokens.get(1).equals("into") || !insertQuery.contains(") values")) {
			System.out.println("Please check INSERT query.");
			return;
		}
		try {
			String tableName = insertQueryTokens.get(2);
			if (tableName.trim().length() == 0) {
				System.out.println("Table name is empty.");
				return;
			}
			if (tableName.indexOf("(") > -1) {
				tableName = tableName.substring(0, tableName.indexOf("("));
			}
			TableMetaData tableMData = new TableMetaData(tableName);
			if (!tableMData.tableExistence) {
				System.out.println("Table doesn't exist. So cannot Insert.");
				return;
			}
			ArrayList<String> tokenListOfColumns = new ArrayList<String>(Arrays.asList(insertQuery.substring(insertQuery.indexOf("(") + 1, insertQuery.indexOf(") values")).split(",")));
			for (String eachToken : tokenListOfColumns) {
				if (!tableMData.colNames.contains(eachToken.trim())) {
					System.out.println("please check column: " + eachToken.trim());
					return;
				}
			}
			String insertValues = insertQuery.substring(insertQuery.indexOf("values") + 6, insertQuery.length() - 1);
			ArrayList<String> tokenListOfValues = new ArrayList<String>(Arrays.asList(insertValues.substring(insertValues.indexOf("(") + 1, insertValues.length()).split(",")));
			List<Property> listOfInsertAttributes = new ArrayList<>();
			for (ColumnMetaData eachMDataAtt : tableMData.columnNameProperties) {
				int i = 0;
				boolean checkCol = false;
				for (i = 0; i < tokenListOfColumns.size(); i++) {
					if (tokenListOfColumns.get(i).trim().equals(eachMDataAtt.name)) {
						checkCol = true;
						try {
							String insertVal = tokenListOfValues.get(i).replace("'", "").replace("\"", "").trim();
							if (tokenListOfValues.get(i).trim().equals("null")) {
								if (!eachMDataAtt.isNull) {
									System.out.println("NULL values can't be placed in:  " + eachMDataAtt.name);
									return;
								}
								eachMDataAtt.dataType = DataType.NULL;
								insertVal = insertVal.toUpperCase();
							}
							Property property = new Property(eachMDataAtt.dataType, insertVal);
							listOfInsertAttributes.add(property);
							break;
						} catch (Exception e) {
							System.out.println("Datatype of column is unknown " + tokenListOfColumns.get(i) + " values: " + tokenListOfValues.get(i));
							return;
						}
					}
				}
				if (tokenListOfColumns.size() > i) {
					tokenListOfColumns.remove(i);
					tokenListOfValues.remove(i);
				}
				if (!checkCol) {
					if (eachMDataAtt.isNull)
						listOfInsertAttributes.add(new Property(DataType.NULL, "NULL"));
					else {
						System.out.println("NULL can't be inserted:  " + eachMDataAtt.name);
						return;
					}
				}
			}
			RandomAccessFile rF = new RandomAccessFile(getTBLFilePath(tableName), "rw");
			int PageNo = BPlusTree.getPageNumberForInsertRows(rF, tableMData.rootPageNumber);
			Page Page = new Page(rF, PageNo);
			int rowNo = Page.insertTableRow(tableName, listOfInsertAttributes);
			if (rowNo != -1) {
				for (int i = 0; i < tableMData.columnNameProperties.size(); i++) {
					ColumnMetaData infoCol = tableMData.columnNameProperties.get(i);
					if (infoCol.hasIndex) {
						RandomAccessFile iF = new RandomAccessFile(getNDXFilePath(tableName, infoCol.name),"rw");
						BTree ObjectOfBt = new BTree(iF);
						ObjectOfBt.insertNode(listOfInsertAttributes.get(i), rowNo);
					}
				}
			}
			rF.close();
			if (rowNo != -1)
				System.out.println("Rows inserted Successfully");
			System.out.println();
		} catch (Exception e) {
			System.out.println("Error occurred during insert");
			System.out.println(e);
		}
	}
/* creates new table */
	public static void createTableQuery(String createString) {
		ArrayList<String> createStringTokens = new ArrayList<String>(Arrays.asList(createString.split(" ")));
		if (!createStringTokens.get(1).equals("table")) {
			System.out.println("Please check the CREATE query.");
			return;
		}
		String tableName = createStringTokens.get(2);
		if (tableName.trim().length() == 0) {
			System.out.println("Provide table name.");
			return;
		}
		try {
			if (tableName.indexOf("(") > -1) {
				tableName = tableName.substring(0, tableName.indexOf("("));
			}
			List<ColumnMetaData> attributeListinfo = new ArrayList<>();
			ArrayList<String> attributeTokens = new ArrayList<String>(Arrays.asList(createString.substring(createString.indexOf("(") + 1, createString.length() - 1).split(",")));
			short ordinalPosition = 1;
			String primaryKeyColumn = "";
			for (String attributeToken : attributeTokens) {
				ArrayList<String> attributeInfo = new ArrayList<String>(Arrays.asList(attributeToken.trim().split(" ")));
				ColumnMetaData attinfo = new ColumnMetaData();
				attinfo.tableName = tableName;
				attinfo.name = attributeInfo.get(0);
				attinfo.isNull = true;
				attinfo.dataType = DataType.get(attributeInfo.get(1).toUpperCase());
				for (int i = 0; i < attributeInfo.size(); i++) {
					if ((attributeInfo.get(i).equals("null"))) {
						attinfo.isNull = true;
					}
					if (attributeInfo.get(i).contains("not") && (attributeInfo.get(i + 1).contains("null"))) {
						attinfo.isNull = false;
						i++;
					}
					if ((attributeInfo.get(i).equals("unique"))) {
						attinfo.isDistinct = true;
					} else if (attributeInfo.get(i).contains("primary") && (attributeInfo.get(i + 1).contains("key"))) {
						attinfo.primaryKey = true;
						attinfo.isDistinct = true;
						attinfo.isNull = false;
						primaryKeyColumn = attinfo.name;
						i++;
					}
				}
				attinfo.location = ordinalPosition++;
				attributeListinfo.add(attinfo);
			}
			
			RandomAccessFile tablesCatDavis = new RandomAccessFile(
					getTBLFilePath(DavisBaseBinaryFile.tablesTable), "rw");
			TableMetaData metDataDavisTable = new TableMetaData(DavisBaseBinaryFile.tablesTable);
			int pageNo = BPlusTree.getPageNumberForInsertRows(tablesCatDavis, metDataDavisTable.rootPageNumber);
			Page page = new Page(tablesCatDavis, pageNo);
			int rowNo = page.insertTableRow(DavisBaseBinaryFile.tablesTable,
					Arrays.asList(new Property[] { new Property(DataType.TEXT, tableName), 
							new Property(DataType.INT, "0"), new Property(DataType.SMALLINT, "0"),
							new Property(DataType.SMALLINT, "0") }));
			tablesCatDavis.close();
			if (rowNo == -1) {
				System.out.println("Table already exsists.");
				return;
			}
			RandomAccessFile tF = new RandomAccessFile(getTBLFilePath(tableName), "rw");
			Page.newPageAddition(tF, TypeOfPage.LEAF, -1, -1);
			tF.close();
			RandomAccessFile columnCatDavis = new RandomAccessFile(
					getTBLFilePath(DavisBaseBinaryFile.columnsTable), "rw");
			TableMetaData metDataDavisColumn = new TableMetaData(DavisBaseBinaryFile.columnsTable);
			pageNo = BPlusTree.getPageNumberForInsertRows(columnCatDavis, metDataDavisColumn.rootPageNumber);
			Page pageone = new Page(columnCatDavis, pageNo);
			for (ColumnMetaData each_col : attributeListinfo) {
				pageone.insertNewColumn(each_col);
			}
			columnCatDavis.close();
			System.out.println("Table Created Successfully.");
			if (primaryKeyColumn.length() > 0) {
				createIndexQuery("create index on " + tableName + "(" + primaryKeyColumn + ")");
			}
		} catch (Exception e) {
			System.out.println("Error occurred while creating table.");
			System.out.println(e.getMessage());
			deleteTableQuery("delete from table " + DavisBaseBinaryFile.tablesTable + " where table_name = '" + tableName
					+ "' ");
			deleteTableQuery("delete from table " + DavisBaseBinaryFile.columnsTable + " where table_name = '" + tableName
					+ "' ");
		}
	}
/* function to delete records from table */
	private static void deleteTableQuery(String deleteQuery) {
		ArrayList<String> deleteQueryTokens = new ArrayList<String>(Arrays.asList(deleteQuery.split(" ")));
		String tableName = "";
		try {
			if (!deleteQueryTokens.get(1).equals("from") || !deleteQueryTokens.get(2).equals("table")) {
				System.out.println("Please check DELETE query.");
				return;
			}
			tableName = deleteQueryTokens.get(3);
			TableMetaData tableMdata = new TableMetaData(tableName);
			Criteria criteria = null;
			try {
				criteria = gettingWhereQuery(tableMdata, deleteQuery);
			} catch (Exception e) {
				System.out.println(e);
				return;
			}
			RandomAccessFile tF = new RandomAccessFile(getTBLFilePath(tableName), "rw");
			BPlusTree BplusTreeObject = new BPlusTree(tF, tableMdata.rootPageNumber, tableMdata.tableName);
			List<Row> deleteRows = new ArrayList<Row>();
			int totaldeleterows = 0;
			for (int each_page : BplusTreeObject.getAllLeavesOfTree(criteria)) {
				short totalDeletesInPage = 0;
				Page newPage = new Page(tF, each_page);
				for (Row eachRow : newPage.getPageRecords()) {
					if (criteria != null) {
						if (!criteria.checkCriteria(eachRow.getProperties().get(criteria.ordinal).cellValue))
							continue;
					}
					deleteRows.add(eachRow);
					newPage.tableRowDeletion(tableName,Integer.valueOf(eachRow.pageHeaderIValue - totalDeletesInPage).shortValue());
					totalDeletesInPage++;
					totaldeleterows++;
				}
			}
			if (criteria == null) {
				
			} else {
				for (int eachMdata = 0; eachMdata < tableMdata.columnNameProperties.size(); eachMdata++) {
					if (tableMdata.columnNameProperties.get(eachMdata).hasIndex) {
						RandomAccessFile iF = new RandomAccessFile(getNDXFilePath(tableName, tableMdata.columnNameProperties.get(eachMdata).name), "rw");
						BTree bTreeObject = new BTree(iF);
						for (Row eachRow : deleteRows) {
							bTreeObject.deleteNode(eachRow.getProperties().get(eachMdata),eachRow.rowId);
						}
					}
				}
			}
			System.out.println();
			tF.close();
			System.out.println(totaldeleterows + " rows deleted");
		} catch (Exception e) {
			System.out.println("Something went wrong while deleting : " + tableName);
			System.out.println(e.getMessage());
		}
	}
	public static String getTBLFilePath(String tableName) {
		return "data/" + tableName + ".tbl";
	}
	public static String getNDXFilePath(String tableName, String columnName) {
		return "data/" + tableName + "_" + columnName + ".ndx";
	}
	private static Criteria gettingWhereQuery(TableMetaData table_check, String statement) throws Exception {
		if (statement.contains("where")) {
			Criteria criteria = new Criteria(DataType.TEXT);
			String criteriaQuery = statement.substring(statement.indexOf("where") + 6, statement.length());
			ArrayList<String> criteriaQueryTokens = new ArrayList<String>(Arrays.asList(criteriaQuery.split(" ")));
			if (criteriaQueryTokens.get(0).equalsIgnoreCase("not")) {
				criteria.setNot(true);
          }
        
          
          for (int i = 0; i < Criteria.operatorsDefined.length; i++) {
				if (criteriaQuery.contains(Criteria.operatorsDefined[i])) {
					criteriaQueryTokens = new ArrayList<String>(
							Arrays.asList(criteriaQuery.split(Criteria.operatorsDefined[i])));
				{	criteria.setOperation(Criteria.operatorsDefined[i]);
				criteria.setCriteria(criteriaQueryTokens.get(1).trim());
			   	 criteria.setColumn(criteriaQueryTokens.get(0).trim());
					break;
				}
				
				}
			}
			if (table_check.tableExistence
					&& table_check.columnExists(new ArrayList<String>(Arrays.asList(criteria.NameOfColumn)))) {
				criteria.ordinal = table_check.colNames.indexOf(criteria.NameOfColumn);
				criteria.dataType = table_check.columnNameProperties.get(criteria.ordinal).dataType;
			} else {
				throw new Exception(
						"Error in your Table or Column : " + table_check.tableName + " . " + criteria.NameOfColumn);
			}
			return criteria;
		} else
			return null;
	}
}