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

	private static final String ddlFilePath = "F:\\Temp";
	private static final String ddlFileName = "dll_example.sql";
	private static List<String> statementList = null;
	private static Map<String, List<String>> groupMap = null;

	private static void loadStatements() {
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

	private static void getGroups() {
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

	private static void doGrouping() {
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

	private static void generateGroupedFiles() {
		for (Map.Entry<String, List<String>> entry : groupMap.entrySet()) {
			String group = entry.getKey();
			List<String> statements = entry.getValue();
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
						System.out.println("  |");
						System.out.println("  ---Statement: " + st);
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			System.out.println("Group: " + group);
		}
	}

	public static void main(String[] args) {
		loadStatements();
		getGroups();
		doGrouping();
		generateGroupedFiles();
	}

}
