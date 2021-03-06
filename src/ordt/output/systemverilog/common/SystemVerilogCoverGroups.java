/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.output.systemverilog.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import ordt.output.OutputWriterIntf;

/** class to hold/generate systemverilog info associated with a set of coverage groups
 *  @author snellenbach      
 *  Nov 8, 2015
 *
 * uses the following builder methods:
 *   writeStmt(..)
 */
public class SystemVerilogCoverGroups {

	private HashMap<String, CoverGroupInfo> coverGroups = new HashMap<String, CoverGroupInfo>();  // info for a set of registers
	private OutputWriterIntf writer;
	private String clkName;  // clock name for this group of covergroups
	private String resetName;  // reset name for this group of covergroups
	
	/** create VerilogRegisters 
	 * @param clkName */
	public SystemVerilogCoverGroups(OutputWriterIntf writer, String clkName, String resetName) {
		this.writer = writer;  // save reference to calling writer
		this.clkName = clkName;  // clock to be used for this group of covergroups
		this.resetName = resetName;  // reset to be used for this group of covergroups
	}

	/** retrieve a register or add if a new name **/
	public CoverGroupInfo get(String name) {
		CoverGroupInfo regInfo = this.coverGroups.get(name);
		if (regInfo == null) {
			regInfo = new CoverGroupInfo(name);
			this.coverGroups.put(name, regInfo);  // save new reginfo in hashmap
		}
		return regInfo;
	}
	
	/** create a coverpoint and add it to specified group
	 *  @param group - name of covergroup
	 *  @param name - name of new coverpoint
	 *  @param signal - signal to be sampled
	 */
	public void addCoverPoint(String group, String name, String signal, String condition) {
		CoverGroupInfo currentGroup;
		// create group if it doesnt exist
		if (coverGroups.containsKey(group)) currentGroup = coverGroups.get(group);
		else {
			currentGroup = new CoverGroupInfo(group);
			coverGroups.put(group, currentGroup);
		}
		//  add new coverpoint to this group
		currentGroup.addCoverPoint(name, signal, condition);
	}
	
	/** write out systemverilog for this set of covergroups  */
	public void write(int indentLevel) {
		if (coverGroups.isEmpty()) return;
		writer.writeStmt(indentLevel, "`ifndef SYNTHESIS");  
		for (String regName: coverGroups.keySet()) {
			coverGroups.get(regName).write(indentLevel);
		}
		writer.writeStmt(indentLevel, "`endif");  
	}

	// ----------------- inner classes --------------------
	
	/** class to hold info associated with a coverage group */   
	public  class CoverGroupInfo {
		private String name;   // name of this cover group
		private boolean synchronous;  // if true, sample group on clock
		private  List<CoverPointInfo> coverPointList;    // list of coverpoints in this cover group
		
		public CoverGroupInfo(String name) {
			this.name = name;
			this.synchronous = true;    // default to synchronous sample
			this.coverPointList = new ArrayList<CoverPointInfo>();    // list of coverpoints
		}
		/** create a coverpoint and add it to this group
		 *  @param name - name of new coverpoint
		 *  @param signal - signal to be sampled
		 *  @param condition - iff condition to be applead for this signal's coverpoint
		 */
		public void addCoverPoint(String name, String signal, String condition) {
			// create the new coverpoint and add it to this group
			CoverPointInfo cPoint = new CoverPointInfo(name, signal, condition);
			coverPointList.add(cPoint);
		}
		
		/** write out verilog for this covergroup  */
		public void write(int indentLevel) {
			// write this covergroup   
			if (!coverPointList.isEmpty()) {
				writer.writeStmt(indentLevel, "//------- covergroup " + name); 
				String syncString = synchronous? " @(posedge " + clkName + ")" : "";
				writer.writeStmt(indentLevel++, "covergroup " + name + syncString + ";");  
				Iterator<CoverPointInfo> it = coverPointList.iterator();
				while (it.hasNext()) {
					CoverPointInfo cPoint = it.next();
					cPoint.write(indentLevel); 
				}		  
				writer.writeStmt(--indentLevel, "endgroup");  
				writer.writeStmt(indentLevel, "");  			
				writer.writeStmt(indentLevel, name + " " + name + "_inst = new();");  
				writer.writeStmt(indentLevel, "");  			
			}
		}
	}
	
	/** class to hold info associated with a coverage group */   
	public  class CoverPointInfo {
		private String name;   // name of this cover point
		private String signal;   // signal being sampled
		private String condition;   // condition applied to this coverpoint
		
		public CoverPointInfo(String name, String signal, String condition) {
			this.name = name;
			this.signal = signal;
			if (condition != null) this.condition = condition;
			else this.condition = "!" + resetName;  // default to reset inactive condition
		}

		public void write(int indentLevel) {
			String condString = condition.isEmpty()? "" : " iff(" + condition + ")";
			writer.writeStmt(indentLevel, name + ": coverpoint " + signal + condString + ";");  
			
		}
	}

}
