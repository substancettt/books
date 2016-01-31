package com.wellshang.mytools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SQLServerDDLSpliter {

	private String ddlFilePath;
	private String ddlFileName;
	private List<String> statementList;
	private Map<String, List<String>> groupMap;

	public SQLServerDDLSpliter(String path, String file) {
		this.ddlFilePath = path;
		this.ddlFileName = file;
	}

	private void loadStatements() {
		if (ddlFilePath == null || ddlFileName == null) {
			System.out.println("Invalid parameters.");
			return;
		}

		try {
			String strStatement = "";
			String strLine = "";
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(ddlFilePath + "\\" + ddlFileName)));
			if (br != null) {
				statementList = new ArrayList<String>();
				while ((strLine = br.readLine()) != null) {
					strStatement += strLine + "\r\n";
					if (strLine.startsWith("GO")) {
						statementList.add(strStatement);
						strStatement = "";
					}
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void getGroups() {
		groupMap = new HashMap<String, List<String>>();
		List<String> statements = new ArrayList<String>();
		for (String st : statementList) {
			Pattern pattern = Pattern.compile("CREATE TABLE .+\\(");
			Matcher matcher = pattern.matcher(st);
			if (matcher.find()) {
				String tb = matcher.group().replace("CREATE TABLE ", "")
						.replace("(", "");
				if (tb.contains("[")) {
					tb = tb.split("\\.")[1].replace("[", "").replace("]", "")
							.trim();
				}
				groupMap.put(tb, statements);
			} else if (st.contains("CREATE USER")) {
				groupMap.put("USER", statements);
			} else if (st.contains("CREATE ROLE")) {
				groupMap.put("ROLE", statements);
			} else if (st.contains("CREATE SCHEMA")) {
				groupMap.put("SCHEMA", statements);
			} else {
				groupMap.put("OTHERS", statements);
			}
		}
	}

	private void doGrouping() {
		for (Map.Entry<String, List<String>> entry : groupMap.entrySet()) {
			String group = entry.getKey();
			List<String> statements = entry.getValue();

			for (String st : statementList) {
				if (st.contains(group)) {
					statements.add(st);
				} else {
					groupMap.get("OTHERS").add(st);
				}
			}
		}
	}

	private void generateGroupedFiles() {
		for (Map.Entry<String, List<String>> entry : groupMap.entrySet()) {
			String group = entry.getKey();
			List<String> statements = entry.getValue();
			
			System.out.println("Group: " + group);
			
			if (!group.equals("OTHERS")) {
				try {
					String filePath = ddlFilePath + "\\" + group + ".sql";
					File file = new File(filePath);
					if (!file.exists()) {
						file.createNewFile();
					}
					FileOutputStream out = new FileOutputStream(file, true);
					for (String st : statements) {
						st += "\r\n";
						out.write(st.getBytes("utf-8"));
						System.out.println("  |");
						System.out.println("  ---Statement: " + st);
					}
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				int index = 1;
				for (String st : statements) {
					try {
						String filePath = ddlFilePath + "\\" + "procedure_"
								+ index + ".sql";
						File file = new File(filePath);
						if (!file.exists()) {
							file.createNewFile();
						}
						FileOutputStream out = new FileOutputStream(file, true);
						st += "\r\n";
						out.write(st.getBytes("utf-8"));
						index++;
						System.out.println("  |");
						System.out.println("  ---Procedure: " + st);
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public void split() {
		loadStatements();
		getGroups();
		doGrouping();
		generateGroupedFiles();
	}

	public static void main(String[] args) {
		SQLServerDDLSpliter spliter = new SQLServerDDLSpliter("F:\\Temp", "dll_example.sql");
		spliter.split();
	}

}
