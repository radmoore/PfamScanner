package parser;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class HmmerParser {
	
	private File domtblout, outfile;
	private boolean merge = false, resolveOverlaps = false, collapse = false, emptyProteins = false;
	private Double evalue = null;
	
	public HmmerParser(String domtbloutPath, String outfilePath) {
		try {
			domtblout = new File(domtbloutPath);
			outfile = new File(outfilePath);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setMergeMode() {
		this.merge = true;
	}
	
	public void setEmptyProteins(boolean empties) {
		this.emptyProteins = empties;
	}
	
	
	public void setCollapseMode() {
		this.collapse = true;
	}
	
	public void setResolveOverlapsMode() {
		this.resolveOverlaps = true;
	}
	
	// handle wrong format _before_ running hmmer
	public void setEvalueThreshold(Double evalue) {
		this.evalue = evalue;
	}
	
	public File getHmmerOutput(){
		return this.domtblout;
	}
	
	public void destoryTempFile () {
		this.domtblout.delete();
	}
	
	public void writeXdom() {
		
		TreeMap<Integer, Domain> currentDoms = new TreeMap<Integer, Domain>();
		Pattern comment = Pattern.compile("^#.+");
		
		try {
			FileWriter fw = new FileWriter(outfile);
					
			String line;
			FileInputStream fis = new FileInputStream(this.domtblout);
			DataInputStream dis = new DataInputStream(fis);
			BufferedReader br = new BufferedReader(new InputStreamReader(dis));
			String currentId = null;
			StringBuilder xdom = new StringBuilder();
			
			
			while((line = br.readLine())!= null) {	
				if (comment.matcher(line).matches())
					continue;
				
				String[] fields = line.split("\\s+");
				
				// 0 -> domain acc
				// 3 -> protein ID
				// 5 -> protein length
				// 12 -> domain I-evalue
				// 15, 16 -> hmm coord
				// 17, 18 -> align coord
				// 19, 20 -> env coord
				if ( (currentId != null) && (!fields[3].equals(currentId)) ) {
					if (xdom.length() != 0) {
						fw.write(xdom.toString()+"\n");
						
						// merge split hits
						if ( merge )
							currentDoms = mergeHits( currentDoms );
		
						// resolve overlaps
						if ( resolveOverlaps )
							currentDoms = resolveOverlaps( currentDoms, null );
						
						// write the rest of the domains
						for (int key : currentDoms.keySet()) {
							Domain cdom = currentDoms.get(key);
							
							fw.write(cdom.toString()+"\n");
						}
					}
					xdom.setLength(0);
					currentDoms.clear();
				}
				
				if (xdom.length() == 0) {
					currentId = fields[3];
					xdom.setLength(0);
					xdom.append(">"+currentId+"\t"+fields[5]);
				}
				if (evalue != null)
					if (Double.parseDouble(fields[12]) > evalue)
						continue;
				
				Domain dom = new Domain(fields[0],
						Integer.parseInt(fields[17]), 
						Integer.parseInt(fields[18]),
						Integer.parseInt(fields[15]),
						Integer.parseInt(fields[16]),
						Double.parseDouble(fields[12]));
				
				currentDoms.put(Integer.parseInt(fields[17]), dom);
			}
			if (xdom.length() != 0) {
				fw.write(xdom.toString()+"\n");
				if ( merge )
					currentDoms = mergeHits( currentDoms );
				
				if ( resolveOverlaps )
					resolveOverlaps( currentDoms, null );
			
				for (int key : currentDoms.keySet()) {
					Domain cdom = currentDoms.get(key);
					
					fw.write(cdom.toString()+"\n");
				}
			}			
			fis.close();
			dis.close();
			br.close();
			fw.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}	
	}
	
	private TreeMap<Integer, Domain> mergeHits(TreeMap<Integer, Domain> doms) {
		
		Domain lastDom = null, mergedDom = null;
		int lastStart = 0;
		int numOfMerged = 1; // n merge operations means n+1 merged domains
		TreeMap<Integer, Domain> modifedDoms = new TreeMap<Integer, Domain>();
		for ( int startPos : doms.keySet() ) {
			
			Domain curDom = doms.get(startPos);
			
			if (lastDom != null) {
				// if the same domain type...
				if (lastDom.getID().equals(curDom.getID())) {
					// ... check if split hits present
					if ( (lastDom.getHmmTo() < curDom.getHmmFrom()) &&
							(lastDom.getAliTo() < curDom.getAliFrom()) ) {
						
							numOfMerged += 1;
						// if we alread started a merged domain, extend
						if (mergedDom != null){
							mergedDom.setAliTo(curDom.getAliTo());
							mergedDom.setComment(numOfMerged+" merged hits");
						}
						// else start new merged Domain
						else {
							mergedDom = new Domain(curDom.getID(), 
									lastDom.getAliFrom(), // from beginning of last domain
									curDom.getAliTo(),    // to end of current domain
									lastDom.getHmmFrom(),
									curDom.getHmmTo(),
									-1);  // evalue?
							mergedDom.setComment(numOfMerged+" merged hits");
						}
					}
					// last domain ID is the same, but hits are not split
					else {
						modifedDoms.put(lastStart, lastDom);
					}
				}
				// last domain ID is different from current domain ID
				else {
					//first add merged domain, if present
					if (mergedDom != null) {
						modifedDoms.put(mergedDom.aliFrom, mergedDom);
					}
					// all is well - we will keep the last domain
					else { 
						modifedDoms.put(lastStart, lastDom);	
					}
					mergedDom = null;
					numOfMerged = 1;
				}
			}
			lastStart = startPos;
			lastDom = curDom;
		}
		if (mergedDom != null) {
			mergedDom.setComment(numOfMerged+" merged hits");
			modifedDoms.put(mergedDom.aliFrom, mergedDom);
		}
		// all is well - we will keep the last domain
		else
			modifedDoms.put(lastStart, lastDom);
		
		return modifedDoms;
	}
	
	private TreeMap<Integer, Domain> resolveOverlaps(TreeMap<Integer, Domain> doms, Domain lastDom) {
		
		ArrayList<Integer> flaggedToRemove = new ArrayList<Integer>();
		for ( int startPos : doms.keySet() ) {
			int lastStart = 0;
			Domain curDom = doms.get(startPos);
			if (lastDom != null) {
				if (lastDom.overlaps(curDom)) {
					if (lastDom.getEvalue() >= curDom.getEvalue())
						flaggedToRemove.add(lastStart);
						else
							flaggedToRemove.add(startPos);
				}
			}
			lastStart = startPos;
			lastDom = curDom;
		}
		if (! flaggedToRemove.isEmpty()) {
			for (int remPos : flaggedToRemove) {
				doms.remove(remPos);
			}
			resolveOverlaps(doms, null);
		}
		return doms;
		
	}
	
	
	private class Domain{
		
		private int aliFrom, aliTo, hmmFrom, hmmTo;
		private double evalue;
		private String ID, comment;
		
		public Domain(String ID, int aliFrom, int aliTo, int hmmFrom, int hmmTo, double evalue) {
			this.aliFrom = aliFrom;
			this.aliTo = aliTo;
			this.hmmFrom = hmmFrom;
			this.hmmTo = hmmTo;
			this.evalue = evalue;
			this.ID = ID;
		}
		
		public void setAliTo(int endPos) {
			this.aliTo = endPos;
		}
		
		public void setComment(String comment) {
			this.comment = comment;
		}
		
		public int getAliFrom() {
			return this.aliFrom;
		}
		
		public int getAliTo() {
			return this.aliTo;
		}
		
		public int getHmmFrom() {
			return this.hmmFrom;
		}
		
		public int getHmmTo() {
			return this.hmmTo;
		}
		
		public String getID() {
			return this.ID;
		}
		
		public double getEvalue() {
			return this.evalue;
		}
		
		public String toString() {
			if (comment == null)
				return this.aliFrom+"\t"+this.aliTo+"\t"+this.ID+"\t"+this.evalue;
			else
				return this.aliFrom+"\t"+this.aliTo+"\t"+this.ID+"\t"+this.evalue+"\t;"+this.comment;
		}
		
		public boolean overlaps(Domain nextDom) {
			if (this.aliTo > nextDom.aliFrom)
				return true;
			return false;
		}
		
	}
	
}
