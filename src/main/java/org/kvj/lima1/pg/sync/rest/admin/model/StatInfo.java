package org.kvj.lima1.pg.sync.rest.admin.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class StatInfo {

	public Map<String, Integer> objects = new LinkedHashMap<String, Integer>();
	public int fileCount = 0;
	public long fileSize = 0;
}
