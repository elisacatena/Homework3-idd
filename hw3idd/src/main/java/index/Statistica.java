package index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.benchmark.quality.Judge;
import org.apache.lucene.benchmark.quality.QualityBenchmark;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.QualityQueryParser;
import org.apache.lucene.benchmark.quality.QualityStats;
import org.apache.lucene.benchmark.quality.trec.TrecJudge;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;
import org.apache.lucene.benchmark.quality.utils.SimpleQQParser;
import org.apache.lucene.benchmark.quality.utils.SubmissionReport;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import table.Cella;

public class Statistica {

	private Double nTables = 0.0;
	private Double nRows = 0.0;
	private Double nColumns = 0.0;
	private Double numNullValues = 0.0;
	private Map<Integer, Integer> col2count;
	private Map<Integer, Integer> row2count;
	private Map<Integer, Integer> distinct2col;
	private Map<Integer, Integer> distinct2row;

	public Statistica() {
		this.col2count = new HashMap<>();
		this.row2count = new HashMap<>();
		this.distinct2col = new HashMap<>();
		this.distinct2row = new HashMap<>();
	}

	public Double getNTables() {
		return nTables;
	}

	public void setNTables(Double nTables) {
		this.nTables = nTables;
	}

	public Double getNRows() {
		return nRows;
	}

	public void setNRows(Double nRows) {
		this.nRows = nRows;
	}

	public Double getNColumns() {
		return nColumns;
	}

	public void setNColumns(Double nColumns) {
		this.nColumns = nColumns;
	}

	public Double getNumNullValues() {
		return numNullValues;
	}

	public void setNumNullValues(Double nNullValues) {
		this.numNullValues = nNullValues;
	}

	public Map<Integer, Integer> getRow2count() {
		return row2count;
	}

	public void setRow2count(int size) {
		if(this.row2count.containsKey(size)) {
			Integer value = this.row2count.get(size);
			this.row2count.put(size, value+1);
		}
		else {
			this.row2count.put(size, 1);
		}
	}

	public Map<Integer, Integer> getCol2count() {
		return col2count;
	}

	public void setCol2count(int size) {
		if(this.col2count.containsKey(size)) {
			Integer value = this.col2count.get(size);
			this.col2count.put(size, value+1);
		}
		else {
			this.col2count.put(size, 1);
		}

	}

	public Map<Integer, Integer> getDistinct2col() {
		return distinct2col;
	}

	public void setDistinct2col(Map<Double, Set<String>> map) {
		for(Set<String> set : map.values()) {
			if(this.distinct2col.containsKey(set.size())) {
				Integer value = this.distinct2col.get(set.size());
				this.distinct2col.put(set.size(), value+1);
			}
			else {
				this.distinct2col.put(set.size(), 1);
			}
		}
	}

	public Map<Integer, Integer> getDistinct2row() {
		return distinct2row;
	}

	public void setDistinct2row(Map<Double, Set<String>> map) {
		for(Set<String> set : map.values()) {
			if(this.distinct2row.containsKey(set.size())) {
				Integer value = this.distinct2row.get(set.size());
				this.distinct2row.put(set.size(), value+1);
			}
			else {
				this.distinct2row.put(set.size(), 1);
			}
		}
	}

	public <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
		list.sort(Collections.reverseOrder(Entry.comparingByValue()));

		Map<K, V> result = new LinkedHashMap<>();
		for (Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	public void findTopK(Map<Cella, Integer> map) {
		map = this.sortByValue(map);
		for(Cella k : map.keySet()) {
			System.out.println("colonna " + k.getColonna() + ", tableID " + k.getTableId() + "-> " + map.get(k));
		}
		System.out.println("\n");

		int i = map.entrySet().iterator().next().getValue();

		for(Cella k : map.keySet()) {
			if(map.get(k) == i-3) break;
			System.out.println("colonna " + k.getColonna() + ", tableID " + k.getTableId() + "-> " + map.get(k));
		}
	}

	public void createStats(String tablePath) {
		BufferedReader br = null;
		Double nTab = 0.0;

		try {
			String currentLine;
			br = new BufferedReader(new FileReader(tablePath));

			while ((currentLine = br.readLine()) != null) {
				nTab += 1;
				ObjectMapper objectMapper = new ObjectMapper();

				try {
					JsonNode rootNode = objectMapper.readTree(currentLine);
					// Estrai l'oggetto "cells"
					JsonNode cellsNode = rootNode.get("cells");
					this.getCellStats(cellsNode);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			this.setNTables(nTab);
			this.setNRows(this.getNRows()/this.getNTables());
			this.setNColumns(this.getNColumns()/this.getNTables());
			this.setNumNullValues(this.getNumNullValues()/this.getNTables());

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	public void getCellStats(JsonNode cellsNode) {
		Set<Double> numRows = new HashSet<>();
		Set<Double> numCol = new HashSet<>();
		Map<Double, Set<String>> colDistinctValues = new HashMap<>();
		Map<Double, Set<String>> rowDistinctValues = new HashMap<>();

		for(int i = 0; i < cellsNode.size(); i++) {
			Double col = cellsNode.get(i).get("Coordinates").get("column").asDouble();
			Double row = cellsNode.get(i).get("Coordinates").get("row").asDouble();
			String cleanedText = cellsNode.get(i).get("cleanedText").asText();

			numRows.add(row);
			numCol.add(col);

			if(cleanedText.equals("Null")) {
				this.setNumNullValues(this.getNumNullValues()+1);
			}

			if(!cellsNode.get(i).get("isHeader").booleanValue()) {
				if(colDistinctValues.containsKey(col)) {
					colDistinctValues.get(col).add(cleanedText);
				}
				else {
					Set<String> distinctWords = new HashSet<>();
					distinctWords.add(cleanedText);
					colDistinctValues.put(col, distinctWords);
				}
	
				if(rowDistinctValues.containsKey(row)) {
					rowDistinctValues.get(row).add(cleanedText);
				}
				else {
					Set<String> distinctWords = new HashSet<>();
					distinctWords.add(cleanedText);
					rowDistinctValues.put(row, distinctWords);
				}
			}
		}

		this.setNRows(this.getNRows() + numRows.size());
		this.setRow2count(numRows.size());
		this.setNColumns(this.getNColumns() + numCol.size());
		this.setCol2count(numCol.size());
		this.setDistinct2col(colDistinctValues);
		this.setDistinct2row(rowDistinctValues);
	}

	public void generateMetrics(BufferedReader br) throws IOException {
		
		
	}


}
